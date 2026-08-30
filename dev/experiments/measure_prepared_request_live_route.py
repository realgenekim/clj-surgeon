#!/usr/bin/env python3
"""Measure prepared-request MCP stdio JSON-RPC wire cost without a model."""

import argparse
import hashlib
import json
import select
import subprocess
import threading
import time
from pathlib import Path


CONTROL_HEAD = "c55de2279826af5ed21c90981591479dd2e802b2"
CONTROL_TREE = "565f009f0ff25fdedbc2fba5ad9ba5f55783e023"
CANDIDATE_HEAD = "b445a8c3595d70f6f05b6edccb9b1a924539a195"
CANDIDATE_TREE = "b1e21af8073e66283f82f4036583bfe2971c4b0a"
TOKENIZER_PACKAGE = "tiktoken-0.9.0"
TOKENIZER_ENCODING = "o200k_base"
RESPONSE_TIMEOUT_SECONDS = 120
TIMING_KEYS = {
    "elapsed_ms",
    "execution_ms",
    "inspection_elapsed_ms",
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
    return {
        "bytes": len(payload),
        "tokens": len(encoding.encode(payload.decode("utf-8"))),
        "sha256": sha256_bytes(payload),
    }


def growth(control, candidate):
    result = {}
    for field in ("bytes", "tokens"):
        baseline = control[field]
        treatment = candidate[field]
        added = treatment - baseline
        result[field] = {
            "control": baseline,
            "candidate": treatment,
            "added": added,
            "added_percent": (100.0 * added / baseline if baseline else None),
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
    result = response.get("result") or {}
    return result.get("structuredContent") or result.get("structured_content")


def prepared_request(response):
    structured = structured_content(response) or {}
    return structured.get("prepared_request")


def without_prepared(response):
    structured = drop_timing(structured_content(response))
    if isinstance(structured, dict):
        return {key: value for key, value in structured.items() if key != "prepared_request"}
    return structured


def successful_tool_response(response):
    result = response.get("result")
    return isinstance(result, dict) and not result.get("isError", False)


def call_specs(workspace):
    root = str(workspace)

    def envelope(call_id, owners):
        return {
            "jsonrpc": "2.0",
            "id": call_id,
            "method": "tools/call",
            "params": {
                "name": "inspect_clojure",
                "arguments": {
                    "workspace_root": root,
                    "requests": [
                        {
                            "id": "subject",
                            "operation": "forms",
                            "file": "src/sample.clj",
                            "forms": owners,
                            "expect": {"forms": len(owners)},
                        }
                    ],
                    "expect": {"requests": 1, "files": 1},
                },
            },
        }

    owners = ["alpha", "beta", "gamma", "delta", "epsilon", "zeta", "eta"]
    return [
        ("eligible-one", envelope(101, owners[:1])),
        ("eligible-six", envelope(102, owners[:6])),
        ("ineligible-seven", envelope(103, owners)),
    ]


class McpProcess:
    def __init__(self, worktree, workspace, arm_dir):
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
    (directory / f"{stem}.request.json").write_bytes(request_raw)
    (directory / f"{stem}.response.json").write_bytes(response_raw)
    arguments_raw = json_bytes(request.get("params", {}).get("arguments", {}))
    return {
        "request": metric(request_raw, encoding),
        "arguments": metric(arguments_raw, encoding),
        "response": metric(response_raw, encoding),
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
                    "name": "prepared-request-live-route-measurement",
                    "version": "1",
                },
            },
        }
        request_raw, response_raw, response = server.request(initialize)
        capture_message(
            arm_dir, "00-initialize", request_raw, response_raw, initialize, encoding
        )
        initialized = {"jsonrpc": "2.0", "method": "notifications/initialized"}
        (arm_dir / "01-initialized.request.json").write_bytes(server.notify(initialized))
        tools_list = {"jsonrpc": "2.0", "id": 2, "method": "tools/list", "params": {}}
        request_raw, response_raw, response = server.request(tools_list)
        metrics["tools-list"] = capture_message(
            arm_dir, "02-tools-list", request_raw, response_raw, tools_list, encoding
        )
        tools = (response.get("result") or {}).get("tools") or []
        inspect_tool = next(
            (tool for tool in tools if tool.get("name") == "inspect_clojure"), None
        )
        if inspect_tool is None:
            raise RuntimeError("inspect_clojure missing from tools/list")
        for name, value in (
            ("inspect-tool", inspect_tool),
            ("inspect-input-schema", inspect_tool["inputSchema"]),
            ("inspect-output-schema", inspect_tool["outputSchema"]),
        ):
            raw = json_bytes(value)
            (arm_dir / f"{name}.json").write_bytes(raw)
            metrics[name] = metric(raw, encoding)
        for index, (name, request) in enumerate(call_specs(workspace), start=10):
            request_raw, response_raw, response = server.request(request)
            metrics[name] = capture_message(
                arm_dir, f"{index:02d}-{name}", request_raw, response_raw, request, encoding
            )
            responses[name] = response
    finally:
        exit_code = server.close()
        (arm_dir / "command.json").write_bytes(json_bytes(server.command) + b"\n")
    return {"exit_code": exit_code, "metrics": metrics, "responses": responses}


