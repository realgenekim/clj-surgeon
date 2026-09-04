## NO-GO

1. Provenance and scope are correct. I reviewed the round-four verdict first, then the seven commits and their diffs. The review worktree was detached and clean at the requested commit; no source, test, fixture, index, stash, commit, branch, or remote was changed. Review fixtures were confined to `/var/tmp/forge/o2r5-review-fx` and removed at the end.

   Files: `docs/observations/study-ops-o2-round4-review-opus.md:1`, commit `972cf4c2`.

   Exact command:

   ```sh
   git rev-parse HEAD; git status --short; git branch --show-current; git show -s --format='%H %s' HEAD
   ```

   Verbatim output (`git status --short` and `git branch --show-current` emitted no line):

   ```text
   972cf4c2e44526654a40f4f12ddc35612d17f6cf
   972cf4c2e44526654a40f4f12ddc35612d17f6cf study-ops: O2r5 — the round-five observation, with the before/after measured on one fixed fixture
   ```

   Exact final cleanup/state command:

   ```sh
   test /var/tmp/forge/o2r5-review-fx = /var/tmp/forge/o2r5-review-fx; find /var/tmp/forge/o2r5-review-fx -depth -delete; if [ -e /var/tmp/forge/o2r5-review-fx ]; then echo FIXTURES_REMAIN; else echo FIXTURES_REMOVED; fi; if lsof -nP -iTCP:8150 -sTCP:LISTEN >/dev/null 2>&1; then echo PORT_8150_STILL_LISTENING; else echo PORT_8150_STOPPED; fi; git rev-parse HEAD; git status --short; sed -n '1p;$p' /home/forge/tmp/sol/o2r5-sol-review.md
   ```

   Verbatim output (`git status --short` emitted no line):

   ```text
   FIXTURES_REMOVED
   PORT_8150_STOPPED
   972cf4c2e44526654a40f4f12ddc35612d17f6cf
   ## NO-GO
   This tip is not GO on its own for MCP/main because public results still omit undeclared structured facts and an envelope collision makes the fit return 81,861 bytes against a 32,768-byte cap, and it does not currently compose cleanly onto either `origin/MCP/main` or the MEM-003 second landing at `694f538d`.
   ```

   Exact command:

   ```sh
   git log --oneline 515e8109..972cf4c2
   ```

   Verbatim output:

   ```text
   972cf4c2 study-ops: O2r5 — the round-five observation, with the before/after measured on one fixed fixture
   8210e5c4 study-ops: O2r5 GREEN (§7, §5) — the gate reads the envelope from the namespace that owns it, and the crash is written down
   fe0a4a2e study-ops: O2r5 RED (§7) — the budget gate knows one shape of an envelope it does not own
   0309f846 study-ops: O2r5 GREEN (§4) — a collidable spelling is carried by its label, and the status line spells the receipt
   532c76fb study-ops: O2r5 RED (§4) — a short spelling in the text is not evidence the text carries THAT leaf
   aa8bfe5d study-ops: O2r5 GREEN (§2, §3) — the section charges every byte it prints, and the fit measures what it accepts
   9bbe2bc1 study-ops: O2r5 RED (§2, §3) — every rendered byte is charged, or the fit is searching a lie
   ```

