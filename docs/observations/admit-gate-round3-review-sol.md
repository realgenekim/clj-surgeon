## NO-GO

1. **BLOCKING — the source-derived refusal enumeration is already incomplete, and its witness remains green when a reachable kind is constructed dynamically.** `test/clj_surgeon/admit_patch_test.clj:4336` scans five fixed source files and literal shapes. `src/clj_surgeon/mcp_admit_tool.clj:1901` forwards arbitrary `ex-data :error-type`; `src/clj_surgeon/workspace_lock.clj:83` supplies `:workspace-lock-unavailable`, which is exercised at `test/clj_surgeon/admit_patch_test.clj:1976` but absent from the claimed 37-kind set. Instrumenting every `execute-request!` call in the 120-test admit suite found the live omission:

   Exact command:

   ```text
   ~/bin/suite-run bash -lc 'exec java -cp "$(clojure -Spath -A:clj-surgeon/mcp-test)" clojure.main /var/tmp/forge/gate3-review-fx/runtime-refusals.clj'; gate_rc=$?; echo EXIT_CODE=$gate_rc
   ```

   Verbatim output:

   ```text
   Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge

   Testing clj-surgeon.admit-patch-test

   Ran 120 tests containing 2555 assertions.
   0 failures, 0 errors.
   {:runtime-derived #{"admit-tool-failure" "duplicate-definition" "duplicate-patch-target" "hunk-truncated" "invalid-admit-request" "invalid-patch" "invalid-relative-source-path" "namespace-form-removed" "no-op-patch" "patch-does-not-apply" "patch-too-large" "path-outside-project" "require-removed" "source-file-not-found" "source-hash-mismatch" "target-already-exists" "unreadable-post-image" "unsupported-patch-target" "verification-failed" "verification-incomplete" "workspace-lock-unavailable"}, :runtime-count 21, :source-derived #{"admit-tool-error" "admit-tool-failure" "analyzer-memory-exhausted" "analyzer-output-truncated" "binary-patch-unsupported" "clj-kondo-unavailable" "duplicate-definition" "duplicate-patch-target" "hunk-truncated" "invalid-admit-request" "invalid-compiled-transaction" "invalid-patch" "invalid-relative-source-path" "invalid-source-path" "invalid-target-path" "invalid-workspace-root" "namespace-form-removed" "no-op-patch" "overlapping-hunks" "patch-does-not-apply" "patch-source-missing" "patch-too-large" "path-outside-project" "require-removed" "server-not-initialized" "source-file-not-found" "source-hash-mismatch" "source-not-regular-file" "target-already-exists" "target-parent-not-directory" "transaction-recovery-required" "transaction-write-exception" "transaction-write-failed" "unreadable-post-image" "unsupported-patch-target" "verification-failed" "verification-incomplete"}, :source-count 37, :runtime-minus-source #{"workspace-lock-unavailable"}, :source-minus-runtime #{"admit-tool-error" "analyzer-memory-exhausted" "analyzer-output-truncated" "binary-patch-unsupported" "clj-kondo-unavailable" "invalid-compiled-transaction" "invalid-source-path" "invalid-target-path" "invalid-workspace-root" "overlapping-hunks" "patch-source-missing" "server-not-initialized" "source-not-regular-file" "target-parent-not-directory" "transaction-recovery-required" "transaction-write-exception" "transaction-write-failed"}}
   EXIT_CODE=0
   ```

   On a scratch export I changed the reachable invalid-mode refusal to `(keyword "planted-runtime-kind")`. Both new enumeration/rendering witnesses stayed green and omitted the live kind:

   Exact command:

   ```text
   ~/bin/suite-run bash -lc 'exec java -cp "$(clojure -Spath -A:clj-surgeon/mcp-test)" clojure.main planted-kind-probe.clj'; gate_rc=$?; echo EXIT_CODE=$gate_rc
   ```

   Verbatim output:

   ```text
   Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge
   {:witness-counters {:test 2, :pass 376, :fail 0, :error 0}, :live-error-type :planted-runtime-kind, :source-derived-contains-planted false, :source-derived-count 37}
   EXIT_CODE=0
   ```

