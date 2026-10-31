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
- [x] `uiautomator dump` of a row showing 100 kg includes that number on the weight field's text or content description.
- [x] The complete-set control still has its own description.

## Verification
Log one set on the emulator, dump the hierarchy, and confirm the typed number is present. Check a prefilled row too.

## Notes / decisions needed
T-019 (2026-10-30): `SetRow` sets `semantics { contentDescription = ... }` on `BasicTextField`, and the dump then had `content-desc="Weight, set 1"` with an empty `text` while the screen showed 100. The previous column ("100 × 5") was exposed; the editable value was not.

## Handoff log
<!-- Newest first. Each entry: date, role, what was done, what's left, gotchas, commands run + results. -->
- 2026-10-31 UI: Branch `t-031-set-field-accessibility-value` from `main` 61ca7f5.
  - **Done:** `SetRow` builds each value field's content description as label + value through the new `cd_set_field_value` string (`%1$s, %2$s`), for example "Weight, set 1, 100". An empty field keeps the bare label ("Weight, set 1"). `CompleteButton`, the layout and the touch targets are unchanged, and no taps were added. No unit in the description, which follows the ticket's example. No owner question came up, so there is no DECISIONS row.
  - **Test:** new Robolectric test `LiveWorkoutScreenTest.setFieldsAnnounceTheirValueAndEmptyFieldsOnlyTheirName`. A prefilled row (80 × 8 from history) has "Weight, set 1, 80" and "Reps, set 1, 8". Typing 100 into an empty field gives "Weight, set 1, 100", the empty reps field stays "Reps, set 1", and "Complete set 1" is still there. Existing tests are unchanged and still pass. The existing test looks fields up by their empty-field name, which still matches exactly.
  - **Emulator (medium_phone, API 36, `uiautomator dump`):** the app opened on an existing draft (Barbell Back Squat, 10 sets).
    - Prefilled rows 8–10 (copied by T-030): `text="100" content-desc="Weight, set 8, 100"` and `text="5" content-desc="Reps, set 8, 5"`. The same holds for sets 9 and 10.
    - Logged set 1: typed 100 and 5 into the empty row, then tapped the check. The dump shows `content-desc="Weight, set 1, 100"`, `content-desc="Reps, set 1, 5"`, and `class="android.widget.CheckBox" content-desc="Complete set 1" checked="true"`. Volume went from 1,000 kg to 1,500 kg.
    - Empty set 2: `content-desc="Weight, set 2"` and `content-desc="Reps, set 2"`.
  - **Commands:** `.\gradlew.bat :domain:test testDebugUnitTest assembleDebug lintDebug` → BUILD SUCCESSFUL, 300 tests, 0 failures. Lint: 1 warning (`NewerVersionAvailable`, already there before), nothing new.
  - **Gotchas:** the dump has two nodes per field with the same bounds. One is the `android.widget.EditText` with `text="100" content-desc=""`. Its child is an `android.view.View` with `text="" content-desc="Weight, set 1, 100"`, because the `semantics` modifier on `BasicTextField` becomes its own node. T-019's "empty text" reading was probably the child node. The `EditText`'s `text` may already have held the value before this change; I did not rebuild `main` to check. Now both nodes carry the number. Not checked: a TalkBack listen-through. TalkBack might say the number twice ("100, Weight, set 1, 100") if it merges the child into the focused `EditText`. If so, a follow-up could move the description onto the `EditText` node; that's out of scope here. The emulator's test draft now has set 1 completed (100 × 5).
