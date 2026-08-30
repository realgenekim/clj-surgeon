"""Independent raw-evidence compiler for the prepared-request proxy screen.

The compiler is pure. It performs no file, process, model, network, runtime, or
freeze operation. Raw lifecycle evidence is authority; caller-supplied summary
booleans are not accepted.
"""

from __future__ import annotations

import hashlib
import json
import math
import re
from collections.abc import Mapping, Sequence
from typing import Any


REPORT_SCHEMA = "prepared-request-proxy-evidence/v1"

_SHA256_RE = re.compile(r"[0-9a-f]{64}\Z")
_PHASES = {"efficacy", "safety"}
_ARMS = {"control", "treatment"}
_ACTIVITIES = {"read", "mutation", "other"}
_OUTCOMES = {"success", "refusal", "failure", "timeout"}
_PROXY_OUTCOMES = {"success", "refusal", "failure", "timeout"}
_CHANGE_TYPES = {"created", "modified", "deleted", "renamed"}
_SNAPSHOTS = {"before", "after"}

_FIXED_TOOL_ACTIVITIES = {
    "inspect_clojure": "read",
    "edit_clojure": "mutation",
    "apply_clojure_changes": "mutation",
    "transform_clojure": "mutation",
    "apply_patch": "mutation",
}

_FIXED_MUTATION_ROUTES = {
    "edit_clojure": "edit-clojure",
    "apply_clojure_changes": "apply-clojure-changes",
    "transform_clojure": "transform-clojure",
    "apply_patch": "native-patch",
}

_MUTATION_ROUTES = {
    "edit-clojure",
    "apply-clojure-changes",
    "transform-clojure",
    "native-patch",
    "native-shell-write",
}

_EXPECTED_IDENTITY_KEYS = {
    "catalog_sha256",
    "static_surface_sha256",
    "candidate_policy_sha256",
}

_TOP_LEVEL_KEYS = {"attempt_id", "phase", "arm", "identities", "events"}

_EVENT_KEYS = {
    "tool_start": {
        "event_type", "sequence", "event_id", "request_id", "tool",
        "activity", "mutation_route", "arguments",
    },
    "tool_completion": {
        "event_type", "sequence", "event_id", "request_id", "tool",
        "activity", "mutation_route", "argument_sha256", "outcome",
        "refusal_reason",
    },
    "proxy_call": {
        "event_type", "sequence", "event_id", "tool_event_id", "request_id",
        "arguments", "argument_sha256",
    },
    "proxy_result": {
        "event_type", "sequence", "event_id", "tool_event_id", "request_id",
        "argument_sha256", "outcome", "prepared_request_exposed",
        "candidate_descriptor_sha256", "catalog_sha256",
        "static_surface_sha256",
    },
    "file_snapshot": {
        "event_type", "sequence", "snapshot", "tree_sha256",
    },
    "file_change": {
        "event_type", "sequence", "event_id", "change_type", "path",
        "to_path", "before_sha256", "after_sha256",
    },
}


class EvidenceError(ValueError):
    """A stable fail-closed evidence refusal."""

    def __init__(self, code: str, **details: Any) -> None:
        self.code = code
        self.details = dict(sorted(details.items()))
        super().__init__(code)


def _refuse(code: str, **details: Any) -> None:
    raise EvidenceError(code, **details)


def _require_mapping(value: Any, path: str) -> Mapping[str, Any]:
    if not isinstance(value, Mapping):
        _refuse("mapping-required", path=path)
    if not all(isinstance(key, str) for key in value):
        _refuse("string-keys-required", path=path)
    return value


def _require_exact_keys(value: Mapping[str, Any], expected: set[str], path: str) -> None:
    actual = set(value)
    if actual != expected:
        _refuse(
            "invalid-keys",
            path=path,
            missing=sorted(expected - actual),
            unknown=sorted(actual - expected),
        )


def _require_nonblank(value: Any, path: str) -> str:
    if not isinstance(value, str) or not value.strip():
        _refuse("nonblank-string-required", path=path)
    return value


def _require_bool(value: Any, path: str) -> bool:
    if not isinstance(value, bool):
        _refuse("boolean-required", path=path)
    return value


