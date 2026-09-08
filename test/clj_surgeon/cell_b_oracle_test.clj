(ns clj-surgeon.cell-b-oracle-test
  {:lane :battery}
  (:require
   [clj-surgeon.mcp-process :as process]
   [clojure.string :as str]
   [clojure.test :refer [deftest is]]
   [oracles.cell-b-preservation :as oracle]))

;; @spec NS-SPLIT-046
;; INTENT-TEST: NS-SPLIT-046
(deftest cell-b-lint-case-witness
  (let [r (process/run-bounded! {:command ["python3" "-B" "-m" "unittest" "discover" "-s" "test/oracles" "-p" "test_cell_b_oracle.py"]
                                 :cwd (System/getProperty "user.dir") :timeout-ms 30000})]
    (is (= 0 (:exit r)) (pr-str r))))

;; @spec NS-SPLIT-043
;; INTENT-TEST: NS-SPLIT-043
(deftest cell-b-partial-negative-witnesses
  (let [src oracle/source-file dst oracle/dest-file
        caller (first oracle/caller-files)
        old {src "(ns cfp-scheduler-killer.exports)\n(defn published? [] true)\n(defn calendar-ics [e] (published?))\n(comment (print (calendar-ics e)))\n"
             caller "(ns caller (:require [cfp-scheduler-killer.exports :as exports]))\n(def result (exports/published?))\n"}
        current {src "(ns cfp-scheduler-killer.exports)\n(defn published? [] true)\n(comment )\n"
                 dst "(ns cfp-scheduler-killer.exports.calendar (:require [cfp-scheduler-killer.exports :as exports]))\n(defn calendar-ics [e] (exports/published?))\n"
                 caller (get old caller)}
        check #(oracle/check-sources old %)]
    (is (:retained-qualified (check current)))
    (is (:mixed-callers (check current)))
    (is (:comment-policy (check current)))
    (is (false? (:retained-qualified (check (update current dst str/replace "exports/published?" "published?")))))
    (is (false? (:mixed-callers (check (update current caller str/replace "exports/published?" "calendar/published?")))))
    (is (false? (:comment-policy (check (assoc current src (get old src))))))
    (is (false? (:acyclic-direction (check (update current src str/replace
                                                   "(ns cfp-scheduler-killer.exports)"
                                                   "(ns cfp-scheduler-killer.exports (:require [cfp-scheduler-killer.exports.calendar :as calendar]))")))))))
