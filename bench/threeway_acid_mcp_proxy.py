#!/usr/bin/env python3
"""Session-retaining stdio-to-HTTP proxy for the three-way acid battery."""

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


UPSTREAM_URL = os.environ["THREEWAY_UPSTREAM"]
WORKSPACE = str(Path(os.environ["THREEWAY_WORKSPACE"]).resolve())
LOG_PATH = Path(os.environ["THREEWAY_PROXY_LOG"])
TOOL_NAMES = set(os.environ["THREEWAY_TOOLS"].split(","))


def canonical_bytes(value: Any) -> bytes:
    return json.dumps(value, sort_keys=True, separators=(",", ":"), ensure_ascii=False).encode()


def digest(value: Any) -> str:
    return hashlib.sha256(canonical_bytes(value)).hexdigest()


def log(event: str, **data: Any) -> None:
    LOG_PATH.parent.mkdir(parents=True, exist_ok=True)
    with LOG_PATH.open("a", encoding="utf-8") as handle:
        handle.write(json.dumps({"ts_ns": time.time_ns(), "event": event, **data}, sort_keys=True) + "\n")
        handle.flush()
        os.fsync(handle.fileno())


class Upstream:
    def __init__(self) -> None:
        self.session_id: str | None = None
        self.next_id = 1

    def post(self, payload: dict[str, Any], expect_body: bool = True) -> dict[str, Any] | None:
        headers = {"Accept": "application/json, text/event-stream", "Content-Type": "application/json"}
        if self.session_id:
            headers["Mcp-Session-Id"] = self.session_id
        request = urllib.request.Request(
            UPSTREAM_URL, data=canonical_bytes(payload), headers=headers, method="POST"
        )
        try:
            with urllib.request.urlopen(request, timeout=90) as response:
                response_session = response.headers.get("Mcp-Session-Id")
                if self.session_id is None:
                    if not response_session:
                        raise RuntimeError("initialize response omitted Mcp-Session-Id")
                    self.session_id = response_session
                elif response_session and response_session != self.session_id:
                    raise RuntimeError("upstream changed Mcp-Session-Id")
                body = response.read()
        except urllib.error.HTTPError as exc:
            body = exc.read()
            raise RuntimeError(f"upstream HTTP {exc.code}: {body.decode('utf-8', 'replace')}") from exc
        if not expect_body or not body:
            return None
        decoded = body.decode()
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
                "clientInfo": {"name": "threeway-acid-session-proxy", "version": "1"},
            },
        )
        self.post({"jsonrpc": "2.0", "method": "notifications/initialized", "params": {}}, False)
        return result


def emit(message: dict[str, Any]) -> None:
    sys.stdout.write(json.dumps(message, separators=(",", ":"), ensure_ascii=False) + "\n")
    sys.stdout.flush()


def error_response(request_id: Any, code: int, message: str) -> dict[str, Any]:
    return {"jsonrpc": "2.0", "id": request_id, "error": {"code": code, "message": message}}


upstream = Upstream()
initialize_result = upstream.initialize()
upstream_list = upstream.request("tools/list", {})
filtered_tools = [tool for tool in upstream_list.get("tools", []) if tool.get("name") in TOOL_NAMES]
if {tool.get("name") for tool in filtered_tools} != TOOL_NAMES:
    raise RuntimeError(f"required tools missing: {sorted(TOOL_NAMES)}")
filtered_list = {"tools": filtered_tools}
log(
    "proxy_ready",
    held_session=True,
    upstream_session_sha256=hashlib.sha256(upstream.session_id.encode()).hexdigest(),
    upstream_server=initialize_result.get("serverInfo"),
    offered_tool_names=sorted(TOOL_NAMES),
    offered_tool_list_sha256=digest(filtered_list),
)

for raw_line in sys.stdin:
    if not raw_line.strip():
        continue
    try:
        message = json.loads(raw_line)
        method = message.get("method")
        request_id = message.get("id")
        if request_id is None:
            log("client_notification", method=method)
            continue
        if method == "initialize":
            result = {
                "protocolVersion": message.get("params", {}).get("protocolVersion", "2025-03-26"),
                "capabilities": {"tools": {"listChanged": False}},
                "serverInfo": {"name": "clj-surgeon", "version": "threeway-acid-v1"},
                "instructions": initialize_result.get("instructions", ""),
            }
        elif method == "tools/list":
            result = filtered_list
            log("client_tools_list", offered_tool_list_sha256=digest(result))
        elif method == "tools/call":
            params = message.get("params", {})
            name = params.get("name")
            arguments = params.get("arguments", {})
            if name not in TOOL_NAMES:
                raise ValueError(f"tool not offered: {name}")
            candidate_workspace = arguments.get("workspace_root")
            if candidate_workspace is not None and str(Path(candidate_workspace).resolve()) != WORKSPACE:
                raise ValueError("workspace_root differs from frozen fixture workspace")
            log("client_tool_call", name=name, arguments_sha256=digest(arguments), arguments=arguments)
            result = upstream.request("tools/call", {"name": name, "arguments": arguments})
            log(
                "client_tool_result",
                name=name,
                is_error=bool(result.get("isError", False)),
                result_sha256=digest(result),
                result=result,
            )
        elif method == "ping":
            result = {}
        else:
            emit(error_response(request_id, -32601, f"method not found: {method}"))
            continue
        emit({"jsonrpc": "2.0", "id": request_id, "result": result})
    except Exception as exc:
        log("request_error", error=str(exc))
        emit(error_response(message.get("id"), -32000, str(exc)))
