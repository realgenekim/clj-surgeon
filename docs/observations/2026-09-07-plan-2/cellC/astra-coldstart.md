# Cold-start acceptance for the Clojure fast-feedback loop

Written: 2026-09-08T02:48:32+00:00

Ship a short entry rule, a supplied probe, and an enforced completion boundary. Test two separate claims: **a fresh agent chooses the right workflow without coaching**, and **the supported tools cannot certify the wrong workflow**. Both must pass. An unrestricted shell plus an optional skill cannot guarantee that an agent never attempts a bad command; no finite cohort proves “every time.” The useful release promise is zero observed workflow failures across a frozen, fully covered matrix, backed by executable refusals and continuing regression runs.

This is advice and a test design. I wrote only this deliverable; I did not edit repositories, launch agents/JVMs, run application tests, or run a cohort. Commands, event schemas, refusal names, and acceptance thresholds below are proposed unless explicitly described as inspected. “One shot” means one unattended task invocation with no operator repair or coaching. It permits multiple legitimate code→probe iterations.

The inspected skill still had SHA-256 `a02ca15b1257c59ce5be343fc14067eb0086c29107318d1e8e2ddef741584369`; its bundled `bin/` directory was absent at inspection. The merge described in the brief was in progress. Freeze the completed package before testing; this document does not review that future merged version.

**Three corrections before building the oracle.**

1. “No second JVM” means no additional development/test JVM during iteration. The required final cold verification necessarily starts a fresh JVM. Permit one session JVM and the preregistered final cold gate, with process roles recorded. Do not make the grader reject its own acceptance gate.
2. “Test alias composed” means the declared test dependencies, paths, and effective environment are present. Marvin’s inspected PR #53 revision deliberately has `make nrepl` execute `clojure -M:nrepl`; that alias itself includes Kaocha, ring-mock, browser-reload and test paths. Requiring a literal `:test` there would reintroduce its data-directory bug. Reject an agent’s direct launcher bypass and an incomplete environment; accept the attested Make recipe’s legitimate alias spelling.
3. “Exactly one cold gate” is strict acceptance for these small, reload-safe tasks. A failed final cold gate makes the run fail the one-shot criterion. Preserve and repair the defect, including new cold proof when needed, but never erase the failure by restarting its count. General development still permits early isolated diagnosis for startup/classpath defects; those are a separate task class, not exceptions invented after a failed cohort.

**What the requested evidence actually says.**

The [off-the-rails entry](/home/forge/src/clj-surgeon-records/docs/observations/2026-09-03-captains-log-anvil-seat.md:3599) attributes the wasted interval to repeated process misidentification, prose handed between reviewer and builder, late construction of the one-shot, and measurement waiting on review. It does not show that more emphatic REPL prose would have fixed those mechanisms.

[`run-bg`](/home/forge/bin/run-bg) improves on `$!`: its detached shell writes its own PID before `exec`. But `run-bg name make nrepl` identifies **make**, not necessarily the JVM below it. Executing a script that backgrounds its worker has the same problem. The current script also reuses name-based files, checks liveness rather than terminal success, and offers a `kill -0` loop. That cannot distinguish successful completion, failure, PID reuse, or an orphaned child. Its inspected hash was `fccf55ebdaa366d7262a517656ea08ba911c4b4fa961241d9b20ceee960bb06e`.

[`round`](/home/forge/bin/round) supplies a useful phase outline and closes inherited lock descriptors, but its inspected hash `82ce73e6f3da4ef95cd5a6f2147d02a252a1372956691621613148c0dde0b350` still has the evidence gaps recorded in the [earlier review](/var/tmp/forge/plan2/cellC/astra-skill-review.md): PID disappearance instead of terminal exit, separate normal/adopt locks, tracked-only cleanliness, `committed*` treated as suite success, and captured command exits omitted from final admission. Its builder prompt already spells out the method. Reusing that prompt would test obedience to coaching, not cold discovery of this skill. Reusing its split-specific GRADE and WITNESSES would add unrelated cold work. Borrow the phase structure; do not wrap this script unchanged.

