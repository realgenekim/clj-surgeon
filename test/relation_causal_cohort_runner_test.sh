#!/usr/bin/env bash
set -euo pipefail

source_script=$(cd "$(dirname "$0")/.." && pwd -P)/bench/run_relation_causal_cohort.sh
source_repo=$(cd "$(dirname "$source_script")/.." && pwd -P)
test_root=$(mktemp -d /tmp/clj-surgeon-relation-coordinator-test.XXXXXX)
trap '[ "${KEEP_RELATION_TEST_TMP:-false}" = true ] || rm -rf "$test_root"' EXIT
mkdir -p "$test_root/codex-bin" "$test_root/codex-lib" "$test_root/node-bin"
test_platform_os=$(uname -s)
test_platform_arch=$(uname -m)
case "$test_platform_os:$test_platform_arch" in
  Darwin:arm64)
    test_platform_package_name=codex-darwin-arm64
    test_platform_target=aarch64-apple-darwin
    ;;
  Darwin:x86_64)
    test_platform_package_name=codex-darwin-x64
    test_platform_target=x86_64-apple-darwin
    ;;
  Linux:aarch64|Linux:arm64)
    test_platform_package_name=codex-linux-arm64
    test_platform_target=aarch64-unknown-linux-musl
    ;;
  Linux:x86_64)
    test_platform_package_name=codex-linux-x64
    test_platform_target=x86_64-unknown-linux-musl
    ;;
  *)
    printf 'FAIL: unsupported test platform: %s/%s\n' \
      "$test_platform_os" "$test_platform_arch" >&2
    exit 1
    ;;
esac
test_platform_package_root="$test_root/node_modules/@openai/$test_platform_package_name"
test_codex_native_executable="$test_platform_package_root/vendor/$test_platform_target/bin/codex"
mkdir -p "$(dirname "$test_codex_native_executable")"
cat > "$test_root/codex-lib/codex.js" <<'CODEX'
#!/usr/bin/env bash
if [ "${1:-}" = --version ]; then
  printf '%s\n' 'codex-cli test-1.0'
  exit 0
fi
exit 23
CODEX
chmod +x "$test_root/codex-lib/codex.js"
ln -s ../codex-lib/codex.js "$test_root/codex-bin/codex"
printf '{"name":"@openai/codex","version":"test-1.0"}\n' \
  > "$test_root/package.json"
printf '{"name":"@openai/%s","version":"test-1.0"}\n' \
  "$test_platform_package_name" > "$test_platform_package_root/package.json"
cat > "$test_codex_native_executable" <<'NATIVE'
#!/usr/bin/env bash
exit 0
NATIVE
cat > "$test_root/node-bin/node" <<'NODE'
#!/usr/bin/env bash
if [ "${1:-}" = --version ]; then
  printf '%s\n' 'vtest-1.0'
  exit 0
fi
exit 24
NODE
chmod +x "$test_codex_native_executable" "$test_root/node-bin/node"
test_path="$test_root/codex-bin:$test_root/node-bin:$PATH"
test_codex_executable=$(realpath "$test_root/codex-bin/codex")
test_codex_sha256=$(shasum -a 256 "$test_codex_executable" | awk '{print $1}')
test_codex_version=$($test_codex_executable --version)
test_codex_package_sha256=$(shasum -a 256 "$test_root/package.json" | awk '{print $1}')
test_codex_platform_package_sha256=$(shasum -a 256 "$test_platform_package_root/package.json" | awk '{print $1}')
test_codex_native_executable=$(realpath "$test_codex_native_executable")
test_codex_native_sha256=$(shasum -a 256 "$test_codex_native_executable" | awk '{print $1}')
test_node_executable=$(realpath "$test_root/node-bin/node")
test_node_sha256=$(shasum -a 256 "$test_node_executable" | awk '{print $1}')
test_node_version=$($test_node_executable --version)

fail() {
  printf 'FAIL: %s\n' "$1" >&2
  exit 1
}

assert_contains() {
  local file=$1 expected=$2
  grep -F "$expected" "$file" >/dev/null \
    || fail "$file does not contain: $expected"
}

