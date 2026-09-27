ALTER TABLE devices ADD COLUMN direct_execution INTEGER NOT NULL DEFAULT 0 CHECK(direct_execution IN (0,1));
ALTER TABLE tasks ADD COLUMN execution_mode TEXT NOT NULL DEFAULT 'legacy-isolated' CHECK(execution_mode IN ('legacy-isolated','review','direct'));
ALTER TABLE tasks ADD COLUMN plan_report TEXT;
ALTER TABLE tasks ADD COLUMN plan_turn_id TEXT;
ALTER TABLE tasks ADD COLUMN plan_version TEXT;
ALTER TABLE tasks ADD COLUMN plan_decision TEXT;
ALTER TABLE tasks ADD COLUMN plan_decided_at INTEGER;
