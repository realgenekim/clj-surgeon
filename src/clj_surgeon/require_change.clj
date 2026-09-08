(ns clj-surgeon.require-change
  "Pure standalone require intent; only owned complete lines are spliced."
  (:require
   [clj-surgeon.mcp-paths :as paths]
   [clj-surgeon.structural-lens :as lens]
   [clojure.string :as str]
   [clojure.walk :as walk]
   [rewrite-clj.node :as n]
   [rewrite-clj.parser :as parser]))

(def max-files 128)
(def max-file-bytes (* 2 1024 1024))
(def max-bytes (* 16 1024 1024))
(def layout {:order "stable-lib-symbol" :comments "attach-following" :blank_lines 0})
(def string-schema {:type "string" :minLength 1 :maxLength 4096})
(defn- object [properties required]
  {:type "object" :additionalProperties false :properties properties :required required})

;; @spec REQUIRE-CHANGE-001
(def schema
  (object
    {"op" {:type "string" :enum ["require_change"]}
     "workspace_root" string-schema
     "add" (object {"lib" string-schema
                    "alias_policy" {:type "array" :minItems 1 :maxItems 32 :uniqueItems true :items string-schema}}
                   ["lib" "alias_policy"])
     "files" {:type "array" :minItems 1 :maxItems max-files
              :items (object {"file" string-schema
                              "source_hash" {:type "string" :pattern "^[0-9a-f]{64}$"}
                              "remove" (object {"lib" string-schema "as" string-schema} ["lib" "as"])} ["file"])}
     "expect" (object (into {} (for [k ["files" "adds" "removes"]] [k {:type "integer" :minimum 0}]))
                      ["files" "adds" "removes"])
     "layout" (object (into {} (for [[k v] layout] [(name k) {:enum [v]}])) (mapv name (keys layout)))
     "verification" (object {"profile" string-schema} ["profile"])
     "plan_only" {:type "boolean"}}
    ["workspace_root" "add" "files" "expect" "verification"]))

(defn refuse! [type message evidence]
  (throw (ex-info message (assoc evidence :error-type type))))

