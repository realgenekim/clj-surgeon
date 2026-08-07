# Atlas Paper Exercises: seven real goals on this codebase

**Status:** Design validation for the [Three Rounds roadmap](three-rounds-roadmap.md)
**Method:** the same paper discipline that ran 15 edits through the
structural change language before implementation. Each exercise states a
real goal on the clj-surgeon codebase itself, the route a caller pays today,
the ideal atlas manifest and result shape, and the rounds accounting. The
exercises are scored honestly: several change the design, which is the point.

**Grounding:** form names, line ranges, and namespace facts below come from
one live `inspect_clojure` call (three outlines, 383 ms, 0 source
characters, hash-bound), not from memory.

## E1 — The trace: "How does an MCP call become bytes on disk?"

The canonical onboarding question for this repo.

**Today:** outline `mcp_http_server.clj`, read `handle-clj-change`, read
`execute-request!`, notice `transaction/run!`, outline
`intent_transaction.clj`, read two more forms. Five to seven rounds, most of
them source-bearing, with the caller carrying the growing call chain in
working context.

**Atlas:**

```clojure
{:requests
 [{:id "route" :operation "witness-path"
   :from 'clj-surgeon.mcp-tool/handle-clj-change
   :to :effect/file-write
   :paths 1}]}
```

```clojure
{:path
 [{:node 'mcp-tool/handle-clj-change  :lines [191 203] :calls-via :direct}
  {:node 'mcp-tool/execute-request!   :lines [142 189] :calls-via :direct}
  {:node 'intent-transaction/run!     :calls-via :direct}
  {:node 'transaction-write/commit!   :calls-via :direct
   :effect {:kind :file-write :authority :sink-allowlist}}]
 :elided-siblings 3
 :authority :static-kondo}
```

One round. The caller expands only the node that surprises it.

