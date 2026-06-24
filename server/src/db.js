const Database = require('better-sqlite3');
const path = require('path');

const db = new Database(path.join(__dirname, '..', 'shoplist.db'));
db.pragma('journal_mode = WAL');

db.exec(`
  CREATE TABLE IF NOT EXISTS items (
    id              TEXT PRIMARY KEY,
    name            TEXT NOT NULL DEFAULT '',
    description     TEXT NOT NULL DEFAULT '',
    count           INTEGER NOT NULL DEFAULT 1,
    section         TEXT NOT NULL DEFAULT 'Other',
    imageUrl        TEXT,
    status          TEXT NOT NULL DEFAULT 'active',   -- active | complete | deleted
    completedAt     INTEGER,
    createdAt       INTEGER NOT NULL,
    nameUpdatedAt   INTEGER NOT NULL DEFAULT 0,
    descUpdatedAt   INTEGER NOT NULL DEFAULT 0,
    countUpdatedAt  INTEGER NOT NULL DEFAULT 0,
    sectionUpdatedAt INTEGER NOT NULL DEFAULT 0,
    imageUpdatedAt  INTEGER NOT NULL DEFAULT 0,
    statusUpdatedAt INTEGER NOT NULL DEFAULT 0,
    updatedAt       INTEGER NOT NULL DEFAULT 0
  );

  CREATE TABLE IF NOT EXISTS sections (
    name       TEXT PRIMARY KEY,
    position   INTEGER NOT NULL DEFAULT 0,
    updatedAt  INTEGER NOT NULL DEFAULT 0
  );

  CREATE INDEX IF NOT EXISTS idx_items_updatedAt ON items(updatedAt);
  CREATE INDEX IF NOT EXISTS idx_sections_updatedAt ON sections(updatedAt);
`);

// Ensure a section row exists (used by AI sectioning + add flow)
function ensureSection(name, now) {
  if (!name) return;
  const existing = db.prepare('SELECT name FROM sections WHERE name = ?').get(name);
  if (!existing) {
    const max = db.prepare('SELECT COALESCE(MAX(position), 0) AS m FROM sections').get().m;
    db.prepare('INSERT INTO sections (name, position, updatedAt) VALUES (?, ?, ?)')
      .run(name, max + 1, now);
  }
}

module.exports = { db, ensureSection };
