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

## B01 authorization: register round-5 debt

Register the existing structural ruling, exact in-place require witness and
full-profile registry promise as NS-SPLIT-034..036. The design and append-only
registry carry the current ruling; keep the earlier IDs and link existing code
and tests with both @spec and INTENT markers. No semantic implementation change
or fresh performance claim is part of this block. Acceptance: the current
repository audit, existing witnesses, touched lint, and
`~/bin/round /home/forge/src/clj-surgeon-split /dev/null b01 --no-build` with
`intents=ok`. Record the exact ROUND line and direct audit map in the B01 report.

## B07 partial retention and facts (authorized 2026-09-08)

Gene authorizes this complete linked-intent, fail-first build and proof cycle on
astra/namespace-split, without push. Four-hour gate: 2026-09-08T06:51:44Z through
10:51:44Z. Subject: exact Cell B 92a7ca14fd904e4d962df38614b9af3a7ef41a11,
25 of 112 owners, eight retained dependencies, three retained-private promotions,
and removal of only the offending calendar invocation in the comment block.
No caller table, generated requires, caller-authored splitter or fixture weakening.

Derive a factual plan before candidate emission. Assign unmapped owners to the
retained source; share references, alias allocation, promotions and graph proof
with full retirement. Publish all files through the existing guarded inverse.
Facts mode exposes owners, exact anchors, retained dependencies, promotions and
graph facts without candidate bytes or an emitter. Retain plan-only compatibility.

Matrix: full-retirement compatibility; subset/singleton/empty mapping; both
reference directions; mixed and load-only callers; alias collisions; authorized
and unauthorized promotions; narrow comment removal and unauthorized shapes;
cycles; stale snapshot; fact parity/emitter exclusion; atomic undo; CLI/MCP schema.
First mutation-test the oracle's uppercase unresolved defect. Negative witnesses
cover omitted retained qualification, wrongly redirected retained caller and
comment cycle. Use one warm development image, split/output oracle and warm tests
per iteration, then one cold make test and intent audit. Freeze implementation,
adapter, manifest and oracle hashes; hand off 24 sequential N/F/D runs. No timing
comparison is performed during this build. A representational gap is FAILURE.

## Rows sublime: approved Row 5 contract (2026-09-08)

The operator confirms the preflight as the spec and authorizes the complete
branch-only loop with linked-intent-testing as the substitute skill. NS-SPLIT-047
loads an explicit absolute external EDN config before injected/workspace profiles,
without creating configuration in the graded tree. The file contains the existing
`:verification-profiles {name spec}` map; the request still names `profile`.
Relative, missing, oversized, malformed and workspace-contained files refuse
before mutation, including symlinks resolving into the workspace. CLI
`:profile-file` overrides the request's nested path.

NS-SPLIT-048 derives completion from executed command evidence: a cold profile
whose only commands are `true` or `/bin/true` leaves `cold-suite` pending. Warm
mode retains all skipped cold argv as pending. Other configured commands remain
trusted operator proof; the verb cannot infer arbitrary program semantics.
NS-SPLIT-049 refuses `:commands []` as `verification-empty-profile`, before writes,
with `cold-suite` pending. It does not weaken shared helper extraction admission.

Matrix: path source/precedence/confinement/EDN bounds; CLI help/dispatch/exit;
true-only cold success, failed or missing command evidence, ordinary cold gate,
warm pending; empty profile refusal and unchanged bytes. Each fix has a red
witness, warm tests and the papercut oracle; one final cold make test and intent
audit. Cell C proof tiers and Row 2 telemetry are separately recorded slices.

The real true-process and external-profile publication witnesses run in the
existing receipt-artifacts battery namespace. Pure/path witnesses remain fast;
the corpus counts both without loss. Direct warm run-tests does not enforce the
cold runner's process-spawn isolation, so the cold failure and repaired lane
allocation are retained in the report.

## Rows sublime batch 3: independently closed proof and finished-work facts

