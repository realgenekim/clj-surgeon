(ns clj-surgeon.analyzer-contract-test
  (:require
   [babashka.fs :as fs]
   [clj-surgeon.binding-rename :as binding-rename]
   [clj-surgeon.fix-declares-test :as fix-fixtures]
   [clj-surgeon.forward-refs :as forward-refs]
   [clj-surgeon.mcp-change-buffer :as change-buffer]
   [clj-surgeon.mcp-process :as process-env]
   [clj-surgeon.move-dependency-test :as move-fixtures]
   [clojure.java.io :as io]
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
           (str (-> (str namespace)
                    (str/replace "." "/")
                    (str/replace "-" "_"))
                ".clj")))

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
            (is (zero? (:exit result))
                (pr-str {:out (:out result) :err (:err result)})))))
      (finally
        (fs/delete-tree root)))))

(deftest diagnostic-baseline-and-future-share-one-real-provider-contract
  (let [root (fs/create-temp-dir {:prefix "clj-surgeon-diagnostic-contract-"})
        core (io/file (str root) "src/sample/core.clj")
        caller (io/file (str root) "src/sample/caller.clj")
        macros (io/file (str root) "src/sample/macros.clj")
        kondo-config (io/file (str root) ".clj-kondo/config.edn")
        profiles {"fast" {:commands [["clj-kondo" "--lint" "{files}"]]}}
        files ["src/sample/core.clj" "src/sample/caller.clj"
               "src/sample/macros.clj"]]
    (try
      (doseq [file [core caller macros kondo-config]]
        (.mkdirs (.getParentFile file)))
      (spit kondo-config
            "{:lint-as {sample.macros/>defn clojure.core/defn}}\n")
      (spit macros
            (str "(ns sample.macros)\n"
                 "(defmacro >defn [& body] (cons 'defn body))\n"))
      (spit core
            (str "(ns sample.core\n"
                 "  (:require [sample.macros :as macros]))\n"
                 "(defn existing [] :ok)\n"))
      (spit caller
            (str "(ns sample.caller\n"
                 "  (:require [sample.core :as core]))\n"
                 "(def before (core/existing))\n"))
      (let [baseline (change-buffer/capture-verification-baseline!
                       (io/file (str root)) "fast" profiles files)]
        (is (:ok baseline) (pr-str baseline))
        (is (= 1 (count (get-in baseline [:checks 0 :diagnostics :findings])))
            "the future transaction may remove a pre-existing warning")
        (spit core
              (str "(ns sample.core\n"
                   "  (:require [sample.macros :as macros]))\n"
                   "(defn existing [] :ok)\n"
                   "(macros/>defn added [] :added)\n"))
        (spit caller
              (str "(ns sample.caller\n"
                   "  (:require [sample.core :as core]))\n"
                   "(def before (core/existing))\n"
                   "(def after (core/added))\n"))
        (let [verification (change-buffer/run-verification!
                             (io/file (str root)) "fast" profiles files baseline)]
          (is (:ok verification) (pr-str verification))
          (is (empty? (get-in verification
                              [:checks 0 :diagnostic-delta
                               :blocking-introduced])))))
      (finally
        (fs/delete-tree root)))))

(deftest forward-reference-analysis-retains-the-provider-schema
  (let [root (fs/create-temp-dir {:prefix "clj-surgeon-forward-contract-"})
        source (fs/path root "src/my/app.clj")]
    (try
      (fs/create-dirs (fs/parent source))
      (spit (str source) fix-fixtures/simple-forward-ref)
      (is (= [{:name 'helper
               :used-at 6
               :defined-at 8
               :gap 2}]
             (forward-refs/detect-forward-refs (str source) 'my.app)))
      (finally
        (fs/delete-tree root)))))

(deftest binding-analysis-retains-local-identity-and-destructuring-schema
  (let [root (fs/create-temp-dir {:prefix "clj-surgeon-binding-contract-"})
        source-file (fs/path root "src/demo.clj")
        source (str "(ns demo)\n"
                    "(defn feed [{:keys [sort-by] :or {sort-by :score}}] [sort-by :sort-by clojure.core/sort-by])\n"
                    "(defn table [{:keys [sort-by]}] (name sort-by))\n")]
    (try
      (fs/create-dirs (fs/parent source-file))
      (let [analysis (binding-rename/analyze-source (str source-file) source)
            locals (:locals analysis)
            usages (:local-usages analysis)
            keywords (:keywords analysis)
            local-ids (set (map :id locals))]
        (is (= 2 (count locals)))
        (is (= 3 (count usages)))
        (is (= 2 (count keywords)))
        (is (= #{'sort-by} (set (map :name locals))))
        (is (= local-ids (set (map :id usages))))
        (is (every? :keys-destructuring keywords))
        (is (= #{"sort-by"} (set (map :name keywords)))))
      (finally
        (fs/delete-tree root)))))
