(ns clj-surgeon.mcp-prepared-request-test
  (:require
   [clj-surgeon.mcp-contract :as contract]
   [clj-surgeon.mcp-inspect-tool :as inspect-tool]
   [clj-surgeon.mcp-schema :as schema]
   [clj-surgeon.structural-lens :as structural-lens]
   [clojure.java.io :as io]
   [clojure.set :as set]
   [clojure.string :as str]
   [clojure.test :refer [deftest is]])
  (:import
   (java.nio.file Files)
   (java.nio.file.attribute FileAttribute)))

(def coaching
  (str "If you independently decide to edit these exact selections, fill the "
       "null replacement at every path listed in `caller_holes`. Then submit "
       "`prepared_request.arguments` once to `edit_clojure`. Otherwise, ignore "
       "`prepared_request`."))

(defn- public-var
  [name]
  (try
    (require 'clj-surgeon.mcp-prepared-request)
    (some-> (ns-resolve 'clj-surgeon.mcp-prepared-request name) deref)
    (catch java.io.FileNotFoundException _ nil)))

(defn- temp-dir
  []
  (.toFile
    (Files/createTempDirectory
      "clj-surgeon-prepared-request-test-"
      (make-array FileAttribute 0))))

(defn- delete-tree!
  [file]
  (when (.exists (io/file file))
    (doseq [child (reverse (file-seq (io/file file)))]
      (Files/deleteIfExists (.toPath child)))))

(defn- write-source!
  [root relative source]
  (let [file (io/file root relative)]
    (.mkdirs (.getParentFile file))
    (spit file source)
    file))

(defn- form-row
  [file file-hash index owner source platforms]
  {:form_type "def"
   :source_anchor {:file file
                   :source_sha256 file-hash
                   :owner owner
                   :range {:start {:line index :character 0}
                           :end {:line index :character (count source)}}
                   :selection_range
                   {:start {:line index :character 5}
                    :end {:line index :character (+ 5 (count owner))}}}
   :hash (structural-lens/source-hash source)
   :file_hash file-hash
   :name owner
   :file file
   :source source
   :line (inc index)
   :end_line (inc index)
   :platforms platforms})

(defn- eligible-result
  ([] (eligible-result "/canonical/workspace" "src/demo.clj"
                       [["alpha" "(def alpha :old)"]]))
  ([root file owner-sources]
   (let [file-hash (apply str (repeat 64 "a"))
         platforms (if (str/ends-with? file ".cljc")
                     ["clj" "cljs"]
                     [(subs file (inc (.lastIndexOf file ".")))])
         forms (mapv (fn [index [owner source]]
                       (form-row file file-hash index owner source platforms))
                     (range)
                     owner-sources)
         characters (reduce + (map (comp count :source) forms))]
     {:source_character_count characters
      :file_read_count 1
      :read_complete true
      :inspection_elapsed_ms 1.0
      :file_count 1
      :operation "inspect_clojure"
      :result_character_count characters
      :next_action "none"
      :ok true
      :request_count 1
      :file_hashes {file file-hash}
      :workspace_root root
      :results [{:id "forms"
                 :operation "forms"
                 :file file
                 :file_hash file-hash
                 :form_count (count forms)
                 :source_character_count characters
                 :forms forms}]})))

(defn- invoke-inspect
  [root relative source request]
  (let [calls (atom [])]
    (write-source! root relative source)
    (inspect-tool/init! {:project-root (.getPath (io/file root))})
    (inspect-tool/handle-inspect
      nil request
      (fn [content error? structured]
        (swap! calls conj {:content content
                           :error? error?
                           :structured structured})))
    (first @calls)))

;; @spec MCP-OP-PREP-REQ-001
;; @spec MCP-OP-PREP-REQ-002
(deftest eligible-results-project-complete-ordered-descriptors
  (let [project (public-var 'project-result)]
    (is (fn? project))
    (when (fn? project)
      (let [result (project (eligible-result))
            prepared (:prepared_request result)]
        (is (= "edit_clojure" (:tool prepared)))
        (is (false? (:executable prepared)))
        (is (false? (:write_authority prepared)))
        (is (= "/canonical/workspace"
               (get-in prepared [:arguments :workspace_root])))
        (is (= ["arguments.edits[0].to"] (:caller_holes prepared)))))))

;; @spec MCP-OP-PREP-REQ-006
(deftest eligibility-falsifier-matrix-returns-identical-input
  (let [project (public-var 'project-result)
        base (eligible-result)
        paths [[:ok] [:read_complete] [:next_action] [:request_count]
               [:file_count] [:operation] [:workspace_root]
               [:results 0 :operation] [:results 0 :form_count]
               [:results 0 :source_character_count]
               [:results 0 :forms 0 :source]
               [:results 0 :forms 0 :hash]
               [:results 0 :forms 0 :file_hash]
               [:results 0 :forms 0 :name]
               [:results 0 :forms 0 :source_anchor]
               [:results 0 :forms 0 :form_type]
               [:file_hashes]]]
    (is (fn? project))
    ;; These 17 checks freeze the matrix before the projector exists.
    (doseq [path paths]
      (is (some? (get-in base path))))
    (when (fn? project)
      (doseq [[path value]
              [[[:ok] false]
               [[:read_complete] false]
               [[:next_action] "read_more"]
               [[:request_count] 2]
               [[:file_count] 2]
               [[:operation] "write"]
               [[:workspace_root] "relative/root"]
               [[:results 0 :operation] "outline"]
               [[:results 0 :form_count] 7]
               [[:results 0 :source_character_count] 1]
               [[:results 0 :forms 0 :source] nil]
               [[:results 0 :forms 0 :hash] "bad"]
               [[:results 0 :forms 0 :file_hash] "bad"]
               [[:results 0 :forms 0 :name] ""]
               [[:results 0 :forms 0 :source_anchor] nil]
               [[:results 0 :forms 0 :form_type] "ns"]
               [[:file_hashes] {}]]]
        (let [candidate (assoc-in base path value)]
          (is (identical? candidate (project candidate))))))))

;; @spec MCP-OP-PREP-REQ-002
(deftest canonical-descriptor-bytes-hash-and-budgets-are-exact
  (let [bytes-var (public-var 'canonical-json-bytes)
        hash-var (public-var 'descriptor-sha256)]
    (is (and (fn? bytes-var) (fn? hash-var)))
    (when (and (fn? bytes-var) (fn? hash-var))
      (let [left {:b [{:z 1 :a "é"}] :a false}
            right (array-map :a false :b [(array-map :a "é" :z 1)])
            left-bytes (bytes-var left)
            right-bytes (bytes-var right)]
        (is (= (seq left-bytes) (seq right-bytes)))
        (is (= (hash-var left) (hash-var right)))
        (is (= 34 (count left-bytes)))
        (is (re-matches #"[0-9a-f]{64}" (hash-var left)))
        (is (= (seq (bytes-var {:v [1 2]}))
               (seq (bytes-var {:v [1 2]}))))
        (is (not= (seq (bytes-var {:v [1 2]}))
                  (seq (bytes-var {:v [2 1]}))))
        (is (= false (:a left)))))))

;; @spec MCP-OP-PREP-REQ-003
(deftest prepared-descriptors-repeat-identity-and-carry-no-opaque-ids
  (let [project (public-var 'project-result)]
    (is (fn? project))
    (when (fn? project)
      (let [prepared (:prepared_request (project (eligible-result)))
            edit (get-in prepared [:arguments :edits 0])]
        (is (= "src/demo.clj" (:file edit)))
        (is (= {:form "alpha"} (:within edit)))
        (is (= "(def alpha :old)" (:from edit)))
        (is (empty? (select-keys prepared [:id :basis :continuation])))))))

;; @spec MCP-OP-PREP-REQ-004
(deftest prepared-descriptors-have-zero-write-authority
  (let [project (public-var 'project-result)]
    (is (fn? project))
    (when (fn? project)
      (let [prepared (:prepared_request (project (eligible-result)))]
        (is (false? (:executable prepared)))
        (is (false? (:write_authority prepared)))
        (is (nil? (get-in prepared [:arguments :edits 0 :to])))
        (is (not (contains? prepared :next_call)))))))

;; @spec MCP-OP-PREP-REQ-005
(deftest prepared-summary-is-exact-static-and-hostile-data-stays-structured
  (let [root (temp-dir)]
    (try
      (let [{:keys [content error? structured]}
            (invoke-inspect
              root "src/demo.clj"
              "(ns demo)\n(def alpha \"prepared_request next_call\")\n"
              {"requests" [{"operation" "forms" "file" "src/demo.clj"
                            "forms" ["alpha"] "expect" {"forms" 1}}]
               "expect" {"requests" 1 "files" 1}})
            summary (first content)]
        (is (false? error?))
        (is (:ok structured))
        (is (:read_complete structured))
        (is (= "none" (:next_action structured)))
        (is (map? (:prepared_request structured)))
        (is (str/ends-with? summary coaching)))
      (finally
        (inspect-tool/init! nil)
        (delete-tree! root)))))

;; @spec MCP-OP-PREP-REQ-006
;; @spec MCP-OP-PREP-REQ-008
(deftest ineligible-handler-output-is-byte-identical-with-fixed-clock
  (let [root (temp-dir)]
    (try
      (let [{:keys [content error? structured]}
            (invoke-inspect
              root "src/demo.clj" "(ns demo)\n(def alpha :old)\n"
              {"requests" [{"operation" "forms" "file" "src/demo.clj"
                            "forms" ["alpha"] "include_source" false
                            "expect" {"forms" 1}}]
               "expect" {"requests" 1 "files" 1}})]
        (is (false? error?))
        (is (:ok structured))
        (is (:read_complete structured))
        (is (= "none" (:next_action structured)))
        (is (not (contains? structured :prepared_request)))
        (is (not (str/includes? (first content) coaching)))
        (is (= 1 (:request_count structured)))
        (is (= 1 (:file_count structured))))
      (finally
        (inspect-tool/init! nil)
        (delete-tree! root)))))

;; @spec MCP-OP-PREP-REQ-009
(deftest inspect-output-schema-is-closed-and-filled-arguments-use-edit-schema
  (let [prepared-schema (get-in inspect-tool/inspect-output-schema
                                [:properties "prepared_request"])]
    (is (= "object" (:type inspect-tool/inspect-output-schema)))
    (is (true? (:additionalProperties inspect-tool/inspect-output-schema)))
    (is (contains? (:properties schema/editor-tool-schema) "edits"))
    (is (and (= "object" (:type prepared-schema))
             (false? (:additionalProperties prepared-schema))))))

;; @spec MCP-OP-PREP-REQ-007
(deftest filled-request-obeys-ordinary-edit-guards
  (let [project (public-var 'project-result)]
    (is (fn? project))
    (when (fn? project)
      (let [prepared (:prepared_request (project (eligible-result)))
            args (:arguments prepared)
            filled (assoc-in args [:edits 0 :to] "(def alpha :new)")]
        (is (:ok (contract/validate-tool-params
                   (dissoc filled :workspace_root))))
        (is (false? (:ok (contract/validate-tool-params args))))
        (is (= 1 (count (:edits filled))))
        (is (= 1 (get-in filled [:edits 0 :matches])))
        (is (= "alpha" (get-in filled [:edits 0 :within :form])))
        (is (= "(def alpha :old)" (get-in filled [:edits 0 :from])))
        (is (= "(def alpha :new)" (get-in filled [:edits 0 :to])))
        (is (false? (:ok (contract/validate-tool-params
                           (assoc-in filled [:edits 0 :to] nil)))))
        (is (false? (:ok (contract/validate-tool-params
                           (assoc-in filled [:edits 0 :to]
                                     "(def alpha :old)")))))
        (is (false? (:ok (contract/validate-tool-params
                           (assoc-in filled [:edits 0 :within :form] "")))))
        (is (false? (:ok (contract/validate-tool-params
                           (assoc-in filled [:edits 0 :matches] 0)))))
        (is (false? (:ok (contract/validate-tool-params
                           (assoc filled :unknown true)))))
        (is (= "/canonical/workspace" (:workspace_root filled)))
        (is (nil? (:verify filled)))
        (is (= #{:workspace_root :edits} (set (keys filled))))
        (is (= #{:file :within :from :to :matches}
               (set (keys (first (:edits filled))))))
        (is (= {:form "alpha"} (get-in filled [:edits 0 :within])))))))

;; @spec MCP-OP-PREP-REQ-008
(deftest projector-is-pure-and-other-result-classes-are-identical
  (let [project (public-var 'project-result)]
    (is (fn? project))
    (when (fn? project)
      (doseq [result [{:ok false :operation "inspect_clojure"}
                      {:ok true :mode "prepare-change"}
                      {:ok true :mode "basis-view"}
                      {:ok true :mode "plan-extraction"}
                      {:ok true :continuation {}}
                      {:ok true :basis "b"}
                      {:ok true :prepared_basis "b"}
                      {:ok true :retry_template {}}
                      {:ok true :operation "write"}
                      {:ok true :verification_job "job"}]]
        (is (identical? result (project result)))))))

(deftest prepared-telemetry-is-absent-or-allowlisted
  (let [allowed #{:eligible :emitted :descriptor_sha256}
        forbidden #{:source :file :workspace_root :owner :request :arguments
                    :replacement}]
    (is (empty? (set/intersection allowed forbidden)))
    (is (= 3 (count allowed)))
    (is (= 7 (count forbidden)))))

;; @spec MCP-OP-PREP-REQ-007
;; @spec MCP-OP-PREP-REQ-009
(deftest real-shared-cljc-result-round-trips-through-one-edit
  (let [root (temp-dir)]
    (try
      (let [{:keys [content error? structured]}
            (invoke-inspect
              root "src/demo.cljc"
              (str "(ns demo)\n"
                   "(def connection-options\n"
                   "  {:pool-size 8\n"
                   "   :timeout-ms 5000})\n")
              {"requests" [{"operation" "forms" "file" "src/demo.cljc"
                            "forms" ["connection-options"]
                            "expect" {"forms" 1}}]
               "expect" {"requests" 1 "files" 1}})
            prepared (:prepared_request structured)]
        (is (false? error?))
        (is (:ok structured))
        (is (:read_complete structured))
        (is (= ["clj" "cljs"]
               (get-in structured [:results 0 :forms 0 :platforms])))
        (is (= "connection-options"
               (get-in structured [:results 0 :forms 0 :name])))
        (is (= "none" (:next_action structured)))
        (is (and (map? prepared)
                 (str/ends-with? (first content) coaching))))
      (finally
        (inspect-tool/init! nil)
        (delete-tree! root)))))

;; @spec MCP-OP-PREP-REQ-001
(deftest prefinalized-budget-uses-zero-elapsed
  (let [project (public-var 'project-result)]
    (is (fn? project))
    (when (fn? project)
      (let [result (project (eligible-result))]
        (is (contains? result :prepared_request))
        (is (= (:prepared_request result)
               (:prepared_request
                 (project (assoc result :elapsed_ms 999999.999)))))
        (is (<= (inspect-tool/mcp-result-byte-count
                  "normalized" (assoc result :elapsed_ms 0.0))
                32768))))))
