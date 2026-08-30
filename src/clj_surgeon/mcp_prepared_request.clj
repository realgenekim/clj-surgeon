(ns clj-surgeon.mcp-prepared-request
  "Pure projection of complete forms-read evidence into an inert edit template."
  (:require
   [cheshire.core :as json]
   [clj-surgeon.structural-lens :as structural-lens]
   [clojure.string :as str])
  (:import
   (java.nio.charset StandardCharsets)
   (java.nio.file Paths)
   (java.security MessageDigest)))

(def coaching-text
  (str "If you independently decide to edit these exact selections, fill the "
       "null replacement at every path listed in `caller_holes`. Then submit "
       "`prepared_request.arguments` once to `edit_clojure`. Otherwise, ignore "
       "`prepared_request`."))

(def prepared-request-schema
  {:type "object"
   :additionalProperties false
   :properties
   {"tool" {:type "string" :enum ["edit_clojure"]}
    "executable" {:type "boolean" :enum [false]}
    "write_authority" {:type "boolean" :enum [false]}
    "arguments"
    {:type "object"
     :additionalProperties false
     :properties
     {"workspace_root" {:type "string" :minLength 1}
      "edits"
      {:type "array"
       :minItems 1
       :maxItems 6
       :items
       {:type "object"
        :additionalProperties false
        :properties
        {"file" {:type "string" :minLength 1}
         "within" {:type "object"
                   :additionalProperties false
                   :properties {"form" {:type "string" :minLength 1}}
                   :required ["form"]}
         "from" {:type "string" :minLength 1}
         "to" {:type "null"}
         "matches" {:type "integer" :enum [1]}}
        :required ["file" "within" "from" "to" "matches"]}}}
     :required ["workspace_root" "edits"]}
    "caller_holes"
    {:type "array"
     :minItems 1
     :maxItems 6
     :uniqueItems true
     :items {:type "string"
             :pattern "^arguments\\.edits\\[[0-5]\\]\\.to$"}}}
   :required ["tool" "executable" "write_authority" "arguments"
              "caller_holes"]})

(defn- public-key
  [key]
  (cond
    (string? key) key
    (keyword? key) (name key)
    :else (throw (ex-info "Canonical public JSON keys must be strings or keywords"
                          {:key key}))))

(defn- canonical-json-value
  [value]
  (cond
    (map? value)
    (let [entries (map (fn [[key child]]
                         [(public-key key) (canonical-json-value child)])
                       value)
          keys (map first entries)]
      (when-not (= (count keys) (count (distinct keys)))
        (throw (ex-info "Canonical public JSON keys collide"
                        {:keys keys})))
      (into (sorted-map) entries))

    (vector? value) (mapv canonical-json-value value)
    (sequential? value) (mapv canonical-json-value value)
    :else value))

;; @spec MCP-OP-PREP-REQ-002
(defn canonical-json-bytes
  "Return deterministic UTF-8 JSON bytes for a public JSON-shaped value."
  [value]
  (.getBytes (json/generate-string (canonical-json-value value))
             StandardCharsets/UTF_8))

