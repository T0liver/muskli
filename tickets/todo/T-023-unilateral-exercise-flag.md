# T-023 — Unilateral flag on exercises

- **Role:** BUILD
- **Phase:** 2
- **Depends on:** T-006, T-010
- **Refs:** D-014, D-022, D-026 · TC-VOL-09
- **Size:** fits one session

## Goal
Volume knows which exercises are unilateral, so `setVolumeKg(..., unilateral = true)` is passed for them and a `both` set counts 2× (D-014, D-026).

## In scope
- Decide (owner) where the flag lives: exercise template field, catalogue field (`docs/CATALOGUE.md`), or both.
- Wire it from the template into every volume caller.

## Out of scope (do NOT do)
- Left/right logging UI (D-022 keeps it out of the MVP).

## Acceptance criteria
- [ ] Owner decision recorded in `docs/DECISIONS.md`.
- [ ] Volume for a unilateral exercise doubles `both` sets; bilateral exercises are unchanged.

## Verification
`./gradlew :domain:test testDebugUnitTest`

## Notes / decisions needed
Needs owner approval for any schema or catalogue format change. Owner, 2026-10-30: **both** (D-037); the schema and catalogue changes are approved. Custom exercises get a unilateral toggle in the custom-exercise editor.

## Handoff log
- 2026-10-29 BUILD (T-002): Created. `:domain` already accepts the flag; nothing supplies it yet, so every caller currently gets `false`.
