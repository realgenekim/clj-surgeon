---
parent: high-level-design
prefix: PERF-SENT
---

# Adaptive Performance Regression Sentinel

## Context and Design Philosophy

The sentinel protects a known-good Surgeon release from a slower candidate. It
does not prove that a candidate is faster and cannot satisfy a promotion gate.
It uses a lower-cost adaptive schedule because a detector that is routinely
skipped provides no protection.

The measured subject is the complete verified agent route, not only server
execution. A frozen model, prompt, task, tool-selection policy and external
controller exercise the public operation exposed by two versioned product
runtimes. Versioned descriptions and schemas are part of the product under
test. The controller, scorer, fixture, clock and archive never come from either
product arm.

The first release slice monitors the frozen Sessionize 15-owner extraction on
`dev-a`. It compares one immutable stable release with one exact candidate. The
ordinary screen runs `C1, S1`; a slowdown at or above eight percent buys the
reverse `S2, C2` pair. The strict promotion protocols remain separate.

## Component Boundary

```text
controller request + allowlisted release owner
                       |
           adaptive sentinel coordinator
             /         |          \
    product materializer     environment admission
             |                + identity fences
      exact S and C roots             |
             \                existing clean Codex worker
              \             /       |        \
               versioned MCP     event clock  task scorer
                    |                 |            |
        product transaction + verifier receipts   |
                    \_________________|____________/
                                      |
                         pure evidence compiler
                                      |
                  green / yellow / red / invalid
                                      |
          decision + archive + external hash chain
                                      |
                 issue + Director + release receipt
```

The product runtime alone mutates the fixture and produces transaction and
verifier receipts. The existing clean Codex worker owns fresh workspaces,
private homes and MCP servers, event observation, semantic scoring, terminal
receipts, child cleanup and exit propagation. The new controller owns only
admission, exact product selection, serial scheduling, verdict compilation,
retention and external projections.

## Repository Components

| Component | Responsibility | Dependency rule |
|---|---|---|
| `bench/run_performance_regression_sentinel.sh` | Validate one invocation, materialize S and C, schedule attempts and pairs, retain the remote outer result and write `remote-execution-receipt.edn`. | Shell owns processes and order only. It does not calculate timing, parse semantic truth, append the ledger or mutate product source. |
| `bench/performance_regression_sentinel.clj` | Compile decoded attempt evidence, apply exact threshold arithmetic, derive screen decisions, final verdicts and chain-independent event bodies, and validate publication receipts. | `compile-attempt`, `compile-screen-decision` and `compile-verdict` are total pure functions over closed values. They never perform I/O or launch a model, MCP server, formatter, verifier or installer. |
| `bench/performance_regression_sentinel_io.clj` | Read exact named bytes, decode them into closed values for the pure compiler, and write returned canonical bytes once. | Imperative adapter only. It cannot derive a verdict independently or change returned bytes. |
| `bench/authorize_performance_regression_run.clj` | Under a short Skiff ledger lock, issue one controller-authenticated launch-admission receipt for the exact invocation. | Launch authority only. It cannot reserve a future head, append, clear red or authorize publication. |
| `bench/append_performance_regression_event.clj` | Lock and read the Skiff ledger, ask the pure compiler for a verdict/event body, wrap it with the current prior hash, and append the envelope once. | Imperative boundary only. It cannot alter compiler-returned body bytes, score independently or project issues. |
| `bench/import_performance_regression_result.sh` | Import a retained `dev-a` archive, structured result and archive receipt through an exact transfer manifest; then invoke local verdict, ledger and projection boundaries. | Runs on the Skiff release controller after remote retention. It never reads an unretained remote result tree. |
| `bench/project_performance_regression_event.clj` | Idempotently project an authoritative ledger event to its durable issue and Director mirror. | Projection cannot mutate ledger state or launch a model cohort. |
| `bench/run_clean_codex.sh` | Existing per-attempt lifecycle. | Gains only a closed versioned MCP product-root seam. All prompts, fixtures, clocks, scoring, cleanup and result formats remain controller-owned. |
| `bench/materialize_benchmark_candidate.sh` | Materialize each exact product source tree and receipt. | Reused unchanged for S and C. |
| `bench/sample_performance_regression_pressure.sh` | Capture one complete host-pressure sample immediately before one attempt under a frozen admission policy. | Observation and typed admission only. It never launches, queues or kills benchmark work. |
| `bench/event_timing.clj` and `bench/score_format_extraction.clj` | Existing raw clock and semantic authorities. | Reused unchanged; summaries and caller-supplied booleans are not evidence. |
| `bench/retain_benchmark_result.sh` | Existing immutable result retention. | Runs once on `dev-a` for the complete outer invocation under the executing controller checkout's `bench/results/`, never once per child attempt. |
| `bench/fixtures/performance_regression_sentinel/stable.edn` | Current explicitly nominated baseline identity and route capsule. | Data only. The runner cannot update it. |
| `bench/performance_regression_sentinel_artifacts.sha256` | Exact controller authority set. | Every path and hash is checked before model launch and after each pair. |
| `make install-verified` | Authoritative stable-publication command: deterministically run `verify-performance-regression-release`, then low-level `make install`. | The cutover receipt binds this exact chain. Raw `make install` is an emergency/non-publication artifact install and carries no stable-publication claim. |

