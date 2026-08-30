#!/usr/bin/env python3
"""Independent native-write/error-loop audit over one bounded Codex receipt."""

from __future__ import annotations

import argparse
from collections import Counter, defaultdict
from datetime import datetime
import hashlib
import importlib.util
import json
from pathlib import Path
import re


READER_PATTERNS = {
    "syntax-reading": re.compile(r"Syntax error reading source", re.I),
    "reader-exception": re.compile(r"ReaderException", re.I),
    "eof-reading": re.compile(r"EOF while reading|EOF while reading string", re.I),
    "unmatched-delimiter": re.compile(r"Unmatched delimiter", re.I),
    "invalid-token": re.compile(r"Invalid token", re.I),
    "bad-escape": re.compile(r"Unsupported escape|Invalid unicode escape", re.I),
    "odd-map": re.compile(r"Map literal must contain an even number", re.I),
    "duplicate-key": re.compile(r"Duplicate key", re.I),
}

COMPILE_PATTERNS = {
    "syntax-compiling": re.compile(r"Syntax error compiling", re.I),
    "syntax-macroexpanding": re.compile(r"Syntax error macroexpanding", re.I),
    "unable-resolve": re.compile(r"Unable to resolve symbol", re.I),
    "no-namespace": re.compile(r"No such namespace", re.I),
    "class-not-found": re.compile(r"ClassNotFoundException", re.I),
}

INTENTIONAL_MARKERS = re.compile(
    r"malformed|invalid.intent|invalid source|reader error|syntax error|"
    r"red test|falsif|refus|rollback|source.unchanged|negative witness",
    re.I,
)

ERROR_LOCATION = re.compile(
    r"(?:Syntax error [^\n]*? at|Location:)\s+\(?([^:()\n]+):(\d+):(\d+)\)?",
    re.I,
)

# Human-reviewed against only the receipt-named write/error/rewrite windows.
# These labels are evidence annotations, not inferred from task vocabulary.
MANUAL_LOOP_CLASSIFICATIONS = {
    ("216732d7abe4", 1926, 1934, 1944):
        ("genuine-accidental", "causal", False, "missing namespace require"),
    ("216732d7abe4", 1993, 2076, 2157):
        ("genuine-accidental", "causal", False, "classpath and test wiring"),
    ("216732d7abe4", 8111, 8160, 8166):
        ("intentional-experiment", "causal-expected-red", False, "absent product seam"),
    ("216732d7abe4", 9380, 9388, 9392):
        ("genuine-accidental", "causal", False, "wrong library function"),
    ("216732d7abe4", 9404, 9412, 9416):
        ("genuine-accidental", "causal", False, "missing imported class"),
    ("216732d7abe4", 9861, 9869, 9885):
        ("intentional-experiment", "causal-expected-red", False, "tests preceded implementation"),
    ("216732d7abe4", 12120, 12128, 12132):
        ("genuine-accidental", "causal", False, "forward reference"),
    ("216732d7abe4", 19100, 19108, 19112):
        ("genuine-accidental", "causal", False, "missing helper"),
    ("216732d7abe4", 21788, 21802, 21840):
        ("undecidable", "non-causal", False, "test invocation named an absent var"),
    ("52b31dc01678", 1606, 1620, 1624):
        ("genuine-accidental", "causal", False, "forward declaration"),
    ("c54a7c4283f9", 182, 210, 220):
        ("genuine-accidental", "causal", False, "stale symbol reference"),
    ("c54a7c4283f9", 647, 667, 682):
        ("undecidable", "non-causal", False, "malformed REPL expression"),
    ("471c92d91531", 363, 389, 436):
        ("undecidable", "non-causal", False, "next write did not repair reported error"),
    ("471c92d91531", 571, 579, 585):
        ("intentional-experiment", "causal-expected-red", False, "explicit red witness"),
}


def load_base(path: Path):
    spec = importlib.util.spec_from_file_location("adoption_census_base", path)
    module = importlib.util.module_from_spec(spec)
    assert spec.loader
    spec.loader.exec_module(module)
    return module


def short_hash(value: str) -> str:
    return hashlib.sha256(value.encode()).hexdigest()[:12]


def classify_error(text: str):
    reader = sorted(name for name, pattern in READER_PATTERNS.items() if pattern.search(text))
    compile_errors = sorted(
        name for name, pattern in COMPILE_PATTERNS.items() if pattern.search(text)
    )
    if reader:
        return "reader-catchable", reader
    if compile_errors:
        return "compile-not-reader", compile_errors
    return None, []


def error_location(text: str):
    match = ERROR_LOCATION.search(text)
    return match.group(1) if match else None


def signature_occurrences(text: str) -> int:
    return sum(
        len(pattern.findall(text))
        for pattern in list(READER_PATTERNS.values()) + list(COMPILE_PATTERNS.values())
    )


def target_risks(patch: str):
    return {
        "reader-conditionals": "#?" in patch,
        "possible-tagged-literals": bool(
            re.search(r"#[A-Za-z][A-Za-z0-9_.-]*/[A-Za-z][A-Za-z0-9_.-]*", patch)
        ),
        "cljc-target": ".cljc" in patch,
    }