new_repo() {
  local name=$1 root
  root="$test_root/$name"
  mkdir -p "$root/bench/fixtures/edit_portfolio/submission-row-extraction-cleanup"
  cp "$source_script" "$root/bench/run_relation_causal_cohort.sh"
  printf '{}\n' > "$root/deps.edn"
  printf '{}\n' > "$root/bb.edn"
  printf '*.out\ncalls.log\nscorer.log\n' > "$root/.gitignore"
  mkdir -p "$root/src/clj_surgeon"
  printf '(ns clj-surgeon.mcp-change-buffer)\n' \
    > "$root/src/clj_surgeon/mcp_change_buffer.clj"
  printf '(ns capture-codex-mcp-registry)\n' \
    > "$root/bench/capture_codex_mcp_registry.clj"
  printf '{}\n' > "$root/bench/fixtures/edit_portfolio/submission-row-extraction-cleanup/capsule.edn"
  printf 'task\n' > "$root/bench/fixtures/edit_portfolio/submission-row-extraction-cleanup/task.txt"
  printf 'profile\n' > "$root/bench/fixtures/edit_portfolio/submission-row-extraction-cleanup/exact-profile.edn"
  local phase relative fixture_file
  for phase in before after; do
    for relative in \
      src/sample/review_updates.clj \
      src/sample/views/log.clj \
      src/sample/views/people.clj \
      src/sample/views/review.clj \
      test/sample/board_test.clj \
      test/sample/reviews_test.clj \
      test/sample/status_workflow_test.clj \
      test/sample/views_test.clj \
      test/sample/voting_policy_test.clj; do
      fixture_file="$root/bench/fixtures/edit_portfolio/submission-row-extraction-cleanup/$phase/$relative"
      mkdir -p "$(dirname "$fixture_file")"
      printf '%s %s\n' "$phase" "$relative" > "$fixture_file"
    done
  done
  while read -r _ approved_path; do
    if [ ! -e "$root/$approved_path" ]; then
      mkdir -p "$(dirname "$root/$approved_path")"
      printf 'approved placeholder: %s\n' "$approved_path" > "$root/$approved_path"
    fi
  done < "$source_repo/bench/relation_causal_artifacts.sha256"
  cat > "$root/bench/run_clean_codex.sh" <<'WORKER'
#!/usr/bin/env bash
set -euo pipefail
mkdir -p "$BENCH_RESULT_DIR"
printf '%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\n' \
  "$BENCH_RESULT_DIR" "$BENCH_RUN_MATRIX" "$BENCH_TASKS" \
  "$BENCH_REPLICATES" "$BENCH_PARALLELISM" "$BENCH_MCP_TOOL_PROFILE" \
  "$BENCH_MODEL" "$BENCH_REASONING" \
  >> "$FAKE_CALL_LOG"
printf 'run_id\tstate\nretained\tstarted\n' > "$BENCH_RESULT_DIR/runs.tsv"
if [ "${FAKE_FAIL_BLOCK:-}" = "$(basename "$BENCH_RESULT_DIR")" ]; then
  printf 'retained child failure\n' > "$BENCH_RESULT_DIR/child-failure.txt"
  exit 17
fi
position=0
for cell in $BENCH_RUN_MATRIX; do
  position=$((position + 1))
  context=${cell#*:}
  run_id=$(printf '%02d-r01-%s-%s-mcp' "$position" "$BENCH_TASKS" "$context")
  run_dir="$BENCH_RESULT_DIR/$run_id"
  workspace="$run_dir/workspace"
  if [ "${FAKE_WORKSPACE_MODE:-}" = reused ]; then
    workspace="$BENCH_RESULT_DIR/reused-workspace"
  fi
  mkdir -p "$run_dir" "$workspace"
  workspace_identity=$(cd "$workspace" && pwd -P)
  case "${FAKE_WORKSPACE_MODE:-}" in
    missing)
      [ "$position" -ne 1 ] && printf '%s\n' "$workspace_identity" \
        > "$run_dir/workspace-root.txt"
      ;;
    noncanonical)
      workspace_identity="$(dirname "$workspace_identity")/../$(basename "$(dirname "$workspace_identity")")/$(basename "$workspace_identity")"
      printf '%s\n' "$workspace_identity" > "$run_dir/workspace-root.txt"
      ;;
    *)
      printf '%s\n' "$workspace_identity" > "$run_dir/workspace-root.txt"
      ;;
  esac
  event_workspace_identity=$workspace_identity
  if [ "${FAKE_WORKSPACE_MODE:-}" = event-mismatch ]; then
    event_workspace_identity="$workspace_identity-other"
  fi
  jq -nc --arg root "$event_workspace_identity" \
    '{type:"item.started",item:{id:"call",type:"mcp_tool_call",server:"clj-surgeon",tool:"apply_clojure_changes",arguments:{workspace_root:$root,verify:"exact"}}}' \
    > "$run_dir/events.jsonl"
  jq -nc \
    '{type:"item.completed",item:{id:"call",type:"mcp_tool_call",server:"clj-surgeon",tool:"apply_clojure_changes",status:"completed",result:{structured_content:{verification_complete:true,next_action:"none"}}}}' \
    >> "$run_dir/events.jsonl"
  printf 'frozen prompt\n' > "$run_dir/prompt.txt"
  jq -nc \
    '{ok:true,"tool-names":["apply_clojure_changes"],"tool-projection":[{name:"apply_clojure_changes"}],"codex-executable":"/usr/bin/false"}' \
    > "$run_dir/codex-mcp-registry.json"
  printf '0\t1000000\t1000\t100\n1\t2000000\t2000\t100\n' > "$run_dir/event-clock.tsv"
  printf 'run_id\t%s\nstate\tcompleted\nexit_code\t0\n' "$run_id" > "$run_dir/terminal.tsv"
  if [ "${FAKE_WORKSPACE_MODE:-}" = deleted ]; then
    rm -rf "$workspace"
  fi
done
printf 'retained\tcomplete\n' >> "$BENCH_RESULT_DIR/runs.tsv"
if [ "${FAKE_DIRTY_RUNTIME_AFTER_BLOCK:-}" = "$(basename "$BENCH_RESULT_DIR")" ]; then
  printf '; worker dirtied runtime\n' >> "$FAKE_RUNTIME_FILE"
fi
WORKER
  cat > "$root/bench/relation_causal_score.clj" <<'SCORER'
