# Frame 7 protocol and item 1 evidence-consumer contract

**Status: protocol frozen for adoption; implementation and runner qualification are not claimed.** Gene authorized item 0, “go with astra’s order,” on 2026-09-09. This document specifies the experiment and the first consumer slice. It authorizes no experiment launch, installation, service call, or publication. The repository was read-only; this report is the only file intentionally written. No commits, new arms, server calls, or connections to the prohibited ports were made.

Authority is Gene’s current brief and the consult at `/home/forge/src/clj-surgeon-records/docs/observations/2026-09-09-astra-squares.md`. Repository contracts were read at checkout commit `3ea3803ea2e303a961a3edc1d76b8095ab5ff4f7`, tree `ad473e5d4600fed158e7bbe453571debadb2a20d`, in `/home/forge/src/clj-surgeon-astra-consult2`. Below, **R** means that checkout and **O** means `/home/forge/src/clj-surgeon-records/docs/observations`. These are absolute-path abbreviations, not references to an unspecified current trunk. Appendix A pins the bytes used.

“MUST,” “refuse,” and “unknown” below are normative. A historical observation is explicitly identified as historical. New field names and refusal types specify item 1’s interface; their appearance here does not mean existing programs implement them. This is a protocol, not a declaration that the rows, custody boundary, consumer, or Frame 7 entrance have qualified. Gene’s September 8 condition remains: qualify the rows and reliably sublime operation before the exit cohort. See `O/2026-09-03-captains-log-anvil-seat.md`, 16:35Z entry on September 8.

## 1. The clocks and the acceptance event

**The primary is elapsed request-to-accepted-and-landed time, `L = tL − t0`.**

- **`t0` is the independent runner’s prospective release of the task, before the caller receives its bytes or performs task-specific preparation.** The runner records release durably before opening task access or dispatching it. A caller’s first shell command, first model response, “orient-start,” tool entry, or ship invocation is not t0.
- **`tL` is the first independently observed publication of the final accepted tree on the registered benchmark landing ref, after all required behavior checks, review, coverage and freshness obligations are discharged.** The runner/observer reads the destination ref and tree itself and binds them to the acceptance record. A local commit, push command exit, producer “LANDED,” receipt path, or caller “done” is not tL. Observation/transport latency remains charged; do not backdate to an asserted remote publication time.

The landing ref is specified in §3. An accepted worktree with no observed landing has request-to-accepted evidence and **landed unknown**. It must never be relabelled landed. Publication before acceptance is a lifecycle violation; later acceptance cannot retroactively legitimize that publication timestamp.

| Event/clock | Exact boundary and use | Owner |
|---|---|---|
| `release` / t0 | Prospective release, before task disclosure and task-specific work | Independent runner |
| `candidate-submitted(i)` | Runner freezes attempt i’s actual candidate identity after the submission event; retains the pre-cleanup state too | Runner, never caller timestamp |
| `oracle-enqueued/start/end(i)` | Submission automatically queues the registered oracle on that frozen candidate. Record actual queue and execution separately | Runner |
| `review-start/end(i)` | Independent review of the identified candidate, with complete diff available and declared review obligations | Runner observes reviewer lifecycle |
| `accepted(i)` | Consumer conjunction of every obligation in §4, including held-out oracle and review, succeeds on one candidate | Qualified acceptance consumer, observed by runner |
| `tA` / request-to-accepted | Accepted event for the final candidate that remains eligible for publication; `A = tA − t0` | Runner |
| `caller-done` / tD | Runner receives caller completion/termination. Retain every resumed completion and the final one; `D = tD − t0` is secondary | Runner |
| `candidate-ready` | Optional caller-declared milestone observed on arrival, separately labelled; preserves old candidate-window comparisons | Runner observes claim; claim is not acceptance |
| `published` / tL | First independent destination-ref observation proving the final accepted tree is published after acceptance | Independent observer/runner |
| `first-decisive-red`, `next-accepted` | First terminal red that requires repair; measure red-to-next-accepted and all retries | Runner |
| `request-timeout` | 1,200 s after t0, including landing, unless amended before the cohort starts | Runner |

The declared lifecycle boundary is **candidate submission after required candidate cleanup**, not a human noticing a log. On every submission the runner freezes bytes, registers the candidate, and automatically enqueues acceptance. Checks and review may overlap according to the registered DAG. The acceptance consumer fires automatically when the last required witness arrives and its final identity/freshness check succeeds. Worker completion after caller-done is still inside L. A pending detached closure cannot terminate the request.

If housekeeping changes the candidate, it creates a new candidate identity, incurs its time, and triggers fresh acceptance for that identity. A cleanup explicitly confined to runner-owned output outside the candidate is recorded but is not a code repair. Never silently delete unexpected candidate paths before preserving and judging them. Review amendments, reseals, merge-base movement and proof invalidation retain all earlier events; they do not reset t0. If a previously accepted candidate changes before publication, its earlier tA is retained as an invalidated milestone and the final tA must be reacquired. If a request fails after acceptance but before landing, A may be known while L remains absent.

### Charged work and preparation

Charge **orientation, route discovery, schema fetch, receipt decoding, every repair and refusal, queueing, task-specific setup, cold loads, required tests, review, resealing, transport and publication**. Charge fallback, failed delivery, capability repair, observer calls, machine-owned proof and cleanup after the caller says done. No subtractions for a file-pointer brief, slow tool discovery, an operator delay, a repeated gate, a restamp, or “work the model did not do.” Six ship attempts are **one request with six attempts**, not six independent successes and not the last green ship’s wall. Keep attempt-level first-red, refusal, repair and resource costs underneath that request.

Task-independent prepared services may exist before t0: the P/G readiness package is the treatment. Freeze and attest its contents, startup wall, resources and custody **before task disclosure**. Report prepared L, cold-entry total (actual startup plus completion, respecting overlap), and startup cost amortized over the predeclared cohort of 24 comparison requests per arm. Also show the unamortized first-request cost. Do not amortize over an imagined future workload. A generic test image may be warm; a preload, preview, mapping, discovery or request file solved for the selected task belongs after t0. If a fully mapped task is itself the user’s request, disclose and give the exact same mapping to N/G/P; that experiment tests supplied-mapping execution, not discovery. All path binding, rendering and task-specific checkout setup is charged.

Publish the event dependency graph, with each worker’s start/end, dependencies, queue wait, model delay, CPU/RSS and occupied-slot time. Never sum overlapping suites to obtain L. A gate finishing before a longer review may save compute without changing landing wall. The `573 s review / 316 s prewarm / 165 s battery / 594 s ship` record is a historical critical-path example, not a sum or a new native comparison (`O/2026-09-03-captains-log-anvil-seat.md`, September 9 08:17Z). The same entry records **six ship runs** leading to that last run. Its 594 s is not the complete six-attempt request clock; a prospective t0 for that request is not retained here.

### Clock uncertainty and custody

Use one runner monotonic clock domain for authoritative release and observed terminal events, retaining UTC, monotonic ticks, host/boot identity, sequence number, event source, request/attempt ID and candidate ID. If a remote worker reports its own spans, keep those as diagnostics; runner receive times bound the authoritative lifecycle. Record polling cadence and last-negative/first-positive observations. Cross-host subtraction requires measured offset/error bounds; otherwise use runner observation times or mark the duration unknown. Clock resets, missing release, lost events, contradictory candidate identities or inaccessible observer evidence are **unverified-clock**, never zero or an estimated success.

Do not derive missing t0 from the first action or missing tL from “ship_elapsed.” Conservative endpoint bounds produce ratio bounds; a claimed improvement must survive the worst compatible bounds. Observer/credential authority must be unreachable by the measured subject, including through retained admin credentials. The observer must acquire the relevant facts, not sign supplied assertions. Recorder v1’s same-UID forgery and the `gene` SSH/sudo path remain qualification counterexamples (`O/2026-09-08-recorder-v1.md`; `O/2026-09-09-anvil2-provisioning.md`). An account name is not qualified custody. Offline consumer replay may proceed with explicitly synthetic authority; live consumption and the confirmatory clock wait for custody qualification.

## 2. Statistics, failures and the second Frame 7 number

For stratum j and fixed task slot s in 1…6, calculate **paired per-slot ratios** `rPN[j,s] = L(P,j,s)/L(N,j,s)`, `rPG[j,s] = L(P,j,s)/L(G,j,s)`, and `rGN[j,s] = L(G,j,s)/L(N,j,s)`. For each contrast sort its **six** ratios and take **the arithmetic mean of the third and fourth ordered ratios**. This is the registered six-slot statistic. Do not divide cohort totals or marginal medians; do not average rounded inputs. Apply the same paired convention to reported secondary clocks.

The four-stratum portfolio summary is the arithmetic mean of the four stratum median ratios, weighting repositories, caller families and the six slots equally. Publish all four medians and all 24 per-case ratios immediately beside it; the portfolio number does not create evidence for another fixture or caller. The earlier consult proposed mean log ratios; the current brief explicitly selects the six-pair median. **This document makes that prospective change explicit: paired median is primary; mean log ratios, if printed, are secondary and cannot rescue a failed primary.**

A request without acceptance/publication by the cap has no L. Retain its timeout and all elapsed/resource cost. For the conservative admission ledger, any incomplete member of a contrast contributes **+infinity/non-winning**, including a failed native denominator; report this sentinel separately from a measured ratio. Unknown clock or apparatus invalidity blocks admission, rather than becoming an infinity labelled a performance loss. Print both a complete six-slot admission ledger and any finite-pair sensitivity, explicitly secondary. No deletion of inconvenient slots, individual failure replacements or success-only headline. A whole rerun requires a new registration and keeps the invalid original cohort visible.

