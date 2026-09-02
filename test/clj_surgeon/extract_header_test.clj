(ns clj-surgeon.extract-header-test
  (:require
   [clj-surgeon.extract-header :as header]
   [clojure.string :as str]
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

;; @spec MCP-OP-EXTRACT-002
;; @spec MCP-OP-EXTRACT-003
;; SUPERSEDED CONTRACT, rf2-1: this used to assert that the source docstring and
;; every source import were copied into the target verbatim. Cohort rf1 measured
;; that exact behavior as the content of every run's native repair patch, so the
;; minimal policy now drops the docstring unless the caller supplies :doc and
;; prunes imports the way it already prunes requires. Metadata, the attribute
;; map, and every unreferenced-but-proved clause still survive.
(deftest namespace-name-metadata-and-other-clauses-survive-header-compilation
  (testing "metadata and the attribute map survive; the docstring and an
            unreferenced import do not"
    (let [result (header/compile-target-header
                   {:source-ns-form
                    "(ns ^:no-doc sample.core \"docs\" {:author \"team\"} (:import java.time.Instant))"
                    :target-ns "sample.extracted"
                    :form-sources ["(defn moved [] :ok)"]})]
      (is (:ok result))
      (is (= "(ns ^:no-doc sample.extracted {:author \"team\"})"
             (:ns-form result)))))

  (testing "a referenced import keeps the clause, the metadata and the map"
    (let [result (header/compile-target-header
                   {:source-ns-form
                    "(ns ^:no-doc sample.core \"docs\" {:author \"team\"} (:import java.time.Instant))"
                    :target-ns "sample.extracted"
                    :form-sources ["(defn moved [] (Instant/now))"]
                    :doc "docs"})]
      (is (:ok result))
      (is (= "(ns ^:no-doc sample.extracted \"docs\" {:author \"team\"} (:import java.time.Instant))"
             (:ns-form result))))))

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

;; ============================================================
;; rf2-1 — the target header the extraction actually needs
;; Field provenance: cohort rf1. Every structural run's native fallback patch
;; did the same five things and nothing else, and three of them are here:
;;   A-g2: "its minimal header correctly prunes requires but copies unrelated
;;          imports (Path, Paths, LinkOption, UUID)"
;;   the source docstring "Proof-carrying semantic selection followed by one
;;          addressed transaction." was copied verbatim into a namespace it
;;          does not describe;
;;   and `[... :as mcp-exact-verify :refer [...]]` had to be rewritten to
;;   `[... :as exact-verify]` in 4 of 4 runs.
;; ============================================================

;; @spec MCP-OP-EXTRACT-002
(deftest a-target-namespace-carries-no-docstring-unless-the-caller-supplies-one
  (testing "the source's docstring is not copied into a namespace it does not describe"
    (let [result (header/compile-target-header
                   {:source-ns-form
                    (str "(ns sample.core\n"
                         "  \"Proof-carrying semantic selection followed by one"
                         " addressed transaction.\"\n"
                         "  (:require\n   [alpha.core :as alpha]))")
                    :target-ns "sample.extracted"
                    :form-sources ["(defn moved [] (alpha/go))"]})]
      (is (:ok result))
      (is (not (str/includes? (:ns-form result) "Proof-carrying"))
          (str "the source docstring survived: " (:ns-form result)))
      (is (= "(ns sample.extracted\n  (:require\n   [alpha.core :as alpha]))"
             (:ns-form result)))))

  (testing "a supplied :doc is emitted exactly, in the source's own layout"
    (let [result (header/compile-target-header
                   {:source-ns-form
                    "(ns sample.core\n  \"Old docs.\"\n  (:require\n   [alpha.core :as alpha]))"
                    :target-ns "sample.extracted"
                    :form-sources ["(defn moved [] (alpha/go))"]
                    :doc "One project-owned line.\n   And a continuation."})]
      (is (:ok result))
      (is (= (str "(ns sample.extracted\n"
                  "  \"One project-owned line.\n"
                  "   And a continuation.\"\n"
                  "  (:require\n   [alpha.core :as alpha]))")
             (:ns-form result)))))

  (testing ":copy-all keeps its promise to preserve the header exactly"
    (let [source "(ns ^:no-doc sample.core\n  \"Docs stay.\"\n  (:import java.time.Instant))"
          result (header/compile-target-header
                   {:source-ns-form source
                    :target-ns "sample.extracted"
                    :form-sources ["(defn moved [] :ok)"]
                    :require-policy :copy-all})]
      (is (:ok result))
      (is (str/includes? (:ns-form result) "\"Docs stay.\"")))))

;; @spec MCP-OP-EXTRACT-003
(deftest target-imports-are-pruned-exactly-as-requires-already-are
  (testing "only the classes the moved forms reference survive"
    (let [result (header/compile-target-header
                   {:source-ns-form
                    (str "(ns sample.core\n"
                         "  (:require\n   [alpha.core :as alpha])\n"
                         "  (:import\n"
                         "   (java.nio.charset StandardCharsets)\n"
                         "   (java.nio.file LinkOption Path Paths)\n"
                         "   (java.security MessageDigest)\n"
                         "   (java.util UUID)))")
                    :target-ns "sample.extracted"
                    :form-sources
                    ["(defn moved [t]\n  (alpha/tap)\n  (.digest (MessageDigest/getInstance \"SHA-256\")\n           (.getBytes t StandardCharsets/UTF_8)))"]})]
      (is (:ok result))
      (is (= ["(java.nio.charset StandardCharsets)"
              "(java.security MessageDigest)"]
             (:target-imports result)))
      (is (= ["(java.nio.file LinkOption Path Paths)" "(java.util UUID)"]
             (:omitted-target-imports result)))
      (is (= (str "(ns sample.extracted\n"
                  "  (:require\n   [alpha.core :as alpha])\n"
                  "  (:import\n"
                  "   (java.nio.charset StandardCharsets)\n"
                  "   (java.security MessageDigest)))")
             (:ns-form result)))))

  (testing "a package group keeps exactly the classes that are referenced"
    (let [result (header/compile-target-header
                   {:source-ns-form
                    "(ns sample.core\n  (:import\n   (java.nio.file Path Paths LinkOption)))"
                    :target-ns "sample.extracted"
                    :form-sources ["(defn moved [p] (Paths/get p (into-array String [])))"]})]
      (is (:ok result))
      (is (= ["(java.nio.file Paths)"] (:target-imports result)))))

  (testing "an emptied :import clause is removed, not left behind"
    (let [result (header/compile-target-header
                   {:source-ns-form
                    "(ns sample.core\n  (:import\n   (java.util UUID)))"
                    :target-ns "sample.extracted"
                    :form-sources ["(defn moved [] :ok)"]})]
      (is (:ok result))
      (is (not (str/includes? (:ns-form result) ":import"))
          (str "an empty import clause survived: " (pr-str (:ns-form result)))))))

;; @spec MCP-OP-EXTRACT-005
(deftest the-source-header-loses-exactly-what-left-with-the-moved-forms
  (testing "a require and imports whose last use moved out are removed"
    (let [ns-form (str "(ns sample.core\n"
                       "  (:require\n"
                       "   [alpha.core :as alpha]\n"
                       "   [beta.core :as beta])\n"
                       "  (:import\n"
                       "   (java.security MessageDigest)\n"
                       "   (java.util UUID)))")
          result (header/narrow-source-ns-header
                   ns-form
                   ["(defn moved [] (beta/go) (MessageDigest/getInstance \"SHA-256\"))"]
                   ["(defn stays [] (alpha/go) (UUID/randomUUID))"])]
      (is (:ok result))
      (is (= ["beta.core"] (:removed-requires result)))
      (is (= ["(java.security MessageDigest)"] (:removed-imports result)))
      (is (= (str "(ns sample.core\n"
                  "  (:require\n"
                  "   [alpha.core :as alpha])\n"
                  "  (:import\n"
                  "   (java.util UUID)))")
             (:ns-form result)))))

  (testing "an entry the remaining body still uses is retained"
    (let [result (header/narrow-source-ns-header
                   "(ns sample.core\n  (:require\n   [beta.core :as beta]))"
                   ["(defn moved [] (beta/go))"]
                   ["(defn stays [] (beta/also))"])]
      (is (:ok result))
      (is (= [] (:removed-requires result)))))

  (testing "an entry that was ALREADY dead is never touched: this narrows only
            what the extraction itself made dead"
    (let [ns-form "(ns sample.core\n  (:require\n   [gamma.core :as gamma]))"
          result (header/narrow-source-ns-header
                   ns-form
                   ["(defn moved [] :ok)"]
                   ["(defn stays [] :ok)"])]
      (is (:ok result))
      (is (= [] (:removed-requires result)))
      (is (= ns-form (:ns-form result))))))
