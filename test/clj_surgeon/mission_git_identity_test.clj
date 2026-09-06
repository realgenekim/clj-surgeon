(ns clj-surgeon.mission-git-identity-test
  {:lane :battery}
  (:require
   [clj-surgeon.mission-git-boundary-test :as fixture]
   [clj-surgeon.mission-git-identity-fixture :as identity]
   [clojure.test :refer [deftest is]]))

(def seat-env
  {"GIT_AUTHOR_NAME" "Fixture seat author" "GIT_AUTHOR_EMAIL" "seat-author@example.invalid"
   "GIT_AUTHOR_DATE" "1700000000 +0000"
   "GIT_COMMITTER_NAME" "Fixture seat committer" "GIT_COMMITTER_EMAIL" "seat-committer@example.invalid"
   "GIT_COMMITTER_DATE" "1700000001 +0000"})

(deftest explicit-seat-identity-beats-conflicting-repository-identity
  (fixture/with-repository
    (fn [_root run p]
      ;; Exact Opus failure mechanism: inherited seat authority vs a different
      ;; repository author. Never use a real human identity in this fixture.
      (run ["config" "user.name" "Conflicting repository author"] nil)
      (run ["config" "user.email" "repo-author@example.invalid"] nil)
      (is (:ok (identity/commit p seat-env)))
      (is (= "Fixture seat author <seat-author@example.invalid>|1700000000|Fixture seat committer <seat-committer@example.invalid>|1700000001\n"
             (run ["show" "-s" "--format=%an <%ae>|%at|%cn <%ce>|%ct" "HEAD"] nil))))))

(deftest omitted-seat-environment-keeps-local-config-behavior
  (fixture/with-repository
    (fn [_root run p]
      (run ["config" "user.name" "Explicit local author"] nil)
      (run ["config" "user.email" "local@example.invalid"] nil)
      (is (:ok (identity/commit p {})))
      (is (= "Explicit local author <local@example.invalid>|Explicit local author <local@example.invalid>\n"
             (run ["show" "-s" "--format=%an <%ae>|%cn <%ce>" "HEAD"] nil))))))

(deftest nonidentity-git-environment-is-still-removed
  (let [dangerous {"GIT_CONFIG_COUNT" "1" "GIT_CONFIG_KEY_0" "user.name" "GIT_CONFIG_VALUE_0" "Injected"
                   "GIT_CONFIG_PARAMETERS" "injected" "GIT_DIR" "/not-the-repository"
                   "GIT_WORK_TREE" "/wrong-worktree" "GIT_INDEX_FILE" "/wrong-index"
                   "GIT_SSH_COMMAND" "do-not-execute" "GIT_AUTHOR_IDENT" "synthetic"
                   "GIT_TERMINAL_PROMPT" "1"}
        result (identity/isolated (merge seat-env dangerous)
                 "(require '[clj-surgeon.mission-git-process :as p] '[clojure.edn :as edn]) (print (p/run-process! \"/var/tmp/forge\" [\"bb\" \"-e\" \"(prn (into {} (filter (fn [[k _]] (.startsWith k \\\"GIT_\\\")) (System/getenv))))\"] nil 10000))")]
    (is (= (assoc seat-env "GIT_TERMINAL_PROMPT" "0") result))))
