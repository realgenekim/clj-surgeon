(ns clj-surgeon.mission-cli
  "The mission ledger's ENTRANCE: six verbs over the pure core.

  PROTOTYPE (2026-09-05). This is the impure half — it binds a mission's intent
  verb to a real planner, a real guarded transaction, and a real inverse. The
  pure half is `clj-surgeon.mission` and knows none of that.

  DISPATCH pattern, mirrored from the production CLI: `core/-main` routes its
  non-`:op` subcommands (`up`, `recover`, `report-failure`) by first argument
  and reaches their implementations with `requiring-resolve`, so the launcher
  never loads a namespace a call did not ask for. This entrance does the same,
  and deliberately does NOT touch `core/ops-registry` or `mcp_tool` — a
  prototype that edits the production dispatch table has to be reviewed as a
  production change.

  STRUCTURED ARGUMENTS come in the spelling the repository already uses:
  `--spec-file -` reads one EDN map on stdin, so a request with nested maps and
  vectors never has to survive shell quoting.

  ADDING A VERB (alias_migration is the intended next one) means adding one
  entry to `verbs` below. Nothing in `clj-surgeon.mission` changes: the object
  carries `:verb` and an opaque `:intent`, the dossier projection is the only
  verb-aware function, and it takes a plan map rather than a plan function."
  (:require
   [clj-surgeon.mission :as mission]
   [clj-surgeon.mcp-extraction :as extraction]
   [clj-surgeon.mcp-helper-extraction :as helper]
   [clj-surgeon.mcp-workspace :as workspace]
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.pprint :as pp]
   [clojure.string :as str])
  (:import
   (java.time Instant)))

(defn- now [] (str (Instant/now)))

;; ---------------------------------------------------------------------------
;; the verb registry
;;
;; One entry per bounded intent. `:plan` is a pure-enough dry run that writes no
;; bytes; `:execute!` runs the guarded transaction and returns the terminal
;; receipt; `:undo` inverts a committed one from its own receipt file.

(def verbs
  {"helper_extraction"
   {:plan     (fn [request profiles] (helper/plan request profiles))
    :execute! (fn [request config] (helper/execute! config request))
    :undo     (fn [undo-receipt]
                (extraction/undo! (edn/read-string (slurp undo-receipt))))}})

;; ---------------------------------------------------------------------------
;; argument handling

(defn- parse-flags
  [args]
  (loop [[a b & more :as remaining] args acc {} positional []]
    (cond
      (empty? remaining) (assoc acc :positional positional)
      (str/starts-with? (str a) "--")
      (recur more (assoc acc (keyword (subs a 2)) b) positional)
      :else (recur (rest remaining) acc (conj positional a)))))

(defn- read-spec
  [spec-file]
  (cond
    (nil? spec-file) nil
    (= "-" spec-file) (edn/read-string (slurp *in*))
    :else (edn/read-string (slurp spec-file))))

(defn state-dir-for
  "The local-state directory one workspace's ledger hangs from.

  Public because a caller — a test, a script, a human with `cat` — has to be
  able to find the ledger without the tool. `state-home` overrides the user
  home the directory hangs from; it is test isolation, not a request field."
  [workspace-root state-home]
  (workspace/state-dir workspace-root state-home))

(defn- ledger-of
  "Read every mission and refresh the human index in one pass."
  [state-dir]
  (let [missions (mission/read-all state-dir)]
    (mission/write-index! state-dir missions)
    missions))

(defn- save!
  "Stamp the one executable next move, write, and refresh the index."
  [state-dir m]
  (let [m (assoc m :next-action (mission/next-action m))]
    (mission/write-mission! state-dir m)
    (ledger-of state-dir)
    m))

;; ---------------------------------------------------------------------------
;; the six verbs

