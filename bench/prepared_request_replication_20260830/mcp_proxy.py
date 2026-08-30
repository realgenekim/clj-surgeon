#!/usr/bin/env python3
"""Fixture-scoped proxy for the prepared-request replication.

Both arms forward the exact production inspect_clojure/edit_clojure tools.
Arm P appends one exact ready-to-submit edit_clojure argument object to a
successful inspection. Arm U emits the upstream result byte-for-data unchanged.
"""

from __future__ import annotations

import hashlib
import json
import os
import sys
import time
import urllib.error
import urllib.request
from pathlib import Path
from typing import Any


ROOT = Path(__file__).resolve().parent
UPSTREAM_URL = os.environ.get("PREPARED_UPSTREAM", "http://127.0.0.1:7888/mcp")
ARM = os.environ["PREPARED_ARM"]
WORKSPACE = str(Path(os.environ["PREPARED_WORKSPACE"]).resolve())
LOG_PATH = Path(os.environ["PREPARED_SERVER_LOG"])
INSPECT_TEMPLATE = json.loads((ROOT / "inspect-template.json").read_text(encoding="utf-8"))
EDIT_TEMPLATE = json.loads((ROOT / "edit-template.json").read_text(encoding="utf-8"))
TOOL_NAMES = {"inspect_clojure", "edit_clojure"}
PAYLOAD_PREFIX = "Prepared guarded edit_clojure request (submit this exact argument object unchanged if you choose that route):\n"

if ARM not in {"U", "P"}:
    raise RuntimeError(f"invalid arm: {ARM!r}")


def canonical_bytes(value: Any) -> bytes:
    return json.dumps(value, sort_keys=True, separators=(",", ":"), ensure_ascii=False).encode("utf-8")


def digest(value: Any) -> str:
    return hashlib.sha256(canonical_bytes(value)).hexdigest()


def log(event: str, **data: Any) -> None:
    LOG_PATH.parent.mkdir(parents=True, exist_ok=True)
    record = {"ts_ns": time.time_ns(), "event": event, **data}
    with LOG_PATH.open("a", encoding="utf-8") as handle:
        handle.write(json.dumps(record, sort_keys=True, ensure_ascii=False) + "\n")
        handle.flush()
        os.fsync(handle.fileno())


def prepared_arguments() -> dict[str, Any]:
    return {"workspace_root": WORKSPACE, **json.loads(json.dumps(EDIT_TEMPLATE))}


def expected_inspect_arguments() -> dict[str, Any]:
    return {"workspace_root": WORKSPACE, **json.loads(json.dumps(INSPECT_TEMPLATE))}


class Upstream:
    def __init__(self) -> None:
        self.session_id: str | None = None
        self.next_id = 1

    def post(self, payload: dict[str, Any], expect_body: bool = True) -> dict[str, Any] | None:
        headers = {"Accept": "application/json, text/event-stream", "Content-Type": "application/json"}
        if self.session_id:
            headers["Mcp-Session-Id"] = self.session_id
        request = urllib.request.Request(UPSTREAM_URL, data=canonical_bytes(payload), headers=headers, method="POST")
        try:
            with urllib.request.urlopen(request, timeout=45) as response:
                if not self.session_id:
                    self.session_id = response.headers.get("Mcp-Session-Id")
                body = response.read()
        except urllib.error.HTTPError as exc:
            body = exc.read()
            raise RuntimeError(f"upstream HTTP {exc.code}: {body.decode('utf-8', 'replace')}") from exc
        if not expect_body or not body:
            return None
        decoded = body.decode("utf-8")
        if decoded.lstrip().startswith("data:") or "\ndata:" in decoded:
            rows = [line[5:].strip() for line in decoded.splitlines() if line.startswith("data:")]
            if not rows:
                raise RuntimeError("upstream SSE response had no data event")
            decoded = rows[-1]
        return json.loads(decoded)

    def request(self, method: str, params: dict[str, Any]) -> dict[str, Any]:
        request_id = self.next_id
        self.next_id += 1
        response = self.post({"jsonrpc": "2.0", "id": request_id, "method": method, "params": params})
        assert response is not None
        if "error" in response:
            raise RuntimeError(f"upstream {method} error: {response['error']}")
        return response["result"]

    def initialize(self) -> dict[str, Any]:
        result = self.request(
            "initialize",
            {
                "protocolVersion": "2025-03-26",
                "capabilities": {},
                "clientInfo": {"name": "prepared-request-replication-proxy", "version": "1"},
            },
        )
        self.post({"jsonrpc": "2.0", "method": "notifications/initialized", "params": {}}, expect_body=False)
        return result


