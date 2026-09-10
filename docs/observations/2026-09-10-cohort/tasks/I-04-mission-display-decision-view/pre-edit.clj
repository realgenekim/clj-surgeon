(ns clj-surgeon.mission-display
  "Bounded saved-receipt presentation shared by the BB and JVM entrances."
  (:require
   [clojure.pprint :as pp]
   [clojure.string :as str]))

(def byte-limit 4096)

(defn shell-quote [value]
  (str "'" (str/replace (str value) "'" "'\"'\"'") "'"))

(defn command [argv]
  {:argv argv :command (str/join " " (map shell-quote argv))})

(defn workspace-args [result opts]
  (cond-> []
    (or (:workspace opts) (:root result) (get-in opts [:request :workspace_root]))
    (into ["--workspace" (or (:workspace opts) (:root result)
                             (get-in opts [:request :workspace_root]))])
    (:state-home opts) (into ["--state-home" (:state-home opts)])))

(defn with-recovery [result opts]
  (if (and (map? result)
           (or (false? (:ok result)) (#{:blocked :failed} (:state result))))
    (let [id (or (:id result) (:id opts))
          unknown? (= "mission-unknown-id" (:error_type result))
          workspace (workspace-args result opts)
          argv (cond
                 (empty? workspace) ["bin/mission" "help" "run"]
                 (or unknown? (nil? id)) (into ["bin/mission" "list"] workspace)
                 :else (into ["bin/mission" "show" id] workspace))]
      (assoc result :example (command argv)))
    result))

(defn limit-data
  "Bound diagnostic detail. The enclosing view explicitly marks any truncation."
  [value depth width text-limit]
  (cond
    (string? value) (subs value 0 (min (count value) text-limit))
    (or (nil? value) (boolean? value)) value
    (or (number? value) (keyword? value))
    (if (<= (count (str value)) 96) value :detail-omitted)
    (zero? depth) :detail-omitted
    (map? value) (into {} (map (fn [[k v]] [k (limit-data v (dec depth) width text-limit)]))
                       (take width (filter #(and (keyword? (key %)) (<= (count (str (key %))) 96)) value)))
    (sequential? value) (mapv #(limit-data % (dec depth) width text-limit) (take width value))
    :else :detail-omitted))

(defn candidate-view [candidate]
  (cond-> (select-keys candidate [:index :compiled :error-type :error_type :refusal :lost])
    (:proof candidate)
    (assoc :proof (let [proof (:proof candidate)]
                    (cond-> (select-keys proof [:ok :proof-inputs-unchanged])
                      (:gate proof) (assoc :gate (select-keys (:gate proof) [:ok :id]))
                      (:acceptance proof) (assoc :acceptance (select-keys (:acceptance proof) [:ok :id])))))))

(defn pretty [value]
  (with-out-str (pp/pprint value)))

(defn show-refusal [view opts]
  (let [original (with-recovery view opts)
        bounded (limit-data original 4 8 64)
        result (assoc bounded :example (:example original)
                              :truncated (not= original bounded))]
    (if (<= (alength (.getBytes (pretty result) "UTF-8")) byte-limit)
      result
      (assoc bounded :example (command ["bin/mission" "help" "show"]) :truncated true))))

(defn show-result [view opts]
  (cond
    (false? (:ok view)) (show-refusal view opts)
    (:full opts) view
    :else
    (let [candidates (get-in view [:receipt :candidates])
          total (count candidates)
          details (command (into ["bin/mission" "show" (:id view)]
                                 (conj (workspace-args view opts) "--full")))
          receipt (select-keys (:receipt view)
                               [:ok :committed :verification-complete :error-type :error_type
                                :error :elapsed_ms :receipt_hash :undo_receipt :artifacts
                                :mutation-attempted :refusal])
          route (get-in view [:plan :typist :route])
          route (when route (cond-> (select-keys route [:executor :k :candidate-format])
                              (:provider route) (assoc :provider (select-keys (:provider route) [:id :model :upstream]))))
          base (cond-> (merge (select-keys view [:id :state :effective_state :verb :question
                                                 :decision :decision_summary :effective_next_action
                                                 :graph :dependencies :config_sources])
                              {:ok true :operation "mission-show" :authority :saved-mission
                               :details details})
                 route (assoc :route (select-keys route [:executor :k :candidate-format :provider])))]
      (loop [[[shown text-limit] & more] [[5 192] [2 128] [1 64] [0 32]]]
        (let [raw (assoc base :receipt (when (:receipt view) (assoc receipt
                                                               :candidate-count total
                                                               :candidates-omitted (max 0 (- total shown))
                                                               :candidates (mapv candidate-view (take shown candidates)))))
              bounded (limit-data raw 8 20 text-limit)
              result (assoc bounded :details details
                            :truncated (or (> total shown) (not= raw bounded)))]
          (if (<= (alength (.getBytes (pretty result) "UTF-8")) byte-limit)
            result
            (if more
              (recur more)
              (limit-data
                {:ok true :operation "mission-show" :authority :saved-mission
                 :id (:id view) :state (:state view) :effective_state (:effective_state view)
                 :truncated true :details {:flag "--full" :instruction "Repeat this show command with --full for omitted detail."}
                 :receipt (assoc (select-keys receipt [:committed :verification-complete :error-type :error_type])
                                 :candidate-count total :candidates-omitted total)}
                4 12 96))))))))

(def workspace-required
  {:ok false :error-type :mission-workspace-required
   :error "Supply --workspace with the workspace root; --state-home only selects ledger storage."
   :example (command ["bin/mission" "help" "show"])})
