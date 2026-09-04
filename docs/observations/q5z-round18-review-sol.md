## NO-GO

Round-eighteen independent review of `bridge/q5z-alias-migration` at `1f92608e`.

1. **BLOCKING — the reader fix recognizes the six advertised constructor shapes, but a constructor whose kind is its SECOND parameter disables the whole per-file scan and leaves a live kind unenumerated.** `test/clj_surgeon/mcp_alias_migration_test.clj:1654` (`own-refusal-constructor-takes-kind?`) examines only the first parameter; `test/clj_surgeon/mcp_alias_migration_test.clj:1718` gates every call site in the file on that result. The direct matrix also confirms the required form-discovery and mint attacks: reader conditionals, macro bodies, and `#_` discards are seen; aliased/applied calls are invisible as explicitly declared; all named deep-walk attacks are rejected as forwards, including the opaque helper call and string table.

   Ruling on the adjacent shapes: `(mk kind)` is correctly NAMED, hence must become a typed refusal rather than an acceptable declaration. A `refusal` Var aliased through `def`/`let`, and `(apply refusal …)`, remain acceptable-declared only under the source comment's explicit no-alias/no-apply assumption and the real-tree `rg` showing no such call; they are not literal `(refusal …)` sites. A reader-conditional and macro-body call are conservatively visible. A `#_` call is a conservative false positive, never an invisible live kind.

   Exact command:

   ```text
   ~/bin/suite-run bash -lc 'cp=$(clojure -Spath -M:clj-surgeon/mcp-test); exec java -cp "$cp" clojure.main /var/tmp/forge/q5z18-review-fx/probe_scan.clj'; rc=$?; echo EXIT=$rc
   ```

   Verbatim output:

   ```text
   Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge
   === constructors ===
   canonical          own=true  sites=1 literal=[] dynamic=["plant:4"]
   docstring          own=true  sites=1 literal=[] dynamic=["plant:4"]
   multi-arity        own=true  sites=1 literal=[] dynamic=["plant:4"]
   def-fn             own=true  sites=1 literal=[] dynamic=["plant:4"]
   attribute-map      own=true  sites=1 literal=[] dynamic=["plant:4"]
   arg-k              own=true  sites=1 literal=[] dynamic=["plant:4"]
   SECOND-param-kind  own=false sites=1 literal=["plant"] dynamic=[]
   === call discovery ===
   reader-conditional sites=1 dynamic=["plant:2"]
   macro-body         sites=1 dynamic=["plant:2"]
   discard            sites=1 dynamic=["plant:2"]
   def-alias          sites=0 dynamic=[]
   let-alias          sites=0 dynamic=[]
   apply              sites=0 dynamic=[]
   === forwarded attacks ===
   cond->             forwarded=false
   as->               forwarded=false
   apply-keyword      forwarded=false
   let-f-keyword      forwarded=false
   case               forwarded=false
   if                 forwarded=false
   or                 forwarded=false
   helper             forwarded=false
   string-table       forwarded=false
   EXIT=0
   ```

