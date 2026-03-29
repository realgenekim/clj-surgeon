(ns ns-surgeon.outline-test
  (:require [clojure.test :refer [deftest is testing]]
            [ns-surgeon.outline :as outline]
            [clojure.string :as str]))

(def simple-ns
  "(ns my.app
  (:require [clojure.string :as str]))

(def version \"1.0\")

;; The main entry point
(defn -main
  \"Start the app.\"
  [& args]
  (println \"Hello\" (str/join args)))

(defn- helper [x]
  (inc x))

(defonce state (atom {}))
")

(def forward-ref-ns
  "(ns my.forward
  (:require [clojure.string :as str]))

(defn caller []
  (callee 42))

(def some-val 10)

(defn callee [x]
  (+ x some-val))
")

(defn outline-from-string [source]
  (let [tmp (java.io.File/createTempFile "ns-surgeon-test" ".clj")]
    (spit tmp source)
    (try
      (outline/outline (.getAbsolutePath tmp))
      (finally
        (.delete tmp)))))

(deftest test-basic-outline
  (let [result (outline-from-string simple-ns)]
    (testing "namespace detection"
      (is (= 'my.app (:ns result))))

    (testing "form count"
      (is (= 4 (:form-count result))))

    (testing "form types"
      (let [types (mapv :type (:forms result))]
        (is (= '[def defn defn- defonce] types))))

    (testing "form names"
      (let [names (mapv :name (:forms result))]
        (is (= '[version -main helper state] names))))

    (testing "line boundaries"
      (let [main-form (first (filter #(= '-main (:name %)) (:forms result)))]
        (is (some? main-form))
        (is (= 7 (:line main-form)))
        (is (= 10 (:end-line main-form)))))

    (testing "comment headers detected"
      (let [main-form (first (filter #(= '-main (:name %)) (:forms result)))]
        (is (= 6 (:comment-start main-form)))))

    (testing "arglists"
      (let [main-form (first (filter #(= '-main (:name %)) (:forms result)))]
        (is (= "[& args]" (:args main-form)))))))

(deftest test-form-boundaries-precise
  (let [result (outline-from-string simple-ns)
        forms (:forms result)]
    (testing "first form starts at correct line"
      (is (= 4 (:line (first forms)))))

    (testing "last form ends at correct line"
      (is (= 15 (:end-line (last forms)))))))

(deftest test-various-def-types
  (let [source "(ns my.types)

(def a 1)
(defn b [] 2)
(defn- c [] 3)
(defonce d (atom 4))
(defmacro e [x] `(inc ~x))
(defmulti f class)
(defprotocol G (h [this]))
(declare z)
"
        result (outline-from-string source)
        types (mapv :type (:forms result))]
    (is (= '[def defn defn- defonce defmacro defmulti defprotocol declare]
           types))))

(deftest test-metadata-handling
  (let [source "(ns my.meta)

(def ^:private secret 42)

(def ^:dynamic *binding* nil)

(defn ^:deprecated old-fn [] :old)
"
        result (outline-from-string source)
        names (mapv :name (:forms result))]
    (testing "metadata stripped from names"
      ;; Names should be the actual symbol, not the metadata
      (is (every? some? names))
      (is (not-any? #(str/starts-with? (str %) "^") (map str names))))))

(deftest test-guardrails-defn
  (let [source "(ns my.guardrails)

(>defn validated-fn
  [x y]
  [int? string? => map?]
  {:x x :y y})
"
        result (outline-from-string source)
        form (first (:forms result))]
    (is (= '>defn (:type form)))
    (is (= 'validated-fn (:name form)))))

(deftest test-empty-file
  (let [result (outline-from-string "(ns my.empty)\n")]
    (is (= 'my.empty (:ns result)))
    (is (= 0 (:form-count result)))
    (is (empty? (:forms result)))))
