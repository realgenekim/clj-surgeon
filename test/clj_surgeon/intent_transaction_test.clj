(ns clj-surgeon.intent-transaction-test
  (:require
   [clj-surgeon.intent-transaction :as transaction]
   [clojure.string :as str]
   [clojure.test :refer [deftest is testing]]
   [rewrite-clj.zip :as z]))

(defn- spec
  [intents expected]
  {:intents intents
   :expect expected})

(defn- intent
  [files from to expected-count]
  {:files files
   :from from
   :to to
   :expect-count expected-count})

(defn- valid-source?
  [source]
  (try
    (z/of-string source)
    true
    (catch Exception _ false)))

(deftest compiles-one-model-plan-into-heterogeneous-edits
  (let [sources
        {"src/a.clj"
         "(ns app.a)\n\n(defn page [x]\n  [(old-call x) :body])\n"
         "src/b.clj"
         "(ns app.b)\n\n(defn shell []\n  [:main :body])\n"}
        result
        (transaction/compile-transaction
          sources
          (spec [(intent ["src/a.clj"]
                         "(old-call x)"
                         "#(new-call %)"
                         1)
                 (intent ["src/a.clj" "src/b.clj"]
                         ":body"
                         ":body.ide-shell-page"
                         2)]
                {:intent-count 2
                 :edit-count 3
                 :changed-file-count 2}))]
    (is (= true (:ok result)))
    (is (= :change (:operation result)))
    (is (= 2 (:intent-count result)))
    (is (= 3 (:match-count result)))
    (is (= 2 (:changed-file-count result)))
    (is (= {"src/a.clj" 2 "src/b.clj" 1}
           (into {} (map (juxt :file :match-count) (:files result)))))
    (is (= "(ns app.a)\n\n(defn page [x]\n  [#(new-call %) :body.ide-shell-page])\n"
           (get-in result [:future-sources "src/a.clj"])))
    (is (= "(ns app.b)\n\n(defn shell []\n  [:main :body.ide-shell-page])\n"
           (get-in result [:future-sources "src/b.clj"])))
    (is (every? valid-source? (vals (:future-sources result))))
    (is (str/includes? (:diff result) "#(new-call %)"))
    (is (= {:whole-files-parsed true :file-count 2}
           (:validated result)))))

(deftest matches-lossless-syntax-while-ignoring-whitespace
  (testing "whitespace may vary, but literal replacement spelling is exact"
    (let [result
          (transaction/compile-transaction
            {"src/a.clj" "(ns app.a)\n(def values [(old-call   x) (old-call x)])\n"}
            (spec [(intent ["src/a.clj"]
                           "(old-call x)"
                           "(new-call\n  x)"
                           2)]
                  {:intent-count 1 :edit-count 2 :changed-file-count 1}))]
      (is (:ok result))
      (is (= "(ns app.a)\n(def values [(new-call\n  x) (new-call\n  x)])\n"
             (get-in result [:future-sources "src/a.clj"])))))

  (testing "an undeclared interior comment prevents a destructive match"
    (let [source "(ns app.a)\n(def value (old-call ; keep this reason\n             x))\n"
          sources {"src/a.clj" source}
          result
          (transaction/compile-transaction
            sources
            (spec [(intent ["src/a.clj"] "(old-call x)" "(new-call x)" 1)]
                  {:intent-count 1 :edit-count 1 :changed-file-count 1}))]
      (is (= :expect-count-mismatch (:error-type result)))
      (is (= 0 (:actual-count result)))
      (is (= {"src/a.clj" source} sources))))

  (testing "declaring the interior comment permits the replacement"
    (let [result
          (transaction/compile-transaction
            {"src/a.clj" "(ns app.a)\n(def value (old-call ; keep this reason\n             x))\n"}
            (spec [(intent ["src/a.clj"]
                           "(old-call ; keep this reason\n x)"
                           "(new-call ; replacement keeps the reason\n x)"
                           1)]
                  {:intent-count 1 :edit-count 1 :changed-file-count 1}))]
      (is (:ok result))
      (is (str/includes? (get-in result [:future-sources "src/a.clj"])
                         "; replacement keeps the reason")))))

