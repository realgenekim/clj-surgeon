#!/usr/bin/env python3
"""Run the bounded real-install-surface routing-guidance transfer screen."""

from __future__ import annotations

import argparse
import hashlib
import json
import os
import shutil
import subprocess
import sys
import tempfile
import time
from pathlib import Path
from typing import Any


HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[1]
SCREEN_DIR = HERE / "screens/live-routing-guidance"
CONTROL_SOURCE = ROOT / "resources/clj-surgeon-agent-routing.md"
TREATMENT_SOURCE = SCREEN_DIR / "treatment-routing.md"
PREREGISTRATION = SCREEN_DIR / "preregistration.md"
SERVER = HERE / "mock_mcp.py"
FIXTURES = json.loads((HERE / "fixtures.json").read_text(encoding="utf-8"))
PROMPT = (HERE / "prompt.txt").read_text(encoding="utf-8")
MODEL = "gpt-5.6-sol"
REASONING = "high"
CONTROL_PILOT = [(1, "A", "f01"), (2, "A", "f02"), (3, "A", "f03"), (4, "A", "f04")]
COHORT = [
    (1, "A", "f05"), (2, "B", "f05"),
    (3, "B", "f06"), (4, "A", "f06"),
    (5, "B", "f07"), (6, "A", "f07"),
    (7, "A", "f08"), (8, "B", "f08"),
]


def canonical(value: Any) -> bytes:
    return json.dumps(value, sort_keys=True, separators=(",", ":"), ensure_ascii=False).encode()


def sha_bytes(value: bytes) -> str:
    return hashlib.sha256(value).hexdigest()


def sha_file(path: Path) -> str:
    return sha_bytes(path.read_bytes())


def atomic_json(path: Path, value: Any) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    temporary = path.with_suffix(path.suffix + ".tmp")
    temporary.write_bytes(canonical(value) + b"\n")
    temporary.replace(path)


def capture(argv: list[str], cwd: Path, env: dict[str, str] | None = None, timeout: int = 120) -> dict[str, Any]:
    completed = subprocess.run(argv, cwd=cwd, env=env, text=True, capture_output=True, timeout=timeout, check=False)
    return {"argv": argv, "exit_code": completed.returncode, "stdout": completed.stdout, "stderr": completed.stderr}


def fixture(identifier: str) -> dict[str, Any]:
    return next(row for row in FIXTURES if row["id"] == identifier)


def source_for(row: dict[str, Any], expected: bool = False) -> str:
    value = row["new"] if expected else row["old"]
    return f"(ns {row['namespace']})\n\n(defn {row['owner']} []\n  {{{json.dumps(row['key'])} {value}}})\n"


def distractor_for(row: dict[str, Any]) -> str:
    return f"(ns acme.distractor)\n\n(defn distractor-policy []\n  {{{json.dumps(row['key'])} {row['old']}}})\n"


def initialize_workspace(path: Path, row: dict[str, Any]) -> dict[str, str]:
    target = path / row["file"]
    distractor = path / "src/acme/distractor.clj"
    target.parent.mkdir(parents=True)
    target.write_text(source_for(row), encoding="utf-8")
    distractor.write_text(distractor_for(row), encoding="utf-8")
    (path / "README.md").write_text("Synthetic routing fixture.\n", encoding="utf-8")
    for argv in (["git", "init", "-q"], ["git", "add", "."], ["git", "-c", "user.name=fixture", "-c", "user.email=fixture@invalid", "commit", "-q", "-m", "fixture: frozen bounded literal"]):
        result = capture(list(argv), path)
        if result["exit_code"] != 0:
            raise RuntimeError("fixture initialization failed")
    return {"target_initial_sha256": sha_file(target), "distractor_initial_sha256": sha_file(distractor)}


def render_prompt(row: dict[str, Any]) -> str:
    return PROMPT.replace("__FILE__", row["file"]).replace("__OWNER__", row["owner"]).replace("__OLD__", row["old"]).replace("__NEW__", row["new"])


