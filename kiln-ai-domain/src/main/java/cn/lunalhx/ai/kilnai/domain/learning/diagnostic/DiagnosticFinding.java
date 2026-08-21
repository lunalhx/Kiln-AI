package cn.lunalhx.ai.kilnai.domain.learning.diagnostic;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * A validated, Flow-scoped, non-evidentiary observation from one submitted
 * Diagnostic Attempt. It never becomes Learning Evidence or a mastery claim.
 */
public record DiagnosticFinding(
        UUID findingId,
        UUID flowId,
        UUID attemptId,
        Kind kind,
        List<String> coveredCriterionIds,
        List<String> missingCriteria,
        List<String> errorDimensions,
        Instant recordedAt
) {

    public enum Kind {
        PASSING_OBSERVATION,
        CONCLUSIVE_GAP,
        UNCONFIRMED_PERFORMANCE
    }

    public DiagnosticFinding {
        Objects.requireNonNull(findingId, "findingId must not be null");
        Objects.requireNonNull(flowId, "flowId must not be null");
        Objects.requireNonNull(attemptId, "attemptId must not be null");
        Objects.requireNonNull(kind, "kind must not be null");
        Objects.requireNonNull(coveredCriterionIds, "coveredCriterionIds must not be null");
        Objects.requireNonNull(missingCriteria, "missingCriteria must not be null");
        Objects.requireNonNull(errorDimensions, "errorDimensions must not be null");
        Objects.requireNonNull(recordedAt, "recordedAt must not be null");
        coveredCriterionIds = List.copyOf(coveredCriterionIds);
        missingCriteria = List.copyOf(missingCriteria);
        errorDimensions = List.copyOf(errorDimensions);
    }
}
