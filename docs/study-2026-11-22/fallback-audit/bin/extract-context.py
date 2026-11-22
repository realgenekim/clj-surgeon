#!/usr/bin/env python3
"""Extract bounded, unlabelled context using the original miner's parser.

Single process, stdlib only. Transcript text is never evaluated or executed.
The miner drops secret-bearing JSONL lines before parsing and redacts homes.
"""
import collections
import csv
import importlib.util
import json
from pathlib import Path
import random
import sys


AUDIT = Path(__file__).resolve().parents[1]
STUDY = AUDIT.parent
spec = importlib.util.spec_from_file_location(
    "study_session_miner", STUDY / "bin" / "mine-sessions.py")
miner = importlib.util.module_from_spec(spec)
sys.dont_write_bytecode = True
spec.loader.exec_module(miner)


def identity(row):
    return tuple(miner.one(row[field], 100000) for field in miner.CALL_FIELDS)


def excerpt(value, limit):
    return miner.one(value, limit)


def context(action, following):
    return {
        "tool_name": miner.safe(action["name"]),
        "input": excerpt(miner.compact(action["arg"]), 400),
        "output": excerpt(action["out"], 600),
        "output_attribution": "unavailable: multi-call wrapper" if action.get("ambiguous_output") else "miner attachment",
        "next_six": [
            {
                "tool_name": miner.safe(a["name"]),
                "arguments": excerpt(a["cmd"] or miner.compact(a["arg"]), 200),
                "output": excerpt(a["out"], 160),
                "surgeon": bool(a["surface"]),
                "output_attribution": "unavailable: multi-call wrapper" if a.get("ambiguous_output") else "miner attachment",
            }
            for a in following
        ],
        "another_surgeon": any(a["surface"] for a in following),
    }


class Extractor(miner.Miner):
    def __init__(self, wanted):
        super().__init__(AUDIT)
        self.wanted = wanted
        self.found = {}

    def finish_turn(self, actions, store, session, session_key):
        # Delegate turn IDs, row shapes, operation detection and output joins.
        super().finish_turn(actions, store, session, session_key)
        calls = [(i, a) for i, a in enumerate(actions) for _ in a["ops"]]
        assert len(calls) == len(self.rows)
        for row, (index, action) in zip(self.rows, calls):
            key = identity(row)
            if key in self.wanted:
                extracted = context(action, actions[index + 1:index + 7])
                if key in self.found and self.found[key] != extracted:
                    raise RuntimeError("Selected row has conflicting matching calls")
                self.found[key] = extracted
        self.rows.clear()
        self.controls.clear()
        self.native.clear()


def main():
    with (STUDY / "calls.tsv").open(encoding="utf-8", newline="") as handle:
        # The miner writes literal tab-separated fields without CSV quoting.
        rows = list(csv.DictReader(handle, delimiter="\t", quoting=csv.QUOTE_NONE))
    selected = [r for r in rows if r["op"] in (
        "split-ns!", "alias_migration", "alias-migration")]
    selected += random.Random(20261122).sample(
        [r for r in rows if r["op"] == "cat"], 60)
    wanted = {identity(r) for r in selected}
    extractor = Extractor(wanted)
    roots = {"codex": miner.HOME / ".codex/sessions",
             "claude": miner.HOME / ".claude/projects"}
    for store, root in roots.items():
        names = {r["session"] for r in selected if r["store"] == store}
        paths = collections.defaultdict(list)
        for path in sorted(root.glob("**/*.jsonl")):
            if path.name in names:
                paths[path.name].append(path)
        for name in sorted(names):
            if not paths[name]:
                raise RuntimeError(f"Missing session: {store} {name}")
            # Full row identity includes the miner's absolute-path session hash.
            for path in paths[name]:
                extractor.scan(path, store)
    missing = wanted - extractor.found.keys()
    if missing:
        raise RuntimeError(f"Unmatched selected rows: {len(missing)}")

    fields = miner.CALL_FIELDS + ["tool_name", "input", "output",
        "output_attribution", "next_six", "another_surgeon", "label"]
    markdown = ["# Extracted call context", "",
        "Selection: all split-ns! and alias migration rows, followed by 60 cat rows",
        "sampled in calls.tsv order with random.Random(20261122).sample.", "",
        "Next calls follow the miner's unwrapped action order within each turn.",
        "Secret-bearing transcript lines are dropped before parsing; home paths are redacted.",
        "Unavailable multi-call wrapper outputs stay empty, as in the miner.", ""]
    output_rows = []
    for number, row in enumerate(selected, 1):
        data = extractor.found[identity(row)]
        output_rows.append(dict(row, **data, label=""))
        markdown.extend([
            f'## {number}. {row["op"]} {row["outcome"]} {row["date"]} {row["store"]} {Path(row["session"]).name} turn {row["turn_id"]}',
            "",
            "- Tool: " + miner.compact(data["tool_name"]),
            "- Input: " + miner.compact(data["input"]),
            "- Output: " + miner.compact(data["output"]),
            "- Output attribution: " + data["output_attribution"],
        ])
        for i, call in enumerate(data["next_six"], 1):
            markdown.append(f"- Next {i}: " + miner.compact(call))
        if not data["next_six"]:
            markdown.append("- Next calls: none (turn ended)")
        markdown.extend([
            "- Another surgeon call in next six: " + str(data["another_surgeon"]).lower(),
            "", "label: ", ""])
    with (AUDIT / "rows.tsv").open("w", encoding="utf-8", newline="") as handle:
        writer = csv.DictWriter(handle, fields, delimiter="\t", lineterminator="\n")
        writer.writeheader()
        for row in output_rows:
            writer.writerow(dict(row, next_six=miner.compact(row["next_six"])))
    (AUDIT / "rows.md").write_text("\n".join(markdown), encoding="utf-8")
    print("Selected rows per op: " + miner.compact(dict(sorted(
        collections.Counter(r["op"] for r in selected).items()))))
    for path in (Path(__file__).resolve(), AUDIT / "rows.md", AUDIT / "rows.tsv"):
        print("Written: " + str(path.relative_to(STUDY.parent.parent)))


if __name__ == "__main__":
    main()
