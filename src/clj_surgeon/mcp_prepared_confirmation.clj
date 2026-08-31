(ns clj-surgeon.mcp-prepared-confirmation
  "Bounded session state and pure projections for prepared confirmation/preview."
  (:require
   [clj-surgeon.mcp-contract :as contract]
   [clj-surgeon.mcp-prepared-request :as prepared-request]
   [clj-surgeon.structural-lens :as structural-lens]
   [clojure.set :as set]
   [clojure.string :as str])
  (:import
   (io.modelcontextprotocol.server McpAsyncServerExchange)
   (java.util UUID)))

(def ^:private ttl-ms 300000)
(def ^:private max-preview-count 3)
(def ^:private max-preview-diff-bytes 16384)
(def ^:private max-preview-diff-lines 256)
(def ^:private max-public-result-bytes 32768)

(def ^:private refusal-remedies
  {"invalid-prepared-confirmation" "Correct only the named invalid fields."
   "prepared-confirmation-unknown" "Run one eligible inspect_clojure read again."
   "prepared-confirmation-expired" "Run one eligible inspect_clojure read again."
   "prepared-confirmation-evicted" "Run one eligible inspect_clojure read again."
   "prepared-confirmation-consumed" "Run one eligible inspect_clojure read again; do not retry the old commit blind."
   "prepared-confirmation-hash-collision" "Use explicit ordinary edit_clojure arguments for this server boot."
   "prepared-confirmation-hole-mismatch" "Fill exactly the returned expected paths."
   "prepared-confirmation-snapshot-drift" "Run one eligible inspect_clojure read again."
   "prepared-confirmation-preview-limit" "Commit from current evidence or run one eligible inspect_clojure read again."
   "prepared-preview-output-limit" "Use ordinary explicit review or narrow the prepared selection."})

(defn confirmation-refusal
  ([error-type failed-stage]
   (confirmation-refusal error-type failed-stage {}))
  ([error-type failed-stage data]
   (merge {:ok false
           :error_type error-type
           :error (str "Prepared confirmation refused at " failed-stage)
           :failed_stage failed-stage
           :source_unchanged true
           :mutation_attempted false
           :mutation_succeeded false
           :write_authority false
           :remedy (get refusal-remedies error-type
                        "Run one eligible inspect_clojure read again.")}
          data)))

;; @spec MCP-OP-PREP-ACT-002
;; @spec MCP-OP-PREP-ACT-017
(defn new-registry
  [{:keys [clock boot-epoch digest-fn
           per-session-live global-live
           per-session-tombstones global-tombstones]
    :or {clock #(quot (System/nanoTime) 1000000)
         boot-epoch (str (UUID/randomUUID))
         digest-fn prepared-request/descriptor-sha256
         per-session-live 32
         global-live 256
         per-session-tombstones 64
         global-tombstones 512}}]
  {:clock clock
   :boot-epoch boot-epoch
   :digest-fn digest-fn
   :per-session-live per-session-live
   :global-live global-live
   :per-session-tombstones per-session-tombstones
   :global-tombstones global-tombstones
   :lock (Object.)
   :state (atom {:enabled true :live {} :tombstones {}})})

(defonce process-registry (new-registry {}))

(defn reset-registry!
  ([] (reset-registry! process-registry))
  ([registry]
   (locking (:lock registry)
     (swap! (:state registry)
            #(assoc % :live {} :tombstones {}))
     {:ok true})))

;; @spec MCP-OP-PREP-ACT-004
;; @spec MCP-OP-PREP-ACT-017
(defn exchange-session-key
  [exchange]
  (when (instance? McpAsyncServerExchange exchange)
    (let [session-id (.sessionId ^McpAsyncServerExchange exchange)]
      (when-not (str/blank? session-id) session-id))))

(defn- live-order-key
  [[[_boot session digest] entry]]
  [(:expires-at entry) (:issued-at entry) digest session])

(defn- tombstone-order-key
  [[[_boot session digest] tombstone]]
  [(:expires-at tombstone) (:terminal-at tombstone) digest session])

(defn- entry-key
  [registry session digest]
  [(:boot-epoch registry) session digest])

(defn- tombstone
  [now reason]
  {:reason reason
   :terminal-at now
   :expires-at (+ now ttl-ms)})

(defn- add-tombstone
  [state registry session digest reason now]
  (assoc-in state [:tombstones (entry-key registry session digest)]
            (tombstone now reason)))

(defn- trim-map
  [entries limit order-key]
  (loop [entries entries]
    (if (<= (count entries) limit)
      entries
      (recur (dissoc entries (ffirst (sort-by order-key entries)))))))

(defn- trim-session-map
  [entries session limit order-key]
  (loop [entries entries]
    (let [session-entries (filter (fn [[[_ candidate _] _]]
                                    (= session candidate))
                                  entries)]
      (if (<= (count session-entries) limit)
        entries
        (recur (dissoc entries
                       (ffirst (sort-by order-key session-entries))))))))

(defn- trim-tombstones
  [state registry session]
  (update state :tombstones
          #(-> %
               (trim-session-map session (:per-session-tombstones registry)
                                 tombstone-order-key)
               (trim-map (:global-tombstones registry) tombstone-order-key))))

