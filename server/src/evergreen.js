// Product-image lookup against the Evergreen Kosher storefront catalog API
// (Stor.ai platform). Returns real product photos for a query. Used as the
// PRIMARY image source; images.js falls back to Serper when this returns none.
//
// This hits the same public storefront API the website itself calls. The
// retailer/branch/appId/filters below come from a real request captured from
// the site. If the storefront ever changes its API or app params, update the
// env vars (or these defaults) and re-test.

const RETAILER = process.env.EVERGREEN_RETAILER_ID || '1120';
const BRANCH   = process.env.EVERGREEN_BRANCH_ID   || '883';
const APP_ID   = process.env.EVERGREEN_APP_ID      || '4';
const LANG     = process.env.EVERGREEN_LANGUAGE_ID || '2';   // 2 = English
const IMG_SIZE = process.env.EVERGREEN_IMG_SIZE    || 'large';
const TIMEOUT  = Number(process.env.EVERGREEN_TIMEOUT_MS || 6000);

const BASE = `https://www.shopevergreenkosher.com/v2/retailers/${RETAILER}/branches/${BRANCH}/products`;

// Only active, visible, in-stock, priced products.
const FILTERS = {
  must: {
    exists: ['family.id', 'family.categoriesPaths.id', 'branch.regularPrice'],
    term: { 'branch.isActive': true, 'branch.isVisible': true },
  },
  mustNot: {
    term: { 'branch.regularPrice': 0, 'branch.isOutOfStock': true },
  },
};
const ENC_FILTERS = encodeURIComponent(JSON.stringify(FILTERS));

// The catalog stores image URLs as templates, e.g.
//   .../items/{{size}}/303417.{{extension||'jpg'}}?v=5
// Fill in a concrete size + extension so the CDN serves a real image.
function resolveImageUrl(raw) {
  if (!raw) return null;
  return raw
    .replace(/\{\{\s*extension[^}]*\}\}/g, 'jpg')
    .replace(/\{\{\s*size[^}]*\}\}/g, IMG_SIZE);
}

async function queryEvergreen(term, num) {
  const url =
    `${BASE}?appId=${APP_ID}&filters=${ENC_FILTERS}` +
    `&from=0&isSearch=true&languageId=${LANG}` +
    `&query=${encodeURIComponent(term)}&size=24`;

  const res = await fetch(url, {
    headers: {
      Accept: 'application/json',
      // A UA helps avoid being treated as a bot by the storefront CDN/WAF.
      'User-Agent':
        'Mozilla/5.0 (compatible; ShopList/1.0; +https://shoplist.mobrauntech.com)',
    },
    signal: AbortSignal.timeout(TIMEOUT),
  });
  if (!res.ok) return [];

  const data = await res.json();
  const products = Array.isArray(data?.products) ? data.products : [];

  const urls = [];
  for (const p of products) {
    const u = resolveImageUrl(p?.image?.url);
    if (u && !urls.includes(u)) urls.push(u);
    if (urls.length >= num) break;
  }
  return urls;
}

// Try the full query first; if the catalog finds nothing, retry with just the
// first word (the app sometimes appends a note to the query, which narrows the
// search too much). First non-empty result wins.
async function evergreenImages(query, num = 2) {
  const q = (query || '').trim();
  if (!q) return [];

  const terms = [q];
  const firstWord = q.split(/\s+/)[0];
  if (firstWord && firstWord !== q) terms.push(firstWord);

  for (const term of terms) {
    try {
      const urls = await queryEvergreen(term, num);
      if (urls.length) return urls;
    } catch (e) {
      console.error('[evergreen] query failed:', e.message);
    }
  }
  return [];
}

module.exports = { evergreenImages };
