-- Ticket 01: replace the append-only assessment ledgers with the
-- exactly-once evaluation checkpoint keyed by Attempt, responsibility, and
-- evaluation version. There is intentionally no data migration.
DROP TABLE IF EXISTS teach_back_assessments;
DROP TABLE IF EXISTS assessments;

CREATE TABLE evaluation_results (
    id UUID PRIMARY KEY,
    attempt_id UUID NOT NULL REFERENCES attempts (id),
    responsibility VARCHAR(80) NOT NULL,
    evaluation_version VARCHAR(50) NOT NULL,
    result_schema VARCHAR(120) NOT NULL,
    result_payload JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    UNIQUE (attempt_id, responsibility, evaluation_version)
);
