(ns clj-surgeon.lane-manifest
  "TEST-ISO-001 -- THE SINGLE SOURCE OF TRUTH for which JVM test lane every
   test namespace runs in.

   Round one (docs/observations/2026-09-04-suite-spike-round1.md) measured the
   49-namespace `clojure -M:clj-surgeon/mcp-test` lane at 716.7 s and found
   that ELEVEN namespaces which launch cold JVM/bb/CLI child processes are
   674.0 s of it (94%), while the other 36 finish 865 tests' worth of work in
   20.9 s. The lane cap the fleet pays on every builder brief is bought by
   those eleven, not by the tests as a body. This manifest is the partition
   that lets the rest stop queueing behind them.

   THREE THINGS AGREE, AND A WITNESS CHECKS ALL THREE
   (`clj-surgeon.lane-manifest-test`):
     1. this map -- what the runner actually runs;
     2. each namespace's OWN ns metadata `{:lane :fast}` -- readable at the
        file you are editing, so moving a test needs a reason at the pin;
     3. the set of `*_test.clj` files on disk -- so a new test namespace that
        nobody put in a lane fails the suite by name instead of silently
        never running.
   The map is the authority; 2 and 3 are cross-checks against it. A namespace
   the runner is asked for that is not in this map is a TYPED REFUSAL
   (`clj-surgeon.mcp-test-runner/lane-namespaces`), not a silent skip.

   THE LANE RULES -- what a lane MEANS, not merely which names are in it:

   :fast        No child process. No socket bind. No network. No read of the
                real `$HOME` or of anything outside the run's own
                `java.io.tmpdir` subtree -- and TEST-ISO-006 makes that one
                unrepresentable rather than merely checked: the fast lane's
                JVM is launched with `-Duser.home` AND `-Djava.io.tmpdir` on
                a throwaway root that is deleted when the run ends. No write
                into the repository working tree. Target: under 60 s cold.

   :integration Binds an EPHEMERAL port (`:port 0`) or drives a server
                in-process, or writes a per-test workspace into the
                repository root. Per-test unique resources only; still no
                cold child JVM and still no network.

   :battery     Launches a JVM, `bb`, a CLI, `clj-kondo`, `git`, `strace`, or
                anything else that costs a cold runtime; or measures the
                machine (wall-clock deadlines); or reaches the NETWORK.
                Minutes-scale. Deliberately OUT of the merge-gate lane
                (`make mcp-test` = fast + integration). The landing gate
                runs the complete alias/artifact batteries and checks freshness
                for the remaining battery; `make test-battery` runs it all.

   NETWORK IS A BATTERY PROPERTY, EXPLICITLY. Round one's runtime sampler
   caught `mcp-prepared-wire-test` spawning `clojure -X:clj-surgeon/mcp`,
   which spawns `git remote-https origin https://github.com/bhauman/clojure-mcp`
   through `~/.gitlibs`. No source scan of that namespace names a URL. A fast
   or integration lane MUST NOT touch the network: those lanes run N-wide
   from N clones, and a lane whose wall depends on a remote host is not a
   merge gate."
  (:require
   [clojure.string :as str]))

(def lanes
  "Declaration order is execution order across lanes."
  [:fast :integration :battery])

