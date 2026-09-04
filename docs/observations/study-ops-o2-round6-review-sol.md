## NO-GO

1. Provenance is correct, and the round-six lineage is the requested merge followed by four RED→GREEN pairs. The checkout was detached and clean at `dafc7f37`; I read both prior verdicts and the named diffs. Files: `docs/observations/study-ops-o2-round4-review-opus.md:1`, `docs/observations/study-ops-o2-round5-review-sol.md:1`, commits `e6d4416f..dafc7f37`.

   Exact command:

   ```sh
   pwd && git rev-parse HEAD && git status --short --branch && git log --oneline --first-parent 972cf4c2..dafc7f37
   ```

   Verbatim output:

   ```text
   /home/forge/tmp/sol/o2r5-wt
   dafc7f376860a8ddfe41d8aa0b41f1172a8e85b0
   ## HEAD (no branch)
   dafc7f37 study-ops: O2r6 GREEN (§5) — the descent starts where the lines alone stop fitting
   6bed20b0 study-ops: O2r6 RED (§5) — the fit is rare, so nobody costed it
   468ca52e study-ops: O2r6 GREEN (§4) — who added a key is a fact about construction, not about spelling
   8b2a4aa5 study-ops: O2r6 RED (§4) — the publisher's keys are known by their names, so a caller can forge them
   0362a4f9 study-ops: O2r6 GREEN (§3) — the declaration is the floor, not the first thing dropped
   b410e31b study-ops: O2r6 RED (§3) — the tightest budgets are the ones that declare nothing
   42cff0ff study-ops: O2r6 GREEN (§2) — one walk decides carriage, and the header reads it
   28ae1897 study-ops: O2r6 RED (§2) — the declaration counts a different walk than the audit
   580e167a merge-fix: the trunk's controls follow their kernel into clj-surgeon.study
   e6d4416f merge: origin/MCP/main into bridge/study-ops-mcp (O2 round six, finding 10)
   ```

   Exact final cleanup/state command:

   ```sh
   test /var/tmp/forge/o2r6-review-fx = /var/tmp/forge/o2r6-review-fx && find /var/tmp/forge/o2r6-review-fx -depth -delete; if [ -e /var/tmp/forge/o2r6-review-fx ]; then echo FIXTURES_REMAIN; else echo FIXTURES_REMOVED; fi; for port in 8150 8151 8152; do if lsof -nP -iTCP:$port -sTCP:LISTEN >/dev/null 2>&1; then echo PORT_${port}_STILL_LISTENING; else echo PORT_${port}_STOPPED; fi; done; git rev-parse HEAD; git status --short; sed -n '1p;$p' /home/forge/tmp/sol/o2r6-sol-review.md
   ```

   Verbatim output (`git status --short` emitted no line):

   ```text
   FIXTURES_REMOVED
   PORT_8150_STOPPED
   PORT_8151_STOPPED
   PORT_8152_STOPPED
   dafc7f376860a8ddfe41d8aa0b41f1172a8e85b0
   ## NO-GO
   This tip is not GO on its own for MCP/main because public text still treats pointer/substring coincidence as carriage and can declare a different omitted-fact count than its structured face, and it does not yet compose onto the MEM-003 second landing without seven explicit conflict resolutions.
   ```

