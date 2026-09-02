(ns clj-surgeon.patch-apply
  "Pure, strict unified-diff application with hunk span bookkeeping.

  This namespace reads a patch; it never writes one and never repairs one.
  Application is exact: context and removed lines must equal the frozen
  snapshot at the hunk's declared position. There is no offset search, no fuzz
  factor, and no whitespace tolerance, because a patch that only almost applies
  is a patch whose author and whose applier disagree about the current file."
  (:require
   [clojure.string :as str]))

(def ^:private hunk-header-pattern
  #"^@@ -(\d+)(?:,(\d+))? \+(\d+)(?:,(\d+))? @@")

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

(defn- parse-hunk
  "Consume one hunk's body. Returns [hunk remaining-lines] or a refusal."
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
        [{:pre-start pre-start :pre-count pre-count
          :post-start post-start :post-count post-count
          :body body}
         remaining]
        (let [line (first remaining)]
          (cond
            (nil? line)
            (refusal :invalid-patch
                     (str "Hunk " hunk-index " of " file
                          " ends before its declared line counts")
                     {:file file :hunk-index hunk-index})

            (str/starts-with? line "\\")
            (recur (next remaining) body pre-seen post-seen)

            :else
            ;; A producer that strips trailing whitespace turns a context line
            ;; for an empty source line into an empty string. Only a truly
            ;; empty line is read that way; " " keeps its space marker.
            (let [marker (if (= "" line) \space (first line))
                  text (if (= "" line) "" (subs line 1))]
              (case marker
                \space (recur (next remaining) (conj body [:context text])
                              (inc pre-seen) (inc post-seen))
                \- (recur (next remaining) (conj body [:remove text])
                          (inc pre-seen) post-seen)
                \+ (recur (next remaining) (conj body [:add text])
                          pre-seen (inc post-seen))
                (refusal :invalid-patch
                         (str "Unrecognized hunk body line in " file
                              ": " (pr-str line))
                         {:file file :hunk-index hunk-index})))))))))

;; @spec MCP-OP-ADMIT-014
(defn parse-patch
  "Parse unified diff text into ordered per-file hunk plans.

  Returns {:ok true :files [{:file .. :hunks [..]}]} or a typed refusal."
  [patch-text]
  (if-not (and (string? patch-text) (not (str/blank? patch-text)))
    (refusal :invalid-patch "patch must be non-blank unified diff text")
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
                       "patch contains no unified diff file headers")

              (some (comp empty? :hunks) files)
              (refusal :invalid-patch
                       "patch contains a file header with no hunks"
                       {:file (:file (first (filter (comp empty? :hunks)
                                                    files)))})

              ;; Two headers for one file describe two different post images
              ;; of the same bytes. Refusing here keeps that ambiguity out of
              ;; the transaction, where it can only surface as a write that
              ;; fails and rolls back.
              (not= (count files) (count (distinct (map :file files))))
              (let [repeated (->> (map :file files)
                                  frequencies
                                  (keep (fn [[file n]] (when (< 1 n) file)))
                                  sort
                                  vec)]
                (refusal :duplicate-patch-target
                         (str "patch names the same file in more than one "
                              "file header: " (str/join ", " repeated))
                         {:files repeated :file (first repeated)}))

              :else
              {:ok true :files files}))

          (str/starts-with? line "--- ")
          (recur (next remaining)
                 (cond-> files current (conj current))
                 (header-path line)
                 nil)

          (str/starts-with? line "+++ ")
          (let [new-path (header-path line)]
            (cond
              (or (= :dev-null old-path) (= :dev-null new-path))
              (refusal :unsupported-patch-operation
                       (str "Whole-file creation and deletion are not admitted; "
                            "apply them natively and admit the edits separately")
                       {:file (if (= :dev-null old-path) new-path old-path)})

              (nil? old-path)
              (refusal :invalid-patch
                       "patch has a +++ header with no matching --- header")

              (not= old-path new-path)
              (refusal :unsupported-patch-operation
                       "Renaming a file is not admitted"
                       {:file old-path :renamed-to new-path})

              :else
              (recur (next remaining) files old-path
                     {:file new-path :hunks []})))

          (re-find hunk-header-pattern line)
          (if-not current
            (refusal :invalid-patch
                     "patch has a hunk header with no file header")
            (let [result (parse-hunk (:file current) (count (:hunks current))
                                     line (next remaining))]
              (if (map? result)
                result
                (let [[hunk rest-lines] result]
                  (recur rest-lines files old-path
                         (update current :hunks conj hunk))))))

          :else
          (recur (next remaining) files old-path current))))))

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
      (if-let [{:keys [pre-start pre-count body]} (first remaining)]
        (let [start (dec pre-start)
              expected (mapv second (filter #(#{:context :remove} (first %)) body))
              emitted (mapv second (filter #(#{:context :add} (first %)) body))
              actual (when (<= (+ start (count expected)) (count lines))
                       (subvec lines start (+ start (count expected))))]
          (cond
            (< start cursor)
            (refusal :overlapping-hunks
                     (str "Hunk " index " of " file " overlaps an earlier hunk")
                     {:file file :hunk-index index})

            (or (neg? start) (nil? actual))
            (refusal :patch-does-not-apply
                     (str "Hunk " index " of " file
                          " names lines beyond the end of the file")
                     {:file file :hunk-index index
                      :pre-start pre-start :pre-count pre-count})

            (not= expected actual)
            (let [mismatch (first (keep-indexed
                                    (fn [i expect]
                                      (when (not= expect (nth actual i nil)) i))
                                    expected))]
              (refusal :patch-does-not-apply
                       (str "Hunk " index " of " file
                            " does not match the current file at line "
                            (+ pre-start mismatch))
                       {:file file :hunk-index index
                        :line (+ pre-start mismatch)
                        :expected-line (nth expected mismatch)
                        :actual-line (nth actual mismatch nil)}))

            :else
            (let [prefix (subvec lines cursor start)
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
                     (+ start (count expected))
                     (conj pre-spans (span-of pre-start pre-removed))
                     (conj post-spans (span-of (inc (count out))
                                               post-added))))))
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
  "Parse and apply one unified diff against a frozen source map."
  [sources patch-text]
  (let [parsed (parse-patch patch-text)]
    (if-not (:ok parsed)
      parsed
      (apply-parsed sources (:files parsed)))))
