## NO-GO

Independent adversarial review of `2ecce8c44b7e5f1cc36078d3da262f68a59ac85b` (`bridge/suite-spike`), completed 2026-09-04.

1. **The review oracle is absent from the branch tip but recoverable from the working trunk (non-blocking unless hygiene requires the branch to carry it).** The two round reports refer to `docs/observations/2026-09-04-suite-spike-spec.md`, but the path is absent from the detached tree. A fetch (no checkout) resolved current `origin/MCP/main` to `96272b9d11170f74c66d502fa4cf1a7341c196d5`, where the spec exists. The spec says Gene's round-two ruling is TEST-ISO-001, TEST-ISO-006, and TEST-ISO-009; 002–005, 007, 010 are explicitly round three. I use that trunk blob as the canonical oracle below.

   Exact command:

   ```text
   wc -l docs/observations/2026-09-04-suite-spike-spec.md docs/observations/2026-09-04-suite-spike-round1.md docs/observations/2026-09-04-suite-spike-round2.md
   ```

   Verbatim output:

   ```text
   wc: docs/observations/2026-09-04-suite-spike-spec.md: No such file or directory
     282 docs/observations/2026-09-04-suite-spike-round1.md
     347 docs/observations/2026-09-04-suite-spike-round2.md
     629 total
   ```

   Exact command:

   ```text
   rg --files docs/observations | rg 'suite-spike|test-iso|suite.*spec'
   git status --short --branch
   git rev-parse HEAD
   ```

   Verbatim output:

   ```text
   docs/observations/2026-09-04-suite-spike-round1-classification.md
   docs/observations/2026-09-04-suite-spike-round1-timing.md
   docs/observations/2026-09-04-suite-spike-round1-timing.edn
   docs/observations/2026-09-04-suite-spike-round2.md
   docs/observations/2026-09-04-suite-spike-round1.md
   ## HEAD (no branch)
   2ecce8c44b7e5f1cc36078d3da262f68a59ac85b
   ```

   Exact command:

   ```text
   git fetch origin MCP/main bridge/suite-spike
   git rev-parse origin/MCP/main
   git rev-parse origin/bridge/suite-spike
   git show origin/MCP/main:docs/observations/2026-09-04-suite-spike-spec.md
   ```

   Verbatim identifying output (the complete spec was read):

   ```text
   From /home/forge/src/clj-surgeon
    * branch              MCP/main   -> FETCH_HEAD
    * branch              bridge/suite-spike -> FETCH_HEAD
      69c859c6..96272b9d  MCP/main   -> origin/MCP/main
   96272b9d11170f74c66d502fa4cf1a7341c196d5
   2ecce8c44b7e5f1cc36078d3da262f68a59ac85b
   # Spike: the JVM test suite — speed, purity, isolation (filed 2026-09-04, Gene)
   ...
   Round two builds TEST-ISO-001 (lane declaration + registry audit), TEST-ISO-006 (fast-lane JVM started on a throwaway
   home and tmpdir), and TEST-ISO-009 (the concurrency battery: N clones of the fast lane at once, 0 failures, three times)
   on the partition round one proposes.
   ```

2. **Baseline fast lane reproduces exactly (provisional GREEN).** A real no-local clone was fetched and detached at the reviewed SHA. Immediately before the suite the 1-minute load was 9.49, so no wait was required. The runner printed 36 namespaces and `home-isolated true`; its count exactly matches the claim: 358 tests / 3,569 assertions / 0 failures / 0 errors. Reviewer wall was 30.91 s.

   Exact setup command:

   ```text
   git clone --no-local /home/forge/tmp/sol/suite-wt /var/tmp/forge/suite2-review-fx/baseline-fast
   git -C /var/tmp/forge/suite2-review-fx/baseline-fast fetch /home/forge/src/clj-surgeon '+refs/heads/*:refs/remotes/upstream/*'
   git -C /var/tmp/forge/suite2-review-fx/baseline-fast checkout --detach 2ecce8c44b7e5f1cc36078d3da262f68a59ac85b
   ```

   Verbatim identifying output:

   ```text
   Cloning into '/var/tmp/forge/suite2-review-fx/baseline-fast'...
   From /home/forge/src/clj-surgeon
    * [new branch]        MCP/main               -> upstream/MCP/main
    * [new branch]        bridge/suite-spike     -> upstream/bridge/suite-spike
   HEAD is now at 2ecce8c4 spike(suite): round two -- the partition, the throwaway home, the concurrency battery
   ```

   Exact load command and verbatim output:

   ```text
   $ uptime
    20:14:41 up 23 days,  1:06, 28 users,  load average: 9.49, 11.61, 12.20
   ```

   Exact suite command:

   ```text
   /usr/bin/time -f 'REVIEW_WALL=%e' make test-fast
   ```

   Verbatim decisive output:

   ```text
   clojure -J-Xms64m -J-Xmx512m -M:clj-surgeon/test-fast
   Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge
   lanes: fast -- 36 namespace(s), home-isolated true
   ...
   Ran 358 tests containing 3569 assertions.
   0 failures, 0 errors.
   REVIEW_WALL=30.91
   ```

