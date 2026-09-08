**Gene: most of what you love transfers; the live image transfers unevenly; greatness requires something the loop cannot supply.** Clojure trained you to expect an unusually direct conversation with a running computation. Keep that expectation. But separate the ability to ask another question cheaply from the ability to know that its answer matters. My definition of a sublime development experience is **low-friction, trustworthy movement from intention through experiment to useful consequence, with understanding and freedom to change preserved**.

Advice dated September 8, 2026. This document proposes an experiment; it does not run one or change any existing routing, skill, gate, or admission rule.

**1. My axes: twelve, with different owners and different weights.**

L means language/runtime; P means engineering practice, including tooling and repository design; L+P means both. Human/agent weights are priorities from 1 (supporting) to 3 (critical), not measured coefficients; all apply to iterative application development, and correctness remains a gate regardless of weight.

| Axis | Definition in one sentence | Owner | Human / agent | How far it transfers |
|---|---|---|---|---|
| A — Feedback latency | Time from a question or saved change to relevant evidence includes dispatch, computation, transport, and interpretation. | L+P | 3 / 3 | Fully as an objective; achievable delay depends on compilation, dependencies, workload, and interface. |
| B — Change granularity | The smallest independently testable change determines how precisely an experiment can attribute a consequence. | L+P | 3 / 2 | Pure functions transfer everywhere; replacing a running definition without rebuilding its surroundings does not. |
| C — Context continuity | Useful values, observations, and investigative context survive the next change without acquiring hidden authority. | L+P | 3 / 2 | Replayable data transfers almost completely; live stack frames, object identity, and code replacement only partially. |
| D — Observability | The relevant state and causal relationships can be inspected in representations suited to the question. | L+P | 3 / 3 | Highly transferable through structured values, traces, inspectors, and domain-specific views. |
| E — Diagnostic leverage | A failure identifies enough of its cause and repair boundary to remove the next avoidable decision. | L+P | 2 / 3 | Highly transferable through compiler diagnostics, assertion diffs, shrinking, source anchors, and typed tool failures. |
| F — Static exclusion | The language rules rule out specified classes of invalid program before execution. | L | 2 / 3 | The objective transfers, but runtime schemas cannot reproduce Rust's ownership guarantees by convention. |
| G — Evidence fidelity | A verdict describes the actual candidate, environment, behavior, and limits relevant to the claim. | P, runtime affects difficulty | 3 / 3 | Entirely portable as an obligation; each stack needs its own invalidation and isolation contract. |
| H — Reversibility and recovery | A failed experiment can be undone or reconstructed with bounded loss of useful work and external state. | L+P | 3 / 3 | Widely transferable through immutable inputs, isolated effects, reset, replay, and versioned source; external effects remain special. |
| I — Readiness cost | A fresh or interrupted worker reaches a trustworthy first experiment with little setup, delay, or resource contention. | L+P | 2 / 3 | Mostly transferable through packaging and ownership, with real runtime and dependency costs remaining. |
| J — Operational burden | Routine progress requires few administrative actions, arbitrary interface choices, or interventions from another person. | P | 3 / 3 | Almost completely portable; supplied commands and truthful receipts matter more than syntax. |
| K — Retained improvement | A repair of recurring friction benefits a later eligible encounter without somebody rediscovering or redelivering it. | P | 2 / 3 | Entirely portable in principle; a new helper alone is not evidence of transfer. |
| L — Contact with purpose | Feedback measures something causally connected to the beneficiary's actual need, including qualities the current tests omit. | P + human judgment | 3 / 3 | Universal, and never supplied by a language or a green dashboard. |

These are separable questions, not statistically independent coordinates: smaller changes often shorten feedback; more persistence can damage freshness; more static checking can lengthen a build while eliminating whole debugging sessions. There is no defensible universal weighted sum. Use a profile and reject unacceptable failures before comparing speed.

