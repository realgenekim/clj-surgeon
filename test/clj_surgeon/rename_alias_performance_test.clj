(ns clj-surgeon.rename-alias-performance-test
  {:lane :battery}
  (:require
   [clj-surgeon.rename-alias-plan :as p]
   [clj-surgeon.rename-alias-test :as r]
   [clojure.test :refer [deftest is]]))

;; @spec RENAME-ALIAS-015
;; INTENT-TEST: RENAME-ALIAS-015
(deftest four-thousand-line-planning-under-five-seconds
  ;; Opus F4: 4,000 ordinary lines and one alias reference; -J-Xmx1g.
  (let [source (str r/header (apply str (repeat 3998 "(def ordinary 123456)\n")) "events/x\n")
        request (r/request {r/file source} 1)
        start (System/nanoTime)
        result (p/plan {r/file source} request)
        seconds (/ (- (System/nanoTime) start) 1e9)]
    (println "4000-line planner seconds:" seconds)
    (is (:ok result) (pr-str (dissoc result :candidates :detail)))
    (is (< seconds 5.0) (str "4000-line planner took " seconds " seconds"))))
