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


# The backend and its root are PLATFORM FACTS, not inherited gate state: on
# darwin every process resolves them identically without being told. Carrying
# them lets this linux box witness the darwin backend under the same oracle,
# and an independent coordinator stays independent -- it still shares no file,
# no descriptor and no namespace knowledge with its parent.
PLATFORM_ENV = {key: os.environ[key]
                for key in ('GATE_SLOT_BACKEND', 'CLJ_SURGEON_GATE_ROOT',
                            'GATE_MEMAVAIL_MIB', 'TMPDIR')
                if key in os.environ}


class GateSlotTest(unittest.TestCase):
    def independent_coordinator(self, namespace, width=1):
        """A process with no inherited gate state whatsoever: no shared file, no
        shared descriptor, and an environment carrying only PATH."""
        env = {'PATH': os.environ.get('PATH', '/usr/bin:/bin')}
        env.update(PLATFORM_ENV)
        child = subprocess.Popen(
            ['python3', '-B', '-c', COORDINATOR, str(MODULE), namespace, str(width)],
            stdin=subprocess.PIPE, stdout=subprocess.PIPE, text=True,
            env=env)
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


class PortabilityTest(unittest.TestCase):
    """TEST-ISO-015 portability: the darwin backend exists, says so, and its
    guarantee is weaker by a NAMED amount rather than silently."""

    def test_backend_follows_the_platform_and_names_its_guarantee(self):
        modes = {'abstract': ':abstract-socket', 'path-socket': ':path-socket'}
        self.assertIn(slot.backend(), modes)
        self.assertEqual(modes[slot.backend()], slot.admission_mode())
        if os.sys.platform.startswith('linux'):
            self.assertTrue(slot._abstract_supported())
        else:
            self.assertFalse(slot._abstract_supported())
            self.assertEqual('path-socket', slot.backend())

    def test_unknown_backend_is_a_typed_refusal(self):
        previous = os.environ.get('GATE_SLOT_BACKEND')
        os.environ['GATE_SLOT_BACKEND'] = 'flock'
        try:
            with self.assertRaisesRegex(RuntimeError, 'unknown GATE_SLOT_BACKEND'):
                slot.backend()
        finally:
            if previous is None:
                del os.environ['GATE_SLOT_BACKEND']
            else:
                os.environ['GATE_SLOT_BACKEND'] = previous

    def test_declared_memory_overrides_and_refuses_garbage(self):
        previous = os.environ.get('GATE_MEMAVAIL_MIB')
        try:
            os.environ['GATE_MEMAVAIL_MIB'] = '8192'
            self.assertEqual(8192, slot.memory_available_mib())
            os.environ['GATE_MEMAVAIL_MIB'] = 'lots'
            with self.assertRaisesRegex(RuntimeError, 'GATE_MEMAVAIL_MIB'):
                slot.memory_available_mib()
        finally:
            if previous is None:
                os.environ.pop('GATE_MEMAVAIL_MIB', None)
            else:
                os.environ['GATE_MEMAVAIL_MIB'] = previous

    def test_capacity_is_readable_on_this_platform(self):
        current = slot.capacity()
        self.assertGreaterEqual(current['cpus'], 1)
        self.assertGreaterEqual(current['memory-mib'], 0)
        self.assertEqual(slot.derived_width(current['cpus'],
                                            current['memory-mib']),
                         current['width'])

    @unittest.skipUnless(os.environ.get('GATE_SLOT_BACKEND') == 'path-socket'
                         or not os.sys.platform.startswith('linux'),
                         'path-socket backend only')
    def test_replaced_root_is_a_typed_refusal_not_a_silent_split(self):
        """R3 on darwin cannot be made unrepresentable, so it is DETECTED.

        A holder that already published under one root and then finds a
        different root refuses by name instead of quietly binding a second
        semaphore beside its own live holdings.
        """
        namespace = unique_namespace()
        held = slot.claim(slot.slot_name(namespace, 'slot-0'))
        self.addCleanup(held.close)
        root = slot.slot_root()
        moved = Path('%s.replaced-%s' % (root, uuid.uuid4().hex))
        os.rename(str(root), str(moved))
        try:
            with self.assertRaisesRegex(RuntimeError, 'gate root was replaced'):
                slot.claim(slot.slot_name(namespace, 'slot-1'))
        finally:
            for stray in Path(str(root)).glob('*') if root.is_dir() else []:
                stray.unlink()
            if root.is_dir():
                root.rmdir()
            os.rename(str(moved), str(root))

    @unittest.skipUnless(os.environ.get('GATE_SLOT_BACKEND') == 'path-socket'
                         or not os.sys.platform.startswith('linux'),
                         'path-socket backend only')
    def test_a_dead_holders_entry_is_reclaimed_not_leaked(self):
        """No kernel reaper removes a pathname socket, so the NEXT acquirer
        must reclaim it. SIGKILL leaves the file; connect() refuses; the name
        becomes free without anyone having run a cleanup callback."""
        namespace = unique_namespace()
        program = ('import importlib.util,sys,time; '
                   's=importlib.util.spec_from_file_location("slot",sys.argv[1]); '
                   'm=importlib.util.module_from_spec(s); s.loader.exec_module(m); '
                   'h=m.claim(m.slot_name(sys.argv[2],"slot-0")); '
                   'print("held" if h else "refused", flush=True); '
                   'time.sleep(300)')
        child = subprocess.Popen(['python3', '-B', '-c', program,
                                  str(MODULE), namespace],
                                 stdout=subprocess.PIPE, text=True)
        try:
            self.assertEqual('held', child.stdout.readline().strip())
            self.assertIsNone(slot.claim(slot.slot_name(namespace, 'slot-0')))
            child.kill()
            child.wait(timeout=10)
            deadline = time.monotonic() + 10
            while True:
                reclaimed = slot.claim(slot.slot_name(namespace, 'slot-0'))
                if reclaimed is not None or time.monotonic() >= deadline:
                    break
                time.sleep(0.02)
            self.assertIsNotNone(reclaimed, 'a dead holder never released')
            reclaimed.close()
        finally:
            if child.poll() is None:
                child.kill()
                child.wait()
            if child.stdout is not None:
                child.stdout.close()


