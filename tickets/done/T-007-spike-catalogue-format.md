# T-007 — SPIKE: catalogue file format and publish flow

- **Role:** SPIKE
- **Phase:** 0
- **Depends on:** none
- **Refs:** D-005, D-021 · research/09 §3 · research/04 enums

## Goal
Define the **catalogue file format**, validation rules, and GitHub Pages API shape per **D-005** and **D-021**. The same file is the bundled snapshot (T-010) and the published bundle (T-021).

## In scope
- Pick JSON vs TOML (or other) with rationale; schema covering our enums (type, muscles, equipment, etc.).
- CI checker spec: what fails a PR on the `catalogue` branch; merge/publish flow to GitHub Pages.
- Sample catalogue of ~10 exercises; document the published API URL layout and a **monotonic version field** so the app can apply a bundle only when it is newer (D-021). Stable exercise ids required (upsert key).
- License/attribution rules for contributed exercises (no Hevy data).

## Out of scope (do NOT do)
- App loader implementation (T-010), delta-sync, copying Hevy's library/media.

## Acceptance criteria
- [x] Schema + sample file + CI checklist documented (link from `docs/ARCHITECTURE.md` §2).
- [x] Format is sufficient for T-010 (bundled load) and T-021 (full sync): version field, stable ids, URL layout.

