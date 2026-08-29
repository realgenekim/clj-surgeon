#!/usr/bin/env bash

# Capture-only C-T-T-C screen for the extraction-only apply surface.
# Every catalog handler is no-effect. This runner cannot authorize publication.

set -euo pipefail

repo_root=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)
base_commit=6ec1abb606c4d5289702abba029eb491ae678a26
model=gpt-5.6-sol
reasoning=high
arms=(control treatment treatment control)
run_ids=(01-control 02-treatment 03-treatment 04-control)
expected_prompt_sha=101af5af476bc6f3e477e1170254cb30f7673e8857feeac157d0d402a343f139
expected_prompt_bytes=2053
surface_source_sha=8e86b9b6f6e5a73a82af7322a796670a832371cc2c7c3a065f5879f22d8d2de4
capture_server_sha=f41bff100c0930bcf12445c511da8fbd9004f026087c1edb4ad5288082696dd0
protocol_sha=950b97e1d403a814b94deb00d221527366adb839d0524f7b50adbe73850d2ddb
result_dir=${BENCH_RESULT_DIR:-}
auth_file=${BENCH_AUTH_FILE:-${CODEX_HOME:-$HOME/.codex}/auth.json}
timeout_seconds=${BENCH_TIMEOUT_SECONDS:-180}
expected_head=""
expected_tree=""
expected_runner_sha=""
expected_scorer_sha=""
self_test=false
run=false

usage() {
  cat <<'EOF'
Usage: bench/run_extraction_tool_surface_capture_screen.sh OPTIONS

  --self-test        Run all zero-model gates
  --run              Run the exact capture-only C-T-T-C cohort
  --output DIR       Required for --run
  --auth-file FILE   Codex auth.json (default: current CODEX_HOME)
  --expected-head SHA Exact clean checkout required for --run
  --expected-tree SHA Exact checkout tree required for --run
  --expected-runner-sha SHA Exact runner bytes required for --run
  --expected-scorer-sha SHA Exact scorer bytes required for --run
  --timeout SEC      Per-run timeout (default: 180)

Capture-only results measure request construction. They cannot promote product code.
EOF
}

while [ "$#" -gt 0 ]; do
  case "$1" in
    --self-test) self_test=true; shift ;;
    --run) run=true; shift ;;
    --output) result_dir=${2:?--output requires a value}; shift 2 ;;
    --auth-file) auth_file=${2:?--auth-file requires a value}; shift 2 ;;
    --expected-head) expected_head=${2:?--expected-head requires a value}; shift 2 ;;
    --expected-tree) expected_tree=${2:?--expected-tree requires a value}; shift 2 ;;
    --expected-runner-sha) expected_runner_sha=${2:?--expected-runner-sha requires a value}; shift 2 ;;
    --expected-scorer-sha) expected_scorer_sha=${2:?--expected-scorer-sha requires a value}; shift 2 ;;
    --timeout) timeout_seconds=${2:?--timeout requires a value}; shift 2 ;;
    -h|--help) usage; exit 0 ;;
    *) echo "Unknown option: $1" >&2; usage >&2; exit 2 ;;
  esac
done

screen_prompt() {
  cat "$repo_root/bench/fixtures/edit_portfolio/sessionize-format-extraction/task.txt"
  printf '%s\n' '' \
    'Your first emitted item must be the apply_clojure_changes tool call. Emit no preamble, status narration, or explanation before it. Use exactly this object shape: {"workspace_root":"<current workspace>","extraction":{"file":"<supplied source>","to":"<supplied destination>","forms":["<all supplied forms in order>"],"require_policy":"minimal","public_forms":["<task-declared public form>"],"caller_changes":[],"ignored_caller_files":[]},"verify":"exact"}. Every extraction field is nested inside extraction; verify is top-level. The project-owned exact profile runs the task-declared clj-kondo command against staged bytes inside the atomic transaction. Do not run clj-kondo or any other shell verifier. Treat verification_complete=true and the exact-exit evidence as terminal mutation and verification proof.' \
    'After the no-effect capture receipt, reply with exactly this text and no additional analysis: Captured.'
}

clojure_screen() {
  clojure -J-Xms64m -J-Xmx512m \
    -Sdeps '{:paths ["src" "test" "bench" "dev/experiments"]}' \
    -M:clj-surgeon/mcp -m extraction-tool-surface-capture-screen "$@"
}

write_codex_config() {
  local path=$1 url=$2
  cat > "$path" <<EOF
[mcp_servers.clj-surgeon]
url = "$url"
required = true
default_tools_approval_mode = "approve"
startup_timeout_sec = 30
tool_timeout_sec = 45
EOF
}

wait_for_pid() {
  local pid=$1 deadline=$((SECONDS + timeout_seconds))
  while kill -0 "$pid" 2>/dev/null; do
    if [ "$SECONDS" -ge "$deadline" ]; then
      kill -TERM "$pid" 2>/dev/null || true
      sleep 1
      kill -KILL "$pid" 2>/dev/null || true
      wait "$pid" 2>/dev/null || true
      return 124
    fi
    sleep 1
  done
  wait "$pid"
}

