(ns replay-test
  (:require [babashka.classpath :as cp] [babashka.fs :as fs] [babashka.process :refer [shell]]
            [clojure.edn :as edn] [clojure.test :refer [deftest is run-tests]]))
(def fixture (fs/parent (fs/absolutize *file*)))
(cp/add-classpath (str fixture))
(require '[replay :as r])
(def recorded (edn/read-string (slurp (str (fs/path fixture "sentinel-20261025-split-receipt.edn")))))
(def facts (r/receipt-facts recorded))
(deftest donor-environment
  (doseq [safe [{} {"PATH" "/bin"} {"PATH" "/bin" "HOME" "/home" "JAVA_HOME" "/jdk" "LANG" "C"}]]
    (is (= safe (r/donor-env (merge safe {"SMTP_HOST" "example.invalid" "SMTP_PASS" "secret"
                                        "PYTHONOPTIMIZE" "1" "TOKEN" "secret"}))))))
(deftest frozen-facts-and-comparison-matrix
  (doseq [[k _] facts v [:changed :replay/missing]]
    (is (= 1 (r/comparison-exit (r/compare-facts recorded (assoc facts k v))))))
  (doseq [paths [nil [] [".clj-surgeon.edn"] (:unexpected_paths facts) ["unrelated.txt"]] exit [0 1 97 nil]]
    (is (= (if (and (= 0 exit) (not= paths ["unrelated.txt"])) 0 1)
           (r/comparison-exit (r/compare-facts recorded (assoc facts :unexpected_paths paths)) exit)))))
(deftest missing-created-and-refused
  (is (every? #(.contains % " DIFFERS ") (r/compare-facts {} (r/receipt-facts {}))))
  (doseq [[before after n] [[[] [] 0] [[] ["a" "b" "other"] 2] [["a"] ["a" "b"] 1]]]
    (is (= n (r/created-count ["a" "b" "a"] before after))))
  (let [r {:state "refused" :error_type "unavailable" :mutation_attempted false :committed false}]
    (is (r/plan-fallback? r))
    (doseq [k (keys r) v [nil true "" :unknown]] (is (not (r/plan-fallback? (assoc r k v)))))))
(deftest frozen-profile-and-digests
  (is (nil? (r/verify-manifest! fixture)))
  (let [config (edn/read-string (slurp (str (fs/path fixture "workspace.clj-surgeon.edn"))))]
    (is (= [["bin/kaocha" "unit" "--fail-fast"]
            ["env" "-u" "PYTHONOPTIMIZE" "python3" "/f\"x/cc-oracle-replay.py" "/a space" "/f\"x/cc-split.json"]]
           (get-in (r/rebind-config config "/a space" "/f\"x") [:verification-profiles "split-unit" :commands])))))
(deftest oracle-refuses-optimization-and-substitution-drift
  (is (= 2 (:exit (shell {:extra-env {"PYTHONOPTIMIZE" "1"} :out :string :err :string :continue true}
                    "python3" (str (fs/path fixture "cc-oracle-replay.py")) "x" "y"))))
  (let [dir (fs/create-temp-dir) wrapper (str (fs/path dir "cc-oracle-replay.py"))
        anchor "'/var/tmp/forge/plan2/cellC/cc-split.json'"]
    (try (fs/copy (fs/path fixture "cc-oracle-replay.py") wrapper)
         (doseq [[source exit] [["" 2] [(str anchor "\n" anchor) 2]
                                [(str "assert " anchor " == 'y'") 0] [(str anchor "\nassert False") 1]]]
           (spit (str (fs/path dir "cc-oracle.py")) source)
           (is (= exit (:exit (shell {:out :string :err :string :continue true}
                                "env" "-u" "PYTHONOPTIMIZE" "python3" wrapper "x" "y")))))
         (finally (fs/delete-tree dir)))))
(deftest runner-subprocess-acceptance-and-refusal
  (let [dir (fs/create-temp-dir) donor (fs/path dir "donor") core (fs/path donor "src/clj_surgeon/core.clj")]
    (try (fs/create-dirs (fs/parent core))
         (spit (str (fs/path donor "recorded.edn")) (pr-str recorded))
         (spit (str core) (str "(ns clj-surgeon.core (:require [babashka.fs :as fs] [clojure.edn :as e]))\n"
                (pr-str '(let [file (last *command-line-args*) r (e/read-string (slurp file)) root (:workspace_root r)
                               mode (e/read-string (slurp "mode.edn"))]
                           (assert (every? #{"PATH" "HOME" "JAVA_HOME" "LANG"} (keys (System/getenv))))
                           (assert (= (fs/parent root) (fs/parent (fs/parent file))))
                           (assert (fs/executable? (fs/path root "bin/kaocha")))
                           (assert (fs/exists? (fs/path root ".clj-surgeon.edn")))
                           (doseq [{:keys [file]} (:destinations r)]
                             (fs/create-dirs (fs/parent (fs/path root file))) (spit (str (fs/path root file)) ""))
                           (prn (if (= mode :refused)
                                  {:state "refused" :error_type "stub" :mutation_attempted false :committed false}
                                  (e/read-string (slurp "recorded.edn"))))
                           (System/exit (if (number? mode) mode 0))))))
         (doseq [[mode exit] [[0 0] [97 1] [:refused 1]]]
           (spit (str (fs/path donor "mode.edn")) (pr-str mode))
           (let [result (shell {:out :string :err :string :continue true
                                :extra-env {"SMTP_HOST" "example.invalid" "PYTHONOPTIMIZE" "1" "TOKEN" "secret"}}
                          "bb" (str (fs/path fixture "replay.clj")) (str donor) (str (fs/path dir (str mode))))]
             (is (= exit (:exit result)) (pr-str result))
             (is (= (= mode :refused) (.contains (:out result) "Invocation: plan-only")))))
         (finally (fs/delete-tree dir)))))
(let [{:keys [fail error]} (run-tests 'replay-test)] (System/exit (if (zero? (+ fail error)) 0 1)))
