(ns clj-surgeon.forward-refs-test
  (:require
   [babashka.fs :as fs]
   [clj-surgeon.forward-refs :as fwd]
   [clojure.test :refer [deftest is testing]]))

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
