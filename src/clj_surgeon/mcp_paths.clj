(ns clj-surgeon.mcp-paths
  "Shared project-root confinement for MCP source tools."
  (:require
   [clojure.string :as str])
  (:import
   (java.nio.file Files LinkOption Path Paths)))

(def supported-source-extensions #{"clj" "cljs" "cljc"})

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
      "Expected a project-relative .clj, .cljs, or .cljc path without parent traversal"
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
