# 11 — Visual Capture Checklist

**Why this file exists:** I could only read text. UI details (layout, spacing, colors, icons, motion, microcopy) are the part of Hevy that "feels nice", and they're the part I can't capture. **You can.** Spend ~30 minutes taking screenshots and a couple of screen recordings from your own Hevy app and put them in a `/docs/reference-ui/` folder. Agents (and the planning session) can then work from real visuals.

> These captures are for **your personal reference** when designing your own UI. Don't ship Hevy's assets, icons, copy, or illustrations in your app.

## 1. Priority screenshots (do these first)

### Live workout screen (most important)
- [ ] Empty workout just started (no exercises)
- [ ] Workout with 3 exercises: one with warm-up/normal/drop/failure sets, one in a superset, one duration (plank)
- [ ] Set row **before** and **after** tapping ✓ (checked state color/animation)
- [ ] **Set type menu** open (tap the set number)
- [ ] Exercise ⋯ menu open
- [ ] Rest timer **running** (in-app UI) + the **notification / lock-screen** appearance
- [ ] Keyboard open with the **plate Calculator** button visible; the calculator sheet itself
- [ ] **Warm-up** insertion dialog and the resulting sets
- [ ] **Live PR** notification
- [ ] Stopwatch tap → duration/start-time editor
- [ ] Top bar with live sets/volume counters
- [ ] Bottom area ("+ Add Exercise" / "Settings")
- [ ] In-workout Settings sheet

### Finish & post-workout
- [ ] Finish/save screen (name, duration, date, media, note, privacy)
- [ ] Post-save celebration screen (workout #, streak, PRs)
- [ ] Share sheet / share card options

### Routines
- [ ] Workout tab (routines list with folders)
- [ ] Routine editor (sets, rep range, rest timer, notes, superset)
- [ ] "Explore" routine library list + one program detail
- [ ] Routine ⋯ menu

## 2. Progress & stats
- [ ] Profile tab (top)
- [ ] Statistics: 7-day body graph; sets per muscle (3 different periods); muscle distribution chart; body diagram; main exercises
- [ ] Exercise detail: Summary, Set Records, Strength Level, History
- [ ] Exercise chart (full-screen) for volume / best weight / reps
- [ ] Calendar (month, year, multi-year) + streak
- [ ] Monthly report (all pages)
- [ ] Measures: list, add-entry sheet (show all 14 circumference fields), weight chart
- [ ] Progress photos gallery + compare view

## 3. Library & settings
- [ ] Exercise library (search + filters), a "recently used" and "custom" section
- [ ] Exercise detail with demo media (just for layout reference)
- [ ] Create custom exercise form (all fields and options lists: type, muscle, equipment)
- [ ] **Settings → Workouts** (every toggle, scroll through the whole list)
- [ ] Warm-up calculator settings (**including the default ladder** — answers open question A1)
- [ ] Plate calculator settings (bar weights and available plates)
- [ ] Units, first day of week
- [ ] Export data screen

## 4. Navigation / general
- [ ] Bottom nav bar (all tabs) + Home/feed (just to see what to *omit*)
- [ ] Dark mode and light mode of the live workout screen
- [ ] Empty states (no workouts yet, no routines)
- [ ] Any onboarding screens

## 5. Screen recordings (short, 10–30 s each)
1. Logging 3 sets of an exercise using **Previous** values: tap-by-tap (shows speed and gesture feel).
2. Starting a routine → completing sets → rest timer firing → finishing → celebration.
3. Creating a **superset** and using smart superset scrolling.
4. Drop-set sequence (note: no auto rest timer).
5. Reordering exercises (drag) and swiping to delete a set.
6. Opening Statistics and switching periods/muscles.

## 6. Annotations to add (a few lines each)
- What you **like** about each screen (speed, clarity, gestures).
- What you **dislike** or would change (feeds file 07 Part B).
- Tap counts for the common actions (e.g., "log a set = 2 taps").

## 7. Data fixtures to export (for tests and importer)
- [ ] Hevy data export (workouts, measurements) — check the format (open question A15).
- [ ] A short list of your custom exercises with their muscle/equipment.
- [ ] Your typical warm-up ladder and plate inventory (bar weight + plates) — reusable as default settings.

## 8. Suggested folder layout
```
docs/reference-ui/
  01-live-workout/
  02-finish-and-share/
  03-routines/
  04-stats/
  05-exercise-detail/
  06-measures/
  07-settings/
  recordings/
  notes.md   # likes / dislikes / tap counts
```
