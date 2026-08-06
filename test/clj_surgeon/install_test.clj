(ns clj-surgeon.install-test
  (:require
   [babashka.fs :as fs]
   [babashka.process :as proc]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.test :refer [deftest is testing]]))

;; These are deliberately subprocess tests. The public contract under test is
;; GNU Make variable expansion plus filesystem installation; there is no pure
;; Clojure decision function hiding behind that boundary.

(def ^:private project-root
  (.getCanonicalPath (io/file ".")))

(defn- run-make
  [& args]
  @(proc/process
     (into ["make" "--no-print-directory"] args)
     {:dir project-root
      :err :string
      :out :string}))

(defn- run-installed-cli
  [path & args]
  @(proc/process
     (into ["bb" (str path)] args)
     {:err :string :out :string}))

(defn- slurp-path
  [path]
  (slurp (str path)))

(defn- delete-temp-tree
  [path]
  (fs/delete-tree path {:force true}))

(def ^:private canonical-skill-path
  (fs/path project-root "skills" "clj-surgeon" "SKILL.md"))

(def ^:private source-commit
  (let [{:keys [exit out]}
        @(proc/process ["git" "rev-parse" "HEAD"]
                       {:dir project-root :out :string :err :string})]
    (if (zero? exit) (str/trim out) "unknown")))

(def ^:private stable-copy-stamp
  (str "\nStable copy installed from commit " source-commit ".\n"
       "When working inside the clj-surgeon repository, the working-tree skill.md\n"
       "supersedes this copy.\n"))

(def ^:private canonical-reference-path
  (fs/path project-root "skills" "clj-surgeon" "references" "advanced-operations.md"))

(defn- assert-skill-contracts
  [label skill]
  (doseq [contract ["Invoke before using Read, Edit, grep, sed, or cat"
                    "Native Write is right for new files"
                    ":op :xray"
                    ":op :edit"
                    ":op :replace-subform!"
                    "(match :href) right"
                    "`right`: next structural sibling"
                    "`left`: previous structural sibling"
                    "`up`: structural parent"
                    "`down`: first structural child"
                    "Navigation skips whitespace and comments"
                    "no selected ancestor"
                    "There is no variadic wildcard"
                    "(loop _ _)"
                    "do not reopen the plan file"
                    "refuses"
                    ":verified"]]
    (is (str/includes? skill contract)
        (str label " must retain the shared " contract " contract"))))

(deftest repository-agent-skill-entrances-do-not-drift
  (let [canonical (slurp-path canonical-skill-path)
        claude-native (slurp ".claude/skills/clj-surgeon/SKILL.md")
        legacy (-> (slurp "skill.md")
                   (str/replace
                     "[the advanced operations reference](skills/clj-surgeon/references/advanced-operations.md)"
                     "[references/advanced-operations.md](references/advanced-operations.md)"))]
    (testing "the native Claude package is byte-identical to the canonical package"
      (is (= canonical claude-native))
      (is (= (slurp-path canonical-reference-path)
             (slurp ".claude/skills/clj-surgeon/references/advanced-operations.md"))))
    (testing "the root legacy entrance differs only by its valid relative reference"
      (is (= canonical legacy))
      (is (<= (count (str/split-lines (slurp "skill.md"))) 90)))
    (testing "X-ray, planning, refusal, and receipt guidance is shared"
      (assert-skill-contracts "canonical Codex skill" canonical)
      (assert-skill-contracts "native Claude skill" claude-native)
      (assert-skill-contracts "legacy Claude entrance" legacy))))

(deftest install-help-makes-both-destinations-explicit
  (let [{:keys [exit out err]} (run-make "help")]
    (testing "help succeeds"
      (is (zero? exit) err))
    (testing "an agent can discover how to install only the CLI"
      (is (str/includes? out "make install-cli"))
      (is (str/includes? out "CLI_DEST=/path/to/clj-surgeon")))
    (testing "the CLI and skill destinations cannot be confused"
      (is (str/includes? out "CLI_DEST"))
      (is (str/includes? out "CODEX_HOME"))
      (is (str/includes? out "CLAUDE_HOME"))
      (is (str/includes? out "INSTALL_ROOT")))
    (testing "stable and explicitly branch-live modes are distinguishable"
      (is (str/includes? out "Stable copied CLI"))
      (is (str/includes? out "make install-claude-skill"))
      (is (str/includes? out "make install-dev"))
      (is (str/includes? out "Branch-live")))
    (testing "bounded clean-context acceptance batteries are discoverable"
      (doseq [target ["benchmark-codex-skill" "benchmark-claude-skill" "benchmark-agent-skills" "benchmark-agent-skills-self-test" "study-agent-usage" "study-agent-usage-self-test" "retain-benchmark-result" "verify-benchmark-retention"]]
        (is (str/includes? out target))))))

