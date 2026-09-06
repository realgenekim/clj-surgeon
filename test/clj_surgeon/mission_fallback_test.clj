(ns clj-surgeon.mission-fallback-test
  {:lane :battery}
  (:require
   [clj-surgeon.mission :as mission]
   [clj-surgeon.mission-cli :as cli]
   [clj-surgeon.telemetry-events :as events]
   [clojure.data.json :as json]
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.java.shell :as shell]
   [clojure.test :refer [deftest is]]))

(defn report [opts]
  (if-let [f (ns-resolve 'clj-surgeon.mission-cli 'fallback!)]
    (f opts)
    {:ok false :error-type :not-implemented}))

(defn with-ledger [f]
  (let [root (.toFile (java.nio.file.Files/createTempDirectory "mission-fallback-" (make-array java.nio.file.attribute.FileAttribute 0)))
        home (str (io/file root "home"))
        opts {:workspace (str root) :state-home home :id "M-1" :reason "refusal"}
        state-dir (mission/workspace-state-dir (str root) home)]
    (try
      (spit (io/file root "source.clj") "(def unchanged 1)\n")
      (mission/write-mission! state-dir {:id "M-1" :root (str root) :state :failed :verb "owner_forms"
                                         :receipt {:committed false :verification-complete false}})
      (f root opts)
      (finally (doseq [p (reverse (file-seq root))] (io/delete-file p true))))))

(defn bytes-under [root]
  (into {} (for [p (file-seq root) :when (.isFile p)] [(str p) (slurp p)])))

(deftest explicit-report-never-changes-proof-or-source
  (with-ledger
    (fn [root opts]
      (let [before (bytes-under root) calls (atom [])]
        (with-redefs [events/record! (fn [event] (swap! calls conj event) (events/line-map event))]
          (doseq [reason ["refusal" "unsupported" "slower-than-native" "user-choice"]]
            (let [r (report (assoc opts :reason reason))]
              (is (true? (:ok r)))
              (is (true? (:recorded r)))
              (is (= "user-reported" (get-in r [:event :report_basis])))
              (is (= "native-tool" (get-in r [:event :fallback_kind])))
              (is (= "failed" (get-in r [:event :mission_state])))
              (is (= reason (get-in r [:event :fallback_reason])))
              (is (false? (:native_edit_verified r))))))
        (is (= 4 (count @calls)))
        (is (= before (bytes-under root)))))))

(deftest append-failure-is-not-a-successful-report
  (with-ledger
    (fn [root opts]
      (let [before (bytes-under root)]
        (doseq [writer [(constantly nil) (fn [_] (throw (ex-info "sensitive" {})))]]
          (with-redefs [events/record! writer]
            (let [r (report opts)]
              (is (false? (:ok r)))
              (is (false? (:recorded r)))
              (is (= :mission-fallback-record-failed (:error-type r)))
              (is (not (.contains (pr-str r) "sensitive"))))))
        (is (= before (bytes-under root)))))))

(deftest invalid-reports-append-nothing
  (with-ledger
    (fn [root opts]
      (let [before (bytes-under root) calls (atom 0)]
        (with-redefs [events/record! (fn [_] (swap! calls inc))]
          (doseq [bad [(assoc opts :reason "raw arbitrary text") (dissoc opts :reason)
                       (assoc opts :id "M-999") (dissoc opts :workspace) (dissoc opts :id)]]
            (is (false? (:ok (report bad))))))
        (is (zero? @calls))
        (is (= before (bytes-under root)))))))

(deftest fallback-schema-is-closed
  (let [good {:fallback_kind "native-tool" :fallback_reason "refusal" :report_basis "user-reported" :mission_verb "fallback"}]
    (is (= good (select-keys (events/line-map good) (keys good))))
    (doseq [k (keys good) v ["arbitrary" {:raw "secret"} ["native-tool"]]]
      (is (not (contains? (events/line-map {k v}) k))))))

(deftest actual-cli-report-has-one-isolated-event-and-no-mission-write
  (with-ledger
    (fn [root opts]
      (let [before (bytes-under root)
            ledger (str (io/file root "events.jsonl"))
            env (assoc (into {} (System/getenv)) "CLJ_SURGEON_EVENTS_FILE" ledger)
            argv ["bin/mission" "fallback" "M-1" "--workspace" (:workspace opts)
                  "--state-home" (:state-home opts) "--reason" "refusal"]
            result (apply shell/sh (concat argv [:env env]))]
        (is (= 0 (:exit result)) (:err result))
        (when (= 0 (:exit result))
          (let [r (edn/read-string (:out result))
                lines (clojure.string/split-lines (slurp ledger))
                event (json/read-str (first lines) :key-fn keyword)]
            (is (= 1 (count lines)))
            (is (= (:event r) event))
            (is (= "mission-fallback" (:kind event)))
            (is (= "M-1" (:mission_id event)))
            (is (= "fallback" (:mission_verb event)))
            (is (true? (:recorded r)))
            (is (false? (:native_edit_verified r)))))
        (let [failed (apply shell/sh (concat argv [:env (assoc env "CLJ_SURGEON_EVENTS_FILE" (str root))]))]
          (is (= 1 (:exit failed)))
          (let [r (edn/read-string (:out failed))]
            (is (false? (:recorded r)))
            (is (= :mission-fallback-record-failed (:error-type r)))))
        (is (= before (dissoc (bytes-under root) ledger)))
        (let [bad (apply shell/sh (concat (assoc argv (dec (count argv)) "invalid") [:env env]))]
          (is (= 1 (:exit bad)))
          (when (.exists (io/file ledger))
            (is (= 1 (count (clojure.string/split-lines (slurp ledger)))))))
        (is (.contains (:out (shell/sh "bin/mission" "help" "fallback" :env env)) "user-reported"))))))
