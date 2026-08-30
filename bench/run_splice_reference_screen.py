#!/usr/bin/env python3
"""Run the frozen pilot and 8/arm splice-by-reference synthetic screen."""

from __future__ import annotations

import argparse
import datetime as dt
import hashlib
import json
import os
from pathlib import Path
import shutil
import signal
import subprocess
import sys
import tempfile
import time
from typing import Any

import tiktoken

from splice_reference_proxy import canonical_json, sha256_file
from splice_reference_screen import ideal_requests, score_episode, sha256_manifest, summarize


MODEL = "gpt-5.6-sol"
REASONING = "high"
PILOT = ["Q", "R"]
COHORT = ["Q", "R", "R", "Q", "R", "Q", "Q", "R",
          "Q", "R", "R", "Q", "R", "Q", "Q", "R"]


def run(command: list[str], cwd: Path) -> str:
    return subprocess.check_output(command, cwd=cwd, text=True).strip()


def file_sha(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def utc_now() -> str:
    return dt.datetime.now(dt.timezone.utc).isoformat()


def write_json(path: Path, value: Any) -> None:
    path.write_text(json.dumps(value, ensure_ascii=False, sort_keys=True, indent=2) + "\n",
                    encoding="utf-8")


def toml_string(value: str) -> str:
    return json.dumps(value)


def write_codex_config(path: Path, python: Path, proxy: Path, arm: str,
                       repo_root: Path, workspace: Path, manifest: Path,
                       run_dir: Path, run_id: str) -> None:
    proxy_args = [
        str(proxy), "--arm", arm,
        "--repo-root", str(repo_root),
        "--workspace", str(workspace),
        "--manifest", str(manifest),
        "--receipts", str(run_dir / "proxy-receipts.jsonl"),
        "--stream", str(run_dir / "proxy-stream.jsonl"),
        "--child-stderr", str(run_dir / "mcp-child.stderr"),
        "--telemetry-dir", str(run_dir / "mcp-telemetry"),
        "--run-id", run_id,
    ]
    rendered_args = ", ".join(toml_string(value) for value in proxy_args)
    path.write_text(
        "\n".join([
            "[mcp_servers.clj-surgeon]",
            f"command = {toml_string(str(python))}",
            f"args = [{rendered_args}]",
            "required = true",
            'enabled_tools = ["inspect_clojure", "edit_clojure"]',
            'default_tools_approval_mode = "approve"',
            "startup_timeout_sec = 120",
            "tool_timeout_sec = 120",
            "",
        ]),
        encoding="utf-8",
    )


def run_codex(command: list[str], env: dict[str, str], cwd: Path,
              stdout_path: Path, stderr_path: Path, timeout: int) -> tuple[int, float, bool]:
    started = time.monotonic()
    timed_out = False
    with stdout_path.open("w", encoding="utf-8") as stdout, \
         stderr_path.open("w", encoding="utf-8") as stderr:
        process = subprocess.Popen(
            command, cwd=cwd, env=env, stdin=subprocess.DEVNULL,
            stdout=stdout, stderr=stderr, text=True, start_new_session=True,
        )
        try:
            exit_code = process.wait(timeout=timeout)
        except subprocess.TimeoutExpired:
            timed_out = True
            os.killpg(process.pid, signal.SIGTERM)
            try:
                exit_code = process.wait(timeout=10)
            except subprocess.TimeoutExpired:
                os.killpg(process.pid, signal.SIGKILL)
                exit_code = process.wait(timeout=5)
    return exit_code, time.monotonic() - started, timed_out


def run_episode(index: int, phase: str, arm: str, output: Path, repo_root: Path,
                auth_file: Path, timeout: int, manifest: dict[str, Any]) -> dict[str, Any]:
    run_id = f"{index:02d}-{arm}"
    run_dir = output / phase / run_id
    fixture_root = repo_root / "bench/fixtures/splice_reference"
    workspace = run_dir / "workspace"
    run_dir.mkdir(parents=True)
    shutil.copytree(fixture_root / "before", workspace)
    shutil.copy2(fixture_root / "task.txt", run_dir / "prompt.txt")
    home = Path(tempfile.mkdtemp(prefix=f"splice-ref-{phase}-{run_id}-", dir="/private/tmp"))
    try:
        (home / "auth.json").symlink_to(auth_file)
        config = home / "config.toml"
        write_codex_config(
            config, Path(sys.executable).resolve(), repo_root / "bench/splice_reference_proxy.py",
            arm, repo_root, workspace, fixture_root / "spans.json", run_dir, run_id,
        )
        shutil.copy2(config, run_dir / "codex-config.toml")
        prompt = (fixture_root / "task.txt").read_text(encoding="utf-8")
        command = [
            "codex", "exec", "--json", "--ephemeral", "--ignore-rules",
            "--skip-git-repo-check", "--sandbox", "read-only", "--color", "never",
            "-m", MODEL, "-c", f'model_reasoning_effort="{REASONING}"',
            "-C", str(workspace), prompt,
        ]
        env = dict(os.environ)
        env["CODEX_HOME"] = str(home)
        env.pop("OPENAI_API_KEY", None)
        started_utc = utc_now()
        exit_code, wall, timed_out = run_codex(
            command, env, workspace, run_dir / "events.jsonl", run_dir / "stderr.txt", timeout,
        )
        episode = {
            "schema": "clj-surgeon.splice-reference-episode.v1",
            "run_id": run_id,
            "phase": phase,
            "arm": arm,
            "model": MODEL,
            "reasoning": REASONING,
            "subscription_route": True,
            "openai_api_key_removed": True,
            "started_utc": started_utc,
            "ended_utc": utc_now(),
            "wall_seconds": wall,
            "timeout_seconds": timeout,
            "timed_out": timed_out,
            "codex_exit_code": exit_code,
            "command": command[:-1] + ["<prompt from prompt.txt>"],
        }
        write_json(run_dir / "episode.json", episode)
        score = score_episode(
            run_dir, arm, manifest,
            fixture_root / "expected" / manifest["file"],
        )
        write_json(run_dir / "score.json", score)
        print(
            f"{phase} {run_id}: complete={score['completed_task']} "
            f"exact={score['exact_bytes']} refs={score['reference_count']} "
            f"edit_tokens={score['emitted']['mutation_tokens']} "
            f"wrong_subject={score['wrong_subject']}",
            flush=True,
        )
        return score
    finally:
        shutil.rmtree(home)


def pilot_summary(scores: list[dict[str, Any]], ideal: dict[str, Any]) -> dict[str, Any]:
    q, r = scores
    gate = {
        "zero_model_possible_token_reduction_at_least_25_percent":
            ideal["possible_reduction"]["tokens"] >= 0.25,
        "Q_completed_exact_conventional":
            q["completed_task"] and q["conventional_call_count"] >= 1
            and q["quoted_anchor_count"] >= 4,
        "R_completed_exact_strict_reference":
            r["completed_task"] and r["reference_used_strict"]
            and r["resolved_identity_count"] == 4,
        "observed_arms_differ":
            r["emitted"]["mutation_tokens"] < q["emitted"]["mutation_tokens"],
        "wrong_subject_zero": q["wrong_subject"] + r["wrong_subject"] == 0,
    }
    return {
        "schema": "clj-surgeon.splice-reference-pilot.v1",
        "order": PILOT,
        "ideal": ideal,
        "scores": scores,
        "sub_ceiling_gate": gate,
        "continue_to_cohort": all(gate.values()),
    }


def check_frozen(repo_root: Path, expected_head: str) -> dict[str, str]:
    head = run(["git", "rev-parse", "HEAD"], repo_root)
    tree = run(["git", "rev-parse", "HEAD^{tree}"], repo_root)
    status = run(["git", "status", "--porcelain"], repo_root)
    if head != expected_head:
        raise SystemExit(f"HEAD mismatch: expected {expected_head}, found {head}")
    if status:
        raise SystemExit("protocol checkout must be clean before the first model call")
    base = run(["git", "rev-parse", "origin/release/closed-relations-published"], repo_root)
    source_diff = subprocess.run(
        ["git", "diff", "--quiet", base, "--", "src", "test"], cwd=repo_root
    ).returncode
    if source_diff != 0:
        raise SystemExit("experiment branch changes product source or tests relative to release base")
    return {"head": head, "tree": tree, "release_base": base}


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--self-test", action="store_true")
    parser.add_argument("--run", action="store_true")
    parser.add_argument("--output", type=Path)
    parser.add_argument("--auth-file", type=Path)
    parser.add_argument("--expected-head")
    parser.add_argument("--timeout", type=int, default=240)
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    repo_root = Path(__file__).resolve().parent.parent
    if args.self_test:
        command = [sys.executable, "-m", "unittest", "-v", "bench/test_splice_reference_screen.py"]
        return subprocess.call(command, cwd=repo_root)
    if not args.run or args.output is None or args.auth_file is None or not args.expected_head:
        raise SystemExit("--run requires --output, --auth-file, and --expected-head")
    if args.output.exists():
        raise SystemExit("--output must not exist")
    if not args.auth_file.is_file():
        raise SystemExit("--auth-file must name an existing file")
    if args.timeout < 1:
        raise SystemExit("--timeout must be positive")
    frozen = check_frozen(repo_root, args.expected_head)
    fixture_root = repo_root / "bench/fixtures/splice_reference"
    manifest = json.loads((fixture_root / "spans.json").read_text(encoding="utf-8"))
    ideal = ideal_requests(manifest)
    if ideal["possible_reduction"]["tokens"] < 0.25:
        raise SystemExit("zero-model instrument cannot express the registered token effect")
    args.output.mkdir(parents=True)
    config = {
        "schema": "clj-surgeon.splice-reference-run-config.v1",
        **frozen,
        "created_utc": utc_now(),
        "model": MODEL,
        "reasoning": REASONING,
        "route": "codex exec ChatGPT subscription; OPENAI_API_KEY removed",
        "tokenizer": {
            "encoding": "o200k_base",
            "tiktoken_version": tiktoken.__version__,
        },
        "pilot_order": PILOT,
        "cohort_order": COHORT,
        "attempts_per_arm": 8,
        "hashes": {
            str(path.relative_to(repo_root)): file_sha(path)
            for path in [
                repo_root / "bench/run_splice_reference_screen.py",
                repo_root / "bench/splice_reference_proxy.py",
                repo_root / "bench/splice_reference_screen.py",
                fixture_root / "spans.json",
                fixture_root / "task.txt",
                fixture_root / "before" / manifest["file"],
                fixture_root / "expected" / manifest["file"],
                repo_root / "docs/observations/2026-08-30-splice-reference-screen-preregistration.md",
            ]
        },
        "codex_version": run(["codex", "--version"], repo_root),
        "python": sys.version,
        "ideal_requests": ideal,
    }
    write_json(args.output / "run-config.json", config)
    pilot_scores = [
        run_episode(index, "pilot", arm, args.output, repo_root, args.auth_file.resolve(),
                    args.timeout, manifest)
        for index, arm in enumerate(PILOT, start=1)
    ]
    pilot = pilot_summary(pilot_scores, ideal)
    write_json(args.output / "pilot-summary.json", pilot)
    if not pilot["continue_to_cohort"]:
        (args.output / "manifest.sha256").write_text(
            "\n".join(sha256_manifest(args.output)) + "\n", encoding="utf-8"
        )
        print("pilot gate refused cohort launch", flush=True)
        return 1
    print("pilot gate passed; launching frozen 16-run cohort", flush=True)
    cohort_scores = [
        run_episode(index, "cohort", arm, args.output, repo_root, args.auth_file.resolve(),
                    args.timeout, manifest)
        for index, arm in enumerate(COHORT, start=1)
    ]
    summary = summarize(cohort_scores, expected_attempts=8)
    summary["order"] = COHORT
    summary["registered_kill_thresholds"] = {
        "minimum_token_reduction": 0.25,
        "maximum_wrong_subject": 0,
        "minimum_strict_reference_use_rate_R": 0.5,
    }
    write_json(args.output / "cohort-summary.json", summary)
    (args.output / "manifest.sha256").write_text(
        "\n".join(sha256_manifest(args.output)) + "\n", encoding="utf-8"
    )
    print(canonical_json(summary), flush=True)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
