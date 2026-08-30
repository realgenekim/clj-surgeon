#!/usr/bin/env python3
"""Freeze, preflight, run, score, and archive the n=10/arm replication."""

from __future__ import annotations

import argparse
import difflib
import gzip
import hashlib
import json
import math
import os
import select
import shutil
import statistics
import subprocess
import sys
import tarfile
import time
from pathlib import Path
from typing import Any


ROOT = Path(__file__).resolve().parent
INITIAL = ROOT / "fixture" / "initial"
EXPECTED = ROOT / "fixture" / "expected"
PROXY = ROOT / "mcp_proxy.py"
PROMPT_TEMPLATE = (ROOT / "prompt.txt").read_text(encoding="utf-8")
INSPECT_TEMPLATE = json.loads((ROOT / "inspect-template.json").read_text(encoding="utf-8"))
EDIT_TEMPLATE = json.loads((ROOT / "edit-template.json").read_text(encoding="utf-8"))
FREEZE = ROOT / "freeze.json"
PREFLIGHT = ROOT / "preflight"
RUNS = ROOT / "runs"
WORKSPACES = ROOT / "workspaces"
ATTEMPTS = ROOT / "attempts.jsonl"
AGGREGATE = ROOT / "aggregate.json"
ARCHIVES = ROOT / "archives"
PYTHON = Path(sys.executable).resolve()
CODEX = Path(shutil.which("codex") or "/missing/codex").resolve()
MODEL = "gpt-5.6-sol"
REASONING = "high"
SCHEDULE = list("UPPUPUUPUPPUPUUPUPPU")
TARGETS = {"src/acme/checkout_policy.clj", "src/acme/checkout_client.clj"}
DISTRACTOR = "src/acme/analytics_policy.clj"
STATIC_NAMES = {
    "preregistration.md",
    "prompt.txt",
    "inspect-template.json",
    "edit-template.json",
    "mcp_proxy.py",
    "run_experiment.py",
    "replay.sh",
    "README.md",
    "quota-preflight.json",
    "freeze-original.json",
    "preflight-repair-addendum.md",
}


def canonical_bytes(value: Any) -> bytes:
    return json.dumps(value, sort_keys=True, separators=(",", ":"), ensure_ascii=False).encode("utf-8")


def sha_bytes(value: bytes) -> str:
    return hashlib.sha256(value).hexdigest()


def sha_file(path: Path) -> str:
    return sha_bytes(path.read_bytes())


def atomic_json(path: Path, value: Any) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    stage = path.with_suffix(path.suffix + ".tmp")
    stage.write_text(json.dumps(value, indent=2, sort_keys=True, ensure_ascii=False) + "\n", encoding="utf-8")
    stage.replace(path)


def append_jsonl(path: Path, value: Any) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("a", encoding="utf-8") as handle:
        handle.write(json.dumps(value, sort_keys=True, ensure_ascii=False) + "\n")
        handle.flush()
        os.fsync(handle.fileno())


def read_jsonl(path: Path) -> list[dict[str, Any]]:
    rows: list[dict[str, Any]] = []
    if not path.exists():
        return rows
    for number, line in enumerate(path.read_text(encoding="utf-8", errors="replace").splitlines(), start=1):
        if not line.strip():
            continue
        try:
            rows.append(json.loads(line))
        except json.JSONDecodeError:
            rows.append({"_invalid_json_line": number})
    return rows


def run_capture(argv: list[str], cwd: Path, timeout: int = 120, env: dict[str, str] | None = None) -> dict[str, Any]:
    started_ns = time.time_ns()
    try:
        completed = subprocess.run(argv, cwd=cwd, text=True, capture_output=True, timeout=timeout, check=False, env=env)
        return {
            "argv": argv,
            "started_ns": started_ns,
            "ended_ns": time.time_ns(),
            "exit_code": completed.returncode,
            "stdout": completed.stdout,
            "stderr": completed.stderr,
            "timed_out": False,
        }
    except subprocess.TimeoutExpired as exc:
        return {
            "argv": argv,
            "started_ns": started_ns,
            "ended_ns": time.time_ns(),
            "exit_code": None,
            "stdout": exc.stdout or "",
            "stderr": exc.stderr or "",
            "timed_out": True,
        }


def initial_files() -> list[Path]:
    return sorted(path for path in INITIAL.rglob("*") if path.is_file())


def static_files() -> list[Path]:
    files = [ROOT / name for name in sorted(STATIC_NAMES)]
    files.extend(initial_files())
    files.extend(sorted(path for path in EXPECTED.rglob("*") if path.is_file()))
    return files


def static_hashes() -> dict[str, str]:
    return {str(path.relative_to(ROOT)): sha_file(path) for path in static_files()}


