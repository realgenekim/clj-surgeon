(ns clj-surgeon.split-proof-gate-test
  {:lane :fast}
  (:require
   [clojure.test :refer [deftest is]]))

;; @spec NS-SPLIT-051
;; INTENT-TEST: NS-SPLIT-051
(deftest background-status-state-space
  (let [status (try (requiring-resolve 'clj-surgeon.split-proof-gate/closure-status) (catch Exception _ nil))
        original {:receipt_id "r" :candidate_hash "h" :verification_complete false
                  :background_gate {:pid 42 :commands [["check-a"] ["check-b"]]}}
        closure {:receipt_id "r" :pid 42 :original_hash "o" :candidate_hash "h"
                 :state "complete" :checks [{:command ["check-a"] :exit 0 :finished? true :before_hash "h" :after_hash "h"}
                                            {:command ["check-b"] :exit 0 :finished? true :before_hash "h" :after_hash "h"}]}]
    (is (some? status) "background proof must have a public pure status reducer")
    (when status
      (doseq [[c current expected] [[nil "h" "pending"] [closure "h" "complete"]
                                    [(assoc-in closure [:checks 1 :exit] 1) "h" "failed"]
                                    [(assoc-in closure [:checks 0 :after_hash] "moved") "h" "stale"]
                                    [closure "moved" "stale"]
                                    [(assoc closure :original_hash "forged") "h" "failed"]
                                    [(update closure :checks pop) "h" "failed"]
                                    [(assoc closure :pid 7) "h" "failed"]
                                    [(assoc closure :after_hash "moved") "h" "stale"]
                                    [(assoc-in closure [:checks 1 :finished?] false) "h" "failed"]]]
        (is (= expected (:state (status original "o" c current))) (pr-str [c current expected])))
      (is (= ["check-b"] (:failed_command (status original "o" (assoc-in closure [:checks 1 :exit] 1) "h")))))))

;; @spec NS-SPLIT-051
(deftest status-retains-cold-and-trivial-proof-honesty
  (let [status (requiring-resolve 'clj-surgeon.split-proof-gate/closure-status)
        original {:candidate_hash "h" :state "committed" :verification_complete true
                  :proof_pending [] :checks [{:name "suite" :exit 0 :status "passed"}]}]
    (is (= "complete" (:state (status original "o" nil "h"))))
    (is (= "stale" (:state (status original "o" nil "changed"))))
    (is (= "pending" (:state (status (assoc original :verification_complete false) "o" nil "h"))))))
