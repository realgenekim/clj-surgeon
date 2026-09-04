## GO-WITH-FIX

1. **Provenance — the requested detached tip is exact and the review began from a clean worktree.** `HEAD:1`

   Exact command:

   ```text
   git rev-parse HEAD && git status --short --branch && git remote -v && git log --oneline -1
   ```

   Verbatim output:

   ```text
   612ea68c0702292984835d251f714fc9ee713cf1
   ## HEAD (no branch)
   origin	https://github.com/realgenekim/clj-surgeon.git (fetch)
   origin	https://github.com/realgenekim/clj-surgeon.git (push)
   612ea68c GREEN MCP-OP-ADMIT-152: three states — absent skips, complete satisfies, incomplete is RED
   ```

2. **FIX REQUIRED — mixed-type arm keys fail closed, but escape the promised failed-precondition bucket and clearing command.** `test/clj_surgeon/admit_patch_test.clj:134-137` correctly rejects a key set different from the script's `[8 32 64]`, but constructs its reason with `(sort (keys verdicts))`. A receipt containing the requested mixed key attack, `{"8" true, 32 true, 64 true}`, therefore throws `ClassCastException` before `check-battery-precondition!` can call `fail-precondition!`. The process is still non-green, so this does not reopen round nine's false-green hole; however, MCP-OP-ADMIT-152's stronger claim that every present incomplete receipt is counted in `precondition-failures` and printed with the clearing command is false. Sort heterogeneous keys with a total comparator (or do not sort them), and add this exact case to the witness.

   Exact command:

   ```text
   ~/bin/suite-run bash -lc 'exec java -cp "$(clojure -Spath -A:clj-surgeon/mcp-test)" clojure.main /var/tmp/forge/gate11-review-fx/precondition-shapes.clj'; gate_rc=$?; echo EXIT_CODE=$gate_rc
   ```

   Verbatim output:

   ```text
   Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge
   {:case :complete, :result {:state :satisfied, :counters {:test 0, :pass 0, :fail 0, :error 0}}, :report-types {:pass 3}, :skips [], :failures []}
   {:case :permuted-arms, :result {:state :failed, :counters {:test 0, :pass 0, :fail 0, :error 0, :precondition-failed 1}}, :report-types {:fail 1, :pass 2}, :skips [], :failures ["battery receipt at /var/tmp/forge/gate11-review-fx/precondition-receipt.edn is PRESENT but does NOT record a complete run · the receipt declares arms [64 32 8] but the battery script declares [8 32 64] · a receipt may not shrink its own subject · re-run `make admit-transaction-recovery-battery` and make it pass before trusting this lane"]}
   {:case :extra-arm, :result {:state :failed, :counters {:test 0, :pass 0, :fail 0, :error 0, :precondition-failed 1}}, :report-types {:fail 1, :pass 2}, :skips [], :failures ["battery receipt at /var/tmp/forge/gate11-review-fx/precondition-receipt.edn is PRESENT but does NOT record a complete run · the receipt declares arms [8 32 64 128] but the battery script declares [8 32 64] · a receipt may not shrink its own subject · re-run `make admit-transaction-recovery-battery` and make it pass before trusting this lane"]}
   {:case :string-key, :result {:threw "java.lang.ClassCastException", :message "class java.lang.String cannot be cast to class java.lang.Number (java.lang.String and java.lang.Number are in module java.base of loader 'bootstrap')"}, :report-types {}, :skips [], :failures []}
   {:case :missing-verdicts, :result {:state :failed, :counters {:test 0, :pass 0, :fail 0, :error 0, :precondition-failed 1}}, :report-types {:fail 1, :pass 2}, :skips [], :failures ["battery receipt at /var/tmp/forge/gate11-review-fx/precondition-receipt.edn is PRESENT but does NOT record a complete run · the receipt records no per-arm verdict (`:arm-verdicts`), so it cannot show that every arm passed · it reports :arms-passed 3 of 3 · re-run `make admit-transaction-recovery-battery` and make it pass before trusting this lane"]}
   {:case :contradictory-count, :result {:state :failed, :counters {:test 0, :pass 0, :fail 0, :error 0, :precondition-failed 1}}, :report-types {:fail 1, :pass 2}, :skips [], :failures ["battery receipt at /var/tmp/forge/gate11-review-fx/precondition-receipt.edn is PRESENT but does NOT record a complete run · the receipt says :arms-passed 2 but declares 3 arms · re-run `make admit-transaction-recovery-battery` and make it pass before trusting this lane"]}
   {:case :contradictory-verdict, :result {:state :failed, :counters {:test 0, :pass 0, :fail 0, :error 0, :precondition-failed 1}}, :report-types {:fail 1, :pass 2}, :skips [], :failures ["battery receipt at /var/tmp/forge/gate11-review-fx/precondition-receipt.edn is PRESENT but does NOT record a complete run · the receipt's verdict is :failed, not :passed · re-run `make admit-transaction-recovery-battery` and make it pass before trusting this lane"]}
   {:case :older-shape, :result {:state :failed, :counters {:test 0, :pass 0, :fail 0, :error 0, :precondition-failed 1}}, :report-types {:fail 1, :pass 2}, :skips [], :failures ["battery receipt at /var/tmp/forge/gate11-review-fx/precondition-receipt.edn is PRESENT but does NOT record a complete run · the receipt records no per-arm verdict (`:arm-verdicts`), so it cannot show that every arm passed · it reports :arms-passed 2 of 3 · re-run `make admit-transaction-recovery-battery` and make it pass before trusting this lane"]}
   {:case :unreadable, :result {:state :failed, :counters {:test 0, :pass 0, :fail 0, :error 0, :precondition-failed 1}}, :report-types {:fail 1, :pass 2}, :skips [], :failures ["battery receipt at /var/tmp/forge/gate11-review-fx/precondition-receipt.edn is PRESENT but does NOT record a complete run · the receipt could not be read: \"EOF while reading\" · re-run `make admit-transaction-recovery-battery` and make it pass before trusting this lane"]}
   EXIT_CODE=0
   ```

