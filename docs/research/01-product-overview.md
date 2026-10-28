# 01 — Product Overview

## The idea in one paragraph

Hevy is a **gym workout logger** whose core promise is: *logging a set should take almost no effort, and seeing last time's numbers should be automatic.* Around that core loop it layers **progress analytics** (charts, PRs, muscle-volume stats, body measurements) and an **optional social layer** (follow friends, feed, leaderboards). It targets people who lift weights and self-program (or follow a split like PPL / Upper-Lower), not people who want to be told what to do. **[OFFICIAL, 3RD-PARTY]**

## The three pillars (Hevy's own framing) [OFFICIAL]

1. **Workout logging** — fast, flexible, detailed (set types, supersets, rest timers, notes).
2. **Progress tracking** — per-exercise stats, PRs, muscle-group volume, body measurements, photos, calendar/streaks, monthly & yearly reports.
3. **Socializing** — follow others, like/comment, copy routines, leaderboards, share cards.

For a personal rebuild, **pillars 1 and 2 are the product. Pillar 3 is the part to drop or radically reduce.**

## Design philosophy (what the store listing and docs emphasize) [OFFICIAL]

- "Simplest, most intuitive workout tracker" — simplicity is the stated north star.
- No ads. Free tier is permanent (no trial countdown). **[OFFICIAL + 3RD-PARTY]**
- Heavy **customizability behind toggles**: advanced features (RPE, warm-up calculator, plate calculator, superset scrolling, inline timer, live PR) are *opt-in settings*, so the default screen stays clean. This is a key UX pattern to copy.
- Built by lifters: set types (warm-up/drop/failure) and supersets are first-class, not afterthoughts. **[3RD-PARTY review quotes]**

## Who uses it / what for

- Powerlifting, bodybuilding, general strength, 5x5/StrongLifts, calisthenics, some cardio/HIIT logging. **[OFFICIAL — Play Store text]**
- Splits explicitly supported by content/guides: 3/4/5-day, PPL, Upper/Lower, bro split, Arnold split, powerbuilding. **[OFFICIAL]**
- Primary device: phone at the gym, in-between sets. Secondary: smartwatch (log from wrist), web (planning/analysis on a big screen). **[OFFICIAL]**

## Platforms & surfaces [OFFICIAL unless noted]

| Surface | Notes |
|---|---|
| iOS app | Main platform; Live Activity, home-screen widgets, iMessage stickers |
| Android app | Package `com.hevy`; last update shown on Play Store: 2026-10-21 |
| Apple Watch | Log sets, use routines, offline, sync with phone |
| Wear OS | Log sets, live sync with phone, tile, heart rate, duration timers, set types; watch workouts auto-save when reconnecting to phone |
| Web (hevy.com) | Create/plan routines, analyze exercise progress, see friends' workouts |
| Public API | Pro-only; API key from web settings; `api.hevyapp.com` (see file 06) |
| ChatGPT app (`@hevy`) | Analyze history, generate programs, save routines back; replaced the older "HevyGPT" custom GPT **[3RD-PARTY, citing Hevy]** |
| "Send to ChatGPT/Claude" button | Added on finish-workout screen in v3.1.13 (Sept 2026) **[3RD-PARTY]** |
| Hevy Coach | **Separate** B2B product for personal trainers; clients receive programs inside the Hevy app, can chat with coach. Out of scope here. |

## Business model

- **Free tier:** unlimited workout logging, full exercise library, rest timer, social feed, PRs, ChatGPT app. **[3RD-PARTY]**
- **Pro** (subscription or lifetime): removes caps on routines/custom exercises/history window; unlocks Hevy Trainer; advanced body measurements; API access. **[3RD-PARTY + OFFICIAL]**
- Typical prices reported (US, 2026): **$2.99/mo, $23.99/yr, $74.99 lifetime** (a second $3.99/mo SKU also appears). One travel/phone-plan site quotes "$4.99/mo" — likely outdated or a different SKU. **[3RD-PARTY, conflicting]**
- Not relevant to a personal app, but it tells you **what Hevy itself considers "premium"** = unlimited routines/custom exercises, long history, auto-progression, API. In your version, all of these are just free.

### Free-tier limits — sources disagree

| Limit | Value reported | Source note |
|---|---|---|
| Routines | **4** | SensAI blog, Hevy-vs-Strong post (2026) |
| Routines | **7** | Android Authority/WhistleOut review |
| Custom exercises | **7** | SensAI; an iOS reviewer says they hit a cap at ~5 |
| Graph/stat history | **3 months** (Pro: 1 year / all time) | Hevy's own feature page confirms 3 months for free |

Treat "4 routines / 7 custom exercises / 3-month history" as the most-cited, but **verify in the app** — Hevy appears to have changed caps over time.

## Scale & reputation (vendor claims) [OFFICIAL]

- "17+ million athletes" on the homepage (another source quotes 16M+). Ratings ~4.9 on both stores; Play Store shows ~282K reviews and 5M+ downloads. Treat as marketing numbers.
- Team is small (Spain-based studio, founded ~2019 **[3RD-PARTY]**), shipping frequent incremental updates. Latest Play Store release notes: rebuilt **notes** (pin a note to an exercise or routine; one-off note per workout) and share to Snapchat.

## Why people like it (synthesis) [3RD-PARTY + OFFICIAL]

1. Previous-session numbers auto-appear in the logging screen → progressive overload is effortless.
2. Logging is quick: complete a set with one tap, timer starts automatically.
3. Enough depth (drop/failure/warm-up sets, supersets, RPE, notes) without clutter.
4. Good exercise demos (videos/animations) and a big library with filters.
5. Charts are clear and per-exercise; muscle-volume stats are genuinely useful for hypertrophy training.
6. Works on Android and watches; generous free tier; social is skippable.

## Takeaway for the rebuild [INFERENCE]

The "magic" is concentrated in a small number of details — **prefilled previous values, one-tap set completion, automatic rest timer, flexible set types, and clean per-exercise history**. Get those right first; everything else (reports, social, trainer) is optional garnish. See file 03 for the detailed list of what creates the "easy to use" feeling.