Gene authorizes this complete red-first branch build with linked-intent-testing.
NS-SPLIT-050..054 preserve the original pending receipt forever. The verb starts
a detached Babashka worker with fixed argv and file redirects, anchored to its own
classpath, then seals a job after recording the worker pid in the original receipt.
This uses the existing confined capture and synchronous argv runner, avoiding a
second snapshot or subprocess implementation. A separate process survives caller
exit; no background JVM thread or shared service owns completion.

The worker runs pending commands in order, stops on failure, hashes the committed
source inventory before and after each command, and atomically writes id-closure.edn.
It records command argv, exit, elapsed wall, real timestamps, candidate hashes and
the original receipt hash. Drift refuses closure as stale. No late rollback touches
caller edits. CLI :op :proof-status :receipt ORIGINAL reads both receipts and
current inventory: pending, complete, failed (named command), or stale.

Successful full and retained-source receipts include destination owners and counts
of rewritten static sites per caller, retained vars and unexpected workspace paths.
Review facts are encoded at the receipt boundary, with a finite receipt ceiling;
overflow refuses before publication instead of silently dropping owners. Full raw
facts stay in details. A committed-request index resolves facts-only against that
immutable snapshot; drift names the closure path in a typed refusal. A printed
manifest includes verification and roundtrips through plan-only on the input tree.

Witness matrix: all-zero / exit-one / launch failure / timeout / missing closure /
new or edited caller / receipt tamper / immutable original; full and partial facts;
forged caller newlines and bidi; ceiling overflow; exact printed manifest replay.
The owned image runs affected tests and the papercut oracle after each step; one
final cold make test, serialized lint, intent audit, and one split/base2 background
timing run finish the build. Single-run wall is mechanism evidence, not superiority.

Sol's landing fence adds NS-SPLIT-055..058: closure pid-reuse/replay identity,
mandatory `/var/tmp` worker temp admission, independent EDN/escaped-JSON receipt
bounds, and snapshot-bound printed-manifest tamper refusal. Each failed probe earns
a focused witness before its repair; no full-suite rerun is part of this fence.

## Comment identity diff repair (2026-09-08)

The authorized branch-only repair strengthens NS-SPLIT-060. Row5-adopt-2
(e1de51d1) observes positional pairing in retained exports.clj (Cell B): the
comment originally at 1822 survives at 1465, yet the receipt calls it deleted.
Full Cell C is the independent whole-partition acceptance fixture.

Before comparing replacement hunks, match exact comment content within its
original file and named owning form, mapping moved owners to their assigned
destination. File-local unowned comment lines use surviving content anchors.
A location shift alone is omitted. A comment carried to another file by its
owner is a moved fact. Only unmatched lines within the same anchored hunk can
be changed facts; additions and removals remain explicit. A removed duplicate
occurrence with surviving content must say so, never claim content deletion.

The grouped :comment_edits vector carries :file, :after_file and :edits.
Each edit group names :owner and its :changes. Unchanged moved occurrences share
:moved {:from [original-lines] :to [candidate-lines]}, retaining every location
without echoing text. Consecutive locations may use inclusive [start end]
ranges. :changed {:before texts :after texts} carries corresponding text vectors
:line/:after_line, or :moved locations when relocated. Indentation-only after text may
use [:indent N] under the declared lossless encoding; a before-text [N text]
expands to N spaces followed by text. The projection has independent roundtrip
witnesses, including range cardinality and both receipt encodings. :deleted carries
:line, :added carries :after_line, and :removed_occurrence carries :line.
Existing :comment_policy and lossless string encoding apply. No meter is added.

Matrix: identical and line-shifted files; exact 1822/1465 survivor with prior
removal; moved owner; policy rewrite; insertion/deletion separated by surviving
anchors; repeated text; namesakes; reordered comments; hostile string encoding;
full Cell C receipt under both 65,536-byte ceilings. Pure tests precede code;
then affected warm tests, Cell C papercut oracle, one affected cold gate.
