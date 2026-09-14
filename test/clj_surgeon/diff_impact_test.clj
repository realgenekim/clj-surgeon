(ns clj-surgeon.diff-impact-test
  "Selected-set class oracle: inb-f85401 and inb-1b7f3c. Real Git fixture diffs,
   actual script discovery; fixture subjects are never required or executed."
  {:lane :integration}
  (:require
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.java.shell :as shell]
   [clojure.string :as str]
   [clojure.test :refer [deftest is testing]]))

(defn run-fixture
  "Run the script on a committed fixture plus an uncommitted diff. Only the
   environment admission predicate is replaced: these are selection/CLI tests,
   not gate-envelope attestations. Use fixed-point only for an empty selection."
  [files changes mode]
  (let [root (.toFile (java.nio.file.Files/createTempDirectory
                        "impact-" (make-array java.nio.file.attribute.FileAttribute 0)))
        repository (.getCanonicalPath (io/file "."))
        script (str repository "/test/diff_impact.clj")
        classpath (str/join java.io.File/pathSeparator
                    (map #(str repository "/" %) ["src" "test" "libs/clj-splice/src"]))
        output (io/file root "out")
        command (fn [& args] (apply shell/sh (concat args [:dir (str root)])))
        write! (fn [[path text]]
                 (let [f (io/file root path)] (io/make-parents f) (spit f text)))]
    (try
      (doseq [entry files] (write! entry))
      (doseq [args [["git" "init" "-q"] ["git" "add" "."]
                    ["git" "-c" "user.name=Fixture" "-c" "user.email=fixture@example.invalid"
                     "-c" "core.hooksPath=/dev/null" "commit" "-qm" "fixture"]]]
        (let [result (apply command args)]
          (when-not (zero? (:exit result)) (throw (ex-info "fixture git failed" result)))))
      (doseq [entry changes] (write! entry))
      (let [code (str "(binding [*command-line-args* [\"--library\"]] (load-file " (pr-str script) ")) "
                      "(with-redefs [user/gate-environment? (constantly true)] "
                      "(user/main " (pr-str ["HEAD" (str output) mode]) "))")
            result (command "bb" (str "-Djava.io.tmpdir=" (System/getProperty "java.io.tmpdir"))
                            "--classpath" classpath "-e" code)
            inventory (io/file output (str "impact-" mode ".edn"))]
        (assoc result :inventory (when (.isFile inventory) (edn/read-string (slurp inventory)))))
      (finally
        (doseq [f (reverse (file-seq root))] (io/delete-file f true))))))

(defn selected-set [result]
  (set (map :namespace (get-in result [:inventory :namespaces]))))

(def base-files
  {"src/fixture/core.clj" "(ns fixture.core)\n(def value 1)"
   "src/fixture/unrequired.clj" "(ns fixture.unrequired)\n(def kind :old)"
   "test/fixture/control_test.clj" "(ns fixture.control-test (:require [fixture.core]))"
   "test/fixture/unrelated_test.clj" "(ns fixture.unrelated-test)"})

;; @spec DIFF-IMPACT-001
(deftest data-file-reader-selected
  (doseq [path ["docs/intent/probe/refusals.edn" "resources/registry.edn"]]
    (let [result (run-fixture
                   (assoc base-files path "{:kind :old}"
                          "test/fixture/reader_test.clj"
                          (str "(ns fixture.reader-test) (def registry (slurp " (pr-str path) "))"))
                   {path "{:kind :new}"} "list")]
      (is (= 0 (:exit result)) (pr-str result))
      (is (= '#{fixture.reader-test} (selected-set result)) (pr-str result)))))

;; @spec DIFF-IMPACT-002
(deftest source-text-scanner-selected
  ;; Faithful reduced shape of splice-envelope-test/probe-vocabulary at df0c9e1c.
  (doseq [body ["(defn scan [] (slurp \"src/fixture/unrequired.clj\"))"
                "(defn scan [] (map slurp (file-seq (io/file \"src\" \"fixture\"))))"
                "(def owners [\"unrequired.clj\"]) (defn scan [] (for [f owners] (slurp (str \"src/fixture/\" f))))"]]
    (let [result (run-fixture
                   (assoc base-files "test/fixture/scanner_test.clj"
                          (str "(ns fixture.scanner-test) " body))
                   {"src/fixture/unrequired.clj" "(ns fixture.unrequired) (def kind :new)"} "list")]
      (is (= 0 (:exit result)) (pr-str result))
      (is (= '#{fixture.scanner-test} (selected-set result)) (pr-str result)))))

;; @spec DIFF-IMPACT-003
(deftest test-helper-transitive-selected
  (let [result (run-fixture
                 (merge base-files
                        {"test/fixture/helper.clj" "(ns fixture.helper) (def value 1)"
                         "test/fixture/middle.clj" "(ns fixture.middle (:require [fixture.helper]))"
                         "test/fixture/direct_test.clj" "(ns fixture.direct-test (:require [fixture.helper]))"
                         "test/fixture/leaf_test.clj" "(ns fixture.leaf-test (:require [fixture.middle]))"})
                 {"test/fixture/helper.clj" "(ns fixture.helper) (def value 2)"} "list")]
    (is (= 0 (:exit result)) (pr-str result))
    (is (= '#{fixture.direct-test fixture.leaf-test} (selected-set result)) (pr-str result))))

;; @spec DIFF-IMPACT-004
(deftest nothing-selected-is-typed
  (doseq [mode ["list" "fixed-point"]]
    (let [result (run-fixture (assoc base-files "README.md" "old") {"README.md" "new"} mode)
          inventory (:inventory result)]
      (is (= 0 (:exit result)) (pr-str result))
      (is (= :nothing-selected (:status inventory)) (pr-str result))
      (is (= ["README.md"] (:changed-files inventory)) (pr-str result))
      (is (= [{:file "README.md" :reason :no-dependency-edge}] (:unmatched-files inventory))
          (pr-str result))
      (is (= #{} (selected-set result))))))

;; @spec DIFF-IMPACT-003
(deftest require-edge-control
  (let [result (run-fixture base-files
                 {"src/fixture/core.clj" "(ns fixture.core) (def value 2)"} "list")]
    (is (= 0 (:exit result)) (pr-str result))
    (is (= '#{fixture.control-test} (selected-set result)) (pr-str result))
    (is (= [["fixture.core"]] (get-in result [:inventory :namespaces 0 :paths])))))