2. **BLOCKING — a public `name` rung declares 23 omitted facts while its own product audit finds only 19.** A leaf whose distinctive value equals its pointer is declared dropped, but the `dropped:` pointer itself makes `leaf-rendered?` call it carried. This violates MCP-OP-STUDY-047/048 and the review's bright line: the declaration and structured face disagree. The isolated one-leaf case says one dropped versus zero audited; the real fitted public result says 23 versus 19. Files: `src/clj_surgeon/mcp_inspect.clj:667`, `src/clj_surgeon/mcp_inspect.clj:843`, `src/clj_surgeon/mcp_inspect.clj:876`, `src/clj_surgeon/mcp_inspect.clj:904`, `src/clj_surgeon/mcp_inspect.clj:1999`, `src/clj_surgeon/mcp_inspect.clj:2026`.

   Exact command:

   ```sh
   ~/bin/suite-run clojure -M:clj-surgeon/mcp /var/tmp/forge/o2r6-review-fx/attacks.clj; code=$?; echo EXIT_CODE=$code
   ~/bin/suite-run clojure -M:clj-surgeon/mcp /var/tmp/forge/o2r6-review-fx/carriage-attacks.clj; code=$?; echo CARRIAGE_EXIT_CODE=$code
   ```

   Verbatim output:

   ```text
   == declared AND carried by the dropped pointer ==
   section= "  receipt facts · 0 of 1 rendered · the complete receipt is in structuredContent\n  dropped: abcdefghijklmnop"
   shown= 0 total= 1 declared_dropped= 1 audited_uncarried= 0 value_in_drop_pointer= true
   EXIT_CODE=0

   == public fall-through rung with pointer/value collision ==
   padding= 31750 text_omitted= name bytes= 32748 declared_dropped= 23 audited_uncarried= 19 text= "inspect_clojure\n\n  receipt facts · 0 of 23 rendered · the complete receipt is in structuredContent\n  dropped: source_character_count, elapsed_ms, read_complete, pointervalue0010, pointervalue0004, file_count, pointervalue0003, pointervalue0005 (+15 more)"
   CARRIAGE_EXIT_CODE=0
   ```

3. **BLOCKING — both declared-open coincidence cases remain real omissions, not acceptable declarations or GO-WITH-FIX.** A 16-character value carried only as a substring of a decoy yields `2 of 2 rendered` with no `target` line. Two distinct paths carrying the same long value yield `2 of 2 rendered` with only `alpha` named and one spelling occurrence. Neither fact is independently readable/removable. The nested-vector boolean and numeric string attacks do pass: the section contains `nested[0][0]=true` and `numeric_string=42`. Files: `src/clj_surgeon/mcp_inspect.clj:620-690`, `src/clj_surgeon/mcp_inspect.clj:713-780`, `src/clj_surgeon/mcp_inspect.clj:843-901`.

   Exact command:

   ```sh
   ~/bin/suite-run clojure -M:clj-surgeon/mcp /var/tmp/forge/o2r6-review-fx/carriage-attacks.clj; code=$?; echo CARRIAGE_EXIT_CODE=$code
   ```

   Verbatim output:

   ```text
   == 16-character substring leaf ==
   shown= 1 total= 2 declared_dropped= 0 target_label_present= false target_spelling_present= true audited_uncarried= 0
   section= "  receipt facts · 2 of 2 rendered\n  decoy: XXabcdefghijklmnopYY"

   == same distinctive spelling at two paths ==
   shown_lines= 1 total_facts= 2 declared_dropped= 0 alpha_label_present= true beta_label_present= false spelling_occurrences= 1 audited_uncarried= 0
   section= "  receipt facts · 2 of 2 rendered\n  alpha: the-same-distinctive-value-rendered-twice"

   == nested boolean and numeric string ==
   nested_boolean_label= true numeric_string_label= false audited_uncarried= 0
   section= "  receipt facts · 2 of 2 rendered\n  nested[0][0]=true\n  numeric_string=42"
   ```

   (`numeric_string_label=false` is only the probe checking the hyphenated spelling; the verbatim section shows the correct underscore label.) The intended duplicate-value witness is ineffective: its final assertion compares an expression with itself. Files: `test/clj_surgeon/mcp_study_test.clj:4336-4356`.

   Exact command:

   ```sh
   sed -n '4336,4358p' test/clj_surgeon/mcp_study_test.clj
   ```

   Verbatim output (terminal assertion):

   ```text
      (is (= (- (:total narrow) (count (:dropped-labels narrow)))
             (- (:total narrow) (count (:dropped-labels narrow))))))))
   ```

