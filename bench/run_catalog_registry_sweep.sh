#!/usr/bin/env bash

# Token-free companion to run_catalog_floor_sweep.sh. It times only Codex
# app-server initialization and static MCP registry ingestion.

set -euo pipefail

repo_root=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd -P)
result_dir=${1:?usage: bench/run_catalog_registry_sweep.sh OUT_DIR}
auth_file=${BENCH_AUTH_FILE:-${CODEX_HOME:-$HOME/.codex}/auth.json}
max_load=${BENCH_MAX_LOAD:-4.0}
server_arms=(T D P M R)
mcp_arms=(T D P M I R)
blocks=${BENCH_REGISTRY_BLOCKS:-14}
codex_bin=$(command -v codex)
expected="$repo_root/dev/experiments/catalog_floor_expected.json"

[ -f "$auth_file" ] || { echo "auth file missing: $auth_file" >&2; exit 2; }
[ ! -e "$result_dir" ] || { echo "output already exists: $result_dir" >&2; exit 2; }
[ ! -e "$repo_root/.cpcache" ] || { echo "checkout contains .cpcache" >&2; exit 2; }
mkdir -p "$result_dir/servers" "$result_dir/runs"
result_dir=$(cd "$result_dir" && pwd -P)

server_pids=()
server_pid_count=0
cleanup() {
  local index=0 pid
  while [ "$index" -lt "$server_pid_count" ]; do
    pid=${server_pids[$index]}
    kill "$pid" 2>/dev/null || true
    wait "$pid" 2>/dev/null || true
    index=$((index + 1))
  done
}
trap cleanup EXIT INT TERM

arm_keyword() {
  case "$1" in
    T) echo tiny ;; D) echo description ;; P) echo parameters ;;
    M) echo many ;; R) echo real ;; *) return 2 ;;
  esac
}

server_path_for_arm() { [ "$1" = I ] && echo R || echo "$1"; }

seed_home() {
  local home=$1 source_dir
  source_dir=$(dirname "$auth_file")
  install -m 600 "$auth_file" "$home/auth.json"
  [ ! -f "$source_dir/installation_id" ] || install -m 600 "$source_dir/installation_id" "$home/installation_id"
  [ ! -f "$source_dir/models_cache.json" ] || install -m 600 "$source_dir/models_cache.json" "$home/models_cache.json"
}

write_config() {
  local path=$1 url=$2 arm=$3
  {
    echo '[mcp_servers.catalog-probe]'
    printf 'url = "%s"\n' "$url"
    echo 'required = true'
    echo 'startup_timeout_sec = 30'
    [ "$arm" != I ] || echo 'enabled_tools = ["inspect_clojure"]'
  } > "$path"
}

read_load() { awk '{print $1}' /proc/loadavg; }
load_ok() { awk -v v="$1" -v m="$max_load" 'BEGIN {exit !(v <= m)}'; }
wait_green() {
  local attempt=0 value
  while [ "$attempt" -lt 180 ]; do
    value=$(read_load)
    if load_ok "$value"; then echo "$value"; return 0; fi
    attempt=$((attempt + 1)); sleep 1
  done
  return 1
}

for arm in "${server_arms[@]}"; do
  server_dir="$result_dir/servers/$arm"; mkdir -p "$server_dir"
  (cd "$repo_root"
   exec clojure -J-Xms32m -J-Xmx256m \
     -Sdeps '{:paths ["src" "dev/experiments"]}' \
     -X:clj-surgeon/mcp catalog-floor-server/start \
     :arm ":$(arm_keyword "$arm")" \
     :surface-receipt-file "\"$server_dir/advertised.json\"" \
     :project-dir "\"$repo_root\"" :nrepl-port :none :port 0 \
     :ready-file "\"$server_dir/ready.edn\"" \
     > "$server_dir/stdout.txt" 2> "$server_dir/stderr.txt") &
  server_pids+=("$!"); server_pid_count=$((server_pid_count + 1))
