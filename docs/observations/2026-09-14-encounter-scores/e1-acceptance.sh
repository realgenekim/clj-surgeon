#!/bin/bash
# accepted iff the state-home fix landed on trunk AND its final Sol verdict is GO (names no envelope-class finding)
git -C /home/forge/src/clj-surgeon-land merge-base --is-ancestor f1433d82 origin/MCP/main || exit 1
[ "$(head -1 '/home/forge/src/clj-surgeon-records/docs/observations/2026-09-14-state-home-sol-fence-verdict-4-GO.md' | tr -d '[:space:]')" = "GO" ] || exit 2
exit 0
