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
  {;; ---- :fast (49) ----
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
(def namespace-runtimes
  '{clj-surgeon.admit-patch-test :jvm, clj-surgeon.agent-routing-test :bb, clj-surgeon.alias-migration-test :bb, clj-surgeon.analyze-test :bb, clj-surgeon.analyzer-contract-test :jvm, clj-surgeon.battery-ledger-test :bb, clj-surgeon.battery-parallel-test :bb, clj-surgeon.cell-b-oracle-test :bb, clj-surgeon.census-pool-test :jvm, clj-surgeon.cli-dispatch-test :bb, clj-surgeon.cljc-existing-ops-test :bb, clj-surgeon.cljc.analyze-test :bb, clj-surgeon.cljc.merge-test :bb, clj-surgeon.cljc.require-ops-test :bb, clj-surgeon.cljc.split-test :bb, clj-surgeon.core-discovery-test :bb, clj-surgeon.diagnostic-delta-test :bb, clj-surgeon.edit-dsl-test :bb, clj-surgeon.edit-test :bb, clj-surgeon.edn-config-integration-test :bb, clj-surgeon.extract-header-test :bb, clj-surgeon.extract-test :bb, clj-surgeon.failure-report-test :bb, clj-surgeon.fast-lane-isolation-test :bb, clj-surgeon.file-ops-test :bb, clj-surgeon.fix-declares-test :bb, clj-surgeon.forms-test :bb, clj-surgeon.help-test :bb, clj-surgeon.helper-extraction-test :bb, clj-surgeon.insert-forms-parity-test :bb, clj-surgeon.insert-forms-receipt-test :jvm, clj-surgeon.insert-forms-test :bb, clj-surgeon.insertion-gap-test :bb, clj-surgeon.install-test :bb, clj-surgeon.intent-transaction-test :bb, clj-surgeon.jvm-error-test :bb, clj-surgeon.lane-manifest-test :bb, clj-surgeon.lens-query-test :bb, clj-surgeon.ls-tree-test :bb, clj-surgeon.mcp-alias-migration-test :jvm, clj-surgeon.mcp-change-buffer-test :jvm, clj-surgeon.mcp-cold-verify-test :bb, clj-surgeon.mcp-combinable-transaction-test :jvm, clj-surgeon.mcp-compact-edit-fields-test :bb, clj-surgeon.mcp-compact-edit-test :jvm, clj-surgeon.mcp-compact-location-test :jvm, clj-surgeon.mcp-compact-relations-test :jvm, clj-surgeon.mcp-contract-test :bb, clj-surgeon.mcp-create-files-test :jvm, clj-surgeon.mcp-expect-guard-test :jvm, clj-surgeon.mcp-extraction-plan-test :jvm, clj-surgeon.mcp-extraction-test :bb, clj-surgeon.mcp-feature-thread-sed-test :bb, clj-surgeon.mcp-feature-thread-test :bb, clj-surgeon.mcp-formatter-test :jvm, clj-surgeon.mcp-helper-extraction-test :jvm, clj-surgeon.mcp-hot-verify-test :jvm, clj-surgeon.mcp-http-server-test :jvm, clj-surgeon.mcp-inspect-cold-job-test :jvm, clj-surgeon.mcp-inspect-contract-test :bb, clj-surgeon.mcp-inspect-tool-test :jvm, clj-surgeon.mcp-intent-contract-test :bb, clj-surgeon.mcp-namespace-split-test :jvm, clj-surgeon.mcp-operation-async-test :jvm, clj-surgeon.mcp-operation-registry-test :jvm, clj-surgeon.mcp-operation-test :jvm, clj-surgeon.mcp-paths-test :bb, clj-surgeon.mcp-prepared-confirmation-test :jvm, clj-surgeon.mcp-prepared-request-test :jvm, clj-surgeon.mcp-prepared-wire-test :jvm, clj-surgeon.mcp-process-test :bb, clj-surgeon.mcp-program-tool-test :bb, clj-surgeon.mcp-read-request-normalization-test :bb, clj-surgeon.mcp-recovery-test :bb, clj-surgeon.mcp-relation-census-launcher-test :bb, clj-surgeon.mcp-relation-census-round20-test :jvm, clj-surgeon.mcp-relation-census-test :jvm, clj-surgeon.mcp-schema-test :bb, clj-surgeon.mcp-semantic-client-test :jvm, clj-surgeon.mcp-server-test :jvm, clj-surgeon.mcp-telemetry-test :bb, clj-surgeon.mcp-tool-test :jvm, clj-surgeon.mcp-workspace-test :bb, clj-surgeon.mcp-write-refusal-test :jvm, clj-surgeon.memory-battery-test :bb, clj-surgeon.memory.journal-green-test :bb, clj-surgeon.memory.oom-reproduction-test :bb, clj-surgeon.mission-candidate-race-test :jvm, clj-surgeon.mission-candidate-test :bb, clj-surgeon.mission-commit-cli-test :jvm, clj-surgeon.mission-display-test :bb, clj-surgeon.mission-events-test :jvm, clj-surgeon.mission-fallback-test :jvm, clj-surgeon.mission-forms-source-test :bb, clj-surgeon.mission-forms-test :bb, clj-surgeon.mission-git-boundary-test :bb, clj-surgeon.mission-git-fence-test :bb, clj-surgeon.mission-git-identity-test :bb, clj-surgeon.mission-git-ledger-test :jvm, clj-surgeon.mission-git-process-test :bb, clj-surgeon.mission-git-submodule-test :bb, clj-surgeon.mission-git-test :bb, clj-surgeon.mission-phase-events-test :jvm, clj-surgeon.mission-plain-forms-test :bb, clj-surgeon.mission-provider-fallback-events-test :jvm, clj-surgeon.mission-publication-test :jvm, clj-surgeon.mission-run-test :jvm, clj-surgeon.mission-test :jvm, clj-surgeon.mission-typist-executor-admission-test :jvm, clj-surgeon.mission-typist-executor-test :jvm, clj-surgeon.mission-typist-test :bb, clj-surgeon.mission-usage-executor-test :jvm, clj-surgeon.mission-usage-test :bb, clj-surgeon.move-dependency-test :bb, clj-surgeon.move-test :bb, clj-surgeon.namespace-split-test :bb, clj-surgeon.namespace-split-warm-test :jvm, clj-surgeon.ns-isolation-test :jvm, clj-surgeon.operation-algebra-test :bb, clj-surgeon.outermost-test :bb, clj-surgeon.outline-corpus-integration-test :bb, clj-surgeon.outline-differential-test :bb, clj-surgeon.outline-memory-test :jvm, clj-surgeon.outline-test :bb, clj-surgeon.owner-hypotheses-test :bb, clj-surgeon.parser-admission-test :bb, clj-surgeon.partition-all-test :bb, clj-surgeon.platform-selector-test :bb, clj-surgeon.quoted-var-refs-test :bb, clj-surgeon.reader-eval-fence-test :bb, clj-surgeon.receipt-artifacts-boundary-test :jvm, clj-surgeon.receipt-booleans-test :jvm, clj-surgeon.recovery-test :bb, clj-surgeon.relation-census-test :bb, clj-surgeon.rename-alias-parity-test :bb, clj-surgeon.rename-alias-performance-test :bb, clj-surgeon.rename-alias-receipt-test :bb, clj-surgeon.rename-alias-test :bb, clj-surgeon.rename-test :bb, clj-surgeon.repository-hygiene-test :bb, clj-surgeon.require-change-boundary-test :bb, clj-surgeon.require-change-test :bb, clj-surgeon.scope-stream-test :jvm, clj-surgeon.show-form-test :bb, clj-surgeon.splice-envelope-test :bb, clj-surgeon.split-proof-gate-boundary-test :bb, clj-surgeon.split-proof-gate-test :bb, clj-surgeon.structural-lens-test :bb, clj-surgeon.syntax-var-refs-test :bb, clj-surgeon.telemetry-events-test :bb, clj-surgeon.tmp-leak-support-test :bb, clj-surgeon.txn-journal-test :jvm, clj-surgeon.workspace-onboarding-test :bb, clj-surgeon.worktree-lifecycle-cli-test :bb, clj-surgeon.worktree-lifecycle-io-test :bb, clj-surgeon.worktree-lifecycle-prune-test :bb, clj-surgeon.worktree-lifecycle-recovery-test :bb, clj-surgeon.worktree-lifecycle-test :bb, clj-surgeon.xray-test :bb})

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
