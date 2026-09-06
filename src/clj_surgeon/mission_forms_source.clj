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
  * A mission that deliberately REWRITES the guarded expression is REFUSED,
    strictly and with no opt-in. There is no basis flag, no escape hatch and no
    fallback: `:forms-comment-moved` says so and its `:next_call` names the one
    runnable recovery, a reviewed native edit made by hand. An earlier round
    shipped a `:comment-follows-rewrite` Boolean; it was removed because it
    restored the reproduced false acceptance (under it a deliberate swap of two
    body expressions passes), because the public mission request had no proven
    route for setting it, and because a blanket Boolean is a new authority mode
    rather than a recovery. An exact per-comment rewrite authorization can be
    designed later; it is not this.

  * Expression identity is EXACT SOURCE IDENTITY, compared as a rewrite-clj
    NODE FINGERPRINT: node tags plus literal token and string bytes, with only
    whitespace, newline and comma nodes discarded. Source strings are never
    regex-collapsed. So a pure re-indentation of a multi-line guarded
    expression is the SAME expression, while `\"a  b\"` and `\"a b\"` differ --
    those spaces are inside a string literal, they are program text, and the
    node keeps them byte for byte.

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

(defn node-fingerprint
  "EXACT SOURCE IDENTITY of a node, as a comparable value.

   Not canonical source: nothing is normalised, rewritten or re-printed. The
   fingerprint keeps every node TAG and every literal token/string node's bytes
   VERBATIM, and discards exactly one thing -- `trivia`, the whitespace, newline
   and comma nodes that carry no program text. Nothing else is collapsed, and a
   source string is never touched by a regular expression.

   Two consequences, and both are the contract:

   * A pure RE-INDENTATION of a multi-line guarded expression is the same
     expression. Only trivia nodes changed.
   * `\"a  b\"` and `\"a b\"` are DIFFERENT expressions. Those spaces are inside
     a string literal, so rewrite-clj lexed them into one string token and its
     bytes are part of the fingerprint. A comparator that collapsed source text
     would call them equal, which is a false acceptance of different program
     text."
  [n]
  (let [t (node/tag n)]
    (cond
      (contains? trivia t) nil
      (and (node/inner? n) (every? node/node? (node/children n)))
      (into [t] (keep node-fingerprint (node/children n)))
      :else [t (node/string n)])))

(defn- expr-text
  "The attached expression's source, surrounding whitespace trimmed. This is for
   REPORTING only -- `:from`/`:to` in a refusal must be readable by the caller.
   It is never the thing compared; `node-fingerprint` is."
  [n]
  (when n (.trim ^String (node/string n))))

