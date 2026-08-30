#!/usr/bin/env python3
"""Attribute external write opportunity to routing distribution and task shape.

The fold reads only the frozen census rollouts. Its output contains counts,
hashes, and categorical evidence. It never emits paths, session identifiers,
commands, source, patches, prompts, or tool arguments.
"""

import argparse
import hashlib
import json
import sys
from collections import Counter, defaultdict
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))

import external_corpus_shape_census as census


MANAGED_MARKER = "BEGIN CLJ-SURGEON ROUTING"
INSTRUCTION_BUNDLE = "# AGENTS.md instructions for"


def instruction_text(event):
    payload = event.get("payload") or {}
    if (payload.get("type") == "message"
            and payload.get("role") in {"developer", "user"}):
        return json.dumps(payload.get("content"), ensure_ascii=False)
    if (event.get("type") == "event_msg"
            and payload.get("type") == "user_message"):
        return str(payload.get("message", ""))
    return ""


def guidance_states(paths, since, until):
    """Return call-time states and privacy-safe active-session counts."""
    by_call = {}
    session_states = Counter()
    active_count = 0
    for path in paths:
        state = "undeterminable"
        active = False
        with path.open() as handle:
            for line in handle:
                event = json.loads(line)
                timestamp_text = event.get("timestamp")
                if not timestamp_text:
                    continue
                timestamp = census.parse_instant(timestamp_text)
                if timestamp < since or timestamp >= until:
                    continue
                active = True
                text = instruction_text(event)
                if INSTRUCTION_BUNDLE in text:
                    state = ("guidance-present" if MANAGED_MARKER in text
                             else "guidance-absent")
                payload = event.get("payload") or {}
                if (event.get("type") == "response_item"
                        and payload.get("type") in {
                            "custom_tool_call", "function_call"}):
                    call_id = payload.get("call_id")
                    if call_id in by_call and by_call[call_id] != state:
                        raise ValueError("call id reused with conflicting guidance state")
                    by_call[call_id] = state
        if active:
            active_count += 1
            session_states[state] += 1
    return by_call, {
        "active_evidence_files": active_count,
        "by_final_instruction_state": dict(sorted(session_states.items())),
    }


def attributed_native_rows(paths, root, since, until):
    states, session_report = guidance_states(paths, since, until)
    calls, evidence = census.collect_calls(paths, root, since, until)
    native_calls = [call for call in calls if call["kind"] == "native"]
    rows = census.native_rows(calls, root)
    if len(native_calls) != len(rows):
        raise ValueError("native call and row cardinality differ")
    for call, row in zip(native_calls, rows):
        row["guidance_state"] = states.get(
            call.get("call_id"), "undeterminable")
    return rows, session_report, evidence


def is_addressable(row):
    return bool(
        row["status"] == "success"
        and row["in_repo_write"]
        and row["existing_clojure"]
        and not row["comment_only"]
        and not row["small_single_hunk"]
        and not row["all_same_mission_created"]
        and row["all_src_or_test"]
    )


def action_shape(row):
    hunks = [
        hunk for hunk in row["hunks"]
        if hunk["shape"] != "empty"
        and Path(hunk["target"]).suffix in census.CLOJURE_EXTENSIONS
    ]
    return {
        "file_count": len(set(row["clojure_targets"])),
        "hunk_count": len(hunks),
        "changed_line_count": sum(
            hunk["added_lines"] + hunk["removed_lines"] for hunk in hunks),
    }


def percentile(values, fraction):
    ordered = sorted(values)
    if not ordered:
        return None
    index = max(0, min(len(ordered) - 1, int(len(ordered) * fraction + 0.999999) - 1))
    return ordered[index]


def metric_summary(values):
    return {
        "min": min(values) if values else None,
        "median": percentile(values, 0.5),
        "p90": percentile(values, 0.9),
        "max": max(values) if values else None,
    }


def bin_counts(values, bins):
    counts = Counter()
    for value in values:
        for label, lower, upper in bins:
            if value >= lower and (upper is None or value <= upper):
                counts[label] += 1
                break
    return {label: counts[label] for label, _, _ in bins}


FILE_BINS = [
    ("1", 1, 1), ("2", 2, 2), ("3-4", 3, 4),
    ("5-8", 5, 8), ("9+", 9, None),
]
COUNT_BINS = [
    ("1-3", 1, 3), ("4-14", 4, 14),
    ("15-50", 15, 50), ("51+", 51, None),
]


