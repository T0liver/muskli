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
- 2026-10-31: VERIFY. **Pass.**
  - **Build:** `app-debug.apk` from `5e08a7b` (code unchanged at `cb9a776`, review log only), `adb install -r` OK.
  - **Device:** emulator `medium_phone`, Android 16 (API 36), `emulator-5554`.
  - **AC1, under a minute:** Start workout → add an exercise (Barbell Back Squat, prefilled 4 sets) → complete set 1 → Finish after about 7 s. The finish screen showed 0 h / 0 min; Save without editing the duration → "Discard and save" for the 3 uncompleted sets. History: "7 s · 500 kg · 1 set" (not "0 min"). `workout` row (DB copied with `adb exec-out run-as … cat`, read with Python sqlite3 on the host, since the image has no `sqlite3`): `end_time − start_time = 7,000` ms. Pass.
  - **AC2, several minutes:** Start workout → Barbell Bench Press 100 kg × 5 → complete → Finish after about 3 min 20 s. The finish screen showed 0 h / 3 min; Save unedited. History: "3 min · 500 kg · 1 set"; stored `end_time − start_time = 180,000` ms. Pass.
  - **Not verified:** the backup JSON export (SAF picker); it writes `endTimeMs` from the `end_time` column checked above.
  - **Evidence:** `build/verify/T-032/` (git-ignored): `long-02-finish.png`, `long-03-history.png`, `short-01-set.png`, `short-02-finish.png`, `short-03-history.png`, DB copy `muskli.db*`.
- 2026-10-31: REVIEW of `5e08a7b` (`git diff main...t-032-subminute-workout-duration`).
  - **Scope:** matches the ticket and D-051. A workout under a minute with the editors at 0 saves `elapsedMs / 1000` (at least 1 s); otherwise `finishDurationS` returns the typed seconds unchanged, so a multi-minute workout keeps the shown minutes. The hour/minute editors, their validation (`durationS()`), `WorkoutDetails`, and `FinishWorkoutUseCase` are untouched. Log past creates a 1 h workout (D-033), so its elapsed time is ≥ 1 min and the typed duration is saved as before. No schema change, no dependency, no change to the logging screen.
  - **Tests:** only additions (4 in `FinishAndHistoryUseCasesTest`, 1 in `FinishViewModelTest`); no existing expected value edited. Logic is a pure `:domain` function; the ViewModel only measures elapsed time with the injected `Clock`. New string in `strings.xml`.
  - **Blocker:** none.
  - **Should-fix:** none.
  - **Nit:** `durationText`'s new "N s" branch has no unit test (it is a `@Composable`; checked on the emulator instead).
  - **Nit:** the first domain test sets `clock.now` only to compute `elapsedMs`; passing the literal 30 400 would read the same. Left as is.
  - **Nit:** changing the start in the finish form of a sub-minute workout does not change the saved seconds (they come from the opening time). That matches D-051 ("measures the elapsed time when it opens"), and the duration was already independent of the start.
  - **Commands:** `.\gradlew.bat :domain:test testDebugUnitTest assembleDebug lintDebug`: BUILD SUCCESSFUL; reran tests with `--rerun`: 305 tests, 0 failures across `:domain`, `:data`, `:app`. Lint: 0 errors, 1 warning (existing `NewerVersionAvailable`).
  - **Result:** no fixes needed; moved to `verify/`.
- 2026-10-31: BUILD.
  - **Decision:** D-051 (auto-accepted recommendation, owner-delegated 2026-10-30). Under a minute with the editors still at 0: save the exact elapsed whole seconds, at least 1 s (not rounded up to 1 min). Otherwise the typed minutes are saved, as before. History shows "N s" for 1–59 s; 0 s stays "0 min". No schema change, no new dependency.
  - **`:domain`:** new pure `finishDurationS(elapsedMs, typedS)` in `FinishAndHistoryUseCases.kt`. `WorkoutDetails` and `FinishWorkoutUseCase` are unchanged. Four tests in `FinishAndHistoryUseCasesTest` were written first; with a stub returning `typedS`, 2 failed (the 30 s and 400 ms cases), then all passed after the implementation. One of them finishes a 30.4 s draft through `FinishWorkoutUseCase` with a `TestClock` and checks end = start + 30 s.
  - **`:app`:** `FinishViewModel` keeps the elapsed ms measured with the injected `Clock` when the screen opens, and passes the typed duration through `finishDurationS`. New `FinishViewModelTest.aWorkoutUnderAMinuteSavedUneditedKeepsItsSeconds`. `durationText` in `HistoryComponents.kt` gains the seconds case (new string `duration_s`). It is also used by the workout detail screen.
  - **Gotcha:** edited past workouts follow the same rule, so saving a 30 s past workout without touching the duration keeps 30 s instead of truncating to 0. Workouts already stored as 0 s (like "Air") still show "0 min"; nothing is migrated.
  - **Commands:** `.\gradlew.bat :domain:test testDebugUnitTest assembleDebug lintDebug` all pass. Lint: 1 issue, the existing `NewerVersionAvailable`, none in touched files.
  - **Emulator (medium_phone, API 36, emulator-5554):** an old draft from earlier sessions (9 h 4 min, 1 completed set) was saved through Finish: the finish screen showed 9 h / 4 min, History "9 h 4 min · 68,100 kg · 4 sets", stored end − start = 32,640,000 ms (AC2). Then Start workout → Bench Press → 100 kg × 5 → complete → Finish showed 0 h / 0 min → Save without editing. History: "53 s · 500 kg · 1 set". The `workout` row read with `run-as` + sqlite3: `end_time − start_time = 53,000` ms (AC1). This run took 53 s instead of about 30 s because of adb round trips.
  - **Not verified:** a backup JSON export was not run (SAF picker). The backup writes `endTimeMs` from the same `end_time` column that was checked directly.
  - **Left:** nothing in scope.
