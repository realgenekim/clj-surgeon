(ns clj-surgeon.mcp-telemetry
  (:require
   [cheshire.core :as json]
   [clojure.java.io :as io]
   [clojure.string :as str])
  (:import
   (java.nio.file Files)
   (java.nio.file.attribute PosixFilePermissions)
   (java.time Instant)
   (java.util UUID)))

(def telemetry-schema-version 1)
(def default-retention-days 30)
(def supported-modes #{:off :metrics :full})

(defn default-directory
  []
  (str (io/file (System/getProperty "user.home")
                ".local" "state" "clj-surgeon" "telemetry")))

(defn- normalize-mode
  [mode]
  (let [normalized (cond
                     (nil? mode) :metrics
                     (keyword? mode) mode
                     (string? mode) (keyword (str/lower-case mode))
                     :else mode)]
    (when-not (contains? supported-modes normalized)
      (throw (ex-info "Telemetry mode must be off, metrics, or full"
                      {:error-type :invalid-telemetry-mode
                       :actual mode
                       :supported (vec (sort supported-modes))})))
    normalized))

(defn- private-permissions!
  [file permissions]
  (try
    (Files/setPosixFilePermissions
      (.toPath (io/file file))
      (PosixFilePermissions/fromString permissions))
    (catch Exception _ nil))
  file)

(defn prune!
  "Delete telemetry JSONL files older than retention-days. Return removed paths."
  ([directory retention-days]
   (prune! directory retention-days (System/currentTimeMillis)))
  ([directory retention-days now-ms]
   (let [cutoff (- now-ms (* retention-days 24 60 60 1000))]
     (if-let [files (some-> (io/file directory) .listFiles seq)]
       (->> files
            (filter #(and (.isFile %)
                          (str/ends-with? (.getName %) ".jsonl")
                          (< (.lastModified %) cutoff)))
            (sort-by #(.getCanonicalPath %))
            (keep (fn [file]
                    (let [path (.getCanonicalPath file)]
                      (when (Files/deleteIfExists (.toPath file)) path))))
            vec)
       []))))

(defn start!
  "Create one local telemetry session. Modes are :off, :metrics, and :full."
  [{:keys [mode directory retention-days session-id run-id]}]
  (let [mode (normalize-mode mode)
        session-id (str (or session-id (UUID/randomUUID)))]
    (if (= :off mode)
      {:mode :off :session-id session-id :run-id run-id :file nil :lock (Object.)}
      (let [directory (str (or directory (default-directory)))
            directory-file (io/file directory)
            _ (.mkdirs directory-file)
            _ (private-permissions! directory-file "rwx------")
            _ (prune! directory (or retention-days default-retention-days))
            safe-id (if (re-matches #"[A-Za-z0-9._-]+" session-id)
                      session-id
                      (str (UUID/randomUUID)))
            file (io/file directory (str safe-id ".jsonl"))]
        (when (.exists file)
          (throw (ex-info "Telemetry session file already exists"
                          {:error-type :telemetry-session-exists
                           :session-id session-id})))
        (spit file "")
        (private-permissions! file "rw-------")
        {:mode mode
         :session-id session-id
         :run-id run-id
         :file (.getCanonicalPath file)
         :lock (Object.)}))))

(defn emit!
  "Append one structured event. Never writes to stdout."
  [state event data]
  (when (and state (not= :off (:mode state)))
    (let [record (cond->
                   (merge
                     {:telemetry_schema telemetry-schema-version
                      :timestamp (str (Instant/now))
                      :event (name event)
                      :session_id (:session-id state)
                      :telemetry_mode (name (:mode state))}
                     data)
                   (:run-id state) (assoc :run_id (:run-id state)))
          line (str (json/generate-string record) "\n")]
      (locking (:lock state)
        (spit (:file state) line :append true))
      record)))

(defn- value
  [m key]
  (if (contains? m key) (get m key) (get m (name key))))

(defn request-shape
  "Return content-free cardinality and payload-size evidence."
  [request]
  (let [changes (vec (or (value request :changes) []))
        aggregate (or (value request :expect) {})
        all-files (mapcat #(or (value % :files) []) changes)
        all-forms (mapcat #(or (value % :forms) []) changes)]
    {:changes (count changes)
     :declared_edits (value aggregate :edits)
     :declared_files (value aggregate :files)
     :file_references (count all-files)
     :form_references (count all-forms)
     :find_characters (reduce + (map #(count (str (or (value % :find) "")))
                                     changes))
     :replacement_characters
     (reduce + (map #(count (str (or (value % :replace) ""))) changes))}))

(defn outcome-shape
  "Return stable result fields without paths, source, hashes, or receipts."
  [response]
  (cond->
    {:ok (boolean (:ok response))}
    (contains? response :committed) (assoc :committed (:committed response))
    (contains? response :verification_complete)
    (assoc :verification_complete (:verification_complete response))
    (contains? response :changes) (assoc :changes (:changes response))
    (contains? response :edits) (assoc :edits (:edits response))
    (contains? response :files) (assoc :files (:files response))
    (:error_type response) (assoc :error_type (:error_type response))
    (contains? response :source_unchanged)
    (assoc :source_unchanged (:source_unchanged response))
    (contains? response :rolled_back)
    (assoc :rolled_back (:rolled_back response))))

(defn call-event
  "Build the mode-dependent tool.call event payload without performing I/O."
  [mode request response timings]
  (cond->
    {:tool "apply_clojure_changes"
     :request_shape (request-shape request)
     :outcome (outcome-shape response)
     :timings_ms timings}
    (= :full (normalize-mode mode))
    (assoc :request request :response response)))

(defn record-call!
  [state request response timings]
  (emit! state :tool.call
         (call-event (:mode state) request response timings)))

(defn inspect-request-shape
  "Return source-free shape and payload-size evidence for inspect_clojure."
  [request]
  (let [requests (vec (or (value request :requests) []))
        operations (map #(value % :operation) requests)
        files (map #(value % :file) requests)]
    {:requests (count requests)
     :files (count (distinct files))
     :operations (frequencies operations)
     :form_references
     (reduce + 0 (map #(count (or (value % :forms) [])) requests))
     :match_characters
     (reduce + 0 (map #(count (str (or (value % :match) ""))) requests))
     :expression_characters
     (reduce + 0
             (map #(count (str (or (value % :expression) ""))) requests))}))

(defn inspect-outcome-shape
  "Return inspect result metrics without paths, hashes, source, or values."
  [response]
  (cond->
    {:ok (boolean (:ok response))}
    (contains? response :read_complete)
    (assoc :read_complete (:read_complete response))
    (contains? response :request_count)
    (assoc :requests (:request_count response))
    (contains? response :file_count)
    (assoc :files (:file_count response))
    (contains? response :file_read_count)
    (assoc :file_reads (:file_read_count response))
    (contains? response :source_character_count)
    (assoc :source_characters (:source_character_count response))
    (:error_type response) (assoc :error_type (:error_type response))))

(defn inspect-call-event
  "Build mode-dependent inspect telemetry; metrics mode never includes source."
  [mode request response timings]
  (cond->
    {:tool "inspect_clojure"
     :request_shape (inspect-request-shape request)
     :outcome (inspect-outcome-shape response)
     :timings_ms timings}
    (= :full (normalize-mode mode))
    (assoc :request request :response response)))

(defn record-inspect-call!
  [state request response timings]
  (emit! state :tool.call
         (inspect-call-event (:mode state) request response timings)))
