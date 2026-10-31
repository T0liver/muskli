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
- **2026-10-31, UI.** Done: Settings → Backup has "Import from Hevy" under "Import from file", opening `Route.HevyImport(entry)` (encode/decode in `AppRoutes.kt`, fresh state per opening). New `ui/hevy/`: `HevyImportUiState` (Pick, Busy, Rejected with parser errors / measurements file / unreadable, Preview with mapping dialog, Done, Failed), `HevyImportViewModel` (preview/apply use cases from T-037, `SearchExercisesUseCase` for titles and dialog search, `BackupDocuments` for reading; parse and apply on the new `@WorkDispatcher` in `di/DispatcherModule.kt`), stateless `HevyImportScreen` with one light+dark `@Preview` pair per state (plus nothing-new and mapping-dialog variants). Strings and plurals in `strings.xml`; rows and buttons ≥ 48dp. `:domain`: additive `HevyPreview.Ready.newCustomCount(resolutions)` so the summary recounts after a mapping change, tests written first (2 new tests in `HevyImportUseCasesTest`; seen failing to compile, then passing). Decision **D-059** recorded (auto-accepted). ViewModel tests: `HevyImportViewModelTest` (9: pick, non-Hevy rejected, measurements + unreadable, default preview with unmatched first, mapping lowers count and import uses it, match changed to custom, dismiss keeps resolution, apply success then same file 0 new and disabled, write failure).
  - **Emulator (medium_phone, API 36, already running; fixture pushed to `/sdcard/Download/`):** non-Hevy CSV → "Nothing was changed", "Record 1: not a Hevy export" (History unchanged). Fixture → 2 in file, 2 new, 0 duplicates, 13 sets, 3 new customs, Mar 12–14 2026, 2 RPE values not stored; Lateral Raise (Dumbbell), Goblet Squat (Kettlebell), Assisted Pull Up listed first as unmatched. Mapped Assisted Pull Up → Machine Assisted Pull-Up via search: count 2. Import → "Imported 2 workouts, 2 new custom exercises, 0 duplicates skipped." History shows Push, heavy (8 set rows; the card says "7 sets" because History's summary leaves out the warm-up, existing behaviour) and Legs (5 sets, Machine Assisted Pull-Up used); Library shows the two new customs and no "Assisted Pull Up". Same file again → 0 new, 2 duplicates, "Import 0 workouts" disabled; the two customs now match exactly. Dark mode checked with `cmd uimode night yes` on the mapping dialog.
  - **Commands:** `.\gradlew.bat :domain:test testDebugUnitTest assembleDebug lintDebug` → BUILD SUCCESSFUL; lint SARIF has only the existing `NewerVersionAvailable` in `libs.versions.toml`, nothing new.
  - **Gotchas / left:** previews compile but were not opened in Android Studio. The emulator now holds the two imported workouts and two customs. Measurements file and body weight stay T-039; the personal `docs/fixtures/` files were not used (T-040).
