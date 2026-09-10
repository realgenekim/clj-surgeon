(ns clj-surgeon.receipt-artifacts
  "External verb bookkeeping and measured post-write workspace evidence."
  (:require
   [clj-surgeon.path-classification :as pc]
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

(defn- owned-by-invoking-user?
  "True when the NEAREST EXISTING ANCESTOR of `path` is owned by the invoking
   user, checked by real filesystem ownership rather than by a path-string
   convention. This is what lets an explicit CLJ_SURGEON_ARTIFACT_ROOT like
   /var/tmp/forge -- a scratch directory THIS invocation already owns under a
   shared sticky-bit base -- pass, while /home/someone-else/... fails: its
   nearest existing ancestor belongs to someone else, and the check is
   honest about that regardless of the literal path text. NEVER creates
   anything (SPF-004: the previous version mkdirs'd the candidate before
   this check ran, so a refused root was created and left behind)."
  [path]
  (when-let [ancestor (pc/nearest-existing-ancestor path)]
    (try
      (= (System/getProperty "user.name")
         (str (Files/getOwner (.toPath ancestor) (make-array java.nio.file.LinkOption 0))))
      (catch Exception _ false))))

(defn- under-home? [path]
  (try
    (let [home (.getCanonicalPath (io/file (System/getProperty "user.home")))
          path (.getCanonicalPath (io/file path))]
      (or (= path home) (str/starts-with? path (str home java.io.File/separator))))
    (catch Exception _ false)))

;; @spec ALIAS-MIGRATION-001
(defn validate-artifact-root!
  "Fails CLOSED on the resolved artifact root before any receipt writer
   trusts it (SPF-001: `default-artifact-root` accepted CLJ_SURGEON_ARTIFACT_ROOT
   and XDG_STATE_HOME unvalidated -- blank, relative, RAM-backed and another
   user's path were all silently accepted). Returns `root` unchanged when it
   passes every check; otherwise throws ex-info with a typed :error-type and
   the offending :root, naming the two variables that could have produced it.

   SPF-004 repaired TWO defects in the first version of this function, both
   in how it classified rather than in the checks themselves:
     - it called `mkdirs` on the candidate BEFORE the ownership/RAM checks,
       so a refused root was created and left behind on disk (/tmp/... and
       /dev/shm/... probes were created, then rejected). This version
       creates NOTHING -- classification runs only against paths that
       already exist (`pc/nearest-existing-ancestor`), never against a
       side effect of validating.
     - its self-contained RAM predicate FAILED OPEN when `findmnt` was
       unavailable (a `/run/user/.../` tmpfs path was accepted). Real-disk
       classification now goes through `clj-surgeon.path-classification`,
       the production fail-closed classifier (mounts-table + Darwin
       fallback, `:unknown` filesystem == refusal, never a pass) --
       requirable from here because it lives under src/, unlike the
       test/-only `clj-surgeon.tmp-leak-support` this function tried and
       reverted to depend on in the previous round (see path_classification's
       own docstring for why that broke the bb CLI).

   Checks, in order, against `pc/nearest-existing-ancestor` of the
   candidate (never against a path this function created):
     1. non-blank
     2. absolute
     3. owned by the invoking user, or under $HOME -- trivially safe even
        before it exists
     4. real disk, never RAM-backed, per `pc/base-refusal`

   This retains a controlled external override (CLJ_SURGEON_ARTIFACT_ROOT
   pointed at a real-disk path this invocation owns, e.g. the parity harness
   case /var/tmp/forge, which already exists on every box that runs this
   suite) while refusing every uncontrolled widening the fence found."
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

      ;; Checked by NAME first, ahead of ownership: /tmp and /dev/shm are
      ;; RAM-backed regardless of who owns them (often root, via a sticky
      ;; bit), and a caller reading the refusal deserves the precise reason
      ;; rather than "not owned" masking "RAM-backed" for exactly the two
      ;; paths this whole repair exists to catch.
      (pc/literal-ram-path? root)
      (throw (ex-info (pc/refusal-message {:reason :ram-path-prefix :base root})
                       {:reason :ram-path-prefix :base root
                        :error-type :artifact-root-ram-backed
                        :root root :checked-vars checked-vars}))

      (not (or (under-home? root) (owned-by-invoking-user? root)))
      (throw (ex-info "Artifact root is outside $HOME and not owned by the invoking user"
                       {:error-type :artifact-root-not-owned :root root :checked-vars checked-vars}))

      :else
      (let [ancestor (pc/nearest-existing-ancestor root)]
        (if-let [refusal (and ancestor (pc/base-refusal ancestor))]
          (throw (ex-info (pc/refusal-message refusal)
                           (assoc refusal :error-type :artifact-root-ram-backed
                                  :root root :checked-vars checked-vars)))
          root)))))

;; @spec ALIAS-MIGRATION-001
(defn writable-root!
  "THE single entrance every writer under *artifact-root* must call before
   creating anything (SPF-005: mcp-alias-migration/append-telemetry! built
   its directory straight from *artifact-root* and called mkdirs directly,
   never through here or through `directory` -- an unvalidated root reached
   a real write). Validates and returns *artifact-root* unchanged; throws on
   any failure. No mkdirs, no touch, no side effect happens in this
   function or in `validate-artifact-root!` -- callers do their own mkdirs
   only AFTER this returns."
  []
  (validate-artifact-root! *artifact-root*))

;; @spec ALIAS-MIGRATION-001
(defn directory [verb workspace]
  (writable-root!)
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
