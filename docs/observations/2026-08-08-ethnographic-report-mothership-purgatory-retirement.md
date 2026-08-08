# Ethnographic report: retiring Mothership's Purgatory

**Date:** 2026-08-08

**Field site:** `/Users/genekim/src.local/mothership`

**Observed outcome:** deletion of `resources/public/js/app-purgatory.js`

**Surgeon assessment:** 8/10 overall

## Research question

To what extent did clj-surgeon change the conduct and safety of a real
architectural retirement, as opposed to merely replacing familiar file-reading
and patch commands?

The field session was unusually revealing. Mothership's source reader is a
daily-work surface. It had recently suffered ten-second loads, timeouts, broken
keyboard behavior, a missing function in an obsolete App2 viewer, and a period
when production files could not be opened. The user simultaneously wanted the
application made smaller and imposed a strict migration rule: never improve
Purgatory or Doomed in place. Build the safe Datastar/server-owned replacement,
route callers to it, and delete the obsolete implementation.

The work therefore combined urgency, active user steering, legacy code,
cross-language ownership, and a requirement to delete rather than accumulate.

## Evidence and method

This report is a bounded reconstruction from the completed Mothership work and
its terminal receipts. It uses:

- the final Mothership Captain's Log;
- the deletion commit `ae0a87d`;
- the documentation commit `a02499c`;
- the tool receipts retained in the working session;
- the permanent source-retirement tests; and
- the final Clojure, JavaScript, and live-page verification results.

It does not claim a controlled speed comparison. The session included browser
incidents, corrections, design discussion, and user interruptions. The useful
question is behavioral: what work did the tool remove from the agent, where did
it alter decisions, and where did it fail to own the route?

## Setting: two applications inside one application

Mothership contained a newer production source-reader path and an older App2
fragment/editor path. The legacy JavaScript bundles blurred ownership across
both. `app-purgatory.js` mixed browser gestures, client rendering, state, and
shell-specific behavior. `app-doomed.js` remains a larger collection of similar
responsibilities.

The user's architectural language was visceral but precise. “Purgatory” and
“Doomed” were not ordinary modules. They were quarantine zones. Editing them to
fix the current symptom would preserve the failed ownership model. Success
meant starving them of callers and then deleting them.

That framing changed what counted as progress. A working patch inside the
legacy file was negative progress. A slightly larger bounded asset that made a
whole legacy block removable was positive progress.

## Observed workflow

### 1. The agent treated the bundle as an inventory, not an implementation site

Responsibilities were named and moved one at a time. The production reader
replaced the App2 fragment/editor path. Navigation became a browser-capability
adapter. Command-center and monitor rendering moved toward server ownership.
Issue gestures gained a bounded external asset.

The legacy code was read for its executable contract, then left untouched until
the replacement was live. This was the strangler rule in actual use rather
than an architectural slogan.

### 2. Structural reads constrained attention

The Clojure shell and view files were large enough that ordinary file reads
would have flooded the working context with unrelated forms. Surgeon instead
returned the named owners needed for the current decision.

One batched call inspected three files and four forms. The important behavioral
effect was not only fewer returned characters. It gave the agent a stable unit
of thought: “this owner and these exact bytes,” rather than “some lines near a
grep result.”

Earlier in the larger Mothership session, a remembered form shape was wrong.
The structural read exposed that mismatch before a guessed patch landed. This
reinforced trust in the tool as a correction mechanism, not merely a concise
reader.

### 3. Exact source became semantic authorization

Named-form results carried source anchors. The agent copied the anchor for
`app.views.ide-layout/ide-shell` into cclsp rather than restarting discovery
with a workspace-wide text search. The semantic result resolved eight
references under one `lsp_session`.

This moment was central to the retirement. A legacy loader can be deleted only
when its consumers are known. Text search can suggest that surface; the
source-anchored semantic result authorized it.

The distinction affected the agent's language. It could say “eight resolved
references” rather than “these appear to be the relevant occurrences.” That is
a qualitative change in the proof offered to the user.

### 4. Refusals participated in the design

Not every Surgeon call succeeded. Exact counts rejected stale expectations.
Insertion refused when a source gap contained comments. A cosmetic prepared
rewrite reached fast verification, encountered unrelated existing lint
warnings, and rolled back to the original bytes.

These events did not feel like generic tool errors. They were typed boundaries
that narrowed what the agent was permitted to claim. The comment-gap refusal
said that whitespace was not empty mechanical space. The verification rollback
said the proposed transaction did not possess sufficient completion evidence.

The agent then used the documented native fallback where the operation was
outside Surgeon's supported contract. This is an important ethnographic point:
trust did not require the tool to do everything. It required the tool to refuse
honestly and identify the boundary.

### 5. Terminal receipts stopped ritual rereading

Successful structural reads returned `read_complete=true`. Successful changes
returned `verification_complete=true`. The working rule treated those fields as
terminal evidence.

Without that rule, agents commonly reopen the edited source, inspect a diff,
and run a second search to reassure themselves. Those actions duplicate the
mechanical proof and can reintroduce inconsistency. Here the receipt allowed the
agent to move directly to the next architectural slice and finally to execution
tests.

