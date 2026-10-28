# T-001 — Project scaffold

- **Role:** BUILD
- **Phase:** 0
- **Depends on:** none (needs owner answers to D-006, D-007, D-008, D-016 first; ask if still open)
- **Refs:** docs/ARCHITECTURE.md §1–2, §9 · AGENTS.md §5

## Goal
An empty but healthy Android project: three modules, version catalog, test tooling, app launches a placeholder screen.

## In scope
- Gradle Kotlin DSL project with `:domain` (pure Kotlin/JVM), `:data` (Android library), `:app` (Compose, Material 3).
- `gradle/libs.versions.toml` with only the dependencies needed now (versions verified as current stable at the time, not from memory).
- Test setup: JUnit + kotlin.test in `:domain`; a trivial passing test per module.
- `.gitignore`, package id from D-008, minSdk from D-007, edge-to-edge + dark-mode-ready theme stub.
- Fill the **Commands** section of AGENTS.md with the commands that actually work.

## Out of scope (do NOT do)
- Any feature, screen beyond a placeholder, database code, DI beyond what D-006 requires, CI, analytics/crash SDKs.

## Acceptance criteria
- [ ] `./gradlew :domain:test` passes.
- [ ] `./gradlew assembleDebug` builds; app installs and shows the placeholder.
- [ ] `:domain` has no Android imports (verified).
- [ ] AGENTS.md commands section updated to reality.

## Verification
Run the commands above; install on the owner's phone or an emulator.

## Handoff log
