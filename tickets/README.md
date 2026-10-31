# Tickets

Status = folder: `todo/ -> doing/ -> review/ -> verify/ -> done/`. Move with `git mv`.
Rules and prompts: `docs/WORKFLOW.md`. Use `TEMPLATE.md` for new tickets. WIP limit: 1 in `doing/`.

## Initial backlog (order = number; dependencies are listed in each ticket)
| ID | Role | Title |
|---|---|---|
| T-001 | BUILD | Project scaffold (modules, version catalog, test setup) |
| T-002 | BUILD | Domain: units, set volume, estimated 1RM |
| T-003 | BUILD | Domain: records (PR detection) |
| T-004 | BUILD | Domain: rest-timer rule |
| T-005 | BUILD | Domain: previous-values lookup |
| T-006 | BUILD | Domain models + Room schema, DAOs, migrations |
| T-007 | SPIKE | Catalogue file format and publish flow |
| T-008 | UI | Design tokens + live-workout screen (static, with previews) |
| T-009 | SPIKE | Rest timer reliability on Android |
| T-010 | BUILD | Exercise library: bundled catalogue + repository |
| T-011 | UI+BUILD | Exercise library screen (search, filter, custom) |
| T-012 | BUILD | Live workout: draft persistence + use cases |
| T-013 | UI+BUILD | Live workout screen: sets, set types, completion |
| T-014 | BUILD | Previous values wired into the live workout |
| T-015 | BUILD | Rest timer wired in (mechanism from T-009) |
| T-016 | BUILD | Finish flow + history list/detail/edit/delete |
| T-017 | BUILD | Settings (units, default rest, keep awake, ...) |
| T-018 | BUILD | Export/import JSON |
| T-019 | VERIFY | MVP acceptance on the owner's phone (AC-01..AC-06, AC-08) |
| T-020 | SPIKE | Hevy export format + importer plan |
| T-021 | BUILD | Catalogue full sync (GitHub Pages, after T-010) |
| T-022 | BUILD | Catalogue CI checks and Pages publish (D-025) |
| T-033 | BUILD | Routines: model, storage, start a workout from one |
| T-034 | UI | Routines UI: list, editor, start |
| T-035 | BUILD | Hevy import: CSV reader and parser in `:domain` |
| T-036 | BUILD | Hevy import: exercise matching and import plan in `:domain` |
| T-037 | BUILD | Hevy import: repository and use cases (preview, apply) |
| T-038 | UI | Hevy import screen: pick file, preview, map exercises, confirm |
| T-039 | UI | Hevy import: body weight from the measurements file |
| T-040 | VERIFY | Hevy import with the owner's real export (AC-07) |
| T-041 | UI | Enable the "Last time in this routine" setting |

REVIEW sessions pick up BUILD/UI tickets sitting in `review/` (findings go into that ticket's Handoff log; blockers become new tickets in `todo/`). VERIFY sessions pick up tickets in `verify/`.
