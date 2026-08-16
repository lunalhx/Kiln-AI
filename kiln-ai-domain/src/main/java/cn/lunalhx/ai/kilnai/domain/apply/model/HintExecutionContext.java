package cn.lunalhx.ai.kilnai.domain.apply.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Objects;

/**
 * The closed {@code hint_execution_context/v1} data handed to the Hint model
 * port. It carries the current learner-visible Practice task, the private
 * canonical expected answer of its verified Task Package, the approved source
 * passages, the already exposed levels, the requested next level, and the
 * learner locale. It never carries assessment reasoning, other raw answers,
 * the full Blackboard, or the learner's unsubmitted draft.
 */
public record HintExecutionContext(
        @JsonProperty("schema") String schema,
        @JsonProperty("task") TaskView task,
        @JsonProperty("expected_answer") ExpectedAnswer expectedAnswer,
        @JsonProperty("source_passages") List<SourcePassageView> sourcePassages,
        @JsonProperty("exposed_levels") List<Integer> exposedLevels,
        @JsonProperty("requested_level") int requestedLevel,
        @JsonProperty("learner_locale") String learnerLocale
) {
    public static final String SCHEMA = "hint_execution_context/v1";

    public HintExecutionContext {
        Objects.requireNonNull(schema, "schema must not be null");
        Objects.requireNonNull(task, "task must not be null");
        Objects.requireNonNull(expectedAnswer, "expectedAnswer must not be null");
        Objects.requireNonNull(sourcePassages, "sourcePassages must not be null");
        Objects.requireNonNull(exposedLevels, "exposedLevels must not be null");
        Objects.requireNonNull(learnerLocale, "learnerLocale must not be null");
        sourcePassages = List.copyOf(sourcePassages);
        exposedLevels = List.copyOf(exposedLevels);
    }

    public record TaskView(
            @JsonProperty("task_text") String taskText,
            @JsonProperty("locale") String locale
    ) {
        public TaskView {
            Objects.requireNonNull(taskText, "taskText must not be null");
            Objects.requireNonNull(locale, "locale must not be null");
        }
    }

    public record ExpectedAnswer(
            @JsonProperty("expression") String expression,
            @JsonProperty("variables") List<String> variables,
            @JsonProperty("domain") String domain
    ) {
        public ExpectedAnswer {
            Objects.requireNonNull(expression, "expression must not be null");
            Objects.requireNonNull(variables, "variables must not be null");
            Objects.requireNonNull(domain, "domain must not be null");
            variables = List.copyOf(variables);
        }
    }

    public record SourcePassageView(
            @JsonProperty("source_document_id") String sourceDocumentId,
            @JsonProperty("source_version") String sourceVersion,
            @JsonProperty("passage_id") String passageId,
            @JsonProperty("content") String content
    ) {
        public SourcePassageView {
            Objects.requireNonNull(sourceDocumentId, "sourceDocumentId must not be null");
            Objects.requireNonNull(sourceVersion, "sourceVersion must not be null");
            Objects.requireNonNull(passageId, "passageId must not be null");
            Objects.requireNonNull(content, "content must not be null");
        }
    }
}
