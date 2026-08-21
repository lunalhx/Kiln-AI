package cn.lunalhx.ai.kilnai.domain.learning.diagnostic;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * The application-owned terminal result of one Diagnostic stage, derived from
 * the frozen Plan and accumulated Findings rather than the last Attempt alone.
 */
public final class DiagnosticRoutingDecision {

    public enum Route {
        CONTINUE_DIAGNOSTIC,
        FRESH_INDEPENDENT_TEST,
        TARGET_LEARNING_WITH_SUMMARY,
        TARGET_LEARNING_NEUTRAL
    }

    private DiagnosticRoutingDecision() {
    }

    public static Route decide(DiagnosticPlan plan, List<DiagnosticFinding> findings) {
        Objects.requireNonNull(plan, "plan must not be null");
        Objects.requireNonNull(findings, "findings must not be null");
        if (!plan.supportingConcepts().isEmpty()) {
            throw new IllegalStateException("ticket 02 tracer does not route Required Supporting Concepts");
        }
        if (findings.stream().anyMatch(finding -> finding.kind() == DiagnosticFinding.Kind.CONCLUSIVE_GAP)) {
            return Route.TARGET_LEARNING_WITH_SUMMARY;
        }
        Set<String> confirmed = new HashSet<>();
        for (DiagnosticFinding finding : findings) {
            if (finding.kind() == DiagnosticFinding.Kind.PASSING_OBSERVATION) {
                confirmed.addAll(finding.coveredCriterionIds());
            }
        }
        if (confirmed.containsAll(plan.targetReadinessCriterionIds())
                && !plan.targetReadinessCriterionIds().isEmpty()) {
            return Route.FRESH_INDEPENDENT_TEST;
        }
        return findings.size() < plan.maximumAttempts()
                ? Route.CONTINUE_DIAGNOSTIC
                : Route.TARGET_LEARNING_NEUTRAL;
    }

    public static String learnerSafeSummary(List<DiagnosticFinding> findings) {
        Objects.requireNonNull(findings, "findings must not be null");
        List<String> strengths = findings.stream()
                .filter(finding -> finding.kind() == DiagnosticFinding.Kind.PASSING_OBSERVATION)
                .flatMap(finding -> finding.coveredCriterionIds().stream())
                .distinct()
                .toList();
        List<String> priorities = findings.stream()
                .filter(finding -> finding.kind() == DiagnosticFinding.Kind.CONCLUSIVE_GAP)
                .flatMap(finding -> {
                    List<String> missing = finding.missingCriteria();
                    return missing.isEmpty() ? finding.coveredCriterionIds().stream() : missing.stream();
                })
                .distinct()
                .toList();
        StringBuilder message = new StringBuilder("诊断摘要：");
        if (!strengths.isEmpty()) {
            message.append("已确认 ").append(String.join("、", strengths)).append("。");
        }
        if (!priorities.isEmpty()) {
            message.append("教学重点：").append(String.join("、", priorities)).append("。");
        }
        message.append("未探测的内容仍为未知。");
        return message.toString();
    }
}
