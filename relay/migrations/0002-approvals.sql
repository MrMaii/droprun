CREATE TABLE IF NOT EXISTS approvals (
 id TEXT PRIMARY KEY, task_id TEXT NOT NULL, thread_id TEXT NOT NULL, turn_id TEXT NOT NULL,
 item_id TEXT NOT NULL, details TEXT NOT NULL, status TEXT NOT NULL DEFAULT 'pending',
 created_at INTEGER NOT NULL, expires_at INTEGER NOT NULL, decided_at INTEGER
);
CREATE INDEX IF NOT EXISTS approvals_task_status ON approvals(task_id, status);
