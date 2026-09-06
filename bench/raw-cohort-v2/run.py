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
import stat
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


def parse_native_header(raw, expected):
    """Parse only the CLI's opening stderr header, never a later model quotation."""
    marker = b'\n----- stderr -----\n'
    if len(raw) > 16 * 1024 * 1024 or raw.count(marker) != 1:
        raise ApparatusFault('native-stderr-boundary-missing-or-ambiguous')
    stderr = raw.split(marker, 1)[1]
    header, boundary, _ = stderr.partition(b'\n--------\n')
    if not boundary or not header.startswith(b'OpenAI Codex v') or len(header) > 128:
        raise ApparatusFault('native-cli-opening-header-missing')
    rest = stderr[len(header) + len(boundary):]
    fields_raw, closing, _ = rest.partition(b'\n--------\n')
    if not closing or len(fields_raw) > 4096:
        raise ApparatusFault('native-cli-header-unbounded-or-incomplete')
    fields = {}
    try:
        for line in fields_raw.decode('utf-8').splitlines():
            key, separator, value = line.partition(': ')
            if not separator or key in fields:
                raise ValueError('duplicate or malformed field')
            fields[key] = value
        version = header.decode('ascii').removeprefix('OpenAI Codex v')
    except (UnicodeError, ValueError):
        raise ApparatusFault('native-cli-header-malformed') from None
    if set(fields) != {'workdir', 'model', 'provider', 'approval', 'sandbox',
                       'reasoning effort', 'reasoning summaries', 'session id'}:
        raise ApparatusFault('native-cli-header-fields-unexpected')
    values = {**fields, 'cli_version': version}
    if any(values.get(k) != v for k, v in expected.items()):
        raise ApparatusFault('native-resolved-cli-identity-mismatch')
    fork = fields.get('session id', '')
    if not re.fullmatch(r'[0-9a-f]{8}(?:-[0-9a-f]{4}){3}-[0-9a-f]{12}', fork) or fork == SESSION:
        raise ApparatusFault('native-fork-session-missing-or-not-forked')
    return {**values, 'fork_session_id': fork, 'orientation_session_id': SESSION,
            'header_sha256': hashlib.sha256(header + boundary + fields_raw + closing).hexdigest(),
            'capture_sha256': hashlib.sha256(raw).hexdigest(),
            'authority': 'opening CLI stderr header; not arbitrary transcript text'}


def native_capture(path, expected):
    path = Path(path)
    if path.is_symlink() or not path.is_file() or path.stat().st_size > 16 * 1024 * 1024:
        raise ApparatusFault('native-capture-missing-or-out-of-bounds')
    return parse_native_header(path.read_bytes(), expected)


def fixture_inventory(root):
    """Exact regular-file bytes/modes and directory modes; no followed links."""
    root = Path(root)
    if root.is_symlink() or root.resolve() != root.absolute() or not root.is_dir():
        raise ApparatusFault('fixture-root-not-owned-directory')
    result = {'files': {}, 'directories': {'': stat.S_IMODE(root.stat().st_mode)}}
    def visit(directory):
        for entry in sorted(directory.iterdir()):
            rel = str(entry.relative_to(root))
            mode = entry.lstat().st_mode
            if stat.S_ISLNK(mode):
                raise ApparatusFault('fixture-symlink-forbidden')
            if stat.S_ISDIR(mode):
                result['directories'][rel] = stat.S_IMODE(mode)
                visit(entry)
            elif stat.S_ISREG(mode):
                if entry.stat().st_size > 1024 * 1024:
                    raise ApparatusFault('fixture-file-out-of-bounds')
                result['files'][rel] = {'sha256': sha(entry), 'mode': stat.S_IMODE(mode)}
            else:
                raise ApparatusFault('fixture-special-file-forbidden')
            if len(result['files']) + len(result['directories']) > 64:
                raise ApparatusFault('fixture-inventory-out-of-bounds')
    visit(root)
    return result


def check_fixture(root, expected):
    if fixture_inventory(root) != expected:
        raise ApparatusFault('tool-fixture-seed-bytes-modes-or-files-changed')


