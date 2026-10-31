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
- [ ] Picking the T-035 fixture (pushed to the emulator) shows 2 new workouts, 13 sets, 2 RPE values not stored, and 3 unmatched exercises listed first.
- [ ] Mapping one unmatched exercise to a library exercise lowers the new custom count to 2 and the import uses that exercise.
- [ ] Confirming imports; History shows both workouts with their sets; the Library shows the new custom exercises.
- [ ] Picking the same file again shows 0 new and 2 duplicates, and the import button is disabled.
- [ ] A non-Hevy file shows the Rejected state and changes nothing.
- [ ] Previews exist for every state; dark mode works; no hardcoded strings.
- [ ] `./gradlew :domain:test testDebugUnitTest assembleDebug lintDebug` pass with no new lint warnings.

## Verification
`.\gradlew.bat :domain:test testDebugUnitTest assembleDebug lintDebug`; on an emulator: `adb push domain/src/test/resources/hevy/workout_data.csv /sdcard/Download/`, then run the acceptance criteria by hand and note results in the Handoff log.

## Notes / decisions needed
None open: D-040, D-043, D-044. The screen is outside the live-workout flow, so the "no extra taps" rule does not apply.

## Handoff log
<!-- Newest first. Each entry: date, role, what was done, what's left, gotchas, commands run + results. -->
