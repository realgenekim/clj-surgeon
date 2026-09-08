# Timed extraction run (arm M, Cell B) — Curtain Call CFP

Complete it correctly, stamped, and report. **Budget 30 minutes** (hard cap; a timeout is
a censored unfinished run, not a failure to hide — stamp `done` and report where you were).

Work ONLY in `/home/forge/src/cc-cellB-M` (branch `cellB/m`, created from
`origin/main` @ `92a7ca14fd904e4d962df38614b9af3a7ef41a11`). Do not commit, push, stash or
checkout. Temp files only under `/var/tmp/forge/plan2/cellB/M/` — never `/tmp`.
Never touch `~/acid`, `chain-*.sh`, `.cohort-lock`, `GO-*` files or any Curtain Call
fleet/runtime directory; never contact ports 7888/7890/7894/7895/83xx; never `pkill` or
pattern-kill; never hand-type a timestamp.

## ROUTE RULE FOR THIS ARM
Use the clj-surgeon **MCP** surface for the structural work. Load the schemas with
ToolSearch `select:mcp__clj-surgeon__helper_extraction,mcp__clj-surgeon__inspect_clojure,mcp__clj-surgeon__apply_clojure_changes`
and read the `clj-surgeon` skill (Skill tool, name `clj-surgeon`) for the extraction and
movement workflows. `workspace_root` is `/home/forge/src/cc-cellB-M` on every call.

- Plan with `inspect_clojure` **plan-extraction** mode, then execute with
  `helper_extraction`. Ordinary native preparation and native fallback are allowed.
- Use the tool for every form move and caller rewrite it has a route for; native edits
  only where it does not — **say which, and why, for each one**.
- If a call refuses: repair **one** clear argument error from the refusal text, retry
  once, then go native for that step and record the refusal **verbatim**.
- A fallback is counted as a fallback. Do not relabel a native step as a tool step, and
  do not switch to the CLI — that is arm C, not this arm.
- Save every MCP request and response verbatim as JSON in
  `/var/tmp/forge/plan2/cellB/M/` (`request-N.json`, `response-N.json`).

## STOPWATCH (mandatory)
```bash
export DOGFOOD_LOG=/var/tmp/forge/plan2/cellB/M/stopwatch.log
```
Your **very first action** is `/var/tmp/forge/dogfood-stamp.sh orient-start`. Then stamp:

| stamp | when |
|---|---|
| `plan-complete` | you have your move plan |
| `moves-complete` | all 25 forms are in the destination file and gone from the source |
| `callers-complete` | every caller compiles (requires/aliases updated) |
| `verify-start`/`verify-done` | stamped for you by `dogfood-verify.sh` — see below |
| `oracle` | the frozen oracle has printed `ORACLE RESULT: PASS` |
| `done` | last action |

Run the suite THROUGH the wrapper, from the worktree root:
```bash
/var/tmp/forge/dogfood-verify.sh kaocha -- bin/kaocha unit --fail-fast
```
If it is red, fix and re-run through `dogfood-verify.sh` (every run is stamped).

## TASK
Read `/var/tmp/forge/plan2/cellB/task.md` **in full first**. It is the frozen task: the
base sha, the exact 25-form closure, the destination namespace
`cfp-scheduler-killer.exports.calendar`
(`src/cfp_scheduler_killer/exports/calendar.clj`), the architectural rationale, the
retained vars the new namespace will need, the authorised file footprint, and the rule
deviation that was accepted when the task was frozen.

It deliberately does **not** give you a caller-site table: no file list of the references,
no line numbers, no per-file counts, no alias inventory. Finding and rewriting the caller
surface is the work being measured. Do not ask for it.

Do not change behaviour: same arities, same visibility, same docstrings, same output bytes.
Promoting a private var in `exports` to public is permitted; duplicating any var is not;
compatibility forwarding vars are not.

## ORACLE (run it yourself; it is the acceptance verdict)
From the worktree root:
```bash
bash /var/tmp/forge/plan2/cellB/oracle.sh
```
It prints PASS/FAIL per check and exits non-zero on any FAIL. Its checks are:
structural inventory over clj-kondo's resolved var usages; a lint pass for unresolved
vars; fresh-process load in two orders with a runtime intern assertion; the focused
cluster tests plus one integration path; `make runtests-once` and
`bin/kaocha unit --fail-fast`; and a byte-identity check of every file outside the
authorised footprint against the base commit.

Do not edit, copy, or work around `oracle.sh`. If you believe a check is wrong, say so
with the exact failing line in your report and leave it alone.

## REPORT (identical format for every arm)
(a) **stamp table** — phase, seconds from `orient-start`, delta;
(b) `git -C /home/forge/src/cc-cellB-M status --short | wc -l` and the `git diff --stat` summary;
(c) **the oracle output verbatim** — the whole PASS/FAIL block including `ORACLE RESULT`;
(d) **how the work was done** — tool receipts: every call (verb, and the files/forms per the tool's own response fields), every refusal verbatim, server-reported ms where the response carries it, and which steps went native and why. Then: script or manual for the native parts, and what broke.
(e) **doubts** — what you are not sure survived, and what you did not verify.
