(ns clj-surgeon.extract-rewire
  "Pure caller rewiring for one extraction: qualify the source's remaining call
   sites and repoint every proved caller file at the new namespace.

   Every function here takes source text plus data and returns source text plus
   data. Nothing in this namespace touches a file, a process, or a clock, so the
   extraction executor owns every effect and this engine stays replayable.

   Two properties are load bearing:

   * Scoping. `qualify-owner-call-sites` rewrites only inside the top-level
     forms the planner proved reference a moved Var. A same-named symbol
     anywhere else in the file is left alone.
   * Byte preservation. Every edit is a single rewrite-clj node replacement on a
     zipper, so nothing outside the replaced token is re-indented, re-wrapped,
     or re-printed."
  (:require
   [clj-surgeon.cljc.require-ops :as require-ops]
   [clojure.string :as str]
   [rewrite-clj.node :as n]
   [rewrite-clj.zip :as z]))

;; ============================================================
;; Parsing helpers
;; ============================================================

(defn- parse
  "Zipper on the first top-level form, or nil when the source will not parse."
  [source]
  (try
    (z/of-string source)
    (catch Exception _ nil)))

(defn- top-level-zlocs [zloc]
  (take-while some? (iterate z/right zloc)))

(defn- strip-meta
  "Step past ^meta wrappers to the value they decorate."
  [zloc]
  (loop [zl zloc]
    (if (and zl (= :meta (z/tag zl)))
      (recur (some-> zl z/down z/rightmost))
      zl)))

(defn- top-level-name
  "The name a top-level (def... name ...) form defines, as a string, or nil."
  [zloc]
  (when (= :list (z/tag zloc))
    (let [head (z/down zloc)]
      (when (and head
                 (= :token (z/tag head))
                 (str/starts-with? (z/string head) "def"))
        (some-> head z/right strip-meta z/string)))))

(defn- ns-form-zloc? [zloc]
  (and (= :list (z/tag zloc))
       (= "ns" (some-> zloc z/down z/string))))

(defn- form-children
  "Significant (non-whitespace, non-comment) children of a node."
  [node]
  (if (n/inner? node)
    (remove n/whitespace-or-comment? (n/children node))
    []))

(defn- node-head
  "Unqualified head symbol of a list node, as a string, or nil."
  [node]
  (when (= :list (n/tag node))
    (let [c (first (form-children node))]
      (when (and c (= :token (n/tag c)))
        (let [s (n/string c)
              i (str/index-of s "/")]
          (if (and i (pos? i)) (subs s (inc i)) s))))))

;; ============================================================
;; Locals introduced inside an owner — the fail-closed shadow check
;; ============================================================

(def ^:private pair-binding-heads
  "Heads whose binding vector is a strict target/value pair sequence."
  #{"let" "let*" "loop" "loop*" "binding" "with-open" "with-local-vars"
    "with-redefs" "if-let" "when-let" "if-some" "when-some" "when-first"
    "dotimes"})

