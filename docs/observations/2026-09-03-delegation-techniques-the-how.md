# Delegation techniques — the HOW behind "we inspect the results as if we did the work ourselves"

*Gene, 2026-09-03: "Ideally, you'd find examples in Claude history where you did exactly that;
describe in more detail the techniques. The how (versus just why and what)."*

`docs/vision.md` §"When the work is delegated" states six principles. This is the field manual
underneath them: each technique as executed, with command text lifted verbatim from the session
transcript (`b623492c-…jsonl`), the timestamped moment, what it caught, and the rule.

Times are UTC as the transcript records them (−7 h for Pacific). Seat: forge@anvil (forge@buster
before 2026-09-03 ~01:30Z; paths differ accordingly). Ten builder/reviewer lanes ran concurrently.

---

## 1. Read the suite's own lines, not the agent's summary — and gate the push on a count you computed

**Moment:** first form 2026-09-02T23:17:52Z (census); final form 2026-09-03T04:30:57Z (ratchets r2).

The builder reports "all green." That is a claim. The receipt is the suite's own output: count the
failure lines yourself, subtract the branch's *pre-measured* baseline, and let arithmetic — not a
human reading a paragraph — decide whether the branch is pushed. Final form:

```bash
head -8 …/tasks/b1q8ixsp0.output
N=$(grep -hoE 'FAIL in \([^)]*\)' ~/tmp/ratchets2-test-fast.log ~/tmp/ratchets2-mcp-test.log \
     | grep -vcE 'terminal-response-routing-is-conditional-on-complete-user-work|exact-profile-compilation-is-project-owned-and-snapshot-bound')
E=$(grep -hoE 'ERROR in \([^)]*\)' ~/tmp/ratchets2-test-fast.log ~/tmp/ratchets2-mcp-test.log | wc -l)
echo "non-baseline: $N errors: $E"
if [ "$N" = 0 ] && [ "$E" = 0 ] \
   && grep -q '^Ran ' ~/tmp/ratchets2-test-fast.log \
   && grep -q '^Ran ' ~/tmp/ratchets2-mcp-test.log \
   && grep -qa 'oracle: pass' ~/tmp/ratchets2-mcp-test.log; then
  cd ~/src/clj-surgeon-ratchets && git push -q origin bridge/receipt-ratchets 2>&1 | tail -1
  git ls-remote --heads origin bridge/receipt-ratchets | cut -c1-12
fi
```

The `grep -q '^Ran '` clauses exist because a suite that *crashed before running* also produces
zero `FAIL in` lines: two conditions test that the evidence exists, two test what it says — same
defect as `verdict-label-was-a-noun` (2026-09-02: a chain printed the literal word `pass` over a
score file that did not exist). Where kaocha prints one summary line, the else branch is explicit
(03:51:52Z, fold-diff r2):

```bash
R=$(grep -aoE '[0-9]+ tests, [0-9]+ assertions, [0-9]+ failures' ~/tmp/folddiff2-unit.log | tail -1)
C=$(tail -n 1 ~/tmp/folddiff2-compile.log)
if [ "${R##* }" = failures ] && echo "$R" | grep -q ' 0 failures' && [ "$C" = "EXIT 0" ]; then
  … push … ; else echo "NOT PUSHED: $R / $C"; fi
```

Logs live under `~/tmp/<lane>/` (`~/tmp/census/my-*.log`) — a rule added 04:26Z after a sibling
lane overwrote a builder's `~/tmp` log and destroyed a completed round's evidence.

**Rule:** the push is a function of a number you computed from the suite's own bytes, plus a
separate clause proving the suite ran at all. Never a function of the report.

---

## 2. Read the pushed sha back from the remote

**Moment:** every push; e.g. 2026-09-03T04:35:39Z (rf2).

```bash
cd ~/src/clj-surgeon-rf2 && git status --short | grep -v cpcache
git log --oneline 5ccb4f0..HEAD | wc -l
git push -q origin bridge/rf2-extract-rewire 2>&1 | tail -1
git ls-remote --heads origin bridge/rf2-extract-rewire | cut -c1-12
```

`git push` returning 0 is a claim about a network operation; `ls-remote` is the remote's own answer
to "what is at this ref" — and it is the sha the mayor's merge queue will fetch. The preceding
`git status --short` proves nothing uncommitted was left behind.

**Rule:** a branch is delivered when the remote says so. (Delivery invariant 18: test delivery, not
identity.)

---

## 3. Read the diff before marking GO

