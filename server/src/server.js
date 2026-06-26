require('dotenv').config();
const express = require('express');
const cors = require('cors');
const multer = require('multer');
const path = require('path');
const fs = require('fs');
const crypto = require('crypto');

const { db, ensureSection } = require('./db');
const { categorize } = require('./ai');
const { searchImages } = require('./images');
const { extract } = require('./extract');

const PORT = process.env.PORT || 4000;
const PUBLIC_URL = (process.env.PUBLIC_URL || `http://localhost:${PORT}`).replace(/\/$/, '');

const app = express();
app.use(cors());
app.use(express.json({ limit: '2mb' }));

// ---- uploads ----
const uploadDir = path.join(__dirname, '..', 'uploads');
fs.mkdirSync(uploadDir, { recursive: true });
app.use('/uploads', express.static(uploadDir));

const storage = multer.diskStorage({
  destination: (_req, _file, cb) => cb(null, uploadDir),
  filename: (_req, file, cb) => {
    const ext = path.extname(file.originalname) || '.jpg';
    cb(null, `${Date.now()}-${crypto.randomBytes(4).toString('hex')}${ext}`);
  },
});
const upload = multer({ storage, limits: { fileSize: 8 * 1024 * 1024 } });

const ITEM_FIELDS = ['name', 'description', 'count', 'section', 'imageUrl', 'status'];
const FIELD_TS = {
  name: 'nameUpdatedAt',
  description: 'descUpdatedAt',
  count: 'countUpdatedAt',
  section: 'sectionUpdatedAt',
  imageUrl: 'imageUpdatedAt',
  status: 'statusUpdatedAt',
};

const getItem = db.prepare('SELECT * FROM items WHERE id = ?');

const insertItem = db.prepare(`
  INSERT INTO items
    (id, name, description, count, section, imageUrl, status, completedAt, createdAt,
     nameUpdatedAt, descUpdatedAt, countUpdatedAt, sectionUpdatedAt, imageUpdatedAt, statusUpdatedAt, updatedAt)
  VALUES
    (@id, @name, @description, @count, @section, @imageUrl, @status, @completedAt, @createdAt,
     @nameUpdatedAt, @descUpdatedAt, @countUpdatedAt, @sectionUpdatedAt, @imageUpdatedAt, @statusUpdatedAt, @updatedAt)
`);

const updateItem = db.prepare(`
  UPDATE items SET
    name=@name, description=@description, count=@count, section=@section,
    imageUrl=@imageUrl, status=@status, completedAt=@completedAt,
    nameUpdatedAt=@nameUpdatedAt, descUpdatedAt=@descUpdatedAt, countUpdatedAt=@countUpdatedAt,
    sectionUpdatedAt=@sectionUpdatedAt, imageUpdatedAt=@imageUpdatedAt, statusUpdatedAt=@statusUpdatedAt,
    updatedAt=@updatedAt
  WHERE id=@id
`);

// Field-level last-write-wins merge of an incoming client item against the server row.
function mergeItem(incoming) {
  const now = Date.now();
  const existing = getItem.get(incoming.id);

  if (!existing) {
    const row = {
      id: incoming.id,
      name: incoming.name ?? '',
      description: incoming.description ?? '',
      count: incoming.count ?? 1,
      section: incoming.section ?? 'Other',
      imageUrl: incoming.imageUrl ?? null,
      status: incoming.status ?? 'active',
      completedAt: incoming.completedAt ?? null,
      createdAt: incoming.createdAt ?? now,
      nameUpdatedAt: incoming.nameUpdatedAt ?? now,
      descUpdatedAt: incoming.descUpdatedAt ?? now,
      countUpdatedAt: incoming.countUpdatedAt ?? now,
      sectionUpdatedAt: incoming.sectionUpdatedAt ?? now,
      imageUpdatedAt: incoming.imageUpdatedAt ?? now,
      statusUpdatedAt: incoming.statusUpdatedAt ?? now,
    };
    row.updatedAt = Math.max(
      row.nameUpdatedAt, row.descUpdatedAt, row.countUpdatedAt,
      row.sectionUpdatedAt, row.imageUpdatedAt, row.statusUpdatedAt
    );
    ensureSection(row.section, now);
    insertItem.run(row);
    return getItem.get(incoming.id);
  }

  // Per-field comparison: take whichever side has the newer field timestamp.
  const merged = { ...existing };
  for (const field of ITEM_FIELDS) {
    const tsKey = FIELD_TS[field];
    const incomingTs = incoming[tsKey] ?? 0;
    if (incomingTs > (existing[tsKey] ?? 0)) {
      merged[field] = incoming[field] ?? (field === 'count' ? 1 : null);
      merged[tsKey] = incomingTs;
    }
  }
  // completedAt rides with status
  if ((incoming.statusUpdatedAt ?? 0) > (existing.statusUpdatedAt ?? 0)) {
    merged.completedAt = incoming.completedAt ?? null;
  }
  merged.updatedAt = Math.max(
    merged.nameUpdatedAt, merged.descUpdatedAt, merged.countUpdatedAt,
    merged.sectionUpdatedAt, merged.imageUpdatedAt, merged.statusUpdatedAt
  );
  ensureSection(merged.section, now);
  updateItem.run(merged);
  return getItem.get(incoming.id);
}

