#!/usr/bin/env python3
"""Score a retained Codex catalog-floor cohort without reading live state."""

import argparse
import csv
import json
import math
import random
import statistics
from pathlib import Path


ARMS = ("C", "T", "D", "P", "M", "I", "R")
MARGIN_MS = 125.0


def median(values):
    return statistics.median(values)


def percentile(values, probability):
    ordered = sorted(values)
    if not ordered:
        return None
    position = (len(ordered) - 1) * probability
    lower = math.floor(position)
    upper = math.ceil(position)
    if lower == upper:
        return ordered[lower]
    fraction = position - lower
    return ordered[lower] * (1 - fraction) + ordered[upper] * fraction


def interval(values):
    return [percentile(values, 0.025), percentile(values, 0.975)]


def paired(rows, left, right, metric="total_ms"):
    by_block = {}
    for row in rows:
        by_block.setdefault(int(row["block"]), {})[row["arm"]] = row
    return [float(block[right][metric]) - float(block[left][metric])
            for block in by_block.values()
            if left in block and right in block]


def bootstrap_paired(values, seed=20260829, samples=10000):
    rng = random.Random(seed)
    return [median([rng.choice(values) for _ in values]) for _ in range(samples)]


def theil_sen(rows, catalog_bytes):
    slopes = []
    mcp_rows = [row for row in rows if row["arm"] != "C"]
    by_block = {}
    for row in mcp_rows:
        by_block.setdefault(int(row["block"]), []).append(row)
    for block_rows in by_block.values():
        for index, left in enumerate(block_rows):
            for right in block_rows[index + 1:]:
                left_bytes = catalog_bytes[left["arm"]]
                right_bytes = catalog_bytes[right["arm"]]
                if left_bytes == right_bytes:
                    continue
                slopes.append((float(right["total_ms"]) - float(left["total_ms"])) /
                              (right_bytes - left_bytes))
    return median(slopes), slopes


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("result_dir", type=Path)
    args = parser.parse_args()
    root = args.result_dir
    rows = list(csv.DictReader((root / "runs.tsv").open(), delimiter="\t"))
    surfaces = json.loads((root / "catalogs.json").read_text())
    catalog_bytes = {arm: int(data["client_bytes"])
                     for arm, data in surfaces.items()}
    valid = [row for row in rows
             if row["environment_valid"] == "true"
             and row["semantic_correct"] == "true"
             and row["route_adherent"] == "true"]
    complete_blocks = []
    by_block = {}
    for row in valid:
        by_block.setdefault(int(row["block"]), set()).add(row["arm"])
    for block, arms in by_block.items():
        if arms == set(ARMS):
            complete_blocks.append(block)
    admitted = [row for row in valid if int(row["block"]) in complete_blocks]

    fixed = paired(admitted, "C", "T")
    byte_effect = paired(admitted, "T", "D")
    fixed_boot = bootstrap_paired(fixed) if fixed else []
    byte_boot = bootstrap_paired(byte_effect) if byte_effect else []
    slope, slopes = theil_sen(admitted, catalog_bytes) if admitted else (None, [])
    fixed_ci = interval(fixed_boot)
    byte_ci = interval(byte_boot)

    fixed_positive = bool(fixed_ci and fixed_ci[0] > MARGIN_MS)
    bytes_positive = bool(byte_ci and byte_ci[0] > MARGIN_MS)
    bytes_equivalent = bool(byte_ci and byte_ci[0] >= -MARGIN_MS
                            and byte_ci[1] <= MARGIN_MS)
    if fixed_positive and bytes_positive:
        verdict = "mixed"
    elif bytes_positive:
        verdict = "proportional"
    elif fixed_positive and bytes_equivalent:
        verdict = "fixed"
    else:
        verdict = "noise-or-unresolved"

    arm_summary = {}
    for arm in ARMS:
        samples = [float(row["total_ms"]) for row in admitted if row["arm"] == arm]
        arm_summary[arm] = {
            "n": len(samples),
            "median_total_ms": median(samples) if samples else None,
            "min_total_ms": min(samples) if samples else None,
            "max_total_ms": max(samples) if samples else None,
            "client_bytes": catalog_bytes[arm],
            "tool_count": surfaces[arm]["tool_count"],
            "parameter_count": surfaces[arm]["parameter_count"],
        }

    report = {
        "schema": "clj-surgeon.catalog-floor-score.v1",
        "verdict": verdict,
        "practical_equivalence_margin_ms": MARGIN_MS,
        "run_count": len(rows),
        "valid_run_count": len(valid),
        "complete_blocks": sorted(complete_blocks),
        "arm_summary": arm_summary,
        "fixed_handshake": {
            "contrast": "T-C",
            "paired_median_ms": median(fixed) if fixed else None,
            "bootstrap_95_ci_ms": fixed_ci,
        },
        "byte_effect": {
            "contrast": "D-T",
            "paired_median_ms": median(byte_effect) if byte_effect else None,
            "bootstrap_95_ci_ms": byte_ci,
            "theil_sen_ms_per_byte": slope,
            "theil_sen_pair_count": len(slopes),
        },
        "shape_checks": {
            "parameters_minus_description_ms": median(paired(admitted, "D", "P")) if admitted else None,
            "many_tools_minus_description_ms": median(paired(admitted, "D", "M")) if admitted else None,
            "full_projection_minus_inspect_only_ms": median(paired(admitted, "I", "R")) if admitted else None,
            "real_minus_tiny_ms": median(paired(admitted, "T", "R")) if admitted else None,
        },
        "promotion_authority": False,
    }
    (root / "score.json").write_text(json.dumps(report, indent=2, sort_keys=True) + "\n")
    print(json.dumps(report, indent=2, sort_keys=True))


if __name__ == "__main__":
    main()
