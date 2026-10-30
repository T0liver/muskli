# T-025 — Un-archive a custom exercise

- **Role:** UI+BUILD
- **Phase:** 1
- **Depends on:** T-010, T-011
- **Refs:** SPEC F-01 AC3 · `ArchiveCustomExerciseUseCase` · `ExerciseFilter.includeArchived`
- **Size:** fits one session

## Goal
An archived custom exercise can be found again and restored, so it shows up in search, filters, and the picker.

## In scope
- A use case that clears `archived` on a custom exercise and updates its timestamp. Refuse a missing id and a catalogue exercise, the same way archive does.
- Library screen: a way to list archived custom exercises (the search already hides them unless `includeArchived` is set), and an action that restores the one the user picks.
- Tests: restore brings it back into search results; a catalogue id is refused.

## Out of scope (do NOT do)
- Un-archiving a catalogue exercise. Those are archived by a bundle update (D-021) and the next sync would archive an unreferenced one again.
- Hard delete. A body-measurement or history change.

## Acceptance criteria
- [x] An archived custom exercise can be found and restored, then matches search and filters again.
- [x] Restoring does not create a second row, and workouts that referenced it are unchanged.
- [x] A catalogue exercise cannot be restored from this screen.

## Verification
`./gradlew :domain:test testDebugUnitTest assembleDebug`

## Notes / decisions needed
Owner asked for this on 2026-10-30, after T-011. SPEC F-01 AC3 says archive and does not mention restore; this ticket is that addition. Do not edit `SPEC.md` unless the owner asks.

## Handoff log
- 2026-10-30: Created. T-011 archives a custom exercise and the library then hides it. Nothing lists archived rows or clears the flag.
- 2026-10-30 UI+BUILD: Branch `t-025-unarchive-custom-exercise`, cut from `t-011-exercise-library` (the screen lives there). T-011 is in `review/`, not `done/`.
  - **`:domain`:** `UnarchiveCustomExerciseUseCase`. Clears `archived` on the same row and updates the timestamp. Returns false for a missing id, a catalogue exercise, or one that is not archived, and writes nothing in those cases. Workouts are untouched because only the template row is written.
  - **`:app`:** `LibraryViewModel` takes the use case. An **Archived** chip lists archived custom exercises (search still applies). Catalogue rows stay hidden. Tapping a row restores it. Recents stay hidden while the chip is on. The chip row wraps (`FlowRow`) so it fits a large font.
  - **Tests:** domain: restore returns the same id to search with its muscle and equipment filter, and refuses published, missing, and already-active ids. ViewModel: the chip lists only archived custom exercises, restore puts that id back into search, a catalogue id is refused.
  - **Commands:** `.\gradlew.bat :domain:test testDebugUnitTest assembleDebug lintDebug` BUILD SUCCESSFUL, 0 failures. Lint: only the existing junit-bom `NewerVersionAvailable` (D-016). On `medium_phone`: the Archived chip showed the previously archived "Landmine Press" with Restore; after Restore, searching "landm" listed it under All exercises.
  - **Left:** nothing in scope. `SPEC.md` was not edited.
- 2026-10-30: Owner review: no findings (Blocker / Should-fix / Nit: none). Moved to `done/`.
