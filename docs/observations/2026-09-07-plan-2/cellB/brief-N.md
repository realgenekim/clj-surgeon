# Timed extraction run (arm N, Cell B) — Curtain Call CFP

Complete it correctly, stamped, and report. **Budget 30 minutes** (hard cap; a timeout is
a censored unfinished run, not a failure to hide — stamp `done` and report where you were).

Work ONLY in `/home/forge/src/cc-cellB-N` (branch `cellB/n`, created from
`origin/main` @ `92a7ca14fd904e4d962df38614b9af3a7ef41a11`). Do not commit, push, stash or
checkout. Temp files only under `/var/tmp/forge/plan2/cellB/N/` — never `/tmp`.
Never touch `~/acid`, `chain-*.sh`, `.cohort-lock`, `GO-*` files or any Curtain Call
fleet/runtime directory; never contact ports 7888/7890/7894/7895/83xx; never `pkill` or
pattern-kill; never hand-type a timestamp.

## ROUTE RULE FOR THIS ARM
Do **NOT** use any `mcp__clj-surgeon__*` tool and do not call any Surgeon server or the
`~/bin/clj-surgeon` CLI. Native tools only: Read/Edit/Bash, plus any script you write.
Installed general-purpose analyzers are allowed — `~/bin/clj-kondo` (always through that
paved path, never a direct binary), `rg`, `git`. You may batch the whole job into one
script and fuse edits with verification; you are not required to imitate per-form
tool calls or to use a deliberately weak regex.

## STOPWATCH (mandatory)
```bash
export DOGFOOD_LOG=/var/tmp/forge/plan2/cellB/N/stopwatch.log
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
(b) `git -C /home/forge/src/cc-cellB-N status --short | wc -l` and the `git diff --stat` summary;
(c) **the oracle output verbatim** — the whole PASS/FAIL block including `ORACLE RESULT`;
(d) **how the work was done** — script or manual? what you wrote, what broke, and every place the plan had to change.
(e) **doubts** — what you are not sure survived, and what you did not verify.