#!/usr/bin/env bash
set -euo pipefail
phase=
output=
while [ "$#" -gt 0 ]; do
  case "$1" in
    --phase) phase=$2; shift 2 ;;
    --manifest|--block1-manifest|--block2-manifest) test -s "$2"; shift 2 ;;
    --output) output=$2; shift 2 ;;
    *) exit 22 ;;
  esac
done
printf '%s\n' "$phase" >> "$FAKE_SCORER_LOG"
case "$phase" in
  block1)
    if [ "${FAKE_AUTHORIZE_BLOCK2:-true}" = true ]; then
      printf '{:schema :clj-surgeon.edit-025-relation-causal-cohort/v1 :ok true :run-count 4 :gate {:block-2-authorized true :promote false}}\n' > "$output"
    else
      printf '{:schema :clj-surgeon.edit-025-relation-causal-cohort/v1 :ok true :run-count 4 :gate {:block-2-authorized false :promote false}}\n' > "$output"
    fi
    ;;
  final)
    printf '{:schema :clj-surgeon.edit-025-relation-causal-cohort/v1 :ok true :run-count 8 :gate {:block-2-authorized true :promote true}}\n' > "$output"
    ;;
  *) exit 23 ;;
esac
SCORER
  chmod +x "$root/bench/run_clean_codex.sh" "$root/bench/relation_causal_score.clj" \
    "$root/bench/run_relation_causal_cohort.sh"
  (
    cd "$root"
    git init -q
    git config user.email test@example.com
    git config user.name Test
    git add .
    git commit -qm baseline
    while read -r _ approved_path; do
      shasum -a 256 "$approved_path"
    done < "$source_repo/bench/relation_causal_artifacts.sha256" \
      > artifacts.sha256
    git add artifacts.sha256
    git commit -qm manifest
  )
  printf '%s\n' "$root"
}

invoke() {
  local root=$1 output=$2
  shift 2
  local head tree manifest_sha
  head=$(git -C "$root" rev-parse 'HEAD^{commit}')
  tree=$(git -C "$root" rev-parse 'HEAD^{tree}')
  manifest_sha=$(shasum -a 256 "$root/artifacts.sha256" | awk '{print $1}')
  env \
    PATH="$test_path" \
    BENCH_RELATION_EXPECTED_HEAD="$head" \
    BENCH_RELATION_EXPECTED_TREE="$tree" \
    BENCH_RELATION_ARTIFACT_MANIFEST="$root/artifacts.sha256" \
    BENCH_RELATION_EXPECTED_MANIFEST_SHA256="$manifest_sha" \
    BENCH_RELATION_EXPECTED_CODEX_EXECUTABLE="$test_codex_executable" \
    BENCH_RELATION_EXPECTED_CODEX_SHA256="$test_codex_sha256" \
    BENCH_RELATION_EXPECTED_CODEX_VERSION="$test_codex_version" \
    BENCH_RELATION_EXPECTED_PLATFORM_OS="$test_platform_os" \
    BENCH_RELATION_EXPECTED_PLATFORM_ARCH="$test_platform_arch" \
    BENCH_RELATION_EXPECTED_CODEX_PACKAGE_SHA256="$test_codex_package_sha256" \
    BENCH_RELATION_EXPECTED_CODEX_PLATFORM_PACKAGE_SHA256="$test_codex_platform_package_sha256" \
    BENCH_RELATION_EXPECTED_CODEX_NATIVE_EXECUTABLE="$test_codex_native_executable" \
    BENCH_RELATION_EXPECTED_CODEX_NATIVE_SHA256="$test_codex_native_sha256" \
    BENCH_RELATION_EXPECTED_NODE_EXECUTABLE="$test_node_executable" \
    BENCH_RELATION_EXPECTED_NODE_SHA256="$test_node_sha256" \
    BENCH_RELATION_EXPECTED_NODE_VERSION="$test_node_version" \
    BENCH_RELATION_RESULT_DIR="$output" \
    BENCH_RELATION_SCORER="$root/bench/relation_causal_score.clj" \
    BENCH_RELATION_SCORER_LAUNCHER=bash \
    FAKE_CALL_LOG="$root/calls.log" \
    FAKE_SCORER_LOG="$root/scorer.log" \
    FAKE_RUNTIME_FILE="$root/src/clj_surgeon/mcp_change_buffer.clj" \
    "$@" \
    bash "$root/bench/run_relation_causal_cohort.sh"
}

