(ns clj-surgeon.show-form-test
  (:require
   [babashka.fs :as fs]
   [babashka.process :as proc]
   [clj-surgeon.core :as core]
   [clj-surgeon.fields :as fields]
   [clj-surgeon.forms :as forms]
   [clj-surgeon.outline :as outline]
   [clj-surgeon.show-form :as show-form]
   [clj-surgeon.structural-lens :as structural-lens]
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.test :refer [deftest is testing]]))

(def basic-source
  "(ns sample.core)

;; Explain alpha.
;; Preserve this context.
(defn ^:private alpha [x]
  (+ x
     1))

(comment
  (def hidden :example))

(defn omega []
  :done)
")

(def cljc-source
  "(ns sample.portable)
#?(:clj
   (defn shared [] :clj)
   :cljs
   (defn shared [] :cljs))
#?@(:clj [(defn spliced [] :clj)]
    :cljs [(defn spliced [] :cljs)])
(defn common [] :both)
")

(defn- line-containing
  [source fragment]
  (some (fn [[index line]]
          (when (str/includes? line fragment) (inc index)))
        (map-indexed vector (str/split-lines source))))

(defn- selector-result
  [source selector]
  (show-form/select-form "sample.clj" source selector))

(deftest outline-source-preserves-the-existing-public-contract
  (let [from-source (outline/outline-source "sample.clj" basic-source)
        tmp (java.io.File/createTempFile "show-form-outline" ".clj")]
    (try
      (spit tmp basic-source)
      (is (= (assoc from-source :file (.getAbsolutePath tmp))
             (outline/outline (.getAbsolutePath tmp))))
      (is (not-any? :source (:forms from-source))
          ":ls must not begin returning complete form bodies")
      (finally
        (.delete tmp)))))

