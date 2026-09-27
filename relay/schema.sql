CREATE TABLE IF NOT EXISTS devices (id TEXT PRIMARY KEY, token_hash TEXT NOT NULL UNIQUE, created_at INTEGER NOT NULL, direct_execution INTEGER NOT NULL DEFAULT 0 CHECK(direct_execution IN (0,1)));
CREATE TABLE IF NOT EXISTS pairing_codes (
 id INTEGER PRIMARY KEY CHECK(id=1), code_hash TEXT NOT NULL,
 created_at INTEGER NOT NULL, expires_at INTEGER NOT NULL, consumed_at INTEGER, device_id TEXT
);
CREATE TABLE IF NOT EXISTS state (key TEXT PRIMARY KEY, value TEXT NOT NULL);
CREATE TABLE IF NOT EXISTS tasks (
 id TEXT PRIMARY KEY, device_id TEXT NOT NULL, project_id TEXT NOT NULL, project_name TEXT NOT NULL,
 content TEXT NOT NULL, message TEXT NOT NULL, assets TEXT NOT NULL DEFAULT '[]',
 status TEXT NOT NULL DEFAULT 'queued', thread_id TEXT, turn_id TEXT, report TEXT,
 error TEXT, events TEXT NOT NULL DEFAULT '[]', created_at INTEGER NOT NULL, updated_at INTEGER NOT NULL,
 cancel_requested INTEGER NOT NULL DEFAULT 0, parent_task_id TEXT, permission_version TEXT,
 execution_mode TEXT NOT NULL DEFAULT 'legacy-isolated' CHECK(execution_mode IN ('legacy-isolated','review','direct')),
 plan_report TEXT, plan_turn_id TEXT, plan_version TEXT, plan_decision TEXT, plan_decided_at INTEGER,
 model TEXT, effort TEXT, title TEXT, material_summary TEXT,
 preview_url TEXT, preview_expires_at INTEGER,
 preview_status TEXT, preview_kind TEXT, preview_version TEXT, preview_requested_at INTEGER,
 sync_version INTEGER NOT NULL DEFAULT 0,
 root_task_id TEXT, history_incomplete INTEGER NOT NULL DEFAULT 0,
 terminal_at INTEGER
);
CREATE INDEX IF NOT EXISTS tasks_status ON tasks(status, created_at);
CREATE INDEX IF NOT EXISTS tasks_thread_status ON tasks(thread_id, status);
CREATE TABLE IF NOT EXISTS uploads (id TEXT PRIMARY KEY, device_id TEXT NOT NULL, name TEXT NOT NULL, mime TEXT NOT NULL, size INTEGER NOT NULL);
CREATE TABLE IF NOT EXISTS deliverables (
 task_id TEXT NOT NULL, id TEXT NOT NULL, name TEXT NOT NULL, kind TEXT NOT NULL,
 sha256 TEXT NOT NULL, size INTEGER NOT NULL, ready INTEGER NOT NULL DEFAULT 0,
 created_at INTEGER NOT NULL, PRIMARY KEY(task_id,id)
);
CREATE TABLE IF NOT EXISTS approvals (
 id TEXT PRIMARY KEY, task_id TEXT NOT NULL, thread_id TEXT NOT NULL, turn_id TEXT NOT NULL,
 item_id TEXT NOT NULL, details TEXT NOT NULL, status TEXT NOT NULL DEFAULT 'pending',
 created_at INTEGER NOT NULL, expires_at INTEGER NOT NULL, decided_at INTEGER
);
CREATE INDEX IF NOT EXISTS approvals_task_status ON approvals(task_id, status);
CREATE TABLE IF NOT EXISTS project_permissions (
 device_id TEXT NOT NULL, project_id TEXT NOT NULL, enabled INTEGER NOT NULL DEFAULT 0,
 version TEXT NOT NULL, updated_at INTEGER NOT NULL, PRIMARY KEY(device_id,project_id)
);
CREATE INDEX IF NOT EXISTS tasks_device_project_history ON tasks(device_id,project_id,created_at DESC,id DESC);
CREATE INDEX IF NOT EXISTS tasks_device_root ON tasks(device_id,root_task_id);
CREATE TABLE IF NOT EXISTS upload_lifecycle (upload_id TEXT PRIMARY KEY, created_at INTEGER NOT NULL);
CREATE TABLE IF NOT EXISTS preview_snapshots (
 id TEXT PRIMARY KEY, task_id TEXT NOT NULL, version TEXT NOT NULL, manifest TEXT NOT NULL,
 created_at INTEGER NOT NULL, expires_at INTEGER NOT NULL, ready INTEGER NOT NULL DEFAULT 0,
 UNIQUE(task_id,version)
);
CREATE TABLE IF NOT EXISTS preview_files (
 snapshot_id TEXT NOT NULL, path TEXT NOT NULL, sha256 TEXT NOT NULL, size INTEGER NOT NULL,
 content_type TEXT NOT NULL, ready INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(snapshot_id,path)
);
CREATE TABLE IF NOT EXISTS preview_sessions (
 token_hash TEXT PRIMARY KEY, snapshot_id TEXT NOT NULL, expires_at INTEGER NOT NULL
);
CREATE INDEX IF NOT EXISTS preview_sessions_expiry ON preview_sessions(expires_at);
INSERT INTO state(key,value) VALUES ('schema_version','11') ON CONFLICT(key) DO NOTHING;
CREATE TRIGGER IF NOT EXISTS tasks_identity_insert AFTER INSERT ON tasks BEGIN UPDATE tasks SET root_task_id=COALESCE(NEW.root_task_id,(SELECT root_task_id FROM tasks WHERE id=NEW.parent_task_id),NEW.parent_task_id,NEW.id), history_incomplete=COALESCE((SELECT history_incomplete FROM tasks WHERE id=NEW.parent_task_id),NEW.history_incomplete), terminal_at=CASE WHEN NEW.status IN ('completed','blocked','failed','cancelled') THEN NEW.updated_at ELSE NULL END WHERE id=NEW.id; END;
CREATE TRIGGER IF NOT EXISTS tasks_terminal_update AFTER UPDATE OF status ON tasks WHEN NEW.status IN ('completed','blocked','failed','cancelled') AND NEW.terminal_at IS NULL BEGIN UPDATE tasks SET terminal_at=NEW.updated_at WHERE id=NEW.id; UPDATE preview_snapshots SET expires_at=NEW.updated_at+(expires_at-created_at) WHERE task_id=NEW.id; END;
INSERT INTO state(key,value) VALUES ('task_sync_version','0') ON CONFLICT(key) DO NOTHING;
CREATE TRIGGER IF NOT EXISTS tasks_sync_insert AFTER INSERT ON tasks BEGIN UPDATE state SET value=CAST(value AS INTEGER)+1 WHERE key='task_sync_version'; UPDATE tasks SET sync_version=(SELECT CAST(value AS INTEGER) FROM state WHERE key='task_sync_version') WHERE id=NEW.id; END;
CREATE TRIGGER IF NOT EXISTS tasks_sync_update AFTER UPDATE ON tasks WHEN NEW.sync_version=OLD.sync_version BEGIN UPDATE state SET value=CAST(value AS INTEGER)+1 WHERE key='task_sync_version'; UPDATE tasks SET sync_version=(SELECT CAST(value AS INTEGER) FROM state WHERE key='task_sync_version') WHERE id=NEW.id; END;
