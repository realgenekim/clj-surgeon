## GO-WITH-FIX

1. **GO-WITH-FIX — the scalar allowlist still permits caller-controlled `toString` through an arbitrary `java.lang.Number`.** `src/clj_surgeon/mcp_tool.clj:1310-1324,1390-1392` treats every value satisfying `number?` as a safe leaf and hands it to `print-method`. A proxy subclass of `Number` is a number, but its `toString` remains arbitrary: a throwing implementation escapes `bounded-pr-str`, and a looping implementation does not return within the required two-second guard. This falsifies the implementation comment that the admitted leaves cannot invoke caller-supplied `toString`, although no current JSON request or live refusal field constructs such a JVM value. Whitelist the concrete numeric representations the program emits, or render an unknown `Number` subclass opaquely.

   Exact command:

   ```text
   $ timeout 15s ~/bin/suite-run bash -lc 'cp=$(clojure -Spath -M:clj-surgeon/mcp-test); exec java -cp "$cp" clojure.main /var/tmp/forge/q5z15-review-fx/hostile_number_probe.clj'; rc=$?; echo EXIT=$rc
   ```

   Verbatim output:

   ```text
   Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge
   throwing-number-number?= true
   throwing-number-result= {:status :threw, :class clojure.lang.ExceptionInfo, :message number toString exploded}
   looping-number-number?= true
   looping-number-result= TIMED-OUT
   EXIT=1
   ```

2. **GO-WITH-FIX — `forwarded-refusal-kind` is now an unchecked comment capability, so the marker itself is the enumeration hole.** `test/clj_surgeon/mcp_alias_migration_test.clj:1239-1250,1287-1312` exempts any dynamic site when the marker text occurs in the preceding twelve lines; it does not establish that the value is actually forwarded. I planted a reachable helper in the router whose marked expression was `:error_type (keyword (:review_dynamic_kind params))`, routed it from `handle-alias-migration`, and drove `marked-runtime-kind`. The source enumeration remained 139, `unscannable-sites` remained empty, the live kind was absent, and all four advertised source/enumeration witnesses stayed green over 20 assertions. This is a future-regression hole, not a missing kind in the unmodified tip. A marker must be coupled to a mechanically checked forwarding shape (for example, the exact parameter or `(name parameter)`), or the live enumeration must independently fail on the constructed kind.

   Exact command:

   ```text
   $ ~/bin/suite-run bash -lc 'cp=$(clojure -Spath -M:clj-surgeon/mcp-test); exec java -cp "$cp" clojure.main /var/tmp/forge/q5z15-review-fx/marker_probe.clj'; probe_rc=$?; echo PROBE_EXIT=$probe_rc; ~/bin/suite-run bash -lc 'cp=$(clojure -Spath -M:clj-surgeon/mcp-test); exec java -cp "$cp" clojure.main /var/tmp/forge/q5z15-review-fx/test_vars.clj no-reachable-namespace-spells-a-refusal-kind-dynamically the-source-guard-exempts-only-the-forwarded-refusal-kind-marker the-refusal-enumeration-contains-every-kind-the-entrance-constructs the-enumeration-reaches-the-routers-entrance-slice-and-every-spelling'; test_rc=$?; echo TEST_EXIT=$test_rc
   ```

   Verbatim output:

   ```text
   Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge
   enumerated= 139
   unscannable-sites= []
   live-kind= marked-runtime-kind
   live-kind-enumerated= false
   PROBE_EXIT=0
   Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge
   {:test 4, :pass 20, :fail 0, :error 0}
   TEST_EXIT=0
   ```

