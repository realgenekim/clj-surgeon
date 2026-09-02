(ns clj-surgeon.mcp-alias-migration
  "Request boundary for the alias_migration MCP verb.

  This namespace owns the filesystem half — scope expansion, project-root
  confinement, the frozen read, the transaction spec, the durable per-file
  detail file, and the O(1) receipt. It knows nothing about the rewrite itself,
  which lives in the pure `clj-surgeon.alias-migration` planner, and nothing
  about tool registration, which lives in `clj-surgeon.mcp-tool`."
  (:require
   [clj-surgeon.alias-migration :as planner]
   [clj-surgeon.file-ops :as file-ops]
   [clj-surgeon.intent-transaction :as transaction]
   [clj-surgeon.mcp-change-buffer :as change-buffer]
   [clj-surgeon.mcp-paths :as mcp-paths]
   [clojure.java.io :as io]
   [clojure.set :as set]
   [clojure.string :as str])
  (:import
   (java.nio.file FileSystems Files Path)
   (java.util UUID)))

(def request-fields
  #{:op :workspace_root :from :to :scope :expect})

(def skipped-directories
  #{".git" ".hg" "target" "node_modules" ".cpcache" ".clj-kondo" ".lsp"
    ".clj-surgeon" ".shadow-cljs" "out"})

;; ---------------------------------------------------------------------------
;; refusals

(defn- refusal
  [error-type message extra]
  (merge {:ok false
          :operation "alias_migration"
          :error_type (name error-type)
          :error message
          :source_unchanged true
          :mutation_attempted false
          :write_authority false
          :next_action "correct_request"}
         extra))

(defn- invalid-request
  [message path]
  (refusal :invalid-mcp-request message
           {:path path
            :remedy (str "Send exactly op, workspace_root, from {lib, var}, "
                         "to {lib, var, alias_policy}, scope {paths}, and "
                         "expect {files}.")}))

;; ---------------------------------------------------------------------------
;; request validation

(defn- nonblank-string?
  [value]
  (and (string? value) (not (str/blank? value))))

