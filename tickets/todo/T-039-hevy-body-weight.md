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
- [ ] With body weight unset, adding the T-035 measurement fixture shows "Set body weight to 80.4 kg (from 1 Mar 2026)" checked; after import, Settings shows 80.4 kg.
- [ ] With body weight already set, the checkbox starts unchecked and importing leaves it unchanged.
- [ ] With the lb setting, the label shows the converted value and the stored value is still kg.
- [ ] `./gradlew :domain:test testDebugUnitTest assembleDebug lintDebug` pass with no new lint warnings.

## Verification
`.\gradlew.bat :domain:test testDebugUnitTest assembleDebug lintDebug`; on an emulator with both fixtures pushed to `/sdcard/Download/`, run the acceptance criteria by hand.

## Notes / decisions needed
None open: D-045.

## Handoff log
<!-- Newest first. Each entry: date, role, what was done, what's left, gotchas, commands run + results. -->
