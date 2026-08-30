#!/usr/bin/env python3
"""Independent production-corpus screen for native Clojure parse-gate loops.

This program deliberately does not import or execute the earlier circular-corpus
audit.  It reads only the documented Codex session-store layout, admits exact
production cwd values, and emits a privacy-safe JSON receipt.  It never calls a
model, opens a network connection, or mutates a production repository.
"""

from __future__ import annotations

import argparse
import datetime as dt
import glob
import hashlib
import json
import os
import re
import subprocess
import sys
import tempfile
from collections import Counter, defaultdict
from dataclasses import dataclass
from pathlib import Path
from typing import Any, Iterable


SESSION_GLOB = "/Users/genekim/.codex/sessions/*/*/*/*.jsonl"
WINDOW_SINCE = "2026-08-08T18:08:23.783Z"
WINDOW_UNTIL = "2026-08-30T06:00:00.000Z"
LOOP_SECONDS = 300.0
PRODUCTION_ROOTS = {
    "/Users/genekim/src.local/sessionize-sched-killer": "service-a",
    "/Users/genekim/src.local/curtain-call-staging": "service-b-staging",
    "/Users/genekim/src.local/curtaincall-cfp3-reconcile": "service-b-reconcile",
}
SOURCE_SUFFIXES = (".clj", ".cljs", ".cljc")

# Broad on purpose: the bounded review decides whether a matched failure was
# caused by the preceding write.  A match is not itself a reader-error claim.
ERROR_SIGNATURES = {
    "reader-syntax": re.compile(
        r"syntax error reading source|unmatched delimiter|eof while reading|"
        r"invalid token|no reader function for tag|reader tag must be a symbol|"
        r"map literal must contain an even number|unsupported escape character",
        re.I,
    ),
    "compile-syntax": re.compile(
        r"syntax error (?:compiling|macroexpanding)|unable to resolve symbol|"
        r"could not locate .+ on classpath|classnotfoundexception|"
        r"filenotfoundexception|compilerexception",
        re.I | re.S,
    ),
    "runtime-error": re.compile(
        r"execution error|exception in thread|exceptioninfo|assertionerror",
        re.I,
    ),
    "test-failure": re.compile(
        r"(?:^|\n)(?:fail|error) in \(|\b[1-9]\d* failures?\b|"
        r"\b[1-9]\d* errors?\b",
        re.I,
    ),
}
NONZERO_PATTERNS = (
    re.compile(r'"exit_code"\s*:\s*(-?\d+)'),
    re.compile(r"process exited with code\s+(-?\d+)", re.I),
    re.compile(r"command exited with (?:status|code)\s+(-?\d+)", re.I),
)

# Bounded review is frozen as data after inspecting only the five shortlisted
# write/failure/rewrite neighborhoods.  Descriptions intentionally omit source,
# prompts, paths, people, and business data.
MANUAL_LOOP_REVIEWS = {
    "53aa8501e9a2/264": {
        "classification": "genuine-accident",
        "causal": True,
        "post_state_parser_refused": False,
        "error_shape": "direct-handler test request did not reproduce production parameter middleware",
    },
    "e7935cd15b8f/167": {
        "classification": "undecidable-or-non-causal",
        "causal": False,
        "post_state_parser_refused": False,
        "error_shape": "malformed structural-tool stdin, followed later by scope-cleanup reapplication",
    },
    "e7935cd15b8f/186": {
        "classification": "intentional",
        "causal": True,
        "post_state_parser_refused": True,
        "error_shape": "expected-red topology witness; the same write also carried an accidental delimiter defect",
    },
    "c1f7aaacac3e/175": {
        "classification": "genuine-accident",
        "causal": True,
        "post_state_parser_refused": False,
        "error_shape": "missing namespace require",
    },
    "c1f7aaacac3e/369": {
        "classification": "genuine-accident",
        "causal": True,
        "post_state_parser_refused": False,
        "error_shape": "rendered-HTML assertion depended on attribute order",
    },
}

SHADOW_REFUSAL_REVIEWS = {
    "e7935cd15b8f/176": {
        "disposition": "true-catch",
        "false_refusal": False,
        "error_shape": "unmatched closing delimiter in a production view source",
        "same_target_rewrite_within_seconds": 245.298,
    },
    "e7935cd15b8f/186": {
        "disposition": "true-catch",
        "false_refusal": False,
        "error_shape": "unmatched closing delimiter in a test namespace import",
        "same_target_rewrite_within_seconds": 242.232,
    },
}


