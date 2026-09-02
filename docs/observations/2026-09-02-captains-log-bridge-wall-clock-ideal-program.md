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
