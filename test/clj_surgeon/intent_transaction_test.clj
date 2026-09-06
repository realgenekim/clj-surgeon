(ns clj-surgeon.intent-transaction-test
  (:require
   [babashka.fs :as fs]
   [babashka.process :as proc]
   [clj-surgeon.core :as core]
   [clj-surgeon.intent-transaction :as transaction]
   [clj-surgeon.structural-lens :as structural-lens]
   [clojure.edn :as edn]
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

(defn- change-spec
  [changes expected]
  {:changes changes
   :expect expected})

(defn- change
  [id files forms find replacement expected]
  (cond-> {:id id
           :in files
           :find find
           :do [:replace replacement]
           :expect expected}
    forms (assoc :forms forms)))

(defn- valid-source?
  [source]
  (try
    (z/of-string source)
    true
    (catch Exception _ false)))

;; @spec OP-ALG-FORM-COUNT-001
(deftest single-form-cardinality-refusals-report-syntax-units-without-source
  (let [sources {"src/a.clj" "(ns app.a)\n(def values [:old])\n"}
        expected {:intent-count 1 :edit-count 1 :changed-file-count 1}
        cases [{:from ":old :other" :to ":new" :field ":from" :count 2}
               {:from ":old" :to ":new :other" :field ":to" :count 2}
               {:from " \n\t" :to ":new" :field ":from" :count 0}
               {:from ":old" :to " \n\t" :field ":to" :count 0}
               {:from ":old" :to "#_private-marker :new" :field ":to" :count 2}]]
    (doseq [{:keys [from to field count]} cases]
      (testing (str field " with " count " syntax units")
        (let [result (transaction/compile-transaction
                       sources
                       (spec [(intent ["src/a.clj"] from to 1)] expected))]
          (is (= :invalid-intent-form (:error-type result)))
          (is (= field (:field result)))
          (is (= 1 (:expected result)))
          (is (= count (:actual result)))
          (is (= count (:form-count result)))
          (is (= (str field ": one complete form expected; " count " supplied.")
                 (:error result)))
          (is (nil? (:future-sources result))))))
    (doseq [replacement ["; private-marker\n:new" "; private-marker\n"]]
      (testing "detached comments retain their existing explanation"
        (let [result (transaction/compile-transaction
                       sources
                       (spec [(intent ["src/a.clj"] ":old" replacement 1)] expected))]
          (is (= :invalid-intent-form (:error-type result)))
          (is (= ":to must contain exactly one complete form with no detached comments"
                 (:error result)))
          (is (not (contains? result :expected)))
          (is (not (contains? result :actual)))
          (is (nil? (:future-sources result))))))
    (testing "malformed reader input does not invent a cardinality"
      (let [result (transaction/compile-transaction
                     sources
                     (spec [(intent ["src/a.clj"] ":old" "[" 1)] expected))]
        (is (= :invalid-intent-form (:error-type result)))
        (is (= ":to" (:field result)))
        (is (not (contains? result :expected)))
        (is (not (contains? result :actual)))
        (is (not (contains? result :form-count)))
        (is (nil? (:future-sources result)))))
    (doseq [[replacement future]
            [[":new" "(ns app.a)\n(def values [:new])\n"]
             ["(comment :private-marker)"
              "(ns app.a)\n(def values [(comment :private-marker)])\n"]
             ["#_private-marker"
              "(ns app.a)\n(def values [#_private-marker])\n"]]]
      (testing "one complete form retains the existing accepted bytes"
        (let [result (transaction/compile-transaction
                       sources
                       (spec [(intent ["src/a.clj"] ":old" replacement 1)] expected))]
          (is (nil? (:error-type result)))
          (is (= {"src/a.clj" future} (:future-sources result))))))))

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
         {:label "detached comment before before-form"
          :spec (spec [(intent ["src/a.clj"]
                               "; explain\n(old-call x)"
                               "(new-call x)"
                               1)]
                      valid-expect)
          :error :invalid-intent-form}
         {:label "detached comment before replacement"
          :spec (spec [(intent ["src/a.clj"]
                               "(old-call x)"
                               "; explain\n(new-call x)"
                               1)]
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

(deftest compiles-scoped-find-replace-changes-as-one-transaction
  (let [sources
        {"src/layout.clj"
         (str "(ns app.layout)\n\n"
              "(defn ide-shell []\n  [:main :body])\n\n"
              "(defn source-reader-shell [current-location]\n"
              "  [:main :body])\n")
         "src/common.clj"
         (str "(ns app.common)\n\n"
              "(defn stylesheets []\n"
              "  [(views/static \"/a.css\")])\n")}
        result
        (transaction/compile-transaction
          sources
          (change-spec
            [(change :shell-body
                     ["src/layout.clj"]
                     ['ide-shell 'source-reader-shell]
                     ":body"
                     ":body.ide-shell-page"
                     {:matches 2 :each-form 1})
             (change :static-head
                     ["src/common.clj"]
                     ['stylesheets]
                     "views/static"
                     "assets/static"
                     {:matches 1 :each-file 1})]
            {:changes 2 :edits 3 :files 2}))]
    (is (:ok result))
    (is (= 2 (:change-count result)))
    (is (= 3 (:match-count result)))
    (is (= 2 (:changed-file-count result)))
    (is (= [:shell-body :static-head]
           (mapv :id (:changes result))))
    (is (= "(ns app.layout)\n\n(defn ide-shell []\n  [:main :body.ide-shell-page])\n\n(defn source-reader-shell [current-location]\n  [:main :body.ide-shell-page])\n"
           (get-in result [:future-sources "src/layout.clj"])))
    (is (= "(ns app.common)\n\n(defn stylesheets []\n  [(assets/static \"/a.css\")])\n"
           (get-in result [:future-sources "src/common.clj"])))
    (is (every? valid-source? (vals (:future-sources result))))))

(deftest deletes-several-exact-owners-as-one-compiled-change
  (let [source (str "(ns sample.core)\n\n"
                    ";; belongs to alpha\n"
                    "(defn alpha [] :a)\n\n"
                    "(defn beta [] :b)\n\n"
                    "(defn keep-me [] :kept)\n")
        result
        (transaction/compile-transaction
          {"src/sample/core.clj" source}
          (change-spec
            [{:id :delete-obsolete
              :in ["src/sample/core.clj"]
              :forms ['alpha 'beta]
              :do [:delete true]
              :expect {:matches 2 :each-form 1}}]
            {:changes 1 :edits 2 :files 1}))
        receipt (when (:ok result) (transaction/build-receipt result))
        inverse (when receipt
                  (transaction/compile-inverse
                    receipt (:future-sources result)))]
    (is (:ok result))
    (is (= 2 (:match-count result)))
    (is (= :delete (get-in result [:changes 0 :operator])))
    (is (= "(ns sample.core)\n\n(defn keep-me [] :kept)\n"
           (get-in result [:future-sources "src/sample/core.clj"])))
    (is (valid-source? (get-in result [:future-sources "src/sample/core.clj"])))
    (is (not (str/includes?
               (get-in result [:future-sources "src/sample/core.clj"])
               "belongs to alpha")))
    (is (:ok inverse))
    (is (= source
           (get-in inverse [:future-sources "src/sample/core.clj"])))))

(deftest scoped-change-guards-prove-owner-and-file-distribution
  (let [sources
        {"src/a.clj"
         (str "(ns app.a)\n"
              "(defn first-owner [] [:body :body])\n"
              "(defn second-owner [] [:other])\n")
         "src/b.clj"
         "(ns app.b)\n(defn first-owner [] [:other])\n"}
        aggregate
        (change :body
                ["src/a.clj"]
                ['first-owner 'second-owner]
                ":body"
                ":body.page"
                {:matches 2})
        each-form (assoc aggregate :expect {:matches 2 :each-form 1})
        across-files
        (change :body
                ["src/a.clj" "src/b.clj"]
                nil
                ":body"
                ":body.page"
                {:matches 2 :each-file 1})]
    (is (:ok (transaction/compile-transaction
               sources
               (change-spec [aggregate]
                            {:changes 1 :edits 2 :files 1}))))
    (let [result (transaction/compile-transaction
                   sources
                   (change-spec [each-form]
                                {:changes 1 :edits 2 :files 1}))]
      (is (= :change-distribution-mismatch (:error-type result)))
      (is (= :each-form (:distribution result)))
      (is (= {"src/a.clj"
              {'first-owner 2 'second-owner 0}}
             (:actual result)))
      (is (nil? (:future-sources result))))
    (let [result (transaction/compile-transaction
                   sources
                   (change-spec [across-files]
                                {:changes 1 :edits 2 :files 1}))]
      (is (= :change-distribution-mismatch (:error-type result)))
      (is (= :each-file (:distribution result)))
      (is (= {"src/a.clj" 2 "src/b.clj" 0}
             (:actual result)))
      (is (nil? (:future-sources result))))))

(deftest defmethod-owner-addresses-one-name-and-dispatch-without-lines
  (let [source (str "(ns app.render)\n"
                    "(defmulti render :kind)\n"
                    "(defmethod render :card [x] [:card :old x])\n"
                    "(defmethod render [:panel :wide] [x] [:wide :old x])\n"
                    "(defmethod other :card [x] [:other :old x])\n")
        owner {:kind :defmethod :name 'render :dispatch ":card"}
        requested (change :card-method ["src/render.clj"] [owner]
                          ":old" ":new" {:matches 1 :each-form 1})
        result (transaction/compile-transaction
                 {"src/render.clj" source}
                 (change-spec [requested]
                              {:changes 1 :edits 1 :files 1}))]
    (is (:ok result))
    (is (= (str "(ns app.render)\n"
                "(defmulti render :kind)\n"
                "(defmethod render :card [x] [:card :new x])\n"
                "(defmethod render [:panel :wide] [x] [:wide :old x])\n"
                "(defmethod other :card [x] [:other :old x])\n")
           (get-in result [:future-sources "src/render.clj"])))
    (is (= {"src/render.clj" {owner 1}}
           (get-in result [:changes 0 :per-form-counts]))))
  (testing "dispatch structure is exact and duplicate methods refuse"
    (let [owner {:kind :defmethod :name 'render :dispatch "[:panel :wide]"}
          requested (change :wide-method ["src/render.clj"] [owner]
                            ":old" ":new" {:matches 1})
          duplicate-source
          (str "(ns app.render)\n"
               "(defmethod render [:panel :wide] [x] [:first :old x])\n"
               "(defmethod render [:panel :wide] [x] [:second :old x])\n")
          duplicate (transaction/compile-transaction
                      {"src/render.clj" duplicate-source}
                      (change-spec [requested]
                                   {:changes 1 :edits 1 :files 1}))
          malformed (transaction/compile-transaction
                      {"src/render.clj" duplicate-source}
                      (change-spec [(assoc-in requested [:forms 0 :dispatch]
                                              "[:panel")]
                                   {:changes 1 :edits 1 :files 1}))]
      (is (= :change-owner-mismatch (:error-type duplicate)))
      (is (= 2 (:actual-count duplicate)))
      (is (nil? (:future-sources duplicate)))
      (is (= :invalid-intent-form (:error-type malformed)))
      (is (= ":forms dispatch" (:field malformed)))
      (is (nil? (:future-sources malformed))))))

(deftest staged-future-sources-become-the-hashed-reversible-transaction
  (let [original "(ns app.core)\n(defn render [] [:old])\n"
        compiled (transaction/compile-transaction
                   {"src/app/core.clj" original}
                   (change-spec
                     [(change :render ["src/app/core.clj"] ['render]
                              ":old" ":new" {:matches 1})]
                     {:changes 1 :edits 1 :files 1}))
        formatted "(ns app.core)\n\n(defn render\n  []\n  [:new])\n"
        prepared (transaction/with-future-sources
                   compiled {"src/app/core.clj" formatted})
        receipt (transaction/build-receipt prepared)
        inverse (transaction/compile-inverse
                  receipt (:future-sources prepared))]
    (is (:ok prepared))
    (is (= formatted (get-in prepared [:future-sources "src/app/core.clj"])))
    (is (= 1 (get-in prepared [:format :changed-file-count])))
    (is (= 1 (count (get-in prepared [:files 0 :edits]))))
    (is (true? (get-in prepared [:files 0 :edits 0 :raw])))
    (is (:ok inverse))
    (is (= original (get-in inverse [:future-sources "src/app/core.clj"])))
    (doseq [invalid [{}
                     {"src/app/core.clj" formatted "src/extra.clj" "(ns extra)"}
                     {"src/app/core.clj" "(defn broken ["}]]
      (let [result (transaction/with-future-sources compiled invalid)]
        (is (not (:ok result)))
        (is (nil? (:committed result)))))))

(deftest scoped-changes-refuse-every-field-and-owner-error-as-data
  (let [sources
        {"src/a.clj"
         (str "(ns app.a)\n"
              "(defn one [] [:body])\n"
              "(defn two [] [:body])\n")
         "src/duplicate.clj"
         (str "(ns app.duplicate)\n"
              "(defn repeated [] :first)\n"
              "(defn repeated [] :second)\n")}
        valid (change :body ["src/a.clj"] ['one]
                      ":body" ":body.page" {:matches 1})
        valid-expect {:changes 1 :edits 1 :files 1}
        cases
        [{:label "empty changes"
          :spec (change-spec [] {:changes 0 :edits 0 :files 0})
          :error :invalid-changes}
         {:label "legacy and scoped modes cannot mix"
          :spec {:intents [(intent ["src/a.clj"] ":body" ":body.page" 2)]
                 :changes [valid]
                 :expect valid-expect}
          :error :mixed-transaction-modes}
         {:label "unknown change key"
          :spec (change-spec [(assoc valid :where :anywhere)] valid-expect)
          :error :unknown-change-arguments}
         {:label "duplicate change id"
          :spec (change-spec [valid valid]
                             {:changes 2 :edits 2 :files 1})
          :error :duplicate-change-id}
         {:label "invalid owner list"
          :spec (change-spec [(assoc valid :forms [])] valid-expect)
          :error :invalid-change-forms}
         {:label "unknown owner"
          :spec (change-spec [(assoc valid :forms ['missing])] valid-expect)
          :error :change-owner-mismatch}
         {:label "duplicate owner definition"
          :spec (change-spec [(change :duplicate
                                      ["src/duplicate.clj"]
                                      ['repeated]
                                      ":first" ":only"
                                      {:matches 1})]
                             valid-expect)
          :error :change-owner-mismatch}
         {:label "find must be one source form"
          :spec (change-spec [(assoc valid :find ":body :other")]
                             valid-expect)
          :error :invalid-intent-form}
         {:label "valid Hiccup plus a trailing parent delimiter"
          :spec (change-spec [(assoc valid :find "[:body :child]]")]
                             valid-expect)
          :error :invalid-intent-form}
         {:label "unsupported operator"
          :spec (change-spec [(assoc valid :do [:delete])] valid-expect)
          :error :unsupported-change-operator}
         {:label "replacement must be one source form"
          :spec (change-spec [(assoc valid :do [:replace ":a :b"])]
                             valid-expect)
          :error :invalid-intent-form}
         {:label "missing positive match expectation"
          :spec (change-spec [(assoc valid :expect {})] valid-expect)
          :error :invalid-change-expectation}
         {:label "each-form requires owners"
          :spec (change-spec [(-> valid
                                  (dissoc :forms)
                                  (assoc :expect {:matches 1 :each-form 1}))]
                             valid-expect)
          :error :invalid-change-expectation}
         {:label "unknown expectation key"
          :spec (change-spec [(assoc valid :expect {:matches 1 :each 1})]
                             valid-expect)
          :error :unknown-change-expectation-arguments}
         {:label "lossless no-op"
          :spec (change-spec [(assoc valid :do [:replace ":body"])]
                             valid-expect)
          :error :no-op-intent}
         {:label "aggregate change mismatch"
          :spec (change-spec [valid] (assoc valid-expect :changes 2))
          :error :transaction-expectation-mismatch}]]
    (doseq [{:keys [label spec error]} cases]
      (testing label
        (let [result (transaction/compile-transaction sources spec)]
          (is (= error (:error-type result)))
          (is (nil? (:future-sources result))))))
    (testing "parse refusals name the change, field, and complete-input failure"
      (let [incomplete (transaction/compile-transaction
                         sources
                         (change-spec [(assoc valid :find "(defn incomplete")]
                                      valid-expect))
            trailing-parent (transaction/compile-transaction
                              sources
                              (change-spec [(assoc valid :find "[:body :child]]")]
                                           valid-expect))]
        (is (= :invalid-intent-form (:error-type incomplete)))
        (is (= 0 (:change-index incomplete)))
        (is (= :body (:change-id incomplete)))
        (is (= ":find" (:field incomplete)))
        (is (= :invalid-intent-form (:error-type trailing-parent)))
        (is (str/includes? (:error trailing-parent) "Unmatched delimiter: ]"))
        (is (nil? (:future-sources trailing-parent)))))))

(deftest refuses-overlapping-intents-before-producing-future-source
  (let [sources
        {"src/a.clj"
         "(ns app.a)\n(def value (outer (inner x)))\n"}
        result
        (transaction/compile-transaction
          sources
          (change-spec
            [(change :outer ["src/a.clj"] ['value]
                     "(outer (inner x))" "(outer (new x))"
                     {:matches 1 :each-file 1 :each-form 1})
             (change :inner ["src/a.clj"] ['value]
                     "(inner x)" "(new x)"
                     {:matches 1 :each-file 1 :each-form 1})]
            {:changes 2 :edits 2 :files 1}))]
    (is (= :overlapping-intents (:error-type result)))
    (is (= "src/a.clj" (:file result)))
    (is (= #{0 1} (set (:intent-indexes result))))
    (is (= [:outer :inner] (:change-ids result)))
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

(deftest change-preview-reads-each-file-once-and-omits-full-future-source
  (let [reads (atom [])
        canonical-file (.getCanonicalPath (java.io.File. "src/a.clj"))
        sources {canonical-file "(ns app.a)\n(def value [(old x) :body])\n"}
        change-spec
        (spec [(intent ["src/a.clj"] "(old x)" "(new x)" 1)
               (intent ["src/a.clj"] ":body" ":body.page" 1)]
              {:intent-count 2 :edit-count 2 :changed-file-count 1})]
    (with-redefs [clojure.core/slurp
                  (fn [file]
                    (swap! reads conj file)
                    (get sources file))]
      (let [result (transaction/plan-change {:op :change :spec change-spec})]
        (is (:ok result))
        (is (= [canonical-file] @reads))
        (is (nil? (:future-sources result)))
        (is (= 2 (:match-count result)))
        (is (= 2 (count (get-in result [:files 0 :edits]))))
        (is (every? #(not-any? (set (keys %)) [:before :after])
                    (get-in result [:files 0 :edits]))))))

  (testing "unknown CLI arguments refuse before source reads"
    (let [read? (atom false)
          result (with-redefs [clojure.core/slurp
                               (fn [_]
                                 (reset! read? true)
                                 "")]
                   (transaction/plan-change
                     {:op :change
                      :spec {:intents [] :expect {}}
                      :surprise true}))]
      (is (= :unknown-arguments (:error-type result)))
      (is (false? @read?))))

  (testing "malformed specs refuse before source reads"
    (let [read? (atom false)
          malformed
          [{:spec {:intents {} :expect {}}
            :error :invalid-intents}
           {:spec {:intents [(assoc (intent ["src/a.clj"]
                                            "(old x)" "(new x)" 1)
                                    :surprise true)]
                   :expect {:intent-count 1 :edit-count 1
                            :changed-file-count 1}}
            :error :unknown-intent-arguments}
           {:spec {:intents [(intent "src/a.clj"
                                     "(old x)" "(new x)" 1)]
                   :expect {:intent-count 1 :edit-count 1
                            :changed-file-count 1}}
            :error :invalid-files}
           {:spec {:intents [(intent ["src/a.clj"]
                                     "(old x)" "(new x)" 1)]
                   :expect {:intent-count 1 :edit-count 1
                            :changed-file-count 1}
                   :surprise true}
            :error :unknown-transaction-arguments}]]
      (with-redefs [clojure.core/slurp
                    (fn [_]
                      (reset! read? true)
                      "")]
        (doseq [{:keys [spec error]} malformed]
          (is (= error
                 (:error-type
                   (transaction/plan-change {:op :change :spec spec}))))))
      (is (false? @read?)))))

(deftest change-preview-canonicalizes-one-physical-file-across-intents
  (let [temp-dir (fs/create-temp-dir {:prefix "intent-path-alias-"})
        canonical-file (str (fs/path temp-dir "sample.clj"))
        aliased-file (str (fs/path temp-dir "." "sample.clj"))
        source "(ns sample)\n(def value [(old x) :body])\n"
        change-spec
        (spec [(intent [canonical-file] "(old x)" "(new x)" 1)
               (intent [aliased-file] ":body" ":body.page" 1)]
              {:intent-count 2 :edit-count 2 :changed-file-count 1})]
    (try
      (spit canonical-file source)
      (let [result (transaction/plan-change {:op :change :spec change-spec})]
        (is (:ok result))
        (is (= 1 (count (:files result))))
        (is (= (.getCanonicalPath (java.io.File. canonical-file))
               (get-in result [:files 0 :file])))
        (is (= 2 (get-in result [:files 0 :match-count])))
        (is (= source (slurp canonical-file))))
      (finally
        (fs/delete-tree temp-dir)))))

(deftest assoc-entry-matches-semantic-maps-and-preserves-comments
  (let [source
        (str "(ns demo)\n"
             "(defn assertions []\n"
             "  [(is (= {:a 1 :b 2} actual-a))\n"
             "   (is (= {:a 1\n"
             "           ;; This contract comment must remain attached.\n"
             "           :b 2} actual-b))])\n")
        spec
        {:changes
         [{:id :add-status
           :in ["src/demo.clj"]
           :forms ['assertions]
           :find "{:a 1 :b 2}"
           :do [:assoc-entry {:key ":status" :value ":ready"}]
           :expect {:matches 2 :each-file 2}}]
         :expect {:changes 1 :edits 2 :files 1}}
        result (transaction/compile-transaction {"src/demo.clj" source} spec)
        future (get-in result [:future-sources "src/demo.clj"])]
    (is (:ok result))
    (is (= 2 (:match-count result)))
    (is (str/includes? future "{:a 1 :b 2 :status :ready}"))
    (is (str/includes? future
                       ";; This contract comment must remain attached."))
    (is (str/includes? future ":b 2 :status :ready}"))))

(deftest assoc-entry-semantic-ancestor-selects-one-comment-bearing-map
  (let [source
        (str "(ns demo)\n"
             "(defn assertions []\n"
             "  [(is (= {:a 1 :b 2} actual-a))\n"
             "   (is (= {:a 1\n"
             "           ;; Keep this exact explanation.\n"
             "           :b 2} actual-b))])\n")
        change
        {:id :add-status
         :in ["src/demo.clj"]
         :forms ['assertions]
         :find "{:a 1 :b 2}"
         :inside "(is (= {:a 1 :b 2} actual-b))"
         :do [:assoc-entry {:key ":status" :value ":ready"}]
         :expect {:matches 1}}
        result
        (transaction/compile-transaction
          {"src/demo.clj" source}
          {:changes [change]
           :expect {:changes 1 :edits 1 :files 1}})
        future (get-in result [:future-sources "src/demo.clj"])]
    (is (:ok result))
    (is (str/includes? future "{:a 1 :b 2} actual-a"))
    (is (str/includes? future ";; Keep this exact explanation."))
    (is (str/includes? future ":b 2 :status :ready} actual-b")))
  (let [source (str "(ns demo)\n"
                    "(defn assertions [] [{:a 1} {:a 1}])\n")
        result
        (transaction/compile-transaction
          {"src/demo.clj" source}
          {:changes
           [{:id :ambiguous
             :in ["src/demo.clj"]
             :forms ['assertions]
             :find "{:a 1}"
             :do [:assoc-entry {:key ":status" :value ":ready"}]
             :expect {:matches 1}}]
           :expect {:changes 1 :edits 1 :files 1}})]
    (is (= :expect-count-mismatch (:error-type result)))
    (is (nil? (:future-sources result)))))

(deftest assoc-entry-refuses-an-existing-key-before-producing-source
  (let [source (str "(ns demo)\n"
                    "(defn assertion [] {:a 1 :status :old})\n")
        result
        (transaction/compile-transaction
          {"src/demo.clj" source}
          {:changes
           [{:id :duplicate-status
             :in ["src/demo.clj"]
             :forms ['assertion]
             :find "{:a 1 :status :old}"
             :do [:assoc-entry {:key ":status" :value ":ready"}]
             :expect {:matches 1}}]
           :expect {:changes 1 :edits 1 :files 1}})]
    (is (= :map-key-already-present (:error-type result)))
    (is (nil? (:future-sources result)))))

(def ^:private two-owner-binding-analysis
  {:locals
   [{:row 2 :col 21 :end-row 2 :end-col 28 :name 'sort-by :id 1}
    {:row 3 :col 22 :end-row 3 :end-col 29 :name 'sort-by :id 2}]
   :local-usages
   [{:row 2 :col 35 :end-row 2 :end-col 42 :name 'sort-by :id 1}
    {:row 2 :col 54 :end-row 2 :end-col 61 :name 'sort-by :id 1}
    {:row 3 :col 39 :end-row 3 :end-col 46 :name 'sort-by :id 2}]
   :keywords
   [{:row 2 :col 21 :end-row 2 :end-col 28
     :name "sort-by" :keys-destructuring true}
    {:row 3 :col 22 :end-row 3 :end-col 29
     :name "sort-by" :keys-destructuring true}]})

(defn- binding-rename-spec
  [matches]
  {:changes
   [{:id :rename-sort-binding
     :in ["src/demo.clj"]
     :forms ['feed 'table]
     :do [:rename-binding
          {:from 'sort-by
           :to 'sort-field
           :preserve-external-key true}]
     :expect {:matches matches :each-form 1}}]
   :expect {:changes 1 :edits matches :files 1}})

(deftest binding-rename-preserves-external-keys-across-several-owners
  (let [source (str "(ns demo)\n"
                    "(defn feed [{:keys [sort-by] :or {sort-by :score}}] [sort-by :sort-by clojure.core/sort-by])\n"
                    "(defn table [{:keys [sort-by]}] (name sort-by))\n")
        result (binding [transaction/*binding-analyzer*
                         (fn [_ _] two-owner-binding-analysis)]
                 (transaction/compile-transaction
                   {"src/demo.clj" source}
                   (binding-rename-spec 5)))]
    (is (:ok result))
    (is (= 5 (:match-count result)))
    (is (= 2 (get-in result [:changes 0 :binding-count])))
    (is (= (str "(ns demo)\n"
                "(defn feed [{:keys [] :or {sort-field :score} sort-field :sort-by}] [sort-field :sort-by clojure.core/sort-by])\n"
                "(defn table [{:keys [] sort-field :sort-by}] (name sort-field))\n")
           (get-in result [:future-sources "src/demo.clj"])))
    (is (str/includes?
          (get-in result [:future-sources "src/demo.clj"])
          "clojure.core/sort-by"))))

(deftest binding-rename-refuses-stale-count-ambiguity-and-capture
  (let [source (str "(ns demo)\n"
                    "(defn feed [{:keys [sort-by] :or {sort-by :score}}] [sort-by :sort-by clojure.core/sort-by])\n"
                    "(defn table [{:keys [sort-by]}] (name sort-by))\n")
        compile-with
        (fn [analysis matches]
          (binding [transaction/*binding-analyzer* (fn [_ _] analysis)]
            (transaction/compile-transaction
              {"src/demo.clj" source}
              (binding-rename-spec matches))))]
    (is (= :expect-count-mismatch
           (:error-type (compile-with two-owner-binding-analysis 6))))
    (is (= :binding-identity-ambiguous
           (:error-type
             (compile-with
               (update two-owner-binding-analysis :locals conj
                       {:row 2 :col 54 :end-row 2 :end-col 61
                        :name 'sort-by :id 3})
               5))))
    (is (= :binding-capture-risk
           (:error-type
             (compile-with
               (update two-owner-binding-analysis :locals conj
                       {:row 2 :col 54 :end-row 2 :end-col 64
                        :name 'sort-field :id 3})
               5))))))

(deftest binding-rename-refuses-comment-sensitive-destructuring
  (let [source (str "(ns demo)\n"
                    "(defn feed [{:keys [sort-by ; public key\n"
                    "                     ]}] sort-by)\n"
                    "(defn table [{:keys [sort-by]}] sort-by)\n")
        analysis
        {:locals
         [{:row 2 :col 21 :end-row 2 :end-col 28 :name 'sort-by :id 1}
          {:row 4 :col 22 :end-row 4 :end-col 29 :name 'sort-by :id 2}]
         :local-usages
         [{:row 3 :col 26 :end-row 3 :end-col 33 :name 'sort-by :id 1}
          {:row 4 :col 32 :end-row 4 :end-col 39 :name 'sort-by :id 2}]}
        result (binding [transaction/*binding-analyzer* (fn [_ _] analysis)]
                 (transaction/compile-transaction
                   {"src/demo.clj" source}
                   (binding-rename-spec 4)))]
    (is (= :comment-sensitive-binding (:error-type result)))
    (is (nil? (:future-sources result)))))

(deftest guarded-sibling-insertion-preserves-collection-style
  (doseq [[label source find operator inserted expected]
          [[:vector "(ns sample)\n(def xs [:a :c])\n"
            ":a" :insert-right [":b"]
            "(ns sample)\n(def xs [:a :b :c])\n"]
           [:set "(ns sample)\n(def xs #{:a :c})\n"
            ":c" :insert-left [":b"]
            "(ns sample)\n(def xs #{:a :b :c})\n"]
           [:map "(ns sample)\n(def xs {:a 1 :c 3})\n"
            ":c" :insert-left [":b" "2"]
            "(ns sample)\n(def xs {:a 1 :b 2 :c 3})\n"]
           [:list "(ns sample)\n(defn run [] (f a c))\n"
            "a" :insert-right ["b"]
            "(ns sample)\n(defn run [] (f a b c))\n"]
           [:multiline "(ns sample)\n(def xs [:a\n         :c])\n"
            ":c" :insert-left [":b"]
            "(ns sample)\n(def xs [:a\n         :b\n         :c])\n"]
           [:boundary "(ns sample)\n(def xs [:a])\n"
            ":a" :insert-right [":b" ":c"]
            "(ns sample)\n(def xs [:a :b :c])\n"]]]
    (testing (name label)
      (let [owner (if (= :list label) 'run 'xs)
            spec {:changes [{:id :insert
                             :in ["src/sample.clj"]
                             :forms [owner]
                             :find find
                             :do [operator inserted]
                             :expect {:matches 1}}]
                  :expect {:changes 1 :edits 1 :files 1}}
            result (transaction/compile-transaction
                     {"src/sample.clj" source} spec)]
        (is (:ok result))
        (is (= expected (get-in result [:future-sources "src/sample.clj"])))
        (is (valid-source? expected))))))

(deftest guarded-top-level-insertion-targets-one-owner-without-repeating-it
  (let [file "src/sample.clj"
        source (str "(ns sample)\n\n"
                    "(defn alpha [] :a)\n\n"
                    "(defn omega [] :z)\n")
        expected (str "(ns sample)\n\n"
                      "(defn alpha [] :a)\n\n"
                      "(defn beta [] :b)\n\n"
                      "(defn gamma [] :c)\n\n"
                      "(defn omega [] :z)\n")
        compiled
        (transaction/compile-transaction
          {file source}
          {:changes [{:id :add-neighbors
                      :in [file]
                      :forms ['alpha]
                      :do [:insert-right
                           ["(defn beta [] :b)"
                            "(defn gamma [] :c)"]]
                      :expect {:matches 1 :each-file 1 :each-form 1}}]
           :expect {:changes 1 :edits 1 :files 1}})
        receipt (when (:ok compiled) (transaction/build-receipt compiled))
        inverse (when receipt
                  (transaction/compile-inverse
                    receipt (:future-sources compiled)))]
    (is (:ok compiled))
    (is (= expected (get-in compiled [:future-sources file])))
    (is (= 1 (:match-count receipt)))
    (is (:ok inverse))
    (is (= {file source} (:future-sources inverse)))))

;; @spec MCP-OP-INSERT-001
(deftest owner-boundary-insertion-is-disjoint-from-interior-replacements
  ;; Minimized from the 2026-09-02 bridge4-page-html field refusal.
  (let [file "src/sample.clj"
        source (str "(ns sample)\n"
                    "(defn bridge4-page-html []\n"
                    "  (str (cancel-js) (over-cancel-buttons)))\n")
        expected (str "(ns sample)\n"
                      "(def cancel-js-def :installed)\n"
                      "(defn bridge4-page-html []\n"
                      "  (str (new-cancel-js) (new-over-cancel-buttons)))\n")
        result
        (transaction/compile-transaction
          {file source}
          {:changes
           [{:id :cancel-js-def
             :in [file]
             :forms ['bridge4-page-html]
             :find (str "(defn bridge4-page-html []\n"
                        "  (str (cancel-js) (over-cancel-buttons)))")
             :do [:insert-left ["(def cancel-js-def :installed)"]]
             :expect {:matches 1}}
            {:id :cancel-js
             :in [file]
             :forms ['bridge4-page-html]
             :find "(cancel-js)"
             :do [:replace "(new-cancel-js)"]
             :expect {:matches 1}}
            {:id :over-cancel-buttons
             :in [file]
             :forms ['bridge4-page-html]
             :find "(over-cancel-buttons)"
             :do [:replace "(new-over-cancel-buttons)"]
             :expect {:matches 1}}]
           :expect {:changes 3 :edits 3 :files 1}})]
    (is (:ok result))
    (is (= expected (get-in result [:future-sources file])))
    (is (valid-source? expected))))

;; @spec MCP-OP-INSERT-002
;; @spec MCP-OP-INSERT-006
(deftest boundary-insertion-still-refuses-ambiguous-overlaps
  (let [file "src/sample.clj"
        source (str "(ns sample)\n"
                    "(defn owner [] (str \"a\" \"b\"))\n")
        insertion {:id :insert
                   :in [file]
                   :forms ['owner]
                   :find "(defn owner [] (str \"a\" \"b\"))"
                   :do [:insert-left ["(def before :x)"]]
                   :expect {:matches 1}}
        cases
        [[:whole-owner
          [insertion
           {:id :whole-owner :in [file] :forms ['owner]
            :find "(defn owner [] (str \"a\" \"b\"))"
            :do [:replace "(defn owner [] :replaced)"] :expect {:matches 1}}]]
         [:delete-owner
          [insertion
           {:id :delete-owner :in [file] :forms ['owner]
            :do [:delete true] :expect {:matches 1}}]]
         [:same-boundary
          [insertion (assoc insertion :id :second-insertion)]]
         [:non-owner-containing-boundary
          [{:id :nested-insert :in [file] :forms ['owner]
            :find "\"b\"" :do [:insert-left ["(side-effect)"]]
            :expect {:matches 1}}
           {:id :containing-span :in [file] :forms ['owner]
            :find "(str \"a\" \"b\")" :do [:replace "(str \"new\")"]
            :expect {:matches 1}}]]]]
    (doseq [[label changes] cases]
      (testing (name label)
        (let [sources {file source}
              result (transaction/compile-transaction
                       sources
                       {:changes changes
                        :expect {:changes 2 :edits 2 :files 1}})]
          (is (= :overlapping-intents (:error-type result)))
          (is (nil? (:future-sources result)))
          (is (not (true? (:write-authority result))))
          (is (= source (get sources file))))))))

;; @spec MCP-OP-INSERT-003
;; @spec MCP-OP-INSERT-004
;; @spec MCP-OP-INSERT-005
(deftest insertion-gap-uses-opposite-newline-style-before-space-fallback
  ;; Minimized from the 2026-09-02 channel.clj long string-literal insertion.
  (let [long-literal (str "\"" (apply str (repeat 450 "x")) "\"")
        source (str "(ns sample)\n"
                    "(defn render []\n"
                    "  (str\n"
                    "    \"prefix\"\n"
                    "    " long-literal "))\n")
        expected (str "(ns sample)\n"
                      "(defn render []\n"
                      "  (str\n"
                      "    \"prefix\"\n"
                      "    " long-literal "\n"
                      "    (when enabled? (render-extra))))\n")
        result
        (transaction/compile-transaction
          {"src/sample.clj" source}
          {:changes [{:id :add-render-extra
                      :in ["src/sample.clj"]
                      :forms ['render]
                      :find long-literal
                      :do [:insert-right ["(when enabled? (render-extra))"]]
                      :expect {:matches 1}}]
           :expect {:changes 1 :edits 1 :files 1}})]
    (is (:ok result))
    (is (= expected (get-in result [:future-sources "src/sample.clj"])))
    (is (valid-source? expected))))

(deftest guarded-sibling-insertion-refuses-comment-bearing-or-invalid-gaps
  (doseq [[label operator]
          [[:before-comment :insert-left]
           [:after-comment :insert-right]]]
    (testing (name label)
      (let [source "(ns sample)\n(def xs [:a\n ;; belongs to :c\n :c])\n"
            find (if (= :insert-left operator) ":c" ":a")
            result
            (transaction/compile-transaction
              {"src/sample.clj" source}
              {:changes [{:id :ambiguous
                          :in ["src/sample.clj"]
                          :forms ['xs]
                          :find find
                          :do [operator [":b"]]
                          :expect {:matches 1}}]
               :expect {:changes 1 :edits 1 :files 1}})]
        (is (= :ambiguous-insertion-gap (:error-type result)))
        (is (= :ambiguous (:change-id result)))
        (is (= "src/sample.clj" (:file result))))))
  (doseq [[operator inserted]
          [[:insert-left []]
           [:insert-right [" "]]
           [:insert-left ["(:broken"]]]]
    (let [result
          (transaction/compile-transaction
            {"src/sample.clj" "(ns sample)\n(def xs [:a])\n"}
            {:changes [{:id :invalid
                        :in ["src/sample.clj"]
                        :forms ['xs]
                        :find ":a"
                        :do [operator inserted]
                        :expect {:matches 1}}]
             :expect {:changes 1 :edits 1 :files 1}})]
      (is (contains? #{:unsupported-change-operator :invalid-intent-form}
                     (:error-type result))))))

(deftest namespace-owner-is-an-exact-structural-scope
  (let [sources {"src/app.clj"
                 (str "(ns app.core\n"
                      "  (:require [legacy.api :as legacy]))\n"
                      "(defn use-api [] [legacy.api :as legacy])\n")}
        change {:id :namespace-require
                :in ["src/app.clj"]
                :owner {:kind :namespace :name 'app.core}
                :find "[legacy.api :as legacy]"
                :do [:replace "[current.api :as current]"]
                :expect {:matches 1 :each-file 1}}
        result (transaction/compile-transaction
                 sources
                 (change-spec [change]
                              {:changes 1 :edits 1 :files 1}))]
    (is (:ok result))
    (is (= (str "(ns app.core\n"
                "  (:require [current.api :as current]))\n"
                "(defn use-api [] [legacy.api :as legacy])\n")
           (get-in result [:future-sources "src/app.clj"])))
    (is (= {:kind :namespace :name 'app.core}
           (get-in result [:intents 0 :owner])))
    (let [inferred (assoc change :owner {:kind :namespace})
          inferred-result
          (transaction/compile-transaction
            sources
            (change-spec [inferred]
                         {:changes 1 :edits 1 :files 1}))]
      (is (:ok inferred-result) (pr-str inferred-result))
      (is (= (get-in result [:future-sources "src/app.clj"])
             (get-in inferred-result [:future-sources "src/app.clj"]))))
    (let [ambiguous-sources
          {"src/app.clj"
           (str "(ns app.core (:require [legacy.api :as legacy]))\n"
                "(ns app.second (:require [legacy.api :as legacy]))\n")}
          inferred (assoc change :owner {:kind :namespace})
          inferred-result
          (transaction/compile-transaction
            ambiguous-sources
            (change-spec [inferred]
                         {:changes 1 :edits 1 :files 1}))]
      (is (= :change-owner-mismatch (:error-type inferred-result)))
      (is (nil? (:future-sources inferred-result))))
    (doseq [[label invalid error]
            [["wrong namespace"
              (assoc-in change [:owner :name] 'app.other)
              :change-owner-mismatch]
             ["unsupported owner kind"
              (assoc-in change [:owner :kind] :file)
              :invalid-change-owner]
             ["owner and forms are mutually exclusive"
              (assoc change :forms ['use-api])
              :ambiguous-change-owner]]]
      (testing label
        (let [refusal (transaction/compile-transaction
                        sources
                        (change-spec [invalid]
                                     {:changes 1 :edits 1 :files 1}))]
          (is (= error (:error-type refusal)))
          (is (nil? (:future-sources refusal))))))))

(deftest intent-order-does-not-change-compiled-future-files
  (let [sources {"src/a.clj" "(ns app.a)\n(def value [(old x) :body])\n"}
        first-intent (intent ["src/a.clj"] "(old x)" "(new x)" 1)
        second-intent (intent ["src/a.clj"] ":body" ":body.page" 1)
        expected {:intent-count 2 :edit-count 2 :changed-file-count 1}
        forward (transaction/compile-transaction
                  sources (spec [first-intent second-intent] expected))
        reverse (transaction/compile-transaction
                  sources (spec [second-intent first-intent] expected))]
    (is (:ok forward))
    (is (:ok reverse))
    (is (= (:future-sources forward) (:future-sources reverse)))
    (is (= (mapv :result-hash (:files forward))
           (mapv :result-hash (:files reverse))))))

(defn- small-permutations
  [values]
  (if (empty? values)
    [[]]
    (vec
      (mapcat
        (fn [value]
          (map #(into [value] %)
               (small-permutations (vec (remove #{value} values)))))
        values))))

(defn- compiler-shaped-effect-transaction
  [file-records]
  (let [files
        (mapv
          (fn [{:keys [file source future edits]}]
            {:file file
             :source-hash (structural-lens/source-hash source)
             :result-hash (structural-lens/source-hash future)
             :match-count (count edits)
             :edits edits})
          file-records)
        effect-count (reduce + (map :match-count files))
        changed-file-count (count (filter (comp pos? :match-count) files))]
    {:ok true
     :operation :change
     :transaction-version 1
     :intent-count 1
     :match-count effect-count
     :changed-file-count changed-file-count
     :intents [{}]
     :files files
     :diff "synthetic compiler witness"
     :original-sources (into {} (map (juxt :file :source) file-records))
     :future-sources (into {} (map (juxt :file :future) file-records))
     :validated {:whole-files-parsed true
                 :file-count (count files)}}))

(deftest independent-change-permutations-compile-and-invert-identically
  (let [sources {"src/a.clj"
                 "(ns app.a)\n(def value [(old x) :body :tail])\n"
                 "src/b.clj"
                 "(ns app.b)\n(def view {:status :idle :untouched 42})\n"}
        intents [(intent ["src/a.clj"] "(old x)" "(new x)" 1)
                 (intent ["src/a.clj"] ":body" ":body.page" 1)
                 (intent ["src/b.clj"] ":idle" ":ready" 1)]
        expected {:intent-count 3 :edit-count 3 :changed-file-count 2}
        compiled
        (mapv #(transaction/compile-transaction
                 sources (spec % expected))
              (small-permutations intents))
        future-sources (mapv :future-sources compiled)]
    (is (= 6 (count compiled)))
    (is (every? :ok compiled))
    (is (apply = future-sources)
        "all independent intent orders materialize identical bytes")
    (is (every? #(str/includes? (get % "src/a.clj") ":tail")
                future-sources)
        "unselected source survives every permutation")
    (is (every? #(str/includes? (get % "src/b.clj") ":untouched 42")
                future-sources)
        "unselected map entries survive every permutation")
    (doseq [plan compiled]
      (let [receipt (transaction/build-receipt plan)
            inverse (transaction/compile-inverse
                      receipt (:future-sources plan))]
        (is (:ok inverse))
        (is (= sources (:future-sources inverse)))
        (is (every? valid-source? (vals (:future-sources plan))))
        (is (every? valid-source? (vals (:future-sources inverse))))))))

(deftest canonical-effect-identity-is-exact-and-permutation-invariant
  ;; @spec MCP-OP-EDIT-028
  ;; @spec MCP-OP-EDIT-029
  (let [sources {"/work/project/src/a.clj"
                 "(ns app.a)\n(def value [(old x) :body :tail])\n"
                 "/work/project/src/b.clj"
                 "(ns app.b)\n(def view {:status :idle :untouched 42})\n"}
        intents [(intent ["/work/project/src/a.clj"] "(old x)" "(new x)" 1)
                 (intent ["/work/project/src/a.clj"] ":body" ":body.page" 1)
                 (intent ["/work/project/src/b.clj"] ":idle" ":ready" 1)]
        expected {:intent-count 3 :edit-count 3 :changed-file-count 2}
        identities
        (mapv
          (fn [ordered-intents]
            (->> (transaction/compile-transaction
                   sources (spec ordered-intents expected))
                 (transaction/canonical-effect-identity "/work/project")))
          (small-permutations intents))
        expected-projection
        [:canonical-effect/v1
         [[:file
           "src/a.clj"
           "688cf7aeedceb62b4d0893d499f192196a4e24116e191f268cb5f2a86ff0bfa5"
           "3d6cb4ed0668c0bb505413bd161120382543783adfda6f04063c0f45a61fd8d1"
           [[:effect :replace [0 1 2 0] 7 9 nil "(old x)" "(new x)"]
            [:effect :replace [0 1 2 1] 10 10 nil ":body" ":body.page"]]]
          [:file
           "src/b.clj"
           "3aeebe815e650d182cf9e581d528579b46ba46866f6d0d54b918668d2b21f463"
           "fd6e367c72326dffcdd3e7b3731725b6b43c18cb0f21409edc07a4a78d55cc70"
           [[:effect :replace [0 1 2 1] 8 8 nil ":idle" ":ready"]]]]
         2
         3]]
    (is (= 6 (count identities)))
    (is (apply = identities)
        "caller order cannot change an already-proven disjoint effect set")
    (is (= {:version 1
            :sha256 "83ec6c5417d81d5736dd15799a40a01999a6356b14bff6a1e6e5fe5b2fdc87c2"
            :files 2
            :effects 3
            :projection expected-projection}
           (first identities))
        "the private identity retains exact source strings and resolved addresses")))

(deftest canonical-effect-identity-binds-lossless-effect-provenance
  (let [source "(ns app.a)\n(def value [(old x) (gone)])\n"
        future "(ns app.a)\n(def value [(a) (new x)])\n"
        source-hash (structural-lens/source-hash source)
        result-hash (structural-lens/source-hash future)
        compiled
        (compiler-shaped-effect-transaction
          [{:file "/work/project/src/a.clj"
            :source source
            :future future
            :edits
            [{:path [0 3]
              :address {:preorder 9}
              :end-preorder 10
              :offset 30
              :raw true
              :delete true
              :before "(gone)"
              :after ""}
             {:path [0 1]
              :address {:preorder 2}
              :end-preorder 2
              :offset 10
              :raw true
              :insert-side :insert-left
              :before ""
              :after "(a) "}
             {:path [0 2]
              :address {:preorder 5}
              :end-preorder 6
              :before "(old x)"
              :after "(new x)"}]}])
        projection
        [:canonical-effect/v1
         [[:file
           "src/a.clj"
           source-hash
           result-hash
           [[:effect :insert-left [0 1] 2 2 10 "" "(a) "]
            [:effect :replace [0 2] 5 6 nil "(old x)" "(new x)"]
            [:effect :delete [0 3] 9 10 30 "(gone)" ""]]]]
         1
         3]
        identity
        (transaction/canonical-effect-identity "/work/project" compiled)]
    (is (= {:version 1
            :sha256 (structural-lens/source-hash (pr-str projection))
            :files 1
            :effects 3
            :projection projection}
           identity))
    (doseq [changed
            [(assoc-in compiled [:files 0 :edits 1 :offset] 11)
             (assoc-in compiled [:files 0 :edits 1 :after] "(a)\n")
             (assoc-in compiled [:files 0 :edits 2 :before] " (old x)")
             (assoc-in compiled [:files 0 :edits 2 :path] [0 4])]]
      (is (not= (:sha256 identity)
                (:sha256
                  (transaction/canonical-effect-identity
                    "/work/project" changed)))))))

(deftest canonical-effect-identity-orders-all-four-effect-permutations
  (let [source "(ns app.a)\n(def value [:old])\n"
        future "(ns app.a)\n(def value [:new])\n"
        effects
        [{:path [0 1]
          :address {:preorder 2}
          :end-preorder 2
          :offset 10
          :raw true
          :insert-side :insert-left
          :before ""
          :after "(a) "}
         {:path [0 2]
          :address {:preorder 5}
          :end-preorder 6
          :before "(old x)"
          :after "(new x)"}
         {:path [0 3]
          :address {:preorder 9}
          :end-preorder 10
          :offset 30
          :raw true
          :delete true
          :before "(gone)"
          :after ""}
         {:path [0 4]
          :address {:preorder 12}
          :end-preorder 12
          :before ":old"
          :after ":new"}]
        identities
        (mapv
          (fn [ordered-effects]
            (transaction/canonical-effect-identity
              "/work/project"
              (compiler-shaped-effect-transaction
                [{:file "/work/project/src/a.clj"
                  :source source
                  :future future
                  :edits ordered-effects}])))
          (small-permutations effects))]
    (is (= 24 (count identities)))
    (is (apply = identities))))

(deftest equifinal-effect-decompositions-retain-distinct-identities
  (let [source "(ns app.a)\n(def value (f a b))\n"
        future "(ns app.a)\n(def value (f x y))\n"
        compiled
        (fn [edits]
          (compiler-shaped-effect-transaction
            [{:file "/work/project/src/a.clj"
              :source source
              :future future
              :edits edits}]))
        whole
        (transaction/canonical-effect-identity
          "/work/project"
          (compiled [{:path [0 1]
                      :address {:preorder 2}
                      :end-preorder 8
                      :before "(f a b)"
                      :after "(f x y)"}]))
        parts
        (transaction/canonical-effect-identity
          "/work/project"
          (compiled [{:path [0 1 1]
                      :address {:preorder 4}
                      :end-preorder 4
                      :before "a"
                      :after "x"}
                     {:path [0 1 2]
                      :address {:preorder 6}
                      :end-preorder 6
                      :before "b"
                      :after "y"}]))]
    (is (= 1 (:effects whole)))
    (is (= 2 (:effects parts)))
    (is (not= (:sha256 whole) (:sha256 parts))
        "future hashes alone cannot substitute for lossless effect identity")))

(deftest canonical-effect-identity-does-not-change-receipt-or-inverse
  (let [sources {"/work/project/src/a.clj"
                 "(ns app.a)\n(def value (old x))\n"}
        compiled
        (transaction/compile-transaction
          sources
          (spec [(intent ["/work/project/src/a.clj"]
                         "(old x)" "(new x)" 1)]
                {:intent-count 1 :edit-count 1 :changed-file-count 1}))
        identity
        (transaction/canonical-effect-identity "/work/project" compiled)
        plain-receipt (transaction/build-receipt compiled)
        identity-receipt
        (transaction/build-receipt
          (assoc compiled :canonical-effect-identity identity))]
    (is (= plain-receipt identity-receipt))
    (is (= (:receipt-hash plain-receipt)
           (:receipt-hash identity-receipt)))
    (is (= (:inverse plain-receipt)
           (:inverse identity-receipt)))))

(deftest canonical-effect-identity-refuses-unproven-or-outside-root-input
  (let [effect-a {:path [0 1]
                  :address {:preorder 2}
                  :end-preorder 2
                  :before "a"
                  :after "b"}
        effect-b {:path [0 2]
                  :address {:preorder 4}
                  :end-preorder 4
                  :before "c"
                  :after "d"}
        transaction
        (fn [file-records]
          (compiler-shaped-effect-transaction file-records))]
    (doseq [compiled
            [{:error "overlap" :error-type :overlapping-intents}
             {:ok true :files []}
             (dissoc
               (transaction
                 [{:file "/work/project/src/a.clj"
                   :source "a"
                   :future "b"
                   :edits [effect-a]}])
               :validated)
             (transaction
               [{:file "/other/project/src/a.clj"
                 :source "a"
                 :future "b"
                 :edits [effect-a]}])
             (transaction
               [{:file "/work/project/src/a.clj"
                 :source "a"
                 :future "b"
                 :edits [effect-a]}
                {:file "/work/project/src/../src/a.clj"
                 :source "c"
                 :future "d"
                 :edits [effect-b]}])
             (transaction
               [{:file "/work/project/src/a.clj"
                 :source "a"
                 :future "b"
                 :edits [(assoc effect-a
                                :insert-side :insert-left
                                :delete true)]}])]]
      (let [error
            (try
              (transaction/canonical-effect-identity "/work/project" compiled)
              nil
              (catch clojure.lang.ExceptionInfo exception
                (ex-data exception)))]
        (is (= :invalid-canonical-effect-input (:error-type error)))
        (is (true? (:source-unchanged error)))
        (is (false? (:mutation-attempted error)))
        (is (false? (:write-authority error)))
        (is (nil? (:projection error)))
        (is (nil? (:sha256 error)))))))

(deftest canonical-effect-identity-refuses-impossible-kind-evidence
  ;; @spec MCP-OP-EDIT-028
  (let [base {:path [0 1]
              :address {:preorder 2}
              :end-preorder 2}
        invalid-effects
        [(assoc base
                :insert-side :insert-left
                :before ""
                :after "(a) ")
         (assoc base
                :insert-side :insert-left
                :raw true
                :offset 1
                :before "x"
                :after "(a) ")
         (assoc base
                :delete true
                :raw true
                :offset 1
                :before "a"
                :after "b")
         (assoc base
                :insert-side :middle
                :raw true
                :offset 1
                :before ""
                :after "(a) ")
         (assoc base
                :insert-side :insert-left
                :delete true
                :raw true
                :offset 1
                :before ""
                :after "")]]
    (doseq [effect invalid-effects]
      (let [compiled
            (compiler-shaped-effect-transaction
              [{:file "/work/project/src/a.clj"
                :source "a"
                :future "b"
                :edits [effect]}])
            error
            (try
              (transaction/canonical-effect-identity "/work/project" compiled)
              nil
              (catch clojure.lang.ExceptionInfo exception
                (ex-data exception)))]
        (is (= :invalid-canonical-effect-input (:error-type error)))
        (is (true? (:source-unchanged error)))
        (is (nil? (:projection error)))))))

(deftest canonical-effect-identity-ignores-untouched-scoped-files
  (let [effect {:path [0 1]
                :address {:preorder 2}
                :end-preorder 2
                :before "a"
                :after "b"}
        changed {:file "/work/project/src/a.clj"
                 :source "a"
                 :future "b"
                 :edits [effect]}
        untouched {:file "/work/project/src/b.clj"
                   :source "c"
                   :future "c"
                   :edits []}
        narrow (compiler-shaped-effect-transaction [changed])
        broad (compiler-shaped-effect-transaction [changed untouched])]
    (is (= (transaction/canonical-effect-identity "/work/project" narrow)
           (transaction/canonical-effect-identity "/work/project" broad)))
    (is (= 1 (:files (transaction/canonical-effect-identity
                       "/work/project" broad))))))

(deftest intents-match-only-original-snapshots-and-never-cascade
  (let [source "(ns app.a)\n(def values [(old x) (new y)])\n"
        result
        (transaction/compile-transaction
          {"src/a.clj" source}
          (spec [(intent ["src/a.clj"] "(old x)" "(new x)" 1)
                 (intent ["src/a.clj"] "(new y)" "(final y)" 1)]
                {:intent-count 2 :edit-count 2 :changed-file-count 1}))
        future (get-in result [:future-sources "src/a.clj"])]
    (is (:ok result))
    (is (= "(ns app.a)\n(def values [(new x) (final y)])\n" future))
    (is (str/includes? future "(new x)"))
    (is (not (str/includes? future "(final x)")))))

(deftest real-program-ui-plan-compiles-six-coordinated-edits-once
  (let [shell-file "test/fixtures/intent_transaction/app_shell.clj"
        reader-file "test/fixtures/intent_transaction/source_reader.clj"
        original-shell (slurp shell-file)
        original-reader (slurp reader-file)
        change-spec
        (spec
          [(intent [shell-file reader-file]
                   ":body" ":body.ide-shell-page" 2)
           (intent [reader-file]
                   "[project-id projects artifact current-location reader-region show-all?]"
                   "[project-id projects artifact document-title current-location reader-region show-all?]"
                   1)
           (intent [reader-file]
                   "[:title \"Workbench\"]"
                   "[:title (str document-title \" — Workbench\")]"
                   1)
           (intent [reader-file]
                   "[:span.tab-label artifact]"
                   "[:span.tab-label {:title artifact} document-title]"
                   1)
           (intent [shell-file]
                   "#(str \"/assets\" %)"
                   "(partial str \"/assets\")"
                   1)]
          {:intent-count 5 :edit-count 6 :changed-file-count 2})
        result (transaction/plan-change {:op :change :spec change-spec})]
    (is (:ok result))
    (is (= 5 (:intent-count result)))
    (is (= 6 (:match-count result)))
    (is (= 2 (:changed-file-count result)))
    (is (= [2 4] (mapv :match-count (:files result))))
    (is (= {:whole-files-parsed true :file-count 2}
           (:validated result)))
    (is (str/includes? (:diff result) "(partial str \"/assets\")"))
    (is (str/includes? (:diff result)
                       "[:title (str document-title \" — Workbench\")]"))
    (is (not (str/includes? (:diff result)
                            "The text :body is not a structural tag.")))
    (is (= original-shell (slurp shell-file)))
    (is (= original-reader (slurp reader-file)))))

(defn- compiled-two-file-change
  []
  (transaction/compile-transaction
    {"src/a.clj" "(ns app.a)\n(def value (old x))\n"
     "src/b.clj" "(ns app.b)\n(def value :body)\n"}
    (spec [(intent ["src/a.clj"] "(old x)" "(new x)" 1)
           (intent ["src/b.clj"] ":body" ":body.page" 1)]
          {:intent-count 2 :edit-count 2 :changed-file-count 2})))

(defn- memory-io
  [state write-fn]
  {:read-source (fn [file] (get @state file))
   :write-source! (fn [file source]
                    (write-fn state file source))})

(deftest commit-protocol-updates-and-verifies-every-file
  (let [compiled (compiled-two-file-change)
        originals (:original-sources compiled)
        futures (:future-sources compiled)
        state (atom originals)
        writes (atom [])
        result
        (transaction/commit-compiled!
          compiled
          (memory-io state
                     (fn [state file source]
                       (swap! writes conj file)
                       (swap! state assoc file source))))]
    (is (:ok result))
    (is (= :change! (:operation result)))
    (is (:committed result))
    (is (= ["src/a.clj" "src/b.clj"] @writes))
    (is (= futures @state))
    (is (= (into {} (map (juxt :file :result-hash) (:files compiled)))
           (get-in result [:verified :read-back-hashes])))
    (is (= {:whole-files true :file-count 2}
           (select-keys (:verified result) [:whole-files :file-count])))))

(deftest commit-protocol-refuses-stale-source-before-any-write
  (let [compiled (compiled-two-file-change)
        state (atom (assoc (:original-sources compiled)
                           "src/b.clj" "(ns app.b)\n(def value :changed-elsewhere)\n"))
        writes (atom 0)
        result
        (transaction/commit-compiled!
          compiled
          (memory-io state
                     (fn [state file source]
                       (swap! writes inc)
                       (swap! state assoc file source))))]
    (is (= :source-hash-mismatch (:error-type result)))
    (is (= "src/b.clj" (:file result)))
    (is (zero? @writes))
    (is (= "(ns app.a)\n(def value (old x))\n"
           (get @state "src/a.clj")))
    (is (= "(ns app.b)\n(def value :changed-elsewhere)\n"
           (get @state "src/b.clj")))))

(deftest commit-protocol-rolls-back-a-partial-write
  (let [compiled (compiled-two-file-change)
        originals (:original-sources compiled)
        state (atom originals)
        write-count (atom 0)
        result
        (transaction/commit-compiled!
          compiled
          (memory-io state
                     (fn [state file source]
                       (swap! state assoc file source)
                       (when (= 2 (swap! write-count inc))
                         (throw (ex-info "injected second-write failure"
                                         {:error-type :injected-write-failure}))))))]
    (is (= :transaction-write-failed (:error-type result)))
    (is (= :injected-write-failure (:cause-error-type result)))
    (is (true? (:rolled-back result)))
    (is (= originals @state))
    (is (= {"src/a.clj" :restored "src/b.clj" :restored}
           (into {} (map (juxt :file :status) (:recovery result)))))))

(deftest commit-protocol-reports-recovery-required-without-clobbering-concurrent-source
  (let [compiled (compiled-two-file-change)
        originals (:original-sources compiled)
        concurrent "(ns app.b)\n(def value :concurrent-user-change)\n"
        state (atom originals)
        write-count (atom 0)
        result
        (transaction/commit-compiled!
          compiled
          (memory-io state
                     (fn [state file source]
                       (swap! state assoc file source)
                       (when (= 1 (swap! write-count inc))
                         (swap! state assoc "src/b.clj" concurrent)))))]
    (is (= :transaction-recovery-required (:error-type result)))
    (is (false? (:rolled-back result)))
    (is (= "src/b.clj" (:file result)))
    (is (= originals
           (assoc @state "src/b.clj" (get originals "src/b.clj"))))
    (is (= concurrent (get @state "src/b.clj")))
    (is (= :unexpected-source
           (:status (first (filter #(= "src/b.clj" (:file %))
                                   (:recovery result))))))))

(deftest commit-protocol-rolls-back-after-read-back-corruption
  (let [compiled (compiled-two-file-change)
        originals (:original-sources compiled)
        state (atom originals)
        corrupt-read-once? (atom true)
        b-result (get (:future-sources compiled) "src/b.clj")
        result
        (transaction/commit-compiled!
          compiled
          {:read-source
           (fn [file]
             (let [source (get @state file)]
               (if (and (= "src/b.clj" file)
                        (= b-result source)
                        (compare-and-set! corrupt-read-once? true false))
                 (str source "\n; corrupted read")
                 source)))
           :write-source! (fn [file source]
                            (swap! state assoc file source))})]
    (is (= :transaction-write-failed (:error-type result)))
    (is (= :read-back-hash-mismatch (:cause-error-type result)))
    (is (true? (:rolled-back result)))
    (is (= originals @state))))

(deftest commit-protocol-makes-rollback-failure-explicit
  (let [compiled (compiled-two-file-change)
        originals (:original-sources compiled)
        futures (:future-sources compiled)
        state (atom originals)
        forward-failed? (atom false)
        result
        (transaction/commit-compiled!
          compiled
          (memory-io
            state
            (fn [state file source]
              (cond
                (and (= "src/b.clj" file)
                     (= source (get futures file))
                     (compare-and-set! forward-failed? false true))
                (do
                  (swap! state assoc file source)
                  (throw (ex-info "injected forward failure"
                                  {:error-type :injected-write-failure})))

                (and (= "src/b.clj" file)
                     (= source (get originals file)))
                (throw (ex-info "injected rollback failure"
                                {:error-type :injected-rollback-failure}))

                :else
                (swap! state assoc file source)))))]
    (is (= :transaction-recovery-required (:error-type result)))
    (is (false? (:rolled-back result)))
    (is (= (get originals "src/a.clj") (get @state "src/a.clj")))
    (is (= (get futures "src/b.clj") (get @state "src/b.clj")))
    (is (= :restore-failed
           (:status (first (filter #(= "src/b.clj" (:file %))
                                   (:recovery result))))))))

(deftest durable-receipt-round-trips-and-restores-shape-changing-edits
  (let [original "(ns app.a)\n(def views [#(old %) (old account) (old account)])\n"
        compiled
        (transaction/compile-transaction
          {"src/a.clj" original}
          (spec [(intent ["src/a.clj"]
                         "(old account)"
                         "(wrapper (new value) {:deep true})"
                         2)]
                {:intent-count 1 :edit-count 2 :changed-file-count 1}))
        receipt (transaction/build-receipt compiled)
        round-tripped (edn/read-string (pr-str receipt))
        inverse (transaction/compile-inverse
                  round-tripped (:future-sources compiled))]
    (is (:ok compiled))
    (is (string? (:receipt-hash receipt)))
    (is (= 1 (:receipt-version receipt)))
    (is (nil? (:original-sources receipt)))
    (is (nil? (:future-sources receipt)))
    (is (every? (fn [edit]
                  (and (vector? (:path edit))
                       (seq (:path edit))))
                (mapcat :inverse-edits (:files receipt))))
    (is (= receipt round-tripped))
    (is (:ok inverse))
    (is (= {"src/a.clj" original} (:future-sources inverse)))
    (is (= (get-in compiled [:files 0 :source-hash])
           (get-in inverse [:files 0 :result-hash])))
    (is (valid-source? (get (:future-sources inverse) "src/a.clj")))))

(deftest formatter-coalescing-keeps-logical-and-physical-counts-independent
  (let [file "src/sample.clj"
        original (str "(ns sample.core)\n"
                      "(defn alpha [] :old-a)\n"
                      "(defn beta [] :old-b)\n")
        compiled
        (transaction/compile-transaction
          {file original}
          (spec [(intent [file] "sample.core" "sample.next" 1)
                 (intent [file] ":old-a" ":new-a" 1)
                 (intent [file] ":old-b" ":new-b" 1)]
                {:intent-count 3 :edit-count 3 :changed-file-count 1}))
        formatted (str "(ns sample.next)\n\n"
                       "(defn alpha\n  []\n  :new-a)\n\n"
                       "(defn beta\n  []\n  :new-b)\n")
        coalesced (transaction/with-future-sources compiled {file formatted})
        receipt (transaction/build-receipt coalesced)
        inverse (transaction/compile-inverse
                  receipt (:future-sources coalesced))]
    (is (:ok compiled))
    (is (:ok coalesced))
    (is (= 3 (:match-count receipt)))
    (is (= 1 (:inverse-edit-count receipt)))
    (is (= 1 (count (get-in receipt [:files 0 :inverse-edits]))))
    (is (:ok inverse))
    (is (= {file original} (:future-sources inverse)))))

(deftest addressed-delete-owns-only-attached-comments-and-round-trips
  (let [original (str "(ns app.a)\n\n"
                      ";; attached to doomed\n"
                      "(defn doomed [] 1)\n\n"
                      ";; detached from kept\n\n"
                      "(defn kept [] 2)\n")
        doomed (transaction/addressed-form-at
                 original {:line 4 :character 1})
        kept (transaction/addressed-form-at
               original {:line 8 :character 1})
        compiled (transaction/compile-addressed-transaction
                   {"src/a.clj" original}
                   [(assoc doomed :id "delete-doomed" :file "src/a.clj"
                           :delete true)
                    (assoc kept :id "update-kept" :file "src/a.clj"
                           :after "(defn kept [] 3)")])
        expected (str "(ns app.a)\n\n"
                      ";; detached from kept\n\n"
                      "(defn kept [] 3)\n")
        receipt (when (:ok compiled) (transaction/build-receipt compiled))
        inverse (when receipt
                  (transaction/compile-inverse
                    receipt {"src/a.clj" expected}))]
    (is (:ok compiled))
    (is (= expected (get (:future-sources compiled) "src/a.clj")))
    (is (:ok inverse))
    (is (= original (get (:future-sources inverse) "src/a.clj")))))

(deftest addressed-delete-protects-the-namespace-form
  (let [source "(ns app.a)\n(defn kept [] 2)\n"
        namespace-form (transaction/addressed-form-at
                         source {:line 1 :character 1})
        result (transaction/compile-addressed-transaction
                 {"src/a.clj" source}
                 [(assoc namespace-form :id "delete-ns" :file "src/a.clj"
                         :delete true)])]
    (is (= :protected-namespace-form (:error-type result)))
    (is (nil? (:future-sources result)))))

(deftest inverse-refuses-stale-corrupt-and-unsupported-receipts
  (let [compiled (compiled-two-file-change)
        receipt (transaction/build-receipt compiled)
        receipt-hash-fn
        (ns-resolve 'clj-surgeon.intent-transaction 'receipt-hash)
        stale-sources (assoc (:future-sources compiled)
                             "src/b.clj"
                             "(ns app.b)\n(def value :changed-after-transaction)\n")]
    (testing "one stale result hash refuses the complete inverse"
      (let [result (transaction/compile-inverse receipt stale-sources)]
        (is (= :result-hash-mismatch (:error-type result)))
        (is (= "src/b.clj" (:file result)))
        (is (nil? (:future-sources result)))))
    (testing "receipt contents are hash fenced"
      (let [result (transaction/compile-inverse
                     (assoc receipt :diff "tampered")
                     (:future-sources compiled))]
        (is (= :invalid-transaction-receipt (:error-type result)))
        (is (str/includes? (:error result) "hash"))))
    (testing "unknown versions refuse before inverse compilation"
      (let [result (transaction/compile-inverse
                     (assoc receipt :receipt-version 999)
                     (:future-sources compiled))]
        (is (= :invalid-transaction-receipt (:error-type result)))
        (is (= 1 (:supported-receipt-version result)))))
    (testing "a corrupt path refuses even when its receipt hash is recomputed"
      (let [tampered (assoc-in receipt [:files 0 :inverse-edits 0 :path]
                               [0 999])
            rehashed (assoc tampered :receipt-hash (receipt-hash-fn tampered))
            result (transaction/compile-inverse
                     rehashed (:future-sources compiled))]
        (is (= :invalid-transaction-receipt (:error-type result)))
        (is (= :stale-path (:cause-error-type result)))))
    (testing "logical and physical receipt counts are independently guarded"
      (testing "logical matches agree with intent evidence"
        (let [tampered (update receipt :match-count inc)
              rehashed (assoc tampered :receipt-hash (receipt-hash-fn tampered))
              result (transaction/compile-inverse
                       rehashed (:future-sources compiled))]
          (is (= :invalid-transaction-receipt (:error-type result)))
          (is (str/includes? (:error result) "logical match count"))))
      (testing "physical inverse count agrees with inverse records"
        (let [tampered (update receipt :inverse-edit-count inc)
              rehashed (assoc tampered :receipt-hash (receipt-hash-fn tampered))
              result (transaction/compile-inverse
                       rehashed (:future-sources compiled))]
          (is (= :invalid-transaction-receipt (:error-type result)))
          (is (str/includes? (:error result) "inverse edit count"))))
      (testing "legacy ordinary receipts derive inverse count from match count"
        (let [legacy (dissoc receipt :inverse-edit-count)
              rehashed (assoc legacy :receipt-hash (receipt-hash-fn legacy))
              result (transaction/compile-inverse
                       rehashed (:future-sources compiled))]
          (is (:ok result))
          (is (= (:original-sources compiled) (:future-sources result))))))))

(deftest receipt-publication-failure-restores-source-and-preserves-old-receipt
  (let [temp-dir (fs/create-temp-dir {:prefix "intent-receipt-failure-"})
        file (str (fs/path temp-dir "sample.clj"))
        receipt-file (str (fs/path temp-dir "receipt.edn"))
        source "(ns sample)\n(def value (old x))\n"
        change-spec
        (spec [(intent [file] "(old x)" "(new x)" 1)]
              {:intent-count 1 :edit-count 1 :changed-file-count 1})
        publish-var (ns-resolve 'clj-surgeon.intent-transaction
                                'publish-staged-receipt!)]
    (try
      (spit file source)
      (spit receipt-file "{:old-receipt true}\n")
      (let [result
            (with-redefs-fn
              {publish-var
               (fn [& _]
                 (throw (ex-info "injected publication failure"
                                 {:error-type :injected-publish-failure})))}
              #(transaction/execute-change!
                 {:spec change-spec :receipt-out receipt-file}))]
        (is (= :receipt-write-failed (:error-type result)))
        (is (true? (:rolled-back result)))
        (is (= source (slurp file)))
        (is (= "{:old-receipt true}\n" (slurp receipt-file)))
        (is (:ok (:recovery result))))
      (finally
        (fs/delete-tree temp-dir)))))

(deftest public-mutation-boundary-refuses-before-changing-source-or-receipt
  (let [temp-dir (fs/create-temp-dir {:prefix "intent-boundary-refusal-"})
        file (str (fs/path temp-dir "sample.clj"))
        receipt-file (str (fs/path temp-dir "receipt.edn"))
        malformed-receipt (str (fs/path temp-dir "malformed.edn"))
        missing-parent-receipt
        (str (fs/path temp-dir "missing" "receipt.edn"))
        source "(ns sample)\n(def value (old x))\n"
        change-spec
        (spec [(intent [file] "(old x)" "(new x)" 1)]
              {:intent-count 1 :edit-count 1 :changed-file-count 1})]
    (try
      (spit file source)
      (spit receipt-file "{:old-receipt true}\n")
      (spit malformed-receipt "{:not valid")
      (testing "a receipt may not alias source"
        (let [result (transaction/execute-change!
                       {:spec change-spec :receipt-out file})]
          (is (= :invalid-receipt-path (:error-type result)))
          (is (= source (slurp file)))))
      (testing "a missing receipt parent refuses before commit"
        (let [result (transaction/execute-change!
                       {:spec change-spec
                        :receipt-out missing-parent-receipt})]
          (is (= :invalid-receipt-path (:error-type result)))
          (is (= source (slurp file)))))
      (testing "a plan refusal preserves a pre-existing receipt"
        (let [bad-spec (assoc-in change-spec [:intents 0 :expect-count] 2)
              result (transaction/execute-change!
                       {:spec bad-spec :receipt-out receipt-file})]
          (is (= :expect-count-mismatch (:error-type result)))
          (is (= source (slurp file)))
          (is (= "{:old-receipt true}\n" (slurp receipt-file)))))
      (testing "malformed undo receipt cannot reach source mutation"
        (let [result (transaction/execute-undo!
                       {:receipt malformed-receipt})]
          (is (= :invalid-transaction-receipt (:error-type result)))
          (is (= source (slurp file)))))
      (testing "unknown mutation flags refuse"
        (let [result (transaction/execute-change!
                       {:spec change-spec :receipt-out receipt-file
                        :surprise true})]
          (is (= :unknown-arguments (:error-type result)))
          (is (= source (slurp file)))
          (is (= "{:old-receipt true}\n" (slurp receipt-file)))))
      (finally
        (fs/delete-tree temp-dir)))))

(deftest change-help-teaches-one-plan-materialization
  (let [op-def (get core/ops-registry :change)
        help (core/format-op-help :change op-def)
        global-help (core/format-global-help core/ops-registry)]
    (is (= :change (core/resolve-op :change)))
    (is (str/includes? global-help "    change"))
    (is (str/includes? help "complete mechanical model plan"))
    (is (str/includes? help "writes nothing"))
    (is (str/includes? help "heterogeneous model plan"))
    (is (str/includes? help ":changes"))
    (is (str/includes? help ":in"))
    (is (str/includes? help ":forms"))
    (is (str/includes? help ":find"))
    (is (str/includes? help "[:replace SOURCE]"))
    (is (str/includes? help ":each-form"))
    (is (str/includes? help ":intent-count"))
    (is (str/includes? help ":changed-file-count"))))

(deftest change-and-undo-help-teach-one-shot-guarded-materialization
  (let [apply-help (core/format-op-help :change!
                                        (get core/ops-registry :change!))
        undo-help (core/format-op-help :undo-change!
                                       (get core/ops-registry :undo-change!))
        global-help (core/format-global-help core/ops-registry)]
    (is (= :change! (core/resolve-op :change!)))
    (is (= :undo-change! (core/resolve-op :undo-change!)))
    (is (str/includes? global-help "    change!"))
    (is (str/includes? global-help "    undo-change!"))
    (is (str/includes? apply-help "complete mechanical model plan once"))
    (is (str/includes? apply-help ":changes"))
    (is (str/includes? apply-help "per-change count or distribution guard"))
    (is (str/includes? apply-help ":spec-file -"))
    (is (str/includes? apply-help "kubectl apply -f -"))
    (is (str/includes? apply-help "without probing source"))
    (is (str/includes? apply-help "publishes the receipt last"))
    (is (str/includes? apply-help ":receipt-out"))
    (is (str/includes? apply-help "Do not open :receipt-out"))
    (is (str/includes? apply-help ":receipt PATH"))
    (is (str/includes? undo-help "entire inverse before writing"))
    (is (str/includes? undo-help "second undo refuses"))))

(deftest public-docs-preserve-the-one-shot-transaction-contract
  (let [readme (slurp "README.md")
        skill (str (slurp "skills/clj-surgeon/SKILL.md")
                   "\n"
                   (slurp "skills/clj-surgeon/references/cli-fallback.md"))
        changelog (slurp "CHANGELOG.md")
        vision (slurp "docs/vision.md")]
    (doseq [[label text] [["README" readme]
                          ["skill" skill]
                          ["changelog" changelog]
                          ["vision" vision]]]
      (is (str/includes? text ":change!")
          (str label " must document guarded intent application"))
      (is (str/includes? text ":undo-change!")
          (str label " must document the hash-fenced inverse"))
      (is (str/includes? text ":changes")
          (str label " must document scoped change compilation")))
    (doseq [[label text] [["README" readme]
                          ["skill" skill]]]
      (is (str/includes? text ":each-form")
          (str label " must teach owner distribution guards"))
      (is (str/includes? text "[:replace")
          (str label " must show the supported scoped operator")))
    (is (str/includes? readme
                       "Do not split one known multi-edit plan into repeated"))
    (is (str/includes? skill
                       "Do not split one known plan into repeated edit calls"))
    (testing "controlled benchmark evidence stays explicit" (is (str/includes? readme "Assisted `apply_clojure_changes`")) (is (str/includes? readme "4 / 4")) (is (str/includes? readme "43.2%")) (is (str/includes? readme "Tool metadata alone did not cause adoption")))))

(deftest change-cli-previews-real-files-and-refuses-with-nonzero-exit
  (let [temp-dir (fs/create-temp-dir {:prefix "intent-change-cli-"})
        file (str (fs/path temp-dir "sample.clj"))
        source "(ns sample)\n(defn page [x] [(old x) :body])\n"
        change-spec
        (spec [(intent [file] "(old x)" "#(new %)" 1)
               (intent [file] ":body" ":body.page" 1)]
              {:intent-count 2 :edit-count 2 :changed-file-count 1})]
    (try
      (spit file source)
      (let [{:keys [exit out err]}
            @(proc/process ["bb" "-m" "clj-surgeon.core"
                            ":op" ":change"
                            ":spec" (pr-str change-spec)]
                           {:out :string :err :string})
            result (edn/read-string out)]
        (is (= 0 exit))
        (is (= "" err))
        (is (:ok result))
        (is (= 2 (:match-count result)))
        (is (= 1 (:changed-file-count result)))
        (is (str/includes? (:diff result) "#(new %)"))
        (is (nil? (:future-sources result)))
        (is (= source (slurp file))))

      (let [wrong-spec
            (assoc-in change-spec [:intents 0 :expect-count] 2)
            {:keys [exit out err]}
            @(proc/process ["bb" "-m" "clj-surgeon.core"
                            ":op" ":change"
                            ":spec" (pr-str wrong-spec)]
                           {:out :string :err :string})
            result (edn/read-string out)]
        (is (= 1 exit))
        (is (= "" err))
        (is (= :expect-count-mismatch (:error-type result)))
        (is (= 0 (:intent-index result)))
        (is (= 1 (:actual-count result)))
        (is (= source (slurp file))))
      (finally
        (fs/delete-tree temp-dir)))))

(deftest change-cli-loads-one-spec-document-from-file-or-stdin
  (let [temp-dir (fs/create-temp-dir {:prefix "intent-change-spec-input-"})
        file (str (fs/path temp-dir "sample.clj"))
        spec-file (str (fs/path temp-dir "change.edn"))
        source "(ns sample)\n(defn page [x] [(old x)])\n"
        change-spec
        (spec [(intent [file] "(old x)" "(new x)" 1)]
              {:intent-count 1 :edit-count 1 :changed-file-count 1})
        run-cli (fn [args input]
                  @(proc/process (into ["bb" "-m" "clj-surgeon.core"] args)
                                 (cond-> {:out :string :err :string}
                                   input (assoc :in input))))]
    (try
      (spit file source)
      (spit spec-file (pr-str change-spec))
      (doseq [[label args input]
              [["file" [":op" ":change" ":spec-file" spec-file] nil]
               ["stdin" [":op" ":change" ":spec-file" "-"]
                (pr-str change-spec)]]]
        (testing label
          (let [{:keys [exit out err]} (run-cli args input)
                result (edn/read-string out)]
            (is (= 0 exit))
            (is (= "" err))
            (is (:ok result))
            (is (= source (slurp file))))))
      (doseq [[label args input error-type]
              [["conflicting inputs"
                [":op" ":change" ":spec" (pr-str change-spec)
                 ":spec-file" spec-file]
                nil :conflicting-spec-inputs]
               ["missing input" [":op" ":change"] nil :missing-spec-input]
               ["empty stdin" [":op" ":change" ":spec-file" "-"] ""
                :missing-spec-stdin]
               ["trailing form" [":op" ":change" ":spec-file" "-"]
                (str (pr-str change-spec) "\n:extra")
                :invalid-spec-document]]]
        (testing label
          (let [{:keys [exit out err]} (run-cli args input)
                result (edn/read-string out)]
            (is (= 1 exit))
            (is (= "" err))
            (is (= error-type (:error-type result)))
            (is (= source (slurp file))))))
      (finally
        (fs/delete-tree temp-dir)))))

(deftest change-cli-dogfoods-one-shot-multi-file-apply-and-exact-undo
  (let [temp-dir (fs/create-temp-dir {:prefix "intent-change-dogfood-"})
        app-file (str (fs/path temp-dir "app_shell.clj"))
        reader-file (str (fs/path temp-dir "source_reader.clj"))
        receipt-file (str (fs/path temp-dir "receipt.edn"))
        original-app (slurp "test/fixtures/intent_transaction/app_shell.clj")
        original-reader
        (slurp "test/fixtures/intent_transaction/source_reader.clj")
        change-spec
        (spec [(intent [app-file reader-file]
                       ":body" ":body.intent-page" 2)
               (intent [app-file]
                       "\"/app.css\"" "\"/intent.css\"" 1)
               (intent [reader-file]
                       "[:title \"Workbench\"]"
                       "[:title \"Intent Workbench\"]" 1)]
              {:intent-count 3 :edit-count 4 :changed-file-count 2})]
    (try
      (spit app-file original-app)
      (spit reader-file original-reader)
      (let [{:keys [exit out err]}
            @(proc/process ["bb" "-m" "clj-surgeon.core"
                            ":op" ":change!"
                            ":spec-file" "-"
                            ":receipt-out" receipt-file]
                           {:in (pr-str change-spec)
                            :out :string :err :string})
            result (edn/read-string out)
            changed-app (slurp app-file)
            changed-reader (slurp reader-file)
            saved (edn/read-string (slurp receipt-file))]
        (is (= 0 exit))
        (is (= "" err))
        (is (:ok result))
        (is (= :change! (:operation result)))
        (is (= 4 (:match-count result)))
        (is (= 2 (:changed-file-count result)))
        (is (= (str (fs/canonicalize receipt-file))
               (:receipt-file result)))
        (is (= (:receipt-hash result) (:receipt-hash saved)))
        (is (nil? (:files result)))
        (is (not (str/includes? out ":inverse-edits")))
        (is (< (count out) (count (slurp receipt-file))))
        (is (str/includes? changed-app "#(str"))
        (is (not (str/includes? changed-app "fn*")))
        (is (str/includes? changed-app ":body.intent-page"))
        (is (str/includes? changed-reader
                           ";; Keep this explanation attached to the page body."))
        (is (str/includes? changed-reader "[:title \"Intent Workbench\"]"))
        (is (valid-source? changed-app))
        (is (valid-source? changed-reader)))

      (let [{:keys [exit out err]}
            @(proc/process ["bb" "-m" "clj-surgeon.core"
                            ":op" ":undo-change!"
                            ":receipt" receipt-file]
                           {:out :string :err :string})
            result (edn/read-string out)]
        (is (= 0 exit))
        (is (= "" err))
        (is (:ok result))
        (is (= :undo-change! (:operation result)))
        (is (= original-app (slurp app-file)))
        (is (= original-reader (slurp reader-file))))

      (let [{:keys [exit out err]}
            @(proc/process ["bb" "-m" "clj-surgeon.core"
                            ":op" ":undo-change!"
                            ":receipt" receipt-file]
                           {:out :string :err :string})
            result (edn/read-string out)]
        (is (= 1 exit))
        (is (= "" err))
        (is (= :result-hash-mismatch (:error-type result)))
        (is (= original-app (slurp app-file)))
        (is (= original-reader (slurp reader-file))))
      (finally
        (fs/delete-tree temp-dir)))))

(deftest scoped-change-cli-applies-owner-guarded-edits-and-undoes-once
  (let [temp-dir (fs/create-temp-dir {:prefix "scoped-change-dogfood-"})
        app-file (str (fs/path temp-dir "app_shell.clj"))
        reader-file (str (fs/path temp-dir "source_reader.clj"))
        receipt-file (str (fs/path temp-dir "receipt.edn"))
        original-app (slurp "test/fixtures/intent_transaction/app_shell.clj")
        original-reader
        (slurp "test/fixtures/intent_transaction/source_reader.clj")
        scoped-spec
        (change-spec
          [(change :app-body [app-file] ['ide-shell]
                   ":body" ":body.scoped-page"
                   {:matches 1 :each-form 1 :each-file 1})
           (change :reader-body [reader-file] ['source-reader-shell]
                   ":body" ":body.scoped-page"
                   {:matches 1 :each-form 1 :each-file 1})
           (change :app-css [app-file] ['ide-shell]
                   "\"/app.css\"" "\"/scoped.css\""
                   {:matches 1 :each-form 1 :each-file 1})
           (change :reader-title [reader-file] ['source-reader-shell]
                   "[:title \"Workbench\"]"
                   "[:title \"Scoped Workbench\"]"
                   {:matches 1 :each-form 1 :each-file 1})]
          {:changes 4 :edits 4 :files 2})]
    (try
      (spit app-file original-app)
      (spit reader-file original-reader)
      (let [{:keys [exit out err]}
            @(proc/process ["bb" "-m" "clj-surgeon.core"
                            ":op" ":change!"
                            ":spec-file" "-"
                            ":receipt-out" receipt-file]
                           {:in (pr-str scoped-spec)
                            :out :string :err :string})
            result (edn/read-string out)]
        (is (= 0 exit))
        (is (= "" err))
        (is (:ok result))
        (is (= 4 (:change-count result)))
        (is (= 4 (:match-count result)))
        (is (= 2 (:changed-file-count result)))
        (is (str/includes? (slurp app-file) ":body.scoped-page"))
        (is (str/includes? (slurp app-file) "\"/scoped.css\""))
        (is (str/includes? (slurp reader-file) ":body.scoped-page"))
        (is (str/includes? (slurp reader-file)
                           "[:title \"Scoped Workbench\"]"))
        (is (valid-source? (slurp app-file)))
        (is (valid-source? (slurp reader-file))))

      (let [{:keys [exit out err]}
            @(proc/process ["bb" "-m" "clj-surgeon.core"
                            ":op" ":undo-change!"
                            ":receipt" receipt-file]
                           {:out :string :err :string})
            result (edn/read-string out)]
        (is (= 0 exit))
        (is (= "" err))
        (is (:ok result))
        (is (= original-app (slurp app-file)))
        (is (= original-reader (slurp reader-file))))
      (finally
        (fs/delete-tree temp-dir)))))

(deftest scoped-change-cli-refuses-wrong-owner-distribution-without-writing
  (let [temp-dir (fs/create-temp-dir {:prefix "scoped-change-refusal-"})
        source-file (str (fs/path temp-dir "owners.clj"))
        receipt-file (str (fs/path temp-dir "receipt.edn"))
        original-source
        (str "(ns fixture.owners)\n\n"
             "(defn first-owner [] [:body :body])\n\n"
             "(defn second-owner [] [:other])\n")
        scoped-spec
        (change-spec
          [(change :body-class [source-file] ['first-owner 'second-owner]
                   ":body" ":body.scoped-page"
                   {:matches 2 :each-form 1})]
          {:changes 1 :edits 2 :files 1})]
    (try
      (spit source-file original-source)
      (let [{:keys [exit out err]}
            @(proc/process ["bb" "-m" "clj-surgeon.core"
                            ":op" ":change!"
                            ":spec-file" "-"
                            ":receipt-out" receipt-file]
                           {:in (pr-str scoped-spec)
                            :out :string :err :string})
            result (edn/read-string out)]
        (is (= 1 exit))
        (is (= "" err))
        (is (= :change-distribution-mismatch (:error-type result)))
        (is (= :each-form (:distribution result)))
        (is (= original-source (slurp source-file)))
        (is (not (fs/exists? receipt-file))))
      (finally
        (fs/delete-tree temp-dir)))))

;; ---------------------------------------------------------------------------
;; Matched-but-unaddressed reporting. Field case: 2026-09-02 session 4 on
;; curtain-call src/cfp_scheduler_killer/folds.clj — one `match` on the guard
;; shape returned 19 arms, the transaction addressed 16, and the exclusion
;; rationale for the other 3 existed only in the driver's head.
;; ---------------------------------------------------------------------------

(def ^:private guard-pattern
  "(if-let [slug (:slug (event-by-id state (:event-id payload)))] _ state)")

(defn- fold-arm
  [dispatch]
  (str "(defmethod fold-event \"" dispatch "\"\n"
       "  [state payload]\n"
       "  ;; INTENT: LENS-004\n"
       "  (if-let [slug (:slug (event-by-id state (:event-id payload)))]\n"
       "    (assoc-in state [:events slug :settings :" dispatch "] true)\n"
       "    state))\n"))

(def ^:private folds-source
  (str "(ns cfp-scheduler-killer.folds)\n\n"
       (str/join "\n" (map #(fold-arm (str "flag" %)) (range 19)))
       "\n(defn event-by-id [state id] nil)\n"))

(defn- folds-transaction
  [addressed-dispatches]
  (transaction/compile-transaction
    {"src/folds.clj" folds-source}
    {:changes
     (mapv (fn [dispatch]
             {:id (keyword (str "arm-" dispatch))
              :in ["src/folds.clj"]
              :forms [{:kind :defmethod
                       :name 'fold-event
                       :dispatch (str "\"" dispatch "\"")}]
              :find (str "(if-let [slug (:slug (event-by-id state"
                         " (:event-id payload)))]\n"
                         "    (assoc-in state [:events slug :settings :"
                         dispatch "] true)\n"
                         "    state)")
              :do [:replace
                   (str "(update-settings state (:event-id payload) assoc :"
                        dispatch " true)")]
              :expect {:matches 1}})
           addressed-dispatches)
     :expect {:changes (count addressed-dispatches)
              :edits (count addressed-dispatches)
              :files 1}}))

(defn- folds-basis
  [expected-count]
  {:file "src/folds.clj"
   :file-hash (structural-lens/source-hash folds-source)
   :match guard-pattern
   :count expected-count})

(deftest expect-matched-lists-the-matched-sites-a-transaction-did-not-address
  ;; @spec MCP-OP-MATCHED-001
  (let [compiled (folds-transaction (mapv #(str "flag" %) (range 16)))
        _ (is (:ok compiled))
        result (transaction/matched-basis-evidence compiled (folds-basis 19))
        evidence (:evidence result)]
    (is (:ok result))
    (is (= 19 (:matched-count evidence)))
    (is (= 16 (:addressed-matches evidence)))
    (is (= 3 (:unaddressed-match-count evidence)))
    (is (= 3 (count (:unaddressed-matches evidence))))
    (is (every? #(and (integer? (:line %))
                      (re-matches #"[0-9a-f]{64}" (:hash %)))
                (:unaddressed-matches evidence)))
    (testing "the reported lines are the pre-image lines of the skipped arms"
      (let [lines (mapv :line (:unaddressed-matches evidence))
            source-lines (vec (str/split-lines folds-source))]
        (is (= 3 (count (distinct lines))))
        (is (every? #(str/includes? (nth source-lines (dec %)) "if-let [slug")
                    lines))))))

(deftest expect-matched-reports-zero-when-the-transaction-addressed-everything
  ;; @spec MCP-OP-MATCHED-001
  (let [compiled (folds-transaction (mapv #(str "flag" %) (range 19)))
        result (transaction/matched-basis-evidence compiled (folds-basis 19))
        evidence (:evidence result)]
    (is (:ok result))
    (is (= 19 (:matched-count evidence)))
    (is (= 19 (:addressed-matches evidence)))
    (is (= 0 (:unaddressed-match-count evidence)))
    (is (= [] (:unaddressed-matches evidence)))))

(deftest expect-matched-refuses-a-stale-basis-before-any-write
  ;; @spec MCP-OP-MATCHED-002
  (let [compiled (folds-transaction ["flag0"])]
    (testing "a file hash from a different snapshot"
      (let [result (transaction/matched-basis-evidence
                     compiled
                     (assoc (folds-basis 19)
                            :file-hash (structural-lens/source-hash "(ns other)")))]
        (is (nil? (:ok result)))
        (is (= :expect-matched-stale (:error-type result)))
        (is (= "file_hash" (:mismatch result)))
        (is (= (structural-lens/source-hash folds-source)
               (:actual-file-hash result)))))
    (testing "a file this transaction did not read"
      (let [result (transaction/matched-basis-evidence
                     compiled (assoc (folds-basis 19) :file "src/other.clj"))]
        (is (= :expect-matched-stale (:error-type result)))
        (is (= "file_not_in_transaction" (:mismatch result)))
        (is (= ["src/folds.clj"] (:transaction-files result)))))
    (testing "a match count that does not describe this snapshot"
      (let [result (transaction/matched-basis-evidence
                     compiled (folds-basis 21))]
        (is (= :expect-matched-stale (:error-type result)))
        (is (= "match_count" (:mismatch result)))
        (is (= 21 (:expected-match-count result)))
        (is (= 19 (:actual-match-count result)))))))

(deftest expect-matched-refuses-a-pattern-that-is-not-one-complete-form
  ;; @spec MCP-OP-MATCHED-003
  (let [compiled (folds-transaction ["flag0"])
        result (transaction/matched-basis-evidence
                 compiled (assoc (folds-basis 19) :match "(a) (b)"))]
    (is (= :expect-matched-invalid-pattern (:error-type result)))
    (is (= "src/folds.clj" (:file result)))))

;; ---------------------------------------------------------------------------
;; Two matched sites can share one pre-image line. Line granularity called the
;; unedited sibling "addressed" — the wrong failure direction for a receipt
;; whose whole job is naming what the transaction skipped.
;; ---------------------------------------------------------------------------

(def ^:private one-line-source
  "(ns one-line)\n\n(defn go [] (do (f 1) (f 2)))\n")

(defn- one-line-transaction
  [find-source replace-source]
  (transaction/compile-transaction
    {"src/one_line.clj" one-line-source}
    {:changes [{:id :one-call
                :in ["src/one_line.clj"]
                :forms ['go]
                :find find-source
                :do [:replace replace-source]
                :expect {:matches 1}}]
     :expect {:changes 1 :edits 1 :files 1}}))

(defn- one-line-basis
  []
  {:file "src/one_line.clj"
   :file-hash (structural-lens/source-hash one-line-source)
   :match "(f _)"
   :count 2})

(deftest expect-matched-separates-two-sites-that-share-one-line
  ;; @spec MCP-OP-MATCHED-004
  (let [compiled (one-line-transaction "(f 1)" "(g 1)")
        _ (is (:ok compiled))
        result (transaction/matched-basis-evidence compiled (one-line-basis))
        evidence (:evidence result)]
    (is (:ok result))
    (is (= 2 (:matched-count evidence)))
    (is (= 1 (:addressed-matches evidence)))
    (is (= 1 (:unaddressed-match-count evidence)))
    (testing "the skipped sibling is the one sharing the edited line"
      (is (= [3] (mapv :line (:unaddressed-matches evidence))))
      (is (= [(structural-lens/source-hash "(f 2)")]
             (mapv :hash (:unaddressed-matches evidence)))))))

(deftest expect-matched-counts-sites-inside-one-edited-form-as-addressed
  ;; @spec MCP-OP-MATCHED-004
  (let [compiled (one-line-transaction "(do (f 1) (f 2))" "(h)")
        _ (is (:ok compiled))
        result (transaction/matched-basis-evidence compiled (one-line-basis))
        evidence (:evidence result)]
    (is (:ok result))
    (is (= 2 (:addressed-matches evidence)))
    (is (= 0 (:unaddressed-match-count evidence)))
    (is (= [] (:unaddressed-matches evidence)))))

;; ---------------------------------------------------------------------------
;; The exact-owner selector reads every arm sharing a multimethod name while it
;; looks for one. A `#_` discard or a `^meta` wrapper on any of those arms threw
;; while scanning, so one unrelated arm made the whole file unaddressable.
;; ---------------------------------------------------------------------------

(def ^:private discarded-and-meta-arms-source
  (str "(ns t)\n\n"
       "(defmulti f (fn [x] x))\n\n"
       "(defmethod f #_skipped :actual [x] (inc x))\n\n"
       "(defmethod f ^:meta :withmeta [x] (inc x))\n\n"
       "(defmethod f :plain [x] (inc x))\n"))

(defn- dispatch-arm-transaction
  [dispatch]
  (transaction/compile-transaction
    {"src/t.clj" discarded-and-meta-arms-source}
    {:changes [{:id :one-arm
                :in ["src/t.clj"]
                :forms [{:kind :defmethod :name 'f :dispatch dispatch}]
                :find "(inc x)"
                :do [:replace "(dec x)"]
                :expect {:matches 1}}]
     :expect {:changes 1 :edits 1 :files 1}}))

(deftest defmethod-selector-scans-past-discarded-and-metadata-dispatches
  ;; @spec MCP-OP-DISPATCH-005
  (testing "an ordinary arm is still addressable in that file"
    (let [compiled (dispatch-arm-transaction ":plain")]
      (is (:ok compiled))
      (is (= 1 (count (mapcat :edits (:files compiled)))))))
  (testing "the arm behind a #_ discard is addressed by its real dispatch"
    (let [compiled (dispatch-arm-transaction ":actual")]
      (is (:ok compiled))))
  (testing "the arm behind ^meta is addressed by the value the metadata wraps"
    (let [compiled (dispatch-arm-transaction ":withmeta")]
      (is (:ok compiled)))))

(deftest expect-matched-separates-an-unreadable-file-from-an-unusable-pattern
  ;; @spec MCP-OP-MATCHED-003
  (let [compiled (one-line-transaction "(f 1)" "(g 1)")
        broken "(ns broken)\n\n(defn go [] (f 1"
        unreadable (assoc-in compiled [:original-sources "src/one_line.clj"]
                             broken)]
    (testing "an unusable pattern is named as one"
      (let [result (transaction/matched-basis-evidence
                     compiled (assoc (one-line-basis) :match "(a) (b)"))]
        (is (= :expect-matched-invalid-pattern (:error-type result)))))
    (testing "a file the pattern cannot be evaluated against is not"
      (let [result (transaction/matched-basis-evidence
                     unreadable
                     (assoc (one-line-basis)
                            :file-hash (structural-lens/source-hash broken)))]
        (is (= :expect-matched-unreadable-source (:error-type result)))
        (is (= "src/one_line.clj" (:file result)))
        (is (true? (:source-unchanged result)))))))
