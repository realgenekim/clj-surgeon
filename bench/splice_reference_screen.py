#!/usr/bin/env python3
"""Scoring and receipt helpers for adversarial splice-reference replication."""

from __future__ import annotations

import hashlib
import json
from pathlib import Path
import statistics
from typing import Any

import tiktoken

from splice_reference_proxy import canonical_json, sha256_file


TOKENIZER_NAME = "o200k_base"


def request_metrics(arguments: dict[str, Any]) -> dict[str, int]:
    rendered = canonical_json(arguments)
    encoding = tiktoken.get_encoding(TOKENIZER_NAME)
    return {
        "utf8_bytes": len(rendered.encode("utf-8")),
        "tokens": len(encoding.encode(rendered)),
    }


def load_jsonl(path: Path) -> list[dict[str, Any]]:
    if not path.is_file():
        return []
    return [json.loads(line) for line in path.read_text(encoding="utf-8").splitlines() if line]


def ideal_requests(manifest: dict[str, Any]) -> dict[str, Any]:
    targets = [spec for spec in manifest["candidates"] if "to" in spec]
    conventional = {
        "edits": [
            {
                "file": manifest["file"],
                "within": spec["within"],
                "from": spec["from"],
                "to": spec["to"],
                "matches": 1,
            }
            for spec in targets
        ]
    }
    reference = {
        "edits": [
            {
                "file": manifest["file"],
                "within": spec["within"],
                "from_ref": spec["label"],
                "to": spec["to"],
                "matches": 1,
            }
            for spec in targets
        ]
    }
    q = request_metrics(conventional)
    r = request_metrics(reference)
    return {
        "Q": {"arguments": conventional, **q},
        "R": {"arguments": reference, **r},
        "possible_reduction": {
            "utf8_bytes": 1 - r["utf8_bytes"] / q["utf8_bytes"],
            "tokens": 1 - r["tokens"] / q["tokens"],
        },
    }


def count_codex_actions(events: list[dict[str, Any]], item_type: str) -> int:
    return sum(
        1 for event in events
        if event.get("type") == "item.started"
        and isinstance(event.get("item"), dict)
        and event["item"].get("type") == item_type
    )


def mutation_request_rows(receipts: list[dict[str, Any]]) -> list[dict[str, Any]]:
    """Return every model-emitted edit attempt, including proxy refusals."""
    return [
        row for row in receipts
        if (
            row.get("event") == "tool-request" and row.get("tool") == "edit_clojure"
        ) or row.get("event") == "reference-refusal"
    ]


