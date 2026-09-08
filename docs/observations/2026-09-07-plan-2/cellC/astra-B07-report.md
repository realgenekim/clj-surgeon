# B07 — partial retention and shared facts feasibility gate

**FEASIBILITY PASS.** The shared compiler represents the exact Cell B task without
a caller-authored splitter. The hand-drive and all required checks pass with the
explicitly documented A8 baseline correction below. No cohort has run and no
performance admission is claimed.

Committed `02bbf482309834039b2dd22b92662bb0f7cb5f02` on `astra/namespace-split` as forge-anvil,
with Gene's Co-Authored-By trailer. Working tree clean; no push. Build began
2026-09-08 06:51:44 UTC and completed 2026-09-08 08:09:04 UTC:
4640.2 seconds / 1.289 engineering wall hours, including all
failed attempts and cold gates, below the four-hour 10:51:44 UTC deadline.

## Scope and representation

Exact Cell B base `92a7ca14fd904e4d962df38614b9af3a7ef41a11`, owned fixture
`/var/tmp/forge/plan2/build/cc-b07`, branch `split/b07`, created with
`~/bin/worktree-add /home/forge/src/curtaincall-cfp /var/tmp/forge/plan2/build/cc-b07 split/b07 92a7ca14fd904e4d962df38614b9af3a7ef41a11`.
The supplied manifest moves 25 of 112 owners into exports.calendar; 87 stay in
exports. It discovers 43 external sites in nine files and the one comment-block
invocation. Eight retained dependencies and exactly communicated?,
content-publishable?, speaker-names promotions are derived by the compiler.
No caller-authored splitter, caller table or generated requires were supplied.
The adapter injects only the trusted verification profile and receipt directory.
The form-count deviation, 25 > the original 16-form ceiling, remains explicit.

The original compiler now has prepare-split and emit-split phases. Retention
assigns unmapped owners to the source. Shared facts drive both qualification
directions, alias allocation, promotions, graph constraints and guarded publication.
The authorized comment policy removes the moved invocation and its sole print
wrapper while preserving surrounding comment content. Newly unused class imports
and pure-moved caller source requires are retired; mixed and load-only requires
remain. A retained declaration of a moved Var refuses instead of leaving a stub.
CLJC/CLJS, unsupported owners, dynamic references and arbitrary decomposition
remain outside the contract. No automatic routing admission is changed.

The facts entrance is CLI `:op :split-ns! :request-file X :facts-only true`, or
MCP namespace_split with `:plan_only "facts"`. It bypasses emission, publication,
warm discovery and proof-profile preflight. It returns owners, exact reference
anchors, retained/external dependencies, Java class uses, promotions and graph
facts without generated requires or candidate bytes. Existing plan_only=true
retains its broader plan behavior. Both paths use the same prepared facts.

## Linked intent and development evidence

HLD → namespace-split design/plan → NS-SPLIT EARS and append-only registry →
fail-first witnesses → implementation. The missing linked-intent-dev skill was
searched for; the available linked-intent-testing skill plus the repository intent
arrow were applied under Gene's explicit complete-build authorization.

| IDs | Behavior | Red evidence under b07/ |
|---|---|---|
| 037–041 | Retention, bidirectional edges/promotions, narrow comment policy, shared facts, CLI/MCP grammar | compiler-red.log |
| 042 (superseded by 046) | Original A8 case defect | oracle-red.log: seven failed assertions |
| 043 | Independent omitted qualification, redirected retained caller and comment-cycle mutants | negative-oracle-red.log |
| 044 | Separate bounded analyzer output budget | analysis-budget-red.log |
| 045 | Newly unused headers; complete external and Java reference facts | header-facts-red.log |
| 046 | Exact-base unresolved multiset comparison | baseline-oracle-red.log: unchanged baseline rejected before repair |

Additional retained-declare negative witness: declaration-red.log. Final green
logs and intent audit are listed below. The source capture stays capped at 16 MiB;
the independent analyzer-output cap is now 64 MiB because this exact fixture's
captured analysis is about 40 MiB. Holding prior large analysis maps in the 512 MiB
development image caused an early OOM refusal; releasing those captures resolved
it. This is retained as development friction, not hidden from a performance result.

