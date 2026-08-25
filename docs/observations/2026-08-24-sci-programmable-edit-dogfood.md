# Captain's Log: Clojure Became the Editor Language

**Date:** 2026-08-24  
**Status:** local implementation and self-hosting proof complete; clean-agent
comparative benchmarks pending

## The question

If clj-surgeon already contains a capability-limited Clojure interpreter, why
should an LLM enumerate concrete replacement text? Why not let it write a tiny
editor program over Clojure structure, then compile that program into ordinary
guarded edits?

This was the missing sixth architecture candidate. It is not hypothetical.
The CLI already had SCI compilation, structural paths, pure `transform`,
lossless addresses, concrete plans, source hashes, an atomic transaction
compiler, read-back verification, and inverse receipts. The experiment asked
whether those pieces could become a model-facing MCP gesture.

## What we tried

All initial probes ran inside the existing clj-surgeon MCP JVM through its
embedded nREPL. No additional JVM was started. The probes first exercised the
real pure compiler against in-memory source, then connected it to the existing
addressed transaction compiler. No real file was written until the path had
passed fidelity, refusal, and virtual-commit tests.

### One computed leaf

```clojure
(-> (form 'retry-policy)
    (match :delays)
    right
    (transform #(mapv (partial + 100) %)))
```

The program compiled `[100 250 500]` into `[200 350 600]`. Its concrete diff
touched only the selected vector. An inline comment immediately after the
vector and every unrelated byte survived. The plan contained the old source,
new source, structural address, source hash, result hash, and inverse material;
the function itself was gone.

**Feeling:** excellent. This was the first route that felt like operating an
editor rather than filling out an edit API. The intent and the action were the
same small expression.

### One broad sexpr

The next program selected the enclosing map and ran `assoc`:

```clojure
(-> (form 'retry-policy)
    initializer
    (transform #(assoc % :max-attempts 4)))
```

It produced a semantically correct map but canonicalized the layout and
dropped an inline comment from the proposed replacement. The command was
plan-only, so nothing was damaged.

**Feeling:** a clean warning shot. Homoiconicity is not losslessness. Ordinary
Clojure data forgets concrete spelling, whitespace, comments, reader syntax,
and some metadata placement. “Run a function on the sexpr” is not a safe
definition of a source editor.

### Three repeated leaves

The existing CLI transform required exactly one selection. Selecting three
`:timeout` values refused as ambiguous. Transforming their enclosing vector
computed the right values but collapsed three formatted map lines into one.

The live prototype instead applied the function to every exact selected leaf,
then lowered the results separately. Its first attempt retained both semantic
paths and preorder addresses. The paths were intentionally identical across
the repeated maps, so later sites could not be resolved uniquely. Removing the
non-unique paths and retaining the frozen preorder addresses produced three
disjoint edits:

```diff
-100
+150
-250
+300
-500
+550
```

**Feeling:** this was the breakthrough. The missing primitive was not an
interpreter. It was a tiny adapter from an N-node selection to the transaction
compiler that already existed.

### Fidelity and concurrency

A more hostile fixture contained duplicate values, metadata, commas, multiline
maps, and inline comments. Two distinct `100` leaves became two `150` leaves.
The complete future source was byte-identical to changing only those six
numeral characters.

An incorrect expected count refused before compilation. A virtual atomic
commit succeeded and returned read-back hashes. When a concurrent comment was
added after compilation, the same compiled transaction refused with
`source-hash-mismatch` and performed no write.

**Feeling:** the safety model is natural. The model describes `f(old)` once;
the tool captures each exact `old`, materializes each `new`, and the existing
compare-and-swap fence guarantees that the file still equals the snapshot at
commit. This is much closer to a database transaction than an editor macro.

## The MCP gesture

The local implementation publishes `transform_clojure` as the fourth
clj-surgeon MCP tool:

```json
{
  "workspace_root": "/workspace",
  "file": "src/policy.clj",
  "expression": "(-> (form 'configs) (match :timeout) right (transform #(+ % 50)))",
  "expect": {
    "matches": 3,
    "max_changed_characters": 9
  },
  "commit": true
}
```

The exact match count and changed-character budget are mandatory. Preview is
the default. `commit=true` is the one-call route for an already-decided bounded
relationship. Every match becomes a distinct concrete addressed edit. The
future file must parse; commit compares the complete source hash, writes
atomically, reads back, and returns an inverse receipt.

