(ns owner-aware-call-construction-screen
  "Pure catalog projection and scorer for the owner-aware call-construction screen."
  (:require
   [cheshire.core :as json]
   [clj-surgeon.experiments.mcp-candidate-admission :as admission]
   [clj-surgeon.intent-transaction :as transaction]
   [clj-surgeon.mcp-compact-location :as compact-location]
   [clj-surgeon.mcp-contract :as contract]
   [clj-surgeon.mcp-schema :as mcp-schema]
   [clj-surgeon.mcp-tool :as mcp-tool]
   [clj-surgeon.mcp-workspace :as workspace]
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [owner-aware-symbol-migration :as migration]))

(def candidate-field-name "symbol_migration")

(def candidate-field-schema
  {:type "object"
   :additionalProperties false
   :description
   (str "Grouped exact owner-scoped symbol replacements. target_alias replaces "
        "the qualifier while preserve-name retains each from symbol's name. "
        "Each files entry is [file, sites]; each site is [owner, from, matches]. "
        "Owners, old symbols, and positive counts are authority, not discovery.")
   :properties
   {"target_alias" {:type "string" :minLength 1}
    "target_rule" {:type "string" :enum ["preserve-name"]}
    "columns" {:type "array"
               :minItems 3
               :maxItems 3
               :prefixItems [{:const "owner"}
                             {:const "from"}
                             {:const "matches"}]}
    "files" {:type "array"
             :minItems 1
             :items
             {:type "array"
              :minItems 2
              :maxItems 2
              :prefixItems
              [{:type "string" :minLength 1}
               {:type "array"
                :minItems 1
                :items
                {:type "array"
                 :minItems 3
                 :maxItems 3
                 :prefixItems [{:type "string" :minLength 1}
                               {:type "string" :minLength 1}
                               {:type "integer" :minimum 1 :maximum 128}]}}]}}}
   :required ["target_alias" "target_rule" "columns" "files"]})

(def candidate-description-suffix
  (str " For repeated exact symbol migrations across named owners, use one "
       "symbol_migration table with target_alias, target_rule=preserve-name, "
       "columns=[owner,from,matches], and files=[[file,sites],...]. It lowers "
       "each declared row to a guarded literal edit. It never discovers or "
       "chooses files, owners, old symbols, aliases, or counts."))

(def candidate-tool-schema
  (assoc-in mcp-schema/editor-tool-schema
            [:properties candidate-field-name]
            candidate-field-schema))

(def candidate-tool-description
  (str mcp-tool/edit-tool-description candidate-description-suffix))

(defn tool-surface
  "Return the exact control or candidate edit_clojure surface."
  [arm]
  (case arm
    :control {:name "edit_clojure"
              :description mcp-tool/edit-tool-description
              :schema mcp-schema/editor-tool-schema}
    :candidate {:name "edit_clojure"
                :description candidate-tool-description
                :schema candidate-tool-schema}
    (throw (ex-info "Unknown screen arm" {:arm arm}))))

(defn public-json
  "Normalize captured JSON arguments to string-keyed public data."
  [value]
  (json/parse-string (json/generate-string value)))

(defn expand-submission
  "Lower one arm's public arguments to the current edit_clojure request."
  [arm arguments]
  (let [arguments (public-json arguments)]
    (case arm
      :control {:ok true :request arguments}
      :candidate (migration/compile-manifest arguments)
      (throw (ex-info "Unknown screen arm" {:arm arm})))))

(defn- route-workspace-root
  "Apply the public workspace adapter without constructing a runtime context."
  [arguments expected-workspace-root]
  (let [expected (workspace/canonical-root expected-workspace-root)]
    (if-not (:ok expected)
      expected
      (if-let [requested-value (get arguments "workspace_root")]
        (let [requested (workspace/canonical-root requested-value)]
          (cond
            (not (:ok requested)) requested

            (not= (:workspace-root expected) (:workspace-root requested))
            {:ok false
             :error "workspace_root does not match the frozen benchmark workspace"
             :error-type :benchmark-workspace-root-mismatch
             :expected-workspace-root (:workspace-root expected)
             :actual-workspace-root (:workspace-root requested)}

            :else
            {:ok true
             :workspace-root (:workspace-root requested)
             :request (dissoc arguments "workspace_root")}))
        {:ok true
         :workspace-root (:workspace-root expected)
         :request arguments}))))

(defn compile-submission
  "Compile captured public arguments through production admission and normalization."
  ([sources arm arguments]
   (compile-submission sources arm arguments migration/canonical-workspace))
  ([sources arm arguments expected-workspace-root]
   (let [arguments (public-json arguments)
         public-admission (admission/authorize
                            (:schema (tool-surface arm)) arguments)]
     (if-not (:ok public-admission)
       {:ok false :product public-admission}
       (let [routed (route-workspace-root arguments expected-workspace-root)]
         (if-not (:ok routed)
           {:ok false :product routed}
           (let [expanded (expand-submission arm (:request routed))]
             (if-not (:ok expanded)
               expanded
               (let [validated (contract/validate-tool-params (:request expanded))]
                 (if-not (:ok validated)
                   (assoc expanded :product validated)
                   (let [spec (contract/tool-params->transaction (:params validated))
                         prepared
                         (compact-location/normalize-spec
                           sources spec (:compact-location-normalization validated))
                         product
                         (if (:error prepared)
                           prepared
                           (assoc prepared
                                  :transaction (:spec prepared)
                                  :compiled
                                  (transaction/compile-transaction
                                    sources (:spec prepared))))]
                     (assoc expanded
                            :workspace-root (:workspace-root routed)
                            :product product))))))))))))

