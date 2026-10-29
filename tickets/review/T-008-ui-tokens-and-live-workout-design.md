# T-008 — UI: design tokens + live-workout screen (static)

- **Role:** UI
- **Phase:** 1
- **Depends on:** T-001. Live-workout reference screenshots are in `docs/reference-ui/workout-screen/`. Use those. Do not copy Hevy assets.
- **Refs:** SPEC §2, F-02, F-03 · docs/ARCHITECTURE.md §6

## Goal
The look and structure of the live-workout screen as stateless composables with previews and fake data; theme tokens (color, type, shape, spacing), light and dark.

## In scope
- Theme tokens; components: stopwatch/header (sets + volume), exercise card, set row (SET | PREVIOUS | KG | REPS | check) with type marker, rest-timer bar, add-set / add-exercise controls, exercise menu (UI only).
- Previews for: empty, 3 exercises with all four set types, checked/unchecked sets, dark mode, large font.
- Inspired by `docs/reference-ui/workout-screen/`; own colors/icons/text. No Hevy assets.

## Out of scope (do NOT do)
- State, ViewModels, persistence, business logic, other screens.

## Acceptance criteria
- [x] All previews render; touch targets >= 48dp; strings in `strings.xml`.
- [x] Logging a prefilled set needs <= 2 taps in the design (owner confirms from previews).
- [x] Owner approved the look (note in Handoff log).

## Handoff log
- 2026-10-28 DOCS: Screenshot-folder dependency closed. Owner sorts `docs/reference-ui/` themselves; this ticket must not wait on that.
- 2026-10-28 DOCS: Owner sorted the screenshots. Live workout is `docs/reference-ui/workout-screen/` (not `01-live-workout/`).
- 2026-10-29 UI: Taken on the owner's explicit instruction while T-001 is still in `verify/`. Built tokens (`ui/theme/`: own orange/teal palette light+dark, `WorkoutColors` for completed rows and W/D/F markers, tabular-figure typography, shapes, `Spacing` incl. 48dp `minTouchTarget`) and stateless components (`ui/components/`: `WorkoutHeader`, `ExerciseCard`, `SetTableHeader`/`SetRow`, `RestTimerBar`, `AddSetButton`/`AddExerciseButton`, `ExerciseMenu`) plus static `ui/workout/LiveWorkoutContent` with previews: empty, 3 exercises with all four set types, checked/unchecked + resting, dark, 1.5x font; each component has a light+dark preview. Own vector icons (`ic_check`, `ic_add`, `ic_more`). Design tap count: logging a pre-filled set = 1 tap (the check), because pre-filled values sit in the fields as real values. Changing one value first = tap field, type, tap check (2 taps). Gotcha: the app draws edge to edge, so the header pads the top safe-drawing insets and the rest bar pads the navigation bar. Previews were checked on the `medium_phone` emulator via the debug `androidx.compose.ui.tooling.PreviewActivity` (`adb shell am start -n hu.toliver.muskli/androidx.compose.ui.tooling.PreviewActivity --es composable <FileKt.Fn>`); dark/large font via `cmd uimode night yes` / `settings put system font_scale 1.5`. Commands: `:domain:test testDebugUnitTest assembleDebug lintDebug` all pass; lint has no findings in new code.
- 2026-10-29 UI: Owner approved the look and confirmed the tests. Tap count accepted: a pre-filled set logs in 1 tap. All acceptance criteria ticked. Moving to `review/` for a REVIEW session.
