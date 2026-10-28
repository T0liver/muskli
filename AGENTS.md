# AGENTS.md

Personal Android workout tracker, inspired by Hevy. **One user (the owner). Offline-first. Native Kotlin + Jetpack Compose.**
Working name: **Muskli** (`hu.toliver.muskli`, see docs/DECISIONS.md D-008).

This file is the rulebook. Keep it short; details live in the linked docs.

## 1. Session start (always, in this order)
1. Read this file.
2. Look in `tickets/doing/`. If a ticket is there, **continue it**. Otherwise take the **lowest-numbered ticket in `tickets/todo/` whose dependencies are in `tickets/done/`** and `git mv` it to `tickets/doing/`.
3. Read only what the ticket references: the relevant part of `SPEC.md`, `docs/ARCHITECTURE.md`, the listed test cases in `docs/TEST-CASES.md`, and `docs/research/` files **only if the ticket cites them**.
4. State your plan in 3-6 lines before writing code.

## 2. Prime directives: do the ticket, nothing more
- **One ticket per session.** Meet its acceptance criteria and stop.
- **No scope creep.** No extra features, refactors, "cleanups", renamed files, or new abstractions beyond the ticket. If you spot something worth doing, create a ticket in `tickets/todo/` (use `tickets/TEMPLATE.md`) and move on.
- **Ask the owner before:** adding or upgrading a dependency, changing the DB schema, changing a public `:domain` API, changing `SPEC.md` scope, changing an existing test's expected values, or choosing between options listed as open in `docs/DECISIONS.md`.
- **Never make a failing test pass by editing the test.** If a test looks wrong, stop and flag it in the ticket's Handoff log.
- **Never invent product rules.** If a rule is unknown, check `docs/DECISIONS.md` and `docs/research/10-open-questions-and-decisions.md`. If still unknown, ask.
- **The logging screen is sacred.** Never add a tap or a visible control to the live-workout flow unless it is behind a setting or the ticket says so.
- **Do not copy Hevy's assets, text, icons, videos, or exercise data.** Ideas and standard data shapes only.

## 3. Architecture in one breath
Three Gradle modules: `:domain` (pure Kotlin/JVM: models, calculations, repository interfaces, use cases), `:data` (Room, repository implementations, seed, import/export), `:app` (Compose UI, ViewModels, DI, services). Unidirectional data flow: UI → ViewModel (`StateFlow<UiState>`) → use case → repository. Details: `docs/ARCHITECTURE.md`.

## 4. Code rules
- **Business logic never lives in composables or ViewModels' UI code.** It lives in `:domain` as pure functions/use cases with unit tests.
- **Test first for `:domain`.** Write tests from `docs/TEST-CASES.md`, see them fail, then implement.
- Weights are stored in **kg**; convert only at the UI edge.
- Inject time (`Clock`) and IDs; no `System.currentTimeMillis()` / `UUID.randomUUID()` in domain code.
- Immutable data classes, sealed types for states/events, coroutines + Flow. No global mutable state.
- Composables: stateless where possible (state hoisting), one `@Preview` per screen/component, no hardcoded user-visible strings (use `strings.xml`), touch targets at least 48dp, support dark mode.
- Names: packages by feature under each module; `XxxScreen`, `XxxViewModel`, `XxxUiState`, `XxxUseCase`, `XxxRepository`.
- Comments explain *why*, not *what*. No dead code, no commented-out code, no TODO without a ticket id (`TODO(T-123)`).
- Dependencies come from `gradle/libs.versions.toml` only.

## 5. Commands (confirm/fill in during T-001)
```
./gradlew :domain:test            # fast pure-Kotlin tests (run constantly)
./gradlew testDebugUnitTest       # all JVM unit tests
./gradlew assembleDebug           # build
./gradlew lintDebug               # Android lint
./gradlew connectedDebugAndroidTest   # instrumented/UI tests (device/emulator)
```
Run `:domain:test` and `assembleDebug` before declaring any ticket done. Never claim something works without running it; if you couldn't run it, say so.

## 6. Session end (always)
1. Run the checks above. Fix what you broke.
2. Update the ticket: tick acceptance criteria, add a **Handoff log** entry (what was done, what's left, gotchas, commands run, results).
3. Move the ticket with `git mv`: BUILD/UI/SPIKE tickets → `tickets/review/`; REVIEW tickets → `tickets/verify/` or back to `todo/` with findings; VERIFY tickets → `tickets/done/`.
4. Commit: `T-XXX: short imperative summary`. Small commits are fine; one ticket's work per branch `t-xxx-slug`.
5. Reply with: summary, files changed, how you verified, open questions.

## 7. Roles (a session wears exactly one hat; ticket says which)
`BUILD` · `UI` · `REVIEW` · `VERIFY` · `SPIKE` · `DOCS` — responsibilities and prompts in `docs/WORKFLOW.md`.

## 8. Where things are
| What | Where |
|---|---|
| Product scope & acceptance | `SPEC.md` |
| Technical design | `docs/ARCHITECTURE.md` |
| Decisions (decided/open) | `docs/DECISIONS.md` |
| Test cases (IDs `TC-*`, `AC-*`) | `docs/TEST-CASES.md` |
| Process & prompts | `docs/WORKFLOW.md` |
| Tickets by status | `tickets/{todo,doing,review,verify,done}/` |
| Background research on Hevy | `docs/research/` (read-only reference) |
| Screenshots of the reference app | `docs/reference-ui/` (personal reference only) |
