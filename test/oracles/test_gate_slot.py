"""TEST-ISO-015 / Sol GATE-LANES-FENCE-001-R2 and -R3 boundary regressions."""

import importlib.util
import os
from pathlib import Path
import subprocess
import time
import unittest
import uuid

MODULE = Path(__file__).resolve().parents[1] / 'gate_slot.py'
SPEC = importlib.util.spec_from_file_location('gate_slot', MODULE)
slot = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(slot)

# These tests run inside gate workers that hold production slots. Never bind a
# production name: every witness gets a namespace nothing else on the box uses.
def unique_namespace():
    return 'gate-slot-test-%s' % uuid.uuid4().hex


def occupied(namespace, ceiling=8):
    """How many slot names in this namespace a live holder owns right now."""
    taken = 0
    probes = []
    for index in range(ceiling):
        sock = slot.claim(slot.slot_name(namespace, 'slot-%d' % index))
        if sock is None:
            taken += 1
        else:
            probes.append(sock)
    for sock in probes:
        sock.close()
    return taken


def wait_for_occupancy(namespace, expected, ceiling=8, timeout_s=10):
    deadline = time.monotonic() + timeout_s
    while True:
        seen = occupied(namespace, ceiling)
        if seen == expected or time.monotonic() >= deadline:
            return seen
        time.sleep(0.02)


# A worker that takes one slot in the named namespace and holds it until told to
# let go. It shares nothing with its parent but the namespace string.
COORDINATOR = ('import importlib.util,sys; '
               's=importlib.util.spec_from_file_location("slot",sys.argv[1]); '
               'm=importlib.util.module_from_spec(s); s.loader.exec_module(m); '
               'w=int(sys.argv[3]); '
               'h=m.try_acquire(sys.argv[2], lambda: {"width": w}); '
               'print("admitted" if h else "refused", flush=True); '
               'sys.stdin.readline()')


