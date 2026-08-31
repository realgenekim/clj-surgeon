(ns clj-surgeon.mcp-combinable-transaction
  "Bounded per-session memory of the last committed edit_clojure transaction,
  and the pure steering note that names an adjacent combinable pair.

  A caller that commits an edits-only transaction and then, seconds later, a
  create_files-only transaction in the same workspace has spent two receipts
  where one atomic call would have carried both under mutual rollback. The
  server notices that pair and says so in the second receipt.

  This is steering, not authority. The note never refuses, never alters `ok`,
  `committed`, or any other published field, never appears on a refused
  transaction, and argues atomicity rather than payload size. Anything the
  memory cannot decide leaves the result exactly as the kernel wrote it."
  (:require
   [clojure.string :as str])
  (:import
   (java.util Collection Map UUID)))

;; A transaction stops being one caller gesture long before it stops being
;; interesting, so the memo window is short: ten minutes.
(def ^:private ttl-ms 600000)

(def combinable-hint
  (str "these two transactions were combinable: one atomic call carrying both "
       "edits and create_files would have produced one receipt with mutual "
       "rollback"))

;; ---------------------------------------------------------------------------
;; Shape classification — the caller's supplied verbs, not the kernel's counts
;; ---------------------------------------------------------------------------

(def ^:private edits-side-verbs
  [:edits :programs :delete_owners :symbol_migration :require_change])

(def ^:private create-verb :create_files)

(defn- map-like?
  [value]
  (or (map? value) (instance? Map value)))

(defn- present?
  "Decide whether one supplied request field actually carries work."
  [value]
  (cond
    (nil? value) false
    (instance? Collection value) (not (.isEmpty ^Collection value))
    (instance? Map value) (not (.isEmpty ^Map value))
    (coll? value) (boolean (seq value))
    (string? value) (not (str/blank? value))
    :else true))

(defn- supplied?
  "Read one verb through both the keyword and the public JSON string key."
  [params verb]
  (or (present? (get params verb))
      (present? (get params (name verb)))))

(defn- prepared-confirmation?
  "A prepared confirmation carries no verbs of its own.

  Its reconstructed arguments are edits by construction: the prepared-request
  schema declares `arguments` with `additionalProperties false` and requires
  `edits`, so no create_files can reach this route."
  [params]
  (and (supplied? params :confirm) (supplied? params :fill)))

