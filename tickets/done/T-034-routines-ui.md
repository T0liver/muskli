# T-034 — Routines UI: list, editor, start

- **Role:** UI
- **Phase:** 2
- **Depends on:** T-033, T-011
- **Refs:** SPEC §6 (Routines) · docs/ARCHITECTURE.md §6
- **Size:** fits one session

## Goal
The user can see their routines, create and edit one (add exercises from the library, set planned sets and rest), delete one, start a workout from it, and save a finished workout as a routine.

## In scope
- Routines list reachable from Home, with Start on each routine and an empty state.
- Routine editor screen reusing the library picker; planned set rows with weight/reps.
- "Save as routine" from a finished workout's detail screen.
- Previews (light/dark), strings in `strings.xml`, 48dp targets, ViewModel tests.

## Out of scope (do NOT do)
- Folders, reordering by drag (a move up/down menu is fine), any change to the live-workout logging screen.

## Acceptance criteria
- [x] Create a routine with two exercises, restart the app, start it: the live workout shows those exercises and sets.
- [x] Edit and delete work; Home's Start workout (empty) still works.

## Verification
`testDebugUnitTest`, `assembleDebug`, manual check on the emulator.

## Notes / decisions needed
Created 2026-10-30 together with T-033.

## Handoff log
<!-- Newest first. Each entry: date, role, what was done, what's left, gotchas, commands run + results. -->
- 2026-10-31 VERIFY (build: debug APK of `6ff13eb` (code), branch head `2f4eaaa` (ticket move only); device: emulator `medium_phone` `emulator-5554`, `sdk_gphone64_x86_64`, Android 16 / API 36; driven with `adb` + `uiautomator dump`; screenshots `build/verify/01..16-*.png`, git-ignored):
  - **Create with two exercises: PASS.** Routines → **+** → name "Verify A", Add exercise → picked Barbell Deadlift + Plank (each arrived with one empty normal set). Deadlift 120 × 5, **Add set** copied it (2 × 120 × 5), rest 150; Plank 45 s. Save → listed as "Barbell Deadlift, Plank" (`03`, `04`, `05`).
  - **Restart, then start: PASS.** `am force-stop` + relaunch; Home showed Start workout (no draft), the routine was still listed. **Start Verify A** → live workout with Deadlift sets 1–2 at 120 kg × 5 and Plank 0:45, all uncompleted, PREVIOUS "–" (`06`). Finish screen pre-filled the name "Verify A" (D-052).
  - **Start with a draft running: PASS.** Start Leg day → "Workout in progress" dialog; **Resume workout** returned to the same Verify A draft, nothing created (`07`). Draft then discarded via Finish → Save → Discard workout.
  - **Home's empty Start workout: PASS.** Opened an empty workout ("No exercises yet") (`08`); discarded.
  - **Edit: PASS.** Opened Verify A, ⋮ → **Move up** on Plank, Deadlift set 1 reps 5 → 3, Save, reopened: Plank first, reps 3, rest 150 kept (`09`, `10`).
  - **Delete: PASS.** Delete → confirm dialog with D-054 copy; **Cancel** stayed in the editor; Delete → Delete removed it from the list (`11`, `12`).
  - **Dark mode: PASS.** List and editor are readable with themed colors (`13`, `14`).
  - **Extra, Save as routine: PASS.** History → Leg day → ⋮ → Save as routine: dialog pre-filled "Leg day", Save showed the "Saved as routine" snackbar (`15`). Test routines were deleted afterwards; emulator is back to light mode with the builder's two routines.
  - **Observation (not reproduced, no ticket):** the first name typed into the Save-as-routine dialog by `adb input text` came out as "Ver Bi" instead of "Verify B". Three more fast `adb` entries in the same dialog ("Verify Bravo routine", "Alpha beta gamma delta", "Verify B") were exact, so it was most likely the keyboard still opening when typing started. If it ever shows up with real typing, the cause to check is that the dialog's text comes back through `combine(...).stateIn`, not a synchronous state.
  - **Result:** all acceptance steps pass; moved to `done/`.
