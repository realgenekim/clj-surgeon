(when-not (find-ns 'relation-causal-corpus)
  ;; Direct `bb bench/relation_causal_score.clj` does not add sibling scripts
  ;; to the classpath. Load the frozen public corpus before declaring this ns.
  (load-file
    (.getPath
      (java.io.File. (.getParentFile (java.io.File. *file*))
                     "relation_causal_corpus.clj"))))

(ns relation-causal-score
  "Pure, fail-closed scorer for the EDIT-025 N/R causal cohort.

  The imperative runner owns capture and independent proof production. This
  namespace accepts only explicit row data, validates its joins and identities,
  derives both clocks, and applies the frozen counterbalanced gates."
  (:require
   [cheshire.core :as json]
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [relation-causal-corpus :as corpus])
  (:import
   (java.nio.charset StandardCharsets)
   (java.nio.file Files Paths)
   (java.security MessageDigest)))

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

(defn- sha256 [text]
  (let [digest (MessageDigest/getInstance "SHA-256")]
    (.update digest (.getBytes ^String text StandardCharsets/UTF_8))
    (apply str (map #(format "%02x" (bit-and 0xff %)) (.digest digest)))))

(defn- canonical-data [x]
  (cond
    (map? x) (into (sorted-map-by #(compare (str %1) (str %2)))
                   (map (fn [[key child]] [key (canonical-data child)]))
                   x)
    (vector? x) (mapv canonical-data x)
    (sequential? x) (mapv canonical-data x)
    :else x))

(defn canonical-sha256
  "Hash one EDN value after recursive deterministic map ordering."
  [x]
  (sha256 (pr-str (canonical-data x))))

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

(defn- evidence-oracle-complete? [evidence]
  (and (map? evidence)
       (every? #(sha256? (value evidence %))
               [:canonical-transaction-sha256
                :future-hashes-sha256
                :read-back-sha256])
       (sha256? (get-in evidence [:verifier :profile-sha256]))))

(defn- evidence-matches-oracle? [expected actual]
  (and (evidence-oracle-complete? expected)
       (= (select-keys expected
                       [:canonical-transaction-sha256
                        :future-hashes-sha256
                        :read-back-sha256])
          (select-keys actual
                       [:canonical-transaction-sha256
                        :future-hashes-sha256
                        :read-back-sha256]))
       (= (get-in expected [:verifier :profile-sha256])
          (get-in actual [:verifier :profile-sha256]))))

(defn- stable-evidence [evidence]
  {:canonical-transaction-sha256
   (:canonical-transaction-sha256 evidence)
   :future-hashes-sha256 (:future-hashes-sha256 evidence)
   :read-back-sha256 (:read-back-sha256 evidence)
   :verifier-profile-sha256 (get-in evidence [:verifier :profile-sha256])})

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
        expected-arguments (:expected-arguments row)
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
             (< (.indexOf events (first turn-start-events))
                (.indexOf events (first turn-complete-events))))
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
                   (add-error (not (map? expected-arguments))
                              :request-evidence-missing)
                   (add-error (not= expected-arguments arguments)
                              :request-evidence-mismatch)
                   (add-error (not (evidence-complete? actual-evidence))
                              :evidence-incomplete)
                   (add-error (not (evidence-matches-oracle?
                                     expected-evidence actual-evidence))
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
                            (= 1 (count (set (map (comp stable-evidence :evidence)
                                                  scores)))))
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

(defn- read-record-bytes [path]
  (let [bytes (Files/readAllBytes (Paths/get path (make-array String 0)))
        length (alength bytes)]
    (loop [start 0 index 0 records []]
      (cond
        (= index length)
        (cond-> records (< start length)
                (conj (java.util.Arrays/copyOfRange bytes start length)))

        (= 10 (bit-and 0xff (aget bytes index)))
        (recur (inc index) (inc index)
               (conj records
                     (java.util.Arrays/copyOfRange bytes start (inc index))))

        :else
        (recur start (inc index) records)))))

(defn- record-text [record]
  (let [length (alength record)
        without-lf (if (and (pos? length)
                            (= 10 (bit-and 0xff (aget record (dec length)))))
                     (dec length)
                     length)
        without-cr (if (and (pos? without-lf)
                            (= 13 (bit-and 0xff
                                           (aget record (dec without-lf)))))
                     (dec without-lf)
                     without-lf)]
    (String. record 0 without-cr StandardCharsets/UTF_8)))

(defn- parse-long! [text field]
  (try
    (Long/parseLong text)
    (catch Exception _
      (throw (ex-info "Invalid event-clock integer"
                      {:field field :value text})))))

(defn- read-clock-rows [path]
  (with-open [reader (io/reader path)]
    (mapv (fn [line]
            (let [parts (str/split line #"\t" -1)]
              (when-not (= 4 (count parts))
                (throw (ex-info "Invalid event-clock row" {:line line})))
              (let [[sequence monotonic utc bytes] parts]
                {:sequence (parse-long! sequence :sequence)
                 :observer-monotonic-ns (parse-long! monotonic :monotonic)
                 :observer-utc-ms (parse-long! utc :utc)
                 :line-byte-count (parse-long! bytes :bytes)})))
          (line-seq reader))))

(defn- raw-event-kind [event]
  (case (:type event)
    "thread.started" :thread-started
    "turn.started" :turn-started
    "turn.completed" :turn-completed
    "item.started" :item-started
    "item.completed" :item-completed
    "item.updated" :item-updated
    :unknown-event))

(defn join-event-clock-files
  "Join raw Codex JSONL to the observer clock by sequence and exact line bytes."
  [events-path clock-path]
  (try
    (let [records (read-record-bytes events-path)
          clocks (read-clock-rows clock-path)]
      (when-not (= (count records) (count clocks))
        (throw (ex-info "Event and clock counts differ"
                        {:event-count (count records)
                         :clock-count (count clocks)})))
      {:ok true
       :events
       (mapv
         (fn [index record clock]
           (let [expected-sequence (inc index)]
             (when-not (= expected-sequence (:sequence clock))
               (throw (ex-info "Event clock sequence is not contiguous"
                               {:expected expected-sequence
                                :actual (:sequence clock)})))
             (when-not (= (alength record) (:line-byte-count clock))
               (throw (ex-info "Event and clock byte counts differ"
                               {:sequence expected-sequence
                                :event-bytes (alength record)
                                :clock-bytes (:line-byte-count clock)})))
             (let [raw (try
                         (json/parse-string (record-text record) true)
                         (catch Exception error
                           (throw (ex-info "Invalid JSON event"
                                           {:sequence expected-sequence}
                                           error))))]
               {:event (raw-event-kind raw)
                :observer-monotonic-ns (:observer-monotonic-ns clock)
                :observer-utc-ms (:observer-utc-ms clock)
                :sequence expected-sequence
                :item (:item raw)})))
         (range) records clocks)})
    (catch Exception error
      {:ok false
       :errors [:artifact-join-failed]
       :error (.getMessage error)
       :data (ex-data error)})))

(defn- normalize-workspace-paths [workspace value]
  (let [prefix (str workspace java.io.File/separator)]
    (cond
      (map? value) (into {} (map (fn [[key child]]
                                   [key (normalize-workspace-paths workspace child)]))
                         value)
      (vector? value) (mapv #(normalize-workspace-paths workspace %) value)
      (sequential? value) (mapv #(normalize-workspace-paths workspace %) value)
      (and (string? value) (str/starts-with? value prefix))
      (subs value (count prefix))
      :else value)))

(defn- json-key [key]
  (if (keyword? key)
    (if-let [namespace (namespace key)]
      (str namespace "/" (name key))
      (name key))
    key))

(defn- json-object-keys [value]
  (cond
    (map? value) (into {} (map (fn [[key child]]
                                 [(json-key key) (json-object-keys child)]))
                       value)
    (vector? value) (mapv json-object-keys value)
    (sequential? value) (mapv json-object-keys value)
    :else value))

(defn- receipt-evidence [workspace structured]
  (try
    (let [receipt-path (value structured :undo_receipt)
          public-receipt-hash (value structured :receipt_hash)
          public-read-back
          (->> (value structured :read_back_hashes)
               json-object-keys
               (normalize-workspace-paths workspace))
          verification (value structured :verification)]
      (when-not (and (canonical-workspace? receipt-path)
                     (canonical-workspace? workspace)
                     (true? (value structured :ok))
                     (true? (value structured :committed))
                     (= "apply_clojure_changes"
                        (value structured :operation))
                     (= 9 (count public-read-back))
                     (sha256? public-receipt-hash))
        (throw (ex-info "Receipt or read-back evidence is incomplete"
                        {:receipt receipt-path
                         :receipt-hash public-receipt-hash
                         :read-back-count (count public-read-back)})))
      {:receipt-sha256 public-receipt-hash
       :read-back-sha256 (canonical-sha256 public-read-back)
       :read-back-hashes public-read-back
       :edit-count (value structured :edits)
       :file-count (value structured :files)
       :verification-complete (value structured :verification_complete)
       :next-action (token (value structured :next_action))
       :verifier {:profile-sha256 (value verification :profile-sha256)
                  :output-sha256 (value verification :output-sha256)
                  :exit (value verification :exit)}})
    (catch Exception error
      {:error :receipt-evidence-invalid
       :message (.getMessage error)
       :data (ex-data error)})))

(defn- compiled-request-evidence [arguments]
  (try
    (let [{:keys [sources]} (corpus/load-fixture)
          result (corpus/compile-request sources arguments)
          product (:compiled result)
          future-hashes (:future-hashes result)]
      (when-not (and (true? (get-in result [:public-schema :ok]))
                     (true? (get-in result [:runtime-contract :ok]))
                     (map? product)
                     (nil? (:error product))
                     (= 51 (:match-count product))
                     (= 9 (:changed-file-count product))
                     (= 9 (count future-hashes)))
        (throw (ex-info "Actual arguments do not compile to the frozen corpus"
                        {:public-schema (:public-schema result)
                         :runtime-contract (:runtime-contract result)
                         :compiled (select-keys product
                                                [:error :error-type
                                                 :match-count
                                                 :changed-file-count])})))
      {:canonical-transaction-sha256
       (canonical-sha256 (:canonical-transaction result))
       :future-hashes-sha256 (canonical-sha256 future-hashes)
       :future-hashes future-hashes})
    (catch Exception error
      {:error :request-compilation-invalid
       :message (.getMessage error)
       :data (ex-data error)})))

(defn- expected-arguments [arm workspace]
  (case arm
    :N (corpus/normalized-flat-request workspace)
    :R (corpus/closed-relation-request workspace)
    nil))

(defn- exact-profile-runtime-sha256 []
  (let [definition (get-in corpus/exact-profile
                           [:verification-profiles "exact"])]
    (sha256 (pr-str (into (sorted-map) definition)))))

(def ^:private expected-evidence-by-arm
  (delay
    (into {}
          (map (fn [arm]
                 (let [compiled
                       (compiled-request-evidence
                         (expected-arguments arm "/workspace"))]
                   [arm
                    (when-not (:error compiled)
                      {:canonical-transaction-sha256
                       (:canonical-transaction-sha256 compiled)
                       :future-hashes-sha256
                       (:future-hashes-sha256 compiled)
                       :read-back-sha256
                       (:future-hashes-sha256 compiled)
                       :verifier
                       {:profile-sha256
                        (exact-profile-runtime-sha256)}})])))
          [:N :R])))

(defn- expected-run-evidence [arm]
  (get @expected-evidence-by-arm arm))

(defn- structured-content [completion]
  (let [result (get-in completion [:item :result])]
    (or (value result :structured_content)
        (value result :structuredContent))))

(defn- terminal-receipt [path]
  (try
    (let [rows (with-open [reader (io/reader path)]
                 (mapv #(str/split % #"\t" -1) (line-seq reader)))
          pairs? (every? #(= 2 (count %)) rows)
          values (when pairs? (into {} rows))
          run-directory (some-> path io/file .getParentFile .getName)]
      (if (and pairs?
               (= 4 (count rows))
               (= 4 (count values))
               (= run-directory (get values "run_id"))
               (= "completed" (get values "state"))
               (= "0" (get values "exit_code"))
               (boolean (re-matches
                          #"[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9:]+Z"
                          (or (get values "finished_utc") ""))))
        {:ok true :values values}
        {:ok false :errors [:terminal-receipt-invalid] :values values}))
    (catch Exception error
      {:ok false
       :errors [:terminal-receipt-invalid]
       :error (.getMessage error)})))

(defn manifest-entry->row
  "Build one score-run row from a manifest entry and raw retained artifacts."
  [entry _default-expected-evidence]
  (let [declared-workspace (:workspace-root entry)
        artifacts (:artifacts entry)
        terminal (terminal-receipt (:terminal artifacts))
        joined (if (canonical-workspace? declared-workspace)
                 (if (:ok terminal)
                   (join-event-clock-files (:events artifacts)
                                           (:event-clock artifacts))
                   terminal)
                 {:ok false
                  :errors [:workspace-not-canonical]
                  :workspace-root declared-workspace})]
    (if-not (:ok joined)
      joined
      (let [events (:events joined)
            starts (filterv #(and (= :item-started (:event %))
                                  (= :mcp-tool-call
                                     (token (get-in % [:item :type]))))
                            events)
            start (first starts)
            call-id (get-in start [:item :id])
            completion (first (filter #(and (= :item-completed (:event %))
                                            (= call-id (get-in % [:item :id])))
                                      events))
            structured (structured-content completion)
            arguments (some-> (get-in start [:item :arguments])
                              json-object-keys)
            expected-arguments (expected-arguments (:arm entry)
                                                   declared-workspace)
            request-evidence (compiled-request-evidence arguments)
            commit-evidence (receipt-evidence (:workspace-root entry)
                                              structured)
            evidence
            (if (or (:error request-evidence)
                    (:error commit-evidence)
                    (not= (:future-hashes request-evidence)
                          (:read-back-hashes commit-evidence)))
              {:error :combined-evidence-invalid
               :request request-evidence
               :commit commit-evidence}
              (merge (dissoc request-evidence :future-hashes)
                     (dissoc commit-evidence :read-back-hashes)))
            enriched-events
            (mapv #(if (and (= :item-completed (:event %))
                            (= call-id (get-in % [:item :id])))
                     (assoc-in % [:item :evidence] evidence)
                     %)
                  events)
            turn-start (first (filter #(= :turn-started (:event %)) events))
            turn-completed (first (filter #(= :turn-completed (:event %)) events))]
        {:ok true
         :row {:run-id (:run-id entry)
               :block (:block entry)
               :position (:position entry)
               :arm (:arm entry)
               :workspace-root (:workspace-root entry)
               :expected-arguments expected-arguments
               :expected-evidence (expected-run-evidence (:arm entry))
               :clocks {:turn-start-ns (:observer-monotonic-ns turn-start)
                        :turn-completed-ns
                        (:observer-monotonic-ns turn-completed)}
               :events (mapv #(if (and (= :item-started (:event %))
                                       (= call-id (get-in % [:item :id])))
                                (assoc-in % [:item :arguments] arguments)
                                %)
                             enriched-events)}}))))

(defn manifest->rows [manifest]
  (let [entries (:runs manifest)
        mapped (mapv #(manifest-entry->row % nil) entries)
        failures (filterv (comp not :ok) mapped)]
    (if (seq failures)
      {:ok false
       :errors [:manifest-artifact-invalid]
       :failures failures}
      {:ok true :rows (mapv :row mapped)})))

(defn phase-report
  "Load one frozen phase from manifest data and apply its exact stop gate."
  [phase manifests]
  (let [mapped (mapv manifest->rows manifests)
        failures (filterv (comp not :ok) mapped)]
    (if (seq failures)
      {:schema :clj-surgeon.edit-025-relation-causal-phase/v1
       :phase phase
       :ok false
       :errors [:manifest-artifact-invalid]
       :failures failures}
      (let [rows (vec (mapcat :rows mapped))
            cohort (cohort-report rows)
            gate-passed (case phase
                          :block1 (get-in cohort [:gate :block-2-authorized])
                          :final (get-in cohort [:gate :promote])
                          false)]
        (assoc cohort
               :phase phase
               :cohort-valid (:ok cohort)
               :ok (boolean (and (:ok cohort) gate-passed)))))))

(defn- parse-options [args]
  (when (odd? (count args))
    (throw (ex-info "Expected --key value pairs" {:args args})))
  (into {}
        (map (fn [[key value]]
               (when-not (str/starts-with? key "--")
                 (throw (ex-info "Expected --key value pair" {:key key})))
               [(keyword (subs key 2)) value]))
        (partition 2 args)))

(defn run-cli! [args]
  (try
    (let [options (parse-options args)
          phase (keyword (:phase options))
          expected-keys (case phase
                          :block1 #{:phase :manifest :output}
                          :final #{:phase :block1-manifest
                                   :block2-manifest :output}
                          #{})]
      (when-not (= expected-keys (set (keys options)))
        (throw (ex-info "Invalid phase options"
                        {:phase phase :keys (set (keys options))})))
      (let [manifests (case phase
                        :block1 [(edn/read-string (slurp (:manifest options)))]
                        :final [(edn/read-string
                                  (slurp (:block1-manifest options)))
                                (edn/read-string
                                  (slurp (:block2-manifest options)))])
            report (phase-report phase manifests)]
        (spit (:output options) (str (pr-str report) "\n"))
        {:exit (if (:ok report) 0 1) :report report}))
    (catch Exception error
      {:exit 2
       :report {:schema :clj-surgeon.edit-025-relation-causal-phase/v1
                :ok false
                :errors [:invalid-cli-input]
                :error (.getMessage error)
                :data (ex-data error)}})))

(defn -main [& args]
  (let [{:keys [exit report]} (run-cli! args)]
    (when (and (pos? exit)
               (not (some #{"--output"} args)))
      (binding [*out* *err*]
        (println (pr-str report))))
    (when (pos? exit)
      (System/exit exit))))

(when-let [entry-file (System/getProperty "babashka.file")]
  (when (= (.getCanonicalPath (io/file *file*))
           (.getCanonicalPath (io/file entry-file)))
    (apply -main *command-line-args*)))
