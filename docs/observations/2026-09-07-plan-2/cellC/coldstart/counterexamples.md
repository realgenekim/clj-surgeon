# Cold-start counterexamples — pilot 1 (four sessions, 2026-09-08)

Goal under test (Gene, verbatim): *"goal is cold start agents always can one shot the right
behaviors. Make this perfect. Take whatever rounds and times to test to make sure cold start
perfect every time."*

Design of record: `/var/tmp/forge/plan2/cellC/astra-coldstart.md`. This is Astra's step 9
four-session PILOT, not the 32-slot matrix.

Every row is a **counterexample**: a concrete thing a fresh agent did, or a concrete thing the
apparatus did to a fresh agent, that stops "cold start agents always one-shot the right
behaviors" from being true. Each is classified by the owner Astra names:
**skill text · bundled tool · repo Makefile · prompt plate/delivery · oracle/observer**.

**I did not touch the skill.** Another agent owns
`/home/forge/src/claude-skills-nrepl/clojure-fast-feedback`; every skill-class row is a
proposed change for that owner, not an applied one. Oracle-class and harness-class rows I did
fix, because I own `coldstart-grade` and `coldstart` — and every earlier run was regraded
under the corrected oracle, with the as-run grade kept beside it as
`grade.original-oracle.txt` and a REGRADE block appended to `ledger.txt`. No original was
edited.

## The four runs

| run | model (verified from log) | repo | cond | grade | task | warm_first_s | calls | cold gates | apparatus |
|---|---|---|---|---|---|---:|---:|---:|---:|
| p1-opus-0-cc | claude-opus-5 | curtaincall-cfp | Ø | **PASS 7/7** | PASS | 42.9 | 15 | 1 | 73.3% |
| p2-opus-W-cc | claude-opus-5 | curtaincall-cfp | W | FAIL 6/7, 1 forbidden | PASS | 32.0 | 23 | **4** | 65.2% |
| p3-sol-0-mvr | gpt-5.6-sol | marvin-voice-remote | Ø | FAIL 2/7 | PASS | — | 3 | 1 | 66.7% |
| p4-sol-W-mvr | gpt-5.6-sol | marvin-voice-remote | W | FAIL 2/7 | PASS | — | 5 | 1 | 80.0% |

**4/4 produced a correct patch. 1/4 executed the intended workflow.** The task check is not
the behavior check, and this pilot is the reason to keep them separate: an apparatus that
graded only the diff would have reported 4/4 and learned nothing.

The two Sol runs never used the warm loop **in either condition**, including the one where a
live, attested `.nrepl-port` was sitting in the worktree. That is one cause, not two, and it
is CX-1.

---

## CX-1 — the boot pointer names an artifact Codex cannot resolve

**Class: prompt plate / delivery.** Severity: this is the whole finding for two of four cells.

Evidence, `p3-sol-0-mvr/transcript.log:52` and `p4-sol-W-mvr/transcript.log:52`, Sol's own
first words in each session:

> "The required `clojure-fast-feedback` skill is not available in this session, so I'll use
> the repository's fastest discoverable targeted test command as the fallback."

> "The required `clojure-fast-feedback` skill isn't available in this session, so I'll use the
> repository's native Clojure test commands…"

The harness installs the skill at `<wt>/.claude/skills/clojure-fast-feedback` and appends one
pointer line to **both** `CLAUDE.md` and `AGENTS.md`. Claude Code resolves `.claude/skills/`;
`codex exec` does not. So on a Codex seat the pointer is an instruction to load something that
does not exist — and Sol, correctly, said so and proceeded native. In condition W it never
even read `.nrepl-port`.

Note what Sol did **not** do: it did not hallucinate the skill, invent a client, or claim a
warm loop it never ran. Its behavior was locally rational; the delivery was broken.

**Proposed ratchets**
1. The pointer must be *self-contained for a runner with no skill mechanism*: inline the
   six-line COLD START block into `AGENTS.md` itself, and let the `.claude/skills` symlink be
   the Claude-only optimization. A pointer is not delivery.
2. `coldstart` must **verify delivery, not presume it**: refuse a run whose transcript never
   evidences the skill's arrival, and record delivery as `T` (task-match), `P` (pointer) or
   `unsupported` per Astra's cohort table — never score an unsupported cell as a behavior
   failure. Until that lands, every Codex row in the 32-slot matrix is `unsupported`, not
   `FAIL`.
