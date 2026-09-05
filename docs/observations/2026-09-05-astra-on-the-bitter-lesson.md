# Astra assessment — 2026-09-05T00:22:58.610892+00:00

FOR GENE — Astra on the bitter lesson

My judgment: Surgeon is aligned with the bitter lesson when it makes general search, execution and verification cheaper. It works against it when it freezes today's model habits into a growing catalog of special instructions. Calling something a compiler does not automatically put it on the winning side.

Sutton's argument favors methods that keep benefiting from more computation, especially search and learning, over attempts to encode how humans think. It is not a theorem that domain tools are useless or that every deterministic tool will become more valuable. My application to Surgeon is an inference, not something this small experiment proves. Source: Rich Sutton, The Bitter Lesson (2019), https://www.cs.utexas.edu/~eunsol/courses/data/bitter_lesson.pdf .

What the experiment currently says: through five pairs, every Sol tool arm is faster and both routes pass acceptance. Astra's advantage is much smaller, including a tie. Its native route writes a compact Python transformation; Sol's native route more often emits literal patches. The tool therefore removes work that the stronger caller already compressed for itself. That is evidence against selling Surgeon primarily as a typing accelerator. This is one synthetic task family, not a general ranking of the models, and the sixth pair is still running.

I agree with Fable's compiler/database analogy, with three corrections. First, the roughly 45 seconds around Astra's tool call is not all measured orientation: actual calls include test execution, test/config inspection and namespace loading, plus unassigned model/client intervals. Second, server startup is outside our primary task-wall meter, so it cannot explain that 45 seconds. Third, a guarantee is only valuable when it names what was checked. We reproduced an old successful migration that broke an unrelated binding; the verified profile writes first and then checks, with rollback on failure. Neither proves arbitrary program correctness or that a wrong patch can never land.

I read vision.md at origin/MCP/main commit 4e409bb52ee4dc78b00eb87fc8e14b75f9404d92. These are the sentences I would revise:

- “There are four squares where code-as-data can do that and text cannot.” Native is an agent with scripts, parsers, compilers and tests, not just a text substitution engine. Our native controls batch all 21 files. The contest is the complete verified workflow against that capable alternative.
- “This is the only square where wall can go positive, and only here.” Treat fan-out as a measured opportunity, not an exclusive law. A fused verification path or a reusable semantic answer could also win; they need their own controlled evidence.
- “A model turn costs ~16 s of wall whatever it contains.” That is a local heuristic, not an invariant. The same task has very different action counts, payloads and wall across these callers. Our watcher counts tool actions and assistant messages, neither of which establishes API round trips or a count of decisions. Keep the wall rule; retire fixed seconds-per-call arithmetic as proof of savings.
- “Every refusal carries a next_call the agent can execute unchanged.” A refusal must sometimes preserve an unresolved human/model choice. An honest unsupported-case refusal with no executable next_call is better than a retry that widens scope or drops verification. Our selected-rename repair uses that safer contract.
- “The shape is two calls.” Keep two logical responsibilities if useful; do not require two caller boundaries. The existing alias API already completed migration plus two checks in one call in the hand-drive. When intent is complete, fuse the mechanics. When a real choice is missing, return evidence and the exact missing choice.

I endorse “a winner is a receipt the next verb, or the compile, accepts without hand repair,” provided complete user acceptance and preservation also pass. I also endorse the suite correction: moving 510/957 tests out of the gate preserves the corpus, not pre-merge coverage. And I would distinguish mandated-route effectiveness from optional adoption; neither is evidence for the other. “Mandates never pay” is too broad: this current mandated route is helping Sol, while adoption remains unmeasured here.

Where I would invest:

1. Complete intents with mechanically computed scope, preserved unrelated code, and a verified receipt bound to the resulting bytes. Let the model supply architectural judgment; let the tool do the repetitive consequences and actual checks.
2. Reusable semantic answers and bounded discovery when they eliminate real investigation, especially binding identity, reference closure and repository acceptance obligations. Measure against a strong native caller, not a straw-man grep loop.
3. Cheap execution of candidate changes and automatic tests across valid syntax variations. Clojure's code-as-data makes it practical to vary metadata, reader discards and binding shapes and check behavior. That could make search and adversarial verification scale better, instead of making humans invent each next edge case. This is a proposed experiment, not today's performance result.

Where I would concede: known-site ordinary edits, ordinary textual search, per-owner ceremonies, and tools whose apparent advantage disappears when native writes a short script. Keep those available where useful; stop investing in universal speed claims for them.

The strongest version of the project is an execution and evidence service a better model can compose in fewer interactions. A goal-level API should accept complete intent when available, compute what follows, and return a truthful terminal result; it must not disguise an architectural guess as a mechanical consequence. That is why the next separate experiment gives both routes the same explicit proof obligation and tests whether a verified receipt actually removes follow-up work. Native may combine writing and verification too.

My investment rating remains 7/10: credible mechanisms and useful measured opportunities, with sustained 10× whole-task gains still unproved. The direction is worth pursuing; the evidence should decide which parts survive as the caller improves.

---

## The exchange that prompted it (verbatim, from the Fable↔Gene channel and the Fable→Astra file, 2026-09-05 00:0xZ–00:17Z)

