#!/usr/bin/env bash
set -euo pipefail

script_path=$(cd "$(dirname "$0")" && pwd)/$(basename "$0")
repo_root=$(cd "$(dirname "$0")/.." && pwd)
timestamp=$(date -u +%Y%m%dT%H%M%SZ)
result_dir=${BENCH_RESULT_DIR:-/tmp/clj-surgeon-clean-codex-$timestamp}
computed_site_count=${BENCH_COMPUTED_SITE_COUNT:-10}
owner_dir="$result_dir/.benchmark-owner"
owner_metadata="$owner_dir/owner.tsv"
owner_token=""

if ! [[ "$computed_site_count" =~ ^[1-9][0-9]*$ ]] \
  || [ "$computed_site_count" -gt 128 ]; then
  echo "BENCH_COMPUTED_SITE_COUNT must be an integer from 1 through 128: $computed_site_count" >&2
  exit 2
fi

owner_field() {
  local key=$1
  local file=$2
  awk -F '\t' -v key="$key" '$1 == key {print substr($0, length($1) + 2); exit}' "$file" 2>/dev/null || true
}

owner_is_live() {
  local metadata=$1
  local recorded_pid recorded_host recorded_start current_start
  recorded_pid=$(owner_field pid "$metadata")
  recorded_host=$(owner_field host "$metadata")
  recorded_start=$(owner_field process_start "$metadata")
  if [ "$recorded_host" != "$(hostname)" ] || ! [[ "$recorded_pid" =~ ^[1-9][0-9]*$ ]]; then
    return 1
  fi
  kill -0 "$recorded_pid" 2>/dev/null || return 1
  current_start=$(ps -p "$recorded_pid" -o lstart= 2>/dev/null | awk '{$1=$1; print}')
  [ -n "$recorded_start" ] && [ "$current_start" = "$recorded_start" ]
}

print_owner_diagnostic() {
  local state=$1
  echo "Benchmark result directory has a $state owner: $result_dir" >&2
  if [ -f "$owner_metadata" ]; then
    sed 's/^/  /' "$owner_metadata" >&2
  else
    echo "  owner metadata is missing or incomplete" >&2
  fi
}

release_result_owner() {
  [ -n "$owner_token" ] || return 0
  if [ -f "$owner_metadata" ] && [ "$(owner_field token "$owner_metadata")" = "$owner_token" ]; then
    rm -f "$owner_metadata"
    rmdir "$owner_dir" 2>/dev/null || true
  fi
  owner_token=""
}

acquire_result_owner() {
  mkdir -p "$result_dir"
  if ! mkdir "$owner_dir" 2>/dev/null; then
    if owner_is_live "$owner_metadata"; then
      print_owner_diagnostic live
      echo "Refusing a concurrent writer before runs.tsv or run artifacts are changed." >&2
      return 3
    fi
    print_owner_diagnostic stale-or-unverifiable
    if [ "${BENCH_RECOVER_STALE_OWNER:-false}" != true ]; then
      echo "Inspect the metadata, then recover explicitly with BENCH_RECOVER_STALE_OWNER=true and BENCH_RESUME=true." >&2
      return 4
    fi
    local recovered_dir="$result_dir/.benchmark-owner.recovered-$timestamp-$$"
    if ! mv "$owner_dir" "$recovered_dir" 2>/dev/null; then
      echo "Owner changed while stale recovery was attempted; retry after inspecting $result_dir." >&2
      return 4
    fi
    echo "Recovered stale benchmark ownership metadata to: $recovered_dir" >&2
    if ! mkdir "$owner_dir" 2>/dev/null; then
      echo "Another writer acquired benchmark ownership during recovery; refusing." >&2
      return 3
    fi
  fi

  owner_token="$timestamp-$$"
  local process_start command_text metadata_tmp
  process_start=$(ps -p $$ -o lstart= | awk '{$1=$1; print}')
  command_text=$(ps -p $$ -o command= | tr '\t\n' '  ')
  metadata_tmp="$owner_dir/owner.tsv.tmp.$$"
  printf '%s\t%s\n' \
    token "$owner_token" \
    pid "$$" \
    host "$(hostname)" \
    process_start "$process_start" \
    started_utc "$(date -u +%Y-%m-%dT%H:%M:%SZ)" \
    command "$command_text" \
    > "$metadata_tmp"
  mv "$metadata_tmp" "$owner_metadata"
}

row_exists() {
  local file=$1
  local run_id=$2
  [ -f "$file" ] && awk -F '\t' -v id="$run_id" '$1 == id {found=1} END {exit !found}' "$file"
}

acquire_row_lock() {
  local run_id=$1
  local lock_dir="$result_dir/.runs-lock"
  local timeout=${BENCH_ROW_LOCK_TIMEOUT_SECONDS:-10}
  local attempts attempt=0
  if ! [[ "$timeout" =~ ^[1-9][0-9]*$ ]]; then
    echo "BENCH_ROW_LOCK_TIMEOUT_SECONDS must be a positive integer: $timeout" >&2
    return 2
  fi
  attempts=$((timeout * 20))
  until mkdir "$lock_dir" 2>/dev/null; do
    attempt=$((attempt + 1))
    if [ "$attempt" -ge "$attempts" ]; then
      echo "Timed out after ${timeout}s waiting for row lock for $run_id: $lock_dir" >&2
      if [ -f "$lock_dir/owner.tsv" ]; then
        sed 's/^/  /' "$lock_dir/owner.tsv" >&2
      else
        echo "  row-lock owner metadata is missing; remove the lock only after confirming no child writer is active" >&2
      fi
      return 5
    fi
    sleep 0.05
  done
  printf '%s\t%s\n' pid "$$" run_id "$run_id" started_utc "$(date -u +%Y-%m-%dT%H:%M:%SZ)" \
    > "$lock_dir/owner.tsv"
}

release_row_lock() {
  local lock_dir="$result_dir/.runs-lock"
  rm -f "$lock_dir/owner.tsv"
  rmdir "$lock_dir" 2>/dev/null || true
}

append_result_row() {
  local run_id=$1
  local row=$2
  acquire_row_lock "$run_id" || return
  if ! row_exists "$result_dir/runs.tsv" "$run_id"; then
    printf '%s\n' "$row" >> "$result_dir/runs.tsv"
  fi
  release_row_lock
}

