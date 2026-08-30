#!/usr/bin/env python3
"""Experiment-only MCP response proxy for the prepared-request screen.

Both arms expose and forward the complete ordered production tool catalog.
Control returns every upstream result unchanged. Treatment may add one
non-executable prepared_request descriptor to an actual successful forms
result. The descriptor is derived only from returned source evidence; it never
contains a future replacement value.
"""

from __future__ import annotations

import copy
import hashlib
import json
import os
import re
import sys
import time
import urllib.error
import urllib.request
from pathlib import Path, PurePosixPath
from typing import Any


ARMS = {"C", "T"}
MAX_PREPARED_ITEMS = 6
MAX_DESCRIPTOR_BYTES = 4_096
COACHING = (
    "If you independently decide these exact selections are intended mutation "
    "subjects, fill every replacement hole and submit "
    "prepared_request.arguments once to edit_clojure; otherwise ignore the "
    "template."
)
SHA256 = re.compile(r"[0-9a-f]{64}\Z")
SUPPORTED_CLOJURE_SUFFIXES = {".clj", ".cljc", ".cljs"}


def canonical_bytes(value: Any) -> bytes:
    return json.dumps(
        value, sort_keys=True, separators=(",", ":"), ensure_ascii=False
    ).encode("utf-8")


def digest(value: Any) -> str:
    return hashlib.sha256(canonical_bytes(value)).hexdigest()


def _omission(reason: str) -> tuple[None, str]:
    return None, reason


def _sha256(value: Any) -> bool:
    return isinstance(value, str) and SHA256.fullmatch(value) is not None


def _source_sha256(source: str) -> str:
    return hashlib.sha256(source.encode("utf-8")).hexdigest()


def _character_count(value: str) -> int:
    """Return the Java/Clojure UTF-16 character count for one string."""

    return len(value.encode("utf-16-le")) // 2


def _project_relative_clojure_file(value: Any) -> bool:
    if not isinstance(value, str) or not value or value.strip() != value:
        return False
    if "\\" in value or any(ord(character) < 32 for character in value):
        return False
    raw_parts = value.split("/")
    if any(part in {"", ".", ".."} for part in raw_parts):
        return False
    path = PurePosixPath(value)
    return (
        not path.is_absolute()
        and str(path) == value
        and path.suffix in SUPPORTED_CLOJURE_SUFFIXES
    )


def _point(value: Any) -> tuple[int, int] | None:
    if not isinstance(value, dict):
        return None
    line = value.get("line")
    character = value.get("character")
    if (
        not isinstance(line, int)
        or isinstance(line, bool)
        or line < 0
        or not isinstance(character, int)
        or isinstance(character, bool)
        or character < 0
    ):
        return None
    return line, character


def _source_range(value: Any) -> tuple[tuple[int, int], tuple[int, int]] | None:
    if not isinstance(value, dict):
        return None
    start = _point(value.get("start"))
    end = _point(value.get("end"))
    if start is None or end is None or end < start:
        return None
    return start, end


def _valid_anchor(anchor: Any, file_name: str, owner: str, file_hash: str) -> bool:
    if not isinstance(anchor, dict):
        return False
    source_range = _source_range(anchor.get("range"))
    selection_range = _source_range(anchor.get("selection_range"))
    return bool(
        anchor.get("file") == file_name
        and anchor.get("owner") == owner
        and anchor.get("source_sha256") == file_hash
        and source_range is not None
        and selection_range is not None
        and source_range[0] <= selection_range[0]
        and selection_range[1] <= source_range[1]
    )


def validate_workspace(arguments: Any, workspace: str) -> None:
    """Confine an explicit root while preserving the public optional default."""

    if not isinstance(arguments, dict):
        raise ValueError("tool arguments must be an object")
    if "workspace_root" not in arguments:
        return
    candidate = arguments.get("workspace_root")
    candidate_path = Path(candidate) if isinstance(candidate, str) else None
    if (
        candidate_path is None
        or not candidate_path.is_absolute()
        or candidate_path.resolve() != Path(workspace).resolve()
    ):
        raise ValueError("explicit workspace_root must equal the fixture workspace")