def validate_workspace(arguments: dict[str, Any]) -> None:
    candidate = arguments.get("workspace_root")
    if not isinstance(candidate, str) or str(Path(candidate).resolve()) != WORKSPACE:
        raise ValueError("workspace_root must equal the fixture workspace")


upstream = Upstream()
upstream_initialize = upstream.initialize()
upstream_list = upstream.request("tools/list", {})
filtered_tools = [tool for tool in upstream_list.get("tools", []) if tool.get("name") in TOOL_NAMES]
if {tool.get("name") for tool in filtered_tools} != TOOL_NAMES:
    raise RuntimeError(f"required production tools missing: {[tool.get('name') for tool in filtered_tools]}")
filtered_list = {"tools": filtered_tools}
log(
    "proxy_ready",
    arm=ARM,
    upstream_server=upstream_initialize.get("serverInfo"),
    server_instructions=upstream_initialize.get("instructions", ""),
    server_instructions_sha256=digest(upstream_initialize.get("instructions", "")),
    upstream_tool_names=sorted(tool.get("name") for tool in upstream_list.get("tools", [])),
    offered_tool_names=sorted(TOOL_NAMES),
    offered_tool_list_sha256=digest(filtered_list),
    offered_tool_list=filtered_list,
    prepared_arguments_sha256=digest(prepared_arguments()),
)


def emit(message: dict[str, Any]) -> None:
    sys.stdout.write(json.dumps(message, separators=(",", ":"), ensure_ascii=False) + "\n")
    sys.stdout.flush()


def error_response(request_id: Any, code: int, message: str) -> dict[str, Any]:
    return {"jsonrpc": "2.0", "id": request_id, "error": {"code": code, "message": message}}


for raw_line in sys.stdin:
    if not raw_line.strip():
        continue
    try:
        message = json.loads(raw_line)
    except json.JSONDecodeError as exc:
        log("invalid_json", error=str(exc))
        continue

    method = message.get("method")
    request_id = message.get("id")
    if request_id is None:
        log("client_notification", method=method)
        continue

    try:
        if method == "initialize":
            result = {
                "protocolVersion": message.get("params", {}).get("protocolVersion", "2025-03-26"),
                "capabilities": {"tools": {"listChanged": False}},
                "serverInfo": {"name": "clj-surgeon", "version": "prepared-request-replication-v1"},
                "instructions": upstream_initialize.get("instructions", ""),
            }
        elif method == "tools/list":
            result = filtered_list
            log("client_tools_list", offered_tool_names=sorted(TOOL_NAMES), offered_tool_list_sha256=digest(result))
        elif method == "tools/call":
            params = message.get("params", {})
            name = params.get("name")
            arguments = params.get("arguments", {})
            if name not in TOOL_NAMES:
                raise ValueError(f"tool is not offered: {name}")
            validate_workspace(arguments)
            if name == "inspect_clojure" and arguments != expected_inspect_arguments():
                raise ValueError("inspect_clojure arguments differ from the frozen batched request")
            log("client_tool_call", name=name, arguments=arguments, arguments_sha256=digest(arguments))
            result = upstream.request("tools/call", {"name": name, "arguments": arguments})
            upstream_result = json.loads(json.dumps(result))
            prepared_emitted = False
            if name == "inspect_clojure" and ARM == "P" and not result.get("isError", False):
                payload = json.dumps(prepared_arguments(), indent=2, sort_keys=True, ensure_ascii=False)
                result.setdefault("content", []).append({"type": "text", "text": PAYLOAD_PREFIX + payload})
                prepared_emitted = True
            log(
                "client_tool_result",
                name=name,
                is_error=bool(result.get("isError", False)),
                upstream_result_sha256=digest(upstream_result),
                emitted_result_sha256=digest(result),
                prepared_emitted=prepared_emitted,
                prepared_arguments_sha256=digest(prepared_arguments()) if prepared_emitted else None,
                upstream_result=upstream_result,
                emitted_result=result,
            )
        elif method == "ping":
            result = {}
        else:
            emit(error_response(request_id, -32601, f"method not found: {method}"))
            continue
        emit({"jsonrpc": "2.0", "id": request_id, "result": result})
    except Exception as exc:
        log("request_error", method=method, error=str(exc))
        emit(error_response(request_id, -32000, str(exc)))
