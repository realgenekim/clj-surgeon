#!/usr/bin/env python3
"""Run the frozen warm-executor economics screen through Codex app-server."""

from __future__ import annotations

import argparse
import hashlib
import json
import math
import os
import platform
import queue
import re
import shutil
import statistics
import subprocess
import sys
import threading
import time
from pathlib import Path
from typing import Any


MODELS = ("gpt-5.3-codex-spark", "gpt-5.6-sol")
MODEL_LABEL = {MODELS[0]: "spark", MODELS[1]: "sol"}
FIXTURE_REL = Path("src/warm_executor/fixture.clj")
METERED_ENV = (
    "OPENAI_API_KEY",
    "OPENAI_BASE_URL",
    "OPENAI_ORG_ID",
    "OPENAI_PROJECT_ID",
)


def now_ns() -> int:
    return time.monotonic_ns()


def wall_ns() -> int:
    return time.time_ns()


def sha256_bytes(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def sha256_file(path: Path) -> str:
    return sha256_bytes(path.read_bytes())


def write_json(path: Path, value: Any) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, indent=2, sort_keys=True) + "\n")


def append_jsonl(path: Path, value: Any) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("a") as stream:
        stream.write(json.dumps(value, sort_keys=True) + "\n")


def clean_env(codex_home: Path) -> dict[str, str]:
    env = dict(os.environ)
    for name in METERED_ENV:
        env.pop(name, None)
    env["CODEX_HOME"] = str(codex_home)
    return env


def alternating_order(index: int) -> tuple[str, str]:
    return MODELS if index % 2 == 1 else tuple(reversed(MODELS))


def load_average() -> list[float] | None:
    try:
        return list(os.getloadavg())
    except OSError:
        return None


def tree_hashes(root: Path) -> dict[str, str]:
    result: dict[str, str] = {}
    for path in sorted(root.rglob("*")):
        if path.is_file() and ".git" not in path.parts:
            result[str(path.relative_to(root))] = sha256_file(path)
    return result


def expected_fixture(template: str, completed: int) -> str:
    result = template
    for index in range(1, completed + 1):
        result = result.replace(
            f"(def slot-{index:02d} {{:slot {index} :state :todo}})",
            f"(def slot-{index:02d} {{:slot {index} :state :done-{index:02d}}})",
        )
    return result


def prepared_arguments(workspace: Path, index: int) -> dict[str, Any]:
    return {
        "workspace_root": str(workspace),
        "edits": [
            {
                "file": str(FIXTURE_REL),
                "within": {"form": f"slot-{index:02d}"},
                "from": ":todo",
                "to": f":done-{index:02d}",
                "matches": 1,
            }
        ],
    }


def prepared_prompt(workspace: Path, index: int) -> str:
    args = json.dumps(prepared_arguments(workspace, index), separators=(",", ":"))
    return (
        f"Prepared bang {index:02d}. Call the clj-surgeon MCP edit_clojure tool "
        f"exactly once with these complete arguments: {args}. Do not inspect, "
        "use shell, call any other tool, or change another subject. After the "
        "verified success, reply with exactly EXACT_OK."
    )


class ProtocolError(RuntimeError):
    pass