4. The ordinary near-cap fit is deterministic and fitting; the declaration floor behaves at allowance zero and at exactly its own length. The post-merge two-source fixture lands at 32,721 bytes/47 headroom; one extra fact lands at 32,758/10, with declared and audited both 1,147. This closes the ordinary one-fact-larger attack, but not findings 2–3. Files: `src/clj_surgeon/mcp_inspect.clj:904-1012`, `src/clj_surgeon/mcp_inspect_tool.clj:2111-2200`.

   Exact command:

   ```sh
   ~/bin/suite-run clojure -M:clj-surgeon/mcp /var/tmp/forge/o2r6-review-fx/batch-metrics.clj; code=$?; echo EXIT_CODE=$code
   ```

   Verbatim output:

   ```text
   files= ["src/clj_surgeon/mcp_inspect_tool.clj" "src/clj_surgeon/mcp_inspect.clj"]
   ordinary= {:bytes 32721, :headroom 47, :limit 6239, :omitted nil, :text_chars 6575, :declared 1147, :audited 1147, :deterministic true}
   plus_one= {:bytes 32758, :headroom 10, :limit 6265, :omitted nil, :text_chars 6593, :declared 1147, :audited 1147, :deterministic true}
   EXIT_CODE=0
   ```

   Exact floor/performance command:

   ```sh
   ~/bin/suite-run clojure -M:clj-surgeon/mcp /var/tmp/forge/o2r6-review-fx/attacks.clj; code=$?; echo EXIT_CODE=$code
   ```

   Verbatim output:

   ```text
   == allowance exactly declaration length ==
   declaration_chars= 103 exact_section_chars= 103 shown= 0 declared_dropped= 2 audited_uncarried= 2
   section= "  receipt facts · 0 of 2 rendered · the complete receipt is in structuredContent\n  dropped: alpha, beta"

   == 10k and 100k at the real cap ==
   facts=10000 elapsed_ms= 194.89 error_type= inspect-output-limit text_omitted= nil bytes= 1260 fits= true declared= 0 audited= 0
   facts=100000 elapsed_ms= 1311.18 error_type= inspect-output-limit text_omitted= nil bytes= 1265 fits= true declared= 0 audited= 0

   == 10k on an actual fitted (non-refusal) path ==
   facts=10000 fitted_path_elapsed_ms= 3695.46 ok= true error_type= nil text_evidence_limit= 66472 bytes= 199951 fits_test_cap= true declared= 4823 audited= 4823
   EXIT_CODE=0
   ```

   Ruling: the real-cap 100k case refuses typed within two seconds and the shipped 10k refusal path is fast. The lifted-cap probe exposes a cost finding: when 10k facts can take the fitting path rather than the builder's refusal path, it takes 3.70 seconds, above MCP-OP-STUDY-050's two-second claim. It is not an additional public-cap correctness blocker because 10k keyed facts cannot fit structuredContent under 32,768 bytes.

5. The §4 envelope repair survives the requested attacks at this tip. A domain result carrying every current envelope spelling publishes its real top-level clock, preserves the domain nested measurement, and the decoded wire result reads the published top-level clock. Simulating the MEM-003 nested envelope overwrites the forged domain clock with 1.25. An oversized all-key receipt becomes a 1,265-byte typed refusal. Metadata intentionally does not survive serialization (`wire_finalized=false`); no post-wire code calls the fit, so that is not an escape. Files: `src/clj_surgeon/mcp_operation.clj:54`, `src/clj_surgeon/mcp_operation.clj:73`, `src/clj_surgeon/mcp_operation.clj:83`, `src/clj_surgeon/mcp_operation.clj:112`, `src/clj_surgeon/mcp_inspect_tool.clj:2043-2108`.

   Exact command:

   ```sh
   ~/bin/suite-run clojure -M:clj-surgeon/mcp /var/tmp/forge/o2r6-review-fx/envelope-wire.clj; code=$?; echo EXIT_CODE=$code
   ```

   Verbatim output:

   ```text
   published_ok= true published_top_elapsed= 0.010146 published_nested_elapsed= 8.88888888E8 domain_measured_preserved= true envelope= {:elapsed_ms 0.010146}
   wire_finalized= false wire_request_elapsed_ms= 0.010146 wire_uses_top= true
   nested_simulation_request_elapsed_ms= 1.25 nested_domain_value= envelope
   EXIT_CODE=0
   ```

   The merged `run-ls-tree` typed return is also handled: its only production registry caller is `run`, and `-main` exits 1 for a returned map with `:error`; the memory battery caller supplies a generated valid root. Files: `src/clj_surgeon/core.clj:176`, `src/clj_surgeon/core.clj:533`, `src/clj_surgeon/core.clj:1043`, `src/clj_surgeon/memory_battery_runner.clj:47-57`.

   Exact command:

   ```sh
   rg -n "run-ls-tree" --glob '!docs/**' .
   bb -cp src -m clj-surgeon.core :op :ls-tree :dir /var/tmp/forge/o2r6-review-fx/no-such-dir :format :edn; code=$?; echo EXIT_CODE=$code
   ```

   Verbatim output (production hits and CLI result):

   ```text
   ./src/clj_surgeon/core.clj:176:(defn run-ls-tree
   ./src/clj_surgeon/core.clj:533:    :ls-tree          {:handler   run-ls-tree
   ./src/clj_surgeon/memory_battery_runner.clj:51:    :entrance "clj-surgeon.core/run-ls-tree {:dir root :format :edn}"
   ./src/clj_surgeon/memory_battery_runner.clj:55:           ((requiring-resolve 'clj-surgeon.core/run-ls-tree)
   {:error
    ":ls-tree :dir must be an existing directory: \"/var/tmp/forge/o2r6-review-fx/no-such-dir\"",
    :error-type :workspace-root-not-a-directory,
    :dir "/var/tmp/forge/o2r6-review-fx/no-such-dir",
    :next-action "pass_an_existing_directory_path"}
   EXIT_CODE=1
   ```

