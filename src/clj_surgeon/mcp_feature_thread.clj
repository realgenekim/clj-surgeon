(ns clj-surgeon.mcp-feature-thread
  "`feature_thread` — one read call that returns a cross-language feature as an
  EDIT BASIS, not a richer locator.

  Origin: Gene's 2026-09-04 request, the social-media-writer Dequote/Format
  transcript, and cohort E-THREAD (docs/observations/2026-09-04-ethread-cohort.md).
  E-THREAD measured that native search already finds every leg, so a receipt that
  only LOCATES buys nothing new. What it does buy is calls: 7.4 -> 3.3 on
  social-media-writer, and the remaining calls are reads of the forms the receipt
  already located. So this receipt carries the bodies, the insertion anchors, the
  content hashes an edit can assert its pre-image against, the sibling the new
  feature must mirror, and the wiring rules a grep script cannot produce.

  Two fences this namespace never crosses:

  * It does not parse JavaScript. A script body is produced by a LEXER-driven
    brace match (states for strings, template interpolation, line and block
    comments, and regex literals) labelled `brace-window(lexed,closed)`, and when
    the counter does not close it downgrades to a labelled line window. A lexer
    has no semantics to be wrong about; its one failure mode is `did not close`,
    and that failure is loud.
  * It does not know any repository's file roles. The five leg roles, their file
    globs, the sibling rule and the governance tail are DATA, read from
    `.clj-surgeon/feature-thread.edn` under the workspace root or passed inline."
  (:require
   [cheshire.core :as json]
   [clj-surgeon.mcp-operation :as mcp-operation]
   [clj-surgeon.outline :as outline]
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.string :as str])
  (:import
   (java.security MessageDigest)))

;; ---------------------------------------------------------------------------
;; Ceilings. Every one of them is named in a refusal rather than applied in
;; silence.
;; ---------------------------------------------------------------------------

;; @spec MCP-OP-THREAD-002
(def default-budget-bytes
  "Default receipt budget, in UTF-8 bytes of the RENDERED TEXT.

  The fleet measured the five-leg social-media-writer thread with four full
  bodies, one ranked test body, five anchors and five hashes at 9,848 bytes of
  TEXT (Opus, poll 2026-09-04), so 10240 is the soft budget it recommended.

  BOTH faces are measured and BOTH are reported -- `text_bytes`,
  `structured_bytes`, and `receipt_bytes` for their sum -- but the budget is
  applied to the TEXT. The reason is this verb's own MCP-OP-THREAD-012
  guarantee: the text block is a superset of the structured content, so the
  structured half is a machine-readable COPY of bytes already counted, not
  additional information. Budgeting the sum would halve the real payload to buy
  nothing, and the trunk's own `public-byte-budget` is likewise measured on one
  face. The structured half is guarded separately by that trunk budget.

  The default is the fleet's 16384 rather than its text-only soft figure of
  10240, because this receipt carries more than the minimal edit basis it
  costed: the searches that found each leg, the governance rows, and the
  elision ledger. 10240 is a good explicit `budget_bytes` for a caller that
  wants only the bodies."
  16384)

;; @spec MCP-OP-THREAD-002
(def hard-cap-bytes
  "Largest `budget_bytes` this verb accepts. A larger request is REFUSED with
  the cap named; it is never silently clamped."
  32768)

(def trunk-public-byte-budget
  "The trunk's one public MCP payload budget, applied here to the structured
  face of the receipt (`clj-surgeon.mcp-write-refusal/public-byte-budget`)."
  32640)

(def max-scanned-files
  "Files this verb will open in one call before refusing."
  4000)

(def max-file-bytes
  "Largest single file this verb will read."
  (* 2 1024 1024))

(def max-aggregate-bytes
  "Total bytes this verb will read in one call."
  (* 96 1024 1024))

;; @spec MCP-OP-THREAD-006
(def js-close-ceiling-lines
  "Lines the lexed brace matcher will scan before giving up and downgrading."
  400)

;; @spec MCP-OP-THREAD-006
(def js-window-radius
  "Half-height of the labelled line window a failed brace match falls back to."
  40)

;; @spec MCP-OP-THREAD-005
(def oversized-form-lines
  "A Clojure top-level form larger than this is reported with its EXACT range,
  and the body is a labelled window around the hit rather than the whole form.
  `docs/intent/registry.edn` is one top-level vector of a thousand lines; a leg
  inside it must not drag the whole file into the receipt."
  200)

(def default-skip-dirs
  #{".git" ".hg" ".svn" "target" "node_modules" ".cpcache" ".clj-surgeon"
    ".beads" "compiles"})

