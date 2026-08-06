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
    (is (= :xray (get-in result [:remedy :read-operation])))
    (is (= :expr (get-in result [:remedy :read-argument])))
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

(deftest edit-help-distinguishes-the-default-plan-from-the-guarded-one-call-route
  (let [global (core/format-global-help core/ops-registry)
        help (core/format-op-help :edit (get core/ops-registry :edit))]
    (is (contains? core/ops-registry :edit))
    (is (str/includes? global "edit"))
    (is (str/includes? global "clj-surgeon :op :edit"))
    (is (str/includes? global ":expr"))
    (is (str/includes? global ":expect :done"))
    (is (str/includes? global ":plan-out plan.edn"))
    (is (str/includes? help "Without :expect"))
    (is (str/includes? help "PLAN ONLY"))
    (is (str/includes? help "modified only by a successful :expect-guarded edit"))
    (is (str/includes? help "Use :xray to read"))
    (is (not (str/includes? help "Use :q to read")))
    (is (str/includes? help "first source-bearing call"))
    (is (str/includes? help "never reproduce it with apply_patch"))
    (is (str/includes? help "preserves its exact replacement spelling"))
    (is (str/includes? help "including #(), comments, commas, metadata, and multiline layout"))
    (is (str/includes? help "computed replacement or :query"))
    (is (str/includes? help ":selector :query is semantic data"))
    (is (str/includes? help "may display #() as fn*"))
    (is (str/includes? help "edit :after and :diff fields report the exact source"))
    (is (str/includes? help "[:replace"))
    (is (str/includes? help "[:replace-span"))
    (is (str/includes? help "SCI"))
    (is (str/includes? help "exactly one of :query and :expr"))
    (is (str/includes? help "(-> (form 'transition)"))
    (is (str/includes? help ":replace-subform!"))
    (is (= #{:file :query :expr :expect :plan-out}
           (set (keys (get-in core/ops-registry [:edit :args])))))
    (is (true? (get-in core/ops-registry [:edit :args :file :required])))
    (is (not-any? :required
                  (map #(get-in core/ops-registry [:edit :args %])
                       [:query :expr :expect :plan-out])))
    (is (str/includes? help "Omit :plan-out unless"))))

(deftest agent-facing-surfaces-teach-the-native-edit-boundary
  (let [help (core/format-op-help :edit (get core/ops-registry :edit))
        operational {"README" (slurp "README.md")
                     "installed skill" (slurp "skills/clj-surgeon/SKILL.md")
                     "Claude skill" (slurp ".claude/skills/clj-surgeon/SKILL.md")
                     "legacy skill" (slurp "skill.md")
                     "edit help" help}
        durable (assoc operational
                       "vision" (slurp "docs/vision.md")
                       "changelog" (slurp "CHANGELOG.md"))]
    (is (<= (count (str/split-lines (get operational "installed skill")))
            90)
        "The installed skill must fit in one compact 1-90 line read")
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
        (is (str/includes? (str/lower-case text) "do not preflight whether"))
        (is (str/includes? text "(-> (form 'transition)"))
        (is (str/includes? text "(match :finish)"))
        (is (str/includes? text "(replace '(assoc state"))
        (is (str/includes? text "(transform #(mapv"))
        (is (str/includes? text "#()"))
        (is (str/includes? (str/lower-case text) "canonical printing"))
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
      (testing "plan-only edit explains both safe routes when plan-out is absent"
        (let [result (run-cli ":op" ":edit"
                              ":file" (str source-file)
                              ":query" (pr-str node-query))
              refusal (edn/read-string (:out result))]
          (is (pos? (:exit result)))
          (is (= :missing-plan-out (:error-type refusal)))
          (is (= #{:guarded-edit :plan-only}
                 (set (keys (:remedies refusal)))))
          (is (str/includes? (get-in refusal [:remedies :guarded-edit :reason])
                             ":expect"))
          (is (str/includes? (get-in refusal [:remedies :plan-only :reason])
                             ":plan-out"))
          (is (= edit-source (slurp (str source-file))))))
      (finally
        (fs/delete-tree tmp-dir)))))

