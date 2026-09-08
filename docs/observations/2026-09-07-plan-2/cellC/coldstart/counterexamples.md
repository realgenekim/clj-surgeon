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


---

## pilot 4 (p17-p20) - all four cells REFUSED before launch: the row-1 seeds ship COMPILE-TIME reds

Filed 2026-09-08T08:06:02Z by the acceptance-4 operator (independent Opus). Apparatus hashes:
coldstart `feb8d59aa1b1b21f`, coldstart-grade `1693e4a282c37055`, round `a3bb40098501f5e7`,
round-resume `24688df75c2b211a`. Bases: curtaincall-cfp `8aec4c93c50d6126`
(origin/nrepl/test-alias), marvin-voice-remote `94393708b6312c4d` (origin/nrepl/test-alias),
both fetched immediately before the run.

| Cell | rc | Where it stopped | Wall |
|---|---|---|---|
| p17-opus-0-cc | 3 | READINESS, `red-wrong-reason/parse-or-load-error` | GATE_QUALIFY 84 s, refused at +106 s |
| p18-opus-W-cc | 3 | READINESS, same | GATE_QUALIFY 87 s, refused at +109 s |
| p19-sol-0-mvr | 3 | READINESS, same | GATE_QUALIFY 18 s, refused at +25 s |
| p20-sol-W-mvr | 3 | READINESS, same | GATE_QUALIFY 18 s, refused at +24 s |

**No cell reached LAUNCH. No model ran. No receipt, no grade and no task check exists for
pilot 4**, so there is nothing to grade PASS or UNVERIFIED, and no COLDSTART or
COLDSTART-GRADE line to paste. Each bundle holds its refusal, its gate-qualify log and its
red-qualify log, and nothing else.

### The counterexample, and its owner

**Owner: THE SEED** - `/var/tmp/forge/coldstart/tasks/cc-1.md` with `cc-1.overlay/`, and
`/var/tmp/forge/coldstart/tasks/mvr-1.md` with `mvr-1.overlay/`. NOT `coldstart`, NOT
`coldstart-grade`. The apparatus behaved exactly as B02e item 5 requires and as
acceptance-3 fix 5 demanded; it refused a red it could not attribute.

Both overlays are a test namespace that CANNOT COMPILE, because the var the test calls does
not exist yet:

```
CC  (p17/p18 red-qualify.log)
  ERROR in unit (ns.clj:9)  Failed loading tests:
  Exception: clojure.lang.Compiler$CompilerException: Syntax error compiling at
    (cfp_scheduler_killer/views/format_test.clj:8:17).
  Caused by: java.lang.RuntimeException: No such var: fmt/fmt-percent
  1 tests, 1 assertions, 1 errors, 0 failures.

MVR (p19/p20 red-qualify.log)
  Caused by: java.lang.RuntimeException: No such var: echo-guard/coverage-percent
```

The declared RED var - `fmt-percent-renders-whole-percents` and
`coverage-percent-renders-whole-percents` - appears ZERO times in either red-qualify log
(`grep -c` returns 0). The namespace dies at compile-syntax-check, so the assertion is
never reached, never run, and never red. Kaocha reports `1 errors, 0 failures`: a LOAD
error, not an assertion failure.

**This is not a new discovery; it is acceptance-3 section 5 executing.** Acceptance-3 wrote
that both row-1 seeds' declared reds are compile-time reds (`no such var: fmt/fmt-percent`,
`no such var: echo-guard/coverage-percent`) and that their red-to-green golden proof exists
only as a hand-typed `QUALIFIED:` line in the seed, not as a harness receipt. B02e item 5
turned the old advisory NOTE into a refusal, and the refusal fired on the first real seed
it met. The apparatus is right and the seeds are not ready.

### What the seeds need before pilot 5

1. **An assertion-red, not a load-red.** The overlay must COMPILE against the base and fail
   AT the assertion, so the gate's output names the declared var at a failure site. Two
   shapes that work: resolve the missing var at run time inside the test body, or ship a
   stub of the target var that returns a wrong value so the `is` assertion fails and kaocha
   prints `FAIL in (...-renders-whole-percents)`. Acceptance criterion:
   `red_attested=assertion` in the READINESS line, which is what the harness already prints
   for a well-formed seed.
2. **A `<task>.golden/` directory.** Neither seed has one, so `red_golden=none`: the
   red-to-green proof is still only the hand-typed `QUALIFIED:` lines. With a golden dir the
   harness proves red-to-green in the same run (`red_golden=green`). That path is already
   implemented and corpus-covered by `b02e-red-golden-green` and `b02e-red-golden-not-green`.
