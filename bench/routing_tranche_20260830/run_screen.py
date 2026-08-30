#!/usr/bin/env python3
"""Freeze, pilot, execute, score, and archive one routing-lens screen."""

from __future__ import annotations

import argparse
import concurrent.futures
import hashlib
import json
import math
import os
import shutil
import subprocess
import sys
import tarfile
import time
from pathlib import Path
from typing import Any


ROOT = Path(__file__).resolve().parent
FIXTURES = json.loads((ROOT / "fixtures.json").read_text(encoding="utf-8"))
PROMPT = (ROOT / "prompt.txt").read_text(encoding="utf-8")
SERVER = ROOT / "mock_mcp.py"
MODEL = "gpt-5.6-sol"
EFFORT = "high"
PAIRS = 24
PARALLEL = 4
SCREENS = ["native-description", "action-native-name", "minimal-schema", "refusal-handoff"]
PREDICTIONS = {
    "native-description": {"A": 0.25, "B": 0.50, "lift_pp": 25, "kill_lift_below_pp": 15, "max_reversing_fixtures": 4},
    "action-native-name": {"A": 0.20, "B": 0.45, "lift_pp": 25, "initial_kill_lift_below_pp": 10, "replication_required_lift_pp": 20},
    "minimal-schema": {"A": 0.25, "B": 0.50, "lift_pp": 25, "kill_lift_below_pp": 15, "max_invalid_or_wrong_owner_increase_pp": 5},
    "refusal-handoff": {"A_voluntary": 0.25, "B_voluntary": 0.25, "immediate_handoff": 0.80, "success_A": 0.95, "success_B": 0.95, "kill_handoff_below": 0.70, "kill_success_drop_over_pp": 10},
}
STATIC = [ROOT / "fixtures.json", ROOT / "prompt.txt", ROOT / "mock_mcp.py", Path(__file__).resolve(), ROOT / "README.md", ROOT / "replay.sh"]


def canonical(value: Any) -> bytes:
    return json.dumps(value, sort_keys=True, separators=(",", ":"), ensure_ascii=False).encode()


def sha_bytes(value: bytes) -> str:
    return hashlib.sha256(value).hexdigest()


def sha_file(path: Path) -> str:
    return sha_bytes(path.read_bytes())


def atomic_json(path: Path, value: Any) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    stage = path.with_suffix(path.suffix + ".tmp")
    stage.write_text(json.dumps(value, indent=2, sort_keys=True) + "\n", encoding="utf-8")
    stage.replace(path)


def read_jsonl(path: Path) -> list[dict[str, Any]]:
    rows = []
    if not path.exists():
        return rows
    for line in path.read_text(encoding="utf-8", errors="replace").splitlines():
        if line.strip():
            try:
                rows.append(json.loads(line))
            except json.JSONDecodeError:
                rows.append({"invalid_json": line})
    return rows


def run_capture(argv: list[str], cwd: Path, timeout: int = 120, env: dict[str, str] | None = None) -> dict[str, Any]:
    started_ns = time.time_ns()
    try:
        row = subprocess.run(argv, cwd=cwd, text=True, capture_output=True, check=False, timeout=timeout, env=env)
        return {"argv": argv, "started_ns": started_ns, "ended_ns": time.time_ns(), "exit_code": row.returncode, "stdout": row.stdout, "stderr": row.stderr, "timed_out": False}
    except subprocess.TimeoutExpired as exc:
        return {"argv": argv, "started_ns": started_ns, "ended_ns": time.time_ns(), "exit_code": None, "stdout": exc.stdout or "", "stderr": exc.stderr or "", "timed_out": True}


def screen_dir(screen: str) -> Path:
    return ROOT / "screens" / screen


def prereg_path(screen: str) -> Path:
    return screen_dir(screen) / "preregistration.md"