**Moment:** 2026-09-03T04:42:40Z, routing-doc lane.

The builder's job was to fix five long-standing `terminal-response-routing…` baseline failures; its
report said "doc drift." Before that branch entered the queue as a GO:

```bash
cd ~/src/clj-surgeon-routing && git status --short | grep -v cpcache
git show --stat --format='%h %an' HEAD | head -4
git diff HEAD~1 HEAD -- resources/clj-surgeon-agent-routing.md | grep '^+' | grep -v '^+++'
git push -q origin bridge/routing-doc-test 2>&1 | tail -1
git ls-remote --heads origin bridge/routing-doc-test | cut -c1-12
```

`grep '^+'` prints the added lines only — the restored MCP-OP-RELAY-004 paragraph, verbatim, next
to the author line and file stat. It established (04:42Z log) that commit `01f0739` had rewritten
the routing doc for the Surgeon-default ruling and silently dropped the paragraph the test asserts;
the fix restored the verbatim shape, `test-fast` 702/5912/0.

**Rule:** for doc, config, or prompt-plate changes the added lines get read in the terminal before
GO. A test that goes green because the *assertion's subject* was edited looks identical to one that
goes green because the code was fixed.

---

## 4. The brief is the spec and the pre-registration

**Moment:** 2026-09-03T01:59:59Z (ratchets r1), 03:07:28Z (ratchets fix round), 03:58:42Z (q5z r3).

Every builder brief opens with the same load-bearing paragraph before a word of task content
(03:07:28Z, verbatim):

> Repo: clj-surgeon, worktree /home/forge/src/clj-surgeon-ratchets, branch bridge/receipt-ratchets
> at ece8c1c (pushed; `git status --short` empty ignoring .cpcache, else STOP). Box: ANVIL, shared
> 16-core; NEVER contact ports 7888–7895; free port ≥ 7900 for any server (stop after); nothing
> outside this worktree and /home/forge/tmp; no sudo; never git stash; NEVER `git add -A` (two
> rounds tonight swept `.cpcache/` into commits — add files by name); do not push; commit as
> GIT_AUTHOR_NAME="forge-anvil" … Suites one at a time under `flock /home/forge/tmp/suite.lock`:
> `make test-fast` (baseline on this branch 712/5970 with 5 pre-existing routing failures) and
> `clojure -M:clj-surgeon/mcp-test` (387/4020 with 1 pre-existing exact-profile; no swipl here).
> Lint with `clj-kondo`. No fence changes.

Six mechanisms live in that paragraph:

- **The verify command, named, with its baselines.** "Green" is undefined without the number the
  branch started at. The r1 brief pre-registers the measurement: "measure the baseline on the clean
  tree FIRST and record it."
- **The disjoint file-set.** One agent, one worktree. Where two lanes touch the same file the brief
  names the sibling: "another builder is working in /home/forge/src/curtaincall-cfp-lens2 on other
  arms of the same file — do not touch that worktree" (02:02:00Z).
- **Witness first, commit red.** "witnesses that FAIL FIRST with real bytes"; the report must give
  "each witness name with red→green."
- **`NEVER git add -A` — add files by name.** Earned the same night: two rounds swept `.cpcache/`
  into commits. Related 2026-09-02 scar: `git add -A -- . ":!.cpcache"` exits 1 whenever the
  ignored path exists, which silently skipped every diff in an experiment arm.
- **"never signal a process you did not start (prove ownership by pid, ppid and cmdline first)"** —
  in every brief from 03:58:42Z. Earned at 03:50:20Z from a builder's *own* report: it killed a
  `flock` waiter (`kill 3724641`) before confirming ownership; ownership was checked only for the
  second kill. On a shared 16-core box a wrong kill is another seat's outage.
- **Refusal is a legal output; scope creep is reported, not done.** "If (2) needs a contract change
  to an existing verb beyond an optional field, STOP on that item and report the exact change
  instead"; "anything you were tempted to widen (report, do not do)"; "Do NOT touch the SCI
  allowlist / `:classes`, the evaluation fence, or path/workspace confinement."

**Rule:** a brief the agent could satisfy with a green suite and a narrowed promise is a bad brief.
Name the verify command *and its baseline*, the fence, the witness order, and what a refusal looks
like — before the work starts, so the report has nowhere to hide.

---

## 5. Executed re-review by a second model — the sol-yolo mechanism

**Moment:** wrapper written 2026-09-03T04:14:21Z; reviews launched 04:31:27Z (ratchets), 04:35:39Z
(rf2), 04:37:49Z (census).

