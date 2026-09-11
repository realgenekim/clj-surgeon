(ns clj-surgeon.rename-alias-oracle
  "Separate acceptance selector; shares rewrite-clj and insertion oracle inventory only."
  (:require [clj-surgeon.insert-forms-oracle :as o]
            [clojure.string :as str]))

(defn intervals [s old new]
  (let [tree (o/inventory s)]
    (letfn [(walk [x in-ns? tag-name? declaration?]
              (let [text (:text x) children (o/code x)
                    ns? (or in-ns? (and (= :list (:tag x)) (= "ns" (o/operator x))))
                    declaration? (or declaration? (and ns? (= :list (:tag x))
                                                       (#{":require" ":import" ":refer-clojure" ":gen-class"} (:text (first children)))))
                    prefix (cond
                             (and (= :token (:tag x)) (not declaration?) (not tag-name?)
                                  (str/starts-with? text (str old "/"))) 0
                             (and (#{:token :keyword} (:tag x)) (not declaration?) (str/starts-with? text (str "::" old "/"))) 2
                             (and (= :namespaced-map (:tag x)) (str/starts-with? text (str "#::" old "{"))) 3)
                    binding (when (and ns? declaration? (= :vector (:tag x)))
                              (for [[a b] (partition 2 1 children)
                                    :when (and (#{":as" ":as-alias"} (:text a)) (= old (:text b)))]
                                [(:start b) (:end b) new]))]
                (if (= :uneval (:tag x)) []
                    (concat binding
                            (when prefix [[(+ (:start x) prefix) (+ (:start x) prefix (count old)) new]])
                            (mapcat (fn [i c] (walk c ns? (and (= :reader-macro (:tag x)) (zero? i)) declaration?))
                                    (range) children)))))]
      (vec (sort-by first (walk tree false false false))))))
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
      (o/assert-law (= (dec (count spans)) (:references_changed detail)) :reference-count)
      (o/assert-law (= 1 (:bindings_changed detail)) :binding-count)
      (o/assert-law (= (count aa) (+ (count (:changed_forms detail)) (count (get-in detail [:preservation :other_forms])))) :complete-forms)
      (o/assert-law (= (- (count aa) (count changed)) (get-in detail [:preservation :other_forms_checked])) :other-count)
      (o/assert-law (= (mapv (fn [[start end _]] [(o/width (subs a 0 start)) (o/width (subs a start end))]) spans)
                      (mapv (juxt :offset :length) (:sites detail))) :site-byte-spans)
      (let [inverse (:inverse_splices detail)
            bytes (.getBytes ^String b "UTF-8")
            recovered (reduce (fn [bs {:keys [result_offset before after before_sha256 after_sha256]}]
                                (o/assert-law (= (o/digest before) before_sha256) :inverse-before-hash)
                                (o/assert-law (= (o/digest after) after_sha256) :inverse-after-hash)
                                (let [offset (int result_offset) end (+ offset (o/width after))]
                                  (o/assert-law (= after (String. (java.util.Arrays/copyOfRange bs offset (int end)) "UTF-8")) :inverse-target)
                                  (byte-array (concat (java.util.Arrays/copyOfRange bs 0 offset)
                                                      (.getBytes ^String before "UTF-8")
                                                      (java.util.Arrays/copyOfRange bs (int end) (alength bs))))))
                              bytes (reverse inverse))]
        (o/assert-law (= a (String. recovered "UTF-8")) :inverse-identity))
      (doseq [{:keys [before_index after_index before_sha256 after_sha256]}
              (concat (:changed_forms detail) (get-in detail [:preservation :other_forms]))]
        (o/assert-law (= before_sha256 (o/digest (:text (nth aa (dec before_index))))) :before-form)
        (o/assert-law (= after_sha256 (o/digest (:text (nth bb (dec after_index))))) :after-form))
      {:ok true})
    (catch Exception e {:ok false :law (or (:law (ex-data e)) :oracle-error)})))