The original Cell C full-retirement regression was compiled in the same warm image
on a fresh d9205abc worktree: 141 owners, 20 destinations, 87 sites, five callers,
26 files. `b07/cell-c-regression.log` and `b07/cell-c-papercuts.log` record success
and PAPERCUTS: 0 (existing advisories remain advisory).

## A8 repair and preserved failure

The documented case-sensitive grep defect was mutation-tested and fixed first.
The first complete hand-drive passed every check except the now case-correct
absolute A8. It correctly rolled back all 11 files, restored=true. The exact base,
independently archived and linted with the same ~/bin/clj-kondo, has the same five
unresolved findings: rubric_predicates `=>`, two protected views' clojure.string,
protected board_test's starfederation.datastar.clojure.api, and unchanged comms_test's
cfp-scheduler-killer.views.log. Four files are protected from modification.

NS-SPLIT-046 repairs this contradictory baseline by independently archiving and
analyzing the exact base for every acceptance run. A8 compares file, severity and
exact message as a multiset, ignoring coordinates only. Every new identity or
increased multiplicity fails, in every letter case; empty/failed analyzers and
malformed unresolved findings fail closed. Removing one error cannot cancel a
different new error. The five baseline findings are printed and preserved.
The fixture and all other acceptance checks are unchanged. An optional
clarification received no reply before implementation; this was treated as a
routine repair within the explicit oracle-repair authorization. This acceptance
amendment is disclosed here and in every arm's brief, before freeze/cohort.

The failed absolute run is not regraded as success: see
`b07/hand-drive-attempt-03-absolute-hand-drive-{receipt.edn,details.edn,oracle.log,log}`.
Its complete in-call wall was 267092.9908 ms; oracle 229233.760151 ms. Prior
nonmutating OOM/configuration refusals are attempts 01/02. Attempt 04 hit the REPL
client's default two-minute timeout; the transaction rolled back and the fixture
was confirmed clean before restarting with a 30-minute client limit. Its receipt
and log are retained. All are untimed engineering/feasibility work, not cohort runs.

## Final hand-drive, verification and freeze

The final guarded hand-drive is PASS, state=committed, committed=true,
verification_complete=true, proof_pending=[], source_retired=false. All 11 files
are covered by the shared inverse (source, destination and nine callers). Receipt:
`b07/receipts/73eae310-0c51-4ae4-b1d3-e97589aefd1f-undo.edn`; complete proof:
`b07/hand-drive-receipt.edn` and `b07/hand-drive-details.edn`.

| Check | Result |
|---|---|
| Captured analysis | baseline exit 3, explicit baseline map |
| Future source parse | exit 0 |
| Candidate lint delta | baseline exit 3, explicit baseline map; zero error/warning/info delta |
| Independent Cell B oracle | exit 0, all checks PASS |
| Verified snapshot guard | exit 0 |
| Facts/full parity | exact facts equality=true, snapshot equality=true |

Snapshot `74d26c1f02cd138438285ea1686a03e78c81453836ba9613c4e6b76268d38ca9`.
Facts-only boundary 25656.823013 ms; final D in-call 278818.089153 ms, including
239492.235305 ms independent oracle. These are outside-timing feasibility
observations, not matched arm estimates. Build wall is separate below.

Exact oracle lines (`b07/hand-drive-oracle.log`):

```text
PASS  A4 all 43 external references across 9 namespaces resolve to cfp-scheduler-killer.exports.calendar
PASS  A6 retained-var references unchanged (197 external refs still resolve to cfp-scheduler-killer.exports)
A8 baseline unresolved: 5; retained: 5; new/increased: 0
B07 PAPERCUTS: 0
PASS  B (destination-first) fresh process loaded clean; all 25 vars interned in the destination only
PASS  B (source-first) fresh process loaded clean; all 25 vars interned in the destination only
PASS  C focused cluster tests: 8 tests, 80 assertions, 0 failures.
PASS  C integration path (public-widgets .ics route → handler → calendar-ics-for): 28 tests, 461 assertions, 0 failures.
PASS  D make runtests-once green — 1021 tests, 12397 assertions, 0 failures.
PASS  D bin/kaocha unit --fail-fast green — 1021 tests, 12397 assertions, 0 failures.
PASS  E every file outside the authorised footprint is byte-identical to 92a7ca14fd904e4d962df38614b9af3a7ef41a11
ORACLE RESULT: PASS (all checks)
```

