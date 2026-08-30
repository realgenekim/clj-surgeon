#!/usr/bin/env python3
"""Fixture-scoped local MCP registry for the frozen routing tranche."""

from __future__ import annotations

import hashlib
import json
import os
import sys
import time
from pathlib import Path
from typing import Any


SCREEN = os.environ["ROUTING_SCREEN"]
ARM = os.environ["ROUTING_ARM"]
WORKSPACE = Path(os.environ["ROUTING_WORKSPACE"]).resolve()
LOG_PATH = Path(os.environ["ROUTING_SERVER_LOG"])
FIXTURE = json.loads(os.environ["ROUTING_FIXTURE"])
TARGET = WORKSPACE / FIXTURE["file"]

if SCREEN not in {"native-description", "action-native-name", "minimal-schema", "refusal-handoff"}:
    raise RuntimeError(f"unknown screen: {SCREEN}")
if ARM not in {"A", "B"}:
    raise RuntimeError(f"unknown arm: {ARM}")


def canonical(value: Any) -> bytes:
    return json.dumps(value, sort_keys=True, separators=(",", ":"), ensure_ascii=False).encode()


def digest(value: Any) -> str:
    return hashlib.sha256(canonical(value)).hexdigest()


def log(event: str, **data: Any) -> None:
    LOG_PATH.parent.mkdir(parents=True, exist_ok=True)
    with LOG_PATH.open("a", encoding="utf-8") as handle:
        handle.write(json.dumps({"ts_ns": time.time_ns(), "event": event, **data}, sort_keys=True) + "\n")
        handle.flush()
        os.fsync(handle.fileno())


def object_schema(properties: dict[str, Any], required: list[str]) -> dict[str, Any]:
    return {"type": "object", "properties": properties, "required": required, "additionalProperties": False}


FILE = {"type": "string", "description": "Repository-relative path to one existing file."}
TEXT = {"type": "string"}
WITHIN = object_schema({"form": {"type": "string"}}, ["form"])
EDIT = object_schema(
    {
        "file": FILE,
        "within": WITHIN,
        "from": TEXT,
        "to": TEXT,
        "matches": {"type": "integer", "const": 1},
    },
    ["file", "within", "from", "to", "matches"],
)
GENERAL_SCHEMA = object_schema(
    {
        "operation": {"type": "string", "enum": ["guarded_edit"]},
        "changes": {"type": "array", "items": EDIT, "minItems": 1, "maxItems": 1},
        "options": object_schema({"verify_written_bytes": {"type": "boolean"}}, ["verify_written_bytes"]),
    },
    ["operation", "changes", "options"],
)
MINIMAL_SCHEMA = object_schema(
    {"file": FILE, "old": TEXT, "new": TEXT, "within_form": {"type": "string"}},
    ["file", "old", "new"],
)
NATIVE_SCHEMA = object_schema({"file": FILE, "old": TEXT, "new": TEXT}, ["file", "old", "new"])


def structural_name() -> str:
    if SCREEN == "action-native-name" and ARM == "A":
        return "clj_surgeon"
    return "edit_clojure"


def structural_schema() -> dict[str, Any]:
    if SCREEN == "minimal-schema" and ARM == "B":
        return MINIMAL_SCHEMA
    if SCREEN == "refusal-handoff":
        return GENERAL_SCHEMA
    return GENERAL_SCHEMA


def native_description() -> str:
    base = "Apply one exact text replacement to an existing file. Use for small, literal, localized edits."
    if SCREEN == "native-description" and ARM == "B":
        return base + " For bounded edits to existing Clojure forms, call `edit_clojure` instead; use this tool for prose, new files, or unsupported changes."
    return base


def tool_list() -> dict[str, Any]:
    return {
        "tools": [
            {"name": "native_patch", "description": native_description(), "inputSchema": NATIVE_SCHEMA},
            {
                "name": structural_name(),
                "description": "Perform one guarded structural Clojure edit within a named top-level form and verify written bytes.",
                "inputSchema": structural_schema(),
            },
        ]
    }


def expected_general() -> dict[str, Any]:
    return {
        "operation": "guarded_edit",
        "changes": [
            {
                "file": FIXTURE["file"],
                "within": {"form": FIXTURE["owner"]},
                "from": FIXTURE["old"],
                "to": FIXTURE["new"],
                "matches": 1,
            }
        ],
        "options": {"verify_written_bytes": True},
    }


