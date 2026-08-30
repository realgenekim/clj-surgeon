#!/usr/bin/env python3
"""Score retained Codex streams for the preregistered rename-verb screen."""

from __future__ import annotations

import argparse
import hashlib
import importlib.metadata
import json
import re
import statistics
from pathlib import Path


EXPECTED_VERB = {
    "op": "rename-symbol",
    "from": "jitter-ms",
    "to": "retry-jitter-ms",
}


def load_events(path: Path) -> list[dict]:
    events = []
    for line in path.read_text().splitlines():
        if line.strip():
            events.append(json.loads(line))
    return events


def compact_argument(value) -> tuple[str, object]:
    if isinstance(value, str):
        parsed = json.loads(value)
        return json.dumps(parsed, ensure_ascii=False, separators=(",", ":")), parsed
    return json.dumps(value, ensure_ascii=False, separators=(",", ":")), value


def structured_result(item: dict) -> dict:
    result = item.get("result") or {}
    if not isinstance(result, dict):
        return {}
    value = result.get("structured_content", result.get("structuredContent", {}))
    if isinstance(value, str):
        try:
            return json.loads(value)
        except json.JSONDecodeError:
            return {}
    return value if isinstance(value, dict) else {}


def source_manifest(root: Path) -> dict[str, str]:
    manifest = {}
    for path in sorted(root.rglob("*.clj")):
        if path.is_file():
            manifest[path.relative_to(root).as_posix()] = hashlib.sha256(
                path.read_bytes()
            ).hexdigest()
    return manifest


def score(args: argparse.Namespace) -> dict:
    import tiktoken

    events = load_events(Path(args.events))
    started = [
        event.get("item", {})
        for event in events
        if event.get("type") == "item.started"
    ]
    completed_items = [
        event.get("item", {})
        for event in events
        if event.get("type") == "item.completed"
    ]
    mcp_started = [item for item in started if item.get("type") == "mcp_tool_call"]
    mcp_completed = [
        item for item in completed_items if item.get("type") == "mcp_tool_call"
    ]

    encoding = tiktoken.get_encoding("o200k_base")
    arguments = []
    for index, item in enumerate(mcp_started, start=1):
        compact, parsed = compact_argument(item.get("arguments", {}))
        arguments.append(
            {
                "index": index,
                "tool": item.get("tool"),
                "arguments": parsed,
                "compact_json": compact,
                "bytes": len(compact.encode("utf-8")),
                "o200k_tokens": len(encoding.encode(compact)),
            }
        )

    Path(args.request_output).write_text(
        "".join(json.dumps(item, ensure_ascii=False) + "\n" for item in arguments)
    )

    failures = 0
    for item in mcp_completed:
        result = structured_result(item)
        if (
            item.get("status") == "failed"
            or result.get("ok") is False
            or result.get("error_type") is not None
        ):
            failures += 1

    workspace = Path(args.workspace)
    expected = Path(args.expected)
    final_manifest = source_manifest(workspace)
    expected_manifest = source_manifest(expected)
    exact = final_manifest == expected_manifest
    unexpected_paths = sorted(set(final_manifest) - set(expected_manifest))
    missing_paths = sorted(set(expected_manifest) - set(final_manifest))
    mismatched_paths = sorted(
        path
        for path in set(final_manifest) & set(expected_manifest)
        if final_manifest[path] != expected_manifest[path]
    )
    source_text = "\n".join(
        path.read_text() for path in sorted(workspace.rglob("*.clj")) if path.is_file()
    )
    old_symbol_count = len(re.findall(r"(?<!retry-)jitter-ms", source_text))
    new_symbol_count = source_text.count("retry-jitter-ms")

    messages = [
        item.get("text", "")
        for item in completed_items
        if item.get("type") == "agent_message"
    ]
    final_message = messages[-1] if messages else ""
    turns = [event for event in events if event.get("type") == "turn.completed"]
    usage = (turns[-1].get("usage") or {}) if turns else {}
    exit_code = int(args.exit_code)
    verb_adopted = bool(arguments) and arguments[0]["arguments"] == EXPECTED_VERB
    schema_fumble = args.arm == "V" and (
        not verb_adopted or failures > 0 or len(arguments) != 1
    )
    completed = exact and exit_code == 0 and final_message == "RENAME_OK"
    command_calls = sum(item.get("type") == "command_execution" for item in started)
    file_changes = sum(item.get("type") == "file_change" for item in started)
    one_shot = (
        completed
        and len(arguments) == 1
        and failures == 0
        and command_calls == 0
        and file_changes == 0
    )

    return {
        "schema": "clj-surgeon.rename-verb-run-score.v1",
        "run_id": args.run_id,
        "cohort": args.cohort,
        "arm": args.arm,
        "replicate": int(args.replicate),
        "model": args.model,
        "exit_code": exit_code,
        "wall_ms": int(args.wall_ms),
        "turns": len(turns),
        "mcp_calls": len(arguments),
        "mcp_failures": failures,
        "command_calls": command_calls,
        "file_changes": file_changes,
        "request_bytes": sum(item["bytes"] for item in arguments),
        "request_o200k_tokens": sum(item["o200k_tokens"] for item in arguments),
        "output_tokens": usage.get("output_tokens", 0),
        "reasoning_output_tokens": usage.get("reasoning_output_tokens", 0),
        "input_tokens": usage.get("input_tokens", 0),
        "cached_input_tokens": usage.get("cached_input_tokens", 0),
        "exact": exact,
        "completed": completed,
        "one_shot": one_shot,
        "wrong_subject": 0 if exact else 1,
        "verb_adopted": verb_adopted,
        "schema_fumble": schema_fumble,
        "old_symbol_count": old_symbol_count,
        "new_symbol_count": new_symbol_count,
        "final_message": final_message,
        "unexpected_paths": unexpected_paths,
        "missing_paths": missing_paths,
        "mismatched_paths": mismatched_paths,
        "final_manifest": final_manifest,
        "expected_manifest": expected_manifest,
        "tokenizer": {
            "name": "o200k_base",
            "package": "tiktoken",
            "version": importlib.metadata.version("tiktoken"),
            "classification": "request-token estimate, not billing usage",
        },
    }


