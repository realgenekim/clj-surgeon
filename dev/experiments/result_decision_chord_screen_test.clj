(ns result-decision-chord-screen-test
  (:require
   [cheshire.core :as json]
   [clojure.java.io :as io]
   [clojure.test :refer [deftest is testing]]
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

(defn- isolation [suffix]
  {:workspace (str "/tmp/workspace-" suffix)
   :codex-home (str "/tmp/codex-home-" suffix)
   :server-pid suffix
   :capture (str "/tmp/capture-" suffix ".json")
   :events (str "/tmp/events-" suffix ".jsonl")
   :capture-sha256 (format "%064x" suffix)
   :events-sha256 (format "%064x" (+ 10 suffix))})

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
                           :to "\"Hi, \""}]}))))))

(deftest capture-requires-exact-two-call-route
  (let [inspect {:tool_name "inspect_clojure"
                 :params screen/expected-inspect-request}
        edit {:tool_name "edit_clojure" :params exact-edit}]
    (is (:correct
          (screen/score-capture
            {:arm :control :position 1
             :capture (capture-file [inspect edit])})))
    (doseq [calls [[edit]
                   [inspect]
                   [inspect edit edit]
                   [inspect {:tool_name "apply_clojure_changes" :params {}}]]]
      (is (false?
            (:correct
              (screen/score-capture
                {:arm :control :position 1
                 :capture (capture-file calls)})))))))

(deftest cohort-gates-order-correctness-both-pairs-and-twenty-percent
  (let [passing [{:position 1 :arm :control :correct true
                  :inspect-to-edit-ms 100.0 :logical-edit-argument-bytes 200
                  :isolation (isolation 1)}
                 {:position 2 :arm :treatment :correct true
                  :inspect-to-edit-ms 70.0 :logical-edit-argument-bytes 190
                  :isolation (isolation 2)}
                 {:position 3 :arm :treatment :correct true
                  :inspect-to-edit-ms 75.0 :logical-edit-argument-bytes 190
                  :isolation (isolation 3)}
                 {:position 4 :arm :control :correct true
                  :inspect-to-edit-ms 110.0 :logical-edit-argument-bytes 200
                  :isolation (isolation 4)}]]
    (is (:ok (screen/cohort-report passing)))
    (is (false? (:ok (screen/cohort-report (assoc-in passing [2 :correct] false)))))
    (is (false? (:ok (screen/cohort-report
                       (assoc-in passing [2 :inspect-to-edit-ms] 120.0)))))
    (is (false? (:ok (screen/cohort-report
                       (mapv #(update % :inspect-to-edit-ms
                                      (fn [value]
                                        (if (= :treatment (:arm %))
                                          (+ value 18.0)
                                          value)))
                             passing)))))
    (is (false? (:ok (screen/cohort-report (vec (reverse passing))))))
    (is (false? (:ok (screen/cohort-report
                       (assoc-in passing [2 :isolation] (isolation 2))))))
    (is (false? (:ok (screen/cohort-report
                       (assoc-in passing [2 :logical-edit-argument-bytes] 250)))))))

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
           :tool "inspect_clojure"})
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
  [{:tool_name "inspect_clojure" :params inspect-arguments}
   {:tool_name "edit_clojure" :params exact-edit}])

(deftest lifecycle-joins-the-exact-two-calls-and-direct-clock
  (let [result (screen/strict-lifecycle exact-events exact-timing exact-calls)]
    (is (:ok result))
    (is (= 1500.0 (:inspect-to-edit-ms result))))
  (testing "a preamble is observable and refuses"
    (is (false?
          (:ok (screen/strict-lifecycle
                 (into [(event "item.completed"
                               {:id "early" :type "agent_message" :text "Working."})]
                       exact-events)
                 exact-timing exact-calls)))))
  (testing "capture and event arguments must agree"
    (is (false?
          (:ok (screen/strict-lifecycle
                 exact-events exact-timing
                 (assoc-in exact-calls [1 :params :edits 0 :to] "\"Hi, \""))))))
  (testing "completion must follow its matching start"
    (is (false?
          (:ok (screen/strict-lifecycle
                 [(nth exact-events 1) (nth exact-events 0)
                  (nth exact-events 2) (nth exact-events 3) (nth exact-events 4)]
                 exact-timing exact-calls)))))
  (testing "completed-only file changes refuse"
    (is (false?
          (:ok (screen/strict-lifecycle
                 (conj exact-events
                       (event "item.completed"
                              {:id "file-1" :type "file_change"}))
                 exact-timing exact-calls)))))
  (testing "a boundary larger than process wall refuses"
    (is (false?
          (:ok (screen/strict-lifecycle
                 exact-events
                 (assoc exact-timing :process-wall-ms 1000.0)
                 exact-calls))))))
