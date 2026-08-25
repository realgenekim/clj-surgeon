# Captain's Log: Clojure Became the Editor Language

**Date:** 2026-08-24  
**Status:** local self-hosting and fresh Sol/high Anvil dogfood complete;
three-arm comparative benchmark in progress

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

## Anvil production and fresh-caller dogfood

The local proof was promoted to an isolated Anvil candidate before production
changed. Commit `e6bb84d` was integrated as deploy commit `5082d9c`. The MCP
suite passed at `-Xmx512m` with **189 tests and 1,525 assertions**. The isolated
server advertised exactly four tools, including `transform_clojure`, and was
then stopped without disturbing production or clojure-lsp.

The rollback-armed production replacement ultimately passed the same catalog
gate. Its process runs as user `surgeon` from:

```text
/srv/fleet/shared-tools/clj-surgeon-5082d9c
```

It uses `-Xmx512m`. A direct canary against a `surgeon`-owned disposable
fixture performed one `inspect_clojure` read followed by one committed,
guarded `transform_clojure` write. The resulting bytes were exact.

A fresh `gpt-5.6-sol` / high caller then completed the computed repeated-edit
task exactly on its first mutation attempt:

| Result | Observation |
|---|---:|
| exact final bytes | true |
| complete caller wall | 51.23 s |
| overall actions | 5 |
| MCP mutation calls | 1 |
| mutation tool | `transform_clojure` |
| failed mutation attempts | 0 |

Raw evidence is retained on Anvil at:

```text
/srv/fleet/dev-a/clj-surgeon-study-results/20260825T024835Z-transform-clojure-sol-high-canary/
```

This advances the capability ladder through **fresh caller succeeds**. It does
not yet pass the controlled efficiency gate. The 51.23-second wall belongs to
the complete model turn, not the editor call alone, and has no matched native
or `edit_clojure` control yet.

### Benchmark admission and one useful refusal

Benchmark head `20fffb8` adds the four-tool catalog to clean callers, counts a
committed transform as a mutation, rejects preview-only transforms as commits,
scores route violations as incorrect, and exposes one repeatable Anvil canary
command. Its schedule, route, portfolio, and harness self-tests are green.

The first three-seat dispatch stopped before any model launch because the
shared benchmark checkout was owned by root and Git correctly reported dubious
ownership to dev-a, dev-b, and dev-c. No global `safe.directory` exception was
added. The failed preflight directories are retained; the retry uses exact
`20fffb8` seat-owned checkouts. This is an orchestration refusal, not a model,
MCP, or structural-edit result.

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

## Ethnographic finding: subsecond scalpels can still create four-minute work

The first local repair of the published `transform_clojure` example exposed a
caller-side performance failure. The exact field failure was small:
`expect-count` returned a guarded selection map, while terminal `transform`
accepted only a vector path. A native broad read could have exposed the
relevant builders and compiler in one perception round. Instead, the primary
Codex seat serialized several narrow structural reads and deliberated between
them. Individual `inspect_clojure` calls took 39--792 milliseconds, but the
human-visible repair loop took roughly four minutes.

The bounded usage receipt for 2026-08-25T00:49:16Z through
2026-08-25T03:30:20Z confirms the pattern:

| Measure | Observed |
|---|---:|
| `inspect_clojure` calls | 39 |
| request bundles across those calls | 59 |
| median requests per call | 1 |
| median inspect wall | 118 ms |
| total inspect tool wall | 10.020 s |
| source characters returned | 106,954 |

The tool was not the dominant clock. The caller forfeited batching and paid a
model reasoning boundary after nearly every cheap read. This is the structural
equivalent of pressing one organ key, stopping to rethink the score, then
pressing the next.

The corrected default loop is:

```text
one batched perception snapshot
  -> one compiled guarded transaction
  -> one warm semantic proof
```

Follow-up source reads are admitted only when the first receipt exposes a new
uncertainty that could not have been named in the initial batch. The server
need not parallelize tiny filesystem reads for this gain; batching them into
one model-facing call removes the expensive serialization.

The same dogfood pass found a second feedback defect: `make mcp-reload`
depended on `mcp-test`, so invoking reload immediately after a green suite
reran all 187 tests and 1,518 assertions before performing an immediate nREPL
reload. The documented workflow already says `make mcp-test` followed by
`make mcp-reload`. Reload is now the hot publication gesture; verification
remains an explicit preceding gate.

After the composition repair, the exact public example compiled through the
warm JVM in 0.9 seconds. A direct live `transform_clojure` preview against the
real `max-transform-matches` form completed in 0.5 seconds, selected exactly
one leaf, proposed `128` to `129`, reported the source and result hashes, and
left source unchanged with `next_action=commit`.

A fresh same-model local control then exercised the complete caller route:

| Route | Exact | Wall | Tool sequence | Failed mutations |
|---|---:|---:|---|---:|
| `transform_clojure` | yes | 25.712 s | one committed MCP transform | 0 |
| native | no | 21.347 s | one bounded `rg`, then one file change | 0 |

