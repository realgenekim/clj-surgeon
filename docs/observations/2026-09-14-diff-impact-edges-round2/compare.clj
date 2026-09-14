(require '[clojure.edn :as edn] '[clojure.set :as set])
(doseq [[id old new] [[:a "/var/tmp/forge/impact-fx/real-a-final/impact-list.edn" "/var/tmp/forge/impact-fx/round2-result-a/impact-list.edn"]
                      [:b "/var/tmp/forge/impact-fx/real-b-final/impact-list.edn" "/var/tmp/forge/impact-fx/round2-result-b/impact-list.edn"]]]
  (let [a (edn/read-string (slurp old)) b (edn/read-string (slurp new))
        as (set (map :namespace (:namespaces a))) bs (set (map :namespace (:namespaces b)))
        added (set/difference bs as)]
    (prn {:case id :before (count as) :after (count bs) :removed (sort (set/difference as bs))
          :added (vec (filter #(added (:namespace %)) (:namespaces b)))
          :unmatched-files (:unmatched-files b)})))
