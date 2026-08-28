#!/usr/bin/env python3
"""Find direct cclsp client entrances without exposing configuration values."""

from __future__ import annotations

import argparse
import json
import os
import re
from pathlib import Path


SCHEMA = "clj-surgeon.direct-cclsp-client-audit.v1"
PRUNED_DIRS = {
    ".git", ".cpcache", ".clj-kondo", ".lsp", "node_modules",
    "target", "data", "run-logs",
}
CONFIG_NAMES = {".mcp.json", "config.toml", "settings.json", "settings.local.json"}
CONFIG_PATTERNS = (
    ("codex-server", re.compile(r"^\s*\[mcp_servers\.cclsp(?:\.|\])", re.I)),
    ("claude-enabled-server", re.compile(r'"cclsp"', re.I)),
    ("json-server", re.compile(r'"cclsp"\s*:', re.I)),
)
MAKEFILE_PATTERNS = (
    ("make-target", re.compile(r"^\s*(?:install-|start-|stop-)?cclsp[^:]*:", re.I)),
    ("make-install-reference", re.compile(r"install-cclsp", re.I)),
    ("make-public-endpoint", re.compile(r"mcp_servers\.cclsp|:7890/mcp\s*\(cclsp\)", re.I)),
)


def implementation_or_frozen(path: Path, root: Path) -> bool:
    try:
        parts = path.relative_to(root).parts
    except ValueError:
        parts = path.parts
    if not parts:
        return False
    repo = parts[0]
    return (
        repo == "clj-surgeon"
        or repo.startswith("clj-surgeon-")
        or repo == "cclsp"
        or repo.startswith("cclsp-")
        or repo.endswith("-baseline")
    )


def candidate_files(root: Path):
    for current, dirs, files in os.walk(root):
        dirs[:] = [name for name in dirs if name not in PRUNED_DIRS]
        current_path = Path(current)
        for name in files:
            path = current_path / name
            if name == "Makefile":
                yield "makefile", path
            elif name in CONFIG_NAMES and (
                ".codex" in path.parts or ".claude" in path.parts or name == ".mcp.json"
            ):
                yield "config", path


def scan(root: Path) -> dict:
    violations = []
    scanned = {"config_files": 0, "makefiles": 0}
    for category, path in candidate_files(root):
        if category == "makefile" and implementation_or_frozen(path, root):
            continue
        scanned["makefiles" if category == "makefile" else "config_files"] += 1
        try:
            lines = path.read_text(errors="replace").splitlines()
        except OSError:
            continue
        patterns = MAKEFILE_PATTERNS if category == "makefile" else CONFIG_PATTERNS
        for line_number, line in enumerate(lines, 1):
            for kind, pattern in patterns:
                if pattern.search(line):
                    violations.append({"kind": kind, "file": str(path), "line": line_number})
                    break
    return {
        "schema": SCHEMA,
        "root": str(root),
        "ok": not violations,
        "scanned": scanned,
        "violations": violations,
        "exemptions": [
            "cclsp provider repositories",
            "clj-surgeon implementation and experiment worktrees",
            "frozen benchmark baselines",
        ],
    }


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", type=Path, required=True)
    args = parser.parse_args()
    result = scan(args.root.resolve())
    print(json.dumps(result, indent=2, sort_keys=True))
    return 0 if result["ok"] else 1


if __name__ == "__main__":
    raise SystemExit(main())
