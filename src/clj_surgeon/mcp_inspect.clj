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
   [clojure.walk :as walk]))

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
               {:missing missing}))))

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
                 {:missing (vec missing)})))
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

(defn- json-key
  [key]
  (if (keyword? key)
    (keyword (str/replace (name key) "-" "_"))
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
(defn leaf-label
  "`results[0].source_anchor.range.start.line` — the JSON pointer a caller
  reads the same fact back out of `structuredContent` with."
  [path]
  (apply str
         (map-indexed (fn [index segment]
                        (cond
                          (integer? segment) (str "[" segment "]")
                          (zero? index) (name segment)
                          :else (str "." (name segment))))
                      path)))

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
  why `null`, `{}`, `[]` and a blank string can never be carried by value."
  [value]
  (or (value-less-leaf? value)
      (number? value)
      (boolean? value)
      (< (count (leaf-spelling value)) min-distinctive-spelling)))

;; @spec MCP-OP-STUDY-044
(defn labelled-leaf
  "`results[0].platforms=[]` — the ONE spelling that carries a value-less
  leaf, and the exact string its witness looks for."
  [path value]
  (str (leaf-label path) "=" (leaf-spelling value)))

;; @spec MCP-OP-STUDY-044
(defn leaf-rendered?
  "Does `text` carry this receipt leaf VERBATIM?

  ONE predicate, used both by the renderer that guarantees the property and by
  the witness that checks it, so the two can never drift apart. A multi-line
  value is carried when every one of its non-blank lines is; a COLLIDABLE leaf
  — value-less, numeric, boolean, or spelled in fewer than
  `min-distinctive-spelling` characters — is carried only as
  `pointer=spelling`, because a short spelling found in the text is not
  evidence that the text carries THIS leaf."
  [text path value]
  (if (collidable-leaf? value)
    (str/includes? text (labelled-leaf path value))
    (let [rendered (leaf-spelling value)]
      (if (str/includes? rendered "\n")
        (every? #(or (str/blank? %) (str/includes? text %))
                (str/split-lines rendered))
        (str/includes? text rendered)))))

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
  (into []
        (remove (fn [[path value]]
                  (or (leaf-excluded? path)
                      (leaf-rendered? text path value))))
        (receipt-leaf-pairs result)))

;; @spec MCP-OP-STUDY-044
(defn receipt-fact-entries
  "One entry per receipt leaf `structural-text` does not already carry, in
  receipt order: its JSON pointer and the line that renders it.

  The default is to RENDER: the rows a mode renders structurally satisfy the
  criterion where they can, and everything left over prints here. A receipt
  field added tomorrow therefore travels into the text the day it is added,
  and only a member of `text-excluded-leaf-keys` can keep it out.

  A COLLIDABLE leaf prints as `pointer=spelling` — the only rendering of it a
  reader can attribute to the leaf — and every other leaf as `pointer: value`.

  Field evidence (Sol O2 round-2 review, section 3): the round-2 renderer
  projected selected row fields, so `forms` dropped every `source_anchor`
  range and both hashes, `outline` dropped every `platforms` entry, `match`
  dropped every `hash` and `preorder`, `ls-deps` dropped every `leaf?`, and
  every mode dropped its top-level metadata — 182 leaves over nine modes, not
  one of them named anywhere as deliberately excluded."
  [structural-text result]
  (first
    (reduce
      (fn [[entries text] [path value]]
        (if (or (leaf-excluded? path)
                (leaf-rendered? text path value))
          [entries text]
          (let [rendered (leaf-spelling value)
                line (cond
                       (collidable-leaf? value)
                       (str "  " (labelled-leaf path value))

                       (str/includes? rendered "\n")
                       (str/join "\n"
                                 (cons (str "  " (leaf-label path) ":")
                                       (map #(str "    " %)
                                            (str/split-lines rendered))))

                       :else (str "  " (leaf-label path) ": " rendered))]
            [(conj entries {:path path :label (leaf-label path) :line line})
             (str text "\n" line)])))
      [[] structural-text]
      (receipt-leaf-pairs result))))

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

