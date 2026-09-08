# Where this way of working belongs

My commitment: keep Fable’s separation of policy, technique, and program operations, but add the missing executable delivery layer. Ship a session-owned warm JVM and one verification entrance shared by native editing and Surgeon. Put receipt truth and cohort admission in code. Use a short boot rule to explain the contract and a task-matched nREPL guide to explain recovery. A Make target, a port file, and an optional skill cannot by themselves make an agent take this route.

This is advice only. I read the three requested evidence sources and relevant local guidance; I changed no repository files and ran no tests. Proposed command names and refusal types below are design recommendations, not claims about shipped capabilities.

The evidence supports the intervention, with a narrower claim than “REPL-driven development wins.” Cell C’s initial verb median was 78 seconds versus native’s 447 seconds. Already-warm R5/R6 still took 513/554 seconds versus native’s 408/486. The last captain’s-log entry identifies 21/60 seconds spent authoring clients and roughly four minutes of planning. The verb removed work that warming the JVM did not. The round-three brief targets a different denominator: repeated output-wart repairs, with seconds-scale evaluation and an oracle instead of another fresh-caller campaign. Its 1–2 minute iteration is a target; the supplied brief is not a receipt proving that target was achieved. The later captain’s log reports all wave-three arms accepted, whereas the earlier result document still has pending-report cells. Keep that provenance visible.

## Placement follows the moment of use

**At boot: obligations and the current way in.** Shared house rules should carry a small, conditional rule: when doing iterative Clojure development, use the provisioned image and the repository’s probe entrance; retain cold proof for completion. “All work uses an hour-cut block ledger” does not belong in every coding task. The shared management rule should be conditional on an active program: recover its current block and resume note before continuing. Its particular cadence, queues, paths, and experiments belong to that program.

A compact candidate for the boot rule is:

> Use the repository’s declared probe/proof entrance for iterative Clojure work.
> The session owner provisions `make nrepl` with the repository’s test aliases before work begins.
> Worker agents connect through the attested `.nrepl-port`; they do not start, stop, or repair the JVM’s classpath.
> Reload affected code and run affected tests in the warm image for feedback.
> For output changes, use the bounded output oracle in each repair loop.
> A probe never establishes final proof; receipts must name pending checks.
> Run the required cold proof once on the final candidate; changes or failures invalidate that proof.
> Admit timing cohorts only after their preregistered output and acceptance gates are green.
> In a managed program, resume from its active block and durable handoff; let processes record time.
> Boot-rule changes include regeneration, installation, and verification of every affected managed block.

This is a ten-line contract, not ten lines plus the accumulated mechanics of every incident. Do not add a universal Surgeon preference: the fast probe must be equally available after a native patch.

**At task match: technique and exceptions.** Extend the existing nREPL guidance with connection, workspace attestation, reload order, focused test selection, output-loop examples, and stale-image recovery. Keep the safe-refactor and clj-surgeon pointers only where their workflows actually enter verification. Point both to one authoritative guide; do not duplicate its procedure. The guide may explain a command, but should not require a caller to write a client or compose a test expression from scratch.

There is a distribution detail to resolve before calling this “the existing nREPL skill.” On this seat I found the material at `claude-skills/_commands/clojure-nrepl.md` and `_commands/start-nrepl.md`. A Claude command file is not evidence that a Codex task-matched skill is installed. Package the same canonical content for each supported runner, and attest that installation. Sol/Astra are model choices in this design; the delivery obligation belongs to the runner/seat that invokes them, not a separate model-specific copy of the rules.

**At a command or tool call: behavior and truthful evidence.** The verifier owns warm-before-cold execution, failure classification, affected-test scope, and receipts. The cohort launcher owns admission and clocks. The session supervisor owns process lifetime. A rule about these is useful explanation, but their implementation must not depend on remembering it.

## The smallest artifact that actually changes the default

The smallest useful vertical slice is **one shared verifier wired into the existing session launcher and repository test workflow**. It has three small seams, not a new management platform:

