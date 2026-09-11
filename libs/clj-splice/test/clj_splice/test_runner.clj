(ns clj-splice.test-runner
  (:require [clojure.test :as t] [clj-splice.core-test :as tests]))
(defn run [_]
  (let [result (t/run-tests 'clj-splice.core-test)]
    (println "projection-sha256" (tests/projection-hash))
    (when (pos? (+ (:fail result) (:error result))) (System/exit 1))))
(defn -main [& _] (run nil))
