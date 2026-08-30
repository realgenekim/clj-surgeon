#!/Users/genekim/anaconda3/bin/python
"""Run and score the preregistered multi-site mutation head-to-head."""

from __future__ import annotations

import argparse
import hashlib
import itertools
import json
import os
import re
import select
import shutil
import signal
import subprocess
import sys
import tempfile
import time
import urllib.request
from pathlib import Path
from statistics import median
from typing import Any

import tiktoken
from scipy.optimize import brentq
from scipy.stats import nct, t


ROOT = Path(__file__).resolve().parent
REPO = ROOT.parents[2]
SERVER_BASE = "c55de2279826af5ed21c90981591479dd2e802b2"
MODEL = "gpt-5.6-sol"
REASONING = "high"
CODEX = Path(shutil.which("codex") or "/missing/codex").resolve()
CLOJURE = Path(shutil.which("clojure") or "/missing/clojure").resolve()
AUTH = Path("/Users/genekim/.codex/auth.json")
MODEL_CACHE = Path("/Users/genekim/.codex/models_cache.json")
BEFORE = ROOT / "fixture" / "before"
EXPECTED = ROOT / "fixture" / "after" / "src" / "acme" / "retry_policy.clj"
SOURCE_REL = Path("src/acme/retry_policy.clj")
TASK_TEMPLATE = ROOT / "task-prompt.txt"
SYSTEM_PROMPT = ROOT / "system-prompt.txt"
ORACLE = ROOT / "oracle-edit-arguments.json"
HOOK = ROOT / "capture_tool_hook.py"
PREREG = ROOT / "preregistration.md"
PREFLIGHT = ROOT / "preflight"
RUNS = ROOT / "runs"
NATIVE_CATALOG = ROOT / "model-catalog-native.json"
SURGEON_CATALOG = ROOT / "model-catalog-surgeon.json"
EFFECTIVE_PROMPT = ROOT / "effective-task-prompt.txt"
SCHEDULE = ["N", "S", "S", "N", "S", "N", "N", "S", "N", "S", "S", "N"]
FIXED_GIT_DATE = "2026-08-30T00:00:00Z"
PROCESS_TIMEOUT_SECONDS = 300


def canonical_json(value: Any) -> str:
    return json.dumps(value, ensure_ascii=False, separators=(",", ":"))


def sha_bytes(value: bytes) -> str:
    return hashlib.sha256(value).hexdigest()


def sha_file(path: Path) -> str:
    return sha_bytes(path.read_bytes())


def write_json(path: Path, value: Any) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    stage = path.with_suffix(path.suffix + ".tmp")
    stage.write_text(
        json.dumps(value, indent=2, sort_keys=True, ensure_ascii=False) + "\n",
        encoding="utf-8",
    )
    stage.replace(path)


def run_capture(
    argv: list[str], cwd: Path, env: dict[str, str] | None = None, timeout: int = 120
) -> dict[str, Any]:
    started_ns = time.time_ns()
    try:
        completed = subprocess.run(
            argv,
            cwd=cwd,
            env=env,
            text=True,
            capture_output=True,
            timeout=timeout,
            check=False,
        )
        return {
            "argv": argv,
            "started_ns": started_ns,
            "ended_ns": time.time_ns(),
            "exit_code": completed.returncode,
            "timed_out": False,
            "stdout": completed.stdout,
            "stderr": completed.stderr,
        }
    except subprocess.TimeoutExpired as exc:
        return {
            "argv": argv,
            "started_ns": started_ns,
            "ended_ns": time.time_ns(),
            "exit_code": None,
            "timed_out": True,
            "stdout": exc.stdout or "",
            "stderr": exc.stderr or "",
        }


def clean_child_env() -> dict[str, str]:
    return {key: value for key, value in os.environ.items() if not key.startswith("OPENAI_")}


def effective_prompt() -> str:
    source = (BEFORE / SOURCE_REL).read_text(encoding="utf-8").rstrip("\n")
    template = TASK_TEMPLATE.read_text(encoding="utf-8")
    if template.count("{{SOURCE}}") != 1:
        raise RuntimeError("task prompt must contain one {{SOURCE}} marker")
    return template.replace("{{SOURCE}}", source)


