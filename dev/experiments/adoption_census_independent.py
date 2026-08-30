#!/usr/bin/env python3
"""Independently fold retained Codex rollouts into a mutation-adoption census.

The output is privacy-safe: it emits counts, hashed session keys already present
in the study receipt, coarse repository classes, and refusal types. It never
emits prompts, commands, source, paths, or tool arguments.
"""

import argparse
import csv
import hashlib
import json
import os
import re
from collections import Counter, defaultdict
from datetime import datetime, timezone
from pathlib import Path


CLOJURE_EXTENSIONS = {".clj", ".cljc", ".cljs"}
SURGEON_OPERATIONS = ("inspect_clojure", "edit_clojure", "apply_clojure_changes")
SURGEON_CALL = {
    operation: re.compile(rf"tools\.mcp__clj_surgeon__{operation}\s*\(")
    for operation in SURGEON_OPERATIONS
}
PATCH_CALL = re.compile(r"tools\.apply_patch\s*\(")
PATCH_LITERAL = re.compile(r"\bconst\s+patch\s*=\s*(\"(?:\\.|[^\"\\])*\")\s*;")
PATCH_FILE = re.compile(r"^\*\*\* (Add|Update|Delete) File: (.+)$")
PATCH_MOVE = re.compile(r"^\*\*\* Move to: (.+)$")
REFUSAL = re.compile(r"\brefused\s+·\s+([A-Za-z0-9_-]+)")


def parse_instant(text):
    return datetime.fromisoformat(text.replace("Z", "+00:00"))


def sha256_file(path):
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for chunk in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def sha256_text(text):
    return hashlib.sha256(text.encode()).hexdigest()


def evidence_path(root, filename):
    match = re.match(r"rollout-(\d{4})-(\d{2})-(\d{2})T", filename)
    if not match:
        raise ValueError(f"unexpected evidence filename: {filename}")
    return root.joinpath(*match.groups(), filename)


def output_text(payload):
    pieces = []
    for item in payload.get("output") or []:
        if isinstance(item, dict) and isinstance(item.get("text"), str):
            pieces.append(item["text"])
    return "\n".join(pieces)


def extract_patch(source):
    match = PATCH_LITERAL.search(source)
    if not match:
        return None
    try:
        return json.loads(match.group(1))
    except json.JSONDecodeError:
        # JavaScript permits a backslash-newline continuation inside a quoted
        # string. JSON does not. Remove only that syntax and retry; any other
        # malformed literal remains unparsed and is counted explicitly.
        continued = re.sub(r"\\\\\r?\n", "", match.group(1))
        try:
            return json.loads(continued)
        except json.JSONDecodeError:
            return None


def strip_js_literals(source):
    """Replace JavaScript strings and comments while preserving executable tokens."""
    result = []
    index = 0
    state = "code"
    quote = None
    while index < len(source):
        char = source[index]
        following = source[index + 1] if index + 1 < len(source) else ""
        if state == "code":
            if char in {'"', "'", "`"}:
                state = "string"
                quote = char
                result.append(" ")
            elif char == "/" and following == "/":
                state = "line-comment"
                result.extend("  ")
                index += 1
            elif char == "/" and following == "*":
                state = "block-comment"
                result.extend("  ")
                index += 1
            else:
                result.append(char)
        elif state == "string":
            if char == "\\":
                result.append(" ")
                if following:
                    result.append("\n" if following == "\n" else " ")
                    index += 1
            elif char == quote:
                state = "code"
                quote = None
                result.append(" ")
            else:
                result.append("\n" if char == "\n" else " ")
        elif state == "line-comment":
            if char == "\n":
                state = "code"
                result.append("\n")
            else:
                result.append(" ")
        elif state == "block-comment":
            if char == "*" and following == "/":
                state = "code"
                result.extend("  ")
                index += 1
            else:
                result.append("\n" if char == "\n" else " ")
        index += 1
    return "".join(result)


def object_source_after(source, index):
    """Return one object literal starting at or after index, without evaluating JS."""
    while index < len(source) and source[index].isspace():
        index += 1
    if index >= len(source) or source[index] != "{":
        return None
    depth = 0
    in_string = False
    escaped = False
    for cursor in range(index, len(source)):
        char = source[cursor]
        if in_string:
            if escaped:
                escaped = False
            elif char == "\\":
                escaped = True
            elif char == '"':
                in_string = False
            continue
        if char == '"':
            in_string = True
        elif char == "{":
            depth += 1
        elif char == "}":
            depth -= 1
            if depth == 0:
                return source[index:cursor + 1]
    return None