2. BLOCKING — the fixed 140+30-form public result still contains an uncarried, undeclared fact. The text says `98 of 882 rendered`, hence 784 dropped, while the product audit finds 785 uncarried leaves. The extra leaf is `results[1].outline.requires[0]`; it never entered the fact entries and therefore has neither a pointer nor coverage in the `(+776 more)` count. This is the declared “fact spelled twice” residual occurring in the branch's own primary fixture, not a hypothetical edge. `receipt-fact-entries` makes its decision against a text accumulated from earlier fact lines; a later leaf can be deemed carried by an earlier line that the budget subsequently drops.

   Files: `src/clj_surgeon/mcp_inspect.clj:629-646`, `src/clj_surgeon/mcp_inspect.clj:673-713`, `src/clj_surgeon/mcp_inspect.clj:782-837`, `docs/intent/study-ops/study-ops-specs.md:104`.

   Exact command:

   ```sh
   ~/bin/suite-run clojure -M:clj-surgeon/mcp /var/tmp/forge/o2r5-review-fx/fixed-audit.clj; code=$?; echo EXIT_CODE=$code
   ```

   Verbatim output:

   ```text
   Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge
   header= "  receipt facts · 98 of 882 rendered · the complete receipt is in structuredContent"
   drop_line= "  dropped: results[0].outline.forms[15].line, results[0].outline.forms[15].end_line, results[0].outline.forms[15].name, results[0].outline.forms[16].type, results[0].outline.forms[16].platforms[0], results[0].outline.forms[16].line, results[0].outline.forms[16].end_line, results[0].outline.forms[16].name (+776 more)"
   uncarried_count= 785 never_in_fact_entries_count= 1
   never_in_fact_entries= [[[:results 1 :outline :requires 0] "[clojure.string :as str]"]]
   EXIT_CODE=0
   ```

   The requested near-cap attack itself is deterministic and does find fitting candidates: the fixture at this review path has four bytes rather than thirteen bytes of headroom because its absolute `workspace_root` is longer; adding exactly one top-level fact still selects a fitting ordinary candidate on all three evaluations. That success does not repair the undeclared 785th leaf.

   Exact command:

   ```sh
   ~/bin/suite-run clojure -M:clj-surgeon/mcp /var/tmp/forge/o2r5-review-fx/adversarial.clj; code=$?; echo EXIT_CODE=$code
   ```

   Verbatim output:

   ```text
   Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge
   == fixed 140+30 fixture and exactly one added fact ==
   ordinary bytes=32764 headroom=4 limit=12266 omitted=nil text_chars=12474 uncarried=785 deterministic=true
     fact-header= "  receipt facts · 98 of 882 rendered · the complete receipt is in structuredContent"
     drop-line= "  dropped: results[0].outline.forms[15].line, results[0].outline.forms[15].end_line, results[0].outline.forms[15].name, results[0].outline.forms[16].type, results[0].outline.forms[16].platforms[0], results[0].outline.forms[16].line, results[0].outline.forms[16].end_line, results[0].outline.forms[16].name (+776 more)"
     three_equal= true
   plus_one bytes=32752 headroom=16 limit=12216 omitted=nil text_chars=12443 uncarried=785 deterministic=true
     fact-header= "  receipt facts · 99 of 883 rendered · the complete receipt is in structuredContent"
     drop-line= "  dropped: results[0].outline.forms[15].line, results[0].outline.forms[15].end_line, results[0].outline.forms[15].name, results[0].outline.forms[16].type, results[0].outline.forms[16].platforms[0], results[0].outline.forms[16].line, results[0].outline.forms[16].end_line, results[0].outline.forms[16].name (+776 more)"
     three_equal= true

   == fact-block: drop line alone exceeds allowance; zero allowance ==
   allowance=0 dropped_line_chars=139 section_chars=nil shown=0 total=2 dropped=true pointers=2
   allowance=8 dropped_line_chars=139 section_chars=nil shown=0 total=2 dropped=true pointers=2
   allowance=32 dropped_line_chars=139 section_chars=nil shown=0 total=2 dropped=true pointers=2
   allowance=512 dropped_line_chars=139 section_chars=169 shown=2 total=2 dropped=false pointers=0

   == pointer-syntax collision ==
   text_equal_after_target_removal= true
   target_label_present= true
   target_own_line_present= false
   product_uncarried= []

   == declared-open 16-char substring and duplicated fact ==
   spelling_chars= 16 collidable= false substring_fact_removal_invisible= true
   one_spelling_carries_both_paths= true spelling_occurrences= 1

   == nested boolean and numeric string ==
   nested_boolean_label= true
   numeric_string_label= true

   == reachable notice rung and text-to-structure carriage ==
   notice_fixture_padding= 32319
   rung= "notice" bytes= 32764 fits= true uncarried= 11 pointer_declared= 0
   text= "inspect_clojure\n! text omitted · the complete receipt left no room to render it\n→ the complete result is in structuredContent\n→ read_structured_content"

   == user-level envelope-key collision ==
   input_has_top_clock= true input_has_user_measured= true
   result_ok= false error_type= inspect-output-limit preserved_user_measured= true
   published_bytes= 81861 budget= 32768 fits= false
   EXIT_CODE=0
   ```

