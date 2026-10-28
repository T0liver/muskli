# 07 — Pain Points, Gaps & Opportunities

You said there are features you *don't* like about Hevy. I don't know which ones yet, so this file has two parts:
- **Part A:** problems and gaps other people report (so the planning session has candidates).
- **Part B:** a template for **your** dislikes — fill this in; it's the most important input for deciding what to change.

Source reliability: many items come from competitor blogs or single user comments **[3RD-PARTY]**. Treat them as hypotheses, not facts.

---

## Part A — Reported problems & gaps

### A1. Logging model gaps
| Issue | Evidence | Opportunity in your version |
|---|---|---|
| **Unilateral (left/right) exercises** are awkward; volume can be inaccurate. Users say Hevy's team is working on it. | Reddit r/Hevy thread (Oct 2026) | Add a `side` field per set (`both/left/right`) and define volume rules; UI: "L/R" toggle or paired rows |
| **Half/partial reps** not trackable | AI-summarized review report (Clarity) | Add `partial_reps` (or fractional reps) per set |
| **RPE cannot be a target in routines**, only logged live; unavailable on duration exercises | Hevy docs | Support `rpe_target` / RIR in routine sets |
| **Rest time per exercise isn't remembered** when starting empty workouts (one reviewer) | Play Store review | Store `default_rest_s` on the exercise template; use it everywhere |
| **Plate calculator only for barbells** (no dumbbells, machines, per-side options) | Hevy docs | Broader load calculators; per-side / total toggle |
| **Warm-up calculator** limited to percent ladder | Hevy docs | Smarter ladders (by working weight, equipment, first-exercise-only) |
| **Date picker bug** on weight entry (year keeps scrolling back) | Play Store review (Sept 2026) | Test date UI thoroughly |

### A2. Data & platform limitations
| Issue | Evidence | Opportunity |
|---|---|---|
| **Offline**: one AlternativeTo user says logging needs internet. Hevy's watch apps work offline and a competitor review calls Hevy "not offline-first" by contrast with FitNotes. *Unverified.* | AlternativeTo comment; WhistleOut review | **Offline-first by design** (local DB is source of truth) — trivial for a personal app and a real differentiator |
| **Free-tier caps** (routines, custom exercises, 3-month analytics window) | Multiple reviews | Non-issue for a personal app — you own everything |
| **API is Pro-only, no deletes, full-object PUT, can't edit templates** | Client authors | Your own data layer is fully editable |
| **Data portability**: export exists; import supports Strong CSV | Help-center article (via review) | Lossless full-fidelity export (JSON + CSV) as a first-class feature |
| **Not many pre-loaded exercises** for ~10% of users | AI-summarized report | Own library + easy custom exercise creation (unlimited) |

### A3. Programming/coaching limitations
| Issue | Evidence | Opportunity |
|---|---|---|
| **Trainer = simple double progression**, no recovery inputs, no explanation of plateaus | SensAI blog (competitor; biased) | Optional rules engine: double progression + deload detection; or an AI/MCP-driven coach over your own data |
| **Chat lives outside the app** (ChatGPT/Claude), data handed over via a button | Hevy docs/blog | Local MCP server or built-in "ask your data" over your DB |
| **Social layer** forced on users who don't want it | Boostcamp comparison (competitor) | Not applicable; you won't build it |

### A4. Business-model-driven friction
Pro upsell for charts older than 3 months, more routines, auto-progression, extra measurements. **[3RD-PARTY]** All disappear in a personal app.

### A5. Hevy Coach (separate product) complaints
Lacks nutrition integration, group messaging, scheduling, payments. **[3RD-PARTY, G2/GetApp]** Out of scope.

---

## Part B — **Your** dislikes (please fill in)

> Fill this in before the planning session, even roughly. For each item, say what annoys you and what you'd want instead.

| # | Hevy feature / behavior you dislike | Why it bothers you | What you'd want instead | Severity (1–5) |
|---|---|---|---|---|
| 1 | | | | |
| 2 | | | | |
| 3 | | | | |
| 4 | | | | |
| 5 | | | | |

**Prompts to jog your memory:**
- Logging screen: too many taps? clutter? wrong defaults? hard-to-hit targets?
- Charts: missing metrics? wrong time windows? can't compare what you want?
- Routines: awkward editing? folder organization? can't plan progressions/deloads/mesocycles?
- Timers/notifications: behavior while locked/backgrounded?
- Stats: how muscle volume is attributed (primary/secondary)? how PRs are defined?
- Exercise library: naming, duplicates, missing exercises, equipment categories?
- Social: feed noise, privacy, notifications?
- Anything you wish it did that it doesn't (nutrition? sleep? cardio? body-recomp dashboards? calendar planning?)

## Part C — Design opportunities that follow from A (summary) [INFERENCE]

1. **Offline-first, local-first, with your own export/backup** — removes a whole class of complaints.
2. **Richer set model** — unilateral side, partial reps, RIR/RPE targets, tempo, per-set notes — as optional fields.
3. **Programming layer** — mesocycle/periodization planner, deload suggestions, progression rules per exercise (beyond rep-range double progression).
4. **Analytics freedom** — any time window, any metric, custom muscle weighting, e1RM trendlines, volume landmarks (MEV/MAV/MRV) if you care.
5. **AI-ready data** — local MCP/API so an assistant can read history and propose programs; keeps data private and uncapped.
6. **Quality bar from Hevy to keep** — previous values, auto rest timer, set types, supersets, previous-value modes, retroactive logging.
