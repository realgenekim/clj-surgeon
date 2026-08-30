#!/usr/bin/env python3
"""Freeze, run, score, and archive the ordinal refusal recovery screen."""

from __future__ import annotations

import argparse
import hashlib
import json
import os
import shutil
import statistics
import subprocess
import sys
import tarfile
import time
from pathlib import Path
from typing import Any

import proxy


ROOT = Path(__file__).resolve().parent
RESULTS = ROOT / "results"
RAW = RESULTS / "raw"
ARCHIVES = RESULTS / "archives"
PRIVATE_AUTH = RESULTS / "private-auth"
FREEZE = RESULTS / "freeze.json"
PILOT_RECEIPT = RESULTS / "pilot-receipt.json"
RESULT_RECEIPT = RESULTS / "result-receipt.json"
AGGREGATE = RESULTS / "aggregate.json"
MANIFEST = RESULTS / "artifact-manifest.sha256"
PROXY = ROOT / "proxy.py"
PROMPT = ROOT / "prompt.txt"
INITIAL = ROOT / "fixture" / "initial"
EXPECTED = ROOT / "fixture" / "expected"
TARGET = Path(proxy.TARGET_FILE)
MODEL = "gpt-5.6-sol"
REASONING = "high"
PILOT_SCHEDULE = ["C", "T", "T", "C"]
COHORT_SCHEDULE = ["C", "T", "T", "C"] * 6
MIN_VALID_PER_ARM = 8
TIMEOUT_SECONDS = 360
CODEX = Path(shutil.which("codex") or "/missing/codex").resolve()
PYTHON = Path(sys.executable).resolve()
AUTH_SOURCE = Path(
    os.environ.get("CODEX_HOME", str(Path.home() / ".codex"))
) / "auth.json"


def canonical_bytes(value: Any) -> bytes:
    return json.dumps(
        value, sort_keys=True, separators=(",", ":"), ensure_ascii=False
    ).encode("utf-8")


def sha_bytes(value: bytes) -> str:
    return hashlib.sha256(value).hexdigest()


def sha_file(path: Path) -> str:
    return sha_bytes(path.read_bytes())


def atomic_json(path: Path, value: Any) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    temporary = path.with_suffix(path.suffix + ".tmp")
    temporary.write_text(
        json.dumps(value, indent=2, sort_keys=True, ensure_ascii=False) + "\n",
        encoding="utf-8",
    )
    temporary.replace(path)


def append_jsonl(path: Path, value: Any) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("a", encoding="utf-8") as handle:
        handle.write(json.dumps(value, sort_keys=True, ensure_ascii=False) + "\n")
        handle.flush()
        os.fsync(handle.fileno())


def read_jsonl(path: Path) -> list[dict[str, Any]]:
    if not path.exists():
        return []
    return [json.loads(line) for line in path.read_text(encoding="utf-8").splitlines() if line]


def static_paths() -> list[Path]:
    paths = [
        ROOT / "README.md",
        ROOT / "preregistration.md",
        PROMPT,
        PROXY,
        ROOT / "run_experiment.py",
        ROOT / "test_screen.py",
    ]
    paths.extend(sorted(path for path in INITIAL.rglob("*") if path.is_file()))
    paths.extend(sorted(path for path in EXPECTED.rglob("*") if path.is_file()))
    return paths


def static_hashes() -> dict[str, str]:
    return {str(path.relative_to(ROOT)): sha_file(path) for path in static_paths()}


def run_capture(argv: list[str], cwd: Path = ROOT, env: dict[str, str] | None = None) -> dict[str, Any]:
    completed = subprocess.run(
        argv,
        cwd=cwd,
        env=env,
        text=True,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        check=False,
    )
    return {
        "argv": argv,
        "exit_code": completed.returncode,
        "stdout": completed.stdout,
        "stderr": completed.stderr,
    }


