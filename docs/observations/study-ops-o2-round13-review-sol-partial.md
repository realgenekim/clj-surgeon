## NO-GO

1. Review subject is the requested clean detached commit.

   Exact command:

   ```text
   git checkout --detach e24ee131
   git status --short --branch
   git rev-parse HEAD
   ```

   Verbatim output:

   ```text
   HEAD is now at e24ee131 merge-fix: the trunk's fence witnesses were red against this branch's kernel — three real gaps
   ## HEAD (no branch)
   e24ee131684c40898141001c948a79e14a209ee4
   ```

   Location: repository root at `e24ee131:1`.

2. **BLOCKING — MCP-OP-STUDY-054 does not identify the wire member after UTF-8 serialization.** `wire-member-name` compares JVM strings before the byte boundary. A receipt with the distinct keys lone high-surrogate U+D800 and `"?"` passes the collision gate, publishes `error_type=nil`, and UTF-8 publication turns both keys into the same `"?"` member. Decoding leaves one member. Thus choice (a) is not enforced for every wire-spelling collision. The ordinary requested attacks do pass: `0`/`"0"` and a collision three levels down produce fitting typed refusals with zero uncarried leaves; newline and literal-backslash-n keys stay distinct; 10,000 noncolliding keys scan in 64 ms and return a fitting typed output-limit refusal in 541 ms. Files: `src/clj_surgeon/mcp_inspect.clj:698`, `src/clj_surgeon/mcp_inspect.clj:721`, `src/clj_surgeon/mcp_inspect.clj:752`, `src/clj_surgeon/mcp_inspect_tool.clj:2240`, `docs/intent/study-ops/study-ops-specs.md:122`.

   Exact command:

   ```text
   ~/bin/suite-run clojure -M:clj-surgeon/mcp /var/tmp/forge/o2r13-review-fx/round13-collision-attacks.clj; code=$?; echo COLLISION_ATTACK_EXIT_CODE=$code
   ```

   Verbatim output:

   ```text
   Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge
   zero-string {:collision {:path [], :member 0, :keys [0 0]}, :error_type receipt-key-collision, :path , :member 0, :keys [0 "0"], :bytes 1694, :uncarried 0}
   nested-three-levels {:collision {:path [:level1 0 :level2 :level3], :member a, :keys [:a a]}, :error_type receipt-key-collision, :path level1[0].level2.level3, :member a, :keys [:a "a"], :bytes 1730, :uncarried 0}
   valid-json-escape-distinct {:collision nil, :error_type nil, :path nil, :member nil, :keys nil, :bytes 880, :uncarried 0}
   ten-thousand-keys {:collision nil, :scan_ms 64.17792, :fit_ms 540.942295, :error_type inspect-output-limit, :bytes 1571, :fits true}
   post-utf8-collision {:wire {"?":"the-same-distinctive-value-rendered-twice","?":"the-same-distinctive-value-rendered-twice"}, :raw_key_code_units [55296], :declared_text inspect_clojure
     1 request · 0 files

   ✓ all requests resolved
   ✓ ordered snapshot
   ✓ hashes attached
   ✓ terminal evidence · read_complete=true · next action none

     receipt facts · 10 of 10 rendered
     source_character_count=0
     elapsed_ms=1.25
     read_complete=true
     file_count=0
     operation=inspect_clojure
     mode=outline
     ?: the-same-distinctive-value-rendered-twice
     ok=true
     request_count=1
     ?: the-same-distinctive-value-rendered-twice
     0 source characters · 1.25 ms, :decoded {? the-same-distinctive-value-rendered-twice}, :error_type nil, :decoded_member_count 1, :gate_collision nil, :generated_code_units [123 34 55296 34 58 34 116 104 101 45 115 97 109 101 45 100 105 115 116 105 110 99 116 105 118 101 45 118 97 108 117 101 45 114 101 110 100 101 114 101 100 45 116 119 105 99 101 34 44 34 63 34 58 34 116 104 101 45 115 97 109 101 45 100 105 115 116 105 110 99 116 105 118 101 45 118 97 108 117 101 45 114 101 110 100 101 114 101 100 45 116 119 105 99 101 34 125], :wire_duplicate_member_count 2, :published_bytes 835, :uncarried 0}
   COLLISION_ATTACK_EXIT_CODE=0
   ```

