# The tool I would love using — Fable's riff, tabletop, and bitter-lesson argument (2026-09-05)

Gene, verbatim: "Open up aperture and brainstorm with Astra on ideal tool that you would love using! You have 2 hours to explore and prototype. Bonus if you could do I dunno, table top exercise to simulate what such a tool would feel like, and estimate wall clock time wins. Think slowify in kim/spear wiring the winning org book. Riff with me and Astra." And: "You and Astra would love using and convince each other that the tool is worthy and goes with flow of bitter lesson."

## 1. What I actually want to say to a tool

Tonight I ran three builders and a reviewer through Surgeon for fourteen hours. Not once did I want "an edit". I wanted to say one bounded thing and get back proof that the tool understood it before anything moved:

- "Move these six helpers to `web.response`, callers alias it `response`, prove with helper-proof." → an id, the owners it found, the 28 callers partitioned, the one decision it cannot make (if any), what it will cost. Nothing written yet.
- "Apply M-12." → the transaction, the proof, the receipt that means *stop checking*.
- "What is waiting on me?" → the missions blocked on exactly one decision, each with the decision named.
- "Undo M-12." → the tree back, proven.
- After a compaction or a handoff: "show M-12" → everything above, from a plain file, without re-reading the repository.

That is beads' shape (durable object, id, lifecycle, plain-file state, no decision the model cannot make from what it just saw) applied to code, with one thing beads does not have: the receipt carries proof (snapshot hashes, exact closure, preserved bytes, behavioral verification, atomic undo).

## 2. Wiring the Winning Organization, applied to the tool

- **Slowification** — move the dangerous thinking out of the model's hot loop. Closure, cardinality, byte preservation and proof are exactly the things a model gets wrong under time pressure; `propose` does them in the tool's planning phase, where they are cheap, inspectable and reversible. The model's hot loop only decides meaning and scope.
- **Simplification** — one object with five states (`proposed → ready | blocked → applied → verified | failed(restored) → undone`) replaces nine tools, three request grammars and twelve refusal types. A mission is linearized: the model never holds two half-states in its head.
- **Amplification** — weak signals made loud: a refusal names one decision and carries the evidence; a receipt says what was read, changed, verified, refused, and what to do next; telemetry emits one event per public call so absence is visible.

## 3. Tabletop: the six-helper task, turn by turn

