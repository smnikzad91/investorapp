# tradeBot Investor (Android)

Native Kotlin/Compose client for the investor panel at `https://devtrader.ir/api/investor`.

## Stack

- Jetpack Compose (Material 3), Navigation-Compose
- Retrofit + OkHttp + kotlinx.serialization
- ViewModel + StateFlow, manual DI via `AppContainer` (no Hilt — not worth it at this size)
- EncryptedSharedPreferences (AndroidX Security / Keystore-backed) for the auth token
- WorkManager periodic job for notification polling (see caveat below)
- `minSdk 26`, `compileSdk`/`targetSdk 34`, package `ir.devtrader.investor`

## Opening the project

This was scaffolded outside Android Studio, so the Gradle wrapper jar (`gradle/wrapper/gradle-wrapper.jar`)
isn't present — only `gradle-wrapper.properties` (pinned to Gradle 8.9). Open the folder directly in
Android Studio (Hedgehog/Iguana or newer with AGP 8.5 support); it will offer to generate the wrapper
and sync automatically. If you'd rather do it from the command line first, run `gradle wrapper --gradle-version 8.9`
once with a local Gradle install, then use `./gradlew` as normal.

## What's implemented

- **Login** — POST `/auth/login`, token stored via `TokenManager` (EncryptedSharedPreferences),
  navigates to Dashboard on success.
- **Dashboard** — wallet available/margin/PNL from `assets`, open positions count, trade summary
  (win rate, total realized PNL, sample size vs. total trades), "bind your API key" prompt when
  `hasApiKey` is false, `accountError` shown as a dismissible-style banner rather than a hard error.
- **Debt Ledger** — running `debt` balance pinned at top, entries colored by `debtDelta` sign
  (red = debt increased, green = debt decreased).
- **Notifications** — list, unseen visually distinct (bold + tinted by type), marks all seen when
  the screen is opened (mirrors `POST /notifications/seen`).
- **Settings / Bind API Key** — key + secret form, requires an explicit confirmation dialog before
  submitting (per the brief: write endpoints, bind-key especially, shouldn't be fire-and-forget),
  shows the backend's success/failure message verbatim, surfaces `tradePermissionWarning` as a
  soft warning on success.
- **Session handling** — any 401 from any endpoint (except login itself) clears the stored token
  via an OkHttp interceptor and the nav graph reacts by clearing the back stack and returning to
  Login — there's no refresh-token endpoint yet, so this is intentionally a hard reset.
- **Security** — `usesCleartextTraffic="false"` + an explicit network security config, HTTP logging
  interceptor capped at `BASIC` (headers/bodies are never logged, since they can carry the token or
  the investor's exchange key/secret), token never touches plain `SharedPreferences`.

## Push notifications — not implemented, by design

The backend has no FCM/push sender yet (per the brief). `/notifications` is polled instead:
- in-app, when the Notifications screen is opened, and
- in the background, via a `WorkManager` `PeriodicWorkRequest` in `NotificationsPollWorker`
  (`TradeBotApplication`), which surfaces `unseenCount` as a local system notification/badge.

Two things worth knowing:
- WorkManager enforces a **15-minute minimum** interval for periodic work — that's the closest
  this can get to "every few minutes" without exact alarms.
- **Fast-follow, not yet built:** real push (FCM) needs a Firebase project (separate console/account
  setup) and a server-side sender that doesn't exist yet. Flagging this explicitly rather than
  silently shipping only polling as if it were the final design.

## Not built in v1

`GET /positions` is documented in the API but has no dedicated screen in the v1 spec (the dashboard's
`openPositionsCount` comes from `/dashboard` directly) — left out to avoid building UI nobody asked for yet.