(defn- attachment-identities
  "Comments under `children`, each attached to the EXPRESSION it guards.

   An attachment is `{:comment text :expr <readable source of the attached
   sexpr> :expr-id <that sexpr's exact-source-identity fingerprint> :side
   :before|:after :depth-path <containing-sexpr path, WITHOUT the final ordinal>
   :ordinal <index of the attached sexpr at that level>}`.

   `:expr-id` is what is COMPARED -- a `node-fingerprint`, so a re-indentation is
   the same expression and a changed byte inside a string literal is not.
   `:expr` is the same expression as readable text and exists so a refusal can
   name it; it is never the comparison.

   One exception, and it is the owner contract rather than a weakening: at the
   span's own top level (`depth-path []`) `:expr` and `:expr-id` are both the
   sentinel `:owner-form`. The expression there is the owner definition itself,
   which the mission replaces by design; comparing its bytes here would refuse
   every rename, which is the one thing this route exists to do.

   What the sentinel costs, stated plainly. The owner definition's identity is
   guarded SEPARATELY, and only there: `mission-forms/compile-forms` requires
   exactly ONE declared owner in the span (`definition` refuses
   `:forms-invalid-definition` for a span carrying two definitions) and then
   requires that owner's HEAD, its NAME (the original owner name, or the
   planner's `:new-owner`) and its DOCSTRING to match, refusing
   `:forms-owner-mismatch` otherwise. That is an OWNER-IDENTITY BOUNDARY: it
   proves the replacement is still the same declared thing, with the same
   contract text. It is NOT a proof that a top-level comment -- or the docstring
   it echoes -- remains semantically TRUE after a behaviour change inside the
   body. No machine here claims that; a leading `;; returns nil on miss` note
   survives a body that starts throwing, and only a human review catches it.
   `:side` still carries the rest of the meaning (a leading note turned trailing
   still refuses).

   `:comment`, `:expr-id`, `:side` and `:depth-path` are the IDENTITY. `:ordinal`
   and `:expr` ride alongside, never inside it: `:ordinal` is only the tie-break
   between two attachments whose identity is already equal."
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
                  ord (if before? seen (max 0 (dec total)))
                  top? (empty? prefix)
                  target (get sexprs ord)]
              (recur (inc i) seen
                     (conj acc {:comment (comment-text k)
                                :expr (if top? :owner-form (expr-text target))
                                :expr-id (if top? :owner-form (some-> target node-fingerprint))
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

(defn- identity-of
  "The comparable identity of an attachment: its text, the guarded expression's
   EXACT SOURCE IDENTITY fingerprint, the side and the depth-path. `:ordinal` is
   a tie-break, not identity; `:expr` is readable reporting text, not identity."
  [a]
  (dissoc a :ordinal :expr))

(defn- report-of
  "How an attachment is NAMED in a refusal: readable, and never the fingerprint."
  [a]
  (select-keys a [:expr :side :depth-path]))

(defn comment-preservation
  "Compare the comments of an owner span against a replacement span by TEXT AND
   ATTACHED-EXPRESSION IDENTITY.

   A comment is preserved when the same text is attached to the same expression,
   on the same side, at the same depth-path. Expression sameness is EXACT SOURCE
   IDENTITY (`node-fingerprint`): tags plus literal token/string bytes, only
   whitespace/newline/comma nodes discarded, never a collapsed source string. So
   a pure re-indentation preserves the comment, and a byte changed INSIDE a
   string literal does not.

   Four consequences follow, and each one is a deliberate contract:

   * Swapping two body expressions MOVES every comment attached to them, even
     though no ordinal changed. This is the case a path/ordinal comparison
     accepts falsely.
   * INSERTING a new expression before a commented one preserves the comment.
     The identity did not change, so nothing is refused.
   * RE-INDENTING the guarded expression preserves the comment. Only trivia
     nodes changed, and trivia is the one thing the fingerprint drops.
   * Two identical expressions carrying the same comment text are
     indistinguishable by identity, so they are matched in document order --
     a stable tie-break -- and swapping them is a no-op that is accepted.

   When the mission REWRITES the guarded expression, its identity changes and
   the comment is reported `:forms-comment-moved` with the `:from` and `:to`
   expression texts. That refusal is STRICT and final: there is no opt-in, no
   basis flag and no fallback that accepts it. The recovery is a reviewed native
   edit made by hand -- a human decides whether the comment is still true of the
   rewritten expression, which is exactly the judgement no flag can delegate.

   Order-preserving and multiset-like: each owner comment consumes at most one
   replacement comment. Identity matches are assigned first, across ALL owner
   comments, before any move is reported -- otherwise an earlier comment could
   steal the replacement a later one matches exactly.

   Returns `{:preserved true}`, or `{:preserved false}` plus `:lost [...]` and/or
   `:moved [{:comment ... :from {:expr :side :depth-path} :to {...}}]`."
  [owner-source replacement-source]
  (let [owners (vec (comment-attachments owner-source))
        repl (vec (comment-attachments replacement-source))
        pick (fn [used pred] (first (keep-indexed (fn [i r] (when (and (not (used i)) (pred r)) i)) repl)))
        [used-1 pend-1]
        (reduce (fn [[u p] idx]
                  (let [c (nth owners idx)]
                    (if-let [k (pick u #(= (identity-of %) (identity-of c)))]
                      [(conj u k) p]
                      [u (conj p idx)])))
                [#{} []] (range (count owners)))
        [_ lost moved]
        (reduce (fn [[u lost moved] idx]
                  (let [c (nth owners idx)]
                    (if-let [k (pick u #(= (:comment %) (:comment c)))]
                      [(conj u k) lost
                       (conj moved {:comment (:comment c)
                                    :from (report-of c)
                                    :to (report-of (nth repl k))})]
                      [u (conj lost (:comment c)) moved])))
                [used-1 [] []] pend-1)]
    (cond-> {:preserved (and (empty? lost) (empty? moved))}
      (seq lost) (assoc :lost lost)
      (seq moved) (assoc :moved moved))))

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
