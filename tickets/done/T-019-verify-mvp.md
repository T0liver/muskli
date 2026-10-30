# T-019 — VERIFY: MVP acceptance on the owner's phone

- **Role:** VERIFY
- **Phase:** 1
- **Depends on:** T-015, T-016, T-017, T-018 (and their REVIEW sessions)
- **Refs:** AC-01..AC-06, AC-08 · SPEC §8

## Goal
Prove the MVP meets the quality bar in real use.

## In scope
- Build the debug/release-like APK, run AC-01..AC-06 and AC-08, record device, Android version, commit, results, screenshots, and measured cold-start time.
- File bug tickets for every failure; list UX frictions as tickets too.

## Out of scope (do NOT do)
- Fixing anything.

## Acceptance criteria
- [x] Every AC has a recorded pass/fail with evidence.
- [x] Owner decision recorded: ship as daily driver / fix list first. **Fix list first** (D-036).

## Handoff log
### 2026-10-30 — VERIFY
Recorded pass/fail for AC-01..AC-06 and AC-08 on the emulator. No product code changed.

**Tested commit:** `893cbd94b83d3d0e21f58399bb6c65e31570cf0a` (main HEAD).
**Device:** emulator-5554, emulator medium_phone, Android 16 (API 36), `sdk_gphone64_x86_64`, 1080×2400 at 420 dpi. The emulator replaces the owner's phone (owner-delegated 2026-10-30).
**Ship decision:** fix list first (D-036). AC-08 failed.

**Checks** (`JAVA_TOOL_OPTIONS=-Djavax.net.ssl.trustStoreType=Windows-ROOT`, `.\gradlew.bat :domain:test testDebugUnitTest assembleDebug lintDebug`): BUILD SUCCESSFUL in 1m 27s, exit 0, 129 tasks (10 executed, 119 up-to-date). Current test reports: 41 suites, 271 tests, 0 failures, 0 errors. `lintDebug` completed in that build.

| AC | Result | Evidence |
|---|---|---|
| AC-01 | Pass | Seed workout "Friday" (4 exercises, 12 typed sets) then empty workout "Prefill" using last-time values. Prefill set 1 was 100 kg and 5 reps with no typing (`crop-prefill-w.png`, `crop-prefill-r.png`); pull-up set 1 was 10 and 8 (`crop-pu-w.png`, `crop-pu-r.png`). Each of the 12 prefilled sets was one check tap. History: Prefill 5 min · 4,940 kg · 12 sets and Friday 17 min · 4,940 kg · 12 sets, same four exercises (`ac01-history.png`). |
| AC-02 | Pass | Completed a set, rest showed 1:28 (`ac02-rest-started.png`). `KEYCODE_SLEEP`: `mWakefulness=Asleep` for the whole wait. Alert at 20:28:04, about 90 s after 20:26:34. `dumpsys notification`: id 2, channel `rest_alert_sound_vibration`, importance 4, title "Rest over", text "Time for your next set", category alarm. `mSoundNotificationKey` names that notification (sound played). Channel vibration `[0, 400, 200, 400]`, vibration enabled. Not on the deviceidle whitelist. Emulator has no `vibrator` service, so the motor itself could not buzz. Emulator CPUs do not suspend. Countdown id 1 was up while asleep, then removed (`ac02-notif-during.txt`, `ac02-notif-after.txt`, `ac02-logcat.txt`). |
| AC-03 | Pass | Before force-stop (20:29:39): duration 5:25, rest 1:28, 1,730 kg, 3 sets, bench 80×8, drop 80×8, 75×6, empty set 4 (`ac03-before-kill.png`). After 8 s and relaunch (20:29:56): duration 5:41, rest 1:11 (both moved with the wall clock), same kg, sets, values, and empty set 4, opened on the workout (`ac03-after-reopen.png`). |
| AC-04 | Pass | `cmd connectivity airplane-mode enable` (`airplane_mode_on=1`, airplane icon). Finished the open workout as "Bench" (6 min · 1,730 kg · 3 sets) and did a new one, "Air" (squat 100×5, 500 kg), start to save, still in airplane mode. History kept both plus Friday and Prefill (`ac04-history.png`). No fatal log lines for the app. |
| AC-05 | Pass | Set 2 changed to drop set (`content-desc` "Set 2, drop set", marker D). Completing set 1 (80×8, 640 kg, 1 set) showed no Rest bar and no Skip. No `NotificationRecord` for `hu.toliver.muskli` (`ac05-after-complete.png`, `ac05-notifications.txt`). |
| AC-06 | Pass | Rest set to 75, body weight 82, custom exercise Zercher. Export through the app picker into Downloads: `muskli-backup-2026-10-30.json` (19,077 bytes), pulled to `build/verify/` and not committed. JSON: schema 1; Friday/Prefill 12 completed sets, Bench 3, Air 1; Zercher custom; `defaultRestSeconds` 75; `bodyWeightKg` 82. `pm clear`, import the same file, Replace: "Imported 4 workouts." Settings showed 75 and 82 (`ac06-imported.png`). History names and set counts matched (`ac06-history-after-import.png`). Friday detail matched the logged sets exactly: 100×5, 100×5, 90×5, 80×8, 80×8, 75×6, 140×5, 140×5, 120×3, +10×8, +10×8, +5×6 (`ac06-friday-detail.png`). Zercher still "Quadriceps · Barbell · Custom". Friday/Prefill volume read 6,934 kg after import versus 4,940 kg before body weight was set; the 1,994 kg gap is pull-up volume at 82 kg (D-017). The sets did not change. |
| AC-08 | Fail | After the import, airplane mode off. Three `force-stop` + `am start -W -n hu.toliver.muskli/.MainActivity`, all `LaunchState: COLD`: **6011 ms, 3743 ms, 3398 ms. Median 3743 ms** (bar is under 2000). Last launch's home showed Start workout (`ac08-ready.png`). Filed T-029. |

**Cold starts:** 6011, 3743, 3398 ms. Median **3743 ms**.

**Tickets filed:** T-029 (AC-08), T-030 (Add set is empty), T-031 (set fields hide their value from accessibility), T-032 (sub-minute workout stored as 0 min). Header overlap was reproduced (`ac01-twelve-logged.png`, rows drawn through the title and the volume line) and is already T-028; not filed again.

**Left as found:** airplane mode off (`airplane_mode_on=0`), `font_scale` 1.0, `cmd uimode night` = no, screen awake.

**Screenshots** (under `build/verify/`, not committed): `ac01-history.png` both gym-session workouts; `ac01b-prefilled.png` previous column filled; `ac02-rest-started.png` rest at 1:28; `ac02-awake.png` workout after the screen-off alert; `ac03-before-kill.png` / `ac03-after-reopen.png` draft and rest survived force-stop; `ac04-history.png` workouts saved in airplane mode; `ac05-after-complete.png` drop set, no rest; `ac06-exported.png` "Backup saved."; `ac06-imported.png` "Imported 4 workouts." with rest 75 and body weight 82; `ac06-friday-detail.png` sets after import; `ac08-ready.png` home after the slow cold start.