Final acceptance must be equal and complete across all comparison requests for a portfolio speed claim: no false green, scope escape, missing pair or unresolved observation. Report first-attempt acceptance, final acceptance, refusals by type, repaired attempts, fallback and interventions separately. “Final green after repair” is not “first-attempt success.” A correctness failure suspends the affected routed class immediately. Preserve failures even if they occur in a control.

**Uncertainty rule:** a win requires the primary ratio below 1, a pre-registered 95% uncertainty interval excluding 1, and an operational effect greater than two standard deviations of the six identical N floor runs, measured in raw seconds. For each stratum, calculate sample SD with denominator 5 and paired saved seconds as the median of the six `L(B)−L(A)` differences. Every stratum included in an unqualified whole-portfolio win must improve and clear its own `2×SD` screen; otherwise label any eligible narrower result. The anchor floor diagnoses local noise, not every task’s variance, and the two-SD screen is not a significance test. Missing floor endpoints invalidate the screen. No population p95 from six observations; print the worst observed latency.

To make “interval” executable rather than discretionary, qualify and freeze this finite-portfolio randomization method before launch. For each contrast A/B, condition on the third arm’s position. Within each stratum the six N/G/P orders contain two tasks at each third-arm position; swapping A/B in both tasks of one such pair preserves the six-order design. There are three independent swaps per stratum, hence 8 admissible assignments in a one-stratum pilot and 4,096 across four strata. For a candidate multiplicative effect theta>0, divide observed A times by theta, keep B times, enumerate those admissible reassignments, and recompute the same equally weighted median-ratio statistic T. Use `abs(log(T))` as the two-sided statistic, counting ties in the tail; p is the exact tail fraction. Invert tests at alpha .05; report the hull of non-rejected theta values, including unbounded endpoints rather than clipping them. Retain the entire accepted set if disconnected. Missing outcomes or unresolved timing intervals do not yield a confirmatory interval. With bounded clock errors, use the conservative union over compatible times.

This is a randomization interval under the explicit sharp constant-multiplicative-effect/no-interference model for the **fixed portfolio**, not a population interval over all Clojure tasks. Publish heterogeneous per-task results and that assumption alongside it. A tiny pilot may be incapable of a 5% rejection under its eight assignments; do not replace its analysis after seeing that. Consumer/scorer qualification must verify enumeration, inversion and median arithmetic on fixed synthetic inputs before the first arm. P/N is the first primary test; only if it clears all gates is P/G tested as the incremental primary (fixed-sequence testing). G/N and all secondary clocks are descriptive; narrower post-hoc strata do not become confirmatory findings.

**Frame 7’s second number remains apparatus actions / all caller actions.** One externally observed tool invocation or public shell invocation is one action; a compound shell invocation remains one, with all purposes annotated. Count model returns separately. Across the entire caller lifecycle classify task, apparatus, mixed or unknown using full commands/results. Publish absolute A and T and the interval `[A/T, (A+mixed+unknown)/T]`; T=0 is undefined. Report bytes, receipt-only decode actions, covered re-derivations, independent checks, refusals/fallbacks and supervisor actions. Two independent judges resolve disputed classifications; unresolved motives remain annotations. No count window that excludes orientation or machinery after mutation can stand in for the complete census. Worker actions and costs get their own ledger; moving work to a worker does not erase wall.

## 3. Portfolio, equipment, refs and launch registration

| Arm | Frozen equipment and task access | Claim it identifies |
|---|---|---|
| N — ordinary native | Fresh caller, ordinary tools and normal task context, no prepared boot or ready warm image. Same repository requirements, acceptance oracle and modern landing rules. May discover normal repository entrances; preparation it performs is charged | P/N: whole prepared package |
| G — equally prepared native | Same prepared context, observer, warm test image and probes, structured results, prewarm, generic affected ordering, review/landing consumer, toolchain and resource reservation as P. Native writes; can generate/consume equivalent generic evidence | G/N: platform effect; P/G: Surgeon increment |
| P — prepared with Surgeon | Everything in G plus eligible Surgeon operations/evidence. Native remains available and is the default for ordinary edits | Surgeon’s increment must beat G; platform benefit alone is not an editor win |

The benchmark manipulation explicitly permits native execution in N/G even where a house routing plate would normally prefer Surgeon. P’s three eligible cases must be optional, with the fastest-safe-completion instruction and no demand to suppress independent checking. Record uptake and non-consideration. If a mandated entrance is studied instead, label it mandated and add a separately registered optional cohort before claiming voluntary adoption. Give N/G the same task facts, required counts and supplied mapping; only prepared equipment differs.

**Confirmatory minimum: four strata = MVR × two caller families and curtaincall-cfp × the same two caller families.** Pin exact caller model builds/configuration and runner adapters (Codex/Sol and Claude/Opus seats represented by retained evidence); do not conflate a model, adapter and schema difference. Each stratum has six identical N anchor-floor requests, then six matched N/G/P triples. Total **24 floors + 72 comparison requests = 96 initial requests**. Internal retries add attempts, not sample size. One-repository/one-caller pilot: 6 floors + 18 comparison requests = 24; it cannot establish cross-repo/caller performance. The old two-arm four-stratum minimum is 72 initial requests, not 96; the extra 24 G requests price the attribution question.

| Slot in each stratum | Required task class | Frozen content rule |
|---|---|---|
| 1 | Ordinary known-site edit | One identified, small native patch; no discovery-dependent claim. Preselected anchor for the six identical N floor runs |
| 2 | Second ordinary known-site edit | Distinct task, same ordinary-edit class; no artificial fan-out to favor the tool |
| 3 | Behavior change | Retained percent-helper tasks: MVR `coverage-percent`, curtaincall `fmt-percent`, exact protected assertion overlay and full gate; details below |
| 4 | Eligible alias migration | MVR anchor is retained JSON centralization, 21 live sites/9 callers, source helper excluded; other repo uses a witnessed eligible alias shape |
| 5 | Second eligible migration/refactoring | Different supported alias shape with its own source/scope and protected prose; no unsupported extraction admitted by analogy |
| 6 | Third eligible migration/refactoring | Curtaincall uses the exact admitted Cell C views split: base d9205abc, 141 owners, 20 destinations, 87 static sites/five callers, frozen complete mapping/policies. MVR uses a third supported alias case |

Use the same six task bytes for both caller families in a repository. The retained JSON task is `/var/tmp/forge/row2-pilot/task.md`, base `6dcdbc9db67a91179b5940a16c16b760583f2603`: migrate `clojure.data.json/write-str` to `marvin-voice-remote.json/write`, `src` excluding `src/marvin_voice_remote/json.clj`, alias preference `[json mjson m-json json-policy]`, preserve protected content and minimal footprint. Cell C’s mapping is retained at `R/docs/observations/2026-09-07-plan-2/cellC/manifest.edn`, with its result and admission at `R/docs/observations/2026-09-07-plan-2-cellC-result.md` and `O/2026-09-08-row1-split-ledger.md`. Freeze the **complete executable request**, not merely an abbreviated manifest. **E3/E4’s 25-owner exports/calendar partition is Cell B/B07, not this admitted Cell C case.**

Slot 3 uses the retained behavior requests `/var/tmp/forge/coldstart/tasks/mvr-1.md` (base `94393708b6312c4de114f4ebf31dc82313e2914e`) and `/var/tmp/forge/coldstart/tasks/cc-1.md` (base `8aec4c93c50d61266fe9de79e889a2fd919f8cef`). Implement `marvin-voice-remote.reducer.echo-guard/coverage-percent` or `cfp-scheduler-killer.views.format/fmt-percent`, respectively, with protected tests already present. Required cases are 0→"0%", .5→"50%", 1→"100%", .333→"33%", .666→"67%", nil→nil, −.2→"0%", 1.4→"100%". Retain the task's focused namespace verify and `bin/kaocha unit` full gate. The actual assertion overlay, source seed, and qualified intended assertion-red/golden-green witness must also be registered; the task prose's historical QUALIFIED line alone is insufficient. These later bases are **per-task** bases, not Cell C or row-2 migration bases. Both paired controls receive exactly the same overlay. The task restricts caller commits/pushes; the independent runner performs benchmark sealing/publication under this protocol.

This fixes the portfolio composition, two migration anchors and two behavior-task anchors; it does not fabricate complete task bytes for the remaining ordinary/alias cases. The retained consult did not supply the remaining ordinary edits or additional alias cases. Before launch the runner MUST fill a task registry with six concrete cases per repository: task bytes/hash, base commit/tree, authorized paths, protected bytes/policies, expected outcome and negative cases, exact oracle/profile/recipe hashes, supplied mapping and preparation classification. Empty entries refuse `portfolio-unfrozen`. This mechanical instantiation is required preregistration, not permission to select cases after results. No new cases or arms were created in item 0.

Randomly assign all six permutations of N/G/P once per stratum to the six cases; retain seed, algorithm and assignment before the first floor. Fresh sessions and review contexts, blinded to arm/order where feasible, prevent a reviewer recognizing the previous solution. Run on one timing-qualified host sequentially through terminal landing, or use genuinely isolated equal reservations with a registered contention design. Ordinary load thresholds do not prove isolation. Apply the same host and application execution location to every arm; a remote anvil2 observer must not silently move only one arm’s execution to a slower CPU.

**Landing destination:** runner-owned isolated benchmark remote (a dedicated local/bare remote is acceptable if registered as such), no public remote write permission, refs `refs/heads/bench/frame7/<cohort>/<repo>/<caller>/<slot>/<arm>` and analogous floor refs. Each request owns one ref from its frozen base; retries publish only through that request’s consumer. The observer independently resolves the fully qualified remote ref and commit/tree; record remote identity and object format. Never public `main`, never `MCP/main`, never a production working ref. A local-only benchmark measures that registered transport and does not establish public-network landing latency. Block ref/remote substitutions mechanically, including default-push behavior.

