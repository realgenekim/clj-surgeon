(ns clj-surgeon.failure-report
  "Privacy-safe, deduplicating local failure reports."
  (:require
   [cheshire.core :as json]
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.string :as str])
  (:import
   (java.security MessageDigest)))

(def allowed-fields
  [:operation :phase :terminal-state :error-type :agent-session-restart-required
   :next-action])

(defn sanitized-failure
  "Return only diagnostic fields that cannot contain source or local paths."
  [receipt]
  (into (sorted-map) (select-keys receipt allowed-fields)))

(defn- sha256
  [value]
  (let [digest (.digest (MessageDigest/getInstance "SHA-256")
                        (.getBytes (str value) "UTF-8"))]
    (apply str (map #(format "%02x" (bit-and % 0xff)) digest))))

(defn failure-fingerprint
  [receipt]
  (subs (sha256 (pr-str (sanitized-failure receipt))) 0 20))

(defn issue-draft
  [receipt]
  (let [sanitized (sanitized-failure receipt)
        fingerprint (failure-fingerprint receipt)
        error-type (or (:error-type sanitized) :unknown-recovery-failure)]
    {:title (str "Recovery failure: " (name error-type))
     :priority "P1"
     :type "bug"
     :fingerprint fingerprint
     :description
     (str "A bounded clj-surgeon recovery attempt failed.\n\n"
          "Redacted receipt:\n\n```edn\n" (pr-str sanitized) "\n```\n\n"
          "Failure fingerprint: `" fingerprint "`.\n\n"
          "No source, prompts, URLs, or workspace paths are retained.")}))

(defn- default-tool-root
  []
  (let [root-file (some-> (System/getenv
                            "CLJ_SURGEON_CONTROL_PLANE_ROOT_FILE")
                          io/file)
        installed-root (when (and root-file (.isFile root-file))
                         (str/trim (slurp root-file)))
        cwd (io/file (System/getProperty "user.dir"))]
    (cond
      (seq installed-root) (io/file installed-root)
      (.isFile (io/file cwd "src" "clj_surgeon" "core.clj")) cwd
      :else nil)))

(defn run-captured!
  [directory command]
  (let [process (-> (ProcessBuilder. (into-array String command))
                    (.directory (io/file directory))
                    (.redirectErrorStream true)
                    .start)
        output (slurp (.getInputStream process))
        exit (.waitFor process)]
    {:command command :exit exit :out output}))

(defn- require-success
  [result]
  (when-not (zero? (:exit result))
    (throw (ex-info "Local failure report command failed"
                    {:error-type :failure-report-command-failed
                     :exit (:exit result)})))
  result)

(defn report-failure!
  "Create or append to one local Bead. Off-laptop, return a safe draft."
  [{:keys [receipt receipt-file tool-root runner]}]
  (let [receipt (or receipt
                    (some-> receipt-file slurp edn/read-string))
        tool-root (some-> (or tool-root (default-tool-root))
                          io/file
                          .getCanonicalFile)
        runner (or runner run-captured!)
        draft (issue-draft receipt)
        beads-dir (when tool-root (io/file tool-root ".beads"))]
    (when-not (map? receipt)
      (throw (ex-info "A readable EDN recovery receipt is required"
                      {:error-type :invalid-failure-receipt})))
    (if-not (and beads-dir (.isDirectory beads-dir))
      {:ok true
       :operation :clj-surgeon-report-failure
       :reported false
       :reason :local-beads-unavailable
       :issue-draft draft
       :next-action :file-issue-draft-or-use-cli-fallback}
      (let [fingerprint (:fingerprint draft)
            list-command ["bd" "--directory" (.getPath tool-root)
                          "list" "--all" "--json"
                          "--metadata-field"
                          (str "failure_fingerprint=" fingerprint)]
            existing-result (require-success
                              (runner (.getPath tool-root) list-command))
            existing (json/parse-string (:out existing-result) true)
            issue-id (:id (first existing))
            note (str "Repeated redacted recovery failure `" fingerprint "`.")
            result
            (if issue-id
              (require-success
                (runner (.getPath tool-root)
                        ["bd" "--directory" (.getPath tool-root)
                         "update" issue-id "--append-notes" note "--json"]))
              (require-success
                (runner (.getPath tool-root)
                        ["bd" "--directory" (.getPath tool-root)
                         "create" "--type" (:type draft)
                         "--priority" (:priority draft)
                         "--title" (:title draft)
                         "--description" (:description draft)
                         "--metadata"
                         (json/generate-string
                           {:failure_fingerprint fingerprint})
                         "--json"])))]
        {:ok true
         :operation :clj-surgeon-report-failure
         :reported true
         :deduplicated (boolean issue-id)
         :issue-id (or issue-id
                       (:id (json/parse-string (:out result) true)))
         :fingerprint fingerprint
         :next-action :none}))))