1. The normal session/worktree launcher invokes the repo’s `make nrepl` once, before admitting coding work, and keeps ownership across worker exits and compactions. Each writable worktree has its own image. Each repo owns its alias composition and runtime limits. The fixture’s `:run-tests:test:nrepl` and Surgeon’s `:clj-surgeon/mcp-test:clj-surgeon/nrepl` are different implementations of the same readiness contract; do not copy one alias string everywhere.
2. A supplied client/verifier connects through `.nrepl-port` and verifies the canonical worktree root, live process/session identity, and expected test environment. A port is discovery, not identity or readiness. Cache expensive readiness checks against an environment fingerprint; do not add a new preflight tour to every iteration. If the warm image disappears, report it to its owner. In the managed session, do not quietly start a replacement or repair the classpath inside the worker.
3. An explicit probe target, illustratively `make test-probe`, reloads and runs affected test namespaces and the applicable output oracle, returning one bounded receipt. The existing final suite target retains its cold semantics. Surgeon delegates to the same verifier: its ordinary mutation default remains `:proof :cold`, with a warm probe first when eligible; an explicit iteration mode stops after the probe and reports pending proof.

Do **not** silently change `make test` to mean a focused warm run. That would improve a clock by weakening an existing meaning. “One cold suite” means one successful execution of each required cold gate for the final snapshot, with no duplicate rerun merely because a different caller wants reassurance. If the verb already executed that exact required gate, its valid receipt counts. A failed gate or a changed candidate needs new proof. The round-three brief separately requires fixture acceptance and Surgeon’s `make test`; neither disappears under a slogan about “one.”

For a fresh managed agent, provisioning must precede its first action, the verifier must already be callable, and completion admission must consume its receipts. That gives common behavior across callers without a bespoke task prompt. **There is no prompt-free universal guarantee for an unrestricted shell agent that can bypass every entrance.** A target nobody invokes is availability; a configured launcher plus an enforced completion boundary is a default with a defined scope. Do not build shell-command policing to conceal this distinction.

The output loop should accept the fixture/request once and batch reset, emission, warm witnesses, and tree oracle into one round trip. “Small interactions” means a small experiment per iteration, not one model return per expression. Supply bounded fixture reset mechanics that can operate only on an owned disposable fixture. Keep the compiler’s development image and the target fixture’s test image distinct when their worktrees or classpaths differ.

## What earns rung e

The local ratchet ladder defines rung e as a typed refusal in the operation when the bad state can be excluded by construction. Apply it to concrete authority and evidence errors, not to the inferred thought process “this agent seems to be doing a cold-only loop.”

- **False proof:** construct receipts from executed checks, with probe/proof represented distinctly. Probe-only success is a legal state (`committed-probe-only`, `verification_complete=false`, named `proof_pending`). Refuse completion admission with `proof-required` when that is the only evidence. Bind evidence to the relevant source snapshot, test configuration, check scope, and tool/oracle versions; refuse stale evidence with `proof-stale`. “No matching tests” must not become an unexplained green.
- **Wrong or unusable image:** refuse the warm operation with `warm-workspace-mismatch`, `warm-environment-mismatch`, or `warm-unavailable`, including an actionable owner handoff. A stale port file must not redirect reloads into another project. Outside a managed warm-required session, a cold proof may proceed with warm status explicitly unavailable; correct cold verification should remain possible in CI and startup/classpath work.
- **Warm failure:** fail before launching the cold suite, and report file mutation/rollback state separately from image state. Reloading can execute top-level effects; restoring files cannot generally undo them or remove stale Vars. A failed reload may leave the image tainted. Record that condition and suspend further warm use until its owner recovers it. Never claim transactional JVM rollback from a transactional file write. Use an image lease to prevent concurrent reloads from invalidating another caller’s probe.
- **Premature timing cohort:** the launcher refuses `oracle-not-clean`, `acceptance-missing`, or `cohort-spec-mismatch` before starting arms. Admission binds the clean receipt to the build, fixture, request, and oracle version, and checks preregistered arms, controls, resources, and endpoints. Cold acceptance remains mandatory; the seconds-scale style oracle is not its replacement.
- **Invented timing or completion:** process-owned events supply run IDs, UTC stamps, monotonic elapsed times, command exits, and receipt references. A valid end stamp depends on the child actually ending and its artifacts being complete. Missing observations remain unknown. The agent supplies interpretation, not stopwatch readings.
- **Stale operational configuration:** the managed session launcher can refuse a claimed-ready program whose required prompt plate or watcher configuration does not match its pinned version. A disabled or stale watcher must be visibly disabled or stale, never healthy because its cron entry exists.