Use the original **20-minute whole-request cap**. The 96-request serial ceiling is 32 hours before reset/setup/scoring; expected execution was forecast at 12–20 hours if requests take 7–12 minutes, plus about half a day scoring. No automatic timeout extension, host switch or individual-arm replacement. All preflight refusals after release are charged. Qualification work before the study is separately priced, never hidden in a speedup.

## 4. Item 1: repository-derived obligations and admissible evidence

The first slice is **same-candidate, exact-check consumption**. It decides which already-required check can be discharged by executed evidence. It does not derive omissions from a namespace graph, grant general cross-candidate reuse, turn a probe into a gate, or transfer the transform author’s authority to approve its own work. Start with one whole existing check; whole-namespace fixtures, ordering and aggregate isolation budgets make arbitrary Var-level splicing a separate qualification.

Define an obligation as `(id, authority-policy, candidate, required-inputs, runtime-contract, command/check-contract, selected-test-identities, scope, exclusions, lifecycle/freshness, result-predicate)`. The consumer derives the obligation set independently from the approved repository policy and actual candidate; it then matches receipt executions to it. **The producer’s list is never the expected list.** A candidate that edits the gate, inventory or exclusions must have an independently approved policy change; it cannot erase its own obligations merely by changing the manifest. Compare the frozen base policy and approved intent with candidate membership. Missing, duplicate and unexpected obligations or unexplained test deletions refuse.

### Exact R landing-gate inventory

At the pinned R snapshot, `make test` delegates to `landing-gate`; the authoritative ordered `gate-stage-manifest` in `test/clj_surgeon/battery_parallel_runner.clj` is:

`admit-transaction-recovery-battery → battery-fresh → {alias-migration-test, mcp-test, test-bb} → repository-hygiene → intent-audit`.

The braces denote a coordinated pool, with the global fast-before-integration barrier, not a license to reorder fixtures arbitrarily. `make print-gate-stages` is the public projection. Item 0 read its source and retained exact stdout; it did not run Make. The seven-target set is necessary but not sufficient. Resolve the following nested obligations independently; retain the expanded names and recipe/input hashes in the consumer’s obligation manifest.

| ID / obligation | Independently derived exact selection, runtime and inputs | Evidence that can discharge it |
|---|---|---|
| R1 Recovery | `make admit-transaction-recovery-battery`: `java -cp <clojure -Spath -A:clj-surgeon/mcp-test> clojure.main test/admit_transaction_recovery_battery.clj`. Required before its `admit-patch-test` consumer. Pin script, aliases/classpath and inputs | Executed cold recovery receipt, all required arms/results and exit 0, same input/candidate binding; mere existence of `target/admit-transaction-recovery-battery-receipt.edn` is insufficient |
| R2 Freshness | `bb test/clj_surgeon/battery_ledger.clj check`, from `make battery-fresh`, on actual landing candidate/history and current clock. Newest ledger entry must be readable/passing, at most 26 h old and at most 30 counted commits behind, and an ancestor. Preserve the ledger’s audited archive-only counting rule and raw/counted distance | Fresh execution of the repository freshness predicate with input ledger hash, observed time, ancestry/distance audit and exit 0. A historical passing battery is the policy’s evidence, not proof that all battery tests just ran on this candidate |
| R3 Alias | `suite-namespaces "alias"` = **clj-surgeon.mcp-alias-migration-test** and **clj-surgeon.receipt-artifacts-boundary-test**, in cold JVMs, whole namespaces with fixtures/hooks. Must run at every landing; battery freshness cannot substitute | Complete current-candidate alias suite and child evidence, exactly those namespaces and all required loaded test identities, exits 0, no errors/failures/skipped preconditions/isolation/leak violations |
| R4 MCP | Sorted `lane-manifest` members in `:fast` followed by `:integration`: **55 + 7 = 62 namespaces** at this snapshot, plus `mcp-test-checks` below. Cold JVM workers use `clojure -J-Xms64m -J-Xmx512m -M:clj-surgeon/test-deps -m clj-surgeon.mcp-test-runner --emit-edn <output> --ns <selected...>` | Complete current-candidate suite/children and shell-check evidence. Preserve fast barrier, whole-namespace hooks, loaded expected/executed Var census and union isolation/time budgets; a pool exit alone is insufficient |
| R5 BB | Exact **49 namespace** literal in `test/run_all.clj` (Appendix B). Actual children use `bb -Xmx512m test/run_all.clj --emit-edn <output> --ns <selected...>`; JVM coordinator is not the BB execution runtime | Full BB namespace/Var coverage, counters, each child exit and BB temp-leak guard. JVM tests of corresponding source do not discharge BB compatibility |
| R6 Hygiene | `sh test/repository_hygiene_gate.sh`, including actual index, ignore rules, paths and file modes it inspects | Exit 0 and execution/input evidence at final candidate lifecycle; earlier pre-cleanup status does not certify later workspace hygiene |
| R7 Intent audit | `bb --classpath src:test -e` requiring `clj-surgeon.mcp-intent-contract` and invoking `audit-current-repository`, with process exit reflecting `:ok`. Inputs include registered intent and code/test annotations | Exact audit result `:ok true`, `:violations []`, exit 0 on this candidate and independently derived registry; not a quoted historical “audit-ok” |
| R8 Coverage/policy integrity | `lane-manifest`, declared exclusions, `test/run_all.clj`, discovered `*_test.clj[c]` namespaces, per-test lane metadata, and named `test/clj_surgeon/deftest_census.edn`. Reconcile names, not just totals | Independent expected manifest plus per-namespace executed identities/fixture semantics; omissions, duplicates, renamed same-count substitutions and unexplained census deletion refuse |
| R9 Task acceptance/review | Frozen task behavior, scope/preservation oracle and required independent review, including held-out tests. These are additional to R1–R8 when the task requires them | Current candidate oracle passes and independent review verdict addressing all required decisions; tool commit/body hashes are not review |
| R10 Publication freshness | Final immutable candidate identity, policy/toolchain/input binding, proof closure, no intervening mutation, destination ref authorization; reevaluate temporal freshness when consuming/publishing | Consumer’s independently derived accepted record followed by observer’s destination-ref evidence. Producer `next_action=none`/`terminal_response` only describes its operation |

`mcp-test-checks` includes **all** of: `mcp-operation-oracle` (SWI-Prolog `test/mcp_operation_contract_oracle.pl`); `performance-regression-sentinel-intent-test`; Python unittest discoveries for `test_gate_slot.py`, `test_namespace_split_papercut_oracle.py`, `test_require_change_oracle.py`, and `test_cell_b_oracle.py`; and Make targets `repository-hygiene-self-test`, `txn-kernel-warning-check`, `mcp-heap-config-self-test`, `tmp-leak-ratchet-self-test`, `clj-kondo-admission-path-self-test`, `analyzer-contract-target-self-test`, `cclsp-start-self-test`, `cclsp-client-audit-self-test`. This is a specification of the pinned recipe, not an instruction to call retired MCP services. Resolve and pin those targets’ transitive recipes too. A changed/unknown recipe refuses consumption and executes the approved required gate.

The `:battery` lane has **36 named namespaces** at this checkout; its members and cadence are distinct from the 62 MCP namespaces. Five explicit JVM exclusions redirect to actual runners: `clj-surgeon.analyzer-contract-test` → `make analyzer-contract-test`; `clj-surgeon.memory.journal-green-test` and `clj-surgeon.memory.oom-reproduction-test` → `make memory-red-kernel`; `clj-surgeon.worktree-lifecycle-prune-test` → `make worktree-lifecycle-test`; `clj-surgeon.worktree-lifecycle-recovery-test` → `make worktree-lifecycle-recovery-test`. Verify concrete redirected membership, not just existence of a target name. A declared cadence/exclusion does not claim execution in this landing; if user/task policy requires one of these checks now, append it as an obligation and require its evidence. Do not silently promote `make test` into `make test-full`, or silently shrink a user-required full suite to the landing subset.

Historical gate totals **164/3,495 alias, 898/11,197 MCP, 891/7,888 BB** are sanity checks for the pinned snapshot, not authorities replacing names. R’s source digest covers `Makefile`, `deps.edn`, `bb.edn`, and regular files beneath `src`, `test`, `resources`, `docs/intent`. It does **not** cover every possible proof input: boot/skill prose, `bin`, external tools, git state and freshness ledgers can matter. The item 1 manifest must include all actual declared inputs; exact Git tree plus observed worktree/index, relevant untracked/ignored inputs, external file digests and environment is the conservative initial boundary. Unknown influence means run the complete required check, not “unaffected.”

### Runtime, identity and input binding

Bind every accepted execution to: repository/workspace identity; candidate commit **and** Git tree; actual worktree/index/input snapshot (not HEAD alone); runner/check code and recipe hashes; resolved dependencies and classpath; test configuration, aliases, resources/fixtures/data and seeds; relevant environment and runtime flags; loaded namespace/Var identities; output digest and terminal exit. Use a versioned sorted path/type/mode/byte-digest manifest, with explicit exclusions and no paths silently omitted. Include generated inputs and any external service fixture contract. Reject unexplained drift before/after execution and before consumption. Same source digest with a different Git history does not establish freshness equivalence.

Record actual executable identity/version and relevant flags for Java/JVM vendor/runtime, Clojure runtime and CLI, Babashka, the paved `~/bin/clj-kondo`, Python, Git, Make, shell and SWI-Prolog, plus Node/Kaocha when the specimen recipe uses them. Include plugin pins, configuration and resolved classpath hashes. Store argv as a vector and environment as structured, policy-selected data; secret-valued inputs use protected identity witnesses, not secret text in receipts. `Picked up JAVA_TOOL_OPTIONS...` is **not** a Java version. Capture stderr/stdout and parsed version separately, require successful commands, and refuse missing or ambiguous identity. Runtime process identity needs start/boot identity as well as PID; PID reuse is not equivalence. A warm image needs independently witnessed loaded-candidate identity, not just on-disk equality.

