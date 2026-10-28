# 09 — Tech & AI-Agent Build Considerations

> **Everything in this file is suggestion [INFERENCE], not research findings.** I do not know your platform, skills, or preferences. Hevy's own tech stack is **unknown** — I found nothing reliable about it. Use this as a checklist of decisions for the planning session.

## 1. Platform decision (your call)

You linked the Play Store, so I assume Android is your primary device. Options:

| Option | Pros | Cons | Fit |
|---|---|---|---|
| **Native Android (Kotlin + Jetpack Compose + Room)** | Best timers/notifications/background reliability; best-feeling UI on Android; Wear OS later is natural | Android-only | Strong if you only use Android |
| **Flutter** | One codebase iOS/Android; good UI control; SQLite (drift) mature | Dart; some native work for timers/notifications | Good cross-platform |
| **React Native + Expo** | Familiar TS stack; great AI-agent ecosystem; SQLite (expo-sqlite) | Background timer/notification edge cases need care | Good if you like TS |
| **PWA / web app** | Fastest to build and iterate with agents; installable | Weakest for background timers, notifications, keep-awake reliability | OK for prototype, risky for gym use |

**Rule of thumb [INFERENCE]:** if the rest timer and screen-off reliability matter (they do for a gym app), prefer native or Flutter/RN over PWA.

## 2. Architecture principles
- **Local-first:** SQLite as source of truth; no account needed.
- **Layered:** `domain/calc` (pure functions, fully tested) → `data` (repositories over SQLite) → `ui`. The calc layer should have zero platform dependencies so agents can test it quickly.
- **Single-writer live-workout state machine:** a live workout is a draft that persists on every change (crash-safe), then "finalizes" on Finish.
- **Backup/sync (optional):** start with file export/import (JSON) + optional auto-backup to a cloud folder; add real sync only if you need multi-device. If you do, design with soft-deletes + `updated_at` + an events/changes feed (Hevy's API exposes deletions via an events feed, a hint at their sync model **[API]**).

## 3. Exercise library — content you can legally use
Hevy's library, videos and images are proprietary. Options:
1. **Open datasets** (verify licenses & attribution before use): candidates include `yuhonas/free-exercise-db` (JSON, with images; reported as public-domain-style) and `wger` (open-source fitness manager with an exercise database; license terms vary by content). *I have not verified current licenses.*
2. **Generate your own** with an LLM and review — title, type, primary/secondary muscle, equipment — map to the enums in file 04. No media at first.
3. **Import your own exercises from Hevy** (your custom ones) via the API/export.

Map any dataset into Hevy-like enums: 8 exercise types, 20 muscle groups, 9 equipment categories (file 02/04), so your importer and stats logic stay simple.

## 4. Key technical risks & notes

| Area | Note |
|---|---|
| **Rest timer when screen off** | Use platform notification/alarm APIs; test with battery optimization on |
| **Keep-awake** | Native "keep screen on" flag; make it a setting |
| **Float math** | Do plate math in integer grams/hundredths; store kg as REAL but round at display |
| **Timezones** | Streak/calendar use local weeks; store UTC timestamps plus the timezone offset for the workout |
| **Charts** | Use an off-the-shelf chart lib; compute series in SQL or in the repository layer |
| **Performance** | A personal dataset (thousands of sets) is tiny — compute records/stats on demand with indexes; avoid premature caching |
| **Migrations** | Version the schema from day one |
| **Accessibility/ergonomics** | Big tap targets, one-handed use, sweaty-hands tolerance, dark mode |

## 5. Structuring the work for AI agents [INFERENCE]

Why: agents are strongest with **clear specs, small vertical slices, and executable tests.**

1. **Repo docs as the spec.** Put this research pack under `/docs/research/` and add `SPEC.md` (product statement + MVP scope), `DOMAIN.md` (rules from file 05, decisions from file 10), `ARCHITECTURE.md`.
2. **Tests-first for domain logic.** Seed the test suite from file 05's test cases; agents implement until green.
3. **Vertical slices.** One slice = UI + state + persistence + tests for one user story (e.g., "complete a set → rest timer starts").
4. **Short, explicit task briefs** with acceptance criteria; ask agents to propose a plan before coding.
5. **Design tokens + one reference screen first.** Agents drift visually; lock down the live-workout screen design (from your screenshots, file 11) and reuse components.
6. **Golden data fixtures.** A realistic JSON of ~20 workouts (can be produced from your Hevy export) to test stats, charts, previous values, PRs.
7. **Review gates.** You approve schema changes and any new dependency; agents handle implementation.
8. **Keep an `AGENTS.md`/`CLAUDE.md`** with conventions: language, folder layout, how to run tests, "never add a tap to the logging flow without a setting", etc.

## 6. Optional: an MCP/API layer over your own data
Hevy's ChatGPT integration and the community's Hevy MCP servers show strong demand for "ask an AI about my training". In your app, a **local MCP server** (read workouts, exercise history, routines; create routines) gives the same benefit with no subscription and full privacy. Treat it as Phase 3+.

## 7. Questions the planning session should settle first
See file 10, section "Decisions for you".
