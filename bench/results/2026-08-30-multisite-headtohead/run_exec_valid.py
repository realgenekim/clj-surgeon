#!/Users/genekim/anaconda3/bin/python
"""Run the final exec-path-validated cohort without replacing prior evidence."""

from __future__ import annotations

import argparse
import importlib.util
import json
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
SPEC = importlib.util.spec_from_file_location("multisite_base_exec_valid", BASE_PATH)
if SPEC is None or SPEC.loader is None:
    raise RuntimeError("cannot load frozen base runner")
base = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(base)

ADDENDUM = HERE / "preregistration-exec-valid-addendum.md"
PREFLIGHT = HERE / "exec-valid-preflight"
RUNS = HERE / "runs-exec-valid"
AGGREGATE = HERE / "aggregate-exec-valid.json"
TSV = HERE / "runs-exec-valid.tsv"
BOUNDARY = HERE / "registration-boundary-exec-valid.json"
SHA_MANIFEST = HERE / "SHA256SUMS-exec-valid"
EXPECTED_PREVIOUS_COMMIT = "d52d29c4577e3e0506ec0b939315be2c82ef5a06"
INVALID_PROVIDER = "__multisite_preflight_no_provider__"
FROZEN_BASE_CONFIG_SOURCE = base.config_source


def final_config_source(arm: str, run_dir: Path, server_url: str | None) -> str:
    source = FROZEN_BASE_CONFIG_SOURCE(arm, run_dir, server_url)
    removals = [
        "update_plan_enabled = false\n",
        "\n[tools]\nview_image = false\nweb_search = false\n",
    ]
    for removal in removals:
        if source.count(removal) != 1:
            raise RuntimeError(f"base config does not contain one removal: {removal!r}")
        repaired = source.replace(removal, "")
        if len(source.encode("utf-8")) - len(repaired.encode("utf-8")) != len(
            removal.encode("utf-8")
        ):
            raise RuntimeError("config repair changed more than the registered text")
        source = repaired
    return source


base.config_source = final_config_source
base.RUNS = RUNS


def write_json(path: Path, value: Any) -> None:
    base.write_json(path, value)


def clean_preflight_env(home: Path, hook_log: Path) -> dict[str, str]:
    env = base.clean_child_env()
    env["CODEX_HOME"] = str(home)
    env["MULTISITE_HOOK_LOG"] = str(hook_log)
    return env


