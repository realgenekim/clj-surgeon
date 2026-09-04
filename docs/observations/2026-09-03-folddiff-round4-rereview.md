# curtain-call fold-diff-tool 347fe6d3 — Sol executed round-4 re-check: GO-WITH-FIX for the production read at 347fe6d3 (retain the full receipt; the verdict line lacks the fallback caveat) — round 6 launched

GO-WITH-FIX for the mayor to run the production read at `347fe6d3` instead of `2b56a484`, provided the full combined receipt is retained. The bare `VERDICT` line is not sufficient because it omits the baseline-validator fallback caveat.

I did not contact production; `STORE_BACKEND` remained unset.

Gates, all once under `suite-run` with JVMs capped at ≤1g:

- Unit: `1096 tests, 13386 assertions, 0 failures`
- Compile check: passed
- Registry suites: `6/298/0`, `2/606/0`, `1/157/0`
- Six-case checkpoint self-test: passed

Case 3 receipt:

```text
==> baseline   origin/main (00e8f0fae2c19258f0eab008e6b02caa8545591d)
==> candidate  HEAD (347fe6d3c58cc55ec882d900bbdaccce8004d163)
==> checkpoint-source (baseline): candidate-fallback (own copy lacks: validate)  6b23cb4f5597050208b71d9c81af956d001e7a8e30922d0c7c40f337910fdcba
==> checkpoint-source (baseline) diff-stat: 00e8f0fa… vs 347fe6d3… -- src/cfp_scheduler_killer/store_checkpoint.clj
 src/cfp_scheduler_killer/store_checkpoint.clj | 70 +++++++++++++++------------
 1 file changed, 40 insertions(+), 30 deletions(-)
VERDICT: IDENTICAL — the candidate ref's folds project exactly what the baseline ref's do.
exit=0
```

The temporary JSONL fixture and trace files were removed. Final Git state contains only the two pre-existing `.codex` changes.

1. **(7) CLOSED** — [bin/fold-diff-checkpoint:284](bin/fold-diff-checkpoint:284): current scan produced exactly `{checkpoint-path, validate, write-checkpoint!}`; case 3 used named fallback plus diff-stat and verdict, case 5 named absent-file fallback, and case 6 used `HEAD~1`’s own complete copy.

2. **(8) CLOSED** — [fold_diff.clj:932](src/cfp_scheduler_killer/fold_diff.clj:932): forced invalid-checkpoint rerun printed `REFUSED :checkpoint-invalid — …` on stdout, `==> REFUSED during phase: baseline-emit` on stderr, and exited 2; both driver phases are covered at [bin/fold-diff-checkpoint:394](bin/fold-diff-checkpoint:394).

3. **(9) CLOSED** — [fold_diff.clj:851](src/cfp_scheduler_killer/fold_diff.clj:851): `:sessions` and `:api-keys` are unconditionally unioned with the environment set, while unknown names refuse at [fold_diff.clj:686](src/cfp_scheduler_killer/fold_diff.clj:686) before the first fingerprint or file read.

4. **(a) OPEN robustness gap** — [bin/fold-diff-checkpoint:284](bin/fold-diff-checkpoint:284): today’s `:as checkpoint` namespace form is fully covered, but mechanically changing it to `:as sc` made the alias scan lose both `validate` and `checkpoint-path`, allowing a false “own complete” followed by an exit-3 compile failure.

5. **(b) PARTIAL** — [bin/fold-diff-checkpoint:301](bin/fold-diff-checkpoint:301): synthetic `defmacro`, `declare`, and nested-`do` definitions were all reported missing; this causes conservative, disclosed fallback rather than a false green, but genuine macro/nested definitions violate FOLD-DIFF-013’s “exposes every var” contract.

6. **(c) OPEN** — [fold_diff.clj:603](src/cfp_scheduler_killer/fold_diff.clj:603): case 3’s `VERDICT: IDENTICAL` carries no notice that the baseline was accepted and parsed using the candidate validator; the only caveat is the driver’s earlier stderr line at [bin/fold-diff-checkpoint:346](bin/fold-diff-checkpoint:346).

7. **(d) PARTIAL** — [fold_diff_test.clj:78](test/cfp_scheduler_killer/fold_diff_test.clj:78): syscall tracing proved every resolved open of the symlinked repository `events.jsonl` by the self-test was `O_RDONLY`, with no write-capable open; however, the full unit suite grew the one-row fixture from 265 to 1,336 bytes by appending five session facts through unisolated public-CFP tests such as [public_cfp_test.clj:484](test/cfp_scheduler_killer/public_cfp_test.clj:484), so depending on the shared real JSONL makes the witness order-dependent even though fold-diff itself is read-only.

8. **(e) PARTIAL, exact pair unaffected** — [fold_diff.clj:661](src/cfp_scheduler_killer/fold_diff.clj:661): `known-relations` comes solely from the candidate’s `store/empty-state`; a baseline-only relation explicitly requested for redaction is therefore refused before reading. `origin/main` and `347fe6d3` currently have identical empty-state relation sets, but reverse/older-candidate comparisons can reject a legitimate baseline relation.