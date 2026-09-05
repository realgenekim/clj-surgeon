#!/usr/bin/env python3
"""Collect bounded Codex and Claude Code usage evidence without transcript prose."""

from __future__ import annotations

import argparse
import hashlib
import json
import os
import re
import shlex
import sys
import tempfile
from collections import Counter
from datetime import datetime, timezone
from pathlib import Path


# Opt-in registration for the inspected single-call wrapper contract. Never guess
# defaults from a basename; registration requires the exact reviewed source hash.
REGISTERED_PUBLIC_WRAPPER = None
PUBLIC_WRAPPER_SHA256 = "8a3d8edd211bff1be34f4968e024a16e83ceef632e79af6226706b96c33bf14f"
PUBLIC_REQUEST_HASHES = {
    "public-positive-01": "67b0eebd738176061cac00c6cd0825b4fdd0d16b5c0c6a699888fb829f91bad3",
    "public-rollback-01": "cbd1d8dedc1d92caec528805db83f7cb715140be842bcfb895f8942b62e53d7c",
}
PUBLIC_TMPDIR = "/var/tmp/forge/astra-helper-program"
REGISTERED_MCP_READ_WRAPPER = None
MCP_READ_WRAPPER_SHA256 = "41b083aaab9598b17bf62e6c1e578bfd509dd3a7e05998ba7cb7037456fad44b"

SCHEMA = "clj-surgeon.agent-usage-ethnography.v6"
# Registered client server names, not guesses from arbitrary tool/gateway text.
SURGEON_MCP_SERVERS = {"clj-surgeon", "clj_surgeon", "surgeon"}
SURGEON_MCP_PREFIXES = tuple(f"mcp__{server}__" for server in sorted(SURGEON_MCP_SERVERS))
LOGICAL_ARGUMENT_DOMAIN = b"clj-surgeon.logical-tool-arguments.v1\0"
MARKER_RE = re.compile(r"<!--\s*agent-usage-window-end:\s*([^\s]+)\s*-->")
CLJ_PATH_RE = re.compile(r"(?<![\w.-])([~/.$\w-][~/.$\w-]*\.clj(?:c|s)?)(?![\w.-])")
SURGEON_RE = re.compile(
    r"(?:(?:~/bin/|[\w./-]*/)?clj-surgeon|"
    r"bb(?:\s+(?!-m(?:\s|$))\S+)*\s+-m\s+clj-surgeon\.core)"
    r"\s+(?:(?:--help)|:op\s+(:[\w!?-]+))"
)
SKILL_LOAD_RE = re.compile(
    r"(?:cat|sed\b[^\n]*|Read\b[^\n]*)[^\n]*clj-surgeon[^\n]*/SKILL\.md",
    re.IGNORECASE,
)
ROUTE_KIND_ORDER = {
    "skill-load": 0,
    "surgeon-read": 1,
    "surgeon-plan": 2,
    "surgeon-apply": 3,
    "native-read": 4,
    "native-patch": 5,
    "semantic-read": 6,
    "live-probe": 7,
    "verify": 8,
    "git": 9,
}
SURGEON_READ_OPS = {
    ":cat", ":deps", ":find-subform", ":get", ":get-form", ":grep-form",
    ":help", ":ls", ":ls-deps", ":ls-extract", ":match-form", ":outline",
    ":q", ":read", ":topo", ":xray",
}
SURGEON_PLAN_OPS = {
    ":change", ":edit", ":extract", ":fix-declares", ":mv", ":rename-ns",
    ":replace-subform",
}
SURGEON_APPLY_OPS = {
    ":change!", ":extract!", ":fix-declares!", ":mv-with-deps",
    ":rename-ns!", ":replace-subform!", ":undo-change!",
}
BACKGROUND_ACTION_KINDS = {
    "collaboration", "context-compaction", "coordination", "native-patch",
    "native-read", "other-tool", "semantic-read", "shell", "surgeon-apply",
    "surgeon-plan", "surgeon-read", "verify",
}


def parse_time(value: str) -> datetime:
    normalized = value.strip().replace("Z", "+00:00")
    parsed = datetime.fromisoformat(normalized)
    if parsed.tzinfo is None:
        parsed = parsed.replace(tzinfo=timezone.utc)
    return parsed.astimezone(timezone.utc)


def iso_time(value: datetime) -> str:
    return value.astimezone(timezone.utc).isoformat().replace("+00:00", "Z")


def stable_key(value: str) -> str:
    return hashlib.sha256(value.encode("utf-8")).hexdigest()[:12]


def canonical_sha256(value: object) -> str:
    return hashlib.sha256(canonical_json_bytes(value)).hexdigest()


def canonical_json_bytes(value: object) -> bytes:
    rendered = json.dumps(
        value, sort_keys=True, separators=(",", ":"), ensure_ascii=False
    )
    return rendered.encode("utf-8")


def root_normalized_tool_arguments(arguments: dict) -> dict:
    """Normalize only the public routing root; preserve every decision field."""
    normalized = dict(arguments)
    if "workspace_root" in normalized:
        normalized["workspace_root"] = "<workspace>"
    return normalized


def compile_mcp_action_evidence(item: dict) -> dict:
    """Compile byte/hash scalars while private arguments and results are in scope."""
    evidence = {}
    arguments = item.get("arguments")
    if isinstance(arguments, dict):
        argument_bytes = canonical_json_bytes(arguments)
        logical_bytes = canonical_json_bytes(
            root_normalized_tool_arguments(arguments)
        )
        evidence["argument_canonical_bytes"] = len(argument_bytes)
        evidence["logical_argument_sha256"] = hashlib.sha256(
            LOGICAL_ARGUMENT_DOMAIN + logical_bytes
        ).hexdigest()
    if item.get("server") in SURGEON_MCP_SERVERS and "result" in item:
        evidence["result_canonical_bytes"] = len(
            canonical_json_bytes(item.get("result"))
        )
    return evidence


def structural_target(value: object) -> object:
    """Remove request bookkeeping while retaining only hashable target shape."""
    omitted_keys = {
        "expect", "id", "include_source", "snapshot_guards", "workspace_root"
    }
    if isinstance(value, dict):
        return {
            key: structural_target(item)
            for key, item in sorted(value.items())
            if key not in omitted_keys
        }
    if isinstance(value, list):
        return [structural_target(item) for item in value]
    return value


def source_hashes(
    value: object, parent_key: str = "", hash_context: bool = False
) -> set[str]:
    """Collect only exact SHA-256 values carried by source/hash evidence fields."""
    result = set()
    if isinstance(value, dict):
        for key, item in value.items():
            key_text = str(key)
            result.update(source_hashes(
                item,
                key_text,
                hash_context
                or key_text.endswith("_hash")
                or key_text.endswith("_hashes"),
            ))
    elif isinstance(value, list):
        for item in value:
            result.update(source_hashes(item, parent_key, hash_context))
    elif (
        isinstance(value, str)
        and re.fullmatch(r"[0-9a-f]{64}", value)
        and (
            hash_context
            or parent_key.endswith("_hash")
            or parent_key.endswith("_hashes")
        )
    ):
        result.add(value)
    return result


def compile_inspect_clock_evidence(arguments: object, result: object) -> dict:
    """Compile comparable inspect identities without retaining request/source data."""
    safe_arguments = arguments if isinstance(arguments, dict) else {}
    requests = safe_arguments.get("requests")
    requests = requests if isinstance(requests, list) else []
    operations = Counter(
        str(request.get("operation") or "unknown")
        for request in requests
        if isinstance(request, dict)
    )
    evidence = {
        "batch_cardinality": len(requests),
        "file_cardinality": len(target_files({"requests": requests})),
        "request_operations": dict(sorted(operations.items())),
        "result_outcome": inspect_result_outcome(result),
        "selector_cardinality": selector_cardinality(requests),
        "structural_target_sha256": canonical_sha256(
            structural_target({"requests": requests})
        ),
    }
    hashes = source_hashes(result)
    if hashes:
        evidence["snapshot_sha256"] = canonical_sha256(sorted(hashes))
    return evidence


def target_files(value: object, parent_key: str = "") -> set[str]:
    """Return internal file identities used only to compile a public relation."""
    result = set()
    if isinstance(value, dict):
        for key, item in value.items():
            result.update(target_files(item, str(key)))
    elif isinstance(value, list):
        for item in value:
            result.update(target_files(item, parent_key))
    elif isinstance(value, str) and parent_key in {"file", "files"}:
        result.add(value)
    return result


def selector_cardinality(requests: list[object]) -> int:
    """Count explicit owner/subject selectors without retaining their values."""
    total = 0
    for request in requests:
        if not isinstance(request, dict):
            continue
        for key in ("forms", "subjects"):
            values = request.get(key)
            if isinstance(values, list):
                total += len(values)
        for key in ("owner", "subject"):
            if request.get(key) is not None:
                total += 1
    return total


def inspect_result_outcome(result: object) -> str:
    if not isinstance(result, dict):
        return "unknown"
    structured = result.get("structuredContent")
    if isinstance(structured, dict) and structured.get("ok") is True:
        return "ok"
    if isinstance(structured, dict) and structured.get("ok") is False:
        return "refused"
    if result.get("isError") is True:
        return "error"
    return "unknown"


def structural_target_relation(current: object, following: object) -> str:
    """Classify adjacent inspect targets without emitting either target."""
    if not isinstance(current, dict) or not isinstance(following, dict):
        return "unknown"
    current_files = target_files(current)
    following_files = target_files(following)
    if not current_files or not following_files:
        return "unknown"
    if current == following:
        return "exact"
    if current_files == following_files:
        return "same-files"
    if current_files & following_files:
        return "overlapping-files"
    return "disjoint-files"


def js_tool_methods(source: str) -> set[str]:
    """Return tools.<method> calls outside JavaScript strings and comments."""
    methods = set()
    index = 0
    quote = None
    escaped = False
    line_comment = False
    block_comment = False
    while index < len(source):
        char = source[index]
        following = source[index + 1] if index + 1 < len(source) else ""
        if line_comment:
            if char == "\n":
                line_comment = False
            index += 1
            continue
        if block_comment:
            if char == "*" and following == "/":
                block_comment = False
                index += 2
            else:
                index += 1
            continue
        if quote:
            if escaped:
                escaped = False
            elif char == "\\":
                escaped = True
            elif char == quote:
                quote = None
            index += 1
            continue
        if char in {'"', "'", "`"}:
            quote = char
            index += 1
            continue
        if char == "/" and following == "/":
            line_comment = True
            index += 2
            continue
        if char == "/" and following == "*":
            block_comment = True
            index += 2
            continue
        if source.startswith("tools.", index):
            match = re.match(r"tools\.([A-Za-z_][A-Za-z0-9_-]*)", source[index:])
            if match:
                methods.add(match.group(1))
                index += len(match.group(0))
                continue
        index += 1
    return methods


def registered_wrapper_ops(source: str) -> list[str]:
    """Literal JSON-string cmd at a real tools.exec_command call, exact argv only.

    Counts invocation attempts, not successful RPCs. Shell composition, dynamic
    JS, unknown interpreter options, and arbitrary Python wrappers stay unknown.
    """
    if not (REGISTERED_MCP_READ_WRAPPER or REGISTERED_PUBLIC_WRAPPER):
        return []
    operations = []
    # Skip quoted JS text and comments before accepting a call-shaped token.
    token = re.compile(r'"(?:\\.|[^"\\])*"|\'(?:\\.|[^\'\\])*\'|`(?:\\.|[^`\\])*`|//[^\n]*|/\*.*?\*/|tools\.exec_command\s*\(', re.S)
    decoder = json.JSONDecoder()
    for match in token.finditer(source):
        if not match.group().startswith("tools.exec_command"):
            continue
        tail = source[match.end():]
        prefix = re.match(r'\s*\{\s*(?:"cmd"|cmd)\s*:\s*', tail)
        if not prefix:
            continue
        try:
            command, end = decoder.raw_decode(tail[prefix.end():])
        except ValueError:
            continue
        if not isinstance(command, str) or not re.match(r'\s*[,}]', tail[prefix.end()+end:]):
            continue
        if any(c in command for c in "\n;|&<>`$"):
            continue
        try:
            argv = shlex.split(command)
        except ValueError:
            continue
        if (REGISTERED_PUBLIC_WRAPPER and len(argv) == 5
                and argv[:2] == ["env", "TMPDIR=" + PUBLIC_TMPDIR]
                and argv[2] in {"python3", "/usr/bin/python3"}
                and argv[3] == REGISTERED_PUBLIC_WRAPPER
                and argv[4] in PUBLIC_REQUEST_HASHES):
            operations.append("helper_extraction")
            continue
        if len(argv) < 5 or argv[0] not in {"python3", "/usr/bin/python3"} or argv[1] != REGISTERED_MCP_READ_WRAPPER:
            continue
        rest = argv[2:]
        positional, options = [], {}
        valid = True
        while rest:
            value, *rest = rest
            if value.startswith("-"):
                if value not in {"--tool", "--port", "--receipt"} or value in options or not rest:
                    valid = False
                    break
                options[value], *rest = rest
            else:
                positional.append(value)
        tool = options.get("--tool", "inspect_clojure")
        port = options.get("--port", "8171")
        if (valid and len(positional) == 1 and options.get("--receipt")
                and tool in {"inspect_clojure", "relation_census", "feature_thread"}
                and port.isdecimal() and (int(port) == 8171 or 8300 <= int(port) <= 8339)):
            operations.append(tool)
    return operations


def validate_public_wrapper(path: Path) -> str:
    """Opt-in exact wrapper and request identities; no transcript-derived paths."""
    if (not path.is_absolute() or path.is_symlink()
            or hashlib.sha256(path.read_bytes()).hexdigest() != PUBLIC_WRAPPER_SHA256):
        raise ValueError("public wrapper identity mismatch")
    for case, digest in PUBLIC_REQUEST_HASHES.items():
        request_path = path.parent / case / "request.json"
        if request_path.is_symlink() or hashlib.sha256(request_path.read_bytes()).hexdigest() != digest:
            raise ValueError("public wrapper request identity mismatch")
        request = json.loads(request_path.read_text())
        if request.get("method") != "tools/call" or request.get("params", {}).get("name") != "helper_extraction":
            raise ValueError("public wrapper request operation mismatch")
    return str(path)