No production namespace under `src/` changes in this feature. `make install`
and MCP reload remain deterministic and model-free.

## Versioned MCP Product Root

The current benchmark materializes PRE and POST CLI wrappers, but its MCP
server and client registry always load product source from the controller
checkout. Running historical checkouts' own workers would import historical
prompts and scorers and violate the external-controller boundary.

The worker therefore receives one closed `BENCH_MCP_PRODUCT_ROOT` for sentinel
attempts. It defaults to the controller repository outside this route. The
sentinel admits it only when all of these facts hold:

- it is canonical, absolute, directory-valued and confined to the invocation's
  setup root;
- an exact materializer receipt binds its full commit, tree, archive hash and
  relative source root, and the invocation manifest separately binds that
  receipt hash to the independently resolved canonical materialized root;
- the server launch directory resolves product `deps.edn`, `src` and `dev`
  from that root; and
- the client-registry launch resolves the same versioned product dependency
  while retaining the controller's registry observer.

Changing only the server or only the registry creates a split-brain surface and
is invalid evidence. The product root never supplies the fixture, prompt,
scorer, observer, controller or artifact manifest.

The worker validates the materialized root, commit, tree, cleanliness,
materializer-receipt hash and complete controller artifact manifest before and
after every attempt. Drift at either fence invalidates the invocation.

## Closed Invocation Request

The controller accepts one closed EDN request. Unknown and missing keys refuse
before authentication, product materialization or model launch.

```clojure
{:schema :clj-surgeon.performance-regression-sentinel-request/v1
 :invocation-id #uuid "..."
 :mode :prepublish ; :prepublish | :nightly | :backfill | :recovery
 :candidate {:commit "40-hex" :tree "40-hex"}
 :stable {:kind :release-tag
          :tag "stable-name"
          :tag-object "40-hex-or-nil"
          :commit "40-hex"
          :tree "40-hex"
          :baseline-receipt-sha256 "64-hex"}
 :controller {:commit "40-hex"
              :tree "40-hex"
              :artifact-manifest-sha256 "64-hex"}
 :recovery nil}
```

Recovery replaces `nil` with this closed arm. The blocking issue identity is
derived from the referenced ledger red and is never caller authority.

```clojure
{:red-event-id #uuid "..."
 :scope {:route-id :sessionize-format-extraction/apply-v1
         :task-sha256 hex64
         :policy-version 1}
 :stable {:commit hex40 :tree hex40}
 :candidate {:kind :same ; :same | :replacement
             :commit hex40 :tree hex40
             :supersedes nil} ; replacement requires exact prior commit/tree
 :owner {:id :allowlisted-release-owner
         :authorization-receipt-sha256 hex64}}
```

For `:kind :replacement`, `:supersedes` is exactly `{:commit hex40 :tree
hex40}` and must equal the blocked candidate. For `:kind :same`, it is `nil`.
Recovery is non-nil only in `:recovery` mode and mandatory there. Backfill may
use `:kind :historical-commit` with exact commit and tree instead of pretending
that a short commit label is an immutable release tag. Backfill has no
publication authority.

Recovery identities are not independent claims. Nested stable commit/tree must
equal both the top-level stable and the referenced red stable. Nested scope
must equal the referenced red scope and the controller-frozen route, task and
policy. A `:same` candidate equals both the top-level and blocked candidate. A
`:replacement` candidate equals the top-level candidate and its `:supersedes`
equals the blocked candidate. Owner authorization binds the red event, scope,
top-level stable, blocked candidate and accepted candidate. Any mismatch is a
prelaunch `:invalid :identity-drift`.