def materialize_workspace(destination: Path) -> str:
    if destination.exists():
        raise RuntimeError(f"refusing to replace existing workspace: {destination}")
    shutil.copytree(BEFORE, destination)
    env = clean_child_env()
    env.update({"GIT_AUTHOR_DATE": FIXED_GIT_DATE, "GIT_COMMITTER_DATE": FIXED_GIT_DATE})
    commands = [
        ["git", "init", "-q"],
        ["git", "add", "."],
        [
            "git",
            "-c",
            "user.name=fixture",
            "-c",
            "user.email=fixture@invalid",
            "commit",
            "-q",
            "-m",
            "frozen multisite fixture",
        ],
    ]
    for command in commands:
        result = run_capture(command, destination, env=env)
        if result["exit_code"] != 0:
            raise RuntimeError(f"fixture git command failed: {result}")
    return run_capture(["git", "rev-parse", "HEAD"], destination)["stdout"].strip()


def source_unchanged_from_base() -> bool:
    result = run_capture(
        ["git", "diff", "--quiet", SERVER_BASE, "--", "src", "deps.edn", "resources"],
        REPO,
    )
    return result["exit_code"] == 0


def write_model_catalogs() -> dict[str, Any]:
    cache = json.loads(MODEL_CACHE.read_text(encoding="utf-8"))
    matches = [entry for entry in cache.get("models", []) if entry.get("slug") == MODEL]
    if len(matches) != 1:
        raise RuntimeError(f"expected one {MODEL} model object, found {len(matches)}")
    native_model = json.loads(json.dumps(matches[0]))
    if native_model.get("apply_patch_tool_type") != "freeform":
        raise RuntimeError("native model object does not advertise freeform apply_patch")
    surgeon_model = json.loads(json.dumps(native_model))
    surgeon_model["apply_patch_tool_type"] = None
    write_json(NATIVE_CATALOG, {"models": [native_model]})
    write_json(SURGEON_CATALOG, {"models": [surgeon_model]})
    differing = [
        key
        for key in sorted(set(native_model) | set(surgeon_model))
        if native_model.get(key) != surgeon_model.get(key)
    ]
    if differing != ["apply_patch_tool_type"]:
        raise RuntimeError(f"arm model objects differ at unexpected fields: {differing}")
    return {
        "source_cache_sha256": sha_file(MODEL_CACHE),
        "native_catalog_sha256": sha_file(NATIVE_CATALOG),
        "surgeon_catalog_sha256": sha_file(SURGEON_CATALOG),
        "differing_fields": differing,
    }


class StdioMcpClient:
    def __init__(self, workspace: Path, receipt_dir: Path) -> None:
        receipt_dir.mkdir(parents=True, exist_ok=True)
        self.stderr_handle = (receipt_dir / "mcp-stdio.stderr").open("w", encoding="utf-8")
        argv = [
            str(CLOJURE),
            "-J-Xms32m",
            "-J-Xmx768m",
            "-X:clj-surgeon/mcp-stdio",
            ":project-dir",
            json.dumps(str(workspace)),
            ":telemetry",
            ":full",
            ":telemetry-dir",
            json.dumps(str(receipt_dir / "mcp-telemetry")),
            ":run-id",
            json.dumps("multisite-preflight"),
            ":nrepl-port",
            ":none",
        ]
        self.process = subprocess.Popen(
            argv,
            cwd=REPO,
            env=clean_child_env(),
            stdin=subprocess.PIPE,
            stdout=subprocess.PIPE,
            stderr=self.stderr_handle,
            text=True,
            bufsize=1,
        )
        self.request_id = 1
        self.log_path = receipt_dir / "mcp-stdio.jsonl"

    def request(self, method: str, params: dict[str, Any]) -> dict[str, Any]:
        request_id = self.request_id
        self.request_id += 1
        request = {"jsonrpc": "2.0", "id": request_id, "method": method, "params": params}
        assert self.process.stdin is not None and self.process.stdout is not None
        with self.log_path.open("a", encoding="utf-8") as handle:
            handle.write(canonical_json({"direction": "request", "payload": request}) + "\n")
        self.process.stdin.write(canonical_json(request) + "\n")
        self.process.stdin.flush()
        ready, _, _ = select.select([self.process.stdout], [], [], 45)
        if not ready:
            raise TimeoutError(method)
        line = self.process.stdout.readline()
        response = json.loads(line)
        with self.log_path.open("a", encoding="utf-8") as handle:
            handle.write(canonical_json({"direction": "response", "payload": response}) + "\n")
        if response.get("error") is not None:
            raise RuntimeError(response["error"])
        return response["result"]

    def initialize(self) -> dict[str, Any]:
        result = self.request(
            "initialize",
            {
                "protocolVersion": "2025-03-26",
                "capabilities": {},
                "clientInfo": {"name": "multisite-preflight", "version": "1"},
            },
        )
        assert self.process.stdin is not None
        notification = {"jsonrpc": "2.0", "method": "notifications/initialized", "params": {}}
        self.process.stdin.write(canonical_json(notification) + "\n")
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


