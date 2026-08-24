#!/usr/bin/env bash
set -uo pipefail

repo=/srv/fleet/dev-c/clj-surgeon-one-shot-canary-20260823
evidence_root="$repo/docs/observations/evidence/startup-memory"
lock_file="$evidence_root/.probe.lock"

if [ "$#" -ne 4 ]; then
  echo "usage: $0 NAME XMX_MIB PORT DEADLINE_SECONDS" >&2
  exit 64
fi

name=$1
xmx_mib=$2
port=$3
deadline_seconds=$4
out="$evidence_root/$name"

exec 9> "$lock_file"
if ! flock -n 9; then
  echo "refusing: another startup-memory probe holds $lock_file" >&2
  exit 75
fi
trap 'rm -f "$lock_file"' EXIT

if [ "$PWD" != "$repo" ]; then
  echo "refusing: run from $repo" >&2
  exit 64
fi

available_mib=$(awk '/^MemAvailable:/ {print int($2 / 1024)}' /proc/meminfo)
if [ "$available_mib" -lt 4096 ]; then
  echo "refusing: MemAvailable=${available_mib} MiB is below the 4096 MiB floor" >&2
  exit 75
fi

if ss -ltn "sport = :$port" | awk 'NR > 1 {found=1} END {exit !found}'; then
  echo "refusing: TCP port $port is already listening" >&2
  exit 75
fi

mkdir -p "$out/telemetry"

cmd=(
  clojure
  -J-Xms64m
  "-J-Xmx${xmx_mib}m"
  -J-XX:NativeMemoryTracking=summary
  -J-XX:+HeapDumpOnOutOfMemoryError
  "-J-XX:HeapDumpPath=$out"
  "-J-XX:StartFlightRecording=filename=$out/startup.jfr,settings=profile,dumponexit=true,maxsize=256m"
  "-J-Xlog:gc*,safepoint=debug:file=$out/gc-safepoint.log:time,uptime,level,tags:filecount=1"
  -X:clj-surgeon/mcp
  :project-dir "\"$repo\""
  :port "$port"
  :telemetry :full
  :telemetry-dir "\"$out/telemetry\""
  :run-id "\"startup-memory-$name\""
  :nrepl-port :none
  :ready-file "\"$out/ready.edn\""
  :log-file "\"$out/application.log\""
)

{
  printf 'cwd=%q\n' "$repo"
  printf 'mem_available_before_mib=%s\n' "$available_mib"
  printf 'started_utc=%s\n' "$(date -u +%Y-%m-%dT%H:%M:%SZ)"
  printf 'command='
  printf '%q ' "${cmd[@]}"
  printf '\n'
} > "$out/probe-metadata.txt"

printf 'elapsed_ms\trss_kib\tvm_hwm_kib\tvm_size_kib\tstate\n' > "$out/rss-samples.tsv"

start_epoch=$(date +%s)
/usr/bin/time -v -o "$out/time.txt" \
  "${cmd[@]}" > "$out/stdout.log" 2> "$out/stderr.log" &
runner_pid=$!

jvm_pid=
for _attempt in $(seq 1 100); do
  candidate=$(pgrep -P "$runner_pid" | head -n 1 || true)
  if [ -n "$candidate" ] && ps -p "$candidate" -o comm= | awk '$1 ~ /java/ {found=1} END {exit !found}'; then
    jvm_pid=$candidate
    break
  fi
  if ! kill -0 "$runner_pid" 2>/dev/null; then
    break
  fi
  sleep 0.05
done

if [ -n "$jvm_pid" ]; then
  printf 'runner_pid=%s\njvm_pid=%s\n' "$runner_pid" "$jvm_pid" >> "$out/probe-metadata.txt"
  ps -p "$jvm_pid" -o pid=,ppid=,lstart=,args= > "$out/jvm-process.txt"
else
  printf 'runner_pid=%s\njvm_pid=unresolved\n' "$runner_pid" >> "$out/probe-metadata.txt"
fi

