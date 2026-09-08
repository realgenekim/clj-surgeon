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
qualified class tokens. A replaced call head shifts every subsequent line inside its call span by the
head's width delta, regardless of indentation or nesting depth; multiline string
contents stay byte-identical. Ordinary and anonymous #(...) calls share the rule.
Each changed enclosing head contributes its own delta once on shared lines.
Destination requires use one sorted block
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

## Round 3: exact caller position and optional warm probe

NS-SPLIT-028 supersedes the lexical-position portion of NS-SPLIT-017/026 when
there is a retired require: its exact span is replaced with the sorted destination
block. Existing neighboring libspecs and trivia stay byte-identical. Without a
retired entry, additions use the existing lexical/group insertion policy.

Before verification, the boundary probes a confined `.nrepl-port` with a 300 ms
cwd evaluation. Missing, malformed, stale and foreign-workspace ports are ignored
for cold profiles. Discovery never starts a JVM. A discovered JVM receives only
explicit `require :reload` for destinations, rewritten callers and affected test
namespaces, ordered through the final dependency graph. Test selection includes
captured `_test.clj` namespaces depending transitively on touched code and existing
conventional `-test` peers. It runs `clojure.test/run-tests` for that set. Untouched
intermediate dependencies influence order but are not explicitly reloaded.

The profile in `.clj-surgeon.edn` selects `:proof :cold` (default) or `:proof :warm`:

```clojure
{:verification-profiles
 {"split-unit" {:proof :cold
                :commands [["bin/kaocha" "unit" "--fail-fast"]]}
  "split-probe" {:proof :warm
                 :commands [["bin/kaocha" "unit" "--fail-fast"]]}}}
```

A failed warm reload/test or bounded evaluation rolls back disk immediately and
runs no cold commands. The `warm-probe` check carries `duration_ms`, failures,
errors, summary and explicit reload/test names. Cold mode then executes the named
profile exactly as before; individual cold command walls are separate check rows.
Warm mode requires a live successful probe, skips that profile's cold command list,
and returns `state "committed-probe-only"`, `verification_complete false` and
`proof_pending` listing every skipped command. An unavailable probe refuses warm
mode before publication; invalid proof modes refuse. Snapshot guards still apply.

A probe cannot clear stale Vars, cached state, macro expansion, classpath/startup
or dynamic callers. It is deliberately incomplete evidence. No `remove-ns`,
`ns-unmap`, `reload-all`, runtime restart or unrelated explicit reload is performed.
Rollback restores disk; effects of evaluation in the caller's JVM are not reversible.
The full cold profile and repository suite remain required final proof.

## Round 4 structural continuation ownership

Gene authorizes the complete fail-first, repair and image-loop proof cycle for
Sol's two blocking findings on 5b78bed2, starting at round 3 tip 38ecea11.
NS-SPLIT-032 follows Fable's 2026-09-08 round-5 ruling, superseding the
round-4 column match and nested-body protection. Every line after the head line
inside the changed call's structural span shifts by the head's width delta,
including nested bodies, comments, standalone closing delimiters and later outer
siblings. A multiline string's opening line may move; subsequent lines inside
its literal never move. Lines outside the call stay byte-identical. No first-argument
column test defines ownership; a first argument on a later line also moves.

Round-5 witnesses use Sol's unindented literals from the review of 14c0501f:
call at column zero, growth v/x to longer/x and shrinkage views/x to v/x,
nested do, body, comment, standalone close, multiline string and outer sibling.
Both direct alignment and compile-split must fail first on that reviewed tip.
The warm image is retained across every split/oracle/test iteration; one final
cold make test provides the repository proof. This is a correctness repair,
with no comparative performance claim.

NS-SPLIT-033 pins the original d9205abc forms/polish headers. Replacement entries
are sorted only among themselves and spliced into the retired entry's span;
unrelated host entries and trivia retain their order and bytes. Round 3 already
implements this rule; the exact field inputs must fail against Sol's reviewed
commit and pass at the starting tip. The registry carries requirements and direct
witnesses. Each iteration runs warm tests, split-in-image and the current output
oracle. Final acceptance uses the real profile, four fixture oracles, current-trunk
manifest census, touched-file lint and one cold make test.

## B01: round-5 promises remain traceable

Gene's B01 authorization registers existing behavior, without another compiler
change. NS-SPLIT-034 makes the structural-realignment ruling explicit: every
line owned by the changed call form shifts by the head delta; ownership follows
paren structure, including nested bodies, comments and closing delimiters, not
indentation. Multiline string contents never move. The head line is rewritten
at its token; subsequent owned lines accumulate each enclosing changed head's
delta once. The literal's opening-line indentation may move without changing
the string value. Outside-call bytes are protected.

NS-SPLIT-035 retains the exact d9205abc forms/polish in-place require pin.
NS-SPLIT-036 links the existing full-profile registry list to its public catalog
implementation and battery witness. The append-only registry supersedes 032/033
with 034/035; original IDs and witnesses remain resolvable. The repository census
grows from 190 to 193 non-MCP IDs. The paper-cut contract scans the catalog and
its existing admit-test witness so registration cannot fall outside that guard.

