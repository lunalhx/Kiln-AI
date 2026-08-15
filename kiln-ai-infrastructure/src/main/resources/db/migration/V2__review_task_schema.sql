-- Delayed Review cadence: durable Review Task coordination state. Review
-- Tasks are application-owned work items, never Task Packages; milestones are
-- projected only from apply_evidence. At most one unfinished Review Task
-- (SCHEDULED, DUE, or STARTED) may exist per learner and Concept.

CREATE TABLE review_tasks (
    id UUID PRIMARY KEY,
    learner_id UUID NOT NULL,
    concept_id UUID NOT NULL,
    flow_id UUID NOT NULL REFERENCES apply_flows (id),
    review_number SMALLINT NOT NULL CHECK (review_number BETWEEN 1 AND 4),
    status VARCHAR(20) NOT NULL,
    due_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    cancelled_at TIMESTAMPTZ
);

CREATE UNIQUE INDEX review_tasks_one_unfinished_per_learner_concept
    ON review_tasks (learner_id, concept_id)
    WHERE status IN ('SCHEDULED', 'DUE', 'STARTED');
