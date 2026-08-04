(ns clj-surgeon.edit-dsl-test
  (:require
   [clj-surgeon.edit-dsl :as dsl]
   [clojure.test :refer [deftest is testing]]))

(deftest native-expression-compiles-to-the-existing-case-query
  (is (= [[:form 'transition]
          [:find :finish]
          :right
          [:replace '(assoc state :status :complete)]]
         (-> (dsl/form 'transition)
             (dsl/match :finish)
             dsl/right
             (dsl/replace '(assoc state :status :complete))))))

(deftest every-navigation-builder-appends-one-existing-step
  (let [start (dsl/form 'f)
        cases [["match" #(dsl/match % '(call _)) [:find '(call _)]]
               ["where" #(dsl/where % {:parent-tag :vector})
                [:where {:parent-tag :vector}]]
               ["right" dsl/right :right]
               ["left" dsl/left :left]
               ["up" dsl/up :up]
               ["down" dsl/down :down]
               ["span" #(dsl/span % 2) [:span 2]]
               ["partition" #(dsl/partition-all % 2) [:partition-all 2]]]]
    (doseq [[label build expected-step] cases]
      (testing label
        (is (= (conj start expected-step) (build start)))
        (is (= [[:form 'f]] start))))))

(deftest replacement-builders-append-the-existing-terminal-steps
  (is (= [[:form 'f] [:replace '(inc x)]]
         (dsl/replace (dsl/form 'f) '(inc x))))
  (is (= [[:form 'f] [:span 2] [:replace-span :finish '(inc x)]]
         (-> (dsl/form 'f)
             (dsl/span 2)
             (dsl/replace-span :finish '(inc x))))))

(deftest builders-preserve-clojure-data-without-evaluation
  (let [pattern (with-meta '(dangerous _ {:reader (reader-conditional :clj :jvm)})
                  {:line 9})
        replacement '(do (spit "/tmp/must-not-exist" "no") :value)
        query (-> (dsl/form 'f)
                  (dsl/match pattern)
                  (dsl/replace replacement))]
    (is (= pattern (-> query second second)))
    (is (= {:line 9} (meta (-> query second second))))
    (is (= replacement (-> query last second)))
    (is (= 3 (count query)))))

(deftest builders-refuse-invalid-paths-and-post-terminal-composition
  (doseq [path [nil '() {} :path]]
    (testing (pr-str path)
      (let [error (try
                    (dsl/right path)
                    nil
                    (catch Exception e (ex-data e)))]
        (is (= :invalid-edit-path (:error-type error)))
        (is (= path (:path error))))))
  (doseq [terminal [[[:replace :done]]
                    [[:replace-span :a :b]]]]
    (let [error (try
                  (dsl/right terminal)
                  nil
                  (catch Exception e (ex-data e)))]
      (is (= :terminal-edit-step (:error-type error)))
      (is (= (last terminal) (:terminal-step error))))))

(deftest counted-builders-require-positive-integers
  (doseq [[builder step] [[dsl/span :span]
                          [dsl/partition-all :partition-all]]
          value [nil 0 -1 1.5 "2" :two]]
    (testing (str step " " (pr-str value))
      (let [error (try
                    (builder (dsl/form 'f) value)
                    nil
                    (catch Exception e (ex-data e)))]
        (is (= :invalid-step-argument (:error-type error)))
        (is (= step (:step error)))
        (is (= value (:value error))))))
  (is (= [[:form 'f] [:span 1]]
         (dsl/span (dsl/form 'f) 1)))
  (is (= [[:form 'f] [:partition-all 3]]
         (dsl/partition-all (dsl/form 'f) 3))))

(deftest real-program-peer-routes-compile-exactly
  (is (= [[:form 'classify-request]
          [:find '(:public? resource)]
          :right
          [:find :public]
          [:replace :public-resource]]
         (-> (dsl/form 'classify-request)
             (dsl/match '(:public? resource))
             dsl/right
             (dsl/match :public)
             (dsl/replace :public-resource))))
  (is (= [[:form 'prepare-request]
          [:find 'timeout-ms]
          [:where {:parent-tag :vector}]
          :right
          [:replace '(or (:timeout-ms request) 5000)]]
         (-> (dsl/form 'prepare-request)
             (dsl/match 'timeout-ms)
             (dsl/where {:parent-tag :vector})
             dsl/right
             (dsl/replace '(or (:timeout-ms request) 5000)))))
  (is (= [[:form 'route-event]
          [:find 'case]
          :up
          :down
          :right
          :right
          [:partition-all 2]]
         (-> (dsl/form 'route-event)
             (dsl/match 'case)
             dsl/up
             dsl/down
             dsl/right
             dsl/right
             (dsl/partition-all 2)))))
