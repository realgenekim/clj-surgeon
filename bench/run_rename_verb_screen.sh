#!/usr/bin/env bash

set -euo pipefail

repo_root=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)
fixture_root="$repo_root/bench/fixtures/rename-verb-screen"
product_commit=c55de2279826af5ed21c90981591479dd2e802b2
sol_model=gpt-5.6-sol
spark_model=gpt-5.3-codex-spark
reasoning=high
arms=(V T T V T V V T V T T V)
timeout_seconds=180
result_root=
auth_file=
self_test=false
skip_spark=false
mcp_pid=

usage() {
  printf '%s\n' \
    'Usage: bench/run_rename_verb_screen.sh --output DIR --auth-file FILE [options]' \
    '' \
    'Options:' \
    '  --timeout SEC   Per-model-call timeout (default: 180)' \
    '  --self-test     Run zero-model fixture/proxy/scorer falsifiers only' \
    '  --skip-spark    Skip the optional two-run Spark V bonus'
}

while [ "$#" -gt 0 ]; do
  case "$1" in
    --output) result_root=${2:?--output requires a directory}; shift 2 ;;
    --auth-file) auth_file=${2:?--auth-file requires a file}; shift 2 ;;
    --timeout) timeout_seconds=${2:?--timeout requires seconds}; shift 2 ;;
    --self-test) self_test=true; shift ;;
    --skip-spark) skip_spark=true; shift ;;
    -h|--help) usage; exit 0 ;;
    *) printf 'Unknown argument: %s\n' "$1" >&2; usage >&2; exit 2 ;;
  esac
done

cleanup() {
  if [ -n "${mcp_pid:-}" ]; then
    kill "$mcp_pid" 2>/dev/null || true
    wait "$mcp_pid" 2>/dev/null || true
    mcp_pid=
  fi
}
trap cleanup EXIT INT TERM

now_ms() {
  perl -MTime::HiRes=time -e 'printf "%.0f\n", time()*1000'
}

last_message() {
  jq -s -r '[.[] | select(.type == "item.completed" and .item.type == "agent_message")][-1].item.text // ""' "$1"
}

assert_schedule() {
  local observed
  observed=$(printf '%s\n' "${arms[@]}" | jq -R . | jq -cs .)
  [ "$observed" = '["V","T","T","V","T","V","V","T","V","T","T","V"]' ]
  [ "$(printf '%s\n' "${arms[@]}" | rg -c '^V$')" -eq 6 ]
  [ "$(printf '%s\n' "${arms[@]}" | rg -c '^T$')" -eq 6 ]
}

run_zero_model_tests() {
  assert_schedule
  git -C "$repo_root" merge-base --is-ancestor "$product_commit" HEAD
  git -C "$repo_root" diff --quiet "$product_commit" -- src test
  clojure -J-Xms64m -J-Xmx512m \
    -Sdeps '{:paths ["src" "test" "bench" "dev/experiments"]}' \
    -M:clj-surgeon/mcp -e \
    '(load-file "dev/experiments/rename_verb_proxy_test.clj")
     (let [result (clojure.test/run-tests (quote rename-verb-proxy-test))]
       (when-not (zero? (+ (:fail result) (:error result)))
         (throw (ex-info "Rename verb proxy tests failed" {:result result}))))'
  uv run --quiet --python 3.12 --with tiktoken==0.11.0 \
    python -m unittest dev/experiments/test_score_rename_verb_run.py
  printf '%s\n' \
    'rename verb screen self-test: PASS' \
    '  schedule: V T T V T V V T V T T V' \
    '  proxy: exact closed verb -> eight published edit_clojure rows' \
    '  fixture: two files, one definition, seven references, exact common handler' \
    '  scorer: 64-byte verb falsifier and all three registered kill rules'
}

if [ "$self_test" = true ]; then
  run_zero_model_tests
  exit 0
fi

