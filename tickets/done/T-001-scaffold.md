# T-001 — Project scaffold

- **Role:** BUILD
- **Phase:** 0
- **Depends on:** none (needs owner answers to D-006, D-007, D-008, D-016 first; ask if still open)
- **Refs:** docs/ARCHITECTURE.md §1–2, §9 · AGENTS.md §5

## Goal
An empty but healthy Android project: three modules, version catalog, test tooling, app launches a placeholder screen.

## In scope
- Gradle Kotlin DSL project with `:domain` (pure Kotlin/JVM), `:data` (Android library), `:app` (Compose, Material 3).
- `gradle/libs.versions.toml` with only the dependencies needed now (versions verified as current stable at the time, not from memory).
- Test setup: JUnit + kotlin.test in `:domain`; a trivial passing test per module.
- `.gitignore`, package id from D-008, minSdk from D-007, edge-to-edge + dark-mode-ready theme stub.
- Fill the **Commands** section of AGENTS.md with the commands that actually work.

## Out of scope (do NOT do)
- Any feature, screen beyond a placeholder, database code, DI beyond what D-006 requires, CI, analytics/crash SDKs.

## Acceptance criteria
- [x] `./gradlew :domain:test` passes.
- [x] `./gradlew assembleDebug` builds; app installs and launches. (Verified 2026-10-30 on emulator medium_phone, Android 16 (API 36). Owner override: emulator pass replaces the owner's phone.)
- [x] `:domain` has no Android imports (verified).
- [x] AGENTS.md commands section updated to reality.

## Verification
Run the commands above; install on the owner's phone or an emulator.

## Handoff log
### 2026-10-30 — VERIFY
**Pass.** All four acceptance criteria pass. Commit `e80fda7ac9a4227ac003878e23399f76840aa035` on branch `t-001-scaffold` (fast-forwarded onto `main`). Device: emulator medium_phone, Android 16 (API 36). Owner override 2026-10-30: an emulator pass replaces the owner's phone.

**Per criterion:**
- `./gradlew :domain:test` — PASS. In this run the task was UP-TO-DATE (inputs unchanged since the last successful execution on this commit). Report `domain/build/test-results/test`: 143 tests, 0 failures, 0 errors, 0 skipped.
- `./gradlew assembleDebug` builds; app installs and launches — PASS. `assembleDebug` was UP-TO-DATE; `app/build/outputs/apk/debug/app-debug.apk` is present. `adb install -r` reported Success. Launcher activity from the manifest is `hu.toliver.muskli/.MainActivity`. It stayed in the foreground in light (pid 13807) and dark (pid 13932); `topResumedActivity` was `MainActivity` both times. Crash buffer empty both times; no `FATAL EXCEPTION` or `AndroidRuntime` lines. The current home screen (the original placeholder is gone) shows the title Muskli plus Start workout, History, Exercise library, and Settings.
- `:domain` has no Android imports — PASS. Search of `domain/` for `android.`, `androidx.`, and `com.android`: 0 matches.
- AGENTS.md commands section — PASS. The documented `:domain:test`, `testDebugUnitTest`, `assembleDebug`, and `lintDebug` commands completed in one `.\gradlew.bat` invocation (`BUILD SUCCESSFUL in 57s`; 129 tasks: 7 executed, 122 up-to-date). `testDebugUnitTest` reports: `:app` 77 tests (executed this run), `:data` 43 tests (task UP-TO-DATE), 0 failures. `lintDebug`: `:app` 0 errors and 1 warning (`NewerVersionAvailable`, JUnit BOM 5.14.4 vs 6.1.3; stay on JUnit 5 per the existing decision); `:data` no issues. Report path `app/build/reports/lint-results-debug.html` matches the comment. `adb install -r app/build/outputs/apk/debug/app-debug.apk` matches the documented install line and succeeded. `connectedDebugAndroidTest` was not re-run; it remains the documented instrumented-test command.

**Screenshots** (under `build/verify/`, git-ignored, not committed):
- `build/verify/t001-light.png` — light mode (`cmd uimode night no`): white home screen, Muskli title, orange Start workout, outlined History / Exercise library / Settings.
- `build/verify/t001-dark.png` — dark mode (`cmd uimode night yes`): black home screen, same controls, lighter orange Start workout.

**Commands and results:**
- `$env:JAVA_TOOL_OPTIONS = "-Djavax.net.ssl.trustStoreType=Windows-ROOT"; .\gradlew.bat :domain:test testDebugUnitTest assembleDebug lintDebug --console=plain` → BUILD SUCCESSFUL in 57s.
- `adb devices -l` → `emulator-5554 device` (`sdk_gphone64_x86_64`). `getprop ro.build.version.release` → 16; `ro.build.version.sdk` → 36; `emu avd name` → `medium_phone`.
- `adb install -r app/build/outputs/apk/debug/app-debug.apk` → Success.
- Light: `adb shell cmd uimode night no`, `am force-stop hu.toliver.muskli`, `am start -n hu.toliver.muskli/.MainActivity`, `screencap` to `/sdcard/t001-light.png`, `adb pull` to `build/verify/t001-light.png`. No fatal log lines.
- Dark: same sequence with `cmd uimode night yes` and `t001-dark.png`. No fatal log lines.

**Left:** none. Moved to `tickets/done/`.

### 2026-10-29 — REVIEW
Owner review: no findings (Blocker / Should-fix / Nit: none). Moved to `verify/`. Remaining check is the phone install already noted below; the emulator run stands.

### 2026-10-28 — BUILD
**Done.** Three-module Gradle Kotlin DSL project: `:domain` (Kotlin/JVM, JUnit 5 + kotlin.test per D-016), `:data` (Android library, depends on `:domain`), `:app` (Compose + Material 3, Hilt per D-006, `hu.toliver.muskli` per D-008, minSdk 24 / targetSdk 37 per D-007). Hilt wiring is limited to `@HiltAndroidApp MuskliApplication` and `@AndroidEntryPoint MainActivity`. `PlaceholderScreen` is edge-to-edge, takes its text from `strings.xml`, and has light and dark previews. `MuskliTheme` uses Material default light/dark schemes until T-008. One trivial passing test per module. `.gitignore` now also ignores `.kotlin/`.

**Versions** (latest stable, checked 2026-10-28 against Google Maven, Maven Central, and services.gradle.org): Gradle 9.8.2, AGP 9.5.0, Kotlin 2.4.21, KSP 2.3.12, Hilt 2.60.1, Compose BOM 2026.09.00, activity-compose 1.13.0, JUnit 5.14.4, JUnit 4.13.2, compileSdk/targetSdk 37 (newest stable platform). No navigation, lifecycle, serialization, coroutines, or androidTest libraries yet; later tickets add them.

**Verified (all green):**
- `.\gradlew.bat :domain:test` ran 1 test, 0 failures.
- `.\gradlew.bat assembleDebug testDebugUnitTest` passed (`:app` and `:data` tests).
- `.\gradlew.bat lintDebug` passed with 0 errors. Warnings: `NewerVersionAvailable` (JUnit 6.1.3 exists; kept on 5.x per D-016) and `ObsoleteLintCustomCheck` (from a library's lint jar).
- `.\gradlew.bat connectedDebugAndroidTest` passed on the `medium_phone` emulator (API 36). There are no instrumented tests yet.
- Installed on the emulator and launched; the placeholder renders in light and dark mode with no app crash.
- Searched `domain/` for `android.`, `androidx.`, and `com.android` imports: 0 matches.

**Gotchas:**
- AGP 9 has built-in Kotlin, so Android modules don't apply `org.jetbrains.kotlin.android`. The root build declares `kotlin-jvm` with `apply false` so AGP uses the catalog's Kotlin version instead of its bundled one.
- Gradle prints one deprecation (`Configuration.setVisible`, removal in Gradle 11). It's raised while configuring `:app`, so it comes from the Hilt or KSP plugin, not our scripts. Recheck when those plugins update.
- This machine sits behind Zscaler. Java rejects the proxy's certificates unless the shell sets `JAVA_TOOL_OPTIONS=-Djavax.net.ssl.trustStoreType=Windows-ROOT`. That variable crashes the `android` CLI, so unset it before using the CLI. Nothing proxy-specific is committed.
- `android:allowBackup="true"` is intentional (D-024, 2026-10-29): Android Auto Backup may include workout data. No exclusion rules in this scaffold.

**Left:** install on the owner's phone (VERIFY).

**Open questions:** none. Owner confirmed 2026-10-29: stay on JUnit 5; do not upgrade to JUnit 6 for now.
