#!/usr/bin/env python3
"""STATE-HOME-001: inb-3c65d7, real Make recipe in a fresh committed copy."""
import os
import pathlib
import shutil
import signal
import subprocess
import sys
import tempfile
import time

root = pathlib.Path.cwd()
base = pathlib.Path('/var/tmp/forge/statehome-fx')
base.mkdir(parents=True, exist_ok=True)
with tempfile.TemporaryDirectory(prefix='warm-', dir=base) as tmp:
    temp = pathlib.Path(tmp)
    checkout = temp / 'checkout'
    checkout.mkdir()
    files = subprocess.check_output(['git', 'ls-files', '-z']).decode().split('\0')
    for name in filter(None, files):
        source = root / name
        if source.is_file():
            target = checkout / name
            target.parent.mkdir(parents=True, exist_ok=True)
            shutil.copy2(source, target)
    def git(*args):
        return subprocess.check_output(['git', '-C', str(checkout), *args], stderr=subprocess.STDOUT).decode()
    git('init', '-q')
    git('add', '.')
    git('-c', 'user.name=State home witness', '-c', 'user.email=witness@example.invalid', 'commit', '-qm', 'fixture')
    assert git('status', '--porcelain') == ''
    cp = sys.argv[1] if len(sys.argv) > 1 else subprocess.check_output(
        ['clojure', '-J-Xmx1024m', '-Spath', '-M:clj-surgeon/test-deps'], text=True).strip()
    cp = ':'.join(str(checkout / p) if not pathlib.Path(p).is_absolute() else
                  str(checkout / pathlib.Path(p).relative_to(root)) if pathlib.Path(p).is_relative_to(root) else p
                  for p in cp.split(':'))
    bindir = temp / 'bin'
    bindir.mkdir()
    # Reuse resolved dependencies; run the recipe's exact expression in a bounded
    # child. This prevents dependency cache writes outside this fixture.
    launcher = bindir / 'clojure'
    launcher.write_text('#!/bin/bash\nset -eu\necho $$ > "$STATEHOME_PID"\n'
                        'while [[ "$1" != "-e" ]]; do shift; done\n'
                        'exec java -Xmx1024m -cp "$STATEHOME_CP" clojure.main "$@"\n')
    launcher.chmod(0o755)
    home = temp / 'home'
    home.mkdir()
    env = dict(os.environ, PATH=str(bindir) + ':' + os.environ['PATH'],
               TMPDIR=str(temp), TMP=str(temp), TEMP=str(temp),
               JAVA_TOOL_OPTIONS=f'-Xmx1024m -Djava.io.tmpdir={temp} -Duser.home={home}',
               CLJ_SURGEON_STATE_HOME=str(temp / 'state'),
               STATEHOME_PID=str(temp / 'pid'), STATEHOME_CP=cp)
    # Choose a free allowed port without contacting any existing service.
    import socket
    port = None
    for candidate in range(19000, 20000):
        with socket.socket() as sock:
            try:
                sock.bind(('127.0.0.1', candidate))
                port = candidate
                break
            except OSError:
                pass
    assert port is not None
    log = temp / 'process.log'
    with log.open('w') as output:
        process = subprocess.Popen(['make', 'warm', f'PORT={port}'], cwd=checkout, env=env,
                                   stdout=output, stderr=subprocess.STDOUT)
        try:
            deadline = time.monotonic() + 120
            while 'persistent server ready on' not in log.read_text():
                if process.poll() is not None or time.monotonic() > deadline:
                    raise AssertionError('Warm startup failed: ' + log.read_text())
                time.sleep(0.2)
        finally:
            if (temp / 'pid').exists():
                pid = int((temp / 'pid').read_text())
                try:
                    os.kill(pid, signal.SIGTERM)
                except ProcessLookupError:
                    pass
            try:
                process.wait(timeout=15)
            except subprocess.TimeoutExpired:
                os.kill(pid, signal.SIGKILL)
                process.wait(timeout=10)
    status = git('status', '--porcelain')
    print('STATE-HOME-001 git status:', repr(status))
    assert status == '', 'Warm image dirtied fresh checkout: ' + status
