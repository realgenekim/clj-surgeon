#!/usr/bin/env python3
"""TEST-ISO-015: box-wide worker admission, inherited across exec.

Only lock ownership denotes occupancy. Never unlink slot files: doing so would
create two separately lockable inodes for the same slot. No environment width
override or cached startup memory can enlarge the live admission budget.
"""

import fcntl
import json
import os
from pathlib import Path
import re
import sys
import time

SLOT_ROOT = Path('/var/tmp/forge/gate-slots')


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


def try_acquire(root, read_capacity=capacity):
    """Serialize sample/count/reservation; include holders above a shrunken width."""
    root.mkdir(parents=True, exist_ok=True)
    with (root / 'admission.lock').open('a+') as admission:
        fcntl.flock(admission, fcntl.LOCK_EX)
        current = read_capacity()
        width = current['width']
        for index in range(width):
            (root / f'slot-{index}.lock').touch(exist_ok=True)
        free = []
        occupied = 0
        try:
            for path in sorted(root.glob('slot-*.lock')):
                handle = path.open('r+')
                try:
                    fcntl.flock(handle, fcntl.LOCK_EX | fcntl.LOCK_NB)
                except BlockingIOError:
                    occupied += 1
                    handle.close()
                else:
                    free.append(handle)
            eligible = next((h for h in free
                             if int(Path(h.name).stem.split('-')[1]) < width), None)
            if occupied >= width or eligible is None:
                return None
            eligible.seek(0)
            eligible.truncate()
            json.dump({**current, 'pid': os.getpid(), 'admitted-at': time.time(),
                       'occupied-after': occupied + 1}, eligible)
            eligible.flush()
            os.set_inheritable(eligible.fileno(), True)
            free.remove(eligible)
            return eligible
        finally:
            for handle in free:
                handle.close()


def main(argv):
    if len(argv) < 2 or argv[0] != '--':
        raise RuntimeError('usage: gate_slot.py -- COMMAND [ARG ...]')
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
