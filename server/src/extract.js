// Unfurl a shared URL into product info using Open Graph / Twitter / JSON-LD
// metadata (the same data link previews use). Falls back gracefully.
const cheerio = require('cheerio');

const UA =
  'Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) ' +
  'Chrome/120.0.0.0 Mobile Safari/537.36';

// Basic SSRF guard: only http(s), and refuse obvious internal hosts.
function assertSafe(rawUrl) {
  const u = new URL(rawUrl);
  if (u.protocol !== 'http:' && u.protocol !== 'https:') {
    throw new Error('unsupported protocol');
  }
  const h = u.hostname;
  if (/^(localhost|0\.0\.0\.0|127\.|169\.254\.|10\.|192\.168\.|172\.(1[6-9]|2\d|3[01])\.)/.test(h)) {
    throw new Error('blocked host');
  }
  return u;
}

// Unwrap common Google redirect / image-viewer links to the real target.
function unwrap(raw) {
  try {
    const u = new URL(raw);
    if (u.hostname.includes('google.')) {
      if (u.pathname === '/imgres') {
        const img = u.searchParams.get('imgurl');
        if (img) return img;
      }
      if (u.pathname === '/url') {
        const t = u.searchParams.get('url') || u.searchParams.get('q');
        if (t) return t;
      }
    }
    return raw;
  } catch {
    return raw;
  }
}

function absolutize(src, base) {
  if (!src) return null;
  try {
    return new URL(src, base).href;
  } catch {
    return src;
  }
}

// Pull a Product node out of any JSON-LD blocks on the page.
function fromJsonLd($) {
  let result = null;
  $('script[type="application/ld+json"]').each((_, el) => {
    if (result) return;
    try {
      const data = JSON.parse($(el).contents().text());
      const nodes = Array.isArray(data) ? data : data['@graph'] || [data];
      for (const node of nodes) {
        const type = node?.['@type'];
        const isProduct =
          type === 'Product' || (Array.isArray(type) && type.includes('Product'));
        if (isProduct) {
          let image = node.image;
          if (Array.isArray(image)) image = image[0];
          if (image && typeof image === 'object') image = image.url;
          result = {
            name: node.name || '',
            description: typeof node.description === 'string' ? node.description : '',
            image: image || null,
          };
          return;
        }
      }
    } catch {
      /* ignore malformed JSON-LD */
    }
  });
  return result;
}

async function extract(rawUrl) {
  const target = unwrap(rawUrl.trim());
  assertSafe(target);

  const res = await fetch(target, {
    headers: { 'User-Agent': UA, Accept: 'text/html,application/xhtml+xml,image/*,*/*' },
    redirect: 'follow',
    signal: AbortSignal.timeout(9000),
  });
  const finalUrl = res.url || target;
  const ct = (res.headers.get('content-type') || '').toLowerCase();

  // The shared link is itself an image.
  if (ct.startsWith('image/')) {
    return { name: '', description: '', imageUrl: finalUrl };
  }

  const html = await res.text();
  const $ = cheerio.load(html);
  const meta = (sel) => ($(sel).attr('content') || '').trim();

  const ld = fromJsonLd($);

  const name =
    (ld && ld.name) ||
    meta('meta[property="og:title"]') ||
    meta('meta[name="twitter:title"]') ||
    $('title').first().text().trim();

  const description =
    (ld && ld.description) ||
    meta('meta[property="og:description"]') ||
    meta('meta[name="twitter:description"]') ||
    '';

  const image =
    (ld && ld.image) ||
    meta('meta[property="og:image"]') ||
    meta('meta[property="og:image:url"]') ||
    meta('meta[name="twitter:image"]') ||
    meta('meta[name="twitter:image:src"]') ||
    '';

  return {
    name: (name || '').replace(/\s+/g, ' ').trim().slice(0, 120),
    description: (description || '').replace(/\s+/g, ' ').trim().slice(0, 300),
    imageUrl: absolutize(image, finalUrl),
  };
}

module.exports = { extract };
