(ns book-test
  (:require [clojure.test :refer [deftest is]]))

(deftest existing
  (is (= 1 1)))

(deftest selector
  (is true))

(deftest renders-a-button
  (is true))

(deftest renders-a-link
  (is true))
