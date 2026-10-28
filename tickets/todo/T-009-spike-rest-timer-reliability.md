# T-009 — SPIKE: rest timer reliability on Android

- **Role:** SPIKE
- **Phase:** 1
- **Depends on:** T-001
- **Refs:** D-015 · SPEC F-05 · AC-02 · docs/ARCHITECTURE.md §5

## Goal
Choose a mechanism that fires the rest-timer alert reliably with the screen off and the app backgrounded, on the owner's phone.

## In scope
- Read the current Android documentation on foreground services, exact alarms, notification permission, and battery optimization (cite pages; do not rely on memory, rules change by Android version).
- Build a throwaway prototype (kept on a branch, not merged) for the 1–2 most promising approaches; test on the owner's phone: lock screen, 90 s timer, 5 repetitions each, with default battery settings.
- Report: reliability table, permissions/user-visible side effects, complexity, battery impact. **Validate D-015** (FGS + exact-alarm backup); note any required tweak for owner approval.

## Out of scope (do NOT do)
- Merging prototype code; polishing UI; any other feature.

## Acceptance criteria
- [ ] Measured results on the real device (not emulator only).
- [ ] Report confirms D-015 combo or documents required change for owner approval.

## Handoff log
