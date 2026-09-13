(ns clj-surgeon.probe-state
  "Shared warm-image descriptor location and admitted publication."
  (:require
   [clj-surgeon.receipt-artifacts :as artifacts]
   [clojure.java.io :as io]
   [clojure.string :as str])
  (:import
   (java.nio.file Files StandardCopyOption)
   (java.nio.file.attribute FileAttribute)
   (java.security MessageDigest)))

;; @spec STATE-HOME-002
(defn state-root [env home]
  (or (not-empty (get env "CLJ_SURGEON_STATE_HOME"))
      (some-> (not-empty (get env "XDG_STATE_HOME")) (io/file "clj-surgeon") str)
      (str (io/file (or (not-empty home) ".") ".local" "state" "clj-surgeon"))))

;; @spec STATE-HOME-003
(defn workspace-key [canonical-root]
  (apply str (map #(format "%02x" (bit-and 255 %))
                  (.digest (MessageDigest/getInstance "SHA-256")
                           (.getBytes ^String canonical-root "UTF-8")))))

;; @spec STATE-HOME-002
;; @spec STATE-HOME-003
;; @spec STATE-HOME-004
(defn image-file
  ([workspace override]
   (image-file workspace override (System/getenv) (System/getProperty "user.home")))
  ([workspace override env home]
   (str (artifacts/resolved-target
          (if override (io/file override)
            (io/file (state-root env home) "workspaces"
                     (workspace-key (.getCanonicalPath (io/file workspace))) "probe.edn"))))))

(defn native-failure [error path]
  (let [native-class (.getName (class error))
        message (.getMessage error)]
    {:path (str path)
     :errno
     (or
       (get
         {"java.nio.file.AccessDeniedException" "EACCES"
          "java.io.InterruptedIOException" "EINTR"
          "java.nio.channels.ClosedByInterruptException" "EINTR"
          "java.nio.file.FileAlreadyExistsException" "EEXIST"
          "java.nio.file.NoSuchFileException" "ENOENT"
          "java.nio.file.NotDirectoryException" "ENOTDIR"}
         native-class)
       (some
         (fn [[text errno]]
           (when (str/includes? (or message "") text) errno))
         [["Read-only file system" "EROFS"]
          ["File too large" "EFBIG"]
          ["Interrupted system call" "EINTR"]
          ["Not a directory" "ENOTDIR"]
          ["Is a directory" "EISDIR"]
          ["No space left" "ENOSPC"]
          ["Disk quota exceeded" "EDQUOT"]
          ["Permission denied" "EACCES"]])
       :unavailable)
     :native-class native-class
     :native-message message}))

;; @spec STATE-HOME-011
(defn publish-stage!
  "One rebindable I/O seam; publication is the final forward operation.
   ATOMIC_MOVE preserves the prior name if publication fails. No restore race."
  [stage temporary target bytes]
  (case stage
    :cleanup (Files/deleteIfExists temporary)
    :create (Files/createFile temporary (make-array FileAttribute 0))
    :write (with-open [out (java.io.FileOutputStream. (.toFile temporary))]
             (.write out ^bytes bytes))
    :sync (with-open [out (java.io.FileOutputStream. (.toFile temporary) true)]
            (.sync (.getFD out)))
    :publish (Files/move temporary target
                         (into-array java.nio.file.CopyOption [StandardCopyOption/ATOMIC_MOVE]))))

;; @spec STATE-HOME-001
;; @spec STATE-HOME-007
;; @spec STATE-HOME-008
;; @spec STATE-HOME-011
;; @spec STATE-HOME-012
(defn write-image! [path descriptor]
  ;; Admission is outside the I/O catch: an envelope refusal is never relabeled
  ;; as a permissions error, and mkdir cannot happen before destination admission.
  (let [target (io/file (artifacts/admit-target! path :probe-image))
        temporary (.toPath (io/file (.getParentFile target) (str ".probe-" (random-uuid) ".tmp")))]
    (try
      (Files/createDirectories (.toPath (.getParentFile target)) (make-array FileAttribute 0))
      (let [bytes (.getBytes (str (pr-str descriptor) "\n") "UTF-8")]
        (publish-stage! :create temporary (.toPath target) bytes)
        (publish-stage! :write temporary (.toPath target) bytes)
        (when-not (= (alength bytes) (Files/size temporary))
          (throw (java.io.IOException. "Short write")))
        (publish-stage! :sync temporary (.toPath target) bytes)
        (publish-stage! :publish temporary (.toPath target) bytes))
      (str target)
      (catch Exception error
        ;; A stage can perform its side effect and then throw. Cleanup must not
        ;; depend on create returning, nor mask the original refusal if it fails.
        (let [cleanup-failure (try
                                (publish-stage! :cleanup temporary (.toPath target) nil)
                                nil
                                (catch Exception cleanup
                                  (native-failure cleanup temporary)))]
          (if (instance? java.io.IOException error)
            (throw (ex-info "Warm image state destination is not writable"
                            (cond-> (assoc (native-failure error path)
                                           :error-type :probe-state-not-writable)
                              cleanup-failure (assoc :cleanup-failure cleanup-failure))
                            error))
            (if cleanup-failure
              (throw (ex-info (.getMessage error)
                              (assoc (ex-data error) :cleanup-failure cleanup-failure) error))
              (throw error))))))))

;; @spec STATE-HOME-009
;; @spec STATE-HOME-010
(defn admit-state-root! [workspace requested]
  (let [resolved (artifacts/resolved-target requested)
        checkout (artifacts/resolved-target workspace)
        facts {:path (str requested) :canonical-path (str resolved)}]
    (when (.startsWith resolved checkout)
      (throw (ex-info "Warm state resolves inside the workspace"
                      (assoc facts :error-type :state-root-inside-workspace))))
    (try
      (artifacts/admit-target! requested :probe-image)
      (catch clojure.lang.ExceptionInfo e
        (if (= :write-outside-envelope (:error-type (ex-data e)))
          (throw (ex-info "Warm state is outside the pre-existing destination envelope"
                          (assoc facts :error-type :state-root-outside-envelope) e))
          (throw e))))
    (str resolved)))

;; @spec STATE-HOME-001
;; @spec STATE-HOME-002
;; @spec STATE-HOME-009
;; @spec STATE-HOME-010
(defn warm! [opts]
  (let [workspace (or (:project-dir opts) (System/getProperty "user.dir"))
        root (state-root (System/getenv) (System/getProperty "user.home"))
        envelope (or (:destination-envelope opts) (artifacts/current-envelope))]
    (try
      (binding [artifacts/*destination-envelope* envelope]
        (let [canonical-root (admit-state-root! workspace root)
              path (image-file workspace (:probe-image-file opts))]
          ;; An admitted root does not prove redirected descendants or overrides.
          (admit-state-root! workspace path)
          ;; Materialize an admitted dangling home/state link before server
          ;; startup's other state consumers traverse the configured spelling.
          (try
            (Files/createDirectories (.toPath (io/file canonical-root)) (make-array FileAttribute 0))
            (catch java.io.IOException e
              (throw (ex-info "Warm image state destination is not writable"
                              (assoc (native-failure e path) :error-type :probe-state-not-writable) e))))
          ((requiring-resolve 'clj-surgeon.mcp-http-server/start)
           (assoc opts :probe-image-file path :destination-envelope envelope))))
      (catch Exception error
        (binding [*out* *err*]
          (prn (merge {:state :probe-refused :error (.getMessage error)}
                      (ex-data error))))
        (throw error)))))
