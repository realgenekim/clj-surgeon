#!/usr/bin/env bash
set -u
export TMPDIR=/var/tmp/forge/bbtower-fx
export JAVA_TOOL_OPTIONS='-Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx'
unset _JAVA_OPTIONS
label="$1"
shift
root=docs/observations/2026-09-12-bbtower-block-b/attempt24
mkdir -p "$root/checks"
log="$root/checks/$label.log"
started=$(date -u +%Y-%m-%dT%H:%M:%SZ)
t0=$(date +%s)
{
  echo "STARTED=$started"
  git rev-parse HEAD
  printf 'COMMAND:'
  printf ' %q' "$@"
  printf '\n'
  "$@"
} > "$log" 2>&1
rc=$?
t1=$(date +%s)
echo "EXIT=$rc WALL_S=$((t1-t0))" >> "$log"
bb -e '(let [[path label log started wall exit] *command-line-args*]
         (spit path (str (pr-str {:label label :log log :started started
                                 :wall-s (parse-long wall) :exit (parse-long exit)}) "\n")
               :append true))' "$root/checks/index.edn" "$label" "$log" "$started" "$((t1-t0))" "$rc"
tail -10 "$log"
exit "$rc"