def frozen_input_hashes() -> dict[str, str]:
    paths = [PREREG, TASK_TEMPLATE, SYSTEM_PROMPT, ORACLE, HOOK, Path(__file__).resolve()]
    paths.extend(sorted(path for path in BEFORE.rglob("*") if path.is_file()))
    paths.extend(sorted(path for path in (ROOT / "fixture" / "after").rglob("*") if path.is_file()))
    paths.extend([NATIVE_CATALOG, SURGEON_CATALOG, EFFECTIVE_PROMPT])
    return {str(path.relative_to(ROOT)): sha_file(path) for path in paths}


def preflight() -> None:
    if PREFLIGHT.exists():
        raise RuntimeError(f"refusing to replace preflight receipt: {PREFLIGHT}")
    if not source_unchanged_from_base():
        raise RuntimeError("server source, deps.edn, or resources differ from registered base")
    if not AUTH.is_file():
        raise RuntimeError("ChatGPT auth receipt is unavailable")
    catalog_receipt = write_model_catalogs()
    EFFECTIVE_PROMPT.write_text(effective_prompt(), encoding="utf-8")
    workspace = PREFLIGHT / "workspace"
    fixture_commit = materialize_workspace(workspace)
    before_sha = sha_file(workspace / SOURCE_REL)
    client = StdioMcpClient(workspace, PREFLIGHT)
    try:
        initialized = client.initialize()
        listed = client.request("tools/list", {})
        names = sorted(tool["name"] for tool in listed.get("tools", []))
        if "edit_clojure" not in names:
            raise RuntimeError(f"edit_clojure missing from isolated server: {names}")
        arguments = {"workspace_root": str(workspace)}
        arguments.update(json.loads(ORACLE.read_text(encoding="utf-8")))
        edited = client.request("tools/call", {"name": "edit_clojure", "arguments": arguments})
    finally:
        client.close()
    exact = (workspace / SOURCE_REL).read_bytes() == EXPECTED.read_bytes()
    structured = edited.get("structuredContent") or edited.get("structured_content") or {}
    if edited.get("isError") or not exact:
        raise RuntimeError(f"isolated edit preflight failed: {edited}")
    shutil.rmtree(workspace / ".git")
    codex_version = run_capture([str(CODEX), "--version"], REPO)
    login = run_capture([str(CODEX), "login", "status"], REPO, env=clean_child_env())
    if codex_version["exit_code"] != 0 or "0.149.1" not in codex_version["stdout"]:
        raise RuntimeError(f"wrong Codex CLI: {codex_version}")
    if login["exit_code"] != 0 or "ChatGPT" not in (login["stdout"] + login["stderr"]):
        raise RuntimeError(f"ChatGPT subscription login unavailable: {login}")
    receipt = {
        "schema": "multisite-headtohead-preflight.v1",
        "status": "ok",
        "completed_at_ns": time.time_ns(),
        "server_base": SERVER_BASE,
        "server_source_unchanged_from_base": True,
        "fixture_commit": fixture_commit,
        "before_sha256": before_sha,
        "expected_sha256": sha_file(EXPECTED),
        "fixture_lines": len((workspace / SOURCE_REL).read_text(encoding="utf-8").splitlines()),
        "new_symbol_count_after_preflight": (workspace / SOURCE_REL)
        .read_text(encoding="utf-8")
        .count("backoff-delay-ms"),
        "preflight_exact": exact,
        "preflight_verification_complete": structured.get("verification_complete"),
        "offered_server_tools": names,
        "tool_list_sha256": sha_bytes(canonical_json(listed).encode("utf-8")),
        "initialize_sha256": sha_bytes(canonical_json(initialized).encode("utf-8")),
        "codex_version": codex_version["stdout"].strip(),
        "login_status": (login["stdout"] + login["stderr"]).strip(),
        "child_openai_environment_variables": [],
        "tiktoken_version": __import__("tiktoken").__version__,
        "tiktoken_encoding": "o200k_base",
        "catalog_receipt": catalog_receipt,
        "schedule": SCHEDULE,
    }
    receipt["frozen_input_hashes"] = frozen_input_hashes()
    write_json(PREFLIGHT / "preflight.json", receipt)
    print(json.dumps(receipt, indent=2, sort_keys=True))


