# T-013 — Live workout screen: sets, set types, completion

- **Role:** UI+BUILD
- **Phase:** 1
- **Depends on:** T-002, T-008, T-012
- **Refs:** SPEC F-02, F-03 · AC-01, AC-03 · docs/ARCHITECTURE.md §4, §6

## Goal
The real live-workout screen wired to the draft: stopwatch, add exercises via the picker, set rows with inputs by exercise type, set-type menu, swipe-delete, check to complete, header counters, keep-awake setting hook.

## In scope
- `WorkoutSessionViewModel` + `LiveWorkoutUiState`; reuse T-008 components; ViewModel tests with fakes; a Compose UI test for the happy path (add exercise, enter values, complete set).
- Resume banner/behavior when a draft exists at app start.
- Unit display kg/lb via T-002 conversions (global setting only; per-exercise toggle later).

## Out of scope (do NOT do)
- Previous values (T-014), rest timer (T-015), finish flow (T-016), supersets, notes, RPE.

## Acceptance criteria
- [x] Happy path UI test passes; kill/restart restores the draft (AC-03 steps).
- [x] Only completed sets count in the header volume/sets.
- [x] No extra taps beyond the design approved in T-008.

## Handoff log
- 2026-10-30 UI+BUILD: Taken on the owner's instruction while T-008 is still in `review/` (owner approved its look, see T-008 log).
  - **Done:**
    - `ui/workout/WorkoutSessionViewModel` over `observeActiveDraft()` with the T-012 use cases (DI in `UseCaseModule`). `LiveWorkoutUiState` is `Loading | NoWorkout | Active`.
    - Edits run one at a time behind a FIFO `Mutex`, because `EditSetValues` replaces all values and a late write must not overtake an earlier one.
    - Typed text is kept per set and column (`textEdits`), so a field never reformats under the cursor ("82." stays "82."). The stored value is parsed from the text on every keystroke and written at once. There is no debounce yet; Room writes were instant on the emulator.
    - `ui/workout/SetFields.kt` holds the columns per exercise type: weight/reps, reps, +kg/reps, −kg/reps, time, kg/time, km/time, kg/m. It also does kg↔lb at the edge (2 decimals, so 1.25 kg steps survive), takes a decimal comma, and accepts time as `m:ss` or keypad digits ("130" = 1:30, because the number keyboard has no colon).
    - Header volume and set count come from `:domain` `sessionVolumeKg` and the new `completedSetCount`. Both count completed sets only, with warm-ups left out per D-010.
    - T-008 components were generalized, not redesigned:
      - `WorkoutSetUi.previous/values` are lists that follow the exercise's `SetColumn`s.
      - `SetRow` has a set-type menu on the set-number cell (F-03 AC3).
      - `ExerciseCard` wraps rows in swipe-to-delete (end to start), with the same "Delete set" offered as an accessibility action.
      - A new `ReorderDialog` (up/down) opens from the existing menu's Reorder item.
      - The non-completed row background is now `surface` instead of transparent, so the swipe background does not show through.
    - No new control on the logging row and no left/right control (D-022).
    - Home has "Start workout", or a "Workout in progress · Resume" banner when a draft exists. A cold start with a draft opens the workout directly (0 taps for AC-03).
    - Keep-awake sets `View.keepScreenOn` while the workout screen shows and the setting is on.
    - The picker clears its selection when it closes, because its ViewModel is activity-scoped (`LibraryEvent.SelectionCleared`).
  - **Left / follow-ups:**
    - Stopwatch tap is a no-op: new ticket **T-026**.
    - Replace reuses the multi-select picker and takes the first pick: new ticket **T-027**.
    - Finish (T-016) and the rest bar (T-015) are placeholders. `CompleteSet`'s rest effect is ignored until T-015.
  - **Owner, please confirm:**
    - Warm-ups are left out of the header set count, the same as volume (D-010).
    - Distance shows km for distance+duration and m for weight+distance, also for lb users.
  - **Tests:** `:domain` `completedSetCount` (written first, failed to compile, then passed). `WorkoutSessionViewModelTest` (10: start, resume after restart, header counts, lb input stored as kg, decimal comma, columns by type, set-type menu and warm-up numbering, add/delete set, add/replace/reorder/remove, keep-awake). `SetFieldsTest` (5). `LiveWorkoutScreenTest` is the Robolectric happy path: picker → type 100/5 → check → header "500 kg" and the stored set is completed.
  - **Manual (emulator `medium_phone`):**
    - Ran AC-03: started a workout and added squat, assisted pull-up, and plank (the columns follow the type). Logged 100×5 and 102.5×3, header 2 sets / 808 kg. `am force-stop`, then reopen: the app went straight to the workout with the same values, completed state, and header, and the stopwatch kept counting from the original start.
    - Set-type menu → Warm-up: header dropped to 1 set. Swipe deleted a set.
  - **Commands:** `.\gradlew.bat :domain:test testDebugUnitTest assembleDebug lintDebug` all pass, 184 tests, 0 failures. Lint has only the existing `NewerVersionAvailable`.
  - **Gotchas:**
    - `adb exec-out screencap` gives a black image on this emulator; `adb emu screenrecord screenshot <file>` works.
    - In the Robolectric picker the list is lazy, so search before clicking an item.
