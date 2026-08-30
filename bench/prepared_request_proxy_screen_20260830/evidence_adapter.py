"""Strict adapter from raw Codex/proxy rows to ``evidence_oracle`` input.

The adapter is pure. It does not read files, run models, freeze state, contact a
runtime, or trust caller-supplied route and correctness summaries.
"""

from __future__ import annotations

import re
from collections import defaultdict
from collections.abc import Mapping, Sequence
from pathlib import PurePosixPath
from typing import Any

import evidence_oracle as oracle


_TOP_LEVEL_KEYS = {
    "attempt_id",
    "phase",
    "arm",
    "workspace_root",
    "process_timed_out",
    "codex_events",
    "proxy_rows",
    "before_tree_sha256",
    "after_tree_sha256",
    "changed_files",
}

_ACTIONABLE_ITEM_TYPES = {
    "mcp_tool_call",
    "command_execution",
    "file_change",
}

_COMPLETED_ONLY_PASSIVE_ITEM_TYPES = {
    "reasoning",
    "agent_message",
}

_WRAPPER_EVENT_TYPES = {
    "thread.started",
    "turn.started",
    "turn.completed",
}

_SURGEON_MUTATION_TOOLS = {
    "edit_clojure": "edit-clojure",
    "apply_clojure_changes": "apply-clojure-changes",
    "transform_clojure": "transform-clojure",
}

_FILE_CHANGE_KINDS = {
    "update": "modified",
    "add": "created",
    "delete": "deleted",
}

_WRITE_COMMAND = re.compile(
    r"(?:apply_patch|sed\s+-i|perl\s+-p?i|\btee\b|\btruncate\b|"
    r"\bmv\b|\bcp\b|\brm\b|\bpatch\b|\btouch\b|\bmkdir\b|"
    r"\brsync\b|\binstall\b|\bgit\s+(?:add|apply|checkout|commit|mv|reset|restore|rm)\b|"
    r"\b(?:python|python3|ruby)\b.*(?:write_text|write_bytes|File\.write|open\()|"
    r"(?:^|\s)>(?:>|\s))"
)


class AdapterError(ValueError):
    """A stable fail-closed adapter refusal."""

    def __init__(self, code: str, **details: Any) -> None:
        self.code = code
        self.details = dict(sorted(details.items()))
        super().__init__(code)


def _refuse(code: str, **details: Any) -> None:
    raise AdapterError(code, **details)


def _mapping(value: Any, path: str) -> Mapping[str, Any]:
    if not isinstance(value, Mapping):
        _refuse("mapping-required", path=path)
    if not all(isinstance(key, str) for key in value):
        _refuse("string-keys-required", path=path)
    return value


def _sequence(value: Any, path: str) -> Sequence[Any]:
    if not isinstance(value, Sequence) or isinstance(value, (str, bytes)):
        _refuse("sequence-required", path=path)
    return value


def _nonblank(value: Any, path: str) -> str:
    if not isinstance(value, str) or not value.strip():
        _refuse("nonblank-string-required", path=path)
    return value


def _request_id_token(value: Any, path: str) -> str:
    if isinstance(value, bool) or value is None:
        _refuse("request-id-required", path=path)
    if isinstance(value, int):
        return f"int:{value}"
    if isinstance(value, str) and value:
        return f"str:{value}"
    _refuse("request-id-required", path=path)


def _canonical_workspace_root(value: Any) -> PurePosixPath:
    raw = _nonblank(value, "workspace_root")
    root = PurePosixPath(raw)
    if (
        not root.is_absolute()
        or root == PurePosixPath("/")
        or str(root) != raw
        or ".." in root.parts
    ):
        _refuse("noncanonical-workspace-root", workspace_root=raw)
    return root


def _workspace_relative_file_change_path(
    value: Any,
    workspace_root: PurePosixPath,
    path: str,
) -> str:
    raw = _nonblank(value, path)
    absolute = PurePosixPath(raw)
    if not absolute.is_absolute() or str(absolute) != raw or ".." in absolute.parts:
        _refuse("noncanonical-file-change-path", path=path, value=raw)
    try:
        relative = absolute.relative_to(workspace_root)
    except ValueError:
        _refuse("file-change-outside-workspace", path=path, value=raw)
    if relative == PurePosixPath("."):
        _refuse("file-change-path-is-workspace-root", path=path)
    return str(relative)


