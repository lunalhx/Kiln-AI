-- Ticket 11: the Learning Flow freezes the operator-owned Model Profile
-- (Strong/Small bindings plus the output-token ceiling) at start. The frozen
-- snapshot is recorded on the Flow and every later model call uses it, never
-- the current defaults.

ALTER TABLE flows ADD COLUMN model_profile JSONB NOT NULL DEFAULT '{}'::jsonb;