(def ^:private seq-binding-heads
  "Heads whose binding vector is pairs plus :let / :when / :while modifiers."
  #{"for" "doseq"})

(def ^:private fn-heads
  #{"fn" "fn*" "defn" "defn-" "defmacro" "defmethod"})

(def ^:private named-fn-heads
  #{"defn" "defn-" "defmacro" "defmethod"})

(defn- symbol-token?
  "True for a token node that reads as a plain symbol rather than a keyword,
   string, number, or character literal."
  [node]
  (and (= :token (n/tag node))
       (let [s (n/string node)]
         (and (seq s)
              (not (str/starts-with? s ":"))
              (not (str/starts-with? s "\""))
              (not (str/starts-with? s "\\"))
              (not (contains? #{\0 \1 \2 \3 \4 \5 \6 \7 \8 \9} (first s)))))))

(defn- symbol-strings
  "Every plain-symbol string appearing anywhere in a subtree."
  [node]
  (if (n/inner? node)
    (into #{} (mapcat symbol-strings) (form-children node))
    (if (symbol-token? node) #{(n/string node)} #{})))

(defn- pair-targets
  "Even-index elements of a binding vector: the targets, never the values."
  [vec-node]
  (->> (form-children vec-node)
       (partition-all 2)
       (keep first)))

(defn- seq-targets
  "Binding targets of a for/doseq vector, honoring :let / :when / :while."
  [vec-node]
  (mapcat (fn [[target value]]
            (let [s (and target (n/string target))]
              (cond
                (= ":let" s) (when (and value (= :vector (n/tag value)))
                               (pair-targets value))
                (and s (str/starts-with? s ":")) nil
                target [target]
                :else nil)))
          (partition-all 2 (form-children vec-node))))

(defn- first-binding-vector [kids]
  (first (filter #(= :vector (n/tag %)) (rest kids))))

(defn- fn-param-vectors
  "Parameter vectors of a fn-like form, single arity or multi arity."
  [head kids]
  (let [after (cond-> (rest kids)
                (contains? named-fn-heads head) rest)
        after (drop-while #(not (contains? #{:vector :list} (n/tag %))) after)]
    (cond
      (empty? after) []
      (= :vector (n/tag (first after))) [(first after)]
      :else (keep (fn [nd]
                    (when (= :list (n/tag nd))
                      (first (filter #(= :vector (n/tag %)) (form-children nd)))))
                  after))))

(defn- introduced-locals
  "Every local name introduced anywhere inside `node`.

   Deliberately over-inclusive on destructuring: an extra name only ever turns
   a rewrite into a refusal, never into a wrong rewrite."
  [node]
  (let [head (node-head node)
        kids (form-children node)
        own (cond
              (contains? pair-binding-heads head)
              (when-let [bv (first-binding-vector kids)]
                (mapcat symbol-strings (pair-targets bv)))

              (contains? seq-binding-heads head)
              (when-let [bv (first-binding-vector kids)]
                (mapcat symbol-strings (seq-targets bv)))

              (contains? fn-heads head)
              (mapcat symbol-strings (fn-param-vectors head kids)))]
    (into (set own)
          (mapcat introduced-locals)
          (filter n/inner? kids))))

;; ============================================================
;; Token rewriting
;; ============================================================

(defn- replace-tokens
  "Replace every token whose string is a key of `renames` with that key's value,
   inside the subtree at `zloc`. Returns [zloc counts], counts keyed by the
   original token string. Keywords, strings, and comments never match because
   their node strings carry their own leading punctuation."
  [zloc renames]
  (let [counts (volatile! {})
        out (z/prewalk
              zloc
              (fn [c] (and (= :token (z/tag c)) (contains? renames (z/string c))))
              (fn [c]
                (let [s (z/string c)]
                  (vswap! counts update s (fnil inc 0))
                  (z/replace c (n/token-node (symbol (get renames s)))))))]
    [out @counts]))

;; ============================================================
;; qualify-owner-call-sites
;; ============================================================

(defn- rewire-one-owner
  "Qualify `vars` inside every top-level form named `owner`."
  [src owner vars alias]
  (if-not (seq vars)
    {:ok true :source src :rewrites [] :unmatched []}
    (if-let [zloc (parse src)]
      (let [renames (into {} (map (fn [v] [v (str alias "/" v)])) vars)
            var-set (set vars)]
        (loop [zl zloc, prev nil, found? false, counts {}]
          (cond
            (nil? zl)
            (if-not found?
              {:ok false
               :error-type :unknown-rewire-owner
               :owner owner
               :error (str "No top-level form named " owner " in the source")}
              {:ok true
               :source (if prev (z/root-string prev) src)
               :rewrites (mapv (fn [v] {:owner owner :var v :count (get counts v 0)})
                               vars)
               :unmatched (vec (for [v vars :when (zero? (get counts v 0))]
                                 {:owner owner :var v}))})

            (= owner (top-level-name zl))
            (if-let [shadow (first (filter var-set
                                           (sort (introduced-locals (z/node zl)))))]
              {:ok false
               :error-type :shadowed-moved-var
               :owner owner
               :var shadow
               :error (str "Owner " owner " introduces a local named " shadow
                           ", which shadows a moved Var; refusing rather than "
                           "guessing which occurrences are the Var")}
              (let [[zl' c] (replace-tokens zl renames)]
                (recur (z/right zl') zl' true (merge-with + counts c))))

            :else (recur (z/right zl) zl found? counts))))
      {:ok false
       :error-type :unparseable-source
       :owner owner
       :error "Source does not parse; refusing to rewire"})))

;; @spec MCP-OP-EXTRACT-006
(defn qualify-owner-call-sites
  "Rewrite unqualified references to moved Vars inside exactly the named owner
   forms of one source.

   source     - complete source text of the file the forms were extracted FROM
   owners     - a vector of maps {:owner \"run-check!\" :moved-vars [\"run-process!\" ...]}
                (this is the planner's :remaining-source-callers, verbatim)
   alias      - the target alias string, e.g. \"exact-verify\"

   Returns
     {:ok true
      :source    <new source, byte-identical outside the replaced tokens>
      :rewrites  [{:owner \"run-check!\" :var \"run-process!\" :count 1} ...]
      :unmatched [{:owner \"run-check!\" :var \"expand-command\"} ...]}

   :rewrites carries one entry per declared (owner, var) pair, including zero
   counts; :unmatched repeats exactly the zero-count pairs so a caller can see
   that the planner and the rewriter disagreed.

   Typed refusals, all {:ok false :error-type ... :error <message>}:
     :invalid-rewire-source  source was not a string
     :invalid-rewire-alias   alias was not a non-blank string
     :invalid-rewire-owners  owners was not a sequence of maps
     :unparseable-source     source does not parse (also carries :owner)
     :unknown-rewire-owner   an owner is not defined in the source (:owner)
     :shadowed-moved-var     an owner introduces a local with a moved Var's
                             name (:owner, :var) — fail closed rather than
                             guess which occurrences are the Var
   No refusal carries :source; nothing is partially applied."
  [source owners alias]
  (cond
    (not (string? source))
    {:ok false :error-type :invalid-rewire-source
     :error "source must be a string"}

    (or (not (string? alias)) (str/blank? alias))
    {:ok false :error-type :invalid-rewire-alias
     :error "alias must be a non-blank string"}

    (not (and (sequential? owners) (every? map? owners)))
    {:ok false :error-type :invalid-rewire-owners
     :error "owners must be a sequence of {:owner .. :moved-vars [..]} maps"}

    :else
    (loop [pending (seq owners), src source, rewrites [], unmatched []]
      (if-not pending
        {:ok true :source src :rewrites rewrites :unmatched unmatched}
        (let [{:keys [owner moved-vars]} (first pending)
              vars (vec (distinct (map str moved-vars)))
              step (rewire-one-owner src (str owner) vars alias)]
          (if-not (:ok step)
            step
            (recur (next pending)
                   (:source step)
                   (into rewrites (:rewrites step))
                   (into unmatched (:unmatched step)))))))))

;; ============================================================
;; requalify-caller
;; ============================================================

(defn- require-clause-zloc [ns-zl]
  (->> (some-> ns-zl z/down)
       top-level-zlocs
       (filter #(and (= :list (z/tag %))
                     (= ":require" (some-> % z/down z/string))))
       first))

(defn- libspec-zlocs [req-zl]
  (->> (some-> req-zl z/down)
       top-level-zlocs
       (filter #(= :vector (z/tag %)))
       vec))

(defn- libspec-ns-string [v-zl]
  (some-> v-zl z/down z/string))

(defn- libspec-alias-string
  "The :as alias of a libspec vector, as a string, or nil."
  [v-zl]
  (loop [c (some-> v-zl z/down)]
    (cond
      (nil? c) nil
      (= ":as" (z/string c)) (some-> c z/right z/string)
      :else (recur (z/right c)))))

(defn- plain-as-libspec?
  "True when the libspec is exactly [lib :as alias] with nothing else."
  [v-zl]
  (let [parts (mapv z/string (top-level-zlocs (z/down v-zl)))]
    (and (= 3 (count parts)) (= ":as" (nth parts 1)))))

(defn- references-alias?
  "True when any token outside the ns form still refers to alias `a` — as a
   `a/...` symbol, a `::a/...` auto-resolved keyword, or a bare `a` symbol.
   Comments and strings are not references."
  [src a]
  (let [prefix (str a "/")
        kw-prefix (str "::" a "/")]
    (boolean
      (when-let [zloc (parse src)]
        (some (fn [top]
                (when-not (ns-form-zloc? top)
                  (some (fn [nd]
                          (and (= :token (n/tag nd))
                               (let [s (n/string nd)]
                                 (or (= s a)
                                     (str/starts-with? s prefix)
                                     (str/starts-with? s kw-prefix)))))
                        (tree-seq n/inner? n/children (z/node top)))))
              (top-level-zlocs zloc))))))

(defn- as-libspec-node [target-ns alias]
  (n/vector-node
    [(n/token-node (symbol (str target-ns)))
     (n/spaces 1)
     (n/keyword-node :as)
     (n/spaces 1)
     (n/token-node (symbol (str alias)))]))

(defn- replace-libspec
  "Swap the [old-ns :as old-alias] vector for [target-ns :as alias] in place,
   touching no whitespace around it."
  [src old-ns target-ns alias]
  (let [zloc (parse src)
        ns-zl (first (filter ns-form-zloc? (top-level-zlocs zloc)))
        req-zl (require-clause-zloc ns-zl)
        target (first (filter #(= (str old-ns) (libspec-ns-string %))
                              (libspec-zlocs req-zl)))]
    (z/root-string (z/replace target (as-libspec-node target-ns alias)))))

;; @spec MCP-OP-EXTRACT-007
(defn requalify-caller
  "Rewrite <old-alias>/<var> to <alias>/<var> across one caller file, for
   exactly the named moved vars, and add or replace the target require.

   Takes one map:
     :source     complete caller source text
     :old-alias  alias the caller currently uses for the source namespace
     :old-ns     the source namespace symbol/string
     :target-ns  the extracted namespace symbol/string
     :alias      alias to bind the extracted namespace to
     :moved-vars vector of moved Var name strings

   Returns
     {:ok true
      :source          <new source, byte-identical outside the edits>
      :rewrites        <integer count of rewritten call sites>
      :rewrites-by-var {\"run-process!\" 1 ...}   ;; extra, non-contractual detail
      :require-action  :added | :replaced | :unchanged}

   :replaced   the caller's only references to old-alias were moved Vars, so its
               [old-ns :as old-alias] libspec became [target-ns :as alias]
               in place.
   :added      [target-ns :as alias] was inserted in alphabetical position and
               the old libspec was left alone.
   :unchanged  [target-ns :as alias] was already present.

   The inserted or substituted libspec is always bare [target-ns :as alias] and
   never carries :refer. Libspecs that are not [lib :as alias] — a
   [clojure.test :refer [...]] entry, for instance — neither block the
   operation nor change.

   Typed refusals, all {:ok false :error-type ... :error <message>}:
     :invalid-rewire-source   :source missing or not a string
     :invalid-rewire-alias    :alias missing or blank
     :invalid-rewire-target   :target-ns missing or blank
     :unparseable-source      the caller source does not parse
     :no-ns-form              no top-level ns form
     :multiple-ns-forms       more than one top-level ns form (:count)
     :no-require-clause       an ns form with no (:require ...) clause, when a
                              require had to be added or replaced
     :alias-collision         :alias is already bound to a different namespace
                              (:existing-ns)
     :conflicting-target-alias  target-ns is already required under a different
                              alias (:existing-alias)
     :unsupported-old-libspec the old libspec would have to be replaced but is
                              not exactly [old-ns :as old-alias] (:libspec)
   No refusal carries :source; nothing is partially applied."
  [{:keys [source old-alias old-ns target-ns alias moved-vars]}]
  (cond
    (not (string? source))
    {:ok false :error-type :invalid-rewire-source
     :error "source must be a string"}

    (or (not (string? alias)) (str/blank? alias))
    {:ok false :error-type :invalid-rewire-alias
     :error "alias must be a non-blank string"}

    (or (nil? target-ns) (str/blank? (str target-ns)))
    {:ok false :error-type :invalid-rewire-target
     :error "target-ns must be a non-blank namespace"}

    :else
    (if-let [zloc (parse source)]
      (let [ns-zlocs (filter ns-form-zloc? (top-level-zlocs zloc))]
        (cond
          (empty? ns-zlocs)
          {:ok false :error-type :no-ns-form
           :error "Caller file has no top-level ns form"}

          (< 1 (count ns-zlocs))
          {:ok false :error-type :multiple-ns-forms
           :count (count ns-zlocs)
           :error "Caller file has more than one top-level ns form"}

          :else
          (let [vars (vec (distinct (map str moved-vars)))
                renames (into {} (map (fn [v] [(str old-alias "/" v)
                                               (str alias "/" v)]))
                              vars)
                [zl' counts] (if (seq renames)
                               (replace-tokens (z/of-string* source) renames)
                               [nil {}])
                rewritten (if zl' (z/root-string zl') source)
                total (reduce + 0 (vals counts))
                by-var (into {} (map (fn [[k v]]
                                       [(subs k (inc (count (str old-alias)))) v]))
                             counts)
                ;; re-read the ns structure from the rewritten source
                zl2 (parse rewritten)
                ns-zl (first (filter ns-form-zloc? (top-level-zlocs zl2)))
                req-zl (require-clause-zloc ns-zl)
                libspecs (libspec-zlocs req-zl)
                target-entry (first (filter #(= (str target-ns) (libspec-ns-string %))
                                            libspecs))
                alias-holder (first (filter #(= (str alias) (libspec-alias-string %))
                                            libspecs))
                old-entry (first (filter #(= (str old-ns) (libspec-ns-string %))
                                         libspecs))
                done (fn [src action]
                       {:ok true :source src :rewrites total
                        :rewrites-by-var by-var :require-action action})]
            (cond
              target-entry
              (if (= (str alias) (libspec-alias-string target-entry))
                (done rewritten :unchanged)
                {:ok false :error-type :conflicting-target-alias
                 :existing-alias (libspec-alias-string target-entry)
                 :error (str target-ns " is already required under alias "
                             (libspec-alias-string target-entry))})

              (and alias-holder (not= (str target-ns) (libspec-ns-string alias-holder)))
              {:ok false :error-type :alias-collision
               :existing-ns (libspec-ns-string alias-holder)
               :error (str "Alias " alias " is already bound to "
                           (libspec-ns-string alias-holder))}

              (nil? req-zl)
              {:ok false :error-type :no-require-clause
               :error "Caller ns form has no (:require ...) clause"}

              (not (references-alias? rewritten (str old-alias)))
              (cond
                (nil? old-entry)
                (done (require-ops/insert-into-require-sorted
                        rewritten (symbol (str target-ns)) (str alias) nil)
                      :added)

                (plain-as-libspec? old-entry)
                (done (replace-libspec rewritten old-ns target-ns alias) :replaced)

                :else
                {:ok false :error-type :unsupported-old-libspec
                 :libspec (z/string old-entry)
                 :error (str "Refusing to replace a libspec that is not exactly "
                             "[" old-ns " :as " old-alias "]")})

              :else
              (done (require-ops/insert-into-require-sorted
                      rewritten (symbol (str target-ns)) (str alias) nil)
                    :added)))))
      {:ok false :error-type :unparseable-source
       :error "Caller source does not parse; refusing to rewire"})))
