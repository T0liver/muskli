# TEST-CASES.md

Test IDs are referenced by tickets and tests (use the ID in the test name, e.g. `fun TC_1RM_03_epley_100x6()`).
`TC-*` = automated unit/integration cases (write first, in `:domain` where possible). `AC-*` = manual acceptance scenarios on the owner's phone.
Cases marked **DECISION** depend on an open item in `docs/DECISIONS.md`: do not implement them until decided.
Derived from `docs/research/05-business-rules-and-algorithms.md`; numbers were hand-checked.

## TC-UNIT: unit conversion
Constant: 1 kg = 2.20462262 lb. Display rounding is a UI concern; the calc returns full precision.
| ID | Input | Expected |
|---|---|---|
| TC-UNIT-01 | 100 kg to lb | 220.462262 |
| TC-UNIT-02 | 225 lb to kg | 102.0583 (4 dp) |
| TC-UNIT-03 | 45 lb to kg | 20.4117 (4 dp) |
| TC-UNIT-04 | kg to lb to kg round trip of 82.5 | 82.5 within 1e-9 |

## TC-1RM: estimated one-rep max
Epley: `w * (1 + r/30)`; Brzycki: `w * 36 / (37 - r)`. For r = 1 return `w` for both formulas. Invalid inputs return `null`.
| ID | w (kg) | r | Formula | Expected |
|---|---|---|---|---|
| TC-1RM-01 | 100 | 1 | Epley | 100.00 |
| TC-1RM-02 | 100 | 10 | Epley | 133.33 |
| TC-1RM-03 | 100 | 6 | Epley | 120.00 |
| TC-1RM-04 | 60 | 5 | Epley | 70.00 |
| TC-1RM-05 | 100 | 1 | Brzycki | 100.00 |
| TC-1RM-06 | 100 | 10 | Brzycki | 133.33 |
| TC-1RM-07 | 100 | 6 | Brzycki | 116.13 |
| TC-1RM-08 | 60 | 5 | Brzycki | 67.50 |
| TC-1RM-09 | 100 | 0 | any | null |
| TC-1RM-10 | 100 | 37 | Brzycki | null (division by zero) |
| TC-1RM-11 | 0 or negative | 5 | any | null |

(Tolerance 0.01.)

## TC-VOL: set and session volume
Volume = weight_kg x reps for `weight_reps`. Only **completed** sets count (D-010). Warm-ups excluded by default.
| ID | Case | Expected |
|---|---|---|
| TC-VOL-01 | warm-up 40x10, normal 100x5, normal 100x5; warm-ups excluded | 1000 |
| TC-VOL-02 | same sets; warm-ups included | 1400 |
| TC-VOL-03 | normal 100x5 completed, 100x5 **not** completed | 500 |
| TC-VOL-04 | set with null weight or null reps | 0, no crash |
| TC-VOL-05 | `reps_only` exercise | volume 0 (volume undefined for reps-only) |
| TC-VOL-06 | bodyweight: profile 80 kg, added 0, 10 reps | 800 |
| TC-VOL-07 | bodyweight: profile 80 kg, added 10 kg, 8 reps | 720 |
| TC-VOL-08 | assisted: profile 80 kg, assistance 20 kg, 5 reps | 300 |
| TC-VOL-09 | unilateral `both`: 20 kg × 10 reps (one row, both sides) | 400 (2× 20×10, D-014) |
| TC-VOL-10 | bodyweight or assisted, `bodyweightKg` null (unset) | 0, no crash (D-017) |

## TC-PR: records
Records computed from all completed qualifying sets of one exercise.
| ID | Case | Expected |
|---|---|---|
| TC-PR-01 | History best 100x5 (Epley 116.67). New set 100x6 | New 1RM PR and new reps-at-weight PR |
| TC-PR-02 | Same history. New set 105x3 (Epley 115.50) | New heaviest-weight PR; **not** a 1RM PR |
| TC-PR-03 | First-ever set for the exercise | Counts as a PR in every category it qualifies for |
| TC-PR-04 | Equal to existing best | Not a PR (strictly greater required) |
| TC-PR-05 | Delete the workout that holds the PR | Previous record is restored (records recomputed, not stored) |
| TC-PR-06 | Warm-up sets | Never produce PRs |
| TC-PR-07 | Set Records, best weight at **N or more** reps (D-020). Sets: 80×5, 100×8, 100×3 | 3+ reps: 100, 5+ reps: 100, 8+ reps: 100. Exactly-5 would be 80; that is wrong |

