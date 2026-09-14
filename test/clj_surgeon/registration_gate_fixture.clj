(ns clj-surgeon.registration-gate-fixture
  "File-only setup for the cold REAL-GATE-001 class oracle. Never changes Vars."
  (:require
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

;; INTENT: REGNS-012
;; @spec REGNS-012
(defn -main [root mode]
  (let [root (.getCanonicalFile (io/file root))
        source (io/file root source)
        manifest (io/file root reg/manifest-file)
        witness (io/file root reg/witness-file)]
    (if (= "seed" mode)
      (do
        (spit source (str "(ns " subject " {:lane :battery}"
                       " (:require [clojure.test :refer [deftest is]]))\n"
                       "(deftest works (is true))\n"))
        (let [result (reg/register! root request)]
          (when-not (:ok result)
            (throw (ex-info "Fixture enrollment must execute real controls" result)))))
      (let [mask (Long/parseLong mode)
            missing? #(bit-test mask (dec %))]
        (when (missing? 1)
          (edit-collection! manifest 'manifest :map #(dissoc % (list 'quote subject)))
          (edit-collection! manifest 'portability-runtimes :map #(dissoc % subject)))
        (when (missing? 2)
          (spit source (str/replace (slurp source) "{:lane :battery}" "{}")))
        (when (missing? 3)
          (let [s (slurp witness)
                pin (z/sexpr ((ns-resolve 'clj-surgeon.test-registration 'pin-loc) s))]
            (spit witness
              ((ns-resolve 'clj-surgeon.test-registration 'replace-pin) s (dec pin)))))
        (when (missing? 4)
          (edit-collection! witness 'adopted-since-round-one :set #(disj % subject)))
        (when (missing? 5)
          (io/delete-file (io/file root ((reg/control-paths subject) :jvm)) true))))))
