(ns clj-surgeon.parse-admission
  "Bounded lexical/parser admission — MCP-OP-MEM-005.

   A tree-scale operation's heap must be bounded by a file's SHAPE, not only by
   its byte count. Measured on anvil 2026-09-03 (`make memory-red`): ONE 111 KB
   file nested 300 `{:k [` levels deep either throws StackOverflowError out of
   the reader in 42 ms on a cold JVM — killing the whole `ls-tree` scan, because
   `core/safe-outline` catches `Exception` and not `Error` — or, once the
   parser's hot path is JIT-compiled, completes while consuming 312.4 MB, 2,876x
   its own source, against a 247.8 MB budget. A byte ceiling cannot see either:
   111 KB is a fifth of the per-file byte ceiling the design calls for.

   So before a full rewrite-clj tree is allocated, one pass over the raw string
   estimates the tree's node count and its maximum nesting depth — nesting
   meaning every construct the READER recurses into, reader-macro prefixes as
   well as structural delimiters, because a prefix tower 155x smaller than that
   111 KB file overflows the same stack. Over
   a ceiling, the input is refused with a typed refusal that names the limit,
   what was observed, and a remedy — and the tree constructor is never invoked.

   Scope, deliberately narrow: this is a SHAPE ceiling. Per-file and aggregate
   BYTE ceilings belong to MCP-OP-MEM-002 and run first; they carry the byte
   remedy and the narrowing `next_call`. Tuning `max-parse-nodes` down until it
   refuses large ORDINARY files converts this control into a worse copy of that
   one. See docs/intent/read-path-memory/read-path-memory-design.md for the
   ceiling derivation and the measurements behind every number.")

;; ============================================================
;; Ceilings
;; ============================================================

(def default-ceilings
  "Server parser ceilings. Both are derived from measurement; neither is a
   judgement call. The derivation, with its ladders and margins, is in
   docs/intent/read-path-memory/read-path-memory-design.md.

   `:max-parse-depth` 150 — the lowest observed COLD StackOverflowError in the
   rewrite-clj reader is 460 nesting levels at the default 1 MB stack (440
   completes, 480 always overflows). The deepest of the 163 Clojure-family
   sources under `src/` and `test/` is 22. 150 sits 6.8x above real code and
   3.07x below the crash, and is placed against the COLD threshold because that
   is the lower of the two JIT branches.

   `:max-parse-nodes` 200,000 — bounded from above so it can never pre-empt
   MEM-002's ~512 KiB per-file byte ceiling (at the highest node density
   measured over any corpus, 273.6 nodes/KiB, that ceiling yields at most
   140,288 nodes), and from below so it actually bounds heap (scaling the
   measured 1.9 MiB cell, 532,424 nodes peaked 339.9 MB at -Xmx512m, puts
   200,000 nodes near 128 MB of transient peak, inside the battery's 247.8 MB
   budget). The largest source here is 19,528 nodes — a 10.2x margin."
  {:max-parse-nodes 200000
   :max-parse-depth 150})

(def ^:dynamic *ceilings*
  "The ceilings admission applies. Bind it to exercise a ceiling dynamically
   rather than asserting a constant, or to lift it for a differential check."
  default-ceilings)

;; ============================================================
;; The lexical scan
;; ============================================================

;; Character codes, compared as primitive longs. The scan runs on EVERY read
;; through the parse entries, so it must not box a character per input byte:
;; boxing cost 65 ms on the 1.9 MiB fixture, primitive comparison costs 8 ms.
(def ^:private ^:const TAB 9)
(def ^:private ^:const NEWLINE 10)
(def ^:private ^:const SPACE 32)
(def ^:private ^:const QUOTE 34)
(def ^:private ^:const HASH 35)
(def ^:private ^:const COMMA 44)
(def ^:private ^:const SEMICOLON 59)
(def ^:private ^:const OPEN-PAREN 40)
(def ^:private ^:const CLOSE-PAREN 41)
(def ^:private ^:const OPEN-BRACKET 91)
(def ^:private ^:const BACKSLASH 92)
(def ^:private ^:const CLOSE-BRACKET 93)
(def ^:private ^:const OPEN-BRACE 123)
(def ^:private ^:const CLOSE-BRACE 125)
(def ^:private ^:const BANG 33)
(def ^:private ^:const SINGLE-QUOTE 39)
(def ^:private ^:const EQUALS 61)
(def ^:private ^:const QUESTION 63)
(def ^:private ^:const AT 64)
(def ^:private ^:const CARET 94)
(def ^:private ^:const UNDERSCORE 95)
(def ^:private ^:const BACKTICK 96)
(def ^:private ^:const TILDE 126)

