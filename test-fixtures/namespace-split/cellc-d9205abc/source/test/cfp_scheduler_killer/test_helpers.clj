(ns cfp-scheduler-killer.test-helpers
  "Shared fixtures. Every test runs against its OWN temp log file, so tests are
   isolated from each other and from the developer's real data/store — and there
   is nothing to clean up afterwards but a temp file."
  (:require [clojure.java.io :as io]
            [cfp-scheduler-killer.store :as store]))

(defn temp-store-path []
  (let [dir (java.nio.file.Files/createTempDirectory
             "cfp-store-test" (into-array java.nio.file.attribute.FileAttribute []))]
    (str (io/file (str dir) "events.jsonl"))))

(defn with-temp-store
  "kaocha fixture: point the store at a fresh temp file and clear the atom."
  [f]
  (let [path (temp-store-path)
        prev-path @store/store-path
        prev-state @store/state]
    (try
      (store/reset-for-test! path)
      (f)
      (finally
        ;; Drain async sink deliveries BEFORE swapping the store back. Some
        ;; sinks append to the log; a straggler would otherwise write into the
        ;; next test's world and produce failures that move around between runs.
        (store/await-sinks!)
        (io/delete-file (io/file path) true)
        (reset! store/store-path prev-path)
        (reset! store/state prev-state)
        (reset! store/runtime-sinks [])))))
