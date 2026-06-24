// Image lookup via Google Programmable Search (Custom Search JSON API).
// Returns up to N real product image URLs for a query.

const KEY = process.env.GOOGLE_CSE_KEY;
const CX = process.env.GOOGLE_CSE_ID;

async function searchImages(query, num = 2) {
  if (!KEY || !CX || !query) return [];
  try {
    const url =
      `https://www.googleapis.com/customsearch/v1` +
      `?key=${KEY}&cx=${CX}&searchType=image&num=${num}` +
      `&safe=active&q=${encodeURIComponent(query)}`;
    const res = await fetch(url, { signal: AbortSignal.timeout(6000) });
    if (!res.ok) return [];
    const data = await res.json();
    return (data.items || [])
      .map((it) => it.link)
      .filter(Boolean)
      .slice(0, num);
  } catch (e) {
    console.error('[images] search failed:', e.message);
    return [];
  }
}

module.exports = { searchImages };