(defn- ws?
  "Clojure treats a comma as whitespace."
  [^long c]
  (or (== c SPACE) (== c NEWLINE) (== c TAB) (== c COMMA)
      (Character/isWhitespace (int c))))

(defn- opens? [^long c]
  (or (== c OPEN-PAREN) (== c OPEN-BRACKET) (== c OPEN-BRACE)))

(defn- closes? [^long c]
  (or (== c CLOSE-PAREN) (== c CLOSE-BRACKET) (== c CLOSE-BRACE)))

(defn- delimiter? [^long c]
  (or (opens? c) (closes? c)))

(defn- token-char? [^long c]
  (and (not (ws? c))
       (not (delimiter? c))
       (not (== c QUOTE))
       (not (== c SEMICOLON))
       (not (== c BACKSLASH))))

(defn- token-end
  "Index one past the token run starting at `i`."
  ^long [^String source ^long i ^long n]
  (loop [j i]
    (if (and (< j n) (token-char? (int (.charAt source j))))
      (recur (inc j))
      j)))

(defn- reader-prefix-start? [^long c]
  (or (== c SINGLE-QUOTE) (== c BACKTICK) (== c TILDE)
      (== c AT) (== c CARET) (== c HASH)))

(defn- prefix-length
  "The character length of the reader-macro prefix at `i`, or 0 when there is
   none there.

   These are the constructs that make the reader ALLOCATE A WRAPPING NODE AND
   RECURSE INTO ITS CHILD without opening a structural delimiter: quote,
   syntax-quote, unquote, unquote-splicing, deref, metadata, var-quote,
   discard, eval, and the two reader conditionals. A DELIMITER-only estimate
   scores a tower of them zero — measured on anvil 2026-09-03, a 710-byte
   `(def x @@@...y)` scanned at depth 1, was admitted, and threw
   StackOverflowError out of the reader, killing a whole `ls-tree` scan.

   Consulted only at a form-start position. Inside a token run `'`, `@` and `~`
   are ordinary constituent characters (`foo'`), and the token branch has
   already consumed them."
  ^long [^String source ^long i ^long n]
  (let [c (int (.charAt source i))
        d (if (< (inc i) n) (int (.charAt source (inc i))) -1)]
    (cond
      (== c HASH)
      (cond
        (or (== d SINGLE-QUOTE) (== d UNDERSCORE) (== d EQUALS)) 2
        (== d QUESTION) (if (and (< (+ i 2) n)
                                 (== AT (int (.charAt source (+ i 2)))))
                          3
                          2)
        :else 0)

      (== c TILDE) (if (== d AT) 2 1)

      (or (== c SINGLE-QUOTE) (== c BACKTICK) (== c AT) (== c CARET)) 1

      :else 0)))

