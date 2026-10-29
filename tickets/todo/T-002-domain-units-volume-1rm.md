# T-002 — Domain: units, set volume, estimated 1RM

- **Role:** BUILD
- **Phase:** 0
- **Depends on:** T-001
- **Refs:** TC-UNIT-*, TC-VOL-01..10, TC-1RM-* · research/05 §4–5 · D-003, D-010, D-014, D-017

## Goal
Pure, tested functions for kg/lb conversion, set/session volume and estimated 1RM, plus the minimal domain types they need (`SetType`, `ExerciseType`, `SetData`).

## In scope
- Tests first from the listed TC ids, then implementation in `:domain/calc`.
- `OneRmFormula` enum (Epley, Brzycki); default per D-003 is chosen by the caller, not hardcoded in the function.
- Volume counts only completed sets; warm-up inclusion is a parameter (default excluded).
- Bodyweight and assisted volume take `bodyweightKg` (D-010, D-017). Unilateral `side` multiplier per D-014 (TC-VOL-09). Unset body weight returns 0 (TC-VOL-10).

## Out of scope (do NOT do)
- Persistence. UI. PR logic. Reading settings or storing body weight (T-017).

## Acceptance criteria
- [ ] All listed TC tests exist (test names contain the TC id) and pass.
- [ ] Functions are pure; no Android imports; no `System.currentTimeMillis`.
- [ ] Invalid inputs return `null`/0 as specified, no exceptions.

## Verification
`./gradlew :domain:test`

## Handoff log
- 2026-10-28 DOCS: Owner pinned D-017. TC-VOL-06..10 are in scope. Body weight is a parameter.
