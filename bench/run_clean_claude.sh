#!/usr/bin/env bash
set -euo pipefail

repo_root=$(cd "$(dirname "$0")/.." && pwd)
timestamp=$(date -u +%Y%m%dT%H%M%SZ)
result_dir=${CLAUDE_BENCH_RESULT_DIR:-$repo_root/bench/results/2026-08-04-claude-fable-opus-$timestamp}
deadline_seconds=${CLAUDE_BENCH_DEADLINE_SECONDS:-90}
models=${CLAUDE_BENCH_MODELS:-fable opus}
tasks=${CLAUDE_BENCH_TASKS:-ops-registry-xray pair-view-edit pair-view-expect-edit}

now_ms() {
  perl -MTime::HiRes=time -e 'printf "%.0f\n", time * 1000'
}

write_state() {
  local destination=$1
  shift
  local stage="$destination.tmp.$BASHPID"
  : > "$stage"
  while [ "$#" -gt 0 ]; do
    printf '%s\t%s\n' "$1" "$2" >> "$stage"
    shift 2
  done
  mv "$stage" "$destination"
}

bounded_exec() {
  local run_dir=$1
  local deadline=$2
  shift 2
  local started_ms finished_ms exit_code terminal_state

  started_ms=$(now_ms)
  write_state "$run_dir/state.tsv" \
    state running \
    pid "$BASHPID" \
    started_utc "$(date -u +%Y-%m-%dT%H:%M:%SZ)" \
    started_ms "$started_ms" \
    deadline_seconds "$deadline" \
    cleanup "gtimeout TERM, then KILL after 1s"

  set +e
  gtimeout --signal=TERM --kill-after=1 "${deadline}s" "$@" \
    > "$run_dir/raw.jsonl" 2> "$run_dir/stderr.txt"
  exit_code=$?
  set -e

  finished_ms=$(now_ms)
  if [ "$exit_code" -eq 124 ] || [ "$exit_code" -eq 137 ]; then
    terminal_state=timeout
  elif [ "$exit_code" -eq 0 ]; then
    terminal_state=completed
  else
    terminal_state=failed
  fi
  write_state "$run_dir/command-terminal.tsv" \
    state "$terminal_state" \
    exit_code "$exit_code" \
    finished_utc "$(date -u +%Y-%m-%dT%H:%M:%SZ)" \
    finished_ms "$finished_ms" \
    wall_ms "$((finished_ms - started_ms))"
}

terminal_field() {
  local key=$1
  local file=$2
  awk -F '\t' -v key="$key" '$1 == key {print $2; exit}' "$file"
}

run_fake_child() {
  local run_id=$1
  local deadline=$2
  shift 2
  local run_dir="$result_dir/$run_id"
  mkdir -p "$run_dir"
  bounded_exec "$run_dir" "$deadline" "$@"
  cp "$run_dir/command-terminal.tsv" "$run_dir/terminal.tsv"
  printf '[claude-receipt]\t%s\t%s\t%sms\n' \
    "$run_id" \
    "$(terminal_field state "$run_dir/terminal.tsv")" \
    "$(terminal_field wall_ms "$run_dir/terminal.tsv")"
}

