#!/usr/bin/env bb
(ns suite-classify
  "ROUND-ONE STATIC CLASSIFIER for the JVM test-suite spike
   (docs/observations/2026-09-04-suite-spike-spec.md).

   Scans every test namespace under test/ (both the `clojure -M` mcp-test
   lane and the `bb test/run_all.clj` lane) and tags each one with the
   properties that decide which lane it can safely live in, carrying the
   EVIDENCE (file:line + the matched text) for every tag -- a classification
   with no file:line is an opinion, not a finding.

   A static scan sees SPELLINGS, not behaviour: a test that shells out
   through a helper in another namespace, or names its port through a var,
   is invisible here. That is why round one pairs this with the runtime
   evidence from dev/experiments/suite_timing.clj (descendant-process
   sampling and a per-namespace temp-root diff) and treats a namespace
   flagged by EITHER as carrying the property.

   Usage:  bb dev/experiments/suite_classify.clj > out.edn"
  (:require
   [babashka.fs :as fs]
   [clojure.pprint :as pp]
   [clojure.string :as str]))

(def patterns
  "category -> [[regex label] ...]. Ordered; every match is recorded."
  {:spawns-process
   [[#"ProcessBuilder" "ProcessBuilder"]
    [#"clojure\.java\.shell" "clojure.java.shell"]
    [#"\bsh/sh\b|\(shell/sh\b" "shell/sh"]
    [#"babashka\.process|\bproc/process\b|\bproc/shell\b|\bp/process\b" "babashka.process"]
    [#"\"make\"|\bmake\s+[a-z-]+\"" "make"]
    [#"\"clojure\"|\bclojure\s+-M" "clojure -M"]
    [#"\"bb\"" "bb"]
    [#"\"java\"|java\s+-cp" "java"]
    [#"launcher|launch-|spawn" "launcher/spawn naming"]]

   :temp-filesystem
   [[#"java\.io\.tmpdir" "java.io.tmpdir"]
    [#"create-temp-dir|createTempDirectory" "create-temp-dir"]
    [#"create-temp-file|createTempFile" "create-temp-file"]
    [#"\"/var/tmp" "/var/tmp literal"]
    [#"\"/tmp" "/tmp literal"]]

   :binds-port
   [[#"ServerSocket" "ServerSocket"]
    [#":port\s+0\b" ":port 0 (ephemeral)"]
    [#":port\s+[1-9][0-9]{3,4}\b" ":port <fixed>"]
    [#"localhost:[1-9][0-9]{3,4}" "fixed host:port"]
    [#"jetty|run-server|http-server" "http server"]]

   :shared-paths
   [[#"\"target/|\btarget/" "target/"]
    [#"\.cpcache" ".cpcache"]
    [#"\.local/state" "~/.local/state"]
    [#"user\.home|\(System/getenv \"HOME\"\)" "$HOME"]]

   :global-mutation
   [[#"alter-var-root" "alter-var-root"]
    [#"with-redefs" "with-redefs"]
    [#"System/setProperty" "System/setProperty"]
    [#"defonce" "defonce"]
    [#"\(reset! [a-z-]*/" "reset! on a foreign atom"]]

   :sleeps-or-polls
   [[#"Thread/sleep" "Thread/sleep"]
    [#"\(deref [^)]+ \d+" "deref with timeout"]
    [#":timeout\s+\d+" ":timeout"]
    [#"\bpoll\b|wait-for|wait-until" "poll/wait loop"]]})

(defn- ns-name-of
  [text file]
  (or (second (re-find #"\(ns\s+([A-Za-z0-9._-]+)" text))
      (str file)))

(defn classify-file
  [file]
  (let [text (slurp (str file))
        lines (str/split-lines text)
        hits (for [[category pats] patterns
                   [re label] pats
                   [idx line] (map-indexed vector lines)
                   :when (and (re-find re line)
                              ;; skip comment-only and docstring-ish lines to
                              ;; cut the obvious false positives; a scan that
                              ;; cries wolf gets ignored, which is worse than
                              ;; one that misses.
                              (not (str/starts-with? (str/trim line) ";")))]
               {:category category
                :label label
                :evidence (str file ":" (inc idx))
                :text (str/trim (subs line 0 (min 110 (count line))))})]
    {:ns (ns-name-of text file)
     :file (str file)
     :lines (count lines)
     :hits (vec hits)
     :categories (into (sorted-set) (map :category hits))}))

(defn -main [& _]
  (let [files (->> (concat (fs/glob "test" "**/*.clj") (fs/glob "test" "**/*.cljc"))
                   (map str)
                   (filter #(or (str/includes? % "_test.")
                                (str/includes? % "run_all")))
                   sort)
        rows (mapv classify-file files)]
    (pp/pprint {:generated-at (str (java.time.Instant/now))
                :file-count (count rows)
                :counts (frequencies (mapcat :categories rows))
                :pure (mapv :ns (filter (comp empty? :categories) rows))
                :rows rows})))

(when (= *file* (System/getProperty "babashka.file")) (-main))
