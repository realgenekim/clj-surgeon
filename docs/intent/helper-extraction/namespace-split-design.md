# Whole namespace partition compiler

Status: branch feasibility slice, 2026-09-07. Gene authorized both build batches
against Astra's design of record. This leaf implements
[the whole-split plan](../../plans/namespace-split.md); its behavioral contract is
[NS-SPLIT-001 through NS-SPLIT-015](namespace-split-specs.md). It does not change
ordinary editing routes or claim a measured native crossover.

## Intent and ownership

A model supplies one complete assignment of definition names to destination
libraries. The compiler closes that assignment over one captured source map. It
never chooses the decomposition, asks for site tables, or calls extraction once
per destination. Source deletion, all creations, and caller changes form one
future file map consumed by the existing extraction transaction and inverse.

`namespace-split/compile-split` is pure: request plus captured bytes, configured
source roots and clj-kondo facts produce relations, grouped blockers, and future
sources. `namespace-split-io/execute!` owns confinement, captured reference analysis and candidate lint,
profile admission, guarded publication and receipt persistence. MCP and CLI call
this same boundary. Plan-only calls the same compiler and returns its projection;
it skips publication and verification execution. No nREPL dependency is required
by the CLI. Development may reload these namespaces through an owned nREPL.

## Request contract

The schema in `namespace-split-io/schema` is the executable authority. Every
object is closed (`additionalProperties: false`). Both transports accept:

```clojure
{:workspace_root "/work/project"
 :source {:file "src/app/views.clj" :lib "app.views"}
 :destinations [{:lib "app.format" :file "src/app/format.clj"
                 :forms ["fmt-date"] :alias_policy ["fmt" "vfmt"]
                 :doc "Date formatting helpers."}
                {:lib "app.page" :file "src/app/page.clj"
                 :forms ["page"] :alias_policy ["page" "vpage"]}]
 :promotion_policy "promote-required" ; alternatively ["fmt-date"]
 :source_retirement "delete"           ; alternatively "retain-empty"
 :roots ["src" "test"]
 :verification {:profile "split-unit"}
 :constraints {:forbidden_edges [["app.format" "app.page"]]}
 :expect {:destinations 2 :forms 2}
 :plan_only true}
```

`expect` supports files, forms, destinations, caller_files and caller_sites; each
supplied count must match. `snapshot_hash` optionally binds a reviewed analysis to
the next call. `plan_only`, `constraints`, `expect` and `snapshot_hash` are optional.
The remaining fields are required. Names and aliases are strings in both EDN and
JSON. An authorized named promotion set never authorizes unrelated promotions;
only necessary ordinary cross-boundary accesses become public. A quoted private
Var remains private when that is its only crossing access.

Library identity is supplied by the caller and checked against deps.edn `:paths`
anchored at the workspace root. Discovery roots are literal relative directories,
not globs. A source must be inside the captured roots. Destination confinement is
checked before publish; the destination must be absent. Callers outside the
bounded roots are outside the claim. Destinations must also be inside those roots.
Limits: 32 roots, 4,000 Clojure source files,
16 MiB total, 2 MiB per file, 50,000 directory entries per root, 1,000 destinations.
Symlink source roots/files and destination traversal refuse.

## Compiler phases

1. Parse exact captured bytes into top-level definition owners and spans. A
   declaration supplies forward-reference evidence, not a competing definition.
   Account for every source owner exactly once and gather independent mapping,
   identity, promotion and guard blockers.
2. Resolve references once using the repository's serialized `~/bin/clj-kondo`.
   Its input is an isolated mirror of the captured bytes. Reuse the existing
   structural quoted-Var reader for quoted references that kondo omits. Local
   bindings are not Var edges. Unresolved old-namespace qualified references are
   explicit unknowns rather than disappearing with a removed require.
