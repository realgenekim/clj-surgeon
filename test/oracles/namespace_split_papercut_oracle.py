#!/usr/bin/env python3
"""papercut-oracle.py — grade the OUTPUT of a Clojure namespace split, in seconds.

    usage:  papercut-oracle.py <worktree-root> [--json]

WHAT THIS IS
------------
A read-only, no-JVM, no-test grader for a *post-split* checkout of
curtaincall-cfp, in which the monolith

    src/cfp_scheduler_killer/views.clj

was split into

    src/cfp_scheduler_killer/views/*.clj                 ("destinations")

and its callers were rewritten

    src/cfp_scheduler_killer/server.clj                  ("callers")
    test/cfp_scheduler_killer/*_test.clj                 ("callers")

It answers one question only: *does the result read like the host codebase, or
like a machine dropped code into new files?*  It says nothing about
correctness — that is what the suite is for.  Every check is a "paper cut":
something a careful human reviewer would ask you to fix before merge, and that
no compiler or test run will ever complain about.

The tool never mutates the graded worktree. Baseline lint uses a temporary
mirror under /var/tmp, deleted after the analyzer returns.  The
pre-split baseline is read with `git show HEAD:<path>` out of the worktree's own
object database; the working tree is only ever read.

EXIT / OUTPUT
-------------
Prints one named block per check with a count and up to three `file:line`
examples, then a compact table, then a final line `PAPERCUTS: <total>`.
Exit 1 if total > 0, else 0.  `--json` additionally dumps the full machine
record (all offenders, not just the first three) to stdout after the table.

THE CHECKS
----------
1. docstring-clone  [COUNTED]
   The ns docstring of each destination, taken as the *raw source bytes* of the
   first string literal after the ns name.  Any docstring byte-identical across
   >= 2 destinations is a clone; the count is the number of FILES carrying a
   cloned docstring.  This catches the commonest split paper cut: copying the
   monolith's header prose into all N children so every file claims to be the
   whole subsystem.

2. clj-kondo  [unused-import / unused-namespace / unused-referred-var /
   unresolved-symbol COUNTED; unresolved-namespace ADVISORY]
   Runs `~/bin/clj-kondo --lint <views dir> <server.clj> <test dir>
   --config '{:output {:format :json}}'` with cwd = <root> and buckets the
   findings by type. Exact file/type/message multiplicities already present at
   HEAD are advisory; new findings still count, even in previously dirty files.
   Analyzer or baseline capture failure counts as an oracle failure.  `unresolved-namespace` is reported but not counted: in
   this repo it is pre-existing (store/* refs the linter cannot see).

3. continuation-misalignment  [COUNTED]
   THE HEURISTIC, stated plainly, because it is approximate on purpose:

     A split renames a call's head — `header` becomes `layout/header`,
     `views/new-event-page` becomes `event-setup/new-event-page`.  The head gets
     LONGER.  Any continuation line that was hand-aligned under the call's first
     argument is now short by exactly `len(new head) - len(old head)`, and
     neither the compiler nor the tests care.  So:

       - find every multi-line form whose opening line is
         `(<alias>/<name> <first-arg...`  (an inline first argument),
         where <alias> resolves, via this file's own ns :require block, to
         `cfp-scheduler-killer.views` or `cfp-scheduler-killer.views.*`;
       - reconstruct the OLD head: bare `<name>` inside a destination (the call
         was intra-file in views.clj), `views/<name>` inside a caller;
       - require `<name>` to be a real top-level def of the pre-split
         views.clj (so freshly-invented helpers are not judged);
       - delta = len(new head) - len(old head);  delta == 0 => skip;
       - new-col = column of the inline first argument;  old-col = new-col - delta;
       - count every non-blank line inside that form (up to its matching close
         paren) whose indent is EXACTLY old-col.

     Parens are counted on a mask in which string literals, `;` comments, regex
     literals, and character literals have been blanked, so `"(" ` does not
     confuse the scan.  Lines are de-duplicated per file, so one line is one
     paper cut no matter how many enclosing forms flag it.

     FALSE POSITIVES ARE ACCEPTED AND REPORTED HONESTLY.  A line that merely
     happens to sit at old-col for unrelated reasons is counted.  In practice
     the signal dominates because old-col is an unusual, deep column.  Read the
     examples before believing a number.

4. require-layout  [COUNTED, three sub-counters]
   a. `require-layout-dest`   — per destination: are the :require entries one
      block in sorted order, and does the block's continuation indent match the
      pre-split host monolith's (`git show HEAD:src/cfp_scheduler_killer/views.clj`)?
      Files failing either are counted once.
   b. `require-layout-caller` — per caller: the new list of require entries must
      equal the PRE-SPLIT list with the single `cfp-scheduler-killer.views` entry
      replaced, IN PLACE, by an internally sorted block of the new
      `cfp-scheduler-killer.views.*` entries, at the host's own indent.  Files
      failing are counted once, with the reason(s) named:
      not-in-place / block-not-sorted / other-entries-reordered / indent-differs.
   c. `ns-whitespace-line`    — any blank or whitespace-only line inside the ns
      form of a destination or a caller.  Counted per line.  The host has none.

5. stale-prose  [ADVISORY, not counted]
   Occurrences of the regex \b views/[a-z-] (no spaces) inside comments and string literals across
   the graded file set: documentation still pointing at a namespace that no
   longer exists.

6. tracked-log-churn  [ADVISORY, not counted]
   `git status --porcelain -- 00SERVER-LOGS.txt`: a tracked server log that the
   split dirtied.

WHAT IS COUNTED IN `PAPERCUTS`
------------------------------
   docstring-clone + unused-import + unused-namespace + unused-referred-var
   + unresolved-symbol + continuation-misalignment
   + require-layout-dest + require-layout-caller + ns-whitespace-line
"""