def capture_paid_path(worktree, workspace, result_dir, encoding):
    arm_dir = result_dir / "candidate-paid"
    arm_dir.mkdir()
    server = McpProcess(worktree, workspace, arm_dir)
    metrics = {}
    try:
        initialize = {
            "jsonrpc": "2.0",
            "id": 201,
            "method": "initialize",
            "params": {
                "protocolVersion": "2024-11-05",
                "capabilities": {},
                "clientInfo": {
                    "name": "prepared-request-paid-path-measurement",
                    "version": "1",
                },
            },
        }
        server.request(initialize)
        server.notify({"jsonrpc": "2.0", "method": "notifications/initialized"})
        inspect_request = call_specs(workspace)[0][1]
        inspect_request["id"] = 202
        request_raw, response_raw, inspect_response = server.request(inspect_request)
        metrics["inspect"] = capture_message(
            arm_dir,
            "20-paid-inspect",
            request_raw,
            response_raw,
            inspect_request,
            encoding,
        )
        prepared = prepared_request(inspect_response)
        if prepared is None:
            raise RuntimeError("paid path received no prepared request")
        filled_arguments = json.loads(json.dumps(prepared["arguments"]))
        filled_arguments["edits"][0]["to"] = "(def alpha 42)"
        scratch_arguments = {
            "edits": [
                {
                    "file": "src/sample.clj",
                    "from": "(def alpha 1)",
                    "matches": 1,
                    "to": "(def alpha 42)",
                    "within": {"form": "alpha"},
                }
            ],
            "workspace_root": str(workspace),
        }
        filled_raw = json_bytes(filled_arguments)
        scratch_raw = json_bytes(scratch_arguments)
        (arm_dir / "21-filled.arguments.json").write_bytes(filled_raw)
        (arm_dir / "22-from-scratch.arguments.json").write_bytes(scratch_raw)
        metrics["filled-arguments"] = metric(filled_raw, encoding)
        metrics["from-scratch-arguments"] = metric(scratch_raw, encoding)
        edit_request = {
            "jsonrpc": "2.0",
            "id": 203,
            "method": "tools/call",
            "params": {"name": "edit_clojure", "arguments": filled_arguments},
        }
        request_raw, response_raw, edit_response = server.request(edit_request)
        metrics["submitted-edit"] = capture_message(
            arm_dir,
            "23-submitted-edit",
            request_raw,
            response_raw,
            edit_request,
            encoding,
        )
    finally:
        exit_code = server.close()
        (arm_dir / "command.json").write_bytes(json_bytes(server.command) + b"\n")
    structured = structured_content(edit_response) or {}
    return {
        "exit_code": exit_code,
        "metrics": metrics,
        "arguments_semantically_equal": filled_arguments == scratch_arguments,
        "edit_success": successful_tool_response(edit_response),
        "committed": structured.get("committed") is True,
        "verification_complete": structured.get("verification_complete") is True,
    }


