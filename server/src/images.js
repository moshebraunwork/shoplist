// Image lookup for the add/edit sheet.
//
// Strategy:
//   1) Evergreen Kosher catalog (real product photos, right brands/kosher) — PRIMARY
//   2) Serper.dev Google Images — FALLBACK for items Evergreen doesn't carry
//
// Public API is unchanged: searchImages(query, num) -> [url, ...]
// so server.js and the Android app need no changes.

const { evergreenImages } = require('./evergreen');

const SERPER_KEY = process.env.SERPER_API_KEY;

async function serperImages(query, num) {
  if (!SERPER_KEY || !query) return [];
  try {
    const res = await fetch('https://google.serper.dev/images', {
      method: 'POST',
      headers: {
        'X-API-KEY': SERPER_KEY,
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ q: query, num: 10 }),
      signal: AbortSignal.timeout(6000),
    });
    if (!res.ok) return [];
    const data = await res.json();
    return (data.images || [])
      .map((i) => i.imageUrl)
      .filter(Boolean)
      .slice(0, num);
  } catch (e) {
    console.error('[images] serper failed:', e.message);
    return [];
  }
}

async function searchImages(query, num = 2) {
  if (!query) return [];

  // 1) Evergreen catalog first.
  try {
    const ev = await evergreenImages(query, num);
    if (ev.length) return ev.slice(0, num);
  } catch (e) {
    console.error('[images] evergreen failed:', e.message);
  }

  // 2) Fall back to Serper.
  return serperImages(query, num);
}

module.exports = { searchImages };
