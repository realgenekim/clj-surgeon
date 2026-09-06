(ns clj-surgeon.mission-forms-source
  "Comment-preserving source lowering for owner-keyed candidate replacement.

  Why this namespace exists: `mission-forms/definition` used to refuse any owner
  or replacement containing a `;` comment as `:forms-protected-syntax`. That was
  an honest guard, not a technical limit -- the danger was never that rewrite-clj
  cannot see a comment, it is that a model asked to re-emit a definition drops the
  comment and the splice would delete it SILENTLY. The guard traded a whole
  syntactic class away to avoid one silent loss.

  This namespace buys the class back by making the loss LOUD instead of
  impossible. Everything here reads the candidate replacement as SOURCE TEXT and
  keeps it as source text: rewrite-clj nodes, never `read-string`, never
  `sexpr`, never evaluation. A comment survives because the bytes survive.

  Five facts callers should hold:

  * A `;` inside a string literal is NOT a comment. rewrite-clj lexes it into the
    string token, so it never appears in `comment-nodes` and can never be
    reported lost.
  * `#_`, `#=`, and `^meta` remain protected syntax in `mission-forms`. This
    namespace deliberately does not widen that scope; only `:comment` was
    released.
  * NOTHING is carried by position, and position is not what is compared either.
    A comment's meaning is the EXPRESSION it guards, so preservation compares
    the attached expression's IDENTITY -- its own canonical source, the side it
    sits on, and the depth-path of the containing sexpr -- and never its
    ordinal. An ordinal comparison accepts a swap of two body expressions
    falsely (Astra, review of c1614bf9: owner `(defn f [] ; guards risky
    (risky) (safe))` against a replacement that swaps the two bodies has every
    ordinal intact and every guard wrong). A comment whose text survives against
    a DIFFERENT expression is `:forms-comment-moved`, reported with the `:from`
    and `:to` expression texts; one whose text is gone is `:forms-comment-lost`.
    Both are typed refusals, never a guess.
  * INSERTING a new expression before a commented one is ACCEPTED. The guarded
    expression's identity did not change, so nothing moved. The previous round
    compared ordinals and refused this; that conservative narrowing is gone.
  * A mission that deliberately REWRITES the guarded expression may opt in with
    `:comment-follows-rewrite true` on the basis, which accepts an identity
    change when side, depth-path and ordinal are unchanged. That opt-in
    re-admits the ordinal rule for those comments -- under it a deliberate swap
    IS accepted -- which is precisely why it is opt-in and never a fallback.

  No formatter runs here. cljfmt is not on this project's classpath (see
  deps.edn); `format-replacement` is the seam a caller would fill if it ever is,
  and it reports honestly that it did nothing."
  (:require
   [rewrite-clj.node :as node]
   [rewrite-clj.parser :as parser]))