(deftest benchmark-agent-skill-targets-are-bounded-and-composable
  (let [{codex-exit :exit codex-out :out codex-err :err}
        (run-make "--dry-run" "benchmark-codex-skill")
        {claude-exit :exit claude-out :out claude-err :err}
        (run-make "--dry-run" "benchmark-claude-skill")
        {aggregate-exit :exit aggregate-out :out aggregate-err :err}
        (run-make "--dry-run" "benchmark-agent-skills")]
    (testing "Codex defaults to one post-version matched-skill read/edit/guarded-edit trio"
      (is (zero? codex-exit) (str codex-out codex-err))
      (is (str/includes? codex-out "BENCH_POST_COMMIT=\"${BENCH_POST_COMMIT:-HEAD}\""))
      (is (str/includes? codex-out "BENCH_VERSIONS=\"${BENCH_VERSIONS:-post}\""))
      (is (str/includes? codex-out "BENCH_CONTEXTS=\"${BENCH_CONTEXTS:-matched-skill}\""))
      (is (str/includes? codex-out
                         "BENCH_TASKS=\"${BENCH_TASKS:-ops-registry-xray pair-view-edit pair-view-expect-edit}\""))
      (is (str/includes? codex-out "BENCH_REPLICATES=\"${BENCH_REPLICATES:-1}\"")))
    (testing "Claude owns its prompts, grading, models, and deadlines in one harness"
      (is (zero? claude-exit) (str claude-out claude-err))
      (is (str/includes? claude-out "bash bench/run_clean_claude.sh")))
    (testing "the aggregate acceptance target invokes both bounded batteries"
      (is (zero? aggregate-exit) (str aggregate-out aggregate-err))
      (is (str/includes? aggregate-out "benchmark-codex-skill"))
      (is (str/includes? aggregate-out "benchmark-claude-skill")))))

(deftest install-cli-default-is-relative-to-home
  (let [tmp-dir (fs/create-temp-dir {:prefix "clj-surgeon-install-home-"})
        cli-path (fs/path tmp-dir "bin" "clj-surgeon")]
    (try
      (let [{:keys [exit out err]}
            (run-make "--silent" "install-cli"
                      (str "HOME=" tmp-dir)
                      (str "INSTALL_ROOT=" (fs/path tmp-dir "packages")))]
        (testing "the default install creates HOME/bin when it is absent"
          (is (zero? exit) (str out err))
          (is (fs/executable? cli-path))
          (is (str/includes? out (str "Installed stable CLI " cli-path))))
        (testing "the installed shim is the real CLI"
          (let [{cli-exit :exit cli-out :out cli-err :err}
                (run-installed-cli cli-path "--help")]
            (is (zero? cli-exit) cli-err)
            (is (str/includes? cli-out "Usage: clj-surgeon")))))
      (finally
        (delete-temp-tree tmp-dir)))))

(deftest install-cli-supports-a-custom-path-with-spaces
  (let [tmp-dir (fs/create-temp-dir {:prefix "clj-surgeon-install-custom-"})
        home (fs/path tmp-dir "home")
        cli-path (fs/path tmp-dir "custom bin" "surgeon")]
    (try
      (let [{:keys [exit out err]}
            (run-make "--silent" "install-cli"
                      (str "HOME=" home)
                      (str "CLI_DEST=" cli-path)
                      (str "INSTALL_ROOT=" (fs/path tmp-dir "packages")))]
        (testing "the requested parent is created and the exact path is used"
          (is (zero? exit) (str out err))
          (is (fs/executable? cli-path))
          (is (not (fs/exists? (fs/path home "bin" "clj-surgeon")))))
        (testing "shell quoting preserves a destination containing spaces"
          (let [{cli-exit :exit cli-out :out cli-err :err}
                (run-installed-cli cli-path "--help")]
            (is (zero? cli-exit) cli-err)
            (is (str/includes? cli-out "Usage: clj-surgeon")))))
      (finally
        (delete-temp-tree tmp-dir)))))