self_test() {
  local self_test_root started_ms finished_ms fast_finished stall_finished
  self_test_root=$(mktemp -d /tmp/clj-surgeon-claude-harness-self-test.XXXXXX)
  result_dir="$self_test_root/results"
  mkdir -p "$result_dir"
  started_ms=$(now_ms)

  run_fake_child fast 2 bash -c 'printf "%s\n" fast-preserved; sleep 0.05' &
  local fast_pid=$!
  run_fake_child failed 2 bash -c 'printf "%s\n" failed-preserved; exit 7' &
  local failed_pid=$!
  run_fake_child stalled 1 bash -c 'printf "%s\n" stalled-partial; sleep 10' &
  local stalled_pid=$!

  wait "$fast_pid"
  wait "$failed_pid"
  wait "$stalled_pid"
  finished_ms=$(now_ms)

  test "$(terminal_field state "$result_dir/fast/terminal.tsv")" = completed
  test "$(terminal_field state "$result_dir/failed/terminal.tsv")" = failed
  test "$(terminal_field state "$result_dir/stalled/terminal.tsv")" = timeout
  rg -q '^fast-preserved$' "$result_dir/fast/raw.jsonl"
  rg -q '^failed-preserved$' "$result_dir/failed/raw.jsonl"
  rg -q '^stalled-partial$' "$result_dir/stalled/raw.jsonl"
  fast_finished=$(terminal_field finished_ms "$result_dir/fast/terminal.tsv")
  stall_finished=$(terminal_field finished_ms "$result_dir/stalled/terminal.tsv")
  test "$fast_finished" -lt "$stall_finished"
  test "$((finished_ms - started_ms))" -lt 4000
  test -f "$result_dir/fast/terminal.tsv"
  test -f "$result_dir/failed/terminal.tsv"

  local self_test_workspace="$self_test_root/workspace"
  mkdir -p "$self_test_workspace"
  printf '%s\n' 'starting bytes' > "$self_test_workspace/fixture.txt"
  bb "$repo_root/bench/initialize_benchmark_workspace.clj" \
    "$self_test_workspace" >/dev/null
  test "$(git -C "$self_test_workspace" rev-list --count HEAD)" -eq 1
  test -z "$(git -C "$self_test_workspace" status --short)"

  rm -rf "$self_test_root"
  printf '%s\n' "Claude benchmark harness self-test passed: fast and failed receipts survived an independently timed-out child"
}

if [ "${CLAUDE_BENCH_HARNESS_SELF_TEST:-false}" = true ]; then
  self_test
  exit 0
fi

for command_name in claude gtimeout jq bb git perl shasum rg; do
  command -v "$command_name" >/dev/null 2>&1 || {
    echo "Missing required command: $command_name" >&2
    exit 2
  }
done

if ! [[ "$deadline_seconds" =~ ^[1-9][0-9]*$ ]]; then
  echo "CLAUDE_BENCH_DEADLINE_SECONDS must be a positive integer: $deadline_seconds" >&2
  exit 2
fi

source_commit=$(git -C "$repo_root" rev-parse HEAD)
installed_skill=${CLAUDE_BENCH_SKILL_PATH:-$HOME/.claude/skills/clj-surgeon}
installed_receipt="$installed_skill.receipt.edn"
canonical_skill="$repo_root/skills/clj-surgeon/SKILL.md"
project_claude_skill="$repo_root/.claude/skills/clj-surgeon/SKILL.md"

test -f "$installed_receipt" || {
  echo "Installed Claude skill receipt not found: $installed_receipt" >&2
  exit 2
}
rg -q ':artifact :claude-skill' "$installed_receipt"
rg -q ':mode :stable-copy' "$installed_receipt"
rg -q ":source-commit \"$source_commit\"" "$installed_receipt"
test -f "$installed_skill/SKILL.md"
expected_skill=$(mktemp /tmp/clj-surgeon-expected-skill.XXXXXX)
cat "$canonical_skill" > "$expected_skill"
printf '\nStable copy installed from commit %s.\nWhen working inside the clj-surgeon repository, the working-tree skill.md\nsupersedes this copy.\n' \
  "$source_commit" >> "$expected_skill"
cmp -s "$installed_skill/SKILL.md" "$expected_skill"
rm -f "$expected_skill"
cmp -s "$canonical_skill" "$project_claude_skill"
cmp -s "$installed_skill/references/advanced-operations.md" \
  "$repo_root/.claude/skills/clj-surgeon/references/advanced-operations.md"

skill_file_hash=$(shasum -a 256 "$installed_skill/SKILL.md" | awk '{print $1}')
skill_source_hash=$(awk '/:source-hash/ {gsub(/[\"}]/, "", $2); print $2}' "$installed_receipt")
cli_version_root=${CLAUDE_BENCH_CLI_VERSION_ROOT:-$HOME/.local/share/clj-surgeon/versions/$source_commit}
if [ -n "${CLAUDE_BENCH_CLI_PACKAGE:-}" ]; then
  cli_package=$CLAUDE_BENCH_CLI_PACKAGE
else
  shopt -s nullglob
  cli_packages=("$cli_version_root"/cli-*)
  shopt -u nullglob
  if [ "${#cli_packages[@]}" -ne 1 ]; then
    echo "Expected exactly one immutable CLI package under $cli_version_root; found ${#cli_packages[@]}. Set CLAUDE_BENCH_CLI_PACKAGE explicitly." >&2
    exit 2
  fi
  cli_package=${cli_packages[0]}
