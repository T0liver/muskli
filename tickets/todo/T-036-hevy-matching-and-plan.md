# T-036 — Hevy import: exercise matching and import plan in `:domain`

- **Role:** BUILD
- **Phase:** 1
- **Depends on:** T-035
- **Refs:** SPEC F-09 AC1, AC2 · `docs/HEVY-IMPORT.md` §3, §4, §8 · D-025, D-043, D-044 · TC-HEVY-11..19
- **Size:** fits one session

## Goal
Pure `:domain` functions match every Hevy exercise title to an exercise on the phone (or leave it unmatched), and turn parsed workouts plus the user's choices into a ready-to-store plan of new `Workout`s and new custom `ExerciseTemplate`s, with duplicates skipped and a summary for the preview.

## In scope
- In `hu.toliver.muskli.domain.importer.hevy`:
  - `fun matchHevyExercises(titles: Collection<String>, templates: List<ExerciseTemplate>): Map<String, HevyMatch>` with `sealed interface HevyMatch { data class Exact(val templateId: String); data class Normalised(val templateId: String); data class Unmatched(val candidateIds: List<String>) }`. Rules exactly as HEVY-IMPORT §3 (exact via `normalizeForSearch`, then token multiset; ties prefer non-archived; otherwise unmatched with the tied candidates).
  - `fun inferCustomExercise(title: String, sets: List<SetData>): Pair<ExerciseType, Equipment>` per HEVY-IMPORT §3 rules 1-8 and the equipment tokens.
  - `sealed interface HevyResolution { data class UseTemplate(val templateId: String); data object CreateCustom }` and `fun defaultResolutions(matches: Map<String, HevyMatch>): Map<String, HevyResolution>` (matched → UseTemplate, unmatched → CreateCustom).
  - `fun planHevyImport(workouts: List<HevyWorkout>, resolutions: Map<String, HevyResolution>, existingStartsEpochMs: Collection<Long>, ids: IdGenerator, clock: Clock): HevyImportPlan`, where `HevyImportPlan(val newWorkouts: List<Workout>, val newTemplates: List<ExerciseTemplate>, val workoutsInFile: Int, val duplicatesSkipped: Int, val setCount: Int, val rpeDropped: Int, val firstStartEpochMs: Long?, val lastStartEpochMs: Long?)`.
  - Duplicates per D-044 (same start minute as any existing start). Custom templates only for titles used by non-duplicate workouts (TC-HEVY-19). New workouts: ids from `IdGenerator`, `utcOffsetS = clock.utcOffsetS(start)`, created/updated = `clock.nowEpochMs()`, `restSeconds` null, `deletedAtEpochMs` null, every `WorkoutSet.completedAtEpochMs` null (the part of TC-HEVY-01 that `HevySet` cannot carry; assert it in the plan tests).
- Tests first (JUnit 5) for TC-HEVY-11..19, using the T-035 fixture and the 10 catalogue v1 exercises **copied into test code** (not read from `catalogue/`, so catalogue growth cannot break the tests).

## Out of scope (do NOT do)
- Storage, repository, use cases with I/O, DI (T-037). UI (T-038). Body weight (T-039).
- Any Hevy title list, alias table, or Hevy-to-slug mapping in main code, the catalogue, or resources (D-043, CC0 licence).
- Fuzzy matching; persisting mappings.

## Acceptance criteria
- [ ] TC-HEVY-11..19 implemented as tests, seen failing first, passing.
- [ ] Every set in a plan references a template id that is either in the input templates or in `newTemplates`.
- [ ] No Hevy exercise names in `src/main` of any module.
- [ ] `./gradlew :domain:test assembleDebug` pass.

## Verification
`.\gradlew.bat :domain:test assembleDebug`; `rg -i "hevy" domain/src/main` shows only the importer package names and KDoc, no exercise titles.

## Notes / decisions needed
None open: D-043, D-044.

## Handoff log
<!-- Newest first. Each entry: date, role, what was done, what's left, gotchas, commands run + results. -->
