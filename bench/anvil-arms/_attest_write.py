#!/usr/bin/env python3
"""Write attest.json / attest.edn and evaluate A.4's fail-closed refusals.

Called only by attest.sh, which exports every field as an environment variable.
Exit 0 = attested; exit 2 = ATTEST-MISMATCH (the arm must never launch a driver).
"""
from __future__ import annotations

import json
import os
import pathlib
import sys
from datetime import datetime, timezone

UNV = "unverified"


def env(name: str) -> str:
    value = os.environ.get(name, "")
    return value if value.strip() else UNV


def maybe_json(raw: str):
    if raw == UNV:
        return UNV
    try:
        return json.loads(raw)
    except Exception:
        return UNV


def as_int(raw: str):
    try:
        return int(raw)
    except Exception:
        return UNV


def edn(value, indent: int = 0) -> str:
    pad = " " * indent
    if isinstance(value, dict):
        items = "\n".join(
            f"{pad}  :{k.replace('_', '-')} {edn(v, indent + 2).lstrip()}"
            for k, v in value.items()
        )
        return f"{pad}{{\n{items}}}\n" if indent == 0 else "{" + items.strip() + "}"
    if isinstance(value, bool):
        return "true" if value else "false"
    if value is None:
        return "nil"
    if isinstance(value, (int, float)):
        return str(value)
    if isinstance(value, list):
        return "[" + " ".join(edn(v) for v in value) + "]"
    return json.dumps(str(value))


def main() -> int:
    arm_dir = pathlib.Path(os.environ["A"])
    arm = env("ARM")
    port_raw = os.environ.get("PORT", "-")
    port = as_int(port_raw) if port_raw not in ("", "-") else UNV
    expected = os.environ.get("EXPECTED_SERVER_SHA", "").strip()

    start_utc = datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")

    attest = {
        "exp": env("EXP"),
        "rung": env("RUNG"),
        "arm": arm,
        "slot": env("SLOT"),
        "group": env("GROUP"),
        "start_utc": start_utc,
        "worktree": env("WORKTREE"),
        "worktree_head": env("WORKTREE_HEAD"),
        "base": env("BASE"),
        "prompt_path": env("PROMPT"),
        "prompt_sha256": env("PROMPT_SHA"),
        "model": env("MODEL"),
        "driver": env("DRIVER"),
        "runner": env("RUNNER"),
        "runner_sha256": env("RUNNER_SHA"),
        "attest_sha256": env("ATTEST_SHA"),
        "watch_sha256": env("WATCH_SHA"),
        "score_sha256": env("SCORE_SHA"),
        "make_targets": env("MAKE_TARGETS"),
        "make_targets_sha256": env("MAKE_TARGETS_SHA"),
        "mcp_url": os.environ.get("MCP_URL", "") or None,
        "mcp_port": port,
        "expected_server_sha": expected or UNV,
        "healthz": maybe_json(env("HEALTHZ")),
        "port_pid": as_int(env("PORT_PID")) if env("PORT_PID") != UNV else UNV,
        "ready_pid": as_int(env("READY_PID")) if env("READY_PID") != UNV else UNV,
        "ready_project_root": env("READY_PROJECT_ROOT"),
        "server_project_head": env("SERVER_PROJECT_HEAD"),
        "server_cwd": env("SERVER_CWD"),
        "server_sha": env("SERVER_SHA"),
        "mcp_absent_proof": env("MCP_ABSENT_PROOF"),
        "listeners_observed": env("LISTENERS"),
    }

    reasons: list[str] = []

    def refuse(cond: bool, reason: str) -> None:
        if cond:
            reasons.append(reason)

    # --- conditions binding on every arm ---------------------------------------
    refuse(attest["worktree"] == UNV, "worktree-missing")
    refuse(attest["worktree_head"] == UNV, "worktree-head-unverified")
    refuse(attest["base"] == UNV, "base-unverified")
    refuse(
        UNV not in (attest["worktree_head"], attest["base"])
        and not attest["worktree_head"].startswith(attest["base"]),
        f"worktree-head-ne-base({attest['worktree_head'][:12]}!={attest['base'][:12]})",
    )
    refuse(attest["prompt_sha256"] == UNV, "prompt-sha256-unverified")
    refuse(attest["runner_sha256"] == UNV, "runner-sha256-unverified")

    if arm == "N":
        # A.4: arm N with any reachable Surgeon port refuses.  attest.sh scopes
        # "reachable" to the ports this apparatus owns plus the url handed to the arm.
        refuse(not str(attest["mcp_absent_proof"]).startswith("ok:"),
               f"mcp-absent-proof:{attest['mcp_absent_proof']}")
    else:
        refuse(os.environ.get("PORT_IN_RANGE") != "yes",
               f"port-out-of-cohort-range({port_raw})")
        refuse(attest["port_pid"] == UNV, "port-pid-unverified")
        refuse(attest["healthz"] == UNV, "healthz-unverified")
        refuse(attest["ready_project_root"] == UNV, "ready-project-root-unverified")
        refuse(
            attest["ready_project_root"] != UNV
            and attest["worktree"] != UNV
            and os.path.realpath(str(attest["ready_project_root"]))
            != os.path.realpath(str(attest["worktree"])),
            "ready-project-root-ne-worktree",
        )
        refuse(
            attest["server_project_head"] != UNV
            and attest["worktree_head"] != UNV
            and attest["server_project_head"] != attest["worktree_head"],
            "server-project-head-ne-worktree-head",
        )
        # The server's own code must be the sha the runner was told to expect.
        refuse(attest["server_sha"] == UNV, "server-sha-unverified")
        refuse(
            bool(expected)
            and attest["server_sha"] != UNV
            and not str(attest["server_sha"]).startswith(expected),
            f"server-sha-ne-expected({str(attest['server_sha'])[:12]}!={expected[:12]})",
        )
        refuse(not expected, "expected-server-sha-missing")
        # Cross-check the two pid witnesses when both exist.
        refuse(
            UNV not in (attest["port_pid"], attest["ready_pid"])
            and attest["port_pid"] != attest["ready_pid"],
            f"port-pid-ne-ready-pid({attest['port_pid']}!={attest['ready_pid']})",
        )

    attest["attest_ok"] = not reasons
    attest["refusals"] = reasons

    arm_dir.mkdir(parents=True, exist_ok=True)
    (arm_dir / "attest.json").write_text(json.dumps(attest, indent=2, sort_keys=False) + "\n")
    (arm_dir / "attest.edn").write_text(edn(attest))

    mismatch = arm_dir / "ATTEST-MISMATCH"
    if reasons:
        mismatch.write_text(
            f"ATTEST-MISMATCH {arm_dir.name} {start_utc} " + " ".join(reasons) + "\n"
        )
        print("ATTEST-MISMATCH " + " ".join(reasons), file=sys.stderr)
        return 2
    if mismatch.exists():
        mismatch.unlink()
    print(f"attested {arm_dir.name} start_utc={start_utc}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