This is retrospective traceability, not a claim that B01 wrote new behavioral
tests or repeated round 5's red phase. Provenance: 5cb03422 (alignment and
16 recorded red assertions), the retained require witness, and 1f0646e9 (registry
list repair). B01 acceptance is the current audit, existing witnesses, touched
lint and the explicitly requested no-build round; apparatus certification
remains a separate B02 obligation.

## B07: retained source and read-only facts

The B07 build brief authorizes this leaf's full design-to-verification cascade.
`:source {:file ... :lib ... :retain true}` selects partial retention; omit
`:source_retirement` in this mode. Conflicting retirement instructions refuse.
Optional source `:alias_policy` defaults to the final library segment followed by
`source`; the same collision rules apply. Unmapped owners stay in their original
byte positions. The destination manifest remains the sole owner assignment input.
`:promotion_policy` authorizes required promotions in either retained or moved
owners; unrelated private definitions stay private. Mixed callers keep their
source require and retained references. Load-only source dependencies stay put.

Optional source `:comment_policy "remove-moved-invocations"` authorizes removal
of moved call expressions inside top-level comment forms, retaining surrounding
comment contents. A print/println/prn wrapper containing only the offending call
is removed with it. Other top-level source forms remain unsupported. Comment
references without this authorization participate in the graph and can refuse
for cycles. No arbitrary source text or caller-site table is accepted.

The compiler first prepares source owners, located references, assignments,
required aliases/imports, promotions and the final graph without generating
candidate bytes. Full execution emits from that prepared plan. CLI `:facts-only
true` and MCP `:plan_only "facts"` return the same plan's public `:facts` map.
The map includes snapshot hash, owners (original and assigned libs, exact spans),
references (file/line/col and disposition), retained dependency names, promotions,
namespace graph and unknown coverage. It contains no generated requires, candidate
source, replacement text or emitter. The full run persists identical facts in its
details receipt. Existing boolean plan-only retains its broader projection.

NS-SPLIT-037..043 register this matrix before implementation. The Cell B oracle
is copied without weakening its acceptance checks, and A8 is repaired to match
uppercase/lowercase unresolved diagnostics. Its failing witness and three
negative mutations are preserved with the build receipts. The supplied fixture
manifest and architecture are immutable; a failed gate records its exact gap.

NS-SPLIT-044 separates the 16 MiB captured source cap from a finite 64 MiB
analyzer-output cap. Cell B's 472-file evidence exceeds source byte size. The
boundary still refuses truncation before reading EDN; the captured universe is
unchanged. Unused keyword analysis is not requested.

NS-SPLIT-045 removes only header dependencies made unused by this extraction:
a caller's old source alias when it has moved usages and no retained usage, and
a source short-class import used by moved owners but no retained owner/comment.
Mixed and explicit load-only source dependencies remain. Facts also expose
located external Var and class usages consumed by destination header planning;
these are captured identities, never generated libspecs or replacement bytes.

Partial mode refuses an existing `declare` of a moved owner with
`:retained-declaration-of-moved-owner`; it never silently leaves a stale source
intern. Retained declarations of unmoved owners remain unchanged. The Cell B
source has no declarations, so this conservative boundary does not alter it.

NS-SPLIT-046 supersedes absolute A8 (042): a fresh archive of the frozen base is
linted with the same analyzer and project configuration as the candidate.
Compare file/severity/exact-message multisets, ignoring only source coordinates.
Any new identity or increased multiplicity fails, in all letter cases. Both
analyzers must finish with recognized statuses and nonempty output. Baseline
findings remain explicit, not silently waived. The original case-corrected
absolute failure and successful rollback remain in the B07 engineering receipt.
This completes the authorized oracle repair without changing the exact fixture
or its protected footprint. All arms receive the same oracle before timing.

## External proof and truthful completion

An explicit `verification.profile-file` is an absolute path outside the canonical
workspace to a bounded EDN configuration containing `:verification-profiles`.
It overrides injected/workspace configuration; it is read without adding any
workspace file. `verification.profile` still selects the named profile. CLI
`:profile-file` sets the same nested field. Invalid paths or data refuse typed.
Executed commands remain named in checks. A successful cold true-only profile
commits with `verification_complete=false`, `proof_pending=["cold-suite"]`.
Empty commands refuse `verification-empty-profile` without mutation. Warm mode
continues to list skipped cold commands. This does not certify the semantics of
arbitrary operator-configured executables; those remain trusted proof inputs.

A help-only caller review requires explicit artifact and pending-proof guidance:
receipts live externally, but verification commands may write workspace logs.
Inspect workspace_status unexpected paths and retain cleanup status separately.
For cold-suite, the target Makefile/testing documentation supplies the command;
for warm mode, use every listed pending command. Keep each argv/exit/output with
receipt_hash and verify the same source hashes/inventory around those gates.
External closure is separate evidence; the original receipt remains immutable.
