#!/usr/bin/env bb

(ns score-source-fidelity
  (:require
   [clojure.test :refer [deftest is run-tests]]
   [rewrite-clj.node :as node]
   [rewrite-clj.parser :as parser]))

(def presentation-tags
  #{:comma :newline :whitespace})

(def score-schema
  "clj-surgeon.source-fidelity-score/v1")

(defn meaning-shape
  "Return lossless concrete syntax with only ordinary presentation removed.
  Internal node tags retain reader macros such as metadata and #_."
  [form-node]
  (when-not (presentation-tags (node/tag form-node))
    [(node/tag form-node)
     (if (node/inner? form-node)
       (->> (node/children form-node)
            (keep meaning-shape)
            vec)
       (node/string form-node))]))

(defn parse-source
  [source]
  (try
    {:ok true :node (parser/parse-string-all source)}
    (catch Exception _
      {:ok false})))

(defn score-source
  [expected candidate]
  (let [expected-parse (parse-source expected)
        candidate-parse (parse-source candidate)
        parseable (boolean (and (:ok expected-parse) (:ok candidate-parse)))
        exact (= expected candidate)
        semantic-equal
        (boolean
          (and parseable
               (= (node/sexpr (:node expected-parse))
                  (node/sexpr (:node candidate-parse)))))
        meaning-preserved
        (boolean
          (and parseable
               (= (meaning-shape (:node expected-parse))
                  (meaning-shape (:node candidate-parse)))))
        correct (boolean (and semantic-equal meaning-preserved))]
    {:parseable parseable
     :semantic-equal semantic-equal
     :meaning-preserved meaning-preserved
     :exact exact
     :presentation-only (boolean (and correct (not exact)))
     :correct correct}))

(deftest source-fidelity-contract
  (is (= {:parseable true
          :semantic-equal true
          :meaning-preserved true
          :exact true
          :presentation-only false
          :correct true}
         (score-source "(def x 1)\n" "(def x 1)\n")))
  (is (= {:parseable true
          :semantic-equal true
          :meaning-preserved true
          :exact false
          :presentation-only true
          :correct true}
         (score-source "(def x [1 2])\n"
                       "(def  x [1,  2])\n")))
  (doseq [[label expected candidate]
          [[:comment-loss "(def x 1) ; keep\n" "(def x 1)\n"]
           [:comment-alteration "(def x 1) ; keep\n" "(def x 1) ; changed\n"]
           [:reader-discard-loss "#_(:keep)\n(def x 1)\n" "(def x 1)\n"]
           [:metadata-loss "^:keep (def x 1)\n" "(def x 1)\n"]
           [:string-change "(def x \"a b\")\n" "(def x \"ab\")\n"]
           [:regex-change "(def x #\"a b\")\n" "(def x #\"ab\")\n"]]]
    (is (false? (:correct (score-source expected candidate))) label))
  (is (= {:parseable false
          :semantic-equal false
          :meaning-preserved false
          :exact false
          :presentation-only false
          :correct false}
         (score-source "(def x 1)\n" "(def x"))))

(defn -main
  [& args]
  (cond
    (= ["--self-test"] args)
    (let [{:keys [fail error]} (run-tests 'score-source-fidelity)]
      (when (pos? (+ fail error))
        (System/exit 1)))

    (= 2 (count args))
    (let [[expected-path candidate-path] args
          score (score-source (slurp expected-path) (slurp candidate-path))]
      (prn score)
      (when-not (:correct score)
        (System/exit 1)))

    (and (= 4 (count args)) (= "--target" (first args)))
    (let [[_ target expected-path candidate-path] args
          score (assoc (score-source (slurp expected-path)
                                     (slurp candidate-path))
                       :schema score-schema
                       :target target)]
      (prn score)
      (when-not (:correct score)
        (System/exit 1)))

    :else
    (throw
      (ex-info
        (str "Usage: score_source_fidelity.clj --self-test | "
             "EXPECTED CANDIDATE | --target TARGET EXPECTED CANDIDATE")
        {}))))

(apply -main *command-line-args*)