def score_episode(run_dir: Path, arm: str, manifest: dict[str, Any],
                  expected_file: Path) -> dict[str, Any]:
    receipts = load_jsonl(run_dir / "proxy-receipts.jsonl")
    events = load_jsonl(run_dir / "events.jsonl")
    episode = json.loads((run_dir / "episode.json").read_text(encoding="utf-8"))
    tool_requests = [row for row in receipts if row.get("event") == "tool-request"]
    edit_requests = mutation_request_rows(receipts)
    inspect_requests = [row for row in tool_requests if row.get("tool") == "inspect_clojure"]
    edit_metrics = [request_metrics(row.get("model_arguments", {})) for row in edit_requests]
    refused_requests = [row for row in receipts if row.get("event") == "reference-refusal"]
    all_metrics = [
        request_metrics(row.get("model_arguments", {}))
        for row in tool_requests + refused_requests
    ]
    emitted = {
        "mutation_utf8_bytes": sum(metric["utf8_bytes"] for metric in edit_metrics),
        "mutation_tokens": sum(metric["tokens"] for metric in edit_metrics),
        "all_mcp_utf8_bytes": sum(metric["utf8_bytes"] for metric in all_metrics),
        "all_mcp_tokens": sum(metric["tokens"] for metric in all_metrics),
    }
    refs = 0
    quoted = 0
    strict_reference_calls = 0
    conventional_calls = 0
    for row in edit_requests:
        edits = row.get("model_arguments", {}).get("edits", [])
        if not isinstance(edits, list):
            continue
        refs += sum(isinstance(edit, dict) and "from_ref" in edit for edit in edits)
        quoted += sum(
            isinstance(edit, dict) and any(key in edit for key in ["from", "old", "before"])
            for edit in edits
        )
        if len(edits) == 4 and all(isinstance(edit, dict) and "from_ref" in edit
                                   and not any(key in edit for key in ["from", "old", "before"])
                                   for edit in edits):
            strict_reference_calls += 1
        if len(edits) == 4 and all(isinstance(edit, dict)
                                   and any(key in edit for key in ["from", "old", "before"])
                                   and "from_ref" not in edit for edit in edits):
            conventional_calls += 1
    wrong_subject_rows = [
        row for row in receipts
        if row.get("event") == "reference-refusal"
        and row.get("refusal", {}).get("wrong_subject") is True
    ]
    identity_audits = [
        resolution
        for row in receipts
        if row.get("event") in {"reference-refusal", "edit-result"}
        for resolution in (
            row.get("resolutions", [])
            if row.get("event") == "reference-refusal"
            else row.get("resolved_references", [])
        )
        if isinstance(resolution, dict) and "identity_match" in resolution
    ]
    wrong_identity_audits = [row for row in identity_audits if row["identity_match"] is False]
    verified_readbacks = [
        row for row in identity_audits
        if row.get("readback_present") is True and row.get("readback_match") is True
    ]
    blind_references = [row for row in identity_audits if row.get("readback_present") is False]
    incorrect_readbacks = [
        row for row in identity_audits
        if row.get("readback_present") is True and row.get("readback_match") is False
    ]
    typed_reference_failures = [row for row in receipts if row.get("event") == "reference-refusal"]
    annotations = [row for row in receipts if row.get("event") == "read-annotation"]
    edit_results = [row for row in receipts if row.get("event") == "edit-result"]
    resolved_count = sum(len(row.get("resolved_references", [])) for row in edit_results)
    product_verified = any(
        row.get("product_result", {}).get("verification_complete") is True
        for row in edit_results
    )
    actual_file = run_dir / "workspace" / manifest["file"]
    expected_sha = sha256_file(expected_file)
    actual_sha = sha256_file(actual_file) if actual_file.is_file() else None
    exact = actual_sha == expected_sha
    shell_calls = count_codex_actions(events, "command_execution")
    file_change_calls = count_codex_actions(events, "file_change")
    usage = next(
        (event.get("usage", {}) for event in reversed(events)
         if event.get("type") == "turn.completed"),
        {},
    )
    environment_valid = episode.get("codex_exit_code") == 0 and bool(events)
    route_adherent = shell_calls == 0 and file_change_calls == 0
    completed = environment_valid and route_adherent and exact and product_verified
    return {
        "schema": "clj-surgeon.splice-reference-adversarial-episode-score.v1",
        "run_id": run_dir.name,
        "arm": arm,
        "environment_valid": environment_valid,
        "semantic_correct": exact,
        "exact_bytes": exact,
        "route_adherent": route_adherent,
        "completed_task": completed,
        "product_verification_complete": product_verified,
        "wrong_subject": max(len(wrong_subject_rows), len(wrong_identity_audits)),
        "typed_reference_failures": len(typed_reference_failures),
        "reference_count": refs,
        "quoted_anchor_count": quoted,
        "strict_reference_call_count": strict_reference_calls,
        "conventional_call_count": conventional_calls,
        "reference_used_strict": strict_reference_calls > 0 and quoted == 0,
        "reference_used_any": refs > 0,
        "resolved_identity_count": resolved_count,
        "resolved_identity_audit_count": len(identity_audits),
        "verified_readback_reference_count": len(verified_readbacks),
        "blind_reference_count": len(blind_references),
        "incorrect_readback_count": len(incorrect_readbacks),
        "readback_behavior": (
            "verified" if identity_audits and len(verified_readbacks) == len(identity_audits)
            else "blind" if blind_references and not verified_readbacks
            else "mixed" if identity_audits
            else "not_applicable"
        ),
        "read_annotation_count": len(annotations),
        "turns": {
            "mcp_round_trips": len(tool_requests) + len(refused_requests),
            "inspect_calls": len(inspect_requests),
            "edit_calls": len(edit_requests),
            "shell_calls": shell_calls,
            "file_change_calls": file_change_calls,
        },
        "emitted": emitted,
        "usage": usage,
        "wall_seconds": episode.get("wall_seconds"),
        "expected_sha256": expected_sha,
        "actual_sha256": actual_sha,
    }


def median(values: list[int | float]) -> float | None:
    return statistics.median(values) if values else None


