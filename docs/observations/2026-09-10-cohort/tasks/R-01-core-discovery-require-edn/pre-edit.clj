(ns clj-surgeon.core-discovery-test
  "Shell-safety and NUL-delimited witnesses for project discovery.

   Andon pull, 2026-09-03. `clj-surgeon.core/find-build-files` interpolated the
   caller-supplied `:dir` (equivalently `workspace_root` on any entrance that
   reaches project discovery) into a command STRING and ran it through
   `sh -c`. Any `:dir` carrying `;`, `$(...)`, backticks, or `&&` executed
   arbitrary commands as the surgeon process. These witnesses assert the
   observable outcome — a canary file that must never come into existence —
   rather than the implementation detail of how `find` is invoked.

   The same defect had a second, quieter face: the output of `find` was split
   with `str/split-lines`, so a project directory whose name contains a newline
   was silently dropped from discovery. `-print0` plus a NUL split fixes both
   the injection and the drop, so both are witnessed here."
  (:require
   [babashka.fs :as fs]
   [babashka.process :as proc]
   [clj-surgeon.core :as core]
   [clojure.string :as str]
   [clojure.test :refer [deftest is testing]]
   [rewrite-clj.node :as rn]
   [rewrite-clj.parser :as rp]))

(def ^:private find-build-files
  "Private discovery helper under test; the reviewer's direct reproduction."
  #'clj-surgeon.core/find-build-files)

(defn- fresh-sandbox
  "Create and return a unique sandbox directory for one witness.

   `fs/create-temp-dir` and not a fixed root: this namespace runs in BOTH
   `test/run_all.clj` and `test/clj_surgeon/mcp_test_runner.clj`, which the
   fleet runs concurrently, and a fixed `/tmp/<name>` root is owned by whoever
   created it first — on a shared box that breaks the suite for every other
   user with no code change. The repo idiom is `create-temp-dir` (55 sites)."
  []
  (fs/create-temp-dir {:prefix "andon-shell-safety"}))

(defn- make-project!
  "Create a minimal deps.edn project named `name` under `root`."
  [root name]
  (let [project (fs/path root name)]
    (fs/create-dirs (fs/path project "src"))
    (spit (str (fs/path project "deps.edn")) "{:paths [\"src\"]}\n")
    (spit (str (fs/path project "src" "core.clj"))
          "(ns core)\n(defn f [] 1)\n")
    project))

(defn- run-cli-ls-tree
  "The `:ls-tree` op through the REAL CLI, in a babashka subprocess, returning
   `{:exit :out :err}`.

   A subprocess and not an in-process call: the empty-result branch of
   `run-ls-tree` calls `(System/exit 1)`, which would take this suite down with
   it (that exit is pre-existing and owed separately — inb-eca3b1). Testing
   delivery, not identity."
  [& args]
  (let [src (str (fs/absolutize "src"))]
    (apply proc/shell {:out :string :err :string :continue true}
           "bb" "-cp" src "-m" "clj-surgeon.core" ":op" "ls-tree" args)))

;; @spec MCP-OP-SHELL-ARGV-001
(deftest hostile-dir-never-reaches-a-shell-from-find-build-files
  (testing "a `;`-separated command in :dir does not execute"
    (let [sandbox (fresh-sandbox)]
      (try
        (make-project! sandbox "H")
        (let [canary  (str (fs/path sandbox "PWNED-SEMICOLON"))
              hostile (str (fs/path sandbox "H") "; touch " canary " ; echo z")
              result  (find-build-files hostile)]
          (is (not (fs/exists? canary))
              "canary must not exist: caller-supplied :dir must never reach a shell")
          (is (= [] result)
              ":dir that is not an existing directory yields no build files"))
        (finally (fs/delete-tree sandbox)))))

  (testing "a `$(...)` substitution in :dir does not execute"
    (let [sandbox (fresh-sandbox)]
      (try
        (make-project! sandbox "H")
        (let [canary  (str (fs/path sandbox "PWNED-SUBST"))
              hostile (str (fs/path sandbox "H") "$(touch " canary ")")
              result  (find-build-files hostile)]
          (is (not (fs/exists? canary))
              "canary must not exist: caller-supplied :dir must never reach a shell")
          (is (= [] result)
              ":dir that is not an existing directory yields no build files"))
        (finally (fs/delete-tree sandbox))))))

;; @spec MCP-OP-SHELL-ARGV-002
(deftest ls-tree-entrance-refuses-a-non-directory-root-without-executing-it
  (testing "end-to-end through the public :ls-tree op — semicolon"
    (let [sandbox (fresh-sandbox)]
      (try
        (make-project! sandbox "H")
        (let [canary  (str (fs/path sandbox "PWNED-E2E-SEMICOLON"))
              hostile (str (fs/path sandbox "H") "; touch " canary " ; echo z")
              ;; Catch so the canary assertion is reported independently of any
              ;; downstream throw; a green run never throws here.
              result  (try (core/run-ls-tree {:dir hostile :format :edn})
                           (catch Exception t {:threw (.getMessage t)}))]
          (is (not (fs/exists? canary))
              "canary must not exist: the :ls-tree entrance must never shell out on :dir")
          (is (= :workspace-root-not-a-directory (:error-type result))
              ":ls-tree must return a typed refusal for a non-directory root"))
        (finally (fs/delete-tree sandbox)))))

  (testing "end-to-end through the public :ls-tree op — command substitution"
    (let [sandbox (fresh-sandbox)]
      (try
        (make-project! sandbox "H")
        (let [canary  (str (fs/path sandbox "PWNED-E2E-SUBST"))
              hostile (str (fs/path sandbox "H") "$(touch " canary ")")
              result  (try (core/run-ls-tree {:dir hostile :format :edn})
                           (catch Exception t {:threw (.getMessage t)}))]
          (is (not (fs/exists? canary))
              "canary must not exist: the :ls-tree entrance must never shell out on :dir")
          (is (= :workspace-root-not-a-directory (:error-type result))
              ":ls-tree must return a typed refusal for a non-directory root"))
        (finally (fs/delete-tree sandbox))))))

;; @spec MCP-OP-SHELL-ARGV-003
(deftest project-directories-containing-a-newline-are-discovered
  (testing "a mixed tree with an ordinary project and a newline-named project"
    (let [sandbox (fresh-sandbox)]
      (try
        (make-project! sandbox "ok")
        (make-project! sandbox "b\nad")
        (is (= 2 (count (find-build-files (str sandbox))))
            "both build files are found; a newline in a path is data, not a separator")
        (let [result (core/run-ls-tree {:dir (str sandbox) :format :edn})
              files  (set (map :file result))]
          (is (contains? files "ok/src/core.clj")
              "the ordinary project is discovered")
          (is (contains? files "b\nad/src/core.clj")
              "the newline-named project is discovered too"))
        (finally (fs/delete-tree sandbox)))))

  (testing "the :grep fast path is the same external file-discovery command"
    (let [sandbox (fresh-sandbox)]
      (try
        (make-project! sandbox "ok")
        (make-project! sandbox "b\nad")
        (let [result (core/run-ls-tree {:dir (str sandbox) :format :edn :grep "defn"})
              files  (set (map :file result))]
          (is (contains? files "ok/src/core.clj")
              "the ordinary project is discovered through rg/grep")
          (is (contains? files "b\nad/src/core.clj")
              "the newline-named project is discovered through rg/grep too"))
        (finally (fs/delete-tree sandbox))))))

;; @spec MCP-OP-SHELL-ARGV-002
(deftest an-empty-scan-names-what-it-searched-instead-of-throwing
  (testing "a directory holding no Clojure file"
    (let [sandbox (fresh-sandbox)]
      (try
        (let [{:keys [exit out err]} (run-cli-ls-tree ":dir" (str sandbox)
                                                      ":format" ":edn")
              both (str out err)]
          (is (= 1 exit)
              (str "an empty scan is a failed scan, not a crash; stderr: " err))
          (is (str/includes? out "No Clojure files found under")
              "it says what it did not find")
          (is (str/includes? out (str sandbox))
              "and names the directory it searched")
          (is (not (str/includes? both "Wrong number of args"))
              "the empty branch must not call its :format value as a function:
               `run-ls-tree` destructured a binding named `format`, shadowing
               clojure.core/format, so the message call became (:edn s d g)")
          (is (not (str/includes? out ":invalid-arguments"))
              "an empty scan is not an argument error; the shadowed-`format`
               throw was caught at the CLI top level and rendered as one"))
        (finally (fs/delete-tree sandbox)))))

  (testing "a :grep that matches nothing names the pattern too"
    (let [sandbox (fresh-sandbox)]
      (try
        (make-project! sandbox "ok")
        (let [{:keys [exit out err]} (run-cli-ls-tree ":dir" (str sandbox)
                                                      ":grep" "zzz-no-such-symbol")
              both (str out err)]
          (is (= 1 exit) (str "stderr: " err))
          (is (str/includes? out "matching 'zzz-no-such-symbol'")
              "a scan narrowed by :grep says which narrowing found nothing")
          (is (not (str/includes? both "Wrong number of args")))
          (is (not (str/includes? out ":invalid-arguments"))))
        (finally (fs/delete-tree sandbox))))))

;; @spec MCP-OP-SHELL-ARGV-002
(deftest a-symlinked-root-is-descended-just-like-its-target
  (testing "the gate and the executor must agree on what a directory is"
    (let [sandbox (fresh-sandbox)]
      (try
        ;; NOT named "target": that name is in `skip-dirs` and would be
        ;; pruned, making the comparison vacuously 0 = 0.
        (make-project! sandbox "proj")
        (let [target (str (fs/path sandbox "proj"))
              link   (str (fs/path sandbox "link"))]
          (fs/create-sym-link link target)
          (is (= (count (find-build-files target))
                 (count (find-build-files link)))
              "`existing-directory?` follows symlinks (Files.isDirectory) but
               `find` under its -P default does not descend a symlinked START
               POINT, so the entrance accepted the root and discovery silently
               found nothing")
          (let [linked  (run-cli-ls-tree ":dir" link ":format" ":edn")
                control (run-cli-ls-tree ":dir" target ":format" ":edn")]
            (is (= 0 (:exit linked))
                (str "a root the entrance accepted must not scan empty; stdout: "
                     (:out linked) " stderr: " (:err linked)))
            (is (str/includes? (:out linked) "src/core.clj")
                "the symlinked root discovers the file its target discovers")
            ;; MEM-005 (bridge/parser-admission) added a MEASURED `scan_ms` to
            ;; the ls-tree receipt, so two runs of the same scan never produce
            ;; byte-identical output. The discovery claim is about what was
            ;; found, not how long it took: the wall-clock reading is masked and
            ;; asserted separately, so everything else -- projects, files, forms,
            ;; and `bytes_scanned` -- is still compared byte for byte.
            (let [mask #(str/replace % #":scan_ms [0-9.]+" ":scan_ms <measured>")]
              (doseq [[what out] [["control" (:out control)] ["linked" (:out linked)]]]
                (is (re-find #":scan_ms [0-9.]+" out)
                    (str "the " what " receipt still charges the scan")))
              (is (= (mask (:out control)) (mask (:out linked)))
                  "and discovers exactly the same projects and files"))))
        (finally (fs/delete-tree sandbox))))))

;; ============================================================
;; The CLASS-level ratchet: no shell interpreter anywhere under src/
;;
;; The two canary witnesses above pin `find-build-files` only, and the intent
;; audit (`mcp-intent-contract/audit-contract`) is marker-PRESENCE: the
;; reviewer reintroduced a `format` + `sh -c` discovery site in `src/` with
;; every @spec marker intact and the audit stayed `OK= true, violations= []`.
;; MCP-OP-SHELL-ARGV-001's promise is structural — "there is no shell" — so
;; this scans the source itself and fails on a NEW site, not just the old one.
;; ============================================================

(def ^:private shell-interpreter-literals
  "Program names that are a shell interpreter."
  #{"sh" "bash" "zsh" "dash" "/bin/sh" "/bin/bash" "/bin/zsh" "/usr/bin/env"})

(def ^:private process-spawning-fns
  "Unqualified names of fns whose first non-option argument is the program."
  #{"shell" "sh" "process" "exec"})

(def ^:private string-building-fns
  "A command string built from data is the exact shape of the Andon defect."
  #{"format" "str" "join" "print-str" "cl-format"})

(defn- sexpr-of
  "The node's sexpr, or ::none when it has none (reader macros, whitespace)."
  [node]
  (try (if (rn/sexpr-able? node) (rn/sexpr node) ::none)
       (catch Exception _e ::none)))

(defn- kids
  "Child nodes, or nil for a leaf (`rn/children` throws on one)."
  [node]
  (when (rn/inner? node) (rn/children node)))

(defn- code-children
  "Children of a node with whitespace and comments removed."
  [node]
  (remove rn/whitespace-or-comment? (kids node)))

(defn- head-name
  "The unqualified name of a form's head symbol, or nil."
  [node]
  (let [h (first (code-children node))
        s (some-> h sexpr-of)]
    (when (symbol? s) (name s))))

(defn- excerpt
  [node]
  (let [s (str/replace (str node) #"\s+" " ")]
    (if (> (count s) 160) (str (subs s 0 160) " ...") s)))

(defn- adjacent-shell-c-violation
  "`\"sh\" \"-c\"` (or bash/zsh, or an absolute /bin/... path) as two adjacent
   literals in one form: an argv that hands a command STRING to a shell."
  [node]
  (let [vals (mapv sexpr-of (code-children node))]
    (when (some (fn [i]
                  (and (contains? shell-interpreter-literals (nth vals i))
                       (= "-c" (nth vals (inc i)))))
                (range (max 0 (dec (count vals)))))
      (str "argv literal hands a command string to a shell interpreter: "
           (excerpt node)))))

(defn- spawn-first-argument-violation
  "A process-spawning call whose PROGRAM argument is a shell interpreter, or a
   string built by `format`/`str` at the call site."
  [node]
  (let [children (code-children node)
        head (head-name node)
        [head children] (if (= "apply" head)
                          [(some-> (second children) sexpr-of
                                   (as-> s (when (symbol? s) (name s))))
                           (drop 2 children)]
                          [head (rest children)])
        ;; option maps (`{:out :string ...}`) precede the program
        args (drop-while #(= :map (rn/tag %)) children)
        program (first args)]
    (when (and (contains? process-spawning-fns head) program)
      (let [s (sexpr-of program)]
        (cond
          (contains? shell-interpreter-literals s)
          (str "process-spawning call names a shell interpreter as its program: "
               (excerpt node))

          (and (= :list (rn/tag program))
               (contains? string-building-fns (head-name program)))
          (str "process-spawning call builds its command string at the call "
               "site (" (head-name program) "); the program must be a literal "
               "and every caller value its own argv token: " (excerpt node)))))))

(defn- absolute-shell-path-violation
  [node]
  (let [s (sexpr-of node)]
    (when (and (string? s) (str/starts-with? s "/bin/")
               (contains? shell-interpreter-literals s))
      (str "absolute shell-interpreter path literal: " (pr-str s)))))

(defn shell-argv-violations
  "Scan every Clojure source under `root` and return
   [{:file <path> :violation <message>} ...]. Empty means the promise holds."
  [root]
  (->> (fs/glob root "**{.clj,.cljc,.cljs}")
       (sort-by str)
       (mapcat
        (fn [file]
          (let [nodes (tree-seq #(seq (kids %)) kids
                                (rp/parse-string-all (slurp (str file))))]
            (->> nodes
                 (mapcat (juxt adjacent-shell-c-violation
                               spawn-first-argument-violation
                               absolute-shell-path-violation))
                 (remove nil?)
                 distinct
                 (map (fn [v] {:file (str file) :violation v}))))))
       vec))

;; @spec MCP-OP-SHELL-ARGV-001
(deftest no-source-file-hands-a-command-string-to-a-shell
  (testing "every Clojure source under src/"
    (let [violations (shell-argv-violations "src")]
      (is (= [] violations)
          (str "MCP-OP-SHELL-ARGV-001 is structural: no file under src/ may "
               "invoke a shell interpreter or build a command string at a "
               "process-spawning call site. Found:\n"
               (str/join "\n" (map (fn [{:keys [file violation]}]
                                     (str "  " file ": " violation))
                                   violations)))))))

;; @spec MCP-OP-SHELL-ARGV-001
(deftest the-source-scan-is-not-vacuous
  (testing "the scanner sees the exact shape the Andon cord was pulled on"
    (let [sandbox (fresh-sandbox)
          bad (fs/path sandbox "bad.clj")]
      (try
        (spit (str bad)
              (str "(ns bad)\n"
                   "(defn- find-build-files-legacy [dir]\n"
                   "  (babashka.process/shell {:out :string :continue true}\n"
                   "    \"sh\" \"-c\" (format \"find %s -name deps.edn -print\" (str dir))))\n"))
        (let [found (shell-argv-violations (str sandbox))]
          (is (seq found) "the reintroduced site must be seen")
          ;; both detectors fire on this shape, and that is correct: it is a
          ;; literal `sh -c` argv AND a command string built at the call site.
          (is (some #(str/includes? (:violation %) "argv literal") found))
          (is (some #(str/includes? (:violation %)
                                    "names a shell interpreter as its program")
                    found)))
        (finally (fs/delete-tree sandbox)))))

  (testing "and the format-built-command shape on its own, with no literal sh"
    (let [sandbox (fresh-sandbox)
          bad (fs/path sandbox "bad2.clj")]
      (try
        (spit (str bad)
              (str "(ns bad2)\n"
                   "(defn- run [dir]\n"
                   "  (babashka.process/sh (format \"find %s\" (str dir))))\n"))
        (let [found (shell-argv-violations (str sandbox))]
          (is (= 1 (count found)) (pr-str found))
          (is (str/includes? (:violation (first found))
                             "builds its command string at the call site")))
        (finally (fs/delete-tree sandbox)))))

  (testing "a clean argv vector is NOT flagged"
    (let [sandbox (fresh-sandbox)
          ok (fs/path sandbox "ok.clj")]
      (try
        (spit (str ok)
              (str "(ns ok)\n"
                   "(defn- run [dir]\n"
                   "  (babashka.process/shell {:out :string} \"find\" (str dir) \"-name\" \"deps.edn\"))\n"
                   "(defn- guard [argv] (not-any? #(or (= % \"sh\") (= % \"bash\") (= % \"-c\")) argv))\n"))
        (is (= [] (shell-argv-violations (str sandbox))))
        (finally (fs/delete-tree sandbox))))))
