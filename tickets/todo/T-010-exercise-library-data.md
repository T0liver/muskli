# T-010 — Exercise library: seed loader + repository

- **Role:** BUILD
- **Phase:** 1
- **Depends on:** T-006, T-007 (D-005 decided)
- **Refs:** SPEC F-01 AC1–4 · docs/ARCHITECTURE.md §2

## Goal
Seeded exercise library with search/filter queries and custom-exercise CRUD at the repository level.

## In scope
- Seed file in `:data` assets loaded once (versioned, idempotent); repository: search by name, filter by muscle/equipment, create/edit/archive custom, recently used.
- Tests for search/filter/CRUD and seed idempotency.

## Out of scope (do NOT do)
- UI, media/images, seed content beyond the approved source.

## Acceptance criteria
- [ ] Seed loads on first run only; re-run does not duplicate.
- [ ] Search is case/diacritic-insensitive; filters combine with AND.
- [ ] Custom exercises are unlimited and distinguishable (`isCustom`).

## Handoff log