## Handoff log
- 2026-10-30 VERIFY: **Pass.** Both acceptance criteria pass. Verified commit `144f0b84cdae35e2f84798625135c865d5c38700` on branch `t-007-catalogue-format` (fast-forwarded onto `main`). Device: emulator medium_phone, Android 16 (API 36). Owner override 2026-10-30: an emulator pass replaces the owner's phone. Moved to `done/`. No new tickets or decisions.
  - **AC1 (schema + sample + CI checklist documented, linked from ARCHITECTURE §2): PASS.** `docs/CATALOGUE.md`, `catalogue/catalogue.schema.json`, and `catalogue/catalogue.json` exist and agree. ARCHITECTURE §2 "Exercise catalogue" links `docs/CATALOGUE.md` (D-025). CI checklist is CATALOGUE §5. D-025 is in `docs/DECISIONS.md`, status decided. Python 3.12.10; `jsonschema` is not installed and was not pip-installed. A throwaway stdlib script (not committed) checked every keyword this schema uses (`$ref`, `const`, `enum`, `type`, `required`, `additionalProperties`, `minLength`/`maxLength`, `pattern`, `minimum`, `uniqueItems`, `items`) plus UTF-8 without BOM and no duplicate keys: 0 errors. Also: 10 kebab ids, unique across `exercises` and `retiredIds`, each equal to `slug(title)`, titles unique, exercises sorted by `id`, no primary muscle in `secondaryMuscles`, all 8 types present, `formatVersion` 1, `version` 1, `license` `CC0-1.0`, LF-normalized text matches `json.dumps(..., indent=2, ensure_ascii=False) + "\n"`. Domain `ExerciseType` (8), `MuscleGroup` (20), and `Equipment` (9) lowercase names match the schema enums. Full Draft 2020-12 via `check-jsonschema` was not run; that checker is T-022.
  - **AC2 (format sufficient for T-010 bundled load and T-021 full sync): PASS.** Version field, stable ids, and URL layout are in the file and in CATALOGUE §3, §4, and §7 (`https://t0liver.github.io/muskli/catalogue/v1/catalogue.json` plus `catalogue/v1/catalogue.schema.json`). The app bundles that same file: `copyBundledCatalogue` in `data/build.gradle.kts` copies `catalogue/catalogue.json` into assets; `BundledCatalogueLoader` reads `catalogue/catalogue.json`. APK `assets/catalogue/catalogue.json` is byte-identical to the working copy (2612 bytes, sha256 `8174c8e858daea72af29fa47784ef31f621bc057ca31f1d9bc23ff2f30bad0ad`) and parses to the same document with `version` 1. Emulator: `pm clear`, install, start `hu.toliver.muskli/.MainActivity`, tap Exercise library. uiautomator showed all 10 catalogue titles (Barbell Back Squat, Barbell Bench Press, Barbell Deadlift, Dumbbell Farmer's Walk, Hanging Knee Raise, Machine Assisted Pull-Up, Plank, Pull-Up, Rowing Machine, Weighted Plank). Screenshots `build/verify/t007-library.png` and `build/verify/t007-library-scrolled.png` (git-ignored).
  - **Gotcha, not a failure:** `core.autocrlf=true` makes the working copy and the APK asset CRLF (2612 bytes) while the git blob is LF (2505 bytes; `git hash-object` matches `HEAD:catalogue/catalogue.json`). Same JSON. `.gitattributes` `catalogue/** text eol=lf` is already T-022. No new ticket.
  - **Commands:** `.\gradlew.bat :domain:test testDebugUnitTest assembleDebug lintDebug` → BUILD SUCCESSFUL in 4s (129 tasks: 2 executed, 127 up-to-date; `:domain:test` UP-TO-DATE). Re-ran `.\gradlew.bat :domain:test :data:testDebugUnitTest :app:testDebugUnitTest --rerun-tasks` → BUILD SUCCESSFUL in 2m 47s (76 executed). Reports: `:domain` 143, `:data` 43, `:app` 77, 0 failures, 0 errors, 0 skipped. `lintDebug`: `:app` 0 errors, 1 warning (`NewerVersionAvailable`, JUnit 5.14.4); `:data` no issues. Not from this spike. `adb install -r` Success. `adb devices`: emulator-5554. `ro.build.version.release` 16, `ro.build.version.sdk` 36, `ro.boot.qemu.avd_name` medium_phone.
- 2026-10-29 REVIEW: Owner review: no findings (Blocker / Should-fix / Nit: none). Moved to `verify/`. Nothing to install; D-025 is already confirmed.
- 2026-10-28 DOCS: Renamed from "seed source and license". Delivery is decided (D-021); this spike is format, CI, and URL layout only.
- 2026-10-29 SPIKE: **Owner confirmed** everything: recorded D-025 and noted it on D-005. Repo will be public for Pages. Two-step flow: PRs into `catalogue`, then a release PR into `main`. CI-only `check-jsonschema` approved. Created T-022 for the checker, workflows, PR template, and `.gitattributes`. Moved to `review/`. Ran `.\gradlew.bat :domain:test assembleDebug`: BUILD SUCCESSFUL (no code changed).
- 2026-10-29 SPIKE: Proposal written; waiting for owner confirmation before editing `docs/DECISIONS.md`, moving to `review/`, and committing.
  - Added `docs/CATALOGUE.md` (spec), `catalogue/catalogue.schema.json` (JSON Schema 2020-12), and `catalogue/catalogue.json` (10 exercises covering all 8 types, version 1, CC0-1.0). Linked from ARCHITECTURE §2.
  - Recommendation: JSON. Integer `version` must be strictly greater than the base branch and the live file. `formatVersion` matches the `/vN/` URL segment. Ids are kebab slugs equal to `slug(title)` at creation, never changed, and moved into `retiredIds` rather than deleted. Published at `https://t0liver.github.io/muskli/catalogue/v1/catalogue.json` via the Actions Pages deploy. Whole catalogue is CC0; no Hevy or share-alike sources.
  - Verified: a throwaway stdlib Python check (not committed) of the staged blob passed rules 1 and 3–7 of CATALOGUE §5, minus the git-base comparisons. Full JSON Schema validation was not run because `jsonschema` isn't installed and I didn't add it. No Gradle run; no code changed.
  - Gotcha: `core.autocrlf=true` makes the Windows working copy CRLF, while committed blobs are LF. Proposed fix: `.gitattributes` with `catalogue/** text eol=lf`.
  - Follow-ups once confirmed: a BUILD ticket for `check_catalogue.py`, the PR workflow, the Pages workflow, the PR template, and `.gitattributes`. Proposed D-025 (catalogue format).