2. **BLOCKING — “every leaf” is false by explicit exclusion and by shape, and the test copies the exclusion policy it is meant to check.** `src/clj_surgeon/mcp_admit_tool.clj:1949-1963` excludes `:files` and `:hashes`; `src/clj_surgeon/mcp_admit_tool.clj:1986-2007` emits no entry for empty maps, empty sequences, or `nil`. The independently implemented traversal at `test/clj_surgeon/admit_patch_test.clj:4385` is useful, but `test/clj_surgeon/admit_patch_test.clj:4406-4418` independently hard-codes the same exclusions, making the exclusion-policy check tautological. Blank strings render, while empty collections and nil disappear. The stated numeric bounds themselves are correct at 200/201 characters and 40/41 facts.

   Exact command:

   ```text
   ~/bin/suite-run bash -lc 'exec java -cp "$(clojure -Spath -A:clj-surgeon/mcp-test)" clojure.main /var/tmp/forge/gate3-review-fx/render-probes.clj'; gate_rc=$?; echo EXIT_CODE=$gate_rc
   ```

   Verbatim output:

   ```text
   Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge
   {:probe :shape-exclusions, :contains-files false, :contains-pre-hash false, :contains-empty false, :contains-map false, :contains-nil false, :contains-blank true}
   {:probe :fact-character-bound, :input-length 200, :full-value-present true, :ellipsis-present false}
   {:probe :fact-character-bound, :input-length 201, :full-value-present false, :ellipsis-present true}
   {:probe :fact-count-bound, :input-count 40, :overflow-marker false, :contains-last true}
   {:probe :fact-count-bound, :input-count 41, :overflow-marker true, :contains-last false}
   {:probe :next-call-bound, :padding-length 144, :encoded-length 201, :verbatim true, :pointer false}
   {:probe :next-call-bound, :padding-length 967, :encoded-length 1024, :verbatim true, :pointer false}
   {:probe :next-call-bound, :padding-length 968, :encoded-length 1025, :verbatim false, :pointer true}
   {:probe :successful-commit-superset, :contains-file-path false, :contains-pre-hash false, :contains-post-hash false, :contains-test-namespace false, :contains-detectors-empty false, :contains-protected-drift false}
   EXIT_CODE=0
   ```

3. **BLOCKING — the successful branch is not text ⊇ structuredContent, and the relabelled witness still is not a real superset assertion.** `src/clj_surgeon/mcp_admit_tool.clj:2068-2084` renders counts/status/next_call for `:ok true`, not the receipt leaves. A real two-file COMMIT carried file records, pre/post hashes, focused test namespaces, and `detectors_not_run []`; its text omitted the first file, first pre-hash, and first test namespace. At `test/clj_surgeon/admit_patch_test.clj:3818`, the old overclaim was correctly renamed, but the replacement block at `test/clj_surgeon/admit_patch_test.clj:3830-3839` calls itself “really ... a superset” while asserting only that `next_call` appears.

   Exact command:

   ```text
   git show 9e1b587a:test/clj_surgeon/admit_patch_test.clj | rg -n 'testing "the text block is a superset of the structured receipt"'; rg -n 'testing "the text block (names every detector|really is a superset)' test/clj_surgeon/admit_patch_test.clj
   ```

   Verbatim output:

   ```text
   3816:          (testing "the text block is a superset of the structured receipt"
   3818:          (testing "the text block names every detector and reason detectors_not_run carries"
   3830:          (testing "the text block really is a superset of the structured receipt"
   ```

   Exact replay/COMMIT command:

   ```text
   ~/bin/suite-run bash -lc 'exec java -cp "$(clojure -Spath -A:clj-surgeon/mcp-test)" clojure.main /var/tmp/forge/gate3-review-fx/replay-next-call.clj'; gate_rc=$?; echo EXIT_CODE=$gate_rc
   ```

   Verbatim output:

   ```text
   Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge
   {:pre-image-binding "bound", :extracted-hash-files #{"app/core.clj" "app/util.clj"}, :commit-ok true, :error-type nil, :text-next-call-prefix "next_call · {\"tool\":\"admit_clojure_patch\",\"arguments\":{\"mode\":\"commit\",\"verify\":\"focused\",", :structured-first-file "src/app/core.clj", :structured-first-test "app.core-test", :commit-text "admit_clojure_patch\n  admit-patch! · 2 file(s) · owners +0 ~2 -0 · drift 0 bytes · hazards 0 · 1.00 ms\nverification_complete=true verification_status=complete\nnext_call · none — this receipt has no follow-up call", :text-has-first-pre false, :structured-detectors-not-run [], :committed true, :extracted-mode "commit", :preview-ok true, :text-has-first-file false, :text-has-first-test false, :structured-first-pre "73b6d249fcac76606fc2fd9ae8ed5309d82d20f37a16cd09d8a2eac220b4621a"}
   EXIT_CODE=0
   ```

   This also proves the small happy path: the JSON extracted from preview text carried both `expect_pre_sha256` entries and committed unchanged bytes with `pre_image_binding="bound"`.

4. **BLOCKING — a reachable `next_call` becomes non-copyable text.** The tool description at `src/clj_surgeon/mcp_admit_tool.clj:66` tells callers to copy `expect_pre_sha256`. `src/clj_surgeon/mcp_admit_tool.clj:1978-1983,2051-2066` replaces JSON above 1,024 characters with a structuredContent pointer. A routine 14-file preview produced 1,554 characters, so a text-only caller cannot perform the instructed copy/send operation. This is not merely a synthetic boundary case.

   Exact command:

   ```text
   ~/bin/suite-run bash -lc 'exec java -cp "$(clojure -Spath -A:clj-surgeon/mcp-test)" clojure.main /var/tmp/forge/gate3-review-fx/large-next-call.clj'; gate_rc=$?; echo EXIT_CODE=$gate_rc
   ```

   Verbatim output:

   ```text
   Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge
   {:ok true, :file-count 14, :next-call-characters 1554, :verbatim-in-text false, :pointer-in-text true}
   EXIT_CODE=0
   ```