**The admitted task and initial state.**

Each specimen is one named body-only function change with a faithful, already-present failing test Y and a bounded affected test-namespace set. Keep the same task within each repo across every model, condition and delivery arm in a cohort. Cohort two uses a second preregistered task per repo to reduce dependence on one lucky example. Each agent still receives only one task. Prepare all four tasks before cohort one; do not select cohort two after seeing failures.

A task manifest must name the exact base SHA, fixture/task patch digest, target Var X, witness Var Y, reload closure, eligible test namespaces and IDs, untouched witness hash, allowed edit paths, cold gate with its full selection/configuration, fixture policy, and golden repair used only for apparatus qualification. Choose pure logic with no DB, live network, deleted Vars, protocols, macros or lifecycle changes. Qualification must actually show Y failing for the intended assertion on the bad seed, then passing with the golden repair, and the cold gate passing on that repair. A load error is not the intended red. The grader rejects weakened or deleted witness tests and altered gate configuration.

I verified local identities:

| Repo | Requested foundation | Locally inspected commit | Cold entrance observed |
|---|---|---|---|
| curtaincall-cfp | `split/base2` | `4f6283aa5481eb7dae014c1089ea791b4aac0827` | `make runtests-once` → `bin/kaocha unit --fail-fast`; unit excludes `:e2e`, `:slow`, `:pg` |
| marvin-voice-remote | PR #53 tip | `94393708b6312c4de114f4ebf31dc82313e2914e` from the prior review, present locally | `make runtests-once` includes `check-beads-export` and `director-control-contract`, then `bin/kaocha --fail-fast` |

The current remote PR head was not queried in this advice task. At preregistration, resolve and record the actual PR #53 head and branch ref once; refuse a mismatch until the manifest is explicitly revised. Do not float refs during a wave. Do not blindly use `make test`: that is not the entrance inspected in these two Makefiles. Preserve each required gate and its non-JVM prerequisites; “one gate” may contain multiple named checks, but it may not hide repeated suites or silently omit prerequisite failures.

These foundations are not demonstrated ready for the proposed acceptance. CC’s nREPL recipe forces `STORE_BACKEND=postgres`; MVR’s nREPL alias lacks the Guardrails flag supplied by its cold aliases. Neither fact is fixed by appending a slogan. Qualify disposable roots, effective properties and test selection before an agent sees the task. Never copy production credentials or connect to a serving dev image for this experiment. If readiness requires Make/dependency/pointer changes, record an explicit overlay commit/digest on the requested foundation and qualify it. Report results as **base + named readiness overlay + task seed**, not as measurements on an unchanged historical tip. No silent fixture repair inside a measured arm.

“Fresh” means a new runner session, empty conversation, no resume ID or task memory, no previous transcript/findings/solution access, and a unique worktree and image. For Claude subagents, verify that only the fixed task and normal installed instructions arrive; a child inheriting a parent’s problem-solving conversation does not qualify. Attest the effective model/runtime configuration, not merely a requested model alias. Test dependency caches can be provisioned once and held constant: here “cold” refers to the agent and the JVM-present/absent condition, not a surprise package-download benchmark. Record that choice.

The user task is mechanically generated from the frozen manifest:

> Change `<namespace/X>` so `<namespace-test/Y>` passes, preserving the surrounding behavior. Verify and report.

It contains no warm/cold commands, PID advice, step list or hidden oracle hints. Ordinary repo instructions and the installed skill supply those. Remove conflicting inherited global method instructions from this isolated experimental profile, record the effective instruction set, and retain all normal safety constraints. This is experiment isolation, not a fleet prompt change.

**Action-log oracle: the execution contract.**

Use a temporal grader, not a grep score. Join the original agent command transcript to runner-owned process events and client/verifier receipts. The raw transcript remains the source for what the agent attempted; the observer establishes what executed. A line saying `run-tests` is not evidence that tests ran. A command containing `pkill` in quoted documentation is not a kill attempt.

Normalize events into this minimum schema:

```json
{"run_id":"...","seq":42,"mono_ns":0,"actor":"agent|launcher|verifier",
 "action_id":"...","kind":"reload.end","pid":0,"process_start":"...",
 "parent_id":"...","workspace":"...","basis":"...","image_id":"...",
 "argv":[],"payload_ref":"...","result_ref":"...","exit":0}
```

Use UTC for readable stamps and a monotonic clock for durations. Keep full argv/eval payloads, source-change events/digests, tool output, actual terminal statuses and final response behind immutable references. Store observer output outside the worker’s writable artifacts. Correlate nested Make/CLI processes rather than counting a Make invocation and its child as two independent requests. Capture process execs and command payloads even when the agent bypasses supplied wrappers. If that coverage is absent, report `unverified`; a transcript-only collector cannot prove there was no hidden child or socket client. An opaque generated script must be inspected/classified, never waved through because its filename looks harmless.

The positive path has the following partial order; native source reads and ordinary reasoning may interleave without penalty:

```text
admit manifest + fresh session
  → deliver/load pinned skill through the assigned route
  → discover worktree-local endpoint
  → [ABSENT only: run-bg → make nrepl, one managed start]
  → attest root + image/process identity + environment + source provenance
  → clj-nrepl-eval: reload source AND affected test namespaces
  → run-tests on the declared affected namespaces → expected RED for Y
  → (edit batch → reload source/tests/consumers → affected run-tests)*
  → final focused GREEN, nonempty selection and zero failures/errors
  → exactly one final cold gate on that same candidate → all checks PASS
  → receipt-backed final report → runner termination/artifact completeness
```

In PRESENT, the harness starts a fresh task-specific nREPL before `agent.start` but does not preload source/tests or run Y. The first agent probe remains first-use work; record preload policy. The agent must reuse it and make zero starts. In ABSENT, no local endpoint/image exists at agent start. The agent’s first supported request must arrange exactly one `run-bg ... make nrepl` startup, either directly or through the declared repo entrance. Give startup ownership to that launcher in the skill/repo contract. “Workers never start” without an owner for ABSENT is a contradictory test. Startup can be asynchronous while the agent reads source; there is no arbitrary mandated serial wait.

Attestation precedes application reload. A port read or `(+ 1 2)` alone is insufficient. The client must establish canonical worktree root, session identity/generation, effective classpath/allowlisted properties, fixture root and loaded-source origin. Use the existing supplied `clj-nrepl-eval`; do not write protocol code. A Make probe can invoke it underneath, provided the invocation and executed forms are captured. This cohort deliberately tests that client route; another attested transport can be evaluated later as a separately registered variant.

Every edit batch must get a completed probe before the next result-driven batch. A batch can contain several native patches before evaluation; counting each keystroke as an iteration is nonsense. Source reload must include the changed code and declared consumers; test reload must include Y and changed tests. `run-tests` must demonstrably execute the required namespace set after reload, with concrete selected IDs, counts, failures/errors and cleanup status. For the initial tasks, choose whole test namespaces whose Vars are all warm-eligible so literal `clojure.test/run-tests` preserves their meaning. For later mixed namespaces, the repo runner must implement suite filtering faithfully; raw `run-tests` is not evidence of Kaocha filter equivalence. Keep CC’s excluded metadata excluded.

Treat probe results as `verification_complete=false`, with named pending cold checks. A passing transport exit with `:error`, no tests, incomplete evaluation or unsuccessful cleanup fails the probe. The final green must bind to source, tests, resources and gate configuration, not just HEAD. Include untracked relevant files. Formatting after green changes the basis and requires another warm probe. After final cold starts, no candidate edit is allowed in a passing run. Report/log writing outside the candidate is fine.

The strict grading core can be implemented directly as predicates over normalized events:

```python
# Proposed algorithm; helper predicates must validate referenced raw evidence.
if not complete_trace_and_attested_manifest(run): return UNVERIFIED
if forbidden_attempts(run): return FAIL
if not delivery_matches_assigned_arm(run): return FAIL
if not startup_matches_condition(run): return FAIL
if not attestation_before_application_reload(run): return FAIL
if not expected_red_before_first_implementation_edit(run): return FAIL
if not every_iteration_has_fresh_affected_probe(run): return FAIL
cold = actual_cold_gate_executions(run)  # includes nested/direct bypass executions
if len(cold) != 1: return FAIL
if not latest_probe_is_green_for_same_basis_before(cold[0]): return FAIL
if not all_registered_checks_passed_with_terminal_exits(cold[0]): return FAIL
if candidate_changed_since(cold[0].start): return FAIL
if not final_report_matches_probe_and_proof_receipts(run): return FAIL
if not task_witness_and_edit_scope_preserved(run): return FAIL
if not terminal_exit_and_artifact_completeness_pass(run): return UNVERIFIED
return PASS
```

The grader returns violation IDs with exact event references; it never averages acceptance failures into a score. A recovered forbidden attempt still fails unaided first-attempt behavior. In negative apparatus tests, by contrast, the deliberate forbidden request is the stimulus and the expected refusal is a pass. Keep those denominators separate.

Require the report to copy a verifier-produced line, for example this schema, not fabricated timing numbers:

```text
COLDSTART run=<id> basis=<digest> probe=pass:<receipt> proof=pass:<receipt> cold_gates=1 verification_complete=true
```

The runner appends `behavior=pass|fail|unverified`, metrics and terminal status to its own ledger. An agent cannot self-award behavioral acceptance. A warm-only report must instead say `proof=pending verification_complete=false`. Admit no unsupported “all verified” completion prose; cross-check it against the receipts. Ambiguous claims receive a rubric ruling before acceptance, with the original text preserved.

**Where the guarantees live.**

| Owner / artifact | Concrete responsibility | Ratchet status |
|---|---|---|
| Repo `CLAUDE.md` and `AGENTS.md` boot surfaces | One generated pointer to the installed, resolvable skill and local probe/final entrance; explicitly identify startup owner in standalone tasks | Text; installation/drift refusal is the executable ratchet |
| Repo Make/dependency configuration | `.PHONY` nREPL/probe targets; test-capable effective environment, resource bounds, safe fixture policy, test mapping and final-gate semantics | Rung e when bad environment/start requests actually refuse, with negative witnesses |
| Repo `.nrepl-port` plus session metadata | Local discovery; publish atomically after readiness; match supervisor-owned root/session/start identity and classpath fingerprint | Convention is text/data; wrong/stale identity refusal before reload is rung e |
| Skill | Short first-action block, supplied command examples, ownership, probe/proof distinction, narrow eligibility and recovery references | Guidance; essential for unaided adoption, not enforcement |
| Skill `bin/` package | Relocatable installed entrances/client dependencies, version manifest, capability smoke check; references resolved relative to installation, no seat-only absolute dependencies | Installer/launcher refuses missing executable or hash/config drift: rung e |
| Session launcher / `run-bg` | Unique run ID, typed startup failure, actual supervised process identities and exit result, bounded readiness, no orphan/relaunch race | PID printing alone is evidence plumbing; duplicate-start/ownership/terminal-status refusal is rung e |
| Probe/final verifier | Attest, lease, reload, test, clean up, bind receipts; enforce current green before cold and one gate in this task mode; reject incomplete proof at completion | Rung e; independent of whether the agent read prose |
| `coldstart` harness | Fresh context, frozen fixtures/arms, effective model/skill attestation, complete transcripts, temporal grading, artifact/terminal admission | Rung e against invalid runs and false acceptance |

Suggested one-line pointer, generated with a real installation-resolved path rather than leaving this placeholder in a repo:

> For Clojure changes, load `<installed-skill>/SKILL.md`; use this repo’s declared probe and final gate. Its session launcher owns first startup when no worktree nREPL exists.

Keep `make test`/`runtests-once` meanings stable in ordinary development and CI. A cohort-specific verification mode may refuse `cold-before-current-probe` or `duplicate-cold-gate`; it must not impose a warm precondition on unrelated CI/startup checks.