class AppServer:
    def __init__(
        self,
        codex: str,
        codex_home: Path,
        run_dir: Path,
        model: str,
        cwd: Path,
    ) -> None:
        self.codex = codex
        self.codex_home = codex_home
        self.run_dir = run_dir
        self.model = model
        self.cwd = cwd
        self.history: list[dict[str, Any]] = []
        self.inbox: queue.Queue[dict[str, Any]] = queue.Queue()
        self.request_id = 0
        self.log_lock = threading.Lock()
        self.protocol_path = run_dir / "protocol.jsonl"
        self.stderr_stream = (run_dir / "app-server.stderr").open("w")
        self.process_start_ns = now_ns()
        command = [
            codex,
            "app-server",
            "--listen",
            "stdio://",
            "-c",
            f'model="{model}"',
            "-c",
            'model_reasoning_effort="low"',
        ]
        write_json(
            run_dir / "app-server-command.json",
            {
                "argv": command,
                "cwd": str(cwd),
                "codex_home": str(codex_home),
                "metered_env_present": [
                    name for name in METERED_ENV if name in clean_env(codex_home)
                ],
            },
        )
        self.process = subprocess.Popen(
            command,
            cwd=cwd,
            env=clean_env(codex_home),
            stdin=subprocess.PIPE,
            stdout=subprocess.PIPE,
            stderr=self.stderr_stream,
            text=True,
            bufsize=1,
        )
        assert self.process.stdout is not None
        self.reader = threading.Thread(target=self._read_stdout, daemon=True)
        self.reader.start()
        init, init_arrival = self.request(
            "initialize",
            {
                "clientInfo": {
                    "name": "clj-surgeon-warm-executor-screen",
                    "version": "1.0.0",
                },
                "capabilities": {"experimentalApi": True},
            },
            timeout=30,
        )
        self.initialize_response = init
        self.initialize_arrival_ns = init_arrival
        self.notify("initialized", {})

    def _write_protocol(self, direction: str, message: Any, mono: int) -> None:
        with self.log_lock:
            append_jsonl(
                self.protocol_path,
                {
                    "direction": direction,
                    "mono_ns": mono,
                    "wall_ns": wall_ns(),
                    "message": message,
                },
            )

    def _read_stdout(self) -> None:
        assert self.process.stdout is not None
        for line in self.process.stdout:
            arrived = now_ns()
            stripped = line.rstrip("\n")
            try:
                message = json.loads(stripped)
            except json.JSONDecodeError:
                message = {"protocol_parse_error": stripped}
            entry = {"mono_ns": arrived, "message": message}
            self._write_protocol("in", message, arrived)
            self.inbox.put(entry)
        self.inbox.put(
            {
                "mono_ns": now_ns(),
                "message": {"process_eof": self.process.poll()},
            }
        )

    def send(self, message: dict[str, Any]) -> int:
        if self.process.poll() is not None:
            raise ProtocolError(f"app-server exited: {self.process.returncode}")
        assert self.process.stdin is not None
        sent = now_ns()
        self._write_protocol("out", message, sent)
        self.process.stdin.write(json.dumps(message, separators=(",", ":")) + "\n")
        self.process.stdin.flush()
        return sent

    def notify(self, method: str, params: dict[str, Any]) -> int:
        return self.send({"method": method, "params": params})

    def receive(self, timeout: float) -> dict[str, Any]:
        try:
            entry = self.inbox.get(timeout=timeout)
        except queue.Empty as error:
            raise ProtocolError(f"timeout waiting for app-server after {timeout}s") from error
        self.history.append(entry)
        if "process_eof" in entry["message"]:
            raise ProtocolError(f"app-server EOF: {entry['message']}")
        return entry

    def drain(self) -> None:
        while True:
            try:
                entry = self.inbox.get_nowait()
            except queue.Empty:
                return
            self.history.append(entry)

    def request(
        self, method: str, params: dict[str, Any], timeout: float = 180
    ) -> tuple[dict[str, Any], int]:
        self.request_id += 1
        request_id = self.request_id
        self.send({"id": request_id, "method": method, "params": params})
        deadline = time.monotonic() + timeout
        while True:
            entry = self.receive(max(0.01, deadline - time.monotonic()))
            message = entry["message"]
            if message.get("id") != request_id:
                continue
            if "error" in message:
                raise ProtocolError(f"{method} failed: {message['error']}")
            return message, entry["mono_ns"]

    def start_thread(self, sandbox: str) -> dict[str, Any]:
        self.drain()
        sent = now_ns()
        response, arrival = self.request(
            "thread/start",
            {
                "model": self.model,
                "cwd": str(self.cwd),
                "sandbox": sandbox,
                "approvalPolicy": "never",
                "ephemeral": True,
                "config": {"model_reasoning_effort": "low"},
                "threadSource": "warm-executor-screen",
            },
            timeout=60,
        )
        result = response.get("result", {})
        thread = result.get("thread", {})
        thread_id = thread.get("id")
        if not thread_id:
            raise ProtocolError(f"thread/start omitted thread id: {response}")
        actual_model = result.get("model")
        if actual_model != self.model:
            raise ProtocolError(
                f"model fallback/refusal: requested {self.model}, got {actual_model}"
            )
        return {
            "thread_id": thread_id,
            "sent_ns": sent,
            "arrival_ns": arrival,
            "thread_setup_ms": (arrival - sent) / 1_000_000,
            "response": response,
        }

    def turn(self, thread_id: str, prompt: str, timeout: float = 180) -> dict[str, Any]:
        self.drain()
        marker = len(self.history)
        dispatch_ns = now_ns()
        response, response_ns = self.request(
            "turn/start",
            {
                "threadId": thread_id,
                "input": [{"type": "text", "text": prompt}],
                "model": self.model,
                "effort": "low",
                "cwd": str(self.cwd),
            },
            timeout=timeout,
        )
        turn = response.get("result", {}).get("turn", {})
        turn_id = turn.get("id")
        if not turn_id:
            raise ProtocolError(f"turn/start omitted turn id: {response}")
        deadline = time.monotonic() + timeout

        def completed_event() -> dict[str, Any] | None:
            for entry in self.history[marker:]:
                message = entry["message"]
                if message.get("method") != "turn/completed":
                    continue
                params = message.get("params", {})
                if params.get("threadId") != thread_id:
                    continue
                if params.get("turn", {}).get("id") == turn_id:
                    return entry
            return None

        completed = completed_event()
        while completed is None:
            self.receive(max(0.01, deadline - time.monotonic()))
            completed = completed_event()
        events = self.history[marker:]
        first_delta = next(
            (
                entry
                for entry in events
                if entry["message"].get("method") == "item/agentMessage/delta"
                and entry["message"].get("params", {}).get("turnId") == turn_id
            ),
            None,
        )
        first_delta_ns = first_delta["mono_ns"] if first_delta else None
        completed_ns = completed["mono_ns"]
        return {
            "thread_id": thread_id,
            "turn_id": turn_id,
            "dispatch_ns": dispatch_ns,
            "response_ns": response_ns,
            "completed_ns": completed_ns,
            "first_delta_ns": first_delta_ns,
            "turn_e2e_ms": (completed_ns - dispatch_ns) / 1_000_000,
            "request_ack_ms": (response_ns - dispatch_ns) / 1_000_000,
            "request_to_first_token_ms": (
                (first_delta_ns - dispatch_ns) / 1_000_000
                if first_delta_ns is not None
                else None
            ),
            "decode_tail_ms": (
                (completed_ns - first_delta_ns) / 1_000_000
                if first_delta_ns is not None
                else None
            ),
            "events": events,
            "response": response,
        }

    def stop(self) -> None:
        if self.process.poll() is None:
            if self.process.stdin is not None:
                self.process.stdin.close()
            try:
                self.process.wait(timeout=3)
            except subprocess.TimeoutExpired:
                self.process.terminate()
                try:
                    self.process.wait(timeout=3)
                except subprocess.TimeoutExpired:
                    self.process.kill()
                    self.process.wait(timeout=3)
        self.stderr_stream.close()