def require_preflight() -> dict[str, Any]:
    path = PREFLIGHT / "preflight.json"
    if not path.is_file():
        raise RuntimeError("run preflight before any model episode")
    receipt = json.loads(path.read_text(encoding="utf-8"))
    if receipt.get("status") != "ok":
        raise RuntimeError("preflight did not complete")
    if receipt.get("frozen_input_hashes") != frozen_input_hashes():
        raise RuntimeError("frozen preregistration input drift")
    return receipt


def parse_ready_url(path: Path) -> str:
    match = re.search(r':url\s+"([^"]+)"', path.read_text(encoding="utf-8"))
    if not match:
        raise RuntimeError(f"could not parse MCP ready file: {path.read_text(encoding='utf-8')}")
    return match.group(1)


def start_http_server(workspace: Path, run_dir: Path, run_id: str) -> tuple[subprocess.Popen[str], str]:
    ready = run_dir / "mcp-ready.edn"
    stdout_handle = (run_dir / "mcp-server.stdout").open("w", encoding="utf-8")
    stderr_handle = (run_dir / "mcp-server.stderr").open("w", encoding="utf-8")
    argv = [
        str(CLOJURE),
        "-J-Xms32m",
        "-J-Xmx768m",
        "-X:clj-surgeon/mcp",
        ":project-dir",
        json.dumps(str(workspace)),
        ":telemetry",
        ":full",
        ":telemetry-dir",
        json.dumps(str(run_dir / "mcp-telemetry")),
        ":run-id",
        json.dumps(run_id),
        ":nrepl-port",
        ":none",
        ":port",
        "0",
        ":ready-file",
        json.dumps(str(ready)),
    ]
    process = subprocess.Popen(
        argv,
        cwd=REPO,
        env=clean_child_env(),
        stdout=stdout_handle,
        stderr=stderr_handle,
        text=True,
    )
    setattr(process, "_receipt_handles", (stdout_handle, stderr_handle))
    deadline = time.monotonic() + 60
    while time.monotonic() < deadline:
        if ready.is_file() and ready.stat().st_size:
            break
        if process.poll() is not None:
            raise RuntimeError(f"isolated MCP server exited before readiness: {run_dir}")
        time.sleep(0.1)
    else:
        raise RuntimeError(f"isolated MCP server readiness timeout: {run_dir}")
    url = parse_ready_url(ready)
    health = urllib.request.urlopen(url.removesuffix("/mcp") + "/healthz", timeout=5)
    if health.status != 200:
        raise RuntimeError(f"isolated MCP health returned {health.status}")
    return process, url


def stop_process(process: subprocess.Popen[str] | None) -> None:
    if process is None:
        return
    if process.poll() is None:
        process.terminate()
        try:
            process.wait(timeout=5)
        except subprocess.TimeoutExpired:
            process.kill()
            process.wait(timeout=5)
    for handle in getattr(process, "_receipt_handles", ()):
        handle.close()


def toml_string(value: str) -> str:
    return json.dumps(value, ensure_ascii=False)


def config_source(arm: str, run_dir: Path, server_url: str | None) -> str:
    catalog = NATIVE_CATALOG if arm == "N" else SURGEON_CATALOG
    lines = [
        f"model = {toml_string(MODEL)}",
        f"model_reasoning_effort = {toml_string(REASONING)}",
        f"model_catalog_json = {toml_string(str(catalog))}",
        f"model_instructions_file = {toml_string(str(SYSTEM_PROMPT))}",
        'approval_policy = "never"',
        'sandbox_mode = "danger-full-access"',
        "update_plan_enabled = false",
        'web_search = "disabled"',
        "check_for_update_on_startup = false",
        "analytics.enabled = false",
        "feedback.enabled = false",
        "",
        "[tools]",
        "view_image = false",
        "web_search = false",
        "",
        "[features]",
        "shell_tool = false",
        "apps = false",
        "plugins = false",
        "multi_agent = false",
        "memories = false",
        "hooks = true",
        "",
        "[[hooks.PreToolUse]]",
        'matcher = ".*"',
        "",
        "[[hooks.PreToolUse.hooks]]",
        'type = "command"',
        f"command = {toml_string('/usr/bin/python3 ' + str(HOOK))}",
        "timeout = 10",
    ]
    if arm == "S":
        if not server_url:
            raise RuntimeError("Surgeon arm requires a server URL")
        lines.extend(
            [
                "",
                "[mcp_servers.clj-surgeon]",
                f"url = {toml_string(server_url)}",
                "required = true",
                'enabled_tools = ["edit_clojure"]',
                'default_tools_approval_mode = "approve"',
                "startup_timeout_sec = 10",
                "tool_timeout_sec = 45",
            ]
        )
    return "\n".join(lines) + "\n"


