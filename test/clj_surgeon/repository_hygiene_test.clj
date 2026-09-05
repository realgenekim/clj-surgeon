(ns ^{:lane :battery} clj-surgeon.repository-hygiene-test
  "Working-tree hygiene the repository itself must satisfy.

  A machine-local build cache carries absolute paths from whichever machine
  produced it, so committing one publishes a foreign filesystem layout and
  guarantees a conflict on the next build. This suite is the executable
  statement that such a cache is never tracked and cannot silently return."
  (:require
   [clojure.java.io :as io]
   [clojure.java.shell :as shell]
   [clojure.string :as str]
   [clojure.test :refer [deftest is testing]])
  (:import
   (java.nio.file Files)
   (java.nio.file.attribute FileAttribute)))

(def machine-local-cache-directories
  "Directory names that hold machine-local build artifacts, at ANY depth.

  Depth matters: a cache under a subproject is exactly as unportable as one at
  the root, and a root-anchored prefix test calls `sub/.cpcache/x` clean."
  [".cpcache"])

;; @spec MCP-OP-ALIAS-053
(def machine-local-cache-pattern
  (re-pattern (str "(^|/)(" (str/join "|" (map #(str/replace % "." "\\.")
                                               machine-local-cache-directories))
                   ")/")))

;; @spec MCP-OP-ALIAS-053
(defn machine-local-cache-path?
  "True when one project-relative path lies inside a machine-local build cache."
  [path]
  (boolean (re-find machine-local-cache-pattern path)))

(def ignore-probes
  "Paths a gitignore rule must cover, one at the root and one below it."
  [".cpcache/probe.marker" "sub/nested/.cpcache/probe.marker"])

(defn- git
  "Run one git command in `dir`, or nil when git is unusable there."
  [dir & args]
  (try
    (apply shell/sh (concat (cons "git" args) [:dir dir]))
    (catch Exception _ nil)))

(defn tracked-paths
  "Every path git tracks in `dir`, or nil when git is unusable there.

  nil is the fail-closed signal: a caller must treat it as a failure, never as
  an excuse to pass."
  [dir]
  (when-let [{:keys [exit out]} (git dir "ls-files")]
    (when (zero? exit)
      (vec (remove str/blank? (str/split-lines out))))))

;; @spec MCP-OP-ALIAS-036
;; @spec MCP-OP-ALIAS-053
(deftest no-machine-local-build-cache-is-tracked
  (let [repository (System/getProperty "user.dir")
        tracked (tracked-paths repository)]
    ;; a suite that degrades to (is true) outside a repository is a suite that
    ;; reports green precisely when it can see nothing
    (is (some? tracked)
        (str "git is unusable in " repository
             ", so repository hygiene cannot be observed; this gate fails"
             " closed rather than passing on an empty view"))
    (when tracked
      (testing "no tracked path lies inside a machine-local build cache, at any depth"
        (let [committed (filterv machine-local-cache-path? tracked)]
          (is (= [] committed)
              (str "Machine-local build cache files are tracked: "
                   (str/join ", " committed)))))
      (testing "the machine-local build cache is ignored at every depth"
        (doseq [probe ignore-probes]
          (let [{:keys [exit]} (git repository "check-ignore" "-q" probe)]
            (is (zero? exit)
                (str probe " is not covered by any gitignore rule"))))))))

;; @spec MCP-OP-ALIAS-053
(deftest the-cache-test-recognises-a-cache-below-the-root
  (testing "the shapes a root-anchored prefix test called clean"
    (is (machine-local-cache-path? ".cpcache/x"))
    (is (machine-local-cache-path? "sub/.cpcache/x"))
    (is (machine-local-cache-path? "a/b/c/.cpcache/deep/x")))
  (testing "and the shapes that are not a cache"
    (is (not (machine-local-cache-path? "src/cpcache/x")))
    (is (not (machine-local-cache-path? "docs/.cpcache-notes.md")))
    (is (not (machine-local-cache-path? "src/clj_surgeon/core.clj")))))

(defn- temp-dir
  []
  (.toFile (Files/createTempDirectory "clj-surgeon-hygiene"
                                      (make-array FileAttribute 0))))

(defn- delete-tree!
  [file]
  (when (.exists (io/file file))
    (doseq [child (reverse (file-seq (io/file file)))]
      (Files/deleteIfExists (.toPath child)))))

;; @spec MCP-OP-ALIAS-053
(deftest a-forced-cache-below-the-root-is-caught-in-a-throwaway-repository
  (let [repository (temp-dir)
        path (.getPath repository)
        nested (io/file repository "sub" ".cpcache")]
    (try
      (is (zero? (:exit (git path "init" "-q"))) "git init failed")
      (spit (io/file repository ".gitignore") ".cpcache/\n")
      (.mkdirs nested)
      (spit (io/file nested "x") "machine-local\n")
      (testing "the ignore rule covers it, so only -f can track it"
        (is (zero? (:exit (git path "check-ignore" "-q" "sub/.cpcache/x")))))
      (git path "add" "-f" "sub/.cpcache/x")
      (is (= ["sub/.cpcache/x"]
             (filterv machine-local-cache-path? (tracked-paths path)))
          "a cache forced in below the root was not recognised")
      (finally
        (delete-tree! repository)))))

;; @spec MCP-OP-ALIAS-053
(deftest an-unusable-git-is-reported-as-nil-so-callers-fail-closed
  (let [not-a-repository (temp-dir)]
    (try
      (is (nil? (tracked-paths (.getPath not-a-repository)))
          "a directory outside any repository must not look like a clean one")
      (finally
        (delete-tree! not-a-repository)))))