fi
test -f "$cli_package/install-receipt.edn"
rg -q ":source-commit \"$source_commit\"" "$cli_package/install-receipt.edn"
cli_source_hash=$(awk '/:source-hash/ {gsub(/[\"}]/, "", $2); print $2}' "$cli_package/install-receipt.edn")

mkdir -p "$result_dir"
cp "$installed_receipt" "$result_dir/installed-claude-skill.receipt.edn"
cp "$cli_package/install-receipt.edn" "$result_dir/installed-cli-package.receipt.edn"
write_state "$result_dir/matrix.tsv" \
  source_commit "$source_commit" \
  installed_skill "$installed_skill" \
  skill_file_sha256 "$skill_file_hash" \
  skill_source_sha256 "$skill_source_hash" \
  immutable_cli_package "$cli_package" \
  cli_source_sha256 "$cli_source_hash" \
  deadline_seconds "$deadline_seconds" \
  requested_models "$models" \
  requested_tasks "$tasks" \
  claude_version "$(claude --version)"

make_launcher() {
  local destination=$1
  local stage="$destination.tmp.$BASHPID"
  printf '%s\n' \
    '#!/usr/bin/env bb' \
    ';; benchmark launcher pinned to an immutable installed package' \
    '(require (quote [babashka.classpath :as cp]))' \
    "(cp/add-classpath \"$cli_package/src\")" \
    '(require (quote [clj-surgeon.core :as core]))' \
    '(apply core/-main *command-line-args*)' > "$stage"
  chmod +x "$stage"
  mv "$stage" "$destination"
}

write_prompt() {
  local task=$1
  local destination=$2
  case "$task" in
    ops-registry-xray)
      printf '%s\n' 'Load and follow the installed clj-surgeon skill. In src/bench/ops_registry.clj, analyze the hash-map inside ops-registry. Return category frequencies, the total number of argument specs whose :required value is true, and the sorted operation keywords whose specs contain :pair. Do not modify files or read the whole file. Your final answer must be exactly one EDN map with keys :category-frequencies, :required-arg-count, and :paired-ops, with no prose or code fence.' > "$destination"
      ;;
    pair-view-edit)
      printf '%s\n' 'Load and follow the installed clj-surgeon skill. In src/bench/pair_view.clj, change only the :finish result inside route-event so its :status value is :complete instead of :done. Preserve its attached comment and every unrelated byte. Generate and review an :edit plan at plan.edn, then apply that plan in a separate shell command with :replace-subform!. Do not combine planning and application. Do not read the whole file. In the final answer, state whether the plan and apply both succeeded.' > "$destination"
      ;;
    pair-view-expect-edit)
      printf '%s\n' 'Load and follow the installed clj-surgeon skill. In src/bench/pair_view.clj, change only the :finish result inside route-event so its :status value is :complete instead of :done. Preserve its attached comment and every unrelated byte. Complete the change with a single guarded edit call that declares the expected before-state so the tool itself refuses if your declaration is wrong; do not use a separate apply command. If the guard refuses, recover using the refusal'"'"'s evidence and finish with a corrected guarded call. Do not read the whole file. In the final answer, state whether your first declaration matched.' > "$destination"
      ;;
    *)
      echo "Unknown Claude benchmark task: $task" >&2
      return 2
      ;;
  esac
}

extract_results() {
  local run_dir=$1
  : > "$run_dir/tool-calls.jsonl"
  jq -c 'select(.type == "assistant") | .message.content[]? | select(.type == "tool_use") | {name, input}' \
    "$run_dir/raw.jsonl" > "$run_dir/tool-calls.jsonl" 2> "$run_dir/jq-errors.txt" || true
  jq -c 'select(.type == "result")' "$run_dir/raw.jsonl" 2>> "$run_dir/jq-errors.txt" \
    | tail -1 > "$run_dir/result.json" || true
  jq -r '.result // empty' "$run_dir/result.json" > "$run_dir/final.txt" 2>> "$run_dir/jq-errors.txt" || true
  jq -c '{usage, total_cost_usd, duration_ms, duration_api_ms, num_turns, modelUsage}' \
    "$run_dir/result.json" > "$run_dir/usage.json" 2>> "$run_dir/jq-errors.txt" || true
  jq -r 'select(.type == "system" and .subtype == "init") | .model // empty' \
    "$run_dir/raw.jsonl" 2>> "$run_dir/jq-errors.txt" | tail -1 > "$run_dir/resolved-model.txt" || true
  if [ ! -s "$run_dir/resolved-model.txt" ]; then
    jq -r '.modelUsage // {} | keys[]?' "$run_dir/result.json" 2>> "$run_dir/jq-errors.txt" \
      | paste -sd, - > "$run_dir/resolved-model.txt" || true
  fi
}

