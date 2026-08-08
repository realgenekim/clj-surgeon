# Captain's Log: Editing From Inside the Tool

**Date:** 2026-08-07

**Question:** How does editing feel when I use clj-surgeon for Clojure,
native patching for Clojure, and native patching for JavaScript?

This is a first-person field note, not a controlled benchmark. It records the
mechanical state I had to carry while finishing a real cross-layer refactor and
adding a missing editor primitive. A formal history study can follow later.

## The first useful contrast

The consumer refactor ended with one obsolete compatibility adapter, one live
caller, and one obsolete test. The semantic surface was three sites in three
files.

With the new whole-site delete decision, the complete edit was:

```text
remove-adapter/s01  delete owner
remove-adapter/s02  replace one nested call
remove-adapter/s03  delete owner
```

One `apply_clojure_changes` call committed all three edits, read every file
back, ran the fast verification profile, and returned one receipt. The call
took 6.2 seconds, including about 4.0 seconds of lint and formatting. I did not
carry line numbers, write order, partial completion, or rollback state.

The experience matched the intended product model:

```text
one semantic decision
  -> one complete surface
  -> three small choices
  -> one verified transaction
```

The tool was not merely a safer patch. It removed mechanical edit state from
working memory.

## Clojure without Surgeon felt heavier

Surgeon could not express whole-site deletion before this work. I therefore
used native patches to bootstrap the primitive into existence.

The patches were fast, but I owned more state:

- function definition order and forward references;
- delimiter correctness across several nested functions;
- the distinction between a form's source and its attached comments;
- stable inverse offsets after a deletion shifts later forms;
- formatter cleanup;
- linter failures at exact interop call sites;
- the aggregate relationship between compiler, receipt, schema, help, and
  tests.

Native patching did not corrupt the files. The burden was cognitive: every
patch was locally plausible, but I had to reconstruct the system-wide state
after each one. One forward-reference mistake and three linter corrections
were cheap, yet they are precisely the bookkeeping the structural compiler is
supposed to own.

## JavaScript felt direct but manually bounded

The browser-controller changes were locally easy to patch and easy to test.
For a small function body, native patching remains an excellent control.

The weakness appeared at the cross-file boundary. I had to establish safety
through searches, file knowledge, and a dedicated JavaScript test suite. There
was no equivalent of a complete structural surface followed by one atomic
transaction. The edit syntax was easier; the proof of completeness was more
manual.

This suggests a narrower claim than “structural editing always wins”:

| Situation | Best-feeling route in this session | Why |
|---|---|---|
| One small arbitrary body edit | Native patch | One decision already maps to one edit |
| One exact nested Clojure form | Roughly tied | Surgeon adds proof; native patch has less ceremony |
| Related Clojure edits across owners or files | Surgeon | One decision remains one transaction |
| Cross-file JavaScript behavior | Native patch plus tests | No structural transaction surface exists yet |

## Dogfood produced one bout of food poisoning

The first live delete transaction refused even though the new schema had been
published. Hot reload had refreshed the MCP schema and change-buffer code but
not the transaction kernel. The live server therefore advertised the new
decision while executing the old compiler.

This was a useful failure:

- the whole transaction refused;
- all consumer source remained unchanged;
- the diagnostic identified the first invalid addressed edit;
- retry required no recovery archaeology.

The developer loop was still wrong. `make mcp-reload` now reloads the complete
routed dependency chain, including the transaction kernel, before it publishes
tool schemas. Its test manifest pins that closure. The retry succeeded without
restarting the server.

## Whole-site delete earned its place

The public basis decision is deliberately small:

```json
{"site":"rename/s03","delete":true}
```

It means: delete the prepared owner and its contiguous leading comment block.
It does not mean “replace the form with an empty string.” The compiler:

- protects the namespace form;
- validates the retained structural address and source bytes;
- compiles mixed delete and replace operations into disjoint byte spans;
- applies spans from right to left;
- validates the complete result files;
- stores exact inverse spans for durable undo;
- rolls back the complete transaction if verification fails.

The operation fits obsolete definitions, call sites, tests, and `declare`
forms created or retired during a move or hoist.

## A small edit remains a useful control

After the refactor, one test had a stale nested expected map. Reading its exact
owner took 19 ms. One direct Surgeon edit then updated and verified the map in
about 100 ms of tool time.

That felt good, but not transformative. A native patch would also have been
reasonable. Surgeon's advantage was that the exact owner, match count, parse,
write, and read-back proof were bundled. The number of semantic decisions was
already one, so there was little cognitive state to remove.

## Working hypothesis

The product becomes irresistible when it preserves the size of the model's
decision:

```text
one decision in the model
  == one transaction at the tool boundary
```

The strongest observed win is not keystroke reduction. It is the disappearance
of partial mechanical state. The next field notes should watch for three
signals:

1. Did I have to remember which sites remain?
2. Did I reread or diff after terminal evidence?
3. Did one semantic decision fragment into several mutation calls?

When the answers are no, no, and no, the tool feels like a structural
exocortex. When they are not, the friction is product evidence.

## Cognitive win proven; wall-clock win still open

The field run proves a usability advantage for related Clojure edits. It does
not yet prove an end-to-end speed advantage.

The observed direct timings are encouraging but incomplete:

