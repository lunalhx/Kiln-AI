-- Ticket 08: one database-owned Active Learning Work claim spans both a
-- non-terminal Flow and an unfinished Review Task.  The claim is deliberately
-- created without a data backfill: this schema is for a fresh destructive
-- baseline and does not migrate historical Flow or cadence rows.
CREATE TABLE active_learning_work (
    learner_id UUID NOT NULL,
    concept_id UUID NOT NULL,
    -- The claim is inserted before the Flow inside the same transaction so
    -- the unique row is the concurrency boundary for Start. The transaction
    -- either creates both rows or rolls both back.
    flow_id UUID NOT NULL,
    claimed_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (learner_id, concept_id),
    UNIQUE (flow_id)
);
