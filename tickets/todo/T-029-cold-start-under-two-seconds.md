# T-029 — Cold start to ready-to-log under 2 seconds

- **Role:** BUILD
- **Phase:** 1
- **Depends on:** T-019
- **Refs:** AC-08 · SPEC §8
- **Size:** fits one session

## Goal
A cold start of the debug build reaches the home screen in under 2 seconds.

## In scope
- Find what makes `am start -W` TotalTime exceed 2 s after the app has already been opened once, and remove that work from the startup path.
- Keep the bundled catalogue load and a failed network sync from blocking first frame.

## Out of scope (do NOT do)
- A new splash screen, lazy feature flags, or measuring on a physical phone.

## Acceptance criteria
- [ ] On emulator medium_phone, Android 16 (API 36), three `am force-stop hu.toliver.muskli` then `am start -W -n hu.toliver.muskli/.MainActivity` runs are all `LaunchState: COLD`, and the median TotalTime is under 2000 ms.
- [ ] The home screen shows Start workout (or Resume if a draft exists) when that start returns.

## Verification
Repeat the three-start measurement above with data already in the database. Record each TotalTime.

## Notes / decisions needed
Found in T-019 (2026-10-30), commit `893cbd94`, emulator-5554. Official trio after workouts were imported: 6011 ms, 3743 ms, 3398 ms (median 3743). Incidental cold starts the same day: 2107 ms (first launch after install), 3966 ms (first launch after `pm clear`), 2541 ms (reopen onto a live draft). Home did show Start workout, so the window was the logger, just late.

## Handoff log
<!-- Newest first. Each entry: date, role, what was done, what's left, gotchas, commands run + results. -->