(deftest top-level-form-records-are-the-shared-pure-source-of-truth
  (let [records (outline/top-level-form-records "sample.clj" basic-source)
        alpha (first (filter #(= 'alpha (:name %)) records))]
    (is (= 'defn (:type alpha)))
    (is (= [:clj] (:platforms alpha)))
    (is (= 3 (:comment-start alpha)))
    (is (= "(defn ^:private alpha [x]\n  (+ x\n     1))" (:source alpha)))
    (is (some #(= 'ns (:type %)) records))
    (is (some #(= 'comment (:type %)) records))))

(deftest show-form-by-name-accepts-cli-strings-and-symbols
  (doseq [form-selector ["alpha" 'alpha]]
    (let [result (selector-result basic-source {:form form-selector})]
      (is (= :show-form (:operation result)))
      (is (= "sample.clj" (:file result)))
      (is (= {:form 'alpha} (:selector result)))
      (is (= 'alpha (:name result)))
      (is (= 'defn (:type result)))
      (is (= [:clj] (:platforms result)))
      (is (= 3 (:comment-start result)))
      (is (= "(defn ^:private alpha [x]\n  (+ x\n     1))" (:source result)))
      (is (= (structural-lens/source-hash basic-source) (:source-hash result))))))

(deftest show-forms-returns-requested-order-from-one-snapshot
  (let [result (selector-result basic-source {:forms ['omega "alpha"]})]
    (is (= :show-form (:operation result)))
    (is (= "sample.clj" (:file result)))
    (is (= {:forms ['omega 'alpha]} (:selector result)))
    (is (= 2 (:form-count result)))
    (is (= (reduce + (map count ["(defn omega []\n  :done)"
                                 "(defn ^:private alpha [x]\n  (+ x\n     1))"]))
           (:source-char-count result)))
    (is (= ['omega 'alpha] (mapv :name (:forms result))))
    (is (= ["(defn omega []\n  :done)"
            "(defn ^:private alpha [x]\n  (+ x\n     1))"]
           (mapv :source (:forms result))))
    (is (= (structural-lens/source-hash basic-source) (:source-hash result)))
    (is (not (contains? result :source)))))

(deftest show-forms-builds-top-level-records-once
  (let [calls (atom 0)
        original outline/top-level-form-records]
    (with-redefs [outline/top-level-form-records
                  (fn [& args]
                    (swap! calls inc)
                    (apply original args))]
      (let [result (selector-result basic-source {:forms ['alpha 'omega]})]
        (is (= ['alpha 'omega] (mapv :name (:forms result))))
        (is (= 1 @calls))))))

(deftest show-forms-refuses-invalid-and-duplicate-selector-vectors
  (let [too-many (vec (map #(symbol (str "form-" %)) (range 51)))
        cases [{:forms nil
                :error-type :invalid-forms-selector}
               {:forms 'alpha
                :error-type :invalid-forms-selector}
               {:forms []
                :error-type :invalid-forms-selector}
               {:forms ['alpha 'sample/omega]
                :error-type :invalid-forms-selector}
               {:forms ['alpha :omega]
                :error-type :invalid-forms-selector}
               {:forms too-many
                :error-type :invalid-forms-selector}
               {:forms ['alpha "alpha"]
                :error-type :duplicate-form-selectors}]]
    (doseq [{:keys [forms error-type]} cases]
      (let [result (selector-result basic-source {:forms forms})]
        (is (= error-type (:error-type result)) (pr-str forms))
        (is (not (contains? result :source)) (pr-str forms))))
    (is (= ['alpha]
           (:duplicate-forms
             (selector-result basic-source {:forms ['alpha "alpha"]}))))))

(deftest show-forms-refuses-the-complete-read-on-any-missing-or-ambiguous-name
  (testing "one missing owner suppresses every successful owner's source"
    (let [result (selector-result basic-source {:forms ['alpha 'missing]})]
      (is (= :batch-form-selection-failed (:error-type result)))
      (is (= 2 (:requested-form-count result)))
      (is (= 1 (:resolved-form-count result)))
      (is (= [{:form 'missing
               :error-type :form-not-found
               :match-count 0}]
             (:failures result)))
      (is (not (contains? result :forms)))
      (is (not-any? #(and (map? %) (contains? % :source))
                    (tree-seq coll? seq result)))))
  (testing "a reader-conditional ambiguity names its platform remedy"
    (let [result (show-form/select-form "sample.cljc" cljc-source
                                        {:forms ['shared 'common]})
          failure (first (:failures result))]
      (is (= :batch-form-selection-failed (:error-type result)))
      (is (= 'shared (:form failure)))
      (is (= :ambiguous-form (:error-type failure)))
      (is (= [:clj :cljs]
             (get-in failure [:remedies :select-platform :platforms])))
      (is (not (contains? result :forms))))))

(deftest show-forms-applies-one-platform-to-the-complete-batch
  (let [result (show-form/select-form "sample.cljc" cljc-source
                                      {:forms ['common 'shared]
                                       :platform :cljs})]
    (is (= {:forms ['common 'shared] :platform :cljs} (:selector result)))
    (is (= ['common 'shared] (mapv :name (:forms result))))
    (is (= ["(defn common [] :both)" "(defn shared [] :cljs)"]
           (mapv :source (:forms result))))))

(deftest show-forms-refuses-before-returning-an-oversized-source-batch
  (let [payload (apply str (repeat 33000 "x"))
        source (str "(ns large)\n"
                    "(def first-large " (pr-str payload) ")\n"
                    "(def second-large " (pr-str payload) ")\n")
        result (show-form/select-form "large.clj" source
                                      {:forms ['first-large 'second-large]})]
    (is (= :batch-source-limit-exceeded (:error-type result)))
    (is (= 65536 (:source-char-limit result)))
    (is (< (:source-char-limit result) (:source-char-count result)))
    (is (= "Request fewer forms in each batch" (:remedy result)))
    (is (not (contains? result :forms)))
    (is (not-any? #(and (map? %) (contains? % :source))
                  (tree-seq coll? seq result)))))

(deftest show-form-accepts-the-standalone-slash-as-an-unqualified-name
  (let [result (show-form/select-form "operators.clj"
                                      "(ns operators)\n(def / 1)\n"
                                      {:form "/"})]
    (is (= '/ (:name result)))
    (is (= "(def / 1)" (:source result)))))

(deftest show-form-by-line-selects-start-interior-end-and-attached-comment
  (let [expected-source "(defn ^:private alpha [x]\n  (+ x\n     1))"
        lines [(line-containing basic-source ";; Explain alpha.")
               (line-containing basic-source "(defn ^:private alpha")
               (line-containing basic-source "(+ x")
               (line-containing basic-source "1))")]]
    (doseq [line lines]
      (let [result (selector-result basic-source {:line line})]
        (is (= {:line line} (:selector result)))
        (is (= 'alpha (:name result)))
        (is (= expected-source (:source result)))))))

(deftest show-form-by-line-handles-unnamed-top-level-forms
  (testing "namespace form"
    (let [result (selector-result basic-source {:line 1})]
      (is (= 'ns (:type result)))
      (is (= "(ns sample.core)" (:source result)))
      (is (not (contains? result :name)))))
  (testing "comment form"
    (let [line (line-containing basic-source "(def hidden")
          result (selector-result basic-source {:line line})]
      (is (= 'comment (:type result)))
      (is (str/includes? (:source result) "(def hidden :example)"))
      (is (not (contains? result :name))))))

(deftest show-form-by-contains-selects-one-form-with-literal-evidence
  (testing "attached comments and exact source are searchable"
    (let [result (selector-result basic-source
                                  {:contains "Preserve this context."})]
      (is (= :show-form (:operation result)))
      (is (= {:contains "Preserve this context."} (:selector result)))
      (is (= 'alpha (:name result)))
      (is (= 1 (:occurrence-count result)))
      (is (= [{:line 4 :column 4}] (:occurrences result)))
      (is (= "(defn ^:private alpha [x]\n  (+ x\n     1))"
             (:source result)))))
  (testing "multiple literal occurrences in one form still select one form"
    (let [source "(ns repeated)\n\n(defn only []\n  [\"needle\" \"needle\"])\n"
          result (selector-result source {:contains "needle"})]
      (is (= 'only (:name result)))
      (is (= 2 (:occurrence-count result)))
      (is (= [{:line 4 :column 5}
              {:line 4 :column 14}]
             (:occurrences result)))))
  (testing "literal matching is case-sensitive and does not interpret regex"
    (let [source "(ns literal)\n(defn target [] \"a.*b\")\n"]
      (is (= 'target (:name (selector-result source {:contains "a.*b"}))))
      (is (= :contains-not-found
             (:error-type (selector-result source {:contains "A.*B"})))))))

(deftest show-form-by-contains-refuses-zero-or-many-containing-forms
  (let [source "(ns ambiguous)\n(defn alpha [] \"needle\")\n(defn beta [] \"needle\")\n"]
    (testing "no match is an exact refusal"
      (let [result (selector-result source {:contains "absent"})]
        (is (= :contains-not-found (:error-type result)))
        (is (zero? (:match-count result)))
        (is (= {:contains "absent"} (:selector result)))))
    (testing "multiple containing forms are bounded evidence, never a choice"
      (let [result (selector-result source {:contains "needle"})]
        (is (= :ambiguous-form (:error-type result)))
        (is (= 2 (:match-count result)))
        (is (= ['alpha 'beta] (mapv :name (:matches result))))
        (is (= [1 1] (mapv :occurrence-count (:matches result))))
        (is (not (contains? result :source)))))
    (testing "text outside every owned top-level form is not silently attached"
      (let [trailing (str source "\n;; trailing orphan needle\n")
            result (selector-result trailing {:contains "trailing orphan"})]
        (is (= :contains-not-found (:error-type result)))
        (is (zero? (:match-count result)))))))

(deftest show-form-refuses-invalid-selector-contracts
  (let [cases [{:opts {}
                :error-type :missing-selector
                :field [:required-one-of [:form :forms :line :contains]]}
               {:opts {:form "alpha" :line 5}
                :error-type :conflicting-selectors
                :field [:supplied-selectors [:form :line]]}
               {:opts {:form "alpha" :forms ['omega]}
                :error-type :conflicting-selectors
                :field [:supplied-selectors [:form :forms]]}
               {:opts {:forms ['alpha] :line 5}
                :error-type :conflicting-selectors
                :field [:supplied-selectors [:forms :line]]}
               {:opts {:forms ['alpha] :contains "context"}
                :error-type :conflicting-selectors
                :field [:supplied-selectors [:forms :contains]]}
               {:opts {:form "alpha" :contains "context"}
                :error-type :conflicting-selectors
                :field [:supplied-selectors [:form :contains]]}
               {:opts {:line 5 :contains "context"}
                :error-type :conflicting-selectors
                :field [:supplied-selectors [:line :contains]]}
               {:opts {:form "alpha" :line 5 :contains "context"}
                :error-type :conflicting-selectors
                :field [:supplied-selectors [:form :line :contains]]}
               {:opts {:form "alpha" :forms ['omega]
                       :line 5 :contains "context"}
                :error-type :conflicting-selectors
                :field [:supplied-selectors [:form :forms :line :contains]]}
               {:opts {:form "sample/alpha"}
                :error-type :invalid-form-selector
                :field [:form "sample/alpha"]}
               {:opts {:form :alpha}
                :error-type :invalid-form-selector
                :field [:form :alpha]}
               {:opts {:line 0}
                :error-type :invalid-line
                :field [:line 0]}
               {:opts {:line -1}
                :error-type :invalid-line
                :field [:line -1]}
               {:opts {:line "five"}
                :error-type :invalid-line
                :field [:line "five"]}
               {:opts {:contains ""}
                :error-type :invalid-contains-selector
                :field [:contains ""]}
               {:opts {:contains "   "}
                :error-type :invalid-contains-selector
                :field [:contains "   "]}
               {:opts {:contains :needle}
                :error-type :invalid-contains-selector
                :field [:contains :needle]}
               {:opts {:form "alpha" :platform "clj"}
                :error-type :invalid-platform
                :field [:platform "clj"]}]]
    (doseq [{:keys [opts error-type field]} cases]
      (let [result (selector-result basic-source opts)]
        (is (= error-type (:error-type result)) (pr-str opts))
        (is (= (second field) (get result (first field))) (pr-str opts))
        (is (= :show-form (:operation result)))
        (is (= (structural-lens/source-hash basic-source)
               (:source-hash result)))))))

(deftest show-form-refuses-absent-and-gap-selectors-with-ls-remedy
  (doseq [[opts error-type]
          [[{:form "missing"} :form-not-found]
           [{:line 2}
            :line-not-in-form]]]
    (let [result (selector-result basic-source opts)
          remedy (get-in result [:remedies :list-forms])]
      (is (= error-type (:error-type result)))
      (is (zero? (:match-count result)))
      (is (= :ls (:operation remedy)))
      (is (= ["clj-surgeon" ":op" ":ls" ":file" "sample.clj"]
             (:command-args remedy)))
      (is (= "clj-surgeon :op :ls :file sample.clj" (:command remedy))))))

(deftest show-form-never-chooses-among-duplicate-names
  (let [source "(ns duplicate)\n(defn same [] 1)\n(defn same [] 2)\n"
        result (show-form/select-form "duplicate.clj" source {:form "same"})]
    (is (= :ambiguous-form (:error-type result)))
    (is (= 2 (:match-count result)))
    (is (= [{:type 'defn :name 'same :platforms [:clj] :line 2 :end-line 2}
            {:type 'defn :name 'same :platforms [:clj] :line 3 :end-line 3}]
           (:matches result)))
    (is (not (contains? result :source)))))

(deftest show-form-bounds-ambiguous-candidate-evidence
  (let [source (str "(ns many)\n"
                    (str/join "\n" (repeat 30 "(defn same [] :value)"))
                    "\n")
        result (show-form/select-form "many.clj" source {:form "same"})]
    (is (= :ambiguous-form (:error-type result)))
    (is (= 30 (:match-count result)))
    (is (= 10 (:candidate-limit result)))
    (is (true? (:matches-truncated? result)))
    (is (= 10 (count (:matches result)))))
  (testing "contains ambiguity uses the same bounded evidence contract"
    (let [source (str "(ns many-text)\n"
                      (str/join "\n"
                                (map #(str "(defn f" % " [] \"needle\")")
                                     (range 30)))
                      "\n")
          result (show-form/select-form "many-text.clj" source
                                        {:contains "needle"})]
      (is (= :ambiguous-form (:error-type result)))
      (is (= 30 (:match-count result)))
      (is (= 10 (:candidate-limit result)))
      (is (true? (:matches-truncated? result)))
      (is (= 10 (count (:matches result)))))))

(deftest show-form-preserves-reader-conditional-platforms
  (testing "name is ambiguous across platforms until the caller selects one"
    (let [ambiguous (show-form/select-form "sample.cljc" cljc-source
                                           {:form "shared"})
          clj-result (show-form/select-form "sample.cljc" cljc-source
                                            {:form "shared" :platform :clj})
          cljs-result (show-form/select-form "sample.cljc" cljc-source
                                             {:form "shared" :platform :cljs})]
      (is (= :ambiguous-form (:error-type ambiguous)))
      (is (= 2 (:match-count ambiguous)))
      (is (= [:clj] (:platforms clj-result)))
      (is (= "(defn shared [] :clj)" (:source clj-result)))
      (is (= [:cljs] (:platforms cljs-result)))
      (is (= "(defn shared [] :cljs)" (:source cljs-result)))))
  (testing "spliced conditionals use the same platform filter"
    (let [result (show-form/select-form "sample.cljc" cljc-source
                                        {:form "spliced" :platform :cljs})]
      (is (= [:cljs] (:platforms result)))
      (is (= "(defn spliced [] :cljs)" (:source result)))))
  (testing "shared form carries both platforms"
    (let [result (show-form/select-form "sample.cljc" cljc-source
                                        {:form "common" :platform :clj})]
      (is (= [:clj :cljs] (:platforms result)))
      (is (= "(defn common [] :both)" (:source result)))))
  (testing "contains preserves the same CLJC ambiguity and platform filter"
    (let [ambiguous (show-form/select-form "sample.cljc" cljc-source
                                           {:contains "defn shared"})
          cljs-result (show-form/select-form "sample.cljc" cljc-source
                                             {:contains "defn shared"
                                              :platform :cljs})]
      (is (= :ambiguous-form (:error-type ambiguous)))
      (is (= 2 (:match-count ambiguous)))
      (is (= [:cljs] (:platforms cljs-result)))
      (is (= "(defn shared [] :cljs)" (:source cljs-result)))))
  (testing "a platform with no candidate is an exact not-found refusal"
    (let [result (show-form/select-form "sample.cljc" cljc-source
                                        {:form "shared" :platform :bb})]
      (is (= :form-not-found (:error-type result)))
      (is (zero? (:match-count result))))))

(deftest show-form-line-selection-refuses-overlapping-reader-branch-ranges
  (let [line (line-containing cljc-source "#?(:clj")
        result (show-form/select-form "sample.cljc" cljc-source {:line line})]
    (is (= :line-not-in-form (:error-type result)))
    (is (zero? (:match-count result)))))

(deftest show-form-uses-the-source-extension-platform-for-cljs
  (let [result (show-form/select-form "sample.cljs"
                                      "(ns sample)\n(defn browser-only [] :cljs)\n"
                                      {:form "browser-only"})]
    (is (= [:cljs] (:platforms result)))
    (is (= 'browser-only (:name result)))))

(deftest show-form-uses-project-config-name-extractors-like-outline
  (let [source "(ns api)\n(defendpoint handle :get \"/items\" [request] request)\n"
        aliases {"defendpoint"
                 {:kind :defn
                  :fields {:name fields/->first-symbol}}}
        result (show-form/select-form "api.clj" source
                                      {:form "handle"
                                       :project-aliases aliases})]
    (is (= 'handle (:name result)))
    (is (= 'defendpoint (:type result)))
    (is (= "(defendpoint handle :get \"/items\" [request] request)"
           (:source result)))))

(deftest pure-selection-does-not-read-the-global-project-alias-atom
  (let [source "(ns api)\n(defendpoint handle :get \"/items\" [request] request)\n"]
    (with-redefs [forms/project-aliases
                  (atom {"defendpoint"
                         {:kind :defn
                          :fields {:name fields/->first-symbol}}})]
      (let [result (show-form/select-form "api.clj" source {:form "handle"})]
        (is (= :form-not-found (:error-type result)))))))

(deftest show-form-refuses-invalid-source-as-edn-data
  (let [source "(ns broken\n(defn x [] 1)"
        result (show-form/select-form "broken.clj" source {:form "x"})]
    (is (= :invalid-source (:error-type result)))
    (is (string? (:error result)))
    (is (= (structural-lens/source-hash source) (:source-hash result)))))

(deftest show-file-refuses-an-unreadable-path-as-edn-data
  (let [result (show-form/show-file
                 {:file "/definitely/missing/show-form.clj" :form "x"})]
    (is (= :file-read-failed (:error-type result)))
    (is (= :show-form (:operation result)))
    (is (str/includes? (:error result) "Cannot read source file"))))

(deftest show-file-snapshots-configured-project-aliases-at-the-io-boundary
  (let [tmp-dir (fs/create-temp-dir {:prefix "show-form-project-alias"})
        file (fs/path tmp-dir "api.clj")]
    (try
      (spit (str file)
            "(ns api)\n(defendpoint handle :get \"/items\" [request] request)\n")
      (with-redefs [forms/project-aliases
                    (atom {"defendpoint"
                           {:kind :defn
                            :fields {:name fields/->first-symbol}}})]
        (let [result (show-form/show-file {:file (str file) :form "handle"})]
          (is (= 'handle (:name result)))
          (is (= 'defendpoint (:type result)))))
      (finally
        (fs/delete-tree tmp-dir)))))

(deftest cross-file-cat-reads-each-snapshot-once-in-manifest-order
  (let [temp-dir (fs/create-temp-dir {:prefix "cross-file-cat-"})
        first-file (str (fs/path temp-dir "first.clj"))
        second-file (str (fs/path temp-dir "second.clj"))
        reads (atom [])
        original show-form/read-source
        spec {:reads [{:file second-file :forms ['second-b 'second-a]}
                      {:file first-file :forms ['first-a]}]
              :expect {:file-count 2 :form-count 3}}]
    (try
      (spit first-file "(ns first)\n(defn first-a [] :a)\n")
      (spit second-file
            "(ns second)\n(defn second-a [] :a)\n(defn second-b [] :b)\n")
      (with-redefs [show-form/read-source
                    (fn [file]
                      (swap! reads conj file)
                      (original file))]
        (let [result (show-form/show-files spec)]
          (is (= :show-form (:operation result)))
          (is (= {:spec :cross-file} (:selector result)))
          (is (= 2 (:file-count result)))
          (is (= 3 (:form-count result)))
          (is (= [second-file first-file] (mapv :file (:files result))))
          (is (= [['second-b 'second-a] ['first-a]]
                 (mapv #(mapv :name (:forms %)) (:files result))))
          (is (= [second-file first-file] @reads))
          (is (every? :source-hash (:files result)))))
      (finally
        (fs/delete-tree temp-dir)))))

(deftest cross-file-cat-validates-the-whole-contract-before-reading
  (let [temp-dir (fs/create-temp-dir {:prefix "cross-file-cat-contract-"})
        file (str (fs/path temp-dir "sample.clj"))
        alias-file (str (fs/path temp-dir "." "sample.clj"))
        valid-read {:file file :forms ['target]}
        cases [["root type" [] :invalid-read-spec]
               ["unknown root key"
                {:reads [valid-read] :expect {:file-count 1 :form-count 1}
                 :surprise true "also-surprise" true}
                :unknown-read-spec-keys]
               ["empty reads"
                {:reads [] :expect {:file-count 1 :form-count 1}}
                :invalid-reads]
               ["unknown entry key"
                {:reads [(assoc valid-read :line 1)]
                 :expect {:file-count 1 :form-count 1}}
                :invalid-read-entries]
               ["invalid forms"
                {:reads [(assoc valid-read :forms ['target "target"])]
                 :expect {:file-count 1 :form-count 2}}
                :invalid-read-entries]
               ["duplicate physical file"
                {:reads [valid-read {:file alias-file :forms ['other]}]
                 :expect {:file-count 2 :form-count 2}}
                :duplicate-read-files]
               ["incomplete expectation"
                {:reads [valid-read] :expect {:file-count 1}}
                :invalid-read-expectation]
               ["wrong expectation"
                {:reads [valid-read] :expect {:file-count 1 :form-count 2}}
                :read-expectation-mismatch]
               ["excessive limit"
                {:reads [valid-read] :expect {:file-count 1 :form-count 1}
                 :limits {:source-chars 65537}}
                :invalid-read-source-limit]
               ["explicit nil limits"
                {:reads [valid-read] :expect {:file-count 1 :form-count 1}
                 :limits nil}
                :invalid-read-limits]]]
    (try
      (spit file "(ns sample)\n(defn target [] :ok)\n")
      (with-redefs [show-form/read-source
                    (fn [_] (throw (ex-info "must validate before I/O" {})))]
        (doseq [[label spec error-type] cases]
          (testing label
            (let [result (show-form/show-files spec)]
              (is (= error-type (:error-type result)))
              (is (not-any? #(and (map? %) (contains? % :source))
                            (tree-seq coll? seq result)))))))
      (finally
        (fs/delete-tree temp-dir)))))

(deftest cross-file-cat-refuses-any-failed-owner-and-the-global-source-cap
  (let [temp-dir (fs/create-temp-dir {:prefix "cross-file-cat-refusal-"})
        first-file (str (fs/path temp-dir "first.clj"))
        second-file (str (fs/path temp-dir "second.clj"))
        payload (apply str (repeat 33000 "x"))]
    (try
      (spit first-file
            (str "(ns first)\n(def first-large " (pr-str payload) ")\n"))
      (spit second-file
            (str "(ns second)\n(def second-large " (pr-str payload) ")\n"))
      (let [missing-result
            (show-form/show-files
              {:reads [{:file first-file :forms ['first-large]}
                       {:file second-file :forms ['missing]}]
               :expect {:file-count 2 :form-count 2}})
            oversized-result
            (show-form/show-files
              {:reads [{:file first-file :forms ['first-large]}
                       {:file second-file :forms ['second-large]}]
               :expect {:file-count 2 :form-count 2}})]
        (is (= :read-transaction-failed (:error-type missing-result)))
        (is (= :batch-form-selection-failed
               (get-in missing-result [:failures 0 :error-type])))
        (is (= :read-source-limit-exceeded (:error-type oversized-result)))
        (is (= 65536 (:source-char-limit oversized-result)))
        (doseq [result [missing-result oversized-result]]
          (is (not (contains? result :files)))
          (is (not-any? #(and (map? %) (contains? % :source))
                        (tree-seq coll? seq result)))))
      (finally
        (fs/delete-tree temp-dir)))))

(deftest semantic-cross-file-cat-is-compact-ordered-and-explicitly-lossy
  (let [temp-dir (fs/create-temp-dir {:prefix "semantic-cross-file-cat-"})
        first-file (str (fs/path temp-dir "first.clj"))
        second-file (str (fs/path temp-dir "second.clj"))
        spec {:reads [{:file second-file :forms ['second-b 'second-a]}
                      {:file first-file :forms ['first-a]}]
              :expect {:file-count 2 :form-count 3}}]
    (try
      (spit first-file "(ns first)\n;; lexical comment\n(defn first-a [] #(+ % 1))\n")
      (spit second-file
            "(ns second)\n(defn second-a [] :a)\n(defn second-b [] :b)\n")
      (let [exact-result (show-form/show {:spec spec})
            semantic-result (show-form/show {:spec spec :format :semantic})]
        (is (map? exact-result))
        (is (string? semantic-result))
        (is (str/starts-with? semantic-result "CLJ-SURGEON-SEMANTIC "))
        (is (< (.indexOf semantic-result (str "FILE 1 " (pr-str second-file)))
               (.indexOf semantic-result (str "FILE 2 " (pr-str first-file)))))
        (is (< (.indexOf semantic-result "FORM 1 second-b ")
               (.indexOf semantic-result "FORM 2 second-a ")))
        (is (str/includes? semantic-result "(fn* [p1__"))
        (is (not (str/includes? semantic-result "lexical comment")))
        (is (not (str/includes? semantic-result "#(+ % 1)")))
        (is (every? #(str/includes? semantic-result (:source-hash %))
                    (:files exact-result)))
        (is (< (count semantic-result) (count (pr-str exact-result)))))
      (finally
        (fs/delete-tree temp-dir)))))

(deftest semantic-render-refuses-before-returning-malformed-or-oversized-output
  (let [base {:operation :show-form
              :file-count 1
              :form-count 1
              :source-char-count 1
              :files [{:file "sample.clj"
                       :source-hash "hash"
                       :form-count 1
                       :forms [{:name 'sample
                                :type 'def
                                :line 1
                                :end-line 1
                                :source "("}]}]}
        malformed (show-form/render-semantic base)
        payload (apply str (repeat 65520 "x"))
        oversized (show-form/render-semantic
                    (-> base
                        (assoc :source-char-count (count payload))
                        (assoc-in [:files 0 :forms 0 :source]
                                  (str "(def sample " (pr-str payload) ")"))))]
    (is (= :semantic-render-failed (:error-type malformed)))
    (is (= :semantic-output-limit-exceeded (:error-type oversized)))
    (doseq [result [malformed oversized]]
      (is (map? result))
      (is (not (contains? result :output)))
      (is (not-any? #(and (map? %) (contains? % :source))
                    (tree-seq coll? seq result))))))

(deftest read-format-is-explicit-and-cross-file-only
  (let [spec {:reads [{:file "sample.clj" :forms ['sample]}]
              :expect {:file-count 1 :form-count 1}}]
    (is (= :invalid-read-format
           (:error-type (show-form/show {:spec spec :format :brief}))))
    (is (= :read-format-requires-spec
           (:error-type
             (show-form/show {:file "sample.clj"
                              :form 'sample
                              :format :semantic}))))))

(deftest invocation-remedy-is-narrow-executable-and-space-safe
  (let [opts {:file "dir with space/state.clj" :form "alpha"}
        remedy (show-form/invocation-remedy opts)]
    (is (= :cat (:operation remedy)))
    (is (= "Read one named top-level form" (:reason remedy)))
    (is (= ["clj-surgeon" ":op" ":cat"
            ":file" "dir with space/state.clj" ":form" "alpha"]
           (:command-args remedy)))
    (is (= "clj-surgeon :op :cat :file 'dir with space/state.clj' :form alpha"
           (:command remedy))))
  (testing "a valid named batch remains one executable argument"
    (let [remedy (show-form/invocation-remedy
                   {:file "state.clj" :forms ['alpha 'omega]})]
      (is (= "Read several named top-level forms from one snapshot"
             (:reason remedy)))
      (is (= ["clj-surgeon" ":op" ":cat" ":file" "state.clj"
              ":forms" "[alpha omega]"]
             (:command-args remedy)))
      (is (= "clj-surgeon :op :cat :file state.clj :forms '[alpha omega]'"
             (:command remedy)))))
  (is (nil? (show-form/invocation-remedy {:file "x.clj"})))
  (is (nil? (show-form/invocation-remedy {:file "x.clj" :form "x" :line 2})))
  (testing "only remedies that can succeed are emitted"
    (is (some? (show-form/invocation-remedy {:file "x.clj" :line "12"})))
    (is (= "clj-surgeon :op :cat :file x.clj :contains 'distinctive text'"
           (:command (show-form/invocation-remedy
                       {:file "x.clj" :contains "distinctive text"}))))
    (is (nil? (show-form/invocation-remedy {:file "x.clj" :contains "   "})))
    (is (nil? (show-form/invocation-remedy {:file "x.clj" :line "nope"})))
    (is (nil? (show-form/invocation-remedy
                {:file "x.clj" :line "999999999999999999999999"})))
    (is (nil? (show-form/invocation-remedy
                {:file "x.clj" :form "qualified/name"})))
    (is (nil? (show-form/invocation-remedy
                {:file "x.clj" :forms ['alpha 'alpha]})))
    (is (nil? (show-form/invocation-remedy
                {:file "x.clj" :form "name" :platform "clj"}))))
  (testing "shell metacharacters in Clojure names are always quoted"
    (is (= "clj-surgeon :op :cat :file state.clj :form '*state*'"
           (:command (show-form/invocation-remedy
                       {:file "state.clj" :form "*state*"}))))
    (is (= "clj-surgeon :op :cat :file state.clj :form 'ready?'"
           (:command (show-form/invocation-remedy
                       {:file "state.clj" :form "ready?"}))))
    (is (= "clj-surgeon :op :cat :file state.clj :form 'source->target'"
           (:command (show-form/invocation-remedy
                       {:file "state.clj" :form "source->target"}))))))

(deftest rendered-form-remedy-survives-zsh-metacharacter-parsing
  (let [tmp-dir (fs/create-temp-dir {:prefix "show form shell quote "})
        file (fs/path tmp-dir "names.clj")
        redirected-file (fs/path tmp-dir "target")]
    (try
      (spit (str file) "(ns shell.names)\n(defn source->target [] :ok)\n")
      (let [command (:command
                      (show-form/invocation-remedy
                        {:file (str file) :form "source->target"}))
            result @(proc/process ["zsh" "-c" command]
                                  {:dir (str tmp-dir)
                                   :err :string
                                   :out :string})
            receipt (edn/read-string (:out result))]
        (is (zero? (:exit result)) (:err result))
        (is (= 'source->target (:name receipt)))
        (is (not (fs/exists? redirected-file))))
      (finally
        (fs/delete-tree tmp-dir)))))

(def ^:private project-root
  (.getCanonicalPath (io/file ".")))

(defn- run-cli
  [& args]
  @(proc/process
     (into ["bb" "-m" "clj-surgeon.core"] args)
     {:dir project-root :err :string :out :string}))

(deftest cli-show-form-selectors-and-cat-contains-are-one-shot-edn-reads
  (let [tmp-dir (fs/create-temp-dir {:prefix "show form cli "})
        file (fs/path tmp-dir "migration fixture.clj")
        source "(ns field.case)\n\n;; :target\n(defn target [x]\n  (inc x))\n\n(defn other [] :other)\n"]
    (try
      (spit (str file) source)
      (doseq [[op selector] [[":show-form" [":form" "target"]]
                             [":show-form" [":line" (str (line-containing source "(inc x)"))]]
                             [":show-form" [":contains" "(inc x)"]]
                             [":cat" [":contains" "(inc x)"]]
                             [":cat" [":contains" ":target"]]]]
        (let [{:keys [exit out err]} (apply run-cli ":op" op
                                            ":file" (str file) selector)
              result (edn/read-string out)]
          (is (zero? exit) err)
          (is (= :show-form (:operation result)))
          (is (= 'target (:name result)))
          (is (= "(defn target [x]\n  (inc x))" (:source result)))
          (is (str/blank? err))))
      (testing "cat reads several named forms in one CLI call"
        (let [{:keys [exit out err]}
              (run-cli ":op" ":cat" ":file" (str file)
                       ":forms" "[target other]")
              result (edn/read-string out)]
          (is (zero? exit) err)
          (is (= {:forms ['target 'other]} (:selector result)))
          (is (= ['target 'other] (mapv :name (:forms result))))
          (is (= ["(defn target [x]\n  (inc x))" "(defn other [] :other)"]
                 (mapv :source (:forms result))))
          (is (str/blank? err))))
      (testing "cat returns no partial source when one batch name is absent"
        (let [{:keys [exit out err]}
              (run-cli ":op" ":cat" ":file" (str file)
                       ":forms" "[target missing]")
              result (edn/read-string out)]
          (is (pos? exit))
          (is (= :batch-form-selection-failed (:error-type result)))
          (is (= ['missing] (mapv :form (:failures result))))
          (is (not (contains? result :forms)))
          (is (nil? (get-in result [:remedies :cat])))
          (is (not-any? #(and (map? %) (contains? % :source))
                        (tree-seq coll? seq result)))
          (is (str/blank? err))))
      (finally
        (fs/delete-tree tmp-dir)))))

(deftest cli-cross-file-cat-loads-one-manifest-from-file-or-stdin
  (let [temp-dir (fs/create-temp-dir {:prefix "cross-file-cat-cli-"})
        first-file (str (fs/path temp-dir "first.clj"))
        second-file (str (fs/path temp-dir "second.clj"))
        spec-file (str (fs/path temp-dir "read.edn"))
        spec {:reads [{:file first-file :forms ['first-a]}
                      {:file second-file :forms ['second-a 'second-b]}]
              :expect {:file-count 2 :form-count 3}
              :limits {:source-chars 4096}}
        run-with-input
        (fn [args input]
          @(proc/process (into ["bb" "-m" "clj-surgeon.core"] args)
                         (cond-> {:out :string :err :string}
                           input (assoc :in input))))]
    (try
      (spit first-file "(ns first)\n(defn first-a [] :a)\n")
      (spit second-file
            "(ns second)\n(defn second-a [] :a)\n(defn second-b [] :b)\n")
      (spit spec-file (pr-str spec))
      (doseq [[label args input]
              [["file" [":op" ":cat" ":spec-file" spec-file] nil]
               ["stdin" [":op" ":cat" ":spec-file" "-"] (pr-str spec)]]]
        (testing label
          (let [{:keys [exit out err]} (run-with-input args input)
                result (edn/read-string out)]
            (is (zero? exit) err)
            (is (= 2 (:file-count result)))
            (is (= 3 (:form-count result)))
            (is (= [first-file second-file] (mapv :file (:files result))))
            (is (str/blank? err)))))
      (testing "semantic stdin is one noninteractive source-complete call"
        (let [{:keys [exit out err]}
              (run-with-input [":op" ":cat" ":spec-file" "-"
                               ":format" ":semantic"]
                              (pr-str spec))]
          (is (zero? exit) err)
          (is (str/starts-with? out "CLJ-SURGEON-SEMANTIC "))
          (is (= 3 (count (re-seq #"(?m)^FORM " out))))
          (is (< (.indexOf out " first-a ")
                 (.indexOf out " second-a ")
                 (.indexOf out " second-b ")))
          (is (str/includes? out "(defn first-a [] :a)"))
          (is (str/blank? err))))
      (doseq [[label args input error-type]
              [["empty stdin" [":op" ":cat" ":spec-file" "-"] ""
                :missing-spec-stdin]
               ["trailing form" [":op" ":cat" ":spec-file" "-"]
                (str (pr-str spec) "\n:extra") :invalid-spec-document]
               ["direct selector conflict"
                [":op" ":cat" ":spec" (pr-str spec)
                 ":file" first-file :forms "[first-a]"]
                nil :conflicting-read-inputs]]]
        (testing label
          (let [{:keys [exit out err]} (run-with-input args input)
                result (edn/read-string out)]
            (is (pos? exit))
            (is (= error-type (:error-type result)))
            (when (= :missing-spec-stdin error-type)
              (is (str/includes? (:remedy result) "printf"))
              (is (str/includes? (:remedy result) ":spec-file -")))
            (is (str/blank? err)))))
      (finally
        (fs/delete-tree temp-dir)))))

(deftest cli-show-form-refusals-and-historical-guesses-have-actionable-exits
  (let [tmp-dir (fs/create-temp-dir {:prefix "show form remedy "})
        file (fs/path tmp-dir "state file.clj")]
    (try
      (spit (str file) "(ns refusal)\n(defn target [] :ok)\n")
      (testing "show-form refusal is nonzero EDN without stack trace"
        (let [{:keys [exit out err]}
              (run-cli ":op" ":show-form" ":file" (str file))
              result (edn/read-string out)]
          (is (pos? exit))
          (is (= :missing-selector (:error-type result)))
          (is (str/blank? err))))
      (testing "dispatch-level missing-file refusal retains canonical context"
        (let [{:keys [exit out err]}
              (run-cli ":op" ":cat" ":form" "target")
              result (edn/read-string out)]
          (is (pos? exit))
          (is (= :show-form (:operation result)))
          (is (= {:form 'target} (:selector result)))
          (is (= [:file] (:missing result)))
          (is (= :missing-arguments (:error-type result)))
          (is (str/blank? err))))
      (testing ":cat is a strict alias with the canonical machine identity"
        (let [{:keys [exit out err]}
              (run-cli ":op" ":cat" ":file" (str file) ":form" "target")
              result (edn/read-string out)
              canonical-result (-> (run-cli ":op" ":show-form" ":file"
                                            (str file) ":form" "target")
                                   :out
                                   edn/read-string)]
          (is (zero? exit))
          (is (= :show-form (:operation result)))
          (is (= {:form 'target} (:selector result)))
          (is (= 'target (:name result)))
          (is (= (:source canonical-result) (:source result)))
          (is (= (:source-hash canonical-result) (:source-hash result)))
          (is (str/blank? err))))
      (testing "bare :cat refuses rather than dumping the complete file"
        (let [{:keys [exit out err]}
              (run-cli ":op" ":cat" ":file" (str file))
              result (edn/read-string out)]
          (is (pos? exit))
          (is (= :show-form (:operation result)))
          (is (= :missing-selector (:error-type result)))
          (is (str/blank? err))))
      (testing "CLI line coercion is local and overflow refuses as an invalid selector"
        (let [{:keys [exit out err]}
              (run-cli ":op" ":show-form" ":file" (str file)
                       ":line" "999999999999999999999999")
              result (edn/read-string out)]
          (is (pos? exit))
          (is (= :invalid-line (:error-type result)))
          (is (= "999999999999999999999999" (:line result)))
          (is (str/blank? err))))
      (testing "unknown :get recommends the exact named-form command"
        (let [{:keys [exit out err]}
              (run-cli ":op" ":get" ":file" (str file) ":form" "target")
              result (edn/read-string out)
              remedy (get-in result [:remedies :cat])
              recovered (apply run-cli (rest (:command-args remedy)))
              recovered-result (edn/read-string (:out recovered))]
          (is (pos? exit))
          (is (= :unknown-operation (:error-type result)))
          (is (= :cat (:operation remedy)))
          (is (str/includes? (:command remedy) ":op :cat"))
          (is (zero? (:exit recovered)) (:err recovered))
          (is (= 'target (:name recovered-result)))
          (is (str/blank? err))))
      (testing "unknown :get repairs the observed :name spelling"
        (let [{:keys [exit out err]}
              (run-cli ":op" ":get" ":file" (str file) ":name" "target")
              result (edn/read-string out)
              remedy (get-in result [:remedies :cat])
              recovered (apply run-cli (rest (:command-args remedy)))]
          (is (pos? exit))
          (is (= :unknown-operation (:error-type result)))
          (is (= ["clj-surgeon" ":op" ":cat" ":file" (str file)
                  ":form" "target"]
                 (:command-args remedy)))
          (is (zero? (:exit recovered)) (:err recovered))
          (is (str/blank? err))))
      (testing "legacy show-form repairs the observed :name selector"
        (let [{:keys [exit out err]}
              (run-cli ":op" ":show-form" ":file" (str file) ":name" "target")
              result (edn/read-string out)
              remedy (get-in result [:remedies :cat])
              recovered (apply run-cli (rest (:command-args remedy)))]
          (is (pos? exit))
          (is (= :missing-selector (:error-type result)))
          (is (= :cat (:operation remedy)))
          (is (= ":form" (nth (:command-args remedy) 5)))
          (is (zero? (:exit recovered)) (:err recovered))
          (is (str/blank? err))))
      (testing "line-only find-subform preserves its error and recommends cat"
        (let [{:keys [exit out err]}
              (run-cli ":op" ":find-subform" ":file" (str file)
                       ":line" "2")
              result (edn/read-string out)]
          (is (pos? exit))
          (is (= :missing-arguments (:error-type result)))
          (is (= [:match] (:missing result)))
          (is (= :cat (get-in result [:remedies :cat :operation])))
          (is (str/blank? err))))
      (testing "structural pattern spelling repairs to match-form"
        (let [{:keys [exit out err]}
              (run-cli ":op" ":grep-form" ":file" (str file)
                       ":pattern" "(defn _ [] :ok)")
              result (edn/read-string out)
              remedy (get-in result [:remedies :match-form])
              recovered (apply run-cli (rest (:command-args remedy)))
              recovered-result (edn/read-string (:out recovered))]
          (is (pos? exit))
          (is (= :missing-arguments (:error-type result)))
          (is (= :match-form (:operation remedy)))
          (is (str/includes? (:command remedy) ":op :match-form"))
          (is (str/includes? (:command remedy) ":match '(defn _ [] :ok)'"))
          (is (zero? (:exit recovered)) (:err recovered))
          (is (= 1 (:match-count recovered-result)))
          (is (str/blank? err))))
      (testing "grep-form does not mislabel regex alternation as a structural pattern"
        (let [{:keys [exit out err]}
              (run-cli ":op" ":grep-form" ":file" (str file)
                       ":pattern" "target|missing")
              result (edn/read-string out)
              remedy (get-in result [:remedies :text-search])]
          (is (pos? exit))
          (is (= :missing-arguments (:error-type result)))
          (is (nil? (get-in result [:remedies :grep-form])))
          (is (= :text-search (:operation remedy)))
          (is (str/includes? (:reason remedy) "not a regular expression"))
          (is (= ["rg" "-n" "--max-count" "20" "target|missing" (str file)]
                 (:command-args remedy)))
          (is (str/blank? err))))
      (finally
        (fs/delete-tree tmp-dir)))))

(deftest cli-selection-refusal-matrix-is-stable-edn-with-nonzero-exits
  (let [tmp-dir (fs/create-temp-dir {:prefix "show-form-refusal-matrix"})
        file (fs/path tmp-dir "duplicates.clj")
        invalid-file (fs/path tmp-dir "invalid.clj")
        absent-file (fs/path tmp-dir "absent.clj")]
    (try
      (spit (str file) "(ns duplicate)\n\n(defn same [] 1)\n(defn same [] 2)\n")
      (spit (str invalid-file) "(ns invalid\n(defn broken [] 1)\n")
      (doseq [{:keys [label args error-type]}
              [{:label "conflicting selectors"
                :args [":file" (str file) ":form" "same" ":line" "3"]
                :error-type :conflicting-selectors}
               {:label "contains conflict"
                :args [":file" (str file) ":form" "same" ":contains" "same"]
                :error-type :conflicting-selectors}
               {:label "qualified form"
                :args [":file" (str file) ":form" "other/same"]
                :error-type :invalid-form-selector}
               {:label "nonpositive line"
                :args [":file" (str file) ":line" "0"]
                :error-type :invalid-line}
               {:label "blank contains"
                :args [":file" (str file) ":contains" "   "]
                :error-type :invalid-contains-selector}
               {:label "nonkeyword platform"
                :args [":file" (str file) ":form" "same" ":platform" "clj"]
                :error-type :invalid-platform}
               {:label "absent name"
                :args [":file" (str file) ":form" "missing"]
                :error-type :form-not-found}
               {:label "line between forms"
                :args [":file" (str file) ":line" "2"]
                :error-type :line-not-in-form}
               {:label "duplicate name"
                :args [":file" (str file) ":form" "same"]
                :error-type :ambiguous-form}
               {:label "contains absent"
                :args [":file" (str file) ":contains" "missing text"]
                :error-type :contains-not-found}
               {:label "contains ambiguous"
                :args [":file" (str file) ":contains" "defn same"]
                :error-type :ambiguous-form}
               {:label "invalid source"
                :args [":file" (str invalid-file) ":form" "broken"]
                :error-type :invalid-source}
               {:label "unreadable source"
                :args [":file" (str absent-file) ":form" "missing"]
                :error-type :file-read-failed}]]
        (let [{:keys [exit out err]} (apply run-cli ":op" ":show-form" args)
              result (edn/read-string out)]
          (is (pos? exit) label)
          (is (= :show-form (:operation result)) label)
          (is (= error-type (:error-type result)) label)
          (is (str/blank? err) label)))
      (finally
        (fs/delete-tree tmp-dir)))))

(deftest cli-match-form-and-grep-form-aliases-are-one-shot-structural-search
  (let [file "test/fixtures/show_form_migration.cljc"
        match "(json/write-value-as-string _)"
        canonical (run-cli ":op" ":find-subform" ":file" file ":match" match)
        alias (run-cli ":op" ":match-form" ":file" file ":match" match)
        grep-alias (run-cli ":op" ":grep-form" ":file" file ":match" match)
        canonical-result (edn/read-string (:out canonical))
        alias-result (edn/read-string (:out alias))
        grep-result (edn/read-string (:out grep-alias))]
    (is (zero? (:exit alias)) (:err alias))
    (is (= 1 (:match-count alias-result)))
    (is (nil? (:inside alias-result)))
    (is (= "upsert-starred-post!"
           (get-in alias-result [:matches 0 :inside])))
    (is (= (:matches canonical-result) (:matches alias-result)))
    (is (= (:source-hash canonical-result) (:source-hash alias-result)))
    (is (= (:matches canonical-result) (:matches grep-result)))
    (is (= (:source-hash canonical-result) (:source-hash grep-result)))))

(deftest real-program-derived-fixture-supports-both-motivating-reads
  (let [file "test/fixtures/show_form_migration.cljc"
        source (slurp file)
        target-line (line-containing source "on conflict")
        by-name (show-form/select-form file source {:form "upsert-starred-post!"})
        by-line (show-form/select-form file source {:line target-line})
        by-contains (show-form/select-form file source {:contains "on conflict"})]
    (is (= 'upsert-starred-post! (:name by-name)))
    (is (= (:source by-name) (:source by-line)))
    (is (= (:source by-name) (:source by-contains)))
    (is (= 1 (:occurrence-count by-contains)))
    (is (= [:clj :cljs] (:platforms by-name)))
    (is (str/includes? (:source by-name) "on conflict"))
    (is (str/includes? (:source by-name) "json/write-value-as-string"))))

(deftest current-core-distinctive-help-phrase-is-a-one-shot-self-hosting-read
  (let [file "src/clj_surgeon/core.clj"
        result (show-form/select-form file (slurp file)
                                      {:contains "Per-command help"})]
    (is (= 'format-op-help (:name result)))
    (is (= 1 (:occurrence-count result)))
    (is (str/includes? (:source result) "Per-command help"))))

(deftest current-show-form-implementation-is-a-one-shot-batch-read
  (let [result (show-form/show-file
                 {:file "src/clj_surgeon/show_form.clj"
                  :forms ['select-form 'show-file]})]
    (is (= 2 (:form-count result)))
    (is (= ['select-form 'show-file] (mapv :name (:forms result))))
    (is (every? #(str/starts-with? (:source %) "(defn") (:forms result)))
    (is (not (contains? result :source)))))

(deftest agent-facing-surfaces-do-not-drift
  (let [readme (slurp "README.md")
        skill (str (slurp "skills/clj-surgeon/SKILL.md")
                "\n"
                (slurp "skills/clj-surgeon/references/cli-fallback.md"))
        legacy-skill (str (slurp "skill.md")
                       "\n"
                       (slurp "skills/clj-surgeon/references/cli-fallback.md"))
        changelog (slurp "CHANGELOG.md")
        help (core/format-op-help
               :show-form
               (get core/ops-registry :show-form))
        apply-help (core/format-op-help
                     :replace-subform!
                     (get core/ops-registry :replace-subform!))
        plan-help (core/format-op-help
                    :replace-subform
                    (get core/ops-registry :replace-subform))
        find-help (core/format-op-help
                    :find-subform
                    (get core/ops-registry :find-subform))]
    (doseq [[surface text] {"README" readme
                            "changelog" changelog}]
      (is (str/includes? text ":show-form") surface))
    (is (str/includes? help "Compatibility aliases: show-form")
        "cat help")
    (doseq [[surface text] {"installed skill" skill
                            "legacy skill" legacy-skill}]
      (is (str/includes? text ":cat") surface)
      (is (not (str/includes? text ":show-form")) surface))
    (doseq [[surface text] {"README" readme
                            "installed skill" skill
                            "legacy skill" legacy-skill
                            "changelog" changelog
                            "show-form help" help}]
      (is (str/includes? text ":contains") surface))
    (doseq [[surface text] {"README" readme
                            "installed skill" skill
                            "legacy skill" legacy-skill
                            "changelog" changelog
                            "show-form help" help}]
      (is (str/includes? text ":forms") surface))
    (doseq [[surface text] {"README" readme
                            "installed skill" skill
                            "legacy skill" legacy-skill
                            "changelog" changelog
                            "show-form help" help}]
      (let [normalized (str/replace text #"\s+" " ")]
        (is (str/includes? normalized ":spec-file -") surface)
        (is (str/includes? normalized ":file-count") surface)
        (is (str/includes? normalized ":form-count") surface)
        (is (str/includes? normalized ":format :semantic") surface)
        (is (str/includes? normalized "printf") surface)
        (is (str/includes? normalized "comments") surface)
        (is (str/includes? normalized "layout") surface)))
    (doseq [[surface text] {"README" readme
                            "installed skill" skill
                            "legacy skill" legacy-skill
                            "changelog" changelog}]
      (is (str/includes? text ":cat") surface)
      (when-not (= "changelog" surface)
        (is (not (str/includes? text ":get"))
            (str surface " must not teach unsupported :get"))))
    (is (str/includes? help "clj-surgeon :op cat") "cat help")
    (doseq [[surface text] {"README" readme
                            "installed skill" skill
                            "legacy skill" legacy-skill
                            "changelog" changelog
                            "find-subform help" find-help}]
      (is (str/includes? text ":match-form") surface))
    (doseq [[surface text] {"README" readme
                            "installed skill" skill
                            "legacy skill" legacy-skill
                            "replace-subform! help" apply-help}]
      (let [normalized (str/replace text #"\s+" " ")]
        (is (str/includes? normalized "Do not edit the plan") surface)
        (is (str/includes? normalized "generate a new plan") surface)
        (is (str/includes? normalized "read-back") surface)
        (is (str/includes? normalized ":verified") surface)))
    (doseq [[surface text] {"README" readme
                            "installed skill" skill
                            "legacy skill" legacy-skill
                            "replace-subform help" plan-help
                            "replace-subform! help" apply-help}]
      (let [normalized (-> text (str/replace #"\s+" " ") str/lower-case)]
        (is (str/includes? normalized "never chain") surface)
        (is (str/includes? normalized "plan generation") surface)))
    (doseq [[surface text] {"README" readme
                            "installed skill" skill
                            "legacy skill" legacy-skill
                            "replace-subform help" plan-help}]
      (let [normalized (-> text (str/replace #"\s+" " ") str/lower-case)]
        (is (str/includes? normalized "case") surface)
        (is (str/includes? normalized "binding pair") surface)
        (is (str/includes? normalized "not a synthetic wrapper list") surface)))
    (doseq [[surface text] {"README" readme
                            "installed skill" skill
                            "legacy skill" legacy-skill
                            "show-form help" help}]
      (let [normalized (-> text
                           (str/replace #"\s+" " ")
                           (str/replace "`" "")
                           str/lower-case)]
        (is (str/includes? normalized "instead of reconstructing a sed range")
            surface)
        (is (str/includes? normalized "do not run :ls solely as a preflight")
            surface)
        (is (str/includes? normalized ":contains") surface)
        (is (str/includes? normalized "distinctive text") surface)))
    (is (not (str/includes? help "rg -n"))
        "show-form help must teach the one-call contains route")))
