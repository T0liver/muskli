# T-012 — Live workout: draft persistence + use cases

- **Role:** BUILD
- **Phase:** 1
- **Depends on:** T-006, T-010
- **Refs:** SPEC F-02 AC2–3, F-03 AC1,3,4 · TC-SET-* · TC-DATA-02 · AC-03 · docs/ARCHITECTURE.md §4

## Goal
All live-workout operations as use cases over a persisted draft: start, add/replace/remove/reorder exercise, add/delete set, change set type, edit values, complete/uncomplete set.

## In scope
- Use cases in `:domain`, repository support in `:data`, tests first for TC-SET-01..04 and TC-DATA-02.
- `CompleteSet` calls `restTimerEffect` (T-004) and returns the effect; no timer implementation here.
- Exactly one active draft; starting while a draft exists returns a clear result (resume).

## Out of scope (do NOT do)
- UI, previous values, rest timer service, finishing (T-016).

## Acceptance criteria
- [x] TC-SET-01..04, TC-DATA-02 pass.
- [x] Every mutation is persisted immediately (verified by recreating the DB connection in a test).

## Handoff log
- 2026-10-30 BUILD: Started at the owner's request with T-010 in `review/`, not `done/`. Branch `t-012-live-workout-draft-and-usecases` is stacked on `t-010-exercise-library-data`.
  - **Owner decisions (recorded as D-029):** an added exercise starts with one empty normal set. Replace drops the old sets for one empty normal set. Next set for the rest timer is plain order. `WorkoutRepository.update(id, transform)` and `startDraftUnlessActive(draft)` approved. `Clock.utcOffsetS(epochMs)` approved, so `Clock` is no longer a `fun interface`.
  - **`:domain`:** `usecase/workout/LiveWorkoutUseCases.kt` has one class per operation: `StartWorkoutUseCase` (returns `StartWorkoutResult.Started`/`Resumed`; the title stays empty until T-016), `AddExercisesUseCase` (several at once, in order), `ReplaceExerciseUseCase` (keeps the superset, resets notes/rest), `RemoveExerciseUseCase`, `ReorderExerciseUseCase` (by id, target index clamped), `AddSetUseCase`, `DeleteSetUseCase`, `ChangeSetTypeUseCase`, `EditSetValuesUseCase` (`SetValues`: weight kg, reps, duration, distance), `CompleteSetUseCase` (returns `RestEffect` from `restTimerEffect`; the caller passes the settings default), `UncompleteSetUseCase`. Every mutation runs in one `update` transaction and stamps `updatedAt`. An unknown exercise or set id writes nothing. Completing an already completed set writes nothing and starts no timer.
  - **`:data`:** `repo/RoomWorkoutRepository` (all of `WorkoutRepository`; `update` and `startDraftUnlessActive` use `withTransaction`). `WorkoutDao.get` and `activeDraft` added. `SystemWallClock.utcOffsetS` via `TimeZone.getDefault()`. `:app` `DataModule` provides `WorkoutRepository`.
  - **Tests:** domain `LiveWorkoutUseCasesTest` (19: TC-SET-01..04, start/resume, add/replace/remove/reorder, edit, complete with exercise rest / default rest / drop set / last set / already completed, uncomplete, unknown ids, `updatedAt`). These failed to compile before the implementation. Data `LiveWorkoutPersistenceTest` (4, file DB): TC-DATA-02 closes and reopens the database after every use case and compares with what the use case returned; stored `position` columns are contiguous from 0 after delete/reorder (TC-SET-03/04); resume keeps one draft row; 20 concurrent `AddSet` calls on `Dispatchers.IO` lose nothing. I didn't check that the concurrency test fails without the transaction.
  - **Commands:** `.\gradlew.bat :domain:test testDebugUnitTest assembleDebug lintDebug` → BUILD SUCCESSFUL. `:domain` 96 tests and `:data` 29 tests, 0 failures. Lint: the same 2 warnings as before (ObsoleteLintCustomCheck, NewerVersionAvailable). Not run: on-device AC-03 (no UI yet; that's T-013/T-019), `connectedDebugAndroidTest`.
  - **Gotchas:** `save` rewrites the workout's exercises and sets on every edit (delete + insert). That's fine at workout size; typing should be debounced in the ViewModel (ARCHITECTURE §4). Raw `db.query` in tests must run off the main thread.
  - **Left:** nothing in scope. The rest timer (T-015) persists `restTimerEndsAtEpochMs`. Finishing is T-016.
