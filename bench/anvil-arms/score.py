#!/usr/bin/env python3
"""score.py — A.7 + A.10 of docs/observations/2026-09-04-e3-e6-prestaged.md.

Reads one arm directory's `attest.json`, `rollout.jsonl`, `watch.jsonl`, `run.json`,
`diff.patch` and `gate.json`, and writes `receipt.json` + `receipt.md`.

Two rules govern every line of this file, and they are the scars this program has
already paid for:

  * COMPUTED COUNTS ONLY.  Nothing prints a verdict word over a missing number.  A
    missing or empty rollout is exit 3 with NO receipt written -- not a zero, not a
    "pass", not an empty table row.
  * TWO WITNESSES, AND THE DISAGREEMENT IS THE SIGNAL.  Returns and tool calls are
    re-derived independently from the raw rollout AND from the watcher, and any
    disagreement is recorded in `meter.sources` rather than silently resolved.

  * A RECEIPT IS EMITTED ONLY FROM A STREAM THAT VALIDATES.  Every non-empty line
    must be a well-formed JSON object; call ids must be unique and no output may
    precede its own call; the watcher's `ms_since_start` must be non-decreasing and
    its return/call ordinals dense from 1.  A stream that fails any of these is a
    typed abort with NO receipt -- and every abort DELETES any receipt the arm
    directory still holds, because a refusal that leaves the old answer standing
    is not a refusal.

  * A STREAM MUST SAY WHICH WATCHER WROTE IT.  `watch.jsonl` opens with a header
    record carrying `schema_version` and the bound rollout identity (st_dev, st_ino,
    session id).  A stream without both is `watch-schema-unsupported`: rc 3, no
    receipt.  A scorer cannot infer the instrument from the reading, and an old
    split-brain artifact is syntactically indistinguishable from a repaired one.

Exit codes: 0 scored; 2 no attestation or attest_ok=false; 3 missing/empty/invalid
rollout or watch, an unsupported watch schema, zero returns, or ANY watcher abort (no
receipt written, any stale one removed); 4 missing watch.jsonl.
"""
from __future__ import annotations

import argparse
import json
import pathlib
import re
import sys

# score.py imports watch.py from its own directory, which is INSIDE the repository
# being measured.  Without this, every direct run leaves bench/anvil-arms/__pycache__
# in the checkout and every later `git status` is ambiguous about what the experiment
# touched.  The self-test's PYTHONDONTWRITEBYTECODE covers the suite; this covers the
# reviewer who runs the scorer by hand.
sys.dont_write_bytecode = True
sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))
from watch import (  # noqa: E402  (same-directory module, deliberate)
    APPLY_PATCH_CMD_RE, COMMITTING_VERBS, WATCH_SCHEMA_VERSION, _shell_script,
    is_test_command, load_make_map, normalize, parse_utc, patch_targets,
)

READ_VERBS = {"inspect_clojure"}
WRITE_MARKERS = ("apply_patch", *sorted(COMMITTING_VERBS))
CLJ_FILE_RE = re.compile(r"[A-Za-z0-9_][A-Za-z0-9_/.\-]*\.cljc?\b")
TOOLCALLS_RE = re.compile(r"^\s*TOOLCALLS:\s*(\d+)\s*$", re.M)
LS_TREE_RE = re.compile(r'"mode"\s*:\s*"ls-tree"')
UNV = "unverified"


class StreamError(RuntimeError):
    """A stream that does not validate.  Never a note, never a skipped line."""


def read_jsonl_strict(path: pathlib.Path) -> list[dict]:
    """Every non-empty line of `path` must be a well-formed JSON object.

    The old reader `continue`d past anything it could not parse, so a rollout
    whose final record was cut in half scored rc 0 and produced a citeable
    receipt (Sol, item 1).  A line the reader cannot read is missing evidence,
    and missing evidence is an abort -- not a smaller number.
    """
    raw = path.read_bytes()
    if raw and not raw.endswith(b"\n"):
        raise StreamError(f"truncated-final-line {path.name} "
                          f"(no terminating newline; the last record is partial)")
    out: list[dict] = []
    for lineno, line in enumerate(raw.decode("utf-8", errors="replace").split("\n"), 1):
        line = line.strip()
        if not line:
            continue
        try:
            obj = json.loads(line)
        except Exception as exc:
            raise StreamError(f"malformed-line {path.name}:{lineno} ({exc})") from None
        if not isinstance(obj, dict):
            raise StreamError(f"non-object-line {path.name}:{lineno}")
        out.append(obj)
    return out