def inspect_arguments(workspace: Path) -> dict[str, Any]:
    return {"workspace_root": str(workspace.resolve()), **json.loads(json.dumps(INSPECT_TEMPLATE))}


def edit_arguments(workspace: Path) -> dict[str, Any]:
    return {"workspace_root": str(workspace.resolve()), **json.loads(json.dumps(EDIT_TEMPLATE))}


def render_prompt(workspace: Path) -> str:
    payload = json.dumps(inspect_arguments(workspace), indent=2, sort_keys=True, ensure_ascii=False)
    return PROMPT_TEMPLATE.replace("__INSPECT_REQUEST__", payload)


def reset_workspace(path: Path) -> str:
    if path.exists():
        shutil.rmtree(path)
    shutil.copytree(INITIAL, path)
    # The production MCP service runs as the dedicated `surgeon` account while
    # Codex/native patch runs as dev-a. These are disposable synthetic clones;
    # make only their fixture tree mutually writable so both routes are real.
    for directory in [path, *sorted(candidate for candidate in path.rglob("*") if candidate.is_dir())]:
        directory.chmod(0o777)
    for file_path in sorted(candidate for candidate in path.rglob("*") if candidate.is_file()):
        file_path.chmod(0o666)
    init = run_capture(["git", "init", "-q"], path)
    add = run_capture(["git", "add", "."], path)
    commit = run_capture(
        ["git", "-c", "user.name=fixture", "-c", "user.email=fixture@invalid", "commit", "-q", "-m", "fixture: initial multi-file policy"],
        path,
    )
    if any(row["exit_code"] != 0 for row in (init, add, commit)):
        raise RuntimeError("fixture repository initialization failed")
    return run_capture(["git", "rev-parse", "HEAD"], path)["stdout"].strip()


def proxy_env(arm: str, workspace: Path, log_path: Path) -> dict[str, str]:
    env = os.environ.copy()
    env.update({
        "PREPARED_ARM": arm,
        "PREPARED_WORKSPACE": str(workspace.resolve()),
        "PREPARED_SERVER_LOG": str(log_path.resolve()),
    })
    return env


def proxy_argv(arm: str, workspace: Path, log_path: Path) -> list[str]:
    return [
        "PREPARED_ARM=" + arm,
        "PREPARED_WORKSPACE=" + str(workspace.resolve()),
        "PREPARED_SERVER_LOG=" + str(log_path.resolve()),
        str(PYTHON),
        str(PROXY),
    ]


class ProxyClient:
    def __init__(self, arm: str, workspace: Path, log_path: Path, stderr_path: Path) -> None:
        stderr_path.parent.mkdir(parents=True, exist_ok=True)
        self.stderr_handle = stderr_path.open("w", encoding="utf-8")
        self.process = subprocess.Popen(
            [str(PYTHON), str(PROXY)],
            stdin=subprocess.PIPE,
            stdout=subprocess.PIPE,
            stderr=self.stderr_handle,
            text=True,
            bufsize=1,
            env=proxy_env(arm, workspace, log_path),
        )
        self.next_id = 1

    def request(self, method: str, params: dict[str, Any]) -> dict[str, Any]:
        request_id = self.next_id
        self.next_id += 1
        assert self.process.stdin is not None and self.process.stdout is not None
        self.process.stdin.write(json.dumps({"jsonrpc": "2.0", "id": request_id, "method": method, "params": params}) + "\n")
        self.process.stdin.flush()
        ready, _, _ = select.select([self.process.stdout], [], [], 60)
        if not ready:
            raise TimeoutError(method)
        response = json.loads(self.process.stdout.readline())
        if "error" in response:
            raise RuntimeError(response["error"])
        return response["result"]

    def initialize(self) -> dict[str, Any]:
        result = self.request(
            "initialize",
            {"protocolVersion": "2025-03-26", "capabilities": {}, "clientInfo": {"name": "replication-preflight", "version": "1"}},
        )
        assert self.process.stdin is not None
        self.process.stdin.write(json.dumps({"jsonrpc": "2.0", "method": "notifications/initialized", "params": {}}) + "\n")
        self.process.stdin.flush()
        return result

    def close(self) -> None:
        if self.process.poll() is None:
            self.process.terminate()
            try:
                self.process.wait(timeout=5)
            except subprocess.TimeoutExpired:
                self.process.kill()
                self.process.wait(timeout=5)
        self.stderr_handle.close()


def expected_bytes_ok(workspace: Path) -> bool:
    return all((workspace / relative).read_bytes() == (EXPECTED / relative).read_bytes() for relative in sorted(TARGETS))


def unrelated_bytes_ok(workspace: Path) -> bool:
    for initial in initial_files():
        relative = str(initial.relative_to(INITIAL))
        if relative in TARGETS:
            continue
        candidate = workspace / relative
        if not candidate.exists() or candidate.read_bytes() != initial.read_bytes():
            return False
    return True


