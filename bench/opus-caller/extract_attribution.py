#!/usr/bin/env python3
"""extract_attribution.py — the attribution path, from the session transcript.

Two witnesses, and the disagreement is the signal:

  * the SESSION TRANSCRIPT the CLI wrote to
    ~/.claude/projects/<escaped-cwd>/<session-id>.jsonl  -- the authority.  The
    run is bound to that file by path and sha256; the model id and every tool
    call are read out of it.
  * the STREAM the launcher captured on stdout (run.log, --output-format
    stream-json) -- a corroborator.  Its `init` record announces the resolved
    model, the session id and the attached MCP servers.

Neither is asked to confirm the other.  Where they differ the difference is
recorded in `sources`, never resolved into one number.  A requested model that
does not appear in the transcript is `model_matches_request: false` -- the arm
is then :unverified for attribution, and the caller's own summary is never a
counting authority for anything here.

ROUND THREE, after his review.  Three things were too loose:

  * model matching was a SUBSTRING test across however many models the transcript
    happened to name.  It now requires EXACTLY ONE model in the transcript -- the
    resolved id -- and that id must begin with the requested alias.  Two models is a
    refusal, not a set.
  * a stream/transcript model disagreement was recorded and tolerated.  It is now
    TERMINAL: two witnesses that disagree about which model ran mean nobody knows.
  * native arms proved MCP absence by not passing a URL.  With an explicitly empty
    --mcp-config, absence is now also CHECKED: --expect-no-mcp makes any tool call
    whose name begins `mcp__` a refusal.

Writes <arm>/calls.json and <arm>/attribution.json.  Exit 0 on a bound and
self-consistent session, 3 otherwise (never a silent zero).
"""
from __future__ import annotations

import argparse
import hashlib
import json
import pathlib
import sys


def digest(value) -> str:
    payload = json.dumps(value, sort_keys=True, ensure_ascii=False).encode("utf-8")
    return hashlib.sha256(payload).hexdigest()


def read_jsonl(path: pathlib.Path):
    """Every non-empty line must parse.  A line we cannot read is missing evidence."""
    rows, bad = [], []
    if not path.is_file():
        return rows, ["missing-file"]
    for lineno, line in enumerate(path.read_text(errors="replace").splitlines(), 1):
        line = line.strip()
        if not line:
            continue
        try:
            obj = json.loads(line)
        except Exception as exc:
            bad.append(f"{path.name}:{lineno} {exc}")
            continue
        if isinstance(obj, dict):
            rows.append(obj)
        else:
            bad.append(f"{path.name}:{lineno} non-object")
    return rows, bad


