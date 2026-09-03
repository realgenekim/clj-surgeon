(ns clj-surgeon.mcp-extraction
  "Pure compiler for one typed MCP namespace-extraction decision."
  (:require
   [clj-surgeon.extract :as extract]
   [clj-surgeon.file-ops :as file-ops]
   [clj-surgeon.intent-transaction :as transaction]
   [clj-surgeon.structural-lens :as structural-lens]
   [clojure.java.io :as io]
   [clojure.set :as set]
   [rewrite-clj.parser :as parser]))

(def receipt-version 1)

(def caller-proof-levels
  {:semantic-complete
   {:scan-complete true
    :semantic-provider-used true
    :zero-callers-authoritative true
    :meaning "Resolved callers came from one hash-bound semantic session."}
   :structural-candidates-only
   {:scan-complete true
    :semantic-provider-used false
    :zero-callers-authoritative false
    :meaning "Exact structural scans completed; aliases, macros, or generated callers may remain."}
   :caller-proof-unavailable
   {:scan-complete false
    :semantic-provider-used false
    :zero-callers-authoritative false
    :meaning "No trustworthy caller inventory completed; absence must not authorize deletion."}})

(def structural-caller-proof
  (assoc (:structural-candidates-only caller-proof-levels)
         :level :structural-candidates-only))

(defn refusal
  [error-type message data]
  (merge
    {:ok false
     :error-type error-type
     :error message
     :source-unchanged true
     :target-unchanged true}
    data))

(defn normalize-mechanical-fields
  "Default omitted bookkeeping without erasing an omitted visibility decision."
  [{:keys [file to forms caller-changes ignored-caller-files expect]
    :as request}]
  (let [caller-changes (or caller-changes [])
        ignored-caller-files (or ignored-caller-files [])
        expect (or expect
                   {:forms (count forms)
                    :caller-edits
                    (reduce + (map #(get-in % [:expect :matches])
                                   caller-changes))
                    :files (count (distinct (concat [file to]
                                                    (mapcat :files
                                                            caller-changes))))})]
    (assoc request
           :caller-changes caller-changes
           :ignored-caller-files ignored-caller-files
           :expect expect)))

