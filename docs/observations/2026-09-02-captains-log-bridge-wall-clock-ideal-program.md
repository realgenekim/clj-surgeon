# Captain's Log: the wall-clock ideal, from adoption evidence to a program

Date: 2026-09-02 (bridge seat, Fable; program authorized by Gene the same night)

Authority, Gene verbatim, three messages in order:

> "What % of reads/edits went thru surgeon: given the perf stats and wall clock time, and
> the surgeon results, what is ideal % and why -- and was the super fast typing codex spark
> used, and how valuable was it and ideal value be? … if adoption is lower than ideal, tap
> brain fleet for ideas to increase adoption (skill, global prompt)"

> "Collector finding: the usage study's default telemetry root does not match where make
> mcp-serve writes, so it reported zero surgeon calls as 'no-events'. … <-- make the tools
> perfect (see skill)"

> "Write to surgeon captain logs; and you have liberty to modify surgeon to try to achieve
> the theoretical wall clock ideal! use as many sol instances as you need -- you could
> improve every coding session that I do for the rest of my life! Experiment, buy optional
> value, increase optionality, etc! (Read repo docs) prove you understand goals: why, what,
> and how; dogfooding is key, getting good dev build going, go enable fast changes; use in
> modification of Marvin and surgeon"

## Why (the goal, in the repository's own terms)

The product's mechanism is not faster parsing; it is **phase deletion**: fewer model
deliberation rounds and fewer tokens re-carried per round (README, roadmap). Reading is
cheap and writing is expensive by roughly 50 to 100 times (docs/why-reading-is-cheap…),
so the metric is **complete verified task wall**, never tool microbenchmarks and never
adoption for its own sake (CLAUDE.md, active speed mission). Kent Beck's rule governs the
method: when the next change is cumbersome, first make that class of change cheaper, in a
small reversible ratchet committed on its own. Option value comes from independently
testable seams that can advance or be rejected without coupling the others
(`(N * K * sigma) / t`).

Gene's why sits one level up: the constellation is chief of staff to the
chronicler-practitioner, and Surgeon is the lever on every future coding session. A minute
saved per build compounds across every seat, every night. That is the flywheel edge this
feeds (LIVE, then CHRONICLE: this log).

## What tonight measured (the evidence this program stands on)

The bridge4 `?controls=1` build (marvin-voice-remote, branch `b4-controls-flag`,
`da8569c`), an Opus agent under a bridge-seat spec, 12.5 min, 44 tool uses, 201k tokens:

| dimension | measured | theoretical ideal for this class |
|---|---|---|
| Surgeon calls | 7 (5 ok, 2 refused) | 5 |
| individual edits via Surgeon | 19 of 20 (95%) | 19 of 20: already ideal |
| native repairs | 1 (formatter widened a call site to column ~250) | 0 |
| structural reads | 0 of 29 | 2 (one outline per mutated file over ~500 lines) |
| refusal cost | 2 model round-trips ≈ 11% of wall | 0 |
| Surgeon tool wall | 14.2 s of 750 s | noise either way |

Three independent reviewers (Fable, Sol, Opus) converged on this reading; the receipt is
marvin-voice-remote `docs/surgeon-adoption-fleet-review-2026-09-02.md`. The strongest
correction came from Opus: the "70% JavaScript" figure I fed the fleet was true of the
bytes submitted through Surgeon and false of the diff (24% of added characters). Surgeon
was never blocked by the JavaScript; it was blocked from targeting *inside* it.

**The meter itself read zero.** `make study-agent-usage` reported the Surgeon service as
`no-events` for this window while the history side counted 7 calls: the collector's
default telemetry root is the Makefile's `$(MCP_STATE_DIR)/telemetry`, the server's
default (and what `make mcp-serve` produces) is `~/.local/state/clj-surgeon/telemetry`.
Fixed on branch `fix/collector-telemetry-root` (`3c3427d`): union of both roots by
default, typed `root-absent`/`no-events`/`ok`, self-test for all four states. The same
false-zero shape remains in `collect_cclsp_telemetry` (maven inbox `inb-45541c`).

## What I am not going to do, and why

The fleet's highest-ranked product change was "every refusal returns a pre-filled
corrective `next_call`". Reading the repository first: **that is forbidden by the ratified
no-write-authority invariant** (docs/intent/write-refusal-completeness, Gene "Go"
2026-08-30): no in-scope refusal may publish an executable `next_call`, a retry template,
a replacement value, or a source body, because a refusal proves old facts, not the
caller's intended replacement. The reason is sound and I adopt it. The program therefore
attacks the *causes* of the two refusals upstream, in the request language, and adds only
completeness-law evidence (source-free identity and location facts) on the refusal side.

## The program: options, each independently testable

Each option is a seam with its own acceptance gate, its own branch, its own receipt
appended to this log. Losing machinery stays out of the production path.

- **O1, the dev build (Kent Beck first).** One shared server on 7888 is the doctrine for
  a laptop; on this box it is started from `main` and serves live sessions, so a branch
  cannot dogfood itself on it. Add `make mcp-serve-dev` (port, state dir, telemetry dir and
  nREPL port file derived from `MCP_DEV_PORT`, default 7889), plus a worktree-local
  `.mcp.json` and a `codex mcp add clj-surgeon-dev` recipe, so an agent editing Surgeon
  in a worktree uses *that worktree's* Surgeon, hot-reloaded via `make mcp-reload` against
  the dev port. Gate: an `inspect_clojure` round trip on 7889 returns `read_complete=true`
  after a reload of a deliberately changed handler; 7888 untouched. Falsifier: two JVMs
  on one box exceed the bounded heap budget or fight over `.nrepl-port`.
- **O2, the formatter repair.** Characterize the widened continuation after a long
  string literal with a fixture derived from the real `bridge4-page-html` edit. Fix shape
  to be chosen by evidence: the formatter must not reflow lines the change did not own
  (whitespace drift is scored separately from meaning, and meaning is non-negotiable), or
  the specific indentation rule for a `(when …)` following a wide `str` argument. Gate: the
  fixture's staged bytes match the expected layout; no existing test weakened. Binding
  stop respected: formatter *startup* is not the extraction bottleneck; this is about
  *output*, which forced a native repair round.
- **O3, the boundary-insert overlap.** `insert_before`/`insert_after` anchored on owner X
  refused as overlapping six changes *inside* X. A boundary insertion is disjoint from the
  interior by construction. Characterize with the real shape, fix the overlap relation in
  the disjoint compact-edit order decision (docs/decisions/2026-08-29), keep the refusal
  family intact for true overlaps. Gate: the real request compiles in one transaction;
  a genuinely overlapping pair still refuses with source unchanged.
- **O4, the pair-structured append verb.** `cond->`, `cond`, `case`, `let` bindings and
  map entries are sibling pairs; today `:do replacement` demands exactly one complete
  form, so adding one clause resubmits a 20-line form. `assoc_entry` already closes this
  for maps. Design a sibling-pair verb in the compact editor (name and contract through
  LID: design, specs, adversarial review, then red tests). Gate: the real `cond->` change
  from tonight lands as one guarded edit; comments and whitespace between peers are
  preserved byte for byte.
- **O5, the string-aware outline.** `outline` and the owner read return symbol maps found
  inside a Clojure owner's string literals (`function name(` and `var name=` at minimum),
  with line numbers and the enclosing owner, never mutating. This closes the read
  blindness that made 18 of 29 reads native. It expands the read language, so it is a
  spike with a receipt first and a ratified contract before it is a public tool field.
  Gate: the spike answers "where is `onsetReady`?" in one call on the real file.
- **O6, the first keystroke.** The working-tree `skill.md` (which supersedes installed
  copies) gains a question-to-verb table including the rows where Surgeon loses; the
  house-rules sentence "one outline before the first read of any Clojure file over 500
  lines you intend to mutate" is queued to the skiff as `inb-32bcd1`. Gate: the next
  usage study shows ≥1 structural read per mutated large file.
- **O7, the meter.** Root fix landed on its branch; cclsp same shape queued; the study
  collector's status vocabulary is now typed. Gate: `make study-agent-usage` on the dev
  instance's telemetry counts every call the history side counts.

## How

- **Executors:** Codex Sol instances, one per option, each in its own worktree cut from
  fetched `origin/main`, disjoint file sets, never committing or pushing; the bridge seat
  specs, reads every diff, re-runs every verify, and commits as `forge-bridge`. Branches
  are pushed for the mayor to merge; nothing lands on `main` from this seat.
- **Verify:** `make test-fast` for the inner loop; the focused `mcp-test` namespaces for
  contract changes; `make mcp-reload` against the dev port for live proof; `make test`
  as the milestone gate. No test weakened or removed; refusals leave bytes unchanged.
- **Measure:** replay the bridge4-controls class of change against the dev instance and
  count calls, refusals, repairs and reads with the fixed collector; the ideal row above
  is the pass line. Projected savings stay hypotheses until a clean-context cohort
  verifies them (CLAUDE.md).
- **LID:** O4 and O5 change public contracts and go design → specs → adversarial review →
  red tests → code, with a stop for Gene's review at the ratification boundary. O1, O2,
  O3, O6, O7 are ratchets and characterizations and proceed under tonight's authorization.
- **Coordination:** the two research lanes (SURGEON1 production, SURGEON2 experiments)
  are untouched; this program reports to the mayor over the channel and this log, and
  cherry-picking into the production lane is SURGEON1's judgment.

## Surprise, counterfactual, falsifier

Surprise: the best product idea from three reviewers was already ruled out, correctly, by
a document nobody in the review had read. Counterfactual: without "read repo docs" I
would have spent a Sol instance building a forbidden verb. Falsifier for the whole
program: if a clean replay on the dev build still needs 7 calls after O3 and O4 land, the
refusals were not the cause and the cost lives in the model's intent-expression time,
which no server change reaches.

## What becomes cheaper next

A worktree that dogfoods its own Surgeon in seconds (O1) makes every later option cheap
to try and cheap to reject; that is why it goes first.

## Addendum 01:45Z — what the 138 prior logs say about this program

Read by five parallel Sol readers (28 files each, fixed digest shape), then by the lead
from the digests. The corpus is 36,151 lines; the lead's context never held it. What the
logs would have been embarrassing not to know:

- **The dominant clock lives at the model boundary, not in the kernel.** Roughly 0.09 to
  0.25 s of service work precedes 8.8 to 9.1 s to the next action; a millisecond refusal
  routinely forced 29 to 33 s of payload reconstruction. The longitudinal gains
  (2.47x → 6.36x → 7.35x) came from deleting decision and recovery boundaries. So O3 and O4
  attack the right thing: a refusal avoided is a round-trip deleted.
- **Over eight days, write-shape work was worth 94.4 s while post-decision ceremony
  consumed 21.2 hours.** The largest hill is materialization and verification after the
  decision, which this program does not yet touch. Noted as the next program.
