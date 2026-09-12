# Executed verification entrances

All suite commands were sequential. Each long-running suite wrote its log
outside the repository and was copied into attempt15 only after exit. No
repository edits occurred during suite purity snapshots.

```sh
git checkout bb-rewrite-tower-local
python3 -B docs/observations/2026-09-12-bbtower-block-b/attempt15/restricted-oracle.py
standard-clj fix test/clj_surgeon/mission_git_process_test.clj test/clj_surgeon/mission_git_identity_test.clj test/clj_surgeon/mission_git_ledger_test.clj
```

The Landlock command ran on original source (red), intermediate source (one
typed path-budget error), then corrected source (green). Its logs retain the
fresh TMPDIR for each invocation. The standalone fresh oracle used the same
unittest command as the wrapper, without Landlock; it asserted that its
fresh TMPDIR was empty before and after, and compared sorted `gate-*` listings.

The remaining entrances used:

```sh
export TMPDIR=/var/tmp/forge/bbtower-fx
export JAVA_TOOL_OPTIONS='-Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx'
~/bin/clj-kondo --config '{:lint-as {clj-surgeon.tmp-leak-support/with-temp-dir clojure.core/let}}' --lint test/clj_surgeon/mission_git_process_test.clj test/clj_surgeon/mission_git_identity_test.clj test/clj_surgeon/mission_git_ledger_test.clj
clojure -J-Xmx1g -M:clj-surgeon/test-deps -e "(require 'clj-surgeon.mission-git-process-test 'clj-surgeon.mission-git-identity-test 'clj-surgeon.mission-git-ledger-test) (binding [clojure.test/*report-counters* (ref clojure.test/*initial-report-counters*)] (clojure.test/test-vars [#'clj-surgeon.mission-git-process-test/nonreading-child-cannot-block-stdin-deadline #'clj-surgeon.mission-git-identity-test/nonidentity-git-environment-is-still-removed #'clj-surgeon.mission-git-ledger-test/public-handler-does-not-accept-proof-overrides]) (let [r @clojure.test/*report-counters*] (prn r) (System/exit (if (zero? (+ (:fail r) (:error r))) 0 1))))"
make test-fast
make landing-gate-prewarm
```

The initial lint omitted `--config` and could not recognize the existing
macro's bindings. The preliminary bare-bb test load used `bb -Xmx1g
-Djava.io.tmpdir=/var/tmp/forge/bbtower-fx -e` to require the three namespaces;
it failed loading nrepl before executing any tests. Both unsuccessful outputs
are retained; neither is a green check.

```sh
git grep -n /var/tmp/forge 2f7b4cf8 -- test libs
rg -n /var/tmp/forge test libs
cmp /var/tmp/forge/bbtower-fx/attempt15-prewarm.log docs/observations/2026-09-12-bbtower-block-b/attempt15/gate.md
git diff 2f7b4cf8 HEAD --check
```
