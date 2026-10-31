# T-028 — Live-workout header stays readable while scrolling

- **Role:** UI
- **Phase:** 1
- **Depends on:** T-008
- **Refs:** SPEC §2, F-02 · docs/ARCHITECTURE.md §6
- **Size:** fits one session

## Goal
Scrolling a live workout no longer draws set rows through the title, duration, volume, or set count.

## In scope
- Give the pinned workout header an opaque surface in light and dark so content scrolling underneath does not show through.
- Keep the existing header contents and tap targets.

## Out of scope (do NOT do)
- New header actions, layout changes beyond making the bar cover the list, or other screens.

## Acceptance criteria
- [x] On a workout long enough to scroll, the header text stays readable. Set rows do not show through the title or the duration / volume / sets row.
- [x] Light and dark both hold. Touch targets stay at least 48dp.

## Verification
Scroll `LiveWorkoutAllSetTypesPreview` on the emulator (or the live workout screen) and confirm the header covers the rows. `build/verify/t008-all-set-types-scrolled.png` from the T-008 verify shows the overlap.

## Notes / decisions needed
Found in the T-008 verify (2026-10-30). `WorkoutHeader` has no background, and `LiveWorkoutContent` lays the list out behind the Scaffold top bar.

## Handoff log
<!-- Newest first. Each entry: date, role, what was done, what's left, gotchas, commands run + results. -->
- **2026-10-31 · UI** · Wrapped the Scaffold `topBar` in `LiveWorkoutContent` in a Material3 `Surface`. The insets padding now sits inside it, so the bar is opaque from the status bar down to the divider. `WorkoutHeader` is unchanged (contents, tap targets, previews). No new strings. Theme `surface` equals `background` in light and dark, so the bar looks the same as before when nothing is scrolled under it. A side effect: the opaque `Surface` also stops taps on the header area from reaching rows scrolled behind it.
  - Verified on emulator `medium_phone` (API 36, 1080x2400, 420 dpi) with the debug build from this branch. Added 7 sets to the existing live workout so the list scrolls, then scrolled partway. Rows clip at the header divider in light and dark (`build/verify/t028-light-scrolled.png`, `build/verify/t028-dark-scrolled.png`, both git-ignored). A uiautomator dump gives Finish 126 px tall (48dp) and the duration stat 148 px tall (about 56dp).
  - Commands: `.\gradlew.bat :domain:test testDebugUnitTest assembleDebug lintDebug`: BUILD SUCCESSFUL. Lint has 1 issue, the pre-existing `NewerVersionAvailable`, and none in the changed file.
  - Left: nothing. No owner decisions needed. I did not check the preview renders in Android Studio. The emulator's live workout now has the 7 extra empty sets.