3. **The requested §1 collection attacks otherwise close, and the ordinary-receipt corpus is byte-identical; there is a narrower compatibility drift to document or restore.** `src/clj_surgeon/mcp_tool.clj:1394-1418` correctly treats a record and a custom `IPersistentMap` as maps and renders their fields; poisonous lazy-seq elements and map keys become identity markers; a Java `HashMap` is opaque; and a ten-megabyte string nested in a map stops at 160 characters plus the ellipsis. Default metadata behavior is unchanged, and the ordinary corpus diff is empty. However, compared with the RED parent, a record loses its `#namespace.Record` tag and collection metadata disappears when `*print-meta*` is true; symbol metadata still renders. Thus “nothing that fitted before renders differently” is true for ordinary receipts, not universally.

   Exact commands:

   ```text
   $ timeout 20s ~/bin/suite-run bash -lc 'cp=$(clojure -Spath -M:clj-surgeon/mcp-test); exec java -cp "$cp" clojure.main /var/tmp/forge/q5z15-review-fx/renderer_probe.clj'; rc=$?; echo EXIT=$rc
   $ ~/bin/suite-run bash -lc 'diff -u <(cd /var/tmp/forge/q5z15-review-fx/red1 && cp=$(clojure -Spath -M:clj-surgeon/mcp-test) && java -cp "$cp" clojure.main /var/tmp/forge/q5z15-review-fx/ordinary_corpus.clj) <(cd /home/forge/tmp/sol/q5z14-wt && cp=$(clojure -Spath -M:clj-surgeon/mcp-test) && java -cp "$cp" clojure.main /var/tmp/forge/q5z15-review-fx/ordinary_corpus.clj)'; rc=$?; echo DIFF_EXIT=$rc
   $ ~/bin/suite-run bash -lc 'for tree in /var/tmp/forge/q5z15-review-fx/red1 /home/forge/tmp/sol/q5z14-wt; do printf "TREE=%s\n" "$tree"; cd "$tree"; cp=$(clojure -Spath -M:clj-surgeon/mcp-test); java -cp "$cp" clojure.main /var/tmp/forge/q5z15-review-fx/compat_probe.clj; done'; rc=$?; echo EXIT=$rc
   ```

   Verbatim outputs:

   ```text
   Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge
   looping-object= {:status :returned, :value "#object[LoopingToString HASH]"}
   throwing-object= {:status :returned, :value "#object[ThrowingToString HASH]"}
   java-map= {:status :returned, :value "#object[HashMap HASH]"}
   record-map?= true
   record= {:status :returned, :value "{:safe 7, :poison #object[ThrowingToString HASH]"}
   lazy-poison-elements= {:status :returned, :value "(#object[LoopingToString HASH] #object[ThrowingToString HASH])"}
   poison-map-key= {:status :returned, :value "{#object[LoopingToString HASH] :value}"}
   deftype-ipersistent-map-map?= true
   deftype-ipersistent-map= {:status :returned, :value "{:safe 1, :poison #object[ThrowingToString HASH]"}
   metadata-default= {:a 1}
   metadata-print-meta= {:a 1}
   ten-mb-map-status= :returned
   ten-mb-map-result-chars= 161
   ten-mb-map-elided= true
   hostile-number-number?= true
   hostile-number= {:status :threw, :class "clojure.lang.ExceptionInfo", :message "number toString exploded"}
   EXIT=0
   ```

   ```text
   Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge
   Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge
   Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge
   DIFF_EXIT=0
   ```

   ```text
   TREE=/var/tmp/forge/q5z15-review-fx/red1
   Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge
   record= #compat_probe.SafeRecord{:a 1, :b 2}
   meta-map= ^{:receipt true} {:a 1}
   meta-vector= ^{:receipt true} [1 2]
   meta-list= ^{:receipt true} (1 2)
   meta-symbol= ^{:receipt true} alpha
   TREE=/home/forge/tmp/sol/q5z14-wt
   Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge
   record= {:a 1, :b 2}
   meta-map= {:a 1}
   meta-vector= [1 2]
   meta-list= (1 2)
   meta-symbol= ^{:receipt true} alpha
   EXIT=0
   ```

