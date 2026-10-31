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
- [x] `rg -n "\((Barbell|Dumbbell|Kettlebell|Cable|Machine|Band)\)" app/src/main data/src/main domain/src/main` finds nothing.
- [x] Previews still render; `./gradlew assembleDebug lintDebug` pass with no new warnings.

## Verification
The `rg` above; `.\gradlew.bat assembleDebug lintDebug`.

## Notes / decisions needed
None. Found during T-036; the AC "no Hevy exercise names in `src/main`" was met for T-036's own code, these predate it.

## Handoff log
<!-- Newest first. Each entry: date, role, what was done, what's left, gotchas, commands run + results. -->

### 2026-10-31 · UI
- **Done:** replaced the Hevy-style preview titles in all 9 listed files. Catalogue titles where one exists (`Barbell Bench Press`, `Barbell Back Squat`, `Pull-Up`), neutral equipment-first names otherwise (`Dumbbell Lateral Raise`, `Barbell Row`). In `LibraryScreen.kt` the preview template ids (`bench-press-barbell`, `squat-barbell`, and the replace preview's `currentId`) now use the catalogue ids `barbell-bench-press` / `barbell-back-squat`, so the replace preview still marks the current exercise. Every edited line sits inside `@Preview` functions or `preview*` sample values; no production string, layout or logic changed.
- **Left alone (out of scope):** `app/src/test` fakes/tests still use `Bench Press (Barbell)` / `squat-barbell` (test fixture data, and the Hevy import tests need Hevy's CSV names); `Cable Fly (Low)` in the Library preview is a custom-exercise sample and does not match the AC pattern.
- **Verification:** the AC `rg` returns no matches (exit 1). Previews checked by reading their parameters; no emulator run, since no user-visible screen changed.
- **Commands:** `$env:JAVA_TOOL_OPTIONS='-Djavax.net.ssl.trustStoreType=Windows-ROOT'; .\gradlew.bat :domain:test testDebugUnitTest assembleDebug lintDebug` → BUILD SUCCESSFUL. Lint: 1 warning (`NewerVersionAvailable` in `libs.versions.toml`), same as before the change; no new warnings.
