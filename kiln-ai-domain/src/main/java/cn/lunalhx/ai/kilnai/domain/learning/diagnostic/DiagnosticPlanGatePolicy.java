package cn.lunalhx.ai.kilnai.domain.learning.diagnostic;

import cn.lunalhx.ai.kilnai.domain.gate.GateContext;
import cn.lunalhx.ai.kilnai.domain.gate.GatePolicy;
import cn.lunalhx.ai.kilnai.domain.gate.GateResult;
import cn.lunalhx.ai.kilnai.domain.gate.GateViolation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Type-specific Gate for Concept Preparation's Diagnostic Plan artifact.
 * Every rule is deterministic and evaluated against approved preparation
 * facts; the candidate cannot expand the Rubric, invent source authority, or
 * alter the hard Diagnostic Attempt ceiling.
 */
public final class DiagnosticPlanGatePolicy implements GatePolicy<DiagnosticPlan> {

    private static final int MAX_ATTEMPTS = 8;
    private final DiagnosticPlanGateContext context;

    public DiagnosticPlanGatePolicy(DiagnosticPlanGateContext context) {
        this.context = Objects.requireNonNull(context, "context must not be null");
    }

    @Override
    public GateResult<DiagnosticPlan> evaluate(DiagnosticPlan plan, GateContext gateContext) {
        List<GateViolation> violations = new ArrayList<>();
        if (!DiagnosticPlan.SCHEMA.equals(plan.schema())) {
            violations.add(new GateViolation("plan.schema", "unsupported Diagnostic Plan schema"));
        }
        if (plan.maximumAttempts() < 1 || plan.maximumAttempts() > MAX_ATTEMPTS) {
            violations.add(new GateViolation("attempts.maximum", "maximum Diagnostic Attempts must be between 1 and 8"));
        }
        if (!context.targetConceptId().equals(plan.targetConceptId())) {
            violations.add(new GateViolation("target.reference", "Plan target Concept does not match the prepared Concept"));
        }
        if (!context.conceptContractId().equals(plan.conceptContractId())
                || !context.conceptContractVersion().equals(plan.conceptContractVersion())) {
            violations.add(new GateViolation("contract.reference", "Plan must reference the accepted Concept Contract"));
        }
        if (!context.masteryRubricId().equals(plan.masteryRubricId())
                || !context.masteryRubricVersion().equals(plan.masteryRubricVersion())) {
            violations.add(new GateViolation("rubric.reference", "Plan must reference the accepted Mastery Rubric"));
        }

        Set<String> readiness = new HashSet<>(plan.targetReadinessCriterionIds());
        if (readiness.isEmpty()) {
            violations.add(new GateViolation("readiness.empty", "Target Readiness Set must not be empty"));
        }
        if (readiness.size() != plan.targetReadinessCriterionIds().size()) {
            violations.add(new GateViolation("readiness.duplicate", "Target Readiness Set must not repeat criteria"));
        }
        if (!context.masteryCriterionIds().containsAll(readiness)) {
            violations.add(new GateViolation("rubric.expansion", "Target Readiness Set cannot expand the Mastery Rubric"));
        }
        if (!readiness.containsAll(context.requiredTargetReadinessCriterionIds())) {
            violations.add(new GateViolation("readiness.unsafe", "Target Readiness Set omits a criterion required for safe Independent routing"));
        }
        if (!"all_target_readiness_criteria".equals(plan.coverageRule().kind())) {
            violations.add(new GateViolation("coverage.invalid", "Plan must declare complete Target Readiness coverage"));
        }
        if (!"early_stop_when_readiness_complete".equals(plan.terminationRule().kind())) {
            violations.add(new GateViolation("termination.invalid", "Plan must declare the accepted readiness termination rule"));
        }
        if (!approvedSource(plan.sourceBasis())) {
            violations.add(new GateViolation("source.ungrounded", "Plan source basis must reference approved source passages"));
        }
        validateRationale(plan, readiness, violations);
        validateSupportingConcepts(plan, violations);

        return violations.isEmpty() ? GateResult.passed(plan) : GateResult.rejected(violations);
    }

    private void validateRationale(
            DiagnosticPlan plan,
            Set<String> readiness,
            List<GateViolation> violations
    ) {
        DiagnosticPlan.RationalePolicy rationale = plan.rationalePolicy();
        if ("disabled".equals(rationale.mode())) {
            if (!rationale.criterionIds().isEmpty()) {
                violations.add(new GateViolation("rationale.unjustified", "disabled rationale cannot name criteria"));
            }
            return;
        }
        if (!"primary_or_corroborated".equals(rationale.mode())
                || rationale.criterionIds().isEmpty()
                || !readiness.containsAll(rationale.criterionIds())
                || !context.rationaleRelevantCriterionIds().containsAll(rationale.criterionIds())) {
            violations.add(new GateViolation("rationale.unjustified",
                    "enabled rationale requires a rationale-relevant Target Rubric mapping"));
        }
    }

