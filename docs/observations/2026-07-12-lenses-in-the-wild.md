# Ethnographic Study: Structural Lenses in the Wild

**Date:** 2026-07-12  
**Observed interval:** 2026-07-11 15:31 PDT–2026-07-12 01:16 PDT  
**Primary setting:** live repair and feature work in `social-media-writer`

## Method

This study reconstructs lens use from full local Codex session history, not
from recollection or the final Git diff. It follows the sequence:

> operator intent → agent hypothesis → exact command → tool result → changed
> understanding → edit or refusal

The corpus contains three production sessions plus the live
`social-media-writer` ethnographic journal. Help invocations, documentation
examples, transcript copies, and the clj-surgeon implementation session were
excluded from the usage count.

| Direct production invocation | Count |
|---|---:|
| `:find-subform` | 21 |
| `:replace-subform` plans | 9 |
| `:replace-subform!` applies | 8 |
| **Total** | **38** |

The one-plan/apply difference is the useful failed attempt containing invalid
JavaScript `\x27` escapes. An independent adversarial review is discussed
separately because it tested the tool rather than using it on production work.

## Executive Finding

Lenses quickly became more than a safer replacement mechanism. Agents used
them in four distinct modes:

1. **Orientation:** reduce a huge namespace to one named form and one nested
   structure.
2. **Executable inquiry:** test whether the code has the shape the agent thinks
   it has; zero matches are evidence, not merely failure.
3. **Guarded mutation:** turn an exact subtree into a reviewable capability
   bound to one source snapshot.
4. **Sequential refactoring:** make several small structural edits while every
   successful apply deliberately invalidates the previous address and hash.

The central qualitative shift was captured by the working agent:

> The replay address plus source hash makes it feel like AST-addressed surgery
> instead of “hope this regex still matches.”

The strongest utility appeared in deeply nested Hiccup, large handler/state
forms, repeated storage-boundary code, and route tables. `rg` remained the
preferred broad-discovery tool.

## Episode 1: Discovery Becomes Proof

During the Captain editor save-race investigation, `:ls` reduced a 4,036-line,
322-form `state.clj` to the synchronization and session-admission forms. Three
scoped searches then proved that there was exactly one:

- Save Draft Hiccup button inside `editor-pane`;
- unsafe browser-sync `set-draft!` inside `handle-save-from-draft`;
- `save-session!` inside the old `handle-save`.

The paths did explanatory work. They showed, for example:

```text
editor-pane
  → div#editor-pane.editor-pane
  → div.editor-actions
  → button.action-btn.secondary
```

and:

```text
handle-save → let → binding ack → state/save-session!
```

This was the first important behavioral change: the agent stopped treating a
text hit as sufficient evidence. It reported uniqueness, ancestry, source, and
snapshot identity together.

The comparison with ordinary tools was not absolutist:

- `rg` was faster for discovering candidate symbols across files.
- Lenses were stronger once the question became “is this exact syntax inside
  this exact owner, and how many times?”
- `sed` remained useful for reading a bounded form after its location was
  established.

## Episode 2: The First Production Apply

The first write targeted a 13-line inline-JavaScript “Edit in Draft” Hiccup
button. The search pattern used `_` for irrelevant style and handler subtrees
while retaining the visible label and keyboard hint. It returned one match.

The first plan failed before planning because its generated JavaScript used
`\x27`, which is not a valid EDN/Clojure string escape. The agent adapted by
using `pr-str` to generate JavaScript string literals. The corrected plan was
reviewed and replayed successfully with result hash:

```text
37ef0b96dedc9e00477b4f0872fa1bb45fb0519f0742cbb8806fe097e3219e64
```

The actual edit collapsed a long inline `fetch` program into the shared
single-flight `editBookNodeInDraft(...)` command.

Two findings appeared immediately:

1. The plan/apply ceremony was not perceived as friction. It was the reason the
   agent was willing to perform the edit in a live, heavily modified file.
2. Shell, EDN, Clojure, and embedded JavaScript form a real quoting boundary.
   The tool needs friendly parse errors and first-class documentation for it.

## Episode 3: A Zero Match Corrects the Mental Model

While diagnosing a title editor that disappeared after an SSE morph, the agent
first searched for the whole expected title-bar vector:

```clojure
[:div#draft-node-title-bar.draft-node-title-bar _ _]
```

It returned zero matches. Instead of falling back immediately to line search,
the agent refined the hypothesis to the distinctive initializer map:

```clojure
{:data-star-signals "{draftTitleEdit:false}"}
```

That returned exactly one match at the semantic path through `editor-pane`,
`div#editor-tabs`, and `div#draft-node-title-bar`. The plan then changed it to:

