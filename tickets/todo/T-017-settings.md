# T-017 — Settings

- **Role:** BUILD
- **Phase:** 1
- **Depends on:** T-001
- **Refs:** SPEC F-07 · D-009

## Goal
Settings screen backed by DataStore with typed accessors: units, default rest, previous mode (stored, only "anywhere" active), keep awake, sound/vibration, week start.

## In scope
- Settings repository (interface in `:domain`, implementation in `:app` or `:data`), screen, tests for defaults and persistence.

## Out of scope (do NOT do)
- Settings for features that don't exist yet.

## Acceptance criteria
- [ ] Defaults match D-009 (once confirmed); changes persist across restarts.
- [ ] Settings expose `Flow`s so screens react live.

## Handoff log