def validate_rollout(records: list[dict]) -> None:
    """Monotonic time, unique call ids, no output before its own call.

    Sol duplicated a rollout and reversed one; both produced receipts asserting
    `sources.agree=true`, because two witnesses derived from the SAME corrupted
    stream always agree.  Agreement is only evidence once the stream itself is.

    Structure is checked BEFORE ordering, in two passes, so a given corruption
    always reports the same reason: a duplicated stream is `duplicate-call-id`
    and a reversed one is `output-before-call`, whatever the driver's timestamp
    resolution happened to be that second.
    """
    # pass 1 -- call identity and correlation
    seen: set = set()
    open_ids: set = set()
    for lineno, obj in enumerate(records, 1):
        for event in normalize(obj):
            call_id = event.get("call_id")
            if call_id is None:
                continue
            if event["kind"] == "call":
                if call_id in seen:
                    raise StreamError(f"duplicate-call-id {call_id!r} rollout:{lineno}")
                seen.add(call_id)
                open_ids.add(call_id)
            elif event["kind"] == "call_output":
                if call_id not in seen:
                    raise StreamError(
                        f"output-before-call {call_id!r} rollout:{lineno}")
                if call_id not in open_ids:
                    raise StreamError(
                        f"duplicate-call-output {call_id!r} rollout:{lineno}")
                open_ids.discard(call_id)

    # pass 2 -- time never runs backwards
    prev_ts = None
    for lineno, obj in enumerate(records, 1):
        stamp = obj.get("timestamp") or obj.get("t")
        ts = parse_utc(stamp) if stamp else None
        if ts is None:
            continue
        if prev_ts is not None and ts < prev_ts:
            raise StreamError(f"out-of-order-timestamp rollout:{lineno} ({stamp})")
        prev_ts = ts


