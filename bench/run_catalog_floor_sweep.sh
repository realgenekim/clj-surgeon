#!/usr/bin/env bash

# Counterbalanced Codex floor sweep. Server processes are started before the
# measured calls; only the client-visible static catalog differs by MCP arm.

set -euo pipefail

repo_root=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)
result_dir=${1:?usage: bench/run_catalog_floor_sweep.sh OUT_DIR}
auth_file=${BENCH_AUTH_FILE:-${CODEX_HOME:-$HOME/.codex}/auth.json}
model=${BENCH_MODEL:-gpt-5.6-sol}
reasoning=${BENCH_REASONING:-low}
blocks=${BENCH_BLOCKS:-14}
max_load=${BENCH_MAX_LOAD:-4.0}
arms=(C T D P M I R)
server_arms=(T D P M R)
mcp_arms=(T D P M I R)

command -v codex >/dev/null || { echo "codex not on PATH" >&2; exit 2; }
command -v clojure >/dev/null || { echo "clojure not on PATH" >&2; exit 2; }
command -v jq >/dev/null || { echo "jq not on PATH" >&2; exit 2; }
[ -f "$auth_file" ] || { echo "auth file missing: $auth_file" >&2; exit 2; }
[ ! -e "$result_dir" ] || { echo "output already exists: $result_dir" >&2; exit 2; }

mkdir -p "$result_dir/servers" "$result_dir/runs"
result_dir=$(cd "$result_dir" && pwd -P)
repo_root=$(cd "$repo_root" && pwd -P)
prompt='Do not use any tools. Reply with exactly one word: ok'
printf '%s' "$prompt" > "$result_dir/prompt.txt"
prompt_sha=$(shasum -a 256 "$result_dir/prompt.txt" | awk '{print $1}')
codex_bin=$(command -v codex)
codex_sha=$(shasum -a 256 "$codex_bin" | awk '{print $1}')
head=$(git -C "$repo_root" rev-parse HEAD)
tree=$(git -C "$repo_root" rev-parse HEAD^{tree})
test ! -e "$repo_root/.cpcache" || {
  echo "checkout already contains generated .cpcache" >&2
  exit 2
}

server_pids=()
server_pid_count=0
cleanup() {
  local index pid
  index=0
  while [ "$index" -lt "$server_pid_count" ]; do
    pid=${server_pids[$index]}
    kill "$pid" 2>/dev/null || true
    wait "$pid" 2>/dev/null || true
    index=$((index + 1))
  done
}
trap cleanup EXIT INT TERM

archive_generated_cache() {
  if [ -d "$repo_root/.cpcache" ]; then
    test ! -e "$result_dir/generated-repo-cpcache"
    mv "$repo_root/.cpcache" "$result_dir/generated-repo-cpcache"
  fi
}

arm_keyword() {
  case "$1" in
    T) printf '%s\n' tiny ;;
    D) printf '%s\n' description ;;
    P) printf '%s\n' parameters ;;
    M) printf '%s\n' many ;;
    R) printf '%s\n' real ;;
    *) return 2 ;;
  esac
}

write_config() {
  local path=$1 url=$2 arm=${3:-}
  {
    printf '%s\n' '[mcp_servers.catalog-probe]'
    printf 'url = "%s"\n' "$url"
    printf '%s\n' 'required = true'
    printf '%s\n' 'default_tools_approval_mode = "approve"'
    printf '%s\n' 'startup_timeout_sec = 30'
    printf '%s\n' 'tool_timeout_sec = 30'
    if [ "$arm" = I ]; then
      printf '%s\n' 'enabled_tools = ["inspect_clojure"]'
    fi
  } > "$path"
}

server_path_for_arm() {
  if [ "$1" = I ]; then
    printf '%s\n' R
  else
    printf '%s\n' "$1"
  fi
}

seed_codex_home() {
  local home=$1
  install -m 600 "$auth_file" "$home/auth.json"
  if [ -f "$(dirname "$auth_file")/installation_id" ]; then
    install -m 600 "$(dirname "$auth_file")/installation_id" "$home/installation_id"
  fi
  if [ -f "$(dirname "$auth_file")/models_cache.json" ]; then
    install -m 600 "$(dirname "$auth_file")/models_cache.json" "$home/models_cache.json"
  fi
}

read_load() {
  awk '{print $1}' /proc/loadavg
}

load_ok() {
  awk -v value="$1" -v limit="$max_load" 'BEGIN {exit !(value <= limit)}'
}

for arm in "${server_arms[@]}"; do
  server_dir="$result_dir/servers/$arm"
  mkdir -p "$server_dir"
  arm_name=$(arm_keyword "$arm")
  (cd "$repo_root"
   exec clojure -J-Xms32m -J-Xmx256m \
     -Sdeps '{:paths ["src" "dev/experiments"]}' \
     -X:clj-surgeon/mcp catalog-floor-server/start \
     :arm ":$arm_name" \
     :surface-receipt-file "\"$server_dir/advertised.json\"" \
     :project-dir "\"$repo_root\"" \
     :nrepl-port :none :port 0 \
     :ready-file "\"$server_dir/ready.edn\"" \
     > "$server_dir/stdout.txt" 2> "$server_dir/stderr.txt") &
  server_pids+=("$!")
  server_pid_count=$((server_pid_count + 1))
