package cn.lunalhx.ai.kilnai.domain.blackboard;

import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.FlowStatus;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.LearnerInputKind;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.LearningStage;
import cn.lunalhx.ai.kilnai.domain.pedagogy.model.valobj.TeachingAction;

import java.util.List;
import java.util.UUID;

public record LearningBlackboard(
        int schemaVersion,
        UUID flowId,
        UUID learnerId,
        UUID conceptId,
        UUID contractId,
        UUID rubricId,
        UUID sourcePackId,
        FlowStatus status,
        LearningStage stage,
        int interactionVersion,
        List<TeachingAction> legalCandidates,
        TeachingAction acceptedAction,
        UUID openAttemptId,
        UUID taskPackageArtifactId,
        List<LearnerInputKind> allowedEventKinds,
        String visibleContent,
        boolean explanationDelivered,
        LearnerInputKind pendingInput,
        List<String> compactFeedbackFacts,
        String lastRoute
) {
    public static final int SCHEMA_VERSION = 2;

    public LearningBlackboard {
        legalCandidates = legalCandidates == null ? List.of() : List.copyOf(legalCandidates);
        allowedEventKinds = allowedEventKinds == null ? List.of() : List.copyOf(allowedEventKinds);
        compactFeedbackFacts = compactFeedbackFacts == null ? List.of() : List.copyOf(compactFeedbackFacts);
    }

    public static LearningBlackboard initial(
            UUID flowId, UUID learnerId, UUID conceptId, UUID contractId, UUID rubricId, UUID sourcePackId
    ) {
        return new LearningBlackboard(
                SCHEMA_VERSION, flowId, learnerId, conceptId, contractId, rubricId, sourcePackId,
                FlowStatus.READY, LearningStage.LEARNING_AND_PRACTICE, 0,
                List.of(TeachingAction.EXPLAIN), null, null, null,
                List.of(), "", false, null, List.of(), "start"
        );
    }
}