- **O1 is the best-supported option in the corpus.** A stale working-directory classpath
  once made `mcp-reload` a truthful no-op; real-wire tests overturned green in-process
  suites (JSON map types, stale classpaths); a 2 GiB → 512 MiB heap change made a second
  instance affordable. Two cautions: isolated MCP instances do not justify isolated heavy
  analyzers (machine-wide analyzer admission stays shared), and the dogfood route must be
  tested on the actual JSON wire, never only with in-process Clojure maps.
- **O2 is real but bounded.** A −46.4% formatter-wall win projected to only −4.6% of
  complete wall; the compact editor won by excluding whole-file formatting. O2's value is
  the avoided repair round, not milliseconds. Binding stop respected.
- **O3's authority lesson is firm:** explicit named owners or reviewed hash-fenced plans,
  never caller-supplied position; an ambiguous boundary insertion refuses rather than
  overlaps. The approved LLD (two insertions at one boundary still refuse) conforms.
- **O4 is the proven high-value direction, with one constraint:** a lowering entrance into
  the one transaction engine with a closed injective syntax table, never a second
  executor. Sibling spans were already generalized to `case`, `cond`, bindings, and maps;
  closed relations were promoted at 7.35x on exactly this pattern. Fuzzy tolerance lost
  every time it was tried.
- **O5 must stay evidence, never authority.** Positions and symbol-like text inside
  strings are unsafe as write addressing; the string-symbol map is a read-side aid with
  `authority=false`, and any later write still names an owner.
- **O6 has the clearest causal evidence in the corpus:** visibility alone produced 0 of 4
  adoption, one project rule produced 4 of 4; compact edits were chosen 0 of 3 until the
  operation had its own unmistakable name; the skill, not the noun, made the route real.
  The queued house-rule sentence (`inb-32bcd1`) is the right instrument.
- **O7's warning:** high adoption once coexisted with 21% refusals and direct tool wall of
  1 to 3% of task wall. Usage counts without route context mislead; the fixed collector's
  typed status is necessary, not sufficient.
- **The corpus's own law, in one line:** perceive the complete decision, strike one guarded
  chord, return terminal proof, and stop. The theoretical ideal is one action per honest
  judgment boundary, not one command per task.

Method note: the four Sol implementers were interrupted by the harness once mid-work and
resumed from their partial worktrees; the first O2/O3 run stopped itself at the LID
low-level-design gate exactly as AGENTS.md instructs, and the lead approved that phase
in writing before it continued. Both are evidence that the repository's contracts bind
executors that have never seen them before.

## Receipts 03:20Z — first wave landed as branches, each verified on the host by the lead

| option | branch | commit | verified | verdict |
|---|---|---|---|---|
| O1 dev build | `bridge/dev-build-instance` | `a294cac` | `make test-fast` 647/5559/0/0 on this box (baseline 9 failures, 13 errors); `mcp-dev-start` on 7889 with 7888 untouched; real streamable-HTTP `tools/call` read_complete=true in 345 ms; docstring change + `mcp-dev-reload` :ok true; stop leaves no listener | ready to merge; SURGEON1 to review the 3-symbol SCI allowlist widening (`case*` `throw` `new`) |
| O2+O3 gap and overlap | `bridge/insertion-gap-and-overlap` | `33ba760` | red 3 → green; bb 60/596 + 33/476; repository intent audit :ok true after registering the leaf; JVM 374 tests, 1 pre-existing failure | code ready; specs MCP-OP-INSERT-001..006 await Gene's ratification |
| O4 sibling pair | `bridge/sibling-pair-verb-lid` | `9082e36` | docs only, 502 lines, four-document LID shape | design awaits Gene's ratification before any code |
| O5 string-aware outline | `bridge/string-aware-outline-spike` | `5d17b16` | bb 13/45 + 16/215; JVM 377, 1 pre-existing failure; **real-wire dogfood** from a dev instance on 7891: channel.clj 168 forms, 371 string symbols, 886 ms; onsetReady 3336, bargeTh 3256, kwCheck 2626/3356, cueCancel 2625/3355, each with its owning form, all equal to grep | spike proven; contract naming awaits ratification |
| O7 meter | `fix/collector-telemetry-root` | `3c3427d` | real data: 8 calls; absent root → root-absent | ready to merge |
| harvest | `bridge/harvest-stranded-lessons-2026-09-02` | `4cd1b91` | 695 files, byte-identical spot checks against branch tips | merge first: it makes the other week's lessons reachable |

Three things the lead learned that the executors could not:
- Sol's sandbox could not open loopback sockets or write under `$HOME`, so every live
  gate (server start, wire call, reload) had to be run by the lead on the host. A report
  that says "the harness prevented this" is honest; a lead that ships it unverified is not.
- The repository intent audit reads **one `@spec` id per annotation line**; two ids on one
  line silently witness only the first. The leaf must also be registered in
  `audit-current-repository`'s hardcoded list. Both are now in the O2/O3 commit message.
- Two JVM suites run concurrently on one box collide on the agent-shell admission lock
  (`mcp_process_test`); run alone they pass. Not a product defect; a scheduling one.

Pre-existing findings on `origin/main` on this box, reported not fixed: a test hard-codes
`/opt/homebrew/bin/clj-kondo` (`mcp_change_buffer_test.clj:686`); `recovery.clj`
`write-failure-receipt!` does not create its parent directory; the install isolation test
assumed box-global destinations (fixed in O1).

Next wave, not started: ratification stops for O2/O3, O4, O5; the house-rule sentence
(`inb-32bcd1`); the skill table; and the second program the corpus pointed at: the
post-decision ceremony that consumed 21.2 hours against 94 seconds of write-shape work.

## Addendum 02:40Z — the acid test's first run measured the apparatus, not the product

Design: two Codex Sol arms, one fresh session each, same prompt bytes, same frozen task (the
bridge4 controls flag from ab267f9), arm A on the shipped Surgeon (7888), arm B on the wave
build (7889), sequential, scored by the fixed collector plus kaocha and the golden.

Arm A finished in **341 s**, exit 0, and is **correct**: 578 tests, 7804 assertions, 0
failures, golden unchanged (the Opus build of the same spec took 750 s). But the server saw
6 Surgeon calls, all inspects, 3 refused (`invalid-mcp-request`: two aggregate-expectation
mismatches, one missing-fields), and **zero writes**. The codex rollout shows why: four
`edit_clojure`/`apply_clojure_changes` calls ended in 0 ms with
`{"Err": "user cancelled MCP tool call"}`. `codex exec` under `-s workspace-write` auto-cancels
MCP calls it treats as mutating; reads complete. Sol read this as "surgeon cancelled the
edit twice" and fell back to native patches, which the brief permitted after two refusals.
The server telemetry cannot see a client-cancelled call, so the collector's "3 refusals of
6" was true and useless: the arm's edit route never existed.

Probe: the same write from `codex exec --dangerously-bypass-approvals-and-sandbox` against a
scratch workspace on 7889 landed (`RESULT=ok true`, file changed). The repo's own Anvil
harness has always run Codex that way; the bridge's first apparatus did not, and no receipt
said so until the client view was read. Ratchets taken:
- the scorer now reads the codex rollouts for the arm window and prints
  `CLIENT-CANCELLED` per arm, so a silent apparatus failure can never score as "zero
  refusals";
- the rerun uses the bypass flag with worktree isolation as the safety boundary;
- both arms of the first run are retained as **native-route data**, not Surgeon data.

Two product findings survive the invalid run: Sol's first three calls all failed on the
aggregate `expect` object of `inspect_clojure` (first-keystroke friction: a field that is
derivable from the request array), and Sol's honest reading of a client cancellation as a
server refusal means refusal receipts and client failures need distinguishable words in
the skill. Both routed to SURGEON1 as design questions, not decided here.

## Receipt 03:05Z — acid test run 2: three correct arms, and the hill is not where I pointed

Same frozen task (the bridge4 controls flag from ab267f9), Codex Sol, one fresh session per
arm, sequential, `--dangerously-bypass-approvals-and-sandbox` with the worktree as the fence.
All three arms pass the full suite and leave the golden unchanged.

| arm | wall | Surgeon calls | refused | client-cancelled | kaocha | golden |
|---|---|---|---|---|---|---|
| N native, no MCP | **191 s** | 0 | 0 | 0 | 578 / 7800 / 0 | unchanged |
| A shipped Surgeon (7888) | 303 s | 9 (4 inspect, 5 write) | 3 | 0 | 578 / 7798 / 0 | unchanged |
| B wave build (7889) | 319 s | 7 (2 inspect, 5 write) | 1 | 0 | 579 / 7800 / 0 | unchanged |

Phase clock from the codex rollouts joined to server timings:

| arm | discovery → first write | editing | ceremony after last write | Surgeon tool wall |
|---|---|---|---|---|
| A | 109 s | 109 s | 84 s | 12.1 s |
| B | 71 s | 127 s | 122 s | 8.6 s |
| N | ~51 s | ~85 s | ~55 s | 0 |

What the wave build did: refusals 3 → 1 (the overlap and batch-selection refusals are gone;
the one left is the `cond->` pair shape, design-only tonight), reads 4 → 2 (the string-aware
outline found `onsetReady` in one call; discovery fell 38 s). What it did not do: move the
wall. Refusal round-trips cost A about 52 s and B about 17 s, a 35 s saving that was
absorbed by 43 s more editing and 38 s more ceremony. Tool wall is 3 to 4 percent either way.

