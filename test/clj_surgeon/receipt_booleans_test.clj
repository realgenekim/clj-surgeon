(ns clj-surgeon.receipt-booleans-test
  {:lane :fast}
  (:require
   [clj-surgeon.insert-forms :as insert]
   [clj-surgeon.insert-forms-support :as h]
   [clj-surgeon.intent-transaction :as tx]
   [clj-surgeon.mcp-server :as server]
   [clj-surgeon.rename-alias :as rename]
   [clj-surgeon.rename-alias-receipt-test :as rr]
   [clj-surgeon.rename-alias-test :as r]
   [clojure.edn :as edn]
   [clojure.string :as str]
   [clojure.test :refer [deftest is testing]]))

(defn boolean-paths [value]
  (letfn [(visit [path x]
            (cond
              (boolean? x) [path]
              (map? x) (mapcat (fn [[k v]] (visit (conj path k) v)) x)
              (vector? x) (mapcat (fn [i v] (visit (conj path i) v)) (range) x)
              :else []))]
    (vec (visit [] value))))
(defn registered-verbs [catalog]
  (set (for [tool catalog
             :when (and (contains? (:outcome-classes tool) :committed)
                        (contains? (get-in tool [:schema :properties]) "version"))]
         (:name tool))))
(defn missing-registrations [verbs registry]
  (vec (sort (remove (set (keys registry)) verbs))))
(defn missing-fields [receipt seams]
  (vec (sort (remove (set (keys seams)) (set (map peek (boolean-paths receipt)))))))
(defn rename-receipt [result]
  (if-let [path (:receipt_details_path result)]
    (assoc result :per_file (:per_file (edn/read-string (slurp path)))) result))
(defn rename-scenario [seam]
  (if-let [change ({:neighbor-corruption #(str/replace % "untouched 1" "untouched 2")
                    :trivia-corruption #(str % " ")
                    :discard-corruption #(str/replace % "#_ignored" "#_changed")} seam)]
    (let [{:keys [receipt detail]} (rr/disk-fault change)]
      (assoc receipt :per_file (:per_file detail)))
    (h/with-file rr/source
      (fn [dir _ _]
        (rename-receipt
          (rename/execute! (assoc (r/request {r/file rr/source} 1) :workspace_root (.getCanonicalPath dir))
                           (if (= seam :stage-failure) {:stage #(throw (java.io.IOException. "injected staging failure"))} {})))))))
(defn insert-scenario [seam]
  (h/with-file h/source
    (fn [_ target req]
      (if (= seam :neighbor-corruption)
        (let [commit tx/commit-compiled!]
          (with-redefs [tx/commit-compiled!
                        (fn [compiled io-fns]
                          (commit compiled
                                  (update io-fns :write-source!
                                          (fn [write]
                                            (fn [f text]
                                              (write f text)
                                              (when (= text (get-in compiled [:future-sources f]))
                                                (spit target (str/replace text "ab [] 2" "ab [] 9"))))))))]
            (insert/execute! req)))
        (insert/execute! req (if (= seam :stage-failure)
                               {:stage #(throw (java.io.IOException. "injected staging failure"))} {}))))))
(def scenarios {"insert_forms" insert-scenario "rename_alias" rename-scenario})

;; @spec RECEIPT-BOOL-001
;; INTENT-TEST: RECEIPT-BOOL-001
(deftest every-receipt-boolean-has-a-driven-false-witness
  (let [registry (:verbs (edn/read-string (slurp "docs/intent/receipt-booleans/registry.edn")))
        verbs (registered-verbs (server/public-tool-registry))]
    (is (= [] (missing-registrations verbs registry)) "Missing boolean seam registry by verb")
    (is (= [] (missing-registrations verbs scenarios)) "Missing executable scenario by verb")
    (doseq [verb verbs :let [run (scenarios verb)] :when run]
      (testing verb
        (let [committed (run :completed-write) seams (registry verb)]
          (is (= "committed" (:state committed)))
          (is (= [] (missing-fields committed seams)) (str verb " missing boolean fields"))
          (doseq [path (boolean-paths committed) :let [seam (seams (peek path))] :when seam]
            (let [faulted (run seam)]
              (is (false? (get-in faulted path)) (str verb " " path " via " seam)))))))))

;; @spec RECEIPT-BOOL-001
;; INTENT-TEST: RECEIPT-BOOL-001
(deftest unregistered-verbs-and-fields-fail-by-name
  (let [catalog (conj (server/public-tool-registry)
                      {:name "third_verb" :outcome-classes #{:committed}
                       :schema {:properties {"version" {:const 1}}}})]
    (is (= ["third_verb"] (missing-registrations (registered-verbs catalog) scenarios))))
  (is (= [:unwitnessed] (missing-fields {:ok true :nested {:unwitnessed true}} {:ok :stage-failure}))))