def normalized(name: str, arguments: dict[str, Any]) -> dict[str, Any]:
    if name == "native_patch":
        return {"file": arguments.get("file"), "old": arguments.get("old"), "new": arguments.get("new"), "within_form": None}
    if SCREEN == "minimal-schema" and ARM == "B":
        return {
            "file": arguments.get("file"),
            "old": arguments.get("old"),
            "new": arguments.get("new"),
            "within_form": arguments.get("within_form"),
        }
    changes = arguments.get("changes")
    change = changes[0] if isinstance(changes, list) and len(changes) == 1 and isinstance(changes[0], dict) else {}
    within = change.get("within") if isinstance(change.get("within"), dict) else {}
    return {
        "file": change.get("file"),
        "old": change.get("from"),
        "new": change.get("to"),
        "within_form": within.get("form"),
        "operation": arguments.get("operation"),
        "matches": change.get("matches"),
        "verify": (arguments.get("options") or {}).get("verify_written_bytes") if isinstance(arguments.get("options"), dict) else None,
    }


def validate(name: str, arguments: dict[str, Any]) -> tuple[bool, str, dict[str, Any]]:
    row = normalized(name, arguments)
    expected = {"file": FIXTURE["file"], "old": FIXTURE["old"], "new": FIXTURE["new"]}
    for key, value in expected.items():
        if row.get(key) != value:
            return False, f"{key} must equal the frozen requested value", row
    if name != "native_patch":
        if row.get("within_form") not in {None, FIXTURE["owner"]}:
            return False, "within_form names the wrong owner", row
        if not (SCREEN == "minimal-schema" and ARM == "B"):
            if row.get("within_form") != FIXTURE["owner"] or row.get("operation") != "guarded_edit" or row.get("matches") != 1 or row.get("verify") is not True:
                return False, "general guarded schema is incomplete", row
    return True, "ok", row


def apply_edit() -> tuple[bool, str]:
    source = TARGET.read_text(encoding="utf-8")
    if source.count(FIXTURE["old"]) != 1:
        return False, "old literal cardinality is not one"
    owner_anchor = f"(defn {FIXTURE['owner']} "
    if owner_anchor not in source:
        return False, "frozen owner is absent"
    updated = source.replace(FIXTURE["old"], FIXTURE["new"], 1)
    TARGET.write_text(updated, encoding="utf-8")
    if TARGET.read_text(encoding="utf-8") != updated:
        return False, "written-byte verification failed"
    return True, "changed=1; guard_verified=true; verification_complete=true; next_action=none"


TOOLS = tool_list()
log(
    "server_ready",
    screen=SCREEN,
    arm=ARM,
    fixture=FIXTURE["id"],
    offered_tool_names=[tool["name"] for tool in TOOLS["tools"]],
    offered_tool_list_sha256=digest(TOOLS),
)


def emit(payload: dict[str, Any]) -> None:
    sys.stdout.write(json.dumps(payload, separators=(",", ":")) + "\n")
    sys.stdout.flush()


for line in sys.stdin:
    if not line.strip():
        continue
    message = json.loads(line)
    request_id = message.get("id")
    method = message.get("method")
    if request_id is None:
        continue
    try:
        if method == "initialize":
            result = {
                "protocolVersion": message.get("params", {}).get("protocolVersion", "2025-03-26"),
                "capabilities": {"tools": {"listChanged": False}},
                "serverInfo": {"name": "routing-lab", "version": "frozen-v1"},
                "instructions": "Synthetic routing screen. Use only the two offered mutation tools.",
            }
        elif method == "tools/list":
            result = TOOLS
        elif method == "tools/call":
            params = message.get("params", {})
            name = params.get("name")
            arguments = params.get("arguments") if isinstance(params.get("arguments"), dict) else {}
            log("tool_call", name=name, arguments=arguments, arguments_sha256=digest(arguments))
            if name not in {"native_patch", structural_name()}:
                raise ValueError("tool not offered")
            valid, reason, normalized_args = validate(name, arguments)
            if not valid:
                log("call_rejected", name=name, reason=reason, normalized=normalized_args)
                result = {"content": [{"type": "text", "text": "Invalid frozen-fixture call: " + reason}], "isError": True}
            elif SCREEN == "refusal-handoff" and ARM == "B" and name == "native_patch":
                payload = json.dumps(expected_general(), sort_keys=True)
                text = "Native patch refused this eligible bounded Clojure edit without mutation. Call `edit_clojure` with this schema-valid payload: " + payload
                log("native_refusal", name=name, handoff_tool="edit_clojure", handoff_arguments=expected_general())
                result = {"content": [{"type": "text", "text": text}], "isError": True}
            else:
                ok, text = apply_edit()
                log("mutation_result", name=name, ok=ok, text=text)
                result = {"content": [{"type": "text", "text": text}], "isError": not ok}
        elif method == "ping":
            result = {}
        else:
            raise ValueError("method not found")
        emit({"jsonrpc": "2.0", "id": request_id, "result": result})
    except Exception as exc:
        log("server_error", method=method, error=str(exc))
        emit({"jsonrpc": "2.0", "id": request_id, "error": {"code": -32000, "message": str(exc)}})
