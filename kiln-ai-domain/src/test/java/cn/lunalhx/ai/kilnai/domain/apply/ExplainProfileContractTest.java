package cn.lunalhx.ai.kilnai.domain.apply;

import cn.lunalhx.ai.kilnai.domain.apply.bundle.BundleStack;
import cn.lunalhx.ai.kilnai.domain.apply.bundle.ReferenceBundles;
import cn.lunalhx.ai.kilnai.domain.apply.fake.ExplainScriptData;
import cn.lunalhx.ai.kilnai.domain.apply.fake.ExplainScriptData.StepJson;
import cn.lunalhx.ai.kilnai.domain.apply.fake.ScriptedExplainGenerationModel;
import cn.lunalhx.ai.kilnai.domain.apply.fixture.ExplainApplyFixture;
import cn.lunalhx.ai.kilnai.domain.apply.model.ApplyJson;
import cn.lunalhx.ai.kilnai.domain.apply.model.ApplyLearnerEvent;
import cn.lunalhx.ai.kilnai.domain.apply.model.ExplainDeliveryResult;
import cn.lunalhx.ai.kilnai.domain.apply.model.ExplainExecutionContext;
import cn.lunalhx.ai.kilnai.domain.apply.model.ExplainTeachingArtifact;
import cn.lunalhx.ai.kilnai.domain.apply.model.ExplainUnavailableReason;
import cn.lunalhx.ai.kilnai.domain.apply.model.TeachingProjection;
import cn.lunalhx.ai.kilnai.domain.apply.profile.ExplainProfile;
import cn.lunalhx.ai.kilnai.domain.apply.profile.ExplainProfileExecutor;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The focused Explain Profile contract suite: the compiled Profile contract,
 * the closed {@code explain_generation/v1} context and generation contract,
 * the two-gate validation with one bounded repair, Source Gap semantics, and
 * the learner/private visibility boundary. Whole-flow state assertions live
 * only in the Learning Flow graph contract test.
 */
class ExplainProfileContractTest {

    private final ExplainExecutionContext context = ExplainApplyFixture.explainContext();
    private final BundleStack stack = ReferenceBundles.explainStack();

    @Test
    void deliversAValidTeachingArtifactWithExactlyOneMappedWorkedExample() {
        ScriptedExplainGenerationModel generation =
                new ScriptedExplainGenerationModel(List.of(ExplainScriptData.explainReadyJson()));

        ExplainDeliveryResult.Delivered delivered = (ExplainDeliveryResult.Delivered)
                new ExplainProfileExecutor(stack, generation).deliver(context);

        ExplainTeachingArtifact artifact = delivered.artifact();
        TeachingProjection projection = artifact.learnerProjection();
        assertEquals(ExplainScriptData.PRINCIPLE_SUMMARY, projection.principleSummary());
        assertEquals(ExplainScriptData.EXPLAIN_PROBLEM, projection.workedExample().problem());
        assertEquals(4, projection.workedExample().steps().size(), "exactly one complete worked example");
        assertEquals(ExplainScriptData.EXPLAIN_FINAL_RESULT, projection.workedExample().finalResult());
        TeachingProjection.Step first = projection.workedExample().steps().get(0);
        assertEquals("d/dx[5x³] = 5 · d/dx[x³]", first.expression());
        assertEquals("constant-multiple rule", first.ruleReference());
        assertEquals("常数倍数法则：提出系数 5，只对 x³ 求导。", first.explanation());
        assertEquals(List.of(ApplyLearnerEvent.CONTINUE_REQUESTED, ApplyLearnerEvent.CLARIFICATION_ASKED,
                ApplyLearnerEvent.FLOW_CONTROL), projection.allowedEvents());

        assertEquals(1, artifact.sourceTrace().size());
        assertEquals("1.0.0", artifact.sourceTrace().get(0).sourceVersion());
        assertEquals("profile", artifact.exampleFingerprint().derivedBy());
        assertFalse(artifact.exampleFingerprint().value().isBlank());
        assertEquals(ExplainProfile.PROFILE_ID, artifact.executionTrace().profile());
        assertEquals(ExplainProfile.FIXED_STACK, artifact.executionTrace().skillStack());
    }

