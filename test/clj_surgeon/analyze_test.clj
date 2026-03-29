(ns clj-surgeon.analyze-test
  (:require [clojure.test :refer [deftest is testing]]
            [clj-surgeon.analyze :as a]
            [clojure.string :as str]))

;; ============================================================
;; Test fixtures: small Clojure snippets as strings (no files!)
;; ============================================================

(def simple-ns
  "(ns my.app
  (:require [clojure.string :as str]
            [my.db :as db]))

(defn helper [x]
  (str/upper-case x))

(defn- private-util [x]
  (+ x 1))

(defn main-fn [x]
  (helper (private-util x)))

(defn- dead-code []
  :never-called)

(def config {:port 3000})
")

(def forward-ref-ns
  "(ns my.forward)

(defn caller []
  (callee 42))

(defn middle []
  :ok)

(defn callee [x]
  (+ x (helper x)))

(defn helper [x]
  (* x 2))
")

(def circular-ns
  "(ns my.circular)

(defn ping [x]
  (when (pos? x)
    (pong (dec x))))

(defn pong [x]
  (when (pos? x)
    (ping (dec x))))
")

(def extraction-ns
  "(ns my.extract
  (:require [clojure.string :as str]
            [my.db :as db]))

(defn public-api [x]
  (format-result (process x)))

(defn- process [x]
  (clean (transform x)))

(defn- transform [x]
  (str/upper-case x))

(defn- clean [x]
  (str/trim x))

(defn- format-result [x]
  (str \"[\" x \"]\"))

(defn unrelated [y]
  (db/save y))
")

;; ============================================================
;; symbols-in-form
;; ============================================================

(deftest test-symbols-in-form
  (let [zloc (a/string->zloc "(defn foo [x] (bar (+ x 1) \"hello\" :keyword true nil))")]
    (testing "extracts symbol tokens, not literals"
      (let [syms (a/symbols-in-form zloc)]
        (is (contains? syms "defn"))
        (is (contains? syms "foo"))
        (is (contains? syms "bar"))
        (is (contains? syms "x"))
        (is (contains? syms "+"))
        (is (not (contains? syms "1")))       ;; number
        (is (not (contains? syms ":keyword"))) ;; keyword
        (is (not (contains? syms "\"hello\""))) ;; string
        (is (not (contains? syms "true")))     ;; literal
        (is (not (contains? syms "nil")))))))  ;; literal

;; ============================================================
;; qualified-symbols
;; ============================================================

(deftest test-qualified-symbols
  (let [zloc (a/string->zloc "(defn foo [x] (str/join (db/query x) (local-fn x)))")]
    (testing "extracts namespace prefixes from qualified symbols"
      (let [quals (a/qualified-symbols zloc)]
        (is (= #{"str" "db"} quals))))))

;; ============================================================
;; parse-ns-aliases
;; ============================================================

(deftest test-parse-ns-aliases
  (let [zloc (a/string->zloc simple-ns)
        ;; Find the ns form (first list)
        ns-zloc zloc
        aliases (a/parse-ns-aliases ns-zloc)]
    (testing "extracts alias map from ns :require"
      (is (= {"str" "clojure.string"
              "db" "my.db"}
             aliases)))))

;; ============================================================
;; required-aliases
;; ============================================================

(deftest test-required-aliases
  (let [alias-map {"str" "clojure.string" "db" "my.db" "io" "clojure.java.io"}
        zloc (a/string->zloc "(defn foo [x] (str/join x))")]
    (testing "only includes aliases actually used by the form"
      (let [needed (a/required-aliases zloc alias-map)]
        (is (= {"str" "clojure.string"} needed))))))

;; ============================================================
;; intra-ns-deps
;; ============================================================

(deftest test-intra-ns-deps-skips-declares
  (let [source "(ns my.app)

(declare helper)

(defn caller []
  (helper 42))

(defn helper [x]
  (inc x))
"
        zloc (a/string->zloc source)
        deps (a/intra-ns-deps zloc)]
    (testing "declares excluded from deps list"
      (is (not (some #(= "declare" (:type %)) deps))))
    (testing "defn helper found with correct deps"
      (let [h (first (filter #(= "helper" (:name %)) deps))]
        (is (some? h))
        (is (= "defn" (:type h)))))))

(deftest test-intra-ns-deps
  (let [zloc (a/string->zloc simple-ns)
        deps (a/intra-ns-deps zloc)]
    (testing "main-fn depends on helper and private-util"
      (let [main (first (filter #(= "main-fn" (:name %)) deps))]
        (is (contains? (:depends-on main) "helper"))
        (is (contains? (:depends-on main) "private-util"))))
    (testing "helper has no intra-ns deps"
      (let [h (first (filter #(= "helper" (:name %)) deps))]
        (is (empty? (:depends-on h)))))
    (testing "dead-code has no intra-ns deps"
      (let [d (first (filter #(= "dead-code" (:name %)) deps))]
        (is (empty? (:depends-on d)))))))

;; ============================================================
;; unreferenced-forms (dead code detection)
;; ============================================================

(deftest test-unreferenced-forms
  (let [zloc (a/string->zloc simple-ns)
        dead (a/unreferenced-forms zloc)]
    (testing "finds unreferenced private forms"
      (let [dead-names (set (map :name dead))]
        (is (contains? dead-names "dead-code"))))
    (testing "does NOT flag referenced private forms"
      (let [dead-names (set (map :name dead))]
        (is (not (contains? dead-names "private-util")))))))

;; ============================================================
;; extraction-closure
;; ============================================================

(deftest test-extraction-closure
  (let [zloc (a/string->zloc extraction-ns)]
    (testing "public-api closure pulls in its private helpers"
      (let [closure (a/extraction-closure zloc "public-api")
            names (set (map :name (:forms closure)))]
        (is (contains? names "public-api"))
        (is (contains? names "process"))
        (is (contains? names "format-result"))
        ;; transform and clean are pulled in through process
        (is (contains? names "transform"))
        (is (contains? names "clean"))))
    (testing "closure does NOT include unrelated forms"
      (let [closure (a/extraction-closure zloc "public-api")
            names (set (map :name (:forms closure)))]
        (is (not (contains? names "unrelated")))))))

;; ============================================================
;; dep-tree (transitive dependency tree)
;; ============================================================

(def dep-tree-ns
  "(ns my.app)

(def config {:x 1})

(defn helper [x]
  (+ x (:x config)))

(defn process [x]
  (helper (inc x)))

(defn main []
  (process 42))

(defn unrelated []
  :nope)
")

(deftest test-dep-tree-simple
  (let [zloc (a/string->zloc dep-tree-ns)
        deps (a/intra-ns-deps zloc)
        tree (a/dep-tree deps "main")]
    (testing "root is main"
      (is (= "main" (:name tree))))
    (testing "main depends on process"
      (is (= ["process"] (mapv :name (:deps tree)))))
    (testing "process depends on helper"
      (let [process-node (first (:deps tree))]
        (is (= ["helper"] (mapv :name (:deps process-node))))))
    (testing "helper depends on config (leaf)"
      (let [helper-node (-> tree :deps first :deps first)]
        (is (= "helper" (:name helper-node)))
        (let [config-node (first (:deps helper-node))]
          (is (= "config" (:name config-node)))
          (is (:leaf? config-node)))))))

(deftest test-dep-tree-leaf
  (let [zloc (a/string->zloc dep-tree-ns)
        deps (a/intra-ns-deps zloc)
        tree (a/dep-tree deps "config")]
    (testing "leaf form has no deps"
      (is (:leaf? tree))
      (is (nil? (:deps tree))))))

(deftest test-dep-tree-circular
  (let [zloc (a/string->zloc circular-ns)
        deps (a/intra-ns-deps zloc)
        tree (a/dep-tree deps "ping")]
    (testing "circular dep is marked"
      (let [pong-node (first (:deps tree))
            ping-back (first (:deps pong-node))]
        (is (= "pong" (:name pong-node)))
        (is (:circular? ping-back))))))

(deftest test-dep-tree-unrelated-excluded
  (let [zloc (a/string->zloc dep-tree-ns)
        deps (a/intra-ns-deps zloc)
        tree (a/dep-tree deps "main")
        all-names (a/flatten-dep-tree tree)]
    (testing "unrelated forms not in tree"
      (is (not (contains? all-names "unrelated"))))
    (testing "full transitive closure"
      (is (= #{"main" "process" "helper" "config"} all-names)))))

(deftest test-dep-tree-real-file
  (let [file "test-fixtures/state.clj"]
    (when (.exists (java.io.File. file))
      (let [zloc (a/file->zloc file)
            deps (a/intra-ns-deps zloc)
            tree (a/dep-tree deps "transition!")]
        (testing "transition! has deps"
          (is (some? (:deps tree)))
          (is (not (:leaf? tree))))
        (testing "transitive closure includes log-event! and its deps"
          (let [all (a/flatten-dep-tree tree)]
            (is (contains? all "transition!"))
            (is (contains? all "log-event!"))))))))

;; ============================================================
;; topological-sort
;; ============================================================

(deftest test-topological-sort
  (let [zloc (a/string->zloc forward-ref-ns)
        result (a/topological-sort zloc)]
    (testing "produces a valid ordering"
      (is (not (:has-cycles? result)))
      (is (= 4 (count (:sorted result)))))
    (testing "helper comes before callee (callee depends on helper)"
      (let [order (:sorted result)
            idx (into {} (map-indexed (fn [i n] [n i]) order))]
        (is (< (idx "helper") (idx "callee")))))
    (testing "callee comes before caller"
      (let [order (:sorted result)
            idx (into {} (map-indexed (fn [i n] [n i]) order))]
        (is (< (idx "callee") (idx "caller")))))))

(deftest test-topological-sort-cycles
  (let [zloc (a/string->zloc circular-ns)
        result (a/topological-sort zloc)]
    (testing "detects mutual recursion as cycles"
      (is (:has-cycles? result))
      (is (seq (:cycles result))))))

;; ============================================================
;; Integration: Run against real state.clj fixture
;; ============================================================

(deftest test-real-file-outline
  (let [file "test-fixtures/state.clj"]
    (when (.exists (java.io.File. file))
      (let [zloc (a/file->zloc file)
            deps (a/intra-ns-deps zloc)]
        (testing "parses real 2768-line file"
          (is (> (count deps) 200)))
        (testing "topological sort runs on real file"
          (let [result (a/topological-sort zloc)]
            (is (vector? (:sorted result)))
            (is (> (count (:sorted result)) 100))))))))