3. BLOCKING — the zero-allowance and notice rungs do not satisfy text ⊇ structure. `fact-block` stops at zero before establishing that its header/drop line fits and returns `section=nil`; the reachable notice rung then publishes a generic pointer to `structuredContent`, with eleven uncarried facts and zero fact pointers. This directly meets the review's blocking rule. The pointer-syntax attack also defeats the product predicate: a decoy value `probe.target=42` makes the numeric `probe.target` look carried, even though there is no target fact line and removal is byte-invisible.

   Files: `src/clj_surgeon/mcp_inspect.clj:622-646`, `src/clj_surgeon/mcp_inspect.clj:742-770`, `src/clj_surgeon/mcp_inspect.clj:817-837`, `src/clj_surgeon/mcp_inspect.clj:1778-1793`, `src/clj_surgeon/mcp_inspect_tool.clj:1830-1843`, `src/clj_surgeon/mcp_inspect_tool.clj:2071-2077`.

   Command and verbatim output: the `adversarial.clj` command and output in finding 2, specifically the `fact-block`, `pointer-syntax collision`, and `reachable notice rung` sections.

   Ruling on the two declared-open §4 items: they are not acceptable-declared and not merely GO-WITH-FIX under the supplied rule. A 16-character fact that is only a substring of a decoy is byte-invisible when removed, and one occurrence is accepted as carrying two paths. The second case is already the undeclared `results[1].outline.requires[0]` in finding 2. Both are omitted structured facts, hence blocking. The nested vector boolean and numeric string `"42"` attacks pass and are correctly label-carried.

4. BLOCKING — `fit-public-result` can return a non-fitting candidate when a domain/user key collides with the declared envelope. `envelope-keys` treats any top-level `:measured` as publisher metadata. `public-budget-refusal` then merges that user value into the substitute without measuring the substitute. A 40,000-character user `measured.user_blob` produces an `inspect-output-limit` result of 81,861 bytes against the 32,768-byte cap. This is exactly the review's “fit can return a non-fitting candidate” blocker.

   Files: `src/clj_surgeon/mcp_operation.clj:26-51`, `src/clj_surgeon/mcp_operation.clj:68-76`, `src/clj_surgeon/mcp_inspect_tool.clj:1527-1544`, `src/clj_surgeon/mcp_inspect_tool.clj:1951-1975`, `src/clj_surgeon/mcp_inspect_tool.clj:2073-2077`.

   Command and verbatim output: the `adversarial.clj` command in finding 2, section `user-level envelope-key collision`:

   ```text
   input_has_top_clock= true input_has_user_measured= true
   result_ok= false error_type= inspect-output-limit preserved_user_measured= true
   published_bytes= 81861 budget= 32768 fits= false
   ```

   The ordinary nested-MEM envelope fix and the §5 crash-as-design witness are otherwise honest: the red envelope witness errors at `fe0a4a2e`, and both selected witnesses pass at `8210e5c4`. The collision class is absent from those witnesses.

5. The §2/§3 implementation closes the round-four bisection regression for the ordinary boundary, but its cost claim is not robust. A result with 10,000 tiny numeric facts took 69,132.18 ms in the fit, versus the observation's ~560 ms “worst call” statement. Prefix sums make one `fact-block` descent cheap, but each of roughly 66 candidates rebuilds `receipt-fact-entries`, whose reducer repeatedly appends the cumulative text. The result fit, so this is not an additional correctness blocker, but it is a 69-second read-path latency finding that must be carried into the fix.

   Files: `src/clj_surgeon/mcp_inspect.clj:692-713`, `src/clj_surgeon/mcp_inspect.clj:802-820`, `src/clj_surgeon/mcp_inspect_tool.clj:1977-1986`, `src/clj_surgeon/mcp_inspect_tool.clj:2048-2069`.

   Exact command:

   ```sh
   timeout 120s ~/bin/suite-run clojure -M:clj-surgeon/mcp /var/tmp/forge/o2r5-review-fx/ten-thousand.clj; code=$?; echo EXIT_CODE=$code
   ```

   Verbatim output:

   ```text
   Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge
   facts=10000 elapsed_ms=69132.18 published_bytes=32752 budget=32768 fits=true omitted=nil error_type=nil
   EXIT_CODE=0
   ```

