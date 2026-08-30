#!/usr/bin/env bash
set -euo pipefail

repo_root=$(cd "$(dirname "$0")/.." && pwd -P)
product_root=""
result_root=""
auth_file=${CODEX_AUTH_FILE:-$HOME/.codex/auth.json}
timeout_seconds=180
reasoning=high
sol_model=gpt-5.6-sol
expected_product=c55de2279826af5ed21c90981591479dd2e802b2
spark_aliases=(gpt-5.3-spark 5.3-spark spark gpt-5.3-codex-spark)
mcp_pid=""

usage() {
  printf '%s\n' \
    'Usage: bench/run_spark_caller_screen.sh --product-root DIR --output DIR [options]' \
    '' \
    '  --auth-file FILE   Existing ChatGPT subscription auth.json' \
    '  --timeout SEC      Per-model-call timeout (default: 180)'
}

while [ "$#" -gt 0 ]; do
  case "$1" in
    --product-root) product_root=${2:?--product-root requires a value}; shift 2 ;;
    --output) result_root=${2:?--output requires a value}; shift 2 ;;
    --auth-file) auth_file=${2:?--auth-file requires a value}; shift 2 ;;
    --timeout) timeout_seconds=${2:?--timeout requires a value}; shift 2 ;;
    -h|--help) usage; exit 0 ;;
    *) printf 'Unknown option: %s\n' "$1" >&2; usage >&2; exit 2 ;;
  esac
done

[ -n "$product_root" ] && [ -n "$result_root" ] || { usage >&2; exit 2; }
[[ "$timeout_seconds" =~ ^[1-9][0-9]*$ ]] || { printf '%s\n' 'timeout must be positive' >&2; exit 2; }
for command_name in bb clojure codex curl git jq perl shasum tar; do
  command -v "$command_name" >/dev/null || { printf 'Missing command: %s\n' "$command_name" >&2; exit 2; }
done
[ -f "$auth_file" ] || { printf 'Auth file not found: %s\n' "$auth_file" >&2; exit 2; }
[ ! -e "$result_root" ] || { printf 'Output already exists: %s\n' "$result_root" >&2; exit 2; }

product_root=$(cd "$product_root" && pwd -P)
[ "$(git -C "$product_root" rev-parse HEAD)" = "$expected_product" ] || {
  printf '%s\n' 'Product worktree is not at the frozen commit' >&2
  exit 2
}
git -C "$product_root" diff --quiet -- . || {
  printf '%s\n' 'Product worktree has tracked changes' >&2
  exit 2
}

mkdir -p "$result_root"
result_root=$(cd "$result_root" && pwd -P)

cleanup() {
  if [ -n "$mcp_pid" ]; then
    kill "$mcp_pid" 2>/dev/null || true
    wait "$mcp_pid" 2>/dev/null || true
    mcp_pid=""
  fi
}
trap cleanup EXIT INT TERM

now_ms() {
  perl -MTime::HiRes=time -e 'printf "%.0f\n", time()*1000'
}

source_manifest() {
  local workspace=$1 output=$2
  (
    cd "$workspace"
    find . -type f \( -name '*.clj' -o -name '*.cljs' -o -name '*.cljc' \) -print \
      | LC_ALL=C sort \
      | while IFS= read -r path; do
          shasum -a 256 "$path"
        done
  ) > "$output"
}

last_message() {
  jq -s -r '[.[] | select(.type == "item.completed" and .item.type == "agent_message")][-1].item.text // ""' "$1"
}