def derive_prepared_request(
    result: dict[str, Any],
    *,
    max_items: int = MAX_PREPARED_ITEMS,
    max_bytes: int = MAX_DESCRIPTOR_BYTES,
) -> tuple[dict[str, Any] | None, str | None]:
    """Return one all-or-none descriptor derived from a completed forms read."""

    if result.get("isError", False):
        return _omission("transport-or-tool-error")
    structured = result.get("structuredContent")
    if not isinstance(structured, dict):
        return _omission("structured-content-missing")
    if not structured.get("ok") or not structured.get("read_complete"):
        return _omission("read-incomplete")
    if structured.get("operation") != "inspect_clojure":
        return _omission("not-inspect-result")
    if structured.get("next_action") != "none":
        return _omission("nonterminal-read-result")
    workspace = structured.get("workspace_root")
    results = structured.get("results")
    if not isinstance(workspace, str) or not workspace:
        return _omission("workspace-missing")
    if not isinstance(results, list) or len(results) != 1:
        return _omission("requires-one-forms-result")
    if structured.get("request_count") != 1 or structured.get("file_count") != 1:
        return _omission("batch-cardinality-mismatch")
    file_hashes = structured.get("file_hashes")
    if not isinstance(file_hashes, dict) or len(file_hashes) != 1:
        return _omission("snapshot-evidence-missing")
    if any(
        key in structured
        for key in ("basis", "prepared_basis", "continuation", "retry_template")
    ):
        return _omission("basis-or-partial-result")

    edits: list[dict[str, Any]] = []
    identities: set[tuple[str, str]] = set()
    for read_result in results:
        if not isinstance(read_result, dict) or read_result.get("operation") != "forms":
            return _omission("non-forms-result")
        forms = read_result.get("forms")
        if not isinstance(forms, list) or not (1 <= len(forms) <= max_items):
            return _omission("forms-missing")
        if read_result.get("form_count") != len(forms):
            return _omission("form-count-mismatch")
        request_id = read_result.get("id")
        if not isinstance(request_id, str) or not request_id:
            return _omission("request-id-missing")
        result_file = read_result.get("file")
        result_hash = read_result.get("file_hash")
        if not _project_relative_clojure_file(result_file):
            return _omission("result-file-unsupported")
        if not _sha256(result_hash):
            return _omission("result-hash-missing")
        if file_hashes != {result_file: result_hash}:
            return _omission("snapshot-identity-mismatch")
        snapshot_guards = structured.get("snapshot_guards")
        if snapshot_guards is not None and snapshot_guards != file_hashes:
            return _omission("snapshot-guard-mismatch")
        source_character_count = read_result.get("source_character_count")
        if (
            not isinstance(source_character_count, int)
            or isinstance(source_character_count, bool)
            or source_character_count < 1
        ):
            return _omission("source-character-count-missing")

        for form in forms:
            if not isinstance(form, dict):
                return _omission("form-not-object")
            file_name = form.get("file")
            owner = form.get("name")
            source = form.get("source")
            form_hash = form.get("file_hash")
            anchor = form.get("source_anchor")
            if file_name != result_file or form_hash != result_hash:
                return _omission("form-result-identity-mismatch")
            if not isinstance(owner, str) or not owner:
                return _omission("owner-missing")
            if not isinstance(source, str) or not source:
                return _omission("source-missing")
            if not source.startswith("(") or not _project_relative_clojure_file(
                file_name
            ):
                return _omission("unsupported-source")
            if not _sha256(form.get("hash")):
                return _omission("form-hash-missing")
            if form["hash"] != _source_sha256(source):
                return _omission("form-source-hash-mismatch")
            if not _valid_anchor(anchor, file_name, owner, result_hash):
                return _omission("source-anchor-mismatch")
            identity = (file_name, owner)
            if identity in identities:
                return _omission("duplicate-owner")
            identities.add(identity)
            edits.append(
                {
                    "file": file_name,
                    "within": {"form": owner},
                    "from": source,
                    "to": None,
                    "matches": 1,
                }
            )

        if source_character_count != sum(
            _character_count(form["source"]) for form in forms
        ):
            return _omission("source-character-count-mismatch")

    if structured.get("source_character_count") != sum(
        read_result["source_character_count"] for read_result in results
    ):
        return _omission("batch-source-character-count-mismatch")

    if not edits:
        return _omission("no-editable-selections")
    if len(edits) > max_items:
        return _omission("item-budget-exceeded")

    descriptor = {
        "tool": "edit_clojure",
        "executable": False,
        "write_authority": False,
        "arguments": {"workspace_root": workspace, "edits": edits},
        "caller_holes": [f"arguments.edits[{index}].to" for index in range(len(edits))],
    }
    if len(canonical_bytes(descriptor)) > max_bytes:
        return _omission("byte-budget-exceeded")
    return descriptor, None


