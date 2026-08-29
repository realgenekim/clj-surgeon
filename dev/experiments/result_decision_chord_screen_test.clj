(ns result-decision-chord-screen-test
  (:require
   [cheshire.core :as json]
   [clj-surgeon.mcp-server :as mcp-server]
   [clojure.java.io :as io]
   [clojure.test :refer [deftest is testing]]
   [owner-aware-mcp-surface-observer :as surface-observer]
   [result-decision-chord-capture-server :as server]
   [result-decision-chord-screen :as screen]))

(def exact-edit
  {:edits [{:file screen/source-file
            :within {:form "greet"}
            :from "\"Hello, \""
            :to "\"Welcome, \""}]})

(defn- capture-file [calls]
  (let [file (java.io.File/createTempFile "decision-chord" ".json")]
    (spit file (json/generate-string
                 {:schema "clj-surgeon.result-decision-chord-capture.v1"
                  :calls calls}))
    (.getAbsolutePath file)))

(defn- json-file [prefix value]
  (let [file (java.io.File/createTempFile prefix ".json")]
    (spit file (json/generate-string value))
    (.getAbsolutePath file)))

(defn- json-lines-file [prefix values]
  (let [file (java.io.File/createTempFile prefix ".jsonl")]
    (spit file (str (clojure.string/join
                      "\n" (map json/generate-string values))
                    "\n"))
    (.getAbsolutePath file)))

(defn- edn-file [prefix value]
  (let [file (java.io.File/createTempFile prefix ".edn")]
    (spit file (pr-str value))
    (.getAbsolutePath file)))

(defn- isolation [suffix]
  {:workspace (str "/tmp/workspace-" suffix)
   :codex-home (str "/tmp/codex-home-" suffix)
   :server-pid suffix
   :capture (str "/tmp/capture-" suffix ".json")
   :events (str "/tmp/events-" suffix ".jsonl")
   :capture-sha256 (format "%064x" suffix)
   :events-sha256 (format "%064x" (+ 10 suffix))})

(defn- attempt-ledger [runs]
  (mapcat (fn [{:keys [position arm run-id correct]}]
            [{:event "launched" :position position :arm (name arm) :run-id run-id}
             {:event "completed" :position position :arm (name arm) :run-id run-id}
             {:event "scored" :position position :arm (name arm) :run-id run-id
              :correct correct}])
          runs))

(deftest compiler-scores-semantic-future-not-request-spelling
  (testing "the narrow literal request reaches the frozen future"
    (is (screen/correct-future? (screen/compile-edit exact-edit))))
  (testing "a wider exact subtree can reach the same future"
    (is (screen/correct-future?
          (screen/compile-edit
            {:edits [{:file screen/source-file
                      :within {:form "greet"}
                      :from "(str \"Hello, \" name)"
                      :to "(str \"Welcome, \" name)"}]}))))
  (testing "a parseable but wrong future is rejected"
    (is (not (screen/correct-future?
               (screen/compile-edit
                 {:edits [{:file screen/source-file
                           :within {:form "greet"}
                           :from "\"Hello, \""
                           :to "\"Hi, \""}]})))))
  (testing "publicly forbidden fields never earn a correct future"
    (doseq [request [(assoc exact-edit :verify "exact")
                     (assoc exact-edit :expect {:files 1})
                     (assoc exact-edit :unexpected true)]]
      (let [result (screen/compile-edit request)]
        (is (false? (:ok result)))
        (is (= :public-schema-denied (:error-type result)))
        (is (false? (screen/correct-future? result)))))))

