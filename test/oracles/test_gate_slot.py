"""TEST-ISO-015 / Sol GATE-LANES-FENCE-001 boundary regressions."""

import fcntl
import importlib.util
import os
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest

MODULE = Path(__file__).resolve().parents[1] / 'gate_slot.py'
OWNED_TMP = '/var/tmp/forge'
SPEC = importlib.util.spec_from_file_location('gate_slot', MODULE)
slot = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(slot)


class GateSlotTest(unittest.TestCase):
    def test_capacity_boundaries(self):
        for cpus, memory, width in [(16, 18524, 8), (16, 3584, 1),
                                    (1, 65536, 1), (16, 5120, 2), (64, 8192, 4)]:
            self.assertEqual(width, slot.derived_width(cpus, memory))
        for memory in [0, 2048, 3583]:
            with self.assertRaisesRegex(RuntimeError, 'insufficient memory'):
                slot.derived_width(16, memory)

    def test_live_recompute_and_high_slot_shrink(self):
        with tempfile.TemporaryDirectory(dir='/var/tmp', prefix='gate-slot-test-') as tmp:
            root = Path(tmp)
            widths = iter([3, 3, 3, 1, 1, 2])
            samples = []

            def live():
                width = next(widths)
                samples.append(width)
                return {'width': width}

            holders = [slot.try_acquire(root, live) for _ in range(3)]
            try:
                self.assertTrue(all(holders))
                self.assertIsNone(slot.try_acquire(root, live))
                holders[0].close()
                holders[1].close()
                # The high slot alone fills the new width-one budget.
                self.assertIsNone(slot.try_acquire(root, live))
                extra = slot.try_acquire(root, live)
                self.assertIsNotNone(extra)
                extra.close()
                self.assertEqual([3, 3, 3, 1, 1, 2], samples)
            finally:
                for handle in holders:
                    handle.close()

    def test_exec_inherits_lock_and_exit_releases_it(self):
        with tempfile.TemporaryDirectory(dir='/var/tmp', prefix='gate-slot-test-') as tmp:
            root = Path(tmp)
            # This is the same exec boundary as production, with an owned root.
            program = ('import importlib.util,sys; from pathlib import Path; '
                       's=importlib.util.spec_from_file_location("slot",sys.argv[1]); '
                       'm=importlib.util.module_from_spec(s); s.loader.exec_module(m); '
                       'm.SLOT_ROOT=Path(sys.argv[2]); '
                       'm.main(["--", "sh", "-c", "echo ready; read value; exit 7"])')
            child = subprocess.Popen(['python3', '-B', '-c', program, str(MODULE), str(root)],
                                     stdin=subprocess.PIPE, stdout=subprocess.PIPE, text=True)
            try:
                self.assertEqual('ready', child.stdout.readline().strip())
                locks = []
                for path in root.glob('slot-*.lock'):
                    with path.open('r+') as handle:
                        try:
                            fcntl.flock(handle, fcntl.LOCK_EX | fcntl.LOCK_NB)
                        except BlockingIOError:
                            locks.append(path)
                self.assertEqual(1, len(locks))
                child.communicate('done\n', timeout=10)
                self.assertEqual(7, child.returncode)
                with locks[0].open('r+') as handle:
                    fcntl.flock(handle, fcntl.LOCK_EX | fcntl.LOCK_NB)
            finally:
                if child.poll() is None:
                    child.kill()
                    child.wait()
                child.stdin.close()
                child.stdout.close()

    def test_unknown_capacity_refuses_and_does_not_leak_admission_lock(self):
        with tempfile.TemporaryDirectory(dir='/var/tmp', prefix='gate-slot-test-') as tmp:
            root = Path(tmp)

            def unknown():
                raise RuntimeError('gate-refused: available memory is unknown')

            with self.assertRaisesRegex(RuntimeError, 'unknown'):
                slot.try_acquire(root, unknown)
            with (root / 'admission.lock').open('r+') as handle:
                fcntl.flock(handle, fcntl.LOCK_EX | fcntl.LOCK_NB)

    def test_killed_owner_and_failed_exec_release_slots(self):
        for fail_exec in [False, True]:
            with self.subTest(fail_exec=fail_exec):
                with tempfile.TemporaryDirectory(dir='/var/tmp', prefix='gate-slot-test-') as tmp:
                    root = Path(tmp)
                    command = '["--", "/no-such-gate-slot-worker"]' if fail_exec else (
                        '["--", "sh", "-c", "echo ready; read value"]')
                    program = ('import importlib.util,sys; from pathlib import Path; '
                               's=importlib.util.spec_from_file_location("slot",sys.argv[1]); '
                               'm=importlib.util.module_from_spec(s); s.loader.exec_module(m); '
                               'm.SLOT_ROOT=Path(sys.argv[2]); m.main(' + command + ')')
                    child = subprocess.Popen(['python3', '-B', '-c', program, str(MODULE), str(root)],
                                             stdin=subprocess.PIPE, stdout=subprocess.PIPE,
                                             stderr=subprocess.PIPE, text=True)
                    try:
                        if not fail_exec:
                            self.assertEqual('ready', child.stdout.readline().strip())
                            child.kill()
                        child.communicate(timeout=10)
                        self.assertNotEqual(0, child.returncode)
                        self.assertTrue(list(root.glob('slot-*.lock')))
                        for path in root.glob('slot-*.lock'):
                            with path.open('r+') as handle:
                                fcntl.flock(handle, fcntl.LOCK_EX | fcntl.LOCK_NB)
                    finally:
                        if child.poll() is None:
                            child.kill()
                            child.wait()
                        child.stdin.close()
                        child.stdout.close()
                        child.stderr.close()


    def test_removed_slot_root_refuses_and_never_splits_the_semaphore(self):
        """Sol GATE-LANES-FENCE-001-R2, executed verbatim: the production
        `try_acquire`, a live width of one, the root removed while the first
        handle is held. The bound survives because the second call refuses --
        it never recreates the root underneath a live holder."""
        with tempfile.TemporaryDirectory(dir=OWNED_TMP, prefix='gate-slot-delete-') as tmp:
            root = Path(tmp) / 'gate-slots'
            coordinator = slot.open_root(root)
            first = second = None
            refusal = None
            try:
                first = slot.try_acquire(root, lambda: {'width': 1})
                shutil.rmtree(root)
                try:
                    second = slot.try_acquire(root, lambda: {'width': 1})
                except RuntimeError as error:
                    refusal = str(error)
                observed = {'first_held': first is not None,
                            'root_removed': not root.exists(),
                            'second_admitted': second is not None,
                            'derived_width': 1,
                            'bound_violated': first is not None and second is not None}
                self.assertEqual({'first_held': True, 'root_removed': True,
                                  'second_admitted': False, 'derived_width': 1,
                                  'bound_violated': False}, observed)
                self.assertEqual('gate-refused: slot root missing', refusal)
                self.assertFalse(root.exists())
            finally:
                for handle in (first, second):
                    if handle is not None:
                        handle.close()
                os.close(coordinator)

    def test_replaced_slot_root_is_refused_against_the_coordinator_inode(self):
        """A root removed AND recreated is a different inode. The coordinator
        holds the admitted one open, so the identity it published cannot be
        reused, and a worker refuses the impostor instead of locking it."""
        with tempfile.TemporaryDirectory(dir=OWNED_TMP, prefix='gate-slot-replace-') as tmp:
            root = Path(tmp) / 'gate-slots'
            coordinator = slot.open_root(root)
            previous = os.environ.get(slot.ROOT_ID_ENV)
            os.environ[slot.ROOT_ID_ENV] = slot.published_identity(root, coordinator)
            first = None
            try:
                first = slot.try_acquire(root, lambda: {'width': 1})
                self.assertIsNotNone(first)
                shutil.rmtree(root)
                root.mkdir()
                with self.assertRaisesRegex(RuntimeError,
                                            'gate-refused: slot root replaced'):
                    slot.try_acquire(root, lambda: {'width': 1})
                # Nothing was locked, counted or created inside the impostor.
                self.assertEqual([], sorted(root.glob('slot-*.lock')))
            finally:
                if first is not None:
                    first.close()
                if previous is None:
                    os.environ.pop(slot.ROOT_ID_ENV, None)
                else:
                    os.environ[slot.ROOT_ID_ENV] = previous
                os.close(coordinator)

    def test_two_coordinators_share_one_bound_over_one_root_inode(self):
        """Two separate coordinator processes plus a third caller admit against
        one root: exactly the width is admitted, the surplus is refused."""
        with tempfile.TemporaryDirectory(dir=OWNED_TMP, prefix='gate-slot-pair-') as tmp:
            root = Path(tmp) / 'gate-slots'
            coordinator = slot.open_root(root)
            program = ('import importlib.util,sys; from pathlib import Path; '
                       's=importlib.util.spec_from_file_location("slot",sys.argv[1]); '
                       'm=importlib.util.module_from_spec(s); s.loader.exec_module(m); '
                       'h=m.try_acquire(Path(sys.argv[2]), lambda: {"width": 2}); '
                       'print("admitted" if h else "refused", flush=True); '
                       'sys.stdin.readline()')
            children = []
            try:
                verdicts = []
                for _ in range(3):
                    child = subprocess.Popen(
                        ['python3', '-B', '-c', program, str(MODULE), str(root)],
                        stdin=subprocess.PIPE, stdout=subprocess.PIPE, text=True)
                    children.append(child)
                    verdicts.append(child.stdout.readline().strip())
                self.assertEqual(['admitted', 'admitted', 'refused'], verdicts)
            finally:
                for child in children:
                    try:
                        child.communicate('go\n', timeout=10)
                    except Exception:
                        child.kill()
                        child.wait()
                os.close(coordinator)



    def test_published_identity_binds_to_its_own_root_path(self):
        """The coordinator's identity travels in the environment to every
        worker, including workers that run this module against a root of their
        own. It is scoped to the path it names, so an unrelated root is admitted
        normally instead of being refused as an impostor."""
        with tempfile.TemporaryDirectory(dir=OWNED_TMP, prefix='gate-slot-scope-') as tmp:
            box = Path(tmp) / 'box'
            own = Path(tmp) / 'own'
            box_fd = slot.open_root(box)
            own_fd = slot.open_root(own)
            previous = os.environ.get(slot.ROOT_ID_ENV)
            os.environ[slot.ROOT_ID_ENV] = slot.published_identity(box, box_fd)
            handle = None
            try:
                handle = slot.try_acquire(own, lambda: {'width': 1})
                self.assertIsNotNone(handle)
                self.assertIsNone(slot._expected_from_env(own))
                self.assertEqual(slot.root_identity(box_fd), slot._expected_from_env(box))
            finally:
                if handle is not None:
                    handle.close()
                if previous is None:
                    os.environ.pop(slot.ROOT_ID_ENV, None)
                else:
                    os.environ[slot.ROOT_ID_ENV] = previous
                os.close(box_fd)
                os.close(own_fd)



if __name__ == '__main__':
    unittest.main()
