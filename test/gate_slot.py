#!/usr/bin/env python3
"""TEST-ISO-015: box-wide worker admission, inherited across exec.

Only lock ownership denotes occupancy. Never unlink slot files: doing so would
create two separately lockable inodes for the same slot. No environment width
override or cached startup memory can enlarge the live admission budget.

Sol GATE-LANES-FENCE-001-R2: POSIX flock names an inode, not a path, so a
removed or replaced slot root used to split the semaphore in two -- the holder
kept locking the unlinked inode while the next caller recreated the directory
and locked a brand new one. The root is therefore the coordinator's: it is
created exactly once by `open_root` at run start, the coordinator holds that
directory fd for the whole run so the kernel cannot reuse the inode number, and
`try_acquire` never creates anything above a slot file. Every open here is
relative to one directory fd, and both the admission lock and the taken slot
are re-identified by (device, inode) while held. A missing root is
`gate-refused: slot root missing`; a root that is no longer the coordinator's
inode is `gate-refused: slot root replaced`. Neither is ever repaired by
recreating the root underneath a live holder.
"""

import fcntl
import json
import os
from pathlib import Path
import re
import sys
import time

SLOT_ROOT = Path('/var/tmp/forge/gate-slots')

# The coordinator publishes `<absolute root path>|<device>:<inode>` here; every
# worker it starts inherits it and refuses any other inode AT THAT PATH. The
# path is half the value on purpose: a worker running this module's own tests
# against its own temporary root must not be judged against the box root.
ROOT_ID_ENV = 'GATE_SLOT_ROOT_ID'

SLOT_NAME = re.compile(r'^slot-(\d+)\.lock$')


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


def _identity(status):
    """The kernel's name for a file: a path is not an identity, this is."""
    return '%d:%d' % (status.st_dev, status.st_ino)


def root_identity(dir_fd):
    return _identity(os.fstat(dir_fd))


def published_identity(root, dir_fd):
    """What the coordinator hands its workers: the path AND the inode."""
    return '%s|%s' % (os.path.abspath(root), root_identity(dir_fd))


def _expected_from_env(root):
    """The published identity, but only for the root it was published for."""
    published = os.environ.get(ROOT_ID_ENV) or ''
    path, separator, identity = published.partition('|')
    if not separator or path != os.path.abspath(root):
        return None
    return identity or None


def open_root(root):
    """Coordinator only: create the slot root exactly once and pin its inode.

    Hold the returned fd for the run. While it is open the kernel cannot reuse
    that inode number, so a worker comparing identities cannot be fooled by a
    root that was removed and recreated at the same path.
    """
    root.mkdir(parents=True, exist_ok=True)
    return os.open(root, os.O_RDONLY | os.O_DIRECTORY)


def _open_at(dir_fd, name, flags):
    """Open strictly inside the coordinator's directory inode (openat)."""
    fd = os.open(name, flags, 0o600, dir_fd=dir_fd)
    try:
        return os.fdopen(fd, 'r+')
    except BaseException:
        os.close(fd)
        raise


def _same_inode(handle, name, dir_fd):
    try:
        return (_identity(os.fstat(handle.fileno()))
                == _identity(os.stat(name, dir_fd=dir_fd)))
    except OSError:
        return False


def _root_intact(dir_fd, root, expected):
    """The directory we hold open is still the one this path names, and it is
    still the coordinator's."""
    try:
        if _identity(os.fstat(dir_fd)) != _identity(os.stat(root)):
            return False
    except OSError:
        return False
    return expected is None or root_identity(dir_fd) == expected


def try_acquire(root, read_capacity=capacity, expected=None):
    """Serialize sample/count/reservation; include holders above a shrunken width.

    Refuses -- never recreates -- when the slot root is missing or is not the
    coordinator's inode, so a deleted root can never produce a second live
    semaphore beside the first.
    """
    if expected is None:
        expected = _expected_from_env(root)
    try:
        dir_fd = os.open(root, os.O_RDONLY | os.O_DIRECTORY)
    except OSError:
        raise RuntimeError('gate-refused: slot root missing') from None
    try:
        if not _root_intact(dir_fd, root, expected):
            raise RuntimeError('gate-refused: slot root replaced')
        with _open_at(dir_fd, 'admission.lock', os.O_RDWR | os.O_CREAT) as admission:
            fcntl.flock(admission, fcntl.LOCK_EX)
            if not (_same_inode(admission, 'admission.lock', dir_fd)
                    and _root_intact(dir_fd, root, expected)):
                raise RuntimeError('gate-refused: slot root replaced')
            current = read_capacity()
            width = current['width']
            for index in range(width):
                _open_at(dir_fd, 'slot-%d.lock' % index,
                         os.O_RDWR | os.O_CREAT).close()
            free = []
            occupied = 0
            try:
                for name in sorted(n for n in os.listdir(dir_fd) if SLOT_NAME.match(n)):
                    handle = _open_at(dir_fd, name, os.O_RDWR)
                    try:
                        fcntl.flock(handle, fcntl.LOCK_EX | fcntl.LOCK_NB)
                    except BlockingIOError:
                        occupied += 1
                        handle.close()
                    else:
                        free.append((int(SLOT_NAME.match(name)[1]), name, handle))
                eligible = next((entry for entry in free if entry[0] < width), None)
                if occupied >= width or eligible is None:
                    return None
                _, name, handle = eligible
                if not (_same_inode(handle, name, dir_fd)
                        and os.fstat(handle.fileno()).st_dev == os.fstat(dir_fd).st_dev
                        and _root_intact(dir_fd, root, expected)):
                    raise RuntimeError('gate-refused: slot root replaced')
                handle.seek(0)
                handle.truncate()
                json.dump({**current, 'pid': os.getpid(), 'admitted-at': time.time(),
                           'occupied-after': occupied + 1}, handle)
                handle.flush()
                os.set_inheritable(handle.fileno(), True)
                free.remove(eligible)
                return handle
            finally:
                for _, _, handle in free:
                    handle.close()
    finally:
        os.close(dir_fd)


def hold_root(root, expected=None):
    """The coordinator entrance: create the root once, publish its identity on
    stdout, and hold its fd until stdin closes -- the coordinator's lifetime.

    A nested coordinator inherits the outer coordinator's published identity and
    refuses here rather than coordinating over a second inode."""
    if expected is None:
        expected = _expected_from_env(root)
    dir_fd = open_root(root)
    try:
        if expected is not None and root_identity(dir_fd) != expected:
            raise RuntimeError('gate-refused: slot root replaced')
        print(published_identity(root, dir_fd), flush=True)
        sys.stdin.read()
    finally:
        os.close(dir_fd)


def main(argv):
    if argv and argv[0] == '--hold-root':
        return hold_root(SLOT_ROOT)
    if len(argv) < 2 or argv[0] != '--':
        raise RuntimeError('usage: gate_slot.py [--hold-root] | -- COMMAND [ARG ...]')
    while True:
        slot = try_acquire(SLOT_ROOT)
        if slot is not None:
            break
        time.sleep(0.05)
    # The worker itself owns the open file description. SIGKILL and exec
    # failure release it without a coordinator cleanup callback.
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