class McpServer:
    def __init__(self, repo: Path, workspace: Path, run_dir: Path, run_id: str) -> None:
        self.repo = repo
        self.workspace = workspace
        self.run_dir = run_dir
        ready_file = run_dir / "mcp-ready.edn"
        telemetry = run_dir / "mcp-telemetry"
        telemetry.mkdir(parents=True, exist_ok=True)
        command = [
            "clojure",
            "-J-Xms64m",
            "-J-Xmx512m",
            "-X:clj-surgeon/mcp",
            ":tool-profile",
            ":edit",
            ":project-dir",
            json.dumps(str(workspace)),
            ":telemetry",
            ":full",
            ":telemetry-dir",
            json.dumps(str(telemetry)),
            ":run-id",
            json.dumps(run_id),
            ":nrepl-port",
            ":none",
            ":port",
            "0",
            ":ready-file",
            json.dumps(str(ready_file)),
        ]
        write_json(run_dir / "mcp-command.json", {"argv": command, "cwd": str(repo)})
        self.stdout = (run_dir / "mcp-server.stdout").open("w")
        self.stderr = (run_dir / "mcp-server.stderr").open("w")
        self.process = subprocess.Popen(
            command, cwd=repo, stdout=self.stdout, stderr=self.stderr, text=True
        )
        deadline = time.monotonic() + 90
        while time.monotonic() < deadline:
            if ready_file.exists() and ready_file.stat().st_size:
                break
            if self.process.poll() is not None:
                raise RuntimeError(f"MCP exited before readiness: {self.process.returncode}")
            time.sleep(0.1)
        else:
            raise RuntimeError("MCP readiness timeout")
        match = re.search(r':url\s+"([^"]+)"', ready_file.read_text())
        if not match:
            raise RuntimeError(f"MCP ready receipt omitted URL: {ready_file.read_text()}")
        self.url = match.group(1)

    def stop(self) -> None:
        if self.process.poll() is None:
            self.process.terminate()
            try:
                self.process.wait(timeout=5)
            except subprocess.TimeoutExpired:
                self.process.kill()
                self.process.wait(timeout=3)
        self.stdout.close()
        self.stderr.close()


