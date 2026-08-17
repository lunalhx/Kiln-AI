-- Ticket 02: one Active Learning Work claim per learner and Target Concept
-- (ADR-0070). A Flow holds the claim while it is non-terminal; the store
-- mirrors every committed interaction's status (AWAITING_LEARNER_INPUT or
-- TERMINAL) onto flows.status, so a terminal Flow releases the claim and a
-- fresh Diagnostic may start. The partial unique index is the database-level
-- concurrency guard behind the domain invariant, paired with the existing
-- unfinished-Review index on review_tasks.
CREATE UNIQUE INDEX flows_one_active_per_learner_concept
    ON flows (learner_id, concept_id)
    WHERE status <> 'TERMINAL';
