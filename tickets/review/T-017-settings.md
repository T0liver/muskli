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
- [x] Defaults match D-009; body weight defaults to unset (D-017). Changes persist across restarts.
- [x] Settings expose `Flow`s so screens react live.

## Handoff log
- 2026-10-28 DOCS: Owner pinned D-017. Current body weight is a single settings value.
- 2026-10-30 BUILD: Started at the owner's request although T-001 is in `verify/`, not `done/` (same as T-008).
  - **Owner decisions (this session):** new dependencies `androidx.datastore:datastore-preferences` 1.3.0 (`api` in `:data`), `androidx.lifecycle:lifecycle-viewmodel-compose` + `lifecycle-runtime-compose` 2.11.0, `androidx.hilt:hilt-lifecycle-viewmodel-compose` 1.4.0 (latest stable on Maven today). The `:domain` API below is approved, with the DataStore implementation in `:data`. Sound and vibration are two toggles, both on by default. Week start can be any of the seven days. Default rest is free entry in whole seconds, or Off. Screens are reached from a simple home screen with no navigation library (navigation-compose 2.10.2 was approved but not added, since the owner chose the minimal home).
  - **`:domain`:** `settings/Settings.kt` (`Settings` with the D-009/D-017 defaults, `WeightUnit { KG, LB }`, `WeekStartDay` with seven values; own enum since minSdk 24 has no `java.time`). `repo/SettingsRepository` (`settings: Flow<Settings>` + one suspend setter per field; rest and body weight reject non-positive values, null means Off / unset).
  - **`:data`:** `settings/DataStoreSettingsRepository`. Missing keys and unknown enum names read as defaults. Rest "Off" is stored as `0` so it differs from "never set" (default 90 s). Clearing body weight removes the key.
  - **`:app`:** `di/DataModule` provides a singleton `DataStore<Preferences>` (file `settings.preferences_pb`) and the repository. `ui/settings/` `SettingsViewModel` (`StateFlow<SettingsUiState>`, sealed `SettingsEvent`), `SettingsScreen` + `SettingsRoute` with previews (defaults, dark, 1.5x font, lb/rest off/invalid). Text fields keep what was typed, and an invalid entry shows an error and is not saved. Body weight is entered in the current unit and stored in kg (`lbToKg`); a decimal comma is accepted. "Last time in this routine" is shown disabled until routines exist (T-024). `ui/components/BackTopBar`, own `ic_arrow_back`. `ui/home/HomeScreen` replaces the placeholder; `MainActivity` switches Home/Settings with `rememberSaveable` + `BackHandler`.
  - **Tests:** domain `SettingsTest` (2, failed to compile before the model existed). Data `DataStoreSettingsRepositoryTest` (6: defaults, every field across a reopened DataStore, rest Off vs never set, clear body weight, non-positive rejected, Flow emits on change). App `SettingsViewModelTest` (8, fake repository).
  - **Commands:** `.\gradlew.bat :domain:test testDebugUnitTest assembleDebug lintDebug`: BUILD SUCCESSFUL, 0 failures. Lint: only the existing `NewerVersionAvailable` for junit-bom (D-016). On the `medium_phone` emulator: defaults shown, then lb + sound off + 176 lb survived `am force-stop` and relaunch; switching to kg showed 79.8; dark mode checked.
  - **Gotchas:** the DataStore test runs under Robolectric. On the plain JVM, `Build.VERSION.SDK_INT` is 0, so DataStore falls back to `File.renameTo`, which cannot replace an existing file on Windows and fails the second write. Keep-awake and sound/vibration are only stored; T-013 and T-015 apply them.
  - **Left:** nothing in scope.
