#!/usr/bin/env python3
"""Isolated Sol/Astra arm adapter. No server lifecycle or correctness claims.

run --arm ROOT/arms/NAME --worktree ROOT/fixtures/NAME --prompt ROOT/PROMPT
    --model gpt-6-astra --cpus 12,13 --canonical ROOT/canonical
Tool arms additionally require --mcp-url, --server-sha and --ready JSON.
Ready JSON: mcp_url, healthz_url, server_sha, project_root, port_pid, server_cwd,
healthz_sha256 (SHA256 of the exact live response bytes). Parent owns and
independently verifies the ready evidence before supplying it.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import sys
import time
from datetime import datetime, timezone
from urllib.parse import urlsplit
from urllib.request import urlopen

ROOT = Path('/var/tmp/forge/astra-fair2-data-fx')
CODEX = Path('/home/forge/.local/bin/codex')
CODEX_HOME = Path('/home/forge/.codex')
CODEX_VENDOR = Path('/home/forge/.local/lib/node_modules/@openai/codex/node_modules/@openai/codex-linux-x64/vendor/x86_64-unknown-linux-musl/bin/codex')
VERSION = 'codex-cli 0.153.3'
MODELS = ('gpt-5.6-sol', 'gpt-6-astra')
HERE = Path(__file__).resolve()
METERS = HERE.parent.parent / 'anvil-arms'


def digest(data):
    return hashlib.sha256(data).hexdigest()


def file_digest(path):
    value = hashlib.sha256()
    with path.open('rb') as source:
        for chunk in iter(lambda: source.read(1024 * 1024), b''):
            value.update(chunk)
    return value.hexdigest()


def confined(path):
    path = Path(path).resolve()
    if path == ROOT or not path.is_relative_to(ROOT):
        raise ValueError(f'path outside experiment root: {path}')
    return path


def model_argv(model, worktree, cpus, mcp_url=None):
    if model not in MODELS:
        raise ValueError('unsupported model')
    if not re.fullmatch(r'\d+(?:-\d+)?(?:,\d+(?:-\d+)?)*', cpus):
        raise ValueError('invalid CPU list')
    argv = ['taskset', '-c', cpus, str(CODEX), 'exec',
            '--ignore-user-config', '--ignore-rules',
            '--dangerously-bypass-approvals-and-sandbox',
            '-m', model, '-c', 'model_reasoning_effort="high"',
            '-C', str(worktree), '--color', 'never']
    if mcp_url:
        validate_url(mcp_url)
        argv += ['-c', 'mcp_servers.surgeon.url=' + json.dumps(mcp_url),
                 '-c', 'mcp_servers.surgeon.required=true']
    return argv + ['-']


def validate_url(url):
    parsed = urlsplit(url)
    if (parsed.scheme != 'http' or parsed.hostname != '127.0.0.1'
            or not parsed.port or not 8300 <= parsed.port <= 8339
            or parsed.username or parsed.password or parsed.query or parsed.fragment):
        raise ValueError('require isolated loopback HTTP URL on ports 8300–8339')


def validate_ready(ready, url, sha, worktree, health_bytes):
    validate_url(url)
    validate_url(ready['healthz_url'])
    if urlsplit(url).netloc != urlsplit(ready['healthz_url']).netloc:
        raise ValueError('healthz is not bound to MCP port')
    expected = {'mcp_url': url, 'server_sha': sha, 'project_root': str(worktree)}
    if (any(ready.get(k) != v for k, v in expected.items())
            or not re.fullmatch(r'[0-9a-f]{40}', sha)
            or not isinstance(ready.get('port_pid'), int) or ready['port_pid'] <= 0
            or ready.get('healthz_sha256') != digest(health_bytes)):
        raise ValueError('server ready evidence mismatch')
    health = json.loads(health_bytes)
    if (health.get('ok') is not True or health.get('server') != 'clj-surgeon'
            or health.get('tool_runtime') != 'ready' or health.get('tool_registry') != 'ready'):
        raise ValueError('server is not functionally ready')


def pid_listens(pid, port):
    sockets = {os.readlink(fd) for fd in Path(f'/proc/{pid}/fd').iterdir()}
    for table in ('tcp', 'tcp6'):
        for line in Path('/proc/net/' + table).read_text().splitlines()[1:]:
            fields = line.split()
            if (int(fields[1].split(':')[1], 16) == port and fields[3] == '0A'
                    and 'socket:[' + fields[9] + ']' in sockets):
                return True
    return False


def resolved_model(rows, run, driver_log, requested):
    sessions = re.findall(r'session[\s_-]*id\s*[:=]?\s*([0-9a-f-]{36})',
                          driver_log, re.I)
    sessions = set(sessions)
    metas = [r['payload'].get('id') for r in rows if r.get('type') == 'session_meta']
    models = {r['payload']['model'] for r in rows
              if r.get('type') in ('turn_context', 'session_meta')
              and r.get('payload', {}).get('model')}
    if len(sessions) != 1 or metas != list(sessions):
        raise ValueError('rollout does not match announced unique session')
    session = next(iter(sessions))
    if run.get('rollout_binding') != 'session-id:' + session:
        raise ValueError('watcher session binding mismatch')
    if models != {requested}:
        raise ValueError(f'resolved model mismatch: requested={requested} actual={models}')
    return {'session_id': session, 'resolved_model': requested}


def write_json(path, value):
    path.write_text(json.dumps(value, indent=2, sort_keys=True) + '\n')


def command(argv, cwd=None, **kwargs):
    return subprocess.run(argv, cwd=cwd, check=True, capture_output=True, **kwargs)


def snapshot(root, names):
    result = {}
    for name in names:
        start = root / name
        paths = [start] if start.is_file() else sorted(start.rglob('*'))
        for path in paths:
            if path.is_symlink():
                raise ValueError(f'symlink in protected tree: {path}')
            if path.is_file():
                result[str(path.relative_to(root))] = {
                    'sha256': digest(path.read_bytes()), 'mode': path.stat().st_mode & 0o777}
    return result


def prepare(args):
    arm, wt, prompt, canonical = map(confined, (args.arm, args.worktree, args.prompt, args.canonical))
    if arm.exists():
        raise ValueError('arm already exists; refusing overwrite')
    if arm.is_relative_to(wt) or wt.is_relative_to(arm) or canonical.is_relative_to(wt):
        raise ValueError('artifacts/canonical must be outside worktree')
    argv = model_argv(args.model, wt, args.cpus, args.mcp_url)
    if os.environ.get('CODEX_HOME', str(CODEX_HOME)) != str(CODEX_HOME):
        raise ValueError('inherited CODEX_HOME is not the shared home')
    for parent in (wt, *wt.parents):
        if (parent / '.codex').exists():
            raise ValueError(f'project/ancestor .codex config forbidden: {parent}')
    if any(wt.rglob('.codex')):
        raise ValueError('nested .codex config forbidden')
    if command([str(CODEX), '--version']).stdout.decode().strip() != VERSION:
        raise ValueError('pinned Codex version mismatch')
    if command([str(CODEX_VENDOR), '--version']).stdout.decode().strip() != VERSION:
        raise ValueError('pinned Codex vendor version mismatch')
    git_root = Path(command(['git', 'rev-parse', '--show-toplevel'], wt).stdout.decode().strip()).resolve()
    if git_root != wt:
        raise ValueError('fixture must be its own git worktree root')
    head = command(['git', 'rev-parse', 'HEAD'], wt).stdout.decode().strip()
    if command(['git', 'status', '--porcelain'], wt).stdout:
        raise ValueError('fixture must start clean')
    for required in ('src', 'test', 'bin/fan-test'):
        if not (wt / required).exists():
            raise ValueError('fanout fixture lacks ' + required)
    if not (canonical / 'src').is_dir():
        raise ValueError('canonical source missing')
    ready = None
    if args.mcp_url:
        if not args.ready or not args.server_sha:
            raise ValueError('tool arm requires --ready and --server-sha')
        ready = json.loads(confined(args.ready).read_text())
        validate_url(ready['healthz_url'])
        with urlopen(ready['healthz_url'], timeout=5) as response:
            health = response.read(1024 * 1024)
        validate_ready(ready, args.mcp_url, args.server_sha, wt, health)
        server_cwd = confined(ready['server_cwd'])
        if Path(f'/proc/{ready["port_pid"]}/cwd').resolve() != server_cwd:
            raise ValueError('server PID cwd mismatch')
        if not pid_listens(ready['port_pid'], urlsplit(args.mcp_url).port):
            raise ValueError('server PID does not own MCP listener')
        actual_sha = command(['git', 'rev-parse', 'HEAD'], server_cwd).stdout.decode().strip()
        if actual_sha != args.server_sha:
            raise ValueError('server checkout SHA mismatch')
    elif args.ready or args.server_sha:
        raise ValueError('native arm cannot carry server evidence')
    arm.mkdir(parents=True, exist_ok=False)
    (arm / 'tmp').mkdir()
    (arm / 'prompt.txt').write_bytes(prompt.read_bytes())
    guard = snapshot(wt, ['test', 'bin/fan-test'])
    for name in guard:
        target = arm / 'guard' / name
        target.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(wt / name, target)
        target.chmod(0o444)
    write_json(arm / 'guard.json', guard)
    write_json(arm / 'canonical-src.json', snapshot(canonical, ['src']))
    write_json(arm / 'command.json', argv)
    make = subprocess.run([sys.executable, str(METERS / '_make_targets.py'), str(wt),
                           str(arm / 'make-targets.json')], capture_output=True)
    if make.returncode not in (0, 3):
        raise ValueError('make target map refused: ' + make.stderr.decode())
    attest = {'attest_ok': True, 'start_utc': datetime.now(timezone.utc).isoformat(),
              'arm': 'T' if ready else 'N', 'exp': 'astra-fanout', 'driver': 'codex',
              'model': args.model, 'effort': 'high', 'worktree': str(wt),
              'worktree_head': head, 'base': head, 'prompt_path': str(arm / 'prompt.txt'),
              'prompt_sha256': digest(prompt.read_bytes()), 'driver_command': argv,
              'codex_version': VERSION, 'codex_sha256': digest(CODEX.read_bytes()),
              'codex_vendor_path': str(CODEX_VENDOR),
              'codex_vendor_sha256': file_digest(CODEX_VENDOR),
              'runner_sha256': digest(HERE.read_bytes()),
              'watch_sha256': digest((METERS / 'watch.py').read_bytes()),
              'score_sha256': digest((METERS / 'score.py').read_bytes()),
              'make_targets_sha256': (digest((arm / 'make-targets.json').read_bytes())
                                      if (arm / 'make-targets.json').exists() else None),
              'mcp_url': args.mcp_url, 'server_sha': args.server_sha,
              'port_pid': ready['port_pid'] if ready else None,
              'server_cwd': ready['server_cwd'] if ready else None,
              'server_ready': ready, 'correctness': 'pending-independent-acceptance'}
    write_json(arm / 'attest.json', attest)
    return arm, wt, head


def run(args):
    adapter_start = time.monotonic()
    load_start = Path('/proc/loadavg').read_text().strip()
    arm, wt, base = prepare(args)
    env = dict(os.environ, TMPDIR=str(arm / 'tmp'),
               JAVA_TOOL_OPTIONS=f'-Xms64m -Xmx512m -Djava.io.tmpdir={arm / "tmp"}')
    watch_start = time.monotonic()
    watch_load_start = Path('/proc/loadavg').read_text().strip()
    watch = subprocess.run([sys.executable, str(METERS / 'watch.py'), '--arm', str(arm),
                            '--codex-home', str(CODEX_HOME), '--max-wall', str(args.max_wall),
                            '--idle-timeout', '300', '--', sys.executable, str(HERE),
                            'driver', '--arm', str(arm)], cwd=wt, env=env)
    watch_end = time.monotonic()
    watch_load_end = Path('/proc/loadavg').read_text().strip()
    command(['git', 'add', '-A'], wt)
    (arm / 'diff.patch').write_bytes(command(['git', 'diff', '--cached', '--binary', base], wt).stdout)
    result = {'watch_rc': watch.returncode, 'valid_measurement': False,
              'timing': {'adapter_start_monotonic_s': adapter_start,
                         'watch_start_monotonic_s': watch_start,
                         'watch_end_monotonic_s': watch_end,
                         'preparation_wall_s': watch_start - adapter_start,
                         'watch_subprocess_wall_s': watch_end - watch_start,
                         'adapter_load_start': load_start,
                         'watch_load_start': watch_load_start,
                         'watch_load_end': watch_load_end,
                         'lock_wait_included': False},
              'correctness': 'pending-independent-acceptance'}
    try:
        rows = [json.loads(line) for line in (arm / 'rollout.jsonl').read_text().splitlines()]
        run_data = json.loads((arm / 'run.json').read_text())
        result.update(resolved_model(rows, run_data, (arm / 'driver-output.log').read_text(), args.model))
        if watch.returncode or run_data.get('driver_rc') != 0:
            raise ValueError('watcher or driver failed')
        result['protected_bytes_match'] = snapshot(wt, ['test', 'bin/fan-test']) == json.loads((arm / 'guard.json').read_text())
        result['canonical_src_match'] = snapshot(wt, ['src']) == json.loads((arm / 'canonical-src.json').read_text())
        result['valid_measurement'] = True
    except (ValueError, OSError, KeyError) as error:
        result['refusal'] = str(error)
    result['timing'].update(adapter_wall_s=time.monotonic() - adapter_start,
                            adapter_load_end=Path('/proc/loadavg').read_text().strip(),
                            adapter_wall_scope='prepare-through-freeze-and-attestation; excludes scorer')
    write_json(arm / 'adapter-result.json', result)
    if not result['valid_measurement']:
        return 2
    return subprocess.run([sys.executable, str(METERS / 'score.py'), str(arm)], env=env).returncode


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('action', choices=['run', 'driver'])
    parser.add_argument('--arm', required=True)
    for key in ('worktree', 'prompt', 'model', 'cpus', 'canonical', 'mcp-url', 'server-sha', 'ready'):
        parser.add_argument('--' + key)
    parser.add_argument('--max-wall', type=int, default=900)
    args = parser.parse_args()
    if args.action == 'driver':
        arm = confined(args.arm)
        argv = json.loads((arm / 'command.json').read_text())
        with (arm / 'prompt.txt').open('rb') as prompt:
            os.dup2(prompt.fileno(), 0)
        os.execvp(argv[0], argv)
    if any(getattr(args, key) is None for key in ('worktree', 'prompt', 'model', 'cpus', 'canonical')):
        parser.error('run requires worktree, prompt, model, cpus, canonical')
    if not 1 <= args.max_wall <= 1800:
        parser.error('max-wall must be 1..1800 seconds')
    try:
        return run(args)
    except (ValueError, OSError, subprocess.CalledProcessError) as error:
        print('ADAPTER REFUSED: ' + str(error), file=sys.stderr)
        return 2


if __name__ == '__main__':
    sys.exit(main())
