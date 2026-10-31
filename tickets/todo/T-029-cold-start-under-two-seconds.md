# T-029 — Cold start to ready-to-log under 2 seconds

- **Role:** BUILD
- **Phase:** 1
- **Depends on:** T-019
- **Refs:** AC-08 · SPEC §8
- **Size:** fits one session

## Goal
A cold start of the debug build reaches the home screen in under 2 seconds.

## In scope
- Find what makes `am start -W` TotalTime exceed 2 s after the app has already been opened once, and remove that work from the startup path.
- Keep the bundled catalogue load and a failed network sync from blocking first frame.

## Out of scope (do NOT do)
- A new splash screen, lazy feature flags, or measuring on a physical phone.

## Acceptance criteria
- [ ] On emulator medium_phone, Android 16 (API 36), three `am force-stop hu.toliver.muskli` then `am start -W -n hu.toliver.muskli/.MainActivity` runs are all `LaunchState: COLD`, and the median TotalTime is under 2000 ms.
- [x] The home screen shows Start workout (or Resume if a draft exists) when that start returns.

## Verification
Repeat the three-start measurement above with data already in the database. Record each TotalTime.

## Notes / decisions needed
Found in T-019 (2026-10-30), commit `893cbd94`, emulator-5554. Official trio after workouts were imported: 6011 ms, 3743 ms, 3398 ms (median 3743). Incidental cold starts the same day: 2107 ms (first launch after install), 3966 ms (first launch after `pm clear`), 2541 ms (reopen onto a live draft). Home did show Start workout, so the window was the logger, just late.

## Handoff log
<!-- Newest first. Each entry: date, role, what was done, what's left, gotchas, commands run + results. -->

### 2026-10-31 · REVIEW · bar NOT met, back to todo; only the lazy init is merged
**Result:** acceptance criterion 1 is **not met** and stays unticked. Official trio on the branch: 4818, 3040, 2502 ms (median **3040**), against `main` 2655, 3641, 2663 (median 2663) and 5-run medians of 2896 (`main`) vs 2731 (branch). The required median is under 2000 ms. The ±1 s run-to-run noise on this emulator is larger than any gain measured.

**Findings:**
- **Blocker (reverted):** uncompressed dex (`packaging.dex.useLegacyPackaging = false`) grew the APK from 13.9 to 35.2 MB in every build type, release included, for a gain smaller than the noise, and the official trio was slower than `main`. Reverted; `app/build.gradle.kts` is identical to `main` again. D-061 now records it as tried and rejected (auto-accepted recommendation, owner-delegated 2026-10-30).
- **Kept:** `MuskliApplication` injects `BundledCatalogueLoader`, `CatalogueSync` and `RestTimerSync` as `dagger.Lazy` and resolves them in the worker coroutines. Correct: the database is `@Singleton` (Hilt's double-check is thread-safe, also against the activity's own injection on the main thread), the constructors only read the Context and `AlarmManager`, and order, try/catch and logging are unchanged. No main-thread I/O, no new tap, no dependency, schema or API change. Merged to `main` with this review.

**Still open (owner decision, not taken):** how AC-08 is measured. Options: (1) measure a release / non-debuggable build or a dexopted install; (2) a stronger emulator (more RAM, host GPU); (3) the owner's phone, which is what SPEC §8 says. Even with verified dex the emulator median was ~2.2 s (see the BUILD entry), so app-side work alone is unlikely to reach 2 s on this emulator.

**Commands:** `$env:JAVA_TOOL_OPTIONS='-Djavax.net.ssl.trustStoreType=Windows-ROOT'; .\gradlew.bat :domain:test testDebugUnitTest assembleDebug lintDebug`: BUILD SUCCESSFUL; lint shows only the existing `NewerVersionAvailable`. `app-debug.apk` after the revert: 14,605,119 bytes (13.9 MiB). No new start-time measurement in this review.

### 2026-10-31 · BUILD · bar NOT met (median 3040 ms), safe improvement committed
**Result:** acceptance criterion 1 is **not met** and stays unticked; the number was not changed. Criterion 2 met: after the start returned, a `uiautomator dump` shows Muskli / Start workout / Routines / History / Exercise library / Settings (no draft in the DB).