3. The trunk merge’s four ported shell intents are real, driven controls rather than stubs. SHELL-ARGV-004 drives all three discovered build-file names through both real launchers and verifies no reader-eval side effect; -005 scans `src/` for the full evaluator vocabulary; -006 drives escaping and non-string `:paths` through the ported `study/confined-source-dir` and both output formats; -007 drives a 10,001-deep build file to the shared reader-free `argv-depth` ceiling. The focused namespace is 7/74/0. The `find` start-point defect reproduces at the O2 pre-merge parent `6f3fb04a` and is fixed at the tip: a real directory literally named `target` changes from no files/exit 1 to the expected file/exit 0. Files: `src/clj_surgeon/study.clj:134`, `src/clj_surgeon/study.clj:183`, `src/clj_surgeon/study.clj:212`, `src/clj_surgeon/study.clj:335`, `src/clj_surgeon/study.clj:1082`, `src/clj_surgeon/argv_depth.clj:41`, `src/clj_surgeon/argv_depth.clj:99`, `test/clj_surgeon/reader_eval_fence_test.clj:92`, `test/clj_surgeon/reader_eval_fence_test.clj:155`, `test/clj_surgeon/reader_eval_fence_test.clj:221`, `test/clj_surgeon/reader_eval_fence_test.clj:289`, `test/clj_surgeon/reader_eval_fence_test.clj:525`.

   Exact command:

   ```text
   review_cp=$(clojure -Spath -M:clj-surgeon/mcp-test)
   ~/bin/suite-run java -cp "$review_cp" clojure.main -e "(require 'clj-surgeon.reader-eval-fence-test)(let [r (clojure.test/run-tests 'clj-surgeon.reader-eval-fence-test)] (System/exit (+ (:fail r) (:error r))))"
   printf 'READER_FENCE_EXIT_CODE=%s\n' "$?"
   ```

   Verbatim output:

   ```text
   Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge
   Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge

   Testing clj-surgeon.reader-eval-fence-test

   Ran 7 tests containing 74 assertions.
   0 failures, 0 errors.
   READER_FENCE_EXIT_CODE=0
   ```

   Exact command:

   ```text
   git checkout --quiet --detach 6f3fb04a
   printf 'REV=%s\n' "$(git rev-parse --short=8 HEAD)"
   bb -cp src -m clj-surgeon.core :op :ls-tree :dir /var/tmp/forge/o2r13-review-fx/target :format :edn
   printf 'MERGE_BASE_EXIT_CODE=%s\n' "$?"
   git checkout --quiet --detach e24ee131
   printf 'REV=%s\n' "$(git rev-parse --short=8 HEAD)"
   bb -cp src -m clj-surgeon.core :op :ls-tree :dir /var/tmp/forge/o2r13-review-fx/target :format :edn
   printf 'TIP_EXIT_CODE=%s\n' "$?"
   ```

   Verbatim output:

   ```text
   REV=6f3fb04a
   No Clojure files found under /var/tmp/forge/o2r13-review-fx/target
   MERGE_BASE_EXIT_CODE=1
   REV=e24ee131
   [{:ns demo,
     :file "src/demo.clj",
     :lines 3,
     :form-count 1,
     :forms
     [{:type defn,
       :platforms [:clj],
       :line 3,
       :end-line 3,
       :name answer,
       :args "[]"}],
     :requires []}
    {:receipt {:resources {:scan_ms 1.204, :bytes_scanned 31}}}]
   TIP_EXIT_CODE=0
   ```
