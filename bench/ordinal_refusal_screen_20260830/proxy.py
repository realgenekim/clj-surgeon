#!/usr/bin/env python3
"""Isolated experiment-only MCP proxy for ordinal refusal recovery.

This is deliberately not a product server. It simulates one measured refusal
family and, only in treatment, exposes a confirmation-hole tool. Every receipt
states that the surface is experiment-only and outside the product contract.
"""

from __future__ import annotations

import hashlib
import json
import os
import re
import sys
import time
from pathlib import Path
from typing import Any


ARMS = {"C", "T"}
TARGET_FILE = "src/sample/views.clj"
TARGET_OWNER = "render-dashboard"
NEAR_MISS = "render-dashbord"
FIND = ":status :pending"
REPLACE = ":status :ready"
OWNER_PATTERN = re.compile(r"^\(defn ([^ ]+) \[\] .*\)$", re.MULTILINE)
EXPERIMENT_NOTICE = (
    "EXPERIMENT SURFACE ONLY — not product code, not product authority, and "
    "not compliant evidence that product refusals may carry executable payloads."
)


def canonical_bytes(value: Any) -> bytes:
    return json.dumps(
        value, sort_keys=True, separators=(",", ":"), ensure_ascii=False
    ).encode("utf-8")


def digest(value: Any) -> str:
    return hashlib.sha256(canonical_bytes(value)).hexdigest()