tool_call_contract() {
  local run_dir=$1
  local task=$2
  local skill_loaded=false route_used=false separate_plan_apply=false
  local tool_calls skill_calls xray_calls edit_plan_calls apply_calls true_one_shot=false
  local expect_edit_calls expect_mismatch_seen=false
  tool_calls=$(jq -s 'length' "$run_dir/tool-calls.jsonl")
  skill_calls=$(jq -s '[.[] | select(.name == "Skill" and (.input | tostring | contains("clj-surgeon")))] | length' "$run_dir/tool-calls.jsonl")
  xray_calls=$(jq -s '[.[] | select(.name == "Bash" and ((.input.command // "") | contains(":op :xray")))] | length' "$run_dir/tool-calls.jsonl")
  edit_plan_calls=$(jq -s '[.[] | select(.name == "Bash" and ((.input.command // "") | test(":op :edit([^a-zA-Z!-]|$)")))] | length' "$run_dir/tool-calls.jsonl")
  apply_calls=$(jq -s '[.[] | select(.name == "Bash" and ((.input.command // "") | contains(":op :replace-subform!")))] | length' "$run_dir/tool-calls.jsonl")
  expect_edit_calls=$(jq -s '[.[] | select(.name == "Bash" and ((.input.command // "") | test(":op :edit([^a-zA-Z!-]|$)")) and ((.input.command // "") | contains(":expect")))] | length' "$run_dir/tool-calls.jsonl")
  if jq -e -s '[.. | objects | select(.type? == "tool_result") | tostring] | any(contains(":error-type :expect-mismatch"))' \
    "$run_dir/raw.jsonl" >/dev/null 2>&1; then
    expect_mismatch_seen=true
  fi
  if jq -e 'select(.name == "Skill") | .input | tostring | contains("clj-surgeon")' \
    "$run_dir/tool-calls.jsonl" >/dev/null 2>&1; then
    skill_loaded=true
  fi
  case "$task" in
    ops-registry-xray)
      if jq -e 'select(.name == "Bash") | .input.command // "" | contains(":op :xray")' \
        "$run_dir/tool-calls.jsonl" >/dev/null 2>&1; then
        route_used=true
      fi
      if [ "$xray_calls" -eq 1 ]; then
        true_one_shot=true
      fi
      ;;
    pair-view-edit)
      local plan_line apply_line combined_line
      plan_line=$(jq -r 'select(.name == "Bash") | .input.command // ""' "$run_dir/tool-calls.jsonl" \
        | awk '/:op :edit/ {print NR; exit}')
      apply_line=$(jq -r 'select(.name == "Bash") | .input.command // ""' "$run_dir/tool-calls.jsonl" \
        | awk '/:op :replace-subform!/ {print NR; exit}')
      combined_line=$(jq -r 'select(.name == "Bash") | .input.command // ""' "$run_dir/tool-calls.jsonl" \
        | awk '/:op :edit/ && /:op :replace-subform!/ {print NR; exit}')
      if [ -n "$plan_line" ] && [ -n "$apply_line" ] && [ -z "$combined_line" ] \
        && [ "$plan_line" -lt "$apply_line" ]; then
        route_used=true
        separate_plan_apply=true
      fi
      if [ "$edit_plan_calls" -eq 1 ] && [ "$apply_calls" -eq 1 ] \
        && [ "$separate_plan_apply" = true ]; then
        true_one_shot=true
      fi
      ;;
    pair-view-expect-edit)
      # The guarded route is one :edit call carrying :expect and no separate
      # apply; separate_plan_apply does not apply and stays false.
      separate_plan_apply=false
      if [ "$expect_edit_calls" -ge 1 ] && [ "$apply_calls" -eq 0 ]; then
        route_used=true
      fi
      if [ "$expect_edit_calls" -eq 1 ] && [ "$apply_calls" -eq 0 ] \
        && [ "$route_used" = true ]; then
        true_one_shot=true
      fi
      ;;
  esac
  write_state "$run_dir/tool-contract.tsv" \
    skill_loaded "$skill_loaded" \
    route_used "$route_used" \
    separate_plan_apply "$separate_plan_apply" \
    tool_calls "$tool_calls" \
    skill_calls "$skill_calls" \
    xray_calls "$xray_calls" \
    edit_plan_calls "$edit_plan_calls" \
    expect_edit_calls "$expect_edit_calls" \
    expect_mismatch_seen "$expect_mismatch_seen" \
    apply_calls "$apply_calls" \
    true_one_shot "$true_one_shot"
}