def project_inspect_result(
    upstream_result: dict[str, Any], arm: str
) -> tuple[dict[str, Any], dict[str, Any]]:
    """Project one result and return auditable projection metadata."""

    if arm not in ARMS:
        raise ValueError(f"invalid arm: {arm!r}")
    if arm == "C":
        return upstream_result, {
            "eligible": False,
            "prepared_emitted": False,
            "omission_reason": "control-pass-through",
            "prepared_request_sha256": None,
        }

    projected = copy.deepcopy(upstream_result)
    descriptor, omission = derive_prepared_request(projected)
    if descriptor is None:
        return upstream_result, {
            "eligible": False,
            "prepared_emitted": False,
            "omission_reason": omission,
            "prepared_request_sha256": None,
        }

    structured = projected["structuredContent"]
    structured["prepared_request"] = descriptor
    content = projected.get("content")
    text_item = None
    if isinstance(content, list):
        text_item = next(
            (
                item
                for item in content
                if isinstance(item, dict) and item.get("type") == "text"
            ),
            None,
        )
    if text_item is None or not isinstance(text_item.get("text"), str):
        return upstream_result, {
            "eligible": False,
            "prepared_emitted": False,
            "omission_reason": "concise-text-missing",
            "prepared_request_sha256": None,
        }
    text_item["text"] = text_item["text"] + "\n" + COACHING
    return projected, {
        "eligible": True,
        "prepared_emitted": True,
        "omission_reason": None,
        "prepared_request_sha256": digest(descriptor),
    }


def project_tools_list(upstream_list: dict[str, Any]) -> dict[str, Any]:
    """The experiment has no catalog arm: return production order and bytes."""

    return upstream_list


class Upstream:
    def __init__(self, url: str) -> None:
        self.url = url
        self.session_id: str | None = None
        self.next_id = 1

    def post(
        self, payload: dict[str, Any], expect_body: bool = True
    ) -> dict[str, Any] | None:
        headers = {
            "Accept": "application/json, text/event-stream",
            "Content-Type": "application/json",
        }
        if self.session_id:
            headers["Mcp-Session-Id"] = self.session_id
        request = urllib.request.Request(
            self.url,
            data=canonical_bytes(payload),
            headers=headers,
            method="POST",
        )
        try:
            with urllib.request.urlopen(request, timeout=45) as response:
                if not self.session_id:
                    self.session_id = response.headers.get("Mcp-Session-Id")
                body = response.read()
        except urllib.error.HTTPError as exc:
            body = exc.read()
            raise RuntimeError(
                f"upstream HTTP {exc.code}: {body.decode('utf-8', 'replace')}"
            ) from exc
        if not expect_body or not body:
            return None
        decoded = body.decode("utf-8")
        if decoded.lstrip().startswith("data:") or "\ndata:" in decoded:
            rows = [
                line[5:].strip()
                for line in decoded.splitlines()
                if line.startswith("data:")
            ]
            if not rows:
                raise RuntimeError("upstream SSE response had no data event")
            decoded = rows[-1]
        return json.loads(decoded)

    def request(self, method: str, params: dict[str, Any]) -> dict[str, Any]:
        request_id = self.next_id
        self.next_id += 1
        response = self.post(
            {
                "jsonrpc": "2.0",
                "id": request_id,
                "method": method,
                "params": params,
            }
        )
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
                "clientInfo": {
                    "name": "prepared-request-proxy-screen",
                    "version": "1",
                },
            },
        )
        self.post(
            {
                "jsonrpc": "2.0",
                "method": "notifications/initialized",
                "params": {},
            },
            expect_body=False,
        )
        return result


def _emit(message: dict[str, Any]) -> None:
    sys.stdout.write(
        json.dumps(message, separators=(",", ":"), ensure_ascii=False) + "\n"
    )
    sys.stdout.flush()


