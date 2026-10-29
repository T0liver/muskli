# T-005 — Domain: previous-values lookup

- **Role:** BUILD
- **Phase:** 0
- **Depends on:** T-002
- **Refs:** TC-PREV-01, 02, 04, 05, 06, 07 · SPEC F-04 · D-019

## Goal
Pure function that returns the previous set for (exercise, set position) from finished-workout history, mode "anywhere".

## In scope
- Tests first. Align by raw set index, warm-ups included (D-019, TC-PREV-05).
- Ignore drafts and soft-deleted workouts; use the most recent finished workout containing the exercise; first occurrence if repeated (TC-PREV-07, D-019).
- Leave a `PreviousMode.RoutineScoped` variant **unimplemented but typed** (TC-PREV-03 arrives with routines).

## Out of scope (do NOT do)
- Routines, persistence, UI, prefill-on-add (that's T-014).

## Acceptance criteria
- [x] Listed TC tests pass, including TC-PREV-05 (raw index). TC-PREV-03 stays unimplemented (Phase 2) and is noted in the Handoff log.

## Verification
`./gradlew :domain:test`

## Handoff log
### 2026-10-29 — REVIEW
Owner review: no findings (Blocker / Should-fix / Nit: none). TC-PREV-03 stays with T-024. Verification is `:domain:test`, already green. Moved to `done/`.

### 2026-10-29 — BUILD (validation)
**Fixed before merge.** The first version dropped uncompleted sets and then indexed, so a gap shifted every later set. That contradicts D-019's raw index. `previousFor` now returns the set at the raw index only when it is completed. The TC-PREV-06 test covers a warm-up at index 0, an uncompleted set at index 1, and a completed set that stays at index 2.

### 2026-10-29 — BUILD
**Done.** `:domain/calc/PreviousValues.kt` adds `PreviousMode { ANYWHERE, ROUTINE_SCOPED }`, `WorkoutExerciseRecord`, and `previousFor(history, setIndex, mode, routineId)` (ARCHITECTURE §7 signature). `WorkoutExerciseRecord` holds the workout id, start, finished-at, deleted-at, the exercise's position in the workout, its sets, and an optional `routineId`. The caller passes one exercise's records.

**How the lookup works:**
- Drafts (null finished-at) and soft-deleted workouts are skipped.
- The most recent workout by **start time** wins, so a back-dated workout doesn't count as "last time".
- If the exercise appears more than once in that workout, the lowest `positionInWorkout` is used (TC-PREV-07).
- The set is picked by raw index, warm-ups included (TC-PREV-05). An uncompleted set at that index returns null and does not shift the sets after it (TC-PREV-06).
- An out-of-range or negative index returns null.

**TC-PREV-03 is not implemented (Phase 2).** `ROUTINE_SCOPED` is typed but calls `TODO("T-024 ...")`, which throws `NotImplementedError`. Nothing may select that mode until **T-024** (new) lands. T-024 also records the open question of what routine mode returns for a workout without a routine.

**Gotcha:** a finished workout that contains the exercise but has no completed sets for it still counts as "last time", so the lookup returns null and doesn't fall back to an older workout. D-011 drops uncompleted sets on finish, so this should not come up in practice.

**Verified:** `.\gradlew.bat :domain:test assembleDebug` → BUILD SUCCESSFUL, 56 domain tests, 0 failures (PreviousValues 8). The tests failed to compile before the implementation existed.

**Left:** TC-PREV-03 → T-024. Prefill-on-add is T-014. **Open questions:** none for this ticket.

- 2026-10-28 DOCS: Owner pinned D-019. Raw index is decided; only TC-PREV-03 stays pending.