test_wrong_and_dirty_identity_refuse() {
  local root output head tree manifest_sha
  root=$(new_repo identity)
  output="$test_root/identity-wrong-head"
  head=$(git -C "$root" rev-parse 'HEAD^{commit}')
  tree=$(git -C "$root" rev-parse 'HEAD^{tree}')
  manifest_sha=$(shasum -a 256 "$root/artifacts.sha256" | awk '{print $1}')
  if env PATH="$test_path" \
    BENCH_RELATION_EXPECTED_HEAD=0000000000000000000000000000000000000000 \
    BENCH_RELATION_EXPECTED_TREE="$tree" \
    BENCH_RELATION_ARTIFACT_MANIFEST="$root/artifacts.sha256" \
    BENCH_RELATION_EXPECTED_MANIFEST_SHA256="$manifest_sha" \
    BENCH_RELATION_EXPECTED_CODEX_EXECUTABLE="$test_codex_executable" \
    BENCH_RELATION_EXPECTED_CODEX_SHA256="$test_codex_sha256" \
    BENCH_RELATION_EXPECTED_CODEX_VERSION="$test_codex_version" \
    BENCH_RELATION_EXPECTED_PLATFORM_OS="$test_platform_os" \
    BENCH_RELATION_EXPECTED_PLATFORM_ARCH="$test_platform_arch" \
    BENCH_RELATION_EXPECTED_CODEX_PACKAGE_SHA256="$test_codex_package_sha256" \
    BENCH_RELATION_EXPECTED_CODEX_PLATFORM_PACKAGE_SHA256="$test_codex_platform_package_sha256" \
    BENCH_RELATION_EXPECTED_CODEX_NATIVE_EXECUTABLE="$test_codex_native_executable" \
    BENCH_RELATION_EXPECTED_CODEX_NATIVE_SHA256="$test_codex_native_sha256" \
    BENCH_RELATION_EXPECTED_NODE_EXECUTABLE="$test_node_executable" \
    BENCH_RELATION_EXPECTED_NODE_SHA256="$test_node_sha256" \
    BENCH_RELATION_EXPECTED_NODE_VERSION="$test_node_version" \
    BENCH_RELATION_RESULT_DIR="$output" \
    BENCH_RELATION_SCORER="$root/bench/relation_causal_score.clj" \
    BENCH_RELATION_SCORER_LAUNCHER=bash \
    bash "$root/bench/run_relation_causal_cohort.sh" >"$root/wrong.out" 2>&1; then
    fail 'wrong HEAD was accepted'
  fi
  assert_contains "$root/wrong.out" 'Candidate HEAD mismatch'
  printf '# dirty\n' >> "$root/bench/run_clean_codex.sh"
  if invoke "$root" "$test_root/identity-dirty" >"$root/dirty.out" 2>&1; then
    fail 'dirty manifested artifact was accepted'
  fi
  assert_contains "$root/dirty.out" 'dirty cohort artifacts'
}

test_admission_shell_error_propagates() {
  local root output head tree manifest_sha status=0
  root=$(new_repo admission-shell-error)
  output="$test_root/admission-shell-error-output"
  head=$(git -C "$root" rev-parse 'HEAD^{commit}')
  tree=$(git -C "$root" rev-parse 'HEAD^{tree}')
  manifest_sha=$(shasum -a 256 "$root/artifacts.sha256" | awk '{print $1}')
  (
    unset BENCH_RELATION_EXPECTED_CODEX_VERSION
    env PATH="$test_path" \
      BENCH_RELATION_EXPECTED_HEAD="$head" \
      BENCH_RELATION_EXPECTED_TREE="$tree" \
      BENCH_RELATION_ARTIFACT_MANIFEST="$root/artifacts.sha256" \
      BENCH_RELATION_EXPECTED_MANIFEST_SHA256="$manifest_sha" \
      BENCH_RELATION_EXPECTED_CODEX_EXECUTABLE="$test_codex_executable" \
      BENCH_RELATION_EXPECTED_CODEX_SHA256="$test_codex_sha256" \
      BENCH_RELATION_EXPECTED_PLATFORM_OS="$test_platform_os" \
      BENCH_RELATION_EXPECTED_PLATFORM_ARCH="$test_platform_arch" \
      BENCH_RELATION_EXPECTED_CODEX_PACKAGE_SHA256="$test_codex_package_sha256" \
      BENCH_RELATION_EXPECTED_CODEX_PLATFORM_PACKAGE_SHA256="$test_codex_platform_package_sha256" \
      BENCH_RELATION_EXPECTED_CODEX_NATIVE_EXECUTABLE="$test_codex_native_executable" \
      BENCH_RELATION_EXPECTED_CODEX_NATIVE_SHA256="$test_codex_native_sha256" \
      BENCH_RELATION_EXPECTED_NODE_EXECUTABLE="$test_node_executable" \
      BENCH_RELATION_EXPECTED_NODE_SHA256="$test_node_sha256" \
      BENCH_RELATION_EXPECTED_NODE_VERSION="$test_node_version" \
      BENCH_RELATION_RESULT_DIR="$output" \
      bash "$root/bench/run_relation_causal_cohort.sh"
  ) >"$root/run.out" 2>&1 || status=$?
  [ "$status" -ne 0 ] || fail 'admission shell error was reported as success'
  assert_contains "$root/run.out" 'BENCH_RELATION_EXPECTED_CODEX_VERSION is required'
  [ ! -e "$root/calls.log" ] || fail 'worker ran after admission shell error'
}

test_first_required_setting_error_propagates() {
  local root output status=0
  root=$(new_repo first-admission-shell-error)
  output="$test_root/first-admission-shell-error-output"
  (
    unset BENCH_RELATION_EXPECTED_HEAD
    env PATH="$test_path" \
      BENCH_RELATION_RESULT_DIR="$output" \
      bash "$root/bench/run_relation_causal_cohort.sh"
  ) >"$root/run.out" 2>&1 || status=$?
  [ "$status" -ne 0 ] || fail 'first admission shell error was reported as success'
  assert_contains "$root/run.out" 'BENCH_RELATION_EXPECTED_HEAD is required'
  [ ! -e "$root/calls.log" ] || fail 'worker ran after first admission shell error'
}

