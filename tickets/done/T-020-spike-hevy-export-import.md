# T-020 — SPIKE: Hevy export format + importer plan

- **Role:** SPIKE
- **Phase:** 1
- **Depends on:** T-001 · **needs the owner's Hevy export file in `docs/fixtures/`**
- **Refs:** SPEC F-09 · research/06, 10 (A15)

## Goal
Understand the real export format and plan the importer.

## In scope
- Inspect the file: columns, units, timestamps/timezones, set types, superset info, notes, exercise naming.
- Map to our model; list unmappable/ambiguous fields; propose exercise-matching strategy (exact title, normalized, manual mapping UI).
- Write the importer ticket(s), sized to one session each.

## Out of scope (do NOT do)
- Writing the importer; committing personal data (add fixtures to `.gitignore` if private).

## Acceptance criteria
- [x] Mapping table + risks + follow-up tickets in `todo/`.

## Handoff log
- 2026-10-30 SPIKE: done. Inspected the owner's `docs/fixtures/workout_data.csv` and `measurement_data.csv` (git-ignored by `docs/fixtures/*`; not committed, no personal content copied).
  - **Findings:** CSV, UTF-8 no BOM, CRLF, no final newline, strings quoted and numbers bare, one row per set. 14 workout columns (`title, start_time, end_time, description, exercise_title, superset_id, exercise_notes, set_index, set_type, weight_kg, reps, distance_km, duration_seconds, rpe`), 17 measurement columns. Times `28 Oct 2026, 07:10`: minute precision, English month, **no time zone**. No exercise id, type, equipment, or muscles, only the title. Distance is in **km** (the API uses metres). The sample has only `normal` sets and empty `superset_id`, `rpe`, `distance_km`, `duration_seconds`; the other values are taken from research/06 and must be handled anyway. Exercises in the sample happen to be in alphabetical order (risk: may not be the order performed; T-040 checks).
  - **Recommendation** written as `docs/HEVY-IMPORT.md`: file facts, mapping table, validation, matching strategy (exact, then normalised token multiset, then the user maps; unmatched default to local custom exercises; no Hevy data in the CC0 catalogue), merge-only semantics with same-start-minute duplicate detection, time-zone rule, measurements rule, risks, and an invented fixture with expected values.
  - **Decisions** (owner-delegated, auto-accepted): D-040 format and parser location, D-041 time zone, D-042 field mapping (RPE dropped), D-043 matching and licence, D-044 merge and duplicates (no schema change), D-045 latest body weight only.
  - **Docs:** TC-HEVY-01..22 added to `docs/TEST-CASES.md`; AC-07 gains "re-import adds nothing" (D-044). SPEC F-09 AC1 now points to HEVY-IMPORT.md; ARCHITECTURE lists `:domain/importer/`.
  - **Tickets:** T-035 parser (after T-020), T-036 matching and plan (after T-035), T-037 repository and use cases (after T-036), T-038 import screen (after T-037), T-039 body weight (after T-038), T-040 VERIFY AC-07 (after T-038, T-039). Listed in `tickets/README.md`.
  - **Gotchas for BUILD:** `:domain` cannot use `java.time` (minSdk 24, see `Clock`), so dates are parsed by hand and resolved via `Clock.utcOffsetS`. `core.autocrlf` is true and there is no `.gitattributes`, so the reader must accept CRLF and LF and normalise breaks inside fields.
  - **Commands:** PowerShell inspection of both CSVs (bytes, line ends, field counts), field-count check of the fixtures in HEVY-IMPORT §8 (14 and 17 on every record), epoch values computed with `DateTimeOffset`. No code changed; `.\gradlew.bat :domain:test assembleDebug` BUILD SUCCESSFUL (all up to date).
- 2026-10-30 SPIKE: blocked: no export file. `docs/fixtures/` holds only `.gitkeep`. Ticket stays in `todo/`; no format was inferred. Owner: put a Hevy export in `docs/fixtures/` (git-ignored), then rerun this spike.
