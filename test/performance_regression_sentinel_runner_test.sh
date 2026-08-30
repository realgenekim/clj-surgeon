#!/usr/bin/env bash
set -euo pipefail

source_repo=$(cd "$(dirname "$0")/.." && pwd -P)
source_runner="$source_repo/bench/run_performance_regression_sentinel.sh"
test_root=$(mktemp -d /tmp/clj-surgeon-performance-sentinel-runner-test.XXXXXX)
trap '[ "${KEEP_PERF_SENTINEL_TEST_TMP:-false}" = true ] || rm -rf "$test_root"' EXIT

fail() {
  printf 'FAIL: %s\n' "$1" >&2
  exit 1
}

assert_contains() {
  local file=$1 expected=$2
  grep -F "$expected" "$file" >/dev/null \
    || fail "$file does not contain: $expected"
}

assert_not_contains() {
  local file=$1 unexpected=$2
  if grep -F "$unexpected" "$file" >/dev/null; then
    fail "$file unexpectedly contains: $unexpected"
  fi
}

sha256() {
  shasum -a 256 "$1" | awk '{print $1}'
}

new_repo() {
  local name=$1 root
  root="$test_root/$name"
  mkdir -p "$root/bench/results" "$root/fixtures" "$root/product"
  cp "$source_runner" "$root/bench/run_performance_regression_sentinel.sh"
  printf '%s\n' \
    'bench/results/' \
    'request.edn' \
    'launch-admission.edn' \
    'controller-artifacts.sha256' \
    'pressure.log' \
    'worker.log' \
    'retain.log' \
    '*.out' > "$root/.gitignore"

  cat > "$root/bench/fake-materializer.sh" <<'MATERIALIZER'
#!/usr/bin/env bash
set -euo pipefail
[ "$#" -eq 3 ] || exit 64
repository=$1
source_ref=$2
destination=$3
mkdir -p "$destination/source"
case "$source_ref" in
  stable|aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa)
    commit=aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa
    tree=bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb
    ;;
  candidate|cccccccccccccccccccccccccccccccccccccccc)
    commit=cccccccccccccccccccccccccccccccccccccccc
    tree=dddddddddddddddddddddddddddddddddddddddd
    ;;
  *) exit 65 ;;
esac
printf '%s\n' "$source_ref" > "$destination/source/product.txt"
printf '{:schema :clj-surgeon.benchmark-candidate/v1 :source-repository "%s" :source-commit "%s" :source-tree "%s" :source-root "source"}\n' \
  "$repository" "$commit" "$tree" > "$destination/candidate-receipt.edn"
printf '%s\n' "$destination/candidate-receipt.edn"
MATERIALIZER

  cat > "$root/bench/fake-pressure.sh" <<'PRESSURE'
#!/usr/bin/env bash
set -euo pipefail
position=
output=
while [ "$#" -gt 0 ]; do
  case "$1" in
    --position) position=$2; shift 2 ;;
    --output) output=$2; shift 2 ;;
    *) exit 64 ;;
  esac
done
[ -n "$position" ] && [ -n "$output" ] || exit 64
mkdir -p "$(dirname "$output")"
sample_id="sample-$position"
if [ "${FAKE_PRESSURE_REUSE_AT:-}" = "$position" ]; then
  sample_id=sample-C1
fi
case "$position" in
  C1) sampled_at_ns=1000 ;;
  S1) sampled_at_ns=2000 ;;
  S2) sampled_at_ns=3000 ;;
  C2) sampled_at_ns=4000 ;;
  *) exit 64 ;;
esac
printf '%s\n' "$position:$sample_id:$output" >> "$FAKE_PRESSURE_LOG"
printf '{:schema :clj-surgeon.performance-regression-pressure/v1 :position :%s :sample-id "%s" :complete true :status :admitted :sampled-at-ns %s :policy-sha256 "3333333333333333333333333333333333333333333333333333333333333333"}\n' \
  "$position" "$sample_id" "$sampled_at_ns" > "$output"
PRESSURE

  cat > "$root/bench/fake-worker.sh" <<'WORKER'
