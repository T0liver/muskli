# 10 — Open Questions & Decisions

Two lists: **(A)** things I could not determine and that you can check in minutes inside your own Hevy app, and **(B)** decisions you need to make so the planning session can proceed.

---

## A. Things to verify in the real app (facts I couldn't confirm)

| # | Question | Why it matters | Where to look |
|---|---|---|---|
| A1 | What is Hevy's **default warm-up ladder** (percentages and reps)? | Seed for your warm-up calculator | Settings → Workouts → Warm-up Calculator → Warmup Method |
| A2 | What **1RM formula** does Hevy use? (Compare a known set against Epley/Brzycki: e.g., 100 kg × 6 → Epley 120.0 / Brzycki 116.1) | PR logic must match your history | Any exercise → Summary |
| A3 | Do **secondary muscles** count as full sets in "sets per muscle group"? | Defines stats semantics | Log a bench set; check chest/shoulders/triceps counts |
| A4 | How are **bodyweight / assisted** exercises counted in volume? | Volume definition | Log pull-ups; look at volume |
| A5 | Do **warm-up sets** count in the live "sets" counter and in PRs by default? | Rules in file 05 §1 | Live workout header & PR toasts |
| A6 | Does **completing a set in the middle of a superset** start the rest timer immediately or after the round? | Rest timer rules | Make a 2-exercise superset |
| A7 | What happens when you **complete a set while the timer is running**? Can you ±15 s? | Timer UX | Live workout |
| A8 | Is there **pause/resume** of the rest timer, and is it shown in a notification/lock screen? | Android notification design | Lock the screen during rest |
| A9 | **PR types** shown (heaviest weight, 1RM, volume, reps, duration) and the exact wording/ordering | Feature parity | Exercise detail + live PR toast |
| A10 | **Set Records**: best weight at exactly N reps, or "N or more"? | Query semantics | Exercise detail → Set Records |
| A11 | The exact **14 circumference measurements** and the weight/fat/lean fields | Body schema | Profile → Measures → + |
| A12 | **Streak** logic: does the current empty week break the streak? First-day-of-week effect? | Streak function | Calendar |
| A13 | Does the **API expose rep ranges and rest times** on routine exercises? Fields? | Routine schema | Live docs at `api.hevyapp.com/docs` or a test call |
| A14 | **Offline behavior:** can you log a full workout in airplane mode and sync later? | Whether "offline-first" is truly a differentiator | Airplane mode test |
| A15 | **Export format** (CSV? columns?) and whether it includes routines, measurements, notes | Importer design | Profile → Settings → Export data (help center article exists) |
| A16 | Current **free-tier caps** (routines? custom exercises? history window?) | Context only | Hevy Pro/pricing screen |
| A17 | Does **drop-set** change the *previous* values alignment or volume? | Rules | Log a drop-set exercise twice |
| A18 | How are **superset** exercises displayed and re-ordered, and can supersets be created in routines and live workouts? | UI spec | Both flows |
| A19 | **Bottom tab list** and where Discover/Feed lives | Nav map | App home |
| A20 | What does the **"copy workout"** do with notes/supersets/warm-ups? | Copy semantics | Copy a workout |

## B. Decisions for you (the planning session should ask these first)

| # | Decision | Options / my default suggestion |
|---|---|---|
| B1 | **Target platform(s)** | Android only (native or Flutter/RN) vs. Android+iOS; default: Android-first |
| B2 | **Tech stack preference** | Kotlin/Compose · Flutter · React Native/Expo · PWA — see file 09 |
| B3 | **Backend/sync?** | None (local + file backup) vs. self-hosted/cloud sync. Default: none for MVP |
| B4 | **Continue your Hevy history?** | Import via in-app export or API (needs Pro). Default: yes, build an importer |
| B5 | **Exercise library source** | Open dataset vs. LLM-generated vs. import of your own list. Default: open dataset + your custom exercises (verify license) |
| B6 | **Exercise media (videos/images)** | None initially; later open/own media |
| B7 | **Your top dislikes of Hevy** | Fill file 07 Part B |
| B8 | **Training style** | Hypertrophy / strength / calisthenics / mixed → affects stats (muscle volume vs. 1RM), set model, programming |
| B9 | **Units & locale** | kg only vs. kg/lb; language; week start day (you're in Budapest — likely metric, Monday start) |
| B10 | **Muscle volume semantics** | Primary only vs. primary + 0.5×secondary |
| B11 | **Unilateral logging model** | `side` per set vs. paired rows vs. a "per-side" flag; volume = sum of sides? |
| B12 | **Programming features** | Just routines? progression rules? mesocycles/deloads? AI coach? |
| B13 | **Wearables** | Needed soon? (Wear OS/Apple Watch) |
| B14 | **Cardio & non-lifting** | In scope? (Hevy has some duration/distance types; it's a lifting app first) |
| B15 | **Design direction** | Clone Hevy's look & feel vs. your own identity. (Own name/icon/assets either way) |
| B16 | **Notifications & timers** | Sound/vibration preferences; behavior when screen is off |
| B17 | **Privacy/security** | Local-only is simplest. Any cloud backup needed? Encrypted? |
| B18 | **Success criteria** | e.g., "I log a full workout faster than in Hevy"; "I can import 2 years of history" |

## C. Discrepancies noted across sources (for transparency)

| Topic | Conflict | Treatment |
|---|---|---|
| Free routine cap | 4 vs. 7 | Verify (A16); not needed for your app |
| Pro price | $2.99/mo vs. $4.99/mo (and a $3.99 SKU) | Irrelevant to build |
| User count | 17M+ (homepage) vs. 16M+ (third party) | Marketing numbers |
| Exercise count | 400+ (docs) vs. 200+ videos (older store text) | Library is "hundreds" |
| Routine library categories | "eight" vs. "seven" categories (same page) | Minor |
| Offline | Reviewer says internet required; watch apps work offline | Verify (A14) |
| Pre-built programs | Competitor says none; Hevy docs list 26 programs | Hevy docs win |
| Company location | Play Store: Calonge i Sant Antoni; blog: "Barcelona" | Irrelevant |
