#!/usr/bin/env bash
set -euo pipefail

case_id=${1:?usage: run_counterfactual_replay.sh CASE_ID ARM RESULT_DIR}
arm=${2:?usage: run_counterfactual_replay.sh CASE_ID ARM RESULT_DIR}
result_dir=${3:?usage: run_counterfactual_replay.sh CASE_ID ARM RESULT_DIR}

repo_root=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd -P)
case_dir="$repo_root/bench/counterfactual-replay/cases/$case_id"
model=${BENCH_MODEL:-gpt-5.6-sol}
reasoning=${BENCH_REASONING:-high}
exec_user=${REPLAY_EXEC_USER:-}
mcp_write_user=${REPLAY_MCP_WRITE_USER:-}
mcp_url=${REPLAY_MCP_URL:-}
dry_run=${REPLAY_DRY_RUN:-false}
skip_verification=${REPLAY_SKIP_VERIFICATION:-false}
codex_command=${REPLAY_CODEX_CMD:-codex}
sandbox=${BENCH_SANDBOX_MODE:-workspace-write}
route_card_file=${REPLAY_ROUTE_CARD_FILE:-}

case "$arm" in
  native|structural|production) ;;
  *)
    echo "arm must be native, structural, or production: $arm" >&2
    exit 2
    ;;
esac
case "$sandbox" in
  read-only|workspace-write|danger-full-access) ;;
  *)
    echo "BENCH_SANDBOX_MODE must be read-only, workspace-write, or danger-full-access: $sandbox" >&2
    exit 2
    ;;
