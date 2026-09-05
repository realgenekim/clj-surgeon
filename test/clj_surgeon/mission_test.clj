(ns clj-surgeon.mission-test
  "Witnesses for the MISSION LEDGER prototype.

  Two halves, matching the object's own two halves:

    PURE      transitions, EDN round-trip, dossier projection, index. No tree,
              no JVM subprocess, no clock.
    END-TO-END  one real helper_extraction on the fixture tree, driven through
              the mission verbs only: propose -> show -> apply -> undo. It
              asserts the FEEL, which is the thing this prototype exists to
              prove: one bounded intent in, an id and a dossier out, an apply
              that publishes a terminal receipt into the mission file, and an
              undo that puts every byte back."
  (:require
   [clj-surgeon.helper-extraction-fixture :as fixture]
   [clj-surgeon.mission :as mission]
   [clj-surgeon.mission-cli :as cli]
   [clj-surgeon.mcp-workspace :as workspace]
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.java.shell]
   [clojure.string :as str]
   [clojure.test :refer [deftest is testing]]))

(def ^:private tmp-root
  (or (System/getenv "CLJ_SURGEON_MISSION_TMP") "/var/tmp/forge/mission-fx"))

(defn- delete-tree!
  [^java.io.File file]
  (when (.isDirectory file)
    (run! delete-tree! (.listFiles file)))
  (.delete file))

;; ---------------------------------------------------------------------------
;; the pure half

(deftest every-legal-move-is-in-the-table-and-nothing-else-is
  (testing "the lifecycle is DATA, and the table is the only authority. A
            transition absent from it must refuse whatever the caller does."
    (is (= #{:proposed :ready :blocked :applied :verified :failed :undone}
           mission/states))
    (testing "the seven legal moves"
      (doseq [[from to] [[nil :proposed] [:proposed :ready] [:proposed :blocked]
                         [:ready :applied] [:applied :verified]
                         [:applied :failed] [:verified :undone]]]
        (is (mission/legal-transition? from to)
            (str (pr-str from) " -> " (pr-str to)))))
    (testing "and the moves a caller would most plausibly try to take"
      (doseq [[from to] [[:blocked :ready]      ; edit the intent in place
                         [:proposed :applied]   ; skip the dossier
                         [:failed :verified]    ; relabel a failure
                         [:undone :applied]     ; re-apply an inverted mission
                         [:verified :verified]] ; idempotent apply
              :let [result (mission/advance {:state from} to "test" {})]]
        (is (mission/refused? result) (str (pr-str from) " -> " (pr-str to)))
        (is (= "mission-illegal-transition" (:error_type result)))
        (is (string? (:decision result))
            "and a refusal names the one decision it is waiting on")))))

(deftest a-transition-appends-history-and-cannot-be-bypassed
  (testing "`advance` is the only setter: a handler that hands it a :state or a
            :history in its facts must not have them applied, or the ledger's
            record is whatever the last caller felt like writing."
    (let [proposed (mission/advance nil :proposed "propose"
                                    {:at "T0" :id "M-9" :verb "helper_extraction"
                                     :state :verified
                                     :history [{:forged true}]})]
      (is (= :proposed (:state proposed)) "the smuggled :state was dropped")
      (is (= [{:from nil :to "proposed" :event "propose" :at "T0"}]
             (:history proposed))
          "the smuggled :history was dropped and one honest entry appended")
      (is (= "M-9" (:id proposed)) "ordinary facts do land")
      (is (nil? (:at proposed))
          ":at belongs to the history entry, never to the mission body"))))