- 2026-10-31 REVIEW (branch `t-034-routines-ui` at `6ff13eb`, `git diff main...t-034-routines-ui`):
  - **Scope:** matches the ticket and D-054: Routines list from Home, editor via the existing PICK picker, Move up/down/Remove menu (no drag, no folders), Save as routine in the detail ⋮ menu. Live-workout screen and its ViewModel are untouched (no new tap). No dependency, no schema change, no `:domain` change; `SwipeToDeleteSet` made public and `PlannedSetRow`/`PlannedSetTableHeader` added for reuse only.
  - **Layers:** ViewModels call the T-033 use cases; starting goes through `StartWorkoutFromRoutineUseCase` → `startDraftUnlessActive` (atomic, so a second draft cannot be made). What stays in the editor ViewModel is form state, text parsing at the UI edge (`parseInto`, rest text), and display numbering identical to `WorkoutSessionViewModel`.
  - **Tests:** three new test files plus a fake; no existing test or expected value changed. Strings all in `strings.xml`, light and dark previews for both new screens, 48dp on Start, the card, the top-bar buttons, and the menus (IconButton).
  - **Blocker:** none. **Should-fix:** none.
  - **Nit 1:** the editor's Add set copies the row above as a normal set in the ViewModel, a one-line copy of the `AddSetUseCase` rule (D-050). That's acceptable because routines are saved whole (D-052) and there's no domain operation per set, but if the rule changes, both places need updating.
  - **Nit 2:** `RoutinesViewModel.startRoutine` takes an `onStarted` callback into `viewModelScope`. A double-tap on Start makes the second call return `Resumed`, so the "Workout in progress" flag can be set while the app is already on the live workout, and it shows on return to Routines. A rotation mid-call would also invoke a stale lambda. Both are harmless and rare; a one-shot event would be cleaner.
  - **Nit 3:** each editor opening keeps its keyed `RoutineEditorViewModel` in the Activity store until the Activity finishes (same pattern as `Finish` entries); small and bounded by use.
  - **Commands:** `.\gradlew.bat :domain:test --rerun testDebugUnitTest --rerun assembleDebug lintDebug` → BUILD SUCCESSFUL; 349 tests (domain 182, app 105, data 62), 0 failures; lint 0 errors, 1 warning (`NewerVersionAvailable` junit-bom in `libs.versions.toml`, pre-existing).
  - **Result:** no fixes needed; moved to `verify/`.
- 2026-10-31 UI (branch `t-034-routines-ui`, from `main` at `2fe8389`):
  - **Done:** Home gets a **Routines** button → `RoutinesScreen` (list by title with exercise names, **Start** per routine, **+**, empty state, "Workout in progress" dialog when a draft exists) → `RoutineEditorScreen` (name, exercises via the existing `LibraryRoute` PICK picker, rest per exercise, planned set rows, Add set, swipe to delete a set, ⋮ Move up / Move down / Remove, Save, Delete with confirm). `WorkoutDetailScreen` gets a ⋮ menu with **Save as routine** (name dialog + snackbar). ViewModels only call the T-033 use cases (`ObserveRoutines`, `Create/Update/DeleteRoutine`, `StartWorkoutFromRoutine`, `SaveWorkoutAsRoutine`). Routes `Routines`, `RoutineEditor(routineId, entry)`, `RoutineAddExercises(routineId, entry)`; the picker reaches the editor's ViewModel through the same `routine-editor-$entry` key, like Add exercise does for the live workout. New `PlannedSetRow` / `PlannedSetTableHeader` in `SetRow.kt` reuse that file's private cells; `SwipeToDeleteSet` made public for reuse. Live-workout screen untouched. No dependency, no schema change, no `:domain` change. UI choices recorded as **D-054** (auto-accepted).
  - **Tests:** `RoutinesViewModelTest` (4), `RoutineEditorViewModelTest` (7), `WorkoutDetailViewModelTest` (2), app-side `FakeRoutineRepository`.
  - **Gotchas:** the editor keeps only *typed* text per field and leaves untouched values as stored kg, since re-parsing the rounded lb text would change them (20 kg → 19.9995 kg; covered by a test). The detail snackbar runs in a screen scope: marking it shown restarts the `LaunchedEffect` and cancelled it (found on the emulator, fixed).
  - **Commands:** `.\gradlew.bat :domain:test testDebugUnitTest assembleDebug lintDebug` → BUILD SUCCESSFUL; domain 182, app 105, data 62 tests, 0 failures; lint 1 warning (`NewerVersionAvailable` in `gradle/libs.versions.toml`, pre-existing, file untouched).
  - **Manual (emulator `medium_phone`, API 36, debug build of this branch):** empty Routines state shown. Created "Leg day" with Barbell Back Squat (100×5 ×2, rest 180) and Barbell Bench Press (80×8 ×2), saved, force-stopped and relaunched: routine still listed; Start → live workout showed both exercises with those sets. Start again with the draft running → "Workout in progress" dialog, Resume returned to the same draft. Finish showed the title "Leg day". From History detail, ⋮ → Save as routine created "Leg day B" (completed sets only) and showed "Saved as routine". Edit: moved Bench up and changed a rep to 6, saved, reopened: persisted. Delete: Cancel kept it, Delete removed it. Home → Start workout (empty) still opens an empty workout. Dark mode checked for the list and editor.
  - **Left / not verified:** no Compose UI test for the new screens (ViewModel tests only, as the ticket asks). Back from the editor discards unsaved changes without asking (D-054). The Settings "last time in this routine" radio stays disabled; enabling it is T-041.
