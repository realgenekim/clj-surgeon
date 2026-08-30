#!/usr/bin/env python3
"""Recompute and verify the committed warm-executor result without model calls."""

from __future__ import annotations

import argparse
import importlib.util
import json
from pathlib import Path


RUNNER = Path(__file__).with_name("run_warm_executor_screen.py")
SPEC = importlib.util.spec_from_file_location("warm_executor_screen", RUNNER)
assert SPEC and SPEC.loader
screen = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(screen)


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("result_dir")
    args = parser.parse_args()
    root = Path(args.result_dir).resolve()

    results = json.loads((root / "results.json").read_text())
    expected_summary = json.loads((root / "summary.json").read_text())
    actual_summary = screen.summarize(results)
    if actual_summary != expected_summary:
        raise RuntimeError("summary.json does not match the deterministic fold")
    if (root / "SUMMARY.md").read_text() != screen.render_summary(actual_summary):
        raise RuntimeError("SUMMARY.md does not match the deterministic rendering")
    finalization = json.loads((root / "finalization.json").read_text())
    if finalization["final_results_sha256"] != screen.sha256_file(root / "results.json"):
        raise RuntimeError("results.json hash differs from finalization receipt")

    for model in screen.MODELS:
        warm = [row for row in results["warm_prepared"] if row["model"] == model]
        if len(warm) != 10 or len({row["thread_id"] for row in warm}) != 1:
            raise RuntimeError(f"warm thread cardinality failed for {model}")
        if [row["bang"] for row in warm] != list(range(1, 11)):
            raise RuntimeError(f"warm bang sequence failed for {model}")
        if not all(row["score"]["exact"] and row["score"]["one_shot"] for row in warm):
            raise RuntimeError(f"warm exact/one-shot gate failed for {model}")
        if any(row["score"]["wrong_subject"] for row in warm):
            raise RuntimeError(f"wrong-subject gate failed for {model}")
        cold = [
            row
            for row in results["cold_prepared"]
            if row["model"] == model and row["score"]["exact"]
        ]
        if len(cold) != 5:
            raise RuntimeError(f"exact cold comparator cardinality failed for {model}")

    print(
        "verified: deterministic fold, hashes, 5 exact cold bangs/model, "
        "10 one-thread exact warm bangs/model, wrong-subject=0"
    )
    print(screen.render_summary(actual_summary))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
