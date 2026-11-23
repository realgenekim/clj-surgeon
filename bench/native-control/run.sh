#!/usr/bin/env bash
set -euo pipefail
[[ $# == 5 ]] || { echo 'Usage: run.sh TASKS.edn RUN_ID TASK_ID ARM REP' >&2; exit 2; }
script_dir=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)
tasks=$(realpath "$1"); run_id=$2; task_id=$3; arm=$4; rep=$5
[[ $run_id == "$task_id-$arm-r$rep" ]] || { echo "run_id must be TASK-ARM-rREP: $run_id" >&2; exit 2; }
# Identifiers become directory components; reject traversal before any writes.
keep=$(bb "$script_dir/schedule.clj" keep "$run_id" "${NC_KEEP:-0}")
root=${NC_RESULT_ROOT:-/var/tmp/forge/native-control}
mkdir -p "$root"
root=$(realpath "$root")
run_dir=$root/runs/$run_id
if [[ -f $run_dir/run.json ]]; then echo 'already complete'; exit 0; fi
model=${NC_MODEL:-gpt-6-astra}; reasoning=${NC_REASONING:-medium}
# workspace-write: the read-only sandbox blocks the analyzer admission temp file; writes are still caught by tree_changed.
# The two writable roots are the INVOKER's ~/.local/state/clj-surgeon (the shared clj-kondo admission lock and events;
# the analyzer resolves them from the passwd home, not $HOME) and ~/.local/state/diagnose-skiff-cpu-memory (the pressure
# status file the admission wrapper reads). Both are outside the experiment tree by design; nothing else is writable.
# Linux only: GNU date %3N, realpath, timeout and find -exec semantics are assumed.
config=$(realpath "${NC_SURGEON_BB_CONFIG:-$script_dir/../../bb.edn}")
auth=$(realpath "${NC_CODEX_AUTH:-$HOME/.codex/auth.json}")
bb_bin=$(command -v bb); java_bin=$(command -v java); codex_bin=$(command -v codex)
repo=$(bb "$script_dir/schedule.clj" task "$tasks" "$task_id" repo)
sha=$(bb "$script_dir/schedule.clj" task "$tasks" "$task_id" sha)
arm_file=$script_dir/$(bb "$script_dir/schedule.clj" task "$tasks" "$task_id" "$arm")
wrapper=$(bb "$script_dir/schedule.clj" wrapper "$arm" "$config")
ws=$root/ws/$run_id; codex_home=$root/homes/$run_id; bin_dir=$root/bin/$arm
rm -rf -- "$ws" "$codex_home"
mkdir -p "$ws" "$codex_home" "$bin_dir" "$run_dir"
# Incomplete retries must not inherit an earlier answer.
rm -f -- "$run_dir/answer.txt" "$bin_dir/clj-surgeon"
git -C "$repo" archive "$sha" | tar -x -C "$ws"
# Remove nested boot text too, so only the selected arm supplies rules.
find "$ws" \( -name AGENTS.md -o -name CLAUDE.md -o -name .codex -o -name .clj-surgeon.edn -o -name .mcp.json \) -prune -exec rm -rf -- {} +
cp "$arm_file" "$ws/AGENTS.md"
git -C "$ws" init -q
git -C "$ws" add -A -f
git -C "$ws" add -f AGENTS.md
git -C "$ws" -c user.name=native-control -c user.email=native-control@anvil -c commit.gpgsign=false commit -qm 'Frozen read experiment'
if [[ -n $wrapper ]]; then
  printf '%s\n' "$wrapper" > "$bin_dir/clj-surgeon"; chmod +x "$bin_dir/clj-surgeon"
  # The outline runs clj-kondo; expose only that binary, never the rest of ~/bin. The analyzer resolver
  # excludes $user.home/bin/clj-kondo as its shim (Java reads user.home from passwd, not $HOME), so point
  # at the system binary when there is one.
  kondo=/usr/local/bin/clj-kondo; [[ -x $kondo ]] || kondo=$(command -v clj-kondo)
  ln -sf "$kondo" "$bin_dir/clj-kondo"
  ln -sf "$bb_bin" "$bin_dir/bb"
fi
# No /usr/local/bin: a system-wide clj-surgeon lives there. bb reaches arms 2 and 3 through the symlink above.
run_path=$bin_dir:/usr/bin:/bin:$(dirname "$java_bin")
# Codex runs every command through `bash -lc`; a login shell reads $HOME/.profile, which on this box
# prepends ~/bin (the installed clj-surgeon launcher). So the run gets its own HOME whose profile sets PATH.
printf 'export PATH=%q\n' "$run_path" > "$codex_home/.profile"; cp "$codex_home/.profile" "$codex_home/.bashrc"
resolved=$(env -i HOME="$codex_home" PATH="$run_path" bash -lc 'command -v clj-surgeon || true')
if [[ -n $wrapper ]]; then
  [[ $resolved == "$bin_dir/clj-surgeon" ]] || { echo "preflight: clj-surgeon resolves to '$resolved', not the arm wrapper" >&2; exit 3; }
  [[ -n $(env -i HOME="$codex_home" PATH="$run_path" bash -lc 'command -v clj-kondo') ]] || { echo 'preflight: clj-kondo not on the arm PATH' >&2; exit 3; }
else
  [[ -z $resolved ]] || { echo "preflight: arm1 must not see clj-surgeon, found '$resolved'" >&2; exit 3; }
fi
printf 'resolved_clj_surgeon=%s\nbb_config=%s\n' "$resolved" "$config" > "$run_dir/preflight.txt"
# The credential is copied, not linked, and kept 0600 inside a 0700 home; the result root is 0700 too.
chmod 700 "$root" "$codex_home"; install -m 600 "$auth" "$codex_home/auth.json"
printf 'model = "%s"\nmodel_reasoning_effort = "%s"\n' "$model" "$reasoning" > "$codex_home/config.toml"
prompt=$(bb "$script_dir/schedule.clj" prompt "$tasks" "$task_id")
started_at=$(date -u +%Y-%m-%dT%H:%M:%SZ); start_ms=$(date +%s%3N)
set +e
HOME="$codex_home" CODEX_HOME="$codex_home" PATH="$run_path" timeout "${NC_TIMEOUT_S:-900}" \
  "$codex_bin" exec --json --ephemeral --ignore-user-config --skip-git-repo-check \
  --sandbox "${NC_SANDBOX:-workspace-write}" --color never \
  -c "sandbox_workspace_write.writable_roots=[\"$HOME/.local/state/clj-surgeon\",\"$HOME/.local/state/diagnose-skiff-cpu-memory\"]" -m "$model" -c "model_reasoning_effort=\"$reasoning\"" \
  -C "$ws" -o "$run_dir/answer.txt" "$prompt" > "$run_dir/events.jsonl" 2> "$run_dir/stderr.txt" </dev/null
exit_code=$?
set -e
end_ms=$(date +%s%3N); ended_at=$(date -u +%Y-%m-%dT%H:%M:%SZ)
status=$(git -C "$ws" status --porcelain --ignored --untracked-files=all)
bb "$script_dir/schedule.clj" receipt "$run_id" "$task_id" "$arm" "$rep" "$model" "$reasoning" \
  "$repo" "$sha" "$start_ms" "$end_ms" "$exit_code" "$status" "$(wc -c < "$arm_file")" \
  "$started_at" "$ended_at" > "$run_dir/run.json.tmp"
mv "$run_dir/run.json.tmp" "$run_dir/run.json"
if [[ $keep != true ]]; then rm -rf -- "$ws" "$codex_home"; fi
