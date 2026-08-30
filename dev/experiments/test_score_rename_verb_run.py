import importlib.util
import json
import tempfile
import unittest
from pathlib import Path


MODULE_PATH = Path(__file__).with_name("score_rename_verb_run.py")
SPEC = importlib.util.spec_from_file_location("rename_verb_score", MODULE_PATH)
SCORE = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(SCORE)


class RenameVerbScoreTest(unittest.TestCase):
    def test_compact_argument_preserves_event_key_order(self):
        compact, parsed = SCORE.compact_argument(
            {"op": "rename-symbol", "from": "jitter-ms", "to": "retry-jitter-ms"}
        )
        self.assertEqual(SCORE.EXPECTED_VERB, parsed)
        self.assertEqual(
            '{"op":"rename-symbol","from":"jitter-ms","to":"retry-jitter-ms"}',
            compact,
        )
        self.assertEqual(64, len(compact.encode("utf-8")))

    def test_registered_percentage_reduction(self):
        self.assertEqual(90.0, SCORE.percentage_reduction(640, 64))
        self.assertIsNone(SCORE.percentage_reduction(0, 64))

    def test_aggregate_applies_all_three_kill_rules(self):
        clean = {
            "cohort": "sol",
            "completed": True,
            "exact": True,
            "one_shot": True,
            "wrong_subject": 0,
            "verb_adopted": True,
            "schema_fumble": False,
            "request_bytes": 64,
            "request_o200k_tokens": 19,
            "output_tokens": 20,
            "wall_ms": 1000,
        }
        runs = []
        for index in range(6):
            runs.append(clean | {"run_id": f"V{index}", "arm": "V"})
            runs.append(
                clean
                | {
                    "run_id": f"T{index}",
                    "arm": "T",
                    "verb_adopted": False,
                    "request_bytes": 640,
                    "request_o200k_tokens": 190,
                }
            )
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "runs.jsonl"
            path.write_text("".join(json.dumps(run) + "\n" for run in runs))
            summary = SCORE.aggregate(type("Args", (), {"runs": str(path)})())
        self.assertEqual("PASS_SCREEN", summary["decision"])
        self.assertEqual(
            90.0, summary["observed"]["median_request_byte_reduction_percent"]
        )
        self.assertFalse(any(summary["kill_rules"].values()))


if __name__ == "__main__":
    unittest.main()