def distribution(values: list[int]) -> dict:
    ordered = sorted(values)
    if not ordered:
        return {"count": 0, "total_ms": 0, "median_ms": None, "p90_ms": None, "max_ms": None}
    count = len(ordered)
    median = ordered[(count - 1) // 2] if count % 2 else round((ordered[count // 2 - 1] + ordered[count // 2]) / 2)
    p90 = ordered[min(count - 1, max(0, int(count * 0.9 + 0.999) - 1))]
    return {
        "count": count,
        "total_ms": sum(ordered),
        "median_ms": median,
        "p90_ms": p90,
        "max_ms": ordered[-1],
    }


def quantity_distribution(values: list[int]) -> dict:
    timed = distribution(values)
    return {
        "count": timed["count"],
        "total": timed["total_ms"],
        "median": timed["median_ms"],
        "p90": timed["p90_ms"],
        "max": timed["max_ms"],
    }


def route_kinds(text: str, action: str) -> list[str]:
    """Classify one outer tool action without retaining command or path text."""
    if action == "apply_patch":
        return ["native-patch"] if CLJ_PATH_RE.search(text) else []
    kinds = set()
    matches = list(SURGEON_RE.finditer(text))
    ops = {match.group(1) or ":help" for match in matches}
    wrapper_ops = registered_wrapper_ops(text)
    nested_methods = js_tool_methods(text)
    surgeon_methods = {
        method for method in nested_methods
        if method.startswith(SURGEON_MCP_PREFIXES)
    }
    cclsp_methods = {
        method for method in nested_methods
        if method.startswith("mcp__cclsp__")
    }
    if SKILL_LOAD_RE.search(text) or action == "skill-load":
        kinds.add("skill-load")
    if ops:
        if ops & SURGEON_APPLY_OPS or (":edit" in ops and ":expect" in text):
            kinds.add("surgeon-apply")
        if ops & SURGEON_PLAN_OPS and not (":edit" in ops and ":expect" in text):
            kinds.add("surgeon-plan")
        if ops & SURGEON_READ_OPS:
            kinds.add("surgeon-read")
    if any(method.endswith("__inspect_clojure") for method in surgeon_methods):
        kinds.add("surgeon-read")
    if any(method.endswith(("__apply_clojure_changes", "__alias_migration", "__helper_extraction")) for method in surgeon_methods):
        kinds.add("surgeon-apply")
    if set(wrapper_ops) & {"inspect_clojure", "relation_census"}:
        kinds.add("surgeon-read")
    if "helper_extraction" in wrapper_ops:
        kinds.add("surgeon-apply")
    if "feature_thread" in wrapper_ops:
        kinds.add("surgeon-plan")
    if cclsp_methods:
        kinds.add("semantic-read")
    has_clojure_path = bool(CLJ_PATH_RE.search(text))
    if action == "apply_patch" and has_clojure_path and not ops:
        kinds.add("native-patch")
    elif has_clojure_path and not ops:
        kinds.add("native-read")
    if "clj-nrepl-eval" in text:
        kinds.add("live-probe")
    if re.search(
        r"(?:standard-clojure-style|standard-clj-format|clj-kondo|"
        r"(?:make|bb)\s+(?:test|lint|check)|clojure\s+-M(?::test)?|"
        r"npm\s+test|pytest)",
        text,
    ):
        kinds.add("verify")
    if re.search(r"(?:^|[\s;])git\s+(?:status|diff|log|show|branch|check-ignore)\b", text):
        kinds.add("git")
    return sorted(kinds, key=lambda kind: ROUTE_KIND_ORDER[kind])


def route_action(text: str, action: str, surgeon_calls: int = 0) -> dict | None:
    kinds = route_kinds(text, action)
    if not kinds:
        return None
    return {
        "kinds": kinds,
        "actions": 1,
        "clj_surgeon_calls": surgeon_calls,
        "input_chars": len(text),
        "output_chars": 0,
        "wall_ms": 0,
    }


def collapse_route_actions(actions: list[dict]) -> list[dict]:
    """Collapse adjacent equivalent route actions into privacy-safe phases."""
    phases = []
    for action in actions:
        if not action:
            continue
        if phases and phases[-1]["kinds"] == action["kinds"]:
            phase = phases[-1]
            for key in ("actions", "clj_surgeon_calls", "input_chars", "output_chars", "wall_ms"):
                phase[key] += action[key]
        else:
            phases.append(dict(action))
    return phases


def first_route_kind(kinds: list[str]) -> str:
    """Return one stable clock lane without retaining a command or payload."""
    return kinds[0] if kinds else "shell"


def safe_item_status(value: object) -> str | None:
    rendered = str(value or "").lower()
    return rendered if rendered in {
        "cancelled", "completed", "declined", "failed", "in_progress"
    } else None


def completed_item_clock_sample(payload: dict) -> dict | None:
    """Compile one Codex completed item into privacy-safe timing evidence."""
    item = payload.get("item") if isinstance(payload.get("item"), dict) else {}
    item_type = str(item.get("type") or "")
    started_at_ms = payload.get("started_at_ms")
    completed_at_ms = payload.get("completed_at_ms")
    if not isinstance(started_at_ms, (int, float)) or not isinstance(completed_at_ms, (int, float)):
        return None
    if completed_at_ms < started_at_ms:
        return None

    sample = {
        "kind": "other-tool",
        "started_at_ms": round(started_at_ms),
        "completed_at_ms": round(completed_at_ms),
    }
    status = safe_item_status(item.get("status"))
    if status:
        sample["status"] = status

    if item_type == "Reasoning":
        sample["kind"] = "model-reasoning"
    elif item_type == "AgentMessage":
        sample["kind"] = "model-message"
        phase = str(item.get("phase") or "").lower()
        if phase in {"commentary", "final"}:
            sample["phase"] = phase
    elif item_type == "McpToolCall":
        server = str(item.get("server") or "")
        tool = str(item.get("tool") or "")
        sample["transport"] = "mcp"
        action_evidence = compile_mcp_action_evidence(item)
        if action_evidence:
            sample["action_evidence"] = action_evidence
        if server in SURGEON_MCP_SERVERS:
            if tool == "inspect_clojure":
                sample["kind"] = "surgeon-read"
            elif tool in {"apply_clojure_changes", "edit_clojure", "alias_migration", "helper_extraction"}:
                sample["kind"] = "surgeon-apply"
            else:
                sample["kind"] = "surgeon-plan"
            if tool in {
                "apply_clojure_changes", "edit_clojure", "inspect_clojure",
                "transform_clojure", "alias_migration", "helper_extraction"
            }:
                sample["operation"] = tool
            if tool == "inspect_clojure":
                arguments = (
                    item.get("arguments")
                    if isinstance(item.get("arguments"), dict)
                    else {}
                )
                sample.update(compile_inspect_clock_evidence(
                    arguments, item.get("result")
                ))
                requests = arguments.get("requests")
                requests = requests if isinstance(requests, list) else []
                sample["_structural_target"] = structural_target(
                    {"requests": requests}
                )
        elif server == "cclsp":
            sample["kind"] = "semantic-read"
            if tool in {
                "find_references", "inspect_runtime", "resolve_var_surface",
                "resolve_var_surfaces"
            }:
                sample["operation"] = tool
        elif server == "director-progress":
            sample["kind"] = "coordination"
        else:
            sample["kind"] = "other-tool"
    elif item_type == "CommandExecution":
        command = item.get("command")
        if isinstance(command, list):
            command = " ".join(str(value) for value in command)
        command = str(command or "")
        kinds = route_kinds(command, "shell")
        sample["kind"] = first_route_kind(kinds)
        matches = list(SURGEON_RE.finditer(command))
        operations = sorted({match.group(1) or ":help" for match in matches})
        sample["transport"] = "cli" if matches else "shell"
        if matches:
            sample["invocation_count"] = len(matches)
        if len(operations) == 1:
            sample["operation"] = operations[0]
    elif item_type == "FileChange":
        sample["kind"] = "native-patch"
        sample["transport"] = "native"
    elif item_type == "UserMessage":
        sample["kind"] = "human-input"
    elif item_type == "ContextCompaction":
        sample["kind"] = "context-compaction"
    elif item_type in {"CollabAgentToolCall", "SubAgentActivity"}:
        sample["kind"] = "collaboration"
    return sample


def merge_intervals(intervals: list[tuple[int, int]]) -> list[tuple[int, int]]:
    merged = []
    for start, end in sorted(intervals):
        if end <= start:
            continue
        if merged and start <= merged[-1][1]:
            merged[-1] = (merged[-1][0], max(merged[-1][1], end))
        else:
            merged.append((start, end))
    return merged


def interval_coverage(intervals: list[tuple[int, int]]) -> int:
    return sum(end - start for start, end in merge_intervals(intervals))


def clipped_kind_coverage(items: list[dict], kind: str, start: int, end: int) -> int:
    return interval_coverage([
        (max(start, item["_start_ms"]), min(end, item["_end_ms"]))
        for item in items
        if item["kind"] == kind and item["_end_ms"] > start and item["_start_ms"] < end
    ])


def compile_action_emission_evidence(
    source: dict,
    endpoint: dict | None,
    items: list[dict],
    boundary_start: int,
    boundary_end: int,
) -> dict:
    """Compile safe action-size and post-reasoning wall evidence for one boundary."""
    result = {}
    source_evidence = source.get("action_evidence")
    source_evidence = source_evidence if isinstance(source_evidence, dict) else {}
    previous_result_bytes = source_evidence.get("result_canonical_bytes")
    if isinstance(previous_result_bytes, int) and not isinstance(previous_result_bytes, bool):
        result["previous_surgeon_result_canonical_bytes"] = previous_result_bytes

    endpoint_evidence = endpoint.get("action_evidence") if endpoint else None
    endpoint_evidence = endpoint_evidence if isinstance(endpoint_evidence, dict) else {}
    next_argument_bytes = endpoint_evidence.get("argument_canonical_bytes")
    if isinstance(next_argument_bytes, int) and not isinstance(next_argument_bytes, bool):
        result["next_argument_canonical_bytes"] = next_argument_bytes
    next_logical_hash = endpoint_evidence.get("logical_argument_sha256")
    if isinstance(next_logical_hash, str) and re.fullmatch(r"[0-9a-f]{64}", next_logical_hash):
        result["next_logical_argument_sha256"] = next_logical_hash

    completed_reasoning_ends = [
        item["_end_ms"]
        for item in items
        if item["kind"] == "model-reasoning"
        and item["_end_ms"] > boundary_start
        and item["_end_ms"] <= boundary_end
    ]
    if endpoint and completed_reasoning_ends:
        result["last_reasoning_end_to_next_action_start_ms"] = max(
            0, boundary_end - max(completed_reasoning_ends)
        )

    background_intervals = [
        (max(boundary_start, item["_start_ms"]), min(boundary_end, item["_end_ms"]))
        for item in items
        if item is not source
        and item is not endpoint
        and item["kind"] in BACKGROUND_ACTION_KINDS
        and item["_end_ms"] > boundary_start
        and item["_start_ms"] < boundary_end
    ]
    result["overlapping_background_wall_ms"] = interval_coverage(
        background_intervals
    )
    return result


def compile_post_surgeon_boundaries(items: list[dict], turn_start_ms: int, turn_end_ms: int) -> list[dict]:
    """Measure Surgeon completion to the caller's next externally visible act."""
    endpoint_kinds = {
        "collaboration", "coordination", "human-input", "model-message",
        "native-patch", "native-read", "other-tool", "semantic-read", "shell",
        "surgeon-apply", "surgeon-plan", "surgeon-read", "verify",
    }
    boundaries = []
    for item in items:
        if not item["kind"].startswith("surgeon-"):
            continue
        endpoint = next((
            candidate for candidate in items
            if candidate["kind"] in endpoint_kinds
            and candidate["_start_ms"] >= item["_end_ms"]
            and candidate is not item
        ), None)
        boundary_end = endpoint["_start_ms"] if endpoint else turn_end_ms
        boundary_start = item["_end_ms"]
        result = {
            "offset_ms": item["_start_ms"] - turn_start_ms,
            "operation": item.get("operation"),
            "transport": item.get("transport"),
            "status": item.get("status"),
            "tool_wall_ms": item["_end_ms"] - item["_start_ms"],
            "boundary_ms": max(0, boundary_end - boundary_start),
            "model_reasoning_ms": clipped_kind_coverage(
                items, "model-reasoning", boundary_start, boundary_end
            ),
            "model_message_ms": clipped_kind_coverage(
                items, "model-message", boundary_start, boundary_end
            ),
            "next_kind": endpoint["kind"] if endpoint else "turn-end",
        }
        result["action_emission"] = compile_action_emission_evidence(
            item, endpoint, items, boundary_start, boundary_end
        )
        for key in (
            "action_ordinal", "batch_cardinality", "file_cardinality",
            "request_operations", "result_outcome", "selector_cardinality",
            "snapshot_sha256", "structural_target_sha256",
        ):
            if key in item:
                result[key] = item[key]
        if endpoint and "action_ordinal" in endpoint:
            result["next_action_ordinal"] = endpoint["action_ordinal"]
        if endpoint and endpoint.get("kind") == "surgeon-read":
            result["target_relation"] = structural_target_relation(
                item.get("_structural_target"),
                endpoint.get("_structural_target"),
            )
            for key in (
                "batch_cardinality", "file_cardinality", "request_operations",
                "result_outcome", "selector_cardinality",
            ):
                if key in endpoint:
                    result[f"next_{key}"] = endpoint[key]
        if endpoint and endpoint.get("operation"):
            result["next_operation"] = endpoint["operation"]
        if endpoint and endpoint.get("transport"):
            result["next_transport"] = endpoint["transport"]
        boundaries.append({key: value for key, value in result.items() if value is not None})
    return boundaries


def compile_event_clock(started_at: str, duration_ms: int, samples: list[dict]) -> dict:
    """Build an ordered measured-item and unattributed-gap clock for one turn."""
    turn_start_ms = round(parse_time(started_at).timestamp() * 1000)
    turn_end_ms = turn_start_ms + max(0, int(duration_ms or 0))
    bounded = []
    for sample in samples:
        start = max(turn_start_ms, min(turn_end_ms, sample["started_at_ms"]))
        end = max(start, min(turn_end_ms, sample["completed_at_ms"]))
        item = {
            key: value
            for key, value in sample.items()
            if key not in {"started_at_ms", "completed_at_ms"}
        }
        item["offset_ms"] = start - turn_start_ms
        item["wall_ms"] = end - start
        item["_end_ms"] = end
        item["_start_ms"] = start
        bounded.append(item)
    bounded.sort(key=lambda item: (item["_start_ms"], item["_end_ms"], item["kind"]))

    items = []
    cursor = turn_start_ms
    by_kind = Counter()
    for item in bounded:
        if item["_start_ms"] > cursor:
            items.append({
                "kind": "unattributed-gap",
                "offset_ms": cursor - turn_start_ms,
                "wall_ms": item["_start_ms"] - cursor,
            })
        public_item = {
            key: value for key, value in item.items() if not key.startswith("_")
        }
        items.append(public_item)
        by_kind[public_item["kind"]] += public_item["wall_ms"]
        cursor = max(cursor, item["_end_ms"])
    if cursor < turn_end_ms:
        items.append({
            "kind": "unattributed-gap",
            "offset_ms": cursor - turn_start_ms,
            "wall_ms": turn_end_ms - cursor,
        })

    measured_intervals = merge_intervals([
        (item["_start_ms"], item["_end_ms"]) for item in bounded
    ])
    measured_coverage_ms = interval_coverage(measured_intervals)
    unattributed_wall_ms = max(0, turn_end_ms - turn_start_ms - measured_coverage_ms)
    duration = max(0, turn_end_ms - turn_start_ms)
    return {
        "items": items,
        "by_kind_ms": dict(sorted(by_kind.items())),
        "measured_coverage_ms": measured_coverage_ms,
        "unattributed_wall_ms": unattributed_wall_ms,
        "coverage_ratio": round(measured_coverage_ms / duration, 4) if duration else None,
        "post_surgeon_boundaries": compile_post_surgeon_boundaries(
            bounded, turn_start_ms, turn_end_ms
        ),
    }


def finalize_turn(turn: dict) -> dict:
    result = dict(turn)
    samples = result.pop("clj_surgeon_action_wall_ms")
    prompts = result.pop("_user_messages")
    final_message = result.pop("_final_message")
    route_actions = result.pop("_route_actions")
    clock_samples = result.pop("_clock_samples")
    result.pop("_turn_id")
    prompt_text = "\n\n".join(prompts)
    result["clj_surgeon_action_wall"] = distribution(samples)
    duration = result.get("duration_ms") or 0
    result["clj_surgeon_tool_wall_share"] = round(sum(samples) / duration, 4) if duration else None
    result["user_message_count"] = len(prompts)
    result["prompt_chars"] = len(prompt_text)
    result["prompt_sha256"] = hashlib.sha256(prompt_text.encode("utf-8")).hexdigest()
    result["final_message_sha256"] = hashlib.sha256(final_message.encode("utf-8")).hexdigest()
    result["commit_candidates"] = sorted(set(re.findall(r"(?<![0-9a-f])([0-9a-f]{7,40})(?![0-9a-f])", final_message)))
    result["route_phases"] = collapse_route_actions(route_actions)
    result["event_clock"] = compile_event_clock(
        result["started_at"], result.get("duration_ms") or 0, clock_samples
    )
    return result


def iter_jsonl(path: Path):
    try:
        with path.open("r", encoding="utf-8", errors="replace") as handle:
            for line in handle:
                try:
                    value = json.loads(line)
                except json.JSONDecodeError:
                    continue
                if isinstance(value, dict):
                    yield value
    except OSError:
        return


def event_time(event: dict) -> datetime | None:
    value = event.get("timestamp")
    if not isinstance(value, str):
        return None
    try:
        return parse_time(value)
    except ValueError:
        return None


def newest_marker(observations_root: Path) -> tuple[datetime | None, str | None]:
    newest = None
    source = None
    if not observations_root.exists():
        return newest, source
    for path in observations_root.glob("*.md"):
        try:
            text = path.read_text(encoding="utf-8", errors="replace")
        except OSError:
            continue
        for raw in MARKER_RE.findall(text):
            try:
                candidate = parse_time(raw)
            except ValueError:
                continue
            if newest is None or candidate > newest:
                newest = candidate
                source = path.name
    return newest, source


def empty_session(provider: str, path: Path) -> dict:
    return {
        "provider": provider,
        "session_key": stable_key(str(path)),
        "evidence_file": path.name,
        "first_event": None,
        "last_event": None,
        "event_count": 0,
        "skill_visible": False,
        "activation_trigger_visible": False,
        "skill_loads": 0,
        "clj_surgeon_calls": 0,
        "clj_surgeon_tool_actions": 0,
        "clj_surgeon_result_actions": 0,
        "clj_surgeon_refusal_actions": 0,
        "clj_surgeon_refusal_types": Counter(),
        "clj_surgeon_execution_error_actions": 0,
        "clj_surgeon_output_chars": 0,
        "clj_surgeon_action_wall_ms": [],
        "native_apply_patch_action_wall_ms": [],
        "task_turns": [],
        "clj_surgeon_ops": Counter(),
        "cclsp_methods": Counter(),
        "route_features": Counter(),
        "native_clojure_actions": Counter(),
        "bounded_clojure_reads": 0,
        "unbounded_clojure_reads": 0,
        "clojure_read_output_chars": 0,
        "clojure_file_keys": set(),
        "tool_input_chars": 0,
        "tool_output_chars": 0,
        "route_actions": [],
    }


def record_time(session: dict, timestamp: datetime) -> None:
    rendered = iso_time(timestamp)
    session["event_count"] += 1
    if session["first_event"] is None:
        session["first_event"] = rendered
    session["last_event"] = rendered


def record_tool_text(session: dict, text: str, action: str, scan_commands: bool = True) -> None:
    matches = list(SURGEON_RE.finditer(text)) if scan_commands else []
    wrapper_ops = registered_wrapper_ops(text)
    nested_methods = js_tool_methods(text)
    surgeon_methods = sorted(
        method for method in nested_methods
        if method.startswith(SURGEON_MCP_PREFIXES)
    )
    cclsp_methods = sorted(
        method for method in nested_methods if method.startswith("mcp__cclsp__")
    )
    for method in cclsp_methods:
        session["cclsp_methods"][method.rsplit("__", 1)[-1]] += 1
    if matches or surgeon_methods or wrapper_ops:
        session["clj_surgeon_tool_actions"] += 1
        session["clj_surgeon_calls"] += len(matches) + len(surgeon_methods) + len(wrapper_ops)
        for operation in wrapper_ops:
            session["clj_surgeon_ops"][operation] += 1
        for match in matches:
            session["clj_surgeon_ops"][match.group(1) or ":help"] += 1
        for method in surgeon_methods:
            session["clj_surgeon_ops"][method.rsplit("__", 1)[-1]] += 1
        feature_patterns = {
            "contains": r":contains\b",
            "expect": r":expect\b",
            "form": r"(?:\(form\b|:form\b)",
            "line": r"(?:\(line\b|:line\b)",
            "match": r"\(match\b",
            "plan-out": r":plan-out\b",
            "right": r"(?:\bright\b|:right\b)",
            "transform": r"\(transform\b",
        }
        for feature, pattern in feature_patterns.items():
            if re.search(pattern, text):
                session["route_features"][feature] += 1
    paths = CLJ_PATH_RE.findall(text)
    session["clojure_file_keys"].update(stable_key(path) for path in paths)
    if paths and not matches:
        session["native_clojure_actions"][action] += 1
    if scan_commands and SKILL_LOAD_RE.search(text):
        session["skill_loads"] += 1


def finalize_session(session: dict) -> dict:
    return {
        "provider": session["provider"],
        "session_key": session["session_key"],
        "evidence_file": session["evidence_file"],
        "first_event": session["first_event"],
        "last_event": session["last_event"],
        "event_count": session["event_count"],
        "skill_visible": session["skill_visible"],
        "activation_trigger_visible": session["activation_trigger_visible"],
        "skill_loads": session["skill_loads"],
        "clj_surgeon_calls": session["clj_surgeon_calls"],
        "clj_surgeon_tool_actions": session["clj_surgeon_tool_actions"],
        "clj_surgeon_result_actions": session["clj_surgeon_result_actions"],
        "clj_surgeon_refusal_actions": session["clj_surgeon_refusal_actions"],
        "clj_surgeon_refusal_types": dict(sorted(session["clj_surgeon_refusal_types"].items())),
        "clj_surgeon_execution_error_actions": session["clj_surgeon_execution_error_actions"],
        "clj_surgeon_output_chars": session["clj_surgeon_output_chars"],
        "clj_surgeon_action_wall": distribution(session["clj_surgeon_action_wall_ms"]),
        "native_apply_patch_action_wall": distribution(session["native_apply_patch_action_wall_ms"]),
        "clj_surgeon_action_wall_samples_ms": session["clj_surgeon_action_wall_ms"],
        "native_apply_patch_action_wall_samples_ms": session["native_apply_patch_action_wall_ms"],
        "task_turns": session["task_turns"],
        "clj_surgeon_ops": dict(sorted(session["clj_surgeon_ops"].items())),
        "cclsp_methods": dict(sorted(session["cclsp_methods"].items())),
        "route_features": dict(sorted(session["route_features"].items())),
        "native_clojure_actions": dict(sorted(session["native_clojure_actions"].items())),
        "bounded_clojure_reads": session["bounded_clojure_reads"],
        "unbounded_clojure_reads": session["unbounded_clojure_reads"],
        "clojure_read_output_chars": session["clojure_read_output_chars"],
        "distinct_clojure_files": len(session["clojure_file_keys"]),
        "tool_input_chars": session["tool_input_chars"],
        "tool_output_chars": session["tool_output_chars"],
        "route_phases": collapse_route_actions(session["route_actions"]),
    }


def analyze_codex_file(path: Path, since: datetime, until: datetime) -> dict | None:
    session = empty_session("codex", path)
    pending = {}
    current_turn = None
    events = list(iter_jsonl(path))
    serialized = "\n".join(json.dumps(event, separators=(",", ":")) for event in events)
    session["skill_visible"] = "clj-surgeon:" in serialized or "name: clj-surgeon" in serialized
    session["activation_trigger_visible"] = "Invoke before using Read, Edit, grep, sed, or cat" in serialized

    for event in events:
        timestamp = event_time(event)
        if timestamp is None or timestamp < since or timestamp > until:
            continue
        record_time(session, timestamp)
        if event.get("type") == "event_msg":
            event_payload = event.get("payload") or {}
            if event_payload.get("type") == "task_started":
                current_turn = {
                    "_turn_id": str(event_payload.get("turn_id") or ""),
                    "turn_key": stable_key(str(event_payload.get("turn_id") or timestamp)),
                    "started_at": iso_time(timestamp),
                    "completed": False,
                    "duration_ms": None,
                    "clj_surgeon_calls": 0,
                    "clj_surgeon_tool_actions": 0,
                    "native_apply_patch_actions": 0,
                    "clj_surgeon_action_wall_ms": [],
                    "_user_messages": [],
                    "_final_message": "",
                    "_route_actions": [],
                    "_clock_samples": [],
                }
            elif event_payload.get("type") == "task_complete" and current_turn:
                current_turn["completed"] = True
                current_turn["duration_ms"] = int(event_payload.get("duration_ms") or ((timestamp - parse_time(current_turn["started_at"])).total_seconds() * 1000))
                current_turn["_final_message"] = str(event_payload.get("last_agent_message") or "")
                session["task_turns"].append(finalize_turn(current_turn))
                current_turn = None
            elif event_payload.get("type") == "user_message" and current_turn:
                message = str(event_payload.get("message") or "")
                if message and not message.startswith("<environment_context>"):
                    current_turn["_user_messages"].append(message)
            elif event_payload.get("type") == "item_completed" and current_turn:
                item_turn_id = str(event_payload.get("turn_id") or "")
                if not item_turn_id or item_turn_id == current_turn["_turn_id"]:
                    sample = completed_item_clock_sample(event_payload)
                    if sample:
                        sample["action_ordinal"] = len(current_turn["_clock_samples"]) + 1
                        current_turn["_clock_samples"].append(sample)
            continue
        if event.get("type") != "response_item":
            continue
        payload = event.get("payload") or {}
        payload_type = payload.get("type")
        if payload_type == "custom_tool_call":
            call_id = payload.get("call_id") or payload.get("id")
            name = str(payload.get("name") or "unknown")
            tool_input = str(payload.get("input") or "")
            session["tool_input_chars"] += len(tool_input)
            tool_methods = js_tool_methods(tool_input)
            has_shell = bool({"exec_command", "write_stdin"} & tool_methods)
            has_patch = "apply_patch" in tool_methods
            command_text = tool_input if has_shell else ""
            surgeon_methods = {
                method for method in tool_methods
                if method.startswith(SURGEON_MCP_PREFIXES)
            }
            mcp_methods = {
                method for method in tool_methods
                if method.startswith("mcp__")
            }
            wrapper_ops = registered_wrapper_ops(command_text)
            surgeon_action = bool(SURGEON_RE.search(command_text) or surgeon_methods or wrapper_ops)
            surgeon_call_count = len(SURGEON_RE.findall(command_text)) + len(surgeon_methods) + len(wrapper_ops)
            native_apply_patch = "tools.apply_patch" in tool_input and bool(CLJ_PATH_RE.search(tool_input)) and not surgeon_action
            if current_turn:
                current_turn["clj_surgeon_calls"] += surgeon_call_count
                current_turn["clj_surgeon_tool_actions"] += int(surgeon_action)
                current_turn["native_apply_patch_actions"] += int(native_apply_patch)
            if has_patch and not has_shell:
                action = "apply_patch"
            elif has_shell:
                action = "shell"
            else:
                action = name
            route_text = tool_input if action == "apply_patch" or mcp_methods else command_text
            action_route = route_action(route_text, action, surgeon_call_count)
            if action_route:
                session["route_actions"].append(action_route)
                if current_turn:
                    current_turn["_route_actions"].append(action_route)
            if call_id:
                pending[call_id] = {
                    "name": name,
                    "surgeon": surgeon_action,
                    "native_apply_patch": native_apply_patch,
                    "started_at": timestamp,
                    "turn": current_turn,
                    "route_action": action_route,
                }
            record_tool_text(
                session,
                tool_input if action == "apply_patch" or mcp_methods else command_text,
                action,
                scan_commands=action != "apply_patch",
            )
        elif payload_type == "custom_tool_call_output":
            output = str(payload.get("output") or "")
            session["tool_output_chars"] += len(output)
            call = pending.pop(payload.get("call_id"), None)
            if call:
                wall_ms = max(0, round((timestamp - call["started_at"]).total_seconds() * 1000))
                if call["route_action"]:
                    call["route_action"]["output_chars"] = len(output)
                    call["route_action"]["wall_ms"] = wall_ms
                if call["surgeon"]:
                    session["clj_surgeon_action_wall_ms"].append(wall_ms)
                    if call["turn"]:
                        call["turn"]["clj_surgeon_action_wall_ms"].append(wall_ms)
                    session["clj_surgeon_result_actions"] += 1
                    session["clj_surgeon_output_chars"] += len(output)
                    refusal = bool(re.search(r":error(?:-type)?\b|\"error-type\"", output))
                    if refusal:
                        session["clj_surgeon_refusal_actions"] += 1
                        refusal_types = re.findall(r":error-type\s+(:[\w!?-]+)|\"error-type\"\s*:\s*\"?([:\w!?-]+)", output)
                        for edn_type, json_type in refusal_types:
                            session["clj_surgeon_refusal_types"][edn_type or json_type] += 1
                    if not refusal and re.search(r"Script failed|Traceback|Exception|(?:exit_code|exit code)[^0-9]*[1-9]", output, re.IGNORECASE):
                        session["clj_surgeon_execution_error_actions"] += 1
                if call["native_apply_patch"]:
                    session["native_apply_patch_action_wall_ms"].append(wall_ms)
    if current_turn:
        current_turn["duration_ms"] = max(0, round((until - parse_time(current_turn["started_at"])).total_seconds() * 1000))
        session["task_turns"].append(finalize_turn(current_turn))
    if not session["event_count"]:
        return None
    return finalize_session(session)


def claude_tool_uses(event: dict):
    if event.get("type") != "assistant":
        return
    message = event.get("message") or {}
    content = message.get("content") or []
    if not isinstance(content, list):
        return
    for item in content:
        if isinstance(item, dict) and item.get("type") == "tool_use":
            yield item


def analyze_claude_file(path: Path, since: datetime, until: datetime) -> dict | None:
    session = empty_session("claude", path)
    events = list(iter_jsonl(path))
    for event in events:
        if event.get("type") == "attachment":
            attachment = event.get("attachment") or {}
            if attachment.get("type") == "skill_listing" and "- clj-surgeon:" in str(attachment.get("content") or ""):
                session["skill_visible"] = True
                if "Invoke before using Read, Edit, grep, sed, or cat" in str(attachment.get("content") or ""):
                    session["activation_trigger_visible"] = True

    tool_names = {}
    for event in events:
        timestamp = event_time(event)
        if timestamp is None or timestamp < since or timestamp > until:
            continue
        record_time(session, timestamp)
        for tool in claude_tool_uses(event) or []:
            name = str(tool.get("name") or "unknown")
            tool_input = tool.get("input") or {}
            rendered = json.dumps(tool_input, separators=(",", ":"), ensure_ascii=False)
            mcp_rendered = f"tools.{name}({rendered})" if name.startswith("mcp__") else rendered
            session["tool_input_chars"] += len(rendered)
            if tool.get("id"):
                tool_names[tool["id"]] = {"name": name, "clojure_read": False, "route_action": None}
            if name == "Skill" and tool_input.get("skill") == "clj-surgeon":
                session["skill_loads"] += 1
            if name in {"Read", "Edit", "Write"}:
                path_value = str(tool_input.get("file_path") or "")
                record_tool_text(session, path_value, name.lower())
                if name == "Read" and CLJ_PATH_RE.search(path_value):
                    bounded = "offset" in tool_input or "limit" in tool_input
                    session["bounded_clojure_reads" if bounded else "unbounded_clojure_reads"] += 1
                    if tool.get("id"):
                        tool_names[tool["id"]]["clojure_read"] = True
            elif name == "Bash":
                record_tool_text(session, str(tool_input.get("command") or ""), "shell")
            elif name == "Skill":
                record_tool_text(session, rendered, name.lower())
            elif name.startswith("mcp__"):
                record_tool_text(session, mcp_rendered, name.lower())
            if name == "Bash":
                route_text = str(tool_input.get("command") or "")
                route_action_name = "shell"
            elif name in {"Read", "Edit", "Write"}:
                route_text = str(tool_input.get("file_path") or "")
                route_action_name = name.lower()
            else:
                route_text = mcp_rendered
                route_action_name = "skill-load" if name == "Skill" and tool_input.get("skill") == "clj-surgeon" else name.lower()
            action_route = route_action(route_text, route_action_name, len(SURGEON_RE.findall(route_text)))
            if action_route:
                session["route_actions"].append(action_route)
                if tool.get("id"):
                    tool_names[tool["id"]]["route_action"] = action_route

        if event.get("type") == "user":
            content = ((event.get("message") or {}).get("content") or [])
            if isinstance(content, list):
                for item in content:
                    if not isinstance(item, dict) or item.get("type") != "tool_result":
                        continue
                    value = item.get("content")
                    rendered = value if isinstance(value, str) else json.dumps(value, ensure_ascii=False)
                    session["tool_output_chars"] += len(rendered)
                    tool = tool_names.pop(item.get("tool_use_id"), None)
                    if tool and tool["clojure_read"]:
                        session["clojure_read_output_chars"] += len(rendered)
                    if tool and tool["route_action"]:
                        tool["route_action"]["output_chars"] = len(rendered)
    if not session["event_count"]:
        return None
    return finalize_session(session)


def candidate_files(root: Path, pattern: str, since: datetime):
    if not root.exists():
        return []
    threshold = since.timestamp()
    result = []
    for path in root.rglob(pattern):
        try:
            if path.stat().st_mtime >= threshold:
                result.append(path)
        except OSError:
            continue
    return sorted(result)


def provider_summary(provider: str, sessions: list[dict]) -> dict:
    relevant = [
        session
        for session in sessions
        if session["skill_loads"]
        or session["clj_surgeon_calls"]
        or sum(session["cclsp_methods"].values())
        or sum(session["native_clojure_actions"].values())
    ]
    ops = Counter()
    native = Counter()
    route_features = Counter()
    route_action_kinds = Counter()
    cclsp_methods = Counter()
    refusal_types = Counter()
    clock_item_wall_by_kind = Counter()
    post_surgeon_boundary_wall = []
    post_surgeon_reasoning_wall = []
    post_surgeon_endpoints = Counter()
    post_surgeon_transports = Counter()
    surgeon_wall = []
    native_patch_wall = []
    for session in relevant:
        ops.update(session["clj_surgeon_ops"])
        native.update(session["native_clojure_actions"])
        route_features.update(session["route_features"])
        cclsp_methods.update(session["cclsp_methods"])
        refusal_types.update(session["clj_surgeon_refusal_types"])
        surgeon_wall.extend(session["clj_surgeon_action_wall_samples_ms"])
        native_patch_wall.extend(session["native_apply_patch_action_wall_samples_ms"])
        for phase in session["route_phases"]:
            for kind in phase["kinds"]:
                route_action_kinds[kind] += phase["actions"]
        for turn in session["task_turns"]:
            clock = turn.get("event_clock") or {}
            clock_item_wall_by_kind.update(clock.get("by_kind_ms") or {})
            for boundary in clock.get("post_surgeon_boundaries") or []:
                post_surgeon_boundary_wall.append(boundary.get("boundary_ms") or 0)
                post_surgeon_reasoning_wall.append(boundary.get("model_reasoning_ms") or 0)
                post_surgeon_endpoints[boundary.get("next_kind") or "unknown"] += 1
                post_surgeon_transports[boundary.get("transport") or "unknown"] += 1
    return {
        "provider": provider,
        "sessions_in_window": len(sessions),
        "clojure_relevant_sessions": len(relevant),
        "skill_visible_relevant_sessions": sum(bool(s["skill_visible"]) for s in relevant),
        "activation_trigger_visible_sessions": sum(bool(s["activation_trigger_visible"]) for s in relevant),
        "skill_loaded_sessions": sum(bool(s["skill_loads"]) for s in relevant),
        "skill_loads": sum(s["skill_loads"] for s in relevant),
        "clj_surgeon_calls": sum(s["clj_surgeon_calls"] for s in relevant),
        "clj_surgeon_tool_actions": sum(s["clj_surgeon_tool_actions"] for s in relevant),
        "clj_surgeon_result_actions": sum(s["clj_surgeon_result_actions"] for s in relevant),
        "clj_surgeon_refusal_actions": sum(s["clj_surgeon_refusal_actions"] for s in relevant),
        "clj_surgeon_refusal_types": dict(sorted(refusal_types.items())),
        "clj_surgeon_execution_error_actions": sum(s["clj_surgeon_execution_error_actions"] for s in relevant),
        "clj_surgeon_output_chars": sum(s["clj_surgeon_output_chars"] for s in relevant),
        "clj_surgeon_action_wall": distribution(surgeon_wall),
        "native_apply_patch_action_wall": distribution(native_patch_wall),
        "clj_surgeon_ops": dict(sorted(ops.items())),
        "cclsp_calls": sum(cclsp_methods.values()),
        "cclsp_methods": dict(sorted(cclsp_methods.items())),
        "route_features": dict(sorted(route_features.items())),
        "route_action_kinds": dict(sorted(route_action_kinds.items())),
        "event_clock_item_wall_by_kind_ms": dict(sorted(clock_item_wall_by_kind.items())),
        "post_surgeon_boundary_wall": distribution(post_surgeon_boundary_wall),
        "post_surgeon_reasoning_wall": distribution(post_surgeon_reasoning_wall),
        "post_surgeon_endpoints": dict(sorted(post_surgeon_endpoints.items())),
        "post_surgeon_transports": dict(sorted(post_surgeon_transports.items())),
        "native_clojure_actions": dict(sorted(native.items())),
        "bounded_clojure_reads": sum(s["bounded_clojure_reads"] for s in relevant),
        "unbounded_clojure_reads": sum(s["unbounded_clojure_reads"] for s in relevant),
        "clojure_read_output_chars": sum(s["clojure_read_output_chars"] for s in relevant),
        "tool_input_chars": sum(s["tool_input_chars"] for s in relevant),
        "tool_output_chars": sum(s["tool_output_chars"] for s in relevant),
        "sessions": relevant,
    }


def value_time(value: dict) -> datetime | None:
    timestamp = value.get("timestamp")
    if not isinstance(timestamp, str):
        return None
    try:
        return parse_time(timestamp)
    except ValueError:
        return None


def in_window(value: dict, since: datetime, until: datetime) -> bool:
    timestamp = value_time(value)
    return timestamp is not None and since <= timestamp <= until


def default_surgeon_telemetry_roots() -> list[Path]:
    """The known-good Surgeon telemetry roots, scanned together when no
    explicit --surgeon-telemetry-root is given.

    Two conventions exist for this box and both are legitimate:
      - the MCP server's own default (mcp_telemetry.clj default-directory),
        used whenever a launcher (e.g. `make mcp-serve`) does not pass
        :telemetry-dir explicitly;
      - $MCP_STATE_DIR/telemetry, the Makefile launchd convention used by
        `make mcp-start` / `make mcp-serve-benchmark`.
    An absent root here does not mean zero usage; see collect_surgeon_telemetry.
    """
    home = Path.home()
    mcp_state_dir = Path(
        os.environ.get("MCP_STATE_DIR", str(home / ".local" / "state" / "clj-surgeon" / "mcp"))
    )
    return [
        home / ".local" / "state" / "clj-surgeon" / "telemetry",
        mcp_state_dir / "telemetry",
    ]


def collect_surgeon_telemetry(roots: Path | list[Path], since: datetime, until: datetime) -> dict:
    roots = [roots] if isinstance(roots, Path) else list(roots)
    roots_checked = [str(root) for root in roots]
    roots_present = [str(root) for root in roots if root.exists()]

    events = []
    seen_paths: set[str] = set()
    for root in roots:
        for path in candidate_files(root, "*.jsonl", since):
            try:
                resolved = str(path.resolve())
            except OSError:
                resolved = str(path)
            if resolved in seen_paths:
                continue
            seen_paths.add(resolved)
            events.extend(value for value in iter_jsonl(path) if in_window(value, since, until))

    starts = [event for event in events if event.get("event") == "server.start"]
    calls = [event for event in events if event.get("event") == "tool.call"]
    tools = Counter()
    outcomes = Counter()
    errors = Counter()
    operations = Counter()
    timings = []
    timings_by_tool: dict[str, list[int]] = {}
    file_reads = 0
    source_characters = 0
    inspect_requests = []
    inspect_files = []
    apply_edits = []
    apply_files = []
    for call in calls:
        tool = str(call.get("tool") or "unknown")
        tools[tool] += 1
        outcome = call.get("outcome") if isinstance(call.get("outcome"), dict) else {}
        response = call.get("response") if isinstance(call.get("response"), dict) else {}
        shape = call.get("request_shape") if isinstance(call.get("request_shape"), dict) else {}
        ok = outcome.get("ok") is True or response.get("ok") is True
        outcomes["ok" if ok else "refused"] += 1
        error_type = (
            response.get("error_type")
            or response.get("error-type")
            or outcome.get("error_type")
            or outcome.get("error-type")
        )
        if error_type:
            errors[str(error_type)] += 1
        for operation, count in (shape.get("operations") or {}).items():
            if isinstance(count, int):
                operations[str(operation)] += count
        total_ms = (call.get("timings_ms") or {}).get("total_ms")
        if isinstance(total_ms, (int, float)):
            elapsed = round(total_ms)
            timings.append(elapsed)
            timings_by_tool.setdefault(tool, []).append(elapsed)
        file_reads += int(outcome.get("file_reads") or 0)
        source_characters += int(outcome.get("source_characters") or 0)
        if tool == "inspect_clojure" and ok:
            inspect_requests.append(int(outcome.get("requests") or 0))
            inspect_files.append(int(outcome.get("files") or 0))
        if tool == "apply_clojure_changes" and ok:
            apply_edits.append(int(outcome.get("edits") or 0))
            apply_files.append(int(outcome.get("files") or 0))

    if not roots_present:
        status = "root-absent"
    elif events:
        status = "ok"
    else:
        status = "no-events"

    return {
        "status": status,
        "roots_checked": roots_checked,
        "roots_present": roots_present,
        "event_count": len(events),
        "server_starts": len(starts),
        "sessions": len({str(event.get("session_id")) for event in events if event.get("session_id")}),
        "mcp_tool_calls": len(calls),
        "tools": dict(sorted(tools.items())),
        "outcomes": dict(sorted(outcomes.items())),
        "error_types": dict(sorted(errors.items())),
        "inspect_operations": dict(sorted(operations.items())),
        "tool_wall": distribution(timings),
        "tool_wall_by_name": {
            tool: distribution(values) for tool, values in sorted(timings_by_tool.items())
        },
        "file_reads": file_reads,
        "source_characters_returned": source_characters,
        "inspect_request_batch": quantity_distribution(inspect_requests),
        "inspect_file_batch": quantity_distribution(inspect_files),
        "apply_edit_batch": quantity_distribution(apply_edits),
        "apply_file_batch": quantity_distribution(apply_files),
        "multi_edit_apply_calls": sum(value >= 2 for value in apply_edits),
        "multi_file_apply_calls": sum(value >= 2 for value in apply_files),
    }


def collect_cclsp_telemetry(path: Path, since: datetime, until: datetime) -> dict:
    events = [value for value in iter_jsonl(path) if in_window(value, since, until)]
    event_types = Counter(str(event.get("event") or "unknown") for event in events)
    workspaces = {
        stable_key(str(event.get("workspace_root")))
        for event in events
        if event.get("workspace_root")
    }
    sessions = {str(event.get("lsp_session")) for event in events if event.get("lsp_session")}
    lsp_methods = Counter()
    lsp_outcomes = Counter()
    lsp_outcomes_by_method: dict[str, Counter] = {}
    lsp_wall = []
    lsp_wall_by_method: dict[str, list[int]] = {}
    mcp_tools = Counter()
    mcp_statuses = Counter()
    mcp_wall = []
    mcp_wall_by_tool: dict[str, list[int]] = {}
    subject_request_keys = Counter()
    for event in events:
        event_type = event.get("event")
        if event_type in {
            "lsp_request_complete",
            "lsp_request_failed",
            "lsp_request_timeout",
        }:
            method = str(event.get("method") or "unknown")
            lsp_methods[method] += 1
            outcome = {
                "lsp_request_complete": "complete",
                "lsp_request_failed": "failed",
                "lsp_request_timeout": "timeout",
            }[event_type]
            lsp_outcomes[outcome] += 1
            lsp_outcomes_by_method.setdefault(method, Counter())[outcome] += 1
            elapsed = event.get("elapsed_ms")
            if isinstance(elapsed, (int, float)):
                rounded = round(elapsed)
                lsp_wall.append(rounded)
                lsp_wall_by_method.setdefault(method, []).append(rounded)
        elif event_type == "mcp_request_complete":
            tool = str(event.get("tool") or "unknown")
            mcp_tools[tool] += 1
            mcp_statuses[str(event.get("status") or "unknown")] += 1
            elapsed = event.get("elapsed_ms")
            if isinstance(elapsed, (int, float)):
                rounded = round(elapsed)
                mcp_wall.append(rounded)
                mcp_wall_by_tool.setdefault(tool, []).append(rounded)
            if event.get("workspace_root") and event.get("subject"):
                subject_request_keys[
                    stable_key(
                        "\u0000".join(
                            [
                                tool,
                                str(event.get("workspace_root")),
                                str(event.get("subject")),
                            ]
                        )
                    )
                ] += 1

    initialization_wall = lsp_wall_by_method.get("initialize", [])
    semantic_wall = [
        elapsed
        for method, values in lsp_wall_by_method.items()
        if method != "initialize"
        for elapsed in values
    ]
    repeated_subject_keys = {
        key: count for key, count in subject_request_keys.items() if count > 1
    }
    total_lsp_wall = sum(lsp_wall)
    total_mcp_wall = sum(mcp_wall)

    return {
        "status": "ok" if events else "no-events",
        "event_count": len(events),
        "events": dict(sorted(event_types.items())),
        "workspace_count": len(workspaces),
        "workspace_keys": sorted(workspaces),
        "lsp_sessions": len(sessions),
        "lsp_requests": sum(lsp_methods.values()),
        "lsp_methods": dict(sorted(lsp_methods.items())),
        "lsp_outcomes": dict(sorted(lsp_outcomes.items())),
        "lsp_outcomes_by_method": {
            method: dict(sorted(values.items()))
            for method, values in sorted(lsp_outcomes_by_method.items())
        },
        "lsp_wall": distribution(lsp_wall),
        "lsp_wall_by_method": {
            method: distribution(values) for method, values in sorted(lsp_wall_by_method.items())
        },
        "lsp_initialization_requests": len(initialization_wall),
        "lsp_initialization_wall": distribution(initialization_wall),
        "lsp_semantic_requests": len(semantic_wall),
        "lsp_semantic_wall": distribution(semantic_wall),
        "initialization_share_of_lsp_wall": (
            round(sum(initialization_wall) / total_lsp_wall, 4)
            if total_lsp_wall
            else None
        ),
        "initialization_to_cclsp_mcp_wall_ratio": (
            round(sum(initialization_wall) / total_mcp_wall, 4)
            if total_mcp_wall
            else None
        ),
        "document_syncs": event_types.get("lsp_document_sync", 0),
        "workspace_recoveries": event_types.get("lsp_workspace_recovered", 0),
        "cclsp_mcp_calls": sum(mcp_tools.values()),
        "cclsp_mcp_tools": dict(sorted(mcp_tools.items())),
        "cclsp_mcp_statuses": dict(sorted(mcp_statuses.items())),
        "cclsp_mcp_wall": distribution(mcp_wall),
        "cclsp_mcp_wall_by_tool": {
            tool: distribution(values)
            for tool, values in sorted(mcp_wall_by_tool.items())
        },
        "unfenced_subject_repeat_candidates": {
            "eligible_requests": sum(subject_request_keys.values()),
            "unique_request_keys": len(subject_request_keys),
            "repeated_request_keys": len(repeated_subject_keys),
            "repeat_requests_after_first": sum(
                count - 1 for count in repeated_subject_keys.values()
            ),
            "safe_cache_hits_claimed": 0,
        },
    }


def collect(args) -> dict:
    marker, marker_source = newest_marker(Path(args.observations_root))
    if args.since:
        since = parse_time(args.since)
        since_source = "argument"
    elif marker:
        since = marker
        since_source = marker_source
    else:
        return {
            "schema_version": SCHEMA,
            "status": "unavailable",
            "error": "No --since value and no agent-usage-window-end marker found",
        }
    until = parse_time(args.until) if args.until else datetime.now(timezone.utc)
    if until <= since:
        return {
            "schema_version": SCHEMA,
            "status": "error",
            "error": "--until must be later than --since",
        }

    codex_sessions = []
    for path in candidate_files(Path(args.codex_root), "rollout-*.json*", since):
        value = analyze_codex_file(path, since, until)
        if value:
            codex_sessions.append(value)

    claude_sessions = []
    for path in candidate_files(Path(args.claude_root), "*.jsonl", since):
        value = analyze_claude_file(path, since, until)
        if value:
            claude_sessions.append(value)

    return {
        "schema_version": SCHEMA,
        "status": "ok",
        "generated_at": iso_time(datetime.now(timezone.utc)),
        "window": {
            "since": iso_time(since),
            "until": iso_time(until),
            "since_source": since_source,
        },
        "privacy": {
            "transcript_prose_emitted": False,
            "workspace_paths_emitted": False,
            "session_keys_hashed": True,
            "raw_service_events_emitted": False,
            "structural_targets_hashed": True,
            "source_hashes_rehashed": True,
            "tool_argument_content_emitted": False,
            "tool_result_content_emitted": False,
            "tool_logical_arguments_hashed": True,
        },
        "providers": {
            "codex": provider_summary("codex", codex_sessions),
            "claude": provider_summary("claude", claude_sessions),
        },
        "services": {
            "clj_surgeon_mcp": collect_surgeon_telemetry(
                [Path(args.surgeon_telemetry_root)] if getattr(args, "surgeon_telemetry_root", None)
                else default_surgeon_telemetry_roots(),
                since, until,
            ),
            "cclsp_and_clojure_lsp": collect_cclsp_telemetry(
                Path(args.cclsp_log), since, until
            ),
        },
        "next_marker": f"<!-- agent-usage-window-end: {iso_time(until)} -->",
    }


def write_fixture(path: Path, events: list[dict]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text("".join(json.dumps(event) + "\n" for event in events), encoding="utf-8")


def self_test_registered_surgeon_alias() -> None:
    """Field regression: optional caller used registered server `surgeon`."""
    start = parse_time("2026-09-05T02:20:00Z")
    start_ms = round(start.timestamp() * 1000)
    arguments = {"workspace_root": "/PRIVATE/ALIAS/ROOT", "from": {"var": "PRIVATE_ALIAS_VAR"}}
    result = {"structuredContent": {"ok": True, "private": "PRIVATE_ALIAS_RESULT"}}
    with tempfile.TemporaryDirectory(prefix="study-agent-alias-") as tmp:
        for server in ["surgeon", "clj-surgeon", "clj_surgeon", "unrelated"]:
            method = f"mcp__{server}__alias_migration"
            path = Path(tmp) / f"rollout-{server}.jsonl"
            write_fixture(path, [
                {"timestamp": iso_time(start), "type": "event_msg", "payload": {"type": "task_started", "turn_id": "alias-turn"}},
                {"timestamp": "2026-09-05T02:20:01Z", "type": "response_item", "payload": {"type": "custom_tool_call", "name": "exec", "call_id": "alias-call", "input": f"const r = await tools.{method}({json.dumps(arguments)}); text(r);"}},
                {"timestamp": "2026-09-05T02:20:01.100Z", "type": "response_item", "payload": {"type": "custom_tool_call_output", "call_id": "alias-call", "output": json.dumps(result)}},
                {"timestamp": "2026-09-05T02:20:01.100Z", "type": "event_msg", "payload": {"type": "item_completed", "turn_id": "alias-turn", "started_at_ms": start_ms + 1000, "completed_at_ms": start_ms + 1100, "item": {"type": "McpToolCall", "server": server, "tool": "alias_migration", "arguments": arguments, "result": result, "status": "completed"}}},
                {"timestamp": "2026-09-05T02:20:02Z", "type": "event_msg", "payload": {"type": "task_complete", "turn_id": "alias-turn", "duration_ms": 2000}},
            ])
            session = analyze_codex_file(path, start, parse_time("2026-09-05T02:21:00Z"))
            known = server != "unrelated"
            assert session["clj_surgeon_calls"] == int(known), (server, "direct-call-count")
            assert session["clj_surgeon_ops"] == ({"alias_migration": 1} if known else {})
            assert session["clj_surgeon_result_actions"] == int(known)
            assert session["clj_surgeon_action_wall_samples_ms"] == ([100] if known else [])
            clock = session["task_turns"][0]["event_clock"]
            assert clock["by_kind_ms"].get("surgeon-apply", 0) == (100 if known else 0), (server, "completed-clock-apply")
            if known:
                assert session["route_phases"][0]["kinds"] == ["surgeon-apply"]
            assert "PRIVATE_ALIAS" not in json.dumps(session)
            assert "/PRIVATE/ALIAS/ROOT" not in json.dumps(session)
            evidence = compile_mcp_action_evidence({"server": server, "arguments": arguments, "result": result})
            assert ("result_canonical_bytes" in evidence) == known
        assert route_kinds('const note = "tools.mcp__surgeon__alias_migration({})";', "exec") == []
        assert route_kinds('await tools.mcp__unrelated__alias_migration({});', "exec") == []


def self_test_registered_mcp_read_wrapper() -> None:
    global REGISTERED_MCP_READ_WRAPPER
    previous = REGISTERED_MCP_READ_WRAPPER
    REGISTERED_MCP_READ_WRAPPER = "/registered/mcp_read.py"
    def invocation(command):
        return "text(await tools.exec_command({cmd:" + json.dumps(command) + "}));"
    try:
        command = "python3 /registered/mcp_read.py args.json --receipt receipt.json"
        source = invocation(command)
        assert registered_wrapper_ops(source) == ["inspect_clojure"]
        assert registered_wrapper_ops(source + source) == ["inspect_clojure"] * 2
        assert route_kinds(source, "shell") == ["surgeon-read"]
        session = empty_session("codex", Path("fixture.jsonl"))
        record_tool_text(session, source, "shell")
        assert session["clj_surgeon_calls"] == 1
        assert session["clj_surgeon_ops"] == {"inspect_clojure": 1}
        for bad in ["cat /registered/mcp_read.py", "echo " + command,
                    command + "; echo done", command + " --tool alias_migration",
                    command.replace("/registered/", "/unregistered/"),
                    command + " --port 7890", command + " --receipt duplicate"]:
            assert registered_wrapper_ops(invocation(bad)) == []
        for bad in ["const note=" + json.dumps(source), "// " + source,
                    "/* " + source + " */", "tools.exec_command({cmd: variable})"]:
            assert registered_wrapper_ops(bad) == []
        REGISTERED_MCP_READ_WRAPPER = None
        assert registered_wrapper_ops(source) == []
    finally:
        REGISTERED_MCP_READ_WRAPPER = previous


def self_test_public_wrapper() -> None:
    from unittest.mock import patch
    global REGISTERED_PUBLIC_WRAPPER
    previous = REGISTERED_PUBLIC_WRAPPER
    wrapper = "/registered/run_public.py"
    def invocation(command):
        return "text(await tools.exec_command({cmd:" + json.dumps(command) + "}));"
    command = "env TMPDIR=" + PUBLIC_TMPDIR + " python3 " + wrapper + " public-positive-01"
    try:
        REGISTERED_PUBLIC_WRAPPER = wrapper
        for case in PUBLIC_REQUEST_HASHES:
            assert registered_wrapper_ops(invocation(command.replace("public-positive-01", case))) == ["helper_extraction"]
        source = invocation(command)
        assert registered_wrapper_ops(invocation("python3 - <<'PY'\nprint('run_public.py')\nPY") + source) == ["helper_extraction"]
        for invalid in ["cat " + wrapper, "echo " + command, command + " extra",
                        command + "; echo done", command.replace("TMPDIR=", "OTHER="),
                        command.replace("positive-01", "positive-02"),
                        command.replace("run_public.py", "run_public_v2.py")]:
            assert registered_wrapper_ops(invocation(invalid)) == []
        for invalid in ["const note=" + json.dumps(source), "// " + source,
                        "tools.exec_command({cmd: dynamic})"]:
            assert registered_wrapper_ops(invalid) == []
        assert route_kinds(source, "shell") == ["surgeon-apply"]
        session = empty_session("codex", Path("fake.jsonl"))
        record_tool_text(session, source, "shell")
        assert session["clj_surgeon_calls"] == 1
        assert session["clj_surgeon_result_actions"] == 0
        assert session["clj_surgeon_ops"] == {"helper_extraction": 1}
        # No result supplied: attempted invocation cannot manufacture completion.
        REGISTERED_PUBLIC_WRAPPER = None
        assert registered_wrapper_ops(source) == []
        with patch.object(Path, "is_absolute", return_value=True), patch.object(Path, "is_symlink", return_value=False), patch.object(Path, "read_bytes", return_value=b"changed"):
            try:
                validate_public_wrapper(Path(wrapper))
                assert False, "changed wrapper admitted"
            except ValueError:
                pass
            with patch.dict(globals(), PUBLIC_WRAPPER_SHA256=hashlib.sha256(b"changed").hexdigest()):
                try:
                    validate_public_wrapper(Path(wrapper))
                    assert False, "changed request admitted"
                except ValueError:
                    pass
        for server in ["surgeon", "unknown"]:
            method = "tools.mcp__" + server + "__helper_extraction({})"
            assert route_kinds(method, "exec") == (["surgeon-apply"] if server == "surgeon" else [])
            assert route_kinds("const note=" + json.dumps(method), "exec") == []
            clock = completed_item_clock_sample({"started_at_ms": 1, "completed_at_ms": 2,
                "item": {"type": "McpToolCall", "server": server, "tool": "helper_extraction"}})
            assert (clock.get("operation") == "helper_extraction") == (server == "surgeon")
            if server == "surgeon":
                assert clock["kind"] == "surgeon-apply"
    finally:
        REGISTERED_PUBLIC_WRAPPER = previous


def self_test() -> int:
    self_test_public_wrapper()
    self_test_registered_mcp_read_wrapper()
    self_test_registered_surgeon_alias()
    assert [
        match.group(1)
        for match in SURGEON_RE.finditer(
            "bb -cp src -m clj-surgeon.core :op :change! :spec-file -"
        )
    ] == [":change!"]
    assert route_kinds(
        "bb -cp src -m clj-surgeon.core :op :change :spec-file -", "shell"
    ) == ["surgeon-plan"]
    assert route_kinds(
        "bb -cp src -m clj-surgeon.core :op :change! :spec-file -", "shell"
    ) == ["surgeon-apply"]
    assert route_kinds(
        "bb -cp src -m clj-surgeon.core :op :undo-change! :receipt r.edn", "shell"
    ) == ["surgeon-apply"]
    assert js_tool_methods(
        'await tools.apply_patch("docs mention tools.exec_command and clj-surgeon :op :cat")'
    ) == {"apply_patch"}
    assert js_tool_methods(
        'await tools.exec_command({cmd: "clj-surgeon :op :cat"})'
    ) == {"exec_command"}
    assert js_tool_methods(
        '// tools.write_stdin({})\nconst note = "tools.exec_command"; /* tools.apply_patch */'
    ) == set()
    assert js_tool_methods(
        'tools.mcp__clj-surgeon__inspect_clojure({})'
    ) == {"mcp__clj-surgeon__inspect_clojure"}
    clock_start = "2026-08-05T00:00:00Z"
    clock_start_ms = round(parse_time(clock_start).timestamp() * 1000)
    overlap_clock = compile_event_clock(clock_start, 1000, [
        {
            "kind": "model-reasoning",
            "started_at_ms": clock_start_ms - 100,
            "completed_at_ms": clock_start_ms + 600,
        },
        {
            "kind": "other-tool",
            "started_at_ms": clock_start_ms + 500,
            "completed_at_ms": clock_start_ms + 1200,
        },
    ])
    assert overlap_clock["measured_coverage_ms"] == 1000
    assert overlap_clock["unattributed_wall_ms"] == 0
    assert overlap_clock["coverage_ratio"] == 1.0
    assert overlap_clock["by_kind_ms"] == {
        "model-reasoning": 600,
        "other-tool": 500,
    }
    cli_sample = completed_item_clock_sample({
        "started_at_ms": clock_start_ms,
        "completed_at_ms": clock_start_ms + 10,
        "item": {
            "type": "CommandExecution",
            "command": "~/bin/clj-surgeon :op :cat :file /PRIVATE/CLI/PATH.clj :form f",
            "cwd": "/PRIVATE/CWD",
            "stdout": "PRIVATE_STDOUT",
        },
    })
    assert cli_sample == {
        "kind": "surgeon-read",
        "operation": ":cat",
        "transport": "cli",
        "invocation_count": 1,
        "started_at_ms": clock_start_ms,
        "completed_at_ms": clock_start_ms + 10,
    }
    assert completed_item_clock_sample({
        "started_at_ms": 2,
        "completed_at_ms": 1,
        "item": {"type": "Reasoning"},
    }) is None
    private_snapshot_hash = "a" * 64
    inspect_evidence = compile_inspect_clock_evidence(
        {
            "workspace_root": "/PRIVATE/WORKSPACE",
            "requests": [
                {
                    "id": "PRIVATE_REQUEST_ID",
                    "operation": "forms",
                    "file": "src/private.clj",
                    "forms": ["PRIVATE_OWNER"],
                    "expect": {"forms": 1},
                },
                {
                    "id": "PRIVATE_OUTLINE_ID",
                    "operation": "outline",
                    "file": "src/private.clj",
                },
            ],
            "expect": {"files": 1, "requests": 2},
        },
        {
            "structuredContent": {
                "file_hashes": {"src/private.clj": private_snapshot_hash},
                "results": [{"source": "PRIVATE_SOURCE_CANARY"}],
            },
        },
    )
    assert inspect_evidence == {
        "batch_cardinality": 2,
        "file_cardinality": 1,
        "request_operations": {"forms": 1, "outline": 1},
        "result_outcome": "unknown",
        "selector_cardinality": 1,
        "structural_target_sha256": "cd87f8ce80370f498648589d63c92dec2a5b9deac8760a7390440866fce90b73",
        "snapshot_sha256": "5adb8c3d42601f14dc3c467830d23dcc75ffae17236d02c6bb26c6e31b1c3c8e",
    }
    assert inspect_result_outcome(
        {"structuredContent": {"ok": True}}
    ) == "ok"
    assert inspect_result_outcome(
        {"structuredContent": {"ok": False}}
    ) == "refused"
    assert inspect_result_outcome({"isError": True}) == "error"
    assert "PRIVATE" not in json.dumps(inspect_evidence)
    assert private_snapshot_hash not in json.dumps(inspect_evidence)
    assert structural_target_relation(
        {"requests": [{"file": "PRIVATE_A", "forms": ["first"]}]},
        {"requests": [{"file": "PRIVATE_A", "forms": ["first"]}]},
    ) == "exact"
    assert structural_target_relation(
        {"requests": [{"file": "PRIVATE_A", "forms": ["first"]}]},
        {"requests": [{"file": "PRIVATE_A", "forms": ["second"]}]},
    ) == "same-files"
    assert structural_target_relation(
        {"requests": [{"files": ["PRIVATE_A", "PRIVATE_B"]}]},
        {"requests": [{"file": "PRIVATE_B"}]},
    ) == "overlapping-files"
    assert structural_target_relation(
        {"requests": [{"file": "PRIVATE_A"}]},
        {"requests": [{"file": "PRIVATE_B"}]},
    ) == "disjoint-files"
    assert structural_target_relation({}, {}) == "unknown"
    assert structural_target_relation({"requests": []}, None) == "unknown"
    relation_clock = compile_event_clock(clock_start, 1000, [
        {
            "kind": "surgeon-read",
            "operation": "inspect_clojure",
            "transport": "mcp",
            "action_ordinal": 1,
            "_structural_target": {
                "requests": [{"file": "PRIVATE_A", "forms": ["first"]}]
            },
            "started_at_ms": clock_start_ms,
            "completed_at_ms": clock_start_ms + 100,
        },
        {
            "kind": "surgeon-read",
            "operation": "inspect_clojure",
            "transport": "mcp",
            "action_ordinal": 2,
            "_structural_target": {
                "requests": [{"file": "PRIVATE_A", "forms": ["second"]}]
            },
            "started_at_ms": clock_start_ms + 200,
            "completed_at_ms": clock_start_ms + 300,
        },
    ])
    assert relation_clock["post_surgeon_boundaries"][0]["target_relation"] == "same-files"
    assert "PRIVATE_A" not in json.dumps(relation_clock)

    # Action-emission v6 privacy and compatibility witnesses. These fixtures
    # use only synthetic completed-item data; no provider history is read.
    private_source_hash = "f" * 64
    private_result_hash = "e" * 64
    private_arguments = {
        "workspace_root": "/PRIVATE/ROOT/ALPHA",
        "edits": [{
            "file": "src/PRIVATE_PATH.clj",
            "from": "PRIVATE_SOURCE_LITERAL",
            "to": "λ",
        }],
        "request_id": "PRIVATE_REQUEST_ID",
        "url": "https://private.example.invalid/path",
        "account": "PRIVATE_ACCOUNT",
        "secret": "PRIVATE_SECRET",
        "source_hash": private_source_hash,
    }
    private_result = {
        "structuredContent": {
            "source": "PRIVATE_RESULT_SOURCE",
            "diagnostic": "PRIVATE_RESULT_DIAGNOSTIC",
            "source_hash": private_result_hash,
        }
    }

    def synthetic_mcp_sample(arguments, *, result_marker="missing"):
        item = {
            "type": "McpToolCall",
            "server": "clj-surgeon",
            "tool": "edit_clojure",
            "arguments": arguments,
            "status": "completed",
        }
        if result_marker != "missing":
            item["result"] = result_marker
        return completed_item_clock_sample({
            "started_at_ms": clock_start_ms,
            "completed_at_ms": clock_start_ms + 10,
            "item": item,
        })

    private_sample = synthetic_mcp_sample(
        private_arguments, result_marker=private_result
    )
    private_evidence = private_sample["action_evidence"]
    expected_argument_json = json.dumps(
        private_arguments,
        sort_keys=True,
        separators=(",", ":"),
        ensure_ascii=False,
    )
    # UTF-8 byte count, not Python character count.
    assert private_evidence["argument_canonical_bytes"] == len(
        expected_argument_json.encode("utf-8")
    )
    assert private_evidence["argument_canonical_bytes"] > len(expected_argument_json)
    assert private_evidence["result_canonical_bytes"] == len(json.dumps(
        private_result,
        sort_keys=True,
        separators=(",", ":"),
        ensure_ascii=False,
    ).encode("utf-8"))

    short_root_sample = synthetic_mcp_sample({
        **private_arguments, "workspace_root": "/x"
    })
    long_root_sample = synthetic_mcp_sample({
        **private_arguments, "workspace_root": "/PRIVATE/MUCH/LONGER/ROOT"
    })
    assert (
        short_root_sample["action_evidence"]["argument_canonical_bytes"]
        != long_root_sample["action_evidence"]["argument_canonical_bytes"]
    )
    assert (
        short_root_sample["action_evidence"]["logical_argument_sha256"]
        == long_root_sample["action_evidence"]["logical_argument_sha256"]
    )
    changed_decision_sample = synthetic_mcp_sample({
        **private_arguments, "secret": "PRIVATE_DIFFERENT_DECISION"
    })
    assert (
        private_evidence["logical_argument_sha256"]
        != changed_decision_sample["action_evidence"]["logical_argument_sha256"]
    )
    assert len(private_evidence["logical_argument_sha256"]) == 64

    public_private_sample = json.dumps(private_sample)
    for canary in [
        "/PRIVATE/ROOT/ALPHA", "PRIVATE_PATH", "PRIVATE_SOURCE_LITERAL",
        "private.example", "PRIVATE_ACCOUNT", "PRIVATE_SECRET",
        "PRIVATE_REQUEST_ID", "PRIVATE_RESULT_SOURCE",
        "PRIVATE_RESULT_DIAGNOSTIC", private_source_hash, private_result_hash,
    ]:
        assert canary not in public_private_sample

    missing_result_evidence = synthetic_mcp_sample(
        private_arguments
    )["action_evidence"]
    null_result_evidence = synthetic_mcp_sample(
        private_arguments, result_marker=None
    )["action_evidence"]
    assert "result_canonical_bytes" not in missing_result_evidence
    assert null_result_evidence["result_canonical_bytes"] == 4

    endpoint_sample = synthetic_mcp_sample(
        {"workspace_root": "/PRIVATE/ENDPOINT", "edits": []},
        result_marker={"ok": True},
    )
    endpoint_sample.update({
        "action_ordinal": 7,
        "started_at_ms": clock_start_ms + 600,
        "completed_at_ms": clock_start_ms + 700,
    })
    emission_clock = compile_event_clock(clock_start, 1000, [
        {
            "kind": "surgeon-read",
            "transport": "mcp",
            "operation": "inspect_clojure",
            "action_ordinal": 1,
            "action_evidence": {"result_canonical_bytes": 91},
            "started_at_ms": clock_start_ms,
            "completed_at_ms": clock_start_ms + 100,
        },
        {
            "kind": "shell",
            "action_ordinal": 2,
            "started_at_ms": clock_start_ms + 50,
            "completed_at_ms": clock_start_ms + 350,
        },
        {
            "kind": "context-compaction",
            "action_ordinal": 3,
            "started_at_ms": clock_start_ms + 80,
            "completed_at_ms": clock_start_ms + 500,
        },
        {
            "kind": "model-reasoning",
            "action_ordinal": 4,
            "started_at_ms": clock_start_ms + 150,
            "completed_at_ms": clock_start_ms + 250,
        },
        {
            "kind": "model-reasoning",
            "action_ordinal": 5,
            "started_at_ms": clock_start_ms + 300,
            "completed_at_ms": clock_start_ms + 400,
        },
        {
            "kind": "model-reasoning",
            "action_ordinal": 6,
            "started_at_ms": clock_start_ms + 550,
            "completed_at_ms": clock_start_ms + 650,
        },
        endpoint_sample,
    ])
    emission_boundary = next(
        boundary
        for boundary in emission_clock["post_surgeon_boundaries"]
        if boundary.get("action_ordinal") == 1
    )
    assert emission_boundary["action_emission"] == {
        "previous_surgeon_result_canonical_bytes": 91,
        "next_argument_canonical_bytes": endpoint_sample[
            "action_evidence"
        ]["argument_canonical_bytes"],
        "next_logical_argument_sha256": endpoint_sample[
            "action_evidence"
        ]["logical_argument_sha256"],
        "last_reasoning_end_to_next_action_start_ms": 200,
        "overlapping_background_wall_ms": 400,
    }

    no_reasoning_clock = compile_event_clock(clock_start, 400, [
        {
            "kind": "surgeon-read",
            "transport": "mcp",
            "operation": "inspect_clojure",
            "action_ordinal": 1,
            "started_at_ms": clock_start_ms,
            "completed_at_ms": clock_start_ms + 100,
        },
        {
            "kind": "other-tool",
            "transport": "mcp",
            "action_ordinal": 2,
            "started_at_ms": clock_start_ms + 200,
            "completed_at_ms": clock_start_ms + 300,
        },
    ])
    no_reasoning_emission = no_reasoning_clock[
        "post_surgeon_boundaries"
    ][0]["action_emission"]
    assert "last_reasoning_end_to_next_action_start_ms" not in no_reasoning_emission
    assert "next_argument_canonical_bytes" not in no_reasoning_emission
    assert "next_logical_argument_sha256" not in no_reasoning_emission
    assert no_reasoning_emission["overlapping_background_wall_ms"] == 0

    malformed_mcp = completed_item_clock_sample({
        "started_at_ms": clock_start_ms,
        "completed_at_ms": clock_start_ms + 10,
        "item": {
            "type": "McpToolCall",
            "server": "clj-surgeon",
            "tool": "edit_clojure",
            "arguments": "PRIVATE_MALFORMED_ARGUMENTS",
        },
    })
    assert "action_evidence" not in malformed_mcp
    assert "action_evidence" not in completed_item_clock_sample({
        "started_at_ms": clock_start_ms,
        "completed_at_ms": clock_start_ms + 10,
        "item": {"type": "AgentMessage", "content": "PRIVATE_MESSAGE"},
    })
    assert "action_evidence" not in completed_item_clock_sample({
        "started_at_ms": clock_start_ms,
        "completed_at_ms": clock_start_ms + 10,
        "item": {"type": "FileChange", "changes": "PRIVATE_PATCH"},
    })
    assert "action_evidence" not in completed_item_clock_sample({
        "started_at_ms": clock_start_ms,
        "completed_at_ms": clock_start_ms + 10,
        "item": {"type": "CommandExecution", "command": "PRIVATE_COMMAND"},
    })

    # A v5-shaped clock remains accepted; unavailable evidence is omitted, not
    # rewritten as a synthetic zero.
    v5_clock = compile_event_clock(clock_start, 400, [
        {
            "kind": "surgeon-read",
            "action_ordinal": 1,
            "started_at_ms": clock_start_ms,
            "completed_at_ms": clock_start_ms + 100,
        },
        {
            "kind": "model-message",
            "action_ordinal": 2,
            "started_at_ms": clock_start_ms + 200,
            "completed_at_ms": clock_start_ms + 300,
        },
    ])
    v5_emission = v5_clock["post_surgeon_boundaries"][0]["action_emission"]
    assert v5_emission == {"overlapping_background_wall_ms": 0}
    turn_end_clock = compile_event_clock(clock_start, 400, [
        {
            "kind": "surgeon-read",
            "action_ordinal": 1,
            "started_at_ms": clock_start_ms,
            "completed_at_ms": clock_start_ms + 100,
        },
        {
            "kind": "model-reasoning",
            "action_ordinal": 2,
            "started_at_ms": clock_start_ms + 150,
            "completed_at_ms": clock_start_ms + 250,
        },
    ])
    assert "last_reasoning_end_to_next_action_start_ms" not in (
        turn_end_clock["post_surgeon_boundaries"][0]["action_emission"]
    )
    for canary in [
        "PRIVATE_MALFORMED_ARGUMENTS", "PRIVATE_MESSAGE", "PRIVATE_PATCH",
        "PRIVATE_COMMAND",
    ]:
        assert canary not in json.dumps({
            "private_sample": private_sample,
            "emission_clock": emission_clock,
            "no_reasoning_clock": no_reasoning_clock,
            "v5_clock": v5_clock,
        })
    with tempfile.TemporaryDirectory(prefix="study-agent-usage-") as tmp:
        root = Path(tmp)
        observations = root / "docs" / "observations"
        observations.mkdir(parents=True)
        (observations / "baseline.md").write_text(
            "<!-- agent-usage-window-end: 2026-08-05T00:00:00Z -->\n",
            encoding="utf-8",
        )
        write_fixture(
            root / "codex" / "rollout-test.jsonl",
            [
                {"timestamp": "2026-08-05T00:59:00Z", "type": "event_msg", "payload": {"type": "task_started", "turn_id": "turn-1"}},
                {"timestamp": "2026-08-05T00:59:30Z", "type": "event_msg", "payload": {"type": "user_message", "message": "private service goal"}},
                {"timestamp": "2026-08-05T01:00:00Z", "type": "response_item", "payload": {"type": "message", "role": "developer", "content": "clj-surgeon:"}},
                {"timestamp": "2026-08-05T01:00:15Z", "type": "event_msg", "payload": {"type": "item_completed", "turn_id": "turn-1", "started_at_ms": 1785891610000, "completed_at_ms": 1785891615000, "item": {"type": "Reasoning", "summary_text": "PRIVATE_REASONING_CANARY", "raw_content": "PRIVATE_RAW_CANARY"}}},
                {"timestamp": "2026-08-05T01:00:20.200Z", "type": "event_msg", "payload": {"type": "item_completed", "turn_id": "turn-1", "started_at_ms": 1785891620000, "completed_at_ms": 1785891620200, "item": {"type": "McpToolCall", "server": "clj-surgeon", "tool": "inspect_clojure", "arguments": {"workspace_root": "/PRIVATE/CLOCK/PATH", "requests": [{"id": "PRIVATE_CLOCK_REQUEST", "operation": "forms", "file": "src/private_clock.clj", "forms": ["PRIVATE_CLOCK_OWNER"]}]}, "result": {"structuredContent": {"file_hashes": {"src/private_clock.clj": "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"}, "results": [{"file": "src/private_clock.clj", "file_hash": "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", "source": "PRIVATE_RESULT_CANARY"}]}}, "status": "completed"}}},
                {"timestamp": "2026-08-05T01:00:22Z", "type": "event_msg", "payload": {"type": "item_completed", "turn_id": "turn-1", "started_at_ms": 1785891621000, "completed_at_ms": 1785891622000, "item": {"type": "AgentMessage", "phase": "commentary", "content": "PRIVATE_MESSAGE_CANARY"}}},
                {"timestamp": "2026-08-05T01:01:00Z", "type": "response_item", "payload": {"type": "custom_tool_call", "call_id": "c1", "name": "exec", "input": "await tools.exec_command({cmd:\"cat /x/clj-surgeon/SKILL.md\"})"}},
                {"timestamp": "2026-08-05T01:02:00Z", "type": "response_item", "payload": {"type": "custom_tool_call", "call_id": "c2", "name": "exec", "input": "await tools.exec_command({cmd:\"clj-surgeon :op :cat :file src/app.clj :form f\"})"}},
                {"timestamp": "2026-08-05T01:03:00Z", "type": "response_item", "payload": {"type": "custom_tool_call", "call_id": "c3", "name": "exec", "input": "await tools.apply_patch(\"*** src/other.clj\\n+ docs say clj-surgeon :op :xray\")"}},
                {"timestamp": "2026-08-05T01:03:15Z", "type": "response_item", "payload": {"type": "custom_tool_call", "call_id": "c4", "name": "exec", "input": "await tools.exec_command({cmd:\"bb -cp src -m clj-surgeon.core :op :change! :spec-file - :receipt-out receipt.edn\"})"}},
                {"timestamp": "2026-08-05T01:03:16Z", "type": "response_item", "payload": {"type": "custom_tool_call_output", "call_id": "c4", "output": "{:ok true :operation :change!}"}},
                {"timestamp": "2026-08-05T01:03:30Z", "type": "response_item", "payload": {"type": "custom_tool_call", "call_id": "c5", "name": "exec", "input": "await tools.mcp__cclsp__resolve_var_surface({subject:\"private.ns/secret\"})"}},
                {"timestamp": "2026-08-05T01:03:31Z", "type": "response_item", "payload": {"type": "custom_tool_call_output", "call_id": "c5", "output": "{\"ok\":true}"}},
                {"timestamp": "2026-08-05T01:04:00Z", "type": "event_msg", "payload": {"type": "task_complete", "duration_ms": 300000, "last_agent_message": "complete"}},
            ],
        )
        write_fixture(
            root / "claude" / "project" / "main.jsonl",
            [
                {"timestamp": "2026-08-05T01:00:00Z", "type": "attachment", "attachment": {"type": "skill_listing", "content": "- clj-surgeon: structural"}},
                {"timestamp": "2026-08-05T01:01:00Z", "type": "assistant", "message": {"content": [{"type": "tool_use", "id": "t1", "name": "Skill", "input": {"skill": "clj-surgeon"}}]}},
                {"timestamp": "2026-08-05T01:02:00Z", "type": "assistant", "message": {"content": [{"type": "tool_use", "id": "t2", "name": "Bash", "input": {"command": "clj-surgeon :op :xray :file src/app.clj :expr '(form f)'"}}]}},
                {"timestamp": "2026-08-05T01:02:30Z", "type": "assistant", "message": {"content": [{"type": "tool_use", "id": "t4", "name": "mcp__cclsp__find_references", "input": {"subject": "private.ns/secret"}}]}},
                {"timestamp": "2026-08-05T01:03:00Z", "type": "assistant", "message": {"content": [{"type": "tool_use", "id": "t3", "name": "Read", "input": {"file_path": "src/other.clj"}}]}},
            ],
        )
        write_fixture(
            root / "surgeon-telemetry" / "session.jsonl",
            [
                {"timestamp": "2026-08-05T01:10:00Z", "event": "server.start", "session_id": "private-session", "mcp_ready_ms": 100},
                {"timestamp": "2026-08-05T01:11:00Z", "event": "tool.call", "session_id": "private-session", "tool": "inspect_clojure", "request_shape": {"operations": {"forms": 2}}, "outcome": {"ok": True, "file_reads": 1, "source_characters": 40}, "timings_ms": {"total_ms": 12.4}, "request": {"private": "not emitted"}},
                {"timestamp": "2026-08-05T01:12:00Z", "event": "tool.call", "session_id": "private-session", "tool": "apply_clojure_changes", "request_shape": {}, "outcome": {"ok": False}, "response": {"error_type": "match-count-mismatch"}, "timings_ms": {"total_ms": 8.2}},
            ],
        )
        write_fixture(
            root / "cclsp.log",
            [
                {"timestamp": "2026-08-05T01:12:30Z", "event": "lsp_request_complete", "lsp_session": "private-lsp", "workspace_root": "/private/workspace", "method": "initialize", "elapsed_ms": 50},
                {"timestamp": "2026-08-05T01:13:00Z", "event": "lsp_request_complete", "lsp_session": "private-lsp", "workspace_root": "/private/workspace", "method": "workspace/symbol", "elapsed_ms": 25},
                {"timestamp": "2026-08-05T01:13:30Z", "event": "lsp_request_failed", "lsp_session": "private-lsp", "workspace_root": "/private/workspace", "method": "textDocument/documentSymbol", "elapsed_ms": 12},
                {"timestamp": "2026-08-05T01:14:00Z", "event": "lsp_request_timeout", "lsp_session": "private-lsp", "workspace_root": "/private/workspace", "method": "textDocument/references", "elapsed_ms": 30000},
                {"timestamp": "2026-08-05T01:15:00Z", "event": "mcp_request_complete", "workspace_root": "/private/workspace", "tool": "resolve_var_surface", "subject": "private.ns/secret", "status": "refused", "elapsed_ms": 30010},
                {"timestamp": "2026-08-05T01:16:00Z", "event": "mcp_request_complete", "workspace_root": "/private/workspace", "tool": "resolve_var_surface", "subject": "private.ns/secret", "status": "completed", "elapsed_ms": 30},
            ],
        )
        args = argparse.Namespace(
            since=None,
            until="2026-08-05T02:00:00Z",
            observations_root=str(observations),
            codex_root=str(root / "codex"),
            claude_root=str(root / "claude"),
            surgeon_telemetry_root=str(root / "surgeon-telemetry"),
            cclsp_log=str(root / "cclsp.log"),
        )
        receipt = collect(args)
        codex = receipt["providers"]["codex"]
        claude = receipt["providers"]["claude"]
        assert receipt["status"] == "ok"
        assert receipt["window"]["since"] == "2026-08-05T00:00:00Z"
        assert codex["clj_surgeon_ops"] == {":cat": 1, ":change!": 1}
        assert codex["clj_surgeon_tool_actions"] == 2
        assert codex["clj_surgeon_result_actions"] == 1
        assert codex["cclsp_methods"] == {"resolve_var_surface": 1}
        assert codex["skill_loads"] == 1
        assert codex["native_clojure_actions"] == {"apply_patch": 1}
        assert codex["route_action_kinds"] == {
            "native-patch": 1,
            "semantic-read": 1,
            "skill-load": 1,
            "surgeon-apply": 1,
            "surgeon-read": 1,
        }
        codex_phases = codex["sessions"][0]["route_phases"]
        assert [phase["kinds"] for phase in codex_phases] == [
            ["skill-load"], ["surgeon-read"], ["native-patch"],
            ["surgeon-apply"], ["semantic-read"]
        ]
        assert all(phase["actions"] == 1 for phase in codex_phases)
        assert codex["sessions"][0]["task_turns"][0]["route_phases"] == codex_phases
        clock = codex["sessions"][0]["task_turns"][0]["event_clock"]
        assert [item["kind"] for item in clock["items"][:5]] == [
            "unattributed-gap", "model-reasoning", "unattributed-gap",
            "surgeon-read", "unattributed-gap"
        ]
        assert [item["wall_ms"] for item in clock["items"][:5]] == [70000, 5000, 5000, 200, 800]
        assert clock["items"][3]["operation"] == "inspect_clojure"
        assert clock["items"][3]["status"] == "completed"
        assert clock["items"][3]["action_ordinal"] == 2
        assert clock["items"][3]["batch_cardinality"] == 1
        assert len(clock["items"][3]["structural_target_sha256"]) == 64
        assert len(clock["items"][3]["snapshot_sha256"]) == 64
        assert clock["items"][3]["action_evidence"][
            "argument_canonical_bytes"
        ] > 0
        assert len(clock["items"][3]["action_evidence"][
            "logical_argument_sha256"
        ]) == 64
        assert clock["items"][3]["action_evidence"][
            "result_canonical_bytes"
        ] > 0
        assert clock["by_kind_ms"]["model-reasoning"] == 5000
        assert clock["by_kind_ms"]["surgeon-read"] == 200
        assert clock["measured_coverage_ms"] == 6200
        assert clock["unattributed_wall_ms"] == 293800
        assert clock["coverage_ratio"] == 0.0207
        assert clock["post_surgeon_boundaries"] == [{
            "offset_ms": 80000,
            "operation": "inspect_clojure",
            "transport": "mcp",
            "status": "completed",
            "tool_wall_ms": 200,
            "boundary_ms": 800,
            "model_reasoning_ms": 0,
            "model_message_ms": 0,
            "next_kind": "model-message",
            "action_emission": {
                "previous_surgeon_result_canonical_bytes": clock[
                    "items"
                ][3]["action_evidence"]["result_canonical_bytes"],
                "overlapping_background_wall_ms": 0,
            },
            "action_ordinal": 2,
            "batch_cardinality": 1,
            "file_cardinality": 1,
            "request_operations": {"forms": 1},
            "result_outcome": "unknown",
            "selector_cardinality": 1,
            "structural_target_sha256": clock["items"][3]["structural_target_sha256"],
            "snapshot_sha256": clock["items"][3]["snapshot_sha256"],
            "next_action_ordinal": 3,
        }]
        assert codex["post_surgeon_boundary_wall"]["median_ms"] == 800
        assert codex["post_surgeon_transports"] == {"mcp": 1}
        rendered_clock = render_event_clock_receipt(receipt, top=1, minimum_ms=0)
        assert "model-reasoning" in rendered_clock
        assert "surgeon-read · mcp inspect_clojure completed" in rendered_clock
        assert "action#2 batch=1 target=" in rendered_clock
        assert "snapshot=" in rendered_clock
        assert "PRIVATE_" not in rendered_clock
        assert claude["clj_surgeon_ops"] == {":xray": 1}
        assert claude["activation_trigger_visible_sessions"] == 0
        assert claude["skill_loads"] == 1
        assert claude["cclsp_methods"] == {"find_references": 1}
        assert claude["native_clojure_actions"] == {"read": 1}
        assert claude["unbounded_clojure_reads"] == 1
        assert claude["bounded_clojure_reads"] == 0
        assert claude["route_action_kinds"] == {
            "native-read": 1,
            "semantic-read": 1,
            "skill-load": 1,
            "surgeon-read": 1,
        }
        assert receipt["privacy"]["transcript_prose_emitted"] is False
        assert receipt["privacy"]["structural_targets_hashed"] is True
        assert receipt["privacy"]["source_hashes_rehashed"] is True
        assert receipt["privacy"]["tool_argument_content_emitted"] is False
        assert receipt["privacy"]["tool_result_content_emitted"] is False
        assert receipt["privacy"]["tool_logical_arguments_hashed"] is True
        assert receipt["services"]["clj_surgeon_mcp"]["mcp_tool_calls"] == 2
        assert receipt["services"]["clj_surgeon_mcp"]["error_types"] == {
            "match-count-mismatch": 1
        }
        assert receipt["services"]["cclsp_and_clojure_lsp"]["lsp_outcomes"] == {
            "complete": 2,
            "failed": 1,
            "timeout": 1,
        }
        assert receipt["services"]["cclsp_and_clojure_lsp"]["lsp_outcomes_by_method"] == {
            "initialize": {"complete": 1},
            "textDocument/documentSymbol": {"failed": 1},
            "textDocument/references": {"timeout": 1},
            "workspace/symbol": {"complete": 1},
        }
        cclsp = receipt["services"]["cclsp_and_clojure_lsp"]
        assert cclsp["lsp_initialization_requests"] == 1
        assert cclsp["lsp_semantic_requests"] == 3
        assert cclsp["initialization_share_of_lsp_wall"] == 0.0017
        assert cclsp["initialization_to_cclsp_mcp_wall_ratio"] == 0.0017
        assert cclsp["cclsp_mcp_wall_by_tool"]["resolve_var_surface"]["count"] == 2
        assert cclsp["unfenced_subject_repeat_candidates"] == {
            "eligible_requests": 2,
            "unique_request_keys": 1,
            "repeated_request_keys": 1,
            "repeat_requests_after_first": 1,
            "safe_cache_hits_claimed": 0,
        }
        assert receipt["services"]["cclsp_and_clojure_lsp"]["workspace_count"] == 1
        assert "/private/workspace" not in json.dumps(receipt)
        assert "private service goal" not in json.dumps(receipt)
        assert "PRIVATE_REASONING_CANARY" not in json.dumps(receipt)
        assert "PRIVATE_RAW_CANARY" not in json.dumps(receipt)
        assert "PRIVATE_RESULT_CANARY" not in json.dumps(receipt)
        assert "PRIVATE_MESSAGE_CANARY" not in json.dumps(receipt)
        assert "/PRIVATE/CLOCK/PATH" not in json.dumps(receipt)
        chain_receipt = {
            "window": receipt["window"],
            "providers": {"codex": {"sessions": [{
                "session_key": "session-safe",
                "task_turns": [{
                    "turn_key": "turn-safe",
                    "event_clock": {"post_surgeon_boundaries": [
                        {"operation": "inspect_clojure", "transport": "mcp", "status": "completed", "boundary_ms": 7000, "model_reasoning_ms": 3000, "next_kind": "surgeon-read", "next_operation": "inspect_clojure", "next_transport": "mcp"},
                        {"operation": "inspect_clojure", "transport": "mcp", "status": "failed", "boundary_ms": 11000, "model_reasoning_ms": 8000, "next_kind": "surgeon-read", "next_operation": "inspect_clojure", "next_transport": "mcp"},
                        {"operation": "inspect_clojure", "transport": "mcp", "status": "completed", "boundary_ms": 9000, "model_reasoning_ms": 2000, "next_kind": "native-patch", "next_transport": "native"},
                    ]},
                }],
            }]}}
        }
        chains = compile_same_route_read_chains(chain_receipt)
        assert chains == [{
            "session_key": "session-safe",
            "turn_key": "turn-safe",
            "transport": "mcp",
            "calls": 3,
            "cumulative_boundary_ms": 18000,
            "max_boundary_ms": 11000,
            "model_reasoning_ms": 11000,
            "operations": ["inspect_clojure"],
            "failed_calls": 1,
        }]
        rendered_chains = render_read_chain_receipt(chain_receipt)
        assert "3 calls" in rendered_chains
        assert "boundary 18.000s" in rendered_chains
        assert "PRIVATE_" not in rendered_chains

        # Regression coverage for the false-zero fix: an absent telemetry
        # root must never be reported as "ok"/"no-events" with zero calls.
        surgeon_since = parse_time("2026-08-05T00:00:00Z")
        surgeon_until = parse_time("2026-08-05T02:00:00Z")
        roots_root = root / "surgeon-roots"
        server_default_root = roots_root / "server-default" / "telemetry"
        mcp_state_root = roots_root / "mcp-state" / "telemetry"

        # 1. Neither convention's directory exists -> root-absent, never a
        #    silent zero.
        absent = collect_surgeon_telemetry(
            [server_default_root, mcp_state_root], surgeon_since, surgeon_until
        )
        assert absent["status"] == "root-absent"
        assert absent["mcp_tool_calls"] == 0
        assert absent["roots_checked"] == [str(server_default_root), str(mcp_state_root)]
        assert absent["roots_present"] == []

        # 2. A root exists but is genuinely empty -> no-events, distinct from
        #    root-absent, with both roots_checked and roots_present reported.
        server_default_root.mkdir(parents=True)
        empty = collect_surgeon_telemetry(
            [server_default_root, mcp_state_root], surgeon_since, surgeon_until
        )
        assert empty["status"] == "no-events"
        assert empty["mcp_tool_calls"] == 0
        assert empty["roots_checked"] == [str(server_default_root), str(mcp_state_root)]
        assert empty["roots_present"] == [str(server_default_root)]

        # 3. One JSONL event in the server-default root, mcp/ root still
        #    absent (tonight's actual defect shape) -> ok, 1 call counted.
        write_fixture(
            server_default_root / "session.jsonl",
            [
                {
                    "timestamp": "2026-08-05T01:11:00Z",
                    "event": "tool.call",
                    "session_id": "roots-session",
                    "tool": "inspect_clojure",
                    "request_shape": {"operations": {"forms": 1}},
                    "outcome": {"ok": True, "file_reads": 1, "source_characters": 10},
                    "timings_ms": {"total_ms": 5.0},
                },
            ],
        )
        one_root_ok = collect_surgeon_telemetry(
            [server_default_root, mcp_state_root], surgeon_since, surgeon_until
        )
        assert one_root_ok["status"] == "ok"
        assert one_root_ok["mcp_tool_calls"] == 1
        assert one_root_ok["roots_present"] == [str(server_default_root)]

        # 4. The same underlying file reachable via both roots (e.g. one
        #    convention symlinked to the other) is counted once, not twice.
        mcp_state_root.parent.mkdir(parents=True, exist_ok=True)
        os.symlink(server_default_root, mcp_state_root)
        deduped = collect_surgeon_telemetry(
            [server_default_root, mcp_state_root], surgeon_since, surgeon_until
        )
        assert deduped["status"] == "ok"
        assert deduped["mcp_tool_calls"] == 1
        assert sorted(deduped["roots_present"]) == sorted(
            [str(server_default_root), str(mcp_state_root)]
        )
    print("study-agent-usage self-test passed")
    return 0


def parser() -> argparse.ArgumentParser:
    result = argparse.ArgumentParser(description=__doc__)
    result.add_argument("--registered-public-wrapper", help="Opt in to pinned 01 public wrapper and its two exact request files; invocation attempts only")
    result.add_argument("--registered-mcp-read-wrapper", help="Opt in to exact inspected mcp_read.py contract; source hash must match")
    result.add_argument("--since", help="ISO-8601 lower bound; defaults to newest observation marker")
    result.add_argument("--until", help="ISO-8601 upper bound; defaults to current UTC time")
    result.add_argument("--observations-root", default="docs/observations")
    result.add_argument("--codex-root", default=str(Path.home() / ".codex" / "sessions"))
    result.add_argument("--claude-root", default=str(Path.home() / ".claude" / "projects"))
    result.add_argument(
        "--surgeon-telemetry-root",
        default=None,
        help=(
            "Override to scan a single Surgeon telemetry root. Omit to scan the "
            "union of both known conventions: the MCP server's own default "
            "(~/.local/state/clj-surgeon/telemetry, used when a launcher does not "
            "pass :telemetry-dir) and $MCP_STATE_DIR/telemetry (default "
            "~/.local/state/clj-surgeon/mcp/telemetry, the Makefile launchd "
            "convention). Events found under both are deduplicated by resolved "
            "absolute file path."
        ),
    )
    result.add_argument(
        "--cclsp-log",
        default=str(Path.home() / ".local" / "state" / "clj-surgeon" / "cclsp" / "server.log"),
    )
    result.add_argument(
        "--receipt-out",
        help="Write the complete receipt here; defaults to a bounded file under /tmp",
    )
    result.add_argument(
        "--full",
        action="store_true",
        help="Also emit the complete receipt on stdout instead of the compact summary",
    )
    result.add_argument(
        "--render-receipt",
        help="Render privacy-safe Codex event clocks from an existing receipt",
    )
    result.add_argument(
        "--render-read-chains",
        help="Rank privacy-safe same-route Surgeon read chains from an existing receipt",
    )
    result.add_argument(
        "--read-chain-top", type=int, default=12,
        help="Maximum read chains to render (default: 12)",
    )
    result.add_argument("--session-key", help="Render only one hashed session key")
    result.add_argument("--turn-key", help="Render only one hashed task-turn key")
    result.add_argument(
        "--timeline-top", type=int, default=5,
        help="Maximum turns to render when --turn-key is absent (default: 5)",
    )
    result.add_argument(
        "--timeline-minimum-ms", type=int, default=250,
        help="Hide non-Surgeon timeline items shorter than this wall (default: 250)",
    )
    result.add_argument(
        "--timeline-around-surgeon", type=int, default=0,
        help="Show only N neighboring clock items around each Surgeon item",
    )
    result.add_argument(
        "--all-turns", action="store_true",
        help="Include turns with no Surgeon call in event-clock rendering",
    )
    result.add_argument("--pretty", action="store_true")
    result.add_argument("--self-test", action="store_true")
    return result


def default_receipt_path(receipt: dict) -> Path:
    window = receipt.get("window", {})

    def filename_time(value: object) -> str:
        return re.sub(r"[^0-9A-Za-z]+", "", str(value or "unknown"))

    since = filename_time(window.get("since"))
    until = filename_time(window.get("until"))
    return Path("/tmp") / f"clj-surgeon-agent-usage-{since}-{until}.json"


def compact_summary(receipt: dict, receipt_path: Path) -> dict:
    providers = {}
    for name, provider in receipt.get("providers", {}).items():
        sessions = provider.get("sessions", [])
        task_turns = [turn for session in sessions for turn in session.get("task_turns", [])]
        surgeon_turns = [turn for turn in task_turns if turn.get("clj_surgeon_calls", 0)]
        providers[name] = {
            "sessions_in_window": provider.get("sessions_in_window", 0),
            "clojure_relevant_sessions": provider.get("clojure_relevant_sessions", 0),
            "task_turns": len(task_turns),
            "surgeon_using_turns": len(surgeon_turns),
            "clj_surgeon_calls": provider.get("clj_surgeon_calls", 0),
            "clj_surgeon_ops": provider.get("clj_surgeon_ops", {}),
            "route_action_kinds": provider.get("route_action_kinds", {}),
            "post_surgeon_boundary_wall": provider.get("post_surgeon_boundary_wall", {}),
            "post_surgeon_reasoning_wall": provider.get("post_surgeon_reasoning_wall", {}),
            "post_surgeon_endpoints": provider.get("post_surgeon_endpoints", {}),
            "post_surgeon_transports": provider.get("post_surgeon_transports", {}),
        }
    return {
        "status": receipt.get("status"),
        "schema_version": receipt.get("schema_version"),
        "window": receipt.get("window"),
        "next_marker": receipt.get("next_marker"),
        "receipt_path": str(receipt_path),
        "privacy": receipt.get("privacy"),
        "providers": providers,
        "services": receipt.get("services", {}),
    }


def concise_duration(milliseconds: int | float) -> str:
    value = max(0, float(milliseconds or 0))
    if value < 1000:
        return f"{round(value)}ms"
    if value < 60000:
        return f"{value / 1000:.3f}s"
    return f"{int(value // 60000)}m{(value % 60000) / 1000:05.2f}s"


def compile_same_route_read_chains(receipt: dict) -> list[dict]:
    """Rank contiguous Surgeon-read chains without retaining source or prompt text."""
    chains = []
    for session in receipt.get("providers", {}).get("codex", {}).get("sessions", []):
        for turn in session.get("task_turns", []):
            current = None
            for boundary in turn.get("event_clock", {}).get("post_surgeon_boundaries", []):
                same_route_read = (
                    boundary.get("next_kind") == "surgeon-read"
                    and boundary.get("transport") == boundary.get("next_transport")
                )
                if same_route_read:
                    if current is None:
                        current = {
                            "session_key": session.get("session_key"),
                            "turn_key": turn.get("turn_key"),
                            "transport": boundary.get("transport"),
                            "calls": 1,
                            "cumulative_boundary_ms": 0,
                            "max_boundary_ms": 0,
                            "model_reasoning_ms": 0,
                            "operations": [],
                            "failed_calls": 0,
                        }
                    current["calls"] += 1
                    current["cumulative_boundary_ms"] += boundary.get("boundary_ms") or 0
                    current["max_boundary_ms"] = max(
                        current["max_boundary_ms"], boundary.get("boundary_ms") or 0
                    )
                    current["model_reasoning_ms"] += boundary.get("model_reasoning_ms") or 0
                    if boundary.get("operation"):
                        current["operations"].append(boundary["operation"])
                    if boundary.get("status") == "failed":
                        current["failed_calls"] += 1
                    continue
                if current is not None:
                    if boundary.get("operation"):
                        current["operations"].append(boundary["operation"])
                    if boundary.get("status") == "failed":
                        current["failed_calls"] += 1
                    current["operations"] = list(dict.fromkeys(current["operations"]))
                    chains.append(current)
                    current = None
            if current is not None:
                current["operations"] = list(dict.fromkeys(current["operations"]))
                chains.append(current)
    chains.sort(
        key=lambda chain: (
            chain["cumulative_boundary_ms"], chain["calls"], chain["max_boundary_ms"]
        ),
        reverse=True,
    )
    return chains


def render_read_chain_receipt(receipt: dict, *, top: int = 12) -> str:
    """Render a privacy-safe shortlist for compiled-read-mission research."""
    chains = compile_same_route_read_chains(receipt)
    lines = [
        f"Same-route Surgeon read chains · {receipt.get('window', {}).get('since')} → {receipt.get('window', {}).get('until')}",
        "A chain contains contiguous read→read boundaries on one transport; rank is opportunity, not proof that the reads were batchable.",
        "",
    ]
    for index, chain in enumerate(chains[:max(1, top)], start=1):
        operations = ",".join(chain["operations"]) or "unknown"
        lines.append(
            f"{index:>2}. session {chain['session_key']} · turn {chain['turn_key']} · "
            f"{chain['transport']} · {chain['calls']} calls · "
            f"boundary {concise_duration(chain['cumulative_boundary_ms'])} · "
            f"reasoning {concise_duration(chain['model_reasoning_ms'])} · "
            f"max {concise_duration(chain['max_boundary_ms'])} · "
            f"failed {chain['failed_calls']} · ops {operations}"
        )
    if not chains:
        lines.append("No contiguous same-route Surgeon read chains.")
    return "\n".join(lines)


def render_event_clock_receipt(
    receipt: dict,
    *,
    top: int = 5,
    minimum_ms: int = 250,
    session_key: str | None = None,
    turn_key: str | None = None,
    all_turns: bool = False,
    around_surgeon: int = 0,
) -> str:
    """Render only privacy-safe clock fields from one completed receipt."""
    candidates = []
    for session in receipt.get("providers", {}).get("codex", {}).get("sessions", []):
        if session_key and session.get("session_key") != session_key:
            continue
        for turn in session.get("task_turns", []):
            if turn_key and turn.get("turn_key") != turn_key:
                continue
            if not all_turns and not turn.get("clj_surgeon_calls"):
                continue
            if turn.get("event_clock"):
                candidates.append((session.get("session_key"), turn))
    candidates.sort(key=lambda value: value[1].get("duration_ms") or 0, reverse=True)
    if not turn_key:
        candidates = candidates[:max(1, top)]

    lines = [
        f"Agent event clock · {receipt.get('window', {}).get('since')} → {receipt.get('window', {}).get('until')}",
        "Measured reasoning is a Codex Reasoning item. Unattributed gaps can include inference, scheduling, transport, serialization, logging, or UI delay.",
        "",
    ]
    if not candidates:
        lines.append("No matching Codex turns with event-clock evidence.")
        return "\n".join(lines)

    for candidate_session_key, turn in candidates:
        clock = turn["event_clock"]
        by_kind = clock.get("by_kind_ms", {})
        surgeon_ms = sum(by_kind.get(kind, 0) for kind in (
            "surgeon-read", "surgeon-plan", "surgeon-apply"
        ))
        model_ms = by_kind.get("model-reasoning", 0) + by_kind.get("model-message", 0)
        lines.extend([
            f"session {candidate_session_key} · turn {turn.get('turn_key')} · {'complete' if turn.get('completed') else 'bounded-incomplete'}",
            "  " + " · ".join([
                f"wall {concise_duration(turn.get('duration_ms') or 0)}",
                f"model items {concise_duration(model_ms)}",
                f"Surgeon {concise_duration(surgeon_ms)}",
                f"unattributed {concise_duration(clock.get('unattributed_wall_ms') or 0)}",
                f"coverage {100 * (clock.get('coverage_ratio') or 0):.1f}%",
            ]),
        ])
        clock_items = clock.get("items", [])
        focus_indices = None
        if around_surgeon > 0:
            surgeon_indices = [
                index for index, item in enumerate(clock_items)
                if str(item.get("kind", "")).startswith("surgeon-")
            ]
            focus_indices = {
                neighbor
                for index in surgeon_indices
                for neighbor in range(
                    max(0, index - around_surgeon),
                    min(len(clock_items), index + around_surgeon + 1),
                )
            }
        omitted_count = 0
        omitted_wall = 0
        last_shown_index = None
        for index, item in enumerate(clock_items):
            keep = (
                index in focus_indices if focus_indices is not None else (
                    item.get("wall_ms", 0) >= minimum_ms
                    or str(item.get("kind", "")).startswith("surgeon-")
                    or item.get("kind") == "semantic-read"
                )
            )
            if not keep:
                omitted_count += 1
                omitted_wall += item.get("wall_ms", 0)
                continue
            if last_shown_index is None and index:
                lines.append(f"  … {index} earlier clock items omitted")
            elif last_shown_index is not None and index > last_shown_index + 1:
                lines.append(
                    f"  … {index - last_shown_index - 1} intervening clock items omitted"
                )
            detail = item.get("operation")
            if item.get("transport"):
                detail = f"{item['transport']} {detail or ''}".strip()
            if item.get("invocation_count", 0) > 1:
                detail = f"{detail or ''} x{item['invocation_count']}".strip()
            if item.get("status"):
                detail = f"{detail or ''} {item['status']}".strip()
            if item.get("phase"):
                detail = f"{detail or ''} {item['phase']}".strip()
            if item.get("action_ordinal") is not None:
                detail = f"{detail or ''} action#{item['action_ordinal']}".strip()
            if item.get("batch_cardinality") is not None:
                detail = (
                    f"{detail or ''} batch={item['batch_cardinality']}"
                ).strip()
            if item.get("structural_target_sha256"):
                detail = (
                    f"{detail or ''} target={item['structural_target_sha256'][:12]}"
                ).strip()
            if item.get("snapshot_sha256"):
                detail = (
                    f"{detail or ''} snapshot={item['snapshot_sha256'][:12]}"
                ).strip()
            suffix = f" · {detail}" if detail else ""
            lines.append(
                f"  +{concise_duration(item.get('offset_ms') or 0):>9}  "
                f"{concise_duration(item.get('wall_ms') or 0):>9}  "
                f"{item.get('kind')}{suffix}"
            )
            last_shown_index = index
        if last_shown_index is not None and last_shown_index < len(clock_items) - 1:
            lines.append(
                f"  … {len(clock_items) - last_shown_index - 1} later clock items omitted"
            )
        if omitted_count:
            lines.append(
                f"  … {omitted_count} items below {minimum_ms}ms omitted "
                f"({concise_duration(omitted_wall)} combined raw item wall)"
            )
        lines.append("")
    return "\n".join(lines).rstrip()


def main() -> int:
    args = parser().parse_args()
    global REGISTERED_MCP_READ_WRAPPER, REGISTERED_PUBLIC_WRAPPER
    if args.registered_public_wrapper:
        REGISTERED_PUBLIC_WRAPPER = validate_public_wrapper(Path(args.registered_public_wrapper))
    if args.registered_mcp_read_wrapper:
        wrapper = Path(args.registered_mcp_read_wrapper)
        if not wrapper.is_absolute() or wrapper.is_symlink() or hashlib.sha256(wrapper.read_bytes()).hexdigest() != MCP_READ_WRAPPER_SHA256:
            raise SystemExit("registered wrapper identity mismatch")
        REGISTERED_MCP_READ_WRAPPER = str(wrapper)
    if args.self_test:
        return self_test()
    if args.render_read_chains:
        receipt = json.loads(Path(args.render_read_chains).expanduser().read_text(encoding="utf-8"))
        print(render_read_chain_receipt(receipt, top=args.read_chain_top))
        return 0
    if args.render_receipt:
        receipt = json.loads(Path(args.render_receipt).expanduser().read_text(encoding="utf-8"))
        print(render_event_clock_receipt(
            receipt,
            top=args.timeline_top,
            minimum_ms=args.timeline_minimum_ms,
            session_key=args.session_key,
            turn_key=args.turn_key,
            all_turns=args.all_turns,
            around_surgeon=args.timeline_around_surgeon,
        ))
        return 0
    receipt = collect(args)
    if REGISTERED_MCP_READ_WRAPPER:
        receipt["wrapper_coverage"] = {"contract": "mcp-read-single-call-v1", "source_sha256": MCP_READ_WRAPPER_SHA256, "semantics": "literal invocation attempts, not RPC success or direct tool wall; unresolved wrappers omitted"}
    if REGISTERED_PUBLIC_WRAPPER:
        receipt["public_wrapper_coverage"] = {"contract": "public-01-request-pinned-v1",
            "collector_source_sha256": hashlib.sha256(Path(__file__).read_bytes()).hexdigest(),
            "source_sha256": PUBLIC_WRAPPER_SHA256,
            "request_sha256": sorted(PUBLIC_REQUEST_HASHES.values()),
            "semantics": "runner invocation attempts only; no completed RPC, proof success, or direct tool wall inferred"}
    receipt_path = Path(args.receipt_out).expanduser() if args.receipt_out else default_receipt_path(receipt)
    receipt_path.parent.mkdir(parents=True, exist_ok=True)
    receipt_path.write_text(json.dumps(receipt, indent=2, sort_keys=True) + "\n", encoding="utf-8")
    output = receipt if args.full else compact_summary(receipt, receipt_path)
    json.dump(output, sys.stdout, indent=2 if args.pretty else None, sort_keys=True)
    sys.stdout.write("\n")
    return 0 if receipt.get("status") == "ok" else 2


if __name__ == "__main__":
    raise SystemExit(main())
