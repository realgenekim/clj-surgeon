"""Pure CLI contracts and one filesystem/subprocess boundary test."""
import contextlib
import importlib.util
import io
import json
import os
from pathlib import Path
import subprocess
import tempfile
import unittest
from unittest.mock import patch
spec = importlib.util.spec_from_file_location('week', Path(__file__).with_name('usage_week.py'))
w = importlib.util.module_from_spec(spec)
spec.loader.exec_module(w)

class Command(unittest.TestCase):
    def test_week(self):
        args, start, end = w.arguments(['--week', '2026-09-07'])
        self.assertEqual('2026-09-07', args.week)
        self.assertEqual(w.instant('2026-09-07T00:00:00Z'), start)
        self.assertEqual(7 * 86400, end - start)

    def test_explicit_window(self):
        _, start, end = w.arguments(['--since', '2026-09-07', '--until', '2026-09-08'])
        self.assertEqual(86400, end - start)
        self.assertEqual(start, w.instant('2026-09-07T01:00:00+01:00'))
        self.assertEqual(start, w.instant('2026-09-06T23:00:00-01:00'))

    def test_invalid_arguments(self):
        for args in [[], ['--week', '2026-09-08'], ['--week', 'nonsense'],
                     ['--since', '2026-09-07'], ['--until', '2026-09-08'],
                     ['--since', '2026-09-08', '--until', '2026-09-07'],
                     ['--week', '2026-09-07', '--since', '2026-09-07']]:
            with self.subTest(args=args), contextlib.redirect_stderr(io.StringIO()):
                with self.assertRaises(SystemExit) as caught: w.arguments(args)
                self.assertEqual(2, caught.exception.code)

    def test_help_and_self_test_flag(self):
        output = io.StringIO()
        with contextlib.redirect_stdout(output), self.assertRaises(SystemExit) as caught:
            w.arguments(['--help'])
        self.assertEqual(0, caught.exception.code)
        for flag in ('--week', '--since', '--until', '--codex-root', '--claude-root', '--self-test'):
            self.assertIn(flag, output.getvalue())
        self.assertTrue(w.arguments(['--self-test'])[0].self_test)

    def test_bounded_end_to_end(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            (root / 'codex').mkdir(); (root / 'claude').mkdir()
            inside, outside = root / 'codex/rollout-inside.jsonl', root / 'claude/outside.jsonl'
            def event(kind, **fields):
                return {'timestamp': '2026-09-07T12:00:00Z', 'type': 'response_item',
                    'payload': dict(type=kind, **fields)}
            records = [event('function_call', name='exec_command', call_id='a',
                arguments='{"cmd":"clj-surgeon :op :ls"}'),
                       event('function_call_output', call_id='a', output='{:error-type :stale}'),
                       event('custom_tool_call', name='apply_patch', call_id='b',
                           input='*** Update File: x.clj'),
                       event('message', role='user', content='next turn'),
                       event('function_call', name='mcp__clj_surgeon__alias_migration', call_id='c',
                           arguments='{}'),
                       event('function_call_output', call_id='c', output='{:ok true}'),
                       event('custom_tool_call', name='apply_patch', call_id='d',
                           input='*** Update File: x.clj')]
            # Timestamp wins over an eligible mtime, and missing timestamps use mtime.
            records.append(dict(event('function_call', name='mcp__clj_surgeon__excluded', call_id='e'),
                timestamp='2026-09-14'))
            records.append({'type': 'assistant', 'message': {'content': [dict(type='tool_use', id='f',
                name='mcp__clj_surgeon__ls', input={})]}})
            records.append({'type': 'user', 'message': {'content': [dict(type='tool_result',
                tool_use_id='f', content='Error: broken')]}})
            records.append(event('message', role='user', content='new turn'))
            records.append(event('custom_tool_call', name='apply_patch', call_id='g',
                input='*** Update File: x.clj'))
            secret = event('function_call', name='mcp__clj_surgeon__secret_call', call_id='h',
                arguments={'secret': 'example'})
            inside.write_text('\n'.join(map(json.dumps, records + [secret])) + '\nnot json\n')
            outside.write_text(json.dumps(event('function_call', name='mcp__clj_surgeon__out_of_bound',
                call_id='i')))
            stamp = w.instant('2026-09-07T12:00:00Z')
            os.utime(inside, (stamp, stamp)); os.utime(outside, (0, 0))
            command = ['python3', '-I', str(Path(w.__file__)), '--codex-root', str(root / 'codex'),
                '--claude-root', str(root / 'claude')]
            result = subprocess.run(command + ['--week', '2026-09-07'], capture_output=True, text=True,
                timeout=10)
            self.assertEqual(0, result.returncode, result.stderr)
            lines = result.stdout.splitlines()
            self.assertEqual('op=ls calls=2 sessions=1 refusals=1 errors=1 unknown=0 fallbacks=1 '
                             'undecodable=0', lines[0])
            self.assertEqual('op=alias_migration calls=1 sessions=1 refusals=0 errors=0 unknown=0'
                             ' fallbacks=0 undecodable=0', lines[1])
            self.assertRegex(lines[2],
                r'^total=3 calls=3 sessions=1 refusals=1 errors=1 unknown=0 fallbacks=1 undecodable=0'
                r' files_scanned=1 seconds=\d+\.\d{3}$')
            # Round 3: an old timestamp remains eligible after an October append.
            stamp = w.instant('2026-10-01')
            os.utime(inside, (stamp, stamp))
            result = subprocess.run(command + ['--week', '2026-09-07'], capture_output=True, text=True,
                timeout=10)
            self.assertEqual(0, result.returncode, result.stderr)
            self.assertIn('total=2 calls=2 sessions=1 refusals=1 errors=0 unknown=0 fallbacks=1',
                          result.stdout)
            self.assertIn('files_scanned=1', result.stdout)
            # Neither provider's old file is opened in the empty week.
            result = subprocess.run(command + ['--week', '2026-11-16'], capture_output=True, text=True,
                timeout=10)
            self.assertEqual(0, result.returncode, result.stderr)
            self.assertRegex(result.stdout,
                r'^total=0 calls=0 sessions=0 refusals=0 errors=0 unknown=0 fallbacks=0 undecodable=0'
                r' files_scanned=0 seconds=\d+\.\d{3}\n$')

def event(kind, **fields):
    return json.dumps({'type': 'response_item', 'payload': dict(type=kind, **fields)})


class ScannerRegressions(unittest.TestCase):
    def scan(self, records):
        return list(w.scan_records(records, 'literal-session', 0))

    def test_identifier_patch_after_refusal_counts_fallback(self):
        # Independent review: identifier patches were invisible in Codex exec wrappers.
        source = (r'const patch = "*** Begin Patch\n*** Update File: src/app/echo_guard.clj\n..."; '
                  'tools.apply_patch(patch)')
        records = [event('function_call', call_id='a', name='clj-surgeon__edit_clojure', arguments={}),
                   event('function_call_output', call_id='a', output='refused · stale'),
                   event('custom_tool_call', call_id='b', name='functions.exec', input=source)]
        calls = self.scan(records)
        self.assertEqual(1, len(calls))
        self.assertEqual('refusal', calls[0]['outcome'])
        self.assertTrue(calls[0]['fallback'])
        self.assertEqual(1, w.p.aggregate(calls)['edit_clojure']['fallbacks'])

    def test_wrapper_output_only_last_call(self):
        records = [event('custom_tool_call', call_id='a', name='exec',
                         input='tools.exec_command({cmd:"clj-surgeon :op :ls"}); '
                               'tools["clj-surgeon__alias_migration"]({})'),
                   event('custom_tool_call_output', call_id='a', output='refused · stale')]
        calls = self.scan(records)
        self.assertEqual(['ls', 'alias_migration'], [c['op'] for c in calls])
        self.assertEqual(['unknown', 'refusal'], [c['outcome'] for c in calls])
        self.assertEqual(1, w.p.aggregate(calls)['ls']['unknown'])

    def test_continuation_join_and_missing_result(self):
        records = [event('custom_tool_call', call_id='a', name='exec',
                         input='tools.exec_command({cmd:"clj-surgeon :op :ls"})'),
                   event('custom_tool_call_output', call_id='a',
                         output='Script running with cell ID 42')]
        self.assertEqual('unknown', self.scan(records)[0]['outcome'])
        records += [event('function_call', call_id='b', name='functions.wait',
                          arguments='{"cell_id":"42"}'),
                    event('function_call_output', call_id='b', output='refused · stale')]
        self.assertEqual('refusal', self.scan(records)[0]['outcome'])
        records[1] = event('custom_tool_call_output', call_id='a',
                           output='refused · stale\nScript running with cell ID 42')
        records[-1] = event('function_call_output', call_id='b', output='exit code 0')
        self.assertEqual('refusal', self.scan(records)[0]['outcome'])

    def test_missing_ids_not_deduplicated(self):
        call = event('function_call', name='clj-surgeon__ls', arguments={})
        real = event('function_call', name='clj-surgeon__ls', call_id='real', arguments={})
        calls = self.scan([call, call, real, real])
        self.assertEqual(3, len(calls))
        self.assertTrue(all(c['outcome'] == 'unknown' for c in calls))

    def test_self_test_uses_current_interpreter(self):
        with patch.object(w.subprocess, 'call', return_value=0) as run:
            self.assertEqual(0, w.main(['--self-test']))
        self.assertEqual(2, run.call_count)
        self.assertTrue(all('-B' in c.args[0] for c in run.call_args_list))
        self.assertTrue(w.sys.dont_write_bytecode)
        self.assertTrue(all(c.args[0][0] == w.sys.executable for c in run.call_args_list))


    def test_content_list_survives_output_join(self):
        records = [event('function_call', name='clj-surgeon__ls', call_id='a', arguments={}),
                   event('function_call_output', call_id='a',
                         output=[{'type': 'text', 'text': 'Error: broken'}]),
                   event('custom_tool_call', name='apply_patch', input='*** Update File: x.clj')]
        calls = self.scan(records)
        self.assertEqual('error', calls[0]['outcome'])
        self.assertTrue(calls[0]['fallback'])

    def test_isolated_parent_disables_bytecode_without_flag(self):
        code = ('import runpy, sys; assert not sys.dont_write_bytecode; '
                'runpy.run_path(sys.argv[1]); assert sys.dont_write_bytecode')
        result = subprocess.run([w.sys.executable, '-I', '-c', code, w.__file__],
                                capture_output=True, text=True, timeout=10)
        self.assertEqual(0, result.returncode, result.stderr)

    def test_signal_killed_self_test_fails(self):
        for statuses in ([-9, 0], [0, -9], [1, 0]):
            with patch.object(w.subprocess, 'call', side_effect=statuses) as run:
                self.assertEqual(1, w.main(['--self-test']))
            self.assertEqual(2, run.call_count)

    def test_secret_session_line_is_discarded_before_json_parsing(self):
        secret = event('function_call', name='clj-surgeon__ls', call_id='secret',
                       arguments={'password': 'example'})
        ordinary = event('function_call', name='clj-surgeon__ls', call_id='ordinary', arguments={})
        original = w.json.loads
        with patch.object(w.json, 'loads', wraps=original) as parse:
            calls = self.scan([secret, ordinary])
        self.assertEqual(1, len(calls))
        self.assertEqual('ordinary', calls[0]['call_id'])
        self.assertNotIn(secret, [call.args[0] for call in parse.call_args_list])
        self.assertFalse(w.p.secret_line('password=example token=example'))

    def test_undecodable_without_call_is_counted_in_window(self):
        record = event('custom_tool_call', name='exec', input='unknown syntax')
        stats = dict(since=0, until=1, undecodable=0)
        self.assertEqual([], list(w.scan_records([record], 'one', 0, stats)))
        self.assertEqual(1, stats['undecodable'])
        self.assertEqual([], list(w.scan_records([record], 'two', 2, stats)))
        self.assertEqual(1, stats['undecodable'])

if __name__ == '__main__':
    unittest.main()
