#!/usr/bin/env bash

# Capture-only F/A/B screen for three request-construction shapes.
# The model-facing server records arguments and never reads or writes source.

set -euo pipefail

repo_root=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)
model=gpt-5.6-sol
reasoning=high
cohort_arms=(flat file-groups closed-relations closed-relations file-groups flat)
preflight_arms=(flat file-groups closed-relations)
arms=("${cohort_arms[@]}")
result_dir=${BENCH_RESULT_DIR:-}
auth_file=${BENCH_AUTH_FILE:-${CODEX_HOME:-$HOME/.codex}/auth.json}
expected_head=${BENCH_EXPECTED_HEAD:-}
timeout_seconds=${BENCH_TIMEOUT_SECONDS:-180}
self_test=false
preflight_only=false

usage() {
  cat <<'EOF'
Usage: bench/run_owner_aware_call_construction_screen.sh [OPTIONS]

Options:
  --output DIR       Result directory (required outside --self-test)
  --auth-file FILE   Codex auth.json for each fresh home
  --expected-head SHA
                     Exact approved candidate commit (required outside self-test)
  --timeout SEC      Per-call timeout (default: 180)
  --self-test        Run zero-model scorer/adapter falsifiers only
  --preflight-only   Stop after all three real client-surface gates
EOF
}

while [ "$#" -gt 0 ]; do
  case "$1" in
    --output) result_dir=${2:?--output requires a directory}; shift 2 ;;
    --auth-file) auth_file=${2:?--auth-file requires a file}; shift 2 ;;
    --expected-head) expected_head=${2:?--expected-head requires a SHA}; shift 2 ;;
    --timeout) timeout_seconds=${2:?--timeout requires seconds}; shift 2 ;;
    --self-test) self_test=true; shift ;;
    --preflight-only) preflight_only=true; shift ;;
    -h|--help) usage; exit 0 ;;
    *) echo "Unknown argument: $1" >&2; usage >&2; exit 2 ;;
  esac
done

if [ "$preflight_only" = true ]; then
  arms=("${preflight_arms[@]}")
fi

screen_prompt() {
  cat <<'EOF'
Call the single available clj-surgeon tool exactly once. Construct the complete
request from the supplied decision. Do not inspect or read source, use shell,
or emit a file-change action. Omit workspace_root because the isolated server
owns it. After the capture-only call succeeds, reply exactly: call captured

EOF
  cat "$repo_root/bench/fixtures/edit_portfolio/submission-row-extraction-cleanup/task.txt"
}

