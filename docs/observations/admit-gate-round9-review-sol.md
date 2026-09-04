## NO-GO

1. **BLOCKING — a failed recovery battery writes a receipt that the fast witness accepts as a fully satisfied precondition.** `test/admit_transaction_recovery_battery.clj:105,148-149` records `:arms-passed` and writes the receipt unconditionally before exiting nonzero; `test/clj_surgeon/admit_patch_test.clj:5404-5417` checks only the union of `:kinds-published`, target, and timestamp. It never checks `:arms-passed`. I forced only the n=8 arm to fail while leaving the real entrance receipt untouched. The battery was red at 2/3 and wrote `:arms-passed 2`; the complete fast lane then ran 762/10553/0, printed **0 preconditions skipped**, and exited 0. This is worse than the deliberate fresh-clone green-with-an-asterisk: the red battery's archive suppresses the asterisk. A real timing miss in one arm can have the same union-of-kinds shape when either other arm publishes `:transaction-recovery-required`. The fast witness must reject a receipt unless every declared arm passed (or the battery must not write a satisfiable receipt on failure).

   Exact fixture change and battery command (working directory `/var/tmp/forge/gate9-review-fx/broken-battery`):

   ```text
   git diff --check && git diff -- test/admit_transaction_recovery_battery.clj && ~/bin/suite-run make admit-transaction-recovery-battery; battery_rc=$?; echo BATTERY_EXIT_CODE=$battery_rc && sed -n '1p' target/admit-transaction-recovery-battery-receipt.edn
   ```

   Verbatim output:

   ```text
   diff --git a/test/admit_transaction_recovery_battery.clj b/test/admit_transaction_recovery_battery.clj
   index 2af95b61..6773c75e 100644
   --- a/test/admit_transaction_recovery_battery.clj
   +++ b/test/admit_transaction_recovery_battery.clj
   @@ -117,7 +117,8 @@
              ;; @spec MCP-OP-ADMIT-138
              _ (when kind (swap! observed-kinds conj kind))
              enumerated? (contains? admit/admit-refusal-kinds kind)
   -          hit? (and (false? (:ok receipt))
   +          hit? (and (not= n 8)
   +                    (false? (:ok receipt))
                        (= :transaction-recovery-required kind)
                        (not (true? (:source-unchanged receipt))))]
          (cond
   java -cp "$(clojure -Spath -A:clj-surgeon/mcp-test)" clojure.main test/admit_transaction_recovery_battery.clj
   Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge
   WARNING: Use of :main-opts with -A is deprecated. Use -M instead.
   Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge
   FAIL n=8 attempts=3 kind=:transaction-recovery-required source-unchanged=false enumerated=true wall-ms=51
   PASS n=32 attempts=1 kind=:transaction-recovery-required source-unchanged=false enumerated=true wall-ms=144
   PASS n=64 attempts=1 kind=:transaction-recovery-required source-unchanged=false enumerated=true wall-ms=185
   admit-transaction-recovery-battery: 2/3 arms passed
   battery receipt · target/admit-transaction-recovery-battery-receipt.edn · kinds #{:transaction-recovery-required}
   make: *** [Makefile:215: admit-transaction-recovery-battery] Error 1
   BATTERY_EXIT_CODE=2
   {:target "make admit-transaction-recovery-battery", :script "test/admit_transaction_recovery_battery.clj", :at "2026-09-04T14:50:20.257007318Z", :arms [8 32 64], :arms-passed 2, :kinds-published #{:transaction-recovery-required}}
   ```

   Exact fast-lane command, run immediately afterward in that same fresh export:

   ```text
   ~/bin/suite-run clojure -M:clj-surgeon/mcp-test; gate_rc=$?; echo EXIT_CODE=$gate_rc
   ```

   Verbatim terminal output:

   ```text
   Ran 762 tests containing 10553 assertions.
   0 failures, 0 errors.
   0 preconditions skipped.
   EXIT_CODE=0
   ```