run_codex() {
  local run_dir=$1 model=$2 workspace=$3 prompt=$4 sandbox=$5 use_mcp=$6
  local codex_home="$run_dir/codex-home"
  local start_ms end_ms deadline exit_code=0
  mkdir -p "$run_dir" "$codex_home" "$workspace"
  ln -s "$auth_file" "$codex_home/auth.json"
  printf '%s\n' "$prompt" > "$run_dir/prompt.txt"
  printf '%s\n' "$model" > "$run_dir/requested-model.txt"

  local args=(exec --json --ephemeral --ignore-rules --skip-git-repo-check
              --sandbox "$sandbox" --color never -m "$model"
              -c "model_reasoning_effort=\"$reasoning\"" -C "$workspace")
  if [ "$use_mcp" = false ]; then
    args+=(--ignore-user-config)
  fi

  start_ms=$(now_ms)
  set +e
  CODEX_HOME="$codex_home" codex "${args[@]}" "$prompt" \
    > "$run_dir/events.jsonl" 2> "$run_dir/stderr.txt" </dev/null &
  local codex_pid=$!
  deadline=$((SECONDS + timeout_seconds))
  while kill -0 "$codex_pid" 2>/dev/null; do
    if [ "$SECONDS" -ge "$deadline" ]; then
      kill "$codex_pid" 2>/dev/null || true
      sleep 1
      kill -9 "$codex_pid" 2>/dev/null || true
      exit_code=124
      break
    fi
    sleep 0.1
  done
  if [ "$exit_code" -ne 124 ]; then
    wait "$codex_pid"
    exit_code=$?
  else
    wait "$codex_pid" 2>/dev/null || true
  fi
  set -e
  end_ms=$(now_ms)
  printf '%s\n' "$exit_code" > "$run_dir/exit-code.txt"
  printf '%s\n' "$((end_ms - start_ms))" > "$run_dir/wall-ms.txt"
  last_message "$run_dir/events.jsonl" > "$run_dir/final.txt"
}

start_mcp() {
  local run_dir=$1 workspace=$2
  local ready_file="$run_dir/mcp-ready.edn"
  local started_ms ready_ms mcp_url
  mkdir -p "$run_dir/mcp-telemetry"
  started_ms=$(now_ms)
  (
    cd "$product_root"
    exec clojure -J-Xms64m -J-Xmx512m -X:clj-surgeon/mcp \
      :project-dir "$(bb -e '(prn (first *command-line-args*))' "$workspace")" \
      :telemetry :full \
      :telemetry-dir "$(bb -e '(prn (first *command-line-args*))' "$run_dir/mcp-telemetry")" \
      :run-id "$(bb -e '(prn (first *command-line-args*))' "$(basename "$run_dir")")" \
      :nrepl-port :none :port 0 \
      :ready-file "$(bb -e '(prn (first *command-line-args*))' "$ready_file")"
  ) > "$run_dir/mcp-server.stdout" 2> "$run_dir/mcp-server.stderr" &
  mcp_pid=$!
  for _ in $(seq 1 240); do
    [ -s "$ready_file" ] && break
    kill -0 "$mcp_pid" 2>/dev/null || {
      sed -n '1,160p' "$run_dir/mcp-server.stderr" >&2
      return 2
    }
    sleep 0.25
  done
  [ -s "$ready_file" ] || { printf '%s\n' 'MCP readiness timeout' >&2; return 2; }
  mcp_url=$(bb -e '(-> *command-line-args* first slurp clojure.edn/read-string :url println)' "$ready_file")
  curl --fail --silent --show-error "${mcp_url%/mcp}/healthz" >/dev/null
  ready_ms=$(now_ms)
  printf '%s\n' "$((ready_ms - started_ms))" > "$run_dir/mcp-bootstrap-ms.txt"
  bb "$product_root/bench/write_mcp_config.clj" "$run_dir/codex-home/config.toml" \
    --url "$mcp_url" --enabled-tools-edn \
    '["inspect_clojure","apply_clojure_changes","edit_clojure","transform_clojure"]' >/dev/null
}

stop_mcp() {
  cleanup
}