(defn- hex
  [bytes]
  (apply str (map #(format "%02x" (bit-and 0xff %)) bytes)))

;; @spec MCP-OP-PREP-REQ-002
(defn descriptor-sha256
  "Hash the exact canonical descriptor bytes."
  [descriptor]
  (hex (.digest (MessageDigest/getInstance "SHA-256")
                (canonical-json-bytes descriptor))))

(def ^:private sha256-pattern #"[0-9a-f]{64}")
(def ^:private supported-platforms
  {".clj" ["clj"]
   ".cljs" ["cljs"]
   ".cljc" ["clj" "cljs"]})
(def ^:private forbidden-artifacts
  #{:basis :prepared_basis :prepared-basis :continuation :retry_template
    :retry-template :prepared_request :next_call :verification_job
    :failed_request :failures :selection_failures})

(defn- exact-sha256?
  [value]
  (and (string? value) (boolean (re-matches sha256-pattern value))))

(defn- project-relative-file?
  [file]
  (and (string? file)
       (seq file)
       (not (str/includes? file "\\"))
       (not (str/starts-with? file "/"))
       (not (re-find #"(?i)^[a-z]:" file))
       (try
         (let [path (Paths/get file (make-array String 0))
               normalized (.normalize path)]
           (and (not (.isAbsolute path))
                (= file (.toString normalized))
                (not= ".." (some-> normalized (.getName 0) str))))
         (catch Exception _ false))))

(defn- canonical-root?
  [root]
  (and (string? root)
       (seq root)
       (try
         (let [path (Paths/get root (make-array String 0))]
           (and (.isAbsolute path)
                (= root (.toString (.normalize path)))))
         (catch Exception _ false))))

(defn- suffix
  [file]
  (some #(when (str/ends-with? file %) %) (keys supported-platforms)))

(defn- absent-artifacts?
  [value]
  (not-any? #(contains? value %) forbidden-artifacts))

(defn- non-negative-integer?
  [value]
  (and (integer? value) (not (neg? value))))

(defn- position?
  [value]
  (and (map? value)
       (non-negative-integer? (:line value))
       (non-negative-integer? (:character value))))

(defn- position<=?
  [left right]
  (not (pos? (compare [(:line left) (:character left)]
                      [(:line right) (:character right)]))))

(defn- range?
  [value]
  (and (map? value)
       (position? (:start value))
       (position? (:end value))
       (position<=? (:start value) (:end value))))

(defn- range-contained?
  [outer inner]
  (and (position<=? (:start outer) (:start inner))
       (position<=? (:end inner) (:end outer))))

(defn- range-fits-source?
  [value source]
  (let [start (:start value)
        end (:end value)
        lines (str/split source #"\n" -1)
        line-span (dec (count lines))
        expected-end-character
        (if (zero? line-span)
          (+ (:character start) (count (first lines)))
          (count (last lines)))]
    (and (= (- (:line end) (:line start)) line-span)
         (contains? #{expected-end-character (inc expected-end-character)}
                    (:character end)))))

(defn- selection-names-owner?
  [form-range selection-range source owner]
  (let [form-start (:start form-range)
        selection-start (:start selection-range)
        selection-end (:end selection-range)
        line-index (- (:line selection-start) (:line form-start))
        lines (str/split source #"\n" -1)
        line (get lines line-index)
        start-character (if (zero? line-index)
                          (- (:character selection-start)
                             (:character form-start))
                          (:character selection-start))
        end-character (if (zero? line-index)
                        (- (:character selection-end)
                           (:character form-start))
                        (:character selection-end))]
    (and (= (:line selection-start) (:line selection-end))
         (string? line)
         (<= 0 start-character)
         (< start-character end-character)
         (<= end-character (count line))
         (= owner (subs line start-character end-character)))))

(defn- form-evidence?
  [file file-hash expected-platforms form]
  (let [source (:source form)
        owner (:name form)
        anchor (:source_anchor form)
        form-range (:range anchor)
        selection-range (:selection_range anchor)]
    (and (map? form)
         (string? source)
         (seq source)
         (string? owner)
         (not (str/blank? owner))
         (string? (:form_type form))
         (not (str/blank? (:form_type form)))
         (not= "ns" (:form_type form))
         (= expected-platforms (:platforms form))
         (= file (:file form) (:file anchor))
         (= file-hash (:file_hash form) (:source_sha256 anchor))
         (exact-sha256? (:hash form))
         (= (:hash form) (structural-lens/source-hash source))
         (= owner (:owner anchor))
         (pos-int? (:line form))
         (pos-int? (:end_line form))
         (<= (:line form) (:end_line form))
         (range? form-range)
         (range? selection-range)
         (range-contained? form-range selection-range)
         (range-fits-source? form-range source)
         (selection-names-owner? form-range selection-range source owner)
         (= (dec (:line form)) (get-in form-range [:start :line]))
         (= (dec (:end_line form)) (get-in form-range [:end :line])))))

(defn- descriptor
  [result forms]
  {:tool "edit_clojure"
   :executable false
   :write_authority false
   :arguments
   {:workspace_root (:workspace_root result)
    :edits
    (mapv (fn [form]
            {:file (:file form)
             :within {:form (:name form)}
             :from (:source form)
             :to nil
             :matches 1})
          forms)}
   :caller_holes
   (mapv #(format "arguments.edits[%d].to" %) (range (count forms)))})

(defn- eligible-descriptor
  [result]
  (let [rows (:results result)
        row (first rows)
        forms (:forms row)
        file (:file row)
        file-suffix (and (string? file) (suffix file))
        file-hash (:file_hash row)
        source-characters (when (vector? forms)
                            (reduce + 0 (map #(count (:source %)) forms)))
        owners (when (vector? forms) (mapv :name forms))]
    (when (and (map? result)
               (true? (:ok result))
               (true? (:read_complete result))
               (= "none" (:next_action result))
               (= "inspect_clojure" (:operation result))
               (= 1 (:request_count result))
               (= 1 (:file_count result))
               (= 1 (count rows))
               (= "forms" (:operation row))
               (project-relative-file? file)
               file-suffix
               (exact-sha256? file-hash)
               (vector? forms)
               (<= 1 (count forms) 6)
               (= (count forms) (:form_count row))
               (= source-characters (:source_character_count row))
               (= source-characters (:source_character_count result))
               (= (count owners) (count (distinct owners)))
               (every? #(form-evidence?
                          file file-hash (get supported-platforms file-suffix) %)
                       forms)
               (canonical-root? (:workspace_root result))
               (= {file file-hash} (:file_hashes result))
               (or (not (contains? result :snapshot_guards))
                   (= (:file_hashes result) (:snapshot_guards result)))
               (absent-artifacts? result)
               (absent-artifacts? row))
      (let [value (descriptor result forms)]
        (when (<= (count (canonical-json-bytes value)) 4096)
          value)))))

;; @spec MCP-OP-PREP-REQ-001
;; @spec MCP-OP-PREP-REQ-003
;; @spec MCP-OP-PREP-REQ-004
;; @spec MCP-OP-PREP-REQ-006
;; @spec MCP-OP-PREP-REQ-008
(defn project-result
  "Attach one inert prepared request to an intrinsically eligible read result."
  [result]
  (try
    (if-let [prepared (eligible-descriptor result)]
      (assoc result :prepared_request prepared)
      result)
    (catch Exception _ result)))
