(ns clj-surgeon.mission-phase-events-test
  {:lane :battery}
  (:require
   [clj-surgeon.file-ops :as file-ops]
   [clj-surgeon.mission-events :as observer]
   [clj-surgeon.mission-typist-executor :as executor]
   [clj-surgeon.telemetry-events :as events]
   [clojure.test :refer [deftest is]]))

(defn observe-phase [phase f]
  (if-let [entry (get (ns-publics 'clj-surgeon.mission-events) 'observe-phase!)]
    (entry phase f)
    (f)))

(def prior {:mission_id "M-17" :mission_state "ready" :mission_verb "owner_forms"
            :executor "typist" :candidate_count 1 :provider "openrouter"
            :model "openai/gpt-oss-120b" :upstream "Cerebras"})

(deftest phase-events-inherit-identity-but-never-invent-mission-state
  (let [seen (atom [])]
    (binding [observer/*context* (atom prior)]
      (with-redefs [events/record! #(swap! seen conj %)]
        (is (= {:ok false} (observe-phase "verify" (fn [] {:ok false}))))
        (is (= {:committed false} (observe-phase "commit" (fn [] {:committed false}))))))
    (is (= ["mission-verify" "mission-commit"] (mapv :kind @seen)))
    (is (= [false false] (mapv :ok @seen)))
    (is (= ["M-17" "M-17"] (mapv :mission_id @seen)))
    (is (every? #(not (contains? % :mission_state)) @seen))
    (is (every? #(and (number? (:wall_ms %)) (<= 0 (:wall_ms %))) @seen))
    (is (= ["Cerebras" "Cerebras"] (mapv :upstream @seen)))))

(deftest phase-logging-failure-cannot-affect-work-or-thrown-identity
  (let [failure (ex-info "SECRET" {}) calls (atom 0)]
    (with-redefs [events/record! (fn [_] (throw (Exception. "logger failed")))]
      (is (= {:committed true}
             (observe-phase "commit" #(do (swap! calls inc) {:committed true}))))
      (is (= 1 @calls))
      (is (identical? failure (try (observe-phase "verify" #(throw failure))
                                   (catch Throwable actual actual)))))
    (with-redefs [events/mission-fields (fn [_] (throw (Exception. "projection failed")))]
      (is (= :success (observe-phase "verify" (fn [] :success)))))))

(defn exercise [compiled proof commit stale-at-commit?]
  (let [seen (atom []) calls (atom []) unchanged (atom 0)
        original-commit executor/commit-candidate!]
    (binding [observer/*context* (atom prior)]
      (with-redefs [events/record! #(swap! seen conj %)
                    executor/unchanged? (fn [_] (or (not stale-at-commit?) (= 1 (swap! unchanged inc))))
                    executor/make-artifacts! (fn [_] "/fixture-artifacts")
                    file-ops/atomic-write! (fn [& _])
                    executor/request-candidates! (fn [_] :fake-handle)
                    executor/candidate-sequence (fn [_] [{:index 0 :usable true :content "invalid"}])
                    executor/close-candidates! (fn [& _] {:terminated? true})
                    executor/compile-candidate! (fn [& _] (swap! calls conj :compile) compiled)
                    executor/verify-candidate! (fn [& _] (swap! calls conj :verify) proof)
                    executor/commit-candidate! (fn [& args]
                                                 (swap! calls conj :commit)
                                                 (if stale-at-commit?
                                                   (apply original-commit args)
                                                   commit))]
        (let [result (executor/execute! nil {:plan {:typist {:route {:executor :typist}}}})]
          {:result result :events @seen :calls @calls})))))

(deftest parse-refusal-does-not-invent-proof-or-commit
  (let [{:keys [events calls result]} (exercise {:ok false :error-type :forms-unparseable}
                                        {:ok true} {:committed true} false)]
    (is (= [:compile] calls))
    (is (empty? events))
    (is (false? (:committed result)))))

(deftest failed-proof-emits-only-a-failed-verification
  (let [{:keys [events calls result]} (exercise {:ok true} {:ok false :gate {:ok false}}
                                        {:committed true} false)]
    (is (= [:compile :verify] calls))
    (is (= ["mission-verify"] (mapv :kind events)))
    (is (= [false] (mapv :ok events)))
    (is (false? (:committed result)))))

(deftest accepted-candidate-emits-actual-phase-order-with-one-mission-id
  (let [{:keys [events calls result]} (exercise {:ok true} {:ok true} {:committed true} false)]
    (is (= [:compile :verify :commit] calls))
    (is (= ["mission-verify" "mission-commit"] (mapv :kind events)))
    (is (= [true true] (mapv :ok events)))
    (is (= ["M-17" "M-17"] (mapv :mission_id events)))
    (is (true? (:committed result)))))

(deftest actual-stale-commit-refusal-is-a-failed-commit-event
  (let [{:keys [events calls result]} (exercise {:ok true} {:ok true} nil true)]
    (is (= [:compile :verify :commit] calls))
    (is (= [true false] (mapv :ok events)))
    (is (= "typist-stale-plan" (:error_type (last events))))
    (is (= :typist-stale-plan (:error-type result)))
    (is (false? (:committed result)))))

(deftest direct-executor-phase-has-no-invented-id
  (let [seen (atom [])]
    (binding [observer/*context* nil]
      (with-redefs [events/record! #(swap! seen conj %)]
        (observe-phase "verify" (fn [] {:ok true}))))
    (is (= 1 (count @seen)))
    (is (nil? (:mission_id (first @seen))))))