def schedule(replication: bool = False) -> list[dict[str, Any]]:
    rows = []
    n = 1
    fixtures = list(reversed(FIXTURES)) if replication else FIXTURES
    for index, fixture in enumerate(fixtures):
        arms = "ABBA" if index % 2 == 0 else "BAAB"
        for arm in arms:
            rows.append({"run": n, "fixture": fixture["id"], "arm": arm})
            n += 1
    return rows


def static_hashes(screen: str) -> dict[str, str]:
    paths = STATIC + [prereg_path(screen)]
    return {str(path.relative_to(ROOT)): sha_file(path) for path in paths}


def freeze(screen: str) -> None:
    path = screen_dir(screen) / "freeze.json"
    if path.exists():
        raise RuntimeError("freeze is one-shot")
    receipt = {
        "schema": "routing-tranche-freeze.v1",
        "status": "frozen",
        "screen": screen,
        "frozen_at_ns": time.time_ns(),
        "model": MODEL,
        "reasoning_effort": EFFORT,
        "subscription_route_only": True,
        "openai_api_key_required_absent": True,
        "pairs": PAIRS,
        "fixtures": len(FIXTURES),
        "schedule": schedule(),
        "schedule_sha256": sha_bytes(canonical(schedule())),
        "replication_schedule": schedule(True) if screen == "action-native-name" else None,
        "replication_schedule_sha256": sha_bytes(canonical(schedule(True))) if screen == "action-native-name" else None,
        "prediction": PREDICTIONS[screen],
        "wrong_subject_required": 0,
        "control_pilot": {"runs": 2, "fixtures": ["f01", "f02"], "pass_if_structural_first_below": 2, "excluded_from_cohort": True},
        "static_hashes": static_hashes(screen),
    }
    atomic_json(path, receipt)
    print(json.dumps(receipt, sort_keys=True))


def require_freeze(screen: str) -> dict[str, Any]:
    path = screen_dir(screen) / "freeze.json"
    if not path.exists():
        raise RuntimeError("frozen preregistration required")
    row = json.loads(path.read_text(encoding="utf-8"))
    if row.get("status") != "frozen" or row.get("static_hashes") != static_hashes(screen):
        raise RuntimeError("frozen input drift")
    if row.get("schedule") != schedule():
        raise RuntimeError("schedule drift")
    return row


def fixture_by_id(identifier: str) -> dict[str, Any]:
    return next(row for row in FIXTURES if row["id"] == identifier)


def source_for(fixture: dict[str, Any], expected: bool = False) -> str:
    value = fixture["new"] if expected else fixture["old"]
    return f"(ns {fixture['namespace']})\n\n(defn {fixture['owner']} []\n  {{:{fixture['key']} {value}\n   :fixture :target}})\n\n(defn unrelated-helper []\n  :unchanged)\n"


def distractor_for(fixture: dict[str, Any]) -> str:
    return f"(ns {fixture['namespace']}-distractor)\n\n(defn distractor-policy []\n  {{:{fixture['key']} {fixture['old']}\n   :fixture \"distractor-{fixture['id']}\"}})\n"


def reset_workspace(path: Path, fixture: dict[str, Any]) -> dict[str, str]:
    if path.exists():
        shutil.rmtree(path)
    target = path / fixture["file"]
    distractor = path / "src/acme/distractor.clj"
    target.parent.mkdir(parents=True)
    target.write_text(source_for(fixture), encoding="utf-8")
    distractor.write_text(distractor_for(fixture), encoding="utf-8")
    (path / "README.md").write_text("Synthetic routing fixture.\n", encoding="utf-8")
    for argv in (["git", "init", "-q"], ["git", "add", "."], ["git", "-c", "user.name=fixture", "-c", "user.email=fixture@invalid", "commit", "-q", "-m", "fixture: frozen bounded literal"]):
        row = run_capture(list(argv), path)
        if row["exit_code"] != 0:
            raise RuntimeError("fixture git initialization failed")
    return {"target_initial_sha256": sha_file(target), "distractor_initial_sha256": sha_file(distractor), "fixture_commit": run_capture(["git", "rev-parse", "HEAD"], path)["stdout"].strip()}