def auth_file() -> Path:
    explicit = os.environ.get("BENCH_AUTH_FILE")
    if explicit:
        return Path(explicit).resolve()
    current_home = Path(os.environ.get("CODEX_HOME", str(Path.home() / ".codex")))
    return (current_home / "auth.json").resolve()


def install_routing(run_dir: Path, arm: str) -> dict[str, Any]:
    codex_home = run_dir / "codex-home"
    claude_home = run_dir / "claude-home"
    codex_home.mkdir()
    claude_home.mkdir()
    source = CONTROL_SOURCE if arm == "A" else TREATMENT_SOURCE
    auth = auth_file()
    if not auth.is_file():
        raise RuntimeError(f"Codex auth file absent: {auth}")
    (codex_home / "auth.json").symlink_to(auth)
    result = capture(
        [
            "make", "--no-print-directory", "install-agent-routing",
            f"CODEX_HOME={codex_home}",
            f"CLAUDE_HOME={claude_home}",
            f"AGENT_ROUTING_SOURCE={source}",
        ],
        ROOT,
    )
    (run_dir / "install.stdout").write_text(result["stdout"], encoding="utf-8")
    (run_dir / "install.stderr").write_text(result["stderr"], encoding="utf-8")
    if result["exit_code"] != 0:
        raise RuntimeError("install-agent-routing failed")
    codex_instructions = codex_home / "AGENTS.md"
    claude_instructions = claude_home / "CLAUDE.md"
    if codex_instructions.read_bytes() != source.read_bytes() or claude_instructions.read_bytes() != source.read_bytes():
        raise RuntimeError("installed routing bytes differ from source")
    return {
        "codex_home": str(codex_home.resolve()),
        "source": str(source.resolve()),
        "source_sha256": sha_file(source),
        "codex_agents_sha256": sha_file(codex_instructions),
        "claude_instructions_sha256": sha_file(claude_instructions),
    }


def codex_argv(row: dict[str, Any], workspace: Path, run_dir: Path) -> list[str]:
    server_args = [
        "ROUTING_SCREEN=native-description",
        "ROUTING_ARM=A",
        "ROUTING_WORKSPACE=" + str(workspace.resolve()),
        "ROUTING_SERVER_LOG=" + str((run_dir / "server.jsonl").resolve()),
        "ROUTING_FIXTURE=" + json.dumps(row, separators=(",", ":")),
        str(Path(sys.executable).resolve()),
        str(SERVER.resolve()),
    ]
    return [
        str(Path(shutil.which("codex") or "/missing/codex").resolve()),
        "exec", "--json", "--ephemeral", "--ignore-user-config", "--strict-config",
        "-m", MODEL, "-c", 'model_reasoning_effort="high"',
        "--dangerously-bypass-approvals-and-sandbox", "-C", str(workspace.resolve()),
        "-c", 'mcp_servers.routing_lab.command="/usr/bin/env"',
        "-c", "mcp_servers.routing_lab.args=" + json.dumps(server_args, separators=(",", ":")),
        render_prompt(row),
    ]


def run_codex(argv: list[str], workspace: Path, run_dir: Path, codex_home: str) -> dict[str, Any]:
    env = os.environ.copy()
    env.pop("OPENAI_API_KEY", None)
    env["CODEX_HOME"] = codex_home
    started_ns = time.time_ns()
    with (run_dir / "events.jsonl").open("w", encoding="utf-8") as stdout, (run_dir / "stderr.log").open("w", encoding="utf-8") as stderr:
        process = subprocess.Popen(argv, cwd=workspace, env=env, text=True, stdout=stdout, stderr=stderr)
        timed_out = False
        try:
            exit_code = process.wait(timeout=240)
        except subprocess.TimeoutExpired:
            timed_out = True
            process.terminate()
            try:
                exit_code = process.wait(timeout=10)
            except subprocess.TimeoutExpired:
                process.kill()
                exit_code = process.wait(timeout=10)
    return {"exit_code": exit_code, "timed_out": timed_out, "started_ns": started_ns, "ended_ns": time.time_ns()}