I would **not** ship “Surgeon refuses a cold-only loop whenever a warm port exists.” It cannot reliably infer a loop, a live port is insufficient evidence of suitability, and forcing an extra refusal adds precisely the interaction tax we are trying to remove. Automatically run the cheap eligible warm tier; reserve refusals for violated contracts. Explicit isolation checks and final cold proof are legitimate work.

The 1–2 minute target, choice of the next wart, and decision to continue an experiment remain judgment aided by measurements. A cadence watcher should flag a missed boundary once, with evidence and an owner. It should not kill work merely because an hour elapsed.

## Management system: one operational record, several useful views

Keep the executable apparatus repo-owned in clj-surgeon for now, with thin `bin/` entrances over shared logic and a program-of-record document naming contracts and owners. Do not put unrelated management behavior into Surgeon mutation verbs. Do not extract a general fleet framework until a second real program needs the same mechanism.

Use the existing block, cohort, stamp, and inbox machinery where possible. Join them with stable program/block/run/receipt IDs rather than build a replacement event platform. The block ledger records the intended hour cut, objective, acceptance/falsifier, owner, outcome, and explicit extension/stop decisions. The cadence watch consumes that record and process liveness, with a real delivery path, deduplication, and explicit pause/resume state. Its own last successful observation is part of its health.

The cohort launcher owns preregistration and immutable run artifacts. These fresh-caller cohorts are a **timing instrument**, not the next output-wart finder. Acceptance is still a binary gate on every arm; a surprising defect discovered during a cohort invalidates the affected timing claim and returns to the oracle loop. Preserve failures. Do not average invalid arms into a win. Record setup, work-to-acceptance, and reporting tail separately, with the primary endpoint chosen before launch. Today’s reporting tails and changed concurrency show why.

The friction ledger should extend the existing inbox, not create a second queue. Each item needs the receipt, class of obstruction, owner, and the trigger that brings it back into work: recurrence, next eligible task, blocked gate, or a named deadline. A stored complaint with no trigger will not manage anything.

The captain’s log carries interpretation, changed beliefs, decisions, and evidence links. The resume note carries only current state: active block, branches/worktrees, session owner and identity, last trusted receipts, pending proof, next action, and relevant stop conditions. Generate mechanical fields from the operational record; let a human or agent write judgment. The session/resume entrance must surface the current note again after interruption. A pointer buried in a long file is not recovery delivery.

## What I would cut, and what must be added

Cut mechanical duplication in `CLAUDE.md`, repeated startup/client tutorials, a universal hour-block ritual for incidental work, another “house-rules skill” behind optional task matching, and a Surgeon-only enforcement layer. Keep the repo pointer only if the runner’s actual boot path reaches it; provide the corresponding `AGENTS.md` entry or generated equivalent where needed.

Remove contradictions as part of rollout. This checkout’s `CLAUDE.md` explicitly tells agents to start `make nrepl` on failed discovery. The installed `_commands/start-nrepl.md` falls back to `clojure -M:nrepl`, with the very missing-test-classpath risk recorded today. Replacing those routes requires naming the session supervisor as the new owner. An explicitly requested human startup command can still delegate to that supervisor. “Agents never start” cannot mean that no component is responsible for starting.

