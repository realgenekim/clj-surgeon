(ns clj-surgeon.mcp-process
  "Shared process environment for repository-owned formatter and verification commands."
  (:require
   [clj-surgeon.spawn-ledger :as spawn]
   [cheshire.core :as json]
   [clojure.java.io :as io]
   [clojure.string :as str])
  (:import
   (java.lang ProcessHandle)))

(def ^:dynamic *clj-kondo-lock-path*
  "Override only for isolated tests. The default lock is shared across projects."
  nil)

(def ^:dynamic *clj-kondo-priority-lock-path*
  "Override only for isolated tests. This turnstile gives waiting interactive
  work a scheduling point between test-mission children."
  nil)

(def ^:dynamic *analyzer-mission*
  "Internal repository-owned analyzer mission state. Never bind from request data."
  nil)

(def ^:dynamic *clj-kondo-admission-path*
  "Override only for isolated tests. The installed gate execs the analyzer."
  nil)

(def ^:dynamic *executable-path*
  "Override executable discovery only for isolated tests."
  nil)

(def ^:dynamic *clj-kondo-shim-path*
  "Override the installed direct-shell entrance only for isolated tests."
  nil)

(def ^:dynamic *clj-kondo-events-path*
  "Override analyzer launch telemetry only for isolated tests."
  nil)

(def ^:dynamic *pressure-status-path*
  "Override the host pressure snapshot only for isolated tests."
  nil)

(def ^:dynamic *maximum-normalized-load*
  "Override the red-pressure admission threshold only for isolated tests."
  nil)

(defn clj-kondo-command?
  "True when `command` directly launches the clj-kondo executable."
  [command]
  (= "clj-kondo" (some-> command first io/file .getName)))

(defn clj-kondo-lock-path
  "Return the per-user, machine-wide analyzer lock path."
  []
  (or *clj-kondo-lock-path*
      (System/getenv "CLJ_SURGEON_CLJ_KONDO_LOCK")
      (str (System/getProperty "user.home")
           "/.local/state/clj-surgeon/clj-kondo.lock")))

(defn- clj-kondo-priority-lock-path []
  (or *clj-kondo-priority-lock-path*
      (System/getenv "CLJ_SURGEON_CLJ_KONDO_PRIORITY_LOCK")
      (str (System/getProperty "user.home")
           "/.local/state/clj-surgeon/clj-kondo-priority.lock")))

(def analyzer-contract-mission-id "analyzer-contract-v1")
(def analyzer-contract-launch-limit 5)
(def analyzer-contract-duration-ms 300000)

