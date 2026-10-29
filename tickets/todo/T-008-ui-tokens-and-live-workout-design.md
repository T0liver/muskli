# T-008 — UI: design tokens + live-workout screen (static)

- **Role:** UI
- **Phase:** 1
- **Depends on:** T-001. Live-workout reference screenshots are in `docs/reference-ui/workout-screen/`. Use those. Do not copy Hevy assets.
- **Refs:** SPEC §2, F-02, F-03 · docs/ARCHITECTURE.md §6

## Goal
The look and structure of the live-workout screen as stateless composables with previews and fake data; theme tokens (color, type, shape, spacing), light and dark.

## In scope
- Theme tokens; components: stopwatch/header (sets + volume), exercise card, set row (SET | PREVIOUS | KG | REPS | check) with type marker, rest-timer bar, add-set / add-exercise controls, exercise menu (UI only).
- Previews for: empty, 3 exercises with all four set types, checked/unchecked sets, dark mode, large font.
- Inspired by `docs/reference-ui/workout-screen/`; own colors/icons/text. No Hevy assets.

## Out of scope (do NOT do)
- State, ViewModels, persistence, business logic, other screens.

## Acceptance criteria
- [ ] All previews render; touch targets >= 48dp; strings in `strings.xml`.
- [ ] Logging a prefilled set needs <= 2 taps in the design (owner confirms from previews).
- [ ] Owner approved the look (note in Handoff log).

## Handoff log
- 2026-10-28 DOCS: Screenshot-folder dependency closed. Owner sorts `docs/reference-ui/` themselves; this ticket must not wait on that.
- 2026-10-28 DOCS: Owner sorted the screenshots. Live workout is `docs/reference-ui/workout-screen/` (not `01-live-workout/`).
