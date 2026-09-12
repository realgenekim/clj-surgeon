(ns portability-runner
  (:require [clj-surgeon.lane-manifest :as lm]
            [clj-surgeon.tmp-leak-support :as tmp]
            [clojure.test :as t]))

(defn -main [& args]
  (let [[runtime ns-name output mode] args
        n (symbol ns-name)
        guard (tmp/secure-tmpdir!
               {:bb-script "docs/observations/2026-09-12-bbtower-block-b/attempt22/portability_runner.clj"
                :main-ns "portability-runner"
                :bb-heap-mib 1024
                :isolate-home? (not= :battery (lm/lane-of n))}
               args)]
    (when (:refused guard) (System/exit 97))
    (let [started (System/nanoTime)
          loaded (try (require n) {:status :loaded}
                      (catch Throwable e
                        {:status :load-failed
                         :message (ex-message e)
                         :causes (mapv ex-message (take-while some? (iterate ex-cause e)))}))
          result (when (and (= :loaded (:status loaded)) (not= "load" mode))
                   (t/run-tests n))
          row (merge {:namespace n :runtime (keyword runtime)
                      :mode mode :started-at (str (java.time.Instant/now))
                      :elapsed-ms (long (/ (- (System/nanoTime) started) 1000000))}
                     loaded
                     (when result
                       {:result result :status (if (zero? (+ (:fail result) (:error result)))
                                                 :passed :test-failed)}))]
      (spit output (pr-str row))
      (prn row)
      (shutdown-agents)
      (System/exit (if (#{:loaded :passed} (:status row)) 0 1)))))

(when (= *file* (System/getProperty "babashka.file"))
  (apply -main *command-line-args*))
