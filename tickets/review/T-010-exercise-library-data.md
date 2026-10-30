# T-010 — Exercise library: bundled catalogue + repository

- **Role:** BUILD
- **Phase:** 1
- **Depends on:** T-006, T-007 (D-005, D-021)
- **Refs:** SPEC F-01 AC1–4 · docs/ARCHITECTURE.md §2 · D-021

## Goal
Offline exercise library from a bundled catalogue snapshot, with search/filter queries and custom-exercise CRUD at the repository level.

## In scope
- Bundled snapshot in `:data` assets, same format and version field as T-007. Load on startup when the asset version is newer than the stored catalogue version; upsert by id; idempotent (no duplicates).
- Do not modify custom exercises. Do not hard-delete a published exercise a workout references; archive unreferenced rows missing from the bundle.
- Repository: search by name, filter by muscle/equipment, create/edit/archive custom, recently used.
- Tests for search/filter/CRUD, version gate, and idempotency.

## Out of scope (do NOT do)
- Network sync (T-021). UI, media/images. Inventing catalogue content beyond the T-007 sample copied into assets.

## Acceptance criteria
- [x] First launch with an empty DB loads the bundle. A second launch at the same version does not duplicate.
- [x] A newer bundled version upserts; an older or equal version does not.
- [x] Search is case/diacritic-insensitive; filters combine with AND.
- [x] Custom exercises are unlimited, distinguishable (`isCustom`), and untouched by the loader.

## Handoff log
- 2026-10-28 DOCS: Owner pinned D-021. This ticket is the bundled snapshot only. Network sync is T-021.
- 2026-10-30 BUILD: Started at the owner's request although T-007 is in `verify/`, not `done/` (D-025 is decided, so the format is fixed).
  - **Owner decisions (recorded as D-028):** schema v2 with a one-row `catalogue_meta(id, version)` table written in the same transaction as the upsert; muscle filter matches the primary muscle only; recently used = distinct templates from non-deleted workouts including the draft, newest workout start first, archived excluded, with a limit. Owner approved the `:domain` API additions below.
  - **`:domain`:** `repo/Clock` (`fun interface`, epoch ms; not `java.time.Clock` because minSdk 24 has no `java.time` without desugaring). `catalogue/CatalogueUpdate.kt`: `CatalogueBundle`, `CatalogueExercise`, `SUPPORTED_CATALOGUE_FORMAT`, and the pure `planCatalogueUpdate(bundle, storedVersion, existing, referencedIds, now)`, which returns null for an unknown format or a version that isn't newer. Otherwise it returns the changed upserts (creation time kept, unchanged rows skipped, archived rows restored when they come back) and the archive ids. `library/ExerciseSearch.kt`: `ExerciseFilter` + `searchExercises` (NFD and strip combining marks, lowercase, AND filters, archived hidden unless asked, sorted by normalized title). `usecase/library/`: `SearchExercisesUseCase`, `Create/Edit/ArchiveCustomExerciseUseCase` over the existing `upsert` (edit/archive refuse non-custom ids, blank title throws, primary muscle dropped from secondaries). `ExerciseTemplateRepository.observeRecentlyUsed(limit)` added.
  - **`:data`:** schema v2 (`CatalogueMetaEntity`, `CatalogueMetaDao`, `MIGRATION_1_2`, `schemas/.../2.json`). `ExerciseTemplateDao` gained `getAll`, `observeRecentlyUsed`, `referencedTemplateIds` (soft-deleted workouts count, since they can be restored), `archive`. `seed/CatalogueParser` (platform `org.json`, ignores unknown fields, skips exercises for an unknown `formatVersion`), `seed/CatalogueApplier` (reads version + library, plans, and writes in one `withTransaction`; T-021 can reuse it), `seed/BundledCatalogueLoader`, `repo/RoomExerciseTemplateRepository`, `SystemWallClock`. Gradle task `copyBundledCatalogue` copies only `catalogue/catalogue.json` into generated assets, so there's no second copy kept by hand and the schema/tools aren't shipped. `room-runtime` is now `api` so `:app` can provide `MuskliDatabase` through Hilt (no new dependency).
  - **`:app`:** `di/DataModule` (database, clock, ids, exercise repository, loader). `MuskliApplication` runs the loader once on `Dispatchers.Default` at startup and logs and continues on failure (D-021).
  - **Tests:** domain `CatalogueUpdateTest` (11), `ExerciseSearchTest` (8), `ExerciseLibraryUseCasesTest` (8, including 500 custom exercises). Data `CatalogueLoaderTest` (6, real bundled asset: first load, same-version no-op, newer upsert, older/equal refused, custom untouched, referenced kept / unreferenced archived), `CatalogueParserTest` (4), `RoomExerciseTemplateRepositoryTest` (3), `MigrationTest` 1→2 (TC-DATA-04). The domain tests failed to compile before the implementation.
  - **Commands:** `.\gradlew.bat :domain:test testDebugUnitTest assembleDebug lintDebug` → BUILD SUCCESSFUL. `:domain` 77 tests and `:data` 25 tests, 0 failures. The APK contains `assets/catalogue/catalogue.json` (2612 bytes). Lint: 2 warnings (ObsoleteLintCustomCheck, NewerVersionAvailable), neither from this change. Not run: on-device launch, `connectedDebugAndroidTest` (no instrumented tests).
  - **Gotchas:** with `core.autocrlf=true` the bundled file is CRLF on Windows. Parsing doesn't care; the `.gitattributes` fix is T-022. A published exercise missing from the bundle but referenced by a workout stays as it is, still active, per the literal reading of D-021.
  - **Left:** nothing in scope. Network sync is T-021.
