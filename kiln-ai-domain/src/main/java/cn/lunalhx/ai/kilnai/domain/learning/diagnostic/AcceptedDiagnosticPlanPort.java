package cn.lunalhx.ai.kilnai.domain.learning.diagnostic;

import java.util.Optional;
import java.util.UUID;

/**
 * Read-only lookup of the already Gate-accepted Plan for one Target Concept.
 * Start uses the returned immutable value as the snapshot to freeze onto a
 * Flow; it never expands or rewrites the Plan at runtime.
 */
public interface AcceptedDiagnosticPlanPort {

    Optional<DiagnosticPlan> acceptedFor(UUID targetConceptId);
}
