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
   [clj-surgeon.core :as core]
   [clojure.test :refer [deftest is testing]]))

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
