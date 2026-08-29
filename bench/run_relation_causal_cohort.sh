#!/usr/bin/env bash
set -euo pipefail

# Thin EDIT-025 coordinator. run_clean_codex.sh owns every model run and
# relation_causal_score.clj owns every causal decision. This file owns only
# immutable admission, the two fixed serial blocks, and stop/continue wiring.

script_path=$(cd "$(dirname "$0")" && pwd -P)/$(basename "$0")
repo_root=$(cd "$(dirname "$script_path")/.." && pwd -P)

block1_matrix='mcp:mcp-relation-n-no-skill mcp:mcp-relation-r-no-skill mcp:mcp-relation-r-no-skill mcp:mcp-relation-n-no-skill'
block2_matrix='mcp:mcp-relation-r-no-skill mcp:mcp-relation-n-no-skill mcp:mcp-relation-n-no-skill mcp:mcp-relation-r-no-skill'
task='submission-row-extraction-cleanup'

die() {
  printf '%s\n' "$1" >&2
  exit "${2:-2}"
}

require_exact_setting() {
  local name=$1 expected=$2 actual
  actual=$(printenv "$name" 2>/dev/null || true)
  if [ -n "$actual" ] && [ "$actual" != "$expected" ]; then
    die "$name must be exactly: $expected"
  fi
}

