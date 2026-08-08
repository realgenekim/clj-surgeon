(ns bench.legacy-adapter-test
  (:require
   [clojure.test :refer [deftest is]]))

(deftest legacy-adapter-test
  (is (= :current
         (:route (bench.legacy-adapter/legacy-adapter {})))))

(deftest unrelated-test
  (is (= 2 (+ 1 1))))