6. The disclosed +16–59% small-read byte cost is mechanically acceptable in absolute terms (about 0.16–1.05 KB) only if it buys the carriage invariant; findings 2–3 show it still does not. The golden is a product-visible behavior change—one fact line becomes ten label lines, and an absent `read_complete` prints `read_complete=null`. I found no explicit Gene acceptance. Standing alone this cost/golden issue is **GO-WITH-FIX requiring Gene acceptance**, but the carriage findings make the tip NO-GO. Files: `src/clj_surgeon/mcp_inspect.clj:620-780`, `test/clj_surgeon/mcp_inspect_contract_test.clj:507-546`.

   Exact command:

   ```sh
   git show origin/MCP/main:docs/observations/study-ops-o2-round5-review-sol.md | sed -n '160,210p'
   git diff --unified=6 532c76fb..0309f846 -- test/clj_surgeon/mcp_inspect_contract_test.clj | sed -n '1,180p'
   ```

   Verbatim measured output:

   ```text
   REV=515e8109
   outline  1783
   deps     1560
   topo     1421
   ls-tree  1004
   EXIT_CODE=0

   REV=972cf4c2
   outline  2837
   deps     2255
   topo     1884
   ls-tree  1168
   EXIT_CODE=0
   ```

   Verbatim golden hunk:

   ```text
-                "✓ terminal evidence · read_complete=true · next action none\n"
+                "✓ terminal evidence · read_complete=null · next action none\n"
                 "\n"
-                "  receipt facts · 1 of 1 rendered\n"
-                "  source_character_count: 1234\n"
+                "  receipt facts · 10 of 10 rendered\n"
+                "  ok=true\n"
+                "  operation=inspect_clojure\n"
+                "  request_count=2\n"
+                "  file_count=1\n"
   ```

