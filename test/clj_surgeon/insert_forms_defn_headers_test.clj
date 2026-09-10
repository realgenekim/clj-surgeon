(ns clj-surgeon.insert-forms-defn-headers-test
  {:lane :fast}
  (:require
   [clj-surgeon.insert-forms-support :as h]
   [clojure.test :refer [deftest]]))

;; @spec INSERT-FORMS-008
;; INTENT-TEST: INSERT-FORMS-008
(deftest insert-forms-defn-headers-and-arities

  (let [s "(defn \"a\" [] 1)"]
    (h/refused s (assoc-in (h/request s) [:anchor :owner :name] "\"a\"") :invalid-request))
  (doseq [s ["(defn ^:private a \"doc\" {:a 1} [x] {:pre [x]}\n  x)"
             "(clojure.core/defn a [x]\n  x)"]]
    (let [r (assoc (h/request s) :anchor (h/body "defn" "a" {:position "last"})
                   :payload {:text "42" :forms 1})]
      (h/accepted s r (str (subs s 0 (dec (count s))) "\n  42)"))))
  (let [s "(defn a ([] 0) ([x]\n  x) {:attr true})"
        r (assoc (h/request s) :anchor (h/body "defn" "a" {:position "last"})
                 :payload {:text "42" :forms 1})]
    (h/refused s r :anchor-ambiguous)
    (h/accepted s (assoc-in r [:anchor :arity] 2)
                "(defn a ([] 0) ([x]\n  x\n  42) {:attr true})")
    (h/refused s (assoc-in r [:anchor :arity] 3) :anchor-index-out-of-range))
  (h/refused h/source (assoc (h/request h/source) :anchor
                        (assoc (h/body "defn" "a" {:position "last"}) :arity 1))
             :invalid-request)
  (let [s "(defn a :bad)"]
    (h/refused s (assoc (h/request s) :anchor (h/body "defn" "a" {:position "last"}))
               :unsupported-owner-shape)))
