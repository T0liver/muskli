# T-002 — Domain: units, set volume, estimated 1RM

- **Role:** BUILD
- **Phase:** 0
- **Depends on:** T-001
- **Refs:** TC-UNIT-*, TC-VOL-01..10, TC-1RM-* · research/05 §4–5 · D-003, D-010, D-014, D-017

## Goal
Pure, tested functions for kg/lb conversion, set/session volume and estimated 1RM, plus the minimal domain types they need (`SetType`, `ExerciseType`, `SetData`).

## In scope
- Tests first from the listed TC ids, then implementation in `:domain/calc`.
- `OneRmFormula` enum (Epley, Brzycki); default per D-003 is chosen by the caller, not hardcoded in the function.
- Volume counts only completed sets; warm-up inclusion is a parameter (default excluded).
- Bodyweight and assisted volume take `bodyweightKg` (D-010, D-017). Unilateral `side` multiplier per D-014 (TC-VOL-09). Unset body weight returns 0 (TC-VOL-10).

## Out of scope (do NOT do)
- Persistence. UI. PR logic. Reading settings or storing body weight (T-017).

## Acceptance criteria
- [x] All listed TC tests exist (test names contain the TC id) and pass.
- [x] Functions are pure; no Android imports; no `System.currentTimeMillis`.
- [x] Invalid inputs return `null`/0 as specified, no exceptions.

## Verification
`./gradlew :domain:test`

## Handoff log
### 2026-10-29 — BUILD
**Done.** `:domain/model/SetData.kt` adds `SetType`, `ExerciseType` (8 values), `Side` (D-014), and `SetData`. For bodyweight types `weightKg` holds the added weight; for assisted types it holds the assistance. `:domain/calc` adds `kgToLb`/`lbToKg` (full precision), `estimateOneRepMax(weightKg, reps, OneRmFormula)`, `setVolumeKg(set, type, bodyweightKg, unilateral = false)` and `sessionVolumeKg(exercises: List<ExerciseSets>, bodyweightKg, includeWarmUps = false)`. The caller chooses the formula (D-003).

**Owner decisions taken this session (recorded as D-026):**
- D-014 doubling conflicts with D-022's default `both`: doubling it everywhere would break TC-VOL-01 and TC-VOL-06. `setVolumeKg` now takes `unilateral` (default false), and only a unilateral exercise doubles a `both` set. Where the flag comes from is **T-023** (new).
- Assisted volume is clamped to 0 when assistance ≥ body weight (extra test under TC-VOL-08).
- A null added weight or assistance counts as 0 (TC-VOL-06 covers both `0.0` and `null`).

**Behaviour notes:** `setVolumeKg` ignores `completed` and `type`; `sessionVolumeKg` filters to completed and non-warm-up sets. Drop and failure sets count. Volume is 0 for reps ≤ 0, negative loads, and every type except weight_reps, bodyweight_reps and assisted (D-010 defines no others). Brzycki returns null at ≥ 37 reps, not only at 37.

**Verified:** `.\gradlew.bat :domain:test` → 29 tests, 0 failures (Units 4, OneRepMax 11, Volume 13, scaffold 1). Tests failed to compile before the implementation existed. `.\gradlew.bat assembleDebug` → BUILD SUCCESSFUL. Searched `domain/src` for Android imports, `currentTimeMillis` and `randomUUID`: 0 matches.

**Left:** nothing in scope. **Open questions:** none.

- 2026-10-28 DOCS: Owner pinned D-017. TC-VOL-06..10 are in scope. Body weight is a parameter.
