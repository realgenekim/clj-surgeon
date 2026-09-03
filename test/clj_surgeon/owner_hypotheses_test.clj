(ns clj-surgeon.owner-hypotheses-test
  (:require
   [cheshire.core :as json]
   [clj-surgeon.owner-hypotheses :as hypotheses]
   [clojure.string :as str]
   [clojure.test :refer [deftest is testing]]))

(defn- records
  [names]
  (mapv (fn [line owner]
          {:type 'defn
           :name (symbol owner)
           :platforms [:clj]
           :line line
           :end-line line
           :source (str "(defn " owner " [] nil)")})
        (range 1 (inc (count names)))
        names))

(deftest ranks-each-missing-owner-over-real-unresolved-names
  ;; @spec MCP-OP-READ-HYP-001
  (let [owners (records ["editor-hybrid-schema"
                         "editor-gesture-schema"
                         "editor-programs-schema"
                         "other-owner"])
        program (hypotheses/rank-owner-hypotheses
                  "editor-program-schema" owners
                  {:resolved-names ["editor-hybrid-schema"]})
        edit (hypotheses/rank-owner-hypotheses
               "editor-edit-schema" owners
               {:resolved-names ["editor-hybrid-schema"]})]
    (is (= "editor-program-schema" (:requested-owner program)))
    (is (= "editor-edit-schema" (:requested-owner edit)))
    (is (= 4 (:available-owner-count program)))
    (is (= 1 (:excluded-resolved-owner-count program)))
    (is (= 3 (:candidate-count program)))
    (is (= ["editor-programs-schema" "editor-gesture-schema" "other-owner"]
           (mapv :owner (:did-you-mean program))))
    (is (= "editor-gesture-schema" (-> edit :did-you-mean first :owner)))
    (is (not-any? #(= "editor-hybrid-schema" (:owner %))
                  (:did-you-mean program)))))

