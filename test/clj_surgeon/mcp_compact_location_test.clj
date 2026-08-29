(ns clj-surgeon.mcp-compact-location-test
  (:require
   [clj-surgeon.core :as core]
   [clj-surgeon.intent-transaction :as transaction]
   [clj-surgeon.mcp-change-buffer :as change-buffer]
   [clj-surgeon.mcp-compact-location :as compact-location]
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
  ([workspace source]
   (write-source! workspace "src/sample/app.clj" source))
  ([workspace file source]
   (let [source-file (io/file workspace file)]
     (.mkdirs (.getParentFile source-file))
     (spit source-file source)
     source-file)))

(defn- normalize-request
  ([source request]
   (normalize-request "src/sample/app.clj" source request))
  ([file source request]
   (let [validated (contract/validate-tool-params request)]
     (if-not (:ok validated)
       validated
       (let [spec (contract/tool-params->transaction (:params validated))]
         (let [sources {file source}
               prepared
               (compact-location/normalize-spec
                 sources spec
                 (:compact-location-normalization validated))]
           (if (:error prepared)
             prepared
             (let [compiled
                   (transaction/compile-transaction sources (:spec prepared))]
               (if (:error compiled)
                 compiled
                 (assoc prepared :compiled compiled))))))))))

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
    (is (= :invalid-edn-editor-scope (:reason result)))
    (is (= ["edits" 0 "within"] (:path result)))))

