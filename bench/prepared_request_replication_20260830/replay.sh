#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")"
python3 run_experiment.py self-test
python3 run_experiment.py freeze
python3 run_experiment.py preflight
python3 run_experiment.py cohort
python3 run_experiment.py archive
