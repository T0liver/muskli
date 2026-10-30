# T-022 — Catalogue CI checks and Pages publish

- **Role:** BUILD
- **Phase:** 1
- **Depends on:** T-007
- **Refs:** D-005, D-025 · docs/CATALOGUE.md §1, §5–§8
- **Size:** fits one session

## Goal
PRs that touch `catalogue/**` are validated by CI, and a push to `main` publishes the catalogue to GitHub Pages at the D-025 URL.

## In scope
- `catalogue/tools/check_catalogue.py` (stdlib Python) implementing CATALOGUE §5 checks 1 and 3–7 against `git show <base>:catalogue/catalogue.json`.
- PR workflow on PRs into `catalogue` and `main` touching `catalogue/**`: the stdlib checker plus a pinned, CI-only `check-jsonschema` (D-025). Pin a version verified at the time.
- Pages workflow on push to `main` touching `catalogue/**`: re-run the checks, require `version` greater than the live file (404 is OK on the first deploy), build `_site/catalogue/v1/{catalogue.json,catalogue.schema.json}`, deploy with `actions/upload-pages-artifact` and `actions/deploy-pages`.
- PR template with the CATALOGUE §8 license checkbox.
- `.gitattributes`: `catalogue/** text eol=lf`.

## Out of scope (do NOT do)
- App loader or sync (T-010, T-021). New catalogue content beyond the T-007 sample. Changing the format (D-025).

## Acceptance criteria
- [x] Each §5 rule has a failing example that the checker rejects and the sample passes. (Rules 1, 3–7: `test_check_catalogue.py`; rule 2: check-jsonschema 0.39.0 on five broken copies; rule 8 is the human PR-template checkbox.)
- [x] A PR that does not bump `version` fails; one that removes an id without moving it to `retiredIds` fails. (Verified with the exact checker command the PR workflow runs, against a git base ref; a real test PR is in the owner checklist below.)
- [ ] After merge, `https://t0liver.github.io/muskli/catalogue/v1/catalogue.json` serves the file byte-identical to `main`. **Owner, after push** (cannot push from this PC).

## Verification
Run the checker locally on the sample and on broken copies; open a test PR; `curl` the Pages URL and compare its SHA-256 with `git show main:catalogue/catalogue.json`.

## Notes / decisions needed
Owner: make the repo public (D-025), set Pages source to "GitHub Actions", and require branches to be up to date before merging. Owner, 2026-10-30: done. Pushing is not possible from this PC, so the test PR and the Pages `curl` check are the owner's after the push; everything runnable locally must be run.

## Handoff log
<!-- Newest first. Each entry: date, role, what was done, what's left, gotchas, commands run + results. -->
- 2026-10-30 VERIFY: Commit `7a4ebda` on `t-022-catalogue-ci-and-pages` (before the ticket move). No device needed: no Android code changed.
  - **Passed locally:**
    - `python -m unittest discover -s catalogue/tools -p "test_*.py"` → 40 tests OK; also OK with `catalogue.json` temporarily at `version` 2 and a changed title (restored afterwards).
    - Checker as the PR workflow calls it: `--base-ref main` → OK, exit 0. As the Pages workflow calls it, simulating `git cat-file -e "<before>^{commit}"` in PowerShell (no bash here): all-zeros `before` → "no base" path, OK, exit 0; `before` = `origin/main` (`b2fa88a`, no catalogue yet) → "does not exist at …; every id counts as new", OK, exit 0; `before` = `main` → OK, exit 0.
    - check-jsonschema 0.39.0 (temp venv in `%TEMP%`) on `catalogue.json` and the test fixture → `ok -- validation done`, exit 0.
    - actionlint 1.7.12 (temp download) on both workflows → exit 0, no findings.
    - Pins re-checked against the GitHub releases API and PyPI: checkout v7.0.1, setup-python v7.1.0, upload-pages-artifact v5.0.0, deploy-pages v5.0.1 (node24; needs only `pages: write` + `id-token: write`), check-jsonschema 0.39.0. All are the latest releases.
    - Live URL with the workflow's curl flags → `status=404 curl_exit=0`, which is the first-deploy branch.
    - `.\gradlew.bat :domain:test --rerun` and `assembleDebug` → BUILD SUCCESSFUL. APK SHA-256 `1A40DDA6…54A254E4`.
    - `git ls-files --eol catalogue` → `catalogue.json`, schema and fixture are `i/lf w/lf attr/text eol=lf`.
  - **Left for the owner, after pushing `main`** (not possible from this PC). Move the ticket to `done/` once all of these pass:
    1. Push `main`. Then confirm "Publish catalogue" ran on that push, logged "does not exist at <before>" and "first deploy (HTTP 404)", and deployed to the `github-pages` environment.
    2. Compare SHA-256 values after the CDN refresh (about 10 minutes). The live file must match `3f6bdb215093b5ddf569b6c4864edbefbb75723fc580fee3e15a0750463c18de`, which is `git show main:catalogue/catalogue.json`. PowerShell: `curl.exe -s -o live.json https://t0liver.github.io/muskli/catalogue/v1/catalogue.json; Get-FileHash live.json`. Also open `.../catalogue/v1/catalogue.schema.json`. Then tick the last acceptance criterion.
    3. Open a throwaway PR into `main` that changes one title in `catalogue/catalogue.json` without bumping `version`. "Catalogue checks" must fail with `check 6:`. Next, bump `version` and also delete one exercise without listing it in `retiredIds`; it must fail with `check 7:`. Close the PR unmerged and delete its branch.
    4. Optional: create the `catalogue` branch from `main` so the §6 flow (PRs into `catalogue`) has somewhere to land.
