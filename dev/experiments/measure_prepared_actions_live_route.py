#!/usr/bin/env python3
"""Measure W1 confirm-by-hash and W2 preview on exact private MCP routes.

The harness launches exact control and candidate commits as isolated stdio MCP
processes. It never installs, reloads, registers, or contacts the shared MCP
runtime. All writes are confined to disposable arm-specific fixtures.
"""

import argparse
import copy
import hashlib
import json
import select
import subprocess
import threading
import time
from pathlib import Path


CONTROL_HEAD = "9af88fbae9ee720613599feaf8cf58432c5898bb"
CONTROL_TREE = "6f9bc30316eb6417977c07c86caf8eb146dfbdb8"
CANDIDATE_HEAD = "05f5a1962e5a0c5aa0365c673994eca9024c1a44"
CANDIDATE_TREE = "7cb0f58bdc4d8469d1f7757b0f0ee65e61f4fdc1"
TOKENIZER_PACKAGE = "tiktoken-0.9.0"
TOKENIZER_ENCODING = "o200k_base"
RESPONSE_TIMEOUT_SECONDS = 120
REPLACEMENT = "(def alpha :new)"
TIMING_KEYS = {
    "elapsed_ms",
    "execution_ms",
    "inspection_elapsed_ms",
    "operation_elapsed_ms",
    "server_execution_ms",
    "wall_ms",
}
DYNAMIC_RECEIPT_KEYS = {"receipt_hash", "undo_receipt"}


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
            "saved": -added,
            "added_percent": 100.0 * added / baseline if baseline else None,
            "saved_percent": -100.0 * added / baseline if baseline else None,
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


def tool_call(call_id, name, arguments):
    return {
        "jsonrpc": "2.0",
        "id": call_id,
        "method": "tools/call",
        "params": {"name": name, "arguments": arguments},
    }


def inspect_arguments(file_name, form_name):
    return {
        "requests": [
            {
                "id": "forms",
                "operation": "forms",
                "file": file_name,
                "forms": [form_name],
                "expect": {"forms": 1},
            }
        ],
        "expect": {"requests": 1, "files": 1},
    }


def ordinary_edit_arguments():
    return {
        "edits": [
            {
                "file": "src/ordinary.clj",
                "within": {"form": "omega"},
                "from": "(def omega :old)",
                "to": "(def omega :new)",
                "matches": 1,
            }
        ]
    }


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
            if key not in TIMING_KEYS and key not in DYNAMIC_RECEIPT_KEYS
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


def capture_request(server, arm_dir, stem, request, encoding):
    request_raw, response_raw, response = server.request(request)
    return (
        capture_message(
            arm_dir, stem, request_raw, response_raw, request, encoding
        ),
        response,
    )


def fill_prepared_arguments(prepared_request):
    arguments = copy.deepcopy(prepared_request["arguments"])
    edits = arguments.get("edits") or []
    if len(edits) != 1 or edits[0].get("to", "missing") is not None:
        raise RuntimeError("unexpected prepared descriptor shape")
    edits[0]["to"] = REPLACEMENT
    return arguments


