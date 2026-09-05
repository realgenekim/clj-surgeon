(ns clj-surgeon.mission
  "The MISSION LEDGER: a durable, plain-EDN object for one bounded intent.

  PROTOTYPE (2026-09-05). Four independent ideal-shape reviews converged on the
  same picture: Surgeon should be an intent-to-receipt transaction kernel, and
  its usability bar is `bd` — a verb you can say in one sentence, an id you can
  hold, and state you can read with `cat` when the tool is not running.

  This namespace is the PURE HALF of that object: state transitions, EDN
  read/write, the dossier projection, and the human index. It knows nothing
  about helper_extraction, about verification profiles, or about how a plan is
  computed. `clj-surgeon.mission-cli` is the impure half that binds a verb to
  a real planner and a real transaction.

  THREE RULES THIS FILE EXISTS TO KEEP.

  1. A mission is READABLE WITHOUT THE TOOL. One EDN map per file, unqualified
     keys, no reader tags, no records, no printed objects. `cat` is a supported
     client.
  2. A TRANSITION IS A FACT, NOT A SETTER. Every state change goes through
     `advance`, which refuses an illegal one in the repository's typed-refusal
     shape and appends one history entry. Nothing else may `assoc :state`.
  3. THE DOSSIER IS A PROJECTION, NEVER A SECOND SOURCE OF TRUTH. It is derived
     from a plan the planner produced and is stored beside — never instead of —
     the intent that produced it. Re-proposing recomputes it.

  Babashka-safe: `clojure.edn`, `clojure.java.io`, `clojure.string` only."
  (:require
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.string :as str]))

(def operation "mission")

;; ---------------------------------------------------------------------------
;; refusals — the same closed envelope the rest of the repository mints

(defn refusal
  "One typed mission refusal, in the repository's receipt shape."
  ([suffix message] (refusal suffix message {}))
  ([suffix message evidence]
   (merge {:ok false
           :operation operation
           :error_type (str "mission-" suffix)
           :error message
           :next_call nil}
          evidence)))

;; ---------------------------------------------------------------------------
;; the lifecycle
;;
;;   (nothing) -> proposed -> ready   -> applied -> verified -> undone
;;                         \-> blocked          \-> failed
;;
;; `blocked` and `failed` are TERMINAL FOR THIS MISSION OBJECT, not for the
;; work: a blocked mission is waiting on exactly one human decision, and the
;; answer to that decision is a NEW proposal with a narrower intent. That is
;; deliberate. A mission whose intent may be edited in place is a mission whose
;; dossier no longer describes the thing that was applied.