def parse_time(value: str) -> dt.datetime:
    return dt.datetime.fromisoformat(value.replace("Z", "+00:00"))


def short_hash(value: str) -> str:
    return hashlib.sha256(value.encode("utf-8")).hexdigest()[:12]


def strings(value: Any) -> Iterable[str]:
    if isinstance(value, str):
        yield value
    elif isinstance(value, list):
        for item in value:
            yield from strings(item)
    elif isinstance(value, dict):
        for item in value.values():
            yield from strings(item)


def structured_nonzero(value: Any) -> bool:
    if isinstance(value, dict):
        for key, item in value.items():
            if key in {"exit_code", "exitCode", "status_code"}:
                try:
                    if int(item) != 0:
                        return True
                except (TypeError, ValueError):
                    pass
            if structured_nonzero(item):
                return True
    elif isinstance(value, list):
        return any(structured_nonzero(item) for item in value)
    elif isinstance(value, str):
        try:
            decoded = json.loads(value)
        except (json.JSONDecodeError, TypeError):
            decoded = None
        if decoded is not None and decoded is not value and structured_nonzero(decoded):
            return True
        for pattern in NONZERO_PATTERNS:
            for match in pattern.finditer(value):
                if int(match.group(1)) != 0:
                    return True
    return False


def failure_signatures(value: Any) -> list[str]:
    text = "\n".join(strings(value))
    found = [name for name, pattern in ERROR_SIGNATURES.items() if pattern.search(text)]
    if structured_nonzero(value):
        found.append("nonzero-exit")
    return found


@dataclass(frozen=True)
class Write:
    event_index: int
    timestamp: dt.datetime
    call_id: str
    ordinal: int | None
    targets: frozenset[str]
    diffs: tuple[tuple[str, str], ...]


@dataclass(frozen=True)
class Failure:
    event_index: int
    timestamp: dt.datetime
    call_id: str
    ordinal: int | None
    tool_name: str | None
    signatures: tuple[str, ...]


def load_session(path: str) -> tuple[dict[str, Any], list[dict[str, Any]]]:
    events = []
    with open(path, encoding="utf-8") as stream:
        first = json.loads(next(stream))
        events.append(first)
        for line in stream:
            try:
                events.append(json.loads(line))
            except json.JSONDecodeError:
                continue
    return first, events


def eligible_files() -> list[tuple[str, dict[str, Any]]]:
    since = parse_time(WINDOW_SINCE)
    until = parse_time(WINDOW_UNTIL)
    admitted = []
    for path in glob.iglob(SESSION_GLOB):
        try:
            with open(path, encoding="utf-8") as stream:
                first = json.loads(next(stream))
        except (OSError, StopIteration, json.JSONDecodeError):
            continue
        payload = first.get("payload", {})
        if first.get("type") != "session_meta" or payload.get("cwd") not in PRODUCTION_ROOTS:
            continue
        try:
            started = parse_time(payload["timestamp"])
        except (KeyError, TypeError, ValueError):
            continue
        if since <= started < until:
            admitted.append((path, payload))
    return sorted(admitted)


