# ShopList

A shared shopping list for two people. One list, the server is the source of
truth, each phone is a local-first cache that keeps working offline and syncs
while the app is open.

```
shoplist/
├── server/        Node/Express + SQLite backend (source of truth)
└── android-app/   Native Android client (Kotlin + Jetpack Compose)
```

## How it works

- **Two modes.** *Shop* (check things off while shopping) and *Add* (build the list).
- **Add flow.** Type a name → debounced lookups hit the backend for (a) Google
  Programmable Search product images, (b) past items to re-use, and (c) a warning
  if the same thing is already on the list. Pick an image or upload your own, set
  the count with the drag scroller, optional description, hit Add. The backend
  assigns a shopping section with Gemini Flash (falls back to "Other" offline).
- **Swipes.** Shop mode: swipe right = details/edit, swipe left = delete. Add
  mode: swipe right = edit, swipe left = delete. Deleted/completed items live in
  the collapsible "See complete items" tray and can be restored.
- **Sync.** Local-first: every change is written to Room immediately and flagged
  dirty. A foreground loop pushes dirty rows every ~5s until clean, then pulls
  every ~10s. Merge is **field-level last-write-wins** — two phones editing
  different fields of the same item never clobber each other.

## Get it running

1. **Backend** — see `server/README.md`. Fill `.env` (Gemini key + Google CSE
   key/id + `PUBLIC_URL`), `npm install`, `npm start`. Deploy on the VM with the
   included systemd unit + Nginx config + Certbot, same shape as Drop.

2. **App** — open `android-app/` in Android Studio. Set the backend URL in
   `app/build.gradle.kts`:
   ```kotlin
   buildConfigField("String", "BASE_URL", "\"https://shop.mobrauntech.com/\"")
   ```
   Use the `http://150.136.172.77:4000/` form while testing against the raw VM
   (cleartext is already enabled in the manifest for that). Build & run on both
   phones — same backend, same list.

## Notes / things you'll likely tune

- No auth, by design. Anyone who can reach the backend can read/write the list.
  If you ever expose it more widely, put a shared secret header in front of it.
- The Gradle **wrapper jar isn't included** (binary). Android Studio regenerates
  it on first sync, or run `gradle wrapper` once.
- Launcher icon is a simple vector adaptive icon so the project builds with no
  binary assets — swap in your own via Image Asset Studio whenever.
- Compose/AGP/Room versions are pinned in the Gradle files. If Android Studio
  nudges you to newer ones, that's fine; nothing here depends on bleeding-edge APIs.
