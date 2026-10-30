# HEVY-IMPORT.md: Hevy CSV export and how Muskli imports it

Status: **spec v1** from SPIKE T-020 (2026-10-30). Decisions: D-040 to D-045. Build tickets: T-035 to T-040.
SPEC F-09 (AC1: import workouts and custom exercises; AC2: unmapped exercises are listed for manual mapping, never dropped). Acceptance scenario AC-07.

Evidence: the owner's real export, two files in `docs/fixtures/` (git-ignored personal data, never commit them): `workout_data.csv` (18 set rows, 2 workouts plus a 1-minute one) and `measurement_data.csv` (1 row). Anything not visible in those files is marked *assumed* and backed by research/06 (Hevy's public API enums).

## 1. File facts (observed)

| Property | `workout_data.csv` | `measurement_data.csv` |
|---|---|---|
| Encoding | UTF-8, **no BOM** | UTF-8, no BOM |
| Line ends | **CRLF**, no newline after the last row | CRLF, no final newline |
| Delimiter / quoting | comma; every **string** field is double-quoted, numbers and empty numbers are bare | same |
| Header | 14 columns, quoted, first row | 17 columns, quoted, first row |
| Granularity | **one row per set**, workout and exercise fields repeated on every row | one row per measurement entry |
| Row order | newest workout first; inside a workout, exercise after exercise, sets by `set_index` | one row only, order unknown |
| Time format | `28 Oct 2026, 07:10`: day, English month abbreviation, year, 24 h time to the **minute**, **no time zone** | same format; date-only entries show `00:00` |

Not visible in the sample and therefore **assumed** (the parser must handle them anyway): `warmup`/`dropset`/`failure` set types (research/06), integer `superset_id` values, decimal `rpe`, `distance_km`, `duration_seconds`, decimal weights with a `.` point, commas, quotes (`""`), and line breaks inside quoted notes, single-digit days (`5 Mar` or `05 Mar`), and LF-only line ends.

### Workout columns

| Column | Sample values | Meaning |
|---|---|---|
| `title` | `"Tuesday exercise"` | workout title |
| `start_time` / `end_time` | `"28 Oct 2026, 06:20"` | local wall-clock time, minute precision |
| `description` | `""` | workout note |
| `exercise_title` | `"Goblet Squat"`, `"Bench Press (Dumbbell)"` | Hevy's exercise name; **no exercise id, type, equipment, or muscles** |
| `superset_id` | empty | integer group within the workout, empty when not in a superset |
| `exercise_notes` | `""` | note on this exercise in this workout |
| `set_index` | `0,1,2` | 0-based, restarts per exercise occurrence |
| `set_type` | `"normal"` | `normal` \| `warmup` \| `dropset` \| `failure` |
| `weight_kg` | `10`, empty | kg (load, added weight, or assistance, depending on exercise type) |
| `reps` | `8` | integer |
| `distance_km` | empty | **kilometres** (the API uses metres; the CSV does not) |
| `duration_seconds` | empty | integer seconds |
| `rpe` | empty | decimal RPE |

### Measurement columns
`date, weight_kg, fat_percent, neck_cm, shoulder_cm, chest_cm, left_bicep_cm, right_bicep_cm, left_forearm_cm, right_forearm_cm, abdomen_cm, waist_cm, hips_cm, left_thigh_cm, right_thigh_cm, left_calf_cm, right_calf_cm`. Only `date` and `weight_kg` are used (D-045).

## 2. Mapping to Muskli's model

