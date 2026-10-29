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
- 2026-10-28 DOCS: Renamed from "seed source and license". Delivery is decided (D-021); this spike is format, CI, and URL layout only.
- 2026-10-29 SPIKE: **Owner confirmed** everything: recorded D-025 and noted it on D-005. Repo will be public for Pages. Two-step flow: PRs into `catalogue`, then a release PR into `main`. CI-only `check-jsonschema` approved. Created T-022 for the checker, workflows, PR template, and `.gitattributes`. Moved to `review/`. Ran `.\gradlew.bat :domain:test assembleDebug`: BUILD SUCCESSFUL (no code changed).
- 2026-10-29 SPIKE: Proposal written; waiting for owner confirmation before editing `docs/DECISIONS.md`, moving to `review/`, and committing.
  - Added `docs/CATALOGUE.md` (spec), `catalogue/catalogue.schema.json` (JSON Schema 2020-12), and `catalogue/catalogue.json` (10 exercises covering all 8 types, version 1, CC0-1.0). Linked from ARCHITECTURE §2.
  - Recommendation: JSON. Integer `version` must be strictly greater than the base branch and the live file. `formatVersion` matches the `/vN/` URL segment. Ids are kebab slugs equal to `slug(title)` at creation, never changed, and moved into `retiredIds` rather than deleted. Published at `https://t0liver.github.io/muskli/catalogue/v1/catalogue.json` via the Actions Pages deploy. Whole catalogue is CC0; no Hevy or share-alike sources.
  - Verified: a throwaway stdlib Python check (not committed) of the staged blob passed rules 1 and 3–7 of CATALOGUE §5, minus the git-base comparisons. Full JSON Schema validation was not run because `jsonschema` isn't installed and I didn't add it. No Gradle run; no code changed.
  - Gotcha: `core.autocrlf=true` makes the Windows working copy CRLF, while committed blobs are LF. Proposed fix: `.gitattributes` with `catalogue/** text eol=lf`.
  - Follow-ups once confirmed: a BUILD ticket for `check_catalogue.py`, the PR workflow, the Pages workflow, the PR template, and `.gitattributes`. Proposed D-025 (catalogue format).
