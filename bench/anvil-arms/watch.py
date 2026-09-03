#!/usr/bin/env python3
"""watch.py — A.10 of docs/observations/2026-09-04-e3-e6-prestaged.md.

An event-driven wrapper around ONE arm driver (`~/bin/sol-yolo`, `claude -p`, or the
self-test's fake driver).  It tails the driver's own JSONL rollout and appends one
record to `watch.jsonl` per model return and per tool call, then stamps the run's
completion time itself.

    watch.py --arm <ARMDIR> [options] -- <driver command...>

What it is NOT, per the tweezer-loop protocol: it never suggests the next call,
never repairs arguments, never interprets a result for the driver, and never edits a
file.  It only meters.  It carries a self-firing wall cap and an idle stop.

Meters, and why each exists:
  kind=return   one model turn.  `n` is the return ordinal -- what makes "was
                ls-tree called within the first 3 returns" answerable.
  kind=call     one tool call, nested inside the return that was open when it
                issued.  `test_call` is matched AT COMMAND POSITION (the
                edit_wall.py definition), so non-test actions = calls - test calls.
  kind=abort    a typed stop.  Zero returns is an abort, never a silent zero: a
                verdict printed over a missing number is the defect this program
                has already paid for once.
  kind=end      the completion stamp, taken with `datetime.now(timezone.utc)`
                INSIDE the write.  Wall is that stamp minus attest.json's
                `start_utc`; it is never hand-typed and never self-reported.

Exit codes: 0 metered; 2 usage / unattested arm; 4 zero returns; 5 idle or wall cap;
6 incomplete run (a tool call whose result never arrived).
The driver's own exit status is recorded in run.json, never conflated with these.
"""
from __future__ import annotations

import argparse
import glob
import hashlib
import json
import os
import pathlib
import re
import shlex
import signal
import subprocess
import sys
import time
from datetime import datetime, timezone

# ---------------------------------------------------------------------------
# test-call recognition, matched at command position
# ---------------------------------------------------------------------------
TEST_BASENAMES = {"kaocha", "fan-test", "run_all.clj", "run-tests", "runtests"}
# wrappers that stand in front of the real command and must be stepped over
WRAPPERS = {"env", "nohup", "time", "command", "exec", "builtin", "stdbuf", "nice",
            "ionice", "setsid", "sudo", "doas"}
WRAPPERS_WITH_ARG = {"timeout": 1, "flock": 1}
SHELL_RUNNERS = {"bash", "sh", "zsh", "dash", "ksh"}
MAKE_TEST_TARGET = re.compile(r"^(test|test-fast|runtests|mcp-test|.*-test)$")
CLOJURE_TEST_ALIAS = re.compile(r"^-M:.*\b(test|kaocha)\b|^:test$|^-X:.*\btest\b")
SPLITTERS = re.compile(r"\|\||&&|[;\n|&]")

# a native `apply_patch` payload names its own targets; the watcher extracts them
# from the FULL arguments (score.py only ever sees a truncated copy), so predicate 6
# -- "did native apply_patch land functional .clj bytes" -- is computed, not guessed.
PATCH_FILE_RE = re.compile(
    r"^\*\*\* (?:Update|Add|Delete) File: (.+)$|^\+\+\+ (?:b/)?(.+)$", re.M)
APPLY_PATCH_CMD_RE = re.compile(r"(?:^|[\s;|&(])apply_patch(?:\s|$)")


def patch_targets(*texts: str) -> list[str]:
    files: list[str] = []
    for text in texts:
        if not text:
            continue
        for m in PATCH_FILE_RE.finditer(text):
            name = (m.group(1) or m.group(2) or "").strip()
            if name and name != "/dev/null":
                files.append(name)
    return files


# tool names that commit a Clojure change through a clj-surgeon verb
COMMITTING_VERBS = {
    "alias_migration", "require_change", "edit_clojure",
    "apply_clojure_changes", "transform_clojure",
}


def utcnow() -> str:
    return datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")


def parse_utc(stamp: str) -> datetime | None:
    for fmt in ("%Y-%m-%dT%H:%M:%SZ", "%Y-%m-%dT%H:%M:%S.%fZ"):
        try:
            return datetime.strptime(stamp, fmt).replace(tzinfo=timezone.utc)
        except (ValueError, TypeError):
            continue
    try:
        return datetime.fromisoformat(str(stamp).replace("Z", "+00:00"))
    except Exception:
        return None


