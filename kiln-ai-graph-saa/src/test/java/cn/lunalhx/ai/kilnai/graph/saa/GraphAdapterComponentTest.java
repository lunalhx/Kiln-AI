package cn.lunalhx.ai.kilnai.graph.saa;

import cn.lunalhx.ai.kilnai.application.fake.ScriptedAssessmentModel;
import cn.lunalhx.ai.kilnai.application.fake.ScriptedPedagogyModel;
import cn.lunalhx.ai.kilnai.application.fake.ScriptedScenario;
import cn.lunalhx.ai.kilnai.application.fake.ScriptedTeachingModel;
import cn.lunalhx.ai.kilnai.application.fixture.SpikeFixture;
import cn.lunalhx.ai.kilnai.application.graph.LearnerVisibleInteraction;
import cn.lunalhx.ai.kilnai.application.graph.PublicTraceView;
import cn.lunalhx.ai.kilnai.application.graph.ResumeGraphRun;
import cn.lunalhx.ai.kilnai.application.graph.StartGraphRun;
import cn.lunalhx.ai.kilnai.application.kernel.LearningNodeKernel;
import cn.lunalhx.ai.kilnai.application.kernel.PendingCommitBuffer;
import cn.lunalhx.ai.kilnai.application.kernel.PendingLearnerEventHolder;
import cn.lunalhx.ai.kilnai.application.port.SpikeStorePort;
import cn.lunalhx.ai.kilnai.application.store.InMemorySpikeStore;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.FlowStatus;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.LearnerInputKind;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.LearningStage;
import cn.lunalhx.ai.kilnai.types.error.ApplicationException;
import cn.lunalhx.ai.kilnai.types.error.ErrorCode;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GraphAdapterComponentTest {

    @Test
    void startDoesNotCallPedagogyAndResumeFromNewInstanceCompletesApplyAndAssessment() {
        InMemorySpikeStore store = new InMemorySpikeStore();
        ScriptedPedagogyModel pedagogy = new ScriptedPedagogyModel(ScriptedScenario.HAPPY);
        UUID learnerId = UUID.randomUUID();
        UUID flowId = UUID.randomUUID();
        store.insertFlow(new SpikeStorePort.FlowRecord(
                flowId, learnerId, SpikeFixture.CONCEPT_ID, SpikeFixture.CONTRACT_ID,
                SpikeFixture.RUBRIC_ID, SpikeFixture.SOURCE_PACK_ID, FlowStatus.READY,
                LearningStage.LEARNING_AND_PRACTICE, Clock.systemUTC().instant()
        ));

        SpringAiAlibabaGraphRuntime first = runtime(store, pedagogy, ScriptedScenario.HAPPY);
        LearnerVisibleInteraction explained = first.start(new StartGraphRun(
                learnerId, SpikeFixture.PERCENT_CHANGE_V1, UUID.randomUUID(), flowId
        ));

        assertEquals(0, pedagogy.calls());
        assertEquals(FlowStatus.AWAITING_LEARNER_INPUT, explained.status());
        assertTrue(explained.visibleContent().contains("Percent change"));
        assertFalse(explained.visibleContent().contains("do not expose"));
        assertFalse(store.latest(flowId).orElseThrow().blackboard().visibleContent().contains("25"));
        assertFalse(String.valueOf(store.privateTrace(flowId).orElseThrow()).contains("never-persisted-in-framework"));

        SpringAiAlibabaGraphRuntime second = runtime(store, pedagogy, ScriptedScenario.HAPPY);
        LearnerVisibleInteraction practice = second.resume(new ResumeGraphRun(
                flowId, UUID.randomUUID(), explained.interactionVersion(),
                LearnerInputKind.CONTINUE_REQUESTED, null
        ));

        assertEquals(1, pedagogy.calls());
        assertTrue(practice.visibleContent().contains("80 to 100"));
        assertFalse(practice.visibleContent().contains("25"));
        assertTrue(practice.allowedEventKinds().contains(LearnerInputKind.ANSWER_SUBMITTED));

        SpringAiAlibabaGraphRuntime third = runtime(store, pedagogy, ScriptedScenario.HAPPY);
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
        LearnerVisibleInteraction practice = continueAfterExplain(ScriptedScenario.REPAIRABLE_ONCE);
        assertTrue(practice.visibleContent().contains("80 to 100"));
        assertTrue(practice.allowedEventKinds().contains(LearnerInputKind.ANSWER_SUBMITTED));
    }

    @Test
    void rejectedApplyKeepsPreviousCheckpoint() {
        FailureCase failure = failOnContinue(ScriptedScenario.REJECTED);
        assertEquals(ErrorCode.SERVICE_UNAVAILABLE, failure.error().errorCode());
        assertEquals(failure.explained().interactionVersion(), failure.store().latestInteraction(failure.flowId()).orElseThrow().interactionVersion());
        assertFalse(failure.store().latest(failure.flowId()).orElseThrow().blackboard().visibleContent().contains("answer is 25"));
    }

    @Test
    void capabilityGapOnApplyKeepsPreviousCheckpoint() {
        FailureCase failure = failOnContinue(ScriptedScenario.CAPABILITY_GAP);
        assertEquals(ErrorCode.SERVICE_UNAVAILABLE, failure.error().errorCode());
        assertEquals(FlowStatus.AWAITING_LEARNER_INPUT, failure.store().latestInteraction(failure.flowId()).orElseThrow().status());
        assertTrue(failure.store().latestInteraction(failure.flowId()).orElseThrow().visibleContent().contains("Percent change"));
    }

    @Test
    void budgetExhaustionOnContinueKeepsPreviousCheckpoint() {
        FailureCase failure = failOnContinue(ScriptedScenario.BUDGET_EXHAUSTION);
        assertEquals(ErrorCode.SERVICE_UNAVAILABLE, failure.error().errorCode());
        assertEquals(failure.explained().visibleContent(), failure.store().latestInteraction(failure.flowId()).orElseThrow().visibleContent());
    }

    private LearnerVisibleInteraction continueAfterExplain(ScriptedScenario scenario) {
        InMemorySpikeStore store = new InMemorySpikeStore();
        ScriptedPedagogyModel pedagogy = new ScriptedPedagogyModel(scenario);
        UUID learnerId = UUID.randomUUID();
        UUID flowId = UUID.randomUUID();
        store.insertFlow(new SpikeStorePort.FlowRecord(
                flowId, learnerId, SpikeFixture.CONCEPT_ID, SpikeFixture.CONTRACT_ID,
                SpikeFixture.RUBRIC_ID, SpikeFixture.SOURCE_PACK_ID, FlowStatus.READY,
                LearningStage.LEARNING_AND_PRACTICE, Clock.systemUTC().instant()
        ));
        SpringAiAlibabaGraphRuntime first = runtime(store, pedagogy, scenario);
        LearnerVisibleInteraction explained = first.start(new StartGraphRun(
                learnerId, SpikeFixture.PERCENT_CHANGE_V1, UUID.randomUUID(), flowId
        ));
        return runtime(store, pedagogy, scenario).resume(new ResumeGraphRun(
                flowId, UUID.randomUUID(), explained.interactionVersion(),
                LearnerInputKind.CONTINUE_REQUESTED, null
        ));
    }

    private FailureCase failOnContinue(ScriptedScenario scenario) {
        InMemorySpikeStore store = new InMemorySpikeStore();
        ScriptedPedagogyModel pedagogy = new ScriptedPedagogyModel(scenario);
        UUID learnerId = UUID.randomUUID();
        UUID flowId = UUID.randomUUID();
        store.insertFlow(new SpikeStorePort.FlowRecord(
                flowId, learnerId, SpikeFixture.CONCEPT_ID, SpikeFixture.CONTRACT_ID,
                SpikeFixture.RUBRIC_ID, SpikeFixture.SOURCE_PACK_ID, FlowStatus.READY,
                LearningStage.LEARNING_AND_PRACTICE, Clock.systemUTC().instant()
        ));
        SpringAiAlibabaGraphRuntime first = runtime(store, pedagogy, scenario);
        LearnerVisibleInteraction explained = first.start(new StartGraphRun(
                learnerId, SpikeFixture.PERCENT_CHANGE_V1, UUID.randomUUID(), flowId
        ));
        ApplicationException error = org.junit.jupiter.api.Assertions.assertThrows(
                ApplicationException.class,
                () -> runtime(store, pedagogy, scenario).resume(new ResumeGraphRun(
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
                LearningStage.LEARNING_AND_PRACTICE, Clock.systemUTC().instant()
        ));
        SpringAiAlibabaGraphRuntime first = runtime(store, pedagogy, ScriptedScenario.ILLEGAL_PEDAGOGY);
        LearnerVisibleInteraction explained = first.start(new StartGraphRun(
                learnerId, SpikeFixture.PERCENT_CHANGE_V1, UUID.randomUUID(), flowId
        ));
        SpringAiAlibabaGraphRuntime second = runtime(store, pedagogy, ScriptedScenario.ILLEGAL_PEDAGOGY);
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
            ScriptedScenario scenario
    ) {
        PendingCommitBuffer buffer = new PendingCommitBuffer();
        PendingLearnerEventHolder events = new PendingLearnerEventHolder();
        LearningBlackboardMapper mapper = new LearningBlackboardMapper();
        LearningNodeKernel kernel = new LearningNodeKernel(
                buffer, pedagogy, new ScriptedTeachingModel(scenario), new ScriptedAssessmentModel(),
                store, scenario, true, Clock.systemUTC()
        );
        ApplicationCheckpointSaver saver = new ApplicationCheckpointSaver(store, buffer, mapper, Clock.systemUTC());
        LearningStateGraphFactory factory = new LearningStateGraphFactory(kernel, events, mapper, saver);
        return new SpringAiAlibabaGraphRuntime(store, events, mapper, factory);
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
