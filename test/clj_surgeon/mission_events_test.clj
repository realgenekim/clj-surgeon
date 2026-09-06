(ns clj-surgeon.mission-events-test
  {:lane :battery}
  (:require
   [cheshire.core :as json]
   [clj-surgeon.mission :as mission]
   [clj-surgeon.mission-cli :as cli]
   [clj-surgeon.mission-events :as observer]
   [clj-surgeon.telemetry-events :as events]
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.java.shell :as shell]
   [clojure.string :as str]
   [clojure.test :refer [deftest is]]))

(def saved-mission
  {:id "M-1" :verb "owner_forms" :state :ready
   :intent "SECRET-RAW-INTENT"
   :plan {:typist {:route {:executor :typist :k 3
                           :provider {:id :openrouter :model "openai/gpt-oss-120b"
                                      :upstream "Cerebras" :key "SECRET-KEY"}}}}})

(deftest route-fields-survive-real-rendering-without-source-or-secrets
  (let [line (-> (observer/event "propose" {} saved-mission 1.5 false)
                 events/line-map events/render-line (json/parse-string true))]
    (is (= "mission-plan" (:kind line)))
    (is (= "M-1" (:mission_id line)))
    (is (= "ready" (:mission_state line)))
    (is (= "typist" (:executor line)))
    (is (= 3 (:candidate_count line)))
    (is (= "Cerebras" (:upstream line)))
    (is (not (str/includes? (pr-str line) "SECRET")))
    (is (< (count (events/render-line line)) events/line-limit))))

(deftest shared-mission-extension-refuses-unbounded-and-unknown-values
  (doseq [[key values] {:mission_state ["SECRET" [] :ready]
                        :executor ["SECRET" {} :typist]
                        :candidate_count [0 6 1.2 "3" nil]
                        :model ["SECRET" "not-admitted"]
                        :provider ["SECRET" "unknown"]
                        :upstream ["SECRET" "unknown"]
                        :refused_rung ["SECRET" "unknown"]}]
    (doseq [value values]
      (is (nil? (get (events/line-map {key value}) key)))))
  (is (empty? (observer/context {:id "SECRET" :verb "SECRET"
                                 :state "SECRET" :plan "SECRET"}))))

(deftest outcome-and-refusal-rung-are-projected-not-invented-stages
  (let [prior (observer/context saved-mission)
        result {:state :blocked :id "M-1"
                :decision {:error_type "typist-route-refused"
                           :evidence {:condition :cheap-gate :source "SECRET"}}}
        line (observer/event "propose" prior result 15 false)]
    (is (false? (:ok line)))
    (is (= "typist-route-refused" (:error_type line)))
    (is (= "cheap-gate" (:refused_rung line)))
    (is (= "mission-plan" (:kind line)))
    (is (= "blocked" (:mission_state line)))
    (is (= "typist" (:executor line))))
  (is (= "mission-refused"
         (:error_type (observer/event "apply" {} {:ok false :error-type "SECRET"} 0 false)))))

