(ns clj-surgeon.edit-test
  (:require
   [babashka.fs :as fs]
   [babashka.process :as proc]
   [clj-surgeon.core :as core]
   [clj-surgeon.edit-dsl :as edit-dsl]
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
    (is (str/includes? global ":expr"))
    (is (str/includes? global ":plan-out plan.edn"))
    (is (str/includes? help "PLAN ONLY"))
    (is (str/includes? help "never changes source"))
    (is (str/includes? help "Use :q to read"))
    (is (str/includes? help "first source-bearing call"))
    (is (str/includes? help "never reproduce it with apply_patch"))
    (is (str/includes? help "[:replace"))
    (is (str/includes? help "[:replace-span"))
    (is (str/includes? help "SCI"))
    (is (str/includes? help "exactly one of :query and :expr"))
    (is (str/includes? help "(-> (form 'transition)"))
    (is (str/includes? help ":replace-subform!"))
    (is (= #{:file :query :expr :plan-out}
           (set (keys (get-in core/ops-registry [:edit :args])))))
    (is (every? :required
                (map #(get-in core/ops-registry [:edit :args %])
                     [:file :plan-out])))
    (is (not-any? :required
                  (map #(get-in core/ops-registry [:edit :args %])
                       [:query :expr])))))

(deftest agent-facing-surfaces-teach-the-native-edit-boundary
  (let [help (core/format-op-help :edit (get core/ops-registry :edit))
        operational {"README" (slurp "README.md")
                     "installed skill" (slurp "skills/clj-surgeon/SKILL.md")
                     "legacy skill" (slurp "skill.md")
                     "edit help" help}
        durable (assoc operational
                       "vision" (slurp "docs/vision.md")
                       "changelog" (slurp "CHANGELOG.md"))]
    (doseq [[surface text] durable]
      (testing surface
        (let [normalized (-> text
                             str/lower-case
                             (str/replace "`" "")
                             (str/replace #"\s+" " "))]
          (is (str/includes? text ":edit"))
          (is (str/includes? text ":expr"))
          (is (str/includes? normalized "pure clojure"))
          (is (str/includes? text "transform"))
          (is (str/includes? normalized "concrete replacement"))
          (is (str/includes? text ":replace-subform!")))))
    (doseq [[surface text] operational]
      (testing surface
        (is (str/includes? text "(-> (form 'transition)"))
        (is (str/includes? text "(match :finish)"))
        (is (str/includes? text "(replace '(assoc state"))
        (is (str/includes? text "(transform #(mapv"))
        (is (str/includes? text ":plan-out plan.edn"))))))

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

(deftest cli-native-edit-plan-equals-the-legacy-query-plan-and-applies
  (let [tmp-dir (fs/create-temp-dir {:prefix "clj surgeon native edit "})
        source-file (fs/path tmp-dir "source.clj")
        query-plan-file (fs/path tmp-dir "query-plan.edn")
        expr-plan-file (fs/path tmp-dir "expr-plan.edn")
        expression "(-> (form 'transition) (match :finish) right (replace '(assoc state :status :complete)))"]
    (try
      (spit (str source-file) edit-source)
      (let [query-result (run-cli ":op" ":edit"
                                  ":file" (str source-file)
                                  ":query" (pr-str node-query)
                                  ":plan-out" (str query-plan-file))
            expr-result (run-cli ":op" ":edit"
                                 ":file" (str source-file)
                                 ":expr" expression
                                 ":plan-out" (str expr-plan-file))
            query-plan (edn/read-string (:out query-result))
            expr-plan (edn/read-string (:out expr-result))]
        (is (zero? (:exit query-result)) (:err query-result))
        (is (zero? (:exit expr-result)) (:err expr-result))
        (is (= (dissoc query-plan :plan-out)
               (dissoc expr-plan :plan-out)))
        (is (= edit-source (slurp (str source-file))))
        (is (= (dissoc expr-plan :plan-out)
               (edn/read-string (slurp (str expr-plan-file)))))
        (let [applied (run-cli ":op" ":replace-subform!"
                               ":plan" (str expr-plan-file))
              receipt (edn/read-string (:out applied))]
          (is (zero? (:exit applied)) (:err applied))
          (is (= (:result-hash expr-plan)
                 (get-in receipt [:verified :read-back-hash])))
          (is (true? (get-in receipt [:verified :whole-file-parsed])))))
      (finally
        (fs/delete-tree tmp-dir)))))

(deftest cli-native-transform-derives-a-concrete-replayable-plan
  (let [tmp-dir (fs/create-temp-dir {:prefix "clj surgeon transform edit "})
        source-file (fs/path tmp-dir "source.clj")
        plan-file (fs/path tmp-dir "plan.edn")
        source "(ns bench.retry)\n\n(def retry-policy {:delays [100 250 500]})\n\n(def decoy [100 250 500])\n"
        expression "(-> (form 'retry-policy) (match :delays) right (transform #(mapv (partial + 100) %)))"]
    (try
      (spit (str source-file) source)
      (let [planned (run-cli ":op" ":edit"
                             ":file" (str source-file)
                             ":expr" expression
                             ":plan-out" (str plan-file))
            plan (edn/read-string (:out planned))]
        (is (zero? (:exit planned)) (:err planned))
        (is (= source (slurp (str source-file))))
        (is (= [[:form 'retry-policy]
                [:find :delays]
                :right
                [:replace [200 350 600]]]
               (get-in plan [:selector :query])))
        (is (= "[100 250 500]" (get-in plan [:edits 0 :before])))
        (is (= "[200 350 600]" (get-in plan [:edits 0 :after])))
        (is (= (dissoc plan :plan-out)
               (edn/read-string (slurp (str plan-file)))))
        (let [applied (run-cli ":op" ":replace-subform!"
                               ":plan" (str plan-file))
              receipt (edn/read-string (:out applied))]
          (is (zero? (:exit applied)) (:err applied))
          (is (= (:result-hash plan)
                 (get-in receipt [:verified :read-back-hash])))
          (is (true? (get-in receipt [:verified :whole-file-parsed])))
          (is (= "(ns bench.retry)\n\n(def retry-policy {:delays [200 350 600]})\n\n(def decoy [100 250 500])\n"
                 (slurp (str source-file))))))
      (finally
        (fs/delete-tree tmp-dir)))))

(deftest cli-native-edit-refuses-before-source-or-plan-io
  (let [tmp-dir (fs/create-temp-dir {:prefix "clj surgeon native refusal "})
        missing-source (fs/path tmp-dir "missing.clj")
        plan-file (fs/path tmp-dir "plan.edn")
        side-effect-file (str (fs/path tmp-dir "must-not-exist"))
        original-plan "{:existing :review}\n"
        valid-expression "(-> (form 'transition) (match :finish) right (replace :complete))"]
    (try
      (spit (str plan-file) original-plan)
      (doseq [[label args expected]
              [["neither input"
                []
                :missing-edit-input]
               ["both inputs"
                [":query" (pr-str node-query)
                 ":expr" valid-expression]
                :edit-input-conflict]
               ["unsafe expression"
                [":expr" (str "(spit \"" side-effect-file "\" \"bad\")")]
                :invalid-edit-expression]]]
        (testing label
          (let [result (apply run-cli
                              ":op" ":edit"
                              ":file" (str missing-source)
                              ":plan-out" (str plan-file)
                              args)
                refusal (edn/read-string (:out result))]
            (is (pos? (:exit result)) (:out result))
            (is (= expected (:error-type refusal)))
            (is (not (str/includes? (:err result) "Exception")))
            (is (= original-plan (slurp (str plan-file)))))))
      (is (not (fs/exists? side-effect-file)))
      (finally
        (fs/delete-tree tmp-dir)))))

(deftest parse-args-preserves-native-edit-expression-verbatim
  (let [expression "(-> (form 'transition) (replace '(str \"done\")))"]
    (is (= expression
           (:expr (core/parse-args [":op" ":edit" ":expr" expression]))))
    (is (= (edit-dsl/compile-query expression)
           (:query (edit-dsl/prepare-edit-options
                    (core/parse-args [":op" ":edit"
                                      ":file" "src/state.clj"
                                      ":expr" expression
                                      ":plan-out" "plan.edn"])))))))

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