(deftest exact-intents-do-not-cascade-or-match-textual-lookalikes
  (let [source
        (str "(ns app.a)\n"
             ";; (old-call x) is documentation, not syntax\n"
             "(def text \"(old-call x)\")\n"
             "(def value (old-call x))\n")
        result
        (transaction/compile-transaction
          {"src/a.clj" source}
          (spec [(intent ["src/a.clj"]
                         "(old-call x)"
                         "(wrapper (old-call x))"
                         1)]
                {:intent-count 1 :edit-count 1 :changed-file-count 1}))
        future (get-in result [:future-sources "src/a.clj"])]
    (is (:ok result))
    (is (= 3 (count (re-seq #"\(old-call x\)" future))))
    (is (str/includes? future ";; (old-call x) is documentation"))
    (is (str/includes? future "\"(old-call x)\""))
    (is (str/includes? future "(wrapper (old-call x))"))))

(deftest refuses-every-contract-error-as-data
  (let [sources {"src/a.clj" "(ns app.a)\n(def value (old-call x))\n"}
        valid-intent (intent ["src/a.clj"] "(old-call x)" "(new-call x)" 1)
        valid-expect {:intent-count 1 :edit-count 1 :changed-file-count 1}
        cases
        [{:label "empty intents"
          :spec (spec [] {:intent-count 0 :edit-count 0 :changed-file-count 0})
          :error :invalid-intents}
         {:label "missing source"
          :spec (spec [(intent ["src/missing.clj"] "(old-call x)" "(new-call x)" 1)]
                      valid-expect)
          :error :invalid-source}
         {:label "invalid before form"
          :spec (spec [(intent ["src/a.clj"] "(old-call" "(new-call x)" 1)]
                      valid-expect)
          :error :invalid-intent-form}
         {:label "two before forms"
          :spec (spec [(intent ["src/a.clj"] "(old-call x) extra" "(new-call x)" 1)]
                      valid-expect)
          :error :invalid-intent-form}
         {:label "lossless no-op"
          :spec (spec [(intent ["src/a.clj"] "(old-call x)" "(old-call   x)" 1)]
                      valid-expect)
          :error :no-op-intent}
         {:label "zero expected count"
          :spec (spec [(assoc valid-intent :expect-count 0)] valid-expect)
          :error :invalid-expect-count}
         {:label "per-intent mismatch"
          :spec (spec [(assoc valid-intent :expect-count 2)]
                      (assoc valid-expect :edit-count 2))
          :error :expect-count-mismatch}
         {:label "aggregate intent mismatch"
          :spec (spec [valid-intent] (assoc valid-expect :intent-count 2))
          :error :transaction-expectation-mismatch}
         {:label "aggregate edit mismatch"
          :spec (spec [valid-intent] (assoc valid-expect :edit-count 2))
          :error :transaction-expectation-mismatch}
         {:label "aggregate file mismatch"
          :spec (spec [valid-intent] (assoc valid-expect :changed-file-count 2))
          :error :transaction-expectation-mismatch}]]
    (doseq [{:keys [label spec error]} cases]
      (testing label
        (let [result (transaction/compile-transaction sources spec)]
          (is (= error (:error-type result)))
          (is (nil? (:future-sources result))))))))

(deftest refuses-overlapping-intents-before-producing-future-source
  (let [sources
        {"src/a.clj"
         "(ns app.a)\n(def value (outer (inner x)))\n"}
        result
        (transaction/compile-transaction
          sources
          (spec [(intent ["src/a.clj"] "(outer (inner x))" "(outer (new x))" 1)
                 (intent ["src/a.clj"] "(inner x)" "(new x)" 1)]
                {:intent-count 2 :edit-count 2 :changed-file-count 1}))]
    (is (= :overlapping-intents (:error-type result)))
    (is (= "src/a.clj" (:file result)))
    (is (= #{0 1} (set (:intent-indexes result))))
    (is (nil? (:future-sources result)))))

(deftest metadata-reader-syntax-and-token-spelling-are-exact
  (let [source
        "(ns app.a)\n(def values [^String x ^{:tag String} x #_ignored 1 1N])\n"
        result
        (transaction/compile-transaction
          {"src/a.clj" source}
          (spec [(intent ["src/a.clj"] "^String x" "^String renamed" 1)
                 (intent ["src/a.clj"] "1N" "2N" 1)]
                {:intent-count 2 :edit-count 2 :changed-file-count 1}))
        future (get-in result [:future-sources "src/a.clj"])]
    (is (:ok result))
    (is (str/includes? future "^String renamed"))
    (is (str/includes? future "^{:tag String} x"))
    (is (str/includes? future "#_ignored 1 2N"))))