def _require_sha256(value: Any, path: str) -> str:
    if not isinstance(value, str) or not _SHA256_RE.fullmatch(value):
        _refuse("sha256-required", path=path)
    return value


def _require_optional_sha256(value: Any, path: str) -> str | None:
    if value is None:
        return None
    return _require_sha256(value, path)


def _validate_json(value: Any, path: str = "arguments") -> None:
    if value is None or isinstance(value, (str, bool, int)):
        return
    if isinstance(value, float):
        if not math.isfinite(value):
            _refuse("nonfinite-json-number", path=path)
        return
    if isinstance(value, list):
        for index, item in enumerate(value):
            _validate_json(item, f"{path}[{index}]")
        return
    if isinstance(value, Mapping):
        if not all(isinstance(key, str) for key in value):
            _refuse("string-keys-required", path=path)
        for key, item in value.items():
            _validate_json(item, f"{path}.{key}")
        return
    _refuse("json-value-required", path=path, value_type=type(value).__name__)


def canonical_json_bytes(value: Any) -> bytes:
    """Return the one canonical JSON representation accepted by the oracle."""

    _validate_json(value)
    return json.dumps(
        value,
        allow_nan=False,
        ensure_ascii=False,
        separators=(",", ":"),
        sort_keys=True,
    ).encode("utf-8")


def canonical_argument_hash(arguments: Mapping[str, Any]) -> str:
    """Hash public JSON arguments independently of mapping insertion order."""

    _require_mapping(arguments, "arguments")
    return hashlib.sha256(canonical_json_bytes(arguments)).hexdigest()


def _validate_identities(
    raw_identities: Any,
    expected_identities: Mapping[str, Any],
) -> dict[str, str]:
    raw = _require_mapping(raw_identities, "identities")
    expected = _require_mapping(expected_identities, "expected_identities")
    _require_exact_keys(raw, _EXPECTED_IDENTITY_KEYS, "identities")
    _require_exact_keys(expected, _EXPECTED_IDENTITY_KEYS, "expected_identities")

    result: dict[str, str] = {}
    for key in sorted(_EXPECTED_IDENTITY_KEYS):
        actual = _require_sha256(raw[key], f"identities.{key}")
        wanted = _require_sha256(expected[key], f"expected_identities.{key}")
        if actual != wanted:
            _refuse("identity-mismatch", identity=key, actual=actual, expected=wanted)
        result[key] = actual
    return result


def _validate_tool_shape(event: Mapping[str, Any], path: str) -> None:
    tool = _require_nonblank(event["tool"], f"{path}.tool")
    activity = event["activity"]
    if activity not in _ACTIVITIES:
        _refuse("unrecognized-tool-activity", path=path, activity=activity)

    fixed_activity = _FIXED_TOOL_ACTIVITIES.get(tool)
    if fixed_activity is not None and activity != fixed_activity:
        _refuse(
            "tool-activity-mismatch",
            path=path,
            tool=tool,
            activity=activity,
            expected=fixed_activity,
        )

    route = event["mutation_route"]
    if activity != "mutation":
        if route is not None:
            _refuse("mutation-route-for-nonmutation", path=path, tool=tool)
        return

    if tool in _FIXED_MUTATION_ROUTES:
        expected_route = _FIXED_MUTATION_ROUTES[tool]
        if route != expected_route:
            _refuse(
                "mutation-route-mismatch",
                path=path,
                tool=tool,
                route=route,
                expected=expected_route,
            )
        return

    if tool == "exec_command" and route == "native-shell-write":
        return

    _refuse("unrecognized-mutation-tool", path=path, tool=tool, route=route)


def _validate_path(value: Any, path: str) -> str:
    text = _require_nonblank(value, path)
    parts = text.split("/")
    if text.startswith("/") or ".." in parts or "." in parts:
        _refuse("project-relative-path-required", path=path, value=text)
    return text


