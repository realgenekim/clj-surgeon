#!/usr/bin/env python3
"""_make_targets.py — resolve a project's Make targets to the commands they RUN,
BY READING THE MAKEFILE.  This script never executes make, and never executes
anything the repository controls.

Sol's first executed review, item 4: `is_test_command` matches a test runner at
command position, but it had no way THROUGH `make <target>` other than the target's
NAME.  `make verify` -- a Kaocha run behind a target that does not say "test" --
metered as a non-test action, which is the exact quantity E3's pass line is stated in.

Sol's SECOND executed review, item 1: the first repair resolved each target with
`make -n`, on the reasoning that -n prints a recipe and runs nothing.  That is false
about the PARSE.  `make` expands `:=` assignments while reading the file, so a
`SIDE := $(shell touch …)` fires before any recipe is considered; and a recipe line
prefixed `+` is defined to run even under -n.  Sol watched both happen.  The
attestation whose whole job is to decide whether a repository may run was executing
that repository's code to make the decision.

So nothing here runs.  The Makefile is PARSED as text:

  * simple variable assignments are collected and expanded into recipes, so
    `verify: $(KAOCHA) --focus …` resolves without a make process;
  * a recipe the parse cannot trust is REFUSED with a typed reason and kept out of
    the resolved map -- `$(shell`, `$(eval`, `+$(MAKE)`, a target defined inside a
    make conditional, a recipe still holding an unexpandable reference, or a recipe
    whose own SHELL control flow decides which command runs (`if … then bin/kaocha`
    is a test runner on some invocations and not others, and the text cannot say
    which);
  * a hard `include` of a file that does not exist on disk is a file MAKE WOULD
    GENERATE: targets can be defined there that this parse cannot see at all, so the
    whole map is untrustworthy and `dynamic_refusal` is set.  attest.sh turns that
    into ATTEST-MISMATCH `makefile-dynamic` and no driver launches.

A target that is refused, or simply not declared, is NOT a non-test action.  It is
unresolved, and watch.py makes a run that calls one `incomplete-run` (Sol, item 6):
guessing from the name is what produced the original hole.

    _make_targets.py <project-root> <out.json>

Writes {"root", "makefile", "parser", "generated_utc", "declared", "targets",
        "refused", "unresolved", "includes", "dynamic_refusal", "truncated"}.

Exit 0 = a trustworthy map was written.  Exit 3 = no Makefile (the caller records
"no map", a fact about the project, not a failure).  Exit 4 = a map was written and
it is NOT trustworthy; `dynamic_refusal` says why and the arm must not launch.
"""
from __future__ import annotations

import json
import pathlib
import re
import sys
from datetime import datetime, timezone

MAKEFILES = ("GNUmakefile", "makefile", "Makefile")

# A target line: one or more names, then `:` or `::`, but never `:=` / `::=`.
TARGET_RE = re.compile(
    r"^([A-Za-z0-9][A-Za-z0-9._/+-]*(?:[ \t]+[A-Za-z0-9][A-Za-z0-9._/+-]*)*)[ \t]*::?(?![=])[ \t]*(.*)$")
ASSIGN_RE = re.compile(
    r"^(?:export[ \t]+|override[ \t]+)*([A-Za-z_][A-Za-z0-9_.]*)[ \t]*(::=|:=|\?=|\+=|=)[ \t]*(.*)$")
PHONY_RE = re.compile(r"^\.PHONY[ \t]*:[ \t]*(.*)$")
COND_OPEN_RE = re.compile(r"^[ \t]*(ifeq|ifneq|ifdef|ifndef)\b")
COND_ELSE_RE = re.compile(r"^[ \t]*else\b")
COND_CLOSE_RE = re.compile(r"^[ \t]*endif\b")
INCLUDE_RE = re.compile(r"^[ \t]*(-include|sinclude|include)[ \t]+(.*)$")
VAR_REF_RE = re.compile(r"\$[({]([A-Za-z_][A-Za-z0-9_.]*)[)}]")
# anything still looking like an expansion after the substitution pass
LEFTOVER_REF_RE = re.compile(r"\$[({][^)}]*[)}]|\$[A-Za-z@<^*?%+]")
RECIPE_PREFIX_RE = re.compile(r"^[ \t]*([-@+]*)[ \t]*")

MAX_TARGETS = 300           # a bound, so a huge Makefile cannot stall an arm
MAX_RECIPE_CHARS = 8000
MAX_EXPAND_DEPTH = 12

