#!/usr/bin/env python3
"""Build (or check) the four B.4 arm prompts of the E3 pre-registration.

The prompts are NOT typed by hand.  They are extracted verbatim from the fenced
blocks of `docs/observations/2026-09-04-e3-e6-prestaged.md` (sections B.4.1 to
B.4.4) -- from BOUNDED sections, each ending at the next heading of its level, so a
lookup can never reach into the section after it -- and, for the two rung-L arms,
derived from
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
import re
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

# Every section whose PROSE governs what the prompts are or how they are derived.
# Its hash goes in the manifest, so a prose edit fails --check even when every
# fence, every prompt and every prompt hash is byte-identical.
GOVERNING_SECTIONS = [
    ("A.8",   "## A.8 "),
    ("B.4.1", "### B.4.1 "),
    ("B.4.2", "### B.4.2 "),
    ("B.4.3", "### B.4.3 "),
    ("B.4.4", "### B.4.4 "),
]


class BuildError(RuntimeError):
    pass


HEADING_RE = re.compile(r"^(#{1,6})\s")


def section(doc: str, heading: str) -> str:
    """The text of `heading`'s section, BOUNDED BY THE NEXT HEADING of its level.

    Sol, item 9: the old lookup ran from a loose heading match to END OF FILE, so a
    section could silently borrow a fence -- or a whole meaning -- from the sections
    after it, and nothing downstream could tell.  A section is bounded or it is not
    a section.
    """
    idx = doc.find(heading)
    if idx < 0:
        raise BuildError(f"heading not found in doc: {heading!r}")
    if doc.find(heading, idx + 1) >= 0:
        raise BuildError(f"heading is not unique in doc: {heading!r}")
    level = len(HEADING_RE.match(heading).group(1))
    lines = doc[idx:].split("\n")
    out = [lines[0]]
    for line in lines[1:]:
        match = HEADING_RE.match(line)
        if match and len(match.group(1)) <= level:
            break
        out.append(line)
    return "\n".join(out)


def fences_in(text: str) -> list[str]:
    """Every fenced block inside `text`, in order.  An unterminated fence is an error."""
    blocks: list[str] = []
    cur: list[str] | None = None
    for line in text.split("\n"):
        if line.startswith("```"):
            if cur is None:
                cur = []
            else:
                blocks.append("\n".join(cur) + "\n")
                cur = None
            continue
        if cur is not None:
            cur.append(line)
    if cur is not None:
        raise BuildError("unterminated fence inside a bounded section")
    return blocks


def fence_in_section(doc: str, heading: str, which: int = 1) -> str:
    """The `which`-th fenced block THIS SECTION OWNS.  Never the next section's."""
    blocks = fences_in(section(doc, heading))
    if len(blocks) < which:
        raise BuildError(
            f"fence #{which} not found inside section {heading!r} "
            f"(it owns {len(blocks)}); the lookup is bounded and will not "
            f"reach into the following section")
    return blocks[which - 1]


def prose_of(text: str) -> str:
    """A section with its fenced blocks removed: the GOVERNING PROSE.

    The fences say what the prompt is; the prose says what the prompt MEANS and how
    it is derived ("exactly three edits").  Both are the source contract, so both are
    hashed into the manifest -- otherwise a prose edit changes the instruction and
    leaves every hash identical.
    """
    kept: list[str] = []
    inside = False
    for line in text.split("\n"):
        if line.startswith("```"):
            inside = not inside
            continue
        if not inside:
            kept.append(line.rstrip())
    return "\n".join(kept).strip() + "\n"


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


def build(out: pathlib.Path, doc_path: pathlib.Path | None = None,
          l_prompt_path: pathlib.Path | None = None) -> dict[str, str]:
    doc = (doc_path or DOC).read_text()
    l_prompt = (l_prompt_path or L_PROMPT).read_text()

    shared_body = fence_in_section(doc, "### B.4.1 ")
    p_native = fence_in_section(doc, "### B.4.2 ")
    p_tool = fence_in_section(doc, "### B.4.3 ")
    ritual = fence_in_section(doc, "## A.8 ")
    l_native = fence_in_section(doc, "### B.4.4 ", which=1)
    l_tool = fence_in_section(doc, "### B.4.4 ", which=2)
    require(len(fences_in(section(doc, "### B.4.4 "))) == 2,
            "B.4.4 must own exactly two fences (the two §5 TOOLING blocks)")

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
    for label, heading in GOVERNING_SECTIONS:
        prose = prose_of(section(doc, heading))
        digest = hashlib.sha256(prose.encode()).hexdigest()
        digests[f"section:{label}"] = digest
        manifest += f"{digest}  section:{label}\n"
    (out / "MANIFEST.sha256").write_text(manifest)
    return digests


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--out", default=str(HERE))
    ap.add_argument("--doc", default=str(DOC),
                    help="the pre-registration doc the prompts are extracted from")
    ap.add_argument("--l-prompt", default=str(L_PROMPT))
    ap.add_argument("--check-dir", default=str(HERE / ".check"),
                    help="where --check builds its comparison copy (under the "
                         "apparatus, never an ambient system temp dir)")
    ap.add_argument("--check", action="store_true")
    args = ap.parse_args()
    doc_path = pathlib.Path(args.doc)
    l_prompt_path = pathlib.Path(args.l_prompt)

    try:
        if not args.check:
            digests = build(pathlib.Path(args.out), doc_path, l_prompt_path)
            for name in NAMES:
                print(f"{digests[name]}  {name}.md")
            return 0

        installed = pathlib.Path(args.out)
        # Build under the apparatus, not into an ambient system temp dir, and NAME the
        # directory used (Sol, item 12).  A tool that will not say where it wrote
        # cannot be audited for where it wrote.
        check_base = pathlib.Path(args.check_dir)
        check_base.mkdir(parents=True, exist_ok=True)
        tmp = tempfile.mkdtemp(prefix="check-", dir=str(check_base))
        print(f"check-dir: {tmp}")
        try:
            build(pathlib.Path(tmp), doc_path, l_prompt_path)
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
        finally:
            shutil.rmtree(tmp, ignore_errors=True)
        print(f"prompts match {doc_path} (4 files + manifest with "
              f"{len(GOVERNING_SECTIONS)} governing-prose hashes)")
        return 0
    except BuildError as exc:
        print(f"PROMPT-BUILD-FAILED {exc}", file=sys.stderr)
        return 2


if __name__ == "__main__":
    sys.exit(main())
