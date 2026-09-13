(ns clj-surgeon.probe-state
  "Shared warm-image descriptor location and admitted publication."
  (:require
   [clj-surgeon.receipt-artifacts :as artifacts]
   [clojure.java.io :as io]
   [clojure.string :as str])
  (:import
   (java.nio.file Files)
   (java.nio.file.attribute FileAttribute)
   (java.security MessageDigest)))

;; @spec STATE-HOME-002
(defn state-root [env home]
  (or (not-empty (get env "CLJ_SURGEON_STATE_HOME"))
      (some-> (not-empty (get env "XDG_STATE_HOME")) (io/file "clj-surgeon") str)
      (str (io/file home ".local" "state" "clj-surgeon"))))

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
   (.getCanonicalPath
     (if override (io/file override)
       (io/file (state-root env home) "workspaces"
                (workspace-key (.getCanonicalPath (io/file workspace))) "probe.edn")))))

(defn native-failure [error path]
  (let [native-class (.getName (class error))
        message (.getMessage error)]
    {:path (str path)
     :errno
     (or
       (get
         {"java.nio.file.AccessDeniedException" "EACCES"
          "java.nio.file.FileAlreadyExistsException" "EEXIST"
          "java.nio.file.NoSuchFileException" "ENOENT"
          "java.nio.file.NotDirectoryException" "ENOTDIR"}
         native-class)
       (some
         (fn [[text errno]]
           (when (str/includes? (or message "") text) errno))
         [["Read-only file system" "EROFS"]
          ["Not a directory" "ENOTDIR"]
          ["Is a directory" "EISDIR"]
          ["No space left" "ENOSPC"]
          ["Disk quota exceeded" "EDQUOT"]
          ["Permission denied" "EACCES"]])
       :unavailable)
     :native-class native-class
     :native-message message}))

;; @spec STATE-HOME-001
;; @spec STATE-HOME-007
;; @spec STATE-HOME-008
(defn write-image! [path descriptor]
  ;; Admission is outside the I/O catch: an envelope refusal is never relabeled
  ;; as a permissions error, and mkdir cannot happen before destination admission.
  (let [target (io/file (artifacts/admit-target! path :probe-image))]
    (try
      (Files/createDirectories (.toPath (.getParentFile target)) (make-array FileAttribute 0))
      (Files/write (.toPath target) (.getBytes (str (pr-str descriptor) "\n") "UTF-8") (make-array java.nio.file.OpenOption 0))
      (str target)
      (catch java.io.IOException error
        (throw (ex-info "Warm image state destination is not writable"
                        (assoc (native-failure error path)
                               :error-type :probe-state-not-writable)
                        error))))))

;; @spec STATE-HOME-001
;; @spec STATE-HOME-002
(defn warm! [opts]
  (let [workspace (or
                    (:project-dir opts)
                    (System/getProperty "user.dir"))
        path (image-file workspace (:probe-image-file opts))
        root (state-root
               (System/getenv)
               (System/getProperty "user.home"))
        envelope (or
                   (:destination-envelope opts)
                   (artifacts/destination-envelope
                     (conj (:roots (artifacts/current-envelope)) root)
                     :launcher))]
    (try
      ((requiring-resolve 'clj-surgeon.mcp-http-server/start)
       (assoc
         opts
         :probe-image-file
         path
         :destination-envelope
         envelope))
      (catch
        Exception
        error
        (binding [*out* *err*]
          (prn
            (merge
              {:state :probe-refused
               :image-file path
               :error (.getMessage error)}
              (ex-data error))))
        (throw error)))))
