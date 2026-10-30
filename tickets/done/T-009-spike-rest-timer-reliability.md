# T-009 — SPIKE: rest timer reliability on Android

- **Role:** SPIKE
- **Phase:** 1
- **Depends on:** T-001
- **Refs:** D-015 · SPEC F-05 · AC-02 · docs/ARCHITECTURE.md §5

## Goal
Choose a mechanism that fires the rest-timer alert reliably with the screen off and the app backgrounded, on the owner's phone.

## In scope
- Read the current Android documentation on foreground services, exact alarms, notification permission, and battery optimization (cite pages; do not rely on memory, rules change by Android version).
- Build a throwaway prototype (kept on a branch, not merged) for the 1–2 most promising approaches; test on the owner's phone: lock screen, 90 s timer, 5 repetitions each, with default battery settings.
- Report: reliability table, permissions/user-visible side effects, complexity, battery impact. **Validate D-015** (FGS + exact-alarm backup); note any required tweak for owner approval.

## Out of scope (do NOT do)
- Merging prototype code; polishing UI; any other feature.

## Acceptance criteria
- [x] Measured results on the real device (not emulator only).
- [x] Report confirms D-015 combo or documents required change for owner approval.

## Verification
Phone run of the prototype on branch `spike/t-009-rest-timer-prototype` (launcher icon "Muskli T-009"). Steps are in the newest Handoff log entry. Paste each series' "Copy log" output into the reliability table below.

## Findings

### Sources (Android Developers, fetched 2026-10-29)
| Topic | Page | Last updated |
|---|---|---|
| FGS types (`shortService`, `specialUse`, `health`) | https://developer.android.com/develop/background-work/services/fgs/service-types | 2026-10-28 |
| FGS background-start restrictions and exemptions | https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start | 2026-10-28 |
| Launching an FGS | https://developer.android.com/develop/background-work/services/fgs/launch | 2026-10-28 |
| FGS timeouts | https://developer.android.com/develop/background-work/services/fgs/timeout | (no date shown) |
| Scheduling alarms (exact vs inexact, permissions) | https://developer.android.com/develop/background-work/services/alarms/schedule | 2026-10-28 |
| Exact alarms denied by default (Android 14) | https://developer.android.com/about/versions/14/changes/schedule-exact-alarms | 2026-03-03 |
| `POST_NOTIFICATIONS` runtime permission | https://developer.android.com/develop/ui/views/notifications/notification-permission | 2026-10-28 |
| Doze, App Standby, battery optimisation | https://developer.android.com/training/monitoring-device-state/doze-standby | 2026-10-22 |

### What the docs say that matters here
- **An FGS keeps the process alive. It does not keep the CPU awake.** In Doze the system "ignores wake locks" and defers jobs and standard alarms (Doze page). A timer inside the service (`Handler.postDelayed` uses uptime) can be late when the CPU suspends with the screen off. Mode D of the prototype measures this.
- **Exact alarms are the documented way to fire on time with the screen off.** Doze defers `setExact()`. `setExactAndAllowWhileIdle()` fires in Doze, but "can't fire alarms more than once per nine minutes, per app" there (Doze page). `setAlarmClock()` alarms "continue to fire normally. The system exits Doze shortly before those alarms fire". Its cost is that they are "highly visible to users", and the system never adjusts their delivery time (alarms page).
- **Exact alarm permission.** `SCHEDULE_EXACT_ALARM` is denied by default for new installs on Android 14+ and is user-revocable ("Alarms & reminders"); revoking it cancels all exact alarms. `USE_EXACT_ALARM` is granted at install and is not revocable, but Play only allows publishing with it for alarm-clock and calendar apps. Muskli is sideloaded, so Play policy does not apply today.
- **FGS type.** Since Android 14 every FGS needs a type.
  - `shortService` is limited to about 3 minutes, can't be sticky, and can't be extended from the background. That is too tight for long rests or a series of rests.
  - `health` needs a sensor or `ACTIVITY_RECOGNITION` permission.
  - `specialUse` needs only `FOREGROUND_SERVICE_SPECIAL_USE` plus a free-text subtype, with no runtime permission. Play reviews that text, which doesn't matter while the app is sideloaded.