def verify_fixture(workspace: Path, receipt_path: Path) -> dict[str, Any]:
    result = run_capture(["clojure", "-M:test"], workspace, timeout=120)
    atomic_json(receipt_path, result)
    return result


def native_patch(workspace: Path) -> dict[str, Any]:
    chunks = []
    for relative in sorted(TARGETS):
        before = (INITIAL / relative).read_text(encoding="utf-8").splitlines(keepends=True)
        after = (EXPECTED / relative).read_text(encoding="utf-8").splitlines(keepends=True)
        chunks.extend(difflib.unified_diff(before, after, fromfile=f"a/{relative}", tofile=f"b/{relative}"))
    patch = "".join(chunks)
    patch_command = shutil.which("patch")
    if not patch_command:
        return {"available": False, "exact": False, "reason": "patch executable missing in preflight shell"}
    started_ns = time.time_ns()
    completed = subprocess.run([patch_command, "-p1", "--forward", "--batch"], cwd=workspace, text=True, input=patch, capture_output=True, check=False)
    return {
        "available": True,
        "exit_code": completed.returncode,
        "stdout": completed.stdout,
        "stderr": completed.stderr,
        "exact": completed.returncode == 0 and expected_bytes_ok(workspace) and unrelated_bytes_ok(workspace),
        "started_ns": started_ns,
        "ended_ns": time.time_ns(),
    }


def require_freeze() -> dict[str, Any]:
    if not FREEZE.exists():
        raise RuntimeError("freeze.json is required")
    receipt = json.loads(FREEZE.read_text(encoding="utf-8"))
    if receipt.get("status") != "frozen" or receipt.get("static_hashes") != static_hashes():
        raise RuntimeError("frozen input drift")
    if receipt.get("schedule") != SCHEDULE:
        raise RuntimeError("schedule drift")
    return receipt


def freeze() -> None:
    if FREEZE.exists():
        raise RuntimeError("freeze is one-shot")
    self_test(write_receipt=False)
    receipt = {
        "schema": "prepared-request-replication-freeze.v1",
        "status": "frozen",
        "frozen_at_ns": time.time_ns(),
        "static_hashes": static_hashes(),
        "schedule": SCHEDULE,
        "schedule_sha256": sha_bytes(canonical_bytes(SCHEDULE)),
        "model": MODEL,
        "reasoning_effort": REASONING,
        "attempts_per_arm": 10,
        "primary_prediction": {"U": 0.4, "P": 0.8, "risk_difference": 0.4},
        "non_replication_kill_below_risk_difference": 0.2,
        "wrong_subject_required": 0,
    }
    atomic_json(FREEZE, receipt)
    print(json.dumps(receipt, sort_keys=True))