    @Test
    void separatesCompiledSystemInstructionsFromClosedExecutionContextJson() {
        ScriptedExplainGenerationModel generation =
                new ScriptedExplainGenerationModel(List.of(ExplainScriptData.explainReadyJson()));
        new ExplainProfileExecutor(stack, generation).deliver(context);

        String systemPrompt = generation.lastSystemPrompt();
        String contextJson = generation.lastContextJson();

        assertTrue(systemPrompt.contains("# Explain Profile"));
        assertTrue(systemPrompt.contains("[bundle:action:explain.worked-example@1.0.0]"));
        assertTrue(systemPrompt.contains("[bundle:subject:subject.calculus-notation@1.0.0]"));
        assertTrue(systemPrompt.contains("# Response Contract"));
        assertFalse(systemPrompt.contains(context.conceptSourcePack().passages().get(0).content()));
        assertFalse(systemPrompt.contains("\"learner_locale\""));

        assertEquals("explain_execution_context/v1",
                ApplyJson.readTree(contextJson).get("schema").asText());
        assertTrue(contextJson.contains("\"learner_locale\":\"zh-CN\""));
        assertTrue(contextJson.contains("\"intent\":\"remediate_diagnostic_failure\""));
        assertTrue(contextJson.contains("\"exposed_example_fingerprints\":[]"));
        assertFalse(contextJson.contains("# Explain Profile"));
    }

    @Test
    void theContextCarriesOnlySanitizedPedagogyFactsAndNoLearnerAnswers() {
        ScriptedExplainGenerationModel generation = new ScriptedExplainGenerationModel(
                List.of(ExplainScriptData.explainReadyJson()));
        new ExplainProfileExecutor(stack, generation).deliver(context);

        String contextJson = generation.lastContextJson();
        assertTrue(contextJson.contains("\"satisfied_criteria\":[]"));
        assertTrue(contextJson.contains("\"missing_criteria\":[]"));
        assertTrue(contextJson.contains("\"error_dimensions\":[]"));
        assertFalse(contextJson.contains("12x²−6x+7"), "no learner answer may enter the Explain context");
        assertFalse(contextJson.contains("15x² − 4x"), "no private expected answer may enter the Explain context");
    }

    @Test
    void sourceGapEndsGenerationImmediatelyWithoutAnArtifact() {
        ScriptedExplainGenerationModel generation =
                new ScriptedExplainGenerationModel(List.of(ExplainScriptData.explainSourceGapJson()));

        ExplainDeliveryResult.Unavailable unavailable = (ExplainDeliveryResult.Unavailable)
                new ExplainProfileExecutor(stack, generation).deliver(context);

        assertEquals(ExplainUnavailableReason.SOURCE_GAP, unavailable.reason());
        assertEquals(ExplainDeliveryResult.UNAVAILABLE_LEARNER_MESSAGE, unavailable.learnerMessage());
        assertEquals(1, generation.calls().size(), "Source Gap must never retry");
    }

    @Test
    void aRejectedFirstCandidatePermitsOneFreshCycleAndOnlyDeliversTheSecond() {
        List<StepJson> unapprovedRule = List.of(new StepJson(
                "d/dx[5x³ · x²]", "product rule", "乘积法则不在此概念的范围内。"));
        ScriptedExplainGenerationModel generation = new ScriptedExplainGenerationModel(List.of(
                ExplainScriptData.explainReadyJsonWithSteps(
                        ExplainScriptData.PRINCIPLE_SUMMARY, ExplainScriptData.EXPLAIN_PROBLEM,
                        ExplainScriptData.EXPLAIN_FINAL_RESULT, unapprovedRule),
                ExplainScriptData.explainReadyJson()));

        ExplainDeliveryResult.Delivered delivered = (ExplainDeliveryResult.Delivered)
                new ExplainProfileExecutor(stack, generation).deliver(context);

        assertEquals(2, generation.calls().size(), "a rejected candidate must permit one same-plan repair");
        assertEquals(ExplainScriptData.EXPLAIN_FINAL_RESULT,
                delivered.artifact().learnerProjection().workedExample().finalResult());
    }