#!/usr/bin/env bash
set -euo pipefail
: "${BENCH_SENTINEL_POSITION:?}"
: "${BENCH_SENTINEL_ARM:?}"
: "${BENCH_RESULT_DIR:?}"
: "${BENCH_MCP_PRODUCT_ROOT:?}"
: "${BENCH_SENTINEL_PRESSURE_RECEIPT:?}"
mkdir -p "$BENCH_RESULT_DIR/workspace" "$BENCH_RESULT_DIR/home"
position=$BENCH_SENTINEL_POSITION
case "$position" in
  C1) t_verified_ns=${FAKE_C1_NS:-100} ; port=56101 ;;
  S1) t_verified_ns=${FAKE_S1_NS:-100} ; port=56102 ;;
  S2) t_verified_ns=${FAKE_S2_NS:-100} ; port=56103 ;;
  C2) t_verified_ns=${FAKE_C2_NS:-100} ; port=56104 ;;
  *) exit 64 ;;
esac
workspace=$(cd "$BENCH_RESULT_DIR/workspace" && pwd -P)
home=$(cd "$BENCH_RESULT_DIR/home" && pwd -P)
result_root=$(cd "$BENCH_RESULT_DIR" && pwd -P)
product_root=$(cd "$BENCH_MCP_PRODUCT_ROOT" && pwd -P)
printf '%s\t%s\t%s\t%s\t%s\t%s\t%s\n' \
  "$position" "$BENCH_SENTINEL_ARM" "$workspace" "$home" "$port" \
  "$result_root" "$product_root" >> "$FAKE_WORKER_LOG"
printf 'retained child evidence for %s\n' "$position" > "$BENCH_RESULT_DIR/raw-events.jsonl"
if [ "${FAKE_CHILD_FAIL_AT:-}" = "$position" ]; then
  printf 'child failed at %s\n' "$position" > "$BENCH_RESULT_DIR/child-failure.txt"
  exit 17
fi
case "$BENCH_SENTINEL_ARM" in
  candidate)
    commit=cccccccccccccccccccccccccccccccccccccccc
    tree=dddddddddddddddddddddddddddddddddddddddd
    surface_sha=5555555555555555555555555555555555555555555555555555555555555555
    ;;
  stable)
    commit=aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa
    tree=bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb
    surface_sha=6666666666666666666666666666666666666666666666666666666666666666
    ;;
  *) exit 64 ;;
esac
if [ "${FAKE_IDENTITY_DRIFT_AT:-}" = "$position" ]; then
  commit=0000000000000000000000000000000000000000
fi
pressure_sha=$(shasum -a 256 "$BENCH_SENTINEL_PRESSURE_RECEIPT" | awk '{print $1}')
printf '{:schema :clj-surgeon.performance-regression-attempt/v1 :position :%s :arm :%s :t-verified-ns %s :workspace "%s" :home "%s" :port %s :result-root "%s" :product-root "%s" :product-commit "%s" :product-tree "%s" :client-surface-sha256 "%s" :pressure-sha256 "%s" :route-adherent true :correct true :verification-complete true}\n' \
  "$position" "$BENCH_SENTINEL_ARM" "$t_verified_ns" "$workspace" "$home" \
  "$port" "$result_root" "$product_root" "$commit" "$tree" "$surface_sha" "$pressure_sha" \
  > "$BENCH_RESULT_DIR/attempt-evidence.edn"
WORKER

  cat > "$root/bench/fake-policy.sh" <<'POLICY'
#!/usr/bin/env bash
set -euo pipefail
operation=${1:-}
shift || true
case "$operation" in
  compile-attempt)
    input=$1
    output=$2
    expected_commit=$3
    grep -F ":product-commit \"$expected_commit\"" "$input" >/dev/null || exit 42
    cp "$input" "$output"
    ;;
  compile-screen)
    ledger=$1
    output=$2
    c1=$(awk -F '\t' '$1 == "C1" {print $3}' "$ledger")
    s1=$(awk -F '\t' '$1 == "S1" {print $3}' "$ledger")
    if [ $((100 * c1)) -lt $((108 * s1)) ]; then
      printf '{:schema :clj-surgeon.performance-regression-screen/v1 :decision :green-stop}\n' > "$output"
    else
      printf '{:schema :clj-surgeon.performance-regression-screen/v1 :decision :reverse-required}\n' > "$output"
    fi
    ;;
  compile-final)
    ledger=$1
    output=$2
    test -s "$ledger"
    printf '{:schema :clj-surgeon.performance-regression-remote/v1 :remote-execution-complete true :ledger-authority false :append-authority false :projection-authority false :publication-authority false :promotion-authority false}\n' > "$output"
    ;;
  *) exit 64 ;;
esac
POLICY

  cat > "$root/bench/fake-retain.sh" <<'RETAINER'
#!/usr/bin/env bash
set -euo pipefail
result_root=$1
printf '%s\n' "$result_root" >> "$FAKE_RETAIN_LOG"
printf '{:schema :clj-surgeon.benchmark-archive-receipt/v1 :retained true}\n' \
  > "$result_root/archive-receipt.edn"