- **Starting an FGS.** Apps targeting Android 12+ can't start an FGS from the background, with exemptions. Two cover us: "the user performs an action on a UI element" (a set tap in the open app or a notification action) and "your app invokes an exact alarm".
- **Notifications.** On Android 13+ `POST_NOTIFICATIONS` is a runtime permission, and notifications are off by default on a new install. Without it the FGS notice only shows in Task Manager and the rest-over alert is not shown at all. Starting an FGS does not need the permission.
- **Battery optimisation.** Exemption is a user setting. Requesting it directly (`ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`) is restricted by Play policy unless Doze breaks the app's core function. Exempt apps are also allowed exact alarms and background FGS starts. AC-02 requires default settings, so the prototype doesn't ask for an exemption.

### Prototype (branch `spike/t-009-rest-timer-prototype`, never merge)
Package `hu.toliver.muskli.spike`, separate launcher icon, platform APIs only (no new dependencies). It runs a series of N timers back to back: each timer's fire starts the next. Every fire is logged with lateness against the target wall-clock time, which path fired it, screen state, and Doze state.
- **A. Combo (D-015):** `specialUse` FGS with a countdown notification and a Handler, plus `setAlarmClock()`. Whichever fires first wins; the other is ignored.
- **B. `setAlarmClock()` only:** no service.
- **C. `setExactAndAllowWhileIdle()` only:** no service.
- **D. FGS + Handler only, no alarm:** control.
- Permissions: `POST_NOTIFICATIONS`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_SPECIAL_USE`, `USE_EXACT_ALARM`, and `SCHEDULE_EXACT_ALARM` (maxSdk 32). The alert uses an IMPORTANCE_HIGH channel with vibration and the default sound.

### Reliability table
"Felt" means the owner noticed sound or vibration with the screen off. Lateness is the fire time minus the target, from the prototype log.

**Owner's phone, 2026-10-29.** Samsung SM-S916B, Android 16 (API 36), unplugged, screen off, 90 s × 5. Battery optimisation "optimised (default)". Battery saver was **on** (stricter than AC-02's default). Notifications and exact alarms allowed.
| Mode | Runs | Felt (screen off) | Lateness median / max | Fired by | Doze at fire | Notes |
|---|---|---|---|---|---|---|
| A. combo | 5/5 | yes, buzz each round; countdown notification ran | +1 / +11 ms | fgs-handler | no, light, no, light, no | Never deep Doze |
| B. `setAlarmClock` only | 5/5 | yes; owner: it worked | +947 / +1050 ms | alarm-clock | no, light, no, no, no | Also kept firing after the app was closed (the Recents check; no separate 120 s log) |
| C. `setExactAndAllowWhileIdle` only | 5/5 | alert posted each round | +986 / +1551 ms | exact-idle | no, then deep ×4 | 90 s repeats inside deep Doze. The documented 9-minute cap did not apply on this phone |
| D. FGS + Handler only | failed | no | — | — | — | Countdown ran into negative numbers and never stopped. No log lines, so the in-process timer never fired |

**Emulator smoke test (API 36 `medium_phone`; not evidence for AC-02, because emulator CPUs don't suspend):**
| Mode | Runs | Lateness | Fired by | Doze at fire | Notes |
|---|---|---|---|---|---|
| A. combo, 20 s × 3, screen off | 3/3 | +1 to +2 ms | fgs-handler (the alarm came ~10 ms later and was correctly ignored) | no | FGS reported as `isForeground=true types=0x40000000` (specialUse) |
| B. `setAlarmClock`, 20 s, 3 single runs, forced Doze attempted | 3/3 | +6 to +10 ms | alarm-clock | no | `deviceidle force-idle` refused: "Unable to go deep idle; stopped at INACTIVE" while the alarm clock was pending |
| C. `setExactAndAllowWhileIdle`, 20 s × 1, forced Doze | 1/1 | +7 ms | exact-idle | deep | Delivered inside deep Doze |

On the emulator, a pending `setAlarmClock()` stopped the device from entering deep Doze at all. That matches the doc's "the system exits Doze shortly before those alarms fire". My reading, not verified in a doc: the platform holds off deep idle while an alarm clock is near. Battery cost: deep Doze is blocked for the length of each rest. During a workout the phone is picked up every few minutes anyway.

### Permissions and user-visible side effects
| Item | Combo (A) | Alarm-clock only (B) |
|---|---|---|
| Runtime prompts | `POST_NOTIFICATIONS` once (Android 13+) | same |
| Special access | none with `USE_EXACT_ALARM` (sideload). With `SCHEDULE_EXACT_ALARM` the user must enable "Alarms & reminders" in Settings | same |
| Manifest-only | `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_SPECIAL_USE` | none extra |
| Visible while resting | alarm-clock icon in the status bar; "next alarm" on lock screen and clock widgets; ongoing countdown notification (dismissible on Android 14+) | alarm-clock icon and "next alarm"; countdown only if posted as a normal ongoing notification |
| Battery | no wake locks; one alarm per rest; no deep Doze during a pending rest | same |
| Code size (prototype) | service + receiver + alarm, ~300 LOC | receiver + alarm, about 2/3 of that |

### Verdict on D-015
**D-015 stands: foreground service plus `setAlarmClock` backup.** Recorded in `docs/DECISIONS.md`. Neither half is enough on its own.

The in-process timer only stayed on time while an alarm clock was also pending (A never entered deep Doze, and the handler won by a few milliseconds). With no alarm (D), the CPU slept, the handler never ran, and the system countdown — which uses wall clock, not the process — ran past zero. The alarm alone (B, C) still buzzed, about a second late, and B survived the app being closed. So T-015 sets both on every rest: the service for the on-time fire and the live countdown, the alarm so a sleeping or dead process still alerts.

Owner choices already made, and now recorded: `USE_EXACT_ALARM` while the app is sideloaded (switch to `SCHEDULE_EXACT_ALARM` if it is ever published on Play); `POST_NOTIFICATIONS` when the first workout starts, before any set is logged. Service type `specialUse` (not `shortService`: its cap is about 3 minutes). No battery-optimisation exemption. `setExactAndAllowWhileIdle` is not the backup: the docs cap it at one fire per 9 minutes per app in Doze, even though this Samsung delivered one every 90 s.

## Handoff log
- 2026-10-30 VERIFY: Pass. Both acceptance criteria hold. Moved to `done/`. Build `26453bc` on `t-009-rest-timer-reliability` (fast-forwarded onto main). `spike/t-009-rest-timer-prototype` (`70050c1`) is not an ancestor of main (`git merge-base --is-ancestor` exit 1) and stays unmerged.
  - AC (a) measured results: the reliability table is filled for every mode on the owner's phone, Samsung SM-S916B, Android 16 (API 36), 2026-10-29, unplugged, screen off, 90 s × 5, battery optimisation default, battery saver on. A 5/5 felt, +1 / +11 ms, fgs-handler. B 5/5 felt, +947 / +1050 ms, alarm-clock (also fired after the app was closed). C 5/5 alert posted, +986 / +1551 ms, exact-idle, including deep Doze. D failed: countdown went negative, no alert, no log. Not re-measured this session; the recorded phone log is the evidence.
  - AC (b) D-015: `docs/DECISIONS.md` D-015 is **decided** and confirms the combo. `docs/ARCHITECTURE.md` §5 matches (`specialUse` foreground service plus `setAlarmClock` on every rest). `tickets/done/T-015-rest-timer-wired.md` exists. Shipped code follows it: `AndroidManifest.xml` declares `POST_NOTIFICATIONS`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_SPECIAL_USE`, `USE_EXACT_ALARM`, and `SCHEDULE_EXACT_ALARM` (maxSdk 32); `RestTimerService` is `foregroundServiceType="specialUse"`. `RestTimerSync.arm` sets `setAlarmClock` and starts the service whenever a rest still has time left; `MuskliApplication.onCreate` starts `RestTimerSync`. `MainActivity` requests `POST_NOTIFICATIONS` on Start workout, before any set is logged.
  - Emulator smoke (install and launch only; not AC-02 evidence): emulator medium_phone, Android 16 (API 36), `emulator-5554` (`ro.build.version.release` 16, sdk 36). `adb install -r` Success. `am start -W -n hu.toliver.muskli/.MainActivity` Status ok, cold start, activity resumed (pid 14883). Screenshot `build/verify/t009-launch.png` shows the Muskli home screen (Start workout, History, Exercise library, Settings).
  - Commands: `.\gradlew.bat :domain:test testDebugUnitTest assembleDebug lintDebug` BUILD SUCCESSFUL in 4s (129 tasks: 2 executed, 127 up-to-date). On-disk reports from the up-to-date test tasks: domain 143, app 77, data 43, 0 failures. Lint executed: app 0 errors, 1 warning (`NewerVersionAvailable` on junit-bom); data no issues.
  - No new tickets. No decision changes. Spike branch not touched.