def read_jsonl(path: Path) -> list[dict[str, Any]]:
    rows: list[dict[str, Any]] = []
    if not path.is_file():
        return rows
    for number, line in enumerate(path.read_text(encoding="utf-8", errors="replace").splitlines(), 1):
        if not line.strip():
            continue
        try:
            rows.append(json.loads(line))
        except json.JSONDecodeError:
            rows.append({"_invalid_json_line": number, "_raw_sha256": sha_bytes(line.encode())})
    return rows


def run_codex_process(
    workspace: Path, run_dir: Path, arm: str, server_url: str | None
) -> dict[str, Any]:
    hook_log = run_dir / "tool-hooks.jsonl"
    with tempfile.TemporaryDirectory(prefix="multisite-codex-home-", dir="/private/tmp") as home_text:
        codex_home = Path(home_text)
        (codex_home / "auth.json").symlink_to(AUTH)
        config = config_source(arm, run_dir, server_url)
        (codex_home / "config.toml").write_text(config, encoding="utf-8")
        (run_dir / "codex-config.toml").write_text(config, encoding="utf-8")
        env = clean_child_env()
        env["CODEX_HOME"] = str(codex_home)
        env["MULTISITE_HOOK_LOG"] = str(hook_log)
        argv = [
            str(CODEX),
            "exec",
            "--json",
            "--strict-config",
            "--dangerously-bypass-approvals-and-sandbox",
            "--dangerously-bypass-hook-trust",
            "-C",
            str(workspace),
            "-o",
            str(run_dir / "last-message.txt"),
            EFFECTIVE_PROMPT.read_text(encoding="utf-8"),
        ]
        stdout_path = run_dir / "codex-events.jsonl"
        stderr_path = run_dir / "codex.stderr"
        started_ns = time.time_ns()
        started_monotonic = time.monotonic()
        timed_out = False
        exit_code: int | None = None
        with stdout_path.open("w", encoding="utf-8") as stdout_handle, stderr_path.open(
            "w", encoding="utf-8"
        ) as stderr_handle:
            process = subprocess.Popen(
                argv,
                cwd=workspace,
                env=env,
                stdout=stdout_handle,
                stderr=stderr_handle,
                text=True,
            )
            try:
                exit_code = process.wait(timeout=PROCESS_TIMEOUT_SECONDS)
            except subprocess.TimeoutExpired:
                timed_out = True
                process.terminate()
                try:
                    exit_code = process.wait(timeout=10)
                except subprocess.TimeoutExpired:
                    process.kill()
                    exit_code = process.wait(timeout=10)
        ended_monotonic = time.monotonic()
        ended_ns = time.time_ns()
    return {
        "argv": argv,
        "started": True,
        "started_ns": started_ns,
        "ended_ns": ended_ns,
        "wall_seconds": ended_monotonic - started_monotonic,
        "exit_code": exit_code,
        "timed_out": timed_out,
        "child_openai_environment_variables": sorted(key for key in env if key.startswith("OPENAI_")),
    }


def mutation_tool_name(arm: str) -> str:
    return "apply_patch" if arm == "N" else "edit_clojure"


def is_mutation_tool(name: str) -> bool:
    return name == "apply_patch" or name.endswith("edit_clojure")


def intended_tool(arm: str, name: str) -> bool:
    return name == "apply_patch" if arm == "N" else name.endswith("edit_clojure")


def payload_for_hook(arm: str, event: dict[str, Any]) -> str:
    tool_input = event.get("tool_input")
    if arm == "N":
        if not isinstance(tool_input, dict) or not isinstance(tool_input.get("command"), str):
            raise RuntimeError(f"invalid apply_patch hook input: {tool_input}")
        return tool_input["command"]
    if not isinstance(tool_input, dict):
        raise RuntimeError(f"invalid edit_clojure hook input: {tool_input}")
    return canonical_json(tool_input)


def provider_usage(events: list[dict[str, Any]]) -> dict[str, int | None]:
    completed = [event.get("usage", {}) for event in events if event.get("type") == "turn.completed"]
    if not completed:
        return {"input_tokens": None, "cached_input_tokens": None, "output_tokens": None}
    usage = completed[-1]
    return {
        "input_tokens": usage.get("input_tokens"),
        "cached_input_tokens": usage.get("cached_input_tokens"),
        "output_tokens": usage.get("output_tokens"),
    }