def _client_refusal_reason(item: Mapping[str, Any]) -> str | None:
    status = item.get("status")
    if status not in {"failed", "refused"}:
        return None
    if item.get("result") is not None:
        _refuse("client-refusal-carries-result", status=status)
    reason = item.get("error", item.get("refusal_reason"))
    if isinstance(reason, str) and reason.strip():
        return reason
    if isinstance(reason, Mapping) and reason:
        for key in ("message", "reason", "error", "code"):
            value = reason.get(key)
            if isinstance(value, (str, int, float)) and not isinstance(value, bool):
                if str(value).strip():
                    return oracle.canonical_json_bytes(reason).decode("utf-8")
    _refuse("concrete-client-refusal-required", status=status)


def _exact_item_arguments(item: Mapping[str, Any], path: str) -> Mapping[str, Any]:
    if "arguments" not in item:
        _refuse("missing-codex-arguments", path=path)
    return _mapping(item["arguments"], f"{path}.arguments")


def _item_semantics(
    item: Mapping[str, Any],
    path: str,
    workspace_root: PurePosixPath,
) -> dict[str, Any]:
    item_id = _nonblank(item.get("id"), f"{path}.id")
    item_type = item.get("type")
    if item_type not in _ACTIONABLE_ITEM_TYPES:
        _refuse("unrecognized-codex-item-type", path=path, item_type=item_type)

    result: dict[str, Any] = {"id": item_id, "type": item_type}
    if item_type == "mcp_tool_call":
        result.update({
            "server": _nonblank(item.get("server"), f"{path}.server"),
            "tool": _nonblank(item.get("tool"), f"{path}.tool"),
            "arguments": _exact_item_arguments(item, path),
        })
    elif item_type == "command_execution":
        result["command"] = _nonblank(item.get("command"), f"{path}.command")
    elif item_type == "file_change":
        changes = _sequence(item.get("changes"), f"{path}.changes")
        if not changes:
            _refuse("empty-codex-file-change", path=path)
        normalized_changes = []
        for index, raw_change in enumerate(changes):
            change_path = f"{path}.changes[{index}]"
            change = _mapping(raw_change, change_path)
            if set(change) != {"path", "kind"}:
                _refuse(
                    "invalid-codex-file-change-keys",
                    path=change_path,
                    missing=sorted({"path", "kind"} - set(change)),
                    unknown=sorted(set(change) - {"path", "kind"}),
                )
            kind = change.get("kind")
            if kind not in _FILE_CHANGE_KINDS:
                _refuse(
                    "unrecognized-file-change-kind",
                    path=change_path,
                    kind=kind,
                )
            normalized_changes.append({
                "path": _workspace_relative_file_change_path(
                    change.get("path"),
                    workspace_root,
                    f"{change_path}.path",
                ),
                "change_type": _FILE_CHANGE_KINDS[kind],
            })
        result["changes"] = normalized_changes
    return result