;; @spec MCP-OP-ALIAS-002
(defn validate-request
  "Validate one closed alias_migration request without reading source."
  [params]
  (let [unknown (vec (sort (map name (remove request-fields (keys params)))))
        from (:from params)
        to (:to params)
        scope (:scope params)
        expect (:expect params)
        policy (:alias_policy to)]
    (cond
      (seq unknown)
      (invalid-request (str "Unknown alias_migration fields: "
                            (str/join ", " unknown))
                       (vec unknown))

      (not (and (map? from) (nonblank-string? (:lib from))
                (or (nil? (:var from)) (nonblank-string? (:var from)))))
      (invalid-request "from must be {lib, var}; var is a non-blank string or null"
                       ["from"])

      (not (and (map? to) (nonblank-string? (:lib to))
                (or (nil? (:var to)) (nonblank-string? (:var to)))))
      (invalid-request "to must be {lib, var, alias_policy}; var is a non-blank string or null"
                       ["to"])

      (not (contains? #{nil "preserve-refer" "alias-qualify"} (:refer_policy to)))
      (invalid-request
        "to.refer_policy must be preserve-refer (default) or alias-qualify"
        ["to" "refer_policy"])

      (not (and (sequential? policy) (seq policy) (every? nonblank-string? policy)))
      (invalid-request "to.alias_policy must be one non-empty array of names"
                       ["to" "alias_policy"])

      (not (and (map? scope) (sequential? (:paths scope)) (seq (:paths scope))
                (every? nonblank-string? (:paths scope))))
      (invalid-request "scope.paths must be one non-empty array of glob patterns"
                       ["scope" "paths"])

      (and (contains? scope :exclude)
           (not (and (sequential? (:exclude scope))
                     (every? nonblank-string? (:exclude scope)))))
      (invalid-request "scope.exclude must be an array of project-relative paths"
                       ["scope" "exclude"])

      (not (and (map? expect) (integer? (:files expect)) (nat-int? (:files expect))))
      (invalid-request "expect.files must be one non-negative integer"
                       ["expect" "files"])

      :else
      {:ok true
       :request {:workspace-root (:workspace_root params)
                 :from {:lib (:lib from) :var (:var from)}
                 :to {:lib (:lib to) :var (:var to)
                      :alias-policy (vec policy)
                      :refer-policy (or (:refer_policy to) "preserve-refer")}
                 :scope {:paths (vec (:paths scope))
                         :exclude (vec (:exclude scope))}
                 :expect {:files (:files expect)}}})))

;; ---------------------------------------------------------------------------
;; scope expansion

(defn- glob-matcher
  [pattern]
  (.getPathMatcher (FileSystems/getDefault) (str "glob:" pattern)))

(defn- relative-path
  [^Path root ^Path candidate]
  (str/replace (.toString (.relativize root candidate)) "\\" "/"))

;; @spec MCP-OP-ALIAS-004
(defn expand-scope
  "Every confined project-relative Clojure source under scope.paths.

  The walk is bounded to the workspace root and skips build output and version
  control directories; it never leaves the configured project root."
  [^Path root {:keys [paths exclude]}]
  (let [matchers (mapv glob-matcher paths)
        excluded (set exclude)
        root-file (.toFile root)]
    (->> (file-seq root-file)
         (remove (fn [file]
                   (some skipped-directories
                         (str/split (str/replace
                                      (relative-path root (.toPath file))
                                      "\\" "/")
                                    #"/"))))
         (filter #(.isFile ^java.io.File %))
         (keep (fn [file]
                 (let [relative (relative-path root (.toPath file))]
                   (when (and (mcp-paths/relative-source-path? relative)
                              (not (str/ends-with? relative ".edn"))
                              (not (contains? excluded relative))
                              (some #(.matches ^java.nio.file.PathMatcher %
                                               (.getPath (FileSystems/getDefault)
                                                         relative
                                                         (make-array String 0)))
                                    matchers))
                     relative))))
         sort
         vec)))

;; ---------------------------------------------------------------------------
;; planning

;; @spec MCP-OP-ALIAS-006
;; @spec MCP-OP-ALIAS-012
(defn plan!
  "Expand scope, confine every path, freeze the sources, and plan.

  Returns {:ok true :plan p :root r :paths {relative absolute}} or one typed
  refusal. No bytes are written."
  [workspace-root request]
  (let [root (mcp-paths/real-root workspace-root)
        relatives (expand-scope root (:scope request))
        resolutions (mapv #(mcp-paths/resolve-source-path root %) relatives)
        bad (first (remove :ok resolutions))]
    (if bad
      (refusal :alias-migration-scope-path-refused
               (or (:error bad) "A scope path is outside the configured project root")
               {:path (:path bad) :next_call nil})
      (let [sources (mapv (fn [{:keys [relative path]}]
                            {:file relative :source (slurp path)})
                          resolutions)
            plan (planner/plan (assoc request :workspace-root
                                      (.toString root))
                               sources)]
        (cond
          (not (:ok plan))
          (assoc plan :scanned_files (count sources))

          ;; the lib-only migration also moves the defining namespace, so its
          ;; destination is confined and proved absent before anything is written
          (:lib-rename plan)
          (let [{:keys [new-file file]} (:lib-rename plan)
                destination (mcp-paths/resolve-new-source-path root new-file)]
            (if-not (:ok destination)
              (if (= "target-already-exists" (:error_type destination))
                (refusal :alias-migration-target-lib-exists
                         (str new-file " already exists, so " (:to (:lib-rename plan))
                              " cannot take over the definition in " file)
                         {:file new-file
                          :from_lib (:from (:lib-rename plan))
                          :to_lib (:to (:lib-rename plan))
                          :defining_file file
                          :next_call nil})
                (refusal :alias-migration-scope-path-refused
                         (or (:error destination) "The renamed namespace path is refused")
                         {:path new-file :next_call nil}))
              {:ok true
               :plan plan
               :root root
               :scanned-files (count sources)
               :destination destination
               :paths (into {} (map (juxt :relative :path)) resolutions)}))

          :else
          {:ok true
           :plan plan
           :root root
           :scanned-files (count sources)
           :paths (into {} (map (juxt :relative :path)) resolutions)})))))

;; ---------------------------------------------------------------------------
;; transaction spec

;; @spec MCP-OP-ALIAS-016
;; @spec MCP-OP-ALIAS-018
(defn plan->spec
  "Lower one plan into a single failure-atomic transaction spec.

  Every change's `find` is the exact original bytes of one complete top-level
  form. A file that drifted between the frozen read and commit no longer
  matches, so the whole transaction refuses before any write. A lib-only
  migration also carries the renamed defining namespace as one created file, so
  the new namespace and every rewritten caller land or refuse together."
  ([plan paths] (plan->spec plan paths nil))
  ([plan paths destination]
  (let [changes
        (vec (mapcat
               (fn [file-index {:keys [file edits]}]
                 (map-indexed
                   (fn [edit-index {:keys [kind original replacement]}]
                     {:id (keyword "alias-migration"
                                   (str "f" file-index "-e" edit-index
                                        "-" (name kind)))
                      :in [(get paths file)]
                      :find original
                      :do [:replace replacement]
                      :expect {:matches 1}})
                   edits))
               (range) (:files plan)))]
    (cond-> {:changes changes
             :expect {:changes (count changes)
                      :edits (count changes)
                      :files (count (:files plan))}}
      destination
      (assoc :create-files
             [{:file (:path destination)
               :content (get-in plan [:lib-rename :content])
               :directories (mapv str (:missing-parent-directories destination))}])))))

;; ---------------------------------------------------------------------------
;; durable per-file detail

;; @spec MCP-OP-ALIAS-020
(defn write-details!
  "Write per-file detail outside the receipt and return its relative path."
  [^Path root plan]
  (let [directory (io/file (.toFile root) ".clj-surgeon" "alias-migration")
        file-name (str (UUID/randomUUID) ".edn")
        target (io/file directory file-name)]
    (.mkdirs directory)
    (file-ops/atomic-write!
      (.getPath target)
      (pr-str (cond-> {:version 1
                       :files (mapv (fn [entry]
                                      (-> entry
                                          (select-keys [:file :alias :collided :sites
                                                        :refer-sites :require-mode])
                                          (update :collided vec)))
                                    (:files plan))}
                (:lib-rename plan)
                (assoc :lib-rename (dissoc (:lib-rename plan) :content)))))
    (str ".clj-surgeon/alias-migration/" file-name)))

;; ---------------------------------------------------------------------------
;; the receipt

(defn- verification-summary
  [verification]
  (if-not verification
    {:kondo_delta {:status "not-configured"}
     :focused_test {:status "not-configured"}}
    (let [checks (:checks verification)
          deltas (keep :diagnostic-delta checks)]
      {:kondo_delta
       (if (seq deltas)
         {:status "compared"
          :introduced (reduce + 0 (map :introduced-count deltas))
          :removed (reduce + 0 (map :removed-count deltas))
          :blocking_introduced (reduce + 0 (map :blocking-introduced-count deltas))}
         {:status "not-configured"})
       :focused_test
       {:status (if (:ok verification) "pass" "fail")
        :profile (str (:profile verification))
        :ok (boolean (:ok verification))
        :checks (count checks)}})))

;; @spec MCP-OP-ALIAS-019
;; @spec MCP-OP-ALIAS-020
;; @spec MCP-OP-ALIAS-026
(defn- lib-renamed-summary
  "One constant-size record of the defining namespace's move, or nil."
  [plan commit]
  (when-let [{:keys [from to file new-file]} (:lib-rename plan)]
    {:from from
     :to to
     :file file
     :new_file new-file
     :retired_to (:retired-file commit)}))

(defn receipt
  "Render one receipt whose length is constant in the number of namespaces."
  [plan commit details-path]
  (let [totals (:totals plan)]
    (merge
      {:ok true
       :operation "alias_migration"
       :committed true
       :files (:files totals)
       :sites (:sites totals)
       :refer_sites (:refer-sites totals)
       :alias_histogram (into {} (:alias-histogram totals))
       :collisions_resolved (:collisions-resolved totals)
       :lib_renamed (lib-renamed-summary plan commit)
       :details_path details-path
       :next_action "none"}
      (verification-summary (:verification commit))
      (select-keys commit [:undo_receipt :receipt_hash]))))

(defn commit-refusal
  "Translate one kernel refusal into the verb's typed public refusal."
  [plan commit]
  (refusal (or (some-> (or (:error-type commit) (:error_type commit)) name)
               "alias-migration-transaction-refused")
           (or (:error commit) "The alias migration transaction refused")
           (cond-> {:files (get-in plan [:totals :files])
                    :sites (get-in plan [:totals :sites])
                    :source_unchanged (boolean
                                        (or (:source-unchanged commit)
                                            (:source_unchanged commit)
                                            (not (:committed commit))))
                    :remedy (str "Re-send the same alias_migration request; the"
                                 " frozen snapshot is recomputed from current"
                                 " source.")}
             (:change-id commit) (assoc :change_id (str (:change-id commit))))))

(defn declared-file-set
  "The project-relative files one plan will change."
  [plan]
  (into (sorted-set) (map :file) (:files plan)))

(defn unexpected-files
  "Files a plan would change that are not in the declared expectation."
  [plan expected]
  (set/difference (declared-file-set plan) (set expected)))

;; ---------------------------------------------------------------------------
;; the write, through the shared transaction kernel

(defn selected-profile
  "The workspace's own focused profile, or nil when none is configured."
  [verification-profiles]
  (first (filter #(contains? verification-profiles %) ["fast" "full"])))

;; @spec MCP-OP-ALIAS-016
;; @spec MCP-OP-ALIAS-017
;; @spec MCP-OP-ALIAS-022
(defn retire-path
  "Where the superseded defining file is kept so the move stays reversible."
  [project-root relative]
  (str (io/file project-root ".clj-surgeon" "alias-migration" "retired" relative)))

(defn- retire-file!
  "Move the superseded defining file out of the source tree, reversibly."
  [project-root relative]
  (let [source (io/file project-root relative)
        target (io/file (retire-path project-root relative))]
    (.mkdirs (.getParentFile target))
    (Files/move (.toPath source) (.toPath target)
                (into-array java.nio.file.CopyOption
                            [java.nio.file.StandardCopyOption/REPLACE_EXISTING]))
    (.getPath target)))

(defn- restore-retired!
  [project-root relative]
  (let [target (io/file project-root relative)
        retired (io/file (retire-path project-root relative))]
    (when (.exists retired)
      (.mkdirs (.getParentFile target))
      (Files/move (.toPath retired) (.toPath target)
                  (into-array java.nio.file.CopyOption
                              [java.nio.file.StandardCopyOption/REPLACE_EXISTING])))))

(defn commit!
  "Commit one alias migration through `execute-mcp-change!`.

  Verification, when the workspace configures a profile, shares the
  transaction's rollback authority exactly as the other write tools do. A
  lib-only migration additionally retires the superseded defining file after
  the kernel commits; any later failure restores it before rolling back, so the
  tree is never left with two definitions or none."
  ([config project-root spec files] (commit! config project-root spec files nil))
  ([{:keys [verification-profiles receipt-dir]} project-root spec files retire]
  (.mkdirs (io/file receipt-dir))
  (let [profile (selected-profile verification-profiles)
        baseline (when profile
                   (change-buffer/capture-verification-baseline!
                     project-root profile verification-profiles files))]
    (if (and baseline (not (:ok baseline)))
      {:error "Verification baseline capture failed before the alias migration"
       :error-type (or (:error-type baseline) :verification-baseline-failed)
       :verification baseline
       :source-unchanged true}
      (let [result (transaction/execute-mcp-change!
                     {:spec spec
                      :receipt-out (str (io/file receipt-dir
                                                 (str (UUID/randomUUID) ".edn")))
                      :write-refusal-context {:operation "alias_migration"
                                              :project-root project-root}})]
        (if (:error result)
          result
          (let [retired (try
                          (when retire (retire-file! project-root retire))
                          (catch Exception error
                            {:retire-error (.getMessage error)}))]
            (cond
              (:retire-error retired)
              (let [rollback (transaction/execute-undo!
                               {:receipt (:receipt-file result)})]
                {:error (str "The superseded defining file could not be retired; "
                             "the alias migration was rolled back")
                 :error-type :alias-migration-retire-failed
                 :cause-error (:retire-error retired)
                 :rolled-back (boolean (:ok rollback))
                 :source-unchanged (boolean (:ok rollback))})

              (nil? profile)
              (cond-> result retired (assoc :retired-file retired))

              :else
              (let [verification (change-buffer/run-verification!
                                   project-root profile verification-profiles
                                   files baseline)]
                (if (:ok verification)
                  (cond-> (assoc result :verification verification)
                    retired (assoc :retired-file retired))
                  (let [_ (when retire (restore-retired! project-root retire))
                        rollback (transaction/execute-undo!
                                   {:receipt (:receipt-file result)})
                        rolled-back? (boolean (:ok rollback))]
                    (when rolled-back?
                      (.delete (io/file (:receipt-file result))))
                    {:error "Verification failed; the alias migration was rolled back"
                     :error-type (or (:error-type verification) :verification-failed)
                     :verification verification
                     :rolled-back rolled-back?
                     :source-unchanged rolled-back?})))))))))))

;; @spec MCP-OP-ALIAS-001
;; @spec MCP-OP-ALIAS-005
;; @spec MCP-OP-ALIAS-007
;; @spec MCP-OP-ALIAS-008
;; @spec MCP-OP-ALIAS-009
;; @spec MCP-OP-ALIAS-010
;; @spec MCP-OP-ALIAS-011
;; @spec MCP-OP-ALIAS-013
;; @spec MCP-OP-ALIAS-014
;; @spec MCP-OP-ALIAS-015
;; @spec MCP-OP-ALIAS-018
;; @spec MCP-OP-ALIAS-019
(defn execute!
  "Plan, commit, and publish one O(1) alias_migration receipt."
  [config params]
  (let [validated (validate-request params)]
    (if-not (:ok validated)
      validated
      (let [request (:request validated)
            project-root (:project-root config)
            planned (plan! project-root request)]
        (if-not (:ok planned)
          planned
          (let [{:keys [plan root paths destination]} planned
                spec (plan->spec plan paths destination)
                files (mapv #(get paths (:file %)) (:files plan))
                commit (commit! config (.toString root) spec files
                                (get-in plan [:lib-rename :file]))]
            (if (:error commit)
              (commit-refusal plan commit)
              (receipt plan
                       (-> commit
                           (assoc :undo_receipt (:receipt-file commit)
                                  :receipt_hash (:receipt-hash commit)))
                       (write-details! root plan)))))))))