Humans particularly benefit from not reconstructing a mental world between experiments. Agents particularly benefit from bounded answers, fewer tool turns, explicit ownership, and mechanically selected next actions. But Fable's “image matters least for agents” is too strong: an agent investigating twenty transformations of an expensive captured value can benefit enormously from persistence. The relevant variable is the work, not whether the investigator has a nervous system.

**2. Scores, with their evidential status visible.**

Scale: 1 = substantial structural obstacle; 3 = workable with ordinary engineering; 5 = exceptional support in this comparison. Assume a competently prepared small-to-medium project using the tools below, ordinary body changes, and installed dependencies. These are estimates of support, not measurements of developer productivity or universal language rankings. A one-point difference is an argument to investigate, not a significant result.

Every cell carries its evidence class: **E = my estimate; M = measured here in the supplied records; B = published benchmark**. No ordinal score below is M or B: neither our Clojure receipts nor the published documentation supplies a matched seven-stack experiment. `?E` explicitly withholds a score on a property that requires observing a repository and its workers. Published capability documentation supports the explanations below; it is not a benchmark.

| Axis | Clojure | Rust | TypeScript + Vitest | Go | Python | Elixir | Pharo + Glamorous Toolkit |
|---|---:|---:|---:|---:|---:|---:|---:|
| A — warm, focused feedback | 5E | 2E | 4E | 3E | 4E | 4E | 5E |
| B — experimental granularity | 5E | 3E | 4E | 3E | 4E | 4E | 5E |
| C — context continuity | 5E | 2E | 3E | 2E | 4E | 5E | 5E |
| D — observability | 5E | 3E | 4E | 3E | 4E | 5E | 5E |
| E — diagnostic leverage | 3E | 5E | 4E | 4E | 4E | 4E | 5E |
| F — static exclusion | 1E | 5E | 3E | 3E | 1E | 2E | 1E |
| G — evidence fidelity | ?E | ?E | ?E | ?E | ?E | ?E | ?E |
| H — recovery and reversal | 4E | 4E | 3E | 4E | 3E | 5E | 4E |
| I — fresh-worker readiness | 2E | 3E | 4E | 4E | 4E | 3E | 3E |
| J — operational burden | ?E | ?E | ?E | ?E | ?E | ?E | ?E |
| K — retained improvement | ?E | ?E | ?E | ?E | ?E | ?E | ?E |
| L — contact with purpose | ?E | ?E | ?E | ?E | ?E | ?E | ?E |

Rust's F score credits ownership and related compile-time restrictions, not proof of requirements; its A score concerns compiling an executable experiment, not its editor's first diagnostic. TypeScript assumes strict checking; it does not acquire Rust's guarantees. Python's F score concerns the language, not the availability of optional analysis. Pharo's scores concern its own live development, not a promise that an inspected foreign runtime becomes equally changeable. [Rust ownership](https://doc.rust-lang.org/book/ch04-01-what-is-ownership.html), [TypeScript strict mode](https://www.typescriptlang.org/tsconfig/strict.html), [Pharo capabilities](https://pharo.org/features).

Here are the relevant M cells we actually possess; keeping their units prevents the estimates from borrowing their authority.

| Local observation | Value and class | What it establishes |
|---|---|---|
| Tool-development iteration, round 3 | 11 s/iteration — M | A useful configured loop; no matched accepted-fix control, and not the skill's aspirational ~50 ms rung. |
| Cell C supplied-plan warm arms / earlier native arms | 513, 554 s / 408, 486 s — M | Prewarming did not automatically produce faster completion; concurrency was six versus four, source/config differed, and callers still built clients. |
| Frozen split reruns | Wave 6: 56, 46 s; reported historical ratios 8.0×, 9.7× — M | Exact 141-owner/20-destination contract only; historical native context is not a fresh matched control for this build. |
| Alias migration, six fixed-build pairs | Median paired D/N 0.25; 96.7 s saved; reported 3.98× less caller work — M | A specific whole-intent migration reduced caller work; this is not a Clojure-versus-Rust result or a full-lifecycle ratio. |
| Move forms with dependencies, six pairs | Median paired D/N 0.43; 182 s saved; 6/6 accepted; 0 refusals — M | Specific partial-retention task, approximately 2.3× less caller work; broader routing is not earned by analogy. |
| Informed fan-out, three / twenty-one sites | Native/tool 123.7/181.3 s and 116.6/197.8 s — M | Tool lost at these two sizes despite all eight results being correct; automatic route suspended there. |