3. Project every owner to its destination simultaneously. Derive required private
   promotions, external requires and imports, destination dependencies, aliases,
   and forward declarations. Alias choices follow supplied order against existing
   external bindings. Preserve body text and attached comments, changing only
   reference tokens, necessary private metadata, and matching call-continuation alignment. Generated namespace headers
   are deliberately owned text; caller headers preserve unrelated original nodes.
4. Rewrite all bounded callers, including test roots, in the same future map.
   A caller whose only relation is loading the old namespace loads the partition.
   Construct the final namespace graph using generated headers and unchanged
   captured namespaces; report SCCs and forbidden-edge witnesses.
5. Return the future map and its analysis projection. Blockers prevent any write.
   Plan-only reports edge witnesses, promotions with callers, external libspecs
   and imports, forward references, callers, missing/duplicate owners, SCCs,
   architectural violations, snapshot/map identity, and unknown coverage.

## Publication, proof and inverse

The existing `mcp-extraction/commit!` now supports explicit deleted files in the
same future map as creations/edits. Nil future bytes mean deletion only when the
file is also in `deleted-files`. It guards the full captured snapshot, checks absent
destinations, publishes through existing atomic per-file writing, reads back the
whole changed set, and rolls back on failure. Source permissions survive deletion
rollback/undo. Receipts carry before/after hashes and integrity-checked inverses.

`verification-process` and `synchronous-verification` are dependency-light leaves
extracted from the existing change-buffer/helper proof code. Existing entrances
delegate to them; there is one subprocess/proof implementation. Named profiles
come from `.clj-surgeon.edn` `:verification-profiles`, for example:

```clojure
{:verification-profiles
 {"split-unit" {:commands [["bin/kaocha" "unit" "--fail-fast"]]}}}
```

Relative executable paths are anchored at the workspace root. Profiles must be
synchronous and runnable before writing. After commit/read-back, all configured
commands run against the candidate. A final captured-byte guard binds their result
to the candidate. Inventory guards before publication and after proof detect newly
arrived source files without re-running reference analysis. Failed proof invokes
the same guarded inverse. Concurrent foreign
edits are never blindly overwritten; recovery-required is distinct from rollback.
On recovery-required receipts, source_retired reflects whether the source is
actually absent; it does not inherit the pre-mutation default.
This is failure atomicity, not isolation from concurrent filesystem readers or
power-loss durability. Commands execute after candidate bytes become visible.

Success leads with committed and returns counts, correct destination libraries,
actual promotions with reasons, graph result/unknown count, named checks with exits
and durations, verification_complete, details and inverse paths. The compact graph
summary links to full projection details. Analysis exit 2/3 can still yield complete
reference facts; it is labelled pre-mutation baseline lint, never a clean lint pass.
Before publication, the candidate is parsed and linted in the same isolated
analyzer environment. Error type/message multisets ignore moved coordinates;
new error identities or increased multiplicity block publication even when total
errors are unchanged. Warnings and info deltas remain visible and advisory. Verification completeness means the named checks passed over the guarded
candidate, not that arbitrary runtime behavior or external callers were proved.

## Entrances and continuation

MCP full profile registers `namespace_split`. Its text includes the complete JSON
structured receipt. The edit-only catalog is unchanged. CLI uses the same EDN:

```sh
clj-surgeon :op :split-ns! :request-file split.edn :plan-only true
clj-surgeon :op :split-ns! :request-file split.edn
clj-surgeon :op :undo-extract! :receipt /path/from/undo_receipt.edn
```

CLI also accepts `:request '{...}'` or direct request keys. A blocker returns a
nonzero CLI exit. Mutation refusal includes a plan-only next_call for grouped
analysis; it does not guess missing decisions. The Batch 1 kernel continuation is
an inspect_clojure plan-extraction request using workspace-relative paths; the
helper boundary preserves it and its candidate list.

## Supported boundary and acceptance