(defn call-with-analyzer-contract-mission
  "Run one repository-owned five-launch analyzer contract mission.

  This is an internal test-runner entrance, not request or shell authority."
  [cwd scope-sha256 f]
  ;; @spec MCP-OP-ANALYZER-009
  (let [canonical-cwd (.getCanonicalPath (io/file cwd))]
    (when-not (re-matches #"[0-9a-f]{64}" scope-sha256)
      (throw (ex-info "Analyzer contract scope must be one SHA-256 digest"
                      {:error-type :invalid-analyzer-mission-scope})))
    (binding [*analyzer-mission*
              (atom {:mission-id analyzer-contract-mission-id
                     :owner-pid (.pid (ProcessHandle/current))
                     :cwd canonical-cwd
                     :scope-sha256 scope-sha256
                     :launch-limit analyzer-contract-launch-limit
                     :launches-used 0
                     :expires-epoch-ms (+ (System/currentTimeMillis)
                                          analyzer-contract-duration-ms)
                     :last-exit-epoch-ms nil})]
      (f))))

(defn- claim-analyzer-mission-launch! [command-cwd]
  (when *analyzer-mission*
    (locking *analyzer-mission*
      (let [{:keys [mission-id launches-used launch-limit
                    expires-epoch-ms] :as mission} @*analyzer-mission*
            canonical-cwd (.getCanonicalPath (io/file command-cwd))
            now (System/currentTimeMillis)]
        (when (>= now expires-epoch-ms)
          (throw (ex-info "Analyzer mission expired"
                          {:error-type :analyzer-mission-expired
                           :mission-id mission-id})))
        (when (>= launches-used launch-limit)
          (throw (ex-info "Analyzer mission launch budget exhausted"
                          {:error-type :analyzer-mission-budget-exhausted
                           :mission-id mission-id
                           :launch-limit launch-limit})))
        (let [claimed (assoc mission :launches-used (inc launches-used))]
          (reset! *analyzer-mission* claimed)
          (assoc claimed
                 :launch-index (inc launches-used)
                 :command-cwd canonical-cwd))))))

(defn- record-analyzer-mission-exit! [result]
  (when (and *analyzer-mission*
             (= :admitted (get-in result [:admission :status]))
             (:termination-confirmed result))
    (swap! *analyzer-mission* assoc
           :last-exit-epoch-ms (System/currentTimeMillis))))

;; @spec MCP-OP-ADMIT-128
(defn- extract-packaged-wrapper!
  "Copy a wrapper that ships inside a jar somewhere it can be executed."
  [url]
  (let [target (java.io.File/createTempFile "clj-kondo-admission-" ".py")]
    (.deleteOnExit target)
    (with-open [source (io/input-stream url)]
      (io/copy source target))
    (.setExecutable target true false)
    (.getCanonicalPath target)))

;; @spec MCP-OP-ADMIT-128
(def ^:private packaged-admission-wrapper
  "The wrapper this build ships, located without reading `user.dir`.

  `resources/clj-kondo-admission.py` resolved against the JVM's working
  directory is a deployment landmine: a workspace-routed server started
  anywhere but a clj-surgeon checkout resolves it to a path that does not
  exist, every admit call on every workspace it routes then reports
  `clj-kondo-unavailable`, and the receipt says nothing about why. This
  build knows where its own code was loaded from, and the wrapper ships
  beside it. Computed once, because a jar-packaged wrapper is extracted."
  (delay
    (try
      (or (when-let [url (io/resource "clj-kondo-admission.py")]
            (if (= "file" (.getProtocol url))
              (.getCanonicalPath (io/file (.toURI url)))
              (extract-packaged-wrapper! url)))
          (when-let [url (io/resource "clj_surgeon/mcp_process.clj")]
            (when (= "file" (.getProtocol url))
              ;; <root>/src/clj_surgeon/mcp_process.clj -> <root>
              (let [root (-> (io/file (.toURI url))
                             .getParentFile .getParentFile .getParentFile)
                    candidate (io/file root "resources"
                                       "clj-kondo-admission.py")]
                (when (.isFile candidate)
                  (.getCanonicalPath candidate))))))
      (catch Exception _ nil))))

;; @spec MCP-OP-ADMIT-128
(defn clj-kondo-admission-path
  "Return the installed analyzer-lifetime admission wrapper.

  Explicit configuration first -- the test binding, then the environment --
  then the wrapper installed for this user, then the one this build ships,
  and only then the working-directory-relative path, which is kept last so
  the previous behaviour remains reachable and is never the only answer."
  []
  ;; @spec MCP-OP-ANALYZER-005
  (let [installed (str (System/getProperty "user.home")
                       "/bin/clj-kondo-admission")]
    (or *clj-kondo-admission-path*
        ;; @spec MCP-OP-ADMIT-128
        ;; not-empty: an exported-but-blank override is not an override, and
        ;; resolving it would name the empty path as this server's wrapper.
        (not-empty (System/getenv "CLJ_SURGEON_CLJ_KONDO_ADMISSION"))
        (when (and (.isFile (io/file installed))
                   (.canExecute (io/file installed)))
          installed)
        @packaged-admission-wrapper
        (.getCanonicalPath (io/file "resources/clj-kondo-admission.py")))))

(defn- clj-kondo-events-path []
  (or *clj-kondo-events-path*
      (System/getenv "CLJ_SURGEON_CLJ_KONDO_EVENTS")
      (str (System/getProperty "user.home")
           "/.local/state/clj-surgeon/clj-kondo-events.jsonl")))

(defn- pressure-status-path []
  (or *pressure-status-path*
      (System/getenv "CLJ_SURGEON_PRESSURE_STATUS")
      (str (System/getProperty "user.home")
           "/.local/state/diagnose-skiff-cpu-memory/monitor/status.json")))

(defn- maximum-normalized-load []
  (or *maximum-normalized-load*
      (some-> (System/getenv "CLJ_SURGEON_CLJ_KONDO_MAX_NORMALIZED_LOAD")
              Double/parseDouble)
      4.0))

(defn resolve-executable
  "Resolve one executable without a shell. Return nil when it is unavailable.

  Canonical paths in `excluded` are skipped. This prevents the explicit gate
  from resolving the installed `~/bin/clj-kondo` gate shim as its analyzer."
  ([executable]
   (resolve-executable executable #{}))
  ([executable excluded]
   (let [file (io/file executable)
         eligible (fn [candidate]
                    (when (and (.isFile candidate) (.canExecute candidate))
                      (let [canonical (.getCanonicalPath candidate)]
                        (when-not (contains? excluded canonical)
                          canonical))))]
     (if (.isAbsolute file)
       (eligible file)
       (some (fn [directory]
               (eligible (io/file directory executable)))
             (str/split (or *executable-path*
                            (System/getenv "PATH") "")
                        (re-pattern
                          (java.util.regex.Pattern/quote
                            java.io.File/pathSeparator))))))))

(defn- resolve-clj-kondo-analyzer
  [requested gate]
  (let [shim (.getCanonicalPath
               (io/file (or *clj-kondo-shim-path*
                            (str (System/getProperty "user.home")
                                 "/bin/clj-kondo"))))
        excluded #{(.getCanonicalPath (io/file gate)) shim}
        configured (System/getenv "CLJ_SURGEON_CLJ_KONDO_REAL")]
    (or (when configured
          (resolve-executable configured excluded))
        (resolve-executable requested excluded)
        (when (= "clj-kondo" (.getName (io/file requested)))
          (resolve-executable "clj-kondo" excluded)))))

(defn- read-admission-evidence [evidence-file]
  (try
    (let [evidence (json/parse-string (slurp evidence-file) true)]
      (cond-> evidence
        (:status evidence) (update :status keyword)
        (:error_type evidence) (assoc :error-type (keyword (:error_type evidence)))))
    (catch Exception _ nil)))

(defn- prepare-admission
  [command cwd timeout-ms]
  ;; @spec MCP-OP-ANALYZER-001
  ;; @spec MCP-OP-ANALYZER-002
  (if-not (clj-kondo-command? command)
    {:command command
     :admission {:status :not-required}}
    (let [gate (clj-kondo-admission-path)
          analyzer (resolve-clj-kondo-analyzer (first command) gate)
          mission (claim-analyzer-mission-launch! cwd)
          evidence-file (java.io.File/createTempFile
                          "clj-surgeon-kondo-admission-" ".json")]
      (.delete evidence-file)
      (when-not (and (.isFile (io/file gate)) (.canExecute (io/file gate)))
        (throw (ex-info "clj-kondo admission wrapper is unavailable"
                        {:error-type :clj-kondo-admission-unavailable
                         :gate gate})))
      (when-not analyzer
        (throw (ex-info "clj-kondo executable is unavailable"
                        {:error-type :clj-kondo-executable-unavailable
                         :requested-executable (first command)})))
      {:command (into [gate
                       "--lock" (clj-kondo-lock-path)
                       "--priority-lock" (clj-kondo-priority-lock-path)
                       "--timeout-ms" (str (max 1
                                                (- timeout-ms
                                                   (min 100
                                                        (quot timeout-ms 2)))))
                       "--entrance" "clj-surgeon"
                       "--evidence" (.getCanonicalPath evidence-file)
                       "--events" (clj-kondo-events-path)
                       "--pressure-status" (pressure-status-path)
                       "--max-normalized-load" (str (maximum-normalized-load))
                       "--lane" (if mission "test-mission" "interactive")]
                      (concat
                        (when mission
                          ["--mission-id" (:mission-id mission)
                           "--mission-owner-pid" (str (:owner-pid mission))
                           "--mission-cwd" (:cwd mission)
                           "--mission-command-cwd" (:command-cwd mission)
                           "--mission-scope-sha256" (:scope-sha256 mission)
                           "--mission-launch-index" (str (:launch-index mission))
                           "--mission-launch-limit" (str (:launch-limit mission))
                           "--mission-expires-epoch-ms" (str (:expires-epoch-ms mission))])
                        (when-let [last-exit (:last-exit-epoch-ms mission)]
                          ["--mission-after-epoch-ms" (str last-exit)])
                        ["--" analyzer]
                        (rest command)))
       :evidence-file evidence-file
       :mission mission
       :admission {:status :delegated
                   :gate gate
                   :cwd (.getCanonicalPath (io/file cwd))
                   :executable analyzer}})))

(defn with-command-admission
  "Run `run!` through the analyzer-lifetime clj-kondo gate when required.

  `run!` receives the complete shared deadline and prepared admission evidence.
  The wrapper waits for the lock, then execs the analyzer with the lock file
  descriptor inherited. The analyzer therefore retains authority if its JVM
  caller dies."
  [command cwd timeout-ms run!]
  ;; @spec MCP-OP-ANALYZER-003
  (let [timeout-ms (long timeout-ms)]
    (when-not (pos? timeout-ms)
      (throw (ex-info "Process deadline must be positive"
                      {:error-type :invalid-process-deadline
                       :timeout-ms timeout-ms})))
    (let [{:keys [command evidence-file admission] :as prepared}
          (prepare-admission command cwd timeout-ms)]
      (try
        (let [result (run! timeout-ms (assoc admission :command command))
              terminal-evidence (some-> evidence-file read-admission-evidence)]
          (if (map? result)
            (let [completed (assoc result :admission
                                   (merge admission terminal-evidence))]
              (record-analyzer-mission-exit! completed)
              completed)
            result))
        (catch Exception error
          (throw (ex-info (or (.getMessage error) "Admitted command failed")
                          (merge (ex-data error)
                                 {:admission (merge admission
                                                    (some-> evidence-file
                                                            read-admission-evidence))})
                          error)))
        (finally
          (some-> (:evidence-file prepared) .delete))))))

(defn effective-path
  "Prepend the local agent tool directories while preserving the caller PATH."
  ([current]
   (effective-path (System/getProperty "user.home") current))
  ([user-home current]
   (->> (concat [(str user-home "/bin")
                 (str user-home "/.local/bin")
                 "/opt/homebrew/opt/node@20/bin"
                 "/opt/homebrew/bin"
                 "/usr/local/bin"
                 "/usr/bin"
                 "/bin"]
                (str/split (or current "")
                           (re-pattern
                             (java.util.regex.Pattern/quote
                               java.io.File/pathSeparator))))
        (remove str/blank?)
        distinct
        (str/join java.io.File/pathSeparator))))

;; @spec MCP-OP-TMPHYG-005
(defn configure-environment!
  "Give one ProcessBuilder environment the same paved local tool entrance --
   and this process's own temp directory.

   TMPDIR matters because `-Djava.io.tmpdir` is a JVM-internal property no
   child PROCESS inherits: without it a subprocess that picks its own temp
   location writes wherever the ambient environment points, outside any
   isolated per-run root and invisible to the test suites' leak witness."
  [^java.util.Map environment]
  (.put environment "PATH" (effective-path (.getOrDefault environment "PATH" "")))
  (.put environment "TMPDIR" (System/getProperty "java.io.tmpdir"))
  environment)

(defn- destroy-process-tree!
  [^Process process]
  (let [descendants (vec (.toList (.descendants (.toHandle process))))]
    (doseq [^ProcessHandle descendant (reverse descendants)]
      (when (.isAlive descendant)
        (.destroyForcibly descendant)))
    (when (.isAlive process)
      (.destroyForcibly process))
    (.waitFor process 5 java.util.concurrent.TimeUnit/SECONDS)
    (doseq [^ProcessHandle descendant descendants]
      (when (.isAlive descendant)
        (try
          (.get (.onExit descendant) 5 java.util.concurrent.TimeUnit/SECONDS)
          (catch Exception _))))
    (and (not (.isAlive process))
         (every? #(not (.isAlive ^ProcessHandle %)) descendants))))

(defn- sha256-file
  [file]
  (let [digest (java.security.MessageDigest/getInstance "SHA-256")
        buffer (byte-array 8192)]
    (with-open [input (io/input-stream file)]
      (loop []
        (let [read-count (.read input buffer)]
          (when (pos? read-count)
            (.update digest buffer 0 read-count)
            (recur)))))
    (apply str (map #(format "%02x" (bit-and % 0xff)) (.digest digest)))))

(defn- file-evidence
  [file visible-byte-limit]
  (let [byte-count (.length ^java.io.File file)
        visible-count (min byte-count (long visible-byte-limit))
        visible (byte-array (int visible-count))]
    (with-open [input (io/input-stream file)]
      (loop [offset 0]
        (when (< offset visible-count)
          (let [read-count (.read input visible offset
                                  (- (int visible-count) offset))]
            (when (pos? read-count)
              (recur (+ offset read-count)))))))
    {:text (String. visible java.nio.charset.StandardCharsets/UTF_8)
     :bytes byte-count
     :sha256 (sha256-file file)
     :truncated (> byte-count visible-count)}))

(defn run-bounded!
  "Run one command with a shared admission/execution deadline.

  Optional `:stdin-text` is staged to a file before launch. Output is captured
  to files so a verbose analyzer cannot deadlock on a full pipe. Timeout or
  interruption kills and confirms the complete child tree."
  [{:keys [command cwd timeout-ms stdin-text merge-error? visible-byte-limit
           on-start]
    :or {merge-error? false visible-byte-limit 65536}}]
  ;; @spec MCP-OP-ANALYZER-002
  (let [started (System/nanoTime)
        stdout-file (java.io.File/createTempFile "clj-surgeon-process-out-" ".log")
        stderr-file (java.io.File/createTempFile "clj-surgeon-process-err-" ".log")
        stdin-file (when (some? stdin-text)
                     (java.io.File/createTempFile "clj-surgeon-process-in-" ".txt"))]
    (try
      (when stdin-file
        (spit stdin-file stdin-text))
      (with-command-admission
        command cwd timeout-ms
        (fn [remaining-ms admission]
          (let [builder (doto (ProcessBuilder. ^java.util.List
                                (:command admission))
                          (.directory (io/file cwd))
                          (.redirectOutput stdout-file))
                _ (if merge-error?
                    (.redirectErrorStream builder true)
                    (.redirectError builder stderr-file))
                _ (when stdin-file (.redirectInput builder stdin-file))
                environment (.environment builder)
                _ (configure-environment! environment)
                process (.start builder)]
            ;; @spec TEST-ISO-002 -- record the LAUNCH, not the live child. A
            ;; short command finishes before any closing snapshot; the event
            ;; is the only thing that survives it.
            (spawn/record! (.pid process) (:command admission))
            (when on-start (on-start (.pid process)))
            (try
              (let [finished? (.waitFor process remaining-ms
                                        java.util.concurrent.TimeUnit/MILLISECONDS)
                    termination-confirmed? (or finished?
                                               (destroy-process-tree! process))
                    exit (when finished? (.exitValue process))
                    stdout (file-evidence stdout-file visible-byte-limit)
                    stderr (file-evidence stderr-file visible-byte-limit)
                    base {:finished? finished?
                          :exit exit
                          :termination-confirmed termination-confirmed?
                          :elapsed_ms (/ (double (- (System/nanoTime) started))
                                         1000000.0)
                          :admission admission}]
                (if merge-error?
                  (merge base
                         {:output (:text stdout)
                          :output-bytes (:bytes stdout)
                          :output-sha256 (:sha256 stdout)
                          :output-truncated (:truncated stdout)})
                  (merge base
                         {:out (:text stdout)
                          :out-bytes (:bytes stdout)
                          :out-sha256 (:sha256 stdout)
                          :out-truncated (:truncated stdout)
                          :err (:text stderr)
                          :err-bytes (:bytes stderr)
                          :err-sha256 (:sha256 stderr)
                          :err-truncated (:truncated stderr)})))
              (catch InterruptedException error
                (destroy-process-tree! process)
                (.interrupt (Thread/currentThread))
                (throw (ex-info "Bounded process was interrupted"
                                {:error-type :process-interrupted}
                                error)))
              (catch Exception error
                (destroy-process-tree! process)
                (throw error))))))
      (finally
        (.delete stdout-file)
        (.delete stderr-file)
        (some-> stdin-file .delete)))))
