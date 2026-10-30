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
- [x] TC-DATA-05 and 06 pass; AC-06 steps work on the device (emulator; the owner's phone is the VERIFY ticket's job).

## Handoff log
- 2026-10-29 DOCS: D-024 is OS Auto Backup, already on via `allowBackup`. This ticket stays the manual JSON file. "Cloud backup" above means do not build a sync or upload feature.
- **2026-10-30 (BUILD):** Done. Owner choices recorded as **D-034** (kotlinx.serialization, Replace import, settings included).
  - **Dependency:** `org.jetbrains.kotlin.plugin.serialization` at `kotlin` (2.4.21) and `kotlinx-serialization-json` **1.12.0** (latest stable on Maven Central, checked 2026-10-30), `:data` only.
  - **Domain:** new `repo/BackupRepository.kt` (`export()`, `import(json)`, `ImportResult`, `ImportError(problem, where)`, `ImportProblem`). Nothing existing changed.
  - **Data:** `backup/BackupFile.kt` (file model), `backup/BackupMapping.kt` (rows to JSON, and `validate()` that collects every problem with a JSON path), `backup/JsonBackupRepository.kt`, and `db/dao/BackupDao.kt` (queries only, registered on `MuskliDatabase`; **no schema change**, still v3).
  - **App:** a Backup section at the bottom of Settings (`ui/backup/`): Export opens the system Save picker (`muskli-backup-YYYY-MM-DD.json`); Import opens the Open picker, then asks "Replace all data?", then shows "Imported N workouts." or the error list. `BackupDocuments` wraps the content resolver so `BackupViewModel` stays JVM-testable.
  - **Gotchas:** DataStore cannot join the Room transaction, so settings are written right after the commit; they were validated first, so only an I/O failure could leave the workouts imported and the settings not. The catalogue meta version is not exported (the device keeps its own). A catalogue exercise already on the device keeps its own title and muscles. `ignoreUnknownKeys` is on, so a file with extra fields from a later minor change still imports. Lint `UseKtx` is suppressed on `ContentResolverBackupDocuments` because core-ktx is not a declared dependency.
  - **Tests:** `JsonBackupRepositoryTest` (Robolectric, in-memory Room): **TC-DATA-05** round trip into an empty DB (same rows, settings, and byte-identical re-export), Replace keeps the device catalogue, **TC-DATA-06** two bad records apply nothing and both are reported, and rejections for non-JSON, missing fields, newer `schemaVersion`, two drafts, and a custom/catalogue id clash. `BackupViewModelTest` (confirm before import, cancel, error report, read/write failures).
  - **Commands:** `.\gradlew.bat :domain:test testDebugUnitTest assembleDebug lintDebug` → BUILD SUCCESSFUL, 263 tests, 0 failures; lint 0 errors, only the existing `NewerVersionAvailable` warning.
  - **AC-06 on the emulator (medium_phone, API 36):** set lb, Export to Downloads ("Backup saved.", 4.3 kB file holding soft-deleted workouts too), `adb shell pm clear hu.toliver.muskli`, relaunch (History empty, kg), Import → confirm → "Imported 4 workouts."; History shows both workouts as before and the unit is lb again.
  - **Left:** AC-06 on the owner's phone (T-019).
