# T-023 — Unilateral flag on exercises

- **Role:** BUILD
- **Phase:** 2
- **Depends on:** T-006, T-010
- **Refs:** D-014, D-022, D-026 · TC-VOL-09
- **Size:** fits one session

## Goal
Volume knows which exercises are unilateral, so `setVolumeKg(..., unilateral = true)` is passed for them and a `both` set counts 2× (D-014, D-026).

## In scope
- Decide (owner) where the flag lives: exercise template field, catalogue field (`docs/CATALOGUE.md`), or both.
- Wire it from the template into every volume caller.

## Out of scope (do NOT do)
- Left/right logging UI (D-022 keeps it out of the MVP).

## Acceptance criteria
- [x] Owner decision recorded in `docs/DECISIONS.md`.
- [x] Volume for a unilateral exercise doubles `both` sets; bilateral exercises are unchanged.

## Verification
`./gradlew :domain:test testDebugUnitTest`

## Notes / decisions needed
Needs owner approval for any schema or catalogue format change. Owner, 2026-10-30: **both** (D-037); the schema and catalogue changes are approved. Custom exercises get a unilateral toggle in the custom-exercise editor.

## Handoff log
- 2026-10-29 BUILD (T-002): Created. `:domain` already accepts the flag; nothing supplies it yet, so every caller currently gets `false`.
- 2026-10-30 BUILD (branch `t-023-unilateral-exercise-flag`):
  - **Done:** `ExerciseTemplate`, `CatalogueExercise`, and `CustomExerciseInput` gain `unilateral: Boolean = false`. `toTemplate` and the custom create/edit use cases carry it. Room **v4**: `exercise_template.unilateral INTEGER NOT NULL DEFAULT 0`, plus `MIGRATION_3_4`, exported `schemas/.../4.json`, and a `MigrationTest` case (TC-DATA-04). The mappers, `CatalogueParser` (absent = false), and backup `ExerciseJson.unilateral` (optional, default false, written only when true, `schemaVersion` stays 1) are updated. Both `sessionVolumeKg` callers (live header in `WorkoutSessionViewModel`, history in `summarizeWorkout`) pass `template.unilateral`; nothing else calls `setVolumeKg`. Editor: a "Unilateral" switch row below Equipment, with the whole row toggleable and 48 dp; the live-workout screen is unchanged. Catalogue: optional `unilateral` in `catalogue.schema.json` after `equipment`, documented in CATALOGUE §2/§9, with a checker test for its key order. `formatVersion` stays 1. `catalogue.json` is unchanged (none of the 10 sample exercises is unilateral), so `version` is not bumped. ARCHITECTURE §3/§8 have a line each.
  - **Decisions:** D-047 (auto-accepted recommendation, owner-delegated 2026-10-30) covers the catalogue/backup representation, the migration default, the editor placement, and keeping best-set-volume records undoubled.
  - **Tests added:** `:domain` tests first (`CatalogueUpdateTest` ×2, `ExerciseLibraryUseCasesTest` ×1; they failed to compile before the field existed). `:data`: migration 3→4, parser flag/default, backup round-trip plus a file without the key. `:app`: live header 100×5 bilateral + 20×10 unilateral = 900, the same for the history summary, and the editor toggle loads and saves. TC-VOL-09 itself was already in `VolumeTest`.
  - **Commands:** `.\gradlew.bat :domain:test testDebugUnitTest assembleDebug lintDebug` BUILD SUCCESSFUL, 0 failures. Lint: only the existing junit-bom `NewerVersionAvailable`. `python -m unittest discover -s catalogue/tools -p "test_*.py"`: 41 tests OK. `python catalogue/tools/check_catalogue.py --base-ref main`: OK. Note: the first `:data:testDebugUnitTest` after the version bump fails `MigrationTest` with "Missing file 4.json", because the schema is exported in the same run. It passes on the second run, and now that `4.json` is committed it passes the first time.
  - **Manual (emulator `medium_phone`):** installed a `main` build (v3) and created custom "Old Lunge", then installed this build over it. The exercise survived the migration with Unilateral off. Turned it on, saved, and reopened: on. In a live workout, 20 kg × 10 completed showed **Volume 400 kg** (TC-VOL-09). The emulator's system server stalled once ("system isn't responding"); that was an emulator issue, with no app crash in logcat.
  - **Not verified:** schema rule 2 (`check-jsonschema` is not installed locally; CI runs it). Dark mode of the new row on a device (covered by the existing dark editor preview only).
  - **Left / follow-ups:** adding or marking unilateral exercises in the catalogue is a normal catalogue PR, with a `version` bump.
- 2026-10-30 REVIEW (diff `main...t-023-unilateral-exercise-flag` at `68b65f1`):
  - **Scope:** matches the ticket and D-037/D-047. No left/right UI (D-022), the live-workout screen is unchanged, no new dependencies, and no existing test expectations were edited (only new tests were added).
  - **Layers:** the flag lives on the `:domain` model; volume stays in `sessionVolumeKg`/`setVolumeKg`. Both callers (`WorkoutSessionViewModel` header, `summarizeWorkout`) pass `template.unilateral`; a grep finds no other caller. The editor row is stateless (state hoisting via `LibraryEvent.EditorUnilateralChanged`), its strings are in `strings.xml`, it is at least 48 dp with whole-row `toggleable(role = Switch)`, and the edit preview covers it.
  - **Schema:** v4 with `MIGRATION_3_4` (`ADD COLUMN unilateral INTEGER NOT NULL DEFAULT 0`) is registered in `MIGRATIONS`. The exported `4.json` matches the entity (`defaultValue "0"`), and `MigrationTest` covers 3→4 (TC-DATA-04).
  - **Backward compatibility:** the backup field is optional (default false) and `schemaVersion` stays 1, with a test for a file without the key. The catalogue field is optional, `formatVersion` stays 1, the parser defaults absent to false, and the schema keeps `additionalProperties: false` with the new property after `equipment`.
  - **Blocker:** none. **Should-fix:** none.
  - **Nit:** `CatalogueParser` could use `optBoolean("unilateral", false)`, but `optBoolean` would also accept the string `"true"`, so the explicit `has`/`getBoolean` is stricter and fine as is. In `WorkoutSessionViewModel.toActive` the template is looked up twice per exercise (once for `types`, once for `unilateral`); this is harmless. Nothing to change.
  - **Checks:** `.\gradlew.bat :domain:test testDebugUnitTest assembleDebug lintDebug` BUILD SUCCESSFUL. The test tasks were re-run with `--rerun` and also passed. Lint: 0 errors, 1 warning (the existing junit-bom `NewerVersionAvailable`). `python -m unittest discover -s catalogue/tools -p "test_*.py"`: 41 tests OK.
  - **Result:** moved to `verify/`.