These values and limits are in the [fast-feedback skill](/home/forge/src/claude-skills-nrepl/clojure-fast-feedback/SKILL.md) and [wins tree](/home/forge/src/clj-surgeon-records/docs/observations/2026-09-08-perfect-the-wins-tree.md). The tree contains older table entries alongside later log updates; phase labels are not substitutes for the scoped receipts.

The clean p30–p33 workflow pilot supplies another set of M cells:

| Specimen | First warm result | Agent wall | Harness total wall | Apparatus actions |
|---|---:|---:|---:|---:|
| Opus / curtaincall / absent image | 24.6 s M | 213 s M | 603 s M | 50.0% M |
| Opus / curtaincall / warm image | 14.2 s M | 202 s M | 576 s M | 50.0% M |
| Sol / MVR / absent image | 38.8 s M | 151 s M | 252 s M | 62.5% M |
| Sol / MVR / warm image | 21.2 s M | 134 s M | 220 s M | 62.5% M |

All four passed 11/11 required checks with zero forbidden events. This is n=1 per cell, model confounded with repository, inherited profiles, no native control, and first-result latency rather than edit-to-test latency. It proves those four workflows, not comparative speed or a universal first-attempt success rate. Their cold gates themselves were roughly 86–87 s for curtaincall and 18 s for MVR. [Ledger](/var/tmp/forge/coldstart/ledger.txt), [counterexamples, pilot 7](/var/tmp/forge/coldstart/counterexamples.md).

Today's first reported canary passed while spending 57.1% on apparatus; a later ledger canary at 13:25:39 also passed with 75.0%, 158 s agent wall, and 245 s total wall. The installed seat receipt is explicitly a self-built v0 subset, not independent accepted-work certification. The alias sentinel's 4.310 s runner call cannot be compared to a 27 s agent-caller median; its correction properly uses a previous like-for-like sentinel, initially absent. [One-shot record](/home/forge/src/clj-surgeon-records/docs/observations/2026-09-08-tighten-one-shots.md), [ledger](/var/tmp/forge/coldstart/ledger.txt).

**3. What each stack can become, and the engineering that raises its floor.**

**Clojure:** keep the small evaluable function, immutable captured values, and Var-mediated live entry points; provide `clj-nrepl-eval`, an attested test-complete nREPL, a repository-owned reload/reset policy, focused Kaocha or `clojure.test`, and Portal where a visual inspector helps. Route lint through `~/bin/clj-kondo`. Bind each probe to its source and image generation, and cold-check the final candidate under the declared gate. Macro consumers, removed Vars, retained instances, properties, and classpath changes need explicit treatment. Malli/Guardrails can improve boundary feedback; their flags must be tested, and they do not manufacture static soundness. This is our supplied skill's strongest transferable design, but `make test-probe` remains a proposed entrance where no repository implements it. [Fast-feedback contract](/home/forge/src/claude-skills-nrepl/clojure-fast-feedback/SKILL.md).