The source partition currently admits `.clj` def/defn/defn- owners. Unowned
source-level effects, unknown ownership, syntax quotes and namespace-relative
keywords refuse. Reader/macro/dynamic resolution beyond static facts is not a
claim of this slice. No new evaluator, site-table language, multi-source merge,
architecture recommender, per-destination transaction, or obligatory inspect pass
was built. An independent fresh-caller comparison against the same prose plan is
still required before claiming a decisive native crossover or installing routing.

Direct witnesses cover three destinations, cross dependencies, a forward reference,
a private promotion, external alias collision, test callers, quoted Vars, source
trivia, load-only callers, grouped refusals, publication failures, proof failure,
drift during proof and guarded undo. The copied d9205abc views fixture supplies
application acceptance: 141 definitions, 20 destinations, five callers, 87 sites;
unit suite including the supplied architecture test, source absence and exact
owner assignment all pass. Complete receipts, outputs, timings and gate results
are in `/var/tmp/forge/plan2/split-build-report.md`.


## Cell C receipt and source preservation amendments

[NS-SPLIT-016..021](namespace-split-papercuts.edn) register the 2026-09-07
paper cuts with INTENT/INTENT-TEST witnesses and a bidirectional suite guard.
The branch feasibility claim above is historical; Cell C D1/D2 supplied the
field failures motivating this amendment, not evidence of universal speedup.

Caller requires are inserted at their lexical library position using the block's
existing indentation. Only retired libspec spans/lines and new insertion spans
are owned; existing entries and comments are never reordered or reprinted.
An already unsorted block remains otherwise unchanged. The round-1 doc-token
cloning policy is superseded by NS-SPLIT-022 below; original source prose is no
longer assigned to destinations.

The receipt's `:prose_mentions [{:file :line :text}]` names surviving candidate
lines in strings and comments. It recognizes the retired library, original local
aliases and the library's short name (for prose in files with no direct require).
These rows are advisory, not semantic reference claims or blockers; source prose
is left intact. Lines refer to candidate bytes. A short-name hit can be ambiguous.

Executed checks name the actual program (`kaocha unit`, `cc-oracle.py`) and retain
profile, argv, exit and measured wall separately. The boundary preserves profile
order and the shared runner stops at the first failure. The Cell C acceptance
profile explicitly places its read-only owner oracle before kaocha; arbitrary
profiles are not reordered. Candidate parse/lint run before publication, and a
post-publication oracle failure invokes the existing guarded inverse. Success
still requires all configured checks plus the final snapshot guard.

## Round 2 destination ownership and layout

Gene authorizes the complete repair/proof cycle in this leaf. NS-SPLIT-022..027
supersede NS-SPLIT-019: the original docstring goes into none of the destinations.
Optional destination :doc is a nonblank string; absent it, a deterministic summary
names the source, form count and first three public names in source order (including
promotions). Private-only destinations say no public forms. No clock enters the
pure compiler. Explicit docs preserve their values as valid Clojure string literals.

Imports use located kondo java-class-usages excluding declarations and fully
qualified class tokens. A replaced call head with a same-line first argument
shifts only continuation whitespace equal to the original argument column inside
that call; string contents stay intact. Ordinary and anonymous #(...) calls share
the rule, including shorter aliases and nested heads on the same line. Destination requires use one sorted block
at the source continuation indent (default three spaces). Caller entries retain
existing groups and trivia; additions join the matching library-prefix group.
The same rule applies to source and test callers.

The receipt's :unrequired_qualified_refs rows contain candidate :file, :line,
:token and :lib. Java classes, aliases, required namespaces and prose are excluded.
These rows neither add requires nor block writes. Captured facts are reused with
no extra analyzer or suite invocation. The registry records the behavioral matrix
before code. Fresh d9205abc before/after fixtures use the supplied request and same
profile. Gates: all four oracles, baseline-relative view lint, make test and touched
file lint. Single replay timing does not establish a new native crossover.