def self_test() -> dict[str, Any]:
    initial_source = (INITIAL / TARGET).read_text(encoding="utf-8")
    expected_source = (EXPECTED / TARGET).read_text(encoding="utf-8")
    owners = proxy.OWNER_PATTERN.findall(initial_source)
    checks = {
        "codex_present": CODEX.is_file(),
        "auth_present": AUTH_SOURCE.is_file(),
        "fixture_owner_count_27": len(owners) == 27,
        "target_ordinal_19": owners[18] == proxy.TARGET_OWNER,
        "near_miss_absent": proxy.NEAR_MISS not in owners,
        "initial_pending_count_27": initial_source.count(proxy.FIND) == 27,
        "expected_one_ready": expected_source.count(proxy.REPLACE) == 1,
        "expected_pending_count_26": expected_source.count(proxy.FIND) == 26,
        "single_bytewise_effect": initial_source.replace(
            ":screen :operations-dashboard :status :pending",
            ":screen :operations-dashboard :status :ready",
        )
        == expected_source,
        "pilot_interleaved": PILOT_SCHEDULE == ["C", "T", "T", "C"],
        "cohort_balanced": COHORT_SCHEDULE.count("C") == COHORT_SCHEDULE.count("T") == 12,
        "catalog_has_three_tools": [
            row["name"] for row in proxy.tool_catalog()["tools"]
        ]
        == [
            "apply_clojure_changes",
            "inspect_clojure",
            "experiment_confirm_prepared_request",
        ],
        "experiment_notice_names_product_boundary": "not product" in proxy.EXPERIMENT_NOTICE,
    }
    unit = run_capture([str(PYTHON), "-B", "-m", "unittest", "-v"], ROOT)
    checks["unit_tests"] = unit["exit_code"] == 0
    receipt = {
        "schema": "ordinal-refusal-screen-self-test.v1",
        "status": "ok" if all(checks.values()) else "failed",
        "checks": checks,
        "unit_test_stdout": unit["stdout"],
        "unit_test_stderr": unit["stderr"],
    }
    atomic_json(RESULTS / "self-test.json", receipt)
    if receipt["status"] != "ok":
        raise RuntimeError(f"self-test failed: {[key for key, value in checks.items() if not value]}")
    return receipt


def freeze() -> dict[str, Any]:
    if FREEZE.exists():
        raise RuntimeError("freeze.json already exists; freeze is one-shot")
    test_receipt = self_test()
    auth = run_capture([str(CODEX), "login", "status"])
    version = run_capture([str(CODEX), "--version"])
    if auth["exit_code"] != 0 or "chatgpt" not in (auth["stdout"] + auth["stderr"]).lower():
        raise RuntimeError("ChatGPT subscription authentication preflight failed")
    receipt = {
        "schema": "ordinal-refusal-screen-freeze.v1",
        "status": "frozen",
        "frozen_at_ns": time.time_ns(),
        "experiment_only": True,
        "product_contract": False,
        "experiment_notice": proxy.EXPERIMENT_NOTICE,
        "evidence_commits": {
            "complete_vocabulary_sweep": "c1e89d5d1b1f23d1655ef82f941a9d7be5624713",
            "write_refusal_packet": "6d558cb3b5859cce6626fb67225c547483dc646f",
        },
        "model": MODEL,
        "reasoning": REASONING,
        "timeout_seconds": TIMEOUT_SECONDS,
        "pilot_schedule": PILOT_SCHEDULE,
        "cohort_schedule": COHORT_SCHEDULE,
        "minimum_valid_per_arm": MIN_VALID_PER_ARM,
        "predictions": {
            "t_mean_recovery_reads": [0.0, 0.25],
            "c_mean_recovery_reads": [0.75, 1.25],
            "predicted_read_reduction": [0.875, 1.0],
            "t_index_hole_fill_min_fraction": 0.75,
            "t_output_token_reduction": [0.35, 0.60],
            "t_wall_reduction": [0.25, 0.45],
            "wrong_subject_count": 0,
        },
        "kill_rules": {
            "minimum_recovery_read_reduction": 0.50,
            "minimum_t_index_hole_fill_fraction": 0.50,
            "wrong_subject_count": 0,
        },
        "static_hashes": static_hashes(),
        "fixture": {
            "owner_count": 27,
            "target_owner": proxy.TARGET_OWNER,
            "target_ordinal_one_based": 19,
            "initial_sha256": sha_file(INITIAL / TARGET),
            "expected_sha256": sha_file(EXPECTED / TARGET),
        },
        "codex_version": version["stdout"].strip(),
        "subscription_auth_preflight": True,
        "self_test_sha256": sha_file(RESULTS / "self-test.json"),
        "self_test_status": test_receipt["status"],
    }
    atomic_json(FREEZE, receipt)
    return receipt