(defn- schema-errors [s x path]
  (let [type-ok? (case (:type s)
                   "object" (map? x) "array" (vector? x) "string" (string? x)
                   "integer" (integer? x) "boolean" (boolean? x) true)]
    (if-not type-ok? [{:path path :reason :wrong-type}]
      (vec
        (concat
          (when (and (:enum s) (not (some #{x} (:enum s)))) [{:path path :reason :invalid-value}])
          (case (:type s)
            "object" (concat
                       (for [k (:required s) :when (not (contains? x (keyword k)))] {:path (conj path k) :reason :missing-field})
                       (for [k (keys x) :when (not (contains? (:properties s) (name k)))] {:path (conj path (name k)) :reason :unknown-field})
                       (mapcat (fn [[k v]] (when-let [child (get (:properties s) (name k))] (schema-errors child v (conj path (name k))))) x))
            "array" (concat
                      (when (or (and (:minItems s) (< (count x) (:minItems s)))
                                (and (:maxItems s) (> (count x) (:maxItems s)))
                                (and (:uniqueItems s) (not= (count x) (count (distinct x)))))
                        [{:path path :reason :invalid-cardinality}])
                      (mapcat #(schema-errors (:items s) %2 (conj path %1)) (range) x))
            "string" (when (or (and (:minLength s) (< (count x) (:minLength s)))
                               (and (:maxLength s) (> (count x) (:maxLength s)))
                               (and (:pattern s) (not (re-matches (re-pattern (:pattern s)) x))))
                       [{:path path :reason :invalid-string}])
            "integer" (when (< x (:minimum s 0)) [{:path path :reason :negative-count}])
            nil))))))

(defn validate-request [request]
  (schema-errors schema (walk/keywordize-keys request) []))

(defn- plain-symbol? [s]
  (and (string? s) (<= (count s) 256)
       (re-matches #"[A-Za-z_*!?$%&=<>+.-][A-Za-z0-9_*!?$%&=<>+.-]*" s)
       (not (#{"nil" "true" "false" "." ".."} s))))

(defn- validate! [r]
  (when-let [errors (seq (validate-request r))]
    (refuse! :invalid-request "Invalid standalone require_change request" {:blockers (vec errors)}))
  (doseq [s (concat [(get-in r [:add :lib])] (get-in r [:add :alias_policy])
                    (mapcat #(vals (:remove %)) (:files r)))]
    (when-not (plain-symbol? s)
      (refuse! :invalid-request "Library and alias names must be plain unqualified symbols" {:symbol s})))
  (let [files (mapv :file (:files r))]
    (when-not (= (count files) (count (distinct files)))
      (refuse! :invalid-request "File vector must contain unique paths" {}))
    (doseq [f files]
      (when-not (and (paths/relative-source-path? f) (str/ends-with? f ".clj"))
        (refuse! :invalid-path "Expected an explicit relative .clj path" {:file f}))))
  r)

(defn- meaningful? [node]
  (not (or (n/whitespace? node) (n/comment? node))))
(defn- value [node] (try (n/sexpr node) (catch Exception _ nil)))
(defn- headed? [head node]
  (and (= :list (n/tag node)) (= head (some->> (n/children node) (filter meaningful?) first value))))
(defn- line-start [source offset]
  (if (zero? offset) 0 (inc (or (str/last-index-of source "\n" (dec offset)) -1))))
(defn- line-end [source offset]
  (if-let [newline (str/index-of source "\n" offset)] (inc newline) (count source)))
(defn- indent-at [source offset]
  (let [prefix (subs source (line-start source offset) offset)]
    (when (re-matches #"[ \t]*" prefix) prefix)))
(defn- group-start [source start]
  (loop [at (line-start source start)]
    (if (zero? at) at
      (let [previous (line-start source (dec at))]
        (if (re-matches #"[ \t]*;[^\n]*\n" (subs source previous at))
          (recur previous) at)))))

;; @spec REQUIRE-CHANGE-008
(defn require-facts [source file]
  (let [root (parser/parse-string-all source)
        namespaces (filter #(headed? 'ns %) (n/children root))
        _ (when-not (= 1 (count namespaces))
            (refuse! :ambiguous-namespace "Expected exactly one direct namespace form" {:file file}))
        clauses (filter #(headed? :require %) (n/children (first namespaces)))
        _ (when-not (= 1 (count clauses))
            (refuse! :ambiguous-require "Expected exactly one direct require clause" {:file file}))
        clause (first clauses)
        starts (vec (cons 0 (map inc (keep-indexed #(when (= %2 \newline) %1) source))))
        span (fn [node]
               (let [{:keys [row col end-row end-col]} (meta node)]
                 [(+ (nth starts (dec row)) (dec col)) (+ (nth starts (dec end-row)) (dec end-col))]))
        entries (mapv
                  (fn [node]
                    (let [v (value node) [start end] (span node)]
                      (when-not (and (= :vector (n/tag node)) (vector? v) (symbol? (first v))
                                     (or (= 1 (count v))
                                         (and (= 3 (count v)) (= :as (second v)) (symbol? (nth v 2))))
                                     (not-any? #(or (n/comment? %) (= :uneval (n/tag %)))
                                               (tree-seq n/inner? n/children node))
                                     (= (:row (meta node)) (:end-row (meta node))))
                        (refuse! :unsupported-require "Only single-line direct [lib] or [lib :as alias] entries are supported" {:file file}))
                      {:lib (str (first v)) :as (some-> (get v 2) str)
                       :start start :end end :group-start (group-start source start)
                       :indent (indent-at source start)}))
                  (rest (filter meaningful? (n/children clause))))]
    (when (empty? entries) (refuse! :unsupported-require "The direct require clause must contain an entry" {:file file}))
    (doseq [[alias bindings] (group-by :as (filter :as entries)) :when (> (count bindings) 1)]
      (refuse! :ambiguous-alias "An alias has multiple direct bindings" {:file file :alias alias :bindings (mapv :lib bindings)}))
    {:entries entries :clause-span (span clause)}))

;; @spec REQUIRE-CHANGE-002
;; @spec REQUIRE-CHANGE-003
;; @spec REQUIRE-CHANGE-004
(defn choose-alias [file entries lib policy]
  (let [targets (filter #(= lib (:lib %)) entries)
        aliases (into {} (keep #(when (:as %) [(:as %) (:lib %)])) entries)
        collisions (vec (keep (fn [a] (when-let [bound (get aliases a)]
                                        (when (not= lib bound) {:alias a :lib bound}))) policy))
        existing (first (filter #(= lib (get aliases %)) policy))
        free (first (remove #(contains? aliases %) policy))]
    (cond
      (> (count targets) 1) (refuse! :duplicate-target "Target library occurs more than once" {:file file :lib lib})
      existing {:alias existing :reason "reuse-existing" :collisions collisions :adds 0}
      (seq targets) (refuse! :target-alias-outside-policy "Target is already required without a preferred alias" {:file file :lib lib})
      free {:alias free :reason "first-free" :collisions collisions :adds 1}
      :else (refuse! :alias-policy-exhausted "Every policy alias is bound to another library" {:file file :bound_aliases collisions}))))

(defn splice [source edits]
  (reduce (fn [s {:keys [start end text]}] (str (subs s 0 start) text (subs s end)))
          source (sort-by (juxt :start :end) #(compare %2 %1) edits)))

;; @spec REQUIRE-CHANGE-007
(defn- removal [source file entries remove]
  (when remove
    (let [matches (filter #(= remove (select-keys % [:lib :as])) entries)]
      (when-not (= 1 (count matches))
        (refuse! :removal-mismatch "Explicit removal must match exactly one libspec" {:file file :remove remove}))
      (let [{:keys [start end group-start indent]} (first matches)
            begin (line-start source start) finish (line-end source end)]
        (when-not (and indent (= group-start begin)
                       (re-matches #"[ \t]*(?:\r?\n)?" (subs source end finish)))
          (refuse! :unsupported-removal "Removal needs a standalone line without attached or trailing comments or closing parentheses" {:file file :remove remove}))
        {:start begin :end finish :text ""}))))

;; @spec REQUIRE-CHANGE-005
(defn- insertion [source file {:keys [entries clause-span]} lib alias]
  (let [before (first (filter #(pos? (compare (:lib %) lib)) entries))
        nearest (or before (last entries))
        ;; Hanging first entries inherit the actual column of that entry.
        indent (or (:indent nearest)
                   (apply str (repeat (- (:start nearest) (line-start source (:start nearest))) " ")))
        eol (if (str/ends-with? (subs source (:start nearest) (line-end source (:end nearest))) "\r\n") "\r\n" "\n")
        at (if before (:group-start before) (line-end source (:end nearest)))]
    (when (or (and before (nil? (:indent before)))
              (and (nil? before) (>= at (second clause-span))))
      (refuse! :unsupported-layout "A whole-line insertion cannot preserve the existing hanging entry or closing-parenthesis line at this position" {:file file}))
    {:start at :end at :text (str indent "[" lib " :as " alias "]" eol)}))

(defn- compile-file [r sources {:keys [file source_hash remove]}]
  (let [source (get sources file)
        _ (when-not (string? source) (refuse! :missing-source "Explicit file is absent from the snapshot" {:file file}))
        before-hash (lens/source-hash source)
        _ (when (and source_hash (not= source_hash before-hash))
            (refuse! :source-hash-mismatch "Explicit source hash is stale" {:file file :expected source_hash :actual before-hash}))
        {:keys [lib alias_policy]} (:add r)
        _ (when (= lib (:lib remove)) (refuse! :invalid-request "Target library cannot also be removed" {:file file}))
        facts (require-facts source file)
        choice (choose-alias file (:entries facts) lib alias_policy)
        deletion (removal source file (:entries facts) remove)
        ;; Reparse the deletion candidate to allocate against the remaining groups;
        ;; alias choice remains bound to the original namespace.
        without (if deletion (splice source [deletion]) source)
        addition (when (pos? (:adds choice))
                   (if (and deletion (= 1 (count (:entries facts))))
                     (let [entry (first (:entries facts))
                           eol (if (str/ends-with? (subs source (:start deletion) (:end deletion)) "\r\n") "\r\n" "\n")]
                       {:start (:start deletion) :end (:start deletion)
                        :text (str (:indent entry) "[" lib " :as " (:alias choice) "]" eol)})
                     (insertion without file (require-facts without file) lib (:alias choice))))
        future (if addition (splice without [addition]) without)]
    (parser/parse-string-all future)
    {:file file :source source :future future :deletion deletion :addition addition
     :decision (merge choice {:file file :removes (if deletion 1 0)
                              :source_hash before-hash :result_hash (lens/source-hash future)})}))

;; @spec REQUIRE-CHANGE-006
(defn validate-candidate
  "Reconstruct the authorized line delta independently of the supplied candidate.
  No source or formatter output is trusted merely because it carries hashes."
  [request sources futures]
  (try
    (let [expected (mapv #(compile-file request sources %) (:files request))
          expected-map (into {} (keep #(when (not= (:source %) (:future %)) [(:file %) (:future %)])) expected)]
      (= expected-map futures))
    (catch Exception _ false)))

;; @spec REQUIRE-CHANGE-001
;; @spec REQUIRE-CHANGE-009
;; @spec REQUIRE-CHANGE-010
;; @spec REQUIRE-CHANGE-012
(defn compile-change [request sources]
  (try
    (let [r (validate! (walk/keywordize-keys request))
          results (mapv #(compile-file r sources %) (:files r))
          changed (filter #(not= (:source %) (:future %)) results)
          decisions (mapv :decision results)
          counts {:files (count changed) :adds (reduce + (map :adds decisions)) :removes (reduce + (map :removes decisions))}]
      (when-not (= (:expect r) counts)
        (refuse! :expect-mismatch "Actual require changes differ from exact expected counts" {:expected (:expect r) :actual counts}))
      {:ok true :counts counts :expected (:expect r) :decisions decisions
       :original-sources (into (sorted-map) (map (juxt :file :source)) changed)
       :future-sources (into (sorted-map) (map (juxt :file :future)) changed)
       :guard-sources (select-keys sources (map :file (:files r)))
       :created-files [] :deleted-files [] :created-directories [] :form-count 0 :caller-edit-count 0
       :caller-proof {:kind :require-only :symbol-edits 0 :protected-bytes true}
       :format {:mode :whole-line-splice}})
    (catch Exception error
      {:ok false :state "refused" :committed false :source_unchanged true :mutation_attempted false
       :verification_complete false :error_type (name (or (:error-type (ex-data error)) :require-parse-failed))
       :error (.getMessage error) :evidence (dissoc (ex-data error) :error-type)})))