def make_codex_home(root: Path, auth_file: Path, mcp_url: str | None) -> Path:
    root.mkdir(parents=True, exist_ok=True)
    auth_link = root / "auth.json"
    if not auth_link.exists():
        auth_link.symlink_to(auth_file)
    config = ""
    if mcp_url:
        config = (
            "[mcp_servers.clj-surgeon]\n"
            f'url = "{mcp_url}"\n'
            "required = true\n"
            'enabled_tools = ["edit_clojure"]\n'
            'default_tools_approval_mode = "writes"\n'
            "startup_timeout_sec = 10\n"
            "tool_timeout_sec = 90\n"
        )
    (root / "config.toml").write_text(config)
    return root


def prepare_workspace(template_dir: Path, workspace: Path) -> None:
    workspace.mkdir(parents=True, exist_ok=True)
    shutil.copy2(template_dir / "AGENTS.md", workspace / "AGENTS.md")
    target = workspace / FIXTURE_REL
    target.parent.mkdir(parents=True, exist_ok=True)
    shutil.copy2(template_dir / FIXTURE_REL, target)


def reset_fixture(template_dir: Path, workspace: Path) -> None:
    shutil.copy2(template_dir / FIXTURE_REL, workspace / FIXTURE_REL)


def message_method(entry: dict[str, Any]) -> str | None:
    return entry.get("message", {}).get("method")


def turn_items(turn: dict[str, Any], method: str) -> list[dict[str, Any]]:
    return [
        entry["message"].get("params", {}).get("item", {})
        for entry in turn["events"]
        if message_method(entry) == method
        and entry["message"].get("params", {}).get("turnId") == turn["turn_id"]
    ]


def final_text(turn: dict[str, Any]) -> str:
    return "".join(
        entry["message"].get("params", {}).get("delta", "")
        for entry in turn["events"]
        if message_method(entry) == "item/agentMessage/delta"
        and entry["message"].get("params", {}).get("turnId") == turn["turn_id"]
    ).strip()


def score_prepared_turn(
    turn: dict[str, Any],
    workspace: Path,
    index: int,
    template_text: str,
    before: dict[str, str],
    after: dict[str, str],
) -> dict[str, Any]:
    started = turn_items(turn, "item/started")
    completed = turn_items(turn, "item/completed")
    started_mcp = [item for item in started if item.get("type") == "mcpToolCall"]
    completed_mcp = [item for item in completed if item.get("type") == "mcpToolCall"]
    expected_args = prepared_arguments(workspace, index)
    action_types = {
        "commandExecution",
        "fileChange",
        "dynamicToolCall",
        "collabAgentToolCall",
        "mcpToolCall",
    }
    action_started = [item for item in started if item.get("type") in action_types]
    tool_args_exact = (
        len(started_mcp) == 1
        and started_mcp[0].get("server") == "clj-surgeon"
        and started_mcp[0].get("tool") == "edit_clojure"
        and started_mcp[0].get("arguments") == expected_args
    )
    tool_success = (
        len(completed_mcp) == 1
        and completed_mcp[0].get("server") == "clj-surgeon"
        and completed_mcp[0].get("tool") == "edit_clojure"
        and completed_mcp[0].get("status") == "completed"
        and not completed_mcp[0].get("error")
    )
    expected_text = expected_fixture(template_text, index)
    actual_text = (workspace / FIXTURE_REL).read_text()
    target_rel = str(FIXTURE_REL)
    unexpected_paths = sorted(
        path
        for path in set(before) | set(after)
        if path != target_rel and before.get(path) != after.get(path)
    )
    target_changed = before.get(target_rel) != after.get(target_rel)
    source_exact = actual_text == expected_text
    wrong_subject = bool(unexpected_paths) or (target_changed and not source_exact)
    exact = bool(tool_args_exact and tool_success and source_exact and not unexpected_paths)
    one_shot = bool(
        exact
        and len(action_started) == 1
        and len(started_mcp) == 1
        and len(completed_mcp) == 1
    )
    return {
        "exact": exact,
        "one_shot": one_shot,
        "wrong_subject": wrong_subject,
        "tool_arguments_exact": tool_args_exact,
        "tool_success": tool_success,
        "source_exact": source_exact,
        "terminal_exact": final_text(turn) == "EXACT_OK",
        "final_text": final_text(turn),
        "started_action_types": [item.get("type") for item in action_started],
        "mcp_started": len(started_mcp),
        "mcp_completed": len(completed_mcp),
        "unexpected_changed_paths": unexpected_paths,
        "before_hashes": before,
        "after_hashes": after,
    }


