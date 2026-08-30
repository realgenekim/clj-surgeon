"""Red witness drafts for the measurement-evidence ratification leaf.

This file is intentionally not attached to the ordinary test runner until its
HLD, LLD, and EARS phases are ratified.
"""

import importlib.util
from pathlib import Path
import unittest


ROOT = Path(__file__).resolve().parents[4]
COLLECTOR_PATH = ROOT / "skills/study-agent-usage/scripts/collect_agent_usage.py"
SPEC = importlib.util.spec_from_file_location("collect_agent_usage", COLLECTOR_PATH)
collector = importlib.util.module_from_spec(SPEC)
assert SPEC.loader is not None
SPEC.loader.exec_module(collector)


class MeasurementEvidenceTest(unittest.TestCase):
    def test_zero_duration_omits_undefined_coverage_ratio(self):
        # @spec MEASURE-EVID-001 MEASURE-WALL-001
        clock = collector.compile_event_clock(
            "2026-08-29T00:00:00Z", 0, []
        )
        self.assertNotIn("coverage_ratio", clock)

    def test_observed_zero_coverage_remains_zero(self):
        # @spec MEASURE-EVID-001 MEASURE-WALL-001
        clock = collector.compile_event_clock(
            "2026-08-29T00:00:00Z", 1_000, []
        )
        self.assertEqual(0, clock["measured_coverage_ms"])
        self.assertEqual(1_000, clock["unattributed_wall_ms"])
        self.assertEqual(0.0, clock["coverage_ratio"])

    def test_low_coverage_dominant_turn_refuses_before_wall_aggregation(self):
        # @spec MEASURE-WALL-003
        aggregate = getattr(collector, "aggregate_event_clock_wall", None)
        self.assertIsNotNone(aggregate)
        rows = [
            {"turn_key": "covered", "wall_ms": 10_000, "coverage_ratio": 1.0},
            {"turn_key": "idle", "wall_ms": 41_700_000, "coverage_ratio": 0.0046},
        ]
        result = aggregate(rows, minimum_coverage_ratio=0.5)
        self.assertFalse(result["ok"])
        self.assertEqual("insufficient-clock-coverage", result["reason"])
        self.assertEqual(1, result["failing_turn_count"])
        self.assertNotIn("aggregate_wall_ms", result)

    def test_missing_coverage_refuses_without_coercion_or_filtering(self):
        # @spec MEASURE-WALL-002 MEASURE-WALL-003
        aggregate = getattr(collector, "aggregate_event_clock_wall", None)
        self.assertIsNotNone(aggregate)
        rows = [
            {"turn_key": "covered", "wall_ms": 10_000, "coverage_ratio": 1.0},
            {"turn_key": "unknown", "wall_ms": 20_000},
        ]
        result = aggregate(rows, minimum_coverage_ratio=0.5)
        self.assertFalse(result["ok"])
        self.assertEqual("insufficient-clock-coverage", result["reason"])
        self.assertNotIn("aggregate_wall_ms", result)


if __name__ == "__main__":
    unittest.main()
