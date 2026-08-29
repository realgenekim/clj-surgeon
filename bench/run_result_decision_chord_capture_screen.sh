#!/usr/bin/env bash

# Capture-only C-T-T-C screen for a post-inspect decision chord.
# The inspect receipt is frozen product evidence. Every mutation handler is no-effect.

set -euo pipefail

repo_root=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)
model=${BENCH_MODEL:-gpt-5.6-sol}
reasoning=${BENCH_REASONING:-high}
arms=(control treatment treatment control)
run_ids=(01-control 02-treatment 03-treatment 04-control)
fixture="$repo_root/dev/experiments/fixtures/result_decision_chord/before"
task_file="$repo_root/dev/experiments/fixtures/result_decision_chord/task.txt"
auth_file=${BENCH_AUTH_FILE:-${CODEX_HOME:-$HOME/.codex}/auth.json}
timeout_seconds=${BENCH_TIMEOUT_SECONDS:-120}
result_dir=""
expected_head=""
expected_tree=""
expected_runner_sha=""
expected_server_sha=""
expected_scorer_sha=""
self_test=false
run=false

usage() {
  printf '%s\n' \
    'Usage: bench/run_result_decision_chord_capture_screen.sh OPTIONS' \
    '' \
    '  --self-test             Run zero-model gates' \
    '  --run                   Run the exact capture-only C-T-T-C pilot' \
    '  --output DIR            Required absent or empty directory for --run' \
    '  --expected-head SHA     Exact checkout commit' \
    '  --expected-tree SHA     Exact checkout tree' \
    '  --expected-runner-sha SHA' \
    '  --expected-server-sha SHA' \
    '  --expected-scorer-sha SHA' \
    '  --auth-file FILE        Codex auth.json' \
    '  --timeout SEC           Per-run timeout (default 120)'
}

while [ "$#" -gt 0 ]; do
  case "$1" in
    --self-test) self_test=true; shift ;;
    --run) run=true; shift ;;
    --output) result_dir=${2:?--output requires a value}; shift 2 ;;
    --expected-head) expected_head=${2:?--expected-head requires a value}; shift 2 ;;
    --expected-tree) expected_tree=${2:?--expected-tree requires a value}; shift 2 ;;
    --expected-runner-sha) expected_runner_sha=${2:?--expected-runner-sha requires a value}; shift 2 ;;
    --expected-server-sha) expected_server_sha=${2:?--expected-server-sha requires a value}; shift 2 ;;
    --expected-scorer-sha) expected_scorer_sha=${2:?--expected-scorer-sha requires a value}; shift 2 ;;
    --auth-file) auth_file=${2:?--auth-file requires a value}; shift 2 ;;
    --timeout) timeout_seconds=${2:?--timeout requires a value}; shift 2 ;;
    -h|--help) usage; exit 0 ;;
    *) printf 'Unknown option: %s\n' "$1" >&2; usage >&2; exit 2 ;;
  esac
done

clojure_screen() {
  clojure -J-Xms64m -J-Xmx512m \
    -Sdeps '{:paths ["src" "test" "bench" "dev/experiments"]}' \
    -M:clj-surgeon/mcp -m result-decision-chord-screen "$@"
}

output_root_is_clean() {
  local path=$1
  [ ! -L "$path" ] &&
    { [ ! -e "$path" ] ||
      { [ -d "$path" ] &&
        [ -z "$(find "$path" -mindepth 1 -maxdepth 1 -print -quit)" ]; }; }
}

wait_for_pid() {
  local pid=$1 deadline=$((SECONDS + timeout_seconds))
  while kill -0 "$pid" 2>/dev/null; do
    if [ "$SECONDS" -ge "$deadline" ]; then
      kill -TERM "$pid" 2>/dev/null || true
      wait "$pid" 2>/dev/null || true
      return 124
    fi
    sleep 1
  done
  wait "$pid"
}

run_zero_model_tests() {
  [ "${arms[*]}" = 'control treatment treatment control' ]
  [ "${run_ids[*]}" = '01-control 02-treatment 03-treatment 04-control' ]
  bash -n "$repo_root/bench/run_result_decision_chord_capture_screen.sh"
  clojure -J-Xms64m -J-Xmx512m \
    -Sdeps '{:paths ["src" "test" "bench" "dev/experiments"]}' \
    -M:clj-surgeon/mcp -e \
    '(load-file "dev/experiments/result_decision_chord_capture_server.clj")
     (load-file "dev/experiments/result_decision_chord_capture_server_test.clj")
     (load-file "dev/experiments/result_decision_chord_screen.clj")
     (load-file "dev/experiments/result_decision_chord_screen_test.clj")
     (let [result (clojure.test/run-tests
                    (quote result-decision-chord-capture-server-test)
                    (quote result-decision-chord-screen-test))]
       (when-not (zero? (+ (:fail result) (:error result)))
         (throw (ex-info "Decision chord tests failed" result))))'
  printf '%s\n' \
    'result decision-chord capture zero-model gate: PASS' \
    '  schedule: C-T-T-C' \
    '  evidence: frozen product inspect result; identical structured content' \
    '  lifecycle: exact inspect -> edit -> Captured., direct boundary clock' \
    '  compiler: actual workspace source -> exact frozen future' \
    '  effects: no mutation, no shared runtime'
}