```clojure
{:data-star-signals__ifmissing "{draftTitleEdit:false}"}
```

Apply succeeded with result hash
`2d2df7465bc71186f1789a19d404ec94ccfdaf59040773fa5a5c04f118524822`.

This episode reveals an underappreciated use: **a structural selector is an
executable hypothesis about code shape**. Exact-match failure taught the agent
that its assumed child count was wrong; a smaller semantic invariant found the
real fault.

## Episode 4: Critical State Surgery

The most consequential lens use occurred in `transform-apply-tx`. Telemetry
showed that Transform Apply changed the server draft, but the browser later
overwrote it with its unchanged textarea. Investigation also found that the
pure transaction read and wrote the top-level `:draft` projection rather than
the canonical stable-ID Book node.

The search history shows another hypothesis refinement:

```clojure
(assoc _ :draft _)  ; zero matches
(assoc :draft _)    ; exactly one match
```

Once the architectural decision was made, two nested replacements changed:

```clojure
(or (:draft st) "")
```

to:

```clojure
(active-editor-draft st)
```

and:

```clojure
(assoc :draft new-draft)
```

to:

```clojure
(sync-draft-tx {:draft new-draft :allow-blank-draft? true})
```

Both applies succeeded, producing sequential result hashes `cece291d…88a3`
and `d1f80435…7d04`. Tests then proved that the stable-ID node and editor
projection change together while a neighboring node remains untouched.

The agent’s final assessment was explicit:

> clj-surgeon was valuable here: its hash-guarded subform replacement made the
> critical nested state rewrite precise.

This is the strongest evidence for the project’s governing boundary. The tool
did not decide the state model, identify the canonical draft, or design the
single-flight transaction. It performed the dangerous mechanical substitution
after those judgments were settled.

## Episode 5: Repeated Boundary Repair, Then Consolidation

A later storage-boundary repair used lenses four times in the same large
`routes.clj`:

1. In `handle-inbox-import`, replace raw `io/file` + `.mkdirs` + `.renameTo`
   with `storage/ensure-dir!` + `storage/move-entry!`.
2. Apply the same structural repair in `handle-inbox-dismiss`.
3. Replace the new two-operation block in import with
   `(move-inbox-entry-to-done! inbox filename)`.
4. Apply the same helper consolidation in dismiss.

All four plans matched exactly once and all four applies succeeded. Their
result hashes were:

```text
c4bdf9db…631c
6eb333bf…b14
fe128d21…673
34cbb506…98a
```

This two-pass sequence is revealing. The agent first restored the correct
storage abstraction, then recognized duplication and introduced the helper.
Lenses supported evolving judgment rather than requiring the final abstraction
to be known at the beginning.

The snapshot model also behaved correctly: every apply changed the file hash,
so the next edit required a newly generated plan. A plan acted like a temporary
capability for one source generation—not a durable command that could be
replayed later against whatever happened to be on disk.

## Episode 6: Read-Only Mapping of Declarative Structures

Lenses were also used without any intended replacement:

- locating the exact `/api/transform` route vector inside `make-routes`;
- checking nested navigation state transitions;
- finding a Datastar signal initializer and click expressions;
- testing whether a raw draft mutation existed inside a particular state
  transaction.

The route-table path was only:

```clojure
[{:form make-routes} {:vector true} {:vector true}]
```

The match was still useful, but this exposes a quality gradient in semantic
paths. Hiccup tags, map attributes, calls, and bindings read well; anonymous
nested vectors do not. Declarative route/schema paths need better descriptors
if the path itself is meant to explain structure rather than merely accompany
the source snippet.

## Operator and Agent Behavior Changed

The history shows rapid institutionalization:

- The operator explicitly reminded agents: “remember clj-surgeon” and “use
  clj-surgeon.”
- The global agent instructions were amended to require `:ls` before reading
  large Clojure files and recommend lenses for nested targets.
- Agents began announcing lens use before edits and reporting match counts,
  paths, and hashes as evidence.
- An agent voluntarily chose lenses for a critical state-layer edit after the
  operator had only requested use of clj-surgeon generally.
- A retrospective bead was queued from inside the production session rather
  than after the fact.

This matters because tool utility is partly behavioral. Lenses supplied a
repeatable ceremony that agents could remember: find, prove uniqueness, plan,
review, apply, test.

## Friction and Failure Modes

### 1. Pattern exactness is both the safety feature and the learning curve

`_` matches one subtree, not zero-or-more children. Whole-vector searches often
failed when the agent guessed the arity incorrectly. Refining toward a smaller
distinctive map or call worked well, but the tool does not explain *why* a near
match failed.

