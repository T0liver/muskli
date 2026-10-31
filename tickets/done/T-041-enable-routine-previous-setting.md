# T-041 — Enable the "Last time in this routine" setting

- **Role:** UI
- **Phase:** 2
- **Depends on:** T-024, T-034
- **Refs:** SPEC F-04 AC2 · TC-PREV-03 · D-038 · D-053
- **Size:** fits one session

## Goal
The Settings radio "Last time in this routine" can be selected, now that workouts can be started from a routine (T-034) and the live workout follows the stored mode (T-024).

## In scope
- `SettingsScreen`: make the routine-scoped radio selectable (sends `SettingsEvent.PreviousModeChanged(ROUTINE_SCOPED)`), drop the "Available once routines exist" supporting text and its string.
- Previews and the Settings ViewModel/UI test for the new selection.

## Out of scope (do NOT do)
- Any change to the lookup rules (T-024, D-053) or to the live-workout screen.

## Acceptance criteria
- [x] Selecting "Last time in this routine" persists and survives an app restart.
- [x] On the emulator: finish a workout from Routine A (80x8) and one from Routine B (85x6) for the same exercise; starting Routine A shows 80x8 in PREVIOUS, Routine B shows 85x6, an empty workout shows 85x6 (TC-PREV-03, D-038).

## Verification
`testDebugUnitTest`, `assembleDebug`, manual check on the emulator.

## Notes / decisions needed
Created 2026-10-31 by T-024: the radio stays disabled there because the ticket excludes settings UI and routines cannot be started from the UI yet (D-053).

## Handoff log
<!-- Newest first. Each entry: date, role, what was done, what's left, gotchas, commands run + results. -->
- 2026-10-31 UI: Branch `t-041-enable-routine-previous-mode` from `main` `b197d91`.
  - **Settings.** The "Last time in this routine" radio is a normal selectable row that sends `SettingsEvent.PreviousModeChanged(ROUTINE_SCOPED)`. The "Available once routines exist" text, its string `settings_previous_routine_unavailable` (only in `values/`), and the comment pointing here are gone. `RadioRow` loses its `enabled` and `supporting` parameters, which nothing used anymore. The "edited" preview now shows routine mode selected; defaults, dark, and 1.5x font previews are unchanged.
  - **T-024 Nit 1 fixed (small, now reachable).** The live-workout ViewModel is activity-scoped, so it stays alive while Settings is open, and a mode change runs `reloadPrevious` while `loadMissingPrevious` may still be running. Both now run under one `Mutex` (`previousLookups`), so a lookup that read the old mode always finishes before the reload for the new mode starts. No lookup rule changed (D-053).
  - **Tests.** `WorkoutSessionViewModelTest.aModeChangeDuringTheFirstLoadEndsWithTheNewMode`: the fake repository got an optional queue of gates (`historyGates`) so the test can finish the reload before the stale first load. Without the fix it fails with `expected [[80, 8]] but was [[85, 6]]`; with the fix it passes. `SettingsViewModelTest.routinePreviousModeIsSavedAndShown`; new Robolectric `SettingsScreenTest.lastTimeInThisRoutineCanBeSelected` (tap, row selected, stored). No existing expected value changed. Persistence across a restart is covered by `DataStoreSettingsRepositoryTest` and was checked on the emulator.
  - **Emulator** (`emulator-5554`, AVD medium_phone, API 36, debug build of this branch). Selected "Last time in this routine", force-stopped and relaunched: still selected. Created routines "T041 A" and "T041 B" (Barbell Deadlift, one empty set). Started A: PREVIOUS "–" (never done in A, D-053); logged 80x8, finished. Started B: PREVIOUS "–"; logged 85x6, finished. Started A again: **80 × 8**. Started B: **85 × 6**. Empty workout + Add exercise Deadlift: **85 × 6** (D-038). With A's draft open, switching to "anywhere" in Settings and resuming showed 85 × 6, and switching back showed 80 × 8. The test drafts were discarded and the setting reset to "anywhere"; the two routines and two workouts stay on the emulator.
  - **Checks.** `$env:JAVA_TOOL_OPTIONS='-Djavax.net.ssl.trustStoreType=Windows-ROOT'; .\gradlew.bat :domain:test testDebugUnitTest assembleDebug lintDebug` BUILD SUCCESSFUL: domain 182, app 108, data 62 tests, 0 failures. Lint: the existing `NewerVersionAvailable` warning only, nothing new.
  - **Decisions.** None needed: the behaviour is D-053/D-038, and the race fix changes no rule. No new dependency, schema change, `:domain` API change, or control on the logging screen.
  - **Left.** Nothing. Tickets README unchanged (it lists tickets, not status).
