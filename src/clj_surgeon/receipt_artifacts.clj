(ns clj-surgeon.receipt-artifacts
  "External verb bookkeeping and measured post-write workspace evidence."
  (:require
   [clojure.java.io :as io]
   [clojure.java.shell :as shell]
   [clojure.string :as str])
  (:import
   (java.security MessageDigest)))

(defn default-artifact-root
  "Where external verb receipts and undo artifacts live, by default.

   These artifacts must outlive the workspace and must NOT live inside it, so
   the root is deliberately outside the repository. It must also be private to
   the invoking user: a fixed shared directory is either unwritable on a normal
   machine or, worse, writable by everyone on a shared one.

   Resolution order, first that is set:
     CLJ_SURGEON_ARTIFACT_ROOT   an explicit choice always wins
     $XDG_STATE_HOME/clj-surgeon/artifacts
     $HOME/.local/state/clj-surgeon/artifacts

   The last form is the convention the rest of this tool already uses for
   durable per-user state (see clj-surgeon.mcp-process)."
  []
  (or (System/getenv "CLJ_SURGEON_ARTIFACT_ROOT")
      (some-> (System/getenv "XDG_STATE_HOME")
              (str "/clj-surgeon/artifacts"))
      (str (System/getProperty "user.home") "/.local/state/clj-surgeon/artifacts")))

(def ^:dynamic *artifact-root* (default-artifact-root))

;; @spec ALIAS-MIGRATION-001
(defn directory [verb workspace]
  (let [root (.getCanonicalFile (io/file (str workspace)))
        digest (.digest (MessageDigest/getInstance "SHA-256") (.getBytes (str root) "UTF-8"))
        identity (apply str (map #(format "%02x" (bit-and 255 %)) digest))
        dir (.getCanonicalFile (io/file *artifact-root* (str verb "-receipts") identity))]
    (when (.startsWith (.toPath dir) (.toPath root))
      (throw (ex-info "Receipt directory resolves inside the workspace"
                      {:error-type :receipt-dir-inside-workspace :receipt-dir (str dir)})))
    (str dir)))

;; @spec ALIAS-MIGRATION-001
(defn target [verb workspace relative]
  (let [base (.toPath (io/file (directory verb workspace)))
        file (.getCanonicalFile (io/file (str base) relative))]
    (when-not (.startsWith (.toPath file) base)
      (throw (ex-info "Artifact descendant escapes its receipt directory"
                      {:error-type :receipt-dir-escapes :path (str file)})))
    (str file)))

;; @spec ALIAS-MIGRATION-002
(defn porcelain-paths
  "Parse Git's NUL porcelain format, including both sides of renames/copies."
  [output]
  (loop [records (seq (str/split output #"\u0000")) paths #{}]
    (if-let [record (first records)]
      (if (str/blank? record)
        (recur (next records) paths)
        (let [rename? (some #{\R \C} (take 2 record))
              paths (conj paths (subs record 3))]
          (recur (if rename? (nnext records) (next records))
                 (if rename? (conj paths (second records)) paths))))
      paths)))

(defn- git-root [root]
  (loop [file (.getAbsoluteFile (io/file (str root)))]
    (cond (nil? file) nil
          (.exists (io/file file ".git")) file
          :else (recur (.getParentFile file)))))

;; @spec ALIAS-MIGRATION-002
(defn workspace-evidence [root changed]
  (let [root-path (.toPath (.getCanonicalFile (io/file (str root))))
        relative (fn [file]
                   (let [path (.toPath (io/file (str file)))]
                     (str (if (.isAbsolute path) (.relativize root-path path) path))))
        allowed (set (map relative changed))
        command ["git" "-C" (str root) "status" "--porcelain" "--untracked-files=all" "-z"]]
    (try
      (let [repository (git-root root)
            result (if repository (apply shell/sh command) {:exit 128 :err "Not a Git workspace"})
            unexpected (when (zero? (:exit result))
                         (vec (sort (remove allowed
                                            (map #(relative (io/file repository %))
                                                 (porcelain-paths (:out result)))))))
            proven? (and (zero? (:exit result)) (empty? unexpected))]
        (cond-> {:workspace_status {:checked true :command command :exit (:exit result)
                                    :clean_except_proven proven? :unexpected_paths unexpected}}
          proven? (assoc :workspace_clean_except (vec (sort allowed)))
          (not (zero? (:exit result))) (assoc-in [:workspace_status :error] (:err result))))
      (catch Exception error
        {:workspace_status {:checked false :clean_except_proven false :command command
                            :error (.getMessage error)}}))))