def semantic_verdict(control, candidate):
    names = ["eligible-one", "eligible-six", "ineligible-seven"]
    verdict = {
        "all_calls_success": all(
            successful_tool_response(arm["responses"][name])
            for arm in (control, candidate)
            for name in names
        ),
        "control_has_no_descriptors": all(
            prepared_request(control["responses"][name]) is None for name in names
        ),
        "candidate_one_has_one_edit": len(
            prepared_request(candidate["responses"]["eligible-one"])["arguments"]["edits"]
        )
        == 1,
        "candidate_six_has_six_edits": len(
            prepared_request(candidate["responses"]["eligible-six"])["arguments"]["edits"]
        )
        == 6,
        "candidate_seven_has_no_descriptor": prepared_request(
            candidate["responses"]["ineligible-seven"]
        )
        is None,
        "ordinary_structured_results_equal": all(
            without_prepared(control["responses"][name])
            == without_prepared(candidate["responses"][name])
            for name in names
        ),
    }
    verdict["all"] = all(verdict.values())
    return verdict


def comparisons(control, candidate, paid):
    pairs = {}
    for name in ("tools-list", "inspect-tool", "inspect-input-schema", "inspect-output-schema"):
        control_metric = (
            control["metrics"][name]["response"]
            if name == "tools-list"
            else control["metrics"][name]
        )
        candidate_metric = (
            candidate["metrics"][name]["response"]
            if name == "tools-list"
            else candidate["metrics"][name]
        )
        pairs[name] = growth(control_metric, candidate_metric)
    for name in ("eligible-one", "eligible-six", "ineligible-seven"):
        pairs[name] = {
            "request": growth(
                control["metrics"][name]["request"], candidate["metrics"][name]["request"]
            ),
            "arguments": growth(
                control["metrics"][name]["arguments"],
                candidate["metrics"][name]["arguments"],
            ),
            "response": growth(
                control["metrics"][name]["response"],
                candidate["metrics"][name]["response"],
            ),
        }
    for name in ("eligible-one", "eligible-six"):
        raw = json_bytes(prepared_request(candidate["responses"][name]))
        pairs[name]["prepared-request"] = metric(raw, ENCODING)
    pairs["paid-path"] = {
        "filled-vs-from-scratch-arguments": growth(
            paid["metrics"]["from-scratch-arguments"],
            paid["metrics"]["filled-arguments"],
        ),
        "submitted-edit-request": paid["metrics"]["submitted-edit"]["request"],
        "submitted-edit-arguments": paid["metrics"]["submitted-edit"]["arguments"],
        "submitted-edit-response": paid["metrics"]["submitted-edit"]["response"],
    }
    return pairs


def artifact_manifest(result_dir):
    entries = []
    for path in sorted(result_dir.rglob("*")):
        if path.is_file() and path.name != "MANIFEST.sha256":
            entries.append(f"{sha256_file(path)}  {path.relative_to(result_dir)}")
    rendered = "\n".join(entries) + "\n"
    (result_dir / "MANIFEST.sha256").write_text(rendered)
    return sha256_bytes(rendered.encode("utf-8"))