def validate_watch(records: list[dict]) -> None:
    """Monotonic `ms_since_start`, unique ordinals covering 1..N, one final `end`.

    The `end` is REQUIRED, and it must carry the driver's exit status and the run's
    wall: they appear nowhere else in the stream, so a stream without them describes a
    run whose ending nobody witnessed (Sol round two, item 4).

    `ms_since_start` is stamped inside `emit()`, so non-decreasing is a true
    invariant of a watch stream this apparatus wrote; a decreasing one has been
    reordered.  Return ordinals and call seqs are dense from 1, so a duplicated
    stream (Sol, item 1) collides instead of doubling the meter.
    """
    prev_ms = None
    returns: list[int] = []
    seqs: list[int] = []
    ended = False
    end_record: dict | None = None
    header_seen = False
    rollout_bound_seen = False
    for lineno, rec in enumerate(records, 1):
        if ended:
            raise StreamError(f"record-after-end watch:{lineno}")
        ms = rec.get("ms_since_start")
        if not isinstance(ms, int):
            raise StreamError(f"missing-ms_since_start watch:{lineno}")
        if prev_ms is not None and ms < prev_ms:
            raise StreamError(f"out-of-order-ms_since_start watch:{lineno} "
                              f"({ms} after {prev_ms})")
        prev_ms = ms
        kind = rec.get("kind")
        if kind == "return":
            n = rec.get("n")
            if not isinstance(n, int):
                raise StreamError(f"return-without-ordinal watch:{lineno}")
            returns.append(n)
        elif kind == "call":
            seq = rec.get("seq")
            if not isinstance(seq, int):
                raise StreamError(f"call-without-seq watch:{lineno}")
            seqs.append(seq)
        elif kind == "end":
            ended = True
            end_record = rec
        elif kind == "header":
            # Sol round four: `header` was permitted anywhere in the stream with no
            # count limit, so a second, CONTRADICTORY header (a different schema,
            # session or inode than record 1's) scored rc 0 and wrote a receipt.
            # Genuine late binding is announced through `rollout-bound`, never a
            # second `header` -- exactly one header opens a stream, at record zero.
            if lineno != 1 or header_seen:
                raise StreamError(
                    f"duplicate-header watch:{lineno} (a header record belongs "
                    f"only at record 1, once; a stream with a second or misplaced "
                    f"header is not the record of one metered run)")
            header_seen = True
        elif kind == "rollout-bound":
            # at most one late-binding announcement per stream, for the same reason:
            # a second one is a contradiction, not a correction.
            if rollout_bound_seen:
                raise StreamError(
                    f"duplicate-header watch:{lineno} (a second rollout-bound "
                    f"record contradicts the first; at most one is permitted per "
                    f"stream)")
            rollout_bound_seen = True
        elif kind != "abort":
            raise StreamError(f"unknown-record-kind watch:{lineno} kind={kind!r}")
    if sorted(returns) != list(range(1, len(returns) + 1)):
        raise StreamError(f"return-ordinals-not-dense-from-1 "
                          f"(n={len(returns)}, distinct={len(set(returns))})")
    if sorted(seqs) != list(range(1, len(seqs) + 1)):
        raise StreamError(f"call-seqs-not-dense-from-1 "
                          f"(n={len(seqs)}, distinct={len(set(seqs))})")

    # THE `end` RECORD IS THE COMPLETION STAMP.  Sol round two, item 4: this function
    # promised "one final `end`" in its own docstring and never required one, so a
    # stream with the record deleted scored rc 0.  driver rc and wall live nowhere else
    # in the stream; without them the receipt's wall_s is a number about a moment the
    # meter never observed, which is the hand-typed-timestamp defect wearing a schema.
    if not ended or end_record is None:
        raise StreamError("watch-unterminated (no final `end` record: the run's "
                          "completion stamp, driver rc and wall are missing)")
    if not isinstance(end_record.get("driver_rc"), int):
        raise StreamError(f"watch-unterminated (the final `end` carries no driver rc: "
                          f"driver_rc={end_record.get('driver_rc')!r})")
    if not isinstance(end_record.get("wall_s"), (int, float)):
        raise StreamError(f"watch-unterminated (the final `end` carries no wall: "
                          f"wall_s={end_record.get('wall_s')!r})")


# EXACT match, not a floor: Sol round five, item 3.  A header naming a schema this
# scorer has never seen -- even a NEWER one -- used to pass as long as the number was
# >= 2, so an unknown future stream shape scored rc 0 and wrote a receipt.  Fail
# closed on the exact contract this scorer reads, the same evidence-format posture as
# the missing-provenance and no-header refusals right above it.
# Sol round six, item 8: ONE literal.  The scorer reads exactly the schema the
# watcher in the same checkout writes; a second independent `2` here was a drift path.
WATCH_SCHEMA_SUPPORTED = WATCH_SCHEMA_VERSION
PROVENANCE_KEYS = ("rollout_dev", "rollout_ino", "session_id")


