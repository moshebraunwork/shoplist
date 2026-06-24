// AI sectioning via Gemini Flash.
const MODEL = process.env.GEMINI_MODEL || 'gemini-2.0-flash';
const KEY = process.env.GEMINI_API_KEY;

async function categorize(name, description, existingSections = []) {
  if (!KEY) return null;

  const prompt = `You are organizing a grocery / shopping list.
Assign the item below to the single most appropriate supermarket section
(e.g. "Dairy & Fridge", "Produce", "Bakery", "Meat & Fish", "Frozen",
"Beverages", "Household", "Snacks", "Pantry").

Reuse one of the EXISTING sections if it fits. Only invent a new one if none fit.
Keep new names short, in Title Case.

EXISTING SECTIONS: ${existingSections.length ? existingSections.join(', ') : '(none yet)'}
ITEM NAME: ${name}
ITEM DESCRIPTION: ${description || '(none)'}

Respond with JSON only: {"section":"<section name>"}`;

  try {
    const url = `https://generativelanguage.googleapis.com/v1beta/models/${MODEL}:generateContent?key=${KEY}`;
    const res = await fetch(url, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        contents: [{ parts: [{ text: prompt }] }],
        generationConfig: {
          temperature: 0.2,
          maxOutputTokens: 200,
          responseMimeType: 'application/json',
        },
      }),
      signal: AbortSignal.timeout(8000),
    });
    if (!res.ok) {
      console.error('[ai] http', res.status, (await res.text()).slice(0, 300));
      return null;
    }
    const data = await res.json();
    const text = data?.candidates?.[0]?.content?.parts?.[0]?.text;
    if (!text) {
      console.error('[ai] empty response:', JSON.stringify(data).slice(0, 400));
      return null;
    }
    const parsed = JSON.parse(text.replace(/```json|```/g, '').trim());
    const section = (parsed.section || '').trim();
    return section || null;
  } catch (e) {
    console.error('[ai] categorize failed:', e.message);
    return null;
  }
}

module.exports = { categorize };