7. The merge retained both sides of the only textual test conflict (`mcp_inspect_tool_test.clj`): the O2 client-session witness and trunk refusal witnesses are both present. The merge-fix correctly reroutes trunk witnesses to `clj-surgeon.study`; I found no trunk witness deleted or materially weakened by the conflict resolution. The separate round-six duplicate-value witness is weak for the reason in finding 3. Files: merge `e6d4416f`, `test/clj_surgeon/mcp_inspect_tool_test.clj:303-499`, merge-fix `580e167a`, `test/clj_surgeon/core_discovery_test.clj:17-47`, `test/clj_surgeon/mcp_study_test.clj:2185-2201`.

   Exact command:

   ```sh
   git show --remerge-diff --format= --name-only e6d4416f
   git show --remerge-diff --format= e6d4416f -- test/clj_surgeon/mcp_inspect_tool_test.clj
   git show --format= --stat 580e167a
   ```

   Verbatim name output:

   ```text
   docs/tech-tree.md
   src/clj_surgeon/core.clj
   src/clj_surgeon/mcp_inspect.clj
   src/clj_surgeon/mcp_inspect_tool.clj
   src/clj_surgeon/mcp_intent_contract.clj
   test/clj_surgeon/mcp_inspect_tool_test.clj
   ```

   RED→GREEN lineage is honest for each stated reason, including the older round-five pairs. Exact command template used in the disposable history clone:

   ```sh
   git checkout --quiet --detach REV; O2R6_CP="$(clojure -Spath -M:clj-surgeon/mcp-test)"; ~/bin/suite-run java -cp "$O2R6_CP" clojure.main /var/tmp/forge/o2r6-review-fx/run-vars.clj FULLY_QUALIFIED_TEST_VARS; code=$?; echo EXIT_CODE=$code
   ```

   Verbatim summaries:

   ```text
   REV=9bbe2bc1
   Ran 4 tests containing 20 assertions.
   11 failures, 0 errors.
   EXIT_CODE=1
   REV=aa8bfe5d
   Ran 4 tests containing 20 assertions.
   0 failures, 0 errors.
   EXIT_CODE=0
   REV=532c76fb
   Ran 2 tests containing 24 assertions.
   16 failures, 0 errors.
   EXIT_CODE=1
   REV=0309f846
   Ran 2 tests containing 24 assertions.
   0 failures, 0 errors.
   EXIT_CODE=0
   REV=fe0a4a2e
   Ran 1 tests containing 8 assertions.
   0 failures, 1 errors.
   EXIT_CODE=1
   REV=8210e5c4
   Ran 2 tests containing 13 assertions.
   0 failures, 0 errors.
   REV=28ae1897
   Ran 2 tests containing 8 assertions.
   3 failures, 0 errors.
   EXIT_CODE=1
   REV=42cff0ff
   Ran 2 tests containing 8 assertions.
   0 failures, 0 errors.
   EXIT_CODE=0
   REV=b410e31b
   Ran 3 tests containing 17 assertions.
   11 failures, 2 errors.
   EXIT_CODE=1
   REV=0362a4f9
   Ran 3 tests containing 19 assertions.
   0 failures, 0 errors.
   EXIT_CODE=0
   REV=8b2a4aa5
   Ran 1 tests containing 6 assertions.
   4 failures, 0 errors.
   EXIT_CODE=1
   REV=468ca52e
   Ran 1 tests containing 6 assertions.
   0 failures, 0 errors.
   EXIT_CODE=0
   REV=6bed20b0
   the fit took 18111.23 ms over 10,000 receipt facts
   Ran 1 tests containing 4 assertions.
   1 failures, 0 errors.
   EXIT_CODE=1
   REV=dafc7f37
   Ran 1 tests containing 4 assertions.
   0 failures, 0 errors.
   EXIT_CODE=0
   ```

8. The declared prepared-wire flake did not reproduce: ten namespace runs, 30 tests / 270 assertions, zero failures/errors. The wrapper rewrote only the test child's `:port 0` to explicit port 8150. Port 8150 was free before and stopped after. Files: `test/clj_surgeon/mcp_prepared_wire_test.clj:30-48`, `test/clj_surgeon/mcp_prepared_wire_test.clj:205-246`.

   Exact command:

   ```sh
   if lsof -nP -iTCP:8150 -sTCP:LISTEN >/dev/null 2>&1; then echo PORT_8150_BUSY; exit 1; else echo PORT_8150_FREE; fi; O2R6_CP="$(clojure -Spath -M:clj-surgeon/mcp-test)"; failures=0; for i in $(seq 1 10); do echo RUN=$i; ~/bin/suite-run java -cp "$O2R6_CP" clojure.main /var/tmp/forge/o2r6-review-fx/run-prepared-ns.clj 8150; code=$?; echo RUN_EXIT=$code; if [ "$code" -ne 0 ]; then failures=$((failures+1)); fi; done; echo FAILURE_COUNT=$failures; if lsof -nP -iTCP:8150 -sTCP:LISTEN >/dev/null 2>&1; then echo PORT_8150_STILL_LISTENING; else echo PORT_8150_STOPPED; fi
   ```

   Verbatim repeated result and final count:

   ```text
   PORT_8150_FREE
   RUN=1
   Ran 3 tests containing 27 assertions.
   0 failures, 0 errors.
   RUN_EXIT=0
   RUN=2
   Ran 3 tests containing 27 assertions.
   0 failures, 0 errors.
   RUN_EXIT=0
   RUN=3
   Ran 3 tests containing 27 assertions.
   0 failures, 0 errors.
   RUN_EXIT=0
   RUN=4
   Ran 3 tests containing 27 assertions.
   0 failures, 0 errors.
   RUN_EXIT=0
   RUN=5
   Ran 3 tests containing 27 assertions.
   0 failures, 0 errors.
   RUN_EXIT=0
   RUN=6
   Ran 3 tests containing 27 assertions.
   0 failures, 0 errors.
   RUN_EXIT=0
   RUN=7
   Ran 3 tests containing 27 assertions.
   0 failures, 0 errors.
   RUN_EXIT=0
   RUN=8
   Ran 3 tests containing 27 assertions.
   0 failures, 0 errors.
   RUN_EXIT=0
   RUN=9
   Ran 3 tests containing 27 assertions.
   0 failures, 0 errors.
   RUN_EXIT=0
   RUN=10
   Ran 3 tests containing 27 assertions.
   0 failures, 0 errors.
   RUN_EXIT=0
   FAILURE_COUNT=0
   PORT_8150_STOPPED
   ```