test_output_and_settings_refuse() {
  local root nonempty link
  root=$(new_repo admission)
  nonempty="$test_root/nonempty"
  mkdir "$nonempty"
  printf 'old\n' > "$nonempty/old.txt"
  if invoke "$root" "$nonempty" >"$root/nonempty.out" 2>&1; then
    fail 'nonempty output was accepted'
  fi
  assert_contains "$root/nonempty.out" 'must be empty'
  link="$test_root/output-link"
  ln -s "$test_root" "$link"
  if invoke "$root" "$link" >"$root/link.out" 2>&1; then
    fail 'symlink output was accepted'
  fi
  assert_contains "$root/link.out" 'cannot be a symlink'
  if invoke "$root" "$test_root/wrong-schedule" \
      BENCH_RUN_MATRIX='mcp:mcp-relation-r-no-skill' >"$root/schedule.out" 2>&1; then
    fail 'wrong schedule was accepted'
  fi
  assert_contains "$root/schedule.out" 'BENCH_RUN_MATRIX must be exactly'
  if invoke "$root" "$test_root/wrong-parallelism" \
      BENCH_PARALLELISM=2 >"$root/parallel.out" 2>&1; then
    fail 'parallel execution was accepted'
  fi
  assert_contains "$root/parallel.out" 'BENCH_PARALLELISM must be exactly: 1'
  if invoke "$root" "$test_root/wrong-model" \
      BENCH_MODEL=gpt-5.6-terra >"$root/model.out" 2>&1; then
    fail 'wrong model was accepted'
  fi
  assert_contains "$root/model.out" 'BENCH_MODEL must be exactly: gpt-5.6-sol'
  if invoke "$root" "$test_root/wrong-reasoning" \
      BENCH_REASONING=medium >"$root/reasoning.out" 2>&1; then
    fail 'wrong reasoning was accepted'
  fi
  assert_contains "$root/reasoning.out" 'BENCH_REASONING must be exactly: high'
  if invoke "$root" "$test_root/wrong-codex-path" \
      BENCH_RELATION_EXPECTED_CODEX_EXECUTABLE=/usr/bin/false \
      >"$root/codex-path.out" 2>&1; then
    fail 'wrong Codex executable was accepted'
  fi
  assert_contains "$root/codex-path.out" 'Codex executable identity mismatch'
  if invoke "$root" "$test_root/wrong-codex-sha" \
      BENCH_RELATION_EXPECTED_CODEX_SHA256=0000000000000000000000000000000000000000000000000000000000000000 \
      >"$root/codex-sha.out" 2>&1; then
    fail 'wrong Codex SHA was accepted'
  fi
  assert_contains "$root/codex-sha.out" 'Codex SHA mismatch'
  if invoke "$root" "$test_root/wrong-codex-version" \
      BENCH_RELATION_EXPECTED_CODEX_VERSION='codex-cli wrong' \
      >"$root/codex-version.out" 2>&1; then
    fail 'wrong Codex version was accepted'
  fi
  assert_contains "$root/codex-version.out" 'Codex version mismatch'
  if invoke "$root" "$test_root/wrong-package-sha" \
      BENCH_RELATION_EXPECTED_CODEX_PACKAGE_SHA256=0000000000000000000000000000000000000000000000000000000000000000 \
      >"$root/package-sha.out" 2>&1; then
    fail 'wrong Codex package SHA was accepted'
  fi
  assert_contains "$root/package-sha.out" 'Codex package SHA mismatch'
  if invoke "$root" "$test_root/wrong-platform-package-sha" \
      BENCH_RELATION_EXPECTED_CODEX_PLATFORM_PACKAGE_SHA256=0000000000000000000000000000000000000000000000000000000000000000 \
      >"$root/platform-package-sha.out" 2>&1; then
    fail 'wrong Codex platform package SHA was accepted'
  fi
  assert_contains "$root/platform-package-sha.out" \
    'Codex platform package SHA mismatch'
  if invoke "$root" "$test_root/wrong-native-path" \
      BENCH_RELATION_EXPECTED_CODEX_NATIVE_EXECUTABLE=/usr/bin/false \
      >"$root/native-path.out" 2>&1; then
    fail 'wrong Codex native path was accepted'
  fi
  assert_contains "$root/native-path.out" 'Codex native executable identity mismatch'
  if invoke "$root" "$test_root/wrong-native-sha" \
      BENCH_RELATION_EXPECTED_CODEX_NATIVE_SHA256=0000000000000000000000000000000000000000000000000000000000000000 \
      >"$root/native-sha.out" 2>&1; then
    fail 'wrong Codex native SHA was accepted'
  fi
  assert_contains "$root/native-sha.out" 'Codex native SHA mismatch'
  if invoke "$root" "$test_root/wrong-node-sha" \
      BENCH_RELATION_EXPECTED_NODE_SHA256=0000000000000000000000000000000000000000000000000000000000000000 \
      >"$root/node-sha.out" 2>&1; then
    fail 'wrong Node SHA was accepted'
  fi
  assert_contains "$root/node-sha.out" 'Node SHA mismatch'
  if invoke "$root" "$test_root/wrong-node-path" \
      BENCH_RELATION_EXPECTED_NODE_EXECUTABLE=/usr/bin/false \
      >"$root/node-path.out" 2>&1; then
    fail 'wrong Node path was accepted'
  fi
  assert_contains "$root/node-path.out" 'Node executable identity mismatch'
  if invoke "$root" "$test_root/wrong-node-version" \
      BENCH_RELATION_EXPECTED_NODE_VERSION='vwrong' \
      >"$root/node-version.out" 2>&1; then
    fail 'wrong Node version was accepted'
  fi
  assert_contains "$root/node-version.out" 'Node version mismatch'
  if invoke "$root" "$test_root/wrong-platform" \
      BENCH_RELATION_EXPECTED_PLATFORM_OS='ImpossibleOS' \
      >"$root/platform.out" 2>&1; then
    fail 'wrong platform was accepted'
  fi
  assert_contains "$root/platform.out" 'Platform OS mismatch'
  if invoke "$root" "$test_root/wrong-architecture" \
      BENCH_RELATION_EXPECTED_PLATFORM_ARCH='impossible-arch' \
      >"$root/architecture.out" 2>&1; then
    fail 'wrong platform architecture was accepted'
  fi
  assert_contains "$root/architecture.out" 'Platform architecture mismatch'
}

