#!/usr/bin/env python3
"""_make_targets.py — resolve a project's Make targets to the commands they RUN.

Sol's executed review, item 4: `is_test_command` matches a test runner at command
position, but it had no way THROUGH `make <target>` other than the target's NAME.
`make verify` -- a Kaocha run behind a target that does not say "test" -- metered
as a non-test action, which is the exact quantity E3's pass line is stated in.

Guessing from the name is what produced the hole, so nothing here guesses.  Each
target is resolved with `make -n` (a dry run: make PRINTS the recipe it would
execute and runs nothing), at ATTEST TIME, into the map the watcher and the scorer
both read.  The watcher then matches the EXPANDED command at command position.

    _make_targets.py <project-root> <out.json>

Writes {"root", "make", "generated_utc", "targets": {name: expanded recipe},
        "unresolved": [...], "truncated": bool}.

Exit 0 = a map was written.  Exit 3 = no Makefile (the caller records "no map",
which is a fact about the project, not a failure of this script).  Exit 2 = make
is not runnable.
"""
from __future__ import annotations

import json
import os
import pathlib
import re
import subprocess
import sys
import time
from datetime import datetime, timezone

MAKEFILES = ("GNUmakefile", "makefile", "Makefile")
# A target line: a name, then `:` or `::`, but never `:=` / `::=` (an assignment).
TARGET_RE = re.compile(r"^([A-Za-z0-9][A-Za-z0-9._/+-]*)\s*::?(?![=])")
PHONY_RE = re.compile(r"^\.PHONY\s*:\s*(.*)$")
MAX_TARGETS = 300           # a bound, so a huge Makefile cannot stall an arm
PER_TARGET_TIMEOUT = 20.0
TOTAL_BUDGET_S = 180.0
MAX_RECIPE_CHARS = 8000


def makefile_in(root: pathlib.Path) -> pathlib.Path | None:
    for name in MAKEFILES:
        path = root / name
        if path.is_file():
            return path
    return None


def declared_targets(makefile: pathlib.Path) -> list[str]:
    """Every target name the Makefile declares, in file order, de-duplicated."""
    names: list[str] = []
    seen: set[str] = set()

    def add(name: str) -> None:
        name = name.strip()
        if not name or name in seen:
            return
        if "%" in name or "$" in name:      # pattern rules and computed names
            return
        if name.startswith("."):            # .PHONY, .DEFAULT_GOAL, ...
            return
        seen.add(name)
        names.append(name)

    for line in makefile.read_text(errors="replace").split("\n"):
        if line.startswith("\t"):           # a recipe line, never a target line
            continue
        phony = PHONY_RE.match(line)
        if phony:
            for name in phony.group(1).split():
                add(name)
            continue
        match = TARGET_RE.match(line)
        if match:
            add(match.group(1))
    return names


def resolve(root: pathlib.Path, targets: list[str]) -> tuple[dict, list[str], bool]:
    resolved: dict[str, str] = {}
    unresolved: list[str] = []
    started = time.time()
    truncated = False
    for i, target in enumerate(targets):
        if i >= MAX_TARGETS or time.time() - started > TOTAL_BUDGET_S:
            truncated = True
            unresolved.extend(targets[i:])
            break
        try:
            proc = subprocess.run(
                ["make", "-n", "--no-print-directory", target],
                cwd=str(root), capture_output=True, text=True,
                timeout=PER_TARGET_TIMEOUT, stdin=subprocess.DEVNULL,
            )
        except Exception as exc:
            unresolved.append(f"{target}:{type(exc).__name__}")
            continue
        recipe = (proc.stdout or "").strip()
        if not recipe:
            unresolved.append(f"{target}:rc={proc.returncode}:no-recipe")
            continue
        resolved[target] = recipe[:MAX_RECIPE_CHARS]
    return resolved, unresolved, truncated


def main() -> int:
    if len(sys.argv) != 3:
        print("usage: _make_targets.py <project-root> <out.json>", file=sys.stderr)
        return 2
    root = pathlib.Path(sys.argv[1]).resolve()
    out = pathlib.Path(sys.argv[2])

    makefile = makefile_in(root)
    if makefile is None:
        print(f"MAKE-MAP none: no Makefile under {root}", file=sys.stderr)
        return 3
    try:
        version = subprocess.run(["make", "--version"], capture_output=True, text=True,
                                 timeout=10, stdin=subprocess.DEVNULL)
    except Exception as exc:
        print(f"MAKE-MAP failed: make is not runnable ({exc})", file=sys.stderr)
        return 2

    targets = declared_targets(makefile)
    resolved, unresolved, truncated = resolve(root, targets)
    payload = {
        "root": str(root),
        "makefile": makefile.name,
        "make": (version.stdout or "").split("\n")[0],
        "generated_utc": datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ"),
        "declared": len(targets),
        "targets": resolved,
        "unresolved": unresolved,
        "truncated": truncated,
    }
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_text(json.dumps(payload, indent=2, sort_keys=True) + "\n")
    print(f"MAKE-MAP {out} declared={len(targets)} resolved={len(resolved)} "
          f"unresolved={len(unresolved)} truncated={truncated}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
