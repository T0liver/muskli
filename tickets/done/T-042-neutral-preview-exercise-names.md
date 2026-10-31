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

### 2026-10-31 · VERIFY
**Pass. Move to `done/`.** No emulator. The diff only changes `@Preview` sample titles, plus the library preview's template ids so the replace preview still marks the current exercise. A running app shows none of that, so WORKFLOW §6's device install does not apply.
- Build: preview change `c3a794a`, review `004044c`, branch `t-042-neutral-preview-exercise-names`, Windows. `$env:JAVA_TOOL_OPTIONS='-Djavax.net.ssl.trustStoreType=Windows-ROOT'; .\gradlew.bat :domain:test testDebugUnitTest assembleDebug lintDebug` → BUILD SUCCESSFUL. `:domain:test`, `testDebugUnitTest` and `assembleDebug` were UP-TO-DATE for this tree (results from 14:03 on `c3a794a`): domain 239, app 125, data 67, 431 tests, 0 failures, 0 errors, 0 skipped. Lint: 0 errors, 1 warning, the pre-existing `NewerVersionAvailable` on `junit5 = "5.14.4"` in `gradle/libs.versions.toml`. No new warnings.
- `rg -n "\((Barbell|Dumbbell|Kettlebell|Cable|Machine|Band)\)" app/src/main data/src/main domain/src/main` exits 1.
- No production strings, catalogue, or test expected values in the diff. No new dependency.
- Every acceptance criterion holds. No new tickets.

### 2026-10-31 · REVIEW
**Verdict: no Blockers, no Should-fix, no Nits. Move to `verify/`.** Reviewed `git diff main...t-042-neutral-preview-exercise-names` (commit `c3a794a`) against the ticket, D-025, D-043 and WORKFLOW §5.
- **Scope.** Eleven paths: the nine listed preview files, plus this ticket moving `todo/` → `review/`. Every Kotlin hunk is a title (and, in `LibraryScreen`, the matching preview template id) inside a `@Preview` function or a `preview*` helper used only by previews. `currentId` in the replace preview is `barbell-back-squat`, the same id as that row, so the current exercise stays marked.
- **Catalogue vs neutral.** `Barbell Bench Press`, `Barbell Back Squat` and `Pull-Up` are titles in `catalogue/catalogue.json`, and the library preview ids `barbell-bench-press` / `barbell-back-squat` match that file. `Dumbbell Lateral Raise` and `Barbell Row` are not in the catalogue (it has no row or lateral-raise exercise); the neutral equipment-first names match the ticket. `Cable Fly (Low)` is unchanged: it is the custom-exercise sample and does not match the acceptance pattern.
- **Left untouched.** No `strings.xml`, no `catalogue/`, no test file, no `libs.versions.toml` or Gradle build file. `app/src/test` still uses Hevy CSV titles, which this ticket puts out of scope. No new dependency, schema change, or `:domain` API change. No logging-screen control.
- **Pattern.** `rg -n "\((Barbell|Dumbbell|Kettlebell|Cable|Machine|Band)\)" app/src/main data/src/main domain/src/main` exits 1 (no matches).

**Blocker:** none. **Should-fix:** none. **Nit:** none.

### 2026-10-31 · UI
- **Done:** replaced the Hevy-style preview titles in all 9 listed files. Catalogue titles where one exists (`Barbell Bench Press`, `Barbell Back Squat`, `Pull-Up`), neutral equipment-first names otherwise (`Dumbbell Lateral Raise`, `Barbell Row`). In `LibraryScreen.kt` the preview template ids (`bench-press-barbell`, `squat-barbell`, and the replace preview's `currentId`) now use the catalogue ids `barbell-bench-press` / `barbell-back-squat`, so the replace preview still marks the current exercise. Every edited line sits inside `@Preview` functions or `preview*` sample values; no production string, layout or logic changed.
- **Left alone (out of scope):** `app/src/test` fakes/tests still use `Bench Press (Barbell)` / `squat-barbell` (test fixture data, and the Hevy import tests need Hevy's CSV names); `Cable Fly (Low)` in the Library preview is a custom-exercise sample and does not match the AC pattern.
- **Verification:** the AC `rg` returns no matches (exit 1). Previews checked by reading their parameters; no emulator run, since no user-visible screen changed.
- **Commands:** `$env:JAVA_TOOL_OPTIONS='-Djavax.net.ssl.trustStoreType=Windows-ROOT'; .\gradlew.bat :domain:test testDebugUnitTest assembleDebug lintDebug` → BUILD SUCCESSFUL. Lint: 1 warning (`NewerVersionAvailable` in `libs.versions.toml`), same as before the change; no new warnings.
