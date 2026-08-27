(ns clj-surgeon.syntax-var-refs-test
  (:require
   [clj-surgeon.syntax-var-refs :as syntax-var-refs]
   [clojure.test :refer [deftest is testing]]))

(deftest qualified-reference-evidence-is-exact-and-inert-safe
  (let [source
        (str "(ns app.consumer\n"
             "  (:require [source.ns :as src]))\n"
             "(defn live [x]\n"
             "  [(src/owner x) (source.ns/owner x)])\n"
             "(defn inert []\n"
             "  ['src/owner `src/owner '(source.ns/owner)\n"
             "   \"src/owner\" #_(src/owner)])\n"
             "(comment (src/owner))\n"
             "(defn bare [owner] (owner))\n")
        result (syntax-var-refs/scan-sources
                 {"src/app/consumer.clj" source}
                 "source.ns/owner")]
    (is (:ok result))
    (is (= 1 (:scanned-file-count result)))
    (is (= 1 (:candidate-file-count result)))
    (is (= [:alias-qualified :fully-qualified]
           (mapv :relation (:locations result))))
    (is (every? true? (map :authority (:locations result))))
    (is (every? #{:exact-qualified-syntax}
                (map :reference-authority (:locations result))))
    (is (= ["src/owner" "source.ns/owner"]
           (mapv :source (:locations result))))
    (is (every? string? (map :source_sha256 (:locations result))))))

(deftest bare-symbols-never-gain-semantic-authority
  (let [sources
        {"src/source/ns.clj"
         "(ns source.ns)\n(defn owner [x] x)\n(defn local [] (owner 1))\n"
         "src/app/referred.clj"
         "(ns app.referred (:require [source.ns :refer [owner]]))\n(defn use-it [] (owner 1))\n"}
        result (syntax-var-refs/scan-sources sources "source.ns/owner")]
    (is (:ok result))
    (is (empty? (:locations result)))
    (is (= 2 (:candidate-file-count result)))
    (is (= 0 (:reference-count result)))))

(deftest bare-symbols-become-bounded-non-authoritative-proof-gaps
  (let [sources
        {"src/source/ns.clj"
         "(ns source.ns)\n(defn owner [x] x)\n(defn live [] (owner 1))\n"
         "src/app/referred.clj"
         "(ns app.referred (:require [source.ns :refer [owner]]))\n(defn use-it [] (owner 1))\n"
         "src/app/used.clj"
         "(ns app.used (:use [source.ns]))\n(defn use-it [] (owner 1))\n"
         "src/app/shadow.clj"
         "(ns app.shadow)\n(defn local [owner] (owner 1))\n"}
        result (syntax-var-refs/scan-sources sources "source.ns/owner")]
    (is (:ok result))
    (is (empty? (:locations result)))
    (is (= 3 (:proof-gap-count result)))
    (is (= 3 (count (:proof-gaps result))))
    (is (every? false? (map :authority (:proof-gaps result))))
    (is (every? #{:bare-symbol-needs-resolution}
                (map :reason (:proof-gaps result))))
    (is (every? #{"owner"} (map :source (:proof-gaps result))))
    (is (= ["src/app/referred.clj"
            "src/app/used.clj"
            "src/source/ns.clj"]
           (mapv :file (:proof-gaps result))))
    (is (= ["src/app/referred.clj"
            "src/app/shadow.clj"
            "src/app/used.clj"
            "src/source/ns.clj"]
           (:candidate-files result)))))

(deftest retained-agent-routing-shape-separates-alias-evidence-from-bare-gap
  ;; Minimized from the 2026-08-27 managed-begin retained replay.
  (let [sources
        {"src/clj_surgeon/agent_routing.clj"
         "(ns clj-surgeon.agent-routing)\n(defn managed-begin [s] s)\n(defn update-block [s] (managed-begin s))\n"
         "test/clj_surgeon/agent_routing_test.clj"
         "(ns clj-surgeon.agent-routing-test (:require [clj-surgeon.agent-routing :as routing]))\n(deftest route-test (routing/managed-begin \"x\"))\n"}
        result
        (syntax-var-refs/scan-sources
          sources "clj-surgeon.agent-routing/managed-begin")]
    (is (:ok result))
    (is (= 1 (:reference-count result)))
    (is (= [:alias-qualified] (mapv :relation (:locations result))))
    (is (= 1 (:proof-gap-count result)))
    (is (= "src/clj_surgeon/agent_routing.clj"
           (-> result :proof-gaps first :file)))
    (is (false? (-> result :proof-gaps first :authority)))))

(deftest captured-source-scan-is-deterministic-and-bounded
  (let [sources
        (array-map
          "src/z.clj"
          "(ns z (:require [source.ns :as src]))\n(defn z [] (src/owner))\n"
          "src/a.clj"
          "(ns a)\n(defn a [] (source.ns/owner))\n")
        expected-files ["src/a.clj" "src/z.clj"]
        first-result (syntax-var-refs/scan-sources sources "source.ns/owner")]
    (is (= expected-files (mapv :file (:locations first-result))))
    (is (= first-result
           (syntax-var-refs/scan-sources sources "source.ns/owner")))
    (is (= {:ok false
            :error-type :syntax-var-scan-budget-exceeded
            :candidate-file-count 2
            :limit 1
            :source-unchanged true}
           (syntax-var-refs/scan-sources
             sources "source.ns/owner" {:max-candidate-files 1})))))

(deftest invalid-subject-and-parse-failure-refuse-without-evidence
  (testing "the subject must be fully qualified"
    (is (= {:ok false
            :error-type :invalid-syntax-var-subject
            :subject "owner"
            :source-unchanged true}
           (syntax-var-refs/scan-sources {} "owner"))))
  (testing "a candidate parse failure cannot publish a partial surface"
    (let [result
          (syntax-var-refs/scan-sources
            {"src/broken.clj" "(ns broken)\n(source.ns/owner"}
            "source.ns/owner")]
      (is (false? (:ok result)))
      (is (= :syntax-var-scan-failed (:error-type result)))
      (is (:source-unchanged result))
      (is (nil? (:locations result))))))
