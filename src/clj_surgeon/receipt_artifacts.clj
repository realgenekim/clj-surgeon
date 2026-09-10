(ns clj-surgeon.receipt-artifacts
  "External verb bookkeeping and measured post-write workspace evidence."
  (:require
   [clojure.java.io :as io]
   [clojure.java.shell :as shell]
   [clojure.string :as str])
  (:import
   (java.nio.file Files)
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
   durable per-user state (see clj-surgeon.mcp-process). This function only
   SELECTS a candidate; it does not validate it -- `validate-artifact-root!`
   is the one place that decides whether a selection is safe to write to,
   called by every writer (`directory`) before it trusts the value."
  []
  (or (System/getenv "CLJ_SURGEON_ARTIFACT_ROOT")
      (some-> (System/getenv "XDG_STATE_HOME")
              (str "/clj-surgeon/artifacts"))
      (str (System/getProperty "user.home") "/.local/state/clj-surgeon/artifacts")))

(def ^:dynamic *artifact-root* (default-artifact-root))

(defn- nearest-existing-ancestor
  "Walk up from `path` to the first ancestor that exists, or nil if none does
   (should not happen once the filesystem root is reached)."
  [path]
  (loop [f (io/file (str path))]
    (cond
      (nil? f) nil
      (.exists f) f
      :else (recur (.getParentFile f)))))

(defn- owned-by-invoking-user?
  "True when the NEAREST EXISTING ANCESTOR of `path` is owned by the invoking
   user, checked by real filesystem ownership rather than by a path-string
   convention. This is what lets an explicit CLJ_SURGEON_ARTIFACT_ROOT like
   /var/tmp/forge -- a scratch directory THIS invocation created (or already
   owns) under a shared sticky-bit base -- pass, while /home/someone-else/...
   fails: mkdirs cannot create it, its nearest existing ancestor belongs to
   someone else, and the check is honest about that regardless of the
   literal path text."
  [path]
  (when-let [ancestor (nearest-existing-ancestor path)]
    (try
      (= (System/getProperty "user.name")
         (str (Files/getOwner (.toPath ancestor) (make-array java.nio.file.LinkOption 0))))
      (catch Exception _ false))))

(defn- under-home? [path]
  (let [home (System/getProperty "user.home")]
    (or (= path home) (str/starts-with? path (str home java.io.File/separator)))))

;; @spec ALIAS-MIGRATION-001
;; NOTE ON DUPLICATION: `clj-surgeon.tmp-leak-support/base-refusal` already
;; answers this question, more thoroughly (darwin's `mount(8)`, a mounts-table
;; fallback, a seam for tests). It lives under test/, and the bb CLI's actual
;; classpath for this entrance (clj-surgeon.core, loaded by an installed
;; `clj-surgeon` script) does not include test/ -- confirmed the hard way:
;; requiring it from here made `bb -m clj-surgeon.core` fail to load AT ALL,
;; for every verb, not only receipt writes. This is the minimal, self-
;; contained subset (literal /tmp or /dev/shm, or findmnt reporting tmpfs)
;; that a writer reachable from the bb entrance can depend on.
(defn- ram-backed-path?
  [path]
  (let [p (str path)]
    (or (some #(or (= p %) (str/starts-with? p (str % "/"))) ["/tmp" "/dev/shm"])
        (= "tmpfs"
           (try
             (let [{:keys [exit out]} (shell/sh "findmnt" "-n" "-o" "FSTYPE" "--target" p)]
               (when (and (zero? exit) (seq (str/trim out))) (str/trim out)))
             (catch Throwable _ nil))))))

;; @spec ALIAS-MIGRATION-001
(defn validate-artifact-root!
  "Fails CLOSED on the resolved artifact root before any receipt writer
   trusts it (SPF-001: `default-artifact-root` accepted CLJ_SURGEON_ARTIFACT_ROOT
   and XDG_STATE_HOME unvalidated -- blank, relative, RAM-backed and another
   user's path were all silently accepted). Returns `root` unchanged when it
   passes every check; otherwise throws ex-info with a typed :error-type and
   the offending :root, naming the two variables that could have produced it.

   Checks, in order:
     1. non-blank
     2. absolute
     3. (after best-effort mkdirs, so an explicit override under a shared
        sticky-bit base like /var/tmp/forge is judged by what it actually
        resolves to) owned by the invoking user, or under $HOME -- trivially
        safe even before it exists
     4. real disk, never RAM-backed (literal /tmp or /dev/shm, or a tmpfs
        mount by `findmnt`) -- see `ram-backed-path?`'s note on why this
        duplicates rather than requires `clj-surgeon.tmp-leak-support`

   This retains a controlled external override (CLJ_SURGEON_ARTIFACT_ROOT
   pointed at a real-disk path this invocation owns, e.g. the parity harness
   case /var/tmp/forge) while refusing every uncontrolled widening the
   fence found."
  [root]
  (let [root (str root)
        checked-vars ["CLJ_SURGEON_ARTIFACT_ROOT" "XDG_STATE_HOME"]]
    (cond
      (str/blank? root)
      (throw (ex-info "Artifact root is blank"
                       {:error-type :artifact-root-blank :root root :checked-vars checked-vars}))

      (not (.isAbsolute (io/file root)))
      (throw (ex-info "Artifact root is not an absolute path"
                       {:error-type :artifact-root-relative :root root :checked-vars checked-vars}))

      :else
      (do
        (try (.mkdirs (io/file root)) (catch Throwable _ nil))
        (cond
          (not (or (under-home? root) (owned-by-invoking-user? root)))
          (throw (ex-info "Artifact root is outside $HOME and not owned by the invoking user"
                           {:error-type :artifact-root-not-owned :root root :checked-vars checked-vars}))

          (ram-backed-path? root)
          (throw (ex-info "Artifact root is RAM-backed (/tmp, /dev/shm, or a tmpfs mount)"
                           {:error-type :artifact-root-ram-backed :root root :checked-vars checked-vars}))

          :else root)))))

;; @spec ALIAS-MIGRATION-001
(defn directory [verb workspace]
  (validate-artifact-root! *artifact-root*)
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