def tool_evidence_fault(view, closed):
    """Policy is based on typed evidence, never generic false gate attribution."""
    receipt = view.get('receipt') if isinstance(view, dict) else None
    if not isinstance(receipt, dict) or not isinstance(receipt.get('candidates'), list):
        return 'tool-terminal-candidate-evidence-missing'
    if not isinstance(closed, dict) or closed.get('terminated?') is not True:
        return 'tool-transport-cleanup-unconfirmed'
    completed = closed.get('completed')
    if not isinstance(completed, list) or not 1 <= len(completed) <= 3:
        return 'tool-completed-transport-evidence-missing'
    content_failures = {'provider-refusal', 'output-length', 'empty-content', 'nonterminal-output'}
    cancelled = closed.get('cancelled')
    if (not isinstance(cancelled, list) or len(cancelled) > 3
        or any(type(i) is not int or not 0 <= i < 3 for i in cancelled)
        or closed.get('live-processes')):
        return 'tool-transport-cleanup-evidence-malformed'
    indices = [c.get('index') if isinstance(c, dict) else None for c in completed]
    if any(type(i) is not int or not 0 <= i < 3 for i in indices) or len(set(indices)) != len(indices):
        return 'tool-transport-candidate-identities-malformed'
    for candidate in completed:
        if not isinstance(candidate, dict):
            return 'tool-transport-record-malformed'
        error = candidate.get('error_type')
        if error == 'candidate-request-interrupted' and candidate.get('index') in cancelled:
            continue  # Accepted loser cancellation is not a provider success or free usage.
        if error and error not in content_failures:
            return 'tool-transport-fault-needs-review'
        if (candidate.get('model'), candidate.get('upstream')) != ('openai/gpt-oss-120b', 'Cerebras'):
            return 'tool-resolved-provider-identity-mismatch'
        if not error and candidate.get('usable') is not True:
            return 'tool-transport-result-malformed'
    for candidate in receipt['candidates']:
        if (not isinstance(candidate, dict) or candidate.get('index') not in indices
            or type(candidate.get('compiled')) is not bool):
            return 'tool-candidate-proof-evidence-malformed'
        if candidate['compiled']:
            proof = candidate.get('proof')
            if not isinstance(proof, dict) or type(proof.get('ok')) is not bool:
                return 'tool-proof-evidence-missing'
            initial_gate = proof.get('gate')
            if not isinstance(initial_gate, dict):
                return 'tool-profile-evidence-missing'
            for profile in ['gate', 'acceptance'] if initial_gate.get('ok') else ['gate']:
                gate = proof.get(profile)
                if not isinstance(gate, dict) or type(gate.get('ok')) is not bool:
                    return 'tool-profile-evidence-missing'
                results = gate.get('results')
                if not isinstance(results, list) or not results:
                    return 'tool-profile-result-evidence-missing'
                for result in results:
                    if not isinstance(result, dict) or type(result.get('finished?')) is not bool:
                        return 'tool-profile-result-malformed'
                    if result['finished?'] and type(result.get('exit')) is not int:
                        return 'tool-profile-exit-missing'
                # Finished nonzero or candidate proof timeout are failed candidates;
                # neither proves that the proof apparatus itself is broken.
    return None


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
    path = Path(path)
    if path.is_symlink() or not path.is_file() or path.stat().st_size > 16 * 1024 * 1024:
        raise ApparatusFault('edn-evidence-missing-or-out-of-bounds')
    result = subprocess.run(['bb', '-e',
        '(require (quote [clojure.edn :as e]) (quote [cheshire.core :as j])) '
        '(println (j/generate-string (e/read-string (slurp (first *command-line-args*)))))',
        str(path)], capture_output=True, text=True, check=True, timeout=30)
    return json.loads(result.stdout)


def prepare():
    BASE.mkdir(exist_ok=False)
    module = load_native()
    seed = module.real1_seed()
    inventories = []
    for i in range(1, 5):
        directory = BASE / ('T' + str(i))
        workspace = directory / 'workspace'
        workspace.mkdir(parents=True)
        for rel, data in seed.items():
            path = workspace / rel
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text(data)
            path.chmod(0o644)
        for directory_path in [workspace, *(p for p in workspace.rglob('*') if p.is_dir())]:
            directory_path.chmod(0o755)
        inventories.append(fixture_inventory(workspace))
        code = ('(require (quote [clojure.edn :as e])) '
                '(let [[src root dst] *command-line-args* s (e/read-string (slurp src))] '
                '(spit dst (pr-str (assoc-in s [:request :workspace_root] root))))')
        subprocess.run(['bb', '-e', code, str(TEMPLATE), str(workspace), str(directory / 'spec.edn')],
                       check=True, timeout=30)
    if any(value != inventories[0] for value in inventories):
        raise ApparatusFault('prepared-seeds-disagree')
    (BASE / 'native-phase-prompt.txt').write_text(PHASE_PROMPT + '\n')
    files = [REPO / x for x in ('bin/mission', 'bin/typist-run', 'bin/typist_transport.py',
                               'bin/typist-dossier-real-1.md')]
    files += [Path(__file__).resolve(), PREREG, TEMPLATE, BASE / 'native-phase-prompt.txt']
    files += list(BASE.glob('T*/spec.edn'))
    frozen = {'at': datetime.now(timezone.utc).isoformat(), 'engine': head(),
              'hashes': {str(p): sha(p) for p in files},
              'seed': {k: hashlib.sha256(v.encode()).hexdigest() for k, v in seed.items()},
              'seed_inventory': inventories[0],
              'native_header': {'cli_version': '0.153.3',
                                'workdir': '/var/tmp/forge/typist-real-fx/warm-ws-real-1',
                                'model': 'gpt-5.6-sol', 'provider': 'openai',
                                'reasoning effort': 'medium', 'approval': 'never',
                                'sandbox': 'danger-full-access'},
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
    if (not isinstance(candidates, list) or len(candidates) != 1
        or not isinstance(candidates[0], dict)
        or type(candidates[0].get('verified')) is not bool
        or 'first_verified_s' not in receipt or 'semantic_mismatch' not in receipt):
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
    seen_forks = set()
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
                if arm == 'T':
                    check_fixture(directory / 'workspace', frozen['seed_inventory'])
                    row['seed_inventory_verified_before_dispatch'] = True
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
                    artifact_dir = Path(receipt.get('receipt', {}).get('artifacts', ''))
                    if not artifact_dir.resolve().is_relative_to((directory / 'receipts').resolve()):
                        raise ApparatusFault('tool-artifact-path-outside-arm')
                    closed = ednjson(artifact_dir / 'transport-close.edn')
                    row['transport_close'] = closed
                    fault = tool_evidence_fault(receipt, closed)
                    if fault:
                        raise ApparatusFault(fault)
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
                    row['native_capture_path'] = str(path.parent / 'nw-stdout.txt')
                    attestation = native_capture(row['native_capture_path'], frozen['native_header'])
                    row['native_cli_attestation'] = attestation
                    if attestation['fork_session_id'] in seen_forks:
                        raise ApparatusFault('native-fork-session-reused')
                    seen_forks.add(attestation['fork_session_id'])
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
