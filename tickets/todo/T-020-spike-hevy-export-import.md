# T-020 — SPIKE: Hevy export format + importer plan

- **Role:** SPIKE
- **Phase:** 1
- **Depends on:** T-001 · **needs the owner's Hevy export file in `docs/fixtures/`**
- **Refs:** SPEC F-09 · research/06, 10 (A15)

## Goal
Understand the real export format and plan the importer.

## In scope
- Inspect the file: columns, units, timestamps/timezones, set types, superset info, notes, exercise naming.
- Map to our model; list unmappable/ambiguous fields; propose exercise-matching strategy (exact title, normalized, manual mapping UI).
- Write the importer ticket(s), sized to one session each.

## Out of scope (do NOT do)
- Writing the importer; committing personal data (add fixtures to `.gitignore` if private).

## Acceptance criteria
- [ ] Mapping table + risks + follow-up tickets in `todo/`.

## Handoff log