### 2. Nested-language quoting fails too early and too noisily

Invalid `\x27` produced a reader exception and stack trace before a structured
plan error. The successful workaround—single shell quotes plus Clojure `pr-str`
for embedded JavaScript literals—is sound, but error handling should teach it.

### 3. Exit status is not truthful

The independent review showed that stale-plan and ambiguity errors print
`{:error ...}` but exit `0`. This undermines `&&`, CI, and agent orchestration.

### 4. Trailing forms are silently ignored

The adversarial review demonstrated that both:

```clojure
:match '(spit tmp source) (unrelated)'
:with  '(foo) (bar)'
```

accept only the first form. This violates the command’s apparent contract and
is a release blocker for an agent-facing mutation tool.

### 5. Plan vocabulary was guessed incorrectly

One production session tried to select `:source-sha256` and `:result-sha256`
from a plan whose actual keys are `:source-hash` and `:result-hash`. The edit
still succeeded, but the absent values show that machine-readable output needs
stable names, examples, and perhaps an explicit schema version.

### 6. One edit per plan creates ceremony during paired refactors

The storage repair required four plan/apply cycles. That was safe and still
fast, but a composable plan with independent exact-one assertions could reduce
round trips while retaining per-file snapshot protection.

### 7. The diff is reviewable, not truly unified

The compact `@@ line N @@` before/after string worked for small nodes. It became
less pleasant for multiline Hiccup and lacks file headers and surrounding
context.

## What Lenses Are Actually Good For

The history supports these high-confidence use cases:

- one small expression buried in a very large named form;
- repeated Hiccup controls distinguished by ancestry and visible content;
- exact state transitions where changing the wrong `assoc` would be dangerous;
- repeated mechanical boundary repairs in similar handlers;
- declarative route/config entries;
- proving absence or uniqueness before architecture work;
- operating safely in a dirty, fast-moving worktree where line numbers and old
  patches go stale quickly.

The history does **not** support replacing `rg`, inferring semantics, selecting
architectural boundaries, or automatically extracting free variables. The
“dumb structural kernel + smart agent” division held up unusually well.

## Product Recommendations from Observed Use

### Release blockers

1. Require exactly one complete form for both `:match` and `:with`; reject
   trailing syntax.
2. Exit nonzero for every `{:error ...}` result, including ambiguity and stale
   plan refusal.
3. Convert parse failures into concise machine-readable errors rather than
   reader stack traces.

### Highest-leverage improvements

4. Add a plan schema/version and keep hash field names consistent in help and
   examples.
5. Emit a real unified diff or a separate human preview alongside EDN.
6. Add composable plans with one assertion per edit and hashes for every source
   file.
7. Improve semantic breadcrumbs for anonymous vectors, route entries, binding
   pairs, and map values.
8. Add optional near-match diagnostics such as “same vector prefix, different
   child count,” without weakening exact replacement semantics.
9. Offer `:plan-out path.edn` so agents need not depend on shell redirection.
10. Record selector, tool version, source hash, result hash, and validation
    results as explicit provenance.

### Do not build yet

- automatic free-variable inference;
- similarity-ranked extraction suggestions;
- macro-semantic reasoning;
- silent multi-match selection.

The observed value came from exactness, understandable refusal, and a small
contract. Expanding into semantic refactoring before fixing the CLI contracts
would weaken the part that is already earning trust.

## Conclusion

In less than ten hours, lenses moved through four stages:

```text
wishlist born from hand-patching pain
  → structural discovery during a live incident
  → first guarded Hiccup replacement
  → repeated use for state, UI, storage, and route work
```

The most important finding is not that AST editing is possible. It is that a
small syntax-aware tool changed how agents established evidence before editing.
They began asking structurally precise questions, treating zero matches as
model correction, binding writes to reviewed snapshots, and leaving semantic
judgment outside the tool.

That is a genuine comparative advantage over `rg + sed`: not universal search
speed, but disciplined certainty at the moment a nested edit becomes dangerous.

## Evidence Sources

- `~/.codex/sessions/2026/07/11/rollout-2026-07-11T11-09-54-019f525f-212b-75a2-976f-541e593dbea3.jsonl`
- `~/.codex/sessions/2026/07/11/rollout-2026-07-11T11-28-53-019f5270-8511-7c40-93c6-a493c33f6147.jsonl`
- `~/.codex/sessions/2026/07/12/rollout-2026-07-12T00-12-48-019f552b-e5c1-76b0-8e57-4caa146248ba.jsonl`
- `../social-media-writer/docs/ethnographic-study-2026-07-11-live.md`
- [First-use observation](2026-07-11-subform-first-real-use.md)