def validate_config(
    arm: str, source: str, workspace: Path, receipt_dir: Path
) -> dict[str, Any]:
    hook_log = receipt_dir / f"{arm}-tool-hooks.jsonl"
    with tempfile.TemporaryDirectory(prefix="multisite-exec-validate-", dir="/private/tmp") as text:
        home = Path(text)
        (home / "config.toml").write_text(source, encoding="utf-8")
        env = clean_preflight_env(home, hook_log)
        argv = [
            str(base.CODEX),
            "exec",
            "--json",
            "--strict-config",
            "-c",
            f'model_provider="{INVALID_PROVIDER}"',
            "-C",
            str(workspace),
            "CONFIG PARSE SENTINEL. This must not reach a model.",
        ]
        execute = base.run_capture(argv, workspace, env=env, timeout=30)
        write_json(receipt_dir / f"{arm}-exec-validation-process.json", execute)
        (receipt_dir / f"{arm}-exec-validation.stdout").write_text(
            execute["stdout"], encoding="utf-8"
        )
        (receipt_dir / f"{arm}-exec-validation.stderr").write_text(
            execute["stderr"], encoding="utf-8"
        )
        combined = execute["stdout"] + "\n" + execute["stderr"]
        if execute["exit_code"] == 0:
            raise RuntimeError(f"{arm} invalid-provider validation unexpectedly succeeded")
        if INVALID_PROVIDER not in combined or "not found" not in combined.lower():
            raise RuntimeError(f"{arm} did not pass config parsing to missing-provider boundary")
        if '"type":"turn.completed"' in execute["stdout"]:
            raise RuntimeError(f"{arm} preflight unexpectedly completed a model turn")
        if hook_log.exists() and hook_log.stat().st_size:
            raise RuntimeError(f"{arm} preflight unexpectedly invoked a tool")

        models = base.run_capture(
            [str(base.CODEX), "debug", "models"], workspace, env=env, timeout=60
        )
        write_json(receipt_dir / f"{arm}-debug-models-process.json", models)
        (receipt_dir / f"{arm}-debug-models.stdout").write_text(
            models["stdout"], encoding="utf-8"
        )
        (receipt_dir / f"{arm}-debug-models.stderr").write_text(
            models["stderr"], encoding="utf-8"
        )
        if models["exit_code"] != 0:
            raise RuntimeError(f"{arm} model catalog validation failed")
        catalog = json.loads(models["stdout"])
        entries = catalog.get("models", catalog if isinstance(catalog, list) else [])
        matches = [entry for entry in entries if entry.get("slug") == base.MODEL]
        if len(matches) != 1:
            raise RuntimeError(f"{arm} did not resolve one {base.MODEL}")
        expected_patch = "freeform" if arm == "N" else None
        if matches[0].get("apply_patch_tool_type") != expected_patch:
            raise RuntimeError(f"{arm} resolved wrong apply_patch tool type")

        mcp = base.run_capture([str(base.CODEX), "mcp", "list"], workspace, env=env)
        write_json(receipt_dir / f"{arm}-mcp-list-process.json", mcp)
        (receipt_dir / f"{arm}-mcp-list.stdout").write_text(mcp["stdout"], encoding="utf-8")
        (receipt_dir / f"{arm}-mcp-list.stderr").write_text(mcp["stderr"], encoding="utf-8")
        if mcp["exit_code"] != 0:
            raise RuntimeError(f"{arm} MCP validation failed")
        has_surgeon = "clj-surgeon" in mcp["stdout"]
        if has_surgeon != (arm == "S"):
            raise RuntimeError(f"{arm} MCP presence mismatch")

    if "[tools]" in source or "update_plan_enabled" in source:
        raise RuntimeError(f"{arm} retained a registered removal")
    if "shell_tool = false" not in source:
        raise RuntimeError(f"{arm} did not disable shell")
    if arm == "S":
        if source.count('enabled_tools = ["edit_clojure"]') != 1:
            raise RuntimeError("S does not expose exactly the registered MCP mutation tool")
    elif "[mcp_servers." in source:
        raise RuntimeError("N unexpectedly contains an MCP server")
    return {
        "config_sha256": base.sha_bytes(source.encode("utf-8")),
        "exec_validation_exit_code": execute["exit_code"],
        "exec_validation_stopped_at_missing_provider": True,
        "completed_model_turns": 0,
        "tool_hook_bytes": hook_log.stat().st_size if hook_log.exists() else 0,
        "resolved_apply_patch_tool_type": expected_patch,
        "mcp_server_present": has_surgeon,
        "child_openai_environment_variables": [],
    }


def preflight() -> None:
    if PREFLIGHT.exists():
        raise RuntimeError(f"refusing to replace exec-valid preflight: {PREFLIGHT}")
    base.require_preflight()
    current = base.run_capture(["git", "rev-parse", "HEAD"], base.REPO)["stdout"].strip()
    if current != EXPECTED_PREVIOUS_COMMIT:
        raise RuntimeError(f"preflight must start at retained repair commit: {current}")
    PREFLIGHT.mkdir(parents=True)
    workspace = PREFLIGHT / "workspace"
    fixture_commit = base.materialize_workspace(workspace)
    server: subprocess.Popen[str] | None = None
    try:
        native_source = final_config_source("N", PREFLIGHT, None)
        native = validate_config("N", native_source, workspace, PREFLIGHT)
        server, server_url = base.start_http_server(
            workspace, PREFLIGHT, "multisite-exec-valid-preflight"
        )
        surgeon_source = final_config_source("S", PREFLIGHT, server_url)
        surgeon = validate_config("S", surgeon_source, workspace, PREFLIGHT)
    finally:
        base.stop_process(server)
    shutil.rmtree(workspace / ".git")
    (PREFLIGHT / "N-config.toml").write_text(native_source, encoding="utf-8")
    (PREFLIGHT / "S-config.toml").write_text(surgeon_source, encoding="utf-8")
    receipt = {
        "schema": "multisite-exec-valid-preflight.v1",
        "status": "ok",
        "completed_at_ns": time.time_ns(),
        "previous_killed_cohort_commit": current,
        "fixture_commit": fixture_commit,
        "addendum_sha256": base.sha_file(ADDENDUM),
        "runner_sha256": base.sha_file(Path(__file__).resolve()),
        "base_runner_sha256": base.sha_file(BASE_PATH),
        "invalid_provider_sentinel": INVALID_PROVIDER,
        "native": native,
        "surgeon": surgeon,
    }
    write_json(PREFLIGHT / "preflight.json", receipt)
    print(json.dumps(receipt, indent=2, sort_keys=True))