Only qualified custody/provenance can discharge an obligation. A hash authenticates bytes relative to a trusted expected hash; it does not prove a claimed execution occurred. A signer of producer-supplied assertions has not observed execution. Repeated receipt reads do not improve its authority. The same verification adapter must accept equivalent native-produced evidence under the same rules.

### Specimen/task oracles are separate from R’s seven-stage gate

Do not run R’s seven stages in MVR/curtaincall and call them those repositories’ tests. Derive each specimen’s landing obligations from its registered base/candidate recipes and task. Retained anchors specify:

| Artifact / specimen | Required substantive checks, beyond a rewrite receipt |
|---|---|
| MVR row-2 task: `/var/tmp/forge/row2/oracle/o1_load_suite.sh` and `/var/tmp/forge/row2/oracle2/run_oracles_v2.sh` | Cold `clojure -M -e "(require 'marvin-voice-remote.core)"`; `make server-test-run`; `make runtests-once`, all exit 0. O2 alias-neutral structural/golden match, O3 kondo delta, O4 protected prose/collision, O5 papercuts; untracked-path check at the frozen submission boundary. Full retained suite was 579 tests / 7,833 assertions. Explicitly choose and freeze the raw O1–O5 oracle plus any authorized classification ruling; no after-the-fact waiver |
| Exact Cell C views split: `R/docs/observations/2026-09-07-plan-2/cellC/manifest.edn`, `/var/tmp/forge/plan2/cellC/D1-request.edn`, Cell C result, and row-1 ledger | Exact complete request: `promotion_policy=promote-required`, `source_retirement=delete`, roots `src,test`, profile `split-unit`. Required Cell C owner oracle (141 owners exactly once), views source retired, architecture guard (historical 7/7), papercut oracle and full cold `bin/kaocha unit --fail-fast`, plus task-required repository checks and substantive review. Retained cold profile `/var/tmp/forge/row1-cohort/replication/runs/C3/profile.edn` explicitly executes `cc-oracle.py`, `papercut-oracle.py` and that Kaocha command. Rebind only registered owned paths and freeze profile/check bytes before launch. No transfer of Cell B exports/calendar profile or oracle |
| Historical E3/E4, for replay/qualification only: `/var/tmp/forge/plan2/cellC/b07/frozen/cell_b_oracle.sh` with `cell_b_preservation.clj` and `cell_b_lint.py` | Structural A1–A7: 25 owners exactly once, no source leftovers/facade/duplicates, 43 moved external references across 9 namespaces, retained references, signatures/privacy. A8 exact-base lint delta; B07 preservation. Two **fresh processes**, destination-first and source-first, production `clojure -M` classpath, runtime interns and caller loads. Eight named focused tests below, public-widgets integration namespace, `make runtests-once`, `bin/kaocha unit --fail-fast`, and outside-footprint preservation |

E3/E4’s eight focused Vars are `cfp-scheduler-killer.exports-test/{ics-test, ics-escaping-test, issued-invite-uid-survives-title-amendment-test, unroomed-session-calendar-uses-event-location, cfp-deadline-ics-amends-and-never-duplicates-test}`, `cfp-scheduler-killer.schedule-test/{exports-reflect-placement-test, published-session-removal-emits-a-snapshot-cancellation-test}`, and `cfp-scheduler-killer.comms-test/ics-attachment-matches-the-feed-test`. Integration is `cfp-scheduler-killer.public-widgets-test`. The retained oracle pins `init.defaultBranch=main` **only in the test command’s fixture environment**; that setting does not authorize a benchmark publication to main. Full `runtests-once` includes Prolog, Node, worktree fixture and Kaocha checks. These non-Clojure dependencies explain why a require graph cannot authorize full omission.

### What a warm probe can never discharge

A warm namespace/Var probe cannot discharge a **cold process startup/classpath/load-order/clean-intern obligation**, JVM-versus-BB compatibility, a full suite or namespace beyond its recorded selection, fixture/hook/union isolation not actually executed, required external shell/Prolog/Node checks, test-policy/census integrity, current freshness, final post-cleanup workspace state, independent review, custody, or publication. `coverage=complete` means complete **within its recorded scope**. A 1/1 probe, successful warm reload, `loaded []`, or commit result never becomes full-gate proof.

For Surgeon split evidence, `state=committed` proves the write. A `committed-probe-only` or `verification_complete=false` receipt is incomplete. The original background receipt remains pending; require its separate closure bound to the original receipt digest and candidate, `verification_complete=true`, empty `proof_pending`, and every independently required named successful check. Missing/dead/stale/failed closure cannot pass. Only the registered `captured-reference-analysis` and `candidate-lint-delta` checks permit a nonzero exit with the required baseline map and passing delta predicate; other nonzero checks refuse even if labelled passed. A baseline exception cannot excuse a failed required test. E3/E4’s no-op profile was `{:verification-profiles {"b07-cell-b" {:commands [["/bin/true"]]}}}`; its successful process is not substantive cold proof.

### Typed skips and scope

| Type | Exact meaning | Consumer treatment |
|---|---|---|
| `declared-skip` | Named skip-meta/skip-ID exclusion in the independently approved configuration, with exact rule/test IDs and provenance | Report as excluded, never executed. May satisfy only an obligation whose policy expressly permits that exclusion. R’s landing required-suite skipped-precondition policy is zero; declaration does not waive it |
| `focus-omission` | Test or suite omitted by focus/selection, including adapter-injected suite skips | Does not discharge a full-scope obligation. May be outside a narrowly requested probe’s scope. Retain `:focus-rules`, `:totals :skipped-by-focus`, and identity lists |
| `unexecuted-test` | Discovered/required test explained by neither approved declared exclusion nor focus; e.g. fail-fast, aborted work, missing requested Var, failure before execution | Required coverage incomplete; fail closed. Pending tests likewise cannot manufacture completion |
| `load-error` | Target was readable as a request but failed to load; distinct from unreadable evidence | Terminal test/load refusal, even if counters are zero |
| `protocol-error` | Malformed, unsupported, truncated, oversized or unreadable receipt | Refuse consumption; never treat as a test success |
| `legacy-scope-unknown` | Missing scope/basis/focus fields, including old combined skipped counts | Preserve absence; no invented zeros, full scope, or new meaning |

For the Kaocha adapter, preserve `closure_basis="discovered-namespaces-v1"`: total counts discovered namespace groups **per suite**, including focus-omitted/empty discovered groups; selected counts evaluated groups and groups covered by declared exclusions. Do not deduplicate namespace names across suites or rebase a namespace to one focused Var. A suite skipped before discovery contributes no invented leaf/group count. Declared exclusions take precedence over focus, with no double counting. EDN fields include `:scope`, `:closure-basis`, `:declared-skips`, `:skip-rules`, `:focus-rules`, `:unexecuted-tests` and separate focus totals. New additive scope/basis/focus fields are validated together; legacy absence stays unknown. Full FOLLOW requires gate/full scope, exact identity, passing selected/parent outcome, complete coverage and equal closure counts; no legacy or current probe passes a full token. See `O/2026-09-08-kaocha-plugins/astra-kaocha-defects-2-report.md`.

### Fail-closed decision and same-candidate limit

Return one typed decision: **`consume`**, **`run-required`**, **`reject-candidate`**, or **`unverified`**, with obligation IDs and reasons. `consume` means named obligations were discharged, not permission to land. Unknown/unsupported evidence yields `run-required` with reasons such as `receipt-unparseable`, `schema-unsupported`, `reader-unavailable`, `stage-set-mismatch`, `stage-duplicate`, `candidate-mismatch`, `inputs-mismatch`, `toolchain-mismatch`, `scope-incomplete`, `runtime-mismatch`, `proof-pending`, `custody-unverified`, `evidence-missing`, `evidence-stale`. A known failed check rejects the current acceptance attempt and remains recorded. A policy/clock/custody uncertainty that prevents qualification yields `unverified`; it cannot be laundered through a green execution. If safe full execution closes the missing proof, the request may continue inside its original clock.

Same-candidate matching is strict in item 1. No general acceptance of a receipt from an earlier commit/tree, even if source digest matches. Existing repository freshness is an explicitly different policy, not arbitrary cross-candidate execution reuse. The historical phase-A-to-landing battery-ledger/walls delta is not silently grandfathered into this first slice. If battery output or resealing changes the candidate tree, rebind/reexecute required proof on the final candidate; optimizing that requires a separately specified input-equivalence exception and tests. In particular the legacy regexp `^docs/observations/battery-ledger\.edn$|walls` is not an acceptable new delta allowlist. Any later exception must enumerate exact paths, byte/mode rules, owned output producers and proof-input exclusions, and independently prove them.

## 5. Repaired receipt shape, migration and exact-byte replay contract

There are **two layers**. The repository producer’s genuine `target/landing-gate-prewarm.edn` is typed EDN: `:stages` is a vector of maps with string `:target`, integer `:exit` and integer `:wall-ms`; nested `:suites` carry execution details, and `[:pool :shell-checks :exit]` covers the shell/oracle job. `:prewarm? true :landing? false` is valid partial evidence. Full gate has the converse flags. Prewarm plus a valid current freshness obligation must equal the independently derived full seven-stage contract. The raw prewarm must not itself authorize landing.

The **outer ship envelope**, `O/2026-09-09-gate-lanes/gates-prewarm-3ea3803e.edn`, is 3,014 bytes, SHA-256 `0dcbe2724c37724a52223544ddbe2e28c3320763ab90a867c50fdb4bcb7a2e12`. Offline `clojure.edn/read-string` via Babashka reproduced `Unsupported escape character: \.`. Line 9 contains the invalid EDN string escape; line 12 has unescaped nested quotes. Its Java value is the JAVA_TOOL_OPTIONS banner. This does **not** establish that the historical sed consumer failed to land; it establishes that these exact bytes are not a general typed EDN interface. Preserve them unchanged.

