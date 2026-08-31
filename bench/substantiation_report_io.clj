(ns substantiation-report-io
  "Confined I/O shell for one substantiation report."
  (:require
   [clj-surgeon.mcp-substantiation :as substantiation]
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [substantiation-report :as report])
  (:gen-class))

(defn- canonical-file [path]
  (.getCanonicalFile (io/file path)))

(defn- ensure-confined! [root path]
  (let [root (.toPath (canonical-file root))
        path (.toPath (canonical-file path))]
    (when-not (.startsWith path root)
      (throw (ex-info "Substantiation output escapes its named root"
                      {:error-type :substantiation-output-root-escape})))
    (.toFile path)))

(defn- write-once! [file content]
  (when (.exists file)
    (throw (ex-info "Substantiation output already exists"
                    {:error-type :substantiation-output-exists
                     :file (.getPath file)})))
  (spit file content)
  (.setReadable file false false)
  (.setWritable file false false)
  (.setExecutable file false false)
  (.setReadable file true true)
  (.setWritable file true true)
  file)

(defn- create-private-output-root! [root]
  (when (.exists root)
    (throw (ex-info "Substantiation output root already exists"
                    {:error-type :substantiation-output-exists
                     :file (.getPath root)})))
  (when-not (.mkdir root)
    (throw (ex-info "Substantiation output root could not be created"
                    {:error-type :substantiation-output-create-failed
                     :file (.getPath root)})))
  (.setReadable root false false)
  (.setWritable root false false)
  (.setExecutable root false false)
  (.setReadable root true true)
  (.setWritable root true true)
  (.setExecutable root true true)
  root)

(defn- count-feature [compiled feature-id stage]
  (long (or (get-in compiled [:features [feature-id stage]]) 0)))

(defn- markdown-report [compiled]
  (str
    "# Substantiation report\n\n"
    "Prepared requests: "
    (count-feature compiled "prepared-request" "emitted") " emitted; "
    (count-feature compiled "prepared-request" "consumed") " consumed; "
    (count-feature compiled "prepared-request" "committed")
    " committed. [MEASURED]\n\n"
    "Complete refusals: "
    (count-feature compiled "complete-refusal" "fired") " fired; "
    (get-in compiled [:recovery :same_file_rereads])
    " same-file rereads. [MEASURED]\n\n"
    "WRITE-REFUSAL-001: "
    (count-feature compiled "write-refusal-001" "fired") " fired; "
    (count-feature compiled "write-refusal-001" "continuation-returned")
    " continuations returned. [MEASURED]\n\n"
    "Historical comparisons are [OBSERVED BEFORE/AFTER]. "
    "Decode-equivalent arithmetic at "
    (get-in compiled [:projection :emitted_byte_ms])
    " ms/emitted byte is [PROJECTED].\n\n"
    "This report has no performance-promotion or install authority.\n"))

(defn compile-report!
  [{:keys [ledger registry baseline output-root installed-commit installed-tag
           compiler-commit compiler-tree]}]
  (let [ledger-file (canonical-file ledger)
        registry-data (edn/read-string (slurp registry))
        baseline-data (edn/read-string (slurp baseline))
        ledger-text (slurp ledger-file)
        events (report/parse-lines ledger-text)
        output-root (canonical-file output-root)
        registry-text (slurp registry)
        baseline-text (slurp baseline)
        marker-open
        {:ledger_path (.getPath ledger-file)
         :ledger_sha256 (substantiation/sha256 ledger-text)
         :start_sequence (some-> events first :sequence)
         :end_sequence (some-> events last :sequence)
         :last_event_sha256 (some-> events last :event_sha256)
         :key_id (some-> events first :transport :key_id)
         :window_start_utc (some-> events first :observed_at)
         :window_end_utc (some-> events last :observed_at)
         :installed_commit installed-commit
         :installed_tag installed-tag
         :compiler_commit compiler-commit
         :compiler_tree compiler-tree
         :feature_registry_sha256 (substantiation/sha256 registry-text)
         :baseline_sha256 (substantiation/sha256 baseline-text)
         :classifier_sha256 (get-in baseline-data [:authority :classifier_sha256])}
        marker (assoc marker-open
                      :sha256 (substantiation/sha256
                                (substantiation/canonical-json marker-open)))
        compiled (report/compile-report
                   {:events events
                    :registry registry-data
                    :baseline baseline-data
                    :marker marker
                    :ledger-bytes (alength (.getBytes ledger-text "UTF-8"))})
        episodes-json (substantiation/canonical-json (:episodes compiled))
        report-json (substantiation/canonical-json (dissoc compiled :episodes))
        outputs {:episodes.json episodes-json
                 :episodes.sha256 (str (substantiation/sha256 episodes-json) "\n")
                 :report.json report-json
                 :report.md (markdown-report compiled)
                 :receipt.edn (pr-str {:schema "clj-surgeon.substantiation-receipt.v1"
                                       :marker marker
                                       :installed_commit installed-commit
                                       :installed_tag installed-tag
                                       :registry_sha256
                                       (substantiation/sha256 registry-text)
                                       :baseline_sha256
                                       (substantiation/sha256 baseline-text)})}
        _ (create-private-output-root! output-root)]
    (doseq [[artifact content] outputs]
      (write-once! (ensure-confined! output-root
                                     (io/file output-root (name artifact)))
                   content))
    {:ok true :output-root (.getPath output-root) :marker marker}))

(defn -main [& args]
  (let [[ledger output-root installed-commit installed-tag
         compiler-commit compiler-tree] args]
    (when-not (every? #(and (string? %) (not (str/blank? %)))
                      [ledger output-root installed-commit installed-tag
                       compiler-commit compiler-tree])
      (throw (ex-info "Usage: ledger output-root installed-commit installed-tag compiler-commit compiler-tree"
                      {:error-type :invalid-substantiation-report-arguments})))
    (println
      (pr-str
        (compile-report! {:ledger ledger
                          :output-root output-root
                          :installed-commit installed-commit
                          :installed-tag installed-tag
                          :compiler-commit compiler-commit
                          :compiler-tree compiler-tree
                          :registry "bench/fixtures/substantiation_telemetry/feature_registry.edn"
                          :baseline "bench/fixtures/substantiation_telemetry/baseline.edn"})))))
