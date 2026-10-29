# Starter Kit: how to bootstrap

This folder is the skeleton of your new repo (delete this file from the repo after setup).

```
AGENTS.md  CLAUDE.md  SPEC.md
docs/        ARCHITECTURE.md  DECISIONS.md  TEST-CASES.md  WORKFLOW.md
             research/      (the 13-file Hevy research pack)
             reference-ui/  (YOU add screenshots; checklist in research/11)
             fixtures/      (YOU add your Hevy export; keep private data out of git)
tickets/     todo/ doing/ review/ verify/ done/   (T-001..T-020 are in todo/)
```

## Setup (10 minutes)
1. Create the repo folder, copy this kit's contents into it, `git init`, first commit.
2. Add `docs/fixtures/` to `.gitignore` if your workout data is private.
3. Take screenshots/recordings from your Hevy app into `docs/reference-ui/` (checklist: `docs/research/11-visual-capture-checklist.md`). Needed before **T-008**.
4. Export your Hevy data into `docs/fixtures/`. Needed before **T-020**.

## Before the first agent session: decisions only you can make
Open in `docs/DECISIONS.md`, needed by **T-001**:
- **D-006** Hilt or manual DI.
- **D-007** minSdk (your phone's Android version / oldest you care about).
- **D-008** App name and package id (not "Hevy").
- **D-016** JUnit4 or JUnit5 for `:domain` (fine to say "you pick").

Confirm or change the *proposed* defaults too (D-002, D-003, D-004, D-009, D-010, D-011), especially D-003 (1RM formula). If you want parity with Hevy, check what its summary shows for a known set (research file 10, A2).

## Running sessions
1. Open your agent tool (Claude Code, Cursor, etc.) in the repo root.
2. Paste the **Start** prompt from `docs/WORKFLOW.md` section 9. One ticket per session.
3. When a BUILD/UI ticket lands in `review/`, run a **fresh** session with the REVIEW prompt. Then VERIFY on your phone.
4. Your jobs in the loop: approve schema/dependency/API changes, answer open decisions, look at UI previews, and run the device tests when asked.

## Suggested order of play
T-001 -> T-002..T-005 (pure domain, quick wins) -> T-006 (approve the schema) -> T-007 and T-009 spikes can run any time after T-001 -> T-008 (UI, needs screenshots) -> T-010..T-018 -> T-019 verify. Review sessions after each BUILD/UI ticket.

## Adding BMad later (optional)
Install it into this repo when you want heavier process. Its brownfield flow can take `SPEC.md`, `docs/ARCHITECTURE.md`, and the tickets as inputs. Until then, this lightweight loop is enough.

## Know what's proposed vs. decided
Everything in `docs/ARCHITECTURE.md` and `docs/DECISIONS.md` marked *proposed* is a starting suggestion. Test numbers in `docs/TEST-CASES.md` were hand-checked. The canonical copies live under `docs/` only.
