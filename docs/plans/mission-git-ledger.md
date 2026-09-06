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