def json_object_after(source, index):
    object_source = object_source_after(source, index)
    if not object_source:
        return None
    try:
        return json.loads(object_source)
    except json.JSONDecodeError:
        return None


def count_string_character(value, character):
    if isinstance(value, str):
        return value.count(character)
    if isinstance(value, list):
        return sum(count_string_character(item, character) for item in value)
    if isinstance(value, dict):
        return sum(count_string_character(item, character) for item in value.values())
    return 0


def workspace_class(arguments, object_source):
    workspace_root = arguments.get("workspace_root") if isinstance(arguments, dict) else None
    if not isinstance(workspace_root, str) and object_source:
        match = re.search(
            r'(?:"workspace_root"|workspace_root)\s*:\s*("(?:\\.|[^"\\])*")',
            object_source)
        if match:
            try:
                workspace_root = json.loads(match.group(1))
            except json.JSONDecodeError:
                workspace_root = None
    if not isinstance(workspace_root, str):
        return "missing"
    workspace = Path(workspace_root)
    if str(workspace).startswith(("/tmp/", "/private/tmp/")):
        return "temporary"
    if "clj-surgeon" in workspace.parts:
        return "clj-surgeon"
    return "other"


def normalized_target(cwd, target):
    path = Path(target)
    if not path.is_absolute():
        path = Path(cwd) / path
    return os.path.normpath(str(path))


def repository_class(cwd, targets):
    if "clj-surgeon" in Path(cwd).parts:
        return "clj-surgeon"
    if any("clj-surgeon" in Path(target).parts for target in targets):
        return "clj-surgeon"
    if any(Path(target).suffix in CLOJURE_EXTENSIONS for target in targets):
        return "other-clojure"
    return "other"


def src_or_test(cwd, target):
    try:
        relative = Path(target).relative_to(Path(cwd))
    except ValueError:
        relative = Path(target)
    return bool(relative.parts and relative.parts[0] in {"src", "test"})


def parse_patch_files(patch, cwd):
    files = []
    current = None
    for line in patch.splitlines():
        file_match = PATCH_FILE.match(line)
        if file_match:
            current = {
                "operation": file_match.group(1).lower(),
                "target": normalized_target(cwd, file_match.group(2)),
                "hunks": 0,
                "changed": [],
            }
            files.append(current)
            continue
        move_match = PATCH_MOVE.match(line)
        if move_match and current:
            current["move_to"] = normalized_target(cwd, move_match.group(1))
            continue
        if not current:
            continue
        if line.startswith("@@"):
            current["hunks"] += 1
        elif line.startswith(("+", "-")) and not line.startswith(("+++", "---")):
            current["changed"].append(line[1:])
    return files


def comment_only(lines):
    return bool(lines) and all(not line.strip() or line.lstrip().startswith(";")
                               for line in lines)


def tool_status(operation, text):
    refusal = REFUSAL.search(text)
    if refusal:
        return "refused", refusal.group(1).strip()
    if "Script failed" in text or "Script error:" in text:
        return "execution-error", None
    if operation in text and ("atomic commit complete" in text
                              or "verification_complete=true" in text
                              or re.search(r"\d+ edits?\s+·\s+\d+ files?", text)
                              or "all requests resolved" in text):
        return "success", None
    if "Script completed" in text and operation in text:
        return "completed-unknown", None
    return "unknown", None


def addressable_ladder(rows):
    successful = [row for row in rows if row["status"] == "success"]
    existing = [row for row in successful if row["existing_clojure"]]
    substantive = [row for row in existing
                   if not row["comment_only"] and not row["small_single_hunk"]]
    established_all = [row for row in substantive if not row["all_same_session_created"]]
    established_any = [row for row in substantive if not row["any_same_session_created"]]
    return {
        "all_successful_native_patches": len(successful),
        "updates_existing_clojure": len(existing),
        "comment_only": sum(row["comment_only"] for row in existing),
        "small_single_hunk_up_to_4_changed_lines": sum(
            row["small_single_hunk"] for row in existing),
        "trivial_union": sum(row["comment_only"] or row["small_single_hunk"]
                             for row in existing),
        "substantive_existing_clojure": len(substantive),
        "same_session_created_all_targets": sum(
            row["all_same_session_created"] for row in substantive),
        "same_session_created_any_target": sum(
            row["any_same_session_created"] for row in substantive),
        "established_files_all_target_rule": len(established_all),
        "established_files_any_target_rule": len(established_any),
        "established_all_targets_in_src_or_test": sum(
            row["all_src_or_test"] for row in established_all),
        "established_any_target_in_src_or_test": sum(
            row["any_src_or_test"] for row in established_all),
    }


