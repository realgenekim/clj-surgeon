#!/usr/bin/env python3
"""Run the frozen refusal-handoff replacement pilot and unchanged cohort."""

from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path

import run_screen as base


SCREEN = "refusal-handoff"
ROOT = Path(__file__).resolve().parent
DIRECTORY = ROOT / "screens" / SCREEN
ADDENDUM = DIRECTORY / "ceiling-repair-addendum.md"
FREEZE = DIRECTORY / "ceiling-repair-freeze.json"
LADDER = [["f03", "f04"], ["f05", "f06"], ["f07", "f08"]]


def sha(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def require_repair_freeze() -> dict:
    base.require_freeze(SCREEN)
    row = json.loads(FREEZE.read_text(encoding="utf-8"))
    if row.get("status") != "frozen" or row.get("ladder") != LADDER:
        raise RuntimeError("repair freeze invalid")
    if row.get("addendum_sha256") != sha(ADDENDUM) or row.get("runner_sha256") != sha(Path(__file__).resolve()):
        raise RuntimeError("repair frozen-input drift")
    original = json.loads((DIRECTORY / "pilot.json").read_text(encoding="utf-8"))
    if original.get("status") != "ceiling" or original.get("structural_first") != 2:
        raise RuntimeError("repair requires the retained original 2/2 ceiling pilot")
    return row


def pilot() -> None:
    require_repair_freeze()
    receipt_path = DIRECTORY / "replacement-pilot.json"
    if receipt_path.exists():
        raise RuntimeError("replacement pilot is one-shot")
    candidates = []
    selected = None
    for index, pair in enumerate(LADDER, start=1):
        rows = [{"run": 1, "fixture": pair[0], "arm": "A"}, {"run": 2, "fixture": pair[1], "arm": "A"}]
        scores = base.run_rows(SCREEN, rows, DIRECTORY / "replacement-pilots" / f"candidate-{index}")
        structural = sum(row["primary_route"] == "structural" for row in scores)
        valid = all(row["environment_valid"] and row["semantic_correct"] and not row["wrong_subject"] for row in scores)
        candidate = {"candidate": index, "fixtures": pair, "control_runs": 2, "structural_first": structural, "valid": valid, "scores": scores}
        candidates.append(candidate)
        if structural < 2 and valid:
            selected = candidate
            break
    receipt = {"schema": "routing-tranche-replacement-pilot.v1", "screen": SCREEN, "status": "sub-ceiling" if selected else "no-sub-ceiling-pair", "selected_candidate": selected["candidate"] if selected else None, "selected_fixtures": selected["fixtures"] if selected else None, "candidates": candidates, "wrong_subject": sum(row["wrong_subject"] for candidate in candidates for row in candidate["scores"])}
    base.atomic_json(receipt_path, receipt)
    print(json.dumps(receipt, sort_keys=True))
    if not selected:
        raise RuntimeError("replacement ladder exhausted without sub-ceiling control")


def cohort() -> None:
    require_repair_freeze()
    receipt = json.loads((DIRECTORY / "replacement-pilot.json").read_text(encoding="utf-8"))
    if receipt.get("status") != "sub-ceiling" or receipt.get("wrong_subject") != 0:
        raise RuntimeError("valid replacement pilot required")
    root = DIRECTORY / "runs"
    if root.exists():
        raise RuntimeError("cohort is one-shot")
    scores = base.run_rows(SCREEN, base.schedule(), root)
    base.atomic_json(DIRECTORY / "attempts.json", scores)
    base.summarize(SCREEN)


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("command", choices=["pilot", "cohort"])
    args = parser.parse_args()
    pilot() if args.command == "pilot" else cohort()


if __name__ == "__main__":
    main()