def score_episode(run_dir: Path, arm: str, number: int, process: dict[str, Any]) -> dict[str, Any]:
    workspace = run_dir / "workspace"
    source = workspace / SOURCE_REL
    final_bytes = source.read_bytes() if source.is_file() else b""
    (run_dir / "final-retry_policy.clj").write_bytes(final_bytes)
    diff = run_capture(["git", "diff", "--", str(SOURCE_REL)], workspace)
    (run_dir / "git.diff").write_text(diff["stdout"], encoding="utf-8")
    status = run_capture(["git", "status", "--short"], workspace)
    (run_dir / "git-status.txt").write_text(status["stdout"], encoding="utf-8")
    hooks = read_jsonl(run_dir / "tool-hooks.jsonl")
    events = read_jsonl(run_dir / "codex-events.jsonl")
    mutation_hooks = [row for row in hooks if is_mutation_tool(str(row.get("tool_name", "")))]
    intended = [row for row in mutation_hooks if intended_tool(arm, str(row.get("tool_name", "")))]
    payloads = [payload_for_hook(arm, row) for row in intended]
    encoding = tiktoken.get_encoding("o200k_base")
    per_call = [
        {
            "call": index,
            "bytes": len(payload.encode("utf-8")),
            "tokens": len(encoding.encode(payload)),
            "sha256": sha_bytes(payload.encode("utf-8")),
        }
        for index, payload in enumerate(payloads, 1)
    ]
    for index, payload in enumerate(payloads, 1):
        (run_dir / f"mutation-payload-{index:02d}.txt").write_text(payload, encoding="utf-8")
    exact = final_bytes == EXPECTED.read_bytes()
    changed_paths = []
    for line in status["stdout"].splitlines():
        if line.strip():
            changed_paths.append(line[3:])
    only_target_changed = changed_paths == [str(SOURCE_REL)]
    hook_models = sorted({row.get("model") for row in hooks if row.get("model")})
    preflight_receipt = require_preflight()
    catalog = json.loads((NATIVE_CATALOG if arm == "N" else SURGEON_CATALOG).read_text(encoding="utf-8"))
    expected_patch_type = "freeform" if arm == "N" else None
    catalog_valid = catalog["models"][0].get("apply_patch_tool_type") == expected_patch_type
    environment_valid = all(
        [
            process.get("started"),
            process.get("exit_code") == 0,
            not process.get("timed_out"),
            process.get("child_openai_environment_variables") == [],
            hook_models == [MODEL],
            catalog_valid,
            source_unchanged_from_base(),
            preflight_receipt.get("server_base") == SERVER_BASE,
        ]
    )
    route_adherent = bool(intended) and len(intended) == len(mutation_hooks) and only_target_changed
    turns_to_success = len(intended) if exact and intended else None
    usage = provider_usage(events)
    score = {
        "schema": "multisite-headtohead-episode.v1",
        "episode": number,
        "arm": arm,
        "requested_model": MODEL,
        "reasoning_effort": REASONING,
        "environment_valid": environment_valid,
        "semantic_correct": exact,
        "route_adherent": route_adherent,
        "process_exit_code": process.get("exit_code"),
        "process_timed_out": process.get("timed_out"),
        "wall_seconds": process.get("wall_seconds"),
        "hook_models": hook_models,
        "mutation_tools": [row.get("tool_name") for row in mutation_hooks],
        "mutation_calls": len(intended),
        "turns_to_success": turns_to_success,
        "retries": (turns_to_success - 1) if turns_to_success is not None else None,
        "payload_calls": per_call,
        "payload_bytes": sum(call["bytes"] for call in per_call),
        "payload_tokens": sum(call["tokens"] for call in per_call),
        "provider_usage": usage,
        "initial_fixture_commit": preflight_receipt["fixture_commit"],
        "initial_source_sha256": preflight_receipt["before_sha256"],
        "final_source_sha256": sha_bytes(final_bytes),
        "expected_source_sha256": preflight_receipt["expected_sha256"],
        "changed_paths": changed_paths,
        "catalog_valid": catalog_valid,
        "server_source_unchanged_from_base": source_unchanged_from_base(),
    }
    shutil.rmtree(workspace / ".git")
    write_json(run_dir / "process.json", process)
    write_json(run_dir / "score.json", score)
    return score


