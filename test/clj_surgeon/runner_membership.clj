(ns clj-surgeon.runner-membership
  "TEST-ISO-001 -- resolving a named runner to the namespaces it actually runs.

   RED, ROUND FIVE. This file currently holds the round-three implementation
   VERBATIM -- the predicate the landing review's finding 4 broke -- extracted
   from `lane-manifest-test/every-exclusion-names-a-runner-that-actually-exists`
   so the defect it carries is reproducible as a test before it is fixed.

   The defect: it asks `does a target with this name exist?`. A target that
   merely exists is a SPELLING, not a runner. The reviewer's sabotage put the
   false reason \"`make test-fast`\" on `clj-surgeon.analyzer-contract-test`
   -- a target that exists and does not run it -- and the witness passed."
  (:require
   [clojure.string :as str]))

(defn resolve-runner
  "RED: existence only. Answers `#{}` for every runner, because existence
   cannot say what a runner RUNS."
  [_runner _ctx]
  {:namespaces #{} :unresolved []})

(defn runners-named-in
  [reason]
  (concat (map #(str "make " (second %)) (re-seq #"`make ([a-z0-9\-]+)`" reason))
          (map #(str ":clj-surgeon/" (second %)) (re-seq #":clj-surgeon/([a-z0-9\-]+)" reason))))

(defn exclusion-violations
  "RED: the round-three predicate. A runner that EXISTS is accepted."
  [excluded {:keys [makefile-text deps-text]}]
  (let [target? (fn [t] (or (re-find (re-pattern (str "(?m)^" (java.util.regex.Pattern/quote t) ":")) makefile-text)
                            (str/includes? makefile-text (str " " t " "))))
        alias? (fn [a] (str/includes? deps-text (str ":clj-surgeon/" a)))]
    (vec
     (for [[s reason] (sort-by key excluded)
           :let [targets (map second (re-seq #"`make ([a-z0-9\-]+)`" reason))
                 aliases (map second (re-seq #":clj-surgeon/([a-z0-9\-]+)" reason))
                 named (concat (filter target? targets) (filter alias? aliases))]
           :when (empty? named)]
       {:namespace s :kind :no-runner-named :reason reason
        :message (str "excluded namespace " s " names no runner that exists.")}))))

(defn repo-context
  []
  {:makefile-text (slurp (clojure.java.io/file "Makefile"))
   :deps-text (slurp (clojure.java.io/file "deps.edn"))})
