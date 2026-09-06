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
   [clojure.pprint :as pp]
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

(defn sha256
  [^String text]
  (let [digest (.digest (java.security.MessageDigest/getInstance "SHA-256")
                        (.getBytes text "UTF-8"))]
    (apply str (map #(format "%02x" (bit-and 0xff (int %))) digest))))

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

(defn workspace-state-dir
  "The ledger directory for one workspace, computed WITHOUT loading the JVM
  boundary.

  This MIRRORS `clj-surgeon.mcp-workspace/state-dir` — `.getCanonicalFile` on
  the root, SHA-256 of that path, under `<home>/.local/state/clj-surgeon/
  workspaces/`. A second source of truth for where state lives is exactly how a
  reader silently reads an EMPTY ledger and reports nothing in flight, so the
  two are pinned together by a witness rather than by this comment.

  It exists so the READ verbs can run under babashka. Measured on this box: a
  JVM entrance costs ~6 s of start and namespace loading before it does 1 ms of
  work, and `show`/`list`/`ready` are the verbs an agent calls most."
  [workspace-root state-home]
  (let [canonical (.getCanonicalPath (io/file workspace-root))]
    (str (io/file (or state-home (System/getProperty "user.home"))
                  ".local" "state" "clj-surgeon" "workspaces"
                  (sha256 canonical)))))

(defn write-mission!
  "Write one mission as pretty, human-diffable EDN. Key order is fixed so a
   `git diff` of a ledger shows the change and not a rehash."
  [state-dir mission]
  (let [file (io/file (mission-file state-dir (:id mission)))
        ordered (concat [:id :state :verb :created_at :updated_at]
                        [:question :root :scope :intent :verification
                         :depends-on :supersedes
                         :recommendation :because
                         :snapshot :dossier :decision :plan :proof :receipt
                         :undo :next-action :history])
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
;; where the proof authority is CONFIGURED
;;
;; @caller-probe. A real caller wrote `.clj-surgeon.edn` into the workspace with
;; the profile the mission needed, ran four schema-valid applies, and every one
;; died with `configured_profiles []`. The file existed. Nothing read it: the
;; profiles map reached the boundary only from the caller's own spec, and the
;; workspace's own configuration was never consulted by this entrance at all.
;;
;; The fix is not a flag. It is that the ledger LOOKS, and SAYS WHERE IT LOOKED
;; — a caller who cannot see which files were consulted cannot tell "no profile
;; is configured" from "your profile was ignored", and those two need opposite
;; repairs.

(def config-file-name ".clj-surgeon.edn")

(defn config-sources
  "Every place this ledger looks for `:verification-profiles`, whether each was
   there, and what it contributed. Returned by `show` so a caller never has to
   guess which file the tool read."
  [workspace-root explicit-config]
  (vec (for [path (cond-> [(str (io/file workspace-root config-file-name))]
                    explicit-config (conj (str (io/file explicit-config))))
             :let [file (io/file path)
                   present (.isFile file)
                   config (when present
                            (try (edn/read-string (slurp file))
                                 (catch Exception _ ::unreadable)))]]
         (cond-> {:path path :present present}
           (= ::unreadable config) (assoc :readable false)
           (map? config) (assoc :readable true
                                :profiles (vec (sort (keys (:verification-profiles config)))))))))

(defn configured-profiles
  "The `:verification-profiles` this workspace configures, merged in the order
   `config-sources` lists them (an explicit --config wins)."
  [workspace-root explicit-config]
  (reduce (fn [acc {:keys [path present]}]
            (if-not present
              acc
              (merge acc (:verification-profiles
                          (try (edn/read-string (slurp (io/file path)))
                               (catch Exception _ nil))))))
          {}
          (config-sources workspace-root explicit-config)))

;; ---------------------------------------------------------------------------
;; the verification authority, resolved ONCE, at plan time
;;
;; @carry-the-proof. Field report from the first hands-on run: `apply` refused
;; because the caller did not re-pass the profile map it had passed at open. A
;; durable mission that makes the caller re-supply the authority its own proof
;; runs under is not durable — it is a receipt for a call you must still be able
;; to reproduce. Two rules follow.
;;
;; 1. THE MISSION CARRIES ITS OWN PROOF AUTHORITY. The profile NAME plus the
;;    commands it resolved to are copied into the mission at plan time, so
;;    `apply` and `resume` need only an id and a workspace.
;; 2. AN UNADMITTED PROFILE IS A PLAN-TIME DECISION, NOT AN APPLY-TIME REFUSAL.
;;    A mission is never allowed to reach `:ready` on a proof nobody has
;;    admitted: the caller learns at open, in the dossier, that the authority is
;;    missing — never after paying for an apply.

(defn resolve-verification
  "`{:profile name :commands …}` copied from the admitted profiles, or nil when
   the request names no proof. A named-but-unadmitted profile returns
   `{:profile name :admitted? false …}` and the caller must block on it."
  [request profiles]
  (when-let [name (get-in request [:verification :profile])]
    (if-let [admitted (get profiles name)]
      {:profile name
       :commands (vec (:commands admitted))
       :hash (sha256 (pr-str [name (:commands admitted)]))
       :admitted? true}
      {:profile name
       :admitted? false
       :known (vec (sort (keys profiles)))})))

(defn verification-decision
  "The blocking decision an unadmitted proof authority earns AT PLAN TIME."
  [verification]
  (when (and verification (false? (:admitted? verification)))
    {:decision (str "which admitted verification profile proves this write; "
                    (pr-str (:profile verification)) " is not one of them")
     :error_type "mission-verification-profile-not-admitted"
     :because (str "The request names verification profile "
                   (pr-str (:profile verification))
                   ", which this workspace has not admitted. A mission may not "
                   "reach :ready on a proof nobody has admitted.")
     :evidence {:profile (:profile verification)
                :admitted_profiles (:known verification)}}))

(defn verification-profiles
  "The profiles map a mission's own stored authority reconstitutes, so `apply`
   needs only an id and a workspace."
  [mission]
  (when-let [{:keys [profile commands admitted?]} (:verification mission)]
    (when admitted? {profile {:commands commands}})))

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
     :decision {:decision (:decision plan)
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
;; LINKS: one mission may depend on, or supersede, another
;;
;; @migration-plan. A real migration is not one bounded intent, it is a partial
;; order over several: extract the helpers, THEN retire the shim, THEN rename
;; the namespace. The ledger has to be able to say "M-2 cannot move until M-1 is
;; verified" without either mission's dossier lying about the tree it will meet.
;;
;; TWO RULES, and they are the same rule the dossier already keeps.
;;
;; 1. ONLY THE FORWARD EDGE IS STORED. `M-2 :depends-on ["M-1"]` and
;;    `M-3 :supersedes ["M-2"]` live on the mission that names them; the inverse
;;    views (`dependents`, `superseded_by`) are COMPUTED from the ledger. Two
;;    copies of one edge is two things that can disagree, and the ledger is a
;;    directory of files any of which a human may edit with `cat` and an editor.
;; 2. `:blocked` BY DEPENDENCY IS DERIVED, NEVER WRITTEN. A mission's stored
;;    state is what its own transitions made it. Whether its dependencies are
;;    met is a question about OTHER files, and it changes the moment one of them
;;    is verified — so it is recomputed on every read. Writing it would need a
;;    fan-out write to every dependent at verify time, which is exactly the
;;    derived-state-stored-twice failure this object exists to avoid.
;;
;; WHY A `link` VERB rather than a `:depends-on` field on `open`: it is the
;; SMALLER change and the only one that can carry rule (2) of the lifecycle. An
;; open-time field would still need a link verb for `:supersedes` — the answer
;; to a blocked decision is a NEW, narrower mission, which by construction is
;; opened AFTER the mission it supersedes and so can never be named at that
;; mission's open. It also could not create a cycle (a fresh id is always the
;; highest, so nothing can point back at it), which means the cycle refusal
;; would have no site to live at. One verb covers both link kinds, both
;; directions of the graph, and the one refusal both share.

(def link-kinds
  "The two edges one mission may carry, each stored on the mission that names
   the other. `:depends-on` orders work; `:supersedes` records that this mission
   is the narrower re-statement of an earlier one."
  #{:depends-on :supersedes})

(defn by-id
  "Index a ledger by id. Unreadable rows are KEPT — a corrupt dependency must
   read as unmet, not as absent."
  [missions]
  (into {} (keep (fn [m] (when (and (map? m) (:id m)) [(:id m) m]))) missions))

(defn- edges
  [index id kind]
  (vec (get (get index id) kind [])))

(defn- path-to
  "A simple path of `kind` edges from `from` to `target`, or nil."
  [index kind from target]
  (loop [queue [[from]] seen #{}]
    (when-let [[trail & more] (seq queue)]
      (let [current (peek trail)]
        (cond
          (= current target) trail
          (seen current) (recur (vec more) seen)
          :else (recur (into (vec more)
                             (map #(conj trail %))
                             (edges index current kind))
                       (conj seen current)))))))

(defn link
  "Add one `kind` edge from `mission` to `target-id`, or refuse.

  Refuses a cycle BEFORE it is written: adding `m -> t` closes a cycle exactly
  when `t` already reaches `m`, so the check is one walk of the edges already in
  the ledger and the refusal can print the loop it would have made."
  [mission kind target-id index at]
  (let [id (:id mission)]
    (cond
      (not (contains? link-kinds kind))
      (refusal "unknown-link-kind"
               (str "There is no link kind named " (pr-str kind) ".")
               {:id id :kinds (vec (sort (map name link-kinds)))
                :decision "which link this call means: depends-on or supersedes"})

      (not (contains? index target-id))
      (refusal "unknown-id"
               (str "No mission " target-id " in this ledger to link to.")
               {:id id :target target-id
                :known (vec (sort (keys index)))
                :decision "which mission id this link points at"})

      (= id target-id)
      (refusal "dependency-cycle"
               (str "A mission cannot " (name kind) " itself.")
               {:id id :target target-id :cycle [id id] :kind (name kind)
                :mutation_attempted false
                :decision "which of two missions is the earlier one"})

      (path-to index kind target-id id)
      (let [loop-path (conj (path-to index kind target-id id) target-id)]
        (refusal "dependency-cycle"
                 (str "Linking " id " " (name kind) " " target-id
                      " would close a cycle: " (str/join " -> " loop-path)
                      ". Nothing was written.")
                 {:id id :target target-id :kind (name kind) :cycle loop-path
                  :mutation_attempted false
                  :decision "which of these missions is the one that must land first"}))

      :else
      (let [existing (vec (get mission kind []))]
        (if (some #{target-id} existing)
          mission
          (-> mission
              (assoc kind (vec (sort (conj existing target-id))))
              (update :history (fnil conj [])
                      (cond-> {:from (some-> (:state mission) name)
                               :to (some-> (:state mission) name)
                               :event (str "link " (name kind) " " target-id)}
                        at (assoc :at at)))))))))

;; ---------------------------------------------------------------------------
;; the derived half of the graph

(defn unmet-dependencies
  "The ids this mission depends on that are not `:verified` — including ids that
   are not in the ledger at all, which are unmet AND named."
  [mission index]
  (vec (remove #(= :verified (:state (get index %))) (:depends-on mission))))

(defn- stamp-of
  "The `:at` of the LAST history entry matching `pred`, or nil. The history is
   the record; `:updated_at` is a summary of it and moves for reasons that are
   not the one being asked about."
  [mission pred]
  (some->> (:history mission) (filter pred) last :at))

(defn verified-at
  "When this mission became `:verified` — i.e. when it last changed the tree."
  [mission]
  (when (= :verified (:state mission))
    (or (stamp-of mission #(= "verified" (:to %))) (:updated_at mission))))

(defn planned-at
  "When this mission's dossier and snapshot were computed. A re-plan refreshes
   it, which is the whole point: a plan is a claim about a tree AT A TIME."
  [mission]
  (or (stamp-of mission #(= "plan" (:event %))) (:updated_at mission)))

(defn stale-dependencies
  "Dependencies that reached `:verified` AFTER this mission was planned.

  @replan-after-dependency. The failure this closes: M-1 verifies, M-2's
  dependency is met, M-2 reads as `:ready` — on a dossier and a snapshot
  computed against a tree M-1 has since rewritten. The caller then pays for an
  apply to be told `mission-snapshot-stale`, a DOWNSTREAM symptom of an
  UPSTREAM fact the ledger already knew. Timestamps are ISO-8601 instants, so
  string order is time order."
  [mission index]
  (let [planned (planned-at mission)]
    (if-not planned
      []
      (vec (for [id (:depends-on mission)
                 :let [at (verified-at (get index id))]
                 :when (and at (pos? (compare at planned)))]
             id)))))

(defn replan-decision
  [stale]
  (str "re-plan: " (str/join ", " stale) " changed the tree after this plan"))

(defn effective-state
  "The state a READER should see. Two derivations, in order:

    unmet dependency        -> :blocked  (nobody can start this)
    dependency verified
      after this plan       -> :proposed (the plan is about a tree that moved)

  Never written back: both are questions about OTHER files and both change the
  moment one of those files does."
  [mission index]
  (cond
    (and (contains? #{:proposed :ready} (:state mission))
         (seq (unmet-dependencies mission index)))
    :blocked

    (and (= :ready (:state mission))
         (seq (stale-dependencies mission index)))
    :proposed

    :else (:state mission)))

(defn waiting-decision
  [unmet]
  (str "waiting on " (str/join ", " unmet)))

(defn blocking-decision
  "The ONE decision this mission is waiting on, as a string, or nil.

  `:decision` is the name this field carries everywhere in the repository's
  receipts — the mission's own top-level `:question` is the caller's statement
  of intent at open and is a different thing. Those two were spelled the same
  in round 2 and that collision is fixed here."
  [mission index]
  (let [unmet (unmet-dependencies mission index)
        stale (stale-dependencies mission index)]
    (cond
      (seq unmet) (waiting-decision unmet)
      (and (= :ready (:state mission)) (seq stale)) (replan-decision stale)
      :else (get-in mission [:decision :decision]))))

(defn- node
  [index id]
  {:id id :state (or (some-> (:state (get index id)) name) "missing")})

(defn supersede-chain
  "The whole supersession chain this mission sits in, newest first.

  Walks UP to the mission nothing supersedes, then down the `:supersedes` edges,
  so `show M-1` and `show M-3` render the same chain."
  [id index]
  (let [superseder (fn [i] (first (sort (for [[j m] index
                                              :when (some #{i} (:supersedes m))] j))))
        newest (loop [i id seen #{}]
                 (let [up (superseder i)]
                   (if (and up (not (seen up))) (recur up (conj seen i)) i)))]
    (loop [i newest acc [] seen #{}]
      (if (or (nil? i) (seen i))
        acc
        (recur (first (get-in index [i :supersedes]))
               (conj acc (node index i))
               (conj seen i))))))

(defn dependency-view
  "Both directions of both edges, plus what is unmet. All derived."
  [mission index]
  (let [id (:id mission)
        unmet (unmet-dependencies mission index)]
    {:depends_on (mapv #(node index %) (:depends-on mission))
     :unmet unmet
     :dependents (vec (sort (for [[j m] index :when (some #{id} (:depends-on m))] j)))
     :supersedes (mapv #(node index %) (:supersedes mission))
     :superseded_by (vec (sort (for [[j m] index :when (some #{id} (:supersedes m))] j)))
     :chain (supersede-chain id index)}))

(defn dependency-lines
  "The DAG around one mission, as text a human reads without the tool."
  [mission index]
  (let [{:keys [depends_on unmet dependents supersedes superseded_by chain]}
        (dependency-view mission index)]
    (into [(format "%s [%s]" (:id mission)
                   (name (effective-state mission index)))]
          (concat
           (for [n depends_on]
             (format "  depends-on   -> %-6s [%s]%s" (:id n) (:state n)
                     (if (some #{(:id n)} unmet) "   UNMET" "")))
           (for [j dependents] (format "  required-by  <- %s" j))
           (for [n supersedes]
             (format "  supersedes   -> %-6s [%s]" (:id n) (:state n)))
           (for [j superseded_by] (format "  superseded-by<- %s" j))
           (when (< 1 (count chain))
             [(str "  chain: " (str/join " -> " (map #(str (:id %) " [" (:state %) "]") chain)))])))))

(defn dependency-refusal
  "The typed refusal an apply earns while a dependency is unverified, BEFORE
   anything is staged and before any byte is written."
  [mission index]
  (let [unmet (unmet-dependencies mission index)
        stale (stale-dependencies mission index)]
    (cond
      (and (empty? unmet) (seq stale))
      ;; @replan-after-dependency: named BEFORE the snapshot hash gate, so the
      ;; caller is told the UPSTREAM reason (M-1 rewrote the tree) rather than
      ;; the downstream symptom (37 files no longer hash).
      (refusal "dependency-replan-required"
               (str "Mission " (:id mission) " was planned before "
                    (str/join ", " stale)
                    (if (= 1 (count stale)) " changed" " changed")
                    " the tree. Its dossier describes a tree that no longer "
                    "exists. Nothing was written.")
               {:id (:id mission)
                :depends_on (vec (:depends-on mission))
                :replanned_after stale
                :planned_at (planned-at mission)
                :dependency_verified_at (mapv #(verified-at (get index %)) stale)
                :mutation_attempted false
                :source_unchanged true
                :next-action [:plan (:id mission)]
                :decision (replan-decision stale)})

      (seq unmet)
      (refusal "dependency-not-verified"
               (str "Mission " (:id mission) " depends on "
                    (str/join ", " (:depends-on mission)) "; "
                    (str/join ", " unmet)
                    (if (= 1 (count unmet)) " is" " are")
                    " not :verified. Nothing was written.")
               {:id (:id mission)
                :depends_on (vec (:depends-on mission))
                :unverified unmet
                :mutation_attempted false
                :source_unchanged true
                :next-action [:resume (first unmet)]
                :decision (waiting-decision unmet)})

      :else nil)))

(defn replan
  "Refresh one mission's DOSSIER from a new plan, in place.

  Not a transition, and deliberately not `advance`: the dossier is a PROJECTION
  (rule 3 of this namespace), and recomputing a projection is not a state move.
  A mission that is `:ready` stays `:ready`; what changes is the tree its plan
  is a claim about, and the `plan` history entry that stamps when.

  Refuses rather than transitions when the new plan is incomplete: moving a
  planned mission to `:blocked` is not in the table, and inventing that move
  here would put a second setter of `:state` in the file."
  [mission {:keys [dossier decision state recommendation snapshot]} at]
  (cond
    (not (contains? #{:proposed :ready} (:state mission)))
    (refusal "replan-illegal-state"
             (str "Only a mission that has not been applied can be re-planned; "
                  (:id mission) " is " (pr-str (:state mission)) ".")
             {:id (:id mission) :state (some-> (:state mission) name)
              :decision "what to do with a mission that already ran"})

    (= :blocked state)
    (refusal "replan-blocked"
             (str "The re-plan did not complete: " (:decision decision))
             {:id (:id mission) :evidence decision
              :mutation_attempted false
              :decision (:decision decision)})

    :else
    (let [refreshed (-> mission
                        (merge (select-keys recommendation [:recommendation :because]))
                        (assoc :dossier dossier :updated_at at)
                        (cond-> snapshot (assoc :snapshot snapshot))
                        (dissoc :decision))]
      (if (= :proposed (:state mission))
        ;; a real move, so it goes through the one setter
        (advance refreshed :ready "plan" {:at at})
        ;; already :ready: nothing MOVED, only the projection was recomputed.
        ;; `advance` would refuse :ready -> :ready and should — this is not a
        ;; transition, and the history says so with a from = to entry.
        (update refreshed :history (fnil conj [])
                {:from "ready" :to "ready" :event "plan" :at at})))))

;; ---------------------------------------------------------------------------
;; HELP — pure text, so it lives on the babashka entrance
;;
;; @bb-help. Probe 2 reached :verified in 141 s, and EIGHT of its returns were
;; `help` calls at ~5 s each — roughly 40 s, nearly a third of the run, spent
;; starting a JVM to print a string. Help is the verb a caller uses most while
;; it is still learning the tool, which is exactly when it can least afford a
;; five-second turn. None of this text touches a planner or a tree, so it
;; belongs in the pure half with `show`, `list` and `ready`.

(def example-request
  "@caller-probe. THE COPY-PASTE SHAPE. The first probe spent seven refusals and
   ~35 s of JVM starts reverse-engineering this closed map one field at a time,
   and each refusal named the field it rejected but not the shape that would
   have been accepted. A closed schema the caller cannot see is a guessing game
   with a five-second turn."
  {:verb "helper_extraction"
   :question "why this write is being made"
   :request {:op "helper_extraction"
             :workspace_root "/abs/path/to/workspace"
             :from {:file "src/acid/web/http.clj"}
             :to {:lib "acid.web.response" :alias_policy ["response" "resp"]}
             :helpers ["html-response" "see-other" "text-response"]
             :scope {:paths ["src/**/*.clj"]}
             :verification {:profile "mission-proof"}}})

(def example-config
  "What `<workspace_root>/.clj-surgeon.edn` must contain for that profile to be
   ADMITTED. `plan` and `apply` read this file themselves."
  {:verification-profiles {"mission-proof" {:commands [["/bin/true"]]}}})

(def verb-help
  {"open"   "open --spec-file <file|-> [--workspace R] [--state-home H]\n    One bounded intent -> a mission id and its dossier. Writes no bytes."
   "plan"   "plan  [--spec-file <file|->] | plan <id> [--spec-file <file|->]\n    With no id: open-and-plan (same as `open`).\n    With an id: RE-plan that mission against the tree as it now is.\n    With an id AND --spec-file on a :blocked/:failed mission: open a NEW\n    mission carrying the repaired intent, linked :supersedes to the old one."
   "show"   "show <id> --workspace R\n    The mission, its dependency DAG, its supersession chain, and the\n    config files this ledger read (:config_sources)."
   "apply"  "apply <id> --workspace R\n    Run the guarded transaction and its proof. The mission carries its own\n    verification authority; no spec is re-supplied. Exits non-zero on a\n    refusal OR a failed receipt."
   "resume" "resume <id> --workspace R\n    Move it from wherever it is: :ready -> apply, :verified -> undo."
   "undo"   "undo <id> --workspace R\n    The explicit inverse, from the receipt apply published."
   "link"   "link <id> --depends-on <id> | --supersedes <id> --workspace R\n    Order two missions. A cycle is refused before it is written."
   "ready"  "ready --workspace R\n    :ready — what a machine can start now.\n    :waiting — real work held by a dependency or owed a re-plan."
   "list"   "list --workspace R\n    The human index, one fixed-column line per mission."
   "help"   "help [verb]\n    This text, or one verb's. Runs on babashka: ~0.05 s, not ~5 s."})

(def write-verbs
  "The verbs that reach a planner or a transaction kernel and must stay on the
   JVM. Everything else this entrance offers is pure and runs under babashka."
  #{"open" "plan" "propose" "apply" "resume" "undo" "link"})

(def read-verbs
  #{"show" "list" "ready" "blocked" "help"})

(defn help-text
  [verb]
  (str "bin/mission — the mission ledger. Global options may come BEFORE or\n"
       "AFTER the verb: --workspace <root> --state-home <dir> --config <file>\n\n"
       (if-let [one (get verb-help verb)]
         (str "  " one "\n")
         (str/join "\n" (for [[_ text] (sort verb-help)]
                          (str "  " text "\n"))))
       "\nTHE SPEC (copy-paste, closed shape — every field below is required\n"
       "unless marked optional; nothing else is accepted):\n\n"
       (with-out-str (pp/pprint example-request))
       "\nTHE PROFILE CONFIG — write this to <workspace_root>/"
       config-file-name
       ", which\n`plan` and `apply` read themselves (`show` reports :config_sources):\n\n"
       (with-out-str (pp/pprint example-config))
       "\nRunnable end to end:\n"
       "  bin/mission open  --spec-file spec.edn --state-home $H\n"
       "  bin/mission ready --workspace $WS --state-home $H\n"
       "  bin/mission apply M-1 --workspace $WS --state-home $H\n"
       "  bin/mission show  M-1 --workspace $WS --state-home $H\n"))

;; ---------------------------------------------------------------------------
;; the human index

;;
;; One line per mission, fixed columns, no tool required to read it. This is the
;; `bd list` half of the usability bar: the ledger has to answer "what is in
;; flight" without opening seven files.

(defn- one-line-summary
  [mission index]
  (let [state (effective-state mission index)]
    (cond
      (:error_type mission) (str (:error_type mission))
      (= :blocked state) (str "decision: "
                              (or (blocking-decision mission index) "unstated"))
      (= :verified state) (str "verified; undo "
                               (or (get-in mission [:undo :receipt]) "-"))
      (= :failed state) (str "failed: "
                             (or (get-in mission [:receipt :status]) "-"))
      ;; ROUND-2 DEFECT: :undone fell through to the footprint arm and reported
      ;; the size of a write that is no longer standing. An inverted mission's
      ;; one interesting fact is that the tree went back.
      (= :undone state) (str "undone; tree restored"
                             (when-let [v (get-in mission [:undo :verified :whole-files])]
                               (if v " (whole-file compare)" " (UNVERIFIED restore)")))
      :else (str (get-in mission [:dossier :footprint :changed_files] "-")
                 " files, "
                 (get-in mission [:dossier :footprint :sites] "-") " sites"))))

(defn index-lines
  "The human index, one line per mission. The STATE column shows the EFFECTIVE
   state, so a mission held by an unverified dependency reads as blocked here
   without that ever being written to its file."
  [missions]
  (let [index (by-id missions)]
    (into ["ID     STATE      VERB                SUMMARY"]
          (for [m missions]
            (format "%-6s %-10s %-19s %s"
                    (or (:id m) "?")
                    (or (some-> (effective-state m index) name) "?")
                    (or (:verb m) "-")
                    (one-line-summary m index))))))

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
    ;; @caller-probe: a dead mission USED to return nil here, on the reasoning
    ;; that its next move belonged to a human. A caller reported the cost of
    ;; that honesty: the ledger "accumulated blocked/failed missions but
    ;; offered no resolution transition". The human move now has a verb —
    ;; `plan <id> --spec-file <narrower intent>` opens the superseding mission
    ;; — so the ledger names it instead of going quiet.
    (:blocked :failed) [:plan id]
    nil))

(defn effective-next-action
  "The next move for the state a READER sees, not the stored one. A mission
   whose dependency moved the tree under it is told to re-plan, not to apply."
  [mission index]
  (next-action {:id (:id mission) :state (effective-state mission index)}))

(defn ready-missions
  "Missions that can move RIGHT NOW, and who has to move them.

  A dependency-blocked mission is deliberately NOT here: nobody can do anything
  about it until its dependency lands, and a `ready` list that includes work no
  one can start is the list an agent learns to stop reading. It appears in
  `waiting-missions` instead, with the id it is waiting on."
  [missions]
  (let [index (by-id missions)]
    (->> missions
         (filter map?)
         (keep (fn [m]
                 (let [state (effective-state m index)]
                   (when (and (contains? #{:ready :blocked} state)
                              (empty? (unmet-dependencies m index))
                              (empty? (stale-dependencies m index)))
                     {:id (:id m)
                      :state (name state)
                      :waiting_on (if (= :blocked state) "a decision" "apply")
                      :decision (blocking-decision m index)}))))
         vec)))

(defn waiting-missions
  "Missions the graph is holding: waiting on a dependency, or waiting on their
   own re-plan because a dependency already moved the tree."
  [missions]
  (let [index (by-id missions)]
    (->> missions
         (filter map?)
         (keep (fn [m]
                 (let [unmet (unmet-dependencies m index)
                       stale (stale-dependencies m index)]
                   (cond
                     (and (seq unmet) (contains? #{:proposed :ready} (:state m)))
                     {:id (:id m) :state "blocked" :waiting_on (vec unmet)
                      :decision (waiting-decision unmet)
                      ;; nil on purpose: the next move belongs to the DEPENDENCY
                      :next-action nil}

                     (and (seq stale) (= :ready (:state m)))
                     {:id (:id m) :state "proposed" :waiting_on (vec stale)
                      :decision (replan-decision stale)
                      :next-action [:plan (:id m)]}

                     :else nil))))
         vec)))

(defn show-view
  "One mission plus the graph around it: the DAG, the supersession chain, and
   the effective state. DERIVED on every read from the ledger it is handed —
   the mission file itself never carries any of it."
  [missions id]
  (let [index (by-id missions)
        m (get index id)]
    (if-not m
      (refusal "unknown-id"
               (str "No mission " id " in this ledger.")
               {:id id :decision "which mission id this call means"})
      (assoc m
             :effective_state (effective-state m index)
             :decision_summary (blocking-decision m index)
             :effective_next_action (effective-next-action m index)
             :dependencies (dependency-view m index)
             :graph (dependency-lines m index)))))
