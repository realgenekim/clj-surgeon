#!/usr/bin/env python3
"""Score the frozen W1 product-shaped cohort from timestamped Codex JSONL."""

from __future__ import annotations

import argparse
import csv
import hashlib
import json
import statistics
from pathlib import Path

import tiktoken


def canonical_json(value: object) -> str:
    return json.dumps(value, ensure_ascii=False, separators=(",", ":"))


def structured(item: dict) -> dict:
    result = item.get("result") or {}
    return result.get("structured_content") or result.get("structuredContent") or {}


def read_timed_events(path: Path) -> list[tuple[float, dict]]:
    events = []
    for line in path.read_text().splitlines():
        stamp, payload = line.split("\t", 1)
        events.append((float(stamp), json.loads(payload)))
    return events


def sha256_tree(root: Path) -> str:
    digest = hashlib.sha256()
    for path in sorted(p for p in root.rglob("*") if p.is_file()):
        relative = path.relative_to(root).as_posix().encode()
        digest.update(len(relative).to_bytes(8, "big"))
        digest.update(relative)
        data = path.read_bytes()
        digest.update(len(data).to_bytes(8, "big"))
        digest.update(data)
    return digest.hexdigest()


def median(values: list[float]) -> float | None:
    return statistics.median(values) if values else None


def completed_true(receipt: dict) -> bool:
    return receipt.get("committed") is True and receipt.get("verification_complete") is True