;; @spec MCP-OP-STUDY-044
;; @spec MCP-OP-STUDY-040
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

  Field evidence (Opus O2 round-4 review, 2026-09-04, sections 2 and 3): round
  four charged the fact LINES and left the header and the `dropped:` line
  outside the allowance, so at allowance 0 the rendering was 22,785 characters
  — LARGER than at allowance 9,434 — and `fit-public-result` searched a
  `fits?` that was not monotone, found nothing, and published a 151-character
  notice for an ordinary two-file `outline` batch."
  [structural-text result budget]
  (let [entries (vec (receipt-fact-entries structural-text result))
        total (count entries)
        labels (mapv :label entries)
        ;; Prefix sums so the descent below costs one comparison per step: a
        ;; section rendered per candidate would be quadratic in the fact
        ;; count, and the fit evaluates dozens of candidates.
        prefix (vec (reductions + 0 (map #(inc (count (:line %))) entries)))
        section-length
        (fn [shown]
          (let [dropped? (< shown total)]
            (+ (count (fact-section-header shown total dropped?))
               (if dropped?
                 (inc (count (dropped-line (subvec labels shown))))
                 0)
               (nth prefix shown))))
        shown (loop [n total]
                (if (or (zero? n) (<= (section-length n) budget))
                  n
                  (recur (dec n))))
        dropped? (< shown total)
        section (when (or (pos? shown) dropped?)
                  (str/join
                    "\n"
                    (concat
                      [(fact-section-header shown total dropped?)]
                      (when dropped? [(dropped-line (subvec labels shown))])
                      (map :line (subvec entries 0 shown)))))]
    {:lines (mapv :line (subvec entries 0 shown))
     :shown shown
     :total total
     :dropped dropped?
     :dropped-labels (subvec labels shown)
     ;; The section is rendered HERE because this is where it was charged. A
     ;; section assembled elsewhere out of these pieces would be a second
     ;; rendering, and the budget would have been spent on the other one.
     :section (when (and section (<= (count section) budget)) section)}))

;; @spec MCP-OP-STUDY-044
(defn fact-section
  "The rendered receipt-fact section, or nil when the structural rendering
  already carried every leaf, or when the allowance cannot pay for even the
  count line.

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
      {:error "Structural match cardinality did not meet the declared expectation"
       :error-type :inspect-cardinality-mismatch
       :expected (get-in request [:expect :matches])
       :actual (:match-count found)
       :match-count (:match-count found)}

      :else
      (let [matches (mapv (fn [match]
                            (assoc (json-data match)
                                   :hash (structural-lens/source-hash
                                           (:source match))
                                   :file_hash (:hash snapshot)))
                          (:matches found))]
        {:id (:id request)
         :operation "match"
         :file (:file request)
         :file_hash (:hash snapshot)
         :match (:match request)
         :inside (:inside request)
         :match_count (:match-count found)
         :source_character_count (reduce + 0 (map #(count (:source %))
                                                  (:matches found)))
         :matches matches}))))

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
(def text-omitted-notice
  "The text block for a receipt that leaves no room to render itself.

  The last rung before a refusal: it names the tool, says this text is not
  the receipt, and points at the place the complete receipt is."
  (str "inspect_clojure\n"
       "! text omitted · the complete receipt left no room to render it\n"
       "→ the complete result is in structuredContent\n"
       "→ read_structured_content"))

;; @spec MCP-OP-STUDY-040
(def minimum-text-block
  "The shortest honest text block there is: the tool's own name. A receipt
  that cannot leave room even for this is a typed refusal."
  "inspect_clojure")

;; @spec MCP-OP-STUDY-041
;; @spec MCP-OP-STUDY-044
(defn concise-summary
  "Render the MCP text companion — every leaf the receipt carries, bounded.

  Bounded is not the same as elided. Above the allowance the block says how
  many of how many rows and facts it rendered and what to send next; it never
  claims terminal evidence over evidence it dropped."
  [result]
  (case (:text_omitted result)
    "notice" text-omitted-notice
    "name" minimum-text-block
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
          elapsed (:elapsed_ms result)
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