## Division of labor

The session makes the judgment boundary concrete.

| Responsibility | Primary owner |
|---|---|
| Decide that Purgatory must be strangled, not repaired | Agent and user |
| Identify browser-owned versus server-owned behavior | Agent |
| Design bounded replacement assets and Hiccup owners | Agent |
| Determine exact Clojure owner bytes | clj-surgeon |
| Resolve cross-file Var references | cclsp through the source anchor |
| Enforce expected match counts | clj-surgeon |
| Apply multi-owner changes atomically | clj-surgeon |
| Preserve bytes on refusal or failed verification | clj-surgeon |
| Write JavaScript and prose | Native tooling, by contract |
| Exercise the real browser and judge the UX | Agent and user |
| Run application test and live-route gates | Repository tooling |

The tool was most valuable where a language model is least reliable: exact
addresses, exhaustive caller accounting, balanced structural replacement,
cardinality, and remembering whether every intended site changed.

It was deliberately absent where the model's judgment mattered most: choosing
the future architecture and deciding whether a browser capability genuinely
belonged in JavaScript.

## Instrumentality assessment

### Rating: 8/10

The word “instrumental” is justified in a counterfactual sense. The deletion
could have happened without Surgeon, but the evidence and conduct would have
been different.

Without Surgeon, the likely route was:

```text
grep loaders
-> open several large namespaces
-> hand-count likely callers
-> patch each location
-> inspect diff
-> grep again
-> trust tests to reveal omissions
```

With Surgeon, the critical Clojure route was closer to:

```text
inspect exact owners once
-> carry exact anchors into semantic resolution
-> decide over the bounded surface
-> apply guarded transactions
-> accept refusal or terminal verification
```

The difference was not keystrokes alone. It moved correctness evidence earlier.
Tests remained necessary, but they no longer carried the full burden of
detecting an omitted caller or a malformed structural edit.

### Why not 10/10?

Three gaps remained visible:

1. Top-level insertion was not owned by the MCP change grammar and required a
   native fallback.
2. Direct changes and prepared-basis changes exposed different verification
   affordances, which made the caller reason about tool mode rather than only
   intent.
3. Fast verification treated unrelated pre-existing clj-kondo warnings as a
   transaction failure. Rollback was correct, but the verification boundary was
   coarser than the proposed change.

These gaps added ceremony and occasionally prevented Surgeon from being the
single paved route. They did not weaken the safety of the operations it did
accept.

## Observed emotional effect

The user's response—“OMG!!! purgatory totally deleted?”—is relevant product
evidence. The goal was not abstract code quality. Purgatory represented years
of accumulated uncertainty and a pattern that agents had previously been
tempted to patch.

The final deletion felt surprising because the migration had reduced the risk
gradually. By the time the file disappeared, the dangerous moment had already
passed. The user could celebrate a binary fact: the file was gone.

Surgeon contributed to that feeling by making intermediate cuts legible and
reversible. Confidence accumulated through receipts rather than through one
large act of courage.

## Product implications

### 1. Optimize for deletion campaigns, not only edits

The strongest product demonstration was not a complicated rewrite. It was
proving that an obsolete owner had no remaining authority. Reference surfaces,
absence guards, and whole-owner deletion should be first-class benchmark
scenarios.

### 2. Preserve the exact-source handshake

The named-form-to-source-anchor-to-cclsp path avoided rediscovery and bound
semantic evidence to the same bytes the agent had judged. That handshake was
more valuable than a generic symbol search and should remain the normal route.

### 3. Make verification change-relative

Rollback on lint failure is correct. A transaction should also distinguish a
new diagnostic from a pre-existing diagnostic so an unrelated warning cannot
block an otherwise valid bounded change without precise explanation.

### 4. Unify mutation ergonomics

The user thinks in one operation: move ownership away from this legacy
responsibility. The distinction between prepared-basis verification, direct
changes, and unsupported top-level insertion is tool-internal. A more beautiful
contract would preserve the same proof and fallback semantics behind one
decision surface.

### 5. Measure uncertainty removed

Lines changed and tool latency understate the value. Useful future measures
include:

- large namespaces not fully read;
- caller sites proven semantically rather than counted manually;
- unsafe edits refused before mutation;
- transactions rolled back with original-byte evidence;
- terminal receipts that ended a reread loop; and
- legacy owners permanently deleted.

## Outcome

The final application evidence was strong:

- Purgatory: 826 lines to zero, file deleted;
- total gross legacy deletion: 894 lines;
- net compared production JavaScript reduction: 212 lines;
- 235 Clojure tests and 1,772 assertions passed;
- 75 JavaScript tests passed;
- live IDE, command-center, monitor, and issue surfaces loaded without the
  retired asset; and
- permanent tests reject restoration of the file or its loaders.

The result supports the central clj-surgeon thesis. A language model should
make the architectural judgment once. The tool should carry the source
addresses, semantic surface, counts, transaction, rollback, and receipt.

In this field session, that division of labor did not merely produce a safer
patch. It helped make an entire bad place disappear.