4. **Both claimed RED→GREEN pairs reproduce.** `test/clj_surgeon/mcp_alias_migration_test.clj:5076-5146,4539-4584` fails at each RED commit and passes at its following fix. The first RED reports the looping timeout, both throwing failures, and the exceptional Java collection; GREEN is 11/0. The second RED swallows both parameter-derived mints; GREEN is 3/0.

   Exact commands:

   ```text
   $ for tree in red1 green1; do echo TREE=$tree; (cd "/var/tmp/forge/q5z15-review-fx/$tree" && timeout 15s ~/bin/suite-run bash -lc 'cp=$(clojure -Spath -M:clj-surgeon/mcp-test); exec java -cp "$cp" clojure.main /var/tmp/forge/q5z15-review-fx/test_vars.clj the-fact-renderer-never-invokes-an-arbitrary-toString'); rc=$?; echo EXIT=$rc; done
   $ for tree in /var/tmp/forge/q5z15-review-fx/red2 /home/forge/tmp/sol/q5z14-wt; do echo TREE=$tree; (cd "$tree" && ~/bin/suite-run bash -lc 'cp=$(clojure -Spath -M:clj-surgeon/mcp-test); exec java -cp "$cp" clojure.main /var/tmp/forge/q5z15-review-fx/test_vars.clj the-source-guard-exempts-only-the-forwarded-refusal-kind-marker'); rc=$?; echo EXIT=$rc; done
   ```

   Verbatim output for RED `bd006dfd` → GREEN `14731ea1`:

   ```text
   TREE=red1
   Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge

   FAIL in (the-fact-renderer-never-invokes-an-arbitrary-toString) (mcp_alias_migration_test.clj:5073)
   a toString that never returns does not hang the renderer
   bounded-pr-str hung inside a toString that never returns
   expected: (not= :clj-surgeon.mcp-alias-migration-test/timed-out result)
     actual: (not (not= :clj-surgeon.mcp-alias-migration-test/timed-out :clj-surgeon.mcp-alias-migration-test/timed-out))

   FAIL in (the-fact-renderer-never-invokes-an-arbitrary-toString) (mcp_alias_migration_test.clj:5083)
   a toString that throws does not escape the renderer
   bounded-pr-str propagated the object's own toString exception: <threw ExceptionInfo>
   expected: (not (str/starts-with? result "<threw"))
     actual: (not (not true))

   FAIL in (the-fact-renderer-never-invokes-an-arbitrary-toString) (mcp_alias_migration_test.clj:5086)
   a toString that throws does not escape the renderer
   an opaque value renders no identity at all: <threw ExceptionInfo>
   expected: (str/includes? result "ThrowingToStringProbe")
     actual: (not (str/includes? "<threw ExceptionInfo>" "ThrowingToStringProbe"))

   ERROR in (the-fact-renderer-never-invokes-an-arbitrary-toString) (FutureTask.java:122)
   Uncaught exception, not in assertion.
   expected: nil
     actual: java.util.concurrent.ExecutionException: clojure.lang.ExceptionInfo: toString exploded {}
    at java.util.concurrent.FutureTask.report (FutureTask.java:122)
       java.util.concurrent.FutureTask.get (FutureTask.java:205)
       clojure.core$deref_future.invokeStatic (core.clj:2319)
       clojure.core$future_call$reify__8578.deref (core.clj:7121)
       clojure.core$deref.invokeStatic (core.clj:2341)
       clojure.core$deref.invoke (core.clj:2323)
       clj_surgeon.mcp_alias_migration_test$fn__34248$fn__34269.invoke (mcp_alias_migration_test.clj:5092)
       clj_surgeon.mcp_alias_migration_test$fn__34248.invokeStatic (mcp_alias_migration_test.clj:5088)
       clj_surgeon.mcp_alias_migration_test/fn (mcp_alias_migration_test.clj:5055)
       clojure.test$test_var$fn__9898.invoke (test.clj:717)
       clojure.test$test_var.invokeStatic (test.clj:717)
       clojure.test$test_var.invoke (test.clj:708)
       clojure.test$test_vars$fn__9924$fn__9929.invoke (test.clj:735)
       clojure.test$default_fixture.invokeStatic (test.clj:687)
       clojure.test$default_fixture.invoke (test.clj:683)
       clojure.test$test_vars$fn__9924.invoke (test.clj:735)
       clojure.test$default_fixture.invokeStatic (test.clj:687)
       clojure.test$default_fixture.invoke (test.clj:683)
       clojure.test$test_vars.invokeStatic (test.clj:731)
       clojure.test$test_vars.invoke (test.clj:723)
       test_vars$eval34406$fn__34409.invoke (test_vars.clj:10)
       test_vars$eval34406.invokeStatic (test_vars.clj:9)
       test_vars$eval34406.invoke (test_vars.clj:5)
       clojure.lang.Compiler.eval (Compiler.java:7739)
       clojure.lang.Compiler.load (Compiler.java:8211)
       clojure.lang.Compiler.loadFile (Compiler.java:8149)
       clojure.main$load_script.invokeStatic (main.clj:476)
       clojure.main$script_opt.invokeStatic (main.clj:536)
       clojure.main$script_opt.invoke (main.clj:531)
       clojure.main$main.invokeStatic (main.clj:665)
       clojure.main$main.doInvoke (main.clj:617)
       clojure.lang.RestFn.applyTo (RestFn.java:140)
       clojure.lang.Var.applyTo (Var.java:707)
       clojure.main.main (main.java:40)
   Caused by: clojure.lang.ExceptionInfo: toString exploded
   {}
    at clj_surgeon.mcp_alias_migration_test.ThrowingToStringProbe.toString (mcp_alias_migration_test.clj:5052)
       clojure.core$str.invokeStatic (core.clj:555)
       clojure.core$print_object.invokeStatic (core_print.clj:117)
       clojure.core$fn__7384.invokeStatic (core_print.clj:120)
       clojure.core/fn (core_print.clj:120)
       clojure.lang.MultiFn.invoke (MultiFn.java:234)
       clojure.core$pr_on.invokeStatic (core.clj:3700)
       clojure.core$pr_on.invoke (core.clj:3694)
       clojure.core$print_sequential.invokeStatic (core_print.clj:61)
       clojure.core$fn__7483.invokeStatic (core_print.clj:295)
       clojure.core/fn (core_print.clj:294)
       clojure.lang.MultiFn.invoke (MultiFn.java:234)
       clj_surgeon.mcp_tool$bounded_pr_str$fn__17701.invoke (mcp_tool.clj:1335)
       clj_surgeon.mcp_tool$bounded_pr_str.invokeStatic (mcp_tool.clj:1331)
       clj_surgeon.mcp_tool$bounded_pr_str.invoke (mcp_tool.clj:1310)
       clj_surgeon.mcp_alias_migration_test$fn__34248$fn__34269$fn__34271.invoke (mcp_alias_migration_test.clj:5091)
       clojure.core$binding_conveyor_fn$fn__5844.invoke (core.clj:2047)
       clojure.lang.AFn.call (AFn.java:18)
       java.util.concurrent.FutureTask.run (FutureTask.java:317)
       java.util.concurrent.ThreadPoolExecutor.runWorker (ThreadPoolExecutor.java:1144)
       java.util.concurrent.ThreadPoolExecutor$Worker.run (ThreadPoolExecutor.java:642)
       java.lang.Thread.run (Thread.java:1583)
   {:test 1, :pass 0, :fail 3, :error 1}
   EXIT=1
   TREE=green1
   Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge
   Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge
   {:test 1, :pass 11, :fail 0, :error 0}
   EXIT=0
   ```

   Verbatim output for RED `8290aeea` → GREEN `b6d1d17b`:

   ```text
   TREE=/var/tmp/forge/q5z15-review-fx/red2
   Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge

   FAIL in (the-source-guard-exempts-only-the-forwarded-refusal-kind-marker) (mcp_alias_migration_test.clj:4586)
   (keyword (name kind)) mentions the enclosing parameter
   a kind derived from an enclosing parameter with no forwarded-refusal-kind marker was not named: []
   expected: (= 1 (count (runtime-spelled-kind-sites label text)))
     actual: (not (= 1 0))

   FAIL in (the-source-guard-exempts-only-the-forwarded-refusal-kind-marker) (mcp_alias_migration_test.clj:4586)
   (keyword "alias-migration-" (name x)) mentions the enclosing parameter
   a kind derived from an enclosing parameter with no forwarded-refusal-kind marker was not named: []
   expected: (= 1 (count (runtime-spelled-kind-sites label text)))
     actual: (not (= 1 0))
   {:test 1, :pass 1, :fail 2, :error 0}
   EXIT=1
   TREE=/home/forge/tmp/sol/q5z14-wt
   Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge
   {:test 1, :pass 3, :fail 0, :error 0}
   EXIT=0
   ```

