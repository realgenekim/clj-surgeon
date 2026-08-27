#!/bin/sh
set -eu

root=$(CDPATH= cd -- "$(dirname "$0")/.." && pwd)

bb -e '
  (let [config (clojure.edn/read-string (slurp "deps.edn"))
        alias (get-in config [:aliases :clj-surgeon/analyzer-contract-test])]
    (assert (= ["test"] (:extra-paths alias)))
    (assert (= "1.3.1" (get-in alias [:extra-deps (symbol "nrepl/nrepl") :mvn/version])))
    (assert (= "0.5.30" (get-in alias [:extra-deps (symbol "babashka/fs") :mvn/version])))
    (assert (= ["-m" "analyzer-contract-test-runner"] (:main-opts alias))))' \
  --classpath "$root/src:$root/test"

make -C "$root" --no-print-directory -n analyzer-contract-test |
  grep -Fq -- '-M:clj-surgeon/analyzer-contract-test'

if make -C "$root" --no-print-directory -n analyzer-contract-test |
     grep -Eq '(^|[[:space:]])bb([[:space:]]|$)'; then
  echo "analyzer contract target must use its declared JVM dependency closure" >&2
  exit 1
fi
