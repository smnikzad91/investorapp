---
name: env-no-local-android-build
description: "The original dev machine's shell had no Gradle/Android SDK wired up — couldn't compile Android projects via Bash/PowerShell there"
metadata: 
  node_type: memory
  type: project
  originSessionId: fbbbaf49-479a-4474-b5cd-7211814ba1fe
---

On the machine this project was originally scaffolded on, the shell environment (Bash/PowerShell
tools) had JDK 23 available but no `gradle` on PATH, no `ANDROID_HOME`/`ANDROID_SDK_ROOT` set, and
no discoverable Android SDK folder. Android Studio itself was installed there, so it had its own
bundled Gradle/SDK, but that wasn't exposed to the terminal tools. **This is a note about that
specific machine — check whether it still applies wherever you're reading this.**

**Why this mattered:** the Android project ([[project_tradebot_investor]]) couldn't be
built/verified with `./gradlew` or `gradle` from Bash/PowerShell there — attempts failed with
"command not found" or missing SDK errors. Also, the freshly scaffolded project never got a
`gradle-wrapper.jar` (only `gradle-wrapper.properties`) since that binary couldn't be generated
that way — see the "Known gap" note in [[project_tradebot_investor]].

**How to apply (if this still applies to your machine):** don't try to run Gradle builds from the
terminal as a verification step. Instead, do careful manual/static review of Kotlin/Gradle files,
and open the project in Android Studio to sync (which will generate the wrapper jar) and get real
compile feedback. If terminal-verified builds are wanted, point at a CLI Android SDK/Gradle install
or provide `ANDROID_HOME`.