Before remote authentication or model work, Skiff issues a one-invocation
ledger-admission receipt. It binds the current head, current state, invocation
ID, mode, scope, stable, blocked candidate when any, accepted candidate and
supersession, owner authorization, nonce and bounded expiry. Its exact SHA and
controller authentication are frozen in the remote invocation manifest. Here
authentication means Skiff's coordinator supplies the expected receipt and
manifest hashes directly to the remote command boundary after transfer; they
are not fields accepted from the benchmark caller or model. Dev-a validates
them before launch. Ordinary prepublish refuses prelaunch when
the applicable ledger is already blocked; it directs the owner to recovery.
Recovery admission requires that exact red to be open. Final Skiff compilation
still rereads current state under the append lock: admission is launch
authority, not append or clear authority. State drift after launch fails closed
and never lets stale recovery evidence clear a newer state.

The model, reasoning, task, context, apply-only client policy, thresholds,
host, environment policy and release-owner allowlist are controller policy,
not caller options. The controller copies the selected allowlisted owner into
the compiled invocation manifest. The controller-owned ledger root is also
closed configuration and never appears in a caller request. The remote result
root is derived as `<controller-checkout>/bench/results/<invocation-id>` and
must be absent or empty, non-symlinked and canonically confined; arbitrary
caller paths are not accepted.

## Static and Per-Run Evidence

One compiled invocation freezes these static identities:

```clojure
{:controller {:commit hex40 :tree hex40 :artifacts-sha256 hex64}
 :worker hex64
 :policy hex64
 :fixture {:tree hex40 :task-sha256 hex64 :capsule-sha256 hex64
           :prompt-sha256 hex64
           :expected-source-manifest-sha256 hex64}
 :client {:codex-path canonical-absolute :codex-sha256 hex64
          :package-sha256 hex64 :platform-package-sha256 hex64
          :native-path canonical-absolute :native-sha256 hex64
          :node-path canonical-absolute :node-sha256 hex64
          :node-version string :os string :arch string
          :codex-version string}
 :model :gpt-5.6-sol
 :reasoning :high
 :tool-selection-policy hex64
 :profile {:bytes hex64}
 :host {:id "dev-a" :admission-policy-sha256 hex64}
 :surfaces {:stable {:advertised hex64 :client-observed hex64}
            :candidate {:advertised hex64 :client-observed hex64}}}
```

Stable and candidate surfaces may differ and are charged to the corresponding
release. When an arm has a second position, its advertised and client-registry
hashes must repeat exactly there; a two-run green does not infer a second
surface observation. Every other arm-independent identity is equal where
declared.

Each attempt records fresh, unique evidence:

```clojure
{:run-id #uuid "..."
 :position :C1 ; :C1 | :S1 | :S2 | :C2
 :arm :candidate
 :schedule-index 1
 :product {:commit hex40 :tree hex40
           :materializer-receipt-sha256 hex64
           :canonical-materialized-root canonical-absolute
           :pre-inventory-sha256 hex64
           :post-inventory-sha256 hex64
           :inventory-exact true}
 :resources {:workspace canonical-absolute
             :home canonical-absolute
             :port positive-int
             :result-root canonical-absolute}
 :pressure {:sampled-at instant
            :policy-sha256 hex64
            :receipt-sha256 hex64
            :status :admitted
            :complete true}
 :resolved-runtime {:formatter {:binary hex64 :argv hex64}
                    :verifier {:binary hex64 :argv hex64 :profile hex64}}
 :clock {:turn-start-ns positive-int
         :turn-complete-ns positive-int
         :t-verified-ns positive-int}
 :route {:adherent true :expected-tool-calls 1
         :unexpected-actions []}
 :correctness {:passed true :source-set-exact true
               :scorer-schema keyword
               :scorer-sha256 hex64
               :scorer-inputs-sha256 hex64
               :expected-source-manifest-sha256 hex64
               :final-source-manifest-sha256 hex64}
 :verification {:complete true :exact true :receipt-sha256 hex64}
 :artifacts {:events hex64 :event-clock hex64 :terminal hex64
             :runs-row hex64 :semantic-score hex64
             :client-registry hex64 :product-receipt hex64}}
```

Resource roots and ports are pairwise unique. An exact controller-owned
admission command samples immediately before every attempt; C1, S1, S2 and C2
therefore have distinct complete samples newer than the preceding attempt.
Pair-level identity rechecks cannot replace per-attempt admission. Pressure
samples are fresh and bound to the versioned admission policy. A product
receipt is product evidence captured by the controller; the controller does
not fabricate it.

