#!/usr/bin/env python3
"""Score complete caller-turn wall for the frozen three-way acid battery."""

from __future__ import annotations

import csv
import hashlib
import json
import math
import re
import statistics
import sys
from pathlib import Path
from typing import Any


ACTION_TYPES = {"mcp_tool_call", "command_execution", "file_change"}
T_CRITICAL_95 = {2: 12.706204736, 3: 4.302652730, 4: 3.182446305, 5: 2.776445105}
HISTORIC_NATIVE_MS = {"promoted_median": 122278.0, "readme_headline_pair": 207898.0}


def canonical_json(value: Any) -> str:
    return json.dumps(value, ensure_ascii=False, separators=(",", ":"), sort_keys=True)


def tree_sha256(root: Path) -> str:
    digest = hashlib.sha256()
    for path in sorted(p for p in root.rglob("*") if p.is_file()):
        relative = path.relative_to(root).as_posix().encode()
        data = path.read_bytes()
        digest.update(len(relative).to_bytes(8, "big")); digest.update(relative)
        digest.update(len(data).to_bytes(8, "big")); digest.update(data)
    return digest.hexdigest()


def read_timed(path: Path) -> list[tuple[float, dict[str, Any]]]:
    rows = []
    for line in path.read_text().splitlines():
        stamp, payload = line.split("\t", 1)
        rows.append((float(stamp), json.loads(payload)))
    return rows


def structured(item: dict[str, Any]) -> dict[str, Any]:
    result = item.get("result") or {}
    return result.get("structured_content") or result.get("structuredContent") or {}


def allowed_files(task_class: str) -> set[str]:
    if task_class == "fill":
        return {"src/bench/app_shell.clj"}
    if task_class in {"wall", "repair"}:
        return {"src/bench/pair_view.clj"}
    return {
        "src/cfp_scheduler_killer/views.clj",
        "src/cfp_scheduler_killer/views/format.clj",
    }


def explicit_files(value: Any) -> list[str]:
    found: list[str] = []
    if isinstance(value, dict):
        for key, child in value.items():
            if key in {"file", "source_file", "destination_file"} and isinstance(child, str):
                found.append(child)
            found.extend(explicit_files(child))
    elif isinstance(value, list):
        for child in value:
            found.extend(explicit_files(child))
    return found


def server_wall_ms(run_dir: Path) -> float:
    total = 0.0
    for path in (run_dir / "mcp-telemetry").glob("*.jsonl"):
        for line in path.read_text().splitlines():
            row = json.loads(line)
            if row.get("event") == "tool.call":
                total += float((row.get("timings_ms") or {}).get("total_ms") or 0.0)
    return total


