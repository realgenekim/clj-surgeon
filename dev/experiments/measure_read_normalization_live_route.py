#!/usr/bin/env python3
"""Measure current/candidate inspect_clojure JSON-RPC bytes and proxy tokens.

The harness launches two private stdio MCP processes. It never edits either
source checkout, registers an MCP server, reloads a shared server, or writes to
the measured workspace after capture begins.
"""

import argparse
import hashlib
import json
import math
import os
import select
import subprocess
import sys
import threading
import time
from pathlib import Path


CONTROL_HEAD = "b9db064a86c3919660a38f79ab5031dcf6d49f98"
CONTROL_TREE = "e5dd6183de87318bee8689a6129252c51d954e8a"
CANDIDATE_HEAD = "c55de2279826af5ed21c90981591479dd2e802b2"
CANDIDATE_TREE = "565f009f0ff25fdedbc2fba5ad9ba5f55783e023"
INSTALLED_REFERENCE = "19ab864889799b0028a5f7cb66c63b957ff7b973"
RELEVANT_SURFACE_FILES = [
    "src/clj_surgeon/mcp_inspect.clj",
    "src/clj_surgeon/mcp_inspect_tool.clj",
    "src/clj_surgeon/mcp_intent_contract.clj",
    "src/clj_surgeon/mcp_server.clj",
    "src/clj_surgeon/mcp_schema.clj",
]
TOKENIZER_PACKAGE = "tiktoken-0.9.0"
TOKENIZER_ENCODING = "o200k_base"
RESPONSE_TIMEOUT_SECONDS = 120
TIMING_KEYS = {
    "elapsed_ms",
    "execution_ms",
    "operation_elapsed_ms",
    "server_execution_ms",
    "wall_ms",
}


def json_bytes(value):
    return json.dumps(
        value, ensure_ascii=False, separators=(",", ":"), sort_keys=False
    ).encode("utf-8")


def sha256_bytes(value):
    return hashlib.sha256(value).hexdigest()


def sha256_file(path):
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for chunk in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def metric(payload, encoding):
    text = payload.decode("utf-8")
    return {
        "bytes": len(payload),
        "tokens": len(encoding.encode(text)),
        "sha256": sha256_bytes(payload),
    }


def delta(control, candidate):
    result = {}
    for field in ("bytes", "tokens"):
        baseline = control[field]
        treatment = candidate[field]
        saved = baseline - treatment
        result[field] = {
            "control": baseline,
            "candidate": treatment,
            "saved": saved,
            "saved_percent": (100.0 * saved / baseline if baseline else None),
        }
    return result


def git_value(worktree, *arguments):
    return subprocess.check_output(
        ["git", "-C", str(worktree), *arguments], text=True
    ).strip()


def assert_identity(worktree, expected_head, expected_tree):
    head = git_value(worktree, "rev-parse", "HEAD")
    tree = git_value(worktree, "rev-parse", "HEAD^{tree}")
    status = git_value(worktree, "status", "--porcelain=v1", "--untracked-files=all")
    if head != expected_head or tree != expected_tree or status:
        raise RuntimeError(
            f"identity gate failed: head={head} tree={tree} clean={not status}"
        )
    return {"head": head, "tree": tree, "clean": True}


def relevant_surface_matches_installed(control_worktree):
    completed = subprocess.run(
        [
            "git",
            "-C",
            str(control_worktree),
            "diff",
            "--quiet",
            INSTALLED_REFERENCE,
            CONTROL_HEAD,
            "--",
            *RELEVANT_SURFACE_FILES,
        ],
        check=False,
    )
    return completed.returncode == 0


def drop_timing(value):
    if isinstance(value, dict):
        return {
            key: drop_timing(item)
            for key, item in value.items()
            if key not in TIMING_KEYS
        }
    if isinstance(value, list):
        return [drop_timing(item) for item in value]
    return value


def structured_content(response):
    return drop_timing((response.get("result") or {}).get("structuredContent"))


def successful_tool_response(response):
    result = response.get("result")
    return isinstance(result, dict) and not result.get("isError", False)


def failed_tool_response(response):
    if "error" in response:
        return True
    result = response.get("result")
    return isinstance(result, dict) and result.get("isError", False)


