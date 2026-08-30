#!/usr/bin/env python3
"""Measure WRITE-REFUSAL-001 on exact serialized private MCP routes.

The harness launches exact control and candidate commits as isolated stdio MCP
processes. It never installs, reloads, registers, or contacts the shared MCP
runtime. All mutations are confined to disposable arm-specific fixtures.
"""

import argparse
import hashlib
import json
import select
import subprocess
import threading
import time
from pathlib import Path


CONTROL_HEAD = "b445a8c3595d70f6f05b6edccb9b1a924539a195"
CONTROL_TREE = "b1e21af8073e66283f82f4036583bfe2971c4b0a"
CANDIDATE_HEAD = "9af88fbae9ee720613599feaf8cf58432c5898bb"
CANDIDATE_TREE = "6f9bc30316eb6417977c07c86caf8eb146dfbdb8"
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
    return {
        "bytes": len(payload),
        "tokens": len(encoding.encode(payload.decode("utf-8"))),
        "sha256": sha256_bytes(payload),
    }


def delta(control, candidate):
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


def form_source(namespace, count):
    forms = [f"(defn owner-{index:03d} [] :old)" for index in range(count)]
    return f"(ns {namespace})\n\n" + "\n\n".join(forms) + "\n"


def form_names(count):
    return [f"owner-{index:03d}" for index in range(count)]


def tool_call(call_id, arguments):
    return {
        "jsonrpc": "2.0",
        "id": call_id,
        "method": "tools/call",
        "params": {"name": "edit_clojure", "arguments": arguments},
    }


