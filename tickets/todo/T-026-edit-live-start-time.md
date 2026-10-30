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
- [ ] Tapping the stopwatch opens the picker; the new start time persists and the stopwatch follows it.
- [ ] A start time in the future is rejected.

## Verification
`:domain:test`, `testDebugUnitTest`, manual check on the emulator.

## Notes / decisions needed
Found during T-013: F-02 AC1 asks for it, but no ticket covered the live edit.

## Handoff log