6. §4's byte cost is correctly declared and mechanically acceptable for read-side verbs in absolute size: on the review-local equivalent fixture, round four → tip is outline 1,783→2,837 (+59%), deps 1,560→2,255 (+45%), topo 1,421→1,884 (+33%), and ls-tree 1,004→1,168 (+16%). That is roughly 0.16–1.05 KB per call, a reasonable price for removing a return only if the carriage invariant is actually true; findings 2–3 show it is not yet buying that invariant.

   The golden change at `test/clj_surgeon/mcp_inspect_contract_test.clj:502-544` is an observable behavior change (one fact line becomes ten label lines and a receipt without `read_complete` prints `read_complete=null`). It is documented and witnessed, but I found no explicit Gene acceptance of that product-facing text contract in the reviewed lineage. It therefore needs Gene's acceptance before landing; standing alone, that would be GO-WITH-FIX rather than NO-GO.

   Files: `src/clj_surgeon/mcp_inspect.clj:590-620`, `src/clj_surgeon/mcp_inspect.clj:673-713`, `test/clj_surgeon/mcp_inspect_contract_test.clj:502-544`.

   Exact commands:

   ```sh
   git checkout --quiet --detach 515e8109; echo REV=$(git rev-parse --short HEAD); ~/bin/suite-run clojure -M:clj-surgeon/mcp /var/tmp/forge/o2r5-review-fx/cost.clj; code=$?; echo EXIT_CODE=$code
   echo REV=$(git rev-parse --short HEAD); ~/bin/suite-run clojure -M:clj-surgeon/mcp /var/tmp/forge/o2r5-review-fx/cost.clj; code=$?; echo EXIT_CODE=$code
   ```

   Verbatim outputs (first command in the history clone; second at the review tip):

   ```text
   REV=515e8109
   Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge
   outline  1783
   deps     1560
   topo     1421
   ls-tree  1004
   EXIT_CODE=0
   ```

   ```text
   REV=972cf4c2
   Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge
   outline  2837
   deps     2255
   topo     1884
   ls-tree  1168
   EXIT_CODE=0
   ```

7. The declared `stop-child!` flake did not reproduce: 0 failures in 10 complete namespace runs (3 tests / 27 assertions each). The runner rewrote the test's `:port 0` child command to explicit port 8150; it verified the port free before and stopped after. No review server contacted any prohibited port.

   Files: `test/clj_surgeon/mcp_prepared_wire_test.clj:30-48`, `test/clj_surgeon/mcp_prepared_wire_test.clj:205-246`.

   Exact command:

   ```sh
   if lsof -nP -iTCP:8150 -sTCP:LISTEN >/dev/null 2>&1; then echo PORT_8150_BUSY; exit 1; else echo PORT_8150_FREE; fi; failures=0; for i in $(seq 1 10); do echo RUN=$i; ~/bin/suite-run /var/tmp/forge/o2r5-review-fx/run-prepared-ns.sh 8150; code=$?; echo RUN_EXIT=$code; if [ "$code" -ne 0 ]; then failures=$((failures+1)); fi; done; echo FAILURE_COUNT=$failures; if lsof -nP -iTCP:8150 -sTCP:LISTEN >/dev/null 2>&1; then echo PORT_8150_STILL_LISTENING; else echo PORT_8150_STOPPED; fi; exit 0
   ```

   Verbatim repeated result for runs 1–10 and final count:

   ```text
   PORT_8150_FREE
   RUN=1
   Testing clj-surgeon.mcp-prepared-wire-test
   Ran 3 tests containing 27 assertions.
   0 failures, 0 errors.
   RUN_EXIT=0
   RUN=2
   Testing clj-surgeon.mcp-prepared-wire-test
   Ran 3 tests containing 27 assertions.
   0 failures, 0 errors.
   RUN_EXIT=0
   RUN=3
   Testing clj-surgeon.mcp-prepared-wire-test
   Ran 3 tests containing 27 assertions.
   0 failures, 0 errors.
   RUN_EXIT=0
   RUN=4
   Testing clj-surgeon.mcp-prepared-wire-test
   Ran 3 tests containing 27 assertions.
   0 failures, 0 errors.
   RUN_EXIT=0
   RUN=5
   Testing clj-surgeon.mcp-prepared-wire-test
   Ran 3 tests containing 27 assertions.
   0 failures, 0 errors.
   RUN_EXIT=0
   RUN=6
   Testing clj-surgeon.mcp-prepared-wire-test
   Ran 3 tests containing 27 assertions.
   0 failures, 0 errors.
   RUN_EXIT=0
   RUN=7
   Testing clj-surgeon.mcp-prepared-wire-test
   Ran 3 tests containing 27 assertions.
   0 failures, 0 errors.
   RUN_EXIT=0
   RUN=8
   Testing clj-surgeon.mcp-prepared-wire-test
   Ran 3 tests containing 27 assertions.
   0 failures, 0 errors.
   RUN_EXIT=0
   RUN=9
   Testing clj-surgeon.mcp-prepared-wire-test
   Ran 3 tests containing 27 assertions.
   0 failures, 0 errors.
   RUN_EXIT=0
   RUN=10
   Testing clj-surgeon.mcp-prepared-wire-test
   Ran 3 tests containing 27 assertions.
   0 failures, 0 errors.
   RUN_EXIT=0
   FAILURE_COUNT=0
   PORT_8150_STOPPED
   ```

   (`JAVA_TOOL_OPTIONS` appeared before each run and twice on the first run; those environment notices do not change the count.)