import json
import os
import re
import subprocess
import sys
import tempfile
from collections import Counter
from collections import Counter, OrderedDict

KONDO = os.path.expanduser("~/bin/clj-kondo")

VIEWS_DIR_REL = "src/cfp_scheduler_killer/views"
SERVER_REL = "src/cfp_scheduler_killer/server.clj"
TEST_DIR_REL = "test/cfp_scheduler_killer"
MONOLITH_REL = "src/cfp_scheduler_killer/views.clj"
VIEWS_NS_PREFIX = "cfp-scheduler-killer.views"

COUNTED_KONDO = ["unused-import", "unused-namespace", "unused-referred-var",
                 "unresolved-symbol"]
ADVISORY_KONDO = ["unresolved-namespace"]


# --------------------------------------------------------------------------
# git (read-only)
# --------------------------------------------------------------------------

def git(root, *args):
    p = subprocess.run(["git", "-C", root] + list(args),
                       capture_output=True, text=True)
    return p.returncode, p.stdout, p.stderr


def git_show(root, path, rev="HEAD"):
    rc, out, _ = git(root, "show", "%s:%s" % (rev, path))
    return out if rc == 0 else None


# --------------------------------------------------------------------------
# Clojure surface scanner: blank out strings / comments / char+regex literals
# --------------------------------------------------------------------------

