(ns clj-surgeon.namespace-execution
  "Runtime-independent execution facts for the shared process coordinator."
  (:require
   [clojure.test :as t]))

;; @spec TEST-ISO-015 -- count equality is not coverage: a namespace may drive
;; other deftests inside a witness. Observe which declared vars actually ran.
(defn run-observed [n vars run!]
  (let [expected (when-not (ns-resolve n 'test-ns-hook)
                   (mapv (comp :name meta)
                         (or (seq vars) (filter (comp :test meta) (vals (ns-interns n))))))
        executed (atom #{})
        test-var t/test-var
        counters (with-redefs [t/test-var
                               (fn [v]
                                 (when (= n (some-> v meta :ns ns-name))
                                   (swap! executed conj (:name (meta v))))
                                 (test-var v))]
                   (run!))]
    {:counters counters :expected-vars (when expected (vec (sort expected)))
     :executed-vars (vec (sort @executed))}))
