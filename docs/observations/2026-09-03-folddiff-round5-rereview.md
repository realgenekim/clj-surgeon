# curtain-call fold-diff-tool 885f58b3 — Sol executed round-5 re-check: NO-GO for the read at the tip (three witnesses still read the shared store; requiring-resolve fail-open; DATA_DIR under Postgres) — round 7 launched; the GO at 347fe6d3 stands

NO-GO for the mayor’s production read at `885f58b3`.

Gates:

- `bin/kaocha unit`: **RED** — 1100 tests, 13,397 assertions, 3 errors. All three errors were missing `data/store/events.jsonl`; expected 1100/13,409/0.
- `make compile-check`: green.
- Registry suites: green — 6/298, 2/612, and 1/160.
- `bin/test-fold-diff-checkpoint`: all eight checks green under `suite-run`, private data, and `FOLD_DIFF_XMX=1g`.
- `STORE_BACKEND` remained unset.

Residuals and round-six findings:

1. **(c) CLOSED** — [fold_diff.clj:617](src/cfp_scheduler_killer/fold_diff.clj:617): case 3 printed `[baseline parsed with the CANDIDATE validator: own copy lacks: validate]`; case 6 printed no caveat.

2. **(a) CLOSED** — [fold-diff-checkpoint:97](bin/fold-diff-checkpoint:97): both `:as checkpoint` and scratch `:as sc` returned `checkpoint-path validate write-checkpoint!`; `:refer [validate]` also survived an alias-qualified zero-match.

3. **(b) CLOSED** — [fold-diff-checkpoint:181](bin/fold-diff-checkpoint:181): my scratch recognized `defmacro`, `declare`, `defonce`, and one-level nested-`do`; only the deliberately absent var was reported.

4. **(e) CLOSED** — [fold_diff.clj:730](src/cfp_scheduler_killer/fold_diff.clj:730): a non-empty baseline-only relation was diffed, not skipped; the report named `RELATION :retired-relation-only-on-baseline`, its exact path, and `VERDICT: 1 difference(s)`.

5. **(d) PARTIAL** — [fold_diff_test.clj:78](test/cfp_scheduler_killer/fold_diff_test.clj:78): strace found only `O_RDONLY` opens of the real store, but found three of them; with the file initially absent, the full unit gate errored at callers on lines 786, 818, and 850. Write isolation is closed; independence is open.

6. **OPEN fail-open required-set edge** — [fold-diff-checkpoint:468](bin/fold-diff-checkpoint:468): supported alias/`:refer` zero-matches work, so `|| true` alone did not reproduce the suspected loss. However, a valid dynamic `requiring-resolve` of `validate` produced an empty required set, after which origin/main was falsely “complete”; no non-empty/integrity guard refuses that state.

7. **OPEN operator-clarity edge** — [fold-diff-checkpoint:263](bin/fold-diff-checkpoint:263): `FOLD_DIFF_DATA_DIR` accepts any path without warning. Under Postgres, [fold_diff.clj:328](src/cfp_scheduler_killer/fold_diff.clj:328) still reads Postgres, so the copy cannot divert the production read—but the override is silently ignored rather than refused or warned.

**NO-GO** until the three witnesses stop reading the real JSONL store and the full unit gate reaches 1100/13,409/0.