(ns clj-surgeon.analyzer-contract-test
  (:require
   [babashka.fs :as fs]
   [clj-surgeon.mcp-process :as process-env]
   [clj-surgeon.move-dependency-test :as move-fixtures]
   [clojure.string :as str]
   [clojure.test :refer [deftest is testing]]
   [rewrite-clj.node :as n]
   [rewrite-clj.zip :as z]))

(defn isolate-namespace
  "Give one contract snapshot a unique namespace without changing its body."
  [source namespace]
  (let [ns-head (z/find-value (z/of-string source) z/next 'ns)
        ns-name (some-> ns-head z/right)]
    (when-not ns-name
      (throw (ex-info "Analyzer contract source has no namespace"
                      {:namespace namespace})))
    (-> ns-name
        (z/replace (n/token-node namespace))
        z/root-string)))

(defn- source-path [root namespace]
  (fs/path root
           (str (str/replace (str namespace) "." "/") ".clj")))

(defn- materialize-corpus! [root corpus]
  (mapv (fn [{:keys [id source]}]
          (let [namespace (symbol (str "clj-surgeon.analyzer-contract."
                                       (name id)))
                isolated-source (isolate-namespace source namespace)
                path (source-path root namespace)]
            (fs/create-dirs (fs/parent path))
            (spit (str path) isolated-source)
            {:id id
             :namespace namespace
             :path (str path)
             :source isolated-source}))
        corpus))

(deftest all-promised-move-snapshots-cold-lint-in-one-analyzer
  (let [root (fs/create-temp-dir {:prefix "clj-surgeon-move-contract-"})]
    (try
      (let [corpus (move-fixtures/move-lint-corpus)
            materialized (materialize-corpus! root corpus)
            command ["clj-kondo" "--lint" (str root)
                     "--cache" "false"
                     "--fail-level" "error"]]
        (testing "the complete validity promise is present before launch"
          (is (= #{:eager-candidate
                   :macro-candidate
                   :mothership-baseline
                   :mothership-candidate
                   :writer-baseline
                   :writer-candidate}
                 (set (map :id corpus))))
          (is (= 6 (count corpus)))
          (is (= 6 (count (set (map :namespace materialized)))))
          (is (every? #(= (:source %) (slurp (:path %))) materialized)))
        (let [result (process-env/run-bounded!
                       {:command command
                        :cwd (System/getProperty "user.dir")
                        :timeout-ms 120000})]
          (testing "one admitted process validates every isolated snapshot"
            (is (= :admitted (get-in result [:admission :status]))
                (pr-str (:admission result)))
            (is (:finished? result) (pr-str result))
            (is (:termination-confirmed result) (pr-str result))
            (is (zero? (:exit result)) (:err result)))))
      (finally
        (fs/delete-tree root)))))