def analyze_session(path: str, meta: dict[str, Any]) -> dict[str, Any]:
    _, events = load_session(path)
    until = parse_time(WINDOW_UNTIL)
    calls: dict[str, tuple[str | None, int | None]] = {}
    writes: list[Write] = []
    failures: list[Failure] = []

    for index, event in enumerate(events):
        payload = event.get("payload", {})
        kind = payload.get("type")
        call_id = payload.get("call_id")
        if kind in {"function_call", "custom_tool_call"} and call_id:
            calls[call_id] = (payload.get("name"), event.get("ordinal"))

        timestamp_text = event.get("timestamp")
        if not timestamp_text:
            continue
        try:
            timestamp = parse_time(timestamp_text)
        except ValueError:
            continue
        if timestamp >= until:
            continue

        if kind == "patch_apply_end" and payload.get("success") is True:
            targets = {
                target
                for target, change in (payload.get("changes") or {}).items()
                if target.endswith(SOURCE_SUFFIXES)
                and isinstance(change, dict)
                and change.get("type") == "update"
            }
            if targets:
                _, ordinal = calls.get(call_id, (None, None))
                writes.append(
                    Write(
                        index,
                        timestamp,
                        call_id or "",
                        ordinal,
                        frozenset(targets),
                        tuple(
                            sorted(
                                (target, payload["changes"][target]["unified_diff"])
                                for target in targets
                            )
                        ),
                    )
                )

        if kind in {"function_call_output", "custom_tool_call_output"}:
            signatures = failure_signatures(payload.get("output"))
            if signatures:
                tool_name, ordinal = calls.get(call_id, (None, None))
                failures.append(
                    Failure(
                        index,
                        timestamp,
                        call_id or "",
                        ordinal,
                        tool_name,
                        tuple(signatures),
                    )
                )

    # One loop per prior-write/rewrite pair.  Repeated diagnostics between the
    # same writes do not inflate the count; the first matching failure anchors
    # the review neighborhood.
    pairs: dict[tuple[str, str], tuple[Write, Failure, Write]] = {}
    for failure in failures:
        prior = next(
            (
                write
                for write in reversed(writes)
                if write.timestamp < failure.timestamp
                and (failure.timestamp - write.timestamp).total_seconds() <= LOOP_SECONDS
            ),
            None,
        )
        if prior is None:
            continue
        rewrite = next(
            (
                write
                for write in writes
                if write.timestamp > failure.timestamp
                and (write.timestamp - prior.timestamp).total_seconds() <= LOOP_SECONDS
                and write.targets.intersection(prior.targets)
            ),
            None,
        )
        if rewrite is None:
            continue
        pairs.setdefault((prior.call_id, rewrite.call_id), (prior, failure, rewrite))

    session_key = short_hash(meta["id"])
    candidates = []
    for prior, failure, rewrite in sorted(pairs.values(), key=lambda row: row[0].timestamp):
        candidates.append(
            {
                "candidate_key": f"{session_key}/{prior.ordinal if prior.ordinal is not None else prior.event_index}",
                "session_key": session_key,
                "evidence_file": os.path.basename(path),
                "prior_write_event_index": prior.event_index,
                "failure_event_index": failure.event_index,
                "rewrite_event_index": rewrite.event_index,
                "prior_write_ordinal": prior.ordinal,
                "failure_ordinal": failure.ordinal,
                "rewrite_ordinal": rewrite.ordinal,
                "write_to_failure_seconds": round((failure.timestamp - prior.timestamp).total_seconds(), 3),
                "write_to_rewrite_seconds": round((rewrite.timestamp - prior.timestamp).total_seconds(), 3),
                "shared_target_hashes": sorted(short_hash(p) for p in prior.targets & rewrite.targets),
                "failure_tool": failure.tool_name,
                "failure_signatures": list(failure.signatures),
            }
        )
    return {
        "session_key": session_key,
        "repo": PRODUCTION_ROOTS[meta["cwd"]],
        "started_at": meta["timestamp"],
        "source_writes": len(writes),
        "failure_outputs": len(failures),
        "candidates": candidates,
        "_shadow_updates": [
            {
                "write_key": f"{session_key}/{write.event_index}",
                "timestamp": write.timestamp,
                "event_index": write.event_index,
                "root": meta["cwd"],
                "repo": PRODUCTION_ROOTS[meta["cwd"]],
                "diffs": write.diffs,
            }
            for write in writes
        ],
    }


def historical_blobs(root: str, relative_path: str) -> set[bytes]:
    history = subprocess.run(
        ["git", "-C", root, "log", "--all", "--format=%H", "--", relative_path],
        capture_output=True,
        text=True,
        check=True,
    ).stdout.splitlines()
    specs = {f"{commit}:{relative_path}" for commit in history}
    specs.update(f"{commit}^:{relative_path}" for commit in history)
    states: dict[str, bytes] = {}
    for spec in specs:
        result = subprocess.run(
            ["git", "-C", root, "show", spec], capture_output=True
        )
        if result.returncode == 0:
            states[hashlib.sha256(result.stdout).hexdigest()] = result.stdout
    return set(states.values())


def apply_hunks(source: bytes, unified_diff: str) -> bytes | None:
    with tempfile.TemporaryDirectory(prefix="production-parse-replay-") as temp_dir:
        source_path = Path(temp_dir, "source.clj")
        output_path = Path(temp_dir, "post.clj")
        source_path.write_bytes(source)
        result = subprocess.run(
            [
                "patch",
                "--batch",
                "--silent",
                "--fuzz=0",
                "-o",
                str(output_path),
                str(source_path),
            ],
            input=unified_diff.encode("utf-8"),
            capture_output=True,
        )
        if result.returncode != 0 or not output_path.exists():
            return None
        return output_path.read_bytes()