(defn- expire-state
  [registry state now]
  (let [expired (filter (fn [[_ entry]]
                          (<= (:expires-at entry) now))
                        (:live state))
        state (reduce (fn [current [[boot session digest] _]]
                        (-> current
                            (update :live dissoc [boot session digest])
                            (add-tombstone registry session digest :expired now)))
                      state expired)
        state (update state :tombstones
                      (fn [entries]
                        (into {} (remove (fn [[_ value]]
                                           (<= (:expires-at value) now))
                                         entries))))]
    (reduce (fn [current [[_ session _] _]]
              (trim-tombstones current registry session))
            state (:tombstones state))))

(defn- refresh-state!
  [registry]
  (let [now ((:clock registry))]
    (swap! (:state registry) #(expire-state registry % now))
    now))

(defn registry-stats
  [registry]
  (locking (:lock registry)
    (let [now (refresh-state! registry)
          {:keys [enabled live tombstones]} @(:state registry)]
      {:enabled enabled
       :boot-epoch (:boot-epoch registry)
       :now now
       :per-session-live (:per-session-live registry)
       :global-live (:global-live registry)
       :live-count (count live)
       :tombstone-count (count tombstones)
       :tombstone-fields (->> tombstones vals (mapcat keys) distinct vec)})))

(defn- confirmation-object
  [digest]
  {:descriptor_sha256 digest
   :expires_in_ms ttl-ms
   :session_bound true
   :commit_single_use true
   :executable false
   :write_authority false})

(defn- evict-live
  [state registry session now]
  (loop [state state]
    (let [session-entries (filter (fn [[[_ candidate _] _]]
                                    (= session candidate))
                                  (:live state))
          over-session? (> (count session-entries)
                           (:per-session-live registry))
          over-global? (> (count (:live state)) (:global-live registry))]
      (if-not (or over-session? over-global?)
        state
        (let [pool (if over-session? session-entries (:live state))
              [[boot evicted-session digest] _]
              (first (sort-by live-order-key pool))]
          (recur (-> state
                     (update :live dissoc [boot evicted-session digest])
                     (add-tombstone registry evicted-session digest :evicted now)
                     (trim-tombstones registry evicted-session))))))))

;; @spec MCP-OP-PREP-ACT-001
;; @spec MCP-OP-PREP-ACT-002
;; @spec MCP-OP-PREP-ACT-003
(defn register!
  [registry session-key descriptor file-hashes]
  (locking (:lock registry)
    (let [now (refresh-state! registry)
          digest ((:digest-fn registry) descriptor)
          descriptor-bytes (vec (prepared-request/canonical-json-bytes descriptor))
          key (entry-key registry session-key digest)
          prior (get-in @(:state registry) [:live key])]
      (cond
        (not (:enabled @(:state registry)))
        (confirmation-refusal "prepared-confirmation-hash-collision"
                              "registry-disabled")

        (and prior (not= descriptor-bytes (:descriptor-bytes prior)))
        (do
          (swap! (:state registry)
                 #(-> %
                      (assoc :enabled false)
                      (assoc :live {})
                      (assoc :tombstones {})))
          (confirmation-refusal "prepared-confirmation-hash-collision"
                                "descriptor-hash"))

        :else
        (let [entry {:descriptor descriptor
                     :descriptor-bytes descriptor-bytes
                     :descriptor-sha256 digest
                     :workspace-root (get-in descriptor [:arguments :workspace_root])
                     :file-hashes file-hashes
                     :caller-holes (:caller_holes descriptor)
                     :issued-at now
                     :expires-at (+ now ttl-ms)
                     :preview-count (or (:preview-count prior) 0)
                     :lifecycle :live}]
          (swap! (:state registry)
                 (fn [state]
                   (-> state
                       (assoc-in [:live key] entry)
                       (update :tombstones dissoc key)
                       (evict-live registry session-key now))))
          (assoc (confirmation-object digest) :ok true))))))