A materialized Git archive is not treated as a worktree. “Clean” means its
ordered path/type/size/content-hash inventory equals the materializer's frozen
archive inventory before and after the attempt, apart from an explicit
controller-owned generated-path allowlist whose own hash is frozen. Candidate
generated dirt outside that set invalidates the attempt; `git status` is not
used on the materialized root.

The fixture profile bytes are equal before all runs. Formatter and verifier
binary, argv and profile evidence are resolved independently inside each
attempt and must compare equal across arms. Different interpretation of equal
profile bytes makes the invocation invalid.

## Attempt Compilation and `T_verified`

The I/O adapter reads and hashes raw `events.jsonl`, `event-clock.tsv`,
`terminal.tsv`, the exact `runs.tsv` row, semantic score, client registry,
product receipt, environment receipt and materializer receipt. It decodes them
to closed values; pure `compile-attempt` joins only those values. The compiler
requires one unique turn ID
and one unique apply call ID joined exactly across start, completion, capture,
result and receipt records. The apply is the first actionable post-turn item:
there are zero earlier messages or actions, exactly one apply call and zero
other tool, shell or file actions. Captured arguments equal the compiled
request. Product-result, read-back and verifier receipt hashes belong to that
same call ID. Observable ordering is strict:

```text
turn start < apply start < apply completion < turn completion
```

The joined apply completion must itself carry `verification_complete=true`,
the exact verifier receipt, and matching result/read-back hashes for that call
ID. Verification therefore precedes the observed completed response without
inventing a separately observer-clocked verifier event. Server phase timing is
diagnostic only.

Every raw lifecycle record is consumed exactly once. Duplicate, orphan,
reversed, extra or mismatched records invalidate the invocation.

`T_verified` is the positive integer difference between observed turn-start
and turn-complete monotonic nanoseconds. Process wall and server elapsed remain
diagnostic fields and cannot substitute. A missing, reversed, repeated or
non-finite clock makes the invocation invalid before any slowdown is computed.

The controller-owned clean worker derives semantics once while its temporary
workspace exists. Before cleanup it emits a closed semantic-score receipt that
binds the scorer schema and hash, scorer-input hash, expected ordered
path/hash manifest, final ordered path/hash manifest, and exact source-set
manifest. `compile-attempt` validates that receipt plus raw route and verifier
events; it cannot rerun the scorer after workspace deletion. It does not accept
a summary's `correct=true`, a caller aggregate or surviving rows after another
attempt failed.

## Exact Threshold Arithmetic

Thresholds use positive integer nanoseconds and integer cross-products. They do
not use binary floating point, rounded percentages or presentation strings.

```text
first-pair trigger       100*C1 >= 108*S1
position-one loss        C1 > S1
position-two loss        C2 > S2
pooled red               100*(C1+C2) >= 110*(S1+S2)
recovery position one    100*C1 < 108*S1
recovery position two    100*C2 < 108*S2
recovery pooled          100*(C1+C2) < 108*(S1+S2)
```

The median of two observations is their midpoint, so the pooled ratio reduces
exactly to the ratio of sums. Equality is intentional: exactly eight percent
triggers, exactly ten percent can be red, and exactly eight percent does not
recover a red. Equal walls are not a positional loss.

## Screen State Machine

```text
PRECHECK
  invalid -> INVALID; no model launch when detectable before launch
  valid   -> admit C1 -> C1 -> admit S1 -> S1

PAIR 1
  either attempt invalid -> INVALID
  C1 < 1.08*S1          -> GREEN terminal; no reverse pair
  C1 >= 1.08*S1         -> admit S2 -> S2 -> admit C2 -> C2

PAIR 2
  either attempt invalid                  -> INVALID
  C1>S1 and C2>S2 and pooled>=10%         -> RED
  every other valid triggered invocation  -> YELLOW
```

An extra reverse pair after green is a schedule violation. A trigger without
both reverse attempts is invalid, not yellow. Every launched attempt remains
in the invocation ledger. Retrying infrastructure creates a new invocation ID
and never drops or overwrites the failed one.

## Recovery State Machine

Recovery precheck requires one open red event, its exact stable identity, an
allowlisted release owner, explicit resolution authorization and one accepted
candidate identity. A replacement candidate can clear an earlier candidate's
red only when authorization explicitly binds `:supersedes-candidate`; Git
ancestry is not inferred as authority.

Recovery always runs `C1, S1, S2, C2`; it never uses the adaptive stop. Each
position receives its own immediately preceding pressure admission.

