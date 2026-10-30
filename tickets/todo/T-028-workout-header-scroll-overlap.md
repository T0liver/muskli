# T-028 — Live-workout header stays readable while scrolling

- **Role:** UI
- **Phase:** 1
- **Depends on:** T-008
- **Refs:** SPEC §2, F-02 · docs/ARCHITECTURE.md §6
- **Size:** fits one session

## Goal
Scrolling a live workout no longer draws set rows through the title, duration, volume, or set count.

## In scope
- Give the pinned workout header an opaque surface in light and dark so content scrolling underneath does not show through.
- Keep the existing header contents and tap targets.

## Out of scope (do NOT do)
- New header actions, layout changes beyond making the bar cover the list, or other screens.

## Acceptance criteria
- [ ] On a workout long enough to scroll, the header text stays readable. Set rows do not show through the title or the duration / volume / sets row.
- [ ] Light and dark both hold. Touch targets stay at least 48dp.

## Verification
Scroll `LiveWorkoutAllSetTypesPreview` on the emulator (or the live workout screen) and confirm the header covers the rows. `build/verify/t008-all-set-types-scrolled.png` from the T-008 verify shows the overlap.

## Notes / decisions needed
Found in the T-008 verify (2026-10-30). `WorkoutHeader` has no background, and `LiveWorkoutContent` lays the list out behind the Scaffold top bar.

## Handoff log
<!-- Newest first. Each entry: date, role, what was done, what's left, gotchas, commands run + results. -->
