# T-004 — Domain: rest-timer rule

- **Role:** BUILD
- **Phase:** 0
- **Depends on:** T-001
- **Refs:** TC-REST-01..08 · SPEC F-05 · D-012, D-018

## Goal
Pure function `restTimerEffect(...)` returning `Start(seconds)` or `None` per the rules (exercise rest, default rest, off, no start before a drop set, none after the last set, superset uses the completed exercise's rest).

## In scope
- Tests first from TC-REST-01..08; sealed `RestEffect` type.
- TC-REST-07 is **None** (D-018). TC-REST-08 uses the completed exercise's rest (D-012).

## Out of scope (do NOT do)
- Any timer, service, notification, or UI code.

## Acceptance criteria
- [ ] TC-REST-01..08 pass. TC-REST-07 expects None.

## Verification
`./gradlew :domain:test`

## Handoff log
- 2026-10-28 DOCS: Owner pinned D-018. TC-REST-07 is None; TC-REST-08 is in scope. No confirmation flag.
