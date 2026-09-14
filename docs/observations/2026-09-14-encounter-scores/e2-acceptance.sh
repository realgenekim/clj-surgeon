#!/bin/bash
# accepted iff the final Sol verdict on the state-home candidate is GO with no dependency-impact finding
[ "$(head -1 '/home/forge/src/clj-surgeon-records/docs/observations/2026-09-14-state-home-sol-fence-verdict-4-GO.md' | tr -d '[:space:]')" = "GO" ] || exit 2
grep -qiE 'impact oracle (missed|short)|namespaces the oracle missed' '/home/forge/src/clj-surgeon-records/docs/observations/2026-09-14-state-home-sol-fence-verdict-4-GO.md' && exit 3
exit 0