(defn transaction-shape
  "Name the verb shape of one edit_clojure request.

  Returns `:edits-only`, `:create-only`, `:mixed`, or nil when the request
  supplies no eligible work."
  [params]
  (when (map-like? params)
    (let [creates? (supplied? params create-verb)
          edits? (or (boolean (some #(supplied? params %) edits-side-verbs))
                     (prepared-confirmation? params))]
      (cond
        (and creates? edits?) :mixed
        creates? :create-only
        edits? :edits-only
        :else nil))))

;; ---------------------------------------------------------------------------
;; Bounded session memory
;; ---------------------------------------------------------------------------

(defn new-registry
  [{:keys [clock boot-epoch max-sessions]
    :or {clock #(quot (System/nanoTime) 1000000)
         boot-epoch (str (UUID/randomUUID))
         max-sessions 256}}]
  {:clock clock
   :boot-epoch boot-epoch
   :max-sessions max-sessions
   :lock (Object.)
   :state (atom {:memos {}})})

(defonce process-registry (new-registry {}))

(defn reset-registry!
  ([] (reset-registry! process-registry))
  ([registry]
   (locking (:lock registry)
     (swap! (:state registry) assoc :memos {})
     {:ok true})))

(defn- memo-key
  [registry session-key]
  [(:boot-epoch registry) session-key])

(defn- memo-order-key
  [[[_boot session] memo]]
  [(:expires-at memo) (:committed-at memo) session])

(defn- expire-memos
  [memos now]
  (into {} (remove (fn [[_ memo]] (<= (:expires-at memo) now)) memos)))

(defn- trim-memos
  [memos limit]
  (loop [memos memos]
    (if (<= (count memos) limit)
      memos
      (recur (dissoc memos (ffirst (sort-by memo-order-key memos)))))))

(defn registry-stats
  [registry]
  (locking (:lock registry)
    (let [now ((:clock registry))
          state (swap! (:state registry) update :memos expire-memos now)]
      {:boot-epoch (:boot-epoch registry)
       :now now
       :max-sessions (:max-sessions registry)
       :memo-count (count (:memos state))})))

;; ---------------------------------------------------------------------------
;; The note
;; ---------------------------------------------------------------------------

;; Both orders, written out. A set of the two shapes would read more compactly
;; and would throw `Duplicate key` the moment a caller repeats a shape.
(def ^:private combinable-shape-pairs
  #{[:edits-only :create-only]
    [:create-only :edits-only]})

(defn- nonblank-string?
  [value]
  (and (string? value) (not (str/blank? value))))

(defn- combinable-note
  "Project the steering note when the prior memo and this commit are the exact
  adjacent pair one atomic transaction would have carried."
  [prior {:keys [shape workspace-root]} now]
  (let [prior-live? (and (map? prior) (> (long (:expires-at prior 0)) (long now)))
        prior-receipt (:receipt-hash prior)
        prior-named? (nonblank-string? prior-receipt)
        same-workspace? (and (nonblank-string? workspace-root)
                             (= (:workspace-root prior) workspace-root))
        opposite-single-shapes? (contains? combinable-shape-pairs
                                           [(:shape prior) shape])]
    (when (and prior-live?
               prior-named?
               same-workspace?
               opposite-single-shapes?)
      {:prior_receipt_hash prior-receipt
       :hint combinable-hint})))

;; @spec MCP-OP-EDIT-032
(defn observe-transaction!
  "Record one terminal edit_clojure outcome and return the steering note, if any.

  `outcome` is `:committed`, `:refused`, or `:indeterminate`. A refusal forgets
  the session's memo, so only an uninterrupted adjacent pair is ever named. An
  indeterminate outcome — a preview, or any result that is neither a commit nor
  a refusal — leaves the memo exactly as it was."
  [registry session-key {:keys [outcome shape] :as transaction}]
  (when (nonblank-string? session-key)
    (locking (:lock registry)
      (let [now ((:clock registry))
            key (memo-key registry session-key)
            state (swap! (:state registry) update :memos expire-memos now)
            prior (get-in state [:memos key])]
        (case outcome
          :committed
          (when (contains? #{:edits-only :create-only :mixed} shape)
            (let [note (combinable-note prior transaction now)
                  memo {:shape shape
                        :workspace-root (:workspace-root transaction)
                        :receipt-hash (:receipt-hash transaction)
                        :committed-at now
                        :expires-at (+ now ttl-ms)}]
              (swap! (:state registry) update :memos
                     #(-> %
                          (assoc key memo)
                          (trim-memos (:max-sessions registry))))
              note))

          :refused
          (do (swap! (:state registry) update :memos dissoc key) nil)

          nil)))))

(defn- outcome-of
  [result]
  (cond
    (and (true? (:ok result)) (true? (:committed result))) :committed
    (false? (:ok result)) :refused
    :else :indeterminate))

;; @spec MCP-OP-EDIT-032
(defn attach-note!
  "Observe one terminal edit_clojure result and add the combinable steering note.

  The note is the only field this layer ever adds. On any doubt — no session
  key, an unreadable result, an unexpected failure — the result is returned
  exactly as the kernel wrote it."
  [registry session-key params result]
  (if-not (and (map? result) (nonblank-string? session-key))
    result
    (try
      (let [note (observe-transaction!
                  registry session-key
                  {:outcome (outcome-of result)
                   :shape (transaction-shape params)
                   :workspace-root (:workspace_root result)
                   :receipt-hash (:receipt_hash result)})]
        (cond-> result
          note (assoc :combinable_note note)))
      (catch Exception _ result))))
