;; Reproduce Opus F2 in the owned warm namespace; always restore from disk.
(require '[clojure.java.io :as io] '[clojure.walk :as walk])
(let [owner (the-ns 'clj-surgeon.receipt-artifacts)
      forms (with-open [r (java.io.PushbackReader. (io/reader "src/clj_surgeon/receipt_artifacts.clj"))]
              (doall (take-while some? (repeatedly #(read {:eof nil} r)))))
      renamed (fn [form] (walk/postwalk #(if (= 'policy-envelope-roots %) 'policy-envelope-roots-v2 %) form))]
  (try
    (binding [*ns* owner]
      (doseq [form forms :when (#{'policy-envelope-roots 'default-envelope} (second form))]
        (eval (renamed form))))
    (ns-unmap owner 'policy-envelope-roots)
    (binding [clojure.test/*report-counters* (ref clojure.test/*initial-report-counters*)]
      (clojure.test/test-vars
        [#'clj-surgeon.receipt-artifacts-boundary-test/destination-envelope-is-trusted-context-only
         #'clj-surgeon.receipt-artifacts-boundary-test/destination-envelope-policy-witness-assertion-count])
      (prn @clojure.test/*report-counters*)
      (assert (= 2 (:fail @clojure.test/*report-counters*)))
      (assert (zero? (:error @clojure.test/*report-counters*))))
    (finally
      (require 'clj-surgeon.receipt-artifacts :reload)
      (ns-unmap owner 'policy-envelope-roots-v2))))
