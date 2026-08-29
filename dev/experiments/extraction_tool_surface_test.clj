(ns extraction-tool-surface-test
  (:require
   [clj-surgeon.experiments.mcp-candidate-admission :as admission]
   [clojure.test :refer [deftest is testing]]
   [extraction-tool-surface :as surface]))

(def literal-request
  {:workspace_root "/tmp/example-workspace"
   :extraction
   {:file "src/cfp_scheduler_killer/views.clj"
    :to "src/cfp_scheduler_killer/views/format.clj"
    :forms ["date-fmt" "datetime-fmt" "->local-date" "fmt-date"
            "fmt-date-range" "fmt-instant" "->instant" "when-fmt"
            "relative-when" "fmt-when" "fmt-cfp-window" "iso-date-fmt"
            "fmt-close-date" "cfp-public-url" "not-blank"]
    :require_policy "minimal"
    :public_forms ["not-blank"]
    :caller_changes []
    :ignored_caller_files []}
   :verify "exact"})

(deftest treatment-only-removes-irrelevant-apply-branches
  (let [control (surface/tool-surface :control)
        treatment (surface/tool-surface :treatment)]
    (is (= (:id control) (:id treatment)))
    (is (= (:name control) (:name treatment)))
    (is (= (:output-schema control) (:output-schema treatment)))
    (is (= (:annotations control) (:annotations treatment)))
    (is (= #{"workspace_root" "extraction" "verify"}
           (set (keys (get-in treatment [:schema :properties])))))
    (is (= (get-in control [:schema :properties "extraction"])
           (get-in treatment [:schema :properties "extraction"])))
    (is (= (get-in control [:schema :properties "workspace_root"])
           (get-in treatment [:schema :properties "workspace_root"])))
    (is (= (get-in control [:schema :properties "verify"])
           (get-in treatment [:schema :properties "verify"])))
    (is (= ["extraction"] (get-in treatment [:schema :required])))
    (is (false? (get-in treatment [:schema :additionalProperties])))))

(deftest treatment-admission-is-exact-and-fail-closed
  (let [schema (:schema (surface/tool-surface :treatment))]
    (is (:ok (admission/authorize schema literal-request)))
    (testing "irrelevant generic branches remain unavailable"
      (doseq [field [:basis :changes :edits :programs :delete_owners]]
        (let [result (admission/authorize schema (assoc literal-request field []))]
          (is (false? (:ok result)))
          (is (= :public-schema-denied (:error-type result)))
          (is (= [(name field)] (:unexpected-fields result))))))
    (testing "the extraction decision remains mandatory"
      (is (false? (:ok (admission/authorize schema
                                            (dissoc literal-request :extraction))))))))

(deftest treatment-removes-most-of-the-visible-surface
  (let [{:keys [control-bytes treatment-bytes removed-fraction]}
        (surface/surface-report)]
    (is (= 23721 control-bytes))
    (is (= 8123 treatment-bytes))
    (is (> removed-fraction 0.64))))
