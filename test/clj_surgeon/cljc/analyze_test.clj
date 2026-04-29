(ns clj-surgeon.cljc.analyze-test
  (:require [clojure.test :refer [deftest is testing]]
            [clj-surgeon.cljc.analyze :as ana]
            [clj-surgeon.cljc.merge :as m]))

(defn- read-fixture [path]
  (slurp (str "test-fixtures/cljc/merge/" path)))

(deftest analyze-pair-divergent-alias
  (testing "Divergent dom alias is reported in :divergent."
    (let [r (ana/analyze-pair (read-fixture "dom-divergent/in.clj")
                              (read-fixture "dom-divergent/in.cljs"))
          divergent (:divergent (:requires r))]
      (is (= 1 (count divergent)))
      (let [{c :clj cs :cljs} (first divergent)]
        (is (= "com.fulcrologic.fulcro.dom-server" (:ns c)))
        (is (= "com.fulcrologic.fulcro.dom"        (:ns cs)))
        (is (= "dom" (:as c)))
        (is (= "dom" (:as cs)))))))

(deftest analyze-pair-shared-and-one-sided
  (testing "Shared, one-sided, and divergent buckets are populated correctly."
    (let [r (ana/analyze-pair (read-fixture "one-sided-clj/in.clj")
                              (read-fixture "one-sided-clj/in.cljs"))
          {:keys [shared clj-only cljs-only divergent]} (:requires r)]
      (is (= [{:ns "clojure.string" :as "str"}] shared))
      (is (= [{:ns "clojure.java.io" :as "io"}] clj-only))
      (is (empty? cljs-only))
      (is (empty? divergent)))))

(deftest analyze-pair-refer-asymmetric
  (testing "Asymmetric :refer surfaces as a divergent ns entry."
    (let [r (ana/analyze-pair (read-fixture "refer-asymmetric/in.clj")
                              (read-fixture "refer-asymmetric/in.cljs"))
          {:keys [divergent]} (:requires r)]
      (is (= 1 (count divergent)))
      (is (= "clojure.string" (:ns (:clj (first divergent)))))
      (is (= "clojure.string" (:ns (:cljs (first divergent))))))))

(deftest analyze-cljc-round-trip-shape
  (testing "analyze-cljc on a merged CLJC file reports the same divergent set
            as analyze-pair on the originals."
    (let [clj-src  (read-fixture "dom-divergent/in.clj")
          cljs-src (read-fixture "dom-divergent/in.cljs")
          cljc-src (m/merge-files clj-src cljs-src)
          a-pair   (ana/analyze-pair clj-src cljs-src)
          a-cljc   (ana/analyze-cljc cljc-src)]
      (is (= (:requires a-pair) (:requires a-cljc)))
      (is (= :cljc (:input a-cljc))))))

(deftest analyze-pair-form-summaries
  (testing "Top-level forms are summarized with platform tags derived from
            the source's extension."
    (let [r (ana/analyze-pair "(ns x) (defn shared [])"
                              "(ns x) (defn shared [])")]
      (is (= [{:type "defn" :platforms [:clj]  :name "shared" :line 1}]
             (:forms-clj r)))
      (is (= [{:type "defn" :platforms [:cljs] :name "shared" :line 1}]
             (:forms-cljs r))))))
