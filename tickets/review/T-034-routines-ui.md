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
- 2026-10-31 UI (branch `t-034-routines-ui`, from `main` at `2fe8389`):
  - **Done:** Home gets a **Routines** button → `RoutinesScreen` (list by title with exercise names, **Start** per routine, **+**, empty state, "Workout in progress" dialog when a draft exists) → `RoutineEditorScreen` (name, exercises via the existing `LibraryRoute` PICK picker, rest per exercise, planned set rows, Add set, swipe to delete a set, ⋮ Move up / Move down / Remove, Save, Delete with confirm). `WorkoutDetailScreen` gets a ⋮ menu with **Save as routine** (name dialog + snackbar). ViewModels only call the T-033 use cases (`ObserveRoutines`, `Create/Update/DeleteRoutine`, `StartWorkoutFromRoutine`, `SaveWorkoutAsRoutine`). Routes `Routines`, `RoutineEditor(routineId, entry)`, `RoutineAddExercises(routineId, entry)`; the picker reaches the editor's ViewModel through the same `routine-editor-$entry` key, like Add exercise does for the live workout. New `PlannedSetRow` / `PlannedSetTableHeader` in `SetRow.kt` reuse that file's private cells; `SwipeToDeleteSet` made public for reuse. Live-workout screen untouched. No dependency, no schema change, no `:domain` change. UI choices recorded as **D-054** (auto-accepted).
  - **Tests:** `RoutinesViewModelTest` (4), `RoutineEditorViewModelTest` (7), `WorkoutDetailViewModelTest` (2), app-side `FakeRoutineRepository`.
  - **Gotchas:** the editor keeps only *typed* text per field and leaves untouched values as stored kg, since re-parsing the rounded lb text would change them (20 kg → 19.9995 kg; covered by a test). The detail snackbar runs in a screen scope: marking it shown restarts the `LaunchedEffect` and cancelled it (found on the emulator, fixed).
  - **Commands:** `.\gradlew.bat :domain:test testDebugUnitTest assembleDebug lintDebug` → BUILD SUCCESSFUL; domain 182, app 105, data 62 tests, 0 failures; lint 1 warning (`NewerVersionAvailable` in `gradle/libs.versions.toml`, pre-existing, file untouched).
  - **Manual (emulator `medium_phone`, API 36, debug build of this branch):** empty Routines state shown. Created "Leg day" with Barbell Back Squat (100×5 ×2, rest 180) and Barbell Bench Press (80×8 ×2), saved, force-stopped and relaunched: routine still listed; Start → live workout showed both exercises with those sets. Start again with the draft running → "Workout in progress" dialog, Resume returned to the same draft. Finish showed the title "Leg day". From History detail, ⋮ → Save as routine created "Leg day B" (completed sets only) and showed "Saved as routine". Edit: moved Bench up and changed a rep to 6, saved, reopened: persisted. Delete: Cancel kept it, Delete removed it. Home → Start workout (empty) still opens an empty workout. Dark mode checked for the list and editor.
  - **Left / not verified:** no Compose UI test for the new screens (ViewModel tests only, as the ticket asks). Back from the editor discards unsaved changes without asking (D-054). The Settings "last time in this routine" radio stays disabled; enabling it is T-041.
