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
        "make_dynamic_refusal": os.environ.get("MAKE_DYNAMIC_REFUSAL", "").strip() or None,
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
    # The attestation may not execute the repository's own Makefile to decide whether
    # that repository may run (Sol round two, item 1).  _make_targets.py parses it as
    # text; when the parse cannot see the whole file, no driver launches.
    refuse(bool(attest["make_dynamic_refusal"]),
           f"makefile-dynamic:{attest['make_dynamic_refusal']}")

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
        # The health document must be ABOUT THIS SERVER.  Sol, item 6: it was checked
        # only for parseability, so a document reporting ok=false for pid 999 on port 1
        # serving /wrong/project attested ok=true.  Each field is bound to an
        # independently observed witness: the pid to the one `ss` saw owning the port,
        # the port to the one this arm was handed, the project root to this arm's
        # worktree.  Any disagreement is ATTEST-MISMATCH, never a note.
        health = attest["healthz"]
        if attest["healthz"] != UNV and not isinstance(health, dict):
            refuse(True, f"healthz-not-an-object({type(health).__name__})")
        elif isinstance(health, dict):
            refuse(health.get("ok") is not True,
                   f"healthz-not-ok({health.get('ok')!r})")
            # /healthz is served by `mcp-runtime/readiness`, NOT by the rich
            # readiness map `mcp_http_server.clj` builds and writes to ready.edn.
            # The live server returns {ok, server, tool_runtime, tool_registry}: no
            # pid, no port, no project-root.  Asserting those three fields refused
            # every real tool arm (measured 2026-09-04, E3-P preflight, the first
            # time this apparatus met a live server rather than the fake driver).
            #
            # So healthz is a LIVENESS witness, and identity is bound by the
            # witnesses that actually publish it and are already refused-on-missing
            # below: `ss -ltnp` for the pid owning the port, ready.edn for the
            # served project root and pid, and /proc/<pid>/cwd for the server's own
            # sha.  Each healthz identity field is still checked WHEN THE SERVER
            # PUBLISHES IT, so a server that grows the fields is held to them and
            # this never becomes a way to attest a document about another process.
            health_pid = health.get("pid")
            refuse(health_pid is not None and attest["port_pid"] != UNV
                   and health_pid != attest["port_pid"],
                   f"healthz-pid-ne-port-pid({health_pid}!={attest['port_pid']})")
            health_port = health.get("port")
            refuse(health_port is not None and port != UNV and health_port != port,
                   f"healthz-port-ne-arm-port({health_port}!={port})")
            health_root = health.get("project-root", health.get("project_root"))
            refuse(
                health_root is not None
                and attest["worktree"] != UNV
                and os.path.realpath(str(health_root))
                != os.path.realpath(str(attest["worktree"])),
                f"healthz-project-root-ne-worktree({health_root})",
            )
            attest["healthz_identity_fields"] = sorted(
                k for k in ("pid", "port", "project-root", "project_root")
                if health.get(k) is not None)
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
