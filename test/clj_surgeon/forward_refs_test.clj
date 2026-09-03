(ns clj-surgeon.forward-refs-test
  "Regression tests for issue #23: read ops failed on any file clj-kondo had an
  opinion about, and resolved its configuration from the launch directory."
  (:require
   [cheshire.core :as json]
   [clj-surgeon.forward-refs :as forward-refs]
   [clojure.test :refer [deftest is testing]]))

(def ^:private empty-analysis
  {:findings []
   :summary {:error 0 :warning 0 :info 0 :files 1}
   :analysis {:var-definitions [] :var-usages []}})

(defn- kondo-result
  [exit analysis-json]
  {:finished? true
   :exit exit
   :admission {:status :admitted}
   :out analysis-json
   :err ""})

;; ============================================================
;; analysis-from-result — defect 1: exit code is not a verdict
;; ============================================================

(deftest analysis-from-result-accepts-clean-exit
  (testing "exit 0 returns the parsed analysis"
    (is (= empty-analysis
           (forward-refs/analysis-from-result
             (kondo-result 0 (json/generate-string empty-analysis)))))))

(deftest analysis-from-result-accepts-warnings
  (testing "exit 2 means warnings, not analysis failure"
    (is (= empty-analysis
           (forward-refs/analysis-from-result
             (kondo-result 2 (json/generate-string empty-analysis)))))))

(deftest analysis-from-result-accepts-errors
  (testing "exit 3 means lint errors, and the analysis is still usable"
    (is (= empty-analysis
           (forward-refs/analysis-from-result
             (kondo-result 3 (json/generate-string empty-analysis)))))))

(deftest analysis-from-result-refuses-unfinished-run
  (testing "a timed-out analyzer is a real failure carrying its diagnostic"
    (let [data (try
                 (forward-refs/analysis-from-result
                   {:finished? false
                    :exit nil
                    :admission {:status :admitted}
                    :out ""
                    :err "  killed  "})
                 (catch Exception error (ex-data error)))]
      (is (= :forward-reference-analysis-failed (:error-type data)))
      (is (= "killed" (:diagnostic data))))))

(deftest analysis-from-result-refuses-unverified-admission
  (testing "admission is still enforced"
    (is (= :analyzer-authority-unverified
           (:error-type
             (try
               (forward-refs/analysis-from-result
                 (assoc (kondo-result 0 (json/generate-string empty-analysis))
                        :admission {:status :refused}))
               (catch Exception error (ex-data error))))))))

(deftest analysis-from-result-refuses-unparseable-output
  (testing "invalid JSON reports the exit code and stderr rather than nothing"
    (let [data (try
                 (forward-refs/analysis-from-result
                   (assoc (kondo-result 1 "{not json") :err "bad argument"))
                 (catch Exception error (ex-data error)))]
      (is (= :forward-reference-analysis-invalid (:error-type data)))
      (is (= 1 (:exit data)))
      (is (= "bad argument" (:diagnostic data))))))

(deftest analysis-from-result-refuses-output-without-analysis
  (testing "an empty stdout parses to nil and must not read as zero forward refs"
    (let [data (try
                 (forward-refs/analysis-from-result
                   (assoc (kondo-result 1 "") :err "unrecognised option"))
                 (catch Exception error (ex-data error)))]
      (is (= :forward-reference-analysis-invalid (:error-type data)))
      (is (= 1 (:exit data)))
      (is (= "unrecognised option" (:diagnostic data))))))

;; ============================================================
;; project-root-for — defect 2: config resolved from the file
;; ============================================================

(deftest project-root-for-finds-nearest-root
  (testing "the inner project wins when a repository nests several"
    (is (= "/repo/app"
           (forward-refs/project-root-for
             "/repo/app/src/a/b.clj"
             #{"/repo/.clj-kondo" "/repo/app/deps.edn"})))))

(deftest project-root-for-accepts-any-marker
  (testing "a clj-kondo directory alone is enough"
    (is (= "/repo/app"
           (forward-refs/project-root-for
             "/repo/app/src/a/b.clj"
             #{"/repo/app/.clj-kondo"}))))
  (testing "so is a leiningen or babashka build file"
    (is (= "/repo"
           (forward-refs/project-root-for "/repo/src/a/b.clj" #{"/repo/project.clj"})))
    (is (= "/repo"
           (forward-refs/project-root-for "/repo/src/a/b.clj" #{"/repo/bb.edn"})))))

(deftest project-root-for-without-a-root
  (testing "no ancestor qualifies, so the caller keeps its own default"
    (is (nil? (forward-refs/project-root-for "/repo/src/a/b.clj" #{})))))