RETAINER

  chmod +x "$root/bench/"*.sh
  (
    cd "$root"
    git init -q
    git config user.email test@example.com
    git config user.name Test
    git add .
    git commit -qm baseline
  )
  local controller_head controller_tree manifest_sha
  controller_head=$(git -C "$root" rev-parse 'HEAD^{commit}')
  controller_tree=$(git -C "$root" rev-parse 'HEAD^{tree}')
  (
    cd "$root"
    for path in \
      bench/run_performance_regression_sentinel.sh \
      bench/fake-materializer.sh \
      bench/fake-pressure.sh \
      bench/fake-worker.sh \
      bench/fake-policy.sh \
      bench/fake-retain.sh; do
      shasum -a 256 "$path"
    done
  ) > "$root/controller-artifacts.sha256"
  manifest_sha=$(sha256 "$root/controller-artifacts.sha256")
  cat > "$root/request.edn" <<REQUEST
{:schema :clj-surgeon.performance-regression-sentinel-request/v1
 :invocation-id #uuid "11111111-2222-4333-8444-555555555555"
 :mode :nightly
 :candidate {:commit "cccccccccccccccccccccccccccccccccccccccc"
             :tree "dddddddddddddddddddddddddddddddddddddddd"}
 :stable {:kind :release-tag
          :tag "sentinel-stable-test"
          :tag-object nil
          :commit "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
          :tree "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb"
          :baseline-receipt-sha256 "1111111111111111111111111111111111111111111111111111111111111111"}
 :controller {:commit "$controller_head"
              :tree "$controller_tree"
              :artifact-manifest-sha256 "$manifest_sha"}
 :recovery nil}
REQUEST
  cat > "$root/launch-admission.edn" <<'ADMISSION'
{:schema :clj-surgeon.performance-regression-launch-admission/v1
 :invocation-id #uuid "11111111-2222-4333-8444-555555555555"
 :state :clear
 :nonce "test-nonce"
 :expires-at "2099-01-01T00:00:00Z"
 :launch-authority true
 :append-authority false
 :publication-authority false}
ADMISSION
  printf '%s\n' "$root"
}

invoke() {
  local root=$1
  shift
  env \
    BENCH_PERF_SENTINEL_REQUEST="$root/request.edn" \
    BENCH_PERF_SENTINEL_ADMISSION_RECEIPT="$root/launch-admission.edn" \
    BENCH_PERF_SENTINEL_RESULT_PARENT="$root/bench/results" \
    BENCH_PERF_SENTINEL_MATERIALIZER="$root/bench/fake-materializer.sh" \
    BENCH_PERF_SENTINEL_PRESSURE_SAMPLER="$root/bench/fake-pressure.sh" \
    BENCH_PERF_SENTINEL_WORKER="$root/bench/fake-worker.sh" \
    BENCH_PERF_SENTINEL_POLICY="$root/bench/fake-policy.sh" \
    BENCH_PERF_SENTINEL_RETAINER="$root/bench/fake-retain.sh" \
    FAKE_PRESSURE_LOG="$root/pressure.log" \
    FAKE_WORKER_LOG="$root/worker.log" \
    FAKE_RETAIN_LOG="$root/retain.log" \
    "$@" \
    bash "$root/bench/run_performance_regression_sentinel.sh"
}

result_path() {
  printf '%s/bench/results/%s\n' "$1" 11111111-2222-4333-8444-555555555555
}

# @spec PERF-SENT-SCHEDULE-001 PERF-SENT-SCHEDULE-002
# @spec PERF-SENT-RETAIN-001 PERF-SENT-RECEIPT-001
test_green_stops_after_exact_c1_s1() {
  local root result
  root=$(new_repo green-stop)
  result=$(result_path "$root")
  invoke "$root" FAKE_C1_NS=107 FAKE_S1_NS=100 > "$root/run.out" 2>&1
  [ "$(cut -f1 "$root/worker.log" | paste -sd, -)" = C1,S1 ] \
    || fail 'green screen did not run exactly C1,S1'
  [ "$(wc -l < "$root/pressure.log" | tr -d ' ')" -eq 2 ] \
    || fail 'green screen did not capture exactly two pressure samples'
  [ "$(wc -l < "$root/retain.log" | tr -d ' ')" -eq 1 ] \
    || fail 'green screen was not retained exactly once'
  [ -f "$result/remote-execution-receipt.edn" ] \
    || fail 'green remote receipt is missing'
}