(def clojure-extensions #{"clj" "cljc" "cljs" "edn"})

;; @spec MCP-OP-THREAD-011
(def elision-order
  "The order bodies are dropped when the receipt does not fit.

  Cheapest evidence first; the handler last, because the handler is the form the
  caller is about to edit. Fixed and stated so an elision is never a surprise."
  [:secondary-tests :tests :sibling :menu :js-function :route :handler])

;; ---------------------------------------------------------------------------
;; Small utilities
;; ---------------------------------------------------------------------------

(defn sha256-hex
  "Lowercase hex sha256 over the exact UTF-8 bytes of `text`."
  [^String text]
  (let [digest (MessageDigest/getInstance "SHA-256")]
    (->> (.digest digest (.getBytes text "UTF-8"))
         (map #(format "%02x" %))
         (apply str))))

(defn utf8-bytes
  [^String text]
  (count (.getBytes (or text "") "UTF-8")))

(defn- quote-literal
  [s]
  (java.util.regex.Pattern/quote s))

(defn glob->pattern
  "Translate one shell-style glob into a regex over a repo-relative path.

  `**/` spans directories, `*` never crosses one. Everything else is literal."
  [glob]
  (let [sb (StringBuilder.)
        n (count glob)]
    (loop [i 0]
      (if (>= i n)
        (re-pattern (str "^" (.toString sb) "$"))
        (let [c (.charAt ^String glob i)]
          (cond
            (and (= c \*) (< (inc i) n) (= (.charAt ^String glob (inc i)) \*))
            (if (and (< (+ i 2) n) (= (.charAt ^String glob (+ i 2)) \/))
              (do (.append sb "(?:[^/]+/)*") (recur (+ i 3)))
              (do (.append sb ".*") (recur (+ i 2))))

            (= c \*) (do (.append sb "[^/]*") (recur (inc i)))
            (= c \?) (do (.append sb "[^/]") (recur (inc i)))
            :else (do (.append sb (java.util.regex.Pattern/quote (str c)))
                      (recur (inc i)))))))))

(defn matches-any-glob?
  [relative globs]
  (boolean (some #(re-matches (glob->pattern %) relative) globs)))

(defn file-extension
  [^String path]
  (let [dot (.lastIndexOf path ".")]
    (if (neg? dot) "" (subs path (inc dot)))))

(defn clojure-path?
  [path]
  (contains? clojure-extensions (file-extension path)))

;; ---------------------------------------------------------------------------
;; Bounded workspace walk
;; ---------------------------------------------------------------------------

(defn- skip-dir?
  [^java.io.File dir extra-skips]
  (let [name (.getName dir)]
    (or (contains? default-skip-dirs name)
        (contains? (set extra-skips) name))))

(defn walk-relative-paths
  "Repo-relative CANDIDATE paths under `root`, bounded and sorted.

  A candidate is a file matching one of the convention set's own globs. The
  ceiling counts candidates rather than every file in the tree, so a repository
  with ten thousand assets is not refused for owning them; only a convention set
  whose globs really do select thousands of files is.

  Returns `{:ok true :paths [...]}` or a typed refusal naming the ceiling it
  hit. The walk never follows a directory named in `default-skip-dirs` or in the
  convention set's `:exclude-dirs`."
  [root extra-skips globs]
  (let [root-file (io/file root)
        root-path (.getCanonicalPath root-file)
        acc (volatile! [])
        overflow (volatile! false)]
    (letfn [(walk [^java.io.File dir]
              (when-not @overflow
                (doseq [^java.io.File f (or (seq (.listFiles dir)) [])]
                  (when-not @overflow
                    (cond
                      (.isDirectory f) (when-not (skip-dir? f extra-skips) (walk f))
                      (.isFile f)
                      (let [abs (.getCanonicalPath f)]
                        (when (str/starts-with? abs root-path)
                          (let [relative (str/replace
                                           (subs abs (inc (count root-path)))
                                           "\\" "/")]
                            (when (or (empty? globs)
                                      (matches-any-glob? relative globs))
                              (vswap! acc conj relative)
                              (when (> (count @acc) max-scanned-files)
                                (vreset! overflow true)))))))))))]
      (walk root-file))
    (if @overflow
      {:ok false
       :error_type "feature-thread-scope-too-large"
       :error (str "the convention set's globs select more than "
                   max-scanned-files " files; narrow scope.paths")
       :limits {:max_scanned_files max-scanned-files}
       :remedy "Pass scope.paths naming the directories the thread lives in."}
      {:ok true :paths (vec (sort @acc))})))

(defn- under-scope?
  [relative paths]
  (or (empty? paths)
      (boolean (some (fn [p]
                       (let [p (str/replace p #"/+$" "")]
                         (or (= relative p)
                             (str/starts-with? relative (str p "/")))))
                     paths))))

;; ---------------------------------------------------------------------------
;; Source cache
;; ---------------------------------------------------------------------------

(defn make-cache
  [root]
  (atom {:root root :sources {} :aggregate 0 :errors {}}))

(defn read-source
  "Read one repo-relative file through the cache.

  Returns `{:ok true :source s :lines [...]}`, or `{:ok false :reason kw}` for a
  file the verb cannot see. An unreadable file is a NAMED reason, never a miss:
  a leg whose files are unreadable must be able to say WHICH leg it cannot see
  and WHY."
  [cache relative]
  (let [{:keys [root sources aggregate errors]} @cache]
    (cond
      (contains? sources relative) {:ok true
                                    :source (get-in sources [relative :source])
                                    :lines (get-in sources [relative :lines])}
      (contains? errors relative) {:ok false :reason (get errors relative)}
      :else
      (let [f (io/file root relative)]
        (cond
          (not (.exists f))
          (do (swap! cache assoc-in [:errors relative] :absent)
              {:ok false :reason :absent})

          (not (.canRead f))
          (do (swap! cache assoc-in [:errors relative] :unreadable)
              {:ok false :reason :unreadable})

          (> (.length f) max-file-bytes)
          (do (swap! cache assoc-in [:errors relative] :file-too-large)
              {:ok false :reason :file-too-large})

          (> (+ aggregate (.length f)) max-aggregate-bytes)
          (do (swap! cache assoc-in [:errors relative] :aggregate-budget-exhausted)
              {:ok false :reason :aggregate-budget-exhausted})

          :else
          (try
            (let [source (slurp f)
                  lines (str/split source #"\n" -1)]
              (swap! cache (fn [c]
                             (-> c
                                 (assoc-in [:sources relative]
                                           {:source source :lines lines})
                                 (update :aggregate + (.length f)))))
              {:ok true :source source :lines lines})
            (catch java.io.IOException _
              (do (swap! cache assoc-in [:errors relative] :unreadable)
                  {:ok false :reason :unreadable}))
            (catch SecurityException _
              (do (swap! cache assoc-in [:errors relative] :unreadable)
                  {:ok false :reason :unreadable}))))))))

;; ---------------------------------------------------------------------------
;; The JavaScript body: a LEXED brace match, never a parse
;; ---------------------------------------------------------------------------
;;
;; Every real failure of naive brace matching is LEXICAL, not syntactic: braces
;; inside strings, template literals (`${x}` — the `}` closes nothing), regex
;; literals (/\d{2}/), and comments. Nested arrows and object literals are
;; balanced and a counter handles them. So this scanner tracks exactly those
;; lexical states and nothing else. It has no semantics to be wrong about; its
;; single failure mode is "did not close", which is loud, typed, and degrades to
;; the labelled line window we would otherwise have shipped.
;;
;; The one genuinely undecidable case is regex-versus-division (`a /re/ g`). A
;; wrong guess walks into a phantom regex and never closes, so it fails INTO the
;; downgrade rather than into a wrong answer. The evidence label says so.

(def ^:private regex-preceding-chars
  "Characters after which a `/` begins a regex literal rather than a division."
  #{\( \, \= \: \[ \! \& \| \? \{ \} \; \newline \return \+ \- \* \% \~ \^ \< \>})

;; @spec MCP-OP-THREAD-006
(defn lexed-brace-match
  "Scan forward from `start-index` in `source` and return the index just past the
  brace that closes the first `{` found, or nil.

  `:mode` is one of :code :line-comment :block-comment :single :double
  :template :regex, plus a stack for `${` interpolation inside a template."
  [^String source start-index limit-index]
  (let [n (min (count source) limit-index)]
    (loop [i start-index
           mode :code
           depth 0
           opened? false
           template-stack []
           prev-significant nil]
      (if (>= i n)
        nil
        (let [c (.charAt source i)
              next-c (when (< (inc i) n) (.charAt source (inc i)))]
          (case mode
            :line-comment
            (if (= c \newline)
              (recur (inc i) :code depth opened? template-stack prev-significant)
              (recur (inc i) :line-comment depth opened? template-stack prev-significant))

            :block-comment
            (if (and (= c \*) (= next-c \/))
              (recur (+ i 2) :code depth opened? template-stack prev-significant)
              (recur (inc i) :block-comment depth opened? template-stack prev-significant))

            :single
            (cond
              (= c \\) (recur (+ i 2) :single depth opened? template-stack prev-significant)
              (= c \') (recur (inc i) :code depth opened? template-stack \')
              :else (recur (inc i) :single depth opened? template-stack prev-significant))

            :double
            (cond
              (= c \\) (recur (+ i 2) :double depth opened? template-stack prev-significant)
              (= c \") (recur (inc i) :code depth opened? template-stack \")
              :else (recur (inc i) :double depth opened? template-stack prev-significant))

            :regex
            (cond
              (= c \\) (recur (+ i 2) :regex depth opened? template-stack prev-significant)
              (= c \newline) nil
              (= c \/) (recur (inc i) :code depth opened? template-stack \/)
              :else (recur (inc i) :regex depth opened? template-stack prev-significant))

            :template
            (cond
              (= c \\) (recur (+ i 2) :template depth opened? template-stack prev-significant)
              (= c \`) (recur (inc i) :code depth opened? template-stack \`)
              (and (= c \$) (= next-c \{))
              (recur (+ i 2) :code depth opened? (conj template-stack depth) prev-significant)
              :else (recur (inc i) :template depth opened? template-stack prev-significant))

            :code
            (cond
              (and (= c \/) (= next-c \/))
              (recur (+ i 2) :line-comment depth opened? template-stack prev-significant)

              (and (= c \/) (= next-c \*))
              (recur (+ i 2) :block-comment depth opened? template-stack prev-significant)

              (= c \') (recur (inc i) :single depth opened? template-stack prev-significant)
              (= c \") (recur (inc i) :double depth opened? template-stack prev-significant)
              (= c \`) (recur (inc i) :template depth opened? template-stack prev-significant)

              (and (= c \/)
                   (or (nil? prev-significant)
                       (contains? regex-preceding-chars prev-significant)))
              (recur (inc i) :regex depth opened? template-stack prev-significant)

              (= c \{)
              (recur (inc i) :code (inc depth) true template-stack c)

              (= c \})
              (cond
                (and (seq template-stack) (= depth (peek template-stack)))
                (recur (inc i) :template depth opened? (pop template-stack) c)

                (and opened? (= depth 1)) (inc i)

                :else (recur (inc i) :code (dec depth) opened? template-stack c))

              (Character/isWhitespace c)
              (recur (inc i) :code depth opened? template-stack prev-significant)

              :else (recur (inc i) :code depth opened? template-stack c))))))))

(defn- line-start-index
  "Character index at which 1-based `line` begins."
  [lines line]
  (reduce + 0 (map #(inc (count %)) (take (dec line) lines))))

(defn- index->line
  [lines index]
  (loop [remaining index
         ls lines
         n 1]
    (if (or (empty? ls) (< remaining (inc (count (first ls)))))
      n
      (recur (- remaining (inc (count (first ls)))) (rest ls) (inc n)))))

(defn- slice-lines
  [lines from to]
  (str/join "\n" (subvec (vec lines) (dec from) (min (count lines) to))))

;; @spec MCP-OP-THREAD-006
(defn script-body
  "The body of a script-language leg at 1-based `hit-line`.

  Returns `{:from :to :body :boundary}` where `:boundary` is
  `\"brace-window(lexed,closed)\"` for a closed lexed match or
  `\"line-window(+/-N, unclosed at L<n>)\"` for the honest downgrade. Never a
  claimed parse."
  [source lines hit-line]
  (let [total (count lines)
        start (line-start-index lines hit-line)
        ceiling-line (min total (+ hit-line js-close-ceiling-lines))
        limit (+ (line-start-index lines ceiling-line)
                 (count (nth lines (dec ceiling-line) "")))
        closed (lexed-brace-match source start limit)]
    (if closed
      (let [end-line (index->line lines (dec closed))]
        {:from hit-line
         :to end-line
         :body (slice-lines lines hit-line end-line)
         :boundary "brace-window(lexed,closed)"})
      (let [from (max 1 (- hit-line js-window-radius))
            to (min total (+ hit-line js-window-radius))]
        {:from from
         :to to
         :body (slice-lines lines from to)
         :boundary (str "line-window(+/-" js-window-radius
                        ", unclosed at L" hit-line ")")}))))

;; ---------------------------------------------------------------------------
;; The Clojure body: PARSED, exact range
;; ---------------------------------------------------------------------------

;; ---------------------------------------------------------------------------
;; Narrowing a Clojure hit to the MEMBER that answers the question
;; ---------------------------------------------------------------------------
;;
;; A menu item lives inside a 74-line view function and a route entry inside a
;; 293-line route table. The enclosing TOP-LEVEL form is exact and correct and
;; is also the wrong unit: it drags four kilobytes into the receipt to deliver
;; two lines. So for the leg kinds whose member is a table entry, the body is
;; the OUTERMOST bracketed form containing the hit that still fits the member
;; ceiling -- the menu item, not the map inside it and not the view around it.
;; The handler and the function legs are never narrowed: there the whole form is
;; the thing the caller edits.

(def member-ceiling-lines
  "Largest member a table-entry leg will narrow to."
  40)

(defn clj-bracket-spans
  "Every bracket pair in a Clojure source string as `[open-index close-index]`.

  Clojure-aware only where it must be: string literals, character literals and
  line comments cannot open or close a form."
  [^String text]
  (let [n (count text)]
    (loop [i 0 mode :code stack [] spans (transient [])]
      (if (>= i n)
        (persistent! spans)
        (let [c (.charAt text i)]
          (case mode
            :string (cond
                      (= c \\) (recur (+ i 2) :string stack spans)
                      (= c \") (recur (inc i) :code stack spans)
                      :else (recur (inc i) :string stack spans))
            :comment (if (= c \newline)
                       (recur (inc i) :code stack spans)
                       (recur (inc i) :comment stack spans))
            :code (cond
                    (= c \") (recur (inc i) :string stack spans)
                    (= c \;) (recur (inc i) :comment stack spans)
                    (= c \\) (recur (+ i 2) :code stack spans)
                    (contains? #{\( \[ \{} c) (recur (inc i) :code (conj stack i) spans)
                    (contains? #{\) \] \}} c)
                    (if (seq stack)
                      (recur (inc i) :code (pop stack)
                             (conj! spans [(peek stack) i]))
                      (recur (inc i) :code stack spans))
                    :else (recur (inc i) :code stack spans))))))))

(defn- line-start-offsets
  "Character offset at which each 1-based line of `text` begins."
  [^String text]
  (let [n (count text)]
    (loop [i 0 acc (transient [0])]
      (if (>= i n)
        (persistent! acc)
        (recur (inc i) (if (= \newline (.charAt text i))
                         (conj! acc (inc i))
                         acc))))))

(defn- offset->line
  "1-based line of `offset`, by binary search over line-start offsets."
  [offsets offset]
  (loop [lo 0 hi (dec (count offsets))]
    (if (>= lo hi)
      (inc lo)
      (let [mid (quot (+ lo hi 1) 2)]
        (if (<= (nth offsets mid) offset)
          (recur mid hi)
          (recur lo (dec mid)))))))

(defn narrow-to-member
  "The OUTERMOST bracketed form inside `form-source` whose line range contains
  `hit-line` (1-based, relative to the form) and spans at most `ceiling` lines.

  Line containment, not character containment: a route-table entry begins in the
  middle of its own line, and a rule that demanded the open bracket precede the
  line start would never see it. That defect returned a 25-line window where a
  one-line route entry was sitting."
  [^String form-source hit-line ceiling]
  (let [offsets (line-start-offsets form-source)
        spans (clj-bracket-spans form-source)
        sized (keep (fn [[o c]]
                      (let [from (offset->line offsets o)
                            to (offset->line offsets c)]
                        (when (and (<= from hit-line to)
                                   (<= (inc (- to from)) ceiling))
                          {:from from :to to})))
                    spans)]
    (first (sort-by (juxt :from (comp - :to)) sized))))

;; @spec MCP-OP-THREAD-005
(defn clojure-body
  "The enclosing top-level form of 1-based `hit-line`, parsed.

  Returns `{:from :to :body :boundary :form-name}`. The range is exact. A
  table-entry leg narrows to its member; a form larger than
  `oversized-form-lines` that cannot be narrowed degrades to a labelled window
  around the hit and NAMES the enclosing form's real range; a hit outside every
  top-level form says so. A Clojure leg never carries an UNLABELLED window."
  ([relative source lines hit-line] (clojure-body relative source lines hit-line nil))
  ([relative source lines hit-line {:keys [narrow?]}]
   (let [records (try
                   (outline/top-level-form-records relative source {} {:include-source? true})
                   (catch Exception _ nil)
                   (catch Error _ nil))
         enclosing (some (fn [r]
                           (when (and (:line r) (:end-line r)
                                      (<= (:line r) hit-line (:end-line r)))
                             r))
                         records)
         narrowed (when (and enclosing narrow? (:source enclosing))
                    (narrow-to-member (:source enclosing)
                                      (inc (- hit-line (:line enclosing)))
                                      member-ceiling-lines))]
     (cond
       (nil? enclosing)
       (let [from (max 1 (- hit-line 8))
             to (min (count lines) (+ hit-line 8))]
         {:from from :to to
          :body (slice-lines lines from to)
          :boundary "line-window(no-enclosing-top-level-form)"
          :form-name nil})

       narrowed
       (let [from (+ (dec (:line enclosing)) (:from narrowed))
             to (+ (dec (:line enclosing)) (:to narrowed))]
         {:from from :to to
          :body (slice-lines lines from to)
          :boundary (str "form(parsed, member of L" (:line enclosing)
                         "-L" (:end-line enclosing) " "
                         (or (some-> (:name enclosing) str) "form") ")")
          :form-name (some-> (:name enclosing) str)})

       (> (- (:end-line enclosing) (:line enclosing)) oversized-form-lines)
       (let [from (max 1 (- hit-line 12))
             to (min (count lines) (+ hit-line 12))]
         {:from from :to to
          :body (slice-lines lines from to)
          :boundary (str "line-window(in oversized form L" (:line enclosing)
                         "-L" (:end-line enclosing) ", ceiling "
                         oversized-form-lines " lines)")
          :form-name (some-> (:name enclosing) str)})

       :else
       {:from (:line enclosing)
        :to (:end-line enclosing)
        :body (or (:source enclosing)
                  (slice-lines lines (:line enclosing) (:end-line enclosing)))
        :boundary "form(parsed)"
        :form-name (some-> (:name enclosing) str)
        :comment-start (:comment-start enclosing)}))))

(defn body-at
  "Body for one hit, routed by file kind."
  ([relative source lines hit-line] (body-at relative source lines hit-line nil))
  ([relative source lines hit-line opts]
   (if (clojure-path? relative)
     (clojure-body relative source lines hit-line opts)
     (script-body source lines hit-line))))

;; ---------------------------------------------------------------------------
;; Conventions as data
;; ---------------------------------------------------------------------------

(def conventions-file ".clj-surgeon/feature-thread.edn")

(defn- as-keyword
  "A convention set that has crossed the JSON boundary carries `\"use\"` where
  the EDN file wrote `:use`. Both are the same declaration."
  [v]
  (cond (keyword? v) v
        (and (string? v) (seq v)) (keyword (str/replace v #"^:" ""))
        :else nil))

(defn- coerce-leg
  [leg]
  (if (map? leg)
    (cond-> leg
      (contains? leg :kind) (update :kind as-keyword)
      (contains? leg :elide) (update :elide as-keyword)
      (sequential? (:globs leg)) (update :globs vec))
    leg))

(defn- valid-leg?
  [leg]
  (and (map? leg)
       (string? (:id leg))
       (seq (:id leg))
       (keyword? (:kind leg))
       (vector? (:globs leg))
       (seq (:globs leg))
       (every? string? (:globs leg))))

;; @spec MCP-OP-THREAD-003
(defn normalize-conventions
  "Validate one convention set. Five leg roles, each with an id, a kind and file
  globs. A convention set this verb cannot validate is REFUSED with the field
  named; leg roles are never inferred from a built-in table of file names."
  [conventions source-label]
  (let [conventions (cond-> conventions
                      (and (map? conventions) (sequential? (:legs conventions)))
                      (update :legs #(mapv coerce-leg %))

                      (and (map? conventions)
                           (get-in conventions [:sibling :rule]))
                      (update-in [:sibling :rule] as-keyword))]
   (cond
    (not (map? conventions))
    {:ok false
     :error_type "feature-thread-conventions-invalid"
     :error (str "the convention set at " source-label " is not a map")
     :conventions_source source-label
     :remedy "Write a map with :legs, :repo-label and optional :sibling/:governance."}

    (not (and (vector? (:legs conventions)) (<= 5 (count (:legs conventions)))))
    {:ok false
     :error_type "feature-thread-conventions-invalid"
     :error (str "the convention set at " source-label
                 " must declare at least the five leg roles under :legs; found "
                 (if (coll? (:legs conventions)) (count (:legs conventions)) "none"))
     :conventions_source source-label
     :remedy "Declare the five roles this repository's features actually have."}

    (not (every? valid-leg? (:legs conventions)))
    {:ok false
     :error_type "feature-thread-conventions-invalid"
     :error (str "every leg in " source-label
                 " needs a string :id, a keyword :kind and a non-empty :globs vector")
     :conventions_source source-label
     :remedy "Correct the malformed leg entries."}

    :else
    {:ok true
     :conventions (assoc conventions :conventions_source source-label)})))

;; @spec MCP-OP-THREAD-003
(defn load-conventions
  "Resolve the convention set: inline map, or `.clj-surgeon/feature-thread.edn`
  under the workspace root. A missing file is a refusal that NAMES the path it
  searched."
  [root config]
  (cond
    (map? config) (normalize-conventions config "inline")

    :else
    (let [f (io/file root conventions-file)]
      (if-not (.isFile f)
        {:ok false
         :error_type "feature-thread-conventions-absent"
         :error (str "no feature-thread convention set for this workspace")
         :searched [(str conventions-file)]
         :conventions_source (.getPath f)
         :remedy (str "Write " conventions-file
                      " declaring the five leg roles of this repository, or pass"
                      " them inline as config.")}
        (try
          (normalize-conventions (edn/read-string (slurp f)) conventions-file)
          (catch Exception e
            {:ok false
             :error_type "feature-thread-conventions-invalid"
             :error (str conventions-file " did not read as EDN: " (.getMessage e))
             :conventions_source conventions-file
             :remedy "Correct the EDN syntax."}))))))

;; ---------------------------------------------------------------------------
;; Seeds and the per-kind searches
;; ---------------------------------------------------------------------------

(defn split-seeds
  "Seeds beginning with `/` are ROUTES; everything else is an IDENTIFIER."
  [seeds]
  {:identifiers (vec (remove #(str/starts-with? % "/") seeds))
   :routes (vec (filter #(str/starts-with? % "/") seeds))})

(defn- alternation
  [items]
  (when (seq items) (str/join "|" (map quote-literal items))))

(defn- route-tail
  [route]
  (let [segs (remove str/blank? (str/split route #"/"))]
    (if (>= (count segs) 2)
      (str/join "/" (take-last 2 segs))
      (str/join "/" segs))))

(defn searches-for-kind
  "The exact searches one leg kind runs, in order, as `[label regex]` pairs.

  Every search this verb runs is quotable, because a leg that finds nothing must
  be able to say WHAT it looked for."
  [kind {:keys [identifiers routes]}]
  (let [idents (alternation identifiers)
        rts (alternation routes)
        tails (alternation (map route-tail routes))
        seg-adjacent (alternation
                       (for [r routes
                             :let [segs (remove str/blank? (str/split r #"/"))]
                             :when (>= (count segs) 2)]
                         (str/join " " (map #(str "\"" % "\"") (take-last 2 segs)))))]
    (case kind
      :use (remove nil?
                   [(when (or idents rts)
                      ["identifier-or-route"
                       (str/join "|" (remove nil? [idents rts]))])])
      :def (remove nil?
                   [(when idents
                      ["definition-shaped"
                       (str "\\(defn?-? +(?:" idents ")\\b"
                            "|(?:async +)?function +(?:" idents ")\\b"
                            "|(?:const|let|var) +(?:" idents ")\\s*="
                            "|(?:window|globalThis)\\.(?:" idents ")\\s*="
                            "|\\b(?:" idents ")\\s*[:=]\\s*(?:async\\s*)?(?:function|\\()")])
                    (when idents ["identifier" idents])])
      :route (remove nil?
                     [(when rts ["route-literal" rts])
                      (when seg-adjacent ["route-assembled" seg-adjacent])
                      (when tails ["route-tail" tails])])
      :handler (remove nil?
                       [(when (or idents rts)
                          ["identifier-or-route"
                           (str/join "|" (remove nil? [idents rts]))])])
      :test (remove nil?
                    [(when (or idents rts tails)
                       ["identifier-route-or-tail"
                        (str/join "|" (remove nil? [idents rts tails]))])])
      (remove nil? [(when idents ["identifier" idents])]))))

(defn- render-search
  [globs [label regex]]
  (str label ": rg -n -e '" regex "'"
       (apply str (map #(str " -g '" % "'") globs))))

(defn scan
  "Hits for one regex over the leg's candidate files.

  Returns `{:hits [{:file :line :text}] :unreadable [{:file :reason}]}`. An
  unreadable candidate is CARRIED, not dropped: a leg whose files exist but
  cannot be read must say so rather than report a clean absence."
  [cache paths globs regex]
  (let [pattern (re-pattern regex)
        candidates (filter #(matches-any-glob? % globs) paths)]
    (reduce
      (fn [acc relative]
        (let [{:keys [ok lines] :as read} (read-source cache relative)]
          (if-not ok
            (update acc :unreadable conj {:file relative :reason (name (:reason read))})
            (update acc :hits into
                    (keep-indexed
                      (fn [idx line]
                        (when (re-find pattern line)
                          {:file relative :line (inc idx) :text (str/trim line)}))
                      lines)))))
      {:hits [] :unreadable []}
      candidates)))

;; ---------------------------------------------------------------------------
;; Leg resolution
;; ---------------------------------------------------------------------------

(defn refetch-command
  "The exact shell command that fetches an elided body. An elision that does not
  say how to undo itself costs the call it was meant to save."
  [file from to]
  (str "nl -ba " file " | sed -n '" from "," to "p'"))

(defn- hit->member
  [cache {:keys [file line]} evidence & [opts]]
  (let [{:keys [ok source lines]} (read-source cache file)]
    (when ok
      (let [{:keys [from to body boundary form-name comment-start]}
            (body-at file source lines line opts)]
        {:file file
         :from from
         :to to
         :hit_line line
         :evidence evidence
         :boundary boundary
         :form_name form-name
         :comment_start comment-start
         :sha256 (sha256-hex body)
         :bytes (utf8-bytes body)
         :body body
         :refetch (refetch-command file from to)}))))

;; @spec MCP-OP-THREAD-007
(defn- alias-hop
  "`const X = Y;` is definition-SHAPED and is NOT the leg: the implementation is
  elsewhere under another name. Follow it exactly ONE hop, and label the evidence
  either way. Reporting the alias line as the implementation would be a
  four-of-five thread rendered as five."
  [cache paths globs identifiers hits]
  (let [idents (alternation identifiers)
        alias-re (re-pattern (str "(?:const|let|var) +(?:" idents
                                  ") *= *([A-Za-z_$][A-Za-z0-9_$]*) *;"))
        real-re (re-pattern (str "(?:async +)?function +(?:" idents ")\\b"
                                 "|\\b(?:" idents
                                 ")\\s*[:=]\\s*(?:async\\s*)?(?:function|\\()"))
        real (first (filter #(re-find real-re (:text %)) hits))
        alias-hit (first (keep (fn [h]
                                 (when-let [m (re-find alias-re (:text h))]
                                   (assoc h :target (second m))))
                               hits))]
    (cond
      real {:hits hits :evidence "identifier(def)"}

      alias-hit
      (let [target (:target alias-hit)
            target-re (str "(?:async +)?function +" (quote-literal target) "\\b"
                           "|(?:const|let|var) +" (quote-literal target)
                           " *= *(?:async *)?(?:function|\\()")
            {:keys [hits target-hits]} {:hits hits
                                        :target-hits (:hits (scan cache paths globs target-re))}]
        (if (seq target-hits)
          {:hits target-hits
           :evidence (str "identifier(def, one hop: alias at " (:file alias-hit)
                          ":" (:line alias-hit) " -> " target ")")}
          {:hits []
           :evidence "alias-only"
           :extra-search ["alias-target"
                          (str target-re " [after following the alias at "
                               (:file alias-hit) ":" (:line alias-hit)
                               " -> " target "]")]}))

      :else {:hits hits :evidence "identifier(def)"})))

(defn- quoted-var-in
  "Pull `#'alias/handler-name` out of a route-table line."
  [text]
  (when-let [m (re-find #"#'([A-Za-z0-9_.<>*+!?-]+)/([A-Za-z0-9_.<>*+!?-]+)" text)]
    {:ns (nth m 1) :name (nth m 2)}))

;; @spec MCP-OP-THREAD-004
(defn- rank-test-hit
  "Rank one test witness by EVIDENCE KIND, never by hit order.

  A `deftest` that CALLS the handler outranks one that only asserts a string
  appears. E-THREAD's frozen oracle got exactly this backwards and three agents
  overruled it; the ranking is the correction made mechanical."
  [member handler-name]
  (let [body (or (:body member) "")]
    (cond
      (and handler-name
           (re-find (re-pattern (str "\\(\\s*(?:[A-Za-z0-9_.<>*+!?-]+/)?"
                                     (quote-literal handler-name) "\\b"))
                    body))
      {:rank 3 :evidence (str "form(deftest,CALLS-" handler-name ")")}

      (str/includes? body "deftest") {:rank 2 :evidence "form(deftest,string-assert)"}
      (re-find #"(?m)^\s*(?:test|it)\s*\(" body) {:rank 2 :evidence "test(js)"}
      :else {:rank 1 :evidence "identifier"})))

(defn- anchor-for
  "Where the NEW sibling goes -- the one fact neither a search nor a body carries.

  `after:L<n>` is an insertion point immediately AFTER line n. `in-form:La-Lb`
  names the enclosing table a new entry joins."
  [cache member]
  (let [{:keys [file from to]} member
        {:keys [ok source]} (read-source cache file)]
    (if-not (and ok (clojure-path? file))
      (str "after:L" to)
      (let [records (try (outline/top-level-form-records
                           file source {} {:include-source? false})
                         (catch Exception _ nil)
                         (catch Error _ nil))
            enclosing (some (fn [r]
                              (when (and (:line r) (:end-line r)
                                         (<= (:line r) (:hit_line member) (:end-line r))
                                         (> (- (:end-line r) (:line r))
                                            (- to from)))
                                r))
                            records)]
        (if enclosing
          (str "after:L" to " in-form:L" (:line enclosing) "-L" (:end-line enclosing))
          (str "after:L" to))))))

(defn- secondary-row
  [member]
  (select-keys member [:file :from :to :evidence :bytes :refetch]))

(defn- found-leg
  [base cache ranked searches unreadable]
  (let [primary (first ranked)]
    (merge (dissoc base :globs)
           (dissoc primary :rank)
           {:status "FOUND"
            :searches [(last searches)]
            :unreadable unreadable
            :anchor (anchor-for cache primary)
            :also (mapv secondary-row (take 4 (rest ranked)))})))

;; @spec MCP-OP-THREAD-004
;; @spec MCP-OP-THREAD-008
(defn resolve-leg
  "One leg: FOUND with an exact range, a boundary label, a sha256 of the body
  bytes and the body; or ABSENT with every search that was run."
  [cache paths seeds leg {:keys [handler-name]}]
  (let [{:keys [id kind globs]} leg
        member-opts {:narrow? (contains? #{:use :route} kind)}
        joined (when (and (= kind :handler) handler-name)
                 [["handler-join"
                   (str "\\(defn-? +" (quote-literal handler-name) "\\b")]])
        searches (concat joined (searches-for-kind kind seeds))
        base {:id id :leg_kind (name kind) :globs globs}]
    (if (empty? searches)
      (merge base {:status "ABSENT"
                   :searches ["no seed of the kind this leg needs"]
                   :unreadable []})
      (loop [[[label regex :as s] & more] searches
             ran []
             unreadable []]
        (if (nil? s)
          (merge base {:status "ABSENT" :searches ran :unreadable unreadable})
          (let [result (scan cache paths globs regex)
                hits (:hits result)
                ran' (conj ran (render-search globs [label regex]))
                unreadable' (into unreadable (:unreadable result))]
            (cond
              (and (= kind :def) (seq hits))
              (let [{hop-hits :hits hop-evidence :evidence extra :extra-search}
                    (alias-hop cache paths globs (:identifiers seeds) hits)]
                (if (seq hop-hits)
                  (found-leg base cache
                             (vec (keep #(hit->member cache % hop-evidence member-opts) hop-hits))
                             ran' unreadable')
                  (merge base {:status "ABSENT"
                               :evidence hop-evidence
                               :searches (cond-> ran'
                                           extra (conj (render-search globs extra)))
                               :unreadable unreadable'})))

              (seq hits)
              (let [members (keep #(hit->member cache % label member-opts) hits)
                    ranked (if (= kind :test)
                             (->> members
                                  (map #(merge % (rank-test-hit % handler-name)))
                                  (sort-by (juxt (comp - :rank) :file :from))
                                  vec)
                             (vec members))]
                (if (empty? ranked)
                  (recur more ran' unreadable')
                  (found-leg base cache ranked ran' unreadable')))

              :else (recur more ran' unreadable'))))))))

;; ---------------------------------------------------------------------------
;; The route -> handler join, run before the legs so the handler leg can use it
;; ---------------------------------------------------------------------------

(defn derive-handler
  "The handler Var the route table actually NAMES.

  Without this the handler leg searches every mention of the subject in src/ and
  buries the one form that answers the question."
  [cache paths conventions seeds]
  (let [route-leg (first (filter #(= :route (:kind %)) (:legs conventions)))]
    (when route-leg
      (some (fn [[_ regex]]
              (let [hits (:hits (scan cache paths (:globs route-leg) regex))]
                (some (fn [h]
                        (when-let [v (quoted-var-in (:text h))]
                          (assoc v :from (str (:file h) ":" (:line h)))))
                      hits)))
            (searches-for-kind :route seeds)))))

;; ---------------------------------------------------------------------------
;; The sibling: the neighbouring feature the subject must mirror
;; ---------------------------------------------------------------------------

(defn- adjacent-route-seed
  "The route entry beside the located one in the same table."
  [cache route-leg]
  (when (and route-leg (= "FOUND" (:status route-leg)))
    (let [{:keys [ok lines]} (read-source cache (:file route-leg))]
      (when ok
        (let [hit (:hit_line route-leg)
              nearby (concat (range (dec hit) (max 0 (- hit 4)) -1)
                             (range (inc hit) (min (count lines) (+ hit 4))))]
          (some (fn [ln]
                  (when-let [m (re-find #"\"(/[A-Za-z0-9_./-]+)\""
                                        (nth lines (dec ln) ""))]
                    {:seed (second m) :at (str (:file route-leg) ":" ln)}))
                nearby))))))

;; ---------------------------------------------------------------------------
;; The rules row: the wiring contract a grep script cannot produce
;; ---------------------------------------------------------------------------

(def ^:private intent-comment-pattern
  #"(?:INTENT|INTENT-TEST|@spec)[:\s]+([A-Z][A-Z0-9]*(?:-[A-Z0-9]+)+)")

;; @spec MCP-OP-THREAD-010
(defn intents-in
  "Every INTENT identifier in a body and in the comment lines immediately above
  its form. The E-THREAD T5 finding: an `INTENT:` comment two lines above a
  function was the only route to two legs, and no search on the seed reaches it."
  [cache member]
  (let [{:keys [ok lines]} (read-source cache (:file member))
        above (when ok
                (let [start (or (:comment_start member) (:from member) 1)
                      first-line (max 1 (- start 6))]
                  (str/join "\n" (subvec (vec lines)
                                         (dec first-line)
                                         (max (dec first-line) (dec start))))))]
    (->> (concat (re-seq intent-comment-pattern (or (:body member) ""))
                 (re-seq intent-comment-pattern (or above "")))
         (map second)
         distinct
         vec)))

(defn- namespaced-calls
  "Distinct alias-qualified calls in one Clojure body -- the path the handler
  actually routes through."
  [body]
  (->> (re-seq #"\(([a-z][A-Za-z0-9_.-]*/[A-Za-z0-9_.<>*+!?-]+)" (or body ""))
       (map second)
       distinct
       (take 24)
       vec))

(defn- refusal-statuses
  [body]
  (->> (re-seq #":status\s+(\d{3})" (or body ""))
       (map second)
       distinct
       sort
       vec))

;; @spec MCP-OP-THREAD-010
(defn governance-rows
  "The governance tail: the intent registry rows, the intent-contract test and
  the test target -- not code, and the transcript shows the agent needed all of
  it after it had the five owners. Ranges only; a registry row is never inlined
  whole."
  [cache paths conventions intents seeds]
  (let [globs (get-in conventions [:governance :globs])]
    (if (empty? globs)
      []
      (let [needles (concat intents (:identifiers seeds) (:routes seeds))
            regex (alternation needles)]
        (if-not regex
          []
          (->> (:hits (scan cache paths globs regex))
               (map (fn [h]
                      {:file (:file h)
                       :line (:line h)
                       :match (subs (:text h) 0 (min 64 (count (:text h))))
                       :refetch (refetch-command (:file h)
                                                 (max 1 (- (:line h) 4))
                                                 (+ (:line h) 20))}))
               (take 6)
               vec))))))

;; @spec MCP-OP-THREAD-010
(defn build-rules
  "The wiring contract: what the handler routes through, what it refuses, the
  INTENT ids present in the located bodies resolved to their registry rows, and
  the one axis on which the new feature differs from its sibling."
  [cache paths conventions seeds legs axis]
  (let [found (filter #(= "FOUND" (:status %)) legs)
        handler (first (filter #(= "handler" (:leg_kind %)) legs))
        intents (vec (distinct (mapcat #(intents-in cache %) found)))]
    {:durable_path (if (and handler (= "FOUND" (:status handler)))
                     (namespaced-calls (:body handler))
                     [])
     :refusal_statuses (if (and handler (= "FOUND" (:status handler)))
                         (refusal-statuses (:body handler))
                         [])
     :intents intents
     :governance (governance-rows cache paths conventions intents seeds)
     :axis axis
     :assert (str "before any edit, re-hash each leg's line range and compare to"
                  " its sha256; a mismatch is a REFUSAL (stale pre-image), never"
                  " a retry")}))

;; ---------------------------------------------------------------------------
;; Assembling one thread
;; ---------------------------------------------------------------------------

(defn elision-class
  [leg]
  (or (:elide leg)
      (case (:kind leg)
        :test :tests
        :use :menu
        :def :js-function
        :route :route
        :handler :handler
        :menu)))

(defn resolve-thread
  "The five legs of one subject, in the convention set's declared order."
  [cache paths conventions seeds handler]
  (mapv (fn [leg]
          (assoc (resolve-leg cache paths seeds leg {:handler-name (:name handler)})
                 :elide (elision-class leg)))
        (:legs conventions)))

;; @spec MCP-OP-THREAD-013
(defn thread-status
  "COMPLETE only when every declared leg is FOUND. Computed from the leg vector,
  never written as a literal."
  [legs]
  (let [total (count legs)
        found (count (filter #(= "FOUND" (:status %)) legs))
        missing (mapv :id (remove #(= "FOUND" (:status %)) legs))]
    {:status (if (= found total)
               (format "COMPLETE (%d of %d)" found total)
               (format "INCOMPLETE (%d of %d)" found total))
     :complete (= found total)
     :legs_found found
     :legs_declared total
     :legs_missing missing}))

;; @spec MCP-OP-THREAD-009
(defn resolve-sibling
  "The neighbouring feature the subject should mirror, bodies elided to ranges.

  The sibling row never counts toward the five-leg status: it is context for the
  edit, not a leg of the thread."
  [cache paths conventions seeds legs mirror]
  (let [route-leg (first (filter #(= "route" (:leg_kind %)) legs))
        rule (or (get-in conventions [:sibling :rule]) :adjacent-route-entry)
        derived (when (nil? mirror)
                  (case rule
                    :adjacent-route-entry (adjacent-route-seed cache route-leg)
                    nil))
        seed (or mirror (:seed derived))]
    (if (nil? seed)
      {:status "ABSENT"
       :rule (name rule)
       :reason (str "no sibling resolved by rule " (name rule)
                    "; pass mirror to name one explicitly")}
      (let [sib-seeds (split-seeds [seed])
            sib-handler (derive-handler cache paths conventions sib-seeds)
            sib-legs (resolve-thread cache paths conventions sib-seeds sib-handler)]
        {:status "FOUND"
         :rule (if mirror "explicit-mirror" (name rule))
         :seed seed
         :at (:at derived)
         :legs (mapv (fn [l]
                       (cond-> (select-keys l [:id :status :file :from :to
                                               :evidence :sha256 :bytes :anchor])
                         (= "FOUND" (:status l))
                         (assoc :refetch (refetch-command (:file l) (:from l) (:to l)))))
                     sib-legs)}))))

;; ---------------------------------------------------------------------------
;; Rendering: the text block, and the completion pass that makes it a superset
;; ---------------------------------------------------------------------------

(defn- leaf-paths
  "Every scalar leaf of a structured receipt as `[path value]`."
  [node path]
  (cond
    (map? node) (mapcat (fn [[k v]] (leaf-paths v (conj path (name k)))) node)
    (sequential? node) (mapcat (fn [i v] (leaf-paths v (conj path (str i))))
                               (range) node)
    (or (string? node) (number? node) (boolean? node)) [[path node]]
    :else []))

;; @spec MCP-OP-THREAD-012
(defn ensure-superset
  "Guarantee the text block carries every leaf of the structured receipt.

  Not a rule someone must remember: whatever the designed lines forget is
  appended here, so a text-reading client can never be told less than a
  structure-reading one. Its own witness reintroduces a dropped field and
  watches the line appear."
  [text result]
  (let [missing (->> (leaf-paths result [])
                     (remove (fn [[_ v]] (str/includes? text (str v))))
                     (map (fn [[p v]] (str (str/join "." p) "=" (pr-str v))))
                     distinct)]
    (if (seq missing)
      (str text "\n structured-only · " (str/join " · " missing))
      text)))

(defn- leg-line
  [leg]
  (if (= "FOUND" (:status leg))
    (str "leg " (:id leg) "  " (:file leg) " L" (:from leg) "-L" (:to leg)
         " sha256:" (:sha256 leg)
         " evid=" (:evidence leg)
         " boundary=" (:boundary leg)
         " bytes=" (:bytes leg)
         " anchor=" (:anchor leg)
         (when (:form_name leg) (str " form=" (:form_name leg)))
         "\n  found by: " (str/join "\n  found by: " (:searches leg))
         (if (:body leg)
           (str "\n  BODY<<\n" (:body leg) "\n  >>")
           (str "\n  BODY ELIDED reason=" (:elided_reason leg)
                " range=L" (:from leg) "-L" (:to leg)
                " bytes=" (:bytes leg)
                " refetch=" (:refetch leg))))
    (str "leg " (:id leg) "  ABSENT"
         (when (:evidence leg) (str " evid=" (:evidence leg)))
         "\n  searched: "
         (str/join "\n  searched: " (:searches leg))
         (when (seq (:unreadable leg))
           (str "\n  unreadable: "
                (str/join ", " (map #(str (:file %) " (" (:reason %) ")")
                                    (:unreadable leg))))))))

(defn- also-line
  [leg]
  (when (seq (:also leg))
    (str "also " (:id leg) " "
         (str/join " · "
                   (map #(str (:file %) ":L" (:from %) "-L" (:to %)
                              " evid=" (:evidence %)
                              " refetch=" (:refetch %))
                        (:also leg)))
         "  — BODIES ELIDED reason=rank(secondary witness)")))

(defn render-receipt
  "The visible receipt. The completion pass at the end is the ratchet: whatever
  the designed lines forget, the text still carries, because a text-reading
  client must never be told less than a structure-reading one."
  [result]
  (let [legs (:legs result)
        header (str "receipt feature-thread/v2  subject=" (:subject result)
                    (when (seq (:also_seeds result))
                      (str " also=" (str/join "," (:also_seeds result))))
                    "  root=" (:workspace_root result)
                    "  repo=" (:repo_label result)
                    "  budget=" (:budget_bytes result) "B"
                    "  used=" (:receipt_bytes result) "B"
                    "  text=" (:text_bytes result) "B"
                    "  structured=" (:structured_bytes result) "B"
                    "  status=" (:status result))
        body-lines (remove nil? (concat (map leg-line legs) (map also-line legs)))
        sibling (:sibling result)
        sibling-line
        (if (= "FOUND" (:status sibling))
          (str "sibling " (:seed sibling) " rule=" (:rule sibling)
               (when (:at sibling) (str " at=" (:at sibling)))
               "  legs: "
               (str/join " · " (map #(str (:id %) " " (:status %)
                                          (when (= "FOUND" (:status %))
                                            (str " " (:file %) ":L" (:from %)
                                                 "-L" (:to %)
                                                 " sha256:" (:sha256 %)
                                                 " refetch=" (:refetch %))))
                                    (:legs sibling)))
               "  — BODIES ELIDED to ranges")
          (str "sibling ABSENT rule=" (:rule sibling) " reason=" (:reason sibling)))
        rules (:rules result)
        rules-line
        (str "rules durable_path=" (pr-str (:durable_path rules))
             " refusal_statuses=" (pr-str (:refusal_statuses rules))
             " intents=" (pr-str (:intents rules))
             (when-let [axis (:axis rules)]
               (str "\n  axis " (:name axis)
                    " precedents=" (pr-str (:precedents axis))))
             (when (seq (:governance rules))
               (str "\n  governance "
                    (str/join "\n  governance "
                              (map #(str (:file %) ":" (:line %) "  " (:match %)
                                         "  refetch=" (:refetch %))
                                   (:governance rules)))))
             "\nassert " (:assert rules))
        elisions (:elided result)
        elision-lines (map #(str "elided " (:leg %) " " (:bytes %)
                                 "B reason=" (:reason %)
                                 " range=L" (:from %) "-L" (:to %)
                                 " sha256:" (:sha256 %)
                                 " refetch=" (:refetch %))
                           elisions)
        designed (str/join "\n" (remove nil?
                                        (concat [header] body-lines
                                                [sibling-line rules-line]
                                                elision-lines)))
        ]
    (ensure-superset designed
                     (dissoc result :receipt_bytes :text_bytes
                             :structured_bytes))))

;; ---------------------------------------------------------------------------
;; Budget: measured on the FINAL rendered receipt, elided in a stated order
;; ---------------------------------------------------------------------------

(defn measure
  "Render, measure, and settle the byte counts on the receipt they describe.

  The numbers are self-referential (they appear in the text they count), so they
  are reached by fixpoint rather than estimated: at most six rounds, and the
  digit counts settle on the second."
  [result]
  (loop [n 0 tb 0 sb 0 rounds 0]
    (let [candidate (assoc result :receipt_bytes n
                           :text_bytes tb :structured_bytes sb)
          text (render-receipt candidate)
          t (utf8-bytes text)
          st (utf8-bytes (json/generate-string candidate))
          total (+ t st)]
      (if (or (and (= total n) (= t tb) (= st sb)) (>= rounds 6))
        [text (assoc result :receipt_bytes total
                     :text_bytes t :structured_bytes st)
         t]
        (recur total t st (inc rounds))))))

(defn- elide-leg
  [leg reason]
  (if (and (= "FOUND" (:status leg)) (:body leg))
    [(-> leg
         (dissoc :body)
         (assoc :elided_reason reason))
     {:leg (:id leg) :bytes (:bytes leg) :reason reason
      :from (:from leg) :to (:to leg) :sha256 (:sha256 leg)
      :refetch (:refetch leg)}]
    [leg nil]))

;; @spec MCP-OP-THREAD-011
(defn apply-elision
  "Apply one step of the stated elision order. Returns `[result applied?]`."
  [result class]
  (case class
    :secondary-tests
    (let [dropped (reduce + 0 (map #(count (:also %)) (:legs result)))]
      (if (zero? dropped)
        [result false]
        [(-> result
             (update :legs #(mapv (fn [l] (assoc l :also [])) %))
             (update :elided conj {:leg "secondary-witnesses"
                                   :bytes 0
                                   :reason (str "public-budget; " dropped
                                                " secondary witness rows dropped")
                                   :from 0 :to 0 :sha256 "n/a"
                                   :refetch "re-run feature_thread with a larger budget_bytes"}))
         true]))

    :sibling
    (if (seq (get-in result [:sibling :legs]))
      [(-> result
           (assoc-in [:sibling :legs] [])
           (assoc-in [:sibling :elided] true)
           (update :elided conj {:leg "sibling"
                                 :bytes 0
                                 :reason "public-budget"
                                 :from 0 :to 0 :sha256 "n/a"
                                 :refetch (str "feature_thread subject="
                                               (get-in result [:sibling :seed]))}))
       true]
      [result false])

    (let [legs (:legs result)
          idx (first (keep-indexed (fn [i l] (when (= class (:elide l)) i)) legs))]
      (if (nil? idx)
        [result false]
        (let [[leg' entry] (elide-leg (nth legs idx) "public-budget")]
          (if (nil? entry)
            [result false]
            [(-> result
                 (assoc :legs (assoc (vec legs) idx leg'))
                 (update :elided conj entry))
             true]))))))

;; @spec MCP-OP-THREAD-011
(defn fit-to-budget
  "Render, measure, and if the receipt exceeds its budget elide bodies in the
  stated order until it fits, naming every elision.

  When every body has been elided and the receipt still does not fit, the verb
  REFUSES. It never truncates a body mid-form and never cuts in silence."
  [result budget]
  (loop [current (assoc result :elided [])
         remaining elision-order]
    (let [[text measured total] (measure current)]
      (cond
        (and (<= total budget)
             (<= (:structured_bytes measured) trunk-public-byte-budget))
        [text measured]

        (empty? remaining)
        (let [refusal {:ok false
                       :operation "feature_thread"
                       :error_type "feature-thread-budget-exceeded"
                       :error (str "the receipt is " total
                                   " bytes with every body elided, above the"
                                   " budget of " budget)
                       :budget_bytes budget
                       :receipt_bytes total
                       :subject (:subject result)
                       :status (:status result)
                       :remedy (str "Raise budget_bytes (hard cap "
                                    hard-cap-bytes ") or narrow scope.paths.")}]
          [(str "feature_thread refused · " (:error_type refusal) " · "
                (:error refusal) "\nremedy · " (:remedy refusal)
                "\nfacts · budget_bytes=" budget " receipt_bytes=" total
                " subject=" (:subject result) " status=" (:status result))
           refusal])

        :else
        (let [[next-result applied?] (apply-elision current (first remaining))]
          (recur (if applied? next-result current) (rest remaining)))))))

;; ---------------------------------------------------------------------------
;; Admission
;; ---------------------------------------------------------------------------

(def elapsed-reserve-bytes
  "Bytes reserved in the budget for the operation clock line the finalizer adds
  after the receipt is measured."
  96)

(def allowed-fields
  #{:subject :also :scope :config :budget_bytes :include_bodies :mode :mirror
    :axis :workspace_root})

(defn- refuse
  [error-type message extra]
  (merge {:ok false
          :operation "feature_thread"
          :error_type error-type
          :error message
          :source_unchanged true}
         extra))

;; @spec MCP-OP-THREAD-001
;; @spec MCP-OP-THREAD-002
(defn admit
  "Validate the request before any file is read. Every refusal names the field."
  [params]
  (let [unknown (sort (map name (remove allowed-fields (keys params))))
        {:keys [subject also scope budget_bytes include_bodies mode mirror]} params]
    (cond
      (seq unknown)
      (refuse "feature-thread-unknown-field"
              (str "feature_thread does not accept: " (str/join ", " unknown))
              {:unknown_fields (vec unknown)
               :accepted_fields (vec (sort (map name allowed-fields)))
               :remedy "Remove the fields this verb's schema does not declare."})

      (not (and (string? subject) (seq (str/trim subject))))
      (refuse "feature-thread-invalid-subject"
              "subject must be a non-blank identifier or route"
              {:remedy "Pass the identifier or the route the feature is named by."})

      (and (some? also) (not (and (sequential? also) (every? string? also))))
      (refuse "feature-thread-invalid-also"
              "also must be an array of additional identifier or route seeds"
              {:remedy "Pass also as a vector of strings, or omit it."})

      (and (some? scope) (not (map? scope)))
      (refuse "feature-thread-invalid-scope"
              "scope must be an object with optional workspace_root and paths"
              {:remedy "Pass scope as {\"workspace_root\": ..., \"paths\": [...]}."})

      (and (some? (:paths scope))
           (not (and (sequential? (:paths scope)) (every? string? (:paths scope)))))
      (refuse "feature-thread-invalid-scope"
              "scope.paths must be an array of repo-relative directory paths"
              {:remedy "Pass scope.paths as a vector of strings, or omit it."})

      (and (some? budget_bytes)
           (not (and (integer? budget_bytes) (pos? budget_bytes))))
      (refuse "feature-thread-invalid-budget"
              "budget_bytes must be a positive integer"
              {:default_budget_bytes default-budget-bytes
               :hard_cap_bytes hard-cap-bytes
               :remedy "Pass a positive integer, or omit it for the default."})

      (and (integer? budget_bytes) (> budget_bytes hard-cap-bytes))
      (refuse "feature-thread-budget-above-cap"
              (str "budget_bytes " budget_bytes " is above the hard cap of "
                   hard-cap-bytes "; it is never silently clamped")
              {:budget_bytes budget_bytes
               :hard_cap_bytes hard-cap-bytes
               :remedy (str "Request at most " hard-cap-bytes " bytes.")})

      (and (some? include_bodies) (not (boolean? include_bodies)))
      (refuse "feature-thread-invalid-include-bodies"
              "include_bodies must be true or false"
              {:remedy "Omit include_bodies, or pass a boolean."})

      (and (some? mode) (not (contains? #{"edit-basis" "locations"} mode)))
      (refuse "feature-thread-invalid-mode"
              "mode must be \"edit-basis\" (bodies) or \"locations\" (ranges only)"
              {:accepted_modes ["edit-basis" "locations"]
               :remedy "Omit mode, or pass one of the two receipt modes."})

      (and (some? mirror) (not (string? mirror)))
      (refuse "feature-thread-invalid-mirror"
              "mirror must be the seed of the feature this one should mirror"
              {:remedy "Pass mirror as a string, or omit it."})

      :else {:ok true})))

;; ---------------------------------------------------------------------------
;; Execution
;; ---------------------------------------------------------------------------

(defonce runtime-config (atom nil))

(defn init!
  "Set the live feature_thread configuration. Passing nil disarms it."
  [config]
  (reset! runtime-config config))

(defn- strip-bodies
  [legs]
  (mapv (fn [l]
          (if (and (= "FOUND" (:status l)) (:body l))
            (-> l (dissoc :body) (assoc :elided_reason "mode=locations"))
            l))
        legs))

(defn execute-request
  "Resolve one feature thread. Pure with respect to the workspace: it reads
  files and writes nothing."
  [config params]
  (let [normalized (json/parse-string (json/generate-string params) true)
        admission (admit normalized)]
    (if-not (:ok admission)
      admission
      (let [{:keys [subject also scope budget_bytes include_bodies mode mirror axis]}
            normalized
            root (or (:workspace_root scope)
                     (:workspace_root normalized)
                     (:project-root config))
            budget (or budget_bytes default-budget-bytes)
            bodies? (if (some? include_bodies)
                      include_bodies
                      (not= mode "locations"))]
        (cond
          (not (and (string? root) (seq root)))
          (refuse "feature-thread-workspace-unresolved"
                  "no workspace root: pass scope.workspace_root"
                  {:remedy "Pass an existing absolute scope.workspace_root."})

          (not (.isDirectory (io/file root)))
          (refuse "invalid-workspace-root"
                  (str "workspace root is not an existing directory: " root)
                  {:workspace_root (str root)
                   :remedy "Pass an existing absolute scope.workspace_root."})

          :else
          (let [conv (load-conventions root (:config normalized))]
            (if-not (:ok conv)
              (merge (refuse (:error_type conv) (:error conv)
                             (dissoc conv :ok :error_type :error))
                     {:workspace_root (str root)})
              (let [conventions (:conventions conv)
                    candidate-globs (vec (distinct
                                           (concat
                                             (mapcat :globs (:legs conventions))
                                             (get-in conventions
                                                     [:governance :globs]))))
                    walk (walk-relative-paths root (:exclude-dirs conventions)
                                              candidate-globs)]
                (if-not (:ok walk)
                  (merge (refuse (:error_type walk) (:error walk)
                                 (dissoc walk :ok :error_type :error))
                         {:workspace_root (str root)})
                  (let [paths (filterv #(under-scope? % (:paths scope)) (:paths walk))
                        cache (make-cache root)
                        seeds (split-seeds (into [subject] (or also [])))
                        handler (derive-handler cache paths conventions seeds)
                        legs (resolve-thread cache paths conventions seeds handler)
                        legs (if bodies? legs (strip-bodies legs))
                        status (thread-status legs)
                        sibling (resolve-sibling cache paths conventions seeds
                                                 legs mirror)
                        axis' (or axis (:axis conventions))
                        rules (build-rules cache paths conventions seeds legs axis')
                        base (merge
                               {:ok true
                                :operation "feature_thread"
                                :receipt "feature-thread/v2"
                                :subject subject
                                :also_seeds (vec (or also []))
                                :mode (if bodies? "edit-basis" "locations")
                                :workspace_root (str root)
                                :repo_label (or (:repo-label conventions)
                                                (.getName (io/file root)))
                                :conventions_source (:conventions_source conventions)
                                :scanned_files (count paths)
                                :budget_bytes budget
                                :route_handler (when handler
                                                 (str (:ns handler) "/" (:name handler)))
                                :legs legs
                                :sibling sibling
                                :rules rules
                                :elided []}
                               status)
                        [_ fitted] (fit-to-budget base (- budget elapsed-reserve-bytes))]
                    fitted))))))))))

;; ---------------------------------------------------------------------------
;; The MCP entrance
;; ---------------------------------------------------------------------------

;; @spec MCP-OP-THREAD-012
(defn summary
  "The text block. Re-rendered from the structured receipt, so the two faces of
  the result cannot disagree, and completed so the text is a superset."
  [result]
  (let [elapsed (str "\nelapsed " (mcp-operation/format-elapsed-ms
                                    (:elapsed_ms result)))]
    (if (:ok result)
      (str (render-receipt result) elapsed)
      (ensure-superset
        (str "feature_thread refused · " (:error_type result)
             "\n→ " (:error result)
             (when-let [remedy (:remedy result)] (str "\nremedy · " remedy))
             elapsed)
        result))))

(def feature-thread-schema
  {:type "object"
   :additionalProperties false
   :properties
   {"subject" {:type "string" :minLength 1
               :description (str "The identifier or route the feature is named"
                                 " by. A value beginning with / is a route.")}
    "also" {:type "array" :items {:type "string" :minLength 1}
            :description "Additional identifier or route seeds for the same thread."}
    "scope" {:type "object"
             :additionalProperties false
             :properties {"workspace_root" {:type "string" :minLength 1}
                          "paths" {:type "array"
                                   :items {:type "string" :minLength 1}}}}
    "config" {:type "object"
              :description (str "Inline convention set. Omit to read"
                                " .clj-surgeon/feature-thread.edn under the"
                                " workspace root.")}
    "budget_bytes" {:type "integer" :minimum 1 :maximum 32768
                    :description (str "Receipt budget in UTF-8 bytes; default "
                                      "10240, hard cap 32768. Over budget,"
                                      " bodies are elided in a stated order and"
                                      " every elision is named.")}
    "include_bodies" {:type "boolean"
                      :description "false returns ranges only (mode=locations)."}
    "mode" {:type "string" :enum ["edit-basis" "locations"]}
    "mirror" {:type "string" :minLength 1
              :description "Seed of the feature this one should mirror."}
    "axis" {:type "object"
            :description (str "The one axis on which the new feature differs"
                              " from its sibling: {name, precedents}.")}
    "workspace_root" {:type "string" :minLength 1}}
   :required ["subject"]})

(def feature-thread-output-schema
  {:type "object"
   :properties
   {"ok" {:type "boolean"}
    "operation" {:type "string"}
    "receipt" {:type "string"}
    "subject" {:type "string"}
    "status" {:type "string"}
    "complete" {:type "boolean"}
    "legs_found" {:type "integer"}
    "legs_declared" {:type "integer"}
    "legs_missing" {:type "array" :items {:type "string"}}
    "legs" {:type "array" :items {:type "object"}}
    "sibling" {:type "object"}
    "rules" {:type "object"}
    "elided" {:type "array" :items {:type "object"}}
    "budget_bytes" {:type "integer"}
    "receipt_bytes" {:type "integer"}
    "workspace_root" {:type "string"}
    "error_type" {:type "string"}
    "error" {:type "string"}
    "remedy" {:type "string"}
    "elapsed_ms" {:type "number" :minimum 0}}
   :required ["elapsed_ms"]})

(def feature-thread-annotations
  {:title "Feature thread"
   :read-only true
   :destructive false
   :idempotent true
   :open-world false
   :return-direct false})

(def feature-thread-tool-description
  (str
    "Return one cross-language feature as an EDIT BASIS in a single call: the "
    "five leg roles this repository declares (menu caller, script function, "
    "route, handler, tests -- whatever its own conventions name), each FOUND "
    "with an exact line range, an evidence and boundary label, a sha256 of the "
    "body bytes and the body, or ABSENT with every search that was run quoted. "
    "Also: an anchor per leg saying where a NEW sibling goes; the sibling "
    "feature the subject should mirror, bodies elided to ranges; and a rules "
    "row -- the path the handler routes through, the statuses it refuses with, "
    "the INTENT identifiers found in comments above the located forms, and the "
    "governance rows (intent registry, contract test, test target) that a "
    "search on the subject never reaches. Clojure legs are PARSED to the "
    "enclosing top-level form, so the range is exact. Script legs are "
    "lexer-driven brace matches labelled brace-window(lexed,closed), "
    "downgrading to a labelled line window when the counter does not close; "
    "this verb never parses JavaScript and never presents a window as a matched "
    "body. Status is COMPLETE only when every declared leg is FOUND. Before any "
    "edit, re-hash each leg's range and compare it to the sha256 the receipt "
    "carried: a mismatch is a refusal for a stale pre-image, never a retry. "
    "Leg roles are repository data, read from .clj-surgeon/feature-thread.edn "
    "or passed inline as config; the verb infers no file roles of its own."))

;; @spec MCP-OP-THREAD-001
(defn handle-feature-thread
  "clojure-mcp callback handler retained as a Var for hot reload."
  [_exchange params callback]
  (mcp-operation/invoke!
    {:execute #(try
                 (execute-request @runtime-config params)
                 (catch Exception error
                   (refuse "feature-thread-failure"
                           (or (.getMessage error) (.getName (class error)))
                           {:remedy "Report this receipt; no source was read past the failure."}))
                 (catch Throwable error
                   (refuse "feature-thread-error"
                           (or (.getMessage error) (.getName (class error)))
                           {:remedy "Report this receipt; no source was read past the failure."})))
     :summarize summary
     :callback callback}))

;; @spec MCP-OP-THREAD-001
(def feature-thread-tool
  {:id :feature-thread
   :name "feature_thread"
   :description feature-thread-tool-description
   :schema feature-thread-schema
   :output-schema feature-thread-output-schema
   :annotations feature-thread-annotations
   :structured? true
   :tool-fn #'handle-feature-thread})
