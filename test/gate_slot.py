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


# The gate's memory arithmetic, kept spelling-for-spelling with
# clj-surgeon.gate-memory (test/clj_surgeon/gate_memory.clj). This module holds
# the per-acquire re-read because a slot must not pay a JVM start; the numbers
# and the sentence are the coordinator's.
RESERVE_MIB = 2048
LANE_CHARGE_MIB = 1536
SINGLE_LANE_FLOOR_MIB = RESERVE_MIB + LANE_CHARGE_MIB
FLOOR_NOTE = ('%d MiB floor = reserve %d + %d per lane; GATE_MEMAVAIL_MIB=<MiB> '
              'declares what this box may lend'
              % (SINGLE_LANE_FLOOR_MIB, RESERVE_MIB, LANE_CHARGE_MIB))


def insufficient_memory(memory_mib):
    """The refusal, naming the floor, the formula and the override in one line.

    Until 2026-09-10 it said only `insufficient memory for a bounded lane`, and
    the skiff's operator -- who had already granted 3072 MiB by hand -- had no
    way to learn that 3584 was the number or where it came from.
    """
    return RuntimeError('gate-refused: insufficient memory for a bounded lane '
                        '-- %d MiB available, %s' % (memory_mib, FLOOR_NOTE))


def derived_width(cpus, memory_mib):
    allowance = (memory_mib - RESERVE_MIB) // LANE_CHARGE_MIB
    if allowance < 1:
        raise insufficient_memory(memory_mib)
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


# ---------------------------------------------------------------------------
# WHERE THE PATHNAME SLOTS LIVE, and the 104-byte cliff they fell off
#
# `struct sockaddr_un.sun_path` is 104 bytes on darwin and 108 on linux, NUL
# included. It is not advisory and it is not negotiable: bind() answers ENAMETOOLONG.
#
# On 2026-09-10 the skiff refused `AF_UNIX path too long` in 163 ms and took
# `make test` with it. macOS hands every login a per-user TMPDIR of the shape
#     /var/folders/wc/2c5b8h1s7dncqxwf1p1z0z_r0000gn/T/
# which is 63 bytes before this module appends `clj-surgeon-gate/` and a leaf
# `clj-surgeon-gate-admission`. That is 106 bytes and it never had a chance.
# The `/var/tmp` fallback fits, but only a box with NO TMPDIR ever reached it,
# and macOS always sets one.
#
# So the root is chosen by whether it FITS, and darwin is short by default.
# `/tmp/csg-<uid>` is 12 or 13 bytes; on macOS `/tmp` is a symlink to
# `/private/tmp` and is DISK-BACKED, so the suite's no-RAM-temp rule is not
# bent here -- and this directory holds SOCKETS ONLY. Scratch files still
# belong under TMPDIR, where the tmp-hygiene ratchet can see them.
SUN_PATH_MAX = 104

# Stay clear of the smaller cap and its terminating NUL. A gate that binds at
# byte 103 is a gate that breaks on the next slot-name change.
SUN_PATH_BUDGET = 100

# The longest name this module will ever bind, so the fit is decided ONCE,
# against the real worst case, instead of per-slot and by luck.
#
# THE WORST CASE IS NOT A SLOT NAME. It is the `.pending-<32 hex>` private name
# that `_claim_path` binds BEFORE linking a slot into place -- 41 bytes against
# a slot leaf's 26. Sizing this to the slot names alone reproduces the skiff
# bug exactly: `/var/folders/../T/clj-surgeon-gate` (65 bytes) holds
# `clj-surgeon-gate-admission` at 92 and blows sun_path at 107 on the private
# name, which is where bind() actually said `AF_UNIX path too long`. The fit
# check has to cover every name the module binds, not the names it advertises.
PENDING_PREFIX = '.pending-'
LONGEST_LEAF = max(len('%s-admission' % SLOT_NAMESPACE),
                   len('%s-slot-%d' % (SLOT_NAMESPACE, MAX_SLOTS - 1)),
                   len(PENDING_PREFIX) + 32)


def root_fits(root):
    """True when EVERY leaf this module can ask for fits under `root`."""
    return len(str(root)) + 1 + LONGEST_LEAF <= SUN_PATH_BUDGET


def short_slot_root(uid):
    """The short per-user root. Sockets only; never a scratch directory."""
    return Path('/tmp') / ('csg-%d' % uid)


