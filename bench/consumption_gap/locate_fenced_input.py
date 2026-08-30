#!/usr/bin/env python3
"""Locate already-copied telemetry slices by digest without emitting content."""

from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path


def canonical(value):
    return json.dumps(value, sort_keys=True, separators=(",", ":")).encode()


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("root", type=Path)
    parser.add_argument("--since", required=True)
    parser.add_argument("--until", required=True)
    parser.add_argument("--expected", action="append", required=True)
    parser.add_argument("--out", type=Path)
    args = parser.parse_args()
    expected = set(args.expected)

    matches = []
    files_examined = 0
    for path in sorted(args.root.rglob("*.jsonl")):
        try:
            raw = path.read_bytes()
        except (OSError, PermissionError):
            continue
        if b"batch-form-selection-failed" not in raw:
            continue
        files_examined += 1
        selected = []
        selected_lines = bytearray()
        for line in raw.splitlines(keepends=True):
            try:
                value = json.loads(line)
            except (json.JSONDecodeError, UnicodeDecodeError):
                continue
            timestamp = value.get("timestamp") if isinstance(value, dict) else None
            if isinstance(timestamp, str) and args.since <= timestamp <= args.until:
                selected.append(value)
                selected_lines.extend(line)
        digests = {
            "whole_file": hashlib.sha256(raw).hexdigest(),
            "selected_events": hashlib.sha256(canonical(selected)).hexdigest(),
            "selected_lines": hashlib.sha256(selected_lines).hexdigest(),
        }
        hit = sorted(expected & set(digests.values()))
        if hit:
            matches.append({
                "path": str(path),
                "selected_events": len(selected),
                "matching_sha256": hit,
                "digest_kind": [name for name, value in digests.items() if value in hit],
            })

    rendered = json.dumps({
        "schema": "consumption-gap-input-locator.v1",
        "root": str(args.root),
        "files_examined": files_examined,
        "expected_count": len(expected),
        "matches": matches,
    }, indent=2, sort_keys=True) + "\n"
    if args.out:
        args.out.write_text(rendered)
    print(rendered, end="")


if __name__ == "__main__":
    main()
