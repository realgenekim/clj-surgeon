from __future__ import annotations

import copy
import unittest

import evidence_oracle as oracle


CATALOG = "1" * 64
STATIC = "2" * 64
CANDIDATE = "3" * 64
BEFORE_TREE = "4" * 64
AFTER_TREE = "5" * 64
OLD_FILE = "6" * 64
NEW_FILE = "7" * 64

EXPECTED_IDENTITIES = {
    "catalog_sha256": CATALOG,
    "static_surface_sha256": STATIC,
    "candidate_policy_sha256": CANDIDATE,
}


def resequence(trace: dict) -> dict:
    for index, event in enumerate(trace["events"]):
        event["sequence"] = index
    return trace


def valid_trace() -> dict:
    inspect_arguments = {
        "workspace_root": "/work/project",
        "requests": [{"file": "src/example.clj", "forms": ["target"]}],
    }
    edit_arguments = {
        "workspace_root": "/work/project",
        "edits": [{
            "file": "src/example.clj",
            "within": {"form": "target"},
            "from": ":old",
            "to": ":new",
            "matches": 1,
        }],
    }
    inspect_hash = oracle.canonical_argument_hash(inspect_arguments)
    edit_hash = oracle.canonical_argument_hash(edit_arguments)
    return {
        "attempt_id": "E-T-1",
        "phase": "efficacy",
        "arm": "treatment",
        "identities": copy.deepcopy(EXPECTED_IDENTITIES),
        "events": [
            {
                "event_type": "file_snapshot",
                "sequence": 0,
                "snapshot": "before",
                "tree_sha256": BEFORE_TREE,
            },
            {
                "event_type": "tool_start",
                "sequence": 1,
                "event_id": "tool-inspect",
                "request_id": "request-inspect",
                "tool": "inspect_clojure",
                "activity": "read",
                "mutation_route": None,
                "arguments": inspect_arguments,
            },
            {
                "event_type": "proxy_call",
                "sequence": 2,
                "event_id": "proxy-inspect",
                "tool_event_id": "tool-inspect",
                "request_id": "request-inspect",
                "arguments": copy.deepcopy(inspect_arguments),
                "argument_sha256": inspect_hash,
            },
            {
                "event_type": "proxy_result",
                "sequence": 3,
                "event_id": "proxy-inspect",
                "tool_event_id": "tool-inspect",
                "request_id": "request-inspect",
                "argument_sha256": inspect_hash,
                "outcome": "success",
                "prepared_request_exposed": True,
                "candidate_descriptor_sha256": CANDIDATE,
                "catalog_sha256": CATALOG,
                "static_surface_sha256": STATIC,
            },
            {
                "event_type": "tool_completion",
                "sequence": 4,
                "event_id": "tool-inspect",
                "request_id": "request-inspect",
                "tool": "inspect_clojure",
                "activity": "read",
                "mutation_route": None,
                "argument_sha256": inspect_hash,
                "outcome": "success",
                "refusal_reason": None,
            },
            {
                "event_type": "tool_start",
                "sequence": 5,
                "event_id": "tool-edit",
                "request_id": "request-edit",
                "tool": "edit_clojure",
                "activity": "mutation",
                "mutation_route": "edit-clojure",
                "arguments": edit_arguments,
            },
            {
                "event_type": "tool_completion",
                "sequence": 6,
                "event_id": "tool-edit",
                "request_id": "request-edit",
                "tool": "edit_clojure",
                "activity": "mutation",
                "mutation_route": "edit-clojure",
                "argument_sha256": edit_hash,
                "outcome": "success",
                "refusal_reason": None,
            },
            {
                "event_type": "file_change",
                "sequence": 7,
                "event_id": "file-change-1",
                "change_type": "modified",
                "path": "src/example.clj",
                "to_path": None,
                "before_sha256": OLD_FILE,
                "after_sha256": NEW_FILE,
            },
            {
                "event_type": "file_snapshot",
                "sequence": 8,
                "snapshot": "after",
                "tree_sha256": AFTER_TREE,
            },
        ],
    }


def assert_refusal(test: unittest.TestCase, code: str, trace: dict) -> None:
    with test.assertRaises(oracle.EvidenceError) as raised:
        oracle.compile_evidence(trace, EXPECTED_IDENTITIES)
    test.assertEqual(code, raised.exception.code)