# Built-ins this parser is willing to assert a value for.  `$(MAKE)` expands to the
# recursive-make command so `$(MAKE) test` can be followed through the map by name;
# `+$(MAKE)` is refused separately, before any expansion happens.
BUILTIN_VARS = {"MAKE": "make"}


class MakefileUntrustworthy(RuntimeError):
    """The parse cannot see the whole Makefile.  The map must not be used."""


def makefile_in(root: pathlib.Path) -> pathlib.Path | None:
    for name in MAKEFILES:
        path = root / name
        if path.is_file():
            return path
    return None


def logical_lines(text: str) -> list[str]:
    """Backslash-continued physical lines joined into the lines make actually sees."""
    out: list[str] = []
    pending = ""
    for raw in text.split("\n"):
        if raw.endswith("\\"):
            pending += raw[:-1] + " "
            continue
        out.append(pending + raw)
        pending = ""
    if pending:
        out.append(pending)
    return out


def expand(text: str, variables: dict[str, str], depth: int = 0) -> str:
    """Substitute `$(NAME)` / `${NAME}` from `variables`, recursively and bounded."""
    if depth >= MAX_EXPAND_DEPTH:
        return text

    def sub(match: re.Match) -> str:
        name = match.group(1)
        if name in variables:
            return expand(variables[name], variables, depth + 1)
        return match.group(0)

    return VAR_REF_RE.sub(sub, text)


# A recipe whose CONTROL FLOW decides which command runs: `if … then bin/kaocha …`
# runs a test runner on some invocations and not others, and the text cannot say which.
SHELL_CONTROL_RE = re.compile(
    r"(?m)(?:^|[;&|(]|^\s*)\s*(?:if|case|while|until|for)\s|;\s*then\b|\besac\b")


def dynamic_reason(text: str) -> str | None:
    """A typed reason this text cannot be resolved without running something."""
    for marker in ("$(shell", "${shell", "$(eval", "${eval"):
        if marker in text:
            return f"dynamic:{marker}"
    if "`" in text:
        return "dynamic:backtick"
    return None


def parse(makefile: pathlib.Path, root: pathlib.Path) -> dict:
    """The whole Makefile, read as text.  Nothing here executes."""
    variables: dict[str, str] = dict(BUILTIN_VARS)
    order: list[str] = []
    blocks: dict[str, dict] = {}
    phony: list[str] = []
    includes: list[str] = []
    optional_missing: list[str] = []

    cond_depth = 0
    current: list[str] = []

    def start(names: list[str], prereqs: str) -> None:
        nonlocal current
        current = []
        for name in names:
            if name.startswith(".") or "%" in name or "$" in name:
                continue
            if name not in blocks:
                blocks[name] = {"recipe": [], "prereqs": [], "conditional": False}
                order.append(name)
            block = blocks[name]
            if cond_depth:
                block["conditional"] = True
            block["prereqs"] = [p for p in prereqs.split() if p and "$" not in p]
            current.append(name)

    for line in logical_lines(makefile.read_text(errors="replace")):
        if line.startswith("\t"):
            for name in current:
                blocks[name]["recipe"].append(line[1:])
            continue
        stripped = line.strip()
        if not stripped or stripped.startswith("#"):
            continue

        if COND_OPEN_RE.match(line):
            cond_depth += 1
            current = []
            continue
        if COND_CLOSE_RE.match(line):
            cond_depth = max(0, cond_depth - 1)
            current = []
            continue
        if COND_ELSE_RE.match(line):
            current = []
            continue

        inc = INCLUDE_RE.match(line)
        if inc:
            kind, rest = inc.group(1), expand(inc.group(2).strip(), variables)
            for token in rest.split():
                if LEFTOVER_REF_RE.search(token):
                    raise MakefileUntrustworthy(f"include-unresolvable:{token}")
                if (root / token).is_file():
                    includes.append(token)
                elif kind == "include":
                    # make would BUILD this file, then read target definitions out of
                    # it.  A static parse cannot see them, so the map is incomplete in
                    # a way no per-target refusal can express.
                    raise MakefileUntrustworthy(f"include-generated:{token}")
                else:
                    optional_missing.append(token)
            current = []
            continue

        phony_match = PHONY_RE.match(line)
        if phony_match:
            phony.extend(phony_match.group(1).split())
            current = []
            continue

        assign = ASSIGN_RE.match(line)
        if assign and not TARGET_RE.match(line):
            name, op, value = assign.group(1), assign.group(2), assign.group(3).strip()
            if cond_depth:
                variables.pop(name, None)     # value depends on the environment
                current = []
                continue
            if op == "+=":
                variables[name] = (variables.get(name, "") + " " + value).strip()
            elif op == "?=":
                variables.setdefault(name, value)
            else:
                variables[name] = value
            current = []
            continue

        target = TARGET_RE.match(line)
        if target:
            start(target.group(1).split(), target.group(2))
            continue

        current = []

    for name in phony:
        if name.startswith(".") or "%" in name or "$" in name:
            continue
        if name not in blocks:
            blocks[name] = {"recipe": [], "prereqs": [], "conditional": False}
            order.append(name)

    return {"variables": variables, "order": order, "blocks": blocks,
            "includes": includes, "optional_missing": optional_missing}