9. All executable tip gates are green. The old `4480e3d..HEAD` fixture comparison is now nonzero because the merged trunk brought exactly 58 fixture paths; the addendum's relevant check is green: zero fixture changes after `580e167a`, and the `972cf4c2..HEAD` names equal the merge's 58 names exactly. Files: `Makefile:1`, `test/run_all.clj:1`, `test/clj_surgeon/mcp_test_runner.clj:1`, `test-fixtures/`.

   Exact commands and verbatim output:

   ```sh
   ~/bin/suite-run bb test/run_all.clj; code=$?; echo EXIT_CODE=$code
   ```

   ```text
   Ran 843 tests containing 6836 assertions.
   0 failures, 0 errors.
   EXIT_CODE=0
   ```

   ```sh
   ~/bin/suite-run clojure -M:clj-surgeon/mcp-test; code=$?; echo EXIT_CODE=$code
   ```

   ```text
   Ran 834 tests containing 10907 assertions.
   0 failures, 0 errors.
   EXIT_CODE=0
   ```

   ```sh
   make mcp-operation-oracle; code=$?; echo ORACLE_EXIT_CODE=$code
   ~/bin/suite-run clojure -M -e "(require '[clj-surgeon.mcp-intent-contract :as audit]) (let [r (audit/audit-current-repository)] (prn (select-keys r [:ok :spec-count :violations])))"; code=$?; echo AUDIT_EXIT_CODE=$code
   ```

   ```text
   # @spec MCP-OP-ORACLE-001
   swipl -q -f test/mcp_operation_contract_oracle.pl
   mcp-operation oracle: pass; legacy counterexamples=[verification_failed,verification_pending]
   ORACLE_EXIT_CODE=0
   Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge
   {:ok true, :violations []}
   AUDIT_EXIT_CODE=0
   ```

   ```sh
   git diff --exit-code 4480e3d..HEAD -- test-fixtures/ >/dev/null; code=$?; echo FIXTURES_4480_EXIT_CODE=$code
   git diff --exit-code 580e167a..HEAD -- test-fixtures/ >/dev/null; code=$?; echo FIXTURES_POST_MERGE_EXIT_CODE=$code
   git diff --exit-code 972cf4c2..HEAD -- test-fixtures/ >/dev/null; code=$?; echo FIXTURES_SINCE_R5_EXIT_CODE=$code
   echo FIXTURES_SINCE_R5_COUNT=$(git diff --name-only 972cf4c2..HEAD -- test-fixtures/ | wc -l)
   echo FIXTURES_MERGE_COUNT=$(git diff --name-only 972cf4c2..580e167a -- test-fixtures/ | wc -l)
   diff -u <(git diff --name-only 972cf4c2..HEAD -- test-fixtures/) <(git diff --name-only 972cf4c2..580e167a -- test-fixtures/); code=$?; echo FIXTURE_NAMESETS_DIFF_EXIT_CODE=$code
   ```

   ```text
   FIXTURES_4480_EXIT_CODE=1
   FIXTURES_POST_MERGE_EXIT_CODE=0
   FIXTURES_SINCE_R5_EXIT_CODE=1
   FIXTURES_SINCE_R5_COUNT=58
   FIXTURES_MERGE_COUNT=58
   FIXTURE_NAMESETS_DIFF_EXIT_CODE=0
   ```