(def states
  "Every state one mission can occupy."
  #{:proposed :ready :blocked :applied :verified :failed :undone})

(def transitions
  "The only legal moves. A state absent from a value set can never be reached
   from that key, whatever a caller passes."
  {nil       #{:proposed}
   :proposed #{:ready :blocked}
   :ready    #{:applied}
   :applied  #{:verified :failed}
   :verified #{:undone}
   :blocked  #{}
   :failed   #{}
   :undone   #{}})

(defn legal-transition?
  [from to]
  (contains? (get transitions from #{}) to))

(defn advance
  "Move `mission` to state `to`, recording one history entry, or refuse.

  `event` names the verb that caused the move and is the only free text in the
  record. `facts` are merged into the mission, minus `:state` and `:history`,
  so a handler can never smuggle a state change or rewrite the record past this
  function. `facts` may carry `:at`, which lands in the history entry and never
  in the mission body."
  [mission to event facts]
  (let [from (:state mission)]
    (cond
      (not (contains? states to))
      (refusal "unknown-state"
               (str "There is no mission state named " (pr-str to) ".")
               {:state (some-> to name)
                :decision "which of the seven mission states this move targets"})

      (not (legal-transition? from to))
      (refusal "illegal-transition"
               (str "A mission in state " (pr-str from) " cannot move to "
                    (pr-str to) ".")
               {:from (some-> from name)
                :to (name to)
                :legal (vec (sort (map name (get transitions from #{}))))
                :decision (str "what to do with a mission that is already "
                               (pr-str from))})

      :else
      (-> (merge mission (dissoc facts :state :history :at))
          (assoc :state to)
          (update :history (fnil conj [])
                  (cond-> {:from (some-> from name) :to (name to) :event event}
                    (:at facts) (assoc :at (:at facts))))))))

(defn refused?
  "Whether a value is a typed refusal rather than a mission."
  [value]
  (false? (:ok value)))

;; ---------------------------------------------------------------------------
;; identity and storage
;;
;; Ids are SHORT, STABLE and SORTABLE: `M-1`, `M-2`. Short because the caller
;; types them; stable because the file is named after the id and never renamed;
;; sortable because a human reading the index wants creation order. The id is
;; minted from the directory listing, which means the directory — not an
;; in-memory counter — is the authority, and a mission ledger copied to another
;; machine keeps its ids.

(defn missions-dir
  "The missions directory under one workspace's local-state dir."
  [state-dir]
  (str (io/file state-dir "missions")))

(defn mission-file
  [state-dir id]
  (str (io/file (missions-dir state-dir) (str id ".edn"))))

(defn- id-number
  [id]
  (when-let [digits (second (re-matches #"M-(\d+)" (str id)))]
    (parse-long digits)))

(defn mission-ids
  "Every mission id present on disk, in creation order."
  [state-dir]
  (let [dir (io/file (missions-dir state-dir))]
    (if-not (.isDirectory dir)
      []
      (->> (.listFiles dir)
           (map #(.getName ^java.io.File %))
           (keep #(second (re-matches #"(M-\d+)\.edn" %)))
           (sort-by id-number)
           vec))))

(defn next-id
  "The next free mission id. The DIRECTORY is the counter."
  [state-dir]
  (let [highest (reduce max 0 (keep id-number (mission-ids state-dir)))]
    (str "M-" (inc highest))))

(defn write-mission!
  "Write one mission as pretty, human-diffable EDN. Key order is fixed so a
   `git diff` of a ledger shows the change and not a rehash."
  [state-dir mission]
  (let [file (io/file (mission-file state-dir (:id mission)))
        ordered (concat [:id :state :verb :created_at :updated_at]
                        [:intent :dossier :decision :receipt :undo :history])
        body (str "{"
                  (str/join "\n "
                            (for [k ordered :when (contains? mission k)]
                              (str (pr-str k) " " (pr-str (get mission k)))))
                  "}\n")]
    (io/make-parents file)
    (spit file body)
    (str file)))

(defn read-mission
  "Read one mission, or a typed refusal naming the id that is not there."
  [state-dir id]
  (let [file (io/file (mission-file state-dir id))]
    (if-not (.isFile file)
      (refusal "unknown-id"
               (str "No mission " id " in this ledger.")
               {:id id :ledger (missions-dir state-dir)
                :decision "which mission id this call means"})
      (try
        (let [value (edn/read-string (slurp file))]
          (if (and (map? value) (= id (:id value)))
            value
            (refusal "corrupt-mission"
                     (str "The file for " id " does not contain a mission "
                          "whose :id is " id ".")
                     {:id id :file (str file)
                      :decision "whether this ledger file was hand-edited"})))
        (catch Exception error
          (refusal "unreadable-mission"
                   (str "The mission file could not be read as EDN: "
                        (.getMessage error))
                   {:id id :file (str file)
                    :decision "whether this ledger file was hand-edited"}))))))

(defn read-all
  "Every readable mission, in id order. Unreadable files are RETURNED as their
   refusal rather than skipped: a ledger that silently hides a broken row is a
   ledger you cannot trust to be complete."
  [state-dir]
  (mapv #(read-mission state-dir %) (mission-ids state-dir)))


;; ---------------------------------------------------------------------------
;; the snapshot, and the stale-resume gate
;;
;; A dossier is a claim ABOUT A TREE. The moment the tree moves, every number in
;; it is a number about something that no longer exists — and the expensive
;; failure is not that the write is wrong, it is that the caller BELIEVED a
;; stale dossier and spent a proof finding out. The kernel's own frozen-source
;; gate catches drift at commit time and refuses correctly; this gate catches it
;; BEFORE the transaction is entered, and names the files, which is the
;; difference between "your plan is stale" and "which of my 37 files moved".
;;
;; The snapshot is per-file so the refusal can NAME what changed. That is a
;; deliberate exception to the bounded-receipt rule: this is the mission's own
;; durable state, not a wire receipt, and a drift refusal that cannot say WHICH
;; file drifted sends the caller back to re-plan blind.

(defn sha256
  [^String text]
  (let [digest (.digest (java.security.MessageDigest/getInstance "SHA-256")
                        (.getBytes text "UTF-8"))]
    (apply str (map #(format "%02x" (bit-and 0xff (int %))) digest))))

(defn snapshot
  "`{:files n :hash aggregate :by-file {path sha}}` over the frozen bytes a plan
   was derived from."
  [sources]
  (let [by-file (into (sorted-map) (map (fn [[path source]] [path (sha256 source)])) sources)]
    {:files (count by-file)
     :hash (sha256 (str/join "\n" (map (fn [[p h]] (str p " " h)) by-file)))
     :by-file by-file}))

(defn drift
  "Which of the snapshot's files no longer hash to what was planned.

  `read-file` returns the file's current text, or nil when it is gone. Pure in
  the sense that matters: the I/O is the caller's, so this is testable without a
  tree and runs unchanged under babashka."
  [snap read-file]
  (let [changed (vec (sort (keep (fn [[path planned-hash]]
                                   (let [current (read-file path)]
                                     (when-not (and current (= planned-hash (sha256 current)))
                                       path)))
                                 (:by-file snap))))]
    {:changed changed :clean? (empty? changed) :checked (count (:by-file snap))}))

(defn stale-refusal
  "The typed refusal a stale mission earns, BEFORE any byte is written."
  [id snap drifted]
  (refusal "snapshot-stale"
           (str "The tree has moved since this mission was planned: "
                (count (:changed drifted)) " of " (:checked drifted)
                " planned files no longer match. Nothing was written.")
           {:id id
            :changed_files (:changed drifted)
            :planned_hash (:hash snap)
            :mutation_attempted false
            :source_unchanged true
            :next-action [:plan id]
            :decision "whether this intent still describes the tree as it now is"}))

;; ---------------------------------------------------------------------------
;; the native escape
;;
;; The tool has to be willing to say "do not use me." A mission costs the caller
;; at least one propose return and one apply return; native `apply_patch` costs
;; one edit. If the dossier cannot predict at least one SAVED return — an owner
;; the caller would otherwise have had to discover, or a proof obligation it
;; would otherwise have had to satisfy — then the mission is pure overhead and
;; says so. This is the honest version of the measured finding that an agent
;; given a free choice declines the tool, and declines it CORRECTLY.

(defn recommend
  "`{:recommendation :native|:mission :because …}` from the plan's own counts."
  [{:keys [caller_files sites]} proof-obligation?]
  (let [discovered (or caller_files 0)
        site-count (or sites 0)]
    (if (and (<= discovered 1) (<= site-count 1) (not proof-obligation?))
      {:recommendation :native
       :because (str "one owner and " site-count " site with no proof obligation: "
                     "a native edit costs one call, and this mission costs two "
                     "returns to reach the same bytes")}
      {:recommendation :mission
       :because (str discovered " caller files and " site-count
                     " sites the caller would otherwise have to discover and "
                     "rewrite itself"
                     (when proof-obligation? ", under a proof obligation"))})))

;; ---------------------------------------------------------------------------
;; the dossier projection
;;
;; What the caller gets back from `propose` instead of bytes. Every field here
;; is COPIED from a plan the planner produced; nothing is recomputed, and the
;; one field that is an estimate says so in its own name.

(defn dossier
  "Project one planner result onto the mission dossier.

  Takes the boundary's plan map (`:ok true`) or its typed refusal, and returns
  `{:dossier … :decision … :state …}` — the decision is `nil` exactly when the
  plan is complete, and the state that follows from it."
  ([plan] (dossier plan nil))
  ([plan request]
  (if-not (:ok plan)
    {:dossier {:planned false}
     :decision {:question (:decision plan)
                :error_type (:error_type plan)
                :because (:error plan)
                :evidence (dissoc plan :ok :operation :error :error_type
                                  :decision :next_call :source_unchanged
                                  :committed :mutation_attempted
                                  :write_authority)}
     :state :blocked}
    (let [receipt (:receipt plan)
          destination (get-in plan [:plan :destination])]
      {:dossier
       {:planned true
        :owners {:helpers (:helpers receipt)
                 :source_retired (:source_retired receipt)
                 :destination (select-keys destination [:lib :file])}
        :caller_partition (:partition receipt)
        :closure (:closure receipt)
        :footprint {:caller_files (:caller_files receipt)
                    :source_file (:source_file receipt)
                    :changed_files (:changed_files receipt)
                    :sites (:sites receipt)
                    :retained_sites (:retained_sites receipt)}
        ;; FACT, not estimate: this many files were read to derive the plan.
        :sources_read (count (:sources plan))
        ;; ESTIMATE, and named so. Wall time is not knowable before the proof
        ;; runs; what is knowable is the size of the write the proof must cover.
        :estimated_cost {:files_to_write (:changed_files receipt)
                         :sites_to_rewrite (:sites receipt)
                         :basis "plan counts; wall time depends on the profile"}}
       :recommendation (recommend receipt
                                  (boolean (get-in request [:verification :profile])))
       :decision nil
       :state :ready}))))

;; ---------------------------------------------------------------------------
;; the human index
;;
;; One line per mission, fixed columns, no tool required to read it. This is the
;; `bd list` half of the usability bar: the ledger has to answer "what is in
;; flight" without opening seven files.

(defn- one-line-summary
  [mission]
  (cond
    (:error_type mission) (str (:error_type mission))
    (= :blocked (:state mission)) (str "decision: "
                                       (or (get-in mission [:decision :question])
                                           "unstated"))
    (= :verified (:state mission)) (str "verified; undo "
                                        (or (get-in mission [:undo :receipt]) "-"))
    (= :failed (:state mission)) (str "failed: "
                                      (or (get-in mission [:receipt :status]) "-"))
    :else (str (get-in mission [:dossier :footprint :changed_files] "-")
               " files, "
               (get-in mission [:dossier :footprint :sites] "-") " sites")))

(defn index-lines
  "The human index, one line per mission."
  [missions]
  (into ["ID     STATE      VERB                SUMMARY"]
        (for [m missions]
          (format "%-6s %-10s %-19s %s"
                  (or (:id m) "?")
                  (or (some-> (:state m) name) "?")
                  (or (:verb m) "-")
                  (one-line-summary m)))))

(defn write-index!
  "Refresh the ledger's human index file."
  [state-dir missions]
  (let [file (io/file (missions-dir state-dir) "INDEX.txt")]
    (io/make-parents file)
    (spit file (str (str/join "\n" (index-lines missions)) "\n"))
    (str file)))

(defn next-action
  "The ONE executable next move for a mission, as data.

  `bd`'s usability bar restated: a caller should never have to infer the verb
  from the state. `nil` where nothing can move — a blocked mission's next action
  belongs to a human, and inventing `[:apply id]` for it would be a prescription
  the ledger cannot honour."
  [{:keys [id state]}]
  (case state
    :proposed [:plan id]
    :ready [:apply id]
    :applied [:resume id]
    :verified [:resume id]
    nil))

(defn ready-missions
  "Missions waiting on EXACTLY ONE decision, plus the ones ready to apply.

  This is the `bd ready` verb: what can move right now, and who has to move it.
  A blocked mission is listed because a human unblocks it; a ready mission is
  listed because a machine can."
  [missions]
  (->> missions
       (filter #(contains? #{:ready :blocked} (:state %)))
       (mapv (fn [m]
               {:id (:id m)
                :state (name (:state m))
                :waiting_on (if (= :blocked (:state m)) "a decision" "apply")
                :question (get-in m [:decision :question])}))))