def score_run(run_dir: Path, encoding) -> dict:
    meta = json.loads((run_dir / "meta.json").read_text())
    events = read_timed_events(run_dir / "events.timed.jsonl")
    started: list[dict] = []
    completed_by_id: dict[str, tuple[float, dict]] = {}
    command_count = 0
    file_change_count = 0
    for stamp, event in events:
        item = event.get("item") or {}
        if event.get("type") == "item.started" and item.get("type") == "mcp_tool_call":
            started.append({"stamp": stamp, "item": item})
        elif event.get("type") == "item.completed" and item.get("type") == "mcp_tool_call":
            completed_by_id[item.get("id", "")] = (stamp, item)
        elif event.get("type") == "item.started" and item.get("type") == "command_execution":
            command_count += 1
        elif event.get("type") == "item.started" and item.get("type") == "file_change":
            file_change_count += 1

    calls = []
    for call in started:
        item = call["item"]
        completion = completed_by_id.get(item.get("id", ""))
        end_stamp, completed_item = completion if completion else (None, {})
        args = item.get("arguments") or {}
        payload = canonical_json(args)
        calls.append({
            "tool": item.get("tool"),
            "args": args,
            "start": call["stamp"],
            "end": end_stamp,
            "wall_ms": None if end_stamp is None else (end_stamp - call["stamp"]) * 1000.0,
            "bytes": len(payload.encode()),
            "tokens": len(encoding.encode(payload)),
            "completed": completed_item,
            "receipt": structured(completed_item),
        })

    for index, call in enumerate(calls[:-1]):
        call["next_gap_ms"] = None if call["end"] is None else (calls[index + 1]["start"] - call["end"]) * 1000.0
    if calls:
        calls[-1]["next_gap_ms"] = None

    arm = meta["arm"]
    expected_tools = ["inspect_clojure", "edit_clojure", "edit_clojure"] if arm == "C" else ["inspect_clojure", "edit_clojure"]
    tool_sequence_ok = [call["tool"] for call in calls] == expected_tools
    read_args = calls[0]["args"] if calls else {}
    expected_read = {
        "workspace_root": meta["workspace"],
        "requests": [{"file": "src/bench/pair_view.clj", "forms": ["route-event"], "expect": {"forms": 1}}],
        "expect": {"requests": 1, "files": 1},
    }
    read_subject_ok = bool(
        read_args.get("workspace_root") == expected_read["workspace_root"]
        and read_args.get("expect") == expected_read["expect"]
        and len(read_args.get("requests") or []) == 1
        and read_args["requests"][0].get("file") == "src/bench/pair_view.clj"
        and read_args["requests"][0].get("forms") == ["route-event"]
        and read_args["requests"][0].get("expect") == {"forms": 1}
        and read_args["requests"][0].get("operation") in (None, "forms")
    )

    edit_calls = [call for call in calls if call["tool"] == "edit_clojure"]
    preview_calls = [call for call in edit_calls if call["args"].get("preview") is True]
    commit_calls = [call for call in edit_calls if "preview" not in call["args"]]
    preview_call = preview_calls[0] if arm == "C" and preview_calls else None
    commit_call = commit_calls[-1] if commit_calls else None
    if arm == "C":
        preview_shape_ok = bool(
            preview_call
            and set(preview_call["args"]) == {"confirm", "fill", "preview"}
            and preview_call["args"].get("preview") is True
            and set(preview_call["args"].get("fill", {})) == {"arguments.edits[0].to"}
        )
        commit_shape_ok = bool(
            commit_call
            and set(commit_call["args"]) == {"confirm", "fill"}
            and preview_call
            and commit_call["args"] == {k: preview_call["args"][k] for k in ("confirm", "fill")}
        )
        preview_ok = bool(
            preview_call
            and preview_call["receipt"].get("operation") == "edit_clojure-preview"
            and preview_call["receipt"].get("source_unchanged") is True
        )
        ordinary_subject_ok = True
    else:
        preview_shape_ok = True
        preview_ok = True
        edit_args = commit_call["args"] if commit_call else {}
        edits = edit_args.get("edits") or []
        commit_shape_ok = bool(
            set(edit_args) == {"workspace_root", "edits"}
            and edit_args.get("workspace_root") == meta["workspace"]
            and len(edits) == 1
            and edits[0].get("file") == "src/bench/pair_view.clj"
            and edits[0].get("within") == {"form": "route-event"}
            and edits[0].get("expect") == {"matches": 1}
            and isinstance(edits[0].get("from"), str)
            and isinstance(edits[0].get("to"), str)
        )
        ordinary_subject_ok = commit_shape_ok

    commit_ok = bool(commit_call and completed_true(commit_call["receipt"]))
    tree_exact = sha256_tree(Path(meta["workspace"])) == meta["expected_tree_sha256"]
    no_other_actions = command_count == 0 and file_change_count == 0
    route_adherent = bool(
        tool_sequence_ok and read_subject_ok and preview_shape_ok and commit_shape_ok
        and preview_ok and commit_ok and no_other_actions
    )
    wrong_subject = 0 if read_subject_ok and ordinary_subject_ok else 1
    exact = bool(tree_exact and commit_ok and route_adherent and wrong_subject == 0)

    usage = {}
    user_turns = 0
    for _, event in events:
        if event.get("type") == "turn.completed":
            user_turns += 1
            usage = event.get("usage") or usage

    read_call = calls[0] if calls else {}
    mutation_calls = edit_calls
    preview_changed_commit = bool(
        arm == "C" and preview_ok and preview_call and commit_call
        and preview_call["args"].get("fill") != commit_call["args"].get("fill")
    )

    return {
        "run_id": meta["run_id"],
        "ordinal": meta["ordinal"],
        "pair": meta["pair"],
        "arm": arm,
        "exit_code": meta["exit_code"],
        "episode_wall_ms": meta["episode_wall_ms"],
        "read_wall_ms": read_call.get("wall_ms"),
        "read_to_next_ms": read_call.get("next_gap_ms"),
        "preview_wall_ms": preview_call.get("wall_ms") if preview_call else None,
        "preview_to_commit_ms": preview_call.get("next_gap_ms") if preview_call else None,
        "commit_wall_ms": commit_call.get("wall_ms") if commit_call else None,
        "caller_bytes": sum(call["bytes"] for call in calls),
        "caller_tokens_o200k": sum(call["tokens"] for call in calls),
        "mutation_bytes": sum(call["bytes"] for call in mutation_calls),
        "mutation_tokens_o200k": sum(call["tokens"] for call in mutation_calls),
        "read_bytes": read_call.get("bytes", 0),
        "read_tokens_o200k": read_call.get("tokens", 0),
        "preview_bytes": preview_call.get("bytes", 0) if preview_call else 0,
        "preview_tokens_o200k": preview_call.get("tokens", 0) if preview_call else 0,
        "commit_bytes": commit_call.get("bytes", 0) if commit_call else 0,
        "commit_tokens_o200k": commit_call.get("tokens", 0) if commit_call else 0,
        "user_turns": user_turns,
        "mcp_calls": len(calls),
        "model_input_tokens": usage.get("input_tokens", 0),
        "model_cached_input_tokens": usage.get("cached_input_tokens", 0),
        "model_output_tokens": usage.get("output_tokens", 0),
        "tree_exact": tree_exact,
        "route_adherent": route_adherent,
        "exact": exact,
        "wrong_subject": wrong_subject,
        "preview_changed_commit": preview_changed_commit,
        "tool_sequence": ",".join(call["tool"] or "" for call in calls),
    }