def require_freeze() -> dict[str, Any]:
    if not FREEZE.is_file():
        raise RuntimeError("freeze.json is required")
    receipt = json.loads(FREEZE.read_text(encoding="utf-8"))
    if receipt.get("status") != "frozen":
        raise RuntimeError("freeze receipt is not frozen")
    if receipt.get("static_hashes") != static_hashes():
        raise RuntimeError("frozen static input drift; stop before the next token")
    if receipt.get("model") != MODEL or receipt.get("reasoning") != REASONING:
        raise RuntimeError("model stratum drift")
    return receipt


def exact_first_arguments() -> dict[str, Any]:
    return {
        "changes": [
            {
                "files": [proxy.TARGET_FILE],
                "forms": [proxy.NEAR_MISS],
                "find": proxy.FIND,
                "replace": proxy.REPLACE,
                "expect": {"matches": 1},
            }
        ],
        "mode": "execute",
    }


def codex_argv(arm: str, workspace: Path, log_path: Path) -> list[str]:
    server_args = [
        f"ORDINAL_SCREEN_ARM={arm}",
        f"ORDINAL_SCREEN_WORKSPACE={workspace.resolve()}",
        f"ORDINAL_SCREEN_LOG={log_path.resolve()}",
        str(PYTHON),
        str(PROXY),
    ]
    return [
        str(CODEX),
        "exec",
        "--json",
        "--ephemeral",
        "--ignore-user-config",
        "--strict-config",
        "-m",
        MODEL,
        "-c",
        f'model_reasoning_effort="{REASONING}"',
        "--dangerously-bypass-approvals-and-sandbox",
        "-C",
        str(workspace.resolve()),
        "-c",
        'mcp_servers.ordinal-screen.command="/usr/bin/env"',
        "-c",
        "mcp_servers.ordinal-screen.args="
        + json.dumps(server_args, separators=(",", ":")),
        PROMPT.read_text(encoding="utf-8"),
    ]


def private_codex_home(label: str) -> Path:
    home = PRIVATE_AUTH / label
    if home.exists():
        raise RuntimeError(f"private auth home already exists: {home}")
    home.mkdir(parents=True, mode=0o700)
    shutil.copy2(AUTH_SOURCE, home / "auth.json")
    os.chmod(home / "auth.json", 0o600)
    return home


def run_process(
    argv: list[str], cwd: Path, stdout_path: Path, stderr_path: Path, env: dict[str, str]
) -> dict[str, Any]:
    started_ns = time.time_ns()
    timed_out = False
    exit_code: int | None = None
    with stdout_path.open("w", encoding="utf-8") as out, stderr_path.open(
        "w", encoding="utf-8"
    ) as err:
        process = subprocess.Popen(
            argv,
            cwd=cwd,
            env=env,
            stdin=subprocess.DEVNULL,
            stdout=out,
            stderr=err,
            text=True,
        )
        try:
            exit_code = process.wait(timeout=TIMEOUT_SECONDS)
        except subprocess.TimeoutExpired:
            timed_out = True
            process.terminate()
            try:
                exit_code = process.wait(timeout=10)
            except subprocess.TimeoutExpired:
                process.kill()
                exit_code = process.wait(timeout=10)
    return {
        "started": True,
        "started_ns": started_ns,
        "ended_ns": time.time_ns(),
        "exit_code": exit_code,
        "timed_out": timed_out,
    }


def item_structured(item: dict[str, Any]) -> dict[str, Any]:
    result = item.get("result")
    if not isinstance(result, dict):
        return {}
    structured = result.get("structured_content", result.get("structuredContent", {}))
    return structured if isinstance(structured, dict) else {}


def recovery_emission_bytes(events: list[dict[str, Any]]) -> int | None:
    completed_items = [
        row["item"]
        for row in events
        if row.get("type") == "item.completed" and isinstance(row.get("item"), dict)
    ]
    refusal_index: int | None = None
    for index, item in enumerate(completed_items):
        if (
            item.get("type") == "mcp_tool_call"
            and item.get("tool") == "apply_clojure_changes"
            and item_structured(item).get("error_type") == "batch-form-selection-failed"
        ):
            refusal_index = index
            break
    if refusal_index is None:
        return None
    emitted = bytearray()
    for item in completed_items[refusal_index + 1 :]:
        kind = item.get("type")
        if kind == "agent_message" and isinstance(item.get("text"), str):
            emitted.extend(item["text"].encode("utf-8"))
        elif kind == "mcp_tool_call":
            emitted.extend(canonical_bytes(item.get("arguments", {})))
            structured = item_structured(item)
            if structured.get("ok") and structured.get("committed"):
                break
        elif kind == "command_execution" and isinstance(item.get("command"), str):
            emitted.extend(item["command"].encode("utf-8"))
    return len(emitted)