| Hevy CSV | Muskli | Rule |
|---|---|---|
| (`title`, `start_time`, `end_time`) | one `Workout` | rows with the same three values form one workout, kept in **file order of first appearance** |
| `title` | `Workout.title` | trimmed; may be empty (stored as `""`, like a new draft) |
| `start_time` | `startedAtEpochMs` | local time to epoch ms in the **device time zone at import** (D-041) |
| (offset at start) | `utcOffsetS` | `clock.utcOffsetS(startedAtEpochMs)` |
| `end_time` | `finishedAtEpochMs` | same conversion; must be ≥ start, else a row error. Hevy exports only finished workouts, so every import is finished |
| `description` | `Workout.notes` | `""` becomes null |
| (none) | `createdAtEpochMs`, `updatedAtEpochMs` | `clock.nowEpochMs()` at import |
| (none) | `deletedAtEpochMs`, `restTimer` | null |
| `exercise_title` | `WorkoutExercise.exerciseTemplateId` | via matching (section 3) |
| row run | `WorkoutExercise` | a new occurrence starts when `exercise_title` or `superset_id` changes, **or `set_index` is not greater than the previous row's**; repeated exercises in one workout stay separate occurrences (D-019 first occurrence then works) |
| `superset_id` | `supersetGroup` | renumbered per workout 0, 1, 2 in order of first appearance; empty becomes null |
| `exercise_notes` | `WorkoutExercise.notes` | `""` becomes null; CRLF inside the field becomes `\n` |
| (none) | `WorkoutExercise.restSeconds` | null (default rest) |
| `set_index` | set order | sets sorted by `set_index`; positions re-numbered contiguous from 0 (gaps allowed in the file) |
| `set_type` | `SetData.type` | `normal`→NORMAL, `warmup`→WARMUP, `dropset`→DROPSET, `failure`→FAILURE; anything else is a row error |
| `weight_kg` | `SetData.weightKg` | as is, kg (no rounding); empty becomes null |
| `reps` | `SetData.reps` | integer; a decimal with a fraction is a row error; empty becomes null |
| `distance_km` | `SetData.distanceM` | **× 1000**; empty becomes null |
| `duration_seconds` | `SetData.durationS` | integer; empty becomes null |
| `rpe` | **dropped** | Muskli has no RPE field (SPEC Phase 2, "RPE column"); the preview shows how many sets had one (D-042) |
| (none) | `SetData.completed` | **true** (Hevy only exports logged sets of finished workouts) |
| (none) | `WorkoutSet.completedAtEpochMs` | null (unknown) |
| (none) | `SetData.side` | BOTH |
| (none) | ids | `IdGenerator.newId()` for workouts, exercises, sets, and new custom exercises |

Negative numbers in any numeric column are row errors. Values stay as they are even if the matched exercise type does not use them (a `reps` value on a duration exercise is kept, not dropped).

### Validation and errors (D-040)
- Columns are found **by header name**, not by position. Unknown extra columns are ignored. A missing required column rejects the file.
- The file kind is detected from the header: `exercise_title` present means a workout file, `weight_kg` with `date` and no `exercise_title` means a measurement file, anything else is "not a Hevy export".
- All or nothing, like the backup import (D-034): every problem is collected with its **1-based CSV record number** (header is record 1) and column, and any problem rejects the whole file.
- A record with a different field count than the header is an error.

## 3. Exercise matching (D-043)

The CSV carries only the exercise **title**. Every distinct title is resolved once, in this order, against **all** exercise templates on the device (catalogue and custom, archived included):

1. **Exact:** `normalizeForSearch(hevyTitle) == normalizeForSearch(template.title)` (existing `:domain` function: trim, lowercase, no diacritics).
2. **Normalised:** both titles reduced to a sorted multiset of tokens: `normalizeForSearch`, delete apostrophes (`'` and `’`), replace every other non-letter, non-digit with a space, split on spaces. `Bench Press (Barbell)` and `Barbell Bench Press` both become `[barbell, bench, press]`; `Pull Up` matches `Pull-Up`; `Farmers Walk (Dumbbell)` matches `Dumbbell Farmer's Walk`.
3. If a step finds **several** candidates, prefer non-archived ones; if still more than one, the title is **unmatched** and the candidates are offered first in the mapping UI. A later step never overrides an earlier one. No fuzzy or edit-distance matching (false merges silently corrupt history; a wrong match is worse than a manual one).
4. **Unmatched** titles are listed in the preview (F-09 AC2). The default for each is **"Create as custom exercise"**; the user can instead pick any library exercise. The user can also change an automatic match. Nothing is dropped.

**New custom exercise** from a Hevy title: `title` = the Hevy title (trimmed), `isCustom` true, `primaryMuscle` OTHER, no secondary muscles, and inferred type and equipment the user can edit later in the library:
- **Type**, from which fields are non-empty in any of that title's sets in the file; first rule that applies wins:
  1. distance and weight → SHORT_DISTANCE_WEIGHT
  2. distance → DISTANCE_DURATION
  3. duration and weight → WEIGHT_DURATION
  4. duration → DURATION
  5. weight and a title token `assisted` → BODYWEIGHT_ASSISTED_REPS
  6. weight → WEIGHT_REPS
  7. reps → REPS_ONLY (conservative: no invented body-weight volume)
  8. nothing → WEIGHT_REPS
