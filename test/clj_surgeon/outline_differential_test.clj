(ns clj-surgeon.outline-differential-test
  "Acceptance artifact for MCP-OP-MEM-015: the single-parse outline is
   byte-identical to the two-parse outline it replaced.

   `legacy-outline-source` and `legacy-top-level-form-records` below are
   copied verbatim (functions renamed with a `legacy-` prefix only; bodies
   unchanged) from `src/clj_surgeon/outline.clj` at commit 9f48694 on
   `origin/main` — the tip of that file's history before MEM-015 (found via
   `git log --oneline origin/main -- src/clj_surgeon/outline.clj | head -1`),
   RE-FROZEN on the 2026-09-03 integration branch against a28690e, which landed
   the `defmethod` dispatch extraction on main after 9f48694 was taken. The
   freeze must mirror the two-parse builder THIS TREE replaced, not the one an
   older base had, or the differential reports a difference that is really the
   frozen side being stale.
   That is the two-parse `top-level-form-records`: it parses on its own via
   `cwalk/top-level-forms` and builds each record inline, unconditionally
   including `:source`. It is deliberately NOT the current
   `top-level-form-records`, which now delegates to the refactored, shared
   `parse-and-build-records` / `form-records-from-walked` that
   `outline-source` also uses — comparing against that shared builder would
   let a bug in it hide from this differential, since both sides would
   reflect the same bug identically. `extract-ns-requires` is referenced from
   the live `outline` namespace because it is unchanged since 9f48694 and is
   not part of the record-building path in question.

   Both sides are compared as `pr-str`, which captures small-map insertion
   order as well as values."
  {:lane :fast}
  (:require
   [clj-surgeon.cljc.walk :as cwalk]
   [clj-surgeon.forms :as forms]
   [clj-surgeon.outline :as outline]
   [clojure.string :as str]
   [clojure.test :refer [deftest is testing]]
   [rewrite-clj.node :as n]
   [rewrite-clj.zip :as z]))

;; The private helpers below are copied verbatim (as of `origin/main`
;; 9f48694, pre-MEM-015) from `src/clj_surgeon/outline.clj`, renamed with a
;; `legacy-` prefix because the originals are private there.

(defn- legacy-file-extension [file]
  (let [s (str file)
        i (.lastIndexOf s ".")]
    (when (pos? i) (subs s (inc i)))))

(defn- legacy-resolve-user-fields
  [user-fields zloc type-str line]
  (into {} (for [[k f] user-fields
                 :let [v (try (f zloc)
                              (catch Exception e
                                (throw (ex-info
                                         (str ".clj-surgeon.edn: extractor for "
                                              type-str " :fields " k
                                              " threw at line " line ": "
                                              (.getMessage e))
                                         {:macro type-str
                                          :field k
                                          :line line}
                                         e))))]
                 :when (some? v)]
             [k v])))

(defn- legacy-extract-name
  [zloc]
  (loop [child (some-> zloc z/down z/right)]
    (when child
      (let [s (z/string child)
            tag (n/tag (z/node child))]
        (if (= :meta tag)
          (let [inner (some-> child z/down z/rightmost z/string)]
            (or inner s))
          (if (or (= :token tag) (= :symbol tag))
            s
            (recur (z/right child))))))))

(defn- legacy-extract-arglist
  [zloc]
  (let [type-str (some-> zloc z/down z/string)]
    (when (forms/has-arglists? type-str)
      (loop [child (some-> zloc z/down)]
        (when child
          (let [tag (n/tag (z/node child))]
            (cond
              (= :vector tag) (z/string child)
              (= :meta tag)   (let [inner (some-> child z/down z/rightmost)]
                                (if (and inner (= :vector (n/tag (z/node inner))))
                                  (z/string inner)
                                  (recur (z/right child))))
              :else           (recur (z/right child)))))))))

(defn- legacy-defmethod-dispatch-location
  [zloc]
  (loop [location (some-> zloc z/down z/right z/right)
         steps 0]
    (cond
      (or (nil? location) (< 64 steps)) nil
      (= :uneval (z/tag location)) (recur (z/right location) (inc steps))
      (= :meta (z/tag location)) (recur (some-> location z/down z/rightmost)
                                        (inc steps))
      :else location)))

