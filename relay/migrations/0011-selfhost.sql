-- Upgrade an existing schema at 0010 only. Back up D1 before applying once.
ALTER TABLE tasks ADD COLUMN root_task_id TEXT;
ALTER TABLE tasks ADD COLUMN history_incomplete INTEGER NOT NULL DEFAULT 0;
ALTER TABLE tasks ADD COLUMN terminal_at INTEGER;
WITH RECURSIVE lineage(task_id,id,parent_task_id,depth,seen) AS (
 SELECT id,id,parent_task_id,0,','||id||',' FROM tasks
 UNION ALL
 SELECT l.task_id,p.id,p.parent_task_id,l.depth+1,l.seen||p.id||',' FROM lineage l JOIN tasks p ON p.id=l.parent_task_id WHERE l.depth<128 AND instr(l.seen,','||p.id||',')=0
), roots AS (
 SELECT task_id,id,parent_task_id,ROW_NUMBER() OVER(PARTITION BY task_id ORDER BY depth DESC) AS rank FROM lineage
)
UPDATE tasks SET root_task_id=(SELECT COALESCE(r.parent_task_id,r.id) FROM roots r WHERE r.task_id=tasks.id AND r.rank=1),history_incomplete=(SELECT CASE WHEN r.parent_task_id IS NULL THEN 0 ELSE 1 END FROM roots r WHERE r.task_id=tasks.id AND r.rank=1),terminal_at=CASE WHEN status IN ('completed','blocked','failed','cancelled') THEN updated_at ELSE NULL END;
CREATE INDEX IF NOT EXISTS tasks_device_project_history ON tasks(device_id,project_id,created_at DESC,id DESC);
CREATE INDEX IF NOT EXISTS tasks_device_root ON tasks(device_id,root_task_id);
CREATE TABLE IF NOT EXISTS upload_lifecycle (upload_id TEXT PRIMARY KEY, created_at INTEGER NOT NULL);
INSERT INTO upload_lifecycle(upload_id,created_at) SELECT id,CAST(strftime('%s','now') AS INTEGER)*1000 FROM uploads;
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
INSERT INTO state(key,value) VALUES ('schema_version','11') ON CONFLICT(key) DO UPDATE SET value=excluded.value;
CREATE TRIGGER IF NOT EXISTS tasks_identity_insert AFTER INSERT ON tasks BEGIN UPDATE tasks SET root_task_id=COALESCE(NEW.root_task_id,(SELECT root_task_id FROM tasks WHERE id=NEW.parent_task_id),NEW.parent_task_id,NEW.id), history_incomplete=COALESCE((SELECT history_incomplete FROM tasks WHERE id=NEW.parent_task_id),NEW.history_incomplete), terminal_at=CASE WHEN NEW.status IN ('completed','blocked','failed','cancelled') THEN NEW.updated_at ELSE NULL END WHERE id=NEW.id; END;
CREATE TRIGGER IF NOT EXISTS tasks_terminal_update AFTER UPDATE OF status ON tasks WHEN NEW.status IN ('completed','blocked','failed','cancelled') AND NEW.terminal_at IS NULL BEGIN UPDATE tasks SET terminal_at=NEW.updated_at WHERE id=NEW.id; UPDATE preview_snapshots SET expires_at=NEW.updated_at+(expires_at-created_at) WHERE task_id=NEW.id; END;
