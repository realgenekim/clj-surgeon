(ns replay-test (:require [babashka.classpath :as cp] [babashka.fs :as fs] [babashka.process :refer [shell]]
   [clojure.edn :as edn] [clojure.test :refer [deftest is run-tests]]))
(def fixture (fs/parent (fs/absolutize *file*))) (cp/add-classpath (str fixture)) (require '[replay :as r])
(defn read-edn [p] (edn/read-string (slurp (str p))))
(def recorded (read-edn (fs/path fixture "sentinel-20261025-split-receipt.edn")))
(def facts (r/receipt-facts recorded)) (def config (read-edn (fs/path fixture "workspace.clj-surgeon.edn")))
(defn run [& args] (apply shell {:out :string :err :string :continue true} args))
(defn snapshot [root] (into {} (for [p (r/inventory root)] [p (slurp (str (fs/path root p)))])))
(deftest frozen-facts-and-comparison-matrix (doseq [[k _] facts v [:changed :replay/missing]]
    (is (= 1 (r/comparison-exit (r/compare-facts recorded (assoc facts k v))))))
  (doseq [paths [nil [] [".clj-surgeon.edn"] (:unexpected_paths facts) ["unrelated.txt"]] exit [0 1 97 nil]]
    (is (= (if (and (= 0 exit) (some? paths) (not= paths ["unrelated.txt"])) 0 1)
           (r/comparison-exit (r/compare-facts recorded (assoc facts :unexpected_paths paths)) exit)))))
(deftest missing-created-and-refused (is (every? #(.contains % " DIFFERS ") (r/compare-facts {} (r/receipt-facts {}))))
  (doseq [[before after n] [[[] [] 0] [[] ["a" "b" "other"] 2] [["a"] ["a" "b"] 1]]]
    (is (= n (:created (r/inventory-facts {} (map #(str "source/" %) before) ["a" "b" "a"] after)))))
  (let [r {:state "refused" :error_type "unavailable" :mutation_attempted false :committed false}]
  (is (r/plan-fallback? r)) (doseq [k (keys r) v [nil true "" :unknown]] (is (not (r/plan-fallback? (assoc r k v)))))))
(deftest frozen-profile-and-digests (is (= 34 (count (r/verify-manifest! fixture))))
  (is (= [["bin/kaocha" "unit" "--fail-fast"]
          ["env" "-u" "PYTHONOPTIMIZE" "python3" "/f\"x/cc-oracle-replay.py" "/a space" "/f\"x/cc-split.json"]]
         (get-in (r/rebind-config config "/a space" "/f\"x") [:verification-profiles "split-unit" :commands]))))
(deftest oracle-guards
  (is (= 2 (:exit (run "env" "PYTHONOPTIMIZE=1" "python3" (str (fs/path fixture "cc-oracle-replay.py")) "x" "y"))))
  (let [dir (fs/create-temp-dir) wrapper (fs/path dir "cc-oracle-replay.py")
        anchor "'/var/tmp/forge/plan2/cellC/cc-split.json'"]
    (try (fs/copy (fs/path fixture "cc-oracle-replay.py") wrapper)
         (doseq [[source exit] [["" 2] [(str anchor "\n" anchor) 2]
                                [(str "assert " anchor " == 'y'") 0] [(str anchor "\nassert False") 1]]]
           (spit (str (fs/path dir "cc-oracle.py")) source)
           (is (= exit (:exit (run "env" "-u" "PYTHONOPTIMIZE" "python3" (str wrapper) "x" "y")))))
         (finally (fs/delete-tree dir)))))
(def donor-stub '(let [file (last *command-line-args*) r (e/read-string (slurp file)) root (:workspace_root r)
                      mode (e/read-string (slurp "mode.edn"))]
  (assert (every? #{"PATH" "HOME" "JAVA_HOME" "LANG"} (keys (System/getenv))))
  (assert (= (fs/parent root) (fs/parent (fs/parent file)))) (assert (fs/executable? (fs/path root "bin/kaocha")))
  (assert (fs/exists? (fs/path root ".clj-surgeon.edn"))) (if (= mode :refused)
    (prn {:state "refused" :error_type "stub" :mutation_attempted false :committed false :plan_only (:plan_only r)})
    (do (doseq [{:keys [file]} (:destinations r)]
          (fs/create-dirs (fs/parent (fs/path root file))) (spit (str (fs/path root file)) ""))
        (prn (assoc (e/read-string (slurp "recorded.edn")) :details_path (str root "/absent.edn")
                    :workspace_status {:exit 128 :error "Not a Git workspace"}))))
  (System/exit (if (= mode :refused) 0 mode)))) (deftest runner-subprocess-acceptance-and-refusal
  (let [dir (fs/create-temp-dir) donor (fs/path dir "donor") core (fs/path donor "src/clj_surgeon/core.clj")]
    (try (fs/create-dirs (fs/parent core)) (spit (str (fs/path donor "recorded.edn")) (pr-str recorded))
         (spit (str core) (str "(require '[babashka.fs :as fs] '[clojure.edn :as e])\n" (pr-str donor-stub)))
         (doseq [[mode exit] [[0 0] [97 1] [:refused 1]] :let [root (fs/path dir (str mode))]]
           (spit (str (fs/path donor "mode.edn")) (pr-str mode))
           (let [{:keys [out err] :as result} (run "env" "SMTP_HOST=example.invalid" "PYTHONOPTIMIZE=1" "TOKEN=secret"
                      "bb" (str (fs/path fixture "replay.clj")) (str donor) (str root))]
             (is (= exit (:exit result)) (pr-str result)) (is (= "" err))
  (is (= (if (= mode :refused) ["0" "0"] [(str mode)]) (mapv second (re-seq #"Invocation: \S+ exit (\d+)" out))))
             (if (= mode :refused) (let [control (second (re-find #"Control: (\S+)" out))]
                 (is (= (assoc (snapshot (fs/path fixture "source")) ".clj-surgeon.edn"
                               (pr-str (r/rebind-config config (str root) (str fixture)))) (snapshot root)))
                 (is (= [nil true] (mapv #(:plan_only (read-edn (fs/path control %)))
                                        ["receipt-commit.edn" "receipt-diagnostic.edn"]))))
               (is (.contains out "workspace-inspection: failed 128 Not a Git workspace")))))
         (finally (fs/delete-tree dir)))))
(deftest pr94-profile-inventory (doseq [[extra match] [[[] "expected-different"] [[".hidden"] "DIFFERS"]]]
    (let [destinations (mapv #(str "views/" % ".clj") (range 20))
          paths (concat ["a" ".clj-surgeon.edn" ".cpcache/604528847.basis" ".cpcache/604528847.cp"] destinations extra)
          now (r/inventory-facts (assoc facts :unexpected_paths nil) ["source/a"] destinations paths)
          line (first (filter #(.startsWith % "unexpected_paths ") (r/compare-facts recorded now)))]
      (is (= [20 2 extra] ((juxt :created :profile-artefacts :unexpected_paths) now)))
      (doseq [s [match "profile-artefacts: 2 (.cpcache/)" (str "inventory: " (+ 24 (count extra)))]
              :when s] (is (.contains line s))) (when (seq extra) (is (.contains line ".hidden"))))))
(deftest unlisted-paths-refused-by-cli
  (let [dir (fs/create-temp-dir) copy (fs/path dir "fixture") scratch (fs/path dir "scratch")]
    (try (fs/copy-tree fixture copy)
         (doseq [name [".hidden" ".directory-link"] :let [path (fs/path copy "source" name)]]
           (if (= name ".hidden") (spit (str path) "extra") (fs/create-sym-link path dir))
           (let [{:keys [exit out err]} (run "bb" (str (fs/path copy "replay.clj")) (str dir) (str scratch))]
             (is (= 2 exit)) (is (= "" err)) (is (.contains out (str "unlisted-source-file source/" name)))
             (is (not (fs/exists? scratch))) (is (empty? (fs/glob dir "replay-control-*"))))
           (fs/delete path)) (finally (fs/delete-tree dir))))) (when (= *file* (System/getProperty "babashka.file"))
  (let [{:keys [fail error]} (run-tests 'replay-test)] (System/exit (if (zero? (+ fail error)) 0 1))))
