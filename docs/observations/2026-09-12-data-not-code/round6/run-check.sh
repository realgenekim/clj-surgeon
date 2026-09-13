#!/usr/bin/env bash
set -eu
cd /home/forge/src/clj-surgeon-datacode
export TMPDIR=/var/tmp/forge/datacode-fx
export JAVA_TOOL_OPTIONS='-Xmx1024m -Djava.io.tmpdir=/var/tmp/forge/datacode-fx'
export CLJ_SURGEON_MEMORY_TMP="$TMPDIR"
export PYTHONDONTWRITEBYTECODE=1
exec 9>/home/forge/tmp/suite-1.lock
flock 9
if compgen -G '/var/tmp/forge/jvm-lease*' >/dev/null ||
   test "$(ps -eo args | grep -c '[r]eceipt-chain' || true)" != 0; then
  echo 'Check refused: lease or receipt-chain process active' >&2
  exit 90
fi
lease=/var/tmp/forge/jvm-lease-datacode-round6
printf '%s\n' "$$" > "$lease"
trap 'rm -f "$lease"' EXIT
python3 - "$@" <<'PY'
import json, pathlib, subprocess, sys, time
root = pathlib.Path('docs/observations/2026-09-12-data-not-code/round6')
name, *argv = sys.argv[1:]
assert name and '/' not in name and argv
start_utc = time.time_ns()
start = time.monotonic_ns()
with (root / (name + '.log')).open('x') as out:
    result = subprocess.run(argv, stdout=out, stderr=subprocess.STDOUT)
with (root / (name + '-command.json')).open('x') as out:
    json.dump(dict(argv=argv, exit=result.returncode, started_utc_ns=start_utc,
                   wall_ms=(time.monotonic_ns()-start)/1e6), out)
    out.write('\n')
sys.exit(result.returncode)
PY