(defn- compiled-ok? [product]
  (and (:ok product)
       (map? (:compiled product))
       (nil? (get-in product [:compiled :error]))))

(defn score-call
  "Score one captured call without executing a mutation.

  geometry must contain mcp-call-count, refusal-count, recovery-count,
  shell-call-count, file-change-count, and prompt-to-call-ms."
  [sources expected-after-hashes arm arguments geometry]
  (let [arguments (public-json arguments)
        compiled (compile-submission sources arm arguments)
        product (:product compiled)
        product-compiled (:compiled product)
        oracle (compile-submission
                 sources :control
                 (dissoc migration/oracle-request "workspace_root"))
        payload-bytes (migration/json-byte-count arguments)
        future-hashes (when (compiled-ok? product)
                        (migration/file-hashes product-compiled))
        exact-future? (= expected-after-hashes future-hashes)
        normalized-equal? (= (get-in oracle [:product :transaction]) (:transaction product))
        one-action? (and (= 1 (:mcp-call-count geometry))
                         (zero? (:refusal-count geometry))
                         (zero? (:recovery-count geometry))
                         (zero? (:shell-call-count geometry))
                         (zero? (:file-change-count geometry)))
        payload-ok? (or (= :control arm)
                        (<= payload-bytes migration/candidate-payload-budget))
        first-call-valid? (and (:ok compiled)
                               (compiled-ok? product)
                               normalized-equal?
                               exact-future?)
        correct? (and first-call-valid?
                      one-action?
                      payload-ok?
                      (= 51 (:match-count product-compiled))
                      (= 9 (:changed-file-count product-compiled)))]
    {:schema :clj-surgeon.owner-aware-call-screen-run/v1
     :arm arm
     :correct correct?
     :first-call-valid first-call-valid?
     :one-action one-action?
     :payload {:bytes payload-bytes
               :budget (when (= :candidate arm)
                         migration/candidate-payload-budget)
               :within-budget payload-ok?}
     :compiler {:normalized-transaction-equal normalized-equal?
                :future-hashes-equal exact-future?
                :match-count (:match-count product-compiled)
                :changed-file-count (:changed-file-count product-compiled)
                :error-type (or (:error-type compiled)
                                (:error-type product)
                                (get-in product [:compiled :error-type]))}
     :geometry geometry}))

