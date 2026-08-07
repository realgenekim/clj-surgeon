#!/usr/bin/env python3
"""Collect bounded Codex and Claude Code usage evidence without transcript prose."""

from __future__ import annotations

import argparse
import hashlib
import json
import os
import re
import sys
import tempfile
from collections import Counter
from datetime import datetime, timezone
from pathlib import Path


SCHEMA = "clj-surgeon.agent-usage-ethnography.v2"
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
    "live-probe": 6,
    "verify": 7,
    "git": 8,
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
            match = re.match(r"tools\.([A-Za-z_][A-Za-z0-9_]*)", source[index:])
            if match:
                methods.add(match.group(1))
                index += len(match.group(0))
                continue
        index += 1
    return methods


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


def route_kinds(text: str, action: str) -> list[str]:
    """Classify one outer tool action without retaining command or path text."""
    if action == "apply_patch":
        return ["native-patch"] if CLJ_PATH_RE.search(text) else []
    kinds = set()
    matches = list(SURGEON_RE.finditer(text))
    ops = {match.group(1) or ":help" for match in matches}
    if SKILL_LOAD_RE.search(text) or action == "skill-load":
        kinds.add("skill-load")
    if ops:
        if ops & SURGEON_APPLY_OPS or (":edit" in ops and ":expect" in text):
            kinds.add("surgeon-apply")
        if ops & SURGEON_PLAN_OPS and not (":edit" in ops and ":expect" in text):
            kinds.add("surgeon-plan")
        if ops & SURGEON_READ_OPS:
            kinds.add("surgeon-read")
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


def finalize_turn(turn: dict) -> dict:
    result = dict(turn)
    samples = result.pop("clj_surgeon_action_wall_ms")
    prompts = result.pop("_user_messages")
    final_message = result.pop("_final_message")
    route_actions = result.pop("_route_actions")
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
    if matches:
        session["clj_surgeon_tool_actions"] += 1
        session["clj_surgeon_calls"] += len(matches)
        for match in matches:
            session["clj_surgeon_ops"][match.group(1) or ":help"] += 1
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
            surgeon_action = bool(SURGEON_RE.search(command_text))
            surgeon_call_count = len(SURGEON_RE.findall(command_text))
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
            route_text = tool_input if action == "apply_patch" else command_text
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
                tool_input if action == "apply_patch" else command_text,
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
            if name == "Bash":
                route_text = str(tool_input.get("command") or "")
                route_action_name = "shell"
            elif name in {"Read", "Edit", "Write"}:
                route_text = str(tool_input.get("file_path") or "")
                route_action_name = name.lower()
            else:
                route_text = rendered
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
        or sum(session["native_clojure_actions"].values())
    ]
    ops = Counter()
    native = Counter()
    route_features = Counter()
    route_action_kinds = Counter()
    refusal_types = Counter()
    surgeon_wall = []
    native_patch_wall = []
    for session in relevant:
        ops.update(session["clj_surgeon_ops"])
        native.update(session["native_clojure_actions"])
        route_features.update(session["route_features"])
        refusal_types.update(session["clj_surgeon_refusal_types"])
        surgeon_wall.extend(session["clj_surgeon_action_wall_samples_ms"])
        native_patch_wall.extend(session["native_apply_patch_action_wall_samples_ms"])
        for phase in session["route_phases"]:
            for kind in phase["kinds"]:
                route_action_kinds[kind] += phase["actions"]
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
        "route_features": dict(sorted(route_features.items())),
        "route_action_kinds": dict(sorted(route_action_kinds.items())),
        "native_clojure_actions": dict(sorted(native.items())),
        "bounded_clojure_reads": sum(s["bounded_clojure_reads"] for s in relevant),
        "unbounded_clojure_reads": sum(s["unbounded_clojure_reads"] for s in relevant),
        "clojure_read_output_chars": sum(s["clojure_read_output_chars"] for s in relevant),
        "tool_input_chars": sum(s["tool_input_chars"] for s in relevant),
        "tool_output_chars": sum(s["tool_output_chars"] for s in relevant),
        "sessions": relevant,
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
        },
        "providers": {
            "codex": provider_summary("codex", codex_sessions),
            "claude": provider_summary("claude", claude_sessions),
        },
        "next_marker": f"<!-- agent-usage-window-end: {iso_time(until)} -->",
    }