def strip_wrappers(tokens: list[str]) -> list[str]:
    i = 0
    while i < len(tokens):
        tok = tokens[i]
        if "=" in tok and not tok.startswith("=") and re.match(r"^[A-Za-z_][A-Za-z0-9_]*=", tok):
            i += 1                      # leading VAR=value assignment
            continue
        base = os.path.basename(tok)
        if base in WRAPPERS:
            i += 1
            continue
        if base in WRAPPERS_WITH_ARG:
            i += 1
            while i < len(tokens) and tokens[i].startswith("-"):
                i += 1          # the wrapper's own options
            i += WRAPPERS_WITH_ARG[base]   # then its operand (lock target, duration)
            continue
        break
    return tokens[i:]


def command_position_tokens(script: str) -> list[list[str]]:
    """Every simple command in `script`, tokenised, wrappers stripped."""
    out: list[list[str]] = []
    for piece in SPLITTERS.split(script):
        piece = piece.strip().strip("()")
        if not piece:
            continue
        try:
            tokens = shlex.split(piece)
        except ValueError:
            tokens = piece.split()
        tokens = strip_wrappers(tokens)
        if tokens:
            out.append(tokens)
    return out


MAX_MAKE_DEPTH = 4          # a target may reach a runner through another target


def load_make_map(path) -> dict | None:
    """The target -> expanded-recipe map `_make_targets.py` wrote at attest time."""
    try:
        data = json.loads(pathlib.Path(path).read_text())
    except Exception:
        return None
    targets = data.get("targets") if isinstance(data, dict) else None
    return targets if isinstance(targets, dict) and targets else None


def is_test_command(script: str, make_map: dict | None = None,
                    _depth: int = 0) -> tuple[bool, str | None]:
    """True when a TEST RUNNER stands at command position in `script`.

    `make <target>` is resolved through `make_map` -- the expansion `make -n` printed
    at attest time -- and NEVER guessed from the target's name.  Sol, item 4: any
    Make target wrapping Kaocha whose name does not match MAKE_TEST_TARGET bypassed
    the meter entirely, so `make verify` counted a whole test run as a non-test action.
    The name rule stays as a fallback for a run with no map.
    """
    for tokens in command_position_tokens(script):
        head, rest = tokens[0], tokens[1:]
        base = os.path.basename(head)
        if base in SHELL_RUNNERS and rest:
            # `bash -lc "<script>"` -- recurse into the inner script
            for j, tok in enumerate(rest):
                if tok.startswith("-"):
                    continue
                hit, why = is_test_command(tok, make_map, _depth)
                if hit:
                    return True, why
                break
            continue
        if base in TEST_BASENAMES:
            return True, head
        if base == "make":
            operands = [a for a in rest if not a.startswith("-")]
            if any(MAKE_TEST_TARGET.match(a) for a in operands):
                return True, f"make {' '.join(rest)}"
            if make_map and _depth < MAX_MAKE_DEPTH:
                for target in operands:
                    recipe = make_map.get(target)
                    if not recipe:
                        continue
                    hit, why = is_test_command(recipe, make_map, _depth + 1)
                    if hit:
                        return True, f"make {target} -> {why}"
            continue
        if base == "clojure" and any(CLOJURE_TEST_ALIAS.match(a) for a in rest):
            return True, f"clojure {' '.join(rest[:2])}"
        if base == "bb" and any(a.endswith("run_all.clj") or a.startswith("test/") for a in rest):
            return True, f"bb {' '.join(rest[:2])}"
    return False, None


# ---------------------------------------------------------------------------
# rollout normalisation -- codex rollout JSONL and claude -p stream-json
# ---------------------------------------------------------------------------
def _args_to_text(value) -> str:
    if value is None:
        return ""
    if isinstance(value, str):
        return value
    return json.dumps(value, sort_keys=True)


def _shell_script(args_text: str) -> str | None:
    """Pull a shell script out of a tool call's arguments, if it holds one."""
    try:
        obj = json.loads(args_text)
    except Exception:
        return None
    if not isinstance(obj, dict):
        return None
    for key in ("command", "cmd", "script", "input"):
        val = obj.get(key)
        if isinstance(val, list) and val:
            if len(val) >= 3 and os.path.basename(str(val[0])) in SHELL_RUNNERS:
                return str(val[-1])
            return " ".join(str(v) for v in val)
        if isinstance(val, str) and val:
            return val
    return None


