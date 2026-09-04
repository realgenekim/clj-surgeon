#!/usr/bin/env python3
"""Parent-only slot/load/independent acceptance shell around the frozen adapter."""
import argparse
import hashlib
import importlib.util
import json
import os
import shutil
import signal
from pathlib import Path
import subprocess
import sys
import time
from datetime import datetime, timezone

ROOT = Path('/var/tmp/forge/astra-program')
ADAPTER = ROOT / 'repo/bench/astra/adapter.py'
ORACLE = ROOT / 'repo/bench/fanout/rescore-FAN.sh'
SLOT = '/home/forge/bin/slot'
BASE = '65fe39a9071083f478ed091ab64ebdf05c02abbd'


def stamp():
    return {'utc': datetime.now(timezone.utc).isoformat(), 'monotonic_s': time.monotonic(),
            'loadavg': Path('/proc/loadavg').read_text().strip()}


def write(path, data):
    with path.open('x') as out:
        json.dump(data, out, indent=2, sort_keys=True)
        out.write('\n')


def sha(path):
    h = hashlib.sha256()
    with path.open('rb') as source:
        for block in iter(lambda: source.read(1048576), b''):
            h.update(block)
    return h.hexdigest()


def confined(path):
    path = Path(path).resolve()
    if path == ROOT or not path.is_relative_to(ROOT):
        raise ValueError('must be below ' + str(ROOT))
    return path


def guards(wt):
    result = {}
    for root in [wt / 'test', wt / 'bin/fan-test']:
        if root.is_symlink() or (wt / 'bin').is_symlink():
            raise ValueError('guard root symlink: ' + str(root))
        if not root.exists():
            raise ValueError('missing guard: ' + str(root))
        for p in ([root] if root.is_file() else root.rglob('*')):
            if p.is_symlink():
                raise ValueError('guard symlink: ' + str(p))
            if p.is_file():
                result[str(p.relative_to(wt))] = [sha(p), p.stat().st_mode & 0o777]
    return result


def validate_frozen():
    frozen = json.loads((ROOT / 'FROZEN.json').read_text())
    for path, expected in frozen['hashes'].items():
        if sha(Path(path)) != expected:
            raise ValueError('frozen input changed: ' + path)
    return frozen


def tree_hashes(root):
    if root.is_symlink():
        raise ValueError('oracle root symlink: ' + str(root))
    result = {}
    for p in root.rglob('*'):
        if p.is_symlink():
            raise ValueError('oracle symlink: ' + str(p))
        if p.is_file():
            result[str(p.relative_to(root))] = sha(p)
    return result


def verify_oracle(receipt, plan):
    if tree_hashes(receipt / 'oracle-fixtures') != plan['oracle_hashes']:
        raise ValueError('oracle snapshot changed')


def bounded(argv, env, log, timeout_s, cwd=None):
    """One dedicated worker's children only; same identity helpers as frozen watcher."""
    spec = importlib.util.spec_from_file_location('frozen_watch_lifecycle', ADAPTER.parent.parent / 'anvil-arms/watch.py')
    watch = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(watch)
    # No unrelated child can be swept into this operation's cleanup set.
    if set(watch.descendants_of(os.getpid())) - {os.getpid()}:
        return {'returncode': 126, 'terminal': 'supervisor-not-isolated', 'survivors': []}
    subreaper = watch.set_child_subreaper()
    if subreaper != 'ok':
        return {'returncode': 126, 'terminal': subreaper, 'survivors': []}
    try:
        proc = subprocess.Popen(argv, cwd=cwd, env=env, stdout=log,
                                stderr=subprocess.STDOUT, start_new_session=True)
    except OSError as error:
        return {'returncode': 127, 'terminal': 'spawn-failed', 'error': str(error), 'survivors': []}
    recorded = {}

    def scan():
        recorded.update({pid: start for pid, start in watch.descendants_of(os.getpid()).items()
                         if pid not in (os.getpid(), os.getppid(), 1)})

    def alive():
        return [pid for pid, started in recorded.items()
                if (info := watch.proc_stat(pid)) and info['starttime'] == started
                and info['state'] != 'Z']

    def signal_owned(sig):
        for pid in alive():
            try:
                os.kill(pid, sig)
            except ProcessLookupError:
                pass

    started = time.monotonic()
    terminal = 'completed'
    try:
        while True:
            rc = proc.poll()
            if rc is not None:
                break
            if time.monotonic() - started >= timeout_s:
                terminal, rc = 'timeout', 124
                break
            time.sleep(min(0.25, max(0.001, timeout_s - (time.monotonic() - started))))
    finally:
        # The adapter watcher starts a separate session; process-group-only cleanup
        # would miss it. A subreaper tree plus birth identity retains that ownership.
        for sig in (signal.SIGTERM, signal.SIGKILL):
            deadline = time.monotonic() + 1
            while True:
                scan()
                signal_owned(sig)
                if not alive() or time.monotonic() >= deadline:
                    break
                time.sleep(0.05)
        try:
            proc.wait(timeout=1)
        except subprocess.TimeoutExpired:
            pass
        while True:
            try:
                if os.waitpid(-1, os.WNOHANG)[0] == 0:
                    break
            except ChildProcessError:
                break
    survivors = alive()
    return {'returncode': rc if not survivors else 126, 'terminal': terminal,
            'wall_s': time.monotonic() - started, 'child_subreaper': subreaper,
            'recorded_descendants': recorded, 'survivors': survivors}