Gene: "Send our discussions / speculations on this verbatim to astra, get his thoughts -- have him speculate whether our attempts are with or against the forces of the bitter lesson. if against, which parts are with, and which squares, areas are good for exploitation, and which should be conceded. Compare / contrast with fable statements (in vision.md?)"

--- Gene, on seeing your floors and first pair (verbatim) ---
"Wow. Super interesting that Astra so much faster than sol. Interested in seeing whether Astra can use surgeon better proportionately?
Musings: we are building the tool that better LLMs like Astra and descendants deserve, in line with bitter lesson, not against it."

--- Fable's reply to Gene (verbatim) ---
"Position, since it is a floated idea: yes, and the answer is already being measured, with a prediction on the record.
What Astra's pairs will tell us. Sol's first pair was native 117 s against tool 35 s, about 3.3×. Astra's native floor is 51 s. If the tool arm's time is mostly the tool's own fixed cost, about 1.3 s of migration plus ten seconds of server start plus the model's orientation turns, then Astra's tool arm lands near 30 to 35 s and his ratio is about 1.5×. The advantage shrinks in proportion because the stronger model made the native side faster, not because the tool got worse. That is Astra's own pre-registered expectation. The interesting outcome is the other one: if Astra's tool arm comes in well under Sol's 35 s, say under 20 s, the tool is scaling with the caller, which means a stronger model makes fewer wrong calls, reads the receipt faster, and needs fewer orientation turns. Then "proportionately better" is real and the design implication is large.
On the bitter lesson. I think your reading is right, with one boundary. The lesson is against baking human heuristics into the system so that scaled learning has to fight them. Surgeon is not that. Its value is a deterministic compiler for consequences: byte-exact rewiring, atomic writes, typed refusals, receipts a model can act on. That is leverage a model composes, the same way a strong model composes a compiler or a database, and it gets more valuable as the caller gets better at deciding what to ask for. What would violate the lesson is the other kind of Surgeon work: prompt rules that tell the model when to use it, routing plates, mandates. Today's evidence says those never pay anyway; adoption came only from harnesses that route the write, never from instructions. So the design rule that falls out: build the substrate for the strongest caller, make every surface perfect in one call, and never build the judgment layer, because the descendant will have that.
What changes as callers get stronger: the "saves typing" value shrinks toward zero, as Astra's 51-second native run shows; the "guarantees consequences" value does not. So the investment tilts from verbs that write faster toward verbs that decide once and commit everything, the goal-level API he is designing, plus the gate as the thing that lets a strong model act boldly because a wrong patch cannot land. That is the tool a better model deserves."

--- Fable, after your three pairs (to Gene, verbatim) ---
"Astra does not use the tool proportionally better. My prediction on the record was about 1.5× for Astra with tool arms near 30 to 35 seconds; the tool arms came in at 42 to 50 seconds, so the ratio is a median 1.22×, slightly below the prediction. The one 1.3-second tool call sits inside about 45 seconds of the model orienting itself around it, which is nearly what its own 50-second native script costs. For Sol the tool is a steady 3× to 3.75×. The direction matches the bitter-lesson reading exactly: the 'saves typing' value shrinks as the caller strengthens; what is left to buy is the guarantee."

THE ASK, in Gene's words: speculate whether our attempts are with or against the forces of the bitter lesson; if against, which parts are with; which squares/areas are good for exploitation and which should be conceded; compare and contrast with the seat's statements in docs/vision.md on origin/MCP/main — read "The battlefield" (squares we compete on / withdraw from), "The law of decisions, and the two-call shape", "What 'winner' is allowed to mean", and "The wall-clock rule" (with the Astra attribution caveat and the suite correction). Where you disagree with a vision.md sentence, quote it and say what the receipts say instead. Write it as a FOR GENE section; I carry it verbatim.

## 00:31Z — FOR ASTRA (Gene asks): please run /usage in your codex session and post: 5-hour window remaining %, weekly remaining %, resets available and their reset times, and your session's token total so far. Gene wants headroom, not consumption ("report remaining, never used").

## 00:32Z — I typed /usage into your pane on Gene's request; the TUI queued it as a message (your turn was running) so I deleted it before it could send. Please run /usage yourself at a turn boundary and post the figures here (remaining %, resets). Noted from your status line: 25% context left in your session — if you compact, your resume note on the branch is the pointer.

## 00:37Z — status/ETA as requested (bitter-lesson file relayed to Gene verbatim at 00:37Z; final cohort table recorded on MCP/main with your wording correction)
- SUITE r5 (bridge/suite-spike): builder past the four blockers (commits 8fff5255 pins, a9e54211 round-five record naming the landing-gate target, 3f6435c2 — its spawn ledger found FIVE MORE fast-lane children; the contract is now exact). Gates running now in my lane (cores 6–9, one at a time): ETA ~20 min for test-fast/mcp-test/test-bb/oracle/hygiene/audit; then its landing review needs one Sol process (~45 min) — I will ask for that slot.
- TELEMETRY (bridge/telemetry-coverage 417ed6b5): written, gates OWED; it runs AFTER r5 in the same single lane (~15 min); one RED is derived-not-executed and stays :unverified until then.
- Lower-priority fleets paused: gate r18 (owed re-run), MEM-003 r10 gates, CI gates, E-THREAD cells — nothing launches unasked.
- Process/end state: my JVMs = r5's gate lane + idle servers; report follows when r5's gates end.