(deftest cohort-gates-order-correctness-both-pairs-and-twenty-percent
  (let [common {:catalog {:ok true}
                :advertised-catalog-sha256 "advertised"
                :client-catalog-sha256 "client"
                :structured-content-sha256 "structured"}
        passing [(merge common {:position 1 :arm :control :run-id "r1" :correct true
                                :inspect-to-edit-ms 100.0 :logical-edit-argument-bytes 200
                                :visible-content-sha256 "control" :isolation (isolation 1)})
                 (merge common {:position 2 :arm :treatment :run-id "r2" :correct true
                                :inspect-to-edit-ms 70.0 :logical-edit-argument-bytes 190
                                :visible-content-sha256 "treatment" :isolation (isolation 2)})
                 (merge common {:position 3 :arm :treatment :run-id "r3" :correct true
                                :inspect-to-edit-ms 75.0 :logical-edit-argument-bytes 190
                                :visible-content-sha256 "treatment" :isolation (isolation 3)})
                 (merge common {:position 4 :arm :control :run-id "r4" :correct true
                                :inspect-to-edit-ms 110.0 :logical-edit-argument-bytes 200
                                :visible-content-sha256 "control" :isolation (isolation 4)})]
        report #(screen/cohort-report % (attempt-ledger %))]
    (is (:ok (report passing)))
    (is (false? (:ok (report (assoc-in passing [2 :correct] false)))))
    (is (false? (:ok (screen/cohort-report
                       (assoc-in passing [2 :inspect-to-edit-ms] 120.0)
                       (attempt-ledger passing)))))
    (is (false? (:ok (screen/cohort-report
                       (mapv #(update % :inspect-to-edit-ms
                                      (fn [value]
                                        (if (= :treatment (:arm %))
                                          (+ value 18.0)
                                          value)))
                             passing)
                       (attempt-ledger passing)))))
    (is (false? (:ok (report (vec (reverse passing))))))
    (is (false? (:ok (screen/cohort-report
                       (assoc-in passing [2 :isolation] (isolation 2))
                       (attempt-ledger passing)))))
    (is (false? (:ok (screen/cohort-report
                       (assoc-in passing [2 :logical-edit-argument-bytes] 250)
                       (attempt-ledger passing)))))
    (is (false? (:ok (screen/cohort-report passing (drop-last (attempt-ledger passing))))))))

(def inspect-arguments
  {:requests [{:id "target"
               :operation "forms"
               :file screen/source-file
               :forms ["greet"]
               :expect {:forms 1}}]
   :expect {:files 1 :requests 1}})

(defn- event [event-type item]
  {:type event-type :item item})

(def exact-events
  [(event "item.started"
          {:id "inspect-1" :type "mcp_tool_call" :server "clj-surgeon"
           :tool "inspect_clojure" :arguments inspect-arguments})
   (event "item.completed"
          {:id "inspect-1" :type "mcp_tool_call" :server "clj-surgeon"
           :tool "inspect_clojure" :status "completed"
           :result {:content [{:type "text"
                               :text (first server/frozen-inspect-content)}]
                    :structured_content
                    (screen/json-roundtrip server/frozen-inspect-result)
                    :error nil}})
   (event "item.started"
          {:id "edit-1" :type "mcp_tool_call" :server "clj-surgeon"
           :tool "edit_clojure" :arguments exact-edit})
   (event "item.completed"
          {:id "edit-1" :type "mcp_tool_call" :server "clj-surgeon"
           :tool "edit_clojure"})
   (event "item.completed"
          {:id "message-1" :type "agent_message" :text "Captured."})])

(def exact-timing
  {:observations [{:event-kind :mcp-tool-call-completed
                   :item-id "inspect-1" :observer-monotonic-ns 1000000000}
                  {:event-kind :mcp-tool-call-started
                   :item-id "edit-1" :observer-monotonic-ns 2500000000}]
   :process-wall-ms 3000.0})

(def exact-calls
  [{:phase "inspect" :tool_name "inspect_clojure" :params inspect-arguments}
   {:phase "next-action" :tool_name "edit_clojure" :params exact-edit}])

(def valid-advertised
  {:instructions mcp-server/server-instructions
   :tools (screen/expected-advertised-tools)})

(def valid-client-tools
  (->> (:tools valid-advertised)
       (map surface-observer/advertised->client-tool)
       (sort-by :name)
       vec))

(def valid-registry
  {:schema "clj-surgeon.codex-mcp-registry.v1"
   :ok true
   :observation-source
   (assoc surface-observer/registry-observation-source
          :server-selector {:field "name" :value "clj-surgeon"})
   :server "clj-surgeon"
   :tool-names (mapv :name valid-client-tools)
   :tool-projection valid-client-tools})

