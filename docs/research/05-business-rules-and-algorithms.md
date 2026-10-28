# 05 — Business Rules & Algorithms

For each rule: **what Hevy documents**, **what is unknown**, and a **suggested implementation** to start from. Anything labelled "suggested" is my proposal **[INFERENCE]**, not Hevy's confirmed behavior. Test cases at the end are designed so AI agents can write unit tests before implementing.

---

## 1. Set types

| Type | API value | Meaning | Rule effects |
|---|---|---|---|
| Normal | `normal` | Working set | Counts everywhere |
| Warm-up | `warmup` | Preparation set | **Optionally excluded** from sets/volume/PR stats via a setting; toggling recalculates history **[OFFICIAL]** |
| Drop | `dropset` | Reduce weight right after failure, continue | **Rest timer does not auto-start** when the *next* set is a drop set **[OFFICIAL]** |
| Failure | `failure` | Taken to muscular failure | Labeling only (analytics tag) |

Multiple consecutive drop sets are allowed (e.g., 1 normal + 3 drops). Any set's type can change any number of times, in routines and live workouts. **[OFFICIAL]**

**Unknown:** whether drop/failure sets are counted in "sets per muscle" (assumed yes), and whether warm-ups count toward the workout's displayed live "sets" counter.

## 2. Rest timer

**Documented [OFFICIAL]**
- Timer length is chosen **per exercise**; global default exists; can be off globally or per exercise.
- Starts when a set is marked complete. Notifies at zero. Sound & volume configurable.
- Not started when the *following* set is a drop set.
- In supersets, rest timer is adjustable/off per movement.

**Unknown:** what happens if you complete another set while the timer runs (restart vs. ignore); whether timer survives app backgrounding (it must in practice — notifications); in-app timer UI (add/subtract 15s?).

**Suggested implementation**
```
onSetCompleted(set):
  if timer_for(exercise) is OFF: return
  next = nextSetInWorkout(set)              // across exercise boundary in supersets
  if next != null and next.type == 'dropset': return
  startTimer(duration = exercise.rest_seconds ?? settings.default_rest_s)
  // schedule a local notification for when it hits 0 so it works with screen off
```
Superset nuance: after completing a set in a superset, the rest timer should arguably start only after the *last* exercise in the round — verify Hevy's behavior in the app (open question in file 10).

## 3. "Previous" values

**Documented [OFFICIAL]** Two modes (setting):
1. **Any context:** last logged performance for that exercise regardless of routine.
2. **Routine context:** last time that exercise was done *as part of the current routine*.

Also: when adding an exercise you've done before, sets/weights/reps are prefilled from last time.

**Suggested implementation**
```
previous(exercise_template_id, set_index, mode, routine_id):
  last_workout = most recent workout with that exercise (and workout.routine_id == routine_id if mode == 'routine')
  return the set at the same position (set_index) in that workout's exercise, else null
```
**Edge cases to decide:** warm-up sets alignment (does "set 1" map by raw index or by type-aware index?), more sets now than last time, deleted sets, exercises done twice in one workout.

## 4. Volume

**Documented [OFFICIAL]:** "volume load" is shown live and in stats; stats let you choose whether warm-ups are included.

**Suggested definition**
- `set_volume = weight_kg × reps` for `weight_reps` (and `short_distance_weight` if you decide).
- Bodyweight types: **unknown** how Hevy treats them (user bodyweight? reps only?). Options: reps only; or (bodyweight + added weight) × reps using the latest body weight; assisted: (bodyweight − assistance) × reps.
- `session_volume = Σ set_volume` over *completed, qualifying* sets.
- **Unilateral caveat:** Hevy users report volume becomes inaccurate for left/right exercises (file 07). If you add a `side` field, define whether volume sums both sides.

## 5. Estimated 1RM ("Calculation of One Rep Max") and PRs

**Documented [OFFICIAL]:** exercise summary shows "projected or true 1RM"; live PR toast covers "1RM, most reps, best duration, or something else"; summary also lists heaviest weight, best set volume, best session volume, most reps; **Set Records** list best weight per rep count.

**Unknown:** Hevy's exact 1RM formula (not documented in anything I read), and tie-break rules.