    @Test
    void twoInvalidCandidatesReturnNodeExecutionFailedWithoutAnArtifact() {
        List<StepJson> unapprovedRule = List.of(new StepJson(
                "d/dx[5x³ · x²]", "product rule", "乘积法则不在此概念的范围内。"));
        ScriptedExplainGenerationModel generation = new ScriptedExplainGenerationModel(List.of(
                ExplainScriptData.explainReadyJsonWithSteps(
                        ExplainScriptData.PRINCIPLE_SUMMARY, ExplainScriptData.EXPLAIN_PROBLEM,
                        ExplainScriptData.EXPLAIN_FINAL_RESULT, unapprovedRule),
                ExplainScriptData.explainReadyJsonWithSteps(
                        ExplainScriptData.PRINCIPLE_SUMMARY, ExplainScriptData.EXPLAIN_PROBLEM,
                        ExplainScriptData.EXPLAIN_FINAL_RESULT, unapprovedRule)));

        ExplainDeliveryResult.Unavailable unavailable = (ExplainDeliveryResult.Unavailable)
                new ExplainProfileExecutor(stack, generation).deliver(context);

        assertEquals(ExplainUnavailableReason.NODE_EXECUTION_FAILED, unavailable.reason(),
                "a repeated invalid result must be Node Execution Failed, not a task-exhaustion reason");
        assertEquals(2, generation.calls().size());
    }

    @Test
    void anUnparseableCandidateCountsAsAFailedCycleAndAllowsOneRetry() {
        ScriptedExplainGenerationModel generation = new ScriptedExplainGenerationModel(List.of(
                "{not valid json", ExplainScriptData.explainReadyJson()));

        ExplainDeliveryResult.Delivered delivered = (ExplainDeliveryResult.Delivered)
                new ExplainProfileExecutor(stack, generation).deliver(context);

        assertEquals(2, generation.calls().size());
        assertEquals(ExplainScriptData.EXPLAIN_FINAL_RESULT,
                delivered.artifact().learnerProjection().workedExample().finalResult());
    }

    @Test
    void aDraftWithUnknownFieldsIsRejected() {
        String polluted = ExplainScriptData.explainReadyJson().replace(
                "\"principle_summary\"", "\"private_map\": {}, \"principle_summary\"");
        ScriptedExplainGenerationModel generation = new ScriptedExplainGenerationModel(List.of(
                polluted, ExplainScriptData.explainReadyJson()));

        ExplainDeliveryResult.Delivered delivered = (ExplainDeliveryResult.Delivered)
                new ExplainProfileExecutor(stack, generation).deliver(context);

        assertEquals(2, generation.calls().size());
        assertEquals(ExplainScriptData.EXPLAIN_FINAL_RESULT,
                delivered.artifact().learnerProjection().workedExample().finalResult());
    }

    @Test
    void aDraftWithAnUngroundedSourceTraceIsRejected() {
        String ungrounded = ExplainScriptData.explainReadyJson().replace(
                "sec-3.3-differentiation-rules", "sec-9.9-invented");
        ScriptedExplainGenerationModel generation = new ScriptedExplainGenerationModel(List.of(
                ungrounded, ExplainScriptData.explainReadyJson()));

        ExplainDeliveryResult.Delivered delivered = (ExplainDeliveryResult.Delivered)
                new ExplainProfileExecutor(stack, generation).deliver(context);

        assertEquals(2, generation.calls().size());
        assertEquals("openstax-calculus-v1",
                delivered.artifact().sourceTrace().get(0).sourceDocumentId());
    }

    @Test
    void anIncompleteWorkedExampleIsRejected() {
        List<StepJson> incomplete = List.of(new StepJson("d/dx[x³] = 3x²", "power rule for polynomial terms", " "));
        ScriptedExplainGenerationModel generation = new ScriptedExplainGenerationModel(List.of(
                ExplainScriptData.explainReadyJsonWithSteps(
                        ExplainScriptData.PRINCIPLE_SUMMARY, ExplainScriptData.EXPLAIN_PROBLEM,
                        ExplainScriptData.EXPLAIN_FINAL_RESULT, incomplete),
                ExplainScriptData.explainReadyJson()));

        ExplainDeliveryResult.Delivered delivered = (ExplainDeliveryResult.Delivered)
                new ExplainProfileExecutor(stack, generation).deliver(context);

        assertEquals(2, generation.calls().size());
        assertEquals(4, delivered.artifact().learnerProjection().workedExample().steps().size());
    }