;; ============================================================
;; :expect — the optional one-call guarded edit
;; Every row of docs/plans/expect-guarded-edit.md is named below.
;; ============================================================

(def expect-replacement-expr
  "(-> (form 'transition) (match :finish) right (replace '(assoc state :status :complete)))")

(def true-expect
  "The selected form as it really reads in edit-source."
  "(assoc state :status :done)")

(def audit-payload-expect
  "The 2026-08-04 field trap: a caller's belief that is not the source."
  "(assoc state :status :done :audit (:audit payload))")

(def pre-existing-plan "{:existing :review}\n")

(defn- with-expect-workspace
  "A temp dir holding the benchmark source and a pre-existing plan artifact."
  [f]
  (let [dir (fs/create-temp-dir {:prefix "clj surgeon expect "})]
    (try
      (let [source-file (str (fs/path dir "source.clj"))
            plan-file (str (fs/path dir "plan.edn"))]
        (spit source-file edit-source)
        (spit plan-file pre-existing-plan)
        (f {:dir dir :source-file source-file :plan-file plan-file}))
      (finally
        (fs/delete-tree dir)))))

(deftest expect-row-1-omitting-expect-is-todays-plan-only-behavior
  (doseq [[label route] [["query" {:query node-query}]
                         ["expr" {:expr expect-replacement-expr}]]]
    (testing label
      (with-expect-workspace
        (fn [{:keys [source-file plan-file]}]
          (let [result (core/run-edit (merge {:file source-file
                                              :plan-out plan-file}
                                             route))]
            (is (= :replace-subform (:operation result)))
            (is (nil? (:mode result)))
            (is (nil? (:ok result)))
            (is (nil? (:verified result)))
            (is (= edit-source (slurp source-file))
                "plan-only never changes source bytes")
            (is (= (dissoc result :plan-out)
                   (edn/read-string (slurp plan-file))))))))))