def score_episode(
    phase: str,
    position: int,
    arm: str,
    run_dir: Path,
    process: dict[str, Any],
    auth_removed: bool,
) -> dict[str, Any]:
    events = read_jsonl(run_dir / "events.jsonl")
    server = read_jsonl(run_dir / "proxy.jsonl")
    calls = [row for row in server if row.get("event") == "client_tool_call"]
    refusals = [row for row in server if row.get("event") == "controlled_refusal"]
    mutations = [row for row in server if row.get("event") == "mutation_committed"]
    reads = [row for row in server if row.get("event") == "recovery_read"]
    ready = [row for row in server if row.get("event") == "proxy_ready"]
    target_mutations = [row for row in mutations if row.get("owner") == proxy.TARGET_OWNER]
    success = target_mutations[0] if target_mutations else None
    refusal = refusals[0] if refusals else None
    reads_before_success = []
    if refusal is not None and success is not None:
        reads_before_success = [
            row for row in reads if refusal["ts_ns"] < row["ts_ns"] < success["ts_ns"]
        ]
    wrong_subject = any(row.get("owner") != proxy.TARGET_OWNER for row in mutations)
    final_path = run_dir / "workspace" / TARGET
    final_sha = sha_file(final_path) if final_path.is_file() else None
    expected_sha = sha_file(EXPECTED / TARGET)
    semantic_correct = final_sha == expected_sha
    first_call_exact = bool(
        calls
        and calls[0].get("name") == "apply_clojure_changes"
        and calls[0].get("arguments") == exact_first_arguments()
    )
    command_items = [
        row
        for row in events
        if row.get("type") in {"item.started", "item.completed"}
        and isinstance(row.get("item"), dict)
        and row["item"].get("type") == "command_execution"
    ]
    usage_rows = [row.get("usage", {}) for row in events if row.get("type") == "turn.completed"]
    usage = usage_rows[-1] if usage_rows else {}
    environment_valid = bool(
        process.get("started")
        and process.get("exit_code") == 0
        and not process.get("timed_out")
        and len(refusals) == 1
        and len(ready) == 1
        and usage_rows
        and auth_removed
    )
    route_adherent = first_call_exact and not command_items
    fully_valid = environment_valid and semantic_correct and route_adherent
    recovery_mode = None
    if success is not None:
        recovery_mode = (
            "index-hole-fill"
            if success.get("route") == "experiment_confirm_prepared_request"
            else "retyped-corrected-request"
        )
    return {
        "schema": "ordinal-refusal-screen-episode.v1",
        "phase": phase,
        "position": position,
        "arm": arm,
        "experiment_only": True,
        "product_contract": False,
        "experiment_notice": proxy.EXPERIMENT_NOTICE,
        "model": MODEL,
        "reasoning": REASONING,
        "process": process,
        "environment_valid": environment_valid,
        "semantic_correct": semantic_correct,
        "route_adherent": route_adherent,
        "fully_valid": fully_valid,
        "controlled_refusal_count": len(refusals),
        "recovery_reads_before_success": len(reads_before_success),
        "recovery_mode": recovery_mode,
        "successful_mutation": success is not None and semantic_correct,
        "mutation_count": len(mutations),
        "mutated_owners": [row.get("owner") for row in mutations],
        "wrong_subject": wrong_subject,
        "refusal_to_success_ms": (
            (success["ts_ns"] - refusal["ts_ns"]) / 1_000_000
            if refusal is not None and success is not None
            else None
        ),
        "output_tokens": usage.get("output_tokens"),
        "reasoning_output_tokens": usage.get("reasoning_output_tokens"),
        "turn_input_tokens": usage.get("input_tokens"),
        "post_refusal_emitted_bytes": recovery_emission_bytes(events),
        "first_call_exact": first_call_exact,
        "command_execution_count": len(command_items) // 2,
        "initial_sha256": sha_file(INITIAL / TARGET),
        "final_sha256": final_sha,
        "expected_sha256": expected_sha,
        "events_sha256": sha_file(run_dir / "events.jsonl"),
        "proxy_log_sha256": sha_file(run_dir / "proxy.jsonl") if (run_dir / "proxy.jsonl").is_file() else None,
        "stderr_sha256": sha_file(run_dir / "stderr.log"),
        "credential_home_removed": auth_removed,
    }


