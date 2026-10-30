# T-030 — Adding a set copies the row above

- **Role:** BUILD
- **Phase:** 1
- **Depends on:** T-014
- **Refs:** AC-01 · SPEC §8
- **Size:** fits one session

## Goal
"Add set" starts from the weight, reps, and other values of the set above it, so a repeated set is one tap to complete.

## In scope
- `AddSetUseCase` copies the previous set's value fields (not its completion) into the new normal set. A first set stays empty.
- Unit tests for copy, for the first set, and for not marking the copy completed.

## Out of scope (do NOT do)
- Changing how last-time prefills work when an exercise is added (that already copies the whole previous session).
- Drop-set or warm-up shortcuts.

## Acceptance criteria
- [ ] On an exercise whose latest set is 100 kg × 5, Add set shows 100 and 5, uncompleted.
- [ ] The new set is normal, and completing it is the check tap only.
- [ ] An exercise with no sets still gets one empty normal set.

## Verification
`:domain:test` for the new cases. On the emulator, add a second set and confirm the fields match the row above without typing.

## Notes / decisions needed
T-019 seed workout (2026-10-30): every Add set was an empty row, so each extra set needed the weight and the reps typed again. Hevy copies the row above. Last-time prefill on Add exercise is separate and did work (AC-01).

## Handoff log
<!-- Newest first. Each entry: date, role, what was done, what's left, gotchas, commands run + results. -->