def public_turn_timing(turn: dict[str, Any]) -> dict[str, Any]:
    return {
        key: turn[key]
        for key in (
            "thread_id",
            "turn_id",
            "dispatch_ns",
            "response_ns",
            "completed_ns",
            "first_delta_ns",
            "turn_e2e_ms",
            "request_ack_ms",
            "request_to_first_token_ms",
            "decode_tail_ms",
        )
    }


def median(rows: list[dict[str, Any]], key: str) -> float | None:
    values = [float(row[key]) for row in rows if row.get(key) is not None]
    return statistics.median(values) if values else None


def summarize(results: dict[str, Any]) -> dict[str, Any]:
    summary: dict[str, Any] = {"models": {}}
    for model in MODELS:
        cold = [row for row in results["cold_trivial"] if row["model"] == model]
        cold_prepared = [
            row for row in results["cold_prepared"] if row["model"] == model
        ]
        warm = [row for row in results["warm_prepared"] if row["model"] == model]
        gross = median(cold, "total_e2e_ms")
        cold_bang = median(cold_prepared, "total_e2e_ms")
        warm_bang = median(warm, "turn_e2e_ms")
        advantage = (
            cold_bang - warm_bang
            if cold_bang is not None and warm_bang is not None
            else None
        )
        first = warm[:5]
        last = warm[5:10]
        exact_total = sum(bool(row["score"]["exact"]) for row in warm)
        one_shot_total = sum(bool(row["score"]["one_shot"]) for row in warm)
        wrong_total = sum(bool(row["score"]["wrong_subject"]) for row in warm)
        model_summary = {
            "cold_trivial_n": len(cold),
            "cold_trivial_median_e2e_ms": gross,
            "cold_process_bootstrap_median_ms": median(cold, "process_bootstrap_ms"),
            "cold_thread_setup_median_ms": median(cold, "thread_setup_ms"),
            "cold_request_to_first_token_median_ms": median(
                cold, "request_to_first_token_ms"
            ),
            "cold_decode_tail_median_ms": median(cold, "decode_tail_ms"),
            "cold_prepared_n": len(cold_prepared),
            "cold_prepared_median_e2e_ms": cold_bang,
            "warm_prepared_n": len(warm),
            "warm_prepared_median_round_trip_ms": warm_bang,
            "matched_cold_minus_warm_ms": advantage,
            "amortization_edits_at_2s_savings": (
                math.ceil(gross / 2000) if gross is not None else None
            ),
            "amortization_edits_at_1s_savings": (
                math.ceil(gross / 1000) if gross is not None else None
            ),
            "exact": exact_total,
            "one_shot": one_shot_total,
            "wrong_subject": wrong_total,
            "first_five_exact": sum(bool(row["score"]["exact"]) for row in first),
            "last_five_exact": sum(bool(row["score"]["exact"]) for row in last),
        }
        model_summary["drift_signal"] = (
            model_summary["last_five_exact"]
            <= model_summary["first_five_exact"] - 2
        )
        materiality = 1000 if model == MODELS[0] else 500
        reliability_gate = (
            wrong_total == 0
            and exact_total >= (9 if model == MODELS[0] else 10)
            and not model_summary["drift_signal"]
        )
        model_summary["economic_gate"] = bool(
            advantage is not None and advantage >= materiality
        )
        model_summary["reliability_gate"] = reliability_gate
        summary["models"][model] = model_summary
    spark = summary["models"][MODELS[0]]
    summary["winning_pattern"] = bool(
        spark["economic_gate"]
        and spark["reliability_gate"]
        and spark["amortization_edits_at_1s_savings"] <= 4
    )
    return summary