- 2026-10-29 REVIEW: Owner review: no findings (Blocker / Should-fix / Nit: none). The branch records the D-015 verdict; the prototype stays unmerged on `spike/t-009-rest-timer-prototype`. `.\gradlew.bat :domain:test assembleDebug` BUILD SUCCESSFUL. Moved to `verify/`.
- 2026-10-29 SPIKE: Phone results in. Samsung SM-S916B, Android 16, battery optimisation default, battery saver on, screen off, 90 s × 5. A 5/5 at +1 to +11 ms (handler). B 5/5 at about 1 s (`setAlarmClock`), still firing after the app was closed. C 5/5 at about 1 s, including deep Doze. D failed: countdown went negative, no alert, no log. D-015 combo confirmed and written into `docs/DECISIONS.md` and ARCHITECTURE §5. Prototype stays on `spike/t-009-rest-timer-prototype` and is not merged. Moved to `review/`.
  - Commands this step: docs only. `.\gradlew.bat :domain:test assembleDebug` BUILD SUCCESSFUL.
  - Gotcha for T-015: set the alarm on every rest even while the service is running, and cancel the countdown when either path fires. The handler uses uptime and stalls when the CPU sleeps; the notification chronometer does not.
  - Owner still has the spike installed (Desktop APK, no USB). Uninstall "Muskli T-009" when finished, or it stays beside the real app.
