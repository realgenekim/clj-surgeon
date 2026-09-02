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
      (str/starts-with? line "diff --git ") :unified-diff
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

(defn- surplus-change
  "The first byte-changing line left over after a hunk's declared counts."
  [remaining]
  (loop [lines remaining]
    (when-let [line (first lines)]
      (cond
        (or (str/starts-with? line "@@")
            (str/starts-with? line "--- ")
            (str/starts-with? line "+++ ")
            (str/starts-with? line "diff --git ")) nil
        (or (str/starts-with? line "-") (str/starts-with? line "+")) line
        :else (recur (next lines))))))

;; @spec MCP-OP-ADMIT-092
(defn- parse-unified-hunk
  "Consume one unified hunk body. Returns [hunk remaining-lines] or a refusal.

  The declared counts bound the body, and the line that follows must be a
  header or the end of the payload. A header that undercounts its own body
  used to leave the surplus lines to be silently ignored by the top-level
  loop, which applied a truncated hunk and produced an unreadable image out of
  a patch that merely miscounted."
  [file hunk-index header lines]
  (let [[_ pre-start pre-count post-start post-count]
        (re-find hunk-header-pattern header)
        pre-start (parse-long pre-start)
        pre-count (if pre-count (parse-long pre-count) 1)
        post-start (parse-long post-start)
        post-count (if post-count (parse-long post-count) 1)]
    (loop [remaining lines
           body []
           pre-seen 0
           post-seen 0]
      (if (and (= pre-seen pre-count) (= post-seen post-count))
        ;; Only a surplus that changes bytes matters. A trailing context line
        ;; beyond the declared count is redundant -- the lines after a hunk
        ;; are copied through anyway -- but a surplus removal or addition is
        ;; a change the header did not admit, and dropping it silently is how
        ;; a miscounted patch became an unreadable image.
        (if-let [surplus (surplus-change remaining)]
          (refusal :hunk-body-overruns-header
                   (str "Hunk " hunk-index " of " file " declares "
                        pre-count "/" post-count " lines but its body "
                        "continues past them at: " (pr-str surplus))
                   {:file file :hunk-index hunk-index
                    :header header :offending-line surplus})
          [{:pre-start pre-start :pre-count pre-count
            :post-start post-start :post-count post-count
            :body body}
           remaining])
        (let [line (first remaining)]
          (cond
            (nil? line)
            (refusal :invalid-patch
                     (str "Hunk " hunk-index " of " file
                          " ends before its declared line counts")
                     {:file file :hunk-index hunk-index :header header})

            (str/starts-with? line "\\")
            (recur (next remaining) body pre-seen post-seen)

            :else
            (if-let [[kind text] (body-line line)]
              (case kind
                :context (recur (next remaining) (conj body [:context text])
                                (inc pre-seen) (inc post-seen))
                :remove (recur (next remaining) (conj body [:remove text])
                               (inc pre-seen) post-seen)
                :add (recur (next remaining) (conj body [:add text])
                            pre-seen (inc post-seen)))
              (refusal :invalid-patch
                       (str "Unrecognized hunk body line in " file ": "
                            (pr-str line))
                       {:file file :hunk-index hunk-index
                        :offending-line line}))))))))

(defn- parse-unified-diff
  [patch-text]
  (loop [remaining (source-lines patch-text)
         files []
         old-path nil
         current nil]
    (let [line (first remaining)]
      (cond
        (nil? line)
        (let [files (cond-> files current (conj current))]
          (cond
            (empty? files)
            (refusal :invalid-patch
                     "patch contains no unified diff file headers"
                     {:grammar :unified-diff})

            (some (comp empty? :hunks) files)
            (refusal :invalid-patch
                     "patch contains a file header with no hunks"
                     {:grammar :unified-diff
                      :file (:file (first (filter (comp empty? :hunks) files)))})

            :else
            {:ok true :grammar :unified-diff :files files}))

        (str/starts-with? line "--- ")
        (recur (next remaining)
               (cond-> files current (conj current))
               (header-path line)
               nil)

        (str/starts-with? line "+++ ")
        (let [new-path (header-path line)]
          (cond
            (or (= :dev-null old-path) (= :dev-null new-path))
            {:ok true :grammar :unified-diff
             :files (conj (cond-> files current (conj current))
                          {:file (if (= :dev-null old-path) new-path old-path)
                           :operation (if (= :dev-null old-path) :add :delete)
                           :hunks []})}

            (nil? old-path)
            (refusal :invalid-patch
                     "patch has a +++ header with no matching --- header"
                     {:grammar :unified-diff :offending-line line})

            (not= old-path new-path)
            {:ok true :grammar :unified-diff
             :files (conj (cond-> files current (conj current))
                          {:file old-path :operation :move
                           :move-to new-path :hunks []})}

            :else
            (recur (next remaining) files old-path
                   {:file new-path :operation :update :hunks []})))

        (re-find hunk-header-pattern line)
        (if-not current
          (refusal :invalid-patch
                   "patch has a hunk header with no file header"
                   {:grammar :unified-diff :offending-line line})
          (let [result (parse-unified-hunk (:file current)
                                           (count (:hunks current))
                                           line (next remaining))]
            (if (map? result)
              result
              (let [[hunk rest-lines] result]
                (recur rest-lines files old-path
                       (update current :hunks conj hunk))))))

        :else
        (recur (next remaining) files old-path current)))))

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

;; @spec MCP-OP-ADMIT-091
(defn- parse-apply-patch
  "Parse the V4A grammar `apply_patch` actually takes.

  Sections are opened by `*** Update File:`, `*** Add File:` and
  `*** Delete File:`; an update may be followed by `*** Move to:`; hunks
  inside an update are opened by `@@`, whose trailing text is a context
  anchor rather than a line number."
  [patch-text]
  (let [lines (source-lines patch-text)]
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
          (let [files (close-file files current hunk)]
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

          (str/blank? line)
          (recur (next remaining) files current hunk started?)

          (= apply-patch-begin (str/trim line))
          (recur (next remaining) files current hunk true)

          (= apply-patch-end (str/trim line))
          {:ok true :grammar :apply-patch
           :files (close-file files current hunk)}

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
    (let [at (dec start)]
      (cond
        (neg? at) {:error (refusal :patch-does-not-apply
                                   (str "Hunk " index " of " file
                                        " names line " start)
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

(defn apply-parsed
  "Apply already-parsed per-file hunk plans to one frozen source map."
  [sources parsed-files]
  (loop [remaining parsed-files
         images []]
    (if-let [{:keys [file hunks]} (first remaining)]
      (let [source (get sources file)]
        (if-not (string? source)
          (refusal :patch-source-missing
                   (str "Patch names a file that is not in the frozen snapshot: "
                        file)
                   {:file file})
          (let [image (apply-file-hunks file source hunks)]
            (if-not (:ok image)
              image
              (recur (next remaining) (conj images image))))))
      {:ok true :files images})))

(defn apply-patch
  "Parse and apply one patch, in either grammar, against a frozen source map."
  [sources patch-text]
  (let [parsed (parse-patch patch-text)]
    (if-not (:ok parsed)
      parsed
      (apply-parsed sources (:files parsed)))))
