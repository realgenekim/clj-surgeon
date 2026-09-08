# Whole namespace split

Design of record: Astra's 2026-09-07 opinion, ratified by Gene's two-batch build
brief. This implements a single-source partition compiler, not a sequence of
extractions. The model owns the mapping and policies; the compiler owns their
mechanical closure over one captured source map. Native editing remains the default.

Batch 1 repairs workspace-relative namespace identity and the helper planner's
caller classification handoff (Andon inb-731473). Batch 2 exposes a pure mapping
to relations to future-source compiler through MCP and CLI, sharing the existing
transaction, proof and guarded inverse. Analysis is its nonmutating projection.

Inputs identify workspace, source file/lib, destination file/lib/forms/alias policy,
promotion authorization, retirement, bounded roots, constraints and verification.
The final graph is built simultaneously. Unmapped/duplicate owners, cycles, drift,
undecided promotions and namespace/path disagreement refuse before publishing.
Declarations are forward-reference evidence, not duplicate definition identities.

Witness matrix: nested src ancestor and configured roots; English-word docstring
bystander; three-way split, forward reference, private crossing, alias collision,
test caller; each typed refusal leaves bytes unchanged; failed publish/proof restores
the snapshot using existing rollback. Real acceptance uses the 141-owner views
fixture and its four independent oracles. Receipts distinguish write proof from
executed behavioral checks and report unknown coverage honestly.

Non-goals: architectural advice, new evaluator/reference engine/transaction system,
site-table inputs, per-destination transactions, dynamic caller guarantees, batteries,
or performance superiority inferred from compiler time alone. Gates and measured
wall are recorded in /var/tmp/forge/plan2/split-build-report.md.

The implemented leaf is [namespace-split-design](../intent/helper-extraction/namespace-split-design.md),
with NS-SPLIT-001..015 and direct @spec witnesses. The CLI and MCP share the pure
compiler and existing extraction inverse; the common synchronous proof code was
moved into dependency-light leaves so Babashka does not load the MCP/nREPL facade.

## Cell C paper-cut round

Gene's 2026-09-07 brief authorizes this leaf's repair and fresh-fixture proof.
The append-only `namespace-split-papercuts.edn` registry fixes NS-SPLIT-016..021
with misreadings and boundary cases before implementation. Add failing witnesses
first, then repair the pure compiler and confined boundary. Caller headers use
source splices. The original destination doc-token preservation policy is
superseded by NS-SPLIT-022 below. Prose is scanned
structurally in candidate strings/comments using the original namespace aliases.
Lint compares the same isolated analyzer configuration on captured and candidate
bytes; a multiset of error type/message identities ignores moved coordinates but
never lets a removed different error cancel a new one. Keep summaries separate
from newly introduced errors. Candidate parse and lint precede publication.
The fixture explicitly orders its independent owner oracle before kaocha; the
compiler must preserve arbitrary profile order. Every successful proof still runs
all configured commands, and a failing early command exercises the guarded inverse.
Acceptance: fresh d9205abc worktree, exact supplied manifest, all four oracles,
server require diff, branch suite and touched-file lint. Retain red/green and wall
receipts; this repair run does not establish a new comparative speed claim.

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

## Round 3 warm feedback (authorized 2026-09-08)

NS-SPLIT-028 refines caller layout: replace the retired entry in place with the
sorted new block, even in an unsorted host group. NS-SPLIT-029 adds an optional
warm probe before cold verification. Discovery reads only the confined workspace
.nrepl-port and evaluates its cwd with a short timeout; absent/stale/wrong-workspace
ports are unavailable. Never start a JVM. Reload destinations and rewritten callers
in dependency order, then test namespaces in the captured tree that require them
(transitively), plus conventional -test peers. Only these namespaces are explicitly
reloaded. No reload-all, remove-ns, ns-unmap or mutation of unrelated runtime state.
Failed reload/tests roll back disk immediately and skip cold commands. JVM side
effects and stale Vars cannot be undone or proved absent by a probe.

NS-SPLIT-030: profile :proof defaults to :cold; :warm requires a successful live
probe, skips configured cold commands and returns committed-probe-only,
verification_complete false, and proof_pending naming every skipped command.
No live probe with :warm refuses before publication. Unknown proof modes refuse.
Checks report warm-probe wall, test/fail/error counts and cold command walls
separately. Real cold proof and the candidate snapshot guard remain authoritative.

Witness matrix: absent/stale/wrong cwd, green and failing reload/test, warm/cold,
profile mode validation, fail-fast command log, dependency ordering, unrelated
namespace exclusion, rollback and honest incomplete receipts. New tests run in the
owned warm development image; make test runs once at the final milestone.