The problem, in Gene's words at 04:25Z: *"Sol not being able to shell out seems unacceptable."* On
Anvil `codex exec --sandbox read-only` cannot run a shell at all (bwrap loopback failure), so the
first memory consult *reasoned without measuring*. Fix: the codex skill's YOLO recipe with a fresh
scratch clone as the only fence.

```bash
#!/usr/bin/env bash
# sol-yolo <worktree> <prompt-file> [mcp-url] [report-path] — codex exec in YOLO mode (no sandbox, no approvals).
# The worktree is the only fence: use ONLY a directory created for this run.
set -euo pipefail
WT=${1:?worktree}; PF=${2:?prompt-file}; URL=${3:-}; OUT=${4:-$WT/../sol-report.md}
[ -d "$WT" ] || { echo "no such worktree: $WT" >&2; exit 2; }
case "$WT" in /home/forge/src/clj-surgeon|/home/forge/src/clj-surgeon/)
  echo "refusing: that checkout serves my MCP server on 7906" >&2; exit 3;; esac
extra=(); [ -n "$URL" ] && extra=(-c "mcp_servers.clj-surgeon.url=\"$URL\"")
exec codex exec --color never --skip-git-repo-check -C "$WT" \
  --dangerously-bypass-approvals-and-sandbox -m gpt-5.6-sol \
  -c 'model_reasoning_effort="high"' "${extra[@]}" -o "$OUT" - < "$PF"
```

Three details are load-bearing, each paid for:

- **`- < "$PF"`** — the prompt arrives on stdin from a file, and stdin is *closed*. Without that,
  `codex exec` hangs on "Reading additional input from stdin" (`sol-live-on-anvil-seat`: "ALWAYS
  `</dev/null`").
- **`--dangerously-bypass-approvals-and-sandbox`** — `codex exec` silently AUTO-CANCELS MCP *write*
  tools without it; this invalidated a whole measurement run before it was caught by a direct probe
  at 2026-09-02T02:22:20Z (four `mcp_tool_call_end {Err: user cancelled MCP tool call}` at 0 ms in
  the rollout, server telemetry seeing only reads, the same `edit_clojure` landing with the flag).
- **The typed refusal on line 8** — YOLO has no sandbox, so the *directory* is the entire safety
  argument; a wrapper that can be pointed at the live 7906 checkout has no safety argument.

The clone is fresh per review, at the exact reviewed sha (04:31:27Z):

```bash
S=/home/forge/tmp/sol; rm -rf $S/ratchets-wt && git clone -q /home/forge/src/clj-surgeon $S/ratchets-wt \
  && (cd $S/ratchets-wt && git remote set-url origin https://github.com/realgenekim/clj-surgeon.git \
      && git fetch -q origin bridge/receipt-ratchets main && git checkout -q 49f6e12)
cat > $S/ratchets-review-q.md <<'EOF'
You are Sol, doing an executed re-review of clj-surgeon branch bridge/receipt-ratchets at 49f6e12 …
For each of its 8 items: CLOSED / PARTIAL / OPEN with file:line, re-running the probes …
End with GO / GO-WITH-FIX / NO-GO for entering the mayor's merge queue and a numbered list with
file:line and a one-line witness each.
EOF
(cd $S && ~/bin/sol-yolo $S/ratchets-wt $S/ratchets-review-q.md "" $S/ratchets-sol-review.md \
   > $S/ratchets-sol.log 2>&1; echo "EXIT $?" >> $S/ratchets-sol.log) > /dev/null 2>&1 &
```

The reviewer's rules mirror the builder's: "never git commit/stash/push; never contact ports
7888–7895 or 7906; if you need a server use 7909 or 7910 from this checkout … and stop it after;
JVMs one at a time with explicit -Xmx ≤ 1g."

**Rule:** nothing enters the merge queue on the builder's word. The second model *runs the probes*
in a throwaway clone at the exact sha; the clone is the fence, and the wrapper refuses the one
directory that would make the fence a lie.

---

## 6. The waiter loop: `^EXIT` plus a byte count

**Moment:** 2026-09-03T04:37:07Z and 04:37:08Z (two reviews in flight).

```bash
for i in $(seq 1 240); do
  if grep -q '^EXIT' /home/forge/tmp/sol/ratchets-sol.log 2>/dev/null; then
    echo "ratchets done: $(tail -1 /home/forge/tmp/sol/ratchets-sol.log) $(wc -c < /home/forge/tmp/sol/ratchets-sol-review.md 2>/dev/null) bytes"
    exit 0
  fi
  sleep 30
done
echo "timeout 2h"
```

The launcher appends `echo "EXIT $?" >> log` inside the subshell, so the log's last line is the
process's own terminal receipt rather than an inference from `pgrep`. The waiter greps that marker,
then prints the **report file's size** — because a live PID is not progress (2026-08-11: a
backgrounded `codex exec` sat alive and 0-byte for over an hour while being reported as "still
chewing"). Suite waiters take the same shape: `until grep -q '^EXIT' ~/tmp/lens2-unit.log; do
sleep 20; done`, then the `tests, assertions, failures` line and the `FAIL in` tally.

**Rule:** wait on a terminal marker the process wrote, cap the wait, and report the artifact's byte
count with the exit code. A 0-byte output after minutes is wedged, not working.

---

## 7. When the reviewer refuses, say so in the receipt and route the item elsewhere

**Moment:** 2026-09-03T04:44:11Z.

Sol's content filter refused the rf2 round-three re-check — "two 'flagged for possible cybersecurity
risk' lines on the confinement fixtures, 110k tokens, no report." It flags the *fixtures* (symlink /
path-confinement test data), not the prompt. Standing rule: route that class to Opus, record the
substitution, and edit the artifact the next human reads in the same command.

```bash
cd ~/src/clj-surgeon && printf '\n## %s — Sol content filter refused the rf2 round-3 re-check (…). Fallback per the standing rule: Opus re-review launched on a scratch clone, report to ~/tmp/sol/rf2-opus-review.md; receipt will say so.\n' "$(date -u +%H:%MZ)" \
  >> docs/observations/2026-09-03-captains-log-anvil-seat.md
sed -i 's#| 465c956 | pushed, Sol round-3 re-check running |#| 465c956 | pushed; Sol refused by content filter → Opus re-check running |#' \
  docs/observations/2026-09-03-merge-queue-for-mayor.md
```

**Rule:** a refusal an agent paid is a ledger item, not a gap. Name the refusal text, the substitute
reviewer, and correct the merge-queue row — so whoever acts on it knows which model actually
reviewed the branch.

---

## 8. The telemetry read, and the false zero that was a root mismatch

**Moment:** 2026-09-02T00:53:49Z → 00:55:24Z; standing meter on Anvil 2026-09-03T03:31:02Z.

A delegated run is a free adoption arm: every builder has a Surgeon server on a port and nobody
tells it to use it. The meter:

```bash
cd ~/src/clj-surgeon && timeout 280 make study-agent-usage \
  AGENT_USAGE_ARGS="--since 2026-09-01T23:30:00Z --receipt-out $D/surgeon-usage-receipt.json"
```

The first receipt disagreed with itself: the Claude-history side counted **7 surgeon calls**, the
surgeon *service* section said `status: "no-events", mcp_tool_calls: 0`. Two sources disagreeing is
a finding, not a number. Diagnosis was three bounded commands (00:54:30Z–00:54:43Z):

```bash
grep -n -E '^MCP_STATE_DIR|MCP_STATE_DIR\s*[:?]?=' Makefile; grep -n -A6 '^study-agent-usage:' Makefile
T=~/.local/state/clj-surgeon/mcp/telemetry; ls -la $T; for f in $T/*.jsonl; do echo "$(basename $f): total $(wc -l < $f), tonight $(grep -c '2026-09-02T00' $f)"; done
grep -n -B2 -A10 '^mcp-serve:' Makefile; grep -n -E 'telemetry-dir' src/clj_surgeon/mcp_server.clj src/clj_surgeon/mcp_http_server.clj src/clj_surgeon/mcp_telemetry.clj
```

Cause: `make mcp-serve` starts the server **without** `:telemetry-dir`, so it writes to its own
default `~/.local/state/clj-surgeon/telemetry/<session>.jsonl` (9 lines, 8 since 23:30Z); the
collector's default `--surgeon-telemetry-root` is `~/.local/state/clj-surgeon/mcp/telemetry` (the
Makefile's `MCP_STATE_DIR` convention), **which does not exist on that box**. An absent directory
was reported as "no-events," i.e. zero. Re-run with the corrected root, superseded receipt
discarded rather than merged:

```bash
timeout 280 make study-agent-usage AGENT_USAGE_ARGS="--since 2026-09-01T23:30:00Z \
  --surgeon-telemetry-root $HOME/.local/state/clj-surgeon/telemetry \
  --receipt-out $D/surgeon-usage-receipt-v2.json"
```

Corrected receipt: **8 mcp_tool_calls** (7 `apply_clojure_changes`, 1 `inspect_clojure` = the seat's
liveness probe); **outcomes 6 ok, 2 refused**; apply wall median 1.8 s, p90/max 4.2 s, total 14.2 s;
inspect 17 ms. History side: 7 surgeon actions, 22 shell actions, 1 skill load, route shape
`git → native-read ×11 → skill-load → native-read → … → surgeon-apply ×5 → verify`; **zero
`inspect_clojure` from the agent**, matching its own notes. Collector then fixed on
`fix/collector-telemetry-root` (`3c3427d`) to scan both conventions and emit a typed `root-absent`;
an inbox item was filed for the same false-zero shape in `collect_cclsp_telemetry`. On Anvil the
standing meter is a dedicated server: `clojure -X:clj-surgeon/mcp :port 7906 :telemetry :full`
(03:31:02Z).

**Rule:** a false zero is a telemetry-root question, not a finding. When two sources of the same
number disagree, the disagreement is the result; discard the superseded receipt rather than
averaging it, and make the absence *typed* instead of zero.

---

## 9. The watcher is the meter; the driver is not

**Moment:** session-1 verdict 2026-09-02T18:37Z; session-5 watcher spawned 2026-09-02T23:50:13Z.

The driver (this seat, hand-driving a verb) closed session 1 with "15 returns." A Sonnet watcher
reading the same transcript closed it at **35 model returns / 797 s wall** (31 / 697 s to the
committed move), against native's 20–24 / 311–342 s. Both true under their definitions: the driver
had excluded the mayor reply, log commits, the watcher spawn, a monitor stop, a tool-schema load,
and an unrelated log write interleaved mid-session. The meter excluded nothing.

The watcher is a subagent whose only job is metering, given the transcript path, a byte offset, an
inclusion rule, an *exclusion* rule, a record schema, and a stop condition (23:50:13Z, verbatim):

> Driver transcript: …/b623492c-….jsonl, **start at byte offset 23688537**. Read incrementally
> (`tail -c +N`), never the whole file (>23 MB). … Count every driver call that touches this: Bash
> calls invoking mcp_call.py against 7897 or reading .tweezer/ or the lens worktree, and any
> inspect_clojure calls; **exclude housekeeping (git commits to clj-surgeon-main, maven inbox, the
> reply tool, Anvil ssh) but count them as excluded.** … Write ONE record per counted call,
> append-only … (`:n :t :elapsed-ms :cum-returns :cum-wall-s :tool :intent :expected :actual
> :deviation :return-tax :context-privilege`) … **Do NOT suggest calls or repair.** … Stop on the
> marker …/.tweezer/session-5.closed, or 45 minutes after the start offset time, or 12 idle minutes.

Housekeeping is excluded *and counted*, so the number is auditable rather than argued — the direct
ratchet from the session-1 gap. And the watcher may not suggest or repair: an observer that helps
has contaminated its own measurement.

**Rule:** never report a self-counted return or wall figure for a session that has a watcher.
Report the watcher's; if they differ, report both and the reason.

---

## 10. Guarding delegates that can hang; never reporting one as running unseen

**Moment:** timeouts throughout; the confabulation scar 2026-06-29; the wedge scar 2026-08-11.

1. **A hard timeout on every long delegate:** `timeout 1500 codex exec …` (memory consult,
   04:13:54Z), `timeout 2400` on cohort arms, `timeout 900 make mcp-test`, `timeout 240 make
   study-agent-usage`. A `timeout` on a foreground call kills only your *wait* — if you time one
   out, kill the PID.
2. **Output-size and exit-code checks, not liveness** (§6). A live PID with a 0-byte artifact is
   wedged.
3. **Redundancy on the critical path:** the memory design ran as *two* consults — Sol on a scratch
   clone and an Opus agent — so one wedge or one content-filter refusal could not block the
   deliverable.

Underneath them, the reporting discipline: every lane status in this session is printed from a task
output file or a `pgrep`, never from memory — `head -8 …/tasks/b1q8ixsp0.output` — because on
2026-06-29 this seat carried "the extraction is running" forward across many turns having never made
the `Agent` call at all. An assumed status silently becomes an asserted one.

**Rule:** cap it, check its artifact's size, duplicate it if a human is waiting, and never say a
delegate is running without having seen its process or its output file this turn.

---

## 11. Re-run the reviewer's probe — by hand, or by writing it into the next brief

**Moment:** 2026-09-03T03:52:15Z (fold-diff), 04:44:04Z (rf2 round three, Opus).

A review round produces a verdict *and* a set of probes. The next round's brief does not ask "did
you fix items 1–7"; it names the probe and demands its output (rf2 r3, verbatim):

> For each round-2 item and the ruling: CLOSED / PARTIAL / OPEN with file:line and the probe
> **RE-RUN by you** (especially: `src/app/alias_caller.clj -> .git/hooks/caller.clj` refuses
> `:caller-path-in-skipped-tree` with the .git file **byte-identical**, the link **still a link**,
> **no target created**; a link to a NON-pruned file inside the root still yields one caller plan
> writing the real file, link preserved).

Three post-conditions on one probe — the file's bytes, the link's type, the absence of a created
target — because "it refused" is compatible with having written something first. The
production-critical fold-diff brief does the same for a tool the mayor will point at live Postgres:
"can any `require`, `defonce`, or `-main` path reach `store-pg/start!` → `ensure-schema!` →
`CREATE UNIQUE INDEX`? Is `db/start-pool!` still the only opener? … is the link removed on every
exit path incl. exit 3?"

A builder's executed red is worth as much as a reviewer's predicted one. The census round reported,
logged verbatim at 04:26Z: "`pool_size 0` **HUNG the tool forever** (a zero-thread claypoole pool
never completes; killed at 600 s) — worse than the review's 'untyped throw'; `pool_size 4096` really
started 4096 platform threads." The reviewer predicted a throw; the executed probe found a hang.

**Rule:** carry the probe forward, not the verdict. Name its post-conditions, demand its output, and
treat an executed red as evidence that outranks a predicted one.

---

## 12. Poll the fleet after each result, then reconcile — never pick one

**Moment:** Opus consult launched 2026-09-03T04:04:02Z; Sol-2 (with a shell) filed 04:25Z;
reconciliation written 04:37Z.

Gene's memory-design question went to Sol *and* an Opus agent independently, each told the other was
coming: "You stand in for Sol tonight; **a Sol answer will arrive later and be reconciled with
yours, so be specific enough to disagree with**." Both were required to *measure* before designing,
from a scratch clone, explicit `-Xmx`, JVMs one at a time.

They converged on the coefficient independently — Sol 44.7 heap bytes per source byte, outlines
14.9 KiB/file; Opus 48.4×, 12.7–13.5 KB/file — which is what makes the places they *differ*
informative. The reconciliation (`2026-09-03-memory-design-reconciled.md`) is a per-topic ruling
table, not a merge:

| topic | Sol-2 | Opus | ruling |
|---|---|---|---|
| the missing control | unified admission (files, bytes, entries, depth) as MEM-002 | `max_aggregate_bytes` from `File.length()` during the walk, refuse before parse | same thing; **Opus's evidence (450 × 1.9 MB = 855 MB passed both per-file ceilings) is the OOM's cause** |
| outline double parse | noted as why 44.7× is a lower bound | measured: 21% wall, 31% allocation; 76 MB garbage per 52 KB | new leaf **MEM-015** — cheapest win, ships first |
| battery pass line | `reserved_peak ≤ 192 MiB`; `peak ≤ min(start+224, 0.8×Xmx)` | min-`Xmx` ladder; no `-Xmx` by judgement | **union**: Sol's numeric lines are the gate, Opus's ladder the capacity row |

Only one consult produced MEM-015; only one produced the aggregate-bytes diagnosis of the actual
OOM. Averaging would have lost both.

**Rule:** ask K independently, tell each a rival answer is coming, make both measure, publish a
per-topic ruling table. The disagreement is the finding; a synthesis that hides it has thrown away
what the second call was for.

---

## The through-line

Every technique above is one shape: **the delegated artifact is examined at a source that could not
have been written by the thing making the claim.** The suite's own bytes, not the summary. The
remote's ref, not the push's exit code. The diff's added lines, not "restored." The watcher's
transcript, not the driver's memory. The server's telemetry, not the agent's notes. The second
model's executed probe, not the first model's verdict. Where no such source exists — the fold-diff
tool's Postgres path on a box with no Postgres — the receipt says so verbatim: "Unverified without
Postgres, stated: the new `SELECT COALESCE(MAX(seq),0)` has never executed."

That is what "caring about its methods and timings as if we had done the work ourselves" reduces to
in commands.