test_dirty_runtime_closure_refuses() {
  local root output mutation
  for mutation in untracked-shadow tracked-product tracked-capture; do
    root=$(new_repo "dirty-runtime-$mutation")
    output="$test_root/dirty-runtime-$mutation-output"
    case "$mutation" in
      untracked-shadow)
        printf '(ns clj-surgeon.shadow)\n' > "$root/src/clj_surgeon/shadow.clj"
        ;;
      tracked-product)
        printf '; dirty product\n' >> "$root/src/clj_surgeon/mcp_change_buffer.clj"
        ;;
      tracked-capture)
        printf '; dirty capture\n' >> "$root/bench/capture_codex_mcp_registry.clj"
        ;;
    esac
    if invoke "$root" "$output" >"$root/run.out" 2>&1; then
      fail "dirty $mutation runtime closure was accepted"
    fi
    assert_contains "$root/run.out" 'dirty runtime closure'
    [ ! -e "$root/calls.log" ] || fail "worker ran after dirty $mutation runtime closure"
  done
}

test_empty_manifest_refuses_without_masking_exit() {
  local root output status=0
  root=$(new_repo empty-manifest)
  output="$test_root/empty-manifest-output"
  : > "$root/artifacts.sha256"
  (
    cd "$root"
    git add artifacts.sha256
    git commit -qm 'empty manifest'
  )
  invoke "$root" "$output" >"$root/run.out" 2>&1 || status=$?
  [ "$status" -ne 0 ] || fail 'empty manifest admission failure was masked'
  assert_contains "$root/run.out" 'Artifact manifest is empty'
  [ ! -e "$root/calls.log" ] || fail 'worker ran after empty manifest'
}

test_realpath_failure_refuses_without_masking_exit() {
  local root output broken_bin status=0
  root=$(new_repo realpath-failure)
  output="$test_root/realpath-failure-output"
  broken_bin="$test_root/broken-realpath-bin"
  mkdir "$broken_bin"
  cat > "$broken_bin/realpath" <<'REALPATH'
#!/usr/bin/env bash
exit 127
REALPATH
  chmod +x "$broken_bin/realpath"
  invoke "$root" "$output" PATH="$broken_bin:$test_path" \
    >"$root/run.out" 2>&1 || status=$?
  [ "$status" -ne 0 ] || fail 'realpath failure was masked'
  [ ! -e "$root/calls.log" ] || fail 'worker ran after realpath failure'
}

test_manifest_path_set_is_exact() {
  local root output mutation
  for mutation in remove add substitute; do
    root=$(new_repo "manifest-$mutation")
    output="$test_root/manifest-$mutation-output"
    case "$mutation" in
      remove)
        grep -v 'test/clj_surgeon/mcp_compact_relations_test.clj$' \
          "$root/artifacts.sha256" > "$root/artifacts.next"
        mv "$root/artifacts.next" "$root/artifacts.sha256"
        ;;
      add)
        printf 'extra\n' > "$root/extra.txt"
        (cd "$root" && shasum -a 256 extra.txt) >> "$root/artifacts.sha256"
        ;;
      substitute)
        printf 'extra\n' > "$root/extra.txt"
        grep -v 'test/clj_surgeon/mcp_compact_relations_test.clj$' \
          "$root/artifacts.sha256" > "$root/artifacts.next"
        (cd "$root" && shasum -a 256 extra.txt) >> "$root/artifacts.next"
        mv "$root/artifacts.next" "$root/artifacts.sha256"
        ;;
    esac
    (
      cd "$root"
      git add .
      git commit -qm "manifest $mutation"
    )
    if invoke "$root" "$output" >"$root/run.out" 2>&1; then
      fail "manifest $mutation was accepted"
    fi
    case "$mutation" in
      remove|add) assert_contains "$root/run.out" 'must contain exactly 39 paths' ;;
      substitute) assert_contains "$root/run.out" 'path set differs' ;;
    esac
    [ ! -e "$root/calls.log" ] || fail "worker ran after manifest $mutation"
  done
}