- 2026-10-29 SPIKE: **Owner decided** (record in D-015 together with the phone results): use `USE_EXACT_ALARM`; request `POST_NOTIFICATIONS` when the first workout starts, before any set is logged. Still open: keep or drop the FGS, which depends on the mode A vs mode B phone results.
- 2026-10-29 SPIKE: Docs read and cited (see Findings). Prototype built on `spike/t-009-rest-timer-prototype` (commit `70050c1`, never merge) and smoke-tested on the API 36 emulator. Reliability table, side effects, and provisional D-015 verdict written. **Not done:** real-device measurements (owner runs them; steps below). Ticket stays in `doing/` until they are in. `docs/DECISIONS.md` is untouched until the owner confirms.
  - Note: T-001 (dependency) is still in `verify/`. The owner asked for T-009 explicitly.
  - Commands: `.\gradlew.bat :domain:test assembleDebug` BUILD SUCCESSFUL. `lintDebug` reports no errors; the new warnings are only in spike code (`commit()` is intentional so state survives process death). Emulator runs are in the table.
  - Gotcha: `am kill` doesn't kill a recently used app on the emulator, so process death is untested. The phone's "swipe from Recents" step covers it; some OEMs force-stop on swipe, and force-stop cancels alarms.
  - Phone steps:
    1. `git switch spike/t-009-rest-timer-prototype`, then `.\gradlew.bat assembleDebug` (with `JAVA_TOOL_OPTIONS` set as in AGENTS §5).
    2. Phone: enable Developer options and USB debugging, then connect. Run `& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" install -r app\build\outputs\apk\debug\app-debug.apk`. adb isn't on PATH.
    3. **Unplug the USB cable.** Charging blocks Doze. Turn off DND and silent mode; leave battery settings at their defaults.
    4. Open the "Muskli T-009" icon. Tap "Allow notifications" if shown. Check the status block: notifications allowed, exact alarms allowed, battery optimisation "optimised (default)".
    5. Select mode A, 90 s, 5 repetitions, tap "Start series". Lock the phone, put it face down on a table, and don't touch it for about 8 minutes. Count the buzzes.
    6. Unlock, tap "Copy log", paste it into chat. Note the number of buzzes felt and whether you saw an alarm icon or "next alarm" on the lock screen.
    7. Tap "Clear log" and repeat step 5 with mode B, then mode D. Mode C is optional.
    8. Mode B, 120 s, 1 repetition. Start, then swipe the app away from Recents, lock, wait. Report whether it fired.
    9. Afterwards, `git switch main` and reinstall, or uninstall, to remove the spike icon.
