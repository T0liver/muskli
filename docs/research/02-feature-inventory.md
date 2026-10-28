# 02 — Feature Inventory

**Legend**
- **Tier:** Free / Pro / ? (unknown or conflicting)
- **Priority (my suggestion for a personal app) [INFERENCE]:** **P0** = MVP core · **P1** = soon after MVP · **P2** = nice-to-have later · **X** = skip (social/business/not useful solo)
- Details marked **[OFFICIAL]** were read on Hevy's own pages. Items marked *(title only)* appear in Hevy's feature index but I did not read their detail pages.

---

## A. Workout logging

| # | Feature | Behavior / details | Tier | Pri |
|---|---|---|---|---|
| A1 | Start empty workout | Build an improvised session from scratch: add exercises, sets, log values. Stopwatch top-left. | Free | P0 |
| A2 | Start from routine | Tap "Start Routine"; sets are prefilled from the template; stopwatch runs. | Free | P0 |
| A3 | Copy a past workout | From any saved workout (own, or another user's) → starts a new live workout with same details. | Free | P1 |
| A4 | Workout stopwatch | Tap it to change duration, start date/time, or pause/restart. Shown with live **set count** and **volume load**. | Free | P0 |
| A5 | Add exercise from library | "+ Add Exercise" at bottom; search bar + filters (muscle, equipment); can add several at once; includes custom exercises. | Free | P0 |
| A6 | Auto-fill when re-adding an exercise | If done before, number of sets/weight/reps are pre-added from last time (editable). | Free | P0 |
| A7 | Add / delete sets | "+ Add Set"; swipe left on a set → red Delete. No limit on exercises or sets. | Free | P0 |
| A8 | Set types | Normal (default), Warm-up, Drop, Failure. Tap the set number to open type menu (also lets you delete the set). Can be changed any number of times; can mix all four in one exercise. Also settable inside routines. | Free | P0 |
| A9 | Inputs per exercise type | Weight (KG/LBS), Reps or **Rep range** (e.g., 6–8) in routines, Time (duration), plus distance for some types. Tap column header to switch units / reps↔range. | Free | P0 |
| A10 | "Previous" column | Shows what you did last time for that exercise. Two modes (setting): last time **anywhere**, or last time **in this routine**. | Free | P0 |
| A11 | Set completion check | Tapping the check marks the set done, triggers the rest timer, and (optionally) plays a sound. | Free | P0 |
| A12 | Automatic rest timer | Per-exercise duration, a global default, or off. Starts when a set is completed. **Does not start if the next set is a Drop set.** Notifies at zero. | Free | P0 |
| A13 | Supersets / circuits | 3-dot menu → "Add to Superset"; can chain 3+ exercises. Rest timer per movement adjustable/off. Optional "smart superset scrolling" jumps to the next exercise in the superset after completing a set. | Free | P1 |
| A14 | Exercise menu (3 dots) | Reorder (drag), Replace, Add to superset, Add warm-up sets, Remove. | Free | P0 |
| A15 | Notes (rebuilt Oct 2026) | (a) **Routine exercise note** — reappears every time the routine is used (tips). (b) **Workout exercise note** — session-only. (c) **Pinned exercise note** — attached to the exercise itself; appears whenever you add it to a routine/workout/Trainer plan. (d) Routine-level pinned note; (e) one-off note for today's workout. Notes can contain a link (private to you). | Free | P1 |
| A16 | RPE per set | Opt-in setting adds an RPE column during live workouts. Not available for duration exercises. **Cannot set RPE targets in routines** (UI limitation). | Free | P1 |
| A17 | Warm-up calculator | Opt-in. Percent-based warm-up ladder configurable (percent + reps per set, add/remove sets, reset to default), with weight rounding for plates and dumbbells. Enter target weight → "Insert warm-up sets". Only for load-based exercises. | Free | P1 |
| A18 | Plate calculator | Opt-in. For **barbell / EZ bar / short bar** exercises only (not dumbbells). Inputs: target weight, bar weight, your available plates → which plates to load. Opened from a button above the keyboard. | Free | P1 |
| A19 | Inline timer for duration sets | Opt-in. Start/pause button per set in the TIME column (e.g., planks). | Free | P1 |
| A20 | Live PR notification | Opt-in. When a set is completed and sets a new record (1RM, most reps, best duration, etc.), you get a notification (own volume setting). Also viewable later in workout details. | Free | P1 |
| A21 | Keep screen awake | Opt-in toggle so the phone doesn't lock mid-workout. | Free | P0 |
| A22 | Unit switching | Global setting (Units) and per-exercise toggle (KG ↔ LBS). | Free | P0 |
| A23 | Finish screen | Name the workout; see volume & set count; edit duration; edit start date/time (to log past sessions); upload up to 3 photos **or** 2 photos + 1 video; write a note (can tag users); set workout private. | Free | P0 (name/duration/date/note), X (media/tag/private) |
| A24 | Post-save celebration | Shows ordinal workout number ("472nd"), active weekly streak, PRs achieved, share options (transparent/light/dark backgrounds), 7-day consistency summary. | Free | P2 |
| A25 | Log a past workout | Calendar "+" lets you add a previously completed workout for a chosen day. | Free | P1 |
| A26 | Edit / delete saved workout | Profile → workout → 3 dots → Edit (add/remove exercises & sets, notes, RPE…) or Delete. | Free | P0 |
| A27 | Create routine from workout | 3 dots on a saved workout → turns it into a reusable routine. | Free | P1 |
| A28 | Live Activity (iOS) *(title only)* | Lock-screen/Dynamic-Island workout status. | ? | X (Android) |
| A29 | Home-screen widgets *(title only)* | | ? | P2 |

## B. Routines & programming

| # | Feature | Behavior / details | Tier | Pri |
|---|---|---|---|---|
| B1 | Routine builder | Name; exercises; number of sets; set types; load and reps (or rep range) or duration per set; rest period per exercise; notes; superset pairing; warm-up sets. Reorder/replace/remove exercises. | Free (capped) | P0 |
| B2 | Routine folders | Organize routines into folders; choose folder on create. | Free | P1 |
| B3 | Routine library ("Explore") | **26 programs** + routine categories (docs say seven in one place, eight in another). Filter by experience, goal, equipment. Save an entire program (becomes a folder) or a single routine, then edit freely. Free for all. | Free | P2 (author your own content) |
| B4 | Share folders/routines *(title only)* | Share via link; other users can save. | Free | X |
| B5 | Save other athletes' routines | From feed/profile. | Free | X |
| B6 | **Hevy Trainer** | Algorithmic program generator. Inputs: experience, goal, equipment, frequency, session duration, muscle focus. Progression rule: raise the weight only after hitting the **top of the rep range on every prescribed set** (double progression). Vendor calls it an algorithm, not AI. No recovery inputs (sleep/HRV). Can modify generated workouts. | Pro | P2 |
| B7 | ChatGPT integration | `@hevy` in ChatGPT: analyze training history, generate a program, save routines to Hevy. | Free | P2 (see file 09 for an MCP angle) |

## C. Exercise library

| # | Feature | Behavior / details | Tier | Pri |
|---|---|---|---|---|
| C1 | Built-in library | **400+** exercises (older store text said 200+). Each has: title, **exercise type**, **primary muscle**, **secondary muscles**, **equipment**. Includes strength, calisthenics, kettlebell, CrossFit-style, core, some cardio. | Free | P0 (own dataset) |
| C2 | Videos / animations / illustrations | High-quality form demos per exercise. | Free | P2 (licensing!) |
| C3 | Filters & search | By muscle group and equipment; search by name; "recently logged" and "custom" sections. | Free | P0 |
| C4 | Custom exercises | Required: title, exercise type, primary muscle, equipment; optional secondary muscles. Primary/secondary must be set for stats to work correctly. Free cap reported ~7. | Free (capped) | P0 |
| C5 | Exercise detail → Summary | Heaviest weight, projected-or-true 1RM, best set volume, best session volume, most reps; tap a record to jump to that workout. | Free | P1 |
| C6 | Exercise detail → Set Records | Best weight for each rep count (1RM, 2RM … ). | Free | P1 |
| C7 | Exercise detail → Strength Level | Only for barbell **Squat, Bench Press, Deadlift**. Compares you to others of same age/weight/sex → Beginner / Intermediate / Advanced / Elite. | Free | P2 |
| C8 | Exercise detail → History | Session-by-session performance; tap to open the full workout. | Free | P0 |
| C9 | Exercise charts | Full-screen graphs of volume, best weight, total reps (and duration for timed). | Free (3-mo window) | P0 |
| C10 | Share exercise summary | Image/summary of PRs for an exercise, shareable outside the app. | Free | P2 |

### Exercise *types* (from the API — these define which inputs a set has) [API]
`weight_reps`, `reps_only`, `bodyweight_reps`, `bodyweight_assisted_reps`, `duration`, `weight_duration`, `distance_duration`, `short_distance_weight`.

### Muscle groups (20) [API]
abdominals, shoulders, biceps, triceps, forearms, quadriceps, hamstrings, calves, glutes, abductors, adductors, lats, upper_back, traps, lower_back, chest, cardio, neck, full_body, other.

### Equipment categories (9) [API]
none, barbell, dumbbell, kettlebell, machine, plate, resistance_band, suspension, other.

## D. Progress & analytics

| # | Feature | Behavior / details | Tier | Pri |
|---|---|---|---|---|
| D1 | Statistics: last-7-days body graph | Workouts in last 7 days + which muscles were trained. | Free | P1 |
| D2 | Set count per muscle group | Bar/line graph of sets per muscle group; segment weekly/monthly/yearly; windows: 30 days, 3 months, year, all time. | Free 3-mo; Pro more | P1 |
| D3 | Muscle distribution (chart) | How volume is split across muscles + number of workouts, duration, volume, completed sets for the period. | Free 30d/3mo; Pro year/all | P1 |
| D4 | Muscle distribution (body diagram) | Weekly set counts per muscle with trained areas highlighted on a body figure. | Free | P2 |
| D5 | Main exercises | Most-logged exercises for 30d / 3mo / year / all time. | Free | P2 |
| D6 | Personal records | Per-exercise bests (see C5/C6) + live PR toasts + PR highlights on finish screen and monthly report. | Free | P1 |
| D7 | Calendar | Month grid with blue workout days; tap day → workout; "+" to log past workout; zoom to year and multi-year; set first day of week. | Free | P1 |
| D8 | Streak | Number of **consecutive weeks** with at least one logged workout. | Free | P2 |
| D9 | Body measurements | Weight, body fat, and **14 circumference measurements**; entry per day; partial entries allowed (e.g., weight only); weight graph (3-month default window). Some "advanced measurements" are Pro. | Free/Pro | P1 |
| D10 | Progress photos | Attach to a measurement entry or stand alone; gallery by date; compare two photos; edit/replace/delete. | Free | P2 |
| D11 | Monthly report | Available the month after: workouts, volume, sets vs. previous months, PRs, active days & weekly streak, body-part volume distribution, muscles trained, most-logged exercises, shareable. | Free | P2 |
| D12 | Year in Review | Live from start of December: stats, consistency, streak, best month, personal bests, "biggest supporters", shareable. | Free | P2 |
| D13 | Compare performance & stats *(title only)* | Compare with other athletes. | Free | X |

## E. Social (almost all **X** for a personal app)

| # | Feature | Notes | Pri |
|---|---|---|---|
| E1 | Follow/unfollow, athlete profiles | Public/private profile | X |
| E2 | Social feed & Discovery feed *(title only)* | | X |
| E3 | Like & comment on workouts | | X |
| E4 | Tag users in workout notes | | X |
| E5 | Leaderboards *(title only)* | | X |
| E6 | Athlete workouts / save others' routines | | X |
| E7 | Invite friends from contacts | | X |
| E8 | Social media shareables | Generated images (transparent/light/dark bg); Instagram/Snapchat share. | P2 (local "share card" is cheap) |
| E9 | Strava integration *(title only)* | | X |
| E10 | Workout privacy | Per-workout private flag; private profile. | X |
| E11 | Upload gym selfies/video to workouts | Up to 3 photos or 2+1 video. | P2 |

## F. Integrations, data & platforms

| # | Feature | Notes | Pri |
|---|---|---|---|
| F1 | Apple Health / Health Connect sync | Workout data syncing (per third-party review). | P2 |
| F2 | Heart-rate capture from watch | Watch apps record workout HR. | X/P2 |
| F3 | Apple Watch / Wear OS apps | See file 01. | X for MVP |
| F4 | Web app | Plan routines & analyze on desktop. | P2 |
| F5 | Data export | Workouts and measurements can be exported (help center article exists). Exact format **not verified**. | P1 (for migration) |
| F6 | Strong CSV import | Hevy imports Strong-app CSV. Useful as a *format reference*. | P2 |
| F7 | Public API (Pro) | See file 06. | X |
| F8 | "Send workout to ChatGPT/Claude" | Button on finish screen (v3.1.13). | P2 |

## G. Hevy Coach (separate product) — **X**
Trainer builds programs & assigns workouts to clients; clients see them in the Hevy app and chat with the coach. Reviewers complain it lacks nutrition tracking, group messaging, scheduling, payments. Not relevant here.

---

## H. Settings catalog (Profile → gear → Workouts) [OFFICIAL]

| Setting | What it does | Pri |
|---|---|---|
| Sounds | Choose timer sound; volumes (off/low/normal/high) for timer, set-check, and PR notification | P1 |
| Default rest timer | Default duration for newly added exercises, or off | P0 |
| Previous workout values | Anywhere vs. within-this-routine | P0 |
| Warm-up calculator | Enable + define warm-up ladder + plate rounding + dumbbell rounding | P1 |
| Warm-up sets in stats | Include/exclude warm-up sets from volume/PR/stat calculations; toggling **recalculates history** | P1 |
| Keep awake during workout | Prevent screen sleep | P0 |
| Plate calculator | Enable; choose bar weight and available plates | P1 |
| RPE tracking | Adds RPE column | P1 |
| Smart superset scrolling | Auto-scroll to next exercise in superset | P1 |
| Inline timer | Per-set stopwatch for duration sets | P1 |
| Live PR notification (+ volume) | Enable PR toasts | P1 |
| Units | kg / lbs (global) | P0 |
| First day of week | Affects calendar & weekly stats | P1 |
| Profile/privacy, notifications, linked accounts *(not read)* | | X |

> **Pattern to copy:** every non-essential logging feature is a toggle, default off or minimal. That keeps the core screen uncluttered and lets power features exist without hurting beginners (you, though, are the only user — so you can simply default them on if you want).