The doctrine-versus-prompt rule is already written in `_doctrine/house-rules.md`, under “Doctrine cannot silently disagree with the prompt.” Invoke it; do not invent a parallel promise. Inventory affected accounts/runners and blocks, regenerate and install the projections, verify exact installed content against current intent, record source commit and hashes, and keep a functioning drift watch. Announce changes to affected seats. The historical text mentions `main`; deployment must respect today’s frozen-main policy and the approved working-branch revision. Experiments with intentionally empty global prompts need an explicit pinned exception, not a fleet sweep that changes their controls.

“Every seat imports it” is an acceptance claim to demonstrate at the effective boot surface. Proposed rollout acceptance should also demonstrate a fresh caller using the supplied client without authoring one, a wrong/stale port producing the intended result, warm failure skipping cold execution, probe-only receipts failing completion admission, and dirty oracle output preventing timing admission. These are future acceptance criteria, not checks performed in this advice task. Measure complete iteration wall and retain the native control; do not declare a universal performance win from installation success.

Sources read: [Cell C result](/home/forge/src/clj-surgeon-records/docs/observations/2026-09-07-plan-2-cellC-result.md); [captain’s log, last six entries](/home/forge/src/clj-surgeon-records/docs/observations/2026-09-03-captains-log-anvil-seat.md); [round-three in-image brief](/var/tmp/forge/plan2/cellC/brief-astra-round3-inimage.md); [shared doctrine](/home/forge/opt/claude-skills/_doctrine/house-rules.md); [nREPL command guidance](/home/forge/opt/claude-skills/_commands/clojure-nrepl.md); [startup command](/home/forge/opt/claude-skills/_commands/start-nrepl.md); [ratchet definition](/home/forge/opt/claude-skills/linked-intent-testing/SKILL.md); [local repository guidance](/home/forge/src/clj-surgeon-astra-consult/CLAUDE.md). The shared checkout’s installation coverage across other seats was not audited.

| Artifact | Location | Who reads it when | Why not elsewhere |
|---|---|---|---|
| Short conditional behavior contract | `claude-skills/_doctrine/house-rules.md`, projected into managed boot blocks | Every supported seat at boot/resume | Optional skills cannot establish a universal obligation |
| Plate generator, verifier, delivery receipts | Existing routing/deployment tooling; per-seat installed blocks | Installer and session admission; drift watcher afterward | A doctrine commit does not update effective instructions |
| Session owner and supplied client | Existing seat/session launcher, shared installed executable | Supervisor before work; worker on connection | Workers should not pay setup or own shared process lifetime |
| Test-ready JVM composition | Each Clojure repo’s `Makefile` and dependency aliases | Session owner at provisioning | Test dependencies and runtime limits are repo-specific |
| Warm/proof verifier and typed receipts | Shared verification implementation; repo probe/final targets; Surgeon adapter | Every probe, mutation verification, and completion gate | Receipt truth must hold for native and tool callers alike |
| nREPL technique and recovery guide | Canonical nREPL guidance, packaged for each runner; short skill pointers | Agent when Clojure verification/recovery is relevant | Boot text should not become a client manual |
| Output oracle and fixture loop | Owning repo’s fixture/oracle apparatus, with regression witnesses | Every repair iteration; cohort admission | Fresh callers are too costly for routine wart discovery |
| Block/cadence/stamp/cohort machinery and program contract | clj-surgeon `bin/` entrances, shared logic, program-of-record doc | Program operator, scheduler, launcher | Program operations do not belong in language skills or mutation APIs |
| Friction, interpretation, and recovery state | Existing inbox plus program-owned ledger, captain’s log, resume note | Trigger delivery, block boundary, handoff/resume | Separate queues and manually recopied clocks create drift |
| Repository entry pointer | Actual `CLAUDE.md`/`AGENTS.md` boot surfaces | Fresh agent entering the repo | One pointer locates local intent; mechanics retain their own owners |
