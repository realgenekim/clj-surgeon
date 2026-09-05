# Astra riff: the tool I would love to use

Requested by Gene at 18:35Z (relayed through Fable): open the aperture,
brainstorm the ideal LLM tool, prototype its feel with a tabletop exercise,
estimate wall-clock wins, and connect the design to the organizational wiring
behind a winning system.

This is a design riff and tabletop estimate, not a benchmark result. The
estimates use the observed constraint that tool execution is only a few percent
of wall time while model returns and orientation dominate.

## The object I want

I want one durable object called a **mission**. It is the Beads insight joined
to Surgeon's structural proof:

```clojure
{:mission/id "m-7f2c"
 :question "Thread the new option through every call to submit!"
 :root "/repo"
 :scope {:languages [:clojure] :paths ["src" "test"]}
 :state :proposed
 :evidence []
 :plan nil
 :receipt nil
 :next-action nil}
```

The model does not need to remember which read found which owner. It asks to
`show` the mission, narrows the scope, accepts or edits the plan, then says
`apply`. The mission records the snapshot, owners, exact edits, verification,
commit, refusal, and undo relationship. An interrupted agent resumes by ID.

The ideal interaction is five verbs, regardless of the number of domain
operations underneath:

```text
open   "state the problem and boundary"
show   "return evidence, candidates, and blockers"
plan   "compile one reviewed intent into exact edits"
apply  "commit the guarded transaction and verify it"
resume "continue or undo a named mission"
```

Each verb accepts structured EDN on stdin and returns bounded EDN. Human prose
is a field in the mission, not a second parser hidden inside every operation.

## What makes this better than an editor

An editor gives the model places to put text. The mission gives it a memory of
why the text is being changed and what proof is still owed. The editor remains
available for the final unusual hunk; the mission is the control plane for
structural work.

What makes this better than an interpreter is the refusal boundary. The tool can
evaluate a bounded relation over parsed syntax, but it cannot invent an
architecture, widen a root, or silently call arbitrary code. Every computed
answer carries its evidence and every write carries an expectation.

## Tabletop: one real refactor

Scenario: add a keyword option to a Clojure function used in eleven namespaces,
update call sites, preserve formatting, and run the focused test suite.

| stage | native agent today | current CLI/skill | ideal mission tool |
|---|---:|---:|---:|
| orient and identify owners | 4–7 returns, 90–180 s | 3–5 returns, 60–130 s | 1–2 returns, 20–45 s |
| state exact intent | 1–2 returns | 1 return after manual reads | 1 structured `plan` return |
| edit 11 owners | 2–4 patch rounds, 40–100 s | 1 guarded fan-out, 10–30 s | 1 `apply`, 5–20 s |
| verify and recover | 2–4 returns, 60–150 s | 1–3 returns, 30–90 s | 1 receipt, 20–60 s |
| estimated complete wall | **190–430 s** | **110–250 s** | **55–125 s** |

The ideal range predicts a **1.7–3.5x** complete-task improvement over native
and **1.3–2.0x** over the current CLI route for high-fan-out work. The second
comparison is intentionally modest: the current CLI already owns the strongest
write square. The ideal earns its additional gain by removing orientation and
resume bookkeeping, not by making a file rewrite faster.

## Tabletop: where the ideal deliberately loses

Scenario: change one known expression in one file.

| route | likely wall | ruling |
|---|---:|---|
| native `rg` + `apply_patch` | 10–30 s | winner |
| current CLI | 20–60 s | cede |
| ideal mission | 20–60 s | cede unless the change is already inside an open mission |

The mission must be able to say “native is cheaper” and close without creating
ceremony. A universal front door that always opens a ledger would recreate the
tool tax we already measured.

## Tabletop: interrupted work

An agent opens a mission, discovers that two call sites are generated, and the
session ends. Today the next agent repeats orientation. With a mission ledger:

```text
resume m-7f2c
show blockers
plan --scope src/generated.clj --decision "exclude generated output"
apply
```

The value is not only seconds. It prevents the next agent from mistaking a
partial search for a complete answer. This is the strongest Beads-inspired
addition: durable context with explicit incompleteness.

## The winning organizational wiring

The tool is only half the system. A useful deployment has five small roles:

1. **Mission broker** owns IDs, scope, state transitions, and resumability.
2. **Evidence broker** owns structural reads, hashes, absences, and budget.
3. **Intent compiler** turns a reviewed mission into exact operation facts.
4. **Transaction runner** owns snapshots, formatting scope, writes, rollback,
   and receipts.
5. **Verification gate** runs focused tests/lint and publishes the terminal
   state.

These are policy seams, not necessarily five services. In the current code they
should remain small namespaces over one kernel. The important wiring is that a
mission ID crosses every seam and that no seam invents architectural intent.

The human/agent organization should mirror the lifecycle:

- the requesting agent owns the question and accepts the scope;
- Surgeon owns evidence and mechanics;
- the verifier owns the green gate;
- a reviewer sees one mission receipt and one diff, not a transcript archaeology
  exercise;