```text
any invalid attempt or evidence -> INVALID; prior red remains open
all three <8% predicates        -> RECOVERED; append event; close prior red
ordinary red predicate          -> RED; append evidence; prior red remains open
every other valid result        -> YELLOW; prior red remains open
```

A recovery yellow exits nonzero because the prior red still blocks
publication, even though an ordinary screen yellow is non-blocking.

## Closed Verdict

The verdict is a disjoint tagged union. Every variant also carries the common
schema, invocation, stable, candidate, mode, `:promotion-authority false` and
`:next-action :none` fields. It does not contain archive identity; that identity
does not exist until after the decision is retained.

| Status | Required schedule and metrics | Reason enum | Required verdict exit |
|---|---|---|---|
| `:green` | Screen only; `[:C1 :S1]`; `d1` only | `:below-trigger` | `0` for nightly/backfill or prepublish with resulting `CLEAR`; otherwise nonzero |
| `:yellow` | Exactly four rows; `d1`, `d2`, pooled | `:triggered-not-confirmed` or `:recovery-not-cleared` | `0` for nightly/backfill or prepublish with resulting `CLEAR`; otherwise nonzero |
| `:red` | Exactly four rows; `d1`, `d2`, pooled | `:confirmed-regression` | nonzero |
| `:recovered` | Recovery only; exactly four rows; all three strict recovery ratios | `:recovery-proved` | `0` only while candidate identity still equals the authorized publication candidate |
| `:invalid` | Zero through four retained attempts; no slowdown metrics | one closed invalid reason below | nonzero |

Closed invalid reasons are `:candidate-correctness-failure`,
`:stable-baseline-failure`, `:invalid-environment`, `:invalid-evidence`,
`:identity-drift`, `:schedule-violation`, `:not-comparable`, and
`:incomplete-archive-input`. Candidate drift after scoring is always
`:invalid :identity-drift`, never a recovered verdict with a nonzero exit.

Publication state is total:

| Mode/result/current ledger | Publication state |
|---|---|
| Prepublish green or yellow; applicable ledger `CLEAR` | `:allowed` |
| Prepublish green or yellow; applicable ledger `BLOCKED` | `:blocked` with the original red event ID |
| Prepublish red or invalid | `:blocked` |
| Nightly or backfill | `:not-applicable` |
| Recovery recovered for the exact authorized candidate | `:allowed` |
| Recovery yellow, red or invalid | `:blocked` with the prior red event ID |

The verdict contains its exact `:required-process-exit`. Measurement status is
computed first; resulting ledger state is the transition from current state;
publication is allowed only for prepublish/recovery when the resulting state is
`CLEAR` and candidate identity is exact. No status, schedule, reason, metric,
ledger-state or publication combination outside these tables is representable.
The later public terminal receipt contains `:process-exit`: for prepublish and
recovery it is zero only when publication is allowed; for nightly/backfill it
uses the measurement-status exit. It is also nonzero whenever a later lifecycle
boundary fails. A lifecycle failure or applicable prior red can therefore make
a valid green/yellow command fail; neither can turn red or invalid into zero.

Remote scheduling, local verdict compilation and authority are separate:

```text
dev-a: compile attempts -> screen decision -> retained attempt evidence
Skiff: import evidence -> lock/read ledger -> final verdict + event body
Skiff: envelope + append event             -> authoritative ledger verdict
append failure                             -> terminal publication failure, nonzero
```

A ledger-append failure has no ledger verdict or event. The coordinator retains
the raw invocation archive and a failure receipt, but must not claim that the
failed event entered the ledger. Terminal coordinator failure reasons are the
closed set `:retention-failure`, `:import-failure`, `:ledger-append-failure`,
`:projection-failure`, and `:coordinator-boundary-failure`; they are receipts,
not sentinel statuses.

Issue and Director projections are idempotent, replayable consequences of an
authoritative ledger event. A red projection failure leaves the ledger blocked
and exits nonzero. A yellow projection failure leaves the ledger yellow and
nonblocking but the coordinator receipt incomplete and exits nonzero until
replayed. A recovered projection failure leaves the ledger recovered but exits
nonzero until the blocking issue and Director mirror are reconciled; it never
launches a second model cohort. A later green or recovered event closes the
distinct yellow attention issue, red supersedes it, and invalid does not clear
it. Projections never mutate ledger state.

## Append-Only Ledger

