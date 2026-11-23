(ns clj-surgeon.alias-migration-characterization-test
  (:require
   [clj-surgeon.alias-migration :as alias-migration]
   [clj-surgeon.alias-migration-fixture :as fixture]
   [clojure.string :as str]
   [clojure.test :refer [deftest is testing]]
   [rewrite-clj.node :as n]
   [rewrite-clj.parser :as parser]))

(def context
  {:mode :var :from-lib "old.lib" :to-lib "new.lib"
   :from-var "x" :to-var "y" :qualifiers #{"old" "old.lib"}
   :alias "new" :rewrite-bare #{"x"} :count-bare #{"x"} :platform ":clj"})

(deftest rewrite-forms-branches-and-positions
  (doseq [[source expected tallies overrides live]
          [["#_(old/x x)" "#_(old/x x)" {}]
           ["#'old/x" "#'new/y" {:sites 1}]
           ["(var old/x)" "(var new/y)" {:sites 1}]
           ["(old/x 1)" "(new/y 1)" {:sites 1}]
           ["(identity old/x)" "(identity new/y)" {:sites 1}]
           ["(let [x (x)] [x old/x])" "(let [x (new/y)] [x new/y])"
            {:sites 2 :refer-sites 1}]
           ["(binding [old/x x] old/x)" "(binding [new/y new/y] new/y)"
            {:sites 3 :refer-sites 1}]
           ["{old/x x}" "{new/y new/y}" {:sites 2 :refer-sites 1}]
           ["[old/x x]" "[new/y new/y]" {:sites 2 :refer-sites 1}]
           ["^{:tag old/x} x" "^{:tag new/y} new/y" {:sites 2 :refer-sites 1}]
           ["x" "x" {} {} #{}]
           ["old/other" "old/other" {:other-use true}]
           ["::old/k" "::old/k" {:indirect [{:reason :auto-resolved-keyword :form "::old/k"}]}]
           ["'other/x" "'other/x" {}]
           ["'old.lib/x" "'new.lib/y" {:sites 1}]
           ["'old/x" "'old/x" {:indirect [{:reason :quoted-reference :form "'old/x"}]}]
           ["`old/x" "`new/y" {:sites 1}]
           ["(defn f ([] (x)) ([x] x))" "(defn f ([] (new/y)) ([x] x))"
            {:sites 1 :refer-sites 1} {:per-arity-scopes true}]
           ["#?(:clj old/x :cljs old/x)" "#?(:clj new/y :cljs old/x)"
            {:sites 1 :unselected-sites true
             :indirect [{:reason :unselected-reader-conditional-branch
                         :form "#?(:clj old/x :cljs old/x)"}]}]
           ["#?(:clj old/x :cljs 1)" "#?(:clj new/y :cljs 1)" {:sites 1}]
           ["(if-let [x 1] x (x))" "(if-let [x 1] x (x))"
            {:indirect [{:reason :unsupported-binding-scope
                         :form "(if-let [x 1] x (x))" :row 1 :col 1}]}]
           ["(fn [x] [x old/x])" "(fn [x] [x new/y])" {:sites 1}]
           ["(letfn [(f [z] z)] (x))" "(letfn [(f [z] z)] (new/y))"
            {:sites 1 :refer-sites 1}]
           ["#{old/x}" "#{new/y}" {:sites 1}]
           ["\"old/x\"" "\"old/x\"" {}]]]
    (testing source
      (is (= (merge {:node expected :sites 0 :refer-sites 0 :indirect []
                     :other-use false :unselected-sites false} tallies)
             (update (alias-migration/rewrite-forms
                       (parser/parse-string source) (merge context overrides)
                       (or live #{"x"}))
                     :node n/string))))))

(def request
  {:workspace-root "/workspace" :from {:lib "old.lib" :var "x"}
   :to {:lib "new.lib" :var "y" :alias-policy ["new"]}
   :scope {:paths ["src/**"]} :expect {:files 1}})

(deftest file-plan-var-mode
  (is (= {:file "src/demo.clj" :alias "new" :collided [] :sites 2 :refer-sites 1
          :require-mode :replace
          :edits [{:kind :ns :original "(ns demo (:require [old.lib :as old :refer [x]]))"
                   :replacement "(ns demo (:require [new.lib :as new]))"}
                  {:kind :form :original "(old/x x)" :replacement "(new/y new/y)"}]}
         (#'alias-migration/file-plan request "src/demo.clj"
           "(ns demo (:require [old.lib :as old :refer [x]]))\n(old/x x)"))))

(deftest file-plan-lib-mode
  (is (= {:file "src/demo.clj" :alias "new" :collided [] :sites 1 :refer-sites 1
          :require-mode :replace
          :edits [{:kind :ns :original "(ns demo (:require [old.lib :as old :refer [x]]))"
                   :replacement "(ns demo (:require [new.lib :as new :refer [x]]))"}
                  {:kind :form :original "(old/x x)" :replacement "(new/x x)"}]}
         (#'alias-migration/file-plan
           (-> request (assoc-in [:from :var] nil) (assoc-in [:to :var] nil))
           "src/demo.clj" "(ns demo (:require [old.lib :as old :refer [x]]))\n(old/x x)"))))

(deftest file-plan-refusal
  (is (= {:refusal
          {:operation "alias_migration" :mutation_attempted false
           :error_type "alias-migration-indirect-reference" :next_call nil
           :reason "unsupported-selected-renamed-refer" :next_action "correct_request"
           :source_unchanged true :ok false :write_authority false :file "src/demo.clj"
           :error "The selected Var has an unsupported renamed binding in src/demo.clj"
           :remedy (str "This migration does not model the selected Var's renamed binding. "
                        "Review and migrate that binding and its uses explicitly; "
                        "no scope-changing next_call is provided.")}}
         (#'alias-migration/file-plan request "src/demo.clj"
           "(ns demo (:require [old.lib :refer [x] :rename {x z}]))\n(z)"))))

(deftest file-plan-out-of-scope
  (is (nil? (#'alias-migration/file-plan request "src/demo.clj" "(ns demo)\n(identity 1)"))))

(deftest fixture-plan-golden
  ;; Captured from unchanged 2ba9c950 before extracting either function.
  (let [corpus (fixture/corpus)
        request {:workspace-root "/workspace" :from (get-in corpus [:manifest :from])
                 :to (get-in corpus [:manifest :to]) :scope {:paths ["src/**"]}
                 :expect {:files 12}}
        sources (mapv (fn [[file source]] {:file file :source source})
                      (sort (filter #(str/starts-with? (key %) "src/") (:pre corpus))))]
    (is (= "a7395cc9900791357b2207eae077b5293ae5b086493f77b6b2b497d0b56843d6"
           (fixture/sha256 (pr-str (alias-migration/plan request sources)))))))