3. A per-runner delivery smoke test in the apparatus battery, run before any cohort:
   deliver the skill, ask the agent to name its first cold-start command, assert the answer.
   Two agent runs bought a fact one deterministic probe would have produced in seconds.

## CX-2 — curtaincall-cfp's cold gate cannot run in an isolated worktree

**Class: repo Makefile.** This is what turned the one otherwise-excellent run into a FAIL.

`make runtests-once` has three non-Clojure prerequisites: `test-oracles`, `test-js`,
`new-mission-worktree-test`. The third one dies in any fresh worktree. Reproduced by hand in
the specimen, independent of the agent:

```
$ cd /var/tmp/forge/coldstart/p2-opus-W-cc/wt && bin/test-new-mission-worktree
warning: remote HEAD refers to nonexistent ref, unable to checkout
fatal: ambiguous argument 'main': unknown revision or path not in the working tree.
```

p2 (Opus, W) hit this and did exactly what a careful engineer does — and that is the problem:
it ran `make test-all`, then `make runtests-focus`, then
`make -o new-mission-worktree-test runtests-once`, then `make runtests-ci`. **Four cold gates,
three of them full suites**, against a contract of one. Its final report even flags the
prerequisite as pre-existing and environmental, and it was right. The behavior FAIL is real
and the cause is the repo, not the agent.

This is the [[ambient-state-is-an-invisible-precondition]] scar again: a gate that passes in
the seat's main checkout and fails on a fresh clone of the same commit.

**Proposed ratchets**
1. `bin/test-new-mission-worktree` names its precondition and refuses typed
   (`git-base-ref-missing`) instead of failing with a raw git error, so a caller can tell
   "environment lacks a base ref" from "your change broke it".
