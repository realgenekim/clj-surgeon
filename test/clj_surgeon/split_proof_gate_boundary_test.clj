(ns clj-surgeon.split-proof-gate-boundary-test
  {:lane :battery}
  (:require
   [clj-surgeon.mcp-namespace-split-test :as fixture]
   [clj-surgeon.namespace-split-io :as split]
   [clj-surgeon.namespace-split-warm :as warm]
   [clj-surgeon.receipt-artifacts :as artifacts]
   [clj-surgeon.spawn-ledger :as spawn]
   [clj-surgeon.split-proof-gate :as gate]
   [clj-surgeon.structural-lens :as lens]
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.java.shell :as shell]
   [clojure.test :refer [deftest is]]))

;; @spec NS-SPLIT-050
;; INTENT-TEST: NS-SPLIT-050
(deftest background-gate-closes-immutable-receipt
  (doseq [[commands expected timeout] [[[["/usr/bin/printf" "first"] ["/usr/bin/printf" "second"]] "complete"]
                                       [[["/usr/bin/false"] ["/usr/bin/printf" "never"]] "failed"]
                                       [[["/usr/bin/touch" "src/app/arrived.clj"]] "stale"]
                                       [[["/usr/bin/true"]] "pending"]
                                       [[["/usr/bin/sleep" "1"]] "failed" 20]]]
    (fixture/with-workspace
      (fn [_root request]
        (let [receipts (.toFile (java.nio.file.Files/createTempDirectory "split-gate-artifacts-" (make-array java.nio.file.attribute.FileAttribute 0)))]
          (try
            (binding [artifacts/*artifact-root* (str receipts)]
              (with-redefs [split/analyze! fixture/analysis warm/discover! (constantly {:port 1})
                            warm/probe! (constantly {:ok true :name "warm-probe" :duration_ms 0 :exit 0})]
                (let [before (spawn/snapshot)
                      r (split/execute! {:verification-profiles {"unit" {:proof :warm :gate :background :commands commands :timeout-ms timeout}}} request)
                      status (try (requiring-resolve 'clj-surgeon.split-proof-gate/status!) (catch Exception _ nil))]
                  (is (= "committed-probe-only" (:state r)) (pr-str r))
                  (is (pos-int? (get-in r [:background_gate :pid])) (pr-str r))
                  ;; @spec TEST-ISO-002: a reaped child remains observable.
                  (is (some #(= (get-in r [:background_gate :pid]) (:pid %))
                            (spawn/recorded-between before (spawn/snapshot))))
                  (is (string? (:receipt_path r)))
                  (when (and status (:receipt_path r))
                    (let [before (slurp (:receipt_path r))
                          closure (:closure_receipt r)
                          deadline (+ (System/currentTimeMillis) 20000)]
                      (loop [] (when (and (not (.exists (io/file closure))) (< (System/currentTimeMillis) deadline))
                                 (Thread/sleep 50) (recur)))
                      (let [result (status (:receipt_path r))]
                        (is (= expected (:state result)) (pr-str result))
                        (is (= before (slurp (:receipt_path r))))
                        (is (false? (:verification_complete (edn/read-string before))))
                        (when (.exists (io/file closure))
                          (let [c (edn/read-string (slurp closure))]
                            (is (= (get-in r [:background_gate :pid]) (:pid c)))
                            (is (.startsWith ^String (:java_tmpdir c) "/var/tmp/"))
                            (is (every? #(and (:started %) (:finished %) (number? (:wall_ms %))) (:checks c)))
                            (when (= expected "failed") (is (= 1 (count (:checks c)))))))))))))
            (finally (doseq [f (reverse (file-seq receipts))] (.delete f)))))))))

;; @spec NS-SPLIT-050
;; @spec NS-SPLIT-051
(deftest detached-gate-survives-cli-caller-exit
  (fixture/with-workspace
    (fn [root request]
      (let [dir (.toFile (java.nio.file.Files/createTempDirectory "split-cli-gate-" (make-array java.nio.file.attribute.FileAttribute 0)))
            config-file (io/file dir "profiles.edn")
            receipt-root (str dir)
            release-path (str (io/file dir "release"))
            request (assoc-in request [:verification :profile-file] (str config-file))
            code (str "(require '[clj-surgeon.core :as core] '[clj-surgeon.namespace-split-io :as split] "
                      "'[clj-surgeon.namespace-split-warm :as warm] '[clj-surgeon.receipt-artifacts :as artifacts]) "
                      "(System/setProperty \"java.io.tmpdir\" " (pr-str (System/getProperty "java.io.tmpdir")) ") "
                      "(binding [artifacts/*artifact-root* " (pr-str receipt-root) "] "
                      "(with-redefs [split/analyze! (constantly '" (pr-str (fixture/analysis nil)) ") "
                      "warm/discover! (constantly {:port 1}) warm/probe! (constantly {:ok true :name \"warm-probe\" :exit 0 :duration_ms 0})] "
                      "(core/-main \":op\" \":split-ns!\" \":request\" " (pr-str (pr-str request)) "))) ")]
        (try
          (spit config-file (pr-str {:verification-profiles {"unit" {:proof :warm :gate :background
                                                                     :commands [["bb" "-e" (str "(loop [] (when-not (.exists (java.io.File. " (pr-str release-path) ")) (Thread/sleep 20) (recur)))")] ["/usr/bin/printf" "done"]]
                                                                     :timeout-ms 10000}}}))
          (let [process (shell/sh "bb" "--classpath" (str (.getAbsoluteFile (io/file "src"))) "-e" code)
                r (when (zero? (:exit process)) (edn/read-string (:out process)))]
            (is (= 0 (:exit process)) (pr-str process))
            (is (= "committed-probe-only" (:state r)) (pr-str r))
            (when (:receipt_path r)
              (let [original (slurp (:receipt_path r))
                    status (requiring-resolve 'clj-surgeon.split-proof-gate/status!)
                    deadline (+ (System/currentTimeMillis) 20000)]
                (is (= "pending" (:state (status (:receipt_path r)))))
                (spit release-path "continue")
                (loop [] (when (and (not (.exists (io/file (:closure_receipt r)))) (< (System/currentTimeMillis) deadline))
                           (Thread/sleep 20) (recur)))
                (let [result (shell/sh "bb" "--classpath" (str (.getAbsoluteFile (io/file "src")))
                               "-m" "clj-surgeon.core" ":op" ":proof-status" ":receipt" (:receipt_path r))]
                  (is (= 0 (:exit result)) (pr-str result))
                  (is (= "complete" (:state (edn/read-string (:out result))))))
                (is (= original (slurp (:receipt_path r))))
                (spit (io/file root "src/app/changed.clj") "(ns app.changed)\n")
                (let [result (shell/sh "bb" "--classpath" (str (.getAbsoluteFile (io/file "src")))
                               "-m" "clj-surgeon.core" ":op" ":proof-status" ":receipt" (:receipt_path r))]
                  (is (pos? (:exit result)))
                  (is (= "stale" (:state (edn/read-string (:out result)))))))))
          (finally (doseq [f (reverse (file-seq dir))] (.delete f))))))))

;; @spec NS-SPLIT-051
(deftest proof-status-cli-exits-nonzero-for-failed-stale-and-dead-worker
  (fixture/with-workspace
    (fn [root _request]
      (let [dir (.toFile (java.nio.file.Files/createTempDirectory "split-status-cli-" (make-array java.nio.file.attribute.FileAttribute 0)))
            current (gate/current-hash root ["src" "test"])
            argv ["/usr/bin/setsid" "/usr/bin/bb" "-m" "clj-surgeon.split-proof-gate"]
            started "2026-09-08T00:00:00Z"
            cli #(shell/sh "bb" "--classpath" (str (.getAbsoluteFile (io/file "src")))
                   "-m" "clj-surgeon.core" ":op" ":proof-status" ":receipt" (str %))]
        (try
          (doseq [[kind candidate closure? expected-state expected-error]
                  [["failed" current true "failed" "proof-gate-failed"]
                   ["stale" "not-the-current-snapshot" false "stale" "proof-snapshot-stale"]
                   ["dead" current false "failed" "proof-worker-exited"]]]
            (let [receipt (io/file dir (str kind "-receipt.edn"))
                  closure (io/file dir (str kind "-closure.edn"))
                  original {:ok true :committed true :state "committed-probe-only"
                            :receipt_id kind :receipt_path (str receipt) :closure_receipt (str closure)
                            :workspace_root root :coverage {:roots ["src" "test"]}
                            :candidate_hash candidate :verification_complete false
                            :proof_pending ["suite"]
                            :background_gate {:pid 999999999 :argv argv :worker_started started
                                              :commands [["suite"]]}}]
              (spit receipt (pr-str original))
              (when closure?
                (spit closure
                  (pr-str {:receipt_id kind :pid 999999999 :worker_argv argv :worker_started started
                           :original_hash (lens/source-hash (slurp receipt)) :candidate_hash candidate
                           :state "failed" :checks [{:command ["suite"] :exit 1 :finished? true
                                                     :before_hash candidate :after_hash candidate}]})))
              (let [result (cli receipt)
                    body (edn/read-string (:out result))]
                (is (pos? (:exit result)) (pr-str [kind result]))
                (is (= expected-state (:state body)) (pr-str body))
                (is (= expected-error (:error_type body)) (pr-str body)))))
          (finally (doseq [f (reverse (file-seq dir))] (.delete f))))))))