### Version 2 normative transport

Use an actual EDN data writer (`pr-str`/equivalent), never shell interpolation or sed field extraction. UTF-8, one map plus whitespace then EOF; no trailing form, reader evaluation, unknown tags, duplicate keys/IDs, non-finite numeric values, guessed booleans or numeric strings. Use strict version-dispatched schema validation; unknown authority-affecting fields refuse. Optional non-authoritative additions go in a named `:extensions` map. Bound envelope to 65,536 bytes; larger detailed evidence is content-addressed by path/digest/byte length, read under independently registered roots with its own registered size bound. Do not truncate proof to fit. The consumer must parse the entire object and require EOF; a successful first `read-string` alone does not establish that.

The **required v2 envelope fields** are shown below as a syntactically valid, deliberately incomplete migration record. `nil` required identity/provenance fields and empty executions make its expected decision `run-required`, never `consume`. This is the repaired transport shape, not fabricated execution of the historical request. Fresh producers fill these fields from real observations and use `:state :complete` only when their declared evidence scope is complete. The consumer independently computes acceptance regardless of that value.

```edn
{:receipt-version 2
 :kind :landing-evidence
 :state :incomplete
 :request-id nil
 :attempt-id nil
 :producer {:kind :ship :build-sha256 nil :custody :unverified
            :observation-id nil}
 :candidate {:commit "a51dd39e955972ddac1d417d40e73e05f30637c1"
             :tree "ad473e5d4600fed158e7bbe453571debadb2a20d"
             :workspace-snapshot-sha256 nil :inputs-manifest nil}
 :obligations {:policy-sha256 nil :manifest-sha256 nil}
 :scope {:kind :prewarm :requested [] :exclusions []}
 :authority {:landing? false :prewarm? true :debug? false}
 :toolchain {:java {:status :unknown :executable-sha256 nil :version nil
                    :argv ["java" "-version"]
                    :diagnostic "Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge"}
             :manifest nil}
 :stages []
 :executions []
 :proof {:complete? false :pending [:identity :inputs :toolchain :execution :custody]
         :closure nil}
 :events {:started nil :completed nil :clock-domain nil}
 :legacy {:path "gates-prewarm-3ea3803e.edn"
          :sha256 "0dcbe2724c37724a52223544ddbe2e28c3320763ab90a867c50fdb4bcb7a2e12"
          :bytes 3014
          :phase-a-tree "ea189742e43eb8f0ba70e91cc372984b09d372b8"
          :reported-delta-paths ["docs/observations/battery-ledger.edn"
                                 "docs/observations/battery-namespace-walls.edn"]
          :uninterpreted-delta-pattern "^docs/observations/battery-ledger\\.edn$|walls"
          :audit-argv ["clojure" "-M" "-e"
                       "(require 'clj-surgeon.mcp-intent-contract)(prn (select-keys (clj-surgeon.mcp-intent-contract/audit-current-repository) [:ok :violations]))"]}
 :extensions {}}
```

The legacy candidate commit and claimed final tree above are intentionally preserved as claims, **not asserted to match**. The sealed candidate’s reviewed tree was `ea189742…`; the envelope’s final tree is `ad473e5d…`. That mismatch needs independent binding and the historical delta policy; merely repairing quotes cannot repair authority. The audit argv preserves legacy invocation data; fresh R7 uses the repository’s current exact recipe, not that legacy command.

Each populated `:stages` entry is `{:target <string> :exit <integer> :wall-ms <nonnegative-integer> :execution-ids [<unique-ids>...]}`. Each execution has `:id`, `:obligation-ids`, `:candidate`, `:inputs-manifest`, `:check-contract-sha256`, `:runtime`, `:argv`, `:environment-manifest`, `:scope`, `:expected-tests`, `:executed-tests`, `:declared-skips`, `:focus-omissions`, `:unexecuted-tests`, `:result`, `:events`, and `:evidence`. Manifest/evidence references are `{:path <string> :sha256 <64-lowercase-hex> :bytes <nonnegative-integer>}`. Tests are fully qualified identities with suite and fixture/hook context; supported counted/namespace-hook exceptions require their exact qualified contract. `:result` records integer exit, failures, errors, pending, isolation, leaks and skipped preconditions; absence is unknown, not zero. `:runtime` binds kind (:jvm/:bb/:shell/...), cold/warm mode, executable/toolchain and process/loaded-image identity. Exact schemas for these values must be implemented and frozen before emission; incomplete v2 records remain readable but cannot discharge a missing field. No field named `expected` in a producer is the consumer’s authority.

Migration is coordinated: (1) preserve/hash raw v1 bytes; (2) implement a strict v2 reader and offline decisions, with old envelopes yielding typed legacy/unparseable rerun; (3) change the writer to generate real typed v2 data, not convert self-reported strings into attested facts; (4) qualify the paired writer/consumer against **retained producer bytes** and negative cases; (5) only then enable live consumption on an authorized slice after custody qualifies. No parser fallback to sed, command targets, a flat vector, or success summaries after finding a refused receipt. Reader absence is `reader-unavailable` and full required execution. Legacy historical records are not rewritten; a migration side record cites their digest and marks unproved claims. Supporting the genuine typed old repository receipt is a separate explicit adapter; it cannot infer absent toolchain/custody/full coverage.

### Required replay corpus and expected decisions

Appendix A fixes lengths/hashes for raw inputs. Item 1 must use these bytes, not freshly generated “realistic” records that reproduce its own assumptions. Synthetic mutations are labelled and derived from a named hashed parent. A parser-shape success is separate from consumption eligibility.

| Input / mutation family | Required result |
|---|---|
| Exact 3,014-byte malformed outer envelope | Parse refusal; no quoted-string/sed rescue, no consumption. Independently isolate both the invalid escape and nested-quote defects in synthetic derivatives |
| Actual `/var/tmp/forge/gate-lanes/r3/prewarm-receipt.edn` | Typed parse succeeds, exact six-stage vector/suites/pool recognized. Partial prewarm only; as retained historical evidence it cannot authorize a new candidate |
| Actual `/var/tmp/forge/gate-lanes/r3/full-receipt.edn` | Typed parse succeeds, seven stages recognized; fails a prewarm-only adapter’s authority check. Full format alone does not establish fresh v2 custody/toolchain/candidate equivalence |
| `/var/tmp/forge/ship-v3.6/proof/real-prewarm.edn` | 857-byte **derived shape fixture**, not a retained execution of prewarm. Recognize stage maps; missing suites/pool/execution provenance prevents complete consumption. Never call it raw full proof |
| Old flat-vector strings, wrong receipt path, full receipt at prewarm path, nonzero/null/string stage exit, missing/duplicate/extra target, missing reader | Typed refusal/run-required; no guessing from invocation names. Missing alias or intent audit must fail despite self-consistent producer hash and aggregate green |
| Right seven names but missing BB namespace, renamed same-count test, failed/missing child, shell exit nonzero, skipped recovery precondition, wrong union isolation budget | Incomplete/failed coverage; no consume. Preserve fail-fast outcomes and original failed runs |
| Correct source digest with stale Git tree, dirty worktree, changed deps/config/resource/skill, changed seed, wrong Java/banner-only identity, changed BB/runtime, stale process PID, unobserved warm loaded state | Identity/input/runtime refusal. A graph’s “unaffected” result or matching test count never overrides it |
| Changed candidate after cleanup/review/proof, wrong closure hash, original pending receipt with successful unrelated closure, dead worker, debug serial, prototype forged signature | Reject binding/authority; no landing. A valid signature over an unobserved claim does not discharge execution |
| Exact E3 Y1 and E4 Z4 receipts; E4 Z2 helper and chronological transcript | Decode declared fact tables/encodings, recognize pending/no-load state and captured-root exclusions. Receipt-reading helper is decoding; pre-cleanup status cannot prove post-cleanup state |
| Kaocha legacy and repaired RUN-DONE/STATUS lines retained verbatim in `O/2026-09-08-kaocha-plugins/astra-kaocha-defects-2-report.md` | Legacy missing scope unknown; full-token probe refusal even at 1/1; explicit full gate versus selected namespace/Var; declared/focus/unexecuted separation; load-error distinct from protocol-error; parent-outcome/identity checks retained |
| Missing raw Kaocha receipt referenced by a valid RUN-DONE line | `evidence-missing`, not consumption from its digest alone. The nine paths in `/var/tmp/forge/kaocha-defects-2/evidence/receipts-to-verify.edn` are missing at this audit; the report’s exact stream lines remain available. They test the stream parser, not the missing EDN bytes |
| Strict transport: truncation, trailing second form, duplicate keys, unknown tag/version, escaped slash/quote/newline, Unicode, corrupt evidence digest/length, changed path/symlink, oversized reference | Deterministic strict parse/schema/integrity decisions; no partial extraction or arbitrary referenced-path read |
| Positive native and Surgeon equivalents | With independently supplied matching candidate/policy/input/runtime/custody facts, exact completed evidence discharges the same named obligation for either producer; absence of additional qualifying evidence leaves other obligations pending |

Qualification must prove **both** a consume path for an exact, complete controlled fixture and fail-closed paths for the retained/derived negatives. A corpus that only refuses proves no useful consumer. Record original bytes, fixture derivation, expected obligation manifest, expected decision/reason, actual decision, consumer build hash and pass/fail. Do not run new benchmark arms to qualify a pure evidence matcher.

## 6. Retained E3/E4/row-2 incompatibilities: runner regression ledger

These are **prospective Frame 7 incompatibilities**, not accusations that a historical study violated a protocol that did not yet exist. Preserve those studies’ original endpoints and failed gates. This ledger names the inspected locations that must not be copied into Frame 7 unchanged.

