# T-017 — Settings

- **Role:** BUILD
- **Phase:** 1
- **Depends on:** T-001
- **Refs:** SPEC F-07 · D-009, D-017

## Goal
Settings screen backed by DataStore with typed accessors: units, default rest, previous mode (stored, only "anywhere" active), keep awake, sound/vibration, week start, current body weight.

## In scope
- Settings repository (interface in `:domain`, implementation in `:app` or `:data`), screen, tests for defaults and persistence.
- Body weight: stored in kg, default unset (null), editable on the settings screen (D-017). No measurement history.

## Out of scope (do NOT do)
- Settings for features that don't exist yet. Body-measurement log (Phase 2).

## Acceptance criteria
- [ ] Defaults match D-009; body weight defaults to unset (D-017). Changes persist across restarts.
- [ ] Settings expose `Flow`s so screens react live.

## Handoff log
- 2026-10-28 DOCS: Owner pinned D-017. Current body weight is a single settings value.
