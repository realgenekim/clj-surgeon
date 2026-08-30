#!/usr/bin/env python3

import importlib.util
import tempfile
import unittest
from pathlib import Path


RUNNER = Path(__file__).with_name("run_warm_executor_screen.py")
SPEC = importlib.util.spec_from_file_location("warm_executor_screen", RUNNER)
assert SPEC and SPEC.loader
screen = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(screen)


class WarmExecutorScreenTest(unittest.TestCase):
    def test_expected_fixture_advances_only_requested_prefix(self):
        template = (
            "(def slot-01 {:slot 1 :state :todo})\n"
            "(def slot-02 {:slot 2 :state :todo})\n"
        )
        self.assertEqual(
            "(def slot-01 {:slot 1 :state :done-01})\n"
            "(def slot-02 {:slot 2 :state :todo})\n",
            screen.expected_fixture(template, 1),
        )

    def test_alternating_order_is_counterbalanced(self):
        self.assertEqual(screen.MODELS, screen.alternating_order(1))
        self.assertEqual(tuple(reversed(screen.MODELS)), screen.alternating_order(2))

    def test_score_distinguishes_clean_refusal_from_wrong_subject(self):
        template = "(def slot-01 {:slot 1 :state :todo})\n"
        with tempfile.TemporaryDirectory() as raw:
            workspace = Path(raw)
            target = workspace / screen.FIXTURE_REL
            target.parent.mkdir(parents=True)
            target.write_text(template)
            before = screen.tree_hashes(workspace)
            turn = {"turn_id": "t1", "events": []}
            clean = screen.score_prepared_turn(
                turn, workspace, 1, template, before, screen.tree_hashes(workspace)
            )
            self.assertFalse(clean["exact"])
            self.assertFalse(clean["wrong_subject"])

            target.write_text("(def slot-01 {:slot 999 :state :done-01})\n")
            wrong = screen.score_prepared_turn(
                turn, workspace, 1, template, before, screen.tree_hashes(workspace)
            )
            self.assertFalse(wrong["exact"])
            self.assertTrue(wrong["wrong_subject"])

    def test_summary_uses_registered_amortization_and_drift_rules(self):
        results = {
            "cold_trivial": [],
            "cold_prepared": [],
            "warm_prepared": [],
            "warmup": [],
        }
        for model in screen.MODELS:
            for index in range(5):
                results["cold_trivial"].append(
                    {
                        "model": model,
                        "total_e2e_ms": 3200,
                        "process_bootstrap_ms": 300,
                        "thread_setup_ms": 100,
                        "request_to_first_token_ms": 2500,
                        "decode_tail_ms": 300,
                    }
                )
                results["cold_prepared"].append(
                    {
                        "model": model,
                        "total_e2e_ms": 5000,
                        "score": {"exact": True},
                    }
                )
            for index in range(10):
                results["warm_prepared"].append(
                    {
                        "model": model,
                        "turn_e2e_ms": 3500,
                        "score": {
                            "exact": True,
                            "one_shot": True,
                            "wrong_subject": False,
                        },
                    }
                )
        summary = screen.summarize(results)
        spark = summary["models"][screen.MODELS[0]]
        self.assertEqual(2, spark["amortization_edits_at_2s_savings"])
        self.assertEqual(4, spark["amortization_edits_at_1s_savings"])
        self.assertEqual(1500, spark["matched_cold_minus_warm_ms"])
        self.assertFalse(spark["drift_signal"])
        self.assertTrue(summary["winning_pattern"])


if __name__ == "__main__":
    unittest.main()