| Location | Retained behavior / incompatibility | Required Frame 7 rule or test |
|---|---|---|
| `/var/tmp/forge/row5-adopt-{3,4}/setup-E{3,4}.sh`, `task-E{3,4}.md`, per-arm stopwatch; `/var/tmp/forge/dogfood-stamp.sh` | Worktree, request binding, profile and task rendering before caller-owned orient-start. Caller invokes the first clock after receiving instructions | Runner prospective release before task access/setup; separate independently attested generic preparation; no inferred t0 |
| `/var/tmp/forge/row5-adopt-{3,4}/compute-E{3,4}.py`, `grade-E{3,4}.sh`, readout JSON and result reports | `primary = orient-start → candidate-complete`; required caller suites and independent oracle excluded from headline | Keep as historical candidate clock only. Compute final endpoints per request before medians; charge caller and worker proof |
| E3/E4 `watch-E{3,4}.sh` | Automated five-second polling of caller-written `done`, watcher default cap 3,000 s; protocol arm cap 1,800 s | Automation is useful, but qualify runner-observed submission/termination, include polling delay, and enforce one 1,200-second request cap independent of caller stamps |
| E3/E4 `grade-E{3,4}.sh` | Deletes leftover `.clj-surgeon.edn`, stages/captures candidate, then starts oracle; eventually destroys worktree/branch. No candidate publication | Preserve pre-cleanup state; lifecycle-bound final identity and charged cleanup; retain immutable candidate through cohort; no tL without destination observation |
| E3/E4 preregistrations §§3–4, 9–10 | Historical slot chain to stage-1 native; no fresh N/G, one Opus caller, supplied task-specific request/no-op profile; per-arm load/disk checks; next caller overlaps previous oracle | Cannot claim randomized complete-wall P/N or P/G, both callers, equal equipment or isolated capacity. Fresh matched triples and registered task access/setup are required |
| `/var/tmp/forge/row5/clj-surgeon-D-noop.edn`; E3/E4 receipts and frozen Cell B oracle | `/bin/true`, pending cold proof, E4 `load_status :not-run`; substantive cold suite/oracle elsewhere | Named substantive obligations and closure matching; successful no-op/commit cannot be acceptance |
| E4 prereg amendment/deviation 10 and `O/2026-09-09-row5-adopt-4.md` prediction table | Pointer brief vs E3 inline delivery; secondary “corrected” ≈137.5 s subtracts estimated 9.7 s from 147.2 | Identical task delivery or declared treatment; **no subtraction in primary**, even runner-caused overhead |
| E3/E4 `classify.py`, `answerable.py`, compute scripts | Window starts at split return and ends at done; spelling/topic proxy, hex-only digest trigger; E4 Base64 changes classification; conditional filtering can omit missing ratios/rows | Complete action census, unknowns retained, declared digest decoding, six slots always checked; no null-dropping admission |
| Same compute scripts / historical gates | Gate boolean based on median inspection ratio (E3 also comment rule), not a complete acceptance/custody/landing conjunction; ratios rounded before medians | Historical gates stand. New scorer must enforce all evidence/acceptance conditions and compute unrounded ratios |
| E3 result and E4 §8; `O/2026-09-09-row5-e4-dual-score.md` | Comment-report rule confuses praise/complaint; E4 demotes it prospectively. Legacy E4 0.675 still fails 0.50. Dual scoring: 13 covered, 1 silent, 1 decoder, motive agreement 9/15; six status actions outside nominated frame | Preserve failed verdicts; no post-hoc causal relabel as friction pass or complete census; disputed motive cannot become denominator |
| E4 Z1/Z4 cleanup/status and full Z4 receipt; Z2 `slice.py` | Receipt workspace_status precedes cleanup; `:footprint` excludes uncaptured paths; helper opens only receipt. Artifact prose sometimes treats all residuals as independent-witness demand | Test proposition, scope and observation time. Final status obligation remains; decode work is not tree inspection; motive inference is not observation |
| Row-2 `/var/tmp/forge/row2/wall2.sh`, pilot briefs/stamps | orient-start and last candidate-complete are caller stamps; primary oracle-complete later. Missing stamps refuse in old helper, but release is still not independent | Preserve no-invented-stamp rule; move authoritative timing to runner release and lifecycle events |
| `O/2026-09-08-row2-fresh-seat-pilot-2.md`, PB1–PB4 logs/grade2.sh | Operator-delayed grading: PB2 candidate 21:24:01.389 → load 21:32:32.254, **510.865 s** idle; reported primary contaminated. PB2 omitted mutation-end | Automatic oracle queue; idle remains charged if it occurs. Missing diagnostic stamp stays unknown (PB2 correctly declined a fake late stamp) |
| `/var/tmp/forge/row2-entrance/design/prereg-entrance.md`, `apparatus/grade-cell.sh`, E1–E4 logs | All four callers finish before any grading by design. E1 candidate 22:22:02.977 → load 22:33:26.992, **684.015 s**; primary became caller work only | No manual/deferred cross-cell acceptance outside primary. Give each request its automatic boundary and terminal endpoint |
| Row-2 `oracle2/run_oracles_v2.sh` | Actual O1 cold load and suites share the load-start/end interval; synthetic immediate suite-start/end stamps follow the already executed suites | Instrument real child spans or label aggregate O1; do not print near-zero suite cost from placeholder stamps |
| Row-2 `/var/tmp/forge/row2-pilot2/apparatus/grade2.sh`; entrance `apparatus/grade-cell.sh` | Removes `.codex`, restores schema-appended CLAUDE.md; entrance additionally resets base/restores Makefile/AGENTS/CLAUDE and removes entrance before grading | Predeclared external preparation or exact charged normalization; preserve both identities. Never accept a different tree while claiming proof for submitted bytes |
| PB1 untracked-check apparatus note, PB2–PB4 attestations, pilot-2 result | PB1 check after staging following wrong argument; parent brief attestation false due unreadable sealed template; boot-block “schema loaded” still needs ToolSearch and leaves dirty tell | Qualified same-boundary checks and delivery hashes; contradictory attestation is apparatus-invalid until resolved. Boot-schema text is not actual tool-list preload |
| `/var/tmp/forge/row2/classify3.py`, PB4 oracle/classification | Raw O2+O5 FAIL/ACCEPTED NO but registered semantic ruling accepted-with-minor; wrapper suppresses classifier failure (`|| true`). Classifier’s missing-field paths do not constitute a new fail-closed contract | Freeze exact acceptable ruling before launch, validate complete raw results and typed classification, charge classification before tA; no suppressed/missing result can pass |
| Row-2 entrance/readouts and stage-3 comparison | n=4 entrance, n=2 historical native, 27 s directed vs 89–115 s cold; all entrance callers supplied count so discovery branch unwitnessed | No portfolio claim or discovery admission from this evidence. Supplied-count execution and unknown-count discovery remain distinct |
| Row-2 pilot/entrance stop rules | Fifteen-minute caller cap and one delivery re-run allowance, not a twenty-minute whole request with all attempts | New whole-request cap/attempt accounting; no individual result replacement |
| All E3/E4/row-2 artifacts above | Accepted worktrees/patches, no independently witnessed benchmark publication, no equivalent prepared-native G, no complete prospective t0→tL ledger | **Historical Frame 7 L is unknown for every arm**. Do not backfill landed clocks or claim the new protocol has already run |

Appendix A uses actual retained paths. No uninspected code is certified by this table.

### Arithmetic witnesses from retained readouts

These values were recomputed offline from retained per-row endpoint components; they do not create t0 or tL.

| Cohort | Historical candidate median | Historical orient→caller-done median | Historical orient→oracle-complete median | Median caller-suite component | Median runner-oracle component |
|---|---:|---:|---:|---:|---:|
| E3 Y1–Y6 | 141.75 s | 252.0 s | 490.35 s | 109.9 s | 237.15 s |
| E4 Z1–Z6 | 147.2 s | 273.8 s | 514.5 s | 122.15 s | 237.55 s |

E4 historical slotwise candidate/native ratios yield **0.4906509217**, while `median(E4 candidate)/median(N candidate)` is a different statistic. The legacy inspection ratios `[1,.75,.60,.25,.75,.60]` yield `(0.60+0.75)/2 = **0.675**`; ratio of totals is 15/24=.625 and ratio of marginal medians is 3/4=.75. These are fixed scorer regression examples, not new speed claims. Rounding cannot select another answer.

## 7. Falsifiers and forecasts: print before the first arm

The runner prints this table, protocol hash, full task/equipment/ref registration hashes, actual model/configuration, randomization assignment, acceptance policy, clock/cap/statistic/uncertainty rules and launch eligibility before releasing **any floor or comparison request**. Forecasts are elapsed savings unless labelled ratio; positive saving means faster. They are hypotheses against the specified controls, not achieved results and not additive.

| Item / forecast from the consult | Pre-registered falsifier / stop |
|---|---|
| 0 Clock/obligation freeze: no direct speedup | Required setup, attempts or proof cannot be independently accounted for; primary needs subtraction; oracle triggered by discretionary operator action. Block clock qualification |
| 1 Consumer: central **0–30 s saved per eligible clean request**; plausible **10 s slower to 120 s faster** where duplicated execution is critical | No actual overlapping check contract; accepts missing stage/stale candidate/changed config/wrong runtime/probe-as-full; costs more than execution saved. Any false green rejects the consumer slice |
| 2 Recorder: central **0–20 s caller-wall saving**; elapsed delta **−30 to +10 s** (negative delta is faster); landing may save zero | Subject reaches observer authority via credentials; signing without observation; same caller work remains; transport/observation exceeds removed returns. Custody failure blocks live authority |
| 3 Review consumption: **120–240 s less review**, **0–30 s validation/projection overhead** on roughly 573 s move-heavy review; **90–240 s less landing wall** plausible on that schedule; near zero on small behavior fixes | Blind review misses binding, promotion, protected-comment, omitted-owner or load-order defects, issues unsupported GO, or still reconstructs whole diff. Reject unsafe projection; give native identical evidence access |
| 4 Conservative affected ordering: **0–30 s** on clean review-bound requests; **30–120 s** over multiple feedback/repair cycles | Resource/config/macro/fixture/non-source effect escapes, counts change but critical path does not, coordination/fallback erases saving. No omission authority from an unqualified graph |
| 5 Frame 7 whole package: **P/N ≈0.90**, plausible **0.75–1.10** | Equal acceptance, clock integrity, interval and two-SD screen do not clear N. Report loss/inconclusive as applicable |
| 5 Surgeon increment: **P/G ≈0.97**, plausible **0.85–1.10** | G captures the improvement or P does not clear G. Retain platform benefit; make no Surgeon-specific performance claim |

