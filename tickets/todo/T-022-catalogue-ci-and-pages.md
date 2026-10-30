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
- [ ] Each §5 rule has a failing example that the checker rejects and the sample passes.
- [ ] A PR that does not bump `version` fails; one that removes an id without moving it to `retiredIds` fails.
- [ ] After merge, `https://t0liver.github.io/muskli/catalogue/v1/catalogue.json` serves the file byte-identical to `main`.

## Verification
Run the checker locally on the sample and on broken copies; open a test PR; `curl` the Pages URL and compare its SHA-256 with `git show main:catalogue/catalogue.json`.

## Notes / decisions needed
Owner: make the repo public (D-025), set Pages source to "GitHub Actions", and require branches to be up to date before merging. Owner, 2026-10-30: done. Pushing is not possible from this PC, so the test PR and the Pages `curl` check are the owner's after the push; everything runnable locally must be run.

## Handoff log
<!-- Newest first. Each entry: date, role, what was done, what's left, gotchas, commands run + results. -->
- 2026-10-29 SPIKE (T-007): Created after the owner confirmed D-025.