def slot_root_for(platform, tmpdir, uid, declared=None):
    """The root, from the box's facts. PURE, so darwin is decided in a witness.

    Order: an explicit CLJ_SURGEON_GATE_ROOT always wins -- an operator who
    names a directory gets that directory, and a refusal if it cannot hold a
    slot, never a silent relocation to somewhere they did not name. Otherwise
    darwin goes short by construction, and every other platform goes short only
    when its own answer would not fit.

    Never a shared world-writable root: `_ensure_root` proves ownership and
    mode, because /tmp is world-writable and sticky and another user must not
    be able to pre-create a slot name and lock the gate out.
    """
    if declared:
        return Path(declared)
    if platform == 'darwin':
        return short_slot_root(uid)
    candidate = Path(tmpdir) / 'clj-surgeon-gate' if tmpdir else \
        Path('/var/tmp') / ('clj-surgeon-gate-%d' % uid)
    return candidate if root_fits(candidate) else short_slot_root(uid)


def slot_root():
    """The per-user runtime directory holding pathname slots."""
    return slot_root_for(sys.platform, os.environ.get('TMPDIR'), os.getuid(),
                         os.environ.get('CLJ_SURGEON_GATE_ROOT'))


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
    stat = os.stat(root)
    # /tmp is world-writable and sticky, so the short root has to PROVE it is
    # ours rather than assume it. mkdir(mode=) applies only when it creates,
    # and umask can narrow it, so neither owner nor mode is established by the
    # call above. A root somebody else owns is a typed refusal, never a bind.
    if stat.st_uid != os.getuid():
        raise RuntimeError('gate-refused: the gate root %s is owned by uid %d, '
                           'not by this user (%d)'
                           % (root, stat.st_uid, os.getuid()))
    if stat.st_mode & 0o077:
        os.chmod(root, 0o700)
    inode = stat.st_ino
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
    path = str(_ensure_root()[0] / leaf)
    # The cap is checked HERE, once, against the path actually about to be
    # bound. A caller may name any root it likes with CLJ_SURGEON_GATE_ROOT,
    # and a namespace of any length: the skiff learned about sun_path from a
    # bare `AF_UNIX path too long` out of bind(), which named neither the path
    # nor the limit nor who chose them.
    if len(path.encode('utf-8')) > SUN_PATH_BUDGET:
        raise RuntimeError('gate-refused: the slot path is %d bytes, over the '
                           '%d-byte AF_UNIX budget (sun_path is %d on darwin): '
                           '%s -- set CLJ_SURGEON_GATE_ROOT to a shorter '
                           'directory' % (len(path.encode('utf-8')),
                                          SUN_PATH_BUDGET, SUN_PATH_MAX, path))
    return path


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
    private = root / ('%s%s' % (PENDING_PREFIX, uuid.uuid4().hex))
    if len(str(private).encode('utf-8')) > SUN_PATH_BUDGET:
        raise RuntimeError('gate-refused: the private publication path is %d '
                           'bytes, over the %d-byte AF_UNIX budget (sun_path '
                           'is %d on darwin): %s -- set CLJ_SURGEON_GATE_ROOT '
                           'to a shorter directory'
                           % (len(str(private).encode('utf-8')),
                              SUN_PATH_BUDGET, SUN_PATH_MAX, private))
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
            raise insufficient_memory(current['memory-mib'])
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
    # The coordinator asks for the root so the LANDING RECEIPT can name the
    # directory the slots were published under. One implementation answers; the
    # receipt does not get a second opinion about its own semaphore.
    if argv[:1] == ['--print-root']:
        # `abstract` is the honest answer on linux: there IS no directory, and
        # printing one would invite a reader to go looking for it.
        print('abstract' if backend() == ABSTRACT else slot_root())
        return
    if argv[:1] == ['--print-slot-budget']:
        # Worst case, budget, cap. The preflight prints these; it does not
        # recompute them, because a second implementation of the arithmetic is
        # how the memory reader came to disagree with its own preflight.
        print('%d %d %d' % (len(str(slot_root())) + 1 + LONGEST_LEAF,
                            SUN_PATH_BUDGET, SUN_PATH_MAX))
        return
    if len(argv) < 2 or argv[0] != '--':
        raise RuntimeError('usage: gate_slot.py -- COMMAND [ARG ...]\n'
                           '       gate_slot.py --print-root')
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