def run_episode(phase: str, position: int, arm: str) -> dict[str, Any]:
    label = f"{phase}-{position:03d}-{arm.lower()}"
    run_dir = RAW / label
    if run_dir.exists():
        raise RuntimeError(f"episode directory already exists: {run_dir}")
    workspace = run_dir / "workspace"
    run_dir.mkdir(parents=True)
    shutil.copytree(INITIAL, workspace)
    auth_home = private_codex_home(label)
    env = os.environ.copy()
    env["CODEX_HOME"] = str(auth_home)
    env.pop("OPENAI_API_KEY", None)
    argv = codex_argv(arm, workspace, run_dir / "proxy.jsonl")
    launch = {
        "schema": "ordinal-refusal-screen-launch.v1",
        "phase": phase,
        "position": position,
        "arm": arm,
        "model": MODEL,
        "reasoning": REASONING,
        "argv_sha256": sha_bytes(canonical_bytes(argv)),
        "prompt_sha256": sha_file(PROMPT),
        "freeze_sha256": sha_file(FREEZE),
        "initial_sha256": sha_file(workspace / TARGET),
        "experiment_only": True,
        "product_contract": False,
        "experiment_notice": proxy.EXPERIMENT_NOTICE,
    }
    atomic_json(run_dir / "launch.json", launch)
    process = run_process(
        argv, workspace, run_dir / "events.jsonl", run_dir / "stderr.log", env
    )
    shutil.rmtree(auth_home)
    auth_removed = not auth_home.exists()
    score = score_episode(phase, position, arm, run_dir, process, auth_removed)
    atomic_json(run_dir / "score.json", score)
    append_jsonl(RESULTS / f"{phase}-attempts.jsonl", score)
    return score


def median(values: list[Any]) -> float | None:
    numeric = [float(value) for value in values if isinstance(value, (int, float))]
    return statistics.median(numeric) if numeric else None


def mean(values: list[Any]) -> float | None:
    numeric = [float(value) for value in values if isinstance(value, (int, float))]
    return statistics.mean(numeric) if numeric else None


def pilot() -> dict[str, Any]:
    require_freeze()
    if PILOT_RECEIPT.exists() or (RESULTS / "pilot-attempts.jsonl").exists():
        raise RuntimeError("pilot is one-shot")
    rows: list[dict[str, Any]] = []
    stopped_reason = None
    for position, arm in enumerate(PILOT_SCHEDULE, start=1):
        row = run_episode("pilot", position, arm)
        rows.append(row)
        if row["wrong_subject"]:
            stopped_reason = "wrong-subject-kill"
            break
    released = bool(
        len(rows) == len(PILOT_SCHEDULE)
        and all(row["fully_valid"] for row in rows)
        and not any(row["wrong_subject"] for row in rows)
    )
    receipt = {
        "schema": "ordinal-refusal-screen-pilot.v1",
        "status": "passed" if released else "stopped",
        "released_main_cohort": released,
        "stopped_reason": stopped_reason,
        "experiment_only": True,
        "product_contract": False,
        "experiment_notice": proxy.EXPERIMENT_NOTICE,
        "schedule": PILOT_SCHEDULE,
        "launched": len(rows),
        "fully_valid_by_arm": {
            arm: sum(row["fully_valid"] for row in rows if row["arm"] == arm)
            for arm in ("C", "T")
        },
        "wrong_subject_count": sum(row["wrong_subject"] for row in rows),
        "scores": rows,
        "freeze_sha256": sha_file(FREEZE),
    }
    atomic_json(PILOT_RECEIPT, receipt)
    return receipt