(defn propose!
  "One bounded intent in, one mission id and its dossier out. NO BYTES WRITTEN
   to the workspace: the only file this touches is the mission's own EDN."
  [{:keys [verb request profiles state-home question]}]
  (if-not (contains? verbs verb)
    (mission/refusal "unknown-verb"
                     (str "No mission verb named " (pr-str verb) ".")
                     {:verbs (vec (sort (keys verbs)))
                      :decision "which bounded intent this mission states"})
    (let [state-dir (state-dir-for (:workspace_root request) state-home)
          id (mission/next-id state-dir)
          plan ((get-in verbs [verb :plan]) request profiles)
          {:keys [dossier decision state recommendation]} (mission/dossier plan request)
          created (mission/advance nil :proposed "open"
                                   {:at (now) :id id :verb verb
                                    :created_at (now)
                                    ;; the bounded intent as the caller said it,
                                    ;; and the three facts that bound it
                                    :question question
                                    :root (:workspace_root request)
                                    :scope (:scope request)
                                    :intent request})]
      (if (mission/refused? created)
        created
        (let [classified (mission/advance created state "plan"
                                          (cond-> {:at (now) :updated_at (now)
                                                   :dossier dossier}
                                            recommendation (merge recommendation)
                                            ;; @stale-resume: the snapshot is
                                            ;; taken from the plan's OWN frozen
                                            ;; bytes, never a second read
                                            (:ok plan)
                                            (assoc :snapshot
                                                   (mission/snapshot (:sources plan)))
                                            decision (assoc :decision decision)))]
          (if (mission/refused? classified)
            classified
            (save! state-dir classified)))))))

(defn show
  [{:keys [id workspace state-home]}]
  (mission/read-mission (state-dir-for workspace state-home) id))

(defn list-missions
  [{:keys [workspace state-home]}]
  (let [state-dir (state-dir-for workspace state-home)
        missions (ledger-of state-dir)]
    {:ok true :operation "mission" :ledger (mission/missions-dir state-dir)
     :count (count missions)
     :index (mission/index-lines missions)}))

(defn ready
  [{:keys [workspace state-home]}]
  (let [state-dir (state-dir-for workspace state-home)]
    {:ok true :operation "mission"
     :ready (mission/ready-missions (ledger-of state-dir))}))

(defn- read-if-present
  [path]
  (let [f (io/file path)] (when (.isFile f) (slurp f))))

(defn stale?
  "@stale-resume. Whether the tree has moved under this mission's plan.

  Returns the typed refusal, or nil when the snapshot still holds. Called
  BEFORE anything is staged: the kernel's own frozen-source gate would catch
  the same drift at commit time and refuse correctly, but only after the caller
  has paid for a transaction, and its refusal does not name the files."
  [m]
  (when-let [snap (:snapshot m)]
    (let [drifted (mission/drift snap read-if-present)]
      (when-not (:clean? drifted)
        (mission/stale-refusal (:id m) snap drifted)))))

(defn apply!
  "Run the mission's guarded transaction and its proof, and record the terminal
   receipt INTO the mission. `:applied` is written before the transaction and
   is what a crashed apply leaves behind — the one state that means 'a write
   was attempted and nobody recorded the outcome'."
  [{:keys [id workspace state-home profiles receipt-dir] :as opts}]
  (let [state-dir (state-dir-for workspace state-home)
        m (mission/read-mission state-dir id)]
    (if (mission/refused? m)
      m
      (or
       ;; @stale-resume: nothing is staged, nothing is written, and the refusal
       ;; names the files that moved.
       (stale? m)
       (let [staged (mission/advance m :applied "apply" {:at (now) :updated_at (now)})]
        (if (mission/refused? staged)
          staged
          (let [_ (save! state-dir staged)
                config (cond-> {:verification-profiles profiles}
                         receipt-dir (assoc :receipt-dir receipt-dir))
                receipt ((get-in verbs [(:verb m) :execute!]) (:intent m) config)
                committed? (true? (:committed receipt))
                terminal (mission/advance staged
                                          (if committed? :verified :failed)
                                          "apply"
                                          (cond-> {:at (now) :updated_at (now)
                                                   :receipt receipt}
                                            committed?
                                            (assoc :undo
                                                   {:receipt (:undo_receipt receipt)
                                                    :receipt_hash (:receipt_hash receipt)})))]
            (if (mission/refused? terminal)
              terminal
              (save! state-dir terminal)))))))))