5. **The unmodified §2 enumeration remains 139 before and after, with the three named kinds present; the nine real forwarding sites are marked.** `test/clj_surgeon/mcp_alias_migration_test.clj:1192-1320` now includes `keyword` in the fragment-composing set and has no `parameters-of` or `constructor-site?`. The five constructor sites are marked in `src/clj_surgeon/alias_migration.clj:614`, `mcp_alias_migration.clj:78`, `mcp_paths.clj:47`, `binding_rename.clj:16`, and `intent_transaction.clj:35`; the four throws are marked in `structural_lens.clj:48,55,63,67`.

   Exact command:

   ```text
   $ ~/bin/suite-run bash -lc 'for tree in /var/tmp/forge/q5z15-review-fx/red2 /home/forge/tmp/sol/q5z14-wt; do printf "TREE=%s\n" "$tree"; cd "$tree"; cp=$(clojure -Spath -M:clj-surgeon/mcp-test); java -cp "$cp" clojure.main /var/tmp/forge/q5z15-review-fx/enumeration_probe.clj; done'; rc=$?; echo EXIT=$rc
   ```

   Verbatim output:

   ```text
   TREE=/var/tmp/forge/q5z15-review-fx/red2
   Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge
   Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge
   enumerated= 139
   no-match= true
   ambiguous-match= true
   diagnostic-output-truncated= true
   unscannable-sites= []
   TREE=/home/forge/tmp/sol/q5z14-wt
   Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge
   enumerated= 139
   no-match= true
   ambiguous-match= true
   diagnostic-output-truncated= true
   unscannable-sites= []
   EXIT=0
   ```

