---
name: clojure-fast-feedback
description: "The fastest Clojure red→green loop: a session-long warm nREPL carrying the test alias, reload + affected tests as the probe, one cold suite as the proof, an oracle for output quality"
user-invocable: true
---

# Clojure fast feedback — the sublime loop

> Gene, 2026-09-07: *"Fixture hygiene is a tax… let's fix this: fix deps.edn to include test stuff. And try again? (this is making for perfect dev env!!! And most
> perfect sublime REPL driven development style that could / should be codified to benefit all coding agents.)"*

This guide separates dated observations from workflow policy and proposed tooling; `SWEEP.md` beside it links the evidence and its limits. `$SKILL` is this skill's own
directory; its one-shots are in `$SKILL/bin/` (see `bin/README.md`).

## COLD START — a fresh agent entering a Clojure repo, in order

1. `ls .nrepl-port` — a port file is a **handshake, not an identity**.
2. Attest it: `clj-nrepl-eval -p $(cat .nrepl-port) '(System/getProperty "user.dir")'`. A root that is not this worktree is somebody else's JVM — do not reload into it.
3. Absent or mismatched, **and you hold task authority to own one**: `$SKILL/bin/run-bg nrepl make nrepl`, then `until [ -s .nrepl-port ]; do sleep 1; done`, then
   re-attest. **Never write your own nREPL client** — `$SKILL/bin/install-clj-tools` supplies `clj-nrepl-eval`.
4. Loop, affected namespaces only: `(require 'app.thing :reload)`, `(require 'app.thing-test :reload)`, `(clojure.test/run-tests 'app.thing-test)`.
5. Once, on the final candidate: the repo's cold gate (`make test`).
6. Report one receipt line — namespaces reloaded, test/pass/fail counts, and whether it came from the **probe** (`verification_complete=false`, named `proof_pending`
   gates) or the **proof**.

## 1. The principle: feedback latency sets batch size

Sublime is not a mood; it is **loop latency** — intent → edit → evidence → next intent, fast enough that running the test stops being a decision.

