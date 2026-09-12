"""Run arbitrary argv under the check-only filesystem-write envelope.

The caller retains stdout/stderr and gate receipts. This is diagnostic tooling,
not packet or landing authority. No shared permissions are changed.
"""
import sys
import uuid
import json
import time
import ctypes
import os
from pathlib import Path
import subprocess
import tempfile


def restrict(roots):
    libc = ctypes.CDLL(None, use_errno=True)

    def checked(result):
        if result < 0:
            error = ctypes.get_errno()
            raise OSError(error, os.strerror(error))
        return result

    # WRITE_FILE, REMOVE_DIR/FILE, MAKE_* (including socket), REFER and
    # TRUNCATE. Fail closed on kernels too old to mediate truncation.
    abi = checked(libc.syscall(444, 0, 0, 1))
    if abi < 3:
        raise RuntimeError('Landlock ABI >= 3 required for TRUNCATE')
    rights = (1 << 1) | sum(1 << bit for bit in range(4, 15))
    ruleset = ctypes.c_uint64(rights)
    fd = checked(libc.syscall(444, ctypes.byref(ruleset), 8, 0))

    class Rule(ctypes.Structure):
        _pack_ = 1
        _fields_ = [('allowed_access', ctypes.c_uint64),
                    ('parent_fd', ctypes.c_int32)]

    for root in roots:
        parent = os.open(root, os.O_PATH | os.O_CLOEXEC)
        rule = Rule(rights, parent)
        checked(libc.syscall(445, fd, 1, ctypes.byref(rule), 0))
        os.close(parent)
    # Parity with /home/forge/bin/lib/packet.py:78-80: /dev/null is
    # the sole writable file outside the owned roots (WRITE_FILE only).
    parent = os.open('/dev/null', os.O_PATH | os.O_CLOEXEC)
    rule = Rule(1 << 1, parent)
    checked(libc.syscall(445, fd, 1, ctypes.byref(rule), 0))
    os.close(parent)
    checked(libc.prctl(38, 1, 0, 0, 0))  # PR_SET_NO_NEW_PRIVS
    checked(libc.syscall(446, fd, 0))
    os.close(fd)



def main():
    command = sys.argv[1:]
    if command[:1] == ['--']:
        command = command[1:]
    if not command:
        raise SystemExit('usage: restricted-gate.py -- COMMAND [ARG ...]')
    root = Path('/var/tmp/forge/bbtower-fx/packets') / str(uuid.uuid4()) / 'tmp'
    state = Path.home() / '.local/state/clj-surgeon'
    root.mkdir(parents=True)
    state.mkdir(parents=True, exist_ok=True)
    roots = [str(Path.cwd().resolve()), str(root), str(state.resolve())]
    env = dict(os.environ, TMPDIR=str(root),
               JAVA_TOOL_OPTIONS=f'-Xmx1g -Djava.io.tmpdir={root}')
    for name in ['_JAVA_OPTIONS', 'CLJ_SURGEON_GATE_ROOT', 'GATE_SLOT_BACKEND']:
        env.pop(name, None)
    print(json.dumps(dict(command=command, writable_roots=roots,
                          writable_devices=['/dev/null'],
                          started=time.time(), diagnostic=True)), flush=True)
    start = time.monotonic()
    result = subprocess.run(command, env=env, preexec_fn=lambda: restrict(roots))
    print(json.dumps(dict(exit=result.returncode,
                          wall_seconds=time.monotonic()-start)), flush=True)
    return result.returncode

if __name__ == '__main__':
    raise SystemExit(main())