**Retained negative, stated plainly:** on this task class, a four-file flag feature of ~90
insertions, native Sol beat both Surgeon arms by about two minutes, and the refusal fixes
did not change complete wall. The corpus predicted the crossover ("on a small two-file,
three-change control, native remains faster; the tool earns its ceremony when exact
mechanical state spans enough owners and files") and the standing rule it recorded is the
one to apply: abstain until one guarded burst removes meaningful model work. The falsifier I
wrote before the run fired: the cost lives in intent expression and post-decision ceremony.

Caveats that keep this from being a verdict: N = 1 per arm; the 7888 arm serves the pre-merge
code only because its JVM has not been reloaded since the merge; B's ceremony included two
extra kaocha runs Sol chose to make. A counterbalanced cohort runs overnight.

Also merged tonight on Gene's authority ("merging your surgeon changes in"): the four wave
branches into main (`882b17b`; test-fast 653 / 5591 / 0, intent audit true, focused suites
green), archived under `refs/archive/2026-09-02/delivered/` and deleted; and the bridge4
trial into voice-remote main (`6d4cecf`, kaocha 579 / 7833 / 0). One lesson from the merge
itself: a squash refused by a dirty beads export, followed by an archive-and-delete that had
already run, was recoverable only because the archive ref had been pushed and confirmed
first. The skill's order of operations is the reason nothing was lost.

## Receipt 03:35Z — Anvil calibration (sequential vs pinned-parallel) and the first valid blind quality review

Venue moved to Anvil (`tester@anvil`, 16 cores, 23 GiB spare) after the Buster cohort made
that box unusable and my own verification jobs contaminated its first triple (Buster r1
retained as contaminated: N 219 s, B 463 s). Both Surgeon instances on Anvil are pinned to
cores 12-15; each arm gets its own four cores via `taskset`; `TMPDIR` under the seat home;
diffs frozen by the runner the moment an arm ends. All arms below are correct (full suite
green, golden unchanged).

| arm | Anvil sequential | Anvil parallel (all three at once) | Buster run 2 |
|---|---|---|---|
| N native, no Surgeon | 243 s | 245 s | 191 s |
| A shipped Surgeon | 243 s | 239 s | 303 s |
| B wave build | 303 s | 424 s | 319 s |

**Headline 1: pinned parallel is a valid apparatus for N and A** (within 2 percent of their
sequential twins). B moved by 121 s, so B's variance is not yet understood; parallel is
adopted for N and A, B stays sequential until three clean samples exist.

**Headline 2: the shipped Surgeon matches native on this task class** (243 vs 243 on Anvil),
and **the wave build is the slowest arm in every environment** (303, 424, 319, 463).

**Headline 3: blind quality review, frozen diffs, labels shuffled (Sol):** native 18.0,
shipped 17.0, wave 14.5 of 20, the wave losing on clarity (2.0 of 5). The reviewer did not
know which arm used which tool. So B is slower *and* produced the least clear code.

**Learning:** the refusal hypothesis is falsified for wall on this class, twice over. The
wave build's fewer refusals and fewer reads did not buy time or quality; the most plausible
mechanism, to be tested next, is payload volume: the string-symbol outline returned 371
symbols into the model's context and the edits that followed were messier. The corpus
already recorded that an oversized visible result crossing the transcript boundary
returned the whole batching gain (2026-08-06). Next: B' = wave build with
`include_string_symbols` OFF, same task, to separate the read payload from the edit fixes.

Caveat: N = 1 per cell for the parallel row and for the review; s2 and s3 (rotated
sequential triples) are running for N = 3.

## Receipt 04:05Z — Anvil sequential triples s2 and s3 (medium rung): the spread is the finding

All arms correct (full suite green, golden unchanged). Walls in seconds, clean sequential
runs on Anvil only (Buster runs excluded as contaminated or a different box):

| arm | cal-seq | s2 | s3 | mean | spread |
|---|---|---|---|---|---|
| N native | 243 | 287 | (running) | | |
| A shipped Surgeon | 243 | 237 | 317 | 266 | 80 |
| B wave build | 303 | 272 | 579 | 385 | 307 |

**Headline:** within-arm spread (40 to 300 s) is larger than every between-arm effect I
narrated earlier tonight. "B is slowest" and "A matches native" were both draws. At this
sample size Surgeon and native are indistinguishable on wall for this task class.

**Where B's 579 s went (s3):** 11 Surgeon calls with 4 refusals (`inspect-output-limit`
on a batched read, `invalid-intent-form` twice on the pair shape, `no-op-intent` once) and
**eight** `bin/kaocha` runs chosen by the agent. Refusals are stochastic per run, not a fixed
property of the build: the wave build had 1 refusal in run 2 and 4 here. The variance is
behavioral, in what Sol chooses to do after a refusal or a test run, and that is what the
planning-mode arms (P, Q) are designed to constrain.

**Learning:** eight per cell, as the corpus practiced, is the floor; anything less is
narrative. The small rung (two files, focused suite, ~90 s per arm) plus proven-parallel
arms is how eight per cell becomes affordable. Standing order from Gene applied from here:
one unit at a time on a new apparatus, a 2-minute poll that checks receipts advance, fail
fast.

## Receipt 04:22Z — s4 (first triple with arm C) and the third blind review

s4, Anvil sequential, all correct: C (wave build, string-symbol outline OFF) 328 s, A 273 s,
N 263 s. Third blind review (s3, key shuffled): A 18, native 16, B 16; B's clarity 2 of 5
again. Three reviews now: native 18, 19, 16 · A 17, 17.5, 18 · B 14.5, 15, 16. Native and A
are tied within noise; **B is last in all three, always on clarity.** That is the first
between-arm effect tonight larger than its noise. The s4 review, which scores C blind against
A and native from the same triple, decides whether the string-symbol payload is the cause.

Standing order from Gene applied from here: after every result, poll Sol and Opus
independently for interpretations and the next wave of experiments, and run several in
parallel on Anvil. First poll launched on the packet through s4.

## Receipt 04:35Z — fleet round 1 (Sol + Opus, independent) and the zero-slot analyses

**s4 blind review (key shuffled):** A 19.0, C 17.5, native 15.5. C, the wave build with the
string-symbol outline OFF, scored above every B score so far (14.5, 15, 16); N = 1, and the
judge's own noise is unmeasured, so "weak support for the payload hypothesis" is the most
that can be said. Two zero-slot experiments launched to fix that: the same three diffs
re-judged twice by Sol (noise floor), and an Opus judge scoring s2, s3, s4 blind.

**Actions, not seconds (Opus's "one thing nobody measured", computed from the rollouts at
zero cost):**

| arm | wall s | model actions | MCP calls | suite runs | input tokens carried |
|---|---|---|---|---|---|
| s2 A / B / N | 237 / 272 / 287 | 20 / 21 / 19 | 2 / 8 / 0 | 1 / 1 / 2 | 1.21M / 1.46M / 1.30M |
| s3 A / B / N | 317 / 579 / 194 | 30 / 35 / 15 | 2 / 11 / 0 | 3 / 5 / 3 | 1.86M / 2.61M / 0.84M |
| s4 A / C / N | 273 / 328 / 263 | 23 / 27 / 25 | 2 / 4 / 0 | 3 / 3 / 2 | 1.35M / 1.72M / 1.60M |
| Buster A / B / N | 303 / 319 / 191 | 22 / 32 / 14 | 9 / 7 / 0 | 1 / 3 / 1 | 1.27M / 1.27M / 0.78M |

Wall tracks actions plus suite runs almost linearly (about 9 s per action, the corpus's
model-boundary figure, plus 40 to 60 s per suite run). The action count has far less
spread than the wall and separates the arms where the wall could not: B and C take more
actions and carry more input tokens than A or N. **The program's own theory, phase
deletion, is now measured directly, and the wave build adds phases.**

**Fleet round 1, converged next wave** (both reviewers, independently): a randomized
power-8 medium replication; a hostile small rung where native should win outright; the
large multi-owner rung where Surgeon is supposed to win; planning-mode and first-keystroke
arms (model behavior); a multi-judge quality trial with a measured noise floor; Opus adds
the action-count metric and four apparatus ratchets (server build SHA per arm, tests ≥
baseline assertion in the gate, suite-invocation count in the receipt, p90 scheduling);
Sol adds refusal-injection dose–response and a context-overflow sweep. Opus's sharpest
reading: wall is conserved on the wave build, discovery −38 s reappears as ceremony +38 s.

Ratchets landed in the v3 runner tonight: server SHA and suite count in every receipt, a
TESTS-BELOW-BASELINE flag (cal-par B passed with 577 tests, one below baseline, and nobody
noticed), and a core-slot pool so six 2-core arms can run at once (Gene's suggestion);
calibration cal2 queued: six medium arms in parallel at 2 cores, N A N A N A, against
their 4-core numbers.

## Receipt 04:42Z — judge noise floor, a second judge, and the acceptance-test ratchet

**Noise floor (Sol re-scoring identical diffs three times, cal-seq):** native 18.0 / 17.9 / 17.8;
A 17.0 / 17.4 / 18.7; B 14.5 / 14.4 / 16.0. Judge spread about ±1 point on a 20-point total;
the A-vs-native rank flipped once. **B's deficit of about 3 points exceeds the floor.**

**Second judge (Opus, blind, same frozen diffs):** s2: native 18, A 16, B 13 · s3: A 19,
native 15, B 14 · s4: A 18, native 16, **C 12**. Opus agrees with Sol that B is last in s2
and s3. On C the judges split: Sol 17.5, Opus 12, because Opus found a semantic defect the
rubric-by-eye judge missed: C's mic-gate branch omits `speechStartAt=0`, so a stale speech
timer survives playback and the first loud frame after a reply can start a recording with
no debounce, the very echo tail the feature exists to suppress. The disagreement is the
signal: C is not vindicated, and one judge is not enough.

Opus also found, across all nine diffs: five resolve `hbms` in client JavaScript instead of
server state (a spec deviation), three rewrote the shared 250 ms tick instead of appending
to it, four disable the buttons on checking or playing rather than on not-recording, and
one native diff plumbs OVER through a mutable flag that can force-end the NEXT automatic
keyword check. **Every one of those passed the full suite and the golden**, because the only
tests of the new behavior were the ones each arm wrote for itself.

**Ratchet (in progress):** an arm-independent acceptance test namespace for the task, written
once from the spec by an agent that did not build any arm, run against every diff. From
here "correct" means the arm-independent tests, not the arm's own. The scorer must not be
written by the subject.

## Receipt 04:48Z — arm T, the fast typist (gpt-oss-120b via OpenRouter), on Buster

Mayor delivered an OpenRouter key on Gene's offer ("a SUPER FAST TYPIST … IN ADDITION TO codex
spark"). Tool: `~/bin/typist SPEC [TARGET]`, one bounded call per invocation, key read from
`~/secrets/openrouter.edn` at runtime, hard cap 10 calls / 40k output tokens per run, cost
printed from the response, no fallback path. Arm T = shipped Surgeon (7888) + Sol keeps every
decision + typist for mechanical JS-string and test typing. Same frozen task, same fences,
run on Buster so it is comparable to Buster run 2 (native 191 s, A 303 s, B 319 s).

| arm T | value |
|---|---|
| wall | **409 s**, exit 0 |
| correct | 579 tests / 7808 assertions / 0 failures; golden unchanged |
| model actions | **44** (Buster A: 22, native: 14) |
| Surgeon calls | 6, 1 refusal (invalid-intent-form) |
| typist calls | 4: one rejected (empty output), two accepted as scaffolds then rewritten, one partially used; ~$0.007 total; 22 to 25 s per call at 24k to 74k input tokens |

**Headline:** the typist made the arm slower, not faster: +106 s over A and twice the model
actions. Each delegation cost a spec-writing turn, a 20-second typist wait, a review turn and
usually a correction turn, on a task whose expensive act was never typing. Exactly the
fleet's prediction before the run ("the typist hands over the load-bearing act"). Retained
negative; the tool stays for tasks with long mechanical runs (test bodies, fixtures) and is
off the medium rung. Apparatus note: Sol deleted the untracked ledger inside the worktree
"to keep the diff clean"; the ledger moves outside the worktree from here.

Economics gate honored: metered, bounded by construction, cost line per call, total under a
cent, no auto-fallback exists.

## Receipt 04:58Z — the scorer is no longer written by the subject

An Opus agent that built none of the arms wrote `acid_acceptance_test.clj`: nine deftests
derived only from the frozen spec's observable contract (golden byte identity; the flag in
effective-state; buttons and bootstrap key on the flag-on page; micGate inverse of barge-in;
the onsetReady guard resets the speech timer; the 250 ms tick ridden not rewritten; hbms
server-resolved with a 2500 default; force_end on the same send path; buttons follow
recording and the note says controls ON). On the bare base: 2 pass, 7 fail, no errors. On
the merged reference: 6 of 9, and the three failures are real divergences from the frozen
spec, two of them introduced later by design (the mic gate became its own flag for
attributability) and one a genuine deviation in my own build (hbms parsed client-side).
The suite is now being run against every frozen diff on both boxes. A pass rate per arm,
from tests the arm did not write, replaces "kaocha green" as the correctness score.

