#!/usr/bin/env python3
"""Fold bounded Codex rollouts into a privacy-safe external write-shape census.

This extends the independent adoption-census lineage with task-turn ownership,
per-hunk edit shapes, per-mission subject repetition, and exact repository
confinement. Output never includes repository paths, session identifiers,
commands, prompts, source, patches, or tool arguments.
"""

import argparse
import glob
import hashlib
import json
import os
import re
from collections import Counter, defaultdict
from pathlib import Path

from adoption_census_independent import (
    CLOJURE_EXTENSIONS,
    REFUSAL,
    SURGEON_CALL,
    SURGEON_OPERATIONS,
    comment_only,
    json_object_after,
    object_source_after,
    output_text,
    parse_instant,
    parse_patch_files,
    sha256_text,
    strip_js_literals,
    tool_status,
)


MUTATION_OPERATIONS = {"edit_clojure", "apply_clojure_changes"}
APPLY_CALL = re.compile(
    r"tools\.apply_patch\s*\(\s*(\"(?:\\.|[^\"\\])*\"|[A-Za-z_$][\w$]*)",
    re.DOTALL,
)
STRING_ASSIGNMENT = re.compile(
    r"\b(?:const|let|var)\s+([A-Za-z_$][\w$]*)\s*=\s*"
    r"(\"(?:\\.|[^\"\\])*\")",
    re.DOTALL,
)
PATCH_FILE = re.compile(r"^\*\*\* (Add|Update|Delete) File: (.+)$")
OWNER_FORM = re.compile(
    r"^\((ns|defn-?|defmacro|defmulti|defmethod|defprotocol|defrecord|deftype|"
    r"defonce|def|deftest)\s+(?:\^\S+\s+)*([^\s\[\](){}]+)"
)
WORKSPACE_ROOT = re.compile(
    r'(?:(?:"workspace_root")|workspace_root)\s*:\s*("(?:\\.|[^"\\])*")'
)


def sha256_file_prefix(path, until):
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for raw_line in handle:
            try:
                event = json.loads(raw_line)
                timestamp = event.get("timestamp")
                if timestamp and parse_instant(timestamp) >= until:
                    continue
            except (json.JSONDecodeError, TypeError, ValueError):
                pass
            digest.update(raw_line)
    return digest.hexdigest()


def decode_js_string(literal):
    try:
        return json.loads(literal)
    except json.JSONDecodeError:
        continued = re.sub(r"\\\\\r?\n", "", literal)
        try:
            return json.loads(continued)
        except json.JSONDecodeError:
            return None


def patches_from_source(source):
    """Return one decoded patch or None for every apply_patch invocation."""
    assignments = []
    for match in STRING_ASSIGNMENT.finditer(source):
        assignments.append((match.start(), match.group(1), decode_js_string(match.group(2))))

    patches = []
    for call in APPLY_CALL.finditer(source):
        argument = call.group(1)
        if argument.startswith('"'):
            patches.append(decode_js_string(argument))
            continue
        value = None
        for position, name, candidate in assignments:
            if position >= call.start():
                break
            if name == argument:
                value = candidate
        patches.append(value)
    return patches


def normalized_target(root, target):
    path = Path(target)
    if not path.is_absolute():
        path = root / path
    return Path(os.path.realpath(os.path.normpath(str(path))))


def confined_target(root, target):
    normalized_root = Path(os.path.realpath(root))
    normalized = normalized_target(normalized_root, target)
    try:
        relative = normalized.relative_to(normalized_root)
    except ValueError:
        return None
    return relative.as_posix()