def call_specs(workspace):
    root = str(workspace)

    def envelope(call_id, arguments):
        return {
            "jsonrpc": "2.0",
            "id": call_id,
            "method": "tools/call",
            "params": {"name": "inspect_clojure", "arguments": arguments},
        }

    explicit_single = {
        "workspace_root": root,
        "requests": [
            {
                "id": "subject",
                "operation": "forms",
                "file": "src/sample.clj",
                "forms": ["alpha"],
                "expect": {"forms": 1},
            }
        ],
        "expect": {"requests": 1, "files": 1},
    }
    implicit_single = {
        "workspace_root": root,
        "requests": [
            {
                "id": "subject",
                "file": "src/sample.clj",
                "forms": ["alpha"],
                "expect": {"forms": 1},
            }
        ],
        "expect": {"requests": 1, "files": 1},
    }
    explicit_multi = {
        "workspace_root": root,
        "requests": [
            {
                "id": "request-1",
                "operation": "forms",
                "file": "src/sample.clj",
                "forms": ["alpha"],
                "expect": {"forms": 1},
            },
            {
                "id": "request-2",
                "operation": "forms",
                "file": "src/sample.clj",
                "forms": ["beta"],
                "expect": {"forms": 1},
            },
        ],
        "expect": {"requests": 2, "files": 1},
    }
    omitted_multi = {
        "workspace_root": root,
        "requests": [
            {
                "operation": "forms",
                "file": "src/sample.clj",
                "forms": ["alpha"],
                "expect": {"forms": 1},
            },
            {
                "operation": "forms",
                "file": "src/sample.clj",
                "forms": ["beta"],
                "expect": {"forms": 1},
            },
        ],
        "expect": {"requests": 2, "files": 1},
    }
    mixed_ids = {
        "workspace_root": root,
        "requests": [
            {
                "id": "request-1",
                "operation": "forms",
                "file": "src/sample.clj",
                "forms": ["alpha"],
                "expect": {"forms": 1},
            },
            {
                "operation": "forms",
                "file": "src/sample.clj",
                "forms": ["beta"],
                "expect": {"forms": 1},
            },
        ],
        "expect": {"requests": 2, "files": 1},
    }
    return [
        ("explicit-single", envelope(101, explicit_single)),
        ("implicit-single", envelope(102, implicit_single)),
        ("explicit-id-multi", envelope(103, explicit_multi)),
        ("omitted-id-multi", envelope(104, omitted_multi)),
        ("mixed-request-ids", envelope(105, mixed_ids)),
    ]


class McpProcess:
    def __init__(self, worktree, workspace, arm_dir):
        self.worktree = worktree
        self.arm_dir = arm_dir
        self.command = [
            "clojure",
            "-J-Xms64m",
            "-J-Xmx512m",
            "-X:clj-surgeon/mcp-stdio",
            ":project-dir",
            json.dumps(str(workspace)),
            ":receipt-dir",
            json.dumps(str(arm_dir / "receipts")),
            ":telemetry",
            ":off",
            ":nrepl-port",
            ":none",
            ":port-file",
            json.dumps(str(arm_dir / "nrepl-port")),
            ":log-file",
            json.dumps(str(arm_dir / "mcp-server.log")),
        ]
        self.stderr_chunks = []
        self.process = subprocess.Popen(
            self.command,
            cwd=worktree,
            stdin=subprocess.PIPE,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
        )
        self.stderr_thread = threading.Thread(target=self._drain_stderr, daemon=True)
        self.stderr_thread.start()
        self.unsolicited = []

    def _drain_stderr(self):
        while True:
            chunk = self.process.stderr.read(8192)
            if not chunk:
                return
            self.stderr_chunks.append(chunk)

    def _write(self, payload):
        raw = json_bytes(payload)
        self.process.stdin.write(raw + b"\n")
        self.process.stdin.flush()
        return raw

    def notify(self, payload):
        return self._write(payload)

    def request(self, payload):
        raw_request = self._write(payload)
        deadline = time.monotonic() + RESPONSE_TIMEOUT_SECONDS
        while time.monotonic() < deadline:
            ready, _, _ = select.select(
                [self.process.stdout], [], [], max(0.0, deadline - time.monotonic())
            )
            if not ready:
                break
            raw_response = self.process.stdout.readline()
            if not raw_response:
                raise RuntimeError(
                    f"server exited before response id={payload.get('id')}: "
                    f"exit={self.process.poll()}"
                )
            stripped = raw_response.rstrip(b"\r\n")
            parsed = json.loads(stripped)
            if parsed.get("id") == payload.get("id"):
                return raw_request, stripped, parsed
            self.unsolicited.append(stripped)
        raise TimeoutError(f"MCP response timeout id={payload.get('id')}")

    def close(self):
        if self.process.stdin and not self.process.stdin.closed:
            self.process.stdin.close()
        self.process.terminate()
        try:
            self.process.wait(timeout=10)
        except subprocess.TimeoutExpired:
            self.process.kill()
            self.process.wait(timeout=5)
        self.stderr_thread.join(timeout=5)
        (self.arm_dir / "stderr.log").write_bytes(b"".join(self.stderr_chunks))
        if self.unsolicited:
            (self.arm_dir / "unsolicited.jsonl").write_bytes(
                b"\n".join(self.unsolicited) + b"\n"
            )
        return self.process.returncode


