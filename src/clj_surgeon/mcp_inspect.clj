(ns clj-surgeon.mcp-inspect
  "Pure contract and batch evaluator for the read-only inspect_clojure MCP tool."
  (:require
   [cheshire.core :as json]
   [clj-surgeon.edit-dsl :as edit-dsl]
   [clj-surgeon.mcp-contract :as mcp-contract]
   [clj-surgeon.mcp-operation :as mcp-operation]
   [clj-surgeon.mcp-paths :as mcp-paths]
   [clj-surgeon.mcp-source-anchor :as source-anchor]
   [clj-surgeon.outline :as outline]
   [clj-surgeon.show-form :as show-form]
   [clj-surgeon.structural-lens :as structural-lens]
   [clj-surgeon.study :as study]
   [clojure.string :as str]
   [clojure.walk :as walk]
   [rewrite-clj.node :as node]
   [rewrite-clj.parser :as parser]))

(def max-requests 64)
(def max-files 32)
(def max-forms 128)
(def default-output-limits
  {:per-request-source 65536
   :per-request-result 65536
   :aggregate-result 262144})

(def ^:private top-fields #{"requests" "expect" "snapshot_guards"})
(def ^:private required-top-fields #{"requests" "expect"})
(def ^:private top-expect-fields #{"requests" "files"})
(def ^:private common-request-fields #{"id" "operation" "file"})
(def ^:private operation-fields
  {"forms" (into common-request-fields ["forms" "expect" "include_source"])
   "outline" (conj common-request-fields "include_string_symbols")
   "match" (into common-request-fields ["match" "inside" "expect"])
   "xray" (conj common-request-fields "expression")
   "deps" (into common-request-fields ["form" "limit"])
   "topo" (conj common-request-fields "limit")
   "ls-deps" (into common-request-fields ["form" "limit"])
   "ls-extract" (into common-request-fields ["form" "limit"])})
(def ^:private operation-required
  {"forms" (into common-request-fields ["forms" "expect"])
   "outline" common-request-fields
   "match" (conj common-request-fields "match")
   "xray" (get operation-fields "xray")
   "deps" common-request-fields
   "topo" common-request-fields
   "ls-deps" (conj common-request-fields "form")
   "ls-extract" (conj common-request-fields "form")})

(defn- field-name
  [key]
  (cond
    (keyword? key) (if-let [namespace (namespace key)]
                     (str namespace "/" (name key))
                     (name key))
    (string? key) key
    :else (str key)))

(defn- field
  [m key]
  (if (contains? m key)
    (get m key)
    (get m (keyword key))))

(defn- present?
  [m key]
  (or (contains? m key) (contains? m (keyword key))))

;; @spec MCP-OP-STUDY-046
(def refusal-reasons
  "Every reason an `inspect_clojure` request refusal can carry.

  ENUMERATED here and enforced where refusals are CONSTRUCTED: `refuse!`
  will not build a refusal whose reason is absent from this set, so a new
  reason is a deliberate edit here rather than a string a caller discovers in
  the field. The witness drives one fixture per member through the public
  entrance and asserts the set it OBSERVES equals this one — the runtime is
  the enumeration, and a source scan may only complement it.

  Field evidence (Sol O2 round-3 review, sections 3 and 9): the round-3
  ratchet read the set out of the source with `(refuse! :([a-z0-9-]+)`, which
  sees only a literal reason at a literal call site. `unique-strings!` takes
  its reason as an ARGUMENT and the forms validator passes `:duplicate-form`,
  so a reachable refusal was missing from the scanned 22 — and Sol's rung D
  showed the same escape is one `(identity :reason)` away for any of the
  others, with the whole suite green."
  #{:aggregate-expectation-mismatch
    :boolean
    :duplicate-form
    :duplicate-id
    :empty-snapshot-guards
    :expected-object
    :invalid-relative-source-path
    :invalid-snapshot-hash
    :invalid-study-limit
    :missing-fields
    :missing-snapshot-guards
    :mixed-request-ids
    :non-blank-string
    :non-empty-array
    :non-negative-integer
    :operation-required
    :positive-integer
    :request-expectation-mismatch
    :too-many-files
    :too-many-forms
    :too-many-requests
    :unknown-fields
    :unknown-operation})

;; @spec MCP-OP-STUDY-046
(defn- refuse!
  [reason path message & [data]]
  ;; A reason outside `refusal-reasons` is a defect in this namespace, not a
  ;; bad request: it makes the enumeration false at the moment it is used.
  ;; It is thrown as a plain exception rather than an `ex-info`, precisely so
  ;; the evaluator's `catch clojure.lang.ExceptionInfo` cannot turn it into a
  ;; refusal a caller would read as its own fault. Rung (e): the unenumerated
  ;; reason is unrepresentable rather than merely detected.
  (when-not (contains? refusal-reasons reason)
    (throw (IllegalArgumentException.
             (str "inspect_clojure refusal reason is not enumerated in "
                  "clj-surgeon.mcp-inspect/refusal-reasons: " (pr-str reason)))))
  (throw
    (ex-info
      message
      (merge
        {:ok false
         :operation "inspect_clojure"
         :error-type :invalid-mcp-request
         :reason reason
         :path path
         :error message
         :read_complete false
         :source_unchanged true
         :next_action "correct_request"}
        data))))

(def ^:private minimal-request-examples
  {[] {"requests" [{"id" "r1" "operation" "outline" "file" "src/example.clj"}]
       "expect" {"requests" 1 "files" 1}}
   ["expect"] {"requests" 1 "files" 1}
   ["requests" :index] {"id" "r1" "operation" "outline"
                        "file" "src/example.clj"}
   ["requests" :index "expect"] {"forms" 1}})

(defn- example-path
  [path]
  (mapv #(if (integer? %) :index %) (vec path)))

;; @spec MCP-OP-FIELD-001
;; @spec MCP-OP-FIELD-006
(defn- minimal-request-shape
  "The smallest valid object at `path`, restricted to that path's required
   fields. Returns nil when no example covers every required field, so the
   refusal never shows a shape it cannot stand behind.

   Every registered example is pinned to the live validators by test, so an
   example that stops validating fails the suite rather than the caller."
  [path required]
  (when-let [example (get minimal-request-examples (example-path path))]
    (let [shape (into (sorted-map) (select-keys example (vec required)))]
      (when (= (set (keys shape)) (set required))
        shape))))

;; @spec MCP-OP-FIELD-001
;; @spec MCP-OP-FIELD-006
(defn- missing-fields-evidence
  [path required missing]
  (let [shape (minimal-request-shape path required)]
    (cond-> {:missing (vec missing)
             :required (vec (sort required))}
      shape (assoc :minimal_request shape))))

(defn- validate-fields!
  [value allowed required path]
  (when-not (map? value)
    (refuse! :expected-object path "Expected a JSON object"))
  (let [actual (set (map field-name (keys value)))
        unknown (vec (sort (remove allowed actual)))
        missing (vec (sort (remove actual required)))]
    (when (seq unknown)
      (refuse! :unknown-fields path "Request contains unknown fields"
               {:unknown unknown}))
    (when (seq missing)
      (refuse! :missing-fields path "Request is missing required fields"
               (missing-fields-evidence path required missing)))))

(defn- nonblank-string!
  [value path]
  (when-not (and (string? value) (not (str/blank? value)))
    (refuse! :non-blank-string path "Expected a non-blank string"))
  value)

(defn- nonempty-array!
  [value path]
  (when-not (and (vector? value) (seq value))
    (refuse! :non-empty-array path "Expected a non-empty JSON array"))
  value)

(defn- positive-integer!
  [value path]
  (when-not (and (integer? value) (pos? value))
    (refuse! :positive-integer path "Expected a positive integer"))
  value)

(defn- non-negative-integer!
  [value path]
  (when-not (and (integer? value) (not (neg? value)))
    (refuse! :non-negative-integer path "Expected a non-negative integer"))
  value)

(defn- study-limit!
  [value path]
  (when-not (and (integer? value) (pos? value) (<= value 16384))
    (refuse! :invalid-study-limit path
             "Expected a study limit between 1 and 16384 JSON characters"))
  value)

(defn- boolean!
  [value path]
  (when-not (instance? Boolean value)
    (refuse! :boolean path "Expected a boolean"))
  value)

(defn- source-path!
  [value path]
  (when-not (mcp-paths/relative-source-path? value)
    (refuse! :invalid-relative-source-path path
             "Expected a project-relative .clj, .cljs, or .cljc path without parent traversal"))
  value)

(defn- unique-strings!
  [values path duplicate-reason]
  (loop [seen #{}
         index 0]
    (when (< index (count values))
      (let [value (nonblank-string! (nth values index) (conj path index))]
        (when (contains? seen value)
          (refuse! duplicate-reason (conj path index)
                   "Values must be unique" {:value value}))
        (recur (conj seen value) (inc index)))))
  values)

;; @spec MCP-OP-READ-GUARD-001
(defn- snapshot-guards!
  [value path]
  (when-not (map? value)
    (refuse! :expected-object path "snapshot_guards must be a JSON object"))
  (when (empty? value)
    (refuse! :empty-snapshot-guards path
             "snapshot_guards must contain at least one file hash"))
  (into
    (array-map)
    (map-indexed
      (fn [index [raw-file raw-hash]]
        (let [file (field-name raw-file)
              _ (when-not (mcp-paths/relative-source-path? file)
                  (refuse! :invalid-relative-source-path (conj path index "file")
                           "Expected a project-relative .clj, .cljs, or .cljc guard path without parent traversal"
                           {:failed-stage :snapshot :file file}))
              hash raw-hash]
          (when-not (string? hash)
            (refuse! :invalid-snapshot-hash (conj path file)
                     "Snapshot hashes must be strings"
                     {:file file}))
          (when-not (re-matches #"[0-9a-f]{64}" hash)
            (refuse! :invalid-snapshot-hash (conj path file)
                     "Snapshot hashes must be 64 lowercase hexadecimal characters"
                     {:file file}))
          [file hash])))
    value))

(defn- validate-forms-expect!
  [value path form-count]
  (validate-fields! value #{"forms"} #{"forms"} path)
  (let [expected (positive-integer! (field value "forms")
                                    (conj path "forms"))]
    (when-not (= form-count expected)
      (refuse! :request-expectation-mismatch (conj path "forms")
               "Declared form count does not match requested forms"
               {:expected expected :actual form-count}))
    {:forms expected}))

(defn- validate-match-expect!
  [value path]
  (validate-fields! value #{"matches"} #{} path)
  (cond-> {}
    (present? value "matches")
    (assoc :matches (non-negative-integer!
                      (field value "matches")
                      (conj path "matches")))))

(defn- validate-request!
  [request index]
  (let [path ["requests" index]]
    (when-not (map? request)
      (refuse! :expected-object path "Expected a JSON object"))
    (let [actual (set (map field-name (keys request)))]
      (when-let [missing (seq (sort (remove actual common-request-fields)))]
        (refuse! :missing-fields path "Request is missing required fields"
                 (missing-fields-evidence path common-request-fields missing))))
    (let [id (nonblank-string! (field request "id") (conj path "id"))
          operation (nonblank-string! (field request "operation")
                                      (conj path "operation"))
          allowed (get operation-fields operation)
          required (get operation-required operation)]
      (when-not allowed
        (refuse! :unknown-operation (conj path "operation")
                 "Unsupported inspect operation"
                 {:actual operation
                  :supported (vec (sort (keys operation-fields)))}))
      (validate-fields! request allowed required path)
      (let [file (source-path! (field request "file") (conj path "file"))]
        (case operation
          "forms"
          (let [raw-forms (nonempty-array! (field request "forms")
                                           (conj path "forms"))
                _ (when (> (count raw-forms) max-forms)
                    (refuse! :too-many-forms (conj path "forms")
                             "A forms request exceeds the maximum form count"
                             {:maximum max-forms :actual (count raw-forms)}))
                forms (unique-strings! raw-forms (conj path "forms")
                                       :duplicate-form)]
            (cond->
              {:id id :operation operation :file file :forms forms
               :expect (validate-forms-expect!
                         (field request "expect") (conj path "expect")
                         (count forms))}
              (present? request "include_source")
              (assoc :include-source
                     (boolean! (field request "include_source")
                               (conj path "include_source")))))

          "outline"
          (cond-> {:id id :operation operation :file file}
            (present? request "include_string_symbols")
            (assoc :include-string-symbols
                   (boolean! (field request "include_string_symbols")
                             (conj path "include_string_symbols"))))

          "match"
          (cond-> {:id id :operation operation :file file
                   :match (nonblank-string! (field request "match")
                                            (conj path "match"))}
            (present? request "inside")
            (assoc :inside (nonblank-string! (field request "inside")
                                             (conj path "inside")))
            (present? request "expect")
            (assoc :expect (validate-match-expect!
                             (field request "expect")
                             (conj path "expect"))))

          "xray"
          {:id id :operation operation :file file
           :expression (nonblank-string! (field request "expression")
                                         (conj path "expression"))}

          ("deps" "topo" "ls-deps" "ls-extract")
          (cond-> {:id id :operation operation :file file}
            (present? request "form")
            (assoc :form (nonblank-string! (field request "form")
                                           (conj path "form")))
            (present? request "limit")
            (assoc :limit (study-limit! (field request "limit")
                                        (conj path "limit")))))))))

(def ^:private operationless-forms-fields
  #{"id" "file" "forms" "expect" "include_source"})
(def ^:private operationless-forms-required
  #{"id" "file" "forms" "expect"})

(defn- normalize-request-ids!
  [requests]
  (doseq [[index request] (map-indexed vector requests)]
    (when-not (map? request)
      (refuse! :expected-object ["requests" index]
               "Expected a JSON object")))
  (let [supplied? (mapv #(present? % "id") requests)]
    (when (and (some true? supplied?) (some false? supplied?))
      (refuse! :mixed-request-ids ["requests"]
               "Request IDs must be either all supplied or all omitted"
               {:read_started false}))
    (if (every? true? supplied?)
      requests
      (mapv (fn [index request]
              (assoc request "id" (str "request-" (inc index))))
            (range)
            requests))))

(defn- complete-operationless-forms-request?
  [request]
  (let [actual (set (map field-name (keys request)))
        forms (field request "forms")
        expect (field request "expect")
        expect-fields (when (map? expect)
                        (set (map field-name (keys expect))))]
    (and (every? actual operationless-forms-required)
         (every? operationless-forms-fields actual)
         (vector? forms)
         (seq forms)
         (= #{"forms"} expect-fields))))

(defn- normalize-request-operations!
  [requests]
  (mapv
    (fn [index request]
      (if (present? request "operation")
        request
        (if (complete-operationless-forms-request? request)
          (assoc request "operation" "forms")
          (refuse! :operation-required ["requests" index "operation"]
                   "Inspect request requires an explicit operation"
                   {:read_started false
                    :supported (vec (sort (keys operation-fields)))
                    :supplied_fields
                    (vec (sort (map field-name (keys request))))}))))
    (range)
    requests))

(defn validate-inspect-params
  "Validate JSON-shaped inspect_clojure input and return normalized Clojure data."
  ;; @spec MCP-OP-READ-NORM-001
  ;; @spec MCP-OP-READ-NORM-002
  ;; @spec MCP-OP-READ-NORM-003
  ;; @spec MCP-OP-READ-NORM-004
  ;; @spec MCP-OP-READ-NORM-005
  [params]
  (let [params (mcp-contract/json-containers->clj params)]
    (try
      (validate-fields! params top-fields required-top-fields [])
      (let [raw-requests (nonempty-array! (field params "requests") ["requests"])
            _ (when (> (count raw-requests) max-requests)
                (refuse! :too-many-requests ["requests"]
                         "Inspect batch exceeds the maximum request count"
                         {:maximum max-requests :actual (count raw-requests)}))
            normalized-requests (-> raw-requests
                                    normalize-request-ids!
                                    normalize-request-operations!)
            requests (mapv validate-request! normalized-requests (range))
            snapshot-guards
            (when (present? params "snapshot_guards")
              (snapshot-guards! (field params "snapshot_guards")
                                ["snapshot_guards"]))
            request-files (set (map :file requests))
            missing-guards (when snapshot-guards
                             (vec (sort (remove #(contains? snapshot-guards %)
                                                request-files))))
            _ (when (seq missing-guards)
                (refuse! :missing-snapshot-guards ["snapshot_guards"]
                         "Every requested file must have a snapshot guard"
                         {:missing missing-guards}))
            _ (when (> (count snapshot-guards) max-files)
                (refuse! :too-many-files ["snapshot_guards"]
                         "Snapshot guards exceed the maximum file count"
                         {:maximum max-files :actual (count snapshot-guards)}))
            _ (loop [seen #{}
                     index 0]
                (when (< index (count requests))
                  (let [id (:id (nth requests index))]
                    (when (contains? seen id)
                      (refuse! :duplicate-id ["requests" index "id"]
                               "Request IDs must be unique" {:id id}))
                    (recur (conj seen id) (inc index)))))
            expected (field params "expect")
            _ (validate-fields! expected top-expect-fields
                                top-expect-fields ["expect"])
            expected-requests (positive-integer!
                                (field expected "requests")
                                ["expect" "requests"])
            expected-files (positive-integer!
                             (field expected "files")
                             ["expect" "files"])
            actual-requests (count requests)
            actual-files (count (distinct (map :file requests)))]
        (when-not (= expected-requests actual-requests)
          (refuse! :aggregate-expectation-mismatch ["expect" "requests"]
                   "Declared request count does not match the batch"
                   {:expected expected-requests :actual actual-requests}))
        (when-not (= expected-files actual-files)
          (refuse! :aggregate-expectation-mismatch ["expect" "files"]
                   "Declared file count does not match distinct request files"
                   {:expected expected-files :actual actual-files}))
        (when (> actual-files max-files)
          (refuse! :too-many-files ["expect" "files"]
                   "Inspect batch exceeds the maximum distinct file count"
                   {:maximum max-files :actual actual-files}))
        {:ok true
         :params (cond-> {:requests requests
                          :expect {:requests expected-requests
                                   :files expected-files}}
                   snapshot-guards (assoc :snapshot-guards snapshot-guards))})
      (catch clojure.lang.ExceptionInfo error
        (ex-data error)))))

;; @spec MCP-OP-STUDY-051
(defn- json-key
  "One receipt key, normalized to the spelling `structuredContent` publishes.

  The COMPLETE keyword, namespace included. `name` is not it: a receipt key
  that holds a path — `file_hashes` is keyed by `src/demo.clj` — comes back
  from a JSON round trip as the NAMESPACED keyword `:src/demo.clj`, and
  `name` renames it to `demo.clj`. The renderer walks the map it holds and
  the audit walks the map the client parsed, so that rename made the pointer
  the audit computed name a different leaf than the pointer the text printed.
  Field evidence (O2 round 7): the HTTP wire witness reported
  `[:file_hashes :demo.clj]` uncarried against a text carrying
  `file_hashes.src/demo.clj: a803…`, the moment carriage stopped accepting
  the hash's characters wherever in the text they appeared."
  [key]
  (if (keyword? key)
    (keyword (str/replace (subs (str key) 1) "-" "_"))
    key))

(defn json-data
  "Recursively normalize kernel EDN into stable JSON-compatible Clojure data."
  [value]
  (cond
    (map? value)
    (into (array-map)
          (map (fn [[key child]] [(json-key key) (json-data child)]))
          value)

    (vector? value) (mapv json-data value)
    (list? value) (mapv json-data value)
    (set? value) (mapv json-data (sort-by pr-str value))
    (keyword? value) (name value)
    (symbol? value) (str value)
    :else value))

(defn json-character-count
  "Count deterministic compact JSON characters after result normalization."
  [value]
  (count (json/generate-string (json-data value))))

;; @spec MCP-OP-STUDY-044
(def text-excluded-leaf-keys
  "The ONLY receipt facts the TEXT block deliberately does not carry, each
  with the reason it is kept out.

  The default is to RENDER. This set is the whole exception, a witness freezes
  it, and adding a member is a reviewable diff — never an implicit projection.

  - `:workspace_root` — an absolute filesystem path. MCP-OP-STUDY-026 already
    forbids the workspace root inside a refusal's `error` string; a text block
    travels into transcripts, logs, and screenshots that `structuredContent`
    does not, and the root names the operator's machine rather than the
    answer. Structured-content consumers still receive it."
  #{:workspace_root})

;; @spec MCP-OP-STUDY-044
(defn receipt-leaf-pairs
  "Every leaf of a receipt as `[path value]`, over the JSON shape a client is
  handed — so a renderer and a witness walk exactly what `structuredContent`
  carries, not what the kernel happens to hold.

  An EMPTY map or vector is a leaf. Field evidence (Sol O2 round-3 review,
  section 2): descending into `{}` and `[]` yielded nothing, so those receipt
  facts were not excluded by the enumerated set — they were invisible to the
  walker that DEFINES the criterion, and `results=[]` on a zero-result receipt
  reached no client. A shape the walker skips is an exclusion nobody wrote
  down."
  ([result] (receipt-leaf-pairs [] (json-data result)))
  ([path value]
   (cond
     (and (map? value) (seq value))
     (mapcat (fn [[key child]] (receipt-leaf-pairs (conj path key) child))
             value)

     (and (sequential? value) (seq value))
     (mapcat (fn [[index child]] (receipt-leaf-pairs (conj path index) child))
             (map-indexed vector value))

     :else [[path value]])))

;; @spec MCP-OP-STUDY-044
(defn- segment-spelling
  "The COMPLETE spelling of one path segment.

  @spec MCP-OP-STUDY-051 — `name` is not it. A receipt key that survives a
  JSON round trip as a NAMESPACED keyword — `file_hashes` holds
  `:src/demo.clj` once the wire receipt is read back — spells `demo.clj`
  under `name`, so the pointer the audit computed named a different leaf than
  the pointer the renderer printed (`file_hashes.src/demo.clj`). Field
  evidence: the HTTP wire witness reported `[:file_hashes :demo.clj]`
  uncarried against a text that rendered it, once carriage stopped accepting
  the hash's characters wherever they appeared."
  [segment]
  (cond
    (keyword? segment) (subs (str segment) 1)
    (string? segment) segment
    :else (str segment)))

(def ^:private pointer-segment-escapes
  "The characters a receipt path segment may not spell RAW inside a pointer.

  @spec MCP-OP-STUDY-052 — every one of them is a character this syntax has
  already spent: `.` joins segments, `[` and `]` wrap an index, `:` and `=`
  separate a pointer from its spelling, and `~` and `\\` are the two escape
  introducers themselves. `/` is escaped so the rule is RFC 6901's extended
  rather than a second, incompatible one — a caller who knows JSON Pointer
  decodes `~0` and `~1` unchanged.

  @spec MCP-OP-STUDY-053 — and every character in the UNICODE LINE-BOUNDARY
  CLASS, because a pointer that SPLITS is not a pointer: the renderer emitted
  `  bad` and `key: <value>` as two lines while the carriage predicate
  searched for the unsplit whole string, and the text published
  `10 of 10 rendered` over a leaf it did not carry. The class is the one
  `java.util.regex`'s `\\R` matches — `\\n`, `\\r`, the vertical tab, the form
  feed, NEL (U+0085) and the Unicode LINE and PARAGRAPH separators (U+2028,
  U+2029) — not the three characters that were named first: round nine found
  U+2028 and U+2029 raw, so `clojure.string/split-lines` saw one line where a
  caller's Unicode-aware splitter saw two. `\\t` is escaped with them because
  it is a delimiter of this rendering, not because it splits.

  The map is applied in ONE pass by `clojure.string/escape`, so no escape can
  be re-escaped and the encoding is decodable: after `~` comes exactly one of
  `0`-`7`, and after `\\` exactly one of `\\`, `n`, `r`, `t`, or `u` and four
  hex digits. `~7` is spent
  on the EMPTY segment (see `empty-segment-pointer`) and is therefore not in
  this map: no character produces it, so no segment can spell it by accident."
  {\\ "\\\\"
   \newline "\\n"
   \return "\\r"
   \tab "\\t"
   \u000B "\\u000B"
   \formfeed "\\u000C"
   \u0085 "\\u0085"
   \u2028 "\\u2028"
   \u2029 "\\u2029"
   \~ "~0"
   \/ "~1"
   \. "~2"
   \[ "~3"
   \] "~4"
   \: "~5"
   \= "~6"})

;; @spec MCP-OP-STUDY-052
(def ^:private empty-segment-pointer
  "The spelling of the EMPTY path segment — the one segment an escape map
  cannot reach.

  `~7` and nothing shorter. A segment that renders as NO characters is not an
  address: it erases the position it occupies, so `[\"\" 0]` and `[0]` — two
  different JSON paths — spell one pointer. `~` is escaped to `~0` inside
  every segment, so no non-empty segment can produce `~7`, and the encoding
  stays injective and decodable in one direction each."
  "~7")

;; @spec MCP-OP-STUDY-052
(defn escape-pointer-segment
  "One path segment, with every delimiter of this pointer syntax escaped.

  Field evidence (Sol O2 round-7 review, 2026-09-04, section 2): the segments
  were concatenated RAW, so the top-level key `\"a.b\"` and the nested path
  `[:a :b]` both spelled `a.b`. `text-line-index` is a SET, so ONE rendered
  `a.b: <value>` line discharged both leaves and the block declared one
  dropped while an audit of its own text found two — 587 disagreements across
  the allowance band. A pointer that cannot be decoded back to the path that
  made it is not an address.

  @spec MCP-OP-STUDY-052 — an escape map cannot reach the EMPTY segment:
  `str/escape` maps CHARACTERS, and the empty string has none, so the segment
  escaped to zero characters and the position it names was ERASED. Field
  evidence (Sol O2 round-9 review, 2026-09-04, section 1): the distinct JSON
  paths `[\"\" 0]` — an array under the empty top-level key — and `[0]` — the
  integer top-level key, published as the object key `\"0\"` — both spelled
  `[0]`, and a FITTING 32,731-byte public result at evidence allowance 237
  rendered one of them, named the other on its `dropped:` line, and declared
  32 dropped where an audit of that same text found 31."
  [segment]
  (if (empty? segment)
    empty-segment-pointer
    (str/escape segment pointer-segment-escapes)))

(defn leaf-label
  "`results[0].source_anchor.range.start.line` — the JSON pointer a caller
  reads the same fact back out of `structuredContent` with.

  @spec MCP-OP-STUDY-052 — INJECTIVE: distinct paths spell distinct pointers,
  because every character this rendering spends as a delimiter is escaped
  inside a segment. That is what makes a rendered line attributable to ONE
  leaf, and it is the identity the declaration and the audit both rest on."
  [path]
  (apply str
         (map-indexed (fn [index segment]
                        (cond
                          (integer? segment) (str "[" segment "]")
                          (zero? index) (escape-pointer-segment
                                          (segment-spelling segment))
                          :else (str "."
                                     (escape-pointer-segment
                                       (segment-spelling segment)))))
                      path)))

;; @spec MCP-OP-STUDY-054
(defn wire-member-name
  "The JSON object MEMBER NAME `structuredContent` publishes one receipt key
  as — the string a caller sees between the quotes.

  Stated HERE, once, because it is the identity the whole receipt rests on and
  it is NOT the key: the complete keyword including its namespace, a string as
  itself, `nil` as the EMPTY name, and everything else as its `str`. Cheshire
  spells `{:a 1}`, `{\"a\" 1}` as `{\"a\":1}`, `{nil 1}` as `{\"\":1}`, and
  `{0 1}`, `{\"0\" 1}` as `{\"0\":1}`, so four pairs of distinct Clojure keys
  are ONE member name on the wire, and a witness asserts this function against
  cheshire's own rendering rather than against a reading of it.

  Apply it to the key `json-key` produced, never to the raw key: the `-`-to-`_`
  normalization is part of what this namespace publishes, so `:a-b` and
  `:a_b` are a collision this code MAKES."
  [key]
  (cond
    (keyword? key) (subs (str key) 1)
    (string? key) key
    (nil? key) ""
    :else (str key)))

;; @spec MCP-OP-STUDY-054
(defn colliding-receipt-keys
  "The FIRST map inside a receipt that holds two DISTINCT keys publishing as
  ONE JSON object member name, as `{:path :member :keys}` — or nil.

  @spec MCP-OP-STUDY-054 — round eleven NAMED this as a safe residual
  (MCP-OP-STUDY-052): `:a` and `\"a\"` deliberately spell one pointer,
  \"because `structuredContent` publishes them as the same JSON object key.\"
  That is true of one key at a time and false of both at once. With both in
  one map the receipt publishes `{\"a\":1,\"a\":2}` — an object with duplicate
  member names, which ordinary decoders collapse to one member, so the
  STRUCTURED face is lossy before any rendering happens; and `leaf-label`
  hands both leaves one pointer, so the renderer prints the first, declares
  the second dropped, and `uncarried-leaves` finds the second carried by the
  first's identical line. Field evidence (Sol O2 round-11 review, 2026-09-04,
  section 2): declared and audited disagreed at allowances 102 and 100, and an
  ORDINARY 32,684-byte fitted public result declared four dropped against an
  audit of three.

  The question is asked about the WIRE, not about the pointer, because the
  pointer is only half the class: `0` and `\"0\"` spell the DISTINCT pointers
  `[0]` and `0` (MCP-OP-STUDY-052) and still publish as the one member `\"0\"`.

  `:keys` are the keys AS THE RECEIPT SPELLS THEM, so a refusal can name the
  two things a reader can tell apart; `:path` is the receipt path of the map,
  in the `json-key` spelling `receipt-leaf-pairs` uses, so `leaf-label` names
  it the same way it names every other address. The walk is in RECEIPT ORDER
  and returns the first collision, so the refusal is deterministic.

  Shape-for-shape the same descent as `json-data`, and for the same reason:
  a shape this walker declines to enter is a collision nobody sees."
  ([result] (colliding-receipt-keys [] result))
  ([path value]
   (cond
     (map? value)
     ;; `contains?`, never `get` — the first colliding pair this walk had to
     ;; find is `nil` against `""`, and a prior key of `nil` is FALSY: an
     ;; `if-let` here reported no collision for exactly the pair the reviewer
     ;; named. The absence of a member name and a member name whose key is
     ;; `nil` are different questions.
     (or (loop [entries (seq value) seen {}]
           (when entries
             (let [key (ffirst entries)
                   member (wire-member-name (json-key key))]
               (if (contains? seen member)
                 {:path path :member member :keys [(get seen member) key]}
                 (recur (next entries) (assoc seen member key))))))
         (first (keep (fn [[key child]]
                        (colliding-receipt-keys (conj path (json-key key)) child))
                      value)))

     (set? value)
     (first (keep-indexed (fn [index child]
                            (colliding-receipt-keys (conj path index) child))
                          (sort-by pr-str value)))

     (sequential? value)
     (first (keep-indexed (fn [index child]
                            (colliding-receipt-keys (conj path index) child))
                          value))

     :else nil)))

;; @spec MCP-OP-STUDY-044
(defn leaf-spelling
  "The characters `structuredContent` spells one leaf value with.

  A string is its own characters. Everything else is spelled as the JSON a
  client parses spells it — `null`, `{}`, `[]`, `true`, `1.25` — so a caller
  comparing the text against the receipt compares like with like. A BLANK
  string is spelled quoted (`\"\"`, `\"   \"`), because unquoted blank
  characters are not a spelling anyone can find."
  [value]
  (cond
    (nil? value) "null"
    (and (string? value) (str/blank? value)) (pr-str value)
    (string? value) value
    (and (map? value) (empty? value)) "{}"
    (and (sequential? value) (empty? value)) "[]"
    (coll? value) (json/generate-string value)
    :else (str value)))

;; @spec MCP-OP-STUDY-044
(defn value-less-leaf?
  "Is this leaf's value INDISTINGUISHABLE FROM ABSENCE inside a text block?

  `null`, `{}`, `[]`, and a blank string have no characters a reader could
  find and attribute to this leaf: `\"\"` matches at every index of every
  text, `{}` matches any object rendering, and `null` and `[]` occur inside
  unrelated words and lines. Such a leaf is carried by its LABEL or not at
  all.

  Field evidence (Sol O2 round-3 review, section 2): `leaf-rendered?`
  returned true for exactly these shapes without inspecting `text` at all —
  `nil_value present=false label_present=false` — so a second, unenumerated
  exclusion mechanism sat beside the frozen set, and the renderer and the
  witness agreed only because they shared it."
  [value]
  (or (nil? value)
      (and (string? value) (str/blank? value))
      (and (coll? value) (empty? value))))

;; @spec MCP-OP-STUDY-044
(def min-distinctive-spelling
  "How many characters a leaf's spelling needs before its APPEARANCE in a text
  is evidence that the text carries THAT leaf.

  A short spelling collides. Field evidence (Opus O2 round-4 review,
  2026-09-04, section 4): on a real `outline` receipt `file_read_count` could
  be changed from 1 to 0 with the published text BYTE-IDENTICAL — the text's
  only `1`s meaning `request_count` and `file_count` — and
  `results[0].platforms[0]` from the characters c-l-j to the characters
  n-o-n-e, while
  `uncarried-leaves` reported zero misses for both. The same class round three
  blocked on, moved from the value-less shapes to `{any value whose spelling
  occurs elsewhere in the text}`.

  Sixteen characters is the width at which a spelling stops being a word the
  rendering already contains: it covers every mode name, every `next_action`,
  every short form name and `operation` itself, and leaves hashes, paths, and
  source lines to be carried by their own characters. Measured against eight:
  sixteen costs 3.5% more on a small read (an `outline` of a three-form file,
  3,799 bytes against 3,669) and covers `operation`, every mode name, and
  every short form name that eight leaves open."
  16)

;; @spec MCP-OP-STUDY-044
(defn collidable-leaf?
  "Can this leaf's spelling appear in a text for a reason that has nothing to
  do with this leaf?

  A number or a boolean has no self-identifying characters AT ALL — `1`, `0`
  and `true` occur in counts, line numbers, clocks and flags throughout any
  rendering — and a short string or keyword collides with words the rendering
  already contains. Such a leaf is carried by its LABEL or not at all, exactly
  as a value-less leaf is: `pointer=spelling` is the only rendering of it a
  reader can attribute to the leaf.

  This SUBSUMES `value-less-leaf?`, which stays as the narrower statement of
  why `null`, `{}`, `[]` and a blank string can never be carried by value.

  @spec MCP-OP-STUDY-051 — since round seven this chooses the FORM a leaf's
  own line takes (`pointer=spelling` rather than `pointer: value`) and no
  longer decides CARRIAGE: no leaf is carried by anything but its own line,
  so a distinctive spelling is no safer from coincidence than a short one.
  The distinction is kept because the two forms read differently — `ok=true`
  is a flag, `source: (def answer 42)` is a value — and because dropping it
  would rewrite every published golden for no gain."
  [value]
  (or (value-less-leaf? value)
      (number? value)
      (boolean? value)
      (< (count (leaf-spelling value)) min-distinctive-spelling)))

;; @spec MCP-OP-STUDY-044
(def ^:private line-break-escapes
  "The characters a rendered VALUE may not spell raw on its own line.

  @spec MCP-OP-STUDY-053 — a line is a line, and a LINE is whatever a splitter
  of the published text calls one: the whole class `java.util.regex`'s `\\R`
  matches, which is `\\n` and `\\r` plus the vertical tab, the form feed, NEL
  (U+0085) and the Unicode LINE and PARAGRAPH separators (U+2028, U+2029).
  Field evidence (Sol O2 round-9 review, 2026-09-04, section 2): U+2028 and
  U+2029 were left RAW — `value= \"a\u2028b\" rendered= \"  k=a\u2028b\"
  clojure_split_lines= 1 unicode_R_lines= 2`.

  `\\` is escaped with them so the encoding stays decodable — after it comes
  `\\`, `n`, `r`, `t`, or `u` and four hex digits — and `\\t` because it is a
  delimiter of this rendering. Nothing else is touched: a value's other
  characters are the answer the caller came for."
  {\\ "\\\\"
   \newline "\\n"
   \return "\\r"
   \tab "\\t"
   \u000B "\\u000B"
   \formfeed "\\u000C"
   \u0085 "\\u0085"
   \u2028 "\\u2028"
   \u2029 "\\u2029"})

;; @spec MCP-OP-STUDY-053
(defn escape-line-breaks
  "One rendered spelling, with every character that would split its line
  escaped to its visible two-character form.

  This is what makes `leaf-lines` return ONE line by construction, which is
  what makes the renderer's declaration and an audit of its published text the
  same question. Field evidence (Sol O2 round-7 review, 2026-09-04, section
  3): the previous rendering split a multi-line spelling into an indented
  BLOCK and dropped its blank lines, so `\"a\\n\\nb\"` and `\"a\\nb\"` rendered
  byte-identically at one pointer — the same-type substitution
  MCP-OP-STUDY-051 forbids, one level down."
  [spelling]
  (str/escape spelling line-break-escapes))

(defn labelled-leaf
  "`results[0].platforms=[]` — the collidable leaf's own spelling, unindented,
  and the exact string its witness looks for."
  [path value]
  (str (leaf-label path) "=" (escape-line-breaks (leaf-spelling value))))

;; @spec MCP-OP-STUDY-044
(defn leaf-lines
  "The WHOLE LINE a rendering emits for ONE receipt leaf, and the only
  characters anywhere that carry it. Always exactly one.

  `  <pointer>: <spelling>` for a distinctive value and `  <pointer>=<spelling>`
  for a collidable one, with the pointer escaped by MCP-OP-STUDY-052 and the
  spelling by MCP-OP-STUDY-053.

  Both forms begin with the leaf's JSON POINTER, and pointers are INJECTIVE,
  so no leaf's line can be another leaf's. Both are SINGLE LINES BY
  CONSTRUCTION, so the line this returns is the line a splitter of the
  published text finds.

  @spec MCP-OP-STUDY-053 — the indented-block rendering of a multi-line
  spelling is withdrawn. It split one leaf across lines the carriage predicate
  never looked for, and it removed the value's blank lines, so two distinct
  values rendered identically at one pointer. A multi-line value now prints
  its `\\n` visibly, on its own line, whole.

  The vector is kept — one element — because it is the unit `leaf-carried?`
  asks the line index about and the unit the entry list joins."
  [path value]
  [(if (collidable-leaf? value)
     (str "  " (labelled-leaf path value))
     (str "  " (leaf-label path) ": "
          (escape-line-breaks (leaf-spelling value))))])

;; @spec MCP-OP-STUDY-051
(defn text-line-index
  "The set of WHOLE LINES a text is made of — the index every carriage
  question below is asked against.

  Built once per audit rather than once per leaf: `uncarried-leaves` over a
  10,000-leaf receipt would otherwise split the same text 10,000 times."
  [text]
  (set (str/split-lines (or text ""))))

;; @spec MCP-OP-STUDY-051
(defn leaf-carried?
  "Does a text whose lines are `line-index` carry this leaf?

  ONLY as the leaf's OWN lines. Not as a substring of a longer value's line,
  not inside the block's own declaration, and not by any coincidence of
  characters: a leaf is carried when the reader can point at a line that
  spells this leaf's pointer AND this leaf's value, which is the only evidence
  from which the fact can be read back or removed.

  Field evidence (Sol O2 round-6 review, 2026-09-04, sections 2 and 3). The
  substring rule this replaces resolved three coincidences wrongly, and two of
  them made the DECLARATION disagree with the AUDIT of its own text: a leaf
  whose distinctive value equalled its own pointer was declared dropped and
  then found carried by the `dropped:` line that named it (a public `name`
  rung declaring 23 omissions over 19 audited); a sixteen-character value
  occurring only inside `decoy: XXabcdefghijklmnopYY` was counted rendered
  with no line of its own; and one value spelled at two pointers counted as
  two facts carried by one line, so neither copy could be removed
  independently. A JSON pointer spells a leaf's ADDRESS; it is never a
  rendering of its VALUE."
  [line-index path value]
  (every? line-index (leaf-lines path value)))

;; @spec MCP-OP-STUDY-044
;; @spec MCP-OP-STUDY-051
(defn leaf-rendered?
  "Does `text` carry this receipt leaf VERBATIM?

  ONE predicate, used both by the renderer that guarantees the property and by
  the witness that checks it, so the two can never drift apart. The one-shot
  spelling of `leaf-carried?`, for a caller holding a text rather than an
  index."
  [text path value]
  (leaf-carried? (text-line-index text) path value))

;; @spec MCP-OP-STUDY-044
(defn leaf-excluded?
  "Is this leaf a member of the ONE enumerated exclusion?

  Named so that the single exclusion mechanism has a single spelling: a leaf
  is kept out of the text because it is in `text-excluded-leaf-keys`, and for
  no other reason."
  [path]
  (contains? text-excluded-leaf-keys (last path)))

;; @spec MCP-OP-STUDY-044
(defn uncarried-leaves
  "Every receipt leaf `text` does not carry, excluding the enumerated set.

  The criterion, as one function: `content[0].text` shall be a superset of
  every `structuredContent` leaf value. A caller reading only the text has the
  whole receipt or knows exactly which part of it it does not have."
  [text result]
  (let [line-index (text-line-index text)]
    (into []
          (remove (fn [[path value]]
                    (or (leaf-excluded? path)
                        (leaf-carried? line-index path value))))
          (receipt-leaf-pairs result))))

;; @spec MCP-OP-STUDY-044
;; @spec MCP-OP-STUDY-047

(defn receipt-fact-entries
  "One entry per receipt leaf `structural-text` does not already carry, in
  receipt order: its JSON pointer, the line that renders it, and the spelling
  that line carries.

  The default is to RENDER: the rows a mode renders structurally satisfy the
  criterion where they can, and everything left over prints here. A receipt
  field added tomorrow therefore travels into the text the day it is added,
  and only a member of `text-excluded-leaf-keys` can keep it out.

  A COLLIDABLE leaf prints as `pointer=spelling` — the only rendering of it a
  reader can attribute to the leaf — and every other leaf as `pointer: value`.

  Carriage is decided against the STRUCTURAL text ALONE. @spec MCP-OP-STUDY-047
  — field evidence (Sol O2 round-5 review, 2026-09-04, section 2): this used
  to accumulate each rendered line into the text it tested the NEXT leaf
  against, so a leaf could be deemed carried by a fact line the budget then
  dropped. On the branch's own primary fixture that made
  `results[1].outline.requires[0]` invisible to the entry list, hence to the
  count AND to the `dropped:` pointers: 784 declared against 785 audited. @spec
  MCP-OP-STUDY-051 — a duplicate spelling is no longer credited to another
  entry at all: two pointers holding one value are two facts, and each renders
  its own line.

  Deciding against a FIXED text is also what makes the fit affordable: the
  accumulator rebuilt the whole text once per leaf, so one 10,000-leaf result
  cost about 1.25 GB of string copying per candidate and 69,132 ms over a
  fit."
  [structural-text result]
  (into []
        (comp
          (remove (fn [[path value]]
                    (or (leaf-excluded? path)
                        (leaf-rendered? structural-text path value))))
          (map (fn [[path value]]
                 ;; @spec MCP-OP-STUDY-051 — the entry renders EXACTLY the
                 ;; lines `leaf-carried?` looks for, so "this entry printed"
                 ;; and "the text carries this leaf" are one fact rather than
                 ;; two computations that have to agree.
                 {:path path
                  :label (leaf-label path)
                  :line (str/join "\n" (leaf-lines path value))})))
        (receipt-leaf-pairs result)))

;; @spec MCP-OP-STUDY-044
(defn receipt-fact-lines
  "The rendered lines of `receipt-fact-entries`, in receipt order."
  [structural-text result]
  (mapv :line (receipt-fact-entries structural-text result)))

;; @spec MCP-OP-STUDY-044
;; @spec MCP-OP-STUDY-040
(def unbounded-evidence
  "The allowance a rendering gets when NOTHING narrower has been imposed.

  The superset property is the default; an elision is an exception the PUBLIC
  OUTPUT BUDGET forces, and `fit-public-result` is the only thing that can
  force it — it measures the whole candidate and imposes
  `:text_evidence_limit` when, and only when, the complete rendering does not
  fit.

  Field evidence (Sol O2 round-3 review, section 4): the refusal renderer
  spent a FIXED 8,192-character receipt-fact allowance regardless of the
  budget, so a refusal with 10,921 bytes of unspent room still dropped its
  `error`, its `path`, and four more leaves. A fixed allowance is a second
  budget nobody declared, and it makes MCP-OP-STUDY-044's `where the output
  budget forces` untrue at the only place a caller can check it."
  Long/MAX_VALUE)

;; @spec MCP-OP-STUDY-044
;; @spec MCP-OP-STUDY-040
(def max-named-dropped-labels
  "How many dropped leaves the `dropped:` line NAMES before it counts the rest.

  A line that names one label per dropped leaf is not a bounded rendering — it
  GROWS as the allowance shrinks. Field evidence (Opus O2 round-4 review,
  2026-09-04, sections 2 and 3): on a two-file `outline` batch over this
  repository's own sources that line reached 14,732 characters at 406 dropped
  labels and 22,142 at 606, outside any allowance, so the WHOLE rendering grew
  as the budget tightened; `fits?` stopped being monotone, and the fit
  abandoned a rendering that fit with 9,251 bytes to spare.

  Eight pointers are enough to start reading the receipt back with and short
  enough to charge. The count carries the rest, and `:dropped-labels` still
  carries every pointer to a caller that walks the block."
  8)

;; @spec MCP-OP-STUDY-044
(defn dropped-line
  "`  dropped: a, b, c (+N more)` — the BOUNDED naming of dropped leaves.

  Bounded by construction: at most `max-named-dropped-labels` pointers, then a
  count. A naming whose length is the number of things it names cannot be paid
  for out of the allowance those things were dropped from."
  [labels]
  (let [named (vec (take max-named-dropped-labels labels))
        remaining (- (count labels) (count named))]
    (str "  dropped: "
         (str/join ", " named)
         (when (pos? remaining) (format " (+%d more)" remaining)))))

;; @spec MCP-OP-STUDY-044
(defn fact-section-header
  "`  receipt facts · 3 of 9 rendered · …` — the section's own count line."
  [shown total dropped?]
  (format "  receipt facts · %d of %d rendered%s"
          shown total
          (if dropped? " · the complete receipt is in structuredContent" "")))

;; @spec MCP-OP-STUDY-047
;; @spec MCP-OP-STUDY-051
(defn- dropped-indices
  "The indices of the entries a rendering of `shown` lines does NOT carry:
  every entry past `shown`, and nothing else.

  @spec MCP-OP-STUDY-051 — there is nothing to search. A leaf is carried by
  its OWN lines and by no others, so a rendered line can never discharge
  another entry, and `shown` is exactly the set of facts the section carries.
  This replaces `carrier-indices`, which asked whether some earlier line's
  characters CONTAINED this leaf's spelling: on the reviewer's plants that
  credited `target = abcdefghijklmnop` to the line
  `decoy: XXabcdefghijklmnopYY`, and one long value at two pointers to a
  single line, publishing `2 of 2 rendered` over facts the text did not
  contain.

  @spec MCP-OP-STUDY-050 — and it walks the TAIL alone, which is what the
  descent in `fact-block` asks about."
  [total shown]
  (vec (range shown total)))

;; @spec MCP-OP-STUDY-044
;; @spec MCP-OP-STUDY-040
;; @spec MCP-OP-STUDY-047
;; @spec MCP-OP-STUDY-048
;; @spec MCP-OP-STUDY-051
(defn fact-block
  "The bounded receipt-fact section, whether any fact was dropped, and WHICH.

  EVERY character the section renders is charged against `budget`: the fact
  lines, the count header, and the bounded `dropped:` line. That is what makes
  the allowance an allowance — lowering it can only SHRINK the rendering — and
  it is the property every search over the allowance rests on.

  Facts are dropped WHOLE and only from the tail, exactly as rows are: the
  same bound governs both, because both are the receipt. The JSON pointer of
  every dropped fact is carried out, so a caller that walks the block can name
  them all even though the rendering names the first few and counts the rest.

  @spec MCP-OP-STUDY-048 — the DECLARATION is the FLOOR. The count header and
  the `dropped:` line are rendered whenever anything is dropped, whatever the
  budget says. A rendering that omits leaves SILENTLY is the one outcome
  MCP-OP-STUDY-044 exists to prevent, and it is worst exactly where the budget
  is tightest; `fit-public-result` measures the whole candidate, so a
  declaration the budget cannot pay for becomes a typed refusal THERE rather
  than a silent omission here. Field evidence (Sol O2 round-5 review, section
  3): this stopped at `shown = 0` before establishing that its own header and
  `dropped:` line fit, and then gated `:section` on the same budget the lines
  had been charged to — `allowance=0 dropped_line_chars=139 section_chars=nil
  shown=0 total=2 dropped=true`.

  @spec MCP-OP-STUDY-047 — the header's `X of N` is DERIVED from the same
  carriage walk `uncarried-leaves` applies to the published text: `N` is every
  entry, and `N - X` is every entry whose own line this rendering does not
  print. @spec MCP-OP-STUDY-051 — that is now an IDENTITY rather than an
  agreement between two walks, because a leaf is carried by its own lines and
  by nothing else: not by a longer value whose line contains its spelling, and
  not by the `dropped:` line that names its pointer. The round-six rendering
  declared 29 omissions on the `name` rung while an audit of the text it
  published found 25.

  Field evidence (Opus O2 round-4 review, 2026-09-04, sections 2 and 3): round
  four charged the fact LINES and left the header and the `dropped:` line
  outside the allowance, so at allowance 0 the rendering was 22,785 characters
  — LARGER than at allowance 9,434 — and `fit-public-result` searched a
  `fits?` that was not monotone, found nothing, and published a 151-character
  notice for an ordinary two-file `outline` batch."
  [structural-text result budget]
  (let [entries (vec (receipt-fact-entries structural-text result))
        total (count entries)
        ;; Prefix sums so the descent below costs one comparison per step: a
        ;; section rendered per candidate would be quadratic in the fact
        ;; count, and the fit evaluates dozens of candidates.
        prefix (vec (reductions + 0 (map #(inc (count (:line %))) entries)))
        dropped-labels-at
        (memoize
          (fn [shown]
            (mapv #(:label (nth entries %))
                  (dropped-indices total shown))))
        section-at
        (fn [shown]
          (let [labels (dropped-labels-at shown)
                dropped? (seq labels)
                header (fact-section-header (- total (count labels)) total
                                            (boolean dropped?))]
            {:labels labels
             :dropped (boolean dropped?)
             :text (str/join
                     "\n"
                     (concat [header]
                             (when dropped? [(dropped-line labels)])
                             (map :line (subvec entries 0 shown))))}))
        section-length
        (fn [shown]
          (let [labels (dropped-labels-at shown)
                dropped? (seq labels)]
            (+ (count (fact-section-header (- total (count labels)) total
                                           (boolean dropped?)))
               (if dropped? (inc (count (dropped-line labels))) 0)
               (nth prefix shown))))
        ;; @spec MCP-OP-STUDY-050 — the descent starts at the FEASIBLE
        ;; CEILING, not at `total`. `prefix` is strictly increasing and
        ;; `section-length` is never smaller than it — the count header is
        ;; always rendered — so no `n` whose fact lines ALONE overrun the
        ;; budget can fit, and the largest such `n` is a binary search over a
        ;; genuinely monotone quantity. This is not the non-monotone
        ;; bisection Opus's round-4 review killed: nothing here searches
        ;; `fits?`. Below the ceiling the descent is linear and EXACT, and it
        ;; is short by construction — every step it takes removes at least
        ;; one whole line, while the only thing that can grow as it descends
        ;; is the bounded declaration. Field evidence (Sol O2 round-5 review,
        ;; section 5): a 10,000-fact result descended from 10,000 one step at
        ;; a time, per candidate, and the fit took 69,132 ms.
        ceiling (loop [low 0 high total]
                  (if (>= low high)
                    low
                    (let [mid (quot (+ low high 1) 2)]
                      (if (<= (nth prefix mid) budget)
                        (recur mid high)
                        (recur low (dec mid))))))
        shown (loop [n ceiling]
                (if (or (zero? n) (<= (section-length n) budget))
                  n
                  (recur (dec n))))
        rendered (section-at shown)]
    {:lines (mapv :line (subvec entries 0 shown))
     :shown shown
     :total total
     :dropped (:dropped rendered)
     :dropped-labels (:labels rendered)
     ;; The section is rendered HERE because this is where it was charged. A
     ;; section assembled elsewhere out of these pieces would be a second
     ;; rendering, and the budget would have been spent on the other one.
     ;; @spec MCP-OP-STUDY-048 — NOT gated on fitting the budget. A
     ;; declaration the allowance cannot pay for is still the truth about what
     ;; this text omits; suppressing it publishes an undeclared omission.
     :section (when (or (pos? shown) (:dropped rendered)) (:text rendered))}))

;; @spec MCP-OP-STUDY-044
(defn fact-section
  "The rendered receipt-fact section, or nil when the structural rendering
  already carried every leaf.

  @spec MCP-OP-STUDY-048 — never nil merely because the allowance is small:
  an allowance too small to pay for the declaration is a reason to REFUSE, in
  `fit-public-result`, not a reason to omit leaves silently here.

  An elision NAMES what it dropped. `receipt facts · 3 of 9 rendered` tells a
  caller that six leaves are missing; `dropped: error, path, … (+4 more)`
  tells it which to look for first, which is the difference between knowing
  where to look and having to reconstruct the receipt to find out. This
  returns exactly the characters `fact-block` measured — never a second
  rendering assembled after the measurement."
  [block]
  (:section block))

(defn- kernel-refusal
  [request index result]
  (let [error-type (or (:error-type result) :inspect-kernel-refusal)]
    (cond->
      {:ok false
       :operation "inspect_clojure"
       :error_type (if (keyword? error-type)
                     (name error-type)
                     (str error-type))
       :error (or (:error result) "inspect_clojure refused")
       :request_id (:id request)
       :request_index index
       :read_complete false
       :source_unchanged true
       :next_action "correct_request"}
      (= "forms" (:operation request))
      (assoc :failed_request
             {:id (:id request)
              :operation "forms"
              :file (:file request)
              :requested_forms (mapv str (:forms request))})
      (contains? result :failed-stage)
      (assoc :failed_stage (name (:failed-stage result)))
      (contains? result :file-hash)
      (assoc :file_hash (:file-hash result))
      (contains? result :expected) (assoc :expected (json-data (:expected result)))
      (contains? result :actual) (assoc :actual (json-data (:actual result)))
      (contains? result :expected-match-count)
      (assoc :expected_match_count (:expected-match-count result))
      (contains? result :actual-match-count)
      (assoc :actual_match_count (:actual-match-count result))
      (contains? result :match-count)
      (assoc :match_count (:match-count result))
      ;; @spec MCP-OP-FIELD-003
      (contains? result :note) (assoc :note (:note result))
      (contains? result :failure-count)
      (assoc :failure_count (:failure-count result))
      (contains? result :requested-form-count)
      (assoc :requested_form_count (:requested-form-count result))
      (contains? result :resolved-form-count)
      (assoc :resolved_form_count (:resolved-form-count result))
      (contains? result :failures)
      (assoc :failures (json-data (:failures result)))
      (contains? result :available-form-count)
      (assoc :available_form_count (:available-form-count result))
      (contains? result :available-owner-count)
      (assoc :available_owner_count (:available-owner-count result))
      (contains? result :available-owners)
      (assoc :available_owners (:available-owners result))
      (contains? result :available-owners-returned)
      (assoc :available_owners_returned (:available-owners-returned result))
      (contains? result :available-owners-omitted)
      (assoc :available_owners_omitted (:available-owners-omitted result))
      (contains? result :available-owners-truncated)
      (assoc :available_owners_truncated (:available-owners-truncated result))
      (contains? result :selection-failures)
      ;; @spec MCP-OP-READ-NORM-003
      (assoc :selection_failures
             (mapv #(assoc (json-data %) :request_id (:id request))
                   (:selection-failures result)))
      (contains? result :form-candidates)
      (assoc :form_candidates (json-data (:form-candidates result)))
      (contains? result :candidate-limit)
      (assoc :candidate_limit (:candidate-limit result))
      (contains? result :candidates-truncated)
      (assoc :candidates_truncated (:candidates-truncated result))
      (contains? result :form)
      (assoc :form (str (:form result)))
      (contains? result :required)
      (assoc :required (:required result))
      (contains? result :limit)
      (assoc :limit (:limit result))
      ;; A refusal that knows a better next action than "correct the request"
      ;; says so; the base value stays the default for everything else.
      (contains? result :next-action)
      (assoc :next_action (:next-action result))
      (contains? result :remedy)
      (assoc :remedy (:remedy result))
      (contains? result :next-call)
      (assoc :next_call (json-data (:next-call result))))))

;; @spec MCP-OP-READ-DIAG-001
;; @spec MCP-OP-READ-DIAG-003
;; @spec MCP-OP-READ-HYP-001
;; @spec MCP-OP-READ-HYP-002
(defn- forms-result
  [request snapshot]
  (let [found (show-form/select-form
                (:file request) (:source snapshot)
                {:forms (mapv symbol (:forms request))})]
    (if (:error found)
      (assoc found
             :failed-stage :selector
             :file-hash (:hash snapshot))
      {:id (:id request)
       :operation "forms"
       :file (:file request)
       :file_hash (:hash snapshot)
       :form_count (:form-count found)
       :source_character_count (:source-char-count found)
       :forms
       (mapv (fn [form]
               (let [built-anchor
                     (source-anchor/build-form-source-anchor
                       (:file request) (:source snapshot) form)]
                 (when-not (:ok built-anchor)
                   (throw
                     (ex-info "Selected form has no exact source anchor"
                              built-anchor)))
                 (cond->
                   {:hash (structural-lens/source-hash (:source form))
                    :line (:line form)
                    :end_line (:end-line form)
                    :form_type (str (:type form))
                    :name (str (:name form))
                    :platforms (mapv name (:platforms form))
                    :file (:file request)
                    :file_hash (:hash snapshot)
                    :source_anchor (:source-anchor built-anchor)}
                   (not= false (:include-source request))
                   (assoc :source (:source form))
                   (:comment-start form)
                   (assoc :comment_start (:comment-start form)))))
             (:forms found))})))

(defn- outline-result
  [request snapshot]
  {:id (:id request)
   :operation "outline"
   :file (:file request)
   :file_hash (:hash snapshot)
   :source_character_count (count (:source snapshot))
   :outline (json-data
              (outline/outline-source
                (:file request) (:source snapshot) {}
                {:include-string-symbols
                 (:include-string-symbols request)}))})

(def ^:private wildcard-note
  "each `_` matches exactly one subtree; a longer form needs a longer pattern")

;; @spec MCP-OP-FIELD-003
;; @spec MCP-OP-FIELD-005
(defn- wildcard-pattern?
  "Does this pattern use `_` as a standalone wildcard token?

   Decided from the parsed pattern, because bytes cannot tell a wildcard from an
   underscore inside a string literal or inside one symbol, and cannot see a
   wildcard whose only neighbour is a comma. An unreadable pattern carries no
   note; the match request refuses on its own terms."
  [pattern]
  (try
    (let [form (node/sexpr (parser/parse-string (str pattern)))]
      (boolean (some #(= '_ %) (tree-seq coll? seq form))))
    (catch Exception _ false)))

(defn- match-result
  [request snapshot]
  (let [found (structural-lens/find-subforms
                (:source snapshot)
                (cond-> {:match (:match request)}
                  (:inside request) (assoc :inside (symbol (:inside request)))))]
    (cond
      (:error found) found

      (and (contains? (:expect request) :matches)
           (not= (get-in request [:expect :matches]) (:match-count found)))
      (cond-> {:error "Structural match cardinality did not meet the declared expectation"
               :error-type :inspect-cardinality-mismatch
               :expected (get-in request [:expect :matches])
               :actual (:match-count found)
               :match-count (:match-count found)}
        ;; @spec MCP-OP-FIELD-003
        (and (< (:match-count found) (get-in request [:expect :matches]))
             (wildcard-pattern? (:match request)))
        (assoc :note wildcard-note))

      :else
      (let [matches (mapv (fn [match]
                            (assoc (json-data match)
                                   :hash (structural-lens/source-hash
                                           (:source match))
                                   :file_hash (:hash snapshot)))
                          (:matches found))]
        (cond->
          {:id (:id request)
           :operation "match"
           :file (:file request)
           :file_hash (:hash snapshot)
           :match (:match request)
           :inside (:inside request)
           :match_count (:match-count found)
           :source_character_count (reduce + 0 (map #(count (:source %))
                                                    (:matches found)))
           :matches matches}
          ;; @spec MCP-OP-FIELD-003
          (and (zero? (:match-count found))
               (wildcard-pattern? (:match request)))
          (assoc :note wildcard-note))))))

(defn- xray-result
  [request snapshot]
  (let [prepared (edit-dsl/prepare-xray-options
                   {:op :xray
                    :file (:file request)
                    :expr (:expression request)})]
    (if (:error prepared)
      prepared
      (let [found (edit-dsl/evaluate-xray (:source snapshot) prepared)]
        (if (:error found)
          found
          (let [normalized (json-data found)
                sources (keep :source (:matches found))]
            (-> normalized
                (assoc :id (:id request)
                       :operation "xray"
                       :file (:file request)
                       :file_hash (:hash snapshot)
                       :source_character_count (reduce + 0 (map count sources))))))))))

;; ============================================================
;; Study operations — bounded receipts over the clj-surgeon.study kernel
;; ============================================================
;; The CLI and this entrance call the same kernel functions on the same bytes.
;; Only read-only study operations are exposed here; the write operations
;; (:mv, :rename-ns!, :fix-declares!) stay CLI/gate-only by design.

(def study-default-limit 4096)
(def study-max-limit 16384)

(defn- study-request-arguments
  "Rebuild one single-request batch for an executable continuation."
  [request overrides]
  {:requests [(merge (cond-> {:id (:id request)
                              :operation (:operation request)
                              :file (:file request)}
                       (:form request) (assoc :form (:form request)))
                     overrides)]
   :expect {:requests 1 :files 1}})

(defn- study-next-call
  [request overrides]
  {:tool "inspect_clojure"
   :arguments (study-request-arguments request overrides)})

;; @spec MCP-OP-STUDY-007
;; @spec MCP-OP-STUDY-018
(defn bound-rows
  "Bound an already JSON-normalized row vector by total JSON characters.

  Returns [kept omitted truncated?]. A first row larger than the limit keeps
  nothing rather than silently exceeding the receipt budget.

  `used` starts at 1, not 0. The serialized array costs
  `sum(rows) + (n-1) separators + 2 brackets`; charging each row `len + 1`
  pays for n of those n+1 punctuation characters, leaving exactly one bracket
  unpaid, so a kept payload could be `limit + 1` characters. The seed pays for
  it. The one floor no bound can go below is the empty array's two
  characters."
  [rows limit]
  (loop [remaining (seq rows)
         kept []
         used 1]
    (if-let [row (first remaining)]
      (let [cost (inc (json-character-count row))]
        (if (> (+ used cost) limit)
          [kept (count remaining) true]
          (recur (next remaining) (conj kept row) (+ used cost))))
      [kept 0 false])))

(defn- study-limit
  [request]
  (or (:limit request) study-default-limit))

;; @spec MCP-OP-STUDY-016
(defn- study-base
  [request snapshot limit]
  {:id (:id request)
   :operation (:operation request)
   :file (:file request)
   :file_hash (:hash snapshot)
   ;; The bytes actually read, not a hardcoded zero. `forms`, `match`, and
   ;; `xray` always reported this; the study operations reported 0, which also
   ;; meant they were invisible to the per-request source budget.
   :source_character_count (count (:source snapshot))
   :limit limit})

(defn- study-truncation
  "A continuation is emitted only when raising the limit can still advance.

  At the maximum limit the narrower scope is a caller judgment, not a
  deterministic projection of proved facts, so no executable call is served."
  [result request truncated? omitted]
  (let [limit (study-limit request)
        raisable? (< limit study-max-limit)]
    (cond-> (assoc result :truncated truncated? :omitted omitted)
      truncated?
      (assoc :next_action (if raisable?
                            "raise_limit_or_narrow_scope"
                            "narrow_scope")
             :remedy (if raisable?
                       "Replay next_call to widen the receipt, or narrow the request."
                       "The receipt is already at the maximum limit; request one exact form instead."))
      (and truncated? raisable?)
      (assoc :next_call (study-next-call request {:limit study-max-limit})))))

;; @spec MCP-OP-STUDY-010
(defn- study-missing-form
  [request source]
  (let [owners (study/owner-names source)
        total (count owners)
        returned (min 50 total)]
    {:error (str "No top-level form named " (:form request)
                 " in " (:file request))
     :error-type :study-form-not-found
     :failed-stage :study-select
     :form (:form request)
     :available-owners (vec (take returned owners))
     :available-owners-returned returned
     :available-owners-omitted (- total returned)
     :available-owners-truncated (> total returned)
     :available-owner-count total
     :next-call (study-next-call request
                                 {:form "REPLACE-WITH-ONE-EXACT-OWNER"})}))

;; @spec MCP-OP-STUDY-007
;; @spec MCP-OP-STUDY-027
(defn- study-oversized
  "One atomic result that cannot be split refuses rather than half-serialized.

  A continuation is served only while raising `limit` can still advance.
  `(min study-max-limit (max required limit))` EQUALS `limit` at the ceiling,
  so the old unconditional next_call handed back the exact call just made —
  the loop MCP-OP-STUDY-007 forbids. Mirrors `study-truncation`: at the
  ceiling, a narrower scope is a caller judgment, so the receipt names it
  instead of serving an executable call.

  Raising has to be able to SUCCEED, not merely to change the number. An
  atomic result needing 22,141 characters cannot be returned at any limit,
  because the ceiling is 16,384: proposing `limit 16384` to a caller at 4,096
  is a call that is known, here, to fail exactly as this one did. So
  `required` must also fit under the ceiling."
  [request required limit]
  (let [raised (min study-max-limit (max required limit))
        raisable? (and (> raised limit) (<= required study-max-limit))]
    (cond-> {:error "One atomic study result exceeds the receipt limit"
             :error-type :study-output-limit
             :required required
             :limit limit
             :next-action (if raisable?
                            "raise_limit_or_narrow_scope"
                            "narrow_scope")
             :remedy (if raisable?
                       "Replay next_call to widen the receipt, or narrow the request."
                       "The receipt is already at the maximum limit; request one exact form instead.")}
      raisable? (assoc :next-call (study-next-call request {:limit raised})))))

;; @spec MCP-OP-STUDY-016
(defn- deps-result
  [request snapshot]
  (let [limit (study-limit request)
        source (:source snapshot)]
    (if (:form request)
      (if-let [row (study/deps source {:form (:form request)})]
        ;; One adjacency row is atomic: it cannot be truncated at row
        ;; granularity, so it refuses like every other atomic result rather
        ;; than being returned over the caller's budget.
        (let [normalized (json-data row)
              required (json-character-count normalized)]
          (if (> required limit)
            (study-oversized request required limit)
            (study-truncation
              (assoc (study-base request snapshot limit)
                     :returned 1
                     :deps normalized)
              request false 0)))
        (study-missing-form request source))
      (let [rows (mapv json-data (study/deps source {}))
            [kept omitted truncated?] (bound-rows rows limit)]
        (study-truncation
          (assoc (study-base request snapshot limit)
                 :returned (count kept)
                 :form_count (count rows)
                 :deps kept)
          request truncated? omitted)))))

;; @spec MCP-OP-STUDY-016
(defn- topo-result
  "Topological ordering, with EVERY row it returns charged to the budget.

  `:cycles` was unbounded — an all-cycle file returned every cycle member
  whatever the limit — and `form_count` counted only `:sorted`, so the same
  file reported `form_count 0` while listing its forms."
  [request snapshot]
  (let [limit (study-limit request)
        kernel (json-data (study/topo (:source snapshot)))
        envelope-cost (json-character-count (assoc kernel :sorted [] :cycles []))]
    (if (> envelope-cost limit)
      (study-oversized request envelope-cost limit)
      (let [row-budget (- limit envelope-cost)
            [sorted-kept sorted-omitted sorted-truncated?]
            (bound-rows (:sorted kernel) row-budget)
            [cycles-kept cycles-omitted cycles-truncated?]
            (bound-rows (:cycles kernel)
                        (max 0 (- row-budget
                                  (json-character-count sorted-kept))))]
        (study-truncation
          (assoc (study-base request snapshot limit)
                 :returned (+ (count sorted-kept) (count cycles-kept))
                 :form_count (+ (count (:sorted kernel))
                                (count (:cycles kernel)))
                 :topo (assoc kernel
                              :sorted sorted-kept
                              :cycles cycles-kept))
          request
          (or sorted-truncated? cycles-truncated?)
          (+ sorted-omitted cycles-omitted))))))

(defn- ls-deps-result
  [request snapshot]
  (let [limit (study-limit request)
        source (:source snapshot)]
    (if-let [tree (study/ls-deps source {:form (:form request)})]
      (let [normalized (json-data tree)
            required (json-character-count normalized)]
        (if (> required limit)
          (study-oversized request required limit)
          (study-truncation
            (assoc (study-base request snapshot limit)
                   :returned 1
                   :dep_tree normalized)
            request false 0)))
      (study-missing-form request source))))

;; @spec MCP-OP-STUDY-016
(defn- ls-extract-result
  "Minimal extractable closure, with the closure's own non-`:forms` keys —
  target, required requires, and the rest of the envelope — charged to the
  same budget the forms are. Only `:forms` was bounded before, so the receipt
  could exceed `limit` by the size of everything around them."
  [request snapshot]
  (let [limit (study-limit request)
        source (:source snapshot)
        kernel (json-data (study/ls-extract source {:form (:form request)}))]
    (if (empty? (:forms kernel))
      (study-missing-form request source)
      (let [envelope-cost (json-character-count (assoc kernel :forms []))]
        (if (> envelope-cost limit)
          (study-oversized request envelope-cost limit)
          (let [[kept omitted truncated?]
                (bound-rows (:forms kernel) (- limit envelope-cost))]
            (study-truncation
              (assoc (study-base request snapshot limit)
                     :returned (count kept)
                     :form_count (count (:forms kernel))
                     :closure (assoc kernel :forms kept))
              request truncated? omitted)))))))

(defn- evaluate-request
  [request snapshot]
  (try
    (case (:operation request)
      "forms" (forms-result request snapshot)
      "outline" (outline-result request snapshot)
      "match" (match-result request snapshot)
      "xray" (xray-result request snapshot)
      "deps" (deps-result request snapshot)
      "topo" (topo-result request snapshot)
      "ls-deps" (ls-deps-result request snapshot)
      "ls-extract" (ls-extract-result request snapshot))
    (catch Exception error
      {:error (.getMessage error)
       :error-type (or (:error-type (ex-data error)) :invalid-source)})))

;; @spec MCP-OP-STUDY-020
;; @spec MCP-OP-STUDY-034
(defn returned-source-character-count
  "How many characters of file SOURCE one result actually hands back.

  This is NOT `:source_character_count`, which MCP-OP-STUDY-016 defines as the
  characters the request READ. Charging the read count to the per-request
  SOURCE budget conflated the two: `outline` and every study operation return
  a derived structure and no source at all, yet a 126,596-character file made
  them refuse with `inspect-output-limit` / `request_less_evidence` — a remedy
  no caller can act on, because the request was already the smallest one that
  answers the question. The budget exists to bound what crosses the wire, so
  it counts what crosses the wire: every string of file source anywhere in the
  result.

  The keys are enumerated, in both their keyword and their JSON-normalized
  spellings, because a walk keyed on `:source` alone measured the wrong thing:
  `outline` returns no `:source` at all — it returns each form's `:args`,
  lifted verbatim out of the file and normalized to a string key by
  `json-data` — so 2,696 characters of `intent_transaction.clj` crossed the
  wire scored as zero. Coverage here is a naming convention, so it is written
  down rather than assumed: a NEW key that carries verbatim source belongs in
  this set the day it is added."
  [result]
  (let [source-keys #{:source "source" :args "args"}
        total (volatile! 0)]
    (walk/postwalk
      (fn [node]
        (when (and (map-entry? node)
                   (contains? source-keys (key node))
                   (string? (val node)))
          (vswap! total + (count (val node))))
        node)
      result)
    @total))

;; @spec MCP-OP-STUDY-020
(defn enforce-output-budget
  "Apply inclusive per-request source/result and aggregate result limits.

  The source limit is charged against the source a result RETURNS, not the
  source it read; see `returned-source-character-count`.

  Returns the original result vector on success and never truncates data."
  ([results]
   (enforce-output-budget results default-output-limits))
  ([results limits]
   (let [{:keys [per-request-source per-request-result aggregate-result]}
         (merge default-output-limits limits)
         failure
         (some (fn [[index result]]
                 (let [source-count (returned-source-character-count result)
                       result-count (json-character-count result)]
                   (cond
                     (> source-count per-request-source)
                     {:scope "request_source" :request_index index
                      :actual source-count :limit per-request-source}

                     (> result-count per-request-result)
                     {:scope "request_result" :request_index index
                      :actual result-count :limit per-request-result})))
               (map-indexed vector results))
         aggregate-count (json-character-count results)
         failure (or failure
                     (when (> aggregate-count aggregate-result)
                       {:scope "aggregate_result"
                        :actual aggregate-count :limit aggregate-result}))]
     (if failure
       (merge
         {:ok false
          :operation "inspect_clojure"
          :error_type "inspect-output-limit"
          :error "inspect_clojure output exceeds a hard limit; no partial result is returned"
          :read_complete false
          :source_unchanged true
          :next_action "request_less_evidence"}
         failure)
       {:ok true
        :results results
        :result_character_count aggregate-count}))))

(defn- snapshot-guards-for
  [requests snapshots incoming-guards]
  (into
    (array-map)
    (map (fn [file] [file (:hash (get snapshots file))]))
    (distinct (concat (keys incoming-guards) (map :file requests)))))

;; @spec MCP-OP-READ-CONT-001 MCP-OP-READ-CONT-002
(defn- selector-retry-template
  [pending-requests selector-result snapshot-guards]
  (let [failed-values (set (map (comp str :form) (:failures selector-result)))
        failed-request (first pending-requests)
        holes (->> (:forms failed-request)
                   (map-indexed
                     (fn [index form]
                       (when (contains? failed-values form)
                         {:path ["requests" 0 "forms" index]
                          :request_id (:id failed-request)
                          :kind "exact-top-level-owner"
                          :rejected_value form
                          :must_replace true
                          :authority false})))
                   (remove nil?)
                   vec)
        pending-with-holes
        (assoc-in pending-requests [0 :forms]
                  (mapv (fn [form]
                          (when-not (contains? failed-values form) form))
                        (:forms failed-request)))]
    {:executable false
     :snapshot_bound true
     :selector_authority false
     :write_authority false
     :arguments
     {:snapshot_guards snapshot-guards
      :requests pending-with-holes
      :expect {:requests (count pending-requests)
               :files (count (distinct (map :file pending-requests)))}}
     :holes holes}))

(defn- selector-continuation
  [requests failed-index completed-results snapshots incoming-guards
   selector-result limits]
  (when (seq completed-results)
    (let [budget (enforce-output-budget completed-results limits)
          pending-requests (subvec requests failed-index)
          snapshot-guards (snapshot-guards-for requests snapshots incoming-guards)]
      (if-not (:ok budget)
        budget
        {:ok true
         :continuation
         {:snapshot_bound true
          :selector_authority false
          :write_authority false
          :completed_request_count (count completed-results)
          :completed_request_ids (mapv :id completed-results)
          :pending_request_count (count pending-requests)
          :pending_request_ids (mapv :id pending-requests)
          :snapshot_guards snapshot-guards
          :completed_results completed-results
          :retry_template
          (selector-retry-template
            pending-requests selector-result snapshot-guards)}}))))

(defn evaluate-snapshots
  "Evaluate a validated ordered request batch over supplied immutable snapshots.

  `snapshots` maps each request-relative file to `{:file :source :hash}`."
  ([params snapshots]
   (evaluate-snapshots params snapshots default-output-limits))
  ([{:keys [requests expect snapshot-guards]} snapshots limits]
   (loop [index 0
          results []]
     (if (< index (count requests))
       (let [request (nth requests index)
             snapshot (get snapshots (:file request))]
         (if-not snapshot
           {:ok false
            :operation "inspect_clojure"
            :error_type "missing-snapshot"
            :error "A validated request has no captured file snapshot"
            :request_id (:id request)
            :request_index index
            :read_complete false
            :source_unchanged true
            :next_action "retry_call"}
           (let [result (evaluate-request request snapshot)]
             (if (:error result)
               (let [refusal (kernel-refusal request index result)
                     continuation
                     (when (= :selector (:failed-stage result))
                       (selector-continuation
                         requests index results snapshots snapshot-guards
                         result limits))]
                 (cond
                   (and continuation (not (:ok continuation))) continuation
                   continuation (assoc refusal :continuation
                                       (:continuation continuation))
                   :else refusal))
               (recur (inc index) (conj results result))))))
       (let [budget (enforce-output-budget results limits)]
         (if-not (:ok budget)
           budget
           (let [files (distinct (map :file requests))
                 file-hashes (into (array-map)
                                   (map (fn [file]
                                          [file (:hash (get snapshots file))]))
                                   files)
                 source-count (reduce + 0 (map :source_character_count results))
                 truncated? (boolean (some :truncated results))]
             (cond->
               {:ok true
                :operation "inspect_clojure"
                :read_complete (not truncated?)
                :request_count (:requests expect)
                :file_count (:files expect)
                :results results
                :file_hashes file-hashes
                :source_character_count source-count
                :result_character_count (:result_character_count budget)
                :next_action (if truncated?
                               "raise_limit_or_narrow_scope"
                               "none")}
               truncated? (assoc :truncated true)
               snapshot-guards (assoc :snapshot_guards snapshot-guards)))))))))

(defn- plural
  [count singular]
  (str count " " singular
       (when (not= 1 count)
         (if (= "match" singular) "es" "s"))))

;; @spec MCP-OP-STUDY-041
(def max-evidence-characters
  "How many characters of ROW EVIDENCE one `inspect_clojure` text block
  renders when nothing narrower is imposed.

  The text block is not a summary of the receipt; it is the only copy of the
  receipt a text-only client ever sees. It is bounded because it travels
  inside a public MCP result whose complete size is bounded (MCP-OP-STUDY-040)
  — never because rows are optional."
  8192)

;; @spec MCP-OP-STUDY-041
(def ^:private min-evidence-characters
  "The floor one result keeps when a batch divides the allowance."
  512)

;; @spec MCP-OP-STUDY-041
(def ^:private max-continuation-characters
  "How large a `next_call` may be before the text names where it lives
  instead of spelling it. A continuation too long to retype is not a
  continuation, and a truncated one is worse than none."
  2048)

(defn- compact-json
  [value]
  (json/generate-string (json-data value)))

(defn- dependency-row
  [{:keys [name type line depends_on]}]
  (str name " " type "@" line
       (if (seq depends_on)
         (str " → " (str/join ", " depends_on))
         " → (none)")))

(defn- dep-tree-rows
  "One row per node of an `ls-deps` tree, depth-first in receipt order."
  [node depth]
  (let [indent (apply str (repeat depth "  "))]
    (if (:circular? node)
      [(str indent (:name node) " (circular)")]
      (into [(str indent (:name node) " " (:type node) "@" (:line node)
                  (when (:leaf? node) " (leaf)"))]
            (mapcat #(dep-tree-rows % (inc depth)))
            (:deps node)))))

;; @spec MCP-OP-STUDY-041
(defn result-evidence
  "The evidence one result's receipt carries, in RECEIPT ORDER.

  Each entry is `{:row <one line> :body <verbatim source or nil>}`. This is
  the single place a mode says what its rows are: the text renderer prints
  these and nothing else, so the rendered rows and the receipt rows cannot
  disagree by construction, and a witness can compare the two directly.

  Field evidence (O2 re-review, 2026-09-03): every mode below rendered a row
  COUNT — `request-1: deps · 27 of 27 rows` — while the rows sat in
  `structuredContent`. A text-only client was handed the shape of an answer
  and none of it, which is the same defect MCP-OP-STUDY-036 closed on
  `ls-tree` alone."
  [result]
  (case (:operation result)
    "forms"
    (mapv (fn [form]
            {:row (str (:name form) "@" (:line form) "-" (:end_line form)
                       " " (:form_type form))
             :body (:source form)})
          (:forms result))

    "outline"
    (mapv (fn [form]
            {:row (str (:line form) "-" (:end_line form) " "
                       (:type form) " " (:name form)
                       (when (:args form) (str " " (:args form)))
                       (when-let [symbols (:string_symbols form)]
                         (when (seq symbols)
                           (str " · string symbols "
                                (str/join ", " symbols)))))})
          (get-in result [:outline :forms]))

    "match"
    (mapv (fn [match]
            {:row (str (or (:inside match) "(top level)")
                       "@" (:line match) "-" (:end_line match))
             :body (:source match)})
          (:matches result))

    "xray"
    (if (contains? result :value)
      [{:row (str "value " (compact-json (:value result)))}]
      [])

    "deps"
    (mapv #(hash-map :row (dependency-row %)) (:deps result))

    "topo"
    (let [topo (:topo result)]
      (into (into []
                  (map-indexed (fn [index name]
                                 {:row (str "sorted " (inc index) ". " name)}))
                  (:sorted topo))
            (map (fn [name] {:row (str "cycle " name)}))
            (:cycles topo)))

    "ls-deps"
    (mapv #(hash-map :row %) (dep-tree-rows (:dep_tree result) 0))

    "ls-extract"
    (mapv #(hash-map :row (dependency-row %))
          (get-in result [:closure :forms]))

    []))

;; @spec MCP-OP-STUDY-041
(defn result-rows
  "The rows one result's receipt carries, in receipt order."
  [result]
  (mapv :row (result-evidence result)))

(defn- result-headline
  [result]
  (case (:operation result)
    "forms"
    (str (plural (count (:forms result)) "form") " · "
         (:source_character_count result) " source characters read")

    "outline"
    (let [outline (:outline result)
          forms (:forms outline)
          symbols (reduce + 0 (map #(count (:string_symbols %)) forms))]
      (str (:lines outline) " lines · "
           (plural (:form_count outline) "form")
           (when (some #(contains? % :string_symbols) forms)
             (str " · " (plural symbols "string symbol")))))

    "match"
    (str (plural (:match_count result) "match")
         (when (:inside result) (str " inside " (:inside result))))

    "xray" "xray"

    ("deps" "topo" "ls-deps" "ls-extract")
    (str (:operation result) " · " (:returned result) " of "
         (or (:form_count result) (:returned result)) " rows"
         (when (:truncated result)
           (str " · truncated, " (:omitted result) " omitted")))

    (:operation result)))

;; @spec MCP-OP-STUDY-041
(defn- continuation-line
  "The `next_call` spelled where a text-only client can read it."
  [result]
  (when-let [call (:next_call result)]
    (let [rendered (compact-json (:arguments call))]
      (if (<= (count rendered) max-continuation-characters)
        (str "    → next call: " (:tool call) " " rendered)
        (str "    → next call: " (:tool call)
             " · " (count rendered) " characters · see structuredContent"
             ".results[" (:id result) "].next_call")))))

;; @spec MCP-OP-STUDY-041
;; @spec MCP-OP-STUDY-040
(defn- render-evidence
  "Render one result's rows into `allowance` characters of text.

  Rows are dropped WHOLE and only from the tail, and every drop is declared.
  A row whose BODY does not fit renders as its row line alone and is counted
  as a dropped body, never as a row rendered whole.

  Field evidence (Sol O2 round-2 review, section 2): the previous rule always
  rendered the first row, counted it as shown, and silently omitted its body,
  so a 10,000-character form body left the text under `1 of 1 rows` and
  `terminal evidence · read_complete=true · next action none`. A caller told
  the read is terminal and handed none of the answer is worse off than one
  told the text was abridged."
  [result allowance]
  (let [items (result-evidence result)
        total (count items)
        bodies-total (count (filter #(seq (:body %)) items))]
    (loop [remaining items lines [] shown 0 bodies 0 used 0]
      (if-let [{:keys [row body]} (first remaining)]
        (let [row-line (str "    · " row)
              row-cost (inc (count row-line))
              body-lines (when (seq body)
                           (mapv #(str "      " %) (str/split-lines body)))
              body-cost (reduce + 0 (map #(inc (count %)) body-lines))]
          (cond
            (<= (+ used row-cost body-cost) allowance)
            (recur (next remaining)
                   (into lines (cons row-line body-lines))
                   (inc shown)
                   (cond-> bodies (seq body-lines) inc)
                   (+ used row-cost body-cost))

            (<= (+ used row-cost) allowance)
            (recur (next remaining)
                   (conj lines row-line)
                   (inc shown)
                   bodies
                   (+ used row-cost))

            :else
            {:lines lines :shown shown :total total
             :bodies bodies :bodies-total bodies-total
             :abridged true}))
        {:lines lines :shown shown :total total
         :bodies bodies :bodies-total bodies-total
         :abridged (or (< shown total) (< bodies bodies-total))}))))

;; @spec MCP-OP-STUDY-041
(defn- concise-result-block
  "The text one result contributes: its headline, its rows, and — whenever the
  rows do not all fit or the receipt itself is truncated — what to send next."
  [result allowance]
  (let [rendered (render-evidence result allowance)
        headline (str "  " (:id result) ": " (result-headline result))]
    {:abridged (:abridged rendered)
     :text
     (str/join
       "\n"
       (remove nil?
               (concat
                 [headline]
                 (:lines rendered)
                 [(when (:abridged rendered)
                    (format (str "    ! text abridged · %d of %d row%s "
                                 "rendered%s · the complete receipt is in "
                                 "structuredContent.results[%s]")
                            (:shown rendered) (:total rendered)
                            (if (= 1 (:total rendered)) "" "s")
                            (if (< (:bodies rendered) (:bodies-total rendered))
                              (format " · %d of %d row bod%s rendered"
                                      (:bodies rendered)
                                      (:bodies-total rendered)
                                      (if (= 1 (:bodies-total rendered))
                                        "y" "ies"))
                              "")
                            (:id result)))
                  (when (and (:abridged rendered) (not (:truncated result)))
                    (str "    → narrow the request so the whole answer fits "
                         "the text block, or read structuredContent"))
                  (when (:truncated result)
                    (continuation-line result))
                  (when (and (:truncated result) (:remedy result))
                    (str "    → " (:remedy result)))])))}))

;; @spec MCP-OP-STUDY-040
(defn- whole-block-prefix
  "The longest WHOLE-BLOCK prefix of `blocks` inside `limit` characters.

  Rows are dropped whole inside a block; blocks are dropped whole across
  results. Field evidence (Sol O2 round-2 review, section 2): a 32-result
  batch whose receipt measured 31,549 bytes was REFUSED under a 32,768 byte
  budget, because every result claimed a 512-character floor the budget was
  never allowed to lower — so no text rendering existed that the complete
  public result could fit inside."
  [blocks limit]
  (loop [remaining blocks kept [] used 0]
    (if-let [block (first remaining)]
      (let [cost (inc (count (:text block)))]
        (if (and (seq kept) (> (+ used cost) limit))
          {:blocks kept :dropped (- (count blocks) (count kept))
           :total (count blocks) :used used}
          (recur (next remaining) (conj kept block) (+ used cost))))
      {:blocks kept :dropped 0 :total (count blocks) :used used})))

;; @spec MCP-OP-STUDY-040
(def text-omitted-notice-header
  "The header of the text block for a receipt that leaves no room to render
  itself: it names the tool, says this text is not the receipt, and points at
  the place the complete receipt is."
  (str "inspect_clojure\n"
       "! text omitted · the complete receipt left no room to render it\n"
       "→ the complete result is in structuredContent\n"
       "→ read_structured_content"))

;; @spec MCP-OP-STUDY-040
(def minimum-text-header
  "The shortest honest naming there is: the tool's own name."
  "inspect_clojure")

;; @spec MCP-OP-STUDY-048
(defn- rung-text
  "One fall-through rung's text: its fixed header, plus the DECLARATION of
  every receipt leaf that header does not carry.

  Field evidence (Sol O2 round-5 review, 2026-09-04, section 3): the notice
  rung published 151 characters over ELEVEN uncarried leaves and zero fact
  pointers. A rung exists to say `the receipt did not fit here`; a rung that
  cannot say WHAT did not fit tells a caller less than the count line it
  replaced. The declaration is bounded by construction — one count line and at
  most `max-named-dropped-labels` pointers — so a rung carrying it is still a
  rung."
  [header result]
  (let [block (fact-block header result 0)]
    (if-let [section (fact-section block)]
      (str header "\n\n" section)
      header)))

;; @spec MCP-OP-STUDY-040
;; @spec MCP-OP-STUDY-048
(defn text-omitted-notice
  "The text block for a receipt that leaves no room to render itself — the
  last rung before a refusal, and it still names what it omits."
  [result]
  (rung-text text-omitted-notice-header result))

;; @spec MCP-OP-STUDY-040
;; @spec MCP-OP-STUDY-048
(defn minimum-text-block
  "The shortest honest text block there is: the tool's own name, and the count
  and first pointers of the receipt leaves it does not carry. A receipt that
  cannot leave room even for this is a typed refusal."
  [result]
  (rung-text minimum-text-header result))

;; @spec MCP-OP-STUDY-041
;; @spec MCP-OP-STUDY-044
(defn concise-summary
  "Render the MCP text companion — every leaf the receipt carries, bounded.

  Bounded is not the same as elided. Above the allowance the block says how
  many of how many rows and facts it rendered and what to send next; it never
  claims terminal evidence over evidence it dropped."
  [result]
  (case (:text_omitted result)
    "notice" (text-omitted-notice result)
    "name" (minimum-text-block result)
    (let [results (:results result)
          forms (reduce + 0 (map #(or (:form_count %) 0) results))
          matches (reduce + 0 (map #(or (:match_count %) 0) results))
          imposed (:text_evidence_limit result)
          limit (or imposed max-evidence-characters)
          ;; The per-result floor is a fairness rule for the DEFAULT
          ;; allowance. A limit the output budget imposed overrides it: a
          ;; floor the budget cannot lower is a floor that turns a renderable
          ;; receipt into a refusal.
          allowance (if imposed
                      (max 1 (quot limit (max 1 (count results))))
                      (max min-evidence-characters
                           (quot limit (max 1 (count results)))))
          ;; A result with no rows contributes no block: `match` with zero
          ;; matches has nothing to render, and a headline over nothing is the
          ;; count-shaped noise this change exists to remove.
          candidates (into [] (keep #(when (seq (result-evidence %))
                                       (concise-result-block % allowance)))
                           results)
          kept (whole-block-prefix candidates limit)
          blocks (:blocks kept)
          blocks-dropped (:dropped kept)
          rows-abridged? (or (pos? blocks-dropped)
                             (boolean (some :abridged blocks)))
          ;; @spec MCP-OP-STUDY-044
          ;; The ROW allowance is a rendering default (MCP-OP-STUDY-041); the
          ;; FACT allowance is the superset guarantee, and a guarantee bounded
          ;; by a constant is not one. Unimposed it is unbounded, and the
          ;; public output budget is the only thing that narrows it.
          fact-budget (if imposed
                        (max 0 (- limit (:used kept)))
                        unbounded-evidence)
          headline (cond-> [(plural (:request_count result) "request")
                            (plural (:file_count result) "file")]
                     (pos? forms) (conj (plural forms "form"))
                     (pos? matches) (conj (plural matches "match")))
          ;; @spec MCP-OP-STUDY-040
          ;; The clock is read through the envelope's own accessor, so the
          ;; text renders whatever shape the wire gives it.
          elapsed (mcp-operation/request-elapsed-ms result)
          render
          (fn [abridged? fact-text]
            (str "inspect_clojure\n"
                 "  " (str/join " · " headline) "\n\n"
                 "✓ all requests resolved\n"
                 "✓ ordered snapshot\n"
                 "✓ hashes attached\n"
                 ;; @spec MCP-OP-STUDY-044
                 ;; The status line SPELLS the receipt's own `read_complete`
                 ;; rather than a constant that happens to agree with it.
                 ;; Field evidence (Opus O2 round-4 review, section 4): the
                 ;; literal `read_complete=true` is exactly the label form the
                 ;; carriage predicate looks for, so the leaf was reported
                 ;; carried by a string that never read it — removing
                 ;; `:read_complete` from the receipt left the published text
                 ;; byte-identical in every mode.
                 (cond
                   (:truncated result)
                   (str "! bounded receipt · read_complete="
                        (leaf-spelling (:read_complete result))
                        " · next action " (:next_action result) "\n")

                   ;; @spec MCP-OP-STUDY-041
                   ;; Never "terminal evidence · next action none" over
                   ;; evidence the text dropped: the receipt is complete,
                   ;; this rendering is not.
                   abridged?
                   (str "! text abridged · read_complete="
                        (leaf-spelling (:read_complete result))
                        " · next action "
                        "read_structured_content_or_narrow_request\n")

                   :else
                   (str "✓ terminal evidence · read_complete="
                        (leaf-spelling (:read_complete result))
                        " · next action none\n"))
                 (when (pos? blocks-dropped)
                   (format (str "! text abridged · %d of %d result block%s "
                                "rendered · the complete receipt is in "
                                "structuredContent.results\n")
                           (- (:total kept) blocks-dropped) (:total kept)
                           (if (= 1 (:total kept)) "" "s")))
                 (when (seq blocks)
                   (str "\n" (str/join "\n" (map :text blocks)) "\n"))
                 (when fact-text (str "\n" fact-text "\n"))
                 "  " (format "%,d" (long (:source_character_count result)))
                 " source characters · "
                 (mcp-operation/format-elapsed-ms elapsed)))
          ;; @spec MCP-OP-STUDY-044
          ;; The facts are measured against the STRUCTURAL text that actually
          ;; ships, so the pair cannot drift: dropping a fact changes the
          ;; status line, which changes what the structural text already
          ;; carries, so the second pass measures the shipped rendering.
          first-pass (render rows-abridged? nil)
          first-facts (fact-block first-pass result fact-budget)
          abridged? (or rows-abridged? (:dropped first-facts))
          facts (if (= abridged? rows-abridged?)
                  first-facts
                  (fact-block (render abridged? nil) result fact-budget))]
      (render abridged? (fact-section facts)))))