def count_boundary(rows, count):
    ordered = sorted(rows, key=lambda row: row["timestamp"])
    if len(ordered) < count:
        return {"available": len(ordered), "timestamp": None, "next_timestamp": None}
    return {
        "available": len(ordered),
        "timestamp": ordered[count - 1]["timestamp"],
        "next_timestamp": ordered[count]["timestamp"] if len(ordered) > count else None,
    }


def ladder_by_repository(rows):
    return {
        repository: addressable_ladder([
            row for row in rows if row["repository_class"] == repository])
        for repository in sorted({row["repository_class"] for row in rows})
    }


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("receipt", type=Path)
    parser.add_argument("--sessions-root", type=Path, required=True)
    parser.add_argument("--output", type=Path)
    args = parser.parse_args()

    receipt = json.loads(args.receipt.read_text())
    since = parse_instant(receipt["window"]["since"])
    until = parse_instant(receipt["window"]["until"])
    sessions = receipt["providers"]["codex"]["sessions"]

    calls = []
    outputs = {}
    seen_call_ids = set()
    session_meta = {}
    evidence_hashes = {}
    missing_evidence = []

    for session in sessions:
        session_key = session["session_key"]
        path = evidence_path(args.sessions_root, session["evidence_file"])
        if not path.is_file():
            missing_evidence.append(session_key)
            continue
        evidence_digest = hashlib.sha256()
        with path.open() as handle:
            for line in handle:
                event = json.loads(line)
                event_timestamp_text = event.get("timestamp")
                if (not event_timestamp_text
                        or parse_instant(event_timestamp_text) < until):
                    evidence_digest.update(line.encode())
                if event.get("type") == "session_meta" and session_key not in session_meta:
                    original_started_at = event.get("payload", {}).get("timestamp")
                    session_meta[session_key] = {
                        "cwd": event.get("payload", {}).get("cwd") or "",
                        "thread_source": event.get("payload", {}).get("thread_source") or "unknown",
                        "started_in_window": bool(
                            original_started_at
                            and since <= parse_instant(original_started_at) < until),
                    }
                timestamp_text = event.get("timestamp")
                if not timestamp_text:
                    continue
                timestamp = parse_instant(timestamp_text)
                if timestamp < since or timestamp >= until:
                    continue
                if event.get("type") != "response_item":
                    continue
                payload = event.get("payload", {})
                if payload.get("type") == "custom_tool_call_output":
                    outputs[payload.get("call_id")] = output_text(payload)
                    continue
                if payload.get("type") != "custom_tool_call":
                    continue
                source = payload.get("input") or ""
                executable_source = strip_js_literals(source)
                call_id = payload.get("call_id")
                if call_id in seen_call_ids:
                    continue
                seen_call_ids.add(call_id)
                for operation, pattern in SURGEON_CALL.items():
                    for match in pattern.finditer(executable_source):
                        object_source = object_source_after(source, match.end())
                        arguments = json_object_after(source, match.end())
                        calls.append({
                            "kind": "surgeon",
                            "operation": operation,
                            "call_id": call_id,
                            "session_key": session_key,
                            "timestamp": timestamp.isoformat(),
                            "input_chars": len(source),
                            "escaped_backslashes": source.count("\\\\"),
                            "backslash_characters": source.count("\\"),
                            "argument_parsed": arguments is not None,
                            "argument_backslash_characters": count_string_character(
                                arguments, "\\"),
                            "argument_source_backslash_characters": (
                                object_source.count("\\") if object_source else 0),
                            "workspace_class": workspace_class(arguments, object_source),
                        })
                for _ in PATCH_CALL.finditer(executable_source):
                    calls.append({
                        "kind": "native-patch",
                        "call_id": call_id,
                        "session_key": session_key,
                        "timestamp": timestamp.isoformat(),
                        "input_chars": len(source),
                        "patch": extract_patch(source),
                    })
        evidence_hashes[session_key] = evidence_digest.hexdigest()

    calls.sort(key=lambda call: (call["timestamp"], call["session_key"]))
    created_by_session = defaultdict(set)
    patch_rows = []
    surgeon_rows = []
    for call in calls:
        meta = session_meta.get(call["session_key"], {
            "cwd": "", "thread_source": "unknown", "started_in_window": False})
        text = outputs.get(call["call_id"], "")
        if call["kind"] == "surgeon":
            status, refusal_type = tool_status(call["operation"], text)
            surgeon_rows.append({
                "_call_id": call["call_id"],
                "session_key": call["session_key"],
                "timestamp": call["timestamp"],
                "operation": call["operation"],
                "status": status,
                "refusal_type": refusal_type,
                "repository_class": repository_class(meta["cwd"], []),
                "thread_source": meta["thread_source"],
                "session_started_in_window": meta["started_in_window"],
                "input_chars": call["input_chars"],
                "escaped_backslashes": call["escaped_backslashes"],
                "backslash_characters": call["backslash_characters"],
                "argument_parsed": call["argument_parsed"],
                "argument_backslash_characters": call["argument_backslash_characters"],
                "argument_source_backslash_characters": call[
                    "argument_source_backslash_characters"],
                "workspace_class": call["workspace_class"],
            })
            continue

        patch = call["patch"]
        files = parse_patch_files(patch, meta["cwd"]) if patch else []
        targets = [entry["target"] for entry in files]
        updates = [entry for entry in files if entry["operation"] == "update"]
        clojure_updates = [entry for entry in updates
                           if Path(entry["target"]).suffix in CLOJURE_EXTENSIONS]
        prior_created = created_by_session[call["session_key"]]
        created_flags = [entry["target"] in prior_created for entry in clojure_updates]
        changed = [line for entry in clojure_updates for line in entry["changed"]]
        hunk_count = sum(entry["hunks"] for entry in clojure_updates)
        status = "failed" if "Script failed" in text or "Script error:" in text else "success"
        patch_rows.append({
            "_call_id": call["call_id"],
            "session_key": call["session_key"],
            "timestamp": call["timestamp"],
            "status": status,
            "patch_parsed": patch is not None,
            "repository_class": repository_class(meta["cwd"], targets),
            "thread_source": meta["thread_source"],
            "session_started_in_window": meta["started_in_window"],
            "file_count": len(files),
            "existing_clojure": bool(clojure_updates),
            "clojure_target_count": len(clojure_updates),
            "comment_only": comment_only(changed) if clojure_updates else False,
            "small_single_hunk": bool(clojure_updates and hunk_count == 1
                                      and len(changed) <= 4),
            "all_same_session_created": bool(created_flags and all(created_flags)),
            "any_same_session_created": any(created_flags),
            "all_src_or_test": bool(clojure_updates and all(
                src_or_test(meta["cwd"], entry["target"]) for entry in clojure_updates)),
            "any_src_or_test": any(src_or_test(meta["cwd"], entry["target"])
                                   for entry in clojure_updates),
            "changed_line_count": len(changed),
            "hunk_count": hunk_count,
        })
        if status == "success":
            for entry in files:
                if entry["operation"] == "add":
                    prior_created.add(entry["target"])
                elif entry["operation"] == "delete":
                    prior_created.discard(entry["target"])
                if entry.get("move_to"):
                    prior_created.discard(entry["target"])
                    prior_created.add(entry["move_to"])

    successful_patches = [row for row in patch_rows if row["status"] == "success"]
    existing = [row for row in successful_patches if row["existing_clojure"]]
    substantive = [row for row in existing
                   if not row["comment_only"] and not row["small_single_hunk"]]
    established_all = [row for row in substantive if not row["all_same_session_created"]]
    established_any = [row for row in substantive if not row["any_same_session_created"]]

    def count_by(rows, field):
        return dict(sorted(Counter(row[field] for row in rows).items()))

    def unique_outer_actions(rows, dimensions=()):
        by_call = {}
        for row in rows:
            key = (row["_call_id"], *(row[dimension] for dimension in dimensions))
            by_call.setdefault(key, row)
        return list(by_call.values())

    def public_rows(rows):
        return [{key: value for key, value in row.items() if not key.startswith("_")}
                for row in rows]

    surgeon_status = Counter((row["operation"], row["status"]) for row in surgeon_rows)
    surgeon_summary = {
        operation: {
            status: surgeon_status[(operation, status)]
            for status in ("success", "refused", "execution-error", "completed-unknown", "unknown")
            if surgeon_status[(operation, status)]
        }
        for operation in SURGEON_OPERATIONS
    }
    refusal_types = Counter(row["refusal_type"] for row in surgeon_rows
                            if row["refusal_type"])
    failed_edits = [row for row in surgeon_rows
                    if row["operation"] == "edit_clojure" and row["status"] != "success"]

    mutation_timeline = sorted(
        [{"session_key": row["session_key"], "timestamp": row["timestamp"],
          "route": "native-patch", "status": row["status"]}
         for row in patch_rows]
        + [{"session_key": row["session_key"], "timestamp": row["timestamp"],
            "route": row["operation"], "status": row["status"]}
           for row in surgeon_rows
           if row["operation"] in {"edit_clojure", "apply_clojure_changes"}],
        key=lambda row: (row["timestamp"], row["session_key"]))
    for failure in failed_edits:
        later = next((row for row in mutation_timeline
                      if row["session_key"] == failure["session_key"]
                      and row["timestamp"] > failure["timestamp"]), None)
        later_success = next((row for row in mutation_timeline
                              if row["session_key"] == failure["session_key"]
                              and row["timestamp"] > failure["timestamp"]
                              and row["status"] == "success"), None)
        failure["next_mutation_route"] = later["route"] if later else None
        failure["next_mutation_status"] = later["status"] if later else None
        failure["next_mutation_delay_seconds"] = (
            (parse_instant(later["timestamp"])
             - parse_instant(failure["timestamp"])).total_seconds()
            if later else None)
        failure["next_successful_mutation_route"] = (
            later_success["route"] if later_success else None)
        failure["next_successful_mutation_delay_seconds"] = (
            (parse_instant(later_success["timestamp"])
             - parse_instant(failure["timestamp"])).total_seconds()
            if later_success else None)

    patch_actions = unique_outer_actions(patch_rows)
    surgeon_actions = unique_outer_actions(surgeon_rows, dimensions=("operation",))
    start_patch_rows = [row for row in patch_rows if row["session_started_in_window"]]
    start_patch_actions = unique_outer_actions(start_patch_rows)
    start_surgeon_actions = unique_outer_actions([
        row for row in surgeon_rows if row["session_started_in_window"]],
        dimensions=("operation",))
    start_successes = sorted(
        [row for row in start_patch_actions if row["status"] == "success"],
        key=lambda row: row["timestamp"])
    reported_native_boundary = (
        start_successes[1144]["timestamp"] if len(start_successes) >= 1145 else None)
    boundary_patch_rows = [
        row for row in start_patch_actions
        if reported_native_boundary and row["timestamp"] <= reported_native_boundary]
    boundary_surgeon_rows = [
        row for row in surgeon_rows
        if row["session_started_in_window"]
        and reported_native_boundary
        and row["timestamp"] <= reported_native_boundary]

    report = {
        "schema": "clj-surgeon.adoption-census-independent.v1",
        "window": receipt["window"],
        "source_receipt_sha256": sha256_file(args.receipt),
        "session_count": len(sessions),
        "missing_evidence_sessions": missing_evidence,
        "evidence_file_count": len(evidence_hashes),
        "evidence_manifest_sha256": sha256_text("".join(
            f"{session_key}\t{evidence_hashes[session_key]}\n"
            for session_key in sorted(evidence_hashes))),
        "native_patch": {
            "attempted_actions": len(patch_rows),
            "successful_actions": len(successful_patches),
            "failed_actions": len(patch_rows) - len(successful_patches),
            "parsed_actions": sum(row["patch_parsed"] for row in patch_rows),
            "repository_classes": count_by(successful_patches, "repository_class"),
            "thread_sources": count_by(successful_patches, "thread_source"),
            "successful_by_session": dict(sorted(Counter(
                row["session_key"] for row in successful_patches).items())),
            "parsed_successful_by_session": dict(sorted(Counter(
                row["session_key"] for row in successful_patches if row["patch_parsed"]).items())),
            "outer_attempted_actions": len(patch_actions),
            "outer_successful_actions": sum(
                row["status"] == "success" for row in patch_actions),
            "outer_failed_actions": sum(
                row["status"] != "success" for row in patch_actions),
            "outer_successful_by_session": dict(sorted(Counter(
                row["session_key"] for row in patch_actions
                if row["status"] == "success").items())),
        },
        "addressable_action_ladder": addressable_ladder(patch_rows),
        "session_start_population": {
            "population_rule": (
                "first session_meta timestamp is inside the frozen half-open window"),
            "session_count": sum(meta["started_in_window"] for meta in session_meta.values()),
            "native_patch": addressable_ladder([
                row for row in patch_rows if row["session_started_in_window"]]),
            "native_patch_outer_actions": addressable_ladder(start_patch_actions),
            "native_patch_outer_actions_by_repository": ladder_by_repository(
                start_patch_actions),
            "native_patch_by_repository": ladder_by_repository([
                row for row in patch_rows if row["session_started_in_window"]]),
            "native_patch_attempted": sum(
                row["session_started_in_window"] for row in patch_rows),
            "native_patch_failed": sum(
                row["session_started_in_window"] and row["status"] != "success"
                for row in patch_rows),
            "native_patch_unparsed": sum(
                row["session_started_in_window"] and not row["patch_parsed"]
                for row in patch_rows),
            "surgeon_operation_counts": dict(sorted(Counter(
                row["operation"] for row in surgeon_rows
                if row["session_started_in_window"]).items())),
            "surgeon_outer_action_counts": dict(sorted(Counter(
                row["operation"] for row in start_surgeon_actions).items())),
            "surgeon_operation_statuses": {
                operation: dict(sorted(Counter(
                    row["status"] for row in surgeon_rows
                    if row["session_started_in_window"]
                    and row["operation"] == operation).items()))
                for operation in SURGEON_OPERATIONS
            },
            "edit_calls": public_rows([
                row for row in surgeon_rows
                if row["session_started_in_window"]
                and row["operation"] == "edit_clojure"
            ]),
            "reported_count_boundaries": {
                "native_success_1145": count_boundary([
                    row for row in start_patch_actions if row["status"] == "success"
                ], 1145),
                "edit_calls_6": count_boundary([
                    row for row in surgeon_rows
                    if row["session_started_in_window"]
                    and row["operation"] == "edit_clojure"
                ], 6),
                "inspect_calls_400": count_boundary([
                    row for row in surgeon_rows
                    if row["session_started_in_window"]
                    and row["operation"] == "inspect_clojure"
                ], 400),
            },
            "at_reported_native_1145_boundary": {
                "until": reported_native_boundary,
                "native_patch": addressable_ladder(boundary_patch_rows),
                "surgeon_operation_counts": dict(sorted(Counter(
                    row["operation"] for row in boundary_surgeon_rows).items())),
            },
        },
        "substantive_existing_clojure_by_repository": count_by(
            substantive, "repository_class"),
        "established_existing_clojure_by_repository": count_by(
            established_all, "repository_class"),
        "surgeon": {
            "call_count": len(surgeon_rows),
            "by_operation_and_status": surgeon_summary,
            "repository_classes": count_by(surgeon_rows, "repository_class"),
            "thread_sources": count_by(surgeon_rows, "thread_source"),
            "operations_by_session": {
                session_key: dict(sorted(Counter(
                    row["operation"] for row in surgeon_rows
                    if row["session_key"] == session_key).items()))
                for session_key in sorted({row["session_key"] for row in surgeon_rows})
            },
            "outer_operation_counts": dict(sorted(Counter(
                row["operation"] for row in surgeon_actions).items())),
            "outer_operations_by_session": {
                session_key: dict(sorted(Counter(
                    row["operation"] for row in surgeon_actions
                    if row["session_key"] == session_key).items()))
                for session_key in sorted({row["session_key"] for row in surgeon_actions})
            },
            "refusal_types": dict(sorted(refusal_types.items())),
            "failed_edit_count": len(failed_edits),
            "failed_edits": public_rows(failed_edits),
        },
        "privacy": {
            "paths_emitted": False,
            "commands_emitted": False,
            "source_emitted": False,
            "tool_arguments_emitted": False,
        },
    }
    rendered = json.dumps(report, indent=2, sort_keys=True) + "\n"
    if args.output:
        args.output.write_text(rendered)
    print(rendered, end="")


if __name__ == "__main__":
    main()