score_run() {
  local run_dir=$1
  local task=$2
  local source_file
  local bytes_correct=false workflow_correct=false source_unchanged=false
  if [ "$task" = ops-registry-xray ]; then
    source_file="$run_dir/workspace/src/bench/ops_registry.clj"
  else
    source_file="$run_dir/workspace/src/bench/pair_view.clj"
  fi
  shasum -a 256 "$source_file" > "$run_dir/final.sha256"
  diff -u "$run_dir/source.start.clj" "$source_file" > "$run_dir/source.diff" || true

  case "$task" in
    ops-registry-xray)
      if [ "$(bb "$repo_root/bench/score_ops_registry.clj" "$source_file" "$run_dir/final.txt")" = true ]; then
        bytes_correct=true
      fi
      if cmp -s "$run_dir/source.start.clj" "$source_file"; then
        source_unchanged=true
      fi
      ;;
    pair-view-edit|pair-view-expect-edit)
      if cmp -s "$run_dir/expected.clj" "$source_file"; then
        bytes_correct=true
      fi
      ;;
  esac
  if [ "$(terminal_field skill_loaded "$run_dir/tool-contract.tsv")" = true ] \
    && [ "$(terminal_field route_used "$run_dir/tool-contract.tsv")" = true ]; then
    workflow_correct=true
  fi
  write_state "$run_dir/score.tsv" \
    bytes_correct "$bytes_correct" \
    workflow_correct "$workflow_correct" \
    source_unchanged "$source_unchanged" \
    correct "$([ "$bytes_correct" = true ] && [ "$workflow_correct" = true ] && { [ "$task" != ops-registry-xray ] || [ "$source_unchanged" = true ]; } && printf true || printf false)"
}

