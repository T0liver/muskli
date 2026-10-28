# T-004 — Domain: rest-timer rule

- **Role:** BUILD
- **Phase:** 0
- **Depends on:** T-001
- **Refs:** TC-REST-01..06 · SPEC F-05 · D-012

## Goal
Pure function `restTimerEffect(...)` returning `Start(seconds)` or `None` per the rules (exercise rest, default rest, off, no start before a drop set).

## In scope
- Tests first from TC-REST-01..06; sealed `RestEffect` type.
- TC-REST-07: implement the **proposed** behavior (None after the last set of the workout) and note it in the Handoff log for owner confirmation.

## Out of scope (do NOT do)
- TC-REST-08 (superset semantics). Any timer, service, notification, or UI code.

## Acceptance criteria
- [ ] TC-REST-01..06 pass; TC-REST-07 implemented as proposed and flagged.

## Verification
`./gradlew :domain:test`

## Handoff log
