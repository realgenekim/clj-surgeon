(ns clj-surgeon.mission-plain-forms-test
  {:lane :fast}
  (:require
   [clj-surgeon.mission-forms :as forms]
   [clj-surgeon.mission-plain-forms :as plain]
   [clojure.test :refer [deftest is testing]]))

(def original "(defn- field [x] (get x :value))")
(def basis {:sources {"src/a.clj" (str "; untouched\n" original "\n(def other 3)\n")}
            :owners [{:file "src/a.clj" :owner "field" :new-owner "finding-field"
                      :start 12 :end (+ 12 (count original))}]
            :budget {:max-files 1 :max-changed-chars 1000}})
(def replacement "(defn- finding-field [x] (get x :value))")

(deftest plain-response-lowers-to-existing-authority
  (let [expected (forms/compile-forms basis [{:file "src/a.clj" :owner "field" :form replacement}])]
    (is (:ok expected))
    (let [result (plain/compile-response basis (str "\n" replacement "\n"))]
      (is (= expected (dissoc result :replacements)))
      (is (= [{:file "src/a.clj" :owner "field" :form replacement}] (:replacements result)))))
  (is (:ok (plain/compile-response (assoc-in basis [:owners 0 :new-owner] "field") "(defn- field [x] (:value x))"))))

(deftest escaped-newline-field-loss-is-never-repaired
  ;; T4/receipts/mission-16564467186828004922/candidate-0.edn, EDN then
  ;; JSON decoded first :form: after finding-field, bytes [92 110 32 32 91].
  ;; Preserve that exact suffix; also refuse the reported two-backslash variant.
  ;; This proves decoder refusal, not identical old BB/JVM diagnostics.
  (doseq [raw ["(defn- finding-field\\n  [x] (get x :value))"
               "(defn- finding-field\\\\n  [x] (get x :value))"]]
    (let [r (plain/compile-response basis raw)]
      (is (false? (:ok r)))
      (is (false? (:mutation-attempted r)))
      (is (= :plain-unsupported-reader-syntax (:error-type r)))))
  (doseq [raw [(pr-str replacement) (str "```clojure\n" replacement "\n```")
               (str "Here is the change:\n" replacement) "#=(spit \"never\" \"run\")"]]
    (is (false? (:ok (plain/compile-response basis raw))))))

(deftest exact-coverage-and-one-file
  (doseq [[raw code] [["" :plain-empty-response]
                      ["(def extra 1)" :forms-unknown-owner]
                      [(str replacement "\n" replacement) :forms-duplicate-owner]]]
    (is (= code (:error-type (plain/compile-response basis raw)))))
  (let [two (-> basis
                (assoc-in [:sources "src/b.clj"] "(def b 1)")
                (update :owners conj {:file "src/b.clj" :owner "b" :start 0 :end 9}))]
    (is (= :plain-one-file-required (:error-type (plain/compile-response two replacement)))))
  (let [two {:sources {"a.clj" "(def a 1)\n(def b 2)"}
             :owners [{:file "a.clj" :owner "a" :start 0 :end 9}
                      {:file "a.clj" :owner "b" :start 10 :end 19}]
             :budget {:max-files 1 :max-changed-chars 1000}}]
    (is (:ok (plain/compile-response two "(def b 4)\n(def a 3)")))
    (is (= :plain-owner-coverage (:error-type (plain/compile-response two "(def a 3)")))))
  (let [ambiguous {:sources {"a.clj" "(def a 1)\n(def b 2)"}
                   :owners [{:file "a.clj" :owner "a" :new-owner "b" :start 0 :end 9}
                            {:file "a.clj" :owner "b" :start 10 :end 19}]
                   :budget {:max-files 1 :max-changed-chars 1000}}]
    (is (= :plain-ambiguous-owner (:error-type (plain/compile-response ambiguous "(def b 3)"))))))

(deftest quoted-syntax-stays-literal
  (let [source "(def message \"old\")"
        b {:sources {"a.clj" source} :owners [{:file "a.clj" :owner "message" :start 0 :end (count source)}]
           :budget {:max-files 1 :max-changed-chars 1000}}
        raw "(def message \"#= ; ^ \\\" [ ] ( ) \\n ```\")"]
    (is (= raw (get-in (plain/compile-response b raw) [:future-sources "a.clj"])))))