def self_test():
    encoded = json_bytes({"b": 2, "a": "λ"})
    assert encoded == b'{"b":2,"a":"\xce\xbb"}'
    assert growth({"bytes": 100, "tokens": 20}, {"bytes": 125, "tokens": 25})[
        "tokens"
    ]["added_percent"] == 25.0
    assert [name for name, _ in call_specs(Path("/tmp/workspace"))] == [
        "eligible-one",
        "eligible-six",
        "ineligible-seven",
    ]
    print("measure_prepared_request_live_route self-test passed")


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

    global ENCODING
    try:
        import tiktoken
    except ImportError as error:
        raise RuntimeError("tiktoken 0.9.0 is required") from error
    if tiktoken.__version__ != "0.9.0":
        raise RuntimeError(f"wrong tiktoken version: {tiktoken.__version__}")
    ENCODING = tiktoken.get_encoding(TOKENIZER_ENCODING)

    control_identity = assert_identity(args.control_worktree, CONTROL_HEAD, CONTROL_TREE)
    candidate_identity = assert_identity(
        args.candidate_worktree, CANDIDATE_HEAD, CANDIDATE_TREE
    )
    args.result_dir.mkdir(parents=True)
    workspace = args.result_dir / "workspace"
    (workspace / "src").mkdir(parents=True)
    fixture = workspace / "src" / "sample.clj"
    fixture.write_text(
        "(ns sample)\n\n"
        "(def alpha 1)\n(def beta 2)\n(def gamma 3)\n(def delta 4)\n"
        "(def epsilon 5)\n(def zeta 6)\n(def eta 7)\n"
    )
    before_hash = sha256_file(fixture)
    control = capture_arm(
        "control", args.control_worktree, workspace, args.result_dir, ENCODING
    )
    candidate = capture_arm(
        "candidate", args.candidate_worktree, workspace, args.result_dir, ENCODING
    )
    after_hash = sha256_file(fixture)
    paid_workspace = args.result_dir / "paid-workspace"
    (paid_workspace / "src").mkdir(parents=True)
    paid_fixture = paid_workspace / "src" / "sample.clj"
    paid_fixture.write_text("(ns sample)\n\n(def alpha 1)\n")
    paid_before_hash = sha256_file(paid_fixture)
    paid = capture_paid_path(
        args.candidate_worktree, paid_workspace, args.result_dir, ENCODING
    )
    paid_after_hash = sha256_file(paid_fixture)
    expected_paid_fixture = args.result_dir / "expected-paid.clj"
    expected_paid_fixture.write_text("(ns sample)\n\n(def alpha 42)\n")
    expected_paid_hash = sha256_file(expected_paid_fixture)
    semantic = semantic_verdict(control, candidate)
    paid_valid = (
        paid["arguments_semantically_equal"]
        and paid["edit_success"]
        and paid["committed"]
        and paid["verification_complete"]
        and paid_after_hash == expected_paid_hash
    )
    if not semantic["all"] or before_hash != after_hash or not paid_valid:
        raise RuntimeError(
            f"validity gate failed: semantic={semantic} "
            f"read_source_unchanged={before_hash == after_hash} paid_valid={paid_valid}"
        )
    report = {
        "schema": "clj-surgeon.prepared-request-live-route-measurement.v1",
        "identity": {"control": control_identity, "candidate": candidate_identity},
        "tokenizer": {
            "package": TOKENIZER_PACKAGE,
            "encoding": TOKENIZER_ENCODING,
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
            "source_unchanged": before_hash == after_hash,
            "paid_path": {
                "arguments_semantically_equal": paid["arguments_semantically_equal"],
                "edit_success": paid["edit_success"],
                "committed": paid["committed"],
                "verification_complete": paid["verification_complete"],
                "source_hash_before": paid_before_hash,
                "source_hash_after": paid_after_hash,
                "expected_source_hash": expected_paid_hash,
                "exact_expected_source": paid_after_hash == expected_paid_hash,
            },
        },
        "arms": {
            "control": {"metrics": control["metrics"], "exit_code": control["exit_code"]},
            "candidate": {
                "metrics": candidate["metrics"],
                "exit_code": candidate["exit_code"],
            },
            "candidate-paid": {
                "metrics": paid["metrics"],
                "exit_code": paid["exit_code"],
            },
        },
        "comparisons": comparisons(control, candidate, paid),
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
