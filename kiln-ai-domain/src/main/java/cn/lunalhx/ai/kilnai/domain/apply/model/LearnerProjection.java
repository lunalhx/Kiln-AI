package cn.lunalhx.ai.kilnai.domain.apply.model;

import java.util.List;
import java.util.Objects;

public record LearnerProjection(
        String locale,
        String taskText,
        List<AnswerField> answerFields,
        List<ApplyLearnerEvent> allowedEvents,
        SubmissionRule submissionRule
) {

    public LearnerProjection {
        Objects.requireNonNull(locale, "locale must not be null");
        Objects.requireNonNull(taskText, "taskText must not be null");
        Objects.requireNonNull(answerFields, "answerFields must not be null");
        Objects.requireNonNull(allowedEvents, "allowedEvents must not be null");
        Objects.requireNonNull(submissionRule, "submissionRule must not be null");
        answerFields = List.copyOf(answerFields);
        allowedEvents = List.copyOf(allowedEvents);
    }

    public record AnswerField(
            String id,
            String label,
            String kind,
            List<String> variables,
            List<String> acceptedInputFamilies,
            boolean required
    ) {
        public AnswerField {
            Objects.requireNonNull(id, "id must not be null");
            Objects.requireNonNull(label, "label must not be null");
            Objects.requireNonNull(kind, "kind must not be null");
            variables = variables == null ? null : List.copyOf(variables);
            acceptedInputFamilies = acceptedInputFamilies == null ? null : List.copyOf(acceptedInputFamilies);
        }
    }

    public record SubmissionRule(int maxFormalSubmissions) {
    }
}