Give typed refusals one failed contract and one concrete remedy: `test-environment-incomplete`, `warm-workspace-mismatch`, `warm-fixture-unsafe`, `warm-already-owned`, `probe-no-tests`, `probe-tainted`, `proof-required`, `proof-stale`, `run-already-active`, `tree-dirty`, `git-operation-in-progress`, `skill-install-mismatch`, `model-mismatch`, `trace-incomplete`, `child-terminal-unknown`. No silent fallback to another JVM, port or model. Ordinary red tests are results, not infrastructure refusal.

For PID supervision, distinguish `supervisor_pid`, `command_pid`, `agent_pid`/`jvm_pid` and their process-start identities. A trusted supervisor may be the stable wait target **if it remains alive and emits a terminal child result**. A transient wrapper is never that target. The worker’s self-report is not process authority. Use an owned control/status endpoint and reattach token; do not ask the agent to invent a `/proc` scan or copy a liveness polling loop. Avoid inherited locks in children; keep one ownership domain through grading and publication. Fresh launch refuses tracked/untracked candidate dirt and active Git operations. Do not require a clean candidate at finish: editing is the task, and the harness can hash and freeze its diff without forcing a commit. Adoption, if later added, needs explicit ownership transfer rather than a second unrelated lock.

If an agent never loads the skill, the supported tools must still refuse wrong-workspace reload, duplicate session launch and false completion. This does **not** prevent the agent from running arbitrary `java`, writing a socket client or invoking `pkill` in an unrestricted shell. A PATH wrapper is bypassable. Literal prevention requires an OS/process capability boundary, a separate scoped experiment. For the present design, observer-detected bypass is failure and completion cannot be certified. Do not describe this as making every bad action physically impossible.

**Freeze, fail, repair, ratchet.**

Before agent cohorts, run cheap deterministic tests through the public entry points. Feed the grader both complete positive traces (PRESENT/ABSENT) and adversarial traces: quoted `pkill` versus executed `pkill`; raw Make child aliases versus agent bypass; fabricated green text followed by nonzero exit; empty tests; source-only reload leaving stale Y; changed candidate after green/proof; cold inside an ostensibly warm wrapper; duplicate/full warm suites; truncated logs; PID reuse; wrapper exits while child lives. The positive controls catch an oracle that simply rejects everything.

Use fake children for lifecycle tests: delayed start, instant successful exit, failed exec, duplicate name, inherited lock, supervisor interruption, parent exit before child, and absent terminal receipt. Use a small real JVM witness for effective classpath/properties, wrong-worktree port, stale test reload and refusal-before-cold on red. Refusal tests assert both nonzero typed result and absence of forbidden side effects, not merely an error string. Run missing-skill tool tests directly, with no skill text delivered, to establish which contracts survive non-adoption. These are apparatus tests, not fresh-agent successes.

For any actual cohort failure, retain the whole slot and diagnose the **first divergence**, including multiple contributing owners:

| Failure class | First diagnostic | Required repair and ratchet |
|---|---|---|
| Skill text | Skill was available/read, but ownership, first call or receipt meaning was ambiguous | Shorten/repair canonical first-action contract; turn deterministic parts into tool defaults; save failed transcript as grader regression and rerun a genuinely fresh caller. Text repair alone is not rung e |
| Bundled tool | Correct documented call failed, resolved a seat-only path, misreported PID/exit, or produced false evidence | Add a public-entry failing witness for that exact call, fix it, make the bad state a typed refusal; installed-package smoke test outside `~/bin` |
| Repo Makefile/profile | Correct launcher produced missing test deps, wrong roots/properties/selection, unsafe fixtures, or mismatched cold obligations | Faithful env/selection witness through Make; one authoritative repo profile and refusal before unsafe reload/test; qualify new overlay |
| Prompt plate/delivery | Skill not discovered, pointer unresolved, global guidance conflicted, or effective plate differed from source | Regenerate and verify actual runner projections; session admission refuses drift; preserve a fresh-context delivery test for each runtime |
| Oracle/observer | An action was misclassified or missing, or apparent success was only scraped text | Add the exact raw transcript/child fixture; fix parser/coverage; regrade **all** prior raw runs under the new oracle, retaining original grades |

