(ns clj-surgeon.quoted-var-refs-test
  (:require
   [clj-surgeon.quoted-var-refs :as quoted-var-refs]
   [clj-surgeon.structural-lens :as structural-lens]
   [clojure.java.io :as io]
   [clojure.test :refer [deftest is testing]]))

(defn- delete-tree!
  [file]
  (doseq [entry (reverse (file-seq file))]
    (.delete entry)))

(deftest recognizes-var-references-by-namespace-identity
  (testing "fully qualified and aliased reader forms share one identity"
    (let [source (str "(ns consumer.core\n"
                      "  (:require [sample.core :as sample]))\n"
                      "(defn aliased [] #'sample/target)\n"
                      "(defn expanded [] (var sample.core/target))\n"
                      "(defn unrelated [] #'other.core/target)\n"
                      "(def text \"#'sample.core/target\")\n"
                      ";; #'sample.core/target\n"
                      "(def data '(var sample.core/target))\n"
                      "(def discarded #_(var sample.core/target) nil)\n"
                      "(def syntax-quoted `(var sample.core/target))\n")
          references (quoted-var-refs/references-in-source
                       "src/consumer/core.clj" source "sample.core/target")]
      (is (= ["#'sample/target" "(var sample.core/target)"]
             (mapv :source references)))
      (is (= #{:structural-var-quote}
             (set (map :reference-authority references))))))

  (testing "an unqualified Var resolves only inside its defining namespace"
    (is (= ["#'target"]
           (mapv :source
                 (quoted-var-refs/references-in-source
                   "src/sample/core.clj"
                   "(ns sample.core)\n(defn caller [] #'target)\n"
                   "sample.core/target"))))
    (is (empty?
          (quoted-var-refs/references-in-source
            "src/consumer/core.clj"
            "(ns consumer.core)\n(defn caller [] #'target)\n"
            "sample.core/target")))))