One-shot commit currently refuses when a selected subtree itself contains a
comment. The caller must narrow the selection or review the preview. The tool
is bounded to 128 matches and 262,144 generated characters. SCI remains
capability-limited but not termination-proof.

## The self-hosting moment

After the MCP surface and tests were green, the live tool edited its own
implementation:

```clojure
(-> (form 'max-transform-matches)
    initializer
    (transform #(quot % 2)))
```

With `matches=1`, a three-character budget, and `commit=true`, it performed:

```diff
-(def max-transform-matches 256)
+(def max-transform-matches 128)
```

The call returned one compiled edit, atomic commit success, a complete-file
read-back hash, and a concrete inverse receipt. This was not a simulated plan:
the new editor safely edited the code that implements the editor.

**Feeling:** wow. This is the first evidence that the “church organ” metaphor
may be attainable. The LLM can visualize a relation, play one compact chord,
and let the instrument materialize all exact keystrokes with guards.

## Dogfood defects

The first preview returned the entire future file in structured output. The
human-facing summary was compact, but that payload defeated the purpose of an
editor gesture. It was removed immediately. Preview now returns only file,
counts, budget use, safety flag, source/result hashes, and diff.

Final skeptical review also found an evidence-label bug: an unrecovered write
failure could have inherited `source_unchanged=true` from a generic refusal.
The tool now claims unchanged source only for a pre-write hash mismatch or a
confirmed rollback. A dedicated adverse-path test protects that distinction.

## What feels genuinely better

- Computed intent is stated once instead of materialized at every site.
- Duplicate old values are safe because identity is structural, not textual.
- The model does not need to quote replacement source through a shell or JSON
  for every site.
- The executable program is ephemeral. Commit and undo contain only concrete
  source edits.
- Exact count and churn budgets feel like natural assertions around an editor
  macro.
- The final source-hash fence directly answers “did `old` change underneath
  us?”
- Better models can write better bounded transforms without waiting for a new
  catalog operation.

## What does not feel better

- One known literal replacement is still clearer as `from`/`to`.
- Broad sexpr transformation can destroy concrete source qualities while
  remaining semantically valid.
- A generated program adds its own syntax, vocabulary, and runtime failure
  surface.
- Syntactic selection does not prove semantic caller completeness.
- Extraction, namespace rewiring, and binding-sensitive changes still deserve
  tested semantic operations.
- Capability restriction does not prove termination or a small allocation
  footprint.

## Architecture judgment

The implementation suggests one stack with two gestures:

```text
direct literal edit -------------------------+
                                              v
optional handles -> SCI transform -> guarded concrete edit IR -> atomic commit
                          ^
                  tested semantic helpers
```

`edit_clojure` remains the default for a literal one-site change.
`transform_clojure` should be preferred when the replacement depends on the
current leaf or one pure relation applies to several exact sites. Both lower to
the same guarded concrete edit representation. Semantic refactor compilers
remain authoritative for cross-file architectural invariants.

The open question is frequency, not feasibility. Sol/high ranked the
programmable transaction first; Fable ranked it second behind direct guarded
edits. Both independently converged on the same substrate: F compiles to A.

## Verification receipt

- focused programmable-edit tests: **5 tests, 28 assertions, all green**;
- core/CLI suite: **604 tests, 5,222 assertions, all green**;
- MCP suite at `-Xmx512m`: **187 tests, 1,514 assertions, all green**;
- four-tool stdio discovery smoke: green;
- benchmark, onboarding, retention, and evidence self-tests: green;
- live MCP contract synchronized without a JVM restart;
- branch-live CLI and Codex/Claude skills installed locally.

## Next experiment

Run fresh Sol/high and Fable callers on historical counterfactual commits with
three arms:

1. direct guarded edits only;
2. programmable transform as the default;
3. direct edits for literals plus transform for computed/repeated leaves.

Measure exact completion, first-program validity, total wall time, tool actions,
input/output payload, refusal count, unrelated-byte changes, and the crossover
at 1, 3, 10, and 30 sites. The keep gate is zero silent wrong-site or
unrelated-byte edits and a material wall-time win on the admitted computed-edit
stratum. The ≥5x claim remains unearned until fresh-agent counterfactual replays
replicate it.
