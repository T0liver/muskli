# T-031 — Set fields expose the number that was typed

- **Role:** UI
- **Phase:** 1
- **Depends on:** T-013
- **Refs:** AC-01
- **Size:** fits one session

## Goal
TalkBack and the accessibility tree announce the weight, reps, and other values in a set row, not only the field name.

## In scope
- The set-row text fields keep a content description that includes the current value (for example "Weight, set 1, 100").
- An empty field still announces the field name.

## Out of scope (do NOT do)
- A new TalkBack pass over the whole app, or changing the visible layout.

## Acceptance criteria
- [ ] `uiautomator dump` of a row showing 100 kg includes that number on the weight field's text or content description.
- [ ] The complete-set control still has its own description.

## Verification
Log one set on the emulator, dump the hierarchy, and confirm the typed number is present. Check a prefilled row too.

## Notes / decisions needed
T-019 (2026-10-30): `SetRow` sets `semantics { contentDescription = ... }` on `BasicTextField`, and the dump then had `content-desc="Weight, set 1"` with an empty `text` while the screen showed 100. The previous column ("100 × 5") was exposed; the editable value was not.

## Handoff log
<!-- Newest first. Each entry: date, role, what was done, what's left, gotchas, commands run + results. -->
