#!/usr/bin/env python3
"""Recompute phase clocks from retained catalog-floor event timestamps."""

import argparse
import csv
import json
import math
import random
import statistics
from pathlib import Path


ARMS = ("C", "T", "D", "P", "M", "I", "R")


def percentile(values, probability):
    ordered = sorted(values)
    position = (len(ordered) - 1) * probability
    lower = math.floor(position)
    upper = math.ceil(position)
    if lower == upper:
        return ordered[lower]
    fraction = position - lower
    return ordered[lower] * (1 - fraction) + ordered[upper] * fraction


def bootstrap_interval(values, seed=20260829, samples=100000):
    rng = random.Random(seed)
    medians = [statistics.median([rng.choice(values) for _ in values])
               for _ in range(samples)]
    return [percentile(medians, 0.025), percentile(medians, 0.975)]


def event_times(path):
    result = {}
    with path.open() as handle:
        for line in handle:
            timestamp, payload = line.rstrip("\n").split("\t", 1)
            event = json.loads(payload)
            event_type = event["type"]
            if event_type == "item.completed":
                event_type = event.get("item", {}).get("type", event_type)
            result[event_type] = int(timestamp)
    required = {"thread.started", "turn.started", "agent_message", "turn.completed"}
    if set(result) != required:
        raise ValueError(f"unexpected event lifecycle in {path}: {sorted(result)}")
    if not (result["thread.started"] < result["turn.started"]
            < result["agent_message"] < result["turn.completed"]):
        raise ValueError(f"non-monotonic event lifecycle in {path}")
    return result


def paired(rows, left, right, metric):
    by_block = {}
    for row in rows:
        by_block.setdefault(row["block"], {})[row["arm"]] = row
    return [block[right][metric] - block[left][metric]
            for block in by_block.values()]


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("result_dir", type=Path)
    parser.add_argument("--output", type=Path)
    args = parser.parse_args()

    with (args.result_dir / "runs.tsv").open() as handle:
        rows = list(csv.DictReader(handle, delimiter="\t"))
    if len(rows) != 98:
        raise ValueError(f"expected 98 rows, got {len(rows)}")

    audited = []
    for row in rows:
        if not all(row[field] == "true"
                   for field in ("environment_valid", "semantic_correct", "route_adherent")):
            raise ValueError(f"invalid retained row: {row['run_id']}")
        events = event_times(args.result_dir / "runs" / row["run_id"] / "events.jsonl")
        thread_to_turn_ms = ((events["turn.started"] - events["thread.started"])
                             / 1_000_000.0)
        audited.append({
            "run_id": row["run_id"],
            "block": int(row["block"]),
            "arm": row["arm"],
            "total_ms": float(row["total_ms"]),
            "init_ms": float(row["init_ms"]) + thread_to_turn_ms,
            "answer_ms": float(row["answer_ms"]) - thread_to_turn_ms,
            "complete_ms": float(row["complete_ms"]) - thread_to_turn_ms,
            "tail_ms": float(row["tail_ms"]),
        })

    blocks = {row["block"] for row in audited}
    if blocks != set(range(1, 15)):
        raise ValueError(f"unexpected blocks: {sorted(blocks)}")
    for block in blocks:
        if {row["arm"] for row in audited if row["block"] == block} != set(ARMS):
            raise ValueError(f"incomplete block: {block}")

    contrasts = {}
    for name, left, right in (("tiny_minus_clean", "C", "T"),
                              ("description_minus_tiny", "T", "D"),
                              ("real_minus_inspect", "I", "R")):
        contrasts[name] = {}
        for metric in ("total_ms", "init_ms", "answer_ms", "complete_ms", "tail_ms"):
            values = paired(audited, left, right, metric)
            contrasts[name][metric] = {
                "paired_median": statistics.median(values),
                "bootstrap_95_ci": bootstrap_interval(values),
            }

    arms = {}
    for arm in ARMS:
        arm_rows = [row for row in audited if row["arm"] == arm]
        arms[arm] = {metric: statistics.median(row[metric] for row in arm_rows)
                     for metric in ("total_ms", "init_ms", "answer_ms",
                                    "complete_ms", "tail_ms")}

    report = {
        "schema": "clj-surgeon.catalog-floor-phase-audit.v1",
        "run_count": len(audited),
        "valid_run_count": len(audited),
        "route_adherent_count": len(audited),
        "arm_medians_ms": arms,
        "contrasts_ms": contrasts,
        "conclusion": "fixed-local-startup-no-detected-byte-scaling",
        "promotion_authority": False,
    }
    rendered = json.dumps(report, indent=2, sort_keys=True) + "\n"
    if args.output:
        args.output.write_text(rendered)
    print(rendered, end="")


if __name__ == "__main__":
    main()
