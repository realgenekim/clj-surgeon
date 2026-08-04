(ns clj-surgeon.edit-dsl-test
  (:require
   [clj-surgeon.edit-dsl :as dsl]
   [clojure.test :refer [deftest is testing]]))

(deftest native-expression-compiles-to-the-existing-case-query
  (is (= [[:form 'transition]
          [:find :finish]
          :right
          [:replace '(assoc state :status :complete)]]
         (-> (dsl/form 'transition)
             (dsl/match :finish)
             dsl/right
             (dsl/replace '(assoc state :status :complete))))))

(def builder-cases
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
   ["replace span" #(dsl/replace-span % :a :b) [:replace-span :a :b]]])

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
             (dsl/replace-span :finish '(inc x))))))

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
                    [[:replace-span :a :b]]]
          [label build _] builder-cases]
    (testing (str label " after " (pr-str terminal))
      (let [error (try
                    (build terminal)
                    nil
                    (catch Exception e (ex-data e)))]
        (is (= {:error-type :terminal-edit-step
                :terminal-step (last terminal)}
               error))))))

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

(deftest sci-exposes-every-builder-and-no-unrelated-function
  (is (= [[:form 'f]
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
          "(-> (form 'f) (match :anchor) (where {:parent-tag :vector}) right left up down (span 2) (partition-all 2) (replace-span :a :b))")))
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
    (is (= :unsupported-form (:reason result)))
    (is (= expression (:expression result)))
    (is (some #{"(replace path form)"} (:allowed-forms result)))
    (is (re-find #"thread-first" (:remedy result)))
    (is (string? (:error result)))))
