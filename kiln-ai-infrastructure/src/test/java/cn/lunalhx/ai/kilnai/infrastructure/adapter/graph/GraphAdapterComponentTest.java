package cn.lunalhx.ai.kilnai.infrastructure.adapter.graph;

import cn.lunalhx.ai.kilnai.domain.learning.adapter.port.SpikeStorePort;
import cn.lunalhx.ai.kilnai.domain.learning.fake.SkippingModelProfilePort;
import cn.lunalhx.ai.kilnai.domain.learning.fake.ScriptedAssessmentModel;
import cn.lunalhx.ai.kilnai.domain.learning.fake.ScriptedPedagogyModel;
import cn.lunalhx.ai.kilnai.domain.learning.fake.ScriptedScenario;
import cn.lunalhx.ai.kilnai.domain.learning.fake.ScriptedTeachingModel;
import cn.lunalhx.ai.kilnai.domain.learning.fixture.SpikeFixture;
import cn.lunalhx.ai.kilnai.domain.learning.kernel.GraphRunBudgetHolder;
import cn.lunalhx.ai.kilnai.domain.learning.kernel.LearningNodeKernel;
import cn.lunalhx.ai.kilnai.domain.learning.kernel.PendingCommitBuffer;
import cn.lunalhx.ai.kilnai.domain.learning.kernel.PendingLearnerEventHolder;
import cn.lunalhx.ai.kilnai.domain.learning.model.LearnerVisibleInteraction;
import cn.lunalhx.ai.kilnai.domain.learning.model.PublicTraceView;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.FlowStatus;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.LearnerInputKind;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.LearningStage;
import cn.lunalhx.ai.kilnai.domain.learning.service.ResumeGraphRun;
import cn.lunalhx.ai.kilnai.domain.learning.service.StartGraphRun;
import cn.lunalhx.ai.kilnai.infrastructure.adapter.repository.InMemorySpikeStore;
import cn.lunalhx.ai.kilnai.types.error.ApplicationException;
import cn.lunalhx.ai.kilnai.types.error.ErrorCode;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GraphAdapterComponentTest {

    private static final int TOOL_LIMIT = 8;

    @Test
    void startDoesNotCallPedagogyAndResumeFromNewInstanceCompletesApplyAndAssessment() {
        InMemorySpikeStore store = new InMemorySpikeStore();
        ScriptedPedagogyModel pedagogy = new ScriptedPedagogyModel(ScriptedScenario.HAPPY);
        UUID learnerId = UUID.randomUUID();
        UUID flowId = UUID.randomUUID();
        store.insertFlow(new SpikeStorePort.FlowRecord(
                flowId, learnerId, SpikeFixture.CONCEPT_ID, SpikeFixture.CONTRACT_ID,
                SpikeFixture.RUBRIC_ID, SpikeFixture.SOURCE_PACK_ID, FlowStatus.READY,
                LearningStage.LEARNING_AND_PRACTICE, Clock.systemUTC().instant(), SkippingModelProfilePort.SNAPSHOT
        ));

        SpringAiAlibabaGraphRuntime first = runtime(store, pedagogy, ScriptedScenario.HAPPY, true);
        LearnerVisibleInteraction explained = first.start(new StartGraphRun(
                learnerId, SpikeFixture.PERCENT_CHANGE_V1, UUID.randomUUID(), flowId
        ));

        assertEquals(0, pedagogy.calls());
        assertEquals(FlowStatus.AWAITING_LEARNER_INPUT, explained.status());
        assertTrue(explained.visibleContent().contains("Percent change"));
        assertFalse(explained.visibleContent().contains("do not expose"));
        assertFalse(store.latest(flowId).orElseThrow().blackboard().visibleContent().contains("25"));
        assertFalse(String.valueOf(store.privateTrace(flowId).orElseThrow()).contains("never-persisted-in-framework"));

        SpringAiAlibabaGraphRuntime second = runtime(store, pedagogy, ScriptedScenario.HAPPY, true);
        LearnerVisibleInteraction practice = second.resume(new ResumeGraphRun(
                flowId, UUID.randomUUID(), explained.interactionVersion(),
                LearnerInputKind.CONTINUE_REQUESTED, null
        ));

        assertEquals(1, pedagogy.calls());
        assertTrue(practice.visibleContent().contains("80 to 100"));
        assertFalse(practice.visibleContent().contains("25"));
        assertTrue(practice.allowedEventKinds().contains(LearnerInputKind.ANSWER_SUBMITTED));

        SpringAiAlibabaGraphRuntime third = runtime(store, pedagogy, ScriptedScenario.HAPPY, true);
        LearnerVisibleInteraction terminal = third.resume(new ResumeGraphRun(
                flowId, UUID.randomUUID(), practice.interactionVersion(),
                LearnerInputKind.ANSWER_SUBMITTED, "25"
        ));

        assertEquals(FlowStatus.TERMINAL, terminal.status());
        assertTrue(terminal.visibleContent().contains("LEARNING"));
        assertTrue(store.evidenceExists(store.latest(flowId).orElseThrow().blackboard().openAttemptId()));
        PublicTraceView trace = store.publicTrace(flowId).orElseThrow();
        assertTrue(trace.selectedSkills().contains("explain.direct@1"));
        assertTrue(trace.selectedSkills().contains("apply.worked-example@1"));
        assertTrue(trace.selectedSkills().contains("capability.quantitative@1"));
        PublicTraceAssertions.assertNoPrivateFields(terminal.visibleContent());
        PublicTraceAssertions.assertNoPrivateFields(trace.toString());
    }

    @Test
    void repairableApplyCandidateIsRepairedOnceThenAccepted() {
        LearnerVisibleInteraction practice = continueAfterExplain(ScriptedScenario.REPAIRABLE_ONCE, true);
        assertTrue(practice.visibleContent().contains("80 to 100"));
        assertTrue(practice.allowedEventKinds().contains(LearnerInputKind.ANSWER_SUBMITTED));
    }

    @Test
    void rejectedApplyKeepsPreviousCheckpoint() {
        FailureCase failure = failOnContinue(ScriptedScenario.REJECTED, true, 3);
        assertEquals(ErrorCode.SERVICE_UNAVAILABLE, failure.error().errorCode());
        assertEquals(failure.explained().interactionVersion(), failure.store().latestInteraction(failure.flowId()).orElseThrow().interactionVersion());
        assertFalse(failure.store().latest(failure.flowId()).orElseThrow().blackboard().visibleContent().contains("answer is 25"));
    }

    @Test
    void capabilityGapOnApplyKeepsPreviousCheckpoint() {
        FailureCase failure = failOnContinue(ScriptedScenario.HAPPY, false, 3);
        assertEquals(ErrorCode.SERVICE_UNAVAILABLE, failure.error().errorCode());
        assertEquals(FlowStatus.AWAITING_LEARNER_INPUT, failure.store().latestInteraction(failure.flowId()).orElseThrow().status());
        assertTrue(failure.store().latestInteraction(failure.flowId()).orElseThrow().visibleContent().contains("Percent change"));
    }

    @Test
    void budgetExhaustionOnContinueKeepsPreviousCheckpoint() {
        FailureCase failure = failOnContinue(ScriptedScenario.HAPPY, true, 1);
        assertEquals(ErrorCode.SERVICE_UNAVAILABLE, failure.error().errorCode());
        assertEquals(failure.explained().visibleContent(), failure.store().latestInteraction(failure.flowId()).orElseThrow().visibleContent());
    }

    private LearnerVisibleInteraction continueAfterExplain(ScriptedScenario scenario, boolean calculatorAvailable) {
        InMemorySpikeStore store = new InMemorySpikeStore();
        ScriptedPedagogyModel pedagogy = new ScriptedPedagogyModel(scenario);
        UUID learnerId = UUID.randomUUID();
        UUID flowId = UUID.randomUUID();
        store.insertFlow(new SpikeStorePort.FlowRecord(
                flowId, learnerId, SpikeFixture.CONCEPT_ID, SpikeFixture.CONTRACT_ID,
                SpikeFixture.RUBRIC_ID, SpikeFixture.SOURCE_PACK_ID, FlowStatus.READY,
                LearningStage.LEARNING_AND_PRACTICE, Clock.systemUTC().instant(), SkippingModelProfilePort.SNAPSHOT
        ));
        SpringAiAlibabaGraphRuntime first = runtime(store, pedagogy, scenario, calculatorAvailable);
        LearnerVisibleInteraction explained = first.start(new StartGraphRun(
                learnerId, SpikeFixture.PERCENT_CHANGE_V1, UUID.randomUUID(), flowId
        ));
        return runtime(store, pedagogy, scenario, calculatorAvailable).resume(new ResumeGraphRun(
                flowId, UUID.randomUUID(), explained.interactionVersion(),
                LearnerInputKind.CONTINUE_REQUESTED, null
        ));
    }

    private FailureCase failOnContinue(ScriptedScenario scenario, boolean calculatorAvailable, int continueNodeLimit) {
        InMemorySpikeStore store = new InMemorySpikeStore();
        ScriptedPedagogyModel pedagogy = new ScriptedPedagogyModel(scenario);
        UUID learnerId = UUID.randomUUID();
        UUID flowId = UUID.randomUUID();
        store.insertFlow(new SpikeStorePort.FlowRecord(
                flowId, learnerId, SpikeFixture.CONCEPT_ID, SpikeFixture.CONTRACT_ID,
                SpikeFixture.RUBRIC_ID, SpikeFixture.SOURCE_PACK_ID, FlowStatus.READY,
                LearningStage.LEARNING_AND_PRACTICE, Clock.systemUTC().instant(), SkippingModelProfilePort.SNAPSHOT
        ));
        SpringAiAlibabaGraphRuntime first = runtime(store, pedagogy, scenario, calculatorAvailable);
        LearnerVisibleInteraction explained = first.start(new StartGraphRun(
                learnerId, SpikeFixture.PERCENT_CHANGE_V1, UUID.randomUUID(), flowId
        ));
        ApplicationException error = org.junit.jupiter.api.Assertions.assertThrows(
                ApplicationException.class,
                () -> runtime(store, pedagogy, scenario, calculatorAvailable, continueNodeLimit).resume(new ResumeGraphRun(
                        flowId, UUID.randomUUID(), explained.interactionVersion(),
                        LearnerInputKind.CONTINUE_REQUESTED, null
                ))
        );
        return new FailureCase(store, flowId, explained, error);
    }

    private record FailureCase(
            InMemorySpikeStore store,
            UUID flowId,
            LearnerVisibleInteraction explained,
            ApplicationException error
    ) {
    }

    @Test
    void illegalPedagogyPlanFallsBackToExplain() {
        InMemorySpikeStore store = new InMemorySpikeStore();
        ScriptedPedagogyModel pedagogy = new ScriptedPedagogyModel(ScriptedScenario.ILLEGAL_PEDAGOGY);
        UUID learnerId = UUID.randomUUID();
        UUID flowId = UUID.randomUUID();
        store.insertFlow(new SpikeStorePort.FlowRecord(
                flowId, learnerId, SpikeFixture.CONCEPT_ID, SpikeFixture.CONTRACT_ID,
                SpikeFixture.RUBRIC_ID, SpikeFixture.SOURCE_PACK_ID, FlowStatus.READY,
                LearningStage.LEARNING_AND_PRACTICE, Clock.systemUTC().instant(), SkippingModelProfilePort.SNAPSHOT
        ));
        SpringAiAlibabaGraphRuntime first = runtime(store, pedagogy, ScriptedScenario.ILLEGAL_PEDAGOGY, true);
        LearnerVisibleInteraction explained = first.start(new StartGraphRun(
                learnerId, SpikeFixture.PERCENT_CHANGE_V1, UUID.randomUUID(), flowId
        ));
        SpringAiAlibabaGraphRuntime second = runtime(store, pedagogy, ScriptedScenario.ILLEGAL_PEDAGOGY, true);
        LearnerVisibleInteraction again = second.resume(new ResumeGraphRun(
                flowId, UUID.randomUUID(), explained.interactionVersion(),
                LearnerInputKind.CONTINUE_REQUESTED, null
        ));
        assertTrue(again.visibleContent().contains("Percent change"));
        assertFalse(again.allowedEventKinds().contains(LearnerInputKind.ANSWER_SUBMITTED));
    }

    private SpringAiAlibabaGraphRuntime runtime(
            InMemorySpikeStore store,
            ScriptedPedagogyModel pedagogy,
            ScriptedScenario scenario,
            boolean calculatorAvailable
    ) {
        return runtime(store, pedagogy, scenario, calculatorAvailable, 3);
    }

    private SpringAiAlibabaGraphRuntime runtime(
            InMemorySpikeStore store,
            ScriptedPedagogyModel pedagogy,
            ScriptedScenario scenario,
            boolean calculatorAvailable,
            int nodeLimit
    ) {
        PendingCommitBuffer buffer = new PendingCommitBuffer();
        GraphRunBudgetHolder budgets = new GraphRunBudgetHolder();
        PendingLearnerEventHolder events = new PendingLearnerEventHolder();
        LearningBlackboardMapper mapper = new LearningBlackboardMapper();
        LearningNodeKernel kernel = new LearningNodeKernel(
                buffer, budgets, pedagogy, new ScriptedTeachingModel(scenario), new ScriptedAssessmentModel(),
                store, calculatorAvailable, Clock.systemUTC()
        );
        ApplicationCheckpointSaver saver = new ApplicationCheckpointSaver(store, buffer, mapper, Clock.systemUTC());
        LearningStateGraphFactory factory = new LearningStateGraphFactory(kernel, events, mapper, saver);
        return new SpringAiAlibabaGraphRuntime(store, events, mapper, factory, budgets, nodeLimit, TOOL_LIMIT);
    }

    private static final class PublicTraceAssertions {
        private static void assertNoPrivateFields(String text) {
            assertFalse(text.contains("answerKey"));
            assertFalse(text.contains("hiddenReasoning"));
            assertFalse(text.contains("internal-apply-trace"));
            assertFalse(text.contains("internal-explain-trace"));
        }
    }
}
