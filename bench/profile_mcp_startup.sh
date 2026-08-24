#!/usr/bin/env bash
set -euo pipefail

repo_root=$(cd "$(dirname "$0")/.." && pwd -P)

if [ "$#" -ne 2 ] && [ "$#" -ne 3 ]; then
  echo "usage: $0 NAME XMX_MIB [RESULT_ROOT]" >&2
  exit 64
fi

name=$1
xmx_mib=$2
result_root=${3:-/tmp/clj-surgeon-mcp-startup-profile}
out="$result_root/$name"

case "$name" in
  *[!a-zA-Z0-9._-]*|'')
    echo "NAME must contain only letters, digits, dot, underscore, or dash: $name" >&2
    exit 64
    ;;
esac
if ! [[ "$xmx_mib" =~ ^[1-9][0-9]*$ ]]; then
  echo "XMX_MIB must be a positive integer: $xmx_mib" >&2
  exit 64
fi
if [ "$PWD" != "$repo_root" ]; then
  echo "refusing: run from $repo_root" >&2
  exit 64
fi
if [ -e "$out" ]; then
  echo "refusing to replace existing profile: $out" >&2
  exit 73
fi

for command_name in awk bb clj-nrepl-eval clojure curl jcmd pgrep ps perl rg; do
  command -v "$command_name" >/dev/null 2>&1 || {
    echo "missing required command: $command_name" >&2
    exit 69
  }
done

mkdir -p "$out/telemetry"
ready_file="$out/ready.edn"
port_file="$out/nrepl-port"
time_file="$out/time.txt"

cleanup() {
  if [ -n "${jvm_pid:-}" ] && kill -0 "$jvm_pid" 2>/dev/null; then
    kill -TERM "$jvm_pid" 2>/dev/null || true
  fi
  if [ -n "${runner_pid:-}" ]; then
    wait "$runner_pid" 2>/dev/null || true
  fi
}
trap cleanup EXIT INT TERM

started_ms=$(perl -MTime::HiRes=time -e 'printf "%.0f\n", time()*1000')
printf '%s\t%s\n' \
  cwd "$repo_root" \
  xmx_mib "$xmx_mib" \
  started_utc "$(date -u +%Y-%m-%dT%H:%M:%SZ)" \
  > "$out/metadata.tsv"
printf '%s\n' 'elapsed_ms	rss_kib	vsz_kib	state' > "$out/rss-samples.tsv"

/usr/bin/time -l -o "$time_file" \
  clojure -J-Xms32m "-J-Xmx${xmx_mib}m" -X:clj-surgeon/mcp \
    :project-dir "\"$repo_root\"" \
    :port 0 \
    :telemetry :full \
    :telemetry-dir "\"$out/telemetry\"" \
    :run-id "\"startup-profile-$name\"" \
    :nrepl-port 0 \
    :port-file "\"$port_file\"" \
    :ready-file "\"$ready_file\"" \
    :log-file "\"$out/application.log\"" \
    > "$out/stdout.log" 2> "$out/stderr.log" &
runner_pid=$!
jvm_pid=""
for _attempt in $(seq 1 100); do
  candidates=$(pgrep -P "$runner_pid" 2>/dev/null || true)
  for candidate in $candidates; do
    grandchildren=$(pgrep -P "$candidate" 2>/dev/null || true)
    for process_pid in $candidate $grandchildren; do
      if ps -p "$process_pid" -o comm= 2>/dev/null | rg -q '(^|/)java$'; then
        jvm_pid=$process_pid
        break 3
      fi
    done
  done
  kill -0 "$runner_pid" 2>/dev/null || break
  sleep 0.05
done
if [ -z "$jvm_pid" ]; then
  echo "could not resolve startup JVM beneath runner $runner_pid; inspect $out" >&2
  exit 1
fi
printf '%s\t%s\n' runner_pid "$runner_pid" jvm_pid "$jvm_pid" >> "$out/metadata.tsv"

ready=false
for _attempt in $(seq 1 600); do
  now_ms=$(perl -MTime::HiRes=time -e 'printf "%.0f\n", time()*1000')
  if kill -0 "$jvm_pid" 2>/dev/null; then
    read -r rss_kib vsz_kib state < <(ps -p "$jvm_pid" -o rss=,vsz=,state= | awk '{$1=$1; print}') || true
    if [ -n "${rss_kib:-}" ]; then
      printf '%s\t%s\t%s\t%s\n' "$((now_ms - started_ms))" "$rss_kib" "$vsz_kib" "$state" \
        >> "$out/rss-samples.tsv"
    fi
  else
    break
  fi
  if [ -s "$ready_file" ] && [ -s "$port_file" ]; then
    ready=true
    break
  fi
  sleep 0.1
done

if [ "$ready" != true ]; then
  printf '%s\t%s\n' ready false >> "$out/metadata.tsv"
  wait "$runner_pid" || true
  jvm_pid=""
  echo "MCP did not become ready; inspect $out" >&2
  exit 1
fi

ready_ms=$(perl -MTime::HiRes=time -e 'printf "%.0f\n", time()*1000')
mcp_url=$(bb -e '(-> *command-line-args* first slurp clojure.edn/read-string :url println)' "$ready_file")
curl --fail --silent --show-error "${mcp_url%/mcp}/healthz" > "$out/health.txt"
jcmd "$jvm_pid" GC.heap_info > "$out/heap-ready.txt"
jcmd "$jvm_pid" VM.native_memory summary scale=MB > "$out/nmt-ready.txt" 2>&1 || true

nrepl_port=$(<"$port_file")
reload_result=$(clj-nrepl-eval --port "$nrepl_port" \
  "(do (require 'clj-surgeon.mcp-tool :reload) (clj-surgeon.mcp-server/sync-tools!))")
printf '%s\n' "$reload_result" > "$out/reload.edn"
case "$reload_result" in
  *":ok true"*) ;;
  *)
    echo "hot reload probe failed; inspect $out/reload.edn" >&2
    exit 1
    ;;
esac

jcmd "$jvm_pid" GC.run > "$out/gc-run.txt"
jcmd "$jvm_pid" GC.heap_info > "$out/heap-after-gc.txt"
sleep 1

finished_ms=$(perl -MTime::HiRes=time -e 'printf "%.0f\n", time()*1000')
peak_rss_kib=$(awk 'NR > 1 && $2 > peak {peak=$2} END {print peak+0}' "$out/rss-samples.tsv")
printf '%s\t%s\n' \
  ready true \
  ready_ms "$((ready_ms - started_ms))" \
  peak_rss_kib "$peak_rss_kib" \
  hot_reload true \
  wall_ms "$((finished_ms - started_ms))" \
  >> "$out/metadata.tsv"

kill -TERM "$jvm_pid"
wait "$runner_pid" || true
jvm_pid=""
runner_pid=""
trap - EXIT INT TERM

echo "profile complete: name=$name xmx=${xmx_mib}MiB ready_ms=$((ready_ms - started_ms)) peak_rss_kib=$peak_rss_kib hot_reload=true cwd=$repo_root"
