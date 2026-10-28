# T-002 — Domain: units, set volume, estimated 1RM

- **Role:** BUILD
- **Phase:** 0
- **Depends on:** T-001
- **Refs:** TC-UNIT-*, TC-VOL-01..05, TC-1RM-* · research/05 §4–5 · D-003, D-010

## Goal
Pure, tested functions for kg/lb conversion, set/session volume and estimated 1RM, plus the minimal domain types they need (`SetType`, `ExerciseType`, `SetData`).

## In scope
- Tests first from the listed TC ids, then implementation in `:domain/calc`.
- `OneRmFormula` enum (Epley, Brzycki); default per D-003 is chosen by the caller, not hardcoded in the function.
- Volume counts only completed sets; warm-up inclusion is a parameter (default excluded).

## Out of scope (do NOT do)
- TC-VOL-06 (bodyweight volume, undecided). Persistence. UI. PR logic.

## Acceptance criteria
- [ ] All listed TC tests exist (test names contain the TC id) and pass.
- [ ] Functions are pure; no Android imports; no `System.currentTimeMillis`.
- [ ] Invalid inputs return `null`/0 as specified, no exceptions.

## Verification
`./gradlew :domain:test`

## Handoff log