def scan(text):
    """Return (mask, spans) where mask has every non-code char replaced by a
    space (newlines preserved, so offsets and columns are identical to text),
    and spans is a list of (kind, start, end) for kind in {'string','comment'}.
    """
    mask = list(text)
    spans = []
    i, n = 0, len(text)
    while i < n:
        c = text[i]
        if c == '\\':                              # character literal: \a \( \newline
            j = i + 1
            if j < n:
                j += 1
                while j < n and (text[j].isalnum() or text[j] == '-'):
                    j += 1
            for k in range(i, min(j, n)):
                if mask[k] != '\n':
                    mask[k] = ' '
            i = j
            continue
        if c == ';':
            j = text.find('\n', i)
            if j == -1:
                j = n
            spans.append(('comment', i, j))
            for k in range(i, j):
                mask[k] = ' '
            i = j
            continue
        if c == '"':
            j = i + 1
            while j < n:
                if text[j] == '\\':
                    j += 2
                    continue
                if text[j] == '"':
                    j += 1
                    break
                j += 1
            spans.append(('string', i, min(j, n)))
            for k in range(i, min(j, n)):
                if mask[k] != '\n':
                    mask[k] = ' '
            i = j
            continue
        i += 1
    return ''.join(mask), spans


def match_paren(mask, open_idx):
    """Index of the paren matching mask[open_idx], or -1."""
    depth = 0
    for i in range(open_idx, len(mask)):
        c = mask[i]
        if c in '([{':
            depth += 1
        elif c in ')]}':
            depth -= 1
            if depth == 0:
                return i
    return -1


def line_starts(text):
    starts = [0]
    for i, c in enumerate(text):
        if c == '\n':
            starts.append(i + 1)
    return starts


def idx_to_line(starts, idx):
    lo, hi = 0, len(starts) - 1
    while lo < hi:
        mid = (lo + hi + 1) // 2
        if starts[mid] <= idx:
            lo = mid
        else:
            hi = mid - 1
    return lo  # 0-based


# --------------------------------------------------------------------------
# ns-form parsing
# --------------------------------------------------------------------------

SYMCH = r"[A-Za-z0-9*+!_'?<>=&%|.$-]"


def parse_ns(text):
    """Parse the leading (ns ...) form. Returns a dict or None."""
    mask, spans = scan(text)
    m = re.search(r"^\(ns\s", mask, re.M)
    if not m:
        return None
    start = m.start()
    end = match_paren(mask, start)
    if end == -1:
        end = len(mask) - 1
    starts = line_starts(text)
    body = text[start:end + 1]
    mbody = mask[start:end + 1]

    nm = re.match(r"\(ns\s+(" + SYMCH + r"+)", body)
    ns_name = nm.group(1) if nm else None

    # ns docstring: first string literal after the ns name, before anything else
    doc = None
    if nm:
        # NOTE: the mask blanks the quotes too, so whitespace must be skipped on
        # the RAW text, not on the mask.
        k = start + nm.end()
        while k < len(text) and text[k] in " \t\n":
            k += 1
        if k < len(text) and text[k] == '"':
            for kind, s_, e_ in spans:
                if kind == 'string' and s_ == k:
                    doc = text[s_:e_]
                    break

    # :require sub-form
    req = None
    rm = re.search(r"\(:require\b", mbody)
    if rm:
        rstart = start + rm.start()
        rend = match_paren(mask, rstart)
        if rend == -1:
            rend = end
        entries = []
        i = rstart + len("(:require")
        while i < rend:
            ch = mask[i]
            if ch in " \t\n":
                i += 1
                continue
            if ch == '[':
                j = match_paren(mask, i)
                if j == -1:
                    break
                raw = text[i:j + 1]
                em = re.match(r"\[\s*(" + SYMCH + r"+)", raw)
                nsym = em.group(1) if em else None
                am = re.search(r":as\s+(" + SYMCH + r"+)", raw)
                alias = am.group(1) if am else None
                refers = re.search(r":refer\s+\[([^\]]*)\]", raw)
                entries.append({
                    "ns": nsym, "alias": alias,
                    "refer": (refers.group(1).split() if refers else []),
                    "idx": i,
                    "line": idx_to_line(starts, i) + 1,
                    "col": i - starts[idx_to_line(starts, i)],
                })
                i = j + 1
                continue
            if ch == ';':
                i = mask.find('\n', i)
                if i == -1:
                    break
                continue
            # bare symbol require
            em = re.match(SYMCH + r"+", mask[i:rend])
            if em:
                entries.append({
                    "ns": em.group(0), "alias": None, "refer": [],
                    "idx": i,
                    "line": idx_to_line(starts, i) + 1,
                    "col": i - starts[idx_to_line(starts, i)],
                })
                i += em.end()
                continue
            i += 1
        rline = idx_to_line(starts, rstart)
        cont = [e["col"] for e in entries if idx_to_line(starts, e["idx"]) != rline]
        indent = Counter(cont).most_common(1)[0][0] if cont else None
        req = {"entries": entries, "indent": indent,
               "line": rline + 1, "start": rstart, "end": rend}

    # blank / whitespace-only lines inside the ns form, EXCLUDING lines that
    # fall inside a string literal (a blank line inside the ns docstring is
    # prose, not a paper cut).
    first_line = idx_to_line(starts, start)
    last_line = idx_to_line(starts, end)
    lines = text.split('\n')
    in_string = set()
    for kind, s_, e_ in spans:
        if kind != 'string':
            continue
        for ln in range(idx_to_line(starts, s_), idx_to_line(starts, max(s_, e_ - 1)) + 1):
            in_string.add(ln)
    blanks = [i + 1 for i in range(first_line, min(last_line, len(lines) - 1))
              if lines[i].strip() == '' and i not in in_string]

    return {"ns": ns_name, "doc": doc, "require": req,
            "blank_lines": blanks, "start": start, "end": end,
            "mask": mask, "spans": spans, "starts": starts}


