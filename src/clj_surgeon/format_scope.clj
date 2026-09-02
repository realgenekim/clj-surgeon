(ns clj-surgeon.format-scope
  "Confine a managed formatter to the top-level forms a transaction edited.

   A staged candidate file is mostly source nobody asked to change. Handing the
   whole file to a formatter reformats all of it, which is how the 2026-09-02
   `l1` cohort turned 93 lines of work into +508/-476 of review burden
   (`docs/observations/2026-09-02-captains-log-the-big-aha-and-reset.md`,
   \"churn attributed\"). The red team then measured that
   `standard-clojure-style fix` on a **complete top-level form in isolation**
   produces bytes identical to formatting that same form inside the full file;
   only sub-form fragments differ, and only by the starting column they lose
   when cut out of their parent.

   So the unit to format is the enclosing top-level form. This namespace owns
   the arithmetic for that: which top-level forms an edit touches, how to cut
   them out, how to splice the formatted text back at its exact original span,
   and how to prove that every byte between those forms survived untouched.

   Pure. No I/O, no subprocess, no formatter opinion of its own."
  (:require
   [clojure.string :as str]
   [rewrite-clj.node :as n]
   [rewrite-clj.parser :as p]))

(defn- line-offsets
  "Character offset of the first character of every line in `source`."
  [source]
  (loop [offsets [0]
         index 0]
    (if-let [newline (str/index-of source "\n" index)]
      (recur (conj offsets (inc newline)) (inc newline))
      offsets)))

;; @spec MCP-OP-FMT-001
(defn top-level-form-spans
  "Character spans of every top-level form in `source`, ascending and disjoint.

   Whitespace and comments *between* top-level forms are not forms and never
   appear here; that is the point, because those are the bytes a scoped
   formatter must not be able to reach. Comments *inside* a form are part of
   that form's span and travel with it."
  [source]
  (if (str/blank? (str source))
    []
    (let [offsets (line-offsets source)
          at (fn [row col]
               (when (and (pos-int? row) (pos-int? col) (<= row (count offsets)))
                 (+ (nth offsets (dec row)) (dec col))))
          children (try
                     (n/children (p/parse-string-all source))
                     (catch Exception _ nil))]
      (into []
            (keep (fn [node]
                    ;; Whitespace between forms is not a form. Neither is a
                    ;; comment between forms, and that byte range is exactly
                    ;; what a scoped formatter must never be handed.
                    (when-not (n/whitespace-or-comment? node)
                      (let [{:keys [row col end-row end-col]} (meta node)
                            start (at row col)
                            end (at end-row end-col)]
                        (when (and start end (<= 0 start) (<= start end)
                                   (<= end (count source)))
                          {:start start :end end})))))
            children))))

(defn- touches?
  [{:keys [start end]} {:keys [offset length]}]
  (let [offset (long offset)
        length (long length)]
    (if (zero? length)
      ;; A zero-length span is a point. A point on a form's boundary belongs to
      ;; that form: an insertion at a form's first byte is an edit of that form.
      (and (<= start offset) (<= offset end))
      (and (< offset end) (< start (+ offset length))))))

;; @spec MCP-OP-FMT-002
(defn enclosing-form-spans
  "The top-level form spans of `source` that any of `spans` touches.

   `spans` are `{:offset :length}` replacement regions in `source` coordinates —
   the same shape `splice-drift/result-spans` produces. An edit that falls
   between two top-level forms selects neither; there is no enclosing form to
   format and nothing is staged."
  [source spans]
  (let [spans (vec spans)]
    (if (empty? spans)
      []
      (filterv (fn [form] (some #(touches? form %) spans))
               (top-level-form-spans source)))))

;; @spec MCP-OP-FMT-003
(defn splice-forms
  "Replace each region of `source` with its formatted text.

   `regions` are ascending `{:start :end}` spans and `texts` is the formatted
   replacement for each, positionally. The splice runs in **descending** order
   so a region whose formatting changed its length cannot move the offsets of a
   region that has not been spliced yet.

   Returns `{:source s :spans [{:offset :length}]}` with the spans in result
   coordinates, ascending."
  [source regions texts]
  (let [regions (vec regions)
        texts (vec texts)]
    (when-not (= (count regions) (count texts))
      (throw (ex-info "Each region needs exactly one formatted text"
                      {:regions (count regions) :texts (count texts)})))
    (let [spliced
          (reduce (fn [acc index]
                    (let [{:keys [start end]} (nth regions index)]
                      (str (subs acc 0 start) (nth texts index) (subs acc end))))
                  source
                  (reverse (range (count regions))))
          result-spans
          (first
            (reduce (fn [[acc delta] index]
                      (let [{:keys [start end]} (nth regions index)
                            text (nth texts index)]
                        [(conj acc {:offset (+ start delta)
                                    :length (count text)})
                         (+ delta (- (count text) (- end start)))]))
                    [[] 0]
                    (range (count regions))))]
      {:source spliced :spans result-spans})))

(defn- utf8-bytes
  ^long [^String text]
  (alength (.getBytes text "UTF-8")))

(defn- gaps
  "The text between consecutive spans, plus the head and the tail."
  [source spans]
  (loop [remaining (seq spans) cursor 0 acc []]
    (if-let [{:keys [start end]} (first remaining)]
      (recur (next remaining)
             (max cursor (long end))
             (conj acc (subs source
                             (min cursor (count source))
                             (min (max cursor (long start)) (count source)))))
      (conj acc (subs source (min cursor (count source)))))))

;; @spec MCP-OP-FMT-004
(defn scope-drift
  "Assert that the splice arithmetic held: gap `i` of `pre` equals gap `i` of
   `post`.

   **This is a self-test, not a proof, and it must not be described as one.**
   `splice-forms` builds `post` by concatenating the gaps of `pre` with the
   formatted texts, so for any output of `splice-forms` the gaps are equal *by
   construction* and this function cannot return nonzero. Measured (red-team
   probe p1): every formatter output, including garbage and the empty string,
   scores `:exact 0` here. Its value is that it fails loudly if the splice
   arithmetic is ever changed and gets it wrong; it is a regression sentinel on
   `splice-forms`, and nothing about the formatter is bounded by it.

   What actually bounds the formatter after this leaf is
   `clause-normalised-stream`, and it is the only thing that does.

   It never searches, and it never assumes the two sources are the same length
   — a form whose formatting changed its own length is the ordinary case."
  [pre post regions result-spans]
  (let [pre-gaps (gaps pre (map (fn [{:keys [start end]}]
                                  {:start start :end end})
                                regions))
        post-gaps (gaps post (map (fn [{:keys [offset length]}]
                                    {:start offset :end (+ offset length)})
                                  result-spans))
        differing (when (= (count pre-gaps) (count post-gaps))
                    (remove (fn [[a b]] (= a b))
                            (map vector pre-gaps post-gaps)))]
    (if (or (nil? differing) (seq differing))
      {:byte-drift-outside-forms
       (if (nil? differing)
         (+ (utf8-bytes (apply str pre-gaps)) (utf8-bytes (apply str post-gaps)))
         (reduce + 0 (map (fn [[a b]] (+ (utf8-bytes a) (utf8-bytes b)))
                          differing)))
       :span-alignment :unlocatable}
      {:byte-drift-outside-forms 0 :span-alignment :exact})))

(defn- clause-list?
  "True for the `(:require ...)` / `(:import ...)` list inside an `ns` form —
   the one place a formatter is sanctioned to reorder siblings."
  [kids]
  (let [head (first kids)]
    (boolean (and head
                  (n/keyword-node? head)
                  (#{:require :import} (n/sexpr head))))))

(defn- comment-sig
  "One comment, with the layout a formatter owns normalised away.

   `standard-clojure-style` 0.29.0 rewrites `;;foo` to `;; foo` (and `;;;foo`,
   and an end-of-line `;;t`) on ordinary source. Measured red-team probe p11:
   without this, the real pinned formatter on the real wire route refused an
   ordinary `apply_clojure_changes` transaction outright, accusing itself of
   changing code. The semicolons and the comment's text are still compared
   exactly; only the run of spaces or tabs directly after the semicolons, and
   trailing whitespace, are layout."
  [node]
  [[:comment (-> (n/string node)
                 str/trimr
                 (str/replace #"^(;+)[ \t]*" "$1 "))]])

(defn- clause-groups
  "Partition a clause list's siblings into groups of leading comments plus the
   clause they precede.

   The sort that makes a sorted `:require` admissible must move a comment WITH
   its clause and must never move it BETWEEN clauses — which is exactly what
   the real formatter does. Sorting bare siblings would let a comment be
   reattached to a different require and commit (red-team probe p8 i-b, i-c).
   Trailing comments with no clause after them form a final group of their own,
   so they cannot be dropped from the comparison."
  [kids]
  (loop [remaining (seq kids) pending [] acc []]
    (if-let [kid (first remaining)]
      (if (n/comment? kid)
        (recur (next remaining) (conj pending kid) acc)
        (recur (next remaining) [] (conj acc (conj pending kid))))
      (cond-> acc (seq pending) (conj pending)))))

(defn- sig
  [node]
  (cond
    (n/whitespace? node) nil
    (n/comment? node) (comment-sig node)
    (n/inner? node)
    (let [kids (remove n/whitespace? (n/children node))]
      (into [[:open (n/tag node)]]
            (concat (if (clause-list? kids)
                      ;; Sort whole clause GROUPS, so `[a :as x]` moving past
                      ;; `[b :as y]` normalises away while a symbol moving
                      ;; BETWEEN two clauses — or a comment being reattached to
                      ;; a different one — does not.
                      (concat (sig (first kids))
                              (apply concat
                                     (sort (map #(vec (mapcat sig %))
                                                (clause-groups (rest kids))))))
                      (mapcat sig kids))
                    [[:close (n/tag node)]])))
    :else [[:leaf (n/string node)]]))

;; @spec MCP-OP-FMT-005
(defn clause-normalised-stream
  "The ORDERED tokens and comments of `text`, with exactly one reordering
   normalised away: the sibling clauses of a `(:require ...)` or
   `(:import ...)` list are sorted as whole subtrees.

   This is the only bound on what a managed formatter may do inside a form it
   is handed, so it is order-sensitive everywhere else. Sorting `:require`
   clauses is what `standard-clojure-style fix` visibly does and must stay
   admissible; every other reordering is a code change wearing a formatter's
   clothes.

   Measured, 2026-09-02: this check refuses `swap-if-branches`,
   `swap-non-commutative-args`, `move-token-to-sibling` and
   `move-:refer-between-requires` — four rewrites a token multiset admits — and
   cost **zero** false refusals over all 1735 top-level forms of this
   repository formatted by the real `standard-clojure-style` 0.29.0.

   `nil` when `text` does not parse, and a `nil` on either side is a refusal at
   the caller, never a match."
  [text]
  (try
    (vec (mapcat sig (remove n/whitespace?
                             (n/children (p/parse-string-all text)))))
    (catch Exception _ nil)))

;; @spec MCP-OP-FMT-005
(defn one-form?
  "True when `text` parses to exactly one top-level form.

   A formatter handed one complete form must give one complete form back. Two
   forms means it split something; zero means it ate it."
  [text]
  (try
    (= 1 (count (remove n/whitespace-or-comment?
                        (n/children (p/parse-string-all text)))))
    (catch Exception _ false)))

(defn- refusal
  [error-type file message extra]
  (merge {:ok false
          :error-type error-type
          :error message
          :file file
          :source-unchanged true
          :mutation-attempted false
          :write-authority false}
         extra))

;; @spec MCP-OP-FMT-006
;; @spec MCP-OP-FMT-010
;; @spec MCP-OP-FMT-011
(defn file-plan
  "Decide what a scoped format may touch in one staged file.

   Returns `{:file f :regions [...]}` or a typed refusal. Pure, so the decision
   is witnessed without a formatter, a project, or a JVM.

   Four decisions, and three of them are refusals, because every way of getting
   this wrong ends with the transaction's own expectation being replaced by
   bytes nobody measured:

   - an **exemption recorded in the guard** leaves the file alone;
   - **no guard entry, or no reference bytes**, cannot be scoped at all;
   - a **staged candidate that is not the guard's reference** means some earlier
     staging step already churned this file. Formatting it would rewrite the
     guard to point at that churned image and launder the churn through the
     commit gate, so it is refused instead;
   - a candidate that **does not parse** while the guard names spans yields no
     forms. Today that is a silent no-op that still overwrites the guard, which
     is the same laundering with a different cause."
  [file source guard-entry]
  (cond
    (:exempt guard-entry) {:file file :regions []}

    (or (nil? guard-entry) (not (string? (:reference guard-entry))))
    (refusal
      :format-scope-unmeasurable file
      (str "Refusing to format " file ": this transaction records no"
           " byte-preserving reference for it, so the formatter cannot be"
           " scoped to the forms it edited. An unscoped format is refused,"
           " never assumed safe.")
      {:remedy (str "Compile this change through a route that records its"
                    " splice guard for every staged file, then retry once."
                    " No source was changed.")})

    (not= source (:reference guard-entry))
    (refusal
      :format-scope-candidate-mismatch file
      (str "Refusing to format " file ": the staged bytes are not the bytes"
           " this transaction compiled, so a staging step already changed this"
           " file. Formatting it would replace the transaction's expectation"
           " with that changed image and the commit gate would never see it.")
      {:remedy (str "Remove the staging step that rewrote this file, then"
                    " retry once. No source was changed.")})

    (and (seq (:spans guard-entry))
         (empty? (top-level-form-spans source)))
    (refusal
      :format-scope-unparseable-candidate file
      (str "Refusing to format " file ": this transaction names edited spans in"
           " it but no top-level form can be read from the staged bytes, so the"
           " formatter's scope cannot be computed. An unreadable candidate is"
           " refused, never treated as nothing to do.")
      {:remedy (str "Compile this change through a route that stages parseable"
                    " source, then retry once. No source was changed.")})

    :else
    {:file file
     :regions (enclosing-form-spans source (:spans guard-entry))}))

(defn trim-trailing-newlines
  "Drop the trailing line terminators a formatter adds because it was writing a
   file. A top-level form's span never ends in a raw newline, so removing them
   restores the exact shape the span had."
  [text]
  (str/replace (str text) #"[\r\n]+\z" ""))