def preflight() -> None:
    freeze_receipt = require_freeze()
    if PREFLIGHT.exists():
        raise RuntimeError("preflight is one-shot")
    PREFLIGHT.mkdir(parents=True)
    auth_env = os.environ.copy()
    openai_api_key_absent = not bool(auth_env.get("OPENAI_API_KEY"))
    codex_version = run_capture([str(CODEX), "--version"], ROOT)
    codex_auth = run_capture([str(CODEX), "login", "status"], ROOT)
    baseline_workspace = PREFLIGHT / "baseline-workspace"
    fixture_commit = reset_workspace(baseline_workspace)
    baseline_load = run_capture(
        ["clojure", "-M", "-e", "(require 'acme.checkout-policy 'acme.checkout-client 'acme.analytics-policy) (println :baseline-valid)"],
        baseline_workspace,
        timeout=120,
    )
    arm_rows: dict[str, Any] = {}
    for arm in ("U", "P"):
        workspace = PREFLIGHT / f"{arm.lower()}-workspace"
        reset_workspace(workspace)
        client = ProxyClient(arm, workspace, PREFLIGHT / f"{arm.lower()}-server.jsonl", PREFLIGHT / f"{arm.lower()}-server.stderr")
        try:
            initialized = client.initialize()
            listed = client.request("tools/list", {})
            inspected = client.request("tools/call", {"name": "inspect_clojure", "arguments": inspect_arguments(workspace)})
            edited = client.request("tools/call", {"name": "edit_clojure", "arguments": edit_arguments(workspace)})
        finally:
            client.close()
        test = verify_fixture(workspace, PREFLIGHT / f"{arm.lower()}-test.json")
        logs = read_jsonl(PREFLIGHT / f"{arm.lower()}-server.jsonl")
        ready = next(row for row in logs if row.get("event") == "proxy_ready")
        inspect_row = next(row for row in logs if row.get("event") == "client_tool_result" and row.get("name") == "inspect_clojure")
        arm_rows[arm] = {
            "offered_tools": sorted(tool.get("name") for tool in listed.get("tools", [])),
            "tool_list_sha256": ready.get("offered_tool_list_sha256"),
            "server_instructions_sha256": ready.get("server_instructions_sha256"),
            "initialize_sha256": sha_bytes(canonical_bytes(initialized)),
            "inspect_is_error": bool(inspected.get("isError")),
            "prepared_emitted": inspect_row.get("prepared_emitted"),
            "edit_is_error": bool(edited.get("isError")),
            "exact": expected_bytes_ok(workspace),
            "unrelated_exact": unrelated_bytes_ok(workspace),
            "test_exit_code": test["exit_code"],
        }
    native_workspace = PREFLIGHT / "native-workspace"
    reset_workspace(native_workspace)
    native = native_patch(native_workspace)
    native_test = verify_fixture(native_workspace, PREFLIGHT / "native-test.json") if native.get("exact") else None
    surface_equal = arm_rows["U"]["tool_list_sha256"] == arm_rows["P"]["tool_list_sha256"]
    server_equal = arm_rows["U"]["server_instructions_sha256"] == arm_rows["P"]["server_instructions_sha256"]
    status_ok = all([
        openai_api_key_absent,
        codex_version["exit_code"] == 0,
        codex_auth["exit_code"] == 0,
        "chatgpt" in (codex_auth["stdout"] + codex_auth["stderr"]).lower(),
        baseline_load["exit_code"] == 0,
        surface_equal,
        server_equal,
        arm_rows["U"]["offered_tools"] == ["edit_clojure", "inspect_clojure"],
        arm_rows["U"]["prepared_emitted"] is False,
        arm_rows["P"]["prepared_emitted"] is True,
        all(not row["inspect_is_error"] and not row["edit_is_error"] and row["exact"] and row["unrelated_exact"] and row["test_exit_code"] == 0 for row in arm_rows.values()),
        native.get("exact") is True,
        native_test is not None and native_test["exit_code"] == 0,
    ])
    receipt = {
        "schema": "prepared-request-replication-preflight.v1",
        "status": "ok" if status_ok else "failed",
        "completed_at_ns": time.time_ns(),
        "freeze_sha256": sha_file(FREEZE),
        "static_hashes": freeze_receipt["static_hashes"],
        "fixture_commit": fixture_commit,
        "baseline_load_exit_code": baseline_load["exit_code"],
        "codex_version": codex_version["stdout"].strip(),
        "codex_auth": (codex_auth["stdout"].strip() or codex_auth["stderr"].strip()),
        "openai_api_key_absent": openai_api_key_absent,
        "subscription_auth_preflight": codex_auth["exit_code"] == 0 and "chatgpt" in (codex_auth["stdout"] + codex_auth["stderr"]).lower(),
        "arm_surface": arm_rows,
        "surface_equal": surface_equal,
        "server_instructions_equal": server_equal,
        "native_patch": native,
        "native_test_exit_code": native_test["exit_code"] if native_test else None,
    }
    atomic_json(PREFLIGHT / "preflight.json", receipt)
    print(json.dumps(receipt, sort_keys=True))
    if not status_ok:
        raise RuntimeError("preflight failed")


def require_preflight() -> dict[str, Any]:
    freeze_receipt = require_freeze()
    path = PREFLIGHT / "preflight.json"
    if not path.exists():
        raise RuntimeError("green preflight is required")
    receipt = json.loads(path.read_text(encoding="utf-8"))
    if receipt.get("status") != "ok" or receipt.get("freeze_sha256") != sha_file(FREEZE):
        raise RuntimeError("preflight/freeze mismatch")
    if receipt.get("static_hashes") != freeze_receipt["static_hashes"]:
        raise RuntimeError("preflight static-hash mismatch")
    return receipt


def codex_argv(arm: str, workspace: Path, run_dir: Path, prompt: str) -> list[str]:
    args_json = json.dumps(proxy_argv(arm, workspace, run_dir / "server.jsonl"), separators=(",", ":"))
    return [
        str(CODEX),
        "exec",
        "--json",
        "--ignore-user-config",
        "--strict-config",
        "-m",
        MODEL,
        "-c",
        'model_reasoning_effort="high"',
        "--dangerously-bypass-approvals-and-sandbox",
        "-C",
        str(workspace.resolve()),
        "-c",
        'mcp_servers.clj-surgeon.command="/usr/bin/env"',
        "-c",
        "mcp_servers.clj-surgeon.args=" + args_json,
        prompt,
    ]