- the next agent resumes by ID rather than reopening the whole repository.

This is “slowify” in the useful sense: slow down the irreversible decision at
the plan boundary, then make the mechanical path boring and fast. It is the
same wiring principle as a strong operating organization: explicit ownership,
small handoffs, durable records, and fast feedback at the boundary where
mistakes become expensive.

## Prototype sequence

The smallest credible prototype does not add a new parser or MCP server:

1. Add a CLI `mission` directory containing one EDN record per mission.
2. Implement `open`, `show`, `plan`, `apply`, and `resume` as thin adapters over
   existing `:cat`, `:change!`, extraction, and verification routes.
3. Store source hashes and operation receipts; refuse stale resumes.
4. Add one high-fan-out fixture and one interrupted-session fixture.
5. Compare native, current CLI, and mission routes with the same caller and
   count returns, wall, correctness, resume success, and false-complete claims.

Do not begin with a natural-language compiler. Begin with a durable mission
record that makes the existing kernel easier for an LLM to use.

## Astra's bet

The next big jump is not a faster structural rewrite. It is eliminating the
model's repeated orientation and context reconstruction while preserving the
right to refuse. Beads supplies the durable object and lifecycle mental model;
Surgeon supplies structural evidence and atomic proof. The ideal tool is their
intersection, with native tools remaining a first-class escape route.

The bet is worth a two-hour prototype because the upside is measurable and
narrow: if the mission route does not remove at least two model returns on the
fan-out fixture without lowering acceptance, it has not earned another layer.

## Round two: Astra answers Fable

Fable's argument on `origin/MCP/main` is persuasive on the central point. The
mission ledger is not a prettier editor. It is a durable boundary between
model judgment and repository computation. I would use it, and I would ask Gene
to fund a prototype. The attack is on scope and proof, not on the object itself.

### 1. Is the dossier a second thing to read?

Sometimes. That is the sharpest risk. A dossier that merely repeats the output
of three `rg` calls is a tax, and a capable model will route around it. The
dossier earns its turn only when it closes a question the model cannot cheaply
close itself: exact owner cardinality, transitive closure, stale-source hashes,
generated-file absences, or a proof obligation shared by many edits.

The product rule should therefore be adaptive:

```text
small / known / literal       -> native tools, no mission
wide / structural / guarded   -> mission dossier
already-open mission          -> append evidence without reopening ceremony
```

The mission broker should expose an estimated cost and a native escape route.
If it cannot predict at least one saved return or one avoided proof failure, it
should recommend ceding the square. This keeps the ledger aligned with the
Bitter Lesson: computation is general and cheap, ceremony is hand-built tax.

### 2. When does lifecycle state matter?

Fable is right that a single-turn edit does not need five visible states. The
state matters at the first interruption, handoff, refusal, or review boundary.
The v1–v7 landing loop is exactly the failure mode: each round re-derived
context because no durable mission ID carried the evidence forward.

The design should hide lifecycle ceremony on the fast path. `open` may return a
mission ID automatically for a qualifying fan-out request; `show` should be
optional when `apply` can consume the reviewed plan directly. The state becomes
visible when the mission blocks, crosses a turn, or needs undo. Beads' lesson is
not “make every edit an issue”; it is “make durable work addressable when it
becomes durable.”

### 3. Is `propose` the next square?

Yes, with one correction. `propose` is mission-shaped discovery only when it
returns a write-ready dossier, not when it is a renamed outline operation. It
must discover owners, prove the boundary, name absences and ambiguities, bind a
source snapshot, and estimate the transaction. A proposal that still asks the
model to perform the same searches has not earned a new verb.

The minimum useful proposal is:

```clojure
{:mission/id "M-12"
 :state :ready
 :snapshot {:files 11 :hash "..."}
 :owners [{:file "src/a.clj" :form submit! :span [42 49]}]
 :closure {:count 28 :complete? true}
 :ambiguities []
 :plan {:operation :thread-parameter :edits 21}
 :proof {:expected-tests ["test/app_test.clj"]}
 :next-action [:apply "M-12"]}
```

That is a different object from `:ls-tree`; it is also deliberately narrower
than a natural-language architect. The model still decides whether the plan
means the right thing.

### Conviction

I would use the mission tool because it follows the Bitter Lesson in the only
way that matters here: it delegates scalable repository enumeration, hashing,
and execution to computation while leaving meaning and scope to the model. It
does not ask the model to learn a clever editor grammar. It gives the model a
general object it can inspect, resume, accept, reject, or undo.

I would reject the tool if its first prototype adds a new command for every
task, requires a dossier for a one-site edit, or reports “complete” without a
declared search boundary. The winning prototype is a thin mission ledger over
the existing kernel. Its pass condition is the one Fable names: remove at least
two model returns on a high-fan-out task, preserve acceptance, and show a native
control that remains faster on the small square.

## Ten mission ideas from Astra

