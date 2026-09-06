"""Prospective raw cohort. Importing this module performs no IO or dispatch."""
import argparse
import hashlib
import importlib.machinery
import importlib.util
import json
import math
import os
from pathlib import Path
import re
import signal
import statistics
import subprocess
import sys
import time
from datetime import datetime, timezone

REPO = Path('/var/tmp/forge/astra-typist-route-fx')
BASE = Path('/var/tmp/forge/astra-raw-cohort-v2-fx')
SESSION = '01a07465-7a8c-7423-b598-cbac0681c1f5'
TEMPLATE = Path('/var/tmp/forge/astra-raw-live-fx/spec.edn')
PREREG = Path(__file__).resolve().parents[2] / 'docs/observations/2026-09-06-astra-raw-cohort-v2-prereg.md'
PHASE_PROMPT = (
    'Orientation is over. The earlier read-only instruction to reply READY applied '
    'only to orientation and no longer applies. The workspace has been reset to '
    'the original file. Complete the actual edit now exactly as specified in the '
    'task. Edit the file in place; any script or batched patch is allowed. Run the '
    'required verification and KEEP the verified diff in the workspace. Do not '
    'revert the edit to return to orientation. Reply DONE when finished.')


class ApparatusFault(RuntimeError):
    pass


def sha(path):
    return hashlib.sha256(Path(path).read_bytes()).hexdigest()


def write_new(path, value):
    with Path(path).open('x') as stream:
        json.dump(value, stream, indent=2)
        stream.write('\n')


def control_floor(rows):
    if len(rows) != 6 or [(r['arm'], r['pair']) for r in rows] != [('C', i) for i in range(1, 7)]:
        raise ApparatusFault('exactly-six-fixed-controls-required')
    walls = [r['wall_s'] for r in rows]
    if any(type(w) not in (int, float) or not math.isfinite(w) or w < 0 for w in walls):
        raise ApparatusFault('invalid-terminal-wall')
    if any(type(r.get('correct')) is not bool or r.get('fault') for r in rows):
        raise ApparatusFault('incomplete-control-evidence')
    median, sd = statistics.median(walls), statistics.stdev(walls)
    return {'n': 6, 'verified': sum(r['correct'] for r in rows),
            'failed': sum(not r['correct'] for r in rows), 'walls': walls,
            'median': median, 'sample_sd': sd, 'predicted_tool_median': 10,
            'predicted_gain_clears_two_sd': median - 10 > 2 * sd,
            'wall_population': 'all-six-terminal-attempts',
            'success_conditional_latency': 'not used for floor'}


def execute_schedule(run_one, save_row, save_floor):
    """No retries/replacements. Ordinary wrong results are observations."""
    controls = []
    for i in range(1, 7):
        row = run_one('C', i)
        save_row(row)
        if row.get('fault'):
            raise ApparatusFault(row['fault'])
        controls.append(row)
    # The old driver dispatched N1 before this check. Nothing dispatches here.
    save_floor(control_floor(controls))
    for i, order in enumerate(('NT', 'TN', 'NT', 'TN'), 1):
        for arm in order:
            row = run_one(arm, i)
            save_row(row)
            if row.get('fault'):
                raise ApparatusFault(row['fault'])


def load_native():
    os.environ['TYPIST_FX'] = '/var/tmp/forge/typist-real-fx'
    os.environ['TYPIST_REAL1_REPO'] = str(REPO)
    os.environ['PYTHONDONTWRITEBYTECODE'] = '1'
    loader = importlib.machinery.SourceFileLoader('raw_cohort_native', str(REPO / 'bin/typist-run'))
    spec = importlib.util.spec_from_loader(loader.name, loader)
    module = importlib.util.module_from_spec(spec)
    loader.exec_module(module)
    return module


def native_entry(expected_sha):
    if sha(REPO / 'bin/typist-run') != expected_sha:
        raise ApparatusFault('native-runner-identity-changed')
    module = load_native()
    module.WARM_TRIAL_PROMPT = PHASE_PROMPT
    sys.argv = [str(REPO / 'bin/typist-run'), '--arm', 'NW', '--mission', 'real-1',
                '--warm-session', SESSION, '--runs', '1', '--k', '1']
    module.main()


def ednjson(path):
    result = subprocess.run(['bb', '-e',
        '(require (quote [clojure.edn :as e]) (quote [cheshire.core :as j])) '
        '(println (j/generate-string (e/read-string (slurp (first *command-line-args*)))))',
        str(path)], capture_output=True, text=True, check=True, timeout=30)
    return json.loads(result.stdout)


def prepare():
    BASE.mkdir(exist_ok=False)
    module = load_native()
    seed = module.real1_seed()
    for i in range(1, 5):
        directory = BASE / ('T' + str(i))
        workspace = directory / 'workspace'
        workspace.mkdir(parents=True)
        for rel, data in seed.items():
            path = workspace / rel
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text(data)
        code = ('(require (quote [clojure.edn :as e])) '
                '(let [[src root dst] *command-line-args* s (e/read-string (slurp src))] '
                '(spit dst (pr-str (assoc-in s [:request :workspace_root] root))))')
        subprocess.run(['bb', '-e', code, str(TEMPLATE), str(workspace), str(directory / 'spec.edn')],
                       check=True, timeout=30)
    (BASE / 'native-phase-prompt.txt').write_text(PHASE_PROMPT + '\n')
    files = [REPO / x for x in ('bin/mission', 'bin/typist-run', 'bin/typist_transport.py',
                               'bin/typist-dossier-real-1.md')]
    files += [Path(__file__).resolve(), PREREG, TEMPLATE, BASE / 'native-phase-prompt.txt']
    files += list(BASE.glob('T*/spec.edn'))
    frozen = {'at': datetime.now(timezone.utc).isoformat(), 'engine': head(),
              'hashes': {str(p): sha(p) for p in files},
              'seed': {k: hashlib.sha256(v.encode()).hexdigest() for k, v in seed.items()},
              'session': SESSION, 'native_model': 'gpt-5.6-sol', 'native_effort': 'medium'}
    write_new(BASE / 'frozen.json', frozen)


