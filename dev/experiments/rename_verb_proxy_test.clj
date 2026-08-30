(ns rename-verb-proxy-test
  (:require
   [clj-surgeon.mcp-tool :as mcp-tool]
   [clojure.java.io :as io]
   [clojure.test :refer [deftest is testing]]
   [rename-verb-proxy :as proxy])
  (:import
   (java.nio.file Files)
   (java.nio.file.attribute FileAttribute)))

(def fixture-root "bench/fixtures/rename-verb-screen")

(defn- temp-dir []
  (.toFile (Files/createTempDirectory
             "clj-surgeon-rename-verb-test-"
             (make-array FileAttribute 0))))

(defn- delete-tree! [file]
  (when (.exists (io/file file))
    (doseq [child (reverse (file-seq (io/file file)))]
      (Files/deleteIfExists (.toPath child)))))

(defn- copy-tree! [from to]
  (doseq [source (file-seq (io/file from))]
    (let [relative (.relativize (.toPath (io/file from)) (.toPath source))
          target (.toFile (.resolve (.toPath (io/file to)) relative))]
      (if (.isDirectory source)
        (.mkdirs target)
        (do
          (.mkdirs (.getParentFile target))
          (io/copy source target))))))

(deftest verb-grammar-is-closed-and-exact
  (is (false? (:additionalProperties proxy/verb-schema)))
  (is (= ["op" "from" "to"] (:required proxy/verb-schema)))
  (is (= ["rename-symbol"]
         (get-in proxy/verb-schema [:properties "op" :enum])))
  (is (= proxy/expected-verb
         (:emitted_request
           {:emitted_request (proxy/string-keyed proxy/expected-verb)}))))

(deftest verb-lowers-to-the-complete-current-request
  (let [lowered (proxy/lower-verb proxy/expected-verb)]
    (is (:ok lowered))
    (is (= proxy/complete-edit-request (:request lowered)))
    (is (= 8 (count (get-in lowered [:request "edits"]))))
    (is (= #{"src/bench/retry.clj" "src/bench/worker.clj"}
           (set (map #(get % "file")
                     (get-in lowered [:request "edits"])))))))

(deftest malformed-verbs-refuse-without-authority
  (doseq [[label request]
          [[:missing (dissoc proxy/expected-verb "to")]
           [:extra (assoc proxy/expected-verb "owner" "retry-delay-ms")]
           [:wrong-op (assoc proxy/expected-verb "op" "rename")]
           [:wrong-from (assoc proxy/expected-verb "from" "delay-ms")]
           [:wrong-to (assoc proxy/expected-verb "to" "new-delay-ms")]]]
    (testing (name label)
      (let [result (proxy/lower-verb request)]
        (is (false? (:ok result)))
        (is (= "invalid-rename-verb" (:error_type result)))
        (is (false? (:mutation_attempted result)))
        (is (false? (:write_authority result)))))))

(deftest control-retains-the-published-surface
  (let [tool (proxy/screen-tool :T "/tmp/unused-rename-verb-capture.json")]
    (is (= (:schema mcp-tool/edit-clojure-tool) (:schema tool)))
    (is (= (:description mcp-tool/edit-clojure-tool) (:description tool)))
    (is (= "edit_clojure" (:name tool)))))

(deftest verb-handler-delegates-only-the-lowered-current-request
  (let [capture (java.io.File/createTempFile "rename-verb-capture-" ".json")
        delegated (atom nil)
        callback-result (atom nil)]
    (try
      (with-redefs [mcp-tool/handle-edit-clojure
                    (fn [_exchange request callback]
                      (reset! delegated request)
                      (callback ["ok"] false {:ok true}))]
        ((proxy/recording-handler :V (.getPath capture))
         nil proxy/expected-verb
         (fn [content error? result]
           (reset! callback-result [content error? result]))))
      (is (= proxy/complete-edit-request @delegated))
      (is (= [["ok"] false {:ok true}] @callback-result))
      (finally
        (Files/deleteIfExists (.toPath capture))))))

(deftest common-handler-mutates-the-copied-fixture-exactly
  (let [workspace (temp-dir)
        receipt-dir (io/file workspace "receipts")]
    (try
      (copy-tree! (str fixture-root "/before") workspace)
      (let [result (mcp-tool/execute-request!
                     {:project-root (.getPath workspace)
                      :receipt-dir (.getPath receipt-dir)}
                     proxy/complete-edit-request)]
        (is (:ok result) (pr-str result))
        (is (:verification_complete result))
        (is (= 8 (:edits result)))
        (is (= 2 (:files result)))
        (doseq [relative ["src/bench/retry.clj" "src/bench/worker.clj"]]
          (is (= (slurp (io/file fixture-root "after" relative))
                 (slurp (io/file workspace relative))))))
      (finally
        (delete-tree! workspace)))))