2. **PASS for the intended absent/present states; the counted skip is an honest fast-lane result only while it cannot be cleared by a failed receipt.** `test/clj_surgeon/admit_patch_test.clj:33-77,5404-5430` gives the skip its own counter and prints the path plus clearing command on the summary line. `Makefile:975-988` makes `make test` own the battery before `mcp-test`, and the source witness goes red if the executable target line is removed. A fresh clone spent the identical 10,553 assertions in both states: absent was exit 0 with one loud skip; after a genuine 3/3 battery it was exit 0 with zero. I rule that design acceptable in principle: the raw fast suite is explicitly incomplete, while `make test` owns the timing battery. Finding 1 is blocking because a red battery currently turns that explicit incompleteness into an unqualified zero-skip green. Also fix the stale executable comment at `test/admit_transaction_recovery_battery.clj:21-23`, which still says the battery is deliberately not wired into `make test`.

   Exact fresh-clone command before the battery (working directory `/var/tmp/forge/gate9-review-fx/fresh`):

   ```text
   ~/bin/suite-run clojure -M:clj-surgeon/mcp-test; gate_rc=$?; echo EXIT_CODE=$gate_rc
   ```

   Verbatim terminal output:

   ```text
   Ran 762 tests containing 10553 assertions.
   0 failures, 0 errors.
   1 preconditions skipped.
     SKIPPED · no battery receipt at target/admit-transaction-recovery-battery-receipt.edn · run `make admit-transaction-recovery-battery` to prove :transaction-recovery-required by execution rather than by the structural checks alone
   EXIT_CODE=0
   ```

   Exact genuine-battery and repeat command:

   ```text
   ~/bin/suite-run make admit-transaction-recovery-battery; battery_rc=$?; echo BATTERY_EXIT_CODE=$battery_rc && ~/bin/suite-run clojure -M:clj-surgeon/mcp-test; gate_rc=$?; echo MCP_EXIT_CODE=$gate_rc
   ```

   Verbatim output (battery plus terminal suite summary):

   ```text
   PASS n=8 attempts=1 kind=:transaction-recovery-required source-unchanged=false enumerated=true wall-ms=156
   PASS n=32 attempts=1 kind=:transaction-recovery-required source-unchanged=false enumerated=true wall-ms=220
   PASS n=64 attempts=1 kind=:transaction-recovery-required source-unchanged=false enumerated=true wall-ms=234
   admit-transaction-recovery-battery: 3/3 arms passed
   battery receipt · target/admit-transaction-recovery-battery-receipt.edn · kinds #{:transaction-recovery-required}
   BATTERY_EXIT_CODE=0
   Ran 762 tests containing 10553 assertions.
   0 failures, 0 errors.
   0 preconditions skipped.
   MCP_EXIT_CODE=0
   ```

   Exact ownership-sabotage command after deleting only `Makefile:987`:

   ```text
   ~/bin/suite-run bash -lc 'exec java -cp "$(clojure -Spath -A:clj-surgeon/mcp-test)" clojure.main run-admit.clj'; gate_rc=$?; echo EXIT_CODE=$gate_rc
   ```

   Verbatim terminal output:

   ```text
   Ran 165 tests containing 4239 assertions.
   1 failures, 0 errors.
   1 preconditions skipped.
     SKIPPED · no battery receipt at target/admit-transaction-recovery-battery-receipt.edn · run `make admit-transaction-recovery-battery` to prove :transaction-recovery-required by execution rather than by the structural checks alone
   {:test 165, :pass 4238, :fail 1, :error 0, :precondition-skipped 1, :type :summary}
   EXIT_CODE=1
   ```

3. **PASS — the MCP-OP-ADMIT-150 RED→GREEN pair is genuine.** `test/clj_surgeon/admit_patch_test.clj:5437-5450` parses the `test:` recipe and requires the recovery target name; `Makefile:987` is the GREEN executable line. At RED `eaf98c54`, the focused suite fails its ownership assertion; at GREEN `c6ff3014`, the same 164 tests and 4,222 assertions pass with the expected one fresh-clone skip.

   Exact command at each SHA export:

   ```text
   ~/bin/suite-run bash -lc 'exec java -cp "$(clojure -Spath -A:clj-surgeon/mcp-test)" clojure.main run-admit.clj'; gate_rc=$?; echo EXIT_CODE=$gate_rc
   ```

   Verbatim RED output at `eaf98c54`:

   ```text
   Ran 164 tests containing 4222 assertions.
   1 failures, 0 errors.
   1 preconditions skipped.
     SKIPPED · no battery receipt at target/admit-transaction-recovery-battery-receipt.edn · run `make admit-transaction-recovery-battery` to prove :transaction-recovery-required by execution rather than by the structural checks alone
   {:test 164, :pass 4221, :fail 1, :error 0, :precondition-skipped 1, :type :summary}
   EXIT_CODE=1
   ```

   Verbatim GREEN output at `c6ff3014`:

   ```text
   Ran 164 tests containing 4222 assertions.
   0 failures, 0 errors.
   1 preconditions skipped.
     SKIPPED · no battery receipt at target/admit-transaction-recovery-battery-receipt.edn · run `make admit-transaction-recovery-battery` to prove :transaction-recovery-required by execution rather than by the structural checks alone
   {:test 164, :pass 4222, :fail 0, :error 0, :precondition-skipped 1, :type :summary}
   EXIT_CODE=0
   ```