def run_episode(number: int, arm: str) -> dict[str, Any]:
    require_preflight()
    run_dir = RUNS / f"{number:03d}-{arm}"
    if run_dir.exists():
        raise RuntimeError(f"refusing replacement episode: {run_dir}")
    run_dir.mkdir(parents=True)
    workspace = run_dir / "workspace"
    fixture_commit = materialize_workspace(workspace)
    expected_commit = require_preflight()["fixture_commit"]
    if fixture_commit != expected_commit:
        raise RuntimeError(f"fixture commit drift: {fixture_commit} != {expected_commit}")
    if sha_file(workspace / SOURCE_REL) != require_preflight()["before_sha256"]:
        raise RuntimeError("fixture source drift")
    server: subprocess.Popen[str] | None = None
    server_url: str | None = None
    try:
        if arm == "S":
            server, server_url = start_http_server(workspace, run_dir, f"multisite-{number:03d}-S")
        process = run_codex_process(workspace, run_dir, arm, server_url)
    finally:
        stop_process(server)
    score = score_episode(run_dir, arm, number, process)
    print(canonical_json(score), flush=True)
    return score


def exact_permutation_p(native: list[float], surgeon: list[float]) -> float:
    observed = abs(median(surgeon) - median(native))
    pooled = native + surgeon
    extreme = 0
    total = 0
    indices = range(len(pooled))
    for selected in itertools.combinations(indices, len(native)):
        native_indices = set(selected)
        left = [pooled[index] for index in indices if index in native_indices]
        right = [pooled[index] for index in indices if index not in native_indices]
        if abs(median(right) - median(left)) >= observed - 1e-12:
            extreme += 1
        total += 1
    return extreme / total


def standardized_mde() -> float:
    sample = 6
    degrees = 2 * sample - 2
    critical = t.ppf(0.975, degrees)

    def power(effect: float) -> float:
        noncentrality = effect * (sample / 2) ** 0.5
        return nct.cdf(-critical, degrees, noncentrality) + 1 - nct.cdf(
            critical, degrees, noncentrality
        )

    return brentq(lambda effect: power(effect) - 0.8, 0.01, 10.0)


def load_scores() -> list[dict[str, Any]]:
    scores = []
    for number, arm in enumerate(SCHEDULE, 1):
        path = RUNS / f"{number:03d}-{arm}" / "score.json"
        if not path.is_file():
            raise RuntimeError(f"missing scheduled score: {path}")
        score = json.loads(path.read_text(encoding="utf-8"))
        if score.get("episode") != number or score.get("arm") != arm:
            raise RuntimeError(f"score identity mismatch: {path}")
        scores.append(score)
    return scores


def aggregate_scores(scores: list[dict[str, Any]]) -> dict[str, Any]:
    arms: dict[str, Any] = {}
    for arm in ["N", "S"]:
        scheduled = [score for score in scores if score["arm"] == arm]
        eligible = [
            score
            for score in scheduled
            if score["environment_valid"] and score["semantic_correct"] and score["route_adherent"]
        ]
        arms[arm] = {
            "scheduled": len(scheduled),
            "environment_valid": sum(bool(score["environment_valid"]) for score in scheduled),
            "semantic_correct": sum(bool(score["semantic_correct"]) for score in scheduled),
            "route_adherent": sum(bool(score["route_adherent"]) for score in scheduled),
            "confirmatory_eligible": len(eligible),
            "median_payload_bytes": median(score["payload_bytes"] for score in eligible) if eligible else None,
            "median_payload_tokens": median(score["payload_tokens"] for score in eligible) if eligible else None,
            "median_wall_seconds": median(score["wall_seconds"] for score in eligible) if eligible else None,
            "median_turns_to_success": median(score["turns_to_success"] for score in eligible) if eligible else None,
            "total_retries": sum(score["retries"] for score in eligible if score["retries"] is not None),
            "one_call_successes": sum(score["turns_to_success"] == 1 for score in eligible),
        }
    killed = arms["N"]["confirmatory_eligible"] < 6 or arms["S"]["confirmatory_eligible"] < 6
    deltas: dict[str, Any] = {}
    if not killed:
        for metric in ["payload_tokens", "wall_seconds"]:
            native_value = arms["N"][f"median_{metric}"]
            surgeon_value = arms["S"][f"median_{metric}"]
            deltas[metric] = {
                "S_minus_N": surgeon_value - native_value,
                "percent_of_N": 100 * (surgeon_value - native_value) / native_value,
            }
        token_gate = deltas["payload_tokens"]["percent_of_N"] <= -45
        wall_gate = (
            deltas["wall_seconds"]["percent_of_N"] <= -12
            and deltas["wall_seconds"]["S_minus_N"] <= -2.5
        )
        directions = all(deltas[metric]["S_minus_N"] < 0 for metric in deltas)
        if directions and token_gate and wall_gate:
            verdict = "supported"
        elif directions:
            verdict = "directionally-supported-smaller-than-predicted"
        else:
            verdict = "unsupported-on-this-fixture"
    else:
        token_gate = False
        wall_gate = False
        verdict = "killed"
    secondary = {}
    if not killed:
        for metric in ["payload_tokens", "wall_seconds"]:
            native = [score[metric] for score in scores if score["arm"] == "N"]
            surgeon = [score[metric] for score in scores if score["arm"] == "S"]
            secondary[f"exact_label_permutation_p_{metric}"] = exact_permutation_p(native, surgeon)
    return {
        "schema": "multisite-headtohead-aggregate.v1",
        "schedule": SCHEDULE,
        "arms": arms,
        "deltas": deltas,
        "kill_criterion_triggered": killed,
        "payload_gate_cleared": token_gate,
        "wall_gate_cleared": wall_gate,
        "verdict": verdict,
        "secondary": secondary,
        "power_bound": {
            "method": "two-sided pooled two-sample t approximation, alpha=0.05, equal variance normal shift",
            "power": 0.8,
            "n_per_arm": 6,
            "minimum_detectable_standardized_effect": standardized_mde(),
        },
        "episodes": scores,
    }