def _error(request_id: Any, code: int, message: str) -> dict[str, Any]:
    return {
        "jsonrpc": "2.0",
        "id": request_id,
        "error": {"code": code, "message": message},
    }


def main() -> None:
    arm = os.environ["PREPARED_PROXY_ARM"]
    workspace = str(Path(os.environ["PREPARED_PROXY_WORKSPACE"]).resolve())
    log_path = Path(os.environ["PREPARED_PROXY_LOG"])
    upstream_url = os.environ["PREPARED_PROXY_UPSTREAM"]
    if upstream_url == "http://127.0.0.1:7888/mcp":
        raise RuntimeError("shared product runtime is forbidden for this experiment")
    if arm not in ARMS:
        raise RuntimeError(f"invalid arm: {arm!r}")

    def log(event: str, **data: Any) -> None:
        log_path.parent.mkdir(parents=True, exist_ok=True)
        record = {"ts_ns": time.time_ns(), "event": event, **data}
        with log_path.open("a", encoding="utf-8") as handle:
            handle.write(json.dumps(record, sort_keys=True, ensure_ascii=False) + "\n")
            handle.flush()
            os.fsync(handle.fileno())

    upstream = Upstream(upstream_url)
    upstream_initialize = upstream.initialize()
    upstream_list = upstream.request("tools/list", {})
    offered_tool_names = [tool.get("name") for tool in upstream_list.get("tools", [])]
    if (
        "inspect_clojure" not in offered_tool_names
        or "edit_clojure" not in offered_tool_names
    ):
        raise RuntimeError(f"required production tools missing: {offered_tool_names}")
    log(
        "proxy_ready",
        arm=arm,
        upstream_server=upstream_initialize.get("serverInfo"),
        server_instructions=upstream_initialize.get("instructions", ""),
        server_instructions_sha256=digest(upstream_initialize.get("instructions", "")),
        upstream_tool_names=sorted(
            tool.get("name") for tool in upstream_list.get("tools", [])
        ),
        offered_tool_names=offered_tool_names,
        offered_tool_list_sha256=digest(upstream_list),
        offered_tool_list=upstream_list,
    )

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
                    "protocolVersion": message.get("params", {}).get(
                        "protocolVersion", "2025-03-26"
                    ),
                    "capabilities": {"tools": {"listChanged": False}},
                    "serverInfo": {
                        "name": "clj-surgeon",
                        "version": "prepared-request-proxy-screen-v1",
                    },
                    "instructions": upstream_initialize.get("instructions", ""),
                }
            elif method == "tools/list":
                result = project_tools_list(upstream_list)
                log(
                    "client_tools_list",
                    offered_tool_names=offered_tool_names,
                    offered_tool_list_sha256=digest(result),
                )
            elif method == "tools/call":
                params = message.get("params", {})
                name = params.get("name")
                arguments = params.get("arguments", {})
                if name not in offered_tool_names:
                    raise ValueError(f"tool is not offered: {name}")
                validate_workspace(arguments, workspace)
                log(
                    "client_tool_call",
                    request_id=request_id,
                    name=name,
                    arguments=arguments,
                    arguments_sha256=digest(arguments),
                )
                result = upstream.request(
                    "tools/call", {"name": name, "arguments": arguments}
                )
                upstream_result = copy.deepcopy(result)
                projection = {
                    "eligible": False,
                    "prepared_emitted": False,
                    "omission_reason": "not-inspect-call",
                    "prepared_request_sha256": None,
                }
                if name == "inspect_clojure":
                    result, projection = project_inspect_result(result, arm)
                log(
                    "client_tool_result",
                    request_id=request_id,
                    name=name,
                    arguments_sha256=digest(arguments),
                    is_error=bool(result.get("isError", False)),
                    upstream_result_sha256=digest(upstream_result),
                    emitted_result_sha256=digest(result),
                    upstream_result=upstream_result,
                    emitted_result=result,
                    **projection,
                )
            elif method == "ping":
                result = {}
            else:
                _emit(_error(request_id, -32601, f"method not found: {method}"))
                continue
            _emit({"jsonrpc": "2.0", "id": request_id, "result": result})
        except Exception as exc:
            log("request_error", request_id=request_id, method=method, error=str(exc))
            _emit(_error(request_id, -32000, str(exc)))


if __name__ == "__main__":
    main()
