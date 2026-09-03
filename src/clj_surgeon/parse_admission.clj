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
   estimates the tree's node count and measures its maximum nesting depth. Over
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

;; @spec MCP-OP-MEM-005
(defn scan-shape
  "One pass over `source`. Returns the shape a rewrite-clj tree would take,
   without building one:

     {:parse-nodes        lexical node estimate
      :parse-depth        maximum nesting depth of structural delimiters
      :delimiter-balance  opens minus closes; zero on well-formed source}

   The estimate counts one node per opening delimiter, per token run, per
   string, regex, and character literal, per line comment, and per whitespace
   run — because rewrite-clj materialises whitespace and comments as nodes too.
   Delimiters inside strings, regex literals, character literals and comments
   are text, never structure.

   It is an ESTIMATE and reads about 11% low against rewrite-clj's own count
   (19,528 against 21,996 for `intent_transaction.clj`), because a whitespace
   run counts once here where rewrite-clj splits whitespace from newlines. The
   ceilings are derived from measurements of this function, so the offset is
   not a correctness question; the margin witnesses catch drift.

   `:delimiter-balance` is diagnostic, never a verdict: a non-zero balance means
   unbalanced source, which the PARSER reports as a syntax error. Admission does
   not refuse it."
  [^String source]
  (let [n (.length source)]
    (loop [i 0, nodes 0, depth 0, max-depth 0]
      (if (>= i n)
        {:parse-nodes nodes
         :parse-depth max-depth
         :delimiter-balance depth}
        (let [c (int (.charAt source i))]
          (cond
            (ws? c)
            (let [j (long (loop [j i]
                      (if (and (< j n) (ws? (int (.charAt source j))))
                        (recur (inc j))
                        j)))]
              (recur j (inc nodes) depth max-depth))

            ;; line comment: to end of line
            (== c SEMICOLON)
            (let [j (long (loop [j i]
                      (if (and (< j n)
                               (not (== NEWLINE (int (.charAt source j)))))
                        (recur (inc j))
                        j)))]
              (recur j (inc nodes) depth max-depth))

            ;; string literal, backslash escapes
            (== c QUOTE)
            (let [j (long (loop [j (inc i)]
                      (if (>= j n)
                        j
                        (let [d (int (.charAt source j))]
                          (cond
                            (== d BACKSLASH) (recur (+ j 2))
                            (== d QUOTE) (inc j)
                            :else (recur (inc j)))))))]
              (recur j (inc nodes) depth max-depth))

            ;; character literal: \a \newline \( \\ — consumes the delimiter
            (== c BACKSLASH)
            (let [j (long (loop [j (+ i 2)]
                      (if (and (< j n) (token-char? (int (.charAt source j))))
                        (recur (inc j))
                        j)))]
              (recur (max j (min n (+ i 2))) (inc nodes) depth max-depth))

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
              (recur j (inc nodes) depth max-depth))

            (opens? c)
            (let [d (inc depth)]
              (recur (inc i) (inc nodes) d (max max-depth d)))

            (closes? c)
            (recur (inc i) nodes (dec depth) max-depth)

            :else
            (let [j (long (loop [j i]
                      (if (and (< j n) (token-char? (int (.charAt source j))))
                        (recur (inc j))
                        j)))]
              (recur (if (> j i) j (inc i)) (inc nodes) depth max-depth))))))))

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
         {:keys [parse-nodes parse-depth]} (scan-shape source)
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
