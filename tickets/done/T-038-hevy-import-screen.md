# T-038 — Hevy import screen: pick file, preview, map exercises, confirm

- **Role:** UI
- **Phase:** 1
- **Depends on:** T-037
- **Refs:** SPEC F-09 AC1, AC2 · `docs/HEVY-IMPORT.md` §3, §4 · D-040, D-043, D-044 · AC-07
- **Size:** fits one session

## Goal
From Settings, the owner picks a Hevy workout CSV, sees what will be imported, reviews and changes how each Hevy exercise is mapped, confirms, and gets a result. Unmapped exercises are always listed, never dropped.

## In scope
- Settings, Backup section: an **"Import from Hevy"** button under the existing import button. Opens `Route.HevyImport` (add it to `AppRoutes.kt` with encode/decode).
- `HevyImportScreen` (stateless) + `HevyImportViewModel` (`StateFlow<HevyImportUiState>`), states:
  - **Pick:** short hint and "Choose workout file" (SAF `OpenDocument`, MIME `*/*`; the parser decides, D-040).
  - **Busy** while reading or applying (progress indicator).
  - **Rejected:** "Nothing was changed", the parser errors with record number and column (first 20, then "and N more"), "Choose another file". A measurement file picked here shows "This is a measurements file; choose the workout file first".
  - **Preview:** summary (workouts in file, new, duplicates skipped, sets, date range, "N RPE values are not stored") and the exercise list: one row per distinct Hevy title with its resolution text: "Library: <title>" (exact or normalised, normalised marked as "check"), or "New custom exercise (<type>)". **Unmatched rows first.** Tapping a row opens a dialog with "Create as custom exercise" and a searchable list of library exercises (`searchExercises`, archived hidden, tied candidates shown on top). Buttons: "Import N workouts" (disabled when 0 new) and Cancel.
  - **Done:** "Imported N workouts, M new custom exercises, K duplicates skipped", button back to Settings.
- All strings in `strings.xml` (plurals for counts), one `@Preview` per state (light and dark), 48dp targets.
- ViewModel unit tests with fake use cases: rejected file, preview with defaults, changing a resolution updates the summary (new custom count), apply success.

## Out of scope (do NOT do)
- Measurements file and body weight checkbox (T-039).
- Editing type, muscles, or equipment of the new custom exercises in this screen (the library editor does that afterwards).
- Any change to the live-workout screen or the existing backup import.

## Acceptance criteria
- [x] Picking the T-035 fixture (pushed to the emulator) shows 2 new workouts, 13 sets, 2 RPE values not stored, and 3 unmatched exercises listed first.
- [x] Mapping one unmatched exercise to a library exercise lowers the new custom count to 2 and the import uses that exercise.
- [x] Confirming imports; History shows both workouts with their sets; the Library shows the new custom exercises.
- [x] Picking the same file again shows 0 new and 2 duplicates, and the import button is disabled.
- [x] A non-Hevy file shows the Rejected state and changes nothing.
- [x] Previews exist for every state; dark mode works; no hardcoded strings.
- [x] `./gradlew :domain:test testDebugUnitTest assembleDebug lintDebug` pass with no new lint warnings.

## Verification
`.\gradlew.bat :domain:test testDebugUnitTest assembleDebug lintDebug`; on an emulator: `adb push domain/src/test/resources/hevy/workout_data.csv /sdcard/Download/`, then run the acceptance criteria by hand and note results in the Handoff log.

## Notes / decisions needed
None open: D-040, D-043, D-044. The screen is outside the live-workout flow, so the "no extra taps" rule does not apply.