function mergeSection(incoming) {
  const now = Date.now();
  const existing = db.prepare('SELECT * FROM sections WHERE name = ?').get(incoming.name);
  if (!existing) {
    db.prepare('INSERT INTO sections (name, position, updatedAt) VALUES (?, ?, ?)')
      .run(incoming.name, incoming.position ?? 0, incoming.updatedAt ?? now);
  } else if ((incoming.updatedAt ?? 0) > existing.updatedAt) {
    db.prepare('UPDATE sections SET position=?, updatedAt=? WHERE name=?')
      .run(incoming.position ?? existing.position, incoming.updatedAt, incoming.name);
  }
}

// ---- Sync: push local changes + pull everything changed since a watermark ----
// Body: { since: <ms>, items: [...], sections: [...] }
// Returns: { now, items: [...changed since...], sections: [...changed since...] }
app.post('/api/sync', (req, res) => {
  const { since = 0, items = [], sections = [] } = req.body || {};
  try {
    const tx = db.transaction(() => {
      for (const s of sections) mergeSection(s);
      for (const it of items) mergeItem(it);
    });
    tx();

    const changedItems = db.prepare('SELECT * FROM items WHERE updatedAt > ?').all(since);
    const changedSections = db.prepare('SELECT * FROM sections WHERE updatedAt > ?').all(since);
    res.json({ now: Date.now(), items: changedItems, sections: changedSections });
  } catch (e) {
    console.error('[sync] error:', e);
    res.status(500).json({ error: e.message });
  }
});

// Full state (used on first launch / cold start)
app.get('/api/state', (_req, res) => {
  res.json({
    now: Date.now(),
    items: db.prepare('SELECT * FROM items').all(),
    sections: db.prepare('SELECT * FROM sections ORDER BY position').all(),
  });
});

// ---- As-you-type suggestions: reuse (history) + active duplicates ----
app.get('/api/suggest', (req, res) => {
  const q = (req.query.q || '').trim();
  if (!q) return res.json({ reuse: [], duplicates: [] });
  const like = `%${q}%`;

  // Active duplicates: items currently on the list whose name matches.
  const duplicates = db.prepare(
    `SELECT * FROM items WHERE status='active' AND name LIKE ? COLLATE NOCASE ORDER BY updatedAt DESC LIMIT 5`
  ).all(like);

  // Reuse: previously used names (completed/deleted), most recent first, de-duped by name.
  const reuse = db.prepare(
    `SELECT * FROM items
     WHERE status IN ('complete','deleted') AND name LIKE ? COLLATE NOCASE
     GROUP BY name COLLATE NOCASE
     ORDER BY MAX(updatedAt) DESC LIMIT 5`
  ).all(like);

  res.json({ reuse, duplicates });
});

// ---- Image lookup (Google Programmable Search) ----
app.get('/api/images', async (req, res) => {
  const q = (req.query.q || '').trim();
  const images = await searchImages(q, 2);
  res.json({ images });
});

// ---- AI sectioning ----
app.post('/api/ai/section', async (req, res) => {
  const { name = '', description = '' } = req.body || {};
  const existing = db.prepare('SELECT name FROM sections').all().map((r) => r.name);
  let section = await categorize(name, description, existing);
  if (!section) section = 'Other';
  ensureSection(section, Date.now());
  res.json({ section });
});

// ---- Upload your own image ----
app.post('/api/upload', upload.single('image'), (req, res) => {
  if (!req.file) return res.status(400).json({ error: 'no file' });
  res.json({ url: `${PUBLIC_URL}/uploads/${req.file.filename}` });
});

app.get('/api/extract', async (req, res) => {
  const url = (req.query.url || '').trim();
  if (!url) return res.status(400).json({ error: 'no url' });
  try {
    res.json(await extract(url));
  } catch (e) {
    console.error('[extract] failed:', e.message);
    res.json({ name: '', description: '', imageUrl: null });
  }
});

app.get('/api/health', (_req, res) => res.json({ ok: true }));

app.listen(PORT, () => {
  console.log(`shoplist-server listening on :${PORT} (public ${PUBLIC_URL})`);
});