Additional benchmark stops: publication to an unregistered/public ref; task leakage or task-specific prework before release; missing frozen task/oracle bytes; loss of candidate/evidence custody; unequal uncontrolled resources; tool false green; acceptance based on absent test/closure; replacement of failed slots; post-hoc primary/classifier change. Preserve the event trail, stop affected launches and label validity separately from performance. Existing row-1 NO ADMISSION, E3/E4 misses and suspended fan-out remain unchanged by adoption of this protocol.

## 8. Item 0 result and implementation handoff

This report freezes the clocks, accounting, statistics, cohort structure, consumer obligations, versioned receipt contract, migration and falsifiers. It deliberately leaves task-specific registry instances and consumer/observer qualification as **required launch inputs**, not fabricated completed work. Item 1 can begin with the pure obligation matcher and exact-byte replay corpus. Its completion requires a complete positive consumption witness plus all negative cases, with evidence of what was actually discharged and what still runs. No live consumption is qualified by this document.

Verification performed here was read-only artifact/source inspection, offline endpoint arithmetic, SHA-256/length capture and an EDN parse of the actual malformed outer envelope and retained typed gate receipts. No experiment or live consumer was run. The missing Kaocha raw receipt files and the derived nature of ship v3.6’s 857-byte shape fixture are explicit limitations. They must stay explicit in item 1’s test receipts.

## Appendix A. Retained input bytes

Hashes identify read inputs, not qualified execution or current deployment. Detailed gate receipts exceed the proposed bounded envelope; they belong behind validated evidence references.

| Absolute path | Bytes | SHA-256 |
|---|---:|---|
| `/home/forge/src/clj-surgeon-records/docs/observations/2026-09-09-astra-squares.md` | 34053 | `168041da5a628e3e8f3c26007b46adcdd9b525cadf78167c27aa8a380be0da78` |
| `/home/forge/src/clj-surgeon-records/docs/observations/2026-09-09-gate-lanes/gates-prewarm-3ea3803e.edn` | 3014 | `0dcbe2724c37724a52223544ddbe2e28c3320763ab90a867c50fdb4bcb7a2e12` |
| `/var/tmp/forge/gate-lanes/r3/prewarm-receipt.edn` | 248285 | `a34694089167f0d638a531831d0675b32bdf99de8716e8dd48bcf6450d5a4714` |
| `/var/tmp/forge/gate-lanes/r3/full-receipt.edn` | 248381 | `011c3b8703b2babf990295266f3ddf35ef686ef36a8daacb3cc8584fb4cd1c8b` |
| `/var/tmp/forge/ship-v3.6/proof/real-prewarm.edn` | 857 | `ffa0981c77e132039103f8c207f6114e2e02dd5ddc879a0793a276b084f5f596` |
| `/var/tmp/forge/row5-adopt-3/E3-readout.json` | 20594 | `17553ce3aa90982c14226e01bd90938f408dcd6bbcec04774b87011a90573220` |
| `/var/tmp/forge/row5-adopt-4/E4-readout.json` | 23936 | `c2d42a3618c3430f07a28f1bac91ce65f72360b6fc0ac3fa85c36bad97d95086` |
| `/var/tmp/forge/row5-adopt-3/Y1/e3-receipt.edn` | 30924 | `fc1327029e28b8dd9a61dfee77d597d8e4cd13ec8059b2b4b9f362f7ceab3ab6` |
| `/var/tmp/forge/row5-adopt-4/Z4/e4-receipt.edn` | 33927 | `a5c9cef9c863887f1ea7a56f36efe4b69fa92d87ccf857949b9e4f29f71eaf89` |
| `/var/tmp/forge/row5-adopt-4/Z2/slice.py` | 894 | `fec8dae92362b471f38f0efab4173abebf796bee841c4ef040a098f83c9c6e0c` |
| `/var/tmp/forge/row5-adopt-4/transcripts.json` | 1022 | `71d3274c58fadfc55917d062807b87475277d64112f6327bbf27652ad0b811bd` |
| `/var/tmp/forge/row2-pilot/task.md` | 1502 | `ec4c2a9e24fad8fbc4f0733454f773df2d1b20c61129f2de0ff402d9b9c331a2` |
| `/var/tmp/forge/row2/oracle2/run_oracles_v2.sh` | 1869 | `0bc09aec2c9b9fa1606fc2db62467bfd45082f86df621355c88c702f6241733c` |
| `/var/tmp/forge/row2/classify3.py` | 5665 | `17c80f30ce701fa29265650c7daa316c00257153297a0d85e1bc906c63c16dd2` |
| `/var/tmp/forge/row2-pilot2/PB4/oracle-run.txt` | 9239 | `f7fa27401eee9ca161c431eeb2bc7e1b058761c6cfb7e950e5e28e9e6cee98f3` |
| `/var/tmp/forge/plan2/cellC/b07/frozen/cell_b_oracle.sh` | 18956 | `9a9f7a8c75cab8aeeb5987fc9458393f187681a2d19c694bd606408807d6e1c6` |
| `/var/tmp/forge/plan2/cellC/b07/frozen/cell_b_preservation.clj` | 7333 | `4ce21bca8769f59b73da1a86c158bd25eb4b18ea65fdd48c694079601ae703c7` |
| `/var/tmp/forge/plan2/cellC/b07/frozen/cell_b_lint.py` | 1485 | `8041e3f2bbce1389259677f99e6fc15cefa09c1e39399638fa208ccad45cf698` |
| `/home/forge/src/clj-surgeon-records/docs/observations/2026-09-08-kaocha-plugins/astra-kaocha-defects-2-report.md` | 23083 | `b25b374fd33adac296880a3baaa321e6bf5fb177fb280bddb80a72350a59d574` |
| `/var/tmp/forge/kaocha-defects-2/evidence/receipts-to-verify.edn` | 1137 | `a4ce32c2160a03d51449fca4b19a3da4399561082988ae18101c6a22459ee97f` |
| `/home/forge/src/clj-surgeon-astra-consult2/Makefile` | 74492 | `bb606694d2fa888ef097459ea696632676b903017e40e31925ffccd61ec3a5a6` |
| `/home/forge/src/clj-surgeon-astra-consult2/deps.edn` | 6592 | `831cb2510abb5b9a425a23edf159c3eadf74cad07ec660060d63254c1ab96e08` |
| `/home/forge/src/clj-surgeon-astra-consult2/bb.edn` | 88 | `276c9b3d18b9d564deda96f63b1f0e0d70a8d26923155c7dbecd6c11d51c2a00` |
| `/home/forge/src/clj-surgeon-astra-consult2/test/clj_surgeon/battery_parallel_runner.clj` | 60750 | `20698cc0da1db6121d50d2ffaa75639f5824d0d6945a011248b5b62f14baa398` |
| `/home/forge/src/clj-surgeon-astra-consult2/test/clj_surgeon/lane_manifest.clj` | 14637 | `e2565844891558fa8a768592398e2433f73bd59d751e36a8367e9f48e62fce6e` |
| `/home/forge/src/clj-surgeon-astra-consult2/test/run_all.clj` | 3664 | `07dfda13a17a22858b0e2f778972928c600a0ddb7320b8b7b52e75354ec19ebb` |
| `/home/forge/src/clj-surgeon-astra-consult2/test/clj_surgeon/deftest_census.edn` | 148584 | `872459ec1b7ce9e6d1767cbbf325b2c81db78fe181b2c229cc536cf152b6671f` |
| `/home/forge/src/clj-surgeon-astra-consult2/test/clj_surgeon/battery_ledger.clj` | 14513 | `172521fd06f76cab52f219042d31060e22ecc97a83c394515ce2298880dd9437` |
| `/home/forge/src/clj-surgeon-astra-consult2/docs/observations/2026-09-07-plan-2/cellC/manifest.edn` | 11208 | `1982c78be180554afdcae7f35c4c3905aaf9fed83cdd86f7b1edd1cfac132f93` |
| `/var/tmp/forge/coldstart/tasks/cc-1.md` | 1606 | `9ca73c2f75c41e4478b50b3e5877380089571bb3df024248653a62bda700b58a` |
| `/var/tmp/forge/coldstart/tasks/mvr-1.md` | 1658 | `87b1f0205a0bc2edded531d8e71d0d7796d87c187ee6cf1b3c0f36515766f701` |
| `/var/tmp/forge/plan2/cellC/D1-request.edn` | 5816 | `5e7e5052274a773a24005c62108fd831ca415314bc314a93d44d2563704e50ea` |
| `/var/tmp/forge/row1-cohort/replication/runs/C3/profile.edn` | 354 | `9c98a2f704d991d4c07315b0503ae0e92de9a91398210a99ba99d6cd64ed359d` |
| `/var/tmp/forge/row1-cohort/replication/cc-oracle.py` | 899 | `1657421b0ddffefcbf6f9070e8100ed807543e50ecff0691356307e6b83533ed` |
| `/var/tmp/forge/row1-cohort/replication/papercut-oracle.py` | 31631 | `f87dcd2242d2dd930ed789bb24b086b8f7b9944c9ac6ae7157c9d7c540407cf5` |

