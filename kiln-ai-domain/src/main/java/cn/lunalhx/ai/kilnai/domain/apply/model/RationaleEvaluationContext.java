package cn.lunalhx.ai.kilnai.domain.apply.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * The least-privilege, subject-neutral context supplied independently to a
 * Rationale Assessment. It contains task-owned truth, but no learner primary
 * answer, check result, prior evaluation, feedback, or routing state.
 */
public record RationaleEvaluationContext(
        @JsonProperty("schema") String schema,
        @JsonProperty("task_text") String taskText,
        @JsonProperty("rationale") String rationale,
        @JsonProperty("task_rubric") List<TaskRubricCriterion> taskRubric,
        @JsonProperty("expected_answer_facts") ExpectedAnswerFacts expectedAnswerFacts,
        @JsonProperty("source_passages") List<SourcePassage> sourcePassages,
        @JsonProperty("learner_locale") String learnerLocale
) {

    public static final String SCHEMA = "rationale_evaluation_context/v1";
    public static final String CANONICAL_EXPRESSION_KIND = "canonical_expression";
    public static final String RATIONALE_EVIDENCE_CHANNEL = "optional_rule_rationale";
    private static final int MAX_SOURCE_PASSAGES = 8;

    public RationaleEvaluationContext {
        requireText(schema, "schema");
        requireText(taskText, "taskText");
        Objects.requireNonNull(rationale, "rationale must not be null");
        if (rationale.isBlank()) {
            throw new IllegalArgumentException("rationale must not be blank");
        }
        Objects.requireNonNull(taskRubric, "taskRubric must not be null");
        Objects.requireNonNull(expectedAnswerFacts, "expectedAnswerFacts must not be null");
        Objects.requireNonNull(sourcePassages, "sourcePassages must not be null");
        requireText(learnerLocale, "learnerLocale");
        if (!SCHEMA.equals(schema)) {
            throw new IllegalArgumentException("unsupported rationale evaluation context schema: " + schema);
        }
        taskRubric = List.copyOf(taskRubric);
        sourcePassages = List.copyOf(sourcePassages);
        if (sourcePassages.size() > MAX_SOURCE_PASSAGES) {
            throw new IllegalArgumentException("sourcePassages exceed the bounded context limit");
        }
    }

    /**
     * Projects only the task-owned facts permitted at the evaluation
     * boundary. Rubric and source order remain deterministic and all source
     * passages must be present in the approved Source Pack.
     */
    public static RationaleEvaluationContext from(
            TaskPackage taskPackage,
            ApplyExecutionContext executionContext,
            String rationale
    ) {
        Objects.requireNonNull(taskPackage, "taskPackage must not be null");
        Objects.requireNonNull(executionContext, "executionContext must not be null");
        Objects.requireNonNull(rationale, "rationale must not be null");

        Set<String> rationaleCriterionIds = taskPackage.privateAssessorProjection().rubricMapping().stream()
                .filter(mapping -> mapping.evidenceChannels().contains(RATIONALE_EVIDENCE_CHANNEL))
                .map(PrivateAssessorFacts.RubricMapping::masteryCriterionId)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        List<TaskRubricCriterion> rubric = executionContext.masteryRubric().criteria().stream()
                .filter(criterion -> rationaleCriterionIds.contains(criterion.id()))
                .map(criterion -> new TaskRubricCriterion(criterion.id(), criterion.description()))
                .toList();

        PrivateAssessorProjection.CanonicalExpectedAnswer expected =
                taskPackage.privateAssessorProjection().canonicalExpectedAnswer();
        ExpectedAnswerFacts expectedFacts = new ExpectedAnswerFacts(
                CANONICAL_EXPRESSION_KIND, expected.expression(), expected.variables(), expected.domain());

        Set<String> selectedPassages = new HashSet<>();
        List<SourcePassage> passages = taskPackage.privateAssessorProjection().sourceTrace().stream()
                .filter(trace -> selectedPassages.add(sourceKey(
                        trace.sourceDocumentId(), trace.sourceVersion(), trace.passageId())))
                .map(trace -> executionContext.conceptSourcePack().passages().stream()
                        .filter(passage -> passage.sourceDocumentId().equals(trace.sourceDocumentId())
                                && passage.sourceVersion().equals(trace.sourceVersion())
                                && passage.passageId().equals(trace.passageId()))
                        .findFirst()
                        .map(RationaleEvaluationContext::sourcePassageOf)
                        .orElse(null))
                .filter(Objects::nonNull)
                .limit(MAX_SOURCE_PASSAGES)
                .toList();

        return new RationaleEvaluationContext(
                SCHEMA,
                taskPackage.learnerProjection().taskText(),
                rationale,
                rubric,
                expectedFacts,
                passages,
                executionContext.learnerLocale());
    }

    public static RationaleEvaluationContext parse(String json) {
        JsonNode root = ModelContract.object(json);
        ModelContract.requireExactFields(root, Set.of(
                "schema", "task_text", "rationale", "task_rubric",
                "expected_answer_facts", "source_passages", "learner_locale"));
        return new RationaleEvaluationContext(
                ModelContract.requiredSchema(root, SCHEMA),
                ModelContract.requiredText(root, "task_text"),
                ModelContract.requiredText(root, "rationale"),
                parseRubric(ModelContract.requiredArray(root, "task_rubric")),
                parseExpectedFacts(ModelContract.requiredObject(root, "expected_answer_facts")),
                parseSourcePassages(ModelContract.requiredArray(root, "source_passages")),
                ModelContract.requiredText(root, "learner_locale"));
    }

    private static List<TaskRubricCriterion> parseRubric(JsonNode array) {
        java.util.ArrayList<TaskRubricCriterion> values = new java.util.ArrayList<>();
        for (JsonNode item : array) {
            ModelContract.requireExactFields(item, Set.of("id", "description"));
            values.add(new TaskRubricCriterion(
                    ModelContract.requiredText(item, "id"),
                    ModelContract.requiredText(item, "description")));
        }
        return List.copyOf(values);
    }

    private static ExpectedAnswerFacts parseExpectedFacts(JsonNode object) {
        ModelContract.requireExactFields(object, Set.of("kind", "expression", "variables", "domain"));
        return new ExpectedAnswerFacts(
                ModelContract.requiredText(object, "kind"),
                ModelContract.requiredText(object, "expression"),
                ModelContract.requiredStringList(object, "variables"),
                ModelContract.requiredText(object, "domain"));
    }

    private static List<SourcePassage> parseSourcePassages(JsonNode array) {
        java.util.ArrayList<SourcePassage> values = new java.util.ArrayList<>();
        for (JsonNode item : array) {
            ModelContract.requireExactFields(item, Set.of(
                    "source_document_id", "source_version", "passage_id", "source_language", "content"));
            values.add(new SourcePassage(
                    ModelContract.requiredText(item, "source_document_id"),
                    ModelContract.requiredText(item, "source_version"),
                    ModelContract.requiredText(item, "passage_id"),
                    ModelContract.requiredText(item, "source_language"),
                    ModelContract.requiredText(item, "content")));
        }
        return List.copyOf(values);
    }

    private static SourcePassage sourcePassageOf(ApplyExecutionContext.SourcePassage passage) {
        return new SourcePassage(
                passage.sourceDocumentId(), passage.sourceVersion(), passage.passageId(),
                passage.sourceLanguage(), passage.content());
    }

    private static String sourceKey(String documentId, String version, String passageId) {
        return documentId + "\u0000" + version + "\u0000" + passageId;
    }

    private static void requireText(String value, String name) {
        Objects.requireNonNull(value, name + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
    }

    public record TaskRubricCriterion(
            @JsonProperty("id") String id,
            @JsonProperty("description") String description
    ) {
        public TaskRubricCriterion {
            requireText(id, "id");
            requireText(description, "description");
        }
    }

    public record ExpectedAnswerFacts(
            @JsonProperty("kind") String kind,
            @JsonProperty("expression") String expression,
            @JsonProperty("variables") List<String> variables,
            @JsonProperty("domain") String domain
    ) {
        public ExpectedAnswerFacts {
            requireText(kind, "kind");
            requireText(expression, "expression");
            Objects.requireNonNull(variables, "variables must not be null");
            variables = List.copyOf(variables);
            if (!CANONICAL_EXPRESSION_KIND.equals(kind) || variables.isEmpty()
                    || variables.stream().anyMatch(value -> value == null || value.isBlank())) {
                throw new IllegalArgumentException("unsupported expected answer facts");
            }
            requireText(domain, "domain");
        }
    }

    public record SourcePassage(
            @JsonProperty("source_document_id") String sourceDocumentId,
            @JsonProperty("source_version") String sourceVersion,
            @JsonProperty("passage_id") String passageId,
            @JsonProperty("source_language") String sourceLanguage,
            @JsonProperty("content") String content
    ) {
        public SourcePassage {
            requireText(sourceDocumentId, "sourceDocumentId");
            requireText(sourceVersion, "sourceVersion");
            requireText(passageId, "passageId");
            requireText(sourceLanguage, "sourceLanguage");
            requireText(content, "content");
        }
    }
}