## Handoff log
<!-- Newest first. Each entry: date, role, what was done, what's left, gotchas, commands run + results. -->
- **2026-10-31, VERIFY.** Build `30c8d5a` (includes the review fix `db39604`), `app-debug.apk` installed with `adb install -r` on `emulator-5554` (AVD medium_phone, `sdk_gphone64_x86_64`, Android 16 / API 36). Fixture `domain/src/test/resources/hevy/workout_data.csv` pushed to `/sdcard/Download/`; personal `docs/fixtures/` files not used. Screenshots in `build/verify/T-038/` (git-ignored).
  - **On the builder's data (fixture already imported):** Settings → Backup → "Import from Hevy" → Pick → workout_data.csv → 2 in file, 0 new, 2 duplicates, 0 sets, 0 new customs; "Import 0 workouts" disabled (parent node `enabled=false`) and a tap does nothing (`04-preview-again`). History still lists Push, heavy (Mar 14) and Legs (Mar 12). **Pass.**
  - **Fresh run:** app data snapshotted with `run-as … tar`, `pm clear`, then: preview 2 in file, 2 new, 0 duplicates, **13 sets**, 3 new customs, Mar 12–14 2026, **2 RPE values are not stored**; Lateral Raise (Dumbbell), Goblet Squat (Kettlebell), Assisted Pull Up first with "Not found in the library" (`05-preview-fresh`). **AC1 pass.** Tapped Assisted Pull Up, searched "assisted", chose Machine Assisted Pull-Up (`06-mapping-dialog`): row shows "Library: Machine Assisted Pull-Up", count **2** (`07-mapped`). **AC2 pass.** "Import 2 workouts" → "Imported 2 workouts, 2 new custom exercises, 0 duplicates skipped." (`08-done`). History: Push, heavy (8 set rows in detail: W, 1, F, 1, D, 1, 1, 1; card "7 sets" leaves out the warm-up, existing behaviour) and Legs (5 sets, uses Machine Assisted Pull-Up) (`09-history`, `10-push-detail`); Library shows Goblet Squat (Kettlebell) and Lateral Raise (Dumbbell) as Custom, no "Assisted Pull Up" custom (`12-library-*`). **AC3 pass.** Same file again → 0 new, 2 duplicates, button disabled (`13-reimport`). **AC4 pass.** `not_hevy.csv` (`name,value` / `x,1`) → "Nothing was changed", "Record 1: not a Hevy export"; History still exactly the two workouts. **AC5 pass.** Dark mode (`cmd uimode night yes`) on the Rejected screen renders correctly and the state survives the configuration change (`14-rejected-dark`, `15-history-after-rejected-dark`). **AC6 pass** (with the review's code check of strings and previews).
  - **Cleanup:** snapshot restored (`run-as … tar xf`), History again shows the earlier T041/Leg day workouts and the builder's Push, heavy / Legs import (`16-history-restored-bottom`). Night mode back to no.
  - **Result:** all acceptance criteria pass; moved to `done/`. No new tickets.
- **2026-10-31, REVIEW.** Diff `main...t-038-hevy-import-screen` read against the ticket, WORKFLOW §5, and D-059. Scope matches the ticket (Settings button, `Route.HevyImport` with encode/decode, stateless screen + ViewModel, all states, strings/plurals, light+dark preview per state, 48dp rows/buttons/dialog options). No new dependency (`libs.versions.toml` and Gradle files untouched), no schema or `:data` change, no live-workout change. `:domain` change is the additive `HevyPreview.Ready.newCustomCount` allowed by D-059; its two tests are new, no existing test or expected value edited. Recount uses the start minute like the planner (which has no intra-file dedup), so it agrees with `plan.newTemplates.size` for the defaults. File read is on IO (`BackupDocuments`), parse/apply on `@WorkDispatcher`.
  - **Blocker:** none.
  - **Should-fix (fixed, `db39604` "T-038: fix per-render custom type inference in the import preview"):** `rowResolution` re-ran `inferCustomExercise` by scanning every set in the file for each custom row on every `render()`, which runs on the main thread on each keystroke in the mapping search; with a years-long export that is O(titles × sets) per keystroke. Now the types are inferred once per file on `@WorkDispatcher` (sets grouped by title) and looked up while rendering. Same output; the existing ViewModel tests that assert the row types pass unchanged.
  - **Nit:** `read()` catches only `IOException`; a `SecurityException` (revoked document permission) or a `SQLiteException` while reading templates would crash the screen. Same pattern as `BackupViewModel`, so left as is.
  - **Nit:** `MINUTE_MS` is now declared privately in both `HevyImportUseCases.kt` and `HevyImportPlan.kt`.
  - **Commands:** `.\gradlew.bat :domain:test testDebugUnitTest assembleDebug lintDebug` → BUILD SUCCESSFUL (tests forced with `--rerun`: `HevyImportViewModelTest` 9/9, `HevyImportUseCasesTest` 15/15, all other suites green); after the fix `:domain:test :app:testDebugUnitTest assembleDebug lintDebug` → BUILD SUCCESSFUL, lint "0 errors, 1 warning" (the existing `NewerVersionAvailable` junit-bom only). Moved to `verify/`.
- **2026-10-31, UI.** Done: Settings → Backup has "Import from Hevy" under "Import from file", opening `Route.HevyImport(entry)` (encode/decode in `AppRoutes.kt`, fresh state per opening). New `ui/hevy/`: `HevyImportUiState` (Pick, Busy, Rejected with parser errors / measurements file / unreadable, Preview with mapping dialog, Done, Failed), `HevyImportViewModel` (preview/apply use cases from T-037, `SearchExercisesUseCase` for titles and dialog search, `BackupDocuments` for reading; parse and apply on the new `@WorkDispatcher` in `di/DispatcherModule.kt`), stateless `HevyImportScreen` with one light+dark `@Preview` pair per state (plus nothing-new and mapping-dialog variants). Strings and plurals in `strings.xml`; rows and buttons ≥ 48dp. `:domain`: additive `HevyPreview.Ready.newCustomCount(resolutions)` so the summary recounts after a mapping change, tests written first (2 new tests in `HevyImportUseCasesTest`; seen failing to compile, then passing). Decision **D-059** recorded (auto-accepted). ViewModel tests: `HevyImportViewModelTest` (9: pick, non-Hevy rejected, measurements + unreadable, default preview with unmatched first, mapping lowers count and import uses it, match changed to custom, dismiss keeps resolution, apply success then same file 0 new and disabled, write failure).
  - **Emulator (medium_phone, API 36, already running; fixture pushed to `/sdcard/Download/`):** non-Hevy CSV → "Nothing was changed", "Record 1: not a Hevy export" (History unchanged). Fixture → 2 in file, 2 new, 0 duplicates, 13 sets, 3 new customs, Mar 12–14 2026, 2 RPE values not stored; Lateral Raise (Dumbbell), Goblet Squat (Kettlebell), Assisted Pull Up listed first as unmatched. Mapped Assisted Pull Up → Machine Assisted Pull-Up via search: count 2. Import → "Imported 2 workouts, 2 new custom exercises, 0 duplicates skipped." History shows Push, heavy (8 set rows; the card says "7 sets" because History's summary leaves out the warm-up, existing behaviour) and Legs (5 sets, Machine Assisted Pull-Up used); Library shows the two new customs and no "Assisted Pull Up". Same file again → 0 new, 2 duplicates, "Import 0 workouts" disabled; the two customs now match exactly. Dark mode checked with `cmd uimode night yes` on the mapping dialog.
  - **Commands:** `.\gradlew.bat :domain:test testDebugUnitTest assembleDebug lintDebug` → BUILD SUCCESSFUL; lint SARIF has only the existing `NewerVersionAvailable` in `libs.versions.toml`, nothing new.
  - **Gotchas / left:** previews compile but were not opened in Android Studio. The emulator now holds the two imported workouts and two customs. Measurements file and body weight stay T-039; the personal `docs/fixtures/` files were not used (T-040).
