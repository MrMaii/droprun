ALTER TABLE tasks ADD COLUMN sync_version INTEGER NOT NULL DEFAULT 0;
INSERT INTO state(key,value) VALUES ('task_sync_version','0') ON CONFLICT(key) DO NOTHING;
CREATE TRIGGER IF NOT EXISTS tasks_sync_insert AFTER INSERT ON tasks BEGIN UPDATE state SET value=CAST(value AS INTEGER)+1 WHERE key='task_sync_version'; UPDATE tasks SET sync_version=(SELECT CAST(value AS INTEGER) FROM state WHERE key='task_sync_version') WHERE id=NEW.id; END;
CREATE TRIGGER IF NOT EXISTS tasks_sync_update AFTER UPDATE ON tasks WHEN NEW.sync_version=OLD.sync_version BEGIN UPDATE state SET value=CAST(value AS INTEGER)+1 WHERE key='task_sync_version'; UPDATE tasks SET sync_version=(SELECT CAST(value AS INTEGER) FROM state WHERE key='task_sync_version') WHERE id=NEW.id; END;
UPDATE tasks SET sync_version=sync_version WHERE sync_version=0;
