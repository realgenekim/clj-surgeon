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
  * An INTERIOR comment (one between children of the owner form) is never
    carried automatically, even under `:carry-comments`. Its position carries
    meaning, and a machine placing it by guess corrupts that meaning more quietly
    than dropping it. Leading and trailing comments have exactly one legal
    position, so those are the only ones carry moves.

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

(defn comment-preservation
  "Compare the comments of an owner span against a replacement span.

   Multiset difference, order preserved: a comment the owner carried twice and
   the replacement carries once is lost once. Returns `{:preserved true}` or
   `{:preserved false :lost [...]}` with the lost strings verbatim."
  [owner-source replacement-source]
  (let [{:keys [lost]}
        (reduce (fn [{:keys [have lost]} c]
                  (if (pos? (get have c 0))
                    {:have (update have c dec) :lost lost}
                    {:have have :lost (conj lost c)}))
                {:have (frequencies (comment-nodes replacement-source)) :lost []}
                (comment-nodes owner-source))]
    (if (seq lost) {:preserved false :lost lost} {:preserved true})))

(defn carry-comments
  "Carry the owner's lost comments onto the replacement text BY POSITION.

   Leading comments go above the replacement, trailing comments below, each on
   its own line and in the owner's order. An interior lost comment is refused
   rather than guessed: returns `{:ok false :interior [...]}`, and the caller
   must turn that into `:forms-comment-lost`."
  [owner-source replacement-source]
  (let [lost (set (:lost (comment-preservation owner-source replacement-source)))
        wanted (filter #(contains? lost (:text %)) (classified-comments owner-source))
        interior (mapv :text (filter #(= :interior (:position %)) wanted))]
    (if (seq interior)
      {:ok false :interior interior}
      (let [pick (fn [p] (mapv :text (filter #(= p (:position %)) wanted)))
            above (pick :leading)
            below (pick :trailing)]
        {:ok true
         :source (str (apply str (map #(str % "\n") above))
                      replacement-source
                      (apply str (map #(str "\n" %) below)))
         :carried (into above below)}))))

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
