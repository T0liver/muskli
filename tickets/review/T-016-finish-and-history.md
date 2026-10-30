# T-016 — Finish flow + history

- **Role:** BUILD
- **Phase:** 1
- **Depends on:** T-013
- **Refs:** SPEC F-06 · D-011

## Goal
Finish a workout (name, duration, start date/time, note), then browse, open, edit, delete (with undo), and log past workouts; exercise history screen.

## In scope
- Finish screen + use case (discard uncompleted sets after confirmation, per D-011 proposal).
- History list/detail; edit uses the live-workout components in edit mode; soft delete with undo; "add past workout" entry.
- Exercise history (sessions list) reachable from an exercise in a workout.

## Out of scope (do NOT do)
- Media, privacy flags, sharing, post-workout celebration, charts/records UI.

## Acceptance criteria
- [x] Finished workouts appear newest-first; deleting hides and undo restores (TC-DATA-03).
- [x] Editing a past workout persists and updates previous-values for later sessions.
- [x] Logging a past date works.

## Handoff log
- **2026-10-30 (BUILD):** Done. Owner rules recorded as **D-033**.
  - **Domain:** `FinishAndHistoryUseCases.kt` (`FinishWorkoutUseCase`, `DeleteWorkoutUseCase`, `RestoreWorkoutUseCase`, `StartPastWorkoutUseCase`, `PreviousSetsBeforeUseCase`, `MarkSetCompletedUseCase`, `WorkoutDetails`, `FinishWorkoutResult`) and `history/ExerciseSessions.kt`. These are new public `:domain` APIs, nothing existing changed. No schema change (still v3).
  - **App:** Finish screen (`ui/finish`), History list, workout detail, and exercise history (`ui/history`), date/time picker dialogs. The live-workout screen gets an edit mode (assisted `WorkoutSessionViewModel(editWorkoutId)`), header says "Edit workout"/"Save". Exercise menu gains **History**; Home gains **History**. A small hand-rolled, saveable back stack (`AppRoutes.kt`) replaces the old screen switch in `MainActivity`; still no navigation library.
  - **Gotchas:** Back in edit mode opens the Save step (edits are already written). The Undo snackbar lives on History, so detail Delete pops back there. Finish duration has minute precision. PREVIOUS in edit mode only uses workouts that started **before** the edited one; the live screen reloads PREVIOUS on `ScreenShown`, so an edit made meanwhile shows up. Ticking a set in edit mode never starts a rest.
  - **Tests:** `FinishAndHistoryUseCasesTest`, `ExerciseSessionsTest` (domain); `WorkoutHistoryTest` (Room: TC-DATA-03 newest-first/delete/undo, edit changes previous values, past date); `FinishViewModelTest`, `HistoryViewModelTest`, `WorkoutDateTimeTest`, new `WorkoutSessionViewModelTest` cases (app).
  - **Commands:** `.\gradlew.bat :domain:test testDebugUnitTest assembleDebug lintDebug` → BUILD SUCCESSFUL, 0 failures, lint 0 errors (only the existing `NewerVersionAvailable` warning).
  - **Emulator smoke (medium_phone, API 36):** started a workout, logged 100×5 plus an unticked set, Finish → "Discard 1 uncompleted set?" → saved as "Leg day", shown on History. Detail → Edit reps to 6 → Save → detail shows 100 kg × 6. Delete → snackbar → Undo restored it. Log past (Oct 27) with nothing logged → Back deleted it; again with 90×5 → saved, sorted below Leg day. Exercise history lists both sessions newest first. A new live workout shows PREVIOUS 100 × 6; exercise menu History works; Finish with nothing completed → "Discard workout" returned Home.
  - **Left:** nothing in scope.