def summarize(rows: list[dict[str, Any]]) -> dict[str, Any]:
    valid = [row for row in rows if row.get("fully_valid")]
    by_arm = {arm: [row for row in valid if row["arm"] == arm] for arm in ("C", "T")}
    c_reads = mean([row["recovery_reads_before_success"] for row in by_arm["C"]])
    t_reads = mean([row["recovery_reads_before_success"] for row in by_arm["T"]])
    read_reduction = (1 - (t_reads / c_reads)) if c_reads not in (None, 0) and t_reads is not None else None
    t_index_fraction = (
        sum(row["recovery_mode"] == "index-hole-fill" for row in by_arm["T"])
        / len(by_arm["T"])
        if by_arm["T"]
        else None
    )
    wrong_count = sum(row.get("wrong_subject", False) for row in rows)
    sufficient_n = all(len(by_arm[arm]) >= MIN_VALID_PER_ARM for arm in ("C", "T"))
    gates = {
        "sufficient_valid_n": sufficient_n,
        "recovery_read_reduction_at_least_50_percent": read_reduction is not None and read_reduction >= 0.50,
        "t_index_hole_fill_at_least_half": t_index_fraction is not None and t_index_fraction >= 0.50,
        "wrong_subject_zero": wrong_count == 0,
    }
    return {
        "schema": "ordinal-refusal-screen-aggregate.v1",
        "experiment_only": True,
        "product_contract": False,
        "experiment_notice": proxy.EXPERIMENT_NOTICE,
        "launched": len(rows),
        "fully_valid": len(valid),
        "fully_valid_by_arm": {arm: len(by_arm[arm]) for arm in ("C", "T")},
        "wrong_subject_count": wrong_count,
        "per_protocol": {
            arm: {
                "n": len(by_arm[arm]),
                "mean_recovery_reads": mean(
                    [row["recovery_reads_before_success"] for row in by_arm[arm]]
                ),
                "recovery_read_values": [
                    row["recovery_reads_before_success"] for row in by_arm[arm]
                ],
                "median_output_tokens": median([row["output_tokens"] for row in by_arm[arm]]),
                "output_token_values": [row["output_tokens"] for row in by_arm[arm]],
                "median_post_refusal_emitted_bytes": median(
                    [row["post_refusal_emitted_bytes"] for row in by_arm[arm]]
                ),
                "post_refusal_emitted_byte_values": [
                    row["post_refusal_emitted_bytes"] for row in by_arm[arm]
                ],
                "median_refusal_to_success_ms": median(
                    [row["refusal_to_success_ms"] for row in by_arm[arm]]
                ),
                "refusal_to_success_ms_values": [
                    row["refusal_to_success_ms"] for row in by_arm[arm]
                ],
                "recovery_modes": {
                    mode: sum(row["recovery_mode"] == mode for row in by_arm[arm])
                    for mode in ("index-hole-fill", "retyped-corrected-request")
                },
                "semantic_correct": sum(row["semantic_correct"] for row in by_arm[arm]),
            }
            for arm in ("C", "T")
        },
        "effects": {
            "relative_recovery_read_reduction": read_reduction,
            "absolute_mean_recovery_read_difference_c_minus_t": (
                c_reads - t_reads if c_reads is not None and t_reads is not None else None
            ),
            "t_index_hole_fill_fraction": t_index_fraction,
            "median_output_token_reduction": (
                1
                - (
                    median([row["output_tokens"] for row in by_arm["T"]])
                    / median([row["output_tokens"] for row in by_arm["C"]])
                )
                if median([row["output_tokens"] for row in by_arm["C"]]) not in (None, 0)
                and median([row["output_tokens"] for row in by_arm["T"]]) is not None
                else None
            ),
            "median_wall_reduction": (
                1
                - (
                    median([row["refusal_to_success_ms"] for row in by_arm["T"]])
                    / median([row["refusal_to_success_ms"] for row in by_arm["C"]])
                )
                if median([row["refusal_to_success_ms"] for row in by_arm["C"]]) not in (None, 0)
                and median([row["refusal_to_success_ms"] for row in by_arm["T"]]) is not None
                else None
            ),
        },
        "gates": gates,
        "verdict": "prototype-screen-passes" if all(gates.values()) else "prototype-screen-killed",
        "itt_scores": rows,
    }


