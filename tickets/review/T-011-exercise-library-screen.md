# T-011 — Exercise library screen

- **Role:** UI+BUILD
- **Phase:** 1
- **Depends on:** T-008, T-010
- **Refs:** SPEC F-01 · T-008 tokens. The sorted set in `docs/reference-ui/` has no exercise-library folder; do not block on more screenshots.

## Goal
Library screen with search, filters, recents, create/edit custom exercise form, and a picker mode used by "Add exercise" (multi-select).

## In scope
- ViewModel + UiState + Compose screen; create form with type/primary muscle/secondary muscles/equipment pickers.
- ViewModel tests with fake repository; one Compose UI test for search + select.

## Out of scope (do NOT do)
- Exercise detail/stats/charts, media, the live-workout screen itself.

## Acceptance criteria
- [x] Search, filter, recents work; multi-select returns chosen ids.
- [x] Custom exercise can be created, edited, archived.
- [x] Works in dark mode and with large fonts.

## Handoff log
- 2026-10-30 UI+BUILD: Started at the owner's request although T-008 is in `review/`, not `done/`. Branch `t-011-exercise-library` is cut from `t-017-settings` because it reuses that branch's home screen, `BackTopBar`, and lifecycle/Hilt-Compose dependencies; merge T-017 first.
  - **Owner decisions (this session):** `androidx.compose.ui:ui-test-junit4` + `ui-test-manifest` (from the existing Compose BOM) approved; the Compose UI test runs on the JVM with Robolectric in `app/src/test`. No navigation library: the library opens from the home screen.
  - **No `:domain` changes.** Uses the T-010 `SearchExercisesUseCase`, `Create/Edit/ArchiveCustomExerciseUseCase`, and `ExerciseTemplateRepository.observeRecentlyUsed` (limit 10). `di/UseCaseModule` provides the use cases (`:domain` has no `javax.inject`).
  - **`:app` `ui/library/`:** `LibraryViewModel` (`StateFlow<LibraryUiState>`, sealed `LibraryEvent`): query + primary-muscle + equipment filters (AND, D-028), recents shown only with no query and no filter, multi-selection kept in tap order across searches, create/edit form state. `LibraryScreen` + `LibraryRoute(mode, onBack, onPicked)`. `LibraryMode.BROWSE` is opened from home, and a tap on a custom exercise opens the form (catalogue entries aren't editable). `LibraryMode.PICK` toggles a checkbox per row and a bottom "Add N" button calls `onPicked(ids)`. T-013 wires that to "Add exercise". `ExerciseEditorScreen`: name, type (default weight and reps), primary muscle and equipment (both required; errors show only after a save attempt), secondary muscle chips (the primary muscle is left out and dropped), archive with a confirmation dialog. A new exercise is auto-selected, so creating from the picker adds it. `LibraryLabels` maps the enums to `strings.xml` (own wording). Previews: browse light/dark, pick filtered, pick 1.5x font, editor new/errors light/dark, editor edit 1.5x font.
  - **Tests:** `LibraryViewModelTest` (13, fake repository + real use cases): sorted list, case/diacritic search, combined filters, recents hidden while filtering, tap-order multi-select, selection survives search, required-field errors, create (selected, secondaries cleaned), primary removed from secondaries, edit, catalogue not editable, archive (hidden and deselected), dismiss. `LibraryScreenTest` (Robolectric Compose): type "squat", the other exercises disappear, tap the row, "Add 1" returns `["squat-barbell"]`.
  - **Commands:** `.\gradlew.bat :domain:test testDebugUnitTest assembleDebug lintDebug`: BUILD SUCCESSFUL, 0 failures (`:app` 23 tests). Lint: only the existing junit-bom `NewerVersionAvailable` (D-016). On the `medium_phone` emulator with the bundled catalogue: library in dark mode; created "Landmine Press" (shoulders, barbell, triceps secondary), found it by "landm", edited and archived it at font scale 1.5; it disappeared from results.
  - **Not checked on a device:** recents (no workout can be logged until T-013) and the picker (not reachable from home); both are covered by the tests above.
  - **Gotchas:** `app/src/test/resources/robolectric.properties` sets `application=android.app.Application`, so Robolectric tests don't start Hilt and the catalogue loader. LazyColumn keys are prefixed by section because an exercise can be in both Recent and All.
  - **Left:** nothing in scope. There is no un-archive UI; nothing asked for it.
