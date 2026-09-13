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
import hashlib

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
    fixture = checkout / 'test/clj_surgeon/state_home_fixture_test.clj'
    fixture.write_text('(ns clj-surgeon.state-home-fixture-test (:require [clojure.test :refer [deftest is]]))\n(deftest boundary (is (= 2 (+ 1 1))))\n')
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
    mode = sys.argv[2] if len(sys.argv) > 2 else 'explicit'
    expected_root = temp / 'state'
    if mode not in ('explicit', 'unwritable'):
        env.pop('CLJ_SURGEON_STATE_HOME', None)
        env.pop('XDG_STATE_HOME', None)
        if mode == 'xdg':
            env['XDG_STATE_HOME'] = str(temp / 'xdg')
            expected_root = temp / 'xdg' / 'clj-surgeon'
        else:
            expected_root = home / '.local' / 'state' / 'clj-surgeon'
    if mode == 'unwritable':
        expected_root.mkdir()
        expected_root.chmod(0o500)
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
            if mode == 'unwritable':
                process.wait(timeout=120)
                failure = log.read_text()
                assert process.returncode != 0, failure
                assert ':probe-state-not-writable' in failure, failure
                assert ':errno "EACCES"' in failure, failure
                assert ':path "' + str(expected_root) in failure, failure
                assert git('status', '--porcelain') == ''
                print('STATE-HOME-007: make warm refused EACCES with descriptor path; checkout clean')
                sys.exit(0)
            deadline = time.monotonic() + 120
            while 'persistent server ready on' not in log.read_text():
                if process.poll() is not None or time.monotonic() > deadline:
                    raise AssertionError('Warm startup failed: ' + log.read_text())
                time.sleep(0.2)
            descriptor = expected_root / 'workspaces' / hashlib.sha256(str(checkout.resolve()).encode()).hexdigest() / 'probe.edn'
            assert descriptor.is_file(), 'Warm descriptor absent from independently computed state path'
            probe = subprocess.run(
                ['bb', f'-Djava.io.tmpdir={temp}', f'-Duser.home={home}',
                 '--classpath', f'{checkout}/src:{checkout}/libs/clj-splice/src',
                 '-e', "(require '[clj-surgeon.probe :as p]) (let [r (p/cli! {:ns \"clj-surgeon.state-home-fixture-test\"})] (prn r) (assert (= :probe-passed (:state r))) (assert (= 1 (:tests r))) (assert (= (first *command-line-args*) (:image-file r))))",
                 str(descriptor)], cwd=checkout, env=env, capture_output=True, text=True, timeout=90)
            assert probe.returncode == 0, probe.stdout + probe.stderr
            print('STATE-HOME-002/005 live probe:', probe.stdout.strip())
        finally:
            if mode == 'unwritable':
                expected_root.chmod(0o700)
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
