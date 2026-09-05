#!/usr/bin/env python3
"""write_arm_json.py — this arm's three records, from the launcher's environment.

Kept out of run-opus-arm.sh so no shell value is ever interpolated into Python
source (a verdict line containing a quote would otherwise rewrite the program).
Every field arrives as an environment variable and is written verbatim.

Three files, and two of them wear ASTRA'S FIELD NAMES ON PURPOSE:

  arm.json            this flank's own summary, for a human reading one arm.
  attest.json         his adapter's `attest` record, field for field, so his
                      reader and any tooling built on it work unchanged.  The
                      three codex-specific keys become their caller equivalents
                      (`caller_version`/`caller_sha256`/`caller_path` for
                      `codex_version`/`codex_sha256`/`codex_vendor_*`), and the
                      keys that name instruments this flank does not run
                      (`watch_sha256`, `score_sha256`, `make_targets_sha256`)
                      are present and null rather than quietly absent.
  adapter-result.json his `run()` result record, field for field, including the
                      whole `timing` block with `adapter_wall_scope` spelled out.

A field carried under his name means the SAME QUANTITY measured the same way.
Where a quantity cannot be the same for a Claude caller it is renamed, never
silently redefined.
"""
import hashlib
import json
import os
import pathlib


def sha_file(path):
    try:
        return hashlib.sha256(pathlib.Path(path).read_bytes()).hexdigest()
    except OSError:
        return None

e = os.environ
flag = lambda k: e[k] == "true"
oracle_rc = int(e["OPUS_ORACLE_RC"])

json.dump({
    "id": e["OPUS_ID"], "cell": e["OPUS_CELL"], "rep": e["OPUS_REP"],
    "exp": "astra-fanout/opus-caller", "driver": "claude -p",
    "requested_model": e["OPUS_MODEL_REQ"], "cli_version": e["OPUS_CLI"],
    "caller_bin": e["OPUS_CALLER_PATH"],
    "fixture_src": e["OPUS_FIX_SRC"], "base": e["OPUS_BASE"],
    "frozen_prompt": e["OPUS_FROZEN_PROMPT"], "mcp_url": e["OPUS_URL"] or None,
    "session_id": e["OPUS_SID"], "session_file": e["OPUS_SESSION_FILE"],
    "session_bound": flag("OPUS_SESSION_BOUND"),
    "session_sha256": e["OPUS_SESSION_SHA"] or None,
    "driver_rc": int(e["OPUS_DRIVER_RC"]),
    "attribution_rc": int(e["OPUS_ATTRIB_RC"]),
    "protected_bytes_match": flag("OPUS_GUARD"),
    "oracle": e["OPUS_ORACLE_PATH"], "oracle_sha256": e["OPUS_ORACLE_SHA"],
    "oracle_fixtures": e["OPUS_ORACLE_FIXDIR"], "oracle_rc": oracle_rc,
    "oracle_verdict": e["OPUS_VERDICT"] or "MISSING",
    # correctness is the ORACLE's verdict, never the caller's summary
    "correctness": "accepted" if oracle_rc == 0 else "not-accepted",
    "cpus": e["OPUS_CPUS"], "cpu_affinity": "taskset -c " + e["OPUS_CPUS"],
    "canonical_src_match": flag("OPUS_CANONICAL_MATCH"),
}, open(e["OPUS_ARMJSON"], "w"), indent=2, sort_keys=True)

arm_dir = pathlib.Path(e["OPUS_ARMDIR"])
here = pathlib.Path(e["OPUS_HERE"])
ready = None
if e["OPUS_READY_V"]:
    try:
        ready = json.loads(pathlib.Path(e["OPUS_READY_V"]).read_text())
    except (OSError, ValueError):
        ready = None

# --- attest.json: Astra's field names, so his reader works unchanged --------------
json.dump({
    "attest_ok": True, "start_utc": e["OPUS_UTC_START"],
    "arm": e["OPUS_CELL"], "exp": "astra-fanout/opus-caller", "driver": "claude -p",
    "model": e["OPUS_MODEL_REQ"], "effort": "default",
    "worktree": e["OPUS_WT"], "worktree_head": e["OPUS_BASE"], "base": e["OPUS_BASE"],
    "prompt_path": e["OPUS_PROMPT_PATH"], "prompt_sha256": sha_file(e["OPUS_PROMPT_PATH"]),
    "driver_command": (arm_dir / "command.txt").read_text().split("\n")[:-1],
    "cpus": e["OPUS_CPUS"],
    # caller-specific renames of his codex_* keys (same quantities, different binary)
    "caller_version": e["OPUS_CLI"], "caller_path": e["OPUS_CALLER_PATH"],
    "caller_sha256": sha_file(e["OPUS_CALLER_PATH"]),
    "runner_sha256": sha_file(here / "run-opus-arm.sh"),
    # instruments this flank does not run: present and NULL, never quietly absent
    "watch_sha256": None, "score_sha256": None, "make_targets_sha256": None,
    "mcp_url": e["OPUS_URL"] or None, "server_sha": e["OPUS_SERVER_SHA_V"] or None,
    "port_pid": (ready or {}).get("port_pid"),
    "server_cwd": (ready or {}).get("server_cwd"),
    "server_ready": ready,
    "correctness": "pending-independent-acceptance",
}, open(arm_dir / "attest.json", "w"), indent=2, sort_keys=True)

# --- adapter-result.json: his run() record, field for field ----------------------
f = float
adapter_start, driver_start = f(e["OPUS_ADAPTER_START"]), f(e["OPUS_DRIVER_START"])
driver_end = f(e["OPUS_DRIVER_END"])
json.dump({
    "watch_rc": int(e["OPUS_DRIVER_RC"]),
    "valid_measurement": (flag("OPUS_SESSION_BOUND")
                          and int(e["OPUS_ATTRIB_RC"]) == 0
                          and int(e["OPUS_DRIVER_RC"]) == 0),
    "session_id": e["OPUS_SID"],
    "resolved_model": "see attribution.json: models_in_transcript "
                      "(the command alias is never the model claim)",
    "protected_bytes_match": flag("OPUS_GUARD"),
    "canonical_src_match": flag("OPUS_CANONICAL_MATCH"),
    "timing": {
        "adapter_start_monotonic_s": adapter_start,
        "watch_start_monotonic_s": driver_start,
        "watch_end_monotonic_s": driver_end,
        "preparation_wall_s": driver_start - adapter_start,
        "watch_subprocess_wall_s": driver_end - driver_start,
        "adapter_load_start": e["OPUS_ADAPTER_LOAD_START"],
        "watch_load_start": e["OPUS_LOAD_START"],
        "watch_load_end": e["OPUS_LOAD_END"],
        "lock_wait_included": False,
        "adapter_wall_s": driver_end - adapter_start,
        "adapter_load_end": open("/proc/loadavg").read().strip(),
        "adapter_wall_scope": "prepare-through-freeze-and-attestation; excludes scorer",
    },
    "correctness": "accepted" if oracle_rc == 0 else "not-accepted",
}, open(arm_dir / "adapter-result.json", "w"), indent=2, sort_keys=True)