def _join_codex_lifecycles(
    events: Sequence[Any],
    workspace_root: PurePosixPath,
    process_timed_out: bool,
) -> list[dict[str, Any]]:
    starts: dict[str, tuple[int, Mapping[str, Any], dict[str, Any]]] = {}
    completions: dict[str, tuple[int, Mapping[str, Any], dict[str, Any]]] = {}

    for index, raw_event in enumerate(events):
        path = f"codex_events[{index}]"
        event = _mapping(raw_event, path)
        event_type = event.get("type")
        if event_type in _WRAPPER_EVENT_TYPES:
            if "item" in event:
                _refuse("wrapper-event-must-not-contain-item", path=path)
            continue
        if event_type not in {"item.started", "item.completed"}:
            _refuse("unrecognized-codex-event-type", path=path, event_type=event_type)
        item = _mapping(event.get("item"), f"{path}.item")
        item_type = item.get("type")
        if item_type in _COMPLETED_ONLY_PASSIVE_ITEM_TYPES:
            if event_type != "item.completed":
                _refuse(
                    "passive-item-must-be-completed-only",
                    path=path,
                    item_type=item_type,
                )
            if "id" in item:
                _nonblank(item["id"], f"{path}.item.id")
            continue
        semantics = _item_semantics(item, f"{path}.item", workspace_root)
        item_id = semantics["id"]

        if event_type == "item.started":
            if item_id in starts:
                _refuse("duplicate-codex-start", item_id=item_id)
            if item_id in completions:
                _refuse("reversed-codex-lifecycle", item_id=item_id)
            starts[item_id] = (index, item, semantics)
        else:
            if item_id in completions:
                _refuse("duplicate-codex-completion", item_id=item_id)
            if item_id not in starts:
                if any(
                    isinstance(later, Mapping)
                    and later.get("type") == "item.started"
                    and isinstance(later.get("item"), Mapping)
                    and later["item"].get("id") == item_id
                    for later in events[index + 1:]
                ):
                    _refuse("reversed-codex-lifecycle", item_id=item_id)
                _refuse("orphan-codex-completion", item_id=item_id)
            if semantics != starts[item_id][2]:
                _refuse("mismatched-codex-lifecycle", item_id=item_id)
            completions[item_id] = (index, item, semantics)

    missing = sorted(set(starts) - set(completions))
    if missing:
        if not process_timed_out:
            _refuse("orphan-codex-start", item_ids=missing)
        if len(missing) != 1:
            _refuse("multiple-open-codex-on-timeout", item_ids=missing)
        open_item_id = missing[0]
        open_start_index = starts[open_item_id][0]
        latest_action_index = max(
            [entry[0] for entry in starts.values()]
            + [entry[0] for entry in completions.values()]
        )
        if open_start_index != latest_action_index:
            _refuse(
                "nonlatest-open-codex-on-timeout",
                item_id=open_item_id,
            )

    joined = []
    for item_id, (start_index, start_item, semantics) in sorted(
        starts.items(), key=lambda entry: entry[1][0]
    ):
        timed_out = item_id not in completions
        if timed_out:
            completion_index = len(events)
            completion_item = dict(start_item)
            completion_item["status"] = "timeout"
        else:
            completion_index, completion_item, _ = completions[item_id]
        if completion_index <= start_index:
            _refuse("reversed-codex-lifecycle", item_id=item_id)
        joined.append({
            "item_id": item_id,
            "start_index": start_index,
            "completion_index": completion_index,
            "semantics": semantics,
            "start_item": start_item,
            "completion_item": completion_item,
            "timed_out": timed_out,
        })
    return joined


def _proxy_outcome(row: Mapping[str, Any], path: str) -> tuple[str, str | None]:
    emitted = _mapping(row.get("emitted_result"), f"{path}.emitted_result")
    structured = _mapping(
        emitted.get("structuredContent"), f"{path}.emitted_result.structuredContent"
    )
    ok = structured.get("ok")
    if not isinstance(ok, bool):
        _refuse("proxy-result-ok-required", path=path)
    if ok and not bool(row.get("is_error", False)):
        return "success", None
    reason = (
        structured.get("reason")
        or structured.get("error_type")
        or structured.get("error-type")
        or structured.get("error")
    )
    if not isinstance(reason, (str, int, float)) or not str(reason):
        _refuse("proxy-refusal-reason-required", path=path)
    return "refusal", str(reason)


