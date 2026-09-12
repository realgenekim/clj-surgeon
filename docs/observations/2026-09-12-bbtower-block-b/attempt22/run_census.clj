(ns run-census
  (:require [babashka.fs :as fs]
            [babashka.process :as p]
            [clj-surgeon.lane-manifest :as lm]
            [clojure.edn :as edn]))

(def root "docs/observations/2026-09-12-bbtower-block-b/attempt22")
(def output (str root "/controls"))
(fs/create-dirs output)
(def subject (:out (p/shell {:out :string} "git" "rev-parse" "HEAD")))

(defn control! [n runtime mode]
  (let [prefix (str output "/" n "-" (name runtime) "-" mode)
        receipt (str prefix ".edn")
        command (into (if (= :bb runtime)
                        ["bb" "-Xmx1g" "-Djava.io.tmpdir=/var/tmp/forge/bbtower-fx"
                         (str root "/portability_runner.clj")]
                        ["clojure" "-J-Xmx1g" "-Sdeps"
                         (pr-str {:paths ["src" "test" "dev/experiments" root]})
                         "-M:clj-surgeon/test-deps" "-m" "portability-runner"])
                      [(name runtime) (str n) receipt mode])
        _ (spit (str prefix ".command.edn") (pr-str {:command command :subject subject}))
        process (p/process command {:out (str prefix ".log") :err :out})
        exit (:exit @process)
        row (if (fs/exists? receipt) (edn/read-string (slurp receipt))
                {:namespace n :runtime runtime :status :process-failed :exit exit})]
    (println (str n " " runtime " " mode " " (:status row) " exit=" exit))
    (flush)
    (assoc row :exit exit :receipt receipt)))

(doseq [n (sort (keys lm/namespace-runtimes))]
  (let [loaded (control! n :bb "load")
        rows (if (= :loaded (:status loaded))
               [(control! n :jvm "test") (control! n :bb "test")]
               [loaded])]
    (spit (str output "/index.edn") (str (pr-str {:namespace n :rows rows}) "\n") :append true)))
