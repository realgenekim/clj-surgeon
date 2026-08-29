(ns owner-aware-call-construction-prereq
  "Prerequisite-corrected scorer for the owner-aware call-construction screen.

   This namespace changes no product contract. It combines the existing
   symbol_migration experiment with the current public field-pair validator and
   compact-location compiler, then scores capture-only model calls."
  (:require
   [cheshire.core :as json]
   [clj-surgeon.mcp-contract :as contract]
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [namespace-tolerance-replay :as replay]
   [owner-aware-call-construction-screen :as screen]
   [owner-aware-symbol-migration :as migration]))

(def pair-fields
  {"from-to" ["from" "to"]
   "old-new" ["old" "new"]
   "before-after" ["before" "after"]})

(defn utf8-bytes [value]
  (alength (.getBytes (str value) "UTF-8")))

(defn public-json [value]
  (json/parse-string (json/generate-string value)))

(defn compile-submission
  "Expand only the experimental symbol_migration field, then use the current
  product validator, field normalizer, location normalizer, and compiler."
  [sources arm arguments]
  (when-not (#{:control :candidate} arm)
    (throw (ex-info "Unknown screen arm" {:arm arm})))
  (let [arguments (public-json arguments)]
    {:ok true
     :product (replay/compile-product sources arguments)}))

(defn exact-future?
  [product expected-after-hashes]
  (let [compiled (:compiled product)]
    (and (:ok product)
         (:ok compiled)
         (= 51 (:match-count compiled))
         (= 9 (:changed-file-count compiled))
         (= expected-after-hashes (migration/file-hashes compiled)))))

(defn score-call
  [sources expected-after-hashes arm arguments geometry]
  (let [arguments (public-json arguments)
        compiled (compile-submission sources arm arguments)
        product (:product compiled)
        payload-bytes (migration/json-byte-count arguments)
        one-action? (and (= 1 (:mcp-call-count geometry))
                         (zero? (:refusal-count geometry))
                         (zero? (:recovery-count geometry))
                         (zero? (:shell-call-count geometry))
                         (zero? (:file-change-count geometry)))
        exact? (exact-future? product expected-after-hashes)
        payload-ok? (or (= :control arm)
                        (<= payload-bytes migration/candidate-payload-budget))]
    {:schema :clj-surgeon.owner-aware-call-screen-run/v2
     :arm arm
     :correct (and exact? one-action? payload-ok?)
     :first-call-valid exact?
     :one-action one-action?
     :payload {:bytes payload-bytes
               :budget (when (= :candidate arm)
                         migration/candidate-payload-budget)
               :within-budget payload-ok?}
     :compiler {:future-hashes-equal exact?
                :match-count (get-in product [:compiled :match-count])
                :changed-file-count
                (get-in product [:compiled :changed-file-count])
                :field-normalization (:compact-field-normalization product)
                :location-normalization (:location-normalization product)
                :owner-row-count (:owner-row-count product)
                :declared-match-count (:declared-match-count product)
                :owner-match-rows-preserved
                (:owner-match-rows-preserved product)
                :error-type (or (:error-type compiled)
                                (:error-type product)
                                (get-in product [:compiled :error-type]))}
     :geometry geometry}))

(defn rename-pair
  [edit relation]
  (let [[source-field target-field] (get pair-fields relation)
        from (get edit "from")
        to (get edit "to")]
    (cond-> edit
      (and source-field (contains? edit "from") (contains? edit "to"))
      (-> (dissoc "from" "to")
          (assoc source-field from target-field to)))))

(defn request-with-pair
  [request relation]
  (update request "edits" #(mapv (fn [edit] (rename-pair edit relation)) %)))

(defn surface-metrics
  [arm]
  (let [surface (screen/tool-surface arm)]
    {:description-bytes (utf8-bytes (:description surface))
     :schema-bytes (migration/json-byte-count (:schema surface))
     :surface-bytes (migration/json-byte-count surface)}))

(defn ambiguous-field-falsifiers
  []
  (let [edit (first (get migration/oracle-request "edits"))
        request (fn [candidate]
                  {"edits" [candidate]})
        refuse?
        (fn [candidate]
          (let [result (contract/validate-tool-params (request candidate))]
            (and (false? (:ok result))
                 (= :invalid-editor-field-pair (:reason result))
                 (:source-unchanged result)
                 (false? (:write-authority result)))))]
    {:partial
     (refuse? (-> edit (dissoc "from" "to") (assoc "old" (get edit "from"))))
     :mixed
     (refuse? (-> edit (dissoc "from" "to")
                  (assoc "old" (get edit "from")
                         "after" (get edit "to"))))
     :canonical-plus-alias
     (refuse? (assoc edit "old" (get edit "from")
                     "new" (get edit "to")))
     :two-alias-pairs
     (refuse? (-> edit (dissoc "from" "to")
                  (assoc "old" (get edit "from")
                         "new" (get edit "to")
                         "before" (get edit "from")
                         "after" (get edit "to"))))}))

(defn prerequisite-report
  []
  (let [{:keys [sources expected-after-hashes]} (migration/load-fixture)
        manifest (replay/load-retained-manifest)
        captures (replay/retained-captures manifest)
        compile-variant
        (fn [capture relation]
          (let [arm (:base capture)
                arm (if (= :candidate arm) :candidate :control)
                request (request-with-pair (:request capture) relation)
                result (compile-submission sources arm request)
                product (:product result)]
            {:run (:run capture)
             :arm arm
             :relation relation
             :exact-future (exact-future? product expected-after-hashes)
             :owner-row-count (:owner-row-count product)
             :declared-match-count (:declared-match-count product)
             :owner-match-rows-preserved
             (:owner-match-rows-preserved product)
             :field-normalization-count
             (count (:compact-field-normalization product))
             :location-normalization-count
             (count (:location-normalization product))
             :error-type (or (:error-type result)
                             (:error-type product)
                             (get-in product [:compiled :error-type]))}))
        variants (mapv (fn [capture relation]
                         (compile-variant capture relation))
                       (mapcat #(repeat 3 %) captures)
                       (cycle (keys pair-fields)))
        canonical (filterv #(= "from-to" (:relation %)) variants)
        old-new (filterv #(= "old-new" (:relation %)) variants)
        before-after (filterv #(= "before-after" (:relation %)) variants)
        candidate-runs (filterv #(= :candidate (:arm %)) variants)
        falsifiers (ambiguous-field-falsifiers)
        candidate-arguments
        (dissoc migration/candidate-manifest "workspace_root")
        control-arguments
        (dissoc migration/oracle-request "workspace_root")]
    {:schema :clj-surgeon.owner-aware-call-prerequisites/v1
     :model-calls 0
     :mutation-actions 0
     :retained-captures {:count (count captures)
                         :raw-corpus-bound
                         (every? #(and (:schema-valid %)
                                       (:capture-bytes-equal %)
                                       (:capture-hash-equal %)
                                       (:request-hash-equal %))
                                 captures)}
     :surface {:control (surface-metrics :control)
               :candidate (surface-metrics :candidate)}
     :emitted-arguments
     {:control-bytes (migration/json-byte-count control-arguments)
      :candidate-bytes (migration/json-byte-count candidate-arguments)
      :candidate-budget migration/candidate-payload-budget}
     :relations
     {:canonical {:runs (count canonical)
                  :exact (count (filter :exact-future canonical))}
      :old-new {:runs (count old-new)
                :exact (count (filter :exact-future old-new))}
      :before-after {:runs (count before-after)
                     :exact (count (filter :exact-future before-after))}}
     :candidate-migration
     {:runs (count candidate-runs)
      :all-owner-rows-preserved
      (every? #(and (= 23 (:owner-row-count %))
                    (= 27 (:declared-match-count %))
                    (:owner-match-rows-preserved %))
              candidate-runs)}
     :falsifiers falsifiers
     :all-prerequisites-green
     (and (= 24 (count variants))
          (every? :exact-future variants)
          (every? true? (vals falsifiers))
          (every? #(and (= 23 (:owner-row-count %))
                        (= 27 (:declared-match-count %))
                        (:owner-match-rows-preserved %))
                  candidate-runs)
          (<= (migration/json-byte-count candidate-arguments)
              migration/candidate-payload-budget))}))

(defn pilot-report
  [runs]
  (let [by-arm (group-by :arm runs)]
    {:schema :clj-surgeon.owner-aware-call-screen-pilot/v1
     :arms
     (into {}
           (map (fn [arm]
                  (let [run (first (get by-arm arm))]
                    [arm {:runs (count (get by-arm arm))
                          :correct (boolean (:correct run))
                          :prompt-to-call-ms
                          (get-in run [:geometry :prompt-to-call-ms])
                          :payload-bytes (get-in run [:payload :bytes])}]))
                [:control :candidate]))
     :gate {:one-per-arm (= [1 1]
                            (mapv #(count (get by-arm %))
                                  [:control :candidate]))
            :both-correct (every? :correct runs)}}))

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
                    :prompt-to-call-ms
                    (screen/prompt-to-first-call-ms timing)}
          {:keys [sources expected-after-hashes]} (migration/load-fixture)]
      (prn (score-call sources expected-after-hashes (keyword arm)
                       (get (first calls) "params") geometry)))

    "pilot"
    (let [runs (mapv #(edn/read-string (slurp %)) (rest args))]
      (prn (pilot-report runs)))

    "prerequisites"
    (let [result (prerequisite-report)]
      (prn result)
      (when-not (:all-prerequisites-green result)
        (System/exit 1)))

    (throw (ex-info "Usage: score|pilot|prerequisites" {:args args}))))
