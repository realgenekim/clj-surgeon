#!/usr/bin/env python3
"""STATE-HOME-009/010: Sol second recurrence; writable checkout is intentional."""
import ctypes
import hashlib
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


def own_orphans():
    # Linux battery hosts: reap descendants orphaned when Make exits early.
    if sys.platform.startswith('linux'):
        libc = ctypes.CDLL(None, use_errno=True)
        if libc.prctl(36, 1, 0, 0, 0) != 0:  # PR_SET_CHILD_SUBREAPER
            raise OSError(ctypes.get_errno(), 'PR_SET_CHILD_SUBREAPER')


def wait_group(pgid, timeout=10):
    deadline = time.monotonic() + timeout
    while True:
        try:
            while os.waitpid(-pgid, os.WNOHANG)[0]:
                pass
        except ChildProcessError:
            pass
        try:
            os.killpg(pgid, 0)
        except ProcessLookupError:
            return
        if time.monotonic() >= deadline:
            raise AssertionError(f'process group {pgid} still alive after {timeout}s')
        time.sleep(.05)


def stop_image(process, pidfile):
    # The Make parent can exit before its warm JVM or git child. Own a session,
    # stop the exact warm PID, reap Make, then prove the whole group has gone.
    if pidfile.exists():
        try:
            os.kill(int(pidfile.read_text()), signal.SIGTERM)
        except ProcessLookupError:
            pass
    try:
        process.wait(timeout=10)
        wait_group(process.pid)
    except (subprocess.TimeoutExpired, AssertionError):
        try:
            os.killpg(process.pid, signal.SIGKILL)
        except ProcessLookupError:
            pass
        process.wait(timeout=10)
        wait_group(process.pid)


class CellDirectory:
    def __init__(self, facts):
        self.facts = facts
        self.directory = tempfile.TemporaryDirectory(prefix='state-matrix-')

    def __enter__(self):
        return self.directory.name

    def __exit__(self, *error):
        try:
            self.directory.cleanup()
        except OSError as failure:
            self.facts['cleanup_retry'] = str(failure)
            time.sleep(.25)
            self.directory.cleanup()  # A second failure propagates; never green.
        finally:
            print(json.dumps(self.facts), flush=True)


def witness(source, shape, envelope, location):
    root = Path.cwd()
    facts = dict(source=source, shape=shape, envelope=envelope, location=location, cleanup_retry=None)
    with CellDirectory(facts) as tmp:
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
                            'exec java -Xmx1024m -cp "$MATRIX_CP" clojure.main "$MATRIX_BOOT" "$1"\n')
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
                                       stdout=output, stderr=subprocess.STDOUT, start_new_session=True)
            try:
                deadline = time.monotonic() + 90
                while process.poll() is None and 'persistent server ready on' not in log.read_text():
                    if time.monotonic() > deadline:
                        raise AssertionError('timeout: ' + log.read_text())
                    time.sleep(.1)
            finally:
                stop_image(process, temp / 'pid')
        output = log.read_text()
        status = git('status', '--porcelain')
        facts.update(canonical=str(expected), kind=kind, status=status)
        assert status == '', facts
        if kind:
            assert not descriptor.exists(), facts
            assert ':' + kind in output, output
            assert process.returncode != 0, output
            assert ':canonical-path ' + json.dumps(str(expected)) in output, output
        else:
            assert not inside and descriptor.is_file(), (facts, output)
            assert 'persistent server ready on' in output, output


def teardown_regression():
    """Packet 68bacdec: Make exits while a descendant still writes .git/objects."""
    from unittest.mock import patch
    import contextlib
    import io

    with tempfile.TemporaryDirectory(prefix='state-teardown-') as tmp:
        root = Path(tmp)
        pidfile = root / 'pid'
        # The warm stand-in owns a child writer. Terminating its exact PID
        # leaves the writer alive; group waiting must cover its delayed write.
        writer = 'import time, sys; from pathlib import Path; time.sleep(.3); Path(sys.argv[1]).write_text("done")'
        code = ('import os, subprocess, sys, time; '
                'subprocess.Popen([sys.executable, "-c", sys.argv[3], sys.argv[2]]); '
                'open(sys.argv[1], "w").write(str(os.getpid())); time.sleep(30)')
        process = subprocess.Popen([sys.executable, '-c', code, str(pidfile), str(root / 'objects'), writer],
                                   start_new_session=True)
        try:
            deadline = time.monotonic() + 5
            while not pidfile.exists() or not pidfile.read_text():
                assert time.monotonic() < deadline, 'stand-in failed to start'
                time.sleep(.01)
            stop_image(process, pidfile)
            assert (root / 'objects').read_text() == 'done'
            try:
                os.killpg(process.pid, 0)
            except ProcessLookupError:
                pass
            else:
                raise AssertionError('teardown returned with live descendants')
        finally:
            if process.poll() is None:
                stop_image(process, pidfile)
    # The first cleanup failure is a recorded retry; a second remains red.
    for twice in (False, True):
        facts = {}
        directory = CellDirectory(facts)
        cleanup = directory.directory.cleanup
        calls = []
        def failing_cleanup():
            calls.append(1)
            if len(calls) == 1 or twice:
                raise OSError(39, 'Directory not empty')
            cleanup()
        try:
            with patch.object(directory.directory, 'cleanup', failing_cleanup), contextlib.redirect_stdout(io.StringIO()):
                try:
                    with directory:
                        pass
                except OSError:
                    assert twice
                else:
                    assert not twice
            assert len(calls) == 2 and 'Directory not empty' in facts['cleanup_retry']
        finally:
            cleanup()
    print('teardown-regression: descendant exit, recorded retry, repeated failure PASS', flush=True)


own_orphans()
teardown_regression()
failures = []
# Boundary coverage only; the 72-class enumeration is in-process in :fast.
# Preserve the packet 68bacdec failing cell as the inside-workspace witness.
cells = [('user.home', 'symlink', 'narrow', 'inside'),
         ('CLJ_SURGEON_STATE_HOME', 'relative', 'default', 'outside-envelope'),
         ('CLJ_SURGEON_STATE_HOME', 'direct', 'default', 'outside'),
         ('XDG_STATE_HOME', 'symlink', 'default', 'outside'),
         ('user.home', 'relative', 'default', 'outside')]
started = time.monotonic()
if len(sys.argv) > 2:
    cells = [tuple(sys.argv[2:])]
for cell in cells:
    try:
        witness(*cell)
    except Exception as error:
        failures.append((cell, str(error)))
        print('FAIL', cell, str(error), flush=True)
assert not failures, failures

elapsed = time.monotonic() - started
print(json.dumps(dict(warm_cells=len(cells), elapsed_ms=round(elapsed * 1000))), flush=True)
assert elapsed < 120, 'warm cells exceed 120s total'
