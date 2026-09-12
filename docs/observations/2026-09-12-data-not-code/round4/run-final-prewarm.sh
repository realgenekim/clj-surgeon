#!/usr/bin/env bash
set -u
cd /home/forge/src/clj-surgeon-datacode
export TMPDIR=/var/tmp/forge/datacode-fx
export JAVA_TOOL_OPTIONS='-Xmx1024m -Djava.io.tmpdir=/var/tmp/forge/datacode-fx'
round4=docs/observations/2026-09-12-data-not-code/round4
exec 9>/home/forge/tmp/suite-1.lock
flock 9
if compgen -G '/var/tmp/forge/jvm-lease*' >/dev/null; then exit 90; fi
lease=/var/tmp/forge/jvm-lease-datacode-round4
printf '%s\n' "$$" > "$lease"
trap 'rm -f "$lease"' EXIT
date -u +%FT%TZ > "$round4/final-lock-acquired.txt"
python3 - "$round4" <<'PY'
import json, pathlib, subprocess, sys, time
root = pathlib.Path(sys.argv[1])
start = time.monotonic_ns()
with (root / 'prewarm-final.log').open('w') as log:
    result = subprocess.run(['make', 'landing-gate-prewarm'], stdout=log, stderr=subprocess.STDOUT)
elapsed = (time.monotonic_ns() - start) // 1_000_000
(root / 'prewarm-final-command.json').write_text(json.dumps({'argv': ['make', 'landing-gate-prewarm'], 'exit': result.returncode, 'wall_ms': elapsed}) + '\n')
sys.exit(result.returncode)
PY
status=$?
date -u +%FT%TZ > "$round4/final-lock-released.txt"
exit "$status"
