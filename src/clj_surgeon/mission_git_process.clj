(ns clj-surgeon.mission-git-process
  "One bounded subprocess including stdin delivery; argv only."
  (:require
   [clj-surgeon.spawn-ledger :as spawn]
   [clojure.string :as str])
  (:import
   (java.util.concurrent TimeUnit)))

(def max-bytes 1048576)
(defn fail! [kind] (throw (ex-info "Git process boundary refused" {:error-type kind})))

(defn run-process!
  "No shell expansion. One monotonic deadline covers stdin, wait and capture.
   The command and input are trusted planner output, not model-owned fields."
  [root cmd input timeout-ms]
  (let [deadline (+ (System/nanoTime) (* 1000000 timeout-ms))
        builder (doto (ProcessBuilder. ^java.util.List cmd)
                  (.directory (java.io.File. root)) (.redirectErrorStream true))
        env (.environment builder)]
    (doseq [k (vec (.keySet env)) :when (and (str/starts-with? k "GIT_")
                                          (not (contains? #{"GIT_AUTHOR_NAME" "GIT_AUTHOR_EMAIL" "GIT_AUTHOR_DATE"
                                                            "GIT_COMMITTER_NAME" "GIT_COMMITTER_EMAIL" "GIT_COMMITTER_DATE"} k)))] (.remove env k))
    (.put env "GIT_TERMINAL_PROMPT" "0")
    (.put env "LC_ALL" "C")
    (let [process (.start builder)
          output (future (with-open [stream (.getInputStream process)]
                           (let [bytes (.readNBytes stream (inc max-bytes))]
                             (when (> (alength bytes) max-bytes)
                               (.destroyForcibly process) (fail! :git-output-limit))
                             bytes)))
          writer (future (with-open [stream (.getOutputStream process)]
                           (when input (.write stream (.getBytes ^String input "UTF-8")))))
          remaining #(max 0 (quot (- deadline (System/nanoTime)) 1000000))
          await #(let [result (deref % (remaining) ::timeout)]
                   (when (= result ::timeout) (fail! :git-timeout)) result)]
      (try
        (spawn/record! (.pid process) cmd)
        (when-not (.waitFor process (remaining) TimeUnit/MILLISECONDS) (fail! :git-timeout))
        (await writer)
        (let [bytes (await output)]
          (when-not (zero? (.exitValue process)) (fail! :git-command-failed))
          (str (.decode (doto (.newDecoder java.nio.charset.StandardCharsets/UTF_8)
                          (.onMalformedInput java.nio.charset.CodingErrorAction/REPORT)
                          (.onUnmappableCharacter java.nio.charset.CodingErrorAction/REPORT))
                        (java.nio.ByteBuffer/wrap bytes))))
        (finally
          (.destroyForcibly process)
          (when-not (.waitFor process 1000 TimeUnit/MILLISECONDS)
            (fail! :git-process-cleanup-incomplete))
          (future-cancel writer)
          (future-cancel output))))))