6. **FAN run 1 passes the fetched fail-closed scorer 6/6 and is byte-identical after exactly one live call.** `bench/fanout/rescore-FAN.sh:29-128` was taken from fetched tip `f2fa8be9bdaf3b39580aab0055265d323aa61339`. The server was started from `b6d1d17b` with explicit `:port 8120`, bound to the generated seed-7/K-6 run tree. The only live tool call committed 21 files / 63 sites / 30 collisions. The server was then stopped; the final `ss` check for 8120–8122 was empty.

   Exact fetch command and output:

   ```text
   $ git fetch origin bridge/fanout-fixtures-in-git && git rev-parse FETCH_HEAD && git log -1 --oneline FETCH_HEAD
   From https://github.com/realgenekim/clj-surgeon
    * branch              bridge/fanout-fixtures-in-git -> FETCH_HEAD
   f2fa8be9bdaf3b39580aab0055265d323aa61339
   f2fa8be9 docs: document the walk's non-UTF-8 filename limit (finding 9, non-blocking)
   ```

   Exact server command:

   ```text
   $ clojure -X:clj-surgeon/mcp :project-dir '"/var/tmp/forge/q5z15-review-fx/fan/run1"' :port 8120 :telemetry :off :ready-file '"/var/tmp/forge/q5z15-review-fx/server-ready.edn"' :nrepl-port :none
   ```

   Verbatim readiness evidence:

   ```text
   {"ok":true,"server":"clj-surgeon","tool_runtime":"ready","tool_registry":"ready"}
   {:transport :streamable-http, :project-root "/var/tmp/forge/q5z15-review-fx/fan/run1", :server :clj-surgeon, :port 8120, :host "127.0.0.1", :pid 4009879, :ok true, :verification-profile-source :built-in, :url "http://127.0.0.1:8120/mcp"}
   State  Recv-Q Send-Q      Local Address:Port Peer Address:PortProcess
   LISTEN 0      50     [::ffff:127.0.0.1]:8120            *:*    users:(("java",pid=4009879,fd=78))
   ```

   Exact live-call command and output:

   ```text
   $ /var/tmp/forge/q5z15-review-fx/mcp-call.sh 8120 alias_migration /var/tmp/forge/q5z15-review-fx/fan-request.json; rc=$?; echo; echo EXIT=$rc
   SESSION=7f26e948-3687-4af9-9d34-301cf59ff478
   id: 7f26e948-3687-4af9-9d34-301cf59ff478
   event: message
   data: {"jsonrpc":"2.0","id":2,"result":{"content":[{"type":"text","text":"alias_migration\n  21 files · 63 sites · aliases {\"es\" 5, \"st2\" 5, \"store-2\" 5, \"store2\" 6} · 30 collisions resolved · 741.96 ms\n\n✓ atomic commit complete\n✓ written bytes read back and verified\n✓ terminal evidence · per-file detail at .clj-surgeon/alias-migration/detail-62cc3a2d-a9ba-4eb8-bd17-eb84670d557d.edn (best-effort retention)"}],"isError":false,"structuredContent":{"details_retained":20,"workspace_root":"/var/tmp/forge/q5z15-review-fx/fan/run1","committed":true,"kondo_delta":{"status":"not-requested"},"alias_histogram":{"es":5,"st2":5,"store-2":5,"store2":6},"sites":63,"string_mention_sites_shown":0,"details_retention":"best-effort","focused_test":{"status":"not-requested"},"string_mentions":0,"string_mention_sites":[],"lib_renamed":null,"elapsed_ms":741.960487,"next_action":"none","refer_sites":0,"files":21,"collisions_resolved":30,"receipt_hash":"d68852011ad65fc0b0c13f33c18b882ba1dd73f28c1900268218e0f5a1525a17","ok":true,"operation":"alias_migration","undo_receipt":"/home/forge/.local/state/clj-surgeon/workspaces/e2b76e2b6e4201c04cbef7b9e91261a4ea5ac59c450cfe5962d1af7e3a8723ce/receipts/32fae513-aab5-4548-a6e7-e1e812e466c9.edn","details_path":".clj-surgeon/alias-migration/detail-62cc3a2d-a9ba-4eb8-bd17-eb84670d557d.edn"}}}


   EXIT=0
   ```

   Exact scorer and byte-diff command:

   ```text
   $ FAN_BASE=662d4f4333820713cffa8173b0c39977efdeb401 ~/bin/suite-run /var/tmp/forge/q5z15-review-fx/fan-tools/bench/fanout/rescore-FAN.sh /var/tmp/forge/q5z15-review-fx/fan/run1 21 /var/tmp/forge/q5z15-review-fx/fan/generated; score_rc=$?; echo SCORE_EXIT=$score_rc; diff -r --exclude=.git --exclude=.clj-surgeon --exclude=.cpcache --exclude=target /var/tmp/forge/q5z15-review-fx/fan/run1 /var/tmp/forge/q5z15-review-fx/fan/generated/canonical-21; diff_rc=$?; echo BYTE_DIFF_EXIT=$diff_rc; ss -ltnp 'sport = :8120'
   ```

   Verbatim output:

   ```text
   rescore-FAN: worktree=/var/tmp/forge/q5z15-review-fx/fan/run1 n=21 base=662d4f4333820713cffa8173b0c39977efdeb401 base-from=FAN_BASE fixtures=/var/tmp/forge/q5z15-review-fx/fan/generated
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
   State Recv-Q Send-Q Local Address:Port Peer Address:PortProcess
   ```