(defn- legacy-extract-dispatch
  [zloc]
  (some-> (legacy-defmethod-dispatch-location zloc) z/string))

(defn- legacy-attached-comment-start
  [lines form-line]
  (let [idx (dec form-line)]
    (loop [i (dec idx), comment-start form-line]
      (if (neg? i)
        comment-start
        (let [line (str/trim (nth lines i ""))]
          (if (str/starts-with? line ";")
            (recur (dec i) (inc i))
            comment-start))))))

(defn- legacy-top-level-form-records
  "The two-parse `top-level-form-records`, frozen: parses via
   `cwalk/top-level-forms` and builds every record inline, always including
   `:source`. Does not touch the current `form-records-from-walked`."
  [file source project-aliases]
  (let [lines (str/split-lines source)
        defaults (cwalk/platforms-for-extension (legacy-file-extension file))
        walked (cwalk/top-level-forms source defaults)]
    (mapv (fn [{:keys [zloc platforms]}]
            (let [node (z/node zloc)
                  m (meta node)
                  type-str (some-> zloc z/down z/string)
                  form-spec (forms/spec-with-project-aliases
                              project-aliases type-str)
                  user-fields (:fields form-spec)
                  extracted (when user-fields
                              (legacy-resolve-user-fields user-fields zloc
                                                          type-str (:row m)))
                  name-val (cond
                             user-fields (:name extracted)
                             (some? form-spec) (legacy-extract-name zloc))
                  arglist (cond
                            user-fields (:arglist extracted)
                            name-val (legacy-extract-arglist zloc))
                  dispatch (when (and name-val
                                      (= :defmethod (:kind form-spec)))
                             (legacy-extract-dispatch zloc))
                  form-line (:row m)
                  comment-start (when form-line
                                  (legacy-attached-comment-start lines form-line))
                  extras (when extracted
                           (dissoc extracted :name :arglist))]
              (cond-> {:type (symbol (or type-str "?"))
                       :platforms (vec (sort platforms))
                       :source (z/string zloc)}
                form-line (assoc :line form-line)
                (:end-row m) (assoc :end-line (:end-row m))
                name-val (assoc :name (if (symbol? name-val)
                                        name-val
                                        (symbol (str name-val))))
                arglist (assoc :args arglist)
                dispatch (assoc :dispatch dispatch)
                (seq extras) (merge extras)
                (and form-line comment-start (< comment-start form-line))
                (assoc :comment-start comment-start))))
          walked)))

(defn legacy-outline-source
  "The two-parse outline path, frozen from `origin/main` 9f48694: a separate
   `z/of-string` parses the source a second time for the `ns` lookup, and
   every record from `legacy-top-level-form-records` carries `:source` that
   this function then discards via `dissoc`."
  [file source project-aliases]
  (let [records (legacy-top-level-form-records file source project-aliases)
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
(deftest single-parse-outline-has-reader-error-parity-on-malformed-source
  (testing (str "malformed reader input: the frozen two-parse path and the "
                "live single-parse path either both refuse the same way or "
                "both produce the same partial outline")
    (doseq [[label file source]
            [["unmatched delimiter" "unmatched.clj" "(defn a [] 1\n(def b 2)\n"]
             ["unterminated string" "unterminated.clj"
              "(def a \"unterminated\n(def b 2)\n"]
             ["bad dispatch macro" "bad_dispatch.clj" "#z(1 2 3)\n(def b 2)\n"]]]
      (let [outcome (fn [f]
                      (try {:ok (pr-str (f file source {}))}
                           (catch Throwable t
                             {:threw (.getName (class t))
                              :message (.getMessage t)})))
            legacy (outcome legacy-outline-source)
            live (outcome outline/outline-source)]
        (is (= legacy live)
            (str "old/new outcome diverged for " label
                 " — legacy=" (pr-str legacy) " live=" (pr-str live)))))))

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
