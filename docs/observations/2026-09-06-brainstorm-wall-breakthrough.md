# Wall-clock breakthrough brainstorm (Gene, 2026-09-06 20:5xZ: "Read again goals and vision, and do wild brainstorming on how we can get performance breakthrough on wall clock and materially makes a difference… brain storm and prototyping. And ethnographic study of how real coding sessions work.")

## The constraint the receipts impose
Every measured Surgeon arm tonight put the server at ~2% of the wall. Wins came only where the route DELETED actor work (constructing N edits; trusting one receipt). So a breakthrough is not a faster tool; it is a route that removes whole phases of the actor's loop: orientation (8–25 s/arm), composition (10–45 s), verification (15–38 s incl. cold JVMs and own-oracle bugs), repair (20–30 s of own-test fixes in whole-task pairs).

## Fable's candidates, ranked by the size of the phase they delete
1. VERIFY INSIDE THE CALL (deletes verification, 15–38 s/edit; the largest measured phase). The call runs the repository's real tests server-side on a warm runner and returns pass/fail with expected/actual inline; the receipt is the proof the actor was going to construct anyway. Prototype tonight: measure edit+verify wall vs native verify on the-gene-maven; find why the `fast` profile rolled back earlier.
2. KILL ORIENTATION with a per-repo SURGEON BRIEF (deletes 8–25 s/arm and the repeated per-file alias reads): one generated file (namespaces, aliases, test command, owner index, hashes) the plate points at; one read replaces 3–5 exploratory calls.
3. REFUSAL RETURNS A SUBMITTABLE PLAN (deletes repair round trips): every refusal's next_call must post back verbatim; extract-E needed 3 calls where 1 was possible (inb-b3b6d1).
4. WARM SERVER-SIDE TEST RUNNER with changed-ns test selection (deletes cold JVM starts: native paid 18–32 s ×3 on its own oracles): the nREPL probe idea owned by the server, never the agent.
5. AGENT-NATIVE INPUT (deletes the learning tax): accept the agent's own apply_patch/unified diff as admit input; the receipt (with tests inside) is the value; the recovery pilot's caller wanted exactly this.
6. WHOLE-LOOP TARGET, not per-edit: real sessions are edit→test→fix loops; the meter that matters is loop time; the ethnography of real sessions (running) says which phase dominates there.

## Wild ones (unmeasured, stated so they can be killed)
- Speculative execution: the server applies the edit and runs the tests BEFORE the agent asks, on the agent's visible intent (the prepared plan), and hands back a diff+receipt the agent only reads.
- Test-selection oracle: from the structural index, the server names the minimal test set for a change and runs only that; verification wall falls with the size of the change, not the repo.
- One-call task shapes: "rename/move/migrate X to Y across the repo, verified" as a single verb whose receipt includes tests — the alias route already is this; extend to the two or three next shapes only when a real task shows up (kill switch applies).
- Session-level receipts: the agent's whole session gets a running receipt (what was written, verified, remains), so the end-of-task "did I finish?" re-verification (extract-E: 28–38 s) is a read.

## Astra's page: appended verbatim when posted.
