#!/usr/bin/env python3
"""Run preregistered retained replacements for an invalid Spark cold bang."""

from __future__ import annotations

import argparse
import importlib.util
import os
import shutil
from pathlib import Path


RUNNER = Path(__file__).with_name("run_warm_executor_screen.py")
SPEC = importlib.util.spec_from_file_location("warm_executor_screen", RUNNER)
assert SPEC and SPEC.loader
screen = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(screen)


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--out", required=True)
    parser.add_argument("--auth-file")
    args = parser.parse_args()

    repo = Path(__file__).resolve().parents[1]
    out = Path(args.out).resolve()
    if out.exists():
        raise RuntimeError(f"replacement output already exists: {out}")
    auth_file = Path(
        args.auth_file
        or os.getenv("WARM_EXECUTOR_AUTH_FILE")
        or Path.home() / ".codex/auth.json"
    ).resolve()
    codex = shutil.which("codex")
    if not codex:
        raise RuntimeError("codex is not on PATH")
    prereg = screen.validate_prerun(repo, auth_file, codex)
    out.mkdir(parents=True)
    template_dir = repo / "bench/fixtures/warm_executor"
    template_text = (template_dir / screen.FIXTURE_REL).read_text()
    workspace = out / "workspace"
    screen.prepare_workspace(template_dir, workspace)
    mcp = screen.McpServer(repo, workspace, out / "mcp", "spark-cold-replacement")
    rows = []
    try:
        for replicate in (6, 7):
            screen.reset_fixture(template_dir, workspace)
            trial_dir = out / f"r{replicate:02d}"
            home = screen.make_codex_home(trial_dir / "codex-home", auth_file, mcp.url)
            before = screen.tree_hashes(workspace)
            app = screen.AppServer(
                codex, home, trial_dir, screen.MODELS[0], workspace
            )
            try:
                thread = app.start_thread("workspace-write")
                turn = app.turn(
                    thread["thread_id"], screen.prepared_prompt(workspace, 1)
                )
                after = screen.tree_hashes(workspace)
                score = screen.score_prepared_turn(
                    turn, workspace, 1, template_text, before, after
                )
                row = {
                    "model": screen.MODELS[0],
                    "replicate": replicate,
                    "replacement_for": 2,
                    "process_bootstrap_ms": (
                        app.initialize_arrival_ns - app.process_start_ns
                    )
                    / 1_000_000,
                    "thread_setup_ms": thread["thread_setup_ms"],
                    "total_e2e_ms": (
                        turn["completed_ns"] - app.process_start_ns
                    )
                    / 1_000_000,
                    **screen.public_turn_timing(turn),
                    "score": score,
                    "load_average": screen.load_average(),
                }
                screen.write_json(trial_dir / "score.json", row)
                (trial_dir / "after.clj").write_text(
                    (workspace / screen.FIXTURE_REL).read_text()
                )
                screen.append_jsonl(out / "replacements.jsonl", row)
                rows.append(row)
            finally:
                app.stop()
                screen.discard_codex_home(home)
            if row["score"]["exact"]:
                break
    finally:
        mcp.stop()
    screen.write_json(
        out / "meta.json",
        {
            "schema": "clj-surgeon.warm-executor-replacement/v1",
            "preregistration": prereg,
            "replacement_for": {
                "model": screen.MODELS[0],
                "replicate": 2,
                "reason": "MCP catalog unavailable; no tool call and no mutation",
            },
            "maximum_attempts": 2,
            "attempts_used": len(rows),
            "valid_replacement_obtained": any(row["score"]["exact"] for row in rows),
            "runner_sha256": screen.sha256_file(Path(__file__)),
            "base_runner_sha256": screen.sha256_file(RUNNER),
        },
    )
    return 0 if any(row["score"]["exact"] for row in rows) else 1


if __name__ == "__main__":
    raise SystemExit(main())