def summarize(rows: list[dict]) -> dict:
    metrics = [
        "episode_wall_ms", "read_wall_ms", "read_to_next_ms", "preview_wall_ms",
        "preview_to_commit_ms", "commit_wall_ms", "caller_bytes",
        "caller_tokens_o200k", "mutation_bytes", "mutation_tokens_o200k",
        "read_bytes", "read_tokens_o200k", "preview_bytes", "preview_tokens_o200k",
        "commit_bytes", "commit_tokens_o200k", "user_turns", "mcp_calls",
        "model_input_tokens", "model_cached_input_tokens", "model_output_tokens",
    ]
    arms = {}
    for arm in ("C", "O"):
        arm_rows = [row for row in rows if row["arm"] == arm]
        medians = {}
        raw = {}
        for metric in metrics:
            values = [row[metric] for row in arm_rows if row[metric] is not None]
            medians[metric] = median(values)
            raw[metric] = values
        arms[arm] = {
            "n": len(arm_rows),
            "exact": sum(row["exact"] for row in arm_rows),
            "route_adherent": sum(row["route_adherent"] for row in arm_rows),
            "wrong_subject": sum(row["wrong_subject"] for row in arm_rows),
            "medians": medians,
            "raw": raw,
        }

    deltas = {}
    for metric in metrics:
        c_value = arms["C"]["medians"][metric]
        o_value = arms["O"]["medians"][metric]
        if c_value is not None and o_value is not None:
            deltas[metric] = {
                "C_minus_O": c_value - o_value,
                "percent_of_O": None if o_value == 0 else (c_value - o_value) / o_value * 100.0,
            }

    paired = {}
    for metric in ("episode_wall_ms", "caller_bytes", "caller_tokens_o200k", "mutation_bytes", "mutation_tokens_o200k"):
        values = []
        for pair in range(1, 9):
            c_row = next(row for row in rows if row["pair"] == pair and row["arm"] == "C")
            o_row = next(row for row in rows if row["pair"] == pair and row["arm"] == "O")
            values.append(c_row[metric] - o_row[metric])
        paired[metric] = {"median_C_minus_O": median(values), "raw_C_minus_O": values}

    exact_equal = arms["C"]["exact"] == arms["O"]["exact"] == 8
    any_preview_adjustment = any(row["preview_changed_commit"] for row in rows if row["arm"] == "C")
    any_error = any(not row["exact"] for row in rows)
    if not any_error and not any_preview_adjustment:
        preview_value = "UNOBSERVED in this cohort: neither arm erred and no preview changed a subsequent commit."
    elif any_preview_adjustment:
        preview_value = "OBSERVED candidate catch: at least one C fill changed after preview; inspect raw evidence before causal attribution."
    else:
        preview_value = "UNOBSERVED: errors occurred, but no preview-to-commit correction was recorded."

    return {
        "schema": "clj-surgeon.w1-product-cohort-summary/v1",
        "arms": arms,
        "deltas": deltas,
        "paired_deltas": paired,
        "exactness_equal_and_complete": exact_equal,
        "preview_value": preview_value,
    }


def write_tsv(path: Path, rows: list[dict]) -> None:
    fields = list(rows[0])
    with path.open("w", newline="") as handle:
        writer = csv.DictWriter(handle, fieldnames=fields, delimiter="\t")
        writer.writeheader()
        writer.writerows(rows)


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("result_dir", type=Path)
    args = parser.parse_args()
    encoding = tiktoken.get_encoding("o200k_base")
    run_dirs = sorted(path for path in args.result_dir.glob("[0-9][0-9]-*") if path.is_dir())
    rows = [score_run(path, encoding) for path in run_dirs]
    if len(rows) != 16:
        raise SystemExit(f"expected 16 episodes, found {len(rows)}")
    write_tsv(args.result_dir / "episodes.tsv", rows)
    summary = summarize(rows)
    (args.result_dir / "summary.json").write_text(json.dumps(summary, indent=2) + "\n")
    print(json.dumps(summary, indent=2))


if __name__ == "__main__":
    main()
