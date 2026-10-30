# T-015 — Rest timer wired in

- **Role:** BUILD
- **Phase:** 1
- **Depends on:** T-004, T-009 (D-015 decided), T-013, T-017
- **Refs:** SPEC F-05 · AC-02, AC-03, AC-05 · docs/ARCHITECTURE.md §5

## Goal
Completing a set starts the rest timer per the rule; the alert fires with the screen off; state survives process death.

## In scope
- Implement the mechanism approved in D-015; in-app timer bar (+/-15 s, skip); notification with the same actions; sound/vibration per settings.
- `restTimerEndsAt` persisted in the draft; UI derives remaining time from the clock.

## Out of scope (do NOT do)
- Superset semantics, per-exercise rest editing UI beyond what T-013 provides.

## Acceptance criteria
- [x] No timer after completing the set before a drop set (AC-05).
- [x] Alert fires with the screen off on the owner's phone (AC-02) and after force-stop/reopen the remaining time is correct (AC-03).

## Handoff log
- 2026-10-30: Owner ran the AC-02 and AC-03 phone steps and confirmed everything works as intended. Phone model and Android version were not reported. Moved to `done/`.
- 2026-10-30 BUILD: Implemented. AC-05 is covered by tests. AC-02 and AC-03 pass on the API 36 emulator; the owner's phone run is still to do. Moved to `review/`.
  - Taken while T-009 is in `verify/`: its precondition, D-015 decided, holds, and the owner asked for T-015.
  - **Owner decisions (D-032):** schema v3 with nullable `workout.rest_ends_at` and `workout.rest_duration_s`. A None from completing a set cancels a running rest. −15 s with 15 s or less left ends the rest silently. Un-ticking keeps the rest.
  - **`:domain`:** `Workout.restTimer: RestTimer?` (end time and length). `RestTimer.remainingMs(now)` and `fractionLeft(now)` derive time left from `Clock`. `CompleteSetUseCase` stores the effect in the same transaction. New `AdjustRestTimerUseCase`, `SkipRestTimerUseCase`, and `FinishRestTimerUseCase`; the last is a compare-and-clear on the end time, so the service and the alarm can't both alert. TC-REST-07 stays None (D-018).
  - **`:data`:** `MIGRATION_2_3`, exported `3.json`, migration test, and the draft round-trip now includes the rest.
  - **`:app`:** `service/` adds four pieces. `RestTimerSync` starts in `Application.onCreate`; it observes the draft's rest, sets `setAlarmClock` (falling back to `setAndAllowWhileIdle` only if exact alarms are denied), and starts the service. `RestTimerService` (`specialUse`) shows a chronometer countdown notification with −15 s, +15 s, and Skip, and fires on time through a handler. `RestAlarmReceiver` is the backup. `RestActionReceiver` handles the notification buttons. The alert uses four channels, one per sound/vibration combination from Settings, because a channel's sound can't change after it is created. Platform notification APIs only; no new dependency. The manifest adds `POST_NOTIFICATIONS`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_SPECIAL_USE`, `USE_EXACT_ALARM`, and `SCHEDULE_EXACT_ALARM` (maxSdk 32). `POST_NOTIFICATIONS` is requested on "Start workout". The live screen's bar is wired to the ViewModel and is shown only while a rest runs.
  - **Gotchas:** An ended rest that is still stored is left alone (`RestSync.Leave`), never cancelled, so an alarm that is firing still alerts. After a force-stop, Android cancels the alarm; if the rest ended meanwhile, there is no late alert on reopen and the stale value is ignored. The `specialUse` type is passed only on API 34+ (lint InlinedApi). The first `:data` test run after a schema bump fails because `3.json` is generated during that build; run it again.
  - **Not tested:** tapping the notification buttons on a device (the receiver isn't exported, so adb can't reach it). The use cases behind them are unit-tested.
  - **Commands:** `.\gradlew.bat :domain:test testDebugUnitTest assembleDebug lintDebug`: BUILD SUCCESSFUL, 220 JVM tests, 0 failures. Lint: only the existing `NewerVersionAvailable`. Emulator (API 36 `medium_phone`): the FGS ran as `types=0x40000000`, with an exact alarm (`policy_permission`) and a countdown notification with 3 actions. With the screen asleep, "Rest over" was posted once on `rest_alert_sound_vibration` and the service stopped. Force-stop at 1:28, then reopen 20 s later: the bar read 1:00, and the alarm and FGS were back. Skip cleared the alarm, the service, and the bar.
  - **Owner phone steps.** Build with `git switch t-015-rest-timer-wired`, then `.\gradlew.bat assembleDebug` (set `JAVA_TOOL_OPTIONS` as in AGENTS §5). Install `app\build\outputs\apk\debug\app-debug.apk` over the current app; migration 2→3 keeps your data. Uninstall "Muskli T-009" if it is still there. Unplug USB, turn DND and silent mode off, leave battery settings at their defaults. In Muskli Settings: sound and vibration on, default rest 90 s.
    - **AC-02:**
      1. Start a workout and allow notifications if asked. If notifications were denied before: phone Settings → Apps → Muskli → Notifications → on.
      2. Add one exercise and give it at least 3 sets (the last set of the workout starts no rest, D-018). Tick set 1. Check that the bar shows 1:30 with −15 s/+15 s/Skip, a "Rest" countdown notification appears, and there is an alarm icon in the status bar.
      3. Lock the phone, put it face down, and don't touch it for 2 minutes. Pass: sound and vibration at about 90 s, and "Rest over" on the lock screen.
      4. Unlock and tick set 2. Pull down the shade, tap +15 s on the countdown (it jumps by 15 s), then Skip (the notification and alarm icon go away and nothing fires).
    - **AC-03:**
      1. Add a set and tick a set; note the time shown (about 1:30).
      2. Phone Settings → Apps → Muskli → Force stop. The countdown disappears; that is expected.
      3. Wait about 20 s and open Muskli from the launcher. Pass: it opens on the workout, the sets are unchanged, the stopwatch kept counting, the bar shows about 1:10, and the countdown notification is back.
      4. Lock the phone. Pass: it alerts at the original end time.
