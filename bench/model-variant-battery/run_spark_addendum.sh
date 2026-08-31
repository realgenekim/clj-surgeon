#!/usr/bin/env bash

set -uo pipefail

experiment_root=/private/tmp/clj-surgeon-elaborator-fallback-battery-20260831
rig_root=/private/tmp/bang-rig
result_root="$experiment_root/bench/model-variant-battery/results/spark"
fixture="$rig_root/worktree/bench/fixtures/model_variant_battery.clj"
fixture_sha=01fce5a18a5b3035db9db9202dd6f8ec3b700fa9d88911bf32c764aaa47fbb08
target_epoch=$(date -j -f '%Y-%m-%d %H:%M:%S' '2026-08-31 00:45:00' '+%s')

mkdir -p "$result_root/schedule" "$result_root/fill/console" "$result_root/fill/receipts"
printf 'scheduled_at=%s\ntarget_local=2026-08-31T00:45:00-0700\ntarget_epoch=%s\n' \
  "$(date '+%Y-%m-%dT%H:%M:%S%z')" "$target_epoch" \
  > "$result_root/schedule/scheduled.receipt"

while [ "$(date +%s)" -lt "$target_epoch" ]; do
  remaining=$((target_epoch - $(date +%s)))
  if [ "$remaining" -gt 30 ]; then
    sleep 30
  else
    sleep "$remaining"
  fi
done

printf 'started_at=%s\ntarget_epoch=%s\n' "$(date '+%Y-%m-%dT%H:%M:%S%z')" "$target_epoch" \
  > "$result_root/schedule/start.receipt"

"$rig_root/bang-down.sh" > "$result_root/schedule/preflight-down.txt" 2>&1 || true

actual_fixture_sha=$(shasum -a 256 "$fixture" | awk '{print $1}')
printf 'expected=%s\nactual=%s\n' "$fixture_sha" "$actual_fixture_sha" \
  > "$result_root/schedule/fixture-preflight.sha256"
if [ "$actual_fixture_sha" != "$fixture_sha" ]; then
  printf 'fixture SHA mismatch\n' > "$result_root/schedule/FAILED"
  exit 3
fi

RATIO_MODEL=gpt-5.3-codex-spark \
RATIO_REASONING=low \
RATIO_REPLICATES=5 \
RATIO_CONDITIONS='C B' \
RATIO_COUNT_TO=200 \
RATIO_PROFILE=clean \
RATIO_NETWORK_CONTROL=0 \
  "$rig_root/worktree/bench/measure_prefill_decode_ratio.sh" \
  "$result_root/decode/raw" \
  > "$result_root/schedule/decode.stdout" \
  2> "$result_root/schedule/decode.stderr"
decode_exit=$?
printf '%s\n' "$decode_exit" > "$result_root/schedule/decode.exit"

set +e
BANG_MODEL=gpt-5.3-codex-spark "$rig_root/bang-up.sh" \
  > "$result_root/schedule/bang-up.txt" 2>&1
warm_exit=$?
set -e
printf '%s\n' "$warm_exit" > "$result_root/schedule/bang-up.exit"

if [ "$warm_exit" -eq 0 ]; then
  run_case() {
    local id=$1
    local owner=$2
    local decision=$3
    set +e
    "$rig_root/bang.sh" bench/fixtures/model_variant_battery.clj "$owner" "$decision" \
      > "$result_root/fill/console/$id.txt" 2>&1
    local rc=$?
    set -e
    cp "$rig_root/state/last-bang.json" "$result_root/fill/receipts/$id.json"
    printf '%s\n' "$rc" > "$result_root/fill/receipts/$id.exit"
  }

  run_case literal fill-literal 'Change :cold -> :warm.'
  run_case qualified-call fill-qualified-call 'Change legacy/transform -> modern/transform.'
  run_case map-value fill-map-value 'Change :draft -> :ready; preserve :retries.'
  run_case selected-arity fill-selected-arity \
    'Change the zero-arity return :old -> :new; preserve the one-arity behavior.'
  run_case thread-tail fill-thread-tail \
    'Change persist -> audit/persist; preserve the preceding thread steps.'
  run_case branch-call fill-branch-call \
    'Change render -> ui/render; preserve the nil branch.'
fi

"$rig_root/bang-down.sh" > "$result_root/schedule/postflight-down.txt" 2>&1 || true

if [ "$warm_exit" -eq 0 ] && [ "$decode_exit" -eq 0 ]; then
  cd "$experiment_root"
  bb bench/model-variant-battery/score_battery.clj \
    bench/model-variant-battery/results/spark > "$result_root/score.json"
  bb bench/model-variant-battery/score_battery.clj \
    bench/model-variant-battery/results/spark --markdown > "$result_root/SUMMARY.md"
  printf 'completed_at=%s\n' "$(date '+%Y-%m-%dT%H:%M:%S%z')" \
    > "$result_root/schedule/COMPLETE"
else
  printf 'decode_exit=%s\nwarm_exit=%s\n' "$decode_exit" "$warm_exit" \
    > "$result_root/schedule/FAILED"
  exit 4
fi
