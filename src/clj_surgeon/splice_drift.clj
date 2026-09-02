(ns clj-surgeon.splice-drift
  "Prove that a write changed only the spans it named.

   A structural editor earns its review-burden argument by leaving every byte
   outside the edited span alone. This namespace measures the bytes by which a
   candidate source departs from that guarantee and turns the number into one
   typed decision. It is pure and performs no I/O.

   Evidence for why it exists:
   docs/observations/2026-09-02-captains-log-bridge-wall-clock-ideal-program.md
   receipts 08:57Z, 08:58Z and 09:01Z."
  (:require
   [clojure.string :as str]))

(defn- utf8-bytes
  ^long [^String text]
  (alength (.getBytes text "UTF-8")))

(defn- touched-lines
  ^long [^String text]
  (if (str/blank? text)
    (if (zero? (count text)) 0 1)
    (inc (count (filter #(= \newline %) text)))))

(defn- common-prefix-length
  ^long [^String a ^String b]
  (let [limit (min (count a) (count b))]
    (loop [index 0]
      (if (and (< index limit) (= (.charAt a index) (.charAt b index)))
        (recur (inc index))
        index))))

(defn- common-suffix-length
  ^long [^String a ^String b ^long prefix]
  (let [limit (- (min (count a) (count b)) prefix)]
    (loop [index 0]
      (if (and (< index limit)
               (= (.charAt a (- (count a) 1 index))
                  (.charAt b (- (count b) 1 index))))
        (recur (inc index))
        index))))

(defn text-difference
  "UTF-8 bytes and touched lines by which two strings differ, after removing
   their common prefix and suffix. Both sides are charged, because a deletion
   and an insertion are each a change a reviewer must read."
  [^String a ^String b]
  (if (= a b)
    {:bytes 0 :lines 0}
    (let [prefix (common-prefix-length a b)
          suffix (common-suffix-length a b prefix)
          middle-a (subs a prefix (- (count a) suffix))
          middle-b (subs b prefix (- (count b) suffix))]
      {:bytes (+ (utf8-bytes middle-a) (utf8-bytes middle-b))
       :lines (+ (touched-lines middle-a) (touched-lines middle-b))})))

(defn- normalized-spans
  [source spans]
  (->> spans
       (map (fn [{:keys [offset length]}]
              {:offset (max 0 (min (count source) (long offset)))
               :length (max 0 (long length))}))
       (sort-by :offset)
       vec))

(defn- gap-offsets
  "The exact offset of each gap in `source`, paired with its text. Gap i starts
   where span i-1 ended."
  [source spans]
  (let [spans (normalized-spans source spans)]
    (loop [remaining spans cursor 0 acc []]
      (if-let [{:keys [offset length]} (first remaining)]
        (let [start (max cursor offset)
              stop (min (count source) (+ offset length))]
          (recur (rest remaining)
                 (max cursor stop)
                 (conj acc {:offset (min cursor (count source))
                            :text (subs source
                                        (min cursor (count source))
                                        (min start (count source)))})))
        (conj acc {:offset (min cursor (count source))
                   :text (subs source (min cursor (count source)))})))))

;; @spec MCP-OP-CLOSE-010
;; @spec MCP-OP-CLOSE-011
;; @spec MCP-OP-CLOSE-012
(defn- byte-preserved?
  "True when every untouched gap of the reference occurs in the candidate at
   exactly the offset it occupies in the reference, and the candidate ends where
   the reference ends.

   The test is positional, never a search. `str/index-of` from a moving cursor
   lets a gap float, so junk inserted adjacent to a span, trailing whitespace
   after an edited form, and any insertion at a zero-length span all locate
   successfully and score zero. Pinning each gap to its own offset removes that
   whole class: a span's *content* may differ, but its *length* may not, because
   once a span changes length every offset after it is unknowable and the honest
   answer is `unlocatable`, not a guess."
  [reference candidate spans]
  (and (= (count reference) (count candidate))
       (every? (fn [{:keys [offset text]}]
                 (let [stop (+ offset (count text))]
                   (and (<= stop (count candidate))
                        (= text (subs candidate offset stop)))))
               (gap-offsets reference spans))))


;; @spec MCP-OP-CLOSE-005
;; @spec MCP-OP-CLOSE-021
(defn drift-outside-spans
  "Measure how far `candidate` departs from the post-image the request itself
   specifies.

   `reference` is that expected post-image: the original source with each named
   span replaced by the caller's own replacement text, spliced by byte offset.
   `spans` are those replacement regions in `reference` coordinates.

   Two numbers come back, and they answer different questions.

   `byte-drift-from-expected` counts every byte where the candidate differs from
   the expected image, **including inside the spans**. It is zero only when the
   bytes about to be written are exactly the bytes the request asked for. This
   is the number that gates a commit. A staging step that rewrites the caller's
   own replacement text is changing what was asked for, which is churn wearing a
   different hat.

   `byte-drift-outside-span` counts only the untouched gaps, and is the weaker
   claim. It is retained because it is the number the earlier receipts published,
   but it must never gate on its own: when the named spans cover the whole
   source there are no gaps left, the comparison is vacuous, and it reads zero
   for a file that was rewritten end to end. That false green is exactly what
   probe R5 found."
  [reference candidate spans]
  (let [{:keys [bytes lines]} (text-difference reference candidate)
        preserved? (or (= reference candidate)
                       (byte-preserved? reference candidate spans))]
    {:byte-drift-from-expected bytes
     :line-drift-from-expected lines
     :byte-drift-outside-span (if preserved? 0 bytes)
     :line-drift-outside-span (if preserved? 0 lines)
     :span-alignment (if preserved? :exact :unlocatable)}))

;; @spec MCP-OP-CLOSE-006
;; @spec MCP-OP-CLOSE-007
(defn gate
  "Decide whether bytes that drifted outside their named spans may be written.

   Commit refuses any positive drift and reports the number. A non-committing
   compile reports the same number and allows it, so a caller can see the churn
   before deciding."
  [{:keys [file reference candidate spans commit?]}]
  (let [measured (drift-outside-spans reference candidate spans)
        drift (:byte-drift-from-expected measured)]
    (if (and commit? (pos? drift))
      (merge measured
             {:ok false
              :error-type :byte-drift-outside-span
              :error
              (str "Refusing to write " file ": " drift
                   " bytes across " (:line-drift-from-expected measured)
                   " lines differ from the change this request asked for."
                   (if (pos? (:byte-drift-outside-span measured))
                     " Source outside the edited spans would change."
                     (str " The edited span itself would be rewritten beyond"
                          " the replacement text supplied."))
                   " A structural write must put exactly the requested bytes"
                   " on disk and leave every other byte alone.")
              :file file
              :source-unchanged true
              :mutation-attempted false
              :write-authority false
              :remedy
              (str "Express this change through the edit_clojure `edits` route"
                   " (`within` plus `from`/`to`), which does not restage whole"
                   " files, or remove the stage that reformats untouched source."
                   " No source was changed.")})
      (assoc measured :ok true))))

;; @spec MCP-OP-CLOSE-004
(defn result-spans
  "Replacement regions in result coordinates for raw edits given in original
   coordinates. Each edit supplies `:offset`, `:before` and `:after`."
  [raw-edits]
  (loop [remaining (sort-by :offset raw-edits) delta 0 acc []]
    (if-let [{:keys [offset before after]} (first remaining)]
      (recur (rest remaining)
             (+ delta (- (count after) (count before)))
             (conj acc {:offset (+ offset delta) :length (count after)}))
      acc)))
