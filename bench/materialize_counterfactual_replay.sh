#!/usr/bin/env bash
set -euo pipefail

case_id=${1:?usage: materialize_counterfactual_replay.sh CASE_ID DESTINATION RECEIPT}
destination=${2:?usage: materialize_counterfactual_replay.sh CASE_ID DESTINATION RECEIPT}
receipt=${3:?usage: materialize_counterfactual_replay.sh CASE_ID DESTINATION RECEIPT}

repo_root=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd -P)
case_dir="$repo_root/bench/counterfactual-replay/cases/$case_id"

[[ "$case_id" =~ ^[a-z0-9][a-z0-9-]*$ ]] || {
  echo "invalid case id: $case_id" >&2
  exit 2
}
test -f "$case_dir/capsule.edn" || {
  echo "unknown counterfactual replay case: $case_id" >&2
  exit 2
}
[[ "$destination" = /* ]] || {
  echo "destination must be absolute: $destination" >&2
  exit 2
}
test "$destination" != / || {
  echo "refusing broad destination: $destination" >&2
  exit 2
}
test "$destination" != "$HOME" || {
  echo "refusing home destination: $destination" >&2
  exit 2
}
test "$destination" != "$repo_root" || {
  echo "refusing source repository destination: $destination" >&2
  exit 2
}
test ! -e "$destination" || {
  echo "destination already exists: $destination" >&2
  exit 2
}

for command_name in bb git tar shasum; do
  command -v "$command_name" >/dev/null 2>&1 || {
    echo "missing required command: $command_name" >&2
    exit 2
  }
done

bb "$repo_root/bench/verify_counterfactual_replay.clj" >/dev/null

parent=$(bb -e '(require (quote [clojure.edn :as edn]))
                 (print (get-in (edn/read-string (slurp (first *command-line-args*)))
                                [:repository :parent]))' \
  "$case_dir/capsule.edn")

destination_parent=$(dirname "$destination")
mkdir -p "$destination_parent" "$(dirname "$receipt")"
stage=$(mktemp -d "$destination_parent/.counterfactual-replay.XXXXXX")

cleanup() {
  if [[ -n "${stage:-}" && -d "$stage" && "$stage" = "$destination_parent"/.counterfactual-replay.* ]]; then
    rm -rf -- "$stage"
  fi
}
trap cleanup EXIT HUP INT TERM

git -C "$repo_root" archive "$parent" | tar -x -C "$stage"
bb "$repo_root/bench/initialize_benchmark_workspace.clj" "$stage" >/dev/null

while IFS=$'\t' read -r target expected_hash; do
  actual_hash=$(shasum -a 256 "$stage/$target" | awk '{print $1}')
  test "$actual_hash" = "$expected_hash" || {
    echo "materialized hash mismatch: $target" >&2
    exit 1
  }
done < <(bb -e '(require (quote [clojure.edn :as edn]))
                 (let [capsule (edn/read-string (slurp (first *command-line-args*)))]
                   (doseq [target (:targets capsule)]
                     (println (str target "\t" (get-in capsule [:hashes target :before])))))' \
  "$case_dir/capsule.edn")

test -z "$(git -C "$stage" status --short)"
test "$(git -C "$stage" rev-list --count HEAD)" -eq 1
test -z "$(git -C "$stage" remote)"

mv "$stage" "$destination"
stage=""

workspace_commit=$(git -C "$destination" rev-parse HEAD)
capsule_hash=$(shasum -a 256 "$case_dir/capsule.edn" | awk '{print $1}')
task_hash=$(shasum -a 256 "$case_dir/task.md" | awk '{print $1}')

bb -e '(let [[receipt case-id parent workspace workspace-commit capsule-hash task-hash] *command-line-args*]
         (spit receipt
               (str (pr-str {:schema "clj-surgeon.counterfactual-replay-materialization/v1"
                             :case-id (keyword case-id)
                             :parent parent
                             :workspace workspace
                             :workspace-commit workspace-commit
                             :capsule-sha256 capsule-hash
                             :task-sha256 task-hash
                             :history-visible-to-caller false
                             :remote-visible-to-caller false})
                    "\n")))' \
  "$receipt" "$case_id" "$parent" "$destination" "$workspace_commit" \
  "$capsule_hash" "$task_hash"

printf 'materialized\t%s\t%s\t%s\n' "$case_id" "$destination" "$receipt"