def read_jsonl(path: Path) -> list[dict[str, Any]]:
    if not path.exists():
        return []
    rows = []
    for line in path.read_text(encoding="utf-8").splitlines():
        if line.strip():
            rows.append(json.loads(line))
    return rows


def score(run_dir: Path, arm: str, row: dict[str, Any], setup: dict[str, str], process: dict[str, Any], routing: dict[str, Any]) -> dict[str, Any]:
    events = read_jsonl(run_dir / "events.jsonl")
    server_rows = read_jsonl(run_dir / "server.jsonl")
    actions = [event.get("item", {}) for event in events if event.get("type") == "item.completed" and event.get("item", {}).get("type") in {"mcp_tool_call", "file_change", "command_execution"}]
    primary_route = "none"
    for action in actions:
        if action.get("type") == "file_change" or (action.get("type") == "mcp_tool_call" and action.get("tool") == "native_patch"):
            primary_route = "native"
            break
        if action.get("type") == "mcp_tool_call" and action.get("tool") == "edit_clojure":
            primary_route = "structural"
            break
    target = run_dir / "workspace" / row["file"]
    distractor = run_dir / "workspace/src/acme/distractor.clj"
    changed = capture(["git", "diff", "--name-only"], run_dir / "workspace")["stdout"].splitlines()
    wrong_subject = any(path != row["file"] for path in changed)
    rejected = [entry for entry in server_rows if entry.get("event") == "call_rejected"]
    ready = next((entry for entry in server_rows if entry.get("event") == "server_ready"), {})
    usage = next((entry.get("usage", {}) for entry in reversed(events) if entry.get("type") == "turn.completed"), {})
    result = {
        "schema": "routing-live-guidance-score.v1",
        "run": run_dir.name,
        "arm": arm,
        "fixture": row["id"],
        "model": MODEL,
        "reasoning": REASONING,
        "environment_valid": process["exit_code"] == 0 and not process["timed_out"] and ready.get("screen") == "native-description" and ready.get("arm") == "A" and ready.get("fixture") == row["id"],
        "semantic_correct": target.read_bytes() == source_for(row, expected=True).encode() and not wrong_subject,
        "wrong_subject": wrong_subject,
        "invalid_call": bool(rejected),
        "primary_route": primary_route,
        "changed_paths": changed,
        "server_tool_list_sha256": ready.get("offered_tool_list_sha256"),
        "routing_source_sha256": routing["source_sha256"],
        "installed_agents_sha256": routing["codex_agents_sha256"],
        "prompt_sha256": sha_bytes(render_prompt(row).encode()),
        "target_initial_sha256": setup["target_initial_sha256"],
        "target_final_sha256": sha_file(target),
        "distractor_initial_sha256": setup["distractor_initial_sha256"],
        "distractor_final_sha256": sha_file(distractor),
        "wall_ms": (process["ended_ns"] - process["started_ns"]) / 1_000_000,
        "usage": usage,
        "action_count": len(actions),
    }
    atomic_json(run_dir / "process.json", process)
    atomic_json(run_dir / "score.json", result)
    (run_dir / "git.diff").write_text(capture(["git", "diff"], run_dir / "workspace")["stdout"], encoding="utf-8")
    return result