def watch_provenance(records: list[dict]) -> dict:
    """The schema and the bound rollout identity this stream was written under.

    Sol round three, finding (e).  The watcher was repaired to bind its rollout by
    inode and to abort on rotation; Sol then copied a round-TWO split-brain artifact,
    written BEFORE that repair, into the repaired scorer.  It returned rc 0 and a
    receipt reading `sources.agree=true` over evidence that is two different files.
    Every syntactic check passed, because the old stream is syntactically perfect --
    what it lacks is any statement about WHICH watcher produced it.

    A scorer cannot infer the instrument from the reading.  So the stream carries its
    own provenance in its first record, and anything without it is refused rather than
    assumed to be current.  Raises StreamError, whose text begins
    `watch-schema-unsupported`, and no receipt is ever written from such a stream.
    """
    header = records[0] if records else {}
    if header.get("kind") != "header":
        raise StreamError(
            "watch-schema-unsupported (this stream has no header record, so nothing "
            "in it says which watcher wrote it; a stream written before the "
            "inode-binding repair is syntactically identical to one written after it)")
    version = header.get("schema_version")
    if not isinstance(version, int) or version != WATCH_SCHEMA_SUPPORTED:
        raise StreamError(
            f"watch-schema-unsupported (schema_version={version!r}; this scorer reads "
            f"schema_version == {WATCH_SCHEMA_SUPPORTED} only)")
    missing = [key for key in PROVENANCE_KEYS if key not in header]
    if missing:
        raise StreamError(
            f"watch-schema-unsupported (the header carries no inode-binding "
            f"provenance: missing {missing})")
    identity = {key: header.get(key) for key in PROVENANCE_KEYS}
    for rec in records:
        if rec.get("kind") == "rollout-bound":
            # the rollout was discovered after the driver announced its session; the
            # LAST binding record is the one the run was actually metered against
            identity.update({k: rec.get(k) for k in PROVENANCE_KEYS if k in rec})
    identity["schema_version"] = version
    return identity


def abort(arm: pathlib.Path, code: int, reason: str) -> int:
    """Print a typed abort AND remove any receipt this arm directory still holds.

    Sol, item 2: an rc-3 abort left the previous run's receipt.json in place, so
    the directory kept citing a receipt the current evidence cannot support.  A
    refusal that leaves the old answer standing is not a refusal.
    """
    removed = []
    for name in ("receipt.json", "receipt.md"):
        path = arm / name
        if path.exists():
            path.unlink()
            removed.append(name)
    tail = f" (removed stale {', '.join(removed)})" if removed else " (no receipt written)"
    print(f"SCORE-ABORT {reason}{tail}", file=sys.stderr)
    return code


def rollout_meters(rollout: list[dict], make_map: dict | None = None) -> dict:
    """Predicates 1-3, re-derived from the raw rollout, independent of the watcher.

    It reads the SAME attest-time make map the watcher used, so the two witnesses
    differ only where the streams differ -- never because one of them could see
    through `make verify` and the other could not.
    """
    returns = calls = test_calls = 0
    for obj in rollout:
        for event in normalize(obj):
            if event["kind"] == "return":
                returns += 1
            elif event["kind"] == "call":
                calls += 1
                script = _shell_script(event.get("args") or "")
                if script and is_test_command(script, make_map)[0]:
                    test_calls += 1
    return {"returns": returns, "total_actions": calls, "test_actions": test_calls,
            "non_test_actions": calls - test_calls}