(defn- reason->error-type
  [reason]
  (case reason
    :expired "prepared-confirmation-expired"
    :evicted "prepared-confirmation-evicted"
    :consumed "prepared-confirmation-consumed"
    "prepared-confirmation-unknown"))

;; @spec MCP-OP-PREP-ACT-002
;; @spec MCP-OP-PREP-ACT-004
;; @spec MCP-OP-PREP-ACT-008
(defn lookup!
  ([registry session-key digest]
   (lookup! registry session-key digest nil))
  ([registry session-key digest _mutation-succeeded]
   (locking (:lock registry)
     (refresh-state! registry)
     (let [state @(:state registry)
           key (entry-key registry session-key digest)]
       (cond
         (not (:enabled state))
         (confirmation-refusal "prepared-confirmation-hash-collision"
                               "registry-disabled")

         (get-in state [:live key])
         (let [entry (get-in state [:live key])]
           (assoc entry :ok true :expires_at (:expires-at entry)))

         (get-in state [:tombstones key])
         (confirmation-refusal
           (reason->error-type (get-in state [:tombstones key :reason]))
           "registry-lookup")

         :else
         (confirmation-refusal "prepared-confirmation-unknown"
                               "registry-lookup"))))))

;; @spec MCP-OP-PREP-ACT-007
;; @spec MCP-OP-PREP-ACT-008
;; @spec MCP-OP-PREP-ACT-013
(defn consume!
  [registry session-key digest]
  (locking (:lock registry)
    (let [lookup (lookup! registry session-key digest)]
      (if-not (:ok lookup)
        lookup
        (let [now ((:clock registry))
              key (entry-key registry session-key digest)]
          (swap! (:state registry)
                 #(-> %
                      (update :live dissoc key)
                      (add-tombstone registry session-key digest :consumed now)
                      (trim-tombstones registry session-key)))
          {:ok true :descriptor_sha256 digest :consumed true})))))

(defn expire!
  [registry session-key digest]
  (locking (:lock registry)
    (let [now (refresh-state! registry)
          key (entry-key registry session-key digest)]
      (if-not (get-in @(:state registry) [:live key])
        (lookup! registry session-key digest)
        (do
          (swap! (:state registry)
                 #(-> %
                      (update :live dissoc key)
                      (add-tombstone registry session-key digest :expired now)
                      (trim-tombstones registry session-key)))
          {:ok true :expired true :descriptor_sha256 digest})))))

;; @spec MCP-OP-PREP-ACT-013
(defn use-preview!
  [registry session-key digest]
  (locking (:lock registry)
    (let [lookup (lookup! registry session-key digest)]
      (if-not (:ok lookup)
        lookup
        (if (>= (:preview-count lookup) max-preview-count)
          (confirmation-refusal "prepared-confirmation-preview-limit"
                                "preview-count"
                                {:preview_count (:preview-count lookup)
                                 :allowed_preview_count max-preview-count})
          (let [key (entry-key registry session-key digest)
                count (inc (:preview-count lookup))]
            (swap! (:state registry) assoc-in [:live key :preview-count] count)
            {:ok true
             :descriptor_sha256 digest
             :preview_count count
             :expires_at (:expires-at lookup)}))))))

;; @spec MCP-OP-PREP-ACT-002
;; @spec MCP-OP-PREP-ACT-004
(defn end-session!
  [registry session-key]
  (locking (:lock registry)
    (swap! (:state registry)
           (fn [state]
             (-> state
                 (update :live
                         #(into {} (remove (fn [[[_ session _] _]]
                                             (= session-key session)) %)))
                 (update :tombstones
                         #(into {} (remove (fn [[[_ session _] _]]
                                             (= session-key session)) %))))))
    {:ok true}))

;; @spec MCP-OP-PREP-ACT-001
;; @spec MCP-OP-PREP-ACT-014
(defn attach-confirmation!
  [registry session-key prepared-result result-byte-count]
  (if-not (and session-key (:prepared_request prepared-result))
    prepared-result
    (let [descriptor (:prepared_request prepared-result)
          digest ((:digest-fn registry) descriptor)
          candidate (assoc prepared-result :prepared_confirmation
                           (confirmation-object digest))]
      (if (> (result-byte-count candidate) max-public-result-bytes)
        prepared-result
        (let [registered (register! registry session-key descriptor
                                    (:file_hashes prepared-result))]
          (if (:ok registered)
            candidate
            registered))))))

