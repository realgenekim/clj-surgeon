# Astra report: the ideal shape of Surgeon for an LLM

Requested by Gene at 18:28Z (relayed through Fable): review the mental models
of Surgeon, decide whether it is an ultimate editor, a Clojure interpreter, or
something else, and assess how well the API matches the ideal.

## The answer

Surgeon should not try to become Emacs, Vim, or a general Clojure interpreter.
Those tools optimize human cursor control and program evaluation. An LLM needs
a different object: a **bounded intent-to-receipt transaction kernel**.

The model already supplies architectural judgment and can write text. Its
expensive failure mode is reconstructing enough repository structure to act
safely: finding owners, following relationships, preserving source spelling,
proving cardinality, and verifying the result. Surgeon should compress that
bookkeeping into one request and one trustworthy receipt.

The ideal call has this shape:

```text
problem stated in ordinary language
  -> caller chooses a bounded structural intent
  -> Surgeon discovers/proves the exact owners
  -> Surgeon previews or applies one guarded transaction
  -> receipt contains diff, evidence, verification, refusal, and next action
```

The tool should not infer the problem's architecture. It should make a known
intent cheap to state, hard to misapply, and easy to audit.

## Three candidate mental models

### 1. Ultimate Emacs/Vim editor

This is the wrong center of gravity. A better cursor, path language, or nested
editor can make a single edit pleasant, but native `apply_patch` is already an
excellent competitor for a known small region. The measurements repeatedly
show that per-form editing loses on fan-out and often adds interaction cost.
Surgeon should preserve exact source and offer structural navigation, but it
should not compete on keystrokes, modal editing, or arbitrary text manipulation.

### 2. Clojure interpreter or transform language

This is useful as an internal mechanism, not as the product promise. Parsed
Clojure data and bounded transforms are powerful because homoiconicity lets the
tool state structural relationships directly. An unrestricted interpreter would
move architectural judgment into the tool, expand the safety surface, and make
results difficult for an agent to predict. The existing `xray`, edit DSL, and
relation machinery should remain bounded, read-first, and refusal-rich.

### 3. Agent transaction kernel

This is the productive model. Surgeon is a structural database view plus a
failure-atomic transaction layer. Reads answer a semantic question with
provenance. Writes name intent and owners, capture a snapshot, preserve
unrelated bytes, verify expectations, and return a receipt. The CLI and MCP are
transport projections over that kernel; neither should become a second logic
engine.

## Where the current API matches

| Ideal capability | Current accommodation | Assessment |
|---|---|---|
| Cheap structural perception | `:ls`, `:cat`, `:match-form`, `:xray`, `:ls-tree`, dependency and extraction reads | Strong for Clojure; native `rg` remains cheaper for literals |
| One intent over many owners | `:change!`, extraction, movement, rename, declaration repair, and guarded cross-file specs | Strongest proven square; this is where Surgeon can beat patch repetition |
| Exact source preservation | snapshot guards, scoped edits, refusal contracts, receipts | Strong, provided the caller supplies or accepts the bounded scope |
| Structured arguments without quoting | `:spec-file -` and EDN heredocs | Now strong in the production CLI; this carries the clearest ergonomic MCP lesson |
| Proof and recovery | typed refusals, expected counts, receipts, undo routes, focused verification | Good kernel; the skill must keep callers from treating a refusal as a retry loop |
| Cross-language feature threads | `feature_thread` exists in experimental MCP | Not yet a production win; it needs a CLI measurement before promotion |
| From vague problem to completed change in one step | no general natural-language intent compiler | Deliberately incomplete; adding one would smuggle architecture into the tool |

The current API therefore accommodates the ideal **after intent is bounded**.
It does not yet provide a universal “tell me the problem and finish it” verb,
and that is a feature of the safety boundary, not a missing convenience flag.

## The shape to build toward

The next API improvements should be compositional rather than a larger command
catalog:

1. **Mission-shaped reads.** Accept a bounded question and return the exact
   dossier needed by a subsequent write: owners, spans, relationships, hashes,
   and explicit absences. The read must state its search boundary and never
   imply completeness outside it.
2. **Intent-shaped writes.** Accept one semantic operation over an explicit or
   provably discovered owner set. Compile it to exact edits, preserve source
   bytes outside the scope, and return one unified receipt.
3. **A single structured request channel.** Keep EDN on stdin as the CLI's
   stable equivalent of MCP arguments. Avoid adding another shell grammar or a
   second JSON-only facade.
4. **Receipts as the agent interface.** Every result should say what was read,
   what was changed, what was verified, what was refused, and what the caller
   should do next. Wall-clock and server execution time remain separate.
5. **Evidence-gated expansion.** A new verb earns promotion only when it removes
   complete-task model returns or prevents a real false-complete error on a
   measured workload. A convenient join is not automatically a product win.

## What to leave out

Do not make Surgeon a general IDE, a shell replacement, a JavaScript parser, or
an autonomous architect. Do not route one-site edits through it merely because
it is available. Do not hide ambiguity behind a compatibility shim or report a
partial relation as complete. The native floor is part of the design: knowing
when to cede a square keeps the kernel small enough to trust.

## Beads: the model already knows how to use this shape

Gene's addendum is substantially true from Astra's seat. Beads succeeds as an
LLM tool because it presents a durable, inspectable object model rather than a
collection of clever operations. An issue has an identity, title, status,
priority, dependencies, comments, and a predictable lifecycle. The agent can
create it, query it, update it, and hand it to another agent without repeating
the whole context. Its CLI is ordinary, its records survive a turn, and its
receipts are easy to quote in the next action.

That is the part Surgeon should match. Surgeon currently has the kernel pieces
but exposes too much of them as a catalog of verbs and transport-specific
schemas. To match Beads, Surgeon should add:

- a first-class **intent/mission identity** that binds the question, snapshot,
  owners, plan, transaction, receipt, and follow-up;
- stable lifecycle states such as `proposed`, `ready`, `applied`, `verified`,
  `refused`, and `undone`, with every transition recorded in the receipt;
- durable references to evidence and commits, so a later agent can resume from
  an ID instead of rediscovering the repository;
- one discoverable `help`/`show`/`update` pattern over structured EDN, with
  `:spec-file -` as the no-quoting path;
- explicit dependencies and blockers between missions, especially when a
  refactor spans several commits or requires a human decision.

To beat Beads, Surgeon must add domain-specific proof rather than more verbs:
hash-bound source snapshots, exact owner cardinality, byte-preservation evidence,
focused verification, and failure-atomic undo. The model should be able to say
“resume intent X, apply the reviewed plan, and show the proof” with the same
confidence that it says “show issue X and close it.”

Surgeon should drop the opposite things: aliases that preserve several mental
models, MCP-only names that do not work in the stable CLI, one-off operation
schemas, and receipts that are useful only inside the originating process. A
smaller intent algebra with durable IDs will be easier for an LLM to learn than
another dozen specialized endpoints.

This is a design comparison, not a measured superiority claim. The deciding
experiment is a matched task cohort: Beads-style intent records versus today's
CLI on cross-file refactors, measuring complete verified time, model returns,
resume success after interruption, and incorrect or false-complete changes.

## Astra's ruling

Surgeon's ideal is **the shortest trustworthy path from a bounded structural
intent to a verified, reversible receipt**. Homoiconicity makes that path
especially good for Clojure because syntax, relationships, and edits can share
one data model. It does not justify turning the tool into an interpreter or
editor for its own sake.

The production CLI now has the right outer shape: stable process boundary,
structured stdin, typed refusals, and the proven fan-out operations. The main
remaining gap is not more surface area. It is mission-shaped discovery that
hands the write a complete, hash-bound dossier without requiring the model to
reconstruct the repository between calls. That is the next square worth
measuring.