def arm_summary(runs: list[dict], arm: str) -> dict:
    selected = [run for run in runs if run["cohort"] == "sol" and run["arm"] == arm]
    completed = [run for run in selected if run["completed"]]

    def median(field: str):
        values = [run[field] for run in completed]
        return statistics.median(values) if values else None

    return {
        "n": len(selected),
        "completed": len(completed),
        "exact": sum(run["exact"] for run in selected),
        "one_shot": sum(run["one_shot"] for run in selected),
        "wrong_subject": sum(run["wrong_subject"] for run in selected),
        "verb_adopted": sum(run["verb_adopted"] for run in selected),
        "schema_fumbles": sum(run["schema_fumble"] for run in selected),
        "median_request_bytes_completed": median("request_bytes"),
        "median_request_o200k_tokens_completed": median("request_o200k_tokens"),
        "median_output_tokens_completed": median("output_tokens"),
        "median_wall_ms_completed": median("wall_ms"),
        "runs": [run["run_id"] for run in selected],
    }


def percentage_reduction(control, verb):
    if control in (None, 0) or verb is None:
        return None
    return 100.0 * (control - verb) / control


def aggregate(args: argparse.Namespace) -> dict:
    runs = [
        json.loads(line)
        for line in Path(args.runs).read_text().splitlines()
        if line.strip()
    ]
    verb = arm_summary(runs, "V")
    control = arm_summary(runs, "T")
    byte_reduction = percentage_reduction(
        control["median_request_bytes_completed"],
        verb["median_request_bytes_completed"],
    )
    token_reduction = percentage_reduction(
        control["median_request_o200k_tokens_completed"],
        verb["median_request_o200k_tokens_completed"],
    )
    v_rate = verb["exact"] / verb["n"] if verb["n"] else 0
    t_rate = control["exact"] / control["n"] if control["n"] else 0
    correctness_loss = v_rate < t_rate
    wrong_subject = verb["wrong_subject"] + control["wrong_subject"]
    below_gate = byte_reduction is None or byte_reduction < 50.0
    killed = correctness_loss or wrong_subject > 0 or below_gate
    bonus = [run for run in runs if run["cohort"] == "spark"]
    return {
        "schema": "clj-surgeon.rename-verb-screen-summary.v1",
        "arms": {"V": verb, "T": control},
        "observed": {
            "median_request_byte_reduction_percent": byte_reduction,
            "median_request_o200k_token_reduction_percent": token_reduction,
            "point_prediction_percent": 90.0,
            "point_prediction_met_bytes": byte_reduction is not None
            and byte_reduction >= 90.0,
            "equal_correctness": verb["exact"] == control["exact"],
            "correctness_rate_V": v_rate,
            "correctness_rate_T": t_rate,
        },
        "kill_rules": {
            "correctness_loss": correctness_loss,
            "any_wrong_subject": wrong_subject > 0,
            "below_50_percent_byte_reduction": below_gate,
        },
        "decision": "KILL" if killed else "PASS_SCREEN",
        "spark_bonus": {
            "n": len(bonus),
            "exact": sum(run["exact"] for run in bonus),
            "verb_adopted": sum(run["verb_adopted"] for run in bonus),
            "schema_fumbles": sum(run["schema_fumble"] for run in bonus),
            "runs": [run["run_id"] for run in bonus],
        },
    }


def main() -> None:
    parser = argparse.ArgumentParser()
    subparsers = parser.add_subparsers(dest="command", required=True)

    score_parser = subparsers.add_parser("score")
    score_parser.add_argument("--events", required=True)
    score_parser.add_argument("--workspace", required=True)
    score_parser.add_argument("--expected", required=True)
    score_parser.add_argument("--request-output", required=True)
    score_parser.add_argument("--run-id", required=True)
    score_parser.add_argument("--cohort", choices=["sol", "spark"], required=True)
    score_parser.add_argument("--arm", choices=["V", "T"], required=True)
    score_parser.add_argument("--replicate", required=True)
    score_parser.add_argument("--model", required=True)
    score_parser.add_argument("--exit-code", required=True)
    score_parser.add_argument("--wall-ms", required=True)

    aggregate_parser = subparsers.add_parser("aggregate")
    aggregate_parser.add_argument("--runs", required=True)

    args = parser.parse_args()
    result = score(args) if args.command == "score" else aggregate(args)
    print(json.dumps(result, indent=2, sort_keys=True))


if __name__ == "__main__":
    main()