def normalize(obj: dict) -> list[dict]:
    """One rollout line -> zero or more normalised events."""
    events: list[dict] = []
    ts = obj.get("timestamp") or obj.get("t")

    payload = obj.get("payload")
    if isinstance(payload, dict):                       # codex rollout
        ptype = payload.get("type")
        if ptype == "message" and payload.get("role") == "assistant":
            events.append({"kind": "return", "ts": ts})
        elif ptype in ("function_call", "custom_tool_call", "local_shell_call"):
            events.append({
                "kind": "call", "ts": ts,
                "tool": payload.get("name") or payload.get("tool") or "unknown",
                "args": _args_to_text(payload.get("arguments") or payload.get("input")),
                "call_id": payload.get("call_id") or payload.get("id"),
            })
        elif ptype in ("function_call_output", "custom_tool_call_output",
                       "local_shell_call_output"):
            out = payload.get("output")
            events.append({
                "kind": "call_output", "ts": ts,
                "call_id": payload.get("call_id") or payload.get("id"),
                "output": _args_to_text(out),
                "is_error": bool(payload.get("is_error")
                                 or (isinstance(out, dict) and out.get("is_error"))),
            })
        return events

    otype = obj.get("type")
    if otype == "assistant":                            # claude -p stream-json
        msg = obj.get("message") or {}
        events.append({"kind": "return", "ts": ts})
        for block in msg.get("content") or []:
            if isinstance(block, dict) and block.get("type") == "tool_use":
                events.append({
                    "kind": "call", "ts": ts,
                    "tool": block.get("name") or "unknown",
                    "args": _args_to_text(block.get("input")),
                    "call_id": block.get("id"),
                })
    elif otype == "user":
        msg = obj.get("message") or {}
        for block in msg.get("content") or []:
            if isinstance(block, dict) and block.get("type") == "tool_result":
                events.append({
                    "kind": "call_output", "ts": ts,
                    "call_id": block.get("tool_use_id"),
                    "output": _args_to_text(block.get("content")),
                    "is_error": bool(block.get("is_error")),
                })
    return events


ERROR_TYPE_KEYS = ("error_type", "errorType", "error-type", "refusal_type", "reason_code")
ERROR_TYPE_RE = re.compile(
    r'"(?:error_type|errorType|error-type|refusal_type)"\s*:\s*"([^"]+)"')


def extract_error_type(output: str) -> str | None:
    if not output:
        return None
    try:
        obj = json.loads(output)
    except Exception:
        obj = None
    stack = [obj] if obj is not None else []
    while stack:
        cur = stack.pop()
        if isinstance(cur, dict):
            for key in ERROR_TYPE_KEYS:
                val = cur.get(key)
                if isinstance(val, str) and val:
                    return val
            err = cur.get("error")
            if isinstance(err, str) and err:
                return err
            if isinstance(err, dict):
                for key in ("type", "code", "name", *ERROR_TYPE_KEYS):
                    val = err.get(key)
                    if isinstance(val, str) and val:
                        return val
            stack.extend(v for v in cur.values() if isinstance(v, (dict, list)))
        elif isinstance(cur, list):
            stack.extend(v for v in cur if isinstance(v, (dict, list)))
    match = ERROR_TYPE_RE.search(output)
    return match.group(1) if match else None


# ---------------------------------------------------------------------------
# tailing
# ---------------------------------------------------------------------------
class Tailer:
    def __init__(self, path: pathlib.Path):
        self.path = path
        self.handle = None
        self.buffer = ""

    def read_lines(self) -> list[str]:
        if self.handle is None:
            if not self.path.exists():
                return []
            self.handle = self.path.open("r", errors="replace")
        chunk = self.handle.read()
        if not chunk:
            return []
        self.buffer += chunk
        *lines, self.buffer = self.buffer.split("\n")
        return [ln for ln in lines if ln.strip()]

    def close(self):
        if self.handle:
            self.handle.close()


def discover_rollout(pattern: str, not_before: float) -> pathlib.Path | None:
    """The newest rollout WRITTEN AFTER this run started.

    `not_before` is the watcher's own start, with no slack: a file last touched
    before the driver launched belongs to an earlier session, and latching onto it
    would meter somebody else's run.  Arms are serial precisely so this is decidable.
    """
    hits = [p for p in glob.glob(pattern, recursive=True)
            if os.path.isfile(p) and os.path.getmtime(p) >= not_before]
    if not hits:
        return None
    return pathlib.Path(max(hits, key=os.path.getmtime))


