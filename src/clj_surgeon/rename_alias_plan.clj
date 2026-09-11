(ns clj-surgeon.rename-alias-plan
  "Pure closed-snapshot alias prefix planning; no reader evaluation or reprinting."
  (:refer-clojure :exclude [simple-symbol?])
  (:require
   [clj-surgeon.insert-forms-plan :as p]
   [clj-surgeon.mcp-write-refusal :as refusal]
   [clojure.edn :as edn]
   [clojure.string :as str]
   [clojure.walk :as walk]
   [clj-splice.core :as splice]
   [clj-surgeon.splice-projection :as projection]))

(defn remedy [{:keys [error-type] :as data}]
  (case error-type
    (:invalid-request :invalid-path :invalid-guard :unsupported-source :malformed-utf8
     :limit-exceeded :source-hash-mismatch :source-changed-before-commit
     :source-parse-error :candidate-parse-error :candidate-structure-mismatch
     :io-error :commit-outcome-unknown) (p/remedy data)
    (:alias-collision :new-alias-capture) "Choose a fresh alias absent from bindings and references."
    (:alias-library-mismatch :old-alias-absent) "Supply the library and alias bound in the selected namespace."
    (:ambiguous-alias-binding :ambiguous-alias-namespace) "Resolve the ambiguous library or alias binding before retrying."
    (:multiple-ns-forms :ns-not-found :unsupported-ns-shape :unsupported-libspec)
    "Choose source with one supported static namespace declaration and require binding."
    :unsupported-namespace-mutation "Resolve runtime namespace mutations before renaming."
    (:scope-changed-before-commit :scope-file-count-mismatch) "Refresh the scope and its expected file count."
    :expect-count-mismatch "Review the reported references and supply their intended count."
    (p/remedy data)))

(defn refuse [e]
  (assoc (p/refusal e) :operation "rename_alias" :version 1 :remedy (remedy (ex-data e))))

(defn simple-symbol? [s]
  (and (string? s)
       (try (let [v (edn/read-string s)] (and (symbol? v) (nil? (namespace v)) (= s (str v))))
            (catch Exception _ false))))