def render_summary(summary: dict[str, Any]) -> str:
    lines = [
        "# Warm-executor economics screen",
        "",
        "Persistent mechanism: one live `codex app-server` process and one live "
        "thread per model, with repeated `turn/start` requests.",
        "",
        "| Model | cold trivial E2E | cold prepared bang | warm prepared bang | cold-warm | amortization (2s..1s) | exact | one-shot | wrong-subject |",
        "|---|---:|---:|---:|---:|---:|---:|---:|---:|",
    ]
    for model in MODELS:
        row = summary["models"][model]
        lines.append(
            f"| `{model}` | {row['cold_trivial_median_e2e_ms']:.1f} ms | "
            f"{row['cold_prepared_median_e2e_ms']:.1f} ms | "
            f"{row['warm_prepared_median_round_trip_ms']:.1f} ms | "
            f"{row['matched_cold_minus_warm_ms']:.1f} ms | "
            f"{row['amortization_edits_at_2s_savings']}..{row['amortization_edits_at_1s_savings']} edits | "
            f"{row['exact']}/{row['warm_prepared_n']} | "
            f"{row['one_shot']}/{row['warm_prepared_n']} | {row['wrong_subject']} |"
        )
    lines.extend(["", "## Cold-start decomposition", ""])
    for model in MODELS:
        row = summary["models"][model]
        lines.append(
            f"- `{model}` medians: bootstrap {row['cold_process_bootstrap_median_ms']:.1f} ms; "
            f"thread setup {row['cold_thread_setup_median_ms']:.1f} ms; "
            f"request-to-first-token {row['cold_request_to_first_token_median_ms']:.1f} ms; "
            f"decode tail {row['cold_decode_tail_median_ms']:.1f} ms."
        )
    lines.extend(["", "## Reliability drift", ""])
    for model in MODELS:
        row = summary["models"][model]
        lines.append(
            f"- `{model}`: first-five exact {row['first_five_exact']}/5; "
            f"last-five exact {row['last_five_exact']}/5; drift signal "
            f"`{str(row['drift_signal']).lower()}`."
        )
    lines.extend(
        [
            "",
            f"Registered winning-pattern gate: **{str(summary['winning_pattern']).upper()}**.",
            "",
            "Provider auth, queueing, model materialization, prefix processing, and "
            "first-token decode remain bundled in request-to-first-token; the local "
            "protocol does not expose a defensible finer split.",
            "",
        ]
    )
    return "\n".join(lines)


def git_output(repo: Path, *args: str) -> str:
    return subprocess.check_output(["git", *args], cwd=repo, text=True).strip()


def validate_prerun(repo: Path, auth_file: Path, codex: str) -> dict[str, Any]:
    status = git_output(repo, "status", "--porcelain")
    if status:
        raise RuntimeError(f"worktree must be clean before measurement:\n{status}")
    if not auth_file.is_file():
        raise RuntimeError(f"exact auth file is absent: {auth_file}")
    probe_home = repo / ".warm-executor-auth-probe"
    if probe_home.exists():
        raise RuntimeError(f"auth probe path already exists: {probe_home}")
    try:
        make_codex_home(probe_home, auth_file, None)
        login = subprocess.run(
            [codex, "login", "status"],
            env=clean_env(probe_home),
            cwd=repo,
            text=True,
            capture_output=True,
            check=False,
        )
        auth_status = (login.stdout + login.stderr).strip()
        if login.returncode != 0 or "Logged in using ChatGPT" not in auth_status:
            raise RuntimeError(f"subscription auth proof failed: {auth_status}")
    finally:
        if probe_home.exists():
            shutil.rmtree(probe_home)
    return {
        "head": git_output(repo, "rev-parse", "HEAD"),
        "tree": git_output(repo, "rev-parse", "HEAD^{tree}"),
        "branch": git_output(repo, "branch", "--show-current"),
        "auth_status": auth_status,
        "parent_metered_env_present": [name for name in METERED_ENV if os.getenv(name)],
        "child_metered_env_present": [],
    }


