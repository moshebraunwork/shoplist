// AI sectioning via Gemini Flash. Given an item and the list of existing
// sections, return the best section name (reusing an existing one when it fits,
// otherwise proposing a concise new one).

const MODEL = process.env.GEMINI_MODEL || 'gemini-2.0-flash';
const KEY = process.env.GEMINI_API_KEY;

async function categorize(name, description, existingSections = []) {
  if (!KEY) return null; // caller falls back to "Other"

  const prompt = `You are organizing a grocery / shopping list.
Assign the item below to the single most appropriate shopping section
(like aisles in a store: "Dairy", "Produce", "Bakery", "Meat", "Frozen",
"Beverages", "Household", "Snacks", etc.).

Reuse one of the EXISTING sections if it fits well. Only invent a new section
if none of the existing ones fit. Keep new section names short and in Title Case.

EXISTING SECTIONS: ${existingSections.length ? existingSections.join(', ') : '(none yet)'}

ITEM NAME: ${name}
ITEM DESCRIPTION: ${description || '(none)'}

Respond with ONLY raw JSON, no markdown, in this exact shape:
{"section":"<section name>"}`;

  try {
    const url = `https://generativelanguage.googleapis.com/v1beta/models/${MODEL}:generateContent?key=${KEY}`;
    const res = await fetch(url, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        contents: [{ parts: [{ text: prompt }] }],
        generationConfig: { temperature: 0.2, maxOutputTokens: 50 },
      }),
      signal: AbortSignal.timeout(8000),
    });
    if (!res.ok) return null;
    const data = await res.json();
    const text = data?.candidates?.[0]?.content?.parts?.[0]?.text || '';
    const clean = text.replace(/```json|```/g, '').trim();
    const parsed = JSON.parse(clean);
    const section = (parsed.section || '').trim();
    return section || null;
  } catch (e) {
    console.error('[ai] categorize failed:', e.message);
    return null;
  }
}

module.exports = { categorize };
