package cn.lunalhx.ai.kilnai.api.dto;

import java.util.List;
import java.util.UUID;

/**
 * The learner-safe response of one Apply flow command or query. It carries
 * only the learner projection of the current task, flow status, and the safe
 * Concept Progress projection (Current Milestone, Highest Milestone Reached,
 * Stage); expected answers, source traces, Fingerprints, and assessment
 * facts never appear.
 */
public record ApplyFlowResponse(
        UUID flowId,
        int interactionVersion,
        String status,
        String stage,
        UUID attemptId,
        String attemptPurpose,
        ApplyTaskView task,
        String learnerMessage,
        List<String> allowedEvents,
        ProgressView progress
) {

    public record ApplyTaskView(
            String locale,
            String taskText,
            List<AnswerFieldView> answerFields,
            int maxFormalSubmissions
    ) {

        public record AnswerFieldView(
                String id,
                String label,
                String kind,
                List<String> variables,
                List<String> acceptedInputFamilies,
                boolean required
        ) {
        }
    }

    public record ProgressView(
            String currentMilestone,
            String highestMilestoneReached,
            String stage
    ) {
    }
}
