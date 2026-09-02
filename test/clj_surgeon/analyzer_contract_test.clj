(ns clj-surgeon.analyzer-contract-test
  ;; @spec MCP-OP-ANALYZER-008
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
   [clojure.test :as test :refer [deftest is testing]]
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

(def analyzer-pressure-deferred-reason
  :clj-kondo-pressure-deferred)

(defmethod test/report :summary
  [event]
  (let [test-count (:test event)
        assertion-count (+ (:pass event) (:fail event) (:error event))
        skip-count (get event :skip 0)]
    (binding [*out* test/*test-out*]
      (println
        (format
          "\nRan %d tests containing %d assertions; %d skips, %d failures, %d errors."
          test-count assertion-count skip-count (:fail event) (:error event))))))

(defmethod test/report ::analyzer-unavailable
  [{:keys [reason] :as event}]
  (if (= analyzer-pressure-deferred-reason reason)
    (do
      (test/inc-report-counter :skip)
      (binding [*out* test/*test-out*]
        (println "\nAnalyzer unavailable:" (name reason))))
    (let [message (or (:message event)
                      (str "Analyzer contract violation: " (name reason)))]
      (test/report
        (-> event
            (assoc :type :error
                   :message message
                   :actual (or (:actual event)
                               (ex-info message {:error-type reason})))
            (dissoc :reason))))))

(defn analyzer-pressure-deferred?
  [value]
  (boolean
    (some (fn [node]
            (and (map? node)
                 (= analyzer-pressure-deferred-reason (:error-type node))))
          (tree-seq coll? seq value))))

(defn report-analyzer-unavailable!
  [value]
  (when (analyzer-pressure-deferred? value)
    (test/do-report {:type ::analyzer-unavailable
                     :reason analyzer-pressure-deferred-reason})
    true))

(defmacro with-analyzer-contract-authority
  [& body]
  `(try
     ~@body
     (catch clojure.lang.ExceptionInfo error#
       (when-not (report-analyzer-unavailable! (ex-data error#))
         (throw error#)))))

(deftest analyzer-unavailable-report-is-counted-and-fail-closed
  (let [pressure-counters (ref {:test 1 :pass 0 :fail 0 :error 0})
        pressure-output (let [writer (java.io.StringWriter.)]
                          (binding [test/*report-counters* pressure-counters
                                    test/*test-out* writer]
                            (test/do-report
                              {:type ::analyzer-unavailable
                               :reason :clj-kondo-pressure-deferred}))
                          (str writer))
        drift-counters (ref {:test 1 :pass 0 :fail 0 :error 0})
        drift-output (let [writer (java.io.StringWriter.)]
                       (binding [test/*report-counters* drift-counters
                                 test/*test-out* writer]
                         (test/do-report
                           {:type ::analyzer-unavailable
                            :reason :provider-schema-drift}))
                       (str writer))]
    (is (= {:test 1 :pass 0 :fail 0 :error 0 :skip 1}
           @pressure-counters))
    (is (str/includes?
          pressure-output
          "Analyzer unavailable: clj-kondo-pressure-deferred"))
    (is (= {:test 1 :pass 0 :fail 0 :error 1}
           @drift-counters))
    (is (str/includes? drift-output "provider-schema-drift"))))

(deftest analyzer-summary-makes-skip-count-legible
  (let [writer (java.io.StringWriter.)]
    (binding [test/*test-out* writer]
      (test/do-report {:type :summary
                       :test 5
                       :pass 8
                       :fail 0
                       :error 0
                       :skip 4}))
    (is (str/includes?
          (str writer)
          "Ran 5 tests containing 8 assertions; 4 skips, 0 failures, 0 errors."))))

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
          (when-not (report-analyzer-unavailable! result)
            (testing "one admitted process validates every isolated snapshot"
              (is (= :admitted (get-in result [:admission :status]))
                  (pr-str (:admission result)))
              (is (:finished? result) (pr-str result))
              (is (:termination-confirmed result) (pr-str result))
              (is (zero? (:exit result))
                  (pr-str {:out (:out result) :err (:err result)}))))))
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
        (when-not (report-analyzer-unavailable! baseline)
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
            (when-not (report-analyzer-unavailable! verification)
              (is (:ok verification) (pr-str verification))
              (is (empty? (get-in verification
                                  [:checks 0 :diagnostic-delta
                                   :blocking-introduced])))))))
      (finally
        (fs/delete-tree root)))))

(deftest forward-reference-analysis-retains-the-provider-schema
  (with-analyzer-contract-authority
    (let [root (fs/create-temp-dir {:prefix "clj-surgeon-forward-contract-"})
          source (fs/path root "src/my/app.clj")]
      (try
        (fs/create-dirs (fs/parent source))
        (spit (str source) fix-fixtures/simple-forward-ref)
        (let [actual (forward-refs/detect-forward-refs (str source) 'my.app)]
          (is (= [{:name 'helper
                   :used-at 6
                   :defined-at 8
                   :gap 2}]
                 actual)))
        (finally
          (fs/delete-tree root))))))

(deftest binding-analysis-retains-local-identity-and-destructuring-schema
  (with-analyzer-contract-authority
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
              destructuring-keywords (filterv :keys-destructuring keywords)
              local-ids (set (map :id locals))]
          (is (= 2 (count locals)))
          (is (= 3 (count usages)))
          (is (= 2 (count destructuring-keywords)))
          (is (= #{'sort-by} (set (map :name locals))))
          (is (= local-ids (set (map :id usages))))
          (is (= #{2 3} (set (map :row destructuring-keywords))))
          (is (= #{"sort-by"}
                 (set (map :name destructuring-keywords)))))
        (finally
          (fs/delete-tree root))))))