repo_relative_path() {
  local path=$1 absolute
  case "$path" in
    /*) absolute=$path ;;
    *) absolute="$repo_root/$path" ;;
  esac
  if [ -e "$absolute" ]; then
    absolute=$(cd "$(dirname "$absolute")" && pwd -P)/$(basename "$absolute")
  fi
  case "$absolute" in
    "$repo_root"/*) printf '%s\n' "${absolute#"$repo_root"/}" ;;
    *) die "Cohort executable must be inside the candidate repository: $path" ;;
  esac
}

manifest_contains() {
  local wanted=$1 item
  for item in "${manifest_paths[@]}"; do
    [ "$item" = "$wanted" ] && return 0
  done
  return 1
}

write_receipt() {
  local exit_code=$1 state=failed tmp
  [ "$exit_code" -eq 0 ] && state=complete
  [ -n "${result_root_ready:-}" ] || return 0
  tmp="$result_root/.coordinator-receipt.edn.tmp.$$"
  printf '{:schema :clj-surgeon.edit-025-coordinator/v1 :state :%s :stage :%s :exit %s :head "%s" :tree "%s" :block_1 "%s" :block_2 "%s"}\n' \
    "$state" "$stage" "$exit_code" "$actual_head" "$actual_tree" \
    "$block1_matrix" "$block2_matrix" > "$tmp"
  mv "$tmp" "$result_root/coordinator-receipt.edn"
}

cleanup() {
  local exit_code=$?
  trap - EXIT INT TERM
  write_receipt "$exit_code" || true
  if [ -n "${owner_dir:-}" ] && [ -d "$owner_dir" ]; then
    rm -f "$owner_dir/owner.tsv"
    rmdir "$owner_dir" 2>/dev/null || true
  fi
  exit "$exit_code"
}

stage=admission
result_root_ready=
actual_head=
actual_tree=
owner_dir=
trap cleanup EXIT
trap 'exit 130' INT
trap 'exit 143' TERM

expected_head=${BENCH_RELATION_EXPECTED_HEAD:?BENCH_RELATION_EXPECTED_HEAD is required}
expected_tree=${BENCH_RELATION_EXPECTED_TREE:?BENCH_RELATION_EXPECTED_TREE is required}
manifest=${BENCH_RELATION_ARTIFACT_MANIFEST:?BENCH_RELATION_ARTIFACT_MANIFEST is required}
expected_manifest_sha=${BENCH_RELATION_EXPECTED_MANIFEST_SHA256:?BENCH_RELATION_EXPECTED_MANIFEST_SHA256 is required}
result_root=${BENCH_RELATION_RESULT_DIR:?BENCH_RELATION_RESULT_DIR is required}
worker=${BENCH_RELATION_WORKER:-$repo_root/bench/run_clean_codex.sh}
scorer=${BENCH_RELATION_SCORER:-$repo_root/bench/relation_causal_score.clj}
scorer_launcher=${BENCH_RELATION_SCORER_LAUNCHER:-bb}

[[ "$expected_head" =~ ^[0-9a-f]{40}$ ]] || die "Expected head must be one full lowercase Git commit hash"
[[ "$expected_tree" =~ ^[0-9a-f]{40}$ ]] || die "Expected tree must be one full lowercase Git tree hash"
[[ "$expected_manifest_sha" =~ ^[0-9a-f]{64}$ ]] || die "Expected manifest SHA must be one lowercase SHA-256"
[ -f "$manifest" ] || die "Artifact manifest does not exist: $manifest"

require_exact_setting BENCH_RUN_MATRIX "$block1_matrix"
require_exact_setting BENCH_TASKS "$task"
require_exact_setting BENCH_REPLICATES 1
require_exact_setting BENCH_PARALLELISM 1
require_exact_setting BENCH_MCP_TOOL_PROFILE apply

case "$result_root" in
  /*) ;;
  *) die "BENCH_RELATION_RESULT_DIR must be an absolute path" ;;
esac
[ "$result_root" != / ] || die "BENCH_RELATION_RESULT_DIR cannot be /"
[ "$result_root" != "$repo_root" ] || die "BENCH_RELATION_RESULT_DIR cannot be the repository root"
[ ! -L "$result_root" ] || die "BENCH_RELATION_RESULT_DIR cannot be a symlink"
if [ -e "$result_root" ]; then
  [ -d "$result_root" ] || die "BENCH_RELATION_RESULT_DIR must be a directory"
  [ -z "$(find "$result_root" -mindepth 1 -maxdepth 1 -print -quit)" ] \
    || die "BENCH_RELATION_RESULT_DIR must be empty"
else
  mkdir "$result_root"
fi
result_root=$(cd "$result_root" && pwd -P)
result_root_ready=true
owner_dir="$result_root/.coordinator-owner"
mkdir "$owner_dir" || die "Could not acquire exclusive coordinator ownership"
printf 'pid\t%s\nstarted_utc\t%s\n' "$$" "$(date -u +%Y-%m-%dT%H:%M:%SZ)" \
  > "$owner_dir/owner.tsv"

actual_head=$(git -C "$repo_root" rev-parse 'HEAD^{commit}')
actual_tree=$(git -C "$repo_root" rev-parse 'HEAD^{tree}')
[ "$actual_head" = "$expected_head" ] \
  || die "Candidate HEAD mismatch: expected $expected_head, got $actual_head"
[ "$actual_tree" = "$expected_tree" ] \
  || die "Candidate tree mismatch: expected $expected_tree, got $actual_tree"

actual_manifest_sha=$(shasum -a 256 "$manifest" | awk '{print $1}')
[ "$actual_manifest_sha" = "$expected_manifest_sha" ] \
  || die "Artifact manifest SHA mismatch"

manifest_paths=()
manifest_hashes=()
while read -r artifact_sha artifact_path extra; do
  [ -n "${artifact_sha:-}" ] || continue
  [ -z "${extra:-}" ] || die "Artifact manifest rows must contain exactly SHA-256 and path"
  [[ "$artifact_sha" =~ ^[0-9a-f]{64}$ ]] \
    || die "Artifact manifest contains an invalid SHA-256"
  case "$artifact_path" in
    ''|/*|..|../*|*/..|*/../*) die "Artifact manifest path escapes the repository: $artifact_path" ;;
  esac
  for seen in "${manifest_paths[@]}"; do
    [ "$seen" != "$artifact_path" ] || die "Artifact manifest repeats path: $artifact_path"
  done
  [ -f "$repo_root/$artifact_path" ] \
    || die "Artifact manifest path is not a file: $artifact_path"
  manifest_paths+=("$artifact_path")
  manifest_hashes+=("$artifact_sha")
done < "$manifest"
[ "${#manifest_paths[@]}" -gt 0 ] || die "Artifact manifest is empty"

worker_rel=$(repo_relative_path "$worker")
scorer_rel=$(repo_relative_path "$scorer")
script_rel=$(repo_relative_path "$script_path")
exact_profile_rel='bench/fixtures/edit_portfolio/submission-row-extraction-cleanup/exact-profile.edn'
for required in "$script_rel" "$worker_rel" "$scorer_rel" "$exact_profile_rel"; do
  manifest_contains "$required" || die "Artifact manifest omits required path: $required"
done

dirty=$(git -C "$repo_root" status --porcelain=v1 --untracked-files=all -- "${manifest_paths[@]}")
[ -z "$dirty" ] || die "Candidate has dirty cohort artifacts: $dirty"

for index in "${!manifest_paths[@]}"; do
  artifact_path=${manifest_paths[$index]}
  artifact_sha=${manifest_hashes[$index]}
  actual_sha=$(shasum -a 256 "$repo_root/$artifact_path" | awk '{print $1}')
  [ "$actual_sha" = "$artifact_sha" ] \
    || die "Artifact SHA mismatch: $artifact_path"
done

[ -f "$repo_root/$worker_rel" ] || die "Worker is not a file: $worker_rel"
[ -f "$repo_root/$scorer_rel" ] || die "Scorer is not a file: $scorer_rel"
command -v "$scorer_launcher" >/dev/null 2>&1 \
  || die "Scorer launcher is unavailable: $scorer_launcher"

run_block() {
  local name=$1 matrix=$2 destination
  destination="$result_root/$name"
  BENCH_RESULT_DIR="$destination" \
  BENCH_TASKS="$task" \
  BENCH_RUN_MATRIX="$matrix" \
  BENCH_REPLICATES=1 \
  BENCH_PARALLELISM=1 \
  BENCH_MCP_TOOL_PROFILE=apply \
  BENCH_RETENTION=local \
    bash "$repo_root/$worker_rel"
}

run_scorer() {
  "$scorer_launcher" "$repo_root/$scorer_rel" "$@"
}

validate_raw_run() {
  local run_dir=$1
  [ -s "$run_dir/events.jsonl" ] \
    || die "Run is missing raw events: $run_dir" 3
  [ -s "$run_dir/event-clock.tsv" ] \
    || die "Run is missing observer clocks: $run_dir" 3
  [ -s "$run_dir/terminal.tsv" ] \
    || die "Run is missing its terminal receipt: $run_dir" 3
  [ -s "$run_dir/workspace-root.txt" ] \
    || die "Run is missing its workspace identity: $run_dir" 3
  [ "$(awk -F '\t' '$1 == "state" {print $2}' "$run_dir/terminal.tsv")" = completed ] \
    || die "Run terminal state is not completed: $run_dir" 3
  [ "$(awk -F '\t' '$1 == "exit_code" {print $2}' "$run_dir/terminal.tsv")" = 0 ] \
    || die "Run terminal exit is not zero: $run_dir" 3
  jq -s -e '
    ([.[] | select(.type == "item.started"
                   and .item.type == "mcp_tool_call"
                   and .item.server == "clj-surgeon"
                   and .item.tool == "apply_clojure_changes")]
     | length) == 1
    and
    ([.[] | select(.type == "item.completed"
                   and .item.type == "mcp_tool_call"
                   and .item.server == "clj-surgeon"
                   and .item.tool == "apply_clojure_changes")
       | (.item.result.structured_content
          // .item.result.structuredContent
          // null)
       | select(type == "object")]
     | length) == 1' "$run_dir/events.jsonl" >/dev/null \
    || die "Run lacks one raw apply call and structured MCP receipt: $run_dir" 3
  [ "$(wc -l < "$run_dir/workspace-root.txt" | tr -d ' ')" -eq 1 ] \
    || die "Run workspace identity must contain exactly one line: $run_dir" 3
  validated_workspace_root=$(cat "$run_dir/workspace-root.txt")
  case "$validated_workspace_root" in
    ''|/|*'//'|*/.|*/..|*/./*|*/../*|*/) \
      die "Run workspace identity is not canonical: $run_dir" 3 ;;
    /*) ;;
    *) die "Run workspace identity is not absolute: $run_dir" 3 ;;
  esac
  [ -d "$validated_workspace_root" ] \
    || die "Run workspace identity does not name a directory: $run_dir" 3
  canonical_workspace=$(cd "$validated_workspace_root" && pwd -P)
  [ "$canonical_workspace" = "$validated_workspace_root" ] \
    || die "Run workspace identity is not canonical: $run_dir" 3
  for seen_workspace in "${cohort_workspace_roots[@]}"; do
    [ "$seen_workspace" != "$validated_workspace_root" ] \
      || die "Run workspace identity was reused: $validated_workspace_root" 3
  done
  cohort_workspace_roots+=("$validated_workspace_root")
}

write_block_manifest() {
  local block=$1 output=$2 block_dir=$3
  local -a ids arms contexts scorer_args
  if [ "$block" -eq 1 ]; then
    ids=(b1-n1 b1-r1 b1-r2 b1-n2)
    arms=(N R R N)
    contexts=(mcp-relation-n-no-skill mcp-relation-r-no-skill \
              mcp-relation-r-no-skill mcp-relation-n-no-skill)
  else
    ids=(b2-r1 b2-n1 b2-n2 b2-r2)
    arms=(R N N R)
    contexts=(mcp-relation-r-no-skill mcp-relation-n-no-skill \
              mcp-relation-n-no-skill mcp-relation-r-no-skill)
  fi
  scorer_args=()
  local index position run_dir
  for index in 0 1 2 3; do
    position=$((index + 1))
    run_dir=$(printf '%s/%02d-r01-%s-%s-mcp' \
      "$block_dir" "$position" "$task" "${contexts[$index]}")
    validate_raw_run "$run_dir"
    scorer_args+=("${ids[$index]}" "${arms[$index]}" \
      "$validated_workspace_root" "$run_dir")
  done
  bb -e '
    (let [[block output & fields] *command-line-args*
          runs (mapv (fn [[run-id arm workspace-root run-dir] position]
                       {:run-id run-id
                        :block (parse-long block)
                        :position position
                        :arm (keyword arm)
                        :workspace-root workspace-root
                        :artifacts
                        {:events (str run-dir "/events.jsonl")
                         :event-clock (str run-dir "/event-clock.tsv")
                         :terminal (str run-dir "/terminal.tsv")}})
                     (partition 4 fields)
                     (range 1 5))]
      (spit output
            (str (pr-str {:schema :clj-surgeon.edit-025-run-manifest/v1
                          :block (parse-long block)
                          :runs runs}) "\n")))' \
    "$block" "$output" "${scorer_args[@]}"
}

report_authorizes_block2() {
  local report=$1
  bb -e '
    (require (quote [clojure.edn :as edn]))
    (let [report (edn/read-string (slurp (first *command-line-args*)))]
      (System/exit
       (if (and (= :clj-surgeon.edit-025-relation-causal-cohort/v1
                   (:schema report))
                (true? (:ok report))
                (= 4 (:run-count report))
                (true? (get-in report [:gate :block-2-authorized])))
         0 1)))' "$report"
}

report_is_complete() {
  local report=$1
  bb -e '
    (require (quote [clojure.edn :as edn]))
    (let [report (edn/read-string (slurp (first *command-line-args*)))]
      (System/exit
       (if (and (= :clj-surgeon.edit-025-relation-causal-cohort/v1
                   (:schema report))
                (true? (:ok report))
                (= 8 (:run-count report))
                (map? (:gate report))
                (contains? (:gate report) :promote))
         0 1)))' "$report"
}

cohort_workspace_roots=()
validated_workspace_root=
stage=block1
run_block block1 "$block1_matrix"

stage=block1-score
block1_manifest="$result_root/block1-run-manifest.edn"
write_block_manifest 1 "$block1_manifest" "$result_root/block1"
block1_report="$result_root/block1-score.edn"
run_scorer --phase block1 \
  --manifest "$block1_manifest" \
  --output "$block1_report"
[ -s "$block1_report" ] || die "Block-one scorer did not write its report" 3
report_authorizes_block2 "$block1_report" \
  || die "Block one did not authorize Block two" 3

stage=block2
run_block block2 "$block2_matrix"

stage=aggregate
block2_manifest="$result_root/block2-run-manifest.edn"
write_block_manifest 2 "$block2_manifest" "$result_root/block2"
final_report="$result_root/final-report.edn"
run_scorer --phase final \
  --block1-manifest "$block1_manifest" \
  --block2-manifest "$block2_manifest" \
  --output "$final_report"
[ -s "$final_report" ] || die "Final scorer did not write its aggregate" 3
report_is_complete "$final_report" || die "Final scorer report is incomplete" 3

stage=complete
printf 'Relation causal cohort complete: %s\n' "$result_root"