def score_run(run_dir: Path) -> dict[str, Any]:
    meta = json.loads((run_dir / "meta.json").read_text())
    events = read_timed(run_dir / "events.timed.jsonl")
    started: dict[str, tuple[float, dict[str, Any]]] = {}
    actions: list[dict[str, Any]] = []
    usage: dict[str, Any] = {}
    for stamp, event in events:
        item = event.get("item") or {}
        item_id = item.get("id") or f"anonymous-{len(started)}"
        if event.get("type") == "item.started" and item.get("type") in ACTION_TYPES:
            started[item_id] = (stamp, item)
        elif event.get("type") == "item.completed" and item.get("type") in ACTION_TYPES:
            begin = started.pop(item_id, None)
            start_stamp, start_item = begin if begin else (stamp, item)
            actions.append({
                "type": item.get("type"),
                "tool": item.get("tool"),
                "start": start_stamp,
                "end": stamp,
                "wall_ms": max(0.0, (stamp - start_stamp) * 1000.0),
                "arguments": start_item.get("arguments") or item.get("arguments") or {},
                "completed": item,
                "receipt": structured(item),
                "status": item.get("status"),
            })
        elif event.get("type") == "turn.completed":
            usage = event.get("usage") or usage

    action_wall = sum(action["wall_ms"] for action in actions)
    server_wall = server_wall_ms(run_dir) if meta["arm"] != "C" else 0.0
    transport_or_local = max(0.0, action_wall - server_wall)
    caller_wall = max(0.0, float(meta["episode_wall_ms"]) - action_wall)
    expected = meta["expected_tree_sha256"]
    actual = tree_sha256(Path(meta["workspace"]))
    exact_tree = actual == expected

    allowed = allowed_files(meta["class"])
    wrong_files: list[str] = []
    for action in actions:
        for file in explicit_files(action["arguments"]):
            if file not in allowed:
                wrong_files.append(file)
        if action["type"] == "command_execution":
            command = str(action["completed"].get("command") or action["arguments"].get("cmd") or "")
            for file in re.findall(r"(?:src|test)/[A-Za-z0-9_./-]+\.(?:clj|cljc|cljs)", command):
                if file not in allowed:
                    wrong_files.append(file)

    mcp = [action for action in actions if action["type"] == "mcp_tool_call"]
    refused = [action for action in mcp if action["receipt"].get("ok") is False]
    completed_receipts = [
        action for action in mcp
        if action["receipt"].get("committed") is True
        and action["receipt"].get("verification_complete") is True
    ]
    proxy_rows = []
    proxy_path = run_dir / "proxy.jsonl"
    if proxy_path.exists():
        proxy_rows = [json.loads(line) for line in proxy_path.read_text().splitlines() if line.strip()]
    held_session = any(row.get("event") == "proxy_ready" and row.get("held_session") is True for row in proxy_rows)
    confirmation_used = any(set(action["arguments"]) <= {"confirm", "fill"} and "confirm" in action["arguments"] for action in mcp)
    first_refusal_type = refused[0]["receipt"].get("error_type") if refused else None
    outcome_exact = bool(meta["exit_code"] == 0 and exact_tree and not wrong_files)
    if meta["arm"] in {"A", "B"}:
        outcome_exact = bool(outcome_exact and completed_receipts)

    return {
        "run_id": meta["run_id"],
        "ordinal": meta["ordinal"],
        "class": meta["class"],
        "pair": meta["pair"],
        "position": meta["position"],
        "arm": meta["arm"],
        "product_commit": meta["product_commit"],
        "exit_code": meta["exit_code"],
        "episode_wall_ms": float(meta["episode_wall_ms"]),
        "caller_emission_ms": caller_wall,
        "transport_or_local_tool_ms": transport_or_local,
        "server_ms": server_wall,
        "split_residual_ms": float(meta["episode_wall_ms"]) - caller_wall - transport_or_local - server_wall,
        "action_count": len(actions),
        "mcp_calls": len(mcp),
        "refusal_count": len(refused),
        "first_refusal_type": first_refusal_type,
        "caller_argument_bytes": sum(len(canonical_json(action["arguments"]).encode()) for action in mcp),
        "model_output_tokens": usage.get("output_tokens", 0),
        "model_reasoning_tokens": usage.get("reasoning_output_tokens", 0),
        "exact_tree": exact_tree,
        "outcome_exact": outcome_exact,
        "wrong_subject": int(bool(wrong_files)),
        "wrong_files": ",".join(sorted(set(wrong_files))),
        "held_session": held_session if meta["arm"] != "C" else None,
        "confirmation_used": confirmation_used,
        "tool_sequence": ",".join(str(action["tool"] or action["type"]) for action in actions),
    }


def median(values: list[float]) -> float | None:
    return statistics.median(values) if values else None


def log_t_ci(ratios: list[float]) -> dict[str, Any]:
    if len(ratios) < 2 or any(value <= 0 for value in ratios):
        return {"n": len(ratios), "geometric_mean": None, "lower_95": None, "upper_95": None}
    logs = [math.log(value) for value in ratios]
    mean = statistics.mean(logs)
    standard_error = statistics.stdev(logs) / math.sqrt(len(logs))
    critical = T_CRITICAL_95[len(logs)]
    return {
        "n": len(ratios),
        "geometric_mean": math.exp(mean),
        "lower_95": math.exp(mean - critical * standard_error),
        "upper_95": math.exp(mean + critical * standard_error),
        "method": "paired log-ratio Student-t 95% CI",
        "raw_ratios": ratios,
    }


