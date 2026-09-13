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
Request: `{ "symbol": "BTC", "condition": "above"|"below", "price": number }` — symbol is
case-insensitive and `USDT` is appended automatically server-side if missing (send either `"BTC"`
or `"BTCUSDT"`, both work).
Success: `{ "status": true, "alarm": { "_id","symbol","condition","price","createdAt", ... } }`
Failure: `{ "status": false, "message": "Symbol is required." | "Condition must be \"above\" or \"below\"." | "Price must be a positive number." | other }`

**POST /api/investor/alarms/:id/delete**
No body. Deletes one of this investor's own alarms (silently no-ops if the id doesn't belong to
them or doesn't exist — still returns success). `{ "status": true }`

## Push notifications — important caveat
There is NO Firebase/FCM wiring on the backend yet — /api/investor/notifications is poll-only.
For v1, poll it on app foreground and on a WorkManager periodic job (e.g. every few minutes) and
show unseenCount as a badge. Do not build FCM push into this v1 — that requires creating a
Firebase project (a separate account/console step) and server-side sending code that doesn't
exist yet. Flag it clearly as a fast-follow once we're ready to add it, don't silently skip it.

## Screens (v1)
1. **Login** — email/password, calls POST /auth/login, stores token, navigates to Dashboard.
   Link to Register for a new account.
2. **Register** — email, password (min 6 chars, confirm-password field client-side), first/last
   name, phone, and a required "I accept the terms" checkbox (link/WebView to
   https://devtrader.ir/investor/auth/terms) that must be checked before the submit button is
   enabled. Calls POST /auth/register, stores token, navigates to Dashboard. Make clear in the UI
   that mirror trading itself still needs admin approval after this — registering alone doesn't
   activate real trading.
3. **Dashboard** — investor's wallet available/equity (from `assets`), open positions count,
   trade summary (win rate, total realized PNL, sample size vs total trades), and a prompt to
   bind an API key if `hasApiKey` is false.
4. **Debt Ledger** — scrollable list of `entries`, current `debt` balance pinned at top, color
   trade wins vs losses by `debtDelta` sign.
5. **Notifications** — list, unseen ones visually distinct, mark-all-seen on open.
6. **Alarms** — list of the investor's own price alarms (symbol, above/below, price) with delete
   swipe/button, and a form (symbol + condition dropdown + price) to add one via POST /alarms.
7. **Settings / Bind API Key** — form to paste key+secret, calls POST /bind-key, shows the
   success/failure message verbatim from the response.

## Security notes
- HTTPS only, no cleartext traffic (default network security config is fine, don't relax it).
- Never log the API key/secret or the auth token.
- Store the auth token in DataStore/EncryptedSharedPreferences, not plain SharedPreferences.
- This talks to a live production system handling real trading accounts — treat every write
  endpoint (bind-key especially) as something that needs a confirming UI step, not a fire-and-
  forget action.

Start by scaffolding the project (Gradle, package structure, DI if you want Hilt — optional for
this size of app), then the networking layer + login/register flow, then the five remaining
screens.
