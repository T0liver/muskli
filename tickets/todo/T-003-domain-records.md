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
- [ ] TC-PR-01..07 tests pass (names contain ids).
- [ ] Removing a workout from the input list restores previous records (TC-PR-05).

## Verification
`./gradlew :domain:test`

## Handoff log
- 2026-10-28 DOCS: Owner pinned D-020 (at least N). TC-PR-07 is in scope; no UI.