2. **MECHANICALLY CLEAN, SEMANTICALLY NO-GO — the tip merges without conflicts but is not GO on its own for MCP/main.** The dry-run is clean against the trunk SHA actually tested, `a44ded9277607512b2af6f420bfc3f47b31b81b3` (newer than the builder's claimed `71c1ed98`), but finding 1/9 meets the brief's explicit blocking rule.

   Exact command:

   ```text
   git merge-tree --write-tree HEAD origin/MCP/main; rc=$?; echo MERGE_TREE_EXIT=$rc; git rev-parse origin/MCP/main
   ```

   Verbatim output:

   ```text
   8bace5077d5e99450e07c2095c35b03fbacc6bfe
   MERGE_TREE_EXIT=0
   a44ded9277607512b2af6f420bfc3f47b31b81b3
   ```


3. **VERIFIED — every requested RED commit is red for its stated reason and the following GREEN is green.** The ladder uses untouched `git archive` exports of each SHA and the named witness only. Round seventeen reproduces 5→0 (deep mint), 4→0 (form reader), 6→0 (allowlist), and 1→0 (receipt deadline). Round eighteen reproduces 5→0 (reader constructor) and 4→0 (the combined string-literal plus call-site enumeration RED); the separately reverted green seams are the 3/1 sabotage results in finding 7.

   Exact command:

   ```text
   chmod +x /var/tmp/forge/q5z18-review-fx/ladder.sh && ~/bin/suite-run /var/tmp/forge/q5z18-review-fx/ladder.sh; rc=$?; echo LADDER_EXIT=$rc
   ```

   Verbatim result lines:

   ```text
   ########## red1 :: the-forwarded-kind-check-is-form-deep-and-not-merely-head-shaped
   RESULT= {:test 1, :pass 2, :fail 5, :error 0}
   ########## green1 :: the-forwarded-kind-check-is-form-deep-and-not-merely-head-shaped
   RESULT= {:test 1, :pass 7, :fail 0, :error 0}
   ########## red2 :: the-refusal-call-site-scan-reads-forms-and-not-lines
   RESULT= {:test 1, :pass 0, :fail 4, :error 0}
   ########## green2 :: the-refusal-call-site-scan-reads-forms-and-not-lines
   RESULT= {:test 1, :pass 4, :fail 0, :error 0}
   ########## red3 :: the-scalar-allowlist-refuses-a-benign-subclass-of-an-admitted-class
   RESULT= {:test 1, :pass 14, :fail 6, :error 0}
   ########## green3 :: the-scalar-allowlist-refuses-a-benign-subclass-of-an-admitted-class
   RESULT= {:test 1, :pass 20, :fail 0, :error 0}
   ########## red4 :: the-refusal-fact-line-spends-one-print-budget-for-the-whole-receipt
   RESULT= {:test 1, :pass 2, :fail 1, :error 0}
   ########## green4 :: the-refusal-fact-line-spends-one-print-budget-for-the-whole-receipt
   RESULT= {:test 1, :pass 3, :fail 0, :error 0}
   ########## red5 :: the-refusal-constructor-is-decided-by-the-reader-and-not-by-a-regex
   RESULT= {:test 1, :pass 2, :fail 5, :error 0}
   ########## green5 :: the-refusal-constructor-is-decided-by-the-reader-and-not-by-a-regex
   RESULT= {:test 1, :pass 7, :fail 0, :error 0}
   ########## red6 :: the-forwarded-kind-check-is-form-deep-and-not-merely-head-shaped
   RESULT= {:test 1, :pass 8, :fail 4, :error 0}
   ########## green6 :: the-forwarded-kind-check-is-form-deep-and-not-merely-head-shaped
   RESULT= {:test 1, :pass 12, :fail 0, :error 0}
   LADDER_EXIT=0
   ```

4. **VERIFIED — all four gates reproduce the claimed tip figures.** `test/clj_surgeon/mcp_alias_migration_test.clj:5546` ran within the 509-test MCP suite, so the real-tree 139-count and frozen membership checks passed in both directions.

   Exact command:

   ```text
   ~/bin/suite-run bb test/run_all.clj; rc=$?; echo BB_EXIT=$rc
   ```

   Verbatim terminal output:

   ```text
   Ran 737 tests containing 6275 assertions.
   0 failures, 0 errors.
   BB_EXIT=0
   ```

   Exact command:

   ```text
   ~/bin/suite-run clojure -M:clj-surgeon/mcp-test; rc=$?; echo MCP_TEST_EXIT=$rc
   ```

   Verbatim terminal output:

   ```text
   Ran 509 tests containing 7112 assertions.
   0 failures, 0 errors.
   MCP_TEST_EXIT=0
   ```

   Exact command:

   ```text
   make mcp-operation-oracle; rc=$?; echo ORACLE_EXIT=$rc
   ```

   Verbatim output:

   ```text
   # @spec MCP-OP-ORACLE-001
   swipl -q -f test/mcp_operation_contract_oracle.pl
   mcp-operation oracle: pass; legacy counterexamples=[verification_failed,verification_pending]
   ORACLE_EXIT=0
   ```

   Exact command:

   ```text
   make repository-hygiene; rc=$?; echo HYGIENE_EXIT=$rc
   ```

   Verbatim output:

   ```text
   # @spec MCP-OP-ALIAS-036
   # @spec MCP-OP-ALIAS-053
   repository hygiene: no machine-local build cache is tracked at any depth
   HYGIENE_EXIT=0
   ```



5. **VERIFIED — FAN run 1 is 6/6 and byte-identical after exactly one live `tools/call`; `:nrepl-port :none` writes no `.nrepl-port`, so CHECK 1 reports `extras=0` without deletion.** The server was the reviewed tip; the scorer was archived from `origin/MCP/main` at `710d116d1c629f9dc2aa48a302853de08275dc75`. The round-seventeen ruling stands: a default server writing `.nrepl-port` into a measured project is a server-launch defect, not a scorer defect; CHECK 1 must continue counting every unexpected file. This launch fixes the harness input, not the scorer.

   Exact server command on the allowed explicit port:

   ```text
   clojure -X:clj-surgeon/mcp :project-dir '"/var/tmp/forge/q5z18-review-fx/fan/repo-21"' :port 8164 :nrepl-port :none :telemetry :full :telemetry-dir '"/var/tmp/forge/q5z18-review-fx/fan/telemetry"'
   ```

   Verbatim output before the call:

   ```text
   Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge
   clj-surgeon MCP: persistent server ready on http://127.0.0.1:8164/mcp
   ```

   Exact pre-call contamination check:

   ```text
   test ! -e /var/tmp/forge/q5z18-review-fx/fan/repo-21/.nrepl-port; rc=$?; echo NREPL_PORT_ABSENT_EXIT=$rc; git -C /var/tmp/forge/q5z18-review-fx/fan/repo-21 status --short
   ```

   Verbatim output:

   ```text
   NREPL_PORT_ABSENT_EXIT=0
   ```

   Exact sole live call:

   ```text
   chmod +x /var/tmp/forge/q5z18-review-fx/mcp-call.sh && /var/tmp/forge/q5z18-review-fx/mcp-call.sh 8164 alias_migration /var/tmp/forge/q5z18-review-fx/fan/call.json; rc=$?; echo CALL_EXIT=$rc
   ```

   Verbatim output:

   ```text
   SESSION=ef62f9c3-aeb9-4ca7-a123-23488b1f2e86
   id: ef62f9c3-aeb9-4ca7-a123-23488b1f2e86
   event: message
   data: {"jsonrpc":"2.0","id":2,"result":{"content":[{"type":"text","text":"alias_migration\n  21 files · 63 sites · aliases {\"es\" 5, \"st2\" 5, \"store-2\" 5, \"store2\" 6} · 30 collisions resolved · 816.00 ms\n\n✓ atomic commit complete\n✓ written bytes read back and verified\n✓ terminal evidence · per-file detail at .clj-surgeon/alias-migration/detail-f2e80810-3cf8-40af-88ec-1a83a30a4623.edn (best-effort retention)"}],"isError":false,"structuredContent":{"details_retained":20,"workspace_root":"/var/tmp/forge/q5z18-review-fx/fan/repo-21","committed":true,"kondo_delta":{"status":"not-requested"},"alias_histogram":{"es":5,"st2":5,"store-2":5,"store2":6},"sites":63,"string_mention_sites_shown":0,"details_retention":"best-effort","focused_test":{"status":"not-requested"},"string_mentions":0,"string_mention_sites":[],"lib_renamed":null,"elapsed_ms":815.996214,"next_action":"none","refer_sites":0,"files":21,"collisions_resolved":30,"receipt_hash":"dd0fb6df1885d1f239ab76a6e486126992cb3629f755a432001ed9519ab04ad2","ok":true,"operation":"alias_migration","undo_receipt":"/home/forge/.local/state/clj-surgeon/workspaces/3e1413728ba4a89703e977bf6639c3908320494792e6e26744f57364cf999ec8/receipts/2b68e152-2f1a-4e91-97b4-73565cc2ef8b.edn","details_path":".clj-surgeon/alias-migration/detail-f2e80810-3cf8-40af-88ec-1a83a30a4623.edn"}}}

   CALL_EXIT=0
   ```

   I stopped the server with `Ctrl-C` immediately after that response. Exact post-call check:

   ```text
   test ! -e /var/tmp/forge/q5z18-review-fx/fan/repo-21/.nrepl-port; echo NREPL_PORT_ABSENT_AFTER_CALL_EXIT=$?; ss -ltn | rg ':8164\b' || true
   ```

   Verbatim output:

   ```text
   NREPL_PORT_ABSENT_AFTER_CALL_EXIT=0
   ```

   Exact scorer and byte comparison command:

   ```text
   FAN_BASE=1a3243716ca1304f86c68ca84512ee86c9970819 ~/bin/suite-run bash /var/tmp/forge/q5z18-review-fx/scorer/bench/fanout/rescore-FAN.sh /var/tmp/forge/q5z18-review-fx/fan/repo-21 21 /var/tmp/forge/q5z18-review-fx/fan; rc=$?; echo SCORE_EXIT=$rc; diff -qr --exclude=.git --exclude=.clj-surgeon /var/tmp/forge/q5z18-review-fx/fan/canonical-21 /var/tmp/forge/q5z18-review-fx/fan/repo-21; echo BYTE_DIFF_EXIT=$?
   ```

   Verbatim output:

   ```text
   rescore-FAN: worktree=/var/tmp/forge/q5z18-review-fx/fan/repo-21 n=21 base=1a3243716ca1304f86c68ca84512ee86c9970819 base-from=FAN_BASE fixtures=/var/tmp/forge/q5z18-review-fx/fan
   fan_check: git=/usr/bin/git exec-path=/usr/lib/git-core version=git version 2.53.0 resolution=absolute-candidate
   CHECK 1 file-set: PASS changed=21 expected=21 missing=0 [] extras=0 []
   CHECK 2 form-equality: PASS compared=21 equal=21 unparseable=0 [] unequal=0 []
   CHECK 3 protected-regions: PASS regions=106 intact=106 manifest-sha-mismatch=0 damaged=0 []
   CHECK 6 residue-and-alias: PASS src-files=100 old-lib-hits=0 [] old-site-residue=0 [] wrong-or-missing-alias=0 [] shadowing=0 []
   fan_check: 4/4 structural checks passed
   CHECK 4 load: PASS namespaces=100 rc=0
   CHECK 5 behaviour: PASS FAN-TEST tests=21 assertions=147 failures=0 errors=0 (base count=21)
   rescore-FAN: 6/6 checks passed
   SCORE_EXIT=0
   BYTE_DIFF_EXIT=0
   ```

6. **CLOSED — exact-class rendering admits no hostile Number subclass, and the fact line spends one deadline.** `src/clj_surgeon/mcp_tool.clj:1351` tests membership by the actual `Class` object. A `Number` proxy's marker-bearing `toString` is never reached on its own, as a map value, or in a vector; BigInteger, BigDecimal, and Ratio subclasses are likewise opaque. `Object.getClass()` is `final native`, so its result cannot be spoofed, and name equality would not help because the allowlist contains `Class` objects. BigInt is final with only a private constructor, Character remains deliberately opaque, and `deftype` cannot extend the Number class. At `src/clj_surgeon/mcp_tool.clj:1613`, sixteen looping facts cost one measured 2,020 ms deadline, deadline 0 safely floors to an immediate marker, and all driven two-arity callers retain their old rendering.

   Exact command:

   ```text
   ~/bin/suite-run bash -lc 'cp=$(clojure -Spath -M:clj-surgeon/mcp-test); exec java -cp "$cp" clojure.main /var/tmp/forge/q5z18-review-fx/probe_render.clj'; rc=$?; echo EXIT=$rc
   ```

   Verbatim output:

   ```text
   Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge
   === exact-class allowlist ===
   Number proxy / own       class=user.proxy$java.lang.Number$ff19274a                    rendered="#object[Number$ff19274a 10a6f7dc]"
   Number proxy / map       class=clojure.lang.PersistentArrayMap                         rendered="{:a #object[Number$ff19274a 10a6f7dc]}"
   Number proxy / vector    class=clojure.lang.PersistentVector                           rendered="[#object[Number$ff19274a 10a6f7dc]]"
   BigInteger subclass      class=user.proxy$java.math.BigInteger$ff19274a                rendered="#object[BigInteger$ff19274a 4db0a570]"
   BigDecimal subclass      class=user.proxy$java.math.BigDecimal$ff19274a                rendered="#object[BigDecimal$ff19274a 4b706a19]"
   Ratio subclass           class=user.proxy$clojure.lang.Ratio$ff19274a                  rendered="#object[Ratio$ff19274a 710cb1e6]"
   Character                class=java.lang.Character                                     rendered="#object[Character 686ae84f]"
   real BigInt              class=clojure.lang.BigInt                                     rendered="42N"
   real BigDecimal          class=java.math.BigDecimal                                    rendered="1.25M"
   real Ratio               class=clojure.lang.Ratio                                      rendered="3/4"
   getClass modifiers= public final native
   BigInt final= true
   BigInt constructors= ["private"]
   deftype-extends-Number= REFUSED: only interfaces are supported, had: java.lang.Number
   === one receipt deadline ===
   wall_ms= 2020 markers= 16
   deadline-0= "#object[LazySeq 3368ce0d]"
   2-arity ordinary= [[42 "42"] ["s" "\"s\""] [{:a [1 2 3]} "{:a [1 2 3]}"] [[1 2] "[1 2]"] [nil "nil"] [:kw ":kw"] [3/4 "3/4"] [1.25M "1.25M"]]
   EXIT=0
   ```



7. **VERIFIED — all three claimed round-eighteen sabotage ratchets are causally bound: 5, 3, and 1 failures.** `test/clj_surgeon/mcp_alias_migration_test.clj:5136` witnesses the reader constructor detector; `:5229` witnesses string literal mint evidence and the `(refusal …)` enumeration. I first confirmed the sabotage text was actually present, then ran each exact witness in its own `git archive 1f92608e` export.

   Exact patch-confirmation command:

   ```text
   sed -n '1746,1751p' /var/tmp/forge/q5z18-review-fx/sab1/test/clj_surgeon/mcp_alias_migration_test.clj; sed -n '1325,1329p' /var/tmp/forge/q5z18-review-fx/sab2/test/clj_surgeon/mcp_alias_migration_test.clj; sed -n '1607,1613p' /var/tmp/forge/q5z18-review-fx/sab3/test/clj_surgeon/mcp_alias_migration_test.clj; sed -n '1758,1764p' /var/tmp/forge/q5z18-review-fx/sab3/test/clj_surgeon/mcp_alias_migration_test.clj
   ```

   Verbatim output:

   ```text
           ;; already has it. Decided with the READER — a text regex over the
           ;; argument name was switched off by one docstring.
           own-refusal-constructor?
           (boolean
             (re-find #"\(defn-?\s+refusal\s*\n?\s*\[\s*(error-type|kind)\b" text))]
       (vec
          (when (and (symbol? value) (contains? kind-minting-symbols value))
            [(str "mints with `" value "`")])
         (when (and (keyword? value)
                     (not head?)
                     (not selector?)
     [text]
     (for [site (refusal-call-sites-in text)
           :let [value (node-value (:kind site))]
           :when (and (keyword? value)
                      (re-matches #"[a-z][a-z0-9-]*" (name value)))]
       (name value)))
   ;; @spec MCP-OP-ALIAS-059
                               (re-matches #"[a-z][a-z0-9-]*" (name value))))
               :when (not (and (marked? (:row site))
                               (forwarded-kind-expression? (:kind site)
                                                           (:context site))))]
           (str label ":" (:row site))))))

   ;; @spec MCP-OP-ALIAS-059
   ```

   Exact sab1 command:

   ```text
   cd /var/tmp/forge/q5z18-review-fx/sab1
   ~/bin/suite-run bash -lc 'cp=$(clojure -Spath -M:clj-surgeon/mcp-test); exec java -cp "$cp" clojure.main /var/tmp/forge/q5z18-review-fx/run_var.clj the-refusal-constructor-is-decided-by-the-reader-and-not-by-a-regex'; rc=$?; echo EXIT=$rc
   ```

   Verbatim output:

   ```text
   Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge
   Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge
   VAR= the-refusal-constructor-is-decided-by-the-reader-and-not-by-a-regex

   FAIL in (the-refusal-constructor-is-decided-by-the-reader-and-not-by-a-regex) (mcp_alias_migration_test.clj:5206)
   every constructor shape that takes the kind first enables the scan with a docstring
   the planted dynamic site was not reported for a constructor shaped `with a docstring` — one ordinary edit to the constructor switched the whole file's scan off: []
   expected: (= [(str "plant:" (planted-row text))] (dynamic-refusal-kind-sites-in "plant" text))
     actual: (not (= ["plant:8"] []))

   FAIL in (the-refusal-constructor-is-decided-by-the-reader-and-not-by-a-regex) (mcp_alias_migration_test.clj:5206)
   every constructor shape that takes the kind first enables the scan multi-arity
   the planted dynamic site was not reported for a constructor shaped `multi-arity` — one ordinary edit to the constructor switched the whole file's scan off: []
   expected: (= [(str "plant:" (planted-row text))] (dynamic-refusal-kind-sites-in "plant" text))
     actual: (not (= ["plant:11"] []))

   FAIL in (the-refusal-constructor-is-decided-by-the-reader-and-not-by-a-regex) (mcp_alias_migration_test.clj:5206)
   every constructor shape that takes the kind first enables the scan (def refusal (fn …))
   the planted dynamic site was not reported for a constructor shaped `(def refusal (fn …))` — one ordinary edit to the constructor switched the whole file's scan off: []
   expected: (= [(str "plant:" (planted-row text))] (dynamic-refusal-kind-sites-in "plant" text))
     actual: (not (= ["plant:8"] []))

   FAIL in (the-refusal-constructor-is-decided-by-the-reader-and-not-by-a-regex) (mcp_alias_migration_test.clj:5206)
   every constructor shape that takes the kind first enables the scan with an attribute map
   the planted dynamic site was not reported for a constructor shaped `with an attribute map` — one ordinary edit to the constructor switched the whole file's scan off: []
   expected: (= [(str "plant:" (planted-row text))] (dynamic-refusal-kind-sites-in "plant" text))
     actual: (not (= ["plant:8"] []))

   FAIL in (the-refusal-constructor-is-decided-by-the-reader-and-not-by-a-regex) (mcp_alias_migration_test.clj:5206)
   every constructor shape that takes the kind first enables the scan a first parameter named `k`
   the planted dynamic site was not reported for a constructor shaped `a first parameter named `k`` — one ordinary edit to the constructor switched the whole file's scan off: []
   expected: (= [(str "plant:" (planted-row text))] (dynamic-refusal-kind-sites-in "plant" text))
     actual: (not (= ["plant:7"] []))
   RESULT= {:test 1, :pass 2, :fail 5, :error 0}
   EXIT=0
   ```

   Exact sab2 command:

   ```text
   cd /var/tmp/forge/q5z18-review-fx/sab2
   ~/bin/suite-run bash -lc 'cp=$(clojure -Spath -M:clj-surgeon/mcp-test); exec java -cp "$cp" clojure.main /var/tmp/forge/q5z18-review-fx/run_var.clj the-forwarded-kind-check-is-form-deep-and-not-merely-head-shaped'; rc=$?; echo EXIT=$rc
   ```

   Verbatim output:

   ```text
   Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge
   Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge
   VAR= the-forwarded-kind-check-is-form-deep-and-not-merely-head-shaped

   FAIL in (the-forwarded-kind-check-is-form-deep-and-not-merely-head-shaped) (mcp_alias_migration_test.clj:5328)
   a STRING literal is a kind source exactly as a keyword literal is get against a literal STRING table
   a STRING kind minted inside a literal table under the marker never reached the enumeration: #{}
   expected: (contains? (structural-error-type-kinds text) kind)
     actual: (not (contains? #{} "brand-new-kind"))

   FAIL in (the-forwarded-kind-check-is-form-deep-and-not-merely-head-shaped) (mcp_alias_migration_test.clj:5328)
   a STRING literal is a kind source exactly as a keyword literal is get-in against a nested literal STRING table
   a STRING kind minted inside a literal table under the marker never reached the enumeration: #{}
   expected: (contains? (structural-error-type-kinds text) kind)
     actual: (not (contains? #{} "table-minted"))

   FAIL in (the-forwarded-kind-check-is-form-deep-and-not-merely-head-shaped) (mcp_alias_migration_test.clj:5340)
   a STRING literal bound above the site and relayed is a mint
   a bare symbol bound to a STRING literal one line above was read as a forward: []
   expected: (= 1 (count (runtime-spelled-kind-sites "plant" text)))
     actual: (not (= 1 0))
   RESULT= {:test 1, :pass 9, :fail 3, :error 0}
   EXIT=0
   ```

   Exact sab3 command:

   ```text
   cd /var/tmp/forge/q5z18-review-fx/sab3
   ~/bin/suite-run bash -lc 'cp=$(clojure -Spath -M:clj-surgeon/mcp-test); exec java -cp "$cp" clojure.main /var/tmp/forge/q5z18-review-fx/run_var.clj the-forwarded-kind-check-is-form-deep-and-not-merely-head-shaped'; rc=$?; echo EXIT=$rc
   ```

   Verbatim output:

   ```text
   Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge
   Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge
   VAR= the-forwarded-kind-check-is-form-deep-and-not-merely-head-shaped

   FAIL in (the-forwarded-kind-check-is-form-deep-and-not-merely-head-shaped) (mcp_alias_migration_test.clj:5360)
   a kind a marked (refusal …) site can spell reaches the enumeration
   a kind a marked (refusal …) site can hand back reached neither the guard nor the enumeration: ()
   expected: (contains? (set (literal-refusal-kinds-in text)) "brand-new-kind")
     actual: (not (contains? #{} "brand-new-kind"))
   RESULT= {:test 1, :pass 11, :fail 1, :error 0}
   EXIT=0
   ```


8. **CLOSED — plantD is named; plantI is enumerated at 141, which is the correct fail-closed disposition for a closed literal table.** `test/clj_surgeon/mcp_alias_migration_test.clj:1291` now treats strings exactly like keywords as literal kind sources; `test/clj_surgeon/mcp_alias_migration_test.clj:1588` feeds every literal a `(refusal …)` kind expression can return into the enumeration. On the docstring plant, the reader-based constructor detector keeps the guard enabled and names line 118. On the string-table plant, both the table key and value are conservatively enumerated, moving the planted tree from 139 to 141; the actual returned value, `brand-new-kind`, is present. Enumerating is complete here because the literal table is closed in source. Counting its key too is an over-approximation, not an omission. The unmodified real tree remains governed by the frozen 139-member two-direction pin at `test/clj_surgeon/mcp_alias_migration_test.clj:5546`.

   Exact plantD command:

   ```text
   cd /var/tmp/forge/q5z18-review-fx/plantD
   ~/bin/suite-run bash -lc 'cp=$(clojure -Spath -M:clj-surgeon/mcp-test); exec java -cp "$cp" clojure.main /var/tmp/forge/q5z18-review-fx/plant_probe.clj'; rc=$?; echo EXIT=$rc
   ```

   Verbatim output:

   ```text
   Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge
   enumerated= 139
   unscannable-sites= ["src/clj_surgeon/mcp_alias_migration.clj:118"]
   dynamic-sites= ["src/clj_surgeon/mcp_alias_migration.clj:118"]
   live-kind= "planted-runtime-kind"
   live-kind-enumerated= false
   EXIT=0
   ```

   Exact plantI command:

   ```text
   cd /var/tmp/forge/q5z18-review-fx/plantI
   ~/bin/suite-run bash -lc 'cp=$(clojure -Spath -M:clj-surgeon/mcp-test); exec java -cp "$cp" clojure.main /var/tmp/forge/q5z18-review-fx/plant_probe.clj'; rc=$?; echo EXIT=$rc
   ```

   Verbatim output:

   ```text
   Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge
   enumerated= 141
   unscannable-sites= []
   dynamic-sites= []
   live-kind= "brand-new-kind"
   live-kind-enumerated= true
   EXIT=0
   ```


9. **BLOCKING corroboration — plantS reproduces finding 1 end to end.** The fixture changes the reachable verb's constructor from `[error-type message extra]` to `[message error-type extra]`, puts the caller-derived kind in that second parameter, and invokes `execute!`. The scanner finds the call form but reports no dynamic or unscannable site; the live kind is absent from the still-139-member enumeration. This is exactly the brief's blocking condition: a `(refusal …)` site the scan cannot see and a live refusal kind that stays unenumerated.

   Exact command:

   ```text
   ~/bin/suite-run bash -lc 'cp=$(clojure -Spath -M:clj-surgeon/mcp-test); exec java -cp "$cp" clojure.main /var/tmp/forge/q5z18-review-fx/plant_probe.clj'; rc=$?; echo EXIT=$rc
   ```

   Verbatim output:

   ```text
   Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge
   enumerated= 139
   unscannable-sites= []
   dynamic-sites= []
   live-kind= "planted-runtime-kind"
   live-kind-enumerated= false
   EXIT=0
   ```

10. **HYGIENE — reviewed HEAD is proved, the clone is clean, all fixtures are removed, and no allowed review port remains listening.** Fixture cleanup used the host trash rather than an irreversible delete; the original path is absent and the material is recoverable from the forge user's trash if needed.

    Exact command:

    ```text
    gio trash /var/tmp/forge/q5z18-review-fx; test ! -e /var/tmp/forge/q5z18-review-fx; echo FIXTURE_REMOVED_EXIT=$?; ss -ltn | rg ':816[2-4]\b' || true; ps -eo args | rg '[c]lojure .*:port 816[2-4]\b' || true; git rev-parse HEAD; git status --porcelain; echo STATUS_EXIT=$?
    ```

    Verbatim output:

    ```text
    FIXTURE_REMOVED_EXIT=0
    1f92608efcdbc14ce5a3202653294a4293d2d65d
    STATUS_EXIT=0
    ```

## NO-GO

This tip is not GO on its own for MCP/main because the second-parameter refusal constructor leaves a live kind unenumerated, although `git merge-tree --write-tree HEAD origin/MCP/main` is mechanically clean against tested trunk `a44ded9277607512b2af6f420bfc3f47b31b81b3`.