# @spec PERF-SENT-SCHEDULE-001 PERF-SENT-SCHEDULE-002 PERF-SENT-THRESHOLD-001
# @spec PERF-SENT-ADMIT-001 PERF-SENT-ADMIT-002 PERF-SENT-IDENT-002
# @spec PERF-SENT-SURFACE-001
test_exact_eight_percent_runs_reverse_pair() {
  local root result positions unique_workspaces unique_homes unique_ports unique_results
  local candidate_root stable_root
  root=$(new_repo exact-trigger)
  result=$(result_path "$root")
  invoke "$root" FAKE_C1_NS=108 FAKE_S1_NS=100 FAKE_S2_NS=101 FAKE_C2_NS=102 \
    > "$root/run.out" 2>&1
  positions=$(cut -f1 "$root/worker.log" | paste -sd, -)
  [ "$positions" = C1,S1,S2,C2 ] \
    || fail "exact eight-percent schedule was $positions"
  [ "$(wc -l < "$root/pressure.log" | tr -d ' ')" -eq 4 ] \
    || fail 'reverse screen did not sample pressure four times'
  [ "$(cut -d: -f2 "$root/pressure.log" | sort -u | wc -l | tr -d ' ')" -eq 4 ] \
    || fail 'pressure sample identity was reused'
  unique_workspaces=$(cut -f3 "$root/worker.log" | sort -u | wc -l | tr -d ' ')
  unique_homes=$(cut -f4 "$root/worker.log" | sort -u | wc -l | tr -d ' ')
  unique_ports=$(cut -f5 "$root/worker.log" | sort -u | wc -l | tr -d ' ')
  unique_results=$(cut -f6 "$root/worker.log" | sort -u | wc -l | tr -d ' ')
  [ "$unique_workspaces:$unique_homes:$unique_ports:$unique_results" = 4:4:4:4 ] \
    || fail 'per-attempt resources were not unique'
  candidate_root=$(awk -F '\t' '$1 == "C1" {print $7}' "$root/worker.log")
  stable_root=$(awk -F '\t' '$1 == "S1" {print $7}' "$root/worker.log")
  [ "$candidate_root" != "$stable_root" ] \
    || fail 'candidate and stable shared one product root'
  [ "$(awk -F '\t' '$1 == "C2" {print $7}' "$root/worker.log")" = "$candidate_root" ] \
    || fail 'candidate product root drifted between positions'
  [ "$(awk -F '\t' '$1 == "S2" {print $7}' "$root/worker.log")" = "$stable_root" ] \
    || fail 'stable product root drifted between positions'
  [ "$(grep -h -o ':client-surface-sha256 "[0-9a-f]*"' \
      "$result/attempts/C1/attempt-evidence.edn" \
      "$result/attempts/C2/attempt-evidence.edn" | sort -u | wc -l | tr -d ' ')" -eq 1 ] \
    || fail 'candidate client surface drifted between positions'
  [ "$(grep -h -o ':client-surface-sha256 "[0-9a-f]*"' \
      "$result/attempts/S1/attempt-evidence.edn" \
      "$result/attempts/S2/attempt-evidence.edn" | sort -u | wc -l | tr -d ' ')" -eq 1 ] \
    || fail 'stable client surface drifted between positions'
  [ -f "$result/remote-execution-receipt.edn" ] \
    || fail 'reverse remote receipt is missing'
}

# @spec PERF-SENT-SCHEDULE-004 PERF-SENT-INVALID-002 PERF-SENT-RETAIN-001
test_child_failure_propagates_and_is_retained_without_retry() {
  local root result status=0
  root=$(new_repo child-failure)
  result=$(result_path "$root")
  invoke "$root" FAKE_C1_NS=108 FAKE_S1_NS=100 FAKE_CHILD_FAIL_AT=S1 \
    > "$root/run.out" 2>&1 || status=$?
  [ "$status" -eq 17 ] || fail "child failure exit was $status, expected 17"
  [ "$(cut -f1 "$root/worker.log" | paste -sd, -)" = C1,S1 ] \
    || fail 'child failure was retried or later attempts ran'
  [ -f "$result/attempts/S1/child-failure.txt" ] \
    || fail 'child failure evidence was not retained'
  [ -f "$result/remote-execution-failure.edn" ] \
    || fail 'remote failure receipt is missing'
  [ ! -e "$result/attempts/S2" ] && [ ! -e "$result/attempts/C2" ] \
    || fail 'reverse attempts ran after child failure'
}

