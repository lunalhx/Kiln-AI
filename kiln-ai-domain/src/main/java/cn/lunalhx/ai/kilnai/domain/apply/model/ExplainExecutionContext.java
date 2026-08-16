package cn.lunalhx.ai.kilnai.domain.apply.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Objects;

/**
 * The closed {@code explain_execution_context/v1} Node Context View of one
 * Explain invocation: the Concept Contract, Mastery Rubric, sanitized pedagogy
 * intent, bounded approved source passages, example-fingerprint novelty
 * exclusions, and the learner locale. It contains no raw learner answers,
 * private expected answers, assessment reasoning, complete Learning State, or
 * unrestricted Flow history. The Flow-frozen plan and Skill Stack live on the
 * compiled prompt side and are pinned in the execution trace.
 */
public record ExplainExecutionContext(
        @JsonProperty("schema") String schema,
        @JsonProperty("concept_contract") ConceptContract conceptContract,
        @JsonProperty("mastery_rubric") MasteryRubric masteryRubric,
        @JsonProperty("pedagogy_intent") PedagogyIntent pedagogyIntent,
        @JsonProperty("concept_source_pack") ConceptSourcePack conceptSourcePack,
        @JsonProperty("novelty_exclusions") NoveltyExclusions noveltyExclusions,
        @JsonProperty("learner_locale") String learnerLocale
) {

    public ExplainExecutionContext {
        Objects.requireNonNull(schema, "schema must not be null");
        Objects.requireNonNull(conceptContract, "conceptContract must not be null");
        Objects.requireNonNull(masteryRubric, "masteryRubric must not be null");
        Objects.requireNonNull(pedagogyIntent, "pedagogyIntent must not be null");
        Objects.requireNonNull(conceptSourcePack, "conceptSourcePack must not be null");
        Objects.requireNonNull(noveltyExclusions, "noveltyExclusions must not be null");
        Objects.requireNonNull(learnerLocale, "learnerLocale must not be null");
    }

    public ExplainExecutionContext withNoveltyExclusions(List<String> exampleFingerprints) {
        return new ExplainExecutionContext(
                schema, conceptContract, masteryRubric, pedagogyIntent, conceptSourcePack,
                new NoveltyExclusions(exampleFingerprints), learnerLocale);
    }

    public record ConceptContract(
            @JsonProperty("id") String id,
            @JsonProperty("version") String version,
            @JsonProperty("included_scope") List<String> includedScope,
            @JsonProperty("excluded_scope") List<String> excludedScope
    ) {

        public ConceptContract {
            Objects.requireNonNull(id, "id must not be null");
            Objects.requireNonNull(version, "version must not be null");
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
            Objects.requireNonNull(id, "id must not be null");
            Objects.requireNonNull(version, "version must not be null");
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

    public record ConceptSourcePack(
            @JsonProperty("id") String id,
            @JsonProperty("version") String version,
            @JsonProperty("passages") List<SourcePassage> passages
    ) {

        public ConceptSourcePack {
            Objects.requireNonNull(id, "id must not be null");
            Objects.requireNonNull(version, "version must not be null");
            passages = List.copyOf(passages);
        }
    }

    public record SourcePassage(
            @JsonProperty("source_document_id") String sourceDocumentId,
            @JsonProperty("source_version") String sourceVersion,
            @JsonProperty("passage_id") String passageId,
            @JsonProperty("source_language") String sourceLanguage,
            @JsonProperty("content") String content
    ) {
    }

    public record NoveltyExclusions(
            @JsonProperty("exposed_example_fingerprints") List<String> exposedExampleFingerprints
    ) {

        public NoveltyExclusions {
            exposedExampleFingerprints = List.copyOf(exposedExampleFingerprints);
        }
    }
}