The verdict ledger lives under the controller-owned canonical root
`~/.local/state/clj-surgeon/performance-regression-sentinel/ledger`, expanded
and frozen by a versioned configuration hash before launch. The root cannot be
caller-selected, symlinked, `/`, the workspace root or a result root. Skiff is
the sole ledger and final-verdict authority. Dev-a never reads or writes the
ledger. An exclusive ledger lock serializes the following local transaction:

1. read and validate the current chain head and prior state;
2. call pure `compile-verdict` with imported evidence and that state;
3. produce a chain-independent canonical event body;
4. create the authoritative envelope
   `{:prior-event-sha256 current-head :event-body-sha256 body-sha
   :event-body body}`; and
5. append the envelope once and advance the head cache.

Each event envelope is created once. A mutable head pointer is a cache, not
authority. The archive binds the exact evidence from which the local body is
compiled; it does not pretend to contain future chain bytes. A ledger append
must succeed before an externally usable verdict exists. Concurrent state is
resolved under this short local lock; no filesystem lock or stale head is held
across remote model work.

The ledger maintains one authoritative event per exact invocation ID. Under
the lock, an absent ID may compile and append once. A present ID with identical
evidence, body and envelope identities returns the original append receipt and
replays projections only. A present ID with any differing hash or state is a
terminal coordinator boundary failure. Replay never recompiles an already
appended invocation against a later head. If a crash creates the immutable
event file before updating the head cache, chain-file validation repairs the
cache and returns that original event; the cache never decides whether a
second event is allowed. Projection idempotency is keyed by authoritative event
ID.

Ledger state is keyed by exact stable identity and the closed regression scope
`{:route-id :sessionize-format-extraction/apply-v1 :task-sha256 hex64
:policy-version 1}`:

```text
CLEAR   + GREEN/YELLOW/INVALID   -> CLEAR; append event
CLEAR   + RED                    -> BLOCKED(red-id)
BLOCKED + GREEN/YELLOW/INVALID   -> BLOCKED(original-red-id); append event
BLOCKED + RED                    -> BLOCKED(original-red-id); append evidence
BLOCKED + RECOVERED              -> CLEAR; append recovered event referencing red
```

The durable issue is the work-and-owner projection. Red creates or updates one
blocking issue. Yellow creates or updates one distinct non-blocking issue.
Director mirrors the issue. Projection receipts bind the ledger event ID and
are safe to replay. Acknowledgement, issue closure or Director state cannot
mutate ledger state.

## Retention and Publication Receipt

The exact controller checkout runs on `dev-a`. Its outer result is derived from
the invocation ID and must be an empty
path under that checkout's `bench/results/`. Child attempts do not retain
independently. After final scoring, the remote controller invokes the existing
retention script exactly once. A later deterministic Skiff import copies only
the immutable archive, structured result and archive receipt through an exact
transfer manifest into a fixed controller archive root. The caller cannot
select either archive root.

Retention and ledger authority use this non-circular sequence:

1. Pure functions return canonical attempt evidence and an adaptive screen
   decision; the remote coordinator writes those bytes once.
2. Remote retention archives those exact bytes, `remote-execution-receipt.edn`,
   invocation and environment receipts, materializer receipts, the complete
   attempt ledger, every raw attempt, and screen or final reports. Failed and
   losing attempts are never omitted.
3. The Skiff importer verifies the transfer manifest, archive SHA, structured
   manifest SHA and archive receipt. It independently recompiles every attempt
   and the adaptive screen decision from imported raw evidence, requiring exact
   equality with dev-a's attempt evidence, schedule and screen decision. This
   catches an early stop after a true trigger or an extra pair after green.
4. Under the local ledger lock, the pure compiler returns the final verdict and
   chain-independent event-body bytes from imported evidence plus current
   ledger state. The append boundary wraps and appends them once.
5. The projection adapter reconciles issue and Director state.
6. Skiff writes `publication-receipt.edn` outside the tar. It binds the remote
   execution receipt, attempt-evidence SHA, archive SHA, structured-manifest
   SHA, verdict SHA, event-body SHA, event-envelope SHA/ID, append receipt and
   projection receipts.

`remote-execution-receipt.edn` can claim only remote execution. The existing
post-tar archive receipt claims retention and archive identity. Only the later
Skiff `publication-receipt.edn` can claim ledger authority,
projection completion or publication eligibility. An append or import failure
is a terminal publication failure, not an invalid ledger event. The immutable
archive and failure receipt remain in a durable local outbox. Publication-
boundary reconciliation replays the same imported evidence through the failed
import, append or projection boundary without another model cohort. It can only
finish the same verdict/event/projection; it cannot produce `:recovered`,
change sentinel status or clear a red. Product-regression recovery always uses
the fresh forced four-run state machine.

