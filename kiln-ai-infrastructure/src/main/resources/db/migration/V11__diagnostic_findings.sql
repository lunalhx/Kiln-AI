-- Ticket 02: Flow-scoped Diagnostic Findings, distinct from Learning Evidence.
CREATE TABLE diagnostic_findings (
    finding_id UUID PRIMARY KEY,
    flow_id UUID NOT NULL REFERENCES flows (id),
    attempt_id UUID NOT NULL UNIQUE,
    kind TEXT NOT NULL,
    covered_criterion_ids JSONB NOT NULL,
    missing_criteria JSONB NOT NULL,
    error_dimensions JSONB NOT NULL,
    recorded_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX diagnostic_findings_flow_id_recorded_at
    ON diagnostic_findings (flow_id, recorded_at, finding_id);
