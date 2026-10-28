# 08 — Suggested MVP Scope & Roadmap

> **All of this is my suggestion [INFERENCE].** It is meant to be argued with in the planning session. It is derived from file 02's feature list and file 07's problems, under the constraint "single user, personal use, built with AI agents".

## 1. Product statement (draft)

> A fast, offline-first gym logger for one lifter. It remembers what I did last time, runs my rest timer, understands set types and supersets, and shows me my progress per exercise and per muscle — and it never blocks me with caps, accounts, or social noise.

## 2. Guiding principles
1. **The logging screen is sacred.** Every decision is judged by "does it make logging a set faster?"
2. **Local-first.** App works fully with no network; data lives on-device; backup/export is built-in.
3. **Optional features are toggles** (Hevy's best pattern).
4. **Compute, don't store** derived stats (records, volume) so edits stay consistent.
5. **Own your content** — exercise library and any media must be legally yours.
6. **Test the math** — 1RM, volume, PR, warm-up, plates, streaks get unit tests first (file 05).

## 3. Scope cuts (explicitly NOT building)

| Cut | Reason |
|---|---|
| Social feed, follow, like/comment, tagging, leaderboards, discovery, compare | Single user |
| Hevy Coach–style client/trainer features | Business feature |
| Subscriptions, tiers, paywalls, caps | Not needed |
| Strava integration | Not needed (revisit) |
| Public API hosting | Not needed (a *local* API/MCP is different — see P3) |
| Apple Watch / Wear OS apps | High effort; revisit after MVP |
| Live Activity / widgets | Platform-specific polish; revisit |
| Strength Level percentile tables | Requires external standards data |
| Year-in-Review / share cards | Fun, not essential |
| Routine marketplace / sharing | Single user |

## 4. Phases

### Phase 0 — Foundations (before any UI polish)
- Data model + migrations (file 04).
- Exercise library seed (own/open dataset; see file 09) with type/muscle/equipment.
- Pure-logic module with unit tests: set volume, 1RM, PR detection, warm-up ladder, plate calc, streak (file 05).
- Settings store (units, default rest, previous-values mode, toggles).

### Phase 1 — MVP: "log a workout the way Hevy does"  (P0 features)
1. Exercise library: search, filter (muscle/equipment), custom exercises (unlimited).
2. Start empty workout; add exercises; add/delete sets.
3. Set row with **Previous** column + prefill from last time.
4. Set types: normal / warm-up / drop / failure.
5. One-tap set completion → **auto rest timer** (per exercise/default/off; skip before drop sets; works with screen off via local notification).
6. Reorder/replace/remove exercise; weight/reps/time/distance inputs by exercise type.
7. Finish flow: name, duration/start-time edit, note → save.
8. History list; open/edit/delete past workouts; retroactive log.
9. Units (kg/lb); keep-awake.
10. Backup/export (JSON) and import — **and a Hevy importer** if you want to continue your history (file 10).

**MVP acceptance test:** do a real gym session start-to-finish using only the app, faster than in Hevy.

### Phase 2 — Routines & progress
- Routine builder (sets, set types, target weight, rep range, rest, notes), folders, "start routine", "create routine from workout", "copy workout".
- Supersets (+ smart scrolling setting).
- Notes system: routine notes, session notes, pinned exercise notes.
- Per-exercise page: Summary records, Set Records, History, charts (volume, best weight, total reps, e1RM).
- Calendar + weekly streak.
- RPE column (+ RPE/RIR *targets* — improvement over Hevy).
- Warm-up calculator + plate calculator.
- Live PR notification.
- Body measurements + weight chart.

### Phase 3 — Insight & your differentiators
- Statistics: sets per muscle (weekly/monthly), muscle distribution, 7-day body view; configurable primary/secondary weighting.
- Monthly report.
- Unilateral logging (`side`), partial reps, other set-model extensions from file 07.
- Progression engine (double progression, per-exercise increments); deload hints.
- Progress photos & compare.
- Local API/MCP server so an AI assistant can read history and generate routines.

### Phase 4 — Optional
- Watch app, widgets, Health Connect sync, web view, share cards, Year in Review, Strength Level.

## 5. Suggested first backlog (Phase 0–1), ordered

1. Repo/tooling setup, CI with unit tests. 
2. Schema + migrations + seed loader.
3. `calc` module (1RM/volume/PR/warm-up/plates/streak) with tests from file 05.
4. Exercise library screen (search/filter/create custom).
5. Live workout screen skeleton (state machine: idle → active → finished).
6. Set row component (inputs by exercise type, set-type menu, complete).
7. Previous-values service (+ modes).
8. Rest timer service (foreground + notification).
9. Finish flow & persistence; history list/detail/edit.
10. Settings screen; export/import; Hevy importer.

## 6. Definition of done (per feature)
- Unit tests for logic; UI test for the happy path.
- Works offline.
- Doesn't add a tap to the logging flow unless it's behind a setting.
- Data round-trips through export/import.

## 7. Risks [INFERENCE]
| Risk | Mitigation |
|---|---|
| Scope creep from social/coach features | Keep the cut list above visible in repo README |
| UI feel never matches Hevy's polish | Gather screenshots/recordings (file 11); iterate on the logging screen only |
| Background rest-timer reliability on Android (battery optimizations) | Use platform alarm/notification APIs; test on your phone early |
| Exercise library licensing | Choose openly licensed dataset or write your own (file 09) |
| AI-agent drift on domain math | Tests first (file 05); keep calc module pure and isolated |