run_actual_child() {
  local model=$1
  local task=$2
  local run_id="$model-$task"
  local run_dir="$result_dir/$run_id"
  local workspace="$run_dir/workspace"
  local source_file command_state command_exit terminal_state
  mkdir -p "$workspace/src/bench" "$workspace/.claude/skills" "$workspace/bin"
  ln -s "$installed_skill" "$workspace/.claude/skills/clj-surgeon"
  make_launcher "$workspace/bin/clj-surgeon"
  write_prompt "$task" "$run_dir/prompt.txt"

  if [ "$task" = ops-registry-xray ]; then
    source_file="$workspace/src/bench/ops_registry.clj"
    cp "$repo_root/src/clj_surgeon/core.clj" "$source_file"
  else
    source_file="$workspace/src/bench/pair_view.clj"
    cp "$repo_root/bench/fixtures/bench/pair_view.clj" "$source_file"
    perl -0pe 's/\(assoc state :status :done :audit/\(assoc state :status :complete :audit/' \
      "$source_file" > "$run_dir/expected.clj"
    shasum -a 256 "$run_dir/expected.clj" > "$run_dir/expected.sha256"
  fi
  bb "$repo_root/bench/initialize_benchmark_workspace.clj" "$workspace" >/dev/null
  cp "$source_file" "$run_dir/source.start.clj"
  shasum -a 256 "$source_file" > "$run_dir/start.sha256"
  cp "$installed_skill/SKILL.md" "$run_dir/SKILL.md"
  shasum -a 256 "$run_dir/SKILL.md" > "$run_dir/skill.sha256"
  cp "$installed_receipt" "$run_dir/skill-install-receipt.edn"
  write_state "$run_dir/request.tsv" \
    requested_model "$model" \
    task "$task" \
    deadline_seconds "$deadline_seconds" \
    max_budget_usd "$([ "$model" = opus ] && printf '%s' "${CLAUDE_BENCH_OPUS_MAX_BUDGET_USD:-5.00}" || printf '%s' "${CLAUDE_BENCH_FABLE_MAX_BUDGET_USD:-2.00}")" \
    skill_file_sha256 "$skill_file_hash" \
    skill_source_sha256 "$skill_source_hash" \
    cli_source_sha256 "$cli_source_hash" \
    workspace "$workspace"

  local budget
  budget=$(terminal_field max_budget_usd "$run_dir/request.tsv")
  local -a command=(claude -p
    --model "$model"
    --output-format stream-json
    --verbose
    --permission-mode bypassPermissions
    --allowedTools "Skill,Bash"
    --max-budget-usd "$budget"
    --no-session-persistence
    "$(< "$run_dir/prompt.txt")")
  printf '%q ' "${command[@]}" > "$run_dir/command.txt"
  printf '\n' >> "$run_dir/command.txt"

  (
    cd "$workspace"
    PATH="$workspace/bin:$PATH" bounded_exec "$run_dir" "$deadline_seconds" "${command[@]}"
  )
  extract_results "$run_dir"
  if [ -f "$workspace/plan.edn" ]; then
    cp "$workspace/plan.edn" "$run_dir/applied-plan.edn"
  fi
  tool_call_contract "$run_dir" "$task"
  score_run "$run_dir" "$task"

  command_state=$(terminal_field state "$run_dir/command-terminal.tsv")
  command_exit=$(terminal_field exit_code "$run_dir/command-terminal.tsv")
  if [ "$command_state" = completed ]; then
    terminal_state=$(terminal_field correct "$run_dir/score.tsv")
    [ "$terminal_state" = true ] && terminal_state=passed || terminal_state=incorrect
  else
    terminal_state=$command_state
  fi
  write_state "$run_dir/terminal.tsv" \
    state "$terminal_state" \
    command_state "$command_state" \
    exit_code "$command_exit" \
    requested_model "$model" \
    resolved_model "$(< "$run_dir/resolved-model.txt")" \
    wall_ms "$(terminal_field wall_ms "$run_dir/command-terminal.tsv")" \
    correct "$(terminal_field correct "$run_dir/score.tsv")" \
    result_dir "$run_dir"
  printf '[claude-receipt]\t%s\t%s\t%s\t%sms\n' \
    "$run_id" "$terminal_state" "$(< "$run_dir/resolved-model.txt")" \
    "$(terminal_field wall_ms "$run_dir/terminal.tsv")"
}

declare -a child_pids=()
for model in $models; do
  for task in $tasks; do
    run_actual_child "$model" "$task" &
    child_pids+=("$!")
  done
done

matrix_exit=0
for child_pid in "${child_pids[@]}"; do
  wait "$child_pid" || matrix_exit=1
done

printf '%b\n' 'run_id\tstate\trequested_model\tresolved_model\twall_ms\tcorrect' > "$result_dir/runs.tsv"
for terminal in "$result_dir"/*/terminal.tsv; do
  run_dir=${terminal%/terminal.tsv}
  printf '%s\t%s\t%s\t%s\t%s\t%s\n' \
    "${run_dir##*/}" \
    "$(terminal_field state "$terminal")" \
    "$(terminal_field requested_model "$terminal")" \
    "$(terminal_field resolved_model "$terminal")" \
    "$(terminal_field wall_ms "$terminal")" \
    "$(terminal_field correct "$terminal")" >> "$result_dir/runs.tsv"
done

manifest_stage="$result_dir/MANIFEST.sha256.tmp.$$"
(
  cd "$result_dir"
  find . -type f ! -name 'MANIFEST.sha256*' -print | LC_ALL=C sort \
    | while IFS= read -r file; do shasum -a 256 "$file"; done
) > "$manifest_stage"
mv "$manifest_stage" "$result_dir/MANIFEST.sha256"

printf '%s\n' "Claude benchmark receipts: $result_dir"
case "${BENCH_RETENTION:-archive}" in
  archive)
    result_abs="$(cd "$(dirname "$result_dir")" && pwd -P)/$(basename "$result_dir")"
    case "$result_abs" in
      "$repo_root/bench/results/"*)
        bash "$repo_root/bench/retain_benchmark_result.sh" "$result_abs"
        ;;
    esac
    ;;
  local) ;;
  *)
    echo "BENCH_RETENTION must be archive or local: ${BENCH_RETENTION}" >&2
    exit 2
    ;;
esac
exit "$matrix_exit"
