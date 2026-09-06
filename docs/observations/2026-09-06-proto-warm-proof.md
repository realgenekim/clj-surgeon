# Prototype hand-drive: warm proof via the :hot (nREPL) verification profile (2026-09-06 21:3xZ; measurement only; receipts /var/tmp/forge/proto-hot-fx/)

Contract exists: mcp_hot_verify.clj; profile shape {:hot {:port-file ".nrepl-port" :reload [ns…] :tests [ns/var…] :timeout-ms 100..60000}}; must ride the fast/full enum slots (a profile named "hot" is refused). Service setup charged separately: nREPL JVM to port file ~12 s (+ one-time ~30 s jar download); first hot call's namespace load folded into H1.

| # | mode | timeout | client wall | server ms | result | detail |
|---|---|---|---|---|---|---|
| H1 | hot, first call (cold ns load in the warm JVM) | 60,000 | ~61.2 s | 61,107 | ok | {:test 3 :pass 11}, pid, cwd, reload/law counts |
| H5 | hot, warm | 3,000 | 3,187 ms | 3,077 | ok | same |
| H6 | hot, warm | 700 | 903 ms | 792 | ok | same |
| H8 | hot, warm, breaking edit | 700 | 1,693 ms | 778 | failed + rolled back | full expected/actual + stack; source_unchanged true |
| C | cold reference (python3 proof/run.py gate) | — | 2,164 ms | 2,053 | ok | exit code + elapsed only |

HEADLINE DEFECT (inb-adcc9e): every passing hot call's elapsed equals its :timeout-ms — verify! doall's the nrepl client's response seq, which never terminates on "done", so the timeout is a fixed per-call COST, not a ceiling; real work ≤ ~0.8 s (H6 passed inside 700 ms; H8's failure path returned at 778 ms). Second paper cut (inb-d064a5): the verify enum and the strict "exact" shape turn a config typo into an mcp-adapter-failure.
NOT equivalent to the cold gate: hot ran exactly the three named test vars (11 assertions) in the app JVM after requiring two namespaces with :reload; it did not run the seed's 8 test namespaces, lint, format, or compile anything outside :reload; JVM state carries over between calls (earlier edits stayed loaded; no tools.namespace refresh, no fresh classloader); an edit outside the :reload list is invisible to it. Rollback hygiene works (reload-after-rollback! re-requires with :tests []).
VERDICT: a warm real-test verification with pass/fail detail at ~0.8 s vs 2.05 s cold is available (≈60% per verified call) ONLY with :timeout-ms set near the expected work and only for a named-var subset; at the natural 60 s setting it is a 30x regression. Not a warm equivalent of the cold gate — a warm equivalent of a focused test-vars call. Charge service setup separately; disclose state contamination.
