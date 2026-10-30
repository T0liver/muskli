# T-027 — Single-select picker for "Replace exercise"

- **Role:** UI
- **Phase:** 1
- **Depends on:** T-013
- **Refs:** SPEC F-02 AC2
- **Size:** fits one session

## Goal
Replacing an exercise picks exactly one exercise, so the user cannot select several and wonder which one is used.

## In scope
- A single-select mode of the library picker (tap picks and returns), used by the exercise menu's Replace.

## Out of scope (do NOT do)
- Changes to multi-select "Add exercise".

## Acceptance criteria
- [ ] Replace opens the picker; one tap on an exercise replaces and returns to the workout.

## Verification
Compose UI test on the picker; manual check on the emulator.

## Notes / decisions needed
Found during T-013: Replace reuses the multi-select picker and takes the first selected exercise.

## Handoff log
