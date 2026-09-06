"""Offline orchestration witnesses: never import the provider runner."""
import importlib.util
from pathlib import Path
import types
import unittest
from unittest.mock import patch

spec = importlib.util.spec_from_file_location('cohort', Path(__file__).with_name('run.py'))
cohort = importlib.util.module_from_spec(spec)
spec.loader.exec_module(cohort)


class ScheduleTests(unittest.TestCase):
    def run_fake(self, bad=(), fault=None, floor_fails=False):
        events, rows = [], []
        def one(arm, i):
            events.append((arm, i))
            return {'arm': arm, 'pair': i, 'wall_s': float(i + 20),
                    'correct': (arm, i) not in bad,
                    'fault': 'identity-changed' if (arm, i) == fault else None}
        def floor(value):
            events.append(('floor', value))
            if floor_fails:
                raise cohort.ApparatusFault('cannot-persist-floor')
        try:
            cohort.execute_schedule(one, rows.append, floor)
            error = None
        except cohort.ApparatusFault as e:
            error = str(e)
        return events, rows, error

    def test_failed_sixth_native_control_is_retained_without_replacement(self):
        events, rows, error = self.run_fake(bad={('C', 6), ('T', 2)})
        self.assertIsNone(error)
        self.assertEqual(14, len(rows))
        self.assertEqual([('C', i) for i in range(1, 7)], events[:6])
        self.assertEqual('floor', events[6][0])
        self.assertEqual(5, events[6][1]['verified'])
        self.assertEqual(1, events[6][1]['failed'])
        self.assertEqual(26.0, events[6][1]['walls'][-1])
        self.assertFalse(rows[5]['correct'])
        self.assertEqual([('N', 1), ('T', 1), ('T', 2), ('N', 2),
                          ('N', 3), ('T', 3), ('T', 4), ('N', 4)], events[7:])

    def test_all_genuine_control_failures_still_have_six_fixed_attempts(self):
        events, rows, error = self.run_fake(bad={('C', i) for i in range(1, 7)})
        self.assertIsNone(error)
        self.assertEqual(0, events[6][1]['verified'])
        self.assertEqual(14, len(rows))

    def test_identity_fault_is_saved_then_stops(self):
        events, rows, error = self.run_fake(fault=('C', 3))
        self.assertEqual('identity-changed', error)
        self.assertEqual(3, len(rows))
        self.assertEqual(3, len(events))
        self.assertEqual('identity-changed', rows[-1]['fault'])

    def test_floor_is_persisted_before_any_paired_dispatch(self):
        events, rows, error = self.run_fake(floor_fails=True)
        self.assertEqual('cannot-persist-floor', error)
        self.assertEqual(6, len(rows))
        self.assertEqual('floor', events[-1][0])

    def test_actual_failure_shape_is_model_outcome_not_capture_fault(self):
        # Scalar fields from retained NW-real-1-1788663054-1248360-0/receipt.edn.
        receipt = {'arm': 'NW', 'mission': 'real-1', 'warm_session': cohort.SESSION,
                   'model': 'gpt-5.6-sol', 'dossier_sha256': 'pinned',
                   'first_verified_s': None, 'semantic_mismatch': 0,
                   'candidates': [{'raw_chars': 6, 'error': None, 'refusal': None,
                                   'apply_detail': 'no unified diff in the response'}]}
        self.assertFalse(cohort.native_correct(receipt, 'pinned'))
        with self.assertRaises(cohort.ApparatusFault):
            cohort.native_correct({**receipt, 'model': 'different-model'}, 'pinned')

    def test_native_entry_ends_orientation_and_keeps_verified_diff(self):
        captured = {}
        module = types.SimpleNamespace()
        def main():
            captured.update(prompt=module.WARM_TRIAL_PROMPT, argv=cohort.sys.argv)
        module.main = main
        with patch.object(cohort, 'sha', return_value='pinned'), \
             patch.object(cohort, 'load_native', return_value=module), \
             patch.object(cohort.sys, 'argv', ['offline-test']):
            cohort.native_entry('pinned')
        self.assertIn('Orientation is over.', captured['prompt'])
        self.assertIn('Complete the actual edit now', captured['prompt'])
        self.assertIn('KEEP the verified diff', captured['prompt'])
        self.assertIn('Reply DONE', captured['prompt'])
        self.assertEqual(['--arm', 'NW', '--mission', 'real-1', '--warm-session',
                          cohort.SESSION, '--runs', '1', '--k', '1'], captured['argv'][1:])


if __name__ == '__main__':
    unittest.main()