def parse_update_hunks(patch, root):
    """Parse existing-file update hunks and classify their changed-byte mass."""
    hunks = []
    current_target = None
    current_operation = None
    current = None

    def finish():
        nonlocal current
        if current is None:
            return
        added = current.pop("_added")
        removed = current.pop("_removed")
        context = current.pop("_context")
        all_lines = current.pop("_all_lines")
        current["added_lines"] = len(added)
        current["removed_lines"] = len(removed)
        current["added_bytes"] = sum(len(line.encode("utf-8")) for line in added)
        current["removed_bytes"] = sum(len(line.encode("utf-8")) for line in removed)
        if added and removed:
            current["shape"] = "replacement"
        elif added:
            current["shape"] = "insertion"
        elif removed:
            current["shape"] = "deletion"
        else:
            current["shape"] = "empty"
        owner = infer_owner(all_lines or context)
        current["owner_hash"] = sha256_text(owner) if owner else None
        hunks.append(current)
        current = None

    for line in patch.splitlines():
        file_match = PATCH_FILE.match(line)
        if file_match:
            finish()
            current_operation = file_match.group(1).lower()
            current_target = confined_target(root, file_match.group(2))
            continue
        if line.startswith("*** "):
            finish()
            continue
        if current_operation != "update" or current_target is None:
            continue
        if line.startswith("@@"):
            finish()
            current = {
                "target": current_target,
                "_added": [],
                "_removed": [],
                "_context": [line[2:].strip()],
                "_all_lines": [line[2:].strip()],
            }
            continue
        if current is None:
            continue
        if line.startswith("+") and not line.startswith("+++"):
            current["_added"].append(line[1:])
            current["_all_lines"].append(line[1:])
        elif line.startswith("-") and not line.startswith("---"):
            current["_removed"].append(line[1:])
            current["_all_lines"].append(line[1:])
        else:
            current["_context"].append(line)
            current["_all_lines"].append(line)
    finish()
    return hunks


def infer_owner(context):
    """Infer a named top-level owner from visible hunk context, if present."""
    candidates = set()
    for line in context:
        stripped = line.strip()
        match = OWNER_FORM.match(stripped)
        if match:
            candidates.add(f"{match.group(1)}:{match.group(2)}")
    return next(iter(candidates)) if len(candidates) == 1 else None


def mission_key(session_token, turn_id):
    return sha256_text(f"{session_token}\0{turn_id}")


def event_mission_state(event, session_token, current, legacy_counter):
    payload = event.get("payload") or {}
    if event.get("type") == "event_msg" and payload.get("type") == "task_started":
        turn_id = payload.get("turn_id") or f"task-{event.get('ordinal', legacy_counter)}"
        return mission_key(session_token, turn_id), legacy_counter
    if event.get("type") == "turn_context" and not current:
        turn_id = payload.get("turn_id")
        if turn_id:
            return mission_key(session_token, turn_id), legacy_counter
    if (event.get("type") == "event_msg"
            and payload.get("type") == "user_message" and not current):
        legacy_counter += 1
        return mission_key(session_token, f"legacy-{legacy_counter}"), legacy_counter
    if (event.get("type") == "event_msg"
            and payload.get("type") in {"task_complete", "turn_aborted"}):
        return None, legacy_counter
    return current, legacy_counter


def structural_target_status(arguments, object_source, root):
    if not isinstance(arguments, dict):
        workspace = None
    else:
        workspace = arguments.get("workspace_root")
    if not isinstance(workspace, str) and object_source:
        match = WORKSPACE_ROOT.search(object_source)
        if match:
            workspace = decode_js_string(match.group(1))
    if not isinstance(workspace, str):
        return "missing"
    return ("match" if Path(os.path.realpath(workspace)) == Path(os.path.realpath(root))
            else "mismatch")


def session_cwd(path):
    with path.open() as handle:
        for line in handle:
            event = json.loads(line)
            if event.get("type") == "session_meta":
                return (event.get("payload") or {}).get("cwd")
    return None