def capture_message(directory, stem, request_raw, response_raw, request, encoding):
    request_path = directory / f"{stem}.request.json"
    response_path = directory / f"{stem}.response.json"
    request_path.write_bytes(request_raw)
    response_path.write_bytes(response_raw)
    arguments_raw = json_bytes(request.get("params", {}).get("arguments", {}))
    return {
        "request": metric(request_raw, encoding),
        "request_wire_bytes_with_newline": len(request_raw) + 1,
        "arguments": metric(arguments_raw, encoding),
        "response": metric(response_raw, encoding),
        "response_wire_bytes_with_newline": len(response_raw) + 1,
    }


def capture_arm(label, worktree, workspace, result_dir, encoding):
    arm_dir = result_dir / label
    arm_dir.mkdir()
    server = McpProcess(worktree, workspace, arm_dir)
    responses = {}
    metrics = {}
    try:
        initialize = {
            "jsonrpc": "2.0",
            "id": 1,
            "method": "initialize",
            "params": {
                "protocolVersion": "2024-11-05",
                "capabilities": {},
                "clientInfo": {
                    "name": "clj-surgeon-live-route-measurement",
                    "version": "1",
                },
            },
        }
        request_raw, response_raw, response = server.request(initialize)
        capture_message(
            arm_dir, "00-initialize", request_raw, response_raw, initialize, encoding
        )
        responses["initialize"] = response
        initialized = {
            "jsonrpc": "2.0",
            "method": "notifications/initialized",
        }
        (arm_dir / "01-initialized.request.json").write_bytes(server.notify(initialized))
        tools_list = {"jsonrpc": "2.0", "id": 2, "method": "tools/list", "params": {}}
        request_raw, response_raw, response = server.request(tools_list)
        metrics["tools-list"] = capture_message(
            arm_dir, "02-tools-list", request_raw, response_raw, tools_list, encoding
        )
        responses["tools-list"] = response

        tools = (response.get("result") or {}).get("tools") or []
        inspect_tool = next((tool for tool in tools if tool.get("name") == "inspect_clojure"), None)
        if inspect_tool is None:
            raise RuntimeError("inspect_clojure missing from tools/list")
        inspect_raw = json_bytes(inspect_tool)
        schema_raw = json_bytes(inspect_tool["inputSchema"])
        (arm_dir / "inspect-tool.json").write_bytes(inspect_raw)
        (arm_dir / "inspect-input-schema.json").write_bytes(schema_raw)
        metrics["inspect-tool-definition"] = metric(inspect_raw, encoding)
        metrics["inspect-input-schema"] = metric(schema_raw, encoding)

        for index, (name, request) in enumerate(call_specs(workspace), start=10):
            request_raw, response_raw, response = server.request(request)
            metrics[name] = capture_message(
                arm_dir,
                f"{index:02d}-{name}",
                request_raw,
                response_raw,
                request,
                encoding,
            )
            responses[name] = response
    finally:
        exit_code = server.close()
        (arm_dir / "command.json").write_bytes(json_bytes(server.command) + b"\n")
    return {
        "exit_code_after_termination": exit_code,
        "metrics": metrics,
        "responses": responses,
        "command": server.command,
    }


