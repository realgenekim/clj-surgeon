(ns clj-surgeon.study-test
  "Kernel witnesses for the read-only study operations.

   @spec MCP-OP-STUDY-002 MCP-OP-STUDY-003 MCP-OP-STUDY-004
   @spec MCP-OP-STUDY-005 MCP-OP-STUDY-008 MCP-OP-STUDY-009"
  (:require
   [babashka.fs :as fs]
   [babashka.process :as process]
   [clj-surgeon.core :as core]
   [clj-surgeon.study :as study]
   [clojure.string :as str]
   [clojure.test :refer [deftest is testing]]))

(def ^:private real-file "src/clj_surgeon/analyze.clj")
(def ^:private fixture-dir "test-fixtures/cljc/existing-ops")
(def ^:private golden-file "test-fixtures/study/ls-tree-existing-ops.golden.txt")
(def ^:private edn-golden-file
  "test-fixtures/study/ls-tree-existing-ops-edn.golden.txt")
(def ^:private refusal-golden-file
  "test-fixtures/study/ls-tree-no-clojure-files.golden.txt")

;; ============================================================
;; The kernel takes data and returns data
;; ============================================================

;; @spec MCP-OP-STUDY-002
(deftest deps-kernel-returns-the-call-graph-and-one-owner-row
  (let [source (slurp real-file)
        rows (study/deps source {})]
    (is (vector? rows))
    (is (seq rows))
    (is (every? #(and (string? (:name %)) (set? (:depends-on %))) rows))
    (testing "one owner"
      (let [row (study/deps source {:form "dep-tree"})]
        (is (= "dep-tree" (:name row)))
        (is (= "defn" (:type row)))))
    (testing "an absent owner is nil, never an invented row"
      (is (nil? (study/deps source {:form "no-such-form"}))))))

;; @spec MCP-OP-STUDY-003
(deftest topo-kernel-reports-order-and-cycles
  (let [result (study/topo (slurp real-file))]
    (is (vector? (:sorted result)))
    (is (vector? (:cycles result)))
    (is (contains? result :has-cycles?))))

;; @spec MCP-OP-STUDY-004
(deftest ls-deps-kernel-returns-a-tree-or-nil
  (let [source (slurp real-file)]
    (is (= "extraction-closure"
           (:name (study/ls-deps source {:form "extraction-closure"}))))
    (is (nil? (study/ls-deps source {:form "no-such-form"})))))

;; @spec MCP-OP-STUDY-005
(deftest ls-extract-kernel-returns-the-minimal-closure
  (let [result (study/ls-extract (slurp real-file)
                                 {:form "extraction-closure"})]
    (is (= "extraction-closure" (:target result)))
    (is (= ["extraction-closure"] (mapv :name (:forms result))))))

;; @spec MCP-OP-STUDY-001
(deftest ls-tree-kernel-returns-typed-refusals-without-printing
  (testing "a missing dir is data, not a println"
    (let [result (study/ls-tree {})]
      (is (false? (:ok result)))
      (is (= :missing-dir (:error-type result)))
      (is (= "Error: :dir is required for :ls-tree" (:error result)))))
  (testing "a directory with no Clojure sources refuses with its message"
    (let [result (study/ls-tree {:dir "docs/intent/study-ops"})]
      (is (false? (:ok result)))
      (is (= :no-clojure-files (:error-type result)))
      (is (str/starts-with? (:error result) "No Clojure files found under "))))
  (testing "a real tree discovers files without parsing any of them"
    (let [result (study/ls-tree {:dir fixture-dir})]
      (is (true? (:ok result)))
      (is (= 7 (:file-count result)))
      (is (= 7 (study/total-file-count (:projects result))))
      (is (every? #(nil? (:outlines %)) (:projects result))
          "discovery lists names; outlining is the separate bounded step")
      (is (= 7 (reduce + 0 (map #(count (:outlines %))
                                (study/outline-all (:projects result)))))))))

;; @spec MCP-OP-STUDY-001
(deftest ls-tree-refuses-a-flag-shaped-grep-or-ns-grep-pattern
  ;; A pattern beginning with '-' would otherwise reach rg/grep looking like
  ;; a flag (e.g. "--pre=/bin/sh" runs an arbitrary preprocessor command).
  ;; Refused before any subprocess runs, never silently reinterpreted.
  (testing "grep"
    (let [result (study/ls-tree {:dir fixture-dir :grep "--pre=/bin/sh"})]
      (is (false? (:ok result)))
      (is (= :invalid-grep-pattern (:error-type result)))
      (is (str/includes? (:error result) "must not start with"))))
  (testing "ns-grep"
    (let [result (study/ls-tree {:dir fixture-dir :ns-grep "-x"})]
      (is (false? (:ok result)))
      (is (= :invalid-ns-grep-pattern (:error-type result)))
      (is (str/includes? (:error result) "must not start with")))))

;; @spec MCP-OP-STUDY-001
(deftest grep-tree-argv-always-separates-the-pattern-with-double-dash
  ;; Defense in depth alongside the leading-dash refusal above: even a
  ;; pattern that is not flag-shaped must never be adjacent to an
  ;; unseparated argv, so a caller cannot smuggle an rg/grep flag by any
  ;; other means. Captures the real subprocess argv via with-redefs rather
  ;; than asserting on live rg/grep output.
  (let [captured (atom nil)
        probes (atom 0)
        grep-tree #'study/grep-tree]
    (with-redefs [babashka.process/shell
                  (fn [_opts & args]
                    (if (= ["rg" "--version"] (vec args))
                      (do (swap! probes inc)
                          {:exit 0 :out "ripgrep 14.0.0" :err ""})
                      (do (reset! captured (vec args))
                          {:exit 1 :out "" :err ""})))]
      (grep-tree "some-pattern" "/tmp/somewhere"))
    (is (some? @captured) "the real rg invocation must have been captured")
    (let [args @captured
          dash-dash-idx (.indexOf ^java.util.List args "--")
          pattern-idx (.indexOf ^java.util.List args "some-pattern")]
      (is (not= -1 dash-dash-idx))
      (is (= (inc dash-dash-idx) pattern-idx)
          "the pattern must immediately follow the -- argv separator"))
    ;; The availability probe was called twice — once to decide whether to
    ;; warn, once to build the argv — so every scan spawned `rg --version`
    ;; twice before doing any work.
    (is (= 1 @probes)
        "ripgrep availability must be probed once per scan, not once per use")))

;; ============================================================
;; Reading a build file is a read, never an evaluation
;; ============================================================

(defn- with-temp-dir
  [f]
  (let [dir (fs/create-temp-dir {:prefix "clj-surgeon-study-test"})]
    (try (f dir) (finally (fs/delete-tree dir)))))

;; @spec MCP-OP-STUDY-013
(deftest reading-a-build-file-never-evaluates-it
  ;; Executed against the branch bytes before this fix: discovery read every
  ;; deps.edn/bb.edn/project.clj in the scanned tree with
  ;; `clojure.core/read-string` and `*read-eval*` at its ambient true, so a
  ;; `#=(clojure.core/spit …)` form anywhere in any of those files RAN as the
  ;; scanning process — and the silent `(catch Exception _e ["src"])` hid it.
  (with-temp-dir
    (fn [dir]
      (let [extract #'study/extract-source-paths
            marker (str (fs/path dir "PWNED"))
            evil (str "#=(clojure.core/spit \"" marker "\" \"x\")")]
        (doseq [[filename source]
                [["deps.edn" (str "{:paths [\"src\"]\n :evil " evil "}")]
                 ["bb.edn" (str "{:paths [\"src\"]\n :evil " evil "}")]
                 ["project.clj" (str "(defproject demo \"0.1.0\"\n"
                                     "  :source-paths [\"src\"]\n"
                                     "  :evil " evil ")")]]]
          (testing filename
            (let [build-file (fs/path dir filename)]
              (spit (str build-file) source)
              (is (= ["src"] (extract build-file))
                  "an unreadable build file falls back to the documented default")
              (is (not (fs/exists? marker))
                  "reading a build file must never write one")
              (fs/delete-if-exists build-file))))))))

;; @spec MCP-OP-STUDY-013
(deftest the-safe-reader-still-answers-the-source-paths-question
  ;; The eval-free reader must still parse the declarations the unsafe one
  ;; parsed, or the fix would have bought safety with a silent scan regression.
  (with-temp-dir
    (fn [dir]
      (let [extract #'study/extract-source-paths]
        (doseq [[filename source expected]
                [["deps.edn" "{:paths [\"lib\" \"src\"] :deps {}}" ["lib" "src"]]
                 ["bb.edn" "{:paths [\"scripts\"]}" ["scripts"]]
                 ["deps.edn" "{:deps {}}" ["src"]]
                 ["project.clj"
                  "(defproject demo \"0.1.0\" :source-paths [\"s1\" \"s2\"])"
                  ["s1" "s2"]]]]
          (testing (str filename " " source)
            (let [build-file (fs/path dir filename)]
              (spit (str build-file) source)
              (is (= expected (extract build-file)))
              (fs/delete-if-exists build-file))))))))

;; ============================================================
;; Discovery is confined to the canonical scan root
;; ============================================================

(defn- build-confinement-fixture!
  "A scan root holding exactly one real source file, plus the two escapes the
   red team executed: a .clj SYMLINK whose target is outside the root, and a
   sibling project whose deps.edn declares an unnormalized parent-traversal
   source path. `<tmp>/a/b` is the scan root, so the traversal escape lands on
   `<tmp>` — small and bounded — rather than on `/` or `/tmp`."
  [tmp]
  (let [scan-root (str (fs/path tmp "a" "b"))]
    (fs/create-dirs (str (fs/path scan-root "proj" "src")))
    (fs/create-dirs (str (fs/path scan-root "escape")))
    (spit (str (fs/path scan-root "proj" "deps.edn")) "{:paths [\"src\"]}")
    (spit (str (fs/path scan-root "proj" "src" "real.clj"))
          "(ns real)\n(defn only-file [] :ok)")
    (fs/create-sym-link (str (fs/path scan-root "proj" "src" "leak.clj"))
                        "/etc/passwd")
    ;; `<scan-root>/escape/../../..` normalizes to <tmp>
    (spit (str (fs/path scan-root "escape" "deps.edn")) "{:paths [\"../../..\"]}")
    (spit (str (fs/path tmp "decoy.clj")) "(ns decoy)\n(defn decoy-fn [] :no)")
    scan-root))

;; @spec MCP-OP-STUDY-014
(deftest ls-tree-kernel-drops-paths-that-resolve-outside-the-scan-root
  ;; Both escapes executed against the branch bytes: `find` reports a symlink
  ;; by the LINK's own name, so `src/leak.clj -> /etc/passwd` matched
  ;; `-name '*.clj'` and was outlined (slurped); and `(fs/path root "../../..")`
  ;; was NOT normalized, so a scanned deps.edn could move discovery outside the
  ;; root entirely.
  (with-temp-dir
    (fn [tmp]
      (let [scan-root (build-confinement-fixture! tmp)
            result (study/ls-tree {:dir scan-root})
            files (mapcat :files (:projects result))]
        (is (true? (:ok result)))
        (is (= 1 (count files))
            "exactly the one real source file inside the scan root")
        (is (str/ends-with? (first files) "/proj/src/real.clj"))
        (is (not-any? #(str/includes? % "leak") files)
            "a .clj symlink whose realpath is outside the root must be dropped")
        (is (not-any? #(str/includes? % "decoy") files)
            "an unnormalized :paths traversal must not move the scan out")))))

;; ============================================================
;; One kernel: the CLI handler adds print, never a second answer
;; ============================================================

;; @spec MCP-OP-STUDY-008
(deftest cli-handlers-are-the-kernel-plus-nothing
  (let [source (slurp real-file)]
    (is (= (study/deps source {}) (core/run-deps {:file real-file})))
    (is (= (study/deps source {:form "dep-tree"})
           (core/run-deps {:file real-file :form "dep-tree"})))
    (is (= (study/topo source) (core/run-topo {:file real-file})))
    (is (= (study/ls-deps source {:form "extraction-closure"})
           (core/run-ls-deps {:file real-file :form "extraction-closure"})))
    (is (= (study/ls-extract source {:form "extraction-closure"})
           (core/run-closure {:file real-file :form "extraction-closure"})))))

;; @spec MCP-OP-STUDY-008
(deftest cli-ls-tree-bytes-match-the-frozen-golden
  (let [result (process/shell {:out :string :err :string :continue true}
                              "bb" "-m" "clj-surgeon.core"
                              ":op" ":ls-tree" ":dir" fixture-dir)]
    (is (zero? (:exit result)))
    (is (= (slurp golden-file) (:out result)))))

;; @spec MCP-OP-STUDY-008
(deftest cli-ls-tree-edn-bytes-match-the-frozen-golden
  ;; The golden covered only the default text path, so `run-ls-tree`'s :edn
  ;; branch and its refusal branch — the two places its `format` destructuring
  ;; actually mattered — were unfrozen.
  (let [result (process/shell {:out :string :err :string :continue true}
                              "bb" "-m" "clj-surgeon.core"
                              ":op" ":ls-tree" ":dir" fixture-dir
                              ":format" ":edn")]
    (is (zero? (:exit result)))
    (is (= (slurp edn-golden-file) (:out result)))))

;; @spec MCP-OP-STUDY-008
;; @spec MCP-OP-STUDY-026
(deftest cli-ls-tree-refusal-bytes-match-the-frozen-golden
  ;; The refusal used to name the canonical scanned directory — a
  ;; machine-specific absolute path — so the golden had to normalize the
  ;; workspace root away. It now names the directory the CALLER asked for, so
  ;; the bytes are the same on every machine and the golden is frozen whole.
  (let [result (process/shell {:out :string :err :string :continue true}
                              "bb" "-m" "clj-surgeon.core"
                              ":op" ":ls-tree" ":dir" "docs/intent/study-ops")]
    (is (= 1 (:exit result)))
    (is (= (slurp refusal-golden-file) (:out result)))
    (is (not (str/includes? (:out result) (System/getProperty "user.dir")))
        "a refusal message must not publish where the workspace lives")))

;; @spec MCP-OP-STUDY-008
;; @spec MCP-OP-STUDY-030
(deftest cli-ls-tree-prunes-every-directory-named-target-including-a-source-one
  ;; `-prune` matches `target` by NAME at any depth. `target/foo.clj` is
  ;; compiled output and correctly absent; `src/app/target/bar.clj` is a real
  ;; source namespace and is absent too — a known limitation that had no
  ;; witness at all. The golden freezes BOTH, so the day the rule becomes
  ;; path-anchored this test changes and the note in `skip-dirs` is read.
  (let [result (process/shell {:out :string :err :string :continue true}
                              "bb" "-m" "clj-surgeon.core"
                              ":op" ":ls-tree"
                              ":dir" "test-fixtures/study/prune-target")]
    (is (zero? (:exit result)))
    (is (= (slurp "test-fixtures/study/ls-tree-prune-target.golden.txt")
           (:out result)))
    (is (str/includes? (:out result) "src/app/core.clj"))
    (is (not (str/includes? (:out result) "target/foo.clj"))
        "compiled output must never be walked")
    (is (not (str/includes? (:out result) "src/app/target/bar.clj"))
        "and the same rule hides a source namespace: the documented limitation")))

;; ============================================================
;; The format shadow that made a documented refusal unreachable
;; ============================================================

;; @spec MCP-OP-STUDY-001
(deftest ls-tree-refusal-message-is-reachable
  ;; Regression: run-ls-tree destructured `format` as a local, shadowing
  ;; clojure.core/format, so the "No Clojure files found" branch threw a
  ;; NullPointerException and the CLI reported {:error nil}.
  (let [result (process/shell {:out :string :err :string :continue true}
                              "bb" "-m" "clj-surgeon.core"
                              ":op" ":ls-tree" ":dir" "docs/intent/study-ops")]
    (is (= 1 (:exit result)))
    (is (str/starts-with? (:out result) "No Clojure files found under "))
    (is (not (str/includes? (:out result) ":error nil")))))

;; ============================================================
;; No write authority
;; ============================================================

;; @spec MCP-OP-STUDY-009
(deftest the-study-kernel-exposes-no-write-operation
  (let [public-names (set (map name (keys (ns-publics 'clj-surgeon.study))))]
    (is (empty? (filter #(str/ends-with? % "!") public-names))
        "a bang-suffixed public in the study kernel would be a write reachable from the read entrance")
    (is (empty? (filter #{"mv" "rename-ns" "fix-declares"} public-names)))))
