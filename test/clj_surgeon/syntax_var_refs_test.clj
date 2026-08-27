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