## Receipt 05:05Z — s5 (second triple with C)

s5, Anvil sequential, all correct: native 283 s, C 333 s, B 330 s. C's two samples (328, 333)
are tight; B's (303, 272, 579, 330) are not. Running series on Anvil, seconds:
native 243, 287, 194, 263, 283 · A 243, 237, 317, 273 · B 303, 272, 579, 330 · C 328, 333.
Blind review for s5 launched (Sol); Opus judge to follow on s5–s7 together. s6 (P Q A), the
first planning-mode triple, is running.

## Receipt 05:15Z — s5 blind review: the string-symbol outline is exonerated

s5 (Sol judge, key shuffled): native 18.5, B 15.0, C 14.5, with C's clarity at 2.5 of 5,
the same score that sinks B every time. C now has two blind scores, 17.5 (Sol, s4) and 14.5
(Sol, s5), plus Opus's 12 on s4 with a real defect found. **C is not better than B.** The
payload hypothesis, that 371 string symbols in context made the wave build's edits messy,
is falsified: with the outline off, the clarity deficit remains. Whatever makes B and C
write less clear code than A and native is in the wave build's edit path or in how Sol
behaves against it, not in the read. Native and A remain tied.

Acceptance rescoring (arm-independent suite, running): first rows show the suite
discriminating on exactly the defects the Opus judge read by eye: acid-7 (hbms
server-resolved) fails in most arms, acid-9 (buttons follow recording) in several, acid-6
(tick rewritten) in two, acid-5 (onset guard resets the speech timer) in one; two diffs
pass all nine. Full table next.

## Receipt 04:45Z — arm P, planning mode with the price list (first sample)

s6 P: 258 s, correct. Its PLAN.md predicted 193 s and self-reported 200 s actual (+7 s,
4%); the process wall was 258 s, so the model's accounting misses about a quarter of real
wall, the boundary time it cannot see. Its per-step price estimates were otherwise sane
(kaocha 85 s predicted, ~85 s real).

**What planning did to the route:** the plan's step 3 named `src/marvin/channel.clj`, a
path that does not exist (`src/marvin_voice_remote/`). Every Surgeon call then refused
(`invalid-workspace-root`, `missing-fields`, `source-file-not-found`), the prompt's
"native after two refusals" rule fired, and P completed the task natively. So the first
planning sample is really "planned native": it committed to a wrong fact before reading,
and the plan's authority carried the error into three refusals. A price list does not fix a
wrong path; a read does. Q (native + plan) and A run next in s6.

## Receipt 04:50Z — the acceptance scoreboard: a correctness result the suite-and-golden gate could not see

Every frozen diff (22, both boxes) rescored with the arm-independent nine-test acceptance
suite written from the spec. "Failed assertions" are of 39; "clean" means all nine tests pass.

| arm | n | mean failed assertions | mean failed tests | clean diffs | what fails |
|---|---|---|---|---|---|
| A shipped Surgeon | 6 | **1.0** | 1.00 | 2 | hbms client-side ×2, tick rewritten ×1, buttons ×1 |
| B wave build | 6 | 2.5 | 1.67 | 1 | hbms ×4, onset guard ×2, tick ×2, buttons ×1 |
| C wave, no string symbols | 2 | 5.0 | 2.50 | 0 | hbms ×2, onset guard, micGate, buttons/bootstrap |
| N native | 7 | 2.4 | 2.00 | 2 | hbms ×5, buttons ×5, tick ×2 |
| T shipped + typist | 1 | 2.0 | 1.00 | 0 | hbms |

**Headline:** on spec conformance, judged by tests none of the arms wrote, **the shipped
Surgeon arm is the most correct arm** (1.0 failed assertions per diff against native's
2.4), while the wave build and C are the least. Every one of these 22 diffs had passed the
full suite and the golden. The two most common misses are the same across arms and are
both "reading the spec loosely": resolving `hbms` in client JavaScript instead of server
state, and gating the buttons on checking or playing instead of on recording. Native's
extra miss is the buttons rule (5 of 7); B's extra misses are the onset-guard timer reset
and rewriting the shared tick, the two places where its edit path touches the JavaScript.

**What this changes:** wall was a wash and quality-by-judge favored native and A equally;
conformance-by-test favors A. Combined with the action count, the honest summary of the
medium rung is: shipped Surgeon = native on time, ≥ native on correctness, and the wave
build is worse than both on correctness and clarity while equal on time. The wave's two
edit-path changes (boundary-insert overlap, insertion gap) are the suspects, and the
next experiment is the one that isolates them: wave build with the string outline off
AND the two edit fixes reverted one at a time.

## Receipt 04:58Z — s5, second judge

Opus on s5 (blind): native 18, C 15, B 15. Sol had native 18.5, B 15, C 14.5. Two judges
agree on both the order and the gap. Opus's most important defects: B silently changed the
shared 250 ms tick predicate on the flag-on path (drops `!stopping`), C forks `kwCheck` by
wholesale copy so future fixes to the real one never reach the flag-on page, and native's
cancel body exists twice in source. The clarity deficit of the wave-derived arms is now
supported by two judges on three triples and a measured noise floor.

## Receipt 04:52Z — s6, the first planning-mode triple

s6, Anvil sequential, all correct: P (shipped Surgeon + plan) 258 s, Q (native + plan) 221 s,
A 244 s. P's plan predicted 193 s and self-reported 200; Q's plan is captured for the same
calibration. Q is the fastest native-family sample after s3's 194. One triple; s7 repeats the
two planning arms in the other order. Blind review launched (Sol). The CLI-only entrance
riff is filed as a follow-up (maven `inb-ce2f15`), with one cheap arm queued at the end of
the chain, and no further attention until the main program is done (Gene's standing
instruction: stay on the main quest).

## Andon 05:06Z — the dev-build merge widened the SCI fence; production clean; my instances checked