3. **Re-qualify both seeds against the CURRENT tips** before the next attempt. A gate
   invocation costs about 85 s on curtaincall-cfp and about 18 s on marvin-voice-remote, so
   a full seed round trip for both is roughly four minutes - far less than one cell of model
   spend.

### Two operator-side apparatus notes recorded with this attempt

- **`coldstart` never re-runs the declared GATE after the agent.** TASK_CHECK runs only
  VERIFY. Pilot pass criterion 3 - the declared VERIFY focus green AND the declared GATE
  green - is therefore not measured by the apparatus, and needs either an explicit operator
  step or an amendment to the criterion.
- **`profile=` is computed once at SPECIMEN and never re-verified**, and nothing binds the
  proof to the tree that is frozen. These are acceptance-4 rows `PILOT-profile-drift` and
  `PILOT-final-source-after-proof`, both FAIL. Any cell that does run must carry operator
  controls C1-C3 from `opus-B02-acceptance-4.md` section 5.

---

## pilot 4b (p21-p24) - all four cells RAN; all four graded FAIL on ONE forbidden check, and that check is a false conviction

Filed 2026-09-08T09:00:39Z. Apparatus hashes: coldstart `eb1f17c7042e863e` (was `feb8d59aa1b1b21f`; this seat's
gate/C1/C2 patch), coldstart-grade `1693e4a282c37055` (UNCHANGED). Bases fetched immediately before the
run: curtaincall-cfp `8aec4c93c50d6126`, marvin-voice-remote `94393708b6312c4d`, both
`origin/nrepl/test-alias`, both identical to the seeds' BASE lines.

**The pilot-4 refusal is closed.** Both row-1 seeds now ship an ASSERTION red, not a load
red: the overlay resolves the target Var at run time (`requiring-resolve`) so the namespace
compiles against the untouched base and dies at `is`. Every cell reached LAUNCH and every
cell carries `red_attested=assertion red_golden=green`.

| Cell | model | cond | repo | grade | required | forbidden | task | gate_final_exit | integrity | agent wall | total wall |
|---|---|---|---|---|---|---|---|---|---|---|---|
| p21-opus-0-cc  | opus(verified) | 0 | CC  | FAIL | 11/11 | 1 | PASS | 0 | ok | 221 s | 610 s |
| p22-opus-W-cc  | opus(verified) | W | CC  | FAIL | 10/11 | 1 | PASS | 0 | ok | 214 s | 590 s |
| p23-sol-0-mvr  | sol(verified)  | 0 | MVR | FAIL | 11/11 | 1 | PASS | 0 | ok | 120 s | 220 s |
| p24-sol-W-mvr  | sol(verified)  | W | MVR | FAIL | 11/11 | 1 | PASS | 0 | ok | 118 s | 202 s |

### CX-18 - F11.observed_tmp_write convicts every compliant cell, because the SANCTIONED entrance writes to /tmp

**Owner: the GRADER (`coldstart-grade`).** Not the agent, not the seed, not `coldstart`.

The one forbidden hit in all four cells is the same, and its three paths are the same in
all four:

```
F11.observed_tmp_write  HIT  3 write(s) under /tmp observed:
  pid ... open /tmp/bbin158
  pid ... open /tmp/clojure-mcp-light/gpid-<worker>-<ts>-proj-<hash>/nrepl/target-127.0.0.1-<port>.edn
  pid ... open /tmp/bbin158
```

The trace names the writer directly (p21, `observer/trace.strace`):

```
2946355 open("/tmp/bbin158", O_WRONLY|O_CREAT|O_TRUNC|O_LARGEFILE, 0666) = 3
2946355 execve("/usr/local/bin/bb", ["bb", "--deps-root",
  ".../org.babashka.bbin/script--693757147-https-github-com-bhauman-clojure-mcp-light-git/...",
  "--config", "/tmp/bbin158", "-m", "clojure-mcp-light.nrepl-eval", "--", "-p", "45705", ...])
2946358 open("/tmp/clojure-mcp-light/gpid-2930132-.../nrepl/target-127.0.0.1-45705.edn", O_WRONLY|O_CREAT|O_TRUNC, 0666) = 9
```

