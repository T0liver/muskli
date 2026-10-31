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
- [x] Replace opens the picker; one tap on an exercise replaces and returns to the workout.

## Verification
Compose UI test on the picker; manual check on the emulator.

## Notes / decisions needed
Found during T-013: Replace reuses the multi-select picker and takes the first selected exercise.

## Handoff log
- 2026-10-31 (UI): Finished the work an aborted session left uncommitted, after reviewing it; nothing was thrown away.
  - **Done:** `LibraryMode.REPLACE`. Tapping a row calls `onPicked` with that one id; there are no checkboxes, no "Add N" button, and a "Replace exercise" title. `MainActivity` passes the template id of the exercise being replaced as `currentId`. That row shows "Current" and cannot be tapped. A custom exercise created from the picker is returned at once: the ViewModel already adds it to the selection, and REPLACE never selects on tap. Multi-select Add (`PICK`) is unchanged. Light and dark `@Preview` for REPLACE. Strings `library_replace_title`, `library_current`. Rules recorded as **D-049** (auto-accepted).
  - **Tests:** Robolectric Compose tests in `app/src/test/.../LibraryScreenTest.kt`, which is the project's Compose test setup; there is no `androidTest` source set. They cover one tap returning one id with no Add button, the current row being disabled and doing nothing on tap, and a new custom exercise being returned.
  - **Commands:** `.\gradlew.bat :domain:test testDebugUnitTest assembleDebug lintDebug` passed. `LibraryScreenTest` ran 4 of 4 green. Lint has 1 warning, the existing `NewerVersionAvailable` for JUnit 6, which D-016 declines; nothing new.
  - **Manual (emulator-5554, medium_phone, API 36):** In a draft, the Barbell Back Squat menu, then Replace, opens "Replace exercise" with the squat marked "Current". One tap on Barbell Deadlift replaced it and returned to the workout. Tapping the "Current" row did nothing, and Back went to the workout in one step with its sets kept. Add exercise still shows checkboxes and "Add 1". Creating a custom exercise from Replace was checked by the JVM test only, not on the emulator.
  - **Gotchas:** The Replace row tap returns immediately, so `onPicked` must not depend on a selection. The `LaunchedEffect` in `LibraryScreen` only reacts to a selection appearing, which in REPLACE can only come from a newly created exercise.
