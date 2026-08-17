-- Durable Pending Operation of an Unavailable Interaction (ADR-0069):
-- one saved resume description per Flow, replaced on each failed retry
-- and cleared when the next interaction commits or the learner leaves.
CREATE TABLE pending_operations (
    flow_id UUID PRIMARY KEY REFERENCES flows (id),
    operation JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);
