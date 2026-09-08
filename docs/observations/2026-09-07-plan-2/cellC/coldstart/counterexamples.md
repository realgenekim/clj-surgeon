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