(def cadences
  "The cadences a lane may be declared at, and what each one MEANS. Gene,
   2026-09-04: the manifest declares WHEN a lane runs in the same place it
   declares what is in it.

   A lane's cadence is not decoration -- it is the reason the partition pays.
   Splitting a suite into lanes buys nothing if every lane still runs at the
   same moment; the win is that the 20.9 s of real tests stop queueing behind
   674 s of cold launcher drives, and that is a statement about WHEN, not
   about WHAT. Putting the two in one source of truth means moving a namespace
   between lanes changes how often it runs, visibly, at the pin."
  {:every-run
   "Every run -- the inner loop. Seconds-scale; an agent runs it after each edit."

   :merge-gate
   "The merge gate, with the fast lane: `make mcp-test`. Runs before anything
    is proposed for landing."

   :landing-and-nightly
   "Before every landing, under `flock /home/forge/tmp/suite.lock` because it
    measures the machine and must not share a box lane with another JVM suite;
    and nightly on the trunk tip. Minutes-scale, deliberately OUT of the merge
    gate."})

(def lane-cadence
  "lane -> cadence. Set equality with `lanes` is asserted by the witness: a
   lane with no cadence, or a cadence this map does not know, is a refusal."
  {:fast :every-run
   :integration :merge-gate
   :battery :landing-and-nightly})

(def manifest
  "test namespace -> lane. THE authority. Adding a JVM test namespace without
   adding it here fails `clj-surgeon.lane-manifest-test` by name."
  {;; Block B: every former bb-only owner has measured cadence and budget.
   ;; Receipt and reasons: attempt10/lane-moves.md in the block-B observations.
   'clj-surgeon.agent-routing-test :fast
   'clj-surgeon.alias-migration-test :fast
   'clj-surgeon.analyze-test :fast
   'clj-surgeon.cli-dispatch-test :battery
   'clj-surgeon.cljc-existing-ops-test :fast
   'clj-surgeon.cljc.analyze-test :fast
   'clj-surgeon.cljc.merge-test :fast
   'clj-surgeon.cljc.require-ops-test :fast
   'clj-surgeon.cljc.split-test :fast
   'clj-surgeon.diagnostic-delta-test :fast
   'clj-surgeon.edit-dsl-test :fast
   'clj-surgeon.edit-test :battery
   'clj-surgeon.edn-config-integration-test :battery
   'clj-surgeon.extract-header-test :fast
   'clj-surgeon.extract-test :battery
   'clj-surgeon.failure-report-test :fast
   'clj-surgeon.file-ops-test :fast
   'clj-surgeon.fix-declares-test :fast
   'clj-surgeon.forms-test :fast
   'clj-surgeon.help-test :battery
   'clj-surgeon.insertion-gap-test :fast
   'clj-surgeon.install-test :battery
   'clj-surgeon.intent-transaction-test :battery
   'clj-surgeon.jvm-error-test :fast
   'clj-surgeon.lens-query-test :battery
   'clj-surgeon.ls-tree-test :fast
   'clj-surgeon.memory-battery-test :fast
   'clj-surgeon.move-dependency-test :fast
   'clj-surgeon.move-test :fast
   'clj-surgeon.operation-algebra-test :fast
   'clj-surgeon.outermost-test :fast
   'clj-surgeon.outline-test :fast
   'clj-surgeon.owner-hypotheses-test :fast
   'clj-surgeon.parser-admission-test :battery
   'clj-surgeon.partition-all-test :battery
   'clj-surgeon.platform-selector-test :battery
   'clj-surgeon.recovery-test :fast
   'clj-surgeon.relation-census-test :fast
   'clj-surgeon.rename-test :fast
   'clj-surgeon.show-form-test :battery
   'clj-surgeon.structural-lens-test :fast
   'clj-surgeon.syntax-var-refs-test :fast
   'clj-surgeon.tmp-leak-support-test :battery
   'clj-surgeon.worktree-lifecycle-cli-test :fast
   'clj-surgeon.worktree-lifecycle-io-test :fast
   'clj-surgeon.worktree-lifecycle-test :fast
   'clj-surgeon.xray-test :battery

   ;; ---- existing cadence members ----
   'clj-surgeon.rename-alias-receipt-test :fast
   'clj-surgeon.receipt-booleans-test :fast
   'clj-surgeon.insert-forms-test :fast
   'clj-surgeon.splice-envelope-test :fast
   'clj-surgeon.rename-alias-performance-test :battery
   'clj-surgeon.rename-alias-test :fast
   'clj-surgeon.rename-alias-parity-test :battery
   'clj-surgeon.insert-forms-receipt-test :fast
   'clj-surgeon.insert-forms-parity-test :battery
   'clj-surgeon.mission-usage-test :fast
   'clj-surgeon.mission-git-test :fast
   'clj-surgeon.mission-typist-test                    :fast
   'clj-surgeon.mission-candidate-race-test :fast
   'clj-surgeon.mission-candidate-test                 :fast
   'clj-surgeon.mission-forms-test                     :fast
   'clj-surgeon.mission-forms-source-test :fast
   'clj-surgeon.mission-plain-forms-test :fast
   'clj-surgeon.battery-ledger-test                     :fast
   'clj-surgeon.battery-parallel-test                   :fast
   'clj-surgeon.census-pool-test                        :fast
   'clj-surgeon.fast-lane-isolation-test                :fast
   'clj-surgeon.helper-extraction-test                  :fast
   'clj-surgeon.lane-manifest-test                      :fast
   'clj-surgeon.mcp-change-buffer-test                  :fast
   'clj-surgeon.mcp-combinable-transaction-test         :fast
   'clj-surgeon.mcp-compact-edit-fields-test            :fast
   'clj-surgeon.mcp-compact-edit-test                   :fast
   'clj-surgeon.mcp-compact-location-test               :fast
   'clj-surgeon.mcp-compact-relations-test              :fast
   'clj-surgeon.mcp-contract-test                       :fast
   'clj-surgeon.mcp-create-files-test                   :fast
   'clj-surgeon.mcp-expect-guard-test                   :fast
   'clj-surgeon.mcp-extraction-plan-test                :fast
   'clj-surgeon.mcp-extraction-test                     :fast
   'clj-surgeon.cell-b-oracle-test                      :battery ; B07 invokes Python to mutation-test the shell oracle.
   'clj-surgeon.namespace-split-test                    :fast
   'clj-surgeon.split-proof-gate-test                   :fast
   'clj-surgeon.require-change-test                     :fast
   'clj-surgeon.require-change-boundary-test             :battery
   'clj-surgeon.namespace-split-warm-test               :integration
   'clj-surgeon.mcp-namespace-split-test                 :fast
   'clj-surgeon.mcp-formatter-test                      :fast
   'clj-surgeon.mcp-inspect-contract-test               :fast
   'clj-surgeon.mcp-inspect-tool-test                   :fast
   'clj-surgeon.mcp-intent-contract-test                :fast
   'clj-surgeon.mcp-operation-async-test                :fast
   'clj-surgeon.mcp-operation-registry-test             :fast
   'clj-surgeon.mcp-operation-test                      :fast
   'clj-surgeon.mcp-paths-test                          :fast
   'clj-surgeon.mcp-prepared-confirmation-test          :fast
   'clj-surgeon.mcp-prepared-request-test               :fast
   'clj-surgeon.mcp-program-tool-test                   :fast
   'clj-surgeon.mcp-read-request-normalization-test     :fast
   'clj-surgeon.mcp-recovery-test                       :fast
   'clj-surgeon.mcp-relation-census-round20-test        :fast
   'clj-surgeon.mcp-schema-test                         :fast
   'clj-surgeon.mcp-semantic-client-test                :fast
   'clj-surgeon.mcp-telemetry-test                      :fast
   'clj-surgeon.mcp-workspace-test                      :fast
   'clj-surgeon.mcp-write-refusal-test                  :fast
   'clj-surgeon.ns-isolation-test                       :fast
   'clj-surgeon.outline-differential-test               :fast
   'clj-surgeon.outline-memory-test                     :fast
   'clj-surgeon.quoted-var-refs-test                    :fast
   'clj-surgeon.scope-stream-test                       :fast
   'clj-surgeon.telemetry-events-test                   :fast
   'clj-surgeon.workspace-onboarding-test               :fast

   ;; ---- :integration (6) ----
   'clj-surgeon.outline-corpus-integration-test          :integration
   'clj-surgeon.mcp-feature-thread-test                 :integration
   'clj-surgeon.mcp-hot-verify-test                     :integration
   'clj-surgeon.mcp-server-test                         :integration
   'clj-surgeon.mcp-http-server-test                    :integration
   'clj-surgeon.mcp-tool-test                           :integration

   ;; ---- :battery (32) ----
   'clj-surgeon.mission-run-test                       :battery
   'clj-surgeon.mission-events-test :battery
   'clj-surgeon.mission-phase-events-test :battery
   'clj-surgeon.mission-provider-fallback-events-test :battery
   'clj-surgeon.mission-display-test :battery
   'clj-surgeon.mission-fallback-test :battery
   'clj-surgeon.mission-usage-executor-test :battery
   'clj-surgeon.mission-typist-executor-admission-test :battery
   'clj-surgeon.mission-git-boundary-test :battery
   'clj-surgeon.mission-git-identity-test :battery
   'clj-surgeon.mission-git-submodule-test :battery
   'clj-surgeon.mission-publication-test :battery
   'clj-surgeon.mission-git-fence-test :battery
   'clj-surgeon.mission-git-process-test :battery
   'clj-surgeon.mission-git-ledger-test :battery
   'clj-surgeon.mission-commit-cli-test :battery
   'clj-surgeon.mission-test                           :battery
   'clj-surgeon.mission-typist-executor-test            :battery
   'clj-surgeon.admit-patch-test                        :battery
   'clj-surgeon.core-discovery-test                     :battery
   'clj-surgeon.receipt-artifacts-boundary-test :battery
   'clj-surgeon.split-proof-gate-boundary-test :battery
   'clj-surgeon.mcp-alias-migration-test                :battery
   'clj-surgeon.mcp-cold-verify-test                    :battery
   'clj-surgeon.mcp-feature-thread-sed-test             :battery
   'clj-surgeon.mcp-helper-extraction-test              :battery
   'clj-surgeon.mcp-inspect-cold-job-test               :battery
   'clj-surgeon.mcp-prepared-wire-test                  :battery
   'clj-surgeon.mcp-process-test                        :battery
   'clj-surgeon.mcp-relation-census-launcher-test       :battery
   'clj-surgeon.mcp-relation-census-test                :battery
   'clj-surgeon.reader-eval-fence-test                  :battery
   'clj-surgeon.repository-hygiene-test                 :battery
   'clj-surgeon.txn-journal-test                        :battery})

;; @spec TEST-ISO-001 -- runtime is independent of cadence.
(def portability-runtimes
  '{clj-surgeon.admit-patch-test :jvm, clj-surgeon.agent-routing-test :bb, clj-surgeon.alias-migration-test :bb, clj-surgeon.analyze-test :bb, clj-surgeon.analyzer-contract-test :jvm, clj-surgeon.battery-ledger-test :bb, clj-surgeon.battery-parallel-test :bb, clj-surgeon.cell-b-oracle-test :bb, clj-surgeon.census-pool-test :jvm, clj-surgeon.cli-dispatch-test :bb, clj-surgeon.cljc-existing-ops-test :bb, clj-surgeon.cljc.analyze-test :bb, clj-surgeon.cljc.merge-test :bb, clj-surgeon.cljc.require-ops-test :bb, clj-surgeon.cljc.split-test :bb, clj-surgeon.core-discovery-test :bb, clj-surgeon.diagnostic-delta-test :bb, clj-surgeon.edit-dsl-test :bb, clj-surgeon.edit-test :bb, clj-surgeon.edn-config-integration-test :bb, clj-surgeon.extract-header-test :bb, clj-surgeon.extract-test :bb, clj-surgeon.failure-report-test :bb, clj-surgeon.fast-lane-isolation-test :bb, clj-surgeon.file-ops-test :bb, clj-surgeon.fix-declares-test :bb, clj-surgeon.forms-test :bb, clj-surgeon.help-test :bb, clj-surgeon.helper-extraction-test :bb, clj-surgeon.insert-forms-parity-test :bb, clj-surgeon.insert-forms-receipt-test :jvm, clj-surgeon.insert-forms-test :bb, clj-surgeon.insertion-gap-test :bb, clj-surgeon.install-test :bb, clj-surgeon.intent-transaction-test :bb, clj-surgeon.jvm-error-test :bb, clj-surgeon.lane-manifest-test :bb, clj-surgeon.lens-query-test :bb, clj-surgeon.ls-tree-test :bb, clj-surgeon.mcp-alias-migration-test :jvm, clj-surgeon.mcp-change-buffer-test :jvm, clj-surgeon.mcp-cold-verify-test :bb, clj-surgeon.mcp-combinable-transaction-test :jvm, clj-surgeon.mcp-compact-edit-fields-test :bb, clj-surgeon.mcp-compact-edit-test :jvm, clj-surgeon.mcp-compact-location-test :jvm, clj-surgeon.mcp-compact-relations-test :jvm, clj-surgeon.mcp-contract-test :bb, clj-surgeon.mcp-create-files-test :jvm, clj-surgeon.mcp-expect-guard-test :jvm, clj-surgeon.mcp-extraction-plan-test :jvm, clj-surgeon.mcp-extraction-test :bb, clj-surgeon.mcp-feature-thread-sed-test :bb, clj-surgeon.mcp-feature-thread-test :bb, clj-surgeon.mcp-formatter-test :jvm, clj-surgeon.mcp-helper-extraction-test :jvm, clj-surgeon.mcp-hot-verify-test :jvm, clj-surgeon.mcp-http-server-test :jvm, clj-surgeon.mcp-inspect-cold-job-test :jvm, clj-surgeon.mcp-inspect-contract-test :bb, clj-surgeon.mcp-inspect-tool-test :jvm, clj-surgeon.mcp-intent-contract-test :bb, clj-surgeon.mcp-namespace-split-test :jvm, clj-surgeon.mcp-operation-async-test :jvm, clj-surgeon.mcp-operation-registry-test :jvm, clj-surgeon.mcp-operation-test :jvm, clj-surgeon.mcp-paths-test :bb, clj-surgeon.mcp-prepared-confirmation-test :jvm, clj-surgeon.mcp-prepared-request-test :jvm, clj-surgeon.mcp-prepared-wire-test :jvm, clj-surgeon.mcp-process-test :bb, clj-surgeon.mcp-program-tool-test :bb, clj-surgeon.mcp-read-request-normalization-test :bb, clj-surgeon.mcp-recovery-test :bb, clj-surgeon.mcp-relation-census-launcher-test :bb, clj-surgeon.mcp-relation-census-round20-test :jvm, clj-surgeon.mcp-relation-census-test :jvm, clj-surgeon.mcp-schema-test :bb, clj-surgeon.mcp-semantic-client-test :jvm, clj-surgeon.mcp-server-test :jvm, clj-surgeon.mcp-telemetry-test :bb, clj-surgeon.mcp-tool-test :jvm, clj-surgeon.mcp-workspace-test :bb, clj-surgeon.mcp-write-refusal-test :jvm, clj-surgeon.memory-battery-test :bb, clj-surgeon.memory.journal-green-test :bb, clj-surgeon.memory.oom-reproduction-test :bb, clj-surgeon.mission-candidate-race-test :jvm, clj-surgeon.mission-candidate-test :bb, clj-surgeon.mission-commit-cli-test :jvm, clj-surgeon.mission-display-test :bb, clj-surgeon.mission-events-test :jvm, clj-surgeon.mission-fallback-test :jvm, clj-surgeon.mission-forms-source-test :bb, clj-surgeon.mission-forms-test :bb, clj-surgeon.mission-git-boundary-test :bb, clj-surgeon.mission-git-fence-test :bb, clj-surgeon.mission-git-identity-test :bb, clj-surgeon.mission-git-ledger-test :jvm, clj-surgeon.mission-git-process-test :bb, clj-surgeon.mission-git-submodule-test :bb, clj-surgeon.mission-git-test :bb, clj-surgeon.mission-phase-events-test :jvm, clj-surgeon.mission-plain-forms-test :bb, clj-surgeon.mission-provider-fallback-events-test :jvm, clj-surgeon.mission-publication-test :jvm, clj-surgeon.mission-run-test :jvm, clj-surgeon.mission-test :jvm, clj-surgeon.mission-typist-executor-admission-test :jvm, clj-surgeon.mission-typist-executor-test :jvm, clj-surgeon.mission-typist-test :bb, clj-surgeon.mission-usage-executor-test :jvm, clj-surgeon.mission-usage-test :bb, clj-surgeon.move-dependency-test :bb, clj-surgeon.move-test :bb, clj-surgeon.namespace-split-test :bb, clj-surgeon.namespace-split-warm-test :jvm, clj-surgeon.ns-isolation-test :jvm, clj-surgeon.operation-algebra-test :bb, clj-surgeon.outermost-test :bb, clj-surgeon.outline-corpus-integration-test :bb, clj-surgeon.outline-differential-test :bb, clj-surgeon.outline-memory-test :jvm, clj-surgeon.outline-test :bb, clj-surgeon.owner-hypotheses-test :bb, clj-surgeon.parser-admission-test :bb, clj-surgeon.partition-all-test :bb, clj-surgeon.platform-selector-test :bb, clj-surgeon.quoted-var-refs-test :bb, clj-surgeon.reader-eval-fence-test :bb, clj-surgeon.receipt-artifacts-boundary-test :jvm, clj-surgeon.receipt-booleans-test :jvm, clj-surgeon.recovery-test :bb, clj-surgeon.relation-census-test :bb, clj-surgeon.rename-alias-parity-test :bb, clj-surgeon.rename-alias-performance-test :bb, clj-surgeon.rename-alias-receipt-test :bb, clj-surgeon.rename-alias-test :bb, clj-surgeon.rename-test :bb, clj-surgeon.repository-hygiene-test :bb, clj-surgeon.require-change-boundary-test :bb, clj-surgeon.require-change-test :bb, clj-surgeon.scope-stream-test :jvm, clj-surgeon.show-form-test :bb, clj-surgeon.splice-envelope-test :bb, clj-surgeon.split-proof-gate-boundary-test :bb, clj-surgeon.split-proof-gate-test :bb, clj-surgeon.structural-lens-test :bb, clj-surgeon.syntax-var-refs-test :bb, clj-surgeon.telemetry-events-test :bb, clj-surgeon.tmp-leak-support-test :bb, clj-surgeon.txn-journal-test :jvm, clj-surgeon.workspace-onboarding-test :bb, clj-surgeon.worktree-lifecycle-cli-test :bb, clj-surgeon.worktree-lifecycle-io-test :bb, clj-surgeon.worktree-lifecycle-prune-test :bb, clj-surgeon.worktree-lifecycle-recovery-test :bb, clj-surgeon.worktree-lifecycle-test :bb, clj-surgeon.xray-test :bb})

;; @spec TEST-ISO-016 -- paired namespace walls on the same box, not startup.
(def runtime-measurements
  (let [rows '{clj-surgeon.battery-ledger-test
               {:n 6
                :jvm
                {:n 6
                 :walls-ms [40 38 40 36 39 36]
                 :mean-ms 38.166666666666664
                 :sd-ms 1.834847859269718}
                :bb
                {:n 6
                 :walls-ms [6 6 6 8 6 6]
                 :mean-ms 6.333333333333333
                 :sd-ms 0.8164965809277259}
                :conservative-ratio 0.23092828953497613
                :runtime :bb
                :previous-runtime :bb}
               clj-surgeon.battery-parallel-test
               {:n 6
                :jvm
                {:n 6
                 :walls-ms [967 967 973 1016 1014 965]
                 :mean-ms 983.6666666666666
                 :sd-ms 24.426761280748348}
                :bb
                {:n 6
                 :walls-ms [34 39 35 34 34 35]
                 :mean-ms 35.166666666666664
                 :sd-ms 1.9407902170679519}
                :conservative-ratio 0.04177117892173059
                :runtime :bb
                :previous-runtime :bb}
               clj-surgeon.fast-lane-isolation-test
               {:n 6
                :jvm
                {:n 6
                 :walls-ms [129 136 127 132 128 130]
                 :mean-ms 130.33333333333334
                 :sd-ms 3.2659863237109037}
                :bb
                {:n 6
                 :walls-ms [46 46 46 46 49 46]
                 :mean-ms 46.5
                 :sd-ms 1.224744871391589}
                :conservative-ratio 0.39538733234903434
                :runtime :bb
                :previous-runtime :bb}
               clj-surgeon.helper-extraction-test
               {:n 6
                :jvm
                {:n 6
                 :walls-ms [785 823 811 765 845 843]
                 :mean-ms 812.0
                 :sd-ms 31.96873472629156}
                :bb
                {:n 6
                 :walls-ms [815 834 818 792 837 812]
                 :mean-ms 818.0
                 :sd-ms 16.358484037342826}
                :conservative-ratio 1.1372270810732203
                :runtime :bb
                :previous-runtime :bb}
               clj-surgeon.insert-forms-test
               {:n 6
                :jvm
                {:n 6
                 :walls-ms [1339 1324 1362 1441 1445 1407]
                 :mean-ms 1386.3333333333333
                 :sd-ms 52.10630160226944}
                :bb
                {:n 6
                 :walls-ms [5192 5318 5234 5166 5016 5126]
                 :mean-ms 5175.333333333333
                 :sd-ms 101.96208445626573}
                :conservative-ratio 4.195593578543493
                :runtime :jvm
                :previous-runtime :jvm}
               clj-surgeon.intent-transaction-test
               {:n 6
                :jvm
                {:n 6
                 :walls-ms [5046 4981 4855 4922 5011 4973]
                 :mean-ms 4964.666666666667
                 :sd-ms 67.74265027784686}
                :bb
                {:n 6
                 :walls-ms [5069 5095 5104 5025 5051 5160]
                 :mean-ms 5084.0
                 :sd-ms 47.09989384276784}
                :conservative-ratio 1.0722727922425561
                :runtime :jvm
                :previous-runtime :jvm
                :contract-failure
                "Shared CLI workspace_status.unexpected_paths is unbounded; attempt8/owed.md. Runtime comparison found no bb-only leak."}
               clj-surgeon.lane-manifest-test
               {:n 6
                :jvm
                {:n 6
                 :walls-ms [1165 1293 1237 1326 1212 1225]
                 :mean-ms 1243.0
                 :sd-ms 57.9551550770076}
                :bb
                {:n 6
                 :walls-ms [1311 1306 1304 1285 1282 1348]
                 :mean-ms 1306.0
                 :sd-ms 23.706539182259394}
                :conservative-ratio 1.200803352703422
                :runtime :bb
                :previous-runtime :bb}
               clj-surgeon.mcp-compact-edit-fields-test
               {:n 6
                :jvm
                {:n 6
                 :walls-ms [33 35 29 30 32 28]
                 :mean-ms 31.166666666666668
                 :sd-ms 2.6394443859772205}
                :bb
                {:n 6
                 :walls-ms [7 6 6 8 6 7]
                 :mean-ms 6.666666666666667
                 :sd-ms 0.816496580927726}
                :conservative-ratio 0.32060147697023417
                :runtime :bb
                :previous-runtime :bb}
               clj-surgeon.mcp-contract-test
               {:n 6
                :jvm
                {:n 6
                 :walls-ms [71 81 73 78 71 69]
                 :mean-ms 73.83333333333333
                 :sd-ms 4.665476038590988}
                :bb
                {:n 6
                 :walls-ms [12 12 12 14 14 14]
                 :mean-ms 13.0
                 :sd-ms 1.0954451150103321}
                :conservative-ratio 0.2355089832993098
                :runtime :bb
                :previous-runtime :bb}
               clj-surgeon.mcp-extraction-test
               {:n 6
                :jvm
                {:n 6
                 :walls-ms [130 143 145 138 135 133]
                 :mean-ms 137.33333333333334
                 :sd-ms 5.819507424745384}
                :bb
                {:n 6
                 :walls-ms [49 51 49 48 48 48]
                 :mean-ms 48.833333333333336
                 :sd-ms 1.169045194450012}
                :conservative-ratio 0.4071100773644849
                :runtime :bb
                :previous-runtime :bb}
               clj-surgeon.mcp-feature-thread-test
               {:n 6
                :jvm
                {:n 6
                 :walls-ms [42963 42677 42596 42800 42621 42999]
                 :mean-ms 42776.0
                 :sd-ms 174.0689518552921}
                :bb
                {:n 6
                 :walls-ms [725420 726104 725937 725790 725346 726826]
                 :mean-ms 725903.8333333334
                 :sd-ms 538.4772666201115}
                :conservative-ratio 17.13451378286988
                :runtime :jvm
                :previous-runtime :jvm}
               clj-surgeon.mcp-inspect-contract-test
               {:n 6
                :jvm
                {:n 6
                 :walls-ms [321 323 296 297 308 311]
                 :mean-ms 309.3333333333333
                 :sd-ms 11.465891446663301}
                :bb
                {:n 6
                 :walls-ms [313 290 319 299 322 303]
                 :mean-ms 307.6666666666667
                 :sd-ms 12.420413304985734}
                :conservative-ratio 1.1609835657865595
                :runtime :bb
                :previous-runtime :bb}
               clj-surgeon.mcp-intent-contract-test
               {:n 6
                :jvm
                {:n 6
                 :walls-ms [320 305 301 290 286 279]
                 :mean-ms 296.8333333333333
                 :sd-ms 14.851487018701754}
                :bb
                {:n 6
                 :walls-ms [249 246 251 255 269 243]
                 :mean-ms 252.16666666666666
                 :sd-ms 9.217736526212207}
                :conservative-ratio 1.0129965775223444
                :runtime :bb
                :previous-runtime :bb}
               clj-surgeon.mcp-paths-test
               {:n 6
                :jvm
                {:n 6
                 :walls-ms [18 14 15 16 15 15]
                 :mean-ms 15.5
                 :sd-ms 1.378404875209022}
                :bb {:n 6, :walls-ms [2 2 2 2 2 2], :mean-ms 2.0, :sd-ms 0.0}
                :conservative-ratio 0.1569465699584616
                :runtime :bb
                :previous-runtime :bb}
               clj-surgeon.mcp-program-tool-test
               {:n 6
                :jvm
                {:n 6
                 :walls-ms [139 171 146 164 146 133]
                 :mean-ms 149.83333333333334
                 :sd-ms 14.688998150543375}
                :bb
                {:n 6
                 :walls-ms [18 22 19 20 18 20]
                 :mean-ms 19.5
                 :sd-ms 1.51657508881031}
                :conservative-ratio 0.18706643252833507
                :runtime :bb
                :previous-runtime :bb}
               clj-surgeon.mcp-read-request-normalization-test
               {:n 6
                :jvm
                {:n 6
                 :walls-ms [33 28 29 34 28 28]
                 :mean-ms 30.0
                 :sd-ms 2.756809750418044}
                :bb
                {:n 6
                 :walls-ms [3 3 3 4 3 5]
                 :mean-ms 3.5
                 :sd-ms 0.8366600265340756}
                :conservative-ratio 0.21127336697413465
                :runtime :bb
                :previous-runtime :bb}
               clj-surgeon.mcp-recovery-test
               {:n 6
                :jvm
                {:n 6
                 :walls-ms [174 169 165 162 162 159]
                 :mean-ms 165.16666666666666
                 :sd-ms 5.49241901776136}
                :bb
                {:n 6
                 :walls-ms [6 6 6 6 7 6]
                 :mean-ms 6.166666666666667
                 :sd-ms 0.408248290463863}
                :conservative-ratio 0.04529173969197451
                :runtime :bb
                :previous-runtime :bb}
               clj-surgeon.mcp-schema-test
               {:n 6
                :jvm
                {:n 6
                 :walls-ms [33 32 34 33 31 35]
                 :mean-ms 33.0
                 :sd-ms 1.4142135623730951}
                :bb
                {:n 6
                 :walls-ms [9 10 9 10 10 10]
                 :mean-ms 9.666666666666666
                 :sd-ms 0.5163977794943223}
                :conservative-ratio 0.35462063147628675
                :runtime :bb
                :previous-runtime :bb}
               clj-surgeon.mcp-telemetry-test
               {:n 6
                :jvm
                {:n 6
                 :walls-ms [36 39 37 37 39 37]
                 :mean-ms 37.5
                 :sd-ms 1.224744871391589}
                :bb
                {:n 6
                 :walls-ms [6 6 7 6 7 6]
                 :mean-ms 6.333333333333333
                 :sd-ms 0.5163977794943223}
                :conservative-ratio 0.21015753660263214
                :runtime :bb
                :previous-runtime :bb}
               clj-surgeon.mcp-workspace-test
               {:n 6
                :jvm
                {:n 6
                 :walls-ms [33 28 31 29 30 28]
                 :mean-ms 29.833333333333332
                 :sd-ms 1.9407902170679516}
                :bb
                {:n 6
                 :walls-ms [16 14 18 18 16 15]
                 :mean-ms 16.166666666666668
                 :sd-ms 1.6020819787597222}
                :conservative-ratio 0.7464170416320959
                :runtime :bb
                :previous-runtime :bb}
               clj-surgeon.mission-candidate-test
               {:n 6
                :jvm
                {:n 6
                 :walls-ms [17 17 20 19 18 16]
                 :mean-ms 17.833333333333332
                 :sd-ms 1.4719601443879746}
                :bb {:n 6, :walls-ms [3 3 3 3 3 3], :mean-ms 3.0, :sd-ms 0.0}
                :conservative-ratio 0.20148544412209776
                :runtime :bb
                :previous-runtime :bb}
               clj-surgeon.mission-forms-source-test
               {:n 6
                :jvm
                {:n 6
                 :walls-ms [348 346 382 389 370 334]
                 :mean-ms 361.5
                 :sd-ms 22.034064536530703}
                :bb
                {:n 6
                 :walls-ms [32 29 28 27 28 30]
                 :mean-ms 29.0
                 :sd-ms 1.7888543819998317}
                :conservative-ratio 0.10262897883841628
                :runtime :bb
                :previous-runtime :bb}
               clj-surgeon.mission-forms-test
               {:n 6
                :jvm
                {:n 6
                 :walls-ms [39 37 37 39 40 39]
                 :mean-ms 38.5
                 :sd-ms 1.224744871391589}
                :bb {:n 6, :walls-ms [6 6 6 6 6 6], :mean-ms 6.0, :sd-ms 0.0}
                :conservative-ratio 0.16643315052104932
                :runtime :bb
                :previous-runtime :bb}
               clj-surgeon.mission-git-test
               {:n 6
                :jvm
                {:n 6
                 :walls-ms [9 14 9 10 10 9]
                 :mean-ms 10.166666666666666
                 :sd-ms 1.9407902170679516}
                :bb {:n 6, :walls-ms [1 1 1 1 1 1], :mean-ms 1.0, :sd-ms 0.0}
                :conservative-ratio 0.1591068066535244
                :runtime :bb
                :previous-runtime :bb}
               clj-surgeon.mission-plain-forms-test
               {:n 6
                :jvm
                {:n 6
                 :walls-ms [81 94 84 92 87 92]
                 :mean-ms 88.33333333333333
                 :sd-ms 5.163977794943222}
                :bb
                {:n 6
                 :walls-ms [44 42 44 43 47 45]
                 :mean-ms 44.166666666666664
                 :sd-ms 1.7224014243685084}
                :conservative-ratio 0.6103613736990519
                :runtime :bb
                :previous-runtime :bb}
               clj-surgeon.mission-typist-test
               {:n 6
                :jvm
                {:n 6
                 :walls-ms [52 55 47 57 56 51]
                 :mean-ms 53.0
                 :sd-ms 3.7416573867739413}
                :bb
                {:n 6
                 :walls-ms [25 20 21 21 21 20]
                 :mean-ms 21.333333333333332
                 :sd-ms 1.8618986725025257}
                :conservative-ratio 0.5505042942752865
                :runtime :bb
                :previous-runtime :bb}
               clj-surgeon.mission-usage-test
               {:n 6
                :jvm
                {:n 6
                 :walls-ms [58 50 53 61 55 52]
                 :mean-ms 54.833333333333336
                 :sd-ms 4.070217029430577}
                :bb {:n 6, :walls-ms [14 14 14 14 14 14], :mean-ms 14.0, :sd-ms 0.0}
                :conservative-ratio 0.29983145654984084
                :runtime :bb
                :previous-runtime :bb}
               clj-surgeon.namespace-split-test
               {:n 6
                :jvm
                {:n 6
                 :walls-ms [1149 1078 1182 1187 1131 1068]
                 :mean-ms 1132.5
                 :sd-ms 50.64681628690988}
                :bb
                {:n 6
                 :walls-ms [752 751 752 755 761 757]
                 :mean-ms 754.6666666666666
                 :sd-ms 3.8297084310253524}
                :conservative-ratio 0.7392565713412248
                :runtime :bb
                :previous-runtime :bb}
               clj-surgeon.outline-corpus-integration-test
               {:n 6
                :jvm
                {:n 6
                 :walls-ms [7952 8025 7936 8240 7817 8254]
                 :mean-ms 8037.333333333333
                 :sd-ms 175.65154900161474}
                :bb
                {:n 6
                 :walls-ms [20724 20726 20889 20909 20981 20673]
                 :mean-ms 20817.0
                 :sd-ms 125.06798151405499}
                :conservative-ratio 2.7409644924618632
                :runtime :jvm
                :previous-runtime :jvm}
               clj-surgeon.outline-differential-test
               {:n 6
                :jvm
                {:n 6
                 :walls-ms [54 52 52 50 63 55]
                 :mean-ms 54.333333333333336
                 :sd-ms 4.589843860815601}
                :bb
                {:n 6
                 :walls-ms [16 16 17 16 16 16]
                 :mean-ms 16.166666666666668
                 :sd-ms 0.40824829046386296}
                :conservative-ratio 0.37611942551971916
                :runtime :bb
                :previous-runtime :bb}
               clj-surgeon.quoted-var-refs-test
               {:n 6
                :jvm
                {:n 6
                 :walls-ms [80 89 88 97 88 91]
                 :mean-ms 88.83333333333333
                 :sd-ms 5.49241901776136}
                :bb
                {:n 6
                 :walls-ms [11 11 10 10 10 11]
                 :mean-ms 10.5
                 :sd-ms 0.5477225575051661}
                :conservative-ratio 0.1489488662645537
                :runtime :bb
                :previous-runtime :bb}
               clj-surgeon.rename-alias-receipt-test
               {:n 6
                :jvm
                {:n 6
                 :walls-ms [489 461 475 534 504 516]
                 :mean-ms 496.5
                 :sd-ms 26.912822222873615}
                :bb
                {:n 6
                 :walls-ms [1797 1823 1762 1789 1849 1795]
                 :mean-ms 1802.5
                 :sd-ms 29.971653274385783}
                :conservative-ratio 4.207253668934333
                :runtime :jvm
                :previous-runtime :jvm}
               clj-surgeon.rename-alias-test
               {:n 6
                :jvm
                {:n 6
                 :walls-ms [3299 3121 3025 3332 3377 3064]
                 :mean-ms 3203.0
                 :sd-ms 150.9052682976973}
                :bb
                {:n 6
                 :walls-ms [6163 6188 6348 6285 6331 6277]
                 :mean-ms 6265.333333333333
                 :sd-ms 74.99511095175916}
                :conservative-ratio 2.211273560778874
                :runtime :jvm
                :previous-runtime :bb}
               clj-surgeon.require-change-test
               {:n 6
                :jvm
                {:n 6
                 :walls-ms [154 156 163 147 151 164]
                 :mean-ms 155.83333333333334
                 :sd-ms 6.6758270399005}
                :bb
                {:n 6
                 :walls-ms [43 42 44 42 47 43]
                 :mean-ms 43.5
                 :sd-ms 1.8708286933869707}
                :conservative-ratio 0.331563030659626
                :runtime :bb
                :previous-runtime :bb}
               clj-surgeon.splice-envelope-test
               {:n 6
                :jvm
                {:n 6
                 :walls-ms [768 760 734 742 761 752]
                 :mean-ms 752.8333333333334
                 :sd-ms 12.812754062521714}
                :bb
                {:n 6
                 :walls-ms [27564 27707 27807 27528 26986 27871]
                 :mean-ms 27577.166666666668
                 :sd-ms 318.77478988569135}
                :conservative-ratio 38.798697247731994
                :runtime :jvm
                :previous-runtime :jvm}
               clj-surgeon.split-proof-gate-test
               {:n 6
                :jvm
                {:n 6
                 :walls-ms [32 30 32 27 28 24]
                 :mean-ms 28.833333333333332
                 :sd-ms 3.125166662222459}
                :bb
                {:n 6
                 :walls-ms [7 6 6 10 6 6]
                 :mean-ms 6.833333333333333
                 :sd-ms 1.6020819787597222}
                :conservative-ratio 0.4444713849755184
                :runtime :bb
                :previous-runtime :bb}
               clj-surgeon.telemetry-events-test
               {:n 6
                :jvm
                {:n 6
                 :walls-ms [174 149 145 151 139 148]
                 :mean-ms 151.0
                 :sd-ms 12.016655108639842}
                :bb
                {:n 6
                 :walls-ms [55 55 55 53 60 54]
                 :mean-ms 55.333333333333336
                 :sd-ms 2.4221202832779936}
                :conservative-ratio 0.47396347816007456
                :runtime :bb
                :previous-runtime :bb}
               clj-surgeon.workspace-onboarding-test
               {:n 6
                :jvm
                {:n 6
                 :walls-ms [164 174 163 169 174 161]
                 :mean-ms 167.5
                 :sd-ms 5.683308895353129}
                :bb
                {:n 6
                 :walls-ms [112 114 113 112 112 113]
                 :mean-ms 112.66666666666667
                 :sd-ms 0.816496580927726}
                :conservative-ratio 0.7320642018457376
                :runtime :bb
                :previous-runtime :bb}}]
    (into {}
          (map (fn [[n row]]
                 [n (reduce (fn [m runtime]
                              (assoc-in m [runtime :logs]
                                (mapv #(str "docs/observations/2026-09-12-bbtower-block-b/attempt20/measurements/" n "-" (name runtime) "-" % ".edn")
                                      (range 1 7))))
                            row [:jvm :bb])]))
          rows)))

;; @spec TEST-ISO-016
(defn measured-runtime [portable-runtime measurement]
  (if (and (= :bb portable-runtime)
           (or (nil? measurement)
               (and (not (:contract-failure measurement))
                    (empty? (:failed-samples measurement))
                    (<= 6 (:n measurement 0))
                    (every? #(and (<= 6 (:n % 0)) (<= 6 (count (:walls-ms %))))
                            [(:jvm measurement) (:bb measurement)])
                    (let [{:keys [jvm bb]} measurement]
                      (<= (/ (+ (:mean-ms bb) (* 2 (:sd-ms bb)))
                             (max 1.0 (- (:mean-ms jvm) (* 2 (:sd-ms jvm)))))
                          2.0)))))
    :bb
    :jvm))

(def namespace-runtimes
  (into {} (map (fn [[n runtime]]
                  [n (measured-runtime runtime (get runtime-measurements n))]))
        portability-runtimes))

(def unmeasured-runtimes
  (apply dissoc namespace-runtimes (keys runtime-measurements)))

(def excluded
  "Test namespaces that are on disk and in NO JVM lane, each with the reason
   it is not. An entry here is a DECLARED omission; anything else on disk
   that is in neither `manifest` nor `test/run_all.clj` fails the witness.

   An exclusion is a REDIRECTION, not a declaration of orphanhood: its reason
   must name a `make <target>` or a :clj-surgeon/<alias> that ACTUALLY RUNS
   the namespace. `clj-surgeon.runner-membership` resolves the named runner to
   the concrete namespace set it executes -- a Makefile rule to its
   prerequisites, its `$(MAKE)` sub-targets and its `-M:clj-surgeon/<alias>`,
   and that alias to its `:main-opts` and so to the lane manifest -- and
   `lane-manifest-test/every-exclusion-is-actually-run-by-the-runner-it-names`
   fails by name when the namespace is not IN that set. It fails CLOSED: a
   runner whose selection cannot be read is a refusal, never an assumption.

   Round two excluded `mcp-formatter-test` with the reason \"required by no
   runner\"; round three adopted it into :fast. Round three then checked only
   that a NAMED TARGET EXISTED, and the round-three landing review's finding
   4 walked through it with the reason \"`make test-fast`\" on a namespace
   test-fast does not run. Existence is a spelling; membership is the fact."
  {'clj-surgeon.analyzer-contract-test
   "own serialized runner -- `make analyzer-contract-test` (alias :clj-surgeon/analyzer-contract-test)"

   'clj-surgeon.memory.journal-green-test
   "transaction-kernel memory witness -- `make memory-red-kernel`, exclusive suite.lock"

   'clj-surgeon.memory.oom-reproduction-test
   "transaction-kernel memory witness -- `make memory-red-kernel`, exclusive suite.lock"

   'clj-surgeon.worktree-lifecycle-prune-test
   "own Make target -- `make worktree-lifecycle-test` (Makefile:824)"

   'clj-surgeon.worktree-lifecycle-recovery-test
   "own Make target -- `make worktree-lifecycle-recovery-test` (Makefile:834)"})

(defn cadence-of-lane
  "The declared cadence for `lane`, or nil when the lane declares none."
  [lane]
  (get lane-cadence lane))

(defn lane-of
  "The declared lane for `ns-sym`, or nil when it is not in the manifest."
  [ns-sym]
  (get manifest ns-sym))

(defn namespaces-for
  "The manifest's namespaces for `lane`, in manifest order."
  [lane]
  (->> manifest (filter (comp #{lane} val)) (map key) sort vec))

(defn cadence-of
  "The cadence at which `ns-sym` runs, via its lane. nil when it has no lane,
   or when its lane declares no cadence."
  [ns-sym]
  (some-> (lane-of ns-sym) cadence-of-lane))

(defn lane-catalogue
  "`lane (cadence)` for every lane, for refusal messages. A refusal that names
   only the legal lanes leaves the reader to guess what choosing one costs."
  []
  (str/join ", "
            (map (fn [lane]
                   (format "%s (%s)" lane (pr-str (cadence-of-lane lane))))
                 lanes)))

(defn refusal-message
  "The typed refusal for a namespace the runner was asked to run that carries
   no lane declaration. Names the subject, the rule, and the remedy."
  [ns-sym]
  (format (str "lane-refused: %s carries no lane declaration. Every JVM test "
               "namespace must appear in clj-surgeon.lane-manifest/manifest "
               "with one of %s AND carry the same {:lane ...} in its own ns "
               "metadata (TEST-ISO-001). The lane you choose decides HOW OFTEN "
               "it runs, which is why the cadence is named beside it here. Add "
               "it to the manifest and to the ns form, or declare why it "
               "belongs to no JVM lane in clj-surgeon.lane-manifest/excluded.")
          ns-sym (lane-catalogue)))
