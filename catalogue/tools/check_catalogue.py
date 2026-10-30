#!/usr/bin/env python3
"""Checks catalogue/catalogue.json against docs/CATALOGUE.md section 5, rules 1 and 3-7.

Rule 2 (JSON Schema) runs in CI through check-jsonschema; rule 8 is the PR template
checkbox. Standard library only, so contributors can run it anywhere:

    python catalogue/tools/check_catalogue.py                    # no base: every id counts as new
    python catalogue/tools/check_catalogue.py --base-ref origin/main
    python catalogue/tools/check_catalogue.py --base-file live.json

Exit status: 0 all checks pass, 1 a rule failed, 2 the checker could not run.
"""

import argparse
import json
import os
import re
import subprocess
import sys
import unicodedata
from pathlib import Path

CATALOGUE_DIR = Path(__file__).resolve().parent.parent
DEFAULT_FILE = CATALOGUE_DIR / "catalogue.json"
DEFAULT_SCHEMA = CATALOGUE_DIR / "catalogue.schema.json"

BOM = b"\xef\xbb\xbf"
UUID_SHAPE = re.compile(r"^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$", re.IGNORECASE)
APOSTROPHES = "'\u2019"
NON_ALPHANUMERIC_RUN = re.compile(r"[^a-z0-9]+")


class _InvalidJson(ValueError):
    pass


def slug(title):
    """CATALOGUE section 4: "Dumbbell Farmer's Walk" becomes "dumbbell-farmers-walk"."""
    text = _strip_diacritics(title).lower()
    for apostrophe in APOSTROPHES:
        text = text.replace(apostrophe, "")
    return NON_ALPHANUMERIC_RUN.sub("-", text).strip("-")


def title_key(title):
    """Case- and diacritic-insensitive form used for title uniqueness (rule 3)."""
    return _strip_diacritics(title).casefold()


def _strip_diacritics(text):
    return "".join(c for c in unicodedata.normalize("NFKD", text) if not unicodedata.combining(c))


def parse(data):
    """Rule 1. Returns (document, errors); the document is None when parsing failed."""
    if data.startswith(BOM):
        return None, ["check 1: file starts with a UTF-8 byte order mark"]
    try:
        text = data.decode("utf-8")
    except UnicodeDecodeError as e:
        return None, [f"check 1: not valid UTF-8 ({e})"]
    try:
        return json.loads(text, object_pairs_hook=_no_duplicate_keys, parse_constant=_no_constants), []
    except _InvalidJson as e:
        return None, [f"check 1: {e}"]
    except json.JSONDecodeError as e:
        return None, [f"check 1: not valid JSON ({e})"]


def _no_duplicate_keys(pairs):
    seen = {}
    for key, value in pairs:
        if key in seen:
            raise _InvalidJson(f"duplicate key {key!r}")
        seen[key] = value
    return seen


def _no_constants(name):
    raise _InvalidJson(f"{name} is not valid JSON")


def canonical_bytes(doc, schema):
    """CATALOGUE section 1 formatting, with keys in schema order and unknown keys last."""
    top_order = list(schema["properties"])
    exercise_order = list(schema["$defs"]["exercise"]["properties"])
    ordered = _reorder(doc, top_order)
    ordered["exercises"] = [_reorder(e, exercise_order) for e in doc["exercises"]]
    return (json.dumps(ordered, indent=2, ensure_ascii=False) + "\n").encode("utf-8")


def _reorder(obj, order):
    keys = [k for k in order if k in obj] + [k for k in obj if k not in order]
    return {k: obj[k] for k in keys}


def _shape_errors(doc):
    """Enough structure for rules 3-7 to run without crashing; the schema check reports the details."""
    if not isinstance(doc, dict):
        return [f"structure: top level is not an object (see the schema check)"]
    errors = []
    exercises = doc.get("exercises")
    if not isinstance(exercises, list) or not all(isinstance(e, dict) for e in exercises):
        errors.append(f"structure: 'exercises' is not an array of objects (see the schema check)")
    else:
        for i, e in enumerate(exercises):
            if not isinstance(e.get("id"), str) or not isinstance(e.get("title"), str):
                errors.append(f"structure: exercises[{i}] needs string 'id' and 'title' (see the schema check)")
            if not isinstance(e.get("secondaryMuscles"), list):
                errors.append(f"structure: exercises[{i}] needs array 'secondaryMuscles' (see the schema check)")
    retired = doc.get("retiredIds")
    if not isinstance(retired, list) or not all(isinstance(r, str) for r in retired):
        errors.append(f"structure: 'retiredIds' is not an array of strings (see the schema check)")
    for field in ("version", "formatVersion"):
        if not isinstance(doc.get(field), int) or isinstance(doc.get(field), bool):
            errors.append(f"structure: '{field}' is not an integer (see the schema check)")
    return errors


