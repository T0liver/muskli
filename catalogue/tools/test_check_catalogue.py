"""Broken copies of the sample catalogue, one or more per docs/CATALOGUE.md section 5 rule.

Run: python -m unittest discover -s catalogue/tools -p "test_*.py" -v
"""

import copy
import json
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path

TOOLS = Path(__file__).resolve().parent
sys.path.insert(0, str(TOOLS))

import check_catalogue as cc  # noqa: E402

# A frozen copy of the T-007 sample: the tests rely on its ids, titles and version 1, which
# the real catalogue.json is free to change in content PRs.
SAMPLE_BYTES = (TOOLS / "testdata" / "sample-catalogue.json").read_bytes()
SCHEMA = json.loads(cc.DEFAULT_SCHEMA.read_text(encoding="utf-8"))
UUID_ID = "abcdef01-2345-4789-abcd-ef0123456789"


def sample():
    return json.loads(SAMPLE_BYTES.decode("utf-8"))


def dump(doc):
    return (json.dumps(doc, indent=2, ensure_ascii=False) + "\n").encode("utf-8")


def bumped(doc):
    doc["version"] += 1
    return doc


def exercise(id_, title):
    return {
        "id": id_,
        "title": title,
        "type": "weight_reps",
        "primaryMuscle": "biceps",
        "secondaryMuscles": ["forearms"],
        "equipment": "dumbbell",
    }


def with_exercise(doc, new):
    doc["exercises"] = sorted(doc["exercises"] + [new], key=lambda e: e["id"])
    return doc


class CheckerTest(unittest.TestCase):
    def assertPasses(self, head, base=None):
        self.assertEqual(cc.check(head, SCHEMA, base), [])

    def assertFailsRule(self, rule, head, base=None):
        errors = cc.check(head, SCHEMA, base)
        self.assertTrue(
            any(e.startswith(f"check {rule}:") for e in errors),
            f"expected a check {rule} failure, got {errors}",
        )


class SampleTest(CheckerTest):
    def test_sample_passes_without_base(self):
        self.assertPasses(SAMPLE_BYTES)

    def test_unchanged_sample_passes_against_itself_without_a_bump(self):
        self.assertPasses(SAMPLE_BYTES, base=SAMPLE_BYTES)

    def test_sample_is_byte_identical_to_its_canonical_form(self):
        self.assertEqual(cc.canonical_bytes(sample(), SCHEMA), SAMPLE_BYTES)

    def test_new_exercise_with_bump_passes(self):
        head = dump(bumped(with_exercise(sample(), exercise("dumbbell-curl", "Dumbbell Curl"))))
        self.assertPasses(head, base=SAMPLE_BYTES)


class Rule1ValidJsonTest(CheckerTest):
    def test_byte_order_mark(self):
        self.assertFailsRule(1, cc.BOM + SAMPLE_BYTES)

    def test_invalid_utf8(self):
        self.assertFailsRule(1, SAMPLE_BYTES.replace(b"Plank", b"Pl\xffnk", 1))

    def test_duplicate_key(self):
        self.assertFailsRule(1, SAMPLE_BYTES.replace(b'"version": 1,', b'"version": 1,\n  "version": 2,', 1))

    def test_invalid_json(self):
        self.assertFailsRule(1, SAMPLE_BYTES.replace(b'"retiredIds": []', b'"retiredIds": [],', 1))

    def test_nan_constant(self):
        self.assertFailsRule(1, SAMPLE_BYTES.replace(b'"version": 1,', b'"version": NaN,', 1))


class Rule3UniquenessTest(CheckerTest):
    def test_duplicate_exercise_id(self):
        doc = sample()
        doc["exercises"].insert(0, copy.deepcopy(doc["exercises"][0]))
        doc["exercises"][0]["title"] = "Barbell Back Squat High Bar"
        self.assertFailsRule(3, dump(doc))

    def test_id_in_both_exercises_and_retired_ids(self):
        doc = sample()
        doc["retiredIds"] = ["plank"]
        self.assertFailsRule(3, dump(doc))

    def test_duplicate_retired_id(self):
        doc = sample()
        doc["retiredIds"] = ["old-curl", "old-curl"]
        self.assertFailsRule(3, dump(doc))

    def test_title_duplicate_ignoring_case(self):
        doc = with_exercise(sample(), exercise("plank-2", "PLANK"))
        self.assertFailsRule(3, dump(doc))

    def test_title_duplicate_ignoring_diacritics(self):
        doc = with_exercise(sample(), exercise("plank-2", "Plánk"))
        self.assertFailsRule(3, dump(doc))


