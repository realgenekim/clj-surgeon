(ns run-controls
  (:require
   [babashka.fs :as fs]
   [babashka.process :as p]
   [clojure.edn :as edn]
   [clojure.string :as str]))

(def root "docs/observations/2026-09-12-bbtower-block-b/attempt23")
(def runner-root "docs/observations/2026-09-12-bbtower-block-b/attempt22")
(fs/create-dirs (str root "/controls"))
(doseq [n (if (seq *command-line-args*) (map symbol *command-line-args*)
              '[clj-surgeon.cljc.merge-test clj-surgeon.cljc.split-test
                clj-surgeon.worktree-lifecycle-prune-test])
        runtime [:jvm :bb]]
  (let [prefix (str root "/controls/" n "-" (name runtime) "-test")
        receipt (str prefix ".edn")
        argv (into (if (= runtime :bb)
                     ["bb" "-Xmx1g" "-Djava.io.tmpdir=/var/tmp/forge/bbtower-fx"
                      (str runner-root "/portability_runner.clj")]
                     ["clojure" "-J-Xmx1g" "-Sdeps"
                      (pr-str {:paths ["src" "test" "dev/experiments" runner-root]})
                      "-M:clj-surgeon/test-deps" "-m" "portability-runner"])
                   [(name runtime) (str n) receipt "test"])
        command {:command argv :subject (str/trim (:out (p/shell {:out :string} "git" "rev-parse" "HEAD")))
                 :started (str (java.time.Instant/now))}
        _ (spit (str prefix ".command.edn") (pr-str command))
        exit (:exit @(p/process argv {:out (str prefix ".log") :err :out}))
        row (if (fs/exists? receipt) (edn/read-string (slurp receipt))
                {:namespace n :runtime runtime :status :process-failed})
        control (merge row command {:exit exit :receipt receipt})]
    (spit (str prefix ".control.edn") (pr-str control))
    (prn (select-keys control [:namespace :runtime :status :exit :result :elapsed-ms]))
    (flush)))
