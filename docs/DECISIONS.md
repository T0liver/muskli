# DECISIONS.md

Lightweight decision log. Statuses: **decided** (owner confirmed) · **proposed** (default agents may assume *only for planning*, still needs confirmation before it shapes code) · **open** (owner must choose; agents must ask).

Agents: never silently resolve an *open* item. Add new decisions with the next free ID.

| ID | Decision | Status | Notes |
|---|---|---|---|
| D-001 | Native Android, Kotlin, Jetpack Compose | **decided** | Owner choice |
| D-002 | Room/SQLite as local source of truth; no backend for MVP | **decided** | 2026-10-28: Room required for MVP — logger/diary must persist locally (see `:data` / T-006). |
| D-003 | Default 1RM formula = Epley; formula is a parameter of `estimateOneRepMax` | **decided** | 2026-10-28: Owner approved default; Hevy formula still unknown (research 10, A2). |
| D-004 | Three Gradle modules: `:domain` (pure Kotlin), `:data`, `:app` | **decided** | 2026-10-28: Keep `:app` (Android *application* module: Compose UI, ViewModels, Hilt, services) — not renamed to `:ui`. |
| D-005 | Exercise catalogue source & distribution | **decided** | 2026-10-28: Dedicated **`catalogue` branch** — exercises added as JSON/TOML (exact format via T-007). PR → **CI validates** schema → merge updates canonical catalogue on `main` → publish **static API on GitHub Pages**. Delivery into the app is **D-021** (bundled snapshot + full sync). **Delta-sync** optional post-MVP. User custom exercises stay local. 2026-10-29: format, flow, and URL fixed by **D-025**. |
| D-006 | DI: Hilt vs. manual constructor injection | **decided** | 2026-10-28: **Hilt** — set up now vs. rewrite later. |
| D-007 | minSdk | **decided** | 2026-10-28: **minSdk 24**; targetSdk = latest stable. |
| D-008 | App name and package id | **decided** | 2026-10-28: **Muskli** · `hu.toliver.muskli`. No Hevy branding. |
| D-009 | Defaults: kg, 90 s rest, previous = "anywhere", keep awake on, week starts Monday | **decided** | 2026-10-28: Approved defaults; all user-changeable in settings (T-017). |
| D-010 | Volume rules for completed sets; warm-ups excluded from stats by default | **decided** | 2026-10-28: **weight_reps:** weight_kg × reps. **Bodyweight:** (bodyweight_kg + added_weight_kg) × reps. **Assisted:** (bodyweight_kg − assistance_kg) × reps. **Unilateral:** D-014. Body weight is a calc parameter, stored per **D-017** (not a measurement log). Warm-up exclusion unchanged; a setting to include warm-ups comes later. |
| D-011 | Finishing a workout discards uncompleted sets after confirmation | **decided** | 2026-10-28: Owner approved. |
| D-012 | Rest timer after completing a set in a superset | **decided** | 2026-10-28: **Per-exercise rest** — timer uses rest seconds for the exercise whose set was just completed (Hevy-style per-movement rest in supersets). |
| D-013 | Muscle volume weighting | **decided** | 2026-10-28: Primary muscle **+1** set; each secondary **+0.5** (e.g. bench → +1 chest, +0.5 shoulders, +0.5 triceps). |
| D-014 | Unilateral logging | **decided** | 2026-10-28: **`side` on each set:** `left` \| `right` \| `both`. One row per set. Volume: **left/right** = load × reps for that side; **`both`** = **2×** (single-side load × reps, both sides counted). Column ships in schema v1 (**D-022**). |
| D-015 | Rest-timer alert mechanism | **decided** | 2026-10-28: **Combo:** foreground service while app recently used **+ exact alarm backup**. T-009 spike validates reliability on owner’s phone (permissions, battery). |
| D-016 | Unit-test style: JUnit4 vs JUnit5 for `:domain` | **decided** | 2026-10-28: **JUnit 5** for `:domain`. 2026-10-29: stay on JUnit 5; do not upgrade to JUnit 6 for now. |
| D-017 | Body weight storage and missing value | **decided** | 2026-10-28: One current body weight in **Settings** (T-017), stored in **kg**, default **unset**. No measurement history in the MVP (that stays Phase 2). Volume functions take `bodyweightKg` and do not read storage. Unset body weight → bodyweight/assisted volume **0**, no crash (TC-VOL-10). TC-VOL-06..09 are in T-002. |
| D-018 | Rest timer after the last set of the workout | **decided** | 2026-10-28: **None** (TC-REST-07). No alarm when the workout has no following set. Superset rest stays D-012; TC-REST-08 is in T-004. |
| D-019 | Previous-values alignment | **decided** | 2026-10-28: **Raw set index**, warm-ups included (TC-PREV-05). Repeated exercise in one history workout: **first occurrence** (TC-PREV-07). Routine-scoped mode (TC-PREV-03) stays Phase 2. |
| D-020 | Set Records rep bucket | **decided** | 2026-10-28: Best weight at **N or more** reps, not exactly N (TC-PR-07). A 100×8 counts toward the 5-rep record. Pure function in T-003; no Set Records UI in the MVP. |
| D-021 | How the app gets the catalogue | **decided** | 2026-10-28: **Both.** T-010 ships a **bundled snapshot** (same format and version field as the published catalogue) so first launch works offline. T-021 **full-syncs** GitHub Pages when online. Apply an incoming bundle only when its version is **newer**. **Upsert by stable id.** Never modify custom exercises. Do not hard-delete a published exercise a workout references; unreferenced exercises absent from the new bundle are archived. Sync failure leaves the current library and does not block startup. HTTP via the **platform API** (`HttpURLConnection`); no new HTTP library. Delta-sync stays post-MVP. |
| D-022 | `side` in the first Room schema | **decided** | 2026-10-28: `workout_set.side` is in **schema v1**, non-null, default **`both`**. No left/right control on the live-workout screen in the MVP. `partial_reps` and `rpe_target` stay out. |
| D-023 | Empty current week and the weekly streak | **decided** | 2026-10-28: An empty current week does **not** break the streak (TC-STREAK-05). Previous 3 weeks with a workout and nothing this week → streak **3**, alive until the week ends. A fully missed week before the current one still resets (TC-STREAK-02). Streak UI stays Phase 2; the rule is fixed for when it is built. |
| D-024 | Android Auto Backup of workout data | **decided** | 2026-10-29: **Include it.** `allowBackup` stays true and nothing excludes app data. The Room database stays in the default database directory, not `noBackup`. The manual JSON file (T-018) is separate. |
| D-025 | Exercise catalogue format and publish flow | **decided** | 2026-10-29 (T-007): Spec is **`docs/CATALOGUE.md`**. **JSON**, `formatVersion` 1, one file `catalogue/catalogue.json` that is both the bundled and the published bundle. Integer **`version`**, strictly greater than the base branch and the live file. **Slug ids** equal to `slug(title)` at creation, never changed or reused (`retiredIds`). Whole catalogue **CC0-1.0**; no Hevy or share-alike sources. Flow: PRs into `catalogue`, release PR `catalogue` → `main`, a push to `main` deploys Pages via Actions. URL: `https://t0liver.github.io/muskli/catalogue/v1/catalogue.json`; repo is public. CI schema check may use a pinned, CI-only **`check-jsonschema`**. |

## How to record a decision
Edit the row: set status, add the date and a one-line reason. If a decision changes code or tests, create/adjust tickets.