3. **The three lane totals reproduce exactly; nothing-count arithmetic closes (GREEN, pin sabotage still pending).** Integration ran 4 namespaces at 71/750/0 in 31.01 s. Battery ran 11 namespaces at 456/8,809/0 in 727.61 s from starting load 9.62. Therefore the separately measured lanes are exactly 36+4+11 = 51 namespaces, 358+71+456 = 885 tests, and 3,569+750+8,809 = 13,128 assertions, matching the claim and exceeding round one's 865/13,023 by 20 tests/105 assertions. The manifest also contains exactly six exclusions, each with prose reasons; sabotage will test whether those declarations really fail closed.

   Exact integration load and suite commands:

   ```text
   uptime
   /usr/bin/time -f 'REVIEW_WALL=%e' make test-integration
   ```

   Verbatim output:

   ```text
    20:19:09 up 23 days,  1:10, 28 users,  load average: 9.77, 10.91, 11.77
   clojure -J-Xms64m -J-Xmx512m -M:clj-surgeon/test-integration
   Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge
   lanes: integration -- 4 namespace(s), home-isolated true
   ...
   Ran 71 tests containing 750 assertions.
   0 failures, 0 errors.
   REVIEW_WALL=31.01
   ```

   Exact battery load and suite commands:

   ```text
   uptime
   /usr/bin/time -f 'REVIEW_WALL=%e' make test-battery
   ```

   Verbatim decisive output:

   ```text
    20:20:00 up 23 days,  1:11, 28 users,  load average: 9.62, 10.70, 11.66
   clojure -J-Xms64m -J-Xmx512m -M:clj-surgeon/test-battery
   Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge
   lanes: battery -- 11 namespace(s), home-isolated false
   ...
   Ran 456 tests containing 8809 assertions.
   0 failures, 0 errors.
   REVIEW_WALL=727.61
   ```

4. **The merge gate is green and isolated, but the headline 426/4,309 count is stale at this tip (non-blocking documentation/count finding).** The current runner prints 40 namespaces and `home-isolated true`, then reports 429 tests / 4,323 assertions / 0 failures / 0 errors. The round-two report itself explains that the final three cadence witnesses made the count 429/4,323, but its top table and the review prompt still quote an earlier 426/4,309 execution. Reviewer wall was 148.53 s from starting load 9.58. The four assertions above the separate fast+integration sum are context-sensitive assertions under the combined 40-namespace load; the separately run lane arithmetic in finding 3 remains exact.

   Exact commands:

   ```text
   uptime
   /usr/bin/time -f 'REVIEW_WALL=%e' make mcp-test
   ```

   Verbatim decisive output:

   ```text
    20:15:55 up 23 days,  1:07, 28 users,  load average: 9.58, 11.16, 12.00
   # @spec MCP-OP-ORACLE-001
   swipl -q -f test/mcp_operation_contract_oracle.pl
   mcp-operation oracle: pass; legacy counterexamples=[verification_failed,verification_pending]
   clojure -J-Xms64m -J-Xmx512m -M:clj-surgeon/mcp-test
   Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge
   lanes: fast+integration -- 40 namespace(s), home-isolated true
   ...
   Ran 429 tests containing 4323 assertions.
   0 failures, 0 errors.
   ...
   tmp-leak ratchet witness passed
   ...
   direct cclsp client audit self-test: ok
   REVIEW_WALL=148.53
   ```

5. **TEST-ISO-001 sabotage suite is genuinely fail-closed (GREEN).** Every requested mutation failed loudly and named the subject. Removal of one ns's metadata refused before running any test; an entirely new test file with no metadata or manifest entry refused at the CLI boundary and printed all three lane/cadence pairs; an unknown cadence failed by cadence and by affected namespaces; a lane with no cadence failed set equality and printed the new lane; an unknown manifest lane named its namespace. Moving a round-one namespace into exclusions did not evade the historical pin: `the-partition-drops-nothing-round-one-measured` named it, with the 36/51 count pins also red.

   Exact metadata-removal command:

   ```text
   uptime
   /usr/bin/time -f 'REVIEW_WALL=%e EXIT=%x' make test-fast
   ```

   Verbatim output:

   ```text
    20:34:04 up 23 days,  1:25, 28 users,  load average: 9.73, 11.84, 11.94
   lanes: fast -- 36 namespace(s), home-isolated true
   lane-refused: 1 namespace(s) whose own ns metadata disagrees with clj-surgeon.lane-manifest (TEST-ISO-001):
     clj-surgeon.mcp-schema-test declares :lane nil but the manifest assigns :fast
   make: *** [Makefile:995: test-fast] Error 96
   Command exited with non-zero status 2
   REVIEW_WALL=13.12 EXIT=2
   ```

   Exact new-file boundary command:

   ```text
   uptime
   /usr/bin/time -f 'REVIEW_WALL=%e EXIT=%x' clojure -J-Xms64m -J-Xmx512m -M:clj-surgeon/test-deps -m clj-surgeon.mcp-test-runner --ns clj-surgeon.reviewer-undeclared-test
   ```

   Verbatim output:

   ```text
    20:34:33 up 23 days,  1:26, 28 users,  load average: 9.92, 11.76, 11.91
   lane-refused: clj-surgeon.reviewer-undeclared-test carries no lane declaration. Every JVM test namespace must appear in clj-surgeon.lane-manifest/manifest with one of :fast (:every-run), :integration (:merge-gate), :battery (:landing-and-nightly) AND carry the same {:lane ...} in its own ns metadata (TEST-ISO-001). The lane you choose decides HOW OFTEN it runs, which is why the cadence is named beside it here. Add it to the manifest and to the ns form, or declare why it belongs to no JVM lane in clj-surgeon.lane-manifest/excluded.
   Command exited with non-zero status 96
   REVIEW_WALL=4.09 EXIT=96
   ```

   Exact unknown-cadence suite command:

   ```text
   uptime
   /usr/bin/time -f 'REVIEW_WALL=%e EXIT=%x' make test-fast
   ```

   Verbatim decisive output:

   ```text
    20:34:41 up 23 days,  1:26, 28 users,  load average: 9.49, 11.60, 11.86
   FAIL in (every-lane-declares-a-cadence-the-runner-knows) (lane_manifest_test.clj:97)
   lane :fast declares cadence :reviewer-unknown-cadence which the runner does not know; known cadences are (:every-run :landing-and-nightly :merge-gate)
   ...
   FAIL in (every-manifest-namespace-resolves-to-a-known-cadence) (lane_manifest_test.clj:108)
   36 namespace(s) with a lane but no cadence the runner knows: clj-surgeon.census-pool-test, ... clj-surgeon.workspace-onboarding-test
   ...
   Ran 358 tests containing 3569 assertions.
   2 failures, 0 errors.
   REVIEW_WALL=32.13 EXIT=2
   ```

   Exact missing-cadence suite command (after the required 60-second wait from load 12.59):

   ```text
   uptime
   /usr/bin/time -f 'REVIEW_WALL=%e EXIT=%x' make test-fast
   ```

   Verbatim decisive output:

   ```text
    20:36:46 up 23 days,  1:28, 28 users,  load average: 7.43, 10.66, 11.53
   FAIL in (every-lane-declares-a-cadence-the-runner-knows) (lane_manifest_test.clj:92)
   a lane with no cadence, or a cadence for a lane that does not exist: lanes [:fast :integration :battery :reviewer-no-cadence] vs (:battery :fast :integration)
   ...
   Ran 358 tests containing 3570 assertions.
   1 failures, 0 errors.
   REVIEW_WALL=35.75 EXIT=2
   ```

   Exact unknown-lane suite command (after three required 60-second waits until load fell below 12):

   ```text
   uptime
   /usr/bin/time -f 'REVIEW_WALL=%e EXIT=%x' make test-fast
   ```

   Verbatim decisive output, including the later ambient kill:

   ```text
    20:41:03 up 23 days,  1:32, 28 users,  load average: 11.20, 11.70, 11.85
   lanes: fast -- 35 namespace(s), home-isolated true
   FAIL in (the-partition-matches-round-ones-measurement) (lane_manifest_test.clj:267)
   expected: (= 36 (count (lm/namespaces-for :fast)))
     actual: (not (= 36 35))
   FAIL in (every-manifest-namespace-resolves-to-a-known-cadence) (lane_manifest_test.clj:108)
   1 namespace(s) with a lane but no cadence the runner knows: clj-surgeon.census-pool-test
   ...
   make: *** [Makefile:995: test-fast] Killed
   Command exited with non-zero status 2
   REVIEW_WALL=17.23 EXIT=2
   ```

   Exact exclusion-pin suite command:

   ```text
   uptime
   /usr/bin/time -f 'REVIEW_WALL=%e EXIT=%x' make test-fast
   ```

   Verbatim decisive output:

   ```text
    20:42:39 up 23 days,  1:34, 28 users,  load average: 10.90, 11.65, 11.83
   lanes: fast -- 35 namespace(s), home-isolated true
   FAIL in (the-partition-drops-nothing-round-one-measured) (lane_manifest_test.clj:261)
   1 namespace(s) that round one MEASURED are in no lane -- partitioning must never drop: clj-surgeon.census-pool-test
   ...
   expected: (= 36 (count (lm/namespaces-for :fast)))
     actual: (not (= 36 35))
   ...
   expected: (= 51 (count lm/manifest))
     actual: (not (= 51 50))
   ...
   Ran 355 tests containing 3563 assertions.
   3 failures, 0 errors.
   REVIEW_WALL=31.63 EXIT=2
   ```