esac
test -f "$case_dir/capsule.edn" || {
  echo "unknown counterfactual replay case: $case_id" >&2
  exit 2
}
[[ "$result_dir" = /* ]] || {
  echo "result directory must be absolute: $result_dir" >&2
  exit 2
}
test ! -e "$result_dir" || {
  echo "result directory already exists: $result_dir" >&2
  exit 2
}
if [ "$arm" != native ] && [ -z "$mcp_url" ]; then
  echo "REPLAY_MCP_URL is required for $arm" >&2
  exit 2
fi
if [ -n "$route_card_file" ]; then
  [ "$arm" = production ] || {
    echo "REPLAY_ROUTE_CARD_FILE is supported only for the production arm" >&2
    exit 2
  }
  test -f "$route_card_file" || {
    echo "route card not found: $route_card_file" >&2
    exit 2
  }
fi

for command_name in bb git jq perl shasum; do
  command -v "$command_name" >/dev/null 2>&1 || {
    echo "missing required command: $command_name" >&2
    exit 2
  }
done
if [ "$dry_run" != true ]; then
  command -v "$codex_command" >/dev/null 2>&1 || {
    echo "missing Codex command: $codex_command" >&2
    exit 2
  }
fi
if [ -n "$exec_user" ]; then
  id "$exec_user" >/dev/null 2>&1 || {
    echo "unknown execution user: $exec_user" >&2
    exit 2
  }
fi
if [ -n "$mcp_write_user" ]; then
  command -v setfacl >/dev/null 2>&1 || {
    echo "setfacl is required for REPLAY_MCP_WRITE_USER" >&2
    exit 2
  }
  id "$mcp_write_user" >/dev/null 2>&1 || {
    echo "unknown MCP write user: $mcp_write_user" >&2
    exit 2
  }
fi

mkdir -p "$result_dir"
workspace="$result_dir/workspace"
"$repo_root/bench/materialize_counterfactual_replay.sh" \
  "$case_id" "$workspace" "$result_dir/materialization.edn" \
  >"$result_dir/materialization.tsv"

if [ -n "$exec_user" ]; then
  chown -R "$exec_user":"$(id -gn "$exec_user")" "$workspace"
fi
if [ -n "$mcp_write_user" ]; then
  setfacl -R -m "u:$mcp_write_user:rwx" "$workspace"
  setfacl -R -d -m "u:$mcp_write_user:rwx" "$workspace"
fi

setup_root=$(mktemp -d "${TMPDIR:-/var/tmp}/clj-surgeon-counterfactual-run.XXXXXX")
cleanup() {
  if [[ -n "${setup_root:-}" && -d "$setup_root" && "$setup_root" = "${TMPDIR:-/var/tmp}"/clj-surgeon-counterfactual-run.* ]]; then
    rm -rf -- "$setup_root"
  fi
}
trap cleanup EXIT HUP INT TERM

codex_home="$setup_root/codex-home"
mkdir -p "$codex_home"
if [ -n "$exec_user" ]; then
  exec_home=$(getent passwd "$exec_user" | cut -d: -f6)
else
  exec_home=$HOME
fi
auth_file=${CODEX_AUTH_FILE:-$exec_home/.codex/auth.json}
test -f "$auth_file" || {
  echo "Codex authentication file not found: $auth_file" >&2
  exit 2
}
ln -s "$auth_file" "$codex_home/auth.json"

if [ "$arm" != native ]; then
  bb -e '(let [[path url] *command-line-args*]
           (spit path
                 (str "[mcp_servers.clj-surgeon]\n"
                      "url = \"" url "\"\n"
                      "required = true\n"
                      "enabled_tools = [\"inspect_clojure\", \"apply_clojure_changes\", \"edit_clojure\", \"transform_clojure\"]\n")))' \
    "$codex_home/config.toml" "$mcp_url"
fi
if [ "$arm" = production ]; then
  mkdir -p "$codex_home/skills/clj-surgeon"
  cp "$repo_root/skills/clj-surgeon/SKILL.md" "$codex_home/skills/clj-surgeon/SKILL.md"
fi
if [ -n "$exec_user" ]; then
  chown -R "$exec_user":"$(id -gn "$exec_user")" "$setup_root"
fi

cp "$case_dir/task.md" "$result_dir/task.md"
case "$arm" in
  native)
    route_prompt='Use native bounded source inspection and apply_patch for all mutations. Do not use clj-surgeon, MCP tools, Git history, reflogs, remotes, or files outside this workspace.'
    ;;
  structural)
    route_prompt='Use inspect_clojure for Clojure inspection and edit_clojure, transform_clojure, or apply_clojure_changes for every Clojure mutation. Prefer transform_clojure when one bounded pure relation computes repeated replacements. Do not use native patching for Clojure, Git history, reflogs, remotes, or files outside this workspace.'
    ;;
  production)
    route_prompt='Use the installed clj-surgeon skill and choose the fastest safe route for each change. Do not use Git history, reflogs, remotes, prior benchmark results, or files outside this workspace.'
    ;;
esac
if [ -n "$route_card_file" ]; then
  cp "$route_card_file" "$result_dir/route-card.md"
  route_prompt="$route_prompt

$(cat "$route_card_file")"
fi
{
  cat "$case_dir/task.md"
  printf '\n%s\n' "$route_prompt"
  printf '%s\n' 'The repository in this workspace has exactly one synthetic baseline commit. Complete the task, run the requested verification, and briefly report the route and result.'
} >"$result_dir/prompt.md"

if [ "$dry_run" = true ]; then
  jq -n \
    --arg case_id "$case_id" \
    --arg arm "$arm" \
    --arg workspace "$workspace" \
    --arg codex_home "$codex_home" \
    --arg mcp_url "$mcp_url" \
    '{schema:"clj-surgeon.counterfactual-replay-preflight/v1",ok:true,case_id:$case_id,arm:$arm,workspace:$workspace,codex_home:$codex_home,mcp_url:$mcp_url}' \
    >"$result_dir/preflight.json"
  printf 'preflight\t%s\t%s\t%s\n' "$case_id" "$arm" "$result_dir"
  exit 0
fi

codex_args=(exec --json --ephemeral --skip-git-repo-check --sandbox "$sandbox" --color never)
if [ "$arm" != production ]; then
  codex_args+=(--ignore-rules)
fi
codex_args+=(-m "$model" -c "model_reasoning_effort=\"$reasoning\"" -C "$workspace")

start_ms=$(perl -MTime::HiRes=time -e 'printf "%.0f\n", time()*1000')
set +e
if [ -n "$exec_user" ]; then
  # The harness remains root and intentionally owns the evidence files; sudo
  # applies only to the Codex process, not these redirects.
  # shellcheck disable=SC2024
  sudo -u "$exec_user" env -u ZMX_SESSION \
    HOME="$exec_home" CODEX_HOME="$codex_home" \
    "$codex_command" "${codex_args[@]}" "$(cat "$result_dir/prompt.md")" \
    >"$result_dir/events.jsonl" 2>"$result_dir/stderr.txt" </dev/null
else
  env -u ZMX_SESSION CODEX_HOME="$codex_home" \
    "$codex_command" "${codex_args[@]}" "$(cat "$result_dir/prompt.md")" \
    >"$result_dir/events.jsonl" 2>"$result_dir/stderr.txt" </dev/null
fi
codex_exit=$?
set -e
end_ms=$(perl -MTime::HiRes=time -e 'printf "%.0f\n", time()*1000')
wall_ms=$((end_ms - start_ms))

git -C "$workspace" status --porcelain=v1 >"$result_dir/status.txt"
git -C "$workspace" diff --binary HEAD >"$result_dir/change.patch"
git -C "$workspace" diff --name-only HEAD >"$result_dir/changed-paths.txt"

exact_targets=true
while IFS=$'\t' read -r target expected_hash; do
  if [ ! -f "$workspace/$target" ]; then
    exact_targets=false
    continue
  fi
  actual_hash=$(shasum -a 256 "$workspace/$target" | awk '{print $1}')
  if [ "$actual_hash" != "$expected_hash" ]; then
    exact_targets=false
  fi
done < <(bb -e '(require (quote [clojure.edn :as edn]))
                 (let [capsule (edn/read-string (slurp (first *command-line-args*)))]
                   (doseq [target (:targets capsule)]
                     (println (str target "\t" (get-in capsule [:hashes target :after])))))' \
  "$case_dir/capsule.edn")

expected_paths="$setup_root/expected-paths.txt"
bb -e '(require (quote [clojure.edn :as edn]))
         (doseq [target (:targets (edn/read-string (slurp (first *command-line-args*))))]
           (println target))' "$case_dir/capsule.edn" >"$expected_paths"
sort -o "$expected_paths" "$expected_paths"
sort -o "$result_dir/changed-paths.txt" "$result_dir/changed-paths.txt"
ignored_paths="$setup_root/ignored-paths.txt"
bb -e '(require (quote [clojure.edn :as edn]))
         (doseq [path (:ignored-paths (edn/read-string (slurp (first *command-line-args*))))]
           (println path))' "$case_dir/capsule.edn" >"$ignored_paths"
unexpected_untracked=false
while IFS= read -r status_line; do
  case "$status_line" in
    '?? '*)
      untracked_path=${status_line#'?? '}
      ignored=false
      while IFS= read -r ignored_prefix; do
        case "$untracked_path" in
          "$ignored_prefix"*) ignored=true ;;
        esac
      done <"$ignored_paths"
      if [ "$ignored" != true ]; then
        unexpected_untracked=true
      fi
      ;;
  esac
done <"$result_dir/status.txt"
exact_paths=false
if cmp -s "$expected_paths" "$result_dir/changed-paths.txt" \
  && [ "$unexpected_untracked" = false ]; then
  exact_paths=true
fi

verification_exit=0
: >"$result_dir/verification.log"
if [ "$skip_verification" = true ]; then
  verification_exit=125
  printf '%s\n' 'verification skipped by explicit harness self-test control' \
    >"$result_dir/verification.log"
else
  while IFS= read -r verification_command; do
    printf '$ %s\n' "$verification_command" >>"$result_dir/verification.log"
    set +e
    if [ -n "$exec_user" ]; then
      # The harness intentionally owns the verification transcript.
      # shellcheck disable=SC2024
      sudo -u "$exec_user" bash -lc "cd '$workspace' && $verification_command" \
        >>"$result_dir/verification.log" 2>&1
    else
      bash -lc "cd '$workspace' && $verification_command" \
        >>"$result_dir/verification.log" 2>&1
    fi
    command_exit=$?
    set -e
    if [ "$command_exit" -ne 0 ]; then
      verification_exit=$command_exit
      break
    fi
  done < <(bb -e '(require (quote [clojure.edn :as edn]))
                   (doseq [command (get-in (edn/read-string (slurp (first *command-line-args*)))
                                           [:verification :commands])]
                     (println command))' "$case_dir/capsule.edn")
fi

semantic_pass=false
if [ "$verification_exit" -eq 0 ] && [ "$exact_paths" = true ]; then
  semantic_pass=true
fi
exact_oracle=false
if [ "$semantic_pass" = true ] && [ "$exact_targets" = true ]; then
  exact_oracle=true
fi

usage=$(jq -s '[.[] | select(.type == "turn.completed")][-1].usage // {}' \
  "$result_dir/events.jsonl" 2>/dev/null || printf '{}')
tool_actions=$(jq -s '[.[] | select(.type == "item.started") | .item | select(.type == "command_execution" or .type == "mcp_tool_call" or .type == "file_change")] | length' \
  "$result_dir/events.jsonl" 2>/dev/null || printf '0')
mcp_actions=$(jq -s '[.[] | select(.type == "item.started" and .item.type == "mcp_tool_call")] | length' \
  "$result_dir/events.jsonl" 2>/dev/null || printf '0')

jq -n \
  --arg case_id "$case_id" \
  --arg arm "$arm" \
  --arg model "$model" \
  --arg reasoning "$reasoning" \
  --arg sandbox "$sandbox" \
  --argjson wall_ms "$wall_ms" \
  --argjson codex_exit "$codex_exit" \
  --argjson verification_exit "$verification_exit" \
  --argjson exact_paths "$exact_paths" \
  --argjson exact_targets "$exact_targets" \
  --argjson unexpected_untracked "$unexpected_untracked" \
  --argjson semantic_pass "$semantic_pass" \
  --argjson exact_oracle "$exact_oracle" \
  --argjson tool_actions "$tool_actions" \
  --argjson mcp_actions "$mcp_actions" \
  --argjson usage "$usage" \
  '{schema:"clj-surgeon.counterfactual-replay-result/v1",case_id:$case_id,arm:$arm,model:$model,reasoning:$reasoning,sandbox:$sandbox,wall_ms:$wall_ms,codex_exit:$codex_exit,verification_exit:$verification_exit,exact_paths:$exact_paths,exact_targets:$exact_targets,unexpected_untracked:$unexpected_untracked,semantic_pass:$semantic_pass,exact_oracle:$exact_oracle,tool_actions:$tool_actions,mcp_actions:$mcp_actions,usage:$usage}' \
  >"$result_dir/result.json"

cat "$result_dir/result.json"
