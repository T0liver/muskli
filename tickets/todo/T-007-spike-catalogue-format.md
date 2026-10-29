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
- [ ] Schema + sample file + CI checklist documented (link from `docs/ARCHITECTURE.md` §2).
- [ ] Format is sufficient for T-010 (bundled load) and T-021 (full sync): version field, stable ids, URL layout.

## Handoff log
- 2026-10-28 DOCS: Renamed from "seed source and license". Delivery is decided (D-021); this spike is format, CI, and URL layout only.
