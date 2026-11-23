#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/../../../.."
export PYTHONDONTWRITEBYTECODE=1
python3 docs/quality/2026-11-22-baseline/bin/report.py baseline
bash docs/quality/2026-11-22-baseline/bin/branches.sh
find docs/quality/2026-11-22-baseline -type f | sort
