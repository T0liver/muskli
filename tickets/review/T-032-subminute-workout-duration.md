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
- [x] Finish a workout about 30 s long without editing duration. History does not say 0 min, and the stored end is after the start by about that much.
- [x] A workout of several minutes still saves the minute duration it shows on the finish screen.

## Verification
On the emulator, start a workout, complete one set, finish immediately, and read the history line. Confirm in the backup JSON that `endTimeMs` is greater than `startTimeMs`.

## Notes / decisions needed
T-019 backup `muskli-backup-2026-10-30.json`, workout "Air": `startTimeMs == endTimeMs` (0 s) after a real set was logged (history: "0 min · 500 kg · 1 set"). Friday, Prefill, and Bench, all over a minute, stored 1020 s, 300 s, and 360 s. The finish screen only edits whole minutes, so a sub-minute workout is saved as 0.

## Handoff log
<!-- Newest first. Each entry: date, role, what was done, what's left, gotchas, commands run + results. -->
- 2026-10-31: BUILD.
  - **Decision:** D-051 (auto-accepted recommendation, owner-delegated 2026-10-30). Under a minute with the editors still at 0: save the exact elapsed whole seconds, at least 1 s (not rounded up to 1 min). Otherwise the typed minutes are saved, as before. History shows "N s" for 1–59 s; 0 s stays "0 min". No schema change, no new dependency.
  - **`:domain`:** new pure `finishDurationS(elapsedMs, typedS)` in `FinishAndHistoryUseCases.kt`. `WorkoutDetails` and `FinishWorkoutUseCase` are unchanged. Four tests in `FinishAndHistoryUseCasesTest` were written first; with a stub returning `typedS`, 2 failed (the 30 s and 400 ms cases), then all passed after the implementation. One of them finishes a 30.4 s draft through `FinishWorkoutUseCase` with a `TestClock` and checks end = start + 30 s.
  - **`:app`:** `FinishViewModel` keeps the elapsed ms measured with the injected `Clock` when the screen opens, and passes the typed duration through `finishDurationS`. New `FinishViewModelTest.aWorkoutUnderAMinuteSavedUneditedKeepsItsSeconds`. `durationText` in `HistoryComponents.kt` gains the seconds case (new string `duration_s`). It is also used by the workout detail screen.
  - **Gotcha:** edited past workouts follow the same rule, so saving a 30 s past workout without touching the duration keeps 30 s instead of truncating to 0. Workouts already stored as 0 s (like "Air") still show "0 min"; nothing is migrated.
  - **Commands:** `.\gradlew.bat :domain:test testDebugUnitTest assembleDebug lintDebug` all pass. Lint: 1 issue, the existing `NewerVersionAvailable`, none in touched files.
  - **Emulator (medium_phone, API 36, emulator-5554):** an old draft from earlier sessions (9 h 4 min, 1 completed set) was saved through Finish: the finish screen showed 9 h / 4 min, History "9 h 4 min · 68,100 kg · 4 sets", stored end − start = 32,640,000 ms (AC2). Then Start workout → Bench Press → 100 kg × 5 → complete → Finish showed 0 h / 0 min → Save without editing. History: "53 s · 500 kg · 1 set". The `workout` row read with `run-as` + sqlite3: `end_time − start_time = 53,000` ms (AC1). This run took 53 s instead of about 30 s because of adb round trips.
  - **Not verified:** a backup JSON export was not run (SAF picker). The backup writes `endTimeMs` from the same `end_time` column that was checked directly.
  - **Left:** nothing in scope.