6. **TEST-ISO-006 holds for both promised entrances; tmpfs refusal ordering is correct (GREEN).** I planted two tests into the existing fast isolation namespace in separate fresh clones: one expected `.m2` under `user.home`; one wrote through `$HOME` and expected the marker in `/home/forge`. Both `make test-fast` and the combined `make mcp-test` printed `home-isolated true`, failed exactly those two counterfactual assertions, and left `/home/forge/suite2-review-home-write.txt` absent. The fast run's entire top-level suite-root set was unchanged. The merge-gate run overlapped other seats (one unrelated suite root disappeared and another appeared), but its own printed root `clj-surgeon-suite-2983296-b91e96d9` was absent immediately after exit, proving this run's throwaway was deleted. Finally, running the no-lane runner with both temp spellings forced to `/tmp` produced `tmp-refused` and exit 97, not the competing no-lane exit 96.

   Exact fast sabotage command:

   ```text
   uptime
   before=$(find /var/tmp/forge -mindepth 1 -maxdepth 1 -type d -name 'clj-surgeon-suite-*' -printf '%f\n' | sort)
   /usr/bin/time -f 'REVIEW_WALL=%e EXIT=%x' make test-fast
   after=$(find /var/tmp/forge -mindepth 1 -maxdepth 1 -type d -name 'clj-surgeon-suite-*' -printf '%f\n' | sort)
   ```

   Verbatim decisive output:

   ```text
    20:48:57 up 23 days,  1:40, 28 users,  load average: 10.24, 11.67, 11.91
   REAL_HOME_BEFORE=absent
   lanes: fast -- 36 namespace(s), home-isolated true
   FAIL in (reviewer-planted-real-home-read) (fast_lane_isolation_test.clj:76)
   expected: (.exists (io/file (prop "user.home") ".m2"))
     actual: false
   FAIL in (reviewer-planted-real-home-write) (fast_lane_isolation_test.clj:83)
   expected: (.exists (io/file "/home/forge" "suite2-review-home-write.txt"))
     actual: false
   ...
   Ran 360 tests containing 3571 assertions.
   2 failures, 0 errors.
   REVIEW_WALL=30.85 EXIT=2
   THROWAWAY_SET_UNCHANGED=yes
   REAL_HOME_AFTER=absent
   ```

   Exact merge-gate sabotage command:

   ```text
   uptime
   before=$(find /var/tmp/forge -mindepth 1 -maxdepth 1 -type d -name 'clj-surgeon-suite-*' -printf '%f\n' | sort)
   /usr/bin/time -f 'REVIEW_WALL=%e EXIT=%x' make mcp-test
   after=$(find /var/tmp/forge -mindepth 1 -maxdepth 1 -type d -name 'clj-surgeon-suite-*' -printf '%f\n' | sort)
   test ! -d /var/tmp/forge/clj-surgeon-suite-2983296-b91e96d9
   ```

   Verbatim decisive output:

   ```text
    20:49:42 up 23 days,  1:41, 28 users,  load average: 9.52, 11.34, 11.79
   REAL_HOME_BEFORE=absent
   lanes: fast+integration -- 40 namespace(s), home-isolated true
   FAIL in (reviewer-planted-real-home-read) (fast_lane_isolation_test.clj:76)
   expected: (.exists (io/file (prop "user.home") ".m2"))
     actual: false
   FAIL in (reviewer-planted-real-home-write) (fast_lane_isolation_test.clj:83)
   expected: (.exists (io/file "/home/forge" "suite2-review-home-write.txt"))
     actual: false
   ...
   Ran 431 tests containing 4325 assertions.
   2 failures, 0 errors.
   REVIEW_WALL=50.63 EXIT=2
   THROWAWAY_SET_UNCHANGED=no
   BEFORE=clj-surgeon-suite-2600557-91559231
   clj-surgeon-suite-2780737-8017afa4
   clj-surgeon-suite-2923838-9a0da855
   clj-surgeon-suite-2936247-8b286ce5
   AFTER=clj-surgeon-suite-2600557-91559231
   clj-surgeon-suite-2780737-8017afa4
   clj-surgeon-suite-2936247-8b286ce5
   clj-surgeon-suite-2992132-b1ebb235
   REAL_HOME_AFTER=absent
   REVIEW_RUN_ROOT_AFTER=absent
   ```

   Exact refusal-order command:

   ```text
   TMPDIR=/tmp JAVA_TOOL_OPTIONS=-Djava.io.tmpdir=/tmp clojure -J-Xms64m -J-Xmx512m -M:clj-surgeon/test-deps -m clj-surgeon.mcp-test-runner
   ```

   Verbatim output:

   ```text
   Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/tmp
   tmp-refused: java.io.tmpdir base=/tmp is a RAM-backed path by name (/tmp or /dev/shm). Launch with -Djava.io.tmpdir=/var/tmp/forge, or export TMPDIR=/var/tmp/forge before invoking bb (bb does not read JAVA_TOOL_OPTIONS -- see ~/bin/suite-run / seat-tmp-guard.sh).
   EXIT=97
   ```