8. RED→GREEN lineage is honest for all stated items. Item 1 has 11 failures at `9bbe2bc1` for the notice/budget/monotonicity defects and 0 at `aa8bfe5d`; item 2 has 16 failures at `532c76fb` for removal/substitution coincidence and 0 at `0309f846`; item 3 errors on the nested envelope at `fe0a4a2e`, while the envelope and §5 crash witnesses both pass at `8210e5c4`.

   Files: `test/clj_surgeon/mcp_study_test.clj:3783-3988`, `test/clj_surgeon/mcp_study_test.clj:3989-4155`, `test/clj_surgeon/mcp_study_test.clj:4156-4254`.

   Exact commands and verbatim summaries:

   ```sh
   git checkout --quiet --detach 9bbe2bc1; ~/bin/suite-run /var/tmp/forge/o2r5-review-fx/run-vars.sh clj-surgeon.mcp-study-test/an-ordinary-two-file-outline-batch-spends-the-budget-on-its-receipt clj-surgeon.mcp-study-test/the-form-count-sweep-never-abandons-a-rendering-that-fits clj-surgeon.mcp-study-test/lowering-the-evidence-allowance-can-only-shrink-the-rendering clj-surgeon.mcp-study-test/the-fit-finds-a-fitting-rendering-whenever-one-exists; code=$?; echo EXIT_CODE=$code; exit 0
   ```

   ```text
   Ran 4 tests containing 20 assertions.
   11 failures, 0 errors.
   EXIT_CODE=1
   ```

   ```sh
   git checkout --quiet --detach aa8bfe5d; ~/bin/suite-run /var/tmp/forge/o2r5-review-fx/run-vars.sh clj-surgeon.mcp-study-test/an-ordinary-two-file-outline-batch-spends-the-budget-on-its-receipt clj-surgeon.mcp-study-test/the-form-count-sweep-never-abandons-a-rendering-that-fits clj-surgeon.mcp-study-test/lowering-the-evidence-allowance-can-only-shrink-the-rendering clj-surgeon.mcp-study-test/the-fit-finds-a-fitting-rendering-whenever-one-exists; code=$?; echo EXIT_CODE=$code; exit 0
   ```

   ```text
   Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge
   Ran 4 tests containing 20 assertions.
   0 failures, 0 errors.
   EXIT_CODE=0
   ```

   ```sh
   git checkout --quiet --detach 532c76fb; ~/bin/suite-run /var/tmp/forge/o2r5-review-fx/run-vars.sh clj-surgeon.mcp-study-test/the-text-depends-on-every-collidable-receipt-leaf clj-surgeon.mcp-study-test/a-short-spelling-is-carried-by-its-label-not-by-coincidence; code=$?; echo EXIT_CODE=$code; exit 0
   ```

   ```text
   Ran 2 tests containing 24 assertions.
   16 failures, 0 errors.
   EXIT_CODE=1
   ```

   ```sh
   git checkout --quiet --detach 0309f846; ~/bin/suite-run /var/tmp/forge/o2r5-review-fx/run-vars.sh clj-surgeon.mcp-study-test/the-text-depends-on-every-collidable-receipt-leaf clj-surgeon.mcp-study-test/a-short-spelling-is-carried-by-its-label-not-by-coincidence; code=$?; echo EXIT_CODE=$code; exit 0
   ```

   ```text
   Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge
   Ran 2 tests containing 24 assertions.
   0 failures, 0 errors.
   EXIT_CODE=0
   ```

   ```sh
   git checkout --quiet --detach fe0a4a2e; ~/bin/suite-run /var/tmp/forge/o2r5-review-fx/run-vars.sh clj-surgeon.mcp-study-test/the-envelope-is-whatever-the-finalizer-added; code=$?; echo EXIT_CODE=$code; exit 0
   ```

   ```text
   ERROR in (the-envelope-is-whatever-the-finalizer-added) (mcp_inspect_tool.clj:2006)
   Uncaught exception, not in assertion.
   expected: nil
     actual: java.lang.IllegalArgumentException: fit-public-result measures the published envelope and needs the finalized result: :elapsed_ms is absent
   Ran 1 tests containing 8 assertions.
   0 failures, 1 errors.
   EXIT_CODE=1
   ```

   ```sh
   git checkout --quiet --detach 8210e5c4; ~/bin/suite-run /var/tmp/forge/o2r5-review-fx/run-vars.sh clj-surgeon.mcp-study-test/the-envelope-is-whatever-the-finalizer-added clj-surgeon.mcp-study-test/an-unenumerated-refusal-reason-reaches-the-caller-as-a-crash; code=$?; echo EXIT_CODE=$code; exit 0
   ```

   ```text
   Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge
   Ran 2 tests containing 13 assertions.
   0 failures, 0 errors.
   EXIT_CODE=0
   ```

