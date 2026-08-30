#!/usr/bin/env python3
"""Experiment-only stdio MCP proxy for the splice-by-reference screen.

This file deliberately lives under bench/. It does not alter the product
handler. Arm R annotates successful reads with snapshot-bound span labels and
lowers from_ref edits to ordinary edit_clojure arguments before the product MCP
validates them. Arm Q is a pass-through control apart from exposing the same
two-tool catalog.
"""

from __future__ import annotations

import argparse
import copy
import hashlib
import json
import os
from pathlib import Path
import signal
import subprocess
import sys
import threading
import time
from typing import Any


def canonical_json(value: Any) -> str:
    return json.dumps(value, ensure_ascii=False, sort_keys=True, separators=(",", ":"))


def sha256_text(value: str) -> str:
    return hashlib.sha256(value.encode("utf-8")).hexdigest()


def sha256_file(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for block in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(block)
    return digest.hexdigest()


def write_jsonl(handle: Any, value: dict[str, Any], lock: threading.Lock) -> None:
    with lock:
        handle.write(canonical_json(value) + "\n")
        handle.flush()


def structured_content(message: dict[str, Any]) -> dict[str, Any] | None:
    result = message.get("result")
    if not isinstance(result, dict):
        return None
    value = result.get("structuredContent")
    return value if isinstance(value, dict) else None


def source_strings(value: Any) -> list[str]:
    found: list[str] = []
    if isinstance(value, dict):
        for key, child in value.items():
            if key == "source" and isinstance(child, str):
                found.append(child)
            else:
                found.extend(source_strings(child))
    elif isinstance(value, list):
        for child in value:
            found.extend(source_strings(child))
    return found


def add_reference_schema(tool: dict[str, Any]) -> dict[str, Any]:
    result = copy.deepcopy(tool)
    result["description"] = (
        result.get("description", "")
        + " EXPERIMENT-ONLY: after an inspect result advertises snapshot-bound "
        "span labels, an edit may replace canonical from with from_ref (for "
        "example s3). The server resolves and receipts the exact readable "
        "identity before ordinary edit_clojure validation."
    )
    item = result["inputSchema"]["properties"]["edits"]["items"]
    item["properties"]["from_ref"] = {
        "type": "string",
        "pattern": "^s[1-9][0-9]*$",
        "description": (
            "Snapshot-bound span label from the immediately preceding inspect "
            "result. Supply only with to; do not also supply from/old/before."
        ),
    }
    pair_choices = item["allOf"][0]["oneOf"]
    pair_choices.append(
        {
            "required": ["from_ref", "to"],
            "not": {
                "anyOf": [
                    {"required": [name]}
                    for name in ["from", "old", "new", "before", "after"]
                ]
            },
        }
    )
    output = result.setdefault("outputSchema", {})
    output.setdefault("properties", {})["resolved_references"] = {
        "type": "array",
        "items": {"type": "object"},
        "description": "Readable identities resolved from from_ref labels.",
    }
    return result


def filter_and_annotate_tools(message: dict[str, Any], arm: str) -> dict[str, Any]:
    result = copy.deepcopy(message)
    tools = result.get("result", {}).get("tools", [])
    selected = [tool for tool in tools if tool.get("name") in {"inspect_clojure", "edit_clojure"}]
    if arm == "R":
        selected = [add_reference_schema(tool) if tool.get("name") == "edit_clojure" else tool
                    for tool in selected]
    result["result"]["tools"] = selected
    return result


class ReferenceState:
    def __init__(self, workspace: Path, manifest: dict[str, Any]) -> None:
        self.workspace = workspace
        self.file = manifest["file"]
        self.specs = manifest["spans"]
        self.labels: dict[str, dict[str, Any]] = {}

    def annotate(self, message: dict[str, Any]) -> tuple[dict[str, Any], list[dict[str, Any]]]:
        result = copy.deepcopy(message)
        structured = structured_content(result)
        if not structured or structured.get("ok") is not True:
            return result, []
        hashes = structured.get("file_hashes")
        snapshot_hash = hashes.get(self.file) if isinstance(hashes, dict) else None
        if not isinstance(snapshot_hash, str):
            return typed_failure_message(
                result.get("id"), "splice-reference-annotation-failed",
                "inspect result did not carry the fixture file hash", False
            ), []
        sources = source_strings(structured.get("results", []))
        labels: dict[str, dict[str, Any]] = {}
        rendered: list[dict[str, Any]] = []
        for index, spec in enumerate(self.specs, start=1):
            old = spec["from"]
            matching_sources = [source for source in sources if source.count(old) == 1]
            if len(matching_sources) != 1:
                return typed_failure_message(
                    result.get("id"), "splice-reference-annotation-failed",
                    f"expected one returned source containing span {index}; found {len(matching_sources)}",
                    False
                ), []
            owner_source = matching_sources[0]
            start = owner_source.index(old)
            label = f"s{index}"
            identity = {
                "label": label,
                "snapshot_hash": snapshot_hash,
                "file": self.file,
                "within": spec["within"],
                "owner_source_sha256": sha256_text(owner_source),
                "start_utf8_byte": len(owner_source[:start].encode("utf-8")),
                "end_utf8_byte": len(owner_source[: start + len(old)].encode("utf-8")),
                "anchor_sha256": sha256_text(old),
                "anchor_utf8_bytes": len(old.encode("utf-8")),
                "anchor_preview": old[:72].replace("\n", "\\n"),
                "source": old,
                "expected_to_sha256": sha256_text(spec["to"]),
            }
            labels[label] = {**identity, "from": old, "to": spec["to"]}
            rendered.append(identity)
        self.labels = labels
        structured["span_references"] = {
            "schema": "clj-surgeon.splice-reference-read.v1",
            "snapshot_hash": snapshot_hash,
            "labels": rendered,
            "instruction": "Use from_ref instead of re-quoting source in edit_clojure.",
        }
        content = result["result"].setdefault("content", [])
        content.append(
            {
                "type": "text",
                "text": "EXPERIMENT-ONLY snapshot-bound span labels:\n" + canonical_json(
                    structured["span_references"]
                ),
            }
        )
        return result, rendered

    def translate(self, arguments: dict[str, Any]) -> tuple[dict[str, Any] | None, list[dict[str, Any]], dict[str, Any] | None]:
        translated = copy.deepcopy(arguments)
        edits = translated.get("edits")
        if not isinstance(edits, list):
            return translated, [], None
        resolutions: list[dict[str, Any]] = []
        for index, edit in enumerate(edits):
            if not isinstance(edit, dict) or "from_ref" not in edit:
                continue
            label = edit.get("from_ref")
            identity = self.labels.get(label)
            if identity is None:
                return None, resolutions, failure(
                    "splice-reference-unknown-label",
                    f"edit {index} refers to unknown or unread label {label!r}",
                    False,
                )
            if any(name in edit for name in ["from", "old", "new", "before", "after"]):
                return None, resolutions, failure(
                    "splice-reference-ambiguous-fields",
                    f"edit {index} mixes from_ref with an ordinary value pair",
                    False,
                )
            if edit.get("file") != identity["file"] or edit.get("within") != identity["within"]:
                return None, resolutions, failure(
                    "splice-reference-wrong-subject",
                    f"{label} resolves to {identity['file']} {identity['within']}, not the requested subject",
                    True,
                )
            target = edit.get("to")
            if not isinstance(target, str) or sha256_text(target) != identity["expected_to_sha256"]:
                return None, resolutions, failure(
                    "splice-reference-wrong-subject",
                    f"{label} was paired with content intended for a different span",
                    True,
                )
            source_path = self.workspace / identity["file"]
            if not source_path.is_file() or sha256_file(source_path) != identity["snapshot_hash"]:
                return None, resolutions, failure(
                    "splice-reference-stale-snapshot",
                    f"{label} is bound to stale snapshot {identity['snapshot_hash']}",
                    False,
                )
            current_source = source_path.read_text(encoding="utf-8")
            if current_source.count(identity["from"]) != 1:
                return None, resolutions, failure(
                    "splice-reference-resolution-cardinality",
                    f"{label} no longer resolves exactly once in {identity['file']}",
                    True,
                )
            del edit["from_ref"]
            edit["from"] = identity["from"]
            resolution = {key: value for key, value in identity.items()
                          if key not in {"from", "to"}}
            resolution["edit_index"] = index
            resolutions.append(resolution)
        return translated, resolutions, None


def failure(error_type: str, error: str, wrong_subject: bool) -> dict[str, Any]:
    return {
        "ok": False,
        "operation": "edit_clojure",
        "error_type": error_type,
        "error": error,
        "wrong_subject": wrong_subject,
        "source_unchanged": True,
        "mutation_attempted": False,
        "next_action": "correct_reference_request",
    }


def typed_failure_message(request_id: Any, error_type: str, error: str,
                          wrong_subject: bool) -> dict[str, Any]:
    receipt = failure(error_type, error, wrong_subject)
    return {
        "jsonrpc": "2.0",
        "id": request_id,
        "result": {
            "content": [{"type": "text", "text": f"refused · {error_type} · {error}"}],
            "structuredContent": receipt,
            "isError": True,
        },
    }


def annotate_edit_response(message: dict[str, Any], resolutions: list[dict[str, Any]]) -> dict[str, Any]:
    result = copy.deepcopy(message)
    structured = structured_content(result)
    if structured is not None and resolutions:
        structured["resolved_references"] = resolutions
    if resolutions:
        result.setdefault("result", {}).setdefault("content", []).append(
            {"type": "text", "text": "Resolved reference identities:\n" + canonical_json(resolutions)}
        )
    return result


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--arm", choices=["Q", "R"], required=True)
    parser.add_argument("--repo-root", type=Path, required=True)
    parser.add_argument("--workspace", type=Path, required=True)
    parser.add_argument("--manifest", type=Path, required=True)
    parser.add_argument("--receipts", type=Path, required=True)
    parser.add_argument("--stream", type=Path, required=True)
    parser.add_argument("--child-stderr", type=Path, required=True)
    parser.add_argument("--telemetry-dir", type=Path, required=True)
    parser.add_argument("--run-id", required=True)
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    manifest = json.loads(args.manifest.read_text(encoding="utf-8"))
    state = ReferenceState(args.workspace.resolve(), manifest)
    args.receipts.parent.mkdir(parents=True, exist_ok=True)
    args.telemetry_dir.mkdir(parents=True, exist_ok=True)
    lock = threading.Lock()
    pending: dict[Any, dict[str, Any]] = {}
    pending_lock = threading.Lock()
    child_command = [
        "clojure", "-J-Xms64m", "-J-Xmx512m", "-X:clj-surgeon/mcp-stdio",
        ":project-dir", json.dumps(str(args.workspace.resolve())),
        ":receipt-dir", json.dumps(str((args.workspace / ".receipts").resolve())),
        ":telemetry", ":full",
        ":telemetry-dir", json.dumps(str(args.telemetry_dir.resolve())),
        ":run-id", json.dumps(args.run_id),
        ":tool-profile", ":full",
        ":nrepl-port", ":none",
    ]
    with args.receipts.open("a", encoding="utf-8") as receipts, \
         args.stream.open("a", encoding="utf-8") as stream, \
         args.child_stderr.open("w", encoding="utf-8") as child_stderr:
        child = subprocess.Popen(
            child_command,
            cwd=args.repo_root,
            stdin=subprocess.PIPE,
            stdout=subprocess.PIPE,
            stderr=child_stderr,
            text=True,
            bufsize=1,
        )

        def stop_child(*_: Any) -> None:
            if child.poll() is None:
                child.terminate()

        signal.signal(signal.SIGTERM, stop_child)
        signal.signal(signal.SIGINT, stop_child)

        def relay_child() -> None:
            assert child.stdout is not None
            for line in child.stdout:
                try:
                    message = json.loads(line)
                except json.JSONDecodeError:
                    write_jsonl(stream, {"direction": "child-invalid", "line": line.rstrip("\n")}, lock)
                    continue
                with pending_lock:
                    request = pending.pop(message.get("id"), None)
                outgoing = message
                event: dict[str, Any] = {
                    "event": "child-response",
                    "request_id": message.get("id"),
                    "request": request,
                }
                if request and request.get("method") == "tools/list":
                    outgoing = filter_and_annotate_tools(message, args.arm)
                elif request and request.get("tool") == "inspect_clojure" and args.arm == "R":
                    outgoing, labels = state.annotate(message)
                    event["labels"] = labels
                    event["event"] = "read-annotation"
                elif request and request.get("tool") == "edit_clojure":
                    outgoing = annotate_edit_response(message, request.get("resolutions", []))
                    event["event"] = "edit-result"
                    event["resolved_references"] = request.get("resolutions", [])
                    event["product_result"] = structured_content(message)
                write_jsonl(receipts, {**event, "time_ns": time.time_ns()}, lock)
                write_jsonl(stream, {"direction": "proxy-to-client", "message": outgoing}, lock)
                sys.stdout.write(canonical_json(outgoing) + "\n")
                sys.stdout.flush()

        reader = threading.Thread(target=relay_child, name="splice-proxy-child-reader", daemon=True)
        reader.start()
        assert child.stdin is not None
        for line in sys.stdin:
            try:
                message = json.loads(line)
            except json.JSONDecodeError:
                write_jsonl(stream, {"direction": "client-invalid", "line": line.rstrip("\n")}, lock)
                continue
            write_jsonl(stream, {"direction": "client-to-proxy", "message": message}, lock)
            outgoing = message
            request = {"method": message.get("method")}
            if message.get("method") == "tools/call":
                params = message.get("params", {})
                tool = params.get("name")
                arguments = params.get("arguments", {})
                request.update({"tool": tool, "model_arguments": arguments})
                if tool == "edit_clojure" and args.arm == "R":
                    translated, resolutions, refusal = state.translate(arguments)
                    request["resolutions"] = resolutions
                    request["translated_arguments"] = translated
                    if refusal is not None:
                        request["event"] = "reference-refusal"
                        request["refusal"] = refusal
                        write_jsonl(receipts, {**request, "time_ns": time.time_ns()}, lock)
                        response = typed_failure_message(
                            message.get("id"), refusal["error_type"], refusal["error"],
                            refusal["wrong_subject"]
                        )
                        write_jsonl(stream, {"direction": "proxy-to-client", "message": response}, lock)
                        sys.stdout.write(canonical_json(response) + "\n")
                        sys.stdout.flush()
                        continue
                    outgoing = copy.deepcopy(message)
                    outgoing["params"]["arguments"] = translated
                write_jsonl(
                    receipts,
                    {"event": "tool-request", "arm": args.arm, **request, "time_ns": time.time_ns()},
                    lock,
                )
            with pending_lock:
                if "id" in message:
                    pending[message["id"]] = request
            child.stdin.write(canonical_json(outgoing) + "\n")
            child.stdin.flush()
        stop_child()
        try:
            child.wait(timeout=10)
        except subprocess.TimeoutExpired:
            child.kill()
            child.wait(timeout=5)
        reader.join(timeout=5)
        return 0 if child.returncode in {0, -signal.SIGTERM} else child.returncode


if __name__ == "__main__":
    raise SystemExit(main())