    private void validateSupportingConcepts(DiagnosticPlan plan, List<GateViolation> violations) {
        Map<String, DiagnosticPlan.SupportingConcept> byId = new HashMap<>();
        for (DiagnosticPlan.SupportingConcept supporting : plan.supportingConcepts()) {
            if (byId.put(supporting.conceptId(), supporting) != null) {
                violations.add(new GateViolation("supporting.duplicate", "Supporting Concepts must be unique"));
            }
            DiagnosticPlanGateContext.SupportingConceptFacts approved =
                    context.supportingConcepts().get(supporting.conceptId());
            if (approved == null) {
                violations.add(new GateViolation("supporting.reference", "Plan names an unprepared Supporting Concept"));
                continue;
            }
            if (!approved.conceptId().equals(supporting.conceptId())
                    || !approved.masteryRubricId().equals(supporting.masteryRubricId())
                    || !approved.masteryRubricVersion().equals(supporting.masteryRubricVersion())
                    || !approved.masteryCriterionId().equals(supporting.masteryCriterionId())) {
                violations.add(new GateViolation("supporting.reference", "Supporting Concept references do not match preparation"));
            }
            if (!approvedSource(supporting.sourceBasis())) {
                violations.add(new GateViolation("source.ungrounded", "Supporting Concept source basis is not approved"));
            }
            if (!approvedSource(approved.sourceBasis())
                    || !approved.sourceBasis().equals(supporting.sourceBasis())) {
                violations.add(new GateViolation("supporting.source", "Supporting Concept must use its approved source basis"));
            }
        }

        Set<String> expectedOrder = byId.keySet();
        Set<String> actualOrder = new HashSet<>(plan.dependencyOrder());
        if (actualOrder.size() != plan.dependencyOrder().size() || !actualOrder.equals(expectedOrder)) {
            violations.add(new GateViolation("dependency.order", "dependency order must contain every prepared Supporting Concept exactly once"));
        }
        for (DiagnosticPlan.SupportingConcept supporting : byId.values()) {
            for (String dependency : supporting.dependencies()) {
                if (!byId.containsKey(dependency)) {
                    violations.add(new GateViolation("dependency.reference", "dependency must reference a declared Supporting Concept"));
                }
            }
        }
        if (hasCycle(byId)) {
            violations.add(new GateViolation("dependency.cycle", "Supporting Concept dependencies must be acyclic"));
        }
        Map<String, Integer> positions = new HashMap<>();
        for (int index = 0; index < plan.dependencyOrder().size(); index++) {
            positions.put(plan.dependencyOrder().get(index), index);
        }
        for (DiagnosticPlan.SupportingConcept supporting : byId.values()) {
            for (String dependency : supporting.dependencies()) {
                Integer dependencyPosition = positions.get(dependency);
                Integer conceptPosition = positions.get(supporting.conceptId());
                if (dependencyPosition != null && conceptPosition != null && dependencyPosition >= conceptPosition) {
                    violations.add(new GateViolation("dependency.order", "dependencies must precede dependent Concepts"));
                }
            }
        }
    }

    private boolean hasCycle(Map<String, DiagnosticPlan.SupportingConcept> concepts) {
        Set<String> visiting = new HashSet<>();
        Set<String> visited = new HashSet<>();
        for (String conceptId : concepts.keySet()) {
            if (hasCycle(conceptId, concepts, visiting, visited)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasCycle(
            String conceptId,
            Map<String, DiagnosticPlan.SupportingConcept> concepts,
            Set<String> visiting,
            Set<String> visited
    ) {
        if (visited.contains(conceptId)) {
            return false;
        }
        if (!visiting.add(conceptId)) {
            return true;
        }
        for (String dependency : concepts.get(conceptId).dependencies()) {
            if (concepts.containsKey(dependency) && hasCycle(dependency, concepts, visiting, visited)) {
                return true;
            }
        }
        visiting.remove(conceptId);
        visited.add(conceptId);
        return false;
    }

    private boolean approvedSource(DiagnosticPlan.SourceBasis source) {
        return !source.passageIds().isEmpty()
                && context.approvedSourceBases().stream().anyMatch(approved ->
                approved.sourcePackId().equals(source.sourcePackId())
                        && approved.sourcePackVersion().equals(source.sourcePackVersion())
                        && approved.passageIds().containsAll(source.passageIds()));
    }
}
