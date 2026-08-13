package cn.lunalhx.ai.kilnai.application.kernel;

import cn.lunalhx.ai.kilnai.domain.blackboard.BlackboardDelta;
import cn.lunalhx.ai.kilnai.domain.blackboard.LearningBlackboard;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.FlowStatus;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.LearnerInputKind;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.LearningStage;
import cn.lunalhx.ai.kilnai.domain.pedagogy.model.valobj.TeachingAction;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class BlackboardApplier {

    @SuppressWarnings("unchecked")
    public LearningBlackboard apply(LearningBlackboard current, BlackboardDelta delta) {
        Map<String, Object> fields = delta.fields();
        return new LearningBlackboard(
                current.schemaVersion(),
                current.flowId(),
                current.learnerId(),
                current.conceptId(),
                current.contractId(),
                current.rubricId(),
                current.sourcePackId(),
                value(fields, "status", current.status()),
                value(fields, "stage", current.stage()),
                intValue(fields, "interactionVersion", current.interactionVersion()),
                list(fields, "legalCandidates", current.legalCandidates()),
                value(fields, "acceptedAction", current.acceptedAction()),
                value(fields, "openAttemptId", current.openAttemptId()),
                value(fields, "taskPackageArtifactId", current.taskPackageArtifactId()),
                list(fields, "allowedEventKinds", current.allowedEventKinds()),
                stringValue(fields, "visibleContent", current.visibleContent()),
                boolValue(fields, "explanationDelivered", current.explanationDelivered()),
                value(fields, "pendingInput", current.pendingInput()),
                intValue(fields, "modelCallCount", current.modelCallCount()),
                intValue(fields, "repairCount", current.repairCount()),
                list(fields, "compactFeedbackFacts", current.compactFeedbackFacts()),
                stringValue(fields, "lastRoute", current.lastRoute())
        );
    }

    @SuppressWarnings("unchecked")
    private <T> T value(Map<String, Object> fields, String key, T fallback) {
        return fields.containsKey(key) ? (T) fields.get(key) : fallback;
    }

    private int intValue(Map<String, Object> fields, String key, int fallback) {
        Object value = fields.get(key);
        return value instanceof Number number ? number.intValue() : fallback;
    }

    private boolean boolValue(Map<String, Object> fields, String key, boolean fallback) {
        Object value = fields.get(key);
        return value instanceof Boolean bool ? bool : fallback;
    }

    private String stringValue(Map<String, Object> fields, String key, String fallback) {
        Object value = fields.get(key);
        return value instanceof String string ? string : fallback;
    }

    @SuppressWarnings("unchecked")
    private <T> List<T> list(Map<String, Object> fields, String key, List<T> fallback) {
        Object value = fields.get(key);
        return value instanceof List<?> list ? (List<T>) List.copyOf(list) : fallback;
    }
}