## Appendix B. Exact pinned namespace inventories

These names are independently read from R’s pinned source, not copied from the producer’s execution list. At consumer runtime expand the approved candidate inventory and test identities again, refusing unexplained drift. Namespace membership alone does not discharge per-test/fixture or runtime obligations.

### JVM fast: 55 namespaces

```text
clj-surgeon.battery-ledger-test
clj-surgeon.battery-parallel-test
clj-surgeon.census-pool-test
clj-surgeon.fast-lane-isolation-test
clj-surgeon.helper-extraction-test
clj-surgeon.lane-manifest-test
clj-surgeon.mcp-change-buffer-test
clj-surgeon.mcp-combinable-transaction-test
clj-surgeon.mcp-compact-edit-fields-test
clj-surgeon.mcp-compact-edit-test
clj-surgeon.mcp-compact-location-test
clj-surgeon.mcp-compact-relations-test
clj-surgeon.mcp-contract-test
clj-surgeon.mcp-create-files-test
clj-surgeon.mcp-expect-guard-test
clj-surgeon.mcp-extraction-plan-test
clj-surgeon.mcp-extraction-test
clj-surgeon.mcp-formatter-test
clj-surgeon.mcp-inspect-contract-test
clj-surgeon.mcp-inspect-tool-test
clj-surgeon.mcp-intent-contract-test
clj-surgeon.mcp-namespace-split-test
clj-surgeon.mcp-operation-async-test
clj-surgeon.mcp-operation-registry-test
clj-surgeon.mcp-operation-test
clj-surgeon.mcp-paths-test
clj-surgeon.mcp-prepared-confirmation-test
clj-surgeon.mcp-prepared-request-test
clj-surgeon.mcp-program-tool-test
clj-surgeon.mcp-read-request-normalization-test
clj-surgeon.mcp-recovery-test
clj-surgeon.mcp-relation-census-round20-test
clj-surgeon.mcp-schema-test
clj-surgeon.mcp-semantic-client-test
clj-surgeon.mcp-telemetry-test
clj-surgeon.mcp-workspace-test
clj-surgeon.mcp-write-refusal-test
clj-surgeon.mission-candidate-race-test
clj-surgeon.mission-candidate-test
clj-surgeon.mission-forms-source-test
clj-surgeon.mission-forms-test
clj-surgeon.mission-git-test
clj-surgeon.mission-plain-forms-test
clj-surgeon.mission-typist-test
clj-surgeon.mission-usage-test
clj-surgeon.namespace-split-test
clj-surgeon.ns-isolation-test
clj-surgeon.outline-differential-test
clj-surgeon.outline-memory-test
clj-surgeon.quoted-var-refs-test
clj-surgeon.require-change-test
clj-surgeon.scope-stream-test
clj-surgeon.split-proof-gate-test
clj-surgeon.telemetry-events-test
clj-surgeon.workspace-onboarding-test
```

### JVM integration: 7 namespaces

```text
clj-surgeon.mcp-feature-thread-test
clj-surgeon.mcp-hot-verify-test
clj-surgeon.mcp-http-server-test
clj-surgeon.mcp-server-test
clj-surgeon.mcp-tool-test
clj-surgeon.namespace-split-warm-test
clj-surgeon.outline-corpus-integration-test
```

### JVM battery: 36 namespaces

```text
clj-surgeon.admit-patch-test
clj-surgeon.cell-b-oracle-test
clj-surgeon.core-discovery-test
clj-surgeon.mcp-alias-migration-test
clj-surgeon.mcp-cold-verify-test
clj-surgeon.mcp-feature-thread-sed-test
clj-surgeon.mcp-helper-extraction-test
clj-surgeon.mcp-inspect-cold-job-test
clj-surgeon.mcp-prepared-wire-test
clj-surgeon.mcp-process-test
clj-surgeon.mcp-relation-census-launcher-test
clj-surgeon.mcp-relation-census-test
clj-surgeon.mission-commit-cli-test
clj-surgeon.mission-display-test
clj-surgeon.mission-events-test
clj-surgeon.mission-fallback-test
clj-surgeon.mission-git-boundary-test
clj-surgeon.mission-git-fence-test
clj-surgeon.mission-git-identity-test
clj-surgeon.mission-git-ledger-test
clj-surgeon.mission-git-process-test
clj-surgeon.mission-git-submodule-test
clj-surgeon.mission-phase-events-test
clj-surgeon.mission-provider-fallback-events-test
clj-surgeon.mission-publication-test
clj-surgeon.mission-run-test
clj-surgeon.mission-test
clj-surgeon.mission-typist-executor-admission-test
clj-surgeon.mission-typist-executor-test
clj-surgeon.mission-usage-executor-test
clj-surgeon.reader-eval-fence-test
clj-surgeon.receipt-artifacts-boundary-test
clj-surgeon.repository-hygiene-test
clj-surgeon.require-change-boundary-test
clj-surgeon.split-proof-gate-boundary-test
clj-surgeon.txn-journal-test
```

### Babashka: 49 namespaces, declared order

```text
clj-surgeon.tmp-leak-support-test
clj-surgeon.forms-test
clj-surgeon.alias-migration-test
clj-surgeon.agent-routing-test
clj-surgeon.outline-test
clj-surgeon.move-test
clj-surgeon.operation-algebra-test
clj-surgeon.move-dependency-test
clj-surgeon.analyze-test
clj-surgeon.diagnostic-delta-test
clj-surgeon.rename-test
clj-surgeon.fix-declares-test
clj-surgeon.extract-header-test
clj-surgeon.extract-test
clj-surgeon.failure-report-test
clj-surgeon.file-ops-test
clj-surgeon.show-form-test
clj-surgeon.structural-lens-test
clj-surgeon.syntax-var-refs-test
clj-surgeon.lens-query-test
clj-surgeon.memory-battery-test
clj-surgeon.cljc.merge-test
clj-surgeon.cljc.split-test
clj-surgeon.cljc.require-ops-test
clj-surgeon.cljc.analyze-test
clj-surgeon.edn-config-integration-test
clj-surgeon.edit-test
clj-surgeon.edit-dsl-test
clj-surgeon.cljc-existing-ops-test
clj-surgeon.ls-tree-test
clj-surgeon.outermost-test
clj-surgeon.owner-hypotheses-test
clj-surgeon.parser-admission-test
clj-surgeon.partition-all-test
clj-surgeon.platform-selector-test
clj-surgeon.quoted-var-refs-test
clj-surgeon.xray-test
clj-surgeon.help-test
clj-surgeon.install-test
clj-surgeon.insertion-gap-test
clj-surgeon.intent-transaction-test
clj-surgeon.workspace-onboarding-test
clj-surgeon.worktree-lifecycle-test
clj-surgeon.worktree-lifecycle-io-test
clj-surgeon.worktree-lifecycle-cli-test
clj-surgeon.recovery-test
clj-surgeon.relation-census-test
clj-surgeon.cli-dispatch-test
clj-surgeon.core-discovery-test
```

Report written 2026-09-09T13:50:53.838951+00:00. Work began 2026-09-09 13:38:04 UTC; completed before the 90-minute interim boundary and within the three-hour timebox.


---
## Brief (item 0 of Astra's order)

# Astra, item 0 of your own order: freeze Frame 7's clocks and the consumer's obligations from retained artifacts (read-only in /home/forge/src/clj-surgeon-astra-consult2; NO commits; write EXACTLY /var/tmp/forge/plan2/cellC/astra-frame7-freeze-report.md; 3-hour timebox, interim at 90 min)
Gene (2026-09-09): "go with astra's order". Your consult is on records: docs/observations/2026-09-09-astra-squares.md. This is your item 0: "Freeze Frame 7's clocks and the consumer's obligations before implementation — 2–4 hours, using retained artifacts."
Deliver a document that can be adopted verbatim as the Frame 7 protocol and as the contract for item 1's consumer:
1. THE CLOCKS. t0 and tL exactly as you defined them (independent runner's prospective release; first independently observed publication of the accepted tree on the registered benchmark landing ref), what is charged (orientation, route discovery, schema fetch, receipt decoding, every repair and refusal, queueing, setup, cold loads, required tests, review, re-sealing, transport, publication; machine-owned work after the caller says done; six attempts = one request), the secondary clocks (request-to-accepted; caller-done), the acceptance oracle and when it fires (automatically at the declared lifecycle boundary), the uncertainty rule, and the exact statistic (paired per slot, median of the six, mean of 3rd/4th ordered). Name every place where the retained E3/E4/row-2 artifacts would have violated this protocol, so the runner can be checked against it.
2. THE PORTFOLIO. Which tasks (the whole prepared package P vs native N; Surgeon over equally prepared native G), how many slots make it honest, what "equally equipped native" must have (the same observer, probes, prewarm, consumer), the isolated benchmark landing refs (never public main), and the runner's ownership of every clock.
3. THE CONSUMER'S OBLIGATIONS (item 1's contract). For a landing gate consuming Surgeon evidence: the exact obligation list the repository derives independently (which tests, which runtime and inputs, scope, exclusions, candidate identity, toolchain), what evidence discharges each, what can NEVER be discharged by a warm probe, the typed meanings of declared skips / focus omissions / unexecuted tests, the fail-closed rule, and the exact bytes a consumer must be tested against (you found gates-prewarm.edn is not valid EDN: specify the repaired receipt shape and the migration).
4. FALSIFIERS AND PRE-REGISTERED FORECASTS restated so the runner prints them before the first arm.
Cite the retained artifacts by path. Prose plus tables; no new arms; no server calls; never ports 7888/7890/7894/7895/8300-8339.
