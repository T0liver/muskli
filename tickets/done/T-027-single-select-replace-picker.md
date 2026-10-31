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
- 2026-10-31 (REVIEW): Reviewed `git diff main...t-027-single-select-replace-picker` (commit `65b158b`) against the ticket, SPEC F-02 AC2, ARCHITECTURE, AGENTS and WORKFLOW §5. **Approved, no Blockers, no Should-fix, so no code changes.**
  - The diff matches the ticket and D-049. PICK (multi-select Add) has the same click, checkbox, colour and bottom-bar logic as before. Only the existing test file gained 3 new tests, and no expected values changed. The REPLACE logic is limited to UI routing (tap returns the id, current row disabled), and the selection rule stays in `LibraryViewModel`. Nothing on the logging screen changed: Replace is still one tap in the existing exercise menu. Both new strings are in `strings.xml`, light and dark previews exist, rows keep `minTouchTarget`, and no dependency or schema change was made. The `TODO(T-027)` was removed.
  - **Nit:** The "Current" row is disabled but not dimmed, so the only cue is the "Current" label. That is fine for one user.
  - **Nit:** `replaceReturnsTheOneTappedExercise` checks that "Add 0" is absent, which proves there is no Add button but not that there are no checkboxes. The manual check covers that.
  - **Nit:** `MainActivity` still does `ids.firstOrNull()`. That is harmless, because REPLACE always returns exactly one id.
  - **Commands:** `$env:JAVA_TOOL_OPTIONS='-Djavax.net.ssl.trustStoreType=Windows-ROOT'; .\gradlew.bat :domain:test testDebugUnitTest assembleDebug lintDebug` passed. I then reran the tests with `:domain:test --rerun :data:testDebugUnitTest --rerun :app:testDebugUnitTest --rerun`: 295 tests, 0 failures, and `LibraryScreenTest` passed 4 of 4. Lint showed 0 errors and 1 warning, the existing `NewerVersionAvailable` for JUnit (D-016).
- 2026-10-31 (VERIFY): Debug build of commit `65b158b`, installed with `adb install -r` on emulator-5554 (AVD medium_phone, Android 16 / API 36). Driven with adb and uiautomator. Screenshots are in `build/verify/T-027/` (git-ignored). **All steps passed.**
  1. Draft workout, Barbell Back Squat menu, Replace: the screen is titled "Replace exercise", with no checkboxes and no Add button. The squat row is labelled "Current", and uiautomator reports it `enabled=false`. Tapping it did nothing (`02-replace-picker`, `03-current-tap-nothing`). **Pass.**
  2. **Builder's unverified case.** From the Replace picker, "+" opened "New exercise". I filled in "Verify Cable Fly", Shoulders and Machine, then tapped Save. The app returned straight to the workout, with Verify Cable Fly in the squat's place (`04-create-form`, `05-form-filled`, `06-after-save`). **Pass.**
  3. Replace on Verify Cable Fly showed it as "Current". One tap on Barbell Deadlift replaced it and returned to the workout (`07-replace-custom-current`, `08-after-tap-deadlift`). **Pass.**
  4. Opening Replace and pressing Back returned to the workout in one step, with the exercise unchanged. **Pass.**
  5. Add exercise still shows "Add exercises" with checkboxes. Tapping two rows changed the button to "Add 2" (`09-add-multiselect`). **Pass.**
  6. Stale selection check: I pressed Back from Add with 2 selected, then opened Replace. The picker stayed open and nothing was picked automatically. "Current" is shown in both Recent and All (`10-replace-after-add-back`). One tap on Barbell Back Squat put the squat back (`11-restored-squat`). **Pass.**
  7. In dark mode, the Replace picker is readable and "Current" is legible (`12-replace-dark`). Night mode was then reset. **Pass.**
  - Side effect: the custom exercise "Verify Cable Fly" is left in the emulator's library. This is only test data.