def capture_arm(label, worktree, result_dir, encoding):
    arm_dir = result_dir / label
    workspace = arm_dir / "workspace"
    (workspace / "src").mkdir(parents=True)
    sources = {
        "demo.clj": "(ns demo)\n(def alpha :old)\n",
        "ineligible.clj": "(ns ineligible)\n",
        "ordinary.clj": "(ns ordinary)\n(def omega :old)\n",
    }
    for name, source in sources.items():
        (workspace / "src" / name).write_text(source)
    before = {
        name: sha256_file(workspace / "src" / name) for name in sorted(sources)
    }
    server = McpProcess(worktree, workspace, arm_dir)
    metrics = {}
    responses = {}
    extracted = {}
    preview_source_hashes = {}
    try:
        initialize = {
            "jsonrpc": "2.0",
            "id": 1,
            "method": "initialize",
            "params": {
                "protocolVersion": "2024-11-05",
                "capabilities": {},
                "clientInfo": {
                    "name": "prepared-actions-live-measure",
                    "version": "1",
                },
            },
        }
        metrics["initialize"], responses["initialize"] = capture_request(
            server, arm_dir, "00-initialize", initialize, encoding
        )
        server.notify(
            {"jsonrpc": "2.0", "method": "notifications/initialized", "params": {}}
        )

        tools_list = {"jsonrpc": "2.0", "id": 2, "method": "tools/list", "params": {}}
        metrics["tools-list"], responses["tools-list"] = capture_request(
            server, arm_dir, "02-tools-list", tools_list, encoding
        )
        tools = (responses["tools-list"].get("result") or {}).get("tools") or []
        for tool_name in ("inspect_clojure", "edit_clojure"):
            tool = next(item for item in tools if item.get("name") == tool_name)
            raw = json_bytes(tool)
            (arm_dir / f"03-{tool_name.replace('_', '-')}-tool.json").write_bytes(raw)
            metrics[f"{tool_name}-tool"] = metric(raw, encoding)

        ineligible = tool_call(
            3,
            "inspect_clojure",
            inspect_arguments("src/ineligible.clj", "ns"),
        )
        metrics["ineligible-inspect"], responses["ineligible-inspect"] = capture_request(
            server, arm_dir, "10-ineligible-inspect", ineligible, encoding
        )

        eligible = tool_call(
            4, "inspect_clojure", inspect_arguments("src/demo.clj", "alpha")
        )
        metrics["eligible-inspect"], responses["eligible-inspect"] = capture_request(
            server, arm_dir, "11-eligible-inspect", eligible, encoding
        )
        eligible_content = structured_content(responses["eligible-inspect"])
        prepared_request = eligible_content.get("prepared_request")
        if not isinstance(prepared_request, dict):
            raise RuntimeError(f"{label} did not return prepared_request")
        full_arguments = fill_prepared_arguments(prepared_request)
        full_arguments_raw = json_bytes(full_arguments)
        extracted["prepared-request-arguments"] = metric(full_arguments_raw, encoding)
        (arm_dir / "12-prepared-request-filled-arguments.json").write_bytes(
            full_arguments_raw
        )

        confirmation = eligible_content.get("prepared_confirmation")
        if label == "candidate":
            if not isinstance(confirmation, dict):
                raise RuntimeError("candidate did not return prepared_confirmation")
            digest = confirmation.get("descriptor_sha256")
            compact_arguments = {
                "confirm": digest,
                "fill": {"arguments.edits[0].to": REPLACEMENT},
            }
            compact_raw = json_bytes(compact_arguments)
            extracted["confirm-fill-arguments"] = metric(compact_raw, encoding)
            (arm_dir / "13-confirm-fill-arguments.json").write_bytes(compact_raw)

            preview_arguments = {**compact_arguments, "preview": True}
            preview = tool_call(5, "edit_clojure", preview_arguments)
            preview_source_hashes["before"] = sha256_file(workspace / "src/demo.clj")
            metrics["preview"], responses["preview"] = capture_request(
                server, arm_dir, "14-preview", preview, encoding
            )
            preview_source_hashes["after"] = sha256_file(workspace / "src/demo.clj")

            commit = tool_call(6, "edit_clojure", compact_arguments)
            metrics["prepared-commit"], responses["prepared-commit"] = capture_request(
                server, arm_dir, "15-prepared-commit", commit, encoding
            )
            replay = tool_call(7, "edit_clojure", compact_arguments)
            metrics["consumed-replay"], responses["consumed-replay"] = capture_request(
                server, arm_dir, "16-consumed-replay", replay, encoding
            )
        else:
            if confirmation is not None:
                raise RuntimeError("control unexpectedly returned prepared_confirmation")
            full_commit = tool_call(6, "edit_clojure", full_arguments)
            metrics["full-commit"], responses["full-commit"] = capture_request(
                server, arm_dir, "15-full-commit", full_commit, encoding
            )

        ordinary = tool_call(8, "edit_clojure", ordinary_edit_arguments())
        metrics["ordinary-edit"], responses["ordinary-edit"] = capture_request(
            server, arm_dir, "17-ordinary-edit", ordinary, encoding
        )
    finally:
        exit_code = server.close()
        (arm_dir / "command.json").write_bytes(json_bytes(server.command) + b"\n")

    after = {
        name: sha256_file(workspace / "src" / name) for name in sorted(sources)
    }
    normalized = {}
    for name in ("ineligible-inspect", "ordinary-edit"):
        value = normalize_dynamic(
            structured_content(responses[name]), workspace, arm_dir
        )
        raw = json_bytes(value)
        (arm_dir / f"20-{name}.normalized.json").write_bytes(raw)
        normalized[name] = {"value": value, "metric": metric(raw, encoding)}
    return {
        "metrics": metrics,
        "responses": responses,
        "extracted": extracted,
        "before": before,
        "after": after,
        "preview_source_hashes": preview_source_hashes,
        "normalized": normalized,
        "exit_code_after_termination": exit_code,
    }