def summarize(rows: list[dict[str, Any]]) -> dict[str, Any]:
    classes: dict[str, Any] = {}
    for task_class in ("fill", "wall", "repair", "flagship"):
        class_rows = [row for row in rows if row["class"] == task_class]
        arms: dict[str, Any] = {}
        for arm in ("A", "B", "C"):
            arm_rows = [row for row in class_rows if row["arm"] == arm]
            walls = [row["episode_wall_ms"] for row in arm_rows]
            arms[arm] = {
                "n": len(arm_rows),
                "outcome_exact": sum(row["outcome_exact"] for row in arm_rows),
                "wrong_subject": sum(row["wrong_subject"] for row in arm_rows),
                "median_wall_ms": median(walls),
                "wall_range_ms": [min(walls), max(walls)] if walls else None,
                "raw_wall_ms": walls,
                "median_caller_emission_ms": median([row["caller_emission_ms"] for row in arm_rows]),
                "median_transport_or_local_tool_ms": median([row["transport_or_local_tool_ms"] for row in arm_rows]),
                "median_server_ms": median([row["server_ms"] for row in arm_rows]),
                "median_argument_bytes": median([row["caller_argument_bytes"] for row in arm_rows]),
                "median_output_tokens": median([row["model_output_tokens"] for row in arm_rows]),
                "median_actions": median([row["action_count"] for row in arm_rows]),
                "first_refusal_types": [row["first_refusal_type"] for row in arm_rows],
                "held_session": all(row["held_session"] is True for row in arm_rows) if arm != "C" else None,
            }
        comparisons: dict[str, Any] = {}
        for other in ("B", "C"):
            ratios = []
            for pair in sorted({row["pair"] for row in class_rows}):
                a = next((row for row in class_rows if row["pair"] == pair and row["arm"] == "A"), None)
                o = next((row for row in class_rows if row["pair"] == pair and row["arm"] == other), None)
                if a and o:
                    ratios.append(o["episode_wall_ms"] / a["episode_wall_ms"])
            comparisons[f"A_vs_{other}"] = {
                "median_paired_speedup": median(ratios),
                "ratio_of_arm_medians": None if not arms["A"]["median_wall_ms"] else arms[other]["median_wall_ms"] / arms["A"]["median_wall_ms"],
                "confidence_interval": log_t_ci(ratios),
            }
        classes[task_class] = {"arms": arms, "comparisons": comparisons}

    all_exact = all(row["outcome_exact"] for row in rows)
    wrong_subject_total = sum(row["wrong_subject"] for row in rows)
    return {
        "schema": "clj-surgeon.threeway-acid-wall-summary/v1",
        "headline_metric": "complete caller turn wall: intent stated through verified commit receipt and caller completion",
        "split_definition": {
            "caller_emission_ms": "episode wall outside recorded tool intervals; includes inference, request emission, receipt interpretation, and final response",
            "transport_or_local_tool_ms": "client-observed action wall minus product server wall; native local tool execution is reported here",
            "server_ms": "sum of product tool.call total_ms telemetry; zero for native",
        },
        "classes": classes,
        "all_episodes_exact": all_exact,
        "wrong_subject_total": wrong_subject_total,
        "historic_flagship_native_ms": HISTORIC_NATIVE_MS,
    }


def write_tsv(path: Path, rows: list[dict[str, Any]]) -> None:
    fields = list(rows[0])
    with path.open("w", newline="") as handle:
        writer = csv.DictWriter(handle, fieldnames=fields, delimiter="\t")
        writer.writeheader(); writer.writerows(rows)


def main() -> None:
    if len(sys.argv) != 2:
        raise SystemExit("usage: score_threeway_acid_wall_battery.py RESULT_DIR")
    result_dir = Path(sys.argv[1])
    run_dirs = sorted(path for path in (result_dir / "runs").iterdir() if path.is_dir())
    if len(run_dirs) != 42:
        raise SystemExit(f"expected 42 run directories, found {len(run_dirs)}")
    rows = [score_run(path) for path in run_dirs]
    write_tsv(result_dir / "episodes.tsv", rows)
    summary = summarize(rows)
    (result_dir / "summary.json").write_text(json.dumps(summary, indent=2) + "\n")
    print(json.dumps(summary, indent=2))


if __name__ == "__main__":
    main()