write_codex_config() {
  local path=$1 url=$2
  cat > "$path" <<EOF
[mcp_servers.clj-surgeon]
url = "$url"
required = true
enabled_tools = ["edit_clojure"]
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

arms_json() {
  printf '%s\n' "$@" | jq -R . | jq -s .
}

assert_declared_orders() {
  [ "$(arms_json "${preflight_arms[@]}" | jq -c .)" = \
    '["flat","file-groups","closed-relations"]' ]
  [ "$(arms_json "${cohort_arms[@]}" | jq -c .)" = \
    '["flat","file-groups","closed-relations","closed-relations","file-groups","flat"]' ]
}

assert_expected_head() {
  local expected=$1 actual=$2
  if ! [[ "$expected" =~ ^[0-9a-f]{40}$ ]]; then
    echo "An exact 40-character --expected-head is required" >&2
    return 1
  fi
  if [ "$expected" != "$actual" ]; then
    echo "Approved candidate head mismatch: expected $expected, actual $actual" >&2
    return 1
  fi
}

run_zero_model_tests() {
  assert_declared_orders
  assert_expected_head 0123456789012345678901234567890123456789 \
    0123456789012345678901234567890123456789
  if assert_expected_head 0123456789012345678901234567890123456789 \
      fedcba9876543210fedcba9876543210fedcba98 2>/dev/null; then
    echo "Expected-head mismatch falsifier was admitted" >&2
    return 1
  fi
  screen_prompt > "${TMPDIR:-/tmp}/owner-aware-call-screen-prompt.$$"
  if rg -q 'file_groups|symbol_migration|target_alias|preserve-name|require_change' \
    "${TMPDIR:-/tmp}/owner-aware-call-screen-prompt.$$"; then
    echo "Prompt leaked the candidate request language" >&2
    return 1
  fi
  rm -f "${TMPDIR:-/tmp}/owner-aware-call-screen-prompt.$$"
  clojure -J-Xms64m -J-Xmx512m \
    -Sdeps '{:paths ["src" "test" "bench" "dev/experiments"]}' \
    -M:clj-surgeon/mcp -e \
    '(load-file "dev/experiments/three_arm_request_shape_screen_test.clj")
     (load-file "dev/experiments/owner_aware_call_capture_server_test.clj")
     (load-file "dev/experiments/owner_aware_mcp_surface_observer_test.clj")
     (load-file "dev/experiments/clj_surgeon/experiments/mcp_candidate_admission_test.clj")
     (let [{:keys [fail error]}
           (clojure.test/run-tests
             (quote clj-surgeon.experiments.mcp-candidate-admission-test))]
       (when (pos? (+ fail error))
         (System/exit 1)))'
  printf '%s\n' \
    'three-arm request-shape screen self-test: PASS' \
    '  cohort: flat, file-groups, closed-relations, closed-relations, file-groups, flat' \
    '  scorer: pure expanders, decision coverage, existing compiler, nine frozen future hashes' \
    '  server: one capture-only edit_clojure tool; no product write handler' \
    '  prompt: identical task bytes and no candidate-language leak' \
    '  observer: app-tool cache rejected; only exact Codex registry projections normalized'
}

if [ "$self_test" = true ]; then
  run_zero_model_tests
  exit 0
fi

if ! [[ "$timeout_seconds" =~ ^[1-9][0-9]*$ ]]; then
  echo "--timeout must be a positive integer" >&2
  exit 2
fi
if [ -z "$result_dir" ] || [ ! -f "$auth_file" ]; then
  echo "--output and an existing --auth-file are required" >&2
  exit 2
fi
for command in bb clojure codex jq mkfifo rg shasum; do
  command -v "$command" >/dev/null || {
    echo "Required command not found: $command" >&2
    exit 2
  }
done

mkdir -p "$result_dir"
result_dir=$(cd "$result_dir" && pwd)
git_head=$(git -C "$repo_root" rev-parse HEAD)
assert_expected_head "$expected_head" "$git_head" || exit 2
integration_base=54aae16f340033dc6d9452043b335c6bb98dea04
if ! git -C "$repo_root" merge-base --is-ancestor "$integration_base" "$git_head" \
  || ! git -C "$repo_root" diff --quiet "$integration_base" -- src test; then
  echo "Screen requires product source/test bytes from integration base 54aae16" >&2
  exit 2
fi

jq -n \
  --arg schema clj-surgeon.three-arm-request-shape-config.v1 \
  --arg head "$git_head" --arg expected_head "$expected_head" \
  --arg base "$integration_base" \
  --arg model "$model" --arg reasoning "$reasoning" \
  --arg mode "$(if [ "$preflight_only" = true ]; then printf preflight; else printf cohort; fi)" \
  --arg harness_sha "$(shasum -a 256 "${BASH_SOURCE[0]}" | awk '{print $1}')" \
  --arg scorer_sha "$(shasum -a 256 "$repo_root/dev/experiments/three_arm_request_shape_screen.clj" | awk '{print $1}')" \
  --arg observer_sha "$(shasum -a 256 "$repo_root/dev/experiments/owner_aware_mcp_surface_observer.clj" | awk '{print $1}')" \
  --arg fixture_sha "$(shasum -a 256 "$repo_root/bench/fixtures/edit_portfolio/submission-row-extraction-cleanup/task.txt" | awk '{print $1}')" \
  --argjson order "$(arms_json "${arms[@]}")" \
  '{schema:$schema,git_head:$head,expected_git_head:$expected_head,
    integration_base:$base,mode:$mode,
    model:$model,reasoning:$reasoning,
    order:$order,
    hashes:{harness:$harness_sha,scorer:$scorer_sha,observer:$observer_sha,task:$fixture_sha}}' \
  > "$result_dir/run-config.json"

