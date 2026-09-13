---
name: project-tradebot-investor
description: "tradeBot Investor Android app — what it is, backend status, and v1 scope decisions"
metadata: 
  node_type: memory
  type: project
  originSessionId: fbbbaf49-479a-4474-b5cd-7211814ba1fe
---

`D:\projects\android\bot` holds "tradeBot Investor", a native Kotlin/Compose Android client
(package `ir.devtrader.investor`, minSdk 26) for an existing, live investor panel backend at
`https://devtrader.ir/api/investor`. The backend is already in production handling real trading
accounts. Built from the spec in [[reference_android_app_prompt]], which has been revised in place
several times — check that memory for how to find the current file.

**Why:** the backend has no FCM/push wiring, so v1 leans on two other channels: a Socket.IO
connection (live, only while foregrounded) plus WorkManager/in-app polling as the background-safe
fallback. Real FCM push is a flagged fast-follow requiring a separate Firebase project + server
sender that doesn't exist yet.

**Spec revision history (each superseded the last — scope only grows, nothing has been walked back):**
1. `android-app-prompt.md` — original 5 screens: Login, Dashboard, Debt Ledger, Notifications,
   Settings/Bind Key.
2. `android-app-prompt_static_alarm.md` — added Register (accept-terms checkbox gates submit,
   terms opened via `LocalUriHandler` rather than an in-app WebView) and Alarms
   (`GET/POST/DELETE /alarms`).