- **Equipment**, from title tokens: `barbell`→BARBELL, `dumbbell`→DUMBBELL, `kettlebell`→KETTLEBELL, `machine`, `cable`, `smith`→MACHINE, `band`→RESISTANCE_BAND, `plate`→PLATE, `suspension`, `trx`→SUSPENSION, `bodyweight`→NONE; otherwise NONE if no set has a weight, else OTHER.

**Licence boundary (D-025).** The bundled catalogue (`catalogue/catalogue.json`) is CC0 and must not contain Hevy exercise data. So: no Hevy title list, alias table, or Hevy-to-slug mapping in the repo, the catalogue, or the app. Matching uses only the titles already on the device and the user's own file at runtime. Unmatched Hevy exercises become **local custom exercises** (never published) or are mapped by the user.

Mappings are **not persisted**. On a later import, the custom exercises created the first time match exactly by title, so only titles the user mapped by hand to a differently named exercise are asked again.

## 4. Merge semantics and duplicates (D-044)
- The Hevy import **adds** to what is on the phone. It never deletes, edits, or replaces existing workouts, exercises, or settings (except the optional body weight, D-045). To start over, restore a Muskli backup (D-034 Replace) first.
- **Duplicate** = an existing workout (any state: finished, draft, or soft-deleted, so a deleted import stays deleted) whose `startedAtEpochMs` falls in the **same minute** (`epochMs / 60_000` equal) as the imported workout's start. Duplicates are skipped and counted in the preview. Two workouts in the file with the same start minute are both imported (the file is the source of truth for itself).
- Workouts and new custom exercises are written in **one Room transaction**; any failure writes nothing. No schema change is needed.
- The live draft is never touched.

## 5. Time zone (D-041)
The file has no zone. Each local time is resolved in the **device time zone at import** through the injected `Clock.utcOffsetS` (no `java.time` in `:domain`: minSdk 24, no desugaring). Suggested algorithm: let `L` be the local fields as if they were UTC; probe `oBefore = offset(L − 1 day)` and `oAfter = offset(L + 1 day)`; candidates `L − oBefore` and `L − oAfter` are valid when `offset(candidate)` equals the offset used; take the **earliest valid** candidate (DST overlap → the first occurrence); if none is valid (DST gap), use `L − oBefore` (the time moves forward by the gap, like `java.time`). Month names are the English three-letter abbreviations; day is 1 or 2 digits; anything else is a row error.

## 6. Measurements (D-045)
Only the **latest** row (by `date`) with a non-empty `weight_kg` is used. It is offered as one checkbox in the import preview: "Set body weight to 80.4 kg (from 1 Mar 2026)", **checked by default only when Settings body weight is unset**. Every other column, and the history, is ignored (body measurements are Phase 2).

## 7. Risks

| Risk | Impact | Mitigation |
|---|---|---|
| Hevy changes column names or adds columns | parse fails or misses data | header-by-name lookup, extra columns ignored, missing required column gives a clear error |
| Month names localised or 12 h clock if the Hevy app language changes | dates rejected | strict parser with record numbers in the error; the owner's export is English. New format = new ticket |
| No time zone in the file | workouts recorded while travelling shift by the zone difference | device zone at import; documented; edit start time later if needed |
| Exercise order: sample exercises happen to be alphabetical | possibly not the order performed | file order kept; the owner checks one workout during T-040 against Hevy |
| No exercise type in the file | custom exercises get a guessed type (bodyweight ↔ reps only) | conservative inference, user edits it in the library; volume of REPS_ONLY is 0 rather than invented |
| `rpe` dropped | RPE history not available when the Phase 2 RPE column comes; re-import skips duplicates so it will not backfill | preview shows the count; a future RPE ticket can add a one-off backfill |
| Wrong automatic match (normalised step) | sets attributed to the wrong exercise | only exact token-multiset equality, ambiguities go to the user, every match is editable in the preview |
| Logging in both apps during the transition | same session twice with different start minutes | not detected; the preview lists dates so the user can spot overlaps; delete one afterwards |
| Large history (years) | slow parse or a long transaction | streaming parse, one transaction; T-040 times the owner's real file |
| Personal data in the repo | privacy | real files stay in git-ignored `docs/fixtures/`; tests use the invented fixture below |
| `autocrlf` turns fixture LF into CRLF on Windows | platform-dependent note text in tests | the reader accepts CRLF and LF and normalises line breaks inside fields to `\n` |

## 8. Test fixtures (invented, anonymised; copy verbatim in T-035)

