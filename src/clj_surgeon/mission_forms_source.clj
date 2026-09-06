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

  Three facts callers should hold:

  * A `;` inside a string literal is NOT a comment. rewrite-clj lexes it into the
    string token, so it never appears in `comment-nodes` and can never be
    reported lost.
  * `#_`, `#=`, and `^meta` remain protected syntax in `mission-forms`. This
    namespace deliberately does not widen that scope; only `:comment` was
    released.
  * NOTHING is carried by position. A comment's position is part of its meaning,
    so preservation compares ATTACHMENT as well as text: the same comment text
    must appear at the same structural path in the replacement. A comment whose
    text survives at a different path is `:forms-comment-moved`; one whose text
    is gone is `:forms-comment-lost`. Both are typed refusals, never a guess.

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

(defn- attachment-paths
  "Comments under `children`, each `{:text ... :path ...}`.

   A path is the structural route to the expression the comment is ATTACHED to,
   never a byte offset and never a raw child index (whitespace and other comments
   would make that unstable). Each element is the ordinal of a containing sexpr,
   and the last element says which sexpr at that level the comment sits against:
   `[:before n]` for the n-th sexpr that follows it, `[:after n]` when nothing
   follows and it trails the n-th. So a comment before the second body expression
   of a top-level `defn` is `[0 [:before 4]]` -- inside top-level form 0, before
   sexpr 4 (`defn-`, name, args, first body, second body)."
  [children prefix]
  (let [total (count (remove #(contains? ignorable (node/tag %)) children))]
    (loop [i 0 seen 0 acc []]
      (if (>= i (count children))
        acc
        (let [k (nth children i)
              t (node/tag k)]
          (cond
            (= :comment t)
            (recur (inc i) seen
                   (conj acc {:text (comment-text k)
                              :path (conj prefix (cond (< seen total) [:before seen]
                                                       (zero? total) [:before 0]
                                                       :else [:after (dec total)]))}))

            (contains? trivia t) (recur (inc i) seen acc)

            :else
            (recur (inc i) (inc seen)
                   (into acc (when (node/inner? k)
                               (attachment-paths (vec (node/children k)) (conj prefix seen)))))))))))

(defn comment-attachments
  "Ordered comments in a span with their structural attachment paths.

   This is the unit of preservation. Comparing text alone would let a model move
   a `;; clj-kondo/ignore` line -- or any comment whose whole value is WHICH
   expression it sits against -- onto a different expression and still pass."
  [source]
  (attachment-paths (vec (node/children (parse-owner-source source))) []))

(defn comment-preservation
  "Compare the comments of an owner span against a replacement span, by TEXT AND
   ATTACHMENT.

   Order-preserving, multiset-like: each owner comment consumes at most one
   replacement comment. An exact `[text path]` match is preserved. Text that
   survives at a DIFFERENT path is moved -- reported with its `:from` and `:to`
   paths, because a directive or a `;; why` note attached to the wrong expression
   is a silent semantic edit. Text that does not survive at all is lost.

   Returns `{:preserved true}`, or `{:preserved false}` plus `:lost [...]` and/or
   `:moved [{:comment ... :from ... :to ...}]`."
  [owner-source replacement-source]
  (let [repl (vec (comment-attachments replacement-source))
        pick (fn [used pred] (first (keep-indexed (fn [i r] (when (and (not (used i)) (pred r)) i)) repl)))]
    (loop [[c & more] (comment-attachments owner-source) used #{} lost [] moved []]
      (if (nil? c)
        (cond-> {:preserved (and (empty? lost) (empty? moved))}
          (seq lost) (assoc :lost lost)
          (seq moved) (assoc :moved moved))
        (if-let [exact (pick used #(and (= (:text %) (:text c)) (= (:path %) (:path c))))]
          (recur more (conj used exact) lost moved)
          (if-let [elsewhere (pick used #(= (:text %) (:text c)))]
            (recur more (conj used elsewhere) lost
                   (conj moved {:comment (:text c) :from (:path c) :to (:path (nth repl elsewhere))}))
            (recur more used (conj lost (:text c)) moved)))))))

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
