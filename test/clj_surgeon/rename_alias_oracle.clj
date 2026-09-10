(ns clj-surgeon.rename-alias-oracle
  "Separate acceptance selector; shares rewrite-clj and insertion oracle inventory only."
  (:require [clj-surgeon.insert-forms-oracle :as o]
            [clojure.string :as str]))

(defn intervals [s old new]
  (let [tree (o/inventory s)]
    (letfn [(walk [x in-ns? tag-name?]
              (let [text (:text x) children (o/code x)
                    ns? (or in-ns? (and (= :list (:tag x)) (= "ns" (o/operator x))))
                    prefix (cond
                             (and (= :token (:tag x)) (not ns?) (not tag-name?)
                                  (str/starts-with? text (str old "/"))) 0
                             (and (#{:token :keyword} (:tag x)) (not ns?) (str/starts-with? text (str "::" old "/"))) 2
                             (and (= :namespaced-map (:tag x)) (str/starts-with? text (str "#::" old "{"))) 3)
                    binding (when (and ns? (= :vector (:tag x)))
                              (for [[a b] (partition 2 1 children)
                                    :when (and (#{":as" ":as-alias"} (:text a)) (= old (:text b)))]
                                [(:start b) (:end b) new]))]
                (if (= :uneval (:tag x)) []
                    (concat binding
                            (when prefix [[(+ (:start x) prefix) (+ (:start x) prefix (count old)) new]])
                            (mapcat (fn [i c] (walk c ns? (and (= :reader-macro (:tag x)) (zero? i))))
                                    (range) children)))))]
      (vec (sort-by first (walk tree false false))))))
(defn verify [a b request detail]
  (try
    (let [spans (intervals a (:old_alias request) (:new_alias request))
          wanted (reduce (fn [s [start end text]] (str (subs s 0 start) text (subs s end))) a (reverse spans))
          before (o/inventory a) after (o/inventory b)
          roots #(vec (remove (fn [x] (#{:whitespace :newline :comma :comment} (:tag x))) (:entries %)))
          aa (roots before) bb (roots after)
          changed (filterv #(not= (:text (first %)) (:text (second %))) (map vector aa bb))]
      (o/assert-law (= wanted b) :authorized-intervals)
      (o/assert-law (= (o/digest a) (:source_hash detail)) :source-hash)
      (o/assert-law (= (o/digest b) (:result_hash detail)) :result-hash)
      (o/assert-law (= (count aa) (count bb)) :root-count)
      (o/assert-law (= (count changed) (:forms_changed detail)) :changed-count)
      (doseq [{:keys [before_index after_index before_sha256 after_sha256]}
              (concat (:changed_forms detail) (get-in detail [:preservation :other_forms]))]
        (o/assert-law (= before_sha256 (o/digest (:text (nth aa (dec before_index))))) :before-form)
        (o/assert-law (= after_sha256 (o/digest (:text (nth bb (dec after_index))))) :after-form))
      {:ok true})
    (catch Exception e {:ok false :law (or (:law (ex-data e)) :oracle-error)})))
