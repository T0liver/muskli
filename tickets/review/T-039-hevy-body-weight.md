# T-039 — Hevy import: body weight from the measurements file

- **Role:** UI
- **Phase:** 1
- **Depends on:** T-038
- **Refs:** SPEC F-07, F-09 · `docs/HEVY-IMPORT.md` §6 · D-017, D-045 · TC-HEVY-09, TC-HEVY-22
- **Size:** fits one session

## Goal
In the Hevy import preview, the owner can add the Hevy measurements file and, with one checkbox, set the Settings body weight to the latest weight in it when the workouts are imported.

## In scope
- Preview state of `HevyImportScreen`: an "Add measurements file (optional)" button (SAF `OpenDocument`, `*/*`).
- After a measurement file is read: a checkbox "Set body weight to X kg (from <date>)", weight shown in the user's unit (kg/lb setting; stored in kg), **checked by default only when Settings body weight is unset** (D-045). A measurement file with no weight shows "No body weight in this file". A workout file picked here shows the parser's "not a measurements file" message.
- Confirm passes the weight to `ApplyHevyImportUseCase(bodyWeightKg = …)` only when the checkbox is checked.
- Done state mentions "Body weight set to X" when it was written.
- Strings in `strings.xml`, previews (with and without the checkbox, light and dark), ViewModel tests: default checked when unset, unchecked when set, weight passed only when checked.

## Out of scope (do NOT do)
- Measurement history, other measurement columns, any body-measurement screen (Phase 2).
- Importing a measurements file without a workout file.

## Acceptance criteria
- [x] With body weight unset, adding the T-035 measurement fixture shows "Set body weight to 80.4 kg (from 1 Mar 2026)" checked; after import, Settings shows 80.4 kg.
- [x] With body weight already set, the checkbox starts unchecked and importing leaves it unchanged.
- [x] With the lb setting, the label shows the converted value and the stored value is still kg.
- [x] `./gradlew :domain:test testDebugUnitTest assembleDebug lintDebug` pass with no new lint warnings.

## Verification
`.\gradlew.bat :domain:test testDebugUnitTest assembleDebug lintDebug`; on an emulator with both fixtures pushed to `/sdcard/Download/`, run the acceptance criteria by hand.

## Notes / decisions needed
None open: D-045.

## Handoff log
<!-- Newest first. Each entry: date, role, what was done, what's left, gotchas, commands run + results. -->

### 2026-10-31 · UI · done, ready for review
**Done.** No domain change: the parser (T-035) already returns the latest body weight and `ApplyHevyImportUseCase(bodyWeightKg)` (T-037) already writes it after the commit (TC-HEVY-09/22 domain tests exist). This ticket is ViewModel + screen only.
- `HevyImportViewModel` injects `SettingsRepository`. New events `MeasurementsFilePicked(uri)` and `BodyWeightToggled(checked)`. The file goes through `PreviewHevyImportUseCase`; `Measurements` with a weight becomes `HevyMeasurements.BodyWeight(weight, measuredAt, checked = Settings body weight == null)`; without a weight `NoBodyWeight`; a workout or rejected file `NotMeasurementsFile`; an `IOException` `ReadFailed`. The workout preview is kept in every case. Import passes the **unrounded kg from the file** only when checked; `Done.bodyWeight` carries the displayed weight when written.
- `HevyImportUiState`: `Preview.measurements`, `Done.bodyWeight` (both default null, so existing expectations are unchanged), `HevyMeasurements`, `HevyWeight`.
- `HevyImportScreen`: second SAF `OpenDocument("*/*")` launcher; a "Body weight" section under the summary with the checkbox row (whole row toggleable, 48dp) or message, and an outlined "Add measurements file (optional)" button; Done appends "Body weight set to X unit." Previews: body weight offered, no body weight in file, done with body weight (light + dark each); the existing preview is the "without the checkbox" case.
- Reuse, no copies: Settings' `formatWeight` moved from the private companion to an `internal` top-level function, and `WeightUnit.labelRes()` became `internal`, so the label matches what Settings shows (177.3 lb).
- 6 strings in `strings.xml`. D-060 records the UI choices (auto-accepted recommendation).
- ViewModel tests (7 new, 16 total in the class): default checked when unset (and Settings gets 80.4), unchecked when set (unchanged after import), lb label "177.3" with 80.4 kg stored, unchecking passes no weight, ticking with a weight set overwrites it, wrong/no-weight/unreadable file keeps the preview and passes no weight, a later file replaces the offer. The only edit to existing test code is passing `settings` to the new constructor parameter.

**Commands and results.**
- `.\gradlew.bat :domain:test testDebugUnitTest assembleDebug lintDebug` → BUILD SUCCESSFUL. Lint SARIF has 1 result, the pre-existing `NewerVersionAvailable` in `libs.versions.toml`; nothing new.
- Emulator `medium_phone`, Android 16, 1080×2400, debug build of this branch, anonymised fixtures `domain/src/test/resources/hevy/{workout,measurement}_data.csv` pushed to `/sdcard/Download/`; app data cleared (`pm clear`) before each scenario so the workouts are new:
  - AC1 (unset, kg): a workout file in the measurements picker shows "This is not a Hevy measurements file."; the measurement fixture shows "Set body weight to 80.4 kg (from Mar 1, 2026)" **checked**; Done reads "... Body weight set to 80.4 kg."; Settings shows 80.4. **Pass.**
  - AC2 (75 kg set): checkbox **unchecked**; Done has no body weight line; Settings still 75. **Pass.**
  - AC3 (lb, dark mode): label "Set body weight to 177.3 lb (from Mar 1, 2026)" checked; Done "Body weight set to 177.3 lb."; Settings shows 177.3 lb; DataStore `body_weight_kg` read with `run-as` is the double `0x405419999999999A` = **80.4** exactly. **Pass.**

**Gotchas / not verified.** The date follows the device locale (medium date), so en-US shows "Mar 1, 2026" where the AC text says "1 Mar 2026"; same formatter as the preview's date range (D-060). "No body weight in this file" was checked by the ViewModel test only, not on the emulator (no such fixture). The Compose previews compile but were not rendered (no Studio in this session); the light and dark layouts were seen on the emulator instead.

**Left.** Nothing in scope. T-040 verifies the whole Hevy import on the owner's real files.
