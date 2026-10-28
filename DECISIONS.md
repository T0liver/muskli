# DECISIONS.md

Lightweight decision log. Statuses: **decided** (owner confirmed) · **proposed** (default agents may assume *only for planning*, still needs confirmation before it shapes code) · **open** (owner must choose; agents must ask).

Agents: never silently resolve an *open* item. Add new decisions with the next free ID.

| ID | Decision | Status | Notes |
|---|---|---|---|
| D-001 | Native Android, Kotlin, Jetpack Compose | **decided** | Owner choice |
| D-002 | Room/SQLite as local source of truth; no backend for MVP | proposed | |
| D-003 | Default 1RM formula = Epley; formula is a parameter of `estimateOneRepMax` | proposed | Hevy's real formula unknown (research 10, A2). Owner can check in Hevy and switch. |
| D-004 | Three Gradle modules: `:domain` (pure Kotlin), `:data`, `:app` | proposed | Fast `:domain:test` for agents |
| D-005 | Exercise seed source | **open** | Options: open dataset (verify license), LLM-generated + reviewed, import owner's Hevy custom list. Decided by ticket T-007 (SPIKE). |
| D-006 | DI: Hilt vs. manual constructor injection | **open** | Manual is simpler for a small app; Hilt is conventional. Owner picks before T-001 finishes. |
| D-007 | minSdk | **open** | Owner: what Android version is your phone / oldest you care about? targetSdk = latest stable. |
| D-008 | App name and package id | **open** | Must not use "Hevy" or its branding. |
| D-009 | Defaults: kg, 90 s rest, previous = "anywhere", keep awake on, week starts Monday | proposed | Matches Budapest/metric assumption; confirm. |
| D-010 | Volume = sum of weight_kg x reps over **completed** sets; warm-ups **excluded** from stats by default (setting later) | proposed | Bodyweight/assisted volume rule **open** until checked in Hevy (research 10, A4) |
| D-011 | Finishing a workout discards uncompleted sets after confirmation | proposed | Check how Hevy behaves |
| D-012 | Rest timer start rule in supersets (after each set vs. after the round) | **open** | Check in Hevy (research 10, A6); not needed until supersets (Phase 2) |
| D-013 | Muscle volume weighting (primary 1, secondary 0 or 0.5) | **open** | Phase 3 |
| D-014 | Unilateral logging model (`side` per set vs. paired rows) | **open** | Phase 3 |
| D-015 | Rest-timer alert mechanism (foreground service / exact alarm) | **open** | Decided by spike T-013 |
| D-016 | Unit-test style: JUnit4 vs JUnit5 for `:domain` | **open** | Small; decide in T-001 |

## How to record a decision
Edit the row: set status, add the date and a one-line reason. If a decision changes code or tests, create/adjust tickets.