def render_prompt(fixture: dict[str, Any]) -> str:
    return PROMPT.replace("__FILE__", fixture["file"]).replace("__OWNER__", fixture["owner"]).replace("__OLD__", fixture["old"]).replace("__NEW__", fixture["new"])


def codex_argv(screen: str, arm: str, fixture: dict[str, Any], workspace: Path, run_dir: Path) -> list[str]:
    env_args = [
        "ROUTING_SCREEN=" + screen,
        "ROUTING_ARM=" + arm,
        "ROUTING_WORKSPACE=" + str(workspace.resolve()),
        "ROUTING_SERVER_LOG=" + str((run_dir / "server.jsonl").resolve()),
        "ROUTING_FIXTURE=" + json.dumps(fixture, separators=(",", ":")),
        str(Path(sys.executable).resolve()),
        str(SERVER.resolve()),
    ]
    return [
        str(Path(shutil.which("codex") or "/missing/codex").resolve()),
        "exec", "--json", "--ephemeral", "--ignore-user-config", "--strict-config",
        "-m", MODEL, "-c", 'model_reasoning_effort="high"',
        "--dangerously-bypass-approvals-and-sandbox", "-C", str(workspace.resolve()),
        "-c", 'mcp_servers.routing_lab.command="/usr/bin/env"',
        "-c", "mcp_servers.routing_lab.args=" + json.dumps(env_args, separators=(",", ":")),
        render_prompt(fixture),
    ]


def run_process(argv: list[str], cwd: Path, out: Path, err: Path, timeout: int = 240) -> dict[str, Any]:
    started_ns = time.time_ns()
    started = False
    exit_code = None
    timed_out = False
    env = os.environ.copy()
    env.pop("OPENAI_API_KEY", None)
    with out.open("w", encoding="utf-8") as stdout, err.open("w", encoding="utf-8") as stderr:
        process = subprocess.Popen(argv, cwd=cwd, stdout=stdout, stderr=stderr, text=True, env=env)
        started = True
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
    return {"started": started, "started_ns": started_ns, "ended_ns": time.time_ns(), "exit_code": exit_code, "timed_out": timed_out}


def action_items(events: list[dict[str, Any]]) -> list[dict[str, Any]]:
    rows = []
    for event in events:
        if event.get("type") != "item.completed":
            continue
        item = event.get("item", {})
        if item.get("type") in {"mcp_tool_call", "file_change", "command_execution"}:
            rows.append(item)
    return rows


def structural_tool(screen: str, arm: str) -> str:
    return "clj_surgeon" if screen == "action-native-name" and arm == "A" else "edit_clojure"


def route_and_handoff(screen: str, arm: str, events: list[dict[str, Any]], server_rows: list[dict[str, Any]]) -> dict[str, Any]:
    actions = action_items(events)
    first_route = "none"
    for item in actions:
        if item.get("type") == "file_change":
            first_route = "native"
            break
        if item.get("type") == "mcp_tool_call" and item.get("server") == "routing_lab":
            tool = item.get("tool")
            if tool == structural_tool(screen, arm):
                first_route = "structural"
                break
            if tool == "native_patch":
                first_route = "native"
                break
    refused = any(row.get("event") == "native_refusal" for row in server_rows)
    immediate = False
    if refused:
        for index, item in enumerate(actions):
            if item.get("type") == "mcp_tool_call" and item.get("server") == "routing_lab" and item.get("tool") == "native_patch":
                if index + 1 < len(actions):
                    nxt = actions[index + 1]
                    immediate = nxt.get("type") == "mcp_tool_call" and nxt.get("server") == "routing_lab" and nxt.get("tool") == structural_tool(screen, arm)
                break
    eventual_structural = any(row.get("event") == "mutation_result" and row.get("name") == structural_tool(screen, arm) and row.get("ok") for row in server_rows)
    return {"primary_route": first_route, "native_refused": refused, "immediate_structural_handoff": immediate, "eventual_structural_mutation": eventual_structural, "action_count": len(actions)}