def discover_evidence(sessions_root, roots):
    """Use only the documented year/month/day rollout layout."""
    selected = defaultdict(list)
    pattern = str(sessions_root / "2026" / "08" / "*" / "rollout-*.jsonl")
    canonical = {label: os.path.realpath(root) for label, root in roots.items()}
    for filename in sorted(glob.glob(pattern)):
        path = Path(filename)
        cwd = session_cwd(path)
        if not cwd:
            continue
        resolved = os.path.realpath(cwd)
        for label, root in canonical.items():
            if resolved == root:
                selected[label].append(path)
                break
    return selected


def collect_calls(paths, root, since, until):
    calls = []
    outputs = {}
    evidence_hashes = []
    active_files = 0

    for path in paths:
        session_token = sha256_text(path.name)
        evidence_hashes.append(sha256_file_prefix(path, until))
        current_mission = None
        legacy_counter = 0
        file_active = False
        with path.open() as handle:
            for line in handle:
                event = json.loads(line)
                current_mission, legacy_counter = event_mission_state(
                    event, session_token, current_mission, legacy_counter)
                timestamp_text = event.get("timestamp")
                if not timestamp_text:
                    continue
                timestamp = parse_instant(timestamp_text)
                if timestamp < since or timestamp >= until:
                    continue
                file_active = True
                if event.get("type") != "response_item":
                    continue
                payload = event.get("payload") or {}
                payload_type = payload.get("type")
                if payload_type in {"custom_tool_call_output", "function_call_output"}:
                    outputs[payload.get("call_id")] = output_text(payload)
                    if payload_type == "function_call_output" and not outputs[payload.get("call_id")]:
                        value = payload.get("output")
                        outputs[payload.get("call_id")] = value if isinstance(value, str) else ""
                    continue
                if payload_type == "custom_tool_call":
                    source = payload.get("input") or ""
                    executable = strip_js_literals(source)
                    for invocation_index, patch in enumerate(patches_from_source(source)):
                        calls.append({
                            "kind": "native",
                            "call_id": payload.get("call_id"),
                            "invocation_index": invocation_index,
                            "mission": current_mission,
                            "patch": patch,
                        })
                    for operation, pattern in SURGEON_CALL.items():
                        for invocation_index, match in enumerate(pattern.finditer(executable)):
                            arguments = json_object_after(source, match.end())
                            object_source = object_source_after(source, match.end())
                            calls.append({
                                "kind": "surgeon",
                                "call_id": payload.get("call_id"),
                                "invocation_index": invocation_index,
                                "mission": current_mission,
                                "operation": operation,
                                "arguments": arguments,
                                "object_source": object_source,
                            })
                elif payload_type == "function_call":
                    name = payload.get("name")
                    if name == "apply_patch":
                        value = payload.get("arguments")
                        try:
                            arguments = json.loads(value) if isinstance(value, str) else value
                        except json.JSONDecodeError:
                            arguments = None
                        patch = (arguments.get("patch") if isinstance(arguments, dict) else None)
                        calls.append({
                            "kind": "native",
                            "call_id": payload.get("call_id"),
                            "invocation_index": 0,
                            "mission": current_mission,
                            "patch": patch,
                        })
        active_files += bool(file_active)

    for call in calls:
        output = outputs.get(call["call_id"], "")
        if call["kind"] == "native":
            call["status"] = (
                "failed" if "Script failed" in output or "Script error:" in output
                else "success")
        else:
            call["status"], call["refusal_type"] = tool_status(
                call["operation"], output)
            call["target_status"] = structural_target_status(
                call["arguments"], call.get("object_source"), root)
    return calls, {
        "matched_evidence_files": len(paths),
        "active_evidence_files": active_files,
        "evidence_manifest_sha256": sha256_text(
            "".join(f"{digest}\n" for digest in sorted(evidence_hashes))),
    }


