#!/usr/bin/env python3
"""Build (or check) the four B.4 arm prompts of the E3 pre-registration.

The prompts are NOT typed by hand.  They are extracted verbatim from the fenced
blocks of `docs/observations/2026-09-04-e3-e6-prestaged.md` (sections B.4.1 to
B.4.4) and, for the two rung-L arms, derived from
`docs/observations/2026-09-02-acid-rung-L/L-prompt-main.md` by the three edits
B.4.4 names -- each of which asserts that it actually matched, so a doc edit that
moves the text fails loudly instead of silently producing a different prompt.

    build-prompts.py            write prompts/*.md + prompts/*.sha256 + MANIFEST.sha256
    build-prompts.py --check    rebuild into a temp dir and diff; nonzero on drift

Exit codes: 0 ok, 2 extraction/derivation failure, 3 drift under --check.
"""
from __future__ import annotations

import argparse
import filecmp
import hashlib
import pathlib
import shutil
import subprocess
import sys
import tempfile

HERE = pathlib.Path(__file__).resolve().parent
REPO = HERE.parents[2]
DOC = REPO / "docs" / "observations" / "2026-09-04-e3-e6-prestaged.md"
L_PROMPT = REPO / "docs" / "observations" / "2026-09-02-acid-rung-L" / "L-prompt-main.md"

# The rung-L substitution named in A.8 ("two substitutions ... and no others").
FAN_TEST = "bin/fan-test"
KAOCHA_FOCUS = "bin/kaocha --focus marvin-voice-remote.bridge3-new-test"

NAMES = ["E3-P-N", "E3-P-T", "E3-L-N", "E3-L-T"]


class BuildError(RuntimeError):
    pass


def fences_after(doc: str, heading: str) -> list[str]:
    """Every fenced block that follows `heading`, in document order."""
    idx = doc.find(heading)
    if idx < 0:
        raise BuildError(f"heading not found in doc: {heading!r}")
    blocks: list[str] = []
    cur: list[str] | None = None
    for line in doc[idx:].split("\n"):
        if line.startswith("```"):
            if cur is None:
                cur = []
            else:
                blocks.append("\n".join(cur) + "\n")
                cur = None
            continue
        if cur is not None:
            cur.append(line)
    return blocks


def fence_after(doc: str, heading: str, which: int = 1) -> str:
    """Return the body of the `which`-th fenced block that follows `heading`."""
    blocks = fences_after(doc, heading)
    if len(blocks) < which:
        raise BuildError(f"fence #{which} not found after {heading!r}")
    return blocks[which - 1]


def require(condition: bool, message: str) -> None:
    if not condition:
        raise BuildError(message)


def derive_rung_l(l_prompt: str, ritual: str, tooling: str) -> str:
    """B.4.4: L-prompt-main.md with exactly three edits."""
    text = l_prompt

    # Edit 1 -- the withdrawn TURNS: instrument becomes TOOLCALLS:.
    old_item5 = (
        "5. a final line of exactly this form, with no other text on it:\n"
        "\n"
        "```\n"
        "TURNS: <n>\n"
        "```\n"
        "\n"
        "where `<n>` is the number of assistant turns you took in this task, "
        "counting this final one.\n"
    )
    require(old_item5 in text, "edit 1: Reporting item 5 (TURNS:) not found verbatim")
    text = text.replace(
        old_item5,
        "5. report your total tool-call count on the last line as  TOOLCALLS: <n>.\n",
    )
    # Edit 1, second half: the Ground rules line that mandates the same withdrawn
    # instrument.  B.4.4 names only "Reporting item 5", but leaving this line would
    # re-mandate TURNS: in the same prompt that just removed it.  Recorded as an
    # interpretive choice in bench/anvil-arms/README.md.
    old_ground = (
        "- End your final message with a line of the exact form `TURNS: <n>` "
        '— see "Reporting" below.\n'
    )
    require(old_ground in text, "edit 1b: Ground rules TURNS: line not found verbatim")
    text = text.replace(
        old_ground,
        "- End your final message with a line of the exact form `TOOLCALLS: <n>` "
        '— see "Reporting" below.\n',
    )
    require("TURNS:" not in text, "edit 1: a TURNS: mandate survived")

    # Edit 2 -- the A.8 ritual block immediately before `## Verify`, with the rung-L
    # test-command substitution.
    require(FAN_TEST in ritual, "edit 2: ritual block does not mention bin/fan-test")
    ritual_l = ritual.replace(FAN_TEST, KAOCHA_FOCUS)
    require(FAN_TEST not in ritual_l, "edit 2: substitution left a bin/fan-test behind")
    marker = "\n## Verify\n"
    require(text.count(marker) == 1, "edit 2: `## Verify` heading is not unique")
    text = text.replace(marker, "\n" + ritual_l + "\n## Verify\n")

    # Edit 3 -- append the arm's §5 TOOLING block.
    if not text.endswith("\n"):
        text += "\n"
    text += "\n" + tooling
    return text


