# CATALOGUE.md

Status: **decided (D-025, 2026-10-29).** Implements D-005 and D-021.

One file, `catalogue/catalogue.json`, is the canonical catalogue on `main`. The same bytes are bundled into the app (T-010) and served from GitHub Pages (T-021). Schema: `catalogue/catalogue.schema.json`.

## 1. Format: JSON
| | JSON | TOML |
|---|---|---|
| Android parsing | platform `org.json` or kotlinx.serialization (already proposed in ARCHITECTURE §1) | needs a new library |
| Served as an API | Pages serves `application/json` directly | needs a JSON build step, so bundled and published files would differ |
| Validation | JSON Schema, editor autocompletion via `$schema` | no standard schema |
| Hand editing | fine with the schema in the editor | a little nicer |

JSON wins: no new app dependency, and one file can serve both uses unchanged, which D-021 requires. The only thing TOML does better is hand editing, and that matters little for six short fields per exercise.

**Canonical formatting:** exactly `json.dumps(doc, indent=2, ensure_ascii=False) + "\n"` (Python), encoded UTF-8 without BOM, LF line endings, keys in schema order, `exercises` sorted by `id`. Contributors can reformat with that one call, and diffs stay one block per exercise. The committed blob must be LF. With `core.autocrlf=true` on Windows the working copy turns CRLF, so a `.gitattributes` line `catalogue/** text eol=lf` keeps the bundled asset byte-identical to the published file on every machine.

## 2. Schema (format 1)
Top level:

| Field | Type | Rule |
|---|---|---|
| `$schema` | string | Published schema URL (editor support) |
| `formatVersion` | int | `1`. Breaking-change counter; equals the `/vN/` URL segment |
| `version` | int ≥ 1 | Content version, strictly increasing (section 3) |
| `license` | string | `CC0-1.0` (section 8) |
| `exercises` | array | Exercise objects, sorted by `id` |
| `retiredIds` | string[] | Ids removed from `exercises`; never reused (section 4) |

Exercise (fields required unless marked optional, no others allowed in format 1):

| Field | Type | Rule |
|---|---|---|
| `id` | string | Stable upsert key (section 4) |
| `title` | string | English display name, 2–80 chars, no leading/trailing space; unique (case- and diacritic-insensitive) |
| `type` | enum | 8 values: `weight_reps`, `reps_only`, `bodyweight_reps`, `bodyweight_assisted_reps`, `duration`, `weight_duration`, `distance_duration`, `short_distance_weight` |
| `primaryMuscle` | enum | 20 MuscleGroup values (ARCHITECTURE §3) |
| `secondaryMuscles` | enum[] | Unique, may be empty, must not contain `primaryMuscle` (D-013 counts each +0.5) |
| `equipment` | enum | 9 Equipment values (ARCHITECTURE §3) |
| `unilateral` | bool, optional | Trained one side at a time, so volume counts a `both` set twice (D-014, D-037). Absent means `false`; write it only when `true` (D-047) |

There are no fields for descriptions, instructions, or media, which keeps license risk and scope down. The fields map one-to-one onto `exercise_template` (T-006), with `isCustom = false`.

`unilateral` was added within format 1 (T-023) under the compatibility rule below: it is optional, and older builds ignore it and treat every exercise as bilateral.

**Compatibility within format 1:** CI rejects unknown fields so contributions stay clean. The app parser must **ignore** unknown fields, so a later optional field can ship without breaking older builds. Anything older builds cannot safely ignore needs `formatVersion: 2` and a new `/v2/` path. That covers a new enum value, a removed or renamed field, or a field that becomes required.

## 3. Version field (monotonic)
- `version` is a plain integer, starting at `1`. Every change to `catalogue.json` raises it; +1 is the convention, CI only requires strictly greater.
- CI compares it against the PR's base branch, and the publish job also compares it against what is currently live. A plain integer avoids clock skew and is trivial to compare.
- App rule (D-021): apply a bundle, bundled or downloaded, only when `bundle.version > storedVersion`. Store `version` with the catalogue in the same transaction. Ignore a bundle whose `formatVersion` the build does not know, treating it like a sync failure.
- Two PRs that both bump to N+1: the second fails CI after the first merges, because N+1 is not greater than N+1. Rebase and bump again. Enable "require branches to be up to date before merging".

