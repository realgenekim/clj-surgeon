(ns clj-surgeon.registration-gate-fixture
  "File-only setup for the cold REAL-GATE-001 class oracle. Never changes Vars."
  (:require
   [cheshire.core :as json]
   [clj-surgeon.battery-parallel-runner :as parallel]
   [clj-surgeon.test-registration :as reg]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [rewrite-clj.zip :as z]))

(def subject 'clj-surgeon.sol-first-contact-test)
(def source "test/clj_surgeon/sol_first_contact_test.clj")
(def request {:namespace subject :lane :battery :runtime :jvm})

(defn- edit-collection! [path owner tag f]
  (spit path ((ns-resolve 'clj-surgeon.test-registration 'update-collection)
              (slurp path) owner tag #(z/replace % (f (z/sexpr %))))))

(defn -main [mode & [out]]
  (if (= "seed" mode)
    (do
      (spit source (str "(ns " subject " {:lane :battery}"
                     " (:require [clojure.test :refer [deftest is]]))\n"
                     "(deftest works (is true))\n"))
      (let [result (reg/register! "." request)]
        (when-not (:ok result)
          (throw (ex-info "Fixture enrollment must execute real controls" result))))
      (spit out (json/generate-string
                  {:argv (parallel/lane-command "-J-Xmx1024m" "lane.edn"
                           ['clj-surgeon.lane-manifest-test])
                   :files (vec (concat [source reg/manifest-file reg/witness-file reg/census-file]
                                 (vals (reg/control-paths subject))))})))
    (let [mask (Long/parseLong mode)
          missing? #(bit-test mask (dec %))]
      (when (missing? 1)
        (edit-collection! reg/manifest-file 'manifest :map #(dissoc % (list 'quote subject)))
        (edit-collection! reg/manifest-file 'portability-runtimes :map #(dissoc % subject)))
      (when (missing? 2)
        (spit source (str/replace (slurp source) "{:lane :battery}" "{}")))
      (when (missing? 3)
        (let [s (slurp reg/witness-file)
              pin (z/sexpr ((ns-resolve 'clj-surgeon.test-registration 'pin-loc) s))]
          (spit reg/witness-file
            ((ns-resolve 'clj-surgeon.test-registration 'replace-pin) s (dec pin)))))
      (when (missing? 4)
        (edit-collection! reg/witness-file 'adopted-since-round-one :set #(disj % subject)))
      (when (missing? 5)
        (io/delete-file ((reg/control-paths subject) :jvm) true)))))
