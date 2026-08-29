(ns clj-surgeon.mcp-compact-location-test
  (:require
   [clj-surgeon.mcp-contract :as contract]
   [clj-surgeon.mcp-tool :as mcp-tool]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.test :refer [deftest is testing]])
  (:import
   (java.nio.file Files)
   (java.nio.file.attribute FileAttribute)))

(defn- temp-dir
  []
  (.toFile
    (Files/createTempDirectory
      "clj-surgeon-compact-location-test-"
      (make-array FileAttribute 0))))

(defn- delete-tree!
  [file]
  (when (.exists (io/file file))
    (doseq [child (reverse (file-seq (io/file file)))]
      (Files/deleteIfExists (.toPath child)))))

(defn- write-source!
  [workspace source]
  (let [source-file (io/file workspace "src/sample/app.clj")]
    (.mkdirs (.getParentFile source-file))
    (spit source-file source)
    source-file))

(deftest source-blind-validation-preserves-omitted-location
  ;; @spec MCP-OP-EDIT-011
  (let [request
        {"edits" [{"file" "src/sample/app.clj"
                   "from" "(defn f [] 1)"
                   "to" "(defn f [] 2)"}]}
        validated (contract/validate-tool-params request)
        transaction (when (:ok validated)
                      (contract/tool-params->transaction (:params validated)))
        change (get-in transaction [:changes 0])]
    (is (:ok validated) (pr-str validated))
    (is (:compact-location-normalization validated))
    (is (not (contains? change :forms)))
    (is (not (contains? change :owner)))
    (is (= ["src/sample/app.clj"] (:in change))))
  (let [result
        (contract/validate-tool-params
          {"edits" [{"file" "bench/config.edn"
                     "from" ":old"
                     "to" ":new"}]})]
    (is (false? (:ok result)))
    (is (= :invalid-edn-editor-scope (:error-type result)))
    (is (= ["edits" 0 "within"] (:path result)))))

(deftest compact-location-normalization-is-injective
  ;; @spec MCP-OP-EDIT-012
  ;; @spec MCP-OP-EDIT-013
  ;; @spec MCP-OP-EDIT-014
  ;; @spec MCP-OP-EDIT-015
  (let [before (str "(ns sample.app\n"
                    "  (:require [old.core :as old]))\n\n"
                    "(defn f [] 1)\n")
        cases
        [{:label "namespace-name-in-form"
          :edit {"file" "src/sample/app.clj"
                 "within" {"form" "sample.app"}
                 "from" "(:require [old.core :as old])"
                 "to" "(:require [new.core :as new])"}
          :expected (str/replace before "old.core :as old" "new.core :as new")}
         {:label "namespace-clause"
          :edit {"file" "src/sample/app.clj"
                 "from" "(:require [old.core :as old])"
                 "to" "(:require [new.core :as new])"}
          :expected (str/replace before "old.core :as old" "new.core :as new")}
         {:label "complete-named-owner"
          :edit {"file" "src/sample/app.clj"
                 "from" "(defn f [] 1)"
                 "to" "(defn f [] 2)"}
          :expected (str/replace before "(defn f [] 1)" "(defn f [] 2)")}]]
    (doseq [{:keys [label edit expected]} cases]
      (testing label
        (let [workspace (temp-dir)
              receipt-dir (io/file workspace "receipts")
              source-file (write-source! workspace before)]
          (try
            (let [result
                  (mcp-tool/execute-request!
                    {:project-root (.getPath workspace)
                     :receipt-dir (.getPath receipt-dir)}
                    {"edits" [edit]})]
              (is (:ok result) (pr-str result))
              (is (:verification_complete result))
              (is (= expected (slurp source-file)))
              (is (= 1 (get-in result [:location_normalization :count])))
              (is (= label
                     (get-in result
                             [:location_normalization :edits 0 :relation])))
              (is (nil? (get-in result
                                [:location_normalization :edits 0 :source]))))
            (finally
              (delete-tree! workspace))))))))

(deftest one-unproved-location-refuses-the-complete-batch
  ;; @spec MCP-OP-EDIT-015
  (let [workspace (temp-dir)
        receipt-dir (io/file workspace "receipts")
        before (str "(ns sample.app)\n\n"
                    "(defn f [] 1)\n")
        source-file (write-source! workspace before)
        request
        {"edits"
         [{"file" "src/sample/app.clj"
           "from" "(defn f [] 1)"
           "to" "(defn f [] 2)"}
          {"file" "src/sample/app.clj"
           "from" "(defn missing [] 1)"
           "to" "(defn missing [] 2)"}]}]
    (try
      (let [result
            (mcp-tool/execute-request!
              {:project-root (.getPath workspace)
               :receipt-dir (.getPath receipt-dir)}
              request)]
        (is (false? (:ok result)))
        (is (= "compact-location-unresolved" (:error_type result)))
        (is (:source_unchanged result))
        (is (= false (:mutation_attempted result)))
        (is (= before (slurp source-file)))
        (is (not (.exists receipt-dir))))
      (finally
        (delete-tree! workspace)))))