done
for arm in "${server_arms[@]}"; do
  for _ in $(seq 1 240); do [ -s "$result_dir/servers/$arm/ready.edn" ] && break; sleep 0.25; done
  [ -s "$result_dir/servers/$arm/ready.edn" ] || exit 2
  bb -e '(-> *command-line-args* first slurp clojure.edn/read-string :url println)' \
    "$result_dir/servers/$arm/ready.edn" > "$result_dir/servers/$arm/url.txt"
done

schedules=(
  'C T D P M I R' 'T D P M I R C' 'D P M I R C T' 'P M I R C T D'
  'M I R C T D P' 'I R C T D P M' 'R C T D P M I' 'R I M P D T C'
  'C R I M P D T' 'T C R I M P D' 'D T C R I M P' 'P D T C R I M'
  'M P D T C R I' 'I M P D T C R')

printf '%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\n' \
  run_id block position arm environment_valid semantic_correct route_adherent \
  total_ms init_ms answer_ms complete_ms tail_ms exit_code load_before load_after > "$result_dir/runs.tsv"
cp "$expected" "$result_dir/catalogs.json"

for block in $(seq 1 "$blocks"); do
  wait_green >/dev/null || exit 3
  position=0
  for arm in ${schedules[$((block - 1))]}; do
    position=$((position + 1)); run_id=$(printf 'b%02d-p%02d-%s' "$block" "$position" "$arm")
    run_dir="$result_dir/runs/$run_id"; home="$run_dir/codex-home"; mkdir -p "$home"
    seed_home "$home"; server=""; expected_count=0
    if [ "$arm" != C ]; then
      server_arm=$(server_path_for_arm "$arm"); server=catalog-probe
      expected_count=$(jq -r --arg arm "$arm" '.[$arm].tool_count' "$expected")
      write_config "$home/config.toml" "$(cat "$result_dir/servers/$server_arm/url.txt")" "$arm"
    fi
    load_before=$(read_load); set +e
    server=catalog-probe
    CODEX_HOME="$home" python3 "$repo_root/dev/experiments/catalog_floor_registry_probe.py" \
      --codex "$codex_bin" --output "$run_dir/receipt.json" \
      --expected-count "$expected_count" --server "$server"
    rc=$?; set -e; load_after=$(read_load)
    ok=$(jq -r '.ok' "$run_dir/receipt.json")
    bytes=$(jq -r '.client_bytes' "$run_dir/receipt.json")
    sha=$(jq -r '.client_sha256' "$run_dir/receipt.json")
    expected_bytes=$(jq -r --arg arm "$arm" '.[$arm].client_bytes' "$expected")
    expected_sha=$(jq -r --arg arm "$arm" '.[$arm].client_sha256 // ""' "$expected")
    evidence_ok=false
    [ "$ok" = true ] && [ "$bytes" = "$expected_bytes" ] && [ "$sha" = "$expected_sha" ] && evidence_ok=true
    environment_valid=false
    load_ok "$load_before" && load_ok "$load_after" && environment_valid=true
    total=$(jq -r '.process_to_registry_ms' "$run_dir/receipt.json")
    init=$(jq -r '.process_to_initialize_ms' "$run_dir/receipt.json")
    registry=$(jq -r '.initialize_to_registry_ms' "$run_dir/receipt.json")
    printf '%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\n' \
      "$run_id" "$block" "$position" "$arm" "$environment_valid" "$evidence_ok" true \
      "$total" "$init" "$registry" "$registry" 0 "$rc" "$load_before" "$load_after" >> "$result_dir/runs.tsv"
    printf 'registry %-12s total=%sms init=%sms registry=%sms ok=%s\n' "$run_id" "$total" "$init" "$registry" "$evidence_ok" >&2
  done
done

python3 "$repo_root/dev/experiments/catalog_floor_score.py" "$result_dir" > "$result_dir/score.stdout.json"
[ ! -d "$repo_root/.cpcache" ] || mv "$repo_root/.cpcache" "$result_dir/generated-repo-cpcache"
test -z "$(git -C "$repo_root" status --short)"
echo "catalog registry sweep complete: $result_dir" >&2
