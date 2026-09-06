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
   [clojure.string :as str]
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
                lines (str/split-lines (slurp ledger))
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
            (is (= 1 (count (str/split-lines (slurp ledger)))))))
        (is (.contains (:out (shell/sh "bin/mission" "help" "fallback" :env env)) "user-reported"))))))

(defn stable-report [report]
  (update report :event #(dissoc % :ts :pid :wall_ms)))

(deftest fallback-bb-and-jvm-share-the-event-contract
  (with-ledger
    (fn [root opts]
      (let [bb-file (str (io/file root "bb-events" "events.jsonl"))
            jvm-file (str (io/file root "jvm-events" "events.jsonl"))
            env (assoc (into {} (System/getenv)) "CLJ_SURGEON_EVENTS_FILE" bb-file)
            before (bytes-under root)
            bb-result (shell/sh "bb" "--classpath" "src" "bin/mission-read.clj"
                                "fallback" "M-1" "--workspace" (:workspace opts)
                                "--state-home" (:state-home opts) "--reason" "refusal" :env env)
            jvm-result (with-redefs [events/default-events-file (constantly jvm-file)] (cli/fallback! opts))]
        (is (= 0 (:exit bb-result)) (:err bb-result))
        (when (= 0 (:exit bb-result))
          (is (= (stable-report jvm-result) (stable-report (edn/read-string (:out bb-result))))))
        (is (= before (apply dissoc (bytes-under root) [bb-file jvm-file])))))))

(deftest public-fallback-starts-bb-and-never-clojure
  (with-ledger
    (fn [root opts]
      (let [bin (io/file root "bin")
            marker (str (io/file root "runtime-witness"))
            ledger-dir (io/file root "private-events")
            ledger (str (io/file ledger-dir "events.jsonl"))
            real-bb (str/trim (:out (shell/sh "bash" "-c" "command -v bb")))
            env (assoc (into {} (System/getenv)) "PATH" (str bin ":" (System/getenv "PATH"))
                       "MISSION_REAL_BB" real-bb "MISSION_BB_WITNESS" marker
                       "CLJ_SURGEON_EVENTS_FILE" ledger)]
        (.mkdirs bin)
        (.mkdirs ledger-dir)
        (java.nio.file.Files/setPosixFilePermissions (.toPath ledger-dir)
          (java.nio.file.attribute.PosixFilePermissions/fromString "rwxr-xr-x"))
        (doseq [[name script] [["bb" "#!/bin/sh\nprintf bb > \"$MISSION_BB_WITNESS\"\nexec \"$MISSION_REAL_BB\" \"$@\"\n"]
                               ["clojure" "#!/bin/sh\nprintf forbidden-jvm > \"$MISSION_BB_WITNESS\"\nexit 91\n"]]]
          (let [file (io/file bin name)] (spit file script) (.setExecutable file true)))
        (let [r (shell/sh "bin/mission" "fallback" "M-1" "--workspace" (:workspace opts)
                          "--state-home" (:state-home opts) "--reason" "refusal" :env env)]
          (is (= 0 (:exit r)) (:err r))
          (is (= "bb" (slurp marker)))
          (when (= 0 (:exit r))
            (is (true? (:recorded (edn/read-string (:out r)))))
            (is (= "rwx------" (java.nio.file.attribute.PosixFilePermissions/toString
                                 (java.nio.file.Files/getPosixFilePermissions (.toPath ledger-dir)
                                   (into-array java.nio.file.LinkOption [])))))
            (is (= 1 (count (str/split-lines (slurp ledger)))))))))))

(deftest bb-fallback-never-adds-owner-write-permission
  (with-ledger
    (fn [root opts]
      (let [dir (io/file root "read-only-events")
            env (assoc (into {} (System/getenv)) "CLJ_SURGEON_EVENTS_FILE"
                       (str (io/file dir "new-events.jsonl")))]
        (.mkdirs dir)
        (try
          (java.nio.file.Files/setPosixFilePermissions (.toPath dir)
            (java.nio.file.attribute.PosixFilePermissions/fromString "r-xr-xr-x"))
          (let [r (shell/sh "bin/mission" "fallback" "M-1" "--workspace" (:workspace opts)
                            "--state-home" (:state-home opts) "--reason" "refusal" :env env)]
            (is (= 1 (:exit r)))
            (is (false? (:recorded (edn/read-string (:out r)))))
            (is (= "r-x------" (java.nio.file.attribute.PosixFilePermissions/toString
                                 (java.nio.file.Files/getPosixFilePermissions (.toPath dir)
                                   (into-array java.nio.file.LinkOption []))))))
          (finally
            (java.nio.file.Files/setPosixFilePermissions (.toPath dir)
              (java.nio.file.attribute.PosixFilePermissions/fromString "rwx------"))))))))
