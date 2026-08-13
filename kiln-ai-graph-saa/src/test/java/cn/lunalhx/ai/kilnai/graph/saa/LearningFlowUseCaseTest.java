package cn.lunalhx.ai.kilnai.graph.saa;

import cn.lunalhx.ai.kilnai.application.fake.ScriptedAssessmentModel;
import cn.lunalhx.ai.kilnai.application.fake.ScriptedPedagogyModel;
import cn.lunalhx.ai.kilnai.application.fake.ScriptedScenario;
import cn.lunalhx.ai.kilnai.application.fake.ScriptedTeachingModel;
import cn.lunalhx.ai.kilnai.application.fixture.SpikeFixture;
import cn.lunalhx.ai.kilnai.application.graph.LearnerVisibleInteraction;
import cn.lunalhx.ai.kilnai.application.graph.ResumeGraphRun;
import cn.lunalhx.ai.kilnai.application.graph.StartGraphRun;
import cn.lunalhx.ai.kilnai.application.kernel.LearningNodeKernel;
import cn.lunalhx.ai.kilnai.application.kernel.PendingCommandHolder;
import cn.lunalhx.ai.kilnai.application.kernel.PendingCommitBuffer;
import cn.lunalhx.ai.kilnai.application.kernel.PendingLearnerEventHolder;
import cn.lunalhx.ai.kilnai.application.store.InMemorySpikeStore;
import cn.lunalhx.ai.kilnai.application.usecase.LearningFlowUseCase;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.LearnerInputKind;
import cn.lunalhx.ai.kilnai.types.error.ApplicationException;
import cn.lunalhx.ai.kilnai.types.error.ErrorCode;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LearningFlowUseCaseTest {

    @Test
    void duplicateIdempotencyKeyReplaysWithoutSecondEvidence() {
        PendingCommandHolder commands = new PendingCommandHolder();
        InMemorySpikeStore store = new InMemorySpikeStore(commands);
        LearningFlowUseCase useCase = useCase(store, commands);
        UUID learner = UUID.randomUUID();
        UUID startKey = UUID.randomUUID();
        LearnerVisibleInteraction started = useCase.start(new StartGraphRun(
                learner, SpikeFixture.PERCENT_CHANGE_V1, startKey, null
        ));
        LearnerVisibleInteraction replay = useCase.start(new StartGraphRun(
                learner, SpikeFixture.PERCENT_CHANGE_V1, startKey, null
        ));
        assertEquals(started.flowId(), replay.flowId());
        assertEquals(started.interactionVersion(), replay.interactionVersion());

        UUID continueKey = UUID.randomUUID();
        ResumeGraphRun continueCommand = new ResumeGraphRun(
                started.flowId(), continueKey, started.interactionVersion(),
                LearnerInputKind.CONTINUE_REQUESTED, null
        );
        LearnerVisibleInteraction practice = useCase.resume(continueCommand);
        assertEquals(practice.interactionVersion(), useCase.resume(continueCommand).interactionVersion());

        ApplicationException conflict = assertThrows(ApplicationException.class, () ->
                useCase.resume(new ResumeGraphRun(
                        started.flowId(), continueKey, started.interactionVersion(),
                        LearnerInputKind.ANSWER_SUBMITTED, "25"
                )));
        assertEquals(ErrorCode.CONFLICT, conflict.errorCode());

        UUID answerKey = UUID.randomUUID();
        LearnerVisibleInteraction terminal = useCase.resume(new ResumeGraphRun(
                started.flowId(), answerKey, practice.interactionVersion(),
                LearnerInputKind.ANSWER_SUBMITTED, "25"
        ));
        UUID attemptId = store.latest(started.flowId()).orElseThrow().blackboard().openAttemptId();
        assertEquals(true, store.evidenceExists(attemptId));
        LearnerVisibleInteraction replayAnswer = useCase.resume(new ResumeGraphRun(
                started.flowId(), answerKey, practice.interactionVersion(),
                LearnerInputKind.ANSWER_SUBMITTED, "25"
        ));
        assertEquals(terminal.interactionVersion(), replayAnswer.interactionVersion());
        assertEquals(true, store.evidenceExists(attemptId));

        LearnerVisibleInteraction afterClose = useCase.resume(new ResumeGraphRun(
                started.flowId(), UUID.randomUUID(), terminal.interactionVersion(),
                LearnerInputKind.ANSWER_SUBMITTED, "25"
        ));
        assertEquals(terminal.interactionVersion(), afterClose.interactionVersion());
        assertEquals(true, store.evidenceExists(attemptId));
    }

    @Test
    void staleInteractionVersionConflicts() {
        PendingCommandHolder commands = new PendingCommandHolder();
        InMemorySpikeStore store = new InMemorySpikeStore(commands);
        LearningFlowUseCase useCase = useCase(store, commands);
        LearnerVisibleInteraction started = useCase.start(new StartGraphRun(
                UUID.randomUUID(), SpikeFixture.PERCENT_CHANGE_V1, UUID.randomUUID(), null
        ));
        ApplicationException conflict = assertThrows(ApplicationException.class, () ->
                useCase.resume(new ResumeGraphRun(
                        started.flowId(), UUID.randomUUID(), started.interactionVersion() - 1,
                        LearnerInputKind.CONTINUE_REQUESTED, null
                )));
        assertEquals(ErrorCode.CONFLICT, conflict.errorCode());
    }

    private LearningFlowUseCase useCase(InMemorySpikeStore store, PendingCommandHolder commands) {
        ScriptedPedagogyModel pedagogy = new ScriptedPedagogyModel(ScriptedScenario.HAPPY);
        PendingCommitBuffer buffer = new PendingCommitBuffer();
        PendingLearnerEventHolder events = new PendingLearnerEventHolder();
        LearningBlackboardMapper mapper = new LearningBlackboardMapper();
        LearningNodeKernel kernel = new LearningNodeKernel(
                buffer, pedagogy, new ScriptedTeachingModel(ScriptedScenario.HAPPY),
                new ScriptedAssessmentModel(), store, ScriptedScenario.HAPPY, true, Clock.systemUTC()
        );
        ApplicationCheckpointSaver saver = new ApplicationCheckpointSaver(store, buffer, mapper, Clock.systemUTC());
        SpringAiAlibabaGraphRuntime runtime = new SpringAiAlibabaGraphRuntime(
                store, events, mapper, new LearningStateGraphFactory(kernel, events, mapper, saver)
        );
        return new LearningFlowUseCase(runtime, store, commands, Clock.systemUTC());
    }
}
