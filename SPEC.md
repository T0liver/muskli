# SPEC.md

Status: **draft v0.1.** Changes to scope here require the owner's approval.
Background and rationale: `docs/research/` (esp. 02, 05, 07, 08).

## 1. Product statement
A fast, offline-first Android gym logger for one lifter. It remembers what I did last time, runs my rest timer, understands set types and supersets, and shows my progress per exercise and per muscle. It never blocks me with caps, accounts, or social features.

## 2. Principles
1. The live-workout screen is the product. Judge every decision by: does it make logging a set faster?
2. Local-first: the on-device database is the source of truth. No account, no network required.
3. Optional features are settings toggles.
4. Derived stats (records, volume, streaks) are computed from raw sets, never stored.
5. Own content only (own code, UI, exercise data).
6. Domain math is tested before it is built.

## 3. Non-goals (do not build, do not propose)
Social anything (feed, follow, likes, comments, leaderboards, sharing), coach/client features, accounts or login, subscriptions, ads, cloud sync (for now), watch apps, widgets, Live Activity-style features, Strava/Health Connect, exercise videos, Strength Level percentiles, Year-in-Review, share cards.

## 4. Glossary
- **Template (exercise):** library entry, e.g. "Bench Press (Barbell)".
- **Workout:** one session. **Workout exercise:** an exercise instance in a workout. **Set:** one logged row.
- **Set types:** `normal`, `warmup`, `dropset`, `failure`.
- **Exercise types:** `weight_reps`, `reps_only`, `bodyweight_reps`, `bodyweight_assisted_reps`, `duration`, `weight_duration`, `distance_duration`, `short_distance_weight`.
- **Superset:** exercises sharing a `superset_group` value.
- **Previous:** values from the last time this exercise was done (see F-04).
- **Volume:** per D-010 — `weight_reps`: weight_kg × reps; bodyweight/assisted use the settings body weight (D-017); unilateral per D-014; summed over qualifying completed sets.

## 5. MVP features (Phase 1)

Each feature lists acceptance criteria (AC). Test IDs refer to `docs/TEST-CASES.md`.

### F-01 Exercise library
- AC1: First launch works offline from a bundled catalogue snapshot; a full sync updates it when online (D-021). Each exercise has type, primary muscle, secondary muscles, equipment (enums in ARCHITECTURE).
- AC2: Search by name; filter by muscle and equipment.
- AC3: Create, edit, archive custom exercises; no limits.
- AC4: Recently used exercises are easy to reach.

### F-02 Live workout
- AC1: Start an empty workout; stopwatch runs; tap to edit start time/duration.
- AC2: Add exercises (several at once), reorder, replace, remove.
- AC3: The workout is a **draft persisted on every change**; killing the app and reopening resumes it exactly. (AC-03)
- AC4: Header shows completed set count and volume.
- AC5: Keep-screen-awake setting works during a live workout.

### F-03 Set logging
- AC1: Add set; delete set (swipe). No limit on sets/exercises.
- AC2: Input fields depend on exercise type (weight/reps/time/distance).
- AC3: Tap the set number to change type (normal/warm-up/drop/failure); type shown inline as a small marker.
- AC4: Tapping the check completes the set; only completed sets count in stats.
- AC5: Unit toggle kg/lb globally and per exercise; storage always kg (TC-UNIT-*).

### F-04 Previous values
- AC1: Each set row shows the previous value for the same set position (TC-PREV-*).
- AC2: Setting: previous = "last time anywhere" or "last time in this routine" (routine mode applies once routines exist; Phase 2).
- AC3: Adding an exercise done before pre-fills set count/weight/reps from last time (editable).

### F-05 Rest timer
- AC1: Completing a set starts the rest timer using exercise rest -> default rest -> off (TC-REST-*).
- AC2: No auto-start when the **next** set is a drop set.
- AC3: Timer survives screen-off and app backgrounding; fires a sound/vibration + notification at zero (AC-02).
- AC4: Timer can be adjusted (+/-15 s) or skipped from the in-app bar and notification (exact UI per ticket).

### F-06 Finish & history
- AC1: Finish screen: name, duration, start date/time, note.
- AC2: History list newest first; open a workout; edit or delete it; undo delete.
- AC3: Log a past workout (start with chosen date).
- AC4: Exercise history screen: sessions for one exercise, tappable.

### F-07 Settings (basic)
- Units, default rest timer, previous-values mode, keep awake, sounds/vibration, week start day, and current body weight. Defaults: kg, 90 s, "anywhere", keep awake on, Monday (D-009); body weight unset (D-017).

### F-08 Backup / export / import (own format)
- Android Auto Backup may include workout data (D-024). That is the OS backup, not this feature.
- AC1: Export everything to one JSON file; import restores identical data (round-trip test).
- AC2: Import is safe: validates, reports errors, never partially applies.

### F-09 Hevy data import
- AC1: Import workouts and custom exercises from the owner's Hevy export (CSV; format, mapping, and rules in `docs/HEVY-IMPORT.md`, D-040..D-045).
- AC2: Unmapped exercises are listed for manual mapping, not dropped silently.

## 6. Next (Phase 2: not now)
Routines + folders, supersets UI, notes system, per-exercise records/charts, calendar + streak, RPE column, warm-up calculator, plate calculator, live PR notification, body measurements.

## 7. Later (Phase 3+)
Muscle-volume statistics, monthly report, unilateral and partial-rep logging, progression engine, progress photos, local MCP/API server.

## 8. Quality bar
- Logging a set from a prefilled row takes at most 2 taps (edit nothing -> tap check).
- App cold start to "ready to log" under 2 s on the owner's phone (measure, don't guess).
- No data loss on crash, kill, or rotation.
- Works in airplane mode.