# ---------------------------------------------------------------------------
def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--arm", required=True, help="the arm directory")
    ap.add_argument("--rollout", default=None,
                    help="JSONL to tail (default <arm>/rollout.jsonl)")
    ap.add_argument("--rollout-glob", default=None,
                    help="discover the newest matching rollout written after start")
    ap.add_argument("--capture-stdout", action="store_true",
                    help="the driver writes its JSONL on stdout (claude -p stream-json)")
    ap.add_argument("--make-map", default=None,
                    help="target -> expanded-recipe map (default <arm>/make-targets.json)")
    ap.add_argument("--zero-return-window", type=float, default=300.0)
    ap.add_argument("--idle-timeout", type=float, default=900.0)
    ap.add_argument("--max-wall", type=float, default=3600.0)
    ap.add_argument("--poll", type=float, default=0.25)
    ap.add_argument("cmd", nargs=argparse.REMAINDER)
    args = ap.parse_args()

    cmd = args.cmd[1:] if args.cmd and args.cmd[0] == "--" else args.cmd
    if not cmd:
        print("watch: no driver command given (use `-- <cmd...>`)", file=sys.stderr)
        return 2

    arm = pathlib.Path(args.arm).resolve()
    attest_path = arm / "attest.json"
    if not attest_path.exists():
        print(f"watch: no attestation at {attest_path}; refusing to run an "
              f"unattested arm", file=sys.stderr)
        return 2
    attest = json.loads(attest_path.read_text())
    if not attest.get("attest_ok"):
        print("watch: attest_ok is false; the arm must not launch a driver",
              file=sys.stderr)
        return 2
    start_dt = parse_utc(attest.get("start_utc", ""))
    if start_dt is None:
        print("watch: attest.json has no parseable start_utc", file=sys.stderr)
        return 2

    make_map = load_make_map(pathlib.Path(args.make_map) if args.make_map
                             else arm / "make-targets.json")

    watch_path = arm / "watch.jsonl"
    watch_path.write_text("")
    sink = watch_path.open("a")

    rollout_path = pathlib.Path(args.rollout) if args.rollout else arm / "rollout.jsonl"
    if args.capture_stdout:
        rollout_path = arm / "rollout.jsonl"
        rollout_path.write_text("")

    t0 = time.time()
    state = {"returns": 0, "seq": 0, "open": {}, "last_event": t0}

    def emit(record: dict) -> None:
        record = {"t": utcnow(),
                  "ms_since_start": int((time.time() - t0) * 1000),
                  **record}
        sink.write(json.dumps(record, sort_keys=False) + "\n")
        sink.flush()

    def handle(event: dict) -> None:
        state["last_event"] = time.time()
        if event["kind"] == "return":
            state["returns"] += 1
            emit({"kind": "return", "n": state["returns"]})
            return
        if event["kind"] == "call":
            state["seq"] += 1
            args_text = event.get("args") or ""
            script = _shell_script(args_text)
            test_call, why = (is_test_command(script, make_map) if script
                              else (False, None))
            tool = event.get("tool") or "unknown"
            short_tool = tool.split("__")[-1]
            is_patch = (
                short_tool == "apply_patch"
                or "*** Begin Patch" in args_text
                or (script is not None and APPLY_PATCH_CMD_RE.search(script) is not None)
            )
            targets = patch_targets(script or "", args_text) if is_patch else []
            record = {
                "kind": "call",
                "n": state["returns"],
                "seq": state["seq"],
                "tool": tool,
                "apply_patch": is_patch,
                "patch_files": targets,
                "patch_clj_files": [f for f in targets if f.endswith((".clj", ".cljc"))],
                "verb": short_tool if short_tool in COMMITTING_VERBS else None,
                "cmd_head": (script.strip().split("\n")[0][:200] if script else None),
                "test_call": test_call,
                "test_match": why,
                "args_sha256": hashlib.sha256(args_text.encode()).hexdigest(),
                "args_len": len(args_text),
                "args": args_text[:4000],
                "elapsed_ms": None,
                "outcome": None,
                "error_type": None,
                "started_ms": int((time.time() - t0) * 1000),
                "ts_event": event.get("ts"),
            }
            call_id = event.get("call_id") or f"seq-{state['seq']}"
            state["open"][call_id] = record
            return
        if event["kind"] == "call_output":
            call_id = event.get("call_id")
            record = state["open"].pop(call_id, None)
            if record is None and state["open"]:
                # no call_id correlation available: close the oldest open call
                oldest = next(iter(state["open"]))
                record = state["open"].pop(oldest)
            if record is None:
                return
            now_ms = int((time.time() - t0) * 1000)
            started = record.pop("started_ms")
            ts_start = parse_utc(record.pop("ts_event") or "")
            ts_end = parse_utc(event.get("ts") or "")
            if ts_start and ts_end:
                record["elapsed_ms"] = max(0, int((ts_end - ts_start).total_seconds() * 1000))
                record["elapsed_source"] = "rollout-timestamps"
            else:
                record["elapsed_ms"] = max(0, now_ms - started)
                record["elapsed_source"] = "watcher-observation"
            output = event.get("output") or ""
            error_type = extract_error_type(output)
            is_error = bool(event.get("is_error"))
            record["outcome"] = "error" if (is_error or error_type) else "ok"
            record["error_type"] = error_type or ("untyped" if is_error else None)
            if record["outcome"] != "ok":
                # kept ONLY for refusals, so the ledger's next_call fields (A.6) can
                # be computed rather than guessed
                record["output_head"] = output[:4000]
            emit(record)

    def flush_open(reason: str) -> int:
        """Emit every call still open, and RETURN HOW MANY there were.

        Sol, item 3: these were flushed as `no-output`, the watcher exited 0, and the
        scorer wrote a citeable receipt over a run whose last action has no outcome.
        A call with no result is missing evidence about what the tool did -- the run
        is incomplete, and an incomplete run is a typed refusal, not a smaller number.
        """
        pending = list(state["open"].values())
        for record in pending:
            record.pop("started_ms", None)
            record.pop("ts_event", None)
            record["outcome"] = "no-output"
            record["error_type"] = reason
            emit(record)
        state["open"].clear()
        return len(pending)

    # start_new_session puts the driver in its OWN session and process group, so the
    # group id equals its pid and every descendant it forks lands in that group.  Sol,
    # item 5: without this the timeout signalled one pid and the executed probe left
    # `sleep 60` running under PPID 1 -- an orphan of an aborted arm that keeps working
    # inside a run nobody is metering any more.
    proc = subprocess.Popen(cmd, stdin=subprocess.DEVNULL,
                            stdout=subprocess.PIPE if args.capture_stdout else None,
                            stderr=None,
                            text=True if args.capture_stdout else None,
                            start_new_session=True)
    try:
        driver_pgid = os.getpgid(proc.pid)
    except Exception:
        driver_pgid = proc.pid

    def group_members() -> list[int]:
        """Every live pid still in the driver's process group (never this watcher's)."""
        if driver_pgid == os.getpgrp():
            return []                   # refuse to enumerate -- or signal -- our own group
        found = []
        for entry in os.listdir("/proc"):
            if not entry.isdigit():
                continue
            try:
                if os.getpgid(int(entry)) == driver_pgid:
                    found.append(int(entry))
            except Exception:
                continue
        return found

    def kill_group(sig: int) -> None:
        if driver_pgid == os.getpgrp():
            return                      # never signal the group this watcher lives in
        try:
            os.killpg(driver_pgid, sig)
        except ProcessLookupError:
            pass
        except Exception:
            pass

    tailer: Tailer | None = None
    if not args.capture_stdout:
        if args.rollout_glob:
            tailer = None                      # discovered below
        else:
            tailer = Tailer(rollout_path)

    stdout_sink = rollout_path.open("a") if args.capture_stdout else None
    abort_reason: str | None = None

    def pump_lines(lines: list[str]) -> None:
        for line in lines:
            try:
                obj = json.loads(line)
            except Exception:
                continue
            if isinstance(obj, dict):
                for event in normalize(obj):
                    handle(event)

    try:
        while True:
            if args.capture_stdout:
                line = proc.stdout.readline() if proc.stdout else ""
                if line:
                    stdout_sink.write(line)
                    stdout_sink.flush()
                    pump_lines([line])
                    continue
            else:
                if tailer is None and args.rollout_glob:
                    found = discover_rollout(args.rollout_glob, t0)
                    if found is not None:
                        rollout_path = found
                        tailer = Tailer(found)
                if tailer is not None:
                    pump_lines(tailer.read_lines())

            done = proc.poll() is not None
            now = time.time()
            if state["returns"] == 0 and now - t0 > args.zero_return_window:
                abort_reason = "zero-returns"
                break
            if now - state["last_event"] > args.idle_timeout:
                abort_reason = "idle-stop"
                break
            if now - t0 > args.max_wall:
                abort_reason = "max-wall"
                break
            if done:
                # one final drain so the last events are not lost to the race
                time.sleep(min(1.0, args.poll * 4))
                if args.capture_stdout:
                    rest = proc.stdout.read() if proc.stdout else ""
                    if rest:
                        stdout_sink.write(rest)
                        stdout_sink.flush()
                        pump_lines(rest.split("\n"))
                elif tailer is not None:
                    pump_lines(tailer.read_lines())
                break
            time.sleep(args.poll)
    finally:
        if abort_reason:
            # signal the GROUP, not the pid: the driver's children are the orphans
            kill_group(signal.SIGTERM)
            time.sleep(2)
            kill_group(signal.SIGKILL)
        driver_rc = proc.wait()
        driver_group_orphans = len(group_members()) if abort_reason else 0
        if driver_group_orphans:
            time.sleep(1)
            kill_group(signal.SIGKILL)
            driver_group_orphans = len(group_members())
        if tailer is not None:
            tailer.close()
        if stdout_sink is not None:
            stdout_sink.close()

    incomplete = flush_open("driver-ended-before-output")

    end_utc = utcnow()
    end_dt = parse_utc(end_utc)
    wall_s = round((end_dt - start_dt).total_seconds(), 1) if end_dt else None

    # If a rollout was discovered elsewhere, land a copy in the arm directory so the
    # receipt and the rollout live together.
    if rollout_path.resolve() != (arm / "rollout.jsonl").resolve() and rollout_path.exists():
        (arm / "rollout.jsonl").write_bytes(rollout_path.read_bytes())

    calls = state["seq"]
    run = {
        "arm_dir": str(arm),
        "driver_cmd": cmd,
        "driver_rc": driver_rc,
        "driver_pid": proc.pid,
        "driver_pgid": driver_pgid,
        "driver_group_orphans": driver_group_orphans,
        "rollout_path": str(rollout_path),
        "start_utc": attest["start_utc"],
        "end_utc": end_utc,
        "wall_s": wall_s,
        "returns": state["returns"],
        "calls": calls,
        "abort": abort_reason,
        "calls_without_output": incomplete,
        "watch_jsonl": str(watch_path),
    }

    if state["returns"] == 0:
        emit({"kind": "abort", "error_type": "zero-returns",
              "detail": f"no assistant return in {int(time.time() - t0)}s; "
                        f"rollout={rollout_path}", "returns": 0})
        run["abort"] = run["abort"] or "zero-returns"
        (arm / "run.json").write_text(json.dumps(run, indent=2) + "\n")
        sink.close()
        print(f"WATCH-ABORT zero-returns arm={arm.name} rollout={rollout_path} "
              f"driver_rc={driver_rc}", file=sys.stderr)
        return 4

    if incomplete and not abort_reason:
        abort_reason = "incomplete-run"
        run["abort"] = abort_reason

    if abort_reason:
        emit({"kind": "abort", "error_type": abort_reason,
              "detail": f"returns={state['returns']} calls={calls} "
                        f"calls_without_output={incomplete}",
              "returns": state["returns"]})

    emit({"kind": "end", "end_utc": end_utc, "wall_s": wall_s,
          "returns": state["returns"], "calls": calls, "driver_rc": driver_rc})
    (arm / "run.json").write_text(json.dumps(run, indent=2) + "\n")
    sink.close()

    if incomplete:
        print(f"WATCH-ABORT incomplete-run arm={arm.name} "
              f"calls_without_output={incomplete} returns={state['returns']} "
              f"calls={calls} driver_rc={driver_rc}", file=sys.stderr)
        return 6

    print(f"watch: arm={arm.name} returns={state['returns']} calls={calls} "
          f"wall_s={wall_s} driver_rc={driver_rc} abort={abort_reason}")
    return 5 if abort_reason else 0


if __name__ == "__main__":
    sys.exit(main())