(defn public-keyword-map
  "Recursively normalize SDK JSON containers and keywordize public envelope keys."
  [value]
  (let [value (contract/json-containers->clj value)]
    (if-not (map? value)
      value
      (into (array-map)
            (map (fn [[key child]]
                   [(keyword (if (keyword? key) (name key) (str key))) child]))
            value))))

;; @spec MCP-OP-PREP-ACT-005
;; @spec MCP-OP-PREP-ACT-008
(defn validate-confirm-request
  [request]
  (let [request (public-keyword-map request)
        allowed #{:confirm :fill :preview}
        supplied (set (keys request))
        unknown (sort (map name (set/difference supplied allowed)))
        confirm (:confirm request)
        fill (:fill request)
        fill-valid? (and (map? fill)
                         (seq fill)
                         (every? (fn [[path replacement]]
                                   (and (string? path)
                                        (string? replacement)
                                        (not (str/blank? replacement))))
                                 fill))
        preview-present? (contains? request :preview)]
    (if (and (= #{:confirm :fill} (set/intersection supplied #{:confirm :fill}))
             (empty? unknown)
             (string? confirm)
             (re-matches #"[0-9a-f]{64}" confirm)
             fill-valid?
             (or (not preview-present?) (true? (:preview request))))
      {:ok true
       :confirm confirm
       :fill fill
       :preview (true? (:preview request))}
      (confirmation-refusal "invalid-prepared-confirmation"
                            "request-shape"
                            {:invalid_fields
                             (vec (concat unknown
                                          (when-not (and (string? confirm)
                                                         (re-matches #"[0-9a-f]{64}"
                                                                     confirm))
                                            ["confirm"])
                                          (when-not fill-valid? ["fill"])
                                          (when (and preview-present?
                                                     (not (true? (:preview request))))
                                            ["preview"])))
                             :supplied_fields (mapv name (keys request))}))))

;; @spec MCP-OP-PREP-ACT-005
;; @spec MCP-OP-PREP-ACT-008
(defn validate-holes
  [expected fill]
  (let [provided (mapv #(if (keyword? %) (name %) (str %)) (keys fill))
        expected-set (set expected)
        provided-set (set provided)
        missing (filterv #(not (contains? provided-set %)) expected)
        extra (filterv #(not (contains? expected-set %)) provided)]
    (if (and (empty? missing)
             (empty? extra)
             (= (count expected) (count provided)))
      {:ok true :expected expected :provided provided}
      (confirmation-refusal "prepared-confirmation-hole-mismatch"
                            "caller-holes"
                            {:expected expected
                             :provided provided
                             :missing missing
                             :extra extra}))))

;; @spec MCP-OP-PREP-ACT-006
(defn reconstruct-arguments
  [descriptor fill]
  (reduce (fn [arguments [path replacement]]
            (let [[_ index] (re-matches #"arguments\.edits\[([0-5])\]\.to"
                                        (if (keyword? path) (name path) path))]
              (assoc-in arguments [:edits (parse-long index) :to] replacement)))
          (:arguments descriptor)
          fill))

;; @spec MCP-OP-PREP-ACT-006
;; @spec MCP-OP-PREP-ACT-013
(defn validate-snapshot
  [expected actual]
  (if (= expected actual)
    {:ok true :file_count (count expected)}
    (confirmation-refusal "prepared-confirmation-snapshot-drift"
                          "snapshot-hash-map"
                          {:expected_file_count (count expected)
                           :actual_file_count (count actual)
                           :mismatched_file_count
                           (count (set/union
                                    (set (for [[file hash] expected
                                               :when (not= hash (get actual file))]
                                           file))
                                    (set (for [file (keys actual)
                                               :when (not (contains? expected file))]
                                           file))))})))

;; @spec MCP-OP-PREP-ACT-012
(defn verification-forecast []
  {:will_run false
   :profile nil
   :reason "edit_clojure-does-not-authorize-transaction-verification"})

(defn- changed-characters
  [old-source new-source]
  (let [prefix (count (take-while true? (map = old-source new-source)))
        old-rest (subs old-source prefix)
        new-rest (subs new-source prefix)
        suffix (count (take-while true?
                                  (map = (reverse old-rest) (reverse new-rest))))]
    (+ (- (count old-rest) suffix)
       (- (count new-rest) suffix))))

(defn- diff-lines
  [prefix source]
  (map #(str prefix %) (str/split source #"\\n" -1)))

(defn- complete-diff
  [sources future-sources]
  (->> (sort (keys sources))
       (keep (fn [file]
               (let [before (get sources file)
                     after (get future-sources file)]
                 (when (not= before after)
                   (str/join "\n"
                             (concat [(str "--- a/" file)
                                      (str "+++ b/" file)
                                      (str "@@ -1," (count (str/split before #"\\n" -1))
                                           " +1," (count (str/split after #"\\n" -1)) " @@")]
                                     (diff-lines "-" before)
                                     (diff-lines "+" after)))))))
       (str/join "\n")))

;; @spec MCP-OP-PREP-ACT-009
;; @spec MCP-OP-PREP-ACT-010
;; @spec MCP-OP-PREP-ACT-012
(defn preview-result
  [{:keys [descriptor-sha256 fill snapshot-guards sources future-sources]}]
  (let [changed (filterv #(not= (get sources %) (get future-sources %))
                         (sort (keys sources)))
        base {:ok true
              :operation "edit_clojure-preview"
              :lifecycle "preview"
              :committed false
              :mutation_attempted false
              :write_authority false
              :receipt false
              :source_unchanged true
              :descriptor_sha256 descriptor-sha256
              :fill_sha256 (prepared-request/descriptor-sha256 fill)
              :snapshot_guards snapshot-guards
              :future_file_hashes
              (into (sorted-map)
                    (map (fn [[file source]]
                           [file (structural-lens/source-hash source)]))
                    future-sources)
              :changed_files (count changed)
              :changed_characters
              (reduce + (map #(changed-characters (get sources %) (get future-sources %))
                             changed))
              :diff (complete-diff sources future-sources)
              :verification_forecast (verification-forecast)
              :next_action "none"}]
    (assoc base :preview_sha256
           (prepared-request/descriptor-sha256 base))))

(defn- utf8-bytes [value]
  (count (.getBytes (str value) "UTF-8")))

(defn- line-count [value]
  (if (empty? value) 0 (inc (count (filter #(= % \newline) value)))))

;; @spec MCP-OP-PREP-ACT-011
(defn enforce-preview-bounds
  [result result-byte-count]
  (let [diff (:diff result)
        diff-bytes (utf8-bytes diff)
        diff-lines (line-count diff)
        result-bytes (result-byte-count result)]
    (if (and (<= diff-bytes max-preview-diff-bytes)
             (<= diff-lines max-preview-diff-lines)
             (<= result-bytes max-public-result-bytes))
      result
      (confirmation-refusal
        "prepared-preview-output-limit" "preview-output-budget"
        {:required {:diff_bytes diff-bytes
                    :diff_lines diff-lines
                    :public_result_bytes result-bytes}
         :limits {:diff_bytes max-preview-diff-bytes
                  :diff_lines max-preview-diff-lines
                  :public_result_bytes max-public-result-bytes}}))))

;; @spec MCP-OP-PREP-ACT-009
;; @spec MCP-OP-PREP-ACT-017
(defn compile-preview
  [_effect-capabilities
   {:keys [compile-fn] :as request}]
  (let [compiled (compile-fn request)]
    (if-not (:ok compiled)
      compiled
      (let [result (preview-result
                     (assoc request :future-sources (:future-sources compiled)))]
        (enforce-preview-bounds
          result #(count (prepared-request/canonical-json-bytes %)))))))

(def ^:private telemetry-allowlist
  #{:eligible :emitted :digest :session-lifecycle-class :age-bucket
    :preview-count :refusal-class :request-bytes :response-bytes
    :phase-clocks :receipt-hash})

;; @spec MCP-OP-PREP-ACT-015
(defn telemetry-fields
  [fields]
  (select-keys fields telemetry-allowlist))

;; @spec MCP-OP-PREP-ACT-016
;; @spec MCP-OP-PREP-ACT-018
(defn promotion-status
  [evidence]
  {:w1 (if (and (:same-task-bytes-and-tokens evidence)
                (:counterbalanced-live-cohort evidence))
         "measured"
         "projected")
   :w2 (if (and (:exact-preview-facts evidence)
                (:zero-effects evidence)
                (:no-incorrect-commit-increase evidence)
                (:no-stale-retry-increase evidence))
         "promotion-eligible"
         "unpromoted")
   :install_authorized
   (boolean (and (:frozen-red evidence)
                 (:implementation evidence)
                 (:surgeon2-verification evidence)
                 (:measurement evidence)
                 (:rollback evidence)
                 (:live-proof evidence)
                 (:gene-install-approval evidence)))})
