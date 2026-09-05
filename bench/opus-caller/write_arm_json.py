#!/usr/bin/env python3
"""write_arm_json.py — one arm's identity record, from the launcher's environment.

Kept out of run-opus-arm.sh so no shell value is ever interpolated into Python
source (a verdict line containing a quote would otherwise rewrite the program).
Every field arrives as an environment variable and is written verbatim.
"""
import json
import os

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
}, open(e["OPUS_ARMJSON"], "w"), indent=2, sort_keys=True)
