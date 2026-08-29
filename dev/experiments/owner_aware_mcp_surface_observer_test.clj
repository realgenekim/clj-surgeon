(ns owner-aware-mcp-surface-observer-test
  (:require
   [clj-surgeon.mcp-tool :as mcp-tool]
   [clojure.test :refer [deftest is testing]]
   [owner-aware-call-construction-screen :as screen]
   [owner-aware-mcp-surface-observer :as observer]))

(def advertised
  {:schema "clj-surgeon.owner-aware-call-surface.v1"
   :tool {:name "edit_clojure"
          :description "exact description"
          :input-schema {:type "object"
                         :anyOf [{:required ["edits"]}]
                         :properties {"edits" {:type "array"}}}
          :output-schema nil
          :annotations nil}})

(defn registry-receipt [tool]
  {:schema observer/registry-schema
   :ok true
   :server "clj-surgeon"
   :observation-source
   (assoc observer/registry-observation-source
          :server-selector {:field "name" :value "clj-surgeon"})
   :tool-names ["edit_clojure"]
   :tool-projection [tool]})

(def projected-tool
  {:name "edit_clojure"
   :description "exact description"
   :input-schema {:type "object"
                  :properties {"edits" {:type "array"}}}
   :output-schema nil
   :annotations {}})

(deftest admits-only-the-two-observed-codex-projection-normalizations
  (let [result (observer/validate-observation
                 advertised (registry-receipt projected-tool)
                 "clj-surgeon" "edit_clojure")]
    (is (:ok result))
    (is (= :codex-mcp-registry (:source result)))
    (is (= [:annotations-null-empty-object :drop-top-level-any-of]
           (:normalizations result))))
  (testing "an exact projection is also accepted"
    (is (:ok (observer/validate-observation
               advertised (registry-receipt (:tool advertised))
               "clj-surgeon" "edit_clojure"))))
  (testing "a different nested schema remains fatal"
    (let [changed (assoc-in projected-tool
                            [:input-schema :properties "edits" :type]
                            "object")]
      (is (= :client-input-schema-mismatch
             (:error-type
               (observer/validate-observation
                 advertised (registry-receipt changed)
                 "clj-surgeon" "edit_clojure")))))))

(deftest codex-apps-cache-cannot-masquerade-as-the-mcp-registry
  (let [app-cache
        {:schema_version 4
         :tools (mapv (fn [index]
                        {:server_name "codex_apps"
                         :tool_name (str "app-tool-" index)
                         :tool_namespace "codex_apps__fixture"})
                      (range 49))}
        result (observer/validate-observation
                 advertised app-cache "clj-surgeon" "edit_clojure")]
    (is (= 49 (count (:tools app-cache))))
    (is (false? (:ok result)))
    (is (= :registry-source-mismatch (:error-type result)))))

(deftest full-candidate-schema-survives-the-exact-observed-projection
  (let [surface (screen/tool-surface :candidate)
        advertised-receipt
        {:schema "clj-surgeon.owner-aware-call-surface.v1"
         :tool {:name (:name surface)
                :description (:description surface)
                :input-schema (:schema surface)
                :output-schema nil
                :annotations nil}}
        projected (-> (:tool advertised-receipt)
                      (assoc :annotations {})
                      (update :input-schema dissoc :anyOf))
        result (observer/validate-observation
                 advertised-receipt (registry-receipt projected)
                 "clj-surgeon" "edit_clojure")
        generic-description-result
        (observer/validate-observation
          advertised-receipt
          (registry-receipt
            (assoc projected :description mcp-tool/tool-description))
          "clj-surgeon" "edit_clojure")]
    (is (contains? (get-in advertised-receipt [:tool :input-schema]) :anyOf))
    (is (= observer/registry-observation-source
           (dissoc (:observation-source (registry-receipt projected))
                   :server-selector)))
    (is (:ok result))
    (is (= [:annotations-null-empty-object :drop-top-level-any-of]
           (:normalizations result)))
    (is (= :client-tool-surface-mismatch
           (:error-type generic-description-result)))))

(deftest every-other-surface-or-provenance-delta-fails-closed
  (doseq [[label receipt expected-error]
          [[:description
            (registry-receipt (assoc projected-tool :description "changed"))
            :client-tool-surface-mismatch]
           [:extra-tool
            (assoc (registry-receipt projected-tool)
                   :tool-names ["edit_clojure" "inspect_clojure"])
            :registry-tool-set-mismatch]
           [:wrong-method
            (assoc-in (registry-receipt projected-tool)
                      [:observation-source :method]
                      "codex_apps_tools")
            :registry-provenance-mismatch]]]
    (testing (name label)
      (is (= expected-error
             (:error-type
               (observer/validate-observation
                 advertised receipt "clj-surgeon" "edit_clojure")))))))

(defn -main [& _]
  (let [{:keys [fail error]}
        (clojure.test/run-tests 'owner-aware-mcp-surface-observer-test)]
    (when (pos? (+ fail error))
      (System/exit 1))))