def score(arm: pathlib.Path, args) -> int:
    attest_path = arm / "attest.json"
    if not attest_path.exists():
        return abort(arm, 2, f"no-attestation {attest_path}")
    attest = json.loads(attest_path.read_text())
    if not attest.get("attest_ok"):
        return abort(arm, 2, f"attest-not-ok {attest_path} "
                             f"refusals={attest.get('refusals')}")

    rollout_path = arm / "rollout.jsonl"
    if not rollout_path.exists():
        return abort(arm, 3, f"missing-rollout {rollout_path}")
    if rollout_path.stat().st_size == 0:
        return abort(arm, 3, f"empty-rollout {rollout_path}")
    try:
        rollout = read_jsonl_strict(rollout_path)
        validate_rollout(rollout)
    except StreamError as exc:
        return abort(arm, 3, f"malformed-rollout {exc}")
    if not rollout:
        return abort(arm, 3, f"unparseable-rollout {rollout_path}")

    make_map = load_make_map(arm / "make-targets.json")
    raw = rollout_meters(rollout, make_map)
    if raw["returns"] == 0:
        return abort(arm, 3, f"zero-returns {rollout_path}")

    watch_path = arm / "watch.jsonl"
    if not watch_path.exists():
        return abort(arm, 4, f"missing-watch {watch_path}")
    if watch_path.stat().st_size == 0:
        return abort(arm, 3, f"empty-watch {watch_path}")
    try:
        watch = read_jsonl_strict(watch_path)
        validate_watch(watch)
    except StreamError as exc:
        return abort(arm, 3, f"malformed-watch {exc}")
    try:
        rollout_identity = watch_provenance(watch)
    except StreamError as exc:
        return abort(arm, 3, f"{exc} {watch_path}")
    if not watch:
        return abort(arm, 3, f"empty-watch {watch_path}")
    wcalls = [w for w in watch if w.get("kind") == "call"]
    wreturns = [w for w in watch if w.get("kind") == "return"]
    aborts = [w for w in watch if w.get("kind") == "abort"]
    no_output = [w for w in wcalls if w.get("outcome") == "no-output"]
    # A `make` target the attest-time map could not resolve: what it RAN is unknown, so
    # the run is incomplete rather than one more non-test action (Sol round two, item 6).
    unresolved_make = sorted({tgt for c in wcalls
                              for tgt in (c.get("make_unresolved") or [])})

    run = {}
    run_path = arm / "run.json"
    if run_path.exists():
        run = json.loads(run_path.read_text())

    if (no_output or unresolved_make
            or any(a.get("error_type") == "incomplete-run" for a in aborts)):
        return abort(arm, 3, f"incomplete-run {watch_path} "
                             f"({len(no_output)} tool call(s) whose result never "
                             f"arrived; seq="
                             f"{[c.get('seq') for c in no_output]}; "
                             f"unresolved make target(s)={unresolved_make})")

    # A WATCHER ABORT IS TERMINAL.  Sol round two, item 3: an abort was appended to
    # `notes` and the run was scored anyway, so an idle-stop (watcher rc 5) became a
    # receipt asserting counts nobody was still metering when the run stopped.  The
    # meter gave up; there is no number here to print.  run.json keeps the abort as
    # the terminal fact about this arm, and the receipt is refused -- and any stale
    # one deleted, because a refusal that leaves the old answer standing is not one.
    stopped = [str(a.get("error_type") or "untyped") for a in aborts]
    if run.get("abort") and str(run["abort"]) not in stopped:
        stopped.append(str(run["abort"]))
    if stopped:
        return abort(arm, 3, f"watch-abort:{','.join(stopped)} {watch_path} "
                             f"(the watcher stopped this run; run.json carries it as "
                             f"the terminal fact and no receipt is written)")
    if rollout_identity["rollout_dev"] is None or rollout_identity["rollout_ino"] is None:
        # The run completed with no abort, so the watcher claims it metered something --
        # but it never bound an inode, which is the only thing that makes "the bytes I
        # metered" and "the bytes retained" the same file.
        return abort(arm, 3, f"watch-schema-unsupported (the run reports no abort and "
                             f"the stream never bound a rollout inode: "
                             f"{ {k: rollout_identity[k] for k in PROVENANCE_KEYS} }) "
                             f"{watch_path}")

    if not wreturns:
        return abort(arm, 3, f"zero-metered-returns {watch_path} "
                             f"(the rollout shows {raw['returns']} returns; the meter shows none)")

    notes: list[str] = []

    # ---- meters ------------------------------------------------------------
    metered = {
        "returns": len(wreturns),
        "total_actions": len(wcalls),
        "test_actions": sum(1 for c in wcalls if c.get("test_call")),
    }
    metered["non_test_actions"] = metered["total_actions"] - metered["test_actions"]
    for key in ("returns", "total_actions", "test_actions", "non_test_actions"):
        if raw[key] != metered[key]:
            notes.append(f"meter-disagreement:{key} rollout={raw[key]} watch={metered[key]}")

    elapsed = [c.get("elapsed_ms") for c in wcalls if isinstance(c.get("elapsed_ms"), int)]
    test_elapsed = [c.get("elapsed_ms") for c in wcalls
                    if c.get("test_call") and isinstance(c.get("elapsed_ms"), int)]

    self_reported = None
    report_text = ""
    for candidate in ("driver-report.md", "driver.log"):
        path = arm / candidate
        if path.exists():
            report_text += path.read_text(errors="replace")
    match = None
    for match in TOOLCALLS_RE.finditer(report_text):
        pass
    if match:
        self_reported = int(match.group(1))

    wall_s = run.get("wall_s")
    if wall_s is None:
        notes.append("wall-unverified: no run.json completion stamp")

    # Sol round four, item 3 (watch.py:1017): the meter's OWN cost -- Sol measured
    # 2.52 watcher CPU seconds over a 61.479s run, about 4.1% of one core, at the
    # 250ms scan interval -- never appeared in the receipt, so a cohort run had no
    # way to see what running the meter itself was costing the box. run.json now
    # carries the three raw numbers (RUSAGE_SELF delta, the scan count, the interval
    # actually used); this is a pass-through, not a recomputation, because only the
    # watcher process itself can measure its own CPU time.
    watcher_cpu_s = run.get("watcher_cpu_s")
    scans = run.get("scans")
    scan_interval_ms = run.get("scan_interval_ms")
    if watcher_cpu_s is None or scans is None or scan_interval_ms is None:
        notes.append("watcher-cost-unverified: run.json carries no "
                     "watcher_cpu_s/scans/scan_interval_ms")

    meter = {
        "wall_s": wall_s if wall_s is not None else UNV,
        "returns": metered["returns"],
        "total_actions": metered["total_actions"],
        "test_actions": metered["test_actions"],
        "non_test_actions": metered["non_test_actions"],
        "in_run_test_s": round(sum(test_elapsed) / 1000.0, 1) if test_elapsed else 0.0,
        "tool_exec_s": round(sum(elapsed) / 1000.0, 1) if elapsed else 0.0,
        "self_reported_toolcalls": self_reported if self_reported is not None else UNV,
        "watcher_cpu_s": watcher_cpu_s if watcher_cpu_s is not None else UNV,
        "scans": scans if scans is not None else UNV,
        "scan_interval_ms": scan_interval_ms if scan_interval_ms is not None else UNV,
        "sources": {"rollout": raw, "watch": metered,
                    "agree": raw == {k: metered[k] for k in raw}},
    }
    if wall_s and meter["tool_exec_s"]:
        meter["tool_exec_pct_of_wall"] = round(100.0 * meter["tool_exec_s"] / wall_s, 1)

    # ---- verbs, writes, fallback ------------------------------------------
    verbs: dict[str, int] = {}
    for call in wcalls:
        verbs[call.get("tool") or "unknown"] = verbs.get(call.get("tool") or "unknown", 0) + 1

    via_verb = [c for c in wcalls if c.get("verb")]
    via_verb_committed = [c for c in via_verb if c.get("outcome") == "ok"]

    # Predicate 6.  The watcher already extracted each apply_patch payload's targets
    # from the FULL arguments; score.py only holds a truncated copy, so it trusts the
    # watcher's fields when present and re-derives from the truncated text only for a
    # watch.jsonl written by an older watcher.
    apply_patch_calls = 0
    apply_patch_clj = 0
    apply_patch_clj_files: set[str] = set()
    for call in wcalls:
        text = call.get("args") or ""
        head = call.get("cmd_head") or ""
        tool = (call.get("tool") or "").split("__")[-1]
        if "apply_patch" in call:
            looks_like_patch = bool(call["apply_patch"])
            targets = call.get("patch_clj_files") or []
        else:
            looks_like_patch = (
                tool == "apply_patch"
                or "*** Begin Patch" in text
                or APPLY_PATCH_CMD_RE.search(head) is not None
            )
            targets = [f for f in patch_targets(_shell_script(text) or "", text)
                       if f.endswith((".clj", ".cljc"))]
        if not looks_like_patch:
            continue
        apply_patch_calls += 1
        if targets:
            apply_patch_clj += 1
            apply_patch_clj_files.update(targets)

    diff_path = arm / "diff.patch"
    if diff_path.exists():
        diff_text = diff_path.read_text(errors="replace")
        insertions = sum(1 for ln in diff_text.split("\n")
                         if ln.startswith("+") and not ln.startswith("+++"))
        deletions = sum(1 for ln in diff_text.split("\n")
                        if ln.startswith("-") and not ln.startswith("---"))
        diff_clj = sorted({f for f in patch_targets(diff_text)
                           if f.endswith((".clj", ".cljc"))})
        churn = {"insertions": insertions, "deletions": deletions,
                 "status": "computed", "clj_files_touched": len(diff_clj)}
    else:
        churn = {"insertions": UNV, "deletions": UNV, "status": UNV,
                 "clj_files_touched": UNV}
        notes.append("churn-unverified: diff.patch missing (see DIFF-FAILED in driver.log)")

    band = None
    within = None
    if args.churn_band:
        try:
            lo_i, hi_i, lo_d, hi_d = [int(x) for x in args.churn_band.split(",")]
            band = [lo_i, hi_i, lo_d, hi_d]
            if churn["status"] == "computed":
                within = (lo_i <= churn["insertions"] <= hi_i
                          and lo_d <= churn["deletions"] <= hi_d)
        except ValueError:
            notes.append(f"churn-band-unparseable:{args.churn_band}")
    churn["band"] = band
    churn["within_band"] = within if within is not None else UNV

    # ---- refusal ledger (A.6) ---------------------------------------------
    refusals = []
    errored = [c for c in wcalls if c.get("outcome") not in (None, "ok")]
    for i, call in enumerate(errored, start=1):
        seq = call.get("seq")
        later_ok_same_tool = any(
            c.get("tool") == call.get("tool") and c.get("outcome") == "ok"
            and (c.get("seq") or 0) > (seq or 0) for c in wcalls)
        next_write = next(
            (c for c in wcalls
             if (c.get("seq") or 0) > (seq or 0) and c.get("verb")
             and c.get("outcome") == "ok"), None)
        refusals.append({
            "n": i,
            "seq": seq,
            "t_offset_s": round((call.get("ms_since_start") or 0) / 1000.0, 1),
            "verb": call.get("tool"),
            "error_type": call.get("error_type") or UNV,
            "class": UNV,                       # a human judgement (A.6), not computed
            "next_call_present": "next_call" in (call.get("output_head") or ""),
            "next_call_sent_verbatim": UNV,     # a human judgement (A.6)
            "returns_to_recover": ((next_write.get("n") or 0) - (call.get("n") or 0))
                                  if next_write else UNV,
            "agent_visible": UNV,               # a human judgement (A.6)
            "abandoned_route": not later_ok_same_tool,
            "outcome": "recovered" if later_ok_same_tool else "abandoned",
        })

    # ---- E6 observables ----------------------------------------------------
    ls_tree = [c for c in wcalls
               if (c.get("tool") or "").split("__")[-1] in READ_VERBS
               and LS_TREE_RE.search(c.get("args") or "")]
    adoption = {
        "ls_tree_calls": len(ls_tree),
        "first_ls_tree_return": ls_tree[0].get("n") if ls_tree else None,
        "early": bool(ls_tree) and (ls_tree[0].get("n") or 99) <= 3,
    }

    files_before_write: set[str] = set()
    for call in sorted(wcalls, key=lambda c: c.get("seq") or 0):
        blob = (call.get("args") or "") + " " + (call.get("cmd_head") or "")
        if call.get("verb") or any(m in blob for m in WRITE_MARKERS):
            break
        files_before_write.update(CLJ_FILE_RE.findall(blob))
    reads = {"clj_files_before_first_write": len(files_before_write),
             "files": sorted(files_before_write)[:50]}

    # ---- gate --------------------------------------------------------------
    gate_path = arm / "gate.json"
    if gate_path.exists():
        gate = json.loads(gate_path.read_text())
        for key in ("name", "green", "detail"):
            gate.setdefault(key, UNV)
    else:
        gate = {"name": UNV, "green": UNV, "detail": UNV}
        notes.append("gate-unverified: no gate.json in the arm directory")

    if attest.get("server_sha") == UNV or attest.get("port_pid") == UNV:
        if attest.get("arm") != "N":
            notes.append("receipt-unverified: attestation carries unverified server identity")
    if run.get("driver_rc") not in (0, None):
        notes.append(f"driver-rc:{run.get('driver_rc')}")

    receipt = {
        "exp": attest.get("exp"), "rung": attest.get("rung"),
        "arm": attest.get("arm"), "slot": attest.get("slot"),
        "driver": attest.get("driver"), "model": attest.get("model"),
        "attest": {
            "start_utc": attest.get("start_utc"),
            "end_utc": run.get("end_utc", UNV),
            "worktree": attest.get("worktree"),
            "worktree_head": attest.get("worktree_head"),
            "base": attest.get("base"),
            "prompt_path": attest.get("prompt_path"),
            "prompt_sha256": attest.get("prompt_sha256"),
            "runner_sha256": attest.get("runner_sha256"),
            "make_targets_sha256": attest.get("make_targets_sha256"),
            "watch_sha256": attest.get("watch_sha256"),
            "score_sha256": attest.get("score_sha256"),
            "mcp_url": attest.get("mcp_url"),
            "mcp_port": attest.get("mcp_port"),
            "expected_server_sha": attest.get("expected_server_sha"),
            "server_sha": attest.get("server_sha"),
            "server_cwd": attest.get("server_cwd"),
            "server_project_head": attest.get("server_project_head"),
            "healthz": attest.get("healthz"),
            "port_pid": attest.get("port_pid"),
            "mcp_absent_proof": attest.get("mcp_absent_proof"),
            "attest_ok": attest.get("attest_ok"),
        },
        "meter": meter,
        "verbs": verbs,
        "writes": {
            "via_verb": len(via_verb),
            "via_verb_committed": len(via_verb_committed),
            "via_verb_by_name": {
                v: sum(1 for c in via_verb_committed if c.get("verb") == v)
                for v in sorted({c.get("verb") for c in via_verb_committed})
            },
            "native_apply_patch_calls": apply_patch_calls,
            "native_apply_patch_clj": apply_patch_clj,
            "native_apply_patch_clj_files": sorted(apply_patch_clj_files),
        },
        "churn": churn,
        "refusals": refusals,
        "refusal_rate": (round(len(refusals) / metered["total_actions"], 3)
                         if metered["total_actions"] else UNV),
        "adoption": adoption,
        "reads": reads,
        "gate": gate,
        "notes": notes,
    }

    (arm / "receipt.json").write_text(json.dumps(receipt, indent=2) + "\n")
    (arm / "receipt.md").write_text(receipt_md(receipt))
    print(json.dumps({
        "arm": arm.name,
        "returns": meter["returns"],
        "total_actions": meter["total_actions"],
        "non_test_actions": meter["non_test_actions"],
        "wall_s": meter["wall_s"],
        "via_verb": receipt["writes"]["via_verb"],
        "native_apply_patch_clj": apply_patch_clj,
        "churn": [churn["insertions"], churn["deletions"]],
        "refusals": len(refusals),
        "gate_green": gate["green"],
        "notes": len(notes),
    }))
    return 0


