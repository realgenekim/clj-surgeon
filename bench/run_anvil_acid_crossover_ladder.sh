#!/usr/bin/env bash
set -euo pipefail

result_root=${1:?usage: run_anvil_acid_crossover_ladder.sh RESULT_ROOT PRODUCT_COMMIT}
product_commit=${2:?usage: run_anvil_acid_crossover_ladder.sh RESULT_ROOT PRODUCT_COMMIT}

if ! git diff --quiet || ! git diff --cached --quiet; then
  echo "Acid crossover ladder requires a clean harness worktree" >&2
  exit 2
fi

product_commit=$(git rev-parse --verify "$product_commit^{commit}")
harness_commit=$(git rev-parse --verify HEAD)

mkdir -p "$result_root"
printf '%s\t%s\n' \
  harness_commit "$harness_commit" \
  product_commit "$product_commit" \
  model gpt-5.6-sol \
  reasoning high \
  schedule '03:SNNS;08:NSSN;16:SNNS;32:NSSN' \
  > "$result_root/protocol.tsv"

run_pair() {
  local rung=$1 pair=$2 order=$3
  local task="acid-crossover-$rung"
  local pair_dir="$result_root/rung-$rung/pair-$pair-$order"
  BENCH_POST_COMMIT="$product_commit" \
    bash bench/run_anvil_portfolio_pair.sh "$pair_dir" "$task" "$order" 1
}

# Fixed before launch. Each rung is serial and position-balanced; the leading
# arm alternates across rungs to reduce drift coupling.
run_pair 03 1 compact-first
run_pair 03 2 native-first
run_pair 08 1 native-first
run_pair 08 2 compact-first
run_pair 16 1 compact-first
run_pair 16 2 native-first
run_pair 32 1 native-first
run_pair 32 2 compact-first

find "$result_root" -type f ! -name MANIFEST.sha256 -print0 \
  | sort -z \
  | xargs -0 shasum -a 256 \
  > "$result_root/MANIFEST.sha256"

printf 'Acid crossover ladder complete: %s\n' "$result_root"
