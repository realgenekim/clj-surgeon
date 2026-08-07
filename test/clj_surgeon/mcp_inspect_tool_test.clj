(ns clj-surgeon.mcp-inspect-tool-test
  (:require
   [cheshire.core :as json]
   [clj-surgeon.mcp-inspect-tool :as inspect-tool]
   [clj-surgeon.mcp-telemetry :as telemetry]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.test :refer [deftest is]])
  (:import
   (java.nio.file Files)
   (java.nio.file.attribute FileAttribute)))

(defn- temp-dir
  []
  (.toFile
    (Files/createTempDirectory
      "clj-surgeon-mcp-inspect-test-"
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

(defn- tree-state
  [root]
  (->> (file-seq (io/file root))
       (filter #(.isFile %))
       (map (fn [file]
              [(.toString (.relativize (.toPath (io/file root))
                                       (.toPath file)))
               (slurp file)]))
       (into (sorted-map))))

(deftest reads-one-coherent-snapshot-once-for-several-requests
  (let [project (temp-dir)
        source-file (write-source!
                      project "src/demo.clj"
                      "(ns demo)\n(def alpha :old)\n(def beta :stable)\n")
        reads (atom 0)
        request
        {"requests"
         [{"id" "forms" "operation" "forms" "file" "src/demo.clj"
           "forms" ["alpha" "beta"] "expect" {"forms" 2}}
          {"id" "outline" "operation" "outline" "file" "src/demo.clj"}
          {"id" "match" "operation" "match" "file" "src/demo.clj"
           "match" ":old" "expect" {"matches" 1}}]
         "expect" {"requests" 3 "files" 1}}]
    (try
      (let [result
            (inspect-tool/execute-inspect!
              {:project-root (.getPath project)
               :read-source
               (fn [path]
                 (swap! reads inc)
                 (let [captured (slurp path)]
                   (spit source-file
                         "(ns demo)\n(def alpha :new)\n(def beta :changed)\n")
                   captured))}
              request)]
        (is (:ok result))
        (is (= 1 @reads))
        (is (= 1 (:file_read_count result)))
        (is (= "(def alpha :old)"
               (get-in result [:results 0 :forms 0 :source])))
        (is (= 1 (get-in result [:results 2 :match_count])))
        (is (= ":old" (get-in result [:results 2 :matches 0 :source])))
        (is (= (get-in result [:file_hashes "src/demo.clj"])
               (get-in result [:results 0 :file_hash]))))
      (finally
        (delete-tree! project)))))

(deftest confines-symlinks-and-canonical-file-expectations-before-read
  (let [project (temp-dir)
        outside (temp-dir)
        target (write-source! project "src/target.clj" "(ns target)\n(def x 1)\n")
        inside-link (io/file project "src/alias.clj")
        outside-file (write-source! outside "outside.clj" "(ns outside)\n")
        outside-link (io/file project "src/outside.clj")
        reads (atom 0)]
    (try
      (Files/createSymbolicLink (.toPath inside-link) (.toPath target)
                                (make-array FileAttribute 0))
      (Files/createSymbolicLink (.toPath outside-link) (.toPath outside-file)
                                (make-array FileAttribute 0))
      (let [alias-result
            (inspect-tool/execute-inspect!
              {:project-root (.getPath project)
               :read-source (fn [path] (swap! reads inc) (slurp path))}
              {"requests"
               [{"id" "one" "operation" "outline" "file" "src/target.clj"}
                {"id" "two" "operation" "outline" "file" "src/alias.clj"}]
               "expect" {"requests" 2 "files" 2}})
            escape-result
            (inspect-tool/execute-inspect!
              {:project-root (.getPath project)
               :read-source (fn [path] (swap! reads inc) (slurp path))}
              {"requests"
               [{"id" "escape" "operation" "outline"
                 "file" "src/outside.clj"}]
               "expect" {"requests" 1 "files" 1}})]
        (is (= "aggregate-file-expectation-mismatch"
               (:error_type alias-result)))
        (is (= "path-outside-project" (:error_type escape-result)))
        (is (zero? @reads)))
      (finally
        (delete-tree! project)
        (delete-tree! outside)))))

(deftest real-two-file-seven-form-dogfood-is-ordered-and-read-only
  (let [before (tree-state "bench")
        reads (atom [])
        request
        {"requests"
         [{"id" "summary-fields" "operation" "forms"
           "file" "bench/summarize_clean_codex.clj"
           "forms" ["numeric-fields" "boolean-fields" "summarize-group"
                    "markdown" "self-test"]
           "expect" {"forms" 5}}
          {"id" "rescore-fields" "operation" "forms"
           "file" "bench/rescore_clean_codex.clj"
           "forms" ["rescore-row" "emit-table"]
           "expect" {"forms" 2}}]
         "expect" {"requests" 2 "files" 2}}
        result
        (inspect-tool/execute-inspect!
          {:project-root (.getCanonicalPath (io/file "."))
           :read-source (fn [path] (swap! reads conj path) (slurp path))}
          request)]
    (is (:ok result))
    (is (= 2 (:file_read_count result)))
    (is (= 2 (count @reads)))
    (is (= ["summary-fields" "rescore-fields"] (mapv :id (:results result))))
    (is (= ["numeric-fields" "boolean-fields" "summarize-group"
            "markdown" "self-test"]
           (mapv :name (get-in result [:results 0 :forms]))))
    (is (= ["rescore-row" "emit-table"]
           (mapv :name (get-in result [:results 1 :forms]))))
    (is (= 7 (reduce + (map :form_count (:results result)))))
    (is (= before (tree-state "bench")))))

(deftest boundary-batch-runs-real-outline-match-and-xray-with-no-artifacts
  (let [project (temp-dir)
        _source (write-source!
                  project "src/demo.clj"
                  (str "(ns demo)\n"
                       "(def settings {:a 1 :b 2 :c 3})\n"
                       "(defn send-batch []\n"
                       "  \"decoy (send! _)\"\n"
                       "  (send! :actual))\n"))
        request
        {"requests"
         [{"id" "outline" "operation" "outline" "file" "src/demo.clj"}
          {"id" "match" "operation" "match" "file" "src/demo.clj"
           "match" "(send! _)" "expect" {"matches" 1}}
          {"id" "xray" "operation" "xray" "file" "src/demo.clj"
           "expression"
           "(-> (form 'settings) initializer (expect-count 1) (analyze (fn [[m]] (reduce + (vals m)))))"}]
         "expect" {"requests" 3 "files" 1}}
        before (tree-state project)]
    (try
      (let [result (inspect-tool/execute-inspect!
                     {:project-root (.getPath project)} request)]
        (is (:ok result))
        (is (= 1 (:file_read_count result)))
        (is (= 1 (get-in result [:results 1 :match_count])))
        (is (= "(send! :actual)"
               (get-in result [:results 1 :matches 0 :source])))
        (is (= 6 (get-in result [:results 2 :value])))
        (is (= before (tree-state project)))
        (is (empty? (filter #(re-find #"(?i)(plan|receipt|manifest)" %)
                            (keys (tree-state project))))))
      (finally
        (delete-tree! project)))))

(deftest callback-separates-concise-content-from-full-structured-evidence
  (let [project (temp-dir)
        _source (write-source! project "src/demo.clj"
                               "(ns demo)\n(def answer 42)\n")
        calls (atom [])]
    (try
      (inspect-tool/init! {:project-root (.getPath project)})
      (inspect-tool/handle-inspect
        nil
        {"requests" [{"id" "answer" "operation" "forms"
                      "file" "src/demo.clj" "forms" ["answer"]
                      "expect" {"forms" 1}}]
         "expect" {"requests" 1 "files" 1}}
        (fn [content error? structured]
          (swap! calls conj {:content content :error? error?
                             :structured structured})))
      (is (= false (:error? (first @calls))))
      (is (str/starts-with? (first (:content (first @calls)))
                            "inspect_clojure\n"))
      (is (not (str/includes? (first (:content (first @calls)))
                              "(def answer")))
      (is (= "(def answer 42)"
             (get-in @calls [0 :structured :results 0 :forms 0 :source])))
      (finally
        (inspect-tool/init! nil)
        (delete-tree! project)))))

(deftest metrics-telemetry-never-records-source-bodies
  (let [directory (temp-dir)
        state (telemetry/start! {:mode :metrics
                                 :directory (.getPath directory)
                                 :session-id "inspect-metrics"})
        request {"requests" [{"id" "secret" "operation" "forms"
                              "file" "src/private.clj"
                              "forms" ["secret"]
                              "expect" {"forms" 1}}]
                 "expect" {"requests" 1 "files" 1}}
        response {:ok true :read_complete true :request_count 1 :file_count 1
                  :file_read_count 1 :source_character_count 99
                  :results [{:forms [{:source "TOP-SECRET-SOURCE"}]}]}]
    (try
      (telemetry/record-inspect-call! state request response {:total_ms 1})
      (let [raw (slurp (:file state))
            event (json/parse-string (str/trim raw) true)]
        (is (= "inspect_clojure" (:tool event)))
        (is (= 1 (get-in event [:request_shape :requests])))
        (is (= 1 (get-in event [:outcome :file_reads])))
        (is (not (str/includes? raw "TOP-SECRET-SOURCE")))
        (is (not (str/includes? raw "private.clj"))))
      (finally
        (delete-tree! directory)))))
