#!/usr/bin/env bash
set -euo pipefail

repo_root=$(cd "$(dirname "$0")/.." && pwd)
fixture_root="$repo_root/bench/fixtures/read_portfolio"
model=${BENCH_MODEL:-gpt-5.6-sol}
reasoning=${BENCH_REASONING:-medium}
port=${BENCH_MCP_PORT:-7889}
replicates=${BENCH_REPLICATES:-4}
lanes=${BENCH_LANES:-"mcp cli native"}
auth_file=${CODEX_AUTH_FILE:-$HOME/.codex/auth.json}
result_root=${BENCH_RESULT_DIR:-$(mktemp -d /tmp/clj-surgeon-inspect-benchmark.XXXXXX)}
corpus="$result_root/corpus"
ready_file="$result_root/mcp-ready.edn"
server_stdout="$result_root/mcp-server.stdout"
server_stderr="$result_root/mcp-server.stderr"
server_pid=""

cleanup() {
  if [ -n "$server_pid" ]; then
    kill "$server_pid" 2>/dev/null || true
    wait "$server_pid" 2>/dev/null || true
  fi
}
trap cleanup EXIT

for command_name in codex clojure bb jq perl curl; do
  command -v "$command_name" >/dev/null 2>&1 || {
    echo "Missing required command: $command_name" >&2
    exit 2
  }
done

test -f "$auth_file" || {
  echo "Codex authentication file not found: $auth_file" >&2
  exit 2
}

mkdir -p "$result_root" \
  "$corpus/bench/fixtures/read_portfolio" \
  "$corpus/src/clj_surgeon"
cp "$repo_root/bench/summarize_clean_codex.clj" "$corpus/bench/"
cp "$repo_root/bench/rescore_clean_codex.clj" "$corpus/bench/"
cp "$fixture_root/match_decoys.clj" "$corpus/bench/fixtures/read_portfolio/"
cp "$repo_root/src/clj_surgeon/show_form.clj" "$corpus/src/clj_surgeon/"
cp "$fixture_root/answer_schema.json" "$result_root/answer_schema.json"
cp "$fixture_root/expected.json" "$result_root/expected.json"
cp "$fixture_root/prompt.md" "$result_root/prompt.md"

if [ "${1:-}" = "--self-test" ]; then
  jq -e . "$result_root/answer_schema.json" >/dev/null
  jq -e . "$result_root/expected.json" >/dev/null
  test "$(find "$corpus" -type f | wc -l | tr -d ' ')" -eq 4
  printf '%s\n' "inspect benchmark self-test passed: $result_root"
  exit 0
fi

server_started_ms=$(perl -MTime::HiRes=time -e 'printf "%.0f\n", time()*1000')
(
  cd "$repo_root"
  exec clojure -X:clj-surgeon/mcp \
    :project-dir "$(bb -e '(prn (first *command-line-args*))' "$corpus")" \
    :telemetry :metrics \
    :telemetry-dir "$(bb -e '(prn (first *command-line-args*))' "$result_root/mcp-telemetry")" \
    :run-id '"inspect-read-portfolio"' \
    :nrepl-port :none \
    :port "$port" \
    :ready-file "$(bb -e '(prn (first *command-line-args*))' "$ready_file")"
) >"$server_stdout" 2>"$server_stderr" &
server_pid=$!

for _attempt in $(seq 1 240); do
  [ -s "$ready_file" ] && break
  if ! kill -0 "$server_pid" 2>/dev/null; then
    echo "inspect MCP server exited before readiness" >&2
    sed -n '1,160p' "$server_stderr" >&2
    exit 2
  fi
  sleep 0.25
done
test -s "$ready_file" || {
  echo "inspect MCP server did not become ready" >&2
  exit 2
}
mcp_url=$(bb -e '(-> *command-line-args* first slurp clojure.edn/read-string :url println)' "$ready_file")
curl --fail --silent --show-error "${mcp_url%/mcp}/healthz" >/dev/null
server_ready_ms=$(perl -MTime::HiRes=time -e 'printf "%.0f\n", time()*1000')
printf '%s\n' "$((server_ready_ms - server_started_ms))" > "$result_root/mcp-bootstrap-ms.txt"

printf '%s\n' $'run_id\tlane\treplicate\torder\tcorrect\twall_ms\texit_code\tinput_tokens\tcached_input_tokens\toutput_tokens\tshell_calls\tmcp_calls\tmcp_successes\tmcp_failures\tsource_bearing_actions\tprocess_startups\trequest_bytes\tresult_bytes' \
  > "$result_root/runs.tsv"

lane_prompt() {
  case "$1" in
    mcp)
      printf '%s\n' "Use inspect_clojure exactly once with one batch containing all five requested operations. Set aggregate expect.requests=5 and expect.files=4 because repeated file paths count once. Do not use shell commands, read files another way, or repeat a successful call."
      ;;
    cli)
      printf '%s\n' "Use the installed clj-surgeon CLI as the structural read route. Do not use native cat, sed, grep, rg, or Python to read source."
      ;;
    native)
      printf '%s\n' "Use ordinary native shell readers and search tools. Do not invoke clj-surgeon or any MCP tool."
      ;;
  esac
}