def shape_report(rows):
    actions = [{**action_shape(row), "mission": row.get("mission")}
               for row in rows]
    missions = defaultdict(lambda: {
        "action_count": 0, "files": set(), "hunk_count": 0,
        "changed_line_count": 0,
    })
    for row, shape in zip(rows, actions):
        mission = missions[row.get("mission")]
        mission["action_count"] += 1
        mission["files"].update(row["clojure_targets"])
        mission["hunk_count"] += shape["hunk_count"]
        mission["changed_line_count"] += shape["changed_line_count"]
    mission_rows = [
        {
            "action_count": value["action_count"],
            "file_count": len(value["files"]),
            "hunk_count": value["hunk_count"],
            "changed_line_count": value["changed_line_count"],
        }
        for value in missions.values()
    ]

    def distribution(items):
        return {
            "count": len(items),
            "file_count": {
                "summary": metric_summary([item["file_count"] for item in items]),
                "bins": bin_counts([item["file_count"] for item in items], FILE_BINS),
            },
            "hunk_count": {
                "summary": metric_summary([item["hunk_count"] for item in items]),
                "bins": bin_counts([item["hunk_count"] for item in items], COUNT_BINS),
            },
            "changed_line_count": {
                "summary": metric_summary(
                    [item["changed_line_count"] for item in items]),
                "bins": bin_counts(
                    [item["changed_line_count"] for item in items], COUNT_BINS),
            },
        }

    action_small = sum(
        item["file_count"] <= 2 and item["hunk_count"] <= 3
        for item in actions)
    action_large_15 = sum(item["hunk_count"] >= 15 for item in actions)
    action_chord = sum(
        item["file_count"] >= 9 and item["hunk_count"] >= 51
        for item in actions)
    mission_small = sum(
        item["file_count"] <= 2 and item["hunk_count"] <= 3
        for item in mission_rows)
    mission_large_15 = sum(item["hunk_count"] >= 15 for item in mission_rows)
    mission_chord = sum(
        item["file_count"] >= 9 and item["hunk_count"] >= 51
        for item in mission_rows)

    return {
        "per_write_action": distribution(actions),
        "per_mission_addressable_subset": {
            **distribution(mission_rows),
            "action_count": {
                "summary": metric_summary(
                    [item["action_count"] for item in mission_rows]),
                "bins": bin_counts(
                    [item["action_count"] for item in mission_rows], COUNT_BINS),
            },
        },
        "acid_test_shape_proxies": {
            "status": "not-a-performance-classification",
            "reason": (
                "retained native patch hunks do not prove semantic change count, "
                "decision completeness, named-form count, or reference count"),
            "per_write_action": {
                "at_or_below_two_file_three_hunk_proxy": action_small,
                "at_or_above_fifteen_hunk_proxy": action_large_15,
                "at_or_above_nine_file_fifty_one_hunk_proxy": action_chord,
            },
            "per_mission_addressable_subset": {
                "at_or_below_two_file_three_hunk_proxy": mission_small,
                "at_or_above_fifteen_hunk_proxy": mission_large_15,
                "at_or_above_nine_file_fifty_one_hunk_proxy": mission_chord,
            },
            "surgeon_measured_or_predicted_win_fraction": None,
            "surgeon_win_fraction_status": "undeterminable",
        },
    }


def guidance_report(rows):
    successful = [
        row for row in rows
        if row["status"] == "success" and row["in_repo_write"]
    ]
    addressable = [row for row in rows if is_addressable(row)]
    return {
        "all_successful_in_repo_native_writes": {
            "count": len(successful),
            "by_guidance_state": dict(sorted(Counter(
                row["guidance_state"] for row in successful).items())),
        },
        "addressable_established_src_test_writes": {
            "count": len(addressable),
            "by_guidance_state": dict(sorted(Counter(
                row["guidance_state"] for row in addressable).items())),
        },
    }


def sha256_file(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--sessions-root", type=Path, required=True)
    parser.add_argument("--since", required=True)
    parser.add_argument("--until", required=True)
    parser.add_argument("--repo", type=census.parse_repo, action="append", required=True)
    parser.add_argument("--source-report", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()

    since = census.parse_instant(args.since)
    until = census.parse_instant(args.until)
    roots = dict(args.repo)
    paths = census.discover_evidence(args.sessions_root, roots)
    source_report = json.loads(args.source_report.read_text())
    repositories = {}
    for label, root in roots.items():
        rows, sessions, evidence = attributed_native_rows(
            paths.get(label, []), root, since, until)
        addressable = [row for row in rows if is_addressable(row)]
        expected = source_report["providers"]["codex"]["repositories"][label]
        if census.sha256_text(str(root)) != expected["repository_root_sha256"]:
            raise ValueError("repository identity differs from source census")
        if (evidence["evidence_manifest_sha256"]
                != expected["evidence_manifest_sha256"]):
            raise ValueError("evidence manifest differs from source census")
        successful = [
            row for row in rows
            if row["status"] == "success" and row["in_repo_write"]
        ]
        if len(successful) != expected["native"]["successful_repo_writes"]:
            raise ValueError("successful write denominator differs from source census")
        if len(addressable) != expected["addressable_ladder"][
                "established_all_targets_in_src_or_test"]:
            raise ValueError("addressable denominator differs from source census")
        repositories[label] = {
            "repository_root_sha256": expected["repository_root_sha256"],
            "source_evidence_manifest_sha256": evidence[
                "evidence_manifest_sha256"],
            "instruction_evidence_files": sessions,
            "routing_guidance_at_write_time": guidance_report(rows),
            "addressable_task_shapes": shape_report(addressable),
        }

    report = {
        "schema": "clj-surgeon.adoption-gap-attribution.v1",
        "window": {"since": args.since, "until": args.until},
        "source_census_report_sha256": sha256_file(args.source_report),
        "repositories": repositories,
        "privacy": {
            "paths_emitted": False,
            "session_identifiers_emitted": False,
            "commands_emitted": False,
            "source_emitted": False,
            "patches_emitted": False,
            "prompts_emitted": False,
            "tool_arguments_emitted": False,
        },
    }
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(report, indent=2, sort_keys=True) + "\n")
    print(json.dumps(report, indent=2, sort_keys=True))


if __name__ == "__main__":
    main()