3. `android-app-prompt.symbol_dropdown.md` — `POST /alarms` gained optional `sms`/`call`; added
   `GET /symbols` backing a filterable `ExposedDropdownMenuBox` on the alarm symbol field (fetched
   once, filtered client-side, still free-text to match the API's own leniency).
4. `notification.md` — the big one, implemented in full:
   - **Real-time**: `io.socket:socket.io-client:2.1.1` connects to the root domain (NOT
     `/api/investor`) after login, bearer-token auth via `IO.Options().auth`. Wrapped in
     `data/remote/RealtimeGateway.kt`, connect/disconnect driven by `AppContainer`'s reaction to
     `SessionManager.isLoggedIn`. Two events: `notification` (live push side of GET
     /notifications) and `alarmTriggered` (alarm already deleted server-side by the time it
     arrives — one-shot, not recurring).
   - **`NotificationsCenter`** (`data/repository/`) is app-scoped, not screen-scoped — holds
     unseenCount/notifications state so the drawer badge and the socket feed stay correct even off
     the Notifications screen. `NotificationsViewModel` and `NotificationsPollWorker` both now read
     through it instead of hitting `InvestorRepository` directly.
   - **Toast-on-trigger lives in `NavGraph.kt`**, not any screen's ViewModel — a `SnackbarHostState`
     shared across the app collects both socket flows directly, since spec requires the alarm
     toast to fire even when the Alarms screen isn't open. `AlarmsViewModel` separately subscribes
     to `alarmTriggered` only to prune its own local list live when it *is* open.
   - **Navigation became a persistent drawer** (`ui/shell/AppShell.kt`, `ModalNavigationDrawer`):
     header (avatar initial, notifications bell+badge, logout) then a nav list — Dashboard,
     Positions, Trades, Debt Ledger, Alarms, Profile, Settings — mirroring the web sidebar. Every
     behind-drawer screen was refactored to drop its own `Scaffold`/`TopAppBar` and just render
     content into the padding `AppShell` provides; Login/Register stay pre-auth, own-Scaffold,
     outside the drawer. Notifications is reached via the header bell (not a nav-list item) and
     uses a back arrow instead of the hamburger.
   - **New screens**: Positions/Trades (`GET /positions`, `GET /trades` — shape not guaranteed, so
     kept as `List<JsonObject>` DTOs and rendered generically via `ui/common/GenericRecordCard`,
     which surfaces symbol/side/pnl-style fields and lists the rest as opaque key:value pairs) and
     Profile (`POST /profile`, prefilled from `GET /dashboard`'s `investor` object — there's no
     dedicated GET /profile — email read-only; note near phone that it's what SMS/call alarms use).
     This also resolved the earlier-noted gap: there IS now a profile-update endpoint for setting
     the phone number SMS/call alarms need.
   - **Known gap, not fixed**: `SessionManager.currentInvestor` is populated only on login/register
     success in-memory, never restored from the persisted token on cold start — so the drawer
     avatar shows "?" until the user has logged in this session. Low-stakes (cosmetic), left alone
     since fixing it wasn't asked for and would mean persisting investor profile data to disk.
   - Follow-up in the same session: `GET /symbols` was originally fetched from `AlarmsViewModel`
     itself (once per ViewModel instance, i.e. once per visit to the Alarms screen). Moved to a new
     app-scoped `data/repository/SymbolsCache.kt`, fetched from `AppContainer`'s same
     `isLoggedIn`-reactive block as the notifications/socket warmup — so it's fetched once at app
     start/login and the Alarms symbol dropdown just reads the in-memory cache, no network wait.
5. `all_settings.md` — supersedes `notification.md`:
   - `Investor` gained `isActive`, `frozen`, `frozenReason`, `profitSharePercent`. A new
     **Account status banner** is computed client-side (`util/AccountStatus.kt`,
     `computeAccountStatus()`) in a fixed priority order that must not be reordered: Pending
     Approval (`!isActive`) > Frozen > Trading Paused (`!tradingEnabled`) > Active — there's no
     single status field from the API, it's derived from those four values every time. Rendered as
     the first thing on Dashboard, above the wallet cards.
   - Two new write endpoints: `POST /settings/trading-enabled` (no body — toggles, doesn't set;
     response carries the authoritative new `tradingEnabled`, so the UI waits for it rather than
     flipping the switch optimistically-and-forgetting, and reverts on failure) and
     `POST /settings/margin-ratio` (`{marginRatio: number}`, 0–100).
   - **Settings screen was rebuilt**: `ui/settings/BindKeyScreen.kt` + `BindKeyViewModel.kt` were
     deleted outright and replaced by `SettingsScreen.kt`/`SettingsViewModel.kt` in the same
     package, now covering four independent sections on one screen — Profit Share (read-only),
     Trading Control (the toggle above), Margin Ratio (numeric input + save), and Bind API Key (the
     original flow, unchanged behavior). `Destinations.BIND_KEY` was renamed to `SETTINGS` and
     `DashboardScreen`'s `onOpenBindKey` callback to `onOpenSettings` to match, since the
     destination is no longer just about the API key.
- `GET /positions` was originally called out as having no dedicated screen — that's now stale,
  see revision 4 above.
- Fixed a pre-existing nav gap while adding Register (revision 2): `NavGraph.kt`'s
  `LaunchedEffect(isLoggedIn)` only reacted to logout; it now also reacts to login/register success
  by navigating forward to Dashboard.
- No Hilt — manual service locator (`AppContainer`) since the app is small.
- Auth token stored via EncryptedSharedPreferences (Keystore-backed), never plain SharedPreferences.
- Any 401 (no refresh-token flow exists) clears the session and the nav graph resets to Login.
- Bind-key write requires an explicit confirmation dialog before submitting — not fire-and-forget,
  since it connects a real exchange account.

**Version control:** the folder wasn't a git repo until 2026-09-13 — initialized, `.gitignore`
added (standard Android: `build/`, `.gradle/`, `.kotlin/`, `.idea` caches, etc.), and pushed to
[[reference_github_investorapp]]. See that memory and [[feedback_local_properties_tracked]] before
touching `.gitignore` or `local.properties` again.
**Known gap, not yet fixed**: the Gradle wrapper (`gradlew`, `gradlew.bat`,
`gradle/wrapper/gradle-wrapper.jar`) was never generated for this project — only
`gradle-wrapper.properties` exists (pins Gradle 8.9). No local Gradle/SDK tooling is available in
this environment to generate it the normal way (see [[env_no_local_android_build]]). Practically:
opening the project in Android Studio elsewhere still works (it offers to create the wrapper on
sync), but `./gradlew`/`gradlew.bat` from a terminal won't work until someone runs
`gradle wrapper --gradle-version 8.9` once, or the wrapper jar is fetched and the scripts written
by hand. Ask before doing the latter — it wasn't done automatically since it needed the user's call
on whether Android-Studio-only setup was good enough.

**How to apply:** when extending this app, keep polling AND the socket connection as the two
non-FCM notification mechanisms unless the user says the backend has grown FCM support. Always
check [[reference_android_app_prompt]] for the current spec file before assuming scope — it has
been revised in place multiple times without warning.
