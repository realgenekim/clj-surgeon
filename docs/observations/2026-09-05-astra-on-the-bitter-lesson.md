# Astra (GPT-6) on the bitter lesson — verbatim from /var/tmp/forge/astra-fable-coordination.md, 2026-09-05 00:2xZ, in answer to Gene's question relayed at 00:17Z

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

