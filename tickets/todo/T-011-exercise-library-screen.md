# T-011 — Exercise library screen

- **Role:** UI+BUILD
- **Phase:** 1
- **Depends on:** T-008, T-010
- **Refs:** SPEC F-01 · T-008 tokens. The sorted set in `docs/reference-ui/` has no exercise-library folder; do not block on more screenshots.

## Goal
Library screen with search, filters, recents, create/edit custom exercise form, and a picker mode used by "Add exercise" (multi-select).

## In scope
- ViewModel + UiState + Compose screen; create form with type/primary muscle/secondary muscles/equipment pickers.
- ViewModel tests with fake repository; one Compose UI test for search + select.

## Out of scope (do NOT do)
- Exercise detail/stats/charts, media, the live-workout screen itself.

## Acceptance criteria
- [ ] Search, filter, recents work; multi-select returns chosen ids.
- [ ] Custom exercise can be created, edited, archived.
- [ ] Works in dark mode and with large fonts.

## Handoff log