def run_one(root: Path, run_number: int, arm: str, fixture_id: str) -> dict[str, Any]:
    run_dir = root / f"{run_number:03d}-{arm}-{fixture_id}"
    if run_dir.exists():
        raise RuntimeError(f"refusing replacement: {run_dir}")
    run_dir.mkdir(parents=True)
    workspace = run_dir / "workspace"
    setup = initialize_workspace(workspace, fixture(fixture_id))
    routing = install_routing(run_dir, arm)
    argv = codex_argv(fixture(fixture_id), workspace, run_dir)
    atomic_json(run_dir / "invocation.json", {"argv": argv, "routing": routing})
    process = run_codex(argv, workspace, run_dir, routing["codex_home"])
    result = score(run_dir, arm, fixture(fixture_id), setup, process, routing)
    print(json.dumps({key: result[key] for key in ("run", "arm", "fixture", "primary_route", "environment_valid", "semantic_correct", "wrong_subject")}, sort_keys=True), flush=True)
    return result


def subscription_preflight() -> dict[str, Any]:
    if os.environ.get("OPENAI_API_KEY"):
        raise RuntimeError("OPENAI_API_KEY must be absent")
    version = capture([str(Path(shutil.which("codex") or "/missing/codex").resolve()), "--version"], ROOT)
    status = capture([str(Path(shutil.which("codex") or "/missing/codex").resolve()), "login", "status"], ROOT)
    if version["exit_code"] != 0 or status["exit_code"] != 0 or "chatgpt" not in (status["stdout"] + status["stderr"]).lower():
        raise RuntimeError("ChatGPT subscription preflight failed")
    return {"codex_version": version["stdout"].strip(), "auth": (status["stdout"] or status["stderr"]).strip(), "model": MODEL, "reasoning": REASONING}


def freeze() -> None:
    if (SCREEN_DIR / "freeze.json").exists():
        raise RuntimeError("freeze already exists")
    status = capture(["git", "status", "--porcelain"], ROOT)
    if status["stdout"]:
        raise RuntimeError("freeze requires a clean worktree")
    head = capture(["git", "rev-parse", "HEAD"], ROOT)["stdout"].strip()
    atomic_json(SCREEN_DIR / "freeze.json", {
        "schema": "routing-live-guidance-freeze.v1",
        "candidate_commit": head,
        "candidate_tree": capture(["git", "rev-parse", "HEAD^{tree}"], ROOT)["stdout"].strip(),
        "control_source_sha256": sha_file(CONTROL_SOURCE),
        "treatment_source_sha256": sha_file(TREATMENT_SOURCE),
        "preregistration_sha256": sha_file(PREREGISTRATION),
        "runner_sha256": sha_file(Path(__file__)),
        "pilot": CONTROL_PILOT,
        "cohort": COHORT,
        "model": MODEL,
        "reasoning": REASONING,
        "minimal_schema_excluded": True,
    })


def require_freeze() -> dict[str, Any]:
    path = SCREEN_DIR / "freeze.json"
    if not path.is_file():
        raise RuntimeError("committed freeze required")
    frozen = json.loads(path.read_text(encoding="utf-8"))
    checks = {
        "control_source_sha256": sha_file(CONTROL_SOURCE),
        "treatment_source_sha256": sha_file(TREATMENT_SOURCE),
        "preregistration_sha256": sha_file(PREREGISTRATION),
        "runner_sha256": sha_file(Path(__file__)),
    }
    for key, actual in checks.items():
        if frozen.get(key) != actual:
            raise RuntimeError(f"frozen hash mismatch: {key}")
    if frozen.get("pilot") != [list(row) for row in CONTROL_PILOT] or frozen.get("cohort") != [list(row) for row in COHORT]:
        raise RuntimeError("frozen schedule mismatch")
    return frozen


def pilot() -> None:
    require_freeze()
    output = SCREEN_DIR / "pilot-runs"
    if output.exists() or (SCREEN_DIR / "pilot.json").exists():
        raise RuntimeError("pilot is one-shot")
    preflight = subscription_preflight()
    rows = [run_one(output, *entry) for entry in CONTROL_PILOT]
    structural = sum(row["primary_route"] == "structural" for row in rows)
    valid = all(row["environment_valid"] and row["semantic_correct"] and not row["wrong_subject"] for row in rows)
    status = "sub-ceiling" if valid and structural <= 2 else "stop"
    atomic_json(SCREEN_DIR / "pilot.json", {"schema": "routing-live-guidance-pilot.v1", "status": status, "structural_first": structural, "attempts": len(rows), "subscription_preflight": preflight, "scores": rows})


