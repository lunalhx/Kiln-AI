package cn.lunalhx.ai.kilnai.domain.apply.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * The closed {@code teach_back_execution_context/v1} Node Context View of one
 * Teach-back invocation: the exact Concept Contract and Mastery Rubric, the
 * sanitized pedagogy intent, the learner locale, and only the eligible
 * exposed anchor content with its anchor id, kind, and source trace. It
 * receives no raw learner answers, assessment reasoning, unexposed hints, or
 * private expected answer beyond content the learner has already seen; the
 * Flow-frozen plan and Skill Stack live on the compiled prompt side and are
 * pinned in the execution trace.
 */
public record TeachBackExecutionContext(
        @JsonProperty("schema") String schema,
        @JsonProperty("concept_contract") ConceptContract conceptContract,
        @JsonProperty("mastery_rubric") MasteryRubric masteryRubric,
        @JsonProperty("pedagogy_intent") PedagogyIntent pedagogyIntent,
        @JsonProperty("anchor") AnchorView anchor,
        @JsonProperty("learner_locale") String learnerLocale
) {

    public TeachBackExecutionContext {
        Objects.requireNonNull(schema, "schema must not be null");
        Objects.requireNonNull(conceptContract, "conceptContract must not be null");
        Objects.requireNonNull(masteryRubric, "masteryRubric must not be null");
        Objects.requireNonNull(pedagogyIntent, "pedagogyIntent must not be null");
        Objects.requireNonNull(anchor, "anchor must not be null");
        Objects.requireNonNull(learnerLocale, "learnerLocale must not be null");
    }

    public TeachBackExecutionContext withAnchor(AnchorView replacement) {
        Objects.requireNonNull(replacement, "replacement must not be null");
        return new TeachBackExecutionContext(
                schema, conceptContract, masteryRubric, pedagogyIntent, replacement, learnerLocale);
    }

    public record ConceptContract(
            @JsonProperty("id") String id,
            @JsonProperty("version") String version,
            @JsonProperty("included_scope") List<String> includedScope,
            @JsonProperty("excluded_scope") List<String> excludedScope
    ) {
        public ConceptContract {
            includedScope = List.copyOf(includedScope);
            excludedScope = List.copyOf(excludedScope);
        }
    }

    public record MasteryRubric(
            @JsonProperty("id") String id,
            @JsonProperty("version") String version,
            @JsonProperty("criteria") List<RubricCriterion> criteria
    ) {
        public MasteryRubric {
            criteria = List.copyOf(criteria);
        }
    }

    public record RubricCriterion(
            @JsonProperty("id") String id,
            @JsonProperty("description") String description
    ) {
    }

    public record PedagogyIntent(
            @JsonProperty("intent") String intent,
            @JsonProperty("satisfied_criteria") List<String> satisfiedCriteria,
            @JsonProperty("missing_criteria") List<String> missingCriteria,
            @JsonProperty("error_dimensions") List<String> errorDimensions
    ) {
        public PedagogyIntent {
            Objects.requireNonNull(intent, "intent must not be null");
            satisfiedCriteria = List.copyOf(satisfiedCriteria);
            missingCriteria = List.copyOf(missingCriteria);
            errorDimensions = List.copyOf(errorDimensions);
        }
    }

    /**
     * The eligible exposed anchor content: the learner already saw this
     * content, so carrying it is not a privacy leak, and it is the only
     * reference the generated Teach-back task may ask the learner to explain.
     */
    public record AnchorView(
            @JsonProperty("anchor_id") UUID anchorId,
            @JsonProperty("anchor_kind") String anchorKind,
            @JsonProperty("learner_content") String learnerContent,
            @JsonProperty("source_trace") List<SourceTraceRef> sourceTrace
    ) {
        public AnchorView {
            Objects.requireNonNull(anchorId, "anchorId must not be null");
            Objects.requireNonNull(anchorKind, "anchorKind must not be null");
            Objects.requireNonNull(learnerContent, "learnerContent must not be null");
            sourceTrace = List.copyOf(sourceTrace);
        }
    }

    public record SourceTraceRef(
            @JsonProperty("source_document_id") String sourceDocumentId,
            @JsonProperty("passage_id") String passageId
    ) {
        public SourceTraceRef {
            Objects.requireNonNull(sourceDocumentId, "sourceDocumentId must not be null");
            Objects.requireNonNull(passageId, "passageId must not be null");
        }
    }
}
