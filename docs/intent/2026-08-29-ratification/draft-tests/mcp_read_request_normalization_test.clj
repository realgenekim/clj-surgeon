(ns ratification.mcp-read-request-normalization-test
  "Red witness draft. This namespace is intentionally not attached to the
  ordinary runner until its HLD, LLD, and EARS phases are ratified."
  (:require
   [clj-surgeon.mcp-inspect :as inspect]
   [clj-surgeon.structural-lens :as structural-lens]
   [clojure.test :refer [deftest is testing]]))

(defn- forms-request
  [request]
  {"requests" [request]
   "expect" {"requests" 1 "files" 1}})

(deftest all-omitted-request-ids-are-assigned-in-input-order
  ;; @spec MCP-OP-READ-NORM-001
  (let [result
        (inspect/validate-inspect-params
          {"requests"
           [{"operation" "forms" "file" "src/a.clj"
             "forms" ["alpha"] "expect" {"forms" 1}}
            {"operation" "outline" "file" "src/b.clj"}]
           "expect" {"requests" 2 "files" 2}})]
    (is (:ok result))
    (when (:ok result)
      (is (= ["request-1" "request-2"]
             (mapv :id (get-in result [:params :requests])))))))

(deftest mixed-request-id-ownership-refuses-before-snapshot-capture
  ;; @spec MCP-OP-READ-NORM-002
  (let [result
        (inspect/validate-inspect-params
          {"requests"
           [{"id" "caller-id" "operation" "outline" "file" "src/a.clj"}
            {"operation" "outline" "file" "src/b.clj"}]
           "expect" {"requests" 2 "files" 2}})]
    (is (false? (:ok result)))
    (is (= :mixed-request-ids (:reason result)))
    (when (= :mixed-request-ids (:reason result))
      (is (= ["requests"] (:path result)))
      (is (:source_unchanged result))
      (is (false? (:read_started result)))
      (is (not (contains? result :continuation)))
      (is (not (contains? result :next_call))))))

(deftest generated-request-ids-survive-selector-continuation
  ;; @spec MCP-OP-READ-NORM-003
  (let [source "(ns example)\n(def alpha 1)\n"
        normalized
        (inspect/validate-inspect-params
          {"requests"
           [{"operation" "forms" "file" "src/a.clj"
             "forms" ["alpha"] "expect" {"forms" 1}}
            {"operation" "forms" "file" "src/a.clj"
             "forms" ["missing"] "expect" {"forms" 1}}]
           "expect" {"requests" 2 "files" 1}})]
    (is (:ok normalized))
    (when (:ok normalized)
      (let [result
            (inspect/evaluate-snapshots
              (:params normalized)
              {"src/a.clj"
               {:file "src/a.clj"
                :source source
                :hash (structural-lens/source-hash source)}})]
        (is (= "request-2"
               (get-in result [:selection_failures 0 :request_id])))
        (is (= ["request-1"]
               (get-in result [:continuation :completed_request_ids])))
        (is (= ["request-2"]
               (get-in result [:continuation :pending_request_ids])))
        (is (= "request-2"
               (get-in result
                       [:continuation :retry_template :holes 0 :request_id])))
        (is (= "request-2"
               (get-in result
                       [:continuation :retry_template :arguments
                        :requests 0 :id])))))))

(deftest complete-operation-less-forms-shape-normalizes-to-forms
  ;; @spec MCP-OP-READ-NORM-004
  (let [implicit
        (inspect/validate-inspect-params
          (forms-request
            {"id" "subject" "file" "src/a.clj"
             "forms" ["alpha"] "expect" {"forms" 1}}))
        explicit
        (inspect/validate-inspect-params
          (forms-request
            {"id" "subject" "operation" "forms" "file" "src/a.clj"
             "forms" ["alpha"] "expect" {"forms" 1}}))]
    (is (:ok implicit))
    (when (:ok implicit)
      (is (= explicit implicit))
      (is (= "forms" (get-in implicit [:params :requests 0 :operation]))))))

(deftest every-other-operation-less-shape-refuses
  ;; @spec MCP-OP-READ-NORM-005
  (doseq [[label request]
          [[:file-only
            {"id" "x" "file" "src/a.clj"}]
           [:match
            {"id" "x" "file" "src/a.clj" "match" "(def _ _)"}]
           [:xray
            {"id" "x" "file" "src/a.clj" "expression" "(form 'alpha)"}]
           [:forms-without-expect
            {"id" "x" "file" "src/a.clj" "forms" ["alpha"]}]
           [:expect-without-forms
            {"id" "x" "file" "src/a.clj" "expect" {"forms" 1}}]
           [:mixed-variant
            {"id" "x" "file" "src/a.clj" "forms" ["alpha"]
             "expect" {"forms" 1} "expression" "(form 'alpha)"}]]]
    (testing (name label)
      (let [result (inspect/validate-inspect-params (forms-request request))]
        (is (false? (:ok result)))
        (is (= :operation-required (:reason result)))
        (when (= :operation-required (:reason result))
          (is (= ["requests" 0 "operation"] (:path result)))
          (is (false? (:read_started result))))))))