record_run() {
  local cell=$1 replicate=$2 run_dir=$3 expected_dir=$4
  local usage input cached output reasoning_tokens exact wrong one_shot route
  local inspect_calls edit_calls mcp_calls command_calls file_changes actions failures
  local operationless=false omitted_ids=false error_types tool_ms
  source_manifest "$run_dir/workspace" "$run_dir/source-final.sha256"
  source_manifest "$expected_dir" "$run_dir/source-expected.sha256"
  exact=false
  if cmp -s "$run_dir/source-final.sha256" "$run_dir/source-expected.sha256"; then
    exact=true
  fi
  wrong=1
  [ "$exact" = true ] && wrong=0

  usage=$(jq -s '[.[] | select(.type == "turn.completed")][-1].usage // {}' "$run_dir/events.jsonl")
  input=$(jq -r '.input_tokens // 0' <<< "$usage")
  cached=$(jq -r '.cached_input_tokens // 0' <<< "$usage")
  output=$(jq -r '.output_tokens // 0' <<< "$usage")
  reasoning_tokens=$(jq -r '.reasoning_output_tokens // 0' <<< "$usage")
  inspect_calls=$(jq -s '[.[] | select(.type == "item.started" and .item.type == "mcp_tool_call" and .item.tool == "inspect_clojure")] | length' "$run_dir/events.jsonl")
  edit_calls=$(jq -s '[.[] | select(.type == "item.started" and .item.type == "mcp_tool_call" and .item.tool == "edit_clojure")] | length' "$run_dir/events.jsonl")
  mcp_calls=$(jq -s '[.[] | select(.type == "item.started" and .item.type == "mcp_tool_call")] | length' "$run_dir/events.jsonl")
  command_calls=$(jq -s '[.[] | select(.type == "item.started" and .item.type == "command_execution")] | length' "$run_dir/events.jsonl")
  file_changes=$(jq -s '[.[] | select(.type == "item.started" and .item.type == "file_change")] | length' "$run_dir/events.jsonl")
  actions=$((mcp_calls + command_calls + file_changes))
  failures=$(jq -s '[.[]
    | select(.type == "item.completed" and .item.type == "mcp_tool_call")
    | {status: .item.status,
       content: (.item.result.structured_content // .item.result.structuredContent // {})}
    | select(.status == "failed" or .content.ok == false or .content.error_type != null)]
    | length' "$run_dir/events.jsonl")
  error_types=$(jq -s -r '[.[] | select(.type == "item.completed" and .item.type == "mcp_tool_call") | (.item.result.structured_content.error_type // .item.result.structuredContent.error_type // empty)] | unique | join(",")' "$run_dir/events.jsonl")
  tool_ms=$(jq -s '[.[] | select(.type == "item.completed" and .item.type == "mcp_tool_call") | (.item.result.structured_content.elapsed_ms // .item.result.structuredContent.elapsed_ms // 0)] | add // 0' "$run_dir/events.jsonl")

  if [ "$inspect_calls" -gt 0 ]; then
    operationless=$(jq -s -r '[.[] | select(.type == "item.started" and .item.type == "mcp_tool_call" and .item.tool == "inspect_clojure")][0].item.arguments.requests[0] | if type == "object" then (has("operation") | not) else false end' "$run_dir/events.jsonl")
    omitted_ids=$(jq -s -r '[.[] | select(.type == "item.started" and .item.type == "mcp_tool_call" and .item.tool == "inspect_clojure")][0].item.arguments.requests | if type == "array" then all(.[]; has("id") | not) else false end' "$run_dir/events.jsonl")
  fi

  route=none
  if [ "$edit_calls" -gt 0 ] && { [ "$command_calls" -gt 0 ] || [ "$file_changes" -gt 0 ]; }; then
    route=mixed
  elif [ "$edit_calls" -gt 0 ]; then
    route=surgeon
  elif [ "$command_calls" -gt 0 ] || [ "$file_changes" -gt 0 ]; then
    route=native
  fi

  one_shot=false
  case "$cell" in
    read)
      [ "$exact" = true ] && [ "$inspect_calls" -eq 1 ] && [ "$mcp_calls" -eq 1 ] \
        && [ "$command_calls" -eq 0 ] && [ "$file_changes" -eq 0 ] && [ "$failures" -eq 0 ] \
        && one_shot=true
      ;;
    write)
      [ "$exact" = true ] && [ "$edit_calls" -eq 1 ] && [ "$mcp_calls" -eq 1 ] \
        && [ "$command_calls" -eq 0 ] && [ "$file_changes" -eq 0 ] && [ "$failures" -eq 0 ] \
        && one_shot=true
      ;;
    recovery)
      [ "$exact" = true ] && [ "$edit_calls" -eq 2 ] && [ "$mcp_calls" -eq 2 ] \
        && [ "$command_calls" -eq 0 ] && [ "$file_changes" -eq 0 ] && [ "$failures" -eq 1 ] \
        && [ "$error_types" = expect-count-mismatch ] && one_shot=true
      ;;
    chord)
      [ "$exact" = true ] && [ "$failures" -eq 0 ] && [ "$actions" -eq 1 ] \
        && one_shot=true
      ;;
  esac

  printf '%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\n' \
    "$cell" "$replicate" "$(cat "$run_dir/requested-model.txt")" \
    "$(cat "$run_dir/wall-ms.txt")" "$(cat "$run_dir/exit-code.txt")" \
    "$input" "$cached" "$output" "$reasoning_tokens" "$actions" \
    "$mcp_calls" "$inspect_calls" "$edit_calls" "$command_calls" "$file_changes" \
    "$failures" "$error_types" "$route" "$one_shot" "$exact" "$wrong" \
    "$operationless/$omitted_ids/$tool_ms" >> "$result_root/runs.tsv"
}

