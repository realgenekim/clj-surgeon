(ns clj-surgeon.mcp-write-refusal
  "Pure, source-free projections for bounded MCP write refusals."
  (:require
   [cheshire.core :as json]
   [clj-surgeon.structural-lens :as structural-lens]
   [clojure.string :as str])
  (:import
   (java.nio.file Paths)))

(def ^:private evidence-version 1)
(def ^:private ordering-version 1)
(def ^:private row-limit 128)
(def public-byte-budget
  "The one public MCP payload budget. Every bounded public result shares it."
  32640)

(defn- canonical-value
  [value]
  (cond
    (map? value)
    (into (sorted-map-by #(compare (pr-str %1) (pr-str %2)))
          (map (fn [[key item]] [key (canonical-value item)]))
          value)

    (set? value)
    (mapv canonical-value (sort-by pr-str value))

    (sequential? value)
    (mapv canonical-value value)

    :else value))

(defn- canonical-sha256
  [value]
  (structural-lens/source-hash (pr-str (canonical-value value))))

(defn- normalized-path
  [path]
  (str/replace (str path) "\\" "/"))

(defn- project-relative-file
  [project-root file]
  (let [file-path (Paths/get (str file) (make-array String 0))]
    (if (and project-root (.isAbsolute file-path))
      (let [root-path (Paths/get (str project-root) (make-array String 0))]
        (if (.startsWith file-path root-path)
          (normalized-path (.relativize root-path file-path))
          (normalized-path file-path)))
      (normalized-path file-path))))

(defn- closed-row
  [project-root scope item]
  (let [base (select-keys item [:line :end-line :address])
        base (assoc base
                    :file (project-relative-file project-root (:file item))
                    :scope-kind (:kind scope))]
    (case (:kind scope)
      :form (assoc base :owner-kind :form :owner-name (:owner item))
      :namespace (assoc base
                        :owner-kind :namespace
                        :owner-name (:name scope))
      :root base)))

(defn project-relative-counts
  "Project compiler count keys into caller-visible project-relative paths."
  [project-root counts]
  (when counts
    (into {}
          (map (fn [[file count]]
                 [(project-relative-file project-root file) count]))
          counts)))

(defn- relative-snapshot-guards
  [project-root guards]
  (into {}
        (map (fn [[file source-sha256]]
               [(project-relative-file project-root file) source-sha256]))
        guards))

(defn generic-count-mismatch-evidence
  ;; @spec MCP-OP-WRITE-REFUSAL-001
  "Project frozen compiler facts into complete, inert refusal evidence."
  [{:keys [operation change-index change-id files scope matcher expectation
           expected-count actual-count per-file-counts per-form-counts items
           snapshot-guards project-root]}]
  (let [files (mapv #(project-relative-file project-root %) files)
        scope (or scope {:kind :root})
        selector-sha256
        (canonical-sha256
         {:files files
          :scope scope
          :matcher matcher
          :expectation expectation})
        subject {:change-index change-index
                 :change-id change-id
                 :selector-sha256 selector-sha256}
        rows (mapv #(closed-row project-root scope %) items)
        guards (relative-snapshot-guards project-root snapshot-guards)]
    (cond->
     {:version evidence-version
      :operation operation
      :family :generic-count-mismatch
      :failed-stage :intent-compilation
      :subject subject
      :expected-count expected-count
      :actual-count actual-count
      :per-file-counts (project-relative-counts project-root per-file-counts)
      :items rows
      :available-count (count rows)
      :returned-count (count rows)
      :omitted-count 0
      :truncated false
      :snapshot-guards guards
      :authority false
      :write-authority false}
      per-form-counts
      (assoc :per-form-counts
             (project-relative-counts project-root per-form-counts)))))

(defn- public-key
  [key]
  (cond
    (keyword? key) (keyword (str/replace (name key) "-" "_"))
    (symbol? key) (str key)
    :else key))

(defn public-evidence
  "Convert internal evidence keys to the stable MCP JSON vocabulary."
  [value]
  (cond
    (map? value)
    (into {}
          (map (fn [[key item]]
                 [(public-key key) (public-evidence item)]))
          value)

    (vector? value)
    (mapv public-evidence value)

    (sequential? value)
    (mapv public-evidence value)

    :else value))

(defn- candidate-continuation
  [evidence returned-count]
  (let [available-count (:available_count evidence)
        query-data [(:version evidence)
                    (:operation evidence)
                    (:family evidence)
                    (:subject evidence)
                    ordering-version
                    (:snapshot_guards evidence)]]
    {:version evidence-version
     :executable false
     :authority false
     :write_authority false
     :operation (:operation evidence)
     :refusal_type "expect-count-mismatch"
     :family (:family evidence)
     :subject (:subject evidence)
     :ordering_version ordering-version
     :snapshot_guards (:snapshot_guards evidence)
     :next_offset returned-count
     :page_limit row-limit
     :remaining_count (- available-count returned-count)
     :candidate_query_sha256 (canonical-sha256 query-data)}))

(defn- evidence-prefix
  [evidence returned-count]
  (let [available-count (:available_count evidence)
        omitted-count (- available-count returned-count)
        truncated? (pos? omitted-count)]
    (cond->
     (assoc evidence
            :items (subvec (:items evidence) 0 returned-count)
            :returned_count returned-count
            :omitted_count omitted-count
            :truncated truncated?)
      truncated?
      (assoc :candidate_continuation
             (candidate-continuation evidence returned-count)))))

(defn json-bytes
  "Serialized size of one public payload in UTF-8 bytes."
  [value]
  (count (.getBytes (json/generate-string value) "UTF-8")))

(defn- bounded-summary-result
  [result summarize]
  (let [summary (summarize result)]
    (cond-> result
      (string? summary) (assoc :error summary))))

(defn- candidate-result
  [result evidence returned-count]
  (assoc result :write_refusal_evidence
         (evidence-prefix evidence returned-count)))

(defn- fitting-result
  [result evidence]
  (let [maximum (min row-limit (:available_count evidence))]
    (some (fn [returned-count]
            (let [candidate (candidate-result result evidence returned-count)]
              (when (<= (json-bytes candidate) public-byte-budget)
                candidate)))
          (range maximum -1 -1))))

(defn- fail-empty-result
  [result summarize]
  {:ok false
   :operation (:operation result)
   :error_type (or (:error_type result) "expect-count-mismatch")
   :write_refusal_evidence_omitted "output-budget"
   :failed_stage :intent-compilation
   :source_unchanged true
   :mutation_attempted false
   :write_authority false})

(defn bound-public-refusal
  ;; @spec MCP-OP-WRITE-REFUSAL-001
  "Fit complete public evidence inside the MCP result budget or fail empty."
  [result summarize]
  (if-let [evidence (:write_refusal_evidence result)]
    (let [evidence (public-evidence evidence)
          direct (fitting-result result evidence)
          summarized (when-not direct
                       (fitting-result
                        (bounded-summary-result result summarize)
                        evidence))]
      (or direct summarized (fail-empty-result result summarize)))
    result))

;; @spec MCP-OP-ADMIT-069
;; @spec MCP-OP-ADMIT-085
(defn bound-public-payload
  "Trim named collection keys until one public result fits the shared budget.

  `bound-public-refusal` fits a write refusal's structured evidence. This is
  its sibling for results whose unbounded growth lives in ordinary receipt
  collections rather than in `write_refusal_evidence`: the collections named
  by `trimmable` are shortened, longest first. Both fit inside
  `public-byte-budget`, which is the contract; neither invents a second budget.

  The omission record is cumulative. Reporting only the last step's loss would
  understate a payload trimmed several times over, and a reader who sees
  `payload_omitted` at all is asking exactly one question: how much am I not
  being shown?"
  [result trimmable]
  (let [annotation-keys [:payload_truncated :payload_truncation
                         :payload_omitted :payload_omitted_bytes]
        content (fn [value] (json-bytes (apply dissoc value annotation-keys)))
        original-bytes (content result)]
    (if (<= original-bytes public-byte-budget)
      result
      (loop [current result
             omitted {}]
        (let [candidates (->> trimmable
                              (map (fn [key] [key (count (get current key))]))
                              (filter (fn [[_ n]] (pos? n)))
                              (sort-by second >))]
          (if (empty? candidates)
            (assoc current
                   :payload_truncated true
                   :payload_truncation "public-byte-budget"
                   :payload_omitted omitted
                   :payload_omitted_bytes (- original-bytes (content current)))
            (let [[key n] (first candidates)
                  kept (max 0 (dec (quot (* n 2) 3)))
                  omitted (update omitted key (fnil + 0) (- n kept))
                  trimmed (update current key #(vec (take kept %)))
                  next-result (assoc trimmed
                                     :payload_truncated true
                                     :payload_truncation "public-byte-budget"
                                     :payload_omitted omitted
                                     :payload_omitted_bytes
                                     (- original-bytes (content trimmed)))]
              (if (<= (json-bytes next-result) public-byte-budget)
                next-result
                (recur trimmed omitted)))))))))
