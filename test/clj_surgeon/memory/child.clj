(ns clj-surgeon.memory.child
  "Run one memory arm in a child JVM with an explicit heap ceiling.

   The child inherits this process's classpath, so an arm is one namespace on
   the test path and needs no dependency resolution of its own. Everything the
   parent asserts comes back as an exit code plus the child's own receipt line."
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.edn :as edn]))

(def receipt-prefix "#MEMORY-RECEIPT ")

(defn- java-binary []
  (str (io/file (System/getProperty "java.home") "bin" "java")))

(defn run-arm
  "Run `main-ns` in a child JVM at `xmx` with `args`.

   Returns {:exit e :out s :err s :receipt m}. `:receipt` is the child's
   emitted receipt map when it printed one, otherwise nil."
  [{:keys [main-ns xmx args timeout-ms properties]
    :or {xmx "256m" args [] timeout-ms 900000 properties {}}}]
  (let [command (concat [(java-binary) (str "-Xmx" xmx) "-XX:+ExitOnOutOfMemoryError"]
                        (map (fn [[k v]] (str "-D" (name k) "=" v)) properties)
                        ["-cp" (System/getProperty "java.class.path")
                         "clojure.main" "-m" (str main-ns)]
                        (map str args))
        process (-> (ProcessBuilder. ^java.util.List (vec command))
                    (.redirectErrorStream false)
                    (.start))
        out (future (slurp (.getInputStream process)))
        err (future (slurp (.getErrorStream process)))
        finished? (.waitFor process timeout-ms java.util.concurrent.TimeUnit/MILLISECONDS)]
    (when-not finished?
      (.destroyForcibly process)
      (.waitFor process))
    (let [out-text @out
          err-text @err
          receipt-line (first (filter #(str/starts-with? % receipt-prefix)
                                      (str/split-lines out-text)))]
      {:exit (if finished? (.exitValue process) :timeout)
       :command (vec command)
       :out out-text
       :err err-text
       :receipt (when receipt-line
                  (edn/read-string (subs receipt-line (count receipt-prefix))))})))

(defn out-of-memory?
  "True when the child died of heap exhaustion rather than any other fault."
  [{:keys [exit out err]}]
  (and (not= 0 exit)
       (boolean (or (str/includes? (str err) "OutOfMemoryError")
                    (str/includes? (str out) "OutOfMemoryError")))))