`verify-performance-regression-release` is a deterministic local receipt check,
not a model run. It verifies the imported publication receipt, candidate commit
and tree, archive, manifest and ledger chain, projection completeness, and that
no applicable red remains open. Missing, invalid, red, projection-incomplete or
identity-mismatched evidence exits nonzero. A valid ordinary yellow remains
publication-eligible under the approved HLD but cannot advance the baseline.
`make install-verified` is the single stable-publication entrance. It runs this
check immediately before low-level `make install` and records the exact chain
in the cutover receipt. Direct `make install` remains possible only as an
emergency/non-publication artifact installer and cannot create a stable tag or
claim sentinel-cleared publication. Install and reload never launch Anvil or
invoke the sentinel.

## Baseline Nomination

The baseline manifest is data owned by release coordination. It binds the tag
literal, tag-object hash when annotated, full peeled commit, tree, route version,
nominating owner and nomination receipt. A separate explicit command may write
a new manifest only after ordinary release gates and no applicable open red.
The sentinel runner cannot write, infer or auto-advance it.

## Historical Backfill

The first backfill uses the common frozen capsule under the external controller:

| Position | Stable | Candidate | Meaning |
|---|---|---|---|
| 1 | `b8e52cb603c35471cab6d4f562161a1a588c3b20` / tree `fe42cf35db2c743bd64351fab65f03f63686034e` | `75585beeda63a4dcc9bb1e219d5721d89b93baa2` / tree `b61fe3610643ddd23f1e7061879ac871b55e623a` | Negative control. The relevant MCP runtime and historical runner are byte-identical; observed delta is noise, not product speed. |
| 2 | `75585beeda63a4dcc9bb1e219d5721d89b93baa2` / tree `b61fe3610643ddd23f1e7061879ac871b55e623a` | `19ab864889799b0028a5f7cb66c63b957ff7b973` / tree `72e1ef5fc09587013e7b8d60f2ed027385280973` | Route-specific release experience. Extraction kernel is unchanged; apply description and schema effects are charged to the candidate. |

This is an observed install sequence, not a linear Git delta. `75585be` is not
an ancestor of `19ab864`; their merge base is `b8e52cb`. The controller never
constructs the second candidate by applying one ref's diff to another.
Comparison two starts only after comparison one has a valid terminal verdict,
and it creates fresh evidence for `75585be` rather than reusing its earlier
stable or candidate run. A direct `b8e52cb` versus `19ab864` span is diagnostic
context only.

The fixture tree is
`b0a3578f0cfc310afe17a4201a8a6095a057f070` in all three refs. The
task Git blob is `58dee06dfef9bf41b7ca26845d07380436354d9a`, the capsule blob is
`a91227ae77a1618eae94f68cd7b7c039064fd619`, and the exact-profile
SHA-256 is
`9976206370ed858dbe0831928e4894f084a779af4f6243485fefdde1fc4e17ac`.
The fully materialized common prompt is 1,949 bytes with SHA-256
`c2e7cdc2c4e3c88aa6b00d395073dade0d38e89ee45a41876892a525f17c3ed1`;
the 808-byte tool-first overlay has SHA-256
`7ec3381861c53053ce98802f0c038b05709843bc6453b2e6e59f9eeb048fd6c7`.
The manifest binds rendered prompt bytes, not only a function that can render
them. The `19ab864` annotated release tag object is
`bd1f53d2ba00f270459cc5c02163e7290da06292`.

Before model launch the controller captures each ref's complete advertised
catalog and canonical client projection. `b8e52cb` and `75585be` projections
must be byte-identical. `19ab864` may differ only in the
`apply_clojure_changes` description and input schema; apply name, output schema,
annotations and peer tools remain equal. The normalized extraction request is
identical after replacing the unique workspace root with one sentinel token.

All three versions must execute the same normalized extraction request,
one-shot apply route, exact profile and semantic oracle. A version that cannot
expose the route is `:not-comparable` and receives no synthetic timing. Complete
wall includes the versioned apply surface and is route-experience evidence.
Server-authoritative extraction clocks are diagnostic localization only.

## Promotion Refusal

Every sentinel verdict fixes `:promotion-authority false`. Sentinel and
promotion invocations use distinct schemas and IDs. The sentinel cannot emit a
speedup-earned result, update a promotion receipt, advance stable, import the
native midpoint as its comparator or contribute a row to a promotion cohort.
Repeated nightlies and a faster backfill remain sentinel evidence only.

