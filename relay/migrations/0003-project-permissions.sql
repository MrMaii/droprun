ALTER TABLE tasks ADD COLUMN permission_version TEXT;
CREATE TABLE IF NOT EXISTS project_permissions (
 device_id TEXT NOT NULL, project_id TEXT NOT NULL, enabled INTEGER NOT NULL DEFAULT 0,
 version TEXT NOT NULL, updated_at INTEGER NOT NULL, PRIMARY KEY(device_id,project_id)
);
