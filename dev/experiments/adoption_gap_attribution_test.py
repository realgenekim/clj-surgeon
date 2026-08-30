#!/usr/bin/env python3

import json
import sys
import tempfile
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))

import adoption_gap_attribution as attribution


def event(timestamp, event_type, payload):
    return {"timestamp": timestamp, "type": event_type, "payload": payload}


class AdoptionGapAttributionTest(unittest.TestCase):
    def test_guidance_is_bound_at_call_time(self):
        rows = [
            event("2026-08-23T00:00:00Z", "response_item", {
                "type": "function_call", "call_id": "before"}),
            event("2026-08-23T00:00:01Z", "response_item", {
                "type": "message", "role": "user",
                "content": "# AGENTS.md instructions for /tmp/x\nno managed block"}),
            event("2026-08-23T00:00:02Z", "response_item", {
                "type": "function_call", "call_id": "absent"}),
            event("2026-08-23T00:00:03Z", "response_item", {
                "type": "message", "role": "user",
                "content": "# AGENTS.md instructions for /tmp/x\nBEGIN CLJ-SURGEON ROUTING"}),
            event("2026-08-23T00:00:04Z", "response_item", {
                "type": "function_call", "call_id": "present"}),
        ]
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "rollout-test.jsonl"
            path.write_text("".join(json.dumps(row) + "\n" for row in rows))
            states, report = attribution.guidance_states(
                [path],
                attribution.census.parse_instant("2026-08-22T00:00:00Z"),
                attribution.census.parse_instant("2026-08-30T00:00:00Z"),
            )
        self.assertEqual("undeterminable", states["before"])
        self.assertEqual("guidance-absent", states["absent"])
        self.assertEqual("guidance-present", states["present"])
        self.assertEqual(
            {"guidance-present": 1}, report["by_final_instruction_state"])

    def test_addressable_filter_matches_census_ladder(self):
        base = {
            "status": "success", "in_repo_write": True,
            "existing_clojure": True, "comment_only": False,
            "small_single_hunk": False,
            "all_same_mission_created": False, "all_src_or_test": True,
        }
        self.assertTrue(attribution.is_addressable(base))
        for key in (
                "in_repo_write", "existing_clojure", "all_src_or_test"):
            self.assertFalse(attribution.is_addressable({**base, key: False}))
        for key in (
                "comment_only", "small_single_hunk",
                "all_same_mission_created"):
            self.assertFalse(attribution.is_addressable({**base, key: True}))

    def test_shape_bins_retain_losses_and_crossover_unknown(self):
        rows = [
            {
                "mission": "m1", "clojure_targets": ["src/a.clj"],
                "hunks": [{"shape": "replacement", "target": "src/a.clj",
                           "added_lines": 1, "removed_lines": 1}],
            },
            {
                "mission": "m1", "clojure_targets": ["src/a.clj", "src/b.clj"],
                "hunks": [
                    {"shape": "insertion", "target": "src/a.clj",
                     "added_lines": 10, "removed_lines": 0},
                    {"shape": "replacement", "target": "src/b.clj",
                     "added_lines": 10, "removed_lines": 10},
                ],
            },
        ]
        report = attribution.shape_report(rows)
        self.assertEqual(2, report["per_write_action"]["count"])
        self.assertEqual(2, report["per_write_action"]["file_count"]["bins"]["1"]
                         + report["per_write_action"]["file_count"]["bins"]["2"])
        proxies = report["acid_test_shape_proxies"]
        self.assertEqual("undeterminable", proxies["surgeon_win_fraction_status"])
        self.assertIsNone(proxies["surgeon_measured_or_predicted_win_fraction"])


if __name__ == "__main__":
    unittest.main()
