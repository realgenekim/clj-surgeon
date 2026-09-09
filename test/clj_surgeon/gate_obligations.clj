(ns clj-surgeon.gate-obligations
  "THE OBLIGATION INVENTORY -- what this tree REQUIRES of a landing, derived
   here and never read from a producer's receipt.

   Frame 7 item 1, section 4 of
   `docs/observations/2026-09-09-frame7-protocol-and-consumer-contract.md`:

     \"The consumer derives the obligation set independently from the approved
      repository policy and actual candidate; it then matches receipt
      executions to it. THE PRODUCER'S LIST IS NEVER THE EXPECTED LIST.\"

   That sentence is the whole reason this namespace exists. `make
   print-gate-stages` already prints the seven stage NAMES, and a consumer
   built on it can only ask \"did the receipt name the same seven strings?\".
   Seven right names prove nothing about which namespaces ran, in which
   runtime, under which recipe, against which inputs -- so a receipt that
   names `test-bb` and ran 3 of 49 Babashka namespaces matched. This target
   prints the RESOLVED obligation: the recipe bytes, the runtime, the exact
   selected test identities, the declared exclusions, the required inputs and
   the result predicate, each under a stable id.

   R1..R10 are section 4's own ids and are kept verbatim so a reader can put
   the table and this output side by side. R9 and R10 are deliberately present
   and deliberately NOT dischargeable by a gate receipt: task acceptance and
   publication freshness belong to the consumer and the observer, and an
   inventory that silently omitted them would let a green gate look like a
   complete landing proof.

   `:policy-sha256` covers the obligations as printed with the volatile
   candidate identity removed, so two checkouts of the same tree agree and a
   changed recipe, a changed lane manifest, a renamed namespace or a dropped
   exclusion all move it."
  (:require
   [clj-surgeon.lane-manifest :as lm]
   [clj-surgeon.toolchain-identity :as tc]
   [clojure.java.io :as io]
   [clojure.string :as str]))

;; DEPENDENCY DIRECTION, ON PURPOSE. This namespace does NOT require the gate
;; runner. `obligations` takes the tree's policy -- the stage manifest and the
;; suite->namespace resolver -- as ARGUMENTS, so the runner can require this
;; namespace to stamp its own receipt without a cycle, and so a test can drive
;; the derivation with a substituted policy and watch the inventory move.

;; ---------------------------------------------------------------------------
;; digests and identity -- shared with the receipt writer
;; ---------------------------------------------------------------------------

(def sha256 tc/sha256)

(defn file-digest
  "`{:path :sha256 :bytes}` for `path`, or `{:path :status :absent}`. A missing
   required input is NAMED, never silently dropped: section 4's `:required-inputs`
   are how a consumer notices that the thing a check reads is not there."
  [path]
  (let [f (io/file path)]
    (if (.isFile f)
      {:path path :sha256 (sha256 (slurp f)) :bytes (.length f)}
      {:path path :status :absent})))

;; ---------------------------------------------------------------------------
;; Makefile recipes
;; ---------------------------------------------------------------------------

(def ^:private makefile-path "Makefile")

(defn- makefile-lines []
  (str/split-lines (slurp makefile-path)))

(defn- rule-block
  "The literal rule for `target`: its `target: prereqs` line plus every
   following recipe line (tab-indented) and comment, up to the first line that
   is neither. Returns nil when the tree declares no such target -- an unknown
   recipe REFUSES consumption (section 4), so nil must be visible, not empty."
  [lines target]
  (let [head (re-pattern (str "^" (java.util.regex.Pattern/quote target) ":"))
        idx (first (keep-indexed (fn [i l] (when (re-find head l) i)) lines))]
    (when idx
      (let [body (->> (drop (inc idx) lines)
                      (take-while #(or (str/starts-with? % "\t")
                                       (str/starts-with? % "#"))))]
        (vec (cons (nth lines idx) body))))))

