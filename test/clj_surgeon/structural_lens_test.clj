(ns clj-surgeon.structural-lens-test
  (:require
   [clj-surgeon.structural-lens :as lens]
   [clojure.edn :as edn]
   [clojure.string :as str]
   [clojure.test :refer [deftest is testing]]))

(def sample-source
  "(ns demo.views)\n\n(defn other-pane []\n  (ds/post-action* \"/api/book/new-node\" :other))\n\n(defn book-workshop-pane [surface]\n  [:div.panel\n   [:button.secondary\n    {:data-star-on:click\n     (ds/post-action* \"/api/book/new-node\" surface)}\n    \"New\"]\n   ;; This sibling must survive a replacement byte-for-byte.\n   [:p.note \"Keep me\"]])\n")

(def target-pattern
  "(ds/post-action* \"/api/book/new-node\" _)")

(defn with-temp-file [source f]
  (let [tmp (java.io.File/createTempFile "clj-surgeon-lens" ".clj")]
    (spit tmp source)
    (try
      (f (.getAbsolutePath tmp))
      (finally
        (.delete tmp)))))

(deftest find-subforms-is-structural-scoped-and-auditable
  (let [result (lens/find-subforms sample-source
                                   {:inside 'book-workshop-pane
                                    :match target-pattern})
        match (first (:matches result))]
    (testing "a symbol _ matches one arbitrary subtree"
      (is (= 1 (:match-count result))))
    (testing "the enclosing top-level form excludes an identical call elsewhere"
      (is (= "book-workshop-pane" (:inside result)))
      (is (= "book-workshop-pane" (:inside match))))
    (testing "the result is small but sufficient for review and later addressing"
      (is (= "(ds/post-action* \"/api/book/new-node\" surface)" (:source match)))
      (is (= 10 (:line match)))
      (is (= [{:form 'book-workshop-pane}
              {:vector-tag :div.panel}
              {:vector-tag :button.secondary}
              {:attr :data-star-on:click}
              {:call 'ds/post-action*}]
             (:path match)))
      (is (integer? (-> match :address :preorder))))
    (testing "plans identify the exact source snapshot"
      (is (re-matches #"[0-9a-f]{64}" (:source-hash result))))))

(deftest structural-matching-ignores-formatting
  (let [source "(ns x)\n(defn f []\n  (+\n    1\n    2))\n"]
    (is (= 1 (:match-count
               (lens/find-subforms source {:inside 'f :match "(+ 1 2)"}))))))

(deftest structural-wildcard-matches-one-subtree-not-variadic-arity
  (let [source "(ns x)\n(defn f [] (loop [i 0] (if (< i 3) (recur (inc i)) i)))\n"]
    (testing "one wildcard cannot absorb both loop arguments"
      (is (zero? (:match-count
                   (lens/find-subforms source {:inside 'f :match "(loop _)"})))))
    (testing "one wildcard per loop argument matches on the first attempt"
      (is (= 1 (:match-count
                 (lens/find-subforms source {:inside 'f :match "(loop _ _)"})))))))

(deftest match-and-replacement-require-exactly-one-complete-form
  (testing "missing forms are not interpreted as the literal nil form"
    (is (= :invalid-match
           (:error-type (lens/find-subforms sample-source {:match nil}))))
    (is (= :invalid-replacement
           (:error-type (lens/plan-replacement sample-source
                                               {:match target-pattern :with nil})))))
  (testing "trailing match syntax is rejected instead of silently ignored"
    (let [result (lens/find-subforms sample-source
                                     {:inside 'book-workshop-pane
                                      :match (str target-pattern " (unrelated)")})]
      (is (= :invalid-match (:error-type result)))
      (is (str/includes? (:error result) "exactly one"))))
  (testing "trailing replacement syntax is rejected instead of silently ignored"
    (let [result (lens/plan-replacement sample-source
                                        {:inside 'book-workshop-pane
                                         :match target-pattern
                                         :with "(replacement) (unrelated)"})]
      (is (= :invalid-replacement (:error-type result)))
      (is (str/includes? (:error result) "exactly one")))))

(deftest find-reports-all-ambiguity-replace-refuses-it
  (let [source "(ns x)\n(defn f [] [(inc 1) (inc 1)])\n"
        found (lens/find-subforms source {:inside 'f :match "(inc 1)"})
        plan (lens/plan-replacement source {:inside 'f
                                            :match "(inc 1)"
                                            :with "(bump 1)"})]
    (is (= 2 (:match-count found)))
    (is (= "Expected exactly one match, found 2" (:error plan)))
    (is (= 2 (:match-count plan)))))

(deftest file-wide-find-names-each-enclosing-form-for-direct-narrowing
  ;; Clean Codex benchmark regression: two identical expressions previously
  ;; forced find-subform -> show-form :line merely to recover their owners.
  (let [source "(ns field.case)\n\n(defn transition [state event]\n  (case event\n    :finish (assoc state :status :done)\n    state))\n\n(defn unrelated-finish [state]\n  (assoc state :status :done))\n"
        result (lens/find-subforms source
                                   {:match "(assoc state :status :done)"})]
    (is (= 2 (:match-count result)))
    (is (= ["transition" "unrelated-finish"]
           (mapv :inside (:matches result))))
    (is (= [5 9] (mapv :line (:matches result))))
    (is (every? #(integer? (get-in % [:address :preorder]))
                (:matches result)))))

(deftest file-wide-find-does-not-invent-an-owner-for-unnamed-top-level-forms
  (let [result (lens/find-subforms "(ns loose)\n(comment (inc 1))\n"
                                   {:match "(inc 1)"})
        match (first (:matches result))]
    (is (= 1 (:match-count result)))
    (is (not (contains? match :inside)))
    (is (= "(inc 1)" (:source match)))))

(deftest replacement-plan-is-pure-reviewable-and-replayable
  (let [opts {:inside 'book-workshop-pane
              :match target-pattern
              :with "(book-tree/creation-actions surface)"}
        plan (lens/plan-replacement sample-source opts)]
    (testing "planning does not need or alter a file"
      (is (nil? (:error plan))))
    (testing "the plan records the exact edit and both snapshot hashes"
      (is (= 1 (:plan-version plan)))
      (is (= :replace-subform (:operation plan)))
      (is (= 1 (:match-count plan)))
      (is (= "(ds/post-action* \"/api/book/new-node\" surface)"
             (-> plan :edits first :before)))
      (is (= "(book-tree/creation-actions surface)"
             (-> plan :edits first :after)))
      (is (not= (:source-hash plan) (:result-hash plan)))
      (is (= {:inside "book-workshop-pane"
              :match target-pattern
              :expected-match-count 1}
             (:selector plan)))
      (is (= "clj-surgeon" (get-in plan [:provenance :tool])))
      (is (string? (get-in plan [:provenance :tool-version])))
      (is (= (:source-hash plan) (get-in plan [:provenance :source-hash])))
      (is (= (:result-hash plan) (get-in plan [:provenance :result-hash]))))
    (testing "the review surface is a real unified diff"
      (is (str/starts-with? (:diff plan) "--- a/source.clj\n+++ b/source.clj\n@@"))
      (is (str/includes? (:diff plan)
                         "-(ds/post-action* \"/api/book/new-node\" surface)"))
      (is (str/includes? (:diff plan)
                         "+(book-tree/creation-actions surface)")))
    (testing "applying the emitted plan, rather than rerunning the selector, produces source"
      (let [applied (lens/apply-plan sample-source plan)]
        (is (:ok applied))
        (is (str/includes? (:source applied)
                           "(book-tree/creation-actions surface)"))
        (is (not (str/includes? (:source applied)
                                "(ds/post-action* \"/api/book/new-node\" surface)")))
        (is (str/includes? (:source applied)
                           ";; This sibling must survive a replacement byte-for-byte."))))))

(deftest applying-a-plan-fails-closed-when-source-changed
  (let [plan (lens/plan-replacement sample-source
                                    {:inside 'book-workshop-pane
                                     :match target-pattern
                                     :with "(book-tree/creation-actions surface)"})
        changed (str sample-source "\n;; concurrent edit\n")
        result (lens/apply-plan changed plan)]
    (is (= "Source hash does not match plan" (:error result)))
    (is (nil? (:source result)))))

(deftest apply-rejects-invalid-plan-version-and-invalid-result-source
  (let [plan (lens/plan-replacement sample-source
                                    {:inside 'book-workshop-pane
                                     :match target-pattern
                                     :with "(book-tree/creation-actions surface)"})]
    (testing "unknown plan schemas fail closed"
      (let [result (lens/apply-plan sample-source (assoc plan :plan-version 999))]
        (is (= :unsupported-plan-version (:error-type result)))))
    (testing "a plan for another operation cannot enter the replacement applier"
      (let [result (lens/apply-plan sample-source (assoc plan :operation :something-else))]
        (is (= :unsupported-plan-operation (:error-type result)))))
    (testing "the complete rewritten file is reparsed before it can be written"
      (let [tampered (-> plan
                         (assoc-in [:edits 0 :after] "(broken")
                         (dissoc :result-hash))
            result (lens/apply-plan sample-source tampered)]
        (is (= :invalid-result-source (:error-type result)))
        (is (nil? (:source result)))))))

(deftest verified-apply-receipt-is-pure-and-refuses-read-back-mismatch
  (let [plan (assoc (lens/plan-replacement
                      sample-source
                      {:inside 'book-workshop-pane
                       :match target-pattern
                       :with "(book-tree/creation-actions surface)"})
                    :file "src/demo/views.clj")
        applied (lens/apply-plan sample-source plan)
        receipt (lens/verified-apply-receipt plan (:source applied))
        mismatch (lens/verified-apply-receipt plan sample-source)]
    (is (= {:ok true
            :operation :replace-subform!
            :planned-operation :replace-subform
            :file "src/demo/views.clj"
            :source-hash (:source-hash plan)
            :result-hash (:result-hash plan)
            :applied-edit (first (:edits plan))
            :verified {:whole-file-parsed true
                       :atomic-write true
                       :read-back-hash (:result-hash plan)}}
           receipt))
    (is (= :read-back-hash-mismatch (:error-type mismatch)))
    (is (= (:result-hash plan) (:expected-result-hash mismatch)))
    (is (= (lens/source-hash sample-source) (:read-back-hash mismatch)))))

(deftest plan-out-writes-the-complete-versioned-plan
  (with-temp-file sample-source
    (fn [source-path]
      (let [plan-file (java.io.File/createTempFile "clj-surgeon-plan" ".edn")]
        (try
          (let [result (lens/plan-file-replacement
                         {:file source-path
                          :inside 'book-workshop-pane
                          :match target-pattern
                          :with "(book-tree/creation-actions surface)"
                          :plan-out (.getAbsolutePath plan-file)})
                saved (edn/read-string (slurp plan-file))]
            (is (= (.getAbsolutePath plan-file) (:plan-out result)))
            (is (= 1 (:plan-version saved)))
            (is (= (:source-hash result) (:source-hash saved)))
            (is (= (:result-hash result) (:result-hash saved)))
            (is (= (:provenance result) (:provenance saved))))
          (finally
            (.delete plan-file)))))))

(deftest execute-plan-writes-only-after-validation
  (with-temp-file sample-source
    (fn [source-path]
      (let [plan (assoc (lens/plan-replacement sample-source
                                               {:inside 'book-workshop-pane
                                                :match target-pattern
                                                :with "(book-tree/creation-actions surface)"})
                        :file source-path)
            result (lens/execute-plan! {:plan plan})]
        (is (:ok result))
        (is (= :replace-subform! (:operation result)))
        (is (= source-path (:file result)))
        (is (= (:source-hash plan) (:source-hash result)))
        (is (= (:result-hash plan) (:result-hash result)))
        (is (= (first (:edits plan)) (:applied-edit result)))
        (is (= {:whole-file-parsed true
                :atomic-write true
                :read-back-hash (:result-hash plan)}
               (:verified result)))
        (is (str/includes? (slurp source-path)
                           "(book-tree/creation-actions surface)"))))))

;; ============================================================
;; :expect — the pure guard behind the one-call guarded edit
;; ============================================================

(deftest parse-expect-accepts-exactly-one-complete-form
  (testing "one form parses to data plus its normalized source"
    (is (= {:expect '(assoc state :status :done)
            :expect-source "(assoc state :status :done)"}
           (lens/parse-expect "(assoc state :status :done)"))))
  (testing "an already-read form is accepted verbatim"
    (is (= '(assoc state :status :done)
           (:expect (lens/parse-expect '(assoc state :status :done))))))
  (testing "row 5: zero, several, or unreadable forms refuse"
    (doseq [[label value] [["zero forms" ""]
                           ["whitespace only" "   \n  "]
                           ["comment only" ";; nothing here\n"]
                           ["two forms" "(assoc state :status :done) :extra"]
                           ["reader error" "(assoc state"]]]
      (testing label
        (let [result (lens/parse-expect value)]
          (is (= :invalid-expect (:error-type result)))
          (is (string? (:error result)))
          (is (nil? (:expect result))))))))

(deftest expect-comparison-is-whitespace-and-comment-insensitive
  (testing "row 2/3: identical structure matches"
    (let [comparison (lens/expect-comparison '(assoc state :status :done)
                                             "(assoc state :status :done)")]
      (is (true? (:match? comparison)))
      (is (= '(assoc state :status :done) (:actual comparison)))
      (is (= "(assoc state :status :done)" (:actual-source comparison)))))
  (testing "row 8: source comments and odd whitespace do not change the verdict"
    (let [before "(assoc    state\n  ;; keep the audit note\n  :status\n\n  :done)"
          comparison (lens/expect-comparison '(assoc state :status :done) before)]
      (is (true? (:match? comparison)))
      (is (= '(assoc state :status :done) (:actual comparison)))
      (is (= before (:actual-source comparison))
          "the exact selected bytes are still reported")))
  (testing "row 4: the audit-payload trap refuses"
    (let [comparison (lens/expect-comparison
                       '(assoc state :status :done)
                       "(assoc state :status :done :audit (:audit payload))")]
      (is (false? (:match? comparison)))
      (is (= '(assoc state :status :done :audit (:audit payload))
             (:actual comparison)))))
  (testing "row 4: a multi-form span selection never matches one :expect form"
    (let [comparison (lens/expect-comparison
                       :finish
                       ":finish\n;; comment\n(assoc state :status :done)")]
      (is (false? (:match? comparison)))
      (is (= 2 (:actual-form-count comparison)))
      (is (= [:finish '(assoc state :status :done)] (:actual comparison))))))

(deftest expect-mismatch-result-reports-both-forms-and-the-exact-source
  (let [plan {:operation :replace-subform
              :file "src/state.clj"
              :selector {:query [[:form 'transition]]}
              :source-hash "abc"
              :edits [{:line 8 :before "(assoc state :status :done)"}]}
        comparison (lens/expect-comparison '(assoc state :status :complete)
                                           "(assoc state :status :done)")
        result (lens/expect-mismatch-result plan comparison)]
    (is (= :expect-mismatch (:error-type result)))
    (is (= :edit (:operation result)))
    (is (= :expect-guarded (:mode result)))
    (is (= "src/state.clj" (:file result)))
    (is (= '(assoc state :status :complete) (:expected result)))
    (is (= '(assoc state :status :done) (:actual result)))
    (is (= "(assoc state :status :done)" (:actual-source result)))
    (is (= 8 (:line result)))
    (is (= "abc" (:source-hash result)))
    (is (nil? (:actual-form-count result)))
    (is (str/includes? (:error result) ":expect"))
    (is (nil? (:ok result)))))