**Finding (design change):** `:to :effect/file-write` requires a small
static registry of effect sinks (`spit`, `io/writer`, `Files/write`, and the
repo's own atomic-write helper). This is an allowlist with a named
authority, not inference — substrate rule 2 forbids guessing effects, and a
declared sink registry is the honest version.

## E2 — The impact cone: "I want to change `normalize-success-receipt`'s return shape. What breaks?"

`mcp-contract/normalize-success-receipt` (lines 249–280) shapes the receipt
callers depend on.

**Today:** `rg normalize-success-receipt`, read each hit's surrounding form,
separately guess which tests exercise it. Four to six rounds; test coverage
remains a guess.

**Atlas:**

```clojure
{:requests
 [{:id "cone" :operation "callers"
   :of 'clj-surgeon.mcp-contract/normalize-success-receipt
   :depth 2 :tests true}]}
```

```clojure
{:callers
 [{:node 'mcp-contract/classify-kernel-result :sites 1 :edge :direct-var-call}
  {:node 'mcp-tool/execute-request!           :sites 1 :edge :direct-var-call}]
 :tests-reaching
 [{:node 'mcp-contract-test/success-receipt-shape-test :relationship :direct}
  {:node 'mcp-http-server-test/protocol-regression-test :relationship :transitive}]
 :unresolved-edges 0}
```

One round, and the answer includes the verification surface — which tests
must go red — not just the call sites.

**Finding (design change):** test-reachability edges are cheap. clj-kondo
already resolves var usages from test namespaces; tagging edges whose
`:from` namespace matches the test source path is pure indexing. Promote
test edges into build step 2 rather than treating them as a later luxury.

## E3 — The broadcast edit: rename `refuse!` to `refuse-params!`

`mcp-contract/refuse!` (lines 66–78) has one definition and many call sites
inside the namespace.

**Today:** grep, read call sites to confirm none are strings or comments,
patch several hunks, diff, hope.

**Atlas — manifest symmetry end to end.** The read *is* the write skeleton:

```clojure
{:requests
 [{:id "sites" :operation "match" :file "src/clj_surgeon/mcp_contract.clj"
   :match "(refuse! _ _ _)" :as :manifest-skeleton}]}
```

returns a pre-filled change manifest — `:in`, `:find`, per-owner counts
proven — with `:do` empty. The caller adds one operator and submits the same
artifact to `apply_clojure_changes` with `:expect` already correct. Two
rounds total, and the cardinality guard was never hand-counted.

**Finding:** `match` patterns with fixed arity (`_ _ _`) miss the 4-ary
`& [data]` call sites. The skeleton generator must surface arity spread in
its negative space ("3 sites at arity 3, 2 at arity 4") or the pre-proven
`:expect` silently under-counts. Negative space applies to *match results*,
not only collapsed tree nodes.

## E4 — The keyword flow: "Who produces and who consumes `:error-type`?"

The repo's error contract promises a stable keyword `:error-type` on every
refusal.

**Today:** `rg ':error-type'` returns hits in docstrings, tests, CHANGELOG,
and markdown — the caller pays rounds separating syntax from prose, and can
still not tell producers from readers.

**Atlas:** keywords are first-class kondo analysis records:

```clojure
{:requests
 [{:id "flow" :operation "keyword-flow" :keyword :error-type
   :roles [:assoc-key :map-literal-key :lookup :destructure]}]}
```

```clojure
{:producers [{:node 'mcp-contract/refuse! :role :map-literal-key :sites 1} ...]
 :consumers [{:node 'core/format-error :role :lookup :sites 2}
             {:node 'mcp-contract-test/... :role :destructure :sites 7}]
 :prose-occurrences {:count 23 :not-indexed true}}
```

One round; strings and comments are structurally excluded; the
producer/consumer split — which no grep can make — is the answer itself.

## E5 — The self-referential check: "Is the atlas about to duplicate `analyze.clj`?"

The live outline shows `analyze.clj` already owns `intra-ns-deps`,
`unreferenced-forms`, `extraction-closure`, `dep-tree`, and
`topological-sort` (lines 427–598) — a working single-namespace dependency
engine built on rewrite-clj.

**Resolution, recorded as doctrine:** the substrate has two authorities with
a clean seam. Within one namespace, `analyze.clj` (rewrite-clj, lossless,
already tested) is the edge authority. Across namespaces, clj-kondo's
resolved var usages are. The atlas joins them at the namespace boundary; it
must not reimplement either. This is substrate rule 2 applied to our own
house: every edge names which of the two engines proved it.

## E6 — The candidate interrogation: prove the E3 rename before committing

**Today:** apply, read the aggregate diff, reread anything the diff made
doubtful.

**Atlas:** apply with `:candidate true`, then ask the future one question:

```clojure
{:requests
 [{:id "delta" :operation "neighborhood-delta"
   :snapshot "candidate-a442"
   :around 'clj-surgeon.mcp-contract/refuse-params!}]}
```

```clojure
{:edges-added   [['mcp-contract/validate-fields! 'mcp-contract/refuse-params!] ...]
 :edges-removed [['mcp-contract/validate-fields! 'mcp-contract/refuse!] ...]
 :newly-unreachable ['mcp-contract/refuse!]
 :new-unresolved-sites []}
```

`:newly-unreachable` showing the *old* name is exactly the receipt a rename
wants; anything else in that field is the bug, found before commit, without
one diff line entering context. Commit is then `{:promote "candidate-a442"}`.

## E7 — The open hunt: "Find every place that builds a refusal map by hand instead of calling `refuse!`"

The exercise that breaks the current schema — deliberately.

**The tension:** `inspect_clojure` today is tuned for questions *known up
front*: `forms` requires known names, `expect` requires exact counts, and
every request names one file. But exploration is precisely when cardinality
is the answer, not the guard, and when the caller does not know which files
to enumerate. A cross-file hunt today still pays a locate round (`rg -l`)
before the structural call — the round the tool exists to delete.

**Findings (two schema changes):**

1. **Workspace-scoped match.** `{:operation "match" :in :workspace :match
   "{:error _ :error-type _}"}` — no file enumeration. The server owns the
   file set; the hot index makes it affordable.
2. **Budget replaces guard in exploration mode.** Where the count is the
   answer, `:expect :matches` must be optional and a result budget takes
   over the refusal role: overflow refuses with per-file counts and no
   partial source, exactly like the existing 65,536-character guard. Guards
   protect known intent; budgets protect open questions. The schema needs
   both modes, explicitly named.

## Scoreboard

| Exercise | Rounds today | Rounds with atlas | Mechanism |
|---|---:|---:|---|
| E1 trace | 5–7 | 1–2 | witness path + negative space |
| E2 impact cone | 4–6 | 1 | reverse index + test edges |
| E3 broadcast rename | 4–6 | 2 | match → manifest skeleton → apply |
| E4 keyword flow | 3–5 | 1 | kondo keyword records, role-typed |
| E5 duplication check | — | — | doctrine, not a call |
| E6 candidate proof | 2–3 | 1 | queryable future + relationship delta |
| E7 open hunt | 3–5 | 1 | workspace match + budget mode |

Aggregate: roughly 21–32 caller rounds collapse to 7–8 across the six
callable exercises — the 3x claim, reproduced on paper against real code.

## The sibling audit re-scored the exercises

After these exercises were written, a scan of `../` sibling repositories
found the semantic layer already installed: clojure-lsp 2026.02.20 with a
persistent workspace index, cclsp wrapping it as an MCP server
(`find_references`, `prepare_call_hierarchy`, `find_workspace_symbols`,
`rename_symbol` with dry-run), and Mothership's `app/analysis.clj` already
projecting kondo records into caller/callee summaries. A live
`clojure-lsp references` call on E2's exact target returned all four sites —
including the test reference — in one answer (behind ~9 s of cold JVM start
that a persistent bridge eliminates). Re-scoring each exercise against what
must be *built* versus *bridged*:

| Exercise | Semantic fact | Who owns it now | What clj-surgeon still adds |
|---|---|---|---|
| E1 trace | call hierarchy | clojure-lsp | path composition, negative space, hash anchoring |
| E2 impact cone | references incl. tests | clojure-lsp (proven live) | one-call batching, snapshot binding, structural nodes |
| E3 broadcast rename | resolved references | clojure-lsp | the model-authored structural intent, guards, atomic commit, receipt, and undo |
| E4 keyword flow | keyword records | clojure-lsp/kondo | role typing, producer/consumer split, budgets |
| E5 duplication check | — | resolved: three graph engines already exist on this machine; build a fourth and the exercise fails itself | — |
| E6 candidate proof | none | — | fully clj-surgeon: LSP has no immutable-snapshot or candidate-future concept |
| E7 open hunt | none | — | fully clj-surgeon: no LSP offers structural form-pattern queries |

The pattern in the last column is the product thesis sharpened: E6 and E7 —
snapshots, candidate futures, structural match, budgets, transactions — have
no owner anywhere in the ecosystem. Everything else is a bridge.

The rewrite boundary is narrower than the capability inventory suggests.
The studied sessions used cclsp for navigation but never used its rewrite
tools. E3 can rent resolved references as evidence about scope. The model
still states the replacement, and clj-surgeon compiles and applies the
transaction. Importing an LSP-generated WorkspaceEdit remains a field-gated
hypothesis, not part of this plan.

## What the exercises changed

Paper exercises that change nothing are ceremony. These changed five things:

1. Witness paths to effects need a declared **effect-sink allowlist** with
   its own authority tag (E1); inferred purity stays forbidden.
2. **Test-reachability edges move up** to build step 2 — the sibling audit
   made them free: `clojure-lsp references` already returns test-namespace
   sites in the same answer, so the bridge only tags them (E2).
3. Manifest skeletons must report **arity spread as negative space** or
   pre-proven counts silently under-count variadic call sites (E3).
4. The intra-namespace edge authority is the existing `analyze.clj` engine;
   kondo is the cross-namespace authority; the atlas **joins, never
   reimplements** (E5).
5. `inspect_clojure` needs **workspace-scoped match** and an explicit
   **exploration mode where budgets replace exact-count guards** (E7) —
   otherwise the locate round survives and open questions remain
   unaskable.