(def trivia
  "Node tags that carry no program text and no comment text."
  #{:whitespace :newline :comma})

(def ignorable
  "Node tags skipped when counting the sexprs of a span."
  (conj trivia :comment))

(defn refuse!
  ([code] (refuse! code {}))
  ([code data] (throw (ex-info "Forms source refused" (assoc data :error-type code)))))

(defn parse-owner-source
  "The owner span's source text -> a rewrite-clj node tree.

   `parse-string-all` is deliberate: it keeps comment, whitespace and
   reader-macro nodes, and `node/string` round-trips the input byte for byte.
   That round-trip is the whole basis of the byte-identity witness in
   `splice-owner`."
  [source]
  (when-not (string? source) (refuse! :forms-source-invalid))
  (parser/parse-string-all source))

(defn- comment-text
  "A comment node's text with its terminating newline removed, so the same
   comment compares equal at end-of-file and mid-file."
  [n]
  (let [s (node/string n)]
    (if (.endsWith s "\n") (subs s 0 (dec (count s))) s)))

(defn- nested-comments [n]
  (->> (tree-seq node/inner? node/children n)
       (filter #(= :comment (node/tag %)))
       (map comment-text)))

(defn classified-comments
  "Ordered comments in a span, each `{:text ... :position :leading|:interior|:trailing}`.

   Leading = before the span's first sexpr. Trailing = after its last. Everything
   else -- including every comment nested inside the form -- is interior."
  [source]
  (let [kids (vec (node/children (parse-owner-source source)))
        idxs (keep-indexed (fn [i n] (when-not (contains? ignorable (node/tag n)) i)) kids)
        first-i (first idxs)
        last-i (last idxs)]
    (vec
      (mapcat
        (fn [i n]
          (cond
            (= :comment (node/tag n))
            [{:text (comment-text n)
              :position (cond (nil? first-i) :leading
                              (< i first-i) :leading
                              (> i last-i) :trailing
                              :else :interior)}]

            (contains? trivia (node/tag n)) []

            :else (map (fn [t] {:text t :position :interior}) (nested-comments n))))
        (range) kids))))

(defn comment-nodes
  "The ordered list of comment strings inside a span. Verbatim, newline trimmed."
  [source]
  (mapv :text (classified-comments source)))

(defn lower-replacement
  "The candidate's replacement as SOURCE TEXT, not data.

   Parses with rewrite-clj and validates that it carries exactly one top-level
   form. Leading and trailing comments are allowed and are not forms; two
   definitions are `:forms-replacement-not-one-form`."
  [source]
  (let [tree (parse-owner-source source)
        forms (remove #(contains? ignorable (node/tag %)) (node/children tree))]
    (when-not (= 1 (count forms))
      (refuse! :forms-replacement-not-one-form {:form-count (count forms)}))
    {:node tree :form (first forms) :comments (comment-nodes source)}))

(defn- normalize-expr
  "The attached expression's canonical source: its own bytes with SURROUNDING
   whitespace trimmed. Interior bytes are deliberately untouched -- collapsing
   whitespace inside the form would make `\"a  b\"` and `\"a b\"` compare equal,
   which is a false acceptance of a different expression."
  [n]
  (when n (.trim ^String (node/string n))))

(defn- attachment-identities
  "Comments under `children`, each attached to the EXPRESSION it guards.

   An attachment is `{:comment text :expr <canonical source of the attached
   sexpr> :side :before|:after :depth-path <containing-sexpr path, WITHOUT the
   final ordinal> :ordinal <index of the attached sexpr at that level>}`.

   One exception, and it is the owner contract rather than a weakening: at the
   span's own top level (`depth-path []`) `:expr` is the sentinel `:owner-form`.
   The expression there is the owner definition itself, which the mission
   replaces by design; its identity is checked separately as
   `:forms-owner-mismatch` (head, name-or-new-owner, docstring). Comparing its
   bytes here would refuse every rename.

   `:comment`, `:expr`, `:side` and `:depth-path` are the IDENTITY. `:ordinal`
   is carried alongside it, never inside it: it is only the tie-break between
   two attachments whose identity is already equal, and the sole thing the
   opt-in `:comment-follows-rewrite` compares."
  [children prefix]
  (let [sexprs (vec (remove #(contains? ignorable (node/tag %)) children))
        total (count sexprs)]
    (loop [i 0 seen 0 acc []]
      (if (>= i (count children))
        acc
        (let [k (nth children i)
              t (node/tag k)]
          (cond
            (= :comment t)
            (let [before? (< seen total)
                  ord (if before? seen (max 0 (dec total)))]
              (recur (inc i) seen
                     (conj acc {:comment (comment-text k)
                                ;; At the span's own top level (`depth-path []`)
                                ;; the attached expression IS the owner
                                ;; definition, which the mission replaces
                                ;; wholesale -- comparing its text there would
                                ;; refuse every rename, which is the one thing
                                ;; this route exists to do. That expression's
                                ;; identity is already guarded, by
                                ;; `:forms-owner-mismatch` on head, name and
                                ;; docstring, so here it is the sentinel
                                ;; `:owner-form` and `:side` carries the rest of
                                ;; the meaning (a leading note turned trailing
                                ;; still refuses).
                                :expr (if (seq prefix)
                                        (normalize-expr (get sexprs ord))
                                        :owner-form)
                                :side (if before? :before :after)
                                :depth-path prefix
                                :ordinal ord})))

            (contains? trivia t) (recur (inc i) seen acc)

            :else
            (recur (inc i) (inc seen)
                   (into acc (when (node/inner? k)
                               (attachment-identities (vec (node/children k)) (conj prefix seen)))))))))))

(defn comment-attachments
  "Ordered comments in a span, each with the identity of the expression it is
   attached to.

   This is the unit of preservation. Comparing text alone would let a model move
   a `;; clj-kondo/ignore` line -- or any comment whose whole value is WHICH
   expression it sits against -- onto a different expression and still pass.
   Comparing an ORDINAL instead of an expression has the same hole from the
   other side: swapping two body expressions leaves every ordinal intact."
  [source]
  (attachment-identities (vec (node/children (parse-owner-source source))) []))

(defn- identity-of [a] (dissoc a :ordinal))

(defn comment-preservation
  "Compare the comments of an owner span against a replacement span by TEXT AND
   ATTACHED-EXPRESSION IDENTITY.

   A comment is preserved when the same text is attached to the same expression,
   on the same side, at the same depth-path. Three consequences follow, and each
   one is a deliberate contract:

   * Swapping two body expressions MOVES every comment attached to them, even
     though no ordinal changed. This is the case a path/ordinal comparison
     accepts falsely.
   * INSERTING a new expression before a commented one preserves the comment.
     The identity did not change, so nothing is refused. The previous round's
     ordinal rule refused this; that narrowing is gone.
   * Two identical expressions carrying the same comment text are
     indistinguishable by identity, so they are matched in document order --
     a stable tie-break -- and swapping them is a no-op that is accepted.

   When the mission legitimately REWRITES the guarded expression, its identity
   changes and the comment is reported `:forms-comment-moved` with the `:from`
   and `:to` expression texts, so the caller can re-emit the comment against the
   rewritten expression. A caller that intends exactly that may pass
   `:comment-follows-rewrite true`, which accepts an identity change when the
   comment's side, depth-path and ordinal are unchanged. Be honest about what
   that opt-in costs: it re-admits the ordinal rule for those comments, so under
   it a deliberate swap of two body expressions is accepted. It is opt-in for
   that reason.

   Order-preserving and multiset-like: each owner comment consumes at most one
   replacement comment. Identity matches are assigned first, across ALL owner
   comments, before any move is reported -- otherwise an earlier comment could
   steal the replacement a later one matches exactly.

   Returns `{:preserved true}`, or `{:preserved false}` plus `:lost [...]` and/or
   `:moved [{:comment ... :from {:expr :side :depth-path} :to {...}}]`."
  ([owner-source replacement-source] (comment-preservation owner-source replacement-source nil))
  ([owner-source replacement-source {:keys [comment-follows-rewrite]}]
   (let [owners (vec (comment-attachments owner-source))
         repl (vec (comment-attachments replacement-source))
         pick (fn [used pred] (first (keep-indexed (fn [i r] (when (and (not (used i)) (pred r)) i)) repl)))
         sweep (fn [[used pending] pred-for]
                 (reduce (fn [[u p] idx]
                           (if-let [k (pick u (pred-for (nth owners idx)))]
                             [(conj u k) p]
                             [u (conj p idx)]))
                         [used []] pending))
         [used-1 pend-1] (sweep [#{} (vec (range (count owners)))]
                                (fn [c] #(= (identity-of %) (identity-of c))))
         [used-2 pend-2] (if comment-follows-rewrite
                           (sweep [used-1 pend-1]
                                  (fn [c] #(and (= (:comment %) (:comment c))
                                                (= (:side %) (:side c))
                                                (= (:depth-path %) (:depth-path c))
                                                (= (:ordinal %) (:ordinal c)))))
                           [used-1 pend-1])
         [_ lost moved]
         (reduce (fn [[u lost moved] idx]
                   (let [c (nth owners idx)]
                     (if-let [k (pick u #(= (:comment %) (:comment c)))]
                       [(conj u k) lost
                        (conj moved {:comment (:comment c)
                                     :from (identity-of (dissoc c :comment))
                                     :to (identity-of (dissoc (nth repl k) :comment))})]
                       [u (conj lost (:comment c)) moved])))
                 [used-2 [] []] pend-2)]
     (cond-> {:preserved (and (empty? lost) (empty? moved))}
       (seq lost) (assoc :lost lost)
       (seq moved) (assoc :moved moved)))))

(defn align-replacement
  "The exact bytes to splice for one owner: the replacement's FORM, plus its
   leading/trailing comments ONLY in positions the owner span itself carried.

   Found by a live fake-provider run: asked to re-emit an owner, the model
   faithfully echoed the `;; why this exists` line that sits ABOVE the owner span
   and the `; trailing note` below it. Neither is inside the span, so splicing
   them put a second copy of each into the file next to the one already there.
   Interior comments need no such rule -- they live inside the form node and ride
   along with it."
  [owner-positions replacement-span form-source]
  (let [cs (classified-comments replacement-span)
        pick (fn [p] (if (contains? owner-positions p)
                       (mapv :text (filter #(= p (:position %)) cs))
                       []))]
    (str (apply str (map #(str % "\n") (pick :leading)))
         form-source
         (apply str (map #(str "\n" %) (pick :trailing))))))

(defn owner-comment-positions [owner-source]
  (set (map :position (classified-comments owner-source))))

(defn splice-owner
  "Replace the owner span [start end) of `file-source` with `replacement`, at the
   node level, and emit the whole file string.

   Both edges must land on a top-level child boundary of the file's node tree --
   the span may cover a run of children, e.g. a leading comment, its newline, the
   form -- otherwise `:forms-source-span-unaligned`. Because every child outside
   the span is re-emitted through `node/string`, which round-trips its own input,
   every byte outside the span is identical by construction."
  [file-source start end replacement]
  (let [kids (vec (node/children (parse-owner-source file-source)))
        ;; Child boundaries in byte offsets. A span may cover a RUN of children
        ;; -- a leading comment, its newline, the form, a trailing comment -- so
        ;; both edges must fall on a boundary, not just one child's extent.
        bounds (vec (reductions + 0 (map #(count (node/string %)) kids)))
        at (fn [x] (first (keep-indexed (fn [i b] (when (= b x) i)) bounds)))
        i (at start)
        j (at end)]
    (when (or (nil? i) (nil? j) (<= j i))
      (refuse! :forms-source-span-unaligned {:start start :end end}))
    (lower-replacement replacement)
    (str (apply str (map node/string (subvec kids 0 i)))
         (node/string (parse-owner-source replacement))
         (apply str (map node/string (subvec kids j))))))

(defn cljfmt-available?
  "cljfmt is optional and is NOT a dependency of this project today."
  []
  (try (require 'cljfmt.core) true (catch Throwable _ false)))

(defn format-replacement
  "Format the REPLACEMENT TEXT ONLY through cljfmt, never the whole file.

   Returns `{:formatted? false :reason :cljfmt-absent :source source}` when
   cljfmt is not on the classpath, which is the case for this project as
   written. Reformatting the file would rewrite bytes outside the owner span,
   which the splice contract forbids."
  [source]
  (if-not (cljfmt-available?)
    {:formatted? false :reason :cljfmt-absent :source source}
    (let [f (resolve 'cljfmt.core/reformat-string)]
      (try {:formatted? true :source (f source)}
           (catch Throwable _ {:formatted? false :reason :cljfmt-failed :source source})))))