## 4. Stable ids
- Lowercase kebab slug: `^[a-z][a-z0-9]*(-[a-z0-9]+)*$`, 2–64 chars. CI also rejects anything shaped like a UUID, so a published id can never collide with a custom exercise's UUID.
- A new id must equal `slug(title)` at creation. The slug is computed as: NFKD normalize, strip diacritics, lowercase, drop apostrophes, replace runs of non-alphanumerics with `-`, then trim `-`. Example: "Dumbbell Farmer's Walk" becomes `dumbbell-farmers-walk`.
- **An id never changes**, even when the title is later corrected; old workouts reference it.
- Removing an exercise means moving its id into `retiredIds`. An id may not vanish from both lists, and a retired id can never come back. The app does not need `retiredIds`: an id absent from `exercises` is archived or kept per D-021. The list exists only so CI can stop an id being reused.

## 5. CI checks (fail the PR)
They run on every PR that touches `catalogue/**`, into `catalogue` or `main`:
1. `catalogue.json` is valid UTF-8 JSON, without a BOM or duplicate keys.
2. It validates against `catalogue.schema.json` (draft 2020-12).
3. Ids are unique across `exercises` and `retiredIds` combined. Titles are unique case- and diacritic-insensitively.
4. `secondaryMuscles` does not contain `primaryMuscle`.
5. `exercises` is sorted by `id`, and the file is byte-identical to its canonical formatting (section 1).
6. `version` is greater than the base branch's `version` if the file changed. `formatVersion` is unchanged, since a format bump is a separate owner-approved change.
7. Every id in the base's `exercises` is still in `exercises` or newly in `retiredIds`. No id leaves `retiredIds`. New ids match `slug(title)`. No id is UUID-shaped.
8. `license` is `CC0-1.0`, and the PR template checkbox for section 8 is ticked (a human check, not automated).

The checker is a small script in the repo, `catalogue/tools/check_catalogue.py` (T-022). Checks 1 and 3–7 need only the Python standard library plus `git show <base>:catalogue/catalogue.json`. Check 2 uses a pinned, CI-only `check-jsonschema` (approved in D-025; never an app dependency).

## 6. Branch, merge, and publish flow
1. Add or edit exercises on a branch, then open a PR into **`catalogue`**. CI runs section 5.
2. To release, open a PR from `catalogue` into **`main`**. CI runs section 5 again against `main`.
3. A push to `main` that touches `catalogue/**` triggers the **Pages workflow** (GitHub Actions source, not a `gh-pages` branch). It re-runs the checks, fetches the live `catalogue.json`, and fails if the new `version` is not greater (a 404 on the first deploy is fine; a byte-identical file may redeploy, D-046). It then copies the two files into `_site/catalogue/v1/` and deploys with `actions/upload-pages-artifact` and `actions/deploy-pages`.
4. The app build bundles `catalogue/catalogue.json` from the same commit, so bundled version ≤ published version. T-010 wires this as a Gradle copy or an assets source directory, with no hand-maintained second copy.

## 7. Published URL layout
Base: `https://t0liver.github.io/muskli/` (repo `T0liver/muskli`).

| URL | Content |
|---|---|
| `catalogue/v1/catalogue.json` | Full bundle, format 1. **T-021 GETs this.** |
| `catalogue/v1/catalogue.schema.json` | Schema, so `$schema` resolves in editors |

- A future format 2 is published under `catalogue/v2/`. The workflow keeps writing a frozen `v1` file so older builds keep syncing. Each Pages deploy replaces the whole site.
- Pages sends an `ETag` and `Cache-Control: max-age=600`, so a fresh publish can take about 10 minutes to show up. T-021 may send `If-None-Match` (a 304 means nothing to do); that is optional.
- There is no separate `version.json` in the MVP. The full bundle is small: the 10-exercise sample is 2.5 KB, so about 25 KB per 100 exercises before gzip, and delta-sync stays post-MVP.

## 8. License and attribution rules
- The whole catalogue is **CC0-1.0**. Contributors dedicate their entries to the public domain, which keeps the app and any future reuse free of attribution duties.
- Allowed sources: your own knowledge, or datasets that are CC0, Unlicense, or public domain, with the license checked at the time of use. Name the source in the PR description.
- Not allowed: anything from Hevy (titles, naming conventions such as "Name (Equipment)", lists, muscle mappings, text, media), and CC-BY-SA or other share-alike or non-commercial sources (for example parts of wger), because their terms are incompatible with CC0.
- Factual data only, meaning name, type, muscles, and equipment. Descriptive text and media are out of format 1.
- The PR template checkbox reads: "These entries are my own work or come from a CC0/public-domain source named above; nothing is copied from Hevy."

## 9. What T-010 and T-021 rely on
- `formatVersion` gates parsing; `version` gates applying (strictly newer).
- `id` is the upsert key. Fields map one-to-one onto `exercise_template`; a missing `unilateral` is stored as `false`.
- Absent ids are archived when unreferenced and never hard-deleted when referenced (D-021). Custom exercises are never touched.
- Sample: `catalogue/catalogue.json` (10 exercises, all 8 types), which T-010 copies into assets.
