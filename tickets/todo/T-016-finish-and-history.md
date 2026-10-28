# T-016 — Finish flow + history

- **Role:** BUILD
- **Phase:** 1
- **Depends on:** T-013
- **Refs:** SPEC F-06 · D-011

## Goal
Finish a workout (name, duration, start date/time, note), then browse, open, edit, delete (with undo), and log past workouts; exercise history screen.

## In scope
- Finish screen + use case (discard uncompleted sets after confirmation, per D-011 proposal).
- History list/detail; edit uses the live-workout components in edit mode; soft delete with undo; "add past workout" entry.
- Exercise history (sessions list) reachable from an exercise in a workout.

## Out of scope (do NOT do)
- Media, privacy flags, sharing, post-workout celebration, charts/records UI.

## Acceptance criteria
- [ ] Finished workouts appear newest-first; deleting hides and undo restores (TC-DATA-03).
- [ ] Editing a past workout persists and updates previous-values for later sessions.
- [ ] Logging a past date works.

## Handoff log