Both writes belong to `clj-nrepl-eval` itself. It is a bbin-installed babashka script:
`bbin` regenerates `/tmp/bbin158` on every invocation, and `clojure-mcp-light` writes its
own nREPL target file under `/tmp/clojure-mcp-light/`. **R1, R3 and R4 REQUIRE the agent to
use that tool.** No cell that satisfies the required checks can avoid the forbidden one.
The agents' own transcripts contain no `/tmp` path at all, which is why the transcript twin
`F8.tmp_write` correctly reads `--` in every cell while its observed sibling HITs.

The defect is a missing exemption, and it is visible in the source: `observed_tmp_writes()`
filters on `RUNTIME_TMP` only (`hsperfdata_*`, `.java_pid*`, `.X11-unix`, `systemd-private*`,
`snap.*`, `.font-unix`, `.ICE-unix`) and never consults `HARNESS_TOOLS`, which every other
observed check does consult (`coldstart-grade` lines 1654, 1662, 1688, 1725, 1801). The
check's own prose says "no **agent** write under /tmp observed" - a claim the predicate
cannot support, because it cannot tell an agent's write from the sanctioned entrance's.

Same class as acceptance-3's FP1/FP2 (F10 convicting AF_UNIX and name resolution) and G1
(`./bin/kaocha` unrecognised as the declared gate): an OBSERVED check with a path/identity
filter narrower than the behaviour it is supposed to license. It is a FALSE CONVICTION, and
it is unfalsifiable from inside a cell: the more correctly an agent uses the entrance the
harness mandates, the more times it fires.

**Ratchet owed (not built here - this seat did not own `coldstart-grade` this round):**
attribute each observed `/tmp` write to its writing process's exec ancestry and exempt
writes whose nearest `execve` ancestor is in `HARNESS_TOOLS` (`bb`/`bbin` under a
`clojure-mcp-light` deps-root, and the `clojure-mcp-light` process itself), exactly as
F9's gate-ownership join already does; then add a negative that keeps a genuine agent
`/tmp` write convicted, and a positive fixture built from one of these four traces.

### The one genuine agent finding: p22 edited before it ever saw the red

`R10.red_first MISS` in p22 is NOT apparatus. The opus cell in condition W read the test
(seq 1), read the src and the endpoint (seq 2-3), **wrote the fix (seq 4)**, and only then
ran its first warm reload+run-tests (seq 5). It never observed
`cfp-scheduler-killer.views.format-test/fmt-percent-renders-whole-percents` red. Its work was
correct - focused verify green, full suite green, diff confined to the named file - but a
green never preceded by a red proves the test runs, not that it discriminates. This is the
first behavioural datum row 1 has from an observed cell, and it is a real one. N=1.

### The three apparatus fixes this round, and what they showed

`coldstart` gained, this round: the declared GATE re-run cold on the agent's FINAL tree
(`gate_final_exit=`, a non-zero value fails the integrity gate); C1, the instruction-profile
digest recomputed at FREEZE over the run's own component list (`profile_drift=`); and C2,
the tracked tree hashed after the proofs and again at the freeze (`src_hash_at_verify=`,
`src_hash_at_freeze=`, `final_source_drift=`). All three now reach `integrity=` and
`NOT CERTIFIED`. Acceptance-4's two FAIL rows, `PILOT-profile-drift` and
`PILOT-final-source-after-proof`, are covered by live production-path rows in
`fixtures/b02f-final-gate.sh`, together with the row acceptance-4 §2 asked for: a focused
VERIFY green over a DECLARED GATE red on the final tree is `task=PASS` and NOT CERTIFIED.

On the four real cells all three read clean: `gate_final_exit=0` (CC 1022 tests / 12402
assertions / 0 failures; MVR 580 / 7842 / 0), `profile_drift=no` with an independent
recompute reproducing the receipt digest and zero drifted components, and
`final_source_drift=no` with `src_hash_at_verify == src_hash_at_freeze` in every cell.
`profile_isolated=no` in all four: the confound is named, per pass criterion 5, never silent.