9. All requested tip gates are green, but none covers findings 2–4.

   Exact command:

   ```sh
   ~/bin/suite-run bb test/run_all.clj
   ```

   Verbatim terminal output and process exit:

   ```text
   Ran 731 tests containing 6023 assertions.
   0 failures, 0 errors.
   ```

   Exit: `0`.

   Exact command:

   ```sh
   ~/bin/suite-run clojure -M:clj-surgeon/mcp-test; code=$?; echo EXIT_CODE=$code
   ```

   Verbatim terminal output:

   ```text
   Ran 493 tests containing 6382 assertions.
   0 failures, 0 errors.
   EXIT_CODE=0
   ```

   Exact command:

   ```sh
   make mcp-operation-oracle; code=$?; echo EXIT_CODE=$code
   ```

   Verbatim output:

   ```text
   # @spec MCP-OP-ORACLE-001
   swipl -q -f test/mcp_operation_contract_oracle.pl
   mcp-operation oracle: pass; legacy counterexamples=[verification_failed,verification_pending]
   EXIT_CODE=0
   ```

   Exact command:

   ```sh
   ~/bin/suite-run clojure -M -e "(require '[clj-surgeon.mcp-intent-contract :as audit]) (let [r (audit/audit-current-repository)] (prn (select-keys r [:ok :spec-count :violations])))"; code=$?; echo EXIT_CODE=$code
   ```

   Verbatim output:

   ```text
   Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge
   Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge
   {:ok true, :violations []}
   EXIT_CODE=0
   ```

   Exact command:

   ```sh
   git diff --exit-code 4480e3d..HEAD -- test-fixtures/; code=$?; echo EXIT_CODE=$code
   ```

   Verbatim output:

   ```text
   EXIT_CODE=0
   ```

