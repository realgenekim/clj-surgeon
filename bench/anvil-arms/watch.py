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
  kind=header   the FIRST record of every stream: `schema_version`, and the identity
                of the rollout this run is bound to (st_dev, st_ino, session id).
                `kind=rollout-bound` repeats it when the rollout is only discovered
                after the driver announces its session.  The scorer REFUSES a stream
                carrying neither -- provenance travels with the evidence, or a later
                scorer is assuming the repair it is measuring.
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
6 incomplete run (a tool call whose result never arrived, or a `make` target the
attest-time map does not resolve); 7 the rollout could not be
bound to THIS driver's own announced session; 8 the bound rollout was ROTATED --
replaced or truncated -- while the run was being metered, or its binding could not be
re-checked at all (`rollout-stat-failed:<ERRNO>`).
The driver's own exit status is recorded in run.json, never conflated with these.
"""
from __future__ import annotations

import argparse
import errno
import hashlib
import json
import os
import pathlib
import re
import resource
import shlex
import signal
import subprocess
import sys
import time
from datetime import datetime, timezone

# The version of the watch-stream contract this watcher writes.  It is the FIRST
# record of every stream, and it carries the identity of the rollout the run was bound
# to.  Sol round three, finding (e): a scorer that cannot tell WHICH watcher wrote a
# stream is assuming the repair it is measuring -- Sol copied a pre-repair split-brain
# artifact into the current scorer and got rc 0 and `sources.agree=true` over evidence
# that was two different files.  Provenance travels with the stream or it does not exist.
WATCH_SCHEMA_VERSION = 2

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
# a command-line variable assignment to `make` (`CMD=bin/kaocha`, `V:=1`, `X+=y`) --
# GNU Make accepts it anywhere among the operands, and it can change what a target's
# recipe actually runs without changing the target name at all.
MAKE_ASSIGNMENT_RE = re.compile(r"^[A-Za-z_][A-Za-z0-9_]*(:=|::=|\+=|\?=|=)")


# Environment variables GNU Make itself reads at startup and treats as if they were
# additional command-line flags/assignments (info make "Options/Recursion",
# "Communicating Options" and "MAKEFILES Variable").  Sol round five: a LEADING
# `MAKEFLAGS=CMD=/bin/echo` on the invocation is not a plain environment variable to
# the recipe -- GNU Make folds it into its own option/variable parsing exactly as if
# `CMD=/bin/echo` had been typed on the command line, and it substituted into
# `verify`'s recipe while the attest-time map still named the un-overridden one.
MAKE_AFFECTING_ENV = {"MAKEFLAGS", "MAKEOVERRIDES", "GNUMAKEFLAGS", "MAKEFILES",
                      "MAKELEVEL"}


def make_runtime_override(rest: list[str]) -> str | None:
    """The first token in a `make` invocation's arguments the attest-time map cannot
    account for: a variable assignment, or any option at all.

    Sol round four: `make CMD=bin/kaocha verify` ran the Kaocha stub -- GNU Make
    substituted the override into `verify`'s recipe -- while `_make_targets.py`'s map
    was built at attest time under the Makefile's own default `CMD`, so the map still
    named `verify`'s recipe as whatever it resolves to WITHOUT the override.  The
    meter classified the call non-test and "resolved" it against a recipe that was
    not the one GNU Make actually ran.  No make option is on a known-safe list here,
    so any `-`-prefixed argument is refused as an unknown option, same as any
    assignment -- both change what will run in a way this parser cannot see.

    Sol round five, item 2: a bare `--` is GNU Make's own end-of-options marker --
    inert by itself -- and was being refused as if it were an unknown option.  It is
    skipped here; anything AFTER it that still looks like an option is still refused.
    """
    for tok in rest:
        if tok == "--":
            continue
        if MAKE_ASSIGNMENT_RE.match(tok):
            return tok
        if tok.startswith("-") and tok != "-":
            return tok
    return None

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


def strip_wrappers(tokens: list[str]) -> tuple[list[str], list[str]]:
    """Returns (leading_env_assignments, remaining_tokens).

    Sol round five, item 1: a leading `VAR=value` used to be discarded outright, so a
    command carrying `MAKEFLAGS=CMD=/bin/echo make verify` was indistinguishable from
    plain `make verify` by the time any `make`-specific check ran.  The assignments
    are now kept alongside the stripped tokens so a caller that cares which NAMES were
    set (Make-affecting ones, in particular) still can.
    """
    i = 0
    env_assignments: list[str] = []
    while i < len(tokens):
        tok = tokens[i]
        if "=" in tok and not tok.startswith("=") and re.match(r"^[A-Za-z_][A-Za-z0-9_]*=", tok):
            env_assignments.append(tok)   # leading VAR=value assignment
            i += 1
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
    return env_assignments, tokens[i:]


def command_position_tokens(script: str) -> list[tuple[list[str], list[str]]]:
    """Every simple command in `script`, tokenised, wrappers stripped.

    Each entry is `(leading_env_assignments, tokens)` -- see `strip_wrappers`.
    """
    out: list[tuple[list[str], list[str]]] = []
    for piece in SPLITTERS.split(script):
        piece = piece.strip().strip("()")
        if not piece:
            continue
        try:
            tokens = shlex.split(piece)
        except ValueError:
            tokens = piece.split()
        env_assignments, tokens = strip_wrappers(tokens)
        if tokens:
            out.append((env_assignments, tokens))
    return out


def make_affecting_env_override(env_assignments: list[str]) -> str | None:
    """A leading env assignment whose NAME GNU Make itself reads at startup.

    Sol round five, item 1: `MAKEFLAGS=CMD=/bin/echo` is not an ordinary environment
    variable as far as the recipe is concerned -- GNU Make parses `MAKEFLAGS` (and
    `MAKEOVERRIDES`/`GNUMAKEFLAGS`/`MAKEFILES`/`MAKELEVEL`) out of its own environment
    and folds their content into its option/variable handling, same as if it had been
    typed on the command line.  The attest-time map cannot see through it.
    """
    for assignment in env_assignments:
        name = assignment.split("=", 1)[0]
        if name in MAKE_AFFECTING_ENV:
            return name
    return None


MAX_MAKE_DEPTH = 4          # a target may reach a runner through another target

# How often the main loop takes a /proc descendant snapshot (Sol round four: named so
# the receipt can report the meter's own cost against the interval it actually ran
# at, not a number quoted from memory of the source).
DESCENDANT_SCAN_INTERVAL_S = 0.25


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
    for env_assignments, tokens in command_position_tokens(script):
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
            # A runtime assignment/option (Sol round four), OR a leading Make-affecting
            # environment assignment (Sol round five, item 1), means the attest-time
            # map cannot be trusted for this call -- GNU Make may substitute a
            # different recipe than the one the map recorded.  Do not resolve through
            # it; the name check above still catches an explicitly-named test target.
            # `unresolved_make_targets` is what turns this into an incomplete-run.
            if (make_map and _depth < MAX_MAKE_DEPTH
                    and not make_runtime_override(rest)
                    and not make_affecting_env_override(env_assignments)):
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


def unresolved_make_targets(script: str, make_map: dict | None,
                            _depth: int = 0) -> list[str]:
    """Make targets the driver typed that the attest-time map does not resolve.

    Sol round two, item 6: an unknown or conditional target fell through to the name
    rule and was metered as one more NON-TEST ACTION -- the exact quantity E3's pass
    line is stated in, so an unmetered test run landed on the other side of the
    comparison rather than merely going missing.  What a target RUNS is either in the
    map or it is not known, and not known is `incomplete-run`, never a smaller number.

    Only the targets the driver actually typed are reported.  Recursion through the
    map is the test predicate's business, and a prerequisite that happens to be a file
    is not evidence about anything the driver did.
    """
    if _depth > 3:
        return []
    out: list[str] = []
    for env_assignments, tokens in command_position_tokens(script):
        head, rest = tokens[0], tokens[1:]
        base = os.path.basename(head)
        if base in SHELL_RUNNERS and rest:
            for tok in rest:
                if tok.startswith("-"):
                    continue
                out.extend(unresolved_make_targets(tok, make_map, _depth + 1))
                break
            continue
        if base != "make":
            continue
        env_override = make_affecting_env_override(env_assignments)
        if env_override:
            # Sol round five, item 1: a leading `MAKEFLAGS=CMD=/bin/echo` changed
            # GNU Make's own option/variable parsing -- it substituted into `verify`'s
            # recipe -- while carrying no assignment or option in `rest` at all, so
            # `make_runtime_override(rest)` alone saw nothing to refuse.
            out.append(f"make-runtime-override:env:{env_override}")
            continue
        override = make_runtime_override(rest)
        if override:
            # Sol round four: `make CMD=bin/kaocha verify` ran the Kaocha stub while
            # the attest-time map still named `verify`'s default-CMD recipe, so the
            # meter classified it non-test and "resolved" it.  Typed and
            # unconditional -- this is unresolved regardless of whether the named
            # target happens to be in the map, because the map cannot speak to what
            # the override changes.
            out.append(f"make-runtime-override:{override}")
            continue
        operands = [a for a in rest if not a.startswith("-") and "=" not in a]
        if not operands:
            # `make` with no goal: which recipe runs depends on .DEFAULT_GOAL and on
            # declaration order, and the map does not carry that.
            out.append("(default-goal)")
            continue
        for target in operands:
            if not make_map or target not in make_map:
                out.append(target)
    return out


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
    """One open fd on ONE inode, bound at open time and never re-resolved by path.

    Sol round two, item 2: the watcher read this fd while the retained copy was
    taken by PATH after the run.  A rollout replaced mid-run therefore produced two
    witnesses of two different files -- each internally consistent, so `sources.agree`
    said true -- and a receipt reporting a verb call the surviving bytes do not
    contain.  The identity is (st_ino, st_dev) taken from the fd itself, the retained
    copy comes from that same fd, and a path that stops naming it is a typed abort.
    """

    def __init__(self, path: pathlib.Path):
        self.path = path
        self.handle = None
        self.buffer = ""
        self.ino = None
        self.dev = None
        self.rotation: str | None = None
        # which typed abort this tailer is asking for: `rollout-rotated`, or
        # `rollout-stat-failed:<ERRNO>` when the binding itself could not be READ
        self.abort_kind: str | None = None

    def read_lines(self) -> list[str]:
        if self.handle is None:
            if not self.path.exists():
                return []
            self.handle = self.path.open("r", errors="replace")
            bound = os.fstat(self.handle.fileno())
            self.ino, self.dev = bound.st_ino, bound.st_dev
        chunk = self.handle.read()
        if not chunk:
            return []
        self.buffer += chunk
        *lines, self.buffer = self.buffer.split("\n")
        return [ln for ln in lines if ln.strip()]

    def _stat_failed(self, exc: OSError, where: str) -> str:
        """A stat that ERRORED is not a stat that said `no rotation`.

        Sol round three, finding (c): both of these were swallowed and read as "the
        binding still holds".  On NFS an ESTALE means exactly the opposite -- the fd
        no longer refers to a file this watcher can reason about -- and every count
        taken after it describes bytes nobody re-checked.  Typed, fail closed, rc 8.
        """
        name = errno.errorcode.get(exc.errno, str(exc.errno)) if exc.errno else "UNKNOWN"
        self.abort_kind = f"rollout-stat-failed:{name}"
        self.rotation = (f"{where} failed on {self.path}: {name} ({exc.strerror or exc}); "
                         f"the binding to inode {self.dev}:{self.ino} could not be "
                         f"re-checked, so nothing after this point is metered evidence")
        return self.rotation

    def check_rotation(self) -> str | None:
        """A typed reason the bound path no longer names the bound inode, or None."""
        if self.handle is None or self.ino is None:
            return None
        try:
            live = self.path.stat()
        except FileNotFoundError:
            self.abort_kind = "rollout-rotated"
            self.rotation = f"unlinked (inode {self.ino} is no longer at {self.path})"
            return self.rotation
        except OSError as exc:
            return self._stat_failed(exc, "stat()")
        if (live.st_ino, live.st_dev) != (self.ino, self.dev):
            self.abort_kind = "rollout-rotated"
            self.rotation = (f"inode-changed at {self.path} "
                             f"({self.dev}:{self.ino} -> {live.st_dev}:{live.st_ino})")
            return self.rotation
        try:
            bound = os.fstat(self.handle.fileno())
        except OSError as exc:
            return self._stat_failed(exc, "fstat()")
        if live.st_size < bound.st_size:
            self.abort_kind = "rollout-rotated"
            self.rotation = (f"truncated in place at {self.path} "
                             f"({bound.st_size} -> {live.st_size} bytes)")
            return self.rotation
        return None

    def identity(self) -> tuple[int | None, int | None]:
        """(st_dev, st_ino) of the inode this tailer is bound to, or (None, None)."""
        return self.dev, self.ino

    def snapshot(self) -> bytes:
        """The whole file AS THIS WATCHER HOLDS IT -- from its own fd, never by path."""
        if self.handle is None:
            return b""
        try:
            with os.fdopen(os.dup(self.handle.fileno()), "rb") as dup:
                dup.seek(0)
                return dup.read()
        except OSError:
            return b""

    def close(self):
        if self.handle:
            self.handle.close()


# The session the driver announces about ITSELF -- the only identity that binds a
# rollout to this arm.  Sol, item 8 (blocker): newest-mtime discovery selected a
# concurrent session's rollout and then latched it permanently, and cohort seriality
# does not prevent another Codex session from existing on a shared box.  There is no
# mtime rule here and no glob over a shared home: each arm gets a PRIVATE CODEX_HOME
# and the file must name the session id the driver printed.
# How much of the driver's own output the BINDING scan reads.  A bound read, because an
# unbounded one on a chatty driver is its own hazard.  A session id announced past it is
# not bound -- and the abort says exactly that, rather than claiming silence.
BANNER_SCAN_BYTES = 65536
LATE_SCAN_BYTES = 4 * 1024 * 1024       # how far the DIAGNOSTIC looks, after refusing

UUID_RE = r"[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}"
SESSION_ID_RE = re.compile(r"session[\s_-]*id\s*[:=]?\s*(" + UUID_RE + r")", re.I)
ROLLOUT_PATH_RE = re.compile(r"(/\S*rollout-[^\s\"'`]*\.jsonl)")


def proc_stat(pid: int) -> dict | None:
    """`state`, `ppid` and `starttime` for one pid, or None if it is gone.

    Everything up to the LAST ')' is the pid and the comm (which may itself contain
    spaces and parentheses), so the tail begins at stat field 3: state, ppid, ... and
    starttime is its 20th token.  starttime is what distinguishes a recorded process
    from whatever later wears its number.
    """
    try:
        raw = pathlib.Path(f"/proc/{pid}/stat").read_text()
    except OSError:
        return None
    head, _, tail = raw.rpartition(")")
    if not head:
        return None
    fields = tail.split()
    if len(fields) < 20:
        return None
    try:
        return {"state": fields[0], "ppid": int(fields[1]), "starttime": fields[19]}
    except ValueError:
        return None


PR_SET_CHILD_SUBREAPER = 36     # <linux/prctl.h>; no privilege required


def set_child_subreaper() -> str:
    """Make THIS process the reaper for its orphaned descendants.  Linux, no sudo.

    Sol round three, finding (b): a child that forks a grandchild and exits between
    two /proc scans loses the race by construction -- the grandchild re-parents to
    init and the walk from the driver's pid can never reach it again.  No polling
    interval fixes that; the walk is sampling a tree that has already been rewritten.

    `prctl(PR_SET_CHILD_SUBREAPER, 1)` rewrites it in our favour instead: an orphan
    below this process re-parents to the WATCHER, not to init, so it stays inside the
    subtree the final scan walks and inside the set the reap may signal.  Sol round
    four: this comment previously said the setting "is inherited by the driver" --
    it is not, and the implementation never relies on that claim.  The flag is set
    HERE, on this watcher process, once, before the driver is even forked; the driver
    itself carries no subreaper flag of its own and does not need one.  Reparenting
    on Linux climbs the ancestor chain to the NEAREST process that has the flag set,
    which is this watcher regardless of how many generations of descendants the
    driver forks.  So it applies to every descendant forked after this call, and it
    needs no privileges.  The per-poll walk stays as belt and braces.
    """
    try:
        import ctypes
        libc = ctypes.CDLL("libc.so.6", use_errno=True)
        if libc.prctl(PR_SET_CHILD_SUBREAPER, 1, 0, 0, 0) != 0:
            return f"prctl-failed:errno={ctypes.get_errno()}"
        return "ok"
    except Exception as exc:                        # not Linux, or no libc
        return f"prctl-unavailable:{type(exc).__name__}"


def descendants_of(root_pid: int) -> dict[int, str]:
    """Every live descendant of `root_pid` right now -> its start time.

    Sol round two, item 5: the reap walked the driver's process GROUP, and a
    descendant that calls setsid is in a different group by definition.  It is still
    a CHILD, though -- setsid(1) does not fork when it is not already a group leader,
    and even when it does, the process is a child until its parent dies.  So the
    descendant set is walked from /proc while the driver is alive, and remembered.
    """
    children: dict[int, list[int]] = {}
    starts: dict[int, str] = {}
    for entry in os.listdir("/proc"):
        if not entry.isdigit():
            continue
        pid = int(entry)
        info = proc_stat(pid)
        if info is None:
            continue
        children.setdefault(info["ppid"], []).append(pid)
        starts[pid] = info["starttime"]
    found: dict[int, str] = {}
    root = proc_stat(root_pid)
    if root is not None:
        found[root_pid] = starts.get(root_pid, "")
    stack = [root_pid]
    seen = {root_pid}
    while stack:
        cur = stack.pop()
        for kid in children.get(cur, ()):
            if kid in seen:
                continue
            seen.add(kid)
            found[kid] = starts.get(kid, "")
            stack.append(kid)
    return found


class RolloutBindingError(RuntimeError):
    pass


def bind_rollout(codex_home: pathlib.Path,
                 text: str) -> tuple[pathlib.Path | None, str | None]:
    """The EXACT rollout of the session whose own output is `text`, or (None, None).

    Two accepted witnesses, both spoken by the session itself: a rollout path it
    printed, or a session id it printed that exactly one file in THIS arm's private
    home names.  Anything else -- including "the newest file" -- is not a binding.
    More than one candidate is an error, never a choice.
    """
    root = codex_home.resolve()
    for match in ROLLOUT_PATH_RE.finditer(text):
        candidate = pathlib.Path(match.group(1))
        if not candidate.is_file():
            continue
        try:
            candidate.resolve().relative_to(root)
        except ValueError:
            continue            # a path outside this arm's own home is not this arm's
        return candidate, "printed-path"

    match = SESSION_ID_RE.search(text)
    if not match:
        return None, None
    session_id = match.group(1)
    hits = sorted(p for p in root.glob(f"**/*{session_id}*.jsonl") if p.is_file())
    if len(hits) > 1:
        raise RolloutBindingError(
            f"rollout-ambiguous session={session_id} candidates={[str(h) for h in hits]}")
    if not hits:
        return None, None
    return hits[0], f"session-id:{session_id}"


# ---------------------------------------------------------------------------
def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--arm", required=True, help="the arm directory")
    ap.add_argument("--rollout", default=None,
                    help="JSONL to tail (default <arm>/rollout.jsonl)")
    ap.add_argument("--codex-home", default=None,
                    help="this arm's PRIVATE CODEX_HOME; the rollout is bound to the "
                         "session id the driver announces, never to a newest-mtime glob")
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
    codex_home = pathlib.Path(args.codex_home).resolve() if args.codex_home else None
    if codex_home is not None:
        codex_home.mkdir(parents=True, exist_ok=True)
    rollout_binding: str | None = None

    watch_path = arm / "watch.jsonl"
    watch_path.write_text("")
    sink = watch_path.open("a")

    rollout_path = pathlib.Path(args.rollout) if args.rollout else arm / "rollout.jsonl"
    if args.capture_stdout:
        rollout_path = arm / "rollout.jsonl"
        rollout_path.write_text("")

    t0 = time.time()
    cpu0 = resource.getrusage(resource.RUSAGE_SELF)
    state = {"returns": 0, "seq": 0, "open": {}, "last_event": t0, "last_scan": 0.0,
             "make_unresolved": [], "bound": False, "scans": 0}

    def emit(record: dict) -> None:
        record = {"t": utcnow(),
                  "ms_since_start": int((time.time() - t0) * 1000),
                  **record}
        sink.write(json.dumps(record, sort_keys=False) + "\n")
        sink.flush()

    def emit_binding(kind: str, dev, ino, session, how) -> None:
        """The rollout identity this run is metered against, written into the stream.

        `header` is the first record of every stream; `rollout-bound` follows it when
        the rollout is discovered later (a codex session id is announced by the driver
        after the watcher starts).  The scorer takes the last of them as the identity
        and refuses any stream carrying neither.
        """
        emit({"kind": kind, "schema_version": WATCH_SCHEMA_VERSION,
              "watcher": "anvil-arms/watch.py", "arm": arm.name,
              "rollout_path": str(rollout_path), "rollout_dev": dev,
              "rollout_ino": ino, "session_id": session, "rollout_binding": how})

    def stat_ids(path: pathlib.Path):
        try:
            info = path.stat()
            return info.st_dev, info.st_ino
        except OSError:
            return None, None

    def session_of(binding) -> str | None:
        prefix = "session-id:"
        if isinstance(binding, str) and binding.startswith(prefix):
            return binding[len(prefix):]
        return None

    # THE FIRST RECORD OF EVERY STREAM.  Written before any event, so a stream that
    # stops after one line still says which contract it was written under.
    header_dev, header_ino = stat_ids(rollout_path)
    emit_binding("header", header_dev, header_ino, None,
                 "capture-stdout" if args.capture_stdout else None)

    def note_binding() -> None:
        """Announce the bound inode ONCE, the moment the tailer actually has one."""
        if tailer is None or tailer.ino is None or state["bound"]:
            return
        state["bound"] = True
        dev, ino = tailer.identity()
        emit_binding("rollout-bound", dev, ino, session_of(rollout_binding),
                     rollout_binding)

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
            unresolved_targets = (unresolved_make_targets(script, make_map)
                                  if script else [])
            state["make_unresolved"].extend(unresolved_targets)
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
                "make_unresolved": unresolved_targets,
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
    driver_out_path = arm / "driver-output.log"
    driver_out = None
    if codex_home is not None and not args.capture_stdout:
        # the driver announces its session on its own stdout/stderr, so the watcher
        # must be able to read it back; bounded to the head of the file when scanned
        driver_out = driver_out_path.open("w")

    def driver_banner() -> str:
        try:
            with driver_out_path.open("rb") as handle:
                return handle.read(BANNER_SCAN_BYTES).decode("utf-8", errors="replace")
        except OSError:
            return ""

    def late_session_id() -> tuple[str, int] | None:
        """A session id the driver DID announce, past the banner scan ceiling.

        Sol round two: an id announced beyond the ceiling failed closed -- correctly --
        and the abort said "the driver never announced a session id", which is a false
        statement about a driver that announced one.  The ceiling stays (an unbounded
        read of a driver's output is its own hazard); the SENTENCE has to be true.  The
        offset is approximate: it is counted in the decoded text past the ceiling.
        """
        try:
            with driver_out_path.open("rb") as handle:
                handle.seek(BANNER_SCAN_BYTES)
                rest = handle.read(LATE_SCAN_BYTES).decode("utf-8", errors="replace")
        except OSError:
            return None
        match = SESSION_ID_RE.search(rest)
        if not match:
            return None
        return match.group(1), BANNER_SCAN_BYTES + match.start(1)

    # BEFORE the driver exists: a subreaper set afterwards would not adopt orphans of
    # children forked in between.
    child_subreaper = set_child_subreaper()

    proc = subprocess.Popen(cmd, stdin=subprocess.DEVNULL,
                            stdout=(subprocess.PIPE if args.capture_stdout
                                    else (driver_out or None)),
                            stderr=(subprocess.STDOUT if driver_out else None),
                            text=True if args.capture_stdout else None,
                            start_new_session=True)
    try:
        driver_pgid = os.getpgid(proc.pid)
    except Exception:
        driver_pgid = proc.pid

    def group_members() -> list[int]:
        """Every LIVE pid still in the driver's process group (never this watcher's).

        A zombie is not a survivor, and since this watcher became a child subreaper it
        SEES zombies it did not use to see: an orphan that once re-parented to init and
        vanished the same instant now re-parents here and stays in /proc, in the
        driver's group, until this process waits on it.  Counting those as orphans
        reported a survivor of a reap that had in fact worked.
        """
        if driver_pgid == os.getpgrp():
            return []                   # refuse to enumerate -- or signal -- our own group
        found = []
        for entry in os.listdir("/proc"):
            if not entry.isdigit():
                continue
            try:
                if os.getpgid(int(entry)) != driver_pgid:
                    continue
            except Exception:
                continue
            info = proc_stat(int(entry))
            if info is None or info["state"] == "Z":
                continue
            found.append(int(entry))
        return found

    # Every descendant this watcher has EVER seen alive, pid -> start time.  A group
    # walk at abort time cannot see a process that left the group, and a process that
    # left the group is exactly the one worth catching.
    recorded: dict[int, str] = {}

    def record_descendants() -> None:
        # From the WATCHER's own pid, not the driver's: with PR_SET_CHILD_SUBREAPER an
        # orphaned grandchild re-parents HERE, so it is a descendant of this process
        # even after the driver and its child are both gone.  The driver's subtree is
        # inside this walk, so nothing that was visible before is lost.
        for pid, start in descendants_of(os.getpid()).items():
            if pid in (os.getpid(), os.getppid(), 1):
                continue
            recorded.setdefault(pid, start)

    def reap_adopted() -> None:
        """Wait on every zombie this watcher has adopted, so none is left behind."""
        while True:
            try:
                pid, _ = os.waitpid(-1, os.WNOHANG)
            except ChildProcessError:
                return
            except OSError:
                return
            if pid == 0:
                return

    def still_alive(pid: int, start: str) -> bool:
        info = proc_stat(pid)
        if info is None or info["state"] == "Z":
            return False
        return not start or info["starttime"] == start

    def signal_recorded(sig: int) -> None:
        """Signal each RECORDED pid individually, start time verified.

        A pid alone is not an identity -- pids are reused -- so a recorded pid whose
        start time has changed is somebody else's process and is never signalled.
        """
        for pid, start in recorded.items():
            if pid in (os.getpid(), os.getppid(), 1) or not still_alive(pid, start):
                continue
            try:
                os.kill(pid, sig)
            except Exception:
                pass

    def live_recorded() -> list[int]:
        return sorted(pid for pid, start in recorded.items() if still_alive(pid, start))

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
    retained: bytes | None = None       # the metered bytes, taken from the open fd
    if not args.capture_stdout and codex_home is None:
        tailer = Tailer(rollout_path)

    stdout_sink = rollout_path.open("a") if args.capture_stdout else None
    abort_reason: str | None = None
    bind_error: str | None = None
    rotation_detail: str | None = None

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
                if tailer is None and codex_home is not None:
                    try:
                        found, how = bind_rollout(codex_home, driver_banner())
                    except RolloutBindingError as exc:
                        bind_error = str(exc)
                        abort_reason = "rollout-ambiguous"
                        break
                    if found is not None:
                        rollout_path = found
                        rollout_binding = how
                        tailer = Tailer(found)
                if tailer is not None:
                    pump_lines(tailer.read_lines())
                    note_binding()
                    if tailer.check_rotation():
                        abort_reason = tailer.abort_kind or "rollout-rotated"
                        break

            done = proc.poll() is not None
            now = time.time()
            if now - state["last_scan"] >= DESCENDANT_SCAN_INTERVAL_S:
                state["last_scan"] = now
                state["scans"] += 1
                record_descendants()
            if (codex_home is not None and tailer is None
                    and now - t0 > args.zero_return_window):
                abort_reason = "rollout-unbound"
                break
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
                    note_binding()
                    if tailer.check_rotation():
                        abort_reason = tailer.abort_kind or "rollout-rotated"
                break
            time.sleep(args.poll)
    finally:
        # One last snapshot while the driver may still be alive: a descendant that left
        # the group is visible as a CHILD only until its parent dies.
        record_descendants()
        if abort_reason:
            # the GROUP first -- and then every pid recorded while the driver lived,
            # because a descendant that called setsid is not in that group
            kill_group(signal.SIGTERM)
            signal_recorded(signal.SIGTERM)
            time.sleep(2)
            kill_group(signal.SIGKILL)
            signal_recorded(signal.SIGKILL)
        driver_rc = proc.wait()
        # AFTER the driver is gone: this is exactly when an adopted orphan becomes
        # visible as a child of the watcher rather than a descendant of the driver.
        record_descendants()
        reap_adopted()
        driver_group_orphans = len(group_members()) if abort_reason else 0
        if driver_group_orphans:
            time.sleep(1)
            kill_group(signal.SIGKILL)
            driver_group_orphans = len(group_members())
        # COMPUTED, never assumed: a final /proc scan of the pids actually recorded.
        # An arm that leaves a process behind contaminates the next one, so the reap
        # runs on every path -- and whatever survives it is reported by pid.
        orphan_pids = live_recorded()
        if orphan_pids:
            kill_group(signal.SIGKILL)
            signal_recorded(signal.SIGKILL)
            time.sleep(1)
            record_descendants()
            reap_adopted()
            orphan_pids = live_recorded()
        reap_adopted()
        if tailer is not None:
            retained = tailer.snapshot()
            rotation_detail = tailer.rotation
            tailer.close()
        if stdout_sink is not None:
            stdout_sink.close()
        if driver_out is not None:
            driver_out.close()

    if codex_home is not None and tailer is None and abort_reason is None:
        try:
            found, how = bind_rollout(codex_home, driver_banner())
        except RolloutBindingError as exc:
            bind_error, found, how = str(exc), None, None
            abort_reason = "rollout-ambiguous"
        if found is not None:
            rollout_path, rollout_binding = found, how
            tailer = Tailer(found)
            pump_lines(tailer.read_lines())
            note_binding()
            if tailer.check_rotation():
                abort_reason = tailer.abort_kind or "rollout-rotated"
                rotation_detail = tailer.rotation
            retained = tailer.snapshot()
            tailer.close()
        elif abort_reason is None:
            abort_reason = "rollout-unbound"

    incomplete = flush_open("driver-ended-before-output")
    unresolved_targets = sorted(set(state["make_unresolved"]))

    end_utc = utcnow()
    end_dt = parse_utc(end_utc)
    wall_s = round((end_dt - start_dt).total_seconds(), 1) if end_dt else None

    # Sol round four, item 3: the meter's OWN cost, MEASURED -- never assumed from the
    # scan interval alone, because how much CPU a scan actually costs depends on how
    # many descendants there are to walk.  RUSAGE_SELF is this PROCESS's own
    # user+system time, so it is exactly the watcher's overhead and nothing the driver
    # or its children did.
    cpu1 = resource.getrusage(resource.RUSAGE_SELF)
    watcher_cpu_s = round((cpu1.ru_utime - cpu0.ru_utime) +
                          (cpu1.ru_stime - cpu0.ru_stime), 3)

    # If a rollout was discovered elsewhere, land a copy in the arm directory so the
    # receipt and the rollout live together.
    if rollout_path.resolve() != (arm / "rollout.jsonl").resolve():
        # FROM THE OPEN FD.  Reading the path again here is what let a replacement
        # inode become the retained evidence for bytes this watcher never metered.
        if retained is not None:
            (arm / "rollout.jsonl").write_bytes(retained)
        elif rollout_path.exists():
            (arm / "rollout.jsonl").write_bytes(rollout_path.read_bytes())

    calls = state["seq"]
    run = {
        "arm_dir": str(arm),
        "driver_cmd": cmd,
        "driver_rc": driver_rc,
        "driver_pid": proc.pid,
        "driver_pgid": driver_pgid,
        "driver_group_orphans": driver_group_orphans,
        "child_subreaper": child_subreaper,
        "descendants_recorded": len(recorded),
        "descendant_pids": sorted(recorded),
        "orphans_after_reap": len(orphan_pids),
        "orphan_pids": orphan_pids,
        "rollout_path": str(rollout_path),
        "rollout_binding": rollout_binding,
        "rollout_rotation": rotation_detail,
        "codex_home": str(codex_home) if codex_home else None,
        "start_utc": attest["start_utc"],
        "end_utc": end_utc,
        "wall_s": wall_s,
        "returns": state["returns"],
        "calls": calls,
        "abort": abort_reason,
        "calls_without_output": incomplete,
        "unresolved_make_targets": unresolved_targets,
        "watch_jsonl": str(watch_path),
        "watcher_cpu_s": watcher_cpu_s,
        "scans": state["scans"],
        "scan_interval_ms": int(DESCENDANT_SCAN_INTERVAL_S * 1000),
    }

    if abort_reason and abort_reason.split(":")[0] in ("rollout-rotated",
                                                       "rollout-stat-failed"):
        # Either the bound path stopped naming the bound inode, or the binding could
        # not be RE-CHECKED at all (Sol round three, finding (c): an ESTALE was read as
        # "no rotation").  Both mean the same thing: no count after this point
        # describes one session.  Refuse, and keep the bytes actually metered as the
        # retained evidence.
        detail = (rotation_detail or "the bound rollout was replaced mid-run") + \
                 "; the metered bytes are retained from the watcher's own fd"
        emit({"kind": "abort", "error_type": abort_reason, "detail": detail,
              "returns": state["returns"]})
        run["abort"] = abort_reason
        (arm / "run.json").write_text(json.dumps(run, indent=2) + "\n")
        sink.close()
        print(f"WATCH-ABORT {abort_reason} arm={arm.name} rollout={rollout_path} "
              f"driver_rc={driver_rc} detail={detail}", file=sys.stderr)
        return 8

    if abort_reason in ("rollout-unbound", "rollout-ambiguous"):
        late = None if bind_error else late_session_id()
        if bind_error:
            detail = bind_error
        elif late is not None:
            session_id, offset = late
            detail = (
                f"the driver announced session {session_id} at about byte {offset} of "
                f"its own output, past the {BANNER_SCAN_BYTES}-byte banner scan "
                f"ceiling, so the binding scan never reached it; nothing was metered "
                f"and nothing was guessed")
        else:
            detail = (
                f"the driver never announced a session id in the first "
                f"{BANNER_SCAN_BYTES} bytes of its output and none appears after them, "
                f"and no rollout under {codex_home} names one; nothing was metered and "
                f"nothing was guessed")
        emit({"kind": "abort", "error_type": abort_reason, "detail": detail,
              "returns": state["returns"]})
        run["abort"] = abort_reason
        (arm / "run.json").write_text(json.dumps(run, indent=2) + "\n")
        sink.close()
        print(f"WATCH-ABORT {abort_reason} arm={arm.name} codex_home={codex_home} "
              f"driver_rc={driver_rc} detail={detail}", file=sys.stderr)
        return 7

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

    if (incomplete or unresolved_targets) and not abort_reason:
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

    if incomplete or unresolved_targets:
        print(f"WATCH-ABORT incomplete-run arm={arm.name} "
              f"calls_without_output={incomplete} "
              f"unresolved_make_targets={unresolved_targets} "
              f"returns={state['returns']} "
              f"calls={calls} driver_rc={driver_rc}", file=sys.stderr)
        return 6

    print(f"watch: arm={arm.name} returns={state['returns']} calls={calls} "
          f"wall_s={wall_s} driver_rc={driver_rc} abort={abort_reason}")
    return 5 if abort_reason else 0


if __name__ == "__main__":
    sys.exit(main())