## TC-WARM: warm-up ladder
Default ladder (D-009/own choice): 40% x10, 60% x5, 80% x3. Rounding to the nearest `roundingKg`.
| ID | Target | Rounding | Expected sets (kg x reps) |
|---|---|---|---|
| TC-WARM-01 | 100 | 2.5 | 40x10, 60x5, 80x3 |
| TC-WARM-02 | 62.5 | 2.5 | 25x10, 37.5x5, 50x3 |
| TC-WARM-03 | 100 | 5 | 40, 60, 80 |
| TC-WARM-04 | any | any | Output types are `warmup`; weights are non-decreasing and all < target |
| TC-WARM-05 | 20, bar 20 | 2.5 | Empty list (nothing sensible below the empty bar) |
| TC-WARM-06 | Custom ladder with 1 row | | 1 set |

## TC-PLATE: plates per side (greedy over inventory)
Inventory default: 25, 20, 15, 10, 5, 2.5, 1.25 kg (unlimited counts unless specified).
| ID | Target | Bar | Expected per side / result |
|---|---|---|---|
| TC-PLATE-01 | 100 | 20 | 25 + 15 |
| TC-PLATE-02 | 62.5 | 20 | 20 + 1.25 |
| TC-PLATE-03 | 20 | 20 | no plates (bar only) |
| TC-PLATE-04 | 15 | 20 | error: target below bar |
| TC-PLATE-05 | 21 | 20 | not exactly achievable; closest achievable 20 (or 22.5) with leftover reported |
| TC-PLATE-06 | 140 | 20, limited inventory 20x2 per side, 5x1 per side | per side 60 > available; closest achievable + leftover reported, no crash |
| TC-PLATE-07 | 61.25 | 20 | per side 20.625 is not achievable with a 1.25 smallest plate; result: 20 per side (total 60), leftover 1.25 kg total, closest alternatives 60 / 62.5. Arithmetic done in integer grams, no float drift |

## TC-STREAK: weekly streak
Streak = consecutive calendar weeks with at least one workout, week start configurable. Use fixed `today` in tests.
| ID | Case | Expected |
|---|---|---|
| TC-STREAK-01 | Workouts in each of 4 consecutive weeks, last in current week | 4 |
| TC-STREAK-02 | A full empty week between the latest and the previous workout | latest week counts as 1 |
| TC-STREAK-03 | Workout Sunday 23:30 vs Monday 00:30 (week start Monday) | Different weeks |
| TC-STREAK-04 | Same dates with week start Sunday | Same-week result differs accordingly |
| TC-STREAK-05 | Current week empty but previous 3 weeks each have a workout | 3 (streak still alive until the week ends, D-023) |
| TC-STREAK-06 | No workouts | 0 |
| TC-STREAK-07 | Timezone: workout stored with UTC offset +01:00/+02:00 across DST | Week assigned by local date |

## TC-REST: rest timer effect
`restTimerEffect(completed, next, exerciseRestS, defaultRestS)` returns `Start(seconds)` or `None`.
| ID | Case | Expected |
|---|---|---|
| TC-REST-01 | Exercise rest 90, next set normal | Start(90) |
| TC-REST-02 | Next set is `dropset` | None |
| TC-REST-03 | Exercise rest null, default 120 | Start(120) |
| TC-REST-04 | Exercise rest 0 (off) | None |
| TC-REST-05 | Both null | None |
| TC-REST-06 | Completed set is the last set of the exercise, next is another exercise's normal set | Start(per completed set's exercise) |
| TC-REST-07 | Completed set is the last set of the whole workout | None (D-018) |
| TC-REST-08 | Superset A then B: complete a set on A (A rest 90, B rest 60) | Start(90) — rest of completed exercise (D-012) |

