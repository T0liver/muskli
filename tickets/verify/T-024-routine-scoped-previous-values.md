# T-024 — Routine-scoped previous values

- **Role:** BUILD
- **Phase:** 2
- **Depends on:** T-005, T-033
- **Refs:** TC-PREV-03 · SPEC F-04 AC2 · D-019
- **Size:** fits one session

## Goal
`previousFor(..., PreviousMode.ROUTINE_SCOPED, routineId)` returns the set from the last workout of the same routine instead of throwing.

## In scope
- Test first from TC-PREV-03, then replace the `TODO("T-024 ...")` in `:domain/calc/PreviousValues.kt`.
- `WorkoutExerciseRecord.routineId` already exists.

## Out of scope (do NOT do)
- Routines themselves, settings UI, persistence.

## Acceptance criteria
- [x] TC-PREV-03 passes; the other TC-PREV tests are unchanged and still pass.

## Verification
`./gradlew :domain:test`

## Notes / decisions needed
What should routine mode return for a workout started without a routine (`routineId` null): anywhere, or null? Owner, 2026-10-30: **anywhere** (D-038). Also wire the existing previous-mode setting to pass the draft's `routineId`.

## Handoff log
- 2026-10-31 REVIEW: Diff `main...t-024-routine-scoped-previous-values` (`340a14e`) checked against the ticket and the WORKFLOW §5 checklist. **No Blockers, no Should-fix.** Moved to `verify/`.
  - **Ticket and TCs.** `PreviousValuesTest` asserts TC-PREV-03 as written (routine A gives 80x8, B gives 85x6, anywhere gives 85x6 for both). TC-PREV-01/02/04/05/06/07 are untouched: the only change to existing test code is the optional `routineId` parameter on the `record(...)` helper, which defaults to null, so no expected value changed. `TODO("T-024 ...")` is gone (no `T-024` left in any `.kt`).
  - **D-038.** `previousSets` only filters when the mode is `ROUTINE_SCOPED` **and** `routineId != null`, so a workout without a routine falls back to anywhere; tested in the domain (`routine mode without a routine falls back...`) and the ViewModel (`routineModeWithoutARoutineShowsLastTimeAnywhere`).
  - **Wiring.** The live workout reads the stored mode with `settings.first()` and passes `workout.routineId` to `PreviousSetsUseCase` and to `AddExercisesUseCase` (prefill); editing a past workout uses that workout's `routineId` via `PreviousSetsBeforeUseCase`. A mode change reloads PREVIOUS. `:data` fills `WorkoutExerciseRecord.routineId` from `workout.routine_id` (`Mappers.kt`), so the filter sees real data.
  - **Checklist.** Logic is in `:domain`, the ViewModel only picks arguments. No new tap or control in the logging flow; the Settings radio stays disabled (D-053, T-041). No new strings, colors, dependency, or schema change. The `:domain` API change is additive with `ANYWHERE` defaults and is recorded in D-053 under D-039. No main-thread I/O added (DataStore `first()` runs in coroutines).
  - **Nit 1 (no fix).** The mode-change collector and `loadMissingPrevious` are not serialised. If the mode changed while a first load was still running, a stale-mode result could be written for one template. This can't happen yet because the radio is disabled, and `ScreenShown` already uses the same pattern. T-041 should keep an eye on it.
  - **Nit 2 (no fix).** `previousOf` reads `settings.first()` once per template rather than once per reload. That cost is small, and reading per lookup is what D-053 describes.
  - **Commands:** `$env:JAVA_TOOL_OPTIONS='-Djavax.net.ssl.trustStoreType=Windows-ROOT'; .\gradlew.bat :domain:test testDebugUnitTest assembleDebug lintDebug` BUILD SUCCESSFUL in 2m 24s. Test reports: domain 182, app 92, data 62 = 336 tests, 0 failures/errors/skipped. Lint: 0 errors, 1 warning (existing junit-bom `NewerVersionAvailable`, D-016).
- 2026-10-31 BUILD: Branch `t-024-routine-scoped-previous-values` from `main` (`a9fe28c`).
  - **Tests first.** `PreviousValuesTest` +5: TC-PREV-03 routine A gives 80x8 and B gives 85x6; TC-PREV-03 anywhere gives 85x6 for both; routine mode with `routineId` null falls back to anywhere (D-038); an exercise never done in the routine is null/empty (D-053); drafts, deleted workouts, and first occurrence still apply in routine mode. Use-case tests: `LiveWorkoutUseCasesTest` +2 (PREVIOUS column and Add-exercise prefill in routine mode), `FinishAndHistoryUseCasesTest` +1 (editing a past workout scopes by its own routine). The use-case signatures were added first with a pass-through, so all 7 routine-mode tests ran and failed with `kotlin.NotImplementedError` from the `TODO`; the anywhere-mode TC-PREV-03 case passed already, as it should. The test helper `record(...)` only gained an optional `routineId`. No existing expected value changed.
  - **Domain.** `previousSets` filters the history to `routineId` when the mode is `ROUTINE_SCOPED` and `routineId` is not null, then applies the unchanged D-019 rules. The `TODO("T-024 ...")` is gone. `PreviousSetsUseCase(templateId, mode, routineId)`, `AddExercisesUseCase(workoutId, templateIds, mode, routineId)`, `PreviousSetsBeforeUseCase(templateId, workout, mode)` (uses `workout.routineId`); the new parameters are optional and default to `ANYWHERE`, so the other callers and tests are unchanged.
  - **Wiring.** `WorkoutSessionViewModel` reads the stored `previousMode` (`settings.first()`, not the `Settings()` initial value) for each lookup and passes the draft's `routineId` to the PREVIOUS column and to Add-exercise prefill. A change of the mode reloads the PREVIOUS column. `WorkoutSessionViewModelTest` +3: a routine-A draft in routine mode shows and prefills 80x8; a draft without a routine in routine mode shows 85x6; an anywhere-mode routine-A draft shows 85x6, then switching the mode updates it to 80x8.
  - **Decision D-053** (auto-accepted recommendation): no fallback when the exercise was never done in the routine; prefill follows the mode; the API shape above; the Settings radio stays disabled. **New ticket T-041** enables the radio once T-034 lets routines be started. The Settings comment and `Settings.previousMode` KDoc now point to the right place.
  - **Not checked on a device.** The "Last time in this routine" radio is still disabled and routines cannot be started without the T-034 UI, so routine mode cannot be reached on the emulator. Covered by the ViewModel tests above instead. Anywhere mode is not changed by this ticket (same code path with the default arguments); it was not re-checked on the emulator either.
  - **Commands:** `$env:JAVA_TOOL_OPTIONS='-Djavax.net.ssl.trustStoreType=Windows-ROOT'`; `.\gradlew.bat :domain:test` before the implementation gave 182 tests, 7 failed (`NotImplementedError`). After it: `.\gradlew.bat :domain:test testDebugUnitTest` BUILD SUCCESSFUL, 336 tests (domain 182, app 92, data 62), 0 failures. `.\gradlew.bat assembleDebug lintDebug` BUILD SUCCESSFUL; lint: only the existing junit-bom `NewerVersionAvailable` (D-016), `:data` no issues. No new dependency, no schema change.
- 2026-10-29 BUILD (T-005): Created. The mode is typed and throws `NotImplementedError` until this ticket.