if [ -z "$result_root" ] || [ -z "$auth_file" ] || [ ! -f "$auth_file" ]; then
  printf '%s\n' '--output and an existing --auth-file are required' >&2
  exit 2
fi
if ! [[ "$timeout_seconds" =~ ^[1-9][0-9]*$ ]]; then
  printf '%s\n' '--timeout must be a positive integer' >&2
  exit 2
fi
for command in bb clojure codex curl git jq perl rg shasum tar uv; do
  command -v "$command" >/dev/null || {
    printf 'Required command not found: %s\n' "$command" >&2
    exit 2
  }
done

run_zero_model_tests

mkdir -p "$result_root"
result_root=$(cd "$result_root" && pwd -P)
runs_jsonl="$result_root/runs.jsonl"
: > "$runs_jsonl"

prompt=$(cat "$fixture_root/task.txt")
head=$(git -C "$repo_root" rev-parse HEAD)
tree=$(git -C "$repo_root" write-tree)
git -C "$repo_root" status --porcelain=v1 > "$result_root/git-status.txt"

jq -n \
  --arg schema clj-surgeon.rename-verb-screen-config.v1 \
  --arg product_commit "$product_commit" \
  --arg experiment_commit "$head" \
  --arg experiment_tree "$tree" \
  --arg model "$sol_model" \
  --arg spark_model "$spark_model" \
  --arg reasoning "$reasoning" \
  --arg codex_version "$(codex --version)" \
  --arg harness_sha "$(shasum -a 256 "$repo_root/bench/run_rename_verb_screen.sh" | awk '{print $1}')" \
  --arg proxy_sha "$(shasum -a 256 "$repo_root/dev/experiments/rename_verb_proxy.clj" | awk '{print $1}')" \
  --arg scorer_sha "$(shasum -a 256 "$repo_root/dev/experiments/score_rename_verb_run.py" | awk '{print $1}')" \
  --arg prompt_sha "$(shasum -a 256 "$fixture_root/task.txt" | awk '{print $1}')" \
  --arg plan_sha "$(shasum -a 256 "$repo_root/docs/plans/2026-08-30-rename-verb-screen.md" | awk '{print $1}')" \
  --argjson order "$(printf '%s\n' "${arms[@]}" | jq -R . | jq -s .)" \
  '{schema:$schema,product_commit:$product_commit,
    experiment_commit:$experiment_commit,experiment_tree:$experiment_tree,
    model:$model,spark_model:$spark_model,reasoning:$reasoning,
    route:"subscription-chatgpt",order:$order,codex_version:$codex_version,
    tokenizer:{package:"tiktoken",version:"0.11.0",encoding:"o200k_base"},
    hashes:{harness:$harness_sha,proxy:$proxy_sha,scorer:$scorer_sha,
            prompt:$prompt_sha,plan:$plan_sha}}' > "$result_root/run-config.json"

catalog_home="$result_root/catalog-codex-home"
mkdir -p "$catalog_home"
ln -s "$auth_file" "$catalog_home/auth.json"
CODEX_HOME="$catalog_home" codex debug models \
  > "$result_root/model-catalog.json" 2> "$result_root/model-catalog.stderr"
jq -r '.models[].slug' "$result_root/model-catalog.json" \
  | LC_ALL=C sort -u > "$result_root/catalog-model-identifiers.txt"
rg -qx "$sol_model" "$result_root/catalog-model-identifiers.txt" || {
  printf 'Required model absent from refreshed catalog: %s\n' "$sol_model" >&2
  exit 2
}

