(ns ^{:lane :battery} clj-surgeon.mission-test
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
  {:lane :battery}
  (:require
   [clj-surgeon.helper-extraction-fixture :as fixture]
   [clj-surgeon.mcp-workspace :as workspace]
   [clj-surgeon.mission :as mission]
   [clj-surgeon.mission-cli :as cli]
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
  (is (= [:plan "M-1"] (mission/next-action {:id "M-1" :state :blocked}))
      "@caller-probe: a blocked mission used to return nil here. A real caller
       reported the cost — the ledger accumulated dead missions and offered no
       resolution — so the human move now has a verb: `plan <id> --spec-file`
       opens the superseding mission with the narrower intent.")
  (is (= [:plan "M-1"] (mission/next-action {:id "M-1" :state :failed})))
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
                 :decision "waiting on M-1" :next-action nil}]
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

(deftest a-dependency-that-verified-after-the-plan-forces-a-re-plan
  (testing "@replan-after-dependency. M-1 verifying does not make M-2 ready: it
            makes M-2's dossier a claim about a tree M-1 has since rewritten.
            The ledger knows that from two stamps it already has, so the caller
            is told to re-plan instead of paying for an apply that would refuse
            with the DOWNSTREAM hash symptom."
    (let [m2 {:id "M-2" :state :ready :verb "helper_extraction"
              :depends-on ["M-1"]
              :history [{:from nil :to "proposed" :event "open" :at "2026-09-05T10:00:00Z"}
                        {:from "proposed" :to "ready" :event "plan" :at "2026-09-05T10:00:01Z"}]}
          m1 (fn [verified-at]
               {:id "M-1" :state :verified
                :history [{:from "applied" :to "verified" :event "apply"
                           :at verified-at}]})
          before (mission/by-id [(m1 "2026-09-05T09:00:00Z") m2])
          after  (mission/by-id [(m1 "2026-09-05T11:00:00Z") m2])]

      (testing "a dependency verified BEFORE the plan is simply met"
        (is (empty? (mission/stale-dependencies m2 before)))
        (is (= :ready (mission/effective-state m2 before)))
        (is (= [:apply "M-2"] (mission/effective-next-action m2 before)))
        (is (nil? (mission/dependency-refusal m2 before))))

      (testing "a dependency verified AFTER the plan sends it back to :proposed"
        (is (= ["M-1"] (mission/stale-dependencies m2 after)))
        (is (= :proposed (mission/effective-state m2 after))
            "never :ready on a stale plan")
        (is (= :ready (:state m2)) "and the STORED state is untouched")
        (is (= [:plan "M-2"] (mission/effective-next-action m2 after)))
        (is (= "re-plan: M-1 changed the tree after this plan"
               (mission/blocking-decision m2 after))))

      (testing "apply refuses with the UPSTREAM reason, naming the dependency"
        (let [refused (mission/dependency-refusal m2 after)]
          (is (mission/refused? refused))
          (is (= "mission-dependency-replan-required" (:error_type refused)))
          (is (= ["M-1"] (:replanned_after refused)))
          (is (= "2026-09-05T10:00:01Z" (:planned_at refused)))
          (is (= ["2026-09-05T11:00:00Z"] (:dependency_verified_at refused)))
          (is (false? (:mutation_attempted refused)))
          (is (= [:plan "M-2"] (:next-action refused))
              "and the continuation is the re-plan, not the apply")))

      (testing "an UNMET dependency still wins: it is the earlier question"
        (let [unmet (mission/by-id [{:id "M-1" :state :ready} m2])]
          (is (= :blocked (mission/effective-state m2 unmet)))
          (is (= "mission-dependency-not-verified"
                 (:error_type (mission/dependency-refusal m2 unmet))))))

      (testing "`ready` holds it back and `waiting` says what to do about it"
        (let [ledger [(m1 "2026-09-05T11:00:00Z") m2]]
          (is (= [] (mapv :id (mission/ready-missions ledger)))
              "M-1 is :verified — its own next move is a resume, not a start —
               and M-2 is held for a re-plan, so NOTHING is offered as ready")
          (is (= [{:id "M-2" :state "proposed" :waiting_on ["M-1"]
                   :decision "re-plan: M-1 changed the tree after this plan"
                   :next-action [:plan "M-2"]}]
                 (mission/waiting-missions ledger)))))

      (testing "and a re-plan refreshes the stamp AND the snapshot, which is
                what puts the mission back to :ready"
        (let [projection {:state :ready
                          :dossier {:planned true}
                          :recommendation {:recommendation :mission :because "x"}
                          :snapshot (mission/snapshot {"/a.clj" "(ns a) ;; after M-1"})}
              refreshed (mission/replan m2 projection "2026-09-05T12:00:00Z")]
          (is (not (mission/refused? refreshed)))
          (is (= :ready (:state refreshed)))
          (is (= "2026-09-05T12:00:00Z" (mission/planned-at refreshed)))
          (is (= (:snapshot projection) (:snapshot refreshed))
              "the snapshot is the NEW tree's, not the one M-1 invalidated")
          (is (= {:from "ready" :to "ready" :event "plan" :at "2026-09-05T12:00:00Z"}
                 (last (:history refreshed)))
              "history records the re-plan; it is not a state move")
          (let [index (mission/by-id [(m1 "2026-09-05T11:00:00Z") refreshed])]
            (is (empty? (mission/stale-dependencies refreshed index)))
            (is (= :ready (mission/effective-state refreshed index)))
            (is (nil? (mission/dependency-refusal refreshed index))
                "and apply is no longer refused"))))

      (testing "a mission that already ran cannot be re-planned"
        (let [refused (mission/replan {:id "M-2" :state :verified} {} "T")]
          (is (= "mission-replan-illegal-state" (:error_type refused)))))

      (testing "and an incomplete re-plan refuses rather than inventing a move
                the transition table does not have"
        (let [refused (mission/replan m2 {:state :blocked
                                          :decision {:decision "which lib"}}
                                      "T")]
          (is (= "mission-replan-blocked" (:error_type refused)))
          (is (= "which lib" (:decision refused)))
          (is (false? (:mutation_attempted refused))))))))

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
          (is (= [:plan "M-1"] (:next-action opened))
              "@caller-probe: the repair verb, not silence")
          (is (nil? (mission/verification-profiles opened))
              "an unadmitted authority is never reconstituted for a proof run"))
        (finally (delete-tree! ws) (delete-tree! (io/file home)))))))

