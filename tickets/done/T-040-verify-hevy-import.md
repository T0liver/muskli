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
- [x] Preview counts equal a count of the CSV: distinct (title, start, end) workouts and data rows (sets). Record the numbers and how they were counted.
- [x] Every distinct `exercise_title` appears in the preview list; titles with no library match are listed as unmatched; none is missing.
- [x] After import, each workout in History has the CSV's start time (device zone), duration, exercises, and sets; exercise order matches the CSV row order. The owner is asked to compare one workout's exercise order against Hevy (HEVY-IMPORT §7 risk).
- [x] Importing the same file again: 0 new, all duplicates, nothing added.
- [x] Body weight: with Settings body weight unset, the checkbox is checked and the latest weight from the measurements file is set after import.
- [x] Time from "Import" to Done is recorded.
- [x] No personal values from the files are committed (only counts and pass/fail).

## Verification
`.\gradlew.bat assembleDebug`, `adb install -r app/build/outputs/apk/debug/app-debug.apk`, `adb push docs/fixtures/workout_data.csv /sdcard/Download/`, `adb push docs/fixtures/measurement_data.csv /sdcard/Download/`, then the steps above. Count rows with PowerShell: `(Import-Csv docs/fixtures/workout_data.csv).Count` and `(Import-Csv docs/fixtures/workout_data.csv | Select-Object title,start_time,end_time -Unique).Count`.

## Notes / decisions needed
None open.

## Handoff log
<!-- Newest first. Each entry: date, role, what was done, what's left, gotchas, commands run + results. -->

### 2026-10-31 · VERIFY · PASS (owner check of exercise order pending)
- **Build/device:** `main` at `4b5cf5f` (T-039 merge), debug APK. **Emulator**, not the phone: AVD `medium_phone`, Android 16 (API 36), zone Europe/Budapest. `pm clear hu.toliver.muskli` first, so this was the first import and body weight was unset.
- **CSV count (PowerShell):** `(Import-Csv workout_data.csv).Count` = **18** rows; `... | Select-Object title,start_time,end_time -Unique` = **3** workouts; `... | Select-Object exercise_title -Unique` = **7** titles. Measurements file: 1 data row with a weight.
- **1. Preview counts: PASS.** 3 workouts in the file, 3 new, 0 duplicates, 18 sets, 7 new custom exercises, date range matches the CSV.
- **2. Exercise list: PASS.** All 7 distinct titles listed, all "Not found in the library" with a new custom exercise. Correct per D-043: the bundled catalogue has 10 exercises and none matches exactly or by token multiset. None missing.
- **3. History: PASS.** 3 workouts; start times equal the CSV's local times in the device zone; durations 1 min, 35 min, 1 min equal end − start; set counts and exercises per workout match the CSV. Detail of the 5-exercise workout ("Tuesday exercise"): exercises in CSV row order, 3 sets each, reps/weights match the rows, volume consistent.
- **4. Re-import (AC-07): PASS.** Same file again: 3 in file, 0 new, 3 duplicates, 0 sets, 0 new custom exercises; all 7 titles now resolve to the custom exercises created by the first import; the import button reads "Import 0 workouts" and is disabled. History still has 3 workouts; the library has 17 exercises (10 catalogue + 7 custom), no duplicates.
- **5. Body weight: PASS.** After "Add measurements file (optional)" the body weight checkbox showed the latest weight and its date and was **checked** by default; after import the Done message confirmed it and Settings → Body weight shows that value.
- **6. Time: PASS.** Tap "Import" to Done screen ≤ 2.8 s (upper bound; includes one uiautomator dump of ~2 s used for polling).
- **7. Personal data: PASS.** Fixtures are git-ignored (`.gitignore:16`); only counts and pass/fail recorded here.
- **Exercise order (HEVY-IMPORT §7):** the import keeps CSV row order as designed. However, in the CSV **every multi-exercise workout is in alphabetical order** (both the 2-exercise and the 5-exercise workout), which strengthens the spike's suspicion that Hevy's export sorts exercises by name. Not a Muskli bug (the file carries no other order). **Owner:** please compare "Tuesday exercise" in Hevy with Muskli; if Hevy's order differs, decide whether to accept it or open a ticket (e.g. note the limitation in the import screen).
- **Commands:** `.\gradlew.bat assembleDebug` (BUILD SUCCESSFUL), `.\gradlew.bat :domain:test` (BUILD SUCCESSFUL), `adb install -r` (Success), `adb shell pm clear` (Success), `adb push` both CSVs to `/sdcard/Download/`, UI driven with `adb shell input` + `uiautomator dump` + `screencap`.
- **Bugs filed:** none.
