#!/usr/bin/env python3

import sys
import tempfile
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))

import external_corpus_shape_census as census


class ExternalCorpusShapeCensusTest(unittest.TestCase):
    def test_decodes_each_apply_patch_invocation(self):
        source = r'''
const patch = "*** Begin Patch\n*** Update File: src/a.clj\n@@\n-:old\n+:new\n*** End Patch";
await tools.apply_patch(patch);
const patch = "*** Begin Patch\n*** Update File: src/b.clj\n@@\n+x\n*** End Patch";
await tools.apply_patch(patch);
await tools.apply_patch("*** Begin Patch\n*** Update File: src/c.clj\n@@\n-x\n*** End Patch");
'''
        patches = census.patches_from_source(source)
        self.assertEqual(3, len(patches))
        self.assertIn("src/a.clj", patches[0])
        self.assertIn("src/b.clj", patches[1])
        self.assertIn("src/c.clj", patches[2])

    def test_classifies_hunks_and_confines_targets(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            patch = """*** Begin Patch
*** Update File: src/a.clj
@@
+abc
@@
-old
+newer
*** Update File: test/b.clj
@@
-gone
*** Update File: ../outside.clj
@@
+nope
*** End Patch"""
            hunks = census.parse_update_hunks(patch, root)
            self.assertEqual(["insertion", "replacement", "deletion"],
                             [hunk["shape"] for hunk in hunks])
            self.assertEqual([3, 8, 4], [
                hunk["added_bytes"] + hunk["removed_bytes"] for hunk in hunks])

    def test_infers_named_owner_without_emitting_it(self):
        owner = census.infer_owner([
            "(defn alpha []",
            "  :old)",
        ])
        self.assertEqual("defn:alpha", owner)
        with tempfile.TemporaryDirectory() as directory:
            hunks = census.parse_update_hunks(
                """*** Begin Patch
*** Update File: src/a.clj
@@
 (defn alpha []
-  :old)
+  :new)
*** End Patch""",
                Path(directory),
            )
        self.assertIsNotNone(hunks[0]["owner_hash"])
        self.assertNotIn("alpha", str(hunks[0]))

    def test_owner_rename_is_ambiguous(self):
        self.assertIsNone(census.infer_owner([
            "-(defn old-name [] :old)",
            "+(defn new-name [] :new)",
        ]))

    def test_workspace_projection_accepts_unquoted_object_keys(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            source = '{workspace_root: ' + repr(str(root)).replace("'", '"') + '}'
            self.assertEqual("match", census.structural_target_status(None, source, root))

    def test_subject_repetition_is_mission_scoped(self):
        rows = [
            {"status": "success", "mission": "m1", "existing_clojure": True,
             "clojure_targets": ["src/a.clj"]},
            {"status": "success", "mission": "m1", "existing_clojure": True,
             "clojure_targets": ["src/a.clj", "src/b.clj"]},
            {"status": "success", "mission": "m2", "existing_clojure": True,
             "clojure_targets": ["src/a.clj"]},
            {"status": "success", "mission": None, "existing_clojure": True,
             "clojure_targets": ["src/a.clj"]},
        ]
        report = census.subject_repetition(rows)
        self.assertEqual(3, report["distinct_subjects"])
        self.assertEqual(4, report["subject_occurrences"])
        self.assertEqual(1, report["repeated_subjects"])
        self.assertAlmostEqual(100 / 3, report["repeated_subject_percent"])
        self.assertEqual(1, report["unassigned_successful_existing_clojure_actions"])

    def test_addressable_ladder_uses_union_and_all_target_rules(self):
        base = {
            "status": "success", "existing_clojure": True,
            "in_repo_write": True,
            "comment_only": False, "small_single_hunk": False,
            "all_same_mission_created": False, "all_src_or_test": True,
        }
        rows = [
            base,
            {**base, "comment_only": True, "small_single_hunk": True},
            {**base, "all_same_mission_created": True},
            {**base, "all_src_or_test": False},
            {**base, "status": "failed"},
            {**base, "in_repo_write": False},
        ]
        report = census.addressable_ladder(rows)
        self.assertEqual(4, report["all_successful_native_writes"])
        self.assertEqual(1, report["trivial_union"])
        self.assertEqual(3, report["substantive_existing_clojure"])
        self.assertEqual(2, report["established_files_all_target_rule"])
        self.assertEqual(1, report["established_all_targets_in_src_or_test"])

    def test_repository_report_contains_no_root(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            report = census.repository_report(
                [],
                {"matched_evidence_files": 0, "active_evidence_files": 0,
                 "evidence_manifest_sha256": "manifest"},
                root,
                "root-hash",
            )
            self.assertNotIn(str(root), str(report))
            self.assertEqual("root-hash", report["repository_root_sha256"])


if __name__ == "__main__":
    unittest.main()
