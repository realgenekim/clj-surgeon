# Saved owner-forms mission to exact Git commit

Public handler: `mission-git-ledger/commit!` accepts exactly `:id`, `:workspace`
and optional `:state-home`. Proof, source, profile or receipt overrides refuse.
The first supported verb is `owner_forms`; other verbs refuse explicitly.
The public `bin/mission commit` boundary retains the explicit-staging and
hook-skipping disclosures in `mission-git-receipt.md`.

The handler uses the existing mission directory/id APIs but reads bounded EDN
bytes itself: `mission/read-mission` currently uses an unbounded slurp. It never
evaluates reader syntax. It requires a verified saved state; a successful,
committed, verification-complete terminal receipt; and exactly one compiled
candidate whose gate and independent acceptance both succeeded with unchanged
proof inputs. Every saved proof result must be finished with exit zero, and the
number of results must equal the number of frozen command vectors. Proof ids
and evidence bind to saved plan authorities; identical command vectors refuse.

The guarded inverse path must agree between saved undo and terminal receipt.
Its stored canonical receipt hash must agree with both references and a fresh
hash of the decoded inverse. Each changed file's source/result hashes must match
its embedded bytes, the saved plan's absolute source mapping and the mission
snapshot. The inverse must describe regular modifications without creations or
deletions, and no file outside the saved owner set. These records normalize into
the existing explicit-stage Git seam; caller-supplied proof is never substituted.

At every Git observation, the callback rehashes both the saved mission EDN and
the inverse artifact. A changed/deleted/symlinked artifact refuses. This detects
staleness, not malicious rewriting of an entire ledger and every related hash
by a principal already able to modify the repository. External ledger writers
do not yet share the Git lock; that narrow concurrency limit remains explicit.

Validation: pure malformed ledger/receipt/proof/source matrices; a saved synthetic
mission created through the real `cli/propose!` and `cli/apply!`; real independent
gate and acceptance subprocesses; only provider transport replaced by a frozen
candidate fixture. Stage exact resulting source, invoke the public handler,
then inspect the Git tree/body and source. Wrong state, stale inverse, partial
proof and caller override refuse without ref advancement. No live provider or
user mission is touched. JVM validation runs under suite-run with Xmx512,
ActiveProcessorCount2 and a fixture-specific telemetry path.

Validation completed: the isolated real saved-ledger roundtrip is green, with
3 tests / 27 assertions in `mission-git-ledger-test` (:battery). It uses actual
saved plan/receipt/undo artifacts and actual independent proof commands from the
existing executor fixture. Seven modified saved-state/proof/snapshot variants
refuse in pure normalization; a tampered on-disk inverse refuses without changing
HEAD; successful handler publication preserves live bytes and commits exactly
the mission source path. No user mission or provider was invoked. Public CLI verification is recorded
below; the integrating branch owns test manifest enrollment.

Combined isolated JVM verification of all five Git seam namespaces: 18 tests /
112 assertions green; formatter and lint clean. Namespace enrollment counts are
mission-git-test :fast 4, mission-git-boundary-test :battery 4,
mission-git-fence-test :battery 5, mission-git-process-test :battery 2, and
mission-git-ledger-test :battery 3.

## Public CLI boundary

`bin/mission commit M-ID --workspace R [--state-home H]` passes exactly these
three fields to the saved-ledger adapter. A pure option gate rejects missing
values, extra positional arguments and every spec/proof/config override before
reading a spec file or touching Git. Ordinary mission verbs retain their paths.
Help and the receipt explicitly distinguish verified source mutation from Git
ref publication: no staging, source write, push, Git hooks or signing. Users
stage precisely the verified changed files first. Ref-update uncertainty remains
`:git-ref-updated :unknown`; callers must inspect Git before retrying.
Verification includes closed pure parser/dispatch witnesses, supported help,
nonzero typed refusals without eager spec reads, and a real public CLI roundtrip
from synthetic saved proof through explicit staging to verified Git tree/body.

Public wiring verification: six Git namespaces pass 22 tests / 156 assertions,
including four new CLI witnesses. Existing display/run/fallback launcher
regressions pass 22 tests / 183 assertions. The actual successful subprocess
uses global options before `commit`; the same fixture first refuses without
staging. Git tree, provenance body, HEAD, skipped hooks/signing, unchanged
source and unchanged ledger are asserted. Events use a scratch override inherited
by subprocesses. No user mission or provider call occurred. CLI/source lint
has zero errors and one pre-existing redundant-let warning, reproduced on the
base revision; shell syntax and diff whitespace checks pass.

Enrollment delta for the whole imported seam plus CLI: one fast namespace
(four tests), five battery namespaces (18 tests), 22 adopted tests total. The
new CLI namespace alone contributes one battery namespace and four tests.

## Publication/undo exclusion and recovery

Git publication and source undo share a bounded per-mission file lock. Before
entering the Git boundary, publication writes a bounded sidecar intent next to
the saved mission, binding canonical workspace, mission id and pre-publication
ledger SHA-256. The file is forced and atomically replaced; the parent directory
is forced too. These are the actual persistence operations, not a stronger
promise about power loss or storage hardware. The sidecar survives an interrupted
or stale ledger writer and is authoritative for refusing silent source undo.

Successful publication records the observed commit oid/tree/parent in the sidecar
and the mission's `:git-publication` field, without changing proof state. Final
metadata failure reports Git's actual outcome, including `:git-ref-updated true`
and oid/tree when known, plus metadata-recorded=false. Pending, malformed,
mismatched or uncertain publication records block undo with a typed reason and
an instruction to inspect Git and both records; no automatic Git inverse or new
recovery command is introduced. A known possible commit is retained on uncertain
outcomes. Only a positively identified pre-ref refusal permits removing the
intent. Generic boundary failure is not sufficient evidence of no publication.

Undo checks publication while holding the same lock, before loading/inverting
source. Resume already delegates verified missions to undo and inherits this
refusal. The mission serializer and bounded show preserve the publication record.
Tests execute a real synthetic saved mission through public publication then
public undo refusal, verify unchanged source/ref, and inject pending, mismatched,
uncertain and final-metadata-failure cases. No user mission/provider is invoked.

Publication/undo validation: the combined Git seam, public CLI, display and legacy
mission gate passes 67 tests / 628 assertions. The final publication namespace
passes 7 tests / 87 assertions after adding the independent review's teardown
counterexample: both a thrown boundary and a generic false result remain unknown
and retain the recovery record. Actual public undo/resume refuse after a real
synthetic publication with stable source/ref. Tests cover throwing and silently
lost ledger writes, final sidecar failure, pending BB show without mutation
advice, no Git queries from show, malformed/bound-marker rejection and immediate
lock contention. No live provider or user mission was involved. Events were
redirected to an isolated scratch ledger throughout. The integrating branch must
enroll one battery namespace, `mission-publication-test`, with seven tests.