# @spec PERF-SENT-ADMIT-001 PERF-SENT-ADMIT-002 PERF-SENT-INVALID-001
test_reused_pressure_refuses_before_attempt_launch() {
  local root result status=0
  root=$(new_repo reused-pressure)
  result=$(result_path "$root")
  invoke "$root" FAKE_C1_NS=108 FAKE_S1_NS=100 FAKE_PRESSURE_REUSE_AT=S1 \
    > "$root/run.out" 2>&1 || status=$?
  [ "$status" -ne 0 ] || fail 'reused pressure was accepted'
  [ "$(cut -f1 "$root/worker.log" | paste -sd, -)" = C1 ] \
    || fail 'S1 launched after its pressure identity was reused'
  [ -f "$result/remote-execution-failure.edn" ] \
    || fail 'pressure refusal evidence was not retained'
  assert_contains "$result/remote-execution-failure.edn" ':invalid-environment'
}

# @spec PERF-SENT-IDENT-001 PERF-SENT-SCHEDULE-004 PERF-SENT-INVALID-001
test_product_identity_drift_stops_and_retains() {
  local root result status=0
  root=$(new_repo identity-drift)
  result=$(result_path "$root")
  invoke "$root" FAKE_C1_NS=108 FAKE_S1_NS=100 FAKE_IDENTITY_DRIFT_AT=S1 \
    > "$root/run.out" 2>&1 || status=$?
  [ "$status" -ne 0 ] || fail 'product identity drift was accepted'
  [ "$(cut -f1 "$root/worker.log" | paste -sd, -)" = C1,S1 ] \
    || fail 'identity drift was retried or later attempts ran'
  [ -f "$result/attempts/S1/attempt-evidence.edn" ] \
    || fail 'identity-drift evidence was dropped'
  assert_contains "$result/remote-execution-failure.edn" ':identity-drift'
}

# @spec PERF-SENT-CONFIG-001
test_derived_result_root_refuses_nonempty_symlink_and_regular_file() {
  local root result mutation
  for mutation in nonempty symlink regular-file; do
    root=$(new_repo "result-$mutation")
    result=$(result_path "$root")
    case "$mutation" in
      nonempty)
        mkdir -p "$result"
        printf 'old\n' > "$result/old.txt"
        ;;
      symlink)
        ln -s "$test_root" "$result"
        ;;
      regular-file)
        printf 'not a directory\n' > "$result"
        ;;
    esac
    if invoke "$root" > "$root/run.out" 2>&1; then
      fail "$mutation derived result root was accepted"
    fi
    [ ! -e "$root/worker.log" ] || fail "worker ran for $mutation result root"
    assert_contains "$root/run.out" 'derived result root refused'
  done
  root=$(new_repo result-alias)
  if invoke "$root" \
      BENCH_PERF_SENTINEL_RESULT_PARENT="$root/bench/../bench/results" \
      > "$root/run.out" 2>&1; then
    fail 'noncanonical result-parent alias was accepted'
  fi
  [ ! -e "$root/worker.log" ] || fail 'worker ran for aliased result root'
  assert_contains "$root/run.out" 'derived result root refused'
}

# @spec PERF-SENT-RETAIN-001 PERF-SENT-RECEIPT-001 PERF-SENT-PROMOTION-001
test_remote_receipt_cannot_claim_skiff_authority() {
  local root result receipt
  root=$(new_repo remote-authority)
  result=$(result_path "$root")
  invoke "$root" FAKE_C1_NS=107 FAKE_S1_NS=100 > "$root/run.out" 2>&1
  receipt="$result/remote-execution-receipt.edn"
  assert_contains "$receipt" ':remote-execution-complete true'
  assert_contains "$receipt" ':ledger-authority false'
  assert_contains "$receipt" ':append-authority false'
  assert_contains "$receipt" ':projection-authority false'
  assert_contains "$receipt" ':publication-authority false'
  assert_contains "$receipt" ':promotion-authority false'
  assert_not_contains "$receipt" ':event-envelope'
  assert_not_contains "$receipt" ':append-receipt'
  assert_not_contains "$receipt" ':publication-state :allowed'
}

test_green_stops_after_exact_c1_s1
test_exact_eight_percent_runs_reverse_pair
test_child_failure_propagates_and_is_retained_without_retry
test_reused_pressure_refuses_before_attempt_launch
test_product_identity_drift_stops_and_retains
test_derived_result_root_refuses_nonempty_symlink_and_regular_file
test_remote_receipt_cannot_claim_skiff_authority

printf 'performance regression sentinel runner boundary tests passed\n'