def file_sha(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def emit(message: dict[str, Any]) -> None:
    sys.stdout.write(json.dumps(message, separators=(",", ":"), ensure_ascii=False) + "\n")
    sys.stdout.flush()


def error(request_id: Any, code: int, message: str) -> dict[str, Any]:
    return {
        "jsonrpc": "2.0",
        "id": request_id,
        "error": {"code": code, "message": message},
    }


def tool_result(structured: dict[str, Any], text: str, *, failed: bool = False) -> dict[str, Any]:
    return {
        "content": [{"type": "text", "text": text}],
        "structuredContent": structured,
        "isError": failed,
    }


def tool_catalog() -> dict[str, Any]:
    apply_schema = {
        "type": "object",
        "additionalProperties": False,
        "required": ["changes", "mode"],
        "properties": {
            "changes": {
                "type": "array",
                "minItems": 1,
                "maxItems": 1,
                "items": {
                    "type": "object",
                    "additionalProperties": False,
                    "required": ["files", "forms", "find", "replace", "expect"],
                    "properties": {
                        "files": {"type": "array", "items": {"type": "string"}},
                        "forms": {"type": "array", "items": {"type": "string"}},
                        "find": {"type": "string"},
                        "replace": {"type": "string"},
                        "expect": {
                            "type": "object",
                            "required": ["matches"],
                            "properties": {"matches": {"type": "integer"}},
                        },
                    },
                },
            },
            "mode": {"type": "string", "enum": ["execute"]},
        },
    }
    return {
        "tools": [
            {
                "name": "apply_clojure_changes",
                "description": (
                    "Experiment-only guarded top-level owner replacement. A missing owner "
                    "returns batch-form-selection-failed; a valid owner changes only that owner."
                ),
                "inputSchema": apply_schema,
            },
            {
                "name": "inspect_clojure",
                "description": (
                    "Experiment-only read of the complete synthetic namespace, including all "
                    "top-level owner names and source."
                ),
                "inputSchema": {
                    "type": "object",
                    "additionalProperties": True,
                    "properties": {
                        "file": {"type": "string"},
                        "requests": {"type": "array"},
                    },
                },
            },
            {
                "name": "experiment_confirm_prepared_request",
                "description": (
                    "EXPERIMENT ONLY. Use only after a refusal explicitly supplies a matching "
                    "non-executable template. Fill its one candidate_index hole; the proxy "
                    "revalidates refusal identity and source hash before applying the frozen edit."
                ),
                "inputSchema": {
                    "type": "object",
                    "additionalProperties": False,
                    "required": ["refusal_id", "candidate_index"],
                    "properties": {
                        "refusal_id": {"type": "string"},
                        "candidate_index": {"type": "integer", "minimum": 1},
                    },
                },
            },
        ]
    }


class Screen:
    def __init__(self, arm: str, workspace: Path, log_path: Path) -> None:
        if arm not in ARMS:
            raise ValueError(f"invalid arm: {arm!r}")
        self.arm = arm
        self.workspace = workspace.resolve()
        self.log_path = log_path.resolve()
        self.source_path = self.workspace / TARGET_FILE
        if not self.source_path.is_file():
            raise ValueError(f"fixture missing: {self.source_path}")
        self.initial_sha256 = file_sha(self.source_path)
        self.owners = OWNER_PATTERN.findall(self.source_path.read_text(encoding="utf-8"))
        if len(self.owners) != 27 or self.owners[18] != TARGET_OWNER:
            raise ValueError("frozen owner vocabulary or target ordinal drift")
        self.refusal_id = digest(
            {
                "version": 1,
                "file_sha256": self.initial_sha256,
                "near_miss": NEAR_MISS,
                "find": FIND,
                "replace": REPLACE,
            }
        )
        self.refused = False
        self.mutated_owners: list[str] = []
        self.log(
            "proxy_ready",
            arm=arm,
            experiment_only=True,
            product_contract=False,
            experiment_notice=EXPERIMENT_NOTICE,
            owner_count=len(self.owners),
            target_ordinal=19,
            initial_sha256=self.initial_sha256,
            offered_tools=[tool["name"] for tool in tool_catalog()["tools"]],
            tool_catalog_sha256=digest(tool_catalog()),
        )

    def log(self, event: str, **data: Any) -> None:
        self.log_path.parent.mkdir(parents=True, exist_ok=True)
        row = {"ts_ns": time.time_ns(), "event": event, **data}
        with self.log_path.open("a", encoding="utf-8") as handle:
            handle.write(json.dumps(row, sort_keys=True, ensure_ascii=False) + "\n")
            handle.flush()
            os.fsync(handle.fileno())

    def validate_workspace(self, arguments: Any) -> dict[str, Any]:
        if not isinstance(arguments, dict):
            raise ValueError("arguments must be an object")
        explicit = arguments.get("workspace_root")
        if explicit is not None and Path(explicit).resolve() != self.workspace:
            raise ValueError("workspace_root must equal the private fixture workspace")
        return arguments

    def refusal(self) -> dict[str, Any]:
        common = {
            "ok": False,
            "operation": "apply_clojure_changes",
            "error_type": "batch-form-selection-failed",
            "reason": "batch-form-selection-failed",
            "requested_form": NEAR_MISS,
            "available_form_count": 27,
            "source_unchanged": True,
            "mutation_attempted": False,
            "next_action": "correct_request",
            "experiment_only": True,
            "product_contract": False,
            "experiment_notice": EXPERIMENT_NOTICE,
            "write_authority": False,
            "refusal_id": self.refusal_id,
            "snapshot_sha256": self.initial_sha256,
        }
        if self.arm == "C":
            structured = {
                **common,
                "form_candidates": self.owners[:10],
                "returned_form_count": 10,
                "omitted_form_count": 17,
                "truncated": True,
            }
            shown = "\n".join(f"- {name}" for name in self.owners[:10])
            text = (
                "apply_clojure_changes refused · batch-form-selection-failed\n"
                "Available forms (first 10 of 27; truncated):\n"
                f"{shown}\nSource unchanged. Correct the request.\n{EXPERIMENT_NOTICE}"
            )
        else:
            numbered = [
                {"index": index, "owner": owner}
                for index, owner in enumerate(self.owners, start=1)
            ]
            template = {
                "experiment_only": True,
                "product_contract": False,
                "executable": False,
                "authority": False,
                "write_authority": False,
                "tool_after_caller_confirmation": "experiment_confirm_prepared_request",
                "arguments": {
                    "refusal_id": self.refusal_id,
                    "candidate_index": "<CALLER_CONFIRMATION_HOLE>",
                },
                "confirmation_holes": ["arguments.candidate_index"],
                "frozen_effect": {
                    "file": TARGET_FILE,
                    "find": FIND,
                    "replace": REPLACE,
                    "expect_matches": 1,
                    "snapshot_sha256": self.initial_sha256,
                },
            }
            structured = {
                **common,
                "form_candidates": numbered,
                "returned_form_count": 27,
                "omitted_form_count": 0,
                "truncated": False,
                "prepared_corrected_request": template,
            }
            shown = "\n".join(f"{row['index']}. {row['owner']}" for row in numbered)
            text = (
                "apply_clojure_changes refused · batch-form-selection-failed\n"
                "Complete numbered owner vocabulary:\n"
                f"{shown}\n"
                "One non-executable experiment template is available in structured content; "
                "it requires the caller to fill candidate_index.\n"
                f"Source unchanged.\n{EXPERIMENT_NOTICE}"
            )
        self.refused = True
        self.log(
            "controlled_refusal",
            arm=self.arm,
            refusal_id=self.refusal_id,
            emitted=structured,
            emitted_sha256=digest(structured),
        )
        return tool_result(structured, text, failed=True)

    def mutate(self, owner: str, route: str) -> dict[str, Any]:
        if owner not in self.owners:
            structured = {
                "ok": False,
                "operation": route,
                "error_type": "batch-form-selection-failed",
                "requested_form": owner,
                "source_unchanged": True,
                "experiment_only": True,
                "product_contract": False,
                "experiment_notice": EXPERIMENT_NOTICE,
            }
            self.log("retry_refusal", owner=owner, route=route)
            return tool_result(structured, f"Unknown owner {owner}; source unchanged.", failed=True)

        before = self.source_path.read_text(encoding="utf-8")
        line_pattern = re.compile(
            rf"^(\(defn {re.escape(owner)} \[\] .*?){re.escape(FIND)}(.*\))$",
            re.MULTILINE,
        )
        after, count = line_pattern.subn(rf"\1{REPLACE}\2", before)
        if count != 1:
            structured = {
                "ok": False,
                "operation": route,
                "error_type": "expect-count-mismatch",
                "expected_count": 1,
                "actual_count": count,
                "source_unchanged": True,
                "experiment_only": True,
                "product_contract": False,
                "experiment_notice": EXPERIMENT_NOTICE,
            }
            self.log("retry_refusal", owner=owner, route=route, match_count=count)
            return tool_result(structured, "Guard refused; source unchanged.", failed=True)
        self.source_path.write_text(after, encoding="utf-8")
        self.mutated_owners.append(owner)
        final_sha = file_sha(self.source_path)
        structured = {
            "ok": True,
            "operation": route,
            "committed": True,
            "verification_complete": True,
            "next_action": "none",
            "mutated_owner": owner,
            "read_back_sha256": final_sha,
            "experiment_only": True,
            "product_contract": False,
            "experiment_notice": EXPERIMENT_NOTICE,
        }
        self.log(
            "mutation_committed",
            owner=owner,
            route=route,
            final_sha256=final_sha,
            wrong_subject=owner != TARGET_OWNER,
        )
        return tool_result(
            structured,
            f"Experiment mutation committed for {owner}; read-back verified.\n{EXPERIMENT_NOTICE}",
        )

    def apply(self, arguments: dict[str, Any]) -> dict[str, Any]:
        changes = arguments.get("changes")
        mode = arguments.get("mode")
        if not isinstance(changes, list) or len(changes) != 1 or mode != "execute":
            raise ValueError("expected one execute-mode change")
        change = changes[0]
        if not isinstance(change, dict):
            raise ValueError("change must be an object")
        files = change.get("files")
        forms = change.get("forms")
        if files != [TARGET_FILE] or not isinstance(forms, list) or len(forms) != 1:
            raise ValueError("change must name the frozen file and one owner")
        if change.get("find") != FIND or change.get("replace") != REPLACE:
            raise ValueError("change does not match the frozen effect")
        if change.get("expect") != {"matches": 1}:
            raise ValueError("change must retain the frozen exact guard")
        owner = forms[0]
        if owner == NEAR_MISS and not self.refused:
            return self.refusal()
        return self.mutate(owner, "apply_clojure_changes")

    def inspect(self, arguments: dict[str, Any]) -> dict[str, Any]:
        source = self.source_path.read_text(encoding="utf-8")
        structured = {
            "ok": True,
            "operation": "inspect_clojure",
            "read_complete": True,
            "next_action": "none",
            "file": TARGET_FILE,
            "file_sha256": file_sha(self.source_path),
            "owner_count": len(self.owners),
            "owners": self.owners,
            "source": source,
            "experiment_only": True,
            "product_contract": False,
            "experiment_notice": EXPERIMENT_NOTICE,
        }
        self.log("recovery_read", arguments=arguments, result_sha256=digest(structured))
        return tool_result(
            structured,
            "Complete 27-owner synthetic namespace read returned.\n" + source,
        )

    def confirm(self, arguments: dict[str, Any]) -> dict[str, Any]:
        if self.arm != "T" or not self.refused:
            raise ValueError("no treatment template is active")
        if arguments.get("refusal_id") != self.refusal_id:
            raise ValueError("refusal_id does not match the active frozen refusal")
        index = arguments.get("candidate_index")
        if not isinstance(index, int) or isinstance(index, bool) or not 1 <= index <= 27:
            raise ValueError("candidate_index must be an integer from 1 through 27")
        if file_sha(self.source_path) != self.initial_sha256:
            raise ValueError("frozen source guard changed before confirmation")
        owner = self.owners[index - 1]
        self.log("confirmation_hole_filled", candidate_index=index, owner=owner)
        return self.mutate(owner, "experiment_confirm_prepared_request")

    def call(self, name: str, arguments: Any) -> dict[str, Any]:
        arguments = self.validate_workspace(arguments)
        self.log("client_tool_call", name=name, arguments=arguments, arguments_sha256=digest(arguments))
        if name == "apply_clojure_changes":
            result = self.apply(arguments)
        elif name == "inspect_clojure":
            result = self.inspect(arguments)
        elif name == "experiment_confirm_prepared_request":
            result = self.confirm(arguments)
        else:
            raise ValueError(f"tool is not offered: {name}")
        self.log(
            "client_tool_result",
            name=name,
            result_sha256=digest(result),
            failed=bool(result.get("isError")),
        )
        return result


def main() -> None:
    screen = Screen(
        os.environ["ORDINAL_SCREEN_ARM"],
        Path(os.environ["ORDINAL_SCREEN_WORKSPACE"]),
        Path(os.environ["ORDINAL_SCREEN_LOG"]),
    )
    catalog = tool_catalog()
    for raw_line in sys.stdin:
        if not raw_line.strip():
            continue
        try:
            message = json.loads(raw_line)
        except json.JSONDecodeError as exc:
            screen.log("invalid_json", error=str(exc))
            continue
        method = message.get("method")
        request_id = message.get("id")
        if request_id is None:
            screen.log("client_notification", method=method)
            continue
        try:
            if method == "initialize":
                result = {
                    "protocolVersion": message.get("params", {}).get(
                        "protocolVersion", "2025-03-26"
                    ),
                    "capabilities": {"tools": {"listChanged": False}},
                    "serverInfo": {
                        "name": "ordinal-refusal-screen",
                        "version": "experiment-only-v1",
                    },
                    "instructions": EXPERIMENT_NOTICE,
                }
            elif method == "tools/list":
                result = catalog
            elif method == "tools/call":
                params = message.get("params", {})
                result = screen.call(params.get("name"), params.get("arguments", {}))
            elif method == "ping":
                result = {}
            else:
                emit(error(request_id, -32601, f"method not found: {method}"))
                continue
            emit({"jsonrpc": "2.0", "id": request_id, "result": result})
        except Exception as exc:
            screen.log("request_error", request_id=request_id, method=method, error=str(exc))
            emit(error(request_id, -32000, str(exc)))


if __name__ == "__main__":
    main()
