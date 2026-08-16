package cn.lunalhx.ai.kilnai.domain.learning.graph;

import cn.lunalhx.ai.kilnai.domain.apply.model.ApplyCheckpoint;
import cn.lunalhx.ai.kilnai.domain.apply.model.ApplyFlowInteraction;
import cn.lunalhx.ai.kilnai.domain.apply.port.LearningFlowStore;
import cn.lunalhx.ai.kilnai.types.error.ApplicationException;
import cn.lunalhx.ai.kilnai.types.error.ErrorCode;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * The typed Learning State of one Learning Flow, reconstructed from the
 * durable domain records and the latest graph checkpoint. It carries the
 * identifiers and execution facts a new Graph Run needs to resume exactly at
 * the last Learner Interaction Boundary; accepted Learning Evidence remains
 * the source of truth for Concept Progress and is not copied here. Only the
 * Learning StateGraph package constructs and owns this state.
 */
public record LearningState(
        LearningFlowStore.FlowRecord flow,
        ApplyFlowInteraction latestInteraction,
        Optional<ApplyCheckpoint> latestCheckpoint
) {

    public LearningState {
        Objects.requireNonNull(flow, "flow must not be null");
        Objects.requireNonNull(latestInteraction, "latestInteraction must not be null");
        Objects.requireNonNull(latestCheckpoint, "latestCheckpoint must not be null");
    }

    public static LearningState rehydrate(LearningFlowStore flowStore, UUID flowId) {
        Objects.requireNonNull(flowStore, "flowStore must not be null");
        Objects.requireNonNull(flowId, "flowId must not be null");
        LearningFlowStore.FlowRecord flow = flowStore.findFlow(flowId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.FLOW_NOT_FOUND, "flow not found"));
        ApplyFlowInteraction latest = flowStore.latestInteraction(flowId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.FLOW_NOT_FOUND, "flow not found"));
        return new LearningState(flow, latest, flowStore.latestCheckpoint(flowId));
    }
}
