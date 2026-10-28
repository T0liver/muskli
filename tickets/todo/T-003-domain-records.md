# T-003 — Domain: records (PR detection)

- **Role:** BUILD
- **Phase:** 0
- **Depends on:** T-002
- **Refs:** TC-PR-01..06 · research/05 §5

## Goal
Given an exercise's history and a candidate set, compute which records it breaks; compute current records from history (recomputable, not stored).

## In scope
- Categories: heaviest weight, estimated 1RM, most reps (overall), best set volume, best duration. Strictly-greater rule. Warm-ups never count.
- Pure functions over lists of completed sets with workout ids/dates.

## Out of scope (do NOT do)
- TC-PR-07 (Set Records semantics undecided), best-session volume, UI, toasts, caching/storage.

## Acceptance criteria
- [ ] TC-PR-01..06 tests pass (names contain ids).
- [ ] Removing a workout from the input list restores previous records (TC-PR-05).

## Verification
`./gradlew :domain:test`

## Handoff log
