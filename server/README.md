# ShopList backend

Node/Express + SQLite. Source of truth for the shared shopping list.

## Endpoints

| Method | Path               | Purpose |
|--------|--------------------|---------|
| POST   | `/api/sync`        | Push local changes, pull everything changed since a watermark. Body: `{ since, items[], sections[] }` → `{ now, items[], sections[] }` |
| GET    | `/api/state`       | Full snapshot (first launch). |
| GET    | `/api/suggest?q=`  | As-you-type: `{ reuse[], duplicates[] }`. |
| GET    | `/api/images?q=`   | Up to 2 product images via Google Programmable Search. |
| POST   | `/api/ai/section`  | `{ name, description }` → `{ section }` via Gemini Flash. |
| POST   | `/api/upload`      | multipart `image` → `{ url }`. |
| GET    | `/api/health`      | `{ ok: true }`. |

## Run locally

```bash
cp .env.example .env      # fill in keys + PUBLIC_URL
npm install               # builds better-sqlite3 (needs build-essential on the VM)
npm start
```

If `better-sqlite3` fails to build on the VM:
```bash
sudo apt-get install -y build-essential python3
```

## Keys you need

- **GEMINI_API_KEY** — Google AI Studio key. Model defaults to `gemini-2.0-flash`.
- **GOOGLE_CSE_KEY** + **GOOGLE_CSE_ID** — a Custom Search API key and a
  Programmable Search Engine configured with **Image search ON** and
  **Search the entire web ON**. Free tier is 100 queries/day; the app debounces
  typing to ~450ms so a normal session stays well under that.

## Deploy on the VM (same shape as Drop)

```bash
# 1. clone into /home/ubuntu/shoplist, fill .env, npm install
# 2. systemd
sudo cp shoplist.service /etc/systemd/system/
sudo systemctl daemon-reload && sudo systemctl enable --now shoplist
# 3. nginx + SSL
sudo cp nginx-shoplist.conf /etc/nginx/sites-available/shop.mobrauntech.com
sudo ln -s /etc/nginx/sites-available/shop.mobrauntech.com /etc/nginx/sites-enabled/
sudo nginx -t && sudo systemctl reload nginx
sudo certbot --nginx -d shop.mobrauntech.com
```

Remember: open the port in the OCI security list **and** add the iptables rule on
the VM (you've hit this before). Once behind Nginx+SSL, set
`PUBLIC_URL=https://shop.mobrauntech.com` in `.env` and point the Android app's
`BASE_URL` at the same domain.

## Notes

- Field-level last-write-wins: each of name/description/count/section/imageUrl/status
  carries its own timestamp, so two phones editing different fields of the same item
  never clobber each other.
- The DB file is `shoplist.db` (WAL mode) next to `src/`. Back it up if it matters.
