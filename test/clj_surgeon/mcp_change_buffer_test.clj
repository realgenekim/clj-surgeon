(ns clj-surgeon.mcp-change-buffer-test
  (:require
   [clj-surgeon.intent-transaction :as transaction]
   [clj-surgeon.mcp-change-buffer :as change-buffer]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.test :refer [deftest is]]))

(def core-source
  (str "(ns sample.core)\n"
       "(defn target [x] (inc x))\n"
       "(defn caller [x]\n"
       "  ;; target here is only a comment\n"
       "  (target x))\n"))

(def test-source
  (str "(ns sample.core-test)\n"
       "(deftest target-test\n"
       "  (is (= 2 (target 1))))\n"))

(defn- temp-project
  []
  (let [root (.toFile (java.nio.file.Files/createTempDirectory
                        "clj-surgeon-change-buffer-"
                        (make-array java.nio.file.attribute.FileAttribute 0)))
        source (io/file root "src/sample/core.clj")
        test-file (io/file root "test/sample/core_test.clj")]
    (.mkdirs (.getParentFile source))
    (.mkdirs (.getParentFile test-file))
    (spit source core-source)
    (spit test-file test-source)
    {:root root :source source :test-file test-file}))

(defn- semantic-result
  [{:keys [source test-file]}]
  {:ok true
   :definition {:file_path (.getCanonicalPath source)
                :line 2 :character 7 :name "target"}
   :references
   [{:file_path (.getCanonicalPath source)
     :line 2 :character 7
     :owner {:name "target" :start_line 2}}
    {:file_path (.getCanonicalPath source)
     :line 5 :character 4
     :owner {:name "caller" :start_line 3}}
    {:file_path (.getCanonicalPath test-file)
     :line 3 :character 13
     :owner {:name "target-test" :start_line 2}}]})

(defn- prepare!
  [project]
  (change-buffer/prepare-change!
    {:project-root (:root project)
     :semantic-resolver (fn [_] (semantic-result project))}
    {:subject "sample.core/target"
     :intent "Rename target to compute-target without touching its test call yet"
     :verify "fast"}))

(deftest addresses-the-smallest-complete-form-at-a-semantic-position
  (let [selected (transaction/addressed-form-at core-source
                                                {:line 5 :character 4})]
    (is (= "(target x)" (:before selected)))
    (is (vector? (:path selected)))
    (is (nat-int? (get-in selected [:address :preorder])))))

