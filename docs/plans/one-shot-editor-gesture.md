# One-shot editor gesture

**Status:** Thin named `edit_clojure` adapter locally green, live, and
self-hosted; fresh local caller selection experiment pending
**Motivating evidence:** The Sol/high exact-nested trial needed a locator
fallback and then misplaced `expect` inside a prepared edit. The eventual
semantic edit succeeded, but whole-file formatting changed an unrelated EOF
blank line. A guarded token replacement that should feel like two editor
keystrokes therefore needed multiple protocol rounds.

## Outcome

`apply_clojure_changes` accepts a small `edits` entrance for the common case:
replace one exact Clojure subtree inside one named top-level form. The caller
states point, old value, and new value. Surgeon resolves the point, binds an
implicit source anchor, checks the old value, and commits through the existing
verified transaction kernel in one call.

```json
{
  "workspace_root": "/repo",
  "edits": [{
    "file": "src/bench/pair_view.clj",
    "within": {"form": "route-event"},
    "from": ":done",
    "to": ":complete",
    "matches": 1
  }],
  "verify": "fast"
}
```

## Bitter-Lesson Boundary

This entrance compiles explicit mechanical intent. It does not infer which
symbol, form, file, or architectural change the caller meant. It adds no MCP
tool and no mutation kernel. Semantic discovery, extraction, owner deletion,
binding-aware rename, and multi-site transforms remain on their existing
specialized surfaces.

## Public Contract

- `edits` is a non-empty array and is mutually exclusive with `basis`,
  `decisions`, `changes`, and `extraction`. A redundant top-level `expect` is
  discarded before compilation and reported in `input_normalization`; exact
  per-edit guards remain authoritative.
- Each edit is a closed object containing `file`, `within`, `from`, `to`, and
  optional positive integer `matches`. `within` initially contains exactly one
  `form` string.
- `file` is a confined project-relative Clojure source path. `from` and `to`
  are non-blank strings containing one complete Clojure form.
- Each edit defaults to exactly one match inside exactly one named owner.
  `matches: N` requests exactly N replacements inside that owner; zero, fewer,
  or more refuse before write. Aggregate change, edit, and distinct-file counts
  are derived by Surgeon.
- `verify` remains optional and accepts `fast` or `full`.
- Success returns the ordinary verified transaction receipt and inverse.
- Missing, duplicate, ambiguous, stale, unparsable, or out-of-root targets
  refuse before write with `source_unchanged=true`.

The implementation assigns deterministic diagnostic IDs (`edit-1`, `edit-2`,
...) and compiles to the existing direct `changes` contract. There is no
second executor.

## Anchor Model

The one-shot call performs locate, anchor, compare, and replace atomically.
The implicit anchor is bound to the canonical file, named owner, source
revision, exact matched subtree, and byte span. A later compatible extension
may let `inspect_clojure` return an opaque `loc_...` handle for repeated edits,
like an Emacs mark or vi position. Reusable handles are optional acceleration,
not a prerequisite for the easy case, and must refuse when their source digest
or expected subtree is stale.

## Safety Invariants

- One user gesture becomes one failure-atomic kernel transaction.
- Match cardinality is always exact and positive. It defaults to one; callers
  may declare a known multiplicity but cannot request an unbounded replacement.
- The exact old subtree is the compare-and-swap guard.
- All target files are confined before source mutation.
- No partial write, receipt, or claimed verification survives a refusal.
- Unrelated bytes remain unchanged. Compact surgical edits do not invoke the
  whole-file formatter, so formatter baseline drift cannot widen their diff;
  configured verification checks still run.
- Existing `changes`, retained-basis, and extraction requests remain behaviorally
  compatible.

## Implementation Shape

Add a pure edit-gesture validator/compiler beside the existing direct
contract. It emits ordinary direct-change JSON-shaped data, which is validated
again by the existing contract and compiled by `tool-params->transaction`.
Routing, path confinement, structural selection, staged verification, commit,
receipt, rollback, and telemetry stay unchanged.