- 2026-10-30 REVIEW: Diff `main...t-022-catalogue-ci-and-pages` checked against the ticket, CATALOGUE §1 and §5–§8, D-025, D-046, and AGENTS. Scope matches the ticket. The rest is in order: no app code changes, no new app dependency, the PR-template checkbox is verbatim from §8, and the `.gitattributes` line is correct.
  - **Blocker (fixed, `50c2204`):** the unit tests built their broken copies from the live `catalogue/catalogue.json` (the hard-coded `"version": 1,` replacement, `plank`, `quadriceps` on `exercises[0]`, the `dumbbell-curl` additions). CI runs these tests on every catalogue PR, so the first legitimate content PR would go red. Reproduced it: with `version` 2 the checker passed, but `test_duplicate_key` and `test_nan_constant` failed. Fix: the tests now read a frozen byte copy, `catalogue/tools/testdata/sample-catalogue.json` (same SHA-256 as today's sample). The real file is still checked by the checker and check-jsonschema steps. No expected values changed. Re-ran with `version` 2 plus a changed title: 40 tests OK.
  - **Should-fix (fixed, `7a4ebda`):** CATALOGUE §6.3 said the publish job fails whenever `version` is not greater than the live one. D-046 (and the code) allow a byte-identical file to redeploy. I added that exception to §6.3 so the spec matches the implementation.
  - **Workflows, checked and correct:**
    - Triggers: `pull_request` into `catalogue`/`main` and `push` to `main`, both filtered on `catalogue/**`.
    - Permissions: `contents: read` for the checks and the build job; the deploy job has only `pages: write` + `id-token: write` and uses environment `github-pages` with the `page_url` output (which ends in `/`).
    - Concurrency: `pages`, without cancel-in-progress, so a queued run fetches the live file only after the previous deploy has finished.
    - `fetch-depth: 0` in both, so `base.sha` and `before` are in the clone.
    - Base handling: an all-zeros or unreachable `before` (new branch, force-push) and a `before` without the catalogue both fall back to "no base". The live-file check still enforces rules 6–7 in those cases, and on the first push it gets a 404 and deploys.
    - Error handling: a curl failure exits non-zero under `bash -e`; any status other than 200/404 fails the job.
  - **Nits (not fixed):**
    - (a) The no-base fallback requires every id to equal `slug(title)`. After a future title correction, a force-push to `main` would therefore fail the "previous main" step until someone pushes normally. This is rare, and the live check covers the case.
    - (b) There is no `workflow_dispatch`, and edits to the workflow files themselves do not trigger a run. To redeploy, push a change under `catalogue/**`.
    - (c) The PR check uses `pull_request.base.sha`. `HEAD^1` of the merge ref would always match the tested merge, but "require branches to be up to date" makes the difference moot.
    - (d) Three `f"..."` strings in `_shape_errors` have no placeholders.
    - (e) As BUILD noted, path filters plus a required status check would block non-catalogue PRs. Don't make "Catalogue checks" a required check, or add a no-op fallback job.
  - **Commands:** listed in the VERIFY entry above.
- 2026-10-30 BUILD: Branch `t-022-catalogue-ci-and-pages`.
  - **Done:** `catalogue/tools/check_catalogue.py` (stdlib; rules 1 and 3–7; base via `--base-ref <commit>` using `git show`, or `--base-file <path>` for the live file; exit 0 pass / 1 rule failed / 2 could not run; `::error` annotations on Actions). `catalogue/tools/test_check_catalogue.py` (40 unittest cases: broken copies per rule plus CLI tests against a throwaway git repo). `.github/workflows/catalogue-checks.yml` (PRs into `catalogue`/`main` touching `catalogue/**`: unit tests, check-jsonschema, checker vs `pull_request.base.sha`). `.github/workflows/catalogue-pages.yml` (push to `main` touching `catalogue/**`: same checks vs `github.event.before`, then vs the live file, 404 = first deploy, other statuses fail; builds `_site/catalogue/v1/{catalogue.json,catalogue.schema.json}`; `upload-pages-artifact` + `deploy-pages`). `.github/pull_request_template.md` (§8 checkbox, verbatim). `.gitattributes` (`catalogue/** text eol=lf`). D-046 records the auto-accepted choices.
  - **Gotchas:** With `core.autocrlf=true` this PC's working copy of `catalogue.json` had 107 CRs before `.gitattributes`; the index was already LF. After re-checkout it is LF, and the APK asset SHA-256 equals the blob (`3f6bdb215093b5ddf569b6c4864edbefbb75723fc580fee3e15a0750463c18de`). Other existing Windows clones keep their CRLF working copy until the files are rewritten: delete `catalogue/catalogue.json` and `catalogue/catalogue.schema.json`, then `git checkout -- catalogue` (a fresh clone is LF already). `origin/main` has no `catalogue/catalogue.json` yet, so the first Pages run checks without a base (every id new; the sample passes). `paths` filters mean the checks workflow does not run on PRs that miss `catalogue/**`; if it becomes a required status check, such PRs will wait on it.
  - **Commands and results:**
    - `python --version` → 3.12.10; `py --version` → 3.14.7.
    - `python -m unittest discover -s catalogue/tools -p "test_*.py" -v` → 40 tests OK (also OK with `py -3.14`).
    - `python catalogue/tools/check_catalogue.py` → `OK ... (no base)`, exit 0; `--base-ref main` → OK, exit 0; `--base-ref origin/main` → note "does not exist at origin/main", OK, exit 0.
    - check-jsonschema 0.39.0 in a temp venv (outside the repo): `check-jsonschema --schemafile catalogue/catalogue.schema.json catalogue/catalogue.json` → `ok -- validation done`; broken copies (bad equipment enum, unknown field `notes`, license `CC-BY-SA-4.0`, id `Plank`, `version` 0) → each exit 1 with the expected message.
    - Versions checked online 2026-10-30 via the GitHub releases API and PyPI: checkout v7.0.1, setup-python v7.1.0, upload-pages-artifact v5.0.0, deploy-pages v5.0.1, check-jsonschema 0.39.0 (requires Python ≥ 3.11). Release notes read; checkout v7's breaking change only affects `pull_request_target`.
    - actionlint 1.7.12 (temp download, not added to the repo) on both workflows → no findings, exit 0. shellcheck is not installed, so the `run:` scripts were re-read by hand.
    - `curl.exe ... https://t0liver.github.io/muskli/catalogue/v1/catalogue.json?ci=local` (same flags as the workflow) → HTTP 404, curl exit 0, which is the first-deploy branch.
    - `.\gradlew.bat :domain:test assembleDebug` → BUILD SUCCESSFUL. `lintDebug` not run (no Android code changed).
  - **Not verified:** a real PR run and a real Pages deploy (cannot push from this PC).
  - **Owner after push:**
    1. Push `main` with this branch merged (or open a PR from this branch into `main`); confirm the "Catalogue checks" workflow runs and passes.
    2. Open a throwaway test PR into `main` that edits a title in `catalogue/catalogue.json` without bumping `version`; confirm "Catalogue checks" fails with `check 6:`. Then remove an exercise without adding it to `retiredIds` (bump `version`); confirm it fails with `check 7:`. Close the PR unmerged.
    3. After the merge to `main`, confirm "Publish catalogue" passes (the first run logs "first deploy") and the `github-pages` environment shows the deploy.
    4. Compare SHA-256 (allow about 10 minutes for the CDN): `curl -s https://t0liver.github.io/muskli/catalogue/v1/catalogue.json | sha256sum` vs `git show main:catalogue/catalogue.json | sha256sum` (PowerShell: `curl.exe -s -o live.json <url>; Get-FileHash live.json`, and `cmd /c "git show main:catalogue/catalogue.json > main.json"; Get-FileHash main.json`). They must match; then tick the last acceptance criterion.
    5. Optional: also check `.../catalogue/v1/catalogue.schema.json` loads.
- 2026-10-29 SPIKE (T-007): Created after the owner confirmed D-025.
