(ns clj-surgeon.splice-projection
  "Surgeon's legacy character-slice and preorder view of the syntax inventory."
  (:require [clj-splice.core :as splice]))

;; @spec RENAME-ALIAS-014
;; INTENT: RENAME-ALIAS-014
;; @spec RENAME-ALIAS-015
;; INTENT: RENAME-ALIAS-015
(defn tree [source]
  (let [{:keys [nodes utf16->byte] :as inventory} (splice/spans source)
        trivia #{:whitespace :newline :comment :comma}
        ;; z/of-string starts on the first significant child; z/next skips only
        ;; trivia. Synthetic children consume a preorder, but had no address.
        addresses (into {} (map-indexed (fn [i n] [(:id n) i])
                                        (remove #(trivia (:tag %)) (rest nodes))))]
    (letfn [(project [id ordinal]
              (let [{:keys [children utf16-start utf16-end row end-row end-col] :as n} (nodes id)]
                (assoc n :byte-start (:start n) :byte-end (:end n)
                       :start utf16-start :end utf16-end :source source
                       :utf16->byte utf16->byte :form_index ordinal :line row
                       :end_line (when end-row (if (and (= 1 end-col) (> end-row (or row 0)))
                                                 (dec end-row) end-row))
                       :address {:preorder (when row (addresses id))}
                       :children (mapv #(project % ordinal) children))))]
      (assoc (project 0 0) :inventory inventory
             :children (loop [ids (:children (nodes 0)) ordinal 0 result []]
                         (if-let [id (first ids)]
                           (let [ordinal (if (trivia (:tag (nodes id))) ordinal (inc ordinal))]
                             (recur (next ids) ordinal (conj result (project id ordinal)))) result))))))
