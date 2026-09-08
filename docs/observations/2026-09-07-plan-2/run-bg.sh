#!/usr/bin/env bash
# run-bg <name> <cmd...> — launch a detached background job and print ONE receipt line naming the REAL pid.
# Built 2026-09-08 after four waiters bound to the wrong pid in 90 min (sol-yolo wrapper, `setsid … &` then $!,
# a helper pid, a cwd scan). setsid forks when the caller leads a process group, so `$!` is a parent that exits at
# once. This one-shot makes the job write its own $$ to a pidfile before exec, and prints it. Waiters bind to
# that pid only:  run-bg NAME CMD…  →  RUN name=NAME pid=P log=/var/tmp/forge/run-bg/NAME.log
set -u; name=${1:?name}; shift; d=/var/tmp/forge/run-bg; mkdir -p "$d"; pf="$d/$name.pid"; log="$d/$name.log"; rm -f "$pf"
nohup setsid bash -c 'echo $$ > "$0"; exec "$@"' "$pf" "$@" > "$log" 2>&1 < /dev/null &
for i in $(seq 1 50); do [ -s "$pf" ] && break; sleep 0.1; done
p=$(cat "$pf" 2>/dev/null); [ -n "$p" ] && kill -0 "$p" 2>/dev/null || { echo "RUN name=$name FAILED to start (see $log)"; exit 1; }
echo "RUN name=$name pid=$p log=$log wait='while kill -0 $p 2>/dev/null; do sleep 30; done'"