;; @spec MCP-OP-MEM-005
(defn scan-shape
  "One pass over `source`. Returns the shape a rewrite-clj tree would take,
   without building one:

     {:parse-nodes        lexical node estimate
      :parse-depth        maximum nesting depth
      :delimiter-balance  opens minus closes; zero on well-formed source}

   The estimate counts one node per opening delimiter, per reader-macro prefix,
   per token run, per string, regex, and character literal, per line comment,
   and per whitespace run — because rewrite-clj materialises whitespace and
   comments as nodes too.

   `:parse-depth` is the depth the READER recurses to, which is not the same as
   the delimiter depth. A reader-macro prefix (`'` `` ` `` `~` `~@` `@` `^`
   `#'` `#_` `#=` `#?` `#?@`) wraps the form that follows it in a node and
   recurses into it, so a run of N prefixes is N levels; they unwind at the
   next atom, or at the delimiter that closes the form they wrapped. Counting
   only delimiters admitted a 710-byte `@`-tower at depth 1 that then exhausted
   the reader's stack. Delimiters inside strings, regex literals, character
   literals and comments are text, never structure. and so is a `#!` shebang line — the reader takes
   it as a line comment, and this repository has 20 files that start with one.

   It is an ESTIMATE and reads about 11% low against rewrite-clj's own count
   (19,528 against 21,996 for `intent_transaction.clj`), because a whitespace
   run counts once here where rewrite-clj splits whitespace from newlines. The
   ceilings are derived from measurements of this function, so the offset is
   not a correctness question; the margin witnesses catch drift.

   `:delimiter-balance` is diagnostic, never a verdict: a non-zero balance means
   unbalanced source, which the PARSER reports as a syntax error. Admission does
   not refuse it, and this scan NEVER throws on it — an unmatched close is
   recorded as a negative balance and the scan carries on, so the reader's own
   `Unmatched delimiter` error reaches the caller unchanged. It counts
   DELIMITERS only, so prefix accounting can never disturb it."
  [^String source]
  (let [n (.length source)]
    ;; `ddepth` is the SIGNED delimiter balance and is the only thing the
    ;; receipt reports: an unmatched close drives it negative and the scan
    ;; carries on, so the PARSER still reports the syntax error it owns.
    ;; `sdepth` is the structural nesting depth — the same count floored at
    ;; zero — and is the ONLY one that may index `stack` or feed `max-depth`.
    ;; One value cannot be both a signed balance and an array subscript: it was,
    ;; and an ordinary extra `)` indexed the array at -1.
    ;; `pdepth` is the reader-macro levels currently open; `pending` is the
    ;; innermost run of them, still waiting for the form it wraps. `stack` saves
    ;; each open delimiter's `pending` so the matching close unwinds exactly
    ;; those levels.
    (loop [i 0, nodes 0, ddepth 0, sdepth 0, pdepth 0, pending 0, max-depth 0,
           ^ints stack (int-array 64)]
      (if (>= i n)
        {:parse-nodes nodes
         :parse-depth max-depth
         :delimiter-balance ddepth}
        (let [c (int (.charAt source i))]
          (cond
            ;; whitespace and comments do not satisfy a pending prefix
            (ws? c)
            (let [j (long (loop [j i]
                      (if (and (< j n) (ws? (int (.charAt source j))))
                        (recur (inc j))
                        j)))]
              (recur j (inc nodes) ddepth sdepth pdepth pending max-depth stack))

            ;; line comment: to end of line
            (== c SEMICOLON)
            (let [j (long (loop [j i]
                      (if (and (< j n)
                               (not (== NEWLINE (int (.charAt source j)))))
                        (recur (inc j))
                        j)))]
              (recur j (inc nodes) ddepth sdepth pdepth pending max-depth stack))

            ;; `#!` — the reader treats a shebang as a line comment, anywhere
            (and (== c HASH)
                 (< (inc i) n)
                 (== BANG (int (.charAt source (inc i)))))
            (let [j (long (loop [j i]
                      (if (and (< j n)
                               (not (== NEWLINE (int (.charAt source j)))))
                        (recur (inc j))
                        j)))]
              (recur j (inc nodes) ddepth sdepth pdepth pending max-depth stack))

            ;; string literal, backslash escapes — an atom: it satisfies prefixes
            (== c QUOTE)
            (let [j (long (loop [j (inc i)]
                      (if (>= j n)
                        j
                        (let [d (int (.charAt source j))]
                          (cond
                            (== d BACKSLASH) (recur (+ j 2))
                            (== d QUOTE) (inc j)
                            :else (recur (inc j)))))))]
              (recur j (inc nodes) ddepth sdepth (- pdepth pending) 0 max-depth stack))

            ;; character literal: \a \newline \( \\ — consumes the delimiter
            (== c BACKSLASH)
            (let [j (token-end source (+ i 2) n)]
              (recur (max j (min n (+ i 2))) (inc nodes)
                     ddepth sdepth (- pdepth pending) 0 max-depth stack))

            ;; regex literal #"..." — its brackets are not structure
            (and (== c HASH)
                 (< (inc i) n)
                 (== QUOTE (int (.charAt source (inc i)))))
            (let [j (long (loop [j (+ i 2)]
                      (if (>= j n)
                        j
                        (let [d (int (.charAt source j))]
                          (cond
                            (== d BACKSLASH) (recur (+ j 2))
                            (== d QUOTE) (inc j)
                            :else (recur (inc j)))))))]
              (recur j (inc nodes) ddepth sdepth (- pdepth pending) 0 max-depth stack))

            ;; reader-macro prefix — one nesting level, unwound by the form it wraps
            (reader-prefix-start? c)
            (let [len (prefix-length source i n)]
              (if (pos? len)
                (let [pd (inc pdepth)]
                  (recur (+ i len) (inc nodes) ddepth sdepth pd (inc pending)
                         (max max-depth (+ sdepth pd)) stack))
                ;; `#` that begins no prefix (`#(`, `#{`, a tagged literal)
                (let [j (token-end source i n)]
                  (recur (if (> j i) j (inc i)) (inc nodes)
                         ddepth sdepth (- pdepth pending) 0 max-depth stack))))

            (opens? c)
            (let [^ints stack (if (>= sdepth (alength stack))
                                (java.util.Arrays/copyOf stack (* 2 (alength stack)))
                                stack)
                  _ (aset stack sdepth (int pending))
                  sd (inc sdepth)]
              (recur (inc i) (inc nodes) (inc ddepth) sd pdepth 0
                     (max max-depth (+ sd pdepth)) stack))

            (closes? c)
            ;; unwind this level's unsatisfied prefixes, then the ones the
            ;; closing form was itself wrapped in. An UNMATCHED close only drives
            ;; the balance negative: `sdepth` floors at zero, so the stack is
            ;; never indexed out of bounds and the scan never throws.
            (let [outer (if (pos? sdepth) (aget stack (dec sdepth)) 0)]
              (recur (inc i) nodes (dec ddepth) (max 0 (dec sdepth))
                     (max 0 (- pdepth pending outer)) 0 max-depth stack))

            :else
            (let [j (token-end source i n)]
              (recur (if (> j i) j (inc i)) (inc nodes)
                     ddepth sdepth (- pdepth pending) 0 max-depth stack))))))))

;; ============================================================
;; The scan's own cost
;; ============================================================

(def ^:private scan-nanos
  "Nanoseconds spent inside `scan-shape` since the last `reset-scan-clock!`.

   Measured on anvil 2026-09-03 the charge is 1.27% of the outline it precedes
   (0.647 ms scan against 51.032 ms outline on a 126,596 B file), exactly one
   scan per parse entry. That is small — and an unreported cost is one nobody
   notices regressing: the first draft of `scan-shape` was 638x slower and
   every test still passed. So a tree-scale receipt charges it.

   A plain atom rather than a dynamic var on purpose: the scan runs inside
   `pmap` workers, and a counter that depends on binding conveyance to be
   correct is a counter that reads zero on the day the executor changes."
  (atom 0))

(defn reset-scan-clock!
  "Zero the scan clock. A tree-scale caller does this once before its scan."
  []
  (reset! scan-nanos 0))

(defn scan-ms
  "Milliseconds spent in admission scans since the last reset, to 3 decimals."
  []
  (/ (Math/round (* 1000.0 (/ (double @scan-nanos) 1e6))) 1000.0))

;; ============================================================
;; Admission
;; ============================================================

(defn public-ceiling-name
  "The wire name of a ceiling, as it appears in a refusal message and in a
   tree-scale receipt: `max_parse_depth`, `max_parse_nodes`."
  [reason]
  (.replace (name reason) "-" "_"))

(defn- remedy-for
  [reason limit observed]
  (case reason
    :max-parse-depth
    (str "This file nests " observed " levels deep; the server parser ceiling "
         "is " limit ". A structural read of it would either exhaust the "
         "reader's stack or allocate a tree sized by its nesting rather than "
         "its bytes. Read it with a line-ranged text tool, or split the nested "
         "literal into named forms. Every clj-surgeon structural read of this "
         "file builds the same tree, so there is no narrower call to retry.")

    :max-parse-nodes
    (str "This file's lexical node estimate is " observed "; the server parser "
         "ceiling is " limit ". A structural read would allocate a tree sized "
         "by its token density. Read it with a line-ranged text tool, or split "
         "it. Every clj-surgeon structural read of this file builds the same "
         "tree, so there is no narrower call to retry. If the file is simply "
         "LARGE rather than dense, the per-file byte ceiling reports it "
         "first once MCP-OP-MEM-002 lands.")))

;; @spec MCP-OP-MEM-005
(defn stack-overflow-refusal
  "The typed refusal for a file whose parse exhausted the reader's stack DESPITE
   being admitted.

   The lexical estimator will always be an ESTIMATE, so the tree-scale caller
   closes the scan-kill class independently of it: an `Error` escaping one
   file's parse becomes the same named, counted, per-file skip a ceiling refusal
   does, never a dead scan. Measured on anvil 2026-09-03: before this, a single
   overflowing file killed the whole `ls-tree` pmap as
   `{:FATAL \"ExecutionException\"}` and no file's outline was returned.

   It carries no `:limit` or `:observed` because nothing was measured — the
   reader ran out of stack before anything could be. Its presence is also a
   report against the estimator, and the remedy says so."
  [file]
  {:refusal :parser_admission_refused
   :error-type :parser-admission-refused
   :file file
   :reason :stack-overflow-during-parse
   :limit nil
   :observed nil
   :remedy (str "A structural read of this file exhausted the reader's stack, "
                "and the lexical pre-scan admitted it — so it nests deeper than "
                "the estimator can currently see. Read it with a line-ranged "
                "text tool, or split the nested literal into named forms. Every "
                "clj-surgeon structural read of this file builds the same tree, "
                "so there is no narrower call to retry. The estimator missing "
                "this shape is a defect: report it with the file.")})

;; @spec MCP-OP-MEM-005
(defn refusal
  "Nil when `source` is admitted; otherwise a typed refusal map.

   Depth is checked before nodes: exhausting the reader's stack is the sharper
   failure, and it is the one a byte or node ceiling cannot see.

   The refusal carries no `:next_call`. That is deliberate and is a boundary of
   this intent: every clj-surgeon structural read of a refused file constructs
   the same tree, so there is no narrower clj-surgeon call to name. Executable
   narrowing for an oversized SCOPE belongs to MCP-OP-MEM-002."
  ([file source] (refusal file source *ceilings*))
  ([file source ceilings]
   (let [{:keys [max-parse-nodes max-parse-depth]} ceilings
         t0 (System/nanoTime)
         {:keys [parse-nodes parse-depth]} (scan-shape source)
         _ (swap! scan-nanos + (- (System/nanoTime) t0))
         measured {:parse-nodes parse-nodes :parse-depth parse-depth}
         over (cond
                (> parse-depth max-parse-depth)
                [:max-parse-depth max-parse-depth parse-depth]

                (> parse-nodes max-parse-nodes)
                [:max-parse-nodes max-parse-nodes parse-nodes])]
     (when over
       (let [[reason limit observed] over]
         {:refusal :parser_admission_refused
          :error-type :parser-admission-refused
          :file file
          :reason reason
          :limit limit
          :observed observed
          :measured measured
          :remedy (remedy-for reason limit observed)})))))

;; @spec MCP-OP-MEM-005
(defn admit!
  "Return `source` when it is admitted, or throw a typed `ex-info` whose
   `ex-data` IS the refusal map.

   Called from the read path's parse entries, so every operation that builds a
   rewrite-clj tree through them inherits the ceiling. A tree-scale caller
   catches this and records one named, counted skip; it never aborts the scan."
  ([file source] (admit! file source *ceilings*))
  ([file source ceilings]
   (if-let [r (refusal file source ceilings)]
     (throw (ex-info (str "parser admission refused " (public-ceiling-name (:reason r))
                          ": " file " observed " (:observed r)
                          ", limit " (:limit r))
                     r))
     source)))