**Pass criterion (acceptance-3 §5's five parts): 0 of 4 cells pass, on part 2 alone.**
Parts 1, 3, 4 and 5 hold in all four cells. Part 2 fails in all four for the same
grader-owned false conviction, and additionally in p22 for a genuine `R10` miss. A pilot
cannot be called clean on a grader that cannot be satisfied; it also cannot be called a
behaviour result, because three of the four cells were behaviourally compliant on every
check the oracle can actually license.

---

## CX-18 — CLOSED, and pilot 4c (p25): the false conviction is gone; the red-first miss is real and now N=2

Filed 2026-09-08T09:23:52Z by the seat that owns `coldstart-grade` and the fixtures corpus.
Apparatus hashes: `coldstart` `eb1f17c7042e863e` (UNCHANGED — this round did not touch it),
`coldstart-grade` `1693e4a282c37055` -> **`95879522f7ff2335`** (backup kept at
`/home/forge/bin/coldstart-grade.bak.cx18`, byte-identical to the old hash). Base fetched
immediately before p25: curtaincall-cfp `8aec4c93c50d6126` (`origin/nrepl/test-alias`), the
same commit p21–p24 ran on. Task file, overlay, golden and COLD START block bytes all unchanged
(`skill_sha256=fd336d86cb8b32dd block_sha256=4f9a502147f60c66` in both p21 and p25).

### The fix — attribution by exec ancestry, and an exemption that is LISTED

`observed_tmp_writes()` filtered on PATH alone. F11 now attributes every observed `/tmp` write
to the program that made it, by walking the writing process's exec ancestry, and reports the
exempted ones as `tmp_writes_harness=<n>` — in the METRIC table, in the check's own detail with
each path and its attributed program, and on the one-line `COLDSTART-GRADE:` receipt.

Three things had to be true for the join to work at all:

1. **A thread is not a process, and strace reports threads by tid.** `bb` is a GraalVM native
   image: every `/tmp` write the sanctioned entrance makes is made by one of its THREADS.
   `parse_strace` dropped `CLONE_THREAD` clones entirely, so those events had no program, no
   parent, and no way to tell whose they were. Thread-group membership is now recorded
   (`obs["threads"]`, tid -> leader) and is deliberately kept OUT of `parent`, which is the
   process tree every other join in the file walks.
2. **The walk needs a BOUND, or it proves too much.** `coldstart` is itself in `HARNESS_TOOLS`
   and is an ancestor of everything in the trace; an unbounded ancestry walk would exempt every
   write there is. The walk stops at the first program it can NAME: one of our tools -> exempt;
   a shell or interpreter -> the agent's own command, convicted; nothing nameable all the way up
   -> convicted. An unattributable write is not an exonerated one.
3. **An exemption is a forgery surface.** The harness name is read only where identity lives —
   the exec'd path, `argv[0]`, and `argv[1]` (the script an interpreter was handed, which is how
   the kernel re-execs `clj-nrepl-eval` as `bb <that path>`). The any-argv-position test the
   other observed checks use is NOT reused here, so `python3 -c '…' clj-nrepl-eval` buys an agent
   nothing. The bbin marker requires the `/org.babashka.bbin/` deps-root path or a
   `clojure-mcp-light.` module, never the bare `bb` basename — `bb` is an INTERPRETER and
   `bb -e '(spit "/tmp/x" 1)'` is an agent write like any other.

**Two corpus rows, one per direction, both from real bytes.**
`cx18-p21-transcript.jsonl` + `cx18-p21-harness-tmp.trace` are p21's OWN transcript and OWN
2.4 MB trace — the bytes that produced the false conviction: **FAIL under the pre-fix grader,
PASS under this one**, so the fixture would not have passed either way.
`cx18-agent-python-tmp.trace` is a fresh capture made through `capture-adversaries.sh` with
`coldstart`'s own observer flags and its own generated filter: the agent's own
`python3 -c 'open("/tmp/x","w").write("hi")'`, which must and does stay **HIT**. The pre-existing
`astra8-tmp-write` (a bash redirection into /tmp) also stays HIT. **Corpus: 0 mismatches.**

### Regrade of p21–p24 from their frozen traces

Originals byte-untouched; each new report is beside its bundle as
`grade.cx18-regrade.{txt,json}`, and the REGRADE lines are appended to `ledger.txt`.

| cell | grade | required | forbidden | tmp_writes_harness | change |
|---|---|---|---|---:|---|
| p21-opus-0-cc  | **PASS** | 11/11 | 0 | 3 | F11 false conviction lifted |
| p22-opus-W-cc  | FAIL | 10/11 | 0 | 3 | F11 lifted; `R10.red_first` MISS stands |
| p23-sol-0-mvr  | **PASS** | 11/11 | 0 | 5 | F11 false conviction lifted |
| p24-sol-W-mvr  | **PASS** | 11/11 | 0 | 3 | F11 false conviction lifted |

