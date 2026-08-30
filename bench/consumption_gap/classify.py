#!/usr/bin/env python3
"""Classify privacy-safe owner-refusal recovery episodes.

The input is a canonical, deidentified JSON array.  This program intentionally
does not read agent history itself: the bounded telemetry adapter must project
only the registered fields below before this classifier is run.
"""

from __future__ import annotations

import argparse
import hashlib
import json
from collections import Counter, defaultdict
from pathlib import Path
from typing import Any


CLASSIFICATIONS = (
    "duplicate-name-disambiguation",
    "location-beyond-cap",
    "same-owner-names-only",
    "other",
    "unclassifiable",
)


def canonical_sha256(value: Any) -> str:
    encoded = json.dumps(value, sort_keys=True, separators=(",", ":")).encode()
    return hashlib.sha256(encoded).hexdigest()


def _row_key(row: dict[str, Any]) -> tuple[str, str]:
    return str(row.get("owner", "")), str(row.get("locator", ""))


def classify_episode(episode: dict[str, Any]) -> dict[str, Any]:
    refusal = episode.get("refusal") or {}
    reread = episode.get("recovery_read") or {}
    owner_names = {str(name) for name in refusal.get("owner_names", [])}
    reread_names = {str(name) for name in reread.get("owner_names", [])}
    kinds = {str(kind) for kind in reread.get("semantic_kinds", [])}

    required = refusal.get("required_selector")
    answer = refusal.get("answer_token")
    unique = refusal.get("answer_unique")
    if required is None or answer is None or unique is None:
        consumability = "indeterminate"
    elif str(required) == str(answer) and unique is True:
        consumability = "yes"
    else:
        consumability = "no"

    core_complete = (
        episode.get("reread") is True
        and refusal.get("owner_names_complete") is True
        and bool(owner_names)
        and reread.get("evidence_complete") is True
    )
    if not core_complete:
        classification = "unclassifiable"
    else:
        duplicate = bool(reread.get("resolved_duplicate"))
        for group in reread.get("duplicate_groups", []):
            locators = {str(value) for value in group.get("candidate_locators", [])}
            if str(group.get("name", "")) in owner_names and len(locators) >= 2:
                duplicate = True

        refusal_rows = {_row_key(row) for row in refusal.get("location_rows", [])}
        refusal_row_owners = {owner for owner, _ in refusal_rows}
        new_omitted_location = any(
            _row_key(row) not in refusal_rows
            and str(row.get("owner", "")) in owner_names
            and str(row.get("owner", "")) not in refusal_row_owners
            for row in reread.get("location_rows", [])
        )

        if duplicate:
            classification = "duplicate-name-disambiguation"
        elif refusal.get("location_rows_capped") is True and new_omitted_location:
            classification = "location-beyond-cap"
        elif kinds and kinds <= {"owner-name"} and reread_names and reread_names <= owner_names:
            classification = "same-owner-names-only"
        else:
            classification = "other"

    return {
        "episode_id": str(episode.get("episode_id", "")),
        "caller_model": str(episode.get("caller_model") or "unknown"),
        "classification": classification,
        "verbatim_consumable": consumability,
        "input_sha256": canonical_sha256(episode),
    }


def summarize(episodes: list[dict[str, Any]]) -> dict[str, Any]:
    classified = [classify_episode(episode) for episode in episodes]
    counts = Counter(row["classification"] for row in classified)
    consumability = Counter(row["verbatim_consumable"] for row in classified)
    by_model: dict[str, Counter[str]] = defaultdict(Counter)
    by_consumability: dict[str, Counter[str]] = defaultdict(Counter)
    for row in classified:
        by_model[row["caller_model"]][row["classification"]] += 1
        by_consumability[row["verbatim_consumable"]][row["classification"]] += 1

    total = len(classified)
    classifiable = total - counts["unclassifiable"]
    location_support = (
        counts["duplicate-name-disambiguation"] + counts["location-beyond-cap"]
    )
    habit_support = counts["same-owner-names-only"]
    habit_high_confidence = sum(
        1
        for row in classified
        if row["classification"] == "same-owner-names-only"
        and row["verbatim_consumable"] == "yes"
    )
    other = counts["other"]

    def share(value: int) -> float | None:
        return value / classifiable if classifiable else None

    coverage = classifiable / total if total else 0.0
    loc_share = share(location_support)
    habit_share = share(habit_high_confidence)
    other_share = share(other)

    if total != 119:
        verdict = "population-mismatch"
        phase2 = False
    elif coverage < 0.90:
        verdict = "instrumentation-repair-required"
        phase2 = False
    elif other_share is not None and other_share >= 0.50:
        verdict = "other-information-dominant"
        phase2 = False
    elif loc_share is not None and habit_share is not None and loc_share >= 0.80 and habit_share <= 0.10:
        verdict = "H-LOC-dominant"
        phase2 = False
    elif loc_share is not None and habit_share is not None and habit_share >= 0.80 and loc_share <= 0.10:
        verdict = "H-HABIT-dominant"
        phase2 = False
    elif location_support >= 8 and habit_high_confidence >= 8:
        verdict = "both-hypotheses-alive"
        phase2 = True
    elif location_support >= 8:
        verdict = "H-LOC-only-material-support"
        phase2 = False
    elif habit_high_confidence >= 8:
        verdict = "H-HABIT-only-material-support"
        phase2 = False
    else:
        verdict = "neither-hypothesis-materially-supported"
        phase2 = False

    return {
        "schema": "consumption-gap-classification.v1",
        "input_sha256": canonical_sha256(episodes),
        "population": total,
        "classifiable": classifiable,
        "coverage": coverage,
        "counts": {name: counts[name] for name in CLASSIFICATIONS},
        "verbatim_consumability": dict(sorted(consumability.items())),
        "by_model": {
            model: dict(sorted(values.items())) for model, values in sorted(by_model.items())
        },
        "by_verbatim_consumability": {
            state: dict(sorted(values.items()))
            for state, values in sorted(by_consumability.items())
        },
        "hypothesis_support": {
            "H-LOC": location_support,
            "H-HABIT": habit_support,
            "H-HABIT-high-confidence-verbatim": habit_high_confidence,
            "other": other,
        },
        "verdict": verdict,
        "phase2_required": phase2,
        "episodes": classified,
    }


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("episodes", type=Path)
    parser.add_argument("--out", type=Path)
    args = parser.parse_args()
    episodes = json.loads(args.episodes.read_text())
    if not isinstance(episodes, list):
        raise SystemExit("episodes must be one JSON array")
    result = summarize(episodes)
    rendered = json.dumps(result, indent=2, sort_keys=True) + "\n"
    if args.out:
        args.out.write_text(rendered)
    else:
        print(rendered, end="")


if __name__ == "__main__":
    main()