def run_process(argv: list[str], cwd: Path, stdout_path: Path, stderr_path: Path, start_record: dict[str, Any], timeout: int = 360) -> dict[str, Any]:
    started_ns = time.time_ns()
    started = False
    exit_code: int | None = None
    timed_out = False
    with stdout_path.open("w", encoding="utf-8") as out, stderr_path.open("w", encoding="utf-8") as err:
        try:
            process = subprocess.Popen(argv, cwd=cwd, stdout=out, stderr=err, text=True)
            started = True
            append_jsonl(ATTEMPTS, {"event": "process_start", "started_at_ns": started_ns, **start_record})
            try:
                exit_code = process.wait(timeout=timeout)
            except subprocess.TimeoutExpired:
                timed_out = True
                process.terminate()
                try:
                    exit_code = process.wait(timeout=10)
                except subprocess.TimeoutExpired:
                    process.kill()
                    exit_code = process.wait(timeout=10)
        except OSError as exc:
            err.write(f"prelaunch error: {exc}\n")
    return {
        "started": started,
        "started_ns": started_ns,
        "ended_ns": time.time_ns(),
        "exit_code": exit_code,
        "timed_out": timed_out,
    }


def completed_successful_mutation(item: dict[str, Any]) -> str | None:
    if item.get("status") != "completed":
        return None
    if item.get("type") == "mcp_tool_call" and item.get("server") == "clj-surgeon" and item.get("tool") == "edit_clojure" and not item.get("error"):
        return "surgeon_mcp"
    if item.get("type") == "file_change":
        return "native"
    return None


def score_run(run_number: int, arm: str, run_dir: Path, workspace: Path, process: dict[str, Any], preflight_receipt: dict[str, Any]) -> dict[str, Any]:
    events = read_jsonl(run_dir / "events.jsonl")
    server = read_jsonl(run_dir / "server.jsonl")
    action_count = 0
    first_route: str | None = None
    turns_to_mutation: int | None = None
    mutation_families: set[str] = set()
    tool_sequence: list[str] = []
    inspect_started_indexes: list[int] = []
    inspect_completed_indexes: list[int] = []
    first_mutation_index: int | None = None
    refusal_count = 0
    for index, event in enumerate(events):
        item = event.get("item", {})
        kind = item.get("type")
        if event.get("type") == "item.started" and kind in {"mcp_tool_call", "file_change", "command_execution"}:
            action_count += 1
            if kind == "mcp_tool_call":
                label = f"mcp:{item.get('server')}:{item.get('tool')}"
                if item.get("server") == "clj-surgeon" and item.get("tool") == "inspect_clojure":
                    inspect_started_indexes.append(index)
            elif kind == "file_change":
                label = "native:file_change"
            else:
                label = "command"
            tool_sequence.append(label)
        if event.get("type") == "item.completed" and kind == "mcp_tool_call":
            if item.get("server") == "clj-surgeon" and item.get("tool") == "inspect_clojure" and item.get("status") == "completed" and not item.get("error"):
                inspect_completed_indexes.append(index)
            if item.get("error"):
                refusal_count += 1
        if event.get("type") == "item.completed":
            route = completed_successful_mutation(item)
            if route:
                mutation_families.add(route)
                if first_route is None:
                    first_route = route
                    first_mutation_index = index
                    turns_to_mutation = action_count
    ready = next((row for row in server if row.get("event") == "proxy_ready"), {})
    inspect_rows = [row for row in server if row.get("event") == "client_tool_result" and row.get("name") == "inspect_clojure"]
    successful_inspect_rows = [row for row in inspect_rows if not row.get("is_error", True)]
    prepared_observed = len(successful_inspect_rows) == 1 and bool(successful_inspect_rows[0].get("prepared_emitted"))
    exposure_valid = prepared_observed if arm == "P" else len(successful_inspect_rows) == 1 and not prepared_observed
    inspect_before_mutation = (
        len(inspect_started_indexes) == 1
        and len(inspect_completed_indexes) == 1
        and first_mutation_index is not None
        and inspect_completed_indexes[0] < first_mutation_index
    )
    target_exact = expected_bytes_ok(workspace)
    unrelated_exact = unrelated_bytes_ok(workspace)
    test = json.loads((run_dir / "test.json").read_text(encoding="utf-8"))
    semantic_correct = target_exact and test.get("exit_code") == 0
    status = run_capture(["git", "diff", "--name-only"], workspace)
    changed_files = sorted(row for row in status["stdout"].splitlines() if row)
    unallowed_edit_calls = []
    for row in server:
        if row.get("event") != "client_tool_call" or row.get("name") != "edit_clojure":
            continue
        files = {edit.get("file") for edit in row.get("arguments", {}).get("edits", [])}
        if not files.issubset(TARGETS):
            unallowed_edit_calls.append(sorted(str(value) for value in files if value not in TARGETS))
    wrong_subject = (not unrelated_exact) or any(name not in TARGETS for name in changed_files) or bool(unallowed_edit_calls)
    expected_surface = preflight_receipt["arm_surface"][arm]
    environment_valid = all([
        process.get("started"),
        process.get("exit_code") == 0,
        preflight_receipt.get("subscription_auth_preflight"),
        preflight_receipt.get("openai_api_key_absent"),
        ready.get("offered_tool_names") == ["edit_clojure", "inspect_clojure"],
        ready.get("offered_tool_list_sha256") == expected_surface.get("tool_list_sha256"),
        ready.get("server_instructions_sha256") == expected_surface.get("server_instructions_sha256"),
        len(successful_inspect_rows) == 1,
        inspect_before_mutation,
        exposure_valid,
    ])
    usage = next((row.get("usage", {}) for row in reversed(events) if row.get("type") == "turn.completed"), {})
    primary_route = first_route or "none"
    route_adherent = all([
        environment_valid,
        semantic_correct,
        not wrong_subject,
        len(mutation_families) == 1,
        refusal_count == 0,
        primary_route in {"surgeon_mcp", "native"},
    ])
    score = {
        "schema": "prepared-request-replication-score.v1",
        "run": run_number,
        "arm": arm,
        "requested_model": MODEL,
        "reasoning_effort": REASONING,
        "process_started": bool(process.get("started")),
        "process_exit_code": process.get("exit_code"),
        "timed_out": process.get("timed_out"),
        "subscription_auth_preflight": preflight_receipt.get("subscription_auth_preflight"),
        "openai_api_key_absent": preflight_receipt.get("openai_api_key_absent"),
        "environment_valid": environment_valid,
        "semantic_correct": semantic_correct,
        "target_bytes_exact": target_exact,
        "unrelated_bytes_exact": unrelated_exact,
        "wrong_subject": wrong_subject,
        "primary_route": primary_route,
        "route_adherent": route_adherent,
        "mutation_families": sorted(mutation_families),
        "inspect_count": len(inspect_started_indexes),
        "successful_inspect_count": len(successful_inspect_rows),
        "inspect_before_mutation": inspect_before_mutation,
        "prepared_payload_observed": prepared_observed,
        "exposure_valid": exposure_valid,
        "refusal_count": refusal_count,
        "turns_to_success": turns_to_mutation if semantic_correct and first_route else None,
        "tool_sequence": tool_sequence,
        "changed_files": changed_files,
        "unallowed_edit_calls": unallowed_edit_calls,
        "test_exit_code": test.get("exit_code"),
        "complete_wall_ms": (process.get("ended_ns", 0) - process.get("started_ns", 0)) / 1_000_000,
        "input_tokens": usage.get("input_tokens"),
        "cached_input_tokens": usage.get("cached_input_tokens"),
        "output_tokens": usage.get("output_tokens"),
        "reasoning_output_tokens": usage.get("reasoning_output_tokens"),
        "invalid_event_lines": sum("_invalid_json_line" in row for row in events),
    }
    return score


