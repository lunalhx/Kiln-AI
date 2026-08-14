package cn.lunalhx.ai.kilnai.domain.apply.model;

import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.AttemptPurpose;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Objects;

public record ApplyExecutionContext(
        @JsonProperty("schema") String schema,
        @JsonProperty("concept_contract") ConceptContract conceptContract,
        @JsonProperty("mastery_rubric") MasteryRubric masteryRubric,
        @JsonProperty("task_blueprint") TaskBlueprint taskBlueprint,
        @JsonProperty("concept_source_pack") ConceptSourcePack conceptSourcePack,
        @JsonProperty("novelty_exclusions") NoveltyExclusions noveltyExclusions,
        @JsonProperty("answer_representation_contract") AnswerRepresentationContract answerRepresentationContract,
        @JsonProperty("learner_locale") String learnerLocale
) {

    public ApplyExecutionContext {
        Objects.requireNonNull(schema, "schema must not be null");
        Objects.requireNonNull(conceptContract, "conceptContract must not be null");
        Objects.requireNonNull(masteryRubric, "masteryRubric must not be null");
        Objects.requireNonNull(taskBlueprint, "taskBlueprint must not be null");
        Objects.requireNonNull(conceptSourcePack, "conceptSourcePack must not be null");
        Objects.requireNonNull(noveltyExclusions, "noveltyExclusions must not be null");
        Objects.requireNonNull(answerRepresentationContract, "answerRepresentationContract must not be null");
        Objects.requireNonNull(learnerLocale, "learnerLocale must not be null");
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

    public record TaskBlueprint(
            @JsonProperty("id") String id,
            @JsonProperty("version") String version,
            @JsonProperty("attempt_purpose") AttemptPurpose attemptPurpose,
            @JsonProperty("task_shape") TaskShape taskShape,
            @JsonProperty("mathematical_scope") MathematicalScope mathematicalScope,
            @JsonProperty("response_fields") ResponseFields responseFields,
            @JsonProperty("assessment_policy_ref") String assessmentPolicyRef
    ) {
        public TaskBlueprint {
            Objects.requireNonNull(attemptPurpose, "attemptPurpose must not be null");
            Objects.requireNonNull(taskShape, "taskShape must not be null");
            Objects.requireNonNull(mathematicalScope, "mathematicalScope must not be null");
            Objects.requireNonNull(responseFields, "responseFields must not be null");
        }

        public String pinnedId() {
            return id + "@" + version;
        }
    }

    public record TaskShape(
            @JsonProperty("task_count") int taskCount,
            @JsonProperty("form") String form,
            @JsonProperty("multipart") String multipart,
            @JsonProperty("answer_choices") String answerChoices,
            @JsonProperty("context_story") String contextStory,
            @JsonProperty("proof") String proof,
            @JsonProperty("named_rule_cue") String namedRuleCue
    ) {
    }

    public record MathematicalScope(
            @JsonProperty("variable") String variable,
            @JsonProperty("expression_kind") String expressionKind,
            @JsonProperty("term_count") Range termCount,
            @JsonProperty("degree") Range degree,
            @JsonProperty("coefficients") CoefficientConstraints coefficients,
            @JsonProperty("require_nonzero_constant_term") boolean requireNonzeroConstantTerm
    ) {
        public MathematicalScope {
            Objects.requireNonNull(variable, "variable must not be null");
            Objects.requireNonNull(expressionKind, "expressionKind must not be null");
            Objects.requireNonNull(termCount, "termCount must not be null");
            Objects.requireNonNull(degree, "degree must not be null");
            Objects.requireNonNull(coefficients, "coefficients must not be null");
        }
    }

    public record Range(@JsonProperty("min") int min, @JsonProperty("max") int max) {
    }

    public record CoefficientConstraints(
            @JsonProperty("kind") String kind,
            @JsonProperty("min") int min,
            @JsonProperty("max") int max
    ) {
    }

    public record ResponseFields(
            @JsonProperty("final_derivative") String finalDerivative,
            @JsonProperty("rule_rationale") String ruleRationale
    ) {
    }

    public record ConceptSourcePack(
            @JsonProperty("id") String id,
            @JsonProperty("version") String version,
            @JsonProperty("passages") List<SourcePassage> passages
    ) {
        public ConceptSourcePack {
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
            @JsonProperty("exposed_task_fingerprints") List<String> exposedTaskFingerprints,
            @JsonProperty("exposed_solution_fingerprints") List<String> exposedSolutionFingerprints
    ) {
        public NoveltyExclusions {
            exposedTaskFingerprints = List.copyOf(exposedTaskFingerprints);
            exposedSolutionFingerprints = List.copyOf(exposedSolutionFingerprints);
        }
    }

    public record AnswerRepresentationContract(
            @JsonProperty("id") String id,
            @JsonProperty("version") String version,
            @JsonProperty("kind") String kind,
            @JsonProperty("variables") List<String> variables,
            @JsonProperty("accepted_input_families") List<String> acceptedInputFamilies
    ) {
        public AnswerRepresentationContract {
            variables = List.copyOf(variables);
            acceptedInputFamilies = List.copyOf(acceptedInputFamilies);
        }

        public String pinnedId() {
            return id + "@" + version;
        }
    }
}