(deftest prepare-captures-each-file-once-and-emits-an-executable-next-call
  (change-buffer/clear-bases!)
  (let [project (temp-project)
        reads (atom [])
        result (change-buffer/prepare-change!
                 {:project-root (:root project)
                  :semantic-resolver (fn [_] (semantic-result project))
                  :read-source (fn [file]
                                 (swap! reads conj file)
                                 (slurp file))}
                 {:subject "sample.core/target"
                  :intent "Rename the Var and selected calls"
                  :verify "fast"})]
    (is (:ok result))
    (is (= 2 (:file-count result)))
    (is (= 3 (:site-count result)))
    (is (= 1 (count (filter #(= :definition (:role %)) (:sites result)))))
    (is (= 2 (count @reads)))
    (is (= #{{:site "s1" :replace nil}
             {:site "s2" :replace nil}
             {:site "s3" :replace nil}}
           (set (get-in result [:next_call :decisions]))))
    (is (some #(and (= "caller" (:owner %))
                    (.contains ^String (:source %) "only a comment")
                    (.startsWith ^String (:source %) "(defn caller"))
              (:sites result)))
    (is (some #(and (= "target-test" (:owner %))
                    (.startsWith ^String (:source %) "(deftest target-test"))
              (:sites result)))))

(deftest basis-request-shape-refuses-mixed-routes-and-unknown-decision-fields
  (is (= {:ok true}
         (change-buffer/validate-basis-request
           {:basis "cb-1"
            :decisions [{:site "s1" :keep true}]
            :verify "fast"})))
  (let [mixed (change-buffer/validate-basis-request
                {:basis "cb-1" :decisions [] :changes []})
        nested (change-buffer/validate-basis-request
                 {:basis "cb-1"
                  :decisions [{:site "s1" :keep true :unexpected true}]})]
    (is (= :invalid-mcp-request (:error-type mixed)))
    (is (= ["changes"] (:unknown-fields mixed)))
    (is (:source-unchanged mixed))
    (is (= :invalid-mcp-request (:error-type nested)))
    (is (= ["decisions[0].unexpected"] (:unknown-fields nested)))
    (is (:source-unchanged nested))))

(deftest apply-uses-the-retained-basis-once-and-preserves-kept-sites
  (change-buffer/clear-bases!)
  (let [project (temp-project)
        prepared (prepare! project)
        sites-by-owner (into {} (map (juxt :owner :id) (:sites prepared)))
        decisions [{:site (sites-by-owner "target")
                    :replace "(defn compute-target [x] (inc x))"}
                   {:site (sites-by-owner "caller")
                    :replace (str "(defn caller [x]\n"
                                  "  ;; target here is only a comment\n"
                                  "  (compute-target x))")}
                   {:site (sites-by-owner "target-test") :keep true}]
        verified (atom 0)
        result (change-buffer/apply-basis!
                 {:project-root (:root project)
                  :receipt-dir (io/file (:root project) "receipts")
                  :verify! (fn [_ profile _ files]
                             (swap! verified inc)
                             {:ok true :profile profile :files files})}
                 {:basis (:basis prepared)
                  :decisions decisions
                  :verify "fast"})]
    (is (:ok result))
    (is (= 1 @verified))
    (is (.exists (io/file (:receipt-file result))))
    (is (= (str "(ns sample.core)\n"
                "(defn compute-target [x] (inc x))\n"
                "(defn caller [x]\n"
                "  ;; target here is only a comment\n"
                "  (compute-target x))\n")
           (slurp (:source project))))
    (is (= test-source (slurp (:test-file project))))))

(deftest stale-source-refuses-before-any-write
  (change-buffer/clear-bases!)
  (let [project (temp-project)
        prepared (prepare! project)
        first-site (first (:sites prepared))
        original-test (slurp (:test-file project))]
    (spit (:source project) (str core-source "; concurrent\n"))
    (let [result (change-buffer/apply-basis!
                   {:project-root (:root project)
                    :receipt-dir (io/file (:root project) "receipts")
                    :verify! (fn [& _] {:ok true})}
                   {:basis (:basis prepared)
                    :decisions (mapv (fn [{:keys [id source]}]
                                       (if (= id (:id first-site))
                                         {:site id :replace
                                          (str/replace-first source "target" "stale-target")}
                                         {:site id :keep true}))
                                     (:sites prepared))})]
      (is (= :source-hash-mismatch (:error-type result)))
      (is (= original-test (slurp (:test-file project)))))))

(deftest failed-verification-rolls-back-every-addressed-edit
  (change-buffer/clear-bases!)
  (let [project (temp-project)
        prepared (prepare! project)
        decisions (mapv (fn [{:keys [id owner]}]
                          (if (= owner "caller")
                            {:site id
                             :replace (str "(defn caller [x]\n"
                                           "  ;; target here is only a comment\n"
                                           "  (compute-target x))")}
                            {:site id :keep true}))
                        (:sites prepared))
        result (change-buffer/apply-basis!
                 {:project-root (:root project)
                  :receipt-dir (io/file (:root project) "receipts")
                  :verify! (fn [& _] {:ok false :profile "fast" :exit 1})}
                 {:basis (:basis prepared) :decisions decisions})]
    (is (= :verification-failed (:error-type result)))
    (is (:rolled-back result))
    (is (= core-source (slurp (:source project))))
    (is (= test-source (slurp (:test-file project))))))

(deftest basis-coverage-and-provider-ambiguity-refuse-as-data
  (change-buffer/clear-bases!)
  (let [project (temp-project)
        prepared (prepare! project)
        incomplete (change-buffer/apply-basis!
                     {:project-root (:root project)}
                     {:basis (:basis prepared) :decisions []})
        ambiguous (change-buffer/prepare-change!
                    {:project-root (:root project)
                     :semantic-resolver (fn [_]
                                          {:ok false
                                           :error-type :semantic-provider-refusal
                                           :error "found 2"})}
                    {:subject "sample.core/target" :intent "Change it"})]
    (is (= :basis-coverage-mismatch (:error-type incomplete)))
    (is (:source-unchanged incomplete))
    (is (= :semantic-provider-refusal (:error-type ambiguous)))
    (is (:source-unchanged ambiguous))))

(deftest budgets-and-decision-holes-refuse-without-changing-source
  (change-buffer/clear-bases!)
  (let [project (temp-project)
        budgeted (with-redefs [change-buffer/max-visible-characters 5]
                   (prepare! project))]
    (is (= :change-buffer-budget-exceeded (:error-type budgeted)))
    (is (= 5 (get-in budgeted [:limits :visible-characters])))
    (is (= core-source (slurp (:source project))))
    (let [prepared (prepare! project)
          sites (:sites prepared)
          invalid-decisions
          (mapv (fn [{:keys [id]}]
                  (if (= id (:id (first sites)))
                    {:site id :keep true :replace "(identity :wrong)"}
                    {:site id :keep true}))
                sites)
          invalid (change-buffer/apply-basis!
                    {:project-root (:root project)}
                    {:basis (:basis prepared) :decisions invalid-decisions})
          unchanged-decisions
          (mapv (fn [{:keys [id source]}]
                  (if (= id (:id (first sites)))
                    {:site id :replace source}
                    {:site id :keep true}))
                sites)
          unchanged (change-buffer/apply-basis!
                      {:project-root (:root project)}
                      {:basis (:basis prepared) :decisions unchanged-decisions})]
      (is (= :invalid-basis-decision (:error-type invalid)))
      (is (:source-unchanged invalid))
      (is (= :unchanged-basis-decision (:error-type unchanged)))
      (is (:source-unchanged unchanged))
      (is (= core-source (slurp (:source project)))))))
