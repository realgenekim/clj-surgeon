(ns clj-surgeon.mission-typist-executor-test
  {:lane :battery}
  (:require
   [cheshire.core :as json]
   [clj-surgeon.mission :as mission]
   [clj-surgeon.mission-cli :as cli]
   [clj-surgeon.mission-typist-executor :as executor]
   [clj-surgeon.mission-typist-test :as facts]
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.test :refer [deftest is]]))

(def source "(ns fixture.core)\n\n(defn old-name [] 1)\n")
(def replacements [{:file "src/fixture/core.clj" :owner "old-name"
                    :form "(defn new-name [] 1)"}])
(defn profile [id expression]
  {:commands [["bb" "-cp" "src" "-e" expression]] :evidence id :measured-ms 100})
(def profiles
  {"gate" (profile "gate-receipt" "(require 'fixture.core) (assert (= 1 (fixture.core/new-name)))")
   "accept" (profile "witness-receipt" "(require 'fixture.core) (assert (nil? (ns-resolve 'fixture.core 'old-name))) (assert (= 1 ((ns-resolve 'fixture.core 'new-name))))")})

(defn with-fixture [f]
  (let [root (str (java.nio.file.Files/createTempDirectory
                    "typist-executor-test-" (make-array java.nio.file.attribute.FileAttribute 0)))
        file (io/file root "src/fixture/core.clj")]
    (try
      (.mkdirs (.getParentFile file))
      (spit file source)
      (f root file)
      (finally (executor/delete-tree! root)))))

(defn request [root]
  {:workspace_root root :owners [{:file "src/fixture/core.clj" :owner "old-name" :new-owner "new-name"}]
   :proof-files [] :intent "Rename old-name to new-name preserving behavior"
   :verification {:profile "gate"} :acceptance_profile "accept"
   :typist (-> facts/eligible
               (dissoc :sources :owners :gate :acceptance :intent)
               (assoc :source-policy {"src/fixture/core.clj" {:generated? false :reader-conditionals? false
                                                              :format-sensitive? false}}
                      :budget {:max-files 1 :max-changed-chars 1000}))})

(deftest real-proof-commit-and-undo
  (with-fixture
    (fn [root file]
      (let [request (request root)
            plan (executor/plan request profiles)
            plan (edn/read-string (pr-str plan))]
        (is (:ok plan))
        (with-redefs [executor/request-candidates! (fn [_] [{:usable true :content (json/generate-string replacements)}])]
          (let [result (executor/execute! request {:plan plan :receipt-dir (str (io/file root "receipts"))})]
            (is (:committed result) (pr-str result))
            (is (= 1 (:match-count result)))
            (is (= :complete (get-in result [:format :status])))
            (is (number? (get-in result [:format :elapsed_ms])))
            (is (re-find #"new-name" (slurp file)))
            (is (= :typist-invalid-undo-hash (:error-type (executor/undo! (:undo_receipt result) "wrong-hash"))))
            (is (:ok (executor/undo! (:undo_receipt result) (:receipt_hash result))))
            (is (= source (slurp file)))))))))

(deftest independent-witness-rejects-green-gate
  (with-fixture
    (fn [root file]
      (let [request (request root)
            plan (executor/plan request (assoc profiles "accept" (profile "different-witness" "(System/exit 1)")))]
        (with-redefs [executor/request-candidates! (fn [_] [{:usable true :content (json/generate-string replacements)}])]
          (let [result (executor/execute! request {:plan plan :receipt-dir (str (io/file root "receipts"))})]
            (is (= :typist-all-candidates-rejected (:error-type result)))
            (is (true? (get-in result [:candidates 0 :proof :gate :ok])))
            (is (false? (get-in result [:candidates 0 :proof :acceptance :ok])))
            (is (= source (slurp file)))))))))

(deftest stale-plan-refuses-before-transport
  (with-fixture
    (fn [root file]
      (let [request (request root) plan (executor/plan request profiles)]
        (spit file (str source "; concurrent change\n"))
        (with-redefs [executor/request-candidates! (fn [_] (throw (AssertionError. "transport must not run")))]
          (is (= :typist-stale-plan (:error-type (executor/execute! request {:plan plan})))))))))

(deftest persisted-ledger-plan-through-real-proof-and-undo
  (with-fixture
    (fn [root file]
      (let [home (str (io/file root "state"))
            opened (cli/propose! {:verb "owner_forms" :request (request root)
                                  :profiles profiles :state-home home})
            opts {:id (:id opened) :workspace root :state-home home
                  :receipt-dir (str (io/file root "receipts"))}]
        (is (= :ready (:state opened)) (pr-str opened))
        (is (get-in opened [:plan :typist :dossier :dossier-hash]))
        (with-redefs [executor/request-candidates! (fn [_] [{:usable true :content (json/generate-string replacements)}])]
          (let [applied (cli/apply! opts)]
            (is (= :verified (:state applied)) (pr-str applied))
            (is (= :typist (get-in applied [:receipt :executor])))
            (is (= :undone (:state (cli/undo! opts))))
            (is (= source (slurp file)))))))))

(deftest duplicate-proof-code-is-not-independent
  (with-fixture
    (fn [root _]
      (let [duplicate (assoc (:commands (get profiles "gate")) 0
                             (first (:commands (get profiles "gate"))))
            p (assoc-in profiles ["accept" :commands] duplicate)]
        (is (= :typist-identical-proof-commands
               (:error-type (executor/plan (request root) p))))))))

(deftest frozen-transport-race-through-real-proof
  (with-fixture
    (fn [root file]
      (let [request (-> (request root)
                        (assoc-in [:typist :rate :verified] 4)
                        (assoc-in [:typist :rate :attempted] 5))
            response (json/generate-string {:candidates [{:usable true :content (json/generate-string replacements)}]})
            client (str "import sys\nsys.stdin.read()\nprint(" (json/generate-string response) ")\n")
            plan (-> (executor/plan request profiles)
                     (assoc-in [:typist :transport :source] client)
                     (assoc-in [:typist :transport :sha256] (mission/sha256 client)))
            result (executor/execute! request {:plan plan :receipt-dir (str (io/file root "receipts"))})]
        (is (:committed result) (pr-str result))
        (is (= 3 (get-in result [:route :k])))
        (is (true? (get-in result [:transport :terminated?])))
        (is (empty? (get-in result [:transport :live-processes])))
        (is (:ok (executor/undo! (:undo_receipt result) (:receipt_hash result))))
        (is (= source (slurp file)))))))
