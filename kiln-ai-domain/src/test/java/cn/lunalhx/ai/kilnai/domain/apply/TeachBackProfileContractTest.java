package cn.lunalhx.ai.kilnai.domain.apply;

import cn.lunalhx.ai.kilnai.domain.apply.bundle.BundleStack;
import cn.lunalhx.ai.kilnai.domain.apply.bundle.ReferenceBundles;
import cn.lunalhx.ai.kilnai.domain.apply.fake.ScriptedTeachBackGenerationModel;
import cn.lunalhx.ai.kilnai.domain.apply.fake.ScriptedModelProfile;
import cn.lunalhx.ai.kilnai.domain.apply.fake.ScriptedTeachBackTaskVerifier;
import cn.lunalhx.ai.kilnai.domain.apply.fake.TeachBackScriptData;
import cn.lunalhx.ai.kilnai.domain.apply.fixture.TeachBackApplyFixture;
import cn.lunalhx.ai.kilnai.domain.apply.model.ApplyJson;
import cn.lunalhx.ai.kilnai.domain.apply.model.ModelProfile;
import cn.lunalhx.ai.kilnai.domain.apply.model.ApplyLearnerEvent;
import cn.lunalhx.ai.kilnai.domain.apply.model.LearnerProjection;
import cn.lunalhx.ai.kilnai.domain.apply.model.TaskAttempt;
import cn.lunalhx.ai.kilnai.domain.apply.model.TaskVerificationVerdict;
import cn.lunalhx.ai.kilnai.domain.apply.model.TeachBackDeliveryResult;
import cn.lunalhx.ai.kilnai.domain.apply.model.TeachBackExecutionContext;
import cn.lunalhx.ai.kilnai.domain.apply.model.TeachBackTaskPackage;
import cn.lunalhx.ai.kilnai.domain.apply.model.TeachBackUnavailableReason;
import cn.lunalhx.ai.kilnai.domain.apply.profile.TeachBackProfile;
import cn.lunalhx.ai.kilnai.domain.apply.profile.TeachBackProfileExecutor;
import cn.lunalhx.ai.kilnai.domain.apply.store.InMemoryArtifactStore;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.AttemptPurpose;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The focused Teach-back Profile contract suite: the compiled Profile
 * contract, the closed {@code teach_back_generation/v1} context and
 * generation contract, the typed Output Gate with one bounded repair,
 * isolated Task Verification with one fresh second candidate, Source Gap
 * semantics, anchor eligibility, and the learner/private visibility boundary.
 * Whole-flow state assertions live only in the Learning Flow graph contract
 * test.
 */
class TeachBackProfileContractTest {

    private static final ModelProfile PROFILE = ScriptedModelProfile.PROFILE;

    private static final Clock CLOCK =
            Clock.fixed(Instant.parse("2026-08-16T00:00:00Z"), ZoneOffset.UTC);

    private final InMemoryArtifactStore artifacts = new InMemoryArtifactStore(CLOCK);
    private final BundleStack stack = ReferenceBundles.teachBackStack();
    private ScriptedTeachBackGenerationModel generation;
    private ScriptedTeachBackTaskVerifier verifier;

    @Test
    void deliversAValidatedShortTextTaskPackageWithTheThreeRubricDimensions() {
        TeachBackProfileExecutor executor = executor(
                List.of(TeachBackScriptData.taskReadyJson()),
                List.of(passVerdict()));

        TeachBackDeliveryResult.Delivered delivered =
                (TeachBackDeliveryResult.Delivered) executor.deliver(PROFILE, context());

        TaskAttempt attempt = delivered.attempt();
        assertEquals(AttemptPurpose.PRACTICE, attempt.purpose());
        assertTrue(attempt.isOpen());
        TeachBackTaskPackage taskPackage =
                artifacts.findTeachBackPackage(attempt.taskPackageId()).orElseThrow();
        assertEquals(TeachBackTaskPackage.SCHEMA, taskPackage.schema());
        LearnerProjection projection = taskPackage.learnerProjection();
        assertEquals("zh-CN", projection.locale());
        assertEquals(TeachBackScriptData.LEARNER_PROMPT, projection.taskText());
        assertEquals(1, projection.answerFields().size(),
                "exactly one short-text response field, no derivative field");
        assertEquals("short_text_response", projection.answerFields().get(0).id());
        assertEquals("short_text", projection.answerFields().get(0).kind());
        assertTrue(projection.answerFields().get(0).required());
        assertEquals(List.of(ApplyLearnerEvent.ANSWER_SUBMITTED, ApplyLearnerEvent.CLARIFICATION_ASKED,
                ApplyLearnerEvent.FLOW_CONTROL), projection.allowedEvents(),
                "Teach-back never permits Hint (ADR-0065)");
        assertEquals(1, projection.submissionRule().maxFormalSubmissions(),
                "exactly one formal submission");
        assertEquals(List.of("rule_identification", "applicability_explanation", "steps_result_coherence"),
                taskPackage.privateProjection().rubricMapping().stream()
                        .map(TeachBackTaskPackage.RubricDimension::dimension).toList());
        assertEquals(TeachBackScriptData.ANCHOR_ID,
                taskPackage.privateProjection().anchorReference().anchorId());
        assertEquals(TeachBackProfile.PROFILE_ID, taskPackage.privateProjection().executionTrace().profile());
        assertEquals(ReferenceBundles.teachBackStack().pinnedIds(),
                taskPackage.privateProjection().executionTrace().skillStack());
        assertEquals(1, artifacts.verificationsFor(taskPackage.taskPackageId()).size(),
                "the delivered Teach-back task must be verified before delivery");
    }