interaction_counts() {
  jq -r -s '
    ([.[] | select(.type == "turn.completed")] | length) as $user_turns
    | [.[]
       | select(.type == "item.started"
                and (.item.type == "command_execution"
                     or .item.type == "file_change"
                     or .item.type == "mcp_tool_call"))
       | .item] as $tool_items
    | ($tool_items
       | map(.type == "file_change"
             or (.type == "mcp_tool_call"
                 and .server == "clj-surgeon"
                 and (.tool == "apply_clojure_changes"
                      or .tool == "edit_clojure"
                      or .tool == "transform_clojure"))
             or (.type == "command_execution"
                 and (((.command // "") | test("(^|[ /])apply_patch( |$)"))
                      or (((.command // "") | contains("clj-surgeon"))
                          and (((.command // "")
                                | test(":op[[:space:]]+(:)?(change!|replace-subform!|mv|mv-with-deps|extract!|rename-ns!|fix-declares!)([^a-zA-Z!-]|$)"))
                               or (((.command // "")
                                    | test(":op[[:space:]]+(:)?edit([^a-zA-Z!-]|$)"))
                                   and ((.command // "") | contains(":expect"))))))))
       | index(true)) as $mutation_index
    | ($tool_items | length) as $tool_round_trips
    | (if $mutation_index == null then $tool_round_trips else $mutation_index end)
      as $discovery_round_trips
    | (if $mutation_index == null then 0 else $tool_round_trips - $mutation_index end)
      as $post_decision_round_trips
    | [$user_turns, $tool_round_trips, $discovery_round_trips, $post_decision_round_trips]
    | @tsv' "$1"
}

mcp_apply_success_count() {
  jq -s '[.[]
    | select(.type == "item.completed"
             and .item.type == "mcp_tool_call"
             and .item.server == "clj-surgeon"
             and (.item.tool == "apply_clojure_changes"
                  or .item.tool == "edit_clojure"
                  or .item.tool == "transform_clojure")
             and .item.status == "completed")
    | (.item.result.structured_content // .item.result.structuredContent // {}) as $receipt
    | select($receipt.verification_complete == true and $receipt.committed == true)]
    | length' "$1"
}

mcp_apply_verified() {
  jq -s '[.[]
    | select(.type == "item.completed"
             and .item.type == "mcp_tool_call"
             and .item.server == "clj-surgeon"
             and (.item.tool == "apply_clojure_changes"
                  or .item.tool == "edit_clojure"
                  or .item.tool == "transform_clojure")
             and .item.status == "completed")
    | (.item.result.structured_content // .item.result.structuredContent // {}) as $receipt
    | select($receipt.verification_complete == true
             and $receipt.committed == true
             and $receipt.next_action == "none")]
    | length > 0' "$1"
}

native_mutation_failure_count() {
  local events_file=$1
  local stderr_file=$2
  local event_failures stderr_failures
  event_failures=$(jq -s '[.[] | select(.type == "item.completed"
    and .item.type == "file_change"
    and ((.item.status // "completed") != "completed"))] | length' "$events_file")
  # Codex currently omits refused apply_patch attempts from the JSON event stream.
  # Preserve their recovery cost from the router's stderr evidence instead.
  stderr_failures=$(grep -c 'error=apply_patch verification failed:' "$stderr_file" || true)
  printf '%s\n' $((event_failures + stderr_failures))
}

mcp_first_mutation() {
  jq '
    ([.[]
      | select((.type == "mcp_tool_call"
                and .server == "clj-surgeon"
                and (.tool == "apply_clojure_changes"
                     or .tool == "edit_clojure"
                     or .tool == "transform_clojure"))
               or .type == "file_change"
               or (.type == "command_execution"
                   and (((.command // "") | test("(^|[ /])apply_patch( |$)"))
                        or (((.command // "") | contains("clj-surgeon"))
                            and ((.command // "")
                                 | test(":op[[:space:]]+(:)?(change!|replace-subform!|mv|mv-with-deps|extract!|rename-ns!|fix-declares!|edit)([^a-zA-Z!-]|$)"))))))]
     | first // {}) as $first
    | ($first.type == "mcp_tool_call"
       and $first.server == "clj-surgeon"
       and ($first.tool == "apply_clojure_changes"
            or $first.tool == "edit_clojure"
            or $first.tool == "transform_clojure"))' "$1"
}

computed_route_adherent() {
  local context=$1 inspect_calls=$2 edit_calls=$3 transform_calls=$4
  local apply_calls=$5 file_changes=$6 source_commands=$7 text_reader=$8
  case "$context" in
    native-computed-hint-no-skill)
      [ "$file_changes" -eq 1 ] && [ "$edit_calls" -eq 0 ] \
        && [ "$transform_calls" -eq 0 ] && [ "$apply_calls" -eq 0 ] \
        && [ "$inspect_calls" -eq 0 ] && [ "$source_commands" -eq 1 ] \
        && [ "$text_reader" = true ]
      ;;
    edit-computed-hint-no-skill)
      [ "$edit_calls" -eq 1 ] && [ "$transform_calls" -eq 0 ] \
        && [ "$apply_calls" -eq 0 ] && [ "$file_changes" -eq 0 ] \
        && [ "$inspect_calls" -eq 1 ] && [ "$source_commands" -eq 0 ]
      ;;
    mcp-transform-hint-no-skill)
      [ "$transform_calls" -eq 1 ] && [ "$edit_calls" -eq 0 ] \
        && [ "$apply_calls" -eq 0 ] && [ "$file_changes" -eq 0 ] \
        && [ "$inspect_calls" -eq 0 ] && [ "$source_commands" -eq 0 ]
      ;;
    *) return 0 ;;
  esac
}

make_native_bin() {
  local destination=$1
  local source_path=$2
  local path_dir executable name
  mkdir -p "$destination"
  while IFS= read -r path_dir; do
    [ -d "$path_dir" ] || continue
    for executable in "$path_dir"/*; do
      [ -x "$executable" ] || continue
      [ -d "$executable" ] && continue
      name=${executable##*/}
      [ "$name" = clj-surgeon ] && continue
      [ -e "$destination/$name" ] || ln -s "$executable" "$destination/$name"
    done
  done < <(printf '%s' "$source_path" | tr ':' '\n')
}

counterbalanced_versions() {
  local replicate=$1
  local configured_versions=$2
  if [ "$configured_versions" = "pre post" ] && ((replicate % 2 == 0)); then
    printf '%s\n' "post pre"
  elif [ "$configured_versions" = "pre post native" ] && ((replicate % 2 == 0)); then
    printf '%s\n' "post pre native"
  else
    printf '%s\n' "$configured_versions"
  fi
}

validate_run_matrix() {
  local matrix=$1 cell version context
  [ -n "$matrix" ] || return 0
  for cell in $matrix; do
    version=${cell%%:*}
    context=${cell#*:}
    if [ "$version" = "$cell" ] || [ -z "$version" ] || [ -z "$context" ]; then
      echo "Invalid BENCH_RUN_MATRIX cell; expected VERSION:CONTEXT: $cell" >&2
      return 2
    fi
    case "$version" in pre|post|native|mcp) ;;
      *) echo "Unknown BENCH_RUN_MATRIX version: $version" >&2; return 2 ;;
    esac
    case "$context" in
      no-skill|matched-skill|compact-skill|compact-v2-skill|pipeline-skill|explicit-no-skill|choice-no-skill|aware-no-skill|partition-hint-no-skill|native-hint-no-skill|native-read-hint-no-skill|mcp-hint-no-skill|mcp-extraction-hint-no-skill|native-computed-hint-no-skill|edit-computed-hint-no-skill|mcp-transform-hint-no-skill|mcp-rule-no-skill|mcp-exploratory-rule-no-skill) ;;
      *) echo "Unknown BENCH_RUN_MATRIX context: $context" >&2; return 2 ;;
    esac
    if [ "$version" = native ] \
      && [ "$context" != no-skill ] \
      && [ "$context" != native-hint-no-skill ] \
      && [ "$context" != native-read-hint-no-skill ]; then
      echo "Native matrix cells require a native no-skill context: $cell" >&2
      return 2
    fi
    if [ "$version" = mcp ] \
      && [ "$context" != no-skill ] \
      && [ "$context" != matched-skill ] \
      && [ "$context" != mcp-hint-no-skill ] \
      && [ "$context" != mcp-extraction-hint-no-skill ] \
      && [ "$context" != native-computed-hint-no-skill ] \
      && [ "$context" != edit-computed-hint-no-skill ] \
      && [ "$context" != mcp-transform-hint-no-skill ] \
      && [ "$context" != mcp-rule-no-skill ] \
      && [ "$context" != mcp-exploratory-rule-no-skill ]; then
      echo "MCP matrix cell has an unsupported context: $cell" >&2
      return 2
    fi
  done
}

if [ "${BENCH_OWNER_PROBE_SELF_TEST:-false}" = true ]; then
  acquire_result_owner
  release_result_owner
  printf '%s\n' "benchmark owner probe acquired and released"
  exit 0
fi

if [ "${BENCH_SCHEDULE_SELF_TEST:-false}" = true ]; then
  test "$(counterbalanced_versions 1 'pre post')" = "pre post"
  test "$(counterbalanced_versions 2 'pre post')" = "post pre"
  test "$(counterbalanced_versions 2 'pre post native')" = "post pre native"
  test "$(counterbalanced_versions 3 'post')" = "post"
  validate_run_matrix 'pre:matched-skill post:matched-skill native:no-skill'
  validate_run_matrix 'mcp:no-skill'
  validate_run_matrix 'mcp:matched-skill'
  validate_run_matrix 'mcp:mcp-rule-no-skill'
  validate_run_matrix 'mcp:mcp-exploratory-rule-no-skill'
  validate_run_matrix 'mcp:native-computed-hint-no-skill'
  validate_run_matrix 'mcp:edit-computed-hint-no-skill'
  validate_run_matrix 'mcp:mcp-transform-hint-no-skill'
  validate_run_matrix 'mcp:mcp-extraction-hint-no-skill'
  validate_run_matrix 'native:native-hint-no-skill'
  validate_run_matrix 'native:native-read-hint-no-skill'
  computed_route_adherent native-computed-hint-no-skill 0 0 0 0 1 1 true
  if computed_route_adherent native-computed-hint-no-skill 1 0 0 0 1 1 true; then
    echo "native route self-test accepted an MCP read" >&2
    exit 1
  fi
  computed_route_adherent edit-computed-hint-no-skill 1 1 0 0 0 0 false
  if computed_route_adherent edit-computed-hint-no-skill 1 1 1 0 0 0 false; then
    echo "edit route self-test accepted transform fallback" >&2
    exit 1
  fi
  computed_route_adherent mcp-transform-hint-no-skill 0 0 1 0 0 0 false
  if computed_route_adherent mcp-transform-hint-no-skill 1 0 1 0 0 0 false; then
    echo "transform route self-test accepted a pre-read" >&2
    exit 1
  fi
  if validate_run_matrix 'native:matched-skill' 2>/dev/null; then
    echo "benchmark matrix self-test accepted an invalid native context" >&2
    exit 1
  fi
  if validate_run_matrix 'post' 2>/dev/null; then
    echo "benchmark matrix self-test accepted a malformed cell" >&2
    exit 1
  fi
  printf '%s\n' "benchmark schedule self-test passed"
  exit 0
fi

if [ "${BENCH_HARNESS_SELF_TEST:-false}" = true ]; then
  self_test_root=$(cd "$(mktemp -d /tmp/clj-surgeon-benchmark-self-test.XXXXXX)" && pwd -P)
  original_result_dir=$result_dir
  result_dir="$self_test_root/results"
  owner_dir="$result_dir/.benchmark-owner"
  owner_metadata="$owner_dir/owner.tsv"

  acquire_result_owner
  set +e
  second_output=$(BENCH_RESULT_DIR="$result_dir" BENCH_OWNER_PROBE_SELF_TEST=true bash "$script_path" 2>&1)
  second_status=$?
  set -e
  test "$second_status" -eq 3
  [[ "$second_output" == *"Refusing a concurrent writer"* ]]
  test ! -e "$result_dir/runs.tsv"
  release_result_owner

  mkdir "$owner_dir"
  printf '%s\t%s\n' pid 999999 host "$(hostname)" process_start stale started_utc 1970-01-01T00:00:00Z command stale-fixture \
    > "$owner_metadata"
  set +e
  stale_output=$(BENCH_RESULT_DIR="$result_dir" BENCH_OWNER_PROBE_SELF_TEST=true bash "$script_path" 2>&1)
  stale_status=$?
  set -e
  test "$stale_status" -eq 4
  [[ "$stale_output" == *"BENCH_RECOVER_STALE_OWNER=true"* ]]
  recovery_output=$(BENCH_RESULT_DIR="$result_dir" BENCH_RECOVER_STALE_OWNER=true \
    BENCH_OWNER_PROBE_SELF_TEST=true bash "$script_path" 2>&1)
  [[ "$recovery_output" == *"Recovered stale benchmark ownership metadata"* ]]
  test -n "$(find "$result_dir" -maxdepth 1 -type d -name '.benchmark-owner.recovered-*' -print -quit)"

  mkdir "$result_dir/.runs-lock"
  printf '%s\t%s\n' pid 999999 run_id killed-row-writer > "$result_dir/.runs-lock/owner.tsv"
  set +e
  BENCH_ROW_LOCK_TIMEOUT_SECONDS=1 acquire_row_lock blocked-row 2> "$self_test_root/row-lock.stderr"
  row_status=$?
  set -e
  test "$row_status" -eq 5
  grep -q "Timed out after 1s" "$self_test_root/row-lock.stderr"
  release_row_lock

  printf '%b\n' 'run_id\tvalue' > "$result_dir/runs.tsv"
  append_result_row complete-row $'complete-row\tfirst'
  append_result_row complete-row $'complete-row\tsecond'
  test "$(awk -F '\t' '$1 == "complete-row" {n++} END {print n+0}' "$result_dir/runs.tsv")" -eq 1
  test "$(awk -F '\t' '$1 == "complete-row" {print $2}' "$result_dir/runs.tsv")" = first

  printf '%s\n' \
    '{"type":"turn.started"}' \
    '{"type":"item.started","item":{"type":"command_execution"}}' \
    '{"type":"item.started","item":{"type":"reasoning"}}' \
    '{"type":"item.started","item":{"type":"file_change"}}' \
    '{"type":"turn.completed"}' \
    '{"type":"item.started","item":{"type":"mcp_tool_call"}}' \
    '{"type":"item.completed","item":{"type":"mcp_tool_call","server":"clj-surgeon","tool":"apply_clojure_changes","status":"failed","result":{"structured_content":{"ok":false,"source_unchanged":true}}}}' \
    '{"type":"item.completed","item":{"type":"mcp_tool_call","server":"clj-surgeon","tool":"apply_clojure_changes","status":"completed","result":{"structured_content":{"ok":true,"committed":true,"verification_complete":true,"next_action":"none"}}}}' \
    '{"type":"item.completed","item":{"type":"mcp_tool_call","server":"clj-surgeon","tool":"edit_clojure","status":"completed","result":{"structured_content":{"ok":true,"committed":true,"verification_complete":true,"next_action":"none"}}}}' \
    '{"type":"item.completed","item":{"type":"mcp_tool_call","server":"clj-surgeon","tool":"transform_clojure","status":"completed","result":{"structured_content":{"ok":true,"committed":true,"verification_complete":true,"next_action":"none"}}}}' \
    '{"type":"item.completed","item":{"type":"mcp_tool_call","server":"clj-surgeon","tool":"transform_clojure","status":"completed","result":{"structured_content":{"ok":true,"committed":false,"verification_complete":false,"next_action":"commit"}}}}' \
    '{"type":"item.completed","item":{"type":"mcp_tool_call","server":"clj-surgeon","tool":"transform_clojure","status":"failed","result":{"structured_content":{"ok":false,"source_unchanged":true}}}}' \
    '{"type":"turn.completed"}' > "$self_test_root/events.jsonl"
  IFS=$'\t' read -r self_test_user_turns self_test_tool_round_trips \
    self_test_discovery_round_trips self_test_post_decision_round_trips \
    < <(interaction_counts "$self_test_root/events.jsonl")
  test "$self_test_user_turns" -eq 2
  test "$self_test_tool_round_trips" -eq 3
  test "$self_test_discovery_round_trips" -eq 1
  test "$self_test_post_decision_round_trips" -eq 2
  test "$(mcp_apply_success_count "$self_test_root/events.jsonl")" -eq 3
  test "$(mcp_apply_verified "$self_test_root/events.jsonl")" = true
  printf '%s\n' \
    '{"type":"item.completed","item":{"type":"file_change","status":"failed"}}' \
    '{"type":"item.completed","item":{"type":"file_change","status":"completed"}}' \
    > "$self_test_root/native-mutations.jsonl"
  printf '%s\n' \
    '2026-08-25 ERROR error=apply_patch verification failed: missing boundary' \
    > "$self_test_root/native-mutations.stderr"
  test "$(native_mutation_failure_count \
    "$self_test_root/native-mutations.jsonl" "$self_test_root/native-mutations.stderr")" -eq 2
  jq -s '[.[] | select(.type == "item.started") | .item]' \
    "$self_test_root/events.jsonl" > "$self_test_root/started-items.json"
  test "$(mcp_first_mutation "$self_test_root/started-items.json")" = false
  printf '%s\n' \
    '{"type":"item.started","item":{"type":"command_execution","command":"cat /tmp/skills/clj-surgeon/SKILL.md"}}' \
    '{"type":"item.started","item":{"type":"mcp_tool_call","server":"clj-surgeon","tool":"inspect_clojure"}}' \
    '{"type":"item.started","item":{"type":"mcp_tool_call","server":"clj-surgeon","tool":"edit_clojure"}}' \
    '{"type":"item.started","item":{"type":"file_change"}}' \
    > "$self_test_root/mcp-first-mutation.jsonl"
  jq -s '[.[] | .item]' "$self_test_root/mcp-first-mutation.jsonl" \
    > "$self_test_root/mcp-first-mutation-items.json"
  test "$(mcp_first_mutation "$self_test_root/mcp-first-mutation-items.json")" = true
  printf '%s\n' \
    '{"type":"item.started","item":{"type":"mcp_tool_call","server":"clj-surgeon","tool":"transform_clojure"}}' \
    '{"type":"item.started","item":{"type":"file_change"}}' \
    > "$self_test_root/transform-first-mutation.jsonl"
  jq -s '[.[] | .item]' "$self_test_root/transform-first-mutation.jsonl" \
    > "$self_test_root/transform-first-mutation-items.json"
  test "$(mcp_first_mutation "$self_test_root/transform-first-mutation-items.json")" = true

  make_native_bin "$self_test_root/native-bin" "$PATH"
  if PATH="$self_test_root/native-bin" command -v clj-surgeon >/dev/null 2>&1; then
    echo "native self-test unexpectedly exposed clj-surgeon" >&2
    exit 1
  fi
  PATH="$self_test_root/native-bin" command -v sh >/dev/null

  self_test_workspace="$self_test_root/workspace"
  mkdir -p "$self_test_workspace"
  printf '%s\n' 'starting bytes' > "$self_test_workspace/fixture.txt"
  bb "$repo_root/bench/initialize_benchmark_workspace.clj" \
    "$self_test_workspace" >/dev/null
  test "$(git -C "$self_test_workspace" rev-list --count HEAD)" -eq 1
  test -z "$(git -C "$self_test_workspace" status --short)"
  bb "$repo_root/bench/score_source_fidelity.clj" --self-test >/dev/null

  result_dir=$original_result_dir
  rm -rf "$self_test_root"
  printf '%s\n' "benchmark harness self-test passed"
  exit 0
fi

pre_commit=${BENCH_PRE_COMMIT:-19a20b0}
post_commit=${BENCH_POST_COMMIT:-80154bc}
model=${BENCH_MODEL:-gpt-5.6-sol}
reasoning=${BENCH_REASONING:-medium}
setup_root=""

cleanup() {
  release_result_owner
  [ -z "$setup_root" ] || rm -rf "$setup_root"
}
trap cleanup EXIT

for command_name in codex bb jq git perl shasum; do
  command -v "$command_name" >/dev/null 2>&1 || {
    echo "Missing required command: $command_name" >&2
    exit 2
  }
done

auth_file=${CODEX_AUTH_FILE:-$HOME/.codex/auth.json}
test -f "$auth_file" || {
  echo "Codex authentication file not found: $auth_file" >&2
  exit 2
}

for source_ref in "$pre_commit" "$post_commit"; do
  if [ "$source_ref" != WORKTREE ]; then
    git -C "$repo_root" cat-file -e "$source_ref^{commit}"
  fi
done

acquire_result_owner
setup_root=$(cd "$(mktemp -d /tmp/clj-surgeon-benchmark-setup.XXXXXX)" && pwd -P)

mkdir -p "$result_dir" "$setup_root/tools/pre" "$setup_root/tools/post" \
  "$setup_root/bin/pre" "$setup_root/bin/post" "$setup_root/templates"

materialize_tool_source() {
  local source_ref=$1 destination=$2
  if [ "$source_ref" = WORKTREE ]; then
    mkdir -p "$destination/src" "$destination/skills" "$destination/bench"
    cp -R "$repo_root/src/." "$destination/src/"
    cp -R "$repo_root/skills/." "$destination/skills/"
    cp -R "$repo_root/bench/fixtures" "$destination/bench/fixtures"
    cp "$repo_root/deps.edn" "$destination/deps.edn"
  else
    git -C "$repo_root" archive "$source_ref" | tar -x -C "$destination"
  fi
}

materialize_tool_source "$pre_commit" "$setup_root/tools/pre"
materialize_tool_source "$post_commit" "$setup_root/tools/post"

make_wrapper() {
  local source_root=$1
  local destination=$2
  {
    printf '%s\n' '#!/usr/bin/env bb'
    printf '%s\n' '(require (quote [babashka.classpath :as cp]))'
    printf '(cp/add-classpath "%s/src")\n' "$source_root"
    printf '%s\n' '(require (quote [clj-surgeon.core :as core]))'
    printf '%s\n' '(apply core/-main *command-line-args*)'
  } > "$destination"
  chmod +x "$destination"
}

make_wrapper "$setup_root/tools/pre" "$setup_root/bin/pre/clj-surgeon"
make_wrapper "$setup_root/tools/post" "$setup_root/bin/post/clj-surgeon"
make_native_bin "$setup_root/bin/native" "$PATH"
make_native_bin "$setup_root/bin/mcp" "$PATH"

# Prewarm both versions outside the measured runs.
PATH="$setup_root/bin/pre:$PATH" clj-surgeon --help >/dev/null
PATH="$setup_root/bin/post:$PATH" clj-surgeon --help >/dev/null

cp "$setup_root/tools/pre/src/clj_surgeon/core.clj" \
  "$setup_root/templates/core.clj"
cp "$setup_root/tools/post/src/clj_surgeon/core.clj" \
  "$setup_root/templates/ops_registry.clj"
cp "$repo_root/bench/fixtures/bench/pair_view.clj" \
  "$setup_root/templates/pair_view.clj"

mkdir -p "$setup_root/expected"
PATH="$setup_root/bin/post:$PATH" clj-surgeon :op :show-form \
  :file "$setup_root/templates/core.clj" :form format-op-help \
  > "$setup_root/expected/show-form.edn"
bb -e "(require '[clojure.edn :as edn]) (print (:source (edn/read-string (slurp \"$setup_root/expected/show-form.edn\"))))" \
  > "$setup_root/expected/format-op-help.clj"

make_structural_fixture() {
  local destination=$1
  {
    printf '%s\n\n' '(ns bench.structural)'
    printf '%s\n' '(def decoy "(select-keys state [:status :attempts])")'
    printf '%s\n\n' ';; (select-keys state [:status :attempts]) is only documentation here.'
    local i
    for i in $(seq 1 105); do
      printf '(defn filler-before-%03d [x]\n  (+ x %d))\n\n' "$i" "$i"
    done
    printf '%s\n\n' '(defn summarize-state [state]' '  (select-keys state [:status :attempts]))'
    printf '%s\n\n' '(defn summarize-job [job]' '  (select-keys' '   job' '   [:status :attempts]))'
    for i in $(seq 106 210); do
      printf '(defn filler-after-%03d [x]\n  (- x %d))\n\n' "$i" "$i"
    done
  } > "$destination"
}

make_edit_fixture() {
  local destination=$1
  {
    printf '%s\n\n' '(ns bench.state)'
    local i
    for i in $(seq 1 105); do
      printf '(defn filler-before-%03d [x]\n  (+ x %d))\n\n' "$i" "$i"
    done
    printf '%s\n' '(defn transition' '  [state event]' '  (case event' '    :start (assoc state :status :running)' '    :finish (assoc state :status :done)' '    state))' ''
    printf '%s\n\n' '(defn unrelated-finish [state]' '  (assoc state :status :done))'
    for i in $(seq 106 210); do
      printf '(defn filler-after-%03d [x]\n  (- x %d))\n\n' "$i" "$i"
    done
  } > "$destination"
}

make_computed_edit_fixture() {
  local destination=$1
  {
    printf '%s\n\n' '(ns bench.policy)'
    local i
    for i in $(seq 1 105); do
      printf '(defn filler-before-%03d [x]\n  (+ x %d))\n\n' "$i" "$i"
    done
    printf '%s\n' \
      '(def retry-policy' \
      '  {:retry-delays [100 250 500 1000]' \
      '   :max-attempts 4})' \
      '' \
      '(def unrelated-policy' \
      '  {:retry-delays [100 250 500 1000]' \
      '   :max-attempts 4})' \
      ''
    for i in $(seq 106 210); do
      printf '(defn filler-after-%03d [x]\n  (- x %d))\n\n' "$i" "$i"
    done
  } > "$destination"
}

make_computed_repeated_edit_fixture() {
  local destination=$1
  {
    printf '%s\n\n' '(ns bench.repeated-policy)'
    local i
    for i in $(seq 1 "$computed_site_count"); do
      printf '(def retry-policy-%03d\n  {:retry-delays [100 250 500 1000]\n   :max-attempts 4})\n\n' "$i"
    done
    printf '%s\n' \
      '(def unrelated-policy' \
      '  {:backoff-delays [100 250 500 1000]' \
      '   :max-attempts 4})' \
      ''
  } > "$destination"
}

make_peer_edit_fixture() {
  local destination=$1
  cp "$setup_root/templates/pair_view.clj" "$destination"
  printf '%s\n' \
    '' \
    '(defn unrelated-public [resource]' \
    '  {:decision :allow :reason :public})' \
    '' \
    '(defn unrelated-timeout [request defaults]' \
    '  (or (:timeout-ms request) (:timeout-ms defaults)))' \
    >> "$destination"
}

make_xray_fixture() {
  local destination=$1
  {
    printf '%s\n\n' '(ns bench.xray)'
    local i category points
    for i in $(seq 1 105); do
      printf '(defn filler-before-%03d [x]\n  (+ x %d))\n\n' "$i" "$i"
    done
    printf '%s\n' '(def audit-report' '  {:events ['
    for i in $(seq 1 60); do
      case $((i % 3)) in
        0) category=deny ;;
        1) category=allow ;;
        2) category=review ;;
      esac
      points=$(((i * 7 % 11) + 1))
      printf '            {:category :%s :points %d}\n' "$category" "$points"
    done
    printf '%s\n\n' \
      '            ]' \
      '   :unrelated-event {:category :deny :points 10000}})'
    printf '%s\n' '(def checksum-report' '  {:events ['
    for i in $(seq 1 300); do
      case $((i % 3)) in
        0) category=deny ;;
        1) category=allow ;;
        2) category=review ;;
      esac
      points=$(((i * 7 % 11) + 1))
      printf '            {:category :%s :points %d}\n' "$category" "$points"
    done
    printf '%s\n\n' \
      '            ]' \
      '   :unrelated-event {:category :deny :points 10000}})'
    for i in $(seq 106 210); do
      printf '(defn filler-after-%03d [x]\n  (- x %d))\n\n' "$i" "$i"
    done
  } > "$destination"
}

make_structural_fixture "$setup_root/templates/structural.clj"
make_edit_fixture "$setup_root/templates/state.clj"
make_computed_edit_fixture "$setup_root/templates/policy.clj"
make_computed_repeated_edit_fixture "$setup_root/templates/repeated_policy.clj"
make_peer_edit_fixture "$setup_root/templates/peer_edit.clj"
make_xray_fixture "$setup_root/templates/xray.clj"
PATH="$setup_root/bin/post:$PATH" clj-surgeon :op :find-subform \
  :file "$setup_root/templates/structural.clj" \
  :match '(select-keys _ [:status :attempts])' \
  > "$setup_root/expected/structural.edn"
PATH="$setup_root/bin/post:$PATH" clj-surgeon :op :q \
  :file "$setup_root/templates/pair_view.clj" \
  :query '[[:form route-event] [:find case] :up :down :right :right [:partition-all 2]]' \
  > "$setup_root/expected/case-inventory.edn"
PATH="$setup_root/bin/post:$PATH" clj-surgeon :op :q \
  :file "$setup_root/templates/pair_view.clj" \
  :query '[[:form classify-request] [:find (nil? actor)] [:partition-all 2]]' \
  > "$setup_root/expected/cond-inventory.edn"
PATH="$setup_root/bin/post:$PATH" clj-surgeon :op :q \
  :file "$setup_root/templates/pair_view.clj" \
  :query '[[:form prepare-request] [:find let] :up :down :right :down [:partition-all 2]]' \
  > "$setup_root/expected/binding-inventory.edn"
bb -e "(require '[clojure.edn :as edn]) (let [r (edn/read-string (slurp \"$setup_root/expected/structural.edn\"))] (print (get-in r [:matches 0 :source])))" \
  > "$setup_root/expected/structural-1.clj"
bb -e "(require '[clojure.edn :as edn]) (let [r (edn/read-string (slurp \"$setup_root/expected/structural.edn\"))] (print (get-in r [:matches 1 :source])))" \
  > "$setup_root/expected/structural-2.clj"
cp "$setup_root/templates/state.clj" "$setup_root/expected/state.clj"
perl -0pi -e 's/\(assoc state :status :done\)/\(assoc state :status :complete\)/' \
  "$setup_root/expected/state.clj"
cp "$setup_root/templates/pair_view.clj" "$setup_root/expected/pair-view-edit.clj"
perl -0pi -e 's/\(assoc state :status :done :audit/\(assoc state :status :complete :audit/' \
  "$setup_root/expected/pair-view-edit.clj"
cp "$setup_root/templates/policy.clj" "$setup_root/expected/computed-edit.clj"
perl -0pi -e 's/\[100 250 500 1000\]/[200 350 600 1100]/' \
  "$setup_root/expected/computed-edit.clj"
cp "$setup_root/templates/repeated_policy.clj" \
  "$setup_root/expected/computed-repeated-edit.clj"
perl -0pi -e 's/(:retry-delays )\[100 250 500 1000\]/${1}[200 350 600 1100]/g' \
  "$setup_root/expected/computed-repeated-edit.clj"
cp "$setup_root/templates/peer_edit.clj" "$setup_root/expected/cond-edit.clj"
perl -0pi -e 's/\{:decision :allow :reason :public\}/\{:decision :allow :reason :public-resource\}/' \
  "$setup_root/expected/cond-edit.clj"
cp "$setup_root/templates/peer_edit.clj" "$setup_root/expected/binding-edit.clj"
perl -0pi -e 's/\(or \(:timeout-ms request\) \(:timeout-ms defaults\)\)/\(or \(:timeout-ms request\) 5000\)/' \
  "$setup_root/expected/binding-edit.clj"

if [ "${BENCH_RESUME:-false}" != true ] && [ -f "$result_dir/runs.tsv" ]; then
  echo "Refusing to replace existing benchmark rows: $result_dir/runs.tsv" >&2
  echo "Choose a new BENCH_RESULT_DIR or set BENCH_RESUME=true." >&2
  exit 2
fi
if [ ! -f "$result_dir/runs.tsv" ]; then
  printf '%b\n' \
    'run_id\tversion\tcontext\ttask\torder\tstart_sha\tfinal_sha\twall_ms\texit_code\tinput_tokens\tcached_input_tokens\tuncached_input_tokens\toutput_tokens\treasoning_output_tokens\tshell_calls\tfile_changes\tatomic_commands\tclj_invocations\tsource_commands\tsource_output_bytes\ttotal_tool_output_bytes\tskill_read\tshow_form\tgrep_form\tls_used\thelp_used\ttext_reader\tq_used\txray_used\tpartition_all_used\tedit_used\texpr_used\tfirst_source_edit\tplan_generated\tplan_applied\tplan_apply_separate\tverified\texact_correct\tcorrect\texpect_used\texpect_route\tdecision_supplied\tpost_decision_source_commands\tchange_used\tchange_apply_used\tchange_apply_successes\tfailed_mutation_actions\ttemp_manifest_patch\tsingle_change_transaction\tmcp_calls\tmcp_successes\tmcp_failures\tmcp_tool_output_bytes\tmcp_first_mutation\tuser_turns\ttool_round_trips\tdiscovery_round_trips\tpost_decision_round_trips' \
    > "$result_dir/runs.tsv"
fi

# The portfolio is the frozen task authority, independent of the historical
# tool snapshots under comparison. Older tool commits may predate the fixtures.
portfolio_fixture_root="$repo_root/bench/fixtures/edit_portfolio"

portfolio_dir_for_task() {
  local task_dir="$portfolio_fixture_root/$1"
  [ -f "$task_dir/capsule.edn" ] \
    && [ -f "$task_dir/task.txt" ] \
    && [ -d "$task_dir/before" ] \
    && [ -d "$task_dir/after" ] \
    || return 1
  printf '%s' "$1"
}

is_portfolio_task() {
  portfolio_dir_for_task "$1" >/dev/null 2>&1
}

task_prompt() {
  local task=$1
  if is_portfolio_task "$task"; then
    command cat "$portfolio_fixture_root/$(portfolio_dir_for_task "$task")/task.txt"
    return
  fi
  case "$task" in
    named-form)
      printf '%s' 'Return the exact complete source of the top-level form named format-op-help in src/clj_surgeon/core.clj. Do not modify files and do not read the whole file. In the final answer, include the exact source and briefly name the commands used.'
      ;;
    semantic-form)
      printf '%s' 'Find the top-level form in src/clj_surgeon/core.clj whose docstring contains the distinctive phrase “Per-command help”. Return its exact name, complete source, and boundaries. Do not modify files and do not read the whole file. Briefly name the commands used.'
      ;;
    structural-find)
      printf '%s' 'Find every real structural occurrence of (select-keys _ [:status :attempts]) in src/bench/structural.clj. String and comment lookalikes are not matches. Return the exact match count, source, and locations. Do not modify files and do not read the whole file. Briefly name the commands used.'
      ;;
    case-edit)
      printf '%s' 'In src/bench/state.clj, change only the :finish case result from (assoc state :status :done) to (assoc state :status :complete). Preserve every unrelated byte, including the similar expression in unrelated-finish. A temporary plan artifact is allowed. Verify the exact change, do not read the whole file, and briefly name the commands used.'
      ;;
    pair-view-edit)
      printf '%s' 'Load and follow the installed clj-surgeon skill. In src/bench/pair_view.clj, change only the :finish result inside route-event so its :status value is :complete instead of :done. Preserve its attached comment and every unrelated byte. Generate and review an :edit plan at plan.edn, then apply that plan in a separate shell command with :replace-subform!. Do not combine planning and application. Do not read the whole file. In the final answer, state whether the plan and apply both succeeded.'
      ;;
    pair-view-expect-edit)
      printf '%s' 'Load and follow the installed clj-surgeon skill. In src/bench/pair_view.clj, change only the :finish result inside route-event so its :status value is :complete instead of :done. Preserve its attached comment and every unrelated byte. Complete the change with a single guarded edit call that declares the expected before-state so the tool itself refuses if your declaration is wrong; do not use a separate apply command. If the guard refuses, recover using the refusal'"'"'s evidence and finish with a corrected guarded call. Do not read the whole file. In the final answer, state whether your first declaration matched.'
      ;;
    computed-edit)
      printf '%s' 'In src/bench/policy.clj, add 100 to every number in the :retry-delays vector inside retry-policy. Preserve every unrelated byte, including the identical vector in unrelated-policy. A temporary plan artifact is allowed. Verify the exact change, do not read the whole file, and briefly name the commands used.'
      ;;
    computed-supplied-edit)
      printf '%s' 'In src/bench/policy.clj, change only the :retry-delays vector inside retry-policy from [100 250 500 1000] to the value computed by adding 100 to every number: [200 350 600 1100]. Preserve every unrelated byte, including the identical vector in unrelated-policy. The owner, before-state, relationship, and expected one match are supplied; do not read source first. Complete the mutation once and treat its successful guarded result as terminal proof.'
      ;;
    computed-repeated-edit)
      printf 'In src/bench/repeated_policy.clj, add 100 to every number in all %s values paired with :retry-delays. Preserve every unrelated byte, including the identical vector paired with :backoff-delays. The relationship and exact match count are supplied; complete the mutation once and treat its successful guarded result as terminal proof.' "$computed_site_count"
      ;;
    cond-edit)
      printf '%s' 'In src/bench/peer_edit.clj, change only the outer cond result paired with (:public? resource) inside classify-request from {:decision :allow :reason :public} to {:decision :allow :reason :public-resource}. Preserve every unrelated byte, including the identical map in unrelated-public and the nested cond. A temporary plan artifact is allowed. Verify the exact change, do not read the whole file, and briefly name the commands used.'
      ;;
    binding-edit)
      printf '%s' 'In src/bench/peer_edit.clj, change only the timeout-ms initializer in the let binding vector inside prepare-request from (or (:timeout-ms request) (:timeout-ms defaults)) to (or (:timeout-ms request) 5000). Preserve every unrelated byte, including the identical expression in unrelated-timeout and later binding uses. A temporary plan artifact is allowed. Verify the exact change, do not read the whole file, and briefly name the commands used.'
      ;;
    case-inventory)
      printf '%s' 'In src/bench/pair_view.clj, return every test/result pair in the case expression inside route-event, in source order, plus its optional default. Do not modify files or read the whole file. Your final answer must be exactly one EDN map with this shape and no prose or code fence: {:pairs [{:left-source "exact source" :right-source "exact source"} ...] :tail-source "exact source or nil" :commands ["command summaries"]}.'
      ;;
    cond-inventory)
      printf '%s' 'In src/bench/pair_view.clj, return every outer guard/result pair beginning with (nil? actor) in the cond expression inside classify-request, in source order. Keep the nested cond as one outer result; do not count its internal branches. Do not modify files or read the whole file. Your final answer must be exactly one EDN map with this shape and no prose or code fence: {:pairs [{:left-source "exact source" :right-source "exact source"} ...] :tail-source nil :commands ["command summaries"]}.'
      ;;
    binding-inventory)
      printf '%s' 'In src/bench/pair_view.clj, return every top-level binding name/initializer pair in the let binding vector inside prepare-request, in source order. Exclude later symbol uses and the returned map. Do not modify files or read the whole file. Your final answer must be exactly one EDN map with this shape and no prose or code fence: {:pairs [{:left-source "exact source" :right-source "exact source"} ...] :tail-source nil :commands ["command summaries"]}.'
      ;;
    xray-summary)
      printf '%s' 'In src/bench/xray.clj, sum :points by :category for the values in the :events vector inside audit-report. Ignore :unrelated-event. Do not modify files or read the whole file. Your final answer must be exactly one EDN map from category keywords to integer totals, with no prose or code fence.'
      ;;
    xray-checksum)
      printf '%s' 'In src/bench/xray.clj, compute this checksum over the :events vector inside checksum-report. In source order with indexes starting at 1, multiply index * :points * category weight, where :deny is 3, :allow is 5, and :review is 7. Sum those products and take modulo 1000003. Ignore :unrelated-event. Do not modify files or read the whole file. Your final answer must be exactly one integer with no prose or code fence.'
      ;;
    ops-registry-xray)
      printf '%s' 'In src/bench/ops_registry.clj, analyze the hash-map inside ops-registry. Return category frequencies, the total number of argument specs whose :required value is true, and the sorted operation keywords whose specs contain :pair. Do not modify files or read the whole file. Your final answer must be exactly one EDN map with keys :category-frequencies, :required-arg-count, and :paired-ops, with no prose or code fence.'
      ;;
    *)
      echo "Unknown task: $task" >&2
      exit 2
      ;;
  esac
}

