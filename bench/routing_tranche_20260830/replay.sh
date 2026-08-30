#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")"
python3 run_screen.py self-test

for screen in native-description action-native-name minimal-schema refusal-handoff; do
  python3 run_screen.py freeze --screen "$screen"
  python3 run_screen.py pilot --screen "$screen"
  python3 run_screen.py cohort --screen "$screen"
  if [[ "$screen" == action-native-name ]] && jq -e '.verdict == "advance-to-fresh-replication"' "screens/$screen/aggregate.json" >/dev/null; then
    python3 run_screen.py replicate --screen "$screen"
  fi
  python3 run_screen.py archive --screen "$screen"
done