## TC-PREV: previous values
| ID | Case | Expected |
|---|---|---|
| TC-PREV-01 | History: last session had sets [60x10, 80x8]. Current set index 1, mode anywhere | 80x8 |
| TC-PREV-02 | Current set index beyond last session's set count | null |
| TC-PREV-03 | Mode routine: exercise last done in Routine A (80x8), then Routine B (85x6). Start Routine A | 80x8; Start Routine B gives 85x6; anywhere mode gives 85x6 in both (needs routines, Phase 2) |
| TC-PREV-04 | No history | null |
| TC-PREV-05 | Last session: warm-up 40×10 at index 0, normal 100×5 at index 1 (D-019) | Index 0 → 40×10; index 1 → 100×5. Raw index, warm-ups included |
| TC-PREV-06 | Only completed sets from finished (non-draft) workouts are considered | true |
| TC-PREV-07 | Exercise appears twice in the history workout | First occurrence (D-019) |

## TC-SET: set type behaviour
| ID | Case | Expected |
|---|---|---|
| TC-SET-01 | Change type any number of times | Persisted; no data loss |
| TC-SET-02 | Multiple consecutive drop sets | Allowed |
| TC-SET-03 | Delete a set | Remaining positions re-indexed contiguous from 0 |
| TC-SET-04 | Reorder exercises | Positions re-indexed; sets keep their exercise |

## TC-DATA: persistence (instrumented or Robolectric)
| ID | Case | Expected |
|---|---|---|
| TC-DATA-01 | Save workout with nested exercises/sets, read back | Equal |
| TC-DATA-02 | Draft workout survives process death (simulate by recreating DB connection) | Same draft |
| TC-DATA-03 | Soft-deleted workout excluded from history and previous-values, restorable | true |
| TC-DATA-04 | Migration N to N+1 retains data (one test per migration) | true |
| TC-DATA-05 | Export then import into an empty DB | Identical data |
| TC-DATA-06 | Import file with a bad record | Nothing applied, error report |
| TC-DATA-07 | Foreign keys: removing a workout removes its exercises and sets | true |

## TC-ROUTINE: routines (D-052)
| ID | Case | Expected |
|---|---|---|
| TC-ROUTINE-01 | Create " Push A " with exercises | Title "Push A", new id, created = updated = now; blank title rejected, nothing written |
| TC-ROUTINE-02 | Update title and exercises of a routine | Replaced; created kept, updated = now; unknown id gives null; blank title rejected |
| TC-ROUTINE-03 | List routines "pull", "Legs", "Push"; delete "pull" | Legs, pull, Push (title ignoring case, then id); then Legs, Push |
| TC-ROUTINE-04 | Start from a routine with no draft | Draft with the routine's exercises in order, notes/rest/superset, planned sets uncompleted with type and values, title = routine title, `routineId` set; an exercise without sets gets one empty normal set |
| TC-ROUTINE-05 | Start from a routine while a draft exists; start from an unknown routine | Existing draft returned as Resumed, nothing written; unknown gives null |
| TC-ROUTINE-06 | Save a finished workout as a routine | Exercises, notes, rest, superset, set types and values copied; side and completion dropped; draft, deleted, or unknown workout gives null |

## TC-HEVY: Hevy CSV import (docs/HEVY-IMPORT.md, D-040..D-045)
Fixtures: `domain/src/test/resources/hevy/` (HEVY-IMPORT §8). Unless stated, the fake clock has a fixed offset of +3600 s.

