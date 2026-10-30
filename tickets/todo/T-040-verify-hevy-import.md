# T-040 — VERIFY: Hevy import with the owner's real export (AC-07)

- **Role:** VERIFY
- **Phase:** 1
- **Depends on:** T-038, T-039
- **Refs:** SPEC F-09 · AC-07 · `docs/HEVY-IMPORT.md` §7 (risks) · D-041, D-043, D-044, D-045
- **Size:** fits one session

## Goal
Evidence that the owner's real Hevy export imports correctly: counts match, unmapped exercises are listed, re-import adds nothing, and the body weight option works.

## In scope
- Debug build on the owner's phone, or the emulator if the phone is not reachable (say which).
- Push `docs/fixtures/workout_data.csv` and `measurement_data.csv` (git-ignored; never commit them or paste their content beyond counts) to `/sdcard/Download/`.
- Run the steps below and record results per step.

## Out of scope (do NOT do)
- Code changes. Failures become new tickets in `todo/`.

## Acceptance criteria
- [ ] Preview counts equal a count of the CSV: distinct (title, start, end) workouts and data rows (sets). Record the numbers and how they were counted.
- [ ] Every distinct `exercise_title` appears in the preview list; titles with no library match are listed as unmatched; none is missing.
- [ ] After import, each workout in History has the CSV's start time (device zone), duration, exercises, and sets; exercise order matches the CSV row order. The owner is asked to compare one workout's exercise order against Hevy (HEVY-IMPORT §7 risk).
- [ ] Importing the same file again: 0 new, all duplicates, nothing added.
- [ ] Body weight: with Settings body weight unset, the checkbox is checked and the latest weight from the measurements file is set after import.
- [ ] Time from "Import" to Done is recorded.
- [ ] No personal values from the files are committed (only counts and pass/fail).

## Verification
`.\gradlew.bat assembleDebug`, `adb install -r app/build/outputs/apk/debug/app-debug.apk`, `adb push docs/fixtures/workout_data.csv /sdcard/Download/`, `adb push docs/fixtures/measurement_data.csv /sdcard/Download/`, then the steps above. Count rows with PowerShell: `(Import-Csv docs/fixtures/workout_data.csv).Count` and `(Import-Csv docs/fixtures/workout_data.csv | Select-Object title,start_time,end_time -Unique).Count`.

## Notes / decisions needed
None open.

## Handoff log
<!-- Newest first. Each entry: date, role, what was done, what's left, gotchas, commands run + results. -->