(deftest expect-rows-2-and-3-matching-expect-saves-and-applies-in-one-call
  (doseq [[label route] [["row 2: :expr route" {:expr expect-replacement-expr}]
                         ["row 3: :query route" {:query node-query}]]]
    (testing label
      (with-expect-workspace
        (fn [{:keys [source-file plan-file]}]
          (let [result (core/run-edit (merge {:file source-file
                                              :plan-out plan-file
                                              :expect true-expect}
                                             route))
                source (slurp source-file)
                saved (edn/read-string (slurp plan-file))]
            (testing "one receipt carries the plan evidence and the apply proof"
              (is (nil? (:error result)))
              (is (true? (:ok result)))
              (is (= :expect-guarded (:mode result)))
              (is (= :replace-subform! (:operation result)))
              (is (= :replace-subform (:planned-operation result)))
              (is (= {:whole-file-parsed true
                      :atomic-write true
                      :read-back-hash (:result-hash result)}
                     (:verified result)))
              (is (= (first (:edits result)) (:applied-edit result)))
              (is (= 1 (:plan-version result)))
              (is (= 1 (:match-count result)))
              (is (some? (:selector result)))
              (is (some? (:diff result)))
              (is (= plan-file (:plan-out result))))
            (testing "the file changed exactly once and comments survive"
              (is (= 1 (count (re-seq #"status :complete" source))))
              (is (= 1 (count (re-seq #"status :done" source)))
                  "the unrelated duplicate is untouched")
              (is (str/includes?
                    source
                    ";; Keep this audit comment attached to the result.")))
            (testing "the saved plan remains the audit artifact"
              (is (= :replace-subform (:operation saved)))
              (is (= (:result-hash result) (:result-hash saved)))
              (is (= (:source-hash result) (:source-hash saved)))
              (is (= (:result-hash saved)
                     (lens/source-hash source))))))))))

(deftest matching-expect-applies-without-plan-artifact-bookkeeping
  (doseq [[label route] [["expr" {:expr expect-replacement-expr}]
                         ["query" {:query node-query}]]]
    (testing label
      (with-expect-workspace
        (fn [{:keys [source-file plan-file]}]
          (let [result (core/run-edit (merge {:file source-file
                                              :expect true-expect}
                                             route))
                source (slurp source-file)]
            (is (nil? (:error result)))
            (is (true? (:ok result)))
            (is (= :expect-guarded (:mode result)))
            (is (= :replace-subform! (:operation result)))
            (is (false? (:plan-artifact-retained result)))
            (is (nil? (:plan-out result)))
            (is (= (:result-hash result)
                   (get-in result [:verified :read-back-hash])))
            (is (= 1 (count (re-seq #"status :complete" source))))
            (is (= pre-existing-plan (slurp plan-file))
                "an unrelated plan artifact remains untouched")))))))

(deftest plan-only-edit-still-requires-a-plan-artifact
  (with-expect-workspace
    (fn [{:keys [source-file plan-file]}]
      (let [result (core/run-edit {:file source-file
                                   :expr expect-replacement-expr})]
        (is (= :missing-plan-out (:error-type result)))
        (is (= edit-source (slurp source-file)))
        (is (= pre-existing-plan (slurp plan-file)))
        (is (= #{:guarded-edit :plan-only}
               (set (keys (:remedies result)))))))))

(deftest expect-with-line-root-is-a-one-call-exact-edit-in-an-unnamed-owner
  (let [source (slurp "test/fixtures/containing_line_owner.clj")
        expected (str/replace-first
                   source
                   "(defcache 'selected-cache '[account-id]\n  '(let [reader (old-reader account-id)]"
                   "(defcache 'selected-cache '[account-id]\n  '(let [reader (new-reader account-id)]")
        tmp-dir (fs/create-temp-dir {:prefix "clj surgeon line expect "})
        source-file (str (fs/path tmp-dir "containing_line_owner.clj"))
        plan-file (str (fs/path tmp-dir "line-plan.edn"))]
    (try
      (spit source-file source)
      (let [result (run-cli
                     ":op" ":edit"
                     ":file" source-file
                     ":expr" (str "(-> (line 14)"
                                  " (match '(old-reader account-id))"
                                  " (replace '(new-reader account-id)))")
                     ":expect" "(old-reader account-id)"
                     ":plan-out" plan-file)
            receipt (edn/read-string (:out result))]
        (is (zero? (:exit result)) (:err result))
        (is (true? (:ok receipt)))
        (is (= :expect-guarded (:mode receipt)))
        (is (= [[:line 14]
                [:find '(old-reader account-id)]
                [:replace '(new-reader account-id)]]
               (get-in receipt [:selector :query])))
        (is (= expected (slurp source-file)))
        (is (= (:result-hash receipt)
               (get-in receipt [:verified :read-back-hash])))
        (is (= (:result-hash receipt)
               (:result-hash (edn/read-string (slurp plan-file))))))
      (finally
        (fs/delete-tree tmp-dir)))))

(deftest line-root-plan-only-and-expect-mismatch-never-change-source
  (let [source (slurp "test/fixtures/containing_line_owner.clj")
        expression (str "(-> (line 14)"
                        " (match '(old-reader account-id))"
                        " (replace '(new-reader account-id)))")
        tmp-dir (fs/create-temp-dir {:prefix "clj surgeon line refusal "})
        source-file (str (fs/path tmp-dir "containing_line_owner.clj"))
        plan-file (str (fs/path tmp-dir "line-plan.edn"))]
    (try
      (spit source-file source)
      (spit plan-file pre-existing-plan)
      (testing "without expect, the line-root edit only replaces the plan artifact"
        (let [plan (core/run-edit {:file source-file
                                   :expr expression
                                   :plan-out plan-file})]
          (is (= :replace-subform (:operation plan)))
          (is (= [[:line 14]
                  [:find '(old-reader account-id)]
                  [:replace '(new-reader account-id)]]
                 (get-in plan [:selector :query])))
          (is (= source (slurp source-file)))
          (is (= (dissoc plan :plan-out)
                 (edn/read-string (slurp plan-file))))))
      (testing "a wrong leaf declaration preserves source and the existing plan"
        (spit plan-file pre-existing-plan)
        (let [refusal (core/run-edit
                        {:file source-file
                         :expr expression
                         :expect "(old-reader different-account)"
                         :plan-out plan-file})]
          (is (= :expect-mismatch (:error-type refusal)))
          (is (= '(old-reader account-id) (:actual refusal)))
          (is (= '(old-reader different-account) (:expected refusal)))
          (is (= source (slurp source-file)))
          (is (= pre-existing-plan (slurp plan-file)))))
      (finally
        (fs/delete-tree tmp-dir)))))

(deftest expect-row-4-mismatch-refuses-and-changes-nothing
  (with-expect-workspace
    (fn [{:keys [source-file plan-file]}]
      (let [result (core/run-edit {:file source-file
                                   :expr expect-replacement-expr
                                   :plan-out plan-file
                                   :expect audit-payload-expect})]
        (is (= :expect-mismatch (:error-type result)))
        (is (= :edit (:operation result)))
        (is (= :expect-guarded (:mode result)))
        (is (= '(assoc state :status :done :audit (:audit payload))
               (:expected result)))
        (is (= '(assoc state :status :done) (:actual result)))
        (is (= "(assoc state :status :done)" (:actual-source result)))
        (is (nil? (:ok result)))
        (is (nil? (:verified result)))
        (is (= edit-source (slurp source-file))
            "row 4 leaves the source bytes unchanged")
        (is (= pre-existing-plan (slurp plan-file))
            "row 4 preserves a pre-existing plan artifact")))))

(deftest expect-row-5-unparseable-expect-refuses-before-source-or-plan-io
  (doseq [[label expect] [["zero forms" "   "]
                          ["comment only" ";; nothing to declare\n"]
                          ["two forms" ":finish (assoc state :status :done)"]
                          ["reader error" "(assoc state :status"]]]
    (testing label
      (with-expect-workspace
        (fn [{:keys [dir source-file plan-file]}]
          (let [missing (str (fs/path dir "missing.clj"))
                result (core/run-edit {:file missing
                                       :expr expect-replacement-expr
                                       :plan-out plan-file
                                       :expect expect})]
            (is (= :invalid-expect (:error-type result)))
            (is (= :edit (:operation result)))
            (is (= expect (:expect result)))
            (is (not (fs/exists? missing))
                "the refusal precedes the source read")
            (is (= pre-existing-plan (slurp plan-file)))
            (is (= edit-source (slurp source-file)))))))))

(deftest expect-row-6-selection-refusals-keep-their-existing-error-types
  (doseq [[label query expected]
          [["zero matches" [[:find :absent] [:replace :done]] :no-match]
           ["ambiguous matches"
            [[:find '(assoc state :status :done)]
             [:replace '(assoc state :status :complete)]]
            :ambiguous-match]]]
    (testing label
      (with-expect-workspace
        (fn [{:keys [source-file plan-file]}]
          (let [result (core/run-edit {:file source-file
                                       :query query
                                       :plan-out plan-file
                                       :expect true-expect})]
            (is (= expected (:error-type result)))
            (is (not= :expect-mismatch (:error-type result)))
            (is (= edit-source (slurp source-file)))
            (is (= pre-existing-plan (slurp plan-file)))))))))

(deftest expect-row-7-getter-only-pipeline-keeps-the-existing-refusal
  (with-expect-workspace
    (fn [{:keys [source-file plan-file]}]
      (let [result (core/run-edit {:file source-file
                                   :query [[:find :finish]]
                                   :plan-out plan-file
                                   :expect true-expect})]
        (is (= :edit-requires-transform (:error-type result)))
        (is (= :xray (get-in result [:remedy :read-operation])))
        (is (= :expr (get-in result [:remedy :read-argument])))
        (is (= edit-source (slurp source-file)))
        (is (= pre-existing-plan (slurp plan-file)))))))

(def comment-laden-source
  "(ns bench.expect)\n\n(defn transition [state event]\n  (case event\n    :finish\n    (assoc    state\n      ;; the status key must survive review\n      :status\n\n      :done)\n    state))\n")

(deftest expect-row-8-whitespace-matches-but-undeclared-comments-refuse
  (let [dir (fs/create-temp-dir {:prefix "clj surgeon expect ws "})]
    (try
      (let [source-file (str (fs/path dir "source.clj"))
            plan-file (str (fs/path dir "plan.edn"))]
        (spit source-file comment-laden-source)
        (spit plan-file pre-existing-plan)
        (let [result (core/run-edit {:file source-file
                                     :expr expect-replacement-expr
                                     :plan-out plan-file
                                     :expect true-expect})
              source (slurp source-file)]
          (is (= :expect-mismatch (:error-type result)))
          (is (= :expect-guarded (:mode result)))
          (is (= :source-syntax-mismatch (:reason result)))
          (is (str/includes? (:actual-source result)
                             ";; the status key must survive review"))
          (is (= comment-laden-source source))
          (is (= pre-existing-plan (slurp plan-file))))
        (let [result (core/run-edit
                       {:file source-file
                        :expr "(-> (form 'transition) (match :done) (replace :complete))"
                        :plan-out plan-file
                        :expect ":done"})
              source (slurp source-file)]
          (is (true? (:ok result)))
          (is (str/includes? source
                             ";; the status key must survive review"))
          (is (str/includes? source ":complete"))
          (is (not (str/includes? source ":done")))))
      (finally
        (fs/delete-tree dir)))))

(deftest expect-refuses-undeclared-metadata-and-accepts-declared-metadata
  (let [dir (fs/create-temp-dir {:prefix "clj surgeon expect metadata "})]
    (try
      (let [source-file (str (fs/path dir "source.clj"))
            plan-file (str (fs/path dir "plan.edn"))
            source "(ns bench.metadata)\n(def x ^:private foo)\n"
            route {:file source-file
                   :expr "(-> (form 'x) initializer (replace 'bar))"
                   :plan-out plan-file}]
        (spit source-file source)
        (spit plan-file pre-existing-plan)
        (let [refusal (core/run-edit (assoc route :expect "foo"))]
          (is (= :expect-mismatch (:error-type refusal)))
          (is (= :source-syntax-mismatch (:reason refusal)))
          (is (= "^:private foo" (:actual-source refusal)))
          (is (= source (slurp source-file)))
          (is (= pre-existing-plan (slurp plan-file))))
        (let [receipt (core/run-edit (assoc route :expect "^:private foo"))]
          (is (true? (:ok receipt)))
          (is (= "(ns bench.metadata)\n(def x bar)\n"
                 (slurp source-file)))))
      (finally
        (fs/delete-tree dir)))))

(deftest expect-refuses-a-computed-transform-without-reviewing-its-after-state
  (with-expect-workspace
    (fn [{:keys [source-file plan-file]}]
      (let [result
            (core/run-edit
              {:file source-file
               :expr "(-> (form 'transition) (match :finish) right (transform (constantly :destroyed)))"
               :plan-out plan-file
               :expect true-expect})]
        (is (= :expect-requires-literal-replacement (:error-type result)))
        (is (= :plan-and-review (get-in result [:remedy :mode])))
        (is (= edit-source (slurp source-file)))
        (is (= pre-existing-plan (slurp plan-file)))))))

(deftest edit-refuses-a-plan-path-that-is-not-an-edn-artifact
  (with-expect-workspace
    (fn [{:keys [dir source-file]}]
      (let [victim (str (fs/path dir "unrelated.clj"))
            victim-source "(ns unrelated)\n(def keep-me true)\n"]
        (spit victim victim-source)
        (let [result (core/run-edit {:file source-file
                                     :expr expect-replacement-expr
                                     :plan-out victim
                                     :expect true-expect})]
          (is (= :invalid-plan-out (:error-type result)))
          (is (= edit-source (slurp source-file)))
          (is (= victim-source (slurp victim))))))))

(deftest expect-verifies-and-repairs-a-concurrently-changed-plan-artifact
  (with-expect-workspace
    (fn [{:keys [source-file plan-file]}]
      (let [original-executor lens/execute-plan!
            result
            (with-redefs
              [lens/execute-plan!
               (fn [{:keys [plan] :as request}]
                 (spit (:plan-out plan) "{:concurrent-writer :won}\n")
                 (original-executor request))]
              (core/run-edit {:file source-file
                              :expr expect-replacement-expr
                              :plan-out plan-file
                              :expect true-expect}))
            saved (edn/read-string (slurp plan-file))]
        (is (true? (:ok result)))
        (is (true? (get-in result [:plan-artifact :verified])))
        (is (true? (get-in result [:plan-artifact :repaired])))
        (is (= (:result-hash result) (:result-hash saved)))
        (is (= (:result-hash result)
               (lens/source-hash (slurp source-file))))))))

(deftest expect-row-9-apply-failure-keeps-the-existing-executor-refusal
  (let [dir (fs/create-temp-dir {:prefix "clj surgeon expect apply "})
        source-dir (fs/path dir "readonly")
        source-file (str (fs/path source-dir "source.clj"))
        plan-file (str (fs/path dir "plan.edn"))]
    (try
      (fs/create-dir source-dir)
      (spit source-file edit-source)
      (.setWritable (java.io.File. (str source-dir)) false false)
      (let [result (core/run-edit {:file source-file
                                   :expr expect-replacement-expr
                                   :plan-out plan-file
                                   :expect true-expect})]
        (is (:error result))
        (is (= :atomic-write-failed (:error-type result)))
        (is (= :expect-guarded (:mode result)))
        (is (nil? (:ok result)))
        (is (= edit-source (slurp source-file))
            "the atomic write never partially replaced the target")
        (is (= :replace-subform
               (:operation (edn/read-string (slurp plan-file))))
            "the audit artifact is saved before the apply attempt"))
      (finally
        (.setWritable (java.io.File. (str source-dir)) true false)
        (fs/delete-tree dir)))))

(deftest expect-help-documents-the-optional-one-call-guarded-edit
  (let [help (core/format-op-help :edit (get core/ops-registry :edit))]
    (is (str/includes? help ":expect"))
    (is (str/includes? help "PLAN ONLY"))
    (is (str/includes? help "Without :expect"))
    (is (str/includes?
          help
          ":expect is optional; without it the default flow is unchanged"))
    (is (str/includes? help ":expect-mismatch"))
    (is (str/includes? help "(line N)"))
    (is (str/includes? help ":actual-source"))
    (is (str/includes? help "comments, metadata, reader macros"))
    (is (str/includes? help "computed transforms"))
    (is (str/includes? help "non-.edn plan paths"))
    (is (some #(and (str/includes? % ":expect")
                    (str/includes? % ":expect '")
                    (not (str/includes? % ":plan-out")))
              (get-in core/ops-registry [:edit :examples]))
        "one documented example shows the artifact-free guarded invocation")))

(deftest parse-args-preserves-the-expect-form-verbatim
  (let [expect "(assoc state :status :done)"]
    (is (= expect
           (:expect (core/parse-args [":op" ":edit" ":expect" expect]))))
    (is (= "[100 250 500]"
           (:expect (core/parse-args [":op" ":edit" ":expect" "[100 250 500]"])))
        "a vector selection stays the caller's exact source text")))

(defn- documented-expect-example []
  (first (filter #(str/includes? % ":expect")
                 (get-in core/ops-registry [:edit :examples]))))

(deftest cli-expect-guarded-edit-refuses-then-applies-in-one-documented-call
  (let [tmp-dir (fs/create-temp-dir {:prefix "clj surgeon cli expect "})
        source-file (str (fs/path tmp-dir "source.clj"))
        plan-file (str (fs/path tmp-dir "plan.edn"))
        missing-file (str (fs/path tmp-dir "missing.clj"))]
    (try
      (spit source-file edit-source)
      (spit plan-file pre-existing-plan)
      (testing "row 5: an unparseable :expect exits nonzero before any I/O"
        (doseq [expect ["   " ":finish (assoc state :status :done)" "(assoc state"]]
          (let [result (run-cli ":op" ":edit"
                                ":file" missing-file
                                ":expr" expect-replacement-expr
                                ":expect" expect
                                ":plan-out" plan-file)
                refusal (edn/read-string (:out result))]
            (is (pos? (:exit result)) (:out result))
            (is (= :invalid-expect (:error-type refusal)))
            (is (not (str/includes? (:err result) "Exception")))
            (is (not (fs/exists? missing-file)))
            (is (= pre-existing-plan (slurp plan-file))))))
      (testing "row 4: a mismatched :expect exits nonzero and changes nothing"
        (let [result (run-cli ":op" ":edit"
                              ":file" source-file
                              ":expr" expect-replacement-expr
                              ":expect" audit-payload-expect
                              ":plan-out" plan-file)
              refusal (edn/read-string (:out result))]
          (is (pos? (:exit result)) (:out result))
          (is (= :expect-mismatch (:error-type refusal)))
          (is (= '(assoc state :status :done) (:actual refusal)))
          (is (= '(assoc state :status :done :audit (:audit payload))
                 (:expected refusal)))
          (is (= "(assoc state :status :done)" (:actual-source refusal)))
          (is (= edit-source (slurp source-file)))
          (is (= pre-existing-plan (slurp plan-file)))))
      (testing "row 2: the documented invocation applies and verifies in one call"
        (let [example (documented-expect-example)]
          (is (str/includes? example ":op :edit"))
          (is (str/includes? example ":expect"))
          (let [result (run-cli ":op" ":edit"
                                ":file" source-file
                                ":expr" expect-replacement-expr
                                ":expect" true-expect)
                receipt (edn/read-string (:out result))
                source (slurp source-file)]
            (is (zero? (:exit result)) (:err result))
            (is (true? (:ok receipt)))
            (is (= :expect-guarded (:mode receipt)))
            (is (= :replace-subform! (:operation receipt)))
            (is (= (:result-hash receipt)
                   (get-in receipt [:verified :read-back-hash])))
            (is (true? (get-in receipt [:verified :whole-file-parsed])))
            (is (= 1 (count (re-seq #"status :complete" source))))
            (is (= 1 (count (re-seq #"status :done" source))))
            (is (false? (:plan-artifact-retained receipt)))
            (is (= pre-existing-plan (slurp plan-file))))))
      (finally
        (fs/delete-tree tmp-dir)))))

(deftest cli-literal-replacement-preserves-anonymous-function-spelling
  (let [tmp-dir (fs/create-temp-dir {:prefix "clj surgeon reader shorthand "})
        source-file (str (fs/path tmp-dir "source.clj"))
        plan-file (str (fs/path tmp-dir "plan.edn"))
        original (slurp "test/fixtures/literal_replacement_source.clj")
        expression (str "(-> (form 'page) (match '{:dev-mode? dev-mode?}) "
                        "(replace '{:dev-mode? dev-mode? "
                        ":head {:asset-url #(str %)}}))")
        expected (str/replace original
                              "{:dev-mode? dev-mode?}"
                              "{:dev-mode? dev-mode? :head {:asset-url #(str %)}}")]
    (try
      (spit source-file original)
      (let [result (run-cli ":op" ":edit"
                            ":file" source-file
                            ":expr" expression
                            ":expect" "{:dev-mode? dev-mode?}"
                            ":plan-out" plan-file)
            receipt (edn/read-string (:out result))
            saved (edn/read-string (slurp plan-file))]
        (is (zero? (:exit result)) (:err result))
        (is (true? (:ok receipt)))
        (is (= expected (slurp source-file)))
        (is (= "{:dev-mode? dev-mode? :head {:asset-url #(str %)}}"
               (get-in receipt [:applied-edit :after])))
        (is (str/includes? (:diff receipt) "#(str %)"))
        (is (not (str/includes? (:diff receipt) "fn*")))
        (is (= "{:dev-mode? dev-mode? :head {:asset-url #(str %)}}"
               (get-in saved [:edits 0 :after]))))
      (finally
        (fs/delete-tree tmp-dir)))))

(deftest literal-replacement-preserves-multiline-layout-comments-and-reader-syntax
  (let [source (str "(ns bench.literal-layout)\n\n"
                    "(defn choose-handler []\n"
                    "  identity)\n")
        expression (str "(-> (form 'choose-handler) (match 'identity) "
                        "(replace '(fn [request]\n"
                        "            ;; Keep the fast path visible.\n"
                        "            (#'dispatch #(handle request %) request))))")
        query (:query (edit-dsl/prepare-edit-options {:op :edit
                                                      :file "source.clj"
                                                      :expr expression
                                                      :plan-out "plan.edn"}))
        plan (edit-dsl/evaluate-edit source {:op :edit
                                             :file "source.clj"
                                             :query query
                                             :plan-out "plan.edn"})
        after (get-in plan [:edits 0 :after])
        applied (lens/apply-plan source plan)]
    (is (= (str "(fn [request]\n"
                "            ;; Keep the fast path visible.\n"
                "            (#'dispatch #(handle request %) request))")
           after))
    (is (str/includes? (:diff plan) "#'dispatch #(handle request %)"))
    (is (not (str/includes? (:diff plan) "fn*")))
    (is (nil? (:error applied)))
    (is (str/includes? (:source applied)
                       ";; Keep the fast path visible."))))

(deftest literal-replacement-preserves-shorthand-inside-a-large-conditional
  (let [source (slurp "test/fixtures/literal_replacement_source.clj")
        expression (str "(-> (form 'include-current) "
                        "(match '(if (and current (not (some #{current} entries))) "
                        "(cons current entries) entries)) "
                        "(replace '(if (and current "
                        "(not (some #(= (clojure.string/lower-case current) (clojure.string/lower-case %)) entries))) "
                        "(cons current entries) entries)))")
        opts (edit-dsl/prepare-edit-options {:op :edit
                                             :file "source.clj"
                                             :expr expression
                                             :plan-out "plan.edn"})
        plan (edit-dsl/evaluate-edit source opts)
        after (get-in plan [:edits 0 :after])
        applied (lens/apply-plan source plan)]
    (is (str/includes? after
                       "#(= (clojure.string/lower-case current) (clojure.string/lower-case %))"))
    (is (not (str/includes? after "fn*")))
    (is (str/includes? (:diff plan)
                       "#(= (clojure.string/lower-case current) (clojure.string/lower-case %))"))
    (is (nil? (:error applied)))
    (is (str/includes? (:source applied)
                       "#(= (clojure.string/lower-case current) (clojure.string/lower-case %))"))))

(deftest source-overrides-never-invent-spelling-for-computed-replacements
  (let [source "(ns bench.computed)\n\n(defn handler [] identity)\n"
        plan-for (fn [expression]
                   (->> {:op :edit
                         :file "source.clj"
                         :expr expression
                         :plan-out "plan.edn"}
                        edit-dsl/prepare-edit-options
                        (edit-dsl/evaluate-edit source)))
        explicit (plan-for
                   "(-> (form 'handler) (match 'identity) (replace '(fn* [value] (handle value))))")
        computed (plan-for
                   "(let [after (list 'handle 'value)] (-> (form 'handler) (match 'identity) (replace after)))")
        forged-query (with-meta [[:form 'handler]
                                 [:find 'identity]
                                 [:replace '(handle value)]]
                       {::lens/replacement-sources ["(wrong value)"]
                        ::lens/replacement-values ['(different value)]})
        forged (lens/evaluate-edit source {:op :edit
                                           :file "source.clj"
                                           :query forged-query
                                           :plan-out "plan.edn"})]
    (is (= "(fn* [value] (handle value))"
           (get-in explicit [:edits 0 :after])))
    (is (= "(handle value)"
           (get-in computed [:edits 0 :after])))
    (is (= "(handle value)"
           (get-in forged [:edits 0 :after]))
        "metadata that does not describe the terminal query cannot override source")))