def score(screen: str, arm: str, fixture: dict[str, Any], run_dir: Path, workspace: Path, process: dict[str, Any], setup: dict[str, str]) -> dict[str, Any]:
    events = read_jsonl(run_dir / "events.jsonl")
    server_rows = read_jsonl(run_dir / "server.jsonl")
    target = workspace / fixture["file"]
    distractor = workspace / "src/acme/distractor.clj"
    expected = source_for(fixture, expected=True).encode()
    changed = run_capture(["git", "diff", "--name-only"], workspace)["stdout"].splitlines()
    wrong_subject = any(path != fixture["file"] for path in changed)
    semantic_correct = target.exists() and target.read_bytes() == expected and not wrong_subject
    rejected = [row for row in server_rows if row.get("event") == "call_rejected"]
    wrong_owner = any("owner" in str(row.get("reason", "")) for row in rejected)
    ready = next((row for row in server_rows if row.get("event") == "server_ready"), {})
    route = route_and_handoff(screen, arm, events, server_rows)
    usage = next((row.get("usage", {}) for row in reversed(events) if row.get("type") == "turn.completed"), {})
    row = {
        "schema": "routing-tranche-run-score.v1",
        "screen": screen,
        "run": run_dir.name,
        "arm": arm,
        "fixture": fixture["id"],
        "requested_model": MODEL,
        "reasoning_effort": EFFORT,
        "subscription_auth_preflight": True,
        "openai_api_key_absent": True,
        "process_started": process["started"],
        "process_exit_code": process["exit_code"],
        "timed_out": process["timed_out"],
        "environment_valid": bool(process["started"] and process["exit_code"] == 0 and ready.get("screen") == screen and ready.get("arm") == arm and ready.get("fixture") == fixture["id"]),
        "semantic_correct": semantic_correct,
        "wrong_subject": wrong_subject,
        "invalid_call": bool(rejected),
        "wrong_owner_edit": wrong_owner,
        "changed_paths": changed,
        "target_initial_sha256": setup["target_initial_sha256"],
        "target_final_sha256": sha_file(target) if target.exists() else None,
        "distractor_initial_sha256": setup["distractor_initial_sha256"],
        "distractor_final_sha256": sha_file(distractor) if distractor.exists() else None,
        "server_tool_list_sha256": ready.get("offered_tool_list_sha256"),
        "prompt_sha256": sha_bytes(render_prompt(fixture).encode()),
        "wall_ms": (process["ended_ns"] - process["started_ns"]) / 1_000_000,
        "usage": usage,
        **route,
    }
    atomic_json(run_dir / "process.json", process)
    atomic_json(run_dir / "score.json", row)
    (run_dir / "git.diff").write_text(run_capture(["git", "diff"], workspace)["stdout"], encoding="utf-8")
    return row


def run_one(screen: str, arm: str, fixture_id: str, run_number: int, root: Path) -> dict[str, Any]:
    require_freeze(screen)
    fixture = fixture_by_id(fixture_id)
    run_dir = root / f"{run_number:03d}-{arm}-{fixture_id}"
    if run_dir.exists():
        raise RuntimeError(f"refusing replacement: {run_dir}")
    run_dir.mkdir(parents=True)
    workspace = run_dir / "workspace"
    setup = reset_workspace(workspace, fixture)
    process = run_process(codex_argv(screen, arm, fixture, workspace, run_dir), workspace, run_dir / "events.jsonl", run_dir / "stderr.log")
    row = score(screen, arm, fixture, run_dir, workspace, process, setup)
    print(json.dumps({key: row[key] for key in ("screen", "run", "arm", "fixture", "primary_route", "environment_valid", "semantic_correct", "wrong_subject")}, sort_keys=True), flush=True)
    return row


