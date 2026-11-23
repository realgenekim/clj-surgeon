"""Literal fixtures minimized from the reference miner's documented record shapes."""
import importlib.util
from pathlib import Path
import unittest
spec = importlib.util.spec_from_file_location('parse', Path(__file__).with_name('usage_week_parse.py'))
p = importlib.util.module_from_spec(spec)
spec.loader.exec_module(p)
CLI = {'name': 'exec_command', 'arguments': '{"cmd":"clj-surgeon :op :split-ns!"}',
    'output': '{:error-type :stale}'}
MCP = {'name': 'mcp__clj_surgeon__alias_migration', 'input': {}, 'output': 'success'}
PATCH = {'name': 'apply_patch',
    'input': '*** Begin Patch\n*** Update File: src/x.clj\n@@\n-x\n+y\n*** End Patch'}

class Relations(unittest.TestCase):
    def test_codex_payload(self):
        call = p.detect_call({'type': 'response_item', 'payload': CLI})
        self.assertEqual(('split-ns!', 'cli', '{:error-type :stale}'),
                         (call['op'], call['surface'], call['output']))
        self.assertEqual('refusal', p.classify_outcome(call['output']))

    def test_mcp(self):
        call = p.detect_call(MCP)
        self.assertEqual(('alias_migration', 'mcp'), (call['op'], call['surface']))
        self.assertEqual('success', p.classify_outcome(call['output']))
        self.assertEqual('inspect_clojure', p.detect_call({'name': 'clj-surgeon.inspect_clojure'})['op'])

    def test_babashka(self):
        call = p.detect_call({'name': 'Bash',
            'input': {'command': 'bb --classpath src -m clj-surgeon.core :op :ls'}})
        self.assertEqual('ls', call['op'])

    def test_non_calls(self):
        for command in ('rg clj-surgeon :op :ls', 'echo "clj-surgeon :op :ls"', 'cat ~/bin/clj-surgeon',
            'clj-surgeon "'):
            self.assertIsNone(p.detect_call({'name': 'exec_command', 'input': {'cmd': command}}))
        self.assertIsNone(p.detect_call({'role': 'user', 'content': 'clj-surgeon :op :ls'}))

    def test_outcomes(self):
        for output, expected in [('', 'unknown'), ('{:error-type nil}', 'unknown'),
                                 ({'refusal_code': None}, 'unknown'), ({'error': 'bad'}, 'error'),
                                     ('exit code -1', 'error'), ('arbitrary text', 'unknown'),
                                         ('exit code 1', 'error'), ({'exit_code': 2}, 'error'),
                                 ('Process exited with code 0', 'success'), ('Error: bad', 'error'),
                                 ({'is_error': True}, 'error'), ('refused · stale', 'refusal'),
                                 ('{:error-type :stale}\nexit code 1', 'refusal')]:
            with self.subTest(output=output): self.assertEqual(expected, p.classify_outcome(output))

    def test_fallback_requires_failed_call_and_later_edit(self):
        self.assertTrue(p.fold_turn([CLI, PATCH])[0]['fallback'])
        self.assertFalse(p.fold_turn([MCP, PATCH])[0]['fallback'])
        self.assertFalse(p.fold_turn([PATCH, CLI])[0]['fallback'])
        self.assertFalse(p.fold_turn([CLI])[0]['fallback'])
        self.assertTrue(p.fold_turn([dict(CLI, output='Error: broken'), PATCH])[0]['fallback'])
        self.assertEqual([], p.fold_turn([PATCH]))
        self.assertNotIn('fallback', CLI)

    def test_edits(self):
        for record in [PATCH, {'name': 'Edit', 'input': {'file_path': 'x.cljc'}},
                       {'name': 'Write', 'input': {'file_path': 'x.edn'}},
                       {'name': 'Bash', 'input': {'command': 'sed -i s/a/b/ x.cljs'}},
                       {'name': 'Bash', 'input': {'command': 'cat > x.clj <<EOF'}},
                       {'name': 'Bash',
                           'input': {'command': "python3 -c \"Path('x.clj').write_text('x')\""}}]:
            self.assertTrue(p.is_native_clj_edit(record), record)
        for record in [{'name': 'Read', 'input': {'file_path': 'x.clj'}},
                       {'name': 'Bash', 'input': {'command': 'cat x.clj'}},
                       {'name': 'Write', 'input': {'file_path': 'x.py'}}]:
            self.assertFalse(p.is_native_clj_edit(record))

    def test_secrets(self):
        for line in ['ghp_' + 'a' * 20, 'github_pat_example', 'sk-' + 'a' * 20,
                     'xoxb-example', '-----BEGIN', 'Bearer ' + 'a' * 20,
                     'AKIA' + 'A' * 16, '"password": "example"', r'\"secret\": \"example\"']:
            self.assertTrue(p.secret_line(line))
        self.assertFalse(p.secret_line('{"name":"inspect_clojure"}'))
        self.assertTrue(p.secret_line('AKİA' + 'A' * 16))

    def test_wrapper_literal(self):
        record = {'name': 'exec', 'arguments': 'text(await tools.exec_command({cmd:"clj-surgeon :op :ls"}));'}
        self.assertEqual('ls', p.detect_call(next(p.actions(record)))['op'])

    def test_shell_segments_and_heredoc_prose(self):
        for command, expected in [
            ('clj-surgeon :op :ls; clj-surgeon :op :xray', ['ls', 'xray']),
            ('cd /tmp\nclj-surgeon :op :ls', ['ls']),
            ("cat > /tmp/example <<'EOF'\nclj-surgeon :op :ls\nEOF\necho done", []),
            ('echo -m clj-surgeon.core :op :ls', []),
            ('echo "clj-surgeon :op :ls"', [])]:
            actions = p.actions({'name': 'Bash', 'input': {'command': command}})
            self.assertEqual(expected, [c['op'] for c in p.fold_turn(list(actions))])

    def test_python_heredoc_edit_survives_shell_normalization(self):
        record = {'name': 'Bash', 'input': {'command': "python3 - <<'PY'\nPath('x.clj').write_text('x')\nPY"}}
        self.assertTrue(p.fold_turn([CLI, *p.actions(record)])[0]['fallback'])

    def test_native_target_not_source_content(self):
        self.assertFalse(p.is_native_clj_edit({'name': 'Write', 'input': {'file_path': 'x.py',
            'content': 'x.clj'}}))
        self.assertFalse(p.is_native_clj_edit({'name': 'apply_patch',
            'input': '*** Update File: x.py\n+x.clj'}))
        self.assertTrue(p.is_native_clj_edit({'name': 'Bash',
            'input': {'command': "python3 -c 'open(\"x.edn\", \"w\").write(\"{}\")'"}}))

    def test_aggregation_and_render(self):
        calls = p.fold_turn([CLI, MCP, MCP, PATCH])
        counts = p.aggregate(calls)
        self.assertEqual(dict(calls=1, sessions=1, refusals=1, errors=0, unknown=0, fallbacks=0),
            counts['split-ns!'])
        totals = dict(calls=3, sessions=1, refusals=1, errors=0, unknown=0, fallbacks=0, files_scanned=2,
            seconds='0.123')
        self.assertEqual('op=alias_migration calls=2 sessions=1 refusals=0 errors=0 unknown=0 fallbacks=0\n'
                         'op=split-ns! calls=1 sessions=1 refusals=1 errors=0 unknown=0 fallbacks=0\n'
                         'total=3 calls=3 sessions=1 refusals=1 errors=0 unknown=0 fallbacks=0'
                         ' files_scanned=2 seconds=0.123', p.render(counts, totals))
        self.assertEqual({}, p.aggregate([]))
        self.assertEqual(2, p.aggregate([dict(calls[0], session='a'), dict(calls[0],
            session='b')])['split-ns!']['sessions'])