target_for_task() {
  if is_portfolio_task "$1"; then
    bb "$repo_root/bench/verify_edit_portfolio.clj" --targets \
      "$portfolio_fixture_root/$(portfolio_dir_for_task "$1")" | head -n 1
    return
  fi
  case "$1" in
    named-form|semantic-form) printf '%s' 'src/clj_surgeon/core.clj' ;;
    structural-find) printf '%s' 'src/bench/structural.clj' ;;
    case-edit) printf '%s' 'src/bench/state.clj' ;;
    computed-edit|computed-supplied-edit) printf '%s' 'src/bench/policy.clj' ;;
    computed-repeated-edit) printf '%s' 'src/bench/repeated_policy.clj' ;;
    cond-edit|binding-edit) printf '%s' 'src/bench/peer_edit.clj' ;;
    case-inventory|cond-inventory|binding-inventory|pair-view-edit|pair-view-expect-edit|exact-nested-edit) printf '%s' 'src/bench/pair_view.clj' ;;
    xray-summary|xray-checksum) printf '%s' 'src/bench/xray.clj' ;;
    ops-registry-xray) printf '%s' 'src/bench/ops_registry.clj' ;;
  esac
}

target_scope_for_task() {
  if is_portfolio_task "$1"; then
    local first count shared_dir
    first=$(target_for_task "$1")
    count=$(targets_for_task "$1" | awk 'NF {n++} END {print n+0}')
    if [ "$count" -eq 1 ]; then
      printf '%s' "$first"
    else
      shared_dir=$(basename "$(dirname "$first")")
      printf '%s/' "$shared_dir"
    fi
    return
  fi
  case "$1" in
    *) target_for_task "$1" ;;
  esac
}

