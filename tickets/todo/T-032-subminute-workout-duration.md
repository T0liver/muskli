# T-032 — A short workout keeps its real duration

- **Role:** BUILD
- **Phase:** 1
- **Depends on:** T-016
- **Refs:** AC-01 · AC-04 · D-033
- **Size:** fits one session

## Goal
Finishing a workout that lasted under a minute stores that elapsed time, and History does not show 0 min.

## In scope
- The duration written at finish is the elapsed time from the stopwatch when the user has not edited the duration fields.
- History shows that duration (seconds are fine when it is under a minute).
- A unit test: a workout shorter than 60 s does not get `end == start`.

## Out of scope (do NOT do)
- Changing the finish screen's hour/minute editors for workouts the user types by hand, or past-workout logging.

## Acceptance criteria
- [ ] Finish a workout about 30 s long without editing duration. History does not say 0 min, and the stored end is after the start by about that much.
- [ ] A workout of several minutes still saves the minute duration it shows on the finish screen.

## Verification
On the emulator, start a workout, complete one set, finish immediately, and read the history line. Confirm in the backup JSON that `endTimeMs` is greater than `startTimeMs`.

## Notes / decisions needed
T-019 backup `muskli-backup-2026-10-30.json`, workout "Air": `startTimeMs == endTimeMs` (0 s) after a real set was logged (history: "0 min · 500 kg · 1 set"). Friday, Prefill, and Bench, all over a minute, stored 1020 s, 300 s, and 360 s. The finish screen only edits whole minutes, so a sub-minute workout is saved as 0.

## Handoff log
<!-- Newest first. Each entry: date, role, what was done, what's left, gotchas, commands run + results. -->