def run(args: argparse.Namespace) -> int:
    repo = Path(__file__).resolve().parents[1]
    out = Path(args.out).resolve()
    if out.exists():
        raise RuntimeError(f"result directory already exists: {out}")
    auth_file = Path(
        args.auth_file or os.getenv("WARM_EXECUTOR_AUTH_FILE") or Path.home() / ".codex/auth.json"
    ).resolve()
    codex = shutil.which("codex")
    if not codex:
        raise RuntimeError("codex is not on PATH")
    prereg = validate_prerun(repo, auth_file, codex)
    out.mkdir(parents=True)
    template_dir = repo / "bench/fixtures/warm_executor"
    template_text = (template_dir / FIXTURE_REL).read_text()
    meta = {
        "schema": "clj-surgeon.warm-executor-screen/v1",
        "preregistration": prereg,
        "codex_version": subprocess.check_output([codex, "--version"], text=True).strip(),
        "models": list(MODELS),
        "reasoning": "low",
        "cold_replicates": 5,
        "warm_bangs": 10,
        "mechanism": "persistent codex app-server stdio; one process and thread per warm model",
        "fixture_sha256": sha256_file(template_dir / FIXTURE_REL),
        "runner_sha256": sha256_file(Path(__file__)),
        "host": platform.node(),
        "platform": platform.platform(),
        "python": sys.version,
        "start_wall_ns": wall_ns(),
        "load_average": load_average(),
    }
    write_json(out / "meta.json", meta)
    results: dict[str, list[dict[str, Any]]] = {
        "cold_trivial": [],
        "cold_prepared": [],
        "warm_prepared": [],
        "warmup": [],
    }

    # Cold trivial: fresh process and fresh ephemeral thread for every trial.
    for replicate in range(1, 6):
        for model in alternating_order(replicate):
            label = MODEL_LABEL[model]
            trial_dir = out / "cold-trivial" / f"r{replicate:02d}-{label}"
            workspace = trial_dir / "workspace"
            workspace.mkdir(parents=True)
            home = make_codex_home(trial_dir / "codex-home", auth_file, None)
            app = AppServer(codex, home, trial_dir, model, workspace)
            try:
                thread = app.start_thread("read-only")
                turn = app.turn(
                    thread["thread_id"],
                    "Do not use tools. Reply with exactly COLD_OK.",
                )
                row = {
                    "model": model,
                    "replicate": replicate,
                    "thread_id": thread["thread_id"],
                    "turn_id": turn["turn_id"],
                    "process_bootstrap_ms": (
                        app.initialize_arrival_ns - app.process_start_ns
                    )
                    / 1_000_000,
                    "thread_setup_ms": thread["thread_setup_ms"],
                    "request_to_first_token_ms": turn["request_to_first_token_ms"],
                    "decode_tail_ms": turn["decode_tail_ms"],
                    "turn_e2e_ms": turn["turn_e2e_ms"],
                    "total_e2e_ms": (
                        turn["completed_ns"] - app.process_start_ns
                    )
                    / 1_000_000,
                    "final_text": final_text(turn),
                    "valid": final_text(turn) == "COLD_OK",
                    "load_average": load_average(),
                }
                write_json(trial_dir / "score.json", row)
                results["cold_trivial"].append(row)
                append_jsonl(out / "cold-trivial.jsonl", row)
            finally:
                app.stop()

    # Matched cold prepared bangs: persistent isolated MCP, fresh Codex process.
    cold_states: dict[str, dict[str, Any]] = {}
    try:
        for model in MODELS:
            label = MODEL_LABEL[model]
            root = out / "cold-prepared" / label
            workspace = root / "workspace"
            prepare_workspace(template_dir, workspace)
            mcp = McpServer(repo, workspace, root / "mcp", f"cold-prepared-{label}")
            cold_states[model] = {"root": root, "workspace": workspace, "mcp": mcp}
        for replicate in range(1, 6):
            for model in alternating_order(replicate):
                label = MODEL_LABEL[model]
                state = cold_states[model]
                workspace = state["workspace"]
                reset_fixture(template_dir, workspace)
                trial_dir = state["root"] / f"r{replicate:02d}"
                home = make_codex_home(
                    trial_dir / "codex-home", auth_file, state["mcp"].url
                )
                before = tree_hashes(workspace)
                app = AppServer(codex, home, trial_dir, model, workspace)
                try:
                    thread = app.start_thread("workspace-write")
                    turn = app.turn(thread["thread_id"], prepared_prompt(workspace, 1))
                    after = tree_hashes(workspace)
                    score = score_prepared_turn(
                        turn, workspace, 1, template_text, before, after
                    )
                    row = {
                        "model": model,
                        "replicate": replicate,
                        "process_bootstrap_ms": (
                            app.initialize_arrival_ns - app.process_start_ns
                        )
                        / 1_000_000,
                        "thread_setup_ms": thread["thread_setup_ms"],
                        "total_e2e_ms": (
                            turn["completed_ns"] - app.process_start_ns
                        )
                        / 1_000_000,
                        **public_turn_timing(turn),
                        "score": score,
                        "load_average": load_average(),
                    }
                    write_json(trial_dir / "score.json", row)
                    (trial_dir / "after.clj").write_text((workspace / FIXTURE_REL).read_text())
                    results["cold_prepared"].append(row)
                    append_jsonl(out / "cold-prepared.jsonl", row)
                finally:
                    app.stop()
    finally:
        for state in cold_states.values():
            state["mcp"].stop()

    # True warm path: two persistent app-server processes, one thread/model.
    warm_states: dict[str, dict[str, Any]] = {}
    try:
        for model in MODELS:
            label = MODEL_LABEL[model]
            root = out / "warm" / label
            workspace = root / "workspace"
            prepare_workspace(template_dir, workspace)
            mcp = McpServer(repo, workspace, root / "mcp", f"warm-{label}")
            home = make_codex_home(root / "codex-home", auth_file, mcp.url)
            app = AppServer(codex, home, root / "app", model, workspace)
            thread = app.start_thread("workspace-write")
            seed = app.turn(
                thread["thread_id"],
                "Warm this persistent session without using tools. Reply exactly WARM_READY.",
            )
            seed_row = {
                "model": model,
                **public_turn_timing(seed),
                "final_text": final_text(seed),
                "valid": final_text(seed) == "WARM_READY",
            }
            write_json(root / "warmup.json", seed_row)
            results["warmup"].append(seed_row)
            warm_states[model] = {
                "root": root,
                "workspace": workspace,
                "mcp": mcp,
                "app": app,
                "thread": thread,
                "alive": True,
            }
        for index in range(1, 11):
            for model in alternating_order(index):
                state = warm_states[model]
                if not state["alive"]:
                    continue
                workspace = state["workspace"]
                before = tree_hashes(workspace)
                turn = state["app"].turn(
                    state["thread"]["thread_id"], prepared_prompt(workspace, index)
                )
                after = tree_hashes(workspace)
                score = score_prepared_turn(
                    turn, workspace, index, template_text, before, after
                )
                row = {
                    "model": model,
                    "bang": index,
                    **public_turn_timing(turn),
                    "score": score,
                    "load_average": load_average(),
                }
                bang_dir = state["root"] / "bangs" / f"{index:02d}"
                write_json(bang_dir / "score.json", row)
                (bang_dir / "after.clj").write_text((workspace / FIXTURE_REL).read_text())
                results["warm_prepared"].append(row)
                append_jsonl(out / "warm-prepared.jsonl", row)
                if score["wrong_subject"]:
                    state["alive"] = False
                    write_json(
                        state["root"] / "KILLED.json",
                        {"bang": index, "reason": "wrong-subject", "score": score},
                    )
    finally:
        for state in warm_states.values():
            state["app"].stop()
            state["mcp"].stop()

    write_json(out / "results.json", results)
    summary = summarize(results)
    write_json(out / "summary.json", summary)
    (out / "SUMMARY.md").write_text(render_summary(summary))
    meta["end_wall_ns"] = wall_ns()
    meta["end_load_average"] = load_average()
    write_json(out / "meta.json", meta)
    print(render_summary(summary))
    return 0


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--out", required=True)
    parser.add_argument("--auth-file")
    args = parser.parse_args()
    return run(args)


if __name__ == "__main__":
    raise SystemExit(main())