4. **PASS — MCP-OP-ADMIT-151 now witnesses the order, not merely the published receipt's self-description.** `src/clj_surgeon/mcp_admit_tool.clj:2401-2406` takes the next-call-only cheap branch; `test/clj_surgeon/admit_patch_test.clj:6718-6819` proves the fixture is 35,455 bytes with the call and 11,355 without it, preserves every other key verbatim, and retains the converse ladder arm. Replacing that branch with `bounded (reduce-receipt-to-budget faced)` produced exactly five focused failures, as claimed.

   Exact sabotage command (working directory `/var/tmp/forge/gate9-review-fx/order-sabotage`, after the shown one-line semantic replacement):

   ```text
   git diff --check && git diff -- src/clj_surgeon/mcp_admit_tool.clj && ~/bin/suite-run bash -lc 'exec java -cp "$(clojure -Spath -A:clj-surgeon/mcp-test)" clojure.main run-admit.clj'; gate_rc=$?; echo EXIT_CODE=$gate_rc
   ```

   Verbatim diff and terminal output:

   ```text
   diff --git a/src/clj_surgeon/mcp_admit_tool.clj b/src/clj_surgeon/mcp_admit_tool.clj
   index 1ad0d23b..c7a69522 100644
   --- a/src/clj_surgeon/mcp_admit_tool.clj
   +++ b/src/clj_surgeon/mcp_admit_tool.clj
   @@ -2398,11 +2398,7 @@
            ;; self-description, and a receipt that pays for a call it is about
            ;; to drop describes itself perfectly honestly. The witness therefore
            ;; asserts that every key other than the call survives VERBATIM.
   -        bounded (if (and (:next_call faced)
   -                         (not (public-faces-fit? faced))
   -                         (public-faces-fit? (dissoc faced :next_call)))
   -                  faced
   -                  (reduce-receipt-to-budget faced))]
   +        bounded (reduce-receipt-to-budget faced)]
        ;; @spec MCP-OP-ADMIT-139
        ;; The oversize decision is taken AFTER reduction, on the receipt that
        ;; would actually be published: if something STILL will not fit once every
   Ran 165 tests containing 4239 assertions.
   5 failures, 0 errors.
   1 preconditions skipped.
     SKIPPED · no battery receipt at target/admit-transaction-recovery-battery-receipt.edn · run `make admit-transaction-recovery-battery` to prove :transaction-recovery-required by execution rather than by the structural checks alone
   {:test 165, :pass 4234, :fail 5, :error 0, :precondition-skipped 1, :type :summary}
   EXIT_CODE=1
   ```

5. **PASS — all claimed nominal gates reproduce at the requested tip.** `Makefile:195-202,214-220`; `src/clj_surgeon/mcp_intent_contract.clj:159`; `test/clj_surgeon/admit_patch_test.clj:1`. The inherited round-seven receipt behavior was not changed by this delta: `8b4f88d2` adds only comments in the runtime namespace, while `c6ff3014` changes Make wiring/docs and the rest is witness code. The complete fresh-clone MCP run, focused admit suite after a good battery, Babashka suite, operation oracle, intent audit, and bounded-memory analyzer all match the builder's claimed counts. These greens do not cure finding 1 because that exact fast gate also accepted the red battery's receipt.

   Exact focused command:

   ```text
   ~/bin/suite-run bash -lc 'exec java -cp "$(clojure -Spath -A:clj-surgeon/mcp-test)" clojure.main run-admit.clj'; gate_rc=$?; echo EXIT_CODE=$gate_rc
   ```

   Verbatim output:

   ```text
   Ran 165 tests containing 4239 assertions.
   0 failures, 0 errors.
   0 preconditions skipped.
   {:test 165, :pass 4239, :fail 0, :error 0, :type :summary}
   EXIT_CODE=0
   ```

   Exact Babashka command:

   ```text
   ~/bin/suite-run bb test/run_all.clj; gate_rc=$?; echo EXIT_CODE=$gate_rc
   ```

   Verbatim terminal output:

   ```text
   Ran 814 tests containing 6724 assertions.
   0 failures, 0 errors.
   EXIT_CODE=0
   ```

   Exact oracle command:

   ```text
   make mcp-operation-oracle; gate_rc=$?; echo EXIT_CODE=$gate_rc
   ```

   Verbatim output:

   ```text
   # @spec MCP-OP-ORACLE-001
   swipl -q -f test/mcp_operation_contract_oracle.pl
   mcp-operation oracle: pass; legacy counterexamples=[verification_failed,verification_pending]
   EXIT_CODE=0
   ```

   Exact audit command:

   ```text
   ~/bin/suite-run clojure -M -e "(require '[clj-surgeon.mcp-intent-contract :as audit]) (let [r (audit/audit-current-repository)] (prn {:ok (:ok r) :specs (count (:specs r)) :violations (count (:violations r))}))"; gate_rc=$?; echo EXIT_CODE=$gate_rc
   ```

   Verbatim output:

   ```text
   Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge
   Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge
   {:ok true, :specs 371, :violations 0}
   EXIT_CODE=0
   ```

   Exact memory command:

   ```text
   ~/bin/suite-run make admit-analyzer-memory-self-test; gate_rc=$?; echo EXIT_CODE=$gate_rc
   ```

   Verbatim output:

   ```text
   clojure -J-Xms64m -J-Xmx512m \
     -Sdeps '{:deps {nrepl/nrepl {:mvn/version "1.3.1"}}}' \
     -M test/admit_analyzer_memory_selftest.clj
   Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge
   Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge
   PASS n=100 findings=600 analyzer-bytes=83606 ran=true introduced=300 heap-start-MiB=24 heap-peak-MiB=35 budget-MiB=409 max-heap-MiB=512 wall-ms=70
   PASS n=1000 findings=6000 analyzer-bytes=847706 ran=true introduced=3000 heap-start-MiB=23 heap-peak-MiB=30 budget-MiB=409 max-heap-MiB=512 wall-ms=236
   PASS n=10000 findings=60000 analyzer-bytes=8596706 ran=true introduced=30000 heap-start-MiB=23 heap-peak-MiB=105 budget-MiB=409 max-heap-MiB=512 wall-ms=1902
   admit-analyzer-memory-self-test: 3/3 arms passed at -Xmx512m
   EXIT_CODE=0
   ```

