(ns clj-surgeon.edit-dsl-test
  (:require
   [clj-surgeon.edit-dsl :as dsl]
   [clj-surgeon.structural-lens :as lens]
   [clojure.string :as str]
   [clojure.test :refer [deftest is testing]]
   [sci.core :as sci]))

(deftest native-expression-compiles-to-the-existing-case-query
  (is (= [[:form 'transition]
          [:find :finish]
          :right
          [:replace '(assoc state :status :complete)]]
         (-> (dsl/form 'transition)
             (dsl/match :finish)
             dsl/right
             (dsl/replace '(assoc state :status :complete))))))

(deftest line-starts-a-query-at-the-top-level-form-containing-a-physical-line
  (is (= [[:line 42]] (dsl/line 42)))
  (is (= [[:line 42]
          [:find '(old-reader account-id)]
          [:replace '(new-reader account-id)]]
         (-> (dsl/line 42)
             (dsl/match '(old-reader account-id))
             (dsl/replace '(new-reader account-id)))))
  (is (= [[:line 42] [:find :finish]]
         (dsl/compile-query "(-> (line 42) (match :finish))"))))

(deftest line-refuses-values-that-cannot-identify-a-physical-source-line
  (doseq [value [nil 0 -1 1.5 "2" :two]]
    (testing (pr-str value)
      (let [error (try
                    (dsl/line value)
                    nil
                    (catch Exception exception
                      (ex-data exception)))]
        (is (= :invalid-line-root (:error-type error)))
        (is (= value (:line error)))))))

(def builder-cases
  (let [transformer identity]
    [["match" #(dsl/match % '(call _)) [:find '(call _)]]
     ["where" #(dsl/where % {:parent-tag :vector})
      [:where {:parent-tag :vector}]]
     ["right" dsl/right :right]
     ["left" dsl/left :left]
     ["up" dsl/up :up]
     ["down" dsl/down :down]
     ["span" #(dsl/span % 2) [:span 2]]
     ["partition" #(dsl/partition-all % 2) [:partition-all 2]]
     ["replace" #(dsl/replace % '(inc x)) [:replace '(inc x)]]
     ["replace span" #(dsl/replace-span % :a :b) [:replace-span :a :b]]
     ["transform" #(dsl/transform % transformer) [:transform transformer]]]))

(deftest every-builder-appends-one-existing-step-to-every-valid-path-shape
  (doseq [start [[]
                 (dsl/form 'f)
                 [[:form 'f] :down]]
          [label build expected-step] builder-cases]
    (testing (str label " after " (pr-str start))
      (is (= (conj start expected-step) (build start)))
      (is (= start (pop (build start)))))))

(deftest replacement-builders-append-the-existing-terminal-steps
  (is (= [[:form 'f] [:replace '(inc x)]]
         (dsl/replace (dsl/form 'f) '(inc x))))
  (is (= [[:form 'f] [:span 2] [:replace-span :finish '(inc x)]]
         (-> (dsl/form 'f)
             (dsl/span 2)
             (dsl/replace-span :finish '(inc x)))))
  (let [transformer #(mapv inc %)]
    (is (= [[:form 'f] [:transform transformer]]
           (dsl/transform (dsl/form 'f) transformer)))))

(deftest builders-preserve-clojure-data-without-evaluation
  (let [effect-count (atom 0)
        values ['symbol
                :keyword
                '(list 1 2)
                [1 :two]
                {:one 1}
                #{:one :two}
                (with-meta '(metadata) {:line 9})
                (reader-conditional '(:clj :jvm) false)
                '(swap! effect-count inc)]]
    (doseq [value values]
      (testing (pr-str value)
        (let [query (-> (dsl/form 'f)
                        (dsl/match value)
                        (dsl/replace value))]
          (is (identical? value (-> query second second)))
          (is (identical? value (-> query last second)))
          (is (= (meta value) (meta (-> query second second)))))))
    (is (zero? @effect-count))))

(deftest every-builder-refuses-invalid-paths
  (doseq [path [nil '() {} :path]
          [label build _] builder-cases]
    (testing (str label " on " (pr-str path))
      (let [error (try
                    (build path)
                    nil
                    (catch Exception e (ex-data e)))]
        (is (= {:error-type :invalid-edit-path
                :path path}
               error))))))

(deftest every-builder-refuses-composition-after-a-terminal-step
  (doseq [terminal [[[:replace :done]]
                    [[:replace-span :a :b]]
                    [[:transform identity]]]
          [label build _] builder-cases]
    (testing (str label " after " (pr-str terminal))
      (let [error (try
                    (build terminal)
                    nil
                    (catch Exception e (ex-data e)))]
        (is (= {:error-type :terminal-edit-step
                :terminal-step (last terminal)}
               error))))))

(deftest transform-requires-a-function
  (doseq [value [nil 1 "not callable" '(not callable)]]
    (let [error (try
                  (dsl/transform (dsl/form 'f) value)
                  nil
                  (catch Exception exception
                    (ex-data exception)))]
      (is (= :invalid-edit-transform (:error-type error)))
      (is (= value (:transform error))))))

(deftest expect-count-composes-with-terminal-transform
  ;; Field failure: the public MCP example guarded a transform with expect-count,
  ;; but expect-count returned a selection map that transform rejected as a path.
  (let [query (dsl/compile-query
                "(-> (form 'retry-policy) initializer (match :retry-delays) right (expect-count 1) (transform (fn [delays] (mapv (partial + 100) delays))))")]
    (is (= [[:form 'retry-policy]
            :initializer
            [:find :retry-delays]
            :right]
           (pop query)))
    (is (= :transform (first (peek query))))
    (is (= [101 102 103]
           ((second (peek query)) [1 2 3])))))

(deftest counted-builders-require-positive-integers
  (doseq [[builder step] [[dsl/span :span]
                          [dsl/partition-all :partition-all]]
          value [nil 0 -1 1.5 "2" :two]]
    (testing (str step " " (pr-str value))
      (let [error (try
                    (builder (dsl/form 'f) value)
                    nil
                    (catch Exception e (ex-data e)))]
        (is (= :invalid-step-argument (:error-type error)))
        (is (= step (:step error)))
        (is (= value (:value error))))))
  (is (= [[:form 'f] [:span 1]]
         (dsl/span (dsl/form 'f) 1)))
  (is (= [[:form 'f] [:partition-all 3]]
         (dsl/partition-all (dsl/form 'f) 3))))

(deftest real-program-peer-routes-compile-exactly
  (is (= [[:form 'classify-request]
          [:find '(:public? resource)]
          :right
          [:find :public]
          [:replace :public-resource]]
         (-> (dsl/form 'classify-request)
             (dsl/match '(:public? resource))
             dsl/right
             (dsl/match :public)
             (dsl/replace :public-resource))))
  (is (= [[:form 'prepare-request]
          [:find 'timeout-ms]
          [:where {:parent-tag :vector}]
          :right
          [:replace '(or (:timeout-ms request) 5000)]]
         (-> (dsl/form 'prepare-request)
             (dsl/match 'timeout-ms)
             (dsl/where {:parent-tag :vector})
             dsl/right
             (dsl/replace '(or (:timeout-ms request) 5000)))))
  (is (= [[:form 'route-event]
          [:find 'case]
          :up
          :down
          :right
          :right
          [:partition-all 2]]
         (-> (dsl/form 'route-event)
             (dsl/match 'case)
             dsl/up
             dsl/down
             dsl/right
             dsl/right
             (dsl/partition-all 2)))))

(deftest sci-compiles-the-native-expression-without-host-setup
  (is (= [[:form 'transition]
          [:find :finish]
          :right
          [:replace '(assoc state :status :complete)]]
         (dsl/compile-query
           "(-> (form 'transition)\n     (match :finish)\n     right\n     (replace '(assoc state :status :complete)))"))))

(deftest sci-compilation-does-not-depend-on-the-callers-current-namespace
  (binding [*ns* (the-ns 'clj-surgeon.edit-dsl-test)]
    (is (= [[:form 'transition]
            [:find :finish]
            :right
            [:replace :complete]]
           (dsl/compile-query
             "(-> (form 'transition) (match :finish) right (replace :complete))")))))

(deftest sci-exposes-every-builder-and-no-unrelated-function
  (is (= [[:line 42]
          [:find :anchor]
          [:where {:parent-tag :vector}]
          :right
          :left
          :up
          :down
          [:span 2]
          [:partition-all 2]
          [:replace-span :a :b]]
         (dsl/compile-query
           "(-> (line 42) (match :anchor) (where {:parent-tag :vector}) right left up down (span 2) (partition-all 2) (replace-span :a :b))")))
  (doseq [expression ["(spit \"/tmp/clj-surgeon-must-not-write\" \"bad\")"
                      "(slurp \"secret\")"
                      "(require '[clojure.java.shell :as shell])"
                      "(eval '(form 'f))"
                      "(resolve 'spit)"
                      "(future (form 'f))"
                      "(def path (form 'f))"
                      "(atom (form 'f))"
                      "(transient (form 'f))"
                      "(loop [] (recur))"
                      "(trampoline identity (form 'f))"
                      "(repeat :right)"
                      "(iterate right (form 'f))"
                      "(java.io.File. \"secret\")"
                      "#=(spit \"/tmp/clj-surgeon-must-not-write\" \"bad\")"]]
    (testing expression
      (let [error (try
                    (dsl/compile-query expression)
                    nil
                    (catch Exception e (ex-data e)))]
        (is (= :invalid-edit-expression (:error-type error)))
        (is (= expression (:expression error)))
        (is (seq (:allowed-symbols error)))
        (is (seq (:allowed-capabilities error)))
        (is (some #{"(match path pattern)"} (:allowed-forms error)))
        (is (re-find #"thread-first" (:remedy error)))))))

(deftest sci-class-mapping-is-the-causal-constructor-capability
  ;; Causal control for the 2026-09-01 Andon review. The pre-ddd074f5 context
  ;; had no class mapping and refused the same shorthand that the widened
  ;; context lowered through `new` into a real host object.
  (let [expression "(IllegalArgumentException. \"boom\")"
        evaluate (fn [classes]
                   (:val (sci/eval-string+
                           (sci/init {:classes classes
                                      :allow '[new]})
                           expression)))
        pre-merge-error (try
                          (evaluate {})
                          nil
                          (catch Exception exception exception))]
    (is (some? pre-merge-error))
    (is (re-find #"Unable to resolve classname"
                 (ex-message pre-merge-error)))
    (is (instance? IllegalArgumentException
                   (evaluate {'IllegalArgumentException
                              IllegalArgumentException})))))

(deftest sci-refuses-host-interop-before-evaluation
  (doseq [{:keys [label expression symbol]}
          [{:label "constructor shorthand"
            :expression "(IllegalArgumentException. \"boom\")"
            :symbol 'IllegalArgumentException.}
           {:label "qualified constructor shorthand"
            :expression "(java.lang.IllegalArgumentException. \"boom\")"
            :symbol 'java.lang.IllegalArgumentException.}
           {:label "host object in a valid replacement query"
            :expression "(-> (form 'f) (replace (IllegalArgumentException. \"boom\")))"
            :symbol 'IllegalArgumentException.}
           {:label "method shorthand in a valid replacement query"
            :expression "(-> (form 'f) (replace (.toUpperCase \"boom\")))"
            :symbol '.toUpperCase}
           {:label "explicit dot form in a valid replacement query"
            :expression "(-> (form 'f) (replace (. \"boom\" toUpperCase)))"
            :symbol '.}
           {:label "field shorthand in a valid replacement query"
            :expression "(-> (form 'f) (replace (.-detail (IllegalArgumentException. \"boom\"))))"
            :symbol '.-detail}]]
    (testing label
      (let [error (try
                    (dsl/compile-query expression)
                    nil
                    (catch Exception exception (ex-data exception)))]
        (is (= :invalid-edit-expression (:error-type error)))
        (is (= :disallowed-symbol (:reason error)))
        (is (= symbol (:symbol error))))))
  (testing "observable host side effects never run"
    (let [expression (str "(do (.printStackTrace "
                          "(IllegalArgumentException. \"SCI-PRINT-SIDE-EFFECT\")) "
                          "(form 'f))")
          previous-err System/err
          output (java.io.ByteArrayOutputStream.)
          capture (java.io.PrintStream. output true "UTF-8")
          error (try
                  (System/setErr capture)
                  (try
                    (dsl/compile-query expression)
                    nil
                    (catch Exception exception (ex-data exception)))
                  (finally
                    (.flush capture)
                    (System/setErr previous-err)
                    (.close capture)))]
      (is (= :invalid-edit-expression (:error-type error)))
      (is (= :disallowed-symbol (:reason error)))
      (is (= '.printStackTrace (:symbol error)))
      (is (= "" (.toString output "UTF-8"))))))

(deftest sci-keeps-case-expansion-and-bounds-its-throw-path
  (is (= [[:form 'f]]
         (dsl/compile-query "(case :match :match (form 'f))")))
  (let [error (try
                (dsl/compile-query "(case :miss :match (form 'f))")
                nil
                (catch Exception exception (ex-data exception)))]
    (is (= :invalid-edit-expression (:error-type error)))
    (is (= :evaluation-failed (:reason error)))))

(deftest sci-provides-pure-clojure-for-structural-computation
  (is (= [[:form 'transition]
          [:find :finish]
          :right
          [:replace {:status :complete :attempts 2}]]
         (dsl/compile-query
           "(let [[target initial] [:finish {:status :done :attempts 1}]
                 candidates (->> [initial {:skip true}]
                                 (filterv #(contains? % :status))
                                 (mapv (comp #(update % :attempts inc)
                                             #(assoc % :status :complete))))]
             (-> (form 'transition)
                 (match target)
                 right
                 (replace (first candidates))))")))
  (is (= [[:form 'f] :right :left :right]
         (dsl/compile-query
           "(reduce (fn [path direction]
                     (case direction
                       :right (right path)
                       :left (left path)))
                   (form 'f)
                   [:right :left :right])")))
  (is (= [[:form 'f] [:replace [:done :done]]]
         (dsl/compile-query
           "(-> (form 'f)
               (replace (first (mapv (juxt identity identity) [:done]))))")))
  (is (= [[:form 'f] [:replace 5]]
         (dsl/compile-query
           "(-> (form 'f) (replace (count (range 5))))")))
  (is (= [[:form 'f] [:replace :yes]]
         (dsl/compile-query
           "(do (form 'ignored)
               (-> (form 'f)
                   (replace (if (and true (not false)) :yes :no))))"))))

(deftest sci-transform-computes-from-the-selected-clojure-form
  (let [source "(ns bench.transform)\n\n(def retry-policy\n  {:delays [100 250 500]})\n\n(def decoy [100 250 500])\n"
        expression "(-> (form 'retry-policy)\n    (match :delays)\n    right\n    (transform #(mapv (partial + 100) %)))"
        query (dsl/compile-query expression)
        plan (dsl/evaluate-edit source
                                {:file "src/bench/transform.clj"
                                 :query query
                                 :plan-out "review.edn"})
        applied (lens/apply-plan source plan)]
    (is (= [[:form 'retry-policy] [:find :delays] :right]
           (pop query)))
    (is (ifn? (-> query peek second)))
    (is (= :replace-subform (:operation plan)))
    (is (= "[100 250 500]" (get-in plan [:edits 0 :before])))
    (is (= "[200 350 600]" (get-in plan [:edits 0 :after])))
    (is (= [[:form 'retry-policy]
            [:find :delays]
            :right
            [:replace [200 350 600]]]
           (get-in plan [:selector :query])))
    (is (:ok applied))
    (is (str/includes? (:source applied) ":delays [200 350 600]"))
    (is (str/includes? (:source applied) "(def decoy [100 250 500])"))))

(deftest transform-runs-only-after-an-exact-selection
  (let [source "(ns bench.transform)\n(def a {:x [1]})\n(def b {:x [1]})\n"
        calls (atom 0)
        transformer (fn [value]
                      (swap! calls inc)
                      value)]
    (doseq [[query error-type] [[(dsl/transform [[:find :missing]] transformer)
                                 :no-match]
                                [(dsl/transform [[:find :x] :right] transformer)
                                 :ambiguous-match]]]
      (is (= error-type
             (:error-type (dsl/evaluate-edit source
                                             {:file "source.clj"
                                              :query query
                                              :plan-out "review.edn"}))))
      (is (zero? @calls)))))

(deftest transform-failure-is-structured-and-never-builds-a-plan
  (let [source "(ns bench.transform)\n(def a {:x [1]})\n"
        query (dsl/transform [[:form 'a] [:find :x] :right]
                             (fn [_]
                               (throw (ex-info "boom" {}))))
        result (dsl/evaluate-edit source
                                  {:file "source.clj"
                                   :query query
                                   :plan-out "review.edn"})]
    (is (= :edit-transform-failed (:error-type result)))
    (is (str/includes? (:error result) "boom"))
    (is (nil? (:result-hash result)))))

(deftest sci-requires-exactly-one-expression-and-a-query-result
  (doseq [[expression reason] [["" :expected-one-form]
                               ["(form 'a) (form 'b)" :expected-one-form]
                               [":not-a-query" :query-must-be-vector]]]
    (testing (pr-str expression)
      (let [error (try
                    (dsl/compile-query expression)
                    nil
                    (catch Exception e (ex-data e)))]
        (is (= :invalid-edit-expression (:error-type error)))
        (is (= reason (:reason error)))
        (is (= expression (:expression error)))))))

(deftest sci-bounds-input-before-parsing
  (doseq [[expression reason] [[nil :expression-must-be-string]
                               [(apply str (repeat 32769 "x"))
                                :expression-too-large]]]
    (let [error (try
                  (dsl/compile-query expression)
                  nil
                  (catch Exception e (ex-data e)))]
      (is (= :invalid-edit-expression (:error-type error)))
      (is (= reason (:reason error)))
      (is (= expression (:expression error))))))

(deftest sci-treats-replacement-code-as-inert-data
  (let [path (str "/tmp/clj-surgeon-must-not-write-" (random-uuid))
        query (dsl/compile-query
                (str "(-> (form 'f) (replace '(spit \"" path "\" \"bad\")))"))]
    (is (= [[:form 'f] [:replace (list 'spit path "bad")]] query))
    (is (not (.exists (java.io.File. path))))))

(deftest native-expressions-retain-exact-literal-replacement-source
  (doseq [[label expression expected-sources]
          [["threaded replacement with nested reader shorthand"
            "(-> (form 'page) (match :config) right (replace '{:asset-url #(views/static %)}))"
            ["{:asset-url #(views/static %)}"]]
           ["direct replacement with explicit fn*"
            "(replace (match (form 'page) :handler) '(fn* [value] (handle value)))"
            ["(fn* [value] (handle value))"]]
           ["span replacement with two anonymous functions"
            "(-> (form 'page) (match 'marker) (span 2) (replace-span '#(left %) '#(right %)))"
            ["#(left %)" "#(right %)"]]
           ["a quoted replacement call is data, not the terminal builder"
            "(-> (form 'page) (replace '{:example (replace :old :new) :handler #(handle %)}))"
            ["{:example (replace :old :new) :handler #(handle %)}"]]
           ["metadata remains attached to reader shorthand"
            "(-> (form 'page) (replace '^:private #(handle %)))"
            ["^:private #(handle %)"]]
           ["a computed replacement has no literal source override"
            "(let [after (list 'handle 'value)] (-> (form 'page) (replace after)))"
            [nil]]]]
    (testing label
      (let [query (dsl/compile-query expression)]
        (is (= expected-sources
               (::lens/replacement-sources (meta query))))))))

(deftest edit-options-accept-exactly-one-authoring-surface
  (let [base {:op :edit
              :file "src/state.clj"
              :plan-out "plan.edn"}
        expression "(-> (form 'transition) (match :finish) right (replace :complete))"
        query [[:form 'transition] [:find :finish] :right [:replace :complete]]]
    (is (= (assoc base :query query)
           (dsl/prepare-edit-options (assoc base :expr expression))))
    (is (= (assoc base :query query)
           (dsl/prepare-edit-options (assoc base :query query))))
    (is (= {:operation :edit
            :file "src/state.clj"
            :error "Supply exactly one of :query and :expr"
            :error-type :edit-input-conflict
            :provided [:expr :query]
            :required-one-of [:query :expr]}
           (dsl/prepare-edit-options
             (assoc base :query query :expr expression))))
    (is (= {:operation :edit
            :file "src/state.clj"
            :error "Supply exactly one of :query and :expr"
            :error-type :missing-edit-input
            :provided []
            :required-one-of [:query :expr]}
           (dsl/prepare-edit-options base)))))

(deftest edit-options-preserve-structured-sci-refusals
  (let [expression "(spit \"/tmp/no\" \"bad\")"
        result (dsl/prepare-edit-options
                 {:op :edit
                  :file "/missing/source.clj"
                  :expr expression
                  :plan-out "plan.edn"})]
    (is (= :edit (:operation result)))
    (is (= "/missing/source.clj" (:file result)))
    (is (= :invalid-edit-expression (:error-type result)))
    (is (= :disallowed-symbol (:reason result)))
    (is (= expression (:expression result)))
    (is (some #{"(replace path form)"} (:allowed-forms result)))
    (is (re-find #"thread-first" (:remedy result)))
    (is (string? (:error result)))))
