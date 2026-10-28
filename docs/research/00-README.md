# Hevy Research Pack — Starting Point for a Personal Rebuild

**Research date:** 2026-10-26
**Goal:** Capture everything needed to start a planning session for building a *personal* workout tracker inspired by Hevy (not a business, not a competitor), to be built with AI agents.
**Subject:** Hevy — gym workout logger by Hevy Studios S.L. (Spain). Android package `com.hevy`.

---

## How to use this pack

Feed these files to the planning session in roughly this order:

| # | File | What it gives the planner |
|---|------|---------------------------|
| 01 | `01-product-overview.md` | What Hevy is, the idea behind it, positioning, business model, scale |
| 02 | `02-feature-inventory.md` | Exhaustive feature list + settings catalog, with tier and suggested priority |
| 03 | `03-core-workflows-and-ux.md` | Screen map, step-by-step flows, and *why it feels easy to use* |
| 04 | `04-data-model.md` | Entities, enums, relationships, and a proposed SQLite schema |
| 05 | `05-business-rules-and-algorithms.md` | Rules and formulas (rest timer, PRs, 1RM, warm-ups, plates, streaks, Trainer) with test cases |
| 06 | `06-public-api-reference.md` | What Hevy's public API exposes (useful for data migration and for copying its schema shape) |
| 07 | `07-pain-points-and-opportunities.md` | Known complaints/gaps + a section for **your** dislikes |
| 08 | `08-mvp-scope-and-roadmap.md` | Suggested scope cuts and phases for a personal app |
| 09 | `09-tech-and-agent-build-considerations.md` | Architecture options and how to structure the AI-agent build (suggestions, not findings) |
| 10 | `10-open-questions-and-decisions.md` | Unknowns to verify + decisions you need to make |
| 11 | `11-visual-capture-checklist.md` | Screenshots/recordings to take from your own Hevy app (I could not see UI images) |
| 12 | `12-sources.md` | Every source consulted, with reliability notes |

**Suggested prompt to start the planning session:**
> Read all files in `hevy-research/`. Treat 02 as the feature menu, 04/05 as the technical spec seed, 07 as the problem list, and 08 as my starting scope. Ask me only about the decisions in 10, then propose a build plan.

---

## Confidence tags used throughout

Every non-trivial claim is tagged so the planner knows how much to trust it:

- **[OFFICIAL]** — from Hevy's own website, Play Store listing, or help center pages I read.
- **[API]** — derived from third-party client libraries / MCP servers for Hevy's public API (the official docs page is JavaScript-rendered and did not load for me). Reliable for field names and enums; may lag the real API.
- **[3RD-PARTY]** — reviews, competitor blogs, forums. Useful signal, but may be biased, outdated, or wrong. Several are written by competitors.
- **[INFERENCE]** — my own reasoning or suggestion. Not a finding.

## Limits of this research (be aware)

1. **I could not use the app.** I only read text. Hevy's pages are full of screenshots that I could not see, so exact layouts, colors, icons, animations, and microcopy are **not** captured. File 11 tells you what to screenshot so the planning session can see them.
2. **Official API docs page (`api.hevyapp.com/docs`) did not render.** Schemas in file 04/06 come from open-source client libraries. Verify against the live docs (requires Hevy Pro to get a key).
3. **Some Hevy feature pages were only read at title level** (social feed, leaderboards, discovery feed, Strava, widgets, Live Activity, Hevy Trainer's own page). They are listed but not detailed. Details I *did* read are marked **[OFFICIAL]**.
4. **Numbers disagree between sources** (free-tier caps, Pro price, user counts). Discrepancies are called out in file 01 and file 10 instead of being silently resolved.
5. **Not legal advice.** See "Scope & IP notes" below.

## Scope & IP notes (short)

- This is a **personal-use** rebuild. Build your own code, your own UI, your own name and icon.
- Do **not** copy Hevy's proprietary assets: exercise videos/animations/images, its exercise-library content, logo, name ("Hevy" is a registered trademark per its site footer), or marketing copy.
- Reusing *ideas, feature concepts, and generic data shapes* (sets/reps/weight, set types, routines) is normal for fitness apps; these are industry-standard.
- I did **not** read Hevy's Terms & Conditions. Skim them before doing anything automated against Hevy (e.g., API use). The official API is Pro-only and documented for personal use with your own key.
- For your own data, the cleanest migration path is Hevy's in-app export (see file 08/10).
