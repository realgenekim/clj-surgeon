#!/usr/bin/env bash

# Capture-only A/B screen for the owner-aware symbol-migration request shape.
# The model-facing server records arguments and never reads or writes source.

set -euo pipefail

repo_root=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)
model=gpt-5.6-sol
reasoning=high
arms=(control candidate candidate control candidate control control candidate)
result_dir=${BENCH_RESULT_DIR:-}
auth_file=${BENCH_AUTH_FILE:-${CODEX_HOME:-$HOME/.codex}/auth.json}
timeout_seconds=${BENCH_TIMEOUT_SECONDS:-180}
self_test=false

usage() {
  cat <<'EOF'
Usage: bench/run_owner_aware_call_construction_screen.sh [OPTIONS]

Options:
  --output DIR       Result directory (required outside --self-test)
  --auth-file FILE   Codex auth.json for each fresh home
  --timeout SEC      Per-call timeout (default: 180)
  --self-test        Run zero-model scorer/adapter falsifiers only
EOF
}

while [ "$#" -gt 0 ]; do
  case "$1" in
    --output) result_dir=${2:?--output requires a directory}; shift 2 ;;
    --auth-file) auth_file=${2:?--auth-file requires a file}; shift 2 ;;
    --timeout) timeout_seconds=${2:?--timeout requires seconds}; shift 2 ;;
    --self-test) self_test=true; shift ;;
    -h|--help) usage; exit 0 ;;
    *) echo "Unknown argument: $1" >&2; usage >&2; exit 2 ;;
  esac
done

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

run_zero_model_tests() {
  local order
  order=$(printf '%s\n' "${arms[@]}" | tr '\n' ' ' | sed 's/ $//')
  [ "$order" = "control candidate candidate control candidate control control candidate" ]
  screen_prompt > "${TMPDIR:-/tmp}/owner-aware-call-screen-prompt.$$"
  if rg -q 'symbol_migration|target_alias|preserve-name' \
    "${TMPDIR:-/tmp}/owner-aware-call-screen-prompt.$$"; then
    echo "Prompt leaked the candidate request language" >&2
    return 1
  fi
  rm -f "${TMPDIR:-/tmp}/owner-aware-call-screen-prompt.$$"
  clojure -Sdeps '{:paths ["src" "test" "dev/experiments"]}' \
    -M:clj-surgeon/mcp -e \
    '(load-file "dev/experiments/owner_aware_call_construction_screen_test.clj")
     (load-file "dev/experiments/owner_aware_call_capture_server_test.clj")'
  printf '%s\n' \
    'owner-aware call-construction screen self-test: PASS' \
    '  cohort: ABBA / BAAB; four runs per arm' \
    '  scorer: real validator/compiler and nine frozen future hashes' \
    '  server: one capture-only edit_clojure tool; no product write handler' \
    '  prompt: identical task bytes and no candidate-language leak'
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
integration_base=4f69761968af256d767ac97948f88bfb48cdcf1e
if ! git -C "$repo_root" merge-base --is-ancestor "$integration_base" "$git_head" \
  || ! git -C "$repo_root" diff --quiet "$integration_base" -- src test; then
  echo "Screen requires product source/test bytes from integration base 4f69761" >&2
  exit 2
fi

jq -n \
  --arg schema clj-surgeon.owner-aware-call-screen-config.v1 \
  --arg head "$git_head" --arg base "$integration_base" \
  --arg model "$model" --arg reasoning "$reasoning" \
  --arg harness_sha "$(shasum -a 256 "${BASH_SOURCE[0]}" | awk '{print $1}')" \
  --arg scorer_sha "$(shasum -a 256 "$repo_root/dev/experiments/owner_aware_call_construction_screen.clj" | awk '{print $1}')" \
  --arg fixture_sha "$(shasum -a 256 "$repo_root/bench/fixtures/edit_portfolio/submission-row-extraction-cleanup/task.txt" | awk '{print $1}')" \
  '{schema:$schema,git_head:$head,integration_base:$base,
    model:$model,reasoning:$reasoning,
    order:["control","candidate","candidate","control","candidate","control","control","candidate"],
    hashes:{harness:$harness_sha,scorer:$scorer_sha,task:$fixture_sha}}' \
  > "$result_dir/run-config.json"

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
  [ "$(jq -r '.ok' "$registry_file")" = true ]
  [ "$(jq -r '."tool-names" | @json' "$registry_file")" = '["edit_clojure"]' ]
  expected_surface=$(jq -S -c '.tool' "$surface_file")
  actual_surface=$(jq -S -c '."tool-projection"[0]' "$registry_file")
  [ "$expected_surface" = "$actual_surface" ] || {
    echo "Codex-observed tool surface differs from the server receipt: $run_id" >&2
    exit 2
  }

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
  [ "$exit_code" -eq 0 ] || echo "Codex exited $exit_code: $run_id" >&2
  [ -s "$capture_file" ] || { echo "No captured call: $run_id" >&2; exit 2; }

  clojure -Sdeps '{:paths ["src" "test" "dev/experiments"]}' \
    -M:clj-surgeon/mcp -m owner-aware-call-construction-screen score \
    --arm "$arm" --capture "$capture_file" \
    --timing "$run_dir/event-timing.edn" --mcp-calls "$mcp_calls" \
    --shell-calls "$shell_calls" --file-changes "$file_changes" \
    > "$run_dir/score.edn"
  score_paths+=("$run_dir/score.edn")
  printf '%s %s\n' "$run_id" "$(bb -e '(-> *command-line-args* first slurp clojure.edn/read-string :correct println)' "$run_dir/score.edn")"

  cleanup_run
  mcp_pid=""
  trap - EXIT INT TERM
done

clojure -Sdeps '{:paths ["src" "test" "dev/experiments"]}' \
  -M:clj-surgeon/mcp -e \
  '(require (quote [clojure.edn :as edn])
            (quote [owner-aware-call-construction-screen :as screen]))
   (prn (screen/cohort-report
          (mapv #(edn/read-string (slurp %)) *command-line-args*)))' \
  "${score_paths[@]}" > "$result_dir/summary.edn"

cat "$result_dir/summary.edn"
