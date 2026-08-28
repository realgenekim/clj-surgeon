#!/usr/bin/env bash

# Cheap, one-call, zero-mutation routing screen for U/V/W/X catalogs.
#
# Every run starts an isolated candidate MCP server and fresh CODEX_HOME. A
# fresh Sol/high caller must route one complete extraction request to exactly
# one tool. The source is deliberately absent, so the call refuses before
# mutation and pays no formatting, lint, or verification cost.

set -euo pipefail

repo_root=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)
model=gpt-5.6-sol
reasoning=high
catalogs=(U V W X)
replicates=${BENCH_REPLICATES:-4}
timeout_seconds=${BENCH_TIMEOUT_SECONDS:-180}
result_dir=${BENCH_RESULT_DIR:-}
auth_file=${BENCH_AUTH_FILE:-${CODEX_HOME:-$HOME/.codex}/auth.json}
self_test=false

usage() {
  cat <<'EOF'
Usage: bench/run_catalog_recognition_screen.sh [OPTIONS]

Options:
  --output DIR       Result directory (required outside --self-test)
  --replicates N     Rotated counterbalance blocks (default: 4)
  --auth-file FILE   Codex auth.json to symlink into each fresh home
  --timeout SEC      Per-model-call timeout (default: 180)
  --self-test        Run pure shell falsifiers; launch no server or model

Environment equivalents: BENCH_RESULT_DIR, BENCH_REPLICATES,
BENCH_AUTH_FILE, and BENCH_TIMEOUT_SECONDS.
EOF
}

while [ "$#" -gt 0 ]; do
  case "$1" in
    --output) result_dir=${2:?--output requires a directory}; shift 2 ;;
    --replicates) replicates=${2:?--replicates requires a count}; shift 2 ;;
    --auth-file) auth_file=${2:?--auth-file requires a file}; shift 2 ;;
    --timeout) timeout_seconds=${2:?--timeout requires seconds}; shift 2 ;;
    --self-test) self_test=true; shift ;;
    -h|--help) usage; exit 0 ;;
    *) echo "Unknown argument: $1" >&2; usage >&2; exit 2 ;;
  esac
done

require_positive_integer() {
  local label=$1 value=$2
  if ! [[ "$value" =~ ^[1-9][0-9]*$ ]]; then
    echo "$label must be a positive integer: $value" >&2
    exit 2
  fi
}