7. **All four historical race stages go RED under sufficient contention, while the tip is GREEN (GREEN).** I used exact historical parents as reversions in git-archive copies: `a48a6e1c` (before `c8b0ad19`) restores teardown throw, skipped workspace cleanup, and first deadline defects; `c8b0ad19` (before `be2088b4`) restores five short rendezvous budgets; `be2088b4` (before `881427bd`) restores the 1-second owner; `881427bd` (before `9c09398f`) restores the shared 100 ms owner/waiter environment. The first three stages reproduced with two copies/12 burners. The last was 4/4 green at lower contention, then reproduced decisively with six copies/24 burners at load 30.95; that miss and escalation are both disclosed. The reviewed tip then ran the same 24-test prepared-wire+process selection two-wide/12 burners at ending load 31.42: both copies 24 tests/102 assertions/0.

   Exact pre-`c8b0ad19` command:

   ```text
   /var/tmp/forge/suite2-review-fx/run_contention.sh prec8 a48a6e1cd4e077d11b2a89363677fa03e83ef9b5 2 12 clj-surgeon.mcp-prepared-wire-test clj-surgeon.mcp-process-test
   ```

   Verbatim output:

   ```text
    20:53:31 up 23 days,  1:45, 28 users,  load average: 4.75, 8.76, 10.75
   contention: label=prec8 commit=a48a6e1cd4e077d11b2a89363677fa03e83ef9b5 N=2 burners=12
    20:54:59 up 23 days,  1:46, 28 users,  load average: 24.02, 14.56, 12.66
   contention: wall 88 s
   --- copy 1 (exit 6) ---
   ERROR in (a-teardown-failure-still-deletes-the-workspace) (FutureTask.java:122)
   ERROR in (prepared-confirm-preview-commit-and-replay-cross-the-real-http-wire) (mcp_prepared_wire_test.clj:57)
   ERROR in (stop-child-reports-a-stderr-reader-failure-as-a-typed-fact) (FutureTask.java:122)
   FAIL in (direct-shell-shim-uses-the-same-host-admission) (mcp_process_test.clj:275)
   FAIL in (admission-wait-and-analyzer-share-one-deadline) (mcp_process_test.clj:369)
   Ran 24 tests containing 85 assertions.
   2 failures, 3 errors.
   temp-leak: 1 entries left under /var/tmp/forge/clj-surgeon-suite-3067118-a1206e83: prepared-wire-teardown-witness-111330803935356658
   --- copy 2 (exit 9) ---
   ERROR in (a-teardown-failure-still-deletes-the-workspace) (FutureTask.java:122)
   ERROR in (prepared-confirm-preview-commit-and-replay-cross-the-real-http-wire) (mcp_prepared_wire_test.clj:57)
   ERROR in (stop-child-reports-a-stderr-reader-failure-as-a-typed-fact) (FutureTask.java:122)
   FAIL in (admission-timeout-launches-no-second-analyzer) (mcp_process_test.clj:191)
   FAIL in (admission-timeout-launches-no-second-analyzer) (mcp_process_test.clj:199)
   FAIL in (admission-timeout-launches-no-second-analyzer) (mcp_process_test.clj:200)
   FAIL in (admission-timeout-launches-no-second-analyzer) (mcp_process_test.clj:201)
   FAIL in (admission-timeout-launches-no-second-analyzer) (mcp_process_test.clj:203)
   Ran 24 tests containing 85 assertions.
   5 failures, 3 errors.
   temp-leak: 1 entries left under /var/tmp/forge/clj-surgeon-suite-3067119-6883678e: prepared-wire-teardown-witness-16688258675159844294
   contention: VERDICT RED
   ```

   Exact pre-`be2088b4` command and verbatim output:

   ```text
   $ /var/tmp/forge/suite2-review-fx/run_contention.sh prebe c8b0ad19e0134a43e4f5cc468f3412e50a46ff98 2 12 clj-surgeon.mcp-process-test
    20:56:34 up 23 days,  1:48, 28 users,  load average: 7.66, 11.51, 11.75
   contention: wall 24 s
   --- copy 1 (exit 3) ---
   FAIL in (explicit-admission-skips-a-path-shadowing-shell-shim) (mcp_process_test.clj:348)
   FAIL in (stale-owner-text-does-not-own-the-operating-system-lock) (mcp_process_test.clj:254)
   FAIL in (stale-owner-text-does-not-own-the-operating-system-lock) (mcp_process_test.clj:255)
   Ran 19 tests containing 71 assertions.
   3 failures, 0 errors.
   --- copy 2 (exit 4) ---
   FAIL in (stale-owner-text-does-not-own-the-operating-system-lock) (mcp_process_test.clj:254)
   FAIL in (stale-owner-text-does-not-own-the-operating-system-lock) (mcp_process_test.clj:255)
   FAIL in (admission-wait-and-analyzer-share-one-deadline) (mcp_process_test.clj:441)
   FAIL in (admission-wait-and-analyzer-share-one-deadline) (mcp_process_test.clj:443)
   Ran 19 tests containing 71 assertions.
   4 failures, 0 errors.
   contention: VERDICT RED
   ```

   Exact pre-`881427bd` command and verbatim output:

   ```text
   $ /var/tmp/forge/suite2-review-fx/run_contention.sh pre881 be2088b4781db2d793a39329994836ea60ddc236 2 12 clj-surgeon.mcp-process-test
    20:58:28 up 23 days,  1:49, 28 users,  load average: 8.68, 11.59, 11.82
   contention: wall 38 s
   --- copy 1 (exit 0) ---
   Ran 19 tests containing 71 assertions.
   0 failures, 0 errors.
   --- copy 2 (exit 2) ---
   FAIL in (direct-shell-shim-uses-the-same-host-admission) (mcp_process_test.clj:314)
   FAIL in (direct-shell-shim-uses-the-same-host-admission) (mcp_process_test.clj:321)
   Ran 19 tests containing 71 assertions.
   2 failures, 0 errors.
   contention: VERDICT RED
   ```

   Exact pre-`9c09398f` commands and verbatim first outcomes:

   ```text
   $ /var/tmp/forge/suite2-review-fx/run_contention.sh pre9c 881427bd74e0c262eac51a54a9457c0134b36b52 2 12 clj-surgeon.mcp-process-test
   ...
   --- copy 1 (exit 0) ---
   Ran 19 tests containing 71 assertions.
   0 failures, 0 errors.
   --- copy 2 (exit 0) ---
   Ran 19 tests containing 71 assertions.
   0 failures, 0 errors.
   contention: VERDICT ALL-GREEN

   $ /var/tmp/forge/suite2-review-fx/run_contention.sh pre9c-b24 881427bd74e0c262eac51a54a9457c0134b36b52 2 24 clj-surgeon.mcp-process-test
   ...
   --- copy 1 (exit 0) ---
   Ran 19 tests containing 71 assertions.
   0 failures, 0 errors.
   --- copy 2 (exit 0) ---
   Ran 19 tests containing 71 assertions.
   0 failures, 0 errors.
   contention: VERDICT ALL-GREEN
   ```

   Exact high-contention command and verbatim decisive output:

   ```text
   $ /var/tmp/forge/suite2-review-fx/run_contention.sh pre9c-n6 881427bd74e0c262eac51a54a9457c0134b36b52 6 24 clj-surgeon.mcp-process-test
    21:05:07 up 23 days,  1:56, 28 users,  load average: 9.19, 11.77, 11.98
   contention: label=pre9c-n6 commit=881427bd74e0c262eac51a54a9457c0134b36b52 N=6 burners=24
    21:06:47 up 23 days,  1:58, 28 users,  load average: 30.95, 19.37, 14.74
   contention: wall 98 s
   --- copy 1 (exit 7) ---
   ...
   FAIL in (direct-shell-shim-uses-the-same-host-admission) (mcp_process_test.clj:344)
   FAIL in (direct-shell-shim-uses-the-same-host-admission) (mcp_process_test.clj:352)
   Ran 19 tests containing 71 assertions.
   6 failures, 1 errors.
   --- copy 2 (exit 2) ---
   Ran 19 tests containing 71 assertions.
   2 failures, 0 errors.
   --- copy 3 (exit 3) ---
   FAIL in (direct-shell-shim-uses-the-same-host-admission) (mcp_process_test.clj:344)
   Ran 19 tests containing 71 assertions.
   3 failures, 0 errors.
   --- copy 4 (exit 2) ---
   Ran 19 tests containing 71 assertions.
   2 failures, 0 errors.
   --- copy 5 (exit 6) ---
   ...
   FAIL in (direct-shell-shim-uses-the-same-host-admission) (mcp_process_test.clj:344)
   Ran 19 tests containing 71 assertions.
   5 failures, 1 errors.
   --- copy 6 (exit 5) ---
   ...
   FAIL in (direct-shell-shim-uses-the-same-host-admission) (mcp_process_test.clj:344)
   FAIL in (direct-shell-shim-uses-the-same-host-admission) (mcp_process_test.clj:352)
   Ran 19 tests containing 71 assertions.
   4 failures, 1 errors.
   contention: VERDICT RED
   ```

   Exact current-tip counterfactual command and verbatim output:

   ```text
   $ /var/tmp/forge/suite2-review-fx/run_contention.sh current 2ecce8c44b7e5f1cc36078d3da262f68a59ac85b 2 12 clj-surgeon.mcp-prepared-wire-test clj-surgeon.mcp-process-test
    21:10:01 up 23 days,  2:01, 28 users,  load average: 7.48, 13.38, 13.26
   contention: label=current commit=2ecce8c44b7e5f1cc36078d3da262f68a59ac85b N=2 burners=12
    21:12:05 up 23 days,  2:03, 28 users,  load average: 31.42, 20.90, 16.06
   contention: wall 124 s
   --- copy 1 (exit 0) ---
   Ran 24 tests containing 102 assertions.
   0 failures, 0 errors.
   --- copy 2 (exit 0) ---
   Ran 24 tests containing 102 assertions.
   0 failures, 0 errors.
   contention: VERDICT ALL-GREEN
   ```