def subscription_preflight() -> dict[str, Any]:
    if os.environ.get("OPENAI_API_KEY"):
        raise RuntimeError("OPENAI_API_KEY must be absent for subscription-only execution")
    version = run_capture([str(Path(shutil.which("codex") or "/missing/codex").resolve()), "--version"], ROOT)
    auth = run_capture([str(Path(shutil.which("codex") or "/missing/codex").resolve()), "login", "status"], ROOT)
    ok = version["exit_code"] == 0 and auth["exit_code"] == 0 and "chatgpt" in (auth["stdout"] + auth["stderr"]).lower()
    if not ok:
        raise RuntimeError("ChatGPT subscription preflight failed")
    return {"status": "ok", "codex_version": version["stdout"].strip(), "auth": (auth["stdout"] or auth["stderr"]).strip(), "openai_api_key_absent": True, "requested_model": MODEL, "reasoning_effort": EFFORT}


def run_rows(screen: str, rows: list[dict[str, Any]], root: Path) -> list[dict[str, Any]]:
    results = []
    for offset in range(0, len(rows), PARALLEL):
        batch = rows[offset:offset + PARALLEL]
        with concurrent.futures.ThreadPoolExecutor(max_workers=len(batch)) as pool:
            futures = [pool.submit(run_one, screen, row["arm"], row["fixture"], row["run"], root) for row in batch]
            results.extend(future.result() for future in futures)
    return results


def pilot(screen: str) -> None:
    require_freeze(screen)
    root = screen_dir(screen) / "pilot"
    if root.exists():
        raise RuntimeError("pilot is one-shot")
    preflight = subscription_preflight()
    rows = [{"run": 1, "fixture": "f01", "arm": "A"}, {"run": 2, "fixture": "f02", "arm": "A"}]
    scores = run_rows(screen, rows, root)
    structural = sum(row["primary_route"] == "structural" for row in scores)
    receipt = {"schema": "routing-tranche-control-pilot.v1", "screen": screen, "status": "sub-ceiling" if structural < 2 else "ceiling", "control_runs": 2, "structural_first": structural, "all_environment_valid": all(row["environment_valid"] for row in scores), "wrong_subject": sum(bool(row["wrong_subject"]) for row in scores), "subscription_preflight": preflight, "scores": scores}
    atomic_json(screen_dir(screen) / "pilot.json", receipt)
    print(json.dumps(receipt, sort_keys=True))


def require_pilot(screen: str) -> dict[str, Any]:
    path = screen_dir(screen) / "pilot.json"
    if not path.exists():
        raise RuntimeError("2-run control pilot required")
    row = json.loads(path.read_text(encoding="utf-8"))
    if row.get("status") != "sub-ceiling" or row.get("wrong_subject") != 0 or not row.get("all_environment_valid"):
        raise RuntimeError("control pilot did not establish valid sub-ceiling routing")
    return row


def cohort(screen: str, replication: bool = False) -> None:
    require_freeze(screen)
    require_pilot(screen)
    if replication and screen != "action-native-name":
        raise RuntimeError("replication stage exists only for action-native-name")
    if replication:
        aggregate = json.loads((screen_dir(screen) / "aggregate.json").read_text(encoding="utf-8"))
        if aggregate.get("verdict") != "advance-to-fresh-replication":
            raise RuntimeError("initial naming gate did not advance")
    root = screen_dir(screen) / ("replication-runs" if replication else "runs")
    if root.exists():
        raise RuntimeError("cohort is one-shot")
    rows = schedule(replication)
    scores = run_rows(screen, rows, root)
    atomic_json(screen_dir(screen) / ("replication-attempts.json" if replication else "attempts.json"), scores)
    summarize(screen, replication)