5. **The RED/GREEN and remedy-removal controls are genuine, but they do not catch findings 1–4.** `test/clj_surgeon/admit_patch_test.clj:4421-4489` detects the intended remedy/next_call regression. RED `98d17270` reproduces 341 failures; green `95e7aed9` is 120/2555/0; deleting the remedy line from `src/clj_surgeon/mcp_admit_tool.clj:2093-2094` on a clean scratch export reproduces 39 failures.

   Exact RED command (working directory `/var/tmp/forge/gate3-review-fx/export-red`):

   ```text
   set -o pipefail; ~/bin/suite-run bash -lc 'exec java -cp "$(clojure -Spath -A:clj-surgeon/mcp-test)" clojure.main run-admit.clj' | tail -n 4; gate_rc=${PIPESTATUS[0]}; echo EXIT_CODE=$gate_rc
   ```

   Verbatim output:

   ```text
   Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge
     actual: false

   Ran 120 tests containing 2620 assertions.
   341 failures, 0 errors.
   EXIT_CODE=1
   ```

   Exact green command:

   ```text
   ~/bin/suite-run bash -lc 'exec java -cp "$(clojure -Spath -A:clj-surgeon/mcp-test)" clojure.main /var/tmp/forge/gate3-review-fx/run-admit.clj'; gate_rc=$?; echo EXIT_CODE=$gate_rc
   ```

   Verbatim output:

   ```text
   Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge

   Testing clj-surgeon.admit-patch-test

   Ran 120 tests containing 2555 assertions.
   0 failures, 0 errors.
   EXIT_CODE=0
   ```

   Exact remedy sabotage command (working directory `/var/tmp/forge/gate3-review-fx/export-remedy`):

   ```text
   set -o pipefail; ~/bin/suite-run bash -lc 'exec java -cp "$(clojure -Spath -A:clj-surgeon/mcp-test)" clojure.main run-admit.clj' | tail -n 4; gate_rc=${PIPESTATUS[0]}; echo EXIT_CODE=$gate_rc
   ```

   Verbatim output:

   ```text
   Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge
     actual: (not (str/includes? "admit_clojure_patch refused · verification-incomplete · 1.25 ms\none sentence stating the verification-incomplete cause\nsource unchanged\nfacts · lint_delta.cap=999 · lint_delta.detector=clj-kondo · lint_delta.observed-bytes=1234 · lint_delta.ok=false · lint_delta.ran=false\nnext_call · {\"tool\":\"admit_clojure_patch\",\"arguments\":{\"mode\":\"preview\",\"verify\":\"focused\"},\"patch_field\":\"patch\",\"patch_sha256\":\"deadbeef\",\"blocked_by\":\"verification-incomplete\"}" "Resend the next_call; it corrects verification-incomplete."))

   Ran 120 tests containing 2555 assertions.
   39 failures, 0 errors.
   EXIT_CODE=39
   ```

6. **Required gates and checkout provenance all reproduce.** `Makefile:180-183` owns the operation oracle; `src/clj_surgeon/mcp_intent_contract.clj:159` owns the repository audit. The intent audit is marker-presence evidence and does not cure the behavioral failures above.

   Exact provenance command:

   ```text
   git rev-parse HEAD; git merge-base --is-ancestor 9e1b587a HEAD; echo BASE_ANCESTOR_EXIT=$?; git log --oneline 9e1b587a..95e7aed9; test -z "$(git status --porcelain)"; echo CLEAN_EXIT=$?
   ```

   Verbatim output:

   ```text
   95e7aed9bb9c38014d7bec0c4e7fd5ad669cba43
   BASE_ANCESTOR_EXIT=0
   95e7aed9 GREEN: admit gate refusal text renders remedy, next_call, and every leaf (MCP-OP-ADMIT-131/132)
   98d17270 RED: admit gate refusal text drops remedy, next_call, and every nested leaf (inb-cbca17)
   CLEAN_EXIT=0
   ```

   Exact command:

   ```text
   ~/bin/suite-run clojure -M:clj-surgeon/mcp-test
   ```

   Verbatim terminal output (exit 0):

   ```text
   Testing clj-surgeon.workspace-onboarding-test

   Ran 717 tests containing 8869 assertions.
   0 failures, 0 errors.
   ```

   Exact command:

   ```text
   ~/bin/suite-run bb test/run_all.clj
   ```

   Verbatim terminal output (exit 0):

   ```text
   Ran 814 tests containing 6724 assertions.
   0 failures, 0 errors.
   ```

   Exact command:

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

   Exact command:

   ```text
   ~/bin/suite-run clojure -M -e "(require '[clj-surgeon.mcp-intent-contract :as audit]) (let [r (audit/audit-current-repository)] (prn {:ok (:ok r) :specs (count (:specs r)) :violations (count (:violations r))}))"; gate_rc=$?; echo EXIT_CODE=$gate_rc
   ```

   Verbatim output:

   ```text
   Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge
   {:ok true, :specs 352, :violations 0}
   EXIT_CODE=0
   ```

## NO-GO

95e7aed9 may not land on MCP/main by merge --no-ff.