def cohort() -> None:
    require_freeze()
    pilot_receipt = json.loads((SCREEN_DIR / "pilot.json").read_text(encoding="utf-8"))
    if pilot_receipt.get("status") != "sub-ceiling":
        raise RuntimeError("sub-ceiling control pilot required")
    output = SCREEN_DIR / "cohort-runs"
    if output.exists() or (SCREEN_DIR / "result.json").exists():
        raise RuntimeError("cohort is one-shot")
    preflight = subscription_preflight()
    rows = [run_one(output, *entry) for entry in COHORT]
    by_arm = {arm: [row for row in rows if row["arm"] == arm] for arm in "AB"}
    structural = {arm: sum(row["primary_route"] == "structural" for row in by_arm[arm]) for arm in "AB"}
    surface_hashes = {row["server_tool_list_sha256"] for row in rows}
    all_valid = all(row["environment_valid"] and row["semantic_correct"] and not row["wrong_subject"] and not row["invalid_call"] for row in rows)
    lift_pp = 25 * (structural["B"] - structural["A"])
    verdict = "clears-transfer-screen" if all_valid and len(surface_hashes) == 1 and lift_pp >= 25 else "stop"
    atomic_json(SCREEN_DIR / "result.json", {"schema": "routing-live-guidance-result.v1", "verdict": verdict, "structural_first": structural, "lift_pp": lift_pp, "client_surface_hashes": sorted(surface_hashes), "all_valid": all_valid, "subscription_preflight": preflight, "scores": rows})


def self_test() -> None:
    if CONTROL_PILOT != [(1, "A", "f01"), (2, "A", "f02"), (3, "A", "f03"), (4, "A", "f04")]:
        raise AssertionError("pilot drift")
    if COHORT != [(1, "A", "f05"), (2, "B", "f05"), (3, "B", "f06"), (4, "A", "f06"), (5, "B", "f07"), (6, "A", "f07"), (7, "A", "f08"), (8, "B", "f08")]:
        raise AssertionError("cohort drift")
    control_lines = CONTROL_SOURCE.read_text(encoding="utf-8").splitlines()
    treatment_lines = TREATMENT_SOURCE.read_text(encoding="utf-8").splitlines()
    added = [line for line in treatment_lines if line not in control_lines]
    if added != ["- For bounded edits to existing Clojure forms, call `edit_clojure`; use native", "  patching for prose, new files, or unsupported changes."]:
        raise AssertionError(f"unexpected treatment delta: {added}")
    with tempfile.TemporaryDirectory(prefix="clj-surgeon-routing-install-self-test-") as temporary:
        run_dir = Path(temporary) / "run"
        run_dir.mkdir()
        original = os.environ.get("BENCH_AUTH_FILE")
        fake_auth = Path(temporary) / "auth.json"
        fake_auth.write_text("{}\n", encoding="utf-8")
        os.environ["BENCH_AUTH_FILE"] = str(fake_auth)
        try:
            for arm in "AB":
                arm_dir = run_dir / arm
                arm_dir.mkdir()
                result = install_routing(arm_dir, arm)
                if result["source_sha256"] != result["codex_agents_sha256"] or result["source_sha256"] != result["claude_instructions_sha256"]:
                    raise AssertionError("installer changed routing bytes")
        finally:
            if original is None:
                os.environ.pop("BENCH_AUTH_FILE", None)
            else:
                os.environ["BENCH_AUTH_FILE"] = original
    print("live routing-guidance self-test passed")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("command", choices=["self-test", "freeze", "pilot", "cohort"])
    args = parser.parse_args()
    {"self-test": self_test, "freeze": freeze, "pilot": pilot, "cohort": cohort}[args.command]()


if __name__ == "__main__":
    main()