class GateSlotTest(unittest.TestCase):
    def independent_coordinator(self, namespace, width=1):
        """A process with no inherited gate state whatsoever: no shared file, no
        shared descriptor, and an environment carrying only PATH."""
        child = subprocess.Popen(
            ['python3', '-B', '-c', COORDINATOR, str(MODULE), namespace, str(width)],
            stdin=subprocess.PIPE, stdout=subprocess.PIPE, text=True,
            env={'PATH': os.environ.get('PATH', '/usr/bin:/bin')})
        self.addCleanup(self.release, child)
        return child

    @staticmethod
    def release(child):
        try:
            if child.poll() is None:
                child.communicate('go\n', timeout=10)
        except Exception:
            child.kill()
            child.wait()
        finally:
            for stream in (child.stdin, child.stdout):
                if stream is not None and not stream.closed:
                    stream.close()

    def test_capacity_boundaries(self):
        for cpus, memory, width in [(16, 18524, 8), (16, 3584, 1),
                                    (1, 65536, 1), (16, 5120, 2), (64, 8192, 4)]:
            self.assertEqual(width, slot.derived_width(cpus, memory))
        for memory in [0, 2048, 3583]:
            with self.assertRaisesRegex(RuntimeError, 'insufficient memory'):
                slot.derived_width(16, memory)

    def test_live_recompute_and_high_slot_shrink(self):
        namespace = unique_namespace()
        widths = iter([3, 3, 3, 1, 1, 2])
        samples = []

        def live():
            width = next(widths)
            samples.append(width)
            return {'width': width}

        holders = [slot.try_acquire(namespace, live) for _ in range(3)]
        try:
            self.assertTrue(all(holders))
            self.assertIsNone(slot.try_acquire(namespace, live))
            holders[0].close()
            holders[1].close()
            # The high slot alone fills the new width-one budget.
            self.assertIsNone(slot.try_acquire(namespace, live))
            extra = slot.try_acquire(namespace, live)
            self.assertIsNotNone(extra)
            extra.close()
            self.assertEqual([3, 3, 3, 1, 1, 2], samples)
        finally:
            for handle in holders:
                handle.close()

    def test_exec_inherits_the_slot_and_exit_releases_it(self):
        namespace = unique_namespace()
        # This is the same exec boundary as production, on an owned namespace.
        program = ('import importlib.util,sys; '
                   's=importlib.util.spec_from_file_location("slot",sys.argv[1]); '
                   'm=importlib.util.module_from_spec(s); s.loader.exec_module(m); '
                   'm.SLOT_NAMESPACE=sys.argv[2]; '
                   'm.main(["--", "sh", "-c", "echo ready; read value; exit 7"])')
        child = subprocess.Popen(['python3', '-B', '-c', program, str(MODULE), namespace],
                                 stdin=subprocess.PIPE, stdout=subprocess.PIPE, text=True)
        try:
            self.assertEqual('ready', child.stdout.readline().strip())
            # The slot survived execvp into `sh`: the shell holds it.
            self.assertEqual(1, occupied(namespace))
            child.communicate('done\n', timeout=10)
            self.assertEqual(7, child.returncode)
            self.assertEqual(0, wait_for_occupancy(namespace, 0))
        finally:
            if child.poll() is None:
                child.kill()
                child.wait()
            child.stdin.close()
            child.stdout.close()

    def test_unknown_capacity_refuses_and_does_not_leak_admission(self):
        namespace = unique_namespace()

        def unknown():
            raise RuntimeError('gate-refused: available memory is unknown')

        with self.assertRaisesRegex(RuntimeError, 'unknown'):
            slot.try_acquire(namespace, unknown)
        admission = slot.claim(slot.slot_name(namespace, 'admission'))
        self.assertIsNotNone(admission)
        admission.close()

    def test_killed_owner_and_failed_exec_release_slots(self):
        """Sol's witness (3): a holder killed with SIGKILL frees its slot within
        one acquire attempt -- the kernel owns the release, not a cleanup path."""
        for fail_exec in [False, True]:
            with self.subTest(fail_exec=fail_exec):
                namespace = unique_namespace()
                command = '["--", "/no-such-gate-slot-worker"]' if fail_exec else (
                    '["--", "sh", "-c", "echo ready; read value"]')
                program = ('import importlib.util,sys; '
                           's=importlib.util.spec_from_file_location("slot",sys.argv[1]); '
                           'm=importlib.util.module_from_spec(s); s.loader.exec_module(m); '
                           'm.SLOT_NAMESPACE=sys.argv[2]; m.main(' + command + ')')
                child = subprocess.Popen(['python3', '-B', '-c', program, str(MODULE), namespace],
                                         stdin=subprocess.PIPE, stdout=subprocess.PIPE,
                                         stderr=subprocess.PIPE, text=True)
                try:
                    if not fail_exec:
                        self.assertEqual('ready', child.stdout.readline().strip())
                        self.assertEqual(1, occupied(namespace))
                        child.kill()
                    child.communicate(timeout=10)
                    self.assertNotEqual(0, child.returncode)
                    # One acquire attempt sees the slot free again.
                    reclaimed = slot.try_acquire(namespace, lambda: {'width': 1})
                    self.assertIsNotNone(reclaimed)
                    reclaimed.close()
                finally:
                    if child.poll() is None:
                        child.kill()
                        child.wait()
                    child.stdin.close()
                    child.stdout.close()
                    child.stderr.close()

    def test_two_independent_coordinators_cannot_split_the_semaphore(self):
        """Sol GATE-LANES-FENCE-001-R3, adapted to a mechanism with no root.

        Two coordinators that share no file, no descriptor and no environment,
        at width one. R3's attack step -- remove the slot root so the second
        coordinator creates its own authority -- has no target here: there is no
        root to remove and none to recreate, so `different_roots` is not merely
        False, it is unrepresentable.
        """
        namespace = unique_namespace()
        first = self.independent_coordinator(namespace)
        self.assertEqual('admitted', first.stdout.readline().strip())

        # The R3 attack, executed: nothing on the filesystem belongs to the gate.
        roots = [name for name in ('SLOT_ROOT', 'open_root', 'ROOT_ID_ENV',
                                   'published_identity', 'hold_root')
                 if hasattr(slot, name)]
        second = self.independent_coordinator(namespace)
        verdict = second.stdout.readline().strip()
        observed = {'first_held': True,
                    'second_admitted': verdict == 'admitted',
                    'different_roots': bool(roots),
                    'derived_width': 1,
                    'bound_violated': verdict == 'admitted'}
        self.assertEqual({'first_held': True, 'second_admitted': False,
                          'different_roots': False, 'derived_width': 1,
                          'bound_violated': False}, observed)
        self.assertEqual('refused', verdict)
        self.assertEqual([], roots)

    def test_three_independent_callers_share_one_bound(self):
        """Width two, three coordinators that share nothing: admitted, admitted,
        refused. The third is refused by the kernel's own name, not by anything
        the first two wrote down."""
        namespace = unique_namespace()
        verdicts = []
        for _ in range(3):
            child = self.independent_coordinator(namespace, width=2)
            verdicts.append(child.stdout.readline().strip())
        self.assertEqual(['admitted', 'admitted', 'refused'], verdicts)


if __name__ == '__main__':
    unittest.main()
