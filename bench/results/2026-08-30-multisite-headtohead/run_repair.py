#!/Users/genekim/anaconda3/bin/python
"""Run the separately registered config-repair cohort without replacing evidence."""

from __future__ import annotations

import argparse
import importlib.util
import json
import os
import shutil
import subprocess
import sys
import tempfile
import time
from pathlib import Path
from typing import Any


sys.dont_write_bytecode = True
HERE = Path(__file__).resolve().parent
BASE_PATH = HERE / "run_experiment.py"
SPEC = importlib.util.spec_from_file_location("multisite_base", BASE_PATH)
if SPEC is None or SPEC.loader is None:
    raise RuntimeError("cannot load frozen base runner")
base = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(base)

ADDENDUM = HERE / "preregistration-repair-addendum.md"
REPAIR_PREFLIGHT = HERE / "repair-preflight"
REPAIR_RUNS = HERE / "runs-repair"
REPAIR_AGGREGATE = HERE / "aggregate-repair.json"
REPAIR_TSV = HERE / "runs-repair.tsv"
REPAIR_BOUNDARY = HERE / "registration-boundary-repair.json"
REPAIR_SHA = HERE / "SHA256SUMS-repair"
EXPECTED_KILLED_COMMIT = "2236337c7922f219c53a3ed259086d360e7f7ce9"


FROZEN_BASE_CONFIG_SOURCE = base.config_source


def repair_config_source(arm: str, run_dir: Path, server_url: str | None) -> str:
    source = FROZEN_BASE_CONFIG_SOURCE(arm, run_dir, server_url)
    forbidden = "update_plan_enabled = false\n"
    if source.count(forbidden) != 1:
        raise RuntimeError("base config does not contain exactly one repair target")
    repaired = source.replace(forbidden, "")
    if len(source.encode("utf-8")) - len(repaired.encode("utf-8")) != len(
        forbidden.encode("utf-8")
    ):
        raise RuntimeError("repair changed more than the registered line")
    return repaired


base.config_source = repair_config_source
base.RUNS = REPAIR_RUNS


def write_json(path: Path, value: Any) -> None:
    base.write_json(path, value)


def config_env(home: Path, hook_log: Path) -> dict[str, str]:
    env = base.clean_child_env()
    env["CODEX_HOME"] = str(home)
    env["MULTISITE_HOOK_LOG"] = str(hook_log)
    return env


def validate_config(arm: str, source: str, workspace: Path, receipt_dir: Path) -> dict[str, Any]:
    with tempfile.TemporaryDirectory(prefix="multisite-repair-config-", dir="/private/tmp") as home_text:
        home = Path(home_text)
        (home / "auth.json").symlink_to(base.AUTH)
        (home / "config.toml").write_text(source, encoding="utf-8")
        result = base.run_capture(
            [str(base.CODEX), "debug", "models"],
            workspace,
            env=config_env(home, receipt_dir / f"{arm}-tool-hooks.jsonl"),
            timeout=60,
        )
        write_json(receipt_dir / f"{arm}-debug-models-process.json", result)
        (receipt_dir / f"{arm}-debug-models.stdout").write_text(result["stdout"], encoding="utf-8")
        (receipt_dir / f"{arm}-debug-models.stderr").write_text(result["stderr"], encoding="utf-8")
        if result["exit_code"] != 0:
            raise RuntimeError(f"{arm} config did not load: {result['stderr']}")
        catalog = json.loads(result["stdout"])
        models = catalog.get("models", catalog if isinstance(catalog, list) else [])
        matches = [model for model in models if model.get("slug") == base.MODEL]
        if len(matches) != 1:
            raise RuntimeError(f"{arm} did not resolve exactly one {base.MODEL}")
        expected = "freeform" if arm == "N" else None
        if matches[0].get("apply_patch_tool_type") != expected:
            raise RuntimeError(f"{arm} resolved wrong apply_patch tool type")
        mcp_command = [str(base.CODEX), "mcp", "list"]
        mcp = base.run_capture(mcp_command, workspace, env=config_env(home, receipt_dir / "unused"))
        write_json(receipt_dir / f"{arm}-mcp-list-process.json", mcp)
        (receipt_dir / f"{arm}-mcp-list.stdout").write_text(mcp["stdout"], encoding="utf-8")
        (receipt_dir / f"{arm}-mcp-list.stderr").write_text(mcp["stderr"], encoding="utf-8")
        if mcp["exit_code"] != 0:
            raise RuntimeError(f"{arm} MCP config did not load")
        has_surgeon = "clj-surgeon" in mcp["stdout"]
        if has_surgeon != (arm == "S"):
            raise RuntimeError(f"{arm} MCP presence mismatch")
        return {
            "config_sha256": base.sha_bytes(source.encode("utf-8")),
            "resolved_apply_patch_tool_type": expected,
            "mcp_server_present": has_surgeon,
            "debug_models_sha256": base.sha_bytes(result["stdout"].encode("utf-8")),
            "mcp_list_sha256": base.sha_bytes(mcp["stdout"].encode("utf-8")),
        }