def harvest(rows):
    """Model ids and tool calls, wherever the record shape puts them."""
    models, calls, sessions = set(), [], set()
    for row in rows:
        for key in ("sessionId", "session_id"):
            if isinstance(row.get(key), str):
                sessions.add(row[key])
        message = row.get("message") if isinstance(row.get("message"), dict) else None
        if isinstance(row.get("model"), str):
            models.add(row["model"])
        if message and isinstance(message.get("model"), str):
            models.add(message["model"])
        content = (message or {}).get("content")
        if isinstance(content, list):
            for item in content:
                if isinstance(item, dict) and item.get("type") == "tool_use":
                    calls.append({
                        "name": item.get("name"),
                        "id": item.get("id"),
                        "args_sha256": digest(item.get("input")),
                        "timestamp": row.get("timestamp"),
                    })
    return models, calls, sessions


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    for key in ("arm", "session", "run-log", "session-id", "requested-model"):
        parser.add_argument("--" + key, required=True)
    parser.add_argument("--expect-no-mcp", action="store_true",
                        help="native cell: any mcp__ tool call is a refusal")
    args = parser.parse_args()

    arm = pathlib.Path(args.arm)
    session_path = pathlib.Path(args.session)
    session_rows, session_bad = read_jsonl(session_path)
    stream_rows, stream_bad = read_jsonl(pathlib.Path(args.run_log))

    s_models, s_calls, s_sessions = harvest(session_rows)
    r_models, r_calls, r_sessions = harvest(stream_rows)

    init = next((r for r in stream_rows
                 if r.get("type") == "system" and r.get("subtype") == "init"), {})

    bound = session_path.is_file() and bool(session_rows)
    mcp_calls = [c for c in s_calls if (c["name"] or "").startswith("mcp__")]
    resolved = sorted(s_models)[0] if len(s_models) == 1 else None
    record = {
        "session_id_requested": args.session_id,
        "session_file": str(session_path),
        "session_file_exists": session_path.is_file(),
        "session_file_sha256": (hashlib.sha256(session_path.read_bytes()).hexdigest()
                                if session_path.is_file() else None),
        "session_bound": bound,
        "session_ids_in_transcript": sorted(s_sessions),
        "session_id_matches": s_sessions == {args.session_id} if s_sessions else False,
        "requested_model": args.requested_model,
        "models_in_transcript": sorted(s_models),
        "resolved_model": resolved,
        "model_unique": len(s_models) == 1,
        "model_matches_request": bool(resolved) and resolved.startswith(args.requested_model),
        "mcp_tool_calls": len(mcp_calls),
        "mcp_expected_absent": bool(args.expect_no_mcp),
        "stream_init": {"session_id": init.get("session_id"),
                        "model": init.get("model"),
                        "mcp_servers": init.get("mcp_servers"),
                        "tools_count": len(init.get("tools") or [])},
        "sources": {
            "transcript": {"rows": len(session_rows), "tool_calls": len(s_calls),
                           "models": sorted(s_models), "unreadable_lines": session_bad},
            "stream": {"rows": len(stream_rows), "tool_calls": len(r_calls),
                       "models": sorted(r_models), "unreadable_lines": stream_bad},
            "agree_tool_call_count": len(s_calls) == len(r_calls),
            "agree_models": s_models == r_models,
            "disagreement_is_terminal": True,
        },
    }
    (arm / "attribution.json").write_text(json.dumps(record, indent=2, sort_keys=True) + "\n")
    (arm / "calls.json").write_text(json.dumps(
        {"authority": "session-transcript", "session_file": str(session_path),
         "count": len(s_calls), "calls": s_calls,
         "by_name": {name: sum(1 for c in s_calls if c["name"] == name)
                     for name in sorted({c["name"] for c in s_calls if c["name"]})}},
        indent=2, sort_keys=True) + "\n")

    if not bound:
        print("ATTRIBUTION :unverified — no readable session transcript at "
              f"{session_path}", file=sys.stderr)
        return 3
    if session_bad:
        print(f"ATTRIBUTION :unverified — unreadable transcript lines: {session_bad[:3]}",
              file=sys.stderr)
        return 3
    if not record["session_id_matches"]:
        print("ATTRIBUTION :unverified — transcript session ids "
              f"{sorted(s_sessions)} != requested {args.session_id}", file=sys.stderr)
        return 3
    if not record["model_unique"]:
        print(f"ATTRIBUTION :unverified — transcript names {len(s_models)} models "
              f"{sorted(s_models)}; a run has exactly one resolved model",
              file=sys.stderr)
        return 3
    if not record["model_matches_request"]:
        print(f"ATTRIBUTION :unverified — resolved model {resolved!r} does not answer "
              f"the requested {args.requested_model!r}", file=sys.stderr)
        return 3
    if r_models and s_models != r_models:
        print(f"ATTRIBUTION :unverified — the two witnesses disagree about the model: "
              f"transcript {sorted(s_models)} vs stream {sorted(r_models)}",
              file=sys.stderr)
        return 3
    if args.expect_no_mcp and mcp_calls:
        print(f"ATTRIBUTION :unverified — a native arm made {len(mcp_calls)} MCP tool "
              f"call(s): {sorted({c['name'] for c in mcp_calls})}", file=sys.stderr)
        return 3
    print(f"ATTRIBUTION ok session={args.session_id} resolved_model={resolved} "
          f"tool_calls={len(s_calls)} mcp_tool_calls={len(mcp_calls)}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
