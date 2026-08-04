#!/usr/bin/env bash
set -euo pipefail

script_path=$(cd "$(dirname "$0")" && pwd)/$(basename "$0")
timestamp=$(date -u +%Y%m%dT%H%M%SZ)
result_dir=${BENCH_RESULT_DIR:-/tmp/clj-surgeon-clean-codex-$timestamp}
owner_dir="$result_dir/.benchmark-owner"
owner_metadata="$owner_dir/owner.tsv"
owner_token=""

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
  printf '%s\n' "benchmark schedule self-test passed"
  exit 0
fi

if [ "${BENCH_HARNESS_SELF_TEST:-false}" = true ]; then
  self_test_root=$(mktemp -d /tmp/clj-surgeon-benchmark-self-test.XXXXXX)
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

  make_native_bin "$self_test_root/native-bin" "$PATH"
  if PATH="$self_test_root/native-bin" command -v clj-surgeon >/dev/null 2>&1; then
    echo "native self-test unexpectedly exposed clj-surgeon" >&2
    exit 1
  fi
  PATH="$self_test_root/native-bin" command -v sh >/dev/null

  result_dir=$original_result_dir
  rm -rf "$self_test_root"
  printf '%s\n' "benchmark harness self-test passed"
  exit 0
fi

repo_root=$(cd "$(dirname "$0")/.." && pwd)
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

git -C "$repo_root" cat-file -e "$pre_commit^{commit}"
git -C "$repo_root" cat-file -e "$post_commit^{commit}"

acquire_result_owner
setup_root=$(mktemp -d /tmp/clj-surgeon-benchmark-setup.XXXXXX)

mkdir -p "$result_dir" "$setup_root/tools/pre" "$setup_root/tools/post" \
  "$setup_root/bin/pre" "$setup_root/bin/post" "$setup_root/templates"

git -C "$repo_root" archive "$pre_commit" | tar -x -C "$setup_root/tools/pre"
git -C "$repo_root" archive "$post_commit" | tar -x -C "$setup_root/tools/post"

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

# Prewarm both versions outside the measured runs.
PATH="$setup_root/bin/pre:$PATH" clj-surgeon --help >/dev/null
PATH="$setup_root/bin/post:$PATH" clj-surgeon --help >/dev/null

git -C "$repo_root" show "$pre_commit:src/clj_surgeon/core.clj" \
  > "$setup_root/templates/core.clj"
git -C "$repo_root" show "$post_commit:src/clj_surgeon/core.clj" \
  > "$setup_root/templates/ops_registry.clj"
git -C "$repo_root" show "$post_commit:bench/fixtures/bench/pair_view.clj" \
  > "$setup_root/templates/pair_view.clj"

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
    'run_id\tversion\tcontext\ttask\torder\tstart_sha\tfinal_sha\twall_ms\texit_code\tinput_tokens\tcached_input_tokens\tuncached_input_tokens\toutput_tokens\treasoning_output_tokens\tshell_calls\tfile_changes\tatomic_commands\tclj_invocations\tsource_commands\tsource_output_bytes\ttotal_tool_output_bytes\tskill_read\tshow_form\tgrep_form\tls_used\thelp_used\ttext_reader\tq_used\txray_used\tpartition_all_used\tedit_used\texpr_used\tfirst_source_edit\tplan_generated\tplan_applied\tplan_apply_separate\tverified\texact_correct\tcorrect' \
    > "$result_dir/runs.tsv"
fi

task_prompt() {
  local task=$1
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
    computed-edit)
      printf '%s' 'In src/bench/policy.clj, add 100 to every number in the :retry-delays vector inside retry-policy. Preserve every unrelated byte, including the identical vector in unrelated-policy. A temporary plan artifact is allowed. Verify the exact change, do not read the whole file, and briefly name the commands used.'
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
  case "$1" in
    named-form|semantic-form) printf '%s' 'src/clj_surgeon/core.clj' ;;
    structural-find) printf '%s' 'src/bench/structural.clj' ;;
    case-edit) printf '%s' 'src/bench/state.clj' ;;
    computed-edit) printf '%s' 'src/bench/policy.clj' ;;
    cond-edit|binding-edit) printf '%s' 'src/bench/peer_edit.clj' ;;
    case-inventory|cond-inventory|binding-inventory|pair-view-edit) printf '%s' 'src/bench/pair_view.clj' ;;
    xray-summary|xray-checksum) printf '%s' 'src/bench/xray.clj' ;;
    ops-registry-xray) printf '%s' 'src/bench/ops_registry.clj' ;;
  esac
}

