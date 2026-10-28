# 06 — Hevy Public API (as reconstructed)

> **Provenance warning.** The official docs (`https://api.hevyapp.com/docs`) are JavaScript-rendered and returned nothing to me. Everything below is reconstructed from open-source clients and MCP servers (Python, Rust, Go, TypeScript) and two integration docs **[API]**. Field names/enums are consistent across several independent libraries, so they're probably right, but **verify against the live docs** before depending on them.

## Why this matters for a personal rebuild
1. **Schema reference:** it's the cleanest public statement of how Hevy models workouts (see file 04).
2. **Data migration:** if you already have a Pro key (or get one for a month), you can pull your entire history out in structured JSON and import it into your own app. *(Alternative: the in-app export — see file 10.)*
3. **Reference for your own future API/MCP:** a personal app with an MCP server for AI-assisted programming is a natural extension (file 09).

## Access
- **Requires Hevy Pro.** Key generated at `hevy.com/settings?developer` (web). **[API, 3RD-PARTY]**
- Base URL: `https://api.hevyapp.com` (paths under `/v1`).
- Auth: HTTP header `api-key: <UUID>`. Keys are tied to the creating account.
- Pagination: `page` + `pageSize`; **max pageSize is ~10** on at least some endpoints (400 if larger) per one MCP server's notes.

## Endpoints (as listed by multiple clients)

| Method | Path | Purpose |
|---|---|---|
| GET | `/v1/workouts` | List workouts (paginated) |
| GET | `/v1/workouts/{id}` | One workout |
| GET | `/v1/workouts/count` | Total workout count |
| GET | `/v1/workouts/events` | **Change feed**: `updated` (full workout) and `deleted` events since a timestamp — the *only* way to detect deletions |
| POST | `/v1/workouts` | Create workout (body: `{ "workout": {...} }`) |
| PUT | `/v1/workouts/{id}` | Update workout — **requires the entire workout object**, not a patch |
| GET | `/v1/routines` · `/v1/routines/{id}` | List / get routines |
| POST | `/v1/routines` | Create routine |
| PUT | `/v1/routines/{id}` | Update routine — `folder_id` **rejected** on update (400) |
| GET | `/v1/routine_folders` · POST | List / create folders |
| GET | `/v1/exercise_templates` · `/{id}` | Built-in + your custom exercises |
| POST | `/v1/exercise_templates` | Create custom exercise (error `exceeds-custom-exercise-limit` on free caps) |
| GET | `/v1/exercise_history/{exercise_template_id}` | Every historical set for one exercise |
| POST | `/v1/body_measurements` | Create one measurement record (read endpoints likely exist too — unverified) |

> **Path confidence:** `GET/POST/PUT /v1/workouts`, `/v1/routines`, `/v1/exercise_templates`, `POST /v1/routine_folders` and `POST /v1/body_measurements` were seen verbatim in client docs. The paths for **count, events, exercise history, and GET of body measurements** are inferred from client *method names* (`get_count`, `get_events`, `exercise_history`) — confirm exact paths in the live docs.

**Limitations reported by client authors**
- **No DELETE endpoints** (workouts/routines/templates) — deletions only observable via events feed.
- Exercise templates can be created but **not edited or deleted** via API.
- Updates are full-object replacement.

## Object shapes (JSON field names)

### Workout (response)
```jsonc
{
  "id": "uuid",
  "title": "Push Day",
  "description": "optional text",
  "start_time": "2026-10-20T17:02:11Z",
  "end_time":   "2026-10-20T18:05:40Z",
  "created_at": "...", "updated_at": "...",
  "exercises": [
    {
      "index": 0,
      "title": "Bench Press (Barbell)",
      "notes": "session note",
      "exercise_template_id": "uuid",
      "superset_id": null,            // int or null
      "sets": [
        { "index": 0, "type": "warmup", "weight_kg": 60, "reps": 10,
          "distance_meters": null, "duration_seconds": null, "rpe": null, "custom_metric": null },
        { "index": 1, "type": "normal", "weight_kg": 80, "reps": 8, "rpe": 8 }
      ]
    }
  ]
}
```
### Create-workout body
Same shape wrapped as `{ "workout": { title, description?, start_time, end_time, exercises: [{ exercise_template_id, superset_id?, notes?, sets: [{ type, weight_kg?, reps?, distance_meters?, duration_seconds?, rpe? }] }] } }`. Request numeric fields for reps/distance/duration are **integers**; responses may deliver floats. (Rust client has a custom deserializer because the API sometimes returns numbers as strings.)

### Routine (response)
`{ id, title, folder_id | null, created_at, updated_at, exercises[] }` with exercises/sets shaped like workouts (set `index` is 0-based).

### Exercise template
```jsonc
{ "id": "uuid", "title": "Bench Press (Barbell)", "type": "weight_reps",
  "primary_muscle_group": "chest", "secondary_muscle_groups": ["shoulders","triceps"],
  "equipment_category": "barbell", "is_custom": false }
```
### Create custom exercise body
```jsonc
{ "exercise": { "title": "...", "exercise_type": "weight_reps",
                "equipment_category": "barbell", "muscle_group": "chest",
                "other_muscles": ["triceps"] } }
```
### Exercise history entry
`workout_id, workout_title, workout_start_time, workout_end_time, exercise_template_id, weight_kg, reps, distance_meters, duration_seconds, rpe, custom_metric, set_type` — one row per set.

## Enums (consistent across clients)
- **Set type:** `normal | warmup | dropset | failure`
- **Exercise type (8):** `weight_reps | reps_only | bodyweight_reps | bodyweight_assisted_reps | duration | weight_duration | distance_duration | short_distance_weight`
- **Muscle group (20):** abdominals, shoulders, biceps, triceps, forearms, quadriceps, hamstrings, calves, glutes, abductors, adductors, lats, upper_back, traps, lower_back, chest, cardio, neck, full_body, other
- **Equipment (9):** none, barbell, dumbbell, kettlebell, machine, plate, resistance_band, suspension, other

## What the API does *not* expose (as far as I can tell)
Social data, PRs/records, statistics, progress photos, streaks, settings (warm-up ladder, plates), pinned notes, Trainer. Compute these yourself from raw sets (file 05).

## Ideas this suggests for your build [INFERENCE]
- **Mirror the JSON shapes** in your own export/import so you can round-trip with Hevy data.
- Build a one-off **Hevy → your DB importer** using `/workouts` (paged) + `/exercise_templates` (to map ids → your library) + `/exercise_history` for verification.
- Expose your own **local API / MCP server** for AI-assisted program generation (the Hevy ChatGPT integration proves the use case; you own the data so no Pro needed).
