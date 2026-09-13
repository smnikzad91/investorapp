---
name: reference-android-app-prompt
description: Where the authoritative spec for the tradeBot Investor Android app lives
metadata: 
  node_type: memory
  type: reference
  originSessionId: fbbbaf49-479a-4474-b5cd-7211814ba1fe
---

The full product/API spec for the tradeBot Investor Android app (screens, backend endpoints,
security requirements) lives as a loose `.md` file directly in `D:\projects\android\bot\` — the
user drops in a new revision under a NEW, unpredictable filename each time (seen so far, oldest to
newest: `android-app-prompt.md`, `android-app-prompt_static_alarm.md`,
`android-app-prompt.symbol_dropdown.md`, `notification.md`, `all_settings.md` — note the naming is
not consistent, don't assume future ones match an `android-app-prompt*` glob). Each revision has
fully superseded the last (scope only grows). Before assuming current scope, glob
`D:\projects\android\bot\*.md`, skip `README.md`, and check file contents/mtimes to find the
newest spec revision — don't rely on filename pattern-matching alone. Treat whichever is current as
the source of truth for scope — check it before adding or changing screens/endpoints, since it
explicitly calls out things to include (confirming UI on writes) and exclude (FCM push in v1). See
[[project_tradebot_investor]] for the project built from it, including a running log of what each
revision added.
