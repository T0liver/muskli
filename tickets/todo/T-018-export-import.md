# T-018 — Export / import (own JSON format)

- **Role:** BUILD
- **Phase:** 1
- **Depends on:** T-016
- **Refs:** SPEC F-08 · TC-DATA-05, 06 · AC-06 · docs/ARCHITECTURE.md §8 · research/06

## Goal
One-file JSON export and safe, all-or-nothing import, via the system file picker.

## In scope
- `schemaVersion`, kotlinx.serialization models mirroring Hevy-like shapes where reasonable; validation; single DB transaction; error report.
- Round-trip test on a realistic fixture.

## Out of scope (do NOT do)
- Cloud backup, scheduling, encryption, Hevy import (T-020 and later).

## Acceptance criteria
- [ ] TC-DATA-05 and 06 pass; AC-06 steps work on the device.

## Handoff log
- 2026-10-29 DOCS: D-024 is OS Auto Backup, already on via `allowBackup`. This ticket stays the manual JSON file. "Cloud backup" above means do not build a sync or upload feature.