targets_for_task() {
  if is_portfolio_task "$1"; then
    bb "$repo_root/bench/verify_edit_portfolio.clj" --targets \
      "$portfolio_fixture_root/$(portfolio_dir_for_task "$1")"
    return
  fi
  target_for_task "$1"
  printf '\n'
}

hash_task_targets() {
  local task=$1 workspace=$2 target count
  count=$(targets_for_task "$task" | awk 'NF {n++} END {print n+0}')
  if [ "$count" -gt 1 ]; then
    # Hash the ordered per-file manifest so a partial multi-file result cannot
    # share a task hash with any complete result.
    while IFS= read -r target; do
      if [ -n "$target" ]; then
        if [ -f "$workspace/$target" ]; then
          shasum -a 256 "$workspace/$target"
        else
          printf 'ABSENT  %s\n' "$target"
        fi
      fi
    done < <(targets_for_task "$task") | shasum -a 256 | awk '{print $1}'
  else
    target=$(target_for_task "$task")
    if [ -f "$workspace/$target" ]; then
      shasum -a 256 "$workspace/$target" | awk '{print $1}'
    else
      printf 'ABSENT  %s\n' "$target" | shasum -a 256 | awk '{print $1}'
    fi
  fi
}

prepare_workspace() {
  local task=$1
  local workspace=$2
  mkdir -p "$workspace/src/clj_surgeon" "$workspace/src/bench"
  if is_portfolio_task "$task"; then
    cp -R "$portfolio_fixture_root/$(portfolio_dir_for_task "$task")/before/." "$workspace/"
    return
  fi
  case "$task" in
    named-form|semantic-form)
      cp "$setup_root/templates/core.clj" "$workspace/src/clj_surgeon/core.clj"
      ;;
    structural-find)
      cp "$setup_root/templates/structural.clj" "$workspace/src/bench/structural.clj"
      ;;
    case-edit)
      cp "$setup_root/templates/state.clj" "$workspace/src/bench/state.clj"
      ;;
    computed-edit|computed-supplied-edit)
      cp "$setup_root/templates/policy.clj" "$workspace/src/bench/policy.clj"
      ;;
    computed-repeated-edit)
      cp "$setup_root/templates/repeated_policy.clj" \
        "$workspace/src/bench/repeated_policy.clj"
      ;;
    cond-edit|binding-edit)
      cp "$setup_root/templates/peer_edit.clj" "$workspace/src/bench/peer_edit.clj"
      ;;
    case-inventory|cond-inventory|binding-inventory|pair-view-edit|pair-view-expect-edit)
      cp "$setup_root/templates/pair_view.clj" "$workspace/src/bench/pair_view.clj"
      ;;
    xray-summary|xray-checksum)
      cp "$setup_root/templates/xray.clj" "$workspace/src/bench/xray.clj"
      ;;
    ops-registry-xray)
      cp "$setup_root/templates/ops_registry.clj" "$workspace/src/bench/ops_registry.clj"
      ;;
  esac
}