def semantic_verdict(control, candidate):
    control_responses = control["responses"]
    candidate_responses = candidate["responses"]
    mixed_text = json.dumps(candidate_responses["mixed-request-ids"], sort_keys=True)
    verdict = {
        "control_explicit_single_success": successful_tool_response(
            control_responses["explicit-single"]
        ),
        "candidate_explicit_single_success": successful_tool_response(
            candidate_responses["explicit-single"]
        ),
        "candidate_implicit_single_success": successful_tool_response(
            candidate_responses["implicit-single"]
        ),
        "control_explicit_multi_success": successful_tool_response(
            control_responses["explicit-id-multi"]
        ),
        "candidate_explicit_multi_success": successful_tool_response(
            candidate_responses["explicit-id-multi"]
        ),
        "candidate_omitted_multi_success": successful_tool_response(
            candidate_responses["omitted-id-multi"]
        ),
        "control_implicit_single_refuses": failed_tool_response(
            control_responses["implicit-single"]
        ),
        "control_omitted_multi_refuses": failed_tool_response(
            control_responses["omitted-id-multi"]
        ),
        "candidate_mixed_ids_refuses": failed_tool_response(
            candidate_responses["mixed-request-ids"]
        ),
        "candidate_mixed_ids_typed": "mixed-request-ids" in mixed_text,
        "candidate_mixed_ids_source_unchanged": '"source_unchanged": true' in mixed_text,
        "candidate_mixed_ids_read_not_started": '"read_started": false' in mixed_text,
        "explicit_single_semantic_equal": (
            structured_content(control_responses["explicit-single"])
            == structured_content(candidate_responses["explicit-single"])
        ),
        "implicit_equals_current_explicit": (
            structured_content(control_responses["explicit-single"])
            == structured_content(candidate_responses["implicit-single"])
        ),
        "explicit_multi_semantic_equal": (
            structured_content(control_responses["explicit-id-multi"])
            == structured_content(candidate_responses["explicit-id-multi"])
        ),
        "omitted_equals_current_explicit_multi": (
            structured_content(control_responses["explicit-id-multi"])
            == structured_content(candidate_responses["omitted-id-multi"])
        ),
    }
    verdict["all"] = all(verdict.values())
    return verdict


def comparisons(control, candidate):
    control_metrics = control["metrics"]
    candidate_metrics = candidate["metrics"]

    def pair(control_name, candidate_name):
        return {
            "request": delta(
                control_metrics[control_name]["request"],
                candidate_metrics[candidate_name]["request"],
            ),
            "arguments": delta(
                control_metrics[control_name]["arguments"],
                candidate_metrics[candidate_name]["arguments"],
            ),
            "response": delta(
                control_metrics[control_name]["response"],
                candidate_metrics[candidate_name]["response"],
            ),
        }

    return {
        "tools_list_full_response": delta(
            control_metrics["tools-list"]["response"],
            candidate_metrics["tools-list"]["response"],
        ),
        "inspect_tool_definition": delta(
            control_metrics["inspect-tool-definition"],
            candidate_metrics["inspect-tool-definition"],
        ),
        "inspect_input_schema": delta(
            control_metrics["inspect-input-schema"],
            candidate_metrics["inspect-input-schema"],
        ),
        "same_explicit_single": pair("explicit-single", "explicit-single"),
        "operationless_vs_current_explicit": pair("explicit-single", "implicit-single"),
        "same_explicit_multi": pair("explicit-id-multi", "explicit-id-multi"),
        "omitted_ids_vs_current_explicit": pair("explicit-id-multi", "omitted-id-multi"),
        "same_mixed_ids": pair("mixed-request-ids", "mixed-request-ids"),
    }


def artifact_manifest(result_dir):
    entries = []
    for path in sorted(result_dir.rglob("*")):
        if path.is_file() and path.name != "MANIFEST.sha256":
            entries.append(f"{sha256_file(path)}  {path.relative_to(result_dir)}")
    rendered = "\n".join(entries) + "\n"
    (result_dir / "MANIFEST.sha256").write_text(rendered)
    return sha256_bytes(rendered.encode("utf-8"))


class FakeEncoding:
    def encode(self, text):
        return list(text)