def _join_proxy_lifecycles(
    rows: Sequence[Any],
    arm: str,
    expected_identities: Mapping[str, Any],
    process_timed_out: bool,
) -> tuple[list[dict[str, Any]], Mapping[str, Any]]:
    ready_rows: list[tuple[int, Mapping[str, Any]]] = []
    calls: dict[tuple[type, Any], tuple[int, Mapping[str, Any]]] = {}
    results: dict[tuple[type, Any], tuple[int, Mapping[str, Any]]] = {}

    for index, raw_row in enumerate(rows):
        path = f"proxy_rows[{index}]"
        row = _mapping(raw_row, path)
        event = row.get("event")
        if event == "proxy_ready":
            ready_rows.append((index, row))
            continue
        if event == "client_tools_list":
            continue
        if event == "client_notification":
            if row.get("method") != "notifications/initialized":
                _refuse(
                    "unrecognized-proxy-notification",
                    path=path,
                    method=row.get("method"),
                )
            unknown = set(row) - {"ts_ns", "event", "method"}
            if unknown:
                _refuse(
                    "invalid-proxy-notification-shape",
                    path=path,
                    unknown=sorted(unknown),
                )
            if "ts_ns" in row and (
                isinstance(row["ts_ns"], bool)
                or not isinstance(row["ts_ns"], int)
                or row["ts_ns"] < 0
            ):
                _refuse("invalid-proxy-notification-timestamp", path=path)
            continue
        if event not in {"client_tool_call", "client_tool_result"}:
            _refuse("unrecognized-proxy-row", path=path, event=event)

        raw_request_id = row.get("request_id")
        token = _request_id_token(raw_request_id, f"{path}.request_id")
        request_key = (type(raw_request_id), raw_request_id)
        _nonblank(row.get("name"), f"{path}.name")
        _nonblank(row.get("arguments_sha256"), f"{path}.arguments_sha256")

        if event == "client_tool_call":
            if request_key in calls:
                _refuse("duplicate-proxy-call", request_id=token)
            if request_key in results:
                _refuse("reversed-proxy-lifecycle", request_id=token)
            arguments = _mapping(row.get("arguments"), f"{path}.arguments")
            actual_hash = oracle.canonical_argument_hash(arguments)
            if actual_hash != row["arguments_sha256"]:
                _refuse("proxy-call-argument-hash-mismatch", request_id=token)
            calls[request_key] = (index, row)
        else:
            if request_key in results:
                _refuse("duplicate-proxy-result", request_id=token)
            if request_key not in calls:
                if any(
                    isinstance(later, Mapping)
                    and later.get("event") == "client_tool_call"
                    and type(later.get("request_id")) is type(raw_request_id)
                    and later.get("request_id") == raw_request_id
                    for later in rows[index + 1:]
                ):
                    _refuse("reversed-proxy-lifecycle", request_id=token)
                _refuse("orphan-proxy-result", request_id=token)
            results[request_key] = (index, row)

    if len(ready_rows) != 1:
        _refuse("exactly-one-proxy-ready-required", count=len(ready_rows))
    ready = ready_rows[0][1]
    wanted_arm = "C" if arm == "control" else "T"
    if ready.get("arm") != wanted_arm:
        _refuse("proxy-ready-arm-mismatch", actual=ready.get("arm"), expected=wanted_arm)
    if ready.get("offered_tool_list_sha256") != expected_identities["catalog_sha256"]:
        _refuse("proxy-ready-catalog-mismatch")
    if ready.get("server_instructions_sha256") != expected_identities["static_surface_sha256"]:
        _refuse("proxy-ready-static-surface-mismatch")

    missing = [key for key in calls if key not in results]
    if missing:
        missing_tokens = sorted(
            _request_id_token(key[1], "request_id") for key in missing
        )
        if not process_timed_out:
            _refuse("orphan-proxy-call", request_ids=missing_tokens)
        if len(missing) != 1:
            _refuse("multiple-open-proxy-on-timeout", request_ids=missing_tokens)
        open_key = missing[0]
        open_call_index = calls[open_key][0]
        latest_action_index = max(
            [entry[0] for entry in calls.values()]
            + [entry[0] for entry in results.values()]
        )
        if open_call_index != latest_action_index:
            _refuse(
                "nonlatest-open-proxy-on-timeout",
                request_id=_request_id_token(open_key[1], "request_id"),
            )

    joined = []
    for request_key, (call_index, call) in sorted(calls.items(), key=lambda entry: entry[1][0]):
        request_token = _request_id_token(request_key[1], "request_id")
        timed_out = request_key not in results
        if timed_out:
            result_index = len(rows)
            result = None
        else:
            result_index, result = results[request_key]
        if result_index <= call_index:
            _refuse("reversed-proxy-lifecycle", request_id=request_token)
        if timed_out:
            outcome, refusal_reason = "timeout", None
            emitted = False
            candidate_hash = None
        else:
            assert result is not None
            for field in ("name", "arguments_sha256"):
                if result.get(field) != call.get(field):
                    _refuse(
                        "mismatched-proxy-lifecycle",
                        request_id=request_token,
                        field=field,
                    )
            outcome, refusal_reason = _proxy_outcome(
                result, f"proxy_rows[{result_index}]"
            )
            emitted = result.get("prepared_emitted")
            if not isinstance(emitted, bool):
                _refuse("prepared-emitted-boolean-required", request_id=request_token)
            candidate_hash = result.get("prepared_request_sha256")
        if emitted:
            if arm != "treatment":
                _refuse("control-exposed-candidate", request_id=request_token)
            if outcome != "success":
                _refuse("candidate-exposed-on-refusal", request_id=request_token)
            emitted_result = _mapping(
                result.get("emitted_result"),
                f"proxy_rows[{result_index}].emitted_result",
            )
            structured = _mapping(
                emitted_result.get("structuredContent"),
                f"proxy_rows[{result_index}].emitted_result.structuredContent",
            )
            descriptor = _mapping(
                structured.get("prepared_request"),
                f"proxy_rows[{result_index}].emitted_result.structuredContent.prepared_request",
            )
            actual_candidate_hash = oracle.canonical_argument_hash(descriptor)
            if candidate_hash != actual_candidate_hash:
                _refuse(
                    "proxy-candidate-hash-mismatch",
                    request_id=request_token,
                )
        elif candidate_hash is not None:
            _refuse("candidate-identity-without-exposure", request_id=request_token)
        elif result is not None and isinstance(result.get("emitted_result"), Mapping):
            structured = result["emitted_result"].get("structuredContent")
            if isinstance(structured, Mapping) and "prepared_request" in structured:
                _refuse(
                    "prepared-request-present-without-exposure",
                    request_id=request_token,
                )

        joined.append({
            "request_id": request_token,
            "name": call["name"],
            "arguments": call["arguments"],
            "arguments_sha256": call["arguments_sha256"],
            "outcome": outcome,
            "refusal_reason": refusal_reason,
            "prepared_request_exposed": emitted,
            "candidate_descriptor_sha256": candidate_hash,
            "call_index": call_index,
            "result_index": result_index,
            "timed_out": timed_out,
        })
    return joined, ready