(deftest full-catalog-evidence-refuses-order-cardinality-and-peer-drift
  (let [advertised (json-file "advertised" valid-advertised)
        result #(screen/catalog-evidence advertised (json-file "registry" %))]
    (is (:ok (result valid-registry)))
    (is (false? (:ok (result
                       (assoc valid-registry :tool-projection
                              (vec (concat (rest valid-client-tools)
                                           [(first valid-client-tools)])))))))
    (is (false? (:ok (result
                       (update valid-registry :tool-projection
                               conj (first valid-client-tools))))))
    (is (false? (:ok (result
                       (update valid-registry :tool-projection pop)))))
    (is (false? (:ok (result
                       (-> valid-registry
                           (update :tool-projection pop)
                           (update :tool-names pop))))))))

(deftest capture-requires-exact-two-call-route-and-result-evidence
  (let [advertised (json-file "advertised" valid-advertised)
        registry (json-file "registry" valid-registry)
        events (json-lines-file "events" exact-events)
        timing (edn-file "timing" exact-timing)
        base {:arm :control :position 1 :run-id "r1"
              :events events :timing timing
              :advertised advertised :registry registry}
        score #(screen/score-capture
                 (assoc base :capture (capture-file %)))]
    (is (:correct (score exact-calls)))
    (doseq [calls [[(second exact-calls)]
                   [(first exact-calls)]
                   [(first exact-calls) (second exact-calls) (second exact-calls)]
                   [(first exact-calls)
                    {:phase "next-action"
                     :tool_name "apply_clojure_changes" :params {}}]]]
      (is (false? (:correct (score calls)))))))

(deftest lifecycle-joins-the-exact-two-calls-and-direct-clock
  (let [result (screen/strict-lifecycle :control exact-events exact-timing exact-calls)]
    (is (:ok result))
    (is (= 1500.0 (:inspect-to-edit-ms result))))
  (testing "a preamble is observable and refuses"
    (is (false?
          (:ok (screen/strict-lifecycle :control
                                        (into [(event "item.completed"
                                                      {:id "early" :type "agent_message" :text "Working."})]
                                              exact-events)
                                        exact-timing exact-calls)))))
  (testing "capture and event arguments must agree"
    (is (false?
          (:ok (screen/strict-lifecycle :control
                                        exact-events exact-timing
                                        (assoc-in exact-calls [1 :params :edits 0 :to] "\"Hi, \""))))))
  (testing "completion must follow its matching start"
    (is (false?
          (:ok (screen/strict-lifecycle :control
                                        [(nth exact-events 1) (nth exact-events 0)
                                         (nth exact-events 2) (nth exact-events 3) (nth exact-events 4)]
                                        exact-timing exact-calls)))))
  (testing "completed-only file changes refuse"
    (is (false?
          (:ok (screen/strict-lifecycle :control
                                        (conj exact-events
                                              (event "item.completed"
                                                     {:id "file-1" :type "file_change"}))
                                        exact-timing exact-calls)))))
  (testing "a boundary larger than process wall refuses"
    (is (false?
          (:ok (screen/strict-lifecycle :control
                                        exact-events
                                        (assoc exact-timing :process-wall-ms 1000.0)
                                        exact-calls)))))
  (testing "the exact arm-specific inspect result is evidence"
    (is (false?
          (:ok (screen/strict-lifecycle
                 :treatment exact-events exact-timing exact-calls))))
    (is (false?
          (:ok (screen/strict-lifecycle
                 :control
                 (assoc-in exact-events [1 :item :result :content 0 :text]
                           "NOT THE FROZEN RESULT")
                 exact-timing exact-calls)))))
  (testing "orphan message starts and capture phase drift refuse"
    (is (false?
          (:ok (screen/strict-lifecycle
                 :control
                 (into [(event "item.started"
                               {:id "early" :type "agent_message"})]
                       exact-events)
                 exact-timing exact-calls))))
    (is (false?
          (:ok (screen/strict-lifecycle
                 :control exact-events exact-timing
                 (assoc-in exact-calls [0 :phase] "next-action")))))))