(deftest logging-failure-cannot-change-result-or-exception
  (let [calls (atom 0) expected {:state :verified}
        failure (ex-info "SECRET" {})]
    (with-redefs [events/record! (fn [_] (throw (ex-info "broken log" {})))]
      (is (identical? expected (observer/observe! "apply" saved-mission
                                 #(do (swap! calls inc) expected))))
      (is (= 1 @calls))
      (is (identical? failure (try (observer/observe! "undo" saved-mission
                                                      #(throw failure))
                                   (catch Throwable actual actual))))))
  (with-redefs [events/mission-fields (fn [_] (throw (Exception. "broken projection")))]
    (is (= :successful (observer/observe! "apply" saved-mission
                         #(do (observer/remember! saved-mission) :successful)))))
  (let [seen (atom [])]
    (with-redefs [events/record! #(swap! seen conj %)]
      (try (observer/observe! "apply" saved-mission #(throw (Exception. "SECRET")))
           (catch Exception _ nil)))
    (is (= 1 (count @seen)))
    (is (= "mission-exception" (:error_type (first @seen))))
    (is (not (str/includes? (pr-str @seen) "SECRET")))))

(deftest public-boundaries-emit-function-events-on-refusal
  (let [seen (atom [])]
    (with-redefs-fn {#'events/record! #(swap! seen conj %)
                     #'cli/state-dir-for (fn [& _] "/ledger")
                     #'mission/read-mission (fn [& _] (mission/refusal "not-found" "missing"))}
      #(do (cli/propose! {:verb "unsupported-secret"})
           (cli/apply! {:id "M-2" :workspace "/fixture"})
           (cli/undo! {:id "M-3" :workspace "/fixture"})))
    (is (= ["mission-plan" "mission-apply" "mission-undo"] (mapv :kind @seen)))
    (is (= [nil "M-2" "M-3"] (mapv :mission_id @seen)))
    (is (every? (comp false? :ok) @seen))
    (is (every? #(and (number? (:wall_ms %)) (<= 0 (:wall_ms %))) @seen))
    (is (not (str/includes? (pr-str @seen) "secret")))))

(deftest application-event-binds-saved-plan-to-terminal-receipt
  (let [stored (atom (assoc saved-mission :intent {:workspace_root "/fixture"}))
        seen (atom [])]
    (with-redefs-fn {#'events/record! #(swap! seen conj %)
                     #'cli/state-dir-for (fn [& _] "/ledger")
                     #'cli/stale? (fn [_] nil)
                     #'cli/admitted-profiles (fn [& _] {})
                     #'mission/read-mission (fn [& _] @stored)
                     #'mission/read-all (fn [_] [])
                     #'cli/save! (fn [_ m] (reset! stored m))
                     #'cli/verbs {"owner_forms" {:execute! (fn [_ _]
                                                             {:committed true
                                                              :receipt_hash "sha"
                                                              :undo_receipt "undo.edn"})}}}
      #(let [result (cli/apply! {:id "M-1" :workspace "/fixture" :receipt-dir "/fixture/receipts"})]
         (is (= :verified (:state result)))
         (is (= (:id result) (:mission_id (first @seen))))
         (is (= (name (:state result)) (:mission_state (first @seen))))))
    (is (= 1 (count @seen)))
    (is (= "mission-apply" (:kind (first @seen))))
    (is (= "Cerebras" (:upstream (first @seen))))))

(deftest real-cli-refusal-writes-jsonl-and-preserves-source
  (let [root (.toFile (java.nio.file.Files/createTempDirectory
                        "mission-events-" (make-array java.nio.file.attribute.FileAttribute 0)))
        source (io/file root "fixture.clj")
        log (io/file root "events.jsonl")
        spec (io/file root "spec.edn")
        before "(defn finding-identity [finding] (:message finding))\n"]
    (try
      (spit source before)
      (spit spec (pr-str {:verb "owner_forms" :request {:workspace_root (str root)}}))
      (let [result (shell/sh "bin/mission" "run" "--spec-file" (str spec)
                             "--state-home" (str (io/file root "state"))
                             :env (assoc (into {} (System/getenv))
                                         "CLJ_SURGEON_EVENTS_FILE" (str log)))
            receipt (edn/read-string (:out result))
            lines (if (.exists log) (mapv #(json/parse-string % true)
                                          (str/split-lines (slurp log))) [])]
        (is (= 1 (:exit result)))
        (is (= before (slurp source)))
        (is (= 1 (count lines)))
        (is (= "mission-plan" (:kind (first lines))))
        (is (= (:id receipt) (:mission_id (first lines))))
        (is (false? (:ok (first lines)))))
      (finally (doseq [p (reverse (file-seq root))] (io/delete-file p true))))))

(deftest observed-stale-and-destination-refusals-retain-closed-event-types
  (doseq [reason ["mission-snapshot-stale" "typist-receipt-dir-required"]]
    (let [seen (atom [])]
      (with-redefs [events/record! #(swap! seen conj %)]
        (observer/observe! "apply" {:id "M-1"}
          (fn [] {:ok false :error_type reason})))
      (is (= reason (:error_type (first @seen))))
      (is (false? (:ok (first @seen)))))))