**Candidate formulas (public domain math)**
- Epley: `1RM = w × (1 + r/30)`
- Brzycki: `1RM = w × 36 / (37 − r)` (valid up to ~10 reps; undefined at r = 37)
- At r = 10 they match; below 10 Epley is slightly higher. If r = 1, 1RM = w.

**Suggested PR categories (per exercise) [INFERENCE]**
| PR | Applies to | Compare |
|---|---|---|
| Heaviest weight | weight types | max `weight_kg` |
| Estimated 1RM | `weight_reps` | max formula value |
| Most reps (at a weight, or overall) | reps types | max `reps` |
| Best set volume | weight_reps | max `w × r` |
| Best session volume | weight_reps | max Σ per workout |
| Best duration | duration types | max `duration_s` |
| Weight-per-rep-count (Set Records) | weight_reps | map r → max w with ≥ r reps (decide: exact r or "≥ r") |

PR detection should run **at set-complete time** (for live toast) and be **recomputable** from history (for edits/deletes). Prefer computing records on the fly from an indexed query for a personal-size dataset; cache later if slow.

## 6. Warm-up calculator

**Documented [OFFICIAL]**
- Percent-based ladder: each warm-up set = a percentage of the target working weight + a rep count.
- User can edit each row's percent/reps, add/remove rows, reset to default.
- Rounding settings: plate rounding and dumbbell rounding (e.g., 2.5 kg increments).
- Applies only to load-based exercises. Inserted as sets of type `warmup` at the top.
- Flow: ⋯ → Add Warm-up Sets → enter target weight → Insert.

**Unknown:** Hevy's **default** ladder (shown only in an image on their page).

**Suggested default ladder (my own, common practice)**
| Set | % of working weight | Reps |
|---|---|---|
| W1 | 40% | 10 |
| W2 | 60% | 5 |
| W3 | 80% | 3 |

```
warmups(target_kg, ladder, rounding_kg):
  for row in ladder: w = roundToNearest(target_kg × row.pct, rounding_kg); emit set(type=warmup, weight=w, reps=row.reps)
```
Rounding choice: nearest vs. down; ensure monotonic non-decreasing weights and none ≥ target.

## 7. Plate calculator

**Documented [OFFICIAL]:** barbell / EZ bar / short bar only; inputs: target weight, bar weight, available plates; outputs which plates to load; entered from a button above the keyboard.

**Unknown:** whether output is per side (almost certainly) and how it handles impossible targets.

**Suggested algorithm (greedy works for standard plate sets; use DP/bounded knapsack for odd inventories)**
```
platesPerSide(target, bar, inventory):   // inventory: {plate_kg: count_per_side}
  remaining = (target − bar) / 2
  if remaining < 0: return error
  for plate in sortedDescending(inventory):
      n = min(inventory[plate], floor(remaining / plate)); take n; remaining −= n × plate
  return plates, leftover = remaining × 2 (show "closest achievable: X kg")
```
Mind float error: do arithmetic in integer grams or hundredths.

## 8. Rep ranges, RPE, duration

- Routines accept either exact reps or a **rep range** (two numbers). Live workouts log one rep number. **[OFFICIAL]**
- RPE: optional column in live workouts; **no RPE targets in routines**; not available for duration exercises. **[OFFICIAL]**
- Duration sets: manual entry or an inline per-set timer (setting). **[OFFICIAL]**

## 9. Streak, calendar, reports

- **Streak = consecutive weeks with ≥ 1 workout.** The "first day of the week" is configurable. **[OFFICIAL]**
  - Unknown: whether the current (incomplete) week counts and what happens on a missed week boundary. Suggested: current week counts if it already has a workout; if not, the streak remains alive until the week ends.
- **Workout ordinal** ("472nd") = number of saved workouts up to and including this one (by date or by creation — decide).
- **Monthly report** is generated for the *previous* month once the new month starts; includes workouts, volume, sets, deltas vs. previous months, PRs, active days, streak, muscle distribution, top exercises. **[OFFICIAL]**
- **Year in Review** appears at the start of December. **[OFFICIAL]**
- Stats windows: 30 days / 3 months / 1 year / all time (free tier caps at 3 months; irrelevant for you).

## 10. Muscle-volume stats

**Documented [OFFICIAL]:** sets are attributed to muscle groups using the exercise's **primary and secondary** muscles; custom exercises must set them for stats to be correct. Can group weekly/monthly/yearly.

