#!/usr/bin/env python3
"""Fold the frozen primary run and preregistered replacement into one result."""

from __future__ import annotations

import argparse
import importlib.util
import json
import shutil
from pathlib import Path


RUNNER = Path(__file__).with_name("run_warm_executor_screen.py")
SPEC = importlib.util.spec_from_file_location("warm_executor_screen", RUNNER)
assert SPEC and SPEC.loader
screen = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(screen)


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--primary", required=True)
    parser.add_argument("--replacement", required=True)
    parser.add_argument("--out", required=True)
    args = parser.parse_args()

    primary = Path(args.primary).resolve()
    replacement = Path(args.replacement).resolve()
    out = Path(args.out).resolve()
    if out.exists():
        raise RuntimeError(f"final output already exists: {out}")
    shutil.copytree(primary, out)
    shutil.copytree(replacement, out / "registered-replacement")

    results = json.loads((primary / "results.json").read_text())
    replacements = screen.read_jsonl(replacement / "replacements.jsonl")
    results["cold_prepared"].extend(replacements)
    for row in replacements:
        screen.append_jsonl(out / "cold-prepared.jsonl", row)
    screen.write_json(out / "results.json", results)

    summary = screen.summarize(results)
    screen.write_json(out / "summary.json", summary)
    (out / "SUMMARY.md").write_text(screen.render_summary(summary))
    screen.write_json(
        out / "finalization.json",
        {
            "schema": "clj-surgeon.warm-executor-finalization/v1",
            "primary": str(primary),
            "primary_results_sha256": screen.sha256_file(primary / "results.json"),
            "replacement": str(replacement),
            "replacement_rows_sha256": screen.sha256_file(
                replacement / "replacements.jsonl"
            ),
            "replacement_count": len(replacements),
            "finalizer_sha256": screen.sha256_file(Path(__file__)),
            "final_results_sha256": screen.sha256_file(out / "results.json"),
        },
    )
    print(screen.render_summary(summary))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
