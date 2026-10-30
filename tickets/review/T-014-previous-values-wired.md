# T-014 — Previous values wired into the live workout

- **Role:** BUILD
- **Phase:** 1
- **Depends on:** T-005, T-013
- **Refs:** SPEC F-04 AC1,3 · TC-PREV-*

## Goal
The PREVIOUS column shows real data, and adding an exercise done before pre-fills sets (count/weight/reps) from last time.

## In scope
- Query last finished workout per exercise; map into UI state; prefill on add (editable, not completed).
- Tests: use case with fake history; one UI test showing previous values.

## Out of scope (do NOT do)
- Routine-scoped mode, settings UI for the mode.

## Acceptance criteria
- [x] Previous column matches TC-PREV-01/02/04.
- [x] Prefilled sets start uncompleted.

## Handoff log
- 2026-10-30 BUILD: Taken on the owner's instruction right after T-013, which is in `review/`, not `done/`. The branch `t-014-previous-values-wired` is stacked on `t-013-live-workout-screen`.
  - **Done:**
    - `:domain` `previousSets(history, mode, routineId)` returns the last session's sets by raw index. It follows D-019: warm-ups included, first occurrence, only finished and non-deleted workouts, and an uncompleted slot is null. `previousFor` now delegates to it, so the T-005 tests still guard the same rules.
    - `PreviousSetsUseCase(templateId)` feeds the PREVIOUS column.
    - `AddExercisesUseCase` pre-fills from the same lookup: same count, types, and values, all uncompleted with no `completedAt`. A slot without a previous value becomes an empty normal set. Without history it stays one empty normal set (D-029).
    - Mode is always "anywhere": routine-scoped stays T-024, and `previousFor` still has its `TODO(T-024)`.
    - `WorkoutSessionViewModel` reads each template's previous sets once per draft and drops the cache when the draft id changes, so the next workout never shows stale values. A previous value is formatted with the exercise's columns and the display unit.
    - In `SetRow`, an empty part of a previous value shows "–".
    - Replace is unchanged (one empty set per D-029); the ticket only asks for pre-fill on add.
  - **Tests:**
    - Domain, written first (failed to compile, then passed): `PreviousValuesTest` +3 for `previousSets`. `LiveWorkoutUseCasesTest` +3: pre-fill count/types/values uncompleted; drafts skipped and an uncompleted slot becomes an empty set; `PreviousSetsUseCase`. Existing tests unchanged; the domain `FakeWorkoutRepository.exerciseHistory` now returns a settable history instead of throwing.
    - App `WorkoutSessionViewModelTest` +4: TC-PREV-01/02 (60×10, 80×8, then none for the added third set), TC-PREV-04 (no history: none and one empty set), pre-fill in lb with warm-up first and nothing counted in the header, cache refreshed for a new draft.
    - `LiveWorkoutScreenTest` +1 UI test: previous "60 × 10" / "80 × 8" shown, set 2 unchecked, one tap completes it, header "640 kg".
  - **Manual (emulator):**
    - Marked the T-013 test draft (W 102.5×3, 100×5, both completed) as finished by editing the app's database (pulled with `run-as`, `end_time` set via Python sqlite3, pushed back), because the finish flow is T-016.
    - Start workout → add Barbell Back Squat. The rows came pre-filled W 102.5×3 and 1: 100×5, unchecked, with PREVIOUS showing the same.
    - Add set → its PREVIOUS shows "–". One tap on the check logged 100×5 (header 1 set / 500 kg). Force-stop and reopen: everything restored, previous values included.
  - **Commands:** `.\gradlew.bat :domain:test testDebugUnitTest assembleDebug lintDebug` all pass, 195 tests, 0 failures. Lint has only the existing `NewerVersionAvailable`.
  - **Left:** nothing in scope. On a real phone the PREVIOUS column only fills once T-016 can finish workouts.
