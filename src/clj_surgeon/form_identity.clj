(ns clj-surgeon.form-identity
  "Pure form-identity delta between two images of one Clojure source file.

  A text differ sees hunks. This namespace sees owners: which top-level
  defining forms actually changed, which bytes moved with no structural reason
  at all, which protected nodes were destroyed, and which hazards a
  line-oriented patcher is structurally unable to notice.

  Form classification comes from `clj-surgeon.forms`, the repository's single
  source of truth for what a defining form is, so the gate cannot drift away
  from the rest of the kernel."
  (:require
   [clj-surgeon.forms :as forms]
   [clojure.string :as str]
   [rewrite-clj.node :as node]
   [rewrite-clj.parser :as parser]))

(def opaque-string-threshold 200)

(def ^:private uncounted-kinds
  "Kinds that name a Var without defining it, or that legitimately repeat.

  `declare` is exempt from being counted as a definition and nothing more: it
  must never license a second real definition of the same symbol. `defmethod`
  repeats by design. `ns` is an owner for reporting, not a Var definition."
  #{:declare :defmethod :ns})

(def ^:private protected-classes
  [:comment :metadata :reader-conditional :discard])

;; ---------------------------------------------------------------------------
;; Node helpers
;; ---------------------------------------------------------------------------

(defn- significant-children
  [n]
  (remove node/whitespace-or-comment? (node/children n)))

(defn- unwrap-meta
  [n]
  (if (= :meta (node/tag n))
    (recur (last (significant-children n)))
    n))

(defn- reader-conditional-node?
  [n]
  (and (= :reader-macro (node/tag n))
       (contains? #{"?" "?@"} (some-> (first (node/children n)) node/string))))

(defn- reader-conditional-branches
  "Branch forms of a reader conditional, paired with their platform keys.

  The platform matters: a symbol defined once under `:clj` and once under
  `:cljs` is defined once for any reader, however many `#?` forms those
  branches are spread across."
  [n]
  (let [body (second (significant-children n))]
    (when (and body (node/inner? body))
      (loop [remaining (significant-children body)
             platform nil
             branches []]
        (if-let [item (first remaining)]
          (let [text (node/string item)]
            (if (str/starts-with? text ":")
              (recur (next remaining) text branches)
              (recur (next remaining) platform
                     (conj branches [(or platform ":unknown") item]))))
          branches)))))

(defn- string-node?
  [n]
  (and (contains? #{:token :multi-line} (node/tag n))
       (str/starts-with? (node/string n) "\"")))

(defn- quoted-form
  "The form a `(quote x)` or `'x` node carries, or nil."
  [n]
  (cond
    (and (= :quote (node/tag n)) (seq (significant-children n)))
    (first (significant-children n))

    (and (= :list (node/tag n))
         (= "quote" (some-> (first (significant-children n)) node/string)))
    (second (significant-children n))))

(defn- intern-definition
  "`(intern ns 'sym value)` binds sym, without naming a def-family form."
  [children]
  (when (= "intern" (some-> (first children) node/string))
    (when-let [subject (some-> (nth children 2 nil) quoted-form node/string)]
      {:name subject :form-kind :intern})))

(declare definitions)

(defn- branch-definitions
  "Definitions inside a reader conditional, each tagged with its platform.

  Nothing is collapsed here. Whether two definitions of one symbol are one
  binding or two is a question about the whole file, not about one `#?` form:
  `#?(:clj (defn parse ..))` beside `#?(:cljs (defn parse ..))` is one, and so
  is a single form carrying both branches, while two `:clj` definitions are
  two wherever they are written. The platform tag is what lets that judgement
  be made once, over every conditional in the image."
  [n path]
  (vec (mapcat (fn [[platform branch]]
                 (map #(assoc % :platform platform)
                      (definitions branch (conj path (str "?" platform)))))
               (reader-conditional-branches n))))

(defn definitions
  "Every Var definition a node introduces, at any depth, in source order.

  A patch can hide a definition inside any wrapper a reader will still
  evaluate -- `when`, `let`, `binding`, `try`, `if`, `do`, a reader
  conditional, metadata, `(eval '(defn ...))`, or `intern` -- and a walk that
  only knew about `do` walked past all of them. The walk therefore descends
  every list, recording the wrapper path so a receipt can say where the
  definition was found.

  It deliberately does not descend `#_` or `(comment ...)`, whose contents are
  read and discarded, and it does not descend a bare quoted form, which is
  data rather than code. `eval` is the exception that proves that rule: its
  quoted argument is executed, so it is walked."
  ([n] (definitions n []))
  ([n path]
   (let [tag (node/tag n)]
     (cond
       (node/whitespace-or-comment? n) []
       (= :uneval tag) []
       (= :meta tag) (definitions (unwrap-meta n) (conj path "^"))
       (reader-conditional-node? n) (branch-definitions n path)

       (= :list tag)
       (let [children (significant-children n)
             head (some-> (first children) node/string)
             subject (some-> (second children) unwrap-meta node/string)]
         (cond
           (= "comment" head) []

           (and (= "ns" head) subject) [{:name subject :form-kind :ns
                                         :wrapper-path path}]

           (and head (forms/defining-form? head) subject)
           (into [{:name subject :form-kind (forms/classify head)
                   :wrapper-path path}]
                 ;; A defining form may still contain another definition in
                 ;; its body; `(defn outer [] (defn inner [] 1))` binds two.
                 (mapcat #(definitions % (conj path head))
                         (rest (rest children))))

           (intern-definition children)
           [(assoc (intern-definition children) :wrapper-path path)]

           (= "eval" head)
           (vec (mapcat (fn [child]
                          (if-let [quoted (quoted-form child)]
                            (definitions quoted (conj path "eval" "quote"))
                            (definitions child (conj path "eval"))))
                        (rest children)))

           :else
           (vec (mapcat #(definitions % (conj path (or head "()")))
                        (rest children)))))

       (contains? #{:vector :map :set :forms} tag)
       (vec (mapcat #(definitions % (conj path (name tag)))
                    (significant-children n)))

       :else []))))

(defn owner-identity
  "The identity a top-level node is aligned by, or nil when it has none.

  Only an unwrapped definition names the unit. A `(when true (defn f ...))`
  contributes a definition to the duplicate detector but is not itself the
  owner `f`, and calling it one would make an ordinary form vanish from the
  alignment the moment somebody wrapped it."
  [n]
  (first (filter #(empty? (:wrapper-path %)) (definitions n))))

(defn code-shape
  "Structural shape of a node with whitespace, comments, metadata, and reader
  discards removed. Two nodes with equal shapes differ only in presentation or
  in protected nodes."
  [n]
  (cond
    (node/whitespace-or-comment? n) nil
    (= :uneval (node/tag n)) nil
    (= :meta (node/tag n)) (code-shape (unwrap-meta n))
    (node/inner? n) [(node/tag n) (vec (keep code-shape (node/children n)))]
    :else [(node/tag n) (node/string n)]))

(defn- collect-protected
  "Per-class vectors of protected-node source text, in document order."
  [n]
  (let [acc (volatile! {:comment [] :metadata []
                        :reader-conditional [] :discard []})]
    (letfn [(walk [x]
              (cond
                (= :comment (node/tag x)) (vswap! acc update :comment conj
                                                  (node/string x))
                (= :uneval (node/tag x)) (vswap! acc update :discard conj
                                                 (node/string x))
                (= :meta (node/tag x))
                (vswap! acc update :metadata conj
                        (some-> (first (significant-children x)) node/string))
                (reader-conditional-node? x)
                (vswap! acc update :reader-conditional conj (node/string x)))
              (when (node/inner? x)
                (run! walk (node/children x))))]
      (walk n))
    @acc))

;; ---------------------------------------------------------------------------
;; Line index
;; ---------------------------------------------------------------------------

(defn newline-index
  "Sorted character offsets of every newline, for O(log n) line lookup."
  [source]
  (let [length (count source)]
    (loop [i 0 offsets (transient [])]
      (if (= i length)
        (persistent! offsets)
        (recur (inc i)
               (if (= \newline (.charAt ^String source i))
                 (conj! offsets i)
                 offsets))))))

(defn line-of
  "1-based line number of a character offset, by binary search.

  A linear scan here is the difference between a linear and a quadratic
  delta: the index is consulted once per unit and once per nested node, so
  its cost multiplies by the size of the file it is indexing."
  [offsets offset]
  (let [target (long offset)]
    (loop [low 0
           high (count offsets)]
      (if (< low high)
        (let [middle (quot (+ low high) 2)]
          (if (< (long (nth offsets middle)) target)
            (recur (inc middle) high)
            (recur low middle)))
        (inc low)))))

;; ---------------------------------------------------------------------------
;; Unit decomposition
;; ---------------------------------------------------------------------------

(defn decompose
  "Split one image into ordered units: owners, anonymous forms, and gaps.

  Each unit carries its exact source, character offsets, line span, and every
  definition it introduces. Joining every unit's source in order reproduces
  the image byte for byte."
  [source]
  (let [root (parser/parse-string-all source)
        offsets (newline-index source)]
    (loop [children (node/children root)
           offset 0
           anonymous 0
           units []]
      (if-let [n (first children)]
        (let [text (node/string n)
              end (+ offset (count text))
              gap? (node/whitespace-or-comment? n)]
          (if gap?
            ;; Merge consecutive whitespace and comment nodes into one gap.
            (let [previous (peek units)]
              (if (= :gap (:kind previous))
                (recur (next children) end anonymous
                       (conj (pop units)
                             (assoc previous
                                    :source (str (:source previous) text)
                                    :end-offset end
                                    :end-line (line-of offsets (max 0 (dec end))))))
                (recur (next children) end anonymous
                       (conj units {:kind :gap
                                    :source text
                                    :start-offset offset
                                    :end-offset end
                                    :start-line (line-of offsets offset)
                                    :end-line (line-of offsets (max 0 (dec end)))}))))
            (let [found (definitions n)
                  owner (first found)]
              (recur (next children) end
                     (if owner anonymous (inc anonymous))
                     (conj units
                           {:kind :form
                            :node n
                            :definitions found
                            :name (or (:name owner) (str "form#" anonymous))
                            :form-kind (or (:form-kind owner) :anonymous)
                            :align-key (str (name (or (:form-kind owner)
                                                      :anonymous))
                                            "/"
                                            (or (:name owner)
                                                (str "form#" anonymous)))
                            :source text
                            :start-offset offset
                            :end-offset end
                            :start-line (line-of offsets offset)
                            :end-line (line-of offsets (max 0 (dec end)))})))))
        {:units units :offsets offsets}))))

(defn- unit-key
  [unit]
  (when-not (= :gap (:kind unit))
    (:align-key unit)))

(defn- keyed-units
  "Index form units by kind and name.

  The key carries the defining kind so an idiomatic `(declare x)` beside its
  `(defn x)` still aligns; only two units of the same kind and name are
  genuinely unalignable, and those are reported by the duplicate detector
  rather than guessed at."
  [units]
  (let [forms (filterv #(= :form (:kind %)) units)
        by-name (group-by :align-key forms)]
    {:unique (into {} (keep (fn [[name matches]]
                              (when (= 1 (count matches))
                                [name (first matches)]))
                            by-name))
     :duplicates (into {} (filter (fn [[_ matches]] (< 1 (count matches)))
                                  by-name))}))


;; @spec MCP-OP-ADMIT-090
(defn effective-count
  "How many times one symbol is actually bound for a single reader.

  Unconditional definitions all bind. Conditional ones bind only when their
  platform is the reader's, so the conditional contribution is the largest
  count any one platform carries, not the total across platforms. This is what
  separates legal `.cljc` -- one definition per platform, spread over as many
  `#?` forms as the author likes -- from a real duplicate, which is two
  definitions a single reader would both evaluate."
  [definitions]
  (let [{unconditional nil :as by-platform} (group-by :platform definitions)
        conditional (dissoc by-platform nil)]
    (+ (count unconditional)
       (if (seq conditional)
         (apply max (map count (vals conditional)))
         0))))

(defn- counted-definitions
  "Every countable definition in one image, grouped by symbol.

  A definition is countable when it actually binds the symbol. `declare`,
  `defmethod`, and `ns` are excluded from the count; excluding them does not
  license anything else, which is the whole point."
  [units]
  (->> units
       (filter #(= :form (:kind %)))
       (mapcat (fn [unit]
                 (keep (fn [definition]
                         (when-not (contains? uncounted-kinds
                                              (:form-kind definition))
                           (assoc definition
                                  :start-line (:start-line unit)
                                  :end-line (:end-line unit))))
                       (:definitions unit))))
       (group-by :name)))

(defn- gap-pairs
  "Gaps keyed by the pair of form names that bracket them."
  [units]
  (loop [remaining units
         previous nil
         pairs {}]
    (if-let [unit (first remaining)]
      (if (= :gap (:kind unit))
        (let [next-name (some-> (first (filter #(= :form (:kind %))
                                               (next remaining)))
                                :name)]
          (recur (next remaining) previous
                 (assoc pairs [previous next-name] unit)))
        (recur (next remaining) (unit-key unit) pairs))
      pairs)))

;; ---------------------------------------------------------------------------
;; Comparison
;; ---------------------------------------------------------------------------

(defn- diff-window
  [a b]
  (if (= a b)
    0
    (let [limit (min (count a) (count b))
          prefix (loop [i 0]
                   (if (and (< i limit) (= (.charAt ^String a i)
                                           (.charAt ^String b i)))
                     (recur (inc i))
                     i))
          suffix (loop [i 0]
                   (if (and (< i (- limit prefix))
                            (= (.charAt ^String a (- (count a) 1 i))
                               (.charAt ^String b (- (count b) 1 i))))
                     (recur (inc i))
                     i))]
      (- (max (count a) (count b)) prefix suffix))))

;; @spec MCP-OP-ADMIT-023
(defn- protected-delta
  "Per-class protected-node delta for one owner, or nil when nothing is owed.

  A class is reported when its nodes differ while the owner's code did not
  change, and always when its count decreased. An increase alongside a real
  code change is ordinary authorship and is not drift."
  [pre-protected post-protected code-changed?]
  (let [rows (keep (fn [class]
                     (let [before (get pre-protected class [])
                           after (get post-protected class [])
                           delta (- (count after) (count before))]
                       (when (and (not= before after)
                                  (or (not code-changed?) (neg? delta)))
                         [class {:pre-count (count before)
                                 :post-count (count after)
                                 :delta delta
                                 :text-changed (and (= (count before)
                                                       (count after))
                                                    (not= before after))}])))
                   protected-classes)]
    (when (seq rows) (into {} rows))))

;; @spec MCP-OP-ADMIT-030
(defn- hazard
  [type file owner span class message extra]
  (merge {:type type :file file :owner owner :span span
          :class class :message message}
         extra))

;; @spec MCP-OP-ADMIT-032
;; @spec MCP-OP-ADMIT-064
(defn- duplicate-hazards
  "Symbols the post image binds more than once, however they are wrapped."
  [file pre-counted post-counted]
  (keep (fn [[name matches]]
          (let [bindings (effective-count matches)]
            (when (< 1 bindings)
              (let [ordered (sort-by :start-line matches)
                    spans (vec (distinct (map (juxt :start-line :end-line)
                                              ordered)))]
                (hazard :duplicate-definition file name (first spans) :refusal
                        (str "Top-level symbol " name " is defined "
                             bindings " times for one reader")
                        {:spans spans
                         :platforms (vec (sort (distinct (keep :platform
                                                               matches))))
                         :kind (:form-kind (first ordered))
                         :introduced-by-patch
                         (< (effective-count (get pre-counted name))
                            bindings)
                         :lift {:description
                                (str "remove or rename all but one binding of "
                                     name)
                                :sites (mapv (fn [[start _]]
                                               {:file file :line start})
                                             spans)}})))))
        post-counted))

(defn- refer-symbols
  "Symbols one libspec refers, or :all."
  [children]
  (loop [remaining children
         refers nil]
    (if-let [item (first remaining)]
      (if (= ":refer" (node/string item))
        (let [value (second remaining)]
          (cond
            (nil? value) refers
            (= ":all" (node/string value)) :all
            (= :vector (node/tag value))
            (into (or refers #{}) (map node/string (significant-children value)))
            :else refers))
        (recur (next remaining) refers))
      refers)))

(defn- alias-symbols
  "Aliases one libspec binds, through `:as` or `:as-alias`.

  The post image no longer says what an alias meant -- the ns form that bound
  it is exactly what the patch removed -- so the binding has to be read from
  the pre-image or an alias-qualified call reads as an unrelated symbol."
  [children]
  (loop [remaining children
         aliases #{}]
    (if-let [item (first remaining)]
      (if (contains? #{":as" ":as-alias"} (node/string item))
        (let [value (second remaining)]
          (recur (if (next remaining) (nnext remaining) nil)
                 (cond-> aliases value (conj (node/string value)))))
        (recur (next remaining) aliases))
      aliases)))

(defn- libspec-entries
  "Every library one `:require` entry names, with the symbols it refers.

  A prefix list names one library per member: `[clojure [string :as str]
  [set :as set]]` is two requires, and reading only the entry's first symbol
  would call dropping one of them no change at all. A libspec wrapped in a
  reader conditional is present for that branch's platform, so its libraries
  count as required rather than as removed."
  [entry]
  (let [tag (node/tag entry)]
    (cond
      (reader-conditional-node? entry)
      (vec (mapcat (fn [[_platform branch]] (libspec-entries branch))
                   (reader-conditional-branches entry)))

      (= :vector tag)
      (let [children (significant-children entry)
            prefix (some-> (first children) node/string)
            rest-children (rest children)
            members (filter (fn [child]
                              (and (not (str/starts-with? (node/string child) ":"))
                                   (contains? #{:vector :token} (node/tag child))))
                            rest-children)
            prefix-list? (and prefix
                              (seq members)
                              (not-any? #(str/starts-with? (node/string %) ":")
                                        (take 1 rest-children)))]
        (cond
          (nil? prefix) []

          prefix-list?
          (vec (keep (fn [member]
                       (if (= :vector (node/tag member))
                         (let [inner (significant-children member)]
                           (when-let [leaf (some-> (first inner) node/string)]
                             {:lib (str prefix "." leaf)
                              :refers (refer-symbols (rest inner))
                              :aliases (alias-symbols (rest inner))}))
                         {:lib (str prefix "." (node/string member))
                          :refers nil
                          :aliases #{}}))
                     members))

          :else [{:lib prefix
                  :refers (refer-symbols rest-children)
                  :aliases (alias-symbols rest-children)}]))

      (= :token tag) [{:lib (node/string entry) :refers nil :aliases #{}}]
      :else [])))

;; @spec MCP-OP-ADMIT-114
(defn ns-require-bindings
  "Libraries an `ns` node requires, with the symbols and aliases each binds.

  Reading the node tree instead of evaluating `sexpr` keeps this working when
  the `ns` form carries a reader conditional, which is exactly the shape that
  silently disabled the check before."
  [n]
  (let [found (volatile! {})]
    (letfn [(walk [x]
              (when (node/inner? x)
                (when (and (= :list (node/tag x))
                           (contains? #{":require" ":require-macros"}
                                      (some-> (first (significant-children x))
                                              node/string)))
                  (doseq [entry (rest (significant-children x))
                          {:keys [lib refers aliases]} (libspec-entries entry)]
                    (vswap! found update lib
                            (fn [existing]
                              {:refers (let [seen (:refers existing)]
                                         (cond
                                           (= :all seen) :all
                                           (= :all refers) :all
                                           :else (into (or seen #{})
                                                       (or refers #{}))))
                               :aliases (into (or (:aliases existing) #{})
                                              (or aliases #{}))}))))
                (run! walk (node/children x))))]
      (walk n))
    @found))

;; @spec MCP-OP-ADMIT-033
;; @spec MCP-OP-ADMIT-065
;; @spec MCP-OP-ADMIT-083
(defn ns-requires
  "Libraries an `ns` node requires, mapped to the symbols each one refers.

  Reading the node tree instead of evaluating `sexpr` keeps this working when
  the `ns` form carries a reader conditional, which is exactly the shape that
  silently disabled the check before."
  [n]
  (into {} (map (fn [[lib binding]] [lib (:refers binding)]))
        (ns-require-bindings n)))

(def ^:private non-code-tags
  "Node tags whose interior the reader never evaluates."
  #{:uneval :comment})

;; @spec MCP-OP-ADMIT-114
(defn- token-sites
  "Every token in these units, with the line it sits on.

  Structural, not textual. Only `:token` nodes count, so a library named
  inside a string literal or a comment is not a use of it; `#_` discards are
  not descended into, because a discarded call is not a call. Reader
  conditional branches ARE descended into: a use that exists only on one
  platform is still a use, and refusing to look inside them is how a live
  reference would read as dead."
  [offsets units]
  (persistent!
    (reduce
      (fn [acc unit]
        (loop [pending [[(:node unit) (:start-offset unit)]]
               acc acc]
          (if-let [[n offset] (first pending)]
            (let [tag (node/tag n)
                  children (when (and (node/inner? n)
                                      (not (contains? non-code-tags tag)))
                             (first (reduce
                                      (fn [[acc position] child]
                                        [(conj acc [child position])
                                         (+ position
                                            (count (node/string child)))])
                                      [[] offset]
                                      (node/children n))))]
              (recur (into (vec (next pending)) children)
                     (if (= :token tag)
                       (conj! acc {:text (node/string n)
                                   :line (line-of offsets offset)})
                       acc)))
            acc)))
      (transient [])
      units)))

;; @spec MCP-OP-ADMIT-114
;; @spec MCP-OP-ADMIT-115
(defn- require-reference-sites
  "Where the patched image still reaches a library its ns form dropped.

  Three ways a reference survives a lost require, and all three have to be
  checked or the gate refuses correct work and admits broken work in the same
  breath: the fully qualified name; any alias the PRE-image ns form bound for
  the library, because the post image no longer says what that alias meant;
  and a symbol the libspec referred, which appears with no qualifier at all."
  [lib bindings sites]
  (let [refers (:refers bindings)
        referred (when (set? refers) refers)
        aliases (or (:aliases bindings) #{})]
    (vec (keep
           (fn [{:keys [text line]}]
             (let [keyword? (str/starts-with? text ":")
                   bare (cond
                          (str/starts-with? text "::") (subs text 2)
                          keyword? (subs text 1)
                          :else text)
                   slash (str/index-of bare "/")
                   qualifier (when slash (subs bare 0 slash))]
               (cond
                 (= qualifier lib)
                 {:line line :symbol text :via :fully-qualified}

                 (and qualifier (contains? aliases qualifier))
                 {:line line :symbol text :via :alias}

                 (and (nil? slash) (not keyword?) referred
                      (contains? referred bare))
                 {:line line :symbol text :via :refer}

                 :else nil)))
           sites))))

(defn- lost-require-hazards
  "One hazard for the libraries still reached, one for the ones now dead.

  Every extraction's sewing removes a require the moved code took with it, and
  clj-kondo reports exactly that require as unused. Refusing all of them made
  a correct patch unadmittable, and there was no override: the refusal's only
  follow-up was preview, which refuses identically. The evidence that settles
  it is in the patched image itself, so the gate reads it rather than asking."
  [file owner span before-bindings lost offsets reference-units]
  (let [sites (token-sites offsets reference-units)
        found (into {} (map (fn [lib]
                              [lib (require-reference-sites
                                     lib (get before-bindings lib) sites)]))
                    lost)
        ;; `:refer :all` cannot be disproved: any bare symbol in the image
        ;; might have come from it, so absence of evidence is not evidence.
        opaque (set (filter #(= :all (:refers (get before-bindings %))) lost))
        live (vec (sort (filter #(or (contains? opaque %) (seq (get found %)))
                                lost)))
        dead (vec (sort (remove (set live) lost)))
        live-sites (vec (mapcat #(map (fn [site] (assoc site :file file))
                                      (get found %))
                                live))]
    (cond-> []
      (seq live)
      (conj (hazard :require-removed file owner span :refusal
                    (str "The ns form no longer requires "
                         (str/join ", " live)
                         ", and the patched image still reaches "
                         (if (< 1 (count live)) "them" "it")
                         ": "
                         (if (seq live-sites)
                           (str/join ", " (map #(str (:symbol %)
                                                     " (line " (:line %) ")")
                                               live-sites))
                           (str "a :refer :all makes any unqualified symbol a "
                                "possible use, so this cannot be shown dead")))
                    {:libraries live
                     :reference-sites live-sites
                     :lift {:description
                            (str "remove the remaining reference site"
                                 (when (< 1 (count live-sites)) "s")
                                 " named below, or keep the require")
                            :sites live-sites}}))

      (seq dead)
      (conj (hazard :require-removed file owner span :note
                    (str "The ns form no longer requires "
                         (str/join ", " dead)
                         "; admitted as a dead-require removal -- the patched "
                         "image references "
                         (if (< 1 (count dead)) "them" "it")
                         " nowhere, by alias, fully qualified name, or "
                         "referred symbol")
                    {:libraries dead
                     :reference-sites []})))))

;; @spec MCP-OP-ADMIT-117
(defn- referred-symbol-sites
  "Where the patched image still uses a dropped `:refer` symbol unqualified.

  The library itself is still required in this branch, so a qualified call is
  not the question: `set/difference` keeps working the moment `difference`
  leaves the `:refer` vector, and counting it would refuse the same correct
  narrowing the lost-require branch was refusing. Only a bare use breaks, so
  only a bare use is evidence."
  [symbols sites]
  (let [wanted (set symbols)]
    (vec (keep (fn [{:keys [text line]}]
                 (when (and (not (str/starts-with? text ":"))
                            (nil? (str/index-of text "/"))
                            (contains? wanted text))
                   {:line line :symbol text :via :refer}))
               sites))))

;; @spec MCP-OP-ADMIT-117
(defn- dropped-refer-hazards
  "One hazard for the dropped refers still used bare, one for the dead ones.

  The sibling of `lost-require-hazards`, and the same argument: narrowing a
  `:refer` vector to the symbols the file still uses is the other half of what
  every extraction's sewing does, and refusing it unconditionally refused
  correct work for the same reason -- the question was `did the vector lose a
  symbol?` when the question that decides it is `does the image still use it?`"
  [file owner span unreferred offsets reference-units]
  (let [sites (token-sites offsets reference-units)
        classified (mapv (fn [{:keys [library symbols]}]
                           (let [found (referred-symbol-sites symbols sites)
                                 used (set (map :symbol found))]
                             {:library library
                              :live (vec (sort (filter used symbols)))
                              :dead (vec (sort (remove used symbols)))
                              :sites (mapv #(assoc % :file file
                                                   :library library)
                                           found)}))
                         unreferred)
        entries (fn [key]
                  (vec (keep (fn [row]
                               (when (seq (get row key))
                                 {:library (:library row)
                                  :symbols (get row key)}))
                             classified)))
        qualified (fn [rows]
                    (str/join ", " (mapcat (fn [{:keys [library symbols]}]
                                             (map #(str library "/" %) symbols))
                                           rows)))
        live (entries :live)
        dead (entries :dead)
        live-sites (vec (mapcat :sites (filter #(seq (:live %)) classified)))]
    (cond-> []
      (seq live)
      (conj (hazard :require-removed file owner span :refusal
                    (str "The ns form no longer refers " (qualified live)
                         ", and the patched image still uses "
                         (if (< 1 (count live-sites)) "them" "it")
                         " unqualified: "
                         (str/join ", " (map #(str (:symbol %)
                                                   " (line " (:line %) ")")
                                             live-sites)))
                    {:libraries []
                     :referred-symbols-removed live
                     :reference-sites live-sites
                     :lift {:description
                            (str "restore the dropped :refer symbol"
                                 (when (< 1 (count live-sites)) "s")
                                 ", qualify the remaining use"
                                 (when (< 1 (count live-sites)) "s")
                                 " named below, or remove "
                                 (if (< 1 (count live-sites)) "them" "it"))
                            :sites live-sites}}))

      (seq dead)
      (conj (hazard :require-removed file owner span :note
                    (str "The ns form no longer refers " (qualified dead)
                         "; admitted as a dead-refer removal -- the patched "
                         "image uses "
                         (if (< 1 (count dead)) "them" "it")
                         " nowhere unqualified")
                    {:libraries []
                     :referred-symbols-removed dead
                     :reference-sites []})))))

(defn- require-hazards
  [file pre-index post-index offsets post-units]
  (let [ns-unit (fn [index] (first (filter #(= :ns (:form-kind %))
                                           (vals (:unique index)))))
        pre-ns (ns-unit pre-index)
        post-ns (ns-unit post-index)
        reference-units (filterv #(and (= :form (:kind %))
                                       (not= :ns (:form-kind %)))
                                 post-units)]
    (cond
      (and pre-ns (nil? post-ns))
      ;; Deleting the namespace form removes every require, alias, import and
      ;; the namespace's own identity at once. Reporting that as one removed
      ;; owner understates it to the point of being misleading.
      [(hazard :namespace-form-removed file (:name pre-ns)
               [(:start-line pre-ns) (:end-line pre-ns)]
               :refusal
               (str "The ns form for " (:name pre-ns) " is gone from the "
                    "patched image")
               {:requires-lost (vec (sort (keys (ns-requires (:node pre-ns)))))
                :lift {:description (str "restore the ns form in the patched "
                                         "image, or send the removal as a "
                                         "whole-file delete")
                       :sites []}})]

      (and pre-ns post-ns)
      (let [before-bindings (ns-require-bindings (:node pre-ns))
            before (into {} (map (fn [[lib b]] [lib (:refers b)]))
                         before-bindings)
            after (ns-requires (:node post-ns))
            lost (vec (sort (remove (set (keys after)) (keys before))))
            unreferred (->> before
                            (keep (fn [[lib refers]]
                                    (let [now (get after lib)]
                                      (when (and (contains? after lib)
                                                 (not= :all now)
                                                 (set? refers))
                                        (let [dropped (vec (sort (remove
                                                                   (or now #{})
                                                                   refers)))]
                                          (when (seq dropped)
                                            {:library lib :symbols dropped}))))))
                            vec)
            span [(:start-line post-ns) (:end-line post-ns)]]
        (cond-> []
          ;; @spec MCP-OP-ADMIT-114
          ;; @spec MCP-OP-ADMIT-115
          (seq lost)
          (into (lost-require-hazards file (:name post-ns) span
                                      before-bindings lost offsets
                                      reference-units))

          ;; @spec MCP-OP-ADMIT-117
          (seq unreferred)
          (into (dropped-refer-hazards file (:name post-ns) span unreferred
                                       offsets reference-units)))))))

(defn- code-shaped?
  [text]
  (and (re-find #"[{}]" text)
       (boolean
         (re-find #"[;=]|\breturn\b|\bfunction\b|\bconst\b|\bvar\b|\blet\b"
                  text))))

(defn- within-any?
  [spans line]
  (boolean (some (fn [[start end]] (<= start line end)) spans)))

(defn- overlaps-any?
  [spans start end]
  (boolean (some (fn [[s e]] (and (<= s end) (<= start e))) spans)))

;; @spec MCP-OP-ADMIT-034
(defn- opaque-string-hazards
  [file offsets post-units post-spans]
  (vec
    (mapcat
      (fn [unit]
        (when (= :form (:kind unit))
          (loop [pending [[(:node unit) (:start-offset unit)]]
                 found []]
            (if-let [[n offset] (first pending)]
              (let [text (node/string n)
                    end (+ offset (count text))
                    children (when (node/inner? n)
                               (first (reduce
                                        (fn [[acc position] child]
                                          [(conj acc [child position])
                                           (+ position
                                              (count (node/string child)))])
                                        [[] offset]
                                        (node/children n))))
                    start-line (line-of offsets offset)
                    end-line (line-of offsets (max 0 (dec end)))]
                (if (and (string-node? n)
                         (> (- (count text) 2) opaque-string-threshold)
                         (code-shaped? text)
                         (overlaps-any? post-spans start-line end-line)
                         (not (within-any? post-spans start-line)))
                  (recur (into (vec (next pending)) children)
                         (conj found
                               (hazard :opaque-string-edit file (:name unit)
                                       [start-line end-line]
                                       :informational
                                       (str "A hunk changed the interior of a "
                                            "code-shaped string literal whose "
                                            "opening delimiter is outside every "
                                            "hunk; the gate cannot check it")
                                       {:string-characters (- (count text) 2)})))
                  (recur (into (vec (next pending)) children) found)))
              found))))
      post-units)))

;; @spec MCP-OP-ADMIT-031
(defn- unreadable
  [file error]
  {:file file
   :ok false
   :owners {:added [] :removed [] :changed []}
   :protected-node-drift {}
   :byte-drift-outside-hunks 0
   :hazards [{:type :unreadable-post-image
              :file file
              :owner nil
              :span [1 1]
              :class :refusal
              :message (str "The patched image cannot be read as balanced "
                            "Clojure: " error)
              ;; @spec MCP-OP-ADMIT-116
              :lift {:description (str "nothing lifts this refusal: the "
                                       "patched image has to parse as "
                                       "balanced Clojure before the gate can "
                                       "read it at all")
                     :liftable false
                     :sites []}}]})

;; @spec MCP-OP-ADMIT-020
;; @spec MCP-OP-ADMIT-021
;; @spec MCP-OP-ADMIT-022
;; @spec MCP-OP-ADMIT-024
;; @spec MCP-OP-ADMIT-067
(defn form-identity-delta
  "Compare two images of one file as forms rather than as lines.

  Returns owners added, removed, and changed by defining-form name; per-owner
  protected-node drift; the bytes the patch moved outside its structural
  change; and every typed hazard."
  [{:keys [file pre post hunk-spans operation]}]
  (let [pre-image (try (decompose pre) (catch Exception e {::error e}))
        post-image (try (decompose post) (catch Exception e {::error e}))]
    (cond
      (::error post-image)
      (unreadable file (.getMessage ^Exception (::error post-image)))

      (::error pre-image)
      (unreadable file
                  (str "the original image already fails to read: "
                       (.getMessage ^Exception (::error pre-image))))

      :else
      (let [pre-units (:units pre-image)
            post-units (:units post-image)
            pre-index (keyed-units pre-units)
            post-index (keyed-units post-units)
            pre-counted (counted-definitions pre-units)
            post-counted (counted-definitions post-units)
            pre-names (set (keys (:unique pre-index)))
            post-names (set (keys (:unique post-index)))
            duplicated (set (concat (keys (:duplicates pre-index))
                                    (keys (:duplicates post-index))))
            shared (sort (remove duplicated (filter post-names pre-names)))
            comparisons
            (for [key shared
                  :let [before (get-in pre-index [:unique key])
                        after (get-in post-index [:unique key])
                        source-equal? (= (:source before) (:source after))
                        code-changed? (not= (code-shape (:node before))
                                            (code-shape (:node after)))]]
              {:name (:name after)
               :source-equal? source-equal?
               :code-changed? code-changed?
               :drift (if (or source-equal? code-changed?)
                        0
                        (diff-window (:source before) (:source after)))
               :protected (when-not source-equal?
                            (protected-delta (collect-protected (:node before))
                                             (collect-protected (:node after))
                                             code-changed?))})
            pre-gaps (gap-pairs pre-units)
            post-gaps (gap-pairs post-units)
            gap-drift (reduce
                        (fn [total [key before]]
                          (if-let [after (get post-gaps key)]
                            (+ total (diff-window (:source before)
                                                  (:source after)))
                            total))
                        0
                        pre-gaps)]
        {:file file
         :ok true
         :owners {:added (vec (sort (keep #(get-in post-index [:unique % :name])
                                          (remove duplicated
                                                  (remove pre-names post-names)))))
                  :removed (vec (sort (keep #(get-in pre-index [:unique % :name])
                                            (remove duplicated
                                                    (remove post-names pre-names)))))
                  :changed (vec (map :name (filter :code-changed? comparisons)))}
         :protected-node-drift (into {} (keep (fn [{:keys [name protected]}]
                                                (when protected [name protected]))
                                              comparisons))
         :byte-drift-outside-hunks (+ (reduce + 0 (map :drift comparisons))
                                      gap-drift)
         :hazards (vec (concat (duplicate-hazards file pre-counted post-counted)
                               ;; A deletion loses its ns form by design. The
                               ;; question that matters there is asked of the
                               ;; workspace, not of the empty post image.
                               (when-not (= :delete operation)
                                 (require-hazards file pre-index post-index
                                                  (:offsets post-image)
                                                  post-units))
                               (opaque-string-hazards file (:offsets post-image)
                                                      post-units
                                                      (vec (:post hunk-spans)))))}))))

(defn refusal-hazards
  [hazards]
  (filterv #(= :refusal (:class %)) hazards))
