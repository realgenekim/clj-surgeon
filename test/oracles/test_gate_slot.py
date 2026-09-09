"""TEST-ISO-015 / Sol GATE-LANES-FENCE-001 boundary regressions."""

import fcntl
import importlib.util
from pathlib import Path
import subprocess
import tempfile
import unittest

MODULE = Path(__file__).resolve().parents[1] / 'gate_slot.py'
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


if __name__ == '__main__':
    unittest.main()
