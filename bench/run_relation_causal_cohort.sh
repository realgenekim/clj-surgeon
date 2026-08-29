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
model='gpt-5.6-sol'
reasoning='high'

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

canonical_file_path() {
  local path=$1
  command -v realpath >/dev/null 2>&1 \
    || die "realpath is required to freeze executable identity"
  realpath "$path"
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
  local wanted=$1 item index
  for ((index=0; index<manifest_count; index++)); do
    item=${manifest_paths[$index]}
    [ "$item" = "$wanted" ] && return 0
  done
  return 1
}

write_receipt() {
  local exit_code=$1 state=failed tmp
  if [ "$exit_code" -eq 0 ] && [ "${coordinator_complete:-false}" = true ]; then
    state=complete
  fi
  [ -n "${result_root_ready:-}" ] || return 0
  tmp="$result_root/.coordinator-receipt.edn.tmp.$$"
  printf '{:schema :clj-surgeon.edit-025-coordinator/v1 :state :%s :stage :%s :exit %s :head "%s" :tree "%s" :model "%s" :reasoning "%s" :block_1 "%s" :block_2 "%s"}\n' \
    "$state" "$stage" "$exit_code" "$actual_head" "$actual_tree" \
    "$model" "$reasoning" "$block1_matrix" "$block2_matrix" > "$tmp"
  mv "$tmp" "$result_root/coordinator-receipt.edn"
}