| Operation | Observed tool wall | What it includes |
|---|---:|---|
| Exact form read | 19 ms | One file, one named test form |
| Exact nested write | about 100 ms | Compile, atomic write, parse, and read-back |
| Three-site delete/edit/delete | 6.2 s | Three files plus about 4.0 s of lint and formatting |

These numbers omit some or all model reasoning, skill loading, semantic
discovery, prompt processing, and recovery time. They therefore cannot answer
the product question by themselves. A 100 ms receipt is not a faster refactor
when the caller spends another 20 seconds deciding what to send.

The correct measurement unit is the complete correct agent turn:

```text
prompt received
  -> source understood
  -> complete change committed
  -> required verification passed
  -> terminal answer emitted
```

The comparison must preserve the same prompt, fixture, expected bytes, model,
runtime, and correctness gate. It must separate startup and skill cost,
semantic discovery, mutation, verification, and recovery. Incorrect runs do
not become fast runs.

Local bead `clj-surgeon-xey` now owns this experiment. It requires fresh native
controls and matched Surgeon lanes across four task strata: one exact nested
edit, one three-site transaction, one repeated multi-owner change, and one
exploratory change whose affected surface is not supplied. Raw logs remain
local; only compact aggregate evidence and anonymized findings belong in Git.

The current falsifiable hypothesis is:

> Native patching wins or ties when one decision already maps to one local
> edit. Surgeon wins wall-clock time when semantic discovery or related
> mechanical edits would otherwise require enough rounds to exceed its setup
> and verification cost.

The crossover point is unknown. Until `clj-surgeon-xey` measures it, the honest
claim is cognitive compression and transaction safety—not proven speed.

## Judgment time should become a larger fraction of a smaller whole

The ideal workflow does not maximize tool calls per second. It makes the tool
calls fast and bounded, then spends the remaining time on the decision only:

```text
fast structural read
  -> judgment about behavior and scope
  -> fast surgical transaction
  -> project verification
```

If navigation and mutation overhead collapse, judgment can become a larger
percentage of the task even when judgment takes the same absolute time and the
complete task becomes faster:

```text
native route    10 s judgment / 40 s total = 25%
Surgeon route   10 s judgment / 17 s total = 59%
```

That is the desired shift. The model should spend less time on fake thinking:
recovering context, rediscovering sites, tracking partial completion, checking
parentheses, reconstructing a diff, and diagnosing command syntax. Those are
mechanical delays, not judgment.

A complete semantic surface can expose a consequence that deserves additional
thought. That extra time is valuable only when it improves correctness or
avoids recovery. The timing study must therefore separate:

- source navigation and semantic discovery;
- decision latency after sufficient evidence exists;
- mutation and receipt latency;
- project verification;
- refusal and recovery;
- unclassified quiet time.

It must not label every gap between tool events as “thinking.” The target is a
higher share of deliberate judgment inside a lower end-to-end wall clock.

## The first shared-workspace proof broke the idempotency claim

The live acceptance run recorded one hot clj-surgeon process and one hot cclsp
process, then alternated `clj-surgeon up` between clj-surgeon and server2. The
Surgeon process stayed hot. cclsp restarted after every invocation.

The cause was deterministic but wrong. The cclsp config upsert removed the
selected workspace entry and appended its replacement to the end of the
`servers` vector. Repeating `up` twice for the same workspace was a no-op, so
the existing test passed. Alternating two already-onboarded workspaces changed
their order on every call:

```text
select workspace A  -> [other, B, A] -> restart
select workspace B  -> [other, A, B] -> restart
select workspace A  -> [other, B, A] -> restart
```

This is not idempotent at the system boundary. Selecting an existing workspace
must preserve its position and exact serialized configuration. A new
alternating-workspace regression now defines that stronger contract. The live
two-workspace proof will be repeated only after it passes.

The experiential lesson is useful: a pure helper can be locally idempotent and
still create operational churn when callers alternate valid inputs. The field
gate must model the actual usage sequence, not only `A -> A`.

## The address must itself be a form

One tiny product correction exposed the difference between textual context
and a structural address. I first tried to replace this binding pair:

```clojure
change-id (:change-id result)
```

The direct MCP route refused it. The pair is two adjacent forms inside a
binding vector. It is not one form. The refusal preserved every byte.

The intended change was inside the value expression, which is one complete
form:

```clojure
(:change-id result)
```

Using that address, Surgeon changed the implementation and its test in one
two-file transaction. This felt like learning the correct Vim motion. The
operator was already capable; the caller had selected the wrong structural
unit. The default skill now teaches this move for bindings, map entries, and
`case` clauses.

## Current experiential breakpoint

The editing modes now feel different for a principled reason:

| Work | Best-feeling route | Experience |
|---|---|---|
| One arbitrary local edit | Native patch | The decision already maps to one hunk |
| One exact Clojure form | Surgeon or native | Surgeon adds cardinality and read-back proof |
| Related Clojure edits | Surgeon | One decision remains one atomic transaction |
| JavaScript or TypeScript body | Native patch plus type/test gate | Textual blocks and compiler feedback fit well |
| Markdown or prose | Native patch | Structural Clojure machinery adds no value |

The strongest subjective change is calm. With Surgeon, I do not carry line
numbers, hashes, partial completion, aggregate diff state, or delimiter risk.
The remaining friction is also clearer: top-level insertion and some sibling
operations still require either a different structural address or a native
fallback. Those are product boundaries, not reasons to weaken the transaction
contract.