(deftest stable-install-isolates-cli-and-both-agent-skills
  (let [tmp-dir (fs/create-temp-dir {:prefix "clj-surgeon-install-all-"})
        cli-path (fs/path tmp-dir "bin" "clj-surgeon")
        codex-home (fs/path tmp-dir "codex")
        claude-home (fs/path tmp-dir "claude")
        install-root (fs/path tmp-dir "packages")
        codex-skill (fs/path codex-home "skills" "clj-surgeon")
        claude-skill (fs/path claude-home "skills" "clj-surgeon")]
    (try
      (let [{:keys [exit out err]}
            (run-make "--silent" "install"
                      (str "CLI_DEST=" cli-path)
                      (str "CODEX_HOME=" codex-home)
                      (str "CLAUDE_HOME=" claude-home)
                      (str "INSTALL_ROOT=" install-root))]
        (testing "the aggregate target succeeds and installs all entrances"
          (is (zero? exit) (str out err))
          (is (fs/executable? cli-path))
          (is (fs/sym-link? codex-skill))
          (is (fs/sym-link? claude-skill)))
        (testing "Codex and Claude resolve the same copied package"
          (is (= (fs/real-path codex-skill) (fs/real-path claude-skill)))
          (is (str/starts-with? (str (fs/real-path codex-skill))
                                (str (fs/real-path install-root))))
          (is (not= (fs/real-path canonical-skill-path)
                    (fs/real-path (fs/path codex-skill "SKILL.md"))))
          (is (= (str (slurp-path canonical-skill-path) stable-copy-stamp)
                 (slurp-path (fs/path codex-skill "SKILL.md"))))
          (is (not (fs/writable? (fs/path (fs/real-path codex-skill) "SKILL.md")))))
        (testing "the CLI launcher resolves only the copied runtime"
          (let [launcher (slurp-path cli-path)]
            (is (str/includes? launcher "clj-surgeon stable launcher"))
            (is (str/includes? launcher (str install-root)))
            (is (not (str/includes? launcher project-root)))))
        (testing "receipts identify source bytes, mode, package, and destination"
          (doseq [receipt [(str cli-path ".receipt.edn")
                           (str codex-skill ".receipt.edn")
                           (str claude-skill ".receipt.edn")]]
            (let [content (slurp receipt)]
              (is (str/includes? content ":mode :stable-copy"))
              (is (str/includes? content ":source-commit"))
              (is (str/includes? content ":source-hash"))
              (is (str/includes? content ":destination"))
              (is (str/includes? content ":package")))))
        (testing "installed agent surfaces retain identical contracts"
          (let [codex (slurp-path (fs/path codex-skill "SKILL.md"))
                claude (slurp-path (fs/path claude-skill "SKILL.md"))]
            (is (= codex claude))
            (assert-skill-contracts "installed Codex skill" codex)
            (assert-skill-contracts "installed Claude skill" claude))))
      (finally
        (delete-temp-tree tmp-dir)))))

(deftest installers-refuse-unrelated-destinations-without-changing-them
  (doseq [[target home-var home-dir relative-dest]
          [["install-codex-skill" "CODEX_HOME" "codex" ["skills" "clj-surgeon"]]
           ["install-claude-skill" "CLAUDE_HOME" "claude" ["skills" "clj-surgeon"]]]]
    (let [tmp-dir (fs/create-temp-dir {:prefix "clj-surgeon-install-refusal-"})
          agent-home (fs/path tmp-dir home-dir)
          destination (apply fs/path agent-home relative-dest)
          sentinel (fs/path destination "KEEP")]
      (try
        (fs/create-dirs destination)
        (spit (str sentinel) "unrelated")
        (let [{:keys [exit out err]}
              (run-make "--silent" target
                        (str home-var "=" agent-home)
                        (str "INSTALL_ROOT=" (fs/path tmp-dir "packages")))]
          (is (not (zero? exit)) (str target " must refuse"))
          (is (str/includes? (str out err) "Refusing to replace unrelated path"))
          (is (= "unrelated" (slurp-path sentinel))))
        (finally
          (delete-temp-tree tmp-dir)))))
  (let [tmp-dir (fs/create-temp-dir {:prefix "clj-surgeon-cli-refusal-"})
        cli-path (fs/path tmp-dir "bin" "clj-surgeon")]
    (try
      (fs/create-dirs (fs/parent cli-path))
      (spit (str cli-path) "unrelated CLI")
      (let [{:keys [exit out err]}
            (run-make "--silent" "install-cli"
                      (str "CLI_DEST=" cli-path)
                      (str "INSTALL_ROOT=" (fs/path tmp-dir "packages")))]
        (is (not (zero? exit)))
        (is (str/includes? (str out err) "Refusing to replace unrelated file"))
        (is (= "unrelated CLI" (slurp-path cli-path))))
      (fs/delete cli-path)
      (spit (str cli-path ".receipt.edn") "unrelated receipt")
      (let [{:keys [exit out err]}
            (run-make "--silent" "install-cli"
                      (str "CLI_DEST=" cli-path)
                      (str "INSTALL_ROOT=" (fs/path tmp-dir "packages")))]
        (is (not (zero? exit)))
        (is (str/includes? (str out err) "Refusing to replace unrelated receipt"))
        (is (= "unrelated receipt" (slurp (str cli-path ".receipt.edn"))))
        (is (not (fs/exists? cli-path))))
      (finally
        (delete-temp-tree tmp-dir)))))

