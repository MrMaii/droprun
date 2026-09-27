ALTER TABLE tasks ADD COLUMN parent_task_id TEXT;
CREATE INDEX IF NOT EXISTS tasks_thread_status ON tasks(thread_id, status);
