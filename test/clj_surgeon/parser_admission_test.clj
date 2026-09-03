(ns clj-surgeon.parser-admission-test
  "Millisecond-scale witnesses for MCP-OP-MEM-005 — bounded lexical/parser
   admission.

   The heavy reproduction of the defect these guard against lives in
   `make memory-red` (subprocess JVMs at explicit -Xmx). Nothing here starts a
   JVM, allocates a large tree, or measures heap: every witness is a pure scan
   of a string, or one small outline.

   Every ceiling is exercised through the CONFIGURED value, not through a
   hard-coded constant, so the witnesses keep meaning if a ceiling is retuned —
   with two deliberate exceptions that assert the shipped defaults against
   real, measured corpora (`default-ceilings-*`), because a ceiling that no
   longer admits this repository's own sources is the failure mode a retune
   causes."
  (:require
   [clj-surgeon.analyze :as analyze]
   [clj-surgeon.outline :as outline]
   [clj-surgeon.parse-admission :as admission]
   [clj-surgeon.show-form :as show-form]
   [clj-surgeon.structural-lens :as lens]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.test :refer [deftest is testing]]
   [rewrite-clj.zip :as z]))

;; ------------------------------------------------------------------
;; fixtures — built, never checked in, so a shape is legible in one line
;; ------------------------------------------------------------------

(defn- tower
  "A source whose single top-level form nests `levels` deep."
  [levels]
  (str "(ns fixture.tower)\n(def t\n  "
       (str/join (repeat levels "["))
       ":leaf"
       (str/join (repeat levels "]"))
       ")\n"))

(defn- dense
  "A source of one vector holding `n` integer tokens. Its lexical node estimate
   is dominated by tokens and the whitespace between them."
  [n]
  (str "(ns fixture.dense)\n(def v [" (str/join " " (range n)) "])\n"))

(defn- prefix-tower
  "A source whose single form carries `n` consecutive reader-macro prefixes.
   `(def x @@@ ... @y)` — no structural delimiter deepens past the `def` list,
   yet the reader recurses once per prefix."
  [prefix n]
  (str "(def x " (str/join (repeat n prefix)) "y)\n"))