def repair_preflight() -> None:
    if REPAIR_PREFLIGHT.exists():
        raise RuntimeError(f"refusing to replace repair preflight: {REPAIR_PREFLIGHT}")
    base.require_preflight()
    current = base.run_capture(["git", "rev-parse", "HEAD"], base.REPO)["stdout"].strip()
    if current != EXPECTED_KILLED_COMMIT:
        raise RuntimeError(f"repair preflight must start at killed cohort commit: {current}")
    REPAIR_PREFLIGHT.mkdir(parents=True)
    workspace = REPAIR_PREFLIGHT / "workspace"
    fixture_commit = base.materialize_workspace(workspace)
    server: subprocess.Popen[str] | None = None
    try:
        native_source = repair_config_source("N", REPAIR_PREFLIGHT, None)
        native = validate_config("N", native_source, workspace, REPAIR_PREFLIGHT)
        server, url = base.start_http_server(workspace, REPAIR_PREFLIGHT, "multisite-repair-preflight")
        surgeon_source = repair_config_source("S", REPAIR_PREFLIGHT, url)
        surgeon = validate_config("S", surgeon_source, workspace, REPAIR_PREFLIGHT)
    finally:
        base.stop_process(server)
    shutil.rmtree(workspace / ".git")
    (REPAIR_PREFLIGHT / "N-config.toml").write_text(native_source, encoding="utf-8")
    (REPAIR_PREFLIGHT / "S-config.toml").write_text(surgeon_source, encoding="utf-8")
    diff_lines = [
        line
        for line in surgeon_source.splitlines()
        if line not in native_source.splitlines()
    ]
    receipt = {
        "schema": "multisite-repair-preflight.v1",
        "status": "ok",
        "completed_at_ns": time.time_ns(),
        "killed_cohort_commit": current,
        "fixture_commit": fixture_commit,
        "addendum_sha256": base.sha_file(ADDENDUM),
        "runner_sha256": base.sha_file(Path(__file__).resolve()),
        "base_runner_sha256": base.sha_file(BASE_PATH),
        "removed_config_line": "update_plan_enabled = false",
        "native": native,
        "surgeon": surgeon,
        "surgeon_only_config_lines": diff_lines,
        "child_openai_environment_variables": [],
    }
    write_json(REPAIR_PREFLIGHT / "preflight.json", receipt)
    print(json.dumps(receipt, indent=2, sort_keys=True))


def require_repair_preflight() -> dict[str, Any]:
    path = REPAIR_PREFLIGHT / "preflight.json"
    if not path.is_file():
        raise RuntimeError("run and commit repair preflight before model calls")
    receipt = json.loads(path.read_text(encoding="utf-8"))
    if receipt.get("status") != "ok":
        raise RuntimeError("repair preflight is not valid")
    if receipt.get("addendum_sha256") != base.sha_file(ADDENDUM):
        raise RuntimeError("repair addendum drift")
    if receipt.get("runner_sha256") != base.sha_file(Path(__file__).resolve()):
        raise RuntimeError("repair runner drift")
    if receipt.get("base_runner_sha256") != base.sha_file(BASE_PATH):
        raise RuntimeError("frozen base runner drift")
    return receipt


def write_repair_aggregate(aggregate: dict[str, Any]) -> None:
    write_json(REPAIR_AGGREGATE, aggregate)
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
    REPAIR_TSV.write_text("\n".join(rows) + "\n", encoding="utf-8")


def write_repair_sha() -> None:
    files = [
        ADDENDUM,
        Path(__file__).resolve(),
        REPAIR_AGGREGATE,
        REPAIR_TSV,
        REPAIR_BOUNDARY,
    ]
    files.extend(sorted(path for path in REPAIR_PREFLIGHT.rglob("*") if path.is_file()))
    files.extend(sorted(path for path in REPAIR_RUNS.rglob("*") if path.is_file()))
    lines = [f"{base.sha_file(path)}  {path.relative_to(HERE)}" for path in sorted(set(files))]
    REPAIR_SHA.write_text("\n".join(lines) + "\n", encoding="utf-8")


def run_cohort() -> None:
    require_repair_preflight()
    status = base.run_capture(["git", "status", "--porcelain"], base.REPO)
    if status["stdout"]:
        raise RuntimeError("commit the repair addendum and preflight before model execution")
    commit = base.run_capture(["git", "rev-parse", "HEAD"], base.REPO)["stdout"].strip()
    write_json(
        REPAIR_BOUNDARY,
        {"registration_commit": commit, "started_at_ns": time.time_ns(), "schedule": base.SCHEDULE},
    )
    scores = [base.run_episode(number, arm) for number, arm in enumerate(base.SCHEDULE, 1)]
    aggregate = base.aggregate_scores(scores)
    write_repair_aggregate(aggregate)
    write_repair_sha()
    print(json.dumps(aggregate, indent=2, sort_keys=True))


def load_repair_scores() -> list[dict[str, Any]]:
    scores = []
    for number, arm in enumerate(base.SCHEDULE, 1):
        path = REPAIR_RUNS / f"{number:03d}-{arm}" / "score.json"
        if not path.is_file():
            raise RuntimeError(f"missing repair score: {path}")
        scores.append(json.loads(path.read_text(encoding="utf-8")))
    return scores


def verify() -> None:
    recomputed = base.aggregate_scores(load_repair_scores())
    committed = json.loads(REPAIR_AGGREGATE.read_text(encoding="utf-8"))
    if recomputed != committed:
        raise RuntimeError("repair aggregate replay mismatch")
    failures = []
    for line in REPAIR_SHA.read_text(encoding="utf-8").splitlines():
        expected, relative = line.split("  ", 1)
        path = HERE / relative
        if not path.is_file() or base.sha_file(path) != expected:
            failures.append(relative)
    if failures:
        raise RuntimeError(f"repair SHA mismatch: {failures}")
    print(base.canonical_json({"ok": True, "verdict": recomputed["verdict"], "files_verified": len(REPAIR_SHA.read_text().splitlines())}))


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("command", choices=["preflight", "run", "verify"])
    args = parser.parse_args()
    if args.command == "preflight":
        repair_preflight()
    elif args.command == "run":
        run_cohort()
    else:
        verify()
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