def parse_session(base, session, sessions_root: Path, since: datetime, until: datetime):
    path = base.evidence_path(sessions_root, session["evidence_file"])
    events = []
    meta = None
    with path.open() as handle:
        for ordinal, line in enumerate(handle, 1):
            event = json.loads(line)
            if event.get("type") == "session_meta" and meta is None:
                meta = event.get("payload", {})
            timestamp_text = event.get("timestamp")
            if not timestamp_text:
                continue
            timestamp = base.parse_instant(timestamp_text)
            if since <= timestamp < until and event.get("type") == "response_item":
                events.append((ordinal, timestamp, event.get("payload", {})))
    if not meta or not meta.get("timestamp"):
        return None
    started = base.parse_instant(meta["timestamp"])
    if not since <= started < until:
        return None

    cwd = meta.get("cwd") or ""
    calls = {}
    outputs = {}
    user_context = ""
    for ordinal, timestamp, payload in events:
        payload_type = payload.get("type")
        if payload_type == "message" and payload.get("role") == "user":
            content = payload.get("content") or []
            user_context = " ".join(
                item.get("text", "") for item in content if isinstance(item, dict)
            )[-4000:]
        elif payload_type == "custom_tool_call":
            calls[payload.get("call_id")] = {
                "ordinal": ordinal,
                "timestamp": timestamp,
                "source": payload.get("input") or "",
                "name": payload.get("name") or "",
                "user_context": user_context,
            }
        elif payload_type == "custom_tool_call_output":
            outputs[payload.get("call_id")] = {
                "ordinal": ordinal,
                "timestamp": timestamp,
                "text": base.output_text(payload),
            }

    writes = []
    error_outputs = []
    for call_id, call in calls.items():
        source = call["source"]
        executable = base.strip_js_literals(source)
        if base.PATCH_CALL.search(executable):
            patch = base.extract_patch(source)
            files = base.parse_patch_files(patch, cwd) if patch else []
            clojure_updates = [
                entry
                for entry in files
                if entry["operation"] == "update"
                and Path(entry["target"]).suffix in base.CLOJURE_EXTENSIONS
            ]
            output = outputs.get(call_id, {})
            output_text = output.get("text", "")
            success = "Script failed" not in output_text and "Script error:" not in output_text
            if clojure_updates:
                writes.append(
                    {
                        "call_id": call_id,
                        "ordinal": call["ordinal"],
                        "timestamp": call["timestamp"],
                        "success": success,
                        "targets": {entry["target"] for entry in clojure_updates},
                        "target_hashes": sorted(short_hash(entry["target"]) for entry in clojure_updates),
                        "intentional_context": bool(INTENTIONAL_MARKERS.search(call["user_context"])),
                        "risks": target_risks(patch or ""),
                    }
                )

        output = outputs.get(call_id)
        if output:
            error_class, signatures = classify_error(output["text"])
            if error_class:
                location = error_location(output["text"])
                error_outputs.append(
                    {
                        "call_id": call_id,
                        "ordinal": output["ordinal"],
                        "timestamp": output["timestamp"],
                        "error_class": error_class,
                        "signatures": signatures,
                        "signature_occurrences": signature_occurrences(output["text"]),
                        "location_kind": (
                            "repl"
                            if location == "REPL"
                            else "source-file"
                            if location and Path(location).suffix in base.CLOJURE_EXTENSIONS
                            else "unknown"
                        ),
                        "intentional_context": bool(INTENTIONAL_MARKERS.search(call["user_context"])),
                    }
                )

    writes.sort(key=lambda row: (row["timestamp"], row["ordinal"]))
    error_outputs.sort(key=lambda row: (row["timestamp"], row["ordinal"]))
    return {
        "session_key": session["session_key"],
        "writes": writes,
        "errors": error_outputs,
    }


def build_loops(session, seconds: int):
    successful = [row for row in session["writes"] if row["success"]]
    loops = []
    for error in session["errors"]:
        prior = [
            row
            for row in successful
            if row["timestamp"] < error["timestamp"]
            and (error["timestamp"] - row["timestamp"]).total_seconds() <= seconds
        ]
        if not prior:
            continue
        write = prior[-1]
        rewrites = [
            row
            for row in successful
            if error["timestamp"] < row["timestamp"]
            and (row["timestamp"] - error["timestamp"]).total_seconds() <= seconds
            and row["targets"].intersection(write["targets"])
        ]
        if not rewrites:
            continue
        rewrite = rewrites[0]
        loops.append(
            {
                "session_key": session["session_key"],
                "write_ordinal": write["ordinal"],
                "error_ordinal": error["ordinal"],
                "rewrite_ordinal": rewrite["ordinal"],
                "write_to_error_seconds": round(
                    (error["timestamp"] - write["timestamp"]).total_seconds(), 3
                ),
                "error_to_rewrite_seconds": round(
                    (rewrite["timestamp"] - error["timestamp"]).total_seconds(), 3
                ),
                "error_class": error["error_class"],
                "signatures": error["signatures"],
                "target_hashes": sorted(
                    short_hash(target)
                    for target in write["targets"].intersection(rewrite["targets"])
                ),
                "intentional_marker": write["intentional_context"] or error["intentional_context"],
            }
        )
    deduped = {}
    for row in loops:
        key = (row["session_key"], row["write_ordinal"], row["rewrite_ordinal"])
        deduped.setdefault(key, row)
    return list(deduped.values())


