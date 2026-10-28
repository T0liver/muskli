# 12 — Sources & Reliability Notes

Research date: **2026-10-26**. URLs are as consulted. Reliability: **High** = Hevy's own pages; **Medium** = open-source code/official-adjacent; **Low** = competitor/affiliate/third-party commentary or user comments.

## Official (Hevy)

| Source | URL | Used for | Reliability |
|---|---|---|---|
| Hevy homepage | https://www.hevyapp.com/ | Pillars, platforms, claims (17M+ athletes, ratings), navigation of site | High (marketing tone) |
| Feature index | https://www.hevyapp.com/features/ | Complete list of feature categories/pages | High |
| Log & Track Workouts | https://www.hevyapp.com/features/track-workouts/ | Routines vs live workouts, set controls, finish flow, library, previous values, plate calc, PR notifications | High |
| Workout Set Types | https://www.hevyapp.com/features/workout-set-types/ | Set type definitions; drop-set/timer rule | High |
| Workout Settings | https://www.hevyapp.com/features/workout-settings/ | Settings catalog (12 settings) | High |
| Exercise Programming Options | https://www.hevyapp.com/features/exercise-programming-options/ | Per-exercise options, notes, rest timer, rep ranges, RPE, 400+ exercises | High |
| Warm-up Set Calculator | https://www.hevyapp.com/features/warm-up-set-calculator/ | Warm-up behavior and settings (default ladder was an image) | High |
| Track Gym Progress | https://www.hevyapp.com/features/gym-progress/ | Statistics, exercise performance, measurements, calendar, monthly report, Year in Review | High |
| Google Play listing | https://play.google.com/store/apps/details?id=com.hevy | Feature bullets, Wear OS, ratings (4.9, ~282K), 5M+ downloads, data safety, latest release notes (Oct 2026) | High |
| Public API docs | https://api.hevyapp.com/docs | **Did not render** (JS app) | — |

Hevy pages I **did not** open (title-level only): social features, leaderboards, discovery feed, Strava, widgets, Live Activity, Hevy Trainer, rest timer, supersets, plate calculator, exercise library, year in review, help-center articles, Terms/Privacy, Hevy Coach.

## Open-source clients of the Hevy API (schema evidence)

| Source | URL | Used for | Reliability |
|---|---|---|---|
| Rust client models | https://docs.rs/crate/hevy/latest/source/src/models/ (workout.rs, routine.rs, exercise_history.rs, common.rs, exercise_template.rs) | Set fields, enums, template fields, history entries | Medium |
| Go CLI API package | https://pkg.go.dev/github.com/obay/hevycli/internal/api | Create-request shapes; base URL | Medium |
| Python wrapper | https://pypi.org/project/hevy-api-wrapper/ | Endpoints; header name `api-key`; Pro requirement | Medium |
| MCP server docs (diecoscai) | https://glama.ai/mcp/servers/diecoscai/hevy-mcp | Enum lists; no DELETE; pageSize ≤ 10; folder_id rule; body measurements | Medium |
| MCP types docs | https://www.mintlify.com/imalfect/hevy-http-mcp/ | TS interfaces; full-object PUT; workout events | Medium |
| Serval integration docs | https://docs.serval.com/sections/integrations/hevy | Pro requirement; auth | Medium |
| OpenClaw/“Hevy CLI” skill | https://playbooks.com/skills/openclaw/skills/hevy | Endpoint list; can't edit/delete templates | Low–Medium |

## Third-party reviews & comparisons

| Source | URL | Used for | Reliability / bias |
|---|---|---|---|
| SensAI — Hevy Review 2026 | https://www.sensai.fit/blog/hevy-review-2026 | Pricing, free/Pro limits, Trainer details, ChatGPT integration, export/import note, limitations | **Low–Medium; competitor** (sells an AI coach); extensive and detailed but self-interested |
| SensAI — Hevy vs Strong | https://www.sensai.fit/blog/hevy-vs-strong-2026 | Tier comparison | Same bias |
| SensAI — pricing breakdown | https://www.sensai.fit/blog/fitness-app-pricing-free-tier-comparison | Price table | Same bias |
| Boostcamp — Hevy alternatives | https://www.boostcamp.app/alternatives/hevy | Complaint themes (caps, no named programs, social) | Low; competitor |
| Android Authority/WhistleOut review | https://cellphoneplans.androidauthority.com/CellPhones/Apps/hevy-app-review | Free tier summary (note: quotes 7 routines/$4.99) | Low; affiliate; some figures inconsistent |
| Dr. Muscle review | https://dr-muscle.com/hevy-workout-app-review/ | Complaint themes | Low; competitor |
| Garage Gym Reviews | https://garagegymreviews.com/equipment/hevy | Pros/cons | Low |
| AlternativeTo | https://alternativeto.net/software/hevy-workout-tracker/about | User comments (offline complaint) | Low (anecdotal) |
| Clarity AI report | https://www.onclarity.com/leaderboard/insight/hevy | Half-rep & pre-loaded exercise themes | Low (AI-summarized) |
| r/Hevy (mirror) | https://redlib.groet-infra.nl/r/Hevy/new | Unilateral logging complaints | Low (anecdotal, but real users) |
| Hevy Coach on G2 / GetApp | https://www.g2.com/products/hevy-coach/reviews · https://www.getapp.com.au/software/2071025/hevy-coach | Coach product gaps | Low |
| App Store listing (snippet) | https://apple.co/2VvCsFt | Pro price points, feature bullets | Medium |
| Wikipedia — One-repetition maximum | https://en.wikipedia.org/wiki/One-repetition_maximum | Epley/Brzycki formulas | Medium (general math, not Hevy-specific) |

## Not found / not verified
- Hevy's tech stack, architecture, sync design.
- Hevy's exact 1RM formula, default warm-up ladder, strength-standard source, volume rules for bodyweight exercises.
- Hevy's Terms of Service.
- The live API documentation.
