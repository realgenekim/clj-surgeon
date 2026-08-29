#!/usr/bin/env python3
"""Decompose privacy-safe Surgeon-complete to next-action event-clock boundaries.

The receipt is the counting authority. This script never reads transcript prose,
source, workspace paths, or raw provider events. It reports only aggregates.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import statistics
from collections import defaultdict
from pathlib import Path


def percentile(values: list[int], fraction: float) -> int | None:
    if not values:
        return None
    ordered = sorted(values)
    index = max(0, min(len(ordered) - 1, round((len(ordered) - 1) * fraction)))
    return ordered[index]


def distribution(values: list[int]) -> dict:
    return {
        "count": len(values),
        "max_ms": max(values) if values else None,
        "median_ms": round(statistics.median(values)) if values else None,
        "p90_ms": percentile(values, 0.9),
        "total_ms": sum(values),
    }


def overlap(start: int, end: int, item: dict) -> int:
    item_start = item["offset_ms"]
    item_end = item_start + item["wall_ms"]
    return max(0, min(end, item_end) - max(start, item_start))


def interval_coverage(intervals: list[tuple[int, int]]) -> int:
    merged: list[list[int]] = []
    for start, end in sorted(intervals):
        if end <= start:
            continue
        if merged and start <= merged[-1][1]:
            merged[-1][1] = max(merged[-1][1], end)
        else:
            merged.append([start, end])
    return sum(end - start for start, end in merged)


def kind_coverage(start: int, end: int, items: list[dict], kind: str) -> int:
    return interval_coverage([
        (max(start, item["offset_ms"]), min(end, item["offset_ms"] + item["wall_ms"]))
        for item in items if item["kind"] == kind
    ])


def item_union_coverage(start: int, end: int, items: list[dict], kinds: set[str] | None = None) -> int:
    return interval_coverage([
        (max(start, item["offset_ms"]), min(end, item["offset_ms"] + item["wall_ms"]))
        for item in items if kinds is None or item["kind"] in kinds
    ])


def provider(receipt: dict, name: str) -> dict:
    return receipt["providers"][name]


def classify_boundary(boundary: dict, items: list[dict]) -> dict:
    surgeon = next(
        item for item in items
        if item.get("action_ordinal") == boundary.get("action_ordinal")
    )
    start = surgeon["offset_ms"] + surgeon["wall_ms"]
    end = start + boundary["boundary_ms"]
    reasoning_items = [
        item for item in items
        if item["kind"] == "model-reasoning" and overlap(start, end, item)
    ]

    kinds = sorted({item["kind"] for item in items})
    coverage_by_kind = {
        kind: kind_coverage(start, end, items, kind)
        for kind in kinds if kind != "unattributed-gap"
    }
    recorded_reasoning = coverage_by_kind.get("model-reasoning", 0)
    recorded_message = coverage_by_kind.get("model-message", 0)
    recorded_other = interval_coverage([
        (max(start, item["offset_ms"]), min(end, item["offset_ms"] + item["wall_ms"]))
        for item in items
        if item["kind"] not in {"unattributed-gap", "model-reasoning", "model-message"}
    ])
    explicit_unattributed = kind_coverage(start, end, items, "unattributed-gap")
    core_union = item_union_coverage(
        start, end, items,
        {"unattributed-gap", "model-reasoning", "model-message"},
    )
    all_item_union = item_union_coverage(start, end, items)
    background_exclusive = max(0, all_item_union - core_union)
    clock_uncovered = max(0, end - start - all_item_union)

    if reasoning_items:
        first_reasoning = min(item["offset_ms"] for item in reasoning_items)
        last_reasoning_end = max(item["offset_ms"] + item["wall_ms"] for item in reasoning_items)
        pre_reasoning = sum(
            overlap(start, min(end, first_reasoning), item)
            for item in items if item["kind"] == "unattributed-gap"
        )
        post_reasoning = sum(
            overlap(max(start, last_reasoning_end), end, item)
            for item in items if item["kind"] == "unattributed-gap"
        )
        inter_reasoning = max(0, explicit_unattributed - pre_reasoning - post_reasoning)
        no_reasoning = 0
    else:
        pre_reasoning = 0
        inter_reasoning = 0
        post_reasoning = 0
        no_reasoning = explicit_unattributed

    endpoint = next((
        item for item in items
        if item.get("action_ordinal") == boundary.get("next_action_ordinal")
    ), None)

    return {
        "next_kind": boundary["next_kind"],
        "boundary_ms": boundary["boundary_ms"],
        "recorded_reasoning_ms": recorded_reasoning,
        "recorded_message_inside_boundary_ms": recorded_message,
        "recorded_other_inside_boundary_ms": recorded_other,
        "recorded_background_exclusive_ms": background_exclusive,
        "clock_uncovered_ms": clock_uncovered,
        "explicit_unattributed_ms": explicit_unattributed,
        "pre_reasoning_unattributed_ms": pre_reasoning,
        "inter_reasoning_unattributed_ms": inter_reasoning,
        "post_reasoning_unattributed_ms": post_reasoning,
        "no_reasoning_unattributed_ms": no_reasoning,
        "reasoning_item_count": len(reasoning_items),
        "next_endpoint_wall_ms": endpoint.get("wall_ms", 0) if endpoint else 0,
        "next_endpoint_phase": endpoint.get("phase") if endpoint else None,
        "source_transport": boundary.get("transport"),
        "source_status": boundary.get("status"),
        "recorded_coverage_by_kind_ms": coverage_by_kind,
    }


def summarize(rows: list[dict]) -> dict:
    fields = [
        "boundary_ms",
        "recorded_reasoning_ms",
        "recorded_message_inside_boundary_ms",
        "recorded_other_inside_boundary_ms",
        "recorded_background_exclusive_ms",
        "clock_uncovered_ms",
        "explicit_unattributed_ms",
        "pre_reasoning_unattributed_ms",
        "inter_reasoning_unattributed_ms",
        "post_reasoning_unattributed_ms",
        "no_reasoning_unattributed_ms",
        "next_endpoint_wall_ms",
    ]
    result = {field: distribution([row[field] for row in rows]) for field in fields}
    result["with_recorded_reasoning"] = sum(row["reasoning_item_count"] > 0 for row in rows)
    result["without_recorded_reasoning"] = sum(row["reasoning_item_count"] == 0 for row in rows)
    result["reasoning_items"] = sum(row["reasoning_item_count"] for row in rows)
    by_kind = defaultdict(int)
    for row in rows:
        for kind, wall_ms in row["recorded_coverage_by_kind_ms"].items():
            by_kind[kind] += wall_ms
    result["recorded_coverage_by_kind_total_ms"] = dict(sorted(by_kind.items()))
    return result


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("receipt", type=Path)
    args = parser.parse_args()
    receipt_bytes = args.receipt.read_bytes()
    receipt = json.loads(receipt_bytes)
    if receipt.get("status") != "ok":
        raise SystemExit(f"receipt status is {receipt.get('status')!r}, not 'ok'")

    rows = []
    for session in provider(receipt, "codex")["sessions"]:
        for turn in session["task_turns"]:
            items = turn.get("event_clock", {}).get("items", [])
            for boundary in turn.get("event_clock", {}).get("post_surgeon_boundaries", []):
                row = classify_boundary(boundary, items)
                if row["recorded_reasoning_ms"] != boundary.get("model_reasoning_ms", 0):
                    raise AssertionError("reasoning coverage differs from receipt boundary")
                if row["clock_uncovered_ms"]:
                    raise AssertionError("event clock leaves boundary wall uncovered")
                rows.append(row)

    authority = provider(receipt, "codex")["post_surgeon_boundary_wall"]
    compiled = distribution([row["boundary_ms"] for row in rows])
    if compiled != authority:
        raise AssertionError(f"boundary aggregate differs from receipt authority: {compiled} != {authority}")

    by_endpoint = defaultdict(list)
    for row in rows:
        by_endpoint[row["next_kind"]].append(row)

    selected = {
        name: summarize(by_endpoint[name])
        for name in ("surgeon-read", "surgeon-apply", "native-read", "native-patch")
    }
    print(json.dumps({
        "receipt_sha256": hashlib.sha256(receipt_bytes).hexdigest(),
        "window": receipt["window"],
        "all_boundaries": summarize(rows),
        "boundaries_without_overlapping_background_work": summarize([
            row for row in rows if row["recorded_background_exclusive_ms"] == 0
        ]),
        "by_endpoint": {name: summarize(group) for name, group in sorted(by_endpoint.items())},
        "selected_endpoint_comparison": selected,
    }, indent=2, sort_keys=True))


if __name__ == "__main__":
    main()
