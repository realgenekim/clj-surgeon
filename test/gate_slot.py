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

DARWIN. macOS has no abstract namespace, so the same discipline is rebuilt on a
PATHNAME socket under a per-user runtime root and the guarantee is WEAKER by a
named amount. Occupancy is still ownership of a bound, listening socket; a dead
holder is still detected by the kernel, because connect() to a socket whose last
descriptor is gone fails ECONNREFUSED and that stale entry is reclaimed. What
darwin cannot make unrepresentable is R3: remove or replace the slot ROOT under
a live holder and two coordinators can bind two different directories, so two
live semaphores exist with one bound. Publication is atomic (bind to a private
name, then os.link into place -- link fails EEXIST rather than clobbering), the
root's identity is checked by inode on every acquire, and a REPLACED root is a
typed refusal rather than a silent split. A root DELETED between the check and
the link is the residual hole; it is documented, not repaired, and it is why the
receipt carries the guarantee level. Callers read it from `admission_mode()`:
:abstract-socket (linux, unrepresentable) or :path-socket (darwin, detected).

The backend follows the platform. `GATE_SLOT_BACKEND=abstract|path-socket`
overrides it so the darwin path is witnessed on linux by the same oracle.
"""

import errno
import os
from pathlib import Path
import re
import socket
import subprocess
import sys
import time
import uuid

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


def cpu_count():
    """The CPU set this process may actually run on.

    linux: the affinity mask, matching `nproc` in the coordinator.
    darwin: there is no affinity mask; `sysctl -n hw.ncpu` is what nproc's
    portable equivalent reports, and os.cpu_count() reads the same figure.
    """
    getaffinity = getattr(os, 'sched_getaffinity', None)
    if getaffinity is not None:
        return len(getaffinity(0))
    count = os.cpu_count()
    if not count:
        raise RuntimeError('gate-refused: CPU count is unknown')
    return count


def _darwin_memory_available_mib():
    """free + inactive + speculative + purgeable pages, in MiB.

    `MemAvailable` has no darwin equivalent. vm_stat's reclaimable classes are
    the closest honest analogue: pages the VM can hand to a new JVM without
    swapping. hw.memsize caps it so a misparse cannot invent capacity.
    """
    try:
        stat = subprocess.run(['vm_stat'], capture_output=True, text=True,
                              timeout=10, check=True).stdout
        total_bytes = int(subprocess.run(['sysctl', '-n', 'hw.memsize'],
                                         capture_output=True, text=True,
                                         timeout=10, check=True).stdout.strip())
    except (OSError, subprocess.SubprocessError, ValueError) as error:
        raise RuntimeError(
            'gate-refused: available memory is unknown (darwin vm_stat/sysctl '
            'unavailable); set GATE_MEMAVAIL_MIB to declare it') from error
    page = re.search(r'page size of (\d+) bytes', stat)
    page_bytes = int(page[1]) if page else 4096
    reclaimable = 0
    seen = 0
    for label in ('Pages free', 'Pages inactive', 'Pages speculative',
                  'Pages purgeable'):
        match = re.search(r'^%s:\s+(\d+)' % re.escape(label), stat, re.MULTILINE)
        if match is not None:
            reclaimable += int(match[1])
            seen += 1
    if seen == 0:
        raise RuntimeError('gate-refused: available memory is unknown '
                           '(darwin vm_stat reported no reclaimable classes); '
                           'set GATE_MEMAVAIL_MIB to declare it')
    return min(reclaimable * page_bytes, total_bytes) // (1024 * 1024)


def memory_available_mib():
    """Available MiB, or a typed refusal naming the documented override.

    GATE_MEMAVAIL_MIB is honoured FIRST on every platform: a box whose memory
    the gate cannot read is allowed to declare it rather than be locked out.
    """
    declared = os.environ.get('GATE_MEMAVAIL_MIB')
    if declared:
        try:
            value = int(declared)
        except ValueError:
            raise RuntimeError('gate-refused: GATE_MEMAVAIL_MIB is not an '
                               'integer MiB count: %r' % declared)
        if value < 0:
            raise RuntimeError('gate-refused: GATE_MEMAVAIL_MIB is negative')
        return value
    meminfo = Path('/proc/meminfo')
    if meminfo.exists():
        match = re.search(r'^MemAvailable:\s+(\d+)',
                          meminfo.read_text(), re.MULTILINE)
        if match is None:
            raise RuntimeError('gate-refused: available memory is unknown')
        return int(match[1]) // 1024
    if sys.platform == 'darwin':
        return _darwin_memory_available_mib()
    raise RuntimeError('gate-refused: available memory is unknown on platform '
                       '%r; set GATE_MEMAVAIL_MIB to declare it' % sys.platform)


def capacity():
    cpus = cpu_count()
    memory = memory_available_mib()
    return {'cpus': cpus, 'memory-mib': memory,
            'width': derived_width(cpus, memory)}


# ---------------------------------------------------------------------------
# Backend selection. The platform decides; the environment may override it so
# the darwin path is witnessed by this repository's oracle on a linux box.
# ---------------------------------------------------------------------------

ABSTRACT = 'abstract'
PATH_SOCKET = 'path-socket'


def backend():
    declared = os.environ.get('GATE_SLOT_BACKEND')
    if declared:
        if declared not in (ABSTRACT, PATH_SOCKET):
            raise RuntimeError('gate-refused: unknown GATE_SLOT_BACKEND %r '
                               '(abstract | path-socket)' % declared)
        if declared == ABSTRACT and not _abstract_supported():
            raise RuntimeError('gate-refused: this platform has no abstract '
                               'Unix-domain namespace')
        return declared
    return ABSTRACT if _abstract_supported() else PATH_SOCKET


def _abstract_supported():
    return sys.platform.startswith('linux')


def admission_mode():
    """The guarantee level, for the landing receipt.

    :abstract-socket -- a split semaphore is UNREPRESENTABLE: no path, no
        inode, no root for two coordinators to disagree about.
    :path-socket -- a split semaphore is DETECTED, not impossible: publication
        is atomic and a replaced root is a typed refusal, but a root deleted
        between the identity check and the link can still split it.
    """
    return ':abstract-socket' if backend() == ABSTRACT else ':path-socket'


def slot_root():
    """The per-user runtime directory holding pathname slots.

    Never a shared world-writable root: another user must not be able to
    pre-create a slot name and lock the gate out.
    """
    declared = os.environ.get('CLJ_SURGEON_GATE_ROOT')
    if declared:
        return Path(declared)
    tmp = os.environ.get('TMPDIR')
    if tmp:
        return Path(tmp) / 'clj-surgeon-gate'
    return Path('/var/tmp') / ('clj-surgeon-gate-%d' % os.getuid())


# The root inode this PROCESS published its first slot under. It is the root's
# identity for our lifetime: if it changes we are about to bind a second,
# disjoint semaphore beside our own live holdings, which is R3. We refuse
# instead. An INDEPENDENT process that starts after the swap has nothing to
# compare against -- that is the residual hole named in the module docstring,
# and it is precisely why linux does not use this backend.
_ROOT_IDENTITY = {}


def _ensure_root():
    """Create the root and return (path, inode), refusing a replaced root."""
    root = slot_root()
    root.mkdir(parents=True, exist_ok=True, mode=0o700)
    inode = os.stat(root).st_ino
    known = _ROOT_IDENTITY.get(str(root))
    if known is None:
        _ROOT_IDENTITY[str(root)] = inode
    elif known != inode:
        raise RuntimeError('gate-refused: the gate root was replaced under a '
                           'live holder (%s)' % root)
    return root, inode


def forget_root_identity():
    """Drop the remembered root identity. Test-only: a fresh process would not
    remember one, and a witness sometimes needs to model that."""
    _ROOT_IDENTITY.clear()


def slot_name(namespace, suffix):
    """The kernel's name for a slot.

    abstract: the leading NUL is the abstract namespace -- not a filesystem
    path, never touching a directory.
    path-socket: a pathname under the per-user root.
    """
    leaf = '%s-%s' % (namespace, suffix)
    if backend() == ABSTRACT:
        return b'\0' + leaf.encode('utf-8')
    return str(_ensure_root()[0] / leaf)


class _PathSlot(socket.socket):
    """A pathname slot that removes its own directory entry when released.

    A pathname socket has no kernel reaper, so a dead holder leaves a file
    behind; that file is reclaimable (its inode is dead and connect() refuses),
    but an orderly release should not leave one at all. The unlink is
    INODE-GUARDED: after our descriptor is closed another actor may already own
    this name, and removing their live entry is exactly the split this backend
    exists to avoid.
    """

    gate_path = None
    gate_inode = None

    def close(self):
        path, inode = self.gate_path, self.gate_inode
        self.gate_path = None
        try:
            super().close()
        finally:
            if path is not None:
                try:
                    if os.stat(path).st_ino == inode:
                        os.unlink(path)
                except (FileNotFoundError, NotADirectoryError):
                    pass


def _is_live(path):
    """True when a holder still owns this pathname socket.

    The holder listens, so connect() succeeds while it lives and fails
    ECONNREFUSED once its last descriptor is gone -- including after SIGKILL,
    because the kernel closed it. ENOENT means it is already gone.
    """
    probe = socket.socket(socket.AF_UNIX, socket.SOCK_STREAM)
    try:
        probe.settimeout(1.0)
        probe.connect(path)
        return True
    except OSError as error:
        if error.errno in (errno.ECONNREFUSED, errno.ENOENT):
            return False
        # EAGAIN/ETIMEDOUT: a live holder with a full backlog. Occupied.
        return True
    finally:
        probe.close()


def _claim_path(path):
    """Bind a pathname slot, or None when a live holder owns it.

    PUBLICATION IS ATOMIC. The socket is bound to a private name and then
    os.link'd into place: link fails EEXIST rather than clobbering, so two
    racing claimants cannot both believe they won. A stale entry (the holder
    died; the file survived because a pathname socket has no kernel reaper) is
    reclaimed only when its inode is unchanged since the liveness probe, which
    closes the reclaim-a-live-holder race that a bare unlink leaves open.
    """
    root, root_inode = _ensure_root()
    target = Path(path)
    private = root / ('.pending-%s' % uuid.uuid4().hex)
    sock = _PathSlot(socket.AF_UNIX, socket.SOCK_STREAM)
    try:
        sock.bind(str(private))
        sock.listen(64)
        for _ in range(8):
            try:
                current = os.stat(root).st_ino
            except FileNotFoundError:
                current = None
            if current != root_inode:
                raise RuntimeError('gate-refused: the gate root was replaced '
                                   'under a live holder (%s)' % root)
            try:
                os.link(str(private), str(target))
            except OSError as error:
                if error.errno != errno.EEXIST:
                    raise
                try:
                    occupant = os.stat(str(target)).st_ino
                except FileNotFoundError:
                    continue
                if _is_live(str(target)):
                    sock.close()
                    return None
                try:
                    if os.stat(str(target)).st_ino == occupant:
                        os.unlink(str(target))
                except FileNotFoundError:
                    pass
                continue
            os.unlink(str(private))
            sock.gate_path = str(target)
            sock.gate_inode = os.stat(str(target)).st_ino
            return sock
        sock.close()
        return None
    except BaseException:
        sock.close()
        raise
    finally:
        try:
            os.unlink(str(private))
        except FileNotFoundError:
            pass


def claim(name):
    """Bind the name, or None when another live holder owns it.

    Ownership is the bound socket itself. Closing it -- deliberately, on exit,
    or because the kernel reaped the process -- releases the name at once
    (abstract) or makes it reclaimable by the next acquirer (path-socket).
    """
    if isinstance(name, str):
        return _claim_path(name)
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
