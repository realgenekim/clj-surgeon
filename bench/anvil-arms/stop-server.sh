#!/usr/bin/env bash
# stop-server.sh — stop ONLY the MCP server this arm-run actually spawned.
#
#   stop-server.sh <ARMDIR> [signal]
#
# It signals the pid recorded in <ARMDIR>/server/spawned.pid, and only if that pid is
# still the SAME PROCESS: the file carries the pid AND the process start time taken
# from /proc/<pid>/stat at spawn, and both must match now.
#
# Sol, item 11: cleanup used to read whatever pid `ready.edn` happened to hold, with
# SERVER_STARTED=1 as its only warrant.  A stale ready.edn -- or one another arm wrote
# -- would have made this script signal a process it never started.  ready.edn is the
# SERVER's claim about itself; spawned.pid is THIS SCRIPT's record of what it forked,
# and only the second is evidence of authorship.  A pid alone is not identity either:
# pids are reused, and the start time is what distinguishes our server from whatever
# now wears its number.
#
# Exit 0 the recorded server was signalled (or had already exited); 3 nothing was
# signalled and why is printed.  It never signals on a doubt.
set -uo pipefail

A=${1:?usage: stop-server.sh <ARMDIR> [signal]}
SIG=${2:-TERM}
REC="$A/server/spawned.pid"

if [ ! -s "$REC" ]; then
  echo "stop-server: no recorded spawn at $REC — NOTHING was signalled" >&2
  exit 3
fi

read -r PID RECORDED_START _ < "$REC"
case "${PID:-}" in
  ''|*[!0-9]*) echo "stop-server: unusable pid '${PID:-}' in $REC — NOTHING was signalled" >&2; exit 3;;
esac
case "${RECORDED_START:-}" in
  ''|*[!0-9]*) echo "stop-server: no recorded start time in $REC — NOTHING was signalled" >&2; exit 3;;
esac

if [ ! -r "/proc/$PID/stat" ]; then
  echo "stop-server: pid $PID is gone; the server it named already exited"
  exit 0
fi

# field 22 of /proc/<pid>/stat is starttime.  Everything up to the LAST ')' is the
# pid and the comm (which may itself contain spaces and parentheses), so the tail
# begins at field 3 and starttime is its 20th token.
LIVE_START=$(cut -d')' -f2- "/proc/$PID/stat" 2>/dev/null | awk '{print $20}')
if [ "$LIVE_START" != "$RECORDED_START" ]; then
  echo "stop-server: start-time-mismatch pid=$PID recorded=$RECORDED_START live=$LIVE_START" >&2
  echo "stop-server: pid $PID has been REUSED by another process — NOTHING was signalled" >&2
  exit 3
fi

if kill -"$SIG" "$PID" 2>/dev/null; then
  echo "stop-server: stopped server pid=$PID (spawned by this arm-run, start time $RECORDED_START)"
  exit 0
fi
echo "stop-server: pid $PID matched the record but could not be signalled" >&2
exit 3
