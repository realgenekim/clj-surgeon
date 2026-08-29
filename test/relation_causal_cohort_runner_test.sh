#!/usr/bin/env bash
set -euo pipefail

source_script=$(cd "$(dirname "$0")/.." && pwd -P)/bench/run_relation_causal_cohort.sh
test_root=$(mktemp -d /tmp/clj-surgeon-relation-coordinator-test.XXXXXX)
trap '[ "${KEEP_RELATION_TEST_TMP:-false}" = true ] || rm -rf "$test_root"' EXIT

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
  printf 'profile\n' > "$root/bench/fixtures/edit_portfolio/submission-row-extraction-cleanup/exact-profile.edn"
  cat > "$root/bench/run_clean_codex.sh" <<'WORKER'
#!/usr/bin/env bash
set -euo pipefail
mkdir -p "$BENCH_RESULT_DIR"
printf '%s\t%s\t%s\t%s\t%s\t%s\n' \
  "$BENCH_RESULT_DIR" "$BENCH_RUN_MATRIX" "$BENCH_TASKS" \
  "$BENCH_REPLICATES" "$BENCH_PARALLELISM" "$BENCH_MCP_TOOL_PROFILE" \
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
  printf '%s\n' \
    '{"type":"item.started","item":{"id":"call","type":"mcp_tool_call","server":"clj-surgeon","tool":"apply_clojure_changes","arguments":{"workspace_root":"/private/tmp/fake","verify":"exact"}}}' \
    '{"type":"item.completed","item":{"id":"call","type":"mcp_tool_call","server":"clj-surgeon","tool":"apply_clojure_changes","status":"completed","result":{"structured_content":{"verification_complete":true,"next_action":"none"}}}}' \
    > "$run_dir/events.jsonl"
  printf '0\t1000000\t1000\t100\n1\t2000000\t2000\t100\n' > "$run_dir/event-clock.tsv"
  printf 'run_id\t%s\nstate\tcompleted\nexit_code\t0\n' "$run_id" > "$run_dir/terminal.tsv"
  case "${FAKE_WORKSPACE_MODE:-}" in
    missing)
      [ "$position" -ne 1 ] && (cd "$workspace" && pwd -P) > "$run_dir/workspace-root.txt"
      ;;
    noncanonical)
      printf '%s/../%s\n' "$(dirname "$workspace")" "$(basename "$workspace")" \
        > "$run_dir/workspace-root.txt"
      ;;
    *)
      (cd "$workspace" && pwd -P) > "$run_dir/workspace-root.txt"
      ;;
  esac
done
printf 'retained\tcomplete\n' >> "$BENCH_RESULT_DIR/runs.tsv"
WORKER
  cat > "$root/bench/fake_scorer.sh" <<'SCORER'
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
  chmod +x "$root/bench/run_clean_codex.sh" "$root/bench/fake_scorer.sh" \
    "$root/bench/run_relation_causal_cohort.sh"
  (
    cd "$root"
    git init -q
    git config user.email test@example.com
    git config user.name Test
    git add .
    git commit -qm baseline
    {
      shasum -a 256 bench/run_relation_causal_cohort.sh
      shasum -a 256 bench/run_clean_codex.sh
      shasum -a 256 bench/fake_scorer.sh
      shasum -a 256 bench/fixtures/edit_portfolio/submission-row-extraction-cleanup/exact-profile.edn
    } > artifacts.sha256
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
    BENCH_RELATION_EXPECTED_HEAD="$head" \
    BENCH_RELATION_EXPECTED_TREE="$tree" \
    BENCH_RELATION_ARTIFACT_MANIFEST="$root/artifacts.sha256" \
    BENCH_RELATION_EXPECTED_MANIFEST_SHA256="$manifest_sha" \
    BENCH_RELATION_RESULT_DIR="$output" \
    BENCH_RELATION_SCORER="$root/bench/fake_scorer.sh" \
    BENCH_RELATION_SCORER_LAUNCHER=bash \
    FAKE_CALL_LOG="$root/calls.log" \
    FAKE_SCORER_LOG="$root/scorer.log" \
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
  if env BENCH_RELATION_EXPECTED_HEAD=0000000000000000000000000000000000000000 \
    BENCH_RELATION_EXPECTED_TREE="$tree" \
    BENCH_RELATION_ARTIFACT_MANIFEST="$root/artifacts.sha256" \
    BENCH_RELATION_EXPECTED_MANIFEST_SHA256="$manifest_sha" \
    BENCH_RELATION_RESULT_DIR="$output" \
    BENCH_RELATION_SCORER="$root/bench/fake_scorer.sh" \
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
}

test_workspace_identity_refuses() {
  local root mode output status
  root=$(new_repo workspace-identity)
  for mode in missing noncanonical reused; do
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
  assert_contains "$output/coordinator-receipt.edn" ':state :complete'
  assert_contains "$output/coordinator-receipt.edn" ':stage :complete'
}

test_wrong_and_dirty_identity_refuse
test_output_and_settings_refuse
test_child_failure_is_retained_without_retry
test_block1_gate_stops_and_retains
test_workspace_identity_refuses
test_passing_boundary_executes_both_blocks_once

printf 'relation causal cohort runner boundary tests passed\n'