done

for arm in "${server_arms[@]}"; do
  server_dir="$result_dir/servers/$arm"
  for _ in $(seq 1 240); do
    [ -s "$server_dir/ready.edn" ] && break
    sleep 0.25
  done
  [ -s "$server_dir/ready.edn" ] || {
    echo "server not ready: $arm" >&2
    sed -n '1,160p' "$server_dir/stderr.txt" >&2
    exit 2
  }
  bb -e '(-> *command-line-args* first slurp clojure.edn/read-string :url println)' \
    "$server_dir/ready.edn" > "$server_dir/url.txt"
done

# Token-free Codex client projection for every MCP arm.
printf '{"C":{"client_bytes":0,"tool_count":0,"parameter_count":0}' \
  > "$result_dir/catalogs.json"
for arm in "${mcp_arms[@]}"; do
  preflight="$result_dir/servers/$arm/preflight"
  server_arm=$(server_path_for_arm "$arm")
  mkdir -p "$preflight/codex-home"
  seed_codex_home "$preflight/codex-home"
  write_config "$preflight/codex-home/config.toml" \
    "$(cat "$result_dir/servers/$server_arm/url.txt")" "$arm"
  start_ns=$(date +%s%N)
  (cd "$repo_root"
   CODEX_HOME="$preflight/codex-home" clojure -J-Xms32m -J-Xmx256m \
     -Sdeps '{:paths ["src" "bench"]}' -M \
     -m capture-codex-mcp-registry --codex "$codex_bin" \
     --output "$preflight/registry.json" --server catalog-probe \
     > "$preflight/stdout.txt" 2> "$preflight/stderr.txt")
  end_ns=$(date +%s%N)
  jq --argjson wall_ms "$(( (end_ns - start_ns) / 1000000 ))" \
    --argjson parameter_count "$(jq '[."tool-projection"[]."input-schema".properties | length] | add // 0' "$preflight/registry.json")" \
    '{client_bytes: (."tool-projection" | tojson | utf8bytelength),
      tool_count: (."tool-projection" | length),
      parameter_count: $parameter_count,
      registry_wall_ms: $wall_ms,
      client_sha256: null}' "$preflight/registry.json" > "$preflight/catalog.json"
  client_sha=$(jq -cS '."tool-projection"' "$preflight/registry.json" | shasum -a 256 | awk '{print $1}')
  jq --arg sha "$client_sha" '.client_sha256 = $sha' "$preflight/catalog.json" > "$preflight/catalog.with-sha.json"
  mv "$preflight/catalog.with-sha.json" "$preflight/catalog.json"
  printf ',"%s":' "$arm" >> "$result_dir/catalogs.json"
  jq -c . "$preflight/catalog.json" >> "$result_dir/catalogs.json"
done
printf '}\n' >> "$result_dir/catalogs.json"
jq -e 'keys == ["C","D","I","M","P","R","T"]' "$result_dir/catalogs.json" >/dev/null

if [ "${BENCH_PREFLIGHT_ONLY:-0}" = 1 ]; then
  archive_generated_cache
  jq . "$result_dir/catalogs.json"
  echo "catalog-floor token-free preflight complete: $result_dir" >&2
  exit 0
fi

cat > "$result_dir/meta.json" <<EOF
{"schema":"clj-surgeon.catalog-floor-run.v1","head":"$head","tree":"$tree","codex_version":"$(codex --version)","codex_sha256":"$codex_sha","model":"$model","reasoning":"$reasoning","blocks":$blocks,"max_load":$max_load,"prompt_sha256":"$prompt_sha","hostname":"$(hostname)","whoami":"$(id -un)"}
EOF

printf '%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\n' \
  run_id block position arm environment_valid semantic_correct route_adherent \
  total_ms init_ms answer_ms complete_ms tail_ms exit_code load_before load_after \
  > "$result_dir/runs.tsv"

schedules=(
  'C T D P M I R'
  'T D P M I R C'
  'D P M I R C T'
  'P M I R C T D'
  'M I R C T D P'
  'I R C T D P M'
  'R C T D P M I'
  'R I M P D T C'
  'C R I M P D T'
  'T C R I M P D'
  'D T C R I M P'
  'P D T C R I M'
  'M P D T C R I'
  'I M P D T C R'
)

