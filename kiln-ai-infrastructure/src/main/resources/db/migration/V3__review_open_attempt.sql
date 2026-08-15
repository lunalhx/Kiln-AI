-- Inconclusive Review continuity: the Started Review Task points at its
-- single OPEN Review Attempt (the attempt that is currently awaiting a
-- submission). A replacement bound after an Inconclusive submission replaces
-- the pointer; an unprepared replacement clears it, leaving the Review
-- resumable through the start endpoint. At most one OPEN Attempt can therefore
-- ever be bound per Review Task.

ALTER TABLE review_tasks ADD COLUMN open_attempt_id UUID;