(defn alias? [s]
  (and (simple-symbol? s) (not (#{"nil" "true" "false" "&" "_"} s))
       (not (re-find #"[\s/:,\[\]{}()'`~@^;\"\\#]" s))))
(defn lib? [s] (simple-symbol? s))
(defn nonnegative! [n at]
  (when-not (and (integer? n) (<= 0 n)) (p/refuse! :invalid-request at "Nonnegative integer required.")))

;; @spec RENAME-ALIAS-007
;; INTENT: RENAME-ALIAS-007
(defn validate! [r]
  (p/request-shape! r) (p/bounded! (pr-str r) :request)
  (p/closed! r [:version :workspace_root :scope :lib :old_alias :new_alias :expect :guards] [] [] :invalid-request)
  (when-not (and (= 1 (:version r)) (integer? (:version r)) (string? (:workspace_root r))
                 (lib? (:lib r)) (alias? (:old_alias r)) (alias? (:new_alias r))
                 (not= (:old_alias r) (:new_alias r)))
    (p/refuse! :invalid-request [] "Version 1, string root, library and distinct simple aliases required."))
  (when (= (:lib r) (:old_alias r))
    (p/refuse! :ambiguous-alias-namespace [:old_alias] "Alias equals the library's full spelling."))
  (let [scope (:scope r) selectors (filter #(contains? scope %) [:file :paths :repository])]
    (when-not (= 1 (count selectors)) (p/refuse! :invalid-request [:scope] "Exactly one scope selector required."))
    (p/closed! scope [(first selectors) :expect_files] [] [:scope] :invalid-request)
    (p/positive! (:expect_files scope) [:scope :expect_files])
    (case (first selectors)
      :file (when-not (string? (:file scope)) (p/refuse! :invalid-request [:scope :file] "String file required."))
      :paths (when-not (and (vector? (:paths scope)) (seq (:paths scope)) (every? string? (:paths scope))
                            (= (count (:paths scope)) (count (distinct (:paths scope)))))
               (p/refuse! :invalid-request [:scope :paths] "Distinct explicit file paths required."))
      :repository (when-not (true? (:repository scope)) (p/refuse! :invalid-request [:scope :repository] "Must be true."))))
  (p/closed! (:expect r) [:references] [] [:expect] :invalid-request)
  (let [refs (get-in r [:expect :references])]
    (when-not (and (map? refs) (#{#{:total} #{:per_file}} (set (keys refs))))
      (p/refuse! :invalid-request [:expect :references] "Exactly total or per_file required."))
    (if (contains? refs :total) (nonnegative! (:total refs) [:expect :references :total])
        (do (when-not (and (map? (:per_file refs)) (every? string? (keys (:per_file refs))))
              (p/refuse! :invalid-request [:expect :references :per_file] "Path-keyed counts required."))
            (doseq [[f count] (:per_file refs)] (nonnegative! count [:expect :references :per_file f])))))
  r)
(defn paths! [paths r]
  (doseq [f paths]
    (when (or (str/starts-with? f "/") (str/includes? f "\u0000")
              (some #{"" "." ".."} (str/split f #"/" -1)))
      (p/refuse! :invalid-path [:scope] "Confined relative file paths required." {:file f})))
  (when (> (count paths) 1000) (p/refuse! :limit-exceeded [:scope] "At most 1000 inspected files."))
  (when-not (= (get-in r [:scope :expect_files]) (count paths))
    (p/refuse! :scope-file-count-mismatch [:scope :expect_files] "Scope cardinality differs."
               {:expected (get-in r [:scope :expect_files]) :actual (count paths) :paths paths})))
(defn guards! [sources r]
  (when-not (and (map? (:guards r)) (= (set (keys sources)) (set (keys (:guards r)))))
    (p/refuse! :invalid-guard [:guards] "Guards must cover every inspected file exactly."))
  (when-let [counts (get-in r [:expect :references :per_file])]
    (when-not (= (set (keys sources)) (set (keys counts)))
      (p/refuse! :invalid-request [:expect :references :per_file] "Counts must cover every inspected path.")))
  (doseq [[f source] (sort-by key sources)]
    (try (p/guard! source {:workspace_root (:workspace_root r) :file f :guard (get-in r [:guards f])})
         (catch Exception e (throw (ex-info (.getMessage e) (assoc (ex-data e) :at [:guards f] :file f)))))))

(defn ns-form? [x] (and (= :list (:tag (p/unwrap x))) (= "ns" (p/kind x))))
(defn simple-node? [x] (and (= :token (:tag x)) (simple-symbol? (p/raw x))))
(defn declaration-vector? [x]
  (and (= :vector (:tag x)) (every? simple-node? (p/effective x))))
(defn libspec! [x file]
  (let [fail #(p/refuse! % [:source file :ns] "Unsupported require declaration." {:file file :line (:line x)})]
    (cond
      (simple-node? x) {:lib (p/raw x)}
      (= :vector (:tag x))
      (let [[lib & tail] (p/effective x) pairs (partition 2 tail) opts (into {} (map (fn [[a b]] [(p/raw a) b]) pairs))]
        (when-not (and (simple-node? lib) (even? (count tail))) (fail :unsupported-libspec))
        (when-not (= (count pairs) (count opts)) (fail :unsupported-libspec))
        (when-not (every? #{":as" ":as-alias" ":refer" ":rename" ":exclude"} (keys opts)) (fail :unsupported-ns-shape))
        (when (and (opts ":as") (opts ":as-alias")) (fail :unsupported-libspec))
        (doseq [k [":as" ":as-alias"] :when (opts k)] (when-not (simple-node? (opts k)) (fail :unsupported-libspec)))
        (when-let [v (opts ":refer")]
          (when-not (or (= ":all" (p/raw v)) (declaration-vector? v)) (fail :unsupported-libspec)))
        (when-let [v (opts ":exclude")] (when-not (declaration-vector? v) (fail :unsupported-libspec)))
        (when-let [v (opts ":rename")]
          (let [xs (p/effective v) referred (set (map p/raw (p/effective (opts ":refer")))) keys (map p/raw (take-nth 2 xs))]
            (when-not (and (= :map (:tag v)) (even? (count xs)) (every? simple-node? xs)
                           (= (count keys) (count (distinct keys)))
                           (or (= ":all" (p/raw (opts ":refer"))) (every? referred keys)))
              (fail :unsupported-libspec))))
        {:lib (p/raw lib) :alias (some-> (or (opts ":as") (opts ":as-alias")) p/raw)
         :binding (or (opts ":as") (opts ":as-alias"))})
      :else (fail :unsupported-ns-shape))))

;; @spec RENAME-ALIAS-006
;; INTENT: RENAME-ALIAS-006
(defn namespace! [root file]
  (let [roots (p/effective root) matches (filter ns-form? roots)]
    (when (empty? matches) (p/refuse! :ns-not-found [:source file] "Effective root ns required."))
    (when (> (count matches) 1) (p/refuse! :multiple-ns-forms [:source file] "Exactly one root ns required."))
    (when-not (= (first roots) (first matches)) (p/refuse! :unsupported-ns-shape [:source file] "ns must be first effective expression."))
    (let [owner (first matches) [_ name & tail] (p/effective (p/unwrap owner))
          _ (when-not (simple-node? (p/unwrap name)) (p/refuse! :unsupported-ns-shape [:source file] "Simple namespace name required."))
          tail (if (str/starts-with? (or (p/raw (first tail)) "") "\"") (rest tail) tail)
          tail (if (= :map (:tag (first tail))) (rest tail) tail)
          bindings (mapcat (fn [clause]
                             (let [[head & libs] (p/effective clause)]
                               (when-not (and (= :list (:tag clause)) (#{":require" ":import" ":refer-clojure" ":gen-class"} (p/raw head)))
                                 (p/refuse! :unsupported-ns-shape [:source file] "Only static standard namespace clauses supported."))
                               (when (= ":require" (p/raw head)) (map #(libspec! % file) libs)))) tail)]
      {:owner owner :bindings (vec bindings)})))

;; @spec RENAME-ALIAS-014
;; INTENT: RENAME-ALIAS-014
(defn site [node prefix alias role context file]
  (let [start (+ (:start node) prefix)]
    (merge (select-keys node [:form_index :line :end_line :address])
           {:file file :scope_kind "root" :role role :context context
            :offset (get (:utf16->byte node) start) :length (alength (p/bytes alias))
            :start start :end (+ start (count alias))})))

;; @spec RENAME-ALIAS-002
;; @spec RENAME-ALIAS-003
;; @spec RENAME-ALIAS-004
;; @spec RENAME-ALIAS-005
;; INTENT: RENAME-ALIAS-002
;; INTENT: RENAME-ALIAS-003
;; INTENT: RENAME-ALIAS-004
;; INTENT: RENAME-ALIAS-005
;; @spec RENAME-ALIAS-016
;; INTENT: RENAME-ALIAS-016
(defn references [root owner alias file]
  (let [declarations (set (map :start (filter #(and (= :list (:tag %))
                                                 (#{":require" ":import" ":refer-clojure" ":gen-class"} (p/head %)))
                                        (p/effective (p/unwrap owner)))))]
    (letfn [(walk [x contexts tag? commented?]
              (let [tag (:tag x) text (p/raw x)
                    contexts (if (and (= :list tag) (#{"quote" "clojure.core/quote"} (p/head x)))
                               (conj contexts "quote")
                               (case tag
                                 (:quote :var) (conj contexts "quote")
                                 :syntax-quote (conj contexts "syntax-quote")
                                 (:unquote :unquote-splicing) (vec (remove #{"syntax-quote"} contexts))
                                 (:meta :meta*) (conj contexts "metadata") contexts))
                    commented? (or commented? (and (= :list tag) (#{"comment" "clojure.core/comment"} (p/head x))))
                    context (peek contexts) children (p/effective x)
                    prefix (cond
                             (and (not tag?) (= :token tag) (str/starts-with? text (str alias "/"))) [0 "symbol"]
                             (and (= :token tag) (str/starts-with? text (str "::" alias "/"))) [2 "auto-keyword"]
                             (and (= :namespaced-map tag) (re-find (re-pattern (str "^#::" (java.util.regex.Pattern/quote alias) "(?=[{\\s,])")) text)) [3 "auto-map"])]
                (cond
                  (= :uneval tag) []
                  (declarations (:start x)) []
                  (and (= :list tag) (not commented?) (not= "quote" context)
                       (#{"in-ns" "alias" "ns-unalias" "require" "clojure.core/in-ns" "clojure.core/alias" "clojure.core/ns-unalias" "clojure.core/require"} (p/head x)))
                  (p/refuse! :unsupported-namespace-mutation [:source file] "Runtime namespace mutation is unsupported." {:file file :line (:line x)})
                  :else (concat (when prefix [(site x (first prefix) alias (second prefix) context file)])
                                (mapcat (fn [i c] (walk c contexts (and (= :reader-macro tag) (zero? i)) commented?)) (range) children)))))]
      (vec (mapcat #(walk % ["ordinary"] false false) (:children root))))))

;; @spec RENAME-ALIAS-008
;; INTENT: RENAME-ALIAS-008
(defn source! [source file]
  (when-not (str/ends-with? file ".clj") (p/refuse! :unsupported-source [:source file] "Only .clj files supported."))
  (try (p/lexical! source :source) (p/newline-style source)
       (try (projection/tree source)
            (catch Exception e
              (p/refuse! :source-parse-error [:source] (.getMessage e)
                         {:line (or (:row (ex-data e)) 1) :column (or (:col (ex-data e)) 1)})))
       (catch Exception e (throw (ex-info (.getMessage e) (assoc (ex-data e) :file file))))))
(defn selected-binding [bindings r]
  (first (filter #(and (= (:alias %) (:old_alias r)) (= (:lib %) (:lib r))) bindings)))
(defn binding! [{:keys [bindings effective-references] :as ns-data} r file]
  (let [duplicates (concat (filter #(> (val %) 1) (frequencies (map :lib bindings)))
                           (filter #(> (val %) 1) (frequencies (keep :alias bindings))))
        selected (selected-binding bindings r)
        old (or selected (first (filter #(= (:alias %) (:old_alias r)) bindings)))
        selected? (boolean selected)
        binding-sites (mapv (fn [b] (merge (select-keys b [:lib :alias])
                                      (when (:binding b) {:site (select-keys (:binding b) [:line :address :form_index :start :end])}))) bindings)]
    (when (and selected? (seq duplicates))
      (p/refuse! :ambiguous-alias-binding [:source file :ns] "Duplicate library or alias bindings." {:file file :bindings binding-sites}))
    (when (and (not selected?) (not (get-in r [:scope :repository])))
      (p/refuse! (if old :alias-library-mismatch :old-alias-absent) [:source file :ns] "Requested library and old alias must be bound." {:file file}))
    (assoc ns-data :selected? selected? :old old :binding-sites binding-sites
           :references (if selected? effective-references []))))
(defn collision! [{:keys [owner bindings binding-sites selected?]} root r file]
  (when selected?
    (when (some #(= (:alias %) (:new_alias r)) bindings)
      (p/refuse! :alias-collision [:new_alias] "New alias is already bound." {:file file :bindings binding-sites}))
    (let [capture (references root owner (:new_alias r) file)]
      (when (seq capture)
        (p/refuse! :new-alias-capture [:new_alias] "New alias would capture existing syntax." {:file file :sites capture})))))
(defn splice [source edits new]
  (splice/splice source (mapv (fn [{:keys [offset length]}] [[offset (+ offset length)] new]) edits)))
(defn form-evidence
  "Namespace-independent form digests, partitioned by declared change ordinals."
  [before after changed-indices]
  (let [aa (p/root-inventory before) bb (p/root-inventory after)
        rows (mapv (fn [i a]
                     {:before_index (inc i) :after_index (inc i)
                      :before_sha256 (p/sha (p/raw a))
                      :after_sha256 (when-let [b (get bb i)] (p/sha (p/raw b)))}) (range) aa)
        other (filterv #(not (contains? changed-indices (:before_index %))) rows)
        changed (filterv #(contains? changed-indices (:before_index %)) rows)
        spans (fn [root tags] (mapv p/raw (filter #(tags (:tag %)) (tree-seq (comp seq :children) :children root))))
        gaps-a (spans before #{:whitespace :newline :comma :comment}) gaps-b (spans after #{:whitespace :newline :comma :comment})
        discards-a (spans before #{:uneval}) discards-b (spans after #{:uneval})
        hashes (fn [a b] (mapv (fn [i] {:ordinal (inc i)
                                        :before_sha256 (when-let [x (get a i)] (p/sha x))
                                        :after_sha256 (when-let [y (get b i)] (p/sha y))})
                               (range (max (count a) (count b)))))]
    {:forms_changed (count changed) :changed_forms changed
     :preservation {:other_forms_checked (count other)
                    :other_forms_unchanged (and (= (count aa) (count bb))
                                             (every? #(= (:before_sha256 %) (:after_sha256 %)) other))
                    :other_forms other :gaps_unchanged (= gaps-a gaps-b) :discards_unchanged (= discards-a discards-b)
                    :gaps (hashes gaps-a gaps-b) :discards (hashes discards-a discards-b)}}))
(defn form-proof [before after changed-indices]
  (let [evidence (form-evidence before after changed-indices)
        preservation (:preservation evidence)]
    (when-not (every? true? (map preservation [:other_forms_unchanged :gaps_unchanged :discards_unchanged]))
      (p/refuse! :candidate-structure-mismatch [:candidate] "Root/trivia/discard preservation failed."))
    evidence))
(defn inverse-splices [source edits new]
  (loop [xs (sort-by :start edits) delta 0 result []]
    (if-let [x (first xs)]
      (let [old (subs source (:start x) (:end x)) after-size (alength (p/bytes new))]
        (recur (next xs) (+ delta (- after-size (:length x)))
               (conj result {:source_offset (:offset x) :result_offset (+ (:offset x) delta)
                             :before old :after new :before_sha256 (p/sha old) :after_sha256 (p/sha new)}))) result)))

;; @spec RENAME-ALIAS-001
;; @spec RENAME-ALIAS-010
;; INTENT: RENAME-ALIAS-001
;; INTENT: RENAME-ALIAS-010
(def ^:dynamic *candidate-text* identity)
(def ^:dynamic *candidate-roles* identity)
(def ^:dynamic *preservation-tree* identity)
(def ^:dynamic *inverse-evidence* identity)

;; @spec RENAME-ALIAS-012
;; INTENT: RENAME-ALIAS-012
(defn candidate! [source root info r file]
  (let [refs (:references info) binding (site (get-in info [:old :binding]) 0 (:old_alias r) "binding" "ordinary" file)
        sites (vec (sort-by :start (conj refs binding))) candidate (*candidate-text* (splice source sites (:new_alias r)))
        after (try (source! candidate file)
                   (catch Exception e (p/refuse! :candidate-parse-error [:candidate file] (.getMessage e) (select-keys (ex-data e) [:line :column]))))
        new-ns (namespace! after file) new-refs (*candidate-roles* (references after (:owner new-ns) (:new_alias r) file))
        old-refs (references after (:owner new-ns) (:old_alias r) file)
        expected-offsets (loop [xs sites delta 0 out []]
                           (if-let [x (first xs)]
                             (recur (next xs) (+ delta (- (count (:new_alias r)) (- (:end x) (:start x))))
                                    (cond-> out (not= "binding" (:role x)) (conj [(+ (:start x) delta) (:role x)]))) out))]
    (when-not (and (empty? old-refs) (= expected-offsets (mapv (juxt :start :role) new-refs))
                   (= 1 (count (filter #(and (= (:lib r) (:lib %)) (= (:new_alias r) (:alias %))) (:bindings new-ns)))))
      (p/refuse! :candidate-structure-mismatch [:candidate file] "Candidate alias roles differ from planned roles."))
    (let [inverse (*inverse-evidence* (inverse-splices source sites (:new_alias r)))
          restored (reduce (fn [bs {:keys [result_offset before after]}]
                             (let [prefix (java.util.Arrays/copyOfRange bs 0 (int result_offset))
                                   suffix (java.util.Arrays/copyOfRange bs (int (+ result_offset (alength (p/bytes after)))) (alength bs))]
                               (byte-array (concat prefix (p/bytes before) suffix)))) (p/bytes candidate) (reverse inverse))]
      (when-not (= (p/sha source) (p/sha restored))
        (p/refuse! :candidate-structure-mismatch [:candidate file] "Inverse did not restore exact original bytes."))
      {:candidate candidate :sites sites
       :detail (merge (form-proof root (*preservation-tree* after) (set (map :form_index sites)))
                      {:file file :references_changed (count refs) :bindings_changed 1
                       :source_hash (p/sha source) :result_hash (p/sha candidate)
                       :source_size (alength (p/bytes source)) :result_size (alength (p/bytes candidate))
                       :sites sites :inverse_splices inverse})})))

(defn count! [sources infos r]
  (let [counts (into (sorted-map) (map (fn [[f i]] [f (count (:references i))]) infos))
        assertion (get-in r [:expect :references]) expected (or (:total assertion) (:per_file assertion))
        actual (if (contains? assertion :total) (reduce + (vals counts)) counts)
        items (vec (mapcat :references (vals infos)))]
    (when (> (count items) 100000) (p/refuse! :limit-exceeded [:expect :references] "At most 100000 references."))
    (when-not (= expected actual)
      (let [e (refusal/generic-count-mismatch-evidence
                {:operation "rename_alias" :files (vec (keys sources)) :scope {:kind :root}
                 :matcher {:operation "rename_alias" :version 1 :lib (:lib r) :old_alias (:old_alias r) :new_alias (:new_alias r)}
                 :expectation assertion :expected-count expected :actual-count actual :per-file-counts counts
                 :snapshot-guards (into {} (map (fn [[f s]] [f (p/sha s)]) sources)) :items []})
            e (walk/postwalk (fn [x] (if (map? x) (into {} (map (fn [[k v]] [(if (keyword? k) (keyword (str/replace (name k) "-" "_")) k) v]) x)) x)) e)]
        (p/refuse! :expect-count-mismatch [:expect :references] (str "Expected " expected " alias references; found " actual ".")
                   (cond-> {:expected_count expected :actual_count actual :per_file_counts counts
                            :write_refusal_evidence (merge e {:family "generic-count-mismatch" :failed_stage "intent-compilation"
                                                              :authority false :write_authority false :items items
                                                              :available_count (count items) :returned_count (count items) :omitted_count 0 :truncated false})}
                     (map? actual) (assoc :mismatched_files (vec (filter #(not= (get expected %) (get actual %)) (keys counts))))))))))
(defn plan [sources request]
  (try
    (validate! request)
    (let [sources (into (sorted-map) sources) paths (vec (keys sources))]
      (paths! paths request)
      (when (> (reduce + (map #(alength (p/bytes %)) (vals sources))) 67108864)
        (p/refuse! :limit-exceeded [:source] "Aggregate source exceeds 64 MiB."))
      (guards! sources request)
      (let [roots (into (sorted-map) (map (fn [[f s]] [f (source! s f)]) sources))
            namespaces (into (sorted-map) (map (fn [[f root]] [f (namespace! root f)]) roots))
            safe-namespaces (into (sorted-map)
                                  (map (fn [[f ns]] [f (assoc ns :effective-references
                                                         (if (selected-binding (:bindings ns) request)
                                                           (references (roots f) (:owner ns) (:old_alias request) f) []))]) namespaces))
            infos (into (sorted-map) (map (fn [[f ns]] [f (binding! ns request f)]) safe-namespaces))
            selected (filterv #(get-in infos [% :selected?]) paths)]
        (when (empty? selected) (p/refuse! :old-alias-absent [:scope] "No matching library alias found."))
        (doseq [[f info] infos] (collision! info (roots f) request f))
        (count! sources infos request)
        (let [plans (into (sorted-map) (for [f selected] [f (candidate! (sources f) (roots f) (infos f) request f)]))
              per-file (mapv (fn [f]
                               (or (get-in plans [f :detail])
                                   (merge (form-proof (roots f) (roots f) #{})
                                          {:file f :references_changed 0 :bindings_changed 0
                                           :source_hash (p/sha (sources f)) :result_hash (p/sha (sources f))
                                           :source_size (alength (p/bytes (sources f))) :result_size (alength (p/bytes (sources f)))
                                           :sites [] :inverse_splices []}))) paths)
              receipt {:version 1 :operation "rename_alias" :files_inspected (count paths) :files_changed (count selected)
                       :bindings_changed (count selected) :references_changed (reduce + (map :references_changed per-file))
                       :forms_changed (reduce + (map :forms_changed per-file))
                       :other_forms_checked (reduce + (map #(get-in % [:preservation :other_forms_checked]) per-file))
                       :other_forms_unchanged (every? #(get-in % [:preservation :other_forms_unchanged]) per-file)
                       :verification_complete false :verification {:tier "parse+byte-preservation" :behavior "not-run"}
                       :concurrency "cooperative-lock+final-recheck" :next_action "none"}]
          {:ok true :candidates (into {} (map (fn [[f p]] [f (:candidate p)]) plans))
           :receipt receipt :detail {:request request :inspected paths :selected selected :skipped (vec (remove (set selected) paths))
                                     :per_file per-file :sites (vec (mapcat :sites per-file))}})))
    (catch Exception e (refuse (if (:error-type (ex-data e)) e (ex-info (.getMessage e) {:error-type :invalid-request :at []}))))))
