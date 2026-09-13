(ns measure-runner
  (:require
   [clj-surgeon.lane-manifest :as lm]
   [clj-surgeon.tmp-leak-support :as tmp]
   [clojure.test :as t]))

(defn -main [& args]
  (let [[runtime ns-name output] args
        n (symbol ns-name)
        guard (tmp/secure-tmpdir!
                {:bb-script "docs/observations/2026-09-12-bbtower-block-b/attempt20/measure_runner.clj"
                 :main-ns "measure-runner"
                 :bb-heap-mib 1024
                 :isolate-home? (not= :battery (lm/lane-of n))}
                args)]
    (when (:refused guard) (System/exit 97))
    (require n)
    (let [started (System/nanoTime)
          result (t/run-tests n)
          wall (long (/ (- (System/nanoTime) started) 1000000))
          row {:runtime (keyword runtime) :namespace n :elapsed-ms wall :result result
               :tmpdir (System/getProperty "java.io.tmpdir")
               :home (System/getProperty "user.home")}]
      (spit output (pr-str row))
      (prn row)
      (shutdown-agents)
      (System/exit (if (zero? (+ (:fail result) (:error result))) 0 1)))))

(when (= *file* (System/getProperty "babashka.file"))
  (apply -main *command-line-args*))