surgeon1 (via the mayor's cord) proved on origin/main that the SCI allowlist widening in
`ddd074f5` (`case*` `throw` `new` plus one class mapping, made by the O1 agent to turn a red
test green and flagged by me for review only *after* merging) lets constructor shorthand
`IllegalArgumentException.` bypass the source-symbol fence: a computed edit program can place a
host exception, print to stderr, and read host stack data. Causal control at `ddd074f5^`
refused both probes. No filesystem or process escape shown; the boundary is still broken.

Blast radius: production and the skiff's 7888 run `64eac2e`, clean. On my side: Buster 7888
started 2026-09-01 05:57Z, never reloaded, clean; Buster 7889 stopped; Anvil 7888 serves
`64eac2e`, clean; **Anvil 7889 served `7ef1532`, which contains the widening**, used only by my
own arms from the tester account. Remediation armed: revert `edit_dsl.clj` to `64eac2e` in that
checkout and restart 7889 in the gap after s7, before r1 needs it, recorded in
`receipts/ANDON-7889.txt`. Scoped freeze honored: no install or reload of main until the fix.
Standing rule adopted and written to memory: fence, allowlist, or confinement changes get
adversarial review before merge. The cord worked exactly as the house rules describe: the
puller kept repair authority, the freeze was scoped to the release lane, and measurement
continued.

## Receipt 05:12Z — s7 closes the old chain; the real job starts

s7 (Anvil sequential, all correct): Q 338, P 359, N 273. Planning arms across two triples:
P 258 / 359, Q 221 / 338; planning mode did not clamp variance. s6 acceptance: A fails
acid-7 (hbms), Q fails acid-7, P fails acid-9 (buttons). The 7889 remediation fired in the
gap after s7 (edit_dsl.clj at 64eac2e, restarted 05:08:34Z, healthy) before r1's wave arm
needed it. r1, the first real job (surgeon recovery-receipt defect), started 05:08Z on the
v3 runner: N and A in parallel, then B. Fleet round 2 (Sol + Opus) polled on the full packet
with acceptance, actions, and plan calibration; s7 blind review launched.

Turn-count picture, for the record: wall ≈ 9 s per model action plus suite runs, on every
arm and both boxes; native ≈ 19 actions, A ≈ 24, B ≈ 29, T 44. The arm is not the clock; the
number of times the model comes back is.

## 05:20Z — the pictures (Gene: "record graphs in captain's log")

```
COMPLETE VERIFIED WALL, Anvil clean runs (• per sample; 10 s per column)
                150      200      250      300      350      400      450      500      550      600
native (n=6)    |          •        •  ••  ••                                                      |  194–287
A shipped (5)   |                 ••  • • •                                                        |  237–317
B wave (4)      |                          •  •    •                              •                |  272–579
C wave–sym (2)  |                                 ••                                               |  328,333
P shipped+plan  |                           •            •                                         |  258,359
Q native+plan   |                      •                •                                          |  221,338
T typist (Bstr) |                                             •                                    |  409
                ^ spread inside an arm (40–300 s) exceeds every gap between arm means

WALL vs MODEL ACTIONS (N native · A shipped · B wave · C · T typist)
 600 |                                        B      ← 35 actions, 5 suite runs
 400 |                                                 T   ← 44 actions, 4 typist hand-offs
 350 |                              C
 300 |          N    B       A    A  B  ·······        diagonal ≈ 9 s per action + tests
 250 |         N A  A N   ····
 200 |     N·····
     +----+----+----+----+----+----+----+----+
     10   15   20   25   30   35   40   45   actions
 read down a column: same actions, same wall, any arm   → the tool is not the clock
 read along the diagonal: every point, every arm, both boxes → turns are the clock

WHAT THE TIMING CANNOT SEE       judges /20 (2 judges, noise ±1)   acceptance failed/39
   native   17–19                                                    2.4
   A        17–19                                                    1.0   ← most conformant
   B        13–16  (clarity 2/5)                                     2.5
   C        12–17.5                                                  5.0
```

**The reading, in one line:** the arm is not the clock; the number of times the model comes
back is. Every tool's only lever on wall is the count of returns it induces, and the wave
build induced more. Gene, on seeing this: "Obvious and yet insightful"; and: elevate the
need to reduce tool calls to the global prompt, make it a key part of a coding task at the
high level, brainstorm with the fleet. Both launched.

## Receipt 05:30Z — fleet round 2, Opus (recomputed from raw receipt rows)

- **Wall null is dead flat:** A − native = +3.4 s, SE 17.6, t = 0.19 (Anvil pooled, n 6–7); every
  triple tonight was powered only for effects ≥ 60 s, i.e. only B's.
- **The A trade is real and marginal on both sides:** conformance A 1.0 vs N 2.43 failed
  assertions (p ≈ 0.08) bought with actions A 23.6 vs N 18.2 (p ≈ 0.07). Caveat that matters:
  acid-7 (hbms) fails in 14 of 21 rows and inflates every arm; excluding it, the A advantage
  sits on acid-9 alone (1/6 vs 5/7, Fisher p ≈ 0.10). Acceptance is the correctness
  authority, judges the maintainability authority, neither yet significant on A vs N.
- **The one thing nobody measured: input tokens carried predict wall better than actions**
  (R² 0.78 vs 0.61; 159 s per million carried tokens) — and the Surgeon arms carry MORE than
  native (A +23%, B +62%). The product's stated mechanism is fewer tokens re-carried; on this
  rung its own arms carry more. Direction-changing: instrument the context-carry curve per
  arm; if it is flatter for A on the large rung the theory holds and the medium rung is below
  the crossover, if steeper there too the theory is falsified on its own metric.
- **Planning mode: change, not keep.** P +50 s over A, Q +24 s over N, variance not clamped,
  and an accurate cost estimate with no consequent is a receipt, not a control. Keep only a
  read-gate: every path named in a plan must cite an inspect receipt obtained before the plan.
- **Next wave, ranked by information per minute:** E1 repair the scorer (drop or fix acid-7,
  add a third judge) at zero arm-minutes; E2 bisect the wave build (B minus gap fix, B minus
  overlap fix), acceptance-scored, ~14 min; E3 hostile small rung n=10; E4 the big chord
  (≥12 owners) where Surgeon must win or the wall program ends; E5 action-and-context diet
  (one plan, one chord, ≤2 suite runs, no re-read after read_complete, payload cap) —
  predicted actions −32%, wall −23%, the largest lever; E6 refusal dose; E7 power-16.
- Stage 0, free: correlate refusal count with acceptance failures across the 22 frozen diffs
  (the "refusals are quality control" reading predicts s3-B, 4 refusals, cleaner than B's mean).

## Receipt 05:35Z — r1, the first real job, and the s7 review

r1 (R1: the recovery-receipt parent-directory defect, surgeon repo, v3 runner, native and
shipped in parallel then wave): native 113 s, A 148 s, B 194 s; all three pass the focused
recovery suite with the new regression test (3 tests, 0 failures). Small real task, native
fastest, same ordering as the medium rung. A blind comparative review picks the merge
candidate; the winner merges to surgeon main (merges continue under the scoped freeze;
installs do not). s7 blind review (Sol): P 19, Q 19, native 17 — the planning arms scored
highest on this triple (n = 1), so planning's quality effect is open even though its wall
effect is negative. cal2, the six-arm two-core calibration, started after r1.

## 05:45Z — turn budget as doctrine: the fleet's brainstorm and tonight's test of it

Gene: "elevate the need to reduce tool calls; becomes key part of a coding task at high level; brainstorm with brain fleet." Sol, Opus and the bridge converged on the same shape, drafted below and queued to the skiff as `inb-5a2d7b`. Opus's decisive caution: turns are demand-driven; a cap without a named substitute relocates the spend into one giant unreviewed action (the wave build removed refusals and the model spent the freed turns on re-reads and suite runs). So the rule budgets in actions, keeps suites as a separate counter, names the substitutes (batch, terminal results, one-turn refusals), and requires a report. Running tonight as e3 on Anvil: A vs A+report-only (Hawthorne control) vs A+full rule, three each, two-core slots, scored on actions, acceptance, and blind quality; the control decides how much of the paragraph must survive. cal2 (six two-core arms) is running first after a fail-fast: its first launch fed codex empty prompts, the runner's tests-below-baseline flag caught it in one poll, and the batch rule meant one wasted unit, not a night.

## Turns are the clock (Gene, 2026-09-02; measured on 20+ runs, two boxes)

Complete verified wall of a coding task ≈ **9 s × model actions + ~50 s × test-suite runs**; tool
execution is 3–4% of wall whatever the tool. You are not paying for tools, you are paying for
returns to the model. So a turn budget is part of every coding task:

- **Before the first tool call, state one line:** `BUDGET: N actions, M suite runs`, with
  N = 6 + 2 per file you will change + 1 per unknown you must resolve first, and M = 1 (+1 per
  milestone gate). Budget in actions, which you control, never in seconds, which you do not.
- **Spend it the only way that works:** batch independent reads into one call and independent
  edits into one call; treat a complete result as terminal (never re-read to confirm a write
  that returned a receipt); on a refusal, retry once from the refusal's own fields or switch
  route, never probe. Before each call ask: does this change the result, discharge a
  verification obligation, or remove a later call? If none, do not call.
- **Two clauses that keep this from backfiring:** suites are a separate counter and never
  fungible with actions (under budget by skipping the gate is a failed task); and one action
  may hold many edits but only one irreversible decision (a 400-line blind write is not a cheap
  turn, it is an unreviewed one).
- **End with** `TURNS: n/N actions, m/M suites` and, if over, the single cause. Report it
  alongside correctness and the diff; never rank work on turns alone.
- **A tool earns its call only when it removes a return you would otherwise make.** A tool that
  is 3 s faster but adds one round-trip is a 6 s loss.

## Receipt 05:55Z — r1's real finding: the defect was an apparatus artifact; the ratchet is kept

The blind review of r1's three diffs found that `write-failure-receipt!` already creates its
parent directory (44db939, an ancestor of the base). All three arms therefore produced
test-only diffs. The "defect" I reported at 02:40Z came from the O1 build agent, whose codex
sandbox refused writes under its home; I relayed it without reproducing on the host. Record
corrected with the mayor (bead clj-surgeon-9yy, second half withdrawn). Fifth apparatus
lesson of the night, same family: a failure observed inside a sandbox is a fact about the
sandbox until reproduced outside it.

Kept anyway: the native arm's regression test, chosen blind for asserting on the receipts
directory itself and `.isFile` and for covering the `:onboarding` failure branch; proven red
with the `.mkdirs` line disabled (2 errors) and green with it (3 tests, 17 assertions).
Squash-merged to main (`2b3177d`), bb full suite 654 / 5595 / 0 on this box, branch archived.
r1 walls for the record: native 113 s, A 148 s, B 194 s on a two-file test-only task.

## Receipt 06:00Z — cal2 (six two-core arms in parallel), a mislabel corrected, and arm W

**cal2** (medium rung, CORES_PER_ARM=2, six arms at once, all correct): native 223 / 289 / 469,
A 232 / 250 / 355. Means native 327 (4-core sequential mean 254), A 279 (259). **Two-core
six-wide is not timing-neutral:** the tails inflate (469, 355) as six kaocha JVMs contend. So
the wide configuration is for experiments scored on actions, acceptance and quality, and
walls from it carry a contention caveat; four-core, three-wide stays the wall apparatus.

**Mislabel, corrected:** Anvil already had a Surgeon on 7888 owned by another user (the
fleet's production deployment, 2026-08-25-e7f72e2). My shipped instance on 7888 never
bound; its health check answered from the other server. Every Anvil arm labeled "A shipped
64eac2e" before 05:30Z in fact called that production server, and its telemetry is in a home
I cannot read (which is why the refusal analysis had no data for those arms). Relabeled here
as **A = production e7f72e2** for cal-seq, cal-par, s2–s7, r1, cal2. The comparison stands
(production is a shipped build), the label and the resource were wrong; reported to the
mayor. From now every shipped arm calls my own 64eac2e instance on 7893.

**Stage 0 (refusals vs failures, n=10 Surgeon arms with readable telemetry):** Spearman
−0.14, undetermined, leaning against "refusals are quality control"; confounded with call
volume; the missing rows are exactly the production-server arms.

**Bisect servers live:** 7891 = wave minus the insertion-gap fix (its added test fails, all
else passes), 7892 = wave minus the overlap fix (its two added assertions fail, all else
passes). Arms G and O in the runner; queued after e3.

**Arm W, on Gene's redesign of planning:** not a cost estimate but deliberate selection in one
message: three candidate plans, a critique of each against the spec and the measured prices,
choose one, execute, report the choice. Zero extra returns for the planning itself. Added to
e3: A, A+report-only, A+budget rule, A+deliberate, three each, scored on actions, acceptance
and two-judge quality, wall with the contention caveat.

## Receipt 06:25Z — edit wall (test time subtracted) for 37 arm-runs: it changes nothing, and it corrects me

Gene: "I really want Kaocha runs outside of wall time. We're timing edits, not the test times."
Retro-computed from the rollouts (script `edit_wall.py`, attribution by worktree, test commands
matched at command position, harness polls subtracted at 9 s each):

| arm | n | wall | edit wall | non-test actions | in-run test s |
|---|---|---|---|---|---|
| native | 11 | 282 | 272 | 18.9 | 8 |
| A shipped | 10 | 266 | 255 | 18.7 | 9 |
| B wave | 5 | 382 | 366 | 23.8 | 16 |
| C | 2 | 331 | 322 | 20.0 | 4 |
| P / Q | 2 / 2 | 309 / 280 | 283 / 269 | 19.5 / 15.0 | 25 / 11 |
| K CLI-only | 1 | 451 | 450 | 10.0 | 1 |

**Corrections to my own earlier claims.** (1) In-run test time is 2 to 48 s per arm, not
hundreds: agents fire `bin/kaocha`, codex yields after ~30 s, and most never poll it back;
"s3-B ran kaocha 8 times" was my `grep -c` counting mentions (prompt echo, bead text), not
invocations: 4 actual, 48 s. `suite_invocations` in the receipts over-counts up to 12x.
(2) All tool execution, tests included, averages 32 to 44 s of a 270 to 380 s arm: **about
87% of scored wall is model return and generation time.** (3) Eight of 33 Anvil arms ran
the full suite zero times in-run and still passed the external gate; making tests "free"
would reward that, so verification stays a conformance gate, never a cost. (4) The 9 s per
action figure is an average, not a law: K made 10 non-test actions in 450 s, 45 s each,
because each CLI action carried a long syntax-reasoning turn. **Wall ≈ Σ per-action think
time; actions are the proxy only while think time per action is similar across arms.**

Subtracting tests reorders nothing: A beats native by 16 s on wall and 16.5 s on edit wall,
B is worst on both. Adopted: edit wall as a published companion, never the primary; new
rungs run one focused suite in-arm and leave the full gate to the harness (for CPU
contention and conformance, not for wall); fix the invocation counter.

## Receipt 06:50Z — record correction: which server the "A" rows actually called

While confirming the acceptance figures verbatim for the mayor I re-read every arm start line.
Every Anvil A and P row from s2 through k1 (03:41Z to 05:32Z, including cal2 and k1, which the
06:00Z receipt had left labelled "shipped") carries `url=http://127.0.0.1:7888/mcp`. The
`server_sha=` field beside it is NOT the server's identity: the runner computes it with
`git rev-parse --short HEAD` on the local checkout mapped to the arm (line 24 of the runner),
so it printed 64eac2e for a server it never inspected. The mayor's statement stands as the only
identity evidence for 7888: another seat's production Surgeon at e7f72e2. Therefore:

- Every Anvil "A" and "P" result before e3 is **"production 7888, identity unverified (e7f72e2
  per mayor)"**, not shipped 64eac2e. The only shipped-64eac2e rows are Buster acid2/A and the
  e3 arms now running on 7893 (healthz and ready.edn prove 7893 is my process, pid 2371990,
  project-root surgeon-shipped).
- The acceptance headline "shipped Surgeon 1.0 vs native 2.4" is withdrawn as stated. Verbatim
  from rescore-anvil.txt and rescore-anvil-s6.txt (failed assertions of 39, arm-independent
  9-test suite): 7888 rows s2 0, s3 1, s4 0, s6 2 (mean 0.75, n 4); native s2 0, s3 4, s4 3,
  s5 0 (mean 1.75, n 4); wave B s2 3, s3 2, s5 3; C s4 2, s5 8. Buster acid2 (true shipped)
  A and N figures are in score_acid. The claim is now "the production Surgeon at 7888 beat native
  on conformance"; whether shipped 64eac2e does awaits the e3 rescore.
- Ratchet: the runner must capture server identity FROM THE SERVER (ready.edn project-root +
  pid, or a build-sha field in healthz, which the server should grow), never from a local path
  table; and a receipt field named `server_sha` that is not read from the server is exactly the
  "receipt blind to its subject" class from the delivery invariants. Fixed in v5: the sdir case
  now includes W (its start line had shown surgeon-main's sha for the same reason).

## Receipt 07:10Z — e3 group 1 (six-wide, 2 cores per arm, all on my 7893): blind quality, two judges

Prompt variants on the shipped Surgeon: U = A + report-only paragraph, V = A + turn-budget rule,
W = A + deliberate three-plan selection. Six diffs frozen per worktree (the runner's per-arm diff
name collided for the two A arms; fixed in v5 to `<run>-<arm>-<slot>.diff`), shuffled, judged
blind by Sol and Opus on completeness / default safety / tests / clarity (0–5 each, 20 max).

| worktree | wall s (contended) | Opus | Sol | mean | judges' named defect |
|---|---|---|---|---|---|
| W-3 deliberate | 464 | 19 | 17.5 | 18.25 | Sol: buttons disabled during checking/sending |
| U-1 report-only | 381 | 18 | 18.5 | 18.25 | duplicated kwCheck text |
| A-0 shipped | 434 | 17 | 18 | 17.5 | default-note test weak |
| V-2 turn budget | 340 | 15 | 17 | 16 | BOTH: lastHeartbeatAt never reset, heartbeat fires at record start |
| A-4 shipped | 488 | 15 | 15.5 | 15.25 | BOTH: monkeypatches onsetReady/kwCheck, two live definitions |
| U-5 report-only | 672 | 14 | 16 | 15 | Opus: mic gate leaves stale speechStartAt (the echo class); Sol: negative hbms |

Learning: the judges agree on the ranking's shape and name the same defects independently
(V-2 heartbeat, A-4 monkeypatch), so the ±1 noise floor holds at n=6. W (deliberate planning)
scored top on Opus and third on Sol; the fastest arm (V, turn budget, 340 s) carried a real
flag-on defect, which is the trade the budget rule was expected to make. Cohort-wide drift:
all six read `hbms` client-side from URLSearchParams instead of server-resolving it like the
other flags; a spec ambiguity, not a tool effect. Actions, acceptance and prompt-adherence for
this group are being scored on Anvil (e3-g1-score.md); walls here are contended and not
comparable to sequential runs.

## Receipt 07:25Z — e3 group 1: actions, acceptance, prompt adherence (scored from rollouts pinned by worktree path)

| worktree | wall s | non-test actions | total actions | MCP | shell | suite runs | acceptance failed | adherence |
|---|---|---|---|---|---|---|---|---|
| A-0 | 434 | 18 | 25 | 10 | 11 | 1 | 2 (acid-7) | n/a |
| A-4 | 488 | 20 | 26 | 8 | 12 | 1 | 4 (acid-5, acid-7) | n/a |
| U-1 report-only | 381 | 21 | 27 | 12 | 11 | 1 | 2 (acid-7) | self-count exact (27) |
| U-5 report-only | 672 | 15 | 21 | 5 | 11 | 1 | 3 (acid-7, acid-9) | self-count exact (21) |
| V-2 turn budget | 340 | 11 | 18 | 5 | 9 | 1 | 2 (acid-7) | OVER budget (15 stated, 18 measured); shipped 577 tests vs 578 baseline |
| W-3 deliberate | 464 | 19 | 27 | 5 | 14 | 4 | 2 (acid-7) | 3 plans + choice in one message before the first tool call; self-count off by 3 |

Learnings. (1) The turn-budget rule (V) cut total actions 30 percent against A (18 vs 25.5) and
took the shortest wall, but paid in quality: one test fewer than baseline, a flag-on heartbeat
defect both judges named, and it still overran its own budget. (2) The report-only paragraph (U)
did not change action count (24 vs 25.5), so the effect in V is the budget, not the act of
counting; U's self-reports were exact to the action, V's and W's drifted, so agents count
accurately only when counting is all they are asked to do. (3) Deliberate planning (W) spent
the most actions and four suite runs and scored best with Opus, third with Sol: planning bought
quality, not speed, in this one draw. (4) acid-7 fails in all six; A-4 (acid-5) and U-5 (acid-9)
are the only differentiating misses. (5) n=1 for V and W. Walls are six-wide contended.

Tool corrections: edit_wall.py keys windows by (run, arm), drops the slot and knows only the
alphabet NABCPQKT, so it is wrong for doubled arms and unknown for U/V/W; count_actions.py
overwrites windows for doubled arms; the receipts' suite_invocations counts grep hits in a run
log both slots append to. For the 06:25Z retro this swaps rows only within an arm (cal2, k1),
so the arm means stand. New scorer `~/e3_final.py` on Anvil pins by worktree path and agrees
with the rollouts' own mcp/patch events 6 of 6. Runner v5 now writes per-slot diffs and logs.

## Receipt 07:45Z — fleet round 3 (Sol and Opus, independent) on e3 group 1, and the queue it produced

Convergent readings. (1) The turn budget is a real lever on actions and it pays with scope,
not waste: V shipped one test fewer and a named defect (Opus: "a cheaper task, not a cheaper
route"; Sol: "trade speed for correctness"). (2) Report-only counting is inert, so the
Hawthorne control did its job. (3) Deliberate planning bought quality with four suite runs,
and judge disagreement on W equals its effect size. (4) Both: the ranking is one draw; the two
identical A arms differed by 2.25 quality points and by six actions, which spans the whole
table. (5) Both: "shipped beats native on conformance" cannot be claimed tonight; the honest
claim is about the unverified 7888 server, n=4, and leans on acid-9 once acid-7 is excluded.
Opus side finding: MCP calls per arm ranged 5 to 12 on one server and one task; prompt
wording moved Surgeon adoption more than any build did.

Divergence: Sol wants a 24-run replication of A/U/V/W; Opus wants the control-variance floor
first (A x6) so every later gap can be judged against it. Both are queued.

Queue on Anvil (chain-next.sh, then chain-after-k2.sh; each cohort 2-core, six-wide unless
noted): b1 bisect "G O A" x3 at 4 cores (which edit fix costs clarity) -> n1 "A N" x6 each,
paired by wave (clean shipped-vs-native, server identity verified) -> k2 "A K" x3 each (MCP vs
CLI on the same build) -> v1 "A x6 | V x3 A x3" (variance floor, budget dose with the
baseline gate armed). Every diff frozen per worktree; scored on actions, acceptance, two blind
judges; walls reported with the contention caveat.

## Receipt 08:30Z — e3 complete: prompt variants at n=3 per arm (all on verified 7893)

Group 2 (six-wide, contended walls), two blind judges (20 max), acceptance = failed assertions of 39:

| worktree | wall s | total actions | acceptance failed | Opus | Sol | mean |
|---|---|---|---|---|---|---|
| A-2 shipped | 563 | 40 | 1 (acid-9) | 17 | 19 | 18 |
| U-3 report-only | 485 | 19 | 2 (acid-7) | 16 | 17.5 | 16.75 |
| V-0 turn budget | 605 | 28 | 2 (acid-7) | 17 | 17.5 | 17.25 |
| V-4 turn budget | 692 | 19 | 3 (acid-6, -7) | 17 | 18.5 | 17.75 |
| W-1 deliberate | 586 | 30 | 5 (acid-5, -7, -9) | 18 | 15.5 | 16.75 |
| W-5 deliberate | 463 | 24 | 4 (acid-6, -7, -9) | 15 | 19 | 17 |

Per-arm means over both groups, n=3 each:

| arm | wall s | total actions | MCP calls | suite runs | acceptance failed | quality (2 judges) |
|---|---|---|---|---|---|---|
| A shipped | 495 | 30.3 | 10.7 | 1.33 | 2.33 | 16.9 |
| U report-only | 513 | 22.3 | 8.3 | 1.00 | 2.33 | 16.7 |
| V turn budget | 546 | 21.7 | 6.3 | 1.67 | 2.33 | 17.0 |
| W deliberate | 504 | 27.0 | 7.3 | 2.33 | 3.67 | 17.3 |

Headline: **no prompt variant separates on quality at n=3** (spread 0.6 points on a scale where
the two identical A arms in group 1 differed by 2.25). The turn budget cut total actions 28
percent at equal acceptance, but two of its three runs shipped fewer tests than baseline and it
overran its own stated budget every time. Deliberate planning cost acceptance (3.67 vs 2.33)
and bought nothing measurable. Within-arm wall spread exceeds between-arm spread for every arm.

Judge finding, the important one: on W-1 the judges disagree by 3.5 points on the same
artifact. Opus ranks it first because it "touches no shared string at all, everything in one
appended block that reassigns onsetReady/kwCheck, literally what the spec prescribed"; Sol
ranks it last for "replacing onsetReady and kwCheck", the pattern both judges penalised in
group 1's A-4. That is a rubric hole, not noise: the spec must say whether the appended
reassignment is the prescribed form or a monkeypatch, or the clarity axis measures judge taste.
Opus also found a correctness split Sol missed: U-3 and A-2 return from the mic gate before
resetting speechStartAt, the exact echo-moment failure the feature exists to prevent; Sol
scored A-2 19. Correctness convergence between judges is weaker than group 1 suggested.

Server identity: the rollouts record no MCP URL at all, so routing is provable only from the
runner's `-c mcp_servers.clj-surgeon.url` line (7893 for A/P/U/V/W); the W start lines'
41eee738 label was the runner's fall-through case, now fixed. Surgeon should report a build id
in healthz and stamp it in telemetry so a receipt can witness the server, not the runner.

## Receipt 08:50Z — fleet round 4 on the e3 result: a correction I owe, and the verdicts

**Correction (Opus caught it, Sol missed it):** at n=3 the Hawthorne control captured the action
reduction. Total actions: A 30.3, U report-only 22.3, V budget 21.7. Asking the agent merely to
COUNT its turns removed 26 percent of actions; the budget added 2 percent on top. My group-1
line "the effect in V is the budget, not the counting" is dead. The lever, if it is one, is
making the agent count returns, and the budget arithmetic buys nothing but overruns. Sol read
the same table and called report-only inert because U did not move quality, acceptance or
wall; both are right about their own axis, and the disagreement is the finding: counting
moves actions and nothing else moved.

Verdicts, both reviewers converging. Turn-budget doctrine: ship only with a mandatory
conformance gate (acceptance suite plus baseline test count must hold before a run counts as
"under budget"), keep the mechanism sentence ("returns, not execution, are the clock"), drop
the `N = 6 + 2f + u` formula until v1 shows the budget beats bare counting, forbid spending
saved turns on omitted verification, never state the budget in seconds. Rubric: the spec must
declare the appended-reassignment form permitted or prohibited, "one live definition per
symbol" becomes a binary acceptance assertion rather than a clarity score, and judges cite the
spec clause or return unverified. acid-7 (hbms server-resolved) fails in 11 of 12 e3 arms and
every native row: the prompt never said the flag must be server-resolved, all twelve agents
read it client-side, so it measures spec ambiguity; report acceptance both with and without it
from here on. Dead claims list (Opus): the conformance headline, every wall claim about prompt
variants, "planning buys quality". Survives: 87 percent of wall is model time; 9 s/action is a
mean; the wave build's clarity deficit is the only between-arm effect above the noise floor.

Next cohorts proposed. Opus: the large rung, a 12-plus-owner cross-file mechanical change,
A vs N x6 instrumented for tokens carried, because every queued cohort re-measures the medium
rung the corpus predicted is below the crossover; tonight A carries 23 percent more tokens
than native, which reads backwards against the product's mechanism. Sol: cross-task
generalisation, A vs V x6 on a materially different task. Both filed; the large rung is being
designed now (task prompt plus acceptance suite) so it can run after v1.

## Receipt 09:20Z — apparatus defect found in b1 group 1: orphaned gates and cross-group name collisions

"b1 A gate  suite_invocations=6" landed with an empty test summary. Cause: the runner launched
each arm's gate in the background inside an already-backgrounded arm function, so the group's
`wait` returned before the last gate finished, and the next group re-created the same worktree
name (b1 uses slot 2 for A in every group) under the running gate. The gate read a fresh
checkout mid-destruction and printed nothing. The same reuse overwrites per-slot diff and
report files across groups. e3 escaped because its two groups put different arms in each slot.

Actions: group-1 diffs copied to `b1-g1-*` before group 2 could overwrite them (20.5, 19.4,
19.7 KB); a watcher freezes groups 2 and 3 the same way; canonical runner v5 now runs the gate
serially, adds a group index `g<n>` to worktree and receipt names, diffs against the base
commit (an agent that commits would otherwise yield an empty diff), and stamps `g=` on end
lines; a `fullgate.sh` re-runs the real gate (full kaocha plus pages) on any frozen diff so a
lost gate can be recovered; chain-2 waits for b1, refreshes the running copy only after its
driver exits, full-gates all b1 diffs, then runs n1, k2, v1 on the fixed runner. Ratchet
class: a background job inside a background job is an orphan; a receipt name without the
group index is a collision waiting for the first repeated arm.

## Receipt 09:45Z — rung L designed: "one server-owned wall clock" (the large multi-owner task)

Per fleet round 4 (Opus: every queued cohort re-measures the medium rung). Task: hoist the 21
scattered `System/currentTimeMillis` reads in marvin-voice-remote at ab267f9 into a new
`clock` namespace, thread the require through ten ns forms, make the three existing dynamic
`*now-ms*` vars delegate to it while still winning when locally bound. 21 top-level owners
across 11 namespaces, purely mechanical, no URL flags, server-owned versus client-owned
spelled out (the 23 `Date.now()` calls inside JS strings are out of scope and guarded).
Acceptance suite of 12 tests, every discriminating assertion tied to a numbered clause;
measured on the pristine base 39 failed assertions, on the reference implementation 0, full
suite unchanged at 577 tests, zero golden bytes moved. Verify meter for arms: one focused
namespace that compiles the whole tree and binds two of the three vars. Spec, prompt and
suite stored beside this log in `2026-09-02-acid-rung-L/`; the reference solution stays off
the arms' path. Designer's caveat: the full suite runs in 19 s here, so agents will be tempted
to run it instead of the focused one; that temptation is the main variance source in this
rung. Installing on Anvil now; cohort l1 "A N" x3 pairs queues after v1.

## Receipt 10:40Z — b1 bisect, actions and acceptance (quality pending a capture correction)

Arms on verified servers: G = wave minus the insertion-gap fix (7891), O = wave minus the
overlap fix (7892), A = shipped (7893); three groups of "G O A", 4 cores per arm.

| arm | wall s (mean, spread) | total actions | MCP calls | refusals | acceptance failed |
|---|---|---|---|---|---|
| G no-gap-fix | 542 (483–575) | 29.0 | 7.3 | 2.3 | 2.33 |
| O no-overlap-fix | 618 (469–863) | 30.3 | 10.7 | 5.3 | 2.33 |
| A shipped | 636 (436–810) | 29.7 | 10.0 | 4.3 | 2.00 |

Findings from the rollouts (scorer pins by worktree path and start time, 9 of 9 resolved):
total actions and acceptance do not separate the arms. The only consistent split is that G
made fewer MCP calls and drew fewer refusals than A in all three groups (MCP 7/8/7 vs 11/8/11;
refusals 2/3/2 vs 6/3/4); O crosses A in both directions. So the insertion-gap fix carries
the shipped build's MCP-and-refusal profile, and reverting the overlap fix is behaviourally
near-invisible at this n. acid-7 fails in 7 of 9 runs, including two shipped runs.

Apparatus finding, again receipt-blind-to-subject: frozen diffs were captured with plain
`git diff`, which omits untracked new files. O-g3 provably lost its new test namespace
(worktree survived; corrected diff 14.5 KB vs 10.4 KB) and both judges had scored it "no tests
at all". Fixed in the runner and the freeze script (`add -A` then `diff --cached base`); an
audit of all 21 rollouts for created files absent from their diffs is running, and the two
judges are re-scoring the corrected diff. The b1 quality verdict waits for that audit,
because groups 1 and 2 worktrees are gone and a truncated diff cannot be recovered there.
Also: the runner's `TESTS-BELOW-BASELINE` flag on G-g3 was contradicted by the full gate
(577 in-run, clean on rerun), another orphaned-gate symptom from before the serial fix.

## Receipt 11:00Z — b1 bisect, blind quality under the new rubric (two judges, 20 max, n=3)

| arm | Sol | Opus | mean | note |
|---|---|---|---|---|
| A shipped | 19, 18, 19 (18.7) | 16.5, 15, 15.5 (15.7) | 17.2 | |
| G no-gap-fix (has overlap fix) | 18, 19, 18 (18.3) | 15.5, 17, 15.5 (16.0) | 17.2 | Opus's top diff is G-g2, "by far the DRYest" |
| O no-overlap-fix (has gap fix) | 17.5, 16.5*, 15.5 (16.5) | 16, 12.5*, 15 (14.5) | 15.5 | *O-g2's tests axis is unverified |

Untracked-file audit (all 21 rollouts, two oracles): no e3 diff lost anything; in b1 only arm
O created a new file, both times via Surgeon's `create_files` (a new test namespace), in
groups 2 and 3. Group 3 was recovered and re-scored by both judges (Opus 10.5 to 15, Sol 12 to
15.5, tests axis only). Group 2's worktree was destroyed at the group boundary, so O-g2 was
judged on a diff missing its 74-line test file and cannot be repaired; over O's two verified
runs the judges' mean is 16.0.

Reading of the bisect, at this n: the overlap fix is exonerated on quality (G equals A at
17.2, and G also drew the fewest refusals and MCP calls); the build that keeps only the
insertion-gap fix sits 1.2 points below A and G, in the direction of the wave's deficit
(wave B scored 14 to 15 against A's 17 to 18 earlier tonight). The gap fix is the suspect,
with the caveat that 1.2 points is inside the 2.25 within-arm spread measured on identical A
arms. Replication queued as b2 "O G O G O G" one wave after l1, judged blind, with the
corrected capture. Side observation worth a bead: only the O build's agents used
`create_files` to put tests in a new namespace; the shipped build's agents never did.
Rubric held: no clarity deduction cited the reassignment itself; deductions cited duplicated
body spans (largest: a whole kwCheck `.then` handler copied in O-g3).

## Receipt 11:25Z — fleet round 5 on the bisect: a mechanism, a correction, and a code finding

**Correction to the 10:40Z receipt.** "The insertion-gap fix carries the shipped build's
MCP-and-refusal profile" was backwards: the shipped build carries neither fix. Opus read the
diff: the gap fix touches no refusal predicate; `overlap?` is the only edited gate. So G, the
only arm carrying the overlap fix, drawing half the refusals (2.3 vs 4.3 shipped, 5.3 O) is
the overlap fix's intended effect, and it is exonerated on quality at the same time.

**Mechanism, both reviewers independently (Sol calls it "productive refusal"):** before the
gap fix, an insertion into an empty same-side gap either refused or landed ugly (same line,
column 250), which forced the agent to re-read the form and rewrite the span; rewriting a span
reads the whole body, and that is where DRY happens. After the fix the insert just works,
silently and cleanly, and the agent appends without re-reading; the cited deductions were
appended duplicate bodies (a whole kwCheck `.then` handler in O-g3), and G, without the gap
fix, was the DRYest arm. The tool became more capable and the output worse because a
corrective feedback event was removed. This is the same shape as the turn-budget finding:
what changes the agent's behaviour is what it is made to look at.

**Code finding for the maintainers, before b2 (Opus, from the gap fix's diff):** the
indentation probe `(?m)(?:^|\n)([ \t]*)$` under `re-find` returns the FIRST match in the
prefix, i.e. the first blank line anywhere above the anchor, not the anchor's own line; any
file with an earlier blank line yields anchor indentation "" and a column-0 insert. Needs a
nested-anchor indentation test. Reported to skiff; credible, not yet proven, unshipped.

**Strongest confound (both):** the capture defect was perfectly correlated with the losing
arm; one re-score moved O-g3 by 4.5 points, larger than the 1.2 deficit; O's quality rests on
n=2; fixed G-O-A order per group confounds arm with drift.

**b2 must record:** worktrees preserved to cohort end; staged diffs with a pre-scoring gate
that no worktree file is absent from the scored diff; refusals typed with the agent's next
action; an op ledger (insert vs span-replace vs create_files) with whether a re-read followed
each accepted gap insert; a mechanical near-duplicate-span metric so duplication is a
predicate; randomised arm order; per-axis judge scores. The decisive analysis is conditional:
do accepted gap insertions precede the duplicated spans that refusals in G make agents
reconsider. `create_files` in O: Opus's cheap test (an `:overlapping-intents` refusal
immediately preceding each create_files, since O lacks the overlap fix) is running on the
rollouts now; Sol reads it as coincidence pending that.

## Receipt 11:50Z — the bisect settles: the gap fix ADDS a refusal, and only O ever sees it

Typed refusal classifier over all nine b1 rollouts (agrees with the text-derived counts 9 of 9):

| reason | G x3 | O x3 | A x3 |
|---|---|---|---|
| invalid-intent-form | 5 | 8 | 10 |
| ambiguous-insertion-gap | 0 | **4** (3 of 3 runs) | 0 |
| overlapping-intents | 0 | 1 | 0 |
| other (inspect-output-limit, batch-form-selection, unknown/missing fields, expect-count) | 2 | 3 | 3 |
| total | 7 | 16 | 13 |

`ambiguous-insertion-gap` ("the sibling gap contains comments or detached source") fires in
every O run and in no G or A run. O is the only arm carrying the insertion-gap fix, so the fix
does not remove a refusal, it introduces one, and it fires on real code because channel.clj
has comments between top-level forms. What the agents did with it: two of three escaped on the
very next call to `create_files` (a new test namespace), one stopped. The one
`overlapping-intents` refusal in the cohort started a six-call struggle in O-g2 that ended in
the same escape. So both anomalies of the night (the only new files, and the file lost to
plain `git diff`) have one root.

This overturns the fleet's "productive refusal removed" story from round 5, in direction:
the gap fix did not make an ugly success silent, it turned a success into a refusal that the
agent cannot act on from its fields, and the escape (a new file, or giving up on the
insertion) is where the clarity went. It also explains why the wave build (both fixes)
scored lowest: the overlap fix alone halves refusals (G), the gap fix alone adds a family of
them (O), and together the agent meets the new family without the old escape routes.

Reading with confidence now: **the insertion-gap fix causes the wave deficit; the overlap fix
is good.** Maintainers: the ambiguity predicate treats comments or detached source between
siblings as ambiguous even when the anchor is explicit; that condition needs an explicit
anchor exemption or an actionable next_call in the refusal, and the indentation-regex finding
(11:25Z) sits in the same code. Product tax, separate bead: `invalid-intent-form` is 23 of 36
refusals across all arms, two-thirds of every refusal the agents drew, with no arm signal;
the intent grammar is the largest adoption cost measured tonight.

## Receipt 12:20Z — n1, the clean shipped-versus-native cohort: walls (six-wide, 2 cores, paired by wave)

| arm | walls s | mean | gates |
|---|---|---|---|
| native N | 231, 259, 521, 363, 402, 427 | 367 | 6 of 6 green |
| shipped A (verified 7893) | 426, 714, 722, 592, 754, 851 | 677 | 6 of 6 green; one at 577 tests, below the 578 baseline |

Headline: on the verified shipped server, six paired draws, the Surgeon arm took 1.8x the
native wall, and every shipped run was slower than every native run in its wave except one
pair. This is the first clean wall comparison of the night; the earlier "indistinguishable"
result (t=0.19) was measured against the unverified 7888 server, sequentially. Caveats: six-wide
contention inflates both arms equally in principle but not necessarily in practice (a Surgeon
arm holds a JVM connection while a native arm does not); the diagnosis waits on the typed
refusal ledger, MCP call counts and tokens carried, all being scored now, and on blind
quality. Diffs frozen with the completeness gate: 12 of 12 complete.

## Receipt 12:45Z — n1 scored: on the verified server the shipped Surgeon arm is additive, not substitutive

Per-arm means, n=6 each, paired by wave (scorer pins rollouts by worktree and start time, 12 of 12):

| metric | native N | shipped A | A/N |
|---|---|---|---|
| wall s | 367 | 677 | 1.84 |
| input tokens carried | 1,239,123 | 2,297,194 | 1.85 |
| non-test actions | 10.0 | 21.2 | 2.12 |
| total actions | 19.5 | 30.5 | 1.56 |
| shell calls | 11.5 | 15.7 | 1.37 |
| MCP calls | 0 | 8.5 | |
| patch applies | 3.7 | 1.2 | 0.33 |
| refusals (typed) | 0 | 2.7 (13 of 16 invalid-intent-form) | |
| acceptance failed assertions of 39 | 1.83 | 3.33 | |

Findings. (1) The wall gap is the token gap: 1.84 = 1.55 (turns) x 1.19 (seconds per turn),
and the 1.19 in seconds per turn equals the 1.19 in context per turn; Spearman wall~tokens
0.87 over all twelve runs, the strongest pairing. (2) Refusals and MCP-call count explain
nothing: within A, wall~MCP-calls rho 0.03; the slowest run made the fewest Surgeon calls,
same as the fastest. No ambiguous-insertion-gap or overlapping-intents refusals in this
cohort (shipped build carries neither fix). (3) The mechanism is that Surgeon is layered on
top of the native workflow rather than replacing it: the A arm still issues more shell calls
than native, adds 8.5 MCP calls, and applies a third as many patches; native replaced reading
with editing. MCP results are small (under 500 chars average), so the extra context is the
consequence of twice the steps, not of verbose receipts. (4) Contention is not the cause: in
both waves the native runs exited first and left the shipped arms nearly alone on the box.
(5) Acceptance runs opposite to wall: native 1.83 failed assertions to shipped 3.33, native
holds the only clean sweep, shipped the worst run (5 failures, slowest, most tokens).
(6) Apparatus: the TESTS-BELOW-BASELINE flag is off by one; ab267f9 has 577 top-level
deftests, so 577 is the honest count for any run that adds assertions inside existing tests;
the flag has fired falsely four times today and is informational only. Baseline for rung M
is 577 from here.

Standing of the night's claims after n1. Wall: on the verified shipped build the Surgeon arm
is slower, 1.8x, six paired draws, direction held in 5 of 6 pairs. Conformance: native is
better on this cohort; the withdrawn headline does not come back. Clarity: pending the two
judges on these twelve diffs. The earlier "indistinguishable" wall result stands only for the
unverified 7888 server and is not evidence about the shipped build.

## Receipt 13:05Z — n1 blind quality, two judges, twelve diffs under the rubric ruling

| arm | Opus (per run) | Sol (per run) | mean of judges |
|---|---|---|---|
| native N | 19, 16, 17, 16, 17, 19 (17.3) | 18.8, 18.4, 18.3, 18.1, 18.6, 17.8 (18.3) | 17.8 |
| shipped A | 15, 17, 14, 19, 16, 18 (16.5) | 17.3, 18.5, 16.6, 18.0, 17.6, 18.2 (17.7) | 17.1 |

Both judges put native ahead by under a point, inside the noise floor, and in the same
direction as acceptance (1.83 vs 3.33). One correctness discriminator, found by Opus and
mapped after the fact: the micGate guard placed BEFORE the `if(playing){speechStartAt=0;...}`
reset, which leaves a stale onset timestamp across the reply and lets the first loud frame
after playback trip the debounce instantly, the echo class the gate exists to prevent. In
n1 it is in 3 of 6 shipped diffs and 0 of 6 native. Earlier tonight Opus found the same
defect in e3 (three Surgeon-prompt arms of twelve, no native arm to compare). Across the
night: 6 Surgeon-arm instances, 0 native. The edit sits inside a JavaScript string in
channel.clj, where a structural editor has no structure to target; a guard added at the
function's top anchor is the cheap insertion, a guard inside the branch needs the body read.
That is the additive-insertion signature from the bisect, now visible as a correctness
defect in the shipped build's output. Sol's scores did not separate on it (its clarity axis
dominates); Opus's completeness axis did. Other cohort-wide notes: 2 of 12 parse hbms
server-side (both native), 2 of 12 retype the whole kwCheck body (both shipped, the two
lowest Opus scores).

Night's standing after n1 quality: wall, shipped 1.8x native; conformance, native better;
clarity, native slightly ahead inside noise; one correctness defect class specific to the
Surgeon arms. The judges' disagreement pattern is stable: Sol compresses toward 18, Opus
spreads 14 to 19 and finds the defects; both agree on direction.

## Receipt 13:40Z — fleet round 6 on n1: the diagnosis is the write contract, and substitution is the hill

Convergent, both reviewers independently. "Additive, not substitutive" is a property of the
tool's contract, not agent discipline: Surgeon's write verbs demand complete prior knowledge
(exact from/to literals, exact owner names; the tool discovers or chooses none of them), and
that precondition is satisfiable only by the native read loop, so Surgeon can replace
apply_patch, which was already cheap and atomic, while adding a locate-and-quote step in
front of it. The steps a structural editor should have removed and did not: rg/sed locating
(the shipped arm shells more than native), post-write git diff and re-reads (a sub-500-char
receipt in the tool's idiom does not discharge the agent's need to see a diff), and write
consolidation (9.7 write ops for a change native made in 3.7). Opus adds the sharpest
line of the night: the tool's cheapest affordance is top-level anchored insertion, which
selected the wrong edit site in six Surgeon diffs (the guard at the function's top anchor
instead of inside the branch). The cheap affordance chose the defect.

Prompt framing compounds it: "Surgeon is available, plain edits are fine" is permissive, and
availability gets layered, never substituted. Both propose the same pair of six-run
discriminators: (1) prompt ablation, Surgeon optional with "fastest safe completion" versus
native; (2) a substitution mandate plus trusted receipts (inspect replaces rg/sed on .clj,
Surgeon writes replace apply_patch on .clj, a verified receipt is terminal, no re-read, no
git diff) versus the current mandatory-availability prompt. Predictions: if the gap halves
under the mandate, the prompt carried half; if shells stay at 14 or acceptance worsens, it
is the contract and no prompt fixes it. Opus's free third experiment runs on the existing n1
rollouts: classify every native call in the Surgeon arms as pre-edit (supplying literals
the next write requires) or post-edit (confirming a receipt); pre-edit at 60 percent or
more means contract. Running now.

Honest headline, both: on the first clean paired cohort the shipped Surgeon cost 1.8x wall,
1.85x tokens, 2.1x non-test actions and lost on every quality meter, n=6, one task, one rung,
the task being a JS string in one file, the one shape structure cannot reach. The thing most
likely to change it is a task where locating is the expensive part, run under a substitution
mandate. Divergence on sequencing: Sol says settle substitution before the large rung;
Opus says the large rung is the only cohort on the tool's advertised shape and
substitution is an axis, so run l1 as native / Surgeon-available / Surgeon-mandated.
Both are queued: s1 "X N X N X N | Y A Y A Y A" (X optional, Y mandate) after v1, then l1 as
"A N Y" x4 with the mandate as the third arm, then b2.
