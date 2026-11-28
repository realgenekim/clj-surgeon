(ns clj-surgeon.forward-refs-test
  "Regression tests for issue #23: read ops failed on any file clj-kondo had an
  opinion about, and resolved its configuration from the launch directory."
  (:require
   [babashka.fs :as fs]
   [cheshire.core :as json]
   [clj-surgeon.forward-refs :as fwd]
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
           (fwd/analysis-from-result
             (kondo-result 0 (json/generate-string empty-analysis)))))))

(deftest analysis-from-result-accepts-warnings
  (testing "exit 2 means warnings, not analysis failure"
    (is (= empty-analysis
           (fwd/analysis-from-result
             (kondo-result 2 (json/generate-string empty-analysis)))))))

(deftest analysis-from-result-accepts-errors
  (testing "exit 3 means lint errors, and the analysis is still usable"
    (is (= empty-analysis
           (fwd/analysis-from-result
             (kondo-result 3 (json/generate-string empty-analysis)))))))

(deftest analysis-from-result-refuses-unfinished-run
  (testing "a timed-out analyzer is a real failure carrying its diagnostic"
    (let [data (try
                 (fwd/analysis-from-result
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
               (fwd/analysis-from-result
                 (assoc (kondo-result 0 (json/generate-string empty-analysis))
                        :admission {:status :refused}))
               (catch Exception error (ex-data error))))))))

(deftest analysis-from-result-refuses-unparseable-output
  (testing "invalid JSON reports the exit code and stderr rather than nothing"
    (let [data (try
                 (fwd/analysis-from-result
                   (assoc (kondo-result 3 "{not json") :err "bad argument"))
                 (catch Exception error (ex-data error)))]
      (is (= :forward-reference-analysis-invalid (:error-type data)))
      (is (= 3 (:exit data)))
      (is (= "bad argument" (:diagnostic data))))))

(deftest analysis-from-result-refuses-output-without-analysis
  (testing "an empty stdout parses to nil and must not read as zero forward refs"
    (let [data (try
                 (fwd/analysis-from-result
                   (assoc (kondo-result 3 "") :err "unrecognised option"))
                 (catch Exception error (ex-data error)))]
      (is (= :forward-reference-analysis-invalid (:error-type data)))
      (is (= 3 (:exit data)))
      (is (= "unrecognised option" (:diagnostic data))))))

;; ============================================================
;; project-root-for — defect 2: config resolved from the file
;; ============================================================

(deftest project-root-for-finds-nearest-root
  (testing "the inner project wins when a repository nests several"
    (is (= "/repo/app"
           (fwd/project-root-for
             "/repo/app/src/a/b.clj"
             #{"/repo/.clj-kondo" "/repo/app/deps.edn"})))))

(deftest project-root-for-accepts-any-marker
  (testing "a clj-kondo directory alone is enough"
    (is (= "/repo/app"
           (fwd/project-root-for
             "/repo/app/src/a/b.clj"
             #{"/repo/app/.clj-kondo"}))))
  (testing "so is a leiningen or babashka build file"
    (is (= "/repo"
           (fwd/project-root-for "/repo/src/a/b.clj" #{"/repo/project.clj"})))
    (is (= "/repo"
           (fwd/project-root-for "/repo/src/a/b.clj" #{"/repo/bb.edn"})))))

(deftest project-root-for-without-a-root
  (testing "no ancestor qualifies, so the caller keeps its own default"
    (is (nil? (fwd/project-root-for "/repo/src/a/b.clj" #{})))))

(deftest admit-complete-analysis-test
  (doseq [result [{:finished? true :exit 0 :out "analysis"}
                  {:finished? true :exit 2 :out "analysis"}
                  {:finished? true :exit 3 :out "analysis"}]]
    (testing (str "complete analysis with exit " (:exit result))
      (is (identical? result (fwd/admit-analysis-result result))))))

(deftest refuse-failed-analysis-test
  (doseq [result [{:finished? true :exit 1}
                  {:finished? true :exit 4}
                  {:finished? true :exit 127}
                  {:finished? false :exit 0}
                  {:finished? false :exit 2}
                  {:finished? false :exit 3}]]
    (testing (str result)
      (let [error (try (fwd/admit-analysis-result result)
                       (catch clojure.lang.ExceptionInfo error error))]
        (is (= "Forward-reference analysis failed" (ex-message error)))
        (is (= {:error-type :forward-reference-analysis-failed
                :exit (:exit result)
                :diagnostic ""}
               (ex-data error)))))))

(deftest refusal-diagnostic-test
  (doseq [[result diagnostic]
          [[{:finished? true :exit 1 :err " \n analyzer failed\t "}
            "analyzer failed"]
           [{:finished? true :exit 1 :err nil} ""]]]
    (let [error (try (fwd/admit-analysis-result result)
                     (catch clojure.lang.ExceptionInfo error error))]
      (is (= diagnostic (:diagnostic (ex-data error)))))))

(def ns-line
  "(ns ^{:clj-kondo/config '{:linters {:namespace-name-mismatch {:level :off}}}} forward-warning)\n")

(defn- forward-refs-with-exit
  "Runs the real analyzer on SOURCE in the system temp dir; returns [refs kondo-exit]."
  [source]
  (let [file (fs/create-temp-file {:prefix "forward-warning-" :suffix ".clj"})
        admit fwd/admit-analysis-result
        exit-code (atom nil)]
    (try
      (spit (str file) source)
      (with-redefs [fwd/admit-analysis-result
                    (fn [result]
                      (reset! exit-code (:exit result))
                      (admit result))]
        [(fwd/detect-forward-refs (str file) 'forward-warning) @exit-code])
      (finally
        (fs/delete file)))))

(deftest anvil-lint-finding-forward-reference-regression-test
  ;; 2026-11-22, Anvil: 9 of 10 real files refused :ls, including
  ;; curtaincall-cfp src/cfp_scheduler_killer/views/schedule.clj (exit 2) and
  ;; marvin-voice-remote src/marvin_voice_remote/reducer/core.clj (exit 3).
  (if-not (fs/which "clj-kondo")
    (is true "clj-kondo absent; real-analyzer regression skipped")
    (do
      (testing "exit 2: a lint warning (unused binding) beside a declared forward use"
        (is (= [[{:name 'later :used-at 3 :defined-at 4 :gap 1}] 2]
               (forward-refs-with-exit
                 (str ns-line
                      "(declare later)\n"
                      "(defn earlier [] (let [unused 1] (later)))\n"
                      "(defn later [] :ok)\n")))))
      (testing "exit 3: an undeclared forward use is a kondo error, the case :fix-declares! repairs"
        (is (= [[{:name 'later :used-at 2 :defined-at 3 :gap 1}] 3]
               (forward-refs-with-exit
                 (str ns-line
                      "(defn earlier [] (later))\n"
                      "(defn later [] :ok)\n"))))))))
