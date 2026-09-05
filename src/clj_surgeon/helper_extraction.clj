(ns clj-surgeon.helper-extraction
  "Pure planner for one selected-helper closure extraction.

  Input is a request that is CONSTANT in the number of callers plus the frozen
  source text of every file under the admitted discovery roots. Output is one
  complete plan or one typed refusal. Nothing here touches the filesystem, so
  the whole closure is testable from literals and the namespace loads under
  Babashka.

  Contract of record: docs/plans/helper-closure-extraction.md revision 3, the
  EARS registry in docs/intent/helper-extraction/helper-extraction-specs.md
  (prefix MCP-OP-HELPER), and the `Planner and boundary surfaces` section of
  docs/intent/helper-extraction/helper-extraction-design.md, which fixes the
  shapes the witnesses bind to.

  What this namespace does NOT do: run a verification profile, touch a
  transaction kernel, or mint a terminal receipt. The plan receipt therefore
  carries NO `:verification` key at all -- an unexecuted check must never be
  implied (MCP-OP-HELPER-022), and the boundary adds the typed map from the
  profile it actually ran. The one verification fact the planner does own is
  ADMISSIBILITY: MCP-OP-HELPER-011 requires the profile to be refused before
  anything is staged, and nothing is more `before staging` than plan time."
  (:require
   [clojure.string :as str]
   [rewrite-clj.node :as n]
   [rewrite-clj.parser :as parser]))