(deftest captured-source-scan-builds-one-context-per-candidate-not-subject
  (let [source
        (str "(ns consumer.core\n"
             "  (:require [sample.core :as sample]))\n"
             "(def refs [#'sample/first\n"
             "           #'sample/second\n"
             "           (var sample.core/first)\n"
             "           #'target])\n"
             "(def inert '(var sample.core/first))\n"
             "(def discarded #_(var sample.core/second) nil)\n")
        sources {"src/consumer/one.clj" source
                 "src/consumer/two.clj" source}
        subjects ["sample.core/second"
                  "sample.core/first"
                  "sample/first"
                  "consumer.core/target"
                  "sample.core/second"]
        context-var (ns-resolve 'clj-surgeon.quoted-var-refs
                                'namespace-context)
        reference-var (ns-resolve 'clj-surgeon.quoted-var-refs
                                  'var-reference-symbol)
        original-context @context-var
        original-reference @reference-var
        measure
        (fn [selected-subjects]
          (let [contexts (atom 0)
                traversals (atom 0)
                result
                (with-redefs-fn
                  {context-var
                   (fn [& args]
                     (swap! contexts inc)
                     (apply original-context args))
                   reference-var
                   (fn [& args]
                     (swap! traversals inc)
                     (apply original-reference args))}
                  #(quoted-var-refs/scan-sources sources selected-subjects))]
            {:result result
             :contexts @contexts
             :traversals @traversals}))
        single (measure ["sample.core/first"])
        many (measure subjects)
        result (:result many)
        expected-locations
        (vec
          (for [[file captured-source] (sort-by key sources)
                subject subjects
                reference
                (quoted-var-refs/references-in-source
                  file captured-source subject)]
            (merge reference
                   {:file_path file
                    :source_sha256
                    (structural-lens/source-hash captured-source)
                    :subject subject
                    :role :reference})))]
    (is (:ok result))
    (is (= 2 (:contexts single) (:contexts many))
        "candidate count, not subject count, owns context construction")
    (is (= (:traversals single) (:traversals many))
        "candidate traversal must not multiply with subject count")
    (is (= expected-locations (:locations result))
        "batch output preserves complete ordered singleton evidence")
    (is (= 2 (:candidate-file-count result)))
    (is (= (count expected-locations) (:reference-count result)))
    (is (= sources (:sources result)))))

(deftest captured-source-scan-refuses-invalid-or-partial-evidence
  (let [invalid (quoted-var-refs/scan-sources
                  {"src/valid.clj" "(ns valid)"}
                  [nil "" "missing-slash" "too/many/slashes"])]
    (is (false? (:ok invalid)))
    (is (= :invalid-quoted-var-subjects (:error-type invalid)))
    (is (= [{:index 0 :subject nil}
            {:index 1 :subject ""}
            {:index 2 :subject "missing-slash"}
            {:index 3 :subject "too/many/slashes"}]
           (:invalid-subjects invalid)))
    (is (not (contains? invalid :locations)))
    (is (not (contains? invalid :sources))))

  (let [result
        (quoted-var-refs/scan-sources
          {"src/a-valid.clj"
           "(ns valid)\n(def x #'sample.core/target)\n"
           "src/z-broken.clj"
           "(ns broken)\n(def x #'sample.core/target\n"}
          ["sample.core/target"])]
    (is (false? (:ok result)))
    (is (= :quoted-var-scan-failed (:error-type result)))
    (is (not (contains? result :locations)))
    (is (not (contains? result :sources)))
    (is (:source-unchanged result))))

(deftest captured-source-scan-is-pure-bounded-and-deterministic
  (let [sources
        {"src/z.clj"
         (str "(ns z (:require [sample.core :as sample]))\n"
              "(def found #'sample/target)\n")
         "src/a.clj"
         (str "(ns a)\n"
              "(def text \"#'sample.core/target\")\n")
         "src/sample/core.clj"
         (str "(ns sample.core)\n"
              "(def local #'target)\n")}
        result (quoted-var-refs/scan-sources
                 sources ["sample.core/target"])]
    (is (:ok result))
    (is (= 3 (:scanned-file-count result)))
    (is (= 3 (:candidate-file-count result)))
    (is (= ["src/sample/core.clj" "src/z.clj"]
           (mapv :file (:locations result))))
    (is (= 2 (:reference-count result)))
    (is (= (quoted-var-refs/scan-sources
             (into (array-map) (reverse sources))
             ["sample.core/target"])
           result)
        "map iteration order cannot change proof output"))

  (let [context-var (ns-resolve 'clj-surgeon.quoted-var-refs
                                'namespace-context)
        result
        (with-redefs-fn
          {context-var
           (fn [& _]
             (throw (ex-info "candidate parsing must not start" {})))}
          #(quoted-var-refs/scan-sources
             {"src/a.clj" "(ns a) (def x #'sample.core/target)"
              "src/b.clj" "(ns b) (def x #'sample.core/target)"}
             ["sample.core/target"]
             {:max-candidate-files 1}))]
    (is (false? (:ok result)))
    (is (= :quoted-var-scan-budget-exceeded (:error-type result)))
    (is (true? (:source-unchanged result))))

  (let [result (quoted-var-refs/scan-sources
                 {"src/broken.clj" "(ns broken) (def x #'sample.core/target"}
                 ["sample.core/target"])]
    (is (false? (:ok result)))
    (is (= :quoted-var-scan-failed (:error-type result)))
    (is (true? (:source-unchanged result)))))

(deftest workspace-proof-refuses-an-invalid-candidate-before-publishing-evidence
  (let [root (.toFile
               (java.nio.file.Files/createTempDirectory
                 "quoted-var-proof-"
                 (make-array java.nio.file.attribute.FileAttribute 0)))
        source (io/file root "src/broken.clj")]
    (try
      (.mkdirs (.getParentFile source))
      (spit source "(ns broken)\n(def x #'sample.core/target\n")
      (let [result (quoted-var-refs/scan-workspace
                     root ["sample.core/target"] slurp)]
        (is (false? (:ok result)))
        (is (= :quoted-var-scan-failed (:error-type result)))
        (is (:source-unchanged result)))
      (finally
        (delete-tree! root)))))