    @Test
    void noveltyRejectsACandidateThatReExposesAnExampleFingerprint() {
        ExplainProfileExecutor executor = new ExplainProfileExecutor(stack,
                new ScriptedExplainGenerationModel(List.of(ExplainScriptData.explainReadyJson())));
        String exposedFingerprint = ((ExplainDeliveryResult.Delivered) executor.deliver(context))
                .artifact().exampleFingerprint().value();

        ExplainExecutionContext withExclusions = context.withNoveltyExclusions(List.of(exposedFingerprint));
        ScriptedExplainGenerationModel second = new ScriptedExplainGenerationModel(List.of(
                ExplainScriptData.explainReadyJson(),
                ExplainScriptData.explainReadyJson(ExplainScriptData.PRINCIPLE_SUMMARY,
                        "设 g(x) = 4x⁴ − x + 2，求 g'(x)。", "16x³ − 1")));

        ExplainDeliveryResult.Delivered fresh = (ExplainDeliveryResult.Delivered)
                new ExplainProfileExecutor(stack, second).deliver(withExclusions);

        assertEquals(2, second.calls().size(), "a re-exposed example must consume one fresh generation cycle");
        assertNotEquals(exposedFingerprint, fresh.artifact().exampleFingerprint().value(),
                "the delivered worked example must be materially different from the exposed one");
    }

    @Test
    void privateFactsNeverReachTheLearnerProjection() {
        String leaked = ExplainScriptData.explainReadyJson().replace(
                "常数法则：常数项的导数为零。", "来源 sec-3.3-differentiation-rules 说明常数法则：常数项的导数为零。");
        ScriptedExplainGenerationModel generation = new ScriptedExplainGenerationModel(List.of(
                leaked, ExplainScriptData.explainReadyJson()));

        ExplainDeliveryResult.Delivered delivered = (ExplainDeliveryResult.Delivered)
                new ExplainProfileExecutor(stack, generation).deliver(context);

        String learnerText = delivered.artifact().learnerProjection().principleSummary()
                + delivered.artifact().learnerProjection().workedExample();
        assertFalse(learnerText.contains("openstax"), "source identities must not reach the learner");
        assertFalse(learnerText.contains("sec-3.3"), "source anchors must not reach the learner");
        assertFalse(learnerText.contains(delivered.artifact().exampleFingerprint().value()),
                "fingerprints must not reach the learner");
        assertFalse(learnerText.contains("explain.worked-example"), "pinned bundle ids must not reach the learner");
        assertEquals(2, generation.calls().size(), "a leaked candidate must be rejected by the visibility gate");
    }

    @Test
    void theExampleFingerprintIsDeterministicallyDerived() {
        ExplainDeliveryResult.Delivered first = (ExplainDeliveryResult.Delivered) new ExplainProfileExecutor(
                stack, new ScriptedExplainGenerationModel(List.of(ExplainScriptData.explainReadyJson())))
                .deliver(context);
        ExplainDeliveryResult.Delivered second = (ExplainDeliveryResult.Delivered) new ExplainProfileExecutor(
                stack, new ScriptedExplainGenerationModel(List.of(ExplainScriptData.explainReadyJson())))
                .deliver(context);

        assertEquals(first.artifact().exampleFingerprint().value(),
                second.artifact().exampleFingerprint().value(),
                "the Profile, not the model, must own the derived example fingerprint");
    }

    @Test
    void theTeachingProjectionNeverContainsAnAssessableQuestion() {
        ScriptedExplainGenerationModel generation =
                new ScriptedExplainGenerationModel(List.of(ExplainScriptData.explainReadyJson()));

        ExplainDeliveryResult.Delivered delivered = (ExplainDeliveryResult.Delivered)
                new ExplainProfileExecutor(stack, generation).deliver(context);

        String learnerText = delivered.artifact().learnerProjection().principleSummary()
                + delivered.artifact().learnerProjection().workedExample();
        assertFalse(learnerText.contains("?"), "teaching content must not contain an assessable question");
        assertFalse(delivered.artifact().learnerProjection().allowedEvents().contains(ApplyLearnerEvent.ANSWER_SUBMITTED),
                "Explain must never open an answer event");
    }
}
