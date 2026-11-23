(ns clj-surgeon.seeded-failure-test
  "Throwaway: proves the branch's linux gate turns red on a failing test (CI brief step 2). Closed, never merged."
  (:require [clojure.test :refer [deftest is]]))

(deftest seeded-failure-must-turn-the-gate-red
  (is (= 1 2) "deliberately false: the gate must report this"))