def head():
    return subprocess.check_output(['git', 'rev-parse', 'HEAD'], cwd=REPO, text=True).strip()


def check_identity(frozen):
    if (head() != frozen['engine']
        or subprocess.run(['git', 'diff', '--quiet', 'HEAD'], cwd=REPO).returncode != 0
        or any(sha(p) != v for p, v in frozen['hashes'].items())):
        raise ApparatusFault('frozen-identity-changed')
    quiet = Path('/var/tmp/forge/quiet-window.md')
    if not quiet.exists() or 'owner=astra' not in quiet.read_text():
        raise ApparatusFault('owned-quiet-window-required')


def native_correct(receipt, dossier_sha):
    if (receipt.get('arm'), receipt.get('mission'), receipt.get('warm_session'), receipt.get('model'),
        receipt.get('dossier_sha256')) != ('NW', 'real-1', SESSION, 'gpt-5.6-sol', dossier_sha):
        raise ApparatusFault('native-receipt-identity-mismatch')
    candidates = receipt.get('candidates')
    if not isinstance(candidates, list) or len(candidates) != 1:
        raise ApparatusFault('native-candidate-evidence-missing')
    if candidates[0].get('error') or candidates[0].get('refusal'):
        raise ApparatusFault('native-transport-refusal-needs-review')
    # No diff, wrong patch, failed acceptance are genuine native results.
    return receipt.get('first_verified_s') is not None and not receipt.get('semantic_mismatch')


def run(frozen_sha):
    if sha(BASE / 'frozen.json') != frozen_sha:
        raise ApparatusFault('parent-reviewed-freeze-required')
    frozen = json.loads((BASE / 'frozen.json').read_text())
    check_identity(frozen)
    module = load_native()
    seed = module.real1_seed()
    with (BASE / 'results.jsonl').open('x') as results:
        def save(row):
            results.write(json.dumps(row) + '\n')
            results.flush()

        def one(arm, i):
            row = {'arm': arm, 'pair': i, 'correct': None, 'fault': None,
                   'started': datetime.now(timezone.utc).isoformat()}
            try:
                check_identity(frozen)
                directory = BASE / (arm + str(i))
                directory.mkdir(exist_ok=(arm == 'T'))
                cmd = ([str(REPO / 'bin/mission'), 'run', '--spec-file', str(directory / 'spec.edn'),
                        '--state-home', str(directory / 'state'), '--receipt-dir', str(directory / 'receipts')]
                       if arm == 'T' else [sys.executable, str(Path(__file__).resolve()), 'native',
                                           '--runner-sha', frozen['hashes'][str(REPO / 'bin/typist-run')]])
                row.update(argv=cmd, load1=os.getloadavg()[0])
                start = time.monotonic()
                with (directory / 'stdout').open('x') as out, (directory / 'stderr').open('x') as err:
                    proc = subprocess.Popen(cmd, cwd=REPO, stdout=out, stderr=err, start_new_session=True)
                    try:
                        row['exit'] = proc.wait(timeout=180)
                    except subprocess.TimeoutExpired:
                        os.killpg(proc.pid, signal.SIGKILL)
                        proc.wait()
                        row['exit'] = 'timeout'
                row['wall_s'] = time.monotonic() - start
                if row['exit'] == 'timeout':
                    raise ApparatusFault('terminal-receipt-unavailable-after-watchdog')
                if arm == 'T':
                    receipt = ednjson(directory / 'stdout')
                    row['receipt'] = receipt
                    workspace = directory / 'workspace'
                    protected = all((workspace / k).read_bytes() == v.encode()
                                    for k, v in seed.items() if k != module.REAL1_SRC)
                    row['proof_bytes_unchanged'] = protected
                    row['correct'] = (receipt.get('state') == 'verified'
                                      and receipt.get('receipt', {}).get('verification-complete') is True
                                      and protected)
                    with (directory / 'candidate-source.clj').open('xb') as out:
                        out.write((workspace / module.REAL1_SRC).read_bytes())
                else:
                    matches = re.findall(r'run=(\S+)', (directory / 'stdout').read_text())
                    if len(matches) != 1 or Path(matches[0]).name != matches[0]:
                        raise ApparatusFault('native-receipt-path-missing-or-ambiguous')
                    path = Path('/var/tmp/forge/typist-real-fx') / matches[0] / 'receipt.edn'
                    row.update(receipt_path=str(path), receipt=ednjson(path))
                    row['correct'] = native_correct(row['receipt'], frozen['hashes'][str(REPO / 'bin/typist-dossier-real-1.md')])
                check_identity(frozen)
            except Exception as error:
                row['fault'] = str(error) if isinstance(error, ApparatusFault) else 'apparatus-exception-' + type(error).__name__
            return row

        execute_schedule(one, save, lambda floor: write_new(BASE / 'floor.json', floor))


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('operation', choices=['prepare', 'run', 'native'])
    parser.add_argument('--frozen-sha')
    parser.add_argument('--runner-sha')
    args = parser.parse_args()
    if args.operation == 'prepare':
        prepare()
    elif args.operation == 'native':
        native_entry(args.runner_sha)
    elif not args.frozen_sha:
        parser.error('run requires the parent-reviewed --frozen-sha')
    else:
        run(args.frozen_sha)