## Failure and Refusal Matrix

| Failure | Classification | Effects |
|---|---|---|
| Missing or unknown request field, wrong mode or mutable ref | Invalid configuration | Refuse before auth, materialization or model launch. |
| Dirty controller or product, moved tag, artifact drift or surface drift within one arm | Invalid evidence | Stop nonzero; retain every launched attempt. |
| Missing, stale, incomplete or rejected pressure admission | Invalid environment | Do not label the product slow. |
| Candidate semantic, route or verifier failure | Candidate correctness failure | Stop publication nonzero without a timing metric. |
| Stable semantic, route or verifier failure | Stable baseline failure | Stop publication nonzero without blaming the candidate. |
| Triggered but incomplete or extra schedule | Schedule violation | Stop nonzero; never infer missing timing. |
| Archive transfer, manifest, retention or ledger append failure | Terminal coordinator failure | No new authoritative ledger verdict; retain raw archive/failure receipt and stop nonzero. |
| Issue or Director projection failure after an authoritative event | Projection failure | Ledger state remains authoritative; coordinator receipt stays incomplete and exits nonzero until idempotent replay succeeds. |
| Unsupported historical public route | Not comparable | No timing; publication not applicable. |

## Cutover and Rollback Gates

The sentinel begins as an on-demand controller and historical backfill. It does
not gate publication until its no-model contract and one complete model-bearing
Anvil replay prove the evidence join, adaptive stop, reverse-pair schedule,
archive and exit behavior. The existing manual release process remains
authoritative until that cutover receipt.

After cutover, rollback removes only the pre-publication receipt requirement and
scheduled trigger. It does not delete ledger events, archives, issues or prior
red state. Product source, install and reload paths are unchanged, so sentinel
rollback cannot change runtime behavior.

## Explicit Non-Goals

- A speed-promotion shortcut or a new native comparison.
- A second model runner, task scorer, event clock, archive engine or mutation
  runtime.
- Live Anvil, model or network work inside `make install` or MCP reload.
- A rolling or automatically advanced stable baseline.
- Pooling nightly invocations to manufacture confidence.
- Byte-identical stable and candidate tool surfaces.
- A claim that a green screen proves no small regression exists.
- Product changes under `src/`.

## Decisions and Alternatives

| Decision | Chosen | Alternatives rejected | Rationale |
|---|---|---|---|
| Schedule | Adaptive `C,S`, then conditional `S,C` | One run; fixed four runs every install | One run is noise theater; fixed counterbalance charges every install and invites skipping. |
| Controller | Current external worker plus a thin coordinator | Each product's historical harness; cloned lifecycle | Keeps prompt, scorer and evidence constant and prevents self-scoring products. |
| Version seam | Exact MCP product root for server and registry | Global installed binary; server-only override | The product under test must own both runtime and visible surface without importing its scorer. |
| Verdict arithmetic | Integer cross-products | Rounded percentages; binary floating point | Makes 8% and 10% boundaries exact and portable. |
| Stable identity | Tag object, peeled commit and tree | Tag literal; rolling mean | A movable name or self-updating baseline cannot be authority. |
| Publication integration | Deterministic receipt validation before install | Run the sentinel inside install | Keeps installation recoverable, deterministic and independent of live Anvil. |
| Red recovery | Explicit owner-authorized forced four-run cohort | Acknowledgement; two-run green; automatic clear | Recovery must prove both orders below the trigger and preserve the original red event. |

## Open Questions and Future Decisions

### Resolved

1. Yellow is a valid non-blocking result for ordinary screens and remains
   visible until a fresh invocation supersedes operational attention.
2. Invalid evidence blocks the publication attempt but is not a product
   slowdown.
3. The negative-control pair calibrates noise and cannot earn a speed claim.
4. Sentinel evidence never satisfies promotion.

### Deferred

1. Whether more frozen task families earn independent sentinels.
2. Whether a dedicated remote ledger service replaces the configured
   controller-owned hash chain.
3. Whether accumulated nightly data later earns a separately predeclared
   statistical detector.
4. Whether a no-model kernel canary becomes an additional attribution tier.

## References

- [High-level design](../../high-level-design.md)
- [Closed relations promotion log](../../observations/2026-08-29-captains-log-closed-relations-earned-a-hold.md)
- [Terminal proof and retained phase clocks](../../observations/2026-08-27-captains-log-terminal-proof-ended-the-second-plan.md)
- [Benchmark evidence retention](../../must-fix/001-archive-benchmark-evidence.md)