| ID | Case | Expected |
|---|---|---|
| TC-HEVY-01 | Parse the workout fixture | Exactly the values in HEVY-IMPORT §8: 2 workouts in file order, 5 + 4 exercise occurrences, 8 + 5 = 13 sets (D-056), all completed, `completedAtEpochMs` null, side BOTH, 2 sets with RPE |
| TC-HEVY-02 | CSV syntax: `""` inside quotes, comma inside quotes, line break inside quotes (CRLF and LF), CRLF and LF record ends, UTF-8 BOM, no final newline | Fields read correctly; line breaks inside a field become `\n`; BOM ignored |
| TC-HEVY-03 | Header handling: columns reordered; an extra unknown column; `exercise_title` column missing | Same result as the fixture; extra column ignored; missing column rejects the file (MISSING_COLUMN, record 1) |
| TC-HEVY-04 | Same exercise twice in one workout (`set_index` back to 0) | Two separate occurrences in file order |
| TC-HEVY-05 | One file with: unknown `set_type`, bad date, negative weight, `reps` `5.5`, end before start, a record with 13 fields | Rejected; one error per problem with its record number and column; no workouts returned |
| TC-HEVY-06 | DST gap: `29 Mar 2026, 02:30`, clock +3600 before `1774746000000` and +7200 from it | `1774747800000` (moved forward by the gap) |
| TC-HEVY-07 | DST overlap: `25 Oct 2026, 02:30`, clock +7200 before `1792890000000` and +3600 from it | `1792888200000` (earliest occurrence) |
| TC-HEVY-08 | `5 Mar 2026, 07:00` vs `05 Mar 2026, 07:00`; `5 Mär 2026, 07:00`; `5 Mar 2026, 7:00 PM` | First two equal; the others rejected (INVALID_DATE) |
| TC-HEVY-09 | Parse the measurement fixture; a measurement file with no `weight_kg` value | Latest body weight 80.4 kg at `1772350200000`; no body weight |
| TC-HEVY-10 | File kind: workout header; measurement header; a Muskli backup JSON; an empty file | Workouts; measurements; NOT_HEVY_EXPORT; NOT_HEVY_EXPORT |
| TC-HEVY-11 | Match the fixture titles against the 10 exercises of catalogue version 1 | Plank, Rowing Machine exact; Bench Press (Barbell), Pull Up, Farmers Walk (Dumbbell) normalised to `barbell-bench-press`, `pull-up`, `dumbbell-farmers-walk`; the other 3 unmatched |
| TC-HEVY-12 | Two non-archived templates with the same token multiset; one archived and one not | Unmatched with both as candidates; the non-archived one |
| TC-HEVY-13 | Custom "Pull Up" and catalogue "Pull-Up", Hevy title "pull up" | Exact match to the custom one |
| TC-HEVY-14 | Type inference, one case per rule 1-8 in HEVY-IMPORT §3; equipment from `(Barbell)`, `(Cable)`, `(Band)`, no token with and without weight | Rule's type; BARBELL, MACHINE, RESISTANCE_BAND, OTHER, NONE |
| TC-HEVY-15 | Plan with every unmatched title as "create custom" | 3 new custom templates (title = Hevy title, primary OTHER, no secondary, inferred type and equipment, ids from IdGenerator); every set references an existing or new template |
| TC-HEVY-16 | Plan with Lateral Raise (Dumbbell) mapped to an existing template and Pull Up re-mapped | No custom for it; sets use the chosen templates |
| TC-HEVY-17 | Existing workouts started at `1773507900000 + 37000` (same minute), and a soft-deleted one at `1773295200000` | Both fixture workouts skipped as duplicates; with the existing start one minute later, "Push, heavy" is imported |
| TC-HEVY-18 | Plan summary for the fixture on an empty phone | 2 in file, 2 new, 0 duplicates, 13 sets, 2 RPE values dropped, 3 new custom exercises, first and last start |
| TC-HEVY-19 | An unmatched title used only by duplicate workouts | No custom exercise created for it |
| TC-HEVY-20 | Apply the plan into a database with an existing finished workout and a draft | Existing rows unchanged; new workouts read back equal to the plan; new customs present; a failure injected after the first insert writes nothing |
| TC-HEVY-21 | Apply the fixture twice | Second time: 0 new, 2 duplicates, 0 new custom exercises |
| TC-HEVY-22 | Apply with "set body weight 80.4" checked; unchecked | Settings body weight 80.4 kg; unchanged |

## AC: manual acceptance scenarios (owner's phone, VERIFY tickets)
| ID | Scenario | Pass criteria |
|---|---|---|
| AC-01 | **Gym session**: start empty workout, add 4 exercises, log 12+ sets using prefilled/previous values, finish | Entire session without needing Hevy; no tap more than Hevy needed for the same action; nothing lost |
| AC-02 | **Rest timer**: complete a set, lock the phone, wait | Notification and sound/vibration at zero with the screen off; works with battery optimization at default settings |
| AC-03 | **Crash safety**: mid-workout, force-stop the app, reopen | Draft restored exactly, stopwatch and rest timer state correct |
| AC-04 | **Airplane mode**: full workout offline | Works |
| AC-05 | **Drop set**: complete a set before a drop set | No rest timer starts |
| AC-06 | **Backup**: export, wipe app data, import | Everything back |
| AC-07 | **Hevy import** (once built, T-040): import real export | Counts match Hevy (workouts, sets), unmapped exercises listed; importing the same file again adds nothing |
| AC-08 | **Speed**: cold start to ready-to-log | Under 2 s (measured) |
