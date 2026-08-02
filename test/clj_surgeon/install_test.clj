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

(deftest install-help-makes-both-destinations-explicit
  (let [{:keys [exit out err]} (run-make "help")]
    (testing "help succeeds"
      (is (zero? exit) err))
    (testing "an agent can discover how to install only the CLI"
      (is (str/includes? out "make install-cli"))
      (is (str/includes? out "CLI_DEST=/path/to/clj-surgeon")))
    (testing "the CLI and skill destinations cannot be confused"
      (is (str/includes? out "CLI_DEST"))
      (is (str/includes? out "CODEX_HOME")))))

(deftest install-cli-default-is-relative-to-home
  (let [tmp-dir (fs/create-temp-dir {:prefix "clj-surgeon-install-home-"})
        cli-path (fs/path tmp-dir "bin" "clj-surgeon")]
    (try
      (let [{:keys [exit out err]}
            (run-make "--silent" "install-cli" (str "HOME=" tmp-dir))]
        (testing "the default install creates HOME/bin when it is absent"
          (is (zero? exit) (str out err))
          (is (fs/executable? cli-path))
          (is (str/includes? out (str "Installed " cli-path))))
        (testing "the installed shim is the real CLI"
          (let [{cli-exit :exit cli-out :out cli-err :err}
                (run-installed-cli cli-path "--help")]
            (is (zero? cli-exit) cli-err)
            (is (str/includes? cli-out "Usage: clj-surgeon")))))
      (finally
        (fs/delete-tree tmp-dir)))))

(deftest install-cli-supports-a-custom-path-with-spaces
  (let [tmp-dir (fs/create-temp-dir {:prefix "clj-surgeon-install-custom-"})
        home (fs/path tmp-dir "home")
        cli-path (fs/path tmp-dir "custom bin" "surgeon")]
    (try
      (let [{:keys [exit out err]}
            (run-make "--silent" "install-cli"
                      (str "HOME=" home)
                      (str "CLI_DEST=" cli-path))]
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
        (fs/delete-tree tmp-dir)))))

(deftest install-still-installs-cli-and-codex-skill
  (let [tmp-dir (fs/create-temp-dir {:prefix "clj-surgeon-install-all-"})
        cli-path (fs/path tmp-dir "bin" "clj-surgeon")
        codex-home (fs/path tmp-dir "codex")
        skill-path (fs/path codex-home "skills" "clj-surgeon")
        skill-source (fs/real-path (fs/path project-root "skills" "clj-surgeon"))]
    (try
      (let [{:keys [exit out err]}
            (run-make "--silent" "install"
                      (str "CLI_DEST=" cli-path)
                      (str "CODEX_HOME=" codex-home))]
        (testing "the aggregate target succeeds and installs both artifacts"
          (is (zero? exit) (str out err))
          (is (fs/executable? cli-path))
          (is (fs/sym-link? skill-path))
          (is (= skill-source (fs/real-path skill-path)))))
      (finally
        (fs/delete-tree tmp-dir)))))
