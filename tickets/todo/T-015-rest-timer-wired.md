# T-015 — Rest timer wired in

- **Role:** BUILD
- **Phase:** 1
- **Depends on:** T-004, T-009 (D-015 decided), T-013, T-017
- **Refs:** SPEC F-05 · AC-02, AC-03, AC-05 · docs/ARCHITECTURE.md §5

## Goal
Completing a set starts the rest timer per the rule; the alert fires with the screen off; state survives process death.

## In scope
- Implement the mechanism approved in D-015; in-app timer bar (+/-15 s, skip); notification with the same actions; sound/vibration per settings.
- `restTimerEndsAt` persisted in the draft; UI derives remaining time from the clock.

## Out of scope (do NOT do)
- Superset semantics, per-exercise rest editing UI beyond what T-013 provides.

## Acceptance criteria
- [ ] No timer after completing the set before a drop set (AC-05).
- [ ] Alert fires with the screen off on the owner's phone (AC-02) and after force-stop/reopen the remaining time is correct (AC-03).

## Handoff log