def validate(control, candidate):
    control_eligible = structured_content(control["responses"]["eligible-inspect"])
    candidate_eligible = structured_content(candidate["responses"]["eligible-inspect"])
    confirmation = candidate_eligible.get("prepared_confirmation") or {}
    preview = structured_content(candidate["responses"]["preview"])
    control_commit = structured_content(control["responses"]["full-commit"])
    candidate_commit = structured_content(candidate["responses"]["prepared-commit"])
    replay = structured_content(candidate["responses"]["consumed-replay"])
    checks = {
        "tool_name_order_equal": (
            [item.get("name") for item in (control["responses"]["tools-list"].get("result") or {}).get("tools") or []]
            == [item.get("name") for item in (candidate["responses"]["tools-list"].get("result") or {}).get("tools") or []]
        ),
        "control_has_prepared_request": isinstance(
            control_eligible.get("prepared_request"), dict
        ),
        "control_has_no_confirmation": "prepared_confirmation" not in control_eligible,
        "candidate_has_prepared_request": isinstance(
            candidate_eligible.get("prepared_request"), dict
        ),
        "candidate_confirmation_inert": (
            isinstance(confirmation.get("descriptor_sha256"), str)
            and len(confirmation.get("descriptor_sha256")) == 64
            and confirmation.get("session_bound") is True
            and confirmation.get("commit_single_use") is True
            and confirmation.get("executable") is False
            and confirmation.get("write_authority") is False
        ),
        "preview_success": (
            preview.get("ok") is True
            and preview.get("operation") == "edit_clojure-preview"
            and preview.get("lifecycle") == "preview"
            and preview.get("committed") is False
            and preview.get("source_unchanged") is True
            and isinstance(preview.get("diff"), str)
            and bool(preview.get("diff"))
        ),
        "preview_source_byte_identical": (
            candidate["preview_source_hashes"].get("before")
            == candidate["preview_source_hashes"].get("after")
        ),
        "both_commit": (
            control_commit.get("ok") is True
            and control_commit.get("committed") is True
            and candidate_commit.get("ok") is True
            and candidate_commit.get("committed") is True
        ),
        "commit_effect_equal": (
            control["after"]["demo.clj"] == candidate["after"]["demo.clj"]
        ),
        "commit_effect_exact": (
            control_commit.get("files") == 1
            and candidate_commit.get("files") == 1
        ),
        "replay_consumed": (
            replay.get("ok") is False
            and replay.get("error_type") == "prepared-confirmation-consumed"
            and replay.get("source_unchanged") is True
        ),
        "ineligible_source_unchanged": all(
            arm["before"]["ineligible.clj"] == arm["after"]["ineligible.clj"]
            for arm in (control, candidate)
        ),
        "ineligible_no_confirmation": (
            "prepared_confirmation"
            not in structured_content(candidate["responses"]["ineligible-inspect"])
        ),
        "ineligible_normalized_byte_identical": (
            control["normalized"]["ineligible-inspect"]["metric"]["sha256"]
            == candidate["normalized"]["ineligible-inspect"]["metric"]["sha256"]
        ),
        "ordinary_request_byte_identical": (
            control["metrics"]["ordinary-edit"]["request"]["sha256"]
            == candidate["metrics"]["ordinary-edit"]["request"]["sha256"]
        ),
        "ordinary_both_commit": all(
            structured_content(arm["responses"]["ordinary-edit"]).get("ok") is True
            and structured_content(arm["responses"]["ordinary-edit"]).get("committed") is True
            for arm in (control, candidate)
        ),
        "ordinary_effect_equal": (
            control["after"]["ordinary.clj"] == candidate["after"]["ordinary.clj"]
        ),
        "ordinary_normalized_byte_identical": (
            control["normalized"]["ordinary-edit"]["metric"]["sha256"]
            == candidate["normalized"]["ordinary-edit"]["metric"]["sha256"]
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
    control_worktree = args.control_worktree.resolve()
    candidate_worktree = args.candidate_worktree.resolve()
    result_dir = args.result_dir.resolve()
    if result_dir.exists():
        raise RuntimeError("result directory must not exist")

    try:
        import tiktoken
    except ImportError as error:
        raise RuntimeError("tiktoken 0.9.0 is required") from error
    if tiktoken.__version__ != "0.9.0":
        raise RuntimeError(f"wrong tiktoken version: {tiktoken.__version__}")
    encoding = tiktoken.get_encoding(TOKENIZER_ENCODING)

    identity = {
        "control": assert_identity(control_worktree, CONTROL_HEAD, CONTROL_TREE),
        "candidate": assert_identity(
            candidate_worktree, CANDIDATE_HEAD, CANDIDATE_TREE
        ),
    }
    result_dir.mkdir(parents=True)
    control = capture_arm("control", control_worktree, result_dir, encoding)
    candidate = capture_arm(
        "candidate", candidate_worktree, result_dir, encoding
    )
    validity = validate(control, candidate)
    if not validity["all"]:
        raise RuntimeError(f"validity gate failed: {validity}")

    comparisons = {
        "tools-list-response": delta(
            control["metrics"]["tools-list"]["response"],
            candidate["metrics"]["tools-list"]["response"],
        ),
        "inspect-clojure-tool": delta(
            control["metrics"]["inspect_clojure-tool"],
            candidate["metrics"]["inspect_clojure-tool"],
        ),
        "edit-clojure-tool": delta(
            control["metrics"]["edit_clojure-tool"],
            candidate["metrics"]["edit_clojure-tool"],
        ),
        "full-versus-confirm-fill": {
            "request": delta(
                control["metrics"]["full-commit"]["request"],
                candidate["metrics"]["prepared-commit"]["request"],
            ),
            "arguments": delta(
                control["metrics"]["full-commit"]["arguments"],
                candidate["metrics"]["prepared-commit"]["arguments"],
            ),
        },
        "filled-descriptor-versus-confirm-fill-arguments": delta(
            control["extracted"]["prepared-request-arguments"],
            candidate["extracted"]["confirm-fill-arguments"],
        ),
        "preview": candidate["metrics"]["preview"],
        "consumed-replay-response": candidate["metrics"]["consumed-replay"]["response"],
        "ineligible-inspect": {
            "request": delta(
                control["metrics"]["ineligible-inspect"]["request"],
                candidate["metrics"]["ineligible-inspect"]["request"],
            ),
            "response": delta(
                control["metrics"]["ineligible-inspect"]["response"],
                candidate["metrics"]["ineligible-inspect"]["response"],
            ),
        },
        "ordinary-edit": {
            "request": delta(
                control["metrics"]["ordinary-edit"]["request"],
                candidate["metrics"]["ordinary-edit"]["request"],
            ),
            "response": delta(
                control["metrics"]["ordinary-edit"]["response"],
                candidate["metrics"]["ordinary-edit"]["response"],
            ),
        },
    }

    report = {
        "schema": "clj-surgeon.prepared-actions-live-route-measurement.v1",
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
                "extracted": control["extracted"],
                "normalized": {
                    name: item["metric"] for name, item in control["normalized"].items()
                },
                "exit_code_after_termination": control["exit_code_after_termination"],
            },
            "candidate": {
                "metrics": candidate["metrics"],
                "extracted": candidate["extracted"],
                "normalized": {
                    name: item["metric"] for name, item in candidate["normalized"].items()
                },
                "exit_code_after_termination": candidate["exit_code_after_termination"],
            },
        },
        "comparisons": comparisons,
        "claim_scope": {
            "measured": "serialized catalog, caller-emitted confirm/fill deletion, preview input price, and no-cue compatibility",
            "projected": "recovery-turn and complete-wall benefit from prior prepared-request cohorts",
            "not_measured": "routing lift, adoption, model wall, or provider billing tokens",
        },
    }
    report_path = result_dir / "report.json"
    report_path.write_bytes(json_bytes(report) + b"\n")
    manifest_sha = artifact_manifest(result_dir)
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