### Pilot 4c — p25, the opus/W cell re-run fresh, uncoached

Same task, same overlay, same block, same base, no coaching, one run.

```
COLDSTART-GRADE: FAIL required=10/11 forbidden=0 warm_first_s=28.0 calls=7 cold_gates=1
  apparatus=57.1% delivery_evidence=evidenced gate_exit=0 starts=0/0 tmp_writes_harness=3
  observer=strace observer_execve=127
```

`integrity=ok`, `task=PASS`, `gate_final_exit=0` (1022 tests / 12402 assertions / 0 failures),
`red_attested=assertion red_golden=green`, `profile_drift=no`, `final_source_drift=no`,
`profile_isolated=no`, diff confined to `src/cfp_scheduler_killer/views/format.clj`,
agent wall 203 s, total 573 s. **The single miss is `R10.red_first`, again.**

p25's own frozen grader was an intermediate `7ec3e34f38cb6384`; regraded under the final
`95879522f7ff2335` the verdict is identical field for field.

### CX-19 — the COLD START block never says the loop OPENS with a run

**Class: skill text. Owner: `/home/forge/src/claude-skills-nrepl/clojure-fast-feedback/COLDSTART.md`,
step 4. PROPOSAL ONLY — this seat does not own that file, and the change was NOT applied.**

p22 and p25 are two independent Opus/W cells on curtaincall-cfp, and their call sequences are the
same shape. p25, verbatim from its transcript:

```
1  cat test/…/format_test.clj
2  cat src/…/format.clj; ls .nrepl-port
3  clj-nrepl-eval … '(System/getProperty "user.dir")'        <- attest
4  python3 - <<'PY' … writes src/…/format.clj                <- THE FIX
5  clj-nrepl-eval … "(do (require … :reload) … (run-tests …))" <- first warm probe, already green
6  bin/kaocha unit                                            <- one cold gate
7  git status --porcelain; git diff --stat
```

Nothing in the delivered instruction set says the loop's FIRST turn is a run. Step 4 says
*"Loop, affected namespaces only, in one eval"*, and an agent that reads an ordered list's "loop"
as *edit -> verify* is reading it correctly. Both Sol cells happened to run before editing; both
Opus cells edited first. **The text does not settle it, so the model settles it** — the same class
as CX-9 (the block left the temp location to the model's guess) and CX-10 (it said what to report
and never said when). N=2 now, on a fresh run of an unchanged block: this is not noise.

**Proposed one-line change to step 4 (not applied):**

> 4. Loop, affected namespaces only, in one eval — **and run it BEFORE your first edit, so you
>    watch the declared failing test go red: a green never preceded by a red proves the test
>    runs, not that it discriminates**: `clj-nrepl-eval -p $(cat .nrepl-port) "(do (require
>    'app.thing :reload) (require 'app.thing-test :reload) (clojure.test/run-tests
>    'app.thing-test))"`

Applying it changes `block_sha256` and therefore every cell's delivered instruction set; the
cells it would be measured against must be run after that, never mixed with these.

### Verdict against acceptance-3 §5, four-cell set {p21, p23, p24, p25}

| part | requirement | p21 | p23 | p24 | p25 |
|---|---|---|---|---|---|
| 1 | `integrity=ok`, verified model, `ceiling=no`, no refusal | ok | ok | ok | ok |
| 2 | `grade=PASS` required 11/11, 0 forbidden, strace, truncated=0, no gap | **PASS** | **PASS** | **PASS** | **FAIL (R10)** |
| 3 | `task=PASS` + declared GATE green + diff in one named src file | ok | ok | ok | ok |
| 4 | `red_attested` names the declared var | assertion | assertion | assertion | assertion |
| 5 | `profile_isolated` reported and carried | no (named) | no (named) | no (named) | no (named) |

**3 of 4 cells pass all five parts. The pilot does not pass: acceptance-3 requires four.**
The one failure is part 2 in the opus/W curtaincall-cfp cell, on `R10.red_first` — and
substituting p22 for p25 gives the same answer, because p22 is the same cell type failing the
same check. Parts 1, 3, 4 and 5 hold in every cell of both sets.

What changed since pilot 4b: **the grader-owned false conviction is gone**, and the remaining gap
is a single, reproduced, agent-visible behaviour with a named skill-text owner. What has not
changed: `profile_isolated=no` is still a declared confound (part 5 is satisfied by NAMING it,
not by curing it), so none of these cells is evidence about a clean-profile agent.