printf '%s\t%s\n' \
  schema clj-surgeon.spark-caller-screen.v1 \
  started_utc "$(date -u +%Y-%m-%dT%H:%M:%SZ)" \
  product_commit "$expected_product" \
  product_tree "$(git -C "$product_root" rev-parse 'HEAD^{tree}')" \
  experiment_commit "$(git -C "$repo_root" rev-parse HEAD)" \
  experiment_tree "$(git -C "$repo_root" write-tree)" \
  harness_sha256 "$(shasum -a 256 "$repo_root/bench/run_spark_caller_screen.sh" | awk '{print $1}')" \
  codex_version "$(codex --version)" \
  reasoning "$reasoning" \
  route subscription-chatgpt > "$result_root/identity.tsv"

catalog_home="$result_root/catalog-codex-home"
mkdir -p "$catalog_home"
ln -s "$auth_file" "$catalog_home/auth.json"
CODEX_HOME="$catalog_home" codex debug models > "$result_root/model-catalog.json" 2> "$result_root/model-catalog.stderr"
jq -r '.models[].slug' "$result_root/model-catalog.json" \
  | LC_ALL=C sort -u > "$result_root/catalog-model-identifiers.txt"

accepted_spark=""
mkdir -p "$result_root/availability"
for alias in "${spark_aliases[@]}"; do
  safe_alias=${alias//\//_}
  run_dir="$result_root/availability/$safe_alias"
  workspace="$run_dir/workspace"
  run_codex "$run_dir" "$alias" "$workspace" 'Reply with exactly: SPARK_OK' read-only false
  if [ "$(cat "$run_dir/exit-code.txt")" -eq 0 ] \
    && [ "$(cat "$run_dir/final.txt")" = SPARK_OK ]; then
    accepted_spark=$alias
    break
  fi
done
printf '%s\n' "$accepted_spark" > "$result_root/accepted-spark-model.txt"

if [ -z "$accepted_spark" ]; then
  printf '%s\n' unavailable > "$result_root/status.txt"
  printf '%s\n' \
    "bash bench/run_spark_caller_screen.sh --product-root '$product_root' --output '<new-output-dir>' --auth-file '$auth_file'" \
    > "$result_root/replay-command.txt"
  exit 3
fi

printf '%s\n' \
  $'cell\treplicate\tmodel\twall_ms\texit_code\tinput_tokens\tcached_input_tokens\toutput_tokens\treasoning_tokens\tactions\tmcp_calls\tinspect_calls\tedit_calls\tcommand_calls\tfile_changes\tmcp_failures\terror_types\troute\tone_shot\texact\twrong_subject\tread_shorthand_operationless_omittedids_toolms' \
  > "$result_root/runs.tsv"
printf '%s\n' \
  $'pair\tposition\tmodel\twall_ms\texit_code\tinput_tokens\tcached_input_tokens\toutput_tokens\treasoning_tokens\tfinal_exact' \
  > "$result_root/baseline.tsv"

baseline_models=("$accepted_spark" "$sol_model" "$sol_model" "$accepted_spark" "$accepted_spark" "$sol_model")
baseline_pairs=(1 1 2 2 3 3)
for index in 0 1 2 3 4 5; do
  model=${baseline_models[$index]}
  pair=${baseline_pairs[$index]}
  run_dir="$result_root/baseline/$(printf '%02d-p%s-%s' "$((index + 1))" "$pair" "${model//\//_}")"
  workspace="$run_dir/workspace"
  prompt="Reply with exactly: BASELINE_${pair}"
  run_codex "$run_dir" "$model" "$workspace" "$prompt" read-only false
  usage=$(jq -s '[.[] | select(.type == "turn.completed")][-1].usage // {}' "$run_dir/events.jsonl")
  final_exact=false
  [ "$(cat "$run_dir/final.txt")" = "BASELINE_${pair}" ] && final_exact=true
  printf '%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\n' \
    "$pair" "$((index + 1))" "$model" "$(cat "$run_dir/wall-ms.txt")" \
    "$(cat "$run_dir/exit-code.txt")" "$(jq -r '.input_tokens // 0' <<< "$usage")" \
    "$(jq -r '.cached_input_tokens // 0' <<< "$usage")" "$(jq -r '.output_tokens // 0' <<< "$usage")" \
    "$(jq -r '.reasoning_output_tokens // 0' <<< "$usage")" "$final_exact" \
    >> "$result_root/baseline.tsv"
done

run_surgeon_cell() {
  local cell=$1 replicate=$2 fixture=$3 expected=$4 prompt=$5
  local run_dir="$result_root/surgeon/$cell/r$replicate"
  local workspace="$run_dir/workspace"
  mkdir -p "$run_dir/codex-home" "$workspace"
  cp -R "$fixture/." "$workspace/"
  source_manifest "$workspace" "$run_dir/source-before.sha256"
  start_mcp "$run_dir" "$workspace"
  run_codex "$run_dir" "$accepted_spark" "$workspace" "$prompt" workspace-write true
  stop_mcp
  record_run "$cell" "$replicate" "$run_dir" "$expected"
}

single_before="$product_root/bench/fixtures/edit_portfolio/exact-nested-edit/before"
single_after="$product_root/bench/fixtures/edit_portfolio/exact-nested-edit/after"
chord_before="$product_root/bench/fixtures/edit_portfolio/decision-batch-edit/before"
chord_after="$product_root/bench/fixtures/edit_portfolio/decision-batch-edit/after"
read_prompt='Use inspect_clojure exactly once to return the exact complete top-level forms route-event and normalize-record from src/bench/pair_view.clj. Do not use shell commands, native file readers, or any mutation tool. After a successful complete read, reply exactly READ_OK.'
write_prompt='The decision is complete. Use edit_clojure exactly once, with no preflight read or shell command, to edit src/bench/pair_view.clj within the top-level form route-event: replace exact form :done with :complete, matches=1. Preserve every unrelated byte. Treat verification_complete=true as terminal and reply exactly WRITE_OK.'
recovery_prompt='Exercise the refusal recovery contract while making this exact change in src/bench/pair_view.clj within route-event: replace :done with :complete. Your first edit_clojure call must deliberately use matches=2. When it refuses, recover only from the complete refusal result: do not read source or use shell, and issue one corrected edit_clojure retry. Stop after terminal success and reply exactly RECOVERY_OK.'
chord_prompt="$(cat "$product_root/bench/fixtures/edit_portfolio/decision-batch-edit/task.txt")

Both native source inspection/editing and the available clj-surgeon tools are permitted. Choose whichever route you expect to complete the supplied decision fastest while remaining exact. Do not inspect benchmark after-data or capsule metadata."

for replicate in 1 2 3; do
  run_surgeon_cell read "$replicate" "$single_before" "$single_before" "$read_prompt"
done
for replicate in 1 2 3; do
  run_surgeon_cell write "$replicate" "$single_before" "$single_after" "$write_prompt"
done
for replicate in 1 2 3; do
  run_surgeon_cell recovery "$replicate" "$single_before" "$single_after" "$recovery_prompt"
done
for replicate in 1 2 3; do
  run_surgeon_cell chord "$replicate" "$chord_before" "$chord_after" "$chord_prompt"
done

printf '%s\n' complete > "$result_root/status.txt"
printf '%s\n' \
  "bash bench/run_spark_caller_screen.sh --product-root '$product_root' --output '<new-output-dir>' --auth-file '$auth_file'" \
  > "$result_root/replay-command.txt"

raw_list="$result_root/raw-files.list"
(
  cd "$result_root"
  find availability baseline surgeon -type f \
    \( -name 'events.jsonl' -o -name 'stderr.txt' -o -name 'prompt.txt' \
       -o -name 'final.txt' -o -name 'mcp-server.stdout' -o -name 'mcp-server.stderr' \
       -o -name 'mcp-ready.edn' -o -name 'mcp-bootstrap-ms.txt' \
       -o -name 'exit-code.txt' -o -name 'wall-ms.txt' \
       -o -name 'source-before.sha256' -o -name 'source-final.sha256' \
       -o -name 'source-expected.sha256' -o -path '*/mcp-telemetry/*' \) \
    | LC_ALL=C sort > "$raw_list"
  tar -czf raw-streams.tar.gz -T "$raw_list"
  shasum -a 256 raw-streams.tar.gz > raw-streams.tar.gz.sha256
)
printf '%s\n' "$(date -u +%Y-%m-%dT%H:%M:%SZ)" > "$result_root/finished-utc.txt"
