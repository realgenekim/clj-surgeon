(require 'clojure.test)
(do
  (assert (= "/home/forge/src/clj-surgeon-datacode" (System/getProperty "user.dir")))
  (require 'clj-surgeon.lane-manifest-test :reload)
  (with-redefs-fn {(ns-resolve 'clj-surgeon.lane-manifest-test 'environment)
                   (constantly {"CENSUS_REGENERATE" "1"})}
    #(clojure.test/test-vars [#'clj-surgeon.lane-manifest-test/the-corpus-only-ever-grows-and-the-arithmetic-is-shown]))
  (require 'clj-surgeon.receipt-artifacts-boundary-test :reload)
  (let [r (clojure.test/run-tests 'clj-surgeon.lane-manifest-test 'clj-surgeon.receipt-artifacts-boundary-test)]
    (spit "docs/observations/2026-09-12-data-not-code/round5/focused-summary.edn" (pr-str r))
    (prn r)))