(defn- prerequisites
  "Targets named on the `target:` line after the colon, with make variables and
   conditionals dropped."
  [lines target]
  (when-let [block (rule-block lines target)]
    (-> (or (second (str/split (first block) #":" 2)) "")
        (str/replace #"\$\([^)]*\)" "")
        (str/split #"\s+")
        (->> (remove str/blank?) vec))))

(defn- sub-makes
  "`$(MAKE) --no-print-directory X` invocations inside a recipe. Section 4:
   \"Resolve and pin those targets' transitive recipes too.\""
  [block]
  (->> block
       (mapcat #(re-seq #"\$\(MAKE\)[^\n]*?--no-print-directory\s+([A-Za-z0-9_.-]+)" %))
       (map second)
       vec))

(defn recipe
  "The TRANSITIVE recipe for `target`: its own block plus every prerequisite and
   `$(MAKE)` sub-target reachable from it, in a deterministic order, with one
   digest over the concatenated bytes.

   `:unresolved` names any target reached but not declared in this Makefile.
   A consumer that finds a non-empty `:unresolved` must run the real gate: an
   unknown recipe is not an empty one."
  [target]
  (let [lines (makefile-lines)]
    (loop [queue [target] seen #{} blocks [] unresolved []]
      (if-let [t (first queue)]
        (if (seen t)
          (recur (rest queue) seen blocks unresolved)
          (if-let [block (rule-block lines t)]
            (recur (into (vec (rest queue))
                         (concat (prerequisites lines t) (sub-makes block)))
                   (conj seen t)
                   (conj blocks {:target t :lines block})
                   unresolved)
            (recur (rest queue) (conj seen t) blocks (conj unresolved t))))
        (let [ordered (vec (sort-by :target blocks))]
          {:root target
           :targets (mapv :target ordered)
           :unresolved (vec (sort unresolved))
           :sha256 (sha256 (str/join "\n" (mapcat :lines ordered)))})))))

;; ---------------------------------------------------------------------------
;; the inventory
;; ---------------------------------------------------------------------------

(def ^:private jvm-worker-argv
  ["clojure" "-J-Xms64m" "-J-Xmx512m" "-M:clj-surgeon/test-deps"
   "-m" "clj-surgeon.mcp-test-runner" "--emit-edn" "<output>" "--ns" "<selected...>"])

(def ^:private bb-worker-argv
  ["bb" "-Xmx512m" "test/run_all.clj" "--emit-edn" "<output>" "--ns" "<selected...>"])

(defn- ns-names [syms] (mapv str (sort-by str syms)))

(def result-predicate-suite
  "What a suite stage has to have PRODUCED, not merely reported. `:skipped-preconditions 0`
   is R's landing policy (section 4: \"R's landing required-suite skipped-precondition
   policy is zero; declaration does not waive it\")."
  {:exit 0 :failures 0 :errors 0 :isolation-violations 0
   :skipped-preconditions 0 :unexecuted-tests [] :coverage :complete})

(defn obligations
  "R1..R10 for THIS tree. Pure with respect to the working copy: it reads the
   Makefile, the lane manifest, `test/run_all.clj` and the declared exclusions,
   and it runs nothing.

   `policy` is `{:stage-manifest [...] :suite-namespaces (fn [suite] ...)}` --
   the repository's own answer, injected rather than imported."
  [{:keys [stage-manifest suite-namespaces]}]
  (let [mcp-ns (suite-namespaces "mcp")
        fast-ns (suite-namespaces "fast")
        bb-ns (suite-namespaces "bb")
        alias-ns (suite-namespaces "alias")
        battery-ns (suite-namespaces "battery")
        integration-ns (vec (remove (set fast-ns) mcp-ns))
        stage-targets (vec stage-manifest)]
    [{:id :R1 :name "recovery"
      :stage "admit-transaction-recovery-battery" :kind :before
      :authority-policy "battery-parallel-runner/gate-stage-manifest"
      :runtime {:kind :jvm :mode :cold
                :argv ["java" "-cp" "$(clojure -Spath -A:clj-surgeon/mcp-test)"
                       "clojure.main" "test/admit_transaction_recovery_battery.clj"]}
      :recipe (recipe "admit-transaction-recovery-battery")
      :required-inputs [(file-digest "test/admit_transaction_recovery_battery.clj")
                        (file-digest "deps.edn")]
      :selected-test-identities []
      :scope {:kind :whole-script}
      :exclusions []
      :result-predicate {:exit 0 :arms :all-passed :failed-arms []}
      :discharge {:by :execution-evidence
                  :note (str "Required BEFORE its admit-patch-test consumer. "
                             "The EXISTENCE of target/admit-transaction-recovery-battery-receipt.edn "
                             "is not evidence that it ran on this candidate.")}}

     {:id :R2 :name "freshness"
      :stage "battery-fresh" :kind :before
      :authority-policy "battery-parallel-runner/gate-stage-manifest"
      :runtime {:kind :bb :mode :cold
                :argv ["bb" "test/clj_surgeon/battery_ledger.clj" "check"]}
      :recipe (recipe "battery-fresh")
      :required-inputs [(file-digest "test/clj_surgeon/battery_ledger.clj")
                        (file-digest "docs/observations/battery-ledger.edn")]
      :selected-test-identities []
      :scope {:kind :predicate
              :bounds {:max-age-hours 26 :max-counted-commits-behind 30
                       :must-be-ancestor true}}
      :exclusions []
      :result-predicate {:exit 0 :ledger-readable true :ledger-passing true}
      :discharge {:by :execution-evidence
                  :note (str "Freshness is a fact about the CURRENT clock and the "
                             "actual candidate history. A historical passing battery is "
                             "the policy's evidence, not proof that the battery just ran.")}}

     {:id :R3 :name "alias"
      :stage "alias-migration-test" :kind :suite :suite "alias"
      :authority-policy "battery-parallel-runner/suite-namespaces \"alias\""
      :runtime {:kind :jvm :mode :cold :argv jvm-worker-argv}
      :recipe (recipe "alias-migration-test")
      :required-inputs [(file-digest "test/clj_surgeon/lane_manifest.clj")]
      :selected-test-identities (ns-names alias-ns)
      :scope {:kind :whole-namespaces :count (count alias-ns)}
      :exclusions []
      :result-predicate result-predicate-suite
      :discharge {:by :execution-evidence
                  :note "Runs at EVERY landing; battery freshness cannot substitute."}}

     {:id :R4 :name "mcp"
      :stage "mcp-test" :kind :suite :suite "mcp"
      :authority-policy "lane-manifest :fast then :integration, sorted"
      :runtime {:kind :jvm :mode :cold :argv jvm-worker-argv}
      :recipe (recipe "mcp-test")
      :required-inputs [(file-digest "test/clj_surgeon/lane_manifest.clj")
                        (file-digest "deps.edn")]
      :selected-test-identities (ns-names mcp-ns)
      :scope {:kind :whole-namespaces :count (count mcp-ns)
              :phases {:fast (count fast-ns) :integration (count integration-ns)}
              :barrier "global fast-before-integration"}
      :exclusions []
      :nested-checks {:target "mcp-test-checks"
                      :recipe (recipe "mcp-test-checks")
                      :members ["mcp-operation-oracle"
                                "performance-regression-sentinel-intent-test"
                                "test/oracles/test_gate_slot.py"
                                "test/oracles/test_namespace_split_papercut_oracle.py"
                                "test/oracles/test_require_change_oracle.py"
                                "test/oracles/test_cell_b_oracle.py"
                                "repository-hygiene-self-test"
                                "txn-kernel-warning-check"
                                "mcp-heap-config-self-test"
                                "tmp-leak-ratchet-self-test"
                                "clj-kondo-admission-path-self-test"
                                "analyzer-contract-target-self-test"
                                "cclsp-start-self-test"
                                "cclsp-client-audit-self-test"]}
      :result-predicate result-predicate-suite
      :discharge {:by :execution-evidence
                  :note "A pool exit alone is insufficient; the loaded Var census and the union isolation budget are part of the claim."}}

     {:id :R5 :name "bb"
      :stage "test-bb" :kind :suite :suite "bb"
      :authority-policy "test/run_all.clj literal namespace vector"
      :runtime {:kind :bb :mode :cold :argv bb-worker-argv}
      :recipe (recipe "test-bb")
      :required-inputs [(file-digest "test/run_all.clj")]
      :selected-test-identities (ns-names bb-ns)
      :scope {:kind :whole-namespaces :count (count bb-ns)}
      :exclusions []
      :result-predicate (assoc result-predicate-suite :temp-leaks 0)
      :discharge {:by :execution-evidence
                  :note "The JVM coordinator is not the BB execution runtime. JVM tests of the same source do not discharge BB compatibility."}}

     {:id :R6 :name "hygiene"
      :stage "repository-hygiene" :kind :after
      :authority-policy "battery-parallel-runner/gate-stage-manifest"
      :runtime {:kind :shell :mode :cold
                :argv ["sh" "test/repository_hygiene_gate.sh"]}
      :recipe (recipe "repository-hygiene")
      :required-inputs [(file-digest "test/repository_hygiene_gate.sh")
                        (file-digest ".gitignore")]
      :selected-test-identities []
      :scope {:kind :workspace-state :lifecycle :final-candidate}
      :exclusions []
      :result-predicate {:exit 0}
      :discharge {:by :execution-evidence
                  :note "Lifecycle-bound: an earlier pre-cleanup status does not certify later workspace hygiene."}}

     {:id :R7 :name "intent-audit"
      :stage "intent-audit" :kind :audit
      :authority-policy "battery-parallel-runner/gate-stage-manifest"
      :runtime {:kind :bb :mode :cold
                :argv ["bb" "--classpath" "src:test" "-e"
                       "(require 'clj-surgeon.mcp-intent-contract) ... audit-current-repository"]}
      :recipe (recipe "intent-audit")
      :required-inputs [(file-digest "src/clj_surgeon/mcp_intent_contract.clj")]
      :selected-test-identities []
      :scope {:kind :registry-and-annotations}
      :exclusions []
      :result-predicate {:exit 0 :ok true :violations []}
      :discharge {:by :execution-evidence
                  :note "The exact audit RESULT on this candidate; a quoted historical \"audit-ok\" is not the audit."}}

     {:id :R8 :name "coverage-policy-integrity"
      :stage nil :kind :policy
      :authority-policy "lane-manifest + run_all + deftest census, reconciled by NAME"
      :runtime {:kind :derivation}
      :recipe {:root nil :targets [] :unresolved [] :sha256 nil}
      :required-inputs [(file-digest "test/clj_surgeon/lane_manifest.clj")
                        (file-digest "test/run_all.clj")
                        (file-digest "test/clj_surgeon/deftest_census.edn")]
      :selected-test-identities []
      :scope {:kind :inventory
              :jvm {:fast (count fast-ns) :integration (count integration-ns)
                    :battery (count battery-ns)}
              :bb (count bb-ns)
              :gate-stages stage-targets}
      :exclusions (mapv (fn [[k v]] {:namespace (str k) :reason v})
                        (sort-by (comp str key) lm/excluded))
      :result-predicate {:missing [] :duplicate [] :unexpected []
                         :census-deletions []}
      :discharge {:by :consumer-derivation
                  :note (str "Reconciled by the consumer against the receipt's executed "
                             "identities. Omissions, duplicates, renamed same-count "
                             "substitutions and unexplained census deletions refuse.")}}

     {:id :R9 :name "task-acceptance-and-review"
      :stage nil :kind :acceptance
      :authority-policy "the task's own frozen oracle and required independent review"
      :runtime {:kind :out-of-band}
      :recipe {:root nil :targets [] :unresolved [] :sha256 nil}
      :required-inputs []
      :selected-test-identities []
      :scope {:kind :task-specific}
      :exclusions []
      :result-predicate {:oracle :passed :review :verdict-present}
      :discharge {:by :out-of-band
                  :note (str "NEVER dischargeable by a gate receipt. A tool's commit or "
                             "body hash is not a review. Present in the inventory so a green "
                             "gate cannot read as a complete landing proof.")}}

     {:id :R10 :name "publication-freshness"
      :stage nil :kind :publication
      :authority-policy "the consumer's accepted record plus the observer's destination-ref evidence"
      :runtime {:kind :out-of-band}
      :recipe {:root nil :targets [] :unresolved [] :sha256 nil}
      :required-inputs []
      :selected-test-identities []
      :scope {:kind :destination-ref}
      :exclusions []
      :result-predicate {:no-intervening-mutation true :destination-authorized true}
      :discharge {:by :out-of-band
                  :note (str "Producer `next_action=none`/`terminal_response` describes only "
                             "the producer's own operation and never this.")}}]))

(defn- volatile-free
  "The obligations with nothing candidate-specific in them, for the policy hash.
   `:required-inputs` digests DO stay: they are what the policy is about."
  [obs]
  (mapv #(dissoc % :candidate) obs))

(defn inventory
  "The printable inventory. `:policy-sha256` moves when a recipe, a selected
   namespace, an exclusion, a required input or a predicate moves; it does not
   move because the same tree was checked out somewhere else."
  [policy]
  (let [obs (obligations policy)]
    {:inventory-version 1
     :kind :gate-obligations
     :stage-manifest (vec (:stage-manifest policy))
     :policy-sha256 (sha256 (pr-str (volatile-free obs)))
     :obligations obs}))

(defn runner-policy
  "The repository's own gate policy, resolved at call time out of the gate
   runner. Resolved dynamically for one reason only: the runner requires THIS
   namespace to stamp its receipt, so a static require here would be a cycle."
  []
  (require 'clj-surgeon.battery-parallel-runner)
  {:stage-manifest (mapv :target @(resolve 'clj-surgeon.battery-parallel-runner/gate-stage-manifest))
   :suite-namespaces @(resolve 'clj-surgeon.battery-parallel-runner/suite-namespaces)})

(defn -main [& args]
  (let [opts (apply hash-map args)]
    (cond
      (= "true" (get opts "--print-gate-obligations"))
      (do (println (pr-str (inventory (runner-policy)))) (flush) (System/exit 0))

      :else
      (binding [*out* *err*]
        (println "usage: -m clj-surgeon.gate-obligations --print-gate-obligations true")
        (System/exit 2)))))
