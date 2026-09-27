CREATE TABLE IF NOT EXISTS pairing_codes (
 id INTEGER PRIMARY KEY CHECK(id=1), code_hash TEXT NOT NULL,
 created_at INTEGER NOT NULL, expires_at INTEGER NOT NULL, consumed_at INTEGER, device_id TEXT
);