    @Test
    void separatesCompiledSystemInstructionsFromClosedExecutionContextJson() {
        TeachBackProfileExecutor executor = executor(
                List.of(TeachBackScriptData.taskReadyJson()),
                List.of(passVerdict()));
        executor.deliver(PROFILE, context());

        String systemPrompt = generation.lastSystemPrompt();
        String contextJson = generation.lastContextJson();

        assertTrue(systemPrompt.contains("# Teach-back Profile"));
        assertTrue(systemPrompt.contains("[bundle:action:teach-back.anchored-explanation@1.0.0]"));
        assertTrue(systemPrompt.contains("[bundle:subject:subject.calculus-notation@1.0.0]"));
        assertTrue(systemPrompt.contains("# Response Contract"));
        assertFalse(systemPrompt.contains("```"), "response-contract examples must be bare JSON, not fenced");
        assertFalse(systemPrompt.contains("\"learner_locale\""));

        assertEquals("teach_back_execution_context/v1",
                ApplyJson.readTree(contextJson).get("schema").asText());
        assertTrue(contextJson.contains("\"learner_locale\":\"zh-CN\""));
        assertTrue(contextJson.contains("\"anchor_id\":\"" + TeachBackScriptData.ANCHOR_ID + "\""));
        assertTrue(contextJson.contains("\"anchor_kind\":\"EXPLAIN_WORKED_EXAMPLE\""));
        assertTrue(contextJson.contains("\"source_document_id\":\"openstax-calculus-v1\""));
        assertFalse(contextJson.contains("# Teach-back Profile"));
    }

    @Test
    void theContextCarriesOnlyTheExposedAnchorAndNoPrivateFacts() {
        TeachBackProfileExecutor executor = executor(
                List.of(TeachBackScriptData.taskReadyJson()),
                List.of(passVerdict()));
        executor.deliver(PROFILE, context());

        String contextJson = generation.lastContextJson();
        assertTrue(contextJson.contains("\"intent\":\"remediate_diagnostic_failure\""));
        assertTrue(contextJson.contains(TeachBackScriptData.ANCHOR_CONTENT),
                "the already exposed anchor content is the only reference material");
        assertFalse(contextJson.contains("\"canonical\""),
                "no private expected answer may enter the Teach-back context");
        assertFalse(contextJson.contains("12x²−6x+7"), "no learner answer may enter the Teach-back context");
        assertFalse(contextJson.contains("\"exposed_task_fingerprints\""),
                "the Teach-back context receives no novelty ledger");
    }

    @Test
    void sourceGapEndsGenerationImmediatelyWithoutAnAttempt() {
        TeachBackProfileExecutor executor = executor(
                List.of(TeachBackScriptData.sourceGapJson()),
                List.of());

        TeachBackDeliveryResult.Unavailable unavailable =
                (TeachBackDeliveryResult.Unavailable) executor.deliver(PROFILE, context());

        assertEquals(TeachBackUnavailableReason.SOURCE_GAP, unavailable.reason());
        assertEquals(TeachBackDeliveryResult.UNAVAILABLE_LEARNER_MESSAGE, unavailable.learnerMessage());
        assertEquals(1, generation.calls().size(), "Source Gap must never retry");
        assertTrue(artifacts.allPackages().isEmpty());
        assertTrue(artifacts.findTeachBackPackage(TeachBackScriptData.ANCHOR_ID).isEmpty());
    }

