# T-021 — Catalogue full sync

- **Role:** BUILD
- **Phase:** 1
- **Depends on:** T-010 (format and version field from T-007; loader rules from D-021)
- **Refs:** D-005, D-021 · docs/ARCHITECTURE.md §2 · SPEC F-01 AC1

## Goal
When online, replace the stored published catalogue with a newer GitHub Pages bundle. Offline, the bundled library from T-010 stays usable.

## In scope
- GET the published catalogue URL from T-007 using the platform HTTP API (`HttpURLConnection`). No new HTTP library.
- Apply only when the remote version is newer. Same upsert rules as T-010: by stable id; never modify custom exercises; do not hard-delete a published exercise a workout references; archive unreferenced rows missing from the bundle.
- Sync failure or no network: keep the current library and do not block startup.
- Tests with a fake HTTP source: newer version applies, older/equal does not, failure leaves data unchanged, custom rows survive.

## Out of scope (do NOT do)
- Delta-sync. UI for sync status beyond a silent failure. Editing the catalogue format (that's T-007).

## Acceptance criteria
- [x] A newer remote bundle upserts; an older or equal version does not.
- [x] Network failure leaves the stored catalogue unchanged and the app still starts.
- [x] Custom exercises are unchanged after sync.

## Verification
`./gradlew testDebugUnitTest`

## Handoff log
- 2026-10-30 VERIFY: Pass. Moved to `done/`.
  - **Commit verified:** `55ba7f1` (T-021: review and move to verify), on top of `45844d5`. Device: emulator medium_phone, Android 16 (API 36) (`emulator-5554`, `sdk_gphone64_x86_64`).
  - **AC1 — newer remote bundle upserts; older or equal does not: PASS on unit tests.** The live happy path cannot be shown on this machine. GitHub Pages is not published yet (T-022), and a TLS-inspecting proxy breaks the emulator's HTTPS, so a strictly newer bundle never arrives. `CatalogueSyncTest` asserts it: `newer remote bundle upserts by id` (title becomes "Barbell Bench Press", `createdAtEpochMs` stays 1000, `updatedAtEpochMs` becomes 2000, `isCustom` false, stored version 2, `sync()` true) and `older or equal remote bundle is not applied` (equal version 3 and older version 2 both return false, the library map is unchanged, stored version stays 3, plank stays unarchived).
  - **AC2 — network failure leaves the stored catalogue unchanged and the app still starts: PASS.** Same test class: `transport failure leaves the stored catalogue unchanged` (`IOException`, `sync()` false, rows and version 1 unchanged), plus `non-200 response`, `malformed json`, and `unknown format version` with the same unchanged-library assertion. On the emulator, after `pm clear` and creating one custom exercise: library count 10 bundled + 1 custom. Force-stop, `cmd connectivity airplane-mode enable` (`airplane_mode_on=1`), cold start: process stayed up (pid 8764), home showed Start workout, log `W/CatalogueSync: Catalogue sync failed`, no `FATAL EXCEPTION`. Library still showed the bundled titles and VerifyCurl (`Abdominals · None · Custom`). Airplane off (`airplane_mode_on=0`), force-stop, cold start: process stayed up (pid 9194), same library, log `W/CatalogueSync: Catalogue sync failed` / `SSLHandshakeException: Trust anchor for certification path not found` at `HttpCatalogueSource.fetch` (`CatalogueHttpSource.kt:31`). No `FATAL EXCEPTION`.
  - **AC3 — custom exercises unchanged after sync: PASS.** Test `custom exercises are unchanged after sync`: a custom `dumbbellRow` is unchanged when the newer bundle reuses its id, while bench-press still updates. On the emulator the failed syncs left VerifyCurl in the library with the same subtitle (`Abdominals · None · Custom`) after both cold starts.
  - **Evidence:** `build/verify/` (not committed). `20-home.png` home after clear. `22-library-scrolled.png` the 10 bundled titles. `29-search.png` VerifyCurl / Custom. `33-count.png` count 11 (bundled titles plus VerifyCurl). `40-airplane-home.png` and `41-airplane-library.png` airplane cold start. `50-online-home.png` and `51-online-library.png` proxy cold start. Log excerpts in `40-airplane-logcat-long.txt` and `50-online-logcat.txt`.
  - **Commands:** `$env:JAVA_TOOL_OPTIONS = "-Djavax.net.ssl.trustStoreType=Windows-ROOT"; .\gradlew.bat :domain:test testDebugUnitTest assembleDebug lintDebug` → BUILD SUCCESSFUL in 45s (129 tasks: 13 executed, 116 up-to-date). Test tasks were UP-TO-DATE. Result XML from the last execution on this code (`2026-10-30T18:18:50Z`): `:domain:test` 143, `:data` `testDebugUnitTest` 51 (`CatalogueSyncTest` 8), `:app` `testDebugUnitTest` 77, 0 failures, 0 errors. Lint: `:data` no issues; `:app` 0 errors, 1 warning `NewerVersionAvailable` on `junit-bom` in `gradle/libs.versions.toml` (pre-existing). `adb install -r` Success. `pm clear`, UI via `uiautomator dump` and `input tap`/`input text`. Screenshots: `screencap -p` then `adb pull` (a black guest buffer before reboot; after reboot, with the emulator window on-screen, screencap showed the UI). Airplane mode turned back off at the end (`airplane_mode_on=0`).
  - **Gotcha:** the emulator window started off-screen, the guest framebuffer was black, `input tap` did not hit a window, and a "Process system isn't responding" dialog appeared. `adb reboot`, then the window was moved on-screen, and the steps above were run on that booted image. Not a product failure.
  - **Left:** nothing in scope. T-022 still owns publishing the catalogue URL.
- 2026-10-30 REVIEW (round 1): No blockers, no should-fixes. Moved to `verify/`.
  - **Verdict:** The diff does T-021 and nothing else. `HttpCatalogueSource` is a platform `HttpURLConnection` GET of the D-025 URL (connect 10 s, read 15 s, `disconnect()` in `finally`). No new dependency, no schema change, no UI, no T-022. `INTERNET` is the only manifest addition. Apply stays on the T-010 `CatalogueApplier` / `:domain` `planCatalogueUpdate`: strictly newer version, upsert by id, custom rows skipped, a referenced published row kept, an unreferenced missing row archived, no hard-delete. `CatalogueSync` catches transport errors, non-200, malformed JSON, and an unknown `formatVersion`, returns false, and does not throw. `MuskliApplication` runs the bundled load and then sync in one `appScope` coroutine (`SupervisorJob` + `Dispatchers.Default`); `sync()` uses `Dispatchers.IO`, and `onCreate` does not wait, so startup is not blocked and the rest-timer launch is separate. `CancellationException` is rethrown. D-035 is one table row, status **decided**, with the owner-delegated note and a one-line reason. No existing test was edited.
  - **Findings:**
    - Blocker: none.
    - Should-fix: none.
    - Nit (ignored, no ticket): `HttpCatalogueSource` (`data/src/main/kotlin/hu/toliver/muskli/data/seed/CatalogueHttpSource.kt` lines 26-27) sets socket connect and read timeouts. On some Android versions the DNS lookup inside `URL.openConnection()` is not covered by `connectTimeout`, so a stuck resolver can hold that coroutine past 10 s. It does not block startup: `MuskliApplication.onCreate` launches the work and returns (`app/src/main/kotlin/hu/toliver/muskli/MuskliApplication.kt` lines 30-37), and a socket timeout still leaves the library unchanged via the `IOException` path. D-035 already chose these two timeouts. No follow-up ticket.
  - **Checks:** `.\gradlew.bat :domain:test testDebugUnitTest assembleDebug lintDebug` → BUILD SUCCESSFUL (assemble and the lint analysis were UP-TO-DATE for `45844d5`; `:data:lintDebug` and `:app:lintDebug` ran). Then `.\gradlew.bat :domain:test testDebugUnitTest --rerun-tasks` → BUILD SUCCESSFUL in 2m 18s, 76 tasks executed. `:domain:test` 143, 0 failures. `testDebugUnitTest`: `:data` 51 (0 failures, `CatalogueSyncTest` 8), `:app` 77 (0 failures). Lint: `:data` no issues; `:app` 0 errors, 1 warning `NewerVersionAvailable` on `junit-bom` in `gradle/libs.versions.toml` (pre-existing, not this diff).
  - **Left:** VERIFY. On a device or emulator: with no network the process stays up and the stored library is unchanged; when TLS to the published URL succeeds, a strictly newer bundle replaces the stored published rows.
- 2026-10-30 BUILD: Full sync of the published catalogue when its version is strictly newer.
  - **Done:** `CatalogueHttpSource` (fakeable) and `HttpCatalogueSource` (`HttpURLConnection` GET of `https://t0liver.github.io/muskli/catalogue/v1/catalogue.json`, connect 10 s, read 15 s). `CatalogueSync` parses with the T-010 parser and applies through the existing `CatalogueApplier` / `catalogue_meta` (no second upsert path, no schema change, no new dependency). `MuskliApplication` runs it in the same off-main startup coroutine, after the bundled load. `INTERNET` added. Transport errors, non-200, malformed JSON, and an unknown `formatVersion` are logged and swallowed; the library stays as it is.
  - **D-035 (auto-accepted recommendation, owner-delegated 2026-10-30):** one coroutine, bundle then sync, so an older asset cannot overwrite a newer download; those timeouts; no `If-None-Match`.
  - **Decision logic:** already in `:domain` `planCatalogueUpdate` (T-010). Not duplicated. A referenced published row missing from the bundle stays active; an unreferenced one is archived; neither is hard-deleted.
  - **Tests:** `CatalogueSyncTest` 8, with a fake source: newer upserts (creation time kept); older and equal do not; `IOException`, HTTP 404, malformed JSON, and `formatVersion` 2 leave the rows and stored version unchanged; a custom row survives even when the bundle reuses its id; referenced `plank` stays, unreferenced `pull-up` is archived.
  - **Commands:** `.\gradlew.bat :domain:test testDebugUnitTest assembleDebug lintDebug` → BUILD SUCCESSFUL. `:domain:test` re-run: 143 tests, 0 failures. `testDebugUnitTest`: `:data` 51 (0 failures, 8 of them `CatalogueSyncTest`), `:app` 77 (0 failures). Lint: `:data` no issues; `:app` 0 errors, 1 warning `NewerVersionAvailable` on `junit-bom` in `gradle/libs.versions.toml` (pre-existing, not this diff).
  - **Emulator smoke** (emulator-5554, `sdk_gphone64_x86_64`, Android 16 / API 36): `adb install -r` succeeded. Network on: process stayed up; log `Catalogue sync failed` / `SSLHandshakeException: Trust anchor for certification path not found` (TLS-inspecting proxy — the GET never reached an HTTP status, so a live 404 was not observed). Airplane mode (`cmd connectivity airplane-mode enable`, `airplane_mode_on=1`): force-stop, relaunch, process stayed up, log `Catalogue sync failed`, no `FATAL EXCEPTION`. Airplane mode turned back off (`airplane_mode_on=0`).
  - **Gotchas:** the emulator does not trust the proxy CA, so this device cannot tell a 404 from a dead network until that cert is in the system trust store. `If-None-Match` stays out (optional in CATALOGUE §7). T-022 (CI/Pages) not built.
  - **Left:** nothing in scope.
- 2026-10-28 DOCS: Created when D-021 split network sync out of T-010. Platform HTTP only.
