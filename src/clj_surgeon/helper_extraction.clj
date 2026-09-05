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
  profile it actually ran, and `verification.profile` reaches this namespace as
  an opaque string it never judges."
  (:require
   [clj-surgeon.alias-migration :as alias-migration]
   [clj-surgeon.extract-header :as extract-header]
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
;; NOTE ON BINDING ANALYSIS. There is deliberately none in this namespace.
;;
;; Whether a bare symbol is still a reference at a point in a tree -- past a
;; `let` that shadows it PAIRWISE, past one arity's parameters but not
;; another's, past a `:or` default, inside a reader discard or an unselected
;; reader-conditional branch -- is answered by
;; `clj-surgeon.alias-migration/rewrite-forms`, the repository's one lexical
;; walker, through its `:decide` seam. A second copy of that analysis here
;; would be a WEAKER copy, and the shapes it got wrong would fail SILENTLY, as
;; an unrewritten call rather than an error. Where that walker reports a shape
;; it does not model, this namespace turns the report into a typed refusal
;; instead of dropping it.

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
;; source definitions

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
;; the rewriting walk: one question asked through the shared lexical walker

(defn- respelled
  "The new spelling of a selected owner's symbol.

  A blank `replacement-qualifier` means the reference now sits INSIDE the
  destination namespace, where the owner's own bare name is the whole answer:
  that is how a moved -> moved peer reference, and a multi-arity helper that
  delegates to another of its own arities by name, keep reading correctly."
  [replacement-qualifier var-name]
  (if (str/blank? (str replacement-qualifier))
    var-name
    (str replacement-qualifier "/" var-name)))

(defn- decide-token
  "The `:decide` seam for `alias-migration/rewrite-forms`.

  It answers ONE question -- does this token name a var in `targets`, reached
  through a qualifier this file binds or as a bare name still in scope -- and
  answers nothing about scope itself, which is the walker's job. A token that
  is not a symbol (a keyword, an auto-resolved `::alias/kw`, a string) never
  names a var and is never a site."
  [node {:keys [qualifiers targets replacement-qualifier]} live-bare]
  (if-let [value (token-symbol node)]
    (let [qualifier (namespace value)
          var-name (name value)]
      (if (and (contains? targets var-name)
               (if qualifier
                 (contains? qualifiers qualifier)
                 (contains? live-bare var-name)))
        {:rewrite (respelled replacement-qualifier var-name)}
        {}))
    {}))

(defn- walk-context
  [{:keys [qualifiers targets replacement-qualifier file]}]
  {:decide decide-token
   ;; each arity of a multi-arity fn is its own binding scope, which the
   ;; walker models only when asked; the l01-l03 witnesses are what asks
   :per-arity-scopes true
   :qualifiers qualifiers
   :targets targets
   :replacement-qualifier replacement-qualifier
   :platform (if (str/ends-with? (str file) ".cljs") ":cljs" ":clj")})

(defn- walk
  "Walk `forms` for one question, through the shared lexical walker.

  `:sites` counts hits, `:nodes` are the rewritten forms in order, `:indirect`
  is every shape the walker declined to model rather than guess at."
  [forms context live-bare]
  (let [walked (mapv #(alias-migration/rewrite-forms % context live-bare) forms)]
    {:sites (reduce + 0 (map :sites walked))
     :nodes (mapv :node walked)
     :walked walked
     :indirect (vec (mapcat :indirect walked))}))

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
   "unknown-field"])

;; @spec MCP-OP-HELPER-010
(defn refusal-types
  "The closed set of v1 `error_type` strings this planner can emit.

  `helper-extraction-verification-preflight-unavailable` is NOT here: profile
  admission is a fact about configured runners, which a pure function cannot
  observe, so MCP-OP-HELPER-011 belongs to the boundary alone."
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

;; NOTE ON verification.profile. The planner takes it as an OPAQUE STRING and
;; never refuses on it. Which profiles are admitted -- synchronous,
;; rollback-capable, runnable now -- is a fact about configured runners, not
;; about source text, and a pure function that cannot observe a runner must not
;; pretend to have validated one. MCP-OP-HELPER-011 is the boundary's.

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

(defn- retained-hit
  "The retained var one moved body actually references, or nil.

  Both passes run through the shared lexical walker, so a `let` that shadows a
  retained name, an arity that binds it as a parameter, and a reader discard
  that merely mentions it are all NOT dependencies. The first pass asks the
  cheap question -- is any retained name reached at all -- and only a positive
  answer pays for the per-name pass that says WHICH, because the refusal has to
  name the var."
  [source-lib retained-order retained-index node]
  (let [names (set (keys retained-index))
        context (walk-context {:qualifiers #{source-lib}
                               :targets names
                               :replacement-qualifier "q"})]
    (when (pos? (:sites (walk [node] context names)))
      (some (fn [candidate]
              (let [one (walk-context {:qualifiers #{source-lib}
                                       :targets #{candidate}
                                       :replacement-qualifier "q"})]
                (when (pos? (:sites (walk [node] one #{candidate})))
                  (get retained-index candidate))))
            retained-order))))

(defn- body-dependency-refusal
  "The first dependency or namespace sensitivity that stops a moved body."
  [source-lib owners definitions]
  (let [selected (set (map :name owners))
        retained (remove #(contains? selected (:name %)) definitions)
        retained-index (into {} (map (juxt :name identity)) retained)
        retained-order (mapv :name retained)]
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
         (when-let [hit (retained-hit source-lib retained-order retained-index node)]
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
                      {:helper name :var (str source-lib "/" (:name hit))})))))
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

(defn- walker-refusal
  "Turn a shape the shared walker declined to model into a typed refusal.

  Never a silent unrewritten call: an unmodelled binding scope, a quoted
  reference, or a selected site in an unselected reader-conditional branch all
  land here, named, with the offending form."
  [file indirect]
  (when-let [{:keys [reason form]} (first indirect)]
    (refusal "unsupported-binding"
             (str file " reaches a selected helper through a shape v1 does not"
                  " model (" (name reason) "), so the reference cannot be closed"
                  " mechanically.")
             (str "how " file " should reach the destination at this site")
             {:file file :reason (name reason) :form form})))

(defn- analyze-caller
  "Discovery for ONE file that is not the source.

  Returns nil when the file neither binds nor mentions the source, a refusal
  map, or the facts the edit builder needs and nothing it does not.

  TWO walks, one question each: which references MOVE, and which references
  STAY. Splitting them keeps the tally honest without teaching the shared
  walker a second counter, and only the selected walk can refuse -- an
  unmodelled scope around a reference that is not moving changes no bytes."
  [{:keys [file source]} {:keys [source-lib selected retained]}]
  (let [root (parser/parse-string-all source)
        ns-node (ns-node-of root)
        direct (filterv map? (ns-libspecs ns-node))
        target (first (filter #(= source-lib (:lib %)) direct))
        others (remove #(= source-lib (:lib %)) direct)
        qualifiers (into #{source-lib} (:aliases target))
        referred (:referred target #{})
        forms (remove #(identical? % ns-node) (top-level-forms root))
        ;; counting-only: the real qualifier is chosen once the partition is
        ;; known, so this spelling is never emitted
        probe (fn [targets]
                (walk forms
                      (walk-context {:qualifiers qualifiers
                                     :targets targets
                                     :replacement-qualifier "q"
                                     :file file})
                      referred))
        moving (probe selected)
        staying (probe retained)
        sites (:sites moving)
        retained-sites (:sites staying)
        ;; @spec MCP-OP-HELPER-006
        ;; An unused RETAINED refer is still a binding this file declared, and
        ;; dropping it is a change nobody asked for. A file that refers a
        ;; retained name it never calls is therefore mixed, not moved_only:
        ;; the old require is retained and one new one is added.
        unused-retained-refers (seq (remove selected referred))]
    (or (when ns-node (prefix-list-refusal file ns-node source-lib))
        (when ns-node (use-clause-refusal file ns-node source-lib))
        (when target (target-grammar-refusal file target))
        (when (pos? sites) (walker-refusal file (:indirect moving)))
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
                        (or (pos? retained-sites) unused-retained-refers) "mixed"
                        :else "moved_only")
           :sites sites
           :retained retained-sites
           ;; @spec MCP-OP-HELPER-007
           ;; ns-level bindings only, which is the repository's doctrine and a
           ;; fact about Clojure: a qualified symbol's namespace part resolves
           ;; through the ns alias map, so a local can never shadow it.
           :bound (ns-bound-names direct)
           :qualifiers qualifiers
           :referred referred
           :root root
           :ns-node ns-node
           :target target
           :direct direct
           :forms forms}))))

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
  "One whole-form edit per top-level form that carries a selected reference.

  The find is the COMPLETE original top-level form and the replacement is that
  same form with only the selected symbols respelled, so every other byte --
  comments, strings, docstrings, `#_` discards, the indentation the file chose
  -- is carried through by the walker rather than reproduced by this namespace."
  [forms context live-bare]
  (let [{:keys [walked]} (walk forms context live-bare)]
    (into []
          (keep (fn [[form result]]
                  (when (pos? (:sites result))
                    {:original (n/string form)
                     :replacement (n/string (:node result))})))
          (map vector forms walked))))

(defn- caller-plan
  [{:keys [file partition sites retained qualifiers referred forms] :as analysis}
   dest-lib alias]
  (let [qualified-only? (= "qualified_only" partition)
        context (walk-context {:qualifiers qualifiers
                               :targets (:selected analysis)
                               ;; a caller with no require of the source gets a
                               ;; plain require of the DESTINATION, so its
                               ;; rewritten symbol stays fully qualified and has
                               ;; a load path of its own (MCP-OP-HELPER-014)
                               :replacement-qualifier (if qualified-only? dest-lib alias)
                               :file file})]
    {:file file
     :partition partition
     :alias (when-not qualified-only? alias)
     :sites sites
     :retained retained
     :edits (into [(caller-ns-edit analysis dest-lib alias)]
                  (form-edits forms context referred))}))

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
        context (walk-context {:qualifiers #{source-lib}
                               :targets selected
                               :replacement-qualifier alias
                               :file file})
        ;; inside the defining namespace every top-level name is in scope as a
        ;; bare symbol, so the walker starts with all of them live and shadows
        ;; them per binding form as it descends
        live-bare (into #{} (map :name) definitions)
        walked (walk retained-forms context live-bare)
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
    (or
     (walker-refusal file (:indirect walked))
     {:file file
      :partition "source"
      :alias alias
      :sites (:sites walked)
      :retained 0
      :policy policy
      :edits (into [{:original (n/string ns-node) :replacement (n/string new-ns)}]
                   (concat (mapv #(deletion-edit root (:node %)) owners)
                           (form-edits retained-forms context live-bare)))})))

;; ---------------------------------------------------------------------------
;; the destination namespace

(defn- destination-source
  "The destination's complete text, or a typed refusal.

  The HEADER is not this namespace's to invent. `require_policy :minimal` is
  already implemented, with real free-symbol analysis, in
  `clj-surgeon.extract-header/compile-target-header`, and it keeps every clause
  the moved bodies actually need -- including `:import`, which a require-only
  view does not see at all. Choosing libspecs here by searching the moved text
  for an alias would both invent requires (an alias named in a comment or a
  string literal) and miss real ones (an imported class, a fully qualified
  symbol with no alias). Where that logic cannot prove a minimal header it says
  so, and this returns a typed refusal rather than a candidate that fails to
  load.

  The BODIES move verbatim in definition order. A moved -> moved peer reference
  is already the destination's own bare symbol; a fully qualified self-reference
  is lowered to one. No moved -> retained edge can reach here: MCP-OP-HELPER-019
  refused before the plan was built, which is why this namespace requires
  nothing of the source and no cycle can pass through it."
  [{:keys [file dest-lib source-lib source-ns-node owners]}]
  (let [context (walk-context {:qualifiers #{source-lib}
                               :targets (set (map :name owners))
                               :replacement-qualifier nil
                               :file file})
        walked (walk (mapv :node owners) context #{})
        bodies (mapv n/string (:nodes walked))
        header (extract-header/compile-target-header
                {:source-ns-form (n/string source-ns-node)
                 :target-ns dest-lib
                 :form-sources bodies
                 :require-policy :minimal})]
    (cond
      (seq (:indirect walked)) (walker-refusal file (:indirect walked))

      (not (:ok header))
      (refusal "unsupported-binding"
               (str "The destination's dependency-minimal header cannot be"
                    " proved from the source's own ns form ("
                    (name (or (:reason header) :unknown)) ").")
               (str "how the destination should declare "
                    (pr-str (:entry header)))
               {:file file
                :reason (name (or (:reason header) :unknown))
                :form (:entry header)})

      :else
      {:source (str (:ns-form header) "\n\n"
                    (str/join "\n\n" (map str/trim-newline bodies))
                    "\n")
       :omitted_requires (vec (:omitted-target-requires header))})))

;; ---------------------------------------------------------------------------
;; the receipt (MCP-OP-HELPER-009, MCP-OP-HELPER-012)
;;
;; Counts and histograms only. Nothing here is a file list, and nothing here is
;; a verification result: the planner has run no proof, so its typed checks are
;; zero and `ok` is nil until the boundary executes the profile.

(defn- receipt
  "The O(1) receipt for one plan.

  THREE COUNTS OF FILES, and they are deliberately not the same number:

    caller_files   EXTERNAL callers only -- files OTHER THAN the source that
                   reference a selected owner and are rewritten. The source is
                   not a caller of itself, so it is never counted here, and
                   `expect.caller_files` is compared against THIS number.
    source_file    the extraction subject: 1.
    changed_files  every file this plan writes: caller_files + source_file + the
                   destination it creates.

  `partition` is unchanged and counts CALLERS, with `untouched` the discovered
  bystanders that gain no mutation authority. `sites` and `retained_sites` count
  references, not files, and both include the source-local uses the extraction
  lowers (MCP-OP-HELPER-015).

  No `details_path`: where per-caller detail is written is a fact about the
  workspace, which a pure function cannot observe. The boundary publishes the
  real path."
  [{:keys [helpers files untouched scope-paths]}]
  (let [by-partition (frequencies (map :partition files))
        caller-files (filter #(contains? #{"moved_only" "mixed" "qualified_only"}
                                         (:partition %))
                             files)
        source-files (filter #(= "source" (:partition %)) files)]
    {:operation "helper_extraction"
     :helpers (count helpers)
     :source_retired (count helpers)
     :destination_created true
     :caller_files (count caller-files)
     :source_file (count source-files)
     :changed_files (+ (count caller-files) (count source-files) 1)
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
               :dynamic_references "not-claimed"}}))

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

  `expect.caller_files` counts EXTERNAL callers -- files other than the source
  that reference a selected owner. The source is the extraction subject, not a
  caller of itself, and is never in that number; the receipt reports it
  separately as `source_file`, and `changed_files` is the total this plan
  writes (external callers + source + destination).
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
                                     :else (update acc :files conj
                                                   (assoc result :selected selected))))))
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
                      ;; @spec MCP-OP-HELPER-007
                      ;; ns-level bindings only, exactly as alias-migration
                      ;; defines a collision: a local never shadows a qualifier
                      source-bound (ns-bound-names
                                    (filterv map? (ns-libspecs source-ns-node)))
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
                       (let [source-result
                             (source-plan {:file from-file
                                           :root source-root
                                           :ns-node source-ns-node
                                           :source-lib source-lib
                                           :owners owners
                                           :definitions definitions
                                           :dest-lib dest-lib
                                           :alias source-alias
                                           :policy policy})
                             destination (when-not (false? (:ok source-result))
                                           (destination-source
                                            {:file from-file
                                             :dest-lib dest-lib
                                             :source-lib source-lib
                                             :source-ns-node source-ns-node
                                             :owners owners}))
                             files (when-not (false? (:ok source-result))
                                     (conj (:plans choices) source-result))
                             ;; @spec MCP-OP-HELPER-013
                             ;; the external callers, on the receipt's own
                             ;; definition: the source is not a caller of itself
                             derived (count (:plans choices))
                             expected (get-in request [:expect :caller_files])]
                         (cond
                           (false? (:ok source-result)) source-result
                           (false? (:ok destination)) destination
                           ;; @spec MCP-OP-HELPER-013 MCP-OP-HELPER-017
                           (and (some? expected) (not= expected derived))
                           (refusal "expect-mismatch"
                                    ;; the wording stays clear of the words a
                                    ;; refusal must never offer: this names a
                                    ;; disagreement about a count, never a
                                    ;; smaller problem to solve instead
                                    (str "expect.caller_files does not equal the"
                                         " derived count of EXTERNAL caller"
                                         " files. The source is the extraction"
                                         " subject, not a caller of itself, so"
                                         " it is counted as source_file.")
                                    "which external caller-file count is correct"
                                    {:derived_caller_files derived
                                     :expected_caller_files expected})

                           :else
                           (let [source-hash (sha256 (:source source-entry))]
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
                                :source (:source destination)
                                ;; requires the minimal-header logic proved the
                                ;; moved bodies do NOT use, named rather than
                                ;; silently dropped
                                :omitted_requires (:omitted_requires destination)}
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
                                                 :scope-paths scope-paths})}))))))))))))))))