def cohort() -> dict[str, Any]:
    require_freeze()
    if not PILOT_RECEIPT.is_file():
        raise RuntimeError("pilot receipt is required")
    pilot_receipt = json.loads(PILOT_RECEIPT.read_text(encoding="utf-8"))
    if not pilot_receipt.get("released_main_cohort"):
        raise RuntimeError("pilot did not release the main cohort")
    if RESULT_RECEIPT.exists() or (RESULTS / "cohort-attempts.jsonl").exists():
        raise RuntimeError("cohort is one-shot")
    rows: list[dict[str, Any]] = []
    stopped_reason = None
    for position, arm in enumerate(COHORT_SCHEDULE, start=1):
        row = run_episode("cohort", position, arm)
        rows.append(row)
        if row["wrong_subject"]:
            stopped_reason = "wrong-subject-kill"
            break
        if position >= 16 and position % 4 == 0:
            valid_counts = {
                candidate: sum(
                    trial["fully_valid"] and trial["arm"] == candidate for trial in rows
                )
                for candidate in ("C", "T")
            }
            if all(valid_counts[candidate] >= MIN_VALID_PER_ARM for candidate in ("C", "T")):
                stopped_reason = "minimum-valid-complete"
                break
    aggregate = summarize(rows)
    aggregate["stopped_reason"] = stopped_reason
    aggregate["freeze_sha256"] = sha_file(FREEZE)
    aggregate["pilot_receipt_sha256"] = sha_file(PILOT_RECEIPT)
    atomic_json(AGGREGATE, aggregate)
    receipt = {
        "schema": "ordinal-refusal-screen-result.v1",
        "status": "complete",
        "experiment_only": True,
        "product_contract": False,
        "experiment_notice": proxy.EXPERIMENT_NOTICE,
        "verdict": aggregate["verdict"],
        "stopped_reason": stopped_reason,
        "launched": len(rows),
        "fully_valid_by_arm": aggregate["fully_valid_by_arm"],
        "wrong_subject_count": aggregate["wrong_subject_count"],
        "effects": aggregate["effects"],
        "gates": aggregate["gates"],
        "aggregate_sha256": sha_file(AGGREGATE),
        "freeze_sha256": sha_file(FREEZE),
        "pilot_receipt_sha256": sha_file(PILOT_RECEIPT),
    }
    atomic_json(RESULT_RECEIPT, receipt)
    return receipt


def archive() -> dict[str, Any]:
    require_freeze()
    if not RESULT_RECEIPT.is_file():
        raise RuntimeError("result receipt is required before archive")
    ARCHIVES.mkdir(parents=True, exist_ok=True)
    archive_path = ARCHIVES / "ordinal-refusal-screen-20260830.tar.gz"
    if archive_path.exists():
        raise RuntimeError("archive already exists")
    excluded_parts = {"archives", "private-auth", "__pycache__"}
    retained = [
        path
        for path in sorted(ROOT.rglob("*"))
        if path.is_file()
        and not any(part in excluded_parts for part in path.relative_to(ROOT).parts)
        and path != MANIFEST
    ]
    manifest_lines = [f"{sha_file(path)}  {path.relative_to(ROOT)}" for path in retained]
    MANIFEST.write_text("\n".join(manifest_lines) + "\n", encoding="utf-8")
    retained.append(MANIFEST)
    with tarfile.open(archive_path, "w:gz") as archive_handle:
        for path in retained:
            archive_handle.add(path, arcname=str(path.relative_to(ROOT)), recursive=False)
    receipt = {
        "schema": "ordinal-refusal-screen-archive.v1",
        "experiment_only": True,
        "product_contract": False,
        "experiment_notice": proxy.EXPERIMENT_NOTICE,
        "archive": str(archive_path.relative_to(ROOT)),
        "archive_sha256": sha_file(archive_path),
        "artifact_count": len(retained),
        "manifest": str(MANIFEST.relative_to(ROOT)),
        "manifest_sha256": sha_file(MANIFEST),
        "result_receipt_sha256": sha_file(RESULT_RECEIPT),
        "aggregate_sha256": sha_file(AGGREGATE),
        "credential_material_included": False,
    }
    atomic_json(ARCHIVES / "archive-receipt.json", receipt)
    return receipt


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "command", choices=["self-test", "freeze", "pilot", "cohort", "archive"]
    )
    args = parser.parse_args()
    result = {
        "self-test": self_test,
        "freeze": freeze,
        "pilot": pilot,
        "cohort": cohort,
        "archive": archive,
    }[args.command]()
    print(json.dumps(result, indent=2, sort_keys=True, ensure_ascii=False))


if __name__ == "__main__":
    main()
