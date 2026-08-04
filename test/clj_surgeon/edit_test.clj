(ns clj-surgeon.edit-test
  (:require
   [babashka.fs :as fs]
   [babashka.process :as proc]
   [clj-surgeon.core :as core]
   [clj-surgeon.structural-lens :as lens]
   [clojure.edn :as edn]
   [clojure.string :as str]
   [clojure.test :refer [deftest is testing]]
   [rewrite-clj.zip :as z]))

;; Derived from the case-edit clean Codex benchmark. The unrelated duplicate is
;; the condition that makes a line-oriented replacement unsafe to guess.
(def edit-source
  "(ns bench.edit)\n\n(defn transition [state event]\n  (case event\n    :start (assoc state :status :running)\n    :finish\n    ;; Keep this audit comment attached to the result.\n    (assoc state :status :done)\n    state))\n\n(defn unrelated-finish [state]\n  (assoc state :status :done))\n")

(def node-query
  [[:form 'transition]
   [:find :finish]
   :right
   [:replace '(assoc state :status :complete)]])

(def span-query
  [[:form 'transition]
   [:find :finish]
   [:span 2]
   [:replace-span :finish '(assoc state :status :complete)]])

(deftest edit-is-a-guarded-facade-over-the-existing-planner
  (doseq [[label query] [["node" node-query]
                         ["span" span-query]]]
    (testing label
      (let [opts {:file "src/bench/edit.clj"
                  :query query
                  :plan-out "review.edn"}
            expected (lens/evaluate-lens edit-source opts)
            actual (lens/evaluate-edit edit-source opts)
            applied (lens/apply-plan edit-source actual)]
        (is (= expected actual))
        (is (#{:replace-subform :replace-span} (:operation actual)))
        (is (:ok applied))
        (is (some? (z/of-string (:source applied))))))))

(deftest edit-requires-an-existing-terminal-transform
  (let [result (lens/evaluate-edit
                 edit-source
                 {:file "src/bench/edit.clj"
                  :query [[:form 'transition] [:find :finish] :right]
                  :plan-out "review.edn"})]
    (is (= :edit (:operation result)))
    (is (= :edit-requires-transform (:error-type result)))
    (is (= :q (get-in result [:remedy :read-operation])))
    (is (= [[:replace 'form] [:replace-span 'form '...]]
           (get-in result [:remedy :terminal-steps])))
    (is (nil? (:result-hash result)))))

(deftest edit-preserves-existing-lens-refusals
  (let [cases [{:label "zero"
                :query [[:find :absent] [:replace :done]]
                :error-type :no-match}
               {:label "many"
                :query [[:find '(assoc state :status :done)]
                        [:replace '(assoc state :status :complete)]]
                :error-type :ambiguous-match}
               {:label "span arity"
                :query [[:form 'transition]
                        [:find :finish]
                        [:span 2]
                        [:replace-span :finish]]
                :error-type :span-arity-mismatch}
               {:label "invalid replacement"
                :query [[:find :finish] [:replace "(broken"]]
                :error-type :invalid-replacement}
               {:label "unsupported step"
                :query [[:find :finish] :sideways [:replace :done]]
                :error-type :invalid-query}]]
    (doseq [{:keys [label query error-type]} cases]
      (testing label
        (let [opts {:file "src/bench/edit.clj"
                    :query query
                    :plan-out "review.edn"}]
          (is (= error-type
                 (:error-type (lens/evaluate-edit edit-source opts)))))))))

(deftest edit-help-is-a-prominent-plan-only-front-door
  (let [global (core/format-global-help core/ops-registry)
        help (core/format-op-help :edit (get core/ops-registry :edit))]
    (is (contains? core/ops-registry :edit))
    (is (str/includes? global "edit"))
    (is (str/includes? global "clj-surgeon :op :edit"))
    (is (str/includes? global ":plan-out plan.edn"))
    (is (str/includes? help "PLAN ONLY"))
    (is (str/includes? help "never changes source"))
    (is (str/includes? help "Use :q to read"))
    (is (str/includes? help "first source-bearing call"))
    (is (str/includes? help "never reproduce it with apply_patch"))
    (is (str/includes? help "[:replace"))
    (is (str/includes? help "[:replace-span"))
    (is (str/includes? help ":replace-subform!"))
    (is (= #{:file :query :plan-out}
           (set (keys (get-in core/ops-registry [:edit :args])))))
    (is (every? :required
                (vals (get-in core/ops-registry [:edit :args]))))))

(def project-root
  (str (fs/normalize (fs/path (System/getProperty "user.dir")))))

(defn- run-cli [& args]
  @(proc/process
     (into ["bb" "-m" "clj-surgeon.core"] args)
     {:dir project-root :err :string :out :string}))

(deftest cli-edit-plans-separately-and-existing-executor-applies
  (let [tmp-dir (fs/create-temp-dir {:prefix "clj surgeon edit "})
        source-file (fs/path tmp-dir "source.clj")
        plan-file (fs/path tmp-dir "plan.edn")]
    (try
      (spit (str source-file) edit-source)
      (let [planned (run-cli ":op" ":edit"
                             ":file" (str source-file)
                             ":query" (pr-str node-query)
                             ":plan-out" (str plan-file))
            plan (edn/read-string (:out planned))]
        (is (zero? (:exit planned)) (:err planned))
        (is (= edit-source (slurp (str source-file))))
        (is (= (dissoc plan :plan-out)
               (edn/read-string (slurp (str plan-file)))))
        (let [applied (run-cli ":op" ":replace-subform!"
                               ":plan" (str plan-file))
              receipt (edn/read-string (:out applied))
              source (slurp (str source-file))]
          (is (zero? (:exit applied)) (:err applied))
          (is (= (:result-hash plan)
                 (get-in receipt [:verified :read-back-hash])))
          (is (true? (get-in receipt [:verified :whole-file-parsed])))
          (is (= 1 (count (re-seq #"status :complete" source))))
          (is (= 1 (count (re-seq #"status :done" source))))))
      (finally
        (fs/delete-tree tmp-dir)))))

(deftest cli-edit-refuses-unsafe-or-incomplete-requests-without-changing-bytes
  (let [tmp-dir (fs/create-temp-dir {:prefix "clj surgeon edit refusal "})
        source-file (fs/path tmp-dir "source.clj")
        plan-file (fs/path tmp-dir "plan.edn")
        original-plan "{:existing :review}\n"]
    (try
      (spit (str source-file) edit-source)
      (spit (str plan-file) original-plan)
      (doseq [[label extra expected]
              [["getter-only"
                [":query" (pr-str [[:find :finish]])
                 ":plan-out" (str plan-file)]
                :edit-requires-transform]
               ["unknown flag"
                [":query" (pr-str node-query)
                 ":plan-out" (str plan-file)
                 ":force" "true"]
                :unsupported-arguments]
               ["source aliases plan"
                [":query" (pr-str node-query)
                 ":plan-out" (str source-file)]
                :plan-overwrites-source]]]
        (testing label
          (let [result (apply run-cli
                              ":op" ":edit"
                              ":file" (str source-file)
                              extra)
                refusal (edn/read-string (:out result))]
            (is (pos? (:exit result)) (:out result))
            (is (= expected (:error-type refusal)))
            (is (= edit-source (slurp (str source-file))))
            (when-not (= label "source aliases plan")
              (is (= original-plan (slurp (str plan-file))))))))
      (testing "plan-out is mandatory"
        (let [result (run-cli ":op" ":edit"
                              ":file" (str source-file)
                              ":query" (pr-str node-query))
              refusal (edn/read-string (:out result))]
          (is (pos? (:exit result)))
          (is (= :missing-arguments (:error-type refusal)))
          (is (= [:plan-out] (:missing refusal)))
          (is (= edit-source (slurp (str source-file))))))
      (finally
        (fs/delete-tree tmp-dir)))))
