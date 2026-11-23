(ns clj-surgeon.write-refusal
  "Test support for assertions that need the filesystem to refuse a write.
   Mode bits refuse a normal user but not uid 0, so a test that needs a real
   refusal asks this namespace for one and skips, saying why, when none exists."
  (:require
   [babashka.process :as proc]
   [clojure.java.io :as io]
   [clojure.string :as str]))

(def root?
  "True when this process runs as uid 0, which ignores mode bits."
  (delay (= "0" (str/trim (:out (proc/sh "id" "-u"))))))

(defn skip!
  "Record on stderr that the caller's assertions did not run, and why."
  [test-name reason]
  (binding [*out* *err*]
    (println (str "SKIP " test-name ": " reason))))

(defn- can-create-in? [dir]
  (try
    (.delete (java.io.File/createTempFile ".write-refusal-probe-" ".tmp" (io/file (str dir))))
    true
    (catch java.io.IOException _ false)))

(defn- chattr! [flag dir]
  (try
    (zero? (:exit (proc/sh "chattr" flag (str dir))))
    (catch java.io.IOException _ false)))

(defn deny-writes!
  "Make dir refuse new entries for this process: mode bits for a normal user,
   plus the immutable attribute for root. Returns nil when a probe proves the
   refusal is real, otherwise a string saying why it is not."
  [dir]
  (.setWritable (io/file (str dir)) false false)
  (when (and @root? (can-create-in? dir))
    (chattr! "+i" dir))
  (when (can-create-in? dir)
    (str "this process can still create files in " dir
         (when @root? " (uid 0 ignores mode bits and chattr +i was unavailable)"))))

(defn allow-writes!
  "Undo deny-writes! so the directory can be deleted."
  [dir]
  (when @root? (chattr! "-i" dir))
  (.setWritable (io/file (str dir)) true false))
