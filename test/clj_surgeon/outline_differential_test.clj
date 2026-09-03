(ns clj-surgeon.outline-differential-test
  "Acceptance artifact for MCP-OP-MEM-015: the single-parse outline is
   byte-identical to the two-parse outline it replaced.

   `legacy-outline-source` below is an independent reconstruction of the path
   as it stood at commit a845215, written only from public functions:
   `top-level-form-records` (which still parses on its own and still returns
   `:source`), a second `z/of-string` for the `ns` lookup, and
   `extract-ns-requires`. It is deliberately NOT a call into the new code, so
   the comparison cannot go tautological.

   Both sides are compared as `pr-str`, which captures small-map insertion
   order as well as values."
  (:require
   [clj-surgeon.outline :as outline]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.test :refer [deftest is testing]]
   [rewrite-clj.zip :as z]))

(defn- legacy-outline-source
  "The two-parse outline path, reconstructed from public functions."
  [file source project-aliases]
  (let [records (outline/top-level-form-records file source project-aliases)
        zloc (z/of-string source {:track-position? true})
        ns-zloc (some-> zloc
                        (z/find-value z/next 'ns)
                        z/up)
        ns-name (some-> ns-zloc z/down z/right z/string symbol)
        requires (outline/extract-ns-requires ns-zloc)]
    {:ns ns-name
     :file file
     :lines (count (str/split-lines source))
     :form-count (count (filter :name records))
     :forms (->> records
                 (remove #(= 'ns (:type %)))
                 (mapv #(dissoc % :source)))
     :requires (or requires [])
     :forward-refs []}))

(defn- source-files
  [roots]
  (->> roots
       (mapcat (fn [root]
                 (let [base (io/file root)]
                   (when (.exists base) (file-seq base)))))
       (filter #(.isFile ^java.io.File %))
       (filter (fn [^java.io.File f]
                 (let [n (.getName f)]
                   (some #(str/ends-with? n %) [".clj" ".cljc" ".cljs"]))))
       (map #(.getPath ^java.io.File %))
       sort))

;; @spec MCP-OP-MEM-015
(deftest single-parse-outline-is-byte-identical-over-the-repository
  (testing "every source file under src/ and test/ outlines identically"
    (let [paths (source-files ["src" "test"])
          mismatches (reduce
                       (fn [acc path]
                         (let [source (slurp path)
                               expected (pr-str
                                          (legacy-outline-source path source {}))
                               actual (pr-str
                                        (outline/outline-source path source {}))]
                           (if (= expected actual)
                             acc
                             (conj acc path))))
                       []
                       paths)]
      (is (<= 100 (count paths))
          "the differential must cover the whole tree, not a stub")
      (is (= [] mismatches)
          (str (count mismatches) " of " (count paths)
               " files outlined differently: "
               (str/join ", " (take 5 mismatches)))))))

;; @spec MCP-OP-MEM-015
(deftest single-parse-outline-is-byte-identical-on-boundary-shapes
  (testing "shapes the repository tree does not contain"
    (doseq [[label file source]
            [["no ns form" "bare.clj" "(defn a [] 1)\n(def b 2)\n"]
             ["empty source" "empty.clj" ""]
             ["whitespace only" "blank.clj" "\n  \n"]
             ["comments only" "comment.clj" ";; just a comment\n"]
             ["reader conditionals"
              "rc.cljc"
              (str "(ns rc.core (:require #?(:clj [clojure.string :as s])\n"
                   "                       #?@(:cljs [[goog.string :as gs]])))\n"
                   "#?(:clj (defn only-clj [x] x))\n"
                   "#?@(:cljs [(defn only-cljs [y] y) (def z 1)])\n"
                   "(defn shared [a] a)\n")]
             ["ns symbol used as a local"
              "local.clj"
              "(defn f [ns] ns)\n(def g 1)\n"]
             ["attached comments"
              "cmt.clj"
              ";; leading\n;; comment\n(defn c [] 1)\n"]]]
      (is (= (pr-str (legacy-outline-source file source {}))
             (pr-str (outline/outline-source file source {})))
          (str "outline differs for " label)))))

;; @spec MCP-OP-MEM-015
(deftest string-symbol-outlines-still-see-form-source
  (testing "include-string-symbols keeps :source available to the scanner"
    (let [source (str "(ns demo.core)\n"
                      "(def js-blob \"function alpha() {}\\nvar beta = 1;\")\n")
          result (outline/outline-source "demo.clj" source {}
                                         {:include-string-symbols true})
          form (first (:forms result))]
      (is (contains? form :string-symbols))
      (is (= #{"alpha" "beta"}
             (set (map :name (:string-symbols form))))))))

;; @spec MCP-OP-MEM-015
(deftest structural-readers-still-receive-exact-source
  (testing "top-level-form-records keeps :source by default and drops it on request"
    (let [source "(ns demo.core)\n(defn keep-me [x] x)\n"
          with-source (outline/top-level-form-records "demo.clj" source {})
          without (outline/top-level-form-records "demo.clj" source {}
                                                  {:include-source? false})]
      (is (= ["(ns demo.core)" "(defn keep-me [x] x)"]
             (mapv :source with-source)))
      (is (every? #(not (contains? % :source)) without))
      (is (= (mapv #(dissoc % :source) with-source) without)))))
