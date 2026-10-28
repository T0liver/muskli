# 03 — Core Workflows & UX

> **Caveat:** Everything here comes from text descriptions. I could not see screenshots, so layout/visual specifics are missing. Use file 11 to capture them from your own phone.

## 1. Navigation map (as far as verified)

Verified from Hevy's docs [OFFICIAL]:

```
Bottom navigation (names verified: Workout, Profile; a Home screen also exists)
│
├── Home            → feed (social); Year-in-Review card appears here in December
├── Workout tab
│     ├── "+ Start Empty Workout" (top)
│     ├── Routines (grouped in folders) → each: Start Routine / 3-dot: Edit, etc.
│     ├── "New Routine"
│     └── "Explore" → program & routine library (filters: experience, goal, equipment)
└── Profile tab
      ├── Statistics   (7-day body graph, sets per muscle, muscle distribution, main exercises)
      ├── Exercises    (library → exercise detail: Summary / Set Records / Strength Level / History)
      ├── Measures     (weight, body fat, 14 circumferences, progress photos; "+" to add)
      ├── Calendar     (workout days, streak, year view; "+" to log past workout)
      ├── Monthly report / Year in Review
      ├── Workout history list (newest→oldest) → each with likes/comments and 3-dot: Create routine / Copy / Edit / Delete
      └── Settings (gear, top right) → Workouts (preferences), Units, etc.
```

There may be additional tabs (e.g., discovery/feed) I did not verify.

## 2. The primary loop: logging a workout

### 2.1 Entry points (3 ways)
1. **Start Empty Workout** — blank session, add exercises as you go.
2. **Start Routine** — prefilled template.
3. **Copy workout** — re-run a past workout (yours or someone else's).

### 2.2 Live workout screen — anatomy [OFFICIAL, text-derived]

```
┌ Top bar ──────────────────────────────────────────┐
│ ⏱ stopwatch (tap: edit duration/start time/pause)  │
│ sets completed · volume load            [Finish]   │
├ Exercise card (repeat per exercise) ───────────────┤
│ Exercise name (tap: stats/demo)      ⋯ menu        │
│ note area (routine note / session note / pinned)   │
│ rest-timer selector                                 │
│ SET │ PREVIOUS │ KG/LBS │ REPS │ [RPE] │ ✓         │
│  1  │ 60×10    │  [ ]   │ [ ]  │       │ ○         │
│  W  │ …  (set number cell shows W / D / F for types)│
│ + Add Set                                           │
├────────────────────────────────────────────────────┤
│ + Add Exercise (blue)   |  Settings (grey)          │
└────────────────────────────────────────────────────┘
Keyboard has a "Calculator" button (plate calc) for barbell exercises.
```

Column set varies by exercise type: weight+reps, reps only, time, distance, etc. **[API-derived inference]**

### 2.3 What happens on each tap
- **Tap set number** → menu: Warm-up / Normal / Failure / Drop / Delete set.
- **Swipe left on a set** → red Delete.
- **Tap ✓ on a set** → set locked as done → **rest timer starts** (unless next set is a Drop set) → optional click sound → if it's a record and Live PR is on, a PR toast appears.
- **Tap "KG/LBS" header** → switch unit for this exercise. **Tap "REPS" header** → switch to rep range (routines).
- **Tap ⋯ on exercise** → Reorder (drag), Replace, Add to Superset, Add Warm-up Sets, Remove.
- **Tap Settings (bottom)** → in-workout shortcut to the same settings under Profile → Workouts.
- **Superset + smart scrolling** → completing a set scrolls to the next exercise in the superset.

### 2.4 Finish flow
1. Tap **Finish** (top right).
2. Save screen: name, stats (volume, sets), editable duration, editable start date/time, media (≤3 photos or 2 photos + 1 video), note (+ tag users), private toggle.
3. Save → **celebration screen**: workout ordinal (e.g., "472nd"), active weekly streak, PR highlights, share buttons, 7-day consistency recap.
4. Workout appears at top of Profile history.

### 2.5 Editing history
Profile → workout → ⋯ → Edit Workout (change any detail: add/remove exercises & sets, notes, RPE) or Delete. Calendar "+" creates a retroactive workout.

## 3. Routine creation flow
Workout tab → **New Routine** → pick folder (if >1) → name → add exercises → per exercise: sets, set types, weight, reps/rep-range/time, rest timer, note, superset, warm-ups → Save. Routine appears under Routines. Programs from the library arrive as folders and are fully editable.

## 4. Progress browsing flows
- **Exercise-centric:** Profile → Exercises → pick exercise → Summary (records) → Set Records → Strength Level → History (tap to open session) → share.
- **Muscle-centric:** Profile → Statistics → Set count per muscle group (choose muscles & period) / Muscle distribution.
- **Time-centric:** Calendar (tap day) · Monthly report · Year in Review.
- **Body-centric:** Measures → graph → add entry (date + metrics + photo).

## 5. Why it "feels easy" — UX patterns worth copying [mostly INFERENCE from docs + reviews]

| Pattern | Evidence | Why it matters |
|---|---|---|
| **Previous values shown inline next to the input** | Docs + many reviews cite it as the top feature | Removes mental load; makes progressive overload a glance, not a lookup |
| **Auto-prefill sets when adding an exercise done before** | Docs | Most sessions repeat; user only edits deltas |
| **One-tap set completion that also starts the rest timer** | Docs | Logging and resting are the same gesture |
| **Set types are a tiny inline tag, not a different screen** | Docs | Depth without mode-switching |
| **Everything optional is a settings toggle** | Settings catalog | Clean default UI; power on demand |
| **Rest timer rule: skip for drop sets** | Docs | Respects the training intent; tiny rule, big smoothness |
| **Keep-awake option** | Docs | Don't unlock phone between sets |
| **Wrist logging + live phone sync** | Play Store | Phone can stay in locker |
| **Calculators live where the input is** (plate calc button above the keyboard) | Docs | No context switch |
| **Retroactive logging is first-class** (edit start date/time on finish; calendar +) | Docs | Handles forgotten sessions |
| **Tap-through everywhere** (record → the workout where it happened) | Docs | History is explorable, not a report |
| **Notes at the right scope** (routine vs. session vs. pinned-to-exercise) | Rebuilt notes (Oct 2026) | Solves "cue vs. observation" cleanly |
| **Celebration + sharing at the end** | Docs | Habit reinforcement (streaks/PRs) |

## 6. UX gaps to learn from [3RD-PARTY]
- Users want per-exercise rest durations to *persist* when they start an empty workout (one Play Store reviewer).
- RPE can't be a *target* in a routine, only a logged value.
- Unilateral (left/right) exercises are awkward to log (see file 07).
- Date-picker bug on weight entry reported by one user (Sept 2026) — a reminder to unit-test date UI.