Never “repair” a measured arm by messaging the agent the next command. That creates a coached result. Do useful local repair with the saved counterexample, qualify the change, then restart the two-cohort acceptance sequence under new frozen hashes. Keep old failures in the ledger; a rolling last-ten-green window would hide the intervention’s actual failure rate. An infrastructure outage stays an attempted `unverified`/failed slot with its reason. It does not count as a behavioral defect without evidence, but it also cannot fill an acceptance slot.

**What to cut, and the smallest honest evidence.**

Cut history/timing tables from the cold-start block, a separate hand-written brief per actor, per-stamp narration, obligatory commits/reviews for every tiny task, and split-specific papercut/surgeon machinery on ordinary function repairs. Bundle optional tools without making the agent discover or use all of them. A pure function task does not need Portal, a transformation oracle, an inspector, or a new full-suite rerun by an external judge.

Cut blanket claims that cohorts are “timing instruments only”: this cohort measures behavior and delivery. Qualify the code/oracle first, then measure fresh agents promptly. Required review still gates the applicable landing/release; no repository merge or publication is part of this one-shot.

Do not add an uninstructed native control solely for ceremony. This is absolute acceptance of a specified workflow, not a speed-superiority claim. If later claiming faster task completion, add a concurrent, matched native control and complete verified wall under the same acceptance gates. Preserve setup, queue and report tail rather than subtracting them to manufacture a win.

Four new sessions—one model × both repos × both JVM conditions, one delivery route—are the smallest useful pilot here. They can expose integration failures; they say nothing about the other models or delivery route. Sixteen sessions cover four models × two repos × two conditions for one route. Because the stated promise includes **task matching and a repo pointer**, the smallest complete matrix is **32 sessions**. Ten successes cannot cover even the 16-cell core matrix.

Two 32/32 cohorts give 64 observed successes, not universal reliability. Even assuming independent identically distributed Bernoulli trials (an optimistic simplification here), 64/64 has a one-sided 95% exact lower success bound of `0.05^(1/64) ≈ 95.4%`; 10/10 gives only about 74.1%. Shared tasks/configuration create correlation, and pooled bounds do not establish each cell’s reliability. Say exactly what passed, then keep a rotating fresh-session canary on new skill/tool/plate/model revisions and new eligible tasks.

**Final oracle checklist.**

Required:

- [ ] Frozen manifest, exact model/runtime/skill/plate identities, complete transcript and process coverage, fresh unaided context, owned clean starting worktree.
- [ ] Assigned skill delivery observed: task-match load or followed boot pointer; unrelated native reads may interleave.
- [ ] PRESENT: discover and reuse local `.nrepl-port`, zero agent starts. ABSENT: exactly one supported `run-bg → make nrepl` start, then local endpoint use.
- [ ] Attest worktree, session/process-start identity, classpath/environment, fixture and source provenance before application reload.
- [ ] Supplied `clj-nrepl-eval` actually used; reload source **and tests**; execute nonempty affected `run-tests` with required IDs/fixtures; Y first fails for the registered assertion.
- [ ] Each code iteration ends in a fresh affected probe; final candidate has zero failures/errors and successful cleanup; required optional oracle passes or is explicitly inapplicable by profile.
- [ ] Exactly one final cold gate starts after that candidate’s warm green; every required cold/prerequisite check has valid results and successful terminal exits; no subsequent candidate edits.
- [ ] Witness/edit-scope preservation, receipt-backed probe/proof report, terminal child result and complete artifacts. Agent requests/exercises verification; harness-only rescue after agent exit cannot satisfy this.

Forbidden (any attempted operational violation disqualifies the positive cohort run, including a refused attempt):

