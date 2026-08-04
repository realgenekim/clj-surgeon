#!/usr/bin/env bash
set -euo pipefail

repo_root=$(cd "$(dirname "$0")/.." && pwd)
pre_commit=${BENCH_PRE_COMMIT:-19a20b0}
post_commit=${BENCH_POST_COMMIT:-80154bc}
model=${BENCH_MODEL:-gpt-5.6-sol}
reasoning=${BENCH_REASONING:-medium}
timestamp=$(date -u +%Y%m%dT%H%M%SZ)
result_dir=${BENCH_RESULT_DIR:-/tmp/clj-surgeon-clean-codex-$timestamp}
setup_root=$(mktemp -d /tmp/clj-surgeon-benchmark-setup.XXXXXX)

cleanup() {
  rm -rf "$setup_root"
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

# Prewarm both versions outside the measured runs.
PATH="$setup_root/bin/pre:$PATH" clj-surgeon --help >/dev/null
PATH="$setup_root/bin/post:$PATH" clj-surgeon --help >/dev/null

git -C "$repo_root" show "$pre_commit:src/clj_surgeon/core.clj" \
  > "$setup_root/templates/core.clj"
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

make_structural_fixture "$setup_root/templates/structural.clj"
make_edit_fixture "$setup_root/templates/state.clj"
make_peer_edit_fixture "$setup_root/templates/peer_edit.clj"
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
cp "$setup_root/templates/peer_edit.clj" "$setup_root/expected/cond-edit.clj"
perl -0pi -e 's/\{:decision :allow :reason :public\}/\{:decision :allow :reason :public-resource\}/' \
  "$setup_root/expected/cond-edit.clj"
cp "$setup_root/templates/peer_edit.clj" "$setup_root/expected/binding-edit.clj"
perl -0pi -e 's/\(or \(:timeout-ms request\) \(:timeout-ms defaults\)\)/\(or \(:timeout-ms request\) 5000\)/' \
  "$setup_root/expected/binding-edit.clj"

if [ "${BENCH_RESUME:-false}" != true ] || [ ! -f "$result_dir/runs.tsv" ]; then
  printf '%b\n' \
    'run_id\tversion\tcontext\ttask\torder\tstart_sha\tfinal_sha\twall_ms\texit_code\tinput_tokens\tcached_input_tokens\tuncached_input_tokens\toutput_tokens\treasoning_output_tokens\tshell_calls\tfile_changes\tatomic_commands\tclj_invocations\tsource_commands\tsource_output_bytes\ttotal_tool_output_bytes\tskill_read\tshow_form\tgrep_form\tls_used\thelp_used\ttext_reader\tq_used\tpartition_all_used\tedit_used\texpr_used\tfirst_source_edit\tplan_generated\tplan_applied\tplan_apply_separate\tverified\texact_correct\tcorrect' \
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
    cond-edit|binding-edit) printf '%s' 'src/bench/peer_edit.clj' ;;
    case-inventory|cond-inventory|binding-inventory) printf '%s' 'src/bench/pair_view.clj' ;;
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
    cond-edit|binding-edit)
      cp "$setup_root/templates/peer_edit.clj" "$workspace/src/bench/peer_edit.clj"
      ;;
    case-inventory|cond-inventory|binding-inventory)
      cp "$setup_root/templates/pair_view.clj" "$workspace/src/bench/pair_view.clj"
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
  if [ "${BENCH_RESUME:-false}" = true ] \
    && awk -F '\t' -v id="$run_id" '$1 == id {found=1} END {exit !found}' "$result_dir/runs.tsv"; then
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
  printf 'export PATH="%s:$%s"\n' "$bin_dir" PATH > "$zsh_dir/.zprofile"
  local resolved_cli
  resolved_cli=$(ZDOTDIR="$zsh_dir" /bin/zsh -lc 'command -v clj-surgeon')
  if [ "$resolved_cli" != "$bin_dir/clj-surgeon" ]; then
    echo "Version isolation failed for $run_id: $resolved_cli" >&2
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
  PATH="$bin_dir:$PATH" ZDOTDIR="$zsh_dir" CODEX_HOME="$codex_home" \
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

  local skill_read show_form grep_form ls_used help_used text_reader q_used partition_all_used
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
    case-edit)
      if cmp -s "$target" "$setup_root/expected/state.clj"; then
        exact_correct=true
        correct=true
      fi
      diff -u "$setup_root/templates/state.clj" "$target" > "$run_dir/target.diff" || true
      ;;
    cond-edit|binding-edit)
      if cmp -s "$target" "$setup_root/expected/$task.clj"; then
        exact_correct=true
        correct=true
      fi
      diff -u "$setup_root/templates/peer_edit.clj" "$target" > "$run_dir/target.diff" || true
      ;;
  esac

  printf '%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\n' \
    "$run_id" "$version" "$context" "$task" "$order" "$start_sha" "$final_sha" \
    "$wall_ms" "$exit_code" "$input_tokens" "$cached_tokens" "$uncached_tokens" \
    "$output_tokens" "$reasoning_tokens" "$shell_calls" "$file_changes" "$atomic_commands" \
    "$clj_invocations" "$source_commands" "$source_output_bytes" "$total_tool_output_bytes" \
    "$skill_read" "$show_form" "$grep_form" "$ls_used" "$help_used" "$text_reader" "$q_used" "$partition_all_used" "$edit_used" "$expr_used" "$first_source_edit" \
    "$plan_generated" "$plan_applied" "$plan_apply_separate" "$verified" "$exact_correct" "$correct" \
    >> "$result_dir/runs.tsv"

  printf '%-58s correct=%-5s wall=%6sms input=%7s commands=%s\n' \
    "$run_id" "$correct" "$wall_ms" "$input_tokens" "$shell_calls"
}

order=0
tasks=${BENCH_TASKS:-'named-form semantic-form structural-find case-edit'}
contexts=${BENCH_CONTEXTS:-'no-skill matched-skill explicit-no-skill'}
include_compact=${BENCH_INCLUDE_COMPACT:-true}
replicates=${BENCH_REPLICATES:-1}
versions=${BENCH_VERSIONS:-'pre post'}

for replicate in $(seq 1 "$replicates"); do
  for context in $contexts; do
    for task in $tasks; do
      order=$((order + 1))
      for version in $versions; do
        run_one "$version" "$context" "$task" "$order" "$replicate"
        order=$((order + 1))
      done
    done
  done
done

if [ "$include_compact" = true ]; then
  for compact_context in compact-skill compact-v2-skill; do
    for task in $tasks; do
      order=$((order + 1))
      run_one post "$compact_context" "$task" "$order"
    done
  done
fi

bb "$repo_root/bench/summarize_clean_codex.clj" "$result_dir/runs.tsv" \
  > "$result_dir/summary.md"

printf '\nResults: %s\n' "$result_dir"
printf 'Summary: %s\n' "$result_dir/summary.md"