def summarize(scores: list[dict[str, Any]], expected_attempts: int) -> dict[str, Any]:
    arms = {arm: [score for score in scores if score["arm"] == arm] for arm in ["Q", "R"]}
    completed = {
        arm: [score for score in values if score["completed_task"]]
        for arm, values in arms.items()
    }
    stats: dict[str, Any] = {}
    for arm in ["Q", "R"]:
        stats[arm] = {
            "attempts": len(arms[arm]),
            "completed": len(completed[arm]),
            "exact": sum(score["exact_bytes"] for score in arms[arm]),
            "environment_valid": sum(score["environment_valid"] for score in arms[arm]),
            "route_adherent": sum(score["route_adherent"] for score in arms[arm]),
            "wrong_subject": sum(score["wrong_subject"] for score in arms[arm]),
            "typed_reference_failures": sum(score["typed_reference_failures"] for score in arms[arm]),
            "strict_reference_uses": sum(score["reference_used_strict"] for score in arms[arm]),
            "verified_readback_episodes": sum(
                score["readback_behavior"] == "verified" for score in arms[arm]
            ),
            "blind_fire_episodes": sum(
                score["readback_behavior"] == "blind" for score in arms[arm]
            ),
            "mixed_readback_episodes": sum(
                score["readback_behavior"] == "mixed" for score in arms[arm]
            ),
            "median_mutation_utf8_bytes": median([
                score["emitted"]["mutation_utf8_bytes"] for score in completed[arm]
            ]),
            "median_mutation_tokens": median([
                score["emitted"]["mutation_tokens"] for score in completed[arm]
            ]),
            "median_all_mcp_tokens": median([
                score["emitted"]["all_mcp_tokens"] for score in completed[arm]
            ]),
            "median_mcp_round_trips": median([
                score["turns"]["mcp_round_trips"] for score in completed[arm]
            ]),
        }
    q_tokens = stats["Q"]["median_mutation_tokens"]
    r_tokens = stats["R"]["median_mutation_tokens"]
    q_bytes = stats["Q"]["median_mutation_utf8_bytes"]
    r_bytes = stats["R"]["median_mutation_utf8_bytes"]
    reduction_tokens = None if not q_tokens or r_tokens is None else 1 - r_tokens / q_tokens
    reduction_bytes = None if not q_bytes or r_bytes is None else 1 - r_bytes / q_bytes
    wrong_subject = sum(score["wrong_subject"] for score in scores)
    reference_rate = (stats["R"]["strict_reference_uses"] / stats["R"]["attempts"]
                      if stats["R"]["attempts"] else 0.0)
    validity = all(
        stats[arm]["attempts"] == expected_attempts
        and stats[arm]["completed"] == expected_attempts
        for arm in ["Q", "R"]
    )
    kills = {
        "less_than_30_percent_token_reduction": reduction_tokens is None or reduction_tokens < 0.30,
        "any_wrong_subject": wrong_subject > 0,
        "R_exact_below_Q_parity": stats["R"]["exact"] < stats["Q"]["exact"],
        "strict_reference_adoption_below_6_of_8": stats["R"]["strict_reference_uses"] < 6,
    }
    return {
        "schema": "clj-surgeon.splice-reference-adversarial-summary.v1",
        "expected_attempts_per_arm": expected_attempts,
        "arms": stats,
        "primary": {
            "median_mutation_token_reduction": reduction_tokens,
            "median_mutation_utf8_byte_reduction": reduction_bytes,
            "counting_rule": "canonical JSON tool arguments; all edit_clojure attempts in each completed task",
            "tokenizer": TOKENIZER_NAME,
        },
        "secondary": {
            "wrong_subject_total": wrong_subject,
            "strict_reference_use_rate_R": reference_rate,
            "readback_behavior_R": {
                "verified_episodes": stats["R"]["verified_readback_episodes"],
                "blind_fire_episodes": stats["R"]["blind_fire_episodes"],
                "mixed_episodes": stats["R"]["mixed_readback_episodes"],
            },
        },
        "validity_gate_passed": validity,
        "kills": kills,
        "screen_survives": validity and not any(kills.values()),
        "fresh_adversarial_replication": True,
    }


def sha256_manifest(root: Path) -> list[str]:
    lines: list[str] = []
    for path in sorted(item for item in root.rglob("*") if item.is_file()
                       and item.name != "manifest.sha256"):
        digest = hashlib.sha256(path.read_bytes()).hexdigest()
        lines.append(f"{digest}  {path.relative_to(root)}")
    return lines