install_treatment_skill() {
  local version=$1
  local context=$2
  local codex_home=$3
  case "$context" in
    matched-skill)
      local skill_version=$version
      if [ "$version" = mcp ]; then
        skill_version=post
      fi
      mkdir -p "$codex_home/skills/clj-surgeon"
      cp "$setup_root/tools/$skill_version/skills/clj-surgeon/SKILL.md" \
        "$codex_home/skills/clj-surgeon/SKILL.md"
      ;;
    compact-skill)
      mkdir -p "$codex_home/skills/clj-surgeon"
      cp "$repo_root/bench/compact-clj-surgeon-skill/SKILL.md" \
        "$codex_home/skills/clj-surgeon/SKILL.md"
      ;;
    compact-v2-skill)
      mkdir -p "$codex_home/skills/clj-surgeon"
      cp "$repo_root/bench/compact-v2-clj-surgeon-skill/SKILL.md" \
        "$codex_home/skills/clj-surgeon/SKILL.md"
      ;;
    pipeline-skill)
      mkdir -p "$codex_home/skills/clj-surgeon-q-bb"
      cp "$repo_root/bench/q-bb-skill/SKILL.md" \
        "$codex_home/skills/clj-surgeon-q-bb/SKILL.md"
      ;;
    no-skill|explicit-no-skill|choice-no-skill|aware-no-skill|partition-hint-no-skill|native-hint-no-skill|native-read-hint-no-skill|mcp-hint-no-skill|mcp-extraction-hint-no-skill|native-computed-hint-no-skill|edit-computed-hint-no-skill|mcp-transform-hint-no-skill|mcp-rule-no-skill|mcp-exploratory-rule-no-skill) ;;
    *)
      echo "Unknown context: $context" >&2
      exit 2
      ;;
  esac
}

bool_from_jq() {
  local expression=$1
  local file=$2
  jq -s -r "if ($expression) then \"true\" else \"false\" end" "$file"
}