    @Test
    void aRejectedCandidatePermitsOneSamePlanRepairAndOnlyDeliversTheSecond() {
        String leaked = TeachBackScriptData.taskReadyJson(
                "请解释 sec-3.3-differentiation-rules 中使用的求导法则。");
        TeachBackProfileExecutor executor = executor(
                List.of(leaked, TeachBackScriptData.taskReadyJson()),
                List.of(passVerdict()));

        TeachBackDeliveryResult.Delivered delivered =
                (TeachBackDeliveryResult.Delivered) executor.deliver(PROFILE, context());

        assertEquals(2, generation.calls().size(),
                "a rejected candidate must permit one same-plan repair");
        assertEquals(TeachBackScriptData.LEARNER_PROMPT, delivered.learnerProjection().taskText());
    }

    @Test
    void aVerifierRejectionDiscardsTheCandidateAndPermitsOneFreshSecondCandidate() {
        TeachBackProfileExecutor executor = executor(
                List.of(TeachBackScriptData.taskReadyJson(), TeachBackScriptData.taskReadyJson()),
                List.of(rejectVerdict(), passVerdict()));

        TeachBackDeliveryResult.Delivered delivered =
                (TeachBackDeliveryResult.Delivered) executor.deliver(PROFILE, context());

        assertEquals(2, generation.calls().size(), "a verifier rejection must permit one fresh second candidate");
        assertEquals(2, verifier.verified().size(),
                "both candidates must pass through isolated Task Verification");
        assertEquals(1, artifacts.verificationsFor(verifier.verified().get(0).taskPackageId()).size(),
                "the rejected first candidate's verdict must be retained as an audit record");
        assertEquals(TaskVerificationVerdict.Verdict.REJECT,
                artifacts.verificationsFor(verifier.verified().get(0).taskPackageId()).get(0).verdict());
        assertNotNull(delivered.learnerProjection().taskText());
    }

    @Test
    void exhaustedGenerationAfterARejectedVerifierCreatesNoAttempt() {
        TeachBackProfileExecutor executor = executor(
                List.of(TeachBackScriptData.taskReadyJson(), TeachBackScriptData.taskReadyJson()),
                List.of(rejectVerdict(), rejectVerdict()));

        TeachBackDeliveryResult.Unavailable unavailable =
                (TeachBackDeliveryResult.Unavailable) executor.deliver(PROFILE, context());

        assertEquals(TeachBackUnavailableReason.TASK_GENERATION_EXHAUSTED, unavailable.reason(),
                "a verifier rejection followed by a rejected fresh candidate is Task Generation Exhausted");
        assertEquals(2, generation.calls().size());
        assertTrue(artifacts.findTeachBackPackage(TeachBackScriptData.ANCHOR_ID).isEmpty(),
                "no Teach-back package or Attempt may exist after exhaustion");
    }

    @Test
    void aRepairedDraftRejectedByTheVerifierStillGetsOneFreshSecondCandidate() {
        String leaked = TeachBackScriptData.taskReadyJson(
                "请解释 sec-3.3-differentiation-rules 中使用的求导法则。");
        TeachBackProfileExecutor executor = executor(
                List.of(leaked, TeachBackScriptData.taskReadyJson(), TeachBackScriptData.taskReadyJson()),
                List.of(rejectVerdict(), passVerdict()));

        TeachBackDeliveryResult.Delivered delivered =
                (TeachBackDeliveryResult.Delivered) executor.deliver(PROFILE, context());

        assertEquals(3, generation.calls().size(),
                "the spec grants one same-plan repair AND one fresh second candidate");
        assertEquals(TeachBackScriptData.LEARNER_PROMPT, delivered.learnerProjection().taskText());
    }

    @Test
    void anUnparseableCandidateCountsAsAFailedCycleAndAllowsOneRetry() {
        TeachBackProfileExecutor executor = executor(
                List.of("{not valid json", TeachBackScriptData.taskReadyJson()),
                List.of(passVerdict()));

        TeachBackDeliveryResult.Delivered delivered =
                (TeachBackDeliveryResult.Delivered) executor.deliver(PROFILE, context());

        assertEquals(2, generation.calls().size());
        assertEquals(TeachBackScriptData.LEARNER_PROMPT, delivered.learnerProjection().taskText());
    }

    @Test
    void anUngroundedSourceTraceIsRejected() {
        String ungrounded = TeachBackScriptData.taskReadyJson(
                TeachBackScriptData.LEARNER_PROMPT, "openstax-calculus-v1", "sec-9.9-invented",
                TeachBackScriptData.ANCHOR_ID.toString(), TeachBackScriptData.ANCHOR_KIND);
        TeachBackProfileExecutor executor = executor(
                List.of(ungrounded, TeachBackScriptData.taskReadyJson()),
                List.of(passVerdict()));

        TeachBackDeliveryResult.Delivered delivered =
                (TeachBackDeliveryResult.Delivered) executor.deliver(PROFILE, context());

        assertEquals(2, generation.calls().size(),
                "a source trace outside the anchor's trace must consume one repair");
        TeachBackTaskPackage deliveredPackage =
                artifacts.findTeachBackPackage(delivered.attempt().taskPackageId()).orElseThrow();
        assertEquals("sec-3.3-differentiation-rules",
                deliveredPackage.privateProjection().sourceTrace().get(0).passageId(),
                "the delivered package must be grounded in the approved anchor passage");
    }