def run_one(run_number: int, arm: str, preflight_receipt: dict[str, Any]) -> dict[str, Any]:
    run_dir = RUNS / f"{run_number:03d}-{arm.lower()}"
    if run_dir.exists():
        raise RuntimeError(f"refusing replacement: {run_dir}")
    run_dir.mkdir(parents=True)
    workspace = WORKSPACES / f"{run_number:03d}-{arm.lower()}"
    fixture_commit = reset_workspace(workspace)
    prompt = render_prompt(workspace)
    argv = codex_argv(arm, workspace, run_dir, prompt)
    launch = {
        "run": run_number,
        "arm": arm,
        "schedule": SCHEDULE,
        "fixture_commit": fixture_commit,
        "cwd": str(workspace.resolve()),
        "prompt_sha256": sha_bytes(prompt.encode("utf-8")),
        "inspect_arguments_sha256": sha_bytes(canonical_bytes(inspect_arguments(workspace))),
        "prepared_arguments_sha256": sha_bytes(canonical_bytes(edit_arguments(workspace))),
        "argv_sha256": sha_bytes(canonical_bytes(argv)),
        "freeze_sha256": sha_file(FREEZE),
        "preflight_sha256": sha_file(PREFLIGHT / "preflight.json"),
    }
    atomic_json(run_dir / "launch.json", launch)
    process = run_process(
        argv,
        workspace,
        run_dir / "events.jsonl",
        run_dir / "stderr.log",
        {"run": run_number, "arm": arm, "argv_sha256": launch["argv_sha256"]},
    )
    atomic_json(run_dir / "process.json", process)
    final_dir = run_dir / "final"
    final_dir.mkdir()
    for relative in sorted(TARGETS | {DISTRACTOR}):
        source = workspace / relative
        if source.exists():
            destination = final_dir / relative
            destination.parent.mkdir(parents=True, exist_ok=True)
            shutil.copy2(source, destination)
    diff = run_capture(["git", "diff", "--binary"], workspace)
    (run_dir / "git.diff").write_text(diff["stdout"], encoding="utf-8")
    test = verify_fixture(workspace, run_dir / "test.json")
    score = score_run(run_number, arm, run_dir, workspace, process, preflight_receipt)
    atomic_json(run_dir / "score.json", score)
    append_jsonl(ATTEMPTS, {"event": "process_complete", **score})
    print(json.dumps({key: score[key] for key in ("run", "arm", "environment_valid", "primary_route", "semantic_correct", "wrong_subject", "output_tokens")}, sort_keys=True), flush=True)
    return score