8. **A seventh timing-dependent fast-lane site remains (REAL, but non-blocking for this round under Gene's explicit scope ruling).** `test/clj_surgeon/scope_stream_test.clj:76,78` belongs to `:fast` and does `(System/gc)`, sleeps exactly 100 ms, repeats, then requires every `WeakReference` to be cleared. That assertion depends on collector/scheduler progress inside 200 ms; it is not a deterministic program contract or a winnable rendezvous. Other residual fixed waits exist too (`census_pool_test.clj:13-19`, `workspace_onboarding_test.clj:308-310`, `mcp_tool_test.clj:1380`), but the GC witness is the clearest seventh race candidate. It must be repaired or reclassified under TEST-ISO-011. The canonical spec explicitly assigns TEST-ISO-011 to round three, while round two is TEST-ISO-001/006/009, so I do not make this known follow-up a second round-two landing blocker.

   Exact discovery command:

   ```text
   rg -n 'Thread/sleep|System/(currentTimeMillis|nanoTime)|timeout-ms|\.waitFor|deref[^\n]*[0-9]{2,}' test/clj_surgeon --glob '*.clj'
   ```

   Verbatim relevant output:

   ```text
   test/clj_surgeon/scope_stream_test.clj:76:          (Thread/sleep 100)
   test/clj_surgeon/scope_stream_test.clj:78:          (Thread/sleep 100)
   test/clj_surgeon/census_pool_test.clj:19:      :else (do (Thread/sleep 20) (recur (inc attempts))))))
   test/clj_surgeon/workspace_onboarding_test.clj:308:          (is (= true (deref first-entered 1000 ::timeout)))
   test/clj_surgeon/workspace_onboarding_test.clj:310:            (is (= ::timeout (deref second-entered 100 ::timeout))
   test/clj_surgeon/mcp_tool_test.clj:1380:                  (do (Thread/sleep 10) (recur (inc attempt))))))]
   ```

   Verbatim source at the primary site (`clj-surgeon :op :cat :file test/clj_surgeon/scope_stream_test.clj :line 76`):

   ```clojure
   (System/gc)
   (Thread/sleep 100)
   (System/gc)
   (Thread/sleep 100)
   (is (every? #(nil? (.get ^WeakReference %)) @refs)
       "every source the planner saw has been collected")
   ```

9. **TEST-ISO-009 passes twice at N=4 and once at N=6 (GREEN; box throughput is the limit observed, not correctness).** Every clone in all three batteries reported the current 429/4,323/0 summary and exit 0. N=4 run 1 took 194 s internally (196.82 s total), starting load 8.17 and ending 20.33. N=4 run 2 took 224 s internally (286.65 s total including its own 60-second load wait), starting 8.60 and ending 22.67. N=6 took 248 s internally (251.79 total), starting 9.62 and ending 31.17. The two extra clones added only 24–54 s relative to the N=4 walls and no failures, so this evidence points to host throughput/queueing as the scaling limit. Each outer invocation ran in a separately fetched no-local clone; `SUITE_BATTERY_FX` redirected the shipped target's internal real clones under the mandated fixture root.

   Exact N=4 run-1 command:

   ```text
   /usr/bin/time -f 'REVIEW_TOTAL_WALL=%e' env SUITE_BATTERY_FX=/var/tmp/forge/suite2-review-fx/battery-n4a/internal make suite-concurrency-battery N=4
   ```

   Verbatim summary:

   ```text
   suite-battery: N=4 target=mcp-test sha=2ecce8c4
   suite-battery: load at start 8.17 15.04 14.74 21/2540 3539802
   suite-battery: load at end   20.33 17.96 15.94 7/2462 3634337
   suite-battery: wall 194 s
   --- clone 1: exit 0 | Ran 429 tests containing 4323 assertions. 0 failures, 0 errors.
   --- clone 2: exit 0 | Ran 429 tests containing 4323 assertions. 0 failures, 0 errors.
   --- clone 3: exit 0 | Ran 429 tests containing 4323 assertions. 0 failures, 0 errors.
   --- clone 4: exit 0 | Ran 429 tests containing 4323 assertions. 0 failures, 0 errors.
   suite-battery: VERDICT PASS -- all 4 clones 0 failures, 0 errors
   REVIEW_TOTAL_WALL=196.82
   ```

   Exact N=4 run-2 command:

   ```text
   /usr/bin/time -f 'REVIEW_TOTAL_WALL=%e' env SUITE_BATTERY_FX=/var/tmp/forge/suite2-review-fx/battery-n4b/internal make suite-concurrency-battery N=4
   ```

   Verbatim summary:

   ```text
   suite-battery: load 10.21 > 10, waiting 60 s (1/10)
   suite-battery: N=4 target=mcp-test sha=2ecce8c4
   suite-battery: load at start 8.60 13.80 14.67 10/2335 3690306
   suite-battery: load at end   22.67 20.63 17.35 4/2554 3792854
   suite-battery: wall 224 s
   --- clone 1: exit 0 | Ran 429 tests containing 4323 assertions. 0 failures, 0 errors.
   --- clone 2: exit 0 | Ran 429 tests containing 4323 assertions. 0 failures, 0 errors.
   --- clone 3: exit 0 | Ran 429 tests containing 4323 assertions. 0 failures, 0 errors.
   --- clone 4: exit 0 | Ran 429 tests containing 4323 assertions. 0 failures, 0 errors.
   suite-battery: VERDICT PASS -- all 4 clones 0 failures, 0 errors
   REVIEW_TOTAL_WALL=286.65
   ```

   Exact N=6 command:

   ```text
   /usr/bin/time -f 'REVIEW_TOTAL_WALL=%e' env SUITE_BATTERY_FX=/var/tmp/forge/suite2-review-fx/battery-n6/internal make suite-concurrency-battery N=6
   ```

   Verbatim summary:

   ```text
   suite-battery: N=6 target=mcp-test sha=2ecce8c4
   suite-battery: load at start 9.62 16.87 16.32 4/2549 3824173
   suite-battery: load at end   31.17 26.01 20.23 12/2598 3946164
   suite-battery: wall 248 s
   --- clone 1: exit 0 | Ran 429 tests containing 4323 assertions. 0 failures, 0 errors.
   --- clone 2: exit 0 | Ran 429 tests containing 4323 assertions. 0 failures, 0 errors.
   --- clone 3: exit 0 | Ran 429 tests containing 4323 assertions. 0 failures, 0 errors.
   --- clone 4: exit 0 | Ran 429 tests containing 4323 assertions. 0 failures, 0 errors.
   --- clone 5: exit 0 | Ran 429 tests containing 4323 assertions. 0 failures, 0 errors.
   --- clone 6: exit 0 | Ran 429 tests containing 4323 assertions. 0 failures, 0 errors.
   suite-battery: VERDICT PASS -- all 6 clones 0 failures, 0 errors
   REVIEW_TOTAL_WALL=251.79
   ```

10. **The orphan formatter test is a real coverage loss (BLOCKING).** `test/clj_surgeon/mcp_formatter_test.clj:23-86` contains three green tests/18 assertions over staged formatting, invalid-command/failure/timeout refusals, and verification-profile rewriting. No runner or Make target requires the namespace: the only references are its own file, the exclusion manifest, the lane audit's explanatory comments, and the design disclosure. Production is live: `src/clj_surgeon/mcp_tool.clj:16,367,778,785,790` and `src/clj_surgeon/mcp_http_server.clj:5,40` use `mcp-formatter`. The apparent formatter coverage in `mcp_tool_test.clj` substitutes `format-candidates!`; it does not replace these real boundary tests. Declaring the orphan with a reason makes omission visible, but does not make it non-loss. Adding this namespace to the battery lane would make the complete JVM corpus 52 namespaces and 888 tests/13,146 separately summed assertions, not the claimed 51/885/13,128. The narrower “nothing was dropped relative to round one's already-incomplete 49-namespace runner” is true; the broader suite-completeness claim is false.

   Exact reference audit command and verbatim output:

   ```text
   $ rg -n --hidden --glob '!.git/**' 'mcp-formatter-test|mcp_formatter_test' Makefile deps.edn test src docs/intent docs/*.md skills bench dev
   docs/intent/test-isolation/test-isolation-design.md:55:noticing: `mcp-formatter-test` is required by no runner and no Make target.
   test/clj_surgeon/lane_manifest.clj:173:   'clj-surgeon.mcp-formatter-test
   test/clj_surgeon/lane_manifest_test.clj:10:   for once (round one's `mcp-formatter-test` is required by no runner and no
   test/clj_surgeon/mcp_formatter_test.clj:1:(ns clj-surgeon.mcp-formatter-test
   test/clj_surgeon/mcp_test_runner.clj:31:    ;; the failure mode `mcp-formatter-test` has been living in unnoticed.
   ```

   Exact standalone command:

   ```text
   uptime
   /usr/bin/time -f 'REVIEW_WALL=%e' clojure -J-Xms64m -J-Xmx512m -M:clj-surgeon/test-deps -e "(require 'clj-surgeon.mcp-formatter-test 'clojure.test) (let [r (clojure.test/run-tests 'clj-surgeon.mcp-formatter-test)] (System/exit (+ (:fail r) (:error r))))"
   ```

   Verbatim output:

   ```text
    21:39:30 up 23 days,  2:31, 29 users,  load average: 10.96, 15.76, 17.45
   Testing clj-surgeon.mcp-formatter-test

   Ran 3 tests containing 18 assertions.
   0 failures, 0 errors.
   REVIEW_WALL=6.28
   ```

11. **The Make/alias rename works, but three live descriptions and an agent brief still mean the old target (MUST-FIX; independently non-blocking, but unsafe to leave operationally ambiguous).** The executable target is correct: `Makefile:991-995` maps `test-fast` to `:clj-surgeon/test-fast`, `Makefile:1006-1009` maps the unchanged Babashka corpus to `test-bb`, and the fresh `make test-bb` run reproduced 840/6,919/0. No script actually invokes `make test-fast`; the `bench/anvil-arms` occurrences only classify strings as test commands. However: (a) `test/clj_surgeon/reader_eval_fence_test.clj:35-39` says the namespace rides `mcp-test`, not `test-fast`, because `test-fast` is Babashka; all three statements are now false—the namespace is `:battery`; (b) `test/clj_surgeon/outline_memory_test.clj:6` calls Babashka “the `test-fast` runner” while the namespace itself is now in JVM fast; (c) `docs/intent/temp-dir-hygiene/temp-dir-hygiene-specs.md:136-137` says `test-fast` and `mcp-test` witness both `test/run_all.clj` and the MCP runner, but the Babashka witness is now `test-bb`; and (d) the reusable `docs/observations/2026-09-02-anvil-builder-seat-brief.md:33` still prescribes `make test-fast / make mcp-test` under the old meaning and would now omit the bb lane. There are zero `test-fast` references under `skills/`.

   The exhaustive inventory is 80 files: 61 under `docs/observations`, 19 elsewhere. Of the observation files, `2026-09-04-suite-spike-round2.md` explains the rename; the other 60 are pre-rename historical receipts/briefs and their quoted `test-fast` counts mean the old bb lane. The historical top-level `LQBM-REPORT.md`, `REPAIR-REPORT.md`, and `dev/experiments/namespace_tolerance_replay_receipt.edn` also mean old. The remaining live generic exclusions (“not reachable from test-fast”) and the watcher command-recognition strings remain semantically valid independent of lane content.

   Exact inventory command and verbatim counts:

   ```text
   $ printf 'all_files='; rg -l --hidden --glob '!.git/**' '\btest-fast\b' . | wc -l
   all_files=80
   $ printf 'observation_files='; rg -l '\btest-fast\b' docs/observations | wc -l
   observation_files=61
   $ printf 'skill_files='; (rg -l '\btest-fast\b' skills || true) | wc -l
   skill_files=0
   ```

   Verbatim stale source text:

   ```text
   test/clj_surgeon/reader_eval_fence_test.clj:35:   This namespace rides the `mcp-test` lane and NOT `test-fast`, for a
   test/clj_surgeon/reader_eval_fence_test.clj:36:   mechanical reason worth writing down: `test-fast` is `bb test/run_all.clj`,
   test/clj_surgeon/outline_memory_test.clj:6:   `test-fast` runner cannot provide. They live in the MCP JVM suite."
   docs/intent/temp-dir-hygiene/temp-dir-hygiene-specs.md:136:Makefile's `test-fast` and `mcp-test` targets are this leaf's
   docs/observations/2026-09-02-anvil-builder-seat-brief.md:33:curtain-call under `~/src/curtaincall-cfp-<branch>`; suites via `make test-fast` / `make mcp-test`
   ```

   Exact bb command and verbatim summary:

   ```text
   $ uptime
    21:40:30 up 23 days,  2:32, 29 users,  load average: 9.37, 14.41, 16.88
   $ /usr/bin/time -f 'REVIEW_WALL=%e' make test-bb
   bb test/run_all.clj
   ...
   Ran 840 tests containing 6919 assertions.
   0 failures, 0 errors.
   REVIEW_WALL=153.25
   ```

12. **Oracle, repository hygiene, and intent audit reproduce (GREEN).** These were run from separately fetched no-local clones. The intent audit covered 404 specs with 388 implementation-witness IDs, 389 test-witness IDs, and no violations.

   Exact commands and verbatim outputs:

   ```text
   $ /usr/bin/time -f 'REVIEW_WALL=%e' make mcp-operation-oracle
   # @spec MCP-OP-ORACLE-001
   swipl -q -f test/mcp_operation_contract_oracle.pl
   mcp-operation oracle: pass; legacy counterexamples=[verification_failed,verification_pending]
   REVIEW_WALL=1.57

   $ uptime
    21:44:05 up 23 days,  2:35, 29 users,  load average: 15.61, 14.25, 16.25
   $ /usr/bin/time -f 'REVIEW_WALL=%e' make repository-hygiene
   # @spec MCP-OP-ALIAS-036
   # @spec MCP-OP-ALIAS-053
   repository hygiene: no machine-local build cache is tracked at any depth
   REVIEW_WALL=1.47

   $ uptime
    21:44:13 up 23 days,  2:35, 29 users,  load average: 16.44, 14.44, 16.30
   $ /usr/bin/time -f 'REVIEW_WALL=%e' clojure -J-Xms64m -J-Xmx512m -M:clj-surgeon/test-deps -e "(require '[clj-surgeon.mcp-intent-contract :as intent]) (let [r (intent/audit-current-repository)] (prn {:ok (:ok r) :specs (count (:specs r)) :implementation-witnesses (count (:implementation-witnesses r)) :test-witnesses (count (:test-witnesses r)) :violations (:violations r)}) (System/exit (if (:ok r) 0 1)))"
   {:ok true, :specs 404, :implementation-witnesses 388, :test-witnesses 389, :violations []}
   REVIEW_WALL=2.31
   ```

13. **Merge-tree claim reproduces against final-fetched `origin/MCP/main` `c61dbe56b414c6c54df8c3c0560872836050c5f4` (GREEN).** There is exactly one textual conflict, in `Makefile`; `test/clj_surgeon/admit_patch_test.clj` auto-merges. Inspecting virtual merge tree `8aad65cf9ea3b2fe9c4ac15da66cb24e43d127c2` shows the only markers surround the `.PHONY` line: this branch adds `test-integration test-battery test-bb suite-concurrency-battery`, while trunk adds `admit-transaction-recovery-battery`. Union is the mechanical resolution.

   Exact command and verbatim output:

   ```text
   $ git fetch origin MCP/main bridge/suite-spike
   From /home/forge/src/clj-surgeon
    * branch              MCP/main   -> FETCH_HEAD
    * branch              bridge/suite-spike -> FETCH_HEAD
      96272b9d..c61dbe56  MCP/main   -> origin/MCP/main
   $ git rev-parse origin/MCP/main
   c61dbe56b414c6c54df8c3c0560872836050c5f4
   $ git rev-parse origin/bridge/suite-spike
   2ecce8c44b7e5f1cc36078d3da262f68a59ac85b
   $ git merge-tree --write-tree HEAD origin/MCP/main
   8aad65cf9ea3b2fe9c4ac15da66cb24e43d127c2
   100644 2795bab419bc045e605b8f33d23464603b77d457 1 Makefile
   100644 604092827e52a8f856e92a3fa31e5d60717ed8d9 2 Makefile
   100644 db1cfbc1a2fbeb9160a04f755313d9848141d780 3 Makefile

   Auto-merging Makefile
   CONFLICT (content): Merge conflict in Makefile
   Auto-merging test/clj_surgeon/admit_patch_test.clj
   EXIT=1
   ```

## Verdict

**NO-GO.** TEST-ISO-001, TEST-ISO-006, and TEST-ISO-009 themselves hold: the manifest refuses every requested sabotage by name, both public merge-gate entrances isolate home/tmp and clean up, the three declared lanes total exactly 51 namespaces/885 tests/13,128 assertions, every requested race reversion went red under sufficient contention while the tip stayed green, and both N=4 batteries plus N=6 passed. The blocking defect is that `clj-surgeon.mcp-formatter-test` is excluded from every target despite being a green 3-test/18-assertion witness over live production formatter paths; the resulting 51-namespace total is not the complete JVM test corpus. Blocking: assign that namespace a cadence and make a gate run it, then update the pinned totals. Non-blocking but required follow-up: remove the stale pre-rename live descriptions/agent brief, correct the stale 426/4,309 headline, and repair/reclassify the `scope-stream-test` GC/sleep timing witness in the explicitly deferred TEST-ISO-011 round. The merge against final-fetched MCP/main `c61dbe56b414c6c54df8c3c0560872836050c5f4` has the claimed single mechanical `.PHONY` conflict.