class ReviewRegressions(unittest.TestCase):
    def test_prefixes_and_shell_list(self):
        for command in ('FOO=1 clj-surgeon :op :ls', 'TMPDIR=/x ~/bin/clj-surgeon :op :ls',
                        'JAVA_TOOL_OPTIONS=... clj-surgeon :op :ls', 'time clj-surgeon :op :ls',
                        'command clj-surgeon :op :ls', 'cd X && clj-surgeon :op :ls',
                        'nohup clj-surgeon :op :ls', 'timeout 10 clj-surgeon :op :ls',
                        'bb --config deps.edn -m clj-surgeon.core :op :ls',
                        ['clj-surgeon', ':op', ':ls']):
            with self.subTest(command=command):
                records = list(p.actions({'name': 'shell', 'input': {'command': command}}))
                self.assertEqual(['ls'], [c['op'] for c in p.fold_turn(records)])

    def test_written_targets(self):
        for command, expected in [
            ('cat > README.md <<EOF\nx.clj\nEOF', False),
            ("cat > README.md <<EOF\nPath('x.clj').write_text('x')\nEOF", False),
            ('rg x.clj; sed -i y README.md', False),
            ('S=/var/tmp/run/scratchpad; cat > $S/tasks/expected.clj', False),
            ('cat >> src/x.clj', True), ('tee src/x.edn', True),
            ('sed -i s/a/b/ /tmp/x.clj', False),
            ('cat > /var/tmp/x.clj', True), ('tee scratchpad/x.clj', False),
            ("python -c \"open('/repo/scratchpad/x.clj', 'w')\"", False)]:
            with self.subTest(command=command):
                self.assertEqual(expected, p.is_native_clj_edit(
                    {'name': 'Bash', 'input': {'command': command}}))
        for path in ('/tmp/x.clj', '/var/tmp/forge/claude-1011/x/scratchpad/t.clj',
                     'scratchpad/x.edn', '/r/scratchpad/x.cljs'):
            self.assertFalse(p.is_native_clj_edit({'name': 'Write', 'input': {'file_path': path}}))
            self.assertFalse(p.is_native_clj_edit({'name': 'apply_patch',
                                                  'input': '*** Add File: ' + path}))


    def test_identifier_patch_wrapper_from_independent_review(self):
        # Round 3: Codex binds a patch string before passing its identifier.
        source = (r'const patch = "*** Begin Patch\n*** Update File: src/app/echo_guard.clj\n..."; '
                  'tools.apply_patch(patch)')
        records = list(p.actions({'name': 'functions.exec', 'input': source}))
        self.assertTrue(any(map(p.is_native_clj_edit, records)))
        self.assertTrue(p.fold_turn([CLI, *records])[0]['fallback'])

    def test_identifier_patch_whole_wrapper_and_order(self):
        for marker in ('Update', 'Add'):
            for extension in ('clj', 'cljc', 'cljs', 'edn'):
                source = ('const $patch = `*** Begin Patch\n*** ' + marker
                          + ' File: src/a.' + extension + '\n*** End Patch`; '
                          'tools.apply_patch($patch); tools["clj-surgeon__alias_migration"]({})')
                with self.subTest(marker=marker, extension=extension):
                    records = list(p.actions({'name': 'exec', 'input': source}))
                    self.assertTrue(p.is_native_clj_edit(records[0]))
                    self.assertEqual('alias_migration', p.detect_call(records[1])['op'])

    def test_identifier_patch_requires_call_and_written_target(self):
        for source in ('const patch = "*** Update File: src/a.clj";',
                       'tools.apply_patch(patch)',
                       r'const patch = "*** Update File: x.py\n+x.clj"; tools.apply_patch(patch)'):
            self.assertFalse(any(map(p.is_native_clj_edit, p.actions({'name': 'exec', 'input': source}))))

    def test_var_tmp_repository_paths(self):
        for path, expected in [
            ('/var/tmp/forge/coldstart/x/wt/src/a.clj', True),
            ('/var/tmp/x.cljc', True),
            ('/var/tmp/forge/claude-1011/src/a.clj', False),
            ('/var/tmp/forge/claude-1011/x/scratchpad/t.clj', False),
            ('/tmp/x.clj', False)]:
            for name, args in [('Write', {'file_path': path}), ('apply_patch', '*** Add File: ' + path)]:
                with self.subTest(path=path, name=name):
                    self.assertEqual(expected, p.is_native_clj_edit({'name': name, 'input': args}))

    def test_success_closes_fallback_window(self):
        self.assertFalse(p.fold_turn([CLI, MCP, PATCH])[0]['fallback'])
        self.assertTrue(p.fold_turn([CLI, PATCH, MCP])[0]['fallback'])

    def test_escaped_and_content_refusals(self):
        for output in (r'{\"refusal_code\": \"stale\"}', r'{\"error_type\": \"stale\"}',
                       [{'type': 'text', 'text': '{"refusal_code": "stale"}'}],
                       [{'type': 'text', 'text': 'refused · stale'}], 'refused · stale'):
            self.assertEqual('refusal', p.classify_outcome(output))

    def test_bracket_mcp(self):
        records = list(p.actions({'name': 'exec',
                                  'input': 'tools["clj-surgeon__alias_migration"]({})'}))
        self.assertEqual('alias_migration', p.detect_call(records[0])['op'])

if __name__ == '__main__':
    unittest.main()