Warm final: 93 tests, 1091 assertions, zero failures/errors, and intent audit
`{:ok true :violations []}` in `b07/warm-verified-final.log`. Actual CLI partial F
and publication with a real proof subprocess passed in `b07/cli-boundary.log`.
Packaged CLI facts smoke on a fresh base archive also returned exactly equal facts
and snapshot, with no candidate/broad-analysis payload (36893.448593 ms under build
load; not a cohort result). The adapter usage smoke loaded the packaged compiler
and returned its expected usage exit 2. See `b07/frozen-facts-smoke-summary.edn`.
Touched lint: zero errors/warnings, one pre-existing informational redundant str
in core, in `b07/touched-lint-final.log`. Fresh-caller simulation reviewed CLI help
without implementation source; request, pending proof and guarded recovery
instructions were clarified. Final `git diff --check` is clean.

An initial cold gate exposed six test-registration/census failures; registration
and census pins were repaired, then warm-verified. Original failed log is retained
as `b07/cold-make-test.log`. The next attempt passed 805 JVM tests / 10224
assertions but found one of 887 Babashka tests failing on the missing repository
Claude reference mirror; `b07/cold-make-test-mirror-failure.log` preserves it.
The canonical package was synchronized using the repository script, the direct
mirror witness passed warm, and the complete gate was rerun. Final log:
`b07/cold-make-test-final.log`. Final cold `make test` exit **0**: 805 JVM tests / 10224 assertions and 887
Babashka tests / 7844 assertions, zero failures/errors. Analyzer recovery,
hygiene, wrapper and required shell self-tests also passed. The gate accepted
the existing fresh battery receipt; it did not rerun the full historical battery.
The newly added Cell B battery witnesses were run in the warm selection above.

Freeze: `b07-freeze.edn` records this commit, all 111 runtime file hashes,
implementation aggregate, adapter, manifest, exact snapshot-bound D request,
oracle shell and both independent helper hashes, original oracle hash,
hand-drive receipt, cold gate log and analyzer/Babashka identities. Runtime and
adapter/oracle copies are under `b07/frozen/`. Implementation SHA256:
`1ecda56273dd1ea524eab6b4fccd22ab2bc97fc684b655095c66facb8c3ad008`.
The copied facts CLI was smoke-tested; final frozen bytes and request binding
were checked. The oracle's only post-hand-drive change was the INTENT trace
comment required by the registry; final cold mutation witnesses cover its frozen
bytes. The original absolute oracle failure remains preserved, not rescored.

The requested cc-b07 worktree remains for review. The auxiliary Cell C worktree
was archived (`b07/cell-c-regression-output.tar.gz`) and removed; temporary baseline
archives and the owned development nREPL were closed. No global agent routing or
installed service configuration was changed.

## What the cohort operator runs next

Use `b07-freeze.edn`, `b07-D-request.edn`, `b07-manifest.edn` and `b07-arms.md`.
The latter contains the self-contained common/native task, F projection command,
D adapter command, unchanged original task, reviewer rubric and allocation.
First six fresh N runs establish the floor; then sequential matched blocks in
orders NFD, NDF, FND, FDN, DNF, DFN: 24 runs total. Same pinned Cell C model/version/
settings, fixed cores, serialized analyzer, fresh caller/worktree per arm. No
mid-cohort repairs, selective reruns or access to previous solutions. Two reviewers
judge anonymized preservation diffs against the frozen rubric.

Primary clock is runner-stamped orient-start through all frozen acceptance checks,
before prose. Discovery, acquisition, queueing, repair, fallback and proof count;
1,800-second cap, timeout/missing completion = failure. D wins only with all six
accepted, at least five without interface repair/native fallback, median paired
D/N <= .75, and median paired seconds saved >= 90 and > twice the native-floor
sample SD. F within 10% of D and the noise floor favors the facts-service mechanism.
A compiler correctness failure suspends the candidate. No comparative performance
claim follows from this feasibility build.