def write_aggregate(aggregate: dict[str, Any]) -> None:
    write_json(ROOT / "aggregate.json", aggregate)
    header = [
        "episode",
        "arm",
        "environment_valid",
        "semantic_correct",
        "route_adherent",
        "payload_bytes",
        "payload_tokens",
        "wall_seconds",
        "turns_to_success",
        "retries",
        "provider_output_tokens",
    ]
    rows = ["\t".join(header)]
    for score in aggregate["episodes"]:
        values = [
            score["episode"],
            score["arm"],
            score["environment_valid"],
            score["semantic_correct"],
            score["route_adherent"],
            score["payload_bytes"],
            score["payload_tokens"],
            f'{score["wall_seconds"]:.6f}',
            score["turns_to_success"],
            score["retries"],
            score["provider_usage"]["output_tokens"],
        ]
        rows.append("\t".join(str(value) for value in values))
    (ROOT / "runs.tsv").write_text("\n".join(rows) + "\n", encoding="utf-8")


def write_sha_manifest() -> None:
    excluded = {ROOT / "SHA256SUMS"}
    files = sorted(path for path in ROOT.rglob("*") if path.is_file() and path not in excluded)
    lines = [f"{sha_file(path)}  {path.relative_to(ROOT)}" for path in files]
    (ROOT / "SHA256SUMS").write_text("\n".join(lines) + "\n", encoding="utf-8")


def run_cohort() -> None:
    require_preflight()
    status = run_capture(["git", "status", "--porcelain"], REPO)
    if status["stdout"]:
        raise RuntimeError("commit the frozen preregistration before model execution")
    registration_commit = run_capture(["git", "rev-parse", "HEAD"], REPO)["stdout"].strip()
    write_json(
        ROOT / "registration-boundary.json",
        {
            "registration_commit": registration_commit,
            "started_at_ns": time.time_ns(),
            "schedule": SCHEDULE,
        },
    )
    scores = []
    for number, arm in enumerate(SCHEDULE, 1):
        scores.append(run_episode(number, arm))
    aggregate = aggregate_scores(scores)
    write_aggregate(aggregate)
    write_sha_manifest()
    print(json.dumps(aggregate, indent=2, sort_keys=True))


def verify() -> None:
    aggregate = aggregate_scores(load_scores())
    committed = json.loads((ROOT / "aggregate.json").read_text(encoding="utf-8"))
    if canonical_json(aggregate) != canonical_json(committed):
        raise RuntimeError("aggregate replay mismatch")
    failures = []
    for line in (ROOT / "SHA256SUMS").read_text(encoding="utf-8").splitlines():
        expected, relative = line.split("  ", 1)
        path = ROOT / relative
        if not path.is_file() or sha_file(path) != expected:
            failures.append(relative)
    if failures:
        raise RuntimeError(f"SHA manifest mismatch: {failures}")
    print(canonical_json({"ok": True, "verdict": aggregate["verdict"], "files_verified": len((ROOT / "SHA256SUMS").read_text().splitlines())}))


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("command", choices=["preflight", "run", "score", "verify"])
    args = parser.parse_args()
    if args.command == "preflight":
        preflight()
    elif args.command == "run":
        run_cohort()
    elif args.command == "score":
        aggregate = aggregate_scores(load_scores())
        write_aggregate(aggregate)
        write_sha_manifest()
        print(json.dumps(aggregate, indent=2, sort_keys=True))
    else:
        verify()
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
