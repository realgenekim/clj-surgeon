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
   [clojure.edn :as edn]
   [clojure.java.io :as io]
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
      (is (= "whether test/ is in this write's scope" (:question decision)))
      (is (= ["test/acid/web/http_test.clj"] (get-in decision [:evidence :files]))
          "the refusal's evidence travels with the decision"))))

(deftest ready-lists-exactly-what-can-move-and-who-moves-it
  (let [missions [{:id "M-1" :state :proposed}
                  {:id "M-2" :state :ready}
                  {:id "M-3" :state :blocked
                   :decision {:question "which path the destination occupies"}}
                  {:id "M-4" :state :verified}]
        ready (mission/ready-missions missions)]
    (is (= ["M-2" "M-3"] (mapv :id ready)))
    (is (= ["apply" "a decision"] (mapv :waiting_on ready))
        "a ready mission is moved by a machine; a blocked one by a human")
    (is (= "which path the destination occupies" (:question (second ready))))
    (testing "and the index is one fixed-column line per mission"
      (let [lines (mission/index-lines missions)]
        (is (= 5 (count lines)))
        (is (str/starts-with? (first lines) "ID "))
        (is (every? #(< (count %) 120) lines))))))

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
              (is (pos? (get-in proposed [:dossier :footprint :changed_files])))
              (is (= pre (tree-on-disk root (keys pre)))
                  "PROPOSE WRITES NO BYTES to the workspace")))

          (testing "SHOW — the same object, read back off disk"
            (let [shown (cli/show (assoc base :id "M-1"))]
              (is (= :ready (:state shown)))
              (is (= request (:intent shown))
                  "the intent is stored verbatim, so the dossier can be recomputed")
              (is (= [{:from nil :to "proposed" :event "propose"}
                      {:from "proposed" :to "ready" :event "propose"}]
                     (mapv #(dissoc % :at) (:history shown))))))

          (testing "READY — the mission is listed as movable by a machine"
            (let [{:keys [ready]} (cli/ready base)]
              (is (= [{:id "M-1" :state "ready" :waiting_on "apply" :question nil}]
                     ready))))

          (testing "APPLY — the guarded transaction runs and the receipt lands
                    INSIDE the mission file"
            (let [applied (cli/apply! (assoc base :id "M-1"))]
              (is (= :verified (:state applied))
                  (str "terminal receipt: " (pr-str (:receipt applied))))
              (is (true? (get-in applied [:receipt :committed])))
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

          (testing "UNDO — the inverse verifies and every byte comes back"
            (let [undone (cli/undo! (assoc base :id "M-1"))]
              (is (= :undone (:state undone)))
              (is (true? (get-in undone [:undo :verified :whole-files])))
              (is (= pre (tree-on-disk root (keys pre)))
                  "the workspace is byte-identical to where it started")
              (testing "and a second undo refuses instead of running again"
                (let [again (cli/undo! (assoc base :id "M-1"))]
                  (is (mission/refused? again))
                  (is (= "mission-illegal-transition" (:error_type again)))))))

          (testing "LIST — the human index tells the whole story in one line"
            (let [{:keys [index]} (cli/list-missions base)]
              (is (= 2 (count index)))
              (is (str/includes? (second index) "M-1"))
              (is (str/includes? (second index) "undone")))))
        (finally
          (delete-tree! root)
          (delete-tree! state-home)
          (delete-tree! receipt-dir))))))