(deftest hypotheses-are-bounded-deterministic-and-never-authoritative
  ;; @spec MCP-OP-READ-HYP-002
  (let [owners (-> (records (mapv #(str "owner-" %) (range 12)))
                   (assoc-in [0 :platforms] [:cljs]))
        opts {:platform :clj}
        first-result (hypotheses/rank-owner-hypotheses
                       "owner-13" owners opts)
        second-result (hypotheses/rank-owner-hypotheses
                        "owner-13" owners opts)]
    (is (= first-result second-result))
    (is (false? (:authority first-result)))
    (is (= :normalized-levenshtein (:ranking-basis first-result)))
    (is (= 11 (:available-owner-count first-result)))
    (is (= 10 (:candidates-returned first-result)))
    (is (:candidates-truncated first-result))
    (is (every? #(false? (:authority %)) (:did-you-mean first-result)))
    (is (every? #(= :normalized-levenshtein (:ranking-basis %))
                (:did-you-mean first-result)))
    (is (every? #(= ["clj"] (:platforms %))
                (:did-you-mean first-result)))
    (is (every? #(not (contains? % :score))
                (:did-you-mean first-result)))
    (is (every? #(not (contains? % :source))
                (:did-you-mean first-result)))))

(def ^:private strict-corpus
  ;; Six one-to-one refusal/recovery pairs from the 2026-08-25 field corpus.
  [["resolve-source-file" "resolve-source-path"
    ["supported-source-extensions" "relative-source-path?" "real-root"
     "path-refusal" "resolve-source-path" "resolve-new-source-path"]]
   ["start-server!" "start"
    ["default-log-file" "server-instructions" "warn" "outcome-classes-by-tool"
     "public-tool-registry" "registered-tools" "make-tools" "live-tool-state"
     "tool-contract" "tool-contracts" "tool-sync-plan" "structured-call-result"
     "create-structured-async-tool" "create-async-tool" "add-tool!" "remove-tool!"
     "register-live-server!" "unregister-live-server!" "sync-tools!"
     "configure-specification" "build-stdio-server" "start-embedded-nrepl!"
     "armor-stdout!" "normalize-option" "start"]]
   ["editor-program-schema" "editor-programs-schema"
    ["verification-schema" "basis-change-schema" "positive-integer-schema"
     "defmethod-owner-schema" "explicit-change-schema" "editor-gesture-schema"
     "editor-programs-schema" "editor-hybrid-schema" "editor-tool-schema"
     "extraction-schema" "clj-change-schema" "closed-object-shape"
     "direct-contract-shape" "direct-change-contract"
     "editor-gesture-contract-shape" "editor-gesture-contract"]
    {:resolved-names ["editor-hybrid-schema"]}]
   ["editor-edit-schema" "editor-gesture-schema"
    ["verification-schema" "basis-change-schema" "positive-integer-schema"
     "defmethod-owner-schema" "explicit-change-schema" "editor-gesture-schema"
     "editor-programs-schema" "editor-hybrid-schema" "editor-tool-schema"
     "extraction-schema" "clj-change-schema" "closed-object-shape"
     "direct-contract-shape" "direct-change-contract"
     "editor-gesture-contract-shape" "editor-gesture-contract"]
    {:resolved-names ["editor-hybrid-schema"]}]
   ["tools-list-publishes-the-compact-editor-contract"
    "exposes-exactly-four-typed-tools"
    ["temp-dir" "delete-tree!"
     "tool-profiles-preserve-full-default-and-isolate-the-editor"
     "exposes-exactly-four-typed-tools"
     "live-tool-sync-plans-contract-changes-without-handler-churn"
     "embedded-nrepl-starts-without-resolving-cider"
     "embedded-nrepl-redefines-the-live-handler-var"
     "nested-live-server-registration-restores-the-outer-server"]]
   ["tool-profiles-expose-only-the-intended-catalog"
    "tool-profiles-preserve-full-default-and-isolate-the-editor"
    ["temp-dir" "delete-tree!"
     "tool-profiles-preserve-full-default-and-isolate-the-editor"
     "exposes-exactly-four-typed-tools"
     "live-tool-sync-plans-contract-changes-without-handler-churn"
     "embedded-nrepl-starts-without-resolving-cider"
     "embedded-nrepl-redefines-the-live-handler-var"
     "nested-live-server-registration-restores-the-outer-server"]]])

(deftest strict-field-corpus-has-complete-top-ten-recall
  (let [ranks
        (mapv
          (fn [[requested intended candidates opts]]
            (some (fn [{:keys [rank owner]}]
                    (when (= intended owner) rank))
                  (:did-you-mean
                    (hypotheses/rank-owner-hypotheses
                      requested (records candidates) opts))))
          strict-corpus)]
    (is (= [1 5 1 2 3 1] ranks))
    (is (every? #(<= % 10) ranks))))

;; ---------------------------------------------------------------------------
;; Multimethod owner addressing. Field case: curtain-call folds.clj, 2026-09-02
;; session 4 — `forms: ["fold-event \"schedule.locked\""]` refused, the outline
;; collapsed every arm to the bare owner name, and the `{kind, name, dispatch}`
;; owner shape was documented only in the apply_clojure_changes schema.
;; ---------------------------------------------------------------------------

(defn- fold-arms
  [dispatches]
  (mapv (fn [line dispatch]
          {:type 'defmethod
           :name 'fold-event
           :dispatch dispatch
           :platforms [:clj]
           :line line
           :end-line line
           :source (str "(defmethod fold-event " dispatch " [s p] s)")})
        (range 10 (+ 10 (count dispatches)))
        dispatches))

(def ^:private plain-owner
  {:type 'defn
   :name 'event-by-id
   :platforms [:clj]
   :line 3
   :end-line 3
   :source "(defn event-by-id [state id] nil)"})

(deftest defmethod-owner-evidence-names-the-exact-owner-form-to-send
  ;; @spec MCP-OP-DISPATCH-002
  (let [records (conj (fold-arms ["\"schedule.locked\"" "\"schedule.unlocked\""
                                  ":agenda/published"])
                      plain-owner)
        evidence (hypotheses/defmethod-owner-evidence
                   "fold-event \"schedule.locked\"" records)]
    (is (= "defmethod" (:owner-kind evidence)))
    (is (= "fold-event" (:name evidence)))
    (is (= 3 (:arm-count evidence)))
    (is (= {:kind "defmethod" :name "fold-event"
            :dispatch "\"schedule.locked\""}
           (:owner-form evidence)))
    (is (true? (:owner-form-is-exact evidence)))
    (is (= "apply_clojure_changes changes[].forms" (:accepted-by evidence)))
    (is (= ["\"schedule.locked\"" "\"schedule.unlocked\"" ":agenda/published"]
           (:dispatch-vocabulary evidence)))
    (is (= 3 (:dispatch-count evidence)))
    (is (= 0 (:dispatch-vocabulary-omitted evidence)))
    (is (false? (:dispatch-vocabulary-truncated evidence)))
    (is (false? (:authority evidence)))))

(deftest defmethod-owner-evidence-bounds-a-large-dispatch-vocabulary
  ;; @spec MCP-OP-DISPATCH-002
  (let [dispatches (mapv #(str "\"event." % "\"") (range 117))
        evidence (hypotheses/defmethod-owner-evidence
                   "fold-event" (fold-arms dispatches))]
    (is (= 117 (:arm-count evidence)))
    (is (= 117 (:dispatch-count evidence)))
    (is (= 40 (:dispatch-vocabulary-returned evidence)))
    (is (= 40 (count (:dispatch-vocabulary evidence))))
    (is (= 77 (:dispatch-vocabulary-omitted evidence)))
    (is (true? (:dispatch-vocabulary-truncated evidence)))
    (testing "a bare multimethod name still teaches the owner form"
      (is (= {:kind "defmethod" :name "fold-event" :dispatch "\"event.0\""}
             (:owner-form evidence)))
      (is (false? (:owner-form-is-exact evidence))))))

(deftest defmethod-owner-evidence-is-absent-for-non-multimethod-owners
  ;; @spec MCP-OP-DISPATCH-002
  (is (nil? (hypotheses/defmethod-owner-evidence "event-by-id" [plain-owner])))
  (is (nil? (hypotheses/defmethod-owner-evidence "no-such-owner"
                                                 [plain-owner]))))

(deftest owner-recovery-evidence-carries-defmethod-addressing-per-failure
  ;; @spec MCP-OP-DISPATCH-002
  (let [records (conj (fold-arms ["\"schedule.locked\"" "\"schedule.unlocked\""])
                      plain-owner)
        failures [{:form "fold-event \"schedule.locked\""
                   :error-type :form-not-found
                   :match-count 0}]
        evidence (hypotheses/owner-recovery-evidence
                   ["fold-event \"schedule.locked\""] records failures)
        failure (first (:selection-failures evidence))]
    (is (= "fold-event \"schedule.locked\"" (:requested-owner failure)))
    (is (= {:kind "defmethod" :name "fold-event"
            :dispatch "\"schedule.locked\""}
           (get-in failure [:defmethod-owner :owner-form])))
    (is (= ["\"schedule.locked\"" "\"schedule.unlocked\""]
           (get-in failure [:defmethod-owner :dispatch-vocabulary])))
    (testing "the collapsed owner vocabulary is still published"
      (is (= ["fold-event" "event-by-id"] (:available-owners evidence))))))

(deftest owner-recovery-evidence-teaches-dispatch-for-an-ambiguous-multimethod
  ;; @spec MCP-OP-DISPATCH-002
  (let [records (fold-arms ["\"schedule.locked\"" "\"schedule.unlocked\""])
        failures [{:form "fold-event"
                   :error-type :ambiguous-form
                   :match-count 2}]
        evidence (hypotheses/owner-recovery-evidence
                   ["fold-event"] records failures)
        failure (first (:selection-failures evidence))]
    (is (= :ambiguous-form (:failure-kind failure)))
    (is (= ["\"schedule.locked\"" "\"schedule.unlocked\""]
           (get-in failure [:defmethod-owner :dispatch-vocabulary])))))

;; ---------------------------------------------------------------------------
;; A dispatch vocabulary bounded only by arm count is not bounded. Sixty long
;; dispatch spellings carrying their own comments produced kilobytes of refusal
;; evidence, and a `;;` inside one commented the rest of the line out.
;; ---------------------------------------------------------------------------

(def ^:private noisy-dispatches
  (mapv (fn [index]
          (str "[:conference.schedule/event-with-a-long-qualified-name-" index
               "\n ;; kept for the 2026 migration\n :legacy-arm]"))
        (range 60)))

(deftest defmethod-owner-evidence-bounds-dispatch-vocabulary-characters
  ;; @spec MCP-OP-DISPATCH-004
  (let [evidence (hypotheses/defmethod-owner-evidence
                   "fold-event" (fold-arms noisy-dispatches))
        vocabulary (:dispatch-vocabulary evidence)
        encoded (json/generate-string vocabulary)]
    (is (= 60 (:dispatch-count evidence)))
    (testing "the vocabulary fits its published character budget"
      (is (<= (count encoded)
              hypotheses/dispatch-vocabulary-character-limit))
      (is (true? (:dispatch-vocabulary-truncated evidence)))
      (is (= (count vocabulary) (:dispatch-vocabulary-returned evidence)))
      (is (= (- 60 (count vocabulary))
             (:dispatch-vocabulary-omitted evidence)))
      (is (pos? (count vocabulary))))
    (testing "every entry is one comment-free line, so a join cannot mangle it"
      (is (not-any? #(str/includes? % ";;") vocabulary))
      (is (not-any? #(str/includes? % "\n") vocabulary))
      (is (not (str/includes? (str/join ", " vocabulary) ";;")))
      (is (not (str/includes? (str/join ", " vocabulary) "\n"))))
    (testing "the exact owner form to send is rendered the same way"
      (let [dispatch (get-in evidence [:owner-form :dispatch])]
        (is (not (str/includes? dispatch ";;")))
        (is (not (str/includes? dispatch "\n")))
        (is (= "[:conference.schedule/event-with-a-long-qualified-name-0 :legacy-arm]"
               dispatch))))))

;; ---------------------------------------------------------------------------
;; A dispatch spelling that embeds a literal newline (a multi-line string
;; literal in source, written with a real Enter inside the quotes rather than
;; an escaped `\n`) survived presentation with that newline intact: Sol's
;; 2026-09-03 re-review returned contains-newline=true, joined-line-count=2 for
;; exactly this shape. `presentation-node` only collapsed whitespace between a
;; node's *children*; a string node has no children, so its raw source text —
;; newline included — passed through `n/string` unchanged.
;; ---------------------------------------------------------------------------

(deftest defmethod-owner-evidence-escapes-a-multiline-string-dispatch
  ;; @spec MCP-OP-DISPATCH-004
  (let [multiline-dispatch (str "\"a" \newline "b\"")
        records (fold-arms [multiline-dispatch])
        evidence (hypotheses/defmethod-owner-evidence "fold-event" records)
        presented (get-in evidence [:owner-form :dispatch])]
    (testing "the presented dispatch is exactly one physical line"
      (is (not (str/includes? presented "\n")))
      (is (= 1 (count (str/split-lines presented)))))
    (testing "the presented dispatch still reads back to the original value"
      (is (= (read-string multiline-dispatch) (read-string presented))))
    (testing "the selector chooses the right arm from the presented spelling"
      (let [attempted (hypotheses/defmethod-owner-evidence
                        (str "fold-event " presented) records)]
        (is (true? (:owner-form-is-exact attempted)))
        (is (= presented (get-in attempted [:owner-form :dispatch])))))))
