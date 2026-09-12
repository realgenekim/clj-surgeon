"""Attempt 15's Landlock envelope, with the caller's TMPDIR retained."""
from pathlib import Path
import os
import subprocess

# Reuse the exact restriction implementation without executing its old runner.
source = Path(__file__).parents[1] / 'attempt15/restricted-oracle.py'
namespace = {}
exec(compile(source.read_text().split('\nwith tempfile.TemporaryDirectory')[0],
             str(source), 'exec'), namespace)
root = os.environ['TMPDIR']
print('Only writable root:', root, flush=True)
env = dict(os.environ)
env.pop('CLJ_SURGEON_GATE_ROOT', None)
env.pop('GATE_SLOT_BACKEND', None)
result = subprocess.run(
    ['python3', '-B', '-m', 'unittest', 'discover', '-v', '-s', 'test/oracles',
     '-p', 'test_gate_slot.py'], env=env,
    preexec_fn=lambda: namespace['restrict'](root))
print('exit:', result.returncode, flush=True)
raise SystemExit(result.returncode)