**Environment:** emulator-5554, AVD medium_phone, API 36 (`google_apis_playstore`, 4 vCPU, 2.5 GB RAM, ~1 GB swap in use), base commit `1999edd`. DB with data (3 workouts, 18 sets, 17 templates). Each trio is install, one warm-up launch, then 3 × (`am force-stop` + `am start -W`). All runs `LaunchState: COLD`.

| Build | TotalTime (ms) | Median |
|---|---|---|
| `main` before, first trio of the session | 2655, 3641, 2663 | 2663 |
| `main` before, 5 runs after an emulator reboot, same conditions as the next row | 2989, 2667, 2562, 2896, 2994 | 2896 |
| branch, 5 runs, same conditions | 2731, 2981, 2554, 3299, 2707 | 2731 |
| **branch, official trio (final build)** | **4818, 3040, 2502** | **3040** |
| diagnostic only: branch after `cmd package compile -m verify -f` (then reinstalled to undo) | 2246, 2232, 2069 | 2232 |

Variance on this emulator is ±1 s between identical runs (6–8 s right after a reboot), so one trio is weak evidence either way.

**Where the time goes** (Perfetto, one 2690 ms `main` start, then one 2272 ms branch start):
- The app has **no dexopt output** (`pm art dump`: `status=run-from-apk`). The emulator has `pm.dexopt.install-fast=skip`, and `adb install` leaves this debuggable APK unverified. So every cold start runs ART's dex verifier in `bindApplication` (~215 ms for the 27 dex files, 35 MB) and verifies ~2500 classes at first use on the main thread (~600 ms; about 270 ms is Compose and ~165 ms is app/Hilt-generated code).
- **Before:** dex was compressed (AGP default below minSdk 28), so ART also extracted it on every start: `Extract dex file` ~255 ms; `bindApplication` 761 ms.
- The emulator's GL stack: `earlyPreloadGlContext` 570 ms on RenderThread, which `activityResume` waits on (~260 ms asleep). In the first frame, buffer allocation and shader compilation leave the main thread asleep ~385 ms.
- First frame (`Choreographer#doFrame`) ~1000 ms: Compose init 180–200 ms, first recompose ~210–240 ms, measure/layout/draw, plus the GPU waits above.
- Startup work of our own was already off the main thread. The catalogue (2.5 KB) load and the failing sync (TLS error on the emulator) never block the first frame.

**Changed:**
- `app/build.gradle.kts`: `packaging.dex.useLegacyPackaging = false`, so dex is stored and mapped in place. Trace: `Extract dex file` gone, `bindApplication` 761 → 455 ms. APK 13.9 → 35.2 MB (D-061).
- `MuskliApplication`: `BundledCatalogueLoader`, `CatalogueSync` and `RestTimerSync` are injected as `dagger.Lazy` and resolved inside the worker coroutines. The Room database, DAOs and HTTP source are no longer built on the main thread in `Application.onCreate`. The behaviour and order are unchanged.
- Tried and dropped: starting the catalogue from a main-looper idle handler. It sometimes fired between `bindApplication` and the activity launch, before the first frame, and gave no measurable gain.

**What remains / options for the owner** (none taken; each is an owner call):
1. Even with verified dex (diagnostic row) the median is ~2.2 s: Compose's first frame in a debuggable build plus the emulator GL cost. App-side, the remaining main-thread work is small. Not creating `HistoryViewModel` at Home would save tens of ms, not hundreds.
2. Measure a non-debuggable build (release / R8) or a dexopted install. This changes what AC-08 measures, so it needs the owner.
3. An emulator with more RAM or host GPU, or measuring on the owner's phone as SPEC §8 says. The phone is out of scope for this ticket.

**Commands:** `.\gradlew.bat assembleDebug`; `adb install -r`, then `am force-stop` / `am start -W` trios (script above); Perfetto (`sched`, `am wm view gfx dalvik res database` atrace) analysed with the `perfetto` Python trace processor; `.\gradlew.bat :domain:test testDebugUnitTest assembleDebug lintDebug`, which passed: BUILD SUCCESSFUL, lint shows only the existing `NewerVersionAvailable` in `libs.versions.toml`.
