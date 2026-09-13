#!/usr/bin/env python3
"""STATE-HOME-009/010: Sol second recurrence; writable checkout is intentional."""
import hashlib
import itertools
import json
import os
from pathlib import Path
import shutil
import signal
import socket
import subprocess
import sys
import tempfile
import time


def witness(source, shape, envelope, location):
    root = Path.cwd()
    with tempfile.TemporaryDirectory(prefix='state-matrix-') as tmp:
        container = Path(tmp).resolve()
        temp = container / 'runtime'
        temp.mkdir()
        checkout = temp / 'checkout'
        checkout.mkdir()
        for name in filter(None, subprocess.check_output(['git', 'ls-files', '-z']).decode().split('\0')):
            origin = root / name
            if origin.is_file():
                dest = checkout / name
                dest.parent.mkdir(parents=True, exist_ok=True)
                shutil.copy2(origin, dest)
        git = lambda *args: subprocess.check_output(['git', '-C', str(checkout), *args], stderr=subprocess.STDOUT).decode()
        git('init', '-q')
        git('add', '.')
        git('-c', 'user.name=Matrix witness', '-c', 'user.email=matrix@example.invalid', 'commit', '-qm', 'fixture')
        assert git('status', '--porcelain') == ''
        cp = ':'.join(str(checkout / p) if not Path(p).is_absolute() else
                      str(checkout / Path(p).relative_to(root)) if Path(p).is_relative_to(root) else p
                      for p in sys.argv[1].split(':'))
        home = temp / 'home'
        home.mkdir()
        selected = (checkout if location == 'inside' else
                    container / 'disallowed' if location == 'outside-envelope' else temp / 'external')
        if shape == 'symlink':
            link = temp / 'link'
            link.symlink_to(selected, target_is_directory=True)
            value = str(link)
        elif shape == 'relative':
            value = os.path.relpath(selected, checkout)
        elif shape == 'empty':
            value = ''
        else:
            value = str(selected)
        env = dict(os.environ)
        for key in ('CLJ_SURGEON_STATE_HOME', 'XDG_STATE_HOME', 'CLJ_SURGEON_ARTIFACT_ROOT'):
            env.pop(key, None)
        if source == 'user.home':
            home_value = value
            expected = (checkout / value if not Path(value).is_absolute() else Path(value)) / '.local/state/clj-surgeon'
        else:
            home_value = str(home)
            env[source] = value
            expected = (checkout / value if not Path(value).is_absolute() else Path(value))
            if not value:
                expected = home / '.local/state/clj-surgeon'
            elif source == 'XDG_STATE_HOME':
                expected /= 'clj-surgeon'
        expected = expected.resolve()
        inside = expected.is_relative_to(checkout)
        outside = not expected.is_relative_to(temp)
        kind = ('state-root-inside-workspace' if inside else
                'state-root-outside-envelope' if outside or envelope == 'narrow' else None)
        bindir = temp / 'bin'
        bindir.mkdir()
        bootstrap = temp / 'bootstrap.clj'
        bootstrap.write_text(
            '(require \'[clj-surgeon.receipt-artifacts :as a])\n'
            '(binding [a/*destination-envelope* '
            + ('(a/destination-envelope [(System/getenv "MATRIX_ALLOWED")] :launcher)' if envelope == 'narrow' else '(a/current-envelope)')
            + '] (load-string (first *command-line-args*)))\n')
        launcher = bindir / 'clojure'
        launcher.write_text('#!/bin/bash\nset -eu\necho $$ > "$MATRIX_PID"\n'
                            'while [[ "$1" != "-e" ]]; do shift; done\nshift\n'
                            'exec java -Xmx512m -cp "$MATRIX_CP" clojure.main "$MATRIX_BOOT" "$1"\n')
        launcher.chmod(0o755)
        env.update(PATH=str(bindir) + ':' + env['PATH'], TMPDIR=str(temp), TMP=str(temp), TEMP=str(temp),
                   JAVA_TOOL_OPTIONS=f'-Djava.io.tmpdir={temp} -Duser.home={home_value}',
                   MATRIX_PID=str(temp / 'pid'), MATRIX_CP=cp, MATRIX_BOOT=str(bootstrap),
                   MATRIX_ALLOWED=str(temp / 'allowed'))
        with socket.socket() as sock:
            sock.bind(('127.0.0.1', 0))
            port = sock.getsockname()[1]
        descriptor = expected / 'workspaces' / hashlib.sha256(str(checkout).encode()).hexdigest() / 'probe.edn'
        log = temp / 'process.log'
        with log.open('w') as output:
            process = subprocess.Popen(['make', 'warm', f'PORT={port}'], cwd=checkout, env=env,
                                       stdout=output, stderr=subprocess.STDOUT)
            try:
                deadline = time.monotonic() + 90
                while process.poll() is None and 'persistent server ready on' not in log.read_text():
                    if time.monotonic() > deadline:
                        raise AssertionError('timeout: ' + log.read_text())
                    time.sleep(.1)
            finally:
                if (temp / 'pid').exists():
                    try:
                        os.kill(int((temp / 'pid').read_text()), signal.SIGTERM)
                    except ProcessLookupError:
                        pass
                try:
                    process.wait(timeout=10)
                except subprocess.TimeoutExpired:
                    os.kill(int((temp / 'pid').read_text()), signal.SIGKILL)
                    process.wait(timeout=10)
        output = log.read_text()
        status = git('status', '--porcelain')
        facts = dict(source=source, shape=shape, envelope=envelope, location=location,
                     canonical=str(expected), kind=kind, status=status)
        print(json.dumps(facts), flush=True)
        assert status == '', facts
        if kind:
            assert not descriptor.exists(), facts
            assert ':' + kind in output, output
            assert process.returncode != 0, output
            assert ':canonical-path ' + json.dumps(str(expected)) in output, output
        else:
            assert not inside and descriptor.is_file(), (facts, output)
            assert 'persistent server ready on' in output, output


failures = []
# Checkout, admitted external and outside-envelope destinations: all 24 required
# intersections at each location. The default envelope must also reject widening.
cells = itertools.product(('CLJ_SURGEON_STATE_HOME', 'XDG_STATE_HOME', 'user.home'),
                              ('direct', 'symlink', 'relative', 'empty'),
                              ('default', 'narrow'), ('inside', 'outside', 'outside-envelope'))
if len(sys.argv) > 2:
    cells = [tuple(sys.argv[2:])]
for cell in cells:
    try:
        witness(*cell)
    except Exception as error:
        failures.append((cell, str(error)))
        print('FAIL', cell, str(error), flush=True)
assert not failures, failures
