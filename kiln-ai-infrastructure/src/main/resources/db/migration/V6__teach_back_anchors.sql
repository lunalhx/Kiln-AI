-- Teach-back anchors, teach-back task packages, and isolated Teach-back
-- assessments. The anchored Teach-back slice keeps its packages separate from
-- Apply packages (their private projection has no canonical expected answer),
-- so the attempt reference to apply_packages is relaxed to a plain id and the
-- package-type discriminator is the package's presence in exactly one of the
-- two package tables. The anchor ledger makes the Guard's eligibility durable
-- across restarts.

CREATE TABLE apply_teach_back_anchors (
    flow_id UUID NOT NULL REFERENCES apply_flows (id),
    anchor_id UUID NOT NULL,
    anchor_kind VARCHAR(40) NOT NULL,
    exposed_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (flow_id, anchor_id)
);

ALTER TABLE apply_attempts DROP CONSTRAINT apply_attempts_task_package_id_fkey;

CREATE TABLE apply_teach_back_packages (
    id UUID PRIMARY KEY,
    attempt_purpose VARCHAR(30) NOT NULL,
    learner_projection JSONB NOT NULL,
    private_projection JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE apply_teach_back_assessments (
    id UUID PRIMARY KEY,
    attempt_id UUID NOT NULL REFERENCES apply_attempts (id),
    assessment JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);