def cohort() -> None:
    preflight_receipt = require_preflight()
    if ATTEMPTS.exists() or RUNS.exists() or WORKSPACES.exists():
        raise RuntimeError("cohort is one-shot and refuses existing run state")
    RUNS.mkdir()
    WORKSPACES.mkdir()
    for run_number, arm in enumerate(SCHEDULE, start=1):
        run_one(run_number, arm, preflight_receipt)
    summarize()


def wilson(successes: int, total: int, z: float = 1.959963984540054) -> tuple[float, float]:
    if total == 0:
        return (math.nan, math.nan)
    p = successes / total
    denominator = 1 + z * z / total
    centre = (p + z * z / (2 * total)) / denominator
    margin = z * math.sqrt(p * (1 - p) / total + z * z / (4 * total * total)) / denominator
    return (centre - margin, centre + margin)


def newcombe_difference(p_success: int, p_total: int, u_success: int, u_total: int) -> tuple[float, float]:
    p_rate = p_success / p_total
    u_rate = u_success / u_total
    p_low, p_high = wilson(p_success, p_total)
    u_low, u_high = wilson(u_success, u_total)
    difference = p_rate - u_rate
    lower = difference - math.sqrt((p_rate - p_low) ** 2 + (u_high - u_rate) ** 2)
    upper = difference + math.sqrt((p_high - p_rate) ** 2 + (u_rate - u_low) ** 2)
    return (max(-1.0, lower), min(1.0, upper))


def median(values: list[float | int | None]) -> float | None:
    clean = [float(value) for value in values if isinstance(value, (int, float))]
    return statistics.median(clean) if clean else None


def summarize() -> dict[str, Any]:
    require_preflight()
    scores = [json.loads(path.read_text(encoding="utf-8")) for path in sorted(RUNS.glob("*/score.json"))]
    if len(scores) != len(SCHEDULE):
        raise RuntimeError(f"expected {len(SCHEDULE)} scores, found {len(scores)}")
    cells: dict[str, Any] = {}
    for arm in ("U", "P"):
        rows = [row for row in scores if row["arm"] == arm]
        surgeon = sum(row["primary_route"] == "surgeon_mcp" for row in rows)
        cells[arm] = {
            "attempts": len(rows),
            "surgeon_first": surgeon,
            "surgeon_rate": surgeon / len(rows),
            "surgeon_wilson_95": wilson(surgeon, len(rows)),
            "native_first": sum(row["primary_route"] == "native" for row in rows),
            "no_mutation": sum(row["primary_route"] == "none" for row in rows),
            "environment_valid": sum(bool(row["environment_valid"]) for row in rows),
            "semantic_correct": sum(bool(row["semantic_correct"]) for row in rows),
            "route_adherent": sum(bool(row["route_adherent"]) for row in rows),
            "wrong_subject": sum(bool(row["wrong_subject"]) for row in rows),
            "median_turns_to_success": median([row.get("turns_to_success") for row in rows]),
            "median_output_tokens": median([row.get("output_tokens") for row in rows]),
            "median_wall_ms": median([row.get("complete_wall_ms") for row in rows]),
            "output_tokens_total": sum(int(row.get("output_tokens") or 0) for row in rows),
        }
    difference = cells["P"]["surgeon_rate"] - cells["U"]["surgeon_rate"]
    difference_interval = newcombe_difference(cells["P"]["surgeon_first"], 10, cells["U"]["surgeon_first"], 10)
    primary_replicates = difference >= 0.2
    safety_passes = cells["P"]["semantic_correct"] >= 9 and sum(row["wrong_subject"] for row in scores) == 0
    verdict = "replicates-and-safety-passes" if primary_replicates and safety_passes else (
        "routing-replicates-but-safety-fails" if primary_replicates else "screen-does-not-replicate"
    )
    aggregate = {
        "schema": "prepared-request-replication-aggregate.v1",
        "generated_at_ns": time.time_ns(),
        "measured_process_starts": sum(row.get("event") == "process_start" for row in read_jsonl(ATTEMPTS)),
        "completed_scores": len(scores),
        "cells": cells,
        "primary": {
            "p_minus_u_risk_difference": difference,
            "newcombe_95": difference_interval,
            "registered_prediction": 0.4,
            "kill_below": 0.2,
            "replication_gate_passed": primary_replicates,
        },
        "safety_gate_passed": safety_passes,
        "verdict": verdict,
        "inference_limit": "n=10/arm; report interval; no equivalence or population-resolution claim",
        "scores": scores,
    }
    atomic_json(AGGREGATE, aggregate)
    print(json.dumps({"status": "ok", "cells": cells, "primary": aggregate["primary"], "verdict": verdict}, sort_keys=True))
    return aggregate


