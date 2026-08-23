(ns clj-surgeon.extract-header-test
  (:require
   [clj-surgeon.extract-header :as header]
   [clojure.test :refer [deftest is testing]]
   [rewrite-clj.parser :as parser]))

(def source-ns
  "(ns sample.core
  (:require
   [alpha.core :as alpha]
   [beta.core :as beta]
   [gamma.core :refer [used-gamma unused-gamma]]
   [delta.core :refer [unused-delta]]
   [epsilon.core :refer [old-epsilon] :rename {old-epsilon renamed-epsilon}]
   [zeta.core :as-alias zeta]))")

(defn- compile-header [forms]
  (header/compile-target-header
    {:source-ns-form source-ns
     :target-ns "sample.extracted"
     :form-sources forms}))

(deftest target-header-keeps-only-proved-dependency-namespaces
  (let [result (compile-header
                 ["(defn moved [x]
                    [alpha/value
                     used-gamma
                     renamed-epsilon
                     ::zeta/id
                     x])"])]
    (is (:ok result))
    (is (= ["alpha.core" "gamma.core" "epsilon.core" "zeta.core"]
           (:target-requires result)))
    (is (= ["beta.core" "delta.core"]
           (:omitted-target-requires result)))
    (is (some? (parser/parse-string-all (:ns-form result))))))

(deftest complete-namespace-qualifier-retains-a-libspec
  (let [result (header/compile-target-header
                 {:source-ns-form "(ns sample.core (:require [alpha.core]))"
                  :target-ns "sample.extracted"
                  :form-sources ["(defn moved [] alpha.core/value)"]})]
    (is (:ok result))
    (is (= ["alpha.core"] (:target-requires result)))))

(deftest namespace-without-requires-compiles-without-an-empty-clause
  (let [result (header/compile-target-header
                 {:source-ns-form "(ns sample.core)"
                  :target-ns "sample.extracted"
                  :form-sources ["(defn moved [] :ok)"]})]
    (is (:ok result))
    (is (= "(ns sample.extracted)" (:ns-form result)))
    (is (empty? (:target-requires result)))
    (is (empty? (:omitted-target-requires result)))))

(deftest namespace-name-metadata-and-other-clauses-survive-header-compilation
  (let [result (header/compile-target-header
                 {:source-ns-form
                  "(ns ^:no-doc sample.core \"docs\" {:author \"team\"} (:import java.time.Instant))"
                  :target-ns "sample.extracted"
                  :form-sources ["(defn moved [] :ok)"]})]
    (is (:ok result))
    (is (= "(ns ^:no-doc sample.extracted \"docs\" {:author \"team\"} (:import java.time.Instant))"
           (:ns-form result)))))

(deftest raw-text-lookalikes-do-not-retain-a-dependency
  (let [result (compile-header
                 ["(defn moved [alpha]
                    ;; beta/value
                    [alpha \"beta/value\" 'beta/value])"])]
    (is (:ok result))
    (is (empty? (:target-requires result)))
    (is (= ["alpha.core" "beta.core" "gamma.core" "delta.core"
            "epsilon.core" "zeta.core"]
           (:omitted-target-requires result)))))

(deftest refer-all-is-retained-conservatively
  (let [result (header/compile-target-header
                 {:source-ns-form "(ns sample.core (:require [alpha.core :refer :all]))"
                  :target-ns "sample.extracted"
                  :form-sources ["(defn moved [] :ok)"]})]
    (is (:ok result))
    (is (= ["alpha.core"] (:target-requires result)))))

(deftest unprovable-require-shapes-refuse-without-candidates
  (doseq [[source reason]
          [["(ns sample.core (:require [alpha.side-effects]))"
            :side-effect-only-require]
           ["(ns sample.core (:require #?(:clj [alpha.core :as alpha])))"
            :reader-conditional-or-non-vector-entry]
           ["(ns sample.core (:require [alpha [core :as alpha]]))"
            :prefix-or-non-keyword-libspec]
           ["(ns sample.core (:require ;; ownership\n [alpha.core :as alpha]))"
            :comment-bearing-require-clause]]]
    (let [result (header/compile-target-header
                   {:source-ns-form source
                    :target-ns "sample.extracted"
                    :form-sources ["(defn moved [] :ok)"]})]
      (is (false? (:ok result)))
      (is (= :unsupported-require-minimization (:error-type result)))
      (is (= reason (:reason result)))
      (is (true? (:source-unchanged result)))
      (is (true? (:target-unchanged result)))
      (is (nil? (:ns-form result))))))

(deftest copy-all-is-a-conservative-byte-preserving-require-policy
  (doseq [[source expected copied-count]
          [["(ns sample.core (:require [alpha.side-effects]))"
            "(ns sample.extracted (:require [alpha.side-effects]))"
            1]
           ["(ns sample.core (:require ;; ownership\n [alpha.core :as alpha]))"
            "(ns sample.extracted (:require ;; ownership\n [alpha.core :as alpha]))"
            1]
           ["(ns sample.core (:require #?(:clj [alpha.core :as alpha])))"
            "(ns sample.extracted (:require #?(:clj [alpha.core :as alpha])))"
            1]
           ["(ns ^:no-doc sample.core\n  \"Docs stay.\"\n  (:import java.time.Instant))"
            "(ns ^:no-doc sample.extracted\n  \"Docs stay.\"\n  (:import java.time.Instant))"
            0]]]
    (let [result (header/compile-target-header
                   {:source-ns-form source
                    :target-ns "sample.extracted"
                    :form-sources ["(defn moved [] :ok)"]
                    :require-policy :copy-all})]
      (is (:ok result))
      (is (= :copy-all (:require-policy result)))
      (is (= copied-count (:copied-require-count result)))
      (is (= :copied-exactly (:target-requires result)))
      (is (= expected (:ns-form result)))))

  (let [result (header/compile-target-header
                 {:source-ns-form "(ns sample.core)"
                  :target-ns "sample.extracted"
                  :form-sources []
                  :require-policy :guess})]
    (is (false? (:ok result)))
    (is (= :unknown-require-policy (:error-type result)))
    (is (= [:copy-all :minimal] (:supported-require-policies result)))))

(deftest alias-allocation-is-stable-and-collision-free
  (testing "use the readable base when it is free"
    (is (= "schedule"
           (header/allocate-alias "sample.views.schedule" {}))))
  (testing "reuse a binding to the same namespace"
    (is (= "existing"
           (header/allocate-alias "sample.views.schedule"
                                  {"existing" "sample.views.schedule"}))))
  (testing "suffix collisions in increasing order"
    (is (= "schedule4"
           (header/allocate-alias "sample.views.schedule"
                                  {"schedule" "domain.schedule"
                                   "schedule2" "other.two"
                                   "schedule3" "other.three"})))))

(deftest source-callers-produce-one-deduplicated-sorted-refer-list
  (let [source "(ns sample.core)
                (defn moved-a [] :a)
                (defn moved-b [] :b)
                (defn caller-one [] [(moved-b) (moved-a)])
                (defn caller-two [] (moved-b))
                (defn unrelated [] :ok)"
        callers (header/remaining-source-callers source ["moved-a" "moved-b"])]
    (is (= [{:owner "caller-one" :moved-vars ["moved-a" "moved-b"]}
            {:owner "caller-two" :moved-vars ["moved-b"]}]
           callers))
    (is (= ["moved-a" "moved-b"]
           (header/source-referred-forms callers)))))

(deftest callerless-extraction-needs-no-source-reference
  (let [source "(ns sample.core)
                (defn moved [] :moved)
                (defn unrelated [] :ok)"
        callers (header/remaining-source-callers source ["moved"])]
    (is (empty? callers))
    (is (empty? (header/source-referred-forms callers)))))