def check(head, schema, base=None):
    """All rule failures of the catalogue bytes `head`, compared with `base` bytes when given."""
    doc, errors = parse(head)
    if doc is None:
        return errors
    errors = _shape_errors(doc)
    if errors:
        return errors

    exercises = doc["exercises"]
    ids = [e["id"] for e in exercises]
    retired = doc["retiredIds"]

    seen_ids = set()
    for id_ in ids + retired:
        if id_ in seen_ids:
            errors.append(f"check 3: id {id_!r} appears more than once across exercises and retiredIds")
        seen_ids.add(id_)
    seen_titles = {}
    for e in exercises:
        key = title_key(e["title"])
        if key in seen_titles:
            errors.append(f"check 3: title {e['title']!r} duplicates {seen_titles[key]!r} ignoring case and diacritics")
        else:
            seen_titles[key] = e["title"]

    for e in exercises:
        if e.get("primaryMuscle") in e["secondaryMuscles"]:
            errors.append(f"check 4: {e['id']!r} lists its primaryMuscle in secondaryMuscles")

    if ids != sorted(ids):
        errors.append("check 5: exercises are not sorted by id")
    if head != canonical_bytes(doc, schema):
        errors.append(
            "check 5: file is not in canonical formatting; rewrite it with "
            "json.dumps(doc, indent=2, ensure_ascii=False) + '\\n' (UTF-8, LF, keys in schema order)"
        )

    for id_ in ids + retired:
        if UUID_SHAPE.match(id_):
            errors.append(f"check 7: id {id_!r} is shaped like a UUID")

    base_doc = None
    if base is not None:
        base_doc, base_errors = parse(base)
        if base_doc is not None:
            base_errors = _shape_errors(base_doc)
        if base_errors:
            return errors + [f"base: {m}" for m in base_errors]

    if base_doc is not None and head != base:
        if doc["version"] <= base_doc["version"]:
            errors.append(f"check 6: version {doc['version']} must be greater than the base's {base_doc['version']}")
    if base_doc is not None and doc["formatVersion"] != base_doc["formatVersion"]:
        errors.append(
            f"check 6: formatVersion changed from {base_doc['formatVersion']} to {doc['formatVersion']}; "
            "a format bump is a separate owner-approved change"
        )

    base_ids = {e["id"] for e in base_doc["exercises"]} if base_doc is not None else set()
    base_retired = set(base_doc["retiredIds"]) if base_doc is not None else set()
    for id_ in sorted(base_ids - set(ids) - set(retired)):
        errors.append(f"check 7: id {id_!r} was removed without moving it to retiredIds")
    for id_ in sorted(base_retired - set(retired)):
        errors.append(f"check 7: id {id_!r} left retiredIds")
    for e in exercises:
        if e["id"] not in base_ids and e["id"] != slug(e["title"]):
            errors.append(f"check 7: new id {e['id']!r} must equal slug(title) {slug(e['title'])!r}")

    return errors


def _git_base(ref, file):
    """Bytes of `file` at `ref`, or None when the file does not exist there."""
    cwd = file.parent
    git_path = f"{ref}:./{file.name}"
    if subprocess.run(["git", "rev-parse", "--verify", "--quiet", f"{ref}^{{commit}}"],
                      cwd=cwd, capture_output=True).returncode != 0:
        raise SystemExit(_fail(f"base ref {ref!r} is not a commit in this repository"))
    if subprocess.run(["git", "cat-file", "-e", git_path], cwd=cwd, capture_output=True).returncode != 0:
        return None
    shown = subprocess.run(["git", "show", git_path], cwd=cwd, capture_output=True)
    if shown.returncode != 0:
        raise SystemExit(_fail(f"git show {git_path} failed: {shown.stderr.decode(errors='replace').strip()}"))
    return shown.stdout


def _fail(message):
    print(f"error: {message}", file=sys.stderr)
    return 2


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--file", type=Path, default=DEFAULT_FILE, help="catalogue to check")
    parser.add_argument("--schema", type=Path, default=DEFAULT_SCHEMA, help="schema giving the key order")
    base_group = parser.add_mutually_exclusive_group()
    base_group.add_argument("--base-ref", help="git commit whose copy of the file is the base (rules 6, 7)")
    base_group.add_argument("--base-file", type=Path, help="file that is the base, e.g. the live catalogue")
    args = parser.parse_args(argv)

    file = args.file.resolve()
    try:
        head = file.read_bytes()
        schema = json.loads(args.schema.read_text(encoding="utf-8"))
        base = args.base_file.read_bytes() if args.base_file else None
    except OSError as e:
        return _fail(str(e))
    if args.base_ref:
        base = _git_base(args.base_ref, file)
        if base is None:
            print(f"note: {file.name} does not exist at {args.base_ref}; every id counts as new")

    errors = check(head, schema, base)
    annotate = os.environ.get("GITHUB_ACTIONS") == "true"
    for error in errors:
        print(f"::error title=Catalogue check::{error}" if annotate else error)
    if errors:
        print(f"FAILED: {len(errors)} problem(s) in {file.name}")
        return 1
    print(f"OK: {file.name} passes checks 1 and 3-7" + ("" if base is not None else " (no base)"))
    return 0


if __name__ == "__main__":
    sys.exit(main())