class Rule4SecondaryMusclesTest(CheckerTest):
    def test_secondary_contains_primary(self):
        doc = sample()
        doc["exercises"][0]["secondaryMuscles"].append("quadriceps")
        self.assertFailsRule(4, dump(doc))


class Rule5SortedAndCanonicalTest(CheckerTest):
    def test_not_sorted_by_id(self):
        doc = sample()
        doc["exercises"].reverse()
        self.assertFailsRule(5, dump(doc))

    def test_crlf_line_endings(self):
        self.assertFailsRule(5, SAMPLE_BYTES.replace(b"\n", b"\r\n"))

    def test_missing_final_newline(self):
        self.assertFailsRule(5, SAMPLE_BYTES.rstrip(b"\n"))

    def test_four_space_indent(self):
        self.assertFailsRule(5, (json.dumps(sample(), indent=4, ensure_ascii=False) + "\n").encode("utf-8"))

    def test_escaped_non_ascii(self):
        doc = with_exercise(sample(), exercise("cafe-curl", "Café Curl"))
        self.assertPasses(dump(doc))
        self.assertFailsRule(5, (json.dumps(doc, indent=2, ensure_ascii=True) + "\n").encode("utf-8"))

    def test_keys_not_in_schema_order(self):
        doc = sample()
        first = doc["exercises"][0]
        doc["exercises"][0] = {"title": first["title"], **{k: v for k, v in first.items() if k != "title"}}
        self.assertFailsRule(5, dump(doc))

    def test_top_level_keys_not_in_schema_order(self):
        doc = sample()
        reordered = {"version": doc["version"], **{k: v for k, v in doc.items() if k != "version"}}
        self.assertFailsRule(5, dump(reordered))


class Rule6VersionTest(CheckerTest):
    def test_changed_file_without_version_bump(self):
        doc = sample()
        doc["exercises"][0]["title"] = "Barbell Back Squat (High Bar)"
        self.assertFailsRule(6, dump(doc), base=SAMPLE_BYTES)

    def test_changed_file_with_version_bump_passes(self):
        doc = bumped(sample())
        doc["exercises"][0]["title"] = "Barbell Back Squat (High Bar)"
        self.assertPasses(dump(doc), base=SAMPLE_BYTES)

    def test_lower_version(self):
        base = dump(bumped(bumped(sample())))
        self.assertFailsRule(6, dump(bumped(sample())), base=base)

    def test_format_version_changed(self):
        doc = bumped(sample())
        doc["formatVersion"] = 2
        self.assertFailsRule(6, dump(doc), base=SAMPLE_BYTES)


class Rule7IdStabilityTest(CheckerTest):
    def test_removed_id_not_moved_to_retired_ids(self):
        doc = bumped(sample())
        doc["exercises"] = [e for e in doc["exercises"] if e["id"] != "plank"]
        self.assertFailsRule(7, dump(doc), base=SAMPLE_BYTES)

    def test_removed_id_moved_to_retired_ids_passes(self):
        doc = bumped(sample())
        doc["exercises"] = [e for e in doc["exercises"] if e["id"] != "plank"]
        doc["retiredIds"] = ["plank"]
        self.assertPasses(dump(doc), base=SAMPLE_BYTES)

    def test_id_leaves_retired_ids(self):
        base = sample()
        base["retiredIds"] = ["old-curl"]
        self.assertFailsRule(7, dump(bumped(sample())), base=dump(base))

    def test_retired_id_cannot_come_back(self):
        base = sample()
        base["retiredIds"] = ["dumbbell-curl"]
        head = bumped(with_exercise(sample(), exercise("dumbbell-curl", "Dumbbell Curl")))
        head["retiredIds"] = ["dumbbell-curl"]
        self.assertFailsRule(3, dump(head), base=dump(base))
        head["retiredIds"] = []
        self.assertFailsRule(7, dump(head), base=dump(base))

    def test_new_id_not_equal_to_slug_of_title(self):
        doc = bumped(with_exercise(sample(), exercise("curl", "Dumbbell Curl")))
        self.assertFailsRule(7, dump(doc), base=SAMPLE_BYTES)

    def test_existing_id_may_keep_its_slug_after_title_change(self):
        doc = bumped(sample())
        doc["exercises"][0]["title"] = "Back Squat"
        self.assertPasses(dump(doc), base=SAMPLE_BYTES)

    def test_uuid_shaped_exercise_id(self):
        doc = bumped(with_exercise(sample(), exercise(UUID_ID, "Dumbbell Curl")))
        self.assertFailsRule(7, dump(doc), base=SAMPLE_BYTES)

    def test_uuid_shaped_retired_id(self):
        doc = bumped(sample())
        doc["retiredIds"] = [UUID_ID]
        self.assertFailsRule(7, dump(doc), base=SAMPLE_BYTES)