def worker(receipt):
    plan = json.loads((receipt / 'plan.json').read_text())
    wt = Path(plan['worktree'])
    env = dict(os.environ, TMPDIR=str(receipt / 'tmp'),
               JAVA_TOOL_OPTIONS=f'-Xms64m -Xmx512m -Djava.io.tmpdir={receipt / "tmp"}',
               FAN_BASE=BASE, FAN_GIT='/usr/bin/git', PYTHONDONTWRITEBYTECODE='1')
    write(receipt / 'slot-acquired.json', stamp())
    try:
        validate_frozen()
        verify_oracle(receipt, plan)
        before = guards(wt)
        write(receipt / 'guards-before.json', before)
        write(receipt / 'adapter-start.json', stamp())
        with (receipt / 'adapter.log').open('x') as log:
            execution = bounded(plan['adapter_command'], env, log, plan['adapter_timeout_s'])
        write(receipt / 'adapter-end.json', dict(stamp(), **execution))
        # Do not execute an agent-modified runner or a changed external oracle.
        validate_frozen()
        verify_oracle(receipt, plan)
        if guards(wt) != before:
            raise ValueError('protected test/runner changed before acceptance')
        start = stamp()
        write(receipt / 'acceptance-start.json', start)
        with (receipt / 'acceptance.log').open('x') as log:
            acceptance = bounded(plan['acceptance_command'], env, log, 60, cwd=ROOT / 'repo')
        end = stamp()
        guards_match = guards(wt) == before
        verify_oracle(receipt, plan)
        result = dict(end, oracle_returncode=acceptance['returncode'],
                      adapter_returncode=execution['returncode'], lifecycle=acceptance,
                      acceptance_wall_s=end['monotonic_s'] - start['monotonic_s'],
                      guards_match=guards_match,
                      independently_accepted=acceptance['returncode'] == 0 and guards_match)
        write(receipt / 'acceptance-result.json', result)
        return 0 if execution['returncode'] == 0 and result['independently_accepted'] else 1
    except (ValueError, OSError) as error:
        write(receipt / 'worker-failure.json', dict(stamp(), terminal='worker-refused', error=str(error)))
        return 2


def interval_load(rows, lo, hi):
    values = [float(r['loadavg'].split()[0]) for r in rows if lo <= r['monotonic_s'] <= hi]
    return dict(samples=len(values), maximum=max(values) if values else None,
                contaminated=any(v > 10 for v in values) if values else None)


