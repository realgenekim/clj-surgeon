(ns clj-surgeon.worktree-lifecycle-cli-test
  (:require
   [clojure.string :as str]
   [clojure.test :refer [deftest is]]))

(def makefile (slurp "Makefile"))

(defn- assert-context [conditions]
  (doseq [condition conditions]
    (is condition)))

(deftest make-exposes-the-paved-lifecycle-entrances
  ;; @spec WTL-CLI-001
  (assert-context [(string? makefile)
                   (str/includes? makefile "help:")
                   (str/includes? makefile "test-fast:")
                   (str/includes? makefile ".PHONY:")
                   (not (str/blank? makefile))])
  (is (str/includes? makefile "worktree-lifecycle-test:")))

(deftest cli-contract-is-versioned-edn-with-typed-exits
  ;; @spec WTL-CLI-002
  (assert-context [(str/includes? makefile "clojure")
                   (str/includes? makefile "help:")
                   (str/includes? makefile "Run all tests")
                   (not (str/includes? makefile "git worktree remove --force"))])
  (is (str/includes? makefile "clj-surgeon.worktree-lifecycle-io")))

(deftest help-names-the-schemas-and-non-deletion-law
  ;; @spec WTL-CLI-003 WTL-PRUNE-009
  (assert-context [(str/includes? makefile "Direct usage:")
                   (str/includes? makefile "Installation overrides:")
                   (str/includes? makefile "make test-fast")
                   (str/includes? makefile "make install")
                   (str/includes? makefile "worktree-registration-prune-request/v1")
                   (str/includes? makefile "branch-tip-on-remote")
                   (str/includes? makefile "commit-on-remote")
                   (str/includes? makefile "one target")
                   (str/includes? makefile "never force")
                   (str/includes? makefile "never runs global git worktree prune")])
  (is (str/includes? makefile "make finish-worktree PLAN=/absolute/path.edn APPLY=1")))

(deftest makefile-does-not-embed-destructive-lifecycle-policy
  (assert-context [(not (str/includes? makefile "git branch -D"))
                   (not (str/includes? makefile "git branch --delete --force"))
                   (not (re-find #"(?m)^\\t@?git worktree prune(?:\\s|$)" makefile))
                   (not (str/includes? makefile "git worktree remove --force"))
                   (not (str/includes? makefile "rm -rf $(WORKTREE)"))]))