def receipt_md(r: dict) -> str:
    m, w, c = r["meter"], r["writes"], r["churn"]
    head = ("| exp | rung | arm | slot | wall s | returns | non-test actions | "
            "write calls via verb | native .clj patches | churn +/- | refusals | gate |\n"
            "|---|---|---|---|---|---|---|---|---|---|---|---|\n")
    row = (f"| {r['exp']} | {r['rung']} | {r['arm']} | {r['slot']} | {m['wall_s']} | "
           f"{m['returns']} | {m['non_test_actions']} | {w['via_verb']} | "
           f"{w['native_apply_patch_clj']} | +{c['insertions']}/-{c['deletions']} | "
           f"{len(r['refusals'])} | {r['gate']['green']} |\n")
    tail = f"\nattest_ok={r['attest']['attest_ok']} server_sha={r['attest']['server_sha']} " \
           f"prompt_sha256={r['attest']['prompt_sha256']}\n"
    if r["notes"]:
        tail += "\nnotes:\n" + "".join(f"- {n}\n" for n in r["notes"])
    return head + row + tail


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("arm")
    ap.add_argument("--churn-band", default=None,
                    help="lo_ins,hi_ins,lo_del,hi_del, e.g. 47,71,27,41")
    args = ap.parse_args()
    return score(pathlib.Path(args.arm).resolve(), args)


if __name__ == "__main__":
    sys.exit(main())