def _codex_proxy_bijection(
    codex_items: list[dict[str, Any]],
    proxy_calls: list[dict[str, Any]],
) -> dict[str, dict[str, Any]]:
    codex_groups: dict[tuple[str, str], list[dict[str, Any]]] = defaultdict(list)
    proxy_groups: dict[tuple[str, str], list[dict[str, Any]]] = defaultdict(list)

    for item in codex_items:
        semantics = item["semantics"]
        if semantics["type"] != "mcp_tool_call" or semantics["server"] != "clj-surgeon":
            continue
        argument_hash = oracle.canonical_argument_hash(semantics["arguments"])
        codex_groups[(semantics["tool"], argument_hash)].append(item)
    for call in proxy_calls:
        proxy_groups[(call["name"], call["arguments_sha256"])].append(call)

    if not set(proxy_groups).issubset(codex_groups):
        _refuse(
            "non-bijective-proxy-match",
            codex_keys=sorted(f"{name}:{digest}" for name, digest in codex_groups),
            proxy_keys=sorted(f"{name}:{digest}" for name, digest in proxy_groups),
        )

    matches: dict[str, dict[str, Any]] = {}
    for key in sorted(codex_groups):
        codex_group = codex_groups[key]
        proxy_group = proxy_groups.get(key, [])
        if not proxy_group:
            client_refusals = [
                _client_refusal_reason(item["completion_item"])
                for item in codex_group
            ]
            if all(reason is not None for reason in client_refusals):
                continue
            if any(reason is not None for reason in client_refusals):
                _refuse(
                    "ambiguous-client-refusal-proxy-match",
                    name=key[0],
                    argument_sha256=key[1],
                )
        if len(codex_group) != len(proxy_group):
            _refuse(
                "non-bijective-proxy-match",
                name=key[0],
                argument_sha256=key[1],
                codex_count=len(codex_group),
                proxy_count=len(proxy_group),
            )
        if len(codex_group) > 1:
            for left, right in zip(codex_group, codex_group[1:]):
                if right["start_index"] < left["completion_index"]:
                    _refuse(
                        "ambiguous-proxy-match",
                        name=key[0],
                        argument_sha256=key[1],
                    )
            for left, right in zip(proxy_group, proxy_group[1:]):
                if right["call_index"] < left["result_index"]:
                    _refuse(
                        "ambiguous-proxy-match",
                        name=key[0],
                        argument_sha256=key[1],
                    )
        for codex_item, proxy_call in zip(codex_group, proxy_group):
            if codex_item["timed_out"] != proxy_call["timed_out"]:
                _refuse(
                    "mismatched-timeout-lifecycle",
                    item_id=codex_item["item_id"],
                    request_id=proxy_call["request_id"],
                )
            matches[codex_item["item_id"]] = proxy_call
    return matches


def _native_outcome(item: Mapping[str, Any]) -> str:
    status = item.get("status")
    if status == "timeout":
        return "timeout"
    if status == "completed" and not item.get("error"):
        return "success"
    if status in {"failed", "completed"} or item.get("error"):
        return "failure"
    _refuse("native-completion-status-required", status=status)