7. **All four required gates pass at the requested counts.** `test/run_all.clj`, the direct `:clj-surgeon/mcp-test` alias, `test/mcp_operation_contract_oracle.pl`, and `test/repository_hygiene_gate.sh` all exited zero. The long namespace-by-namespace progress output was observed; the terminal gate output follows verbatim.

   Exact commands and verbatim terminal outputs:

   ```text
   $ ~/bin/suite-run bb test/run_all.clj; rc=$?; echo EXIT=$rc
   Ran 737 tests containing 6275 assertions.
   0 failures, 0 errors.
   EXIT=0
   ```

   ```text
   $ ~/bin/suite-run clojure -M:clj-surgeon/mcp-test; rc=$?; echo EXIT=$rc
   Ran 500 tests containing 7022 assertions.
   0 failures, 0 errors.
   EXIT=0
   ```

   ```text
   $ make mcp-operation-oracle; rc=$?; echo EXIT=$rc
   # @spec MCP-OP-ORACLE-001
   swipl -q -f test/mcp_operation_contract_oracle.pl
   mcp-operation oracle: pass; legacy counterexamples=[verification_failed,verification_pending]
   EXIT=0
   ```

   ```text
   $ make repository-hygiene; rc=$?; echo EXIT=$rc
   # @spec MCP-OP-ALIAS-036
   # @spec MCP-OP-ALIAS-053
   repository hygiene: no machine-local build cache is tracked at any depth
   EXIT=0
   ```

