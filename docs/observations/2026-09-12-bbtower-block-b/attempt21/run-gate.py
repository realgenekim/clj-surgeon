"""Retain one gate invocation and its machine receipts; never retry."""
import datetime
import json
import os
from pathlib import Path
import shutil
import subprocess
import sys
import time

out = Path(__file__).resolve().parent
mode = sys.argv[1]
assert mode in ('restricted', 'prewarm', 'prewarm-final')
command = ['make', 'landing-gate-prewarm']
if mode == 'restricted':
    command = ['python3', str(out.parent / 'attempt18/restricted-gate.py'), '--'] + command
env = dict(os.environ, TMPDIR='/var/tmp/forge/bbtower-fx',
           JAVA_TOOL_OPTIONS='-Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx')
env.pop('_JAVA_OPTIONS', None)
started = datetime.datetime.now(datetime.timezone.utc).isoformat()
start = time.monotonic()
with (out / (mode + '-gate.log')).open('x') as log:
    result = subprocess.run(command, env=env, stdout=log, stderr=subprocess.STDOUT)
wall = time.monotonic() - start
receipt = Path('target/landing-gate-prewarm.edn')
if receipt.exists():
    shutil.copy2(receipt, out / (mode + '-receipt.edn'))
run_id = Path('target/gate-prewarm/latest-run').read_text().strip()
shutil.copytree(Path('target/gate-prewarm') / run_id, out / (mode + '-run'))
record = dict(command=command, started=started, exit=result.returncode,
              wall_seconds=wall, diagnostic=mode == 'restricted', run_id=run_id)
(out / (mode + '-process.json')).write_text(json.dumps(record, indent=2) + '\n')
print(json.dumps(record), flush=True)
sys.exit(result.returncode)