(deftest skill-installers-refuse-unrelated-symlinks-and-receipts
  (doseq [[target home-var home-dir artifact]
          [["install-codex-skill" "CODEX_HOME" "codex" ":codex-skill"]
           ["install-claude-skill" "CLAUDE_HOME" "claude" ":claude-skill"]]]
    (let [tmp-dir (fs/create-temp-dir {:prefix "clj-surgeon-install-link-refusal-"})
          agent-home (fs/path tmp-dir home-dir)
          destination (fs/path agent-home "skills" "clj-surgeon")
          unrelated (fs/path tmp-dir "unrelated-skill")
          receipt (str destination ".receipt.edn")]
      (try
        (fs/create-dirs (fs/parent destination))
        (fs/create-dirs unrelated)
        (fs/create-sym-link destination unrelated)
        (let [{:keys [exit out err]}
              (run-make "--silent" target
                        (str home-var "=" agent-home)
                        (str "INSTALL_ROOT=" (fs/path tmp-dir "packages")))]
          (is (not (zero? exit)) (str target " must refuse an unrelated symlink"))
          (is (str/includes? (str out err) "Refusing to replace unrelated symlink"))
          (is (= (fs/real-path unrelated) (fs/real-path destination))))
        (fs/delete destination)
        (spit receipt "unrelated receipt")
        (let [{:keys [exit out err]}
              (run-make "--silent" target
                        (str home-var "=" agent-home)
                        (str "INSTALL_ROOT=" (fs/path tmp-dir "packages")))]
          (is (not (zero? exit)) (str target " must refuse an unrelated receipt"))
          (is (str/includes? (str out err) "Refusing to replace unrelated receipt"))
          (is (= "unrelated receipt" (slurp receipt)))
          (is (not (fs/exists? destination))))
        (testing "the expected receipt marker is agent-specific"
          (is (not (str/includes? (slurp receipt) artifact))))
        (finally
          (delete-temp-tree tmp-dir))))))

(deftest development-install-is-explicitly-branch-coupled
  (let [tmp-dir (fs/create-temp-dir {:prefix "clj-surgeon-install-dev-"})
        cli-path (fs/path tmp-dir "bin" "clj-surgeon")
        codex-home (fs/path tmp-dir "codex")
        claude-home (fs/path tmp-dir "claude")
        codex-skill (fs/path codex-home "skills" "clj-surgeon")
        claude-skill (fs/path claude-home "skills" "clj-surgeon")]
    (try
      (let [{:keys [exit out err]}
            (run-make "--silent" "install-dev"
                      (str "CLI_DEST=" cli-path)
                      (str "CODEX_HOME=" codex-home)
                      (str "CLAUDE_HOME=" claude-home))]
        (is (zero? exit) (str out err))
        (is (str/includes? out "DEVELOPMENT LINK"))
        (is (str/includes? out "branch-coupled"))
        (is (str/includes? (slurp-path cli-path) (str (fs/path project-root "src"))))
        (is (= (fs/real-path (fs/path project-root "skills" "clj-surgeon"))
               (fs/real-path codex-skill)
               (fs/real-path claude-skill)))
        (doseq [receipt [(str cli-path ".receipt.edn")
                         (str codex-skill ".receipt.edn")
                         (str claude-skill ".receipt.edn")]]
          (is (str/includes? (slurp receipt) ":mode :development-link")))
        (testing "a normal install replaces the owned development entrances"
          (let [install-root (fs/path tmp-dir "packages")
                {stable-exit :exit stable-out :out stable-err :err}
                (run-make "--silent" "install"
                          (str "CLI_DEST=" cli-path)
                          (str "CODEX_HOME=" codex-home)
                          (str "CLAUDE_HOME=" claude-home)
                          (str "INSTALL_ROOT=" install-root))]
            (is (zero? stable-exit) (str stable-out stable-err))
            (is (str/includes? (slurp-path cli-path) "stable launcher"))
            (is (not (str/includes? (slurp-path cli-path) project-root)))
            (is (str/starts-with? (str (fs/real-path codex-skill))
                                  (str (fs/real-path install-root))))
            (is (= (fs/real-path codex-skill) (fs/real-path claude-skill))))))
      (finally
        (delete-temp-tree tmp-dir)))))
