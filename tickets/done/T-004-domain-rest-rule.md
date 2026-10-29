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
- [x] TC-REST-01..08 pass. TC-REST-07 expects None.

## Verification
`./gradlew :domain:test`

## Handoff log
### 2026-10-29 — REVIEW
Owner review: no findings (Blocker / Should-fix / Nit: none). The not-completed guard stays. Verification is `:domain:test`, already green. Moved to `done/`.

### 2026-10-29 — BUILD
**Done.** `:domain/calc/RestTimer.kt` adds `sealed interface RestEffect { Start(seconds), None }` and `restTimerEffect(completed, next, exerciseRestS, defaultRestS)`, matching the ARCHITECTURE §7 signature. The order of the checks:
1. Return None if `next` is null (last set of the workout, D-018), if the next set is a drop set, or if `completed` isn't marked completed.
2. Use the exercise rest if set, otherwise the default. 0, a negative value, or both null means None.

**Caller contract:** `next` is the next set in workout order, across exercises and supersets. `exerciseRestS` is the rest for the exercise of the set just completed (D-012), which is what TC-REST-06 and TC-REST-08 exercise. The `CompleteSet` use case (T-012/T-015) has to pass those values.

**Gotcha:** the not-completed guard (and its extra test) isn't in a TC. It is there so the function is never a no-op on its `completed` argument. Remove it if the owner prefers otherwise.

**Verified:** `.\gradlew.bat :domain:test assembleDebug` → BUILD SUCCESSFUL, 48 domain tests, 0 failures (RestTimer 9). The tests failed to compile before the implementation existed.

**Left:** nothing in scope. **Open questions:** none.

- 2026-10-28 DOCS: Owner pinned D-018. TC-REST-07 is None; TC-REST-08 is in scope. No confirmation flag.