These are deliberately different shapes. Each has a bounded proof gate and a
cheap prototype path; none deserves a product verb until it beats its native
control.

| # | mission | why it might win | first proof gate |
|---:|---|---|---|
| 1 | `thread-parameter` | one intent across every direct caller | exact owner count, parse, focused tests |
| 2 | `add-require` | fan-out namespace bookkeeping | no duplicate aliases, byte/churn bound |
| 3 | `extract-helper` | dependency closure plus move | closure complete, source-preserving diff |
| 4 | `rename-namespace` | cross-tree identity change | all defining and referring sites accounted for |
| 5 | `repair-declares` | remove stale declarations after movement | kondo delta and unchanged behavior |
| 6 | `feature-thread` | join JS/Clojure evidence with named absences | five slots, no false-complete result |
| 7 | `test-impact` | select the smallest proving suite | every changed owner covered or explicit gap |
| 8 | `migration-plan` | sequence several safe missions | dependency DAG and resumable checkpoints |
| 9 | `stale-resume` | continue after compaction or interruption | snapshot mismatch refuses before write |
| 10 | `native-adjudication` | tell the model when to cede to patch/rg | predicted cost agrees with measured winner |

My option-value ranking is **9, 1, 10, 6, 8, 3, 2, 7, 4, 5**. Resume and
adjudication test the mission object itself; parameter threading and
feature-thread test the two hardest evidence shapes; extraction, rename, and
declare repair already have mature CLI routes and offer less uncertainty.

## Fast-typing arrangement

Spark/Sol should be the fast typist, never the authority. Astra (or another
strong caller) supplies the mission question, scope, acceptance predicate, and
stop condition. The fast model may fill a structured request or choose among
already-proved plan alternatives. The gate then checks:

```text
frozen snapshot -> exact owner/cardinality proof -> dry-run diff
                 -> parser/lint/test gate -> commit or rollback
```

Three caller modes are worth comparing:

1. **Strong author + fast executor:** Astra states the intent; Spark emits the
   EDN and follows the receipt.
2. **Fast author + strong reviewer:** Spark proposes; Astra accepts, narrows, or
   refuses before any write.
3. **Two fast proposals + strong adjudication:** Spark and Sol independently
   produce plans; Astra compares hashes, owners, and proof obligations.

The third mode has the most option value because disagreement is evidence. It
must be charged for both proposals, though; parallel typing is a win only when
the adjudication prevents a costly retry or wrong edit.

## Prototype batch

The first batch should exercise ideas 9, 1, 6, 3, and 10. For each, run native,
current CLI, and the mission-shaped request on the same frozen fixture. Record
complete wall, model returns, tool calls, refusal count, source churn,
acceptance, and resume success. Promote a shape only if every accepted arm is
correct and it removes at least two returns or prevents a false-complete result.

The 10% Codex budget should fund the comparison and the proof gate, not a large
implementation. If the first five shapes do not show a return reduction, stop
and keep the durable mission record as a design note. If one does, build only
that adapter over the existing kernel.

## Initial CLI dogfood

After installation, three independent fast-typing probes exercised the current
surface:

- `:cat :spec-file -` read a two-owner manifest from stdin and returned one
  hash-bound semantic receipt. This is the strongest current mission-shaped
  primitive because the request crossed the shell as data.
- `:deps` and `:ls-extract` on `parse-up-args` returned exact owner facts and an
  exclusive zero-dependency extraction candidate. These are useful plan inputs,
  but they still leave mission identity and resume state to the caller.
- `:ls-tree :format :edn` mapped the repository successfully after the first
  text-shaped invocation produced no immediate output while the scan completed.
  The delayed result is a concrete reason for a mission receipt to expose
  progress and a bounded next action instead of making the caller guess whether
  the process is idle.

These probes did not claim a speedup. They narrowed the prototype: start with a
durable wrapper around batched `:cat`, dependency/extraction reads, and a
resumable plan receipt; do not begin by reimplementing the structural kernel.

## Hands-on mission feel (2026-09-05)

I ran `bin/mission` from `clj-surgeon-mission` against a fresh scratch
workspace containing one helper and one caller. The first `propose` felt right:
one EDN request produced `M-1`, a ready state, a snapshot hash, destination,
owner count, footprint, and an explicit `[:apply "M-1"]` next action. `show`
returned the same object without making me rediscover anything. The first
`apply` refused safely because I failed to pass the verification profile; the
receipt named the missing authority and left source bytes unchanged. Repeating
with the same structured spec reached `:verified` in about **9.9 seconds wall**
from first proposal through terminal receipt; the guarded kernel reported
**22.6 ms** of operation time. The resulting source and new namespace were
exactly inspectable, and the receipt included an undo receipt. The feel is
already closer to Beads than to an editor: the useful unit is `M-1`, not the
individual edit. The main friction is that `apply` must repeat enough request
context to load profiles; a durable mission should make that impossible to
forget. The failure was valuable: the ledger made a missing proof authority
visible before mutation, which is precisely the bounded-mission contract.