start_mcp() {
  local run_dir=$1 arm=$2 workspace=$3
  local ready_file="$run_dir/mcp-ready.edn"
  local started_ms ready_ms mcp_url
  mkdir -p "$run_dir/mcp-telemetry" "$run_dir/codex-home"
  started_ms=$(now_ms)
  (
    cd "$repo_root"
    exec clojure -J-Xms64m -J-Xmx512m \
      -Sdeps '{:paths ["src" "test" "bench" "dev/experiments"]}' \
      -X:clj-surgeon/mcp rename-verb-proxy/start \
      :arm ":$arm" \
      :capture-file "$(bb -e '(prn (first *command-line-args*))' "$run_dir/proxy-capture.json")" \
      :surface-receipt-file "$(bb -e '(prn (first *command-line-args*))' "$run_dir/advertised-surface.json")" \
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
      sed -n '1,180p' "$run_dir/mcp-server.stderr" >&2
      return 2
    }
    sleep 0.25
  done
  [ -s "$ready_file" ] || { printf '%s\n' 'MCP readiness timeout' >&2; return 2; }
  mcp_url=$(bb -e '(-> *command-line-args* first slurp clojure.edn/read-string :url println)' "$ready_file")
  curl --fail --silent --show-error "${mcp_url%/mcp}/healthz" >/dev/null
  ready_ms=$(now_ms)
  printf '%s\n' "$((ready_ms - started_ms))" > "$run_dir/mcp-bootstrap-ms.txt"
  bb "$repo_root/bench/write_mcp_config.clj" \
    "$run_dir/codex-home/config.toml" --url "$mcp_url" \
    --enabled-tools-edn '["edit_clojure"]' >/dev/null
}

stop_mcp() {
  cleanup
}