`domain/src/test/resources/hevy/workout_data.csv`:
```csv
"title","start_time","end_time","description","exercise_title","superset_id","exercise_notes","set_index","set_type","weight_kg","reps","distance_km","duration_seconds","rpe"
"Push, heavy","14 Mar 2026, 18:05","14 Mar 2026, 19:12","Felt ""strong""","Bench Press (Barbell)",,"Pause reps",0,"warmup",40,10,,,
"Push, heavy","14 Mar 2026, 18:05","14 Mar 2026, 19:12","Felt ""strong""","Bench Press (Barbell)",,"Pause reps",1,"normal",82.5,5,,,8.5
"Push, heavy","14 Mar 2026, 18:05","14 Mar 2026, 19:12","Felt ""strong""","Bench Press (Barbell)",,"Pause reps",2,"failure",82.5,4,,,10
"Push, heavy","14 Mar 2026, 18:05","14 Mar 2026, 19:12","Felt ""strong""","Lateral Raise (Dumbbell)",3,"",0,"normal",10,12,,,
"Push, heavy","14 Mar 2026, 18:05","14 Mar 2026, 19:12","Felt ""strong""","Lateral Raise (Dumbbell)",3,"",1,"dropset",6,10,,,
"Push, heavy","14 Mar 2026, 18:05","14 Mar 2026, 19:12","Felt ""strong""","Pull Up",3,"",0,"normal",,8,,,
"Push, heavy","14 Mar 2026, 18:05","14 Mar 2026, 19:12","Felt ""strong""","Plank",,"line one
line two",0,"normal",,,,60,
"Push, heavy","14 Mar 2026, 18:05","14 Mar 2026, 19:12","Felt ""strong""","Bench Press (Barbell)",,"",0,"normal",60,12,,,
"Legs","12 Mar 2026, 07:00","12 Mar 2026, 07:45","","Goblet Squat (Kettlebell)",,"",0,"normal",24,10,,,
"Legs","12 Mar 2026, 07:00","12 Mar 2026, 07:45","","Goblet Squat (Kettlebell)",,"",1,"normal",24,10,,,
"Legs","12 Mar 2026, 07:00","12 Mar 2026, 07:45","","Assisted Pull Up",,"",0,"normal",20,6,,,
"Legs","12 Mar 2026, 07:00","12 Mar 2026, 07:45","","Farmers Walk (Dumbbell)",,"",0,"normal",32,,0.04,,
"Legs","12 Mar 2026, 07:00","12 Mar 2026, 07:45","","Rowing Machine",,"",0,"normal",,,5.02,1260,
```

Expected with a fixed offset of **+3600 s**:
- 2 workouts, in file order. "Push, heavy": start `1773507900000`, end `1773511920000`, notes `Felt "strong"`, 5 exercise occurrences (Bench 3 sets, Lateral Raise 2 sets with `supersetGroup` 0, Pull Up 1 set with `supersetGroup` 0, Plank 1 set with notes `line one\nline two` and `durationS` 60, Bench again 1 set), 9 sets. "Legs": start `1773295200000`, end `1773297900000`, notes null, 4 occurrences, 5 sets; Farmers Walk `distanceM` 40.0, Rowing `distanceM` 5020.0, `durationS` 1260.
- 14 sets total, all completed; 2 sets with RPE.
- Against the 10 exercises of catalogue version 1 (copied into test code, not read from `catalogue/`): Plank and Rowing Machine **exact**; Bench Press (Barbell) → `barbell-bench-press`, Pull Up → `pull-up`, Farmers Walk (Dumbbell) → `dumbbell-farmers-walk` **normalised**; Lateral Raise (Dumbbell) (WEIGHT_REPS, DUMBBELL), Goblet Squat (Kettlebell) (WEIGHT_REPS, KETTLEBELL), Assisted Pull Up (BODYWEIGHT_ASSISTED_REPS, OTHER) **unmatched**.

`domain/src/test/resources/hevy/measurement_data.csv`:
```csv
"date","weight_kg","fat_percent","neck_cm","shoulder_cm","chest_cm","left_bicep_cm","right_bicep_cm","left_forearm_cm","right_forearm_cm","abdomen_cm","waist_cm","hips_cm","left_thigh_cm","right_thigh_cm","left_calf_cm","right_calf_cm"
"20 Feb 2026, 00:00",81.2,,,,,,,,,,,,,,,
"1 Mar 2026, 08:30",80.4,18.5,,,,,,,,,,,,,,
"05 Mar 2026, 00:00",,,,,,,,,,,88,,,,,
```
Expected: latest body weight **80.4 kg** dated 1 Mar 2026 08:30 local (the 5 Mar row has no weight).