def annotate_loop(row):
    key = (
        row["session_key"],
        row["write_ordinal"],
        row["error_ordinal"],
        row["rewrite_ordinal"],
    )
    annotation = MANUAL_LOOP_CLASSIFICATIONS.get(key)
    if not annotation:
        return row
    intent_class, causal_relation, parse_catchable, reason = annotation
    return {
        **row,
        "manual_intent_class": intent_class,
        "manual_causal_relation": causal_relation,
        "post_state_reader_parse_catchable": parse_catchable,
        "manual_reason": reason,
    }


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("receipt", type=Path)
    parser.add_argument("--sessions-root", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()

    base = load_base(Path(__file__).with_name("adoption_census_independent.py"))
    receipt = json.loads(args.receipt.read_text())
    since = base.parse_instant(receipt["window"]["since"])
    until = base.parse_instant(receipt["window"]["until"])
    sessions = [
        row
        for session in receipt["providers"]["codex"]["sessions"]
        if (row := parse_session(base, session, args.sessions_root, since, until))
    ]
    writing_sessions = [row for row in sessions if any(w["success"] for w in row["writes"])]
    writes = [w for session in writing_sessions for w in session["writes"] if w["success"]]
    errors = [e for session in writing_sessions for e in session["errors"]]

    loop_sets = {str(seconds): sum((build_loops(session, seconds) for session in writing_sessions), [])
                 for seconds in (60, 300, 900)}
    loop_sets["300"] = [annotate_loop(row) for row in loop_sets["300"]]
    manual_counts = Counter(
        row.get("manual_intent_class", "unreviewed") for row in loop_sets["300"]
    )
    risk_counts = Counter(
        risk
        for write in writes
        for risk, present in write["risks"].items()
        if present
    )
    target_update_counts = Counter(
        target for write in writes for target in write["targets"]
    )
    ranked_target_counts = sorted(target_update_counts.values(), reverse=True)
    report = {
        "schema": "clj-surgeon.native-prelanding-gate-audit/v1",
        "window": receipt["window"],
        "receipt_sha256": hashlib.sha256(args.receipt.read_bytes()).hexdigest(),
        "session_start_population": len(sessions),
        "clojure_writing_sessions": len(writing_sessions),
        "clojure_writing_sessions_with_reader_or_compile_errors": sum(
            1 for row in writing_sessions if row["errors"]
        ),
        "clojure_writing_sessions_with_300_second_loops": sum(
            1 for row in writing_sessions if build_loops(row, 300)
        ),
        "successful_existing_clojure_writes": len(writes),
        "clojure_file_update_occurrences": sum(ranked_target_counts),
        "distinct_clojure_targets": len(ranked_target_counts),
        "top_13_target_update_occurrences": sum(ranked_target_counts[:13]),
        "maximum_updates_to_one_target": max(ranked_target_counts, default=0),
        "reader_or_compile_error_outputs_in_writing_sessions": len(errors),
        "reader_or_compile_signature_occurrences": sum(
            row["signature_occurrences"] for row in errors
        ),
        "error_classes": dict(sorted(Counter(e["error_class"] for e in errors).items())),
        "error_event_inventory": [
            {
                "session_key": row["session_key"],
                "ordinal": error["ordinal"],
                "error_class": error["error_class"],
                "signatures": error["signatures"],
                "signature_occurrences": error["signature_occurrences"],
                "location_kind": error["location_kind"],
                "intentional_marker": error["intentional_context"],
            }
            for row in writing_sessions
            for error in row["errors"]
        ],
        "loop_counts_by_bound_seconds": {key: len(value) for key, value in loop_sets.items()},
        "loops_300_seconds": loop_sets["300"],
        "manual_loop_classification_counts": dict(sorted(manual_counts.items())),
        "genuine_causal_loops_300_seconds": sum(
            1
            for row in loop_sets["300"]
            if row.get("manual_intent_class") == "genuine-accidental"
            and row.get("manual_causal_relation") == "causal"
        ),
        "post_state_reader_parse_catchable_loops_300_seconds": sum(
            1 for row in loop_sets["300"] if row.get("post_state_reader_parse_catchable")
        ),
        "intentional_marker_loops_300_seconds": sum(
            1 for row in loop_sets["300"] if row["intentional_marker"]
        ),
        "reader_catchable_loops_300_seconds": sum(
            1 for row in loop_sets["300"] if row["error_class"] == "reader-catchable"
        ),
        "retained_write_risk_features": dict(sorted(risk_counts.items())),
    }
    args.output.write_text(json.dumps(report, indent=2, sort_keys=True) + "\n")
    print(json.dumps({key: value for key, value in report.items() if key != "loops_300_seconds"}, indent=2))


if __name__ == "__main__":
    main()
