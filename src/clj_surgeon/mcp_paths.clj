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

;; @spec MCP-OP-STUDY-014
(defn real-path-within
  "Return an absolute path's canonical realpath when it stays inside canonical
  root, and nil otherwise.

  The filesystem half of DISCOVERY confinement — the same resolve-then-compare
  `resolve-source-path` already makes for one caller-named file, applied to a
  path a directory walk produced. `find` reports a symlink by the LINK's own
  name, so `src/leak.clj -> /etc/passwd` matches `-name '*.clj'` and can only
  be recognised as an escape after resolution. A path that does not exist or
  cannot be resolved is not within the root.

  Adds no confinement policy and relaxes no existing check."
  ^Path [^Path root path]
  (try
    (let [real (.toRealPath (Paths/get (str path) (make-array String 0))
                            (make-array LinkOption 0))]
      (when (.startsWith real root) real))
    (catch Exception _ nil)))

;; @spec MCP-OP-STUDY-014
(defn normalized-path-within
  "Return `base` resolved against `child` and lexically normalized, when the
  result stays inside canonical root; nil otherwise.

  The lexical half of discovery confinement, for a path a BUILD FILE declared
  rather than one a caller named: `:paths [\"../../..\"]` in a scanned
  deps.edn must not move the scan outside the root. Performs no I/O — pair it
  with `real-path-within` on whatever the walk then finds."
  ^Path [^Path root ^Path base child]
  (try
    (let [normalized (.normalize (.resolve base (str child)))]
      (when (.startsWith normalized root) normalized))
    (catch Exception _ nil)))

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

(defn relative-directory-path?
  "True for a portable project-relative directory path.

  The lexical half of directory confinement; performs no filesystem I/O.
  \".\" names the project root itself."
  [value]
  (when (string? value)
    (let [portable (str/replace value "\\" "/")]
      (or (= "." portable)
          (and (not (str/blank? portable))
               (not (str/starts-with? portable "/"))
               (not (re-find #"(?i)^[a-z]:/" portable))
               (not (str/includes? portable "\u0000"))
               (every? #(and (not (str/blank? %))
                             (not (#{"." ".."} %)))
                       (str/split portable #"/" -1)))))))

;; @spec MCP-OP-STUDY-006
(defn resolve-directory-path
  "Resolve one lexically valid relative directory inside canonical root.

  Mirrors `resolve-source-path` exactly: symlinks may point within the root,
  but escapes and non-directories refuse. Adds no new confinement policy."
  [^Path root relative]
  (if-not (relative-directory-path? relative)
    (path-refusal
      :invalid-relative-directory-path
      "Expected a project-relative directory path without parent traversal"
      relative)
    (try
      (let [lexical (.normalize (.resolve root relative))]
        (if-not (.startsWith lexical root)
          (path-refusal :path-outside-project
                        "Directory path escapes the configured project root"
                        relative)
          (let [real (.toRealPath lexical (make-array LinkOption 0))]
            (cond
              (not (.startsWith real root))
              (path-refusal :path-outside-project
                            "Directory symlink resolves outside the configured project root"
                            relative)

              (not (Files/isDirectory real (make-array LinkOption 0)))
              (path-refusal :path-not-directory
                            "Path is not a directory"
                            relative)

              :else
              {:ok true
               :relative relative
               :path (.toString real)
               :canonical real}))))
      (catch java.nio.file.NoSuchFileException _
        (path-refusal :directory-not-found "Directory does not exist" relative))
      (catch Exception error
        (path-refusal :invalid-directory-path (.getMessage error) relative)))))