def parse_states(states: dict[str, bytes]) -> dict[str, bool]:
    if not states:
        return {}
    with tempfile.TemporaryDirectory(prefix="production-shadow-parse-") as temp_dir:
        paths = []
        by_path = {}
        for digest, source in sorted(states.items()):
            path = Path(temp_dir, f"{digest}.clj")
            path.write_bytes(source)
            paths.append(str(path))
            by_path[str(path)] = digest
        program = """
(require '[clojure.java.io :as io]
         '[rewrite-clj.parser :as parser])
(with-open [reader (io/reader (first *command-line-args*))]
  (doseq [path (line-seq reader)]
    (try
      (parser/parse-string-all (slurp path))
      (println path "ok")
      (catch Throwable _
        (println path "refused")))))
"""
        program_path = Path(temp_dir, "shadow_parser.clj")
        manifest_path = Path(temp_dir, "manifest.txt")
        program_path.write_text(program, encoding="utf-8")
        manifest_path.write_text("\n".join(paths) + "\n", encoding="utf-8")
        result = subprocess.run(
            ["clojure", "-M", str(program_path), str(manifest_path)],
            capture_output=True,
            text=True,
        )
        if result.returncode != 0:
            raise RuntimeError(f"shadow parser failed: {result.stderr[-1000:]}")
        outcomes = {}
        for line in result.stdout.splitlines():
            path, outcome = line.rsplit(" ", 1)
            if path in by_path and outcome in {"ok", "refused"}:
                outcomes[by_path[path]] = outcome == "ok"
        if set(outcomes) != set(states):
            raise RuntimeError("shadow parser did not report every reconstructed state")
        return outcomes