def rates(rows: list[dict[str, Any]], field: str) -> tuple[int, int, float]:
    return sum(bool(row[field]) for row in rows), len(rows), (sum(bool(row[field]) for row in rows) / len(rows) if rows else 0.0)


def summarize(screen: str, replication: bool = False) -> dict[str, Any]:
    root = screen_dir(screen) / ("replication-runs" if replication else "runs")
    scores = [json.loads(path.read_text(encoding="utf-8")) for path in sorted(root.glob("*/score.json"))]
    by_arm = {arm: [row for row in scores if row["arm"] == arm] for arm in "AB"}
    structural = {arm: sum(row["primary_route"] == "structural" for row in rows) for arm, rows in by_arm.items()}
    success = {arm: sum(bool(row["semantic_correct"]) for row in rows) for arm, rows in by_arm.items()}
    invalid = {arm: sum(bool(row["invalid_call"]) for row in rows) for arm, rows in by_arm.items()}
    wrong_owner = {arm: sum(bool(row["wrong_owner_edit"]) for row in rows) for arm, rows in by_arm.items()}
    lift_pp = 100 * (structural["B"] / max(1, len(by_arm["B"])) - structural["A"] / max(1, len(by_arm["A"])))
    wrong_subject = sum(bool(row["wrong_subject"]) for row in scores)
    details: dict[str, Any] = {}
    if screen == "native-description":
        reversed_fixtures = 0
        fixture_rows = {}
        for fixture in FIXTURES:
            a = sum(row["primary_route"] == "structural" for row in by_arm["A"] if row["fixture"] == fixture["id"])
            b = sum(row["primary_route"] == "structural" for row in by_arm["B"] if row["fixture"] == fixture["id"])
            fixture_rows[fixture["id"]] = {"A": a, "B": b, "reversed": b < a}
            reversed_fixtures += b < a
        verdict = "killed" if lift_pp < 15 or reversed_fixtures > 4 else "clears-screen"
        details = {"reversing_fixtures": reversed_fixtures, "fixture_routes": fixture_rows}
    elif screen == "action-native-name":
        if replication:
            verdict = "clears-screen" if lift_pp >= 20 else "killed-on-fresh-replication"
        else:
            verdict = "killed" if lift_pp < 10 else "advance-to-fresh-replication"
    elif screen == "minimal-schema":
        invalid_increase = 100 * (invalid["B"] / 24 - invalid["A"] / 24)
        owner_increase = 100 * (wrong_owner["B"] / 24 - wrong_owner["A"] / 24)
        verdict = "killed" if lift_pp < 15 or invalid_increase > 5 or owner_increase > 5 else "clears-screen"
        details = {"invalid_call_increase_pp": invalid_increase, "wrong_owner_edit_increase_pp": owner_increase}
    else:
        refused = [row for row in by_arm["B"] if row["native_refused"]]
        immediate = sum(row["immediate_structural_handoff"] for row in refused)
        handoff_rate = immediate / len(refused) if refused else 0.0
        success_drop_pp = 100 * (success["A"] / 24 - success["B"] / 24)
        verdict = "killed" if handoff_rate < 0.70 or success_drop_pp > 10 else "clears-screen"
        details = {
            "voluntary_structural_first": structural,
            "refused_native_calls": len(refused),
            "immediate_structural_handoffs": immediate,
            "immediate_handoff_rate": handoff_rate,
            "forced_routing": sum(row["primary_route"] == "native" and row["native_refused"] and row["eventual_structural_mutation"] for row in by_arm["B"]),
            "success_drop_pp": success_drop_pp,
        }
    aggregate = {
        "schema": "routing-tranche-aggregate.v1",
        "screen": screen,
        "stage": "fresh-replication" if replication else "initial",
        "generated_at_ns": time.time_ns(),
        "attempts": len(scores),
        "pairs": len(scores) // 2,
        "prediction": PREDICTIONS[screen],
        "cells": {arm: {"attempts": len(by_arm[arm]), "structural_first": structural[arm], "semantic_correct": success[arm], "environment_valid": sum(row["environment_valid"] for row in by_arm[arm]), "invalid_call": invalid[arm], "wrong_owner_edit": wrong_owner[arm], "wrong_subject": sum(row["wrong_subject"] for row in by_arm[arm])} for arm in "AB"},
        "routing_lift_pp": lift_pp,
        "wrong_subject": wrong_subject,
        "verdict": "safety-fail-wrong-subject" if wrong_subject else verdict,
        "kill_criterion_applied": True,
        "details": details,
        "raw_streams": [str(path.relative_to(ROOT)) for path in sorted(root.glob("*/events.jsonl"))],
    }
    path = screen_dir(screen) / ("replication-aggregate.json" if replication else "aggregate.json")
    atomic_json(path, aggregate)
    print(json.dumps(aggregate, sort_keys=True))
    return aggregate


