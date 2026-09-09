#!/usr/bin/env python3
"""TEST-ISO-015: box-wide worker admission, inherited across exec.

Only ownership of a slot denotes occupancy, and a slot is a name in the Linux
ABSTRACT Unix-domain socket namespace -- `\\0clj-surgeon-gate-slot-<i>`. Binding
it is acquiring it, EADDRINUSE is "taken", and the kernel frees it when the last
descriptor closes, including on SIGKILL. No environment width override and no
cached startup memory can enlarge the live admission budget.

WHY NOT A FILE. Sol GATE-LANES-FENCE-001-R2 and -R3: flock names an inode, not a
path, and every path-based repair leaks the same way. Remove the slot directory
under a live holder and the holder keeps locking the unlinked inode while the
next actor creates a fresh directory and a fresh lock beside it -- two live
semaphores, one bound. R2 closed that for a worker under the original
coordinator by refusing a replaced root; R3 walked straight through it with an
INDEPENDENT coordinator, which has nothing inherited to compare against and
recreates the root itself (`different_roots True, bound_violated True`).

There is no repair for that at the pathname layer, so this module does not use
one. The abstract namespace has no directory entry, no inode, no path: nothing
to unlink, nothing to recreate, and no root for two coordinators to disagree
about. The bad state is unrepresentable rather than detected -- an independent
coordinator on this box binds the same kernel names as every other one, with no
shared file, environment variable or inherited descriptor between them.
"""

import errno
import os
from pathlib import Path
import re
import socket
import sys
import time

# The box-wide namespace. It is a constant on purpose: two coordinators that
# share nothing at all must still contend for the same names.
SLOT_NAMESPACE = 'clj-surgeon-gate'

# Slots above the live width are still counted as occupied, so probe past it.
MAX_SLOTS = 64

# Admission is held only across sample + count + acquire. A holder that dies
# releases it in the kernel, so this deadline only bounds a live pathology.
ADMISSION_TIMEOUT_S = 60.0


def derived_width(cpus, memory_mib):
    allowance = (memory_mib - 2048) // 1536
    if allowance < 1:
        raise RuntimeError('gate-refused: insufficient memory for a bounded lane')
    return min(max(1, cpus // 2), allowance)


def capacity():
    # Linux box CPU set, matching nproc in the coordinator.
    cpus = len(os.sched_getaffinity(0))
    match = re.search(r'^MemAvailable:\s+(\d+)',
                      Path('/proc/meminfo').read_text(), re.MULTILINE)
    if match is None:
        raise RuntimeError('gate-refused: available memory is unknown')
    memory = int(match[1]) // 1024
    return {'cpus': cpus, 'memory-mib': memory,
            'width': derived_width(cpus, memory)}


def slot_name(namespace, suffix):
    """The kernel's name for a slot. The leading NUL is the abstract namespace:
    it is not a filesystem path and never touches a directory."""
    return b'\0' + ('%s-%s' % (namespace, suffix)).encode('utf-8')


def claim(name):
    """Bind the name, or None when another live holder owns it.

    Ownership is the bound socket itself. Closing it -- deliberately, on exit,
    or because the kernel reaped the process -- releases the name at once.
    """
    sock = socket.socket(socket.AF_UNIX, socket.SOCK_STREAM)
    try:
        sock.bind(name)
    except OSError as error:
        sock.close()
        if error.errno == errno.EADDRINUSE:
            return None
        raise
    return sock


def hold_admission(namespace, timeout_s=ADMISSION_TIMEOUT_S):
    """Serialize sample/count/reservation across every process on the box."""
    name = slot_name(namespace, 'admission')
    deadline = time.monotonic() + timeout_s
    while True:
        sock = claim(name)
        if sock is not None:
            return sock
        if time.monotonic() >= deadline:
            raise RuntimeError('gate-refused: admission is held too long')
        time.sleep(0.002)


def try_acquire(namespace=None, read_capacity=capacity):
    """Serialize sample/count/reservation; include holders above a shrunken width.

    Returns the bound socket that IS the slot, or None when the live width is
    already spent. The caller owns it: hold it for the work's lifetime.
    """
    namespace = namespace or SLOT_NAMESPACE
    admission = hold_admission(namespace)
    try:
        current = read_capacity()
        width = current['width']
        if width < 1:
            raise RuntimeError('gate-refused: insufficient memory for a bounded lane')
        free = []
        occupied = 0
        try:
            for index in range(max(width, MAX_SLOTS)):
                sock = claim(slot_name(namespace, 'slot-%d' % index))
                if sock is None:
                    occupied += 1
                else:
                    free.append((index, sock))
            eligible = next((entry for entry in free if entry[0] < width), None)
            if occupied >= width or eligible is None:
                return None
            free.remove(eligible)
            # The worker itself owns the open descriptor from here: it must
            # survive exec, and only its closure frees the slot.
            os.set_inheritable(eligible[1].fileno(), True)
            return eligible[1]
        finally:
            for _, sock in free:
                sock.close()
    finally:
        admission.close()


def main(argv):
    if len(argv) < 2 or argv[0] != '--':
        raise RuntimeError('usage: gate_slot.py -- COMMAND [ARG ...]')
    while True:
        slot = try_acquire()
        if slot is not None:
            break
        time.sleep(0.05)
    # The worker itself owns the bound socket. SIGKILL and exec failure release
    # it without a coordinator cleanup callback.
    try:
        os.execvp(argv[1], argv[1:])
    finally:
        slot.close()


if __name__ == '__main__':
    try:
        main(sys.argv[1:])
    except (OSError, RuntimeError) as error:
        print(str(error), file=sys.stderr)
        sys.exit(1)