;; ---------------------------------------------------------------------------
;; node helpers (same shapes as clj-surgeon.alias-migration, kept local so the
;; planner has no dependency on that verb's request grammar)

(defn- meaningful?
  [node]
  (not (contains? #{:whitespace :newline :comma :comment :uneval} (n/tag node))))

(defn- children
  [node]
  (if (n/inner? node) (vec (n/children node)) []))

(defn- meaningful-children
  [node]
  (filterv meaningful? (children node)))

(defn- token-symbol
  "The symbol a token node spells, or nil."
  [node]
  (when (= :token (n/tag node))
    (let [value (try (n/sexpr node) (catch Exception _ ::unreadable))]
      (when (symbol? value) value))))

(defn- token-string
  [node]
  (when (= :token (n/tag node)) (n/string node)))

(defn- head-name
  "Name of the head token of a list node, unqualified, or nil."
  [node]
  (when (contains? #{:list :fn} (n/tag node))
    (when-let [value (some-> (first (meaningful-children node)) token-symbol)]
      (name value))))

(defn- simple-symbol-name
  [node]
  (when-let [value (token-symbol node)]
    (when (and (nil? (namespace value))
               (not (contains? #{"&" "_"} (name value))))
      (name value))))

(defn- unmeta-node
  "Inspect a metadata-wrapped value without reading or evaluating it."
  [node]
  (if (= :meta (n/tag node))
    (recur (last (meaningful-children node)))
    node))

(defn- vector-node?
  [node]
  (= :vector (n/tag node)))

(defn- top-level-forms
  [root]
  (filterv meaningful? (children root)))

;; ---------------------------------------------------------------------------
;; local binding scopes: a bare name a form introduces is not a reference

(def ^:private local-binding-vector-heads
  #{"let" "let*" "if-let" "when-let" "if-some" "when-some" "loop" "loop*"
    "with-open" "with-local-vars" "doseq" "for" "dotimes"})

(def ^:private function-heads
  #{"fn" "fn*" "defn" "defn-" "defmacro" "defmethod" "definline"})

(defn- binding-form-names
  "Names declared by a binding form, ignoring metadata, discards and defaults."
  [node]
  (let [parts (meaningful-children node)]
    (case (n/tag node)
      :meta (binding-form-names (last parts))
      :token (set (keep identity [(simple-symbol-name node)]))
      :vector (into #{} (mapcat binding-form-names) parts)
      :map (into #{}
                 (mapcat (fn [[key-node value-node]]
                           (let [key-text (n/string key-node)]
                             (cond
                               (= ":or" key-text) #{}
                               (= ":as" key-text) (binding-form-names value-node)
                               (or (contains? #{":keys" ":syms" ":strs"} key-text)
                                   (some #(str/ends-with? key-text %)
                                         ["/keys" "/syms" "/strs"]))
                               (into #{} (keep #(some-> % token-symbol name))
                                     (meaningful-children value-node))
                               :else (binding-form-names key-node)))))
                 (partition 2 parts))
      #{})))

(defn- pair-binding-names
  [vector-node]
  (into #{} (mapcat binding-form-names) (take-nth 2 (meaningful-children vector-node))))

(defn- parameter-vectors
  "The parameter vectors of a fn-shaped form, arities included."
  [node]
  (let [parts (rest (meaningful-children node))
        direct (filter #(vector-node? (unmeta-node %)) parts)
        arities (mapcat (fn [part]
                          (when (= :list (n/tag part))
                            (filter #(vector-node? (unmeta-node %))
                                    (take 1 (meaningful-children part)))))
                        parts)]
    (map unmeta-node (concat direct arities))))

(defn- function-parameter-names
  [node]
  (into #{} (mapcat binding-form-names) (parameter-vectors node)))

(defn- letfn-binding-names
  [vector-node]
  (into #{}
        (keep (fn [part]
                (when (= :list (n/tag part))
                  (some-> (first (meaningful-children part)) simple-symbol-name))))
        (meaningful-children vector-node)))

(defn- form-introduced-names
  "Names one form introduces into scope for its own children."
  [node]
  (let [head (head-name node)
        parts (meaningful-children node)]
    (cond
      (nil? head) #{}

      (= "letfn" head)
      (if-let [vector-part (first (filter vector-node? (rest parts)))]
        (letfn-binding-names vector-part)
        #{})

      (contains? local-binding-vector-heads head)
      (if-let [vector-part (first (filter vector-node? (rest parts)))]
        (pair-binding-names vector-part)
        #{})

      (contains? function-heads head)
      (function-parameter-names node)

      (contains? #{"as->" "catch"} head)
      (if-let [name-node (nth parts 2 nil)]
        (set (keep identity [(simple-symbol-name name-node)]))
        #{})

      :else #{})))

;; ---------------------------------------------------------------------------
;; ns form and libspecs

(defn- ns-node-of
  [root]
  (first (filter #(= "ns" (head-name %)) (top-level-forms root))))

(defn ns-declared-name
  "The namespace one file declares, as a string, or nil."
  [root]
  (when-let [node (ns-node-of root)]
    (some-> (nth (meaningful-children node) 1 nil) unmeta-node token-symbol name)))

(defn- clause?
  [node clause-keyword]
  (and (= :list (n/tag node))
       (= clause-keyword (some-> (first (meaningful-children node)) token-string))))

(defn- require-clause
  [ns-node]
  (when ns-node
    (first (filter #(clause? % ":require") (meaningful-children ns-node)))))

(defn- refer-symbol-name
  [node]
  (simple-symbol-name (unmeta-node node)))

(defn- libspec-facts
  "Facts for one direct libspec node, or ::indirect for a prefix list."
  [node]
  (case (n/tag node)
    :token (when-let [value (token-symbol node)]
             {:lib (name value) :aliases [] :referred #{} :refer-all? false
              :renames? false :node node})
    :vector
    (let [parts (remove #(= :uneval (n/tag %)) (meaningful-children node))
          lib (some-> (first parts) token-symbol name)
          options (rest parts)]
      (when lib
        (loop [remaining options aliases [] referred #{} refer-all? false renames? false]
          (if-let [option (first remaining)]
            (let [option-text (token-string option)
                  value (second remaining)]
              (cond
                (contains? #{":as" ":as-alias"} option-text)
                (recur (drop 2 remaining)
                       (cond-> aliases
                         (some-> value token-symbol) (conj (name (token-symbol value))))
                       referred refer-all? renames?)

                (= ":rename" option-text)
                (recur (drop 2 remaining) aliases referred refer-all? true)

                (= ":refer" option-text)
                (if (and value (vector-node? (unmeta-node value)))
                  (recur (drop 2 remaining) aliases
                         (into referred (keep refer-symbol-name)
                               (meaningful-children (unmeta-node value)))
                         refer-all? renames?)
                  (recur (drop 2 remaining) aliases referred true renames?))

                :else (recur (rest remaining) aliases referred refer-all? renames?)))
            {:lib lib :aliases aliases :referred referred :refer-all? refer-all?
             :renames? renames? :node node}))))
    :list ::indirect
    nil))

(defn- ns-libspecs
  [ns-node]
  (when-let [clause (require-clause ns-node)]
    (mapv libspec-facts (rest (meaningful-children clause)))))

(defn ns-bound-names
  "Names introduced by the ns form's :as / :as-alias / :refer options."
  [direct]
  (into #{} (concat (mapcat :aliases direct) (mapcat :referred direct))))

;; ---------------------------------------------------------------------------
;; whole-file bound names
;;
;; alias_migration deliberately restricts alias collisions to ns-level bindings,
;; because a LOCAL can never shadow a qualifier. This verb is stricter on
;; purpose: it introduces a NEW alias into a file it did not write, and the
;; contract's own witness (`the-alias-is-the-first-policy-entry-that-collides-
;; with-nothing`) requires a file whose only `response` is a `let` local to be
;; given `resp`. Readability of the rewritten caller is the reason; correctness
;; would have allowed the collision.

(defn- introduced-names
  [root]
  (into #{}
        (mapcat form-introduced-names)
        (filter n/inner? (tree-seq n/inner? children root))))

(def ^:private definition-heads
  #{"def" "defn" "defn-" "defmacro" "defmulti" "defonce" "declare"})

(defn- definition-names
  "The top-level names one form defines, with the head that defines them."
  [node]
  (let [head (head-name node)]
    (when (contains? definition-heads head)
      (let [parts (meaningful-children node)]
        (if (= "declare" head)
          (into [] (keep #(some-> % unmeta-node simple-symbol-name)) (rest parts))
          (into [] (keep identity)
                [(some-> (nth parts 1 nil) unmeta-node simple-symbol-name)]))))))

(defn- private-definition?
  [node]
  (or (contains? #{"defn-" "def-"} (head-name node))
      (let [name-node (nth (meaningful-children node) 1 nil)]
        (and name-node
             (= :meta (n/tag name-node))
             (str/includes? (n/string name-node) ":private")))))

;; ---------------------------------------------------------------------------
;; line numbers: rewrite-clj round-trips exactly, so the offset of each
;; top-level node in the emitted text IS its position in the file

(defn- top-level-lines
  "[{:node n :line l}] for the meaningful top-level nodes of `root`."
  [root]
  (loop [nodes (children root) line 1 acc []]
    (if-let [node (first nodes)]
      (let [text (n/string node)
            newlines (count (filter #(= \newline %) text))]
        (recur (rest nodes)
               (+ line newlines)
               (cond-> acc (meaningful? node) (conj {:node node :line line}))))
      acc)))

;; ---------------------------------------------------------------------------
;; the rewriting walk
;;
;; One walk answers three questions at once: how many references to a SELECTED
;; owner this form carries, how many to a RETAINED one, and what the form reads
;; after the selected ones are re-qualified. Reader discards, comments, strings
;; and every other trivia node are carried through untouched, which is what the
;; fixture's protected regions exist to prove.

(def ^:private empty-walk
  {:nodes [] :selected 0 :retained 0})

(defn- leaf
  [node]
  {:node node :selected 0 :retained 0})

(defn- accumulate
  [state result]
  (-> state
      (update :nodes conj (:node result))
      (update :selected + (:selected result))
      (update :retained + (:retained result))))

(defn- respelled
  "The new spelling of a selected owner's symbol.

  A nil `replacement-qualifier` means the reference now sits INSIDE the
  destination namespace, where the owner's own bare name is the whole answer:
  that is how a moved -> moved peer reference, and a multi-arity helper that
  delegates to another of its own arities by name, keep reading correctly."
  [replacement-qualifier var-name]
  (if (str/blank? (str replacement-qualifier))
    var-name
    (str replacement-qualifier "/" var-name)))

(defn- token-decision
  "Classify one token against the extraction, by whole symbol identity."
  [node {:keys [qualifiers selected retained replacement-qualifier]} live-bare]
  (when-let [value (token-symbol node)]
    (let [qualifier (namespace value)
          var-name (name value)]
      (cond
        qualifier
        (when (contains? qualifiers qualifier)
          (cond
            (contains? selected var-name)
            {:rewrite (respelled replacement-qualifier var-name) :selected 1}
            (contains? retained var-name) {:retained 1}
            :else nil))

        (contains? live-bare var-name)
        (cond
          (contains? selected var-name)
          {:rewrite (respelled replacement-qualifier var-name) :selected 1}
          (contains? retained var-name) {:retained 1}
          :else nil)

        :else nil))))

(defn- rewrite-node
  "Rewrite one node and tally its selected and retained references."
  [node context live-bare]
  (let [tag (n/tag node)]
    (cond
      ;; a reader discard is data the contract keeps exactly as it is
      (= :uneval tag) (leaf node)

      (= :token tag)
      (let [{:keys [rewrite selected retained]} (token-decision node context live-bare)]
        (cond-> (leaf node)
          rewrite (assoc :node (parser/parse-string rewrite))
          selected (assoc :selected selected)
          retained (assoc :retained retained)))

      (n/inner? node)
      (let [body-bare (reduce disj live-bare (form-introduced-names node))
            walked (reduce (fn [state child]
                             (accumulate state (rewrite-node child context body-bare)))
                           empty-walk
                           (children node))]
        {:node (n/replace-children node (:nodes walked))
         :selected (:selected walked)
         :retained (:retained walked)})

      :else (leaf node))))

;; ---------------------------------------------------------------------------
;; namespace-sensitive forms inside a moved body (MCP-OP-HELPER-018)

(defn- namespace-sensitive-form
  "The first namespace-sensitive form in `node`, as its literal text, or nil.

  `::kw` and `::alias/kw` are resolved through the DEFINING namespace's alias
  map, a syntax quote resolves every bare symbol against it, and `*ns*` reads
  it at runtime. All three change meaning when the body moves, and all three
  still compile, so v1 refuses rather than rewriting."
  [node]
  (first
   (keep (fn [child]
           (let [tag (n/tag child)]
             (cond
               (= :syntax-quote tag) (n/string child)
               (= :token tag)
               (let [text (token-string child)]
                 (cond
                   (and text (str/starts-with? text "::")) text
                   (= "*ns*" text) text
                   :else nil))
               :else nil)))
         (tree-seq #(and (n/inner? %) (not= :uneval (n/tag %))) children node))))

;; ---------------------------------------------------------------------------
;; scope globs

(defn- glob-pattern
  "A `scope.paths` glob as a regex: `**` spans separators, `*` does not."
  [glob]
  (let [escaped (str/replace glob #"[.+^$(){}\[\]|\\]" "\\\\$0")
        expanded (str/replace escaped #"\*\*|\*"
                              (fn [match] (if (= "**" match) ".*" "[^/]*")))]
    (re-pattern (str "^" expanded "$"))))

(defn- in-scope?
  [paths path]
  (boolean (some #(re-matches (glob-pattern %) path) paths)))

;; ---------------------------------------------------------------------------
;; node construction

(defn- alias-libspec
  [lib alias]
  (n/vector-node [(n/token-node (symbol lib)) (n/spaces 1)
                  (n/keyword-node :as) (n/spaces 1)
                  (n/token-node (symbol alias))]))

(defn- plain-libspec
  [lib]
  (n/token-node (symbol lib)))

(defn- leading-whitespace
  "The trivia run immediately before `anchor` among `kids`."
  [kids index]
  (loop [i (dec index) acc ()]
    (if (and (>= i 0) (not (meaningful? (nth kids i))))
      (recur (dec i) (cons (nth kids i) acc))
      (vec acc))))

(defn- node-index
  [kids anchor]
  (first (keep-indexed #(when (identical? %2 anchor) %1) kids)))

(defn- insert-after
  "Insert `new-nodes` after `anchor`, repeating the trivia that precedes it, so
  the file's own indentation is reproduced rather than invented."
  [parent anchor new-nodes]
  (let [kids (vec (children parent))
        index (node-index kids anchor)
        trivia (leading-whitespace kids index)]
    (n/replace-children
     parent
     (vec (concat (subvec kids 0 (inc index))
                  (mapcat (fn [node] (concat trivia [node])) new-nodes)
                  (subvec kids (inc index)))))))

(defn- replace-child
  [parent old-node new-node]
  (n/replace-children
   parent
   (mapv #(if (identical? % old-node) new-node %) (children parent))))

(defn- require-clause-node
  [libspec-nodes]
  (n/list-node
   (into [(n/keyword-node :require)]
         (mapcat (fn [node] [(n/newlines 1) (n/spaces 3) node]) libspec-nodes))))

(defn- ns-node-with-new-require-clause
  [ns-node libspec-nodes]
  (n/replace-children
   ns-node
   (concat (children ns-node)
           [(n/newlines 1) (n/spaces 2) (require-clause-node libspec-nodes)])))

(defn- rendered-ns-node
  [lib libspec-nodes]
  (if (seq libspec-nodes)
    (n/list-node [(n/token-node 'ns) (n/spaces 1) (n/token-node (symbol lib))
                  (n/newlines 1) (n/spaces 2) (require-clause-node libspec-nodes)])
    (n/list-node [(n/token-node 'ns) (n/spaces 1) (n/token-node (symbol lib))])))

;; ---------------------------------------------------------------------------
;; refusals (MCP-OP-HELPER-010, MCP-OP-HELPER-016)
;;
;; Every v1 refusal carries `next_call nil`, bounded evidence, and the ONE
;; unresolved decision. None offers a continuation, and none is allowed to
;; propose a smaller problem: no caller left out of the footprint, no reduced
;; scope, no invented alias or destination, no lesser verification profile.

(def ^:private refusal-suffixes
  ["ambiguous-owner"
   "private-dependency"
   "retained-dependency"
   "namespace-sensitive-body"
   "unsupported-binding"
   "ambiguous-reference"
   "caller-outside-scope"
   "alias-policy-exhausted"
   "expect-mismatch"
   "target-exists"
   "unknown-field"
   "verification-preflight-unavailable"])

;; @spec MCP-OP-HELPER-010
(defn refusal-types
  "The closed set of v1 `error_type` strings this planner can emit.

  `helper-extraction-verification-preflight-unavailable` is raised here, at
  plan time, precisely because MCP-OP-HELPER-011 requires the profile to be
  validated BEFORE anything is staged; the boundary re-validates capability
  against the live runner."
  []
  (mapv #(str "helper-extraction-" %) refusal-suffixes))

(defn- refusal
  [suffix message decision evidence]
  (merge {:ok false
          :error_type (str "helper-extraction-" suffix)
          :error message
          :decision decision
          :next_call nil
          :source_unchanged true
          :target_unchanged true}
         evidence))

;; ---------------------------------------------------------------------------
;; request validation

(def ^:private request-fields
  #{:op :workspace_root :from :helpers :to :scope :verification :expect})

(def admitted-roots
  "The discovery roots v1 admits. `scope.paths` is a WRITE-AUTHORIZATION subset
  of these, never a substitute for them (revision 3, rule 4)."
  ["src" "test"])

(def admitted-profile-names
  "Verification profiles v1 admits: synchronous and rollback-capable."
  #{"helper-proof"})

(defn- sha256
  [text]
  (let [digest (java.security.MessageDigest/getInstance "SHA-256")]
    (apply str (map #(format "%02x" (bit-and 0xff %))
                    (.digest digest (.getBytes ^String text "UTF-8"))))))

;; ---------------------------------------------------------------------------
;; step 1 -- owners (MCP-OP-HELPER-003)

(defn- source-definitions
  "Every top-level definition of the source, in definition order.

  `:order` is that position. It is what puts the moved forms into the
  destination in the order the SOURCE defined them rather than the order the
  request happened to name them: `helpers` is a set of decisions, not a layout."
  [root]
  (vec
   (map-indexed
    (fn [order definition] (assoc definition :order order))
    (mapcat (fn [{:keys [node line]}]
             (map (fn [definition-name]
                    {:name definition-name
                     :kind (head-name node)
                     :line line
                     :node node
                     :private? (private-definition? node)})
                   (definition-names node)))
            (top-level-lines root)))))

(defn- resolve-owners
  "One owner per selected helper, or the ambiguous-owner refusal."
  [helpers definitions]
  (reduce
   (fn [acc helper-name]
     (let [found (filterv #(= helper-name (:name %)) definitions)]
       (if (= 1 (count found))
         (conj acc (first found))
         (reduced
          (refusal "ambiguous-owner"
                   (str "The selected helper " helper-name " does not resolve to"
                        " exactly one top-level owner in the source.")
                   (str "which single top-level form owns " helper-name)
                   {:helper helper-name
                    :owners (mapv #(select-keys % [:kind :line]) found)})))))
   []
   helpers))

(defn- in-definition-order
  "Moved forms reach the destination in the order the source defined them."
  [owners]
  (vec (sort-by :order owners)))

;; ---------------------------------------------------------------------------
;; step 2 -- moved bodies (MCP-OP-HELPER-004, -018, -019)

(defn- body-dependency-refusal
  "The first dependency or namespace sensitivity that stops a moved body."
  [source-lib owners definitions]
  (let [selected (set (map :name owners))
        retained (into {} (comp (remove #(contains? selected (:name %)))
                                (map (juxt :name identity)))
                       definitions)]
    (first
     (keep
      (fn [{:keys [name node]}]
        (or
         ;; @spec MCP-OP-HELPER-018
         (when-let [form (namespace-sensitive-form node)]
           (refusal "namespace-sensitive-body"
                    (str "The body of " name " contains a namespace-sensitive"
                         " form, which reads differently from the destination.")
                    (str "how " form " should read once " name " lives in the"
                         " destination namespace")
                    {:helper name :form form}))
         ;; @spec MCP-OP-HELPER-004
         ;; @spec MCP-OP-HELPER-019
         (let [live (reduce disj (set (keys retained)) (form-introduced-names node))
               hit (first
                    (keep (fn [child]
                            (when-let [value (token-symbol child)]
                              (let [qualifier (namespace value)
                                    var-name (clojure.core/name value)]
                                (when (and (or (nil? qualifier) (= source-lib qualifier))
                                           (contains? retained var-name)
                                           (or qualifier (contains? live var-name)))
                                  (get retained var-name)))))
                          (tree-seq #(and (n/inner? %) (not= :uneval (n/tag %)))
                                    children node)))]
           (when hit
             (if (:private? hit)
               (refusal "private-dependency"
                        (str "The selected helper " name " references a private"
                             " var the source retains.")
                        (str "whether " (:name hit) " belongs in the selection")
                        {:helper name :var (str source-lib "/" (:name hit))})
               (refusal "retained-dependency"
                        (str "The selected helper " name " references a public"
                             " var the source retains, so the destination would"
                             " have to require the source.")
                        (str "whether " (:name hit) " belongs in the selection")
                        {:helper name :var (str source-lib "/" (:name hit))}))))))
      owners))))

;; ---------------------------------------------------------------------------
;; step 3 -- reference discovery per file (MCP-OP-HELPER-005, -014, -023)

(defn- prefix-list-refusal
  [file ns-node source-lib]
  (when-let [clause (require-clause ns-node)]
    (let [head-segment (first (str/split source-lib #"\."))
          prefix (first (filter #(and (= :list (n/tag %))
                                      (str/includes? (n/string %) head-segment))
                                (rest (meaningful-children clause))))]
      (when prefix
        (refusal "unsupported-binding"
                 (str file " binds the source through a prefix list, grammar"
                      " v1 does not close over.")
                 (str "how " file " should bind the destination namespace")
                 {:file file :form (n/string prefix)})))))

(defn- use-clause-refusal
  [file ns-node source-lib]
  (when-let [clause (first (filter #(clause? % ":use") (meaningful-children ns-node)))]
    (when (str/includes? (n/string clause) source-lib)
      (refusal "unsupported-binding"
               (str file " reaches the source through a :use clause; the"
                    " referred set is not mechanically knowable.")
               (str "how " file " should bind the destination namespace")
               {:file file :form (n/string clause)}))))

(defn- target-grammar-refusal
  [file target]
  (cond
    (:refer-all? target)
    (refusal "unsupported-binding"
             (str file " requires the source with :refer :all; the referred set"
                  " is not mechanically knowable.")
             (str "how " file " should bind the destination namespace")
             {:file file :form (n/string (:node target))})

    (:renames? target)
    (refusal "unsupported-binding"
             (str file " renames referred names of the source; v1 does not model"
                  " a renamed binding.")
             (str "how " file " should bind the destination namespace")
             {:file file :form (n/string (:node target))})))

(defn- ambiguous-reference-refusal
  "A bare selected name reachable through two required namespaces."
  [file source-lib selected target others]
  (when target
    (first
     (keep (fn [helper-name]
             (when (or (contains? (:referred target) helper-name) (:refer-all? target))
               (when-let [competing (first (filter #(or (contains? (:referred %) helper-name)
                                                        (:refer-all? %))
                                                   others))]
                 (refusal "ambiguous-reference"
                          (str "A bare " helper-name " in " file " could resolve"
                               " to two required namespaces.")
                          (str "which namespace " helper-name " names in " file)
                          {:file file
                           :symbol helper-name
                           :candidates [(str source-lib "/" helper-name)
                                        (str (:lib competing) "/" helper-name)]}))))
           (sort selected)))))

(defn- reader-conditional-refusal
  [file root source-lib]
  (when (some (fn [node]
                (and (= :reader-macro (n/tag node))
                     (str/includes? (n/string node) source-lib)))
              (tree-seq n/inner? children root))
    (refusal "unsupported-binding"
             (str file " reaches the source inside a reader conditional; v1 does"
                  " not model per-platform binding.")
             (str "how " file " should bind the destination namespace")
             {:file file :form source-lib})))

(defn- analyze-caller
  "Discovery for ONE file that is not the source.

  Returns nil when the file neither binds nor mentions the source, a refusal
  map, or `{:file :partition :sites :retained :bound :root :ns-node :target
  :direct :forms}` -- everything the edit builder needs and nothing it does not."
  [{:keys [file source]} {:keys [source-lib selected retained] :as context}]
  (let [root (parser/parse-string-all source)
        ns-node (ns-node-of root)
        direct (filterv map? (ns-libspecs ns-node))
        target (first (filter #(= source-lib (:lib %)) direct))
        others (remove #(= source-lib (:lib %)) direct)
        qualifiers (into #{source-lib} (:aliases target))
        forms (remove #(identical? % ns-node) (top-level-forms root))
        walk-context {:qualifiers qualifiers
                      :selected selected
                      :retained retained
                      ;; counting-only walk: the real qualifier is chosen once the
                      ;; partition is known, so this spelling is never emitted
                      :replacement-qualifier "q"}
        walked (mapv #(rewrite-node % walk-context (:referred target #{})) forms)
        sites (reduce + 0 (map :selected walked))
        retained-sites (reduce + 0 (map :retained walked))]
    (or (when ns-node (prefix-list-refusal file ns-node source-lib))
        (when ns-node (use-clause-refusal file ns-node source-lib))
        (when target (target-grammar-refusal file target))
        (reader-conditional-refusal file root source-lib)
        (when (pos? sites)
          (ambiguous-reference-refusal file source-lib selected target others))
        (cond
          (and (zero? sites) (nil? target)) nil

          (zero? sites)
          {:file file :partition "untouched" :sites 0 :retained retained-sites}

          :else
          {:file file
           :partition (cond
                        (nil? target) "qualified_only"
                        (pos? retained-sites) "mixed"
                        :else "moved_only")
           :sites sites
           :retained retained-sites
           :bound (into (ns-bound-names direct)
                        (concat (introduced-names root)
                                (mapcat definition-names forms)))
           :root root
           :ns-node ns-node
           :target target
           :direct direct
           :forms forms
           :context (assoc context :qualifiers qualifiers)}))))

;; ---------------------------------------------------------------------------
;; step 5 -- alias choice (MCP-OP-HELPER-007)

(defn- choose-alias
  [bound policy]
  {:alias (first (drop-while #(contains? bound %) policy))
   :collided (vec (take-while #(contains? bound %) policy))})

;; ---------------------------------------------------------------------------
;; edit construction

(defn- caller-ns-edit
  "The ns-form edit for one rewritten caller.

  moved_only replaces the source libspec, because every use it carried moves.
  mixed KEEPS the source libspec and gains exactly one more. qualified_only had
  no require at all and gains a plain one, so the rewritten qualified symbol has
  a load path of its own (MCP-OP-HELPER-014)."
  [{:keys [partition ns-node target]} dest-lib alias]
  (let [libspec (if (= "qualified_only" partition)
                  (plain-libspec dest-lib)
                  (alias-libspec dest-lib alias))
        clause (require-clause ns-node)
        new-ns (cond
                 (nil? clause)
                 (ns-node-with-new-require-clause ns-node [libspec])

                 (= "moved_only" partition)
                 (replace-child ns-node clause (replace-child clause (:node target) libspec))

                 (some? target)
                 (replace-child ns-node clause (insert-after clause (:node target) [libspec]))

                 :else
                 (replace-child ns-node clause
                                (insert-after clause
                                              (last (filter #(some? (libspec-facts %))
                                                            (rest (meaningful-children clause))))
                                              [libspec])))]
    {:original (n/string ns-node)
     :replacement (n/string new-ns)}))

(defn- form-edits
  "One whole-form edit per top-level form that carries a selected reference."
  [forms context live-bare]
  (into []
        (keep (fn [form]
                (let [{:keys [node selected]} (rewrite-node form context live-bare)]
                  (when (pos? selected)
                    {:original (n/string form)
                     :replacement (n/string node)}))))
        forms))

(defn- caller-plan
  [analysis dest-lib alias]
  (let [context (assoc (:context analysis)
                       :replacement-qualifier (if (= "qualified_only" (:partition analysis))
                                                dest-lib
                                                alias))
        live-bare (get-in analysis [:target :referred] #{})]
    {:file (:file analysis)
     :partition (:partition analysis)
     :alias (when-not (= "qualified_only" (:partition analysis)) alias)
     :sites (:sites analysis)
     :retained (:retained analysis)
     :edits (into [(caller-ns-edit analysis dest-lib alias)]
                  (form-edits (:forms analysis) context live-bare))}))

;; ---------------------------------------------------------------------------
;; the source file: retirement, one new require, source-local lowering
;; (MCP-OP-HELPER-015). The source is BOTH the mutation subject and a caller,
;; and it is counted ONCE.

(defn- deletion-edit
  "Delete one moved form and the trivia that separated it from its neighbour,
  so the retained forms keep exactly the spacing they had."
  [root node]
  (let [kids (vec (children root))
        index (node-index kids node)
        following (subvec kids (inc index))
        trailing (vec (take-while (complement meaningful?) following))]
    (if (seq trailing)
      {:original (str (n/string node) (apply str (map n/string trailing)))
       :replacement ""}
      (let [leading (leading-whitespace kids index)]
        {:original (str (apply str (map n/string leading)) (n/string node))
         :replacement ""}))))

(defn- source-plan
  [{:keys [file root ns-node source-lib owners definitions dest-lib alias policy]}]
  (let [moved-nodes (set (map :node owners))
        selected (set (map :name owners))
        retained-forms (remove #(or (identical? % ns-node) (contains? moved-nodes %))
                               (top-level-forms root))
        context {:qualifiers #{source-lib}
                 :selected selected
                 :retained #{}
                 :replacement-qualifier alias}
        ;; every top-level name of the source is in scope as a bare symbol
        ;; inside the source itself
        live-bare (into #{} (map :name) definitions)
        edits (form-edits retained-forms context live-bare)
        sites (reduce + 0 (map (fn [form] (:selected (rewrite-node form context live-bare)))
                               retained-forms))
        clause (require-clause ns-node)
        libspec (alias-libspec dest-lib alias)
        anchor (when clause
                 (last (filter #(map? (libspec-facts %))
                               (rest (meaningful-children clause)))))
        new-ns (cond
                 (nil? clause) (ns-node-with-new-require-clause ns-node [libspec])
                 (nil? anchor) (replace-child ns-node clause
                                              (n/replace-children
                                               clause
                                               (concat (children clause)
                                                       [(n/newlines 1) (n/spaces 3) libspec])))
                 :else (replace-child ns-node clause (insert-after clause anchor [libspec])))]
    {:file file
     :partition "source"
     :alias alias
     :sites sites
     :retained 0
     :policy policy
     :edits (into [{:original (n/string ns-node) :replacement (n/string new-ns)}]
                  (concat (mapv #(deletion-edit root (:node %)) owners)
                          edits))}))

;; ---------------------------------------------------------------------------
;; the destination namespace

(defn- destination-source
  "The destination's complete text: `require_policy minimal` over the source's
  own libspecs, then the moved forms verbatim in definition order.

  A moved -> moved reference is already the destination's own bare symbol, and a
  fully qualified self-reference is lowered to one. No moved -> retained edge
  can reach here: MCP-OP-HELPER-019 refused before the plan was built, which is
  why this namespace requires nothing of the source and no cycle can pass
  through it."
  [{:keys [dest-lib source-lib source-ns-node owners]}]
  (let [selected (set (map :name owners))
        context {:qualifiers #{source-lib}
                 :selected selected
                 :retained #{}
                 :replacement-qualifier nil}
        rewritten (mapv (fn [{:keys [node]}]
                          (n/string (:node (rewrite-node node context #{}))))
                        owners)
        body (str/join "\n\n" (map str/trim-newline rewritten))
        moved-text (str/join "\n" rewritten)
        direct (filterv map? (ns-libspecs source-ns-node))
        used (filterv (fn [{:keys [aliases referred]}]
                        (some (fn [needle] (str/includes? moved-text needle))
                              (concat (map #(str % "/") aliases) referred)))
                      direct)
        ns-text (n/string (rendered-ns-node dest-lib (mapv :node used)))]
    (str ns-text "\n\n" body "\n")))

;; ---------------------------------------------------------------------------
;; the receipt (MCP-OP-HELPER-009, MCP-OP-HELPER-012)
;;
;; Counts and histograms only. Nothing here is a file list, and nothing here is
;; a verification result: the planner has run no proof, so its typed checks are
;; zero and `ok` is nil until the boundary executes the profile.

(defn- receipt
  [{:keys [helpers files untouched scope-paths details-path]}]
  (let [by-partition (frequencies (map :partition files))
        caller-files (filter #(contains? #{"moved_only" "mixed" "qualified_only"}
                                         (:partition %))
                             files)]
    {:operation "helper_extraction"
     :helpers (count helpers)
     :source_retired (count helpers)
     :destination_created true
     :caller_files (count files)
     :partition {:moved_only (get by-partition "moved_only" 0)
                 :mixed (get by-partition "mixed" 0)
                 :qualified_only (get by-partition "qualified_only" 0)
                 :untouched (count untouched)}
     :sites (reduce + 0 (map :sites files))
     ;; every discovered bystander counts here even though none is in the
     ;; footprint: `retained_sites` is what this write leaves alone, and a
     ;; caller that uses only what the source keeps is exactly that
     :retained_sites (reduce + 0 (map :retained (concat files untouched)))
     :alias_histogram (frequencies (keep :alias caller-files))
     ;; NO :verification key. The planner runs no proof, and a typed
     ;; verification map with zeroes in it is exactly the ambiguous coverage
     ;; claim MCP-OP-HELPER-022 exists to forbid: an unexecuted check must never
     ;; be implied. The boundary adds it from the profile it actually ran.
     :closure {:roots admitted-roots
               :authorized_paths (vec scope-paths)
               :grammar "supported-libspecs-only"
               :dynamic_references "not-claimed"}
     :details_path details-path}))

;; ---------------------------------------------------------------------------
;; the transaction
;;
;; ONE guarded transaction carrying ONE typed `extraction` change. Source
;; retirement, destination creation and the source-local lowering all belong to
;; that change: `compile-extraction` refuses `caller_changes` that target the
;; extraction's own file or its destination, so the source may never appear as a
;; caller change, and the fixture's witness that the source appears exactly once
;; is the same fact seen from the plan side.

(defn- transaction
  [{:keys [source-file dest-file owners source-hash caller-files untouched-files]}]
  [{:changes
    [{:kind "extraction"
      :file source-file
      :to dest-file
      :forms (mapv :name owners)
      :require_policy "minimal"
      :source_hash source-hash
      :caller_changes
      (into []
            (mapcat (fn [{:keys [file edits]}]
                      (map-indexed
                       (fn [index {:keys [original replacement]}]
                         {:id (str file "#" index)
                          :in [file]
                          :find original
                          :do [:replace replacement]
                          :expect {:matches 1}})
                       edits)))
            caller-files)
      :ignored_caller_files (vec (sort untouched-files))}]}])

;; ---------------------------------------------------------------------------
;; the planner

(defn- destination-file
  "The destination path, derived from the source's own root rather than guessed."
  [from-file source-lib dest-lib]
  (let [lib-path (fn [lib] (str (str/replace (str/replace lib "-" "_") "." "/") ".clj"))
        source-path (lib-path source-lib)
        root (if (str/ends-with? from-file source-path)
               (subs from-file 0 (- (count from-file) (count source-path)))
               "")]
    (str root (lib-path dest-lib))))

;; @spec MCP-OP-HELPER-002
;; @spec MCP-OP-HELPER-003
;; @spec MCP-OP-HELPER-005
;; @spec MCP-OP-HELPER-006
;; @spec MCP-OP-HELPER-007
;; @spec MCP-OP-HELPER-008
;; @spec MCP-OP-HELPER-009
;; @spec MCP-OP-HELPER-010
;; @spec MCP-OP-HELPER-011
;; @spec MCP-OP-HELPER-012
;; @spec MCP-OP-HELPER-013
;; @spec MCP-OP-HELPER-014
;; @spec MCP-OP-HELPER-015
;; @spec MCP-OP-HELPER-016
;; @spec MCP-OP-HELPER-017
;; @spec MCP-OP-HELPER-021
;; @spec MCP-OP-HELPER-024
;; @spec MCP-OP-HELPER-025
(defn plan
  "One complete helper-extraction plan, or one typed refusal.

  request: {:op s :workspace_root s
            :from {:file relative-path}
            :helpers [s]
            :to {:lib s :alias_policy [s]}
            :scope {:paths [glob]}
            :verification {:profile s}
            :expect {:caller_files n}?}      ; OPTIONAL, strict when supplied
  sources: ordered vector of {:file relative-path :source text} covering every
           file under the admitted roots BEFORE the write.

  The request is constant in the number of callers: no per-file, per-owner or
  per-site table, and no hashes."
  [request sources]
  (let [unknown (remove request-fields (keys request))
        helpers (vec (:helpers request))
        dest-lib (get-in request [:to :lib])
        policy (vec (get-in request [:to :alias_policy]))
        scope-paths (vec (get-in request [:scope :paths]))
        profile (get-in request [:verification :profile])
        from-file (get-in request [:from :file])
        source-entry (first (filter #(= from-file (:file %)) sources))]
    (cond
      ;; @spec MCP-OP-HELPER-002
      ;; @spec MCP-OP-HELPER-025
      (seq unknown)
      (refusal "unknown-field"
               "The request carries a field outside the closed set."
               "which of the closed request fields carries this information"
               {:unknown_fields (mapv name unknown)})

      ;; @spec MCP-OP-HELPER-011
      ;; validated BEFORE anything is planned, so nothing is ever staged
      (not (contains? admitted-profile-names profile))
      (refusal "verification-preflight-unavailable"
               (str "The verification profile " (pr-str profile) " is not an"
                    " admitted synchronous, rollback-capable profile.")
               "which admitted profile proves this write"
               {:profile profile})

      (nil? source-entry)
      (refusal "ambiguous-owner"
               (str "The source file " (pr-str from-file) " is not among the"
                    " sources handed to the planner.")
               "which file defines the selected helpers"
               {:helper (first helpers) :owners []})

      :else
      (let [source-root (parser/parse-string-all (:source source-entry))
            source-lib (ns-declared-name source-root)
            source-ns-node (ns-node-of source-root)
            definitions (source-definitions source-root)
            resolved (resolve-owners helpers definitions)
            owners (if (map? resolved) resolved (in-definition-order resolved))]
        (if (map? owners)
          owners
          (let [dest-file (destination-file from-file source-lib dest-lib)
                selected (set (map :name owners))
                retained (into #{} (comp (remove #(contains? selected (:name %)))
                                         (map :name))
                               definitions)]
            (or
             ;; @spec MCP-OP-HELPER-004 MCP-OP-HELPER-018 MCP-OP-HELPER-019
             (body-dependency-refusal source-lib owners definitions)

             ;; @spec MCP-OP-HELPER-024
             (let [occupied (first (filter (fn [{:keys [file source]}]
                                             (or (= dest-file file)
                                                 (= dest-lib (ns-declared-name
                                                              (parser/parse-string-all source)))))
                                           sources))]
               (when occupied
                 (refusal "target-exists"
                          (str "The destination " dest-lib " is already defined"
                               " or its path is occupied.")
                          "which namespace the selected helpers move to"
                          {:lib dest-lib :file (:file occupied)})))

             ;; @spec MCP-OP-HELPER-005 MCP-OP-HELPER-014 MCP-OP-HELPER-023
             (let [context {:source-lib source-lib :selected selected :retained retained}
                   analyses (reduce
                             (fn [acc entry]
                               (if (:refusal acc)
                                 acc
                                 (let [result (analyze-caller entry context)]
                                   (cond
                                     (nil? result) acc
                                     (false? (:ok result)) (assoc acc :refusal result)
                                     :else (update acc :files conj result)))))
                             {:files []}
                             (remove #(= from-file (:file %)) sources))]
               (or
                (:refusal analyses)

                ;; @spec MCP-OP-HELPER-021
                (let [outside (into []
                                    (comp (filter #(pos? (:sites %)))
                                          (remove #(in-scope? scope-paths (:file %)))
                                          (map :file))
                                    (:files analyses))]
                  (when (seq outside)
                    (refusal "caller-outside-scope"
                             (str "A supported reference to a selected helper sits"
                                  " under an admitted discovery root that"
                                  " scope.paths does not authorize for writing.")
                             "whether scope.paths should cover this root"
                             {:files_outside_scope (vec (sort outside))
                              :admitted_roots admitted-roots})))

                ;; @spec MCP-OP-HELPER-007
                (let [rewritten (filterv #(pos? (:sites %)) (:files analyses))
                      untouched (filterv #(zero? (:sites %)) (:files analyses))
                      source-bound (let [moved (set (map :node owners))
                                         retained-forms
                                         (remove #(or (identical? % source-ns-node)
                                                      (contains? moved %))
                                                 (top-level-forms source-root))]
                                     (into (ns-bound-names
                                            (filterv map? (ns-libspecs source-ns-node)))
                                           (concat (mapcat introduced-names retained-forms)
                                                   (mapcat definition-names retained-forms))))
                      choices (reduce
                               (fn [acc analysis]
                                 (if (:refusal acc)
                                   acc
                                   (if (= "qualified_only" (:partition analysis))
                                     (update acc :plans conj (caller-plan analysis dest-lib nil))
                                     (let [{:keys [alias collided]}
                                           (choose-alias (:bound analysis) policy)]
                                       (if alias
                                         (update acc :plans conj
                                                 (caller-plan analysis dest-lib alias))
                                         (assoc acc :refusal
                                                (refusal "alias-policy-exhausted"
                                                         (str "Every alias_policy entry is"
                                                              " already bound in "
                                                              (:file analysis) ".")
                                                         (str "which alias " (:file analysis)
                                                              " should use for the destination")
                                                         {:file (:file analysis)
                                                          :collided_bindings collided})))))))
                               {:plans []}
                               rewritten)]
                  (or
                   (:refusal choices)
                   (let [source-alias (:alias (choose-alias source-bound policy))]
                     (if-not source-alias
                       (refusal "alias-policy-exhausted"
                                (str "Every alias_policy entry is already bound in "
                                     from-file ".")
                                (str "which alias " from-file
                                     " should use for the destination")
                                {:file from-file
                                 :collided_bindings (:collided (choose-alias source-bound policy))})
                       (let [source-file-plan
                             (source-plan {:file from-file
                                           :root source-root
                                           :ns-node source-ns-node
                                           :source-lib source-lib
                                           :owners owners
                                           :definitions definitions
                                           :dest-lib dest-lib
                                           :alias source-alias
                                           :policy policy})
                             files (conj (:plans choices) source-file-plan)
                             derived (count files)
                             expected (get-in request [:expect :caller_files])]
                         (cond
                           ;; @spec MCP-OP-HELPER-013 MCP-OP-HELPER-017
                           (and (some? expected) (not= expected derived))
                           (refusal "expect-mismatch"
                                    (str "expect.caller_files does not equal the"
                                         " derived footprint.")
                                    "which caller-file count is correct"
                                    {:derived_caller_files derived
                                     :expected_caller_files expected})

                           :else
                           (let [source-hash (sha256 (:source source-entry))
                                 details-path (str ".clj-surgeon/helper-extraction/"
                                                   (subs source-hash 0 16) ".edn")]
                             {:ok true
                              :plan
                              {:destination
                               {:lib dest-lib
                                ;; project-relative, derived by stripping the
                                ;; SOURCE lib's own path off from.file: no walk
                                ;; up the filesystem looking for a directory
                                ;; named `src`, so an ancestor named `src`
                                ;; cannot leak into the destination
                                :file dest-file
                                :source (destination-source
                                         {:dest-lib dest-lib
                                          :source-lib source-lib
                                          :source-ns-node source-ns-node
                                          :owners owners})}
                               :moved (mapv :name owners)
                               :files files
                               :transactions
                               (transaction {:source-file from-file
                                             :dest-file dest-file
                                             :owners owners
                                             :source-hash source-hash
                                             :caller-files (:plans choices)
                                             :untouched-files (map :file untouched)})}
                              :receipt (receipt {:helpers helpers
                                                 :files files
                                                 :untouched untouched
                                                 :scope-paths scope-paths
                                                 :details-path details-path})}))))))))))))))))
