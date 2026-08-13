package cn.lunalhx.ai.kilnai.infrastructure.adapter.graph;

import cn.lunalhx.ai.kilnai.domain.blackboard.LearningBlackboard;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.FlowStatus;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.LearnerInputKind;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.LearningStage;
import cn.lunalhx.ai.kilnai.domain.pedagogy.model.valobj.TeachingAction;
import com.alibaba.cloud.ai.graph.OverAllState;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class LearningBlackboardMapper {

    public static final String BLACKBOARD_KEY = "blackboard";
    public static final String NEXT_ROUTE_KEY = "nextRoute";

    public Map<String, Object> toFramework(LearningBlackboard blackboard, String nextRoute) {
        Map<String, Object> state = new HashMap<>();
        state.put(BLACKBOARD_KEY, toMap(blackboard));
        if (nextRoute != null) {
            state.put(NEXT_ROUTE_KEY, nextRoute);
        }
        return state;
    }

    public LearningBlackboard fromState(OverAllState state) {
        Object raw = state.data().get(BLACKBOARD_KEY);
        if (raw instanceof Map<?, ?> map) {
            return fromMap(cast(map));
        }
        throw new IllegalStateException("blackboard missing from framework state");
    }

    public Map<String, Object> toMap(LearningBlackboard board) {
        Map<String, Object> map = new HashMap<>();
        map.put("schemaVersion", board.schemaVersion());
        map.put("flowId", board.flowId().toString());
        map.put("learnerId", board.learnerId().toString());
        map.put("conceptId", board.conceptId().toString());
        map.put("contractId", board.contractId().toString());
        map.put("rubricId", board.rubricId().toString());
        map.put("sourcePackId", board.sourcePackId().toString());
        map.put("status", board.status().name());
        map.put("stage", board.stage().name());
        map.put("interactionVersion", board.interactionVersion());
        map.put("legalCandidates", board.legalCandidates().stream().map(Enum::name).toList());
        map.put("acceptedAction", board.acceptedAction() == null ? null : board.acceptedAction().name());
        map.put("openAttemptId", board.openAttemptId() == null ? null : board.openAttemptId().toString());
        map.put("taskPackageArtifactId", board.taskPackageArtifactId() == null ? null : board.taskPackageArtifactId().toString());
        map.put("allowedEventKinds", board.allowedEventKinds().stream().map(Enum::name).toList());
        map.put("visibleContent", board.visibleContent());
        map.put("explanationDelivered", board.explanationDelivered());
        map.put("pendingInput", board.pendingInput() == null ? null : board.pendingInput().name());
        map.put("modelCallCount", board.modelCallCount());
        map.put("repairCount", board.repairCount());
        map.put("compactFeedbackFacts", board.compactFeedbackFacts());
        map.put("lastRoute", board.lastRoute());
        return map;
    }

    public LearningBlackboard fromMap(Map<String, Object> map) {
        int schemaVersion = ((Number) map.getOrDefault("schemaVersion", 1)).intValue();
        if (schemaVersion != LearningBlackboard.SCHEMA_VERSION) {
            throw new IllegalStateException("unknown blackboard schemaVersion: " + schemaVersion);
        }
        return new LearningBlackboard(
                schemaVersion,
                uuid(map.get("flowId")),
                uuid(map.get("learnerId")),
                uuid(map.get("conceptId")),
                uuid(map.get("contractId")),
                uuid(map.get("rubricId")),
                uuid(map.get("sourcePackId")),
                enumValue(map.get("status"), FlowStatus.class, FlowStatus.READY),
                enumValue(map.get("stage"), LearningStage.class, LearningStage.LEARNING_AND_PRACTICE),
                ((Number) map.getOrDefault("interactionVersion", 0)).intValue(),
                enums(map.get("legalCandidates"), TeachingAction.class),
                enumValue(map.get("acceptedAction"), TeachingAction.class, null),
                uuid(map.get("openAttemptId")),
                uuid(map.get("taskPackageArtifactId")),
                enums(map.get("allowedEventKinds"), LearnerInputKind.class),
                String.valueOf(map.getOrDefault("visibleContent", "")),
                Boolean.TRUE.equals(map.get("explanationDelivered")),
                enumValue(map.get("pendingInput"), LearnerInputKind.class, null),
                ((Number) map.getOrDefault("modelCallCount", 0)).intValue(),
                ((Number) map.getOrDefault("repairCount", 0)).intValue(),
                strings(map.get("compactFeedbackFacts")),
                String.valueOf(map.getOrDefault("lastRoute", "start"))
        );
    }

    private UUID uuid(Object value) {
        return value == null || "null".equals(value) ? null : UUID.fromString(value.toString());
    }

    private <E extends Enum<E>> E enumValue(Object value, Class<E> type, E fallback) {
        if (value == null || "null".equals(value)) {
            return fallback;
        }
        if (type.isInstance(value)) {
            return type.cast(value);
        }
        return Enum.valueOf(type, value.toString());
    }

    private <E extends Enum<E>> List<E> enums(Object value, Class<E> type) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        return list.stream().map(item -> enumValue(item, type, null)).toList();
    }

    private List<String> strings(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        return list.stream().map(String::valueOf).toList();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> cast(Map<?, ?> map) {
        return (Map<String, Object>) map;
    }
}
