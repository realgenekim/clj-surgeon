(ns clj-surgeon.mission-typist-executor-admission-test
  {:lane :battery}
  (:require
   [clj-surgeon.mission-cli :as cli]
   [clj-surgeon.mission-typist :as typist]
   [clj-surgeon.mission-typist-executor :as executor]
   [clj-surgeon.mission-typist-executor-test :as fixture]
   [clj-surgeon.mission-typist-test :as policy-fixture]
   [clojure.java.io :as io]
   [clojure.test :refer [deftest is]]))

(defn with-provider [facts provider]
  (-> facts (assoc :provider provider)
      (update :rate merge {:provider (:id provider) :model (:model provider) :upstream (:upstream provider)})))

(def spark {:id :spark :model "gpt-5.3-codex-spark" :upstream "OpenAI"})

(deftest spark-policy-does-not-admit-unimplemented-executor
  ;; Field failure: pure policy accepted Spark; plan reported ready; request-one!
  ;; later refused before dispatch. Keep policy experiments, move refusal to plan.
  (is (:ok (typist/route (with-provider policy-fixture/eligible spark))))
  (fixture/with-fixture
    (fn [root file]
      (let [request (update (fixture/request root) :typist with-provider spark)
            original (slurp file)
            transport-calls (atom 0)]
        (with-redefs [executor/transport-authority (fn [] (swap! transport-calls inc) {})]
          (let [planned (executor/plan request fixture/profiles)
                proposed (cli/propose! {:verb "owner_forms" :request request :profiles fixture/profiles
                                        :state-home (str (io/file root "state"))})]
            (is (= :typist-executor-provider-unavailable (:error-type planned)))
            (is (false? (:ok planned)))
            (is (not= :ready (:state proposed)))
            (is (= :typist-executor-provider-unavailable (get-in proposed [:plan :error-type])))
            (is (zero? @transport-calls))
            (is (= original (slurp file)))))))))

(deftest implemented-provider-plans-remain-admitted
  (fixture/with-fixture
    (fn [root _file]
      (doseq [provider [{:id :openrouter :model "openai/gpt-oss-120b" :upstream "Cerebras"}
                        {:id :groq :model "openai/gpt-oss-120b" :upstream "Groq"}]]
        (is (:ok (executor/plan (update (fixture/request root) :typist with-provider provider)
                                fixture/profiles)))))))