def alias_map(nsinfo):
    out = {}
    if not nsinfo or not nsinfo["require"]:
        return out
    for e in nsinfo["require"]["entries"]:
        if e["alias"] and e["ns"]:
            out[e["alias"]] = e["ns"]
    return out


def is_views_ns(n):
    return n == VIEWS_NS_PREFIX or (n or "").startswith(VIEWS_NS_PREFIX + ".")


# --------------------------------------------------------------------------
# check 3: continuation misalignment
# --------------------------------------------------------------------------

HEAD_RE = re.compile(r"\((" + SYMCH.replace(".", "") + r"+)/(" + SYMCH + r"+)")


def top_level_defs(monolith_text):
    names = set()
    for m in re.finditer(
            r"^\(def[A-Za-z-]*\s+((?:\^(?:\{[^}]*\}|" + SYMCH + r"+|:" + SYMCH +
            r"+)\s+)*)([^\s()\[\]{}\"';]+)", monolith_text, re.M):
        names.add(m.group(2))
    return names


def misalignments(text, nsinfo, amap, defs, is_dest):
    mask = nsinfo["mask"]
    starts = nsinfo["starts"]
    lines = text.split('\n')
    hits = OrderedDict()          # line-no -> detail
    for m in HEAD_RE.finditer(mask):
        alias, name = m.group(1), m.group(2)
        target = amap.get(alias)
        if not is_views_ns(target):
            continue
        if name not in defs:
            continue
        open_idx = m.start()
        after = m.end()
        # inline first argument on the same line?
        k = after
        while k < len(mask) and mask[k] in " \t":
            k += 1
        if k >= len(mask) or mask[k] == '\n' or k == after:
            continue
        head_line = idx_to_line(starts, open_idx)
        if idx_to_line(starts, k) != head_line:
            continue
        close = match_paren(mask, open_idx)
        if close == -1:
            continue
        end_line = idx_to_line(starts, close)
        if end_line == head_line:
            continue
        new_head = "%s/%s" % (alias, name)
        old_head = name if is_dest else "views/%s" % name
        delta = len(new_head) - len(old_head)
        if delta == 0:
            continue
        new_col = k - starts[head_line]
        old_col = new_col - delta
        if old_col < 0 or old_col == new_col:
            continue
        for ln in range(head_line + 1, end_line + 1):
            if ln >= len(lines):
                break
            raw = lines[ln]
            if raw.strip() == '':
                continue
            indent = len(raw) - len(raw.lstrip())
            if indent == old_col:
                hits.setdefault(ln + 1, {
                    "line": ln + 1, "old_head": old_head, "new_head": new_head,
                    "old_col": old_col, "new_col": new_col,
                    "text": raw.rstrip()[:90],
                })
    return list(hits.values())