ready_epoch=
terminal_reason=
while kill -0 "$runner_pid" 2>/dev/null; do
  now_epoch=$(date +%s)
  elapsed_ms=$(( (now_epoch - start_epoch) * 1000 ))
  if [ -n "$jvm_pid" ] && kill -0 "$jvm_pid" 2>/dev/null; then
    rss_kib=$(awk '/^VmRSS:/ {print $2}' "/proc/$jvm_pid/status")
    vm_hwm_kib=$(awk '/^VmHWM:/ {print $2}' "/proc/$jvm_pid/status")
    vm_size_kib=$(awk '/^VmSize:/ {print $2}' "/proc/$jvm_pid/status")
    state=$(awk '/^State:/ {print $2}' "/proc/$jvm_pid/status")
    printf '%s\t%s\t%s\t%s\t%s\n' "$elapsed_ms" "$rss_kib" "$vm_hwm_kib" "$vm_size_kib" "$state" >> "$out/rss-samples.tsv"
  fi

  if [ -z "$ready_epoch" ] && [ -s "$out/ready.edn" ]; then
    ready_epoch=$now_epoch
    printf 'ready_elapsed_seconds=%s\n' "$((ready_epoch - start_epoch))" >> "$out/probe-metadata.txt"
    if [ -n "$jvm_pid" ]; then
      jcmd "$jvm_pid" GC.heap_info > "$out/heap-ready.txt" 2>&1 || true
      jcmd "$jvm_pid" VM.native_memory summary scale=MB > "$out/nmt-ready.txt" 2>&1 || true
      jcmd "$jvm_pid" GC.class_histogram -all > "$out/class-histogram-ready.txt" 2>&1 || true
    fi
  fi

  if [ -n "$ready_epoch" ] && [ $((now_epoch - ready_epoch)) -ge 5 ]; then
    if [ -n "$jvm_pid" ]; then
      jcmd "$jvm_pid" GC.heap_info > "$out/heap-settled.txt" 2>&1 || true
      jcmd "$jvm_pid" VM.native_memory summary scale=MB > "$out/nmt-settled.txt" 2>&1 || true
    fi
    terminal_reason=ready_then_sampled
    kill -TERM "$jvm_pid" 2>/dev/null || true
    break
  fi

  if [ $((now_epoch - start_epoch)) -ge "$deadline_seconds" ]; then
    if [ -n "$jvm_pid" ] && kill -0 "$jvm_pid" 2>/dev/null; then
      jcmd "$jvm_pid" GC.heap_info > "$out/heap-deadline.txt" 2>&1 || true
      jcmd "$jvm_pid" VM.native_memory summary scale=MB > "$out/nmt-deadline.txt" 2>&1 || true
      jcmd "$jvm_pid" GC.class_histogram -all > "$out/class-histogram-deadline.txt" 2>&1 || true
      kill -TERM "$jvm_pid" 2>/dev/null || true
    fi
    terminal_reason=deadline
    break
  fi
  sleep 0.2
done

for _attempt in $(seq 1 50); do
  if ! kill -0 "$runner_pid" 2>/dev/null; then
    break
  fi
  sleep 0.1
done
if kill -0 "$runner_pid" 2>/dev/null; then
  terminal_reason=${terminal_reason:-forced_after_grace}
  if [ -n "$jvm_pid" ]; then
    kill -KILL "$jvm_pid" 2>/dev/null || true
  fi
fi

wait "$runner_pid"
exit_code=$?
terminal_reason=${terminal_reason:-process_exit}

{
  printf 'finished_utc=%s\n' "$(date -u +%Y-%m-%dT%H:%M:%SZ)"
  printf 'wall_seconds=%s\n' "$(( $(date +%s) - start_epoch ))"
  printf 'exit_code=%s\n' "$exit_code"
  printf 'terminal_reason=%s\n' "$terminal_reason"
  printf 'ready=%s\n' "$([ -s "$out/ready.edn" ] && echo true || echo false)"
  printf 'heap_dump=%s\n' "$([ -e "$out/java_pid${jvm_pid}.hprof" ] && echo true || echo false)"
} >> "$out/probe-metadata.txt"

if JAVA_TOOL_OPTIONS=-Xmx256m \
  jfr scrub \
    --exclude-events 'jdk.InitialEnvironmentVariable,jdk.InitialSystemProperty,jdk.SystemProcess' \
    "$out/startup.jfr" "$out/startup-sanitized.jfr" \
    > "$out/jfr-scrub.txt" 2>&1; then
  rm "$out/startup.jfr"
  JAVA_TOOL_OPTIONS=-Xmx256m \
    jfr summary "$out/startup-sanitized.jfr" > "$out/jfr-summary.txt" 2>&1 || true
else
  echo "warning: JFR scrub failed; unsanitized recording retained locally" >&2
fi
printf 'probe %s complete: ready=%s exit=%s reason=%s\n' \
  "$name" "$([ -s "$out/ready.edn" ] && echo true || echo false)" "$exit_code" "$terminal_reason"
