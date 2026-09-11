(ns clj-surgeon.rename-alias-receipt-test
  {:lane :fast}
  (:require
   [clj-surgeon.insert-forms-support :as h]
   [clj-surgeon.intent-transaction :as tx]
   [clj-surgeon.rename-alias :as sut]
   [clj-surgeon.rename-alias-test :as r]
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.test :refer [deftest is]]
   [clojure.walk :as walk]))

(def source (str r/header "(def y events/x)\n(def untouched 1)\n#_ignored\n"))
(defn disk-fault [change]
  (h/with-file source
    (fn [dir target _]
      (let [req (assoc (r/request {r/file source} 1) :workspace_root (.getCanonicalPath dir))
            commit tx/commit-compiled!
            result (with-redefs [tx/commit-compiled!
                                 (fn [compiled io-fns]
                                   (commit compiled
                                           (update io-fns :write-source!
                                                   (fn [write]
                                                     (fn [f text]
                                                       (write f text)
                                                       (when (= text (get-in compiled [:future-sources f]))
                                                         (spit (io/file dir f) (change text))))))))]
                     (sut/execute! req))
            disk (slurp target)
            detail (when (:receipt_details_path result) (edn/read-string (slurp (:receipt_details_path result))))]
        {:receipt result :detail detail :disk disk}))))

;; @spec RENAME-ALIAS-013
;; INTENT-TEST: RENAME-ALIAS-013
(deftest receipt-observes-disk-neighbor-corruption
  (let [{:keys [receipt detail disk]} (disk-fault #(str/replace % "untouched 1" "untouched 2"))
        per-file (first (:per_file detail))]
    (is (= :commit-outcome-unknown (:error-type receipt)))
    (is (false? (:other_forms_unchanged receipt)))
    (is (false? (get-in per-file [:preservation :other_forms_unchanged])))
    (is (false? (:write_verified receipt)))
    (is (= (h/sha disk) (get-in receipt [:read_back_hashes r/file])))
    (is (= (h/sha disk) (:read_back_hash per-file)))
    (is (not= (:result_hash per-file) (:read_back_hash per-file)))))

;; @spec RENAME-ALIAS-013
;; INTENT-TEST: RENAME-ALIAS-013
(deftest receipt-observes-disk-trivia-and-discard-corruption
  (doseq [[change field] [[#(str % " ") :gaps_unchanged]
                          [#(str/replace % "#_ignored" "#_changed") :discards_unchanged]]]
    (let [{:keys [receipt detail]} (disk-fault change)]
      (is (= :commit-outcome-unknown (:error-type receipt)))
      (is (false? (get-in detail [:per_file 0 :preservation field]))))))

;; @spec RENAME-ALIAS-013
;; INTENT-TEST: RENAME-ALIAS-013
(deftest receipt-details-contains-observes-artifact
  (h/with-file source
    (fn [dir _ _]
      (let [write sut/write-detail!
            req (assoc (r/request {r/file source} 1) :workspace_root (.getCanonicalPath dir))
            result (with-redefs [sut/write-detail!
                                 (fn [path detail]
                                   (write path (walk/postwalk #(if (map? %) (dissoc (into {} %) :sites) %) detail)))]
                     (sut/execute! req))
            detail (when (:receipt_details_path result) (edn/read-string (slurp (:receipt_details_path result))))]
        (is (= "committed" (:state result)) (pr-str result))
        (is (not (contains? detail :sites)))
        (is (false? (boolean (some #{"sites"} (:details_contains result)))))
        (is (some #{"per_file"} (:details_contains result)))))))