(deftest the-entrance-answers-the-caller-probes-four-dead-ends
  (testing "@caller-probe. A real caller spent 24 returns and never reached
            :verified. Each assertion here is one of the dead ends it hit."

    (testing "OPTION ORDER — a global option before the verb is not a reason to
              print help and exit 0, which the probe read as `it ran`"
      (is (= {:state-home "H" :workspace "W" :positional ["plan" "M-1"]}
             (cli/parse-flags ["--state-home" "H" "plan" "--workspace" "W" "M-1"])))
      (is (= {:state-home "H" :workspace "W" :positional ["plan" "M-1"]}
             (cli/parse-flags ["plan" "M-1" "--state-home" "H" "--workspace" "W"]))
          "before or after the verb, the same call")
      (is (= {:help true :positional ["plan"]}
             (cli/parse-flags ["plan" "--help"]))
          "a valueless flag is a boolean, not a swallower of the next token"))

    (testing "EXIT CODE — a failed receipt is a non-zero exit; all four of the
              probe's failed applies exited 0"
      (is (true? (cli/failed-receipt? {:state :failed})))
      (is (true? (cli/failed-receipt? {:receipt {:committed false}})))
      (is (false? (cli/failed-receipt? {:state :verified
                                        :receipt {:committed true}}))))

    (testing "DISCOVERABILITY — help carries the closed shape and the profile
              config, copy-paste, so the schema is not reverse-engineered one
              five-second refusal at a time"
      (let [text (cli/help-text nil)]
        (doseq [field [":workspace_root" ":from" ":to" ":alias_policy" ":helpers"
                       ":scope" ":paths" ":verification" ":profile"
                       ":verification-profiles" ".clj-surgeon.edn"]]
          (is (str/includes? text field) (str "help names " field)))
        (is (str/includes? (cli/help-text "apply") "Exits non-zero")
            "and one verb's help is one verb's")))

    (testing "@bb-help — help is PURE TEXT, so both entrances print the same
              bytes. Probe 2 spent eight returns on `help` at ~5 s of JVM start
              each, about a third of its whole run, to print a string that
              touches no planner and no tree."
      (doseq [verb [nil "apply" "plan"]]
        (let [out (:out (clojure.java.shell/sh
                          "bb" "--classpath" "src" "bin/mission-read.clj"
                          "help" (or verb "") :dir "."))]
          (is (= (mission/help-text verb) out)
              (str "bb and JVM help differ for " (pr-str verb)))))
      (is (= "help" (get mission/read-verbs "help"))
          "and `help` is declared a READ verb, which is what routes it to bb")
      (is (not (contains? mission/write-verbs "help"))))

    (testing "PROFILE DISCOVERY — the workspace's own config file is read, and
              `show` says where the ledger looked"
      (let [ws (io/file tmp-root (str "cfg-ws-" (System/nanoTime)))]
        (try
          (.mkdirs ws)
          (is (= {} (mission/configured-profiles (str ws) nil))
              "no file, no profiles — and no exception")
          (spit (io/file ws ".clj-surgeon.edn")
                (pr-str {:verification-profiles
                         {"mission-proof" {:commands [["/bin/true"]]}}}))
          (is (= {"mission-proof" {:commands [["/bin/true"]]}}
                 (mission/configured-profiles (str ws) nil))
              "the file the probe wrote, and four applies ignored")
          (is (= [{:path (str (io/file ws ".clj-surgeon.edn"))
                   :present true :readable true :profiles ["mission-proof"]}]
                 (mission/config-sources (str ws) nil))
              "and `show` can say WHERE it looked, so `no profile configured`
               is distinguishable from `your profile was ignored`")
          (is (= {"mission-proof" {:commands [["/bin/true"]]}}
                 (cli/admitted-profiles (str ws) {}))
              "with nothing passed in the spec at all")
          (is (= {:commands [["/other"]]}
                 (get (cli/admitted-profiles (str ws)
                                             {:profiles {"mission-proof"
                                                         {:commands [["/other"]]}}})
                      "mission-proof"))
              "an explicit spec still wins")
          (testing "and a mission opened with only the workspace config resolves
                    its authority as ADMITTED"
            (let [home (io/file tmp-root (str "cfg-home-" (System/nanoTime)))]
              (try
                (is (= {:profile "mission-proof"
                        :commands [["/bin/true"]]
                        :hash (mission/sha256
                                (pr-str ["mission-proof" [["/bin/true"]]]))
                        :admitted? true}
                       (mission/resolve-verification
                         {:verification {:profile "mission-proof"}}
                         (cli/admitted-profiles (str ws) {}))))
                (finally (delete-tree! home)))))
          (finally (delete-tree! ws)))))

    (testing "ZERO-BYTE OCCUPANT — a `target-exists` against an empty file is a
              fixture artifact, and the ledger says so in bytes"
      (let [ws (io/file tmp-root (str "occ-ws-" (System/nanoTime)))]
        (try
          (.mkdirs (io/file ws "src"))
          (spit (io/file ws "src" "response.clj") "")
          (let [sizes (#'cli/occupant-sizes {:file "src/response.clj"} (str ws))
                entry (val (first sizes))]
            (is (= 1 (count sizes)))
            (is (= 0 (:bytes entry)))
            (is (str/includes? (:note entry) "ZERO BYTES")))
          (finally (delete-tree! ws)))))))

(deftest a-dead-mission-is-repaired-by-superseding-it
  (testing "@caller-probe. The ledger `accumulated blocked/failed missions but
            offered no resolution transition`. A blocked intent may never be
            edited in place, so the repair is a NEW mission linked to the old."
    (let [ws (io/file tmp-root (str "repair-ws-" (System/nanoTime)))
          home (io/file tmp-root (str "repair-home-" (System/nanoTime)))
          base {:workspace (str ws) :state-home (str home)}]
      (try
        (.mkdirs ws)
        ;; M-1 blocks at plan time on an unadmitted proof authority
        (let [blocked (cli/propose! (assoc base :verb "helper_extraction"
                                           :request {:workspace_root (str ws)
                                                     :verification {:profile "absent"}}))]
          (is (= :blocked (:state blocked)))
          (is (= [:plan "M-1"] (:next-action blocked)) "the repair verb")
          (is (= cli/example-request (get-in blocked [:decision :example]))
              "and the closed shape that WOULD have been accepted")

          (testing "repairing it opens a NEW mission that supersedes the old"
            (let [repaired (cli/repair!
                             (assoc base :id "M-1" :verb "helper_extraction"
                                    :profiles {"admitted" {:commands [["/bin/true"]]}}
                                    :request {:workspace_root (str ws)
                                              :verification {:profile "admitted"}}))]
              (is (= "M-2" (:id repaired)) "a new id, not an edited intent")
              (is (= "M-1" (:repaired repaired)))
              (is (= ["M-1"] (:supersedes repaired)))
              (is (= [{:id "M-2" :state (name (:state repaired))}
                      {:id "M-1" :state "blocked"}]
                     (get-in repaired [:dependencies :chain]))
                  "and the chain reads from either end")))

          (testing "a mission that already ran is re-planned, never superseded:
                    its record is the only evidence of what it did"
            (let [state-dir (cli/state-dir-for (str ws) (str home))]
              (mission/write-mission! state-dir
                                      {:id "M-9" :state :verified
                                       :verb "helper_extraction"})
              (let [refused (cli/repair! (assoc base :id "M-9"
                                                :request {:workspace_root (str ws)}))]
                (is (= "mission-repair-illegal-state" (:error_type refused)))
                (is (= [:plan "M-9"] (:next-action refused))))))

          (testing "and a repair with no corrected intent says what it needs"
            (let [refused (cli/repair! (assoc base :id "M-1"))]
              (is (= "mission-repair-needs-a-request" (:error_type refused)))
              (is (map? (:example refused))))))
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
            (let [shown (cli/show (assoc base :id "M-1" :full true))]
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
                           :decision "waiting on M-1" :next-action nil}]
                         waiting))))
              (testing "and applying M-2 refuses, naming M-1, with nothing written"
                (let [refused (cli/apply! (assoc base :id "M-2"))]
                  (is (= "mission-dependency-not-verified" (:error_type refused)))
                  (is (= ["M-1"] (:unverified refused)))
                  (is (false? (:mutation_attempted refused)))
                  (is (= pre (tree-on-disk root (keys pre)))
                      "no byte of the workspace moved")
                  (is (= :ready (:state (cli/show (assoc base :id "M-2" :full true))))
                      "and M-2 did not leave :ready")))))

          (testing "@stale-resume — a tree that MOVED refuses before any write"
            (let [snap (:snapshot (cli/show (assoc base :id "M-1" :full true)))
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
                (is (= :ready (:state (cli/show (assoc base :id "M-1" :full true))))
                    "the mission did not leave :ready: nothing was staged"))
              (spit victim original)
              (is (= pre (tree-on-disk root (keys pre)))
                  "the tree is back where the plan left it")))

          (testing "the bb READ path renders exactly what the JVM path does"
            (let [out (:out (clojure.java.shell/sh
                              "bb" "--classpath" "src" "bin/mission-read.clj"
                              "show" "M-1" "--workspace" (str root)
                              "--state-home" (str state-home) "--full"
                              :dir "."))]
              (is (= (cli/show (assoc base :id "M-1" :full true)) (edn/read-string out))
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

          (testing "@replan-after-dependency — M-1 verified AFTER M-2 was
                    planned, so M-2 is NOT ready: its dossier describes a tree
                    M-1 has since rewritten. Nothing was written to M-2 to make
                    that true; it is two stamps the ledger already had."
            (let [{:keys [ready waiting]} (cli/ready base)
                  shown (cli/show (assoc base :id "M-2" :full true))]
              (is (= [] (mapv :id ready))
                  "M-2 is NOT offered as ready, and M-1 has already run")
              (is (= [{:id "M-2" :state "proposed" :waiting_on ["M-1"]
                       :decision "re-plan: M-1 changed the tree after this plan"
                       :next-action [:plan "M-2"]}]
                     waiting))
              (is (= :proposed (:effective_state shown)))
              (is (= :ready (:state shown)) "the STORED state is untouched")
              (is (= [:plan "M-2"] (:effective_next_action shown)))
              (is (= [{:id "M-1" :state "verified"}]
                     (get-in shown [:dependencies :depends_on]))))
            (testing "and apply refuses with the UPSTREAM reason, ahead of the
                      snapshot hash gate"
              (let [refused (cli/apply! (assoc (dissoc base :profiles) :id "M-2"))]
                (is (= "mission-dependency-replan-required" (:error_type refused))
                    (str "not the downstream hash symptom: " (pr-str refused)))
                (is (= ["M-1"] (:replanned_after refused)))
                (is (false? (:mutation_attempted refused)))
                (is (= [:plan "M-2"] (:next-action refused)))))
            (testing "`plan M-2` re-plans in place: no new id, no re-supplied
                      spec, and the proof authority comes from the mission"
              (let [replanned (cli/replan! (assoc (dissoc base :profiles) :id "M-2"))]
                (is (= "M-2" (:id replanned))
                    (str "no new mission was opened: " (pr-str replanned)))
                ;; HONEST OUTCOME, pinned. M-2's intent in this witness is the
                ;; SAME extraction as M-1's, so once M-1 retired those helpers
                ;; the intent no longer describes anything: the re-plan refuses
                ;; with the planner's own decision rather than inventing a
                ;; dossier. The successful re-plan -> :ready -> apply loop is
                ;; witnessed deterministically in
                ;; `a-dependency-that-verified-after-the-plan-forces-a-re-plan`.
                (is (mission/refused? replanned))
                (is (= "mission-replan-blocked" (:error_type replanned)))
                (is (string? (:decision replanned))
                    "and it carries the ONE decision the planner is waiting on")
                (is (false? (:mutation_attempted replanned)))
                (is (= :ready (:state (cli/show (assoc base :id "M-2" :full true))))
                    "a refused re-plan leaves the mission exactly as it was"))))

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

;; Optional owner_forms dispatch retains the exact planner snapshot.
(def typist-request {:workspace_root "/fixture" :owners [{:file "a.clj" :owner "a"}]
                     :intent "change a" :verification {:profile "gate"}
                     :acceptance_profile "accept"})
(def typist-planned {:ok true :sources {"/fixture/a.clj" "(def a 1)"}
                     :typist {:dossier {:intent "frozen"} :route {:k 1} :basis "basis"}})

(deftest verb-specific-dossier-and-blocked-evidence
  (is (= :ready (:state (cli/plan-dossier "owner_forms" typist-planned typist-request))))
  (is (= {:dossier {:intent "frozen"} :route {:k 1}}
         (get-in (cli/plan-dossier "owner_forms" typist-planned typist-request) [:dossier :typist])))
  (let [refusal {:ok false :error_type "typist-not-admitted" :error "missing proof"
                 :decision "provide evidence"}
        p (cli/plan-dossier "owner_forms" refusal typist-request)]
    (is (= :blocked (:state p)))
    (is (= "typist-not-admitted" (get-in p [:decision :error_type]))))
  ;; Field witness: commit-candidate! contains comments; the old projection
  ;; dropped its hyphenated refusal code and supplied a helper example.
  (let [request (assoc typist-request :owners [{:file "executor.clj" :owner "commit-candidate!"}])
        refusal {:ok false :committed false :error-type :forms-protected-syntax
                 :mutation-attempted false}
        p (cli/plan-dossier "owner_forms" refusal request)
        saved (atom nil)]
    (is (= "forms-protected-syntax" (get-in p [:decision :error_type])))
    (is (re-find #"(?i)comments" (get-in p [:decision :because] "")))
    (is (re-find #"smaller supported owner|native edit" (get-in p [:decision :decision] "")))
    (is (= "owner_forms" (get-in p [:decision :example :verb])))
    (is (= (:owners request) (get-in p [:decision :example :request :owners])))
    (is (true? (get-in p [:decision :example :requires-decision])))
    (with-redefs-fn
      {#'cli/verbs {"owner_forms" {:plan (fn [& _] refusal)}}
       #'cli/state-dir-for (fn [& _] "/ledger")
       #'cli/admitted-profiles (fn [& _] {"gate" {:commands [["bb" "test"]]}})
       #'mission/next-id (fn [_] "m1")
       #'cli/save! (fn [_ m] (reset! saved m))}
      #(do
         (cli/propose! {:verb "owner_forms" :request request})
         (is (= :blocked (:state @saved)))
         (is (= (:decision p) (:decision @saved)))))))

(deftest helper-projection-stays-identical
  (is (= (mission/dossier typist-planned typist-request)
         (cli/plan-dossier "helper_extraction" typist-planned typist-request))))

(deftest proposal-persists-frozen-plan-and-apply-does-not-replan
  (let [saved (atom nil) calls (atom [])
        profiles {"gate" {:commands [["bb" "test.clj"]]}}
        route {:plan (fn [r p] (swap! calls conj [:plan r p]) typist-planned)
               :execute! (fn [r c] (swap! calls conj [:execute r c])
                           {:committed true :undo_receipt "undo.edn" :receipt_hash "h"})}]
    (with-redefs-fn {#'cli/verbs {"owner_forms" route}
                     #'cli/state-dir-for (fn [& _] "/ledger")
                     #'cli/admitted-profiles (fn [& _] profiles)
                     #'cli/stale? (fn [_] nil)
                     #'mission/next-id (fn [_] "m1")
                     #'mission/read-mission (fn [& _] @saved)
                     #'mission/read-all (fn [_] [])
                     #'cli/save! (fn [_ m] (reset! saved m))}
      #(do
         (cli/propose! {:verb "owner_forms" :request typist-request})
         (is (= :ready (:state @saved)))
         (is (= typist-planned (:plan @saved)))
         (is (= typist-request (:intent @saved)))
         (cli/apply! {:id "m1" :workspace "/fixture"})
         (is (= [:plan :execute] (mapv first @calls)))
         (is (= typist-planned (get-in @calls [1 2 :plan])))
         (is (= :verified (:state @saved)))))))

(deftest replan-replaces-plan-only-when-admitted
  (let [saved (atom {:id "m1" :verb "owner_forms" :state :ready :root "/fixture"
                     :intent typist-request :plan {:old true}
                     :verification {:profile "gate" :commands [["bb" "gate"]]}})
        next-plan (atom typist-planned)
        profiles-seen (atom nil)]
    (with-redefs-fn {#'cli/verbs {"owner_forms" {:plan (fn [_ profiles]
                                                         (reset! profiles-seen profiles)
                                                         @next-plan)}}
                     #'cli/state-dir-for (fn [& _] "/ledger")
                     #'cli/admitted-profiles (fn [& _] {"gate" {} "accept" {}})
                     #'mission/read-mission (fn [& _] @saved)
                     #'mission/read-all (fn [_] [@saved])
                     #'cli/save! (fn [_ m] (reset! saved m))}
      #(do
         (cli/replan! {:id "m1" :workspace "/fixture"})
         (is (contains? @profiles-seen "accept"))
         (is (= typist-planned (:plan @saved)))
         (reset! next-plan {:ok false :error_type "blocked"})
         (is (mission/refused? (cli/replan! {:id "m1" :workspace "/fixture"})))
         (is (= typist-planned (:plan @saved)))))))

(deftest owner-forms-resolves-only-the-selected-executor-entry
  (let [calls (atom [])]
    (with-redefs [clojure.core/requiring-resolve
                  (fn [sym] (fn [& args] (swap! calls conj [sym args]) :called))]
      (is (= :called ((get-in cli/verbs ["owner_forms" :plan]) typist-request {})))
      (is (= :called ((get-in cli/verbs ["owner_forms" :execute!]) typist-request {:plan typist-planned})))
      (is (= :called ((get-in cli/verbs ["owner_forms" :undo]) "receipt.edn" "expected-hash")))
      (is (= '[clj-surgeon.mission-typist-executor/plan
               clj-surgeon.mission-typist-executor/execute!
               clj-surgeon.mission-typist-executor/undo!]
             (mapv first @calls)))
      (is (= '("receipt.edn" "expected-hash") (second (last @calls))))))
  (let [calls (atom [])
        stored {:id "m1" :verb "owner_forms" :state :verified
                :undo {:receipt "receipt.edn" :receipt_hash "wrong-stored-hash"}}]
    (with-redefs-fn
      {#'cli/verbs {"owner_forms"
                    {:undo (fn [path expected]
                             (swap! calls conj [path expected])
                             {:ok false :error-type :typist-invalid-undo-hash})}}
       #'cli/state-dir-for (fn [& _] "/ledger")
       #'mission/read-mission (fn [& _] stored)
       #'io/file (fn [& _] (proxy [java.io.File] ["receipt.edn"] (isFile [] true)))
       #'cli/save! (fn [& _] (throw (ex-info "must not save a refused undo" {})))}
      #(do
         (is (mission/refused? (cli/undo! {:id "m1" :workspace "/fixture"})))
         (is (= [["receipt.edn" "wrong-stored-hash"]] @calls))))))

(deftest owner-forms-publishes-recovery-before-a-crashed-write
  (let [saved (atom {:id "m1" :verb "owner_forms" :state :ready :root "/fixture"
                     :intent typist-request :plan typist-planned})
        recovery {:receipt "/receipts/inverse.edn" :receipt_hash "hash"
                  :artifacts "/receipts"}
        published (atom nil)]
    (with-redefs-fn
      {#'cli/verbs {"owner_forms"
                    {:execute! (fn [_ config]
                                 ((:persist-recovery! config) recovery)
                                 (reset! published @saved)
                                 (throw (ex-info "simulated crash before write" {})))}}
       #'cli/state-dir-for (fn [& _] "/ledger")
       #'cli/admitted-profiles (fn [& _] {})
       #'cli/stale? (fn [_] nil)
       #'mission/read-mission (fn [& _] @saved)
       #'mission/read-all (fn [_] [])
       #'cli/save! (fn [_ m] (reset! saved m))}
      #(do
         (is (thrown? clojure.lang.ExceptionInfo
                      (cli/apply! {:id "m1" :workspace "/fixture"})))
         (is (= :applied (:state @saved)))
         (is (= @published @saved))
         (is (= (select-keys recovery [:receipt :receipt_hash]) (:undo @saved)))
         (is (= recovery (get-in @saved [:proof :typist-recovery])))
         (is (= typist-planned (:plan @saved)))))))

(deftest owner-forms-recovery-save-failure-prevents-executor-continuation
  (let [mission {:id "m1" :verb "owner_forms" :state :ready :root "/fixture"
                 :intent typist-request :plan typist-planned}
        saves (atom 0) continued (atom false)]
    (with-redefs-fn
      {#'cli/verbs {"owner_forms"
                    {:execute! (fn [_ config]
                                 ((:persist-recovery! config) {:receipt "inverse" :receipt_hash "hash"})
                                 (reset! continued true))}}
       #'cli/state-dir-for (fn [& _] "/ledger")
       #'cli/admitted-profiles (fn [& _] {})
       #'cli/stale? (fn [_] nil)
       #'mission/read-mission (fn [& _] mission)
       #'mission/read-all (fn [_] [])
       #'cli/save! (fn [_ m] (if (= 1 (swap! saves inc)) m
                               (throw (ex-info "recovery persistence failed" {}))))}
      #(do
         (is (thrown? clojure.lang.ExceptionInfo
                      (cli/apply! {:id "m1" :workspace "/fixture"})))
         (is (= 2 @saves))
         (is (false? @continued))))))