(deftest a-mission-round-trips-through-plain-edn
  (testing "the storage contract: what `cat` shows is what the tool reads back,
            with no reader tags, no records and no printed objects."
    (let [state-home (str (io/file tmp-root (str "rt-home-" (System/nanoTime))))
          workspace (io/file tmp-root (str "rt-ws-" (System/nanoTime)))]
      (try
        (.mkdirs workspace)
        (let [state-dir (str (io/file state-home "state"))
              m (mission/advance nil :proposed "propose"
                                 {:at "T0" :id "M-1" :verb "helper_extraction"
                                  :created_at "T0"
                                  :intent {:workspace_root (str workspace)
                                           :helpers ["a" "b"]}})
              _ (mission/write-mission! state-dir m)
              raw (slurp (mission/mission-file state-dir "M-1"))
              back (mission/read-mission state-dir "M-1")]
          (is (= m back) "the object survives the round trip exactly")
          (is (not (str/includes? raw "#"))
              (str "no reader tags in the stored form: " raw))
          (is (= m (edn/read-string raw))
              "and a reader with NO custom tags gets the same object")
          (is (str/starts-with? raw "{:id \"M-1\"")
              "id first, so a human scanning a directory reads the id first")
          (testing "the directory is the id counter"
            (is (= "M-2" (mission/next-id state-dir)))
            (is (= ["M-1"] (mission/mission-ids state-dir))))
          (testing "and an id that is not there refuses by name"
            (let [missing (mission/read-mission state-dir "M-404")]
              (is (mission/refused? missing))
              (is (= "mission-unknown-id" (:error_type missing)))
              (is (= "M-404" (:id missing))))))
        (finally
          (delete-tree! (io/file state-home))
          (delete-tree! workspace))))))

(deftest a-corrupt-ledger-row-is-returned-not-hidden
  (testing "a ledger that silently skips a broken row is a ledger you cannot
            trust to be complete."
    (let [state-dir (str (io/file tmp-root (str "corrupt-" (System/nanoTime))))]
      (try
        (io/make-parents (io/file (mission/mission-file state-dir "M-1")))
        (spit (mission/mission-file state-dir "M-1") "{:id \"M-1\" :state :ready")
        (let [[row] (mission/read-all state-dir)]
          (is (mission/refused? row))
          (is (= "mission-unreadable-mission" (:error_type row))))
        (finally (delete-tree! (io/file state-dir)))))))

(deftest the-dossier-is-a-projection-of-a-plan-and-never-invents-a-number
  (testing "a complete plan projects to :ready with the planner's OWN counts,
            and a refusal projects to :blocked carrying the one decision."
    (let [plan {:ok true
                :plan {:destination {:lib "acid.web.response"
                                     :file "src/acid/web/response.clj"}}
                :sources {"a.clj" "..." "b.clj" "..."}
                :receipt {:helpers 6 :source_retired 6
                          :caller_files 28 :source_file 1 :changed_files 30
                          :sites 41 :retained_sites 7
                          :partition {:moved_only 20 :mixed 4
                                      :qualified_only 4 :untouched 2}
                          :closure {:roots ["src"] :grammar "supported-libspecs-only"}}}
          {:keys [dossier decision state]} (mission/dossier plan)]
      (is (= :ready state))
      (is (nil? decision) "a complete plan has no unresolved decision")
      (is (= 6 (get-in dossier [:owners :helpers])))
      (is (= 30 (get-in dossier [:footprint :changed_files])))
      (is (= {:moved_only 20 :mixed 4 :qualified_only 4 :untouched 2}
             (:caller_partition dossier)))
      (is (= 2 (:sources_read dossier))
          "sources_read is a FACT about the plan, not an estimate")
      (is (= 30 (get-in dossier [:estimated_cost :files_to_write])))
      (is (string? (get-in dossier [:estimated_cost :basis]))
          "and the estimate says what it is an estimate FROM")
      (is (not (contains? dossier :wall_ms))
          "no invented wall time: it is not knowable before the proof runs")))
  (testing "a refusal is the blocked half"
    (let [{:keys [decision state dossier]}
          (mission/dossier {:ok false
                            :error_type "helper-extraction-caller-outside-scope"
                            :error "a supported reference is outside scope"
                            :decision "whether test/ is in this write's scope"
                            :files ["test/acid/web/http_test.clj"]})]
      (is (= :blocked state))
      (is (false? (:planned dossier)))
      (is (= "whether test/ is in this write's scope" (:decision decision)))
      (is (= ["test/acid/web/http_test.clj"] (get-in decision [:evidence :files]))
          "the refusal's evidence travels with the decision"))))

