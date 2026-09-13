Build a native Android app in Kotlin for "tradeBot Investor" — a mobile client for an existing
investor panel on a crypto futures mirror-trading platform. The backend already exists and is
live; this is a fresh Android Studio project, Kotlin only, minSdk 26+.

## Stack (recommended, adjust if you have a strong preference)
- Jetpack Compose for UI (Material 3)
- Retrofit + OkHttp + kotlinx.serialization (or Moshi) for networking
- ViewModel + StateFlow for state
- Navigation-Compose for screen routing
- DataStore Preferences (or EncryptedSharedPreferences via Android Keystore) to persist the auth
  token — never plain SharedPreferences, this handles real money data
- `io.socket:socket.io-client` for the real-time connection (see Real-time updates below)
- Package name: `ir.devtrader.investor` (or your own choice)

## Backend API
Base URL: `https://devtrader.ir/api/investor`
All requests/responses are JSON. Every endpoint below except login requires header:
`Authorization: Bearer <token>`

### POST /api/investor/auth/register
Request: `{ "email": "...", "password": "... (min 6 chars)", "first": "...", "last": "...", "phone": "...", "acceptedTerms": true }`
`acceptedTerms` MUST be `true` in the request body or it's rejected — there is a required risk
disclosure / terms-of-use step before signup on the web flow
(`https://devtrader.ir/investor/auth/terms`), and the API enforces the same thing. There is no
JSON endpoint serving the terms text itself yet — either show it in a WebView pointed at that URL,
or embed the same text as a static in-app screen, but the checkbox must be explicitly checked
before this call is made, not defaulted to true.
Success (200): `{ "status": true, "token": "...", "investor": { "id","email","first","last" } }`
Failure (400): `{ "status": false, "error": "An account with that email already exists." | "Please enter a password" | "Password must be at least 6 characters" | "You must accept the risk disclosure / terms to register." | other }`
A brand-new investor has no exchange key bound and is NOT yet approved for mirror trading — that's
normal, see the Dashboard endpoint below. This account is self-service (signup is open), but
mirror trading itself still needs an admin to activate it after the investor binds a key.

### POST /api/investor/auth/login
Request: `{ "email": "...", "password": "..." }`
Success (200): `{ "status": true, "token": "<jwt, valid 30 days, no refresh endpoint yet>", "investor": { "id": "...", "email": "...", "first": "...", "last": "..." } }`
Failure (400): `{ "status": false, "error": "incorrect email" | "incorrect password" | other message }`
On any 401 from any other endpoint: clear the stored token and send the user back to login —
there's no refresh-token flow yet, just re-login.

### GET /api/investor/dashboard
`{
  "status": true,
  "investor": { "id","email","first","last","tradingEnabled": bool, "marginRatio": number, "debt": number },
  "hasApiKey": bool,
  "assets": null | { "status": true, "available": number, "margin": number, "crossUnrealizedPNL": number, "isolationUnrealizedPNL": number, ...other raw Bitunix futures-account fields },
  "openPositionsCount": number,
  "summary": { "totalTrades": number, "sampleSize": number, "totalRealizedPnl": number, "winRate": number|null },
  "accountError": string|null
}`
- `hasApiKey: false` means the investor hasn't bound an exchange key yet — `assets`/`summary` will
  be empty/zeroed in that case; show a "bind your API key" prompt instead of a dashboard.
- `accountError` non-null means the bound key exists but a live Bitunix call failed just now
  (rate limit, revoked key, etc.) — show it as a banner, not a hard error state.

### GET /api/investor/positions
`{ "status": true, "hasApiKey": bool, "positions": [ ...raw Bitunix get_pending_positions array, shape not guaranteed — render generically (symbol/side/size/pnl-style fields) and treat unknown fields as opaque ], "accountError": string|null }`

