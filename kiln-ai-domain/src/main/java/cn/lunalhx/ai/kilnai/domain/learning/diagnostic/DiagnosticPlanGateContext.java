package cn.lunalhx.ai.kilnai.domain.learning.diagnostic;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * The approved source and Concept facts against which a generated
 * Diagnostic Plan is validated. It contains no learner state and is supplied
 * by Concept Preparation, not by the model-produced Plan.
 */
public record DiagnosticPlanGateContext(
        UUID targetConceptId,
        String conceptContractId,
        String conceptContractVersion,
        String masteryRubricId,
        String masteryRubricVersion,
        Set<String> masteryCriterionIds,
        Set<String> requiredTargetReadinessCriterionIds,
        Set<String> rationaleRelevantCriterionIds,
        Set<DiagnosticPlan.SourceBasis> approvedSourceBases,
        Map<String, SupportingConceptFacts> supportingConcepts
) {

    public DiagnosticPlanGateContext {
        Objects.requireNonNull(targetConceptId, "targetConceptId must not be null");
        Objects.requireNonNull(conceptContractId, "conceptContractId must not be null");
        Objects.requireNonNull(conceptContractVersion, "conceptContractVersion must not be null");
        Objects.requireNonNull(masteryRubricId, "masteryRubricId must not be null");
        Objects.requireNonNull(masteryRubricVersion, "masteryRubricVersion must not be null");
        Objects.requireNonNull(masteryCriterionIds, "masteryCriterionIds must not be null");
        Objects.requireNonNull(requiredTargetReadinessCriterionIds,
                "requiredTargetReadinessCriterionIds must not be null");
        Objects.requireNonNull(rationaleRelevantCriterionIds, "rationaleRelevantCriterionIds must not be null");
        Objects.requireNonNull(approvedSourceBases, "approvedSourceBases must not be null");
        Objects.requireNonNull(supportingConcepts, "supportingConcepts must not be null");
        masteryCriterionIds = Set.copyOf(masteryCriterionIds);
        requiredTargetReadinessCriterionIds = Set.copyOf(requiredTargetReadinessCriterionIds);
        rationaleRelevantCriterionIds = Set.copyOf(rationaleRelevantCriterionIds);
        approvedSourceBases = Set.copyOf(approvedSourceBases);
        supportingConcepts = Map.copyOf(supportingConcepts);
    }

    public record SupportingConceptFacts(
            String conceptId,
            String masteryRubricId,
            String masteryRubricVersion,
            String masteryCriterionId,
            DiagnosticPlan.SourceBasis sourceBasis
    ) {
        public SupportingConceptFacts {
            Objects.requireNonNull(conceptId, "conceptId must not be null");
            Objects.requireNonNull(masteryRubricId, "masteryRubricId must not be null");
            Objects.requireNonNull(masteryRubricVersion, "masteryRubricVersion must not be null");
            Objects.requireNonNull(masteryCriterionId, "masteryCriterionId must not be null");
            Objects.requireNonNull(sourceBasis, "sourceBasis must not be null");
        }
    }
}
