(ns clj-surgeon.namespace-split-warm-test
  "Real nREPL boundary; captured analysis is stubbed, cold profile logs its real true command."
  {:lane :integration}
  (:require
   [clj-surgeon.namespace-split-io :as boundary]
   [clj-surgeon.namespace-split-test :refer [with-paper-workspace]]
   [clj-surgeon.namespace-split-warm :as warm]
   [clj-surgeon.verification-process :as process]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.test :as t :refer [deftest is]]
   [nrepl.server :as nrepl-server]
   [nrepl.transport :as transport]))

(defn with-warm-workspace [f]
  (let [root (.toFile (java.nio.file.Files/createTempDirectory "warm-split-" (make-array java.nio.file.attribute.FileAttribute 0)))
        loader (clojure.lang.DynamicClassLoader. (clojure.lang.RT/baseLoader))
        prefix (str "warmcase" (str/replace (str (random-uuid)) "-" ""))
        source-lib (str prefix ".views")
        dest-lib (str prefix ".new")
        test-lib (str prefix ".new-test")
        src-file (str "src/" prefix "/views.clj")
        dst-file (str "src/" prefix "/new.clj")
        test-file (str "test/" prefix "/new_test.clj")
        evaluations (atom [])
        server (nrepl-server/start-server
                 :bind "127.0.0.1" :port 0
                 :handler (fn [{:keys [id code transport]}]
                            (swap! evaluations conj code)
                            (try
                              (let [value (if (str/includes? code "System/getProperty \"user.dir\"")
                                            (str root)
                                            (with-bindings {clojure.lang.Compiler/LOADER loader
                                                            #'*ns* (the-ns 'user)
                                                            #'t/*report-counters* nil
                                                            #'t/*testing-vars* ()
                                                            #'t/*testing-contexts* ()
                                                            #'t/*test-out* (java.io.StringWriter.)}
                                              (load-string code)))]
                                (transport/send transport {:id id :value (pr-str value) :status #{"done"}}))
                              (catch Throwable _
                                (transport/send transport {:id id :status #{"eval-error" "done"}})))))]
    ;; This test-owned endpoint uses an isolated source classloader and workspace
    ;; identity. Probe evals execute the actual require/reload and clojure.test.
    (try
      (doseq [dir ["src" "test"]]
        (.mkdirs (io/file root dir))
        (.addURL loader (.toURL (.toURI (io/file root dir)))))
      (spit (io/file root ".nrepl-port") (:port server))
      (f {:root root :source-lib source-lib :dest-lib dest-lib :test-lib test-lib
          :src-file src-file :dst-file dst-file :test-file test-file :evaluations evaluations
          :port (:port server)})
      (finally
        (nrepl-server/stop-server server)
        (doseq [lib [source-lib dest-lib test-lib]]
          (when (find-ns (symbol lib)) (remove-ns (symbol lib))))
        (doseq [file (reverse (file-seq root))] (.delete file))))))

;; @spec NS-SPLIT-029
;; INTENT-TEST: NS-SPLIT-029
;; @spec NS-SPLIT-030
;; INTENT-TEST: NS-SPLIT-030
(deftest warm-probe-precedes-cold-and-rolls-back
  (doseq [mode [nil :cold :warm]
          damage [:none :reload :test]]
    (with-warm-workspace
      (fn [{:keys [root source-lib dest-lib test-lib src-file dst-file test-file port evaluations]}]
        (let [source (str "(ns " source-lib ")\n(def x "
                          (if (= :reload damage)
                            (str "(if (= (str *ns*) " (pr-str dest-lib) ") (throw (ex-info \"broken destination\" {})) 1)")
                            (if (= :test damage) "2" "1")) ")\n")
              originals {src-file source
                         test-file (str "(ns " test-lib " (:require [clojure.test :refer [deftest is]]))\n"
                                        "(deftest moved-value (is (= 1 @(ns-resolve '" dest-lib " 'x))))\n")}
              command-log (io/file root "commands.log")
              request {:workspace_root (str root) :source {:file src-file :lib source-lib}
                       :destinations [{:file dst-file :lib dest-lib :forms ["x"] :alias_policy ["new"]}]
                       :promotion_policy "promote-required" :source_retirement "delete"
                       :roots ["src" "test"] :verification {:profile "unit"}}]
          (doseq [[file text] (assoc originals "deps.edn" "{:paths [\"src\" \"test\"]}")]
            (let [p (io/file root file)] (.mkdirs (.getParentFile p)) (spit p text)))
          ;; The old destination-dependent throw does not fire in the valid source.
          (is (= 1 (warm/eval! port (str "(do (require '" source-lib ") 1)") 1000)))
          (let [run-process! process/run-process!
                r (with-redefs [boundary/analyze! (fn [_] {:analysis {} :findings [] :check {:exit 0 :duration_ms 0}})
                                process/run-process! (fn [& args]
                                                       (spit command-log "cold\n" :append true)
                                                       (apply run-process! args))]
                    (boundary/execute! {:verification-profiles {"unit" (cond-> {:commands [["/usr/bin/true"]]}
                                                                         mode (assoc :proof mode))}
                                        :receipt-dir (str root "/receipts")} request))
                check (first (filter #(= "warm-probe" (:name %)) (:checks r)))]
            (is (some? check) (pr-str r))
            (is (< (or (:duration_ms check) 99999) 2000))
            (if (= :none damage)
              (do (is (:ok r) (pr-str r))
                  (is (= "passed" (:status check)))
                  (is (= 1 (get-in check [:summary :test])))
                  (is (= (= :warm mode) (= "committed-probe-only" (:state r))))
                  (is (= (not= :warm mode) (:verification_complete r)))
                  (is (= (if (= :warm mode) ["/usr/bin/true"] []) (:proof_pending r)))
                  (is (= (not= :warm mode) (.exists command-log)))
                  (when (not= :warm mode)
                    (is (= "cold\n" (slurp command-log)))
                    (is (pos? (:duration_ms (first (filter #(= "true" (:name %)) (:checks r))))))))
              (do (is (= "rolled-back" (:state r)) (pr-str r))
                  (is (false? (:verification_complete r)))
                  (is (= "failed" (:status check)))
                  (is (not (.exists command-log)))
                  (is (not (.exists (io/file root dst-file))))
                  (doseq [[file text] originals] (is (= text (slurp (io/file root file)))))
                  (when (= :test damage) (is (= 1 (:failures check))))))
            (is (not-any? #(or (str/includes? % "reload-all") (str/includes? % "remove-ns")) @evaluations))))))))

;; @spec NS-SPLIT-029
;; INTENT-TEST: NS-SPLIT-029
(deftest discovery-ignores-stale-and-foreign-jvms
  (with-paper-workspace
    (fn [root _]
      (is (nil? (warm/discover! root)))
      (doseq [port ["not-a-port" "0" "65536" "1"]]
        (spit (io/file root ".nrepl-port") port)
        (is (nil? (warm/discover! root))))
      (let [server (nrepl-server/start-server :bind "127.0.0.1" :port 0)]
        (try
          (spit (io/file root ".nrepl-port") (:port server))
          (is (nil? (warm/discover! root)) "A live JVM for another cwd is unavailable")
          (is (= 42 (warm/eval! (:port server) "42" 1000)))
          (finally (nrepl-server/stop-server server)))))))