class SlugTest(unittest.TestCase):
    def test_examples(self):
        self.assertEqual(cc.slug("Dumbbell Farmer's Walk"), "dumbbell-farmers-walk")
        self.assertEqual(cc.slug("Dumbbell Farmer\u2019s Walk"), "dumbbell-farmers-walk")
        self.assertEqual(cc.slug("Machine Assisted Pull-Up"), "machine-assisted-pull-up")
        self.assertEqual(cc.slug("Café Curl"), "cafe-curl")
        self.assertEqual(cc.slug("  Squat -- (Pause) "), "squat-pause")


class CommandLineTest(unittest.TestCase):
    """The script as CI calls it, against a throwaway git repository."""

    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.repo = Path(self.tmp.name)
        self.file = self.repo / "catalogue.json"
        self.git("init", "-q")

    def tearDown(self):
        self.tmp.cleanup()

    def git(self, *args):
        subprocess.run(
            ["git", "-c", "core.autocrlf=false", "-c", "commit.gpgsign=false",
             "-c", "user.name=t", "-c", "user.email=t@example.invalid", *args],
            cwd=self.repo, check=True, capture_output=True,
        )

    def commit(self, data):
        self.file.write_bytes(data)
        self.git("add", "catalogue.json")
        self.git("commit", "-q", "-m", "c")

    def run_checker(self, *args):
        return subprocess.run(
            [sys.executable, str(TOOLS / "check_catalogue.py"), "--file", str(self.file), *args],
            capture_output=True, text=True,
        )

    def test_sample_against_head_passes(self):
        self.commit(SAMPLE_BYTES)
        result = self.run_checker("--base-ref", "HEAD")
        self.assertEqual(result.returncode, 0, result.stdout + result.stderr)

    def test_changed_without_bump_against_base_ref_fails(self):
        self.commit(SAMPLE_BYTES)
        doc = sample()
        doc["exercises"][0]["title"] = "Back Squat"
        self.file.write_bytes(dump(doc))
        result = self.run_checker("--base-ref", "HEAD")
        self.assertEqual(result.returncode, 1)
        self.assertIn("check 6:", result.stdout)

    def test_file_missing_at_base_ref_counts_every_id_as_new(self):
        (self.repo / "other.txt").write_text("x")
        self.git("add", "other.txt")
        self.git("commit", "-q", "-m", "c")
        self.file.write_bytes(SAMPLE_BYTES)
        result = self.run_checker("--base-ref", "HEAD")
        self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
        self.assertIn("does not exist at HEAD", result.stdout)

    def test_unknown_base_ref_is_a_usage_error(self):
        self.commit(SAMPLE_BYTES)
        self.assertEqual(self.run_checker("--base-ref", "no-such-ref").returncode, 2)

    def test_base_file_with_same_version_but_other_bytes_fails(self):
        self.file.write_bytes(SAMPLE_BYTES)
        live = self.repo / "live.json"
        doc = sample()
        doc["exercises"][0]["title"] = "Back Squat"
        live.write_bytes(dump(doc))
        result = self.run_checker("--base-file", str(live))
        self.assertEqual(result.returncode, 1)
        self.assertIn("check 6:", result.stdout)


if __name__ == "__main__":
    unittest.main()
