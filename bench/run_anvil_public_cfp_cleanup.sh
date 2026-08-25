#!/usr/bin/env bash
set -euo pipefail

script_dir=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)

exec bash "$script_dir/run_anvil_portfolio_pair.sh" \
  "${1:?usage: run_anvil_public_cfp_cleanup.sh RESULT_DIR ORDER [REPLICATES]}" \
  public-cfp-extraction-cleanup \
  "${2:?usage: run_anvil_public_cfp_cleanup.sh RESULT_DIR ORDER [REPLICATES]}" \
  "${3:-1}"
