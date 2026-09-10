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
  [state-dir m]
  (mission/write-mission! state-dir m)
  (ledger-of state-dir)
  m)

;; ---------------------------------------------------------------------------
;; the six verbs

(defn propose!
  "One bounded intent in, one mission id and its dossier out. NO BYTES WRITTEN
   to the workspace: the only file this touches is the mission's own EDN."
  [{:keys [verb request profiles state-home]}]
  (if-not (contains? verbs verb)
    (mission/refusal "unknown-verb"
                     (str "No mission verb named " (pr-str verb) ".")
                     {:verbs (vec (sort (keys verbs)))
                      :decision "which bounded intent this mission states"})
    (let [state-dir (state-dir-for (:workspace_root request) state-home)
          id (mission/next-id state-dir)
          plan ((get-in verbs [verb :plan]) request profiles)
          {:keys [dossier decision state]} (mission/dossier plan)
          created (mission/advance nil :proposed "propose"
                                   {:at (now) :id id :verb verb
                                    :created_at (now)
                                    :intent request})]
      (if (mission/refused? created)
        created
        (let [classified (mission/advance created state "propose"
                                          (cond-> {:at (now) :updated_at (now)
                                                   :dossier dossier}
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
              (save! state-dir terminal))))))))

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

(def usage
  (str "Usage: bin/mission <verb> [args]\n\n"
       "  propose --spec-file -        one bounded intent (EDN on stdin) -> id + dossier\n"
       "  show    <id> --workspace R   the whole mission object\n"
       "  apply   <id> --workspace R   run the guarded transaction + proof\n"
       "  undo    <id> --workspace R   invert a verified mission\n"
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
                    (select-keys spec [:verb :request :profiles]))
        result (case verb
                 "propose" (propose! opts)
                 "show" (show opts)
                 "apply" (apply! opts)
                 "undo" (undo! opts)
                 "ready" (ready opts)
                 "list" (list-missions opts)
                 (do (println usage) {:ok true}))]
    (when (map? result) (pp/pprint result))
    (when (false? (:ok result)) (System/exit 1))))