def require_preflight() -> dict[str, Any]:
    path = PREFLIGHT / "preflight.json"
    if not path.is_file():
        raise RuntimeError("run and commit exec-valid preflight before model calls")
    receipt = json.loads(path.read_text(encoding="utf-8"))
    checks = [
        receipt.get("status") == "ok",
        receipt.get("addendum_sha256") == base.sha_file(ADDENDUM),
        receipt.get("runner_sha256") == base.sha_file(Path(__file__).resolve()),
        receipt.get("base_runner_sha256") == base.sha_file(BASE_PATH),
    ]
    if not all(checks):
        raise RuntimeError("exec-valid preflight drift")
    return receipt


def write_outputs(aggregate: dict[str, Any]) -> None:
    write_json(AGGREGATE, aggregate)
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
    TSV.write_text("\n".join(rows) + "\n", encoding="utf-8")


def write_sha_manifest() -> None:
    files = [ADDENDUM, Path(__file__).resolve(), AGGREGATE, TSV, BOUNDARY]
    files.extend(sorted(path for path in PREFLIGHT.rglob("*") if path.is_file()))
    files.extend(sorted(path for path in RUNS.rglob("*") if path.is_file()))
    lines = [
        f"{base.sha_file(path)}  {path.relative_to(HERE)}"
        for path in sorted(set(files))
    ]
    SHA_MANIFEST.write_text("\n".join(lines) + "\n", encoding="utf-8")


def run_cohort() -> None:
    require_preflight()
    status = base.run_capture(["git", "status", "--porcelain"], base.REPO)
    if status["stdout"]:
        raise RuntimeError("commit exec-valid registration before model execution")
    commit = base.run_capture(["git", "rev-parse", "HEAD"], base.REPO)["stdout"].strip()
    write_json(
        BOUNDARY,
        {"registration_commit": commit, "started_at_ns": time.time_ns(), "schedule": base.SCHEDULE},
    )
    scores = [base.run_episode(number, arm) for number, arm in enumerate(base.SCHEDULE, 1)]
    aggregate = base.aggregate_scores(scores)
    write_outputs(aggregate)
    write_sha_manifest()
    print(json.dumps(aggregate, indent=2, sort_keys=True))


def load_scores() -> list[dict[str, Any]]:
    scores = []
    for number, arm in enumerate(base.SCHEDULE, 1):
        path = RUNS / f"{number:03d}-{arm}" / "score.json"
        if not path.is_file():
            raise RuntimeError(f"missing exec-valid score: {path}")
        score = json.loads(path.read_text(encoding="utf-8"))
        if score.get("episode") != number or score.get("arm") != arm:
            raise RuntimeError(f"score identity mismatch: {path}")
        scores.append(score)
    return scores


def verify() -> None:
    recomputed = base.aggregate_scores(load_scores())
    committed = json.loads(AGGREGATE.read_text(encoding="utf-8"))
    if recomputed != committed:
        raise RuntimeError("exec-valid aggregate replay mismatch")
    failures = []
    for line in SHA_MANIFEST.read_text(encoding="utf-8").splitlines():
        expected, relative = line.split("  ", 1)
        path = HERE / relative
        if not path.is_file() or base.sha_file(path) != expected:
            failures.append(relative)
    if failures:
        raise RuntimeError(f"exec-valid SHA mismatch: {failures}")
    print(
        base.canonical_json(
            {
                "ok": True,
                "verdict": recomputed["verdict"],
                "files_verified": len(SHA_MANIFEST.read_text().splitlines()),
            }
        )
    )


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("command", choices=["preflight", "run", "verify"])
    args = parser.parse_args()
    if args.command == "preflight":
        preflight()
    elif args.command == "run":
        run_cohort()
    else:
        verify()
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
