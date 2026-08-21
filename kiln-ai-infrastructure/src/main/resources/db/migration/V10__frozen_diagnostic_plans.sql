-- Ticket 01: bind the Gate-accepted Diagnostic Plan and its learner-safe
-- completed/max counter to the Flow in the same transaction as the initial
-- package, Attempt, interaction, checkpoint, and command.
CREATE TABLE diagnostic_plans (
    flow_id UUID PRIMARY KEY REFERENCES flows (id),
    plan_json JSONB NOT NULL,
    completed_attempts INTEGER NOT NULL DEFAULT 0 CHECK (completed_attempts >= 0),
    created_at TIMESTAMPTZ NOT NULL
);