run_one() {
  local version=$1
  local context=$2
  local task=$3
  local order=$4
  local replicate=${5:-1}
  local run_id
  run_id=$(printf '%02d-r%02d-%s-%s-%s' "$order" "$replicate" "$task" "$context" "$version")
  if [ "$version" = native ] \
    && [ "$context" != no-skill ] \
    && [ "$context" != native-hint-no-skill ] \
    && [ "$context" != native-read-hint-no-skill ]; then
    echo "The native version requires a native no-skill context: $run_id" >&2
    exit 2
  fi
  if [ "$version" = mcp ] \
    && [ "$context" != no-skill ] \
    && [ "$context" != matched-skill ] \
    && [ "$context" != mcp-hint-no-skill ] \
    && [ "$context" != mcp-extraction-hint-no-skill ] \
    && [ "$context" != native-computed-hint-no-skill ] \
    && [ "$context" != edit-computed-hint-no-skill ] \
    && [ "$context" != mcp-transform-hint-no-skill ] \
    && [ "$context" != mcp-rule-no-skill ] \
    && [ "$context" != mcp-exploratory-rule-no-skill ]; then
    echo "The MCP version has an unsupported context: $run_id" >&2
    exit 2
  fi
  if [ "${BENCH_RESUME:-false}" = true ] \
    && row_exists "$result_dir/runs.tsv" "$run_id"; then
    terminal_state=skipped
    printf '%-58s %s\n' "$run_id" 'already complete; skipping'
    return
  fi
  local run_dir="$result_dir/$run_id"
  local workspace="$setup_root/workspaces/$run_id"
  local codex_home="$setup_root/homes/$run_id"
  local target_rel
  target_rel=$(target_for_task "$task")
  local target_scope
  target_scope=$(target_scope_for_task "$task")
  local target="$workspace/$target_rel"
  local bin_dir="$setup_root/bin/$version"
  local zsh_dir="$codex_home/zsh"

  mkdir -p "$run_dir" "$codex_home" "$zsh_dir"
  ln -s "$auth_file" "$codex_home/auth.json"
  install_treatment_skill "$version" "$context" "$codex_home"
  prepare_workspace "$task" "$workspace"
  if [ "$context" = mcp-rule-no-skill ]; then
    # Markdown backticks are literal project-rule text.
    # shellcheck disable=SC2016
    printf '%s\n' \
      '# Clojure edit routing' \
      '' \
      'Optimize complete task time, not tool adoption. Use native tools for broad discovery and when one literal edit is smaller or clearer. When an already-decided Clojure change spans forms or files, prefer one compact `edit_clojure` transaction with all supplied edits and optional bounded computed programs. Do not preflight-read a supplied decision. Reserve heavyweight `apply_clojure_changes` for prepared semantic decisions, operations absent from compact editing, or gates that must participate in rollback. A successful compact commit with `verification_complete=true` is terminal mutation evidence; do not add a reread merely because Surgeon performed the edit.' \
      > "$workspace/AGENTS.md"
  fi
  if [ "$context" = mcp-exploratory-rule-no-skill ]; then
    # Markdown backticks are literal project-rule text.
    # shellcheck disable=SC2016
    printf '%s\n' \
      '# MCP exploratory routing experiment' \
      '' \
      'For an exploratory Clojure change, batch every currently knowable structural read in one `inspect_clojure` call. When one named Var defines the goal but its surface is unknown, use `mode=prepare-change` with that subject. When the affected owner is unknown in a small file, use a structural match such as `(defn _ _ _)` to return complete candidate forms; an outline alone is not edit evidence. Decide from that bounded snapshot, then call `apply_clojure_changes` once with the complete decision. Preserve `workspace_root`, `basis`, and `next_call` fields exactly when returned. For a direct `changes` request, send only `changes` and `expect`; `verify` belongs only to a retained-basis request. Do not use native source readers or `apply_patch`. Treat `read_complete=true` and `verification_complete=true` as terminal evidence.' \
      > "$workspace/AGENTS.md"
  fi
  bb "$repo_root/bench/initialize_benchmark_workspace.clj" "$workspace" >/dev/null
  if [ "$version" = mcp ]; then
    local ready_file="$run_dir/mcp-ready.edn"
    local server_started_ms server_ready_ms
    local mcp_java_opts=()
    local mcp_profile_args=()
    if [ -n "${BENCH_MCP_JAVA_OPTS:-}" ]; then
      read -r -a mcp_java_opts <<< "$BENCH_MCP_JAVA_OPTS"
      local java_opt
      for java_opt in "${mcp_java_opts[@]}"; do
        case "$java_opt" in
          -J*) ;;
          *)
            echo "BENCH_MCP_JAVA_OPTS accepts only whitespace-separated -J options: $java_opt" >&2
            exit 2
            ;;
        esac
      done
    fi
    case "${BENCH_MCP_TOOL_PROFILE:-full}" in
      full) ;;
      edit) mcp_profile_args=(:tool-profile :edit) ;;
      *)
        echo "BENCH_MCP_TOOL_PROFILE must be full or edit: ${BENCH_MCP_TOOL_PROFILE}" >&2
        exit 2
        ;;
    esac
    server_started_ms=$(perl -MTime::HiRes=time -e 'printf "%.0f\n", time()*1000')
    (
      cd "$repo_root"
      exec clojure "${mcp_java_opts[@]}" -X:clj-surgeon/mcp \
        "${mcp_profile_args[@]}" \
        :project-dir "$(bb -e '(prn (first *command-line-args*))' "$workspace")" \
        :telemetry :full \
        :telemetry-dir "$(bb -e '(prn (first *command-line-args*))' "$run_dir/mcp-telemetry")" \
        :run-id "$(bb -e '(prn (first *command-line-args*))' "$run_id")" \
        :nrepl-port :none \
        :port 0 \
        :ready-file "$(bb -e '(prn (first *command-line-args*))' "$ready_file")"
    ) >"$run_dir/mcp-server.stdout" 2>"$run_dir/mcp-server.stderr" &
    mcp_pid=$!
    local attempt
    for attempt in $(seq 1 240); do
      [ -s "$ready_file" ] && break
      if ! kill -0 "$mcp_pid" 2>/dev/null; then
        echo "Persistent MCP exited before readiness for $run_id" >&2
        cat "$run_dir/mcp-server.stderr" >&2
        exit 2
      fi
      sleep 0.25
    done
    if [ ! -s "$ready_file" ]; then
      echo "Persistent MCP did not become ready for $run_id" >&2
      exit 2
    fi
    local mcp_url
    mcp_url=$(bb -e '(-> *command-line-args* first slurp clojure.edn/read-string :url println)' "$ready_file")
    curl --fail --silent --show-error "${mcp_url%/mcp}/healthz" >/dev/null
    server_ready_ms=$(perl -MTime::HiRes=time -e 'printf "%.0f\n", time()*1000')
    printf '%s\n' "$((server_ready_ms - server_started_ms))" > "$run_dir/mcp-bootstrap-ms.txt"
    bb "$repo_root/bench/write_mcp_config.clj" \
      "$codex_home/config.toml" --url "$mcp_url" >/dev/null
  fi
  local run_path
  run_path="$bin_dir:$PATH"
  if [ "$version" = native ] || [ "$version" = mcp ]; then
    run_path=$bin_dir
    printf 'export PATH="%s"\n' "$bin_dir" > "$zsh_dir/.zprofile"
  else
    printf 'export PATH="%s:$%s"\n' "$bin_dir" PATH > "$zsh_dir/.zprofile"
  fi
  local resolved_cli
  resolved_cli=$(ZDOTDIR="$zsh_dir" /bin/zsh -lc 'command -v clj-surgeon' 2>/dev/null || true)
  if { [ "$version" = native ] || [ "$version" = mcp ]; } && [ -n "$resolved_cli" ]; then
    echo "$version isolation exposed clj-surgeon for $run_id: $resolved_cli" >&2
    exit 2
  elif [ "$version" != native ] && [ "$version" != mcp ] \
    && [ "$resolved_cli" != "$bin_dir/clj-surgeon" ]; then
    echo "Version isolation failed for $run_id: $resolved_cli" >&2
    exit 2
  fi
  if { [ "$version" = native ] \
       || { [ "$version" = mcp ] && [ "$context" != matched-skill ]; }; } \
    && [ -d "$codex_home/skills" ] \
    && [ -n "$(find "$codex_home/skills" -iname '*clj-surgeon*' -print -quit)" ]; then
    echo "$version isolation exposed a clj-surgeon skill for $run_id" >&2
    exit 2
  fi

  task_prompt "$task" > "$run_dir/prompt.txt"
  if [ "$context" = 'explicit-no-skill' ]; then
    printf '%s\n' '' 'Use the installed clj-surgeon as your primary lens for Clojure source. Optimize for the fewest commands and least irrelevant output. Preserve a human review boundary before any write.' \
      >> "$run_dir/prompt.txt"
  fi
  if [ "$context" = 'choice-no-skill' ]; then
    printf '%s\n' '' 'You may use ordinary shell readers and patch editing, or the installed clj-surgeon CLI. Choose whichever route you expect to be fastest, safest, and least wasteful; briefly explain the choice after completing the task.' \
      >> "$run_dir/prompt.txt"
  fi
  if [ "$context" = 'aware-no-skill' ]; then
    printf '%s\n' '' 'The clj-surgeon CLI is installed and available.' \
      >> "$run_dir/prompt.txt"
  fi
  if [ "$context" = 'partition-hint-no-skill' ]; then
    printf '%s\n' '' 'You may use ordinary shell readers and patch editing, or the installed clj-surgeon CLI. For sibling inventories, clj-surgeon has a [:partition-all 2] query step. Choose whichever route you expect to be fastest, safest, and least wasteful; briefly explain the choice after completing the task.' \
      >> "$run_dir/prompt.txt"
  fi
  if [ "$context" = 'mcp-hint-no-skill' ]; then
    printf '%s\n' '' 'Use the available edit_clojure tool once for the complete supplied decision. For supplied exact owner deletion, send workspace_root and one delete_owners array; each group has file and forms. For literal edits, send workspace_root and one edits array; each edit has file, exactly one of within.form or within.namespace, from, to, and matches. Use within.namespace=true only for an ns form; the tool resolves the unique namespace owner in that file, so do not repeat namespace names. Do not add a redundant top-level expect or verify. Do not read source or use apply_patch. Named-owner resolution or each from value plus matches is the stale-source guard. A response with verification_complete=true is terminal proof; do not reread or diff afterward.' \
      >> "$run_dir/prompt.txt"
  fi
  if [ "$context" = 'mcp-extraction-hint-no-skill' ]; then
    printf '%s\n' '' 'Use exactly one inspect_clojure call with mode=plan-extraction, the supplied file, destination, complete forms list, and require_policy=minimal. Do not read source first. Review the complete manifest and its required public_forms. Copy its hash-bound next_call, then call apply_clojure_changes exactly once after filling only genuinely required caller decisions. Do not use edit_clojure, native source readers, or apply_patch. Treat read_complete=true and verification_complete=true as terminal evidence. After the complete mutation, run the requested clj-kondo command once.' \
      >> "$run_dir/prompt.txt"
  fi
  if [ "$context" = 'native-computed-hint-no-skill' ]; then
    printf '%s\n' '' 'Use one bounded native source read, then one apply_patch mutation. Do not call a clj-surgeon MCP tool. A successful apply_patch result is terminal mutation proof; do not reread or diff afterward.' \
      >> "$run_dir/prompt.txt"
  fi
  if [ "$context" = 'edit-computed-hint-no-skill' ]; then
    printf '%s\n' '' 'Use exactly one inspect_clojure call to read the named owner, then exactly one edit_clojure call replacing the exact vector inside retry-policy. Do not call transform_clojure or apply_clojure_changes, and do not use apply_patch. A response with verification_complete=true is terminal proof; do not reread or diff afterward.' \
      >> "$run_dir/prompt.txt"
  fi
  if [ "$context" = 'mcp-transform-hint-no-skill' ]; then
    expected_transform_matches=1
    changed_character_budget=64
    if [ "$task" = computed-repeated-edit ]; then
      expected_transform_matches=$computed_site_count
      changed_character_budget=$((computed_site_count * 32))
    fi
    printf '%s\n' '' "Use exactly one transform_clojure call with commit=true, expect.matches=$expected_transform_matches, and expect.max_changed_characters=$changed_character_budget. Write the bounded SCI expression yourself from the supplied owner and relationship. Do not read source first, call another mutation tool, or use apply_patch. A committed response with verification_complete=true is terminal proof; do not reread or diff afterward." \
      >> "$run_dir/prompt.txt"
  fi
  if [ "$context" = 'native-hint-no-skill' ]; then
    printf '%s\n' '' 'Use the available apply_patch tool once for the complete supplied decision. Send one patch containing all supplied replacements; do not read source first. The patch old lines are the stale-source guards. A successful apply_patch result is terminal proof; do not reread or diff afterward.' \
      >> "$run_dir/prompt.txt"
  fi
  if [ "$context" = 'native-read-hint-no-skill' ]; then
    printf '%s\n' '' 'Use native source inspection and apply_patch for this exact edit. Read only enough of the named owner to construct one precise patch, then apply it once. A successful apply_patch result is terminal mutation proof; do not reread or diff afterward.' \
      >> "$run_dir/prompt.txt"
  fi

  local start_sha
  start_sha=$(hash_task_targets "$task" "$workspace")
  printf '%s\n' "$start_sha" > "$run_dir/start.sha256"

  local start_ms end_ms wall_ms exit_code sandbox
  start_ms=$(perl -MTime::HiRes=time -e 'printf "%.0f\n", time()*1000')
  sandbox=${BENCH_SANDBOX_MODE:-}
  if [ -z "$sandbox" ]; then
    sandbox=read-only
    if is_portfolio_task "$task" || [[ "$task" == *-edit ]]; then
      sandbox='workspace-write'
    fi
  fi
  case "$sandbox" in
    read-only|workspace-write|danger-full-access) ;;
    *)
      echo "BENCH_SANDBOX_MODE must be read-only, workspace-write, or danger-full-access: $sandbox" >&2
      exit 2
      ;;
  esac

  set +e
  local codex_args=(exec --json --ephemeral)
  if [ "$version" != mcp ]; then
    codex_args+=(--ignore-user-config)
  fi
  if [ "$context" != mcp-rule-no-skill ] \
    && [ "$context" != mcp-exploratory-rule-no-skill ]; then
    codex_args+=(--ignore-rules)
  fi
  PATH="$run_path" ZDOTDIR="$zsh_dir" CODEX_HOME="$codex_home" \
    codex "${codex_args[@]}" \
    --skip-git-repo-check --sandbox "$sandbox" --color never \
    -m "$model" -c "model_reasoning_effort=\"$reasoning\"" \
    -C "$workspace" "$(cat "$run_dir/prompt.txt")" \
    > "$run_dir/events.jsonl" 2> "$run_dir/stderr.txt" </dev/null
  exit_code=$?
  set -e

  end_ms=$(perl -MTime::HiRes=time -e 'printf "%.0f\n", time()*1000')
  wall_ms=$((end_ms - start_ms))
  if [ -n "${mcp_pid:-}" ]; then
    kill "$mcp_pid" 2>/dev/null || true
    wait "$mcp_pid" 2>/dev/null || true
    mcp_pid=""
  fi

  jq -s -r '[.[] | select(.type == "item.completed" and .item.type == "agent_message")][-1].item.text // ""' \
    "$run_dir/events.jsonl" > "$run_dir/final.txt"
  jq -s '[.[] | select(.type == "item.completed" and .item.type == "command_execution") | .item]' \
    "$run_dir/events.jsonl" > "$run_dir/commands.json"
  jq -s '[.[] | select(.type == "item.started") | .item]' \
    "$run_dir/events.jsonl" > "$run_dir/started-items.json"
  jq -r '.[] | [.id, .command, (.exit_code // ""), (.aggregated_output | length)] | @tsv' \
    "$run_dir/commands.json" > "$run_dir/commands.tsv"

  local usage input_tokens cached_tokens uncached_tokens output_tokens reasoning_tokens
  local user_turns tool_round_trips discovery_round_trips post_decision_round_trips
  usage=$(jq -s '[.[] | select(.type == "turn.completed")][-1].usage // {}' "$run_dir/events.jsonl")
  input_tokens=$(jq -r '.input_tokens // 0' <<< "$usage")
  cached_tokens=$(jq -r '.cached_input_tokens // 0' <<< "$usage")
  uncached_tokens=$((input_tokens - cached_tokens))
  output_tokens=$(jq -r '.output_tokens // 0' <<< "$usage")
  reasoning_tokens=$(jq -r '.reasoning_output_tokens // 0' <<< "$usage")
  IFS=$'\t' read -r user_turns tool_round_trips discovery_round_trips post_decision_round_trips \
    < <(interaction_counts "$run_dir/events.jsonl")

  local shell_calls file_changes atomic_commands clj_invocations source_commands
  local source_output_bytes total_tool_output_bytes mcp_tool_output_bytes mcp_source_characters
  local mcp_calls mcp_successes mcp_failures mcp_first_mutation mcp_apply_successes
  shell_calls=$(jq 'length' "$run_dir/commands.json")
  file_changes=$(jq -s '[.[] | select(.type == "item.completed" and .item.type == "file_change")] | length' \
    "$run_dir/events.jsonl")
  atomic_commands=$(jq -r '.[].command' "$run_dir/commands.json" \
    | awk 'BEGIN {n=0} {n++; while (sub(/&&/, "")) n++} END {print n+0}')
  clj_invocations=$(jq '[.[] | select(.command | contains("clj-surgeon "))] | length' \
    "$run_dir/commands.json")
  source_commands=$(jq --arg target "$target_scope" \
    '[.[] | select(.command | contains($target))] | length' "$run_dir/commands.json")
  source_output_bytes=$(jq --arg target "$target_scope" \
    '[.[] | select(.command | contains($target)) | (.aggregated_output // "" | utf8bytelength)] | add // 0' \
    "$run_dir/commands.json")
  mcp_calls=$(jq -s '[.[] | select(.type == "item.started"
    and .item.type == "mcp_tool_call"
    and .item.server == "clj-surgeon")] | length' "$run_dir/events.jsonl")
  mcp_successes=$(jq -s '[.[] | select(.type == "item.completed"
    and .item.type == "mcp_tool_call"
    and .item.server == "clj-surgeon"
    and .item.status == "completed"
    and ((.item.result.structured_content.ok // true) == true))] | length' "$run_dir/events.jsonl")
  mcp_failures=$(jq -s '[.[] | select(.type == "item.completed"
    and .item.type == "mcp_tool_call"
    and .item.server == "clj-surgeon")] | length' "$run_dir/events.jsonl")
  mcp_failures=$((mcp_failures - mcp_successes))
  mcp_tool_output_bytes=$(jq -s '[.[] | select(.type == "item.completed"
    and .item.type == "mcp_tool_call"
    and .item.server == "clj-surgeon")
    | .item.result.content[]? | select(.type == "text")
    | (.text // "" | utf8bytelength)] | add // 0' "$run_dir/events.jsonl")
  mcp_source_characters=$(jq -s '[.[] | select(.type == "item.completed"
    and .item.type == "mcp_tool_call"
    and .item.server == "clj-surgeon"
    and .item.tool == "inspect_clojure")
    | (.item.result.structured_content.source_character_count // 0)] | add // 0' \
    "$run_dir/events.jsonl")
  source_output_bytes=$((source_output_bytes + mcp_source_characters))
  mcp_apply_successes=$(mcp_apply_success_count "$run_dir/events.jsonl")
  total_tool_output_bytes=$(jq '[.[] | (.aggregated_output // "" | utf8bytelength)] | add // 0' \
    "$run_dir/commands.json")
  total_tool_output_bytes=$((total_tool_output_bytes + mcp_tool_output_bytes))

  local skill_read show_form grep_form ls_used help_used text_reader q_used xray_used partition_all_used
  local edit_used expr_used first_source_edit
  local decision_supplied post_decision_source_commands
  local change_used change_apply_used change_apply_successes failed_mutation_actions
  local temp_manifest_patch single_change_transaction failed_clj_mutations failed_native_mutations
  local plan_generated plan_applied chained_plan_apply plan_apply_separate verified
  local expect_used expect_route separate_apply_seen
  skill_read=$(jq '[.[] | select(.command | contains("/skills/clj-surgeon/SKILL.md"))] | length > 0' "$run_dir/commands.json")
  show_form=$(jq '[.[] | select((.command | contains("clj-surgeon")) and ((.command | contains("show-form")) or (.command | test(":cat([^a-zA-Z]|$)"))))] | length > 0' "$run_dir/commands.json")
  grep_form=$(jq '[.[] | select((.command | contains("clj-surgeon")) and (.command | contains("grep-form")))] | length > 0' "$run_dir/commands.json")
  ls_used=$(jq '[.[] | select((.command | contains("clj-surgeon")) and (.command | test(":ls([^a-zA-Z]|$)")))] | length > 0' "$run_dir/commands.json")
  help_used=$(jq '[.[] | select(.command | contains("--help"))] | length > 0' "$run_dir/commands.json")
  text_reader=$(jq --arg target "$target_scope" \
    '[.[] | select((.command | contains($target)) and (.command | test("(^|[^A-Za-z0-9_.-])(rg|sed|awk|head|tail|cat)([^A-Za-z0-9_.-]|$)")))] | length > 0' \
    "$run_dir/commands.json")
  q_used=$(jq '[.[] | select((.command | contains("clj-surgeon")) and (.command | test(":op[[:space:]]+(:)?(q|lens)([^a-zA-Z-]|$)")))] | length > 0' "$run_dir/commands.json")
  xray_used=$(jq '[.[] | select((.command | contains("clj-surgeon")) and (.command | test(":op[[:space:]]+(:)?xray([^a-zA-Z-]|$)")))] | length > 0' "$run_dir/commands.json")
  partition_all_used=$(jq '[.[] | select((.command | contains("clj-surgeon")) and (.command | contains("partition-all")))] | length > 0' "$run_dir/commands.json")
  edit_used=$(jq '[.[] | select((.command | contains("clj-surgeon")) and (.command | test(":op[[:space:]]+(:)?edit([^a-zA-Z!-]|$)")))] | length > 0' "$run_dir/commands.json")
  expr_used=$(jq '[.[] | select((.command | contains("clj-surgeon")) and (.command | test(":expr([[:space:]]|$)")))] | length > 0' "$run_dir/commands.json")
  change_used=$(jq '[.[] | select((.command | contains("clj-surgeon")) and (.command | test(":op[[:space:]]+(:)?change!?([^a-zA-Z!-]|$)")))] | length > 0' "$run_dir/commands.json")
  change_apply_successes=$(jq '[.[] | select((.command | contains("clj-surgeon"))
    and (.command | test(":op[[:space:]]+(:)?change!([^a-zA-Z!-]|$)"))
    and ((.exit_code // 1) == 0))] | length' "$run_dir/commands.json")
  change_apply_successes=$((change_apply_successes + mcp_apply_successes))
  if [ "$mcp_calls" -gt 0 ]; then
    change_used=true
  fi
  if [ "$change_apply_successes" -gt 0 ]; then
    change_apply_used=true
  else
    change_apply_used=false
  fi
  failed_clj_mutations=$(jq '[.[] | select((.command | contains("clj-surgeon"))
    and (.command | test(":op[[:space:]]+(:)?(change!|edit|replace-subform!|mv|mv-with-deps|extract!|rename-ns!|fix-declares!)([^a-zA-Z!-]|$)"))
    and ((.exit_code // 0) != 0))] | length' "$run_dir/commands.json")
  failed_native_mutations=$(native_mutation_failure_count \
    "$run_dir/events.jsonl" "$run_dir/stderr.txt")
  failed_mutation_actions=$((failed_clj_mutations + failed_native_mutations + mcp_failures))
  temp_manifest_patch=$(jq -s '[.[] | select(.type == "item.completed" and .item.type == "file_change")
    | .item.changes[]? | (.path // "") | select(endswith(".edn"))] | length > 0' "$run_dir/events.jsonl")
  if is_portfolio_task "$task"; then
    decision_supplied=$(bb "$repo_root/bench/verify_edit_portfolio.clj" \
      --decision-supplied "$portfolio_fixture_root/$(portfolio_dir_for_task "$task")")
  elif [ "$task" = computed-supplied-edit ]; then
    decision_supplied=true
  else
    decision_supplied=false
  fi
  if [ "$decision_supplied" = true ]; then
    post_decision_source_commands=$source_commands
  else
    post_decision_source_commands=0
  fi
  if [ "$change_apply_successes" -eq 1 ] \
    && [ "$failed_mutation_actions" -eq 0 ] \
    && [ "$file_changes" -eq 0 ]; then
    single_change_transaction=true
  else
    single_change_transaction=false
  fi
  first_source_edit=$(jq --arg target "$target_scope" '
    ([.[]
      | select((.type == "command_execution" and (.command | contains($target)))
               or (.type == "file_change"
                   and any(.changes[]?; .path | contains($target))))]
     | first // {}) as $first
    | (($first.type == "command_execution")
       and ($first.command | contains("clj-surgeon"))
       and ($first.command | test(":op[[:space:]]+(:)?edit([^a-zA-Z!-]|$)")))' \
    "$run_dir/started-items.json")
  mcp_first_mutation=$(mcp_first_mutation "$run_dir/started-items.json")
  plan_generated=$(jq '[.[] | select((.exit_code == 0)
    and (.command | contains("clj-surgeon"))
    and (((.command | contains("replace-subform")) and ((.command | contains("replace-subform!")) | not))
         or ((.command | test(":op[[:space:]]+(:)?(q|lens)([^a-zA-Z-]|$)")) and (.command | contains("[:replace")))
         or (.command | test(":op[[:space:]]+(:)?edit([^a-zA-Z!-]|$)")))
    and (.command | contains("--help") | not))] | length > 0' "$run_dir/commands.json")
  plan_applied=$(jq '[.[] | select(((.exit_code == 0)
    or ((.aggregated_output // "") | contains(":verified")))
    and (.command | contains("clj-surgeon"))
    and (.command | contains("replace-subform!"))
    and (.command | contains("--help") | not))] | length > 0' "$run_dir/commands.json")
  chained_plan_apply=$(jq '[.[] | select((.command | contains("replace-subform ")) and (.command | contains("replace-subform!")))] | length > 0' \
    "$run_dir/commands.json")
  if [ "$plan_generated" = true ] && [ "$plan_applied" = true ] && [ "$chained_plan_apply" = false ]; then
    plan_apply_separate=true
  else
    plan_apply_separate=false
  fi

  # The guarded one-call route: an :edit command carrying :expect, and no
  # separate :replace-subform! apply anywhere in the run.
  expect_used=$(jq '[.[] | select((.command | contains("clj-surgeon"))
    and (.command | test(":op[[:space:]]+(:)?edit([^a-zA-Z!-]|$)"))
    and (.command | contains(":expect"))
    and (.command | contains("--help") | not))] | length > 0' "$run_dir/commands.json")
  if [ "$expect_used" = false ]; then
    expect_used=$(jq -s '[.[] | select(.type == "item.started"
      and .item.type == "mcp_tool_call"
      and .item.server == "clj-surgeon"
      and (.item.tool == "edit_clojure"
           or .item.tool == "transform_clojure"))] | length > 0' "$run_dir/events.jsonl")
  fi
  separate_apply_seen=$(jq '[.[] | select((.command | contains("clj-surgeon"))
    and (.command | contains("replace-subform!"))
    and (.command | contains("--help") | not))] | length > 0' "$run_dir/commands.json")
  if [ "$expect_used" = true ] && [ "$separate_apply_seen" = false ]; then
    expect_route=true
  else
    expect_route=false
  fi

  verified=false
  if [ "$mcp_successes" -gt 0 ]; then
    verified=$(mcp_apply_verified "$run_dir/events.jsonl")
  elif [ "$change_apply_successes" -gt 0 ]; then
    verified=$(jq '[.[] | select((.command | contains("clj-surgeon"))
      and (.command | test(":op[[:space:]]+(:)?change!([^a-zA-Z!-]|$)"))
      and ((.exit_code // 1) == 0)
      and ((.aggregated_output // "") | contains(":committed true"))
      and ((.aggregated_output // "") | contains(":whole-files true"))
      and ((.aggregated_output // "") | contains(":read-back-hashes")))] | length > 0' \
      "$run_dir/commands.json")
  elif [ "$task" = pair-view-expect-edit ]; then
    # The guarded call is its own apply receipt: plan evidence and the
    # :replace-subform! verification arrive in one command output.
    verified=$(jq '[.[] | select((.command | contains("clj-surgeon"))
      and (.command | test(":op[[:space:]]+(:)?edit([^a-zA-Z!-]|$)"))
      and (.command | contains(":expect"))
      and (.command | contains("--help") | not)
      and ((.exit_code // 1) == 0)
      and ((.aggregated_output // "") | contains(":verified"))
      and ((.aggregated_output // "") | contains(":whole-file-parsed true"))
      and ((.aggregated_output // "") | contains(":read-back-hash")))] | length > 0' \
      "$run_dir/commands.json")
  elif [[ "$task" == *-edit ]]; then
    verified=$(jq --arg target "$target_rel" '
      ([.[] | .command] | to_entries) as $commands
      | ($commands | map(select((.value | contains("clj-surgeon")) and (.value | contains("replace-subform!")) and (.value | contains("--help") | not))) | first | .key) as $apply
      | if $apply == null then false
        else (any($commands[]; (.key > $apply) and (.value | contains($target)))
              or (((.[$apply].exit_code // 1) == 0)
                  and ((.[$apply].aggregated_output // "") | contains(":verified")
                       and contains(":whole-file-parsed true")
                       and contains(":read-back-hash"))))
        end' "$run_dir/commands.json")
  fi

  local final_sha exact_correct correct
  final_sha=$(hash_task_targets "$task" "$workspace")
  printf '%s\n' "$final_sha" > "$run_dir/final.sha256"
  exact_correct=false
  correct=false
  local score_task=$task
  if is_portfolio_task "$task"; then
    score_task=__portfolio_task__
  fi
  case "$score_task" in
    __portfolio_task__)
      portfolio_task_dir=$(portfolio_dir_for_task "$task")
      portfolio_correct=true
      portfolio_exact=true
      : > "$run_dir/target.diff"
      : > "$run_dir/source-fidelity.edn"
      while IFS= read -r portfolio_target; do
        [ -n "$portfolio_target" ] || continue
        if ! cmp -s "$workspace/$portfolio_target" \
          "$portfolio_fixture_root/$portfolio_task_dir/after/$portfolio_target"; then
          portfolio_exact=false
        fi
        if ! bb "$repo_root/bench/score_source_fidelity.clj" \
          --target "$portfolio_target" \
          "$portfolio_fixture_root/$portfolio_task_dir/after/$portfolio_target" \
          "$workspace/$portfolio_target" >> "$run_dir/source-fidelity.edn"; then
          portfolio_correct=false
        fi
        if [ -f "$portfolio_fixture_root/$portfolio_task_dir/before/$portfolio_target" ]; then
          diff -u \
            "$portfolio_fixture_root/$portfolio_task_dir/before/$portfolio_target" \
            "$workspace/$portfolio_target" >> "$run_dir/target.diff" || true
        else
          diff -u /dev/null "$workspace/$portfolio_target" \
            >> "$run_dir/target.diff" || true
        fi
      done < <(targets_for_task "$task")
      if [ "$task" = dependency-move-edit ] \
        && ! clj-kondo --lint "$workspace/src/bench/move_order.clj" \
          --fail-level error >/dev/null 2>&1; then
        portfolio_correct=false
      fi
      if [ "$portfolio_exact" = true ]; then
        exact_correct=true
      fi
      if [ "$portfolio_correct" = true ]; then
        correct=true
      fi
      ;;
    named-form|semantic-form)
      correct=$(bb -e "(require '[clojure.string :as str]) (println (str/includes? (slurp \"$run_dir/final.txt\") (slurp \"$setup_root/expected/format-op-help.clj\")))") || correct=false
      exact_correct=$correct
      ;;
    structural-find)
      exact_correct=$(bb -e "(require '[clojure.string :as str]) (let [s (slurp \"$run_dir/final.txt\")] (println (boolean (and (str/includes? s (slurp \"$setup_root/expected/structural-1.clj\")) (str/includes? s (slurp \"$setup_root/expected/structural-2.clj\")) (re-find #\"(?i)(found|match count)[^0-9]{0,40}2\" s)))))") || exact_correct=false
      correct=$(bb -e "(require '[clojure.string :as str]) (let [normalize #(str/replace % #\"\\s+\" \" \") s (normalize (slurp \"$run_dir/final.txt\")) a (normalize (slurp \"$setup_root/expected/structural-1.clj\")) b (normalize (slurp \"$setup_root/expected/structural-2.clj\"))] (println (boolean (and (str/includes? s a) (str/includes? s b) (re-find #\"(?i)(found|match count)[^0-9]{0,40}2\" s)))))") || correct=false
      ;;
    case-inventory)
      exact_correct=$(bb "$repo_root/bench/score_pair_inventory.clj" case "$setup_root/expected/case-inventory.edn" "$run_dir/final.txt" exact) || exact_correct=false
      correct=$(bb "$repo_root/bench/score_pair_inventory.clj" case "$setup_root/expected/case-inventory.edn" "$run_dir/final.txt" normalized) || correct=false
      ;;
    cond-inventory)
      exact_correct=$(bb "$repo_root/bench/score_pair_inventory.clj" cond "$setup_root/expected/cond-inventory.edn" "$run_dir/final.txt" exact) || exact_correct=false
      correct=$(bb "$repo_root/bench/score_pair_inventory.clj" cond "$setup_root/expected/cond-inventory.edn" "$run_dir/final.txt" normalized) || correct=false
      ;;
    binding-inventory)
      exact_correct=$(bb "$repo_root/bench/score_pair_inventory.clj" binding "$setup_root/expected/binding-inventory.edn" "$run_dir/final.txt" exact) || exact_correct=false
      correct=$(bb "$repo_root/bench/score_pair_inventory.clj" binding "$setup_root/expected/binding-inventory.edn" "$run_dir/final.txt" normalized) || correct=false
      ;;
    xray-summary)
      exact_correct=$(bb -e "(require '[clojure.edn :as edn]) (try (println (= {:deny 129 :allow 113 :review 121} (edn/read-string (slurp \"$run_dir/final.txt\")))) (catch Exception _ (println false)))") || exact_correct=false
      correct=$exact_correct
      ;;
    xray-checksum)
      exact_correct=$(bb -e "(require '[clojure.edn :as edn]) (try (println (= 358967 (edn/read-string (slurp \"$run_dir/final.txt\")))) (catch Exception _ (println false)))") || exact_correct=false
      correct=$exact_correct
      ;;
    ops-registry-xray)
      exact_correct=$(bb "$repo_root/bench/score_ops_registry.clj" "$target" "$run_dir/final.txt") || exact_correct=false
      correct=$exact_correct
      ;;
    case-edit)
      if cmp -s "$target" "$setup_root/expected/state.clj"; then
        exact_correct=true
        correct=true
      fi
      diff -u "$setup_root/templates/state.clj" "$target" > "$run_dir/target.diff" || true
      ;;
    pair-view-edit)
      if cmp -s "$target" "$setup_root/expected/pair-view-edit.clj"; then
        exact_correct=true
        correct=true
      fi
      diff -u "$setup_root/templates/pair_view.clj" "$target" > "$run_dir/target.diff" || true
      ;;
    computed-edit|computed-supplied-edit)
      if cmp -s "$target" "$setup_root/expected/computed-edit.clj"; then
        exact_correct=true
        correct=true
      fi
      diff -u "$setup_root/templates/policy.clj" "$target" > "$run_dir/target.diff" || true
      ;;
    computed-repeated-edit)
      if cmp -s "$target" "$setup_root/expected/computed-repeated-edit.clj"; then
        exact_correct=true
        correct=true
      fi
      diff -u "$setup_root/templates/repeated_policy.clj" "$target" > "$run_dir/target.diff" || true
      ;;
    cond-edit|binding-edit)
      if cmp -s "$target" "$setup_root/expected/$task.clj"; then
        exact_correct=true
        correct=true
      fi
      diff -u "$setup_root/templates/peer_edit.clj" "$target" > "$run_dir/target.diff" || true
      ;;
  esac

  if ! is_portfolio_task "$task" \
    && [[ "$task" != *-edit ]] \
    && [ "$final_sha" != "$start_sha" ]; then
    exact_correct=false
    correct=false
  fi

  local route_adherent=true inspect_calls edit_calls transform_calls apply_calls
  inspect_calls=$(jq -s '[.[] | select(.type == "item.started"
    and .item.type == "mcp_tool_call"
    and .item.server == "clj-surgeon"
    and .item.tool == "inspect_clojure")] | length' "$run_dir/events.jsonl")
  edit_calls=$(jq -s '[.[] | select(.type == "item.started"
    and .item.type == "mcp_tool_call"
    and .item.server == "clj-surgeon"
    and .item.tool == "edit_clojure")] | length' "$run_dir/events.jsonl")
  transform_calls=$(jq -s '[.[] | select(.type == "item.started"
    and .item.type == "mcp_tool_call"
    and .item.server == "clj-surgeon"
    and .item.tool == "transform_clojure")] | length' "$run_dir/events.jsonl")
  apply_calls=$(jq -s '[.[] | select(.type == "item.started"
    and .item.type == "mcp_tool_call"
    and .item.server == "clj-surgeon"
    and .item.tool == "apply_clojure_changes")] | length' "$run_dir/events.jsonl")
  if ! computed_route_adherent "$context" "$inspect_calls" "$edit_calls" \
    "$transform_calls" "$apply_calls" "$file_changes" "$source_commands" \
    "$text_reader"; then
    route_adherent=false
  fi
  case "$context" in
    mcp-extraction-hint-no-skill)
      if [ "$inspect_calls" -ne 1 ] || [ "$apply_calls" -ne 1 ] \
        || [ "$edit_calls" -ne 0 ] || [ "$transform_calls" -ne 0 ] \
        || [ "$file_changes" -ne 0 ] || [ "$mcp_apply_successes" -ne 1 ] \
        || [ "$mcp_failures" -ne 0 ] || [ "$verified" != true ] \
        || [ "$single_change_transaction" != true ]; then
        route_adherent=false
      fi
      ;;
    edit-computed-hint-no-skill|mcp-transform-hint-no-skill)
      if [ "$mcp_apply_successes" -ne 1 ] || [ "$mcp_failures" -ne 0 ] \
        || [ "$verified" != true ] || [ "$single_change_transaction" != true ] \
        || [ "$failed_mutation_actions" -ne 0 ]; then
        route_adherent=false
      fi
      ;;
    native-computed-hint-no-skill)
      if [ "$failed_mutation_actions" -ne 0 ]; then
        route_adherent=false
      fi
      ;;
  esac
  printf '%s\n' "$route_adherent" > "$run_dir/route-adherent.txt"
  if [ "$route_adherent" != true ]; then
    exact_correct=false
    correct=false
  fi

  if [ "$version" = native ] && { [ "$clj_invocations" -ne 0 ] || [ "$skill_read" != false ]; }; then
    echo "Native-tools control invalid for $run_id: clj_invocations=$clj_invocations skill_read=$skill_read" >&2
    exact_correct=false
    correct=false
  fi

  local row
  printf -v row '%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s' \
    "$run_id" "$version" "$context" "$task" "$order" "$start_sha" "$final_sha" \
    "$wall_ms" "$exit_code" "$input_tokens" "$cached_tokens" "$uncached_tokens" \
    "$output_tokens" "$reasoning_tokens" "$shell_calls" "$file_changes" "$atomic_commands" \
    "$clj_invocations" "$source_commands" "$source_output_bytes" "$total_tool_output_bytes" \
    "$skill_read" "$show_form" "$grep_form" "$ls_used" "$help_used" "$text_reader" "$q_used" "$xray_used" "$partition_all_used" "$edit_used" "$expr_used" "$first_source_edit" \
    "$plan_generated" "$plan_applied" "$plan_apply_separate" "$verified" "$exact_correct" "$correct" \
    "$expect_used" "$expect_route" "$decision_supplied" "$post_decision_source_commands" \
    "$change_used" "$change_apply_used" "$change_apply_successes" "$failed_mutation_actions" \
    "$temp_manifest_patch" "$single_change_transaction" "$mcp_calls" "$mcp_successes" \
    "$mcp_failures" "$mcp_tool_output_bytes" "$mcp_first_mutation" \
    "$user_turns" "$tool_round_trips" "$discovery_round_trips" "$post_decision_round_trips"
  append_result_row "$run_id" "$row"

  printf '%-58s correct=%-5s wall=%6sms input=%7s commands=%s\n' \
    "$run_id" "$correct" "$wall_ms" "$input_tokens" "$shell_calls"
}

write_terminal_receipt() {
  local run_id=$1
  local state=$2
  local exit_code=$3
  local run_dir="$result_dir/$run_id"
  local receipt="$run_dir/terminal.tsv"
  local receipt_tmp="$run_dir/.terminal.tsv.tmp.$$"
  if [ "$state" = skipped ] && [ -f "$receipt" ]; then
    return
  fi
  mkdir -p "$run_dir"
  printf '%s\t%s\n' \
    run_id "$run_id" \
    state "$state" \
    exit_code "$exit_code" \
    finished_utc "$(date -u +%Y-%m-%dT%H:%M:%SZ)" \
    > "$receipt_tmp"
  mv "$receipt_tmp" "$receipt"
}

run_one_with_receipt() (
  local version=$1
  local context=$2
  local task=$3
  local order=$4
  local replicate=${5:-1}
  local run_id terminal_state=completed mcp_pid=""
  run_id=$(printf '%02d-r%02d-%s-%s-%s' "$order" "$replicate" "$task" "$context" "$version")
  trap 'status=$?; if [ -n "${mcp_pid:-}" ]; then kill "$mcp_pid" 2>/dev/null || true; wait "$mcp_pid" 2>/dev/null || true; fi; [ "$status" -eq 0 ] || terminal_state=failed; write_terminal_receipt "$run_id" "$terminal_state" "$status"; trap - EXIT; exit "$status"' EXIT
  run_one "$version" "$context" "$task" "$order" "$replicate"
)

order=0
tasks=${BENCH_TASKS:-'named-form semantic-form structural-find case-edit pair-view-expect-edit'}
contexts=${BENCH_CONTEXTS:-'no-skill matched-skill explicit-no-skill'}
include_compact=${BENCH_INCLUDE_COMPACT:-true}
replicates=${BENCH_REPLICATES:-1}
versions=${BENCH_VERSIONS:-'pre post'}
parallelism=${BENCH_PARALLELISM:-4}
run_matrix=${BENCH_RUN_MATRIX:-}
active_pids=()
active_run_ids=()
all_run_ids=()
benchmark_failed=0

if ! [[ "$parallelism" =~ ^[1-9][0-9]*$ ]]; then
  echo "BENCH_PARALLELISM must be a positive integer: $parallelism" >&2
  exit 2
fi
validate_run_matrix "$run_matrix"

schedule_run() {
  local version=$1
  local context=$2
  local task=$3
  local order=$4
  local replicate=${5:-1}
  local run_id
  run_id=$(printf '%02d-r%02d-%s-%s-%s' "$order" "$replicate" "$task" "$context" "$version")
  run_one_with_receipt "$@" &
  active_pids+=("$!")
  active_run_ids+=("$run_id")
  all_run_ids+=("$run_id")
  if [ "${#active_pids[@]}" -ge "$parallelism" ]; then
    wait_for_first_run
  fi
}

wait_for_first_run() {
  local pid=${active_pids[0]}
  local run_id=${active_run_ids[0]}
  local status=0
  if wait "$pid"; then
    status=0
  else
    status=$?
    benchmark_failed=1
  fi
  if [ ! -f "$result_dir/$run_id/terminal.tsv" ]; then
    write_terminal_receipt "$run_id" supervisor-failed "$status"
    benchmark_failed=1
  fi
  if [ "${#active_pids[@]}" -eq 1 ]; then
    active_pids=()
    active_run_ids=()
  else
    active_pids=("${active_pids[@]:1}")
    active_run_ids=("${active_run_ids[@]:1}")
  fi
}

wait_for_runs() {
  while [ "${#active_pids[@]}" -gt 0 ]; do
    wait_for_first_run
  done
}

if [ -n "$run_matrix" ]; then
  for replicate in $(seq 1 "$replicates"); do
    for task in $tasks; do
      for matrix_cell in $run_matrix; do
        version=${matrix_cell%%:*}
        context=${matrix_cell#*:}
        order=$((order + 1))
        schedule_run "$version" "$context" "$task" "$order" "$replicate"
      done
    done
  done
else
  for replicate in $(seq 1 "$replicates"); do
    for context in $contexts; do
      for task in $tasks; do
        order=$((order + 1))
        for version in $(counterbalanced_versions "$replicate" "$versions"); do
          schedule_run "$version" "$context" "$task" "$order" "$replicate"
          order=$((order + 1))
        done
      done
    done
  done
fi

if [ "$include_compact" = true ]; then
  for compact_context in compact-skill compact-v2-skill; do
    for task in $tasks; do
      order=$((order + 1))
      schedule_run post "$compact_context" "$task" "$order"
    done
  done
fi

wait_for_runs

for run_id in "${all_run_ids[@]}"; do
  if [ ! -f "$result_dir/$run_id/terminal.tsv" ]; then
    echo "Missing terminal receipt after all children exited: $run_id" >&2
    benchmark_failed=1
  fi
done

if [ "$benchmark_failed" -ne 0 ]; then
  echo "One or more benchmark children failed; all children were reaped and summary generation was skipped." >&2
  exit 1
fi

summary_tmp="$result_dir/.summary.md.tmp.$$"
if bb "$repo_root/bench/summarize_clean_codex.clj" "$result_dir/runs.tsv" > "$summary_tmp"; then
  mv "$summary_tmp" "$result_dir/summary.md"
else
  rm -f "$summary_tmp"
  echo "Summary generation failed after all terminal receipts; any existing summary.md was preserved." >&2
  exit 1
fi

printf '\nResults: %s\n' "$result_dir"
printf 'Summary: %s\n' "$result_dir/summary.md"

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
