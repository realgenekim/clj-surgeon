"""Offline orchestration witnesses: never import the provider runner."""
import importlib.util
from pathlib import Path
import types
import tempfile
import os
import copy
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
                   'candidates': [{'verified': False, 'raw_chars': 6, 'error': None, 'refusal': None,
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


class PreflightTests(unittest.TestCase):
    expected = {'cli_version': '0.153.3',
                'workdir': '/var/tmp/forge/typist-real-fx/warm-ws-real-1',
                'model': 'gpt-5.6-sol', 'provider': 'openai',
                'reasoning effort': 'medium', 'approval': 'never',
                'sandbox': 'danger-full-access'}
    capture = Path('/var/tmp/forge/typist-real-fx/NW-real-1-1788663054-1248360-0/nw-stdout.txt')

    def test_actual_c6_header_is_valid_despite_failed_edit(self):
        attested = cohort.native_capture(self.capture, self.expected)
        self.assertEqual('01a074a0-4098-7283-8ee1-603b83d1cf46', attested['fork_session_id'])
        self.assertNotEqual(cohort.SESSION, attested['fork_session_id'])
        self.assertEqual('gpt-5.6-sol', attested['model'])
        receipt = cohort.ednjson(self.capture.with_name('receipt.edn'))
        self.assertFalse(cohort.native_correct(receipt,
            '73760d224c67dfa70736f4f031a12192184c875e53c11fa7a203dbf006010a76'))

    def test_only_opening_stderr_header_can_attest_identity(self):
        raw = self.capture.read_bytes()
        for changed in [raw.replace(b'model: gpt-5.6-sol', b'model: other', 1),
                        raw.replace(b'OpenAI Codex v0.153.3', b'OpenAI Codex v0.147.0', 1),
                        raw.replace(b'reasoning effort: medium', b'reasoning effort: high', 1),
                        raw.replace(b'workdir: /var/tmp/forge/', b'workdir: /elsewhere/', 1),
                        raw.replace(b'----- stderr -----\n', b'----- stderr -----\nwarning\n', 1),
                        b'quoted example\n' + raw.replace(b'----- stderr -----', b'other delimiter'),
                        raw + b'\n----- stderr -----\n' + raw,
                        raw.replace(b'model: gpt-5.6-sol', b'model: gpt-5.6-sol\nmodel: gpt-5.6-sol', 1)]:
            with self.subTest(prefix=changed[:20]), self.assertRaises(cohort.ApparatusFault):
                cohort.parse_native_header(changed, self.expected)
        with self.assertRaises(cohort.ApparatusFault):
            cohort.parse_native_header(raw.replace(b'model: gpt-5.6-sol', b'model: other', 1)
                                       + b'\nmodel: gpt-5.6-sol\n', self.expected)

    def test_seed_bytes_modes_extra_files_and_symlinks_refuse_before_dispatch(self):
        with tempfile.TemporaryDirectory(prefix='raw-v2-offline-', dir='/var/tmp/forge') as tmp:
            root = Path(tmp)
            source = root / 'source.clj'
            source.write_text('(ns fixture)\n')
            source.chmod(0o644)
            baseline = cohort.fixture_inventory(root)
            cohort.check_fixture(root, baseline)
            mutations = [lambda: source.write_text('changed'), lambda: source.chmod(0o600),
                         lambda: (root / 'extra').write_text('extra'),
                         lambda: (root / 'link').symlink_to(source)]
            for change in mutations:
                change()
                with self.assertRaises(cohort.ApparatusFault):
                    cohort.check_fixture(root, baseline)
                for name in ['extra', 'link']:
                    (root / name).unlink(missing_ok=True)
                source.write_text('(ns fixture)\n')
                source.chmod(0o644)
            linked = root / 'root-link'
            linked.symlink_to(root, target_is_directory=True)
            with self.assertRaises(cohort.ApparatusFault):
                cohort.check_fixture(linked, baseline)
            linked.unlink()

    def test_actual_completed_handdrive_transport_and_proof_evidence(self):
        view = cohort.ednjson('/var/tmp/forge/astra-raw-live-fx/stdout')
        closed = cohort.ednjson(Path(view['receipt']['artifacts']) / 'transport-close.edn')
        self.assertIsNone(cohort.tool_evidence_fault(view, closed))

    def test_source_and_proof_failures_are_not_invented_transport_faults(self):
        transport = {'terminated?': True, 'cancelled': [], 'completed': [
            {'index': 0, 'usable': True, 'model': 'openai/gpt-oss-120b', 'upstream': 'Cerebras'}]}
        view = {'receipt': {'candidates': [{'index': 0, 'compiled': False, 'error-type': 'forms-owner-mismatch'}]}}
        self.assertIsNone(cohort.tool_evidence_fault(view, transport))
        for finished, exit_code in [(True, 1), (False, None)]:
            proof = {'ok': False, 'gate': {'ok': False, 'results': [
                {'finished?': finished, 'exit': exit_code}]}, 'acceptance': None}
            failed = {'receipt': {'candidates': [{'index': 0, 'compiled': True, 'proof': proof}]}}
            self.assertIsNone(cohort.tool_evidence_fault(failed, transport))
        for kind in ['provider-refusal', 'output-length', 'empty-content', 'nonterminal-output']:
            other = copy.deepcopy(transport)
            other['completed'][0].update(usable=False, error_type=kind)
            self.assertIsNone(cohort.tool_evidence_fault(view, other))
        for kind in ['timeout', 'provider-rate-limited', 'model-mismatch', 'transport-or-response-error']:
            other = copy.deepcopy(transport)
            other['completed'][0].update(usable=False, error_type=kind)
            self.assertEqual('tool-transport-fault-needs-review', cohort.tool_evidence_fault(view, other))
        missing = {'receipt': {'candidates': [{'index': 0, 'compiled': True, 'proof': {'ok': False}}]}}
        self.assertEqual('tool-profile-evidence-missing', cohort.tool_evidence_fault(missing, transport))
        self.assertEqual('tool-transport-cleanup-unconfirmed', cohort.tool_evidence_fault(view, {}))


if __name__ == '__main__':
    unittest.main()