def self_test():
    encoded = json_bytes({"b": 2, "a": "λ"})
    assert encoded == b'{"b":2,"a":"\xce\xbb"}'
    measured = metric(encoded, FakeEncoding())
    assert measured["bytes"] == len(encoded)
    assert measured["tokens"] == len(encoded.decode("utf-8"))
    difference = delta({"bytes": 100, "tokens": 20}, {"bytes": 75, "tokens": 15})
    assert difference["bytes"]["saved"] == 25
    assert difference["tokens"]["saved_percent"] == 25.0
    timing = {"a": 1, "elapsed_ms": 2, "nested": [{"wall_ms": 3, "b": 4}]}
    assert drop_timing(timing) == {"a": 1, "nested": [{"b": 4}]}
    with_workspace = call_specs(Path("/tmp/workspace"))
    assert [name for name, _ in with_workspace] == [
        "explicit-single",
        "implicit-single",
        "explicit-id-multi",
        "omitted-id-multi",
        "mixed-request-ids",
    ]
    print("measure_read_normalization_live_route self-test passed")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--control-worktree", type=Path)
    parser.add_argument("--candidate-worktree", type=Path)
    parser.add_argument("--result-dir", type=Path)
    parser.add_argument("--self-test", action="store_true")
    args = parser.parse_args()
    if args.self_test:
        self_test()
        return
    if not args.control_worktree or not args.candidate_worktree or not args.result_dir:
        parser.error("control, candidate, and result-dir are required")
    if args.result_dir.exists():
        raise RuntimeError("result directory must not exist")

    try:
        import tiktoken
    except ImportError as error:
        raise RuntimeError("tiktoken 0.9.0 is required") from error
    if tiktoken.__version__ != "0.9.0":
        raise RuntimeError(f"wrong tiktoken version: {tiktoken.__version__}")
    encoding = tiktoken.get_encoding(TOKENIZER_ENCODING)

    control_identity = assert_identity(
        args.control_worktree, CONTROL_HEAD, CONTROL_TREE
    )
    candidate_identity = assert_identity(
        args.candidate_worktree, CANDIDATE_HEAD, CANDIDATE_TREE
    )
    installed_surface_equal = relevant_surface_matches_installed(args.control_worktree)
    if not installed_surface_equal:
        raise RuntimeError("control relevant surface differs from installed reference")

    args.result_dir.mkdir(parents=True)
    workspace = args.result_dir / "workspace"
    (workspace / "src").mkdir(parents=True)
    fixture = workspace / "src" / "sample.clj"
    fixture.write_text("(ns sample)\n\n(def alpha 1)\n\n(def beta 2)\n")
    before_hash = sha256_file(fixture)

    control = capture_arm(
        "control", args.control_worktree, workspace, args.result_dir, encoding
    )
    candidate = capture_arm(
        "candidate", args.candidate_worktree, workspace, args.result_dir, encoding
    )
    after_hash = sha256_file(fixture)
    semantic = semantic_verdict(control, candidate)
    source_unchanged = before_hash == after_hash
    if not semantic["all"] or not source_unchanged:
        raise RuntimeError(
            f"validity gate failed: semantic={semantic} source_unchanged={source_unchanged}"
        )

    report = {
        "schema": "clj-surgeon.read-normalization-live-route-measurement.v1",
        "identity": {
            "control": control_identity,
            "candidate": candidate_identity,
            "installed_reference": INSTALLED_REFERENCE,
            "control_relevant_surface_equals_installed": installed_surface_equal,
        },
        "tokenizer": {
            "package": f"tiktoken-{tiktoken.__version__}",
            "encoding": encoding.name,
            "role": "local stable proxy, not provider billing authority",
        },
        "route": {
            "transport": "MCP stdio JSON-RPC 2.0",
            "protocol_version": "2024-11-05",
            "server_processes": 2,
            "shared_runtime_touched": False,
            "registered_mcp_config_touched": False,
            "telemetry": "off",
            "nrepl": "none",
            "route_adherent": True,
        },
        "validity": {
            "environment_valid": True,
            "semantic_correct": semantic,
            "source_hash_before": before_hash,
            "source_hash_after": after_hash,
            "source_unchanged": source_unchanged,
        },
        "arms": {
            "control": {
                "metrics": control["metrics"],
                "exit_code_after_termination": control["exit_code_after_termination"],
            },
            "candidate": {
                "metrics": candidate["metrics"],
                "exit_code_after_termination": candidate["exit_code_after_termination"],
            },
        },
        "comparisons": comparisons(control, candidate),
    }
    report_path = args.result_dir / "report.json"
    report_path.write_bytes(json_bytes(report) + b"\n")
    manifest_sha = artifact_manifest(args.result_dir)
    print(
        json.dumps(
            {
                "ok": True,
                "report": str(report_path),
                "report_sha256": sha256_file(report_path),
                "manifest_sha256": manifest_sha,
            },
            separators=(",", ":"),
            sort_keys=True,
        )
    )


if __name__ == "__main__":
    main()