**Unknown:** whether secondary muscles count as full sets, fractional (e.g., 0.5), or only appear in the body diagram.

**Decision for you:** many hypertrophy lifters count only *primary* muscles as full sets and secondary as 0.5 or 0. Make this a setting (default: primary = 1, secondary = 0.5 is a common convention **[INFERENCE]**).

## 11. Strength Level (optional, P2)

Only for barbell Squat/Bench/Deadlift; compares estimated or best performance to others by **age, bodyweight, sex** → Beginner/Intermediate/Advanced/Elite **[OFFICIAL]**. Source of Hevy's standards is **unknown**. For a personal app you'd need your own standards table (many public strength-standard tables exist; check licenses) or skip.

## 12. Hevy Trainer's progression (reference for an optional auto-progression feature)

Per Hevy's public description (via third-party review) **[3RD-PARTY citing OFFICIAL]**:
- Inputs: experience level, goal, equipment, frequency, session length, muscle-group focus.
- **Double progression:** keep weight until you reach the top of the rep range on **all** prescribed sets; then increase weight.
- No sleep/HRV/readiness inputs.

**Suggested minimal implementation**
```
nextTarget(routine_exercise, last_session_sets, rep_min, rep_max, increment_kg):
  if all(working sets in last_session_sets has reps >= rep_max): return (weight + increment, rep_min)
  else: return (same weight, min(rep_max, lowest_reps + 1))   // or just "same weight, aim for +1 rep"
```
Per-exercise `increment_kg` (e.g., 2.5 upper / 5 lower, or 1.25 for small lifts) should be configurable.

## 12b. Unit handling
Stored kg; UI kg/lb; per-exercise override exists in Hevy **[OFFICIAL]**. Rounding rules for displayed lb values (e.g., 0.1 lb vs nearest 0.5) should be explicit.

---

## Test cases for agents (write these before the code) [INFERENCE]

**1RM**
| w | r | Epley | Brzycki |
|---|---|---|---|
| 100 | 1 | 103.33 (Epley formally defined for r>1; special-case r=1 → 100) | 100 |
| 100 | 10 | 133.33 | 133.33 |
| 100 | 6 | 120.00 | 116.13 |
| 60 | 5 | 70.00 | 67.50 |

**Rest timer**
1. Complete set 2 of exercise with 90s rest, next set normal → timer starts at 90s.
2. Complete set 3 when set 4 is `dropset` → no timer.
3. Rest = off → no timer.
4. Superset A/B (rest A=60, B=60), complete A1 → (decide) timer or scroll-only; document the decision.

**Warm-up ladder**
- target 100 kg, ladder 40%×10, 60%×5, 80%×3, rounding 2.5 → 40, 60, 80 kg.
- target 62.5 kg, same ladder, rounding 2.5 → 25 (25.0), 37.5, 50.
- target 20 kg with bar-only minimum → decide floor behavior (don't emit sets below empty bar).

**Plates**
- target 100, bar 20, plates {25,20,15,10,5,2.5,1.25} unlimited → per side 40 → 25 + 15.
- target 62.5, bar 20 → per side 21.25 → 20 + 1.25.
- target 21, bar 20 → per side 0.5 → impossible with smallest 1.25 → report closest (20 or 22.5) and leftover.

**Streak** (week starts Monday)
- Workouts in 4 consecutive weeks → streak 4.
- Gap of one full week → streak resets to 1 upon next workout.
- Workout on Sunday 23:30 vs Monday 00:30 → different weeks (timezone-aware).

**Previous values**
- Mode "routine": exercise done last in Routine A (80×8) then in Routine B (85×6); starting Routine A shows 80×8, starting Routine B shows 85×6; "any" mode shows 85×6 in both.

**Volume**
- Sets: warm-up 40×10, normal 100×5, normal 100×5; with warm-ups excluded → 1000; included → 1400.

**PR detection**
- Existing best: 100×5 (est. 1RM 116.67 Epley). New set 100×6 → new 1RM PR and "most reps at 100" PR. New set 105×3 → new heaviest-weight PR; 1RM = 115.5 → **not** a 1RM PR.
- Deleting the PR workout must restore the previous record (recomputability).
