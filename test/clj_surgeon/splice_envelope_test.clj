(ns clj-surgeon.splice-envelope-test
  {:lane :fast}
  (:require [clojure.test :refer [deftest is testing]]
            [clj-surgeon.insert-forms]
            [clj-surgeon.rename-alias]
            [clj-surgeon.rename-alias-test]
            [clj-surgeon.insert-forms-support]
            [clojure.java.io]
            [clojure.edn]
)
  (:import (java.nio.file Files)))



;; @spec INSERT-FORMS-011
;; INTENT-TEST: INSERT-FORMS-011


;; @spec INSERT-FORMS-015
;; INTENT-TEST: INSERT-FORMS-015

(deftest bounded-input-path-encoding
  (testing "Strict decoding is shared; both disk entrances retain their stage"
    (clj-surgeon.insert-forms-support/with-file clj-surgeon.rename-alias-test/header
      (fn [dir file insertion]
        (java.nio.file.Files/write (.toPath file) (byte-array [(unchecked-byte 255)])
                                  (make-array java.nio.file.OpenOption 0))
        (let [rename (assoc (clj-surgeon.rename-alias-test/request
                              {clj-surgeon.rename-alias-test/file clj-surgeon.rename-alias-test/header} 0)
                            :workspace_root (.getCanonicalPath dir))]
          (doseq [[execute request] [[clj-surgeon.insert-forms/execute! insertion]
                                     [clj-surgeon.rename-alias/execute! rename]]]
            (is (= [:unsupported-source [-1]]
                   [(:error-type (execute request))
                    (vec (java.nio.file.Files/readAllBytes (.toPath file)))])))))))
  (doseq [verb ["insert-forms" "rename-alias"]]
    (let [rows (:refusals (clojure.edn/read-string (slurp (str "docs/intent/" verb "/refusals.edn"))))]
      (is (and (seq rows) (= (count rows) (count (set (map :type rows))))
               (every? (fn [row]
                         (and (every? #(contains? row %) [:promise :native_failure :native_method
                                                       :minimal_reproducer :class :existing_check
                                                       :owner :witness :retirement_condition])
                              (or (not= :none (:native_failure row))
                                  (#{:capability :protocol :resource} (:class row))))) rows))
          verb)))
  (testing "insert-forms-reader-safety-and-limits"
  (clj-surgeon.insert-forms-support/refused clj-surgeon.insert-forms-support/source
    (assoc (clj-surgeon.insert-forms-support/request clj-surgeon.insert-forms-support/source) :extra (reduce (fn [x _] [x]) nil (range 600)))
    :limit-exceeded)

  (clj-surgeon.insert-forms-support/with-file clj-surgeon.insert-forms-support/source
    (fn [_ file req]
      (let [text (str "(def x \"" (char 0xd800) "\")")
            result (clj-surgeon.insert-forms/execute! (assoc req :payload {:text text :forms 1}))]
        (is (= :invalid-request (:error-type result)))
        (is (= clj-surgeon.insert-forms-support/source (slurp file))))))
  (let [s (str (apply str (repeat 50001 "x ")) clj-surgeon.insert-forms-support/source)]
    (clj-surgeon.insert-forms-support/refused s (clj-surgeon.insert-forms-support/request s) :limit-exceeded))
  (clj-surgeon.insert-forms-support/refused clj-surgeon.insert-forms-support/source (assoc (clj-surgeon.insert-forms-support/request clj-surgeon.insert-forms-support/source) :payload
                        {:text (str (apply str (repeat 513 "'")) "x") :forms 1}) :limit-exceeded)
  (let [s (str (apply str (repeat 32768 " ")) "(defn a [] 1)")
        req (assoc (clj-surgeon.insert-forms-support/request s) :payload {:text (apply str (repeat 1000 "1\n")) :forms 1000})
        result (clj-surgeon.insert-forms/plan s req)]
    (is (= :limit-exceeded (:error-type result)))
    (is (= true (:source_unchanged result))))
  (clj-surgeon.insert-forms-support/with-file clj-surgeon.insert-forms-support/source
    (fn [_ file req]
      (let [huge (assoc-in req [:anchor :owner :name] (apply str (repeat 2097153 "a")))
            result (clj-surgeon.insert-forms/execute! huge)]
        (is (= :limit-exceeded (:error-type result)))
        (is (= clj-surgeon.insert-forms-support/source (slurp file))))))
  (is (= :invalid-request (:error-type (clj-surgeon.insert-forms/read-request "{:value #inst \"2026-01-01\"}"))))
  (doseq [s ["#=(throw (Exception.)) (defn a [] 1)" "#?(:clj (defn a [] 1))"
             "\ufeff(defn a [] 1)"]]
    (clj-surgeon.insert-forms-support/refused s (clj-surgeon.insert-forms-support/request s) :unsupported-source))
  (doseq [text ["#=(throw (Exception.))" "#?(:clj 1)"]]
    (clj-surgeon.insert-forms-support/refused clj-surgeon.insert-forms-support/source (assoc (clj-surgeon.insert-forms-support/request clj-surgeon.insert-forms-support/source) :payload {:text text :forms 1})
               :unsupported-payload-syntax))
  (doseq [text [(apply str (repeat 1048577 "x"))
                (str (apply str (repeat 513 "(")) "0" (apply str (repeat 513 ")")))]]
    (clj-surgeon.insert-forms-support/refused clj-surgeon.insert-forms-support/source (assoc (clj-surgeon.insert-forms-support/request clj-surgeon.insert-forms-support/source) :payload {:text text :forms 1})
               :limit-exceeded))
  (clj-surgeon.insert-forms-support/refused clj-surgeon.insert-forms-support/source (assoc-in (clj-surgeon.insert-forms-support/request clj-surgeon.insert-forms-support/source) [:payload :forms] 1001) :limit-exceeded)
  (let [s (apply str (repeat 8388609 " "))]
    (clj-surgeon.insert-forms-support/refused s (clj-surgeon.insert-forms-support/request s) :limit-exceeded))
  )
  (testing "insert-forms-path-confinement"

  (doseq [path ["../escape.clj" "/absolute.clj" "missing.clj" "src/example.cljs" "src/\u0000.clj"]]
    (clj-surgeon.insert-forms-support/with-file clj-surgeon.insert-forms-support/source
      (fn [_ file req]
        (let [r (clj-surgeon.insert-forms/execute! (assoc req :file path))]
          (is (= (if (= path "src/example.cljs") :unsupported-source :invalid-path) (:error-type r)))
          (is (= clj-surgeon.insert-forms-support/source (slurp file)))))))
  (doseq [link-type [:symbolic :hard]]
    (clj-surgeon.insert-forms-support/with-file clj-surgeon.insert-forms-support/source
      (fn [dir file req]
        (let [link (.toPath (clojure.java.io/file dir "alias.clj"))]
          (if (= :hard link-type)
            (Files/createLink link (.toPath file))
            (Files/createSymbolicLink link (.toPath file) (make-array java.nio.file.attribute.FileAttribute 0)))
          (is (= :invalid-path (:error-type (clj-surgeon.insert-forms/execute! (assoc req :file "alias.clj")))))
          (is (= clj-surgeon.insert-forms-support/source (slurp file))))))))
)

(deftest exactly-one-edn-request
  (doseq [[text expected] [["{}" {}] ["{} ; final comment" {}]
                          ["{} {}" :invalid-request] ["{:a 1 :a 2}" :invalid-request]
                          ["#foo {}" :invalid-request] ["#=(throw (Exception.))" :invalid-request]
                          ["{" :invalid-request]]]
    (let [r (clj-surgeon.insert-forms/read-request text)]
      (is (= expected (or (:error-type r) r))))))