for block in $(seq 1 "$blocks"); do
  schedule=${schedules[$(((block - 1) % ${#schedules[@]}))]}
  block_load=$(read_load)
  load_ok "$block_load" || { echo "load too high before block $block: $block_load" >&2; exit 3; }
  position=0
  for arm in $schedule; do
    position=$((position + 1))
    run_id=$(printf 'b%02d-p%02d-%s' "$block" "$position" "$arm")
    run_dir="$result_dir/runs/$run_id"
    codex_home="$run_dir/codex-home"
    mkdir -p "$codex_home"
    seed_codex_home "$codex_home"
    if [ "$arm" != C ]; then
      server_arm=$(server_path_for_arm "$arm")
      write_config "$codex_home/config.toml" \
        "$(cat "$result_dir/servers/$server_arm/url.txt")" "$arm"
    fi
    load_before=$(read_load)
    start_ns=$(date +%s%N)
    set +e
    CODEX_HOME="$codex_home" codex exec --json --ephemeral --ignore-rules \
      --skip-git-repo-check --sandbox read-only --color never \
      -m "$model" -c "model_reasoning_effort=\"$reasoning\"" \
      -C "$repo_root" - < "$result_dir/prompt.txt" \
      2> "$run_dir/stderr.txt" \
      | while IFS= read -r line; do printf '%s\t%s\n' "$(date +%s%N)" "$line"; done \
      > "$run_dir/events.jsonl"
    exit_code=${PIPESTATUS[0]}
    set -e
    end_ns=$(date +%s%N)
    load_after=$(read_load)

    first_turn=$(awk -F '\t' '$2 ~ /"type":"thread.started"|"type":"turn.started"/ {print $1; exit}' "$run_dir/events.jsonl")
    answer=$(awk -F '\t' '$2 ~ /"type":"item.completed"/ && $2 ~ /"agent_message"/ {value=$1} END {print value}' "$run_dir/events.jsonl")
    completed=$(awk -F '\t' '$2 ~ /"type":"turn.completed"/ {value=$1} END {print value}' "$run_dir/events.jsonl")
    [ -n "$first_turn" ] || first_turn=$start_ns
    [ -n "$answer" ] || answer=$end_ns
    [ -n "$completed" ] || completed=$end_ns
    total_ms=$(( (end_ns - start_ns) / 1000000 ))
    init_ms=$(( (first_turn - start_ns) / 1000000 ))
    answer_ms=$(( (answer - first_turn) / 1000000 ))
    complete_ms=$(( (completed - first_turn) / 1000000 ))
    tail_ms=$(( (end_ns - completed) / 1000000 ))

    jq -R 'capture("^[^\\t]*\\t(?<json>.*)$").json | fromjson' \
      "$run_dir/events.jsonl" > "$run_dir/events.raw.jsonl"
    final=$(jq -s -r '[.[] | select(.type == "item.completed" and .item.type == "agent_message")][-1].item.text // ""' "$run_dir/events.raw.jsonl")
    tool_calls=$(jq -s '[.[] | select(.type == "item.started" and .item.type == "mcp_tool_call")] | length' "$run_dir/events.raw.jsonl")
    shell_calls=$(jq -s '[.[] | select(.type == "item.started" and .item.type == "command_execution")] | length' "$run_dir/events.raw.jsonl")
    file_changes=$(jq -s '[.[] | select(.type == "item.started" and .item.type == "file_change")] | length' "$run_dir/events.raw.jsonl")
    semantic_correct=false
    [ "$exit_code" -eq 0 ] && [ "$final" = ok ] && semantic_correct=true
    route_adherent=false
    [ "$tool_calls" -eq 0 ] && [ "$shell_calls" -eq 0 ] && [ "$file_changes" -eq 0 ] && route_adherent=true
    environment_valid=false
    if load_ok "$load_before" && load_ok "$load_after" \
      && [ "$(codex --version)" = "$(jq -r .codex_version "$result_dir/meta.json")" ] \
      && [ "$(shasum -a 256 "$codex_bin" | awk '{print $1}')" = "$codex_sha" ]; then
      environment_valid=true
    fi
    printf '%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\n' \
      "$run_id" "$block" "$position" "$arm" "$environment_valid" "$semantic_correct" "$route_adherent" \
      "$total_ms" "$init_ms" "$answer_ms" "$complete_ms" "$tail_ms" "$exit_code" "$load_before" "$load_after" \
      >> "$result_dir/runs.tsv"
    printf 'catalog-floor %-12s total=%sms init=%sms answer=%sms correct=%s route=%s\n' \
      "$run_id" "$total_ms" "$init_ms" "$answer_ms" "$semantic_correct" "$route_adherent" >&2
  done
done

python3 "$repo_root/dev/experiments/catalog_floor_score.py" "$result_dir" > "$result_dir/score.stdout.json"
archive_generated_cache
test "$(git -C "$repo_root" rev-parse HEAD)" = "$head"
test "$(git -C "$repo_root" rev-parse HEAD^{tree})" = "$tree"
test -z "$(git -C "$repo_root" status --short)"
echo "catalog-floor complete: $result_dir" >&2