6. **PASS — provenance, delta order, merge shape, and cleanup reproduce.** `Makefile:55` is the sole merge conflict. `HEAD` is exactly `8b4f88d28f82cd75d8e5b608a398f3add57ad671`; the round-nine order is RED `eaf98c54`, GREEN `c6ff3014`, then GREEN witness `8b4f88d2`. Against current `origin/MCP/main` at `078bc7ec`, `git merge-tree --write-tree` reports only the claimed Makefile `.PHONY` conflict. No server was needed; ports 8144–8146 were clear. The review worktree remained clean, and `/var/tmp/forge/gate9-review-fx` was removed.

   Exact log command:

   ```text
   git log --oneline --reverse 94e4a00c..8b4f88d2
   ```

   Verbatim output:

   ```text
   eaf98c54 RED MCP-OP-ADMIT-150: the fast gate must own or count its battery precondition
   c6ff3014 GREEN MCP-OP-ADMIT-150: `make test` owns the battery, the fast lane counts its absence
   8b4f88d2 GREEN MCP-OP-ADMIT-151: witness the ORDER of the cheap correct move, not the receipt's account of itself
   ```

   Exact merge command:

   ```text
   git rev-parse HEAD && git rev-parse origin/MCP/main && git show -s --format='%H %s' origin/MCP/main && git merge-tree --write-tree HEAD origin/MCP/main; gate_rc=$?; echo EXIT_CODE=$gate_rc
   ```

   Verbatim output:

   ```text
   8b4f88d28f82cd75d8e5b608a398f3add57ad671
   078bc7ec56c6fe4aa9a1c9b3ea6b3cf253fcf5e2
   078bc7ec56c6fe4aa9a1c9b3ea6b3cf253fcf5e2 replay: T4 pair scored (round-six receipt) — line missed on T4b; residual names a dispatch leg
   baeee379615e685a863c0dca72a87cd8d7d5c5c0
   100644 38494a98e70b30f7634b3be3ba94878fb2dda94c 1	Makefile
   100644 c1845228f4435bd02aca70879dd9815efdee9c2d 2	Makefile
   100644 f12d2750e9953e47479d34ed93ab7aeae8e5b5ae 3	Makefile

   Auto-merging Makefile
   CONFLICT (content): Merge conflict in Makefile
   EXIT_CODE=1
   ```

   Exact final provenance/cleanup command:

   ```text
   git rev-parse HEAD; git status --porcelain; git diff --check; test -d /var/tmp/forge/gate9-review-fx; echo FIXTURE_ROOT_PRESENT=$?; find /var/tmp/forge/gate9-review-fx -mindepth 1 -depth -delete && rmdir /var/tmp/forge/gate9-review-fx; test ! -e /var/tmp/forge/gate9-review-fx; echo FIXTURES_REMOVED=$?; ss -ltnp | rg ':(8144|8145|8146)\\b' || true
   ```

   Verbatim output (`git status`, `git diff --check`, and the port check emitted nothing):

   ```text
   8b4f88d28f82cd75d8e5b608a398f3add57ad671
   FIXTURE_ROOT_PRESENT=0
   FIXTURES_REMOVED=0
   ```

## NO-GO

`8b4f88d2` is not GO on its own for `MCP/main`: the merge-tree has only the trivial Makefile conflict, but a red 2/3 recovery battery leaves a receipt that makes the raw fast gate report zero failures **and zero precondition skips**, so the battery-only enumeration proof can read fully green without its battery passing.