(defn prompt-to-first-call-ms
  "Measure the observer interval from turn start to the first MCP call start."
  [timing]
  (let [observations (:observations timing)
        turn (first (filter #(= :turn-started (:event-kind %)) observations))
        call (first (filter #(and (= :mcp-tool-call-started (:event-kind %))
                                  (> (:sequence %) (:sequence turn)))
                            observations))]
    (when (and turn call)
      (/ (- (:observer-monotonic-ns call)
            (:observer-monotonic-ns turn))
         1000000.0))))

(defn- median [values]
  (let [values (vec (sort values))
        n (count values)
        middle (quot n 2)]
    (when (pos? n)
      (if (odd? n)
        (double (nth values middle))
        (/ (+ (double (nth values (dec middle)))
              (double (nth values middle)))
           2.0)))))

(def expected-run-manifest
  [{:run-id "b1-n1" :block 1 :position 1 :arm :control}
   {:run-id "b1-r1" :block 1 :position 2 :arm :candidate}
   {:run-id "b1-r2" :block 1 :position 3 :arm :candidate}
   {:run-id "b1-n2" :block 1 :position 4 :arm :control}
   {:run-id "b2-r1" :block 2 :position 1 :arm :candidate}
   {:run-id "b2-n1" :block 2 :position 2 :arm :control}
   {:run-id "b2-n2" :block 2 :position 3 :arm :control}
   {:run-id "b2-r2" :block 2 :position 4 :arm :candidate}])

(def ^:private sha256-pattern #"[0-9a-f]{64}")

(defn- sha256? [value]
  (and (string? value) (boolean (re-matches sha256-pattern value))))

(defn- positive-number? [value]
  (and (number? value) (pos? value)))

(defn- complete-run?
  [expected run]
  (and (= expected (select-keys run [:run-id :block :position :arm]))
       (true? (:correct run))
       (positive-number? (get-in run [:geometry :prompt-to-call-ms]))
       (positive-number? (get-in run [:geometry :complete-wall-ms]))
       (true? (get-in run [:verification :complete]))
       (every? sha256?
               (map #(get-in run [:verification %])
                    [:canonical-transaction-sha256
                     :future-hashes-sha256
                     :verifier-profile-sha256]))
       (every? #(let [value (get-in run [:isolation %])]
                  (and (string? value) (not (clojure.string/blank? value))))
               [:workspace-id :codex-home-id :session-id
                :receipt-dir-id :server-id])
       (every? sha256?
               (map #(get-in run [:isolation %])
                    [:starting-tree-sha256 :lifecycle-policy-sha256]))))

(defn- unique-values? [runs path]
  (= (count runs) (count (set (map #(get-in % path) runs)))))

(defn- one-value? [runs path]
  (= 1 (count (set (map #(get-in % path) runs)))))

(defn- arm-median [runs arm]
  (median (map #(get-in % [:geometry :complete-wall-ms])
               (filter (comp #{arm} :arm) runs))))

(defn- improvement-ratio [control-ms candidate-ms]
  (when (and (positive-number? control-ms) (positive-number? candidate-ms))
    (/ (- control-ms candidate-ms) control-ms)))

(defn- block-report [runs block]
  (let [block-runs (if block
                     (filter (comp #{block} :block) runs)
                     runs)
        control-ms (arm-median block-runs :control)
        candidate-ms (arm-median block-runs :candidate)]
    {:control-median-verified-ms control-ms
     :candidate-median-verified-ms candidate-ms
     :candidate-improvement-ratio (improvement-ratio control-ms candidate-ms)}))

(defn cohort-report
  "Score the manifest-bound N/R protocol. Four runs may authorize block two;
  all eight are required for promotion."
  [runs]
  (let [runs (vec runs)
        allowed-count? (contains? #{4 8} (count runs))
        expected (subvec expected-run-manifest 0 (min (count runs) 8))
        manifest-exact? (and allowed-count?
                             (= expected
                                (mapv #(select-keys % [:run-id :block :position :arm])
                                      runs)))
        complete? (and manifest-exact?
                       (every? true? (map complete-run? expected runs)))
        isolation-paths [[:isolation :workspace-id]
                         [:isolation :codex-home-id]
                         [:isolation :session-id]
                         [:isolation :receipt-dir-id]
                         [:isolation :server-id]]
        isolated? (and complete?
                       (every? #(unique-values? runs %) isolation-paths)
                       (one-value? runs [:isolation :starting-tree-sha256])
                       (one-value? runs [:isolation :lifecycle-policy-sha256]))
        verification-identical?
        (and complete?
             (every? #(one-value? runs [:verification %])
                     [:canonical-transaction-sha256
                      :future-hashes-sha256
                      :verifier-profile-sha256]))
        block-1 (when (and complete? (>= (count runs) 4))
                  (block-report runs 1))
        block-1-authorized?
        (and isolated? verification-identical?
             (>= (or (:candidate-improvement-ratio block-1) -1.0) 0.15))
        full? (= 8 (count runs))
        block-2 (when (and complete? full?) (block-report runs 2))
        pooled (when (and complete? full?) (block-report runs nil))
        block-2-win? (and complete? full?
                          (< (:candidate-median-verified-ms block-2)
                             (:control-median-verified-ms block-2)))
        pooled-pass? (and complete? full?
                          (>= (or (:candidate-improvement-ratio pooled) -1.0)
                              0.20))
        gate? (and block-1-authorized? block-2-win? pooled-pass?)]
    {:schema :clj-surgeon.owner-aware-call-screen-cohort/v2
     :run-count (count runs)
     :manifest-exact manifest-exact?
     :runs-complete complete?
     :isolation-complete isolated?
     :verification-identical verification-identical?
     :blocks {1 block-1 2 block-2}
     :pooled pooled
     :gate {:block-2-authorized block-1-authorized?
            :minimum-block-1-improvement-ratio 0.15
            :minimum-pooled-improvement-ratio 0.20
            :pass gate?}}))

(defn score-fixture-call [arm arguments geometry]
  (let [{:keys [sources expected-after-hashes]} (migration/load-fixture)]
    (score-call sources expected-after-hashes arm arguments geometry)))

(defn- parse-pairs [args]
  (when (odd? (count args))
    (throw (ex-info "Expected --key value pairs" {:args args})))
  (into {} (map (fn [[key value]] [(keyword (subs key 2)) value]))
        (partition 2 args)))

(defn- read-json [path]
  (json/parse-string (slurp (io/file path))))

(defn -main [& args]
  (case (first args)
    "score"
    (let [{:keys [arm capture timing mcp-calls shell-calls file-changes]}
          (parse-pairs (rest args))
          capture (read-json capture)
          timing (edn/read-string (slurp (io/file timing)))
          calls (get capture "calls")
          geometry {:mcp-call-count (parse-long mcp-calls)
                    :refusal-count 0
                    :recovery-count (max 0 (dec (parse-long mcp-calls)))
                    :shell-call-count (parse-long shell-calls)
                    :file-change-count (parse-long file-changes)
                    :prompt-to-call-ms (prompt-to-first-call-ms timing)}]
      (prn (score-fixture-call (keyword arm) (get (first calls) "params")
                               geometry)))

    (throw (ex-info "Usage: score --arm ARM --capture FILE --timing FILE --mcp-calls N --shell-calls N --file-changes N"
                    {:args args}))))
