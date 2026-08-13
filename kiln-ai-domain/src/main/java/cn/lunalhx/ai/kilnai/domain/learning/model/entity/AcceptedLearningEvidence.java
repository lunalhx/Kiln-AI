package cn.lunalhx.ai.kilnai.domain.learning.model.entity;

import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.AttemptPurpose;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.LearningResult;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record AcceptedLearningEvidence(
        UUID id,
        UUID taskAttemptId,
        UUID flowId,
        UUID conceptId,
        UUID learnerId,
        LearningResult result,
        AttemptPurpose attemptPurpose,
        int highestHintLevel,
        List<String> assistanceTrace,
        Instant acceptedAt
) {
    public AcceptedLearningEvidence {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(taskAttemptId, "taskAttemptId must not be null");
        Objects.requireNonNull(flowId, "flowId must not be null");
        Objects.requireNonNull(conceptId, "conceptId must not be null");
        Objects.requireNonNull(learnerId, "learnerId must not be null");
        Objects.requireNonNull(result, "result must not be null");
        Objects.requireNonNull(attemptPurpose, "attemptPurpose must not be null");
        Objects.requireNonNull(assistanceTrace, "assistanceTrace must not be null");
        Objects.requireNonNull(acceptedAt, "acceptedAt must not be null");
        if (highestHintLevel < 0 || highestHintLevel > 5) {
            throw new IllegalArgumentException("highestHintLevel must be between 0 and 5");
        }
        assistanceTrace = List.copyOf(assistanceTrace);
    }

    public boolean isIndependentSuccess() {
        return result == LearningResult.PASS
                && highestHintLevel == 0
                && attemptPurpose == AttemptPurpose.INDEPENDENT_TEST;
    }

    public boolean isPracticeSuccess() {
        return result == LearningResult.PASS && attemptPurpose == AttemptPurpose.PRACTICE;
    }
}