prepare_workspace() {
  local task=$1
  local workspace=$2
  mkdir -p "$workspace/src/clj_surgeon" "$workspace/src/bench"
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
    computed-edit)
      cp "$setup_root/templates/policy.clj" "$workspace/src/bench/policy.clj"
      ;;
    cond-edit|binding-edit)
      cp "$setup_root/templates/peer_edit.clj" "$workspace/src/bench/peer_edit.clj"
      ;;
    case-inventory|cond-inventory|binding-inventory|pair-view-edit)
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
      mkdir -p "$codex_home/skills/clj-surgeon"
      cp "$setup_root/tools/$version/skills/clj-surgeon/SKILL.md" \
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
    no-skill|explicit-no-skill|choice-no-skill|aware-no-skill|partition-hint-no-skill) ;;
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
  if [ "$version" = native ] && [ "$context" != no-skill ]; then
    echo "The native version is valid only with BENCH_CONTEXTS=no-skill: $run_id" >&2
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
  local target="$workspace/$target_rel"
  local bin_dir="$setup_root/bin/$version"
  local zsh_dir="$codex_home/zsh"

  mkdir -p "$run_dir" "$codex_home" "$zsh_dir"
  ln -s "$auth_file" "$codex_home/auth.json"
  install_treatment_skill "$version" "$context" "$codex_home"
  prepare_workspace "$task" "$workspace"
  local run_path
  run_path="$bin_dir:$PATH"
  if [ "$version" = native ]; then
    run_path=$bin_dir
    printf 'export PATH="%s"\n' "$bin_dir" > "$zsh_dir/.zprofile"
  else
    printf 'export PATH="%s:$%s"\n' "$bin_dir" PATH > "$zsh_dir/.zprofile"
  fi
  local resolved_cli
  resolved_cli=$(ZDOTDIR="$zsh_dir" /bin/zsh -lc 'command -v clj-surgeon' 2>/dev/null || true)
  if [ "$version" = native ] && [ -n "$resolved_cli" ]; then
    echo "Native-tools isolation exposed clj-surgeon for $run_id: $resolved_cli" >&2
    exit 2
  elif [ "$version" != native ] && [ "$resolved_cli" != "$bin_dir/clj-surgeon" ]; then
    echo "Version isolation failed for $run_id: $resolved_cli" >&2
    exit 2
  fi
  if [ "$version" = native ] && [ -d "$codex_home/skills" ] \
    && [ -n "$(find "$codex_home/skills" -iname '*clj-surgeon*' -print -quit)" ]; then
    echo "Native-tools isolation exposed a clj-surgeon skill for $run_id" >&2
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

  local start_sha
  start_sha=$(shasum -a 256 "$target" | awk '{print $1}')
  printf '%s\n' "$start_sha" > "$run_dir/start.sha256"

  local start_ms end_ms wall_ms exit_code sandbox
  start_ms=$(perl -MTime::HiRes=time -e 'printf "%.0f\n", time()*1000')
  sandbox=read-only
  [[ "$task" == *-edit ]] && sandbox='workspace-write'

  set +e
  PATH="$run_path" ZDOTDIR="$zsh_dir" CODEX_HOME="$codex_home" \
    codex exec --json --ephemeral --ignore-user-config --ignore-rules \
    --skip-git-repo-check --sandbox "$sandbox" --color never \
    -m "$model" -c "model_reasoning_effort=\"$reasoning\"" \
    -C "$workspace" "$(cat "$run_dir/prompt.txt")" \
    > "$run_dir/events.jsonl" 2> "$run_dir/stderr.txt" </dev/null
  exit_code=$?
  set -e

  end_ms=$(perl -MTime::HiRes=time -e 'printf "%.0f\n", time()*1000')
  wall_ms=$((end_ms - start_ms))

  jq -s -r '[.[] | select(.type == "item.completed" and .item.type == "agent_message")][-1].item.text // ""' \
    "$run_dir/events.jsonl" > "$run_dir/final.txt"
  jq -s '[.[] | select(.type == "item.completed" and .item.type == "command_execution") | .item]' \
    "$run_dir/events.jsonl" > "$run_dir/commands.json"
  jq -s '[.[] | select(.type == "item.started") | .item]' \
    "$run_dir/events.jsonl" > "$run_dir/started-items.json"
  jq -r '.[] | [.id, .command, (.exit_code // ""), (.aggregated_output | length)] | @tsv' \
    "$run_dir/commands.json" > "$run_dir/commands.tsv"

  local usage input_tokens cached_tokens uncached_tokens output_tokens reasoning_tokens
  usage=$(jq -s '[.[] | select(.type == "turn.completed")][-1].usage // {}' "$run_dir/events.jsonl")
  input_tokens=$(jq -r '.input_tokens // 0' <<< "$usage")
  cached_tokens=$(jq -r '.cached_input_tokens // 0' <<< "$usage")
  uncached_tokens=$((input_tokens - cached_tokens))
  output_tokens=$(jq -r '.output_tokens // 0' <<< "$usage")
  reasoning_tokens=$(jq -r '.reasoning_output_tokens // 0' <<< "$usage")

  local shell_calls file_changes atomic_commands clj_invocations source_commands
  local source_output_bytes total_tool_output_bytes
  shell_calls=$(jq 'length' "$run_dir/commands.json")
  file_changes=$(jq -s '[.[] | select(.type == "item.completed" and .item.type == "file_change")] | length' \
    "$run_dir/events.jsonl")
  atomic_commands=$(jq -r '.[].command' "$run_dir/commands.json" \
    | awk 'BEGIN {n=0} {n++; while (sub(/&&/, "")) n++} END {print n+0}')
  clj_invocations=$(jq '[.[] | select(.command | contains("clj-surgeon "))] | length' \
    "$run_dir/commands.json")
  source_commands=$(jq --arg target "$target_rel" \
    '[.[] | select(.command | contains($target))] | length' "$run_dir/commands.json")
  source_output_bytes=$(jq --arg target "$target_rel" \
    '[.[] | select(.command | contains($target)) | (.aggregated_output // "" | utf8bytelength)] | add // 0' \
    "$run_dir/commands.json")
  total_tool_output_bytes=$(jq '[.[] | (.aggregated_output // "" | utf8bytelength)] | add // 0' \
    "$run_dir/commands.json")

  local skill_read show_form grep_form ls_used help_used text_reader q_used xray_used partition_all_used
  local edit_used expr_used first_source_edit
  local plan_generated plan_applied chained_plan_apply plan_apply_separate verified
  skill_read=$(jq '[.[] | select(.command | contains("/skills/clj-surgeon/SKILL.md"))] | length > 0' "$run_dir/commands.json")
  show_form=$(jq '[.[] | select((.command | contains("clj-surgeon")) and ((.command | contains("show-form")) or (.command | test(":cat([^a-zA-Z]|$)"))))] | length > 0' "$run_dir/commands.json")
  grep_form=$(jq '[.[] | select((.command | contains("clj-surgeon")) and (.command | contains("grep-form")))] | length > 0' "$run_dir/commands.json")
  ls_used=$(jq '[.[] | select((.command | contains("clj-surgeon")) and (.command | test(":ls([^a-zA-Z]|$)")))] | length > 0' "$run_dir/commands.json")
  help_used=$(jq '[.[] | select(.command | contains("--help"))] | length > 0' "$run_dir/commands.json")
  text_reader=$(jq --arg target "$target_rel" \
    '[.[] | select((.command | contains($target)) and (.command | test("(^|[ /])(rg|sed|awk|head|tail|cat)( |$)")))] | length > 0' \
    "$run_dir/commands.json")
  q_used=$(jq '[.[] | select((.command | contains("clj-surgeon")) and (.command | test(":op[[:space:]]+(:)?(q|lens)([^a-zA-Z-]|$)")))] | length > 0' "$run_dir/commands.json")
  xray_used=$(jq '[.[] | select((.command | contains("clj-surgeon")) and (.command | test(":op[[:space:]]+(:)?xray([^a-zA-Z-]|$)")))] | length > 0' "$run_dir/commands.json")
  partition_all_used=$(jq '[.[] | select((.command | contains("clj-surgeon")) and (.command | contains("partition-all")))] | length > 0' "$run_dir/commands.json")
  edit_used=$(jq '[.[] | select((.command | contains("clj-surgeon")) and (.command | test(":op[[:space:]]+(:)?edit([^a-zA-Z!-]|$)")))] | length > 0' "$run_dir/commands.json")
  expr_used=$(jq '[.[] | select((.command | contains("clj-surgeon")) and (.command | test(":expr([[:space:]]|$)")))] | length > 0' "$run_dir/commands.json")
  first_source_edit=$(jq --arg target "$target_rel" '
    ([.[]
      | select((.type == "command_execution" and (.command | contains($target)))
               or (.type == "file_change"
                   and any(.changes[]?; .path | contains($target))))]
     | first // {}) as $first
    | (($first.type == "command_execution")
       and ($first.command | contains("clj-surgeon"))
       and ($first.command | test(":op[[:space:]]+(:)?edit([^a-zA-Z!-]|$)")))' \
    "$run_dir/started-items.json")
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

  verified=false
  if [[ "$task" == *-edit ]]; then
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
  final_sha=$(shasum -a 256 "$target" | awk '{print $1}')
  printf '%s\n' "$final_sha" > "$run_dir/final.sha256"
  exact_correct=false
  correct=false
  case "$task" in
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
    computed-edit)
      if cmp -s "$target" "$setup_root/expected/computed-edit.clj"; then
        exact_correct=true
        correct=true
      fi
      diff -u "$setup_root/templates/policy.clj" "$target" > "$run_dir/target.diff" || true
      ;;
    cond-edit|binding-edit)
      if cmp -s "$target" "$setup_root/expected/$task.clj"; then
        exact_correct=true
        correct=true
      fi
      diff -u "$setup_root/templates/peer_edit.clj" "$target" > "$run_dir/target.diff" || true
      ;;
  esac

  if [[ "$task" != *-edit ]] && [ "$final_sha" != "$start_sha" ]; then
    exact_correct=false
    correct=false
  fi

  if [ "$version" = native ] && { [ "$clj_invocations" -ne 0 ] || [ "$skill_read" != false ]; }; then
    echo "Native-tools control invalid for $run_id: clj_invocations=$clj_invocations skill_read=$skill_read" >&2
    exact_correct=false
    correct=false
  fi

  local row
  printf -v row '%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s' \
    "$run_id" "$version" "$context" "$task" "$order" "$start_sha" "$final_sha" \
    "$wall_ms" "$exit_code" "$input_tokens" "$cached_tokens" "$uncached_tokens" \
    "$output_tokens" "$reasoning_tokens" "$shell_calls" "$file_changes" "$atomic_commands" \
    "$clj_invocations" "$source_commands" "$source_output_bytes" "$total_tool_output_bytes" \
    "$skill_read" "$show_form" "$grep_form" "$ls_used" "$help_used" "$text_reader" "$q_used" "$xray_used" "$partition_all_used" "$edit_used" "$expr_used" "$first_source_edit" \
    "$plan_generated" "$plan_applied" "$plan_apply_separate" "$verified" "$exact_correct" "$correct"
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
  local run_id terminal_state=completed
  run_id=$(printf '%02d-r%02d-%s-%s-%s' "$order" "$replicate" "$task" "$context" "$version")
  trap 'status=$?; [ "$status" -eq 0 ] || terminal_state=failed; write_terminal_receipt "$run_id" "$terminal_state" "$status"; trap - EXIT; exit "$status"' EXIT
  run_one "$version" "$context" "$task" "$order" "$replicate"
)

order=0
tasks=${BENCH_TASKS:-'named-form semantic-form structural-find case-edit'}
contexts=${BENCH_CONTEXTS:-'no-skill matched-skill explicit-no-skill'}
include_compact=${BENCH_INCLUDE_COMPACT:-true}
replicates=${BENCH_REPLICATES:-1}
versions=${BENCH_VERSIONS:-'pre post'}
parallelism=${BENCH_PARALLELISM:-4}
active_pids=()
active_run_ids=()
all_run_ids=()
benchmark_failed=0

if ! [[ "$parallelism" =~ ^[1-9][0-9]*$ ]]; then
  echo "BENCH_PARALLELISM must be a positive integer: $parallelism" >&2
  exit 2
fi

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