def shadow_replay(updates: list[dict[str, Any]]) -> dict[str, Any]:
    pools: dict[tuple[str, str], dict[str, bytes]] = {}
    all_post_states: dict[str, bytes] = {}
    actions = []
    for update in sorted(updates, key=lambda row: row["timestamp"]):
        targets = []
        for absolute_path, unified_diff in update["diffs"]:
            root = update["root"]
            relative_path = os.path.relpath(absolute_path, root)
            key = (root, relative_path)
            if key not in pools:
                seeds = historical_blobs(root, relative_path)
                pools[key] = {hashlib.sha256(value).hexdigest(): value for value in seeds}
            posts: dict[str, bytes] = {}
            for source in list(pools[key].values()):
                post = apply_hunks(source, unified_diff)
                if post is not None:
                    digest = hashlib.sha256(post).hexdigest()
                    posts[digest] = post
            pools[key].update(posts)
            all_post_states.update(posts)
            targets.append(
                {
                    "target_hash": short_hash(absolute_path),
                    "candidate_post_states": sorted(posts),
                }
            )
        actions.append(
            {
                "write_key": update["write_key"],
                "repo": update["repo"],
                "event_index": update["event_index"],
                "targets": targets,
            }
        )

    outcomes = parse_states(all_post_states)
    status_counts = Counter()
    target_counts = Counter()
    refused_actions = []
    for action in actions:
        action_target_statuses = []
        for target in action["targets"]:
            values = [outcomes[digest] for digest in target.pop("candidate_post_states")]
            if not values:
                status = "unavailable"
                target["candidate_post_state_count"] = 0
            elif all(values):
                status = "accepted"
                target["candidate_post_state_count"] = len(values)
            elif not any(values):
                status = "refused"
                target["candidate_post_state_count"] = len(values)
            else:
                status = "ambiguous"
                target["candidate_post_state_count"] = len(values)
            target["status"] = status
            target_counts[status] += 1
            action_target_statuses.append(status)
        if "refused" in action_target_statuses:
            action_status = "refused"
            refused_actions.append(action["write_key"])
        elif "ambiguous" in action_target_statuses:
            action_status = "ambiguous"
        elif "unavailable" in action_target_statuses:
            action_status = "unavailable"
        else:
            action_status = "accepted"
        action["status"] = action_status
        status_counts[action_status] += 1

    return {
        "parser": "rewrite-clj.parser/parse-string-all",
        "reconstruction": "apply each retained hunk with fuzz=0 to every matching historical or previously generated blob; classify only unanimous post-state outcomes",
        "write_actions": len(actions),
        "action_statuses": dict(sorted(status_counts.items())),
        "target_statuses": dict(sorted(target_counts.items())),
        "refused_write_keys": refused_actions,
        "actions": actions,
    }


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--receipt-out", type=Path, required=True)
    args = parser.parse_args()

    sessions = [analyze_session(path, meta) for path, meta in eligible_files()]
    shadow_updates = [update for row in sessions for update in row.pop("_shadow_updates")]
    repo_sessions = Counter(row["repo"] for row in sessions)
    repo_writes = Counter()
    repo_candidates = Counter()
    candidates = []
    for row in sessions:
        repo_writes[row["repo"]] += row["source_writes"]
        repo_candidates[row["repo"]] += len(row["candidates"])
        candidates.extend(row["candidates"])

    since = parse_time(WINDOW_SINCE)
    until = parse_time(WINDOW_UNTIL)
    span_seconds = (until - since).total_seconds()
    span_weeks = span_seconds / (7 * 86400.0)
    candidate_keys = {row["candidate_key"] for row in candidates}
    if candidate_keys != set(MANUAL_LOOP_REVIEWS):
        raise RuntimeError("candidate population changed after bounded review")
    for candidate in candidates:
        candidate["review"] = MANUAL_LOOP_REVIEWS[candidate["candidate_key"]]
    review_counts = Counter(
        review["classification"] for review in MANUAL_LOOP_REVIEWS.values()
    )
    genuine = [
        review
        for review in MANUAL_LOOP_REVIEWS.values()
        if review["classification"] == "genuine-accident"
    ]
    shadow = shadow_replay(shadow_updates)
    if set(shadow["refused_write_keys"]) != set(SHADOW_REFUSAL_REVIEWS):
        raise RuntimeError("shadow refusal population changed after bounded review")
    shadow["bounded_refusal_review"] = SHADOW_REFUSAL_REVIEWS
    shadow["true_catches"] = len(SHADOW_REFUSAL_REVIEWS)
    shadow["false_refusals"] = sum(
        review["false_refusal"] for review in SHADOW_REFUSAL_REVIEWS.values()
    )
    receipt = {
        "schema": "production-parse-gate-reopening-audit.v1",
        "status": "ok",
        "method": {
            "implementation": "independent; does not import or execute the circular-corpus audit",
            "session_layout": SESSION_GLOB.replace("/Users/genekim", "<home>"),
            "session_eligibility": "first record is session_meta; exact admitted cwd; session start in half-open window",
            "write_eligibility": "successful patch_apply_end update to an existing .clj/.cljs/.cljc target",
            "failure_eligibility": "tool output has a declared reader, compile, runtime, test-failure, or nonzero-exit signature",
            "loop_eligibility": "latest prior eligible write, then failure, then first eligible same-target rewrite; all within 300 seconds; repeated diagnostics deduplicated by write pair",
        },
        "window": {
            "utc_since": WINDOW_SINCE,
            "utc_until": WINDOW_UNTIL,
            "half_open": True,
            "span_seconds": span_seconds,
            "span_days": span_seconds / 86400.0,
            "span_weeks": span_weeks,
        },
        "population": {
            "sessions": len(sessions),
            "sessions_by_repo": dict(sorted(repo_sessions.items())),
            "source_writes": sum(repo_writes.values()),
            "source_writes_by_repo": dict(sorted(repo_writes.items())),
            "candidate_loops": len(candidates),
            "candidate_loops_by_repo": dict(sorted(repo_candidates.items())),
            "all_eligible_writes_upper_bound_per_week": sum(repo_writes.values()) / span_weeks,
        },
        "bounded_review": {
            "classification_counts": dict(sorted(review_counts.items())),
            "genuine_loops": len(genuine),
            "genuine_loops_per_week": len(genuine) / span_weeks,
            "genuine_post_states_refused": sum(
                review["post_state_parser_refused"] for review in genuine
            ),
            "shadow_true_catch_repair_loops": len(SHADOW_REFUSAL_REVIEWS),
            "shadow_true_catch_repair_loops_per_week": len(SHADOW_REFUSAL_REVIEWS) / span_weeks,
            "reviews": MANUAL_LOOP_REVIEWS,
        },
        "shadow_parser": shadow,
        "sessions": sessions,
        "candidates": candidates,
    }
    encoded = json.dumps(receipt, indent=2, sort_keys=True) + "\n"
    args.receipt_out.write_text(encoded, encoding="utf-8")
    print(json.dumps({"status": "ok", **receipt["window"], **receipt["population"], "receipt_path": str(args.receipt_out)}, indent=2, sort_keys=True))
    return 0


if __name__ == "__main__":
    sys.exit(main())