catalog_order() {
  local replicate=$1 offset index
  offset=$(((replicate - 1) % ${#catalogs[@]}))
  for index in 0 1 2 3; do
    printf '%s\n' "${catalogs[$(((offset + index) % ${#catalogs[@]}))]}"
  done
}

routing_prompt() {
  cat <<'EOF'
This is a safe routing canary. Call exactly one available clj-surgeon MCP tool.
Do not inspect, read files, use shell commands, or modify files.

The complete decision is to move two exact named top-level owners from one
Clojure namespace into a new namespace. Use exactly these arguments:

{"extraction":{"file":"src/missing/source.clj","to":"src/missing/destination.clj","forms":["alpha","beta"],"public_forms":[],"caller_changes":[],"ignored_caller_files":[],"require_policy":"minimal","expect":{"forms":2,"caller_edits":0,"files":2}}}

Choose the single tool whose public contract owns this extraction. The source
is deliberately absent, so the tool must refuse before mutation. After that
one refusal, make no other tool call and reply: routing canary complete
EOF
}

write_codex_config() {
  local file=$1 url=$2 role_receipt=$3
  local enabled
  enabled=$(jq -r '[.roles[]] | unique | sort | map(tojson) | join(", ")' \
    "$role_receipt")
  cat > "$file" <<EOF
[mcp_servers.clj-surgeon]
url = "$url"
required = true
enabled_tools = [$enabled]
default_tools_approval_mode = "approve"
startup_timeout_sec = 30
tool_timeout_sec = 45
EOF
}

normalize_client_surface() {
  local registry_receipt=$1 role_receipt=$2 output=$3 extraction
  extraction=$(jq -r '.roles.extract' "$role_receipt")
  jq --arg extraction "$extraction" '
    .["tool-projection"]
    | map(if .name == $extraction
          then .name = "__EXTRACTION__"
          else . end)
    | sort_by(.name)
  ' "$registry_receipt" > "$output"
}

expected_client_surface() {
  local role_receipt=$1 output=$2
  jq -S '
    .tools
    | map(
        .["input-schema"] |= del(.oneOf)
        | if .annotations == null then .annotations = {}
          else .annotations = {
            title: .annotations.title,
            readOnlyHint: .annotations["read-only"],
            destructiveHint: .annotations.destructive,
            idempotentHint: .annotations.idempotent,
            openWorldHint: .annotations["open-world"]
          }
          end)
    | sort_by(.name)
  ' "$role_receipt" > "$output"
}

wait_for_model() {
  local pid=$1 deadline now
  deadline=$((SECONDS + timeout_seconds))
  while kill -0 "$pid" 2>/dev/null; do
    now=$SECONDS
    if [ "$now" -ge "$deadline" ]; then
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

self_test_run() {
  local tmp
  tmp=$(mktemp -d "${TMPDIR:-/tmp}/clj-surgeon-recognition-self-test.XXXXXX")
  self_test_tmp=$tmp
  trap 'rm -rf "${self_test_tmp:-}"' EXIT

  local actual_order expected_order
  actual_order=$(for r in 1 2 3 4; do
    catalog_order "$r" | tr -d '\n'
    printf '\n'
  done)
  expected_order=$'UVWX\nVWXU\nWXUV\nXUVW'
  [ "$actual_order" = "$expected_order" ]

  cat > "$tmp/started-items.json" <<'EOF'
[{"type":"mcp_tool_call","server":"clj-surgeon","tool":"move_clojure_forms"}]
EOF
  [ "$(jq -r '[.[] | select(.type == "mcp_tool_call" and .server == "clj-surgeon")][0].tool' "$tmp/started-items.json")" = move_clojure_forms ]
  [ "$(jq '[.[] | select(.type == "mcp_tool_call")] | length' "$tmp/started-items.json")" -eq 1 ]

  cat > "$tmp/advertised-surface.json" <<'EOF'
{"tools":[{"name":"inspect_clojure","description":"read","input-schema":{"type":"object","oneOf":[{"required":["requests"]}]},"output-schema":{"type":"object"},"annotations":{"title":"Inspect","read-only":true,"destructive":false,"idempotent":true,"open-world":false,"return-direct":false}}]}
EOF
  cat > "$tmp/expected-client-surface.json" <<'EOF'
[
  {
    "annotations": {
      "destructiveHint": false,
      "idempotentHint": true,
      "openWorldHint": false,
      "readOnlyHint": true,
      "title": "Inspect"
    },
    "description": "read",
    "input-schema": {
      "type": "object"
    },
    "name": "inspect_clojure",
    "output-schema": {
      "type": "object"
    }
  }
]
EOF
  expected_client_surface "$tmp/advertised-surface.json" \
    "$tmp/actual-client-surface.json"
  cmp "$tmp/expected-client-surface.json" "$tmp/actual-client-surface.json"

  routing_prompt > "$tmp/prompt.txt"
  if rg -q 'apply_clojure_changes|apply_clojure_extraction|extract_clojure|move_clojure_forms' \
    "$tmp/prompt.txt"; then
    echo "Routing prompt leaked a candidate extraction name" >&2
    return 1
  fi
  rg -q 'Call exactly one available clj-surgeon MCP tool' "$tmp/prompt.txt"
  rg -q 'deliberately absent' "$tmp/prompt.txt"

  printf '%s\n' \
    'catalog recognition screen self-test: PASS' \
    '  counterbalance: UVWX / VWXU / WXUV / XUVW' \
    '  scorer: exact first public MCP tool and one-call geometry' \
    '  transport: only observed Codex schema/annotation projection admitted' \
    '  prompt: no candidate extraction name leaked'
  rm -rf "$tmp"
  trap - EXIT
}

if [ "$self_test" = true ]; then
  self_test_run
  exit 0
fi

require_positive_integer BENCH_REPLICATES "$replicates"
require_positive_integer BENCH_TIMEOUT_SECONDS "$timeout_seconds"
if [ -z "$result_dir" ]; then
  echo "--output or BENCH_RESULT_DIR is required" >&2
  usage >&2
  exit 2
fi
if [ ! -f "$auth_file" ]; then
  echo "Codex auth file not found: $auth_file" >&2
  exit 2
fi
for command in bb clojure codex curl jq perl rg shasum; do
  command -v "$command" >/dev/null || {
    echo "Required command not found: $command" >&2
    exit 2
  }
done

mkdir -p "$result_dir"
result_dir=$(cd "$result_dir" && pwd)
printf '%s\n' \
  $'run_id\treplicate\tposition\tcatalog\texpected_first_tool\tchosen_first_tool\tcorrect\tone_call\twall_ms\texit_code\tmcp_calls\tshell_calls\tfile_changes\tfinal_response' \
  > "$result_dir/runs.tsv"

git_head=$(git -C "$repo_root" rev-parse HEAD)
git_tree=$(git -C "$repo_root" write-tree)
jq -n \
  --arg schema clj-surgeon.catalog-recognition-screen.v1 \
  --arg model "$model" --arg reasoning "$reasoning" \
  --arg git_head "$git_head" --arg git_tree "$git_tree" \
  --arg codex_version "$(codex --version)" \
  --arg harness_sha "$(shasum -a 256 "${BASH_SOURCE[0]}" | awk '{print $1}')" \
  --arg catalog_sha "$(shasum -a 256 "$repo_root/dev/experiments/clj_surgeon/experiments/mcp_candidate_catalog.clj" | awk '{print $1}')" \
  --arg capture_sha "$(shasum -a 256 "$repo_root/bench/capture_codex_mcp_registry.clj" | awk '{print $1}')" \
  --argjson replicates "$replicates" \
  '{schema:$schema, model:$model, reasoning:$reasoning,
    git_head:$git_head, git_tree:$git_tree, codex_version:$codex_version,
    replicates:$replicates, counterbalance:"rotated-latin-square",
    hashes:{harness:$harness_sha,candidate_catalog:$catalog_sha,
            registry_capture:$capture_sha}}' > "$result_dir/run-config.json"

baseline_surface="$result_dir/normalized-client-surface.baseline.json"
run_number=0

for replicate in $(seq 1 "$replicates"); do
  position=0
  while IFS= read -r catalog; do
    position=$((position + 1))
    run_number=$((run_number + 1))
    run_id=$(printf '%02d-r%02d-p%02d-%s' "$run_number" "$replicate" "$position" "$catalog")
    run_dir="$result_dir/$run_id"
    codex_home="$run_dir/codex-home"
    workspace="$run_dir/empty-workspace"
    ready_file="$run_dir/mcp-ready.edn"
    role_receipt="$run_dir/catalog-role-receipt.json"
    registry_receipt="$run_dir/codex-mcp-registry.json"
    mkdir -p "$run_dir" "$codex_home" "$workspace" "$run_dir/mcp-telemetry"
    ln -s "$auth_file" "$codex_home/auth.json"
    routing_prompt > "$run_dir/prompt.txt"
    find "$workspace" -type f -print0 | sort -z | xargs -0 shasum -a 256 \
      > "$run_dir/workspace-before.sha256"
    chmod a-w "$workspace"

    mcp_pid=""
    cleanup_run() {
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
        -X:clj-surgeon/mcp \
        clj-surgeon.experiments.mcp-candidate-catalog/start \
        :catalog ":$catalog" \
        :catalog-receipt-file "$(jq -Rn --arg value "$role_receipt" '$value')" \
        :tool-profile :full \
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
      if ! kill -0 "$mcp_pid" 2>/dev/null; then
        echo "Candidate server exited before readiness: $run_id" >&2
        cat "$run_dir/mcp-server.stderr" >&2
        exit 2
      fi
      sleep 0.25
    done
    [ -s "$ready_file" ] || { echo "Candidate server readiness timeout: $run_id" >&2; exit 2; }
    mcp_url=$(bb -e \
      '(-> *command-line-args* first slurp clojure.edn/read-string :url println)' \
      "$ready_file")
    write_codex_config "$codex_home/config.toml" "$mcp_url" "$role_receipt"

    CODEX_HOME="$codex_home" clojure -J-Xms32m -J-Xmx256m \
      -Sdeps '{:paths ["src" "bench"]}' -M \
      -m capture-codex-mcp-registry \
      --codex "$(command -v codex)" \
      --output "$registry_receipt" --server clj-surgeon \
      > "$run_dir/codex-mcp-registry.stdout" \
      2> "$run_dir/codex-mcp-registry.stderr"

    expected_tools=$(jq -c '[.roles[]] | unique | sort' "$role_receipt")
    actual_tools=$(jq -c '.["tool-names"] | sort' "$registry_receipt")
    if [ "$actual_tools" != "$expected_tools" ] || [ "$(jq -r '.["tool-names"] | length' "$registry_receipt")" -ne 5 ]; then
      echo "Client-visible registry parity failed: $run_id" >&2
      printf 'expected=%s\nactual=%s\n' "$expected_tools" "$actual_tools" >&2
      exit 2
    fi
    expected_client_surface "$role_receipt" \
      "$run_dir/admitted-client-surface.json"
    expected_surface=$(jq -S -c '.' \
      "$run_dir/admitted-client-surface.json")
    actual_surface=$(jq -S -c '.["tool-projection"] | sort_by(.name)' \
      "$registry_receipt")
    if [ "$actual_surface" != "$expected_surface" ]; then
      echo "Independent Codex client exceeded the admitted transport projection: $run_id" >&2
      exit 2
    fi
    normalize_client_surface "$registry_receipt" "$role_receipt" \
      "$run_dir/normalized-client-surface.json"
    if [ ! -f "$baseline_surface" ]; then
      cp "$run_dir/normalized-client-surface.json" "$baseline_surface"
    elif ! cmp -s "$baseline_surface" "$run_dir/normalized-client-surface.json"; then
      echo "U/V/W/X client-visible surfaces differ beyond extraction name: $run_id" >&2
      diff -u "$baseline_surface" "$run_dir/normalized-client-surface.json" >&2 || true
      exit 2
    fi

    start_ms=$(perl -MTime::HiRes=time -e 'printf "%.0f\n", time()*1000')
    set +e
    CODEX_HOME="$codex_home" codex exec --json --ephemeral --ignore-rules \
      --skip-git-repo-check --sandbox read-only --color never \
      -m "$model" -c "model_reasoning_effort=\"$reasoning\"" \
      -C "$workspace" "$(cat "$run_dir/prompt.txt")" \
      > "$run_dir/events.jsonl" 2> "$run_dir/stderr.txt" < /dev/null &
    codex_pid=$!
    wait_for_model "$codex_pid"
    exit_code=$?
    set -e
    end_ms=$(perl -MTime::HiRes=time -e 'printf "%.0f\n", time()*1000')
    wall_ms=$((end_ms - start_ms))

    jq -s -r '[.[] | select(.type == "item.completed" and .item.type == "agent_message")][-1].item.text // ""' \
      "$run_dir/events.jsonl" > "$run_dir/final.txt"
    jq -s '[.[] | select(.type == "item.started") | .item]' \
      "$run_dir/events.jsonl" > "$run_dir/started-items.json"
    mcp_calls=$(jq '[.[] | select(.type == "mcp_tool_call")] | length' \
      "$run_dir/started-items.json")
    shell_calls=$(jq '[.[] | select(.type == "command_execution")] | length' \
      "$run_dir/started-items.json")
    file_changes=$(jq '[.[] | select(.type == "file_change")] | length' \
      "$run_dir/started-items.json")
    expected_first=$(jq -r '.roles.extract' "$role_receipt")
    chosen_first=$(jq -r \
      '[.[] | select(.type == "mcp_tool_call" and .server == "clj-surgeon")][0].tool // ""' \
      "$run_dir/started-items.json")

    chmod u+w "$workspace"
    find "$workspace" -type f -print0 | sort -z | xargs -0 shasum -a 256 \
      > "$run_dir/workspace-after.sha256"
    mutation_free=true
    if ! cmp -s "$run_dir/workspace-before.sha256" "$run_dir/workspace-after.sha256" \
      || [ "$shell_calls" -ne 0 ] || [ "$file_changes" -ne 0 ]; then
      mutation_free=false
    fi
    one_call=false
    [ "$mcp_calls" -eq 1 ] && one_call=true
    correct=false
    if [ "$exit_code" -eq 0 ] && [ "$mutation_free" = true ] \
      && [ "$one_call" = true ] && [ "$chosen_first" = "$expected_first" ]; then
      correct=true
    fi
    final_response=$(tr '\t\r\n' ' ' < "$run_dir/final.txt")
    jq -n --arg expected "$expected_first" --arg chosen "$chosen_first" \
      --arg final_response "$final_response" \
      --argjson correct "$correct" --argjson one_call "$one_call" \
      --argjson mutation_free "$mutation_free" \
      --argjson mcp_calls "$mcp_calls" \
      --argjson shell_calls "$shell_calls" \
      --argjson file_changes "$file_changes" \
      --argjson wall_ms "$wall_ms" \
      '{ok:$correct,expected_first_tool:$expected,chosen_first_tool:$chosen,
        one_call:$one_call,mutation_free:$mutation_free,mcp_calls:$mcp_calls,
        shell_calls:$shell_calls,file_changes:$file_changes,wall_ms:$wall_ms,
        final_response:$final_response}' > "$run_dir/score.json"

    printf '%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\n' \
      "$run_id" "$replicate" "$position" "$catalog" "$expected_first" \
      "$chosen_first" "$correct" "$one_call" "$wall_ms" "$exit_code" \
      "$mcp_calls" "$shell_calls" "$file_changes" "$final_response" \
      >> "$result_dir/runs.tsv"
    printf '%-18s first=%-28s correct=%-5s wall=%sms\n' \
      "$run_id" "$chosen_first" "$correct" "$wall_ms"

    cleanup_run
    mcp_pid=""
    trap - EXIT INT TERM
  done < <(catalog_order "$replicate")
done

jq -Rn '
  [inputs | split("\t")]
  | .[1:]
  | map({catalog:.[3], correct:(.[6] == "true"), wall_ms:(.[8] | tonumber)})
  | group_by(.catalog)
  | map({catalog:.[0].catalog, runs:length,
         correct:(map(select(.correct)) | length),
         median_wall_ms:(map(.wall_ms) | sort | .[length / 2 | floor])})
' < "$result_dir/runs.tsv" > "$result_dir/summary.json"

printf 'Recognition screen complete: %s\n' "$result_dir"
jq . "$result_dir/summary.json"
