(ns clj-surgeon.mcp-paths
  "Shared project-root confinement for MCP source tools."
  (:require
   [clojure.string :as str])
  (:import
   (java.nio.file Files LinkOption Path Paths)))

(def supported-source-extensions #{"clj" "cljs" "cljc" "edn"})

(defn relative-source-path?
  "True for a portable project-relative supported Clojure source path.

  This is the lexical half of confinement and performs no filesystem I/O."
  [value]
  (when (string? value)
    (let [portable (str/replace value "\\" "/")
          segments (str/split portable #"/" -1)
          extension (some-> portable (str/split #"\.") last)]
      (and (not (str/blank? portable))
           (not (str/starts-with? portable "/"))
           (not (re-find #"(?i)^[a-z]:/" portable))
           (not (str/includes? portable "\u0000"))
           (every? #(and (not (str/blank? %))
                         (not (#{"." ".."} %)))
                   segments)
           (contains? supported-source-extensions extension)))))

(defn real-root
  "Return the canonical configured project root. Performs filesystem I/O."
  ^Path [root]
  (.toRealPath
    (Paths/get (str root) (make-array String 0))
    (make-array LinkOption 0)))

(defn path-refusal
  [error-type message path]
  {:ok false
   :error_type (name error-type)
   :error message
   :path path
   :source_unchanged true
   :remedy "Use an existing project-relative source path inside the configured project root."})

(defn resolve-source-path
  "Resolve one lexically valid relative source path inside canonical root.

  Symlinks may point within the root, but escapes and non-regular files refuse."
  [^Path root relative]
  (if-not (relative-source-path? relative)
    (path-refusal
      :invalid-relative-source-path
      "Expected a project-relative .clj, .cljs, .cljc, or .edn path without parent traversal"
      relative)
    (try
      (let [lexical (.normalize (.resolve root relative))]
        (if-not (.startsWith lexical root)
          (path-refusal :path-outside-project
                        "Source path escapes the configured project root"
                        relative)
          (let [real (.toRealPath lexical (make-array LinkOption 0))]
            (cond
              (not (.startsWith real root))
              (path-refusal :path-outside-project
                            "Source symlink resolves outside the configured project root"
                            relative)

              (not (Files/isRegularFile real (make-array LinkOption 0)))
              (path-refusal :source-not-regular-file
                            "Source path is not a regular file"
                            relative)

              :else
              {:ok true
               :relative relative
               :path (.toString real)
               :canonical real}))))
      (catch java.nio.file.NoSuchFileException _
        (path-refusal :source-file-not-found "Source file does not exist" relative))
      (catch Exception error
        (path-refusal :invalid-source-path (.getMessage error) relative)))))

(defn resolve-new-source-path
  "Resolve one absent source path below a real project-confined ancestor.

  The destination itself must not exist. Missing parent directories are
  returned shallowest-first without being created, so the write transaction
  can create and roll them back under the same guards as the new file."
  [^Path root relative]
  (if-not (relative-source-path? relative)
    (path-refusal
      :invalid-relative-source-path
      "Expected a project-relative .clj, .cljs, .cljc, or .edn path without parent traversal"
      relative)
    (try
      (let [lexical (.normalize (.resolve root relative))
            parent (.getParent lexical)]
        (cond
          (not (.startsWith lexical root))
          (path-refusal :path-outside-project
                        "Target path escapes the configured project root"
                        relative)

          (Files/exists lexical (make-array LinkOption 0))
          (path-refusal :target-already-exists
                        "Extraction target already exists"
                        relative)

          (nil? parent)
          (path-refusal :invalid-target-path
                        "Extraction target has no parent"
                        relative)

          :else
          (let [[existing-parent missing-parents]
                (loop [candidate parent
                       missing ()]
                  (if (Files/exists candidate (make-array LinkOption 0))
                    [candidate (vec missing)]
                    (recur (.getParent candidate) (conj missing candidate))))
                real-parent (.toRealPath existing-parent
                                         (make-array LinkOption 0))]
            (cond
              (not (.startsWith real-parent root))
              (path-refusal :path-outside-project
                            "Target ancestor symlink resolves outside the configured project root"
                            relative)

              (not (Files/isDirectory real-parent (make-array LinkOption 0)))
              (path-refusal :target-parent-not-directory
                            "Extraction target ancestor is not a directory"
                            relative)

              :else
              (let [[canonical-parent canonical-missing]
                    (reduce
                      (fn [[^Path base paths] ^Path missing]
                        (let [path (.resolve base (.getFileName missing))]
                          [path (conj paths path)]))
                      [real-parent []]
                      missing-parents)
                    canonical (.resolve canonical-parent
                                        (.getFileName lexical))]
                {:ok true
                 :relative relative
                 :path (.toString canonical)
                 :canonical canonical
                 :missing-parent-directories canonical-missing})))))
      (catch Exception error
        (path-refusal :invalid-target-path (.getMessage error) relative)))))

;; @spec MCP-OP-EXTRACT-024
(defn resolve-discovered-source-path
  "Confine one path a filesystem WALK produced -- absolute or process-relative --
  to the canonical project root.

  A walk hands back locations, not the portable project-relative strings
  `resolve-source-path` admits, so the location is first canonicalized through
  its parent directory -- the component a rename/replace primitive follows -- and
  then confined by exactly that same gate. One rule, two entry points: a
  discovered path and a caller-supplied one cannot be admitted by different
  reasoning."
  [^Path root path]
  (try
    (let [lexical (.normalize
                    (.toAbsolutePath
                      (Paths/get (str path) (make-array String 0))))
          parent (.getParent lexical)
          file-name (.getFileName lexical)]
      (if (or (nil? parent) (nil? file-name))
        (path-refusal :invalid-source-path
                      "Discovered path has no parent directory"
                      (str path))
        (let [real-parent (.toRealPath parent (make-array LinkOption 0))]
          (if-not (.startsWith real-parent root)
            (path-refusal
              :path-outside-project
              "Discovered path's directory resolves outside the configured project root"
              (str path))
            (resolve-source-path
              root
              (.toString (.relativize root (.resolve real-parent file-name))))))))
    (catch java.nio.file.NoSuchFileException _
      (path-refusal :source-file-not-found "Source file does not exist" (str path)))
    (catch Exception error
      (path-refusal :invalid-source-path (.getMessage error) (str path)))))
