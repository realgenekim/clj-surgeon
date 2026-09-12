(require '[clj-surgeon.lane-manifest :as lm]
         '[babashka.process :as process]
         '[clojure.string :as str]
         '[clojure.pprint :as pp])

(let [names (sort (keys lm/runtime-measurements))
      data {:subject (str/trim (:out (process/shell {:out :string} "git" "rev-parse" "HEAD")))
            :paired (vec names)
            :runtimes (select-keys lm/namespace-runtimes names)
            :portability (select-keys lm/portability-runtimes names)
            :contract-failures (into {} (keep (fn [[n row]]
                                                (when (:contract-failure row)
                                                  [n (:contract-failure row)])))
                                 lm/runtime-measurements)}]
  (spit "docs/observations/2026-09-12-bbtower-block-b/attempt20/baseline.edn"
        (with-out-str (pp/pprint data))))
