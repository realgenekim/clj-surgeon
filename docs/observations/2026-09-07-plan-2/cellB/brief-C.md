# Timed extraction run (arm C, Cell B) — Curtain Call CFP

Complete it correctly, stamped, and report. **Budget 30 minutes** (hard cap; a timeout is
a censored unfinished run, not a failure to hide — stamp `done` and report where you were).

Work ONLY in `/home/forge/src/cc-cellB-C` (branch `cellB/c`, created from
`origin/main` @ `92a7ca14fd904e4d962df38614b9af3a7ef41a11`). Do not commit, push, stash or
checkout. Temp files only under `/var/tmp/forge/plan2/cellB/C/` — never `/tmp`.
Never touch `~/acid`, `chain-*.sh`, `.cohort-lock`, `GO-*` files or any Curtain Call
fleet/runtime directory; never contact ports 7888/7890/7894/7895/83xx; never `pkill` or
pattern-kill; never hand-type a timestamp.

## ROUTE RULE FOR THIS ARM
Use the clj-surgeon **CLI**, `~/bin/clj-surgeon`, for the extraction. Do **not** use any
`mcp__clj-surgeon__*` tool in this arm. Native preparation, native repairs and native
completion are allowed and are counted as fallback where the CLI had no route.

**The verified invocation** (hand-driven once, untimed, by the acceptance owner against
the base copy of `exports.clj`; it is the dry run — read its report before executing):

```bash
cd /home/forge/src/cc-cellB-C
~/bin/clj-surgeon :op :extract \
  :file src/cfp_scheduler_killer/exports.clj \
  :forms '[calendar-prodid ical-domain ics-escape fold-line ics-date ics-stamp ics-uid ics-sequence ics-local-datetime absolute-link vevent snapshot-published? last-publication-snapshot ics-sequence-at cancellation-vevent cancellation-for hold-the-date-uid hold-the-date-sequence hold-the-date-vevent cfp-deadline-uid cfp-deadline-sequence cfp-deadline-ics submission-ics calendar-ics-for calendar-ics]' \
  :to src/cfp_scheduler_killer/exports/calendar.clj
```
then, to execute:
```bash
~/bin/clj-surgeon :op :extract! \
  :file src/cfp_scheduler_killer/exports.clj \
  :forms '[... the same 25 names, same order ...]' \
  :to src/cfp_scheduler_killer/exports/calendar.clj \
  :require-policy :minimal \
  :receipt-out /var/tmp/forge/plan2/cellB/C/extract-receipt.edn
```

**The caller-rewiring option, determined by reading `:op :extract! --help` and the
skill's `references/advanced-operations.md`:** there is **no external-caller rewriting
option** in `:extract!`. The only two caller-related knobs are

1. `:require-policy` — `:minimal` (default; proves a minimal target header) or
   `:copy-all` (copies the complete source ns header verbatim, changing only the ns
   name). If `:minimal` refuses, `:copy-all` is the documented safest mechanical start;
   excess requires are removed in a later change.
2. the **source-side** rewiring the tool does by itself: when a form remaining in the
   source namespace still calls a moved Var it adds a collision-free source alias and a
   sorted `:refer` list to the source ns. The dry run reports this as
   `remaining-source-callers`, `source-require-added` and `target-alias`. **Read those
   fields and decide whether that edit is what the architecture wants** — the tool is
   reporting a fact, not making the architectural decision for you.

Everything outside the source file — the external caller namespaces — is yours to do,
either with `~/bin/clj-surgeon :op :change!` (structured `:spec-file -` EDN input, see
`:op :change! --help`) or natively. Say which you used for each file.

`:mv-with-deps` is within-file movement and cannot cross namespaces; it is not a route
here. Save every CLI invocation and its EDN result as
`/var/tmp/forge/plan2/cellB/C/cli-N.txt`.

## STOPWATCH (mandatory)
```bash
export DOGFOOD_LOG=/var/tmp/forge/plan2/cellB/C/stopwatch.log
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
(b) `git -C /home/forge/src/cc-cellB-C status --short | wc -l` and the `git diff --stat` summary;
(c) **the oracle output verbatim** — the whole PASS/FAIL block including `ORACLE RESULT`;
(d) **how the work was done** — CLI receipts: every invocation, its EDN result fields (`form-count`, `lines-extracted`, `target-requires`, `omitted-target-requires`, `remaining-source-callers`, `source-require-added`, `target-alias`, `callers-to-review`), every refusal verbatim, and which steps went native and why. Then: script or manual for the native parts, and what broke.
(e) **doubts** — what you are not sure survived, and what you did not verify.
