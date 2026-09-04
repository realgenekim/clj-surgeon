(ns clj-surgeon.alias-migration
  "Pure planner for one alias-policy var migration across N namespaces.

  Input is a request that is constant in N plus the frozen source text of every
  candidate file. Output is one complete rewrite plan or one typed refusal.
  Nothing here touches the filesystem, so the whole closure is testable from
  literals."
  (:require
   [cheshire.core :as json]
   [clojure.string :as str]
   [rewrite-clj.node :as n]
   [rewrite-clj.parser :as parser]))

;; ---------------------------------------------------------------------------
;; node helpers

(defn- meaningful?
  [node]
  (not (contains? #{:whitespace :newline :comma :comment} (n/tag node))))

(defn- children
  [node]
  (if (n/inner? node) (vec (n/children node)) []))

(defn- meaningful-children
  [node]
  (filterv meaningful? (children node)))

(defn- token-symbol
  "Return the symbol a token node spells, or nil."
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
    (when-let [symbol-value (some-> (first (meaningful-children node))
                                    token-symbol)]
      (name symbol-value))))

(defn- simple-symbol-name
  [node]
  (when-let [symbol-value (token-symbol node)]
    (when (and (nil? (namespace symbol-value))
               (not (contains? #{"&" "_"} (name symbol-value))))
      (name symbol-value))))

;; ---------------------------------------------------------------------------
;; local bindings

(def local-binding-vector-heads
  "Heads whose binding vector introduces LOCALS.

  Their left-hand sides are binding forms, not references: they are never
  migration sites, and they shadow a bare referred name inside the body."
  #{"let" "let*" "if-let" "when-let" "if-some" "when-some" "loop" "loop*"
    "with-open" "with-local-vars" "doseq" "for" "dotimes"})


;; @spec MCP-OP-ALIAS-030
(def var-binding-vector-heads
  "Heads whose binding vector names VARS, not locals.

  `(binding [store/*clock* t] ...)` and `(with-redefs [store/snapshot f] ...)`
  rebind Vars: every left-hand side is a REFERENCE, normally a qualified symbol
  resolved through the namespace's alias map, so it is a migration site and it
  introduces no local. Treating these like `let` is what left
  `store/*clock*` behind after the alias was gone."
  #{"binding" "with-redefs" "with-bindings"})

(def binding-vector-heads
  "Every head that takes a binding vector, whatever it binds."
  (into local-binding-vector-heads var-binding-vector-heads))

(def function-heads
  #{"fn" "fn*" "defn" "defn-" "defmacro" "defmethod" "definline"})

(defn- binding-form-names
  "Every simple symbol a binding form introduces, including destructuring."
  [node]
  (into #{}
        (comp (filter #(= :token (n/tag %)))
              (keep simple-symbol-name))
        (tree-seq n/inner? n/children node)))

(defn- vector-node?
  [node]
  (= :vector (n/tag node)))

(defn- pair-binding-names
  "Names bound by a let-style binding vector (even positions only)."
  [vector-node]
  (let [entries (meaningful-children vector-node)]
    (into #{}
          (mapcat binding-form-names)
          (take-nth 2 entries))))

(defn- letfn-binding-names
  [vector-node]
  (into #{}
        (mapcat (fn [entry]
                  (let [parts (meaningful-children entry)]
                    (into (if-let [name-node (first parts)]
                            (set (keep identity [(simple-symbol-name name-node)]))
                            #{})
                          (mapcat binding-form-names)
                          (filter vector-node? parts)))))
        (meaningful-children vector-node)))

(defn- parameter-vectors
  "The exact parameter vectors of a fn-shaped form.

  A fn-shaped form has at most one direct parameter vector — the first vector
  after its head — and every later vector is body. Multi-arity forms carry one
  parameter vector as the first element of each arity list."
  [node]
  (let [parts (rest (meaningful-children node))
        direct (first (filter vector-node? parts))]
    (if direct
      #{direct}
      (into #{}
            (keep (fn [part]
                    (when (= :list (n/tag part))
                      (let [inner (meaningful-children part)]
                        (when (and (seq inner) (vector-node? (first inner)))
                          (first inner))))))
            parts))))

(defn- function-parameter-names
  "Names bound by the parameter vectors of a fn/defn-shaped form."
  [node]
  (into #{} (mapcat binding-form-names) (parameter-vectors node)))

(defn- letfn-binding-parameter-vectors
  "Parameter vectors of every function defined in a letfn binding vector."
  [vector-node]
  (into #{} (mapcat parameter-vectors) (meaningful-children vector-node)))

(defn- form-introduced-names
  "Names one form introduces into scope for its own remaining children."
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

      (= "as->" head)
      (if-let [name-node (nth parts 2 nil)]
        (set (keep identity [(simple-symbol-name name-node)]))
        #{})

      (= "catch" head)
      (if-let [name-node (nth parts 2 nil)]
        (set (keep identity [(simple-symbol-name name-node)]))
        #{})

      :else #{})))

;; ---------------------------------------------------------------------------
;; ns form and requires

(defn- top-level-forms
  [root-node]
  (filterv meaningful? (children root-node)))

(defn- ns-form
  [root-node]
  (first (filter #(= "ns" (head-name %)) (top-level-forms root-node))))

(defn- clause?
  [node clause-keyword]
  (and (= :list (n/tag node))
       (= clause-keyword (some-> (first (meaningful-children node))
                                 token-string))))

(defn- libspec-facts
  "Facts for one direct libspec node, or ::indirect when it is a prefix list."
  [node]
  (case (n/tag node)
    :token (when-let [symbol-value (token-symbol node)]
             {:lib (name symbol-value) :aliases [] :referred #{} :refer-all? false
              :node node})
    :vector
    (let [parts (meaningful-children node)
          lib (some-> (first parts) token-symbol name)
          options (rest parts)]
      (when lib
        (loop [remaining options
               aliases []
               referred #{}
               refer-all? false]
          (if-let [option (first remaining)]
            (let [option-text (token-string option)
                  value (second remaining)]
              (cond
                (contains? #{":as" ":as-alias"} option-text)
                (recur (drop 2 remaining)
                       (cond-> aliases
                         (some-> value token-symbol)
                         (conj (name (token-symbol value))))
                       referred refer-all?)

                (= ":refer" option-text)
                (if (and value (vector-node? value))
                  (recur (drop 2 remaining) aliases
                         (into referred
                               (keep simple-symbol-name)
                               (meaningful-children value))
                         refer-all?)
                  (recur (drop 2 remaining) aliases referred true))

                :else (recur (rest remaining) aliases referred refer-all?)))
            {:lib lib :aliases aliases :referred referred
             :refer-all? refer-all? :node node}))))
    :list ::indirect
    nil))

(defn- require-clause
  [ns-node]
  (first (filter #(clause? % ":require") (meaningful-children ns-node))))

(defn- ns-libspecs
  "Direct libspec facts for the file's single :require clause."
  [ns-node]
  (when-let [clause (require-clause ns-node)]
    (mapv libspec-facts (rest (meaningful-children clause)))))

;; ---------------------------------------------------------------------------
;; reader conditionals

(defn- reader-conditional-node?
  [node]
  (and (= :reader-macro (n/tag node))
       (contains? #{"?" "?@"} (some-> (first (children node)) n/string))))

(defn- platform-keyword
  [file]
  (cond
    (str/ends-with? file ".cljs") ":cljs"
    :else ":clj"))

(defn- reader-conditional-body-index
  "Index in the reader-macro's children of the branch list."
  [node]
  (first (keep-indexed
           (fn [index child]
             (when (contains? #{:list :vector} (n/tag child)) index))
           (children node))))

(defn- branch-positions
  "Split a reader conditional body into selected and unselected value indexes."
  [body-children platform]
  (let [meaningful-positions (vec (keep-indexed
                                    (fn [index child]
                                      (when (meaningful? child) index))
                                    body-children))
        pairs (partition 2 meaningful-positions)
        selected? (fn [key-position]
                    (contains? #{platform ":default"}
                               (token-string (nth body-children key-position))))]
    {:selected (set (keep (fn [[key-position value-position]]
                            (when (selected? key-position) value-position))
                          pairs))
     :unselected (set (keep (fn [[key-position value-position]]
                              (when-not (selected? key-position) value-position))
                            pairs))}))

;; ---------------------------------------------------------------------------
;; walking: sites, rewrites, indirect detection
;;
;; The walk carries `live-bare`: the set of referred names that are still
;; unshadowed at this point in the tree. Entering a binding form removes the
;; names it introduces, so shadowing is lexical rather than file-wide.

(declare rewrite-binding-vector)


;; @spec MCP-OP-ALIAS-025

;; @spec MCP-OP-ALIAS-032
(defn- auto-resolved-keyword-parts
  "For a token spelled ::qualifier/name, return [qualifier name].

  `::store/k` is resolved through the namespace's alias map by the reader
  exactly as `store/x` is, so it moves with the alias. A single-colon
  `:store/k` is a plain keyword that nothing resolves and never moves."
  [node]
  (when-let [text (token-string node)]
    (when (str/starts-with? text "::")
      (let [body (subs text 2)
            separator (str/index-of body "/")]
        (when separator
          [(subs body 0 separator) (subs body (inc separator))])))))

(defn- token-facts
  "Classify one token against the migration.

  Matching is by whole symbol identity, never by substring, so a
  prefix-sharing sibling such as `store-pg` can never be mistaken for `store`.

  :rewrite    the replacement spelling, or nil to leave it alone
  :refer-hit? the token is a live bare occurrence of a referred name
  :other-use? the token names the old lib but is not part of this migration"
  [node {:keys [mode qualifiers from-var to-var alias rewrite-bare count-bare]
         :as context}
   live-bare]
  (if-let [[keyword-qualifier _keyword-name] (auto-resolved-keyword-parts node)]
    (if (contains? qualifiers keyword-qualifier)
      ;; Rewriting would change the KEYWORD'S VALUE, because a keyword's
      ;; namespace is part of its identity and may be persisted, dispatched on,
      ;; or compared elsewhere; leaving it breaks the read once the alias is
      ;; gone. Neither is bookkeeping, so the verb refuses and says so.
      {:indirect {:reason :auto-resolved-keyword}}
      {})
    (let [value (token-symbol node)]
    (if-not value
      {}
      (let [qualifier (namespace value)
            var-name (name value)]
        (if qualifier
          (if-not (contains? qualifiers qualifier)
            {}
            (let [fully-qualified? (= qualifier (:from-lib context))
                  ;; @spec MCP-OP-ALIAS-035
                  ;; Inside a plain quote the symbol is a VALUE, not a
                  ;; reference the reader resolves: the alias map plays no part
                  ;; in what it names. A fully-qualified quoted symbol names
                  ;; exactly one namespace from anywhere, which is the whole
                  ;; reason it is a site at all — (requiring-resolve
                  ;; 'old.lib/v) would otherwise break lazily at call time —
                  ;; so its replacement must name exactly one namespace too.
                  ;; Alias-qualifying it produces 'alias/x, the shape this same
                  ;; verb refuses to READ because nothing resolves it.
                  qualify (if (and (:quoted-data? context) fully-qualified?)
                            (:to-lib context)
                            alias)]
              (if (= :lib mode)
                {:rewrite (str qualify "/" var-name)
                 :fully-qualified? fully-qualified?}
                (if (= var-name from-var)
                  {:rewrite (str qualify "/" to-var)
                   :fully-qualified? fully-qualified?}
                  {:other-use? true}))))
          (let [live? (contains? live-bare var-name)]
            (cond-> {}
              (and live? (contains? count-bare var-name))
              (assoc :refer-hit? true)

              (and live? (contains? rewrite-bare var-name))
              (assoc :rewrite (if (= :lib mode)
                                (str alias "/" var-name)
                                (str alias "/" to-var)))))))))))

;; @spec MCP-OP-ALIAS-031
(defn- quoted-facts
  "Site facts for every token inside a quoted form."
  [node context live-bare]
  (->> (tree-seq n/inner? n/children node)
       (filter #(= :token (n/tag %)))
       (keep #(let [facts (token-facts % context live-bare)]
                (when (:rewrite facts) facts)))
       vec))

(def ^:private empty-walk
  {:nodes [] :sites 0 :refer-sites 0 :indirect [] :other-use false
   :unselected-sites false})

(defn- accumulate
  [state result]
  (-> state
      (update :nodes conj (:node result))
      (update :sites + (:sites result))
      (update :refer-sites + (:refer-sites result))
      (update :indirect into (:indirect result))
      (update :other-use #(or % (:other-use result)))
      (update :unselected-sites #(or % (:unselected-sites result)))))

(defn- walk-result
  [node walked]
  {:node (n/replace-children node (:nodes walked))
   :sites (:sites walked)
   :refer-sites (:refer-sites walked)
   :indirect (:indirect walked)
   :other-use (:other-use walked)
   :unselected-sites (:unselected-sites walked)})

(defn- leaf
  [node]
  {:node node :sites 0 :refer-sites 0 :indirect [] :other-use false
   :unselected-sites false})

;; @spec MCP-OP-ALIAS-029
;; @spec MCP-OP-ALIAS-033
(defn- rewrite-forms
  "Walk one node and return its rewritten node plus the migration's tallies.

  Every node type a qualified symbol can sit in is walked: operator and
  argument position, binding vectors, map keys and values, vector elements,
  metadata values, `:var` nodes (`#'x`), and `(var x)` lists."
  [node {:keys [platform] :as context} live-bare]
  (let [tag (n/tag node)]
    (cond
      ;; a reader discard is data the contract keeps exactly as it is
      (= :uneval tag) (leaf node)

      ;; `alias/x inside a syntax quote IS resolved through the alias map by
      ;; the reader, so it is ordinary code and must migrate with everything
      ;; else. A plain 'alias/x is a literal symbol that nothing resolves, so
      ;; whether it is a reference is a judgment and the verb refuses.
      (= :quote tag)
      (let [facts (quoted-facts node context live-bare)]
        (cond
          (empty? facts) (leaf node)

          ;; 'fully.qualified.lib/x names exactly one namespace and nothing
          ;; resolves it away, so the rewrite is mechanical — this is how a
          ;; runtime (requiring-resolve 'old.lib/v) survives a lib rename
          (every? :fully-qualified? facts)
          (let [quoted-context (assoc context :quoted-data? true)
                walked (reduce (fn [state child]
                                 (accumulate state
                                             (rewrite-forms child quoted-context
                                                            live-bare)))
                               empty-walk
                               (children node))]
            (walk-result node walked))

          :else
          (assoc (leaf node)
                 :indirect [{:reason :quoted-reference :form (n/string node)}])))

      (= :token tag)
      (let [{:keys [rewrite refer-hit? other-use? indirect]}
            (token-facts node context live-bare)]
        (cond-> (leaf node)
          rewrite (assoc :node (parser/parse-string rewrite) :sites 1)
          refer-hit? (assoc :refer-sites 1)
          other-use? (assoc :other-use true)
          indirect (assoc :indirect
                          [(assoc indirect :form (n/string node))])))


      (reader-conditional-node? node)
      (let [kids (children node)
            body-index (reader-conditional-body-index node)
            body (nth kids body-index)
            body-kids (children body)
            {:keys [selected unselected]} (branch-positions body-kids platform)
            branch-site? (fn [position]
                           (some #(:rewrite (token-facts % context live-bare))
                                 (filter #(= :token (n/tag %))
                                         (tree-seq n/inner? n/children
                                                   (nth body-kids position)))))
            unselected-hit? (boolean (some branch-site? unselected))
            walked (reduce
                     (fn [state [index child]]
                       (if (contains? selected index)
                         (accumulate state (rewrite-forms child context live-bare))
                         (update state :nodes conj child)))
                     empty-walk
                     (map-indexed vector body-kids))
            rebuilt (assoc kids body-index
                           (n/replace-children body (:nodes walked)))]
        (-> (walk-result node (assoc walked :nodes rebuilt))
            (update :indirect
                    #(cond-> %
                       unselected-hit?
                       (conj {:reason :unselected-reader-conditional-branch
                              :form (n/string node)})))
            (update :unselected-sites #(or % unselected-hit?))))

      (n/inner? node)
      (let [head (head-name node)
            introduced (form-introduced-names node)
            kids (children node)
            ;; a var-binding vector is ordinary code on BOTH sides
            binding-vector (when (contains? local-binding-vector-heads head)
                             (first (filter vector-node?
                                            (rest (meaningful-children node)))))
            letfn-vector (when (= "letfn" head)
                           (first (filter vector-node?
                                          (rest (meaningful-children node)))))
            skipped-vectors (cond-> #{}
                              (contains? function-heads head)
                              (into (parameter-vectors node))

                              letfn-vector
                              (into (letfn-binding-parameter-vectors letfn-vector)))
            ;; names this form binds leave scope for every child except the
            ;; let-style binding vector, which accumulates them pair by pair
            body-bare (reduce disj live-bare introduced)]
        (walk-result
          node
          (reduce
            (fn [state child]
              (cond
                (identical? child binding-vector)
                (accumulate state (rewrite-binding-vector child context live-bare))

                (identical? child letfn-vector)
                (accumulate state (rewrite-forms child context body-bare))

                (contains? skipped-vectors child)
                (update state :nodes conj child)

                :else
                (accumulate state (rewrite-forms child context body-bare))))
            empty-walk
            kids)))

      :else (leaf node))))

(defn- rewrite-binding-vector
  "Rewrite a let-style binding vector.

  Init expressions are rewritten, binding forms are not, and each pair's names
  leave scope for every later pair and for the form's body."
  [vector-node context live-bare]
  (let [kids (children vector-node)
        meaningful-positions (vec (keep-indexed
                                    (fn [index child]
                                      (when (meaningful? child) index))
                                    kids))
        binding-positions (set (take-nth 2 meaningful-positions))
        walked
        (reduce
          (fn [state [index child]]
            (if (contains? binding-positions index)
              (-> state
                  (update :nodes conj child)
                  (update :live-bare #(reduce disj % (binding-form-names child))))
              (accumulate state (rewrite-forms child context (:live-bare state)))))
          (assoc empty-walk :live-bare live-bare)
          (map-indexed vector kids))]
    (walk-result vector-node walked)))
;; ---------------------------------------------------------------------------
;; require rewriting

(defn- libspec-node
  [lib alias]
  (n/vector-node [(n/token-node (symbol lib))
                  (n/spaces 1)
                  (n/keyword-node :as)
                  (n/spaces 1)
                  (n/token-node (symbol alias))]))

(defn- replace-child
  [parent old-child new-child]
  (n/replace-children
    parent
    (mapv #(if (identical? % old-child) new-child %) (children parent))))

(defn- rewrite-require-clause
  "Replace or add the target libspec inside one :require clause node."
  [clause-node old-node mode lib alias]
  (let [target (libspec-node lib alias)]
    (if (= :replace mode)
      (replace-child clause-node old-node target)
      (let [kids (children clause-node)
            last-libspec-index
            (last (keep-indexed
                    (fn [index child]
                      (when (and (meaningful? child)
                                 (contains? #{:token :vector :list} (n/tag child))
                                 (pos? index))
                        index))
                    kids))
            previous-libspec-index
            (last (butlast (keep-indexed
                             (fn [index child]
                               (when (and (meaningful? child)
                                          (contains? #{:token :vector :list} (n/tag child))
                                          (pos? index))
                                 index))
                             kids)))
            separator (if (and previous-libspec-index last-libspec-index)
                        (subvec kids (inc previous-libspec-index) last-libspec-index)
                        [(n/newline-node "\n") (n/spaces 3)])]
        (n/replace-children
          clause-node
          (vec (concat (subvec kids 0 (inc last-libspec-index))
                       separator
                       [target]
                       (subvec kids (inc last-libspec-index)))))))))

;; ---------------------------------------------------------------------------
;; refusals

(defn- refusal
  [error-type message extra next-call]
  (merge {:ok false
          :operation "alias_migration"
          :error_type (name error-type)
          :error message
          :source_unchanged true
          :mutation_attempted false
          :write_authority false
          :next_action "correct_request"
          :next_call next-call}
         extra))

(defn- base-call
  [request]
  (cond-> {"op" "alias_migration"
           "from" {"lib" (get-in request [:from :lib])
                   "var" (get-in request [:from :var])}
           "to" (cond-> {"lib" (get-in request [:to :lib])
                         "var" (get-in request [:to :var])
                         "alias_policy" (vec (get-in request [:to :alias-policy]))}
                  (get-in request [:to :refer-policy])
                  (assoc "refer_policy" (get-in request [:to :refer-policy])))
           ;; @spec MCP-OP-ALIAS-051
           ;; exclusions travel with EVERY composed call, not only the ones
           ;; that add to them: an expect-mismatch next_call that forgot the
           ;; exclusion it was just handed cannot converge, it loops
           "scope" (cond-> {"paths" (vec (get-in request [:scope :paths]))}
                     (seq (get-in request [:scope :exclude]))
                     (assoc "exclude" (vec (get-in request [:scope :exclude]))))
           "expect" {"files" (get-in request [:expect :files])}}
    (:workspace-root request)
    (assoc "workspace_root" (:workspace-root request))))

;; @spec MCP-OP-ALIAS-051
(def expect-files-unchanged-reason
  "Why a refusal that narrowed or excluded left `expect.files` as declared."
  (str "Not one file of the narrowed or excluded scope was read, so whether it"
       " requires from.lib is not known and expect.files stays as declared. A"
       " count corrected on a guess is worse than one left alone: if the"
       " declaration is now wrong, the alias-migration-expect-mismatch refusal"
       " carries the found count and its own executable next_call."))

(def max-next-call-characters
  "Ceiling on the JSON length of a `next_call` this verb publishes.

  A refusal is a constant-size receipt or it is not a receipt. The narrowing
  call below is bounded here rather than trusted to be short, because the one
  thing in it that grows without limit is the caller's own workspace path."
  512)

(defn- within-next-call-bound?
  "Whether one composed call fits the published `next_call` ceiling.

  Measured on the JSON the MCP boundary actually publishes: the wire form is
  the only form that has a length."
  [call]
  (<= (count (json/generate-string call)) max-next-call-characters))

;; @spec MCP-OP-ALIAS-015
;; @spec MCP-OP-ALIAS-051
(defn excluding-call
  "The same request with one file, or several, excluded from scope.

  `requiring-status` says what the tool KNOWS about the excluded sources.
  `:known-requiring` — the file was read and its reference to from.lib
  established — means the caller's expectation counted it, so `expect.files`
  drops by the number of files this call newly excludes. `:unknown` — the
  filesystem refused the file before it was ever opened — means nothing about
  its requiring status is knowable, and `expect.files` is left exactly as
  declared; the refusal publishes `expect-files-unchanged-reason` beside the
  call to say so. Decrementing there walks the count away from the truth: two
  unread exclusions of non-requiring files drive a correct 2 down to 0, and the
  caller then replays against a count the tool itself corrupted.

  The reason rides the refusal rather than the call because the request is a
  closed field set: an explanatory key inside `next_call` would make the replay
  refuse as an unknown field.

  Exclusions the request already carried are preserved rather than replaced, so
  a chain of these refusals converges instead of forgetting what the previous
  one learned.

  This is the shape every `alias_migration` refusal that names a file uses, the
  filesystem-boundary ones included: a refusal that names one bad file and
  offers nothing executable makes the caller compose the remedy by hand."
  ([request file-or-files] (excluding-call request file-or-files :known-requiring))
  ([request file-or-files requiring-status]
   (let [named (if (string? file-or-files) [file-or-files] (vec file-or-files))
         already (set (get-in request [:scope :exclude]))
         fresh (vec (remove already named))
         exclude (into (vec (get-in request [:scope :exclude])) fresh)
         declared (or (get-in request [:expect :files]) 1)]
     (-> (base-call request)
         (assoc-in ["scope" "exclude"] exclude)
         (assoc-in ["expect" "files"]
                   (if (= :known-requiring requiring-status)
                     (max 0 (- declared (count fresh)))
                     declared))))))

;; @spec MCP-OP-ALIAS-058
(defn next-call-characters
  "The wire length of one composed call.

  `within-next-call-bound?` measures the JSON the MCP boundary publishes, so a
  refusal that REPORTS the size it could not publish must measure it the same
  way, or the caller cannot check the arithmetic the refusal states."
  [call]
  (count (json/generate-string call)))

;; @spec MCP-OP-ALIAS-058
(defn rescoping-call-shape
  "The rescoping call as composed, whether or not it fits the bound.

  `rescoping-call` answers nil past the ceiling, which is the right answer for
  a caller and the wrong one for a refusal that must say HOW FAR past it the
  call was: a call nobody composed has no length. This is the composition
  without the guard, so the guard's own subject can be measured and named."
  [request paths]
  (assoc-in (base-call request) ["scope" "paths"] (vec paths)))

;; @spec MCP-OP-ALIAS-058
(defn rescoping-call
  "The same request with `scope.paths` replaced outright, or nil if it would not fit.

  The executable remedy for a scope that matched NOTHING, where no exclusion and
  no narrowing prefix can help: the caller's spelling selected no file, so the
  only correction is a different spelling. `expect.files` is left exactly as
  declared, because not one file of the new scope has been read."
  [request paths]
  (let [call (rescoping-call-shape request paths)]
    (when (and (seq paths) (within-next-call-bound? call)) call)))

;; @spec MCP-OP-ALIAS-015
;; @spec MCP-OP-ALIAS-055
(defn narrowing-call
  "The same request narrowed to one subtree prefix, or nil if it would not fit.

  This is the executable remedy for the two AGGREGATE ceilings, where no
  bounded set of exclusions exists: one prefix replaces `scope.paths` outright,
  so the call stays constant in the size of the tree it is narrowing. Any
  exclusions the request carried are preserved, and `expect.files` is left as
  declared, because not one file of the narrowed scope has been read."
  [request prefix]
  (let [exclude (vec (get-in request [:scope :exclude]))
        call (cond-> (assoc-in (base-call request) ["scope" "paths"]
                               [(str prefix "/**")])
               (seq exclude) (assoc-in ["scope" "exclude"] exclude))]
    (when (within-next-call-bound? call) call)))

;; ---------------------------------------------------------------------------
;; ---------------------------------------------------------------------------
;; per-file planning

(defn- prefix-list-nodes
  [clause-node]
  (filterv #(= :list (n/tag %)) (rest (meaningful-children clause-node))))

(defn ns-declared-name
  "The namespace one file declares, as a string, or nil."
  [root]
  (when-let [node (ns-form root)]
    (some-> (nth (meaningful-children node) 1 nil) token-symbol name)))

(defn- analyze-requires
  "Classify one file's ns requires.

  Returns nil when the file does not require from.lib, {:refusal r} when the
  reference cannot be closed mechanically, or the direct libspec facts."
  [request file root]
  (let [from-lib (get-in request [:from :lib])
        ns-node (ns-form root)]
    (when ns-node
      (let [clause (require-clause ns-node)
            libspecs (when clause (ns-libspecs ns-node))
            direct (filterv map? libspecs)
            target (first (filter #(= from-lib (:lib %)) direct))
            use-clause (first (filter #(clause? % ":use") (meaningful-children ns-node)))
            prefix-lists (when clause (prefix-list-nodes clause))
            suspicious-prefix
            (first (filter #(str/includes? (n/string %)
                                           (first (str/split from-lib #"\.")))
                           prefix-lists))
            runtime-form
            (first (filter (fn [form]
                             (and (not (identical? form ns-node))
                                  (some #(= from-lib (some-> (token-symbol %) name))
                                        (filter #(= :token (n/tag %))
                                                (tree-seq n/inner? n/children form)))))
                           (top-level-forms root)))]
        (cond
          suspicious-prefix
          {:refusal (refusal :alias-migration-indirect-reference
                             (str "A prefix-list libspec in " file " may name "
                                  from-lib "; the tool cannot close it mechanically")
                             {:file file :form (n/string suspicious-prefix)}
                             (excluding-call request file))}

          (and use-clause
               (str/includes? (n/string use-clause) from-lib))
          {:refusal (refusal :alias-migration-indirect-reference
                             (str file " reaches " from-lib
                                  " through a :use clause; referred names cannot be"
                                  " closed mechanically")
                             {:file file :form (n/string use-clause)}
                             (excluding-call request file))}

          runtime-form
          {:refusal (refusal :alias-migration-indirect-reference
                             (str file " mentions " from-lib " outside its ns form")
                             {:file file
                              :form (str/trim (subs (n/string runtime-form)
                                                    0 (min 200 (count (n/string runtime-form)))))}
                             (excluding-call request file))}

          (nil? target) nil

          :else {:ns-node ns-node :clause clause :direct direct :target target
                 :others (remove #(= from-lib (:lib %)) direct)})))))

(defn- ownership-refusal
  "Refuse when a bare occurrence could resolve to two required namespaces."
  [request file from-lib from-var target others]
  (when from-var
    (let [bare? (or (contains? (:referred target) from-var) (:refer-all? target))
          competing (first (filter #(or (contains? (:referred %) from-var)
                                        (:refer-all? %))
                                   others))]
      (when (and bare? competing)
        (refusal :alias-migration-ambiguous-ownership
                 (str "A bare " from-var " in " file
                      " could resolve to two required namespaces")
                 {:file file
                  :candidates [(str from-lib "/" from-var)
                               (str (:lib competing) "/" from-var)]}
                 (excluding-call request file))))))

(defn- refer-all-refusal
  "A lib-only migration cannot close :refer :all: the referred set is unknown."
  [request file target]
  (when (:refer-all? target)
    (refusal :alias-migration-indirect-reference
             (str file " requires the old lib with :refer :all; the referred set"
                  " is not mechanically knowable")
             {:file file :reason "refer-all" :form (n/string (:node target))}
             (excluding-call request file))))

;; @spec MCP-OP-ALIAS-007
(defn ns-bound-names
  "The names a new require alias could actually collide with in one file.

  Exactly the aliases introduced by :as and :as-alias plus the names introduced
  by :refer — nothing else. A LOCAL is not a collision, and this is a fact about
  Clojure rather than a policy choice: in `store2/fetch-event` the symbol is
  QUALIFIED, and a qualified symbol's namespace part is resolved through the
  namespace's alias map at read and analysis time. A `let`, `loop`, `fn`
  parameter, or destructured name lives in lexical scope and can never shadow a
  qualifier, so `(let [store2 1] (store2/fetch-event id))` is unambiguous and
  correct. Top-level definitions are excluded for the same reason: a var named
  `store2` and an alias named `store2` coexist, because `store2` alone reads the
  var while `store2/x` reads through the alias."
  [direct]
  (into #{} (concat (mapcat :aliases direct) (mapcat :referred direct))))

(defn- choose-alias
  "First alias_policy entry bound to nothing in this file's ns form."
  [_root direct policy]
  (let [bound (ns-bound-names direct)
        collided (vec (take-while #(contains? bound %) policy))]
    {:alias (first (drop-while #(contains? bound %) policy))
     :collided collided}))

(defn- libspec-with-refer
  "The new libspec, carrying :as only when the file still needs the alias."
  [lib alias referred alias-needed?]
  (n/vector-node
    (vec (concat [(n/token-node (symbol lib))]
                 (when alias-needed?
                   [(n/spaces 1) (n/keyword-node :as) (n/spaces 1)
                    (n/token-node (symbol alias))])
                 [(n/spaces 1) (n/keyword-node :refer) (n/spaces 1)
                  (n/vector-node
                    (interpose (n/spaces 1)
                               (mapv #(n/token-node (symbol %)) (sort referred))))]))))

(defn- ns-form-edit
  [ns-node clause target-node mode to-lib alias referred alias-needed?]
  {:kind :ns
   :original (n/string ns-node)
   :replacement
   (n/string
     (replace-child
       ns-node clause
       (if (empty? referred)
         (rewrite-require-clause clause target-node mode to-lib alias)
         (replace-child clause target-node
                        (libspec-with-refer to-lib alias referred
                                            alias-needed?)))))})

;; @spec MCP-OP-ALIAS-013
;; @spec MCP-OP-ALIAS-021
;; @spec MCP-OP-ALIAS-024
;; @spec MCP-OP-ALIAS-035
(defn- file-plan
  "Plan one file, or return {:refusal r}, or nil when the file is out of scope."
  [request file source]
  (let [from-lib (get-in request [:from :lib])
        from-var (get-in request [:from :var])
        to-lib (get-in request [:to :lib])
        to-var (get-in request [:to :var])
        lib-mode? (nil? from-var)
        preserve-refer? (not= "alias-qualify" (get-in request [:to :refer-policy]))
        policy (vec (get-in request [:to :alias-policy]))
        root (parser/parse-string-all source)
        analysis (analyze-requires request file root)]
    (cond
      ;; A file may never require the lib and still reference it fully
      ;; qualified, e.g. (requiring-resolve 'old.lib/v) deliberately avoiding
      ;; the compile-time dependency. Retiring the lib breaks it at RUNTIME,
      ;; where no load check can see it.
      (and (nil? analysis) lib-mode?)
      (let [context {:mode :lib
                     :from-lib from-lib
                     :to-lib to-lib
                     :qualifiers #{from-lib}
                     :alias to-lib
                     :rewrite-bare #{}
                     :count-bare #{}
                     :platform (platform-keyword file)}
            forms (top-level-forms root)
            walked (mapv #(rewrite-forms % context #{}) forms)
            indirect (vec (mapcat :indirect walked))
            sites (reduce + 0 (map :sites walked))]
        (cond
          (seq indirect)
          {:refusal (refusal :alias-migration-indirect-reference
                             (str "An indirect reference to " from-lib " in " file
                                  " cannot be closed mechanically")
                             {:file file
                              :reason (name (:reason (first indirect)))
                              :form (:form (first indirect))}
                             (excluding-call request file))}

          (zero? sites) nil

          :else
          {:file file
           :alias to-lib
           :collided []
           :sites sites
           :refer-sites 0
           :require-mode :qualified-only
           :edits (vec (keep (fn [[form result]]
                               (when (pos? (:sites result))
                                 {:kind :form
                                  :original (n/string form)
                                  :replacement (n/string (:node result))}))
                             (map vector forms walked)))}))

      (nil? analysis) nil
      (:refusal analysis) analysis

      :else
      (let [{:keys [ns-node clause direct target others]} analysis]
        (cond
          (ownership-refusal request file from-lib from-var target others)
          {:refusal (ownership-refusal request file from-lib from-var target others)}

          (and lib-mode? (refer-all-refusal request file target))
          {:refusal (refer-all-refusal request file target)}

          :else
          (let [{:keys [alias collided]} (choose-alias root direct policy)]
            (if (nil? alias)
              {:refusal (refusal :alias-migration-alias-policy-exhausted
                                 (str "Every alias_policy entry is already bound in "
                                      file)
                                 {:file file
                                  :alias_policy policy
                                  :collided_bindings collided}
                                 (-> (base-call request)
                                     (update-in ["to" "alias_policy"] conj
                                                (str (last policy) "-2"))))}
              (let [referred (:referred target)
                    var-bare? (and (not lib-mode?)
                                   (or (contains? referred from-var)
                                       (:refer-all? target)))
                    count-bare (if lib-mode?
                                 referred
                                 (if var-bare? #{from-var} #{}))
                    rewrite-bare (cond
                                   (not lib-mode?) count-bare
                                   preserve-refer? #{}
                                   :else referred)
                    ;; preserve-refer keeps the same referred names against the
                    ;; new lib; alias-qualify rewrites them and drops the :refer
                    kept-refer (if (and lib-mode? preserve-refer?) referred #{})
                    context {:mode (if lib-mode? :lib :var)
                             :from-lib from-lib
                             :to-lib to-lib
                             :qualifiers (into #{from-lib} (:aliases target))
                             :from-var from-var
                             :to-var to-var
                             :alias alias
                             :rewrite-bare rewrite-bare
                             :count-bare count-bare
                             :platform (platform-keyword file)}
                    forms (top-level-forms root)
                    walked (mapv (fn [form]
                                   (if (identical? form ns-node)
                                     (leaf form)
                                     (rewrite-forms form context count-bare)))
                                 forms)
                    indirect (vec (mapcat :indirect walked))
                    unselected? (boolean (some :unselected-sites walked))
                    other-use? (boolean (some :other-use walked))
                    cljc? (str/ends-with? file ".cljc")
                    blocking (first (remove
                                      #(and (= :unselected-reader-conditional-branch
                                               (:reason %))
                                            (not cljc?))
                                      indirect))
                    sites (reduce + 0 (map :sites walked))
                    refer-sites (reduce + 0 (map :refer-sites walked))]
                (cond
                  blocking
                  {:refusal (refusal :alias-migration-indirect-reference
                                     (str "An indirect or macro-mediated reference in "
                                          file " cannot be closed mechanically")
                                     {:file file
                                      :reason (name (:reason blocking))
                                      :form (:form blocking)}
                                     (excluding-call request file))}

                  (and (zero? sites) (zero? refer-sites)) nil

                  :else
                  (let [mode (if (or unselected? other-use?) :add :replace)]
                    {:file file
                     :alias alias
                     :collided collided
                     :sites sites
                     :refer-sites refer-sites
                     :require-mode mode
                     :edits (into [(ns-form-edit ns-node clause (:node target)
                                                 mode to-lib alias
                                                 (if (= :add mode) #{} kept-refer)
                                                 (pos? sites))]
                                  (keep (fn [[form result]]
                                          (when (pos? (:sites result))
                                            {:kind :form
                                             :original (n/string form)
                                             :replacement (n/string (:node result))}))
                                        (map vector forms walked)))}))))))))))

;; ---------------------------------------------------------------------------
;; the defining namespace

(defn lib-path
  "The project-relative path a namespace must live at, given a sibling path."
  [lib extension]
  (str (str/replace (str/replace lib "-" "_") "." "/") extension))

(defn- rename-defining-ns
  "Rewrite only the ns name token of the defining file."
  [source to-lib]
  (let [root (parser/parse-string-all source)
        node (ns-form root)
        kids (children node)
        name-index (second (keep-indexed
                             (fn [index child]
                               (when (meaningful? child) index))
                             kids))]
    (n/string
      (n/replace-children
        root
        (mapv #(if (identical? % node)
                 (n/replace-children
                   node (assoc kids name-index
                               (n/token-node (symbol to-lib))))
                 %)
              (children root))))))

;; @spec MCP-OP-ALIAS-034
(defn string-mentions
  "`file:line` of every STRING literal naming what this migration retires.

  These are not code references — they are assertions about the codebase
  (architecture tests), documentation paths, and data. The verb does not touch
  them and does not refuse for them, but a silent zero would hide real work, so
  the count travels in the receipt.

  The needle is what the migration RETIRES and is chosen by the caller: a
  lib-only migration retires the lib, so the lib name is the needle; a var
  migration retires one qualified var and leaves the lib and its other vars
  standing, so `lib/var` is the needle and a string naming the surviving lib is
  not stale work. Var mode used to pass no needle at all — the count was a
  literal `[]` — so every var migration published `string_mentions: 0`
  whatever the tree held.

  Sites rather than files: a file is where the caller must go, a line is where
  they must look, and two mentions in one file are two edits. Sorted, so the
  receipt is a function of the tree and not of read order."
  [needle sources]
  (let [quoted (str "\"" needle "\"")]
    (vec (sort (mapcat (fn [{:keys [file source]}]
                         (keep-indexed
                           (fn [index line]
                             (when (str/includes? line quoted)
                               (str file ":" (inc index))))
                           (str/split-lines source)))
                       sources)))))

;; @spec MCP-OP-ALIAS-022
;; @spec MCP-OP-ALIAS-023
(defn- lib-rename-plan
  "Plan the move of the defining namespace, or a typed refusal.

  Returns nil when the defining file is not in scope, so a migration whose old
  lib lives outside the workspace still rewrites every caller."
  [request sources]
  (let [from-lib (get-in request [:from :lib])
        to-lib (get-in request [:to :lib])
        declared (keep (fn [{:keys [file source]}]
                         (when-let [declared (try
                                               (ns-declared-name
                                                 (parser/parse-string-all source))
                                               (catch Exception _ nil))]
                           {:file file :source source :ns declared}))
                       sources)
        definer (first (filter #(= from-lib (:ns %)) declared))
        existing (first (filter #(= to-lib (:ns %)) declared))]
    (cond
      (and existing definer)
      {:refusal (refusal :alias-migration-target-lib-exists
                         (str to-lib " is already defined in " (:file existing)
                              " while " from-lib " is still defined in "
                              (:file definer))
                         {:file (:file existing)
                          :from_lib from-lib
                          :to_lib to-lib
                          :defining_file (:file definer)}
                         (assoc-in (base-call request) ["to" "lib"]
                                   (str to-lib "-2")))}

      (nil? definer) nil

      :else
      (let [extension (subs (:file definer) (str/last-index-of (:file definer) "."))
            prefix (subs (:file definer) 0
                         (- (count (:file definer))
                            (count (lib-path from-lib extension))))
            new-file (str prefix (lib-path to-lib extension))]
        {:from from-lib
         :to to-lib
         :file (:file definer)
         :new-file new-file
         :content (rename-defining-ns (:source definer) to-lib)}))))
;; ---------------------------------------------------------------------------
;; public entrance

;; @spec MCP-OP-ALIAS-003
;; @spec MCP-OP-ALIAS-004
;; @spec MCP-OP-ALIAS-005
;; @spec MCP-OP-ALIAS-006
;; @spec MCP-OP-ALIAS-007
;; @spec MCP-OP-ALIAS-008
;; @spec MCP-OP-ALIAS-009
;; @spec MCP-OP-ALIAS-010
;; @spec MCP-OP-ALIAS-011
;; @spec MCP-OP-ALIAS-012
;; @spec MCP-OP-ALIAS-013
;; @spec MCP-OP-ALIAS-014
;; @spec MCP-OP-ALIAS-015
;; @spec MCP-OP-ALIAS-040
;; @spec MCP-OP-ALIAS-021
;; @spec MCP-OP-ALIAS-022
;; @spec MCP-OP-ALIAS-023
;; @spec MCP-OP-ALIAS-024
;; @spec MCP-OP-ALIAS-025
;; @spec MCP-OP-ALIAS-026
(defn plan
  "Return one complete migration plan or one typed refusal.

  request: {:workspace-root s
            :from {:lib s :var s-or-nil}
            :to {:lib s :var s-or-nil :alias-policy [s] :refer-policy s}
            :scope {:paths [s]}
            :expect {:files n}}
  sources: ordered vector of {:file relative-path :source text}

  A nil :var on both sides is a lib-only migration: every var of from.lib
  moves, under every spelling, and the defining namespace is renamed too."
  [request sources]
  (let [from-var (get-in request [:from :var])
        to-var (get-in request [:to :var])]
    (if (not= (nil? from-var) (nil? to-var))
      (refusal :alias-migration-mixed-var-spec
               (str "from.var and to.var must both name a var or both be null; "
                    "got from.var=" (pr-str from-var) " to.var=" (pr-str to-var))
               {:from_var from-var :to_var to-var}
               (-> (base-call request)
                   (assoc-in ["from" "var"] nil)
                   (assoc-in ["to" "var"] nil)))
      ;; only a lib-only migration moves the defining namespace; a var
      ;; migration expects the target lib to exist already
      (let [renamed (when (nil? from-var) (lib-rename-plan request sources))]
        (if (:refusal renamed)
          (:refusal renamed)
          (let [results
                (reduce
                  (fn [state {:keys [file source]}]
                    (if (:refusal state)
                      state
                      (let [result (try
                                     (file-plan request file source)
                                     (catch Exception error
                                       {:refusal
                                        (refusal :alias-migration-indirect-reference
                                                 (str "Cannot analyze " file ": "
                                                      (.getMessage error))
                                                 {:file file :form "<unparsed>"}
                                                 (excluding-call request file))}))]
                        (cond
                          (nil? result) state
                          (:refusal result) (assoc state :refusal (:refusal result))
                          :else (update state :files conj result)))))
                  {:files []}
                  sources)]
            (cond
              (:refusal results) (:refusal results)

              (empty? (:files results))
              (refusal :alias-migration-empty-scope
                       (str "No namespace under scope requires "
                            (get-in request [:from :lib]))
                       {:found_files 0
                        :scanned_files (count sources)
                        :expected_files (get-in request [:expect :files])}
                       (assoc-in (base-call request) ["scope" "paths"]
                                 ["src/**" "test/**"]))

              (and (get-in request [:expect :files])
                   (not= (get-in request [:expect :files]) (count (:files results))))
              (refusal :alias-migration-expect-mismatch
                       (str "Discovery found " (count (:files results))
                            " requiring namespaces; expect.files declared "
                            (get-in request [:expect :files]))
                       {:found_files (count (:files results))
                        :expected_files (get-in request [:expect :files])}
                       (assoc-in (base-call request)
                                 ["expect" "files"] (count (:files results))))

              :else
              (let [files (:files results)]
                (cond-> {:ok true
                         :files files
                         :totals {:files (count files)
                                  :sites (reduce + 0 (map :sites files))
                                  :refer-sites (reduce + 0 (map :refer-sites files))
                                  :alias-histogram (into (sorted-map)
                                                         (frequencies (map :alias files)))
                                  :collisions-resolved
                                  (reduce + 0 (map #(count (:collided %)) files))
                                  ;; @spec MCP-OP-ALIAS-034
                                  ;; the needle follows what this migration
                                  ;; retires: the lib in lib mode, the one
                                  ;; qualified var in var mode
                                  :string-mentions
                                  (string-mentions
                                    (if (nil? from-var)
                                      (get-in request [:from :lib])
                                      (str (get-in request [:from :lib])
                                           "/" from-var))
                                    sources)}}
                  renamed (assoc :lib-rename renamed))))))))))