def native_rows(calls, root):
    rows = []
    created_by_mission = defaultdict(set)
    for call in calls:
        if call["kind"] != "native":
            continue
        patch = call["patch"]
        files = parse_patch_files(patch, root) if patch else []
        confined_files = []
        for entry in files:
            relative = confined_target(root, entry["target"])
            if relative is not None:
                confined_files.append({**entry, "relative": relative})
        updates = [entry for entry in confined_files if entry["operation"] == "update"]
        clojure_updates = [entry for entry in updates
                           if Path(entry["relative"]).suffix in CLOJURE_EXTENSIONS]
        mission_created = created_by_mission[call["mission"]] if call["mission"] else set()
        created_flags = [entry["relative"] in mission_created for entry in clojure_updates]
        changed = [line for entry in clojure_updates for line in entry["changed"]]
        hunk_count = sum(entry["hunks"] for entry in clojure_updates)
        rows.append({
            "status": call["status"],
            "mission": call["mission"],
            "patch_parsed": patch is not None,
            "in_repo_write": bool(confined_files),
            "out_of_root_file_count": len(files) - len(confined_files),
            "file_count": len(confined_files),
            "existing_clojure": bool(clojure_updates),
            "clojure_targets": [entry["relative"] for entry in clojure_updates],
            "comment_only": comment_only(changed) if clojure_updates else False,
            "small_single_hunk": bool(clojure_updates and hunk_count == 1
                                      and len(changed) <= 4),
            "all_same_mission_created": bool(created_flags and all(created_flags)),
            "all_src_or_test": bool(clojure_updates and all(
                Path(entry["relative"]).parts
                and Path(entry["relative"]).parts[0] in {"src", "test"}
                for entry in clojure_updates)),
            "hunks": parse_update_hunks(patch, root) if patch else [],
        })
        if call["status"] == "success" and call["mission"]:
            for entry in confined_files:
                target = entry["relative"]
                if entry["operation"] == "add":
                    mission_created.add(target)
                elif entry["operation"] == "delete":
                    mission_created.discard(target)
    return rows


def addressable_ladder(rows):
    successful = [row for row in rows
                  if row["status"] == "success" and row["in_repo_write"]]
    existing = [row for row in successful if row["existing_clojure"]]
    substantive = [row for row in existing
                   if not row["comment_only"] and not row["small_single_hunk"]]
    established = [row for row in substantive if not row["all_same_mission_created"]]
    return {
        "all_successful_native_writes": len(successful),
        "updates_existing_clojure": len(existing),
        "comment_only": sum(row["comment_only"] for row in existing),
        "small_single_hunk_up_to_4_changed_lines": sum(
            row["small_single_hunk"] for row in existing),
        "trivial_union": sum(row["comment_only"] or row["small_single_hunk"]
                             for row in existing),
        "substantive_existing_clojure": len(substantive),
        "same_mission_created_all_targets": sum(
            row["all_same_mission_created"] for row in substantive),
        "established_files_all_target_rule": len(established),
        "established_all_targets_in_src_or_test": sum(
            row["all_src_or_test"] for row in established),
    }


def subject_repetition(rows):
    occurrences = Counter()
    for row in rows:
        if row["status"] != "success" or not row["mission"]:
            continue
        for target in row["clojure_targets"]:
            occurrences[(row["mission"], target)] += 1
    distinct = len(occurrences)
    repeated = sum(count >= 2 for count in occurrences.values())
    total = sum(occurrences.values())
    return {
        "identity": "canonical repository-relative file within one task turn",
        "distinct_subjects": distinct,
        "subject_occurrences": total,
        "repeated_subjects": repeated,
        "repeated_subject_percent": (100.0 * repeated / distinct if distinct else None),
        "extra_occurrences_after_first": total - distinct,
        "unassigned_successful_existing_clojure_actions": sum(
            row["status"] == "success" and row["existing_clojure"] and not row["mission"]
            for row in rows),
    }