def _validate_file_change(event: Mapping[str, Any], path: str) -> dict[str, Any]:
    change_type = event["change_type"]
    if change_type not in _CHANGE_TYPES:
        _refuse("unrecognized-file-change-activity", path=path, change_type=change_type)

    source_path = _validate_path(event["path"], f"{path}.path")
    to_path = event["to_path"]
    before = _require_optional_sha256(event["before_sha256"], f"{path}.before_sha256")
    after = _require_optional_sha256(event["after_sha256"], f"{path}.after_sha256")

    if change_type == "created":
        valid = to_path is None and before is None and after is not None
    elif change_type == "deleted":
        valid = to_path is None and before is not None and after is None
    elif change_type == "modified":
        valid = to_path is None and before is not None and after is not None and before != after
    else:
        valid = (
            isinstance(to_path, str)
            and before is not None
            and after is not None
            and _validate_path(to_path, f"{path}.to_path") != source_path
        )
    if not valid:
        _refuse("invalid-file-change-shape", path=path, change_type=change_type)

    return {
        "change_type": change_type,
        "path": source_path,
        "to_path": to_path,
        "before_sha256": before,
        "after_sha256": after,
    }


def _event_path(index: int) -> str:
    return f"events[{index}]"


def compile_evidence(
    raw_evidence: Mapping[str, Any],
    expected_identities: Mapping[str, Any],
) -> dict[str, Any]:
    """Validate and compile one attempt's raw evidence.

    ``run_experiment`` can call this function directly. It raises
    :class:`EvidenceError` for every malformed, incomplete, ambiguous, stale,
    or identity-mismatched trace.
    """

    raw = _require_mapping(raw_evidence, "raw_evidence")
    _require_exact_keys(raw, _TOP_LEVEL_KEYS, "raw_evidence")
    attempt_id = _require_nonblank(raw["attempt_id"], "attempt_id")
    phase = raw["phase"]
    arm = raw["arm"]
    if phase not in _PHASES:
        _refuse("invalid-phase", phase=phase)
    if arm not in _ARMS:
        _refuse("invalid-arm", arm=arm)
    identities = _validate_identities(raw["identities"], expected_identities)

    events = raw["events"]
    if not isinstance(events, Sequence) or isinstance(events, (str, bytes)):
        _refuse("event-sequence-required")
    if not events:
        _refuse("events-required")

    tool_starts: dict[str, tuple[int, Mapping[str, Any], str]] = {}
    tool_completions: dict[str, tuple[int, Mapping[str, Any]]] = {}
    proxy_calls: dict[str, tuple[int, Mapping[str, Any], str]] = {}
    proxy_results: dict[str, tuple[int, Mapping[str, Any]]] = {}
    request_to_tool_event: dict[str, str] = {}
    file_event_ids: set[str] = set()
    all_lifecycle_ids: set[str] = set()
    snapshots: dict[str, tuple[int, str]] = {}
    file_changes: list[tuple[int, dict[str, Any]]] = []

    for index, raw_event in enumerate(events):
        path = _event_path(index)
        event = _require_mapping(raw_event, path)
        event_type = event.get("event_type")
        expected_keys = _EVENT_KEYS.get(event_type)
        if expected_keys is None:
            _refuse("unrecognized-event-type", path=path, event_type=event_type)
        _require_exact_keys(event, expected_keys, path)
        if event["sequence"] != index:
            _refuse(
                "noncanonical-event-order",
                path=path,
                actual=event["sequence"],
                expected=index,
            )

        if event_type == "tool_start":
            event_id = _require_nonblank(event["event_id"], f"{path}.event_id")
            request_id = _require_nonblank(event["request_id"], f"{path}.request_id")
            _validate_tool_shape(event, path)
            arguments = _require_mapping(event["arguments"], f"{path}.arguments")
            argument_hash = canonical_argument_hash(arguments)
            if event_id in tool_starts:
                _refuse("duplicate-tool-start", event_id=event_id)
            if event_id in all_lifecycle_ids or event_id in file_event_ids:
                _refuse("duplicate-lifecycle-id", event_id=event_id)
            if request_id in request_to_tool_event:
                _refuse("duplicate-request-id", request_id=request_id)
            tool_starts[event_id] = (index, event, argument_hash)
            request_to_tool_event[request_id] = event_id
            all_lifecycle_ids.add(event_id)

        elif event_type == "tool_completion":
            event_id = _require_nonblank(event["event_id"], f"{path}.event_id")
            _require_nonblank(event["request_id"], f"{path}.request_id")
            _require_sha256(event["argument_sha256"], f"{path}.argument_sha256")
            _validate_tool_shape(event, path)
            if event["outcome"] not in _OUTCOMES:
                _refuse("unrecognized-tool-outcome", path=path, outcome=event["outcome"])
            if event["outcome"] == "refusal":
                _require_nonblank(event["refusal_reason"], f"{path}.refusal_reason")
            elif event["refusal_reason"] is not None:
                _refuse("refusal-reason-without-refusal", path=path)
            if event_id in tool_completions:
                _refuse("duplicate-tool-completion", event_id=event_id)
            if event_id not in tool_starts:
                if any(
                    isinstance(later, Mapping)
                    and later.get("event_type") == "tool_start"
                    and later.get("event_id") == event_id
                    for later in events[index + 1:]
                ):
                    _refuse("reversed-tool-lifecycle", event_id=event_id)
                _refuse("orphan-tool-completion", event_id=event_id)
            if index <= tool_starts[event_id][0]:
                _refuse("reversed-tool-lifecycle", event_id=event_id)
            tool_completions[event_id] = (index, event)

        elif event_type == "proxy_call":
            event_id = _require_nonblank(event["event_id"], f"{path}.event_id")
            _require_nonblank(event["tool_event_id"], f"{path}.tool_event_id")
            _require_nonblank(event["request_id"], f"{path}.request_id")
            arguments = _require_mapping(event["arguments"], f"{path}.arguments")
            argument_hash = canonical_argument_hash(arguments)
            declared_hash = _require_sha256(
                event["argument_sha256"], f"{path}.argument_sha256"
            )
            if declared_hash != argument_hash:
                _refuse("proxy-call-argument-hash-mismatch", event_id=event_id)
            if event_id in proxy_calls:
                _refuse("duplicate-proxy-call", event_id=event_id)
            if event_id in all_lifecycle_ids or event_id in file_event_ids:
                _refuse("duplicate-lifecycle-id", event_id=event_id)
            proxy_calls[event_id] = (index, event, argument_hash)
            all_lifecycle_ids.add(event_id)

        elif event_type == "proxy_result":
            event_id = _require_nonblank(event["event_id"], f"{path}.event_id")
            _require_nonblank(event["tool_event_id"], f"{path}.tool_event_id")
            _require_nonblank(event["request_id"], f"{path}.request_id")
            _require_sha256(event["argument_sha256"], f"{path}.argument_sha256")
            if event["outcome"] not in _PROXY_OUTCOMES:
                _refuse("unrecognized-proxy-outcome", path=path, outcome=event["outcome"])
            _require_bool(
                event["prepared_request_exposed"],
                f"{path}.prepared_request_exposed",
            )
            _require_optional_sha256(
                event["candidate_descriptor_sha256"],
                f"{path}.candidate_descriptor_sha256",
            )
            _require_sha256(event["catalog_sha256"], f"{path}.catalog_sha256")
            _require_sha256(
                event["static_surface_sha256"],
                f"{path}.static_surface_sha256",
            )
            if event_id in proxy_results:
                _refuse("duplicate-proxy-result", event_id=event_id)
            if event_id not in proxy_calls:
                if any(
                    isinstance(later, Mapping)
                    and later.get("event_type") == "proxy_call"
                    and later.get("event_id") == event_id
                    for later in events[index + 1:]
                ):
                    _refuse("reversed-proxy-lifecycle", event_id=event_id)
                _refuse("orphan-proxy-result", event_id=event_id)
            if index <= proxy_calls[event_id][0]:
                _refuse("reversed-proxy-lifecycle", event_id=event_id)
            proxy_results[event_id] = (index, event)

        elif event_type == "file_snapshot":
            snapshot = event["snapshot"]
            if snapshot not in _SNAPSHOTS:
                _refuse("unrecognized-snapshot", path=path, snapshot=snapshot)
            tree_hash = _require_sha256(event["tree_sha256"], f"{path}.tree_sha256")
            if snapshot in snapshots:
                _refuse("duplicate-file-snapshot", snapshot=snapshot)
            snapshots[snapshot] = (index, tree_hash)

        else:
            event_id = _require_nonblank(event["event_id"], f"{path}.event_id")
            if event_id in file_event_ids or event_id in all_lifecycle_ids:
                _refuse("duplicate-event-id", event_id=event_id)
            file_event_ids.add(event_id)
            file_changes.append((index, _validate_file_change(event, path)))

    missing_tool_completions = sorted(set(tool_starts) - set(tool_completions))
    if missing_tool_completions:
        _refuse("orphan-tool-start", event_ids=missing_tool_completions)
    missing_proxy_results = sorted(set(proxy_calls) - set(proxy_results))
    if missing_proxy_results:
        _refuse("orphan-proxy-call", event_ids=missing_proxy_results)
    if set(snapshots) != _SNAPSHOTS:
        _refuse("incomplete-file-snapshots", missing=sorted(_SNAPSHOTS - set(snapshots)))
    if snapshots["before"][0] != 0 or snapshots["after"][0] != len(events) - 1:
        _refuse("snapshot-boundary-order-required")
    if snapshots["before"][0] >= snapshots["after"][0]:
        _refuse("reversed-file-snapshots")

    joined_tools: list[dict[str, Any]] = []
    joined_tool_ids: list[str] = []
    for event_id, (start_index, start, argument_hash) in sorted(
        tool_starts.items(), key=lambda item: item[1][0]
    ):
        completion_index, completion = tool_completions[event_id]
        for key in ("request_id", "tool", "activity", "mutation_route"):
            if completion[key] != start[key]:
                _refuse(
                    "mismatched-tool-lifecycle",
                    event_id=event_id,
                    field=key,
                )
        if completion["argument_sha256"] != argument_hash:
            _refuse("tool-argument-hash-mismatch", event_id=event_id)
        joined_tools.append({
            "tool": start["tool"],
            "activity": start["activity"],
            "mutation_route": start["mutation_route"],
            "argument_sha256": argument_hash,
            "outcome": completion["outcome"],
            "refusal_reason": completion["refusal_reason"],
        })
        joined_tool_ids.append(event_id)
        if completion_index <= start_index:
            _refuse("reversed-tool-lifecycle", event_id=event_id)

    joined_proxies: list[dict[str, Any]] = []
    joined_proxy_ids: list[str] = []
    inspect_tool_ids: set[str] = set()
    for tool_event_id, (_, event, _) in tool_starts.items():
        if event["tool"] == "inspect_clojure":
            inspect_tool_ids.add(tool_event_id)

    proxy_tool_ids: set[str] = set()
    for event_id, (call_index, call, argument_hash) in sorted(
        proxy_calls.items(), key=lambda item: item[1][0]
    ):
        result_index, result = proxy_results[event_id]
        for key in ("tool_event_id", "request_id"):
            if result[key] != call[key]:
                _refuse(
                    "mismatched-proxy-lifecycle",
                    event_id=event_id,
                    field=key,
                )
        if result["argument_sha256"] != argument_hash:
            _refuse("proxy-result-argument-hash-mismatch", event_id=event_id)
        tool_event_id = call["tool_event_id"]
        tool_start = tool_starts.get(tool_event_id)
        tool_completion = tool_completions.get(tool_event_id)
        if tool_start is None or tool_completion is None:
            _refuse("proxy-tool-lifecycle-missing", event_id=event_id)
        if tool_start[1]["tool"] != "inspect_clojure":
            _refuse("proxy-tool-type-mismatch", event_id=event_id)
        if call["request_id"] != tool_start[1]["request_id"]:
            _refuse("proxy-tool-request-id-mismatch", event_id=event_id)
        if argument_hash != tool_start[2]:
            _refuse("proxy-tool-argument-hash-mismatch", event_id=event_id)
        if result["outcome"] != tool_completion[1]["outcome"]:
            _refuse("proxy-tool-outcome-mismatch", event_id=event_id)
        if not (tool_start[0] < call_index < result_index < tool_completion[0]):
            _refuse("invalid-proxy-nesting-order", event_id=event_id)
        if tool_event_id in proxy_tool_ids:
            _refuse("duplicate-proxy-for-tool", tool_event_id=tool_event_id)
        proxy_tool_ids.add(tool_event_id)

        if result["catalog_sha256"] != identities["catalog_sha256"]:
            _refuse("proxy-catalog-identity-mismatch", event_id=event_id)
        if result["static_surface_sha256"] != identities["static_surface_sha256"]:
            _refuse("proxy-static-surface-identity-mismatch", event_id=event_id)
        exposed = result["prepared_request_exposed"]
        candidate_hash = result["candidate_descriptor_sha256"]
        if exposed:
            if arm != "treatment":
                _refuse("control-exposed-candidate", event_id=event_id)
            if result["outcome"] != "success":
                _refuse("candidate-exposed-on-nonsuccess", event_id=event_id)
            if candidate_hash is None:
                _refuse("candidate-identity-required-for-exposure", event_id=event_id)
        elif candidate_hash is not None:
            _refuse("candidate-identity-without-exposure", event_id=event_id)

        joined_proxies.append({
            "argument_sha256": argument_hash,
            "outcome": result["outcome"],
            "prepared_request_exposed": exposed,
            "candidate_descriptor_sha256": candidate_hash,
        })
        joined_proxy_ids.append(event_id)

    extra_proxy_ids = proxy_tool_ids - inspect_tool_ids
    missing_proxy_ids = inspect_tool_ids - proxy_tool_ids
    invalid_missing_proxy_ids = {
        tool_event_id
        for tool_event_id in missing_proxy_ids
        if tool_completions[tool_event_id][1]["outcome"] != "refusal"
    }
    if extra_proxy_ids or invalid_missing_proxy_ids:
        _refuse(
            "inspect-proxy-coverage-mismatch",
            missing=sorted(invalid_missing_proxy_ids),
            extra=sorted(extra_proxy_ids),
        )

    mutation_attempts = [
        {
            "route": tool["mutation_route"],
            "argument_sha256": tool["argument_sha256"],
            "outcome": tool["outcome"],
        }
        for tool in joined_tools
        if tool["activity"] == "mutation"
    ]
    if file_changes and not mutation_attempts:
        _refuse("file-change-without-mutation-attempt")

    before_tree = snapshots["before"][1]
    after_tree = snapshots["after"][1]
    if before_tree != after_tree and not file_changes:
        _refuse("unexplained-tree-change")

    refusals = [
        {"tool": tool["tool"], "reason": tool["refusal_reason"]}
        for tool in joined_tools
        if tool["outcome"] == "refusal"
    ]
    first_route = mutation_attempts[0]["route"] if mutation_attempts else "none"
    prepared_exposure_count = sum(
        proxy["prepared_request_exposed"] for proxy in joined_proxies
    )

    semantic_facts = {
        "schema": REPORT_SCHEMA,
        "phase": phase,
        "arm": arm,
        "identities": identities,
        "tool_calls": joined_tools,
        "proxy_calls": joined_proxies,
        "first_mutation_route": first_route,
        "mutation_attempts": mutation_attempts,
        "mutation_attempt_count": len(mutation_attempts),
        "refusals": refusals,
        "refusal_count": len(refusals),
        "prepared_request_exposure_count": prepared_exposure_count,
        "safety": {
            "prepared_request_exposed": (
                phase == "safety" and prepared_exposure_count > 0
            ),
            "mutation_attempt_count": (
                len(mutation_attempts) if phase == "safety" else 0
            ),
            "file_change_activity_count": (
                len(file_changes) if phase == "safety" else 0
            ),
            "source_changed": phase == "safety" and before_tree != after_tree,
        },
        "correctness_inputs": {
            "before_tree_sha256": before_tree,
            "after_tree_sha256": after_tree,
            "file_changes": [change for _, change in file_changes],
        },
    }
    report_hash = hashlib.sha256(canonical_json_bytes(semantic_facts)).hexdigest()

    return {
        "ok": True,
        "attempt_id": attempt_id,
        "semantic_facts": semantic_facts,
        "evidence_index": {
            "joined_tool_event_ids": joined_tool_ids,
            "joined_proxy_event_ids": joined_proxy_ids,
            "file_change_event_count": len(file_changes),
        },
        "report_sha256": report_hash,
    }


def compile_attempt_evidence(
    raw_evidence: Mapping[str, Any],
    expected_identities: Mapping[str, Any],
) -> dict[str, Any]:
    """Compatibility name for a future ``run_experiment`` caller."""

    return compile_evidence(raw_evidence, expected_identities)


__all__ = [
    "EvidenceError",
    "REPORT_SCHEMA",
    "canonical_argument_hash",
    "canonical_json_bytes",
    "compile_attempt_evidence",
    "compile_evidence",
]