run_zero_model_tests() {
  git -C "$repo_root" merge-base --is-ancestor "$base_commit" HEAD
  [ "$(shasum -a 256 "$repo_root/dev/experiments/extraction_tool_surface.clj" | awk '{print $1}')" = "$surface_source_sha" ]
  [ "$(shasum -a 256 "$repo_root/dev/experiments/extraction_call_capture_server.clj" | awk '{print $1}')" = "$capture_server_sha" ]
  [ "$(shasum -a 256 "$repo_root/dev/experiments/extraction_tool_surface_screen.edn" | awk '{print $1}')" = "$protocol_sha" ]
  [ "$(jq -cS 'if has("workspace_root") then .workspace_root = "<workspace>" else . end' \
       "$repo_root/dev/experiments/fixtures/extraction_tool_surface_retained_arguments.json" | \
       shasum -a 256 | awk '{print $1}')" = \
    '01d502300c9e6af22e22e69f5680a4ed767ecc7fa64e4c9bce1d91b78bdfba47' ]
  [ "${arms[*]}" = 'control treatment treatment control' ]
  [ "${run_ids[*]}" = '01-control 02-treatment 03-treatment 04-control' ]

  local prompt
  prompt=$(mktemp "${TMPDIR:-/tmp}/extraction-surface-prompt.XXXXXX")
  screen_prompt > "$prompt"
  [ "$(wc -c < "$prompt" | tr -d ' ')" -eq "$expected_prompt_bytes" ]
  [ "$(shasum -a 256 "$prompt" | awk '{print $1}')" = "$expected_prompt_sha" ]
  rm -f "$prompt"

  bash -n "$repo_root/bench/run_extraction_tool_surface_capture_screen.sh"
  clojure_screen self-test
  clojure -J-Xms64m -J-Xmx512m \
    -Sdeps '{:paths ["src" "test" "bench" "dev/experiments"]}' \
    -M:clj-surgeon/mcp -e \
    '(load-file "dev/experiments/extraction_tool_surface_test.clj")
     (load-file "dev/experiments/extraction_call_capture_server_test.clj")
     (load-file "dev/experiments/owner_aware_mcp_surface_observer_test.clj")
     (let [result (clojure.test/run-tests
                    (quote extraction-tool-surface-test)
                    (quote extraction-call-capture-server-test)
                    (quote owner-aware-mcp-surface-observer-test))]
       (when-not (zero? (+ (:fail result) (:error result)))
         (throw (ex-info "Extraction surface dependency tests failed" result))))'

  printf '%s\n' \
    'extraction tool-surface capture screen zero-model gate: PASS' \
    '  base: exact 6ec1abb full-catalog no-effect capture server' \
    '  schedule: C-T-T-C; every sample retained; isolated home/workspace/server' \
    '  surface: full ordered production catalog and instructions in both arms' \
    '  treatment: apply_clojure_changes description/schema only' \
    '  route: first external action, one MCP, zero shell/file actions, exact final' \
    '  compiler: root-normalized arguments and two canonical future hashes' \
    '  promotion: both paired wins, >=20% pre-call and >=10% complete-wall'
}

if [ "$self_test" = true ]; then
  [ "$run" = false ] || { echo '--self-test and --run are exclusive' >&2; exit 2; }
  run_zero_model_tests
  exit 0
fi

if [ "$run" != true ]; then
  echo 'No action selected. Use --self-test or --run.' >&2
  exit 2
fi
if [ -z "$result_dir" ]; then
  echo '--output is required for --run' >&2
  exit 2
fi
if [ -e "$result_dir" ] && [ -n "$(find "$result_dir" -mindepth 1 -maxdepth 1 -print -quit)" ]; then
  echo '--output must not contain any prior run artifacts' >&2
  exit 2
fi
if [ -z "$expected_head" ] || [ -z "$expected_tree" ] || \
   [ -z "$expected_runner_sha" ] || [ -z "$expected_scorer_sha" ]; then
  echo '--run requires exact head, tree, runner, and scorer identities' >&2
  exit 2
fi
if [ "$(git -C "$repo_root" rev-parse HEAD)" != "$expected_head" ] || \
   [ "$(git -C "$repo_root" rev-parse 'HEAD^{tree}')" != "$expected_tree" ] || \
   [ "$(shasum -a 256 "$repo_root/bench/run_extraction_tool_surface_capture_screen.sh" | awk '{print $1}')" != "$expected_runner_sha" ] || \
   [ "$(shasum -a 256 "$repo_root/dev/experiments/extraction_tool_surface_capture_screen.clj" | awk '{print $1}')" != "$expected_scorer_sha" ] || \
   [ -n "$(git -C "$repo_root" status --porcelain)" ]; then
  echo '--run checkout or executable evidence does not match the approved immutable candidate' >&2
  exit 2
fi
if [ ! -f "$auth_file" ]; then
  echo '--auth-file must exist for --run' >&2
  exit 2