def owner_repetition(rows):
    """Count only hunk owners mechanically visible in retained patch context."""
    occurrences = Counter()
    all_hunks = []
    for row in rows:
        if row["status"] != "success" or not row["mission"]:
            continue
        eligible = [hunk for hunk in row["hunks"]
                    if hunk["shape"] != "empty"
                    and Path(hunk["target"]).suffix in CLOJURE_EXTENSIONS]
        all_hunks.extend(eligible)
        action_subjects = {
            (hunk["target"], hunk["owner_hash"])
            for hunk in eligible if hunk["owner_hash"]
        }
        for subject in action_subjects:
            occurrences[(row["mission"], *subject)] += 1
    distinct = len(occurrences)
    repeated = sum(count >= 2 for count in occurrences.values())
    return {
        "identity": "repository-relative file plus named top-level owner visible in hunk context",
        "eligible_changed_hunks": len(all_hunks),
        "owner_visible_hunks": sum(bool(hunk["owner_hash"]) for hunk in all_hunks),
        "owner_visible_hunk_percent": (
            100.0 * sum(bool(hunk["owner_hash"]) for hunk in all_hunks) / len(all_hunks)
            if all_hunks else None),
        "distinct_subjects": distinct,
        "subject_occurrences": sum(occurrences.values()),
        "repeated_subjects": repeated,
        "repeated_subject_percent": (100.0 * repeated / distinct if distinct else None),
        "extra_occurrences_after_first": sum(occurrences.values()) - distinct,
    }


def edit_shapes(rows):
    all_hunks = [hunk for row in rows
             if row["status"] == "success" and row["existing_clojure"]
             for hunk in row["hunks"]
             if Path(hunk["target"]).suffix in CLOJURE_EXTENSIONS]
    hunks = [hunk for hunk in all_hunks if hunk["shape"] != "empty"]
    counts = Counter(hunk["shape"] for hunk in hunks)
    byte_counts = Counter()
    for hunk in hunks:
        byte_counts[hunk["shape"]] += hunk["added_bytes"] + hunk["removed_bytes"]
    total_hunks = sum(counts.values())
    total_bytes = sum(byte_counts.values())
    return {
        "hunk_counts": dict(sorted(counts.items())),
        "empty_navigation_anchors_excluded": sum(
            hunk["shape"] == "empty" for hunk in all_hunks),
        "hunk_count_total": total_hunks,
        "changed_bytes": dict(sorted(byte_counts.items())),
        "changed_bytes_total": total_bytes,
        "insertion_hunk_percent": (
            100.0 * counts["insertion"] / total_hunks if total_hunks else None),
        "insertion_changed_byte_percent": (
            100.0 * byte_counts["insertion"] / total_bytes if total_bytes else None),
    }


