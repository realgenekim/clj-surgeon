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