### GET /api/investor/trades
`{ "status": true, "hasApiKey": bool, "trades": [ ...raw Bitunix trade-history array, same "shape not guaranteed" caveat as positions above — each entry has at least a `realizedPNL` field, used elsewhere for the dashboard summary math ], "accountError": string|null }`
Up to 100 most recent, newest first (Bitunix's own cap, no further pagination on this endpoint).

### POST /api/investor/profile
Request: `{ "first": "...", "last": "...", "phone": "..." }` — all three always sent together
(this replaces the stored name/phone wholesale, it's not a partial patch; pre-fill the form from
GET /dashboard's `investor.first`/`last`/`phone` first). `{ "status": true }` on success, `{ "status": false, "message": "..." }` on failure.
Note `phone` is also what SMS/call alarms (see POST /alarms below) send to — this is the one place
to set it if it wasn't filled in at registration.

### GET /api/investor/debt-ledger
`{
  "status": true,
  "debt": number,  // current running balance, same value as investor.debt above
  "entries": [ {
    "_id": "...", "type": "trade" | "settlement" | "adjustment",
    "symbol": string|null, "side": string|null, "entryPrice": string|null, "closePrice": string|null,
    "realizedPnl": number|null, "profitSharePercent": number|null,
    "debtDelta": number,      // signed change this entry made
    "balanceAfter": number,   // running balance right after this entry
    "note": string|null,
    "createdAt": "ISO date"
  } ],  // newest first, capped at 200
}`

### GET /api/investor/notifications
`{ "status": true, "notifications": [ { "_id","type": "success"|"warning"|"error"|"info", "title","message","seen": bool, "seenAt": "ISO date"|null, "createdAt": "ISO date" } ], "unseenCount": number }`
Newest first, capped at 50.

### POST /api/investor/notifications/seen
No body. Marks all of the investor's own unseen notifications as seen. `{ "status": true }`

### POST /api/investor/bind-key
Request: `{ "key": "...", "secret": "..." }` (the investor's own Bitunix API key/secret)
Success: `{ "status": true, "available": number, "equity": number, "tradePermissionWarning": string|null }`
Failure: `{ "status": false, "message": "human-readable reason" }` — show it verbatim, these are
already written to be shown to an end user (e.g. "This key can read your account but can't place
trades: ... Enable Futures Trading permission on this key in Bitunix and try again.")
`tradePermissionWarning` non-null on a *success* response means the key saved but trade-permission
couldn't be fully confirmed (e.g. zero balance) — show as a soft warning, not an error.

### Static price alarms — GET/POST/DELETE /api/investor/alarms
Self-service price alerts, separate from mirror trading — "notify me when a symbol crosses a
price". Fires as a push-style item in the same /notifications feed above (poll it) when triggered.

**GET /api/investor/alarms**
`{ "status": true, "alarms": [ { "_id","symbol": "BTCUSDT", "condition": "above"|"below", "price": number, "isEnabled": true, "createdAt": "ISO date" } ] }` — newest first, this investor's own only.

**POST /api/investor/alarms**
Request: `{ "symbol": "BTC", "condition": "above"|"below", "price": number, "sms": bool, "call": bool }`
— symbol is case-insensitive and `USDT` is appended automatically server-side if missing (send
either `"BTC"` or `"BTCUSDT"`, both work). `sms`/`call` are both optional, default false.
Success: `{ "status": true, "alarm": { "_id","symbol","condition","price","shouldMessage": bool, "shouldCall": bool, "createdAt", ... } }`
Failure: `{ "status": false, "message": "Symbol is required." | "Condition must be \"above\" or \"below\"." | "Price must be a positive number." | "Add a phone number in your profile first to use SMS/call alerts." | other }`

**`sms`/`call` are real, not decorative** — `true` on either triggers an actual SMS text or phone
call (via the same notifycloud.ir integration the rest of this backend uses) to the phone number
on the investor's profile when the alarm fires, in addition to the in-app notification. If it's
blank, point the investor at the Profile screen (POST /profile, above) to set it first — the
create-alarm call itself will reject with a clear message if `sms`/`call` is requested with no
phone on file, so surface that message rather than guessing.

**POST /api/investor/alarms/:id/delete**
No body. Deletes one of this investor's own alarms (silently no-ops if the id doesn't belong to
them or doesn't exist — still returns success). `{ "status": true }`

**GET /api/investor/symbols**
`{ "status": true, "symbols": ["BTC", "ETH", "SOL", ...] }` — the full list of tradeable base
assets (sorted alphabetically), for the Add Alarm form's symbol field. Fetch once (on screen load
or app start, cache in memory) rather than on every keystroke.

## Real-time updates — Socket.IO
The backend runs Socket.IO at the site root (`https://devtrader.ir`, NOT under `/api/investor`).
Use the official `io.socket:socket.io-client` Java library. Connect once, after login/register
succeeds, and keep the connection open while the app is foregrounded:

```kotlin
val opts = IO.Options()
opts.auth = mapOf("token" to storedToken)  // the same bearer token from login/register
val socket = IO.socket("https://devtrader.ir", opts)
socket.connect()
```

This is bearer-token auth (`auth.token` in the handshake), a server-side addition made alongside
this feature specifically so the app never needs cookies — the exact same token used for
`Authorization: Bearer` on every REST call above works here unchanged. An invalid/expired token
fails the socket handshake the same way a REST 401 would; treat that failure the same way (back to
login). Reconnection (dropped wifi, backgrounding, etc.) is handled automatically by the client
library's defaults — no custom retry logic needed.

**Events to listen for:**
- **`notification`** — `{ "type": "success"|"warning"|"error"|"info", "title","message","ts": epoch-ms }`.
  The same events GET /notifications persists — this is the live push side of that feed. On
  receipt: show a toast/snackbar, increment the unread badge, and prepend it to the in-memory
  Notifications list if that screen is currently showing.
- **`alarmTriggered`** — `{ "_id","symbol","condition": "above"|"below","price": number }`. Fired
  the instant a price alarm actually fires — the backend deletes the Alarm the same moment (alarms
  are one-shot, not recurring), so this `_id` is now gone server-side too. On receipt:
  1. Remove the alarm with this `_id` from the local Alarms list state, if the Alarms screen has it
     loaded (don't wait for a re-fetch).
  2. Show a toast, e.g. `"${symbol} is ${condition} $${price}"`.
  3. This fires independently of whether the Alarms screen is currently open — handle it at the
     app/session level (a shared ViewModel or repository the socket connection lives in), not
     inside the screen's own composable, or it'll be silently missed whenever the user isn't on
     that screen. Also refresh `unseenCount`/prepend to Notifications, same as the `notification`
     event above (both fire together for the same trigger).

**Caveat**: this only delivers while the app holds an open connection — no FCM/background push yet
(see below), so an alarm firing while the app is fully killed won't show a toast or notification
until the app is reopened and calls GET /notifications, though the alarm will already be gone from
GET /alarms by then (server-side deletion doesn't wait for the client).

## Push notifications — important caveat
There is NO Firebase/FCM wiring on the backend yet — GET /api/investor/notifications is poll-only,
and the Socket.IO events above only deliver while the app has an active connection (roughly:
foregrounded). For v1, poll notifications on app foreground and on a WorkManager periodic job
(e.g. every few minutes) as a background-safe fallback, and show unseenCount as a badge. Do not
build FCM push into this v1 — that requires creating a Firebase project (a separate
account/console step) and server-side sending code that doesn't exist yet. Flag it clearly as a
fast-follow once we're ready to add it, don't silently skip it.

## Navigation — sidebar drawer
The web investor panel (what this app mirrors) has a persistent left sidebar with a header (brand,
language switch, notification bell with unread badge, logout) and a nav list. Match that structure
here with a standard Compose `ModalNavigationDrawer` (a permanent/rail-style drawer on tablet-width
screens if you want, but a slide-out drawer opened from a hamburger icon in the top bar is the
right default for phones — the web version does the same thing at narrow widths).

**Drawer contents, top to bottom** (same order as the web sidebar):
- Header: app name/logo, current investor's initial as an avatar, a notifications bell (badge =
  `unseenCount` from GET /notifications) that opens the Notifications screen, logout action.
- Nav items, each navigating to its screen: **Dashboard**, **Positions**, **Trades**, **Debt
  Ledger**, **Alarms**, **Profile**, **Settings**.
- Highlight whichever screen is currently active (same as the web sidebar's `.active` state).

The drawer/top-bar only appears after login — Login and Register are their own full-screen flows
with no drawer.

## Screens (v1)
### Pre-auth
1. **Login** — email/password, calls POST /auth/login, stores token, navigates to Dashboard.
   Link to Register for a new account.
2. **Register** — email, password (min 6 chars, confirm-password field client-side), first/last
   name, phone, and a required "I accept the terms" checkbox (link/WebView to
   https://devtrader.ir/investor/auth/terms) that must be checked before the submit button is
   enabled. Calls POST /auth/register, stores token, navigates to Dashboard. Make clear in the UI
   that mirror trading itself still needs admin approval after this — registering alone doesn't
   activate real trading.

### Behind the drawer (same order as the nav list above)
3. **Dashboard** — investor's wallet available/equity (from `assets`), open positions count,
   trade summary (win rate, total realized PNL, sample size vs total trades), and a prompt to
   bind an API key if `hasApiKey` is false.
4. **Positions** — GET /positions, list of currently open positions. Empty state when
   `hasApiKey` is false ("bind your key first") or the list is genuinely empty.
5. **Trades** — GET /trades, closed-trade history (up to the most recent 100). Same empty-state
   handling as Positions.
6. **Debt Ledger** — scrollable list of `entries`, current `debt` balance pinned at top, color
   trade wins vs losses by `debtDelta` sign.
7. **Alarms** — list of the investor's own price alarms (symbol, above/below, price, and whether
   SMS/call are on) with delete swipe/button, and a form to add one. When the `alarmTriggered`
   socket event fires for one of these (see Real-time updates above), remove it from this list
   live and show a toast — don't wait for the user to pull-to-refresh.
   - **Symbol**: a filtered/searchable dropdown (e.g. `ExposedDropdownMenuBox` with the text field
     driving live filtering of the option list, not a plain static Spinner) sourced from
     GET /symbols — type to narrow, tap to pick, still a free-text field underneath (an unlisted
     symbol should still be submittable, matching the API's own leniency).
   - **Condition**: above/below dropdown, same as now.
   - **Price**: numeric field, same as now.
   - **Two switches**: "Text me (SMS)" and "Call me" — independent, both optional, mapped to
     `sms`/`call` in the POST body. Since these trigger a real text/call, show a short inline note
     near them (e.g. "Uses the phone number on your profile") and don't default either to on.
8. **Profile** — first/last name, email (read-only, not editable here), phone — pre-filled from
   GET /dashboard's `investor` object, saved via POST /profile. Mention near the phone field that
   it's what SMS/call alarms use.
9. **Settings / Bind API Key** — form to paste key+secret, calls POST /bind-key, shows the
   success/failure message verbatim from the response.
10. **Notifications** — reached from the drawer header's bell icon rather than the nav list
    itself (same as the web version): list, unseen ones visually distinct, mark-all-seen on open.

## Security notes
- HTTPS only, no cleartext traffic (default network security config is fine, don't relax it).
- Never log the API key/secret or the auth token.
- Store the auth token in DataStore/EncryptedSharedPreferences, not plain SharedPreferences.
- This talks to a live production system handling real trading accounts — treat every write
  endpoint (bind-key especially) as something that needs a confirming UI step, not a fire-and-
  forget action.

Start by scaffolding the project (Gradle, package structure, DI if you want Hilt — optional for
this size of app), then the networking layer + login/register flow, then the navigation drawer
shell, then the screens behind it.