def write_fixture(path: Path, events: list[dict]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text("".join(json.dumps(event) + "\n" for event in events), encoding="utf-8")


def self_test() -> int:
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
                {"timestamp": "2026-08-05T01:01:00Z", "type": "response_item", "payload": {"type": "custom_tool_call", "call_id": "c1", "name": "exec", "input": "await tools.exec_command({cmd:\"cat /x/clj-surgeon/SKILL.md\"})"}},
                {"timestamp": "2026-08-05T01:02:00Z", "type": "response_item", "payload": {"type": "custom_tool_call", "call_id": "c2", "name": "exec", "input": "await tools.exec_command({cmd:\"clj-surgeon :op :cat :file src/app.clj :form f\"})"}},
                {"timestamp": "2026-08-05T01:03:00Z", "type": "response_item", "payload": {"type": "custom_tool_call", "call_id": "c3", "name": "exec", "input": "await tools.apply_patch(\"*** src/other.clj\\n+ docs say clj-surgeon :op :xray\")"}},
                {"timestamp": "2026-08-05T01:03:15Z", "type": "response_item", "payload": {"type": "custom_tool_call", "call_id": "c4", "name": "exec", "input": "await tools.exec_command({cmd:\"bb -cp src -m clj-surgeon.core :op :change! :spec-file - :receipt-out receipt.edn\"})"}},
                {"timestamp": "2026-08-05T01:03:16Z", "type": "response_item", "payload": {"type": "custom_tool_call_output", "call_id": "c4", "output": "{:ok true :operation :change!}"}},
                {"timestamp": "2026-08-05T01:04:00Z", "type": "event_msg", "payload": {"type": "task_complete", "duration_ms": 300000, "last_agent_message": "complete"}},
            ],
        )
        write_fixture(
            root / "claude" / "project" / "main.jsonl",
            [
                {"timestamp": "2026-08-05T01:00:00Z", "type": "attachment", "attachment": {"type": "skill_listing", "content": "- clj-surgeon: structural"}},
                {"timestamp": "2026-08-05T01:01:00Z", "type": "assistant", "message": {"content": [{"type": "tool_use", "id": "t1", "name": "Skill", "input": {"skill": "clj-surgeon"}}]}},
                {"timestamp": "2026-08-05T01:02:00Z", "type": "assistant", "message": {"content": [{"type": "tool_use", "id": "t2", "name": "Bash", "input": {"command": "clj-surgeon :op :xray :file src/app.clj :expr '(form f)'"}}]}},
                {"timestamp": "2026-08-05T01:03:00Z", "type": "assistant", "message": {"content": [{"type": "tool_use", "id": "t3", "name": "Read", "input": {"file_path": "src/other.clj"}}]}},
            ],
        )
        args = argparse.Namespace(
            since=None,
            until="2026-08-05T02:00:00Z",
            observations_root=str(observations),
            codex_root=str(root / "codex"),
            claude_root=str(root / "claude"),
        )
        receipt = collect(args)
        codex = receipt["providers"]["codex"]
        claude = receipt["providers"]["claude"]
        assert receipt["status"] == "ok"
        assert receipt["window"]["since"] == "2026-08-05T00:00:00Z"
        assert codex["clj_surgeon_ops"] == {":cat": 1, ":change!": 1}
        assert codex["clj_surgeon_tool_actions"] == 2
        assert codex["clj_surgeon_result_actions"] == 1
        assert codex["skill_loads"] == 1
        assert codex["native_clojure_actions"] == {"apply_patch": 1}
        assert codex["route_action_kinds"] == {
            "native-patch": 1,
            "skill-load": 1,
            "surgeon-apply": 1,
            "surgeon-read": 1,
        }
        codex_phases = codex["sessions"][0]["route_phases"]
        assert [phase["kinds"] for phase in codex_phases] == [
            ["skill-load"], ["surgeon-read"], ["native-patch"],
            ["surgeon-apply"]
        ]
        assert all(phase["actions"] == 1 for phase in codex_phases)
        assert codex["sessions"][0]["task_turns"][0]["route_phases"] == codex_phases
        assert claude["clj_surgeon_ops"] == {":xray": 1}
        assert claude["activation_trigger_visible_sessions"] == 0
        assert claude["skill_loads"] == 1
        assert claude["native_clojure_actions"] == {"read": 1}
        assert claude["unbounded_clojure_reads"] == 1
        assert claude["bounded_clojure_reads"] == 0
        assert claude["route_action_kinds"] == {
            "native-read": 1,
            "skill-load": 1,
            "surgeon-read": 1,
        }
        assert receipt["privacy"]["transcript_prose_emitted"] is False
        assert "private service goal" not in json.dumps(receipt)
    print("study-agent-usage self-test passed")
    return 0


def parser() -> argparse.ArgumentParser:
    result = argparse.ArgumentParser(description=__doc__)
    result.add_argument("--since", help="ISO-8601 lower bound; defaults to newest observation marker")
    result.add_argument("--until", help="ISO-8601 upper bound; defaults to current UTC time")
    result.add_argument("--observations-root", default="docs/observations")
    result.add_argument("--codex-root", default=str(Path.home() / ".codex" / "sessions"))
    result.add_argument("--claude-root", default=str(Path.home() / ".claude" / "projects"))
    result.add_argument("--pretty", action="store_true")
    result.add_argument("--self-test", action="store_true")
    return result


def main() -> int:
    args = parser().parse_args()
    if args.self_test:
        return self_test()
    receipt = collect(args)
    json.dump(receipt, sys.stdout, indent=2 if args.pretty else None, sort_keys=True)
    sys.stdout.write("\n")
    return 0 if receipt.get("status") == "ok" else 2


if __name__ == "__main__":
    raise SystemExit(main())
