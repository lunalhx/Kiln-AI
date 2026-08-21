package cn.lunalhx.ai.kilnai.domain.apply;

import cn.lunalhx.ai.kilnai.domain.apply.bundle.EvaluationBundleStack;
import cn.lunalhx.ai.kilnai.domain.apply.bundle.ReferenceBundles;
import cn.lunalhx.ai.kilnai.domain.apply.fake.ApplyScriptData;
import cn.lunalhx.ai.kilnai.domain.apply.fake.ScriptedApplyGenerationModel;
import cn.lunalhx.ai.kilnai.domain.apply.fake.ScriptedModelProfile;
import cn.lunalhx.ai.kilnai.domain.apply.fake.ScriptedTaskVerifier;
import cn.lunalhx.ai.kilnai.domain.apply.fixture.DiagnosticApplyFixture;
import cn.lunalhx.ai.kilnai.domain.apply.fixture.DiagnosticPlanFixture;
import cn.lunalhx.ai.kilnai.domain.apply.fixture.IndependentApplyFixture;
import cn.lunalhx.ai.kilnai.domain.apply.flow.DiagnosticFlow;
import cn.lunalhx.ai.kilnai.domain.apply.model.ApplyDeliveryResult;
import cn.lunalhx.ai.kilnai.domain.apply.model.ApplyExecutionContext;
import cn.lunalhx.ai.kilnai.domain.apply.model.ApplyJson;
import cn.lunalhx.ai.kilnai.domain.apply.model.CommittedEvaluationResult;
import cn.lunalhx.ai.kilnai.domain.apply.model.DiagnosticSubmissionResult;
import cn.lunalhx.ai.kilnai.domain.apply.model.ModelContractInvalidException;
import cn.lunalhx.ai.kilnai.domain.apply.model.PostSubmissionEvaluationUnavailableException;
import cn.lunalhx.ai.kilnai.domain.apply.model.RationaleEvaluationContext;
import cn.lunalhx.ai.kilnai.domain.apply.model.RationaleEvaluationResult;
import cn.lunalhx.ai.kilnai.domain.apply.port.ArtifactStore;
import cn.lunalhx.ai.kilnai.domain.apply.port.AssessmentPort;
import cn.lunalhx.ai.kilnai.domain.apply.port.LearningFlowStore;
import cn.lunalhx.ai.kilnai.domain.apply.port.RationaleAssessmentPort;
import cn.lunalhx.ai.kilnai.domain.apply.port.ResponseVerificationPort;
import cn.lunalhx.ai.kilnai.domain.apply.profile.RationaleEvaluationProfile;
import cn.lunalhx.ai.kilnai.domain.apply.profile.RationaleEvaluationProfileExecutor;
import cn.lunalhx.ai.kilnai.domain.apply.profile.RationaleEvaluationPromptCompiler;
import cn.lunalhx.ai.kilnai.domain.apply.profile.CounterexampleReviewProfile;
import cn.lunalhx.ai.kilnai.domain.apply.store.InMemoryArtifactStore;
import cn.lunalhx.ai.kilnai.domain.apply.store.InMemoryLearningFlowStore;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RationaleEvaluationContractTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-08-14T00:00:00Z"), ZoneOffset.UTC);
    private static final UUID FLOW_ID = UUID.fromString("00000000-0000-0000-0000-00000000000b");

    @Test
    void resultDerivesTheClosedVerdictAndCanonicalizesReasonCodes() {
        RationaleEvaluationResult result = new RationaleEvaluationResult(
                RationaleEvaluationResult.SCHEMA,
                RationaleEvaluationResult.Verdict.NOT_APPLICABLE,
                RationaleEvaluationResult.DimensionJudgment.FAIL,
                RationaleEvaluationResult.DimensionJudgment.PASS,
                RationaleEvaluationResult.DimensionJudgment.INCONCLUSIVE,
                List.of(
                        RationaleEvaluationResult.ReasonCode.CONTRADICTION,
                        RationaleEvaluationResult.ReasonCode.MISSING_SUPPORT,
                        RationaleEvaluationResult.ReasonCode.CONTRADICTION));

        assertEquals(RationaleEvaluationResult.Verdict.NOT_APPLICABLE, result.verdict());
        assertEquals(List.of(
                RationaleEvaluationResult.ReasonCode.MISSING_SUPPORT,
                RationaleEvaluationResult.ReasonCode.CONTRADICTION), result.reasonCodes());
        String json = ApplyJson.writeContract(result);
        assertTrue(json.contains("\"reason_codes\":[\"missing_support\",\"contradiction\"]"));
        assertFalse(json.contains("calculus"));
        assertFalse(json.contains("derivative"));
        assertFalse(json.contains("polynomial"));
    }

    @Test
    void inconsistentResultIsAClosedModelContractFailure() {
        assertThrows(ModelContractInvalidException.class, () -> RationaleEvaluationResult.parse("""
                {
                  "schema": "rationale_evaluation/v1",
                  "verdict": "applicable",
                  "rubric_basis": "fail",
                  "task_connection": "pass",
                  "coherence": "pass",
                  "reason_codes": ["material_gap"]
                }
                """));
    }

    @Test
    void profileStackAndPromptAreSubjectNeutral() {
        EvaluationBundleStack stack = ReferenceBundles.rationaleEvaluationStack();
        assertEquals(RationaleEvaluationProfile.FIXED_STACK, stack.pinnedIds());

        String prompt = new RationaleEvaluationPromptCompiler().compile(stack);
        assertTrue(prompt.contains("# Rationale Evaluation Profile"));
        assertTrue(prompt.contains("[bundle:evaluation:evaluation.rationale-assessment@1.0.0]"));
        assertTrue(prompt.contains("[bundle:verification:verification.rationale-sufficiency@1.0.0]"));
        assertFalse(prompt.toLowerCase().contains("calculus"));
        assertFalse(prompt.toLowerCase().contains("derivative"));
        assertFalse(prompt.toLowerCase().contains("polynomial"));
    }

    @Test
    void corroboratingProfileIsDistinctAndTwoApplicableJudgmentsOpenFreshIndependentTask() {
        EvaluationBundleStack stack = ReferenceBundles.counterexampleReviewStack();
        assertEquals(CounterexampleReviewProfile.FIXED_STACK, stack.pinnedIds());
        String prompt = new RationaleEvaluationPromptCompiler().compile(
                stack, CounterexampleReviewProfile.BASE_SYSTEM_PROMPT);
        assertTrue(prompt.contains("[bundle:evaluation:evaluation.counterexample-review@1.0.0]"));
        assertTrue(prompt.toLowerCase().contains("actively search for missing support"));
        assertFalse(prompt.toLowerCase().contains("calculus"));
        assertFalse(prompt.toLowerCase().contains("derivative"));
        assertFalse(prompt.toLowerCase().contains("polynomial"));

        ScriptedApplyGenerationModel generation = new ScriptedApplyGenerationModel(List.of(
                ApplyScriptData.taskReadyJson(),
                ApplyScriptData.taskReadyJson(
                        ApplyScriptData.INDEPENDENT_TASK_TEXT, ApplyScriptData.INDEPENDENT_EXPECTED_EXPRESSION)));
        ArtifactStore artifacts = new InMemoryArtifactStore(CLOCK);
        InMemoryLearningFlowStore flowStore = new InMemoryLearningFlowStore(CLOCK);
        AtomicInteger rationaleCalls = new AtomicInteger();
        List<String> contexts = new java.util.ArrayList<>();
        RationaleAssessmentPort rationalePort = (profile, compiledPrompt, contextJson) -> {
            rationaleCalls.incrementAndGet();
            contexts.add(contextJson);
            return RationaleEvaluationResult.applicable();
        };
        DiagnosticFlow flow = newDiagnosticFlow(
                artifacts, flowStore, generation,
                new ScriptedTaskVerifier(List.of(ApplyScriptData.passVerdict(), ApplyScriptData.passVerdict())),
                rationalePort);

        ApplyDeliveryResult.Delivered diagnostic = assertInstanceOf(
                ApplyDeliveryResult.Delivered.class, flow.startDiagnostic(FLOW_ID, ScriptedModelProfile.PROFILE));
        DiagnosticSubmissionResult.Passed passed = assertInstanceOf(
                DiagnosticSubmissionResult.Passed.class,
                flow.submitDiagnostic(FLOW_ID, ScriptedModelProfile.PROFILE,
                        diagnostic.attempt().attemptId(), ApplyScriptData.WRONG_DERIVATIVE,
                        ApplyScriptData.WRONG_DERIVATIVE, ApplyScriptData.APPLICABLE_RATIONALE));

        assertEquals(2, rationaleCalls.get());
        assertEquals(2, contexts.size());
        assertEquals(contexts.getFirst(), contexts.getLast(),
                "both isolated judgments must receive the same frozen rationale context");
        RationaleEvaluationContext context = RationaleEvaluationContext.parse(contexts.getFirst());
        assertEquals(ApplyScriptData.APPLICABLE_RATIONALE, context.rationale());
        assertFalse(contexts.getFirst().contains("primary_answer"));
        assertFalse(contexts.getFirst().contains("feedback"));
        assertEquals(ApplyScriptData.INDEPENDENT_TASK_TEXT, passed.independentLearnerProjection().taskText());
        assertTrue(flowStore.allEvidence().isEmpty());
        assertTrue(artifacts.findCommittedEvaluationResult(
                diagnostic.attempt().attemptId(), CommittedEvaluationResult.RATIONALE_ASSESSMENT,
                CommittedEvaluationResult.EVALUATION_VERSION).isPresent());
        assertTrue(artifacts.findCommittedEvaluationResult(
                diagnostic.attempt().attemptId(),
                CommittedEvaluationResult.RATIONALE_SUFFICIENCY_VERIFICATION,
                CommittedEvaluationResult.EVALUATION_VERSION).isPresent());
    }

    @Test
    void provenWrongDiagnosticWithInsufficientRationaleCallsOnlyFirstEvaluationAndFailsSafely() {
        ScriptedApplyGenerationModel generation = new ScriptedApplyGenerationModel(List.of(
                ApplyScriptData.taskReadyJson(), ApplyScriptData.taskReadyJson(
                        ApplyScriptData.INDEPENDENT_TASK_TEXT, ApplyScriptData.INDEPENDENT_EXPECTED_EXPRESSION)));
        ScriptedTaskVerifier verifier = new ScriptedTaskVerifier(List.of(
                ApplyScriptData.passVerdict(), ApplyScriptData.passVerdict()));
        ArtifactStore artifacts = new InMemoryArtifactStore(CLOCK);
        InMemoryLearningFlowStore flowStore = new InMemoryLearningFlowStore(CLOCK);
        AtomicInteger rationaleCalls = new AtomicInteger();
        AtomicInteger legacyAssessmentCalls = new AtomicInteger();
        RationaleAssessmentPort rationalePort = (profile, prompt, contextJson) -> {
            rationaleCalls.incrementAndGet();
            RationaleEvaluationContext context = RationaleEvaluationContext.parse(contextJson);
            assertEquals(ApplyScriptData.APPLICABLE_RATIONALE, context.rationale());
            assertEquals("canonical_expression", context.expectedAnswerFacts().kind());
            assertEquals(1, context.taskRubric().size());
            assertEquals(1, context.sourcePassages().size());
            assertEquals("zh-CN", context.learnerLocale());
            assertFalse(contextJson.contains("\"primary_answer\""));
            assertFalse(contextJson.contains("\"final_derivative\""));
            assertFalse(contextJson.contains("\"trusted_primary_answer_check\""));
            assertFalse(contextJson.contains("\"feedback\""));
            assertFalse(contextJson.contains("\"learning_state\""));
            assertFalse(contextJson.contains("\"generator_reasoning\""));
            return RationaleEvaluationResult.notApplicable(
                    List.of(RationaleEvaluationResult.ReasonCode.MATERIAL_GAP));
        };
        AssessmentPort legacyAssessment = (profile, context) -> {
            legacyAssessmentCalls.incrementAndGet();
            throw new AssertionError("legacy response assessment must not run");
        };
        ResponseVerificationPort verification = (profile, context) -> {
            throw new AssertionError("response verification must not run");
        };
        flowStore.attachAcceptedPlan(FLOW_ID, DiagnosticPlanFixture.acceptedPlan());
        DiagnosticFlow flow = new DiagnosticFlow(
                new cn.lunalhx.ai.kilnai.domain.apply.profile.ApplyProfileExecutor(
                        ReferenceBundles.stack(), generation, verifier, artifacts),
                artifacts,
                flowStore,
                legacyAssessment,
                verification,
                new RationaleEvaluationProfileExecutor(
                        ReferenceBundles.rationaleEvaluationStack(), rationalePort),
                new RationaleEvaluationProfileExecutor(
                        ReferenceBundles.counterexampleReviewStack(), rationalePort,
                        CounterexampleReviewProfile.BASE_SYSTEM_PROMPT),
                DiagnosticApplyFixture.diagnosticContext(),
                IndependentApplyFixture.independentContext(),
                CLOCK);

        ApplyDeliveryResult.Delivered diagnostic = assertInstanceOf(
                ApplyDeliveryResult.Delivered.class, flow.startDiagnostic(FLOW_ID, ScriptedModelProfile.PROFILE));
        DiagnosticSubmissionResult.Failed failed = assertInstanceOf(
                DiagnosticSubmissionResult.Failed.class,
                flow.submitDiagnostic(FLOW_ID, ScriptedModelProfile.PROFILE,
                        diagnostic.attempt().attemptId(),
                        ApplyScriptData.WRONG_DERIVATIVE,
                        ApplyScriptData.WRONG_DERIVATIVE,
                        ApplyScriptData.APPLICABLE_RATIONALE));

        assertEquals(1, rationaleCalls.get());
        assertEquals(0, legacyAssessmentCalls.get());
        assertTrue(failed.facts().errorDimensions().contains("material_gap"));
        assertTrue(flowStore.allEvidence().isEmpty());
        assertTrue(artifacts.findCommittedEvaluationResult(
                diagnostic.attempt().attemptId(),
                CommittedEvaluationResult.RATIONALE_ASSESSMENT,
                CommittedEvaluationResult.EVALUATION_VERSION).isPresent());
    }

    @Test
    void secondInconclusiveCorroborationBecomesUnconfirmed() {
        ScriptedApplyGenerationModel generation = new ScriptedApplyGenerationModel(List.of(
                ApplyScriptData.taskReadyJson()));
        ScriptedTaskVerifier verifier = new ScriptedTaskVerifier(List.of(ApplyScriptData.passVerdict()));
        ArtifactStore artifacts = new InMemoryArtifactStore(CLOCK);
        InMemoryLearningFlowStore flowStore = new InMemoryLearningFlowStore(CLOCK);
        AtomicInteger rationaleCalls = new AtomicInteger();
        RationaleAssessmentPort rationalePort = (profile, prompt, contextJson) -> {
            rationaleCalls.incrementAndGet();
            return prompt.contains("counterexample-review")
                    ? RationaleEvaluationResult.inconclusive()
                    : RationaleEvaluationResult.applicable();
        };
        flowStore.attachAcceptedPlan(FLOW_ID, DiagnosticPlanFixture.acceptedPlan());
        DiagnosticFlow flow = new DiagnosticFlow(
                new cn.lunalhx.ai.kilnai.domain.apply.profile.ApplyProfileExecutor(
                        ReferenceBundles.stack(), generation, verifier, artifacts),
                artifacts,
                flowStore,
                (profile, context) -> {
                    throw new AssertionError("legacy response assessment must not run");
                },
                (profile, context) -> {
                    throw new AssertionError("response verification must not run");
                },
                new RationaleEvaluationProfileExecutor(
                        ReferenceBundles.rationaleEvaluationStack(), rationalePort),
                new RationaleEvaluationProfileExecutor(
                        ReferenceBundles.counterexampleReviewStack(), rationalePort,
                        CounterexampleReviewProfile.BASE_SYSTEM_PROMPT),
                DiagnosticApplyFixture.diagnosticContext(),
                IndependentApplyFixture.independentContext(),
                CLOCK);

        ApplyDeliveryResult.Delivered diagnostic = assertInstanceOf(
                ApplyDeliveryResult.Delivered.class, flow.startDiagnostic(FLOW_ID, ScriptedModelProfile.PROFILE));
        DiagnosticSubmissionResult.Unconfirmed unconfirmed = assertInstanceOf(
                DiagnosticSubmissionResult.Unconfirmed.class,
                flow.submitDiagnostic(FLOW_ID, ScriptedModelProfile.PROFILE,
                        diagnostic.attempt().attemptId(),
                        ApplyScriptData.WRONG_DERIVATIVE,
                        ApplyScriptData.WRONG_DERIVATIVE,
                        ApplyScriptData.APPLICABLE_RATIONALE));

        assertEquals(2, rationaleCalls.get());
        assertEquals(List.of(), unconfirmed.facts().missingCriteria());
        assertEquals(List.of(), unconfirmed.facts().errorDimensions());
        assertEquals(1, generation.calls().size(), "an unconfirmed corroboration cannot prepare Independent");
        assertTrue(flowStore.allEvidence().isEmpty());
    }

    @Test
    void firstInconclusiveRationaleBecomesUnconfirmedWithoutASecondCall() {
        ScriptedApplyGenerationModel generation = new ScriptedApplyGenerationModel(List.of(
                ApplyScriptData.taskReadyJson()));
        ScriptedTaskVerifier verifier = new ScriptedTaskVerifier(List.of(ApplyScriptData.passVerdict()));
        ArtifactStore artifacts = new InMemoryArtifactStore(CLOCK);
        InMemoryLearningFlowStore flowStore = new InMemoryLearningFlowStore(CLOCK);
        AtomicInteger rationaleCalls = new AtomicInteger();
        DiagnosticFlow flow = newDiagnosticFlow(
                artifacts, flowStore, generation, verifier, (profile, prompt, contextJson) -> {
                    rationaleCalls.incrementAndGet();
                    return RationaleEvaluationResult.inconclusive();
                });

        ApplyDeliveryResult.Delivered diagnostic = assertInstanceOf(
                ApplyDeliveryResult.Delivered.class, flow.startDiagnostic(FLOW_ID, ScriptedModelProfile.PROFILE));
        DiagnosticSubmissionResult.Unconfirmed unconfirmed = assertInstanceOf(
                DiagnosticSubmissionResult.Unconfirmed.class,
                flow.submitDiagnostic(FLOW_ID, ScriptedModelProfile.PROFILE,
                        diagnostic.attempt().attemptId(), ApplyScriptData.WRONG_DERIVATIVE,
                        ApplyScriptData.WRONG_DERIVATIVE, ApplyScriptData.APPLICABLE_RATIONALE));

        assertEquals(1, rationaleCalls.get());
        assertEquals(List.of(), unconfirmed.facts().missingCriteria());
        assertEquals(List.of(), unconfirmed.facts().errorDimensions());
        assertEquals(1, generation.calls().size());
        assertTrue(flowStore.allEvidence().isEmpty());
    }

    @Test
    void secondNotApplicableCorroborationBecomesUnconfirmedWithNeutralFacts() {
        ScriptedApplyGenerationModel generation = new ScriptedApplyGenerationModel(List.of(
                ApplyScriptData.taskReadyJson()));
        ScriptedTaskVerifier verifier = new ScriptedTaskVerifier(List.of(ApplyScriptData.passVerdict()));
        ArtifactStore artifacts = new InMemoryArtifactStore(CLOCK);
        InMemoryLearningFlowStore flowStore = new InMemoryLearningFlowStore(CLOCK);
        AtomicInteger rationaleCalls = new AtomicInteger();
        DiagnosticFlow flow = newDiagnosticFlow(
                artifacts, flowStore, generation, verifier, (profile, prompt, contextJson) -> {
                    rationaleCalls.incrementAndGet();
                    return prompt.contains("counterexample-review")
                            ? RationaleEvaluationResult.notApplicable(
                            List.of(RationaleEvaluationResult.ReasonCode.MATERIAL_GAP))
                            : RationaleEvaluationResult.applicable();
                });

        ApplyDeliveryResult.Delivered diagnostic = assertInstanceOf(
                ApplyDeliveryResult.Delivered.class, flow.startDiagnostic(FLOW_ID, ScriptedModelProfile.PROFILE));
        DiagnosticSubmissionResult.Unconfirmed unconfirmed = assertInstanceOf(
                DiagnosticSubmissionResult.Unconfirmed.class,
                flow.submitDiagnostic(FLOW_ID, ScriptedModelProfile.PROFILE,
                        diagnostic.attempt().attemptId(), ApplyScriptData.WRONG_DERIVATIVE,
                        ApplyScriptData.WRONG_DERIVATIVE, ApplyScriptData.APPLICABLE_RATIONALE));

        assertEquals(2, rationaleCalls.get());
        assertEquals(List.of(), unconfirmed.facts().missingCriteria());
        assertEquals(List.of(), unconfirmed.facts().errorDimensions());
        assertEquals(1, generation.calls().size());
        assertTrue(flowStore.allEvidence().isEmpty());
    }

    @Test
    void eachRationaleResponsibilityGetsAtMostOneContractRepairBeforeTwoApplicablePasses() {
        ScriptedApplyGenerationModel generation = new ScriptedApplyGenerationModel(List.of(
                ApplyScriptData.taskReadyJson(), ApplyScriptData.taskReadyJson(
                        ApplyScriptData.INDEPENDENT_TASK_TEXT, ApplyScriptData.INDEPENDENT_EXPECTED_EXPRESSION)));
        ScriptedTaskVerifier verifier = new ScriptedTaskVerifier(List.of(
                ApplyScriptData.passVerdict(), ApplyScriptData.passVerdict()));
        ArtifactStore artifacts = new InMemoryArtifactStore(CLOCK);
        InMemoryLearningFlowStore flowStore = new InMemoryLearningFlowStore(CLOCK);
        AtomicInteger firstCalls = new AtomicInteger();
        AtomicInteger secondCalls = new AtomicInteger();
        AtomicInteger totalCalls = new AtomicInteger();
        RationaleAssessmentPort rationalePort = (profile, prompt, contextJson) -> {
            totalCalls.incrementAndGet();
            if (prompt.contains("counterexample-review")) {
                if (secondCalls.getAndIncrement() == 0) {
                    throw new ModelContractInvalidException(List.of("unknown_field"));
                }
            } else if (firstCalls.getAndIncrement() == 0) {
                throw new ModelContractInvalidException(List.of("unknown_field"));
            }
            return RationaleEvaluationResult.applicable();
        };
        DiagnosticFlow flow = newDiagnosticFlow(artifacts, flowStore, generation, verifier, rationalePort);

        ApplyDeliveryResult.Delivered diagnostic = assertInstanceOf(
                ApplyDeliveryResult.Delivered.class, flow.startDiagnostic(FLOW_ID, ScriptedModelProfile.PROFILE));
        DiagnosticSubmissionResult.Passed passed = assertInstanceOf(
                DiagnosticSubmissionResult.Passed.class,
                flow.submitDiagnostic(FLOW_ID, ScriptedModelProfile.PROFILE,
                        diagnostic.attempt().attemptId(), ApplyScriptData.WRONG_DERIVATIVE,
                        ApplyScriptData.WRONG_DERIVATIVE, ApplyScriptData.APPLICABLE_RATIONALE));

        assertEquals(4, totalCalls.get());
        assertEquals(ApplyScriptData.INDEPENDENT_TASK_TEXT, passed.independentLearnerProjection().taskText());
        assertTrue(artifacts.findCommittedEvaluationResult(
                diagnostic.attempt().attemptId(), CommittedEvaluationResult.RATIONALE_ASSESSMENT,
                CommittedEvaluationResult.EVALUATION_VERSION).isPresent());
        assertTrue(artifacts.findCommittedEvaluationResult(
                diagnostic.attempt().attemptId(),
                CommittedEvaluationResult.RATIONALE_SUFFICIENCY_VERIFICATION,
                CommittedEvaluationResult.EVALUATION_VERSION).isPresent());
    }

    @Test
    void malformedRationaleResultGetsOneSameContextRepairWithNormalizedViolations() {
        ScriptedApplyGenerationModel generation = new ScriptedApplyGenerationModel(List.of(
                ApplyScriptData.taskReadyJson()));
        ScriptedTaskVerifier verifier = new ScriptedTaskVerifier(List.of(ApplyScriptData.passVerdict()));
        ArtifactStore artifacts = new InMemoryArtifactStore(CLOCK);
        InMemoryLearningFlowStore flowStore = new InMemoryLearningFlowStore(CLOCK);
        AtomicInteger rationaleCalls = new AtomicInteger();
        List<String> prompts = new java.util.ArrayList<>();
        RationaleAssessmentPort rationalePort = (profile, prompt, contextJson) -> {
            prompts.add(prompt);
            if (rationaleCalls.getAndIncrement() == 0) {
                throw new ModelContractInvalidException(List.of("unknown_field"));
            }
            return RationaleEvaluationResult.notApplicable(
                    List.of(RationaleEvaluationResult.ReasonCode.MATERIAL_GAP));
        };
        DiagnosticFlow flow = newDiagnosticFlow(
                artifacts, flowStore, generation, verifier, rationalePort);

        ApplyDeliveryResult.Delivered diagnostic = assertInstanceOf(
                ApplyDeliveryResult.Delivered.class, flow.startDiagnostic(FLOW_ID, ScriptedModelProfile.PROFILE));
        DiagnosticSubmissionResult.Failed failed = assertInstanceOf(
                DiagnosticSubmissionResult.Failed.class,
                flow.submitDiagnostic(FLOW_ID, ScriptedModelProfile.PROFILE,
                        diagnostic.attempt().attemptId(), ApplyScriptData.WRONG_DERIVATIVE,
                        ApplyScriptData.WRONG_DERIVATIVE, ApplyScriptData.APPLICABLE_RATIONALE));

        assertEquals(2, rationaleCalls.get());
        assertEquals(2, prompts.size());
        assertFalse(prompts.get(0).contains("unknown_field"));
        assertTrue(prompts.get(1).contains("unknown_field"));
        assertTrue(failed.facts().errorDimensions().contains("material_gap"));
        assertTrue(artifacts.findCommittedEvaluationResult(
                diagnostic.attempt().attemptId(), CommittedEvaluationResult.RATIONALE_ASSESSMENT,
                CommittedEvaluationResult.EVALUATION_VERSION).isPresent());
    }

    @Test
    void repeatedMalformedRationaleResultBecomesPostSubmissionUnavailable() {
        ScriptedApplyGenerationModel generation = new ScriptedApplyGenerationModel(List.of(
                ApplyScriptData.taskReadyJson()));
        ScriptedTaskVerifier verifier = new ScriptedTaskVerifier(List.of(ApplyScriptData.passVerdict()));
        ArtifactStore artifacts = new InMemoryArtifactStore(CLOCK);
        InMemoryLearningFlowStore flowStore = new InMemoryLearningFlowStore(CLOCK);
        AtomicInteger rationaleCalls = new AtomicInteger();
        RationaleAssessmentPort rationalePort = (profile, prompt, contextJson) -> {
            rationaleCalls.incrementAndGet();
            throw new ModelContractInvalidException(List.of("unknown_field"));
        };
        DiagnosticFlow flow = newDiagnosticFlow(
                artifacts, flowStore, generation, verifier, rationalePort);

        ApplyDeliveryResult.Delivered diagnostic = assertInstanceOf(
                ApplyDeliveryResult.Delivered.class, flow.startDiagnostic(FLOW_ID, ScriptedModelProfile.PROFILE));
        assertThrows(PostSubmissionEvaluationUnavailableException.class, () ->
                flow.submitDiagnostic(FLOW_ID, ScriptedModelProfile.PROFILE,
                        diagnostic.attempt().attemptId(), ApplyScriptData.WRONG_DERIVATIVE,
                        ApplyScriptData.WRONG_DERIVATIVE, ApplyScriptData.APPLICABLE_RATIONALE));

        assertEquals(2, rationaleCalls.get());
        assertTrue(artifacts.findCommittedEvaluationResult(
                diagnostic.attempt().attemptId(), CommittedEvaluationResult.RATIONALE_ASSESSMENT,
                CommittedEvaluationResult.EVALUATION_VERSION).isEmpty());
    }

    private DiagnosticFlow newDiagnosticFlow(
            ArtifactStore artifacts,
            InMemoryLearningFlowStore flowStore,
            ScriptedApplyGenerationModel generation,
            ScriptedTaskVerifier verifier,
            RationaleAssessmentPort rationalePort
    ) {
        flowStore.attachAcceptedPlan(FLOW_ID, DiagnosticPlanFixture.acceptedPlan());
        return new DiagnosticFlow(
                new cn.lunalhx.ai.kilnai.domain.apply.profile.ApplyProfileExecutor(
                        ReferenceBundles.stack(), generation, verifier, artifacts),
                artifacts,
                flowStore,
                (profile, context) -> {
                    throw new AssertionError("legacy response assessment must not run");
                },
                (profile, context) -> {
                    throw new AssertionError("response verification must not run");
                },
                new RationaleEvaluationProfileExecutor(
                        ReferenceBundles.rationaleEvaluationStack(), rationalePort),
                new RationaleEvaluationProfileExecutor(
                        ReferenceBundles.counterexampleReviewStack(), rationalePort,
                        CounterexampleReviewProfile.BASE_SYSTEM_PROMPT),
                DiagnosticApplyFixture.diagnosticContext(),
                IndependentApplyFixture.independentContext(),
                CLOCK);
    }
}
