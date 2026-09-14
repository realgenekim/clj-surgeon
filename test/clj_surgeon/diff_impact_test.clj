(ns clj-surgeon.diff-impact-test
  "Selected-set class oracle: inb-f85401 and inb-1b7f3c. Real Git fixture diffs,
   actual script discovery; fixture subjects are never required or executed."
  {:lane :integration}
  (:require
   [clj-surgeon.diff-impact :as impact]
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
            inventory (io/file output (str "impact-" mode ".edn"))
            results (io/file output (str "results-" mode ".edn"))]
        (assoc result
               :inventory (when (.isFile inventory) (edn/read-string (slurp inventory)))
               :results (when (.isFile results) (edn/read-string (slurp results)))))
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
      (is (= (if (= mode "list") 0 1) (:exit result)) (pr-str result))
      (is (= :hold-unmatched-files (:status inventory)) (pr-str result))
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

;; @spec DIFF-IMPACT-001
;; @spec DIFF-IMPACT-002
(deftest literal-content-edge-matrix
  (let [files #{"docs/intent/probe/refusals.edn" "resources/registry.edn"
                "src/fixture/a.clj" "src/fixture/deep/b.cljc" "src/other/c.clj"}
        cases [["(slurp \"docs/intent/probe/refusals.edn\")"
                #{{:file "docs/intent/probe/refusals.edn" :edge-kind :data-file}}]
               ["(for [verb [\"probe\"]] (slurp (str \"docs/intent/\" verb \"/refusals.edn\")))"
                #{{:file "docs/intent/probe/refusals.edn" :edge-kind :data-file}}]
               ["(slurp \"resources/./registry.edn\")"
                #{{:file "resources/registry.edn" :edge-kind :data-file}}]
               ["(slurp \"src/fixture/a.clj\")"
                #{{:file "src/fixture/a.clj" :edge-kind :source-text}}]
               ["(scan-source-files \"src/fixture\")"
                #{{:file "src/fixture/a.clj" :edge-kind :source-scan}
                  {:file "src/fixture/deep/b.cljc" :edge-kind :source-scan}}]
               ["(file-seq (io/file \"src\"))"
                #{{:file "src/fixture/a.clj" :edge-kind :source-scan}
                  {:file "src/fixture/deep/b.cljc" :edge-kind :source-scan}
                  {:file "src/other/c.clj" :edge-kind :source-scan}}]
               ["(slurp \"docs/missing.edn\")" #{}]
               ["(slurp \"../docs/intent/probe/refusals.edn\")" #{}]
               ["(slurp \"docs/../../resources/registry.edn\")" #{}]
               ["(slurp \"/resources/registry.edn\")" #{}]
               ["(slurp \"docs\\u0000/bad.edn\")" #{}]
               ["; (slurp \"resources/registry.edn\")\n#\"src/fixture/a.clj\"" #{}]
               ["#_(slurp \"src/fixture/a.clj\")" #{}]
               ["(def root \"src/fixture\")" #{}]
               ;; Never parse string contents or evaluate a reader-eval form.
               ["\"(slurp \\\"src/fixture/a.clj\\\")\"" #{}]
               ["#=(throw (Exception. \"must not execute\"))" #{}]]]
    (doseq [[source expected] cases]
      (testing source
        (is (= expected (set (impact/content-edges source files))))))))

;; @spec DIFF-IMPACT-001
;; @spec DIFF-IMPACT-003
;; @spec DIFF-IMPACT-005
(deftest mixed-edge-fixed-point-and-explanations
  (let [nodes [{:file "test/helper.clj" :namespace 'helper :requires #{'middle}
                :content-edges [{:file "docs/registry.edn" :edge-kind :data-file}]}
               {:file "test/middle.clj" :namespace 'middle :requires #{'helper}}
               {:file "test/direct_test.clj" :namespace 'direct-test :test? true :requires #{'helper}}
               {:file "test/leaf_test.clj" :namespace 'leaf-test :test? true :requires #{'direct-test}}
               {:file "test/unrelated_test.clj" :namespace 'unrelated-test :test? true :requires #{}}]
        changed #{"test/helper.clj" "docs/registry.edn"}
        selected (impact/select-impact nodes changed)]
    (is (= '#{direct-test leaf-test} (set (map :namespace (:namespaces selected)))))
    (is (= :selected (:status selected)))
    (is (= {:require 4 :data-file 1 :source-text 0 :source-scan 0} (:edge-counts selected)))
    (is (= {:data-file 2 :require 2} (:selection-edge-counts selected)))
    (doseq [entry (:namespaces selected)]
      (is (= #{{:file "test/helper.clj" :edge-kind :require :seed 'helper}
               {:file "docs/registry.edn" :edge-kind :data-file :seed 'helper}}
             (set (:reasons entry)))))
    (is (= selected (impact/select-impact (reverse nodes) (reverse (sort changed)))))
    (doseq [edge [0 1 2 3]]
      (let [cut (assoc-in nodes [edge :requires] #{})
            expected (case edge 2 #{} 3 '#{direct-test} '#{direct-test leaf-test})]
        (is (= expected (set (map :namespace (:namespaces (impact/select-impact cut changed))))))))))

;; @spec DIFF-IMPACT-004
(deftest empty-and-unmatched-reasons
  (doseq [[nodes changed reasons]
          [[[] [] []]
           [[{:file "src/a.clj" :namespace 'a :requires #{}}] ["src/a.clj"]
            [{:file "src/a.clj" :reason :no-test-dependent}]]
           [[] ["README.md"] [{:file "README.md" :reason :no-dependency-edge}]]]]
    (let [r (impact/select-impact nodes changed)]
      (is (= (if (seq changed) :hold-unmatched-files :nothing-selected) (:status r)))
      (is (= [] (:namespaces r)))
      (is (= reasons (:unmatched-files r)))))
  (let [r (run-fixture base-files {} "fixed-point")]
    (is (= 0 (:exit r)) (pr-str r))
    (is (= {:status :nothing-selected :changed-files [] :unmatched-files [] :namespaces []}
           (:results r)))))

;; @spec DIFF-IMPACT-005
(deftest printed-reasons-name-the-changed-file
  (let [r (run-fixture
            (assoc base-files "docs/registry.edn" "{}"
                   "test/fixture/reader_test.clj"
                   "(ns fixture.reader-test) (slurp \"docs/registry.edn\")")
            {"docs/registry.edn" "{:new true}"} "list")]
    (is (= 0 (:exit r)) (pr-str r))
    (is (str/includes? (:out r) "selected fixture.reader-test via data-file docs/registry.edn"))
    (is (= {:data-file 1} (get-in r [:inventory :selection-edge-counts])))))

;; Sol F1 pressure-point shapes from sol-verdict-1.md (the verdict omits bodies).
(def round-two-probes
  (let [data "resources/registry.edn"
        source "src/fixture/unrequired.clj"
        reader "(ns fixture.reader-test (:require [fixture.middle]))"
        middle "(ns fixture.middle (:require [fixture.helper]))"]
    (mapv (fn [[id extra path expected]]
            {:id id :files (merge base-files extra)
             :changes {path (if (= path source)
                              "(ns fixture.unrequired) (def kind :new)" "{:new true}")}
             :expected expected})
          [[:src-constant {data "{}"
                           "src/fixture/helper.clj" "(ns fixture.helper) (def path \"resources/registry.edn\")"
                           "src/fixture/middle.clj" middle
                           "test/fixture/reader_test.clj" reader} data '#{fixture.reader-test}]
           [:edn-config {data "{}" "resources/config.edn" "{:path \"resources/registry.edn\"}"
                         "test/fixture/reader_test.clj" "(ns fixture.reader-test) (slurp (:path (edn/read-string (slurp \"resources/config.edn\"))))"} data #{}]
           [:io-resource {data "{}" "test/fixture/reader_test.clj"
                          "(ns fixture.reader-test) (slurp (io/resource \"registry.edn\"))"} data #{}]
           [:test-scan {"test/fixture/helper.clj" "(ns fixture.helper) (defn scan [] (map slurp (file-seq (io/file \"src/fixture\"))))"
                        "test/fixture/middle.clj" middle
                        "test/fixture/reader_test.clj" reader} source '#{fixture.reader-test}]
           [:src-scan {"src/fixture/helper.clj" "(ns fixture.helper) (defn scan [] (map slurp (file-seq (io/file \"src/fixture\"))))"
                       "src/fixture/middle.clj" middle
                       "test/fixture/reader_test.clj" reader} source '#{fixture.reader-test}]
           [:makefile {"Makefile" "all:\n"} "Makefile" #{}]])))

;; @spec DIFF-IMPACT-004
;; @spec DIFF-IMPACT-006
(deftest fixed-point-unmatched-files-must-refuse-or-fallback
  (doseq [{:keys [id files changes expected]} round-two-probes]
    (testing (name id)
      (let [r (run-fixture files changes "list")]
        (is (= 0 (:exit r)))
        (is (= expected (selected-set r)))
        (when (empty? expected)
          (is (= :hold-unmatched-files (get-in r [:inventory :status])))))
      ;; Remove endpoints, retaining the exact changed-file inventory and helpers.
      ;; Every non-list mode must hold; no fixture subject can be launched.
      (doseq [mode ["before" "after" "merged" "fixed-point"]]
        (let [r (run-fixture (into {} (remove #(str/ends-with? (key %) "_test.clj") files)) changes mode)]
          (is (= 1 (:exit r)) (pr-str [id mode r]))
          (is (= :hold-unmatched-files (get-in r [:results :status])))
          (is (= :unmatched-files (get-in r [:results :reason])))
          (is (= (set (keys changes)) (set (map :file (get-in r [:results :unmatched-files]))))))))))

;; @spec DIFF-IMPACT-004
(deftest mixed-selection-must-hold-before-launch
  (doseq [mode ["before" "after" "merged" "fixed-point"]]
    (let [r (run-fixture (assoc base-files "Makefile" "old")
              {"Makefile" "new" "src/fixture/core.clj" "(ns fixture.core) (def value 2)"} mode)]
      (is (= 1 (:exit r)))
      (is (= '#{fixture.control-test} (selected-set r)))
      (is (= :hold-unmatched-files (get-in r [:results :status])))
      (is (= [{:file "Makefile" :reason :no-dependency-edge}]
             (get-in r [:results :unmatched-files])))
      (is (str/includes? (:out r) ":reason :unmatched-files")))))

;; @spec DIFF-IMPACT-004
(deftest no-test-can-depend-allowlist-is-explicit-and-empty
  (is (= [] (edn/read-string (slurp "docs/intent/diff-impact/no-test-can-depend.edn")))))

;; @spec DIFF-IMPACT-004
;; @spec DIFF-IMPACT-006
(deftest src-content-closure-bounds-and-unreachable-reasons
  (let [nodes [{:namespace 'a :file "src/a.clj" :requires #{'b}
                :content-edges [{:file "resources/x.edn" :edge-kind :data-file}]}
               {:namespace 'b :file "src/b.clj" :requires #{'a}}
               {:namespace 'reader-test :test? true :requires #{'b}}
               {:namespace 'unrelated-test :test? true :requires #{}}]]
    (is (= '#{reader-test} (set (map :namespace (:namespaces (impact/select-impact nodes ["resources/x.edn"]))))))
    (let [r (impact/select-impact (assoc-in nodes [2 :requires] #{}) ["resources/x.edn" "src/a.clj"])]
      (is (= [] (:namespaces r)))
      (is (= [{:file "resources/x.edn" :reason :no-dependency-edge}
              {:file "src/a.clj" :reason :no-dependency-edge}] (:unmatched-files r))))))

;; @spec DIFF-IMPACT-006
(deftest src-path-constant-can-name-any-existing-repo-file
  (doseq [path ["Makefile" "config/settings.edn"]]
    (let [r (run-fixture
              (assoc base-files path "old"
                     "src/fixture/paths.clj" (str "(ns fixture.paths) (def path " (pr-str path) ")")
                     "test/fixture/reader_test.clj" "(ns fixture.reader-test (:require [fixture.paths]))")
              {path "new"} "list")]
      (is (= 0 (:exit r)))
      (is (= '#{fixture.reader-test} (selected-set r))))))
