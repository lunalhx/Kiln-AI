package cn.lunalhx.ai.kilnai.api.dto;

import java.util.List;
import java.util.UUID;

/**
 * The learner-safe response of one unified Learning Flow command or query.
 * It exposes one closed committed-interaction union via {@code kind} — a
 * {@code task} carrying the open Attempt's learner projection, a
 * {@code teaching} interaction, an {@code assistance_consent} request, a
 * message-only {@code transition}, or the neutral {@code unavailable}
 * boundary of a failed node. {@code allowedEvents} are the closed learning
 * command names the learner may issue against this committed interaction.
 * Private answers, unexposed hint levels, Rubric internals, source passages,
 * assessment facts, Blackboard content, and execution traces never appear.
 */
public record LearningFlowResponse(
        UUID flowId,
        int interactionVersion,
        String kind,
        String status,
        String stage,
        UUID attemptId,
        String attemptPurpose,
        TaskView task,
        TeachingView teaching,
        ConsentView consent,
        HintView hint,
        String learnerMessage,
        List<String> allowedEvents,
        ProgressView progress,
        DiagnosticProgressView diagnosticProgress
) {

    public record TaskView(
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

    public record TeachingView(
            String principleSummary,
            WorkedExampleView workedExample
    ) {

        public record WorkedExampleView(
                String problem,
                List<StepView> steps,
                String finalResult
        ) {

            public record StepView(
                    String expression,
                    String ruleReference,
                    String explanation
            ) {
            }
        }
    }

    public record ConsentView(
            String warning
    ) {
    }

    public record HintView(
            int level,
            String disclosureKind,
            String learnerContent,
            List<String> reasoningSteps,
            String proposedFinalAnswer
    ) {
    }

    public record ProgressView(
            String currentMilestone,
            String highestMilestoneReached,
            String stage
    ) {
    }

    public record DiagnosticProgressView(
            int completedAttempts,
            int maximumAttempts
    ) {
    }
}