(defn undo!
  "Invert one verified mission through the receipt its own apply published."
  [{:keys [id workspace state-home]}]
  (let [state-dir (state-dir-for workspace state-home)
        m (mission/read-mission state-dir id)]
    (if (mission/refused? m)
      m
      (let [receipt-file (get-in m [:undo :receipt])]
        (cond
          (not= :verified (:state m))
          (mission/advance m :undone "undo" {:at (now)})   ; refuses, typed

          (not (and (string? receipt-file) (.isFile (io/file receipt-file))))
          (mission/refusal "undo-receipt-missing"
                           (str "The mission's undo receipt is not on disk: "
                                (pr-str receipt-file))
                           {:id id :undo_receipt receipt-file
                            :decision "how this write is to be inverted"})

          :else
          (let [result ((get-in verbs [(:verb m) :undo]) receipt-file)]
            (if-not (:ok result)
              (mission/refusal "undo-failed"
                               (str "The inverse did not verify: "
                                    (or (:error result) (:error-type result)))
                               {:id id :evidence result
                                :decision "which files the failed inverse left standing"})
              (let [undone (mission/advance m :undone "undo"
                                            {:at (now) :updated_at (now)
                                             :undo (assoc (:undo m)
                                                          :verified (:verified result))})]
                (if (mission/refused? undone)
                  undone
                  (save! state-dir undone))))))))))

;; ---------------------------------------------------------------------------
;; entrance

(defn resume
  "ONE verb for 'move this mission from wherever it is'.

  Astra's convergence: `continue` and `undo` are the same question asked of
  different states, and a caller holding an id should not have to know which
  one it is in. The mission's `:next-action` is the authority, so the ledger —
  not the caller — decides what resuming means:

    :ready     -> apply    (the guarded transaction, behind the stale gate)
    :verified  -> undo     (the inverse, from the receipt apply published)
    :applied   -> REFUSE   a write was attempted and nobody recorded the
                           outcome. Resuming that automatically is exactly the
                           auto-remediation that must never mutate a delivery
                           chain without a second predicate.
    otherwise  -> the typed illegal-transition refusal, which names what is
                  legal from here."
  [{:keys [id workspace state-home] :as opts}]
  (let [m (mission/read-mission (state-dir-for workspace state-home) id)]
    (cond
      (mission/refused? m) m
      (= :ready (:state m)) (apply! opts)
      (= :verified (:state m)) (undo! opts)
      (= :applied (:state m))
      (mission/refusal "resume-needs-a-human"
                       (str "Mission " id " is :applied: a write was attempted "
                            "and no terminal receipt was recorded. Resuming it "
                            "automatically could double-apply or invert a "
                            "transaction nobody has confirmed.")
                       {:id id :state "applied"
                        :next-action nil
                        :decision "what the interrupted transaction left standing"})
      :else (mission/advance m :applied "resume" {:at (now)}))))

(def usage
  (str "Usage: bin/mission <verb> [args]\n\n"
       "  open|plan --spec-file -      one bounded intent (EDN on stdin) -> id + dossier\n"
       "  show    <id> --workspace R   the whole mission object\n"
       "  apply   <id> --workspace R   run the guarded transaction + proof\n"
       "  resume  <id> --workspace R   move it from wherever it is (apply or undo)\n"
       "  undo    <id> --workspace R   the explicit inverse\n"
       "  ready        --workspace R   missions waiting on exactly one move\n"
       "  list         --workspace R   the human index\n\n"
       "The propose spec is {:verb \"helper_extraction\" :request {...}\n"
       "                     :profiles {\"name\" {:commands [[\"...\"]]}}}\n"
       "Missions live in <state-dir>/missions/<id>.edn and are plain EDN.\n"))

(defn -main [& args]
  (let [[verb & rest-args] args
        {:keys [positional] :as flags} (parse-flags rest-args)
        spec (read-spec (:spec-file flags))
        opts (merge {:workspace (:workspace flags)
                     :state-home (:state-home flags)
                     :profiles (:profiles spec)
                     :receipt-dir (:receipt-dir flags)
                     :id (first positional)}
                    (select-keys spec [:verb :question :request :profiles]))
        result (case verb
                 ;; `open` and `plan` are one call in this prototype: proposing
                 ;; a bounded intent IS computing its dossier. Astra's two-step
                 ;; (state the question, then plan it) is a real split this
                 ;; prototype does not implement; both names reach the same fn
                 ;; so the vocabulary is already the converged one.
                 ("open" "plan" "propose") (propose! opts)
                 "show" (show opts)
                 "apply" (apply! opts)
                 "resume" (resume opts)
                 "undo" (undo! opts)
                 ("ready" "blocked") (ready opts)
                 "list" (list-missions opts)
                 (do (println usage) {:ok true}))]
    (when (map? result) (pp/pprint result))
    (when (false? (:ok result)) (System/exit 1))))