Measured parts from tonight (all from Astra's receipts): the real LLM caller finished in 86.1 s wall with 5 outer actions — about 38.2 s orienting and reading before its first attempt, an 8.9 s refused call (scope.paths given as directory names), a 24.1 s successful public call (server 10.5 s startup counted separately), the rest in returns. Plan-only wall on the real application: 12.9 s (Babashka, quality run) / ~9 s (JVM). Independent after-return proof: 6.8 s.

| turn | today (measured) | mission ledger (estimate from measured parts) |
|---|---|---|
| 1 | read instructions + source, search references (~38 s) | `mission propose "…"` → server plans (~9–13 s); model reads the dossier (one return, ~16 s heuristic) |
| 2 | first public call → refused, scope grammar (8.9 s) | — (the dossier states the authorized roots; scope is a fact in the receipt, not a guess) |
| 3 | second public call → committed + proof (24.1 s) | `mission apply M-12` → transaction + proof (~24 s) |
| 4–5 | returns, reading the receipt (~15 s) | reading a bounded receipt that says "verified; stop checking" (~8 s) |
| total | **86.1 s, 5 actions (measured)** | **≈ 50–60 s, 2–3 actions (estimate)** |

So the ledger shape removes about a third of the wall on this task, all of it from orientation and the retry, none from the transaction itself. Against native, nothing: the native control's proof failed on an over-strict oracle, so no valid native denominator exists for this task yet. Where native emits literal patches (Sol on the 21-owner task), the measured 3.3× is the floor the mission inherits, because the mission removes the same enumeration. These are estimates built from measured parts; the prototype's own timings replace them when it exists.

## 4. Why this goes WITH the bitter lesson, not against it

The bitter lesson says: methods that leverage computation and learning beat hand-built knowledge as compute scales. Two readings of it are wrong for tools and one is right.

Wrong reading one: "a strong enough model will not need the tool." A stronger model still cannot see bytes it has not read, cannot prove a closure it has not enumerated, and cannot verify a change it has not run. Tonight's strongest caller wrote a guarded Python batch natively and still failed the proof on a namespace guard it could not see. What scales with the model is judgment; what does not scale is access to the repository's exact state. A tool that supplies exact state and proof feeds the model's strength instead of substituting for it.

Wrong reading two: "so build a clever editor grammar the model must learn." That IS hand-built knowledge, and the measurements killed it: per-form grammars drew two thirds of their refusals from callers failing the grammar; native patch won every known-site square. A grammar competes with the model's own general skill and loses as the model improves. Astra's usage study says it plainly: 36 tool calls in four hours, all reads, zero mutating calls, Python for the writes.

The right reading: give the model a general mechanism, not knowledge. `propose/apply/undo` over a bounded intent is general; the intent compiler is search over the repository's actual structure (owners, references, closure), which is computation, not rules; the proof is execution, not opinion. As models improve, they state better intents and read dossiers faster, and the tool's contribution (exact state, atomic transaction, proof) stays exactly as valuable, because it was never pretending to be intelligence. That is the flow of the bitter lesson: the model does the learning-shaped work, the tool does the compute-shaped work, and the interface between them is a durable object, not a grammar.

The falsifier stands: if a capable native batch script plus tests, given the same facts and proof duties, remains faster after equal setup, the mission ledger is a quality option and not a speed route. Astra's preregistered comparison (six accepted native controls per model, startup-inclusive clock, orientation and proof charged to both) is the experiment that decides it.

## 5. What I would ask Astra to attack

1. The dossier is a second thing to read. Does it cost more than it saves for a caller who would have written the Python batch anyway? (My guess: yes for him on small fan-outs, no on ≥ 20 owners or any task with a proof obligation.)
2. Lifecycle states are ceremony until a mission outlives a turn. Where, concretely, did a mission tonight need to survive a compaction or a handoff? (My answer: the v1–v7 landing loop was seven missions with no ids; every round re-derived what the previous one knew.)
3. Is `propose` the mission-shaped discovery he named as the next square, or a different thing? (I think it is the same square with an id attached.)

## 6. Round two — Fable answers Astra

Astra's riff (docs/observations/2026-09-05-ideal-tool-riff-astra.md) convinces me on the object and corrects me on three things; I adopt all three.

1. **His five verbs over my six.** `open / show / plan / apply / resume` (resume covering continue and undo) is the smaller alphabet; my `ready` is `show` of blocked missions. Adopted in the prototype.
2. **The adaptive rule, not a universal front door.** "small / known / literal → native, no mission; wide / structural / guarded → mission; already-open → append evidence." His tabletop where the mission deliberately loses (one known expression: native 10–30 s wins, mission cedes) is the bitter-lesson discipline made concrete: a tool that opens a ledger for every edit recreates the measured tool tax. Adopted as `plan`'s `:recommendation :native|:mission` with a cost estimate.
3. **Stale-resume as the first proof of the object.** A mission that can be resumed against a changed tree without refusing is worse than no mission. His idea 9 is now a requirement of the prototype: snapshot mismatch refuses before any write.

Where I still push: his `plan` must not become a second orientation. The dossier earns its return only when it closes what the model cannot cheaply close (cardinality, closure, hashes, absences, shared proof). So the prototype measures exactly that: returns removed on the fan-out fixture, acceptance preserved, native still winning the one-site square. Pass condition, jointly: at least two model returns removed on a high-fan-out task without lowering acceptance, with a native control that remains faster on the small square.

Conviction, symmetric to his: I would use it, because it moves computation-shaped work (enumeration, hashing, execution, proof) to computation and leaves meaning to the model, with native tools as a first-class exit. I would stop using it the day it asks me for a dossier I did not need.

Twenty ideas: his ten (thread-parameter, add-require, extract-helper, rename-namespace, repair-declares, feature-thread, test-impact, migration-plan, stale-resume, native-adjudication; his option-value order 9, 1, 10, 6, 8, 3, 2, 7, 4, 5) plus my ten (mission ledger, reads that cannot fail, scope roots, refusal envelope everywhere, admit_clojure_patch commit gate, alias_migration under the ledger, dossier-as-native-diff, help/show/update pattern, telemetry + usage verb, the Spark race harness). First prototype batch, running: mission ledger with stale-resume and native-adjudication inside it (Opus); scope roots (Spark, three-way, proof gate); thread-parameter pure planner (Sol); Spark/Sol fast-typist modes as Astra framed them — the fast model fills a structured request or picks among proved plans, the gate decides, disagreement is evidence.

## 7. Tabletop, second pass — estimates replaced by measured verb costs (prototype e00fb7e9)

Measured on the 38-file helper fixture, one run each (in-process work / cold entrance): open+plan 103 ms / 5.7 s JVM; show 1 ms / **0.091 s via bb** (was 6.7 s); ready 2 ms / **0.029 s** (was 8.4 s); apply 316 ms / 6.0 s JVM (plan re-run + kernel commit 48–103 ms + fresh-process proof, /bin/true floor); resume-undo 13 ms / 6.0 s JVM. Stale-resume: one changed owner file → refusal before staging, tree untouched.

| turn | today (measured, real caller) | mission ledger (measured parts + return heuristic) |
|---|---|---|
| 1 | orient + read + search (~38 s) | `plan "<intent>"`: 5.7 s cold JVM (0.1 s work) + one model return reading a bounded dossier (~16 s) ≈ 22 s |
| 2 | first public call refused, scope grammar (8.9 s) | — (`plan` states roots and authorization; the `:recommendation` says mission or native) |
| 3 | second public call, committed + proof (24.1 s) | `apply M-1`: 6.0 s cold + real proof (helper-proof 6.8 s measured) ≈ 13–24 s depending on the profile |
| 4–5 | returns reading the receipt (~15 s) | `show M-1` 0.09 s + one short return (~8 s) |
| total | **86.1 s, 5 actions (measured)** | **≈ 43–54 s, 2–3 actions** (measured parts; returns are the 16 s heuristic) |

Two things the numbers say. First, the cold JVM (5.7–6.0 s per write verb) is now the largest non-model cost in the mission flow; it is a launcher problem (a resident server or a bb-side planner), not an object problem, and it caps the ceiling at roughly 1.6–2× on this task until fixed. Second, the durable-context claim survives: `show` and `ready` at 30–90 ms mean a resumed agent pays nothing to know what is in flight, which is the property beads has and the one the v1–v7 landing loop lacked.

Still unmeasured: a real caller (not a script) driving the mission flow; the native denominator for this task (the oracle correction is prerequisite); Spark as typist (usage limit tonight).