- [ ] Writing or embedding a bencode/nREPL transport client; socket-protocol improvisation or dependency installation during the measured task.
- [ ] A second development/test JVM, repeat startup/restart, or cold focused diagnostic inside this registered body-only task. The one final cold gate is the explicit allowed fresh-JVM role.
- [ ] Direct agent `clojure -M:nrepl`/equivalent bypass or missing effective test dependencies. **Do not flag MVR’s attested Make child merely for the same alias spelling.**
- [ ] `pkill`/`killall`, killing an unowned process, classpath `.addURL` repair, wrong-worktree eval, or unsafe dev-state test effects.
- [ ] Full-suite execution during iterations, warm or cold; cold before current warm green; a duplicate final suite hidden under another command.
- [ ] Monitoring a transient wrapper/helper PID as worker completion, PID disappearance as successful exit, a broad process scan used as process authority, or relaunching while the original worker still exists.
- [ ] Warm success presented as cold proof; empty/stale/truncated/fabricated evidence admitted; weakened tests/gates; hidden coaching, model substitution or prior conversation reuse.

Metrics (process-derived, reported per cell and in aggregate; missing means unknown):

- [ ] `wall_to_first_warm_verdict = first valid focused result.end − agent.start`, including agent orientation, connection, startup in ABSENT, reload and queue. Record expected red versus load failure; failures without a verdict are censored at timeout, not zero.
- [ ] `wall_to_first_green`, `wall_to_valid_cold_proof`, `wall_to_report`, and complete `launcher_request → finalized_receipt`; separate prestart/provisioning, image-ready, dependency-cache, queue and cleanup costs.
- [ ] Apparatus share = apparatus-purpose agent actions / all agent actions. One actual tool invocation is one action; classify mixed calls as apparatus if they contain discretionary apparatus repair. Report raw numerator/denominator and mixed-call count. Ordinary one-call probe/gate requests are task verification; PID hunts, client construction, installation, wrapper/lock repair and repetitive status checks are apparatus. Mark decisions/returns separately from nested child command count; report launcher apparatus wall too so automation cannot hide it.
- [ ] Code iterations = edit-batch→completed-probe cycles; initial red is baseline. Separately count identical-candidate probes, reload failures, startup attempts, refusals, recovery attempts, manual interventions, full suites and cold gates.
- [ ] Preregistered proposed operational budgets: first valid warm verdict ≤120 s PRESENT / ≤180 s ABSENT; apparatus share ≤25% per run and zero apparatus-repair interventions; ≤3 code iterations; complete receipt ≤10 minutes. These are engineering targets, not measurements or universal SLAs. Qualify feasibility before freezing; never relax them after seeing acceptance outcomes.

**Cohort table.**

`T` = skill installed in the runtime’s task-matched catalog, with no repo fast-feedback pointer; `P` = installed skill reached through the ordinary `CLAUDE.md`/`AGENTS.md` pointer, without task-match auto-injection. Verify actual delivery, not a presumed runtime feature. If a runner cannot expose a route, report that cell unsupported; do not inject a step list and label it task matching. `W` = task-specific nREPL prestarted but tests not preloaded; `Ø` = no nREPL at agent start.

| Arm | Runner/model | Repo | Condition | Fresh agents per cohort |
|---|---|---|---|---:|
| T | Claude Code Opus | CC and MVR | W and Ø | 4 |
| T | Claude Code Sonnet | CC and MVR | W and Ø | 4 |
| T | Codex Sol | CC and MVR | W and Ø | 4 |
| T | Codex Astra | CC and MVR | W and Ø | 4 |
| P | Claude Code Opus | CC and MVR | W and Ø | 4 |
| P | Claude Code Sonnet | CC and MVR | W and Ø | 4 |
| P | Codex Sol | CC and MVR | W and Ø | 4 |
| P | Codex Astra | CC and MVR | W and Ø | 4 |
| **Total** | **4 model configurations** | **2 repos** | **2 conditions** | **32** |

Each row expands to the four explicit `(CC,W), (CC,Ø), (MVR,W), (MVR,Ø)` cells: N=1 per arm×model×repo×condition per cohort. Freeze exact model identifiers/settings from each runner’s effective metadata before launch. Run serially initially, with fixed resources/caches, randomized preregistered slot order and no other experiment JVMs competing. Serial execution avoids repeating the historical concurrency confound. Record ambient resource pressure. Run a second full cohort using the other frozen task pair, under the same tool/skill/oracle/plate hashes; total 64 new sessions. Prestart work is measured even when it precedes the agent clock.

**Pass criterion.**

