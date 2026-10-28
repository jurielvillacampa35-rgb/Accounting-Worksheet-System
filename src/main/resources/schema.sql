PRAGMA foreign_keys = ON;

CREATE TABLE IF NOT EXISTS users (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    username TEXT NOT NULL UNIQUE COLLATE NOCASE,
    password_salt TEXT NOT NULL,
    password_hash TEXT NOT NULL,
    role TEXT NOT NULL DEFAULT 'USER' CHECK (role IN ('USER', 'ADMIN')),
    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS worksheets (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL REFERENCES users(id),
    title TEXT NOT NULL,
    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS worksheet_entries (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    worksheet_id INTEGER NOT NULL REFERENCES worksheets(id) ON DELETE CASCADE,
    line_number INTEGER NOT NULL,
    entry_date TEXT NOT NULL DEFAULT '',
    account_name TEXT NOT NULL,
    debit TEXT NOT NULL,
    credit TEXT NOT NULL,
    UNIQUE (worksheet_id, line_number)
);

CREATE INDEX IF NOT EXISTS idx_worksheets_user_created
    ON worksheets(user_id, created_at DESC);

CREATE TABLE IF NOT EXISTS adjustment_worksheets (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL REFERENCES users(id),
    title TEXT NOT NULL,
    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS adjustment_entries (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    worksheet_id INTEGER NOT NULL REFERENCES adjustment_worksheets(id) ON DELETE CASCADE,
    line_number INTEGER NOT NULL,
    account_name TEXT NOT NULL,
    account_type TEXT NOT NULL,
    trial_debit TEXT NOT NULL,
    trial_credit TEXT NOT NULL,
    adjustment_debit TEXT NOT NULL,
    adjustment_credit TEXT NOT NULL,
    UNIQUE (worksheet_id, line_number)
);

CREATE INDEX IF NOT EXISTS idx_adjustment_worksheets_user_created
    ON adjustment_worksheets(user_id, created_at DESC);