def _oracle_tool_shape(
    joined_item: dict[str, Any],
    phase: str,
    proxy_match: dict[str, Any] | None,
) -> tuple[str, str, str | None, Mapping[str, Any], str, str | None]:
    semantics = joined_item["semantics"]
    item_type = semantics["type"]
    completion = joined_item["completion_item"]
    if item_type == "mcp_tool_call":
        server = semantics["server"]
        tool = semantics["tool"]
        arguments = semantics["arguments"]
        if server == "clj-surgeon":
            if tool == "inspect_clojure":
                activity, route = "read", None
            elif tool in _SURGEON_MUTATION_TOOLS:
                activity, route = "mutation", _SURGEON_MUTATION_TOOLS[tool]
            else:
                _refuse("unsupported-surgeon-tool", tool=tool)
            if proxy_match is None:
                refusal_reason = _client_refusal_reason(completion)
                if refusal_reason is None:
                    _refuse("missing-proxy-match", item_id=joined_item["item_id"])
                return tool, activity, route, arguments, "refusal", refusal_reason
            return (
                tool,
                activity,
                route,
                arguments,
                proxy_match["outcome"],
                proxy_match["refusal_reason"],
            )
        return (
            f"{server}/{tool}",
            "other",
            None,
            arguments,
            _native_outcome(completion),
            None,
        )
    if item_type == "command_execution":
        command = semantics["command"]
        mutation = phase == "safety" or bool(_WRITE_COMMAND.search(command))
        return (
            "exec_command",
            "mutation" if mutation else "other",
            "native-shell-write" if mutation else None,
            {"command": command},
            _native_outcome(completion),
            None,
        )
    if item_type == "file_change":
        return (
            "apply_patch",
            "mutation",
            "native-patch",
            {"changes": semantics["changes"]},
            _native_outcome(completion),
            None,
        )
    _refuse("passive-item-has-no-tool-shape", item_type=item_type)


