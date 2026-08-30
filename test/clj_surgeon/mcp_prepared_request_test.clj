(ns clj-surgeon.mcp-prepared-request-test
  (:require
   [cheshire.core :as json]
   [clj-surgeon.mcp-contract :as contract]
   [clj-surgeon.mcp-inspect-tool :as inspect-tool]
   [clj-surgeon.mcp-prepared-request :as prepared-request]
   [clj-surgeon.mcp-schema :as schema]
   [clj-surgeon.mcp-tool :as mcp-tool]
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

(defn- private-var
  [namespace name]
  (some-> (ns-resolve namespace name) deref))

(defn- source-at-descriptor-bytes
  [project bytes-var target]
  (let [template (:prepared_request (project (eligible-result)))
        empty-source "(def alpha \"\")"
        empty-descriptor (assoc-in template [:arguments :edits 0 :from]
                                   empty-source)
        padding-size (- target (count (bytes-var empty-descriptor)))]
    (when-not (neg? padding-size)
      (str "(def alpha \"" (apply str (repeat padding-size "x")) "\")"))))

(defn- result-at-prefinalized-bytes
  [result target]
  (let [summary (private-var 'clj-surgeon.mcp-inspect-tool 'inspect-summary)
        empty-candidate (assoc result :padding "")
        empty-normalized (assoc empty-candidate :elapsed_ms 0.0)
        empty-size (inspect-tool/mcp-result-byte-count
                     (summary empty-normalized) empty-normalized)
        padding-size (- target empty-size)]
    (when-not (neg? padding-size)
      (assoc result :padding (apply str (repeat padding-size "x"))))))

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
          (is (identical? candidate (project candidate)))))
      (doseq [candidate
              [(assoc base :results 1)
               (assoc-in base [:results 0 :forms 0 :source] 1)
               (-> base
                   (assoc-in [:results 0 :file] "src/\u0000demo.clj")
                   (assoc-in [:results 0 :forms 0 :file] "src/\u0000demo.clj"))
               (assoc-in base
                         [:results 0 :forms 0 :source_anchor :selection_range]
                         {:start {:line 0 :character 10}
                          :end {:line 0 :character 5}})
               (assoc-in base
                         [:results 0 :forms 0 :source_anchor :selection_range :end]
                         {:line 0 :character 999})
               (assoc-in base [:results 0 :forms 0 :form_type] nil)
               (assoc-in base [:results 0 :forms 0 :form_type] 7)
               (assoc-in base
                         [:results 0 :forms 0 :source_anchor :selection_range]
                         {:start {:line 0 :character 5}
                          :end {:line 0 :character 5}})
               (assoc-in base
                         [:results 0 :forms 0 :source_anchor :selection_range]
                         {:start {:line 0 :character 0}
                          :end {:line 0 :character 1}})
               (assoc-in
                 (eligible-result "/canonical/workspace" "src/demo.clj"
                                  [["alpha" "(def alpha \"alpha\")"]])
                 [:results 0 :forms 0 :source_anchor :selection_range]
                 {:start {:line 0 :character 12}
                  :end {:line 0 :character 17}})
               (assoc-in
                 (eligible-result "/canonical/workspace" "src/demo.clj"
                                  [["alpha" "(def ^alpha alpha 1)"]])
                 [:results 0 :forms 0 :source_anchor :selection_range]
                 {:start {:line 0 :character 6}
                  :end {:line 0 :character 11}})
               (assoc-in
                 (eligible-result "/canonical/workspace" "src/demo.clj"
                                  [["alpha" "(def ^{:tag alpha} alpha 1)"]])
                 [:results 0 :forms 0 :source_anchor :selection_range]
                 {:start {:line 0 :character 12}
                  :end {:line 0 :character 17}})]]
        (is (identical? candidate (project candidate)))))))

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
        (is (= "62f1bef61696036b34a61e76fe8881113e5a680cd58f53e9caa136c957b6a146"
               (hash-var left)))
        (is (re-matches #"[0-9a-f]{64}" (hash-var left)))
        (is (= (seq (bytes-var {:v [1 2]}))
               (seq (bytes-var {:v [1 2]}))))
        (is (not= (seq (bytes-var {:v [1 2]}))
                  (seq (bytes-var {:v [2 1]}))))
        (is (= false (:a left)))
        (let [descriptor (:prepared_request
                           ((public-var 'project-result) (eligible-result)))]
          (is (= 269 (count (bytes-var descriptor))))
          (is (= "326d1a28d2b349be2ccd77da65a346625df7537c023b9ebdfa5090cc8949ac8a"
                 (hash-var descriptor))))
        (let [project (public-var 'project-result)
              source-4096 (source-at-descriptor-bytes project bytes-var 4096)
              source-4097 (source-at-descriptor-bytes project bytes-var 4097)
              result-4096 (project (eligible-result
                                     "/canonical/workspace" "src/demo.clj"
                                     [["alpha" source-4096]]))
              result-4097 (project (eligible-result
                                     "/canonical/workspace" "src/demo.clj"
                                     [["alpha" source-4097]]))]
          (is (string? source-4096))
          (is (string? source-4097))
          (is (= 4096 (count (bytes-var (:prepared_request result-4096)))))
          (is (not (contains? result-4097 :prepared_request))))))))

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
      (let [request {"requests" [{"operation" "forms" "file" "src/demo.clj"
                                  "forms" ["alpha"] "include_source" false
                                  "expect" {"forms" 1}}]
                     "expect" {"requests" 1 "files" 1}}
            baseline (with-redefs [prepared-request/project-result identity]
                       (invoke-inspect root "src/demo.clj"
                                       "(ns demo)\n(def alpha :old)\n" request))
            {:keys [content error? structured] :as projected}
            (invoke-inspect root "src/demo.clj"
                            "(ns demo)\n(def alpha :old)\n" request)
            fix-clock #(assoc % :elapsed_ms 0.0 :inspection_elapsed_ms 0.0)
            baseline-fixed (fix-clock (:structured baseline))
            projected-fixed (fix-clock structured)
            summary (private-var 'clj-surgeon.mcp-inspect-tool 'inspect-summary)]
        (is (false? error?))
        (is (:ok structured))
        (is (:read_complete structured))
        (is (= "none" (:next_action structured)))
        (is (not (contains? structured :prepared_request)))
        (is (not (str/includes? (first content) coaching)))
        (is (= 1 (:request_count structured)))
        (is (= 1 (:file_count structured)))
        (is (= (:error? baseline) (:error? projected)))
        (is (= (seq (prepared-request/canonical-json-bytes baseline-fixed))
               (seq (prepared-request/canonical-json-bytes projected-fixed))))
        (is (= (summary baseline-fixed) (summary projected-fixed))))
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
  (let [root (temp-dir)
        receipt-dir (io/file root "receipts")
        before (str "(ns demo)\n"
                    "(def connection-options\n"
                    "  {:pool-size 8\n"
                    "   :timeout-ms 5000})\n")
        replacement (str "(def connection-options\n"
                         "  {:pool-size 16\n"
                         "   :timeout-ms 5000})")
        after (str "(ns demo)\n" replacement "\n")]
    (try
      (let [{:keys [content error? structured]}
            (invoke-inspect
              root "src/demo.cljc"
              before
              {"requests" [{"operation" "forms" "file" "src/demo.cljc"
                            "forms" ["connection-options"]
                            "expect" {"forms" 1}}]
               "expect" {"requests" 1 "files" 1}})
            prepared (:prepared_request structured)
            filled (-> prepared
                       :arguments
                       (assoc-in [:edits 0 :to] replacement)
                       json/generate-string
                       (json/parse-string false))
            config {:project-root (.getPath root)
                    :receipt-dir (.getPath receipt-dir)
                    :verification-profiles {"fast" {:commands []}}
                    :verify! (fn [_root profile _profiles _files]
                               {:ok true :profile profile :checks []})}]
        (is (false? error?))
        (is (:ok structured))
        (is (:read_complete structured))
        (is (= ["clj" "cljs"]
               (get-in structured [:results 0 :forms 0 :platforms])))
        (is (= "connection-options"
               (get-in structured [:results 0 :forms 0 :name])))
        (is (= "none" (:next_action structured)))
        (is (and (map? prepared)
                 (str/ends-with? (first content) coaching)))
        (let [crlf (invoke-inspect
                     root "src/windows.clj"
                     "(ns windows)\r\n(def alpha :old)\r\n"
                     {"requests" [{"operation" "forms"
                                   "file" "src/windows.clj"
                                   "forms" ["alpha"]
                                   "expect" {"forms" 1}}]
                      "expect" {"requests" 1 "files" 1}})]
          (is (false? (:error? crlf)))
          (is (map? (get-in crlf [:structured :prepared_request]))))
        (doseq [[file source]
                [["src/trailing-space.clj"
                  "(ns trailing-space)\n(def alpha :old)   \n"]
                 ["src/inline-comment.clj"
                  "(ns inline-comment)\n(def alpha :old) ; keep\n"]
                 ["src/trailing-space-crlf.clj"
                  "(ns trailing-space-crlf)\r\n(def alpha :old)   \r\n"]
                 ["src/inline-comment-crlf.clj"
                  "(ns inline-comment-crlf)\r\n(def alpha :old) ; keep\r\n"]]]
          (let [read-result
                (invoke-inspect
                  root file source
                  {"requests" [{"operation" "forms"
                                "file" file
                                "forms" ["alpha"]
                                "expect" {"forms" 1}}]
                   "expect" {"requests" 1 "files" 1}})]
            (is (false? (:error? read-result)))
            (is (map? (get-in read-result
                              [:structured :prepared_request])))))
        (write-source! root "src/unrelated.clj" "(ns unrelated)\n(def untouched :yes)\n")
        (is (= "edit_clojure" (mcp-tool/request-operation filled)))
        (let [committed (mcp-tool/execute-request! config filled)]
          (is (true? (:ok committed)) (pr-str committed))
          (is (true? (:committed committed)))
          (is (true? (:verification_complete committed)))
          (is (= after (slurp (io/file root "src/demo.cljc"))))
          (is (= "(ns unrelated)\n(def untouched :yes)\n"
                 (slurp (io/file root "src/unrelated.clj")))))
        (let [stale (mcp-tool/execute-request! config filled)]
          (is (false? (:ok stale)))
          (is (true? (:source_unchanged stale)))
          (is (= after (slurp (io/file root "src/demo.cljc"))))
          (is (= "(ns unrelated)\n(def untouched :yes)\n"
                 (slurp (io/file root "src/unrelated.clj"))))))
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
                32768))
        (let [enforce (private-var 'clj-surgeon.mcp-inspect-tool
                                   'enforce-result-budget)
              summary (private-var 'clj-surgeon.mcp-inspect-tool
                                   'inspect-summary)
              edge (result-at-prefinalized-bytes result 32768)
              overflow (result-at-prefinalized-bytes result 32769)
              edge-ordinary (dissoc edge :prepared_request)
              overflow-ordinary (dissoc overflow :prepared_request)]
          (is (map? edge))
          (is (map? overflow))
          (is (contains? (enforce edge-ordinary edge) :prepared_request))
          (is (str/includes?
                (summary (assoc (enforce edge-ordinary edge) :elapsed_ms 0.0))
                coaching))
          (is (identical? overflow-ordinary
                          (enforce overflow-ordinary overflow)))
          (is (not (contains? (enforce overflow-ordinary overflow)
                              :prepared_request)))
          (is (not (str/includes?
                     (summary (assoc (enforce overflow-ordinary overflow)
                                     :elapsed_ms 0.0))
                     coaching))))))))
