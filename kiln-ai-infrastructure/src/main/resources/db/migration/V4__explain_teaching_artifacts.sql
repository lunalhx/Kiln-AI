-- Explain teaching artifacts and the example-fingerprint exposure ledger.
-- Explain is a pure teaching action: its durable artifact separates the
-- learner-visible projection from the private source trace, example
-- Fingerprint, and execution trace, and the exposure ledger records every
-- displayed worked example for later novelty checks. The teaching boundary is
-- a first-class learner interaction, so the interaction row also carries the
-- learner-visible teaching projection across restarts.

ALTER TABLE apply_interactions ADD COLUMN teaching_projection JSONB;

CREATE TABLE apply_explain_artifacts (
    id UUID PRIMARY KEY,
    flow_id UUID NOT NULL REFERENCES apply_flows (id),
    artifact JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE apply_example_exposures (
    flow_id UUID NOT NULL REFERENCES apply_flows (id),
    example_fingerprint VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (flow_id, example_fingerprint)
);
