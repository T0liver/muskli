# T-003 — Domain: records (PR detection)

- **Role:** BUILD
- **Phase:** 0
- **Depends on:** T-002
- **Refs:** TC-PR-01..07 · research/05 §5 · D-020

## Goal
Given an exercise's history and a candidate set, compute which records it breaks; compute current records from history (recomputable, not stored).

## In scope
- Categories: heaviest weight, estimated 1RM, most reps (overall), best set volume, best duration. Strictly-greater rule. Warm-ups never count.
- Set Records: best weight at **N or more** reps (D-020, TC-PR-07).
- Pure functions over lists of completed sets with workout ids/dates.

## Out of scope (do NOT do)
- Best-session volume, Set Records UI, toasts, caching/storage.

## Acceptance criteria
- [x] TC-PR-01..07 tests pass (names contain ids).
- [x] Removing a workout from the input list restores previous records (TC-PR-05).

## Verification
`./gradlew :domain:test`

## Handoff log
### 2026-10-29 — REVIEW
Owner review: no findings (Blocker / Should-fix / Nit: none). Verification is `:domain:test`, already green. Moved to `done/`.

### 2026-10-29 — BUILD
**Done.** `:domain/calc/Records.kt` adds `RecordType` (heaviest weight, estimated 1RM, most reps, best set volume, best duration), `HistorySet(workoutId, performedAtEpochMs, set)` and `Record(type, value, workoutId)`. It also adds three functions:
- `currentRecords(history, exerciseType, formula)`: recomputed from the list on each call, nothing stored. When values tie, the earliest set keeps the record (strictly-greater rule).
- `recordsBrokenBy(candidate, history, exerciseType, formula)`: returns an empty set for a warm-up or an uncompleted candidate.
- `setRecords(history)`: best weight at N **or more** reps (D-020), with keys 1 up to the most reps logged.

The caller passes the formula (D-003) and supplies one exercise's history.

**Owner decision taken this session (D-027):** which exercise types each category applies to follows research/05 §5. Best set volume is raw weight × reps for weight_reps, so it is unaffected by body weight or the unilateral flag.

**Behaviour notes:** TC-PR-01's "reps-at-weight PR" is covered two ways: `MOST_REPS` is broken, and `setRecords` gains a 6-rep entry. History entries that are warm-ups or not completed are ignored everywhere. There is an extra test for uncompleted history sets, and a TC-PR-05 test that a later equal set does not take the record over.

**Verified:** `.\gradlew.bat :domain:test` → 39 tests, 0 failures (Records 10). The tests failed to compile before the implementation existed. `.\gradlew.bat assembleDebug` → BUILD SUCCESSFUL.

**Left:** nothing in scope (best-session volume, UI, toasts and caching stay out). **Open questions:** none.

- 2026-10-28 DOCS: Owner pinned D-020 (at least N). TC-PR-07 is in scope; no UI.