test_worker_drift_after_block_one_stops_block_two() {
  local root output status=0
  root=$(new_repo worker-drift)
  output="$test_root/worker-drift-output"
  invoke "$root" "$output" FAKE_DIRTY_RUNTIME_AFTER_BLOCK=block1 \
    >"$root/run.out" 2>&1 || status=$?
  [ "$status" -ne 0 ] || fail 'worker runtime drift was accepted'
  assert_contains "$root/run.out" 'dirty runtime closure'
  [ "$(wc -l < "$root/calls.log" | tr -d ' ')" -eq 1 ] \
    || fail 'block two ran after worker runtime drift'
  [ ! -e "$output/block2" ] || fail 'block two output exists after worker runtime drift'
  assert_contains "$output/coordinator-receipt.edn" ':stage :block1'
  assert_contains "$output/coordinator-receipt.edn" ':state :failed'
}

test_incomplete_fixture_manifest_refuses() {
  local root output
  root=$(new_repo incomplete-fixture-manifest)
  output="$test_root/incomplete-fixture-manifest-output"
  grep -v 'submission-row-extraction-cleanup/task.txt$' \
    "$root/artifacts.sha256" > "$root/artifacts.sha256.next"
  mv "$root/artifacts.sha256.next" "$root/artifacts.sha256"
  (
    cd "$root"
    git add artifacts.sha256
    git commit -qm 'omit task fixture'
  )
  if invoke "$root" "$output" >"$root/run.out" 2>&1; then
    fail 'incomplete fixture manifest was accepted'
  fi
  assert_contains "$root/run.out" \
    'Artifact manifest must contain exactly 39 paths'
  [ ! -e "$root/calls.log" ] || fail 'worker ran after incomplete fixture manifest'
}

test_child_failure_is_retained_without_retry() {
  local root output status
  root=$(new_repo child-failure)
  output="$test_root/child-failure-output"
  status=0
  invoke "$root" "$output" FAKE_FAIL_BLOCK=block1 >"$root/run.out" 2>&1 || status=$?
  [ "$status" -eq 17 ] || fail "child failure exit was $status, expected 17"
  [ -f "$output/block1/child-failure.txt" ] || fail 'child failure evidence was dropped'
  [ -f "$output/block1/runs.tsv" ] || fail 'child rows were dropped'
  [ ! -e "$output/block2" ] || fail 'block two ran after a child failure'
  [ ! -e "$root/scorer.log" ] || fail 'scorer ran after a child failure'
  assert_contains "$output/coordinator-receipt.edn" ':stage :block1'
  assert_contains "$output/coordinator-receipt.edn" ':state :failed'
  [ "$(wc -l < "$root/calls.log" | tr -d ' ')" -eq 1 ] || fail 'child failure was retried'
}

test_block1_gate_stops_and_retains() {
  local root output status
  root=$(new_repo gate-stop)
  output="$test_root/gate-stop-output"
  status=0
  invoke "$root" "$output" FAKE_AUTHORIZE_BLOCK2=false >"$root/run.out" 2>&1 || status=$?
  [ "$status" -eq 3 ] || fail "gate-stop exit was $status, expected 3"
  [ -f "$output/block1/runs.tsv" ] || fail 'block-one rows were dropped'
  [ -f "$output/block1-score.edn" ] || fail 'block-one score was dropped'
  [ ! -e "$output/block2" ] || fail 'block two ran after a failed gate'
  [ "$(wc -l < "$root/scorer.log" | tr -d ' ')" -eq 1 ] || fail 'aggregate ran after a failed gate'
  assert_contains "$output/coordinator-receipt.edn" ':stage :block1-score'
  assert_contains "$output/coordinator-receipt.edn" ':state :failed'
}

test_workspace_identity_refuses() {
  local root mode output status
  root=$(new_repo workspace-identity)
  for mode in missing noncanonical reused event-mismatch; do
    output="$test_root/workspace-$mode-output"
    status=0
    invoke "$root" "$output" FAKE_WORKSPACE_MODE="$mode" \
      >"$root/workspace-$mode.out" 2>&1 || status=$?
    [ "$status" -eq 3 ] || fail "$mode workspace exit was $status, expected 3"
    [ -f "$output/block1/runs.tsv" ] || fail "$mode workspace dropped worker rows"
    [ ! -e "$output/block2" ] || fail "$mode workspace allowed block two"
  done
  assert_contains "$root/workspace-missing.out" 'missing its workspace identity'
  assert_contains "$root/workspace-noncanonical.out" 'workspace identity is not canonical'
  assert_contains "$root/workspace-reused.out" 'workspace identity was reused'
  assert_contains "$root/workspace-event-mismatch.out" 'workspace receipt differs from apply arguments'
}

test_deleted_workspace_after_run_is_valid() {
  local root output
  root=$(new_repo deleted-workspace)
  output="$test_root/deleted-workspace-output"
  invoke "$root" "$output" FAKE_WORKSPACE_MODE=deleted >"$root/run.out" 2>&1
  [ -f "$output/final-report.edn" ] \
    || fail 'deleted workspaces prevented the final aggregate'
  [ -z "$(find "$output/block1" "$output/block2" -type d -name workspace -print -quit)" ] \
    || fail 'fake worker did not delete its workspaces'
  assert_contains "$output/block1-run-manifest.edn" ':workspace-root'
  assert_contains "$output/coordinator-receipt.edn" ':state :complete'
}

