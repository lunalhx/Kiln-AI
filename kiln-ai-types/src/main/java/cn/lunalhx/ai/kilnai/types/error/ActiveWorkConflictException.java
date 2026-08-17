package cn.lunalhx.ai.kilnai.types.error;

import java.util.Objects;
import java.util.UUID;

/**
 * The learner-safe Start conflict of ADR-0070: the learner already holds the
 * unique Active Learning Work claim for the Target Concept — either a
 * non-terminal Flow or an unfinished Review Task. The 409 response carries
 * only the existing Flow id needed for recovery, never private assessor data.
 */
public class ActiveWorkConflictException extends ApplicationException {

    private final UUID existingFlowId;

    public ActiveWorkConflictException(UUID existingFlowId) {
        super(ErrorCode.CONFLICT, "已有进行中的学习，无法重新开始");
        this.existingFlowId = Objects.requireNonNull(existingFlowId, "existingFlowId must not be null");
    }

    public UUID existingFlowId() {
        return existingFlowId;
    }
}