- **The disappearance test** (2026-07-03, Gene's first use of the word, dictated at mile one of a 5K): a loop is sublime when the tool disappears. If you can still name
  the tool while working, the loop is not finished.
- **Latency is judged against the wait it replaced** (Gene, 2026-08-16: *"omg, it works, but fucking sublime after waiting for what seemed an eternity"*). Report the
  new loop time beside the old one, always.
- **Sublime is a standard you can lose** (Gene, 2026-08-31). Once a loop is called sublime, changes to it are breakage-only, and its scars are formalized as enforced
  promises rather than rewritten.

**The economics, without a new ritual.** Savings ≈ `displaced cold probes × (cold probe wall − complete warm probe wall) − incremental setup/recovery`. Put transport,
reload, lease wait, fixtures, oracle and interpretation in the appropriate clocks; retain final acceptance on both sides. An image provisioned before the task can have
zero incremental startup *for that task* while still costing the session resources — report amortization, never "startup is free".

**How to choose.** For iterative work, use the repo's ready, attested, warm-eligible probe entrance; otherwise use the existing focused cold check. **No fixed iteration
count establishes a crossover**, and ten iterations do not make an unsafe image suitable.

**Where warm did not pay.** plan-2 Cell C, 2026-09-07: on a refactor whose plan was *supplied*, REPL arms with an already-warm JVM ran 513/554 s against native's
408/486 s — six concurrent arms against an earlier four-concurrent native wave, differing source and configuration, and those arms still authored clients. It refutes
*"prewarming automatically wins"*; it does **not** isolate the REPL's causal cost. Astra: *"Do not mandate a REPL just to manufacture its saving."* The measured rows
and their caveats live in `SWEEP.md`.

## 2. Repo setup: the repository owns the entrance

**The repository owns `make nrepl`, a test-dependency-complete environment, and the tests eligible to run there. Loadability does not authorize tests to mutate shared
dev data**; effectful or startup-sensitive checks use declared fixtures or a separate process.

```makefile
.PHONY: nrepl test-probe
nrepl: ## Example, not a universal recipe: session-long dev REPL that can load every test ns
	clj -M:test:nrepl        # when the test alias is deps+paths only
	# clj -M:nrepl           # when the test deps live on :nrepl itself — see the trap
```

**The trap: `:jvm-opts` accumulate; among aliases supplying `:main-opts`, the last wins.** The obvious `clj -M:run-tests:nrepl` shipped and was wrong: `:run-tests` set
`-Dmvr.data.dir=target/test-data`, so the warm dev REPL came up on the test data directory (marvin-voice-remote, 2026-09-08). Prefer reusing a deps/paths-only alias
over duplicating dependency versions across aliases.

- **Attest the resolved classpath and the declared effective properties and data roots.** A composed alias is a claim; the running JVM is the receipt.
- **Verify the repo's required boundary checks with a known pure failing input** — a flag alone is not evidence. House rule: Guardrails on and `{:throw? true}` in dev
  (`clojure-guardrails`). The cited MVR run did **not** meet it (warm Guardrails off, cold on): a configured-run observation, not evidence for the prescribed loop.
- **Never redefine the cold gate.** Astra: *"Do not silently change `make test` to mean a focused warm run. That would improve a clock by weakening an existing
  meaning."*

**`make test-probe NS=app.thing` — a proposed contract; use an implementation only where declared.** It uses the repo's declared source-to-tests mapping: attest → lease
→ reload source/tests/consumers → fixture-aware tests → applicable oracle → cleanup → **one candidate-bound receipt**. It never starts a cold suite. Success is
`probe-passed`, `verification_complete=false`, with named `proof_pending` gates. Refusals are typed, name expected-versus-observed identity and the recovery owner, and
each carries a runnable next call — request/profile invalid; warm unavailable, workspace/session/environment mismatch, reload unsupported, busy, source conflict;
selection unknown or no tests; fixture unsafe or reset failed; reload/test failed; timeout or transport error (execution is **unknown** without terminal server
evidence); oracle failed or unavailable; proof required or stale. Completion admission — not the probe — rejects absent or mismatched proof.

## 3. The session: who owns the JVM

**The managed session launcher owns the JVM; workers connect.** In a standalone task the agent may own the JVM it starts under task authority. Record canonical root,
process/session identity, generation, environment fingerprint and recovery owner. **Only that owner starts, replaces or stops it.** The verifier serializes
reload/test/reset under an image lease; source checks use a separate stable basis. **Unknown or tainted image state is not authoritative.**

```bash
$SKILL/bin/install-clj-tools                    # bbin + clj-nrepl-eval + clj-paren-repair, idempotent
clj-nrepl-eval --discover-ports                 # or read ./.nrepl-port
clj-nrepl-eval -p $(cat .nrepl-port) '(System/getProperty "user.dir")'
```

- The supplied client is **`clj-nrepl-eval`** (clojure-mcp-light) — **install with `$SKILL/bin/install-clj-tools`; never write your own bencode client.** In the
  2026-09-07 warm wave, observed connect intervals of 21 s and 60 s included client construction.
- **A `.nrepl-port` file is a handshake, not an identity.** Attest `user.dir` before trusting it; a stale port file will happily let you reload into another workspace.
  Astra: an unattested REPL is *advisory*.
- Keep **one** hot nREPL and at most **one** cold verification JVM. Memory pressure is strongly implicated in apparent tool latency — 2026-08-10, the same *class* of
  write transaction fell 83 s → 9 s after ~6 GB was freed (not a controlled repeat). Check available memory and paging, not `uptime` alone.
- Seat quirks (Buster's `XDG_RUNTIME_DIR`, `/var/tmp` over `/tmp`) live in `bin/README.md` with a dated reproduction.

## 4. The loop

```clojure
;; Body-only example; the repo runner owns dependency closure and fixture policy.
(require 'app.thing :reload)
(require 'app.thing-test :reload)
(clojure.test/run-tests 'app.thing-test)
```

Prefer the repo's runner: it preserves suite hooks, exclusions and fixture-aware Var selection. This snippet does **not** compute affected tests and does not produce
the receipt. Deletions, moves, macro consumers and state/type identity changes need the declared reload/reset lifecycle — configure refresh roots, and stop owned
resources before unloading. Unsupported or failed reload returns to the owner or to an isolated check.

- **Write code the loop can reload.** For the house Datastar web stack, retain named Var handlers (`defn handle-<route>`, referenced as `#'handle-x`) and verify a real
  request or rendered update after reload; long-lived streams may need explicit lifecycle handling.
- **Simulate before you mutate.** Evaluate the pure transformation on a captured representative value; effectful reproduction uses an owned disposable fixture. Preserve
  the observed counterexample as a direct test. (`_commands/clojure-nrepl.md` owns the technique in full.)
- **Keep evidence as data.** Keep snapshots, selections and candidate alternatives as immutable values; batch related image questions into one bounded result. Image
  metadata describes *that generation*; source analysis describes its declared source coverage. Run kondo through `~/bin/clj-kondo`. When already available, Portal /
  `tap>` can expose bounded immutable observations; direct structured results remain the receipt.

**A test watcher can silently lie.** Gene, 2026-09-06: *"kaocha test watcher is often sublime, but certain category (or plural) require restarting it… Suspect a
runbook."* A watcher is a probe. These change classes need it **restarted** before its green means anything: new, renamed, deleted or moved namespaces; deps or
classpath changes; macro changes; protocol, record or type changes; fixture or runner-config changes. Restart on those classes automatically and **say so in the
receipt** — the runbook is a preregistered change-class × watcher-correctness table, not folklore.

**What a warm green cannot prove.** Stale Vars and `defonce` survive reload; captured function values, direct-linked calls and macro expansions keep pointing at the old
code; reloaded protocols leave existing instances on the old implementation; `remove-ns` is not isolation; a `:clj` load never proves the `:cljs` branch. Classloader
and system properties persist for the whole session — **the scar**: `curtaincall-cfp` `test/…/blob_durability_test.clj:43` calls `System/setProperty` inside a `deftest`
with no fixture, so any long-lived REPL that runs the suite leaves `io/blob`'s upload root on `target/test-uploads` for every later run.

**So: proof is separate.** Run each required cold and platform gate successfully **on the final candidate**; reuse an existing receipt only when candidate, environment,
configuration and gate scope match. Failed or changed candidates require fresh applicable proof, and **probe success never completes acceptance.** JVM-startup,
classpath and isolation defects may need a focused cold check immediately — that is diagnosis, not automatic cold chaining after an ordinary warm failure.

## 5. Tool and library development in the image

This is where the warm loop wins outright: many iterations, each cheap. **Keep the compiler loaded and iterate against a fixture** — `reset fixture → run the
transformation in-image → ORACLE over the output → warm run-tests`.

- **Build the oracle before the third cohort, not after.** Gene, 2026-09-07: *"I find this arduous full loop absurd to fix paper cuts -- that should be verified in
  nREPL to find the other paper cuts immediately in 1-2m, not 10m!!!"* An oracle (`$SKILL/bin/papercut-oracle`) is a read-only, no-JVM, no-test predicate over the
  output tree answering only *does this read like the host codebase?* — docstring clones, kondo unused/unresolved, continuation alignment, require layout and indent,
  blank ns lines, stale prose. It says **nothing** about correctness, counts **baseline-relative**, never mutates the graded tree, and **pins its own version and
  baseline in the receipt**: a score that improves while the grader changes is not a measured repair.
- **Use the bounded oracle for routine output repairs; admit timing cohorts only after their declared output and acceptance gates pass. Preserve unexpected defects
  found by fresh callers as new witnesses.**
- **The reviewer's probe and the builder's witness are ONE test file in the repo. A reviewer that finds a defect writes the red test; the builder makes it green. No
  design rule travels as prose between them.** (Scar: Sol r3 "column" → Astra "column" → Sol r4 "structure", 40 minutes.) Astra: *"a green suite cannot substitute for a
  faithful counterexample."*
- **The meter runs on the oracle-clean tip immediately. Review gates LANDING, never measurement.** (Scar: wave 4 waited ~50 min for reviews it did not need — Gene:
  *"Has to be done by now, right???"*)
- **The routine, once per batch** — `$SKILL/bin/round`: `LOCK → PREFLIGHT → BUILD (bound to the builder's child pid) → COMMIT CHECK → GRADE (fresh fixture + oracle) →
  WITNESSES (focused cold namespaces) → RECEIPT`. Program-specific; it owns builder supervision and batch grading, so **inspect its stated coverage and structured check
  results before accepting its receipt — a commit state is not test proof.** When a round takes 45 minutes for 3 minutes of machine work the wall is in the **serial
  chain** (re-orientation, LLM review, landing gates), not the iteration: run those once per batch.

### Grading the loop — is it actually sublime?

- **Assess from the action log, never by asking the agent whether it felt fast** (Gene, 2026-09-06). **The apparatus share is the meter**: count your own last ~100
  actions; if more than half are apparatus — harness scripts, receipt parsing, process polling, git hygiene — the loop is not sublime however fast the tests are
  (observed: ~80% of 128 commands). **Grade in three places separately** — reads, edits, and the loop around them: *"sublime on reads, partial on edits, not on the
  loop."*
- **Intent and receipts only.** Every action that is not "describe the change" or "read the answer" is a missing verb; every refusal carries a runnable `:example` next
  call.
- **Decisions, not commands** (Astra): *"Automating an action without removing a decision can merely hide it."* Count decisions removed, not commands removed. And **a
  prediction is not a result**: benchmark a proposed fast path against the **current** baseline on matched fixtures, including freshness and recovery costs.
- **The inner-rung target is ~50 ms** from edit to a real test namespace's verdict; above ~1 s the test becomes a decision again. **The wrong-belief term**: the loop's
  value is (iterations × cold cost) **plus every wrong belief it prevented** — that term never appears on the wall clock, so state it explicitly when a warm arm loses.
- **Pair tweezer work with a live watcher at the meter** (Gene, 2026-09-02): *"feels good"* and *"was actually faster"* are two separate claims, and both must be
  recorded.

## 6. Surgical edits: pick the right lever

- **Use the applicable repository routing contract.** A supported whole-intent operation (`namespace_split`, `alias_migration`, `require_change`, `within` +
  `from`/`to`) can remove repeated work; **its existence alone does not establish a performance win.**
- **Ordinary edits use native discovery and a native patch**, unless an eligible witnessed route or an explicit task instruction applies. **A plan-supplied whole
  refactor is verb work, not REPL work** — the shape where warm arms lost; the REPL's contribution there was *discovery* and *parse-error feedback*, not typing.
- **Tweezers where taste is dense** (2026-08-17): hand-and-model tweezer work wins exactly where the boundary is taste-dense and undiscoverable-until-rendered;
  specifiable work is delegated.
- **The bitter-lesson split** (Astra): spend model capability on judgment and ordinary source generation; spend deterministic machinery on permissions, identity,
  bounds, scheduling, evidence and reversible writes.
- **Do not add an unconditional warm re-verification after a valid operation receipt.** `safe-refactor` and `clj-surgeon` own the levers; this skill owns the loop
  around them.

## 7. Anti-patterns not already stated above

| anti-pattern | the scar |
|---|---|
| Binding a waiter to a handle — `$!`, a wrapper pid, a helper pid, a cwd scan | Four times in 90 minutes on 2026-09-08. Every launcher prints `RUN name=… pid=<the real child> log=…` as its first line, and waiters bind to **that line only**: use `$SKILL/bin/run-bg`. Never `pgrep -f` (it self-matches your own `bash -c`); never wrap a builder in an outer `timeout` (it kills your wait, not the child). |
| Writing the second hand-written brief | That is the tools-perfect trigger: stop, build the one-shot, then continue. Scar: five briefs before `bin/round` existed. |
| Using a cohort to find warts | Two ten-minute rounds of fresh callers learned what a bounded read-only predicate answers in about a second. Gene called it "absurd", correctly. |

Every other scar keeps exactly one canonical row in `SWEEP.md`; do not restate them here.

## 8. Completion

Probe-only success is a legal, named state (`proof_pending` / `verification_complete=false`), never a green. Run the required cold and platform gates on the **exact**
final candidate, and keep fixture acceptance, tool tests, independent review and any user-required gate separate and unwaived. **Report at phase changes only, never per
stamp.**
