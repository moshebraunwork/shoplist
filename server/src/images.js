// Image lookup via Serper.dev (Google Images). Returns up to N image URLs.
const KEY = process.env.SERPER_API_KEY;

async function searchImages(query, num = 2) {
  if (!KEY || !query) return [];
  try {
    const res = await fetch('https://google.serper.dev/images', {
      method: 'POST',
      headers: { 'X-API-KEY': KEY, 'Content-Type': 'application/json' },
      body: JSON.stringify({ q: query, num: 10 }),
      signal: AbortSignal.timeout(6000),
    });
    if (!res.ok) return [];
    const data = await res.json();
    return (data.images || [])
      .map((it) => it.imageUrl)
      .filter(Boolean)
      .slice(0, num);
  } catch (e) {
    console.error('[images] search failed:', e.message);
    return [];
  }
}

module.exports = { searchImages };