8. **HEAD, history, cleanliness, cleanup, and mergeability are proved.** The checkout remained exactly `b6d1d17b363ca17878ae7d03dd0f1cd3b0043c6b`; `git status --short` stayed empty. The four-commit history is the two RED→GREEN pairs reviewed above. The fixture directory was removed, ports 8120–8122 had no listeners, and `git merge-tree --write-tree HEAD origin/MCP/main` exited zero with a tree ID and no conflict records against fetched `origin/MCP/main` `89e8dadc`.

   Exact commands and verbatim outputs:

   ```text
   $ git rev-parse --verify HEAD && git status --short && git log --oneline 1cc5990b..b6d1d17b
   b6d1d17b363ca17878ae7d03dd0f1cd3b0043c6b
   b6d1d17b q5z-r15-2: the dynamic-kind guard exempts only sites marked forwarded-refusal-kind, dropping the parameter-contains heuristic (ALIAS-059)
   8290aeea q5z-r15-red-2: the source guard's constructor-site? heuristic exempts any dynamic :error-type expression that merely mentions an enclosing parameter (ALIAS-059)
   14731ea1 q5z-r15-1: bounded-pr-str admits only Clojure data before printing, so an arbitrary object's toString is never invoked (ALIAS-059)
   bd006dfd q5z-r15-red-1: bounded-pr-str calls toString on an arbitrary object before any character reaches the ceiling writer (ALIAS-059)
   ```

   ```text
   $ git fetch origin MCP/main && git rev-parse origin/MCP/main && git merge-tree --write-tree HEAD origin/MCP/main; rc=$?; echo EXIT=$rc
   From https://github.com/realgenekim/clj-surgeon
    * branch              MCP/main   -> FETCH_HEAD
      a8a80796..89e8dadc  MCP/main   -> origin/MCP/main
   89e8dadc80261c4c4ebd73aa8f607cb93ca19606
   833490fab50d411d22bd1448cf7163541f4b8b1e
   EXIT=0
   ```

   ```text
   $ git rev-parse --verify HEAD; echo STATUS_BEGIN; git status --short; echo STATUS_END; echo PORTS_BEGIN; (ss -ltnp 2>/dev/null | rg ':812[0-2]\b' || true); echo PORTS_END
   b6d1d17b363ca17878ae7d03dd0f1cd3b0043c6b
   STATUS_BEGIN
   STATUS_END
   PORTS_BEGIN
   PORTS_END
   ```

   ```text
   $ find /var/tmp/forge/q5z15-review-fx -depth -delete && if test -e /var/tmp/forge/q5z15-review-fx; then echo FIXTURE_REMAINS; exit 1; else echo FIXTURE_REMOVED; fi
   FIXTURE_REMOVED
   ```

## GO-WITH-FIX

No: the tip merges conflict-free with `origin/MCP/main` (which carries the admit gate), but it is not GO on its own until the hostile-`Number` and false-marker holes are closed; the collection-metadata/record-tag drift should also be restored or explicitly narrowed in the compatibility claim.
