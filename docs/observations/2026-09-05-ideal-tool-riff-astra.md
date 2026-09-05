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