def build_oracle_input(
    raw_attempt: Mapping[str, Any],
    expected_identities: Mapping[str, Any],
) -> dict[str, Any]:
    """Build the exact strict ``evidence_oracle`` input without compiling it."""

    raw = _mapping(raw_attempt, "raw_attempt")
    if set(raw) != _TOP_LEVEL_KEYS:
        _refuse(
            "invalid-adapter-input-keys",
            missing=sorted(_TOP_LEVEL_KEYS - set(raw)),
            unknown=sorted(set(raw) - _TOP_LEVEL_KEYS),
        )
    expected = _mapping(expected_identities, "expected_identities")
    if set(expected) != {
        "catalog_sha256", "static_surface_sha256", "candidate_policy_sha256"
    }:
        _refuse("invalid-expected-identity-keys")

    attempt_id = _nonblank(raw["attempt_id"], "attempt_id")
    phase = raw["phase"]
    if phase not in {"efficacy", "safety"}:
        _refuse("invalid-phase", phase=phase)
    arm_value = raw["arm"]
    arm = {"C": "control", "T": "treatment", "control": "control", "treatment": "treatment"}.get(arm_value)
    if arm is None:
        _refuse("invalid-arm", arm=arm_value)

    process_timed_out = raw["process_timed_out"]
    if not isinstance(process_timed_out, bool):
        _refuse("process-timed-out-boolean-required")
    workspace_root = _canonical_workspace_root(raw["workspace_root"])
    codex_items = _join_codex_lifecycles(
        _sequence(raw["codex_events"], "codex_events"),
        workspace_root,
        process_timed_out,
    )
    proxy_calls, _ = _join_proxy_lifecycles(
        _sequence(raw["proxy_rows"], "proxy_rows"),
        arm,
        expected,
        process_timed_out,
    )
    proxy_matches = _codex_proxy_bijection(codex_items, proxy_calls)

    before_tree = raw["before_tree_sha256"]
    after_tree = raw["after_tree_sha256"]
    changed_files = _sequence(raw["changed_files"], "changed_files")

    oracle_events: list[dict[str, Any]] = [{
        "event_type": "file_snapshot",
        "sequence": 0,
        "snapshot": "before",
        "tree_sha256": before_tree,
    }]
    successful_mutation = False
    codex_file_change_paths: set[str] = set()

    for item in codex_items:
        semantics = item["semantics"]
        proxy_match = proxy_matches.get(item["item_id"])
        tool, activity, route, arguments, outcome, refusal_reason = _oracle_tool_shape(
            item, phase, proxy_match
        )
        event_id = f"codex-item:{item['item_id']}"
        request_id = (
            proxy_match["request_id"]
            if proxy_match is not None
            else f"codex-item:{item['item_id']}"
        )
        argument_hash = oracle.canonical_argument_hash(arguments)
        oracle_events.append({
            "event_type": "tool_start",
            "sequence": len(oracle_events),
            "event_id": event_id,
            "request_id": request_id,
            "tool": tool,
            "activity": activity,
            "mutation_route": route,
            "arguments": arguments,
        })
        if tool == "inspect_clojure" and proxy_match is not None:
            proxy_event_id = f"proxy:{proxy_match['request_id']}"
            oracle_events.append({
                "event_type": "proxy_call",
                "sequence": len(oracle_events),
                "event_id": proxy_event_id,
                "tool_event_id": event_id,
                "request_id": request_id,
                "arguments": arguments,
                "argument_sha256": argument_hash,
            })
            oracle_events.append({
                "event_type": "proxy_result",
                "sequence": len(oracle_events),
                "event_id": proxy_event_id,
                "tool_event_id": event_id,
                "request_id": request_id,
                "argument_sha256": argument_hash,
                "outcome": proxy_match["outcome"],
                "prepared_request_exposed": proxy_match["prepared_request_exposed"],
                "candidate_descriptor_sha256": proxy_match["candidate_descriptor_sha256"],
                "catalog_sha256": expected["catalog_sha256"],
                "static_surface_sha256": expected["static_surface_sha256"],
            })
        oracle_events.append({
            "event_type": "tool_completion",
            "sequence": len(oracle_events),
            "event_id": event_id,
            "request_id": request_id,
            "tool": tool,
            "activity": activity,
            "mutation_route": route,
            "argument_sha256": argument_hash,
            "outcome": outcome,
            "refusal_reason": refusal_reason,
        })
        if activity == "mutation" and outcome == "success":
            successful_mutation = True
        if semantics["type"] == "file_change" and outcome == "success":
            codex_file_change_paths.update(
                change["path"] for change in semantics["changes"]
            )

    normalized_changes: list[dict[str, Any]] = []
    exact_changed_paths: set[str] = set()
    for index, raw_change in enumerate(changed_files):
        change = _mapping(raw_change, f"changed_files[{index}]")
        required = {
            "change_type", "path", "to_path", "before_sha256", "after_sha256"
        }
        if set(change) != required:
            _refuse("invalid-changed-file-keys", index=index)
        normalized = dict(change)
        normalized_changes.append(normalized)
        exact_changed_paths.add(_nonblank(change["path"], f"changed_files[{index}].path"))
        if isinstance(change.get("to_path"), str):
            exact_changed_paths.add(change["to_path"])

    if normalized_changes and not successful_mutation:
        _refuse("changed-files-without-successful-mutation")
    if (
        before_tree != after_tree
        and codex_file_change_paths
        and not codex_file_change_paths.issubset(exact_changed_paths)
    ):
        _refuse(
            "codex-file-change-facts-mismatch",
            missing=sorted(codex_file_change_paths - exact_changed_paths),
        )
    if before_tree == after_tree and normalized_changes:
        _refuse("changed-file-facts-with-unchanged-tree")
    if before_tree != after_tree and not normalized_changes:
        _refuse("changed-tree-without-file-facts")

    for index, change in enumerate(normalized_changes, start=1):
        oracle_events.append({
            "event_type": "file_change",
            "sequence": len(oracle_events),
            "event_id": f"changed-file:{index}",
            **change,
        })
    oracle_events.append({
        "event_type": "file_snapshot",
        "sequence": len(oracle_events),
        "snapshot": "after",
        "tree_sha256": after_tree,
    })

    return {
        "attempt_id": attempt_id,
        "phase": phase,
        "arm": arm,
        "identities": dict(expected),
        "events": oracle_events,
    }


def adapt_and_compile(
    raw_attempt: Mapping[str, Any],
    expected_identities: Mapping[str, Any],
) -> dict[str, Any]:
    """Adapt raw evidence, invoke the independent oracle, and return its report."""

    strict_input = build_oracle_input(raw_attempt, expected_identities)
    return oracle.compile_evidence(strict_input, expected_identities)


compile_raw_evidence = adapt_and_compile


__all__ = [
    "AdapterError",
    "adapt_and_compile",
    "build_oracle_input",
    "compile_raw_evidence",
]