# --------------------------------------------------------------------------
# clj-kondo
# --------------------------------------------------------------------------

def run_kondo(root, baseline=False):
    paths = [os.path.join(root, MONOLITH_REL if baseline else VIEWS_DIR_REL),
             os.path.join(root, SERVER_REL),
             os.path.join(root, TEST_DIR_REL)]
    cmd = [KONDO, "--lint"] + paths + ["--config", '{:output {:format :json}}']
    try:
        p = subprocess.run(cmd, capture_output=True, text=True, cwd=root)
    except FileNotFoundError:
        return None, "clj-kondo not found at %s" % KONDO
    try:
        data = json.loads(p.stdout)
    except Exception as e:
        return None, "clj-kondo JSON parse failed: %s / %s" % (e, p.stderr[:200])
    out = []
    for f in data.get("findings", []):
        fn = f.get("filename", "")
        if os.path.isabs(fn):
            fn = os.path.relpath(fn, root)
        out.append({"type": f.get("type"), "file": fn, "line": f.get("row"),
                    "col": f.get("col"), "message": f.get("message", "")})
    return out, None


def baseline_kondo(root):
    """Same analyzer/config on HEAD bytes; compare diagnostic multiplicities,
    never blanket-exclude a file merely because some of it was untouched."""
    rc, paths, err = git(root, "ls-tree", "-r", "--name-only", "HEAD", "--",
                         "src", "test", ".clj-kondo")
    if rc:
        return None, err
    with tempfile.TemporaryDirectory(prefix="split-oracle-", dir="/var/tmp") as mirror:
        for path in paths.splitlines():
            content = git_show(root, path)
            if content is None:
                return None, "cannot read baseline " + path
            target = os.path.join(mirror, path)
            os.makedirs(os.path.dirname(target), exist_ok=True)
            with open(target, "w") as f:
                f.write(content)
        return run_kondo(mirror, baseline=True)


# INTENT: NS-SPLIT-031
def partition_baseline(findings, baseline):
    key = lambda f: (f["file"], f["type"], f["message"])
    available = Counter(key(f) for f in baseline)
    new, existing = [], []
    for f in findings:
        k = key(f)
        if available[k]:
            existing.append(f)
            available[k] -= 1
        else:
            new.append(f)
    return new, existing


# --------------------------------------------------------------------------
# main
# --------------------------------------------------------------------------

def read(p):
    with open(p, encoding="utf-8") as fh:
        return fh.read()


def rel(root, p):
    return os.path.relpath(p, root)


