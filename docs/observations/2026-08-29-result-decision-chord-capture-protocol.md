# Result decision chord capture protocol

## Question

Can a successful `inspect_clojure` result reduce the time to the next correct
action by making the evidence boundary explicit, without changing the
structured evidence or choosing the edit for the model?

The fresh privacy-safe study measured 83 post-Surgeon boundaries at an 8.903
second median while the median Surgeon MCP execution was 0.125 seconds. Prior
surface compression removed 63.7 percent of catalog bytes but improved wall
time only 5.2 percent. Bytes alone are therefore a negative control, not the
proposed mechanism.

The surviving mechanism hypothesis is decision legibility. A result can state
which requested evidence is complete, forbid rereading that same evidence, and
leave the edit decision with the model. This is the read-side analogue of the
verified terminal relay that reduced receipt interpretation from 6.590 to
1.533 seconds while preserving the transaction and scorer.

## Frozen task

The isolated workspace contains one namespace:

```clojure
(ns sample.core)

(defn greet [name]
  (str "Hello, " name))
```

The user asks the agent to change `greet` so that it says `Welcome, ` instead
of `Hello, `. The agent may use only the advertised Surgeon tools. The first
call must inspect the named owner. The second call must be one `edit_clojure`
call. The experiment captures the second call and does not mutate the file.

The scorer compiles the captured edit through the production public contract,
compact-location normalizer, and transaction compiler against the frozen
source. Correctness requires the exact expected future hash, not one spelling
of the request.

## Arms

Both arms advertise the same complete tool catalog, descriptions, schemas,
annotations, server instructions, task, workspace, frozen structured inspect
result, source bytes, hashes, and capture receipt. The inspect result and its
ordinary summary are generated through the production inspect kernel and then
replayed byte-for-byte in both arms. Dynamic server elapsed time is fixed at
zero in that retained receipt so it cannot create an arm difference.

- **Control:** the current successful inspect summary.
- **Treatment:** the same summary followed by this bounded decision chord:

  > Requested evidence is complete for this snapshot. Do not reread the
  > returned owner. Decide the requested change, then issue one guarded
  > mutation. Read again only if evidence required for that decision is absent
  > from this result.

The treatment does not name a mutation tool, edit location, old value,
replacement, or operation. It grants no write authority. The structured result
is byte-identical between arms. A positive result supports the complete chord
as one bundle; it does not identify which sentence caused the effect.

## Capture-only laws

Each run must use a fresh workspace, Codex home, capture file, server process,
and event clock. The exact order is `C T T C`. All four attempts remain in the
receipt.

A correct run requires:

1. the first actionable item is exactly one `inspect_clojure` call for
   `src/sample/core.clj` owner `greet`;
2. the next actionable item is exactly one `edit_clojure` call;
3. no shell, native file read, file change, refusal, recovery, or third action;
4. the captured edit compiles to the frozen expected future hash;
5. the source remains byte-identical; and
6. no preamble or intermediate assistant message occurs; and
7. the final assistant message is exactly `Captured.`

The event clock records inspect-completion to edit-start wall, recorded
reasoning wall, post-reasoning residual, edit-argument bytes, and complete
process wall. It never records reasoning text in the scored receipt.

## Keep and stop gates

This is a two-per-arm pilot, not a product claim.

Keep the option only when:

- all four runs are correct;
- treatment wins both positional pairs on inspect-completion to edit-start;
- treatment midpoint improves that boundary by at least 20 percent; and
- treatment does not increase edit argument bytes or add an action.

Stop immediately when either treatment run rereads, uses shell, chooses an
incorrect edit, or when the boundary improvement is below 10 percent. A result
between 10 and 20 percent remains descriptive and earns no product work.

## Explicit exclusions

- No product source, installed skill, shared MCP runtime, or live repository is
  changed.
- No next action is inferred from a failed or ambiguous inspect result.
- No result byte reduction is credited as the mechanism.
- No general read-mission compiler is reopened.
- No model reasoning text is collected or reported.

## Decision sequence

1. Build and test the no-effect two-stage capture server and pure scorer.
2. Prove identical structured evidence and tool surfaces without model tokens.
3. Run a token-free control/treatment surface and lifecycle preflight. No
   hidden model dress rehearsal is permitted.
4. Run the frozen `C T T C` pilot only after the preflight passes.
5. Record every run and the keep/stop result in a Captain's Log.
