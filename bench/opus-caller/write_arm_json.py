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

arm_dir = pathlib.Path(e["OPUS_ARMDIR"])
prepared = json.loads((arm_dir / "prepared.json").read_text())

# --- load: a SAMPLED interval, not two endpoints -------------------------------
ceiling = float(e["OPUS_LOAD_CEILING"])
samples = []
for line in (arm_dir / "load.jsonl").read_text().splitlines():
    line = line.strip()
    if line:
        try:
            samples.append(json.loads(line))
        except ValueError:
            pass
def one_min(row):
    return float(row["loadavg"].split()[0])
by_phase = {}
for row in samples:
    by_phase.setdefault(row.get("phase", "unknown"), []).append(one_min(row))
load_summary = {
    "sampler_interval_s": int(os.environ.get("OPUS_LOAD_SAMPLE_S", "5")),
    "samples": len(samples), "ceiling": ceiling,
    "max_by_phase": {k: max(v) for k, v in by_phase.items()},
    "max_overall": max((one_min(r) for r in samples), default=None),
    # CONTAMINATION IS CLASSIFIED PER PHASE, and an unsampled phase is UNKNOWN,
    # never "clean" (his review: missing measurements must remain unknown).
    "contaminated_driver": (max(by_phase["driver"]) > ceiling
                            if by_phase.get("driver") else None),
    "contaminated_acceptance": (max(by_phase["acceptance"]) > ceiling
                                if by_phase.get("acceptance") else None),
    "boundary_load_start": e["OPUS_ADAPTER_LOAD_START"],
    "boundary_load_end": e["OPUS_ATTESTED_LOAD"],
}
here = pathlib.Path(e["OPUS_HERE"])
ready = None
if e["OPUS_READY_V"]:
    try:
        ready = json.loads(pathlib.Path(e["OPUS_READY_V"]).read_text())
    except (OSError, ValueError):
        ready = None


json.dump({
    "id": e["OPUS_ID"], "cell": e["OPUS_CELL"], "rep": e["OPUS_REP"],
    "exp": "astra-fanout/opus-caller", "driver": "claude -p",
    "requested_model": e["OPUS_MODEL_REQ"], "cli_version": e["OPUS_CLI"],
    "caller_bin": e["OPUS_CALLER_PATH"],
    "fixture_src": e["OPUS_FIX_SRC"], "base": e["OPUS_BASE"],
    "frozen_prompt": prepared["frozen_prompt"], "mcp_url": e["OPUS_URL"] or None,
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
    "resolved_model": e["OPUS_MODEL_RESOLVED"] or None,
    "mcp_config_mode": prepared.get("mcp_config_mode"),
    "immutable_inputs": {k: prepared.get(k) for k in (
        "frozen_prompt_sha256", "composed_prompt_sha256", "oracle_sha256",
        "oracle_manifest_sha256", "oracle_canonical_tree_sha256",
        "verification_profile_sha256")},
    "load": load_summary,
}, open(e["OPUS_ARMJSON"], "w"), indent=2, sort_keys=True)

# --- attest.json: Astra's field names, so his reader works unchanged --------------
json.dump({
    "attest_ok": True, "start_utc": e["OPUS_UTC_START"],
    "arm": e["OPUS_CELL"], "exp": "astra-fanout/opus-caller", "driver": "claude -p",
    "model_requested": e["OPUS_MODEL_REQ"],
    "model_resolved": e["OPUS_MODEL_RESOLVED"] or None,
    "effort": "default",
    "worktree": e["OPUS_WT"], "worktree_head": e["OPUS_BASE"], "base": e["OPUS_BASE"],
    "prompt_path": e["OPUS_PROMPT_PATH"], "prompt_sha256": sha_file(e["OPUS_PROMPT_PATH"]),
    "frozen_prompt": prepared["frozen_prompt"],
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
    "server_attestation": (json.loads((arm_dir / "server-attest.json").read_text())
                           if (arm_dir / "server-attest.json").is_file() else None),
    "mcp_config_mode": prepared.get("mcp_config_mode"),
    "immutable_inputs": {k: prepared.get(k) for k in (
        "frozen_prompt_sha256", "composed_prompt_sha256", "oracle_sha256",
        "oracle_manifest_sha256", "oracle_canonical_tree_sha256",
        "verification_profile_sha256")},
    "correctness": "pending-independent-acceptance",
}, open(arm_dir / "attest.json", "w"), indent=2, sort_keys=True)

# --- adapter-result.json: his run() record, field for field ----------------------
f = float
adapter_start, driver_start = f(e["OPUS_ADAPTER_START"]), f(e["OPUS_DRIVER_START"])
driver_end, attested_end = f(e["OPUS_DRIVER_END"]), f(e["OPUS_ATTESTED_END"])
oracle_start, oracle_end = f(e["OPUS_ORACLE_START"]), f(e["OPUS_ORACLE_END"])
json.dump({
    "watch_rc": int(e["OPUS_DRIVER_RC"]),
    "valid_measurement": (flag("OPUS_SESSION_BOUND")
                          and int(e["OPUS_ATTRIB_RC"]) == 0
                          and int(e["OPUS_DRIVER_RC"]) == 0),
    "session_id": e["OPUS_SID"],
    # AN ACTUAL MODEL ID, not a prose pointer (his review).  Null when the transcript
    # did not name exactly one model -- which is itself a terminal :unverified.
    "resolved_model": e["OPUS_MODEL_RESOLVED"] or None,
    "requested_model_alias": e["OPUS_MODEL_REQ"],
    "resolved_model_source": "session transcript; the command alias is never the claim",
    "load": load_summary,
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
        # THE WALL ENDS WHERE ITS SCOPE SAYS IT ENDS.  Round two computed
        # driver_end - adapter_start and labelled it prepare-through-attestation,
        # which was false: freeze, diff and attribution all happen after the caller
        # exits.  It now ends at the attestation boundary, and adapter_load_end is
        # read at that same instant -- one boundary, one number.
        "adapter_wall_s": attested_end - adapter_start,
        "adapter_load_end": e["OPUS_ATTESTED_LOAD"],
        "adapter_wall_scope": ("prepare-through-freeze-and-attestation; "
                               "excludes the acceptance oracle"),
        "freeze_and_attribution_wall_s": attested_end - driver_end,
        "acceptance_wall_s": oracle_end - oracle_start,
        "acceptance_wall_scope": "the external six-check oracle only",
        "verified_completion_wall_s": oracle_end - adapter_start,
        "verified_completion_scope": ("prepare through accepted; the only total that "
                                      "covers a verified task end to end"),
        "monotonic_source": "/proc/uptime (10 ms granularity) — NOT interchangeable "
                            "with the shared adapter's time.monotonic()",
    },
    "correctness": "accepted" if oracle_rc == 0 else "not-accepted",
}, open(arm_dir / "adapter-result.json", "w"), indent=2, sort_keys=True)
