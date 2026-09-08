(ns clj-surgeon.split-proof-gate-test
  {:lane :fast}
  (:require
   [cheshire.core :as json]
   [clj-surgeon.split-proof-gate :as gate]
   [clojure.test :refer [deftest is]]))

;; @spec NS-SPLIT-051
;; @spec NS-SPLIT-055
;; INTENT-TEST: NS-SPLIT-051
;; INTENT-TEST: NS-SPLIT-055
(deftest background-status-state-space
  (let [status (try (requiring-resolve 'clj-surgeon.split-proof-gate/closure-status) (catch Exception _ nil))
        original {:receipt_id "r" :candidate_hash "h" :verification_complete false
                  :background_gate {:pid 42 :argv ["/usr/bin/setsid" "/usr/bin/bb" "-m" "worker"]
                                    :worker_started "2026-09-08T00:00:00Z"
                                    :commands [["check-a"] ["check-b"]]}}
        closure {:receipt_id "r" :pid 42 :original_hash "o" :candidate_hash "h"
                 :worker_argv ["/usr/bin/setsid" "/usr/bin/bb" "-m" "worker"]
                 :worker_started "2026-09-08T00:00:00Z"
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
                                    [(dissoc closure :worker_argv) "h" "failed"]
                                    [(assoc closure :worker_argv ["/usr/bin/bb" "-m" "replayed-worker"]) "h" "failed"]
                                    [(assoc closure :worker_started "2026-09-08T00:00:01Z") "h" "failed"]
                                    [(assoc closure :after_hash "moved") "h" "stale"]
                                    [(assoc-in closure [:checks 1 :finished?] false) "h" "failed"]]]
        (is (= expected (:state (status original "o" c current))) (pr-str [c current expected])))
      (is (= "failed" (:state (status (update original :background_gate dissoc :argv) "o" closure "h"))))
      (is (= "failed" (:state (status (update original :background_gate dissoc :worker_started) "o" closure "h"))))
      (is (= ["check-b"] (:failed_command (status original "o" (assoc-in closure [:checks 1 :exit] 1) "h")))))))

;; @spec NS-SPLIT-056
;; INTENT-TEST: NS-SPLIT-056
(deftest background-temp-root-refuses-tmpfs-and-relative-paths
  (let [safe? (try (requiring-resolve 'clj-surgeon.split-proof-gate/safe-tmpdir?) (catch Exception _ nil))]
    (is (some? safe?) "background proof must expose its pure temp-root admission rule")
    (when safe?
      (is (true? (safe? (System/getProperty "java.io.tmpdir"))))
      (is (false? (safe? "/tmp/forge/sol")))
      (is (false? (safe? "relative/tmp")))
      (is (false? (safe? nil))))))

;; @spec NS-SPLIT-051
(deftest status-retains-cold-and-trivial-proof-honesty
  (let [status (requiring-resolve 'clj-surgeon.split-proof-gate/closure-status)
        original {:candidate_hash "h" :state "committed" :verification_complete true
                  :proof_pending [] :checks [{:name "suite" :exit 0 :status "passed"}]}]
    (is (= "complete" (:state (status original "o" nil "h"))))
    (is (= "stale" (:state (status original "o" nil "changed"))))
    (is (= "pending" (:state (status (assoc original :verification_complete false) "o" nil "h"))))))

;; @spec NS-SPLIT-057
;; INTENT-TEST: NS-SPLIT-057
(deftest receipt-ceiling-bounds-edn-and-escaped-json-independently
  (let [receipt {:facts {:file (apply str (repeat 11000 \u2028))}}
        edn-bytes (alength (.getBytes (pr-str receipt) "UTF-8"))
        json-bytes (alength (.getBytes (json/generate-string receipt {:escape-non-ascii true}) "UTF-8"))
        refusal (try (gate/bounded-receipt! receipt) nil
                     (catch clojure.lang.ExceptionInfo error error))]
    (is (< edn-bytes gate/max-receipt-bytes))
    (is (> json-bytes gate/max-receipt-bytes))
    (is (= :receipt-size-bound (:error-type (ex-data refusal))))))