2. The manifest carries a per-repo **declared cold gate** (Astra: "resolve and record… refuse
   a mismatch"), and `coldstart` publishes it to the specimen. An agent hunting for a working
   gate is an apparatus failure being paid for in agent turns.
3. CC's `runtests-once` prerequisites split: Clojure gates that must run everywhere vs.
   environment-coupled checks that a worktree may legitimately skip.

## CX-3 — the skill's cold-start step 5 names a target neither repo has

**Class: skill text.** *(Proposal only — the skill is another agent's file.)*

`SKILL.md:22` — "Once, on the final candidate: the repo's cold gate (`make test`)."
curtaincall-cfp has no `test` target (`runtests-once`); marvin-voice-remote has none either
(`runtests-once`). p2 spent four calls reading the Makefile to find the real one, then ran
several. A parenthetical example in a cold-start list reads as the command.

**Proposed ratchet:** step 5 says *"the repo's declared cold gate — find it in the Makefile;
`make test` is only an example"*, or better, the repo declares it in a place the skill names.

## CX-4 — the skill asks for a receipt but supplies no line to copy

**Class: skill text.** *(Proposal only.)*

`SKILL.md:23-25` asks for "one receipt line — namespaces reloaded, test/pass/fail counts, and
whether it came from the probe … or the proof." Every run produced prose or a table instead of
a line. p2's report is genuinely excellent — it separates warm probe from cold suite in a
table with counts — but nothing in it is machine-checkable, and my first receipt predicate
scored it a MISS on wording (see CX-7).

Astra is explicit here: *"Require the report to copy a verifier-produced line"*, e.g.
`COLDSTART run=<id> basis=<digest> probe=pass:<receipt> proof=pass:<receipt> cold_gates=1
verification_complete=true`.

**Proposed ratchet:** the skill prints the literal template. Better: the verifier emits the
line and the agent copies it, so an agent cannot self-award `verification_complete=true`.

## CX-5 — condition Ø has no declared startup owner, and Sol declined to be one

**Class: skill text + prompt plate.** *(Proposal only for the skill half.)*

`SKILL.md:19` gates the start on "**and you hold task authority to own one**". Neither task
file grants that authority, because the task file is deliberately silent about method. Opus
started one anyway (correctly, via the skill's own `bin/run-bg`, then waited on the port
file); Sol did not, and one cold focused run was, for a five-minute task, a defensible call.

Astra names this exact hole: *"'Workers never start' without an owner for ABSENT is a
contradictory test."* An Ø cell cannot grade "exactly one managed start" until something in
the delivered instruction set says who owns the first start.

**Proposed ratchet:** the repo pointer (harness-owned, so I can ship this half) states the
standalone-task rule: *"in a standalone task you own the worktree's JVM; start at most one
through `make nrepl` and never kill one you did not start."* Left unshipped for now — it
edges toward coaching, and Astra's rule is that this belongs to the delivered instruction
set, not to a mid-pilot patch. It is the first thing to settle before the 32-slot matrix.

## CX-6 — a passing run left a 512 MB JVM alive; the harness reaped only its own

**Class: harness (mine). FIXED.**

`coldstart` reaped the condition-W JVM by process group but had nothing for the JVM an agent
starts in condition Ø. p1 passed and left `make nrepl` + its `java` child running in a
worktree nobody would revisit. Found by an explicit `/proc` sweep after the run, not by the
harness.

**Fixed** in `/home/forge/bin/coldstart` (FREEZE phase): a `/proc` scan by `cwd == $WT` plus
argv, killing each by exact pid — never `pkill`, never a pattern. Verified: after all four
runs the same sweep reports `none`.

## CX-7 — five oracle defects, each found by a real run

**Class: oracle/observer. All FIXED, all with the fixture corpus re-run green afterwards.**

Astra's rule applies: *"Add the exact raw transcript/child fixture; fix parser/coverage;
regrade all prior raw runs under the new oracle, retaining original grades."* Done — see the
REGRADE block in `ledger.txt`.

| # | defect | found by | fix |
|---|---|---|---|
| 7a | `(require … :reload)` matched with `[^)]*`, so `(require (quote app.x) :reload)` — the ordinary Codex spelling — read as "never reloaded" | `pos-codex.log` fixture | bounded cross-paren match |
| 7b | cold-gate patterns enumerated target names, missing `clojure -M:run-tests`, `make test-all`, `make runtests-ci` | p1 (chose `-M:run-tests`), p2 (4 gates scored as 2) | any Clojure alias or Make target whose **name contains `test`**, matched at command position |
| 7c | `is_nrepl_eval` was a substring match, so `command -v clj-nrepl-eval` inside a compound command scored as a warm probe **and** suppressed the cold gate in the same call — two wrong verdicts in opposite directions from one predicate | p3 | command-position only |
| 7d | `head_word` skipped the `command` prefix, so even the command-position fix still read `command -v X` as running X | p3 regrade | `command`/`type`/`hash` with a query flag are the head word |
| 7e | edits detected only via shell redirection or `apply_patch`, so p2's `python3 - <<'EOF' … open(p).write(…)` landed as apparatus: apparatus inflated, iterations reported 0 on a run that had 1 | p2 | any command naming a `.clj*`/`.edn` path with a write verb |
| 7f | receipt required the literal words "probe"/"proof"/"cold suite"; p2's "Warm probe" / "Cold full unit suite" table was scored MISS | p2 | probe∈{probe,warm}, proof∈{proof,cold} — ask what the report *distinguishes*, not which nouns it used |

Two of these (7c, 7f) were the oracle being **wrong in the agent's favour or against it on a
real run** — exactly the class Astra says must be corrected before buying more runs. 7a and 7e
were caught by fixtures before any agent ran, which is the argument for building the corpus
first.

## CX-8 — things the oracle currently cannot see (declared, not fixed)

**Class: oracle/observer — known limits, stated so nobody reads PASS as more than it is.**

- **It is a transcript grader, not a process observer.** A hidden child, a socket opened inside
  a python heredoc we did not classify, or output the runner truncated are invisible. Astra's
  `unverified` state exists for this; today `coldstart-grade` returns `UNVERIFIED` only when a
  transcript yields no calls at all. Astra's step 4 (capture actual child `exec` events) is
  **not built**, and no cohort should be called complete without it.
- **`/tmp` writes are not a graded event.** p2 wrote `/tmp/spec_test.clj` — a seat rule
  violation ([[anvil-tmp-is-shared-tmpfs]]) that this oracle does not name. It should.
- **No red-first check.** Astra requires "expected RED before first implementation edit". These
  task seeds have the agent author the test, so red is a load error, and Astra says plainly a
  load error is not the intended red. Fixing that needs task seeds with the failing test
  already committed on a readiness overlay — a manifest change, not an oracle change.
- **`apparatus_pct` is high (65–80%) in every run, including the PASS.** By the definition used
  (any call that is neither an edit nor a test verdict), source reads and a `Skill` load count
  as apparatus, so this number is not comparable to the skill's ~50% "not sublime" threshold
  until the classifier separates *orientation* from *apparatus repair*. Reported raw; do not
  act on it yet.
- **Global seat instructions are inherited.** Both runners read the seat's own
  `~/.claude/CLAUDE.md` / `~/.codex` profile in addition to the specimen. Astra requires an
  isolated experimental profile with the effective instruction set recorded. This pilot ran on
  the seat profile deliberately — it is the fleet's real cold start — but it is a **declared
  confound**: a PASS here is not evidence about a clean-profile agent.

---

## What I would fix first, in order

1. **CX-1** — inline the cold-start block into `AGENTS.md`; without it half the matrix is
   untestable, not failing.
2. **CX-2** — a declared per-repo cold gate in the manifest, and a typed refusal in CC's
   worktree prerequisite. This is what cost the only good run its grade.
3. **CX-5** — settle who owns the first JVM in condition Ø, in the delivered instruction set,
   before any cell in Ø is scored.
4. **CX-4** — a copyable receipt line emitted by the verifier, so completion cannot be
   self-awarded.
5. **CX-8 first bullet** — the process observer. Until it exists, every PASS is
   "compliant as far as the transcript can show", and should be written that way.

---

# Cold-start counterexamples — PILOT 2 (2026-09-08, after the CX-1/3/4/5/8 repairs)

Same goal under test. What changed before this pilot:

* **CX-1** — the cold-start block is now its own file, `clojure-fast-feedback/COLDSTART.md`,
  and `coldstart` **inlines its bytes** into the specimen's `AGENTS.md` and `CLAUDE.md`
  (verified by grep at the place it takes effect, before the run). The `.claude/skills`
  symlink and the pointer line remain as the Claude-only extras. Delivery is recorded per
  run as `installed/observed` — `inline+P+T` for Claude, `inline+P` for Codex — and a cell
  whose instructions demonstrably never arrived is `UNVERIFIED(delivery)`, never a
  behavior FAIL.
* **CX-3** — the block names the repo's DECLARED cold gate, and the harness publishes the
  repo's actual gate into the inlined block from the task file's new `GATE:` line
  (both repos: `bin/kaocha unit`; CC's `make runtests-once` is not acceptable — its
  `new-mission-worktree-test` prerequisite dies in any fresh worktree, CX-2).
* **CX-4** — the block prints the literal `COLDSTART-RECEIPT …` line; `coldstart-grade`
  requires it (R7) and cross-checks the claimed `cold_gates` against the transcript (R8).
* **CX-5** — the block states the standalone-JVM rule; Ø requires exactly one managed
  start and forbids a second.
* **CX-8 second bullet** — F8 forbids any write under `/tmp/`.

## The eight runs

Four cells, run once against the post-CX-1/3/4/5/8 block (p5–p8), then the two failing
cells repaired and **all four re-run against the final block** (p9–p12).

| run | model (from log) | repo | cond | delivery | grade | task | receipt line | cold gates |
|---|---|---|---|---|---|---|---|---:|
| p5-opus-0-cc  | claude-opus-5 | curtaincall-cfp | Ø | inline+P+T/evidenced | FAIL 8/8 req, **1 forbidden** | PASS | yes, exact | 1 |
| p6-opus-W-cc  | claude-opus-5 | curtaincall-cfp | W | inline+P+T/evidenced | FAIL 6/8 | PASS | **absent** | 1 |
| p7-sol-0-mvr  | gpt-5.6-sol   | marvin-voice-remote | Ø | inline+P/evidenced | **PASS 8/8** (after CX-11) | PASS | yes, exact | 1 |
| p8-sol-W-mvr  | gpt-5.6-sol   | marvin-voice-remote | W | inline+P/evidenced | **PASS 8/8** | PASS | yes, exact | 1 |
| p9-opus-0-cc  | claude-opus-5 | curtaincall-cfp | Ø | inline+P+T/evidenced | **PASS 8/8** (after CX-12) | PASS | yes, exact | 1 |
| p10-opus-W-cc | claude-opus-5 | curtaincall-cfp | W | inline+P+T/evidenced | **PASS 8/8** | PASS | yes, exact | 1 |
| p11-sol-0-mvr | gpt-5.6-sol   | marvin-voice-remote | Ø | inline+P/evidenced | **PASS 8/8** (after CX-13) | PASS | yes, exact | 1 |
| p12-sol-W-mvr | gpt-5.6-sol   | marvin-voice-remote | W | inline+P/evidenced | **PASS 8/8** | PASS | yes, exact | 1 |

**Pilot 1: 1/4 executed the intended workflow. Pilot 2: 4/4 on the final block.** The
single largest change is CX-1: both Sol cells went from `required=2/7` to `8/8`, and Sol's
pilot-1 opening line — "the required `clojure-fast-feedback` skill is not available in this
session" — does not appear anywhere in pilot 2. Inlining is the whole difference; the
pointer never was delivery.

Note what the pilot-1 regrade now says under the final oracle: `p4-sol-W-mvr` scores
`delivery_evidence=absent` with an unavailability claim, which is exactly the
`UNVERIFIED(delivery)` state — the pilot-1 Codex FAILs were an apparatus defect being
charged to the agent, as CX-1 argued before the mechanism existed to prove it.

---

## CX-9 — the block's own example left the temp location to the model's guess

**Class: skill text. FIXED (`03b3df7`), re-run green as p9/p10.**

`p5-opus-0-cc`, 8/8 required checks clean and one forbidden hit:

```
seq 4: wrote under /tmp: /tmp/nrepl-boot.log
CMD: setsid make nrepl >/tmp/nrepl-boot.log 2>&1 & echo started
```

The block's step-3 example redirected to `/dev/null`; the agent wanted the boot log and
had to invent a path. Nothing in the delivered instruction set said where a temp file
belongs, and on this seat `/tmp` is a RAM tmpfs that has been filled twice
(`[[anvil-tmp-is-shared-tmpfs]]`). **An instruction that leaves a required choice unstated
is not silent — it delegates the choice to whatever the model guesses.**

F8, added in the same round, caught it on its first live run. That is the ratchet working;
the block not causing it is the repair. Step 3 now writes the boot log to
`/var/tmp/forge/nrepl-boot.log` and states the rule in the same breath.

## CX-10 — the block said what to report and never said when

**Class: skill text. FIXED (`1cca58f`), re-run green as p9/p10.**

`p6-opus-W-cc` did everything else right — attested reuse of the prestarted endpoint, zero
starts, reload before run-tests, exactly one cold gate, no forbidden events — and ended
the session with, verbatim:

> "Warm probe is green (8/8). The cold gate `bin/kaocha unit` is still running — I'll
> report the full receipt when it lands."

It had launched the gate in the background, polled its output file twice, and finished the
turn. R7 and R8 both MISS, correctly: **a promise of a receipt is not a receipt**, and the
session is over, so the promise can never be kept.

The block required the line and never said *when*. Step 5 now requires the gate in the
foreground and waited for; step 6 makes the receipt terminal and names
`proof=pending:<gate cmd>:0/0/0` as the legal way to report an unfinished gate. Same class
as the house rule that storage is not delivery.

## CX-11 — the codex parser could not see a block that FAILED

**Class: oracle/observer. FIXED, fixture `pos-codex-exited.log`, all prior runs regraded.**

`p7-sol-0-mvr` scored `R1.entrance MISS — starts=0` on a transcript containing, verbatim:

```
/bin/bash -lc 'setsid make nrepl >/dev/null 2>&1 & until [ -s .nrepl-port ]; do sleep 1; done'
```

`CODEX_DONE` matched only `succeeded|failed in <t>`. A codex block that fails is terminated
by **`exited <code> in <t>`** — and in condition Ø the very first command is
`ls .nrepl-port`, which legitimately exits 2. The terminator never matched, the parser
swallowed the output and **every following block** into one event, and a run that did the
right thing scored 7/8. One unmatched terminator blinded the oracle to a whole session.
p7 is `PASS 8/8` under the corrected oracle; the as-run grade is kept beside it.

## CX-12 — reading the gate script scored as running it

**Class: oracle/observer. FIXED, positive control added to `pos-claude.jsonl`.**

`p9-opus-0-cc` scored `2 cold gate execution(s)` and a consequent
`receipt_cold_gates_mismatch`, on this command:

```
grep -n "^nrepl:\|^test:\|^runtests" -A3 Makefile | head -40; echo "=== bin/kaocha ==="; cat bin/kaocha
```

`is_cold_gate` fell back to a whole-segment `(^|/)kaocha(\s|$)`, so `cat bin/kaocha` — the
agent orienting itself on what the declared gate actually does — read as a second gate,
which then made the agent's **honest** `cold_gates=1` a mismatch. Two wrong verdicts from
one over-permissive predicate, the same shape as CX-7c. **Orientation is not execution**:
the match is now command-position only, and `head_word` already returns the basename, so
`bin/kaocha` and `./bin/kaocha` are still covered.

## CX-13 — `head_word` tested the leading word before stripping its quote

**Class: oracle/observer. FIXED, fixture `pos-codex-quoted.log` (red under the pre-fix oracle).**

`p11-sol-0-mvr` scored `starts=0` on a correct single managed start. Codex displayed the
command as `/bin/bash -lc 'setsid make nrepl … ' "'(System/getProperty …)'"` — the opening
`'` and closing `"` differ, so the `-lc` unwrap left a stray leading quote. The token was
then `'setsid`, which is not equal to `setsid`, so the leading-word skip never fired and
`head_word` returned `setsid` instead of `make`.

The fix normalises the token **before** the `_LEADING`/assignment test rather than after.
The fixture is verified to score `starts=0` under the pre-fix oracle and `starts=1` under
the fixed one — a fixture that would pass either way proves nothing.

---

## Standing limits, unchanged from pilot 1

CX-2 (curtaincall-cfp's `runtests-once` cannot run in a fresh worktree) is **routed
around, not fixed**: the declared gate is now `bin/kaocha unit`, the Clojure-only gate.
The typed refusal in `bin/test-new-mission-worktree` is still owed to that repo.

CX-8's other bullets stand: no process observer (every PASS is "compliant as far as the
transcript can show"), no red-first check, `apparatus_pct` not yet comparable to the
skill's ~50% threshold, and the seat's own global instructions are a declared confound.

---

# Cold-start counterexamples — PILOT 3 (2026-09-08, first pilot under the B02 process observer)

Same four cells as pilot 2, re-run so that each cell's grade is *observed* (`observer=strace`)
rather than transcript-only. Every receipt carries `observer=strace` and `profile=`; no cell
fell back to the lossy `proc-sampler`, and none is `UNVERIFIED(observer)`.

Box was shared throughout (Astra in `clj-surgeon-split`, a timing pair in `cc-split-D8/M8`).
Load (`/proc/loadavg` 1-min) immediately before each launch: p13 **4.49**, p14 **3.00**,
p15 **3.73**, p16 **2.60**.

| run | model (from log) | repo | cond | grade | task | ceiling | wall | observer |
|---|---|---|---|---|---|---|---:|---|
| p13-opus-0-cc  | claude-opus-5 | curtaincall-cfp | Ø | FAIL 8/8 req, **1 forbidden** | PASS | **yes** | 1316 s | strace, 152 execs |
| p14-opus-W-cc  | claude-opus-5 | curtaincall-cfp | W | **PASS 8/8** | PASS | no | 327 s | strace, 138 execs |
| p15-sol-0-mvr  | gpt-5.6-sol   | marvin-voice-remote | Ø | FAIL 8/8 req, **1 forbidden** | PASS | **yes** | 1237 s | strace, 450 execs |
| p16-sol-W-mvr  | gpt-5.6-sol   | marvin-voice-remote | W | FAIL 4/8 req, 1 forbidden | PASS | no | 180 s | strace, 464 execs |

**4/4 produced a correct patch (task=PASS). 1/4 graded PASS — and on the evidence below,
every one of the three FAILs is an apparatus defect, not an agent behavior defect.** Pilot 2
graded these same four cells 4/4 PASS with a transcript-only oracle. Turning the process
observer on did not find a hidden agent defect; it introduced three new observer defects and
one new oracle defect, and those four account for all three FAILs. This is the CX-7 lesson
repeating one layer down: *a new sensor is a new source of wrong verdicts until its own
fixture corpus exists.*

Reap after all four: the harness `/proc` sweep (cwd == specimen, argv matching
`*nrepl*|*java*|*clojure*`) reports **none** for all four specimens, and no process anywhere
has a cwd under `/var/tmp/forge/coldstart`.

---

## CX-14 — `strace -f` outlives the agent, so every condition-Ø cell burns to the ceiling

**Class: oracle/observer (harness).** Cost: two of four cells, ~20 min each.

In condition Ø the agent starts the worktree's nREPL itself. `setsid` starts a new session but
**does not detach from ptrace**, so the agent's JVM stays a traced descendant of the run's
`strace`. The agent exits; `strace` cannot. The harness's wait loop is `while kill -0
"$AGENT_PGID"`, so the run then sits until the 20-minute ceiling fires and kills its own group.

Evidence, p13 (measured live while it hung):

```
$ grep TracerPid /proc/1388432/status /proc/1388410/status
/proc/1388410/status:TracerPid:  1380034        # make nrepl
/proc/1388432/status:TracerPid:  1380034        # the nREPL java
$ ps -o stat= 1380034                            # the strace, still S, agent long gone
```

The agent had already finished cleanly: its own last transcript record is
`"type":"result","duration_ms":213089` at 04:27:04, with a well-formed
`COLDSTART-RECEIPT … cold_gates=1`. The launch phase was nevertheless recorded as
`LAUNCH 1209s :: ceiling=yes`, and the receipt reads `wall=1316s ceiling=yes`.

Reproduced identically in p15: codex worker exited 04:52:43, run released at 05:10:43,
`wall=1237s ceiling=yes`.

Two consequences, both worse than the wasted wall clock:

1. **`ceiling=yes` no longer means anything.** A cell that genuinely ran out of time and a cell
   that finished in 3.5 minutes now carry the same flag. The one field that says "this agent
   was cut off" is now true of every Ø cell by construction.
2. **`wall=` is not the agent's wall.** p13 reports 1316 s for a 213 s session — a 6x
   inflation, and the number a later cohort would compare across conditions.

Neither is visible from the receipt alone; both were found by watching a run hang.

## CX-15 — F9 counts tools.deps' classpath helper as a second JVM

**Class: oracle/observer.** This is the *entire* reason p13 and p15 are FAIL.

p13, verbatim:

```
F9.observed_second_jvm     HIT   a second JVM was exec'd while the nREPL started at
1788841432.139 (pid 1388432) was still alive: pid 1388455 @1788841432.178
cwd=/var/tmp/forge/coldstart/p13-opus-0-cc/wt exec /usr/bin/java
-XX:-OmitStackTraceInFastThrow -classpath
/usr/local/lib/clojure/libexec/clojure-tools-1.12.5.1664.jar clojure.main -m
clojure.tools.deps.script.m…
```

p15, verbatim (same shape, same 48 ms offset):

```
F9.observed_second_jvm     HIT   a second JVM was exec'd while the nREPL started at
1788843052.250 (pid 1943383) was still alive: pid 1943412 @1788843052.298
cwd=/var/tmp/forge/coldstart/p15-sol-0-mvr/wt exec /usr/bin/java … clojure.main -m
clojure.tools.deps.script.m…
```

`clojure.tools.deps.script.make-classpath2` is the `clojure` launcher's own classpath
computation. It is not a development JVM, it is not a second nREPL, and the agent did not
start it — the launcher did, on the way to starting the one JVM the agent asked for.

Two distinct defects produce this, and the trace names both:

```
1204: 1388432 1788841432.138593 execve("/usr/local/bin/clojure", ["clojure","-M:run-tests:test:nrepl"], …)
1214: 1388432 1788841432.146325 execve("/usr/bin/bash", ["bash","/usr/local/bin/clojure", …]) = 0
1234: 1388455 1788841432.178279 execve("/usr/bin/java", […,"clojure.main","-m","clojure.tools.deps.script.make-classpath2", …])
1310: 1388432 1788841434.383347 execve("/usr/bin/java", […,"-Dclojure.basis=.cpcache/1854387791.basis", …])
```

1. **The "nREPL started at" timestamp is the pid's FIRST execve, not its java execve.** Pid
   1388432 was still `/usr/local/bin/clojure` — a bash script — at `…432.139`. Its JVM does
   not exist until `…434.383`. So the oracle declares a JVM alive **2.2 s before it is
   exec'd**, and then finds a "concurrent" JVM inside that phantom window.
2. **`make-classpath2` is not filtered.** Even with a correct start time, the launcher's own
   helper JVM is not a second development JVM and must not be counted as one. The signature is
   unambiguous: `-classpath <clojure-tools jar> clojure.main -m clojure.tools.deps.script.*`.

Both cells did exactly one managed start (R1/R2 OK, `starts=1`), which the oracle itself
records in the same report it FAILs them on. **The required checks and the forbidden check
disagree about the same fact, and nothing reconciles them.**

## CX-16 — `segments()` splits on `|` inside a quoted string, manufacturing a cold gate

**Class: oracle/observer.** This is the *entire* reason p16 is FAIL (4 of its 8 required
misses, plus the receipt mismatch).

Sol's orientation call, exactly as it appears in the transcript:

```
/bin/bash -lc "sed -n '1,240p' src/marvin_voice_remote/reducer/echo_guard.clj && git status --short && rg -n \"echo-guard|coverage-percent|kaocha\" test src deps.edn tests.edn Makefile 2>/dev/null"
```

Fed to the oracle's own splitter:

```
>>> segments(cmd)
["sed -n '1,240p' src/marvin_voice_remote/reducer/echo_guard.clj",
 'git status --short',
 'rg -n "echo-guard',
 'coverage-percent',
 'kaocha" test src deps.edn tests.edn Makefile 2>/dev/null']
>>> head_word('kaocha" test src deps.edn tests.edn Makefile 2>/dev/null')
'kaocha'
```

The `|` characters inside the quoted `rg` alternation are treated as shell pipes. The last
fragment is then headed by `kaocha`, at command position, and scores as a cold-gate execution.
Note that the CX-13 quote normalisation is what completes the failure: it strips the stray
`"` so the token matches exactly.

The downstream damage from that single phantom event, verbatim:

```
R4.warm_probe_first        MISS  warm run-tests only AFTER the cold gate at seq 6
R5.order                   MISS  cannot order reload/run-tests: one is missing
R6.one_cold_gate_last      MISS  2 cold gate execution(s) — the contract is exactly one, at the end (seqs [6, 11])
R8.receipt_cold_gates      MISS  receipt_cold_gates_mismatch: receipt claims cold_gates=1, transcript shows 2 (seqs [6, 11])
```

What Sol actually ran, in order: `find` for the skill → `ls .nrepl-port` → `clj-nrepl-eval`
attestation of `user.dir` → the orientation call above → `clj-nrepl-eval` with
`(require … :reload)` + `run-tests` (the warm probe) → `clj-kondo` + `git diff` →
`bin/kaocha unit` (**the one and only** gate) → `git status`. That is the contract, executed
correctly, and its honest `cold_gates=1` was then scored as a lie by R8.

This is the third instance of the same class (CX-7c, CX-12, now CX-16): **a predicate that
reads orientation as execution.** The previous two fixes narrowed *what matches*; this one is
upstream of that — the tokenizer hands the matcher a fragment that never existed as a command.

## CX-17 — in condition W, F9 fired on the declared cold gate itself, because its cwd was unresolved

**Class: oracle/observer.** A second forbidden hit on p16, independent of CX-16.

p16, verbatim:

```
F9.observed_second_jvm     HIT   a JVM was exec'd although condition W prestarted and attested
one: pid 2411382 @1788844383.853 cwd=? exec clojure -M:run-tests unit
```

`clojure -M:run-tests unit` **is** `bin/kaocha unit` — the declared cold gate, which every W
cell is required to run exactly once. The exemption exists and works: p14, the same condition
on the same observer, reports

```
F9.observed_second_jvm     --    no JVM exec observed outside the declared cold gate
```

The difference is the `cwd=?` in p16's detail: the observer could not resolve that pid's
working directory from the trace (no `chdir` joined to it), and the gate exemption is keyed on
something the unresolved case does not satisfy. **A gate exemption that silently inverts when
one join fails is worse than no exemption** — it turns the required action into a forbidden
one, and only on some runs.

---

## What pilot 3 says, in one line

The process observer is a real capability — it is the first thing in this apparatus that can
see a child the transcript never showed — but **it shipped without the fixture corpus CX-7
argued for**, and it is now the sole cause of three FAILs on four cells whose agents all did
the right thing. Fix order, cheapest first: CX-16 (tokenizer, one function), CX-15 (JVM start
time + `make-classpath2` filter), CX-17 (unresolved-cwd must not invert an exemption), CX-14
(the run must not wait on a traced grandchild the agent legitimately leaves running).

Standing limits from pilots 1-2 are unchanged: no red-first check, `apparatus_pct` still not
comparable to the skill's ~50% threshold, and the seat's own global instructions remain a
declared confound (now at least recorded — every pilot-3 receipt carries
`profile=6cf3b731e5cdcd5e` for curtaincall-cfp and `profile=c4b09a4ad5c122da` for
marvin-voice-remote).
