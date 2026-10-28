# T-007 — SPIKE: exercise seed source and license

- **Role:** SPIKE
- **Phase:** 0
- **Depends on:** none
- **Refs:** D-005 · research/09 §3 · research/04 enums

## Goal
Define the **catalogue file format**, validation rules, and GitHub Pages API shape per **D-005** (owner decided 2026-10-28).

## In scope
- Pick JSON vs TOML (or other) with rationale; schema covering our enums (type, muscles, equipment, etc.).
- CI checker spec: what fails a PR on the `catalogue` branch; merge/publish flow to GitHub Pages.
- Sample catalogue of ~10 exercises; document the published API URL layout and version field for app full-sync.
- License/attribution rules for contributed exercises (no Hevy data).

## Out of scope (do NOT do)
- App loader implementation (T-010), delta-sync, copying Hevy's library/media.

## Acceptance criteria
- [ ] Schema + sample file + CI checklist documented (link from ARCHITECTURE §2).
- [ ] D-005 notes sufficient for T-010 to implement sync.

## Handoff log
