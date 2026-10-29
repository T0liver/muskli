# T-010 — Exercise library: bundled catalogue + repository

- **Role:** BUILD
- **Phase:** 1
- **Depends on:** T-006, T-007 (D-005, D-021)
- **Refs:** SPEC F-01 AC1–4 · docs/ARCHITECTURE.md §2 · D-021

## Goal
Offline exercise library from a bundled catalogue snapshot, with search/filter queries and custom-exercise CRUD at the repository level.

## In scope
- Bundled snapshot in `:data` assets, same format and version field as T-007. Load on startup when the asset version is newer than the stored catalogue version; upsert by id; idempotent (no duplicates).
- Do not modify custom exercises. Do not hard-delete a published exercise a workout references; archive unreferenced rows missing from the bundle.
- Repository: search by name, filter by muscle/equipment, create/edit/archive custom, recently used.
- Tests for search/filter/CRUD, version gate, and idempotency.

## Out of scope (do NOT do)
- Network sync (T-021). UI, media/images. Inventing catalogue content beyond the T-007 sample copied into assets.

## Acceptance criteria
- [ ] First launch with an empty DB loads the bundle. A second launch at the same version does not duplicate.
- [ ] A newer bundled version upserts; an older or equal version does not.
- [ ] Search is case/diacritic-insensitive; filters combine with AND.
- [ ] Custom exercises are unlimited, distinguishable (`isCustom`), and untouched by the loader.

## Handoff log
- 2026-10-28 DOCS: Owner pinned D-021. This ticket is the bundled snapshot only. Network sync is T-021.