clojure -J-Xms64m -J-Xmx512m \
  -Sdeps '{:paths ["src" "test" "bench" "dev/experiments"]}' \
  -M:clj-surgeon/mcp -m three-arm-request-shape-screen prerequisites \
  > "$result_dir/prerequisite-report.edn"

score_paths=()
run_index=0
for arm in "${arms[@]}"; do
  run_index=$((run_index + 1))
  run_id=$(printf '%02d-%s' "$run_index" "$arm")
  run_dir="$result_dir/$run_id"
  codex_home="$run_dir/codex-home"
  workspace="$run_dir/empty-workspace"
  ready_file="$run_dir/mcp-ready.edn"
  capture_file="$run_dir/captured-calls.json"
  surface_file="$run_dir/advertised-surface.json"
  registry_file="$run_dir/codex-mcp-registry.json"
  mkdir -p "$run_dir" "$codex_home" "$workspace" "$run_dir/mcp-telemetry"
  ln -s "$auth_file" "$codex_home/auth.json"
  screen_prompt > "$run_dir/prompt.txt"
  chmod a-w "$workspace"

  mcp_pid=""
  tap_pid=""
  cleanup_run() {
    [ -z "${tap_pid:-}" ] || kill "$tap_pid" 2>/dev/null || true
    if [ -n "${mcp_pid:-}" ]; then
      kill "$mcp_pid" 2>/dev/null || true
      wait "$mcp_pid" 2>/dev/null || true
    fi
    chmod u+w "$workspace" 2>/dev/null || true
  }
  trap cleanup_run EXIT INT TERM

  (
    cd "$repo_root"
    exec clojure -J-Xms64m -J-Xmx512m \
      -Sdeps '{:paths ["src" "dev/experiments"]}' \
      -X:clj-surgeon/mcp owner-aware-call-capture-server/start \
      :arm ":$arm" \
      :capture-file "$(jq -Rn --arg value "$capture_file" '$value')" \
      :surface-receipt-file "$(jq -Rn --arg value "$surface_file" '$value')" \
      :project-dir "$(jq -Rn --arg value "$workspace" '$value')" \
      :telemetry :full \
      :telemetry-dir "$(jq -Rn --arg value "$run_dir/mcp-telemetry" '$value')" \
      :run-id "$(jq -Rn --arg value "$run_id" '$value')" \
      :nrepl-port :none :port 0 \
      :ready-file "$(jq -Rn --arg value "$ready_file" '$value')"
  ) > "$run_dir/mcp-server.stdout" 2> "$run_dir/mcp-server.stderr" &
  mcp_pid=$!

  for _ in $(seq 1 240); do
    [ -s "$ready_file" ] && break
    kill -0 "$mcp_pid" 2>/dev/null || {
      cat "$run_dir/mcp-server.stderr" >&2
      exit 2
    }
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
  clojure -J-Xms32m -J-Xmx256m \
    -Sdeps '{:paths ["bench" "dev/experiments"] :deps {cheshire/cheshire {:mvn/version "5.13.0"}}}' \
    -M -m owner-aware-mcp-surface-observer \
    --advertised "$surface_file" --registry "$registry_file" \
    --server clj-surgeon --tool edit_clojure \
    > "$run_dir/client-surface-validation.json" \
    2> "$run_dir/client-surface-validation.stderr"

  if [ "$preflight_only" = true ]; then
    printf '%s\t%s\t%s\n' "$run_id" "$arm" "client-surface-green" \
      >> "$result_dir/preflight.tsv"
    cleanup_run
    mcp_pid=""
    trap - EXIT INT TERM
    continue
  fi

  fifo="$run_dir/events.pipe"
  mkfifo "$fifo"
  bb "$repo_root/bench/event_timing.clj" tap "$run_dir/event-clock.tsv" \
    < "$fifo" > "$run_dir/events.jsonl" &
  tap_pid=$!
  start_ms=$(($(date +%s) * 1000))
  set +e
  CODEX_HOME="$codex_home" codex exec --json --ephemeral --ignore-rules \
    --skip-git-repo-check --sandbox read-only --color never \
    -m "$model" -c "model_reasoning_effort=\"$reasoning\"" \
    -C "$workspace" "$(cat "$run_dir/prompt.txt")" \
    > "$fifo" 2> "$run_dir/stderr.txt" < /dev/null &
  codex_pid=$!
  wait_for_pid "$codex_pid"
  exit_code=$?
  wait "$tap_pid"
  tap_pid=""
  set -e
  end_ms=$(($(date +%s) * 1000))
  bb "$repo_root/bench/event_timing.clj" summarize \
    "$run_dir/events.jsonl" "$run_dir/event-clock.tsv" "$start_ms" "$end_ms" \
    > "$run_dir/event-timing.edn"

  mcp_calls=$(jq -s '[.[] | select(.type == "item.started" and .item.type == "mcp_tool_call")] | length' "$run_dir/events.jsonl")
  shell_calls=$(jq -s '[.[] | select(.type == "item.started" and .item.type == "command_execution")] | length' "$run_dir/events.jsonl")
  file_changes=$(jq -s '[.[] | select(.type == "item.started" and .item.type == "file_change")] | length' "$run_dir/events.jsonl")
  jq -j -s '[.[] | select(.type == "item.completed" and .item.type == "agent_message")] | last.item.text // empty' \
    "$run_dir/events.jsonl" > "$run_dir/final-agent-message.txt"
  [ "$exit_code" -eq 0 ] || echo "Codex exited $exit_code: $run_id" >&2
  [ -s "$capture_file" ] || { echo "No captured call: $run_id" >&2; exit 2; }

  clojure -Sdeps '{:paths ["src" "test" "dev/experiments"]}' \
    -M:clj-surgeon/mcp -m three-arm-request-shape-screen score \
    --arm "$arm" --capture "$capture_file" \
    --timing "$run_dir/event-timing.edn" --mcp-calls "$mcp_calls" \
    --shell-calls "$shell_calls" --file-changes "$file_changes" \
    --final-response "$run_dir/final-agent-message.txt" \
    > "$run_dir/score.edn"
  score_paths+=("$run_dir/score.edn")
  printf '%s %s\n' "$run_id" "$(bb -e '(-> *command-line-args* first slurp clojure.edn/read-string :correct println)' "$run_dir/score.edn")"

  cleanup_run
  mcp_pid=""
  trap - EXIT INT TERM
done

if [ "$preflight_only" = true ]; then
  jq -n \
    --arg schema clj-surgeon.three-arm-request-shape-preflight.v1 \
    --slurpfile config "$result_dir/run-config.json" \
    --arg prerequisite_sha "$(shasum -a 256 "$result_dir/prerequisite-report.edn" | awk '{print $1}')" \
    --argjson arms "$(arms_json "${arms[@]}")" \
    '{schema:$schema,ok:true,model_calls:0,mutation_actions:0,
      arms:$arms,config:$config[0],
      prerequisite_report_sha256:$prerequisite_sha}' \
    > "$result_dir/preflight-summary.json"
  cat "$result_dir/preflight-summary.json"
  exit 0
fi

clojure -Sdeps '{:paths ["src" "test" "dev/experiments"]}' \
  -M:clj-surgeon/mcp -m three-arm-request-shape-screen cohort \
  "${score_paths[@]}" > "$result_dir/summary.edn"

cat "$result_dir/summary.edn"