if [ "$self_test" = true ]; then
  [ "$run" = false ] || { printf '%s\n' '--self-test and --run are exclusive' >&2; exit 2; }
  run_zero_model_tests
  exit 0
fi

[ "$run" = true ] || { printf '%s\n' 'Select --self-test or --run.' >&2; exit 2; }
[ -n "$result_dir" ] || { printf '%s\n' '--output is required' >&2; exit 2; }
output_root_is_clean "$result_dir" || { printf '%s\n' '--output must be absent or empty' >&2; exit 2; }
for value in "$expected_head" "$expected_tree" "$expected_runner_sha" \
             "$expected_server_sha" "$expected_scorer_sha"; do
  [ -n "$value" ] || { printf '%s\n' 'All exact identity options are required' >&2; exit 2; }
done
[ -f "$auth_file" ] || { printf '%s\n' 'auth file does not exist' >&2; exit 2; }
[[ "$timeout_seconds" =~ ^[1-9][0-9]*$ ]] || { printf '%s\n' 'timeout must be positive' >&2; exit 2; }

runner="$repo_root/bench/run_result_decision_chord_capture_screen.sh"
server="$repo_root/dev/experiments/result_decision_chord_capture_server.clj"
scorer="$repo_root/dev/experiments/result_decision_chord_screen.clj"
if [ "$(git -C "$repo_root" rev-parse HEAD)" != "$expected_head" ] ||
   [ "$(git -C "$repo_root" rev-parse 'HEAD^{tree}')" != "$expected_tree" ] ||
   [ "$(shasum -a 256 "$runner" | awk '{print $1}')" != "$expected_runner_sha" ] ||
   [ "$(shasum -a 256 "$server" | awk '{print $1}')" != "$expected_server_sha" ] ||
   [ "$(shasum -a 256 "$scorer" | awk '{print $1}')" != "$expected_scorer_sha" ] ||
   ! git -C "$repo_root" diff --quiet -- . ':(exclude).codex/visualizations/2026/08/28'; then
  printf '%s\n' 'checkout or executable evidence does not match the immutable candidate' >&2
  exit 2
fi

for command in bb clojure codex jq mkfifo perl shasum; do
  command -v "$command" >/dev/null || { printf 'Missing command: %s\n' "$command" >&2; exit 2; }
done

run_zero_model_tests
mkdir -p "$result_dir"
result_dir=$(cd "$result_dir" && pwd)
score_paths=()
catalog_hash=""
surface_hash=""

