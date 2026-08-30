#!/usr/bin/env python3
"""Verify read normalization through the installed shared MCP HTTP route."""

import argparse
import hashlib
import json
import urllib.error
import urllib.request
from pathlib import Path


PROTOCOL_VERSION = "2024-11-05"
TOOL_NAME = "inspect_clojure"


def compact_json(value):
    return json.dumps(value, ensure_ascii=False, separators=(",", ":")).encode("utf-8")


def sha256_bytes(value):
    return hashlib.sha256(value).hexdigest()


def sha256_file(path):
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for chunk in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def response_payload(body):
    if not body:
        return None
    text = body.decode("utf-8")
    if text.startswith("{"):
        return json.loads(text)
    for line in text.splitlines():
        if line.startswith("data: "):
            return json.loads(line[6:])
    raise RuntimeError(f"HTTP response contained no JSON payload: {text[:300]!r}")


def post(url, request_value, session_id=None):
    body = compact_json(request_value)
    headers = {
        "Accept": "application/json, text/event-stream",
        "Content-Type": "application/json",
    }
    if session_id:
        headers["Mcp-Session-Id"] = session_id
    request = urllib.request.Request(url, data=body, headers=headers, method="POST")
    try:
        with urllib.request.urlopen(request, timeout=30) as response:
            response_body = response.read()
            return {
                "status": response.status,
                "headers": dict(response.headers.items()),
                "request_body": body,
                "response_body": response_body,
                "json": response_payload(response_body),
            }
    except urllib.error.HTTPError as error:
        response_body = error.read()
        return {
            "status": error.code,
            "headers": dict(error.headers.items()),
            "request_body": body,
            "response_body": response_body,
            "json": response_payload(response_body),
        }


def write_exchange(result_dir, name, exchange):
    (result_dir / f"{name}.request.json").write_bytes(exchange["request_body"])
    (result_dir / f"{name}.response.body").write_bytes(exchange["response_body"])
    (result_dir / f"{name}.response.headers.json").write_bytes(
        compact_json({"status": exchange["status"], "headers": exchange["headers"]})
        + b"\n"
    )
    if exchange["json"] is not None:
        (result_dir / f"{name}.response.json").write_bytes(
            compact_json(exchange["json"]) + b"\n"
        )


def structured_content(response):
    return ((response or {}).get("result") or {}).get("structuredContent") or {}


