#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/../../../.."
PYTHONDONTWRITEBYTECODE=1 python3 docs/quality/2026-11-22-baseline/bin/report.py branches