10. The tip does not compose cleanly onto either current target. `origin/MCP/main` is `b657edb1`; the first dry run conflicts in six files. The MEM-003 second landing is the requested `694f538d`; that dry run conflicts in eight files, adding `src/clj_surgeon/mcp_operation.clj` and `test/run_all.clj` to the first conflict set.

    Files: `docs/tech-tree.md`, `src/clj_surgeon/core.clj`, `src/clj_surgeon/mcp_inspect.clj`, `src/clj_surgeon/mcp_inspect_tool.clj`, `src/clj_surgeon/mcp_intent_contract.clj`, `src/clj_surgeon/mcp_operation.clj`, `test/clj_surgeon/mcp_inspect_tool_test.clj`, `test/run_all.clj`.

    Exact command:

    ```sh
    git merge-tree --write-tree HEAD origin/MCP/main
    ```

    Verbatim conflict output and exit:

    ```text
    22c6b7bde1cddeae713ff36c40beadc2d011ee64
    Auto-merging CLAUDE.md
    Auto-merging Makefile
    Auto-merging deps.edn
    Auto-merging docs/high-level-design.md
    Auto-merging docs/tech-tree.md
    CONFLICT (content): Merge conflict in docs/tech-tree.md
    Auto-merging src/clj_surgeon/core.clj
    CONFLICT (content): Merge conflict in src/clj_surgeon/core.clj
    Auto-merging src/clj_surgeon/mcp_inspect.clj
    CONFLICT (content): Merge conflict in src/clj_surgeon/mcp_inspect.clj
    Auto-merging src/clj_surgeon/mcp_inspect_tool.clj
    CONFLICT (content): Merge conflict in src/clj_surgeon/mcp_inspect_tool.clj
    Auto-merging src/clj_surgeon/mcp_intent_contract.clj
    CONFLICT (content): Merge conflict in src/clj_surgeon/mcp_intent_contract.clj
    Auto-merging src/clj_surgeon/mcp_tool.clj
    Auto-merging test/clj_surgeon/ls_tree_test.clj
    Auto-merging test/clj_surgeon/mcp_http_server_test.clj
    Auto-merging test/clj_surgeon/mcp_inspect_contract_test.clj
    Auto-merging test/clj_surgeon/mcp_inspect_tool_test.clj
    CONFLICT (content): Merge conflict in test/clj_surgeon/mcp_inspect_tool_test.clj
    Auto-merging test/clj_surgeon/mcp_test_runner.clj
    Auto-merging test/run_all.clj
    EXIT_CODE=1
    ```

    Exact command:

    ```sh
    git merge-tree --write-tree HEAD origin/bridge/integration-2026-09-03-mem003
    ```

    Verbatim conflict output and exit:

    ```text
    8d66bafbeb04d4b24f154ee1c45e90f9c841e923
    Auto-merging CLAUDE.md
    Auto-merging Makefile
    Auto-merging deps.edn
    Auto-merging docs/high-level-design.md
    Auto-merging docs/tech-tree.md
    CONFLICT (content): Merge conflict in docs/tech-tree.md
    Auto-merging src/clj_surgeon/core.clj
    CONFLICT (content): Merge conflict in src/clj_surgeon/core.clj
    Auto-merging src/clj_surgeon/mcp_inspect.clj
    CONFLICT (content): Merge conflict in src/clj_surgeon/mcp_inspect.clj
    Auto-merging src/clj_surgeon/mcp_inspect_tool.clj
    CONFLICT (content): Merge conflict in src/clj_surgeon/mcp_inspect_tool.clj
    Auto-merging src/clj_surgeon/mcp_intent_contract.clj
    CONFLICT (content): Merge conflict in src/clj_surgeon/mcp_intent_contract.clj
    Auto-merging src/clj_surgeon/mcp_operation.clj
    CONFLICT (content): Merge conflict in src/clj_surgeon/mcp_operation.clj
    Auto-merging src/clj_surgeon/mcp_tool.clj
    Auto-merging test/clj_surgeon/ls_tree_test.clj
    Auto-merging test/clj_surgeon/mcp_http_server_test.clj
    Auto-merging test/clj_surgeon/mcp_inspect_contract_test.clj
    Auto-merging test/clj_surgeon/mcp_inspect_tool_test.clj
    CONFLICT (content): Merge conflict in test/clj_surgeon/mcp_inspect_tool_test.clj
    Auto-merging test/clj_surgeon/mcp_prepared_request_test.clj
    Auto-merging test/clj_surgeon/mcp_test_runner.clj
    Auto-merging test/run_all.clj
    CONFLICT (content): Merge conflict in test/run_all.clj
    EXIT_CODE=1
    ```

## NO-GO
This tip is not GO on its own for MCP/main because public results still omit undeclared structured facts and an envelope collision makes the fit return 81,861 bytes against a 32,768-byte cap, and it does not currently compose cleanly onto either `origin/MCP/main` or the MEM-003 second landing at `694f538d`.