The public JSON Schema gains one mutually exclusive `edits` branch. The live
server reloads the schema and handler namespaces over its existing nREPL and
resynchronizes `tools/list` without a process restart.

## Test Plan

Pure matrix:

- one valid edit and several edits across repeated/distinct files;
- Java JSON containers;
- every missing, extra, blank, malformed, absolute, parent, and wrong-extension
  field;
- mixed routing fields and invalid verification profile;
- derived IDs and exact aggregate counts.

Boundary matrix:

- one nested fixture edit preserves every unrelated byte;
- zero or undeclared duplicate matches refuse without a write or receipt;
- an exact declared multiplicity replaces all N matches atomically;
- stale old text refuses;
- a later edit failure rolls back earlier candidates;
- read-back, verification, inverse receipt, and undo remain truthful;
- formatter baseline drift cannot widen the committed diff.

## Local-First Verification Gates

1. [done] Format changed Clojure files.
2. [done] Pass focused pure and boundary tests through the local persistent
   nREPL: 48 tests, 498 assertions, zero failures or errors.
3. [done] Pass the repository MCP suite in a disposable bounded JVM:
   `-Xms32m -Xmx512m` completed 181 tests and 1,476 assertions with zero
   failures or errors. Focused lint also passed through live transactions.
4. [done] Record the live MCP PID and CWD, hot-reload in place, and prove the PID did
   not change.
5. [done] Use the reloaded `edits` entrance to change clj-surgeon itself, then prove
   exact bytes, stale-target refusal, tests, receipt, and undo.
6. [pending] Achieve 10/10 fresh local clean-agent one-call exact successes and bind the
   evidence to the implementation hash.
7. [done by explicit authorization] Run one three-seat Anvil canary before the
   formal 10/10 local gate to learn whether fresh Sol/high agents discover and
   one-shot the interface.

## Local self-host evidence

On 2026-08-23 the live MCP remained PID 48029 with CWD
`/Users/genekim/src.local/clj-surgeon`. Its nREPL on port 56170 reloaded the
schema, compiler, and handler; `tools/list` advanced the contract hash from
`76b1b91f` to `e71c3a67` with no server restart.

The first attempted fixture repair omitted `matches`; because the guarded map
occurred twice inside the named test, Surgeon refused with
`expect-count-mismatch` and left source unchanged. After adding the bounded
`matches` capability and hot-reloading, one live `edits` call changed both
maps atomically: 2 edits, 1 file, verified read-back, and terminal evidence.
Replaying the same call refused stale source without writing. The repaired
single-match and new exact-two-match boundary tests both prove commit, stale
replay, inverse receipt, and undo.

The round-two repair was also dogfooded on live MCP PID 75495 with CWD
`/Users/genekim/src.local/clj-surgeon`. A fresh raw MCP session submitted a
compact edit with a deliberately contradictory aggregate `expect`. Surgeon
reported that normalization, changed one exact symbol inside
`tool-description`, verified and receipted it, refused the stale replay, and
then used the same route to restore the idiomatic spelling. The live transaction
did not restart the server. Its fast profile currently has no hot nREPL entry,
so automatic namespace refresh remains a separate paved-road gap; the
sanctioned `make mcp-reload` path resynchronizes handlers and `tools/list`
without a process restart.

## First Anvil canary

At implementation SHA `14beaf5`, three fresh Sol/high agents ran the same exact
nested-edit task without a skill hint. One of three independently discovered
the compact `edits` route, but its first two attempts added redundant top-level
`expect`, which the then-strict contract rejected. Its eventual successful edit
and one direct-transaction run both allowed whole-file formatting to remove an
unrelated EOF blank line; one agent repaired that drift manually. The native
control was exact. The resulting gates were therefore 1/3 discovery, 0/3
compact one-shot success, and 0/3 byte-exact compact success without repair.

Round two directly targets those observations: tolerate and report redundant
aggregate `expect`, front-load the minimal request shape, and preserve every
unrelated byte by skipping whole-file formatting for compact edits. The next
three-seat canary must use the same task and no skill hint so its result is
comparable.

