#!/usr/bin/env bash
# Round 4: the same host lock held by the active ship; no overlapping suites.
set -u
cd /home/forge/src/clj-surgeon-datacode
export TMPDIR=/var/tmp/forge/datacode-fx
export JAVA_TOOL_OPTIONS='-Xmx1024m -Djava.io.tmpdir=/var/tmp/forge/datacode-fx'
round4=docs/observations/2026-09-12-data-not-code/round4
exec 9>/home/forge/tmp/suite-1.lock
date -u +%FT%TZ > "$round4/lock-requested.txt"
flock 9
if compgen -G '/var/tmp/forge/jvm-lease*' >/dev/null; then
  echo 'A separate JVM lease exists; investigate before running.'
  exit 90
fi
lease=/var/tmp/forge/jvm-lease-datacode-round4
printf '%s\n' "$$" > "$lease"
trap 'rm -f "$lease"' EXIT
date -u +%FT%TZ > "$round4/lock-acquired.txt"
run_check() {
  local name=$1 start end status
  shift
  start=$(date +%s%3N)
  "$@" > "$round4/$name.log" 2>&1
  status=$?
  end=$(date +%s%3N)
  printf '%s\t%s\t%s\t%s\t%s\n' "$name" "$start" "$end" "$((end-start))" "$status" >> "$round4/checks.tsv"
  return "$status"
}
run_check focused-jvm clj-nrepl-eval --port 36581 --timeout 600000 '
(assert (= "/home/forge/src/clj-surgeon-datacode" (System/getProperty "user.dir")))
(doseq [n (quote [clj-surgeon.operation-algebra-test clj-surgeon.intent-transaction-test clj-surgeon.edit-dsl-test clj-surgeon.xray-test clj-surgeon.lane-manifest-test clj-surgeon.receipt-artifacts-boundary-test])] (require n :reload))
(let [r (apply clojure.test/run-tests (quote [clj-surgeon.operation-algebra-test clj-surgeon.intent-transaction-test clj-surgeon.edit-dsl-test clj-surgeon.xray-test clj-surgeon.lane-manifest-test clj-surgeon.receipt-artifacts-boundary-test]))]
  (spit "docs/observations/2026-09-12-data-not-code/round4/focused-jvm-summary.edn" (pr-str r))
  (prn r))' || exit $?
bb -Xmx1g -e '(let [r (clojure.edn/read-string (slurp "docs/observations/2026-09-12-data-not-code/round4/focused-jvm-summary.edn"))] (System/exit (+ (:fail r) (:error r))))' || exit $?
run_check test-fast make test-fast
fast_status=$?
run_check prewarm make landing-gate-prewarm
gate_status=$?
date -u +%FT%TZ > "$round4/lock-released.txt"
exit "$((fast_status || gate_status))"