def manifest(result_dir):
    entries = []
    for path in sorted(result_dir.rglob("*")):
        if path.is_file() and path.name != "MANIFEST.sha256":
            entries.append(f"{sha256_file(path)}  {path.relative_to(result_dir)}")
    rendered = "\n".join(entries) + "\n"
    manifest_path = result_dir / "MANIFEST.sha256"
    manifest_path.write_text(rendered)
    return sha256_bytes(rendered.encode("utf-8"))


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--url", default="http://127.0.0.1:7888/mcp")
    parser.add_argument("--workspace", type=Path, required=True)
    parser.add_argument("--result-dir", type=Path, required=True)
    args = parser.parse_args()

    if args.result_dir.exists():
        raise RuntimeError("result directory must not exist")
    args.result_dir.mkdir(parents=True)

    source = args.workspace / "src/clj_surgeon/mcp_inspect.clj"
    source_before = sha256_file(source)

    initialize = post(
        args.url,
        {
            "jsonrpc": "2.0",
            "id": 1,
            "method": "initialize",
            "params": {
                "protocolVersion": PROTOCOL_VERSION,
                "capabilities": {},
                "clientInfo": {
                    "name": "installed-read-normalization-verifier",
                    "version": "1",
                },
            },
        },
    )
    write_exchange(args.result_dir, "00-initialize", initialize)
    session_id = initialize["headers"].get("Mcp-session-id") or initialize[
        "headers"
    ].get("Mcp-Session-Id")
    if not session_id:
        raise RuntimeError("initialize response did not return Mcp-Session-Id")

    initialized = post(
        args.url,
        {"jsonrpc": "2.0", "method": "notifications/initialized"},
        session_id,
    )
    write_exchange(args.result_dir, "01-initialized", initialized)

    tools_list = post(
        args.url,
        {"jsonrpc": "2.0", "id": 2, "method": "tools/list", "params": {}},
        session_id,
    )
    write_exchange(args.result_dir, "02-tools-list", tools_list)
    tools = ((tools_list["json"] or {}).get("result") or {}).get("tools") or []
    inspect_tools = [tool for tool in tools if tool.get("name") == TOOL_NAME]
    if len(inspect_tools) != 1:
        raise RuntimeError(f"expected one {TOOL_NAME} tool, found {len(inspect_tools)}")
    inspect_tool = inspect_tools[0]
    variants = inspect_tool["inputSchema"]["properties"]["requests"]["items"][
        "oneOf"
    ]
    operationless_variants = [
        variant
        for variant in variants
        if "operation" not in variant.get("required", [])
        and {"file", "forms", "expect"}.issubset(set(variant.get("required", [])))
    ]

    operationless_arguments = {
        "workspace_root": str(args.workspace),
        "requests": [
            {
                "file": "src/clj_surgeon/mcp_inspect.clj",
                "forms": ["validate-inspect-params"],
                "expect": {"forms": 1},
            }
        ],
        "expect": {"requests": 1, "files": 1},
    }
    operationless = post(
        args.url,
        {
            "jsonrpc": "2.0",
            "id": 3,
            "method": "tools/call",
            "params": {"name": TOOL_NAME, "arguments": operationless_arguments},
        },
        session_id,
    )
    write_exchange(args.result_dir, "03-operationless-forms", operationless)
    operationless_result = (operationless["json"] or {}).get("result") or {}
    operationless_structured = structured_content(operationless["json"])
    operationless_ok = (
        not operationless_result.get("isError", False)
        and operationless_structured.get("ok") is True
        and operationless_structured.get("read_complete") is True
        and len(operationless_structured.get("results") or []) == 1
        and (operationless_structured.get("results") or [{}])[0].get("id")
        == "request-1"
    )

    mixed_arguments = {
        "workspace_root": str(args.workspace),
        "requests": [
            {
                "id": "supplied",
                "operation": "forms",
                "file": "src/clj_surgeon/mcp_inspect.clj",
                "forms": ["validate-inspect-params"],
                "expect": {"forms": 1},
            },
            {
                "operation": "forms",
                "file": "src/clj_surgeon/mcp_inspect.clj",
                "forms": ["normalize-request-ids!"],
                "expect": {"forms": 1},
            },
        ],
        "expect": {"requests": 2, "files": 1},
    }
    mixed = post(
        args.url,
        {
            "jsonrpc": "2.0",
            "id": 4,
            "method": "tools/call",
            "params": {"name": TOOL_NAME, "arguments": mixed_arguments},
        },
        session_id,
    )
    write_exchange(args.result_dir, "04-mixed-request-ids", mixed)
    mixed_result = (mixed["json"] or {}).get("result") or {}
    mixed_structured = structured_content(mixed["json"])
    mixed_refused = (
        mixed_result.get("isError") is True
        and mixed_structured.get("reason") == "mixed-request-ids"
        and mixed_structured.get("source_unchanged") is True
        and mixed_structured.get("read_started") is False
        and mixed_structured.get("read_complete") is False
    )

    source_after = sha256_file(source)
    report = {
        "schema": "clj-surgeon.installed-read-normalization-verification.v1",
        "url": args.url,
        "workspace": str(args.workspace),
        "session_id_sha256": sha256_bytes(session_id.encode("utf-8")),
        "tools": {
            "count": len(tools),
            "names": [tool.get("name") for tool in tools],
            "inspect_request_variant_count": len(variants),
            "operationless_forms_variant_count": len(operationless_variants),
            "inspect_input_schema_sha256": sha256_bytes(
                compact_json(inspect_tool["inputSchema"])
            ),
        },
        "operationless_call": {
            "ok": operationless_ok,
            "generated_request_id": (
                operationless_structured.get("results") or [{}]
            )[0].get("id"),
            "read_complete": operationless_structured.get("read_complete"),
            "file_hashes": operationless_structured.get("file_hashes"),
        },
        "mixed_request_ids": {
            "refused": mixed_refused,
            "reason": mixed_structured.get("reason"),
            "error_type": mixed_structured.get("error_type"),
            "source_unchanged": mixed_structured.get("source_unchanged"),
            "read_started": mixed_structured.get("read_started"),
            "read_complete": mixed_structured.get("read_complete"),
        },
        "source": {
            "path": str(source),
            "before_sha256": source_before,
            "after_sha256": source_after,
            "unchanged": source_before == source_after,
        },
    }
    report["ok"] = (
        len(variants) == 5
        and len(operationless_variants) == 1
        and operationless_ok
        and mixed_refused
        and source_before == source_after
    )
    report_path = args.result_dir / "report.json"
    report_path.write_bytes(compact_json(report) + b"\n")
    manifest_sha = manifest(args.result_dir)
    print(
        json.dumps(
            {
                "ok": report["ok"],
                "report": str(report_path),
                "report_sha256": sha256_file(report_path),
                "manifest_sha256": manifest_sha,
            },
            separators=(",", ":"),
        )
    )
    if not report["ok"]:
        raise SystemExit(1)


if __name__ == "__main__":
    main()