run_codex() {
  local run_dir=$1 model=$2 workspace=$3 prompt_text=$4 use_mcp=$5
  local start_ms end_ms deadline exit_code=0 codex_pid
  mkdir -p "$run_dir/codex-home" "$workspace"
  [ -e "$run_dir/codex-home/auth.json" ] || ln -s "$auth_file" "$run_dir/codex-home/auth.json"
  printf '%s\n' "$prompt_text" > "$run_dir/prompt.txt"
  printf '%s\n' "$model" > "$run_dir/requested-model.txt"
  local args=(exec --json --ephemeral --ignore-rules --skip-git-repo-check
              --sandbox workspace-write --color never -m "$model"
              -c "model_reasoning_effort=\"$reasoning\"" -C "$workspace")
  if [ "$use_mcp" = false ]; then
    args+=(--ignore-user-config)
  fi
  start_ms=$(now_ms)
  set +e
  CODEX_HOME="$run_dir/codex-home" codex "${args[@]}" "$prompt_text" \
    > "$run_dir/events.jsonl" 2> "$run_dir/stderr.txt" </dev/null &
  codex_pid=$!
  deadline=$((SECONDS + timeout_seconds))
  while kill -0 "$codex_pid" 2>/dev/null; do
    if [ "$SECONDS" -ge "$deadline" ]; then
      kill -TERM "$codex_pid" 2>/dev/null || true
      sleep 1
      kill -KILL "$codex_pid" 2>/dev/null || true
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

score_run() {
  local run_dir=$1 run_id=$2 cohort=$3 arm=$4 replicate=$5 model=$6
  uv run --quiet --python 3.12 --with tiktoken==0.11.0 \
    python "$repo_root/dev/experiments/score_rename_verb_run.py" score \
    --events "$run_dir/events.jsonl" \
    --workspace "$run_dir/workspace" \
    --expected "$fixture_root/after" \
    --request-output "$run_dir/request-arguments.jsonl" \
    --run-id "$run_id" --cohort "$cohort" --arm "$arm" \
    --replicate "$replicate" --model "$model" \
    --exit-code "$(cat "$run_dir/exit-code.txt")" \
    --wall-ms "$(cat "$run_dir/wall-ms.txt")" > "$run_dir/score.json"
  jq -c . "$run_dir/score.json" >> "$runs_jsonl"
}

run_cell() {
  local ordinal=$1 cohort=$2 arm=$3 replicate=$4 model=$5
  local run_id run_dir workspace
  run_id=$(printf '%02d-%s-r%s' "$ordinal" "$arm" "$replicate")
  run_dir="$result_root/$cohort/$run_id"
  workspace="$run_dir/workspace"
  mkdir -p "$workspace"
  cp -R "$fixture_root/before/." "$workspace/"
  start_mcp "$run_dir" "$arm" "$workspace"
  run_codex "$run_dir" "$model" "$workspace" "$prompt" true
  stop_mcp
  score_run "$run_dir" "$run_id" "$cohort" "$arm" "$replicate" "$model"
  jq -r '[.run_id,.arm,.model,.completed,.one_shot,.request_bytes,.request_o200k_tokens,.wall_ms] | @tsv' \
    "$run_dir/score.json"
}

ordinal=0
v_replicate=0
t_replicate=0
for arm in "${arms[@]}"; do
  ordinal=$((ordinal + 1))
  if [ "$arm" = V ]; then
    v_replicate=$((v_replicate + 1))
    replicate=$v_replicate
  else
    t_replicate=$((t_replicate + 1))
    replicate=$t_replicate
  fi
  run_cell "$ordinal" sol "$arm" "$replicate" "$sol_model"
done

spark_available=false
if [ "$skip_spark" = false ] \
  && rg -qx "$spark_model" "$result_root/catalog-model-identifiers.txt"; then
  probe_dir="$result_root/availability/$spark_model"
  probe_workspace="$probe_dir/workspace"
  mkdir -p "$probe_workspace"
  run_codex "$probe_dir" "$spark_model" "$probe_workspace" 'Reply with exactly: SPARK_OK' false
  if [ "$(cat "$probe_dir/exit-code.txt")" -eq 0 ] \
    && [ "$(cat "$probe_dir/final.txt")" = SPARK_OK ]; then
    spark_available=true
  fi
fi
printf '%s\n' "$spark_available" > "$result_root/spark-bonus-available.txt"

if [ "$spark_available" = true ]; then
  run_cell 1 spark V 1 "$spark_model"
  run_cell 2 spark V 2 "$spark_model"
fi

uv run --quiet --python 3.12 --with tiktoken==0.11.0 \
  python "$repo_root/dev/experiments/score_rename_verb_run.py" aggregate \
  --runs "$runs_jsonl" > "$result_root/summary.json"

{
  printf '%s\n' $'run_id\tcohort\tarm\treplicate\tmodel\tcompleted\texact\tone_shot\twrong_subject\tverb_adopted\tschema_fumble\trequest_bytes\trequest_o200k_tokens\toutput_tokens\tturns\twall_ms'
  jq -r '[.run_id,.cohort,.arm,.replicate,.model,.completed,.exact,.one_shot,
          .wrong_subject,.verb_adopted,.schema_fumble,.request_bytes,
          .request_o200k_tokens,.output_tokens,.turns,.wall_ms] | @tsv' "$runs_jsonl"
} > "$result_root/runs.tsv"

printf '%s\n' \
  "bench/run_rename_verb_screen.sh --output '<new-output-dir>' --auth-file '$auth_file'" \
  > "$result_root/replay-command.txt"

archive_inputs=(run-config.json git-status.txt model-catalog.json
                model-catalog.stderr catalog-model-identifiers.txt runs.jsonl
                runs.tsv summary.json replay-command.txt spark-bonus-available.txt
                sol)
[ -d "$result_root/spark" ] && archive_inputs+=(spark)
[ -d "$result_root/availability" ] && archive_inputs+=(availability)
tar -czf "$result_root/raw-receipts.tgz" -C "$result_root" "${archive_inputs[@]}"
(
  cd "$result_root"
  find . -type f ! -name SHA256SUMS -print | LC_ALL=C sort \
    | while IFS= read -r path; do shasum -a 256 "$path"; done
) > "$result_root/SHA256SUMS"

jq . "$result_root/summary.json"