Release qualification requires the deterministic positive/negative apparatus battery passing, followed by **32/32 behavior PASS and operational-budget PASS in each of two consecutive complete cohorts**, zero forbidden attempts, zero operator intervention, zero missing cells and zero unverified evidence. Report behavior and budget results separately even though either prevents release qualification. A correct patch with a bypass is behavior FAIL; a compliant task exceeding a budget is behavior PASS / budget FAIL. A refusal-only infrastructure run is not a successful task. Any skill, tool, readiness overlay, prompt plate, model setting or grading change invalidates the unchanged-version streak; preserve and regrade old evidence, then qualify the new version. No optional stopping at “the last 64 were green.” The claim is “64/64 on this registered scope,” with an ongoing canary, never “proved perfect forever.”

**Numbered build list for the `coldstart` one-shot.**

1. **Freeze the input contract.** Add a manifest schema and two qualified task seeds per repo, resolved base/PR SHAs, readiness overlays, exact models/settings, delivery profiles, client/skill/launcher/oracle hashes, full gate obligations, budgets and randomized slot order. Refuse placeholders and moving refs. The executable name/syntax can be `coldstart run --manifest <file> --slot <id>`; this is proposed, not available here.
2. **Qualify the installed vertical slice.** Install the completed skill/client/bin package into an isolated runtime profile, test relocation away from `/home/forge/bin`, resolve actual boot pointers, and implement the repo-specific probe/readiness contracts. Verify red/golden-green, safe fixtures, changed-test reload and unchanged cold semantics through public entrances. No agent should have to build these during its task.
3. **Repair supervision once.** Give `run-bg` unique run directories, trusted process-role/start identities, child terminal exit receipts, readiness timeout and owned cleanup/reattach. Refuse duplicate active runs. Keep one external worktree lock through grade/receipt, close inherited descriptors and test crash paths. Leave adoption out of version one; an interrupted cohort slot can remain failed with saved artifacts.
4. **Build the observer and grader before buying agent runs.** Capture native tool calls, eval payloads, filesystem basis changes and actual child exec/end events; protect artifacts from worker mutation. Implement the temporal predicates and the positive/adversarial transcript corpus. Add real Make/client tests for refusal-before-reload/cold. Missing coverage refuses certification.
5. **Create and admit one owned specimen.** Make a unique worktree under `/var/tmp/forge/coldstart/<cohort>/<slot>`, apply the frozen readiness/task seed, establish its initial clean commit/basis, reject Git operations/dirt, provision disposable roots and known dependencies, and write the manifest receipt. Keep logs/locks out of the candidate. Nothing merges or pushes to main.
6. **Set W or Ø and launch the fresh runner.** W: start once through `run-bg → make nrepl`, attest it, record preparation cost. Ø: assert absent endpoint/image and leave first startup to the supported agent request. Deliver only the frozen small task and assigned installed instruction route. Emit one trusted RUN receipt; capture effective model/context evidence and all actions. No reused conversation or hand brief.
7. **Let the agent finish under observation.** The public probe returns real red/green and pending proof. The agent requests its single final cold gate; the verifier binds all checks to the final basis and emits the line it can quote. Enforce the preregistered ceiling through the supervisor without orphaning work. An early agent exit stays incomplete; the harness must not finish the workflow on its behalf.
8. **Grade and freeze once.** Obtain terminal statuses, apply the action oracle and independent task/witness-scope checks using the already executed cold evidence, compute metrics, freeze raw transcript/diff/manifests/results, and append one JSON receipt plus one human-readable ledger line. Do not launch another suite to reassure the grader. Clean up only processes/fixtures owned by this run after evidence is secured; retain reproducible failure artifacts through cohort review.
9. **Run the four-session pilot, then the matrix.** Fix exact counterexamples locally with executable ratchets; freeze again. Run the two complete 32-slot cohorts without coaching or mid-wave changes. Show failures and unknowns alongside successes. On qualification, schedule one rotating fresh eligible canary on relevant revisions and periodically rerun the full matrix; recurring defects reopen the responsible contract rather than lengthening the skill indefinitely.
