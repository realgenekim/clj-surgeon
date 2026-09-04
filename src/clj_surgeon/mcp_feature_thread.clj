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

  The default is 24576 rather than the fleet's text-only soft figure of 10240,
  because this receipt carries more than the minimal edit basis it costed: the
  searches that found each leg, the governance rows, and the elision ledger.
  Round two raised it from 16384 after the Dequote/Format fixture measured SIX
  legs: at 16384 four bodies elided, including the JavaScript function the
  transcript re-read four times, which is exactly the call the verb exists to
  save. Round four raised it again, from 24576 to 28672, when `after_context`
  (MCP-OP-THREAD-036) put the four source lines each anchor points AT into the
  receipt: at 24576 the complete receipt measures 25298 bytes and every leg's
  after-context was cut, which is again the call the verb exists to save. 11264
  is a good explicit `budget_bytes` for a caller that wants only the ranges:
  with every body, every peer body and every anchor context elided the fixture
  receipt measures 10484 bytes of text, and the receipt REFUSES rather than
  truncate below that."
  28672)

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

(def script-extensions
  "The extensions this verb treats as a SCRIPT language: lexed, never parsed."
  #{"js" "mjs" "ts"})

;; @spec MCP-OP-THREAD-016
(def implementation-leg-id
  "The id of the leg every convention set gets for free."
  "implementation")

;; @spec MCP-OP-THREAD-016
(def implementation-clojure-globs
  "Where a Clojure DEFINITION named by a seed lives. Added to whatever script
  globs the convention set already declares, so a five-leg conventions file
  keeps working unchanged and still gets the definition leg."
  ["src/**/*.clj" "src/**/*.cljc"])

;; @spec MCP-OP-THREAD-017
(def governance-entry-ceiling-lines
  "Largest top-level EDN entry a governance row will anchor to. The registry is
  one vector of a thousand lines; the ENTRY is the map element the hit sits in,
  and a ceiling is what tells the two apart without a semantic parse."
  200)

;; @spec MCP-OP-THREAD-018
(def test-call-lookback-lines
  "Lines a script test witness scans BACKWARD for its enclosing
  `test(`/`it(`/`describe(` call before giving up and using the hit line."
  200)

;; @spec MCP-OP-THREAD-019

;; @spec MCP-OP-THREAD-026
(def after-context-lines
  "Source lines quoted AFTER an insertion anchor.

  An anchor says `after:L<n>` and stops. The MCP-attached replay arm re-read
  exactly those next lines before it could write its patch -- the indentation,
  the separator, whether the next entry is a `]` or another row -- which is a
  call this verb exists to save. Four is the middle of the 3-6 band the
  addendum asks for: enough to see the shape, small enough that six legs of it
  cost under a kilobyte."
  4)

(def receipt-tail-bytes
  "Width, in ASCII bytes, of the operation-clock tail the receipt always ends
  with.

  FIXED, and that is the whole point: the clock is stamped by the finalizer
  AFTER the receipt has measured itself, so a variable-width tail made
  `text_bytes` describe a text nobody was delivered (round-one review,
  finding 5: 15,392 claimed, 15,435 delivered). A constant-width tail is
  measurable before its value is known."
  96)

(def max-subject-chars
  "Longest `subject` this verb accepts, refused at ADMISSION with the field
  named. A 10,001-character subject used to compile into a regex, scan the
  whole tree for 333 ms, and only then be refused as a BUDGET error naming the
  wrong field (round-one review, finding 4)."
  512)

(def max-also-seeds
  "Most `also` seeds accepted in one request."
  32)

(def max-probe-identifiers
  "Most `probe` identifiers accepted in one request.

  Each one costs a scan of the whole candidate set, so this is a real ceiling
  and not decoration: it is smaller than `max-also-seeds` because a probe buys
  a single yes/no where a seed buys a whole leg."
  16)

(def max-verify-rows
  "Verify rows returned for one tests primary."
  4)

;; @spec MCP-OP-THREAD-011
;; @spec MCP-OP-THREAD-020
;; @spec MCP-OP-THREAD-047
(defn elision-order-for
  "The order bodies are dropped when the receipt does not fit.

  EDIT-AWARE, not merely cheap-first: what goes LAST is what the caller is about
  to type into — the handler, the script function the seed names, and the
  definition the seed names. What goes first is context the caller can re-fetch
  without losing the edit basis. Fixed and stated so an elision is never a
  surprise.

  Peer bodies move: unasked-for they are OPPORTUNISTIC and go first, ahead of
  even the sibling; asked for with `peer_bodies true` they are part of what the
  caller came for and outlive the sibling and the anchor context. A peer ROW is
  not in this order at all — it is about 120 bytes, it is the whole reason peers
  are in the receipt, and eliding it costs a call to get back."
  [peer-bodies-requested?]
  (if peer-bodies-requested?
    [:sibling :after-context :peers :verify-detail :governance-template
     :secondary-tests :next-call :menu :route :tests-js :tests :implementation
     :js-function :handler]
    [:peers :sibling :after-context :verify-detail :governance-template
     :secondary-tests :next-call :menu :route :tests-js :tests :implementation
     :js-function :handler]))

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

;; @spec MCP-OP-THREAD-024
(defn located?
  "A leg the receipt has a range and a body for -- FOUND, or the weaker
  CANDIDATE. Everything a caller can READ is located; only FOUND COUNTS."
  [leg]
  (contains? #{"FOUND" "CANDIDATE"} (:status leg)))

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

(defn script-path?
  [path]
  (contains? script-extensions (file-extension path)))

;; @spec MCP-OP-THREAD-018
(defn language-of
  "The LANGUAGE a path belongs to, for the one question this verb asks of it:
  are two witnesses in the same language or not. `clj`, `js`, or the extension
  itself when it is neither."
  [path]
  (let [ext (file-extension path)]
    (cond
      (contains? clojure-extensions ext) "clj"
      (contains? script-extensions ext) "js"
      :else ext)))

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
        ;; @spec MCP-OP-THREAD-033
        ;; The separator is the whole containment test. Without it a sibling
        ;; `<root>-evil` passes the prefix check, and a symlink pointing at it
        ;; yields `evil/stolen.clj` -- a file outside the root reported as one
        ;; inside it (round-three review, 3.5).
        root-prefix (str root-path java.io.File/separator)
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
                        (when (str/starts-with? abs root-prefix)
                          (let [relative (str/replace
                                           (subs abs (count root-prefix))
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
      ;; @spec MCP-OP-THREAD-033
      ;; De-duplicated: a symlink to a directory inside the tree canonicalises
      ;; to the same file and would otherwise be walked, read and searched twice.
      {:ok true :paths (vec (sort (distinct @acc)))})))

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

;; @spec MCP-OP-THREAD-023
(def ^:private regex-context-keywords
  "Keywords after which a `/` begins a REGEX, not a division.

  Every one of these ends in an identifier character, so a punctuation-only
  preceding-token test reads `return /[}]/` as a division — and then counts the
  `}` inside the regex as a closing brace. That is the ONE failure mode this
  lexer promised it did not have: a WRONG range labelled `closed`, with a
  sha256 over the truncation offered as an edit pre-image. Found by the
  round-one review (Opus, 2026-09-04) on `return /[}]/.test(s)`."
  #{"return" "typeof" "case" "in" "of" "new" "delete" "void" "yield" "do"
    "else" "instanceof" "throw" "await"})

(defn- identifier-char?
  [^Character c]
  (or (Character/isLetterOrDigit c) (= c \_) (= c \$)))

;; @spec MCP-OP-THREAD-023
(defn word-before
  "The identifier ending immediately before `index` in `source`, or nil.

  Whitespace is skipped; anything that is not an identifier character ends the
  scan. Used only to answer one question: is this `/` in regex context."
  [^String source index]
  (let [end (loop [j (dec index)]
              (if (and (>= j 0) (Character/isWhitespace (.charAt source j)))
                (recur (dec j))
                j))]
    (when (and (>= end 0) (identifier-char? (.charAt source end)))
      (let [start (loop [j end]
                    (if (and (>= j 0) (identifier-char? (.charAt source j)))
                      (recur (dec j))
                      (inc j)))]
        (subs source start (inc end))))))

;; @spec MCP-OP-THREAD-006
(defn lexed-brace-match
  "Scan forward from `start-index` in `source` and return the index just past the
  brace that closes the first `{` found, or nil.

  `:mode` is one of :code :line-comment :block-comment :single :double
  :template :regex :regex-class, plus a stack for `${` interpolation."
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

            ;; @spec MCP-OP-THREAD-031
            ;; A `/` inside a character class does NOT end a JavaScript regex:
            ;; `/[/}]/` is valid. Ending the literal at the inner slash returns
            ;; the scanner to :code and counts the `}` as the function's closing
            ;; brace -- a truncated body labelled `closed`, with a sha256 over
            ;; the truncation (round-three review, B1').
            :regex
            (cond
              (= c \\) (recur (+ i 2) :regex depth opened? template-stack prev-significant)
              (= c \newline) nil
              (= c \[) (recur (inc i) :regex-class depth opened? template-stack prev-significant)
              (= c \/) (recur (inc i) :code depth opened? template-stack \/)
              :else (recur (inc i) :regex depth opened? template-stack prev-significant))

            :regex-class
            (cond
              (= c \\) (recur (+ i 2) :regex-class depth opened? template-stack prev-significant)
              (= c \newline) nil
              (= c \]) (recur (inc i) :regex depth opened? template-stack prev-significant)
              :else (recur (inc i) :regex-class depth opened? template-stack prev-significant))

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
                       (contains? regex-preceding-chars prev-significant)
                       (contains? regex-context-keywords
                                  (word-before source i))))
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

(def ^:private test-call-pattern
  #"^\s*(?:async\s+)?(?:test|it|describe)\s*\(")

;; @spec MCP-OP-THREAD-018
(defn enclosing-test-call-line
  "The 1-based line of the `test(`/`it(`/`describe(` call that ENCLOSES
  `hit-line`, or nil.

  A script test witness is almost never the assertion line the search matched:
  it is the test case that assertion lives in, and an anchor after the assertion
  would insert the new test INSIDE the old one. Scanning backward for the call
  is lexical and bounded; failing to find one leaves the hit line, labelled."
  [lines hit-line]
  (some (fn [ln]
          (when (re-find test-call-pattern (nth lines (dec ln) ""))
            ln))
        (range hit-line (max 0 (- hit-line test-call-lookback-lines)) -1)))

(defn- script-body*
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

;; @spec MCP-OP-THREAD-006
(defn script-body
  "The body of a script-language leg at 1-based `hit-line`.

  Returns `{:from :to :body :boundary}` where `:boundary` is
  `\"brace-window(lexed,closed)\"` for a closed lexed match or
  `\"line-window(+/-N, unclosed at L<n>)\"` for the honest downgrade. Never a
  claimed parse."
  ([source lines hit-line] (script-body source lines hit-line nil))
  ([source lines hit-line {:keys [test-call?]}]
   (let [call-line (when test-call? (enclosing-test-call-line lines hit-line))
         hit-line (or call-line hit-line)
         result (script-body* source lines hit-line)]
     (cond-> result
       call-line (update :boundary str ", test-call at L" call-line)))))

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

;; @spec MCP-OP-THREAD-044
(defn route-entry-hit?
  "True when the route literal at 1-based `line`, 0-based `col`, sits directly
  inside a VECTOR or a MAP — the shape of a route-table entry.

  A route literal can appear in a Clojure file in exactly two ways: as the
  first element of a table entry, or as a STRING inside some other form — a
  docstring, a log message, an error string. The bracket that immediately
  encloses the occurrence tells the two apart, and nothing else does. Round-five
  review, finding 3 (BLOCKING): `saveDraft` on social-media-writer @2df99c98
  reported `src/writer/routes.clj:L392-L445` — the body of
  `(defn handle-save \"POST /api/save …\" …)` — as evidence `route-literal`, and
  the thread as COMPLETE (5 of 5). The real entry was 1,729 lines further down.

  A non-Clojure route file is not decided here: this verb never parses another
  language, and claiming a structural fact it did not check would be the same
  false green in a different file extension."
  [relative ^String source line col]
  (if-not (clojure-path? relative)
    true
    (let [offsets (line-start-offsets source)
          offset (+ (nth offsets (dec line) 0) (or col 0))
          containing (filter (fn [[o c]] (<= o offset c))
                             (clj-bracket-spans source))]
      (boolean
        (when (seq containing)
          (let [[o _] (apply max-key first containing)]
            (contains? #{\[ \{} (.charAt source (int o)))))))))

(defn body-at
  "Body for one hit, routed by file kind."
  ([relative source lines hit-line] (body-at relative source lines hit-line nil))
  ([relative source lines hit-line opts]
   (if (clojure-path? relative)
     (clojure-body relative source lines hit-line opts)
     (script-body source lines hit-line opts))))

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

(defn- escaping-glob-shape-reason
  [shape]
  (case shape
    :absolute "it is an absolute path"
    :home "it begins with `~`, which names a path outside the workspace"
    :parent "it contains a `..` path segment"
    "it names a path outside the workspace"))

;; @spec MCP-OP-THREAD-043
(defn escaping-glob-shape
  "The shape that puts a glob OUTSIDE the workspace, or nil when it stays in.

  Decided on the glob AS SPELLED, before any filesystem call: a glob is a
  pattern, not a path, and resolving it to check it would be the very read the
  refusal exists to prevent. Three shapes escape — an absolute path, a `~`
  home reference, and any `..` segment. Everything else is repo-relative and
  the bounded walk confines it.

  Round-five review, finding 2 (BLOCKING): admission checked only that a glob
  was a STRING, so `../outside/*.clj` and `/etc/passwd` were admitted and
  rendered into the receipt's own `rg` lines as the file set the verb claimed
  to have searched."
  [glob]
  (let [g (str/replace (str glob) "\\" "/")]
    (cond
      (or (str/starts-with? g "/")
          (re-find #"^[A-Za-z]:[/\\]" g)) :absolute
      (or (= g "~") (str/starts-with? g "~/")) :home
      (or (= g "..")
          (str/starts-with? g "../")
          (str/includes? g "/../")
          (str/ends-with? g "/..")) :parent
      :else nil)))

(defn- escaping-globs-in
  "Every `[field glob shape]` in a convention set whose glob leaves the root."
  [conventions]
  (concat
    (for [[i leg] (map-indexed vector (:legs conventions))
          glob (when (map? leg) (:globs leg))
          :let [shape (escaping-glob-shape glob)]
          :when shape]
      [(str "legs[" i "].globs") glob shape])
    (for [glob (get-in conventions [:governance :globs])
          :let [shape (escaping-glob-shape glob)]
          :when shape]
      ["governance.globs" glob shape])))

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
;; @spec MCP-OP-THREAD-014
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

    ;; @spec MCP-OP-THREAD-043
    ;; Before the walk, before any read: a convention set may not name a path
    ;; outside the workspace it is a convention set FOR.
    (seq (escaping-globs-in conventions))
    (let [[field glob shape] (first (escaping-globs-in conventions))]
      {:ok false
       :error_type "feature-thread-conventions-escaping-glob"
       :error (str "the glob '" glob "' in " field " of " source-label
                   " names a path outside the workspace: "
                   (escaping-glob-shape-reason shape))
       :field field
       :glob glob
       :escaping_globs (mapv (fn [[f g sh]]
                               {:field f :glob g
                                :reason (escaping-glob-shape-reason sh)})
                             (escaping-globs-in conventions))
       :conventions_source source-label
       :remedy (str "Every glob is repo-relative to the workspace root. Remove"
                    " the leading `/`, the leading `~` or the `..` segment, or"
                    " run feature_thread with that directory as the workspace"
                    " root.")})

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

;; @spec MCP-OP-THREAD-030
(defn- render-search
  "The search line the receipt prints, reproducing the candidate set the verb
  ACTUALLY searched.

  A printed `rg` line is executable, so a caller can run it — and if it names
  a wider file set than the verb walked, the caller gets an answer the receipt
  does not have. That is the strongest possible tell that a receipt does not
  know its own scope (round-three review, B3). `scope.paths` narrowing is
  therefore rendered as extra `-g` filters, never silently dropped."
  ([globs pair] (render-search globs pair nil))
  ([globs [label regex] scope-paths]
   (str label ": rg -n -e '" regex "'"
        (apply str (map #(str " -g '" % "'") globs))
        (apply str (map #(str " -g '" (str/replace % #"/+$" "") "/**'")
                        (remove str/blank? (or scope-paths [])))))))

;; @spec MCP-OP-THREAD-024
(defn comment-mention?
  "True when the match at `start` on `line` sits inside a line COMMENT.

  `;` for Clojure, `//` for scripts, with in-line string state tracked so a
  semicolon inside `\"a;b\"` is not a comment. A comment runs to end of line, so
  the FIRST occurrence deciding is the same as every occurrence deciding.

  Every search this verb runs is `re-find` over a raw line with no lexical
  context, which is how a subject mentioned only in a `;; TODO` comment
  promoted a leg to FOUND and a thread to COMPLETE (round-one review, B2)."
  [^String line start clojure?]
  (loop [i 0 in-string? false]
    (if (>= i (min start (count line)))
      false
      (let [c (.charAt line i)]
        (cond
          in-string? (cond
                       (= c \\) (recur (+ i 2) true)
                       (= c \") (recur (inc i) false)
                       :else (recur (inc i) true))
          (= c \") (recur (inc i) true)
          (and clojure? (= c \;)) true
          (and (not clojure?) (= c \/) (< (inc i) (count line))
               (= \/ (.charAt line (inc i)))) true
          (= c \\) (recur (+ i 2) false)
          :else (recur (inc i) false))))))


;; @spec MCP-OP-THREAD-032
(defn- clj-token-at
  "The token beginning at or after `i` in `text`, or nil. Used for exactly one
  question: is the head of this list the symbol `comment`."
  [^String text i]
  (let [n (count text)
        s (loop [j i]
            (if (and (< j n) (Character/isWhitespace (.charAt text j))) (recur (inc j)) j))
        e (loop [j s]
            (if (and (< j n)
                     (not (Character/isWhitespace (.charAt text j)))
                     (not (contains? #{\( \) \[ \] \{ \} \" \;} (.charAt text j))))
              (recur (inc j))
              j))]
    (when (> e s) (subs text s e))))

(def ^:private clj-open-brackets #{\( \[ \{})
(def ^:private clj-close-brackets #{\) \] \}})

;; @spec MCP-OP-THREAD-032
(defn clj-commented-line-set
  "The 1-based lines of Clojure `text` the READER does not evaluate.

  Three constructs, not one: a `;` line comment, a `(comment …)` form, and a
  form discarded by `#_`. Only the first is visible one line at a time, which is
  why `comment-mention?` alone reported a `(comment (widgetize …))` decoy as
  live code and promoted the thread to COMPLETE (round-three review, B2')."
  [^String text]
  (let [n (count text)
        offsets (line-start-offsets text)
        lof (fn [i] (offset->line offsets i))
        mark (fn [acc from-i to-i]
               (reduce conj! acc (range (lof from-i) (inc (lof to-i)))))]
    (loop [i 0 mode :code stack [] pending 0 acc (transient #{})]
      (if (>= i n)
        (persistent! acc)
        (let [c (.charAt text i)
              next-c (when (< (inc i) n) (.charAt text (inc i)))]
          (case mode
            :string
            (cond
              (= c \\) (recur (+ i 2) :string stack pending acc)
              (= c \") (recur (inc i) :code stack pending acc)
              :else (recur (inc i) :string stack pending acc))

            :comment
            (if (= c \newline)
              (recur (inc i) :code stack pending acc)
              (recur (inc i) :comment stack pending (conj! acc (lof i))))

            :code
            (cond
              (= c \;) (recur (inc i) :comment stack pending (conj! acc (lof i)))

              (and (= c \#) (= next-c \_))
              (recur (+ i 2) :code stack (inc pending) acc)

              ;; a character literal: `\;` and `\"` open nothing
              (= c \\) (recur (+ i 2) :code stack pending acc)

              (and (pos? pending) (= c \"))
              (let [e (loop [j (inc i)]
                        (cond (>= j n) j
                              (= \\ (.charAt text j)) (recur (+ j 2))
                              (= \" (.charAt text j)) (inc j)
                              :else (recur (inc j))))]
                (recur e :code stack (dec pending) (mark acc i (min (dec e) (dec n)))))

              (= c \") (recur (inc i) :string stack pending acc)

              (contains? clj-open-brackets c)
              (let [discarded? (pos? pending)
                    comment-form? (and (= c \()
                                       (= "comment" (clj-token-at text (inc i))))]
                (recur (inc i) :code
                       (conj stack {:open i
                                    :mark? (or discarded? comment-form?)
                                    :outer (if discarded? (dec pending) pending)})
                       0 acc))

              (contains? clj-close-brackets c)
              (if (seq stack)
                (let [f (peek stack)]
                  (recur (inc i) :code (pop stack) (:outer f)
                         (if (:mark? f) (mark acc (:open f) i) acc)))
                (recur (inc i) :code stack pending acc))

              (and (pos? pending) (not (Character/isWhitespace c)))
              ;; a discarded ATOM -- `#_foo`, `#_:kw`, `#_42`
              (let [e (loop [j i]
                        (if (and (< j n)
                                 (not (Character/isWhitespace (.charAt text j)))
                                 (not (contains? clj-open-brackets (.charAt text j)))
                                 (not (contains? clj-close-brackets (.charAt text j))))
                          (recur (inc j))
                          j))]
                (recur e :code stack (dec pending) (mark acc i (max i (dec e)))))

              :else (recur (inc i) :code stack pending acc))))))))

;; @spec MCP-OP-THREAD-032
(defn script-commented-line-set
  "The 1-based lines of script `text` inside a `//` or `/* … */` comment.

  Same lexical states as `lexed-brace-match` -- strings, template literals and
  regex literals (character classes included) cannot open a comment -- because a
  `/*` inside a string is not a comment and a `//` inside a regex is not one
  either. A multi-line block comment is invisible to a per-line test, which is
  how a function that existed only inside `/* … */` was offered as the JS leg."
  [^String text]
  (let [n (count text)
        offsets (line-start-offsets text)
        lof (fn [i] (offset->line offsets i))]
    (loop [i 0 mode :code stack [] prev nil acc (transient #{})]
      (if (>= i n)
        (persistent! acc)
        (let [c (.charAt text i)
              next-c (when (< (inc i) n) (.charAt text (inc i)))]
          (case mode
            :line-comment
            (if (= c \newline)
              (recur (inc i) :code stack prev acc)
              (recur (inc i) :line-comment stack prev (conj! acc (lof i))))

            :block-comment
            (if (and (= c \*) (= next-c \/))
              (recur (+ i 2) :code stack prev (conj! acc (lof i)))
              (recur (inc i) :block-comment stack prev (conj! acc (lof i))))

            :single
            (cond
              (= c \\) (recur (+ i 2) :single stack prev acc)
              (= c \') (recur (inc i) :code stack \' acc)
              :else (recur (inc i) :single stack prev acc))

            :double
            (cond
              (= c \\) (recur (+ i 2) :double stack prev acc)
              (= c \") (recur (inc i) :code stack \" acc)
              :else (recur (inc i) :double stack prev acc))

            :template
            (cond
              (= c \\) (recur (+ i 2) :template stack prev acc)
              (= c \`) (recur (inc i) :code stack \` acc)
              (and (= c \$) (= next-c \{)) (recur (+ i 2) :code (conj stack :tpl) prev acc)
              :else (recur (inc i) :template stack prev acc))

            :regex
            (cond
              (= c \\) (recur (+ i 2) :regex stack prev acc)
              (= c \newline) (recur (inc i) :code stack prev acc)
              (= c \[) (recur (inc i) :regex-class stack prev acc)
              (= c \/) (recur (inc i) :code stack \/ acc)
              :else (recur (inc i) :regex stack prev acc))

            :regex-class
            (cond
              (= c \\) (recur (+ i 2) :regex-class stack prev acc)
              (= c \newline) (recur (inc i) :code stack prev acc)
              (= c \]) (recur (inc i) :regex stack prev acc)
              :else (recur (inc i) :regex-class stack prev acc))

            :code
            (cond
              (and (= c \/) (= next-c \/))
              (recur (+ i 2) :line-comment stack prev (conj! acc (lof i)))

              (and (= c \/) (= next-c \*))
              (recur (+ i 2) :block-comment stack prev (conj! acc (lof i)))

              (= c \') (recur (inc i) :single stack prev acc)
              (= c \") (recur (inc i) :double stack prev acc)
              (= c \`) (recur (inc i) :template stack prev acc)

              (and (= c \})  (seq stack))
              (recur (inc i) :template (pop stack) c acc)

              (and (= c \/)
                   (or (nil? prev)
                       (contains? regex-preceding-chars prev)
                       (contains? regex-context-keywords (word-before text i))))
              (recur (inc i) :regex stack prev acc)

              (Character/isWhitespace c) (recur (inc i) :code stack prev acc)
              :else (recur (inc i) :code stack c acc))))))))

;; @spec MCP-OP-THREAD-032
(defn commented-line-set
  "Every 1-based line of `text` that the language does not execute."
  [^String text clojure?]
  (if clojure? (clj-commented-line-set text) (script-commented-line-set text)))

;; @spec MCP-OP-THREAD-032
(defn commented-out?
  "True when 1-based `line` of `text` is inside a comment, a `(comment …)` form,
  a `#_` discard, or a `/* … */` block. Whole-file lexical state, not one line."
  [^String text line clojure?]
  (contains? (commented-line-set text clojure?) line))

(defn- match-start
  [pattern ^String line]
  (let [m (re-matcher pattern line)]
    (when (.find m) (.start m))))

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
        (let [{:keys [ok source lines] :as read} (read-source cache relative)]
          (if-not ok
            (update acc :unreadable conj {:file relative :reason (name (:reason read))})
            ;; @spec MCP-OP-THREAD-032
            ;; The whole-file comment set is computed ONCE per file and only when
            ;; the file actually has a hit: `(comment ...)`, `#_` and `/* ... */`
            ;; are not decidable from the hit line alone.
            (let [clojure? (clojure-path? relative)
                  commented (delay (commented-line-set source clojure?))]
              (update acc :hits into
                      (keep-indexed
                        (fn [idx line]
                          (when-let [start (match-start pattern line)]
                            {:file relative
                             :line (inc idx)
                             :col start
                             :text (str/trim line)
                             :in_comment (or (comment-mention? line start clojure?)
                                             (contains? @commented (inc idx)))}))
                        lines))))))
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
  [cache {:keys [file line col in_comment]} evidence & [opts]]
  (let [{:keys [ok source lines]} (read-source cache file)]
    (when ok
      (let [{:keys [from to body boundary form-name comment-start]}
            (body-at file source lines line opts)]
        (cond-> {:in-comment? (boolean in_comment)
         :file file
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
         :refetch (refetch-command file from to)}
          ;; @spec MCP-OP-THREAD-044
          (:route-entry-check? opts)
          (assoc :route-entry? (route-entry-hit? file source line col)
                 :enclosing-form-name form-name))))))

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
        ;; @spec MCP-OP-THREAD-015
        ;; The Clojure alternative belongs here too: without it a genuine
        ;; `(defn foo …)` hit falls through to the :else branch and is stamped
        ;; `identifier(def)` by a branch that never recognised a definition.
        real-re (re-pattern (str "\\(defn?-? +(?:" idents ")\\b"
                                 "|(?:async +)?function +(?:" idents ")\\b"
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
          ;; @spec MCP-OP-THREAD-007
          ;; @spec MCP-OP-THREAD-024
          ;; The alias LINE, carried as the CANDIDATE it is. Round-five review,
          ;; finding 9: THREAD-024 already listed `alias-only` among the
          ;; fallback evidences that make a leg CANDIDATE while the code
          ;; reported ABSENT, and CANDIDATE is the truthful one — the verb has a
          ;; located range and simply does not vouch for it as the definition.
          ;; It still does not count toward COMPLETE and still names no anchor.
          {:hits [alias-hit]
           :evidence "alias-only"
           :extra-search ["alias-target"
                          (str target-re " [after following the alias at "
                               (:file alias-hit) ":" (:line alias-hit)
                               " -> " target "]")]}))

      ;; @spec MCP-OP-THREAD-024
      ;; Neither definition-shaped nor an alias: these hits came from the bare
      ;; identifier FALLBACK search, and a mention in a string or a comment is
      ;; the commonest shape they take. Labelling them `identifier(def)` told a
      ;; caller a string literal was the function definition.
      :else {:hits hits :evidence "identifier"})))

;; @spec MCP-OP-THREAD-042
(defn- quoted-var-in
  "Pull `#'alias/handler-name` -- or a bare `#'handler-name` -- out of a
  route-table line.

  The namespace is optional because a route table that lives in the same
  namespace as its handlers writes the var unqualified, and that is not a
  different kind of route entry. Requiring the slash cost `saveDraft` its
  handler leg on social-media-writer @2df99c98: a route entry naming
  `#'handle-save` produced no handler name at all, so the leg fell back to
  hunting the seed's own identifier in handler files -- which a handler named
  after the ROUTE does not contain."
  [text]
  (when-let [m (re-find #"#'(?:([A-Za-z0-9_.<>*+!?-]+)/)?([A-Za-z0-9_.<>*+!?-]+)"
                        text)]
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

;; @spec MCP-OP-THREAD-036
(defn- after-context-for
  "The `after-context-lines` source lines following the member's last line,
  verbatim, with the range they came from.

  Verbatim is the whole point: the caller writes an insertion here, so trailing
  whitespace and indentation are the payload, not noise. Returns nil at end of
  file rather than a padded lie."
  [cache member]
  (let [{:keys [ok lines]} (read-source cache (:file member))
        from (inc (:to member))]
    (when ok
      (let [available (drop (dec from) lines)
            taken (vec (take after-context-lines available))]
        (when (seq taken)
          {:after_context taken
           :after_context_from from
           :after_context_to (+ from (dec (count taken)))})))))

(defn- secondary-row
  [member]
  (select-keys member [:file :from :to :evidence :bytes :refetch]))

(defn- distinct-by-range
  "One member per (file, from, to). Two hits inside one test case or one form
  are one witness, and printing it twice spends budget to say nothing."
  [members]
  (->> members (reduce (fn [[seen acc] m]
                         (let [k [(:file m) (:from m) (:to m)]]
                           (if (contains? seen k)
                             [seen acc]
                             [(conj seen k) (conj acc m)])))
                       [#{} []])
       second))

;; @spec MCP-OP-THREAD-018
(defn co-primary-members
  "The best witness of every OTHER language present in `ranked`.

  A tests leg whose ranked hits span two languages has TWO primaries. Round one
  rendered the second as a secondary `also` row with no boundary, no hash and no
  anchor -- and the JavaScript test file was one of the six sites the real edit
  touched, so the caller still had to go read it."
  [ranked]
  (let [primary (first ranked)
        lang (language-of (:file primary))]
    (->> (rest ranked)
         (remove #(= lang (language-of (:file %))))
         (group-by #(language-of (:file %)))
         (sort-by key)
         (mapv (fn [[l members]] (assoc (first members) :language l))))))

(def weak-evidence-kinds
  "Evidence kinds that are a FALLBACK, not the leg's own shape."
  #{"identifier" "route-assembled" "route-tail" "alias-only"})

(defn- strong-boundary?
  [boundary]
  (boolean (and boundary
                (or (str/starts-with? boundary "form(parsed")
                    (str/starts-with? boundary "brace-window(lexed,closed")))))

;; @spec MCP-OP-THREAD-024
(defn leg-strength
  "FOUND, or CANDIDATE with the reason it is only a candidate.

  A leg is FOUND only when the receipt knows WHERE it ends -- a parsed Clojure
  form or a closed lexed brace window -- and found it by the search that
  expresses the leg's own shape. A labelled line window, an assembled or
  tail-matched route, a bare identifier, or a comment mention is a lead, and a
  lead must never make a thread read COMPLETE. Round-one review, B2: a decoy
  comment in `routes.clj` reported the dev-reload endpoint as the route leg of
  Dequote/Format and the thread as COMPLETE (5 of 5)."
  [member]
  (cond
    (:in-comment? member)
    {:status "CANDIDATE"
     :weak_reason "the hit is a comment mention, not code"}

    ;; @spec MCP-OP-THREAD-044
    (false? (:route-entry? member))
    {:status "CANDIDATE"
     :weak_reason (str "the route literal is a string inside "
                       (if (:enclosing-form-name member)
                         (str "`" (:enclosing-form-name member) "`")
                         "another form")
                       ", not a route-table entry")}

    (not (strong-boundary? (:boundary member)))
    {:status "CANDIDATE"
     :weak_reason (str "the boundary is not a parsed form or a closed brace"
                       " window: " (:boundary member))}

    (contains? weak-evidence-kinds (:evidence member))
    {:status "CANDIDATE"
     :weak_reason (str "evidence " (:evidence member) " is a fallback search,"
                       " not this leg's own shape")}

    :else {:status "FOUND"}))

(defn- found-leg
  ([base cache ranked searches unreadable]
   (found-leg base cache ranked searches unreadable nil))
  ([base cache ranked searches unreadable co-primaries]
   (let [primary (first ranked)
         ;; @spec MCP-OP-THREAD-034
         ;; An `anchor` names where to WRITE. A CANDIDATE is a LEAD: the receipt
         ;; has already said it does not vouch for this range, and offering an
         ;; insertion point next to that refusal is the same false green in a
         ;; quieter field. A lead may name where to read; only a FOUND leg may
         ;; name where to write (round-three review, B2').
         anchor-if-found (fn [m strength]
                           (when (= "FOUND" (:status strength))
                             (anchor-for cache m)))
         co (mapv (fn [m] (let [strength (leg-strength m)]
                            (cond-> (-> m
                                        (merge strength)
                                        (dissoc :rank :in-comment? :route-entry? :enclosing-form-name))
                              (= "FOUND" (:status strength))
                              (-> (assoc :anchor (anchor-for cache m))
                                  (merge (after-context-for cache m))))))
                  co-primaries)
         co-keys (set (map (juxt :file :from :to) co))
         strength (leg-strength primary)]
     (merge (dissoc base :globs)
            (dissoc primary :rank :in-comment? :route-entry? :enclosing-form-name)
            strength
            (cond-> {:searches [(last searches)]
                     :unreadable unreadable
                     :co_primaries co
                     :also (->> (rest ranked)
                                (remove #(contains? co-keys ((juxt :file :from :to) %)))
                                (take 4)
                                (mapv secondary-row))}
              (= "FOUND" (:status strength))
              (-> (assoc :anchor (anchor-if-found primary strength))
                  ;; @spec MCP-OP-THREAD-036
                  ;; The anchor's own lines ride WITH the anchor: an insertion
                  ;; point with nothing after it makes the caller re-read.
                  (merge (after-context-for cache primary))))))))

;; @spec MCP-OP-THREAD-041
(defn- seed-noun
  "What a leg of this kind needs the caller to have named."
  [kind]
  (case kind
    :route "route"
    :test "identifier or route"
    "identifier"))

;; @spec MCP-OP-THREAD-004
;; @spec MCP-OP-THREAD-008
(defn resolve-leg
  "One leg: FOUND with an exact range, a boundary label, a sha256 of the body
  bytes and the body; or ABSENT with every search that was run."
  [cache paths seeds leg {:keys [handler-name exclude-ranges scope-paths]}]
  (let [{:keys [id kind globs]} leg
        exclude-ranges (set exclude-ranges)
        ;; What the exclusion DROPPED, kept so an empty result can say whether
        ;; nothing was found or everything found was already in the receipt.
        dropped (volatile! [])
        keep-new (fn [members]
                   (let [members (distinct-by-range members)
                         [out cut] ((juxt remove filter)
                                    #(contains? exclude-ranges
                                                [(:file %) (:from %) (:to %)])
                                    members)]
                     (vswap! dropped into
                             (map #(select-keys % [:file :from :to :form_name])
                                  cut))
                     (vec out)))
        member-opts {:narrow? (contains? #{:use :route} kind)
                     :test-call? (= kind :test)
                     :route-entry-check? (= kind :route)}
        joined (when (and (= kind :handler) handler-name)
                 [["handler-join"
                   (str "\\(defn-? +" (quote-literal handler-name) "\\b")]])
        searches (concat joined (searches-for-kind kind seeds))
        base {:id id :leg_kind (name kind) :globs globs}
        result
        (if (empty? searches)
          ;; @spec MCP-OP-THREAD-041
          ;; "Could not search" and "searched and found nothing" are different
          ;; facts and were the same string. The leg stays COUNTED -- the verb
          ;; cannot tell an unnamed route from an absent one and the safe
          ;; direction is INCOMPLETE -- but it names the missing INPUT and how
          ;; to supply it, instead of the jargon a real-repo recall run over
          ;; six subjects printed six times.
          (merge base {:status "ABSENT"
                       :absent_cause "no-seed-of-this-leg-kind"
                       :searches []
                       :reason (str "this leg searches for a " (seed-noun kind)
                                    " and the request named none")
                       :remedy (str "Pass the " (seed-noun kind)
                                    " as `subject` or in `also`. It is still"
                                    " counted as missing: the verb cannot tell"
                                    " an unnamed one from an absent one.")
                       :unreadable []})
          (loop [[[label regex :as s] & more] searches
             ran []
             unreadable []]
        (if (nil? s)
          ;; @spec MCP-OP-THREAD-041
          (merge base {:status "ABSENT"
                       :absent_cause "searched-and-absent"
                       :searches ran :unreadable unreadable})
          (let [result (scan cache paths globs regex)
                hits (:hits result)
                ran' (conj ran (render-search globs [label regex] scope-paths))
                ;; @spec MCP-OP-THREAD-013
                ;; One entry per file per LEG: the same unreadable file is a
                ;; candidate of every search this leg runs (round-three, 3.4).
                unreadable' (vec (distinct (into unreadable (:unreadable result))))]
            (cond
              (and (= kind :def) (seq hits))
              (let [{hop-hits :hits hop-evidence :evidence extra :extra-search}
                    (alias-hop cache paths globs (:identifiers seeds) hits)
                    ;; @spec MCP-OP-THREAD-007
                    ;; The search that FOLLOWED the alias is the evidence for
                    ;; the CANDIDATE, so it rides with it: `found-leg` keeps
                    ;; only the last search, and without this the receipt says
                    ;; `alias-only` while quoting the search that found the
                    ;; alias rather than the one that failed to find its target.
                    ran' (cond-> ran'
                           extra (conj (render-search globs extra scope-paths)))
                    ranked (keep-new (keep #(hit->member cache % hop-evidence member-opts)
                                           hop-hits))]
                (if (seq ranked)
                  (found-leg base cache ranked ran' unreadable')
                  (merge base {:status "ABSENT"
                               :evidence hop-evidence
                               :searches ran'
                               :unreadable unreadable'})))

              (seq hits)
              (let [members (keep #(hit->member cache % label member-opts) hits)
                    ranked (cond
                             (= kind :test)
                             (->> members
                                  (map #(merge % (rank-test-hit % handler-name)))
                                  (sort-by (juxt (comp - :rank) :file :from))
                                  vec)

                             ;; @spec MCP-OP-THREAD-044
                             ;; A parsed route-table ENTRY outranks a string
                             ;; occurrence of the same literal, wherever the
                             ;; two sit in the file. `sort-by` is stable, so
                             ;; within each class the scan order is kept and a
                             ;; non-entry hit is still carried as a lead.
                             (= kind :route)
                             (vec (sort-by #(if (:route-entry? %) 0 1) members))

                             :else (vec members))
                    ranked (keep-new ranked)]
                (if (empty? ranked)
                  (recur more ran' unreadable')
                  (found-leg base cache ranked ran' unreadable'
                             (when (= kind :test) (co-primary-members ranked)))))

              :else (recur more ran' unreadable'))))))]
    (cond-> result
      (and (= "ABSENT" (:status result)) (seq @dropped))
      (assoc :excluded (vec @dropped)))))

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
  (when (and route-leg (located? route-leg))
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

;; @spec MCP-OP-THREAD-017
(defn governance-entry
  "The top-level EDN entry containing `hit-line`, as `{:from :to}`, or nil.

  The registry is one vector of a thousand lines and the ENTRY is the map
  element the hit sits in. The two are told apart structurally -- the OUTERMOST
  bracketed span containing the line that is still under the entry ceiling --
  and never by a semantic parse of what a registry row means. A file whose
  brackets do not resolve to an entry gets no anchor and SAYS so."
  [cache relative hit-line]
  (let [{:keys [ok source]} (read-source cache relative)]
    (when (and ok (clojure-path? relative))
      (let [offsets (line-start-offsets source)
            sized (keep (fn [[open close]]
                          (let [from (offset->line offsets open)
                                to (offset->line offsets close)]
                            ;; An ENTRY is a MAP element. A parenthesis inside a
                            ;; rationale string is a bracket pair too, and
                            ;; anchoring after it would name a line that is not
                            ;; an entry at all -- which is how a redacted or
                            ;; unbalanced registry earns `unparsed` instead of a
                            ;; confident wrong answer.
                            (when (and (= \{ (.charAt ^String source open))
                                       ;; and it OPENS its line: an element of a
                                       ;; multi-line vector does, a brace inside
                                       ;; a rationale string does not. A
                                       ;; redacted or unbalanced file desyncs the
                                       ;; string lexer, and this is what keeps
                                       ;; that failure at `unparsed` instead of
                                       ;; a confident wrong anchor.
                                       (str/blank? (subs source
                                                        (nth offsets (dec from))
                                                        open))
                                       (<= from hit-line to)
                                       (<= (inc (- to from))
                                           governance-entry-ceiling-lines))
                              {:from from :to to})))
                        (clj-bracket-spans source))]
        (first (sort-by (juxt :from (comp - :to)) sized))))))

;; @spec MCP-OP-THREAD-010
;; @spec MCP-OP-THREAD-017
(defn governance-rows
  "The governance tail: the intent registry rows, the intent-contract test and
  the test target -- not code, and the transcript shows the agent needed all of
  it after it had the five owners. Ranges only; a registry row is never inlined
  whole.

  Each row carries the END of the entry it sits in and an insertion anchor after
  it, because the real edit's registry change was a NEW entry appended after the
  matched one, and a row that names only the hit LINE tells the caller where to
  read, never where to write."
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
                      (let [entry (governance-entry cache (:file h) (:line h))
                            row {:file (:file h)
                                 :line (:line h)
                                 :match (subs (:text h)
                                              0 (min 64 (count (:text h))))}]
                        (if entry
                          (assoc row
                                 :form_start (:from entry)
                                 :form_end (:to entry)
                                 :anchor (str "after:L" (:to entry))
                                 :refetch (refetch-command (:file h) (:from entry)
                                                           (:to entry)))
                          (assoc row
                                 :anchor "unparsed"
                                 :refetch (refetch-command
                                            (:file h)
                                            (max 1 (- (:line h) 4))
                                            (+ (:line h) 20)))))))
               (take 6)
               vec))))))

;; @spec MCP-OP-THREAD-017
(defn governance-template
  "The entry a NEW governance row is modelled on: the matched row with the
  HIGHEST line, as a range and a refetch, never inlined whole.

  The real edit appended its new registry entry immediately after the last
  matched one. So the last matched entry is both the insertion point and the
  shape to copy, and naming it costs a range where inlining it would cost a
  kilobyte."
  [rows]
  (when-let [row (->> rows (filter :form_end) (sort-by :line) last)]
    {:file (:file row)
     :from (:form_start row)
     :to (:form_end row)
     :anchor (:anchor row)
     :refetch (refetch-command (:file row) (:form_start row) (:form_end row))}))

;; ---------------------------------------------------------------------------
;; The verify row: how the caller RUNS the tests leg it is about to add to
;; ---------------------------------------------------------------------------

(def ^:private make-target-pattern
  #"^([A-Za-z0-9_][A-Za-z0-9_.\-/]*)\s*:(?!=)")

(def ^:private clojure-test-alias-pattern
  #"clojure\s+-[MX]\S*:\S*test")

;; @spec MCP-OP-THREAD-019
(defn- recipe-line
  "One Makefile recipe line as a RUNNABLE command plus the prefix that was
  stripped.

  `@`, `-` and `+` at the head of a recipe are Make directives -- silence, ignore
  errors, always-run -- and not part of the shell command. Printing them inside
  `command` handed the caller `@node --test ...`, which no shell will run
  (round-three review, 3.3). Nothing is erased: what was stripped is named."
  [line-no ^String line]
  (let [trimmed (str/trim line)
        prefix (re-find #"^[@+-]+" trimmed)
        command (str/trim (subs trimmed (count (or prefix ""))))]
    (cond-> {:line line-no :command command}
      prefix (assoc :make_prefix prefix))))

;; @spec MCP-OP-THREAD-019
(defn makefile-targets
  "Every `target:` declaration in a Makefile with its recipe lines.

  Lexical and tiny: a target is a line whose first token ends in `:`, and its
  recipe is the TAB-indented lines that follow. No Make semantics are claimed --
  the receipt quotes the recipe line VERBATIM and names the line it came from,
  so a caller can check it in one glance rather than trust an interpretation."
  [lines]
  (let [{:keys [acc current]}
        (reduce
          (fn [{:keys [acc current]} [idx line]]
            (cond
              (and current (str/starts-with? line "\t"))
              {:acc acc
               :current (update current :recipe conj
                                (recipe-line (inc idx) line))}

              (re-find make-target-pattern line)
              {:acc (cond-> acc current (conj current))
               :current {:target (second (re-find make-target-pattern line))
                         :line (inc idx)
                         :recipe []}}

              (str/blank? line) {:acc acc :current current}
              :else {:acc (cond-> acc current (conj current)) :current nil}))
          {:acc [] :current nil}
          (map-indexed vector lines))]
    (vec (cond-> acc current (conj current)))))

(defn- verify-rows-matching
  [targets pred evidence file]
  (vec (for [t targets
             r (:recipe t)
             :when (pred (:command r))]
         ;; @spec MCP-OP-THREAD-019
         ;; `make_prefix` rides along: the row is runnable as printed, and the
         ;; Make directive that was stripped is still named.
         (cond-> {:target (:target t) :line (:line t) :command (:command r)
                  :for file :evidence evidence}
           (:make_prefix r) (assoc :make_prefix (:make_prefix r))))))

;; @spec MCP-OP-THREAD-019
(defn verify-rows-for
  "The Makefile target(s) that RUN one tests file, strongest evidence first.

  A recipe naming the FILE is the answer; a recipe naming its DIRECTORY is the
  next best; a recipe running the Clojure test alias is labelled `alias`,
  because that is what it is -- the target that runs this file among many, not
  proof this file is in it. The transcript spent three calls finding this line."
  [targets file]
  (let [slash (.lastIndexOf ^String file "/")
        dir (when (pos? slash) (subs file 0 slash))
        by-file (verify-rows-matching targets #(str/includes? % file)
                                      "names-the-file" file)
        by-dir (when dir
                 (verify-rows-matching targets #(str/includes? % dir)
                                       "names-the-directory" file))
        by-alias (verify-rows-matching targets
                                       #(boolean (re-find clojure-test-alias-pattern %))
                                       "alias" file)]
    (vec (take max-verify-rows (or (seq by-file) (seq by-dir) (seq by-alias) [])))))

;; ---------------------------------------------------------------------------
;; Which command picks up a NEW test namespace
;; ---------------------------------------------------------------------------

;; @spec MCP-OP-THREAD-048
(defn namespace-of-test-file
  "The Clojure namespace a test file declares, by PATH, under a source root.

  `test/writer/handlers/transform_apply_test.clj` under root `test` is
  `writer.handlers.transform-apply-test`. A script test has no namespace and
  this returns nil rather than inventing one."
  [file roots]
  (when (clojure-path? file)
    (let [root (first (filter #(str/starts-with? file (str % "/")) roots))]
      (when root
        (-> (subs file (inc (count root)))
            (str/replace #"\.clj[cs]?$" "")
            (str/replace "_" "-")
            (str/replace "/" "."))))))

;; @spec MCP-OP-THREAD-048
(defn kaocha-suites
  "The suites declared in a `tests.edn`, as
  `[{:id :test-paths :ns-patterns :line}]`.

  The `#kaocha/v1` reader tag is read as DATA — this verb runs no test runner
  and claims no Kaocha semantics beyond the two fields that decide whether a
  namespace is picked up: where the suite looks, and which namespace names it
  admits."
  [source lines]
  (let [parsed (try
                 (edn/read-string {:readers {'kaocha/v1 identity}
                                   :default (fn [_ v] v)}
                                  source)
                 (catch Exception _ nil))
        line-of (fn [id]
                  (first (keep-indexed
                           (fn [i l]
                             (when (re-find (re-pattern
                                              (str ":id\\s+:" (name id) "\\b"))
                                            l)
                               (inc i)))
                           lines)))]
    (when (map? parsed)
      (vec (for [t (:tests parsed) :when (map? t)]
             {:id (:id t)
              :test-paths (vec (:test-paths t))
              :ns-patterns (vec (:ns-patterns t))
              :line (line-of (:id t))})))))

;; @spec MCP-OP-THREAD-048
(defn- deps-alias-paths
  "`{alias-keyword #{extra-path …}}` from a `deps.edn`."
  [source]
  (let [parsed (try (edn/read-string {:default (fn [_ v] v)} source)
                    (catch Exception _ nil))]
    (when (map? parsed)
      (into {} (for [[k v] (:aliases parsed) :when (map? v)]
                 [k (set (concat (:extra-paths v) (:paths v)))])))))

(defn- command-aliases
  "Every `:alias` named in a `clojure -M:a:b` invocation."
  [command]
  (->> (re-seq #"-[MXT]\S*" (or command ""))
       (mapcat #(str/split % #":"))
       (remove str/blank?)
       (remove #(re-find #"^-" %))
       (map keyword)
       set))

;; @spec MCP-OP-THREAD-048
(defn runs-namespace-for
  "Whether one verify row's command picks up `ns`, and the evidence for it.

  Both T3 arms spent a read on this question and the receipt could not answer
  it: which target/alias actually RUNS the namespace the new test file will
  declare. On social-media-writer the answer is neither obvious nor benign —
  `make test-unit` runs the kaocha `unit` suite, whose `:ns-patterns` is an
  explicit ALLOWLIST, so a brand-new namespace is silently not run by it.

  Returns nil when the repository declares no runner configuration this verb
  can read; it never guesses."
  [{:keys [suites alias-paths tests-edn-present?]} command ns file]
  (when ns
    (let [tokens (set (re-seq #"[A-Za-z][A-Za-z0-9_-]*" (or command "")))
          named (filter #(contains? tokens (name (:id %))) suites)
          selected (if (and (seq suites) (empty? named)) suites named)
          matches? (fn [suite]
                     (and (some #(str/starts-with? file (str % "/"))
                                (:test-paths suite))
                          (or (empty? (:ns-patterns suite))
                              (some #(re-find (re-pattern %) ns)
                                    (:ns-patterns suite)))))
          hit (first (filter matches? selected))
          alias-hit (first (filter (fn [[a paths]]
                                     (and (contains? (command-aliases command) a)
                                          (some #(str/starts-with? file
                                                                   (str % "/"))
                                                paths)))
                                   alias-paths))]
      (cond
        (and tests-edn-present? (seq selected) hit)
        {:namespace ns
         :picks_up true
         :suite (name (:id hit))
         :from (str "tests.edn" (when (:line hit) (str ":L" (:line hit))))
         :why (if (empty? (:ns-patterns hit))
                (str "the `" (name (:id hit)) "` suite takes every namespace under "
                     (str/join ", " (:test-paths hit)))
                (str "the `" (name (:id hit))
                     "` suite's ns-patterns match this namespace"))}

        (and tests-edn-present? (seq selected))
        {:namespace ns
         :picks_up false
         :suites_tried (vec (sort (map (comp name :id) selected)))
         :from "tests.edn"
         :why (str "every suite this command selects declares an ns-patterns"
                   " ALLOWLIST that does not name " ns
                   " — a new namespace is not run by it until the pattern"
                   " admits it")}

        alias-hit
        {:namespace ns
         :picks_up true
         :from "deps.edn"
         :why (str "the " (key alias-hit) " alias puts this file's directory on"
                   " the test classpath and the command names no suite")}

        :else
        (when (or tests-edn-present? (seq alias-paths))
          {:namespace ns
           :picks_up false
           :from (if tests-edn-present? "tests.edn" "deps.edn")
           :why "no suite or alias this command names reaches this file"})))))

(defn- runner-config
  [cache]
  (let [t (read-source cache "tests.edn")
        d (read-source cache "deps.edn")]
    {:tests-edn-present? (boolean (:ok t))
     :suites (when (:ok t) (kaocha-suites (:source t) (:lines t)))
     :alias-paths (when (:ok d) (deps-alias-paths (:source d)))}))

;; @spec MCP-OP-THREAD-019
(defn verify-row
  "`:verify` for every tests primary in the thread, and the reason when there is
  no Makefile to read."
  [cache legs]
  (let [{:keys [ok lines]} (read-source cache "Makefile")]
    (if-not ok
      {:verify [] :verify_reason "no Makefile at the workspace root"}
      (let [targets (makefile-targets lines)
            files (->> legs
                       (filter #(and (= "test" (:leg_kind %))
                                     (located? %)))
                       (mapcat (fn [l] (into [(:file l)]
                                             (map :file (:co_primaries l)))))
                       distinct
                       vec)
            runner (runner-config cache)
            test-roots (distinct (concat (mapcat :test-paths (:suites runner))
                                         ["test"]))
            ;; @spec MCP-OP-THREAD-048
            rows (vec (for [row (mapcat #(verify-rows-for targets %) files)
                            :let [nsname (namespace-of-test-file (:for row)
                                                                 test-roots)
                                  runs (runs-namespace-for runner (:command row)
                                                           nsname (:for row))]]
                        (cond-> row runs (assoc :runs_namespace runs))))]
        (if (seq rows)
          {:verify rows}
          {:verify []
           :verify_reason (str "no Makefile target names "
                               (str/join ", " files)
                               " or runs a Clojure test alias")})))))

(def ^:private body-source-pattern
  "Marks an expression as the PARSED REQUEST, not just any map."
  #"parse-json-body|:body-params|:json-params|:form-params|:params\b|:body\b|json/read|read-json")

;; @spec MCP-OP-THREAD-037
(defn handler-reads
  "The keys the handler takes off the parsed request body.

  Three shapes, all lexical, none of them a Clojure reader: `{:keys [a b]}`
  bound to an expression that mentions a body-parsing call, `(:k body)`, and
  `(get body \"k\")`. The `{:keys …}` case is GUARDED by that expression --
  without the guard `(let [{:keys [reason] :as conflict} (ex-data e)] …)` in
  the fixture's own 409 branch reads as a request field, which would make the
  contract row confidently wrong rather than merely thin."
  [^String body]
  (when body
    (let [destructured
          (->> (re-seq #"(?s)\{:keys\s+\[([^\]]*)\][^\}]*\}\s*(.{0,160})" body)
               (keep (fn [[_ names tail]]
                       (when (re-find body-source-pattern (or tail ""))
                         names)))
               (mapcat #(str/split (str/trim %) #"\s+")))
          keyworded (->> (re-seq #"\(:([A-Za-z0-9_*+!?<>=-]+)\s+(?:body|params|payload|request-body)\)"
                                 body)
                         (map second))
          getted (->> (re-seq #"\(get\s+(?:body|params|payload|request-body)\s+[\":]([A-Za-z0-9_*+!?<>=-]+)"
                              body)
                      (map second))]
      (->> (concat destructured keyworded getted)
           (remove str/blank?)
           (remove #{":as" ":or" ":keys"})
           distinct
           sort
           vec))))

;; @spec MCP-OP-THREAD-037
(defn js-posts
  "The keys of the object literal the script sends with the request.

  The lexed brace window already isolated the function body, so this is one
  regex over it: the second argument of `postJSON(url, {…})` / `fetch(url,
  {…})`, plus a `JSON.stringify({…})` payload. Shorthand (`{sync}`) and
  `key: value` are both keys; a spread is reported as `...` rather than
  guessed at."
  [^String body]
  (when body
    (let [literals (concat
                     (map second (re-seq #"(?:postJSON|postForm|fetch)\s*\([^,()]*,\s*\{([^{}]*)\}" body))
                     (map second (re-seq #"JSON\.stringify\s*\(\s*\{([^{}]*)\}" body)))]
      (->> literals
           (mapcat #(str/split % #","))
           (map str/trim)
           (remove str/blank?)
           (map (fn [item]
                  (if (str/starts-with? item "...")
                    "..."
                    (-> (first (str/split item #":"))
                        str/trim
                        (str/replace #"^['\"]|['\"]$" "")))))
           (remove str/blank?)
           distinct
           sort
           vec))))

;; @spec MCP-OP-THREAD-037
(defn request-contract
  "The one RELATION between the route, the handler and the script: does the set
  of keys the browser posts match the set the handler reads?

  A caller adding a field to this feature must change both sides, and the
  transcript shows that is exactly where a thread goes wrong. `agree?` is the
  verdict; `only_in_js` and `only_in_handler` are the actionable half, because
  `false` alone tells nobody which side to edit. Returns nil when either side
  is absent -- an absent leg is already reported as absent, and inventing a
  contract over one side would be the false green this verb exists to refuse."
  [seeds legs]
  (let [handler (first (filter #(and (= "handler" (:leg_kind %)) (located? %)) legs))
        js (first (filter #(and (script-path? (str (:file %))) (located? %)
                                (not= "test" (:leg_kind %)))
                          legs))
        reads (handler-reads (:body handler))
        posts (js-posts (:body js))]
    (when (and handler js reads posts)
      (let [r (set reads) p (set posts)]
        {:route (first (:routes seeds))
         :handler_reads reads
         :js_posts posts
         :agree? (= r p)
         :only_in_js (vec (sort (remove r posts)))
         :only_in_handler (vec (sort (remove p reads)))}))))

;; @spec MCP-OP-THREAD-010
(defn build-rules
  "The wiring contract: what the handler routes through, what it refuses, the
  INTENT ids present in the located bodies resolved to their registry rows, and
  the one axis on which the new feature differs from its sibling."
  [cache paths conventions seeds legs axis]
  (let [found (filter located? legs)
        handler (first (filter #(= "handler" (:leg_kind %)) legs))
        intents (vec (distinct (mapcat #(intents-in cache %) found)))
        governance (governance-rows cache paths conventions intents seeds)]
    (merge
      (when-let [rc (request-contract seeds legs)] {:request_contract rc})
      {:durable_path (if (and handler (located? handler))
                       (namespaced-calls (:body handler))
                       [])
       :refusal_statuses (if (and handler (located? handler))
                           (refusal-statuses (:body handler))
                           [])
       :intents intents
       :governance governance
       :governance_template (governance-template governance)
       :axis axis
       ;; @spec MCP-OP-THREAD-021
       ;; Two corrections in one line. A naive reader given only the receipt
       ;; obeyed the old wording literally and planned SIX refetch+sha256sum
       ;; calls before writing -- exactly the calls this verb exists to save.
       ;; And the old wording asserted a REFUSAL that no code here can issue:
       ;; this verb is read-only, and the only pre-image binding in the trunk
       ;; is admit_clojure_patch's expect_pre_sha256 over WHOLE FILES. So the
       ;; line is advisory about itself and points at the call that IS a gate.
       :assert (str "the per-leg sha256 is the human-checkable detail of what"
                    " this read-only verb read; it enforces nothing itself, so"
                    " do NOT re-read the ranges to check it. Pass the subset"
                    " of next_call.expect_pre_sha256 your patch touches"
                    " (whole-file digests; select it from next_call.by_leg,"
                    " because admit_clojure_patch requires EXACTLY the files"
                    " the patch touches) to admit_clojure_patch, which BINDS"
                    " the pre-image at write time and answers a mismatch with"
                    " a typed refusal, never a retry")}
      (verify-row cache legs))))

;; @spec MCP-OP-THREAD-038
(defn negative-evidence
  "The identifiers that are NOT in this tree, each with the search that says so.

  A receipt that names only what it found leaves the reader to prove the
  negative themselves -- `rg -i dequote` over the whole tree -- which is the
  call this verb exists to save. So the absences are first-class: every seed
  identifier and every `probe` identifier that has no definition-shaped hit and
  no occurrence OUTSIDE a comment is reported here, with the rendered search.

  An absence with no search behind it is an opinion, so the search is not
  optional. `probe` exists because the question a reader most wants answered is
  usually about a name that is NOT one of the seeds."
  [cache paths globs identifiers]
  (->> (distinct (remove str/blank? identifiers))
       (keep (fn [ident]
               (let [regex (quote-literal ident)
                     {:keys [hits]} (scan cache paths globs regex)
                     live (remove :in_comment hits)]
                 (when (empty? live)
                   {:identifier ident
                    :searched (render-search globs ["identifier" regex])
                    :reason (if (seq hits)
                              (str "every occurrence is inside a comment ("
                                   (count hits) " of them)")
                              "no occurrence anywhere in the candidate set")}))))
       vec))

;; @spec MCP-OP-THREAD-025
(defn next-call
  "The call that can actually BIND what this receipt asserts.

  `admit_clojure_patch` binds a pre-image as `expect_pre_sha256`, a map of FILE
  to the sha256 of the WHOLE FILE -- a different digest subject from this
  receipt's per-range hashes, which is why the old `a mismatch is a REFUSAL`
  line named a control that did not exist. This row emits digests admit can
  consume, so the receipt hands the caller the gate rather than an instruction."
  [cache legs]
  (let [located (filter located? legs)
        files-of (fn [l] (->> (into [(:file l)] (map :file (:co_primaries l)))
                              (remove nil?)
                              distinct
                              sort))
        files (->> located (mapcat files-of) distinct sort)
        digest (fn [f] (let [{:keys [ok source]} (read-source cache f)]
                         (when ok [f (sha256-hex source)])))
        digests (into {} (keep digest files))
        ;; @spec MCP-OP-THREAD-025
        ;; `admit_clojure_patch` refuses a pre-image map that is not EXACTLY the
        ;; set of files the patch touches. A single map naming all six legs is
        ;; therefore obeyable only by a patch that touches all six; the normal
        ;; edit -- handler plus its test -- is refused. So the row also emits
        ;; the digests split BY LEG, which is the unit a caller actually reasons
        ;; in, and says plainly that the argument is a subset (round-three, 3.2).
        by-leg (into {}
                     (keep (fn [l]
                             (let [m (into {} (keep digest (files-of l)))]
                               (when (seq m) [(keyword (:id l)) m])))
                           located))]
    (when (seq digests)
      ;; @spec MCP-OP-THREAD-049
      ;; ONE clock reading, spelled in both the leaf and the sentence: two
      ;; calls to `Instant/now` would put two different instants in the same
      ;; receipt, and the text-superset check would append the leaf as
      ;; `structured-only` because the sentence did not carry it.
      (let [clock (str (java.time.Instant/now))]
       {:tool "admit_clojure_patch"
       :expect_pre_sha256 digests
       :by_leg by-leg
       :computed_at clock
       :note (str "these whole-file digests were computed at " clock
                  "; admit_clojure_patch re-checks them at write time — do NOT"
                  " re-hash. "
                  "whole-file digests, the subject admit_clojure_patch binds;"
                  " the per-leg sha256 above is over the line range only."
                  " Pass the subset your patch touches -- admit_clojure_patch"
                  " requires expect_pre_sha256 to name EXACTLY the files the"
                  " patch touches, so select from by_leg rather than sending"
                  " this whole map")}))))

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

;; @spec MCP-OP-THREAD-016
(defn implementation-leg
  "The leg every convention set gets for FREE: the DEFINITION a seed names.

  The Dequote/Format fixture is the argument. The seeds named
  `mechanical-format`, the real edit inserted its new function immediately after
  `(defn mechanical-format …)`, and a five-leg receipt never carried that form —
  because no repository would think to declare a leg for `the definition my own
  seed names`. So the verb declares it: Clojure sources plus whatever script
  globs the convention set already has. A convention set that declares its own
  `implementation` leg keeps it and gets no second one."
  [conventions]
  (when-not (some #(= implementation-leg-id (:id %)) (:legs conventions))
    {:id implementation-leg-id
     :kind :def
     ;; @spec MCP-OP-THREAD-040
     ;; NOT the test leg's globs. `implementation` means the definition the
     ;; seed names, and a definition inside a test file is a DOUBLE. Because
     ;; the seed's real definition is usually already another leg, this leg
     ;; excludes that range and takes the NEXT definition-shaped hit -- so
     ;; inheriting `test/**/*.js` made `formatDraft` on social-media-writer
     ;; @2df99c98 report `implementation FOUND
     ;; test/js/editor_conflict_response_test.js` and read `4 of 6`.
     :globs (vec (distinct (concat implementation-clojure-globs
                                   (->> (:legs conventions)
                                        (remove #(= :test (:kind %)))
                                        (mapcat :globs)
                                        (filter script-path?)))))}))

(defn- seed-of-range
  "The seed whose definition the excluded range holds, or nil.

  The `N/A` reason is a specific, checkable statement about a named file and
  line range, so it must be ABOUT the seed it drops. Round three: it named
  `editor-commands.js:L389-L454` — `formatDraft`'s JS function — as the reason
  `mechanical-format` was uncounted."
  [cache identifiers dup]
  (let [{:keys [ok lines]} (when (:file dup) (read-source cache (:file dup)))
        text (when ok
               (str/join "\n" (subvec (vec lines)
                                      (max 0 (dec (or (:from dup) 1)))
                                      (min (count lines) (or (:to dup) 1)))))]
    (or (some (fn [i] (when (= i (:form_name dup)) i)) identifiers)
        (when text
          (some (fn [i]
                  (when (re-find (re-pattern
                                   (str "(?:defn?-? +|function +|[.]|\\b)"
                                        (quote-literal i) "\\b"))
                                 text)
                    i))
                identifiers)))))

;; @spec MCP-OP-THREAD-016
;; @spec MCP-OP-THREAD-029
(defn resolve-implementation
  "Resolve the automatic implementation leg against the already-resolved legs.

  Three outcomes, and the difference between the last two is the whole point.
  FOUND: a definition no declared leg already carries. `N/A`, uncounted: a
  search really ran over this leg's globs and every definition it found is
  already a leg of this receipt — and the reason NAMES THE SEED it dropped.
  `UNSCANNED`, COUNTED: this leg's globs were not part of the workspace walk,
  so nothing was searched and the verb has no right to say the leg is
  inapplicable. Round three's false green was exactly that conflation: the walk
  was bounded before this leg existed, so a definition sitting in
  `src/writer/other/dup.clj` was reported `N/A` and the thread `COMPLETE`."
  [cache paths seeds auto-leg declared walked-globs]
  (let [missing-globs (vec (remove (set walked-globs) (:globs auto-leg)))]
    (if (seq missing-globs)
      {:id (:id auto-leg)
       :leg_kind (name (:kind auto-leg))
       :status "UNSCANNED"
       :searches []
       :unreadable []
       :reason (str "this leg's globs were not part of the workspace walk, so"
                    " nothing was searched for a definition: "
                    (str/join " " missing-globs))
       :elide :implementation}
      (let [taken-status (into {} (for [l declared :when (located? l)]
                                    [[(:file l) (:from l) (:to l)] (:status l)]))
            taken (set (keys taken-status))
            resolved (resolve-leg cache paths seeds auto-leg
                                  {:exclude-ranges taken
                                   :scope-paths (:scope-paths auto-leg)})]
        (if (located? resolved)
          (assoc resolved :elide :implementation)
          (merge (select-keys resolved [:id :leg_kind :searches :unreadable])
                 {:status "N/A"
                  ;; @spec MCP-OP-THREAD-029
                  ;; Round-five review, finding 9: an excluded CANDIDATE range
                  ;; was read as proof that a definition exists, so the row said
                  ;; "the definition of ghostOnly is already a leg" about a
                  ;; STRING MENTION. A CANDIDATE is a lead; saying it is the
                  ;; definition is the same false green in a quieter field.
                  :reason (if-let [dup (first (:excluded resolved))]
                            (let [seed (or (seed-of-range cache
                                                          (:identifiers seeds) dup)
                                           "the seed")
                                  where (str "(" (:file dup) ":L" (:from dup)
                                             "-L" (:to dup) ")")]
                              (if (= "CANDIDATE"
                                     (get taken-status [(:file dup) (:from dup)
                                                        (:to dup)]))
                                (str "the only occurrence of " seed
                                     " is already a CANDIDATE leg of this"
                                     " receipt " where
                                     " — a lead, not a definition")
                                (str "the definition of " seed
                                     " is already a leg of this receipt "
                                     where)))
                            "no seed names a definition")
                  :elide :implementation}))))))

(def ^:private onclick-pattern
  "`{:onclick \"someCommand()\"}` in a menu form. The identifier, not the call."
  #":onclick\s+\"([A-Za-z_$][A-Za-z0-9_$]*)\s*\(")

;; @spec MCP-OP-THREAD-039
(defn co-menu-peers
  "The other commands bound in the SAME menu form as the subject, each resolved
  to its own definition.

  A caller adding a menu command reads its neighbours to learn the shape --
  what a command looks like, where the separators go, which of them have an
  implementation and which are still only bound. The transcript shows the real
  agent reading four such ranges by hand. The peer set is taken from the `:use`
  leg's OWN body, so `saveDraft` in the File menu is not a peer of an Edit menu
  command; the subject is not its own peer.

  A peer with no definition is reported ABSENT with the search that was run,
  never omitted: `expound` and `bulletize` are bound in the fixture's Edit menu
  and defined nowhere, and that is a fact the caller needs."
  [cache paths def-globs leg seed-identifiers]
  (when (and (located? leg) (:body leg) (seq def-globs))
    (let [seeded (set seed-identifiers)
          ids (->> (re-seq onclick-pattern (:body leg))
                   (map second)
                   distinct
                   (remove seeded)
                   vec)]
      (when (seq ids)
        (mapv
          (fn [id]
            (let [[label regex] (first (searches-for-kind
                                         :def {:identifiers [id] :routes []}))
                  searched (render-search def-globs [label regex])
                  live (remove :in_comment (:hits (scan cache paths def-globs regex)))
                  m (when (seq live)
                      (hit->member cache (first live) "co-menu-item"))]
              (if-not m
                {:identifier id :status "ABSENT" :searched searched}
                (let [strength (leg-strength m)]
                  (cond-> (merge {:identifier id}
                                 ;; @spec MCP-OP-THREAD-047
                                 ;; A LOCATED peer is a range row, and a range
                                 ;; row is only worth carrying if it is small:
                                 ;; the search that succeeded, the hit line, the
                                 ;; enclosing form name and the comment offset
                                 ;; are all recoverable from the range itself,
                                 ;; and together they were two thirds of the row.
                                 (dissoc m :in-comment? :rank :route-entry?
                                         :enclosing-form-name :hit_line
                                         :comment_start :form_name)
                                 strength)
                    (= "FOUND" (:status strength))
                    (assoc :anchor (anchor-for cache m)))))))
          ids)))))

;; @spec MCP-OP-THREAD-035
(defn export-note
  "How the browser reaches a script leg's definition -- or the statement that it
  does not have to.

  A classic script's functions are globals: there is no `export`, no
  `module.exports`, no `window.X =`, and a reader who is not TOLD that goes and
  searches for a registration site that does not exist. So say it. When there IS
  one, name the file, the line and the line's own text, which is the only form
  of this answer a reader can check."
  [cache leg identifiers]
  (let [{:keys [ok source lines]} (read-source cache (:file leg))]
    (when ok
      (let [idents (alternation (remove str/blank? (distinct identifiers)))]
        (if-not idents
          "none (no identifier seed to look for)"
          (let [re (re-pattern
                     (str "^\\s*export\\s+(?:default\\s+)?(?:async\\s+)?"
                          "(?:function|const|let|var|class)\\s+(?:" idents ")\\b"
                          "|^\\s*export\\s*\\{[^}]*\\b(?:" idents ")\\b"
                          "|\\bmodule\\.exports\\b[^\\n]*\\b(?:" idents ")\\b"
                          "|\\b(?:window|globalThis|self)\\.(?:" idents ")\\s*="))
                hit (first (keep-indexed
                             (fn [idx line] (when (re-find re line) [(inc idx) line]))
                             lines))]
            (if hit
              (str (:file leg) ":L" (first hit) "  " (str/trim (second hit)))
              (if (re-find #"(?m)^\s*(?:export|import)\s" source)
                "none for this subject (the file is a module: it exports other names)"
                "none (classic script; functions are globals)"))))))))

(defn resolve-thread
  "The legs of one subject, in the convention set's declared order, plus the
  automatic implementation leg.

  `walked-globs` is the glob set the workspace walk actually used. It is passed
  rather than recomputed so the automatic leg can REFUSE to report an outcome
  for files nobody scanned."
  ([cache paths conventions seeds handler]
   (resolve-thread cache paths conventions seeds handler nil nil))
  ([cache paths conventions seeds handler walked-globs scope-paths]
   (let [def-globs (->> (:legs conventions)
                        (filter #(= :def (:kind %)))
                        (mapcat :globs)
                        distinct
                        vec)
         ;; @spec MCP-OP-THREAD-035
         ;; @spec MCP-OP-THREAD-039
         enrich (fn [leg l]
                  (cond-> l
                    (and (located? l) (script-path? (:file l)))
                    (assoc :export (export-note cache l (:identifiers seeds)))

                    (= :use (:kind leg))
                    (as-> l' (if-let [ps (co-menu-peers cache paths def-globs l'
                                                        (:identifiers seeds))]
                               (assoc l' :peers ps)
                               l'))))
         declared (mapv (fn [leg]
                          (-> (resolve-leg cache paths seeds leg
                                           {:handler-name (:name handler)
                                            :scope-paths scope-paths})
                              (assoc :elide (elision-class leg))
                              (->> (enrich leg))))
                        (:legs conventions))]
     (if-let [auto-leg (implementation-leg conventions)]
       (conj declared (resolve-implementation
                        cache paths seeds
                        (assoc auto-leg :scope-paths scope-paths)
                        declared
                        (or walked-globs (:globs auto-leg))))
       declared))))

;; @spec MCP-OP-THREAD-013
;; @spec MCP-OP-THREAD-016
(defn thread-status
  "COMPLETE only when every COUNTED leg is FOUND. Computed from the leg vector,
  never written as a literal. A leg reported `N/A` — the automatic
  implementation leg when no seed names a definition — is not counted, because
  counting an inapplicable leg would report INCOMPLETE for a thread that is
  whole."
  [legs]
  (let [legs (remove #(= "N/A" (:status %)) legs)  ; UNSCANNED is NOT removed
        total (count legs)
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
  ([cache paths conventions seeds legs mirror]
   (resolve-sibling cache paths conventions seeds legs mirror nil nil))
  ([cache paths conventions seeds legs mirror walked-globs scope-paths]
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
            sib-legs (resolve-thread cache paths conventions sib-seeds
                                     sib-handler walked-globs scope-paths)]
        {:status "FOUND"
         :rule (if mirror "explicit-mirror" (name rule))
         :seed seed
         :at (:at derived)
         :legs (mapv (fn [l]
                       (cond-> (select-keys l [:id :status :file :from :to
                                               :evidence :sha256 :bytes :anchor])
                         (located? l)
                         (assoc :refetch (refetch-command (:file l) (:from l) (:to l)))))
                     sib-legs)})))))

;; ---------------------------------------------------------------------------
;; Rendering: the text block, and the completion pass that makes it a superset
;; ---------------------------------------------------------------------------

(defn leaf-paths
  "Every scalar leaf of a structured receipt as `[path value]`."
  [node path]
  (cond
    (map? node) (mapcat (fn [[k v]] (leaf-paths v (conj path (name k)))) node)
    (sequential? node) (mapcat (fn [i v] (leaf-paths v (conj path (str i))))
                               (range) node)
    (or (string? node) (number? node) (boolean? node)) [[path node]]
    :else []))

;; @spec MCP-OP-THREAD-046
(defn- face-walk
  [node strip?]
  (cond
    (map? node)
    (let [m (reduce-kv (fn [acc k v] (assoc acc k (face-walk v strip?))) {} node)
          m (cond-> m (some? (:body node)) (assoc :body_in_text true))]
      (cond-> m strip? (dissoc :body :after_context)))

    (sequential? node) (mapv #(face-walk % strip?) node)

    :else node))

;; @spec MCP-OP-THREAD-046
(defn public-face
  "The STRUCTURED face that is DELIVERED: every locator, no body.

  `text ⊇ structured` is the contract; it never required
  `structured ⊇ text`. The bodies and the anchor context are the receipt's
  bulk, the text block carries them already, and duplicating them into the
  structured face is what pinned that face against the trunk's fixed
  32,640-byte ceiling — 31,338 B on social-media-writer in edit-basis, so
  `peers` and `sibling` were elided at EVERY budget including the hard cap, and
  the agent went and read the peers from source (round-four T3b). So each map
  that carried a body now says `body_in_text true` and drops the bytes; a
  client that reads only this face still gets every file, range, digest,
  anchor, evidence, rule, refetch command and `next_call`."
  [result]
  (if (:ok result) (face-walk result true) result))

;; @spec MCP-OP-THREAD-012
(defn superset-subject
  "The subject of the text ⊇ structured check: the delivered structured face
  PLUS the bodies, so the check still proves every body reached the text."
  [result]
  (if (:ok result) (face-walk result false) result))

;; @spec MCP-OP-THREAD-012
(defn ensure-superset
  "Guarantee the text block carries every leaf of the structured receipt.

  Not a rule someone must remember: whatever the designed lines forget is
  appended here, so a text-reading client can never be told less than a
  structure-reading one. Its own witness reintroduces a dropped field and
  watches the line appear."
  ([text result] (ensure-superset text result text #{}))
  ([text result haystack] (ensure-superset text result haystack #{}))
  ([text result haystack vouched]
   ;; @spec MCP-OP-THREAD-026
   ;; `haystack` is the DESIGNED text WITHOUT the operation clock. The clock's
   ;; digits are not evidence that a leaf was reported: with
   ;; `elapsed_ms=229.543396` the leaf `sibling.legs.3.bytes=396` was "found"
   ;; inside the clock and dropped, so the delivered text came out 28 bytes
   ;; shorter than the `text_bytes` the receipt printed about itself, on 1 run
   ;; in 25 (round-three review, 3.1).
   ;;
   ;; `vouched` names the leaf paths the EXCLUDED tail carries by construction,
   ;; and it is exactly one: `receipt-tail` spells `elapsed_ms=<v>` whenever
   ;; that leaf exists. Exempting it by PATH rather than by substring is the
   ;; whole point -- a substring test over the clock is the defect above.
   (let [missing (->> (leaf-paths result [])
                      (remove (fn [[p _]] (contains? vouched p)))
                      (remove (fn [[_ v]] (str/includes? haystack (str v))))
                      (map (fn [[p v]] (str (str/join "." p) "=" (pr-str v))))
                      distinct)]
     (if (seq missing)
       (str text "\n structured-only · " (str/join " · " missing))
       text))))

;; @spec MCP-OP-THREAD-018
(defn- co-primary-line
  "A co-primary is rendered as a LEG row, never as an `also` row: it carries a
  boundary, a hash, a body and an anchor, and a reader scanning for `leg ` must
  see it."
  [leg member]
  (str "leg " (:id leg) "(" (:language member) ")  " (:file member)
       " L" (:from member) "-L" (:to member)
       " sha256:" (:sha256 member)
       " evid=" (:evidence member)
       " boundary=" (:boundary member)
       " bytes=" (:bytes member)
       (when (:anchor member) (str " anchor=" (:anchor member)))
       " refetch=" (:refetch member)
       (when (= "CANDIDATE" (:status member))
         (str " CANDIDATE weak=" (:weak_reason member)))
       (if (:body member)
         (str "\n  BODY<<\n" (:body member) "\n  >>")
         (str "\n  BODY ELIDED reason=" (:elided_reason member)
              " range=L" (:from member) "-L" (:to member)))))

(defn- leg-line
  [leg]
  (cond
    (= "N/A" (:status leg))
    (str "leg " (:id leg) "  " (:status leg) " · " (:id leg)
         ": n/a (" (:reason leg) ") — not counted in the leg status"
         (when (seq (:searches leg))
           (str "\n  searched: " (str/join "\n  searched: " (:searches leg)))))

    :else
    (if (located? leg)
    (str "leg " (:id leg) "  "
         (when (= "CANDIDATE" (:status leg))
           (str "CANDIDATE weak=" (:weak_reason leg)
                " — NOT counted toward COMPLETE — "))
         (:file leg) " L" (:from leg) "-L" (:to leg)
         " sha256:" (:sha256 leg)
         " evid=" (:evidence leg)
         " boundary=" (:boundary leg)
         " bytes=" (:bytes leg)
         (when (:anchor leg) (str " anchor=" (:anchor leg)))
         (when (:form_name leg) (str " form=" (:form_name leg)))
         (when (:export leg) (str " export=" (:export leg)))
         "\n  found by: " (str/join "\n  found by: " (:searches leg))
         ;; @spec MCP-OP-THREAD-039
         (when (seq (:peers leg))
           (str "\n  peer "
                (str/join "\n  peer "
                          (map (fn [p]
                                 (str (:identifier p) " " (:status p)
                                      (when (located? p)
                                        (str " " (:file p) " L" (:from p)
                                             "-L" (:to p)
                                             " sha256:" (:sha256 p)
                                             " evid=" (:evidence p)
                                             " boundary=" (:boundary p)
                                             (when (:anchor p)
                                               (str " anchor=" (:anchor p)))
                                             " refetch=" (:refetch p)))
                                      (when (:weak_reason p)
                                        (str " weak=" (:weak_reason p)))
                                      "\n    searched: " (:searched p)
                                      (when (:body p)
                                        (str "\n    BODY<<\n" (:body p)
                                             "\n    >>"))
                                      (when (:elided_reason p)
                                        (str "\n    BODY ELIDED reason="
                                             (:elided_reason p)))))
                               (:peers leg)))))
         (when (seq (:after_context leg))
           (str "\n  AFTER<< L" (:after_context_from leg)
                "-L" (:after_context_to leg) "\n"
                (str/join "\n" (:after_context leg)) "\n  >>"))
         (if (:body leg)
           (str "\n  BODY<<\n" (:body leg) "\n  >>")
           (str "\n  BODY ELIDED reason=" (:elided_reason leg)
                " range=L" (:from leg) "-L" (:to leg)
                " bytes=" (:bytes leg)
                " refetch=" (:refetch leg))))
    (str "leg " (:id leg) "  ABSENT"
         ;; @spec MCP-OP-THREAD-041
         (when (:absent_cause leg) (str " cause=" (:absent_cause leg)))
         (when (:evidence leg) (str " evid=" (:evidence leg)))
         (when (:reason leg) (str "\n  reason: " (:reason leg)))
         (when (:remedy leg) (str "\n  remedy: " (:remedy leg)))
         (when (seq (:searches leg))
           (str "\n  searched: "
                (str/join "\n  searched: " (:searches leg))))
         (when (seq (:unreadable leg))
           (str "\n  unreadable: "
                (str/join ", " (map #(str (:file %) " (" (:reason %) ")")
                                    (:unreadable leg)))))))))

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


;; @spec MCP-OP-THREAD-026
(defn receipt-tail
  "The operation clock, padded to exactly `receipt-tail-bytes` ASCII bytes.

  Carries BOTH spellings -- the rendered duration a human reads and the raw
  `elapsed_ms` leaf -- so the text-superset guarantee finds the value it is
  looking for and the padded width does not change between the measuring pass
  and the delivered one."
  [elapsed-ms]
  (let [body (if (number? elapsed-ms)
               (str "elapsed " (mcp-operation/format-elapsed-ms elapsed-ms)
                    " (elapsed_ms=" elapsed-ms ")")
               "elapsed pending (stamped by the finalizer)")
        body (if (> (count body) receipt-tail-bytes)
               (subs body 0 receipt-tail-bytes)
               body)]
    (apply str body (repeat (- receipt-tail-bytes (count body)) \space))))

(defn render-receipt
  "The visible receipt. The completion pass at the end is the ratchet: whatever
  the designed lines forget, the text still carries, because a text-reading
  client must never be told less than a structure-reading one."
  [result]
  (let [legs (:legs result)
        ;; @spec MCP-OP-THREAD-026
        ;; `self?` renders the header with its three SELF-DESCRIBING counts
        ;; blanked. Those digits go into the delivered header but never into
        ;; the superset haystack, for the same reason the clock does not: a
        ;; number the receipt prints ABOUT ITSELF is not evidence that a
        ;; structured leaf was reported. Leaving them in made the completion
        ;; line flip with the digit count and `measure`'s fixpoint oscillate --
        ;; six rounds, then a declared 10084 over a delivered 10121.
        header-for (fn [self?]
                     (str "receipt feature-thread/v2  subject=" (:subject result)
                    (when (seq (:also_seeds result))
                      (str " also=" (str/join "," (:also_seeds result))))
                    "  root=" (:workspace_root result)
                    "  repo=" (:repo_label result)
                    ;; @spec MCP-OP-THREAD-022
                    ;; Which number the budget governs, said in the header: a
                    ;; reader shown `budget=32768B used=40641B ... COMPLETE`
                    ;; read a budget overrun beside a false COMPLETE. The
                    ;; budget governs the TEXT; COMPLETE is about LEGS.
                    "  text=" (if self? (:text_bytes result) "") "B (budget "
                    (:budget_bytes result) "B)"
                    "  structured=" (if self? (:structured_bytes result) "")
                    "B (trunk cap " trunk-public-byte-budget "B)"
                    "  total=" (if self? (:receipt_bytes result) "") "B"
                    "  status=" (:status result) " — legs, not bytes"))
        header (header-for true)
        body-lines (remove nil?
                           (concat (mapcat (fn [l]
                                             (cons (leg-line l)
                                                   (map #(co-primary-line l %)
                                                        (:co_primaries l))))
                                           legs)
                                   (map also-line legs)))
        sibling (:sibling result)
        sibling-line
        (if (= "FOUND" (:status sibling))
          (str "sibling " (:seed sibling) " rule=" (:rule sibling)
               (when (:at sibling) (str " at=" (:at sibling)))
               "  legs: "
               (str/join " · " (map #(str (:id %) " " (:status %)
                                          (when (located? %)
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
             ;; @spec MCP-OP-THREAD-037
             (when-let [rc (:request_contract rules)]
               (str "\n  request_contract " (:route rc)
                    " handler_reads=" (pr-str (:handler_reads rc))
                    " js_posts=" (pr-str (:js_posts rc))
                    " agree?=" (:agree? rc)
                    (when-not (:agree? rc)
                      (str " only_in_js=" (pr-str (:only_in_js rc))
                           " only_in_handler=" (pr-str (:only_in_handler rc))))))
             (when-let [axis (:axis rules)]
               (str "\n  axis " (:name axis)
                    " precedents=" (pr-str (:precedents axis))))
             (when (seq (:governance rules))
               (str "\n  governance "
                    (str/join "\n  governance "
                              (map #(str (:file %) ":" (:line %)
                                         (when (:form_end %)
                                           (str " entry L" (:form_start %)
                                                "-L" (:form_end %)))
                                         " anchor=" (:anchor %)
                                         "  " (:match %)
                                         "  refetch=" (:refetch %))
                                   (:governance rules)))))
             (when-let [t (:governance_template rules)]
               (str "\n  governance-template " (:file t)
                    " L" (:from t) "-L" (:to t)
                    " anchor=" (:anchor t)
                    " refetch=" (:refetch t)
                    "  — the entry a new row is modelled on; NOT inlined"))
             (if (seq (:verify rules))
               (str "\n  verify "
                    (str/join "\n  verify "
                              (map #(str (:target %) " (Makefile:" (:line %) ")"
                                         " for=" (:for %)
                                         " evidence=" (:evidence %)
                                         "  " (:command %)
                                         ;; @spec MCP-OP-THREAD-048
                                         (when-let [r (:runs_namespace %)]
                                           (str "\n    runs_namespace "
                                                (:namespace r)
                                                " picks_up=" (:picks_up r)
                                                (when (:suite r)
                                                  (str " suite=" (:suite r)))
                                                (when (:suites_tried r)
                                                  (str " suites_tried="
                                                       (str/join ","
                                                                 (:suites_tried r))))
                                                " from=" (:from r)
                                                " — " (:why r))))
                                   (:verify rules))))
               (str "\n  verify none · " (:verify_reason rules)))
             "\nassert " (:assert rules)
             ;; @spec MCP-OP-THREAD-038
             (when (seq (:absent result))
               (str "\n  absent "
                    (str/join "\n  absent "
                              (map #(str (:identifier %) " — " (:reason %)
                                         "\n    searched: " (:searched %))
                                   (:absent result)))))
             (when-let [n (:next_call result)]
               (str "\nnext_call " (:tool n) " expect_pre_sha256="
                    (str/join " " (map (fn [[f sha]] (str f ":" sha))
                                       (sort (:expect_pre_sha256 n))))
                    (when (seq (:by_leg n))
                      (str "\n  by_leg "
                           (str/join "\n  by_leg "
                                     (map (fn [[leg m]]
                                            (str (name leg) ": "
                                                 (str/join " " (sort (keys m)))))
                                          (sort-by (comp name key)
                                                   (:by_leg n))))))
                    "  — " (:note n))))
        elisions (:elided result)
        elision-lines (map #(str "elided " (:leg %) " " (:bytes %)
                                 "B reason=" (:reason %)
                                 " range=L" (:from %) "-L" (:to %)
                                 " sha256:" (:sha256 %)
                                 " refetch=" (:refetch %))
                           elisions)
        after-header (remove nil? (concat body-lines [sibling-line rules-line]
                                          elision-lines))
        ;; The DELIVERED text carries the real counts; the HAYSTACK does not.
        designed (str (str/join "\n" (cons header after-header))
                      "\n" (receipt-tail (:elapsed_ms result)))
        without-clock (str/join "\n" (cons (header-for false) after-header))]
    (ensure-superset designed
                     (superset-subject
                       (dissoc result :receipt_bytes :text_bytes
                               :structured_bytes))
                     without-clock
                     #{["elapsed_ms"]})))

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
          ;; @spec MCP-OP-THREAD-046
          ;; The number the trunk cap governs is the face that is DELIVERED.
          st (utf8-bytes (json/generate-string (public-face candidate)))
          total (+ t st)]
      (if (or (and (= total n) (= t tb) (= st sb)) (>= rounds 6))
        [text (assoc result :receipt_bytes total
                     :text_bytes t :structured_bytes st)
         t]
        (recur total t st (inc rounds))))))

;; @spec MCP-OP-THREAD-045
(defn elision-reason
  "The label a cut carries, naming WHAT bound.

  `public-budget` is the caller's `budget_bytes`; `structured-cap` is the
  trunk's fixed ceiling on the structured face, which no `budget_bytes` raises."
  [binder]
  (if (= binder :structured-cap) "structured-cap" "public-budget"))

;; @spec MCP-OP-THREAD-045
(defn elision-remedy
  "How to get the cut content back — the remedy that actually works for the
  constraint that made the cut.

  Round-five review, finding 6: at `budget_bytes 32768` — the hard cap — the
  receipt cut `sibling` and `peers` to satisfy the STRUCTURED cap and told the
  caller to \"re-run with a larger budget_bytes\". There is no larger one. A
  remedy the caller cannot execute is worse than no remedy: it spends a call to
  learn the advice was impossible."
  [binder]
  (if (= binder :structured-cap)
    (str "re-run feature_thread with mode=locations — budget_bytes cannot raise"
         " the structured cap of " trunk-public-byte-budget "B")
    "re-run feature_thread with a larger budget_bytes"))

(defn- elide-leg
  [leg reason]
  (if (and (located? leg) (:body leg))
    [(-> leg
         ;; @spec MCP-OP-THREAD-036
         ;; after_context is body-class detail and goes with the body: a leg
         ;; whose body was cut for budget must not keep quoting source.
         (dissoc :body :after_context :after_context_from :after_context_to)
         (assoc :elided_reason reason))
     {:leg (:id leg) :bytes (:bytes leg) :reason reason
      :from (:from leg) :to (:to leg) :sha256 (:sha256 leg)
      :refetch (:refetch leg)}]
    [leg nil]))

;; @spec MCP-OP-THREAD-011
(defn apply-elision
  "Apply one step of the stated elision order. Returns `[result applied?]`.

  `binder` names the constraint that forced the cut, so every ledger row says
  what actually bound and offers a remedy the caller can execute."
  [result class binder]
  (case class
    :secondary-tests
    (let [dropped (reduce + 0 (map #(count (:also %)) (:legs result)))]
      (if (zero? dropped)
        [result false]
        [(-> result
             (update :legs #(mapv (fn [l] (assoc l :also [])) %))
             (update :elided conj {:leg "secondary-witnesses"
                                   :bytes 0
                                   :reason (str (elision-reason binder) "; " dropped
                                                " secondary witness rows dropped")
                                   :from 0 :to 0 :sha256 "n/a"
                                   :refetch (elision-remedy binder)}))
         true]))

    ;; @spec MCP-OP-THREAD-017
    :governance-template
    (if-let [t (get-in result [:rules :governance_template])]
      [(-> result
           (update :rules dissoc :governance_template)
           (update :elided conj {:leg "governance-template"
                                 :bytes 0
                                 :reason (elision-reason binder)
                                 :from (:from t) :to (:to t)
                                 :sha256 "n/a"
                                 :refetch (:refetch t)}))
       true]
      [result false])

    ;; @spec MCP-OP-THREAD-025
    :next-call
    (if-let [n (:next_call result)]
      [(-> result
           (dissoc :next_call)
           (update :elided conj
                   {:leg "next-call"
                    :bytes 0
                    :reason (elision-reason binder)
                    :from 0 :to 0 :sha256 "n/a"
                    :refetch (str "re-run with mode=locations for the "
                                  (count (:expect_pre_sha256 n))
                                  " whole-file digests admit binds")}))
       true]
      [result false])

    ;; @spec MCP-OP-THREAD-018
    :tests-js
    (let [idx (first (keep-indexed
                       (fn [i l] (when (some :body (:co_primaries l)) i))
                       (:legs result)))]
      (if (nil? idx)
        [result false]
        (let [leg (nth (:legs result) idx)
              cut (filterv :body (:co_primaries leg))
              leg' (update leg :co_primaries
                           #(mapv (fn [m]
                                    (if (:body m)
                                      (-> m (dissoc :body)
                                          (assoc :elided_reason (elision-reason binder)))
                                      m))
                                  %))]
          [(-> result
               (assoc :legs (assoc (vec (:legs result)) idx leg'))
               (update :elided into
                       (map (fn [m] {:leg (str (:id leg) "(" (:language m) ")")
                                     :bytes (:bytes m)
                                     :reason (elision-reason binder)
                                     :from (:from m) :to (:to m)
                                     :sha256 (:sha256 m)
                                     :refetch (:refetch m)})
                            cut)))
           true])))

    ;; @spec MCP-OP-THREAD-039
    ;; Peer BODIES go first after the sibling: a peer is context for shaping
    ;; the edit, not the edit site, and every peer keeps its range, sha256 and
    ;; anchor when its body is cut. One ledger row, not one per peer.
    :peers
    (let [carrying (fn [l] (some :body (:peers l)))
          cut (count (mapcat #(filter :body (:peers %)) (:legs result)))]
      (if (zero? cut)
        [result false]
        [(-> result
             (update :legs
                     #(mapv (fn [l]
                              (if (carrying l)
                                (update l :peers
                                        (fn [ps]
                                          (mapv (fn [pp]
                                                  (if (:body pp)
                                                    (-> pp (dissoc :body)
                                                        (assoc :elided_reason
                                                               (elision-reason binder)))
                                                    pp))
                                                ps)))
                                l))
                            %))
             (update :elided conj
                     {:leg "peers"
                      :bytes 0
                      :reason (str (elision-reason binder) "; " cut
                                   " co-menu-item peer bodies dropped")
                      :from 0 :to 0 :sha256 "n/a"
                      :refetch (elision-remedy binder)}))
         true]))

    ;; @spec MCP-OP-THREAD-048
    ;; The `runs_namespace` explanation is prose ABOUT a command the row still
    ;; names; the command and its Makefile line survive, so what is lost is the
    ;; sentence, not the answer's address. One ledger row, not one per target.
    :verify-detail
    (let [rows (get-in result [:rules :verify])
          cut (count (filter :runs_namespace rows))]
      (if (zero? cut)
        [result false]
        [(-> result
             (assoc-in [:rules :verify]
                       (mapv #(dissoc % :runs_namespace) rows))
             (update :elided conj
                     {:leg "verify-detail"
                      :bytes 0
                      :reason (str (elision-reason binder) "; the"
                                   " runs_namespace explanation of " cut
                                   " verify rows dropped")
                      :from 0 :to 0 :sha256 "n/a"
                      :refetch (elision-remedy binder)}))
         true]))

    ;; @spec MCP-OP-THREAD-036
    ;; after_context is the FIRST thing cut after the sibling: it is the only
    ;; part of the receipt whose re-fetch is a single exact `sed` the receipt
    ;; itself prints, so losing it costs the caller one cheap call and losing
    ;; `next_call` costs it the write gate.
    :after-context
    (let [carrying (fn [l] (or (seq (:after_context l))
                               (some #(seq (:after_context %)) (:co_primaries l))))
          strip (fn [m] (dissoc m :after_context :after_context_from
                                :after_context_to))
          cut (filter carrying (:legs result))]
      (if (empty? cut)
        [result false]
        [(-> result
             (update :legs
                     #(mapv (fn [l] (-> (strip l)
                                        (update :co_primaries
                                                (fn [co] (mapv strip (or co []))))))
                            %))
             ;; ONE ledger row, not one per leg: a per-leg row costs ~120
             ;; bytes each and this step exists to RECLAIM bytes. Every leg
             ;; still names its own anchor and refetch.
             (update :elided conj
                     {:leg "after-context"
                      :bytes 0
                      :reason (str "public-budget; the anchor context of "
                                   (count cut) " legs dropped")
                      :from 0 :to 0 :sha256 "n/a"
                      :refetch (elision-remedy binder)}))
         true]))

    :sibling
    (if (seq (get-in result [:sibling :legs]))
      [(-> result
           (assoc-in [:sibling :legs] [])
           (assoc-in [:sibling :elided] true)
           (update :elided conj {:leg "sibling"
                                 :bytes 0
                                 :reason (elision-reason binder)
                                 :from 0 :to 0 :sha256 "n/a"
                                 :refetch (str "feature_thread subject="
                                               (get-in result [:sibling :seed]))}))
       true]
      [result false])

    (let [legs (:legs result)
          idx (first (keep-indexed (fn [i l] (when (= class (:elide l)) i)) legs))]
      (if (nil? idx)
        [result false]
        (let [[leg' entry] (elide-leg (nth legs idx) (elision-reason binder))]
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
  [result budget order]
  (loop [current (assoc result :elided [])
         remaining order]
    (let [[text measured total] (measure current)]
      (cond
        (and (<= total budget)
             (<= (:structured_bytes measured) trunk-public-byte-budget))
        [text measured]

        (empty? remaining)
        (let [refusal {:ok false
                       :operation "feature_thread"
                       :error_type "feature-thread-budget-exceeded"
                       :error (str "the receipt text is " total
                                   " bytes with every body elided, above the"
                                   " budget of " budget " this request asked"
                                   " for")
                       :budget_bytes budget
                       ;; @spec MCP-OP-THREAD-026
                       ;; NOT `text_bytes`: this counts the receipt that could
                       ;; not be sent, and `text_bytes` means the delivered text
                       ;; everywhere else (round-three review, 3.1).
                       :would_be_text_bytes total
                       :subject (:subject result)
                       :status (:status result)
                       :remedy (str "Raise budget_bytes (hard cap "
                                    hard-cap-bytes ") or narrow scope.paths.")}]
          ;; The TEXT is re-rendered by `summary` from this map, so the string
          ;; here is diagnostic only; the map is the subject.
          [(str "feature_thread refused · " (:error_type refusal) " · "
                (:error refusal) "\nremedy · " (:remedy refusal)
                "\nfacts · budget_bytes=" budget " would_be_text_bytes=" total
                " subject=" (:subject result) " status=" (:status result))
           refusal])

        :else
        ;; @spec MCP-OP-THREAD-045
        ;; Which constraint is still unsatisfied decides the ledger's wording:
        ;; over the caller's text budget is `public-budget`; under it and still
        ;; over the trunk's structured ceiling is `structured-cap`, which no
        ;; `budget_bytes` can raise.
        (let [binder (if (> total budget) :text-budget :structured-cap)
              [next-result applied?] (apply-elision current (first remaining)
                                                    binder)]
          (recur (if applied? next-result current) (rest remaining)))))))

;; ---------------------------------------------------------------------------
;; Admission
;; ---------------------------------------------------------------------------

(def allowed-fields
  #{:subject :also :scope :config :budget_bytes :include_bodies :mode :mirror
    :axis :workspace_root :probe :peer_bodies})

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
        {:keys [subject also scope budget_bytes include_bodies mode mirror
                peer_bodies]} params]
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

      ;; @spec MCP-OP-THREAD-027
      (> (count subject) max-subject-chars)
      (refuse "feature-thread-subject-too-long"
              (str "subject is " (count subject) " characters, above the"
                   " ceiling of " max-subject-chars)
              {:max_subject_chars max-subject-chars
               :field "subject"
               :remedy (str "Pass an identifier or a route, at most "
                            max-subject-chars " characters.")})

      (and (some? also) (not (and (sequential? also) (every? string? also))))
      (refuse "feature-thread-invalid-also"
              "also must be an array of additional identifier or route seeds"
              {:remedy "Pass also as a vector of strings, or omit it."})

      ;; @spec MCP-OP-THREAD-027
      (and (sequential? also) (> (count also) max-also-seeds))
      (refuse "feature-thread-also-too-many"
              (str "also carries " (count also) " seeds, above the ceiling of "
                   max-also-seeds)
              {:max_also_seeds max-also-seeds
               :field "also"
               :remedy (str "Pass at most " max-also-seeds " seeds.")})

      ;; @spec MCP-OP-THREAD-027
      (and (sequential? also) (some #(> (count %) max-subject-chars) also))
      (refuse "feature-thread-also-seed-too-long"
              (str "an also seed is longer than the ceiling of "
                   max-subject-chars " characters")
              {:max_subject_chars max-subject-chars
               :field "also"
               :remedy (str "Every seed is an identifier or a route, at most "
                            max-subject-chars " characters.")})

        ;; @spec MCP-OP-THREAD-038
      (and (some? (:probe params))
           (not (and (sequential? (:probe params))
                     (every? #(and (string? %) (seq (str/trim %))) (:probe params)))))
      (refuse "feature-thread-invalid-probe"
              "probe must be an array of non-blank identifiers to rule in or out"
              {:field "probe"
               :remedy "Pass probe as a vector of strings, or omit it."})

      ;; @spec MCP-OP-THREAD-038
      (and (sequential? (:probe params))
           (> (count (:probe params)) max-probe-identifiers))
      (refuse "feature-thread-probe-too-many"
              (str "probe carries " (count (:probe params)) " identifiers,"
                   " above the ceiling of " max-probe-identifiers)
              {:max_probe_identifiers max-probe-identifiers
               :field "probe"
               :remedy (str "Pass at most " max-probe-identifiers
                            " probe identifiers.")})

      ;; @spec MCP-OP-THREAD-027
      (and (sequential? (:probe params))
           (some #(> (count %) max-subject-chars) (:probe params)))
      (refuse "feature-thread-probe-identifier-too-long"
              (str "a probe identifier is longer than the ceiling of "
                   max-subject-chars " characters")
              {:max_subject_chars max-subject-chars
               :field "probe"
               :remedy (str "Every probe identifier is at most "
                            max-subject-chars " characters.")})

    (and (some? scope) (not (map? scope)))
      (refuse "feature-thread-invalid-scope"
              "scope must be an object with optional workspace_root and paths"
              {:remedy "Pass scope as {\"workspace_root\": ..., \"paths\": [...]}."})

      (and (some? (:paths scope))
           (not (and (sequential? (:paths scope)) (every? string? (:paths scope)))))
      (refuse "feature-thread-invalid-scope"
              "scope.paths must be an array of repo-relative directory paths"
              {:remedy "Pass scope.paths as a vector of strings, or omit it."})

      ;; @spec MCP-OP-THREAD-043
      ;; scope.paths is rendered into the receipt's own `rg` lines as extra
      ;; `-g` filters, so an escaping path would be PUBLISHED as part of the
      ;; file set the verb claims to have searched. Same shapes, same refusal
      ;; point: before any file is read.
      (and (sequential? (:paths scope))
           (some escaping-glob-shape (:paths scope)))
      (let [bad (first (filter escaping-glob-shape (:paths scope)))]
        (refuse "feature-thread-scope-path-escapes-workspace"
                (str "the scope path '" bad "' names a path outside the"
                     " workspace: "
                     (escaping-glob-shape-reason (escaping-glob-shape bad)))
                {:field "scope.paths"
                 :path bad
                 :remedy (str "Pass scope.paths as directories relative to the"
                              " workspace root.")}))

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

      ;; @spec MCP-OP-THREAD-047
      (and (some? peer_bodies) (not (boolean? peer_bodies)))
      (refuse "feature-thread-invalid-peer-bodies"
              "peer_bodies must be true or false"
              {:remedy (str "Omit peer_bodies for opportunistic peer bodies,"
                            " pass true to ask for them, false for ranges"
                            " only.")})

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

;; @spec MCP-OP-THREAD-047
(defn- strip-peer-bodies
  "Peer ROWS without peer BODIES: `{id, file, from, to, sha256, anchor,
  refetch}`, about 120 bytes each. What the caller asked for when they passed
  `peer_bodies false`, and what a peer is worth carrying at any budget."
  [legs]
  (mapv (fn [l]
          (if (seq (:peers l))
            (update l :peers
                    #(mapv (fn [p] (cond-> (dissoc p :body)
                                     (:body p) (assoc :body_omitted
                                                      "peer_bodies=false")))
                           %))
            l))
        legs))

(defn- strip-bodies
  [legs]
  (mapv (fn [l]
          (cond-> l
            (and (located? l) (:body l))
            (-> (dissoc :body) (assoc :elided_reason "mode=locations"))

            (seq (:co_primaries l))
            (update :co_primaries
                    #(mapv (fn [m] (-> m (dissoc :body)
                                       (assoc :elided_reason "mode=locations")))
                           %))))
        legs))

(defn execute-request
  "Resolve one feature thread. Pure with respect to the workspace: it reads
  files and writes nothing."
  [config params]
  (let [normalized (json/parse-string (json/generate-string params) true)
        admission (admit normalized)]
    (if-not (:ok admission)
      admission
      (let [{:keys [subject also scope budget_bytes include_bodies mode mirror axis
                    probe peer_bodies]}
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
                    ;; @spec MCP-OP-THREAD-028
                    ;; The automatic implementation leg's globs are unioned in
                    ;; BEFORE the walk. Round three bounded the walk here and
                    ;; built that leg later, so it could only ever find a
                    ;; definition inside a file a DECLARED leg already selected
                    ;; — and reported `N/A` (uncounted, thread COMPLETE) for the
                    ;; files it never opened.
                    candidate-globs (vec (distinct
                                           (concat
                                             (mapcat :globs (:legs conventions))
                                             (:globs (implementation-leg conventions))
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
                        legs (resolve-thread cache paths conventions seeds
                                             handler candidate-globs
                                             (:paths scope))
                        legs (if bodies? legs (strip-bodies legs))
                        ;; @spec MCP-OP-THREAD-047
                        legs (if (false? peer_bodies)
                               (strip-peer-bodies legs)
                               legs)
                        status (thread-status legs)
                        sibling (resolve-sibling cache paths conventions seeds
                                                 legs mirror candidate-globs
                                                 (:paths scope))
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
                                :next_call (next-call cache legs)
                                ;; @spec MCP-OP-THREAD-038
                                :absent (negative-evidence
                                          cache paths candidate-globs
                                          (concat (:identifiers seeds)
                                                  (or probe [])))
                                :elided []}
                               status)
                        [_ fitted] (fit-to-budget
                                     base budget
                                     (elision-order-for (true? peer_bodies)))]
                    fitted))))))))))

;; ---------------------------------------------------------------------------
;; The MCP entrance
;; ---------------------------------------------------------------------------

;; @spec MCP-OP-THREAD-012
(defn summary
  "The text block. Re-rendered from the structured receipt, so the two faces of
  the result cannot disagree, and completed so the text is a superset.

  The clock is rendered INSIDE the receipt at a fixed width rather than
  appended afterwards, so `text_bytes` counts the text that is delivered."
  [result]
  (if (:ok result)
    (render-receipt result)
    (let [without-clock (str "feature_thread refused · " (:error_type result)
                             "\n→ " (:error result)
                             (when-let [remedy (:remedy result)]
                               (str "\nremedy · " remedy))
                             ;; @spec MCP-OP-THREAD-026
                             ;; The count on a refusal is NOT `text_bytes`: it
                             ;; describes the receipt that could not be sent.
                             ;; The text reader must see the same NAME the
                             ;; structured reader does, not just the digits
                             ;; (round-three review, 3.1).
                             (when-let [wb (:would_be_text_bytes result)]
                               (str "\nfacts · would_be_text_bytes=" wb
                                    " budget_bytes=" (:budget_bytes result)
                                    " — the text delivered is this refusal,"
                                    " not that receipt")))]
      (ensure-superset
        (str without-clock "\n" (receipt-tail (:elapsed_ms result)))
        result
        without-clock
        #{["elapsed_ms"]}))))

(def feature-thread-schema
  {:type "object"
   :additionalProperties false
   :properties
   {"subject" {:type "string" :minLength 1
               :description (str "The identifier or route the feature is named"
                                 " by. A value beginning with / is a route.")}
    "also" {:type "array" :items {:type "string" :minLength 1}
            :description "Additional identifier or route seeds for the same thread."}
    "probe" {:type "array" :items {:type "string" :minLength 1}
             :description (str "Extra identifiers to rule IN or OUT. Any that"
                               " have no occurrence outside a comment are"
                               " reported in `absent` with the search that says"
                               " so, so a reader never has to run rg to prove a"
                               " negative.")}
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
                    :description (str "Receipt budget in UTF-8 bytes of the"
                                      " rendered TEXT; default 28672, hard cap"
                                      " 32768. Over budget, bodies are elided in"
                                      " a stated edit-aware order — context"
                                      " first, the forms you are about to edit"
                                      " last — and every elision is named.")}
    "include_bodies" {:type "boolean"
                      :description "false returns ranges only (mode=locations)."}
    "peer_bodies" {:type "boolean"
                   :description (str "Co-menu-item peers always ride as ranges"
                                     " (file, range, sha256, anchor, refetch)."
                                     " Omit for their BODIES to ride whenever"
                                     " the budget has room after the legs;"
                                     " true to ask for them explicitly, so"
                                     " they outlive the sibling; false for"
                                     " ranges only.")}
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
    "would_be_text_bytes" {:type "integer"}
    "absent" {:type "array" :items {:type "object"}}
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
    "the INTENT identifiers found in comments above the located forms, the "
    "governance rows (intent registry, contract test, test target) that a "
    "search on the subject never reaches, each with the END of the entry it "
    "sits in and an anchor after it, and a `verify` row naming the Makefile "
    "target that runs the tests leg, its line and its recipe verbatim. Every "
    "convention set also gets an automatic `implementation` leg for the "
    "DEFINITION a seed names, deduped against the legs already found and "
    "reported n/a (uncounted) when no seed names one; and a tests leg whose "
    "hits span two languages returns a primary per language, each with its own "
    "boundary, hash and anchor. Clojure legs are PARSED to the "
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
     ;; @spec MCP-OP-THREAD-046
     ;; The text is rendered from the FULL result, which is why `summarize`
     ;; still sees the bodies; what is PUBLISHED as structuredContent — and
     ;; what `structured_bytes` counted — is the body-free face. Both the
     ;; serialized body and the callback get the same one, so the two cannot
     ;; disagree.
     :serialize #(json/generate-string (public-face %))
     :callback (fn [content error? structured]
                 (callback content error? (public-face structured)))}))

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