def monitor(argv, receipt, env):
    started = stamp()
    write(receipt / 'slot-requested.json', started)
    terminal, error = 'completed', None
    with (receipt / 'slot.log').open('x') as log, (receipt / 'load.jsonl').open('x') as loads:
        try:
            proc = subprocess.Popen(argv, stdout=log, stderr=subprocess.STDOUT, env=env)
            while True:
                sample = dict(stamp(), loadavg=Path('/proc/loadavg').read_text().strip())
                loads.write(json.dumps(sample) + '\n')
                loads.flush()
                try:
                    rc = proc.wait(timeout=1)
                    # Endpoint sample includes short acceptance phases.
                    loads.write(json.dumps(dict(stamp(), loadavg=Path('/proc/loadavg').read_text().strip())) + '\n')
                    break
                except subprocess.TimeoutExpired:
                    pass
        except OSError as exc:
            rc, terminal, error = 127, 'spawn-or-monitor-failed', str(exc)
    ended = stamp()
    rows = [json.loads(line) for line in (receipt / 'load.jsonl').read_text().splitlines()]
    def phase(name):
        path = receipt / (name + '.json')
        return json.loads(path.read_text())['monotonic_s'] if path.exists() else None
    for name in ['adapter-start', 'adapter-end', 'acceptance-start', 'acceptance-result']:
        p = receipt / (name + '.json')
        if p.exists():
            boundary = json.loads(p.read_text())
            if 'loadavg' in boundary:
                rows.append(boundary)
    adapter_lo, adapter_hi = phase('adapter-start'), phase('adapter-end')
    acceptance_lo, acceptance_hi = phase('acceptance-start'), phase('acceptance-result')
    contamination = {}
    for name, lo, hi in [('adapter', adapter_lo, adapter_hi),
                         ('acceptance', acceptance_lo, acceptance_hi),
                         ('verified_completion', adapter_lo, acceptance_hi)]:
        contamination[name] = interval_load(rows, lo, hi) if lo is not None and hi is not None else None
    acquired = phase('slot-acquired')
    write(receipt / 'orchestration-result.json', dict(
        ended, slot_returncode=rc, terminal=terminal, error=error,
        orchestration_wall_s=ended['monotonic_s'] - started['monotonic_s'],
        slot_wait_s=acquired - started['monotonic_s'] if acquired is not None else None,
        contamination=contamination, contamination_scope='one-second samples in explicitly named intervals; no samples means unknown',
        retained=True))
    return rc


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--worker', help=argparse.SUPPRESS)
    parser.add_argument('--receipt')
    parser.add_argument('--oracle-fixtures')
    parser.add_argument('--arm')
    parser.add_argument('--worktree')
    parser.add_argument('--prompt')
    parser.add_argument('--model', choices=['gpt-5.6-sol', 'gpt-6-astra'])
    parser.add_argument('--cpus', default='12,13')
    parser.add_argument('--canonical', default=str(ROOT / 'canonical'))
    parser.add_argument('--max-wall', type=int, default=900)
    for flag in ['mcp-url', 'server-sha', 'ready']:
        parser.add_argument('--' + flag)
    args = parser.parse_args()
    if args.worker:
        return worker(confined(args.worker))
    for key in ['receipt', 'oracle_fixtures', 'arm', 'worktree', 'prompt', 'model']:
        if getattr(args, key) is None:
            parser.error('missing --' + key.replace('_', '-'))
    receipt, arm, wt = map(confined, [args.receipt, args.arm, args.worktree])
    if receipt.exists() or arm.exists():
        raise ValueError('receipt or arm exists; refusing overwrite')
    if receipt.is_relative_to(wt) or wt.is_relative_to(receipt) or receipt.is_relative_to(arm) or arm.is_relative_to(receipt):
        raise ValueError('receipt, arm and fixture must be disjoint')
    frozen = validate_frozen()
    fixture = Path(args.oracle_fixtures).resolve()
    manifest = fixture / 'manifest-21.edn'
    if not manifest.is_file() or not (fixture / 'canonical-21').is_dir():
        raise ValueError('oracle fixture lacks manifest-21.edn/canonical-21')
    source_oracle_hashes = tree_hashes(fixture / 'canonical-21')
    if manifest.is_symlink():
        raise ValueError('manifest symlink forbidden')
    cmd = [sys.executable, str(ADAPTER), 'run']
    for key in ['arm', 'worktree', 'prompt', 'model', 'cpus', 'canonical', 'max_wall', 'mcp_url', 'server_sha', 'ready']:
        val = getattr(args, key)
        if val is not None:
            cmd += ['--' + key.replace('_', '-'), str(val)]
    receipt.mkdir(parents=True, exist_ok=False)
    (receipt / 'tmp').mkdir()
    oracle_copy = receipt / 'oracle-fixtures'
    oracle_copy.mkdir()
    shutil.copy2(manifest, oracle_copy / manifest.name)
    shutil.copytree(fixture / 'canonical-21', oracle_copy / 'canonical-21')
    if tree_hashes(oracle_copy / 'canonical-21') != source_oracle_hashes or sha(manifest) != sha(oracle_copy / manifest.name):
        raise ValueError('oracle changed during snapshot')
    oracle_hashes = tree_hashes(oracle_copy)
    for p in oracle_copy.rglob('*'):
        if p.is_file():
            p.chmod(p.stat().st_mode & 0o555)
    write(receipt / 'plan.json', dict(adapter_command=cmd, worktree=str(wt),
          adapter_timeout_s=args.max_wall + 120, oracle_hashes=oracle_hashes, oracle_source=str(fixture),
          acceptance_command=['bash', str(ORACLE), str(wt), '21', str(oracle_copy)],
          frozen=frozen, manifest_sha256=sha(manifest), wrapper_sha256=sha(Path(__file__)),
          slot_sha256=sha(Path(SLOT))))
    env = dict(os.environ, SLOT_OWNER='astra', PYTHONDONTWRITEBYTECODE='1')
    return monitor([SLOT, '-t', sys.executable, str(Path(__file__).resolve()), '--worker', str(receipt)], receipt, env)


if __name__ == '__main__':
    try:
        sys.exit(main())
    except (ValueError, OSError) as error:
        print('ORCHESTRATION REFUSED: ' + str(error), file=sys.stderr)
        sys.exit(2)