for index in 0 1 2 3; do
  arm=${arms[$index]}
  run_id=${run_ids[$index]}
  run_dir="$result_dir/$run_id"
  codex_home="$run_dir/codex-home"
  workspace="$run_dir/workspace"
  mkdir -p "$codex_home" "$workspace"
  cp -R "$fixture"/. "$workspace"/
  ln -s "$auth_file" "$codex_home/auth.json"

  capture_file="$run_dir/capture.json"
  surface_file="$run_dir/advertised-surface.json"
  ready_file="$run_dir/mcp-ready.edn"
  registry_file="$run_dir/codex-mcp-registry.json"

  clojure -J-Xms64m -J-Xmx512m \
    -Sdeps '{:paths ["src" "dev/experiments"]}' \
    -X:clj-surgeon/mcp result-decision-chord-capture-server/start \
    :arm ":$arm" \
    :capture-file "$(jq -Rn --arg value "$capture_file" '$value')" \
    :surface-receipt-file "$(jq -Rn --arg value "$surface_file" '$value')" \
    :project-dir "$(jq -Rn --arg value "$workspace" '$value')" \
    :nrepl-port :none :port 0 \
    :ready-file "$(jq -Rn --arg value "$ready_file" '$value')" \
    > "$run_dir/mcp.stdout" 2> "$run_dir/mcp.stderr" &
  mcp_pid=$!
  tap_pid=""
  cleanup_run() {
    if [ -n "$tap_pid" ]; then
      kill "$tap_pid" 2>/dev/null || true
      wait "$tap_pid" 2>/dev/null || true
    fi
    kill "$mcp_pid" 2>/dev/null || true
    wait "$mcp_pid" 2>/dev/null || true
  }
  trap cleanup_run EXIT INT TERM

  for _ in $(seq 1 120); do
    [ -s "$ready_file" ] && break
    kill -0 "$mcp_pid" 2>/dev/null || { cat "$run_dir/mcp.stderr" >&2; exit 2; }
    sleep 0.25
  done
  [ -s "$ready_file" ] || { printf 'MCP readiness timeout: %s\n' "$run_id" >&2; exit 2; }
  mcp_url=$(bb -e '(-> *command-line-args* first slurp clojure.edn/read-string :url println)' "$ready_file")
  bb "$repo_root/bench/write_mcp_config.clj" "$codex_home/config.toml" \
    --url "$mcp_url" --enabled-tools-edn \
    '["inspect_clojure","apply_clojure_changes","edit_clojure","transform_clojure"]' >/dev/null

  CODEX_HOME="$codex_home" clojure -J-Xms32m -J-Xmx256m \
    -Sdeps '{:paths ["src" "bench"]}' -M \
    -m capture-codex-mcp-registry --codex "$(command -v codex)" \
    --output "$registry_file" --server clj-surgeon \
    > "$run_dir/registry.stdout" 2> "$run_dir/registry.stderr"
  current_catalog_hash=$(jq -cS '.tool_projection' "$registry_file" | shasum -a 256 | awk '{print $1}')
  current_surface_hash=$(jq -cS '{instructions:.instructions,tools:.tools}' "$surface_file" | shasum -a 256 | awk '{print $1}')
  if [ -z "$catalog_hash" ]; then
    catalog_hash=$current_catalog_hash
    surface_hash=$current_surface_hash
  fi
  [ "$catalog_hash" = "$current_catalog_hash" ] || { printf '%s\n' 'client catalog drift' >&2; exit 2; }
  [ "$surface_hash" = "$current_surface_hash" ] || { printf '%s\n' 'advertised surface drift' >&2; exit 2; }

  fifo="$run_dir/events.pipe"
  mkfifo "$fifo"
  bb "$repo_root/bench/event_timing.clj" tap "$run_dir/event-clock.tsv" \
    < "$fifo" > "$run_dir/events.jsonl" &
  tap_pid=$!
  start_ms=$(perl -MTime::HiRes=time -e 'printf "%.0f\n", time()*1000')
  set +e
  CODEX_HOME="$codex_home" codex exec --json --ephemeral --ignore-rules \
    --skip-git-repo-check --sandbox read-only --color never \
    -m "$model" -c "model_reasoning_effort=\"$reasoning\"" \
    -C "$workspace" "$(cat "$task_file")" \
    > "$fifo" 2> "$run_dir/stderr.txt" < /dev/null &
  codex_pid=$!
  wait_for_pid "$codex_pid"
  codex_status=$?
  wait "$tap_pid"
  tap_pid=""
  set -e
  end_ms=$(perl -MTime::HiRes=time -e 'printf "%.0f\n", time()*1000')
  bb "$repo_root/bench/event_timing.clj" summarize \
    "$run_dir/events.jsonl" "$run_dir/event-clock.tsv" "$start_ms" "$end_ms" \
    > "$run_dir/timing.edn"

  if [ "$codex_status" -ne 0 ] || [ ! -s "$capture_file" ]; then
    printf 'post-token run failed before a complete capture: %s\n' "$run_id" >&2
    cleanup_run
    trap - EXIT INT TERM
    exit 1
  fi

  clojure_screen score-run \
    --arm "$arm" --position "$((index + 1))" --capture "$capture_file" \
    --events "$run_dir/events.jsonl" --timing "$run_dir/timing.edn" \
    --workspace "$workspace" --codex-home "$codex_home" --server-pid "$mcp_pid" \
    > "$run_dir/score.edn"
  score_paths+=("$run_dir/score.edn")
  if [ "$(bb -e '(-> *command-line-args* first slurp clojure.edn/read-string :correct println)' "$run_dir/score.edn")" != true ]; then
    printf 'post-token run failed its exact gate; retained and stopping: %s\n' "$run_id" >&2
    cleanup_run
    trap - EXIT INT TERM
    exit 1
  fi

  cleanup_run
  trap - EXIT INT TERM
done

runs_csv=$(IFS=,; printf '%s' "${score_paths[*]}")
clojure_screen cohort-report --runs "$runs_csv" > "$result_dir/cohort-report.edn"
cat "$result_dir/cohort-report.edn"