class EvidenceOracleTest(unittest.TestCase):
    def test_valid_trace_compiles_only_joined_raw_facts(self):
        report = oracle.compile_evidence(valid_trace(), EXPECTED_IDENTITIES)
        facts = report["semantic_facts"]

        self.assertTrue(report["ok"])
        self.assertEqual("edit-clojure", facts["first_mutation_route"])
        self.assertEqual(1, facts["mutation_attempt_count"])
        self.assertEqual(0, facts["refusal_count"])
        self.assertEqual(1, facts["prepared_request_exposure_count"])
        self.assertEqual(BEFORE_TREE, facts["correctness_inputs"]["before_tree_sha256"])
        self.assertEqual(AFTER_TREE, facts["correctness_inputs"]["after_tree_sha256"])
        self.assertEqual("modified", facts["correctness_inputs"]["file_changes"][0]["change_type"])

    def test_orphan_completion_refuses(self):
        trace = valid_trace()
        trace["events"] = [
            event for event in trace["events"]
            if not (event["event_type"] == "tool_start" and event.get("event_id") == "tool-edit")
        ]
        assert_refusal(self, "orphan-tool-completion", resequence(trace))

    def test_duplicate_event_id_refuses(self):
        trace = valid_trace()
        duplicate = copy.deepcopy(trace["events"][1])
        trace["events"].insert(2, duplicate)
        assert_refusal(self, "duplicate-tool-start", resequence(trace))

    def test_reversed_completion_refuses(self):
        trace = valid_trace()
        start = trace["events"].pop(5)
        trace["events"].insert(7, start)
        assert_refusal(self, "reversed-tool-lifecycle", resequence(trace))

    def test_mismatched_tool_types_refuse(self):
        trace = valid_trace()
        trace["events"][6]["tool"] = "apply_clojure_changes"
        trace["events"][6]["mutation_route"] = "apply-clojure-changes"
        assert_refusal(self, "mismatched-tool-lifecycle", trace)

    def test_mismatched_tool_argument_hash_refuses(self):
        trace = valid_trace()
        trace["events"][6]["argument_sha256"] = "8" * 64
        assert_refusal(self, "tool-argument-hash-mismatch", trace)

    def test_mismatched_proxy_request_id_refuses(self):
        trace = valid_trace()
        trace["events"][3]["request_id"] = "wrong-request"
        assert_refusal(self, "mismatched-proxy-lifecycle", trace)

    def test_mismatched_proxy_argument_hash_refuses(self):
        trace = valid_trace()
        trace["events"][3]["argument_sha256"] = "8" * 64
        assert_refusal(self, "proxy-result-argument-hash-mismatch", trace)

    def test_unrecognized_mutation_activity_refuses(self):
        trace = valid_trace()
        trace["events"][5]["tool"] = "mystery_writer"
        trace["events"][6]["tool"] = "mystery_writer"
        assert_refusal(self, "unrecognized-mutation-tool", trace)

    def test_unrecognized_file_change_activity_refuses(self):
        trace = valid_trace()
        trace["events"][7]["change_type"] = "overwrote-maybe"
        assert_refusal(self, "unrecognized-file-change-activity", trace)

    def test_catalog_and_static_proxy_identity_mismatches_refuse(self):
        fixtures = [
            ("catalog_sha256", "9" * 64, "proxy-catalog-identity-mismatch"),
            ("static_surface_sha256", "9" * 64, "proxy-static-surface-identity-mismatch"),
        ]
        for field, value, code in fixtures:
            with self.subTest(field=field):
                trace = valid_trace()
                trace["events"][3][field] = value
                assert_refusal(self, code, trace)

    def test_frozen_candidate_policy_identity_mismatch_refuses(self):
        trace = valid_trace()
        trace["identities"]["candidate_policy_sha256"] = "9" * 64
        assert_refusal(self, "identity-mismatch", trace)

    def test_per_call_candidate_descriptor_hash_is_not_a_global_identity(self):
        trace = valid_trace()
        trace["events"][3]["candidate_descriptor_sha256"] = "9" * 64
        report = oracle.compile_evidence(trace, EXPECTED_IDENTITIES)
        self.assertEqual(
            "9" * 64,
            report["semantic_facts"]["proxy_calls"][0]["candidate_descriptor_sha256"],
        )

        trace = valid_trace()
        trace["events"][3]["candidate_descriptor_sha256"] = "not-a-hash"
        assert_refusal(self, "sha256-required", trace)

    def test_refusal_is_derived_from_joined_completion(self):
        trace = valid_trace()
        trace["events"][6]["outcome"] = "refusal"
        trace["events"][6]["refusal_reason"] = "stale-source"
        trace["events"] = [
            event for event in trace["events"]
            if event["event_type"] != "file_change"
        ]
        trace["events"][-1]["tree_sha256"] = BEFORE_TREE
        report = oracle.compile_evidence(resequence(trace), EXPECTED_IDENTITIES)

        self.assertEqual(1, report["semantic_facts"]["refusal_count"])
        self.assertEqual(
            [{"tool": "edit_clojure", "reason": "stale-source"}],
            report["semantic_facts"]["refusals"],
        )

    def test_safety_exposure_and_unchanged_source_are_raw_derived(self):
        trace = valid_trace()
        trace["phase"] = "safety"
        trace["events"] = [
            event for event in trace["events"]
            if event.get("event_id") not in {"tool-edit", "file-change-1"}
        ]
        trace["events"][-1]["tree_sha256"] = BEFORE_TREE
        report = oracle.compile_evidence(resequence(trace), EXPECTED_IDENTITIES)
        safety = report["semantic_facts"]["safety"]

        self.assertTrue(safety["prepared_request_exposed"])
        self.assertEqual(0, safety["mutation_attempt_count"])
        self.assertEqual(0, safety["file_change_activity_count"])
        self.assertFalse(safety["source_changed"])

    def test_joined_proxy_timeout_is_a_closed_nonsuccess(self):
        trace = valid_trace()
        trace["events"] = [
            event for event in trace["events"]
            if event.get("event_id") not in {"tool-edit", "file-change-1"}
        ]
        proxy_result = next(
            event for event in trace["events"]
            if event["event_type"] == "proxy_result"
        )
        proxy_result["outcome"] = "timeout"
        proxy_result["prepared_request_exposed"] = False
        proxy_result["candidate_descriptor_sha256"] = None
        tool_completion = next(
            event for event in trace["events"]
            if event["event_type"] == "tool_completion"
        )
        tool_completion["outcome"] = "timeout"
        trace["events"][-1]["tree_sha256"] = BEFORE_TREE

        report = oracle.compile_evidence(resequence(trace), EXPECTED_IDENTITIES)
        self.assertEqual("timeout", report["semantic_facts"]["tool_calls"][0]["outcome"])
        self.assertEqual("timeout", report["semantic_facts"]["proxy_calls"][0]["outcome"])
        self.assertEqual(0, report["semantic_facts"]["prepared_request_exposure_count"])

    def test_native_mutate_then_rollback_is_preserved_in_both_phases(self):
        arguments = {
            "changes": [{"path": "src/example.clj", "change_type": "modified"}],
        }
        argument_hash = oracle.canonical_argument_hash(arguments)
        for phase in ("efficacy", "safety"):
            with self.subTest(phase=phase):
                trace = {
                    "attempt_id": f"rollback-{phase}",
                    "phase": phase,
                    "arm": "treatment",
                    "identities": copy.deepcopy(EXPECTED_IDENTITIES),
                    "events": [
                        {
                            "event_type": "file_snapshot",
                            "sequence": 0,
                            "snapshot": "before",
                            "tree_sha256": BEFORE_TREE,
                        },
                        {
                            "event_type": "tool_start",
                            "sequence": 1,
                            "event_id": "native-change",
                            "request_id": "native-change",
                            "tool": "apply_patch",
                            "activity": "mutation",
                            "mutation_route": "native-patch",
                            "arguments": arguments,
                        },
                        {
                            "event_type": "tool_completion",
                            "sequence": 2,
                            "event_id": "native-change",
                            "request_id": "native-change",
                            "tool": "apply_patch",
                            "activity": "mutation",
                            "mutation_route": "native-patch",
                            "argument_sha256": argument_hash,
                            "outcome": "success",
                            "refusal_reason": None,
                        },
                        {
                            "event_type": "file_snapshot",
                            "sequence": 3,
                            "snapshot": "after",
                            "tree_sha256": BEFORE_TREE,
                        },
                    ],
                }
                facts = oracle.compile_evidence(
                    trace, EXPECTED_IDENTITIES
                )["semantic_facts"]
                self.assertEqual("native-patch", facts["first_mutation_route"])
                self.assertEqual("success", facts["mutation_attempts"][0]["outcome"])
                self.assertEqual([], facts["correctness_inputs"]["file_changes"])
                if phase == "safety":
                    self.assertEqual(1, facts["safety"]["mutation_attempt_count"])
                    self.assertEqual(0, facts["safety"]["file_change_activity_count"])
                    self.assertFalse(facts["safety"]["source_changed"])

    def test_report_hash_uses_canonical_semantic_facts_not_event_ids(self):
        first = valid_trace()
        second = valid_trace()
        second["attempt_id"] = "another-provenance-id"
        replacements = {
            "tool-inspect": "renamed-tool-inspect",
            "proxy-inspect": "renamed-proxy-inspect",
            "tool-edit": "renamed-tool-edit",
            "file-change-1": "renamed-file-change",
        }
        for event in second["events"]:
            for field in ("event_id", "tool_event_id"):
                if event.get(field) in replacements:
                    event[field] = replacements[event[field]]

        first_report = oracle.compile_evidence(first, EXPECTED_IDENTITIES)
        second_report = oracle.compile_evidence(second, EXPECTED_IDENTITIES)
        self.assertNotEqual(first_report["attempt_id"], second_report["attempt_id"])
        self.assertNotEqual(first_report["evidence_index"], second_report["evidence_index"])
        self.assertEqual(first_report["semantic_facts"], second_report["semantic_facts"])
        self.assertEqual(first_report["report_sha256"], second_report["report_sha256"])


if __name__ == "__main__":
    unittest.main()