The transform caller immediately used the repaired public expression and
preserved every unrelated byte. The native caller made the intended semantic
change but also deleted an unrelated final blank line. Therefore native is not
admitted to the efficiency comparison. The 4.365-second raw wall advantage for
native is useful pressure on the structural interface, but it is not a win
under the exactness gate. Raw evidence is retained at:

```text
/tmp/clj-surgeon-local-computed-pair-dogfood-20260824T203306/
```

### First model-routing probe

Four additional one-replicate local cells used the same fixture, prompt,
correctness gate, and one-call transform route:

| Caller | Exact | Wall | Total input | Reasoning output |
|---|---:|---:|---:|---:|
| Sol/high | yes | 25.712 s | 44,100 | 360 |
| Sol/medium | yes | 22.936 s | 44,181 | 269 |
| Terra/high | yes | 20.435 s | 43,998 | 360 |
| Terra/medium | yes | 20.552 s | 43,935 | 250 |
| Terra/low | yes | 20.262 s | 44,532 | 311 |

Every caller emitted the same 395-character successful MCP receipt, used one
committed transform, performed zero source reads, and had zero failed
mutations. Terra's three effort levels are separated by only 290 milliseconds,
which is noise at one replicate. Sol/high versus Terra's approximately
20.4-second cluster is a larger hypothesis, not yet a claim.

This suggests a two-stage model-routing architecture:

```text
strong judgment model: discover and decide the change
                         |
                         v
fast materializer model: render one bounded SCI program
                         |
                         v
deterministic compiler: locate, count, hash-fence, commit, prove, undo
```

Once the complete decision is explicit, higher reasoning effort did not buy
observable correctness in these five cells. The next admitted experiment is
three or more rotated replicas per tier on historical computed/repeated edits.
Do not generalize this result to discovery, extraction, semantic caller
completeness, or architectural judgment.

Raw model-routing evidence:

```text
/tmp/clj-surgeon-local-transform-terra-high-20260824T204021/
/tmp/clj-surgeon-local-transform-sol-medium-20260824T204146/
/tmp/clj-surgeon-local-transform-terra-medium-20260824T204310/
/tmp/clj-surgeon-local-transform-terra-low-20260824T204505/
```

### Scale probe: constant gesture, noisy wall

The first ten-site caller exposed a tool-description failure rather than an
SCI mechanism failure. The prompt asked for all ten `:retry-delays` values,
but the caller copied the one-owner example `(form 'retry-policy)`. The actual
owners were numbered, so the exact count guard refused ten expected versus
zero found and left source unchanged. Native changed all ten intended values
but again deleted an unrelated final blank line.

The public contract was repaired to distinguish two editor roots and to omit
the redundant inline count guard:

```clojure
;; one known owner
(-> (form 'owner) ... (transform f))

;; every structural match in this file
(-> [] (match :retry-delays) right (transform f))
```

The next fresh Sol/high caller chose the file-wide root on its first call. The
same relation was then tested at 10, 30, and 60 sites:

| Sites | Route | Exact | Wall | Mutation actions |
|---:|---|---:|---:|---:|
| 1 | SCI transform | yes | 25.712 s | 1 |
| 10 | SCI transform | yes | 25.815 s | 1 |
| 30 | SCI transform | yes | 25.033 s | 1 |
| 60 | SCI transform | yes | 26.339 s | 1 |
| 10 | native | no | 28.783 s | 1 |
| 30 | native | no | 45.551 s | 1 |
| 60 | native | no | 22.400 s | 1 |

SCI caller wall remained within a 1.306-second band while the exact edit count
grew sixtyfold. This proves the desired constant-effort editor-macro property
for one homogeneous relation. It does not prove a monotonic wall advantage:
native `apply_patch` can also carry many homogeneous hunks in one action, and
the native one-replicate wall varied wildly. Every native scale cell deleted
an unrelated final blank line and therefore failed exact admission.

The important ergonomic speedup happened at the refusal boundary. Teaching
the correct root changed the ten-site route from a 38.227-second safe refusal
to a 25.815-second exact commit, a 1.48x wall improvement and one eliminated
recovery round. The route to 2--5x is therefore not site count by itself. It is
removing repeated model boundaries: reacquisition reads, malformed first
programs, recovery calls, patch fragmentation, and redundant verification.

Raw scale evidence:

```text
/tmp/clj-surgeon-local-computed-10site-sol-high-20260824T205252/
/tmp/clj-surgeon-local-computed-10site-rootfix-sol-high-20260824T205646/
/tmp/clj-surgeon-local-computed-30site-sol-high-20260824T205822/
/tmp/clj-surgeon-local-computed-60site-sol-high-20260824T210122/
```

## Verification receipt

- focused programmable-edit tests: **5 tests, 28 assertions, all green**;
- core/CLI suite: **605 tests, 5,225 assertions, all green**;
- MCP suite at `-Xmx512m`: **187 tests, 1,518 assertions, all green**;
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