test_passing_boundary_executes_both_blocks_once() {
  local root output expected_block1 expected_block2
  root=$(new_repo passing)
  output="$test_root/passing-output"
  invoke "$root" "$output" >"$root/run.out" 2>&1
  [ -f "$output/block1/runs.tsv" ] || fail 'block-one rows are missing'
  [ -f "$output/block2/runs.tsv" ] || fail 'block-two rows are missing'
  [ -f "$output/final-report.edn" ] || fail 'final aggregate is missing'
  assert_contains "$output/block1-run-manifest.edn" ':workspace-root'
  assert_contains "$output/block2-run-manifest.edn" ':workspace-root'
  [ "$(wc -l < "$root/calls.log" | tr -d ' ')" -eq 2 ] || fail 'worker did not run exactly twice'
  [ "$(grep -c '^block1$' "$root/scorer.log")" -eq 1 ] || fail 'block-one scorer call count differs'
  [ "$(grep -c '^final$' "$root/scorer.log")" -eq 1 ] || fail 'aggregate did not run exactly once'
  expected_block1='mcp:mcp-relation-n-no-skill mcp:mcp-relation-r-no-skill mcp:mcp-relation-r-no-skill mcp:mcp-relation-n-no-skill'
  expected_block2='mcp:mcp-relation-r-no-skill mcp:mcp-relation-n-no-skill mcp:mcp-relation-n-no-skill mcp:mcp-relation-r-no-skill'
  sed -n '1p' "$root/calls.log" | grep -F "$expected_block1" >/dev/null \
    || fail 'block-one order differs'
  sed -n '2p' "$root/calls.log" | grep -F "$expected_block2" >/dev/null \
    || fail 'block-two order differs'
  [ "$(cut -f3 "$root/calls.log" | sort -u)" = submission-row-extraction-cleanup ] \
    || fail 'task differs'
  [ "$(cut -f4 "$root/calls.log" | sort -u)" = 1 ] || fail 'replicates differ'
  [ "$(cut -f5 "$root/calls.log" | sort -u)" = 1 ] || fail 'parallelism differs'
  [ "$(cut -f6 "$root/calls.log" | sort -u)" = apply ] || fail 'tool profile differs'
  [ "$(cut -f7 "$root/calls.log" | sort -u)" = gpt-5.6-sol ] \
    || fail 'model differs'
  [ "$(cut -f8 "$root/calls.log" | sort -u)" = high ] \
    || fail 'reasoning differs'
  [ -s "$output/environment.edn" ] || fail 'environment receipt is missing'
  assert_contains "$output/environment.edn" ':model "gpt-5.6-sol"'
  assert_contains "$output/environment.edn" ':reasoning "high"'
  assert_contains "$output/environment.edn" \
    ":codex-executable \"$test_codex_executable\""
  assert_contains "$output/environment.edn" \
    ":codex-sha256 \"$test_codex_sha256\""
  assert_contains "$output/environment.edn" \
    ":codex-version \"$test_codex_version\""
  assert_contains "$output/environment.edn" \
    ":codex-package-sha256 \"$test_codex_package_sha256\""
  assert_contains "$output/environment.edn" \
    ":codex-platform-package-sha256 \"$test_codex_platform_package_sha256\""
  assert_contains "$output/environment.edn" \
    ":codex-native-executable \"$test_codex_native_executable\""
  assert_contains "$output/environment.edn" \
    ":codex-native-sha256 \"$test_codex_native_sha256\""
  assert_contains "$output/environment.edn" \
    ":node-executable \"$test_node_executable\""
  assert_contains "$output/environment.edn" \
    ":node-sha256 \"$test_node_sha256\""
  assert_contains "$output/environment.edn" \
    ":node-version \"$test_node_version\""
  assert_contains "$output/environment.edn" \
    ":platform-os \"$test_platform_os\""
  assert_contains "$output/environment.edn" \
    ":platform-arch \"$test_platform_arch\""
  assert_contains "$output/coordinator-receipt.edn" ':state :complete'
  assert_contains "$output/coordinator-receipt.edn" ':stage :complete'
}

test_real_scorer_loads_with_coordinator_classpath() {
  local output="$test_root/real-scorer-invalid.out" status=0
  (
    cd "$source_repo"
    bb -cp src:test:bench bench/relation_causal_score.clj --help
  ) >"$output" 2>&1 || status=$?
  [ "$status" -eq 2 ] \
    || fail "controlled-invalid real scorer exit was $status, expected 2"
  assert_contains "$output" ':clj-surgeon.edit-025-relation-causal-phase/v1'
  assert_contains "$output" ':invalid-cli-input'
}

test_wrong_and_dirty_identity_refuse
test_admission_shell_error_propagates
test_first_required_setting_error_propagates
test_output_and_settings_refuse
test_dirty_runtime_closure_refuses
test_empty_manifest_refuses_without_masking_exit
test_realpath_failure_refuses_without_masking_exit
test_manifest_path_set_is_exact
test_worker_drift_after_block_one_stops_block_two
test_incomplete_fixture_manifest_refuses
test_child_failure_is_retained_without_retry
test_block1_gate_stops_and_retains
test_workspace_identity_refuses
test_deleted_workspace_after_run_is_valid
test_passing_boundary_executes_both_blocks_once
test_real_scorer_loads_with_coordinator_classpath

printf 'relation causal cohort runner boundary tests passed\n'