    @Test
    void aMismatchedAnchorReferenceIsRejected() {
        String wrongAnchor = TeachBackScriptData.taskReadyJson(
                TeachBackScriptData.LEARNER_PROMPT,
                "00000000-0000-0000-0000-00000000b6b6", TeachBackScriptData.ANCHOR_KIND);
        TeachBackProfileExecutor executor = executor(
                List.of(wrongAnchor, TeachBackScriptData.taskReadyJson()),
                List.of(passVerdict()));

        TeachBackDeliveryResult.Delivered delivered =
                (TeachBackDeliveryResult.Delivered) executor.deliver(PROFILE, context());

        assertEquals(2, generation.calls().size(),
                "a draft anchored to a different anchor must be rejected");
        assertEquals(TeachBackScriptData.ANCHOR_ID,
                artifacts.findTeachBackPackage(delivered.attempt().taskPackageId()).orElseThrow()
                        .privateProjection().anchorReference().anchorId());
    }

    @Test
    void privateFactsNeverReachTheLearnerProjection() {
        String leaked = TeachBackScriptData.taskReadyJson(
                "请解释 " + TeachBackScriptData.ANCHOR_ID + " 中使用的求导法则。");
        TeachBackProfileExecutor executor = executor(
                List.of(leaked, TeachBackScriptData.taskReadyJson()),
                List.of(passVerdict()));

        TeachBackDeliveryResult.Delivered delivered =
                (TeachBackDeliveryResult.Delivered) executor.deliver(PROFILE, context());

        String learnerText = delivered.learnerProjection().taskText();
        assertFalse(learnerText.contains("openstax"), "source identities must not reach the learner");
        assertFalse(learnerText.contains("sec-3.3"), "source anchors must not reach the learner");
        assertFalse(learnerText.contains(TeachBackScriptData.ANCHOR_ID.toString()),
                "anchor ids must not reach the learner");
        assertFalse(learnerText.contains("teach-back.anchored-explanation"),
                "pinned bundle ids must not reach the learner");
        assertEquals(2, generation.calls().size(),
                "a leaked candidate must be rejected by the visibility gate");
    }

    private TeachBackExecutionContext context() {
        return TeachBackApplyFixture.teachBackContext().withAnchor(new TeachBackExecutionContext.AnchorView(
                TeachBackScriptData.ANCHOR_ID,
                TeachBackScriptData.ANCHOR_KIND,
                TeachBackScriptData.ANCHOR_CONTENT,
                List.of(new TeachBackExecutionContext.SourceTraceRef(
                        TeachBackScriptData.SOURCE_DOCUMENT, TeachBackScriptData.SOURCE_PASSAGE))));
    }

    private TeachBackProfileExecutor executor(
            List<String> generationResponses,
            List<TaskVerificationVerdict> verdicts
    ) {
        this.generation = new ScriptedTeachBackGenerationModel(generationResponses);
        this.verifier = new ScriptedTeachBackTaskVerifier(verdicts);
        return new TeachBackProfileExecutor(stack, generation, verifier, artifacts);
    }

    private static TaskVerificationVerdict passVerdict() {
        return new TaskVerificationVerdict(
                TaskVerificationVerdict.SCHEMA,
                TaskVerificationVerdict.Verdict.PASS,
                Map.of(
                        "answer_clarity", TaskVerificationVerdict.CheckResult.PASS,
                        "rubric_alignment", TaskVerificationVerdict.CheckResult.PASS,
                        "source_grounding", TaskVerificationVerdict.CheckResult.PASS,
                        "anchor_grounding", TaskVerificationVerdict.CheckResult.PASS,
                        "learner_boundary", TaskVerificationVerdict.CheckResult.PASS),
                List.of());
    }

    private static TaskVerificationVerdict rejectVerdict() {
        return new TaskVerificationVerdict(
                TaskVerificationVerdict.SCHEMA,
                TaskVerificationVerdict.Verdict.REJECT,
                Map.of(
                        "answer_clarity", TaskVerificationVerdict.CheckResult.REJECT,
                        "rubric_alignment", TaskVerificationVerdict.CheckResult.PASS,
                        "source_grounding", TaskVerificationVerdict.CheckResult.PASS,
                        "anchor_grounding", TaskVerificationVerdict.CheckResult.PASS,
                        "learner_boundary", TaskVerificationVerdict.CheckResult.PASS),
                List.of("ambiguous_prompt"));
    }
}