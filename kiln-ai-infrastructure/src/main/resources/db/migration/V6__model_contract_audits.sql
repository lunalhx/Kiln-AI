-- Durable model-boundary audit: identity, responsibility, normalized
-- violation codes, repair count, correlation ID, and provider-health category.
-- Rejected Task Packages are never persisted as packages rows, so task_package_id
-- has no foreign key, matching the verifications ledger.

CREATE TABLE model_contract_audits (
    id UUID PRIMARY KEY,
    flow_id UUID,
    attempt_id UUID,
    task_package_id UUID,
    responsibility VARCHAR(80) NOT NULL,
    violation_codes JSONB NOT NULL,
    repair_count SMALLINT NOT NULL,
    correlation_id VARCHAR(64) NOT NULL,
    provider_category VARCHAR(80) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);
