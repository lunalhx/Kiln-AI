package cn.lunalhx.ai.kilnai.api.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * The learner-safe result of the independent Review cancellation resource.
 * The Review state and progress are always returned; {@code flow} is the
 * committed Flow projection and includes the new terminal boundary for a
 * Started cancellation.
 */
public record ReviewTaskCancellationResponse(
        UUID reviewId,
        UUID conceptId,
        String status,
        int reviewNumber,
        Instant dueAt,
        boolean startable,
        LearningFlowResponse.ProgressView progress,
        LearningFlowResponse flow
) {
}
