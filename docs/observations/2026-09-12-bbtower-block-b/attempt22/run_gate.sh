#!/usr/bin/env bash
# Execute one named gate. The caller owns ordering and the prewarm attempt cap.
set -u
export TMPDIR=/var/tmp/forge/bbtower-fx
export JAVA_TOOL_OPTIONS='-Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx'
unset _JAVA_OPTIONS
task="$1"
label="$2"
root=docs/observations/2026-09-12-bbtower-block-b/attempt22
mkdir -p "$root/checks"
log="$root/checks/$label.log"
started=$(date -u +%Y-%m-%dT%H:%M:%SZ)
t0=$(date +%s)
{
  echo "STARTED=$started"
  git rev-parse HEAD
  echo "TMPDIR=$TMPDIR JAVA_TOOL_OPTIONS=$JAVA_TOOL_OPTIONS make $task"
  make "$task"
} > "$log" 2>&1
rc=$?
t1=$(date +%s)
echo "EXIT=$rc WALL_S=$((t1-t0))" >> "$log"
bb -e '(let [[path task label log started wall exit] *command-line-args*]
         (spit path (str (pr-str {:task task :label label :log log :started started
                                 :wall-s (parse-long wall) :exit (parse-long exit)}) "\n")
               :append true))' "$root/checks/index.edn" "$task" "$label" "$log" "$started" "$((t1-t0))" "$rc"
tail -8 "$log"
exit "$rc"
