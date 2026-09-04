(ns clj-surgeon.mcp-hot-verify
  "Reload namespaces and run exact focused test Vars in one configured app JVM."
  (:require
   [clj-surgeon.measured :as measured]
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.set :as set]
   [clojure.string :as str]
   [nrepl.core :as nrepl]))

(def ^:private qualified-name-pattern
  #"^[A-Za-z0-9._!?*+<>=%$-]+(?:/[A-Za-z0-9._!?*+<>=%$-]+)?$")

(defn valid-profile?
  [profile]
  (and (map? profile)
       (= (set (keys profile))
          (cond-> #{:port-file :reload :tests}
            (:timeout-ms profile) (conj :timeout-ms)))
       (string? (:port-file profile))
       (not (str/blank? (:port-file profile)))
       (not (.isAbsolute (io/file (:port-file profile))))
       (not (str/includes? (:port-file profile) ".."))
       (vector? (:reload profile))
       (every? #(and (string? %) (re-matches qualified-name-pattern %))
               (:reload profile))
       (vector? (:tests profile))
       (every? #(and (string? %)
                     (str/includes? % "/")
                     (re-matches qualified-name-pattern %))
               (:tests profile))
       (or (nil? (:timeout-ms profile))
           (and (int? (:timeout-ms profile))
                (<= 100 (:timeout-ms profile) 60000)))))

(defn- eval-code
  [{:keys [reload tests]}]
  (str
    "(do "
    (apply str (map #(str "(require '" % " :reload) ") reload))
    "(require '[clojure.test :as t]) "
    "(let [summary "
    (if (seq tests)
      (str "(binding [t/*report-counters* (ref t/*initial-report-counters*)] "
           "(t/test-vars ["
           (str/join " " (map #(str "#'" %) tests))
           "]) @t/*report-counters*)")
      "t/*initial-report-counters*")
    "] {:cwd (System/getProperty \"user.dir\") "
    ":pid (.pid (java.lang.ProcessHandle/current)) "
    ":summary summary}))"))

(defn verify!
  "Run one closed hot profile against the configured application nREPL."
  [project-root profile]
  (let [started (measured/start)]
    (if-not (valid-profile? profile)
      {:ok false :status :refused
       :error-type :invalid-hot-verification-profile
       :error "Hot verification profile is not closed data"}
      (let [root (.getCanonicalFile (io/file project-root))
            port-file (.getCanonicalFile (io/file root (:port-file profile)))]
        (if-not (str/starts-with? (.getPath port-file)
                                  (str (.getPath root) java.io.File/separator))
          {:ok false :status :refused
           :error-type :hot-verification-path-escape
           :error "Hot verification port file is outside the project root"}
          (try
            (let [port (parse-long (str/trim (slurp port-file)))
                  timeout (or (:timeout-ms profile) 10000)]
              (when-not (and port (<= 1 port 65535))
                (throw (ex-info "Invalid application nREPL port"
                                {:error-type :invalid-hot-verification-port})))
              (with-open [connection (nrepl/connect :host "127.0.0.1" :port port)]
                (let [client (nrepl/client connection timeout)
                      responses (doall (client {:op "eval"
                                                :code (eval-code profile)}))
                      statuses (set (mapcat :status responses))
                      value-source (last (keep :value responses))
                      value (when value-source (edn/read-string value-source))
                      summary (:summary value)
                      cwd (some-> (:cwd value) io/file .getCanonicalPath)
                      ok (and value
                              (= (.getPath root) cwd)
                              (empty? (set/intersection
                                        statuses
                                        #{"eval-error" "error" "timeout"}))
                              (zero? (long (or (:fail summary) 0)))
                              (zero? (long (or (:error summary) 0))))]
                  {:ok ok
                   :status (if ok :complete :failed)
                   :jvm "application"
                   :pid (:pid value)
                   :cwd cwd
                   :reload-count (count (:reload profile))
                   :law-count (count (:tests profile))
                   :summary summary
                   :elapsed_ms (measured/elapsed-ms started)
                   :error-type (when-not ok :hot-verification-failed)
                   :output (when-not ok
                             (subs (str/join "" (keep #(or (:err %) (:out %))
                                                      responses))
                                   0
                                   (min 4000
                                        (count (str/join ""
                                                         (keep #(or (:err %) (:out %))
                                                               responses))))))})))
            (catch Exception error
              {:ok false
               :status :failed
               :error-type (or (:error-type (ex-data error))
                               :hot-verification-connection-failed)
               :error (.getMessage error)
               :elapsed_ms (measured/elapsed-ms started)})))))))