def build(out: pathlib.Path) -> dict[str, str]:
    doc = DOC.read_text()
    l_prompt = L_PROMPT.read_text()

    shared_body = fence_after(doc, "### B.4.1 ")
    p_native = fence_after(doc, "### B.4.2 ")
    p_tool = fence_after(doc, "### B.4.3 ")
    ritual = fence_after(doc, "## A.8 ")
    l_native = fence_after(doc, "### B.4.4 ", which=1)
    l_tool = fence_after(doc, "### B.4.4 ", which=2)

    require("4. RITUAL" in shared_body, "B.4.1 body lost its §4 RITUAL section")
    require(shared_body.rstrip().endswith("TOOLCALLS: <n>."),
            "B.4.1 body does not end on the TOOLCALLS line")
    require(p_native.startswith("5. TOOLING"), "B.4.2 is not a §5 TOOLING block")
    require(p_tool.startswith("5. TOOLING"), "B.4.3 is not a §5 TOOLING block")
    require(l_native.startswith("5. TOOLING"), "B.4.4 native is not a §5 TOOLING block")
    require(l_tool.startswith("5. TOOLING"), "B.4.4 tool is not a §5 TOOLING block")
    require("alias_migration" in p_tool, "B.4.3 does not name alias_migration")
    require("require_change" in l_tool, "B.4.4 tool arm does not name require_change")
    require(ritual.startswith("4. RITUAL"), "A.8 block is not the RITUAL block")

    texts = {
        "E3-P-N": shared_body + "\n" + p_native,
        "E3-P-T": shared_body + "\n" + p_tool,
        "E3-L-N": derive_rung_l(l_prompt, ritual, l_native),
        "E3-L-T": derive_rung_l(l_prompt, ritual, l_tool),
    }

    out.mkdir(parents=True, exist_ok=True)
    digests = {}
    for name in NAMES:
        body = texts[name]
        require(body.strip(), f"{name}: built prompt is empty")
        (out / f"{name}.md").write_text(body)
        digest = hashlib.sha256(body.encode()).hexdigest()
        (out / f"{name}.sha256").write_text(digest + "\n")
        digests[name] = digest
    manifest = "".join(f"{digests[n]}  {n}.md\n" for n in NAMES)
    (out / "MANIFEST.sha256").write_text(manifest)
    return digests


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--out", default=str(HERE))
    ap.add_argument("--check", action="store_true")
    args = ap.parse_args()

    try:
        if not args.check:
            digests = build(pathlib.Path(args.out))
            for name in NAMES:
                print(f"{digests[name]}  {name}.md")
            return 0

        installed = pathlib.Path(args.out)
        with tempfile.TemporaryDirectory() as tmp:
            build(pathlib.Path(tmp))
            drift = []
            for name in NAMES:
                for suffix in (".md", ".sha256"):
                    a = installed / (name + suffix)
                    b = pathlib.Path(tmp) / (name + suffix)
                    if not a.exists() or not filecmp.cmp(a, b, shallow=False):
                        drift.append(name + suffix)
            a = installed / "MANIFEST.sha256"
            b = pathlib.Path(tmp) / "MANIFEST.sha256"
            if not a.exists() or not filecmp.cmp(a, b, shallow=False):
                drift.append("MANIFEST.sha256")
            if drift:
                print("PROMPT-DRIFT " + " ".join(sorted(drift)), file=sys.stderr)
                for name in sorted(drift):
                    if name.endswith(".md") and (installed / name).exists():
                        subprocess.run(
                            ["diff", "-u", str(installed / name),
                             str(pathlib.Path(tmp) / name)],
                            check=False,
                        )
                return 3
        print("prompts match the doc (4 files + manifest)")
        return 0
    except BuildError as exc:
        print(f"PROMPT-BUILD-FAILED {exc}", file=sys.stderr)
        return 2


if __name__ == "__main__":
    sys.exit(main())
