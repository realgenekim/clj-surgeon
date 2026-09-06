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

(def example-request
  "@caller-probe / @bb-help. Re-exported from the pure core so callers of this
   namespace keep their spelling; the text itself lives in `clj-surgeon.mission`
   because it is PURE TEXT and belongs on the babashka entrance."
  mission/example-request)
(def example-config mission/example-config)
(def verb-help mission/verb-help)
(def help-text mission/help-text)

(defn parse-flags
  "@caller-probe. Global options are accepted BEFORE or AFTER the verb, and a
   flag with no value (`--help`) is a boolean rather than a swallower of the
   next token. The probe lost a return to `mission --state-home X plan …`,
   which printed help and exited 0 — the worst possible answer, because it
   looks like the tool ran."
  [args]
  (loop [[a b & more :as remaining] args acc {} positional []]
    (cond
      (empty? remaining) (assoc acc :positional positional)

      (str/starts-with? (str a) "--")
      (if (or (nil? b) (str/starts-with? (str b) "--"))
        (recur (rest remaining) (assoc acc (keyword (subs a 2)) true) positional)
        (recur more (assoc acc (keyword (subs a 2)) b) positional))

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

(defn admitted-profiles
  "@caller-probe. The profiles this call may prove a write with: the
   WORKSPACE'S OWN `.clj-surgeon.edn` first, then an explicit --config, then
   anything the spec passed. The probe's four applies all died on
   `configured_profiles []` while that file sat in the workspace, because this
   entrance only ever forwarded the spec's map."
  [workspace-root {:keys [config profiles]}]
  (merge (mission/configured-profiles workspace-root config) profiles))

(defn- occupant-sizes
  "@caller-probe. Any file a refusal NAMES, with its size on disk.

  A `target-exists` refusal against a ZERO-BYTE file is almost always a fixture
  artifact — the probe lost four minutes to a materialization recipe that spat
  an empty destination — and a byte count makes that obvious at a glance."
  [refusal workspace-root]
  (let [paths (keep (fn [k] (let [v (get refusal k)]
                              (cond (string? v) v
                                    (map? v) (:file v))))
                    [:file :path :target :destination :to])]
    (into {} (for [path paths
                   :let [f (if (str/starts-with? path "/")
                             (io/file path)
                             (io/file workspace-root path))]
                   :when (.isFile f)]
               [(str f) {:bytes (.length f)
                         :note (when (zero? (.length f))
                                 "ZERO BYTES — an empty occupant is usually a fixture artifact, not real code")}]))))

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
  [{:keys [verb request state-home question] :as opts}]
  (if-not (contains? verbs verb)
    (mission/refusal "unknown-verb"
                     (str "No mission verb named " (pr-str verb) ".")
                     {:verbs (vec (sort (keys verbs)))
                      :decision "which bounded intent this mission states"})
    (let [state-dir (state-dir-for (:workspace_root request) state-home)
          profiles (admitted-profiles (:workspace_root request) opts)
          id (mission/next-id state-dir)
          ;; @carry-the-proof: resolve the proof authority BEFORE planning. An
          ;; unadmitted profile is a decision the caller can be told about now,
          ;; and planning a write that could never be proved is wasted work.
          verification (mission/resolve-verification request profiles)
          proof-decision (mission/verification-decision verification)
          plan (if proof-decision
                 {:ok false :error_type (:error_type proof-decision)
                  :error (:because proof-decision)
                  :decision (:decision proof-decision)
                  :admitted_profiles (get-in proof-decision [:evidence :admitted_profiles])}
                 ((get-in verbs [verb :plan]) request profiles))
          {:keys [dossier decision state recommendation]} (mission/dossier plan request)
          ;; @caller-probe: EVERY blocked mission carries the closed shape that
          ;; would have been accepted, and the size of any file it names.
          decision (when decision
                     (-> decision
                         (assoc :example example-request)
                         (update :evidence merge
                                 (let [occ (occupant-sizes (:evidence decision)
                                                           (:workspace_root request))]
                                   (when (seq occ) {:occupant occ})))))
          created (mission/advance nil :proposed "open"
                                   {:at (now) :id id :verb verb
                                    :created_at (now)
                                    ;; the bounded intent as the caller said it,
                                    ;; and the three facts that bound it
                                    :question question
                                    :root (:workspace_root request)
                                    :scope (:scope request)
                                    :verification verification
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

(defn replan!
  "@replan-after-dependency. Recompute one mission's dossier and snapshot
   against the tree AS IT NOW IS, using the intent and the proof authority the
   mission already carries. No new id, no re-supplied spec."
  [{:keys [id workspace state-home profiles] :as opts}]
  (let [state-dir (state-dir-for workspace state-home)
        m (mission/read-mission state-dir id)]
    (if (mission/refused? m)
      m
      (let [request (:intent m)
            profiles (or profiles
                         (mission/verification-profiles m)
                         (not-empty (admitted-profiles (:root m) opts)))
            plan ((get-in verbs [(:verb m) :plan]) request profiles)
            projection (assoc (mission/dossier plan request)
                              :snapshot (when (:ok plan)
                                          (mission/snapshot (:sources plan))))
            replanned (mission/replan m projection (now))]
        (if (mission/refused? replanned)
          replanned
          (do (save! state-dir replanned)
              (assoc (mission/show-view (mission/read-all state-dir) id)
             :config_sources (mission/config-sources (or workspace (:root m))
                                                     (:config opts)))))))))

(defn repair!
  "@caller-probe. Answer a dead mission with a NARROWER one, and say so.

  The probe's verdict on the ledger after a refusal: it \"accumulated
  blocked/failed missions but offered no resolution transition, next_call, or
  way to repair a request in place.\" A blocked intent may never be edited in
  place — its dossier would stop describing the thing that was planned — so the
  repair is a NEW mission linked `:supersedes` to the old one, which is exactly
  what round 3's links exist for."
  [{:keys [id workspace state-home request] :as opts}]
  (let [state-dir (state-dir-for workspace state-home)
        old (mission/read-mission state-dir id)]
    (cond
      (mission/refused? old) old

      (not (contains? #{:blocked :failed :proposed} (:state old)))
      (mission/refusal "repair-illegal-state"
                       (str "Only a :blocked, :failed or :proposed mission is "
                            "repaired by superseding it; " id " is "
                            (pr-str (:state old))
                            ". Use `plan " id "` to re-plan it in place.")
                       {:id id :state (some-> (:state old) name)
                        :next-action [:plan id]
                        :decision "whether this mission needs a new intent or a fresh plan"})

      (nil? request)
      (mission/refusal "repair-needs-a-request"
                       (str "Repairing " id " means stating the narrower intent: "
                            "pass --spec-file with the corrected request.")
                       {:id id :example example-request
                        :decision "what the corrected intent is"})

      :else
      (let [opened (propose! (assoc opts :verb (or (:verb opts) (:verb old))))]
        (if (mission/refused? opened)
          opened
          (let [missions (mission/read-all state-dir)
                linked (mission/link opened :supersedes id
                                     (mission/by-id missions) (now))]
            (if (mission/refused? linked)
              linked
              (do (save! state-dir linked)
                  (assoc (mission/show-view (mission/read-all state-dir) (:id linked))
                         :repaired id
                         :note (str (:id linked) " supersedes " id
                                    "; the old mission keeps its record and its "
                                    "chain is visible from either end"))))))))))

(defn show
  "The mission, plus the graph around it. @migration-plan: `show` is where a
   caller learns that M-2 is held by M-1, so the DAG is rendered here rather
   than behind a second verb nobody would call."
  [{:keys [id workspace state-home] :as opts}]
  (let [state-dir (state-dir-for workspace state-home)
        m (mission/read-mission state-dir id)]
    (if (mission/refused? m)
      m
      (assoc (mission/show-view (mission/read-all state-dir) id)
             :config_sources (mission/config-sources (or workspace (:root m))
                                                     (:config opts))))))

(defn link!
  "Add one `:depends-on` or `:supersedes` edge, or refuse a cycle.

  The ONLY verb that edits a mission without a state transition — a link is not
  a move, it is a fact about two missions. `advance` stays the only setter of
  `:state`; `mission/link` appends its own history entry so the edge is still
  in the record."
  [{:keys [id workspace state-home] :as opts}]
  (let [state-dir (state-dir-for workspace state-home)
        kind (cond (:depends-on opts) :depends-on
                   (:supersedes opts) :supersedes
                   :else nil)
        target (get opts kind)]
    (if-not kind
      (mission/refusal "link-target-missing"
                       "A link needs --depends-on <id> or --supersedes <id>."
                       {:id id
                        :decision "which mission this one is ordered against"})
      (let [missions (mission/read-all state-dir)
            m (mission/read-mission state-dir id)]
        (if (mission/refused? m)
          m
          (let [linked (mission/link m kind target (mission/by-id missions) (now))]
            (if (mission/refused? linked)
              linked
              (do (save! state-dir linked)
                  (assoc (mission/show-view (mission/read-all state-dir) id)
             :config_sources (mission/config-sources (or workspace (:root m))
                                                     (:config opts)))))))))))

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
    (let [missions (ledger-of state-dir)]
      {:ok true :operation "mission"
       :ready (mission/ready-missions missions)
       ;; @migration-plan: real work that nobody can start yet, and the id it
       ;; is waiting on. Kept OUT of :ready on purpose.
       :waiting (mission/waiting-missions missions)})))

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
       ;; @migration-plan: an unverified dependency refuses FIRST — before the
       ;; snapshot is even checked, because a stale-plan refusal would send the
       ;; caller to re-plan against a tree its dependency has not touched yet.
       (mission/dependency-refusal m (mission/by-id (mission/read-all state-dir)))
       ;; @stale-resume: nothing is staged, nothing is written, and the refusal
       ;; names the files that moved.
       (stale? m)
       (let [staged (mission/advance m :applied "apply" {:at (now) :updated_at (now)})]
        (if (mission/refused? staged)
          staged
          (let [_ (save! state-dir staged)
                ;; @carry-the-proof: the mission's OWN authority, so apply and
                ;; resume need only an id and a workspace. An explicitly passed
                ;; profiles map still wins, for a caller deliberately re-proving
                ;; under a different profile.
                profiles (or profiles
                             (mission/verification-profiles m)
                             (not-empty (admitted-profiles (:root m) opts)))
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

(def usage (help-text nil))

(defn failed-receipt?
  "@caller-probe. A mission whose apply produced a NON-committed receipt is a
   failure the process must report. All four of the probe's failed applies
   exited 0 while their receipts said `:ok false`, `:committed false`, and the
   mission moved to `:failed` — an exit code that says success about a write
   that did not happen is worse than no exit code at all."
  [result]
  (boolean (and (map? result)
                (or (= :failed (:state result))
                    (false? (get-in result [:receipt :committed]))))))

(defn -main [& args]
  (let [{:keys [positional] :as flags} (parse-flags args)
        verb (first positional)
        spec (read-spec (:spec-file flags))
        opts (merge {:workspace (:workspace flags)
                     :state-home (:state-home flags)
                     :config (:config flags)
                     :profiles (:profiles spec)
                     :spec spec
                     :receipt-dir (:receipt-dir flags)
                     :id (second positional)}
                    (select-keys flags [:depends-on :supersedes])
                    (select-keys spec [:verb :question :request :profiles]))]
    (cond
      ;; explicit help is a SUCCESS, and it is the only path that prints usage
      (or (:help flags) (= "help" verb) (nil? verb))
      (do (println (help-text (second positional))) (System/exit 0))

      (not (contains? #{"open" "plan" "propose" "show" "apply" "resume" "undo"
                        "link" "ready" "blocked" "list"} verb))
      (do (binding [*out* *err*]
            (println (str "bin/mission: no verb named " (pr-str verb) ".\n")))
          (println (help-text nil))
          ;; NEVER exit 0 when a verb was given: the probe read that as "ran"
          (System/exit 2))

      :else
      (let [result (case verb
                     ;; `plan <id>` re-plans in place; `plan <id> --spec-file`
                     ;; on a dead mission opens its repaired successor.
                     "plan" (cond
                              (and (second positional) spec) (repair! opts)
                              (second positional) (replan! opts)
                              :else (propose! opts))
                     ("open" "propose") (propose! opts)
                     "show" (show opts)
                     "apply" (apply! opts)
                     "resume" (resume opts)
                     "undo" (undo! opts)
                     "link" (link! opts)
                     ("ready" "blocked") (ready opts)
                     "list" (list-missions opts))]
        (when (map? result) (pp/pprint result))
        (System/exit (cond (false? (:ok result)) 1
                           (failed-receipt? result) 1
                           :else 0))))))
