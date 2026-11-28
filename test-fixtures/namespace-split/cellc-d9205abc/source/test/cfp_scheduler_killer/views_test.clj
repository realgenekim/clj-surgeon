(ns cfp-scheduler-killer.views-test
  "Pure rendering rules — the ones a screenshot review keeps re-finding."
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.string :as str]
            [hiccup2.core :as h]
            [cfp-scheduler-killer.views :as views]))

(defn- render [hiccup] (str (h/html hiccup)))

(def ^:private t0 (java.time.Instant/parse "2026-08-10T01:00:00Z"))

(deftest opinions-stars-ride-first-comment-only-test
  ;; Gene, 2026-08-10: a person's stars ride only their FIRST comment —
  ;; repeating them on every line reads as re-voting.
  (let [row {:ratings [{:person-id "p1" :person-name "Ann" :stars 4.0 :at t0}]
             :comments [{:id "c1" :person-id "p1" :person-name "Ann"
                         :body "First thought." :at t0}
                        {:id "c2" :person-id "p1" :person-name "Ann"
                         :body "Second thought." :at t0}]}
        html (render (#'views/opinions-block row))]
    (testing "both comments render"
      (is (str/includes? html "First thought."))
      (is (str/includes? html "Second thought.")))
    (testing "the visible ★ span appears exactly once across her quote lines
              (the histogram's hover TITLE also names her — count only the
              rendered op-stars span, not the tooltip text)"
      (is (= 1 (count (re-seq #"op-stars" html)))))))

(deftest opinions-silent-raters-stay-named-test
  ;; Doctrine: every score has a name somewhere on the row.
  (let [row {:ratings [{:person-id "p1" :person-name "Ann" :stars 4.0 :at t0}
                       {:person-id "p2" :person-name "Gene" :stars 3.0 :at t0}]
             :comments [{:id "c1" :person-id "p1" :person-name "Ann"
                         :body "A comment." :at t0}]}
        html (render (#'views/opinions-block row))]
    (is (str/includes? html "also rated"))
    (is (str/includes? html "Gene"))
    ;; his stars render as a real span, not merely in the histogram tooltip
    (is (= 2 (count (re-seq #"op-stars" html))) "Ann's on her comment + Gene's in also-rated")))

(deftest histogram-buckets-and-hover-test
  ;; Second ruling (Gene, 2026-08-10): histograms have BARS; five buckets,
  ;; halves folding down. Every bucket renders (empty ones marked), and the
  ;; hover title names every rater precisely.
  (let [html (render (views/star-histogram
                      [{:person-name "Ann" :stars 4.0}
                       {:person-name "Gene" :stars 4.5}
                       {:person-name "Alex" :stars 2.0}]))]
    (testing "five buckets, three empty (1, 3, 5)"
      (is (= 5 (count (re-seq #"hbar" html))))
      (is (= 3 (count (re-seq #"empty" html)))))
    (testing "hover names every rater"
      (is (str/includes? html "Ann ★4"))
      (is (str/includes? html "Gene ★4.5")))))
