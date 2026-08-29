(ns owner-aware-call-construction-screen
  "Pure catalog projection and scorer for the owner-aware call-construction screen."
  (:require
   [cheshire.core :as json]
   [clj-surgeon.intent-transaction :as transaction]
   [clj-surgeon.mcp-compact-location :as compact-location]
   [clj-surgeon.mcp-contract :as contract]
   [clj-surgeon.mcp-schema :as mcp-schema]
   [clj-surgeon.mcp-tool :as mcp-tool]
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

(defn compile-submission
  "Compile captured public arguments through production admission and normalization."
  [sources arm arguments]
  (let [expanded (expand-submission arm arguments)]
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
            (assoc expanded :product product)))))))

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

(defn cohort-report
  "Apply the frozen N=8 screen gate: four correct runs per arm and 15% faster
  candidate median prompt-to-call emission."
  [runs]
  (let [by-arm (group-by :arm runs)
        arm-report
        (into {}
              (map (fn [arm]
                     (let [arm-runs (get by-arm arm [])]
                       [arm {:runs (count arm-runs)
                             :correct (count (filter :correct arm-runs))
                             :median-prompt-to-call-ms
                             (median (keep #(get-in % [:geometry
                                                       :prompt-to-call-ms])
                                           arm-runs))}]))
                   [:control :candidate]))
        control-ms (get-in arm-report [:control :median-prompt-to-call-ms])
        candidate-ms (get-in arm-report [:candidate :median-prompt-to-call-ms])
        improvement (when (and (number? control-ms) (pos? control-ms)
                               (number? candidate-ms))
                      (/ (- control-ms candidate-ms) control-ms))
        gate? (and (= 4 (get-in arm-report [:control :runs]))
                   (= 4 (get-in arm-report [:candidate :runs]))
                   (= 4 (get-in arm-report [:control :correct]))
                   (= 4 (get-in arm-report [:candidate :correct]))
                   (number? improvement)
                   (>= improvement 0.15))]
    {:schema :clj-surgeon.owner-aware-call-screen-cohort/v1
     :arms arm-report
     :candidate-improvement-ratio improvement
     :gate {:four-of-four-each-arm true
            :minimum-improvement-ratio 0.15
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