(deftest compact-location-normalization-is-injective
  ;; @spec MCP-OP-EDIT-016
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

(deftest production-normalizer-refuses-the-complete-falsifier-matrix
  ;; @spec MCP-OP-EDIT-012
  ;; @spec MCP-OP-EDIT-013
  ;; @spec MCP-OP-EDIT-014
  ;; @spec MCP-OP-EDIT-016
  (let [base-source (str "(ns sample.app\n"
                         "  (:require [old.core :as old]))\n\n"
                         "(defn f [] 1)\n")
        namespace-edit
        {"file" "src/sample/app.clj"
         "from" "(:require [old.core :as old])"
         "to" "(:require [new.core :as new])"}
        owner-edit
        {"file" "src/sample/app.clj"
         "from" "(defn f [] 1)"
         "to" "(defn f [] 2)"}
        cases
        [{:label :wrong-namespace-name
          :source base-source
          :expected-error :change-owner-mismatch
          :edit (assoc namespace-edit "within" {"form" "sample.other"})}
         {:label :zero-namespace
          :source "(defn f [] 1)\n"
          :expected-error :change-owner-mismatch
          :edit (assoc namespace-edit "within" {"form" "sample.app"})}
         {:label :several-namespaces
          :source (str base-source "(ns sample.other)\n")
          :expected-error :change-owner-mismatch
          :edit (assoc namespace-edit "within" {"form" "sample.app"})}
         {:label :reader-conditional-namespace
          :source "#?(:clj (ns sample.app (:require [old.core :as old])))\n"
          :expected-error :change-owner-mismatch
          :edit (assoc namespace-edit "within" {"form" "sample.app"})}
         {:label :stale-clause-count
          :source base-source
          :edit (assoc namespace-edit "matches" 2)}
         {:label :mismatched-clause-kind
          :source base-source
          :edit (assoc namespace-edit "to" "(:import java.time.Instant)")}
         {:label :nested-clause-lookalike
          :source "(ns sample.app {:probe (:require [old.core :as old])})\n"
          :edit namespace-edit}
         {:label :external-clause-lookalike
          :source (str base-source
                       "(def probe '(:require [old.core :as old]))\n")
          :edit namespace-edit}
         {:label :detached-clause-comment
          :source base-source
          :edit (assoc namespace-edit
                       "from" ";; keep this\n(:require [old.core :as old])")}
         {:label :anonymous-owner
          :source base-source
          :edit (assoc owner-edit "from" "(+ 1 2)" "to" "(+ 2 3)")}
         {:label :renamed-owner
          :source base-source
          :edit (assoc owner-edit "to" "(defn g [] 2)")}
         {:label :retyped-owner
          :source base-source
          :edit (assoc owner-edit "to" "(def f 2)")}
         {:label :duplicate-owner
          :source (str base-source "(defn f [] 1)\n")
          :edit owner-edit}
         {:label :nested-owner-lookalike
          :source "(ns sample.app)\n(def probe '(defn f [] 1))\n"
          :edit owner-edit}
         {:label :reader-conditional-owner
          :source "#?(:clj (defn f [] 1))\n"
          :edit owner-edit}
         {:label :stale-owner-body
          :source (str/replace base-source "(defn f [] 1)" "(defn f [] 9)")
          :edit owner-edit}]]
    (doseq [{:keys [label source edit expected-error]} cases]
      (testing (name label)
        (let [result (normalize-request source {"edits" [edit]})]
          (is (not (:ok result)) (pr-str result))
          (is (= (or expected-error :compact-location-unresolved)
                 (:error-type result))
              (pr-str result))))))
  (testing "malformed file selectors refuse in the source-blind contract"
    (let [result
          (contract/validate-tool-params
            {"edits" [{"file" "src/sample/app.clj"
                       "files" ["src/sample/app.clj"]
                       "from" "(defn f [] 1)"
                       "to" "(defn f [] 2)"}]})]
      (is (false? (:ok result)))
      (is (= :ambiguous-editor-files (:reason result)))))
  (testing "null and scalar within values refuse as malformed locations"
    (doseq [within [nil false]]
      (let [result
            (contract/validate-tool-params
              {"edits" [{"file" "src/sample/app.clj"
                         "within" within
                         "from" "(defn f [] 1)"
                         "to" "(defn f [] 2)"}]})]
        (is (false? (:ok result)) (pr-str result))
        (is (= :expected-object (:reason result)) (pr-str result)))))
  (testing "an exact named owner remains authoritative instead of falling back"
    (let [source (str "(ns sample.app\n"
                      "  (:require [old.core :as old]))\n\n"
                      "(def sample.app :named-owner)\n")
          result
          (normalize-request
            source
            {"edits" [{"file" "src/sample/app.clj"
                       "within" {"form" "sample.app"}
                       "from" ":named-owner"
                       "to" ":changed"}]})]
      (is (:ok result) (pr-str result))
      (is (= ['sample.app] (get-in result [:spec :changes 0 :forms])))
      (is (nil? (:location-normalization result))))))

(deftest generic-direct-root-scope-does-not-enter-compact-normalization
  ;; @spec MCP-OP-EDIT-015
  (let [workspace (temp-dir)
        receipt-dir (io/file workspace "receipts")
        before "(ns sample.app)\n(def settings {:state :old})\n"
        source-file (write-source! workspace before)
        request
        {"changes"
         [{"id" "root-edit"
           "files" ["src/sample/app.clj"]
           "find" ":old"
           "replace" ":new"
           "expect" {"matches" 1 "each_file" 1}}]
         "expect" {"changes" 1 "edits" 1 "files" 1}}]
    (try
      (let [result
            (mcp-tool/execute-request!
              {:project-root (.getPath workspace)
               :receipt-dir (.getPath receipt-dir)}
              request)]
        (is (:ok result) (pr-str result))
        (is (:verification_complete result))
        (is (nil? (:location_normalization result)))
        (is (= (str/replace before ":old" ":new")
               (slurp source-file))))
      (finally
        (delete-tree! workspace)))))

(deftest noncompact-routes-never-invoke-the-compact-normalizer
  ;; @spec MCP-OP-EDIT-015
  (let [normalizer-calls (atom [])
        normalizer-spy
        (fn [& args]
          (swap! normalizer-calls conj args)
          (throw (ex-info "compact normalizer must not run" {})))]
    (with-redefs [compact-location/normalize-spec normalizer-spy]
      (testing "generic direct changes"
        (let [workspace (temp-dir)
              receipt-dir (io/file workspace "receipts")
              source-file
              (write-source! workspace
                             "(ns sample.app)\n(def settings {:state :old})\n")]
          (try
            (let [result
                  (mcp-tool/execute-request!
                    {:project-root (.getPath workspace)
                     :receipt-dir (.getPath receipt-dir)}
                    {"changes"
                     [{"id" "root-edit"
                       "files" ["src/sample/app.clj"]
                       "find" ":old"
                       "replace" ":new"
                       "expect" {"matches" 1 "each_file" 1}}]
                     "expect" {"changes" 1 "edits" 1 "files" 1}})]
              (is (:ok result) (pr-str result))
              (is (= "(ns sample.app)\n(def settings {:state :new})\n"
                     (slurp source-file))))
            (finally
              (delete-tree! workspace)))))

      (testing "retained basis"
        (let [workspace (temp-dir)
              receipt-dir (io/file workspace "receipts")
              source-file (write-source! workspace
                                         "(ns sample.app)\n(defn f [] 1)\n")]
          (try
            (change-buffer/clear-bases!)
            (let [prepared
                  (change-buffer/prepare-change!
                    {:project-root (.getPath workspace)}
                    {:file "src/sample/app.clj"
                     :form "f"
                     :intent "Change the exact retained owner"})
                  site (get-in prepared [:decision-sites 0 :id])
                  result
                  (mcp-tool/execute-request!
                    {:project-root (.getPath workspace)
                     :receipt-dir (.getPath receipt-dir)
                     :verify! (fn [_ _ _ files]
                                {:ok true :files files})}
                    {"basis" (:basis prepared)
                     "decisions" [{"site" site
                                   "replace" "(defn f [] 2)"}]})]
              (is (:ok prepared) (pr-str prepared))
              (is (:ok result) (pr-str result))
              (is (= "(ns sample.app)\n(defn f [] 2)\n"
                     (slurp source-file))))
            (finally
              (change-buffer/clear-bases!)
              (delete-tree! workspace)))))

      (testing "extraction"
        (let [workspace (temp-dir)
              receipt-dir (io/file workspace "receipts")
              source-file
              (write-source! workspace
                             (str "(ns sample.app)\n\n"
                                  "(defn f [] 1)\n\n"
                                  "(defn retained [] :ok)\n"))]
          (try
            (let [result
                  (mcp-tool/execute-request!
                    {:project-root (.getPath workspace)
                     :receipt-dir (.getPath receipt-dir)}
                    {"extraction"
                     {"file" "src/sample/app.clj"
                      "to" "src/sample/moved.clj"
                      "forms" ["f"]
                      "require_policy" "minimal"}})]
              (is (:ok result) (pr-str result))
              (is (str/includes? (slurp source-file) "(defn retained [] :ok)"))
              (is (.exists (io/file workspace "src/sample/moved.clj"))))
            (finally
              (delete-tree! workspace)))))

      (testing "public edit programs"
        (let [workspace (temp-dir)
              receipt-dir (io/file workspace "receipts")
              source-file
              (write-source! workspace "(ns sample.app)\n(defn f [] :old)\n")]
          (try
            (let [result
                  (mcp-tool/execute-request!
                    {:project-root (.getPath workspace)
                     :receipt-dir (.getPath receipt-dir)}
                    {"programs"
                     [{"file" "src/sample/app.clj"
                       "expression"
                       "(-> (form 'f) (match :old) (transform (constantly :new)))"
                       "expect" {"matches" 1
                                 "max_changed_characters" 8}}]})]
              (is (false? (:ok result)) (pr-str result))
              (is (= "invalid-mcp-request" (:error_type result))
                  (pr-str result))
              (is (= ["changes"] (:path result)) (pr-str result))
              (is (:source_unchanged result) (pr-str result))
              (is (= "(ns sample.app)\n(defn f [] :old)\n"
                     (slurp source-file))))
            (finally
              (delete-tree! workspace)))))

      (testing "CLI change preview"
        (let [workspace (temp-dir)
              source-file
              (write-source! workspace "(ns sample.app)\n(defn f [] :old)\n")
              result (atom nil)]
          (try
            (with-out-str
              (reset! result
                      (core/run
                        {:op :change
                         :spec
                         {:intents [{:files [(.getPath source-file)]
                                     :from ":old"
                                     :to ":new"
                                     :expect-count 1}]
                          :expect {:intent-count 1
                                   :edit-count 1
                                   :changed-file-count 1}}})))
            (is (:ok @result) (pr-str @result))
            (is (= "(ns sample.app)\n(defn f [] :old)\n"
                   (slurp source-file)))
            (finally
              (delete-tree! workspace))))))

    (is (empty? @normalizer-calls)
        "generic-change, retained-basis, extraction, public-program, and CLI routes bypass compact normalization")))

(deftest cljc-platform-conditional-owner-does-not-authorize-a-location
  ;; @spec MCP-OP-EDIT-014
  (let [file "src/sample/app.cljc"
        source (str "(ns sample.app)\n"
                    "#?(:clj (defn f [] :old)\n"
                    "   :cljs (defn f [] :old))\n")
        result
        (normalize-request
          file source
          {"edits" [{"file" file
                     "from" "(defn f [] :old)"
                     "to" "(defn f [] :new)"}]})]
    (is (not (:ok result)) (pr-str result))
    (is (= :compact-location-unresolved (:error-type result))
        (pr-str result))))

(deftest cljc-platform-conditional-namespace-clause-refuses-before-write
  ;; @spec MCP-OP-EDIT-013
  (let [workspace (temp-dir)
        receipt-dir (io/file workspace "receipts")
        source (str "(ns sample.app\n"
                    "  #?(:clj (:require [old.core :as old])\n"
                    "     :cljs (:require [old.core :as old])))\n"
                    "(defn f [] 1)\n")
        file "src/sample/app.cljc"
        source-file (write-source! workspace file source)]
    (try
      (let [result
            (mcp-tool/execute-request!
              {:project-root (.getPath workspace)
               :receipt-dir (.getPath receipt-dir)}
              {"edits"
               [{"file" file
                 "from" "(:require [old.core :as old])"
                 "to" "(:require [new.core :as new])"}]})]
        (is (false? (:ok result)) (pr-str result))
        (is (= "compact-location-unresolved" (:error_type result))
            (pr-str result))
        (is (:source_unchanged result) (pr-str result))
        (is (= source (slurp source-file))))
      (finally
        (delete-tree! workspace)))))

(deftest duplicate-namespace-named-owners-block-namespace-fallback
  ;; @spec MCP-OP-EDIT-012
  (let [source (str "(ns sample.app\n"
                    "  (:require [old.core :as old]))\n\n"
                    "(def sample.app :first)\n"
                    "(def sample.app :second)\n")
        result
        (normalize-request
          source
          {"edits" [{"file" "src/sample/app.clj"
                     "within" {"form" "sample.app"}
                     "from" "(:require [old.core :as old])"
                     "to" "(:require [new.core :as new])"}]})]
    (is (not (:ok result)) (pr-str result))
    (is (= :change-owner-mismatch (:error-type result))
        (pr-str result))))