cleanup() {
  local exit_code=$1
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
coordinator_complete=false
trap 'cleanup "$?"' EXIT
trap 'exit 130' INT
trap 'exit 143' TERM

expected_head=${BENCH_RELATION_EXPECTED_HEAD-}
[ -n "$expected_head" ] || die "BENCH_RELATION_EXPECTED_HEAD is required"
expected_tree=${BENCH_RELATION_EXPECTED_TREE-}
[ -n "$expected_tree" ] || die "BENCH_RELATION_EXPECTED_TREE is required"
manifest=${BENCH_RELATION_ARTIFACT_MANIFEST-}
[ -n "$manifest" ] || die "BENCH_RELATION_ARTIFACT_MANIFEST is required"
expected_manifest_sha=${BENCH_RELATION_EXPECTED_MANIFEST_SHA256-}
[ -n "$expected_manifest_sha" ] || die "BENCH_RELATION_EXPECTED_MANIFEST_SHA256 is required"
expected_codex_executable=${BENCH_RELATION_EXPECTED_CODEX_EXECUTABLE-}
[ -n "$expected_codex_executable" ] || die "BENCH_RELATION_EXPECTED_CODEX_EXECUTABLE is required"
expected_codex_sha256=${BENCH_RELATION_EXPECTED_CODEX_SHA256-}
[ -n "$expected_codex_sha256" ] || die "BENCH_RELATION_EXPECTED_CODEX_SHA256 is required"
expected_codex_version=${BENCH_RELATION_EXPECTED_CODEX_VERSION-}
[ -n "$expected_codex_version" ] || die "BENCH_RELATION_EXPECTED_CODEX_VERSION is required"
expected_platform_os=${BENCH_RELATION_EXPECTED_PLATFORM_OS-}
[ -n "$expected_platform_os" ] || die "BENCH_RELATION_EXPECTED_PLATFORM_OS is required"
expected_platform_arch=${BENCH_RELATION_EXPECTED_PLATFORM_ARCH-}
[ -n "$expected_platform_arch" ] || die "BENCH_RELATION_EXPECTED_PLATFORM_ARCH is required"
expected_codex_package_sha256=${BENCH_RELATION_EXPECTED_CODEX_PACKAGE_SHA256-}
[ -n "$expected_codex_package_sha256" ] || die "BENCH_RELATION_EXPECTED_CODEX_PACKAGE_SHA256 is required"
expected_codex_platform_package_sha256=${BENCH_RELATION_EXPECTED_CODEX_PLATFORM_PACKAGE_SHA256-}
[ -n "$expected_codex_platform_package_sha256" ] || die "BENCH_RELATION_EXPECTED_CODEX_PLATFORM_PACKAGE_SHA256 is required"
expected_codex_native_executable=${BENCH_RELATION_EXPECTED_CODEX_NATIVE_EXECUTABLE-}
[ -n "$expected_codex_native_executable" ] || die "BENCH_RELATION_EXPECTED_CODEX_NATIVE_EXECUTABLE is required"
expected_codex_native_sha256=${BENCH_RELATION_EXPECTED_CODEX_NATIVE_SHA256-}
[ -n "$expected_codex_native_sha256" ] || die "BENCH_RELATION_EXPECTED_CODEX_NATIVE_SHA256 is required"
expected_node_executable=${BENCH_RELATION_EXPECTED_NODE_EXECUTABLE-}
[ -n "$expected_node_executable" ] || die "BENCH_RELATION_EXPECTED_NODE_EXECUTABLE is required"
expected_node_sha256=${BENCH_RELATION_EXPECTED_NODE_SHA256-}
[ -n "$expected_node_sha256" ] || die "BENCH_RELATION_EXPECTED_NODE_SHA256 is required"
expected_node_version=${BENCH_RELATION_EXPECTED_NODE_VERSION-}
[ -n "$expected_node_version" ] || die "BENCH_RELATION_EXPECTED_NODE_VERSION is required"
result_root=${BENCH_RELATION_RESULT_DIR-}
[ -n "$result_root" ] || die "BENCH_RELATION_RESULT_DIR is required"
worker=${BENCH_RELATION_WORKER:-$repo_root/bench/run_clean_codex.sh}
scorer=${BENCH_RELATION_SCORER:-$repo_root/bench/relation_causal_score.clj}
scorer_launcher=${BENCH_RELATION_SCORER_LAUNCHER:-bb}

[[ "$expected_head" =~ ^[0-9a-f]{40}$ ]] || die "Expected head must be one full lowercase Git commit hash"
[[ "$expected_tree" =~ ^[0-9a-f]{40}$ ]] || die "Expected tree must be one full lowercase Git tree hash"
[[ "$expected_manifest_sha" =~ ^[0-9a-f]{64}$ ]] || die "Expected manifest SHA must be one lowercase SHA-256"
case "$expected_codex_executable" in
  /*) ;;
  *) die "Expected Codex executable must be an absolute canonical path" ;;
esac
case "$expected_codex_native_executable" in
  /*) ;;
  *) die "Expected Codex native executable must be an absolute canonical path" ;;
esac
case "$expected_node_executable" in
  /*) ;;
  *) die "Expected Node executable must be an absolute canonical path" ;;
esac
[[ "$expected_codex_sha256" =~ ^[0-9a-f]{64}$ ]] \
  || die "Expected Codex SHA must be one lowercase SHA-256"
[ -n "$expected_codex_version" ] || die "Expected Codex version cannot be empty"
[ -n "$expected_platform_os" ] || die "Expected platform OS cannot be empty"
[ -n "$expected_platform_arch" ] || die "Expected platform architecture cannot be empty"
[[ "$expected_codex_package_sha256" =~ ^[0-9a-f]{64}$ ]] \
  || die "Expected Codex package SHA must be one lowercase SHA-256"
[[ "$expected_codex_platform_package_sha256" =~ ^[0-9a-f]{64}$ ]] \
  || die "Expected Codex platform package SHA must be one lowercase SHA-256"
[[ "$expected_codex_native_sha256" =~ ^[0-9a-f]{64}$ ]] \
  || die "Expected Codex native SHA must be one lowercase SHA-256"
[[ "$expected_node_sha256" =~ ^[0-9a-f]{64}$ ]] \
  || die "Expected Node SHA must be one lowercase SHA-256"
[ -n "$expected_node_version" ] || die "Expected Node version cannot be empty"
[ -f "$manifest" ] || die "Artifact manifest does not exist: $manifest"

require_exact_setting BENCH_RUN_MATRIX "$block1_matrix"
require_exact_setting BENCH_TASKS "$task"
require_exact_setting BENCH_REPLICATES 1
require_exact_setting BENCH_PARALLELISM 1
require_exact_setting BENCH_MCP_TOOL_PROFILE apply
require_exact_setting BENCH_MODEL "$model"
require_exact_setting BENCH_REASONING "$reasoning"

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
manifest_count=0
approved_manifest_count=39
approved_manifest_path_set_sha=bcf008d42930236816053f5e7cdf6dd40f5bd91d24ce5352313d85cd9b1546c8
while read -r artifact_sha artifact_path extra; do
  [ -n "${artifact_sha:-}" ] || continue
  [ -z "${extra:-}" ] || die "Artifact manifest rows must contain exactly SHA-256 and path"
  [[ "$artifact_sha" =~ ^[0-9a-f]{64}$ ]] \
    || die "Artifact manifest contains an invalid SHA-256"
  case "$artifact_path" in
    ''|/*|..|../*|*/..|*/../*) die "Artifact manifest path escapes the repository: $artifact_path" ;;
  esac
  for ((index=0; index<manifest_count; index++)); do
    [ "${manifest_paths[$index]}" != "$artifact_path" ] \
      || die "Artifact manifest repeats path: $artifact_path"
  done
  [ -f "$repo_root/$artifact_path" ] \
    || die "Artifact manifest path is not a file: $artifact_path"
  manifest_paths[$manifest_count]=$artifact_path
  manifest_hashes[$manifest_count]=$artifact_sha
  manifest_count=$((manifest_count + 1))
done < "$manifest"
[ "$manifest_count" -gt 0 ] || die "Artifact manifest is empty"
[ "$manifest_count" -eq "$approved_manifest_count" ] \
  || die "Artifact manifest must contain exactly $approved_manifest_count paths"
actual_manifest_path_set_sha=$(
  for ((index=0; index<manifest_count; index++)); do
    printf '%s\n' "${manifest_paths[$index]}"
  done | LC_ALL=C sort | shasum -a 256 | awk '{print $1}'
)
[ "$actual_manifest_path_set_sha" = "$approved_manifest_path_set_sha" ] \
  || die "Artifact manifest path set differs from the approved cohort"

worker_rel=$(repo_relative_path "$worker")
scorer_rel=$(repo_relative_path "$scorer")
script_rel=$(repo_relative_path "$script_path")
exact_profile_rel='bench/fixtures/edit_portfolio/submission-row-extraction-cleanup/exact-profile.edn'
fixture_paths=(
  "bench/fixtures/edit_portfolio/$task/capsule.edn"
  "bench/fixtures/edit_portfolio/$task/exact-profile.edn"
  "bench/fixtures/edit_portfolio/$task/task.txt"
  "bench/fixtures/edit_portfolio/$task/before/src/sample/review_updates.clj"
  "bench/fixtures/edit_portfolio/$task/before/src/sample/views/log.clj"
  "bench/fixtures/edit_portfolio/$task/before/src/sample/views/people.clj"
  "bench/fixtures/edit_portfolio/$task/before/src/sample/views/review.clj"
  "bench/fixtures/edit_portfolio/$task/before/test/sample/board_test.clj"
  "bench/fixtures/edit_portfolio/$task/before/test/sample/reviews_test.clj"
  "bench/fixtures/edit_portfolio/$task/before/test/sample/status_workflow_test.clj"
  "bench/fixtures/edit_portfolio/$task/before/test/sample/views_test.clj"
  "bench/fixtures/edit_portfolio/$task/before/test/sample/voting_policy_test.clj"
  "bench/fixtures/edit_portfolio/$task/after/src/sample/review_updates.clj"
  "bench/fixtures/edit_portfolio/$task/after/src/sample/views/log.clj"
  "bench/fixtures/edit_portfolio/$task/after/src/sample/views/people.clj"
  "bench/fixtures/edit_portfolio/$task/after/src/sample/views/review.clj"
  "bench/fixtures/edit_portfolio/$task/after/test/sample/board_test.clj"
  "bench/fixtures/edit_portfolio/$task/after/test/sample/reviews_test.clj"
  "bench/fixtures/edit_portfolio/$task/after/test/sample/status_workflow_test.clj"
  "bench/fixtures/edit_portfolio/$task/after/test/sample/views_test.clj"
  "bench/fixtures/edit_portfolio/$task/after/test/sample/voting_policy_test.clj"
)
for required in "$script_rel" "$worker_rel" "$scorer_rel" \
  deps.edn bb.edn "$exact_profile_rel"; do
  manifest_contains "$required" || die "Artifact manifest omits required path: $required"
done
for fixture_file in "${fixture_paths[@]}"; do
  manifest_contains "$fixture_file" \
    || die "Artifact manifest omits fixture path: $fixture_file"
done

assert_candidate_identity() {
  local current_head current_tree current_manifest_sha dirty runtime_dirty
  local index artifact_path artifact_sha actual_sha
  current_head=$(git -C "$repo_root" rev-parse 'HEAD^{commit}')
  current_tree=$(git -C "$repo_root" rev-parse 'HEAD^{tree}')
  [ "$current_head" = "$expected_head" ] \
    || die "Candidate HEAD mismatch: expected $expected_head, got $current_head"
  [ "$current_tree" = "$expected_tree" ] \
    || die "Candidate tree mismatch: expected $expected_tree, got $current_tree"
  current_manifest_sha=$(shasum -a 256 "$manifest" | awk '{print $1}')
  [ "$current_manifest_sha" = "$expected_manifest_sha" ] \
    || die "Artifact manifest SHA mismatch"
  dirty=$(git -C "$repo_root" status --porcelain=v1 --untracked-files=all -- "${manifest_paths[@]}")
  [ -z "$dirty" ] || die "Candidate has dirty cohort artifacts: $dirty"
  runtime_dirty=$(git -C "$repo_root" status --porcelain=v1 --untracked-files=all)
  [ -z "$runtime_dirty" ] \
    || die "Candidate has dirty runtime closure: $runtime_dirty"
  for ((index=0; index<manifest_count; index++)); do
    artifact_path=${manifest_paths[$index]}
    artifact_sha=${manifest_hashes[$index]}
    actual_sha=$(shasum -a 256 "$repo_root/$artifact_path" | awk '{print $1}')
    [ "$actual_sha" = "$artifact_sha" ] \
      || die "Artifact SHA mismatch: $artifact_path"
  done
  actual_head=$current_head
  actual_tree=$current_tree
}

assert_candidate_identity

assert_codex_identity() {
  local entry executable sha version package_root package_file package_sha
  local platform_package_name platform_target platform_package_file platform_package_sha
  local native_executable native_sha node_entry actual_node_executable actual_node_sha
  local actual_node_version actual_platform_os actual_platform_arch
  entry=$(command -v codex) || die "Codex executable is unavailable"
  executable=$(canonical_file_path "$entry")
  sha=$(shasum -a 256 "$executable" | awk '{print $1}')
  version=$("$executable" --version)
  package_root=$(dirname "$(dirname "$executable")")
  package_file="$package_root/package.json"
  [ -f "$package_file" ] || die "Codex package identity is unavailable: $package_file"
  package_sha=$(shasum -a 256 "$package_file" | awk '{print $1}')
  actual_platform_os=$(uname -s)
  actual_platform_arch=$(uname -m)
  case "$actual_platform_os:$actual_platform_arch" in
    Darwin:arm64)
      platform_package_name=codex-darwin-arm64
      platform_target=aarch64-apple-darwin
      ;;
    Darwin:x86_64)
      platform_package_name=codex-darwin-x64
      platform_target=x86_64-apple-darwin
      ;;
    Linux:aarch64|Linux:arm64)
      platform_package_name=codex-linux-arm64
      platform_target=aarch64-unknown-linux-musl
      ;;
    Linux:x86_64)
      platform_package_name=codex-linux-x64
      platform_target=x86_64-unknown-linux-musl
      ;;
    *) die "Unsupported Codex platform: $actual_platform_os/$actual_platform_arch" ;;
  esac
  platform_package_file="$package_root/node_modules/@openai/$platform_package_name/package.json"
  [ -f "$platform_package_file" ] \
    || die "Codex platform package identity is unavailable: $platform_package_file"
  platform_package_sha=$(shasum -a 256 "$platform_package_file" | awk '{print $1}')
  native_executable=$(canonical_file_path \
    "$package_root/node_modules/@openai/$platform_package_name/vendor/$platform_target/bin/codex")
  native_sha=$(shasum -a 256 "$native_executable" | awk '{print $1}')
  node_entry=$(command -v node) || die "Node executable is unavailable"
  actual_node_executable=$(canonical_file_path "$node_entry")
  actual_node_sha=$(shasum -a 256 "$actual_node_executable" | awk '{print $1}')
  actual_node_version=$("$actual_node_executable" --version)
  [ "$executable" = "$expected_codex_executable" ] \
    || die "Codex executable identity mismatch: expected $expected_codex_executable, got $executable"
  [ "$sha" = "$expected_codex_sha256" ] \
    || die "Codex SHA mismatch: expected $expected_codex_sha256, got $sha"
  [ "$version" = "$expected_codex_version" ] \
    || die "Codex version mismatch: expected $expected_codex_version, got $version"
  [ "$package_sha" = "$expected_codex_package_sha256" ] \
    || die "Codex package SHA mismatch: expected $expected_codex_package_sha256, got $package_sha"
  [ "$platform_package_sha" = "$expected_codex_platform_package_sha256" ] \
    || die "Codex platform package SHA mismatch: expected $expected_codex_platform_package_sha256, got $platform_package_sha"
  [ "$native_executable" = "$expected_codex_native_executable" ] \
    || die "Codex native executable identity mismatch"
  [ "$native_sha" = "$expected_codex_native_sha256" ] \
    || die "Codex native SHA mismatch: expected $expected_codex_native_sha256, got $native_sha"
  [ "$actual_node_executable" = "$expected_node_executable" ] \
    || die "Node executable identity mismatch: expected $expected_node_executable, got $actual_node_executable"
  [ "$actual_node_sha" = "$expected_node_sha256" ] \
    || die "Node SHA mismatch: expected $expected_node_sha256, got $actual_node_sha"
  [ "$actual_node_version" = "$expected_node_version" ] \
    || die "Node version mismatch: expected $expected_node_version, got $actual_node_version"
  [ "$actual_platform_os" = "$expected_platform_os" ] \
    || die "Platform OS mismatch: expected $expected_platform_os, got $actual_platform_os"
  [ "$actual_platform_arch" = "$expected_platform_arch" ] \
    || die "Platform architecture mismatch: expected $expected_platform_arch, got $actual_platform_arch"
  codex_executable=$executable
  codex_sha256=$sha
  codex_version=$version
  codex_package_sha256=$package_sha
  codex_platform_package_sha256=$platform_package_sha
  codex_native_executable=$native_executable
  codex_native_sha256=$native_sha
  node_executable=$actual_node_executable
  node_sha256=$actual_node_sha
  node_version=$actual_node_version
  platform_os=$actual_platform_os
  platform_arch=$actual_platform_arch
}

assert_codex_identity
environment_receipt="$result_root/environment.edn"
bb -e '
  (let [[output model reasoning executable executable-sha version
         package-sha platform-package-sha native-executable native-sha node-executable node-sha
         node-version platform-os platform-arch]
        *command-line-args*]
    (spit output
          (str
            (pr-str
              {:schema :clj-surgeon.edit-025-environment/v1
               :model model
               :reasoning reasoning
               :codex-executable executable
               :codex-sha256 executable-sha
               :codex-version version
               :codex-package-sha256 package-sha
               :codex-platform-package-sha256 platform-package-sha
               :codex-native-executable native-executable
               :codex-native-sha256 native-sha
               :node-executable node-executable
               :node-sha256 node-sha
               :node-version node-version
               :platform-os platform-os
               :platform-arch platform-arch})
            "\\n")))' \
  "$environment_receipt" "$model" "$reasoning" "$codex_executable" \
  "$codex_sha256" "$codex_version" "$codex_package_sha256" \
  "$codex_platform_package_sha256" "$codex_native_executable" \
  "$codex_native_sha256" "$node_executable" \
  "$node_sha256" "$node_version" "$platform_os" "$platform_arch"

[ -f "$repo_root/$worker_rel" ] || die "Worker is not a file: $worker_rel"
[ -f "$repo_root/$scorer_rel" ] || die "Scorer is not a file: $scorer_rel"
command -v "$scorer_launcher" >/dev/null 2>&1 \
  || die "Scorer launcher is unavailable: $scorer_launcher"

run_block() {
  local name=$1 matrix=$2 destination
  assert_candidate_identity
  assert_codex_identity
  destination="$result_root/$name"
  BENCH_RESULT_DIR="$destination" \
  BENCH_TASKS="$task" \
  BENCH_RUN_MATRIX="$matrix" \
  BENCH_REPLICATES=1 \
  BENCH_PARALLELISM=1 \
  BENCH_MCP_TOOL_PROFILE=apply \
  BENCH_MODEL="$model" \
  BENCH_REASONING="$reasoning" \
  BENCH_RETENTION=local \
    bash "$repo_root/$worker_rel"
  assert_candidate_identity
  assert_codex_identity
}

run_scorer() {
  if [ "$scorer_launcher" = bb ]; then
    (
      cd "$repo_root"
      bb -cp src:test:bench "$repo_root/$scorer_rel" "$@"
    )
  else
    "$scorer_launcher" "$repo_root/$scorer_rel" "$@"
  fi
}

validate_raw_run() {
  local run_dir=$1
  [ -s "$run_dir/events.jsonl" ] \
    || die "Run is missing raw events: $run_dir" 3
  [ -s "$run_dir/event-clock.tsv" ] \
    || die "Run is missing observer clocks: $run_dir" 3
  [ -s "$run_dir/terminal.tsv" ] \
    || die "Run is missing its terminal receipt: $run_dir" 3
  [ -s "$run_dir/prompt.txt" ] \
    || die "Run is missing its exact prompt: $run_dir" 3
  [ -s "$run_dir/codex-mcp-registry.json" ] \
    || die "Run is missing its Codex client registry: $run_dir" 3
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
  event_workspace_root=$(jq -er -s '
    [.[] | select(.type == "item.started"
                  and .item.type == "mcp_tool_call"
                  and .item.server == "clj-surgeon"
                  and .item.tool == "apply_clojure_changes")]
    | first.item.arguments.workspace_root' "$run_dir/events.jsonl") \
    || die "Run apply event omits its workspace identity: $run_dir" 3
  [ "$event_workspace_root" = "$validated_workspace_root" ] \
    || die "Run workspace receipt differs from apply arguments: $run_dir" 3
  local workspace_index seen_workspace
  for ((workspace_index=0; workspace_index<cohort_workspace_root_count; workspace_index++)); do
    seen_workspace=${cohort_workspace_roots[$workspace_index]}
    [ "$seen_workspace" != "$validated_workspace_root" ] \
      || die "Run workspace identity was reused: $validated_workspace_root" 3
  done
  cohort_workspace_roots[$cohort_workspace_root_count]=$validated_workspace_root
  cohort_workspace_root_count=$((cohort_workspace_root_count + 1))
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
  BENCH_RELATION_ENVIRONMENT_RECEIPT="$environment_receipt" \
    bb -e '
        (let [[block output & fields] *command-line-args*
              environment (System/getenv
                            "BENCH_RELATION_ENVIRONMENT_RECEIPT")
              runs (mapv (fn [[run-id arm workspace-root run-dir] position]
                           {:run-id run-id
                            :block (parse-long block)
                            :position position
                            :arm (keyword arm)
                            :workspace-root workspace-root
                            :artifacts
                            {:events (str run-dir "/events.jsonl")
                             :event-clock (str run-dir "/event-clock.tsv")
                             :terminal (str run-dir "/terminal.tsv")
                             :prompt (str run-dir "/prompt.txt")
                             :client-registry
                             (str run-dir "/codex-mcp-registry.json")
                             :environment environment}})
                         (partition 4 fields)
                         (range 1 5))]
          (spit output
                (str (pr-str
                       {:schema :clj-surgeon.edit-025-run-manifest/v1
                        :block (parse-long block)
                        :runs runs})
                     "\\n")))' \
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
cohort_workspace_root_count=0
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
coordinator_complete=true
printf 'Relation causal cohort complete: %s\n' "$result_root"
