NO-GO

CLASS: every writer deriving a destination from `probe-state/state-root` that reaches filesystem creation/publication without `probe-state/admit-state-root!` on the selected root and final path; oracle: enumerate those writers and drive the STATE-HOME-009/010 inside-workspace/outside-envelope matrix, requiring typed refusal and zero created bytes.

F1 — Namespace-wall state bypasses the state-home admission fence. [battery_parallel_runner.clj](/home/forge/src/clj-surgeon-fence/test/clj_surgeon/battery_parallel_runner.clj:66) derives the destination, while [write-walls!](/home/forge/src/clj-surgeon-fence/test/clj_surgeon/battery_parallel_runner.clj:975) directly executes `make-parents` and `spit`. A `clojure -M:clj-surgeon/test-deps -e` fixture comparing the established admission function with the candidate writer printed:

```clojure
{:inside-admission :state-root-inside-workspace,
 :inside-candidate-write {:result :wrote, :exists true},
 :outside-admission :state-root-outside-envelope,
 :outside-candidate-write {:result :wrote, :exists true}}
```

Thus `CLJ_SURGEON_STATE_HOME` can direct a successful battery to create state inside its checkout or outside a narrowed envelope. This violates the stated pressure-point contract and the existing STATE-HOME-009/010 fence.

Other pressure points:

1. `nl -ba ~/bin/receipt-chain` shows the installed chain sets `BATTERY_LEDGER_APPEND=1` at line 98, checks exactly `+1/-0`, candidate SHA, and passing verdict at lines 116–132, then commits it. Entry fields and append position remain unchanged. `/var/tmp/forge/ship-v3.12/receipt-chain` lacks the variable but retains the delta guard; it stops with typed `LEDGER DELTA REFUSED` before committing—not at `battery-fresh`, and not as a silent stale-receipt pass.

2. `rg -n BATTERY_LEDGER_APPEND` found the sole production decision at [battery_ledger.clj:102](/home/forge/src/clj-surgeon-fence/test/clj_surgeon/battery_ledger.clj:102). Only exact string `"1"` appends. Consequently, a stray exact export can dirty the ledger; it is ambient authority, not bound to receipt-chain provenance. The non-minting output is unambiguous: `battery-ledger: not appended (BATTERY_LEDGER_APPEND unset) {...}`.

3. The walls file is intentionally box-wide per seat, not workspace-keyed: every checkout resolves the same `STATE_HOME/battery/namespace-walls.edn`. The design treats it as acceptance-neutral scheduling data. Seed fallback is read-only, but destination admission is missing as shown above.

4. `make --no-print-directory census-regenerate` exited 0 and printed `census-regenerate: +0/-0`. `JAVA_TOOL_OPTIONS=-XX:DefinitelyInvalidOption make --no-print-directory census-regenerate` exited 2 with `Could not create the Java Virtual Machine` and printed no `+0/-0`; JVM failure propagates. The counts are computed from parsed derived/ledger sets, and the removal witness passed.

5. A correct test-side impact oracle would select:

   - `clj-surgeon.battery-ledger-test`
   - `clj-surgeon.battery-parallel-test`
   - `clj-surgeon.lane-manifest-test`

   The builder’s retained `focused-final.log` names all three and reports `92 tests / 2,312 assertions, 0 failures, 0 errors`. My sealed-tree command running those exact namespaces exited 0 with `92 tests / 2,313 assertions, 0 failures, 0 errors`.

No repository patch was left; `git diff HEAD --numstat` is empty.

> END RECEIPT (fence-run): worktree HEAD at review exit = fe707171674e31c5184dab0458628f778468e7dc = fenced sha.