(defn- repo-sources
  "Every Clojure-family source under src/, test/ AND bench/, as [path source]
   pairs.

   bench/ is in the corpus because it is the only directory holding the
   construct the scanner could not parse: all 20 of this repository's `#!`
   shebang files live there. A corpus witness that excludes the directory
   containing its own counterexample is not a witness."
  []
  (for [root ["src" "test" "bench"]
        ^java.io.File f (file-seq (io/file root))
        :when (and (.isFile f) (re-find #"\.clj[cs]?$" (.getName f)))]
    [(.getPath f) (slurp f)]))

;; ------------------------------------------------------------------
;; the estimator
;; ------------------------------------------------------------------

(defn- depth-of [source] (:parse-depth (admission/scan-shape source)))
(defn- nodes-of [source] (:parse-nodes (admission/scan-shape source)))

;; @spec MCP-OP-MEM-005
(deftest lexical-scan-respects-strings-regexes-chars-and-comments
  (testing "a bracket inside a string is text, not structure"
    (is (= 1 (depth-of "(def s \"(((((\")"))))
  (testing "a bracket inside a regex literal is text"
    (is (= 1 (depth-of "(def r #\"[[[[\\\"]\")"))))
  (testing "an escaped quote does not end the string"
    (is (= 1 (depth-of "(def s \"a\\\"((((\")"))))
  (testing "a character literal delimiter is not structure"
    (is (= 1 (depth-of "(def c \\()"))))
  (testing "a bracket inside a line comment is not structure"
    (is (= 1 (depth-of "(def x 1) ;; ((((\n"))))
  (testing "real nesting is counted"
    (is (= 3 (depth-of "(a [b {:c 1}])")))
    (is (= 51 (depth-of (tower 50))))))

;; @spec MCP-OP-MEM-005
(deftest lexical-scan-balances-on-every-source-in-this-repository
  (testing "the scan's delimiter balance is zero on real code"
    (let [unbalanced (for [[path source] (repo-sources)
                           :let [b (:delimiter-balance
                                     (admission/scan-shape source))]
                           :when (not (zero? b))]
                       [path b])]
      (is (empty? unbalanced)
          (str "the lexical scan mis-tracked a string, regex, character "
               "literal or comment in: " (pr-str (vec unbalanced)))))))

;; ------------------------------------------------------------------
;; malformed input — the shape a structural editor meets most often
;; ------------------------------------------------------------------

(def ^:private malformed-shapes
  "Unbalanced and truncated sources: the family the well-formed corpus and the
   24-shape lexical attack both miss, which is exactly why a crash on one extra
   `)` shipped past both.

   Measured on anvil 2026-09-03 before this witness: the prefix stack let the
   delimiter counter go negative on an unmatched close and then used it as the
   array subscript for the next open, so `(a)) (b)` threw
   `ArrayIndexOutOfBoundsException: Index -1 out of bounds for length 64` out of
   `scan-shape` — and out of `outline`, `run-outline`, `run-deps` and
   `analyze/file->zloc` UNHANDLED, replacing the reader's own
   `Unmatched delimiter: ) [at line 1, column 21]`.

   A syntax error is the single most common defect a structural editing tool
   meets. The scan's contract on all of it is: never throw, record the balance,
   admit, and let the PARSER report the error it owns."
  {"unmatched-open"        "(defn f [x] (inc x)\n"
   "unmatched-open-vec"    "(def v [1 2 3)\n"
   "unmatched-close"       "(defn f [x] (inc x)))\n(defn g [y] 1)\n"
   "unmatched-close-bare"  ")("
   "unmatched-close-many"  ")))((("
   "unmatched-close-brack" "] ["
   "unmatched-close-brace" "} {a 1}"
   "close-at-eof"          "(def x 1)\n)\n"
   "close-only"            ")"
   "close-in-prefix-run"   "(def x '''))\n"
   "close-in-prefix-run-2" "@@@)"
   "prefix-run-at-eof"     "(def x '''"
   "discard-at-eof"        "(def x 1) #_"
   "discard-alone"         "#_"
   "unterminated-string"   "(def s \"abc ((( \n(def y 2)\n"
   "unterminated-regex"    "(def r #\"[a-z \n(def y 2)\n"
   "unterminated-comment"  "(def x 1)\n#|"
   "unterminated-char"     "(def c \\"
   "meta-at-eof"           "(def x ^:a"
   "mixed-garbage"         "(a)) [b} {c] '''@@ #_ \"unterminated\n"})

(defn- fixture-sources
  "Every checked-in fixture under `test-fixtures/`, as [path source] pairs."
  []
  (for [^java.io.File f (file-seq (io/file "test-fixtures"))
        :when (.isFile f)]
    [(.getPath f) (slurp f)]))

;; @spec MCP-OP-MEM-005
(deftest malformed-source-never-crashes-the-scan
  (testing "one extra `)` returns the reader's error, not an internal one"
    (let [f (doto (java.io.File/createTempFile "mem005unbalanced" ".clj")
              .deleteOnExit)]
      (spit f "(defn f [x] (inc x)))\n(defn g [y] 1)\n")
      (is (= {:parse-depth 2 :delimiter-balance -1}
             (select-keys (admission/scan-shape (slurp f))
                          [:parse-depth :delimiter-balance]))
          "an unmatched close is a NEGATIVE balance and the scan continues")
      (is (nil? (admission/refusal (.getPath f) (slurp f)
                                   admission/default-ceilings))
          "admission does not refuse unbalanced source — the parser owns it")
      (is (= "Unmatched delimiter: ) [at line 1, column 21]"
             (try (outline/outline (.getPath f)) ::no-throw
                  (catch clojure.lang.ExceptionInfo e (.getMessage e))
                  (catch Throwable t (str (.getName (class t)) ": "
                                          (.getMessage t)))))
          "the reader's own error, byte-identical to the pre-branch path")))

  (testing "the scan never throws on any malformed shape, or any fixture"
    (let [corpus (concat (for [[nm src] malformed-shapes] [(str "generated:" nm) src])
                         (fixture-sources))
          crashed (for [[path source] corpus
                        :let [r (try (admission/scan-shape source)
                                     (catch Throwable t
                                       (str (.getName (class t)) ": "
                                            (.getMessage t))))]
                        :when (string? r)]
                    [path r])]
      (is (seq corpus) "the corpus must not be empty")
      (is (empty? crashed)
          (str "scan-shape threw on: " (pr-str (vec crashed))))))

  (testing "admission ADMITS every one, so the answer is the reader's alone"
    ;; Admitted + unchanged reader = behaviour identical to the pre-branch path,
    ;; by construction. Confirmed independently by a differential run of
    ;; `outline/outline` over these 20 generated shapes and all 41 checked-in
    ;; fixtures against `origin/main` (6c07015) on anvil 2026-09-03: 62/62
    ;; results identical after the fix, 6/62 differing before it (five unmatched
    ;; closes and one mixed-garbage file, each an `Index -1` where main returned
    ;; `Unmatched delimiter`).
    (let [refused (for [[nm src] malformed-shapes
                        :let [r (admission/refusal (str nm ".clj") src
                                                   admission/default-ceilings)]
                        :when r]
                    [nm (:reason r)])]
      (is (empty? refused)
          (str "malformed source must reach the parser, not a refusal: "
               (pr-str (vec refused)))))))

;; ------------------------------------------------------------------
;; admission AT the ceiling
;; ------------------------------------------------------------------

;; @spec MCP-OP-MEM-005
(deftest a-source-exactly-at-the-depth-ceiling-projects-identically
  (testing "D admits and outlines identically; D+1 refuses"
    (let [d 40
          at (tower d)
          over (tower (inc d))
          ceilings {:max-parse-nodes 1000000 :max-parse-depth (depth-of at)}]
      (is (= (depth-of at) (inc d)) "the fixture's depth is the configured limit")
      (is (nil? (admission/refusal "at.clj" at ceilings)))
      (binding [admission/*ceilings* ceilings]
        (is (= (pr-str (outline/outline-source "at.clj" at))
               (pr-str (binding [admission/*ceilings*
                                 {:max-parse-nodes Long/MAX_VALUE
                                  :max-parse-depth Long/MAX_VALUE}]
                         (outline/outline-source "at.clj" at))))
            "a source exactly at the ceiling projects byte-identically"))
      (let [r (admission/refusal "over.clj" over ceilings)]
        (is (some? r) "one level past the ceiling refuses")
        (is (= :max-parse-depth (:reason r)))
        (is (= (:max-parse-depth ceilings) (:limit r)))
        (is (= (depth-of over) (:observed r)))))))

;; @spec MCP-OP-MEM-005
(deftest a-source-exactly-at-the-node-ceiling-projects-identically
  (testing "N admits and outlines identically; N+1 refuses"
    (let [at (dense 200)
          over (dense 201)
          n (nodes-of at)
          ceilings {:max-parse-nodes n :max-parse-depth 1000}]
      (is (< n (nodes-of over)) "the +1 fixture really has more nodes")
      (is (nil? (admission/refusal "at.clj" at ceilings)))
      (binding [admission/*ceilings* ceilings]
        (is (= (pr-str (outline/outline-source "at.clj" at))
               (pr-str (binding [admission/*ceilings*
                                 {:max-parse-nodes Long/MAX_VALUE
                                  :max-parse-depth Long/MAX_VALUE}]
                         (outline/outline-source "at.clj" at))))
            "a source exactly at the ceiling projects byte-identically"))
      (let [r (admission/refusal "over.clj" over ceilings)]
        (is (some? r))
        (is (= :max-parse-nodes (:reason r)))
        (is (= n (:limit r)))
        (is (= (nodes-of over) (:observed r)))))))

;; @spec MCP-OP-MEM-005
(deftest a-refusal-reaches-no-tree-constructor
  (testing "zero calls into the rewrite-clj parse entry on refusal"
    (let [over (tower 60)
          ceilings {:max-parse-nodes 1000000 :max-parse-depth 20}
          calls (atom 0)
          real z/of-string]
      (with-redefs [z/of-string (fn [& args] (swap! calls inc) (apply real args))]
        (binding [admission/*ceilings* ceilings]
          (is (thrown? clojure.lang.ExceptionInfo
                       (outline/outline-source "over.clj" over)))))
      (is (zero? @calls)
          (str "the tree constructor was invoked " @calls
               " time(s) for a refused input")))))

;; @spec MCP-OP-MEM-005
(deftest a-refusal-names-its-limit-observed-and-remedy
  (testing "the refusal is typed and self-describing"
    (let [over (tower 60)
          ceilings {:max-parse-nodes 1000000 :max-parse-depth 20}
          r (admission/refusal "over.clj" over ceilings)]
      (is (= :parser_admission_refused (:refusal r)))
      (is (= "over.clj" (:file r)))
      (is (= :max-parse-depth (:reason r)))
      (is (= 20 (:limit r)))
      (is (= 61 (:observed r)))
      (is (string? (:remedy r)))
      (is (seq (:remedy r)))
      (is (= {:parse-nodes (nodes-of over) :parse-depth (depth-of over)}
             (:measured r))
          "both measured shape figures travel with the refusal")
      (is (not (contains? r :next_call))
          (str "no narrower clj-surgeon call exists for a refused file — "
               "every structural read of it builds the same tree")))))

;; @spec MCP-OP-MEM-005
(deftest admit-throws-a-typed-exception-carrying-the-refusal
  (let [over (tower 60)]
    (binding [admission/*ceilings* {:max-parse-nodes 1000000
                                    :max-parse-depth 20}]
      (let [e (try (admission/admit! "over.clj" over) nil
                   (catch clojure.lang.ExceptionInfo e e))]
        (is (some? e))
        (is (= :parser_admission_refused (:refusal (ex-data e))))
        (is (str/includes? (ex-message e) "max_parse_depth"))))))

;; @spec MCP-OP-MEM-005
(deftest a-shebang-line-is-a-comment-not-structure
  (testing "the balance-zero invariant survives #!"
    ;; The only overcount found in a 24-fixture lexical attack: `#!` is a line
    ;; comment to Clojure's reader, and scoring its delimiters as structure
    ;; broke the very invariant the spec calls proof of comment handling.
    (let [src "#!/usr/bin/env foo ((((\n(def x 1)\n"]
      (is (zero? (:delimiter-balance (admission/scan-shape src)))
          "a shebang line's brackets are text, not structure")
      (is (= 1 (depth-of src))
          "only the (def x 1) list is real nesting"))))

;; ------------------------------------------------------------------
;; nesting is EVERY construct that makes the reader recurse
;; ------------------------------------------------------------------

;; @spec MCP-OP-MEM-005
(deftest reader-macro-prefixes-count-as-nesting
  (testing "a run of N prefixes is N nesting levels, per prefix family"
    ;; Every one of these makes the rewrite-clj reader allocate a wrapping node
    ;; and recurse into its child. A delimiter-only estimate scores them ZERO.
    (doseq [p ["'" "`" "~" "~@" "@" "^" "#'" "#_" "#=" "#?" "#?@"]]
      (let [src (prefix-tower p 40)]
        (is (<= 40 (depth-of src))
            (str "prefix " (pr-str p) " contributed "
                 (- (depth-of src) 1) " of 40 nesting levels")))))
  (testing "prefixes unwind at the next atom"
    (is (= 1 (depth-of "(a) (b)")) "the delimiter-only baseline")
    (is (= 2 (depth-of "(def a 'x 'y 'z)"))
        "three SEPARATE one-prefix forms are ONE prefix level, not three")
    (is (= 3 (depth-of "(def a '(x))"))
        "a quoted list is the quote's level plus the list's"))
  (testing "prefixes unwind at a closing delimiter"
    (is (= (depth-of "'(a)") (depth-of "'(a) '(b) '(c)"))
        "a quote consumed by its list does not leak past the close")
    (is (= 2 (depth-of "'(a) (b)")))
    (is (zero? (:delimiter-balance (admission/scan-shape "'(a) `[b] ~@{c 1}")))
        "prefix accounting never disturbs the delimiter balance")))

;; @spec MCP-OP-MEM-005
(deftest a-prefix-tower-is-refused-before-it-overflows-the-reader
  (testing "the 710-byte @-tower that killed the whole ls-tree scan"
    ;; Measured on anvil 2026-09-03 with the delimiter-only estimator: this file
    ;; scanned at parse-depth 1, was ADMITTED, and threw StackOverflowError out
    ;; of the reader — an Error, so `core/safe-outline` did not catch it and the
    ;; scan died. 155x smaller than the 111 KB file that motivated the control.
    (let [src (prefix-tower "@" 700)
          calls (atom 0)
          real z/of-string]
      (is (= 710 (count src)))
      (is (< (:max-parse-depth admission/default-ceilings) (depth-of src))
          (str "the tower scans at depth " (depth-of src)
               ", at or under the shipped ceiling "
               (:max-parse-depth admission/default-ceilings)))
      ;; Cost is asserted RELATIVE to one bare scan of the same bytes, not
      ;; against a wall-clock constant. This suite runs on a shared 16-core box
      ;; whose load routinely passes 10, and an absolute millisecond bound there
      ;; measures the neighbours, not the code. The ratio is load-independent
      ;; and catches what actually matters: a second scan, or a parse, creeping
      ;; into the refusal path. Absolute cost is metered in production by
      ;; `scan_ms` in the ls-tree receipt.
      (admission/scan-shape src)                          ; warm
      (let [t0 (System/nanoTime)
            _ (admission/scan-shape src)
            one-scan-ns (max 1 (- (System/nanoTime) t0))
            t1 (System/nanoTime)
            r (with-redefs [z/of-string (fn [& args] (swap! calls inc) (apply real args))]
                (admission/refusal "tower.clj" src admission/default-ceilings))
            refusal-ns (- (System/nanoTime) t1)]
        (is (= :max-parse-depth (:reason r)))
        (is (zero? @calls) "no tree constructor was invoked")
        (is (< refusal-ns (* 20 one-scan-ns))
            (str "the refusal cost " (/ refusal-ns 1e6) " ms against "
                 (/ one-scan-ns 1e6) " ms for one bare scan of the same bytes — "
                 "it should be ONE scan and no parse"))))))

;; ------------------------------------------------------------------
;; every read-path tree constructor, not only the outline's two
;; ------------------------------------------------------------------

(defn- tower-file!
  "The 710-byte prefix tower, on disk."
  []
  (let [f (doto (java.io.File/createTempFile "mem005tower" ".clj") .deleteOnExit)]
    (spit f (prefix-tower "@" 700))
    (.getPath f)))

;; @spec MCP-OP-MEM-005
(deftest the-analyze-constructor-is-gated
  (testing "analyze does not COMPLETE on this shape — it overflows the reader"
    ;; The receipt's section 5 left `analyze` ungated on the stated ground that
    ;; gating it "would convert an operation that completes into one that
    ;; throws". Measured false: it throws either way. Gating swaps an
    ;; uncatchable Error for a typed ExceptionInfo, which is strictly better for
    ;; every caller.
    (let [src (prefix-tower "@" 700)
          path (tower-file!)]
      (doseq [[label thunk] [["string->zloc" #(analyze/string->zloc src)]
                             ["file->zloc"   #(analyze/file->zloc path)]]]
        (let [e (try (thunk) nil (catch clojure.lang.ExceptionInfo e e))]
          (is (some? e) (str label " built a tree instead of refusing"))
          (is (= :parser_admission_refused (:refusal (ex-data e))) label)
          (is (= :max-parse-depth (:reason (ex-data e))) label)))))
  (testing "the CLI ops over analyze return a NAMED plan refusal"
    (let [path (tower-file!)]
      (doseq [op ['clj-surgeon.core/run-topo
                  'clj-surgeon.core/run-deps
                  'clj-surgeon.core/run-ls-deps
                  'clj-surgeon.core/run-closure
                  'clj-surgeon.core/run-declares]]
        (let [r ((requiring-resolve op) {:file path :form "x"})]
          (is (= :parser_admission_refused (:refusal r)) (str op))
          (is (= :max-parse-depth (:reason r)) (str op))
          (is (seq (:remedy r)) (str op)))))))

;; @spec MCP-OP-MEM-005
(deftest the-structural-lens-constructor-is-gated
  (testing "find_subforms is on the MCP read surface and was ungated"
    ;; Reached from mcp_inspect's match_result and the CLI :find-subform op.
    (let [src (prefix-tower "@" 700)
          calls (atom 0)
          real z/of-string
          r (with-redefs [z/of-string (fn [& args] (swap! calls inc) (apply real args))]
              (lens/find-subforms src {:match 'x}))]
      (is (= :parser_admission_refused (:refusal r))
          "the refusal reaches the caller TYPED, not flattened to a string")
      (is (= :max-parse-depth (:reason r)))
      (is (some? (:limit r)))
      (is (some? (:observed r)))
      (is (seq (:remedy r)))
      (is (zero? @calls) "no tree constructor was invoked")))
  (testing "find_file names the file it refused"
    (let [path (tower-file!)
          r (lens/find-file {:file path :match 'x})]
      (is (= :parser_admission_refused (:refusal r)))
      (is (= path (:file r))))))

;; @spec MCP-OP-MEM-005
(deftest show-form-carries-the-typed-refusal
  (testing "the refusal witness family holds on the show_form entrance too"
    (let [path (tower-file!)
          r (show-form/show-file {:file path :form "x"})]
      (is (= :parser_admission_refused (:refusal r))
          "show_form flattened the refusal to a bare :error string")
      (is (= :parser-admission-refused (:error-type r)))
      (is (= :max-parse-depth (:reason r)))
      (is (some? (:limit r)))
      (is (some? (:observed r)))
      (is (seq (:remedy r))))))

;; ------------------------------------------------------------------
;; the shipped defaults, against real corpora
;; ------------------------------------------------------------------

;; @spec MCP-OP-MEM-005
(deftest default-ceilings-admit-every-source-in-this-repository
  (testing "the shipped ceilings never refuse this repository's own code"
    (let [{:keys [max-parse-nodes max-parse-depth]} admission/default-ceilings
          shapes (for [[path source] (repo-sources)]
                   (assoc (admission/scan-shape source) :path path))
          worst-nodes (apply max-key :parse-nodes shapes)
          worst-depth (apply max-key :parse-depth shapes)
          refused (remove #(nil? (admission/refusal (:path %)
                                                    (slurp (:path %))
                                                    admission/default-ceilings))
                          shapes)]
      (is (empty? (map :path refused))
          (str "the shipped ceilings refuse this repository's own sources: "
               (pr-str (mapv :path refused))))
      (is (<= (* 4 (:parse-nodes worst-nodes)) max-parse-nodes)
          (str "node margin below 4x: largest is " (:path worst-nodes)
               " at " (:parse-nodes worst-nodes) " nodes against a "
               max-parse-nodes " ceiling"))
      (is (<= (* 4 (:parse-depth worst-depth)) max-parse-depth)
          (str "depth margin below 4x: deepest is " (:path worst-depth)
               " at " (:parse-depth worst-depth) " levels against a "
               max-parse-depth " ceiling")))))

;; @spec MCP-OP-MEM-005
(deftest default-ceilings-refuse-the-adversarial-shapes
  (testing "the battery's nested arm refuses on DEPTH, not on node count"
    ;; 300 `{:k [` levels = 601 delimiters; 41,252 nodes over 111,183 bytes.
    (let [nested (str "(ns fixture.nested)\n(def tower\n  "
                      (str/join (repeat 300 "{:k ["))
                      ":leaf"
                      (str/join (repeat 300 "]}"))
                      ")\n(def dense [" (str/join " " (range 20000)) "])\n")
          r (admission/refusal "nested.clj" nested admission/default-ceilings)]
      (is (some? r))
      (is (= :max-parse-depth (:reason r))
          (str "a 111 KB file must be refused for its SHAPE, not its size; "
               "reason was " (:reason r)))
      (is (< (:parse-nodes (admission/scan-shape nested))
             (:max-parse-nodes admission/default-ceilings))
          "the node ceiling admits it — depth is the control that fires")))
  (testing "a token-dense file above the node ceiling refuses on nodes"
    (let [n (:max-parse-nodes admission/default-ceilings)
          ;; each integer contributes a token node and a whitespace node
          src (dense (inc (quot n 2)))
          r (admission/refusal "dense.clj" src admission/default-ceilings)]
      (is (some? r))
      (is (= :max-parse-nodes (:reason r))))))

;; ------------------------------------------------------------------
;; the tree-scale receipt: a refusal is a SKIP, never an aborted scan
;; ------------------------------------------------------------------

(defn- scratch-tree!
  "Write a throwaway project holding one ordinary file and one refused file."
  []
  (let [dir (java.nio.file.Files/createTempDirectory
              "mem005" (into-array java.nio.file.attribute.FileAttribute []))
        root (.toFile dir)
        src (io/file root "src" "fixture")]
    (.mkdirs src)
    (spit (io/file root "deps.edn") "{:paths [\"src\"]}\n")
    (spit (io/file src "ok.clj")
          "(ns fixture.ok)\n(defn hello [x] (inc x))\n")
    (spit (io/file src "deep.clj") (tower 400))
    (.getPath root)))

;; @spec MCP-OP-MEM-005
(deftest ls-tree-completes-and-counts-a-refused-file
  (testing "one adversarial file is a named, counted skip, not a dead scan"
    (let [root (scratch-tree!)
          projects ((requiring-resolve 'clj-surgeon.core/outline-all-files)
                    ((requiring-resolve 'clj-surgeon.core/discover-projects) root))
          text ((requiring-resolve 'clj-surgeon.core/format-ls-tree-text)
                projects root)
          edn ((requiring-resolve 'clj-surgeon.core/format-ls-tree-edn)
               projects root)
          receipt (:receipt (last edn))
          refused (:parser_admission_refused receipt)]
      (is (str/includes? text "hello")
          "the admitted file's outline is still produced")
      (is (str/includes? text "parser_admission_refused")
          "the text receipt names the refusal")
      (is (= 1 (:count refused)))
      (is (= "src/fixture/deep.clj" (:file (first (:files refused)))))
      (is (= :max-parse-depth (:reason (first (:files refused)))))
      (is (= 401 (:observed (first (:files refused))))))))

(defn- scratch-ordinary-tree!
  "A throwaway project of two ORDINARY files. Nothing here is refusable; the
   witness forces the reader to blow up instead."
  []
  (let [dir (java.nio.file.Files/createTempDirectory
              "mem005soe" (into-array java.nio.file.attribute.FileAttribute []))
        root (.toFile dir)
        src (io/file root "src" "fixture")]
    (.mkdirs src)
    (spit (io/file root "deps.edn") "{:paths [\"src\"]}\n")
    (spit (io/file src "ok.clj") "(ns fixture.ok)\n(defn hello [x] (inc x))\n")
    (spit (io/file src "overflow.clj") "(ns fixture.overflow)\n(def a 1)\n")
    (.getPath root)))

(defn- overflow-the-readers-stack!
  "Exhaust the JVM stack the way the rewrite-clj reader does on a deep input —
   by really recursing. A constructed `StackOverflowError` is not equivalent: it
   is reflective, and babashka's native image refuses it."
  [n]
  (+ 1 (overflow-the-readers-stack! (inc n))))

;; @spec MCP-OP-MEM-005
(deftest ls-tree-survives-a-stack-overflow-with-the-estimator-blind
  (testing "the scan-kill class is closed independent of estimator completeness"
    ;; `overflow.clj` is an ORDINARY two-line file: admission looks at it and
    ;; admits it, correctly. The reader then overflows anyway. That is the
    ;; residual this witness pins — the estimator will always be an estimate,
    ;; and the scan must not depend on it being complete. StackOverflowError is
    ;; an Error, so the pre-existing `catch Exception` never saw it and the
    ;; whole pmap scan died as {:FATAL "ExecutionException"}.
    (let [root (scratch-ordinary-tree!)
          real outline/outline
          projects (with-redefs
                     [outline/outline
                      (fn [f]
                        (if (str/includes? (str f) "overflow.clj")
                          (overflow-the-readers-stack! 0)
                          (real f)))]
                     ((requiring-resolve 'clj-surgeon.core/outline-all-files)
                      ((requiring-resolve 'clj-surgeon.core/discover-projects) root)))
          text ((requiring-resolve 'clj-surgeon.core/format-ls-tree-text)
                projects root)
          edn ((requiring-resolve 'clj-surgeon.core/format-ls-tree-edn)
               projects root)
          refused (:parser_admission_refused (:receipt (last edn)))]
      (is (str/includes? text "hello")
          "the scan COMPLETED: the other file's outline is still produced")
      (is (= 1 (:count refused))
          "the overflow is ONE named, counted skip")
      (is (= "src/fixture/overflow.clj" (:file (first (:files refused)))))
      (is (= :stack-overflow-during-parse (:reason (first (:files refused)))))
      (is (seq (:remedy (first (:files refused))))
          "a skip a caller cannot act on is a skip nobody reads")
      (is (str/includes? text "parser_admission_refused")
          "the text receipt names it too"))))

;; @spec MCP-OP-MEM-005
(deftest the-scan-charges-itself-in-the-production-receipt
  (testing "an unreported cost is one nobody notices regressing"
    ;; `rg scan-ms src/` returned nothing before this: the charge was measured
    ;; only in bench/. The first draft of `scan-shape` was 638x slower and every
    ;; test passed, which is exactly the regression a receipt figure catches.
    (let [root (scratch-tree!)
          projects ((requiring-resolve 'clj-surgeon.core/outline-all-files)
                    ((requiring-resolve 'clj-surgeon.core/discover-projects) root))
          edn ((requiring-resolve 'clj-surgeon.core/format-ls-tree-edn)
               projects root)
          text ((requiring-resolve 'clj-surgeon.core/format-ls-tree-text)
                projects root)
          ms (get-in (last edn) [:receipt :resources :scan_ms])]
      (is (number? ms) "the EDN receipt carries no :resources block")
      (is (pos? ms) "the scan really ran and the clock really measured it")
      (is (str/includes? text "scan_ms") "the text receipt charges it too"))))

;; @spec MCP-OP-MEM-005
(deftest ls-tree-output-is-unchanged-when-nothing-is-refused
  (testing "a scan with no refusal carries no receipt entry at all"
    (let [dir (java.nio.file.Files/createTempDirectory
                "mem005ok" (into-array java.nio.file.attribute.FileAttribute []))
          root (.toFile dir)
          src (io/file root "src" "fixture")]
      (.mkdirs src)
      (spit (io/file root "deps.edn") "{:paths [\"src\"]}\n")
      (spit (io/file src "ok.clj") "(ns fixture.ok)\n(defn hello [x] (inc x))\n")
      (let [projects ((requiring-resolve 'clj-surgeon.core/outline-all-files)
                      ((requiring-resolve 'clj-surgeon.core/discover-projects)
                       (.getPath root)))
            edn ((requiring-resolve 'clj-surgeon.core/format-ls-tree-edn)
                 projects (.getPath root))]
        (is (= 1 (count edn)) "no trailing receipt map is appended")
        (is (nil? (:receipt (last edn))))
        (is (= 'fixture.ok (:ns (first edn))))))))
