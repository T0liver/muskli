# T-030 — Adding a set copies the row above

- **Role:** BUILD
- **Phase:** 1
- **Depends on:** T-014
- **Refs:** AC-01 · SPEC §8
- **Size:** fits one session

## Goal
"Add set" starts from the weight, reps, and other values of the set above it, so a repeated set is one tap to complete.

## In scope
- `AddSetUseCase` copies the previous set's value fields (not its completion) into the new normal set. A first set stays empty.
- Unit tests for copy, for the first set, and for not marking the copy completed.

## Out of scope (do NOT do)
- Changing how last-time prefills work when an exercise is added (that already copies the whole previous session).
- Drop-set or warm-up shortcuts.

## Acceptance criteria
- [x] On an exercise whose latest set is 100 kg × 5, Add set shows 100 and 5, uncompleted.
- [x] The new set is normal, and completing it is the check tap only.
- [x] An exercise with no sets still gets one empty normal set.

## Verification
`:domain:test` for the new cases. On the emulator, add a second set and confirm the fields match the row above without typing.

## Notes / decisions needed
T-019 seed workout (2026-10-30): every Add set was an empty row, so each extra set needed the weight and the reps typed again. Hevy copies the row above. Last-time prefill on Add exercise is separate and did work (AC-01).

## Handoff log
<!-- Newest first. Each entry: date, role, what was done, what's left, gotchas, commands run + results. -->
- 2026-10-31 BUILD: Branch `t-030-add-set-copies-previous-row` from `main` (`d5f7327`). Tests came first: four new cases in `LiveWorkoutUseCasesTest`. They copy 100 kg × 5 uncompleted from a completed set, copy every value field plus the side, add a normal set below a warm-up, drop, or failure row, and add one empty normal set to an exercise with no sets. The three copy cases failed before the change. The empty case already passed, since the old behaviour was always an empty set. `AddSetUseCase` now copies the exercise's last set: `weightKg`, `reps`, `durationS`, `distanceM`, `side`. The new set is `NORMAL`, `completed = false`, and has no completion time. The now-unused private `emptySet` helper was removed. The new decision is **D-050**, auto-accepted recommendation: there is no RPE field to copy, and a typed row above becomes a normal copy. `AddExercisesUseCase`, last-time prefill, the UI, and the schema are unchanged. No existing test expectation changed.
  - Commands: `.\gradlew.bat :domain:test` failed with 3 of 26 `LiveWorkoutUseCasesTest` cases before the change and passed after. `.\gradlew.bat :domain:test testDebugUnitTest assembleDebug lintDebug` reported BUILD SUCCESSFUL. Lint shows one warning, `NewerVersionAvailable`, which was already there and is a dependency notice; this change touches no `:app` code.
  - Emulator (`emulator-5554`, medium_phone API 36, already running): installed the debug APK and opened the existing draft. I typed 100 / 5 into Back Squat set 8 and tapped Add set. Set 9 appeared as normal with 100 and 5, uncompleted, without typing. One check tap completed it: Sets 1, Volume 500 kg, rest timer started. The first-set case was checked by unit test only, not on the device.
  - Gotcha: that emulator draft now has 9 squat sets with set 8 at 100 × 5 and set 9 completed.
