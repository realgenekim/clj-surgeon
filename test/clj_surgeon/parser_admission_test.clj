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
   [clj-surgeon.outline :as outline]
   [clj-surgeon.parse-admission :as admission]
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
  "Every Clojure-family source under src/ and test/, as [path source] pairs."
  []
  (for [root ["src" "test"]
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
    (is (= 1 (depth-of "(def a 'x 'y 'z)"))
        "three SEPARATE one-prefix forms are one level, not three")
    (is (= 2 (depth-of "(def a '(x))"))
        "a quoted list is the quote's level plus the list's"))
  (testing "prefixes unwind at a closing delimiter"
    (is (= (depth-of "(a) (b)") (depth-of "'(a) (b)") )
        "a quote consumed by its list does not leak past the close")
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
      (let [t0 (System/nanoTime)
            r (with-redefs [z/of-string (fn [& args] (swap! calls inc) (apply real args))]
                (admission/refusal "tower.clj" src admission/default-ceilings))
            ms (/ (- (System/nanoTime) t0) 1e6)]
        (is (= :max-parse-depth (:reason r)))
        (is (zero? @calls) "no tree constructor was invoked")
        (is (< ms 50.0) (str "refusal took " ms " ms"))))))

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