fi
if ! [[ "$timeout_seconds" =~ ^[1-9][0-9]*$ ]]; then
  echo '--timeout must be a positive integer' >&2
  exit 2
fi
for command in bb clojure codex jq mkfifo perl rg shasum; do
  command -v "$command" >/dev/null || { echo "Missing command: $command" >&2; exit 2; }
done

run_zero_model_tests
mkdir -p "$result_dir"
result_dir=$(cd "$result_dir" && pwd)
score_paths=()

for index in 0 1 2 3; do
  arm=${arms[$index]}
  run_id=${run_ids[$index]}
  run_dir="$result_dir/$run_id"
  mkdir -p "$run_dir"
  codex_home="$run_dir/codex-home"
  workspace="$run_dir/workspace"
  mkdir -p "$codex_home" "$workspace"
  ln -s "$auth_file" "$codex_home/auth.json"
  chmod 0555 "$workspace"
  before=$(find "$workspace" -mindepth 1 -print | sort | jq -R . | jq -s .)
  screen_prompt > "$run_dir/prompt.txt"

  capture_file="$run_dir/capture.json"
  surface_file="$run_dir/advertised-surface.json"
  ready_file="$run_dir/mcp-ready.edn"
  registry_file="$run_dir/codex-mcp-registry.json"

  clojure -J-Xms64m -J-Xmx512m \
    -Sdeps '{:paths ["src" "dev/experiments"]}' \
    -X:clj-surgeon/mcp extraction-call-capture-server/start \
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
  [ -s "$ready_file" ] || { echo "MCP readiness timeout: $run_id" >&2; exit 2; }
  mcp_url=$(bb -e '(-> *command-line-args* first slurp clojure.edn/read-string :url println)' "$ready_file")
  write_codex_config "$codex_home/config.toml" "$mcp_url"

  CODEX_HOME="$codex_home" clojure -J-Xms32m -J-Xmx256m \
    -Sdeps '{:paths ["src" "bench"]}' -M \
    -m capture-codex-mcp-registry --codex "$(command -v codex)" \
    --output "$registry_file" --server clj-surgeon \
    > "$run_dir/registry.stdout" 2> "$run_dir/registry.stderr"
  clojure_screen surface-preflight --advertised "$surface_file" \
    --registry "$registry_file" --arm "$arm" --expected-server clj-surgeon \
    > "$run_dir/surface-preflight.edn"

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
    -C "$workspace" "$(cat "$run_dir/prompt.txt")" \
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

  after=$(find "$workspace" -mindepth 1 -print | sort | jq -R . | jq -s .)
  jq -n --argjson before "$before" --argjson after "$after" \
    '{before:$before,after:$after}' \
    > "$run_dir/workspace-manifest.json"

  if [ "$codex_status" -ne 0 ] || [ ! -s "$capture_file" ]; then
    printf '{:schema :clj-surgeon.extraction-tool-surface-capture-run/v1 :run-id "%s" :position %s :arm :%s :correct false :isolation {:workspace "%s" :codex-home "%s" :server-pid %s} :failure :model-or-capture-failed}\n' \
      "$run_id" "$((index + 1))" "$arm" "$workspace" "$codex_home" "$mcp_pid" \
      > "$run_dir/score.edn"
    score_paths+=("$run_dir/score.edn")
    printf '%s\t%s\t%s\t1\n' "$run_id" "$arm" "$codex_status" \
      >> "$result_dir/runs.tsv"
    cleanup_run
    trap - EXIT INT TERM
    continue
  fi

  jq -cS '.calls[0].params // null' "$capture_file" \
    > "$run_dir/actual-arguments.json"
  jq -cS 'if has("workspace_root") then .workspace_root = "<workspace>" else . end' \
    "$run_dir/actual-arguments.json" \
    > "$run_dir/logical-arguments.json"

  set +e
  clojure_screen score-run \
    --repo-root "$repo_root" --run-id "$run_id" --position "$((index + 1))" \
    --arm "$arm" --advertised "$surface_file" --registry "$registry_file" \
    --capture "$capture_file" --events "$run_dir/events.jsonl" \
    --timing "$run_dir/timing.edn" \
    --logical-arguments "$run_dir/logical-arguments.json" \
    --workspace-manifest "$run_dir/workspace-manifest.json" \
    --expected-server clj-surgeon --workspace "$workspace" \
    --codex-home "$codex_home" --server-pid "$mcp_pid" \
    --codex-status "$codex_status" \
    > "$run_dir/score.edn"
  score_status=$?
  set -e
  score_paths+=("$run_dir/score.edn")
  printf '%s\t%s\t%s\t%s\n' "$run_id" "$arm" "$codex_status" "$score_status" \
    >> "$result_dir/runs.tsv"
  kill "$mcp_pid" 2>/dev/null || true
  wait "$mcp_pid" 2>/dev/null || true
  trap - EXIT INT TERM
done

runs_csv=$(IFS=,; echo "${score_paths[*]}")
clojure_screen cohort-report --runs "$runs_csv" > "$result_dir/cohort-report.edn"
cat "$result_dir/cohort-report.edn"
