# T-026 — Edit the live workout's start time from the stopwatch

- **Role:** BUILD
- **Phase:** 1
- **Depends on:** T-013
- **Refs:** SPEC F-02 AC1
- **Size:** fits one session

## Goal
Tapping the stopwatch on the live-workout screen lets the user change the start time (and so the duration) of the running draft.

## In scope
- A `:domain` use case that changes `startedAtEpochMs` of the draft (not in the future), with tests.
- A time picker dialog opened from the existing stopwatch tap target in `WorkoutHeader` (T-013 leaves `onDurationClick` empty).

## Out of scope (do NOT do)
- Finish screen date/time (T-016), logging past workouts.

## Acceptance criteria
- [x] Tapping the stopwatch opens the picker; the new start time persists and the stopwatch follows it.
- [x] A start time in the future is rejected.

## Verification
`:domain:test`, `testDebugUnitTest`, manual check on the emulator.

## Notes / decisions needed
Found during T-013: F-02 AC1 asks for it, but no ticket covered the live edit.

## Handoff log
- 2026-10-30 BUILD (branch `t-026-edit-live-start-time`):
  - **`:domain`:** `ChangeStartTimeUseCase(repository, clock)` in `LiveWorkoutUseCases.kt` returns `ChangeStartTimeResult.Changed(workout)` or `InFuture`, or null for an unknown, finished, or deleted workout. A start after `clock.nowEpochMs()` writes nothing; exactly now is allowed. `utcOffsetS` is taken again at the new start, and `updatedAt` is bumped only when something changed. Sets and the running rest are left alone. Tests written first in `ChangeStartTimeUseCaseTest` (7 tests; they failed to compile before the use case existed).
  - **`:app`:** provided in `UseCaseModule`. `WorkoutSessionViewModel` has new events `DurationClicked`, `StartPicked`, `StartPickerDismissed`, `StartInFutureDismissed`. `LiveWorkoutUiState.Active` gained `startedAtEpochMs`, `startPickerOpen`, `startInFutureShown`. The stopwatch (the existing tap target, no new control) opens the existing `DateTimePickerDialogs` at the stored start. A rejected time shows `StartInFutureDialog` (2 new strings, light and dark preview). In past-workout edit mode the tap does nothing (D-033). The `TODO(T-026)` is gone. 4 new ViewModel tests.
  - **Decisions:** D-048 (date then time picker, future rejected with a dialog, offset recomputed, rest timer and sets untouched, no-op in edit mode). Auto-accepted recommendation, owner-delegated.
  - **Commands:** `.\gradlew.bat :domain:test testDebugUnitTest` BUILD SUCCESSFUL, 291 tests, 0 failures. `.\gradlew.bat assembleDebug lintDebug` BUILD SUCCESSFUL. Lint: only the existing junit-bom `NewerVersionAvailable`.
  - **Manual check** (emulator `emulator-5554`, medium_phone API 36, debug build of this branch): started a workout; tapping the stopwatch opened the date picker at today; picked Oct 30 and 11:01 PM at 00:03 on Oct 31; the stopwatch jumped to 1:02:26 and kept running. After `am force-stop` and relaunch, the draft came back with the new start (1:03:02 at 00:04:06). Picking today 11:01 PM showed "Start time is in the future", and the start stayed unchanged. Cancel leaves it unchanged. The picker reopens at the stored start date and looks right in dark mode.
  - **Gotchas:** the picker has minute precision, so confirming without changing anything drops the seconds of the start. The test draft is still open on the emulator.
  - **Not done:** `connectedDebugAndroidTest` (no instrumented test for this).
- 2026-10-31 REVIEW (diff `main...t-026-edit-live-start-time` at `7f573e5`):
  - **Checklist:** scope matches the ticket (use case, DI, ViewModel events, picker and dialog; no Finish/Log past changes). No existing test edited; 7 new domain tests and 4 ViewModel tests. Logic (future check, offset, draft-only) lives in `:domain`; the screen only forwards events. No new tap or control on the logging screen: the stopwatch was already the tap target (`cd_edit_duration`) and the picker is the existing `DateTimePickerDialogs`. New strings in `strings.xml`, light and dark previews for `StartInFutureDialog`, dialog buttons are Material `TextButton`s (48dp). No dependency, schema or public API change beyond the new use case. D-048 is followed (future rejected with a dialog, exactly now allowed, offset recomputed, rest timer untouched, no-op in edit mode).
  - **Blocker:** none.
  - **Should-fix (fixed, `bb83894`):** confirming the picker without changing anything set the start to the whole minute, so the duration silently grew by up to 59 s and `updatedAt` was bumped. `ChangeStartTimeUseCase` now keeps the stored start when the picked time is in the same minute (checked after the future check, so the existing `now + 1 ms` rejection test keeps its expectation). New test `a start in the same minute keeps the stored seconds`, seen failing first.
  - **Nit:** none worth a change.
  - **Commands:** `.\gradlew.bat :domain:test testDebugUnitTest assembleDebug lintDebug` BUILD SUCCESSFUL, 292 tests, 0 failures; lint 0 errors, 1 warning (the existing junit-bom `NewerVersionAvailable`).
  - **Result:** to `verify/`.