def archive(screen: str) -> None:
    require_freeze(screen)
    directory = screen_dir(screen)
    manifest_paths = sorted(path for path in directory.rglob("*") if path.is_file() and path.name not in {"artifact-manifest.sha256", "archive-receipt.json"} and "workspace/.git" not in str(path))
    manifest = "".join(f"{sha_file(path)}  {path.relative_to(directory)}\n" for path in manifest_paths)
    (directory / "artifact-manifest.sha256").write_text(manifest, encoding="utf-8")
    archive_path = directory / f"{screen}-raw-streams.tar.gz"
    with archive_path.open("wb") as raw:
        import gzip
        with gzip.GzipFile(filename="", mode="wb", fileobj=raw, mtime=0) as zipped:
            with tarfile.open(fileobj=zipped, mode="w") as tar:
                for path in manifest_paths:
                    info = tar.gettarinfo(str(path), arcname=str(path.relative_to(directory)))
                    info.mtime = 0
                    info.uid = info.gid = 0
                    info.uname = info.gname = ""
                    with path.open("rb") as handle:
                        tar.addfile(info, handle)
    receipt = {"schema": "routing-tranche-archive.v1", "screen": screen, "archive": archive_path.name, "archive_sha256": sha_file(archive_path), "manifest_sha256": sha_file(directory / "artifact-manifest.sha256"), "manifest_entries": len(manifest_paths)}
    atomic_json(directory / "archive-receipt.json", receipt)
    print(json.dumps(receipt, sort_keys=True))


def self_test() -> None:
    assert len(FIXTURES) == 12 and len(schedule()) == 48
    assert all(sum(row["arm"] == arm for row in schedule()) == 24 for arm in "AB")
    assert all(sum(row["fixture"] == fixture["id"] and row["arm"] == arm for row in schedule()) == 2 for fixture in FIXTURES for arm in "AB")
    for fixture in FIXTURES:
        source = source_for(fixture)
        assert source.count(fixture["old"]) == 1
        assert source.replace(fixture["old"], fixture["new"], 1) == source_for(fixture, True)
    print(json.dumps({"schema": "routing-tranche-self-test.v1", "status": "ok", "assertions": 40}))


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("command", choices=["self-test", "freeze", "pilot", "cohort", "replicate", "summarize", "archive"])
    parser.add_argument("--screen", choices=SCREENS)
    args = parser.parse_args()
    if args.command == "self-test":
        self_test()
        return
    if not args.screen:
        parser.error("--screen is required")
    if args.command == "freeze":
        freeze(args.screen)
    elif args.command == "pilot":
        pilot(args.screen)
    elif args.command == "cohort":
        cohort(args.screen)
    elif args.command == "replicate":
        cohort(args.screen, True)
    elif args.command == "summarize":
        summarize(args.screen)
    else:
        archive(args.screen)


if __name__ == "__main__":
    main()
