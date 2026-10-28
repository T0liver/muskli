# WORKFLOW.md: how sessions and tickets work

Goal: any fresh agent session can pick up exactly one small piece of work, do it, hand it off, and stop.

## 1. Ticket lifecycle = folders
Status is the folder the ticket file is in (single source of truth; move with `git mv`).

```
todo/  ->  doing/  ->  review/  ->  verify/  ->  done/
                         |             |
                         +-- findings -+--> back to todo/ (new/updated ticket)
```
- **WIP limit: 1** ticket in `doing/`. A new session continues it before starting anything else.
- Order = filename number (T-001, T-002 ...). Take the lowest number whose `Depends on` tickets are all in `done/`.
- `SPIKE` tickets (research/experiments) end with a written recommendation in their Handoff log and a proposed update to `docs/DECISIONS.md`, never with production code.
- New ideas = new ticket in `todo/` using `tickets/TEMPLATE.md`, never silent extra work.

## 2. A slice = the smallest vertical piece that is verifiable
A good ticket has: one goal, explicit in/out of scope, checkable acceptance criteria, referenced `TC-*` / `AC-*` / `SPEC F-*` ids, and fits in one session. If it can't, split it. Prefer vertical slices (UI + state + persistence + tests for one behavior) over horizontal layers once the domain foundation exists.

## 3. Roles (one hat per session)

| Role | Does | Must not |
|---|---|---|
| **BUILD** | Implements the ticket (tests first for `:domain`), runs checks, writes handoff | Expand scope, edit test expectations, add dependencies unasked |
| **UI** | Designs/implements Compose screens/components against tokens and reference screenshots; previews; accessibility | Put business logic in UI; add taps to the logging flow; copy Hevy assets |
| **REVIEW** | Reads the diff against the ticket, SPEC, ARCHITECTURE, AGENTS rules; runs checks; lists findings by severity | Fix code itself (only report), except trivial typos; approve with failing checks |
| **VERIFY** | Executes `AC-*` scenarios / ticket verification steps on a real build; records evidence | Change code; mark pass without running it |
| **SPIKE** | Time-boxed investigation; produces a recommendation with sources/measurements | Merge production code |
| **DOCS** | Updates SPEC/ARCHITECTURE/DECISIONS/tickets when the owner decides something | Change product scope without owner instruction |

Usually the same agent tool plays every role; the point is separate *sessions* with separate focus, especially REVIEW and VERIFY (fresh eyes).

## 4. Definition of Done (BUILD/UI)
- [ ] All acceptance criteria in the ticket met and ticked.
- [ ] `./gradlew :domain:test` and `assembleDebug` pass; relevant extra suites pass; lint has no new warnings.
- [ ] Tests for new logic exist (and were written first for `:domain`).
- [ ] No scope creep: diff contains only what the ticket needs.
- [ ] No new dependency without owner approval; no schema change without approval.
- [ ] Strings in `strings.xml`; previews for new composables; works in dark mode.
- [ ] Handoff log filled; ticket moved to `review/`; commit made.

## 5. Review checklist (REVIEW tickets)
1. Does the diff do exactly what the ticket says, no more?
2. Do tests assert the TC expectations (unchanged)? Any test edited to pass?
3. Business logic in `:domain`? UI stateless? Layers respected?
4. Offline-first, crash-safe, no main-thread I/O?
5. Logging flow: any added taps? Any hardcoded strings/colors?
6. Naming, readability, dead code, TODOs without ticket ids.
7. Findings list: **Blocker / Should-fix / Nit**. Blockers go back to `todo/` as a fix ticket; otherwise move to `verify/`.

## 6. Verify checklist (VERIFY tickets)
Install the debug build on the real phone (or emulator if stated). Run the listed `AC-*` steps. Record: build id/commit, device + Android version, result per step, screenshots/notes. Failures become tickets.

## 7. When to stop and ask the owner
Open decision needed; ambiguous requirement; test seems wrong; dependency/schema/API change needed; ticket larger than one session; discovered conflict between SPEC and research.

## 8. Git conventions
Branch `t-123-short-slug`; commits `T-123: imperative summary`; keep commits small; never commit secrets or keystores.

## 9. Copy-paste session prompts

**Start (BUILD or UI)**
> Follow AGENTS.md. Continue the ticket in `tickets/doing/`, or take the next eligible ticket from `tickets/todo/`. Read only what it references. Post a short plan, then implement. Do exactly what the acceptance criteria say and nothing more. For `:domain` work, write the tests first from the referenced TC ids. Finish with checks, handoff log, ticket move, and commit.

**REVIEW**
> Follow AGENTS.md and docs/WORKFLOW.md section 5. Take the lowest-numbered ticket in `tickets/review/`. Review the diff (`git diff main...HEAD` or the ticket's branch) against the ticket, SPEC, ARCHITECTURE. Run the checks. Do not modify code. Write findings in the ticket's Handoff log (Blocker / Should-fix / Nit), then move it to `verify/` or create fix tickets in `todo/`.

**VERIFY**
> Follow AGENTS.md and docs/WORKFLOW.md section 6. Take the lowest-numbered ticket in `tickets/verify/`. Build the debug APK, run the verification steps/AC scenarios, record evidence in the Handoff log, and move the ticket to `done/` only if everything passes; otherwise create bug tickets.

**SPIKE**
> Follow AGENTS.md. Take the next SPIKE ticket. Investigate within its question and time box. Do not write production code. Output: options, measurements/evidence, recommendation, and the proposed `docs/DECISIONS.md` change. Ask me to confirm.

**DOCS**
> Follow AGENTS.md. I decided: <decision>. Update `docs/DECISIONS.md`, and any SPEC/ARCHITECTURE/TEST-CASES/tickets affected. Change nothing else.

**New ticket from an idea**
> Create a ticket in `tickets/todo/` from `tickets/TEMPLATE.md` for: <idea>. Keep it to one session, with checkable acceptance criteria and referenced ids. Do not implement it.