(deftest existing-protection-is-not-bypassed
  (doseq [raw ["(defn finding-field [x] (get x :value))"
               "(defn- finding-field \"invented doc\" [x] (get x :value))"
               "(defn- ^:private finding-field [x] (get x :value))"]]
    (is (false? (:ok (plain/compile-response basis raw)))))
  ;; A comment inside a form is no longer refused: it lives in the owner's span
  ;; and is carried through as source text. Dropping one the owner had is still
  ;; refused -- now by name, :forms-comment-lost, with the lost text.
  (let [protected "(defn- field [x] ; preserve me\n (get x :value))"
        b (-> basis (assoc-in [:sources "src/a.clj"] protected)
              (assoc-in [:owners 0 :start] 0) (assoc-in [:owners 0 :end] (count protected)))]
    (is (= :forms-comment-lost (:error-type (plain/compile-response b replacement))))
    (is (= ["; preserve me"]
           (:lost (plain/compile-response b replacement))))
    (is (:ok (plain/compile-response b "(defn- finding-field [x] ; preserve me\n (get x :value))")))
    ;; A top-level comment belongs to no owner span, so accepting it would drop
    ;; it silently. That is still protected syntax.
    (is (= :forms-protected-syntax
           (:error-type (plain/compile-response b (str ";; stray\n" replacement)))))))

(deftest preparse-bounds-and-malformed-input
  (doseq [[raw code] [[(apply str (repeat 262145 "x")) :candidate-parser-budget]
                      [(str "(def a " (apply str (repeat 65 "("))) :candidate-parser-depth]
                      [(apply str (repeat 129 "(def a 1)")) :candidate-parser-budget]
                      ["(def a [1))" :candidate-unparseable]
                      ["(def a \"unfinished)" :candidate-unparseable]
                      ["(def a 1" :candidate-unparseable]
                      [nil :plain-invalid-response]]]
    (is (= code (:error-type (plain/compile-response basis raw)))))
  (is (= :candidate-invalid-basis (:error-type (plain/compile-response {} replacement)))))

(deftest bounds-refuse-before-definition-parser
  (let [calls (atom 0)]
    (with-redefs [forms/definition (fn [_] (swap! calls inc) (throw (ex-info "must not parse" {})))]
      (doseq [raw [(apply str (repeat 131073 "é"))
                   (str "(def a [" (apply str (repeat 2048 "[]")) "])")
                   (str "(def a " (apply str (repeat 65 "(")))
                   (apply str (repeat 129 "(def a 1)"))]]
        (is (false? (:ok (plain/compile-response basis raw)))))
      (is (zero? @calls)))))

(deftest inert-dispatch-framing-for-real1
  ;; Real-1 diagnostic-delta uses #(contains? ...), so a blanket # refusal
  ;; excluded the motivating task. Framing is inert; compiler/proof own semantics.
  (doseq [[before after]
          [["(defn diagnostic-delta [xs seen] (remove (fn [x] (contains? seen x)) xs))"
            "(defn diagnostic-delta [xs seen] (remove #(contains? seen %) xs))"]
           ["(def values nil)" "(def values #{:a :b})"]
           ["(def pattern nil)" "(def pattern #\"[(){}]\\\\d+\")"]]]
    (let [name (if (.startsWith before "(defn") "diagnostic-delta"
                 (if (.startsWith before "(def values") "values" "pattern"))
          b {:sources {"a.clj" before}
             :owners [{:file "a.clj" :owner name :start 0 :end (count before)}]
             :budget {:max-files 1 :max-changed-chars 1000}}
          r (plain/compile-response b after)]
      (is (:ok r))
      (is (= after (get-in r [:future-sources "a.clj"])))))
  (doseq [dispatch ["#=(identity 1)" "#_1 2" "#?(:clj 1)" "#inst \"2020\"" "#unknown 1"]]
    (is (= :plain-unsupported-reader-syntax
           (:error-type (plain/compile-response basis (str "(defn- finding-field [x] " dispatch ")")))))))
