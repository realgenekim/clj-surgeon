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