(deftest ready-lists-exactly-what-can-move-and-who-moves-it
  (let [missions [{:id "M-1" :state :proposed}
                  {:id "M-2" :state :ready}
                  {:id "M-3" :state :blocked
                   :decision {:decision "which path the destination occupies"}}
                  {:id "M-4" :state :verified}]
        ready (mission/ready-missions missions)]
    (is (= ["M-2" "M-3"] (mapv :id ready)))
    (is (= ["apply" "a decision"] (mapv :waiting_on ready))
        "a ready mission is moved by a machine; a blocked one by a human")
    (is (= "which path the destination occupies" (:decision (second ready)))
        "ROUND-2 DEFECT: this field was spelled :question, which collided with
         the mission's own top-level :question — the caller's statement of
         intent at open. The blocking decision is :decision everywhere now.")
    (testing "and the index is one fixed-column line per mission"
      (let [lines (mission/index-lines missions)]
        (is (= 5 (count lines)))
        (is (str/starts-with? (first lines) "ID "))
        (is (every? #(< (count %) 120) lines))))))

(deftest the-ledger-dir-mirrors-the-production-state-dir
  (testing "@bb-read-path. The babashka reader computes the ledger location
            itself so it never loads the JVM boundary. That is a SECOND
            SPELLING of where state lives, and a second spelling that drifts
            makes a reader report an empty ledger instead of an error — the
            worst possible failure for a `what is in flight` verb. Pin them."
    (let [home (str (io/file tmp-root (str "ledger-home-" (System/nanoTime))))
          ws (io/file tmp-root (str "ledger-ws-" (System/nanoTime)))]
      (try
        (.mkdirs ws)
        (is (= (workspace/state-dir (str ws) home)
               (mission/workspace-state-dir (str ws) home))
            "the pure mirror and clj-surgeon.mcp-workspace/state-dir agree")
        (finally (delete-tree! ws) (delete-tree! (io/file home)))))))

(deftest a-moved-tree-refuses-before-any-write
  (testing "@stale-resume. A snapshot is a claim about a tree. `drift` names
            exactly which of the planned files no longer hash to what the plan
            froze, and the refusal it earns says nothing was written."
    (let [sources {"/a.clj" "(ns a)" "/b.clj" "(ns b)" "/c.clj" "(ns c)"}
          snap (mission/snapshot sources)
          unmoved (fn [path] (get sources path))
          moved (fn [path] (if (= "/b.clj" path) "(ns b) ;; edited" (get sources path)))
          gone (fn [path] (when-not (= "/c.clj" path) (get sources path)))]
      (is (= 3 (:files snap)))
      (is (:clean? (mission/drift snap unmoved)) "an unmoved tree is clean")
      (testing "one edited file is NAMED"
        (let [d (mission/drift snap moved)]
          (is (false? (:clean? d)))
          (is (= ["/b.clj"] (:changed d)))
          (let [r (mission/stale-refusal "M-1" snap d)]
            (is (mission/refused? r))
            (is (= "mission-snapshot-stale" (:error_type r)))
            (is (= ["/b.clj"] (:changed_files r)))
            (is (false? (:mutation_attempted r)))
            (is (true? (:source_unchanged r)))
            (is (= [:plan "M-1"] (:next-action r))
                "and it offers the one continuation that can help"))))
      (testing "a DELETED planned file is drift too, not a clean read"
        (is (= ["/c.clj"] (:changed (mission/drift snap gone))))))))

(deftest the-tool-says-cede-to-native-when-it-cannot-earn-its-return
  (testing "@native-escape. A mission costs the caller two returns; a native
            edit costs one. When the dossier cannot predict a saved return —
            one owner, one site, no proof obligation — the tool must say so."
    (let [tiny (mission/recommend {:caller_files 1 :sites 1} false)
          tiny-proved (mission/recommend {:caller_files 1 :sites 1} true)
          fixture-sized (mission/recommend {:caller_files 31 :sites 66} true)]
      (is (= :native (:recommendation tiny)))
      (is (string? (:because tiny)) "and it says why, in the caller's terms")
      (is (= :mission (:recommendation tiny-proved))
          "a proof obligation is itself a saved return: the caller would have
           to satisfy it by hand")
      (is (= :mission (:recommendation fixture-sized))))))

(deftest next-action-is-data-and-never-a-prescription-the-ledger-cannot-honour
  (is (= [:plan "M-1"] (mission/next-action {:id "M-1" :state :proposed})))
  (is (= [:apply "M-1"] (mission/next-action {:id "M-1" :state :ready})))
  (is (= [:resume "M-1"] (mission/next-action {:id "M-1" :state :verified})))
  (is (nil? (mission/next-action {:id "M-1" :state :blocked}))
      "a blocked mission's next action belongs to a human, and inventing
       [:apply id] for it would be a prescription with no continuation")
  (is (nil? (mission/next-action {:id "M-1" :state :undone}))))

;; ---------------------------------------------------------------------------
;; @migration-plan — missions that depend on missions

(def ^:private ledger
  "Three missions and no links: the base every graph witness starts from."
  [{:id "M-1" :state :ready :verb "helper_extraction"}
   {:id "M-2" :state :ready :verb "helper_extraction"}
   {:id "M-3" :state :ready :verb "helper_extraction"}])

(defn- index-of [missions] (mission/by-id missions))

(defn- linked
  "Apply a sequence of [from kind to] links to a ledger, asserting each one
   lands. Returns the new ledger."
  [missions edges]
  (reduce (fn [ms [from kind to]]
            (let [m (mission/link (get (index-of ms) from) kind to (index-of ms) "T")]
              (is (not (mission/refused? m)) (str from " " kind " " to))
              (mapv #(if (= from (:id %)) m %) ms)))
          missions
          edges))

(deftest linking-stores-only-the-forward-edge-and-derives-the-inverse
  (testing "one edge is one fact in one file. `dependents` and `superseded_by`
            are COMPUTED, because two copies of one edge are two things that can
            disagree in a directory a human may edit with an editor."
    (let [ms (linked ledger [["M-2" :depends-on "M-1"]])
          index (index-of ms)
          m2 (get index "M-2")
          m1 (get index "M-1")]
      (is (= ["M-1"] (:depends-on m2)))
      (is (nil? (:dependents m1)) "the inverse is NOT stored")
      (is (= ["M-2"] (:dependents (mission/dependency-view m1 index)))
          "and IS derived")
      (is (= [{:id "M-1" :state "ready"}]
             (:depends_on (mission/dependency-view m2 index)))
          "the DAG carries ids AND states")
      (testing "and the edge is in the mission's own history"
        (is (= {:from "ready" :to "ready" :event "link depends-on M-1" :at "T"}
               (last (:history m2)))))
      (testing "linking the same edge twice is a no-op, not a second entry"
        (let [again (mission/link m2 :depends-on "M-1" index "T2")]
          (is (= ["M-1"] (:depends-on again)))
          (is (= 1 (count (:history again)))))))))

(deftest a-cycle-is-refused-at-link-time-and-prints-the-loop
  (testing "adding m -> t closes a cycle exactly when t already reaches m, so
            the check is one walk of edges that already exist and NOTHING is
            written."
    (let [ms (linked ledger [["M-2" :depends-on "M-1"] ["M-3" :depends-on "M-2"]])
          index (index-of ms)
          refused (mission/link (get index "M-1") :depends-on "M-3" index "T")]
      (is (mission/refused? refused))
      (is (= "mission-dependency-cycle" (:error_type refused)))
      (is (= ["M-3" "M-2" "M-1" "M-3"] (:cycle refused))
          "the refusal prints the loop it would have made")
      (is (false? (:mutation_attempted refused)))
      (is (string? (:decision refused))))
    (testing "and a mission cannot depend on itself"
      (let [refused (mission/link (get (index-of ledger) "M-1") :depends-on "M-1"
                                  (index-of ledger) "T")]
        (is (= "mission-dependency-cycle" (:error_type refused)))))
    (testing "nor link to an id that is not in the ledger"
      (let [refused (mission/link (get (index-of ledger) "M-1") :depends-on "M-9"
                                  (index-of ledger) "T")]
        (is (= "mission-unknown-id" (:error_type refused)))
        (is (= ["M-1" "M-2" "M-3"] (:known refused)))))))

(deftest an-unverified-dependency-blocks-and-a-verified-one-releases
  (testing "@migration-plan. The blocked state is DERIVED on every read: the
            dependent's own file never changes when its dependency lands, which
            is the difference between one write at verify time and a fan-out
            write to every dependent."
    (let [ms (linked ledger [["M-2" :depends-on "M-1"]])
          index (index-of ms)
          m2 (get index "M-2")]
      (is (= :ready (:state m2)) "the STORED state is untouched")
      (is (= :blocked (mission/effective-state m2 index)) "the READ state is not")
      (is (= ["M-1"] (mission/unmet-dependencies m2 index)))
      (is (= "waiting on M-1" (mission/blocking-decision m2 index)))
      (testing "`ready` lists only the mission a machine can actually start"
        (is (= ["M-1" "M-3"] (mapv :id (mission/ready-missions ms))))
        (is (= [{:id "M-2" :state "blocked" :waiting_on ["M-1"]
                 :decision "waiting on M-1"}]
               (mission/waiting-missions ms))))
      (testing "apply refuses with a typed refusal that NAMES the ids"
        (let [refused (mission/dependency-refusal m2 index)]
          (is (mission/refused? refused))
          (is (= "mission-dependency-not-verified" (:error_type refused)))
          (is (= ["M-1"] (:unverified refused)))
          (is (false? (:mutation_attempted refused)))
          (is (= [:resume "M-1"] (:next-action refused)))))
      (testing "and the index reads blocked without anything being written"
        (is (str/includes? (nth (mission/index-lines ms) 2) "blocked"))
        (is (str/includes? (nth (mission/index-lines ms) 2) "waiting on M-1"))))

    (testing "the moment M-1 is :verified, M-2 is ready on the NEXT read"
      (let [ms (-> (linked ledger [["M-2" :depends-on "M-1"]])
                   (->> (mapv #(if (= "M-1" (:id %)) (assoc % :state :verified) %))))
            index (index-of ms)
            m2 (get index "M-2")]
        (is (empty? (mission/unmet-dependencies m2 index)))
        (is (= :ready (mission/effective-state m2 index)))
        (is (nil? (mission/dependency-refusal m2 index)))
        (is (= ["M-2" "M-3"] (mapv :id (mission/ready-missions ms))))
        (is (empty? (mission/waiting-missions ms)))))

    (testing "a dependency that is not in the ledger at all is UNMET and named,
              never silently met"
      (let [orphan {:id "M-7" :state :ready :depends-on ["M-404"]}]
        (is (= ["M-404"] (mission/unmet-dependencies orphan {})))
        (is (= :blocked (mission/effective-state orphan {})))))))

(deftest supersession-renders-the-whole-chain-from-either-end
  (testing "the builder's own note: answering a blocked decision means opening a
            NARROWER mission, and the two have to be linked or the ledger loses
            why the first one stopped."
    (let [ms (linked [{:id "M-1" :state :blocked
                       :decision {:decision "whether test/ is in scope"}}
                      {:id "M-2" :state :blocked
                       :decision {:decision "which destination lib"}}
                      {:id "M-3" :state :ready}]
                     [["M-2" :supersedes "M-1"] ["M-3" :supersedes "M-2"]])
          index (index-of ms)
          chain [{:id "M-3" :state "ready"}
                 {:id "M-2" :state "blocked"}
                 {:id "M-1" :state "blocked"}]]
      (is (= chain (mission/supersede-chain "M-1" index))
          "read from the OLDEST mission, the chain is the same")
      (is (= chain (mission/supersede-chain "M-3" index))
          "and from the newest")
      (is (= ["M-3"] (:superseded_by (mission/dependency-view (get index "M-2") index)))
          "derived, not stored")
      (let [lines (mission/dependency-lines (get index "M-1") index)]
        (is (str/includes? (str/join "\n" lines) "superseded-by<- M-2"))
        (is (str/includes? (str/join "\n" lines) "chain: M-3 [ready] -> M-2")))
      (testing "and a supersession cycle refuses like a dependency cycle"
        (is (= "mission-dependency-cycle"
               (:error_type (mission/link (get index "M-1") :supersedes "M-3"
                                          index "T"))))))))

(deftest the-index-summary-tells-the-truth-about-an-undone-mission
  (testing "ROUND-2 DEFECT: :undone fell through to the footprint arm and
            reported the size of a write that is no longer standing."
    (let [undone {:id "M-1" :state :undone :verb "helper_extraction"
                  :dossier {:footprint {:changed_files 30 :sites 41}}
                  :undo {:verified {:whole-files true}}}
          line (second (mission/index-lines [undone]))]
      (is (str/includes? line "undone; tree restored"))
      (is (str/includes? line "whole-file compare"))
      (is (not (str/includes? line "30 files"))
          "the write it describes is not there any more"))))

(deftest an-unadmitted-proof-authority-blocks-at-plan-time-not-apply-time
  (testing "@carry-the-proof. A mission may not reach :ready on a proof nobody
            has admitted, and the caller must learn that at open — never after
            paying for an apply that refuses for a reason plan already knew."
    (let [ws (io/file tmp-root (str "proof-ws-" (System/nanoTime)))
          home (io/file tmp-root (str "proof-home-" (System/nanoTime)))]
      (try
        (.mkdirs ws)
        (let [opened (cli/propose!
                      {:verb "helper_extraction"
                       :workspace (str ws) :state-home (str home)
                       :profiles {"admitted" {:commands [["/bin/true"]]}}
                       :request {:workspace_root (str ws)
                                 :verification {:profile "not-admitted"}}})]
          (is (= :blocked (:state opened)))
          (is (= false (get-in opened [:verification :admitted?])))
          (is (str/includes? (get-in opened [:decision :decision]) "not-admitted"))
          (is (= "mission-verification-profile-not-admitted"
                 (get-in opened [:decision :error_type])))
          (is (= ["admitted"] (get-in opened [:decision :evidence :admitted_profiles])))
          (is (nil? (:next-action opened))
              "and its next move belongs to a human, so nothing prescribes apply")
          (is (nil? (mission/verification-profiles opened))
              "an unadmitted authority is never reconstituted for a proof run"))
        (finally (delete-tree! ws) (delete-tree! (io/file home)))))))

;; ---------------------------------------------------------------------------
;; the end-to-end half
;;
;; One real extraction on the fixture's happy tree, driven only through the
;; mission verbs. The verification profile is a configured `/bin/true`: this
;; witness is about the LEDGER's behaviour around a real transaction, and the
;; proof's own content is exercised exhaustively by the helper_extraction
;; suite. `/bin/true` is a REAL configured profile that really launches -- it is
;; not a fixture flag and not an injected seam.

(def ^:private project-marker
  {"deps.edn" "{:paths [\"src\"]}\n"})

(defn- happy-pre-tree
  []
  (into project-marker
        (keep (fn [entry] (when-let [source (:pre entry)] [(:file entry) source])))
        (fixture/files :happy)))

(defn- materialize!
  [root tree]
  (doseq [[path source] tree]
    (let [target (io/file root path)]
      (io/make-parents target)
      (spit target source)))
  root)

(defn- tree-on-disk
  [root paths]
  (into {} (keep (fn [path]
                   (let [file (io/file root path)]
                     (when (.isFile file) [path (slurp file)]))))
        paths))

(def ^:private mission-profiles
  {"mission-proof" {:commands [["/bin/true"]]}})

;; The one witness this prototype exists for.
(deftest propose-show-apply-undo-on-the-real-fixture
  (testing "the whole FEEL, end to end: one bounded intent in, an id and a
            dossier out with no bytes written; show reads the same object back;
            apply publishes a terminal receipt INTO the mission file; undo puts
            every byte back and the mission says so."
    (let [stamp (System/nanoTime)
          root (io/file tmp-root (str "e2e-ws-" stamp))
          state-home (io/file tmp-root (str "e2e-home-" stamp))
          receipt-dir (io/file tmp-root (str "e2e-receipts-" stamp))
          pre (happy-pre-tree)]
      (try
        (materialize! root pre)
        (.mkdirs receipt-dir)
        (let [request (fixture/request {:workspace_root (str root)
                                        :verification {:profile "mission-proof"}})
              base {:workspace (str root) :state-home (str state-home)
                    :profiles mission-profiles
                    :receipt-dir (str receipt-dir)}
              state-dir (cli/state-dir-for (str root) (str state-home))]

          (testing "PROPOSE — an id, a dossier, and not one byte of the tree"
            (let [proposed (cli/propose! (assoc base :verb "helper_extraction"
                                                :request request))]
              (is (= "M-1" (:id proposed)) "the first mission in a fresh ledger")
              (is (= :ready (:state proposed))
                  (str "the happy fixture has no unresolved decision: "
                       (pr-str (:decision proposed))))
              (is (nil? (:decision proposed)))
              (is (= 6 (get-in proposed [:dossier :owners :helpers])))
              (is (= :mission (:recommendation proposed))
                  "@native-escape: 31 caller files under a proof obligation is
                   a return this tool earns")
              (is (= [:apply "M-1"] (:next-action proposed)))
              (is (pos? (get-in proposed [:snapshot :files]))
                  "@stale-resume: the plan's frozen bytes are snapshotted")
              (is (pos? (get-in proposed [:dossier :footprint :changed_files])))
              (is (= pre (tree-on-disk root (keys pre)))
                  "PROPOSE WRITES NO BYTES to the workspace")))

          (testing "SHOW — the same object, read back off disk"
            (let [shown (cli/show (assoc base :id "M-1"))]
              (is (= :ready (:state shown)))
              (is (= request (:intent shown))
                  "the intent is stored verbatim, so the dossier can be recomputed")
              (is (= [{:from nil :to "proposed" :event "open"}
                      {:from "proposed" :to "ready" :event "plan"}]
                     (mapv #(dissoc % :at) (:history shown))))))

          (testing "READY — the mission is listed as movable by a machine"
            (let [{:keys [ready]} (cli/ready base)]
              (is (= [{:id "M-1" :state "ready" :waiting_on "apply" :decision nil}]
                     ready))))

          (testing "@migration-plan — M-2 depends on M-1, and cannot start"
            (let [second-mission (cli/propose! (assoc base :verb "helper_extraction"
                                                      :request request))]
              (is (= "M-2" (:id second-mission)))
              (let [shown (cli/link! (assoc base :id "M-2" :depends-on "M-1"))]
                (is (= ["M-1"] (:depends-on shown)))
                (is (= :blocked (:effective_state shown))
                    "the READ state is blocked; the stored one is untouched")
                (is (= :ready (:state shown)))
                (is (= "waiting on M-1" (:decision_summary shown)))
                (is (str/includes? (str/join "\n" (:graph shown))
                                   "depends-on   -> M-1")))
              (testing "a cycle back the other way is refused at link time"
                (let [refused (cli/link! (assoc base :id "M-1" :depends-on "M-2"))]
                  (is (mission/refused? refused))
                  (is (= "mission-dependency-cycle" (:error_type refused)))
                  (is (= ["M-2" "M-1" "M-2"] (:cycle refused)))))
              (testing "`ready` lists ONLY M-1; M-2 is waiting on it"
                (let [{:keys [ready waiting]} (cli/ready base)]
                  (is (= ["M-1"] (mapv :id ready)))
                  (is (= [{:id "M-2" :state "blocked" :waiting_on ["M-1"]
                           :decision "waiting on M-1"}]
                         waiting))))
              (testing "and applying M-2 refuses, naming M-1, with nothing written"
                (let [refused (cli/apply! (assoc base :id "M-2"))]
                  (is (= "mission-dependency-not-verified" (:error_type refused)))
                  (is (= ["M-1"] (:unverified refused)))
                  (is (false? (:mutation_attempted refused)))
                  (is (= pre (tree-on-disk root (keys pre)))
                      "no byte of the workspace moved")
                  (is (= :ready (:state (cli/show (assoc base :id "M-2"))))
                      "and M-2 did not leave :ready")))))

          (testing "@stale-resume — a tree that MOVED refuses before any write"
            (let [snap (:snapshot (cli/show (assoc base :id "M-1")))
                  ;; a real planned owner, taken from the mission's own
                  ;; snapshot rather than guessed from the fixture's layout
                  victim (io/file (first (sort (keys (:by-file snap)))))
                  original (slurp victim)]
              (spit victim (str original "\n;; someone edited this\n"))
              (let [refused (cli/apply! (assoc base :id "M-1"))]
                (is (mission/refused? refused))
                (is (= "mission-snapshot-stale" (:error_type refused)))
                (is (= [(str victim)] (:changed_files refused))
                    "and it NAMES the file that moved")
                (is (false? (:mutation_attempted refused)))
                (is (= :ready (:state (cli/show (assoc base :id "M-1"))))
                    "the mission did not leave :ready: nothing was staged"))
              (spit victim original)
              (is (= pre (tree-on-disk root (keys pre)))
                  "the tree is back where the plan left it")))

          (testing "the bb READ path renders exactly what the JVM path does"
            (let [out (:out (clojure.java.shell/sh
                             "bb" "--classpath" "src" "bin/mission-read.clj"
                             "show" "M-1" "--workspace" (str root)
                             "--state-home" (str state-home)
                             :dir "."))]
              (is (= (cli/show (assoc base :id "M-1")) (edn/read-string out))
                  "one object, two entrances")))

          (testing "APPLY — the guarded transaction runs and the receipt lands
                    INSIDE the mission file"
            ;; @carry-the-proof: NO :profiles here. The mission carries the
            ;; authority its own proof runs under; the field report was an
            ;; apply refused for a profile map the caller had already supplied
            ;; once, at open.
            (let [applied (cli/apply! (assoc (dissoc base :profiles) :id "M-1"))]
              (is (= :verified (:state applied))
                  (str "terminal receipt: " (pr-str (:receipt applied))))
              (is (true? (get-in applied [:receipt :committed])))
              (is (= {:profile "mission-proof" :commands [["/bin/true"]]
                      :hash (mission/sha256 (pr-str ["mission-proof" [["/bin/true"]]]))
                      :admitted? true}
                     (:verification applied))
                  "the resolved authority is IN the mission, not in the call")
              (is (= "committed" (get-in applied [:receipt :status])))
              (is (string? (get-in applied [:undo :receipt])))
              (is (not= pre (tree-on-disk root (keys pre)))
                  "the tree really changed")
              (testing "and the mission ON DISK carries it, readable with cat"
                (let [raw (slurp (mission/mission-file state-dir "M-1"))
                      back (edn/read-string raw)]
                  (is (= :verified (:state back)))
                  (is (= "committed" (get-in back [:receipt :status])))
                  (is (str/includes? raw ":undo {:receipt"))))))

          (testing "@migration-plan — M-1 is :verified, so M-2 is ready NOW,
                    with nothing written to M-2's file to make it so"
            (let [{:keys [ready waiting]} (cli/ready base)]
              (is (= ["M-2"] (mapv :id ready)))
              (is (empty? waiting))
              (is (= :ready (:effective_state (cli/show (assoc base :id "M-2")))))
              (is (= [{:id "M-1" :state "verified"}]
                     (get-in (cli/show (assoc base :id "M-2"))
                             [:dependencies :depends_on])))))

          (testing "RESUME — one verb moves it from wherever it is; on a
                    :verified mission that means the inverse"
            (let [undone (cli/resume (assoc (dissoc base :profiles) :id "M-1"))]
              (is (= :undone (:state undone)))
              (is (true? (get-in undone [:undo :verified :whole-files])))
              (is (= pre (tree-on-disk root (keys pre)))
                  "the workspace is byte-identical to where it started")
              (testing "and a second resume refuses instead of running again"
                (let [again (cli/resume (assoc base :id "M-1"))]
                  (is (mission/refused? again))
                  (is (= "mission-illegal-transition" (:error_type again)))))))

          (testing "LIST — the human index tells the whole story in one line"
            (let [{:keys [index]} (cli/list-missions base)]
              (is (= 3 (count index)))
              (is (str/includes? (second index) "M-1"))
              (is (str/includes? (second index) "undone"))
              (is (str/includes? (second index) "tree restored")
                  "ROUND-2 DEFECT: an undone mission reported the size of a
                   write that is no longer standing")
              (is (str/includes? (nth index 2) "blocked")
                  "and M-2 is blocked again the moment M-1 stops being verified"))))
        (finally
          (delete-tree! root)
          (delete-tree! state-home)
          (delete-tree! receipt-dir))))))