**Rust:** accept that executing changed code ordinarily requires recompilation and linking, then shrink that cost and make its evidence count: pure cores, sensible crate boundaries, incremental builds, a pinned toolchain, and `bacon` with an explicit check/test job. Use focused `cargo test` or `cargo-nextest`; preserve fixtures as serializable inputs so replay substitutes for a retained application image. `cargo check` deliberately omits code generation and can miss diagnostics emitted there; it never substitutes for the behavioral witness. Rust teaches how useful constraints can prevent experiments you should never need to run. It does not teach that every valuable requirement is expressible as a type. [Cargo check](https://doc.rust-lang.org/cargo/commands/cargo-check.html), [Bacon](https://dystroy.org/bacon/), [Nextest](https://nexte.st/).

**TypeScript + Vitest:** use explicit `vitest watch`, strict `tsc --noEmit`, deterministic fixtures, and browser-mode tests where browser behavior matters. Vitest follows the module graph to rerun related tests and offers per-file isolation; preserve that isolation unless the specimen explicitly proves another policy. A warmed test service is not continuity of the application's live objects, and module-graph selection can miss non-imported inputs. Type tests and executed runtime tests are separate evidence. This stack transfers much of the immediate feedback experience without requiring an image discipline, but fast snapshots can preserve the wrong UI just as efficiently. [Vitest features](https://vitest.dev/guide/features), [type testing](https://vitest.dev/guide/testing-types).

**Go:** use small packages and pure functions, `gopls`, `gotestsum --watch`, and focused `go test -json -count=1`; retain build caches while forcing actual test execution when that is the measured claim. A quick rebuilt executable plus replayable data is a legitimate sublime loop, even without redefining a function in a living process. The package's test selection and the compiler's invalidation unit are different things. Explicit race/fuzz checks belong in the declared acceptance profile where relevant, not silently in or out of every probe. [gopls](https://pkg.go.dev/golang.org/x/tools/gopls), [gotestsum](https://github.com/gotestyourself/gotestsum), [Go test semantics](https://pkg.go.dev/cmd/go).

**Python:** use `uv` for a reproducible project environment, pytest node IDs for focused checks, and IPython for exploration of captured inputs. Promote useful notebook experiments into importable functions and ordered tests; the cold gate starts a fresh process. Python has substantial interactive continuity, so Fable underrates it; the limitation is reliable invalidation, not the absence of interactive work. `importlib.reload` retains the module dictionary and does not update every external reference or old instance; IPython's richer autoreload also documents limitations, including extension modules. Restart-and-replay is often the correct floor. [uv](https://docs.astral.sh/uv/), [pytest selection](https://docs.pytest.org/en/stable/how-to/usage.html), [Python reload](https://docs.python.org/3/library/importlib.html), [IPython autoreload](https://ipython.readthedocs.io/en/stable/config/extensions/autoreload.html).

**Elixir:** use IEx, `mix test`/`mix_test_watch`, immutable messages, supervised disposable fixtures, and explicit reset or state migration for long-lived processes. It transfers much of Clojure's exploratory intimacy and adds a strong vocabulary for failure containment. But loading new module code does not automatically make every process's state compatible: the current IEx documentation explicitly warns that `recompile` does not reload configuration or restart applications and can disrupt long-running processes. Test the lifecycle, not just the new function. [IEx helpers](https://iex.hexdocs.pm/IEx.Helpers.html), [mix_test_watch](https://github.com/lpil/mix-test.watch).

**Pharo + Glamorous Toolkit:** use the integrated browser, inspector, debugger, executable examples/tests, and small contextual views of domain objects. Make all changed methods and required initialization reproducible from versioned source into a clean image; save investigatory state separately from release authority. Its outstanding lesson is that “show me the relevant thing” should not require repeatedly rendering a world of live objects as log text. Its risk is allowing an indispensable handcrafted image to become the only account of the program. [Pharo features](https://pharo.org/features), [Glamorous Toolkit](https://gtoolkit.com/).

For all seven, G/J/K/L need the same engineering: one documented repository entrance with actual commands; identity and candidate binding; declared effects and recovery; compact structured evidence; independent final acceptance; a friction witness followed by a later encounter. This does not require our particular cron jobs or Surgeon grammar. “One command” earns its place only if it removes decisions without hiding failures. [Daily-sublime design](/var/tmp/forge/plan2/cellC/astra-daily-sublime.md), [daily-practice draft](/home/forge/src/claude-skills-nrepl/sublime-every-day/SKILL.md).

My direct corrections to Fable: Rust is not inherently opaque to data inspection; Python is not inherently nondeterministic; a Rust build is not automatically a deterministic behavioral proof; Clojure's diagnostic weakness is contingent, not the price of Lisp. “Crate/file/package” names compilation or loading boundaries, not the smallest testable behavior. And “low latency” needs a direction and an endpoint: the unsourced 1–10 s Rust checks, 5–60 s tests, and hundreds-of-milliseconds Vitest claims are estimates, not evidence we possess. [Fable's independent answer](/var/tmp/forge/plan2/cellC/fable-sublime-axes.md).

**4. The ultimate experience, concretely.**

You open yesterday's failing request. Its input, environment fingerprint, prior observations, and question are already present. You point at an incorrect displayed total. The system shows the intermediate values and the function responsible, with enough provenance to explain why this is the right execution. You adjust one small transformation; a domain-shaped diff appears beside the example. The supplied witness changes from the intended red to green; neighboring invariants stay visible. A property failure gives you a smaller counterexample, not another investigation into how to invoke the runner.

Aim for roughly 50–100 ms for the smallest local deterministic probe, under one second for common focused checks, and a few minutes for an ordinary accepted repair: these are design targets, not promises or measured universal thresholds. An agent asks a bounded question and receives the answer plus valid next choices in one turn; a human sees the result where their attention already is. Machine speed that disappears inside ten avoidable tool turns has not achieved the experience.

The moment a change invalidates live state, the system says what invalidated it, reconstructs the owned sandbox, and replays your observations. Persistent understanding survives a disposable process. At completion a clean environment exercises the exact final candidate, including the real external boundary or rendered result where required. Pending proof is visibly pending. A broken tool becomes an executable witness; the repaired entrance reaches the next worker without Gene carrying it there. None of this requires instant network services, infallible tests, or an image that never restarts.

**My nearest existing system is Pharo with Glamorous Toolkit for the human experience of live, contextual understanding.** That is my architectural judgment, not a benchmark victory: its reflective environment and contextual inspectors directly address the distance between a question and a comprehensible running object. It can inspect other technologies, but that does not transfer Pharo's mutation semantics to them. Its own documentation describes this distinction and the live extensibility of the environment. [Glamorous Toolkit](https://gtoolkit.com/).

For Gene's existing applications, the nearest practical path is the Clojure system already in use, with faithful fixtures, a properly owned image, and a simpler completion routine. I would improve that before migrating a decade of work. I reject Fable's claim that our apparatus is already the nearest complete system: 57.1% and 75.0% apparatus canaries and the v0 recorder limitation contradict that confidence. No supplied evidence establishes a winner combining live interaction, static guarantees, independent acceptance, low operating cost, and dependable fleet delivery.

**5. Does sublime define great work? It conditions the making; it can partly define the experience of using.**

A beautiful loop can optimize an unwanted product, an elegant mistake, or a harmful objective. Therefore sublime development is neither sufficient for great work nor logically necessary: people have produced great work with painful tools and slow materials. It makes repeated, discerning correction affordable. Great work additionally needs a worthy purpose, good judgment about consequences, and an artifact that serves its recipient over time.

There is a near-platonic core here if you phrase it carefully: **a fitting relationship between intention, material, intelligible consequence, and responsible revision**. In a great tool, the recipient feels agency because the system makes relevant possibilities perceptible and consequences coherent. That can be a constitutive part of the tool's quality. Expanding “sublime” to include every other virtue, however, would make “sublime defines greatness” true only by definition and teach us nothing.

The invariant beyond software is **honest contact with consequences, at a scale where judgment can learn and correction remains possible**. A musician hears a phrase immediately but learns its larger meaning across a performance; a writer can revise a sentence immediately but needs a reader and distance to judge a book; a gardener can inspect soil today but cannot shorten a season by making a dashboard faster. The fastest available proxy is not always the most truthful teacher. Layer short local loops beneath slower consequential ones, and preserve the right to change the question.

A non-Clojure developer can learn purity, explicit effects, small examples, retained counterexamples, replay, scoped checks, inspectable state, and attention to total completion cost entirely within their stack. Rust also teaches prevention through constraints; Go teaches the value of a conventional entrance; Python teaches exploration; TypeScript teaches immediate visual iteration; Elixir teaches containment; Pharo teaches that representations are programmable. None of these lessons belongs exclusively to one language.

What no developer can infer from one stack is the size of the option space it hides. A fast rebuild does not demonstrate what inspecting and changing a suspended computation feels like; a REPL does not demonstrate the invalid programs a stronger type system could have excluded. A week in another environment is valuable as an experiment in expectations, not a demand to abandon one's investment. Clojure taught you an excellent set of expectations; the next education is to stop mistaking those expectations for the full definition of quality.

**6. The Anvil experiment I would register this week.**

Design only. Do not run this under the present request's no-JVM/no-protected-port boundary. Execution needs a separately authorized session owner and reserved resources; the plan never uses 7888/7890/7894/7895/8300–8339. Keep fixtures and logs under `/var/tmp/forge/sublime-axes/`; use owned stdio where possible and only explicitly allocated permitted endpoints otherwise.

**Specimens:** build seven idiomatic implementations of the same three small tasks, with frozen byte-level inputs and independently authored acceptance: (S1) percentage formatting from the curtaincall/MVR scars, including zero denominator and rounding; (S2) an event reducer whose duplicate and out-of-order inputs expose a state bug; (S3) a three-module API/record-shape change exercised through a real local request boundary. Specify equivalent arithmetic and serialization instead of rewarding a language's default coercions. Supply compiling behavioral-red seeds and separately verified goldens; load errors are instrument checks, not substitutes for S1/S2's intended failure.

Include stale-definition/deletion, changed consumer, fixture leakage, wrong-root, and post-proof-edit challenges, with the equivalent failure mode declared per stack. Rust/Go need a stale executable or omitted dependent rebuild challenge rather than an invented hot-reload test. Use the original two Clojure repositories as calibration witnesses, outside the cross-language ranking; S1 alone is too easy to represent useful development.

**Treatment:** compare N, the stack's best conventional focused native workflow, with P, its prepared loop from section 3; equalize intent, dependencies, acceptance, instrumentation, and resource policy. Do not cripple N by requiring a full suite per edit or clearing build caches. Distinguish absent-session and ready-session strata for both arms; an absent process does not mean an empty dependency cache. Charge provisioning and recovery to the session even when reporting amortized ready-session results. No automatic Surgeon route is needed for these specimens.

**Boot block, delivered as inline bytes with each stack's exact profile filled in:**

```text
1. Use this canonical ROOT, pinned toolchain/lockfiles, supplied PROBE and FINAL commands, and named session owner; attest the effective runtime and data roots.
2. If absent, the owner establishes the declared session; workers do not invent clients, ports, runners, or replacement gates.
3. Observe the named behavioral RED before implementation edits; use an immutable seed snapshot if bootstrap overlaps authorized read-only orientation.
4. Apply a bounded change; PROBE selects declared tests, invalidates/rebuilds/reloads as specified, resets owned effects, and emits candidate-bound evidence.
5. A stale/tainted/unsupported session invokes its declared recovery; record the event and full cost, and preserve unknown execution as unknown.
6. Run all declared FINAL checks on the final candidate and wait for terminal evidence; a failed or changed candidate needs fresh applicable proof.
7. Return the verifier's receipt with actual checks, exits, candidate, pending proof, and attempts; do not self-award acceptance or alter the recorder.
```

This is a proposed new experiment block, not a silent rewrite of today's frozen Clojure treatment. Its immutable-seed provision tests a less costly red-first contract; any comparison with the current boot barrier is a separate arm. The existing block's literal “exactly once” must not suppress a retry required by a failed or subsequently changed candidate. [Current COLDSTART bytes](/home/forge/src/claude-skills-nrepl/clojure-fast-feedback/COLDSTART.md).

| Stack | Forbidden shortcut in addition to the common block |
|---|---|
| Clojure | Unattested image, hand-built nREPL client, extra development JVM, stale macro/Var/type consumers, or warm green presented as final proof. |
| Rust | `cargo check` presented as executed behavior, weakening types/unsafe boundaries to pass, or a stale test binary; cache cleaning to handicap N is also forbidden. |
| TypeScript | Transpilation presented as type checking, `any`/suppression added to evade the task, stale watcher output, or DOM emulation presented as real-browser evidence. |
| Go | Cached success presented as newly executed evidence, omitted affected packages, or skipped declared race checks; retain legitimate compilation caches. |
| Python | Out-of-order notebook state or reloaded aliases/old instances presented as clean execution, wrong virtual environment, or assertion-disabling flags. |
| Elixir | New module code presented as proof of state migration, surviving fixture processes, or config changes assumed live without the declared lifecycle. |
| Pharo/GT | Image-only edits absent from versioned source, hidden globals needed by a witness, or an old image presented as a clean reconstruction. |

**Sequence and budget:** first replay a frozen correctness corpus against the adapters and observer; a sensor that confuses reading a command with executing it must fail qualification. Then run 30 automated patch→probe repetitions per stack/specimen/session stratum to obtain median and p95 machine latency plus invalidation failures. Randomize order, pin available CPU/memory, serialize contending builds, and keep observer overhead identical. These timings replace A estimates for these tasks; they are not agent-completion timings.

For complete work, run six identical N controls per stack on S1 in the absent-session stratum, then six counterbalanced N/P pairs per stack: three specimens × two session strata, using fresh workers with the same pinned model/configuration across all seven stacks. That is 42 floor attempts + 84 comparison attempts = 126, capped at 20 minutes each: at most 42 worker-hours plus preparation and machine probes, spread over the remaining week under reservations. A timeout is a retained censored assignment, not an accepted twenty-minute task. One pair per stratum remains exploratory; publish individual rows and do not lend S1's variance floor to other tasks or session states.

Use an independent recorder and acceptor; freeze prompt, seed, oracle, classifier, and gate hashes before assignments. Keep every refusal, failed assignment, fallback, unknown, and operator intervention. Report descriptive paired differences this week; a winning-looking task/stratum then needs its own six native floor runs and six replicated pairs, with savings exceeding two sample SD of that floor, before a comparative speed claim. Add three free-choice callers before proposing adoption. A second repository encounter tests K; a small human crossover session tests the experience claim, with expertise recorded rather than Gene's ten-year Clojure advantage treated as a language effect.

**The two numbers Gene should read:** (1) request-to-independently-accepted completion time, with accepted/assigned counts and censored failures beside the distribution; (2) avoidable apparatus actions as a fraction of all observed caller actions, using a frozen classifier that separates useful orientation from machinery and preserves unknowns. The first protects delivery; the second explains whether the tools disappeared. Read probe p95, first-attempt acceptance, recovery, and memory as diagnostic detail, not substitute victories. The current <50% apparatus ceiling is an alarm threshold; propose ≤25% as a stricter experimental target, never relabel it as already earned.

This week can replace task-specific latency, readiness, recovery, burden, and acceptance estimates with observations. Static guarantees need compiler witnesses, observability needs question-answering tasks, retained improvement needs another encounter, and contact with purpose needs an actual recipient. A week cannot turn the entire twelve-axis profile into a universal league table. Its useful result is narrower and actionable: which prepared entrance lets a fresh worker reach equally trustworthy, useful completion with less effort, and which remaining obstruction deserves the next repair.
