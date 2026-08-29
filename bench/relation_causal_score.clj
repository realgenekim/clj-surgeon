(ns relation-causal-score
  "Pure, fail-closed scorer for the EDIT-025 N/R causal cohort.

  The imperative runner owns capture and independent proof production. This
  namespace accepts only explicit row data, validates its joins and identities,
  derives both clocks, and applies the frozen counterbalanced gates."
  (:require
   [clojure.string :as str]))

(def expected-run-manifest
  [{:run-id "b1-n1" :block 1 :position 1 :arm :N}
   {:run-id "b1-r1" :block 1 :position 2 :arm :R}
   {:run-id "b1-r2" :block 1 :position 3 :arm :R}
   {:run-id "b1-n2" :block 1 :position 4 :arm :N}
   {:run-id "b2-r1" :block 2 :position 1 :arm :R}
   {:run-id "b2-n1" :block 2 :position 2 :arm :N}
   {:run-id "b2-n2" :block 2 :position 3 :arm :N}
   {:run-id "b2-r2" :block 2 :position 4 :arm :R}])

(def ^:private sha256-pattern #"[0-9a-f]{64}")

(defn- sha256? [value]
  (and (string? value)
       (boolean (re-matches sha256-pattern value))))

(defn- value
  "Read one field from a keyword- or JSON-string-keyed map."
  [m key]
  (when (map? m)
    (if (contains? m key)
      (get m key)
      (get m (name key)))))

(defn- present? [m key]
  (and (map? m)
       (or (contains? m key)
           (contains? m (name key)))))

(defn- token [x]
  (when (some? x)
    (-> (if (keyword? x) (name x) (str x))
        (str/replace #"[_.]" "-")
        keyword)))

(defn- finite-number? [x]
  (and (number? x)
       (Double/isFinite (double x))))

(defn- positive-finite? [x]
  (and (finite-number? x) (pos? (double x))))

(defn- canonical-workspace? [workspace]
  (and (string? workspace)
       (str/starts-with? workspace "/")
       (or (= workspace "/")
           (not (str/ends-with? workspace "/")))
       (let [parts (str/split workspace #"/" -1)]
         (and (= "" (first parts))
              (every? #(and (not (str/blank? %))
                            (not (contains? #{"." ".."} %)))
                      (rest parts))))))

(defn- evidence-complete? [evidence]
  (and (map? evidence)
       (every? #(sha256? (value evidence %))
               [:canonical-transaction-sha256
                :future-hashes-sha256
                :receipt-sha256
                :read-back-sha256])
       (= 51 (value evidence :edit-count))
       (= 9 (value evidence :file-count))
       (true? (value evidence :verification-complete))
       (= :none (token (value evidence :next-action)))
       (let [verifier (value evidence :verifier)]
         (and (map? verifier)
              (sha256? (value verifier :profile-sha256))
              (sha256? (value verifier :output-sha256))
              (= 0 (value verifier :exit))))))

(defn- event-kind [event]
  (token (value event :event)))

(defn- item-type [event]
  (token (value (value event :item) :type)))

(defn- mcp-event? [event]
  (= :mcp-tool-call (item-type event)))

(defn- action-event? [event]
  (contains? #{:mcp-tool-call :command-execution :file-change :agent-message}
             (item-type event)))

(defn- forbidden-event? [event]
  (contains? #{:command-execution :file-change}
             (item-type event)))

(defn- event-time [event]
  (value event :observer-monotonic-ns))

(defn- apply-start? [event]
  (let [item (value event :item)]
    (and (= :item-started (event-kind event))
         (= :mcp-tool-call (item-type event))
         (= "clj-surgeon" (value item :server))
         (= "apply_clojure_changes" (value item :tool)))))

(defn- representation-valid? [arm arguments]
  (let [symbol? (present? arguments :symbol_migration)
        require? (present? arguments :require_change)]
    (case arm
      :N (and (not symbol?) (not require?))
      :R (and symbol? require?
              (map? (value arguments :symbol_migration))
              (map? (value arguments :require_change)))
      false)))

(defn- add-error [errors condition error]
  (cond-> errors condition (conj error)))

;; @spec MCP-OP-EDIT-025
(defn score-run
  "Validate one retained attempt and derive T_emit and T_verified.

  `:expected-evidence` is the independently frozen oracle. The single joined
  apply completion must carry an exactly equal, structurally complete
  `:evidence` map. No summary `:correct` flag is accepted as authority."
  [row]
  (let [events (vec (:events row))
        turn-start-events (filterv #(= :turn-started (event-kind %)) events)
        turn-complete-events (filterv #(= :turn-completed (event-kind %)) events)
        actions (filterv action-event? events)
        mcp-starts (filterv #(and (mcp-event? %)
                                  (= :item-started (event-kind %)))
                            events)
        mcp-completions (filterv #(and (mcp-event? %)
                                       (= :item-completed (event-kind %)))
                                 events)
        start (first mcp-starts)
        completion (first mcp-completions)
        start-index (first (keep-indexed #(when (identical? %2 start) %1) events))
        completion-index (first (keep-indexed #(when (identical? %2 completion) %1)
                                              events))
        start-item (value start :item)
        completion-item (value completion :item)
        arguments (value start-item :arguments)
        actual-evidence (value completion-item :evidence)
        expected-evidence (:expected-evidence row)
        workspace (:workspace-root row)
        turn-start (get-in row [:clocks :turn-start-ns])
        turn-completed (get-in row [:clocks :turn-completed-ns])
        call-start (event-time start)
        call-completed (event-time completion)
        clock-order? (and (every? finite-number?
                                  [turn-start call-start call-completed turn-completed])
                          (< (double turn-start) (double call-start))
                          (< (double call-start) (double call-completed))
                          (<= (double call-completed) (double turn-completed)))
        t-emit (when clock-order?
                 (/ (- (double call-start) (double turn-start)) 1000000.0))
        t-verified (when clock-order?
                     (/ (- (double turn-completed) (double turn-start)) 1000000.0))
        event-times (mapv event-time events)
        event-order? (and (every? finite-number? event-times)
                          (every? (fn [[left right]]
                                    (<= (double left) (double right)))
                                  (partition 2 1 event-times)))
        turn-events-valid?
        (and (= 1 (count turn-start-events))
             (= 1 (count turn-complete-events))
             (= turn-start (event-time (first turn-start-events)))
             (= turn-completed (event-time (first turn-complete-events)))
             (= :turn-started (event-kind (first events)))
             (= :turn-completed (event-kind (last events))))
        first-action (first actions)
        errors (-> []
                   (add-error (not turn-events-valid?)
                              :turn-lifecycle-invalid)
                   (add-error (not event-order?)
                              :event-order-invalid)
                   (add-error (not (apply-start? first-action))
                              :first-action-not-apply)
                   (add-error (or (not= 1 (count mcp-starts))
                                  (not= 1 (count mcp-completions)))
                              :mcp-call-count-invalid)
                   (add-error (or (nil? start) (nil? completion)
                                  (not= (value start-item :id)
                                        (value completion-item :id)))
                              :call-id-mismatch)
                   (add-error (or (nil? start) (nil? completion)
                                  (not= :completed
                                        (token (value completion-item :status)))
                                  (and (number? start-index)
                                       (number? completion-index)
                                       (>= start-index completion-index))
                                  (and (finite-number? call-start)
                                       (finite-number? call-completed)
                                       (>= (double call-start)
                                           (double call-completed))))
                              :call-lifecycle-invalid)
                   (add-error (some forbidden-event? events)
                              :forbidden-action)
                   (add-error (not (representation-valid? (:arm row) arguments))
                              :representation-mismatch)
                   (add-error (not (canonical-workspace? workspace))
                              :workspace-not-canonical)
                   (add-error (not= workspace (value arguments :workspace_root))
                              :workspace-mismatch)
                   (add-error (not= "exact" (value arguments :verify))
                              :verify-not-exact)
                   (add-error (not (evidence-complete? actual-evidence))
                              :evidence-incomplete)
                   (add-error (not= expected-evidence actual-evidence)
                              :evidence-mismatch)
                   (add-error (or (not clock-order?)
                                  (not (positive-finite? t-emit))
                                  (not (positive-finite? t-verified)))
                              :clock-invalid))]
    {:ok (empty? errors)
     :run-id (:run-id row)
     :block (:block row)
     :position (:position row)
     :arm (:arm row)
     :errors (vec (distinct errors))
     :metrics {:t-emit-ms t-emit
               :t-verified-ms t-verified}
     :call-id (value start-item :id)
     :workspace-root workspace
     :evidence actual-evidence}))

(defn- median [xs]
  (let [values (vec (sort xs))
        n (count values)
        middle (quot n 2)]
    (when (pos? n)
      (if (odd? n)
        (nth values middle)
        (/ (+ (nth values (dec middle))
              (nth values middle))
           2.0)))))

(defn- improvement [n r]
  (when (and (positive-finite? n) (positive-finite? r))
    (/ (- n r) n)))

(defn- metric-median [scores arm metric]
  (median (map #(get-in % [:metrics metric])
               (filter (comp #{arm} :arm) scores))))

(defn- comparison [scores]
  (let [n-emit (metric-median scores :N :t-emit-ms)
        r-emit (metric-median scores :R :t-emit-ms)
        n-verified (metric-median scores :N :t-verified-ms)
        r-verified (metric-median scores :R :t-verified-ms)]
    {:n-t-emit-median-ms n-emit
     :r-t-emit-median-ms r-emit
     :t-emit-improvement (improvement n-emit r-emit)
     :n-t-verified-median-ms n-verified
     :r-t-verified-median-ms r-verified
     :t-verified-improvement (improvement n-verified r-verified)}))

(defn- manifest-for [count]
  (when (contains? #{4 8} count)
    (subvec expected-run-manifest 0 count)))

;; @spec MCP-OP-EDIT-025
(defn cohort-report
  "Score Block 1 or the complete EDIT-025 cohort.

  Exactly four rows may authorize Block 2. Exactly eight rows may promote.
  Any missing, extra, duplicate, reordered, or invalid row fails closed."
  [rows]
  (let [rows (vec rows)
        manifest (manifest-for (count rows))
        identities (mapv #(select-keys % [:run-id :block :position :arm]) rows)
        manifest-exact? (= manifest identities)
        unique-run-ids? (= (count rows) (count (set (map :run-id rows))))
        scores (mapv score-run rows)
        runs-valid? (and manifest-exact? unique-run-ids?
                         (every? :ok scores))
        same-evidence? (and runs-valid?
                            (= 1 (count (set (map :expected-evidence rows)))))
        unique-workspaces? (and runs-valid?
                                (= (count rows)
                                   (count (set (map :workspace-root rows)))))
        block-1-scores (filterv (comp #{1} :block) scores)
        block-2-scores (filterv (comp #{2} :block) scores)
        block-1 (when (and runs-valid? (= 4 (count block-1-scores)))
                  (comparison block-1-scores))
        block-2 (when (and runs-valid? (= 4 (count block-2-scores)))
                  (comparison block-2-scores))
        pooled (when (and runs-valid? (= 8 (count scores)))
                 (comparison scores))
        block-2-authorized?
        (and runs-valid? same-evidence? unique-workspaces? block-1
             (>= (or (:t-verified-improvement block-1) -1.0) 0.15)
             (pos? (or (:t-emit-improvement block-1) -1.0)))
        promote?
        (and block-2-authorized? (= 8 (count rows)) block-2 pooled
             (>= (or (:t-emit-improvement block-1) -1.0) 0.20)
             (>= (or (:t-emit-improvement block-2) -1.0) 0.20)
             (>= (or (:t-emit-improvement pooled) -1.0) 0.20)
             (pos? (or (:t-verified-improvement block-1) -1.0))
             (pos? (or (:t-verified-improvement block-2) -1.0))
             (>= (or (:t-verified-improvement pooled) -1.0) 0.20))
        errors (cond-> []
                 (nil? manifest) (conj :row-count-invalid)
                 (and manifest (not manifest-exact?)) (conj :manifest-mismatch)
                 (not unique-run-ids?) (conj :duplicate-run-id)
                 (not (every? :ok scores)) (conj :invalid-run)
                 (and runs-valid? (not same-evidence?))
                 (conj :evidence-identity-drift)
                 (and runs-valid? (not unique-workspaces?))
                 (conj :workspace-reused))]
    {:schema :clj-surgeon.edit-025-relation-causal-cohort/v1
     :ok (and runs-valid? same-evidence? unique-workspaces?)
     :run-count (count rows)
     :errors errors
     :runs scores
     :blocks {1 block-1 2 block-2}
     :pooled pooled
     :gate {:block-2-authorized (boolean block-2-authorized?)
            :promote (boolean promote?)
            :minimum-block-1-verified-improvement 0.15
            :minimum-final-emit-improvement 0.20
            :minimum-pooled-verified-improvement 0.20}}))