def call_specs():
    median = {
        "changes": [
            {
                "id": "median-count-mismatch",
                "files": ["src/median.clj"],
                "forms": form_names(27),
                "find": ":old",
                "replace": ":new",
                "expect": {"matches": 28},
            }
        ]
    }
    boundary = {
        "changes": [
            {
                "id": "boundary-count-mismatch",
                "files": ["src/boundary.clj"],
                "forms": form_names(129),
                "find": ":old",
                "replace": ":new",
                "expect": {"matches": 130},
            }
        ]
    }
    success = {
        "changes": [
            {
                "id": "ordinary-success",
                "files": ["src/success.clj"],
                "forms": ["owner-000"],
                "find": ":old",
                "replace": ":new",
                "expect": {"matches": 1},
            }
        ]
    }
    return [
        ("median-refusal", tool_call(101, median)),
        ("boundary-refusal", tool_call(102, boundary)),
        ("ordinary-success", tool_call(103, success)),
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


def structured_content(response):
    return (response.get("result") or {}).get("structuredContent") or {}


def normalize_dynamic(value, workspace, arm_dir):
    if isinstance(value, dict):
        return {
            key: normalize_dynamic(item, workspace, arm_dir)
            for key, item in value.items()
            if key not in TIMING_KEYS
        }
    if isinstance(value, list):
        return [normalize_dynamic(item, workspace, arm_dir) for item in value]
    if isinstance(value, str):
        return value.replace(str(workspace), "$WORKSPACE").replace(
            str(arm_dir), "$ARM"
        )
    return value


def capture_message(directory, stem, request_raw, response_raw, request, encoding):
    (directory / f"{stem}.request.json").write_bytes(request_raw)
    (directory / f"{stem}.response.json").write_bytes(response_raw)
    arguments_raw = json_bytes(request.get("params", {}).get("arguments", {}))
    return {
        "request": metric(request_raw, encoding),
        "arguments": metric(arguments_raw, encoding),
        "response": metric(response_raw, encoding),
    }


def capture_arm(label, worktree, result_dir, encoding):
    arm_dir = result_dir / label
    workspace = arm_dir / "workspace"
    (workspace / "src").mkdir(parents=True)
    (workspace / "src/median.clj").write_text(form_source("fixture.median", 27))
    (workspace / "src/boundary.clj").write_text(
        form_source("fixture.boundary", 129)
    )
    (workspace / "src/success.clj").write_text(form_source("fixture.success", 1))
    before = {
        path.name: sha256_file(path) for path in sorted((workspace / "src").iterdir())
    }
    server = McpProcess(worktree, workspace, arm_dir)
    metrics = {}
    responses = {}
    try:
        initialize = {
            "jsonrpc": "2.0",
            "id": 1,
            "method": "initialize",
            "params": {
                "protocolVersion": "2024-11-05",
                "capabilities": {},
                "clientInfo": {"name": "write-refusal-live-measure", "version": "1"},
            },
        }
        request_raw, response_raw, response = server.request(initialize)
        metrics["initialize"] = capture_message(
            arm_dir, "00-initialize", request_raw, response_raw, initialize, encoding
        )
        responses["initialize"] = response
        server.notify(
            {"jsonrpc": "2.0", "method": "notifications/initialized", "params": {}}
        )
        tools_list = {"jsonrpc": "2.0", "id": 2, "method": "tools/list", "params": {}}
        request_raw, response_raw, response = server.request(tools_list)
        metrics["tools-list"] = capture_message(
            arm_dir, "02-tools-list", request_raw, response_raw, tools_list, encoding
        )
        responses["tools-list"] = response
        tools = (response.get("result") or {}).get("tools") or []
        edit_tool = next(tool for tool in tools if tool.get("name") == "edit_clojure")
        edit_raw = json_bytes(edit_tool)
        (arm_dir / "03-edit-clojure-tool.json").write_bytes(edit_raw)
        metrics["edit-clojure-tool"] = metric(edit_raw, encoding)
        for index, (name, request) in enumerate(call_specs(), start=10):
            request_raw, response_raw, response = server.request(request)
            metrics[name] = capture_message(
                arm_dir, f"{index:02d}-{name}", request_raw, response_raw, request, encoding
            )
            responses[name] = response
    finally:
        exit_code = server.close()
        (arm_dir / "command.json").write_bytes(json_bytes(server.command) + b"\n")
    after = {
        path.name: sha256_file(path) for path in sorted((workspace / "src").iterdir())
    }
    normalized_success = normalize_dynamic(
        structured_content(responses["ordinary-success"]), workspace, arm_dir
    )
    normalized_success_raw = json_bytes(normalized_success)
    (arm_dir / "20-ordinary-success.normalized.json").write_bytes(
        normalized_success_raw
    )
    return {
        "metrics": metrics,
        "responses": responses,
        "before": before,
        "after": after,
        "normalized_success": normalized_success,
        "normalized_success_metric": metric(normalized_success_raw, encoding),
        "exit_code_after_termination": exit_code,
    }


def has_no_authority(value):
    forbidden_keys = {
        "next_call",
        "prepared_request",
        "replacement",
        "selected_candidate",
    }
    if isinstance(value, dict):
        if forbidden_keys.intersection(value):
            return False
        for key in ("authority", "write_authority", "executable"):
            if value.get(key) is True:
                return False
        return all(has_no_authority(item) for item in value.values())
    if isinstance(value, list):
        return all(has_no_authority(item) for item in value)
    return True


def validate(control, candidate):
    control_median = structured_content(control["responses"]["median-refusal"])
    candidate_median = structured_content(candidate["responses"]["median-refusal"])
    candidate_boundary = structured_content(candidate["responses"]["boundary-refusal"])
    evidence = candidate_median.get("write_refusal_evidence") or {}
    boundary_evidence = candidate_boundary.get("write_refusal_evidence") or {}
    checks = {
        "catalog_raw_byte_identical": (
            control["metrics"]["tools-list"]["response"]["sha256"]
            == candidate["metrics"]["tools-list"]["response"]["sha256"]
        ),
        "edit_tool_raw_byte_identical": (
            control["metrics"]["edit-clojure-tool"]["sha256"]
            == candidate["metrics"]["edit-clojure-tool"]["sha256"]
        ),
        "requests_raw_byte_identical": all(
            control["metrics"][name]["request"]["sha256"]
            == candidate["metrics"][name]["request"]["sha256"]
            for name in ("median-refusal", "boundary-refusal", "ordinary-success")
        ),
        "control_median_refuses": control_median.get("ok") is False,
        "candidate_median_refuses": candidate_median.get("ok") is False,
        "candidate_median_complete": (
            evidence.get("available_count") == 27
            and evidence.get("returned_count") == 27
            and evidence.get("omitted_count") == 0
            and evidence.get("truncated") is False
            and len(evidence.get("items") or []) == 27
        ),
        "candidate_boundary_bounded": (
            boundary_evidence.get("available_count") == 129
            and boundary_evidence.get("returned_count") == 128
            and boundary_evidence.get("omitted_count") == 1
            and boundary_evidence.get("truncated") is True
            and len(boundary_evidence.get("items") or []) == 128
        ),
        "candidate_refusals_inert": (
            has_no_authority(candidate_median)
            and has_no_authority(candidate_boundary)
        ),
        "refusal_sources_unchanged": all(
            arm["before"][name] == arm["after"][name]
            for arm in (control, candidate)
            for name in ("median.clj", "boundary.clj")
        ),
        "successes_commit": all(
            structured_content(arm["responses"]["ordinary-success"]).get("ok") is True
            for arm in (control, candidate)
        ),
        "success_effect_equal": (
            control["after"]["success.clj"] == candidate["after"]["success.clj"]
        ),
        "success_no_cue_normalized_byte_identical": (
            control["normalized_success_metric"]["sha256"]
            == candidate["normalized_success_metric"]["sha256"]
        ),
    }
    checks["all"] = all(checks.values())
    return checks


def artifact_manifest(result_dir):
    rows = []
    for path in sorted(result_dir.rglob("*")):
        if path.is_file() and path.name != "manifest.sha256":
            rows.append(f"{sha256_file(path)}  {path.relative_to(result_dir)}")
    payload = ("\n".join(rows) + "\n").encode("utf-8")
    manifest = result_dir / "manifest.sha256"
    manifest.write_bytes(payload)
    return sha256_file(manifest)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--control-worktree", type=Path, required=True)
    parser.add_argument("--candidate-worktree", type=Path, required=True)
    parser.add_argument("--result-dir", type=Path, required=True)
    args = parser.parse_args()
    if args.result_dir.exists():
        raise RuntimeError("result directory must not exist")

    try:
        import tiktoken
    except ImportError as error:
        raise RuntimeError("tiktoken 0.9.0 is required") from error
    if tiktoken.__version__ != "0.9.0":
        raise RuntimeError(f"wrong tiktoken version: {tiktoken.__version__}")
    encoding = tiktoken.get_encoding(TOKENIZER_ENCODING)

    identity = {
        "control": assert_identity(
            args.control_worktree, CONTROL_HEAD, CONTROL_TREE
        ),
        "candidate": assert_identity(
            args.candidate_worktree, CANDIDATE_HEAD, CANDIDATE_TREE
        ),
    }
    args.result_dir.mkdir(parents=True)
    control = capture_arm(
        "control", args.control_worktree, args.result_dir, encoding
    )
    candidate = capture_arm(
        "candidate", args.candidate_worktree, args.result_dir, encoding
    )
    validity = validate(control, candidate)
    if not validity["all"]:
        raise RuntimeError(f"validity gate failed: {validity}")

    comparisons = {
        name: {
            "request": delta(
                control["metrics"][name]["request"],
                candidate["metrics"][name]["request"],
            ),
            "arguments": delta(
                control["metrics"][name]["arguments"],
                candidate["metrics"][name]["arguments"],
            ),
            "response": delta(
                control["metrics"][name]["response"],
                candidate["metrics"][name]["response"],
            ),
        }
        for name in ("median-refusal", "boundary-refusal", "ordinary-success")
    }
    comparisons["tools-list-response"] = delta(
        control["metrics"]["tools-list"]["response"],
        candidate["metrics"]["tools-list"]["response"],
    )
    comparisons["edit-clojure-tool"] = delta(
        control["metrics"]["edit-clojure-tool"],
        candidate["metrics"]["edit-clojure-tool"],
    )

    report = {
        "schema": "clj-surgeon.write-refusal-001-live-route-measurement.v1",
        "identity": identity,
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
            "route_adherent": True,
        },
        "validity": validity,
        "arms": {
            "control": {
                "metrics": control["metrics"],
                "normalized_success_metric": control["normalized_success_metric"],
                "exit_code_after_termination": control["exit_code_after_termination"],
            },
            "candidate": {
                "metrics": candidate["metrics"],
                "normalized_success_metric": candidate["normalized_success_metric"],
                "exit_code_after_termination": candidate["exit_code_after_termination"],
            },
        },
        "comparisons": comparisons,
        "claim_scope": {
            "measured": "response-input price and no-cue behavior for generic write expect-count-mismatch",
            "not_measured": "recovery-turn reduction for this write refusal family",
            "excluded_evidence": "191 firings / 2.72 MB / 33.5 min belongs to read-side batch-form-selection-failed",
        },
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