## Second Anvil canary

At implementation SHA `5c118e1`, three fresh Sol/high agents ran the identical
no-skill task. All three ended at the exact target hash and preserved every
unrelated byte, but none chose compact `edits`. Two used native reads and native
file changes. One used `inspect_clojure` followed by the older direct `changes`
route. Its guarded mutation verified, but direct-route formatting removed the
EOF blank line. The two native routes also removed that blank line. Every agent
detected and repaired it before completion.

The resulting gates are 3/3 exact final bytes, 0/3 compact discovery, 0/3
compact one-shot success, and 0/3 repair-free first mutation. This falsifies
the hypothesis that normalizing redundant `expect`, improving the description,
and fixing compact byte preservation are sufficient to make the overloaded
tool self-revealing. It does not falsify compact-edit mechanics, because no
canary invoked them.

The next local-first candidate is a thin `edit_clojure` MCP discoverability
adapter backed entirely by the existing compact compiler and transaction
kernel. Compare that name against the current `apply_clojure_changes` entrance
and a native control on the same task before another Anvil run. Full evidence
and startup-memory synthesis are in
`docs/observations/2026-08-24-anvil-round-two-and-startup-memory-synthesis.md`.

## Thin named adapter local proof

The adapter adds one MCP contract and no executor. `edit_clojure` now publishes
only `workspace_root` and `edits`, points at the same `handle-clj-change` Var,
and returns the same structured output and receipts as
`apply_clojure_changes {edits: ...}`. The internal editor compiler still accepts
`verify` for the general apply entrance, but the named keystroke-like tool does
not expose it: parse, compare-and-swap, atomic write, read-back hash, receipt,
and undo are mandatory; formatter, linter, and test gates belong to the general
transaction tool.

Outside-in development produced the intended red gate: 181 tests ran, with
five failures and two errors confined to the new three-tool registry
expectations. After adding the adapter, the first green attempt exposed four
HTTP live-registry expectations that still assumed two tools. Updating those
addition/removal laws produced 181 tests, 1,481 assertions, zero failures, and
zero errors in a disposable `-Xmx512m` JVM.

`make mcp-reload` kept live MCP PID 75495 with CWD
`/Users/genekim/src.local/clj-surgeon`, advanced the contract hash from
`48f1a369` to `8c84890a`, and upserted only `edit_clojure`. A fresh raw MCP
session then proved the new three-tool list and the adapter's closed schema.

The new tool immediately self-hosted. One `edit_clojure` call changed the
adapter's own `:id` subtree, verified the write, and returned receipt hash
`2e296422`. Replaying the identical call refused with
`expect-count-mismatch` and `source_unchanged=true`. A reverse
`edit_clojure` call restored the source and returned receipt hash `e927e11b`.
A subsequent two-file `edit_clojure` batch renamed the two stale “exactly two
tools” test Vars with one verified transaction and receipt hash `fbf0ecad`.

The first newly wired local caller exposed two benchmark/interface defects. The
clean harness had hard-coded a two-tool allowlist, so `edit_clojure` was
invisible. Once admitted, Sol/high selected it immediately, but optional
`verify` invited two formatter-gated refusals on an intentionally byte-exact
fixture before a third call without verification succeeded. Permanent harness
and schema tests now require all three tools and forbid `verify` on the narrow
editor entrance. A fresh cohort must establish the repair-free rate.

## Documentation and Release Checklist

Update the MCP description, README example, installed skill copies, and
interface study with the final admitted syntax and receipt. Decide the thin
`edit_clojure` adapter through the local comparison above. Document reusable
location handles only after their lifecycle and stale-source behavior are
implemented.

## Definition of Done

A fresh local agent performs the exact nested edit in one `edit_clojure` call,
Surgeon changes only the intended subtree,
returns a verified undoable receipt, refuses the stale replay without writing,
and repeats this 10/10 times. The same live MCP process self-hosts at least one
follow-up implementation edit after hot reload. The three-seat Anvil learning
canary is evidence, not admission; the formal 10/10 local gate remains open.