def repository_report(calls, evidence, root, root_hash):
    rows = native_rows(calls, root)
    surgeon = [call for call in calls if call["kind"] == "surgeon"]
    mutations = [call for call in surgeon if call["operation"] in MUTATION_OPERATIONS]
    successful_mutations = [call for call in mutations
                            if call["status"] == "success"
                            and call["target_status"] == "match"]
    successful_native = sum(
        row["status"] == "success" and row["in_repo_write"] for row in rows)
    denominator = successful_native + len(successful_mutations)
    ladder = addressable_ladder(rows)
    repetition = subject_repetition(rows)
    owner_repetition_report = owner_repetition(rows)
    shapes = edit_shapes(rows)
    return {
        **evidence,
        "repository_root_sha256": root_hash,
        "mission_count": len({call["mission"] for call in calls if call["mission"]}),
        "native": {
            "session_attempted_writes": len(rows),
            "successful_repo_writes": successful_native,
            "failed_repo_writes": sum(
                row["status"] != "success" and row["in_repo_write"] for row in rows),
            "successful_out_of_repo_writes": sum(
                row["status"] == "success" and not row["in_repo_write"]
                and row["patch_parsed"] for row in rows),
            "unparsed_writes": sum(not row["patch_parsed"] for row in rows),
            "out_of_root_file_references": sum(
                row["out_of_root_file_count"] for row in rows),
        },
        "subject_repetition": repetition,
        "owner_repetition": owner_repetition_report,
        "edit_shapes": shapes,
        "addressable_ladder": ladder,
        "structural_mutations": {
            "attempted_calls": len(mutations),
            "successful_repo_calls": len(successful_mutations),
            "by_operation_and_status": {
                operation: dict(sorted(Counter(
                    call["status"] for call in mutations
                    if call["operation"] == operation).items()))
                for operation in sorted(MUTATION_OPERATIONS)
            },
            "target_statuses": dict(sorted(Counter(
                call["target_status"] for call in mutations).items())),
        },
        "reach": {
            "successful_structural_share_percent": (
                100.0 * len(successful_mutations) / denominator if denominator else None),
            "denominator_successful_native_plus_structural": denominator,
            "final_src_test_ladder_share_of_native_percent": (
                100.0 * ladder["established_all_targets_in_src_or_test"] / successful_native
                if successful_native else None),
        },
        "thresholds": {
            "subject_repetition_reprices": bool(
                owner_repetition_report["repeated_subject_percent"] is not None
                and owner_repetition_report["repeated_subject_percent"] >= 30.0
                and owner_repetition_report["owner_visible_hunk_percent"] is not None
                and owner_repetition_report["owner_visible_hunk_percent"] >= 80.0),
            "file_repetition_upper_bound_crosses": bool(
                repetition["repeated_subject_percent"] is not None
                and repetition["repeated_subject_percent"] >= 30.0),
            "insertion_bytes_reprices": bool(
                shapes["insertion_changed_byte_percent"] is not None
                and shapes["insertion_changed_byte_percent"] >= 50.0),
            "reach_prediction_under_5_percent": bool(
                denominator and 100.0 * len(successful_mutations) / denominator < 5.0),
        },
    }


def parse_repo(value):
    label, separator, root = value.partition("=")
    if not separator or not re.fullmatch(r"external-[a-z]", label):
        raise argparse.ArgumentTypeError("repo must be external-a=/absolute/path")
    path = Path(root)
    if not path.is_absolute():
        raise argparse.ArgumentTypeError("repo path must be absolute")
    return label, path


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--sessions-root", type=Path, required=True)
    parser.add_argument("--since", required=True)
    parser.add_argument("--until", required=True)
    parser.add_argument("--repo", type=parse_repo, action="append", required=True)
    parser.add_argument("--output", type=Path)
    parser.add_argument("--collector-receipt", type=Path)
    args = parser.parse_args()

    since = parse_instant(args.since)
    until = parse_instant(args.until)
    roots = dict(args.repo)
    evidence = discover_evidence(args.sessions_root, roots)
    repositories = {}
    for label, root in roots.items():
        calls, evidence_report = collect_calls(evidence.get(label, []), root, since, until)
        repositories[label] = repository_report(
            calls, evidence_report, root, sha256_text(str(root)))

    report = {
        "schema": "clj-surgeon.external-corpus-shape-census.v1",
        "window": {"since": args.since, "until": args.until},
        "source_collector_receipt_sha256": (
            hashlib.sha256(args.collector_receipt.read_bytes()).hexdigest()
            if args.collector_receipt else None),
        "providers": {
            "codex": {"status": "measured", "repositories": repositories},
            "claude": {
                "status": "unmeasured",
                "reason": (
                    "the bounded collector exposes no privacy-safe repository cwd projection "
                    "for Claude sessions; no Codex/Claude denominator was combined"),
            },
        },
        "privacy": {
            "paths_emitted": False,
            "session_identifiers_emitted": False,
            "commands_emitted": False,
            "source_emitted": False,
            "patches_emitted": False,
            "tool_arguments_emitted": False,
        },
    }
    rendered = json.dumps(report, indent=2, sort_keys=True) + "\n"
    if args.output:
        args.output.write_text(rendered)
    print(rendered, end="")


if __name__ == "__main__":
    main()
