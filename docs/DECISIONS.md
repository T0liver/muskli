# DECISIONS.md

Lightweight decision log. Statuses: **decided** (owner confirmed) · **proposed** (default agents may assume *only for planning*, still needs confirmation before it shapes code) · **open** (owner must choose; agents must ask).

Agents: never silently resolve an *open* item. Add new decisions with the next free ID.

| ID | Decision | Status | Notes |
|---|---|---|---|
| D-001 | Native Android, Kotlin, Jetpack Compose | **decided** | Owner choice |
| D-002 | Room/SQLite as local source of truth; no backend for MVP | **decided** | 2026-10-28: Room required for MVP — logger/diary must persist locally (see `:data` / T-006). |
| D-003 | Default 1RM formula = Epley; formula is a parameter of `estimateOneRepMax` | **decided** | 2026-10-28: Owner approved default; Hevy formula still unknown (research 10, A2). |
| D-004 | Three Gradle modules: `:domain` (pure Kotlin), `:data`, `:app` | **decided** | 2026-10-28: Keep `:app` (Android *application* module: Compose UI, ViewModels, Hilt, services) — not renamed to `:ui`. |
| D-005 | Exercise catalogue source & distribution | **decided** | 2026-10-28: Dedicated **`catalogue` branch** — exercises added as JSON/TOML (exact format via T-007). PR → **CI validates** schema → merge updates canonical catalogue on `main` → publish **static API on GitHub Pages**. App **full sync** for MVP; **delta-sync** optional post-MVP. User custom exercises stay local. |
| D-006 | DI: Hilt vs. manual constructor injection | **decided** | 2026-10-28: **Hilt** — set up now vs. rewrite later. |
| D-007 | minSdk | **decided** | 2026-10-28: **minSdk 24**; targetSdk = latest stable. |
| D-008 | App name and package id | **decided** | 2026-10-28: **Muskli** · `hu.toliver.muskli`. No Hevy branding. |
| D-009 | Defaults: kg, 90 s rest, previous = "anywhere", keep awake on, week starts Monday | **decided** | 2026-10-28: Approved defaults; all user-changeable in settings (T-017). |
| D-010 | Volume rules for completed sets; warm-ups excluded from stats by default | **decided** | 2026-10-28: **weight_reps:** weight_kg × reps. **Bodyweight:** (latest_bodyweight_kg + added_weight_kg) × reps (profile body weight). **Assisted:** (latest_bodyweight_kg − assistance_kg) × reps. Warm-up exclusion unchanged; setting later. |
| D-011 | Finishing a workout discards uncompleted sets after confirmation | **decided** | 2026-10-28: Owner approved. |
| D-012 | Rest timer after completing a set in a superset | **decided** | 2026-10-28: **Per-exercise rest** — timer uses rest seconds for the exercise whose set was just completed (Hevy-style per-movement rest in supersets). |
| D-013 | Muscle volume weighting | **decided** | 2026-10-28: Primary muscle **+1** set; each secondary **+0.5** (e.g. bench → +1 chest, +0.5 shoulders, +0.5 triceps). |
| D-014 | Unilateral logging | **decided** | 2026-10-28: **`side` on each set:** `left` \| `right` \| `both`. One row per set. Volume: **left/right** = load × reps for that side; **`both`** = **2×** (single-side load × reps, both sides counted). |
| D-015 | Rest-timer alert mechanism | **decided** | 2026-10-28: **Combo:** foreground service while app recently used **+ exact alarm backup**. T-009 spike validates reliability on owner’s phone (permissions, battery). |
| D-016 | Unit-test style: JUnit4 vs JUnit5 for `:domain` | **decided** | 2026-10-28: **JUnit 5** for `:domain`. |

## How to record a decision
Edit the row: set status, add the date and a one-line reason. If a decision changes code or tests, create/adjust tickets.
