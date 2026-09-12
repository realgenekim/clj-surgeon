"""Run the real oracle with writes allowed only beneath a fresh TMPDIR.

Linux Landlock makes the shared forge directory and /tmp read-only for this
process and its children; no shared permissions or production roots change.
"""
import ctypes
import os
from pathlib import Path
import subprocess
import tempfile


def restrict(root):
    libc = ctypes.CDLL(None, use_errno=True)

    def checked(result):
        if result < 0:
            error = ctypes.get_errno()
            raise OSError(error, os.strerror(error))
        return result

    # WRITE_FILE, REMOVE_DIR/FILE, MAKE_* (including socket). Reads and
    # execution stay unrestricted; this is a filesystem-write witness.
    rights = (1 << 1) | sum(1 << bit for bit in range(4, 14))
    ruleset = ctypes.c_uint64(rights)
    fd = checked(libc.syscall(444, ctypes.byref(ruleset), 8, 0))

    class Rule(ctypes.Structure):
        _pack_ = 1
        _fields_ = [('allowed_access', ctypes.c_uint64),
                    ('parent_fd', ctypes.c_int32)]

    parent = os.open(root, os.O_PATH | os.O_CLOEXEC)
    rule = Rule(rights, parent)
    checked(libc.syscall(445, fd, 1, ctypes.byref(rule), 0))
    checked(libc.prctl(38, 1, 0, 0, 0))  # PR_SET_NO_NEW_PRIVS
    checked(libc.syscall(446, fd, 0))
    os.close(parent)
    os.close(fd)


with tempfile.TemporaryDirectory(prefix='a15-', dir='/var/tmp/forge/bbtower-fx') as root:
    print('Fresh writable TMPDIR:', root, flush=True)
    print('All other paths read-only, including /var/tmp/forge and /tmp', flush=True)
    env = dict(os.environ, TMPDIR=root)
    env.pop('CLJ_SURGEON_GATE_ROOT', None)
    env.pop('GATE_SLOT_BACKEND', None)
    result = subprocess.run(
        ['python3', '-B', '-m', 'unittest', 'discover', '-s', 'test/oracles',
         '-p', 'test_gate_slot.py'], env=env, preexec_fn=lambda: restrict(root))
    print('exit:', result.returncode, flush=True)
    raise SystemExit(result.returncode)