def main(argv):
    if len(argv) < 2:
        sys.stderr.write(__doc__.split("\n\n")[1] + "\n")
        return 2
    root = os.path.abspath(argv[1])
    want_json = "--json" in argv[2:]
    if not os.path.isdir(os.path.join(root, ".git")) and not os.path.exists(os.path.join(root, ".git")):
        sys.stderr.write("not a git worktree: %s\n" % root)
        return 2

    R = {"root": root, "checks": OrderedDict()}
    out = []

    def say(s=""):
        out.append(s)

    # ---- file sets -------------------------------------------------------
    vdir = os.path.join(root, VIEWS_DIR_REL)
    dests = sorted(os.path.join(vdir, f) for f in os.listdir(vdir)
                   if f.endswith(".clj")) if os.path.isdir(vdir) else []

    rc, status, _ = git(root, "status", "--porcelain")
    callers = []
    for ln in status.splitlines():
        path = ln[3:].strip()
        code = ln[:2]
        if 'D' in code:
            continue
        if not path.endswith(".clj"):
            continue
        if path == SERVER_REL or path.startswith(TEST_DIR_REL + "/"):
            callers.append(os.path.join(root, path))
    callers = sorted(set(callers))

    monolith = git_show(root, MONOLITH_REL) or ""
    mono_ns = parse_ns(monolith) if monolith else None
    host_dest_indent = mono_ns["require"]["indent"] if (mono_ns and mono_ns["require"]) else None
    defs = top_level_defs(monolith)

    say("papercut-oracle  root=%s" % root)
    say("  destinations=%d  callers=%d  pre-split defs=%d  host :require indent=%s"
        % (len(dests), len(callers), len(defs), host_dest_indent))
    say()

    # cache parsed files
    parsed = {}
    for p in dests + callers:
        try:
            t = read(p)
        except Exception:
            continue
        parsed[p] = (t, parse_ns(t))

    # ---- 1. docstring-clone ---------------------------------------------
    docs = {}
    for p in dests:
        t, ns = parsed.get(p, (None, None))
        if ns and ns["doc"]:
            docs.setdefault(ns["doc"], []).append(p)
    clone_files = []
    for d, ps in docs.items():
        if len(ps) >= 2:
            for p in sorted(ps):
                clone_files.append({"file": rel(root, p),
                                    "line": 2,
                                    "docstring_head": d[:70].replace("\n", "\\n")})
    clone_files.sort(key=lambda x: x["file"])
    R["checks"]["docstring-clone"] = clone_files
    say("1. docstring-clone .......................... %d" % len(clone_files))
    for e in clone_files[:3]:
        say("     %s:%d  %s…" % (e["file"], e["line"], e["docstring_head"]))
    if not clone_files:
        say("     (every destination ns docstring is unique)")
    say()

    # ---- 2. clj-kondo ----------------------------------------------------
    touched_rel = set(rel(root, p) for p in dests + callers)
    findings, err = run_kondo(root)
    baseline, baseline_err = baseline_kondo(root)
    err = err or baseline_err
    existing = []
    if not err:
        findings, existing = partition_baseline(findings, baseline)
    kondo = OrderedDict()
    say("2. clj-kondo (new findings relative to HEAD)")
    say("     baseline findings: %d [advisory, not counted]" % len(existing))
    for f in existing:
        say("       %s:%s:%s  %s" % (f["file"], f["line"], f["col"], f["message"]))
    R["checks"]["baseline-lint"] = existing
    if err:
        say("     ERROR: %s" % err)
        for t in COUNTED_KONDO + ADVISORY_KONDO:
            kondo[t] = []
    else:
        for t in COUNTED_KONDO + ADVISORY_KONDO:
            hits = [f for f in findings if f["type"] == t]
            kondo[t] = hits
            tag = "" if t in COUNTED_KONDO else "   [advisory]"
            n_touched = sum(1 for f in hits if f["file"] in touched_rel)
            say("     %-22s %d%s  (%d in split-touched files)"
                % (t, len(hits), tag, n_touched))
            for f in hits[:3]:
                say("       %s:%s:%s  %s" % (f["file"], f["line"], f["col"],
                                             f["message"][:80]))
    R["checks"]["clj-kondo"] = kondo
    say()

    # ---- 3. continuation-misalignment ------------------------------------
    mis = []
    for p in dests + callers:
        t, ns = parsed.get(p, (None, None))
        if not ns:
            continue
        amap = alias_map(ns)
        is_dest = p in dests
        for h in misalignments(t, ns, amap, defs, is_dest):
            h["file"] = rel(root, p)
            mis.append(h)
    mis.sort(key=lambda x: (x["file"], x["line"]))
    R["checks"]["continuation-misalignment"] = mis
    say("3. continuation-misalignment ................ %d  (lines)" % len(mis))
    for e in mis[:3]:
        say("     %s:%d  head %s -> %s ; arg col %d -> %d, line sits at %d"
            % (e["file"], e["line"], e["old_head"], e["new_head"],
               e["old_col"], e["new_col"], e["old_col"]))
        say("       | %s" % e["text"])
    if not mis:
        say("     (no continuation line left at its pre-split column)")
    say()

    # ---- 4. require-layout ----------------------------------------------
    dest_bad, caller_bad, blanks = [], [], []

    for p in dests:
        t, ns = parsed.get(p, (None, None))
        if not ns or not ns["require"]:
            continue
        ents = [e["ns"] for e in ns["require"]["entries"] if e["ns"]]
        reasons = []
        if ents != sorted(ents):
            reasons.append("entries-not-sorted")
        if host_dest_indent is not None and ns["require"]["indent"] not in (None, host_dest_indent):
            reasons.append("indent=%s host=%s" % (ns["require"]["indent"], host_dest_indent))
        if reasons:
            dest_bad.append({"file": rel(root, p), "line": ns["require"]["line"],
                             "reasons": reasons})

    for p in callers:
        t, ns = parsed.get(p, (None, None))
        if not ns or not ns["require"]:
            continue
        pre = git_show(root, rel(root, p))
        pre_ns = parse_ns(pre) if pre else None
        if not pre_ns or not pre_ns["require"]:
            continue
        pre_ents = [e["ns"] for e in pre_ns["require"]["entries"] if e["ns"]]
        new_ents = [e["ns"] for e in ns["require"]["entries"] if e["ns"]]
        added = [e for e in new_ents if is_views_ns(e) and e != VIEWS_NS_PREFIX]
        reasons = []
        if added != sorted(added):
            reasons.append("block-not-sorted")
        # positions of added entries must be contiguous
        idxs = [i for i, e in enumerate(new_ents) if e in added]
        if idxs and idxs != list(range(idxs[0], idxs[0] + len(idxs))):
            reasons.append("block-not-contiguous")
        without = [e for e in new_ents if e not in added]
        others = [e for e in pre_ents if e != VIEWS_NS_PREFIX]
        if without != others:
            reasons.append("other-entries-reordered")
        if VIEWS_NS_PREFIX in pre_ents:
            # the monolith was required here: the block must replace it IN PLACE
            at = pre_ents.index(VIEWS_NS_PREFIX)
            expect = pre_ents[:at] + sorted(added) + pre_ents[at + 1:]
            if new_ents != expect and "other-entries-reordered" not in reasons:
                reasons.append("not-in-place")
        elif added and idxs:
            # nothing to replace: the block must land in sorted position
            # relative to its immediate neighbours in the new list.
            lo, hi = idxs[0], idxs[-1]
            pred = new_ents[lo - 1] if lo > 0 else None
            succ = new_ents[hi + 1] if hi + 1 < len(new_ents) else None
            if (pred is not None and pred > added[0]) or \
               (succ is not None and added[-1] > succ):
                reasons.append("not-in-sorted-position")
        hi, pi = ns["require"]["indent"], pre_ns["require"]["indent"]
        if pi is not None and hi is not None and hi != pi:
            reasons.append("indent=%s host=%s" % (hi, pi))
        if reasons:
            caller_bad.append({"file": rel(root, p), "line": ns["require"]["line"],
                               "reasons": sorted(set(reasons))})

    for p in dests + callers:
        t, ns = parsed.get(p, (None, None))
        if not ns:
            continue
        for ln in ns["blank_lines"]:
            blanks.append({"file": rel(root, p), "line": ln})
    blanks.sort(key=lambda x: (x["file"], x["line"]))

    R["checks"]["require-layout-dest"] = dest_bad
    R["checks"]["require-layout-caller"] = caller_bad
    R["checks"]["ns-whitespace-line"] = blanks

    say("4. require-layout")
    say("     require-layout-dest    %d  (files)" % len(dest_bad))
    for e in dest_bad[:3]:
        say("       %s:%d  %s" % (e["file"], e["line"], ", ".join(e["reasons"])))
    say("     require-layout-caller  %d  (files)" % len(caller_bad))
    for e in caller_bad[:3]:
        say("       %s:%d  %s" % (e["file"], e["line"], ", ".join(e["reasons"])))
    say("     ns-whitespace-line     %d  (lines)" % len(blanks))
    for e in blanks[:3]:
        say("       %s:%d  blank line inside the ns form" % (e["file"], e["line"]))
    say()

    # ---- 5. stale-prose (advisory) ---------------------------------------
    stale = []
    stale_re = re.compile(r"\bviews/[a-z-]")
    scan_set = list(dests) + [os.path.join(root, SERVER_REL)]
    tdir = os.path.join(root, TEST_DIR_REL)
    if os.path.isdir(tdir):
        scan_set += sorted(os.path.join(tdir, f) for f in os.listdir(tdir)
                           if f.endswith(".clj"))
    for p in sorted(set(scan_set)):
        if not os.path.exists(p):
            continue
        t = read(p)
        _, spans = scan(t)
        starts = line_starts(t)
        for kind, s, e in spans:
            seg = t[s:e]
            for m in stale_re.finditer(seg):
                ln = idx_to_line(starts, s + m.start()) + 1
                stale.append({"file": rel(root, p), "line": ln, "kind": kind,
                              "text": t.split("\n")[ln - 1].strip()[:80]})
    stale.sort(key=lambda x: (x["file"], x["line"]))
    R["checks"]["stale-prose"] = stale
    say("5. stale-prose .............................. %d   [advisory]" % len(stale))
    for e in stale[:3]:
        say("     %s:%d (%s)  %s" % (e["file"], e["line"], e["kind"], e["text"]))
    say()

    # ---- 6. tracked-log-churn (advisory) ---------------------------------
    rc, st, _ = git(root, "status", "--porcelain", "--", "00SERVER-LOGS.txt")
    churn = st.strip()
    R["checks"]["tracked-log-churn"] = churn
    say("6. tracked-log-churn ........................ %s   [advisory, not counted]"
        % ("DIRTY" if churn else "clean"))
    if churn:
        say("     %s" % churn)
    say()

    # ---- table -----------------------------------------------------------
    rows = [
        ("docstring-clone", len(clone_files), "files", True),
        ("unused-import", len(kondo.get("unused-import", [])), "findings", True),
        ("unused-namespace", len(kondo.get("unused-namespace", [])), "findings", True),
        ("unused-referred-var", len(kondo.get("unused-referred-var", [])), "findings", True),
        ("unresolved-symbol", len(kondo.get("unresolved-symbol", [])), "findings", True),
        ("unresolved-namespace", len(kondo.get("unresolved-namespace", [])), "findings", False),
        ("continuation-misalignment", len(mis), "lines", True),
        ("require-layout-dest", len(dest_bad), "files", True),
        ("require-layout-caller", len(caller_bad), "files", True),
        ("ns-whitespace-line", len(blanks), "lines", True),
        ("stale-prose", len(stale), "hits", False),
        ("tracked-log-churn", 1 if churn else 0, "files", False),
    ]
    total = sum(c for _, c, _, counted in rows if counted) + (1 if err else 0)

    say("  %-28s %6s  %-9s %s" % ("CHECK", "COUNT", "UNIT", "COUNTED"))
    say("  " + "-" * 60)
    for name, c, unit, counted in rows:
        say("  %-28s %6d  %-9s %s" % (name, c, unit, "yes" if counted else "advisory"))
    say("  " + "-" * 60)
    say()
    say("PAPERCUTS: %d" % total)

    R["total"] = total
    print("\n".join(out))
    if want_json:
        print(json.dumps(R, indent=1, sort_keys=False, default=str))
    return 1 if total > 0 else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