def archive() -> dict[str, Any]:
    aggregate = summarize()
    ARCHIVES.mkdir(exist_ok=True)
    manifest_path = ROOT / "artifact-manifest.sha256"
    included = []
    excluded_roots = {"archives", "workspaces", "__pycache__"}
    for path in sorted(ROOT.rglob("*")):
        if not path.is_file() or path == manifest_path:
            continue
        relative = path.relative_to(ROOT)
        if relative.parts[0] in excluded_roots or path.suffix == ".pyc":
            continue
        included.append(path)
    manifest_path.write_text("".join(f"{sha_file(path)}  {path.relative_to(ROOT)}\n" for path in included), encoding="utf-8")
    included.append(manifest_path)
    archive_path = ARCHIVES / "prepared-request-replication-20260830.tar.gz"
    with archive_path.open("wb") as raw:
        with gzip.GzipFile(filename="", mode="wb", fileobj=raw, mtime=0) as zipped:
            with tarfile.open(fileobj=zipped, mode="w") as tar:
                for path in sorted(included):
                    relative = path.relative_to(ROOT)
                    info = tar.gettarinfo(str(path), arcname=str(relative))
                    info.uid = info.gid = 0
                    info.uname = info.gname = ""
                    info.mtime = 0
                    with path.open("rb") as handle:
                        tar.addfile(info, handle)
    receipt = {
        "schema": "prepared-request-replication-archive.v1",
        "created_at_ns": time.time_ns(),
        "archive": str(archive_path.relative_to(ROOT)),
        "archive_sha256": sha_file(archive_path),
        "manifest": str(manifest_path.relative_to(ROOT)),
        "manifest_sha256": sha_file(manifest_path),
        "files": len(included),
        "aggregate_sha256": sha_file(AGGREGATE),
        "verdict": aggregate["verdict"],
    }
    atomic_json(ARCHIVES / "archive-receipt.json", receipt)
    print(json.dumps(receipt, sort_keys=True))
    return receipt


def self_test(write_receipt: bool = True) -> None:
    assert len(SCHEDULE) == 20
    assert SCHEDULE.count("U") == 10 and SCHEDULE.count("P") == 10
    assert SCHEDULE == list("UPPUPUUPUPPUPUUPUPPU")
    assert set(EDIT_TEMPLATE) == {"edits"} and len(EDIT_TEMPLATE["edits"]) == 7
    assert {edit["file"] for edit in EDIT_TEMPLATE["edits"]} == TARGETS
    assert len(INSPECT_TEMPLATE["requests"]) == 2
    assert wilson(0, 10)[0] == 0.0
    interval = newcombe_difference(8, 10, 4, 10)
    assert interval[0] < 0.4 < interval[1]
    workspace = ROOT / ".self-test-workspace"
    try:
        reset_workspace(workspace)
        assert not expected_bytes_ok(workspace)
        assert unrelated_bytes_ok(workspace)
        assert "__INSPECT_REQUEST__" not in render_prompt(workspace)
        assert inspect_arguments(workspace)["workspace_root"] == str(workspace.resolve())
        assert (workspace.stat().st_mode & 0o777) == 0o777
        assert ((workspace / "src/acme/checkout_policy.clj").stat().st_mode & 0o666) == 0o666
    finally:
        if workspace.exists():
            shutil.rmtree(workspace)
    receipt = {"schema": "prepared-request-replication-self-test.v1", "status": "ok", "assertions": 14, "completed_at_ns": time.time_ns()}
    if write_receipt:
        atomic_json(ROOT / "self-test.json", receipt)
    print(json.dumps(receipt, sort_keys=True))


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("command", choices=["self-test", "freeze", "preflight", "cohort", "summarize", "archive"])
    args = parser.parse_args()
    if args.command == "self-test":
        self_test()
    elif args.command == "freeze":
        freeze()
    elif args.command == "preflight":
        preflight()
    elif args.command == "cohort":
        cohort()
    elif args.command == "summarize":
        summarize()
    else:
        archive()


if __name__ == "__main__":
    main()