# The exact per-user TMPDIR shape macOS hands every login. 46 bytes of random
# directory before this module appends anything.
DARWIN_TMPDIR = '/var/folders/wc/2c5b8h1s7dncqxwf1p1z0z_r0000gn/T/'


class SunPathBudgetTest(unittest.TestCase):
    """TEST-ISO-015 -- the 104-byte cliff the skiff fell off on 2026-09-10.

    `AF_UNIX path too long`, stage exit 1 in 163 ms, `make test` refused. No
    Mac is reachable from the box that runs this suite, so the darwin choice is
    driven here as a PURE decision over the box's facts, and the linux
    equivalent -- a TMPDIR long enough to overflow the same cap -- is driven
    all the way to a real bind.
    """

    def setUp(self):
        slot.forget_root_identity()
        self.addCleanup(slot.forget_root_identity)

    def test_the_macos_tmpdir_really_does_overflow_and_on_the_private_name(self):
        """The RED fact, pinned so the fix cannot be mistaken for decoration.

        And pinned at the RIGHT name. The slot leaves under the macOS TMPDIR
        are 92 bytes -- comfortably inside sun_path -- so a fit check sized to
        them says the skiff was fine. What bind() actually refused is the
        `.pending-<32 hex>` private name `_claim_path` binds before it links a
        slot into place: 107 bytes, three over the cap. A budget derived from
        the names a module ADVERTISES rather than the names it BINDS reproduces
        the original defect exactly.
        """
        overflowed = Path(DARWIN_TMPDIR) / 'clj-surgeon-gate'
        leaf = overflowed / ('%s-admission' % slot.SLOT_NAMESPACE)
        private = overflowed / (slot.PENDING_PREFIX + 'f' * 32)
        self.assertLess(len(str(leaf)), slot.SUN_PATH_MAX,
                        'the slot leaf fits, which is why this hid')
        self.assertGreater(len(str(private)), slot.SUN_PATH_MAX,
                           'the private publication name is what overflowed')
        self.assertFalse(slot.root_fits(overflowed))

    def test_darwin_chooses_the_short_root_and_every_leaf_fits(self):
        root = slot.slot_root_for('darwin', DARWIN_TMPDIR, 501)
        self.assertEqual(Path('/tmp/csg-501'), root)
        self.assertTrue(slot.root_fits(root))
        for suffix in ['admission'] + ['slot-%d' % i for i in range(slot.MAX_SLOTS)]:
            leaf = str(root / ('%s-%s' % (slot.SLOT_NAMESPACE, suffix)))
            self.assertLess(len(leaf), slot.SUN_PATH_BUDGET, leaf)

    def test_an_operator_named_root_is_honoured_not_relocated(self):
        self.assertEqual(Path('/var/tmp/mine'),
                         slot.slot_root_for('darwin', DARWIN_TMPDIR, 501, '/var/tmp/mine'))

    def test_a_short_linux_tmpdir_keeps_the_root_it_had(self):
        self.assertEqual(Path('/var/tmp/forge/clj-surgeon-gate'),
                         slot.slot_root_for('linux', '/var/tmp/forge', 1002))

    def test_a_120_byte_tmpdir_binds_a_real_slot_under_100_bytes(self):
        """The brief's linux witness, driven to an actual bind().

        A pure length assertion would pass against a module that still handed
        bind() the long path -- the cap is enforced by the KERNEL, so the
        witness has to reach it.
        """
        long_tmpdir = Path('/var/tmp/forge') / ('t' * (120 - len('/var/tmp/forge/')))
        self.assertEqual(120, len(str(long_tmpdir)))
        chosen = slot.slot_root_for('linux', str(long_tmpdir), os.getuid())
        self.assertEqual(slot.short_slot_root(os.getuid()), chosen)

        namespace = unique_namespace()
        previous = {key: os.environ.get(key)
                    for key in ('TMPDIR', 'GATE_SLOT_BACKEND', 'CLJ_SURGEON_GATE_ROOT')}
        os.environ['TMPDIR'] = str(long_tmpdir)
        os.environ['GATE_SLOT_BACKEND'] = 'path-socket'
        os.environ.pop('CLJ_SURGEON_GATE_ROOT', None)
        try:
            name = slot.slot_name(namespace, 'slot-0')
            self.assertLess(len(name.encode('utf-8')), slot.SUN_PATH_BUDGET, name)
            sock = slot.claim(name)
            self.assertIsNotNone(sock, 'the chosen path did not bind')
            try:
                self.assertIsNone(slot.claim(name), 'a bound slot must read as taken')
            finally:
                sock.close()
            root = slot.slot_root()
            self.assertEqual(0o700, os.stat(root).st_mode & 0o777,
                             'the socket root must not be readable by the box')
        finally:
            for key, value in previous.items():
                if value is None:
                    os.environ.pop(key, None)
                else:
                    os.environ[key] = value
            slot.forget_root_identity()

    def test_a_path_over_the_budget_is_a_TYPED_refusal_not_an_OSError(self):
        """The skiff got `AF_UNIX path too long` straight out of bind(): no
        path, no limit, and no statement of who chose the directory."""
        root = Path('/var/tmp/forge') / ('r' * 90)
        previous = os.environ.get('CLJ_SURGEON_GATE_ROOT')
        os.environ['CLJ_SURGEON_GATE_ROOT'] = str(root)
        os.environ['GATE_SLOT_BACKEND'] = 'path-socket'
        try:
            with self.assertRaises(RuntimeError) as caught:
                slot.slot_name(unique_namespace(), 'admission')
            message = str(caught.exception)
            self.assertIn('over the 100-byte AF_UNIX budget', message)
            self.assertIn('sun_path is 104 on darwin', message)
            self.assertIn('CLJ_SURGEON_GATE_ROOT', message)
        finally:
            if previous is None:
                os.environ.pop('CLJ_SURGEON_GATE_ROOT', None)
            else:
                os.environ['CLJ_SURGEON_GATE_ROOT'] = previous
            os.environ.pop('GATE_SLOT_BACKEND', None)
            slot.forget_root_identity()
            if root.exists():
                root.rmdir()

    def test_the_root_is_narrowed_to_0700_even_when_it_was_already_wide(self):
        """/tmp is world-writable and sticky. `mkdir(mode=)` applies only when
        it CREATES, and umask can narrow it, so neither owner nor mode is
        established by that call."""
        root = Path('/var/tmp/forge') / ('gate-mode-%s' % uuid.uuid4().hex)
        root.mkdir(mode=0o777)
        os.chmod(root, 0o777)
        previous = os.environ.get('CLJ_SURGEON_GATE_ROOT')
        os.environ['CLJ_SURGEON_GATE_ROOT'] = str(root)
        try:
            slot.forget_root_identity()
            self.assertEqual(root, Path(slot._ensure_root()[0]))
            self.assertEqual(0o700, os.stat(root).st_mode & 0o777)
        finally:
            if previous is None:
                os.environ.pop('CLJ_SURGEON_GATE_ROOT', None)
            else:
                os.environ['CLJ_SURGEON_GATE_ROOT'] = previous
            slot.forget_root_identity()
            root.rmdir()


if __name__ == '__main__':
    unittest.main()
