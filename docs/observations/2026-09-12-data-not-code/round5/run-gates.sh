#!/usr/bin/env bash
set -eu
cd /home/forge/src/clj-surgeon-datacode
export TMPDIR=/var/tmp/forge/datacode-fx
export JAVA_TOOL_OPTIONS='-Xmx1024m -Djava.io.tmpdir=/var/tmp/forge/datacode-fx'
round5=docs/observations/2026-09-12-data-not-code/round5
exec 9>/home/forge/tmp/suite-1.lock
date -u +%FT%TZ > "$round5/gate-lock-requested.txt"
flock 9
if compgen -G '/var/tmp/forge/jvm-lease*' >/dev/null; then exit 90; fi
lease=/var/tmp/forge/jvm-lease-datacode-round5
printf '%s\n' "$$" > "$lease"
trap 'rm -f "$lease"' EXIT
date -u +%FT%TZ > "$round5/gate-lock-acquired.txt"
python3 - "$round5" "${1:-initial}" <<'PY'
import json,pathlib,subprocess,sys,time
root=pathlib.Path(sys.argv[1])
mode=sys.argv[2]
assert mode in ['initial','final']
checks=[('test-fast',['make','test-fast']),('prewarm',['make','landing-gate-prewarm'])] if mode=='initial' else [('prewarm-final',['make','landing-gate-prewarm'])]
for name,argv in checks:
    # Refuse a second invocation under the same name; do not erase failed evidence.
    with (root/(name+'.log')).open('x') as out:
        start=time.monotonic_ns()
        r=subprocess.run(argv,stdout=out,stderr=subprocess.STDOUT)
    (root/(name+'-command.json')).write_text(json.dumps(dict(argv=argv,exit=r.returncode,wall_ms=(time.monotonic_ns()-start)/1e6))+'\n')
    if r.returncode:
        sys.exit(r.returncode)
PY
date -u +%FT%TZ > "$round5/gate-lock-released.txt"
