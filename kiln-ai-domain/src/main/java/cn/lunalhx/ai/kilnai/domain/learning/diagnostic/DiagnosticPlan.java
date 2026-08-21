package cn.lunalhx.ai.kilnai.domain.learning.diagnostic;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * The frozen, versioned preparation artifact that bounds one Diagnostic
 * stage. It is private Flow state; learner projections use only its attempt
 * maximum and never expose the plan's source or routing details.
 */
public record DiagnosticPlan(
        String schema,
        String id,
        String version,
        UUID targetConceptId,
        String conceptContractId,
        String conceptContractVersion,
        String masteryRubricId,
        String masteryRubricVersion,
        List<String> targetReadinessCriterionIds,
        List<SupportingConcept> supportingConcepts,
        List<String> dependencyOrder,
        SourceBasis sourceBasis,
        CoverageRule coverageRule,
        TerminationRule terminationRule,
        RationalePolicy rationalePolicy,
        int maximumAttempts
) {

    public static final String SCHEMA = "diagnostic_plan/v1";

    public DiagnosticPlan {
        Objects.requireNonNull(schema, "schema must not be null");
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(version, "version must not be null");
        Objects.requireNonNull(targetConceptId, "targetConceptId must not be null");
        Objects.requireNonNull(conceptContractId, "conceptContractId must not be null");
        Objects.requireNonNull(conceptContractVersion, "conceptContractVersion must not be null");
        Objects.requireNonNull(masteryRubricId, "masteryRubricId must not be null");
        Objects.requireNonNull(masteryRubricVersion, "masteryRubricVersion must not be null");
        Objects.requireNonNull(targetReadinessCriterionIds, "targetReadinessCriterionIds must not be null");
        Objects.requireNonNull(supportingConcepts, "supportingConcepts must not be null");
        Objects.requireNonNull(dependencyOrder, "dependencyOrder must not be null");
        Objects.requireNonNull(sourceBasis, "sourceBasis must not be null");
        Objects.requireNonNull(coverageRule, "coverageRule must not be null");
        Objects.requireNonNull(terminationRule, "terminationRule must not be null");
        Objects.requireNonNull(rationalePolicy, "rationalePolicy must not be null");
        targetReadinessCriterionIds = List.copyOf(targetReadinessCriterionIds);
        supportingConcepts = List.copyOf(supportingConcepts);
        dependencyOrder = List.copyOf(dependencyOrder);
    }

    public String pinnedId() {
        return id + "@" + version;
    }

    public record SupportingConcept(
            String conceptId,
            boolean required,
            String masteryRubricId,
            String masteryRubricVersion,
            String masteryCriterionId,
            SourceBasis sourceBasis,
            List<String> dependencies
    ) {

        public SupportingConcept {
            Objects.requireNonNull(conceptId, "conceptId must not be null");
            Objects.requireNonNull(masteryRubricId, "masteryRubricId must not be null");
            Objects.requireNonNull(masteryRubricVersion, "masteryRubricVersion must not be null");
            Objects.requireNonNull(masteryCriterionId, "masteryCriterionId must not be null");
            Objects.requireNonNull(sourceBasis, "sourceBasis must not be null");
            Objects.requireNonNull(dependencies, "dependencies must not be null");
            dependencies = List.copyOf(dependencies);
        }
    }

    public record SourceBasis(
            String sourcePackId,
            String sourcePackVersion,
            List<String> passageIds
    ) {

        public SourceBasis {
            Objects.requireNonNull(sourcePackId, "sourcePackId must not be null");
            Objects.requireNonNull(sourcePackVersion, "sourcePackVersion must not be null");
            Objects.requireNonNull(passageIds, "passageIds must not be null");
            passageIds = List.copyOf(passageIds);
        }
    }

    public record CoverageRule(String kind) {
        public CoverageRule {
            Objects.requireNonNull(kind, "kind must not be null");
        }
    }

    public record TerminationRule(String kind) {
        public TerminationRule {
            Objects.requireNonNull(kind, "kind must not be null");
        }
    }

    public record RationalePolicy(String mode, List<String> criterionIds) {
        public RationalePolicy {
            Objects.requireNonNull(mode, "mode must not be null");
            Objects.requireNonNull(criterionIds, "criterionIds must not be null");
            criterionIds = List.copyOf(criterionIds);
        }
    }
}
