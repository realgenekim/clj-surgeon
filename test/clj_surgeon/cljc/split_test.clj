(ns clj-surgeon.cljc.split-test
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.tools.reader :as r]
            [clojure.tools.reader.reader-types :as rt]
            [clj-surgeon.cljc.merge :as m]
            [clj-surgeon.cljc.split :as s]))

(defn- parse-forms [src]
  (let [rdr (rt/string-push-back-reader src)]
    (->> (repeatedly #(r/read {:eof ::end :read-cond :preserve} rdr))
         (take-while #(not= ::end %))
         vec)))

(defn- normalize-ns-form
  "Within an ns form, replace the :require sub-form's contents with a set of
   require entries. Order in :require is semantically irrelevant; using a set
   makes the round-trip property robust to reorderings."
  [form]
  (if (and (seq? form) (= 'ns (first form)))
    (let [[op nm & subs] form
          subs' (mapv (fn [s]
                        (if (and (seq? s) (= :require (first s)))
                          (apply list :require (set (rest s)))
                          s))
                      subs)]
      (apply list op nm subs'))
    form))

(defn- normalize-forms [forms]
  (mapv normalize-ns-form forms))

(defn- read-fixture [path]
  (slurp (str "test-fixtures/cljc/merge/" path)))

(defn- round-trip [fixture-dir]
  (let [clj-src   (read-fixture (str fixture-dir "/in.clj"))
        cljs-src  (read-fixture (str fixture-dir "/in.cljs"))
        cljc-src  (m/merge-files clj-src cljs-src)
        {clj-out :clj cljs-out :cljs} (s/split-file cljc-src)]
    {:orig-clj  (normalize-forms (parse-forms clj-src))
     :orig-cljs (normalize-forms (parse-forms cljs-src))
     :got-clj   (normalize-forms (parse-forms clj-out))
     :got-cljs  (normalize-forms (parse-forms cljs-out))}))

(defn- check-round-trip [fixture-dir]
  (let [{:keys [orig-clj orig-cljs got-clj got-cljs]} (round-trip fixture-dir)]
    (is (= orig-clj got-clj))
    (is (= orig-cljs got-cljs))))

(deftest round-trip-dom-divergent
  (testing "merge → split returns the original CLJ and CLJS file forms"
    (check-round-trip "dom-divergent")))

(deftest round-trip-identical-requires
  (testing "merge → split is identity when CLJ and CLJS are already identical"
    (check-round-trip "identical-requires")))

(deftest round-trip-one-sided-cljs
  (testing "CLJS-only require survives the round trip"
    (check-round-trip "one-sided-cljs")))

(deftest round-trip-one-sided-clj
  (testing "CLJ-only require survives the round trip"
    (check-round-trip "one-sided-clj")))

(deftest round-trip-npm-asymmetric
  (testing "npm string require + divergent dom alias survive the round trip"
    (check-round-trip "npm-asymmetric")))

(deftest round-trip-collision-default
  (testing "Default collision (per-form #?(:clj X :cljs Y)) round-trips"
    (check-round-trip "collision-default")))

(deftest round-trip-refer-asymmetric
  (testing "Asymmetric :refer lists round-trip without losing names"
    (check-round-trip "refer-asymmetric")))

(deftest round-trip-no-requires
  (testing "ns with no :require form at all round-trips"
    (check-round-trip "no-requires")))

(deftest round-trip-unmatched-counts
  (testing "Asymmetric body-form counts round-trip via the strict-split fallback"
    (check-round-trip "unmatched-counts")))

;; ============================================================
;; Double round trip: split → merge → split → merge converges.
;; If merge introduced any normalization that split couldn't preserve,
;; the second pass would diverge from the first.
;; ============================================================

(defn- double-round-trip-stable? [fixture-dir]
  (let [clj1   (read-fixture (str fixture-dir "/in.clj"))
        cljs1  (read-fixture (str fixture-dir "/in.cljs"))
        cljc1  (m/merge-files clj1 cljs1)
        {clj2 :clj cljs2 :cljs} (s/split-file cljc1)
        cljc2  (m/merge-files clj2 cljs2)]
    (= (parse-forms cljc1) (parse-forms cljc2))))

(deftest double-round-trip-converges
  (doseq [f ["dom-divergent" "identical-requires" "one-sided-cljs"
             "one-sided-clj" "npm-asymmetric" "collision-default"
             "refer-asymmetric" "no-requires" "unmatched-counts"]]
    (testing (str "double round-trip stable for fixture: " f)
      (is (double-round-trip-stable? f)))))