10. Mergeability: the tip merges cleanly into the current `origin/MCP/main` (`627d8eeb`). It does **not** compose cleanly onto MEM-003. The remote branch has advanced to `69e58b41`, but both that ref and the brief's exact `694f538d` produce the same seven conflict files—not six: `src/clj_surgeon/core.clj`, `src/clj_surgeon/mcp_inspect.clj`, `src/clj_surgeon/mcp_inspect_tool.clj`, `src/clj_surgeon/mcp_operation.clj`, `test/clj_surgeon/core_discovery_test.clj`, `test/clj_surgeon/mcp_inspect_tool_test.clj`, and `test/run_all.clj`.

    Resolution rule: start from this tip for `core.clj`, `core_discovery_test.clj`, and the O2 carriage/budget code; layer MEM-003's measured-domain fields into those results. Start from MEM-003 for `mcp_operation.clj`'s nested `:measured` wire shape, then add this tip's construction-stamped envelope identity and substitute measurement guards. Union the independent witnesses in `mcp_inspect_tool_test.clj` and union the namespace registry in `test/run_all.clj`; neither test side should delete the other.

    Exact commands:

    ```sh
    echo MCP_MAIN=$(git rev-parse origin/MCP/main); git merge-tree --write-tree HEAD origin/MCP/main; code=$?; echo MCP_MAIN_EXIT_CODE=$code
    echo MEM003=$(git rev-parse origin/bridge/integration-2026-09-03-mem003); git merge-tree --write-tree HEAD origin/bridge/integration-2026-09-03-mem003; code=$?; echo MEM003_EXIT_CODE=$code
    git merge-tree --write-tree HEAD 694f538d; code=$?; echo MEM003_694f538d_EXIT_CODE=$code
    ```

    Verbatim output (tree IDs plus conflict messages):

    ```text
    MCP_MAIN=627d8eebe6adc689e97b1cf291ed5eb226ca0389
    eab66d15ebc5c22512a468384dc2a384b771d850
    MCP_MAIN_EXIT_CODE=0
    MEM003=69e58b418740305ec4755eed4fbb50109b2acd67
    bb1368748537dd2f9cba3bb5ab7e9d482b948369
    CONFLICT (content): Merge conflict in src/clj_surgeon/core.clj
    CONFLICT (content): Merge conflict in src/clj_surgeon/mcp_inspect.clj
    CONFLICT (content): Merge conflict in src/clj_surgeon/mcp_inspect_tool.clj
    CONFLICT (content): Merge conflict in src/clj_surgeon/mcp_operation.clj
    CONFLICT (content): Merge conflict in test/clj_surgeon/core_discovery_test.clj
    CONFLICT (content): Merge conflict in test/clj_surgeon/mcp_inspect_tool_test.clj
    CONFLICT (content): Merge conflict in test/run_all.clj
    MEM003_EXIT_CODE=1
    5a7c66562bbffdf93b4408356a39b1237e89cd59
    CONFLICT (content): Merge conflict in src/clj_surgeon/core.clj
    CONFLICT (content): Merge conflict in src/clj_surgeon/mcp_inspect.clj
    CONFLICT (content): Merge conflict in src/clj_surgeon/mcp_inspect_tool.clj
    CONFLICT (content): Merge conflict in src/clj_surgeon/mcp_operation.clj
    CONFLICT (content): Merge conflict in test/clj_surgeon/core_discovery_test.clj
    CONFLICT (content): Merge conflict in test/clj_surgeon/mcp_inspect_tool_test.clj
    CONFLICT (content): Merge conflict in test/run_all.clj
    MEM003_694f538d_EXIT_CODE=1
    ```

## NO-GO
This tip is not GO on its own for MCP/main because public text still treats pointer/substring coincidence as carriage and can declare a different omitted-fact count than its structured face, and it does not yet compose onto the MEM-003 second landing without seven explicit conflict resolutions.
