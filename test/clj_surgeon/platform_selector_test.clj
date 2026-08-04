(ns clj-surgeon.platform-selector-test
  (:require
   [babashka.fs :as fs]
   [babashka.process :as proc]
   [clj-surgeon.edit-dsl :as dsl]
   [clj-surgeon.structural-lens :as lens]
   [clojure.edn :as edn]
   [clojure.test :refer [deftest is testing]]))

(def ^:private ordinary-source
  "(ns platform.ordinary)\n\n;; Preserve ordinary source evidence.\n(defn shared [] :shared)\n")

(deftest ordinary-form-platform-selection-is-file-aware
  (doseq [{:keys [file platform expected-count]}
          [{:file "src/platform/ordinary.clj"
            :platform :clj
            :expected-count 1}
           {:file "src/platform/ordinary.clj"
            :platform :cljs
            :expected-count 0}
           {:file "src/platform/ordinary.cljs"
            :platform :clj
            :expected-count 0}
           {:file "src/platform/ordinary.cljs"
            :platform :cljs
            :expected-count 1}
           {:file "src/platform/ordinary.cljc"
            :platform :clj
            :expected-count 1}
           {:file "src/platform/ordinary.cljc"
            :platform :cljs
            :expected-count 1}]]
    (testing (str file " selected as " platform)
      (let [result (lens/evaluate-query
                     ordinary-source
                     [[:form 'shared platform]]
                     {:file file})]
        (is (nil? (:error result)))
        (is (= expected-count (:match-count result)))
        (is (= (if (= 1 expected-count)
                 ["(defn shared [] :shared)"]
                 [])
               (mapv :source (:matches result))))
        (is (= [expected-count]
               (mapv :output-count (:trace result))))))))

(def ^:private conditional-source
  (str "(ns platform.conditional)\n"
       "(def shared :ordinary)\n"
       "#?(:clj (def ^:private direct :clj)\n"
       "   :cljs (def direct :cljs))\n"
       "#?@(:clj [(def duplicate :clj-first)\n"
       "          ;; Preserve duplicate branch order.\n"
       "          (def duplicate :clj-second)\n"
       "          (def spliced :clj)]\n"
       "    :cljs [(def duplicate :cljs)\n"
       "           (def spliced :cljs)])\n"))

(deftest cljc-reader-conditional-selection-preserves-ambiguity-and-order
  (doseq [{:keys [label query expected]}
          [{:label "ordinary shared form on clj"
            :query [[:form 'shared :clj]]
            :expected ["(def shared :ordinary)"]}
           {:label "ordinary shared form on cljs"
            :query [[:form 'shared :cljs]]
            :expected ["(def shared :ordinary)"]}
           {:label "unqualified direct branches remain honestly ambiguous"
            :query [[:form 'direct]]
            :expected ["(def ^:private direct :clj)"
                       "(def direct :cljs)"]}
           {:label "direct clj branch"
            :query [[:form 'direct :clj]]
            :expected ["(def ^:private direct :clj)"]}
           {:label "direct cljs branch"
            :query [[:form 'direct :cljs]]
            :expected ["(def direct :cljs)"]}
           {:label "duplicate clj splice stays duplicate and ordered"
            :query [[:form 'duplicate :clj]]
            :expected ["(def duplicate :clj-first)"
                       "(def duplicate :clj-second)"]}
           {:label "cljs splice selects one branch-local form"
            :query [[:form 'spliced :cljs]]
            :expected ["(def spliced :cljs)"]}
           {:label "missing platform is exact zero evidence"
            :query [[:form 'direct :bb]]
            :expected []}]]
    (testing label
      (let [result (lens/evaluate-query conditional-source query
                                        {:file "src/platform/conditional.cljc"})]
        (is (nil? (:error result)))
        (is (= expected (mapv :source (:matches result))))
        (is (= (count expected) (:match-count result)))
        (is (= (count expected) (get-in result [:trace 0 :output-count])))))))

(deftest reader-conditionals-cannot-override-a-plain-file-platform
  (doseq [[file platform expected]
          [["src/platform/conditional.clj" :cljs []]
           ["src/platform/conditional.cljs" :clj []]
           ["src/platform/conditional.clj" :clj ["(def ^:private direct :clj)"]]
           ["src/platform/conditional.cljs" :cljs ["(def direct :cljs)"]]]]
    (let [result (lens/evaluate-query conditional-source
                                      [[:form 'direct platform]]
                                      {:file file})]
      (is (= expected (mapv :source (:matches result)))))))

(deftest platform-selector-refuses-unknowable-file-context
  (doseq [opts [nil {} {:file "source.edn"} {:file "source"}]]
    (let [result (lens/evaluate-query ordinary-source
                                      [[:form 'shared :clj]]
                                      opts)]
      (is (= :platform-context-required (:error-type result)))
      (is (= 0 (:match-count result)))
      (is (= [] (:matches result)))
      (is (= [".clj" ".cljs" ".cljc"]
             (:supported-file-extensions result))))))

(deftest xray-passes-file-context-through-literal-and-computed-reads
  (doseq [{:keys [label xray expected-count expected-value]}
          [{:label "literal cross-platform selection"
            :xray (dsl/compile-xray "(form 'shared :cljs)")
            :expected-count 0}
           {:label "computed actual-platform selection"
            :xray (dsl/xray [[:form 'shared :clj]] count)
            :expected-count 1
            :expected-value 1}]]
    (testing label
      (let [result (dsl/evaluate-xray ordinary-source
                                      {:file "src/platform/ordinary.clj"
                                       :expression label
                                       :xray xray})]
        (is (nil? (:error result)))
        (is (= expected-count (:match-count result)))
        (is (= expected-value (:value result)))))))

(def ^:private project-root
  (str (fs/normalize (fs/path (System/getProperty "user.dir")))))

(deftest cli-platform-selector-refusal-is-structured-edn
  (let [tmp-dir (fs/create-temp-dir {:prefix "clj-surgeon-platform-context-"})
        source-file (fs/path tmp-dir "source.edn")]
    (try
      (spit (str source-file) ordinary-source)
      (let [run @(proc/process
                   ["bb" "-m" "clj-surgeon.core"
                    ":op" ":xray"
                    ":file" (str source-file)
                    ":expr" "(form 'shared :clj)"]
                   {:dir project-root :err :string :out :string})
            result (edn/read-string (:out run))]
        (is (pos? (:exit run)))
        (is (= :platform-context-required (:error-type result)))
        (is (= (str source-file) (:file result)))
        (is (= [".clj" ".cljs" ".cljc"]
               (:supported-file-extensions result)))
        (is (not (re-find #"Exception|Stacktrace" (:err run)))))
      (finally
        (fs/delete-tree tmp-dir)))))
