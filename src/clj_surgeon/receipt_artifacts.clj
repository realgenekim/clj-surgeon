(ns clj-surgeon.receipt-artifacts
  "External verb bookkeeping and measured post-write workspace evidence."
  (:require
   [clj-surgeon.operation-algebra :as algebra]
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

;; @spec DATACODE-ENV-002
(defn policy-envelope-roots [{:keys [tmpdir home workspace artifact-root]}]
  [(if (and (not (str/blank? tmpdir))
            (.isAbsolute (io/file tmpdir))
            (not (pc/literal-ram-path? tmpdir))) tmpdir "/var/tmp")
   (or artifact-root (str home "/.local/state/clj-surgeon"))
   (str (.getAbsoluteFile (io/file workspace)))])

(defn- data-hash [value]
  (apply str (map #(format "%02x" (bit-and 255 %))
                  (.digest (MessageDigest/getInstance "SHA-256")
                           (.getBytes (pr-str value) "UTF-8")))))

(defn destination-envelope [roots source]
  {:id (data-hash roots)
   :roots roots :source source})

(defn- validated-envelope! [envelope]
  (when-not (algebra/valid-destination-envelope? envelope)
    (throw (ex-info "Invalid destination envelope"
                    {:error-type :invalid-operation-context})))
  envelope)

(defn- passwd-home []
  (let [username (System/getProperty "user.name")
        entry (some #(let [fields (str/split % #":")]
                       (when (= username (first fields)) (nth fields 5 nil)))
                    (str/split-lines (slurp "/etc/passwd")))]
    (or entry
        (let [{:keys [exit out]} (shell/sh "getent" "passwd" username)]
          (when (zero? exit) (nth (str/split (str/trim out) #":") 5 nil)))
        (throw (ex-info "Cannot determine invoking user's passwd home"
                        {:error-type :invalid-operation-context})))))

(defn- default-envelope [workspace source]
  (destination-envelope
    (policy-envelope-roots {:tmpdir (System/getenv "TMPDIR")
                            :home (passwd-home) :workspace workspace
                            :artifact-root (System/getenv "CLJ_SURGEON_ARTIFACT_ROOT")})
    source))

(defonce ^:private policy-default
  (default-envelope (System/getProperty "user.dir") :policy-default))
(defonce ^:private launcher-envelope (atom nil))
(def ^:dynamic *destination-envelope* nil)

;; @spec DATACODE-ENV-002, DATACODE-ENV-003
(defn initialize-envelope!
  "Launcher-only initialization. Later workspace requests cannot widen it."
  ([workspace] (initialize-envelope! workspace nil))
  ([workspace supplied]
   (or @launcher-envelope
       (let [envelope (validated-envelope! (or supplied (default-envelope workspace :launcher)))]
         (compare-and-set! launcher-envelope nil envelope)
         @launcher-envelope))))

(defn current-envelope []
  (or *destination-envelope* @launcher-envelope policy-default))

(defn resolved-target
  "Resolve existing ancestors (including dangling links), retaining absent tail."
  [requested]
  (letfn [(resolve-path [^java.nio.file.Path path depth]
            (when (> depth 128)
              (throw (ex-info "Artifact path has cyclic or excessive symlinks"
                              {:error-type :artifact-path-unresolvable :path (str requested)})))
            (cond
              (Files/isSymbolicLink path)
              (let [link (Files/readSymbolicLink path)]
                (resolve-path (if (.isAbsolute link) link (.resolve (.getParent path) link))
                              (inc depth)))
              (Files/exists path (make-array java.nio.file.LinkOption 0))
              (.toRealPath path (make-array java.nio.file.LinkOption 0))
              :else
              (if-let [parent (.getParent path)]
                (.normalize (.resolve (resolve-path parent (inc depth)) (.getFileName path)))
                (throw (ex-info "Artifact path cannot be resolved"
                                {:error-type :artifact-path-unresolvable :path (str requested)})))))]
    (resolve-path (.toAbsolutePath (.toPath (io/file (str requested)))) 0)))

;; @spec DATACODE-ENV-001
(defn admit-target!
  "Admit the final directory OR file before any creation/publication. No I/O writes."
  ([requested] (admit-target! requested :receipt-publish))
  ([requested effect]
   (let [{:keys [id roots]} (validated-envelope! (current-envelope))
         resolved (resolved-target requested)]
     (when-not (some #(.startsWith resolved (resolved-target %)) roots)
       (throw (ex-info "Artifact write is outside the destination envelope"
                       {:error-type :write-outside-envelope :effect effect
                        :path (str requested) :resolved-path (str resolved)
                        :envelope-id id :roots roots})))
     (str resolved))))

;; @spec DATACODE-ENV-004
(defn receipt-evidence [receipt]
  (let [receipt (assoc receipt :envelope-id (:id (current-envelope)))]
    (cond-> receipt
      (:receipt-hash receipt)
      (assoc :receipt-hash
             (data-hash (dissoc receipt :receipt-hash))))))

(defn admitted-file
  "Construct and admit one concrete artifact filename."
  ^java.io.File [& parts]
  (io/file (admit-target! (apply io/file parts))))

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
        dir (io/file (admit-target! (io/file *artifact-root* (str verb "-receipts") identity)))]
    (when (.startsWith (.toPath dir) (.toPath root))
      (throw (ex-info "Receipt directory resolves inside the workspace"
                      {:error-type :receipt-dir-inside-workspace :receipt-dir (str dir)})))
    (str dir)))

;; @spec ALIAS-MIGRATION-001
(defn target [verb workspace relative]
  (let [base (.toPath (io/file (directory verb workspace)))
        file (io/file (admit-target! (io/file (str base) relative)))]
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
        (cond-> {:envelope-id (:id (current-envelope))
                 :workspace_status {:checked true :command command :exit (:exit result)
                                    :clean_except_proven proven? :unexpected_paths unexpected}}
          proven? (assoc :workspace_clean_except (vec (sort allowed)))
          (not (zero? (:exit result))) (assoc-in [:workspace_status :error] (:err result))))
      (catch Exception error
        {:workspace_status {:checked false :clean_except_proven false :command command
                            :error (.getMessage error)}}))))
