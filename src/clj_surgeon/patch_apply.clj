(ns clj-surgeon.patch-apply
  "Pure, strict patch application over the two grammars agents actually emit.

  This namespace reads a patch; it never writes one and never repairs one.

  It accepts both grammars because a gate that sits on the agent's route has
  to sit on it at the byte level. Measured in the field: a prompt that said
  \"write the change as a unified diff, the same format you would give
  apply_patch\" produced 85 admissions of which 59 refused, 32 of them with the
  identical message \"patch contains no unified diff file headers\" -- because
  `apply_patch` does not take a unified diff at all. It takes the V4A grammar,
  `*** Begin Patch` / `*** Update File:` / `@@` context hunks, and that is what
  the agents wrote. Every one of them fought the parser and then fell back to
  their native tool. A contract the caller cannot express is not a contract.

  The two grammars differ in how a hunk is located, and that difference is the
  whole of the work here. A unified diff carries line numbers and is applied at
  them. A V4A hunk carries no numbers at all: it is located by matching its
  context and removed lines against the file's content, optionally anchored by
  the text on its `@@` line. Everything downstream -- hunk spans, drift, the
  pre-image binding -- is computed identically for both."
  (:require
   [clojure.string :as str]))

(def ^:private hunk-header-pattern
  #"^@@ -(\d+)(?:,(\d+))? \+(\d+)(?:,(\d+))? @@")

(def apply-patch-begin "*** Begin Patch")
(def apply-patch-end "*** End Patch")

(def expected-headers
  "The first line each accepted grammar must start with."
  {:apply-patch "*** Begin Patch"
   :unified-diff "--- a/path/to/file.clj   (or a leading `diff --git` line)"})

(defn- refusal
  [error-type message & [data]]
  (merge {:ok false
          :error-type error-type
          :error message
          :source-unchanged true}
         data))

(defn source-lines
  "Split source so that joining the result with newlines reproduces it exactly."
  [source]
  (vec (str/split source #"\n" -1)))

(defn join-lines
  [lines]
  (str/join "\n" lines))

;; @spec MCP-OP-ADMIT-099
(defn patch-lines
  "Split patch text after removing exactly one terminating newline.

  Splitting `\"a\\n\"` yields `[\"a\" \"\"]`, and that trailing empty string reads
  as a blank context line: invisible while the declared counts ended a hunk,
  and the moment the body ended it, an extra line annexed onto every hunk that
  reached the end of a payload. Exactly one newline is removed and never more,
  because a payload that really ends in a blank line has content there and the
  reader has no business editing it away."
  [patch-text]
  (let [text (str patch-text)
        text (if (str/ends-with? text "\n")
               (subs text 0 (dec (count text)))
               text)]
    (source-lines text)))

(defn- body-line
  "Read one hunk body line as [kind text], or nil when it is not one.

  A producer that strips trailing whitespace turns a context line for an empty
  source line into an empty string, so a truly empty line is a blank context
  line; \" \" keeps its space marker."
  [line]
  (cond
    (= "" line) [:context ""]
    (str/starts-with? line " ") [:context (subs line 1)]
    (str/starts-with? line "-") [:remove (subs line 1)]
    (str/starts-with? line "+") [:add (subs line 1)]
    :else nil))

(defn- blank-or-noop?
  [line]
  (or (str/blank? line) (str/starts-with? line "\\")))

;; ---------------------------------------------------------------------------
;; Grammar detection
;; ---------------------------------------------------------------------------

(defn first-content-line
  [patch-text]
  (first (remove str/blank? (source-lines (str patch-text)))))

;; @spec MCP-OP-ADMIT-091
(defn detect-grammar
  "Which grammar a payload is written in, by its first non-blank line."
  [patch-text]
  (let [line (str/trim (str (first-content-line patch-text)))]
    (cond
      (= apply-patch-begin line) :apply-patch
      (str/starts-with? line "*** ") :apply-patch
      (str/starts-with? line "diff ") :unified-diff
      (str/starts-with? line "--- ") :unified-diff
      (str/starts-with? line "Index: ") :unified-diff
      :else nil)))

;; ---------------------------------------------------------------------------
;; Unified diff
;; ---------------------------------------------------------------------------

(defn- header-path
  "Return the project-relative path a `---`/`+++` header names, or :dev-null."
  [header]
  (let [raw (-> header
                (str/replace #"^(---|\+\+\+)\s+" "")
                (str/split #"\t")
                first
                str/trim)]
    (cond
      (= "/dev/null" raw) :dev-null
      (re-find #"^[ab]/" raw) (subs raw 2)
      :else raw)))

;; @spec MCP-OP-ADMIT-092
;; @spec MCP-OP-ADMIT-100
(defn- file-header-start?
  "Is this `--- ` line a file header rather than a removed line of text?

  A removed line whose own text begins `-- ` renders as `--- `, and reading
  that as a header ended the hunk and threw the rest of it away. A unified file
  header is always the pair, so the lookahead is the disambiguation the format
  itself provides."
  [line next-line]
  (and (str/starts-with? line "--- ")
       (some? next-line)
       (str/starts-with? next-line "+++ ")))

(defn- unified-header?
  ([line] (unified-header? line nil))
  ([line next-line]
   (or (str/starts-with? line "@@")
       (str/starts-with? line "diff ")
       (str/starts-with? line "Index: ")
       (file-header-start? line next-line))))

;; @spec MCP-OP-ADMIT-092
;; @spec MCP-OP-ADMIT-100
(defn- parse-unified-hunk
  "Consume one unified hunk. Returns [hunk remaining-lines] or a refusal.

  **The body delimits the hunk; the declared counts are advisory.** Field
  payloads miscount in both directions -- 19 of 77 refused calls declared more
  lines than they carried and 10 declared fewer -- and in every one of them
  the body said exactly what the author meant.

  **A line the reader cannot classify ends the patch, not the hunk.** Reading
  to the next header is only safe if everything before it is understood.
  Treating an unrecognised line as a quiet terminator meant a context line
  that lost its leading space silently discarded the rest of the hunk and the
  gate committed the truncated remainder with `ok: true` -- a receipt claiming
  success for an edit it did not make. There is no lenient reading of a line
  whose marker is missing: the author meant something by it, and the gate
  cannot know what."
  [file hunk-index header lines line-number]
  (let [[_ pre-start pre-count post-start post-count]
        (re-find hunk-header-pattern header)
        ;; A bare `@@`, or `@@ some context`, is the sibling grammar's hunk
        ;; marker and carries no arithmetic. Field payloads mix the two inside
        ;; one file section; the marker is recognised either way and the hunk
        ;; is then located by content, exactly as a V4A hunk is.
        anchor (when-not pre-start
                 (let [text (str/trim (subs header (min 2 (count header))))]
                   (when-not (str/blank? text) text)))
        declared-pre (when pre-count (parse-long pre-count))
        declared-post (when post-count (parse-long post-count))
        finish (fn [body]
                 (let [pre-lines (count (filter #(#{:context :remove} (first %)) body))
                       post-lines (count (filter #(#{:context :add} (first %)) body))]
                   (cond-> {:pre-count pre-lines
                            :post-count post-lines
                            :declared-pre-count declared-pre
                            :declared-post-count declared-post
                            :counts-match? (and (= pre-lines declared-pre)
                                                (= post-lines declared-post))
                            :body body}
                     pre-start (assoc :pre-start (parse-long pre-start))
                     post-start (assoc :post-start (parse-long post-start))
                     anchor (assoc :context anchor))))]
    (loop [remaining lines
           body []
           n (inc line-number)]
      (let [line (first remaining)]
        (cond
          (or (nil? line) (unified-header? line (second remaining)))
          (if (empty? body)
            (refusal :invalid-patch
                     (str "Hunk " hunk-index " of " file " has an empty body")
                     {:file file :hunk-index hunk-index :header header})
            [(finish body) remaining n])

          (str/starts-with? line "\\")
          (recur (next remaining) body (inc n))

          :else
          (if-let [[kind text] (body-line line)]
            (recur (next remaining) (conj body [kind text]) (inc n))
            (refusal :hunk-truncated
                     (str "Line " n " of the patch is inside hunk " hunk-index
                          " of " file " but carries no space, - or + marker: "
                          (pr-str line)
                          ". A line the reader cannot classify is never "
                          "skipped; the rest of this hunk would be lost.")
                     {:file file :hunk-index hunk-index
                      :patch-line n :offending-line line})))))))

;; @spec MCP-OP-ADMIT-100
(defn- parse-unified-diff
  "Read a unified payload, refusing anything it cannot account for.

  The top-level loop used to ignore whatever it did not recognise. Combined
  with a hunk that ended early, that is how body lines vanished without a
  word: the hunk stopped, the remaining lines fell through here, and the gate
  applied a fraction of the requested edit and called it success. Every line
  now has to be a header, a file section it belongs to, or a refusal."
  [patch-text]
  (loop [remaining (patch-lines patch-text)
         files []
         old-path nil
         current nil
         n 1]
    (let [line (first remaining)
          next-line (second remaining)]
      (cond
        (nil? line)
        (let [files (cond-> files current (conj current))]
          (cond
            (empty? files)
            (refusal :invalid-patch
                     "patch contains no unified diff file headers"
                     {:grammar :unified-diff})

            (some (fn [{:keys [operation hunks]}]
                    (and (empty? hunks) (contains? #{:update :move} operation)))
                  files)
            (refusal :invalid-patch
                     "patch contains a file header with no hunks"
                     {:grammar :unified-diff
                      :file (:file (first (filter (comp empty? :hunks) files)))})

            :else
            {:ok true :grammar :unified-diff :files files}))

        (file-header-start? line next-line)
        (recur (next remaining)
               (cond-> files current (conj current))
               (header-path line)
               nil
               (inc n))

        (str/starts-with? line "+++ ")
        (let [new-path (header-path line)]
          (cond
            (nil? old-path)
            (refusal :invalid-patch
                     "patch has a +++ header with no matching --- header"
                     {:grammar :unified-diff :offending-line line :patch-line n})

            ;; /dev/null on either side names a whole-file operation. The
            ;; section still carries hunks -- a creation's body is the file --
            ;; so parsing continues rather than closing the payload here.
            (= :dev-null old-path)
            (recur (next remaining) files old-path
                   {:file new-path :operation :add :hunks []} (inc n))

            (= :dev-null new-path)
            (recur (next remaining) files old-path
                   {:file old-path :operation :delete :hunks []} (inc n))

            (not= old-path new-path)
            (recur (next remaining) files old-path
                   {:file old-path :operation :move
                    :move-to new-path :hunks []} (inc n))

            :else
            (recur (next remaining) files old-path
                   {:file new-path :operation :update :hunks []} (inc n))))

        (str/starts-with? line "@@")
        (if-not current
          (refusal :invalid-patch
                   "patch has a hunk header with no file header"
                   {:grammar :unified-diff :offending-line line :patch-line n})
          (let [result (parse-unified-hunk (:file current)
                                           (count (:hunks current))
                                           line (next remaining) n)]
            (if (map? result)
              result
              (let [[hunk rest-lines end-n] result]
                (recur rest-lines files old-path
                       (update current :hunks conj hunk) end-n)))))

        ;; Anything that looks like a hunk body but is not inside one means a
        ;; hunk ended where the author did not intend it to.
        (and current (body-line line) (not (str/blank? line)))
        (refusal :hunk-truncated
                 (str "Line " n " of the patch carries a patch-body marker but "
                      "belongs to no hunk: " (pr-str line)
                      ". Applying the hunks around it would apply part of the "
                      "requested change and report success.")
                 {:grammar :unified-diff :file (:file current)
                  :patch-line n :offending-line line})

        (or (str/starts-with? line "diff ")
            (str/starts-with? line "Index: ")
            (str/starts-with? line "\\")
            (str/blank? line)
            (nil? current))
        (recur (next remaining) files old-path current (inc n))

        :else
        (refusal :hunk-truncated
                 (str "Line " n " of the patch is inside file section "
                      (:file current) " but is neither a header nor a hunk "
                      "body line: " (pr-str line))
                 {:grammar :unified-diff :file (:file current)
                  :patch-line n :offending-line line})))))

;; ---------------------------------------------------------------------------
;; apply_patch (V4A)
;; ---------------------------------------------------------------------------

(def ^:private apply-patch-directives
  {"*** Update File: " :update
   "*** Add File: " :add
   "*** Delete File: " :delete})

(defn- directive
  "Return [operation path] when a line opens a file section."
  [line]
  (some (fn [[prefix operation]]
          (when (str/starts-with? line prefix)
            [operation (str/trim (subs line (count prefix)))]))
        apply-patch-directives))

(defn- normalize-add
  "An `*** Add File` body is the whole file; carry it as one hunk at line 0.

  Creation then flows through exactly the same application path as every other
  edit, against a pre-image of nothing, instead of needing a second code path
  that nobody's hazards would run over."
  [file]
  (if (and (= :add (:operation file)) (seq (:body file)))
    (-> file
        (assoc :hunks [{:pre-start 0 :body (:body file)}])
        (dissoc :body))
    file))

;; @spec MCP-OP-ADMIT-091
(defn- parse-apply-patch
  "Parse the V4A grammar `apply_patch` actually takes.

  Sections are opened by `*** Update File:`, `*** Add File:` and
  `*** Delete File:`; an update may be followed by `*** Move to:`; hunks
  inside an update are opened by `@@`, whose trailing text is a context
  anchor rather than a line number."
  [patch-text]
  (let [lines (patch-lines patch-text)]
    (loop [remaining lines
           files []
           current nil
           hunk nil
           started? false]
      (let [line (first remaining)
            close-hunk (fn [file hunk]
                         (cond-> file hunk (update :hunks conj hunk)))
            close-file (fn [files file hunk]
                         (cond-> files file (conj (close-hunk file hunk))))]
        (cond
          (nil? line)
          (let [files (mapv normalize-add (close-file files current hunk))]
            (cond
              (not started?)
              (refusal :invalid-patch
                       (str "apply_patch payload does not begin with "
                            (pr-str apply-patch-begin))
                       {:grammar :apply-patch})

              (empty? files)
              (refusal :invalid-patch
                       (str "apply_patch payload names no file; expected a "
                            "line beginning \"*** Update File: \"")
                       {:grammar :apply-patch})

              :else {:ok true :grammar :apply-patch :files files}))

          ;; @spec MCP-OP-ADMIT-101
          ;; Only outside a hunk. Inside one, " " is a context line for a blank
          ;; source line -- str/blank? swallowed it, so any V4A hunk spanning a
          ;; blank line lost that line and then could not apply.
          (and (str/blank? line) (nil? hunk))
          (recur (next remaining) files current hunk started?)

          (= apply-patch-begin (str/trim line))
          (recur (next remaining) files current hunk true)

          (= apply-patch-end (str/trim line))
          {:ok true :grammar :apply-patch
           :files (mapv normalize-add (close-file files current hunk))}

          (directive line)
          (let [[operation path] (directive line)]
            (recur (next remaining)
                   (close-file files current hunk)
                   {:file path :operation operation :hunks []}
                   nil
                   started?))

          (str/starts-with? line "*** Move to: ")
          (if-not current
            (refusal :invalid-patch
                     "apply_patch \"*** Move to:\" has no file section"
                     {:grammar :apply-patch :offending-line line})
            (recur (next remaining) files
                   (assoc current :operation :move
                          :move-to (str/trim (subs line (count "*** Move to: "))))
                   hunk started?))

          (str/starts-with? line "@@")
          (if-not current
            (refusal :invalid-patch
                     "apply_patch hunk has no file section"
                     {:grammar :apply-patch :offending-line line})
            (recur (next remaining) files
                   (close-hunk current hunk)
                   {:context (let [text (str/trim (subs line 2))]
                               (when-not (str/blank? text) text))
                    :body []}
                   started?))

          (str/starts-with? line "*** ")
          (refusal :invalid-patch
                   (str "Unrecognized apply_patch directive: " (pr-str line))
                   {:grammar :apply-patch :offending-line line})

          :else
          (if-let [[kind text] (body-line line)]
            (cond
              ;; An Add File section is all `+` lines and needs no hunk.
              (and current (= :add (:operation current)) (nil? hunk))
              (recur (next remaining) files
                     (update current :body (fnil conj []) [kind text])
                     hunk started?)

              hunk
              (recur (next remaining) files current
                     (update hunk :body conj [kind text])
                     started?)

              :else
              (refusal :invalid-patch
                       (str "apply_patch body line outside any hunk: "
                            (pr-str line))
                       {:grammar :apply-patch :offending-line line}))
            (if (blank-or-noop? line)
              (recur (next remaining) files current hunk started?)
              (refusal :invalid-patch
                       (str "Unrecognized apply_patch line: " (pr-str line))
                       {:grammar :apply-patch :offending-line line}))))))))

;; ---------------------------------------------------------------------------
;; Front door
;; ---------------------------------------------------------------------------

;; @spec MCP-OP-ADMIT-091
;; @spec MCP-OP-ADMIT-093
(defn parse-patch
  "Parse either accepted grammar into ordered per-file hunk plans."
  [patch-text]
  (if-not (and (string? patch-text) (not (str/blank? patch-text)))
    (refusal :invalid-patch "patch must be non-blank patch text"
             {:grammars-tried (vec (sort (keys expected-headers)))
              :expected-headers expected-headers})
    (case (detect-grammar patch-text)
      :apply-patch (parse-apply-patch patch-text)
      :unified-diff (parse-unified-diff patch-text)
      (refusal :invalid-patch
               (str "patch is in neither accepted grammar; its first line is "
                    (pr-str (first-content-line patch-text)))
               {:grammars-tried (vec (sort (keys expected-headers)))
                :expected-headers expected-headers
                :offending-line (first-content-line patch-text)}))))

;; ---------------------------------------------------------------------------
;; Application
;; ---------------------------------------------------------------------------

(defn- diff-window
  "Width of the region in which two strings disagree."
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

;; @spec MCP-OP-ADMIT-013
(defn- span-of
  "Inclusive [start end] over the 1-based lines at the given offsets.

  An empty run is [start (dec start)], which reads as a position rather than a
  range and keeps pure insertions and deletions expressible."
  [base offsets]
  (if (seq offsets)
    [(+ base (apply min offsets)) (+ base (apply max offsets))]
    [base (dec base)]))

(defn- find-block
  "Index of the first occurrence of block in lines at or after from."
  [lines from block]
  (let [size (count block)
        limit (- (count lines) size)]
    (loop [i (max 0 from)]
      (when (<= i limit)
        (if (= block (subvec lines i (+ i size)))
          i
          (recur (inc i)))))))

(defn- find-anchor
  "Index of the first line at or after from whose text carries the anchor."
  [lines from anchor]
  (let [needle (str/trim anchor)]
    (loop [i (max 0 from)]
      (when (< i (count lines))
        (if (or (= needle (str/trim (nth lines i)))
                (str/includes? (nth lines i) needle))
          i
          (recur (inc i)))))))

;; @spec MCP-OP-ADMIT-091
(defn- locate-hunk
  "Where a hunk's expected block sits in the file, or a refusal.

  A unified hunk is applied at its declared line; a V4A hunk is searched for
  by content from the cursor. The `@@` anchor is treated as a hint rather than
  a requirement: it disambiguates when it matches, and a hint an author got
  slightly wrong must not turn a patch that plainly applies into a refusal."
  [file index hunk lines cursor expected]
  (if-let [start (:pre-start hunk)]
    (let [at (max 0 (dec start))]
      (cond
        (and (zero? start) (seq expected))
        {:error (refusal :patch-does-not-apply
                         (str "Hunk " index " of " file
                              " starts at line 0 but expects existing lines")
                         {:file file :hunk-index index})}
        (> (+ at (count expected)) (count lines))
        {:error (refusal :patch-does-not-apply
                         (str "Hunk " index " of " file
                              " names lines beyond the end of the file")
                         {:file file :hunk-index index})}
        :else {:at at}))
    (let [anchored (when-let [anchor (:context hunk)]
                     (when-let [from (find-anchor lines cursor anchor)]
                       (find-block lines from expected)))
          found (or anchored (find-block lines cursor expected))]
      (cond
        found {:at found}

        (empty? expected)
        {:error (refusal :patch-does-not-apply
                         (str "Hunk " index " of " file
                              " has no context to locate its insertion")
                         {:file file :hunk-index index})}

        :else
        {:error (refusal :patch-does-not-apply
                         (str "Hunk " index " of " file
                              " does not match the file; its first line is "
                              (pr-str (first expected)))
                         {:file file :hunk-index index
                          :offending-line (first expected)
                          :anchor (:context hunk)})}))))

;; @spec MCP-OP-ADMIT-010
;; @spec MCP-OP-ADMIT-011
(defn- apply-file-hunks
  [file source hunks]
  (let [lines (source-lines source)]
    (loop [remaining hunks
           index 0
           out []
           cursor 0
           pre-spans []
           post-spans []]
      (if-let [{:keys [body] :as hunk} (first remaining)]
        (let [expected (mapv second (filter #(#{:context :remove} (first %)) body))
              emitted (mapv second (filter #(#{:context :add} (first %)) body))
              located (locate-hunk file index hunk lines cursor expected)]
          (if-let [error (:error located)]
            error
            (let [at (:at located)
                  actual (when (<= (+ at (count expected)) (count lines))
                           (subvec lines at (+ at (count expected))))]
              (cond
                (< at cursor)
                (refusal :overlapping-hunks
                         (str "Hunk " index " of " file
                              " overlaps an earlier hunk")
                         {:file file :hunk-index index})

                (not= expected actual)
                (let [mismatch (first (keep-indexed
                                        (fn [i expect]
                                          (when (not= expect (nth actual i nil)) i))
                                        expected))]
                  (refusal :patch-does-not-apply
                           (str "Hunk " index " of " file
                                " does not match the current file at line "
                                (+ at mismatch 1))
                           {:file file :hunk-index index
                            :line (+ at mismatch 1)
                            :expected-line (nth expected mismatch)
                            :offending-line (nth expected mismatch)
                            :actual-line (nth actual mismatch nil)}))

                :else
                (let [prefix (subvec lines cursor at)
                      out (into out prefix)
                      pre-removed (keep-indexed
                                    (fn [i [kind _]] (when (= :remove kind) i))
                                    (filter #(#{:context :remove} (first %)) body))
                      post-added (keep-indexed
                                   (fn [i [kind _]] (when (= :add kind) i))
                                   (filter #(#{:context :add} (first %)) body))]
                  (recur (next remaining)
                         (inc index)
                         (into out emitted)
                         (+ at (count expected))
                         (conj pre-spans (span-of (inc at) pre-removed))
                         (conj post-spans (span-of (inc (count out))
                                                   post-added))))))))
        (let [out (into out (subvec lines cursor))
              post (join-lines out)]
          {:ok true
           :file file
           :pre source
           :post post
           :changed? (not= source post)
           :hunk-count (count hunks)
           :hunk-spans {:pre pre-spans :post post-spans}
           :diff-window (diff-window source post)})))))

;; @spec MCP-OP-ADMIT-095
;; @spec MCP-OP-ADMIT-096
;; @spec MCP-OP-ADMIT-097
(defn apply-parsed
  "Apply already-parsed per-file plans to one frozen source map.

  Creation and deletion are ordinary applications with one side defined to be
  nothing. A created file's pre-image is the empty string, so its owners all
  read as added and its hazards are computed over the post image alone; a
  deleted file's post-image is the empty string, so its owners all read as
  removed. Giving them a defined image on both sides is what lets one delta,
  one hazard set and one receipt describe every operation."
  [sources parsed-files]
  (loop [remaining parsed-files
         images []]
    (if-let [{:keys [file operation hunks move-to]} (first remaining)]
      (let [operation (or operation :update)
            source (get sources file)]
        (cond
          (not (string? source))
          (refusal :patch-source-missing
                   (str "Patch names a file that is not in the frozen snapshot: "
                        file)
                   {:file file})

          (= :delete operation)
          (recur (next remaining)
                 (conj images {:ok true :file file :operation :delete
                               :pre source :post ""
                               :changed? (not= source "")
                               :hunk-count (count hunks)
                               :hunk-spans {:pre [[1 (count (source-lines source))]]
                                            :post [[1 0]]}
                               :diff-window (count source)}))

          :else
          (let [image (apply-file-hunks file source hunks)]
            (if-not (:ok image)
              image
              (recur (next remaining)
                     (conj images (cond-> (assoc image :operation operation)
                                    move-to (assoc :move-to move-to))))))))
      {:ok true :files images})))

(defn apply-patch
  "Parse and apply one patch, in either grammar, against a frozen source map."
  [sources patch-text]
  (let [parsed (parse-patch patch-text)]
    (if-not (:ok parsed)
      parsed
      (apply-parsed sources (:files parsed)))))