def classify(parsed: dict) -> tuple[dict, dict, list[str], bool]:
    """Every declared target -> a resolved recipe, or a typed refusal."""
    variables = parsed["variables"]
    resolved: dict[str, str] = {}
    refused: dict[str, str] = {}
    truncated = False

    for i, name in enumerate(parsed["order"]):
        if i >= MAX_TARGETS:
            truncated = True
            refused[name] = "truncated:target-budget"
            continue
        block = parsed["blocks"][name]
        raw_lines = block["recipe"]

        if block["conditional"]:
            refused[name] = "conditional"
            continue

        plus_make = any(
            ("$(MAKE)" in raw or "${MAKE}" in raw)
            and "+" in RECIPE_PREFIX_RE.match(raw).group(1)
            for raw in raw_lines)
        if plus_make:
            refused[name] = "dynamic:+$(MAKE)"
            continue

        body = "\n".join(RECIPE_PREFIX_RE.sub("", raw) for raw in raw_lines)
        reason = dynamic_reason(body)
        if reason:
            refused[name] = reason
            continue

        expanded = expand(body, variables).replace("$@", name)
        reason = dynamic_reason(expanded)
        if reason:
            refused[name] = reason              # a variable carried it in
            continue
        leftover = LEFTOVER_REF_RE.search(expanded)
        if leftover:
            refused[name] = f"unexpanded:{leftover.group(0)}"
            continue
        if SHELL_CONTROL_RE.search(expanded):
            # Sol round two, item 6, in the form item 1's repair does not reach: the
            # recipe text is fully known and STILL does not say which command runs.
            # A target whose runner appears only on some invocations cannot be metered
            # as either a test action or a non-test one.
            refused[name] = "shell-conditional"
            continue

        expanded = expanded.strip()
        if not expanded:
            prereqs = block["prereqs"]
            if prereqs:
                # No recipe of its own: what `make <name>` RUNS is its prerequisites.
                # Written as a make invocation so watch.py follows it through this same
                # map, bounded by MAX_MAKE_DEPTH, still without running anything.
                resolved[name] = "make " + " ".join(prereqs)
            else:
                refused[name] = "no-recipe"
            continue
        resolved[name] = expanded[:MAX_RECIPE_CHARS]

    unresolved = [f"{name}:{reason}" for name, reason in sorted(refused.items())]
    return resolved, refused, unresolved, truncated


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

    payload = {
        "root": str(root),
        "makefile": makefile.name,
        "parser": "static",
        "generated_utc": datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ"),
        "declared": 0,
        "targets": {},
        "refused": {},
        "unresolved": [],
        "includes": [],
        "optional_includes_missing": [],
        "dynamic_refusal": None,
        "truncated": False,
    }

    try:
        parsed = parse(makefile, root)
    except MakefileUntrustworthy as exc:
        payload["dynamic_refusal"] = str(exc)
        out.parent.mkdir(parents=True, exist_ok=True)
        out.write_text(json.dumps(payload, indent=2, sort_keys=True) + "\n")
        print(f"MAKE-MAP-REFUSED {out} {exc}", file=sys.stderr)
        return 4

    resolved, refused, unresolved, truncated = classify(parsed)
    payload.update({
        "declared": len(parsed["order"]),
        "targets": resolved,
        "refused": refused,
        "unresolved": unresolved,
        "includes": parsed["includes"],
        "optional_includes_missing": parsed["optional_missing"],
        "truncated": truncated,
    })
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_text(json.dumps(payload, indent=2, sort_keys=True) + "\n")
    print(f"MAKE-MAP {out} parser=static declared={len(parsed['order'])} "
          f"resolved={len(resolved)} refused={len(refused)} truncated={truncated}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