run_lane() {
  local lane=$1
  local replicate=$2
  local order=$3
  local run_id="r${replicate}-${order}-${lane}"
  local run_dir="$result_root/$run_id"
  local codex_home="$run_dir/codex-home"
  local prompt="$run_dir/prompt.md"
  local lane_path
  local -a config_flags
  mkdir -p "$run_dir" "$codex_home"
  ln -s "$auth_file" "$codex_home/auth.json"
  cp "$result_root/prompt.md" "$prompt"
  lane_prompt "$lane" >> "$prompt"

  config_flags=(--ignore-user-config)
  lane_path="/opt/homebrew/bin:/usr/bin:/bin:/usr/sbin:/sbin"
  if [ "$lane" = mcp ]; then
    config_flags=()
    {
      printf '%s\n' '[mcp_servers.clj-surgeon]'
      printf 'url = "%s"\n' "$mcp_url"
      printf '%s\n' 'required = true'
      printf '%s\n' 'enabled_tools = ["inspect_clojure"]'
      printf '%s\n' 'default_tools_approval_mode = "writes"'
      printf '%s\n' 'startup_timeout_sec = 5'
      printf '%s\n' 'tool_timeout_sec = 45'
    } > "$codex_home/config.toml"
  elif [ "$lane" = cli ]; then
    lane_path="$HOME/bin:$lane_path"
  fi

  local start_ms end_ms wall_ms exit_code
  start_ms=$(perl -MTime::HiRes=time -e 'printf "%.0f\n", time()*1000')
  set +eu
  PATH="$lane_path" CODEX_HOME="$codex_home" \
    codex exec --json --ephemeral "${config_flags[@]}" --ignore-rules \
      --skip-git-repo-check --sandbox read-only --color never \
      --output-schema "$result_root/answer_schema.json" \
      -m "$model" -c "model_reasoning_effort=\"$reasoning\"" \
      -C "$corpus" "$(cat "$prompt")" \
      > "$run_dir/events.jsonl" 2> "$run_dir/stderr.txt" </dev/null
  exit_code=$?
  set -eu
  end_ms=$(perl -MTime::HiRes=time -e 'printf "%.0f\n", time()*1000')
  wall_ms=$((end_ms - start_ms))

  jq -s -r '[.[] | select(.type == "item.completed" and .item.type == "agent_message")][-1].item.text // ""' \
    "$run_dir/events.jsonl" > "$run_dir/final.json"
  jq -s '[.[] | select(.type == "item.completed" and .item.type == "command_execution") | .item]' \
    "$run_dir/events.jsonl" > "$run_dir/commands.json"

  local correct=false
  if jq -e . "$run_dir/final.json" >/dev/null 2>&1 \
    && diff -q <(jq -S . "$result_root/expected.json") \
               <(jq -S . "$run_dir/final.json") >/dev/null; then
    correct=true
  fi

  local usage input_tokens cached_tokens output_tokens shell_calls
  local mcp_calls mcp_successes mcp_failures request_bytes result_bytes
  local source_actions process_startups
  usage=$(jq -s '[.[] | select(.type == "turn.completed")][-1].usage // {}' \
    "$run_dir/events.jsonl")
  input_tokens=$(jq -r '.input_tokens // 0' <<< "$usage")
  cached_tokens=$(jq -r '.cached_input_tokens // 0' <<< "$usage")
  output_tokens=$(jq -r '.output_tokens // 0' <<< "$usage")
  shell_calls=$(jq 'length' "$run_dir/commands.json")
  mcp_calls=$(jq -s '[.[] | select(.type == "item.started" and .item.type == "mcp_tool_call" and .item.tool == "inspect_clojure")] | length' "$run_dir/events.jsonl")
  mcp_successes=$(jq -s '[.[] | select(.type == "item.completed" and .item.type == "mcp_tool_call" and .item.tool == "inspect_clojure" and .item.status == "completed" and ((.item.result.structured_content.read_complete == true) or (.item.result.structuredContent.read_complete == true)))] | length' "$run_dir/events.jsonl")
  mcp_failures=$((mcp_calls - mcp_successes))
  request_bytes=$(jq -s '[.[] | select(.type == "item.started" and .item.type == "mcp_tool_call" and .item.tool == "inspect_clojure") | (.item.arguments | tojson | utf8bytelength)] | add // 0' "$run_dir/events.jsonl")
  result_bytes=$(jq -s '[.[] | select(.type == "item.completed" and .item.type == "mcp_tool_call" and .item.tool == "inspect_clojure") | (.item.result | tojson | utf8bytelength)] | add // 0' "$run_dir/events.jsonl")
  if [ "$lane" != mcp ]; then
    result_bytes=$(jq '[.[] | (.aggregated_output // "" | utf8bytelength)] | add // 0' "$run_dir/commands.json")
  fi
  source_actions=$((shell_calls + mcp_calls))
  process_startups=$shell_calls

  printf '%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\n' \
    "$run_id" "$lane" "$replicate" "$order" "$correct" "$wall_ms" \
    "$exit_code" "$input_tokens" "$cached_tokens" "$output_tokens" \
    "$shell_calls" "$mcp_calls" "$mcp_successes" "$mcp_failures" \
    "$source_actions" "$process_startups" "$request_bytes" "$result_bytes" \
    >> "$result_root/runs.tsv"
}

schedules=(
  "mcp cli native"
  "cli native mcp"
  "native mcp cli"
  "mcp native cli"
)

for replicate in $(seq 1 "$replicates"); do
  schedule=${schedules[$(((replicate - 1) % ${#schedules[@]}))]}
  order=0
  for lane in $schedule; do
    case " $lanes " in
      *" $lane "*) ;;
      *) continue ;;
    esac
    order=$((order + 1))
    run_lane "$lane" "$replicate" "$order"
  done
done

bb "$repo_root/bench/summarize_inspect_mcp_benchmark.clj" \
  "$result_root/runs.tsv" > "$result_root/summary.md"
cat "$result_root/summary.md"
printf '\nRaw benchmark directory: %s\n' "$result_root"