(defn validate-request
  [{:keys [file to forms public-forms require-policy expect caller-changes
           ignored-caller-files source-hash]
    :as request}]
  (cond
    (not (map? request))
    (refusal :invalid-extraction-request
             "extraction must be an object"
             {})

    (not (string? file))
    (refusal :invalid-extraction-file
             "extraction.file must be a project-relative source path"
             {})

    (not (string? to))
    (refusal :invalid-extraction-target
             "extraction.to must be a project-relative absent source path"
             {})

    (= file to)
    (refusal :extraction-path-collision
             "extraction.file and extraction.to must differ"
             {:file file})

    (or (not (vector? forms))
        (empty? forms)
        (not-every? string? forms)
        (not= (count forms) (count (distinct forms))))
    (refusal :invalid-extraction-forms
             "extraction.forms must contain distinct form names"
             {})

    (and (contains? request :public-forms)
         (or (not (vector? public-forms))
             (not-every? string? public-forms)
             (not= (count public-forms) (count (distinct public-forms)))))
    (refusal :invalid-public-forms
             "extraction.public_forms must contain distinct form names"
             {})

    ;; @spec MCP-OP-FIELD-002
    (not (#{:minimal :copy-all} require-policy))
    (refusal :invalid-require-policy
             "extraction.require_policy must be minimal or copy-all"
             {:field "extraction.require_policy"
              :accepted ["minimal" "copy-all"]
              :require-policy require-policy
              :remedy (str "extraction.require_policy is required and is never "
                           "defaulted. Send \"minimal\" to copy only the requires "
                           "the moved forms use, or \"copy-all\" to copy the "
                           "source namespace's complete require list, then call "
                           "apply_clojure_changes once.")})

    (and source-hash
         (or (not (string? source-hash))
             (not (re-matches #"[0-9a-f]{64}" source-hash))))
    (refusal :invalid-source-hash
             "extraction.source_hash must be a lowercase SHA-256 hex string"
             {})

    (not (map? expect))
    (refusal :invalid-extraction-expect
             "extraction.expect must be an object"
             {})

    (not= #{:forms :caller-edits :files} (set (keys expect)))
    (refusal :invalid-extraction-expect
             "extraction.expect requires exactly forms, caller_edits, and files"
             {:fields (vec (sort (keys expect)))})

    (not-every? nat-int? (vals expect))
    (refusal :invalid-extraction-expect
             "All extraction.expect counts must be non-negative integers"
             {:actual expect})

    (not (vector? caller-changes))
    (refusal :invalid-caller-changes
             "extraction.caller_changes must be an array"
             {})

    (or (not (vector? ignored-caller-files))
        (not-every? string? ignored-caller-files)
        (not= (count ignored-caller-files)
              (count (distinct ignored-caller-files))))
    (refusal :invalid-ignored-callers
             "extraction.ignored_caller_files must contain distinct paths"
             {})

    :else
    {:ok true}))

(defn- caller-files
  [changes]
  (->> changes (mapcat :in) set))

(defn- compile-callers
  [sources changes expected-edits]
  (if (empty? changes)
    (if (zero? expected-edits)
      {:ok true
       :match-count 0
       :original-sources {}
       :future-sources {}}
      (refusal :caller-edit-count-mismatch
               "No caller changes were supplied for expect.caller_edits"
               {:expected expected-edits :actual 0}))
    (transaction/compile-transaction
      sources
      {:changes changes
       :expect {:changes (count changes)
                :edits expected-edits
                :files (count (caller-files changes))}})))

;; @spec MCP-OP-PLAN-004
;; @spec MCP-OP-PLAN-007
;; @spec MCP-OP-PLAN-008
;; @spec MCP-OP-PLAN-009
;; @spec MCP-OP-PLAN-010
(defn compile-extraction
  "Compile extraction and exact caller changes against one captured snapshot."
  [{:keys [file to forms public-forms require-policy source target-ns
           workspace-sources source-hash created-directories]
    :or {require-policy :minimal workspace-sources {}
         created-directories []}
    :as request}]
  (let [public-forms-supplied? (contains? request :public-forms)
        request (normalize-mechanical-fields request)
        {:keys [expect caller-changes ignored-caller-files]} request
        validation (validate-request request)]
    (if-not (:ok validation)
      validation
      (if (and source-hash
               (not= source-hash (structural-lens/source-hash source)))
        (refusal :source-hash-mismatch
                 "The extraction source changed after planning"
                 {:expected source-hash
                  :actual (structural-lens/source-hash source)})
        (let [plan-input {:file file
                          :source source
                          :forms forms
                          :public-forms (or public-forms [])
                          :derive-required-public-forms
                          (not public-forms-supplied?)
                          :to to
                          :target-ns target-ns
                          :workspace-sources workspace-sources
                          :require-policy require-policy}
              plan (extract/compile-plan plan-input)]
          (cond
            (:error plan)
            (refusal (or (:error-type plan) :extraction-plan-refused)
                     (:error plan)
                     (select-keys plan [:invalid-public-forms
                                        :unsupported-public-forms]))

            (and public-forms-supplied?
                 (seq (:missing-required-public-forms plan)))
            (refusal :required-public-forms-missing
                     "Every moved private form called by remaining source must be public"
                     {:required-public-forms (:required-public-forms plan)
                      :missing-public-forms
                      (:missing-required-public-forms plan)})

            (not= (:forms expect) (:form-count plan))
            (refusal :extraction-count-mismatch
                     "The compiled extraction form count did not match expect.forms"
                     {:expected (:forms expect)
                      :actual (:form-count plan)})

            :else
            (try
              (let [future
                    (extract/compile-candidates
                      {:source source
                       :source-file file
                       :target-file to
                       :form-ranges (:_form-texts plan)
                       :target-source (:_new-file-content plan)
                       :target-ns target-ns
                       :target-alias (:target-alias plan)
                       :source-referred-forms (:_source-referred-forms plan)})
                    changed-caller-files (caller-files caller-changes)
                    forbidden (set/intersection #{file to} changed-caller-files)
                    candidates (set/union
                                 (set (:callers-to-review plan))
                                 (set (map :file (:quoted-var-references plan))))
                    accounted (set/union changed-caller-files
                                         (set ignored-caller-files))
                    omitted (set/difference candidates accounted)
                    caller-sources (select-keys workspace-sources
                                                changed-caller-files)
                    caller-result (compile-callers
                                    caller-sources caller-changes
                                    (:caller-edits expect))
                    originals (merge (sorted-map file source)
                                     (:original-sources caller-result))
                    futures (merge (sorted-map file (:source future)
                                               to (:target future))
                                   (:future-sources caller-result))
                    actual-files (count futures)]
                (cond
                  (seq forbidden)
                  (refusal :caller-change-path-collision
                           "caller_changes may not target extraction.file or extraction.to"
                           {:files (vec (sort forbidden))})

                  (seq omitted)
                  (let [unknowns
                        (mapv (fn [caller-file]
                                {:decision :caller-disposition
                                 :file caller-file
                                 :source-hash
                                 (some-> (get workspace-sources caller-file)
                                         structural-lens/source-hash)})
                              (sort omitted))]
                    (refusal
                      :extraction-decisions-required
                      "Caller candidates require an explicit change or ignore decision"
                      {:files (vec (sort omitted))
                       :mutation-attempted false
                       :write-authority false
                       :remedy
                       "Fill each caller disposition in next_call and call apply_clojure_changes once."
                       :genuine-unknowns unknowns
                       :completed-plan
                       {:file file
                        :to to
                        :forms-to-extract (:forms-to-extract plan)
                        :form-count (:form-count plan)
                        :required-public-forms
                        (:required-public-forms plan)
                        :require-policy (:require-policy plan)
                        :source-hash (:_source-hash plan)
                        :callers-to-review (:callers-to-review plan)
                        :quoted-var-references
                        (:quoted-var-references plan)}}))

                  (not (:ok caller-result))
                  caller-result

                  (not= (:files expect) actual-files)
                  (refusal :extraction-file-count-mismatch
                           "The compiled file count did not match expect.files"
                           {:expected (:files expect) :actual actual-files})

                  :else
                  {:ok true
                   :operation :compiled-extraction
                   :caller-proof structural-caller-proof
                   :file file
                   :to to
                   :forms (:forms-to-extract plan)
                   :form-count (:form-count plan)
                   :caller-edit-count (:match-count caller-result)
                   :require-policy (:require-policy plan)
                   :callers-to-review (:callers-to-review plan)
                   :quoted-var-references (:quoted-var-references plan)
                   :original-sources originals
                   :future-sources futures
                   :created-directories (vec created-directories)
                   :created-files [to]}))
              (catch Exception error
                (refusal :invalid-extraction-result
                         (.getMessage error)
                         {})))))))))

(defn with-future-sources
  "Replace a compiled extraction's candidates after a staged source
   transformation. Created-file and original-source ownership remain exact."
  [compiled future-sources]
  (try
    (when-not (and (:ok compiled)
                   (map? future-sources)
                   (= (set (keys (:future-sources compiled)))
                      (set (keys future-sources))))
      (throw (ex-info "Transformed extraction sources must cover the file set exactly"
                      {:error-type :invalid-future-sources})))
    (doseq [[file source] future-sources]
      (when-not (string? source)
        (throw (ex-info "Transformed extraction source must be text"
                        {:error-type :invalid-future-source :file file})))
      (try
        (parser/parse-string-all source)
        (catch Exception error
          (throw (ex-info "Transformed extraction source does not parse"
                          {:error-type :invalid-future-source
                           :file file
                           :cause-error (.getMessage error)})))))
    (assoc compiled :future-sources (into (sorted-map) future-sources))
    (catch clojure.lang.ExceptionInfo error
      (merge {:ok false :error (.getMessage error)} (ex-data error)))
    (catch Exception error
      {:ok false
       :error-type :future-source-transformation-failed
       :error (.getMessage error)})))

(defn- source-hash
  [source]
  (structural-lens/source-hash source))

(defn build-receipt
  [compiled]
  (let [receipt
        {:receipt-version receipt-version
         :operation :compiled-extraction
         :caller-proof (:caller-proof compiled)
         :created-directories (vec (:created-directories compiled))
         :files
         (mapv
           (fn [[file future]]
             (if-let [original (get (:original-sources compiled) file)]
               {:file file
                :source-hash (source-hash original)
                :result-hash (source-hash future)
                :original-source original
                :result-source future}
               {:file file
                :absent-before true
                :result-hash (source-hash future)
                :result-source future}))
           (:future-sources compiled))
         :inverse {:operation :undo-compiled-extraction}}]
    (assoc receipt :receipt-hash (source-hash (pr-str receipt)))))

(defn- file-state
  [{:keys [read-source exists?]} file]
  (when (exists? file)
    (read-source file)))

(defn- rollback-file!
  [{:keys [read-source write-source! delete-file! exists?] :as io}
   originals futures created-files file]
  (let [current (file-state io file)
        future (get futures file)]
    (cond
      (contains? created-files file)
      (cond
        (nil? current) {:file file :recovered true :state :absent}
        (= current future) (do (delete-file! file)
                               {:file file :recovered (not (exists? file))
                                :state :deleted})
        :else {:file file :recovered false :state :unknown-bytes})

      (= current (get originals file))
      {:file file :recovered true :state :original}

      (= current future)
      (do (write-source! file (get originals file))
          {:file file
           :recovered (= (get originals file) (read-source file))
           :state :restored})

      :else
      {:file file :recovered false :state :unknown-bytes})))

(defn- rollback-created-directories!
  [{:keys [exists? delete-file!]} directories]
  (mapv
    (fn [directory]
      (if-not (exists? directory)
        {:directory directory :recovered true :state :absent}
        (try
          (delete-file! directory)
          {:directory directory
           :recovered (not (exists? directory))
           :state :deleted}
          (catch Exception error
            {:directory directory
             :recovered false
             :state :not-empty-or-changed
             :cause-error (.getMessage error)}))))
    (reverse directories)))

(defn commit!
  "Commit one compiled mixed create/update file set through injected I/O."
  ([compiled]
   (commit! compiled
            {:read-source slurp
             :write-source! file-ops/atomic-write!
             :exists? #(.exists (io/file %))
             :create-directory!
             #(java.nio.file.Files/createDirectory
                (.toPath (io/file %))
                (make-array java.nio.file.attribute.FileAttribute 0))
             :delete-file! #(java.nio.file.Files/delete (.toPath (io/file %)))}))
  ([compiled {:keys [read-source write-source! exists? create-directory!] :as io}]
   (try
     (let [created-files (set (:created-files compiled))
           planned-directories (vec (:created-directories compiled))
           created-directories (atom [])
           originals (:original-sources compiled)
           futures (:future-sources compiled)
           ordered-files (vec (concat (keys originals) created-files))]
       (when-not (:ok compiled)
         (throw (ex-info "Commit requires a successful compiled extraction"
                         {:error-type :invalid-compiled-extraction})))
       (doseq [[file original] originals]
         (when-not (and (exists? file) (= original (read-source file)))
           (throw (ex-info "Extraction source changed before commit"
                           {:error-type :source-hash-mismatch :file file}))))
       (doseq [file created-files]
         (when (exists? file)
           (throw (ex-info "Extraction target appeared before commit"
                           {:error-type :target-already-exists :file file}))))
       (doseq [directory planned-directories]
         (when (exists? directory)
           (throw (ex-info "Extraction target parent appeared before commit"
                           {:error-type :target-parent-state-changed
                            :directory directory}))))
       (try
         (doseq [directory planned-directories]
           (when (exists? directory)
             (throw (ex-info "Extraction target parent appeared during commit"
                             {:error-type :target-parent-state-changed
                              :directory directory})))
           (create-directory! directory)
           (swap! created-directories conj directory)
           (when-not (exists? directory)
             (throw (ex-info "Extraction target parent creation could not be verified"
                             {:error-type :target-parent-create-failed
                              :directory directory}))))
         (doseq [file ordered-files]
           (if (contains? created-files file)
             (when (exists? file)
               (throw (ex-info "Extraction target appeared during commit"
                               {:error-type :target-already-exists :file file})))
             (when-not (= (get originals file) (read-source file))
               (throw (ex-info "Extraction source changed during commit"
                               {:error-type :source-hash-mismatch :file file}))))
           (write-source! file (get futures file))
           (when-not (= (get futures file) (read-source file))
             (throw (ex-info "Extraction read-back verification failed"
                             {:error-type :read-back-hash-mismatch :file file}))))
         (let [receipt (build-receipt
                         (assoc compiled
                                :created-directories @created-directories))]
           {:ok true
            :operation :compiled-extraction
            :committed true
            :change-count 1
            :match-count (+ (:form-count compiled)
                            (:caller-edit-count compiled))
            :changed-file-count (count ordered-files)
            :caller-proof (:caller-proof compiled)
            :format (:format compiled)
            :receipt receipt
            :receipt-hash (:receipt-hash receipt)
            :verified {:whole-files true
                       :file-count (count ordered-files)
                       :read-back true
                       :read-back-hashes
                       (into (sorted-map)
                             (map (fn [[file source]]
                                    [file (source-hash source)]))
                             futures)}})
         (catch Exception cause
           (let [file-recovery (mapv #(rollback-file! io originals futures
                                                      created-files %)
                                     (reverse ordered-files))
                 directory-recovery
                 (rollback-created-directories! io @created-directories)
                 recovery (into file-recovery directory-recovery)
                 rolled-back (every? :recovered recovery)]
             {:ok false
              :error-type (if rolled-back
                            :extraction-write-failed
                            :extraction-recovery-required)
              :error (if rolled-back
                       "Extraction write failed; all files restored"
                       "Extraction write failed; manual recovery required")
              :cause-error (.getMessage cause)
              :cause-error-type (:error-type (ex-data cause))
              :rolled-back rolled-back
              :recovery recovery}))))
     (catch clojure.lang.ExceptionInfo error
       (merge {:ok false :error (.getMessage error)} (ex-data error)))
     (catch Exception error
       {:ok false
        :error-type :extraction-write-exception
        :error (.getMessage error)}))))

(defn undo!
  "Undo a compiled extraction receipt through the same guarded file-set rules."
  ([receipt]
   (undo! receipt
          {:read-source slurp
           :write-source! file-ops/atomic-write!
           :exists? #(.exists (io/file %))
           :create-directory!
           #(java.nio.file.Files/createDirectory
              (.toPath (io/file %))
              (make-array java.nio.file.attribute.FileAttribute 0))
           :delete-file! #(java.nio.file.Files/delete (.toPath (io/file %)))}))
  ([receipt {:keys [read-source write-source! exists? delete-file!
                    create-directory!] :as io}]
   (try
     (when-not (and (= receipt-version (:receipt-version receipt))
                    (= :compiled-extraction (:operation receipt))
                    (vector? (:files receipt)))
       (throw (ex-info "Invalid compiled extraction receipt"
                       {:error-type :invalid-extraction-receipt})))
     (doseq [{:keys [file result-source]} (:files receipt)]
       (when-not (and (exists? file) (= result-source (read-source file)))
         (throw (ex-info "Extraction result changed before undo"
                         {:error-type :stale-extraction-result :file file}))))
     (try
       (doseq [{:keys [file absent-before original-source]} (:files receipt)]
         (if absent-before
           (delete-file! file)
           (write-source! file original-source)))
       (let [directory-recovery
             (rollback-created-directories!
               io (:created-directories receipt))
             verified?
             (and
               (every?
                 (fn [{:keys [file absent-before original-source]}]
                   (if absent-before
                     (not (exists? file))
                     (and (exists? file) (= original-source (read-source file)))))
                 (:files receipt))
               (every? :recovered directory-recovery))]
         (if verified?
           {:ok true
            :operation :undo-compiled-extraction
            :verified {:whole-files true
                       :file-count (count (:files receipt))
                       :read-back true}}
           (throw (ex-info "Extraction undo read-back verification failed"
                           {:error-type :extraction-undo-read-back-failed}))))
       (catch Exception cause
         (let [directory-recovery
               (mapv
                 (fn [directory]
                   (if (exists? directory)
                     {:directory directory :recovered true :state :present}
                     (try
                       (create-directory! directory)
                       {:directory directory
                        :recovered (exists? directory)
                        :state :recreated}
                       (catch Exception error
                         {:directory directory
                          :recovered false
                          :state :recreate-failed
                          :cause-error (.getMessage error)}))))
                 (:created-directories receipt))
               recovery
               (mapv
                 (fn [{:keys [file result-source]}]
                   (let [current (file-state io file)]
                     (cond
                       (= current result-source)
                       {:file file :recovered true :state :result}

                       (or (nil? current)
                           (= current (:original-source
                                        (first (filter #(= file (:file %))
                                                       (:files receipt))))))
                       (do (write-source! file result-source)
                           {:file file
                            :recovered (= result-source (read-source file))
                            :state :restored-result})

                       :else
                       {:file file :recovered false :state :unknown-bytes})))
                 (reverse (:files receipt)))
               recovery (into directory-recovery recovery)
               recovered (every? :recovered recovery)]
           {:ok false
            :error-type (if recovered
                          :extraction-undo-failed
                          :extraction-undo-recovery-required)
            :error (if recovered
                     "Extraction undo failed; transaction result restored"
                     "Extraction undo failed; manual recovery required")
            :cause-error (.getMessage cause)
            :rolled-back recovered
            :recovery recovery})))
     (catch clojure.lang.ExceptionInfo error
       (merge {:ok false :error (.getMessage error)} (ex-data error)))
     (catch Exception error
       {:ok false
        :error-type :extraction-undo-exception
        :error (.getMessage error)}))))
