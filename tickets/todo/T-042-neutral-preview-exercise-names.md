# T-042 — Neutral exercise names in Compose preview sample data

- **Role:** UI
- **Phase:** 1
- **Depends on:** none
- **Refs:** D-025, D-043 · AGENTS.md §2 (no Hevy exercise data)
- **Size:** fits one session

## Goal
`@Preview` sample data in `:app` no longer uses Hevy-style exercise titles such as `Bench Press (Barbell)` or `Lateral Raise (Dumbbell)`; it uses titles from the CC0 catalogue (`Barbell Bench Press`) or neutral ones.

## In scope
- Replace the sample titles in preview code only: `ReorderDialog.kt`, `LibraryScreen.kt`, `HistoryComponents.kt`, `HistoryScreen.kt`, `ExerciseHistoryScreen.kt`, `WorkoutDetailScreen.kt`, `LiveWorkoutContent.kt`, `RoutineEditorScreen.kt`, `RoutinesScreen.kt` (found by T-036's `rg` over `src/main`).

## Out of scope (do NOT do)
- Any change outside preview sample data; strings, layouts, and logic stay as they are.

## Acceptance criteria
- [ ] `rg -n "\((Barbell|Dumbbell|Kettlebell|Cable|Machine|Band)\)" app/src/main data/src/main domain/src/main` finds nothing.
- [ ] Previews still render; `./gradlew assembleDebug lintDebug` pass with no new warnings.

## Verification
The `rg` above; `.\gradlew.bat assembleDebug lintDebug`.

## Notes / decisions needed
None. Found during T-036; the AC "no Hevy exercise names in `src/main`" was met for T-036's own code, these predate it.

## Handoff log
<!-- Newest first. Each entry: date, role, what was done, what's left, gotchas, commands run + results. -->
