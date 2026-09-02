(ns clj-surgeon.mcp-formatter
  "Format staged candidate sources before a transaction writes live files."
  (:require
   [clj-surgeon.format-scope :as format-scope]
   [clj-surgeon.mcp-change-buffer :as change-buffer]
   [clojure.java.io :as io]
   [clojure.string :as str]))

(def formatter-version
  "The exact formatter version this product owns. Pinned, because the check that
   bounds a scoped format — `format-scope/clause-normalised-stream` — was
   measured against one version's behaviour (0 false refusals over 1735 forms),
   and an unpinned `npx` silently resolves a different one."
  "0.29.0")

;; @spec MCP-OP-FMT-012
(def default-command
  ["npx" (str "@chrisoakman/standard-clojure-style@" formatter-version)
   "fix" "{files}"])

(defn verification-profiles-after-format
  "Remove the formatter's corresponding check command after formatting has
   become a mandatory pre-commit stage. Other checks and profiles are exact."
  [profiles formatter-command]
  (let [check-command (mapv #(if (= "fix" %) "check" %) formatter-command)]
    (into {}
          (map (fn [[profile spec]]
                 [profile
                  (if (map? spec)
                    (update spec :commands
                            (fn [commands]
                              (vec (remove #(= check-command %) commands))))
                    spec)]))
          profiles)))

(defn- suffix
  [file]
  (or (some->> (re-find #"\.(?:clj|cljs|cljc)$" file) str)
      ".clj"))

(defn format-candidates!
  "Format future source strings through one closed command. Live project files
   are never passed to the formatter."
  ([project-root command future-sources]
   (format-candidates! project-root command future-sources
                       change-buffer/run-process!))
  ([project-root command future-sources run-process!]
   (if-not (and (vector? command)
                (seq command)
                (every? #(and (string? %) (not (str/blank? %))) command)
                (some #{"{files}"} command))
     {:ok false
      :error-type :invalid-formatter-command
      :error "Formatter command must be a non-empty string vector containing {files}"
      :source-unchanged true}
     (let [staged (mapv (fn [[file source]]
                          (let [temp (java.io.File/createTempFile
                                       "clj-surgeon-candidate-" (suffix file))]
                            (spit temp source)
                            {:file file :temp temp}))
                        (sort-by key future-sources))]
       (try
         (let [temp-files (mapv #(str (:temp %)) staged)
               result (run-process!
                        project-root
                        (change-buffer/expand-command command temp-files))]
           (if (and (:finished? result) (zero? (:exit result)))
             (let [formatted (into (sorted-map)
                                   (map (fn [{:keys [file temp]}]
                                          [file (slurp temp)]))
                                   staged)]
               {:ok true
                :status :complete
                :file-count (count formatted)
                :changed-file-count
                (count (filter (fn [[file source]]
                                 (not= source (get future-sources file)))
                               formatted))
                :elapsed_ms (:elapsed_ms result)
                :future-sources formatted})
             {:ok false
              :error-type (if (:finished? result)
                            :formatter-failed
                            :formatter-timeout)
              :error "Formatter failed on staged candidate files"
              :command (first command)
              :exit (:exit result)
              :elapsed_ms (:elapsed_ms result)
              :output (:output result)
              :source-unchanged true}))
         (finally
           (doseq [{:keys [temp]} staged]
             (io/delete-file temp true))))))))

;; ---------------------------------------------------------------------------
;; Scoped formatting
;;
;; `format-candidates!` above stages whole files. On a route that edits an
;; existing file that reformats every untouched line in it, which is the
;; measured `l1` churn. `format-scoped-candidates!` stages only the top-level
;; forms the transaction actually edited, then splices the formatted text back
;; at its exact original span. Design: docs/intent/mcp-operation-contract/
;; format-scope-design.md.

(defn- fragment-key
  "A staging key for one top-level form. The file suffix is preserved so the
   formatter still sees a .cljc form as .cljc."
  [file index]
  (str file "::" index (suffix file)))

(defn- scope-refusal
  [error-type file message extra]
  (merge {:ok false
          :error-type error-type
          :error message
          :file file
          :source-unchanged true
          :mutation-attempted false
          :write-authority false}
         extra))

;; @spec MCP-OP-FMT-006
;; @spec MCP-OP-FMT-010
;; @spec MCP-OP-FMT-011
;; The decision is pure and lives in `format-scope/file-plan`, so it is
;; witnessed without a formatter or a project.
(def ^:private file-regions format-scope/file-plan)

;; @spec MCP-OP-FMT-005
(defn- accept-formatted-form
  "Check one formatted form before it is allowed anywhere near a splice.

   After this leaf these two checks are the ONLY bound on what the formatter
   may do inside a form it was handed; `format-scope/scope-drift` is a self-test
   of the splice arithmetic and bounds nothing."
  [file index offset original formatted]
  (let [trimmed (format-scope/trim-trailing-newlines formatted)
        before (format-scope/clause-normalised-stream (str original))
        after (format-scope/clause-normalised-stream trimmed)]
    (cond
      (not (format-scope/one-form? trimmed))
      (scope-refusal
        :format-fragment-not-one-form file
        (str "Refusing to write " file ": the formatter returned something"
             " other than one complete top-level form for the form at offset "
             offset ". A formatter handed one form must"
             " give one form back.")
        {:form-index index})

      ;; A stream that could not be read is a refusal, never a match. Two nils
      ;; comparing equal would be the fail-open this check exists to prevent.
      (or (nil? before) (nil? after) (not= before after))
      (scope-refusal
        :format-altered-form file
        (str "Refusing to write " file ": the formatter changed the code in one"
             " top-level form, not just its layout. A token, a comment, or"
             " their order differs. Sorting the clauses of an ns `:require` or"
             " `:import` list is the one reordering a formatter may make;"
             " everything else is a code change.")
        {:form-index index})

      :else {:ok true :text trimmed})))

;; @spec MCP-OP-FMT-001
;; @spec MCP-OP-FMT-002
;; @spec MCP-OP-FMT-003
;; @spec MCP-OP-FMT-004
(defn format-scoped-candidates!
  "Format only the top-level forms a transaction edited, in one formatter
   process, and splice each formatted form back at its exact original span.

   `splice-guard` is the compiled transaction's own guard: per file, the
   byte-preserving reference and the replacement spans in that reference's
   coordinates. Those spans select the enclosing top-level forms; every byte
   outside them is carried through verbatim and is proved to have been.

   Returns the staged sources and an updated splice guard whose reference is
   the post-format image and whose spans are the formatted forms, so the
   commit-time drift gate measures any *later* staging step against what this
   one produced. Every failure is typed and leaves `:source-unchanged` true."
  ([project-root command future-sources splice-guard]
   (format-scoped-candidates! project-root command future-sources splice-guard
                              format-candidates!))
  ([project-root command future-sources splice-guard format!]
   (let [planned (mapv (fn [[file source]]
                         (file-regions file source (get splice-guard file)))
                       (sort-by key future-sources))]
     (if-let [refusal (first (remove :regions planned))]
       refusal
       (let [fragments
             (into (sorted-map)
                   (mapcat (fn [{:keys [file regions]}]
                             (let [source (get future-sources file)]
                               (map-indexed
                                 (fn [index {:keys [start end]}]
                                   [(fragment-key file index)
                                    (subs source start end)])
                                 regions)))
                           planned))
             form-count (count fragments)
             planned-form-count (reduce + 0 (map (comp count :regions) planned))]
         (cond
           ;; Two staging keys that collide would silently share one formatted
           ;; text. The keys are derived from file names, so a collision needs a
           ;; file literally named like a fragment key — unlikely, and a typed
           ;; refusal rather than a silent miscount.
           (not= form-count planned-form-count)
           (scope-refusal
             :format-scope-staging-collision
             (:file (first planned))
             (str "Refusing to format: " planned-form-count " top-level forms"
                  " were selected but only " form-count " distinct staging keys"
                  " could be derived, so at least two forms would share one"
                  " formatted result.")
             {})

           (zero? form-count)
           {:ok true
            :status :complete
            :scope :top-level-forms
            :file-count 0
            :changed-file-count 0
            :form-count 0
            :changed-form-count 0
            :elapsed_ms 0.0
            :future-sources future-sources
            :splice-guard splice-guard}

           :else
           (let [formatted
                 (try
                   (let [returned (format! project-root command fragments)]
                     (if (map? returned)
                       returned
                       ;; @spec MCP-OP-FMT-009
                       {:ok false
                        :error-type :formatter-failed
                        :error "Formatter returned no result for the staged forms"
                        :detail (pr-str returned)
                        :source-unchanged true
                        :mutation-attempted false
                        :write-authority false}))
                   (catch Throwable error
                     ;; @spec MCP-OP-FMT-009
                     {:ok false
                      :error-type :formatter-failed
                      :error "Formatter threw while formatting the staged forms"
                      :detail (str (.getName (class error)) ": "
                                   (.getMessage error))
                      :source-unchanged true
                      :mutation-attempted false
                      :write-authority false}))]
             (if-not (:ok formatted)
               ;; @spec MCP-OP-FMT-009
               ;; An invalid command, a nonzero exit, or a timeout is already a
               ;; typed refusal carrying :source-unchanged. It is passed through
               ;; whole rather than re-wrapped, so the caller sees the same
               ;; reason the whole-file stage would have given.
               formatted
               (let [staged (:future-sources formatted)
                     outcome
                     (reduce
                       (fn [acc {:keys [file regions]}]
                         (if-not (:ok acc)
                           (reduced acc)
                           (let [source (get future-sources file)
                                 texts
                                 (reduce
                                   (fn [texts index]
                                     (if-not (:ok texts)
                                       (reduced texts)
                                       (let [{:keys [start end]}
                                             (nth regions index)
                                             original (subs source start end)
                                             checked
                                             (accept-formatted-form
                                               file index start original
                                               (get staged
                                                    (fragment-key file index)
                                                    ""))]
                                         (if (:ok checked)
                                           (update texts :texts conj
                                                   (:text checked))
                                           checked))))
                                   {:ok true :texts []}
                                   (range (count regions)))]
                             (if-not (:ok texts)
                               (reduced texts)
                               (let [{spliced :source spans :spans}
                                     (format-scope/splice-forms
                                       source regions (:texts texts))
                                     scope (format-scope/scope-drift
                                             source spliced regions spans)]
                                 (if (pos? (:byte-drift-outside-forms scope))
                                   (reduced
                                     (scope-refusal
                                       :format-scope-drift file
                                       (str "Refusing to write " file ": the"
                                            " splice arithmetic did not hold — "
                                            (:byte-drift-outside-forms scope)
                                            " bytes outside the top-level forms"
                                            " this transaction edited differ."
                                            " This is a self-test of"
                                            " `splice-forms`, not a bound on the"
                                            " formatter, and it firing means"
                                            " this server has a defect.")
                                       {:byte-drift-outside-forms
                                        (:byte-drift-outside-forms scope)
                                        :span-alignment
                                        (name (:span-alignment scope))}))
                                   (-> acc
                                       (assoc-in [:sources file] spliced)
                                       (assoc-in [:guard file]
                                                 {:reference spliced
                                                  :spans spans})
                                       (update :changed-form-count
                                               +
                                               (count
                                                 (remove
                                                   (fn [index]
                                                     (let [{:keys [start end]}
                                                           (nth regions index)]
                                                       (= (subs source start end)
                                                          (nth (:texts texts)
                                                               index))))
                                                   (range (count regions))))))))))))
                       {:ok true
                        :sources future-sources
                        :guard splice-guard
                        :changed-form-count 0}
                       planned)]
                 (if-not (:ok outcome)
                   outcome
                   (let [sources (:sources outcome)]
                     {:ok true
                      :status :complete
                      :scope :top-level-forms
                      :file-count (count (filter (comp seq :regions) planned))
                      :changed-file-count
                      (count (filter (fn [[file source]]
                                       (not= source (get future-sources file)))
                                     sources))
                      :form-count form-count
                      :changed-form-count (:changed-form-count outcome)
                      :elapsed_ms (:elapsed_ms formatted)
                      :future-sources sources
                      :splice-guard (:guard outcome)})))))))))))
