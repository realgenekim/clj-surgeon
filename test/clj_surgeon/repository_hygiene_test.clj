(ns clj-surgeon.repository-hygiene-test
  "Working-tree hygiene the repository itself must satisfy.

  A machine-local build cache carries absolute paths from whichever machine
  produced it, so committing one publishes a foreign filesystem layout and
  guarantees a conflict on the next build. This suite is the executable
  statement that such a cache is never tracked and cannot silently return."
  (:require
   [clojure.java.shell :as shell]
   [clojure.string :as str]
   [clojure.test :refer [deftest is testing]]))

(def machine-local-prefixes
  "Project-relative directory prefixes holding machine-local build artifacts."
  [".cpcache/"])

(defn- git
  "Run one git command in the repository under test, or nil when git is unusable."
  [& args]
  (try
    (apply shell/sh (concat (cons "git" args)
                            [:dir (System/getProperty "user.dir")]))
    (catch Exception _ nil)))

(defn- tracked-paths
  "Every path git tracks in this working tree, or nil when git is unavailable."
  []
  (when-let [{:keys [exit out]} (git "ls-files")]
    (when (zero? exit)
      (vec (remove str/blank? (str/split-lines out))))))

;; @spec MCP-OP-ALIAS-036
(deftest no-machine-local-build-cache-is-tracked
  (if-let [tracked (tracked-paths)]
    (do
      (testing "no tracked path lives under a machine-local build cache"
        (doseq [prefix machine-local-prefixes]
          (let [committed (filterv #(str/starts-with? % prefix) tracked)]
            (is (= [] committed)
                (str "Machine-local build cache files are tracked under "
                     prefix ": " (str/join ", " committed))))))
      (testing "the machine-local build cache is ignored, so it cannot return"
        (doseq [prefix machine-local-prefixes]
          (let [{:keys [exit]} (git "check-ignore" "-q"
                                    (str prefix "probe.marker"))]
            (is (zero? exit)
                (str prefix " is not covered by any gitignore rule"))))))
    (is true "git is unavailable; repository hygiene is not observable here")))
