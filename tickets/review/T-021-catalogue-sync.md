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